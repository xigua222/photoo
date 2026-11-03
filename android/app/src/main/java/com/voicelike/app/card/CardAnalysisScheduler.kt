package com.voicelike.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object CardAnalysisScheduler {
    private const val MAX_CONCURRENT_ANALYSES = 2
    private const val MIN_INTERVAL_MS = 800L
    private const val MAX_CACHE_SIZE = 64

    private val semaphore = Semaphore(MAX_CONCURRENT_ANALYSES)
    private val lastAnalyzedAt = ConcurrentHashMap<Long, Long>()
    private val activeCount = AtomicInteger(0)

    fun shouldSkipAnalysis(itemId: Long): Boolean {
        val now = System.currentTimeMillis()
        val last = lastAnalyzedAt[itemId] ?: 0L
        val scaledInterval = AppPerformanceGovernor.scaledDelay(MIN_INTERVAL_MS)
        return now - last < scaledInterval
    }

    fun markAnalyzed(itemId: Long) {
        if (lastAnalyzedAt.size > MAX_CACHE_SIZE) {
            val cutoff = System.currentTimeMillis() - AppPerformanceGovernor.scaledDelay(60_000L)
            lastAnalyzedAt.entries.removeIf { it.value < cutoff }
        }
        lastAnalyzedAt[itemId] = System.currentTimeMillis()
    }

    suspend fun <T> runAnalysis(
        itemId: Long,
        block: suspend () -> T
    ): T? {
        if (shouldSkipAnalysis(itemId)) return null

        return try {
            withContext(Dispatchers.Default) {
                semaphore.withPermit {
                    if (shouldSkipAnalysis(itemId)) return@withPermit null
                    
                    activeCount.incrementAndGet()
                    try {
                        while (AppPerformanceGovernor.shouldPauseNonCriticalWork()) {
                            delay(AppPerformanceGovernor.scaledDelay(200))
                        }
                        val result = block()
                        markAnalyzed(itemId)
                        result
                    } finally {
                        activeCount.decrementAndGet()
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun activeAnalysisCount(): Int = activeCount.get()
}
