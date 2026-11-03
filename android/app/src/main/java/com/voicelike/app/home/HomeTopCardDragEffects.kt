package com.voicelike.app

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val HOME_FOLDER_DROP_EDGE_THRESHOLD_PX = 152f
private const val HOME_FOLDER_DROP_EDGE_MIN_SCROLL_SPEED = 5f
private const val HOME_FOLDER_DROP_EDGE_MAX_SCROLL_SPEED = 28f
private const val HOME_FOLDER_DROP_EDGE_SCROLL_INTERVAL_MS = 12L

@Composable
internal fun BoxScope.HomeTopCardEdgeGlow(
    visible: Boolean,
    swipeIntent: SwipeIntent,
    swipeStrength: Float,
    isUndoAnimating: Boolean,
    isFolderDropMode: Boolean,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction
) {
    if (!visible || swipeIntent == SwipeIntent.None || isUndoAnimating) return

    val maxOpacity = 0.6f
    val opacity = (swipeStrength * maxOpacity).coerceIn(0f, maxOpacity)
    val leftGlowColor = swipeActionColor(swipeLeftAction)
    val rightGlowColor = swipeActionColor(swipeRightAction)
    val upGlowColor = swipeActionColor(swipeUpAction)

    val upGradient = remember(upGlowColor) {
        Brush.verticalGradient(
            colors = listOf(upGlowColor, Color.Transparent)
        )
    }
    val leftGradient = remember(leftGlowColor) {
        Brush.horizontalGradient(
            colors = listOf(leftGlowColor.copy(alpha = 0.9f), Color.Transparent)
        )
    }
    val rightGradient = remember(rightGlowColor) {
        Brush.horizontalGradient(
            colors = listOf(Color.Transparent, rightGlowColor.copy(alpha = 0.9f))
        )
    }

    when {
        swipeIntent == SwipeIntent.Up -> {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .graphicsLayer(alpha = opacity)
                    .background(brush = upGradient)
            )
        }

        swipeIntent == SwipeIntent.Right && !isFolderDropMode -> {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f)
                    .graphicsLayer(alpha = opacity)
                    .background(brush = rightGradient)
            )
        }

        swipeIntent == SwipeIntent.Left && !isFolderDropMode -> {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f)
                    .graphicsLayer(alpha = opacity)
                    .background(brush = leftGradient)
            )
        }
    }
}

private val HOME_EDGE_GLOW_SIDE_DEPTH = 150.dp
private val HOME_EDGE_GLOW_TOP_DEPTH = 210.dp
private const val HOME_EDGE_GLOW_RELEASE_FADE_MS = 280
private val HomeEdgeGlowEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/**
 * Per-frame edge glow intensities driven straight from the top card's drag offset.
 * Values are read only in the draw phase, so dragging redraws the glow without recomposing.
 */
@Stable
internal class HomeTopCardEdgeGlowState(private val scope: CoroutineScope) {
    var left by mutableFloatStateOf(0f)
        private set
    var right by mutableFloatStateOf(0f)
        private set
    var top by mutableFloatStateOf(0f)
        private set
    var fade by mutableFloatStateOf(1f)
        private set
    private var fadeJob: Job? = null

    /** Intensity = min(distance / threshold, 1) x directionShare^2; off while dragging down. */
    fun onDragFrame(
        offsetXPx: Float,
        offsetYPx: Float,
        horizontalThresholdPx: Float,
        folderDropThresholdPx: Float
    ) {
        val ax = abs(offsetXPx)
        val ay = abs(offsetYPx)
        val draggingDown = offsetYPx > 0f && ay > ax * 0.6f
        if (draggingDown || offsetYPx > folderDropThresholdPx) {
            fadeOut()
            return
        }
        fadeJob?.cancel()
        fadeJob = null
        fade = 1f
        val hThreshold = horizontalThresholdPx.coerceAtLeast(1f)
        val vThreshold = (hThreshold * SWIPE_COMMIT_VERTICAL_RATIO).coerceAtLeast(1f)
        val up = (-offsetYPx).coerceAtLeast(0f)
        val total = (ax + ay).coerceAtLeast(1f)
        val horizontalShare = ax / total
        val verticalShare = up / total
        val horizontal = (ax / hThreshold).coerceAtMost(1f) * horizontalShare * horizontalShare
        val vertical = (up / vThreshold).coerceAtMost(1f) * verticalShare * verticalShare
        left = if (offsetXPx < 0f) horizontal else 0f
        right = if (offsetXPx > 0f) horizontal else 0f
        top = vertical
    }

