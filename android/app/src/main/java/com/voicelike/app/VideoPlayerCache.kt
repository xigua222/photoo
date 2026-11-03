@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.voicelike.app

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Centralized player engine:
 * - Keep up to 2 core players (active + warm) to reduce codec churn.
 * - If both are busy, create a temporary fallback player and release it immediately on return.
 * - All operations are serialized by an internal lock to avoid lifecycle races.
 */
object VideoPlayerCache {
    private data class CoreSlot(
        val player: ExoPlayer,
        var inUse: Boolean = false,
        var lastReleasedAtMs: Long = 0L,
        var cleanupJob: Job? = null
    )

    private val lock = Any()
    private val coreSlots = mutableListOf<CoreSlot>()
    private val transientPlayers = HashSet<ExoPlayer>()
    @Volatile
    private var cacheGeneration = 0
    @Volatile
    private var conservativeMode = false
    // 动态调整池大小：正常2个，内存紧张时降到1个
    private const val CORE_POOL_SIZE_NORMAL = 2
    private const val CORE_POOL_SIZE_LOW_MEMORY = 1
    private const val TARGET_BUFFER_BYTES_CONSERVATIVE = 6 * 1024 * 1024
    private const val TARGET_BUFFER_BYTES = 10 * 1024 * 1024  // 平衡：10MB
    private const val LOW_HEADROOM_BYTES = 48L * 1024L * 1024L  // 提高内存压力阈值
    
    private fun getTargetPoolSize(): Int {
        return if (conservativeMode || isMemoryTight()) CORE_POOL_SIZE_LOW_MEMORY else CORE_POOL_SIZE_NORMAL
    }
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun generation(): Int = cacheGeneration

    fun get(context: Context): ExoPlayer {
        conservativeMode = MemorySafetyMode.shouldUseConservativeMemory(context.applicationContext)
        synchronized(lock) {
            trimCorePoolIfNeeded()
            val reusable = coreSlots.firstOrNull { !it.inUse }
            if (reusable != null) {
                markInUse(reusable)
                return reusable.player
            }

            val targetSize = getTargetPoolSize()
            if (coreSlots.size < targetSize) {
                val slot = CoreSlot(player = buildPlayer(context), inUse = true)
                coreSlots.add(slot)
                return slot.player
            }

            // 内存紧张时，创建临时播放器而不是复用
            val temporary = buildPlayer(context)
            transientPlayers.add(temporary)
            return temporary
        }
    }

    fun release(player: ExoPlayer, delayMs: Long = 0L) {
        synchronized(lock) {
            if (transientPlayers.remove(player)) {
                releaseTransient(player)
                return
            }

            val slot = coreSlots.firstOrNull { it.player === player }
            if (slot == null) {
                // Unknown player, fail-safe release.
                runCatching { player.release() }
                return
            }

            if (!slot.inUse) return
            slot.inUse = false
            slot.lastReleasedAtMs = SystemClock.elapsedRealtime()
            lightweightDetach(player)

            slot.cleanupJob?.cancel()
            slot.cleanupJob = null
            
            // 智能清理策略：内存紧张时立即清理，否则短延迟以保持流畅
            val shouldDelayCleanup = delayMs > 0L && !isMemoryTight()
            if (shouldDelayCleanup) {
                // 缩短延迟时间到 150ms（从 320ms），平衡流畅度和内存
                val actualDelay = minOf(delayMs, 150L)
                slot.cleanupJob = releaseScope.launch {
                    delay(actualDelay)
                    val shouldCleanup = synchronized(lock) {
                        val stillIdle = !slot.inUse
                        if (stillIdle) {
                            slot.cleanupJob = null
                        }
                        stillIdle
                    }
                    if (shouldCleanup) {
                        heavyReset(player)
                    }
                }
            } else {
                heavyReset(player)
            }
            trimCorePoolIfNeeded()
        }
    }

