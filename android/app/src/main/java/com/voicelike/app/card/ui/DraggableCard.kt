package com.voicelike.app

import kotlin.math.sqrt
import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val STACK_REORDER_DELAY_MS = 28
private const val STACK_REORDER_DURATION_MS = 230
private const val HORIZONTAL_EXIT_DISTANCE_MULTIPLIER = 1.16f
private val HORIZONTAL_EXIT_MIN_TARGET = 480.dp
private const val SWIPE_EXIT_SPEED_REFERENCE_DP = 400f
private const val SWIPE_EXIT_MIN_SECONDS = 0.18f
private const val SWIPE_EXIT_MAX_SECONDS = 0.30f
private const val SWIPE_EXIT_SCALE = 0.985f
private const val SWIPE_TILT_DIVISOR_DP = 17f
private const val SWIPE_TILT_MAX_DEGREES = 11f
private const val UP_DRAG_SHRINK_DISTANCE_DP = 300f
private const val UP_DRAG_SHRINK_AMOUNT = 0.04f
private const val DOWN_DRAG_SHRINK_DISTANCE_DP = 240f
private const val DOWN_DRAG_SHRINK_AMOUNT = 0.3f
private const val PRESS_SINK_SCALE = 0.98f
private val StackPromotionEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val SwipeExitEaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)

/** Velocity-matched fling time (400 / speed, 0.18..0.30s) divided by the user's speed preference. */
private fun resolveSwipeExitDurationMs(
    releaseSpeedDpPerSec: Float,
    speedMultiplier: Float,
    defaultSpeedMultiplier: Float
): Int {
    val baseSeconds = (SWIPE_EXIT_SPEED_REFERENCE_DP / releaseSpeedDpPerSec.coerceAtLeast(1f))
        .coerceIn(SWIPE_EXIT_MIN_SECONDS, SWIPE_EXIT_MAX_SECONDS)
    val preferenceScale = (normalizeSwipeSpeedMultiplier(speedMultiplier) / defaultSpeedMultiplier)
        .coerceAtLeast(0.1f)
    return (baseSeconds * 1000f / preferenceScale).roundToInt().coerceAtLeast(1)
}

private fun resolveUpExitTargetPx(
    screenHeightPx: Float,
    cardHeightPx: Float
): Float {
    return -(screenHeightPx + cardHeightPx.coerceAtLeast(0f))
}

private class CardDragLayoutSnapshot(
    var cardSizePx: IntSize = IntSize.Zero,
    var cardPositionInRoot: Offset = Offset.Zero,
    var imageBoundsInRoot: Rect? = null
)

private data class CardVisualState(
    val resolvedOffsetX: Float,
    val resolvedOffsetY: Float,
    val visualOffsetX: Float,
    val visualOffsetY: Float,
    val totalScaleX: Float,
    val totalScaleY: Float,
    val swipeTilt: Float
)

private fun calculateCardVisualState(
    resolvedOffsetX: Float,
    resolvedOffsetY: Float,
    baseScale: Float,
    exitScale: Float,
    pressScale: Float,
    isDraggingWithTouchAnchor: Boolean,
    dragAnchorFromCenter: Offset,
    dragStartScaleX: Float,
    dragStartScaleY: Float,
    pxPerDp: Float,
    disableDownSwipeGestures: Boolean,
    suppressDownSwipeScale: Boolean,
    swipeTiltEnabled: Boolean,
    tiltOverride: Float = Float.NaN
): CardVisualState {
    val safePxPerDp = pxPerDp.coerceAtLeast(0.1f)
    val offsetXDp = resolvedOffsetX / safePxPerDp
    val offsetYDp = resolvedOffsetY / safePxPerDp
    val absOffsetXDp = kotlin.math.abs(offsetXDp)
    val absOffsetYDp = kotlin.math.abs(offsetYDp)

    // Up: shrink by up to 4% over 300dp. Down (folder drop): shrink towards 0.7 at 240dp.
    val upProgress = if (offsetYDp < 0f) {
        (-offsetYDp / UP_DRAG_SHRINK_DISTANCE_DP).coerceIn(0f, 1f)
    } else {
        0f
    }
    val downProgress = if (
        !disableDownSwipeGestures &&
        !suppressDownSwipeScale &&
        offsetYDp > 0f
    ) {
        (offsetYDp / DOWN_DRAG_SHRINK_DISTANCE_DP).coerceIn(0f, 1f)
    } else {
        0f
    }
    val dynamicScale = (1f - (UP_DRAG_SHRINK_AMOUNT * upProgress)) *
        (1f - (DOWN_DRAG_SHRINK_AMOUNT * downProgress))

    val totalScaleX = baseScale * dynamicScale * exitScale * pressScale
    val totalScaleY = baseScale * dynamicScale * exitScale * pressScale
    val dragScaleRatioX = if (dragStartScaleX > 0f) totalScaleX / dragStartScaleX else 1f
    val dragScaleRatioY = if (dragStartScaleY > 0f) totalScaleY / dragStartScaleY else 1f
    val dragFollowCompensation = if (isDraggingWithTouchAnchor) {
        Offset(
            x = dragAnchorFromCenter.x * (1f - dragScaleRatioX),
            y = dragAnchorFromCenter.y * (1f - dragScaleRatioY)
        )
    } else {
        Offset.Zero
    }
    val visualOffsetX = resolvedOffsetX + dragFollowCompensation.x
    val visualOffsetY = resolvedOffsetY + dragFollowCompensation.y
    val swipeTilt = when {
        !swipeTiltEnabled -> 0f
        !tiltOverride.isNaN() -> tiltOverride
        else -> resolveSwipeTiltDegrees(offsetXDp, absOffsetXDp, absOffsetYDp)
    }

    return CardVisualState(
        resolvedOffsetX = resolvedOffsetX,
        resolvedOffsetY = resolvedOffsetY,
        visualOffsetX = visualOffsetX,
        visualOffsetY = visualOffsetY,
        totalScaleX = totalScaleX,
        totalScaleY = totalScaleY,
        swipeTilt = swipeTilt
    )
}