    fun release() {
        fadeOut()
    }

    private fun fadeOut() {
        if (fadeJob?.isActive == true) return
        if (left <= 0f && right <= 0f && top <= 0f) return
        fadeJob = scope.launch {
            animate(
                initialValue = fade,
                targetValue = 0f,
                animationSpec = tween(HOME_EDGE_GLOW_RELEASE_FADE_MS, easing = HomeEdgeGlowEaseOut)
            ) { value, _ -> fade = value }
            left = 0f
            right = 0f
            top = 0f
            fade = 1f
        }
    }
}

@Composable
internal fun rememberHomeTopCardEdgeGlowState(): HomeTopCardEdgeGlowState {
    val scope = rememberCoroutineScope()
    return remember(scope) { HomeTopCardEdgeGlowState(scope) }
}

/** Screen-edge gradients (left / right / top) in the swipe action colours, laid behind the cards. */
@Composable
internal fun BoxScope.HomeTopCardEdgeGlowLayer(
    state: HomeTopCardEdgeGlowState,
    enabled: Boolean,
    leftColor: Color,
    rightColor: Color,
    topColor: Color
) {
    if (!enabled) return
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawWithCache {
                val sideDepth = HOME_EDGE_GLOW_SIDE_DEPTH.toPx().coerceAtMost(size.width)
                val topDepth = HOME_EDGE_GLOW_TOP_DEPTH.toPx().coerceAtMost(size.height)
                val leftBrush = Brush.horizontalGradient(
                    0f to leftColor.copy(alpha = 0.38f),
                    0.45f to leftColor.copy(alpha = 0.12f),
                    1f to leftColor.copy(alpha = 0f),
                    startX = 0f,
                    endX = sideDepth
                )
                val rightBrush = Brush.horizontalGradient(
                    0f to rightColor.copy(alpha = 0.38f),
                    0.45f to rightColor.copy(alpha = 0.12f),
                    1f to rightColor.copy(alpha = 0f),
                    startX = size.width,
                    endX = size.width - sideDepth
                )
                val topBrush = Brush.verticalGradient(
                    0f to topColor.copy(alpha = 0.38f),
                    0.45f to topColor.copy(alpha = 0.12f),
                    1f to topColor.copy(alpha = 0f),
                    startY = 0f,
                    endY = topDepth
                )
                onDrawBehind {
                    val fade = state.fade
                    val leftAlpha = (state.left * fade).coerceIn(0f, 1f)
                    val rightAlpha = (state.right * fade).coerceIn(0f, 1f)
                    val topAlpha = (state.top * fade).coerceIn(0f, 1f)
                    if (leftAlpha > 0.001f) {
                        drawRect(
                            brush = leftBrush,
                            topLeft = Offset.Zero,
                            size = Size(sideDepth, size.height),
                            alpha = leftAlpha
                        )
                    }
                    if (rightAlpha > 0.001f) {
                        drawRect(
                            brush = rightBrush,
                            topLeft = Offset(size.width - sideDepth, 0f),
                            size = Size(sideDepth, size.height),
                            alpha = rightAlpha
                        )
                    }
                    if (topAlpha > 0.001f) {
                        drawRect(
                            brush = topBrush,
                            topLeft = Offset.Zero,
                            size = Size(size.width, topDepth),
                            alpha = topAlpha
                        )
                    }
                }
            }
    )
}