    private fun markInUse(slot: CoreSlot) {
        slot.cleanupJob?.cancel()
        slot.cleanupJob = null
        lightweightDetach(slot.player)
        heavyReset(slot.player)
        slot.player.repeatMode = Player.REPEAT_MODE_ONE
        slot.inUse = true
    }

    private fun lightweightDetach(player: ExoPlayer) {
        player.pause()
        player.playWhenReady = false
        player.clearVideoSurface()
    }

    private fun heavyReset(player: ExoPlayer) {
        runCatching {
            player.stop()
            player.clearMediaItems()
        }
    }

    private fun buildPlayer(context: Context): ExoPlayer {
        // 根据内存状态动态调整缓冲参数
        val isLowMemory = isMemoryTight() || MemorySafetyMode.shouldUseConservativeMemory(context.applicationContext)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ if (isLowMemory) 1000 else 1200,
                /* maxBufferMs = */ if (isLowMemory) 3000 else 4000,
                /* bufferForPlaybackMs = */ 200,
                /* bufferForPlaybackAfterRebufferMs = */ 400
            )
            .setBackBuffer(/* backBufferDurationMs = */ 0, /* retainBackBufferFromKeyframe = */ false)
            .setTargetBufferBytes(if (isLowMemory) TARGET_BUFFER_BYTES_CONSERVATIVE else TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(isLowMemory)  // 低内存时优先时间
            .build()
        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private fun isMemoryTight(): Boolean {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val headroom = runtime.maxMemory() - used
        return headroom <= LOW_HEADROOM_BYTES
    }

    private fun trimCorePoolIfNeeded() {
        val targetSize = getTargetPoolSize()
        // 只在超过目标大小且内存紧张时才裁剪
        if (coreSlots.size <= targetSize) return
        if (!isMemoryTight() && !conservativeMode) return

        val removable = coreSlots.firstOrNull { !it.inUse } ?: return
        removable.cleanupJob?.cancel()
        removable.cleanupJob = null
        runCatching {
            removable.player.stop()
            removable.player.clearMediaItems()
            removable.player.clearVideoSurface()
            removable.player.release()
        }
        coreSlots.remove(removable)
    }

    private fun releaseTransient(player: ExoPlayer) {
        runCatching {
            player.stop()
            player.clearMediaItems()
            player.clearVideoSurface()
            player.release()
        }
    }

    fun clear() {
        synchronized(lock) {
            cacheGeneration += 1
            conservativeMode = false
            coreSlots.forEach { slot ->
                slot.cleanupJob?.cancel()
                slot.cleanupJob = null
                runCatching {
                    slot.player.stop()
                    slot.player.clearMediaItems()
                    slot.player.clearVideoSurface()
                    slot.player.release()
                }
            }
            coreSlots.clear()

            transientPlayers.forEach { player ->
                runCatching {
                    player.stop()
                    player.clearMediaItems()
                    player.clearVideoSurface()
                    player.release()
                }
            }
            transientPlayers.clear()
        }
    }

    fun trimToMinimum() {
        synchronized(lock) {
            conservativeMode = true

            transientPlayers.forEach { player ->
                releaseTransient(player)
            }
            transientPlayers.clear()

            while (coreSlots.size > CORE_POOL_SIZE_LOW_MEMORY) {
                val removable = coreSlots.firstOrNull { !it.inUse } ?: break
                removable.cleanupJob?.cancel()
                removable.cleanupJob = null
                runCatching {
                    removable.player.stop()
                    removable.player.clearMediaItems()
                    removable.player.clearVideoSurface()
                    removable.player.release()
                }
                coreSlots.remove(removable)
            }
        }
    }

    fun trimIdlePlayers() {
        synchronized(lock) {
            val idleSlots = coreSlots
                .filter { !it.inUse }
                .sortedBy { it.lastReleasedAtMs }
            idleSlots.forEach { slot ->
                slot.cleanupJob?.cancel()
                slot.cleanupJob = null
                runCatching {
                    slot.player.stop()
                    slot.player.clearMediaItems()
                    slot.player.clearVideoSurface()
                    slot.player.release()
                }
                coreSlots.remove(slot)
            }
        }
    }
}