/** Tilt = clamp(dx / 17, +-11deg) weighted by horizontal dominance; all distances in dp. */
private fun resolveSwipeTiltDegrees(offsetXDp: Float, absOffsetXDp: Float, absOffsetYDp: Float): Float {
    if (absOffsetXDp <= 1f) return 0f
    val horizontalWeight = (absOffsetXDp / (absOffsetXDp + (absOffsetYDp * 1.2f) + 1f)).coerceIn(0f, 1f)
    return (offsetXDp / SWIPE_TILT_DIVISOR_DP).coerceIn(-SWIPE_TILT_MAX_DEGREES, SWIPE_TILT_MAX_DEGREES) *
        horizontalWeight
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableCard(
    item: MediaItem,
    forceHidden: Boolean = false,
    isTopCard: Boolean,
    stackIndex: Int,
    muteVideos: Boolean,
    lensyEnabled: Boolean = true,
    showMlLabelsOnHome: Boolean = true,
    colorCardLongPressEnabled: Boolean = true,
    compactFooter: Boolean = false,
    footerSecondaryText: String? = null,
    footerSecondaryTextColor: Color = Color.Unspecified,
    pauseMainVideos: Boolean,
    topCardPlaybackArmed: Boolean = true,
    shouldAnalyze: Boolean = isTopCard,
    deferHeavyCardWork: Boolean = false,
    predictedSwipeIntent: SwipeIntent = SwipeIntent.None,
    predictionBias: Float = 0f,
    blockVideoDuringEntry: Boolean = false,
    disableStackAnimation: Boolean = false,
    suppressActionIndicators: Boolean = false,
    undoDirection: SwipeDirection? = null,
    undoToken: Long? = null,
    leftAction: SwipeAction = SwipeAction.Delete,
    rightAction: SwipeAction = SwipeAction.Like,
    upAction: SwipeAction = SwipeAction.Keep,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onDetail: () -> Unit,
    onDetailWithSourceBounds: ((Rect?) -> Unit)? = null,
    onSwipeSound: () -> Unit = {},
    onDeleteSound: () -> Unit = {},
    onLikeSound: () -> Unit = {},
    onDragStart: (() -> Unit)? = null,
    onDragUpdate: ((Float, Float) -> Unit)? = null,
    onDragPointerUpdate: ((Float, Float) -> Unit)? = null,
    onTopCardBoundsChanged: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    onTopCardImageBoundsChanged: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    onAiOverlayStateChanged: ((String?, Boolean) -> Unit)? = null,
    onSwipeIntentPreview: ((SwipeIntent, Float, Boolean) -> Unit)? = null,
    onDragVelocityUpdate: ((Float) -> Unit)? = null,
    onDragEnd: ((Float, Float) -> (() -> Unit)?)? = null,
    onDragCancel: (() -> Unit)? = null,
    onFullScreenVideo: ((MediaItem, ExoPlayer) -> Unit)? = null,
    onMuteChange: ((Boolean) -> Unit)? = null,
    onJumpToDay: ((String) -> Unit)? = null,
    onStartupSample: ((Long) -> Unit)? = null,
    isFullScreenVideoOpen: Boolean = false,
    cardBiasY: Float = -0.12f,
    disableDownSwipeGestures: Boolean = false,
    cardWidthFraction: Float = 0.8f,
    cardHeightFraction: Float = 0.6f,
    cardAspectRatio: Float? = null,
    cardMinAspectRatio: Float? = null,
    cardMaxAspectRatio: Float? = null,
    cardMaxWidth: Dp = Dp.Unspecified,
    cardMaxHeight: Dp = Dp.Unspecified,
    allowSwipeLeft: Boolean = true,
    allowSwipeRight: Boolean = true,
    allowSwipeUp: Boolean = true,
    swipeTiltEnabled: Boolean = true,
    leftIndicatorSpec: DraggableCardSwipeIndicatorSpec? = null,
    rightIndicatorSpec: DraggableCardSwipeIndicatorSpec? = null,
    upIndicatorSpec: DraggableCardSwipeIndicatorSpec? = null,
    // Photo-frame card: no chrome, the visible element is the photo fitted inside the card box.
    photoFrameMode: Boolean = false,
    // Explicit card box size; overrides the fraction/aspect sizing when both are specified.
    cardBoxWidth: Dp = Dp.Unspecified,
    cardBoxHeight: Dp = Dp.Unspecified,
    // Extra multiplier on the stack scale of back cards (keeps them inside the top card's photo).
    stackScaleCap: Float = 1f,
    // Raw per-frame drag offset (px) + commit threshold (px); active=false on release/cancel.
    onSwipeGlowFrame: ((Float, Float, Float, Boolean) -> Unit)? = null,
    // Fired on release when a fling exit starts, before the exit animation runs.
    onSwipeCommitStart: ((SwipeIntent) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val isDark = isSystemInDarkTheme()
    val hostView = LocalView.current
    val rootWindowView = hostView.rootView
    val activity = LocalContext.current as? Activity
    val stillImageRequest = rememberHomeCardStillImageRequest(item.id, item.uri, crossfade = false)
    val videoFrameRequest = rememberHomeCardVideoFrameImageRequest(item.id, item.uri, crossfade = false)

    val config = LocalContext.current.resources.configuration
    val density = LocalDensity.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val pxPerDp = density.density

    val initialX = if (undoToken != null && undoDirection != null) {
        when (undoDirection) {
            SwipeDirection.Left -> -screenWidthPx * 1.2f
            SwipeDirection.Right -> screenWidthPx * 1.2f
            else -> 0f
        }
    } else {
        0f
    }
    val initialY = if (undoToken != null && undoDirection != null) {
        when (undoDirection) {
            SwipeDirection.Up -> -screenHeightPx * 1.2f
            SwipeDirection.Down -> screenHeightPx * 1.2f
            else -> 0f
        }
    } else {
        0f
    }
    val initialAlpha = 1f
    val initialScale = if (undoToken != null && undoDirection != null) 0.985f else 1f

    val offsetX = remember { Animatable(initialX) }
    val offsetY = remember { Animatable(initialY) }
    val alphaAnim = remember { Animatable(initialAlpha) }
    val exitScale = remember { Animatable(initialScale) }
    var dragOffsetX by remember(item.id) { mutableFloatStateOf(initialX) }
    var dragOffsetY by remember(item.id) { mutableFloatStateOf(initialY) }
    // Where and when this drag started, for the release-speed fallback on sparse touch input.
    var dragStartUptimeMs by remember(item.id) { mutableLongStateOf(0L) }
    var dragStartOffset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var isTrackingDragOffsets by remember(item.id) { mutableStateOf(false) }

    var isPressed by remember { mutableStateOf(false) }
    var isImageLongPressHoverActive by remember(item.id) { mutableStateOf(false) }
    var isImageLongPressHoverPresented by remember(item.id) { mutableStateOf(false) }
    var isCardDragging by remember { mutableStateOf(false) }
    // Press sink: 0.98 on touch-down, eased back once a drag takes over, springy return on a plain tap.
    val isPressSinking = isPressed && !isCardDragging && !isImageLongPressHoverActive
    val pressScale by animateFloatAsState(
        targetValue = if (isPressSinking) PRESS_SINK_SCALE else 1f,
        animationSpec = when {
            isPressSinking -> spring<Float>(dampingRatio = 0.9f, stiffness = 1250f)
            isCardDragging -> tween<Float>(durationMillis = 120, easing = LinearOutSlowInEasing)
            else -> spring<Float>(dampingRatio = 0.72f, stiffness = 920f)
        },
        label = "pressScale"
    )
    var dragAnchorFromCenter by remember { mutableStateOf(Offset.Zero) }
    var dragStartScaleX by remember { mutableFloatStateOf(1f) }
    var dragStartScaleY by remember { mutableFloatStateOf(1f) }
    var isDraggingWithTouchAnchor by remember { mutableStateOf(false) }
    val dragLayoutSnapshot = remember { CardDragLayoutSnapshot() }

    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val currentOnDragUpdate by rememberUpdatedState(onDragUpdate)
    val currentOnDragPointerUpdate by rememberUpdatedState(onDragPointerUpdate)
    val currentOnTopCardBoundsChanged by rememberUpdatedState(onTopCardBoundsChanged)
    val currentOnTopCardImageBoundsChanged by rememberUpdatedState(onTopCardImageBoundsChanged)
    val currentOnSwipeIntentPreview by rememberUpdatedState(onSwipeIntentPreview)
    val currentOnDragVelocityUpdate by rememberUpdatedState(onDragVelocityUpdate)
    val currentOnDeleteSound by rememberUpdatedState(onDeleteSound)
    val currentOnLikeSound by rememberUpdatedState(onLikeSound)
    val currentOnSwipeSound by rememberUpdatedState(onSwipeSound)
    val playSoundForAction: (SwipeAction) -> Unit = { action ->
        when (action) {
            SwipeAction.Delete -> currentOnDeleteSound()
            SwipeAction.Like -> currentOnLikeSound()
            SwipeAction.Keep -> currentOnSwipeSound()
            SwipeAction.None -> Unit
        }
    }

    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }

    val effectiveStackIndex = stackIndex.coerceAtMost(2)
    val resolvedStackScaleCap = if (stackIndex > 0) stackScaleCap.coerceIn(0.1f, 1f) else 1f
    val scaleValue = (1f - (effectiveStackIndex * 0.04f)) * resolvedStackScaleCap
    val scale by if (disableStackAnimation) {
        rememberUpdatedState(scaleValue)
    } else {
        animateFloatAsState(
            targetValue = scaleValue,
            animationSpec = tween(
                durationMillis = STACK_REORDER_DURATION_MS,
                delayMillis = STACK_REORDER_DELAY_MS,
                easing = StackPromotionEasing
            )
        )
    }
    val yOffsetValue = (effectiveStackIndex * 15).dp
    val yOffset by if (disableStackAnimation) {
        rememberUpdatedState(yOffsetValue)
    } else {
        animateDpAsState(
            targetValue = yOffsetValue,
            animationSpec = tween(
                durationMillis = STACK_REORDER_DURATION_MS,
                delayMillis = STACK_REORDER_DELAY_MS,
                easing = StackPromotionEasing
            )
        )
    }
    var isImageLongPressHoverVisualReady by remember(item.id) { mutableStateOf(false) }
    val isImageLongPressHoverVisualsActive =
        isTopCard &&
            !isExpanded &&
            !isCardDragging &&
            isImageLongPressHoverPresented &&
            isImageLongPressHoverVisualReady
    val hoverVisuals = rememberHomeCardLongPressHoverVisuals(
        active = isImageLongPressHoverVisualsActive
    )
    val shouldSuppressDownSwipeScale =
        undoToken != null &&
            undoDirection == SwipeDirection.Down &&
            isTopCard &&
            exitScale.value < 0.999f
    var isFolderDragMode by remember(item.id) { mutableStateOf(false) }
    var intentBadgeDirection by remember(item.id) { mutableStateOf(SwipeIntentBadgeDirection.None) }
    var exitTiltOverride by remember(item.id) { mutableFloatStateOf(Float.NaN) }
    val currentOnSwipeGlowFrame by rememberUpdatedState(onSwipeGlowFrame)
    val currentOnSwipeCommitStart by rememberUpdatedState(onSwipeCommitStart)
    var lastReportedDragQuantizedX by remember(item.id) { mutableIntStateOf(Int.MIN_VALUE) }
    var lastReportedDragQuantizedY by remember(item.id) { mutableIntStateOf(Int.MIN_VALUE) }
    var suppressCardTapUntilUptimeMs by remember(item.id) { mutableLongStateOf(0L) }
    var hoverBackdropBitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    val hoverWindowSizePx = remember(
        rootWindowView.width,
        rootWindowView.height,
        screenWidthPx,
        screenHeightPx
    ) {
        IntSize(
            width = maxOf(
                rootWindowView.width.takeIf { it > 0 } ?: 0,
                screenWidthPx.roundToInt()
            ),
            height = maxOf(
                rootWindowView.height.takeIf { it > 0 } ?: 0,
                screenHeightPx.roundToInt()
            )
        )
    }
    val dismissImageLongPressHover: () -> Unit = {
        val wasPresented = isImageLongPressHoverActive || isImageLongPressHoverPresented
        isImageLongPressHoverActive = false
        isImageLongPressHoverPresented = false
        isImageLongPressHoverVisualReady = false
        hoverBackdropBitmap = null
        if (wasPresented) {
            suppressCardTapUntilUptimeMs = maxOf(
                suppressCardTapUntilUptimeMs,
                SystemClock.uptimeMillis() + 350L
            )
        }
    }
    LaunchedEffect(colorCardLongPressEnabled) {
        if (!colorCardLongPressEnabled) {
            dismissImageLongPressHover()
        }
    }

    fun currentResolvedOffsetX(): Float = if (isTrackingDragOffsets) dragOffsetX else offsetX.value

    fun currentResolvedOffsetY(): Float = if (isTrackingDragOffsets) dragOffsetY else offsetY.value

    fun currentCardVisualState(): CardVisualState =
        calculateCardVisualState(
            resolvedOffsetX = currentResolvedOffsetX(),
            resolvedOffsetY = currentResolvedOffsetY(),
            baseScale = scale,
            exitScale = exitScale.value,
            pressScale = pressScale,
            isDraggingWithTouchAnchor = isDraggingWithTouchAnchor,
            dragAnchorFromCenter = dragAnchorFromCenter,
            dragStartScaleX = dragStartScaleX,
            dragStartScaleY = dragStartScaleY,
            pxPerDp = pxPerDp,
            disableDownSwipeGestures = disableDownSwipeGestures,
            suppressDownSwipeScale = shouldSuppressDownSwipeScale,
            swipeTiltEnabled = swipeTiltEnabled,
            tiltOverride = exitTiltOverride
        )

    LaunchedEffect(isTopCard, isExpanded, isCardDragging, item.id) {
        if (!isTopCard || isExpanded || isCardDragging) {
            isImageLongPressHoverActive = false
            isImageLongPressHoverPresented = false
            isImageLongPressHoverVisualReady = false
        }
    }
    var isMuted by remember(muteVideos) { mutableStateOf(muteVideos) }
    var isVideoSeeking by remember { mutableStateOf(false) }
    LaunchedEffect(isCardDragging) {
        AppPerformanceGovernor.setCriticalInteraction(isCardDragging)
    }
    DisposableEffect(Unit) {
        onDispose { AppPerformanceGovernor.setCriticalInteraction(false) }
    }
    val gestureState = rememberDraggableCardGestureState(item.id)
    val dragPauseToken = gestureState.dragPauseToken
    val dragGainXStart = 0.88f
    val dragGainXRampDistancePx = 220f
    val dragGainY = 1.10f
    var suppressCardTapOnce by remember(item.id) { mutableStateOf(false) }
    var livePlaybackCompleted by remember(item.id) { mutableStateOf(false) }
    var liveReplayToken by remember(item.id) { mutableLongStateOf(0L) }
    var livePlaybackArmed by remember(item.id) { mutableStateOf(true) }

    val shouldPlayVideo = isTopCard &&
        topCardPlaybackArmed &&
        !isExpanded &&
        !pauseMainVideos &&
        !blockVideoDuringEntry &&
        !isCardDragging
    val shouldRenderVideoPlayer =
        isTopCard &&
            topCardPlaybackArmed &&
            !isExpanded &&
            !blockVideoDuringEntry &&
            !isCardDragging
    val horizontalSwipeSpeedMultiplier = SwipeMotionPreferences.horizontalSpeedMultiplier
    val upSwipeSpeedMultiplier = SwipeMotionPreferences.upSpeedMultiplier

    val shouldUseHdr = isTopCard && isHdrMimeType(item.mimeType)
    DisposableEffect(shouldUseHdr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.window?.colorMode = if (shouldUseHdr) {
                android.content.pm.ActivityInfo.COLOR_MODE_HDR
            } else {
                android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
            }
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.window?.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
            }
        }
    }

    LaunchedEffect(undoToken) {
        if (undoToken == null || undoDirection == null) {
            if (!isTrackingDragOffsets) {
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
            }
            alphaAnim.snapTo(1f)
            exitScale.snapTo(1f)
            livePlaybackArmed = true
            return@LaunchedEffect
        }

        livePlaybackArmed = false
        try {
            val startX = when (undoDirection) {
                SwipeDirection.Left -> -screenWidthPx * 1.2f
                SwipeDirection.Right -> screenWidthPx * 1.2f
                else -> 0f
            }
            val startY = when (undoDirection) {
                SwipeDirection.Up -> -screenHeightPx * 1.2f
                SwipeDirection.Down -> screenHeightPx * 1.2f
                else -> 0f
            }
            isTrackingDragOffsets = false
            offsetX.snapTo(startX)
            offsetY.snapTo(startY)
            alphaAnim.snapTo(1f)
            exitScale.snapTo(0.985f)
            val offsetSpring = spring<Float>(
                stiffness = 520f,
                dampingRatio = 0.92f
            )
            val scaleSpring = spring<Float>(
                stiffness = 640f,
                dampingRatio = 0.86f
            )
            val offsetXJob = launch { offsetX.animateTo(0f, offsetSpring) }
            val offsetYJob = launch { offsetY.animateTo(0f, offsetSpring) }
            val scaleJob = launch { exitScale.animateTo(1f, scaleSpring) }
            joinAll(offsetXJob, offsetYJob, scaleJob)
        } finally {
            withContext(NonCancellable) {
                isTrackingDragOffsets = false
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
                alphaAnim.snapTo(1f)
                exitScale.snapTo(1f)
                livePlaybackArmed = true
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(isTopCard, isExpanded, item.id, item.uri, item.type) {
        if (!isTopCard || isExpanded || item.type == "video") return@LaunchedEffect
        if (AppPerformanceGovernor.isRealtimeMemoryPressureHigh()) return@LaunchedEffect
        val appContext = context.applicationContext
        val request = buildHomeCardHoverStillImageRequest(
            context = appContext,
            uri = item.uri,
            itemId = item.id,
            crossfade = false
        )
        withContext(Dispatchers.IO) {
            runCatching { appContext.imageLoader.execute(request) }
        }
    }
    val currentLang = LocalAppLanguage.current.code
    val cardAnalysis = rememberCardAnalysisUiState(
        context = context,
        item = item,
        currentLang = currentLang,
        shouldAnalyze = shouldAnalyze,
        deferHeavyCardWork = deferHeavyCardWork,
        isTopCard = isTopCard,
        isExpanded = isExpanded,
        isCardDragging = isCardDragging,
        lensyEnabled = lensyEnabled,
        onAiOverlayStateChanged = onAiOverlayStateChanged
    )
    val cardAnalysisState = cardAnalysis.uiState
    val currentItem = cardAnalysis.currentItem
    val mlLabels = cardAnalysis.mlLabels
    val livePlaybackItem = remember(
        item.id,
        item.uri,
        item.livePhotoVideoUri,
        currentItem.id,
        currentItem.uri,
        currentItem.isMotionPhoto
    ) {
        if (currentItem.isMotionPhoto) {
            currentItem
        } else {
            item.copy(uri = item.livePhotoVideoUri ?: item.uri, mimeType = "video/mp4")
        }
    }
    val resolvedCardWidthFraction = cardWidthFraction.coerceIn(0.4f, 1f)
    val resolvedCardHeightFraction = cardHeightFraction.coerceIn(0.3f, 1f)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(0f, cardBiasY)
    ) {
        val widthCap = if (cardMaxWidth != Dp.Unspecified) {
            cardMaxWidth
        } else {
            Dp.Infinity
        }
        val heightCap = if (cardMaxHeight != Dp.Unspecified) {
            (maxHeight * resolvedCardHeightFraction).coerceAtMost(cardMaxHeight)
        } else {
            maxHeight * resolvedCardHeightFraction
        }
        val baseCardWidth = (maxWidth * resolvedCardWidthFraction).coerceAtMost(widthCap)
        val derivedAspectRatio = if (heightCap.value > 0f) {
            baseCardWidth.value / heightCap.value
        } else {
            0f
        }
        val resolvedAspectRatio = cardAspectRatio
            ?.takeIf { it > 0f && it.isFinite() }
            ?: derivedAspectRatio
                .let { ratio ->
                    val minAspect = cardMinAspectRatio
                        ?.takeIf { it > 0f && it.isFinite() }
                        ?: ratio
                    val maxAspect = cardMaxAspectRatio
                        ?.takeIf { it > 0f && it.isFinite() }
                        ?: ratio
                    when {
                        ratio <= 0f || !ratio.isFinite() -> minAspect
                        minAspect <= maxAspect -> ratio.coerceIn(minAspect, maxAspect)
                        else -> ratio.coerceIn(maxAspect, minAspect)
                    }
                }
        val baseCardHeight = if (resolvedAspectRatio > 0f && resolvedAspectRatio.isFinite()) {
            baseCardWidth / resolvedAspectRatio
        } else {
            heightCap
        }
        val cardHeight: Dp
        val cardWidth: Dp
        if (cardBoxWidth != Dp.Unspecified && cardBoxHeight != Dp.Unspecified) {
            cardWidth = cardBoxWidth
            cardHeight = cardBoxHeight
        } else if (baseCardHeight > heightCap && resolvedAspectRatio > 0f && resolvedAspectRatio.isFinite()) {
            cardHeight = heightCap
            cardWidth = heightCap * resolvedAspectRatio
        } else {
            cardHeight = baseCardHeight
            cardWidth = baseCardWidth
        }
        // Photo-frame mode sizes the moving element to the photo rect (rounded to whole px),
        // so shadow, clip, drag and exits all follow the photo itself.
        val visibleCardWidth: Dp
        val visibleCardHeight: Dp
        if (photoFrameMode) {
            val photoSize = resolveHomeCardPhotoSize(
                boxWidth = cardWidth,
                boxHeight = cardHeight,
                aspectRatio = resolveHomeCardPhotoAspectRatio(item)
            )
            visibleCardWidth = with(density) { photoSize.width.toPx().roundToInt().toDp() }
            visibleCardHeight = with(density) { photoSize.height.toPx().roundToInt().toDp() }
        } else {
            visibleCardWidth = cardWidth
            visibleCardHeight = cardHeight
        }
        // The whole card slot grabs the top card, not just the photo: a small or wide photo can be
        // flicked from anywhere around it. The snapshot and pointer math stay in photo coordinates.
        val touchCardWidth = if (photoFrameMode) {
            maxOf(visibleCardWidth, cardWidth)
        } else {
            maxOf(visibleCardWidth, minOf(cardWidth, HOME_CARD_MIN_TOUCH_SIZE))
        }
        val touchCardHeight = if (photoFrameMode) {
            maxOf(visibleCardHeight, cardHeight)
        } else {
            maxOf(visibleCardHeight, minOf(cardHeight, HOME_CARD_MIN_TOUCH_SIZE))
        }
        val touchPaddingPx = with(density) {
            Offset(
                x = ((touchCardWidth - visibleCardWidth) / 2f).toPx(),
                y = ((touchCardHeight - visibleCardHeight) / 2f).toPx()
            )
        }
        val currentTouchPaddingPx by rememberUpdatedState(touchPaddingPx)
        val commitThresholdPx = resolveSwipeCommitThresholdDp(
            cardWidthDp = cardWidth.value,
            cardHeightDp = cardHeight.value
        ) * pxPerDp
        val currentHorizontalThresholdPx by rememberUpdatedState(commitThresholdPx)
        val currentVerticalThresholdPx by rememberUpdatedState(commitThresholdPx * SWIPE_COMMIT_VERTICAL_RATIO)
        val cardShape = if (photoFrameMode) {
            RoundedCornerShape(HOME_CARD_PHOTO_CORNER_RADIUS)
        } else {
            RoundedCornerShape(24.dp)
        }
        val photoFrameShadow = remember(isDark) {
            if (isDark) {
                Shadow(12.dp, Color.Black, 0.dp, DpOffset(0.dp, 7.dp), 0.48f)
            } else {
                Shadow(6.dp, Color.Black, 0.dp, DpOffset(0.dp, 4.dp), 0.14f)
            }
        }
        val photoFrameBorderBrush = remember(isDark) {
            if (isDark) {
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.08f)))
            } else {
                SolidColor(Color.Black.copy(alpha = 0.06f))
            }
        }
        val cardChromeModifier = if (photoFrameMode) {
            Modifier
                .then(if (stackIndex > 2) Modifier else Modifier.dropShadow(cardShape, photoFrameShadow))
                .clip(cardShape)
                .background(organizePanelColor(isDark))
                .border(if (isDark) 1.dp else 0.5.dp, photoFrameBorderBrush, cardShape)
        } else {
            Modifier
                .shadow(
                    elevation = if (stackIndex > 2) 0.dp else 12.dp,
                    shape = cardShape
                )
                .clip(cardShape)
                .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
                .border(
                    1.dp,
                    if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.Transparent,
                    cardShape
                )
        }

        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        val visualState = currentCardVisualState()
                        val stackedYOffset = with(density) { yOffset.toPx().roundToInt() }
                        IntOffset(
                            visualState.visualOffsetX.roundToInt(),
                            visualState.visualOffsetY.roundToInt() + stackedYOffset
                        )
                    }
                    .width(touchCardWidth)
                    .height(touchCardHeight)
                    .onGloballyPositioned { coordinates ->
                        val touchBounds = coordinates.boundsInRoot()
                        val bounds = Rect(
                            left = touchBounds.left + touchPaddingPx.x,
                            top = touchBounds.top + touchPaddingPx.y,
                            right = touchBounds.right - touchPaddingPx.x,
                            bottom = touchBounds.bottom - touchPaddingPx.y
                        )
                        dragLayoutSnapshot.cardPositionInRoot = Offset(bounds.left, bounds.top)
                        if (isTopCard && (!isCardDragging || isFolderDragMode)) {
                            currentOnTopCardBoundsChanged?.invoke(bounds)
                        }
                    }
                    .graphicsLayer {
                        val visualState = currentCardVisualState()
                        scaleX = visualState.totalScaleX
                        scaleY = visualState.totalScaleY
                        alpha = alphaAnim.value
                        rotationZ = visualState.swipeTilt
                    }
                    .pointerInput(isTopCard, isExpanded) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            if (isTopCard && !isExpanded) {
                                isPressed = true
                                waitForUpOrCancellation()
                                isPressed = false
                            }
                        }
                    }
                    .pointerInput(isTopCard, isExpanded, isVideoSeeking) {
                        if (!isTopCard || isExpanded || isVideoSeeking) return@pointerInput

                        val velocityTracker = VelocityTracker()

                        detectDragGestures(
                            onDragStart = { touchAreaOffset ->
                                val touchOffset = touchAreaOffset - currentTouchPaddingPx
                                dragStartUptimeMs = SystemClock.uptimeMillis()
                                dragStartOffset = Offset(offsetX.value, offsetY.value)
                                isPressed = true
                                dismissImageLongPressHover()
                                cardAnalysisState.clearAiOverlay()
                                scope.launch {
                                    offsetX.stop()
                                    offsetY.stop()
                                    alphaAnim.stop()
                                    exitScale.stop()
                                    if (alphaAnim.value != 1f) alphaAnim.snapTo(1f)
                                    if (exitScale.value != 1f) exitScale.snapTo(1f)
                                }
                                gestureState.beginDrag(currentOnSwipeIntentPreview)
                                exitTiltOverride = Float.NaN
                                intentBadgeDirection = SwipeIntentBadgeDirection.None
                                isCardDragging = true
                                isTrackingDragOffsets = true
                                isFolderDragMode = false
                                lastReportedDragQuantizedX = Int.MIN_VALUE
                                lastReportedDragQuantizedY = Int.MIN_VALUE
                                dragOffsetX = offsetX.value
                                dragOffsetY = offsetY.value
                                isDraggingWithTouchAnchor = true
                                val dragStartVisualState = currentCardVisualState()
                                dragStartScaleX = dragStartVisualState.totalScaleX
                                dragStartScaleY = dragStartVisualState.totalScaleY
                                val cardSizePx = dragLayoutSnapshot.cardSizePx
                                dragAnchorFromCenter = if (cardSizePx.width > 0 && cardSizePx.height > 0) {
                                    // Grabbed beside the photo: anchor at the nearest point of the photo.
                                    Offset(
                                        x = touchOffset.x.coerceIn(0f, cardSizePx.width.toFloat()) - (cardSizePx.width / 2f),
                                        y = touchOffset.y.coerceIn(0f, cardSizePx.height.toFloat()) - (cardSizePx.height / 2f)
                                    )
                                } else {
                                    Offset.Zero
                                }
                                gestureState.setPointerFromTouch(
                                    cardPositionInRoot = dragLayoutSnapshot.cardPositionInRoot,
                                    touchOffset = touchOffset,
                                    onDragPointerUpdate = currentOnDragPointerUpdate
                                )
                                velocityTracker.resetTracking()
                                currentOnDragStart?.invoke()
                            },
                            onDragCancel = {
                                isPressed = false
                                dismissImageLongPressHover()
                                isDraggingWithTouchAnchor = false
                                isFolderDragMode = false
                                dragAnchorFromCenter = Offset.Zero
                                gestureState.clearPointer()
                                gestureState.resetSwipeFeedback(currentOnSwipeIntentPreview)
                                intentBadgeDirection = SwipeIntentBadgeDirection.None
                                currentOnSwipeGlowFrame?.invoke(dragOffsetX, dragOffsetY, currentHorizontalThresholdPx, false)
                                currentOnDragCancel?.invoke()
                                scope.launch {
                                    try {
                                        offsetX.snapTo(dragOffsetX)
                                        offsetY.snapTo(dragOffsetY)
                                        isTrackingDragOffsets = false
                                        val snapBackSpring = spring<Float>(stiffness = 900f, dampingRatio = 0.82f)
                                        launch { offsetX.animateTo(0f, snapBackSpring) }
                                        launch { offsetY.animateTo(0f, snapBackSpring) }
                                    } finally {
                                        isCardDragging = false
                                    }
                                }
                            },
                            onDragEnd = {
                                isPressed = false
                                dismissImageLongPressHover()
                                isDraggingWithTouchAnchor = false
                                isFolderDragMode = false
                                dragAnchorFromCenter = Offset.Zero
                                gestureState.clearPointer()

                                val velocity = velocityTracker.calculateVelocity()
                                val nowMs = SystemClock.uptimeMillis()
                                // A short, quick gesture can carry too few move events for the tracker;
                                // then the average speed of the whole drag stands in for the release speed.
                                val dragDurationMs = (nowMs - dragStartUptimeMs).coerceAtLeast(1L)
                                val trackerSpeed = sqrt(velocity.x * velocity.x + velocity.y * velocity.y)
                                val (releaseVx, releaseVy) = if (trackerSpeed < 1f && dragDurationMs <= 300L) {
                                    ((dragOffsetX - dragStartOffset.x) * 1000f / dragDurationMs) to
                                        ((dragOffsetY - dragStartOffset.y) * 1000f / dragDurationMs)
                                } else {
                                    velocity.x to velocity.y
                                }
                                val releaseVelocity = DraggableCardReleaseVelocity(
                                    x = releaseVx,
                                    y = releaseVy,
                                    speed = sqrt(releaseVx * releaseVx + releaseVy * releaseVy)
                                )
                                currentOnDragVelocityUpdate?.invoke(releaseVelocity.speed)
                                val isFolderModeNow =
                                    !disableDownSwipeGestures &&
                                        currentResolvedOffsetY() > DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX
                                val finalIntent = resolveReleaseSwipeIntent(
                                    netXDp = dragOffsetX / pxPerDp,
                                    netYDp = dragOffsetY / pxPerDp,
                                    velocityXDp = releaseVelocity.x / pxPerDp,
                                    velocityYDp = releaseVelocity.y / pxPerDp,
                                    horizontalThresholdDp = currentHorizontalThresholdPx / pxPerDp,
                                    verticalThresholdDp = currentVerticalThresholdPx / pxPerDp,
                                    isFolderMode = isFolderModeNow,
                                    allowSwipeLeft = allowSwipeLeft,
                                    allowSwipeRight = allowSwipeRight,
                                    allowSwipeUp = allowSwipeUp
                                )
                                val releaseSpeedDp = releaseVelocity.speed / pxPerDp
                                intentBadgeDirection = SwipeIntentBadgeDirection.None
                                currentOnSwipeGlowFrame?.invoke(dragOffsetX, dragOffsetY, currentHorizontalThresholdPx, false)
                                gestureState.applyResolvedIntent(
                                    intent = finalIntent.intent,
                                    strength = finalIntent.strength,
                                    onSwipeIntentPreview = currentOnSwipeIntentPreview
                                )

                                scope.launch {
                                    try {
                                        offsetX.snapTo(dragOffsetX)
                                        offsetY.snapTo(dragOffsetY)
                                        isTrackingDragOffsets = false
                                        val postDragCommit = currentOnDragEnd?.invoke(dragOffsetX, dragOffsetY)

                                        if (postDragCommit != null) {
                                            val settleOffsetY = (dragOffsetY + (screenHeightPx * 0.10f).coerceAtLeast(92f))
                                                .coerceAtMost(screenHeightPx * 0.78f)
                                            val settleOffsetX = dragOffsetX * 0.9f
                                            val settleDurationMs = 220
                                            val settleJobs = listOf(
                                                launch {
                                                    exitScale.animateTo(
                                                        0.9f,
                                                        tween(settleDurationMs, easing = FastOutSlowInEasing)
                                                    )
                                                },
                                                launch {
                                                    alphaAnim.animateTo(
                                                        0f,
                                                        tween(settleDurationMs - 20, easing = LinearOutSlowInEasing)
                                                    )
                                                },
                                                launch {
                                                    offsetX.animateTo(
                                                        settleOffsetX,
                                                        tween(settleDurationMs, easing = FastOutSlowInEasing)
                                                    )
                                                },
                                                launch {
                                                    offsetY.animateTo(
                                                        settleOffsetY,
                                                        tween(settleDurationMs, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                            )
                                            settleJobs.joinAll()
                                            postDragCommit()
                                            return@launch
                                        }

                                        if (finalIntent.intent == SwipeIntent.Up) {
                                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            playSoundForAction(upAction)
                                            currentOnSwipeCommitStart?.invoke(SwipeIntent.Up)
                                            val upExitDurationMs = resolveSwipeExitDurationMs(
                                                releaseSpeedDpPerSec = releaseSpeedDp,
                                                speedMultiplier = upSwipeSpeedMultiplier,
                                                defaultSpeedMultiplier = DEFAULT_UP_SWIPE_SPEED_MULTIPLIER
                                            )
                                            alphaAnim.snapTo(1f)
                                            // Vertical exits keep the residual drag tilt instead of un-tilting mid-flight.
                                            exitTiltOverride = currentCardVisualState().swipeTilt
                                            val cardHeightPx = dragLayoutSnapshot.cardSizePx.height.toFloat()
                                            val upExitTarget = resolveUpExitTargetPx(
                                                screenHeightPx = screenHeightPx,
                                                cardHeightPx = cardHeightPx
                                            )
                                            val upExitSpec = tween<Float>(
                                                durationMillis = upExitDurationMs,
                                                easing = LinearEasing
                                            )
                                            val upExitJobs = listOf(
                                                launch { offsetY.animateTo(upExitTarget, upExitSpec) },
                                                launch { offsetX.animateTo(dragOffsetX * 0.3f, upExitSpec) },
                                                launch { exitScale.animateTo(SWIPE_EXIT_SCALE, upExitSpec) }
                                            )
                                            upExitJobs.joinAll()
                                            currentOnSwipeUp()
                                        } else if (
                                            finalIntent.intent == SwipeIntent.Left ||
                                            finalIntent.intent == SwipeIntent.Right
                                        ) {
                                            val isRight = finalIntent.intent == SwipeIntent.Right
                                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            playSoundForAction(if (isRight) rightAction else leftAction)
                                            currentOnSwipeCommitStart?.invoke(finalIntent.intent)
                                            val horizontalExitDistance = maxOf(
                                                screenWidthPx * HORIZONTAL_EXIT_DISTANCE_MULTIPLIER,
                                                HORIZONTAL_EXIT_MIN_TARGET.value * pxPerDp
                                            )
                                            val horizontalExitDurationMs = resolveSwipeExitDurationMs(
                                                releaseSpeedDpPerSec = releaseSpeedDp,
                                                speedMultiplier = horizontalSwipeSpeedMultiplier,
                                                defaultSpeedMultiplier = DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER
                                            )
                                            val horizontalExitSpec = tween<Float>(
                                                durationMillis = horizontalExitDurationMs,
                                                easing = SwipeExitEaseIn
                                            )
                                            // Tilt ramps to the +-11deg clamp on its own as offsetX keeps growing.
                                            val horizontalExitJobs = listOf(
                                                launch {
                                                    offsetX.animateTo(
                                                        if (isRight) horizontalExitDistance else -horizontalExitDistance,
                                                        horizontalExitSpec
                                                    )
                                                },
                                                launch { exitScale.animateTo(SWIPE_EXIT_SCALE, horizontalExitSpec) }
                                            )
                                            horizontalExitJobs.joinAll()
                                            if (isRight) currentOnSwipeRight() else currentOnSwipeLeft()
                                        } else {
                                            // Hand the release velocity to the settle spring so there is no seam.
                                            val snapBackSpring = spring<Float>(stiffness = 900f, dampingRatio = 0.82f)
                                            launch {
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = snapBackSpring,
                                                    initialVelocity = releaseVelocity.x
                                                )
                                            }
                                            launch {
                                                offsetY.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = snapBackSpring,
                                                    initialVelocity = releaseVelocity.y
                                                )
                                            }
                                            gestureState.resetSwipeFeedback(currentOnSwipeIntentPreview)
                                        }
                                    } finally {
                                        isCardDragging = false
                                    }
                                }
                            },
                            onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                change.consume()
                                // Pointer positions are local to the card, which itself moves with the finger;
                                // adding the card's travel so far turns them into screen-space samples.
                                val cardTravel = Offset(dragOffsetX, dragOffsetY)
                                change.historical.forEach { sample ->
                                    velocityTracker.addPosition(sample.uptimeMillis, sample.position + cardTravel)
                                }
                                velocityTracker.addPosition(change.uptimeMillis, change.position + cardTravel)

                                val dragResult = gestureState.applyDragDelta(
                                    dragAmount = dragAmount,
                                    currentOffsetX = dragOffsetX,
                                    currentOffsetY = dragOffsetY,
                                    resolvedOffsetY = currentResolvedOffsetY(),
                                    disableDownSwipeGestures = disableDownSwipeGestures,
                                    dragGainXStart = dragGainXStart,
                                    dragGainXRampDistancePx = dragGainXRampDistancePx,
                                    dragGainY = dragGainY
                                )
                                val newX = dragResult.offsetX
                                val newY = dragResult.offsetY
                                dragOffsetX = newX
                                dragOffsetY = newY
                                val nextFolderDragMode =
                                    !disableDownSwipeGestures && newY > DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX
                                if (isFolderDragMode != nextFolderDragMode) {
                                    isFolderDragMode = nextFolderDragMode
                                }

                                gestureState.updatePreviewDuringDrag(
                                    uptimeMillis = change.uptimeMillis,
                                    position = change.position - currentTouchPaddingPx,
                                    dragAmount = dragAmount,
                                    cardPositionInRoot = dragLayoutSnapshot.cardPositionInRoot,
                                    currentOffsetY = newY,
                                    disableDownSwipeGestures = disableDownSwipeGestures,
                                    adaptiveVerticalSwipeThresholdPx = currentVerticalThresholdPx,
                                    adaptiveHorizontalSwipeThresholdPx = currentHorizontalThresholdPx,
                                    predictedSwipeIntent = predictedSwipeIntent,
                                    predictionBias = predictionBias,
                                    allowSwipeLeft = allowSwipeLeft,
                                    allowSwipeRight = allowSwipeRight,
                                    allowSwipeUp = allowSwipeUp,
                                    onDragVelocityUpdate = currentOnDragVelocityUpdate,
                                    onSwipeIntentPreview = currentOnSwipeIntentPreview,
                                    onDragPointerUpdate = currentOnDragPointerUpdate
                                )

                                val predictedBadge = predictSwipeIntentBadgeDirection(
                                    netXDp = newX / pxPerDp,
                                    netYDp = newY / pxPerDp,
                                    allowDown = !disableDownSwipeGestures
                                ).let { direction ->
                                    when (direction) {
                                        SwipeIntentBadgeDirection.Left ->
                                            if (allowSwipeLeft) direction else SwipeIntentBadgeDirection.None
                                        SwipeIntentBadgeDirection.Right ->
                                            if (allowSwipeRight) direction else SwipeIntentBadgeDirection.None
                                        SwipeIntentBadgeDirection.Up ->
                                            if (allowSwipeUp) direction else SwipeIntentBadgeDirection.None
                                        else -> direction
                                    }
                                }
                                if (intentBadgeDirection != predictedBadge) {
                                    intentBadgeDirection = predictedBadge
                                }
                                currentOnSwipeGlowFrame?.invoke(newX, newY, currentHorizontalThresholdPx, true)

                                if (isTopCard && currentOnDragUpdate != null) {
                                    val visualState = currentCardVisualState()
                                    val dragUpdateQuantizeFactor = if (
                                        visualState.visualOffsetY > (DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX * 0.68f)
                                    ) {
                                        0.25f
                                    } else {
                                        0.1f
                                    }
                                    val quantizedX = (visualState.visualOffsetX * dragUpdateQuantizeFactor).roundToInt()
                                    val quantizedY = (visualState.visualOffsetY * dragUpdateQuantizeFactor).roundToInt()
                                    if (
                                        quantizedX != lastReportedDragQuantizedX ||
                                        quantizedY != lastReportedDragQuantizedY
                                    ) {
                                        lastReportedDragQuantizedX = quantizedX
                                        lastReportedDragQuantizedY = quantizedY
                                        currentOnDragUpdate?.invoke(
                                            visualState.visualOffsetX,
                                            visualState.visualOffsetY
                                        )
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(isTopCard) {
                        if (!isTopCard || isExpanded) return@pointerInput
                        if (item.type == "video") return@pointerInput
                        detectTapGestures(
                            onTap = { tapOffset ->
                                // Only the photo opens the viewer; the grab area around it does not.
                                val padding = currentTouchPaddingPx
                                val onPhoto = tapOffset.x >= padding.x && tapOffset.x <= size.width - padding.x &&
                                    tapOffset.y >= padding.y && tapOffset.y <= size.height - padding.y
                                if (!onPhoto) return@detectTapGestures
                                val nowUptimeMs = SystemClock.uptimeMillis()
                                if (
                                    suppressCardTapOnce ||
                                    isImageLongPressHoverActive ||
                                    isImageLongPressHoverPresented ||
                                    nowUptimeMs < suppressCardTapUntilUptimeMs
                                ) {
                                    suppressCardTapOnce = false
                                } else {
                                    val openDetailWithBounds: (Rect?) -> Unit = { sourceBounds ->
                                        if (onDetailWithSourceBounds != null) {
                                            onDetailWithSourceBounds(sourceBounds)
                                        } else {
                                            onDetail()
                                        }
                                    }
                                    val computeSourceBoundsFromImage: () -> Rect? = {
                                        dragLayoutSnapshot.imageBoundsInRoot?.takeIf {
                                            it.width > 1f && it.height > 1f
                                        }
                                    }
                                    val computeSourceBoundsWithCardFallback: () -> Rect? = {
                                        computeSourceBoundsFromImage() ?: run {
                                            val cardSizePx = dragLayoutSnapshot.cardSizePx
                                            if (cardSizePx.width <= 0 || cardSizePx.height <= 0) {
                                                null
                                            } else {
                                                val visualState = currentCardVisualState()
                                                val sourceScaleX = visualState.totalScaleX
                                                val sourceScaleY = visualState.totalScaleY
                                                val cardLeft = dragLayoutSnapshot.cardPositionInRoot.x
                                                val cardTop = dragLayoutSnapshot.cardPositionInRoot.y
                                                val cardWidth = cardSizePx.width.toFloat()
                                                val cardHeight = cardSizePx.height.toFloat()
                                                val scaledWidth = cardWidth * sourceScaleX
                                                val scaledHeight = cardHeight * sourceScaleY
                                                val left = cardLeft + ((cardWidth - scaledWidth) / 2f)
                                                val top = cardTop + ((cardHeight - scaledHeight) / 2f)
                                                Rect(
                                                    left = left,
                                                    top = top,
                                                    right = left + scaledWidth,
                                                    bottom = top + scaledHeight
                                                )
                                            }
                                        }
                                    }

                                    val sourceBounds = computeSourceBoundsFromImage()
                                    if (sourceBounds != null) {
                                        openDetailWithBounds(sourceBounds)
                                    } else {
                                        scope.launch {
                                            var resolvedFromImage: Rect? = null
                                            var remainingFrames = 12
                                            while (remainingFrames > 0) {
                                                withFrameNanos { }
                                                resolvedFromImage = computeSourceBoundsFromImage()
                                                if (resolvedFromImage != null) break
                                                remainingFrames -= 1
                                            }
                                            openDetailWithBounds(resolvedFromImage ?: computeSourceBoundsWithCardFallback())
                                        }
                                    }
                                }
                            }
                        )
                    }
                    .wrapContentSize(Alignment.Center, unbounded = true)
                    .width(visibleCardWidth)
                    .height(visibleCardHeight)
                    .onSizeChanged { dragLayoutSnapshot.cardSizePx = it }
                    .then(cardChromeModifier)
            ) {
                DraggableCardFrontContent(
                    item = item,
                    currentItem = currentItem,
                    livePlaybackItem = livePlaybackItem,
                    stillImageRequest = stillImageRequest,
                    videoFrameRequest = videoFrameRequest,
                    shouldRenderVideoPlayer = shouldRenderVideoPlayer,
                    shouldPlayVideo = shouldPlayVideo,
                    isCardDragging = isCardDragging,
                    dragPauseToken = dragPauseToken,
                    onStartupSample = onStartupSample,
                    onVideoSeekingChange = { isVideoSeeking = it },
                    onOpenFullScreenVideo = onFullScreenVideo,
                    isFullScreenVideoOpen = isFullScreenVideoOpen,
                    isMuted = isMuted,
                    onMutedChange = { newState ->
                        isMuted = newState
                        onMuteChange?.invoke(newState)
                    },
                    livePlaybackArmed = livePlaybackArmed,
                    livePlaybackCompleted = livePlaybackCompleted,
                    liveReplayToken = liveReplayToken,
                    onLivePlaybackCompletedChange = { livePlaybackCompleted = it },
                    onLiveReplayRequest = {
                        suppressCardTapOnce = true
                        livePlaybackCompleted = false
                        liveReplayToken += 1L
                    },
                    imageLongPressHoverEnabled =
                        colorCardLongPressEnabled && isTopCard && !isExpanded && item.type != "video",
                    longPressOverlaySourceImageAlpha =
                        if (
                            isTopCard &&
                            !isExpanded &&
                            !isCardDragging &&
                            isImageLongPressHoverVisualsActive
                        ) {
                            (1f - hoverVisuals.imageOverlayAlpha).coerceIn(0f, 1f)
                        } else {
                            1f
                        },
                    imageLongPressHoverVisuals = hoverVisuals,
                    onImageLongPressHoverChange = { active ->
                        if (!isCardDragging) {
                            suppressCardTapUntilUptimeMs = maxOf(
                                suppressCardTapUntilUptimeMs,
                                SystemClock.uptimeMillis() + if (active) 1200L else 350L
                            )
                            isImageLongPressHoverActive = active
                            if (active) {
                                isImageLongPressHoverVisualReady = false
                                isImageLongPressHoverPresented = true
                                val fallbackBackdrop =
                                    captureHomeCardLongPressBackdropBitmapFallback(rootWindowView)
                                hoverBackdropBitmap = fallbackBackdrop
                                scope.launch {
                                    val captured =
                                        captureHomeCardLongPressBackdropBitmap(
                                            activity = activity,
                                            rootView = rootWindowView,
                                            fallbackBitmap = fallbackBackdrop
                                        ) ?: return@launch
                                    if (isImageLongPressHoverPresented) {
                                        hoverBackdropBitmap = captured
                                    }
                                }
                            }
                        }
                    },
                    mlLabels = mlLabels,
                    showMlLabelsOnHome = showMlLabelsOnHome,
                    compactFooter = compactFooter,
                    footerSecondaryText = footerSecondaryText,
                    footerSecondaryTextColor = footerSecondaryTextColor,
                    trackImageBoundsInRoot = isTopCard,
                    forceImagePlaceholder = forceHidden,
                    onImageBoundsInRootChanged = { bounds ->
                        dragLayoutSnapshot.imageBoundsInRoot = bounds
                        if (isTopCard && !isCardDragging) {
                            currentOnTopCardImageBoundsChanged?.invoke(bounds)
                        }
                    },
                    onExpandDetails = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = true
                    },
                    photoFrameMode = photoFrameMode,
                    onMediaAspectRatioResolved = if (photoFrameMode) { aspectRatio ->
                        HomeCardPhotoAspectCache.put(item.id, aspectRatio)
                    } else {
                        null
                    }
                )

                DraggableCardActionIndicators(
                    isTopCard = isTopCard,
                    isExpanded = isExpanded,
                    suppressActionIndicators = suppressActionIndicators,
                    isFolderMode = intentBadgeDirection == SwipeIntentBadgeDirection.Down,
                    swipeIntentPreview = when (intentBadgeDirection) {
                        SwipeIntentBadgeDirection.Left -> SwipeIntent.Left
                        SwipeIntentBadgeDirection.Right -> SwipeIntent.Right
                        SwipeIntentBadgeDirection.Up -> SwipeIntent.Up
                        else -> SwipeIntent.None
                    },
                    upAction = upAction,
                    rightAction = rightAction,
                    leftAction = leftAction,
                    upIndicatorSpec = upIndicatorSpec,
                    rightIndicatorSpec = rightIndicatorSpec,
                    leftIndicatorSpec = leftIndicatorSpec
                )

            }

            // Details open in a bottom sheet: the card is sized to the photo, too small for the grid.
            DraggableCardDetailsSheet(
                isExpanded = isExpanded,
                currentItem = currentItem,
                onDismiss = { isExpanded = false },
                onJumpToDay = onJumpToDay
            )
        }

        HomeCardTraceEditingOverlay(
            active = isImageLongPressHoverPresented,
            visuals = hoverVisuals,
            backdropBitmap = hoverBackdropBitmap,
            item = currentItem,
            imageBoundsInRoot = dragLayoutSnapshot.imageBoundsInRoot,
            windowSizePx = hoverWindowSizePx,
            isDark = isDark,
            onImageReady = {
                if (isImageLongPressHoverPresented && !isImageLongPressHoverVisualReady) {
                    isImageLongPressHoverVisualReady = true
                }
            },
            onDismissRequest = dismissImageLongPressHover
        )
    }
}
