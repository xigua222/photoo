package com.voicelike.app

import android.content.Context
import android.os.SystemClock
import com.voicelike.app.MediaInsightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated high-priority queue for current card batch visible metadata.
 * This queue is intentionally independent from full-catalog scheduling.
 */
object CardInsightPreloader {
    private const val LOOKAHEAD_LIMIT = 12
    private const val IMMEDIATE_PRIORITY_COUNT = 3
    private const val IMMEDIATE_ITEM_BUDGET_MS = 650L
    private const val LOOKAHEAD_ITEM_BUDGET_MS = 900L
    private const val LOOKAHEAD_ITEM_DELAY_MS = 70L
    private const val RECENT_COMPLETION_TTL_MS = 1600L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var job: Job? = null
    private val immediatePendingRequests = mutableListOf<PreloadRequest>()
    private val lookaheadPendingRequests = mutableListOf<PreloadRequest>()
    private val inFlightKeys = LinkedHashSet<String>()
    private val recentCompletedAtMs = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > 96
        }
    }

    private data class PreloadRequest(
        val context: Context,
        val item: MediaItem,
        val languageCode: String,
        val priority: Int,
        val rank: Int
    ) {
        val key: String = "${item.id}#$languageCode"
    }

    private fun hasResolvedPreloadLocation(location: String?): Boolean {
        return !location.isNullOrBlank() && !location.equals("Unknown Location", ignoreCase = true)
    }

    private fun isPreloadAlreadySatisfied(
        context: Context,
        item: MediaItem,
        languageCode: String
    ): Boolean {
        val snapshot = MediaInsightRepository.get(context, item.id) ?: return false
        return when (item.type) {
            "photo" -> {
                hasResolvedPreloadLocation(snapshot.location) &&
                    MediaAnalysisManager.hasCachedKeywordsForLanguage(snapshot, item.id, languageCode)
            }
            "video" -> hasResolvedPreloadLocation(snapshot.location)
            else -> true
        }
    }

    fun syncWindow(context: Context, items: List<MediaItem>, languageCode: String) {
        val batch = items
            .asSequence()
            .filter { it.type == "photo" || it.type == "video" }
            .take(LOOKAHEAD_LIMIT)
            .toList()
        if (batch.isEmpty()) return

        val appContext = context.applicationContext
        val now = SystemClock.elapsedRealtime()
        synchronized(lock) {
            pruneRecentCompletionsLocked(now)
            val windowRequests = batch.mapIndexedNotNull { index, item ->
                if (isPreloadAlreadySatisfied(appContext, item, languageCode)) {
                    return@mapIndexedNotNull null
                }
                val request = PreloadRequest(
                    context = appContext,
                    item = item,
                    languageCode = languageCode,
                    priority = if (index < IMMEDIATE_PRIORITY_COUNT) 0 else 1,
                    rank = index
                )
                if (inFlightKeys.contains(request.key)) return@mapIndexedNotNull null
                val completedAt = recentCompletedAtMs[request.key]
                if (completedAt != null && now - completedAt < RECENT_COMPLETION_TTL_MS) {
                    return@mapIndexedNotNull null
                }
                request
            }
            val liveKeys = windowRequests.mapTo(HashSet(maxOf(windowRequests.size, 16))) { it.key }
            immediatePendingRequests.removeAll { existing -> !liveKeys.contains(existing.key) }
            lookaheadPendingRequests.removeAll { existing -> !liveKeys.contains(existing.key) }
            immediatePendingRequests.clear()
            lookaheadPendingRequests.clear()
            windowRequests
                .sortedWith(compareBy<PreloadRequest> { it.rank })
                .forEach { request ->
                    if (request.priority == 0) {
                        immediatePendingRequests.add(request)
                    } else {
                        lookaheadPendingRequests.add(request)
                    }
                }

            if ((immediatePendingRequests.isNotEmpty() || lookaheadPendingRequests.isNotEmpty()) && job?.isActive != true) {
                job = scope.launch { drainQueue() }
            }
        }
    }

    private suspend fun drainQueue() {
        while (true) {
            val request = synchronized(lock) {
                val next = when {
                    immediatePendingRequests.isNotEmpty() -> immediatePendingRequests.removeAt(0)
                    lookaheadPendingRequests.isNotEmpty() -> lookaheadPendingRequests.removeAt(0)
                    else -> null
                }
                if (next == null) {
                    job = null
                    null
                } else {
                    inFlightKeys.add(next.key)
                    next
                }
            } ?: return

            try {
                if (request.priority > 0) {
                    delay(LOOKAHEAD_ITEM_DELAY_MS)
                }
                MediaAnalysisManager.prefetchUnifiedInsights(
                    context = request.context,
                    items = listOf(request.item),
                    languageCode = request.languageCode,
                    includeKeywords = true,
                    maxCount = 1,
                    timeBudgetMs = if (request.priority == 0) {
                        IMMEDIATE_ITEM_BUDGET_MS
                    } else {
                        LOOKAHEAD_ITEM_BUDGET_MS
                    },
                    respectGlobalPause = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Front-card warmup is best-effort and should never block UI.
            } finally {
                synchronized(lock) {
                    inFlightKeys.remove(request.key)
                    recentCompletedAtMs[request.key] = SystemClock.elapsedRealtime()
                    pruneRecentCompletionsLocked(SystemClock.elapsedRealtime())
                }
            }
        }
    }

    private fun pruneRecentCompletionsLocked(now: Long) {
        val iterator = recentCompletedAtMs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > RECENT_COMPLETION_TTL_MS) {
                iterator.remove()
            }
        }
    }
}