@Composable
internal fun HomeFolderDropEffects(
    isFolderDropMode: Boolean,
    mediaForMoveTargets: List<MediaItem>,
    folderUsageState: Map<String, Int>,
    folderLastUsed: Map<String, Long>,
    hiddenFoldersForMoveTargets: Set<String>,
    prioritizedFolders: Set<String>,
    // Folders made in the app that are still empty never show up in the media groups.
    userCreatedFolders: Set<String>,
    currentTopCard: MediaItem?,
    topCardOffset: Offset,
    topCardPointerInRoot: Offset?,
    topCardBoundsInRoot: Rect?,
    folderDropTargets: List<FolderData>,
    onFolderDropTargetsChange: (List<FolderData>) -> Unit,
    folderDropGestureController: FolderDropGestureController,
    folderDropTargetRects: Map<String, Rect>,
    onFolderDropTargetRectsChange: (Map<String, Rect>) -> Unit,
    onActiveDropTargetIdChange: (String?) -> Unit,
    folderListState: LazyListState,
    screenWidthPx: Float,
    screenHeightPx: Float
) {
    val currentTopCardOffset by rememberUpdatedState(topCardOffset)
    val currentTopCardPointerInRoot by rememberUpdatedState(topCardPointerInRoot)
    val currentTopCardBoundsInRoot by rememberUpdatedState(topCardBoundsInRoot)
    val currentFolderDropTargets by rememberUpdatedState(folderDropTargets)
    val currentFolderDropTargetRects by rememberUpdatedState(folderDropTargetRects)
    val currentHiddenFolders by rememberUpdatedState(hiddenFoldersForMoveTargets)

    LaunchedEffect(
        isFolderDropMode,
        mediaForMoveTargets,
        folderUsageState,
        folderLastUsed,
        hiddenFoldersForMoveTargets,
        prioritizedFolders,
        userCreatedFolders,
        currentTopCard?.bucketName
    ) {
        if (!isFolderDropMode) {
            onFolderDropTargetsChange(emptyList())
            return@LaunchedEffect
        }
        val targets = withContext(Dispatchers.Default) {
            buildFolderTargetsForOrganize(
                allMedia = mediaForMoveTargets,
                currentFolder = currentTopCard?.bucketName,
                hiddenFolders = hiddenFoldersForMoveTargets,
                prioritizedFolders = prioritizedFolders,
                folderUsage = folderUsageState,
                folderLastUsed = folderLastUsed,
                userCreatedFolders = userCreatedFolders
            )
        }
        onFolderDropTargetsChange(targets)
    }

    LaunchedEffect(folderDropTargets) {
        onFolderDropTargetRectsChange(emptyMap())
    }

    LaunchedEffect(isFolderDropMode) {
        if (!isFolderDropMode) {
            onFolderDropTargetRectsChange(emptyMap())
            onActiveDropTargetIdChange(folderDropGestureController.reset())
        } else {
            folderListState.scrollToItem(0)
        }
    }

    LaunchedEffect(
        isFolderDropMode,
        folderDropTargetRects,
        hiddenFoldersForMoveTargets,
        folderDropTargets,
        screenWidthPx,
        screenHeightPx
    ) {
        if (!isFolderDropMode) {
            onActiveDropTargetIdChange(folderDropGestureController.reset())
            return@LaunchedEffect
        }
        if (folderDropTargetRects.isEmpty()) {
            return@LaunchedEffect
        }

        snapshotFlow {
            Triple(
                currentTopCardOffset,
                currentTopCardPointerInRoot,
                currentTopCardBoundsInRoot
            )
        }.collect { (offset, pointerInRoot, bounds) ->
            val cardCenter = if (bounds != null) {
                Offset(
                    x = (bounds.left + bounds.right) / 2f,
                    y = (bounds.top + bounds.bottom) / 2f
                )
            } else {
                Offset(
                    x = (screenWidthPx / 2f) + offset.x,
                    y = (screenHeightPx / 2f) + offset.y
                )
            }
            val validTargetIds = (currentFolderDropTargets.map { it.id }.toSet() + "new_folder")
                .filterNot { it in currentHiddenFolders }
                .toSet()

            onActiveDropTargetIdChange(
                folderDropGestureController.update(
                    FolderDropGestureFrame(
                        nowMs = System.currentTimeMillis(),
                        cardOffset = offset,
                        cardCenter = cardCenter,
                        cardBounds = bounds,
                        pointerInRoot = pointerInRoot,
                        targetRects = currentFolderDropTargetRects,
                        validTargetIds = validTargetIds
                    )
                )
            )
        }
    }

    LaunchedEffect(isFolderDropMode, screenWidthPx) {
        if (!isFolderDropMode) return@LaunchedEffect

        while (isActive) {
            val bounds = currentTopCardBoundsInRoot
            val edgeProbeX = currentTopCardPointerInRoot?.x
                ?: bounds?.let { (it.left + it.right) / 2f }
                ?: ((screenWidthPx / 2f) + currentTopCardOffset.x)

            val scrollSpeed = calculateEdgeScrollSpeed(
                cardCenterX = edgeProbeX,
                screenWidthPx = screenWidthPx,
                edgeThreshold = HOME_FOLDER_DROP_EDGE_THRESHOLD_PX,
                minSpeed = HOME_FOLDER_DROP_EDGE_MIN_SCROLL_SPEED,
                maxSpeed = HOME_FOLDER_DROP_EDGE_MAX_SCROLL_SPEED
            )
            if (scrollSpeed != 0f) {
                folderListState.scrollBy(scrollSpeed)
            }

            delay(HOME_FOLDER_DROP_EDGE_SCROLL_INTERVAL_MS)
        }
    }
}
