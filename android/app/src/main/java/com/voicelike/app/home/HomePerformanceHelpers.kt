package com.voicelike.app

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import coil.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.max

const val ENTRY_STACK_RENDER_COUNT = 6
const val STEADY_STACK_RENDER_COUNT = 3

private const val SWIPE_INTERACTION_COOLDOWN_MS = 450L
private const val UNDO_INTERACTION_COOLDOWN_MS = 600L
private const val IDLE_MEMORY_CLEANUP_DELAY_MS = 1200L
private const val IDLE_MEMORY_TRIM_HEADROOM_BYTES = 48L * 1024L * 1024L
private const val CRITICAL_IDLE_HEADROOM_BYTES = 28L * 1024L * 1024L
private const val INTERACTION_BURST_WINDOW_MS = 18_000L
private const val INTERACTION_BURST_MID_THRESHOLD = 8
private const val INTERACTION_BURST_HIGH_THRESHOLD = 16
private const val INTERACTION_BURST_MID_COOLDOWN_MS = 1800L
private const val INTERACTION_BURST_HIGH_COOLDOWN_MS = 4200L
private const val HOT_STATE_PERSIST_DEBOUNCE_MS = 1200L

class HomeInteractionPerfController(
    private val scope: CoroutineScope,
    private val imageLoader: ImageLoader,
    private val isUndoAnimating: () -> Boolean,
    private val hasUndoAvailable: () -> Boolean,
    private val isFullScreenVideoOpen: () -> Boolean
) {
    var interactionCooldownUntilMs by mutableLongStateOf(0L)
        private set

    var stableEpoch by mutableIntStateOf(0)
        private set

    private var cooldownJob: Job? = null
    private var cleanupJob: Job? = null
    private var lowHeadroomIdleChecks = 0
    private val recentInteractionAtMs = ArrayDeque<Long>()

    fun onDragStart() {
        cleanupJob?.cancel()
        cleanupJob = null
    }

    fun onSwipeCommitted() {
        armCooldown(SWIPE_INTERACTION_COOLDOWN_MS)
        scheduleIdleMemoryCleanup()
    }

    fun onUndoRestored() {
        armCooldown(UNDO_INTERACTION_COOLDOWN_MS)
        scheduleIdleMemoryCleanup()
    }

    fun shouldDeferNonCriticalWork(nowMs: Long = SystemClock.elapsedRealtime()): Boolean {
        return nowMs < interactionCooldownUntilMs
    }

    fun cancel() {
        cooldownJob?.cancel()
        cooldownJob = null
        cleanupJob?.cancel()
        cleanupJob = null
    }

    private fun armCooldown(durationMs: Long) {
        val now = SystemClock.elapsedRealtime()
        val resolvedDurationMs = resolveInteractionCooldownDuration(now, durationMs)
        val target = max(interactionCooldownUntilMs, now + resolvedDurationMs)
        interactionCooldownUntilMs = target
        AppPerformanceGovernor.deferNonCriticalWorkFor(resolvedDurationMs)
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            val remaining = (target - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            if (remaining > 0L) delay(remaining)
            if (interactionCooldownUntilMs == target) {
                stableEpoch += 1
            }
        }
    }

    private fun resolveInteractionCooldownDuration(nowMs: Long, baseDurationMs: Long): Long {
        while (recentInteractionAtMs.isNotEmpty() && nowMs - recentInteractionAtMs.first() > INTERACTION_BURST_WINDOW_MS) {
            recentInteractionAtMs.removeFirst()
        }
        recentInteractionAtMs.addLast(nowMs)
        val burstCooldownMs = when {
            recentInteractionAtMs.size >= INTERACTION_BURST_HIGH_THRESHOLD -> INTERACTION_BURST_HIGH_COOLDOWN_MS
            recentInteractionAtMs.size >= INTERACTION_BURST_MID_THRESHOLD -> INTERACTION_BURST_MID_COOLDOWN_MS
            else -> 0L
        }
        return max(baseDurationMs, burstCooldownMs)
    }

    private fun scheduleIdleMemoryCleanup() {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            delay(IDLE_MEMORY_CLEANUP_DELAY_MS)
            if (shouldDeferNonCriticalWork()) return@launch
            if (isUndoAnimating()) return@launch
            if (isFullScreenVideoOpen()) return@launch

            val runtime = Runtime.getRuntime()
            val used = runtime.totalMemory() - runtime.freeMemory()
            val headroom = runtime.maxMemory() - used
            if (headroom >= IDLE_MEMORY_TRIM_HEADROOM_BYTES) {
                lowHeadroomIdleChecks = 0
                return@launch
            }

            runCatching { MediaAnalysisManager.trimTransientCaches() }
            runCatching { MotionPhotoHelper.trimTransientCaches() }

            val preserveUndoHotState =
                hasUndoAvailable() && headroom > CRITICAL_IDLE_HEADROOM_BYTES
            if (preserveUndoHotState) {
                lowHeadroomIdleChecks = 0
                return@launch
            }

            runCatching { VideoPlayerCache.trimIdlePlayers() }

            if (headroom <= CRITICAL_IDLE_HEADROOM_BYTES && lowHeadroomIdleChecks >= 2) {
                withContext(Dispatchers.Main.immediate) {
                    runCatching { imageLoader.memoryCache?.clear() }
                }
                lowHeadroomIdleChecks = 0
            } else {
                lowHeadroomIdleChecks += 1
            }
        }
    }
}

