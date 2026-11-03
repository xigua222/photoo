package com.voicelike.app

import android.app.ActivityManager
import android.content.Context

object MemorySafetyMode {
    private const val PREFS_NAME = "voicelike_prefs"
    private const val KEY_OOM_FALLBACK_ACTIVE = "oom_fallback_active"
    private const val KEY_LAST_OOM_AT_MS = "last_oom_at_ms"
    private const val KEY_LAST_VERSION_CODE = "last_version_code"
    private const val KEY_UPGRADE_COMPAT_ACTIVE = "upgrade_compat_active"
    private const val KEY_UPGRADE_COMPAT_UNTIL_MS = "upgrade_compat_until_ms"
    private const val KEY_UPGRADE_COMPAT_LAUNCHES_LEFT = "upgrade_compat_launches_left"
    private const val UPGRADE_COMPAT_WINDOW_MS = 72L * 60L * 60L * 1000L
    private const val UPGRADE_COMPAT_LAUNCH_BUDGET = 4

    fun isOomFallbackActive(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_OOM_FALLBACK_ACTIVE, false)
    }

    fun registerAppLaunch(context: Context, currentVersionCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastVersionCode = prefs.getInt(KEY_LAST_VERSION_CODE, 0)
        var compatActive = prefs.getBoolean(KEY_UPGRADE_COMPAT_ACTIVE, false)
        var compatUntilMs = prefs.getLong(KEY_UPGRADE_COMPAT_UNTIL_MS, 0L)
        var launchesLeft = prefs.getInt(KEY_UPGRADE_COMPAT_LAUNCHES_LEFT, 0)

        if (lastVersionCode > 0 && lastVersionCode != currentVersionCode) {
            compatActive = true
            compatUntilMs = now + UPGRADE_COMPAT_WINDOW_MS
            launchesLeft = UPGRADE_COMPAT_LAUNCH_BUDGET
        } else if (compatActive) {
            val expired = compatUntilMs <= now
            if (expired || launchesLeft <= 0) {
                compatActive = false
                compatUntilMs = 0L
                launchesLeft = 0
            } else {
                launchesLeft = (launchesLeft - 1).coerceAtLeast(0)
            }
        }

        prefs.edit()
            .putInt(KEY_LAST_VERSION_CODE, currentVersionCode)
            .putBoolean(KEY_UPGRADE_COMPAT_ACTIVE, compatActive)
            .putLong(KEY_UPGRADE_COMPAT_UNTIL_MS, compatUntilMs)
            .putInt(KEY_UPGRADE_COMPAT_LAUNCHES_LEFT, launchesLeft)
            .apply()
    }

    fun markOomCrash(context: Context) {
        activateOomFallback(context)
    }

    fun activateOomFallback(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OOM_FALLBACK_ACTIVE, true)
            .putLong(KEY_LAST_OOM_AT_MS, System.currentTimeMillis())
            .apply()
    }

    fun clearOomFallback(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OOM_FALLBACK_ACTIVE, false)
            .apply()
    }

    fun isUpgradeCompatibilityActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_UPGRADE_COMPAT_ACTIVE, false)) return false
        val untilMs = prefs.getLong(KEY_UPGRADE_COMPAT_UNTIL_MS, 0L)
        val launchesLeft = prefs.getInt(KEY_UPGRADE_COMPAT_LAUNCHES_LEFT, 0)
        return launchesLeft > 0 && untilMs > System.currentTimeMillis()
    }

    fun isLowRamDevice(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return manager?.isLowRamDevice == true
    }

    fun shouldUseConservativeMemory(context: Context): Boolean {
        return isOomFallbackActive(context) ||
            isLowRamDevice(context)
    }

    fun shouldReduceBackgroundWork(context: Context): Boolean {
        return isOomFallbackActive(context) ||
            isLowRamDevice(context) ||
            AppPerformanceGovernor.isMemoryPressureHigh()
    }
}
