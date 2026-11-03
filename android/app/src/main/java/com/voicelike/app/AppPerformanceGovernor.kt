package com.voicelike.app

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Global lightweight performance governor used to reduce heat and battery drain.
 * It adapts background workloads based on app foreground state, power saver, and thermal status.
 */
object AppPerformanceGovernor {
    enum class PerfMode {
        NORMAL,
        BALANCED,
        COOL
    }

    private val initialized = AtomicBoolean(false)
    private val startedActivities = AtomicInteger(0)
    private val criticalInteraction = AtomicBoolean(false)
    private val deferNonCriticalUntilMs = AtomicLong(0L)
    private val transientMemoryPressureUntilMs = AtomicLong(0L)
    private var powerManager: PowerManager? = null
    @Volatile
    private var oomFallbackActive: Boolean = false

    @Volatile
    private var isAppInForeground: Boolean = true

    @Volatile
    private var thermalStatus: Int = PowerManager.THERMAL_STATUS_NONE
    @Volatile
    private var lastTrimLevel: Int = 0
    @Volatile
    private var lowMemorySignaled: Boolean = false

    private const val TRANSIENT_MEMORY_PRESSURE_WINDOW_MS = 4_000L

    fun init(application: Application) {
        if (!initialized.compareAndSet(false, true)) return
        oomFallbackActive = MemorySafetyMode.isOomFallbackActive(application)

        powerManager = application.getSystemService(PowerManager::class.java)
        thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager?.addThermalStatusListener(application.mainExecutor) { status ->
                thermalStatus = status
            }
        }

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivities.incrementAndGet() > 0) {
                    isAppInForeground = true
                    clearTransientMemoryPressureIfRecovered()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (startedActivities.decrementAndGet() <= 0) {
                    startedActivities.set(0)
                    isAppInForeground = false
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) {
                clearTransientMemoryPressureIfRecovered()
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun currentMode(): PerfMode {
        val inForeground = isAppInForeground
        val inPowerSave = powerManager?.isPowerSaveMode == true

        if (!inForeground || inPowerSave) return PerfMode.COOL
        return when {
            thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE -> PerfMode.COOL
            thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE -> PerfMode.BALANCED
            else -> PerfMode.NORMAL
        }
    }

    fun analysisParallelism(availableCores: Int): Int {
        val normalMax = if (oomFallbackActive) 2 else 3
        val memoryPressure = isMemoryPressureHigh()
        return when (currentMode()) {
            PerfMode.COOL -> 1
            PerfMode.BALANCED -> {
                val base = (availableCores / 3).coerceIn(1, 2)
                if (memoryPressure) 1 else base
            }
            PerfMode.NORMAL -> {
                val base = (availableCores / 2).coerceIn(1, normalMax)
                if (memoryPressure) base.coerceAtMost(1) else base
            }
        }
    }

    fun shouldPauseNonCriticalWork(): Boolean {
        // Hard pause only when app is in critical interaction or backgrounded.
        // In COOL / power-save mode we should throttle, not fully stall progress.
        return criticalInteraction.get() ||
            !isAppInForeground ||
            SystemClock.elapsedRealtime() < deferNonCriticalUntilMs.get()
    }

    fun scaledDelay(baseDelayMs: Long): Long {
        val modeDelay = when (currentMode()) {
            PerfMode.COOL -> (baseDelayMs * 3).coerceAtMost(5000L)
            PerfMode.BALANCED -> (baseDelayMs * 2).coerceAtMost(3000L)
            PerfMode.NORMAL -> baseDelayMs
        }
        return if (isMemoryPressureHigh()) {
            (modeDelay * 2).coerceAtMost(5000L)
        } else {
            modeDelay
        }
    }

    fun setCriticalInteraction(active: Boolean) {
        criticalInteraction.set(active)
    }

    fun deferNonCriticalWorkFor(durationMs: Long) {
        if (durationMs <= 0L) return
        val target = SystemClock.elapsedRealtime() + durationMs
        while (true) {
            val current = deferNonCriticalUntilMs.get()
            if (current >= target) return
            if (deferNonCriticalUntilMs.compareAndSet(current, target)) {
                return
            }
        }
    }

    fun onTrimMemory(level: Int) {
        lastTrimLevel = level
        if (isRunningMemoryPressureTrim(level)) {
            lowMemorySignaled = true
            markTransientMemoryPressure()
            return
        }
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            clearTransientMemoryPressureIfRecovered()
            return
        }
        if (!isHeapHeadroomLow()) {
            lowMemorySignaled = false
            transientMemoryPressureUntilMs.set(0L)
        }
    }

    fun onLowMemory() {
        lowMemorySignaled = true
        markTransientMemoryPressure()
    }

    fun onCaughtOutOfMemory() {
        oomFallbackActive = true
        onLowMemory()
        deferNonCriticalWorkFor(2_500L)
    }

    fun isMemoryPressureHigh(): Boolean {
        if (oomFallbackActive) return true
        return isRealtimeMemoryPressureHigh()
    }

    fun isRealtimeMemoryPressureHigh(): Boolean {
        if (isHeapHeadroomLow()) return true
        val transientActive = SystemClock.elapsedRealtime() < transientMemoryPressureUntilMs.get()
        if (!transientActive && lowMemorySignaled) {
            lowMemorySignaled = false
        }
        return transientActive
    }

    fun isRunningModerateTrimLevel(level: Int): Boolean {
        return level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE
    }

    fun isRunningLowTrimLevel(level: Int): Boolean {
        return level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
    }

    fun isRunningCriticalTrimLevel(level: Int): Boolean {
        return level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
    }

    fun isRunningLowOrCriticalTrimLevel(level: Int): Boolean {
        return isRunningLowTrimLevel(level) || isRunningCriticalTrimLevel(level)
    }

    fun isRunningMemoryPressureTrim(level: Int): Boolean {
        return isRunningModerateTrimLevel(level) ||
            isRunningLowTrimLevel(level) ||
            isRunningCriticalTrimLevel(level)
    }

    private fun isHeapHeadroomLow(): Boolean {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val max = runtime.maxMemory().coerceAtLeast(1L)
        val headroom = max - used
        val absoluteFloor = 24L * 1024L * 1024L
        val ratioFloor = max / 10L
        return headroom <= maxOf(absoluteFloor, ratioFloor)
    }

    private fun markTransientMemoryPressure() {
        val target = SystemClock.elapsedRealtime() + TRANSIENT_MEMORY_PRESSURE_WINDOW_MS
        while (true) {
            val current = transientMemoryPressureUntilMs.get()
            if (current >= target) return
            if (transientMemoryPressureUntilMs.compareAndSet(current, target)) {
                return
            }
        }
    }

    private fun clearTransientMemoryPressureIfRecovered() {
        if (isHeapHeadroomLow()) return
        lowMemorySignaled = false
        transientMemoryPressureUntilMs.set(0L)
        lastTrimLevel = 0
    }
}
