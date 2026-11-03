package com.voicelike.app

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.TextureView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters

private const val DRAG_UNFREEZE_RESUME_DELAY_MS = 120L
private const val INLINE_TEXTURE_REVEAL_DELAY_MS = 32L

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    item: MediaItem,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    shouldPlay: Boolean = true,
    repeatOne: Boolean = true,
    isCardDragging: Boolean = false,
    freezeFrame: Boolean = false,
    playerResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    onSeekStateChange: ((Boolean) -> Unit)? = null,
    onFullScreen: ((ExoPlayer) -> Unit)? = null,
    isFullScreenOpen: Boolean = false,
    pauseToken: Long = 0L,
    replayToken: Long = 0L,
    preferFullScreenSeekBar: Boolean = false,
    preferIntentBasedResumeAfterScrub: Boolean = false,
    controlsEnabled: Boolean = true,
    onStartupSample: ((Long) -> Unit)? = null,
    onPlaybackEnded: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isReady by remember(item.id) { mutableStateOf(false) } // Track if video is ready to render
    var isPlaying by remember(item.id) { mutableStateOf(false) }
    var hasFirstFrame by remember(item.id) { mutableStateOf(false) }
    var isInlineTextureVisible by remember(item.id) { mutableStateOf(false) }
    var pendingInlineTextureRevealToken by remember(item.id) { mutableLongStateOf(0L) }
    
    // Progress State
    var currentTime by remember(item.id) { mutableLongStateOf(0L) }
    var duration by remember(item.id) { mutableLongStateOf(0L) }
    var preparedItemId by remember(item.id) { mutableStateOf<Long?>(null) }
    var userPausedByTap by remember(item.id) { mutableStateOf(false) }
    var pausedByDrag by remember(item.id) { mutableStateOf(false) }
    var resumeAfterDrag by remember(item.id) { mutableStateOf(false) }
    var isFrozenByDrag by remember(item.id) { mutableStateOf(false) }
    var waitFirstFrameAfterUnfreeze by remember(item.id) { mutableStateOf(false) }
    var unfreezeResumeStartMs by remember(item.id) { mutableLongStateOf(0L) }
    var prevShouldPlay by remember(item.id) { mutableStateOf(false) }
    var pendingStartupArm by remember(item.id) { mutableStateOf(false) }
    var startupPlayStartMs by remember(item.id) { mutableLongStateOf(0L) }
    var awaitingStartupSample by remember(item.id) { mutableStateOf(false) }
    var resumeAfterLifecycleStop by remember(item.id) { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isUserInteracting = isDragged || isPressed
    var isSeeking by remember { mutableStateOf(false) } // Explicit seek state for clean separation
    var lastSeekReport by remember { mutableStateOf<Boolean?>(null) }
    var inlineScrubFraction by remember { mutableStateOf<Float?>(null) }
    var isInlineScrubbing by remember { mutableStateOf(false) }
    var resumeAfterInlineScrub by remember { mutableStateOf(false) }
    var isLongPressSpeedActive by remember(item.id) { mutableStateOf(false) }
    var longPressSpeedShouldResumeOnRelease by remember(item.id) { mutableStateOf(false) }
    var longPressSpeedShouldRestoreUserPause by remember(item.id) { mutableStateOf(false) }

    val isVideo = item.type == "video"
    val areInlineControlsEnabled = controlsEnabled && !freezeFrame && !isCardDragging

    // Report seek/press state immediately so parent card drag can be disabled
    LaunchedEffect(isUserInteracting, isSeeking, onSeekStateChange) {
        val shouldBlockParentDrag = isUserInteracting || isSeeking
        if (lastSeekReport != shouldBlockParentDrag) {
            onSeekStateChange?.invoke(shouldBlockParentDrag)
            lastSeekReport = shouldBlockParentDrag
        }
    }

    DisposableEffect(onSeekStateChange) {
        onDispose {
            onSeekStateChange?.invoke(false)
        }
    }

    var playerGeneration by remember { mutableIntStateOf(VideoPlayerCache.generation()) }
    val exoPlayer = remember(playerGeneration) { VideoPlayerCache.get(context) }
    val textureViewState = remember { mutableStateOf<TextureView?>(null) }
    val surfaceFrameState = remember { mutableStateOf<AspectRatioFrameLayout?>(null) }
    var boundTextureView by remember { mutableStateOf<TextureView?>(null) }
    var videoAspectRatio by remember(item.id) {
        mutableFloatStateOf(
            if (item.width > 0 && item.height > 0) {
                item.width.toFloat() / item.height.toFloat()
            } else {
                0f
            }
        )
    }
    var extractedMotionVideoUri by remember(item.id) { mutableStateOf<Uri?>(null) }
    val currentItemId by rememberUpdatedState(item.id)
    val currentPreparedItemId by rememberUpdatedState(preparedItemId)
    val currentShouldPlay by rememberUpdatedState(shouldPlay)
    val currentUserPausedByTap by rememberUpdatedState(userPausedByTap)
    val currentIsFullScreenOpen by rememberUpdatedState(isFullScreenOpen)
    val currentIsFrozenByDrag by rememberUpdatedState(isFrozenByDrag)
    val frozenCoverRequest = rememberHomeCardVideoFrameImageRequest(
        itemId = item.id,
        uri = item.uri,
        crossfade = false
    )
    fun debugFreezeLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("VideoPlayerFreeze", "item=${item.id} $message")
        }
    }
    fun updateInlineTextureVisibility(textureView: TextureView?, visible: Boolean) {
        textureView?.alpha = if (visible) 1f else 0f
    }
    fun bindInlineTextureView(textureView: TextureView?, force: Boolean = false) {
        if (textureView == null || !textureView.isAvailable || isFullScreenOpen || isFrozenByDrag) return
        updateInlineTextureVisibility(textureView, isInlineTextureVisible)
        if (force || boundTextureView !== textureView) {
            exoPlayer.setVideoTextureView(textureView)
            boundTextureView = textureView
        }
    }
    fun clearInlineTextureView(textureView: TextureView?) {
        updateInlineTextureVisibility(textureView, false)
        if (textureView != null && boundTextureView === textureView) {
            exoPlayer.clearVideoTextureView(textureView)
            boundTextureView = null
        }
    }
    val currentBindInlineTextureView by rememberUpdatedState<(TextureView?, Boolean) -> Unit> { textureView, force ->
        bindInlineTextureView(textureView, force)
    }
    val currentClearInlineTextureView by rememberUpdatedState<(TextureView?) -> Unit> { textureView ->
        clearInlineTextureView(textureView)
    }
    fun isPreparedForCurrentItem(): Boolean {
        return exoPlayer.currentMediaItem?.mediaId == currentItemId.toString()
    }
    fun shouldAutoResumeAfterLifecycleStop(): Boolean {
        return currentShouldPlay &&
            !currentUserPausedByTap &&
            !currentIsFullScreenOpen &&
            !currentIsFrozenByDrag &&
            currentPreparedItemId == currentItemId
    }
    fun clearInlineLongPressSpeed(restorePlaybackState: Boolean) {
        if (!isLongPressSpeedActive && !restorePlaybackState) return
        exoPlayer.setPlaybackSpeed(1f)
        val shouldResume = longPressSpeedShouldResumeOnRelease
        val shouldRestoreUserPause = longPressSpeedShouldRestoreUserPause
        isLongPressSpeedActive = false
        longPressSpeedShouldResumeOnRelease = false
        longPressSpeedShouldRestoreUserPause = false
        if (!restorePlaybackState) return
        userPausedByTap = shouldRestoreUserPause
        if (shouldResume) {
            pausedByDrag = false
            if (!isFullScreenOpen) {
                bindInlineTextureView(textureViewState.value)
            }
            exoPlayer.playWhenReady = true
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
            }
        } else {
            exoPlayer.pause()
            exoPlayer.playWhenReady = false
        }
    }

    LaunchedEffect(exoPlayer) {
        preparedItemId = null
        isReady = false
        hasFirstFrame = false
        isInlineTextureVisible = false
        pendingInlineTextureRevealToken = 0L
        duration = 0L
        currentTime = 0L
        boundTextureView = null
        updateInlineTextureVisibility(textureViewState.value, false)
        clearInlineLongPressSpeed(restorePlaybackState = false)
    }

    // Pause analysis when video is playing
    DisposableEffect(shouldPlay) {
        if (shouldPlay) {
            MediaAnalysisManager.requestPause()
        }
        onDispose {
            if (shouldPlay) {
                MediaAnalysisManager.releasePause()
            }
        }
    }

    LaunchedEffect(item.id, shouldPlay, exoPlayer) {
        if (!shouldPlay) {
            exoPlayer.pause()
            exoPlayer.playWhenReady = false
            // Only stop if item changed or hard stop
            if (preparedItemId != null && !isFullScreenOpen && preparedItemId != item.id) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                preparedItemId = null
                isReady = false
                hasFirstFrame = false
                isInlineTextureVisible = false
                pendingInlineTextureRevealToken = 0L
                duration = 0L
            }
            return@LaunchedEffect
        }
        if (preparedItemId != item.id) {
            try {
                isReady = false
                hasFirstFrame = false
                isInlineTextureVisible = false
                pendingInlineTextureRevealToken = 0L
                clearInlineLongPressSpeed(restorePlaybackState = false)
                videoAspectRatio = if (item.width > 0 && item.height > 0) {
                    item.width.toFloat() / item.height.toFloat()
                } else {
                    0f
                }
                updateInlineTextureVisibility(textureViewState.value, false)
                clearInlineTextureView(textureViewState.value)
                
                var videoUri = item.uri
                var mimeType = item.mimeType

                if (item.isMotionPhoto) {
                    val cachedUri = extractedMotionVideoUri
                    if (cachedUri != null) {
                        videoUri = cachedUri
                        mimeType = "video/mp4"
                    } else {
                        val file = withContext(Dispatchers.IO) {
                            MotionPhotoHelper.extractVideo(
                                context,
                                item.uri,
                                MotionPhotoHelper.MotionPhotoInfo(true, item.motionPhotoVideoOffset, item.motionPhotoVideoLength)
                            )
                        }
                        if (file != null) {
                            videoUri = Uri.fromFile(file)
                            mimeType = "video/mp4"
                            extractedMotionVideoUri = videoUri
                        }
                    }
                }

                val mediaItem = ExoMediaItem.Builder()
                    .setMediaId(item.id.toString())
                    .setUri(videoUri)
                    .setMimeType(mimeType)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                preparedItemId = item.id
                if (!isFullScreenOpen && !isFrozenByDrag) {
                    bindInlineTextureView(textureViewState.value)
                }
                if (shouldPlay && !userPausedByTap && !pausedByDrag && !isFullScreenOpen && !isFrozenByDrag) {
                    exoPlayer.playWhenReady = true
                    if (!exoPlayer.isPlaying) {
                        exoPlayer.play()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(repeatOne) {
        exoPlayer.repeatMode = if (repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    LaunchedEffect(replayToken) {
        if (replayToken > 0L) {
            userPausedByTap = false
            pausedByDrag = false
            exoPlayer.seekTo(0L)
            if (shouldPlay) {
                exoPlayer.playWhenReady = true
                if (!exoPlayer.isPlaying) {
                    exoPlayer.play()
                }
            }
        }
    }

    LaunchedEffect(pauseToken) {
        if (pauseToken > 0L) {
            clearInlineLongPressSpeed(restorePlaybackState = false)
            pausedByDrag = true
            exoPlayer.pause()
            exoPlayer.playWhenReady = false
        }
    }

    fun resumePlayback() {
        if (!isFullScreenOpen) {
            bindInlineTextureView(textureViewState.value)
        }
        exoPlayer.playWhenReady = true
        if (!exoPlayer.isPlaying) {
            exoPlayer.play()
        }
    }
    fun resumePlaybackIfSurfaceReady() {
        val textureView = textureViewState.value
        if (textureView == null || !textureView.isAvailable || !shouldAutoResumeAfterLifecycleStop()) return
        currentBindInlineTextureView(textureView, true)
        resumePlayback()
        resumeAfterLifecycleStop = false
    }

    LaunchedEffect(freezeFrame, isFullScreenOpen, shouldPlay, userPausedByTap) {
        if (isFullScreenOpen) return@LaunchedEffect
        if (freezeFrame) {
            if (isFrozenByDrag) return@LaunchedEffect
            clearInlineLongPressSpeed(restorePlaybackState = false)
            resumeAfterDrag = shouldPlay && !userPausedByTap && exoPlayer.isPlaying
            pausedByDrag = true
            isFrozenByDrag = true
            unfreezeResumeStartMs = 0L
            exoPlayer.pause()
            exoPlayer.playWhenReady = false
            debugFreezeLog("drag_freeze_enter resume_after_drag=$resumeAfterDrag")
        } else if (isFrozenByDrag) {
            isFrozenByDrag = false
            if (resumeAfterDrag && shouldPlay && !userPausedByTap) {
                delay(DRAG_UNFREEZE_RESUME_DELAY_MS)
                // Re-check after settle delay to avoid stale resume during rapid state transitions.
                if (!shouldPlay || userPausedByTap || isFullScreenOpen || freezeFrame) {
                    exoPlayer.pause()
                    exoPlayer.playWhenReady = false
                    unfreezeResumeStartMs = 0L
                    pausedByDrag = false
                    resumeAfterDrag = false
                    debugFreezeLog("drag_freeze_exit resume_after_drag=cancelled_after_delay")
                    return@LaunchedEffect
                }
                unfreezeResumeStartMs = SystemClock.elapsedRealtime()
                resumePlayback()
                debugFreezeLog("drag_freeze_exit resume_after_drag=true delay_ms=$DRAG_UNFREEZE_RESUME_DELAY_MS")
            } else {
                exoPlayer.pause()
                exoPlayer.playWhenReady = false
                unfreezeResumeStartMs = 0L
                debugFreezeLog("drag_freeze_exit resume_after_drag=false")
            }
            pausedByDrag = false
            resumeAfterDrag = false
        }
    }

    LaunchedEffect(shouldPlay) {
        if (shouldPlay && !prevShouldPlay) {
            pendingStartupArm = true
        }
        if (!shouldPlay) {
            clearInlineLongPressSpeed(restorePlaybackState = false)
            pendingStartupArm = false
            awaitingStartupSample = false
            startupPlayStartMs = 0L
        }
        prevShouldPlay = shouldPlay
    }

    LaunchedEffect(hasFirstFrame, pendingInlineTextureRevealToken, isFullScreenOpen, isFrozenByDrag, exoPlayer) {
        if (!hasFirstFrame || pendingInlineTextureRevealToken == 0L) return@LaunchedEffect
        if (isFullScreenOpen || isFrozenByDrag) return@LaunchedEffect
        delay(INLINE_TEXTURE_REVEAL_DELAY_MS)
        if (
            hasFirstFrame &&
            pendingInlineTextureRevealToken != 0L &&
            isPreparedForCurrentItem() &&
            !isFullScreenOpen &&
            !isFrozenByDrag
        ) {
            isInlineTextureVisible = true
            updateInlineTextureVisibility(textureViewState.value, true)
        }
    }

    LaunchedEffect(shouldPlay, pendingStartupArm, preparedItemId, isFrozenByDrag, isFullScreenOpen, hasFirstFrame, freezeFrame, isCardDragging) {
        if (!shouldPlay || !pendingStartupArm) return@LaunchedEffect
        if (preparedItemId != item.id || isFrozenByDrag || isFullScreenOpen || freezeFrame || isCardDragging) return@LaunchedEffect

        startupPlayStartMs = SystemClock.elapsedRealtime()
        awaitingStartupSample = true
        pendingStartupArm = false

        // If first frame is already available, sample immediately.
        if (hasFirstFrame && startupPlayStartMs > 0L) {
            val elapsed = (SystemClock.elapsedRealtime() - startupPlayStartMs).coerceAtLeast(0L)
            onStartupSample?.invoke(elapsed)
            awaitingStartupSample = false
            startupPlayStartMs = 0L
        }
    }

    LaunchedEffect(isFullScreenOpen, shouldPlay, userPausedByTap, pausedByDrag) {
        if (!isFullScreenOpen) {
            // Immediately sync progress when returning from full screen
            currentTime = exoPlayer.currentPosition

            if (!isFrozenByDrag) {
                bindInlineTextureView(textureViewState.value)
            }
            if (shouldPlay && !userPausedByTap && !pausedByDrag) {
                resumePlayback()
            } else {
                exoPlayer.pause()
                exoPlayer.playWhenReady = false
            }
        } else {
            // Hand over rendering ownership to fullscreen player view.
            clearInlineTextureView(textureViewState.value)
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val shouldResume = shouldAutoResumeAfterLifecycleStop() &&
                        (exoPlayer.isPlaying || exoPlayer.playWhenReady)
                    resumeAfterLifecycleStop = shouldResume
                    if (shouldResume) {
                        exoPlayer.pause()
                        exoPlayer.playWhenReady = false
                    }
                }

                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> {
                    val cacheGeneration = VideoPlayerCache.generation()
                    if (cacheGeneration != playerGeneration) {
                        playerGeneration = cacheGeneration
                        return@LifecycleEventObserver
                    }
                    if (resumeAfterLifecycleStop) {
                        resumePlaybackIfSurfaceReady()
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Progress Loop
    if (isVideo) {
        val shouldTickProgress = !isFullScreenOpen && !isCardDragging && !freezeFrame
        LaunchedEffect(shouldTickProgress) {
            if (!shouldTickProgress) return@LaunchedEffect
            while (isActive) {
                try {
                    // Keep progress updates out of drag/settle window to reduce recomposition jank.
                    if (!isUserInteracting && !isSeeking) {
                        currentTime = exoPlayer.currentPosition
                    }
                } catch (e: Exception) {
                    // Ignore errors if player is released
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        exoPlayer.setSeekParameters(SeekParameters.EXACT)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!isPreparedForCurrentItem()) return
                if (playbackState == Player.STATE_READY) {
                    isReady = true
                    duration = exoPlayer.duration.coerceAtLeast(0)
                } else if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded?.invoke()
                }
            }

            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onRenderedFirstFrame() {
                if (!isPreparedForCurrentItem()) return
                hasFirstFrame = true
                pendingInlineTextureRevealToken = SystemClock.elapsedRealtime()
                if (!isInlineTextureVisible) {
                    updateInlineTextureVisibility(textureViewState.value, false)
                }
                if (unfreezeResumeStartMs > 0L) {
                    val elapsed = SystemClock.elapsedRealtime() - unfreezeResumeStartMs
                    debugFreezeLog("first_frame_restore_ms=$elapsed")
                    unfreezeResumeStartMs = 0L
                }
                if (awaitingStartupSample && startupPlayStartMs > 0L) {
                    val elapsed = (SystemClock.elapsedRealtime() - startupPlayStartMs).coerceAtLeast(0L)
                    onStartupSample?.invoke(elapsed)
                    awaitingStartupSample = false
                    startupPlayStartMs = 0L
                }
            }
            
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    val ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
                    videoAspectRatio = ratio
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            clearInlineLongPressSpeed(restorePlaybackState = false)
            clearInlineTextureView(textureViewState.value)
            boundTextureView = null
            exoPlayer.removeListener(listener)
            // 短延迟以保持滑动流畅，但在内存紧张时会立即清理
            VideoPlayerCache.release(exoPlayer, delayMs = 150L)
        }
    }

    Box(modifier.fillMaxSize()) {
        val showFrozenCover = !isInlineTextureVisible
        AndroidView(
            factory = {
                val frame = AspectRatioFrameLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setResizeMode(playerResizeMode)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    if (videoAspectRatio > 0f) {
                        setAspectRatio(videoAspectRatio)
                    }
                }
                val textureView = TextureView(context)
                textureView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                textureView.isOpaque = false
                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        boundTextureView = null
                        updateInlineTextureVisibility(textureView, false)
                        currentBindInlineTextureView(textureView, true)
                        if (resumeAfterLifecycleStop) {
                            resumePlaybackIfSurfaceReady()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        hasFirstFrame = false
                        isInlineTextureVisible = false
                        pendingInlineTextureRevealToken = 0L
                        updateInlineTextureVisibility(textureView, false)
                        currentClearInlineTextureView(textureView)
                        return true
                    }
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
                frame.addView(textureView)
                updateInlineTextureVisibility(textureView, false)
                if (!isFullScreenOpen && !isFrozenByDrag) {
                    bindInlineTextureView(textureView, force = true)
                }
                textureViewState.value = textureView
                surfaceFrameState.value = frame
                frame
            },
                update = { frame ->
                frame.setResizeMode(playerResizeMode)
                if (videoAspectRatio > 0f) {
                    frame.setAspectRatio(videoAspectRatio)
                }
                val textureView = (frame.getChildAt(0) as? TextureView)
                if (textureView != null) {
                    if (textureViewState.value !== textureView) {
                        textureViewState.value = textureView
                    }
                    updateInlineTextureVisibility(textureView, isInlineTextureVisible)
                    if (!isFullScreenOpen && !isFrozenByDrag) {
                        bindInlineTextureView(textureView)
                    }
                }
                surfaceFrameState.value = frame
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = if (isInlineTextureVisible) 1f else 0f)
        )
        if (showFrozenCover) {
            val coverScale = when (playerResizeMode) {
                AspectRatioFrameLayout.RESIZE_MODE_FIT -> ContentScale.Fit
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> ContentScale.Crop
                AspectRatioFrameLayout.RESIZE_MODE_FILL -> ContentScale.FillBounds
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> ContentScale.FillWidth
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> ContentScale.FillHeight
                else -> ContentScale.Crop
            }
            AsyncImage(
                model = frozenCoverRequest,
                contentDescription = null,
                contentScale = coverScale,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Controls for Video Type
        if (isVideo) {
            // Gradient Overlay for visibility
            if (isReady) {
                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(120.dp)
                         .align(Alignment.BottomCenter)
                         .background(
                             Brush.verticalGradient(
                                 colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                             )
                         )
                 )
            }

            Box(
                modifier = if (areInlineControlsEnabled) {
                    Modifier
                        .fillMaxSize()
                        .pointerInput(item.id, isSeeking, isUserInteracting, isFullScreenOpen, freezeFrame) {
                            detectTapGestures(
                                onPress = {
                                    val released = tryAwaitRelease()
                                    if (isLongPressSpeedActive) {
                                        clearInlineLongPressSpeed(restorePlaybackState = true)
                                    } else if (!released) {
                                        clearInlineLongPressSpeed(restorePlaybackState = false)
                                    }
                                },
                                onLongPress = {
                                    if (
                                        isSeeking ||
                                        isUserInteracting ||
                                        isFullScreenOpen ||
                                        freezeFrame ||
                                        !isPreparedForCurrentItem()
                                    ) {
                                        return@detectTapGestures
                                    }
                                    longPressSpeedShouldResumeOnRelease =
                                        shouldPlay && !userPausedByTap && !pausedByDrag
                                    longPressSpeedShouldRestoreUserPause = userPausedByTap
                                    userPausedByTap = false
                                    pausedByDrag = false
                                    isLongPressSpeedActive = true
                                    exoPlayer.setPlaybackSpeed(2f)
                                    resumePlayback()
                                },
                                onTap = {
                                    if (!isSeeking && !isUserInteracting) {
                                        clearInlineLongPressSpeed(restorePlaybackState = false)
                                        if (isPlaying) {
                                            userPausedByTap = true
                                            exoPlayer.pause()
                                            exoPlayer.playWhenReady = false
                                        } else {
                                            userPausedByTap = false
                                            pausedByDrag = false
                                            resumePlayback()
                                        }
                                    }
                                }
                            )
                        }
                } else {
                    Modifier.fillMaxSize()
                }
            ) {
                AnimatedVisibility(
                    visible = areInlineControlsEnabled && isLongPressSpeedActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "2X",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Play/Pause Center Button
                if (areInlineControlsEnabled && isReady && !isPlaying && !isSeeking && !isUserInteracting) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .clickable {
                                userPausedByTap = false
                                pausedByDrag = false
                                resumePlayback()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            val showProgress = isReady &&
                duration > 0 &&
                areInlineControlsEnabled &&
                (isPlaying || isSeeking || isUserInteracting)
            if (showProgress) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentTime),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        val durationSafe = duration.coerceAtLeast(1L)
                        val liveFraction = (currentTime.toFloat() / durationSafe.toFloat()).coerceIn(0f, 1f)
                        if (preferFullScreenSeekBar) {
                            val displayedFraction = inlineScrubFraction ?: liveFraction
                            FullScreenSeekBar(
                                fraction = displayedFraction,
                                onScrubStart = {
                                    if (!isInlineScrubbing) {
                                        isInlineScrubbing = true
                                        isSeeking = true
                                        resumeAfterInlineScrub = if (preferIntentBasedResumeAfterScrub) {
                                            shouldPlay && !userPausedByTap && !pausedByDrag
                                        } else {
                                            exoPlayer.isPlaying
                                        }
                                        exoPlayer.pause()
                                        exoPlayer.playWhenReady = false
                                        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                                    }
                                },
                                onScrub = { fraction ->
                                    val clampedFraction = fraction.coerceIn(0f, 1f)
                                    inlineScrubFraction = clampedFraction
                                    val newTime = (clampedFraction * durationSafe.toFloat()).toLong()
                                    currentTime = newTime
                                },
                                onScrubEnd = {
                                    val finalFraction = inlineScrubFraction ?: liveFraction
                                    val finalPos = (finalFraction * durationSafe.toFloat()).toLong().coerceIn(0L, durationSafe)
                                    exoPlayer.setSeekParameters(SeekParameters.EXACT)
                                    exoPlayer.seekTo(finalPos)
                                    currentTime = finalPos
                                    inlineScrubFraction = null
                                    isInlineScrubbing = false
                                    isSeeking = false
                                    if (resumeAfterInlineScrub) {
                                        exoPlayer.playWhenReady = true
                                        if (!exoPlayer.isPlaying) exoPlayer.play()
                                    } else {
                                        exoPlayer.pause()
                                        exoPlayer.playWhenReady = false
                                    }
                                    resumeAfterInlineScrub = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Slider(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp),
                                value = inlineScrubFraction ?: liveFraction,
                                onValueChange = { fraction ->
                                    if (!isInlineScrubbing) {
                                        isInlineScrubbing = true
                                        isSeeking = true
                                        resumeAfterInlineScrub = if (preferIntentBasedResumeAfterScrub) {
                                            shouldPlay && !userPausedByTap && !pausedByDrag
                                        } else {
                                            exoPlayer.isPlaying
                                        }
                                        exoPlayer.pause()
                                        exoPlayer.playWhenReady = false
                                        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                                    }
                                    val clampedFraction = fraction.coerceIn(0f, 1f)
                                    inlineScrubFraction = clampedFraction
                                    val newTime = (clampedFraction * durationSafe.toFloat()).toLong()
                                    currentTime = newTime
                                },
                                onValueChangeFinished = {
                                    val finalFraction = inlineScrubFraction ?: liveFraction
                                    val finalPos = (finalFraction * durationSafe.toFloat()).toLong().coerceIn(0L, durationSafe)
                                    exoPlayer.setSeekParameters(SeekParameters.EXACT)
                                    exoPlayer.seekTo(finalPos)
                                    currentTime = finalPos
                                    inlineScrubFraction = null
                                    isInlineScrubbing = false
                                    isSeeking = false
                                    if (resumeAfterInlineScrub) {
                                        exoPlayer.playWhenReady = true
                                        if (!exoPlayer.isPlaying) exoPlayer.play()
                                    } else {
                                        exoPlayer.pause()
                                        exoPlayer.playWhenReady = false
                                    }
                                    resumeAfterInlineScrub = false
                                },
                                valueRange = 0f..1f,
                                interactionSource = interactionSource,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White.copy(alpha = 0.95f),
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                        
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        if (onFullScreen != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { 
                                    onFullScreen?.invoke(exoPlayer) 
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fullscreen,
                                    contentDescription = "Full Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun FullScreenVideoPlayer(
    item: MediaItem,
    player: ExoPlayer? = null,
    isMuted: Boolean,
    onMuteChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    dismissEnabled: Boolean = true,
    enableOpenCloseTransition: Boolean = true,
    showCloseButton: Boolean = true,
    showTopBar: Boolean = true,
    showMuteButton: Boolean = true,
    showSpeedControl: Boolean = true,
    controlsHorizontalPadding: Dp = 24.dp,
    controlsBottomPadding: Dp = 32.dp,
    controlsRightInset: Dp = 0.dp,
    showCenterPlayPause: Boolean = true,
    controlsEnabled: Boolean = true,
    // Single tap pauses/resumes like the organize card's inline player; the Play mark shows while paused.
    tapTogglesPlayPause: Boolean = false,
    // Taps outside the video frame go here instead of dismissing (for players embedded in a page).
    onTapOutsideVideo: (() -> Unit)? = null,
    onControlsHeightChanged: ((Dp) -> Unit)? = null,
    onPlaybackEnded: (() -> Unit)? = null
) {
    val transitionScope = rememberCoroutineScope()
    val openCloseKey = remember(item.id, player) { "${item.id}:${if (player != null) 1 else 0}" }
    val openCloseAlpha = remember(openCloseKey) {
        Animatable(if (enableOpenCloseTransition) 0f else 1f)
    }
    val openCloseScale = remember(openCloseKey) {
        Animatable(if (enableOpenCloseTransition) 0.965f else 1f)
    }
    var isDismissing by remember(openCloseKey) { mutableStateOf(false) }

    val requestDismiss: () -> Unit = {
        if (!dismissEnabled || isDismissing) {
            Unit
        } else {
            isDismissing = true
            transitionScope.launch {
                if (enableOpenCloseTransition) {
                    openCloseAlpha.stop()
                    openCloseScale.stop()
                    launch {
                        openCloseAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        )
                    }
                    launch {
                        openCloseScale.animateTo(
                            targetValue = 0.97f,
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        )
                    }
                    withFrameNanos { }
                }
                onDismiss()
            }
        }
    }

    LaunchedEffect(openCloseKey, enableOpenCloseTransition) {
        isDismissing = false
        if (!enableOpenCloseTransition) {
            openCloseAlpha.snapTo(1f)
            openCloseScale.snapTo(1f)
            return@LaunchedEffect
        }
        openCloseAlpha.snapTo(0f)
        openCloseScale.snapTo(0.965f)
        launch {
            openCloseAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
        launch {
            openCloseScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }

    BackHandler(enabled = dismissEnabled, onBack = requestDismiss)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember { player ?: VideoPlayerCache.get(context) }
    val coverRequest = if (item.type == "video") {
        rememberVideoFrameImageRequest(item.uri, crossfade = false)
    } else {
        rememberStillImageRequest(item.uri, crossfade = false)
    }
    
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isControlsVisible by remember(controlsEnabled) { mutableStateOf(controlsEnabled) }
    var currentTime by remember { mutableLongStateOf(exoPlayer.currentPosition) }
    var duration by remember { mutableLongStateOf(maxOf(exoPlayer.duration.coerceAtLeast(0L), item.duration.coerceAtLeast(0L))) }
    var isPlaying by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(exoPlayer.currentPosition.toFloat()) }
    var fullScreenScrubFraction by remember { mutableStateOf<Float?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isScrubbing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var reportedControlsHeightPx by remember { mutableIntStateOf(0) }
    var localMuted by remember(item.id) { mutableStateOf(isMuted) }
    var containerWidthPx by remember { mutableIntStateOf(1) }
    var containerHeightPx by remember { mutableIntStateOf(1) }
    var hasFirstFrame by remember(item.id, player) { mutableStateOf(false) }
    val fullScreenSwipeDismissState = rememberFullScreenSwipeDismissState(onDismiss = requestDismiss)
    val textureViewState = remember { mutableStateOf<TextureView?>(null) }
    var boundTextureView by remember { mutableStateOf<TextureView?>(null) }
    var resumeAfterLifecycleStop by remember(item.id, player) { mutableStateOf(false) }
    val currentItemId by rememberUpdatedState(item.id)
    var measuredVideoAspect by remember {
        mutableFloatStateOf(
            if (item.width > 0 && item.height > 0) {
                item.width.toFloat() / item.height.toFloat()
            } else {
                16f / 9f
            }
        )
    }
    val shouldRepeatOne = item.type == "video"
    val isLiveLikeItem = item.isLivePhoto || item.isMotionPhoto
    val keepTopActionsVisible = isLiveLikeItem
    val topBarVisible = showTopBar && (keepTopActionsVisible || (controlsEnabled && isControlsVisible))
    val bottomBarVisible = controlsEnabled && isControlsVisible
    val showSpeedControlInTopBar = showSpeedControl && isLiveLikeItem
    val speedLabel = "${if (playbackSpeed % 1 == 0f) playbackSpeed.toInt() else playbackSpeed}x"

    fun cyclePlaybackSpeed() {
        playbackSpeed = when (playbackSpeed) {
            1f -> 1.5f
            1.5f -> 2f
            2f -> 0.8f
            else -> 1f
        }
    }

    fun updateFullScreenTextureVisibility(textureView: TextureView?, visible: Boolean) {
        textureView?.alpha = if (visible) 1f else 0f
    }

    fun bindFullScreenTextureView(textureView: TextureView?, force: Boolean = false) {
        if (textureView == null || !textureView.isAvailable) return
        updateFullScreenTextureVisibility(textureView, hasFirstFrame)
        if (force || boundTextureView !== textureView) {
            exoPlayer.setVideoTextureView(textureView)
            boundTextureView = textureView
        }
    }

    fun clearFullScreenTextureView(textureView: TextureView?) {
        updateFullScreenTextureVisibility(textureView, false)
        if (textureView != null && boundTextureView === textureView) {
            exoPlayer.clearVideoTextureView(textureView)
            boundTextureView = null
        }
    }
    val currentBindFullScreenTextureView by rememberUpdatedState<(TextureView?, Boolean) -> Unit> { textureView, force ->
        bindFullScreenTextureView(textureView, force)
    }
    val currentClearFullScreenTextureView by rememberUpdatedState<(TextureView?) -> Unit> { textureView ->
        clearFullScreenTextureView(textureView)
    }
    fun isPreparedForCurrentItem(): Boolean {
        return exoPlayer.currentMediaItem?.mediaId == currentItemId.toString()
    }

    // When using transferred player from preview, immediately force fullscreen takeover and playback.
    LaunchedEffect(player) {
        if (player != null) {
            exoPlayer.setSeekParameters(SeekParameters.EXACT)
            hasFirstFrame = false
            exoPlayer.playWhenReady = true
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
            }
            duration = maxOf(exoPlayer.duration.coerceAtLeast(0L), item.duration.coerceAtLeast(0L))
            currentTime = exoPlayer.currentPosition
            sliderPosition = currentTime.toFloat()
            fullScreenScrubFraction = null
            bindFullScreenTextureView(textureViewState.value)
        }
    }

    // Sync Mute State from outside if it changes externally
    LaunchedEffect(isMuted) {
        localMuted = isMuted
    }

    LaunchedEffect(localMuted) {
        exoPlayer.volume = if (localMuted) 0f else 1f
    }

    LaunchedEffect(shouldRepeatOne) {
        exoPlayer.repeatMode = if (shouldRepeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded?.invoke()
                }
            }

            override fun onRenderedFirstFrame() {
                if (!isPreparedForCurrentItem()) return
                hasFirstFrame = true
                updateFullScreenTextureVisibility(textureViewState.value, true)
            }

            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val rawWidth = videoSize.width
                val rawHeight = videoSize.height
                if (rawWidth > 0 && rawHeight > 0) {
                    measuredVideoAspect = (rawWidth.toFloat() * videoSize.pixelWidthHeightRatio) / rawHeight.toFloat()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            clearFullScreenTextureView(textureViewState.value)
            exoPlayer.removeListener(listener)
        }
    }

    // Auto-hide controls
    LaunchedEffect(isControlsVisible, isPlaying, isScrubbing, isSeeking, controlsEnabled) {
        if (!controlsEnabled) return@LaunchedEffect
        if (isControlsVisible && isPlaying && !isScrubbing && !isSeeking) {
            delay(3000)
            isControlsVisible = false
        }
    }

    // Playback Speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Progress Loop
    val shouldRunProgressLoop = controlsEnabled
    LaunchedEffect(shouldRunProgressLoop) {
        if (!shouldRunProgressLoop) return@LaunchedEffect
        while (isActive) {
            try {
                val dur = exoPlayer.duration
                if (dur > 0) {
                    duration = dur
                } else if (duration <= 0L && item.duration > 0L) {
                    duration = item.duration
                }
                if (!isSeeking && !isScrubbing) {
                    val current = exoPlayer.currentPosition
                    currentTime = current
                    sliderPosition = current.toFloat()
                }
                isPlaying = exoPlayer.isPlaying
            } catch (e: Exception) {
                // Ignore
            }
            kotlinx.coroutines.delay(200) // Faster update for smooth UI
        }
    }
    
    // Pause analysis during full screen playback
    DisposableEffect(Unit) {
        MediaAnalysisManager.requestPause()
        onDispose {
            MediaAnalysisManager.releasePause()
        }
    }

    // Determine Video URI
    LaunchedEffect(item) {
        if (item.isMotionPhoto) {
             val file = withContext(Dispatchers.IO) {
                 MotionPhotoHelper.extractVideo(context, item.uri, MotionPhotoHelper.MotionPhotoInfo(true, item.motionPhotoVideoOffset, item.motionPhotoVideoLength))
             }
             if (file != null) {
                 videoUri = Uri.fromFile(file)
             }
        } else if (item.isLivePhoto && item.livePhotoVideoUri != null) {
            videoUri = item.livePhotoVideoUri
        } else {
            videoUri = item.uri
        }
    }
    
    // Initialize Player
    DisposableEffect(videoUri) {
        if (videoUri == null) return@DisposableEffect onDispose {}

        // Only prepare if it's a NEW player (not passed in)
        if (player == null) {
            // Prepare source
            try {
                hasFirstFrame = false
                clearFullScreenTextureView(textureViewState.value)
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setSeekParameters(SeekParameters.EXACT)
                
                val mediaItem = ExoMediaItem.Builder()
                    .setMediaId(item.id.toString())
                    .setUri(videoUri!!)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                bindFullScreenTextureView(textureViewState.value)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Ensure transferred player keeps playing when entering fullscreen.
            bindFullScreenTextureView(textureViewState.value)
            exoPlayer.playWhenReady = true
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
            }
        }
        
        onDispose {
            clearFullScreenTextureView(textureViewState.value)
            if (player == null) {
                exoPlayer.pause()
                VideoPlayerCache.release(exoPlayer)
            }
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val shouldResume = isPreparedForCurrentItem() && (exoPlayer.isPlaying || exoPlayer.playWhenReady)
                    resumeAfterLifecycleStop = shouldResume
                    if (shouldResume) {
                        exoPlayer.pause()
                        exoPlayer.playWhenReady = false
                    }
                }

                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> {
                    if (resumeAfterLifecycleStop && isPreparedForCurrentItem()) {
                        currentBindFullScreenTextureView(textureViewState.value, true)
                        exoPlayer.playWhenReady = true
                        if (!exoPlayer.isPlaying) {
                            exoPlayer.play()
                        }
                        resumeAfterLifecycleStop = false
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val controlsBlockHeight = 200.dp
    val topControlsGuardHeight = if (showTopBar) 120.dp else 0.dp
    val density = LocalDensity.current
    val controlsBlockHeightPx = with(density) { controlsBlockHeight.toPx() }
    val topControlsGuardPx = with(density) { topControlsGuardHeight.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                alpha = openCloseAlpha.value
                scaleX = openCloseScale.value * fullScreenSwipeDismissState.contentScale
                scaleY = openCloseScale.value * fullScreenSwipeDismissState.contentScale
                translationY = fullScreenSwipeDismissState.offsetY
            }
            .onSizeChanged { size ->
                containerWidthPx = size.width.coerceAtLeast(1)
                containerHeightPx = size.height.coerceAtLeast(1)
            }
    ) {
        AndroidView(
            factory = { ctx ->
                AspectRatioFrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    val textureView = TextureView(ctx)
                    textureView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    textureView.isOpaque = false
                    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            boundTextureView = null
                            currentBindFullScreenTextureView(textureView, true)
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            currentClearFullScreenTextureView(textureView)
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                            if (!hasFirstFrame &&
                                exoPlayer.playbackState == Player.STATE_READY &&
                                isPreparedForCurrentItem()
                            ) {
                                hasFirstFrame = true
                                updateFullScreenTextureVisibility(textureView, true)
                            }
                        }
                    }
                    addView(textureView)
                    updateFullScreenTextureVisibility(textureView, hasFirstFrame)
                    bindFullScreenTextureView(textureView, force = true)
                    textureViewState.value = textureView
                }
            },
            update = { frame ->
                frame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                if (measuredVideoAspect > 0f) {
                    frame.setAspectRatio(measuredVideoAspect)
                }
                val textureView = frame.getChildAt(0) as? TextureView
                if (textureView != null) {
                    if (textureViewState.value !== textureView) {
                        textureViewState.value = textureView
                    }
                    updateFullScreenTextureVisibility(textureView, hasFirstFrame)
                    bindFullScreenTextureView(textureView)
                }
            },
            onRelease = { frame ->
                clearFullScreenTextureView(frame.getChildAt(0) as? TextureView)
                textureViewState.value = null
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = if (hasFirstFrame) 1f else 0f)
        )
        if (!hasFirstFrame) {
            AsyncImage(
                model = coverRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        val gestureOverlayModifier = if (isControlsVisible) {
            Modifier
                .fillMaxSize()
                .padding(top = topControlsGuardHeight, bottom = controlsBlockHeight)
        } else {
            Modifier.fillMaxSize()
        }
        val gestureModifier = if (controlsEnabled) {
            gestureOverlayModifier
                .fullScreenSwipeToDismiss(
                    enabled = dismissEnabled && !isScrubbing && !isSeeking,
                    state = fullScreenSwipeDismissState
                )
                .pointerInput(isControlsVisible, isScrubbing, isSeeking) {
                    detectTapGestures(
                        onTap = { tap ->
                            val videoRect = fittedVideoRect(
                                containerWidthPx = containerWidthPx,
                                containerHeightPx = containerHeightPx,
                                aspectRatio = measuredVideoAspect
                            )
                            if (!videoRect.contains(tap)) {
                                if (onTapOutsideVideo != null) onTapOutsideVideo() else requestDismiss()
                                return@detectTapGestures
                            }
                            if (isSeeking || isScrubbing) return@detectTapGestures
                            if (tapTogglesPlayPause) {
                                // playWhenReady, not isPlaying: buffering reports not-playing.
                                if (exoPlayer.playWhenReady) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                                isControlsVisible = true
                                return@detectTapGestures
                            }
                            if (!isControlsVisible) {
                                isControlsVisible = true
                                return@detectTapGestures
                            }
                            isControlsVisible = false
                        }
                    )
                }
        } else {
            gestureOverlayModifier
        }
        Box(modifier = gestureModifier)

        // Center Play/Pause Button
        AnimatedVisibility(
            visible = controlsEnabled &&
                ((isControlsVisible && showCenterPlayPause) || (tapTogglesPlayPause && !exoPlayer.playWhenReady && !isPlaying)),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Top Bar
        AnimatedVisibility(
            visible = topBarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(top = 16.dp)
            ) {
                if (showCloseButton) {
                    IconButton(
                        onClick = requestDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 12.dp)
                            .size(44.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            "Close",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSpeedControlInTopBar) {
                        TextButton(
                            onClick = { cyclePlaybackSpeed() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = speedLabel,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (showMuteButton) {
                        IconButton(
                            onClick = {
                                val nextMuted = !localMuted
                                localMuted = nextMuted
                                onMuteChange(nextMuted)
                            }
                        ) {
                            Icon(
                                imageVector = if (localMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                contentDescription = if (localMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Bottom Bar
        AnimatedVisibility(
            visible = bottomBarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        if (size.height != reportedControlsHeightPx) {
                            reportedControlsHeightPx = size.height
                            onControlsHeightChanged?.invoke(with(density) { size.height.toDp() })
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(
                        start = controlsHorizontalPadding,
                        end = controlsHorizontalPadding + controlsRightInset,
                        top = controlsBottomPadding,
                        bottom = controlsBottomPadding
                    )
            ) {
                // Time Labels and Speed Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentTime),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Playback Speed Button
                    if (showSpeedControl && !showSpeedControlInTopBar) {
                        TextButton(
                            onClick = { cyclePlaybackSpeed() }
                        ) {
                            Text(
                                text = speedLabel,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }

                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Seek bar
                val durationSafe = duration.coerceAtLeast(1L)
                val liveFraction = (currentTime.toFloat() / durationSafe.toFloat()).coerceIn(0f, 1f)
                val displayedFraction = fullScreenScrubFraction ?: liveFraction
                val startScrub = {
                    if (!isScrubbing) {
                        isScrubbing = true
                        isSeeking = true
                        isControlsVisible = true
                    }
                }
                val updateScrub: (Float) -> Unit = { fraction ->
                    val clampedFraction = fraction.coerceIn(0f, 1f)
                    fullScreenScrubFraction = clampedFraction
                    val newTime = (clampedFraction * durationSafe.toFloat()).toLong()
                    sliderPosition = newTime.toFloat()
                    currentTime = newTime
                    isSeeking = true
                    isControlsVisible = true
                }
                val finishScrub = {
                    if (isScrubbing || fullScreenScrubFraction != null) {
                        val finalFraction = fullScreenScrubFraction ?: displayedFraction
                        val finalPos = (finalFraction * durationSafe.toFloat()).toLong().coerceIn(0L, durationSafe)
                        exoPlayer.setSeekParameters(SeekParameters.EXACT)
                        exoPlayer.seekTo(finalPos)
                        currentTime = finalPos
                        sliderPosition = finalPos.toFloat()
                        fullScreenScrubFraction = null
                        isScrubbing = false
                        isSeeking = false
                    }
                }
                FullScreenSeekBar(
                    fraction = displayedFraction,
                    onScrubStart = startScrub,
                    onScrub = updateScrub,
                    onScrubEnd = finishScrub,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    DisposableEffect(exoPlayer, onPlaybackEnded) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded?.invoke()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }
}

private fun fittedVideoRect(
    containerWidthPx: Int,
    containerHeightPx: Int,
    aspectRatio: Float
): Rect {
    val cw = containerWidthPx.toFloat().coerceAtLeast(1f)
    val ch = containerHeightPx.toFloat().coerceAtLeast(1f)
    val safeAspect = aspectRatio.coerceAtLeast(0.1f)
    val containerAspect = cw / ch
    val drawW: Float
    val drawH: Float
    if (safeAspect > containerAspect) {
        drawW = cw
        drawH = cw / safeAspect
    } else {
        drawH = ch
        drawW = ch * safeAspect
    }
    val left = (cw - drawW) / 2f
    val top = (ch - drawH) / 2f
    return Rect(left, top, left + drawW, top + drawH)
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun FullScreenSeekBar(
    fraction: Float,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val thumbSize = 14.dp
    val trackHeight = 4.dp
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }

    Box(
        modifier = modifier
            .height(28.dp)
            .onSizeChanged { size ->
                barWidthPx = size.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(barWidthPx) {
                detectTapGestures { offset ->
                    onScrubStart()
                    onScrub((offset.x / barWidthPx).coerceIn(0f, 1f))
                    onScrubEnd()
                }
            }
            .pointerInput(barWidthPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onScrubStart()
                        onScrub((offset.x / barWidthPx).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onScrub((change.position.x / barWidthPx).coerceIn(0f, 1f))
                    },
                    onDragEnd = onScrubEnd,
                    onDragCancel = onScrubEnd
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(clampedFraction)
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    IntOffset(
                        x = ((barWidthPx - thumbSizePx) * clampedFraction).roundToInt(),
                        y = 0
                    )
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