data class HomeUndoPersistenceSnapshot(
    val persistProcessedIds: Boolean,
    val persistPendingTrashIds: Boolean,
    val persistLikedIds: Boolean,
    val persistMovedIds: Boolean,
    val folderUsage: Map<String, Int>?,
    val stats: AppStats?
)

class HomeStatePersistenceWriter(
    private val prefs: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var pendingSnapshot: HomeUndoPersistenceSnapshot? = null

    fun schedule(
        snapshot: HomeUndoPersistenceSnapshot,
        delayMs: Long = HOT_STATE_PERSIST_DEBOUNCE_MS
    ) {
        pendingSnapshot = mergeSnapshots(pendingSnapshot, snapshot)
        job?.cancel()
        job = scope.launch {
            delay(delayMs.coerceAtLeast(HOT_STATE_PERSIST_DEBOUNCE_MS))
            persistPendingSnapshot()
        }
    }

    fun flush() {
        job?.cancel()
        runBlocking(Dispatchers.IO) {
            persistPendingSnapshot()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        scope.cancel()
    }

    private fun persistPendingSnapshot() {
        val snapshot = pendingSnapshot ?: return
        pendingSnapshot = null
        prefs.saveHotStateSnapshot(
            processedIds = if (snapshot.persistProcessedIds) prefs.processedIds else null,
            pendingTrashIds = if (snapshot.persistPendingTrashIds) prefs.pendingTrashIds else null,
            likedIds = if (snapshot.persistLikedIds) prefs.likedIds else null,
            movedIds = if (snapshot.persistMovedIds) prefs.movedIds else null,
            folderUsage = snapshot.folderUsage,
            stats = snapshot.stats
        )
    }

    private fun mergeSnapshots(
        existing: HomeUndoPersistenceSnapshot?,
        incoming: HomeUndoPersistenceSnapshot
    ): HomeUndoPersistenceSnapshot {
        if (existing == null) {
            return incoming
        }
        return HomeUndoPersistenceSnapshot(
            persistProcessedIds = incoming.persistProcessedIds || existing.persistProcessedIds,
            persistPendingTrashIds = incoming.persistPendingTrashIds || existing.persistPendingTrashIds,
            persistLikedIds = incoming.persistLikedIds || existing.persistLikedIds,
            persistMovedIds = incoming.persistMovedIds || existing.persistMovedIds,
            folderUsage = incoming.folderUsage ?: existing.folderUsage,
            stats = incoming.stats ?: existing.stats
        )
    }
}
