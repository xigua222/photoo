package com.voicelike.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central scheduler for non-critical home-page scans.
 * Ensures all scan-like jobs run under one global queue with dedupe and adaptive throttling.
 */
object HomeScanScheduler {
    const val TASK_CARD_WARMUP = "card_warmup"
    const val TASK_SIMILAR_LOOKAHEAD = "similar_lookahead"
    const val TASK_SIMILAR_SEED_ON_OPEN = "similar_seed_on_open"
    const val TASK_STATS_WARMUP = "stats_warmup"

    private val runMutex = Mutex()
    private val runningByTask = ConcurrentHashMap<String, AtomicBoolean>()
    private val lastRunAt = ConcurrentHashMap<String, Long>()

    private fun gate(taskKey: String): AtomicBoolean {
        return runningByTask.computeIfAbsent(taskKey) { AtomicBoolean(false) }
    }

    suspend fun <T> run(
        taskKey: String,
        minIntervalMs: Long = 0L,
        block: suspend () -> T
    ): T? {
        val gate = gate(taskKey)
        if (!gate.compareAndSet(false, true)) return null
        try {
            val now = System.currentTimeMillis()
            val requiredGap = AppPerformanceGovernor.scaledDelay(minIntervalMs)
            val last = lastRunAt[taskKey] ?: 0L
            if (requiredGap > 0 && now - last < requiredGap) {
                return null
            }

            return runMutex.withLock {
                while (AppPerformanceGovernor.shouldPauseNonCriticalWork()) {
                    delay(AppPerformanceGovernor.scaledDelay(400))
                }
                val result = kotlinx.coroutines.withContext(Dispatchers.Default) {
                    block()
                }
                lastRunAt[taskKey] = System.currentTimeMillis()
                result
            }
        } finally {
            gate.set(false)
        }
    }
}
