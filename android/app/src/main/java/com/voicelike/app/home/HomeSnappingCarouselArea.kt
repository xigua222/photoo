package com.voicelike.app

import android.app.Activity
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val HOME_SNAPPING_CAROUSEL_PAGE_SPACING = 6.dp
private val HOME_SNAPPING_CAROUSEL_CARD_SHAPE = RoundedCornerShape(24.dp)
private val HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING = 10.dp
private val HOME_SNAPPING_CAROUSEL_CARD_SHADOW_ELEVATION = 12.dp
private val HOME_SNAPPING_CAROUSEL_DELETE_THRESHOLD = 74.dp
private const val HOME_SNAPPING_CAROUSEL_DELETE_EXIT_MIN_DURATION_MS = 68L
private const val HOME_SNAPPING_CAROUSEL_DELETE_EXIT_MAX_DURATION_MS = 130L
private const val HOME_SNAPPING_CAROUSEL_DELETE_ROW_SHIFT_DURATION_MS = 244L
private const val HOME_SNAPPING_CAROUSEL_ENTRY_INITIAL_DELAY_MS = 10L
private const val HOME_SNAPPING_CAROUSEL_ENTRY_DURATION_MS = 264L
private const val HOME_SNAPPING_CAROUSEL_ENTRY_OFFSET_FRACTION = 0.78f
private const val HOME_SNAPPING_CAROUSEL_UNDO_ROW_SHIFT_DURATION_MS = 244L
private val HOME_SNAPPING_CAROUSEL_UNDO_RESTORE_OFFSET = 30.dp
private val HOME_SNAPPING_CAROUSEL_UNDO_RESTORE_OFFSCREEN_EXTRA = 28.dp
private const val HOME_SNAPPING_CAROUSEL_UNDO_RESTORE_DURATION_MS = 188L
private const val HOME_SNAPPING_CAROUSEL_UNDO_INTERACTION_RELEASE_DELAY_MS = 76L
private const val HOME_SNAPPING_CAROUSEL_UNDO_NAVIGATION_BASE_DURATION_MS = 228L
private const val HOME_SNAPPING_CAROUSEL_UNDO_NAVIGATION_PER_PAGE_DURATION_MS = 48L
private const val HOME_SNAPPING_CAROUSEL_UNDO_REWIND_MAX_DURATION_MS = 820L
private const val HOME_SNAPPING_CAROUSEL_PAGE_SETTLE_EPSILON = 0.001f
private const val HOME_SNAPPING_CAROUSEL_SETTLEMENT_PAGE_KEY = Long.MIN_VALUE + 1L

private data class HomeSnappingCarouselDeleteShiftTarget(
    val sourcePage: Int,
    val token: Long
)

private fun resolveSnappingCarouselUpSpeedT(speedMultiplier: Float): Float {
    val normalized = normalizeSwipeSpeedMultiplier(speedMultiplier)
    return (
        (normalized - MIN_SWIPE_SPEED_MULTIPLIER) /
            (MAX_SWIPE_SPEED_MULTIPLIER - MIN_SWIPE_SPEED_MULTIPLIER)
        ).coerceIn(0f, 1f)
}

private fun resolveSnappingCarouselUpDragFactor(
    speedMultiplier: Float,
    deleteProgress: Float
): Float {
    val speedT = resolveSnappingCarouselUpSpeedT(speedMultiplier)
    val baseFactor = 1.00f + (0.12f * speedT)
    val progressResistance = 1f - (deleteProgress.coerceIn(0f, 1f) * 0.015f)
    return (baseFactor * progressResistance).coerceIn(0.98f, 1.12f)
}

private fun resolveSnappingCarouselUpReturnSpring(speedMultiplier: Float) = spring<Float>(
    stiffness = 410f + (150f * resolveSnappingCarouselUpSpeedT(speedMultiplier)),
    dampingRatio = 0.84f
)

private fun resolveSnappingCarouselDeleteExitDurationMs(
    startOffset: Float,
    exitOffset: Float,
    speedMultiplier: Float
): Int {
    val fullTravel = exitOffset.absoluteValue.coerceAtLeast(1f)
    val remainingTravel = (exitOffset - startOffset).absoluteValue
    val remainingT = (remainingTravel / fullTravel).coerceIn(0f, 1f)
    val speedT = resolveSnappingCarouselUpSpeedT(speedMultiplier)
    val minDuration = HOME_SNAPPING_CAROUSEL_DELETE_EXIT_MIN_DURATION_MS - (10f * speedT)
    val maxDuration = HOME_SNAPPING_CAROUSEL_DELETE_EXIT_MAX_DURATION_MS - (12f * speedT)
    return (minDuration + ((maxDuration - minDuration) * remainingT))
        .roundToInt()
        .coerceIn(
            HOME_SNAPPING_CAROUSEL_DELETE_EXIT_MIN_DURATION_MS.toInt(),
            HOME_SNAPPING_CAROUSEL_DELETE_EXIT_MAX_DURATION_MS.toInt()
        )
}

private fun resolveSnappingCarouselUndoNavigationSpec(distancePages: Int) = tween<Float>(
    durationMillis = (
        HOME_SNAPPING_CAROUSEL_UNDO_NAVIGATION_BASE_DURATION_MS +
            (distancePages.coerceAtLeast(1) * HOME_SNAPPING_CAROUSEL_UNDO_NAVIGATION_PER_PAGE_DURATION_MS)
        ).toInt(),
    easing = FastOutSlowInEasing
)

private fun resolveSnappingCarouselUndoRewindSpec(distancePages: Int) = tween<Float>(
    durationMillis = minOf(
        HOME_SNAPPING_CAROUSEL_UNDO_REWIND_MAX_DURATION_MS,
        HOME_SNAPPING_CAROUSEL_UNDO_NAVIGATION_BASE_DURATION_MS +
            (distancePages.coerceAtLeast(1) * HOME_SNAPPING_CAROUSEL_UNDO_NAVIGATION_PER_PAGE_DURATION_MS)
    ).toInt(),
    easing = FastOutSlowInEasing
)

private fun resolveSnappingCarouselUndoStartOffset(
    target: UndoAnimationTarget,
    deleteThresholdPx: Float,
    fallbackOffsetPx: Float,
    offscreenStartOffsetPx: Float
): Float {
    return when (target.direction) {
        SwipeDirection.Up -> {
            val recordedOffset = target.restoreOffsetY.takeIf { it < 0f } ?: -fallbackOffsetPx
            -maxOf(recordedOffset.absoluteValue, offscreenStartOffsetPx)
        }
        SwipeDirection.Down -> {
            val recordedOffset = target.restoreOffsetY.takeIf { it > 0f } ?: (fallbackOffsetPx * 0.6f)
            recordedOffset.coerceIn(
                minimumValue = fallbackOffsetPx * 0.4f,
                maximumValue = deleteThresholdPx * 1.9f
            )
        }
        else -> 0f
    }
}

private fun buildSnappingCarouselCurrentUndoPreShiftDisplay(
    displayMedia: List<MediaItem>,
    restoredItemId: Long
): List<MediaItem> {
    val restoredIndex = displayMedia.indexOfFirst { it.id == restoredItemId }
    if (restoredIndex < 0) return displayMedia
    return buildList(displayMedia.size - 1) {
        addAll(displayMedia.take(restoredIndex))
        addAll(displayMedia.drop(restoredIndex + 1))
    }
}

private suspend fun PagerState.awaitSnappingCarouselSettled() {
    if (
        !isScrollInProgress &&
            currentPageOffsetFraction.absoluteValue <= HOME_SNAPPING_CAROUSEL_PAGE_SETTLE_EPSILON
    ) {
        return
    }
    snapshotFlow {
        isScrollInProgress to currentPageOffsetFraction
    }.first { (isScrolling, offsetFraction) ->
        !isScrolling && offsetFraction.absoluteValue <= HOME_SNAPPING_CAROUSEL_PAGE_SETTLE_EPSILON
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeSnappingCarouselArea(
    displayMedia: List<MediaItem>,
    currentPage: Int,
    pendingUndoPage: Int? = null,
    undoAnimationTargets: List<UndoAnimationTarget>,
    interactionLocked: Boolean = false,
    isInitialLoad: Boolean = false,
    entryAnimationToken: Long = 0L,
    settlementBatchToken: Long = 0L,
    upSwipeSpeedMultiplier: Float,
    hiddenSourceItemId: Long? = null,
    quickOrganizeMode: Boolean,
    muteVideos: Boolean,
    lensyEnabled: Boolean,
    aiImageResultsEnabled: Boolean,
    colorCardEnabled: Boolean,
    pauseMainVideos: Boolean,
    topCardPlaybackArmed: Boolean,
    isFullScreenVideoOpen: Boolean,
    onCurrentPageChange: (Int) -> Unit,
    onSwipeUp: (MediaItem, Float) -> Unit,
    onDeleteSound: () -> Unit = {},
    onOpenDetail: (MediaItem, Rect?) -> Unit,
    onOpenFullScreenVideo: (MediaItem, ExoPlayer) -> Unit,
    onMuteVideosChange: (Boolean) -> Unit,
    onJumpToDay: (String) -> Unit,
    onTopCardStartupSample: (MediaItem, Long) -> Unit,
    onCenterCardBoundsChanged: (Rect) -> Unit,
    onCenterCardImageBoundsChanged: (Rect) -> Unit,
    onCenterAiOverlayStateChanged: (String?, Boolean) -> Unit,
    onUndoAnimationFinished: ((Long) -> Unit)? = null,
    onEntryAnimationFinished: (() -> Unit)? = null,
    onPendingUndoPageConsumed: (() -> Unit)? = null,
    settlementPageContent: @Composable ((Long) -> Unit)? = null
) {
    if (displayMedia.isEmpty() && settlementPageContent == null) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val visibleIds = displayMedia.map { it.id }
    val pageCount = displayMedia.size + if (settlementPageContent != null) 1 else 0
    val requestedPage = currentPage.coerceIn(0, maxOf(0, pageCount - 1))
    val resolvedPendingUndoPage = pendingUndoPage?.coerceIn(0, maxOf(0, pageCount - 1))
    val pagerState = rememberPagerState(
        initialPage = requestedPage,
        pageCount = { pageCount }
    )
    val isPagerDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var deleteRowShiftTokenSequence by remember { mutableLongStateOf(0L) }
    var deleteRowShiftTarget by remember { mutableStateOf<HomeSnappingCarouselDeleteShiftTarget?>(null) }
    var preparedDeleteRowShiftToken by remember { mutableLongStateOf(Long.MIN_VALUE) }
    val deleteRowShiftProgress = remember { Animatable(0f) }
    var pendingAnimatedPage by remember { mutableStateOf<Int?>(null) }
    var undoReturnPrimed by remember { mutableStateOf(false) }
    var undoInsertGapSourcePage by remember { mutableStateOf(-1) }
    var undoInsertGapDirection by remember { mutableStateOf(0) }
    val undoInsertGapProgress = remember { Animatable(0f) }
    var preparedCurrentUndoShiftToken by remember { mutableLongStateOf(Long.MIN_VALUE) }
    var activeCurrentUndoShiftToken by remember { mutableLongStateOf(Long.MIN_VALUE) }
    var playedCurrentUndoShiftToken by remember { mutableLongStateOf(Long.MIN_VALUE) }
    val currentUndoShiftProgress = remember { Animatable(0f) }
    var settlementCelebrationToken by remember { mutableLongStateOf(0L) }
    var lastCelebratedSettlementBatchToken by remember { mutableLongStateOf(Long.MIN_VALUE) }
    var lastCarouselTickHapticPage by remember { mutableIntStateOf(requestedPage) }
    var carouselTickHapticsArmed by remember { mutableStateOf(false) }
    var previousVisibleIds by remember { mutableStateOf(visibleIds) }
    var previousRequestedPage by remember { mutableStateOf(requestedPage) }
    val immediateDeleteRowShiftSourcePage = run {
        val removedIds = previousVisibleIds.filterNot { visibleIds.contains(it) }
        val addedIds = visibleIds.filterNot { previousVisibleIds.contains(it) }
        val previousFocusedId = previousVisibleIds.getOrNull(previousRequestedPage)
        val currentFocusedKey = displayMedia.getOrNull(requestedPage)?.id ?: run {
            if (settlementPageContent != null && requestedPage == displayMedia.size) {
                HOME_SNAPPING_CAROUSEL_SETTLEMENT_PAGE_KEY
            } else {
                null
            }
        }
        val deletedFocusedCard =
            removedIds.size == 1 &&
                addedIds.isEmpty() &&
                previousFocusedId != null &&
                removedIds.first() == previousFocusedId
        requestedPage.takeIf { deletedFocusedCard && currentFocusedKey != null }
    }
    val undoTargetPage = resolvedPendingUndoPage ?: requestedPage
    val latestUndoAnimationTarget = remember(undoAnimationTargets) {
        undoAnimationTargets.lastOrNull()
    }
    val undoNavigationTarget = remember(latestUndoAnimationTarget, displayMedia, undoTargetPage) {
        val requestedItemId = displayMedia.getOrNull(undoTargetPage)?.id
        latestUndoAnimationTarget?.takeIf { target ->
            requestedItemId == target.itemId
        }
    }
    val isCurrentUndoShiftTargetActive =
        undoNavigationTarget != null && activeCurrentUndoShiftToken == undoNavigationTarget.token
    val shouldPlayCurrentUndoShift =
        resolvedPendingUndoPage == null &&
            undoNavigationTarget != null &&
            undoNavigationTarget.token != playedCurrentUndoShiftToken &&
            !isCurrentUndoShiftTargetActive &&
            displayMedia.getOrNull(requestedPage)?.id == undoNavigationTarget.itemId
    var currentUndoPreShiftDisplayMedia by remember(undoNavigationTarget?.token) {
        mutableStateOf<List<MediaItem>?>(
            if (shouldPlayCurrentUndoShift) {
                undoNavigationTarget.let { target ->
                    buildSnappingCarouselCurrentUndoPreShiftDisplay(
                        displayMedia = displayMedia,
                        restoredItemId = target.itemId
                    )
                }
            } else {
                null
            }
        )
    }
    val effectiveCurrentUndoShiftProgress = when {
        isCurrentUndoShiftTargetActive -> currentUndoShiftProgress.value
        !shouldPlayCurrentUndoShift -> 0f
        preparedCurrentUndoShiftToken != undoNavigationTarget.token -> 1f
        else -> currentUndoShiftProgress.value
    }
    val isCurrentUndoShiftAnimating = effectiveCurrentUndoShiftProgress > 0.001f
    val useCurrentUndoPreShiftDisplay =
        currentUndoPreShiftDisplayMedia != null &&
            (shouldPlayCurrentUndoShift || isCurrentUndoShiftAnimating || isCurrentUndoShiftTargetActive)

    LaunchedEffect(displayMedia.size, displayMedia.firstOrNull()?.id, pageCount) {
        lastCarouselTickHapticPage = pagerState.currentPage.coerceIn(0, maxOf(0, pageCount - 1))
        carouselTickHapticsArmed = false
    }

    LaunchedEffect(deleteRowShiftTarget?.token, pageCount) {
        val target = deleteRowShiftTarget ?: return@LaunchedEffect
        if (pageCount <= 0) {
            if (deleteRowShiftTarget?.token == target.token) {
                deleteRowShiftTarget = null
            }
            if (preparedDeleteRowShiftToken == target.token) {
                preparedDeleteRowShiftToken = Long.MIN_VALUE
            }
            return@LaunchedEffect
        }
        deleteRowShiftProgress.stop()
        try {
            deleteRowShiftProgress.snapTo(1f)
            preparedDeleteRowShiftToken = target.token
            deleteRowShiftProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = HOME_SNAPPING_CAROUSEL_DELETE_ROW_SHIFT_DURATION_MS.toInt(),
                    easing = FastOutSlowInEasing
                )
            )
        } finally {
            deleteRowShiftProgress.snapTo(0f)
            if (deleteRowShiftTarget?.token == target.token) {
                deleteRowShiftTarget = null
            }
            if (preparedDeleteRowShiftToken == target.token) {
                preparedDeleteRowShiftToken = Long.MIN_VALUE
            }
        }
    }

    LaunchedEffect(
        resolvedPendingUndoPage,
        requestedPage,
        pageCount,
        displayMedia.size,
        displayMedia.firstOrNull()?.id,
        displayMedia.lastOrNull()?.id
    ) {
        val pendingPage = resolvedPendingUndoPage ?: return@LaunchedEffect
        if (pageCount <= 0) {
            undoReturnPrimed = false
            onPendingUndoPageConsumed?.invoke()
            return@LaunchedEffect
        }
        pagerState.awaitSnappingCarouselSettled()
        val preservedPage = requestedPage.coerceIn(0, pageCount - 1)
        val targetUndoPage = pendingPage.coerceIn(0, pageCount - 1)
        val currentPagerPage = pagerState.currentPage.coerceIn(0, pageCount - 1)
        val rewindDistance = (currentPagerPage - preservedPage).absoluteValue
        pendingAnimatedPage = preservedPage
        undoReturnPrimed = false
        try {
            if (currentPagerPage != preservedPage) {
                pagerState.animateScrollToPage(
                    page = preservedPage,
                    animationSpec = resolveSnappingCarouselUndoRewindSpec(rewindDistance)
                )
            }
            pagerState.awaitSnappingCarouselSettled()
            if (
                pagerState.currentPage != preservedPage ||
                    pagerState.currentPageOffsetFraction.absoluteValue > HOME_SNAPPING_CAROUSEL_PAGE_SETTLE_EPSILON
            ) {
                pagerState.scrollToPage(preservedPage)
            }
            onCurrentPageChange(preservedPage)
            if (targetUndoPage != preservedPage) {
                undoInsertGapSourcePage = preservedPage
                undoInsertGapDirection = if (targetUndoPage < preservedPage) 1 else -1
                undoInsertGapProgress.snapTo(0f)
                undoInsertGapProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = HOME_SNAPPING_CAROUSEL_UNDO_ROW_SHIFT_DURATION_MS.toInt(),
                        easing = FastOutSlowInEasing
                    )
                )
                pendingAnimatedPage = targetUndoPage
                pagerState.scrollToPage(targetUndoPage)
            }
            if (
                pagerState.currentPage != targetUndoPage ||
                    pagerState.currentPageOffsetFraction.absoluteValue > HOME_SNAPPING_CAROUSEL_PAGE_SETTLE_EPSILON
            ) {
                pagerState.scrollToPage(targetUndoPage)
            }
            undoInsertGapProgress.snapTo(0f)
            undoInsertGapDirection = 0
            undoInsertGapSourcePage = -1
            undoReturnPrimed = true
            onCurrentPageChange(targetUndoPage)
        } finally {
            undoInsertGapProgress.snapTo(0f)
            undoInsertGapDirection = 0
            undoInsertGapSourcePage = -1
            if (pendingAnimatedPage == targetUndoPage) {
                pendingAnimatedPage = null
            }
            onPendingUndoPageConsumed?.invoke()
            undoReturnPrimed = false
        }
    }

    LaunchedEffect(
        requestedPage,
        displayMedia.size,
        displayMedia.firstOrNull()?.id,
        displayMedia.lastOrNull()?.id,
        pageCount,
        undoNavigationTarget?.token,
        resolvedPendingUndoPage
    ) {
        if (resolvedPendingUndoPage != null) return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect
        val targetPage = requestedPage.coerceIn(0, pageCount - 1)
        if (pagerState.currentPage == targetPage) {
            if (pendingAnimatedPage == targetPage) {
                pendingAnimatedPage = null
            }
            return@LaunchedEffect
        }
        if (pagerState.isScrollInProgress) return@LaunchedEffect
        if (shouldPlayCurrentUndoShift) {
            pendingAnimatedPage = null
            pagerState.scrollToPage(targetPage)
            return@LaunchedEffect
        }
        if (undoNavigationTarget != null) {
            val undoNavigationDistance = (pagerState.currentPage - targetPage).absoluteValue
            pendingAnimatedPage = targetPage
            try {
                pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = resolveSnappingCarouselUndoNavigationSpec(undoNavigationDistance)
                )
            } finally {
                if (pendingAnimatedPage == targetPage) {
                    pendingAnimatedPage = null
                }
            }
        } else {
            pendingAnimatedPage = null
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(undoNavigationTarget?.token, requestedPage, resolvedPendingUndoPage) {
        val target = undoNavigationTarget ?: return@LaunchedEffect
        if (!shouldPlayCurrentUndoShift) return@LaunchedEffect
        currentUndoShiftProgress.stop()
        try {
            currentUndoShiftProgress.snapTo(1f)
            activeCurrentUndoShiftToken = target.token
            preparedCurrentUndoShiftToken = target.token
            currentUndoShiftProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = HOME_SNAPPING_CAROUSEL_UNDO_ROW_SHIFT_DURATION_MS.toInt(),
                    easing = FastOutSlowInEasing
                )
            )
        } finally {
            currentUndoShiftProgress.snapTo(0f)
            if (activeCurrentUndoShiftToken == target.token) {
                activeCurrentUndoShiftToken = Long.MIN_VALUE
            }
            playedCurrentUndoShiftToken = target.token
            currentUndoPreShiftDisplayMedia = null
        }
    }

    LaunchedEffect(pagerState.currentPage, pageCount, pendingAnimatedPage, resolvedPendingUndoPage) {
        if (pageCount <= 0 || resolvedPendingUndoPage != null) return@LaunchedEffect
        val reportedPage = pendingAnimatedPage ?: pagerState.currentPage
        onCurrentPageChange(reportedPage.coerceIn(0, pageCount - 1))
    }

    LaunchedEffect(
        isPagerDragged,
        pagerState.isScrollInProgress,
        pageCount,
        resolvedPendingUndoPage,
        pendingAnimatedPage,
        deleteRowShiftTarget?.token,
        undoInsertGapDirection
    ) {
        if (pageCount <= 0) return@LaunchedEffect
        val shouldSuppressHaptic =
            resolvedPendingUndoPage != null ||
                pendingAnimatedPage != null ||
                deleteRowShiftTarget != null ||
                undoInsertGapDirection != 0 ||
                isCurrentUndoShiftAnimating
        if (shouldSuppressHaptic) {
            carouselTickHapticsArmed = false
            lastCarouselTickHapticPage = pagerState.currentPage.coerceIn(0, pageCount - 1)
            return@LaunchedEffect
        }
        if (isPagerDragged) {
            carouselTickHapticsArmed = true
            return@LaunchedEffect
        }
        if (!pagerState.isScrollInProgress) {
            carouselTickHapticsArmed = false
            lastCarouselTickHapticPage = pagerState.currentPage.coerceIn(0, pageCount - 1)
        }
    }

    LaunchedEffect(
        pagerState.currentPage,
        carouselTickHapticsArmed,
        pageCount,
        resolvedPendingUndoPage,
        pendingAnimatedPage,
        deleteRowShiftTarget?.token,
        undoInsertGapDirection
    ) {
        if (pageCount <= 0) return@LaunchedEffect
        val currentPagerPage = pagerState.currentPage.coerceIn(0, pageCount - 1)
        val shouldSuppressHaptic =
            resolvedPendingUndoPage != null ||
                pendingAnimatedPage != null ||
                deleteRowShiftTarget != null ||
                undoInsertGapDirection != 0 ||
                isCurrentUndoShiftAnimating
        if (shouldSuppressHaptic) {
            lastCarouselTickHapticPage = currentPagerPage
            return@LaunchedEffect
        }
        if (!carouselTickHapticsArmed) {
            lastCarouselTickHapticPage = currentPagerPage
            return@LaunchedEffect
        }
        if (currentPagerPage != lastCarouselTickHapticPage) {
            if (hapticsAllowed) {
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick
                )
            }
            lastCarouselTickHapticPage = currentPagerPage
        }
    }

    LaunchedEffect(
        settlementPageContent != null,
        pagerState.currentPage,
        pagerState.currentPageOffsetFraction,
        pagerState.isScrollInProgress,
        pendingAnimatedPage,
        deleteRowShiftTarget?.token,
        undoInsertGapDirection,
        pageCount,
        settlementBatchToken
    ) {
        val settlementPageIndex = displayMedia.size
        val settlementAnimationIdle =
            pendingAnimatedPage == null &&
                deleteRowShiftTarget == null &&
                undoInsertGapDirection == 0
        val settlementPageActive =
            settlementPageContent != null &&
                pageCount > 0 &&
                settlementPageIndex in 0 until pageCount &&
                pagerState.currentPage == settlementPageIndex &&
                pagerState.settledPage == settlementPageIndex &&
                !pagerState.isScrollInProgress &&
                pagerState.currentPageOffsetFraction.absoluteValue <= HOME_SNAPPING_CAROUSEL_PAGE_SETTLE_EPSILON &&
                settlementAnimationIdle
        if (settlementPageActive && lastCelebratedSettlementBatchToken != settlementBatchToken) {
            settlementCelebrationToken += 1L
            lastCelebratedSettlementBatchToken = settlementBatchToken
        }
    }

    LaunchedEffect(visibleIds, requestedPage, settlementPageContent != null) {
        val removedIds = previousVisibleIds.filterNot { visibleIds.contains(it) }
        val addedIds = visibleIds.filterNot { previousVisibleIds.contains(it) }
        val previousFocusedId = previousVisibleIds.getOrNull(previousRequestedPage)
        val currentFocusedKey = displayMedia.getOrNull(requestedPage)?.id ?: run {
            if (settlementPageContent != null && requestedPage == displayMedia.size) {
                HOME_SNAPPING_CAROUSEL_SETTLEMENT_PAGE_KEY
            } else {
                null
            }
        }
        val deletedFocusedCard =
            removedIds.size == 1 &&
                addedIds.isEmpty() &&
                previousFocusedId != null &&
                removedIds.first() == previousFocusedId
        if (deletedFocusedCard && currentFocusedKey != null) {
            deleteRowShiftTokenSequence += 1L
            preparedDeleteRowShiftToken = Long.MIN_VALUE
            deleteRowShiftTarget = HomeSnappingCarouselDeleteShiftTarget(
                sourcePage = requestedPage,
                token = deleteRowShiftTokenSequence
            )
        }
        previousVisibleIds = visibleIds
        previousRequestedPage = requestedPage
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val carouselCardFrameSpec = rememberHomeCarouselCardFrameSpec(quickOrganizeMode)
        val centeredCardWidthFraction = resolveHomeCarouselPageWidthFraction(quickOrganizeMode)
        val pageWidth = (
            (maxWidth * centeredCardWidthFraction)
                .coerceAtMost(carouselCardFrameSpec.maxWidth) +
                (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2)
            )
            .coerceAtMost(maxWidth)
        val horizontalPadding = ((maxWidth - pageWidth) / 2f).coerceAtLeast(0.dp)
        val pageWidthPx = with(density) { pageWidth.toPx() }
        val settlementAvailableWidth =
            (pageWidth - (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2)).coerceAtLeast(0.dp)
        val settlementAvailableHeight =
            (maxHeight - (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2)).coerceAtLeast(0.dp)
        val settlementCardFrameSize = resolveHomeCardFrameSize(
            availableWidth = settlementAvailableWidth,
            availableHeight = settlementAvailableHeight,
            spec = carouselCardFrameSpec
        )
        val settlementCardHeight = settlementCardFrameSize.height
        val settlementCardWidth = settlementCardFrameSize.width
        val startupEntryOffsetX = remember(entryAnimationToken, maxWidth, isInitialLoad) {
            Animatable(
                if (isInitialLoad) {
                    with(density) { maxWidth.toPx() } * HOME_SNAPPING_CAROUSEL_ENTRY_OFFSET_FRACTION
                } else {
                    0f
                }
            )
        }
        val startupEntryAlpha = remember(entryAnimationToken, isInitialLoad) {
            Animatable(if (isInitialLoad) 0.92f else 1f)
        }

        LaunchedEffect(entryAnimationToken, pageCount, isInitialLoad) {
            if (!isInitialLoad || pageCount <= 0) {
                startupEntryOffsetX.snapTo(0f)
                startupEntryAlpha.snapTo(1f)
                return@LaunchedEffect
            }
            val initialOffsetPx = with(density) { maxWidth.toPx() } * HOME_SNAPPING_CAROUSEL_ENTRY_OFFSET_FRACTION
            startupEntryOffsetX.stop()
            startupEntryAlpha.stop()
            startupEntryOffsetX.snapTo(initialOffsetPx)
            startupEntryAlpha.snapTo(0.92f)
            delay(HOME_SNAPPING_CAROUSEL_ENTRY_INITIAL_DELAY_MS)
            try {
                launch {
                    startupEntryOffsetX.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = HOME_SNAPPING_CAROUSEL_ENTRY_DURATION_MS.toInt(),
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                startupEntryAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = HOME_SNAPPING_CAROUSEL_ENTRY_DURATION_MS.toInt(),
                        easing = FastOutSlowInEasing
                    )
                )
            } finally {
                startupEntryOffsetX.snapTo(0f)
                startupEntryAlpha.snapTo(1f)
                onEntryAnimationFinished?.invoke()
            }
        }
        val isPagerUndoTransitionRunning =
            resolvedPendingUndoPage != null ||
                pendingAnimatedPage != null ||
                undoInsertGapDirection != 0 ||
                useCurrentUndoPreShiftDisplay

        HorizontalPager(
            state = pagerState,
            key = { page -> page },
            beyondViewportPageCount = if (
                resolvedPendingUndoPage != null ||
                    pendingAnimatedPage != null ||
                    undoInsertGapDirection != 0 ||
                    isCurrentUndoShiftAnimating
            ) {
                2
            } else {
                0
            },
            pageSize = PageSize.Fixed(pageWidth),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            pageSpacing = HOME_SNAPPING_CAROUSEL_PAGE_SPACING,
            userScrollEnabled = !isInitialLoad && !interactionLocked && !isPagerUndoTransitionRunning,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = startupEntryOffsetX.value
                    alpha = startupEntryAlpha.value
                }
        ) { page ->
            val actualItem = displayMedia.getOrNull(page)
            val visualItem = if (useCurrentUndoPreShiftDisplay) {
                currentUndoPreShiftDisplayMedia?.getOrNull(page)
            } else {
                actualItem
            }
            val pageKey = actualItem?.id ?: HOME_SNAPPING_CAROUSEL_SETTLEMENT_PAGE_KEY
            val isCentered = page == pagerState.currentPage
            val visualAnchorPage = if (
                resolvedPendingUndoPage != null &&
                    !undoReturnPrimed &&
                    undoInsertGapSourcePage >= 0
            ) {
                undoInsertGapSourcePage
            } else {
                pagerState.currentPage
            }
            val visualAnchorOffsetFraction = if (visualAnchorPage == pagerState.currentPage) {
                pagerState.currentPageOffsetFraction
            } else {
                0f
            }
            val deleteRowShiftSourcePage =
                deleteRowShiftTarget?.sourcePage ?: immediateDeleteRowShiftSourcePage
            val effectiveDeleteRowShiftProgress = when {
                deleteRowShiftTarget != null &&
                    preparedDeleteRowShiftToken != deleteRowShiftTarget?.token -> 1f
                deleteRowShiftTarget != null -> deleteRowShiftProgress.value
                immediateDeleteRowShiftSourcePage != null -> 1f
                else -> 0f
            }
            val deleteRowShiftOffsetPages = if (
                deleteRowShiftSourcePage != null &&
                    page >= deleteRowShiftSourcePage
            ) {
                effectiveDeleteRowShiftProgress
            } else {
                0f
            }
            val signedPageOffset = (page - visualAnchorPage) - visualAnchorOffsetFraction
            val visualSignedPageOffset = signedPageOffset + deleteRowShiftOffsetPages
            val pageOffset = visualSignedPageOffset.absoluteValue.coerceIn(0f, 1.5f)
            val settledProgress = 1f - pageOffset.coerceIn(0f, 1f)
            val isUndoInsertShifting =
                undoInsertGapDirection != 0 && undoInsertGapProgress.value > 0.001f
            val undoInsertGapOffsetX = when {
                !isUndoInsertShifting -> 0f
                undoInsertGapDirection > 0 && page >= undoInsertGapSourcePage -> {
                    pageWidthPx * undoInsertGapProgress.value
                }
                undoInsertGapDirection < 0 && page <= undoInsertGapSourcePage -> {
                    -pageWidthPx * undoInsertGapProgress.value
                }
                else -> 0f
            }
            val currentUndoShiftOffsetX = if (useCurrentUndoPreShiftDisplay && page >= requestedPage) {
                pageWidthPx * (1f - effectiveCurrentUndoShiftProgress)
            } else {
                0f
            }
            val currentUndoLeadSettledProgress = if (useCurrentUndoPreShiftDisplay && page == requestedPage) {
                effectiveCurrentUndoShiftProgress
            } else {
                settledProgress
            }
            val cardScale = 0.968f + (0.032f * currentUndoLeadSettledProgress)
            val cardAlpha = (0.92f + (0.08f * currentUndoLeadSettledProgress)).coerceIn(0f, 1f)
            val cardOffsetY = (1f - currentUndoLeadSettledProgress) * 10f
            val isDeleteRowShifting =
                effectiveDeleteRowShiftProgress > 0.001f
            val item = visualItem
            val previewSubstituteItem: MediaItem? = null
            val isSettlementPageVisiblyActive =
                item == null &&
                    settlementPageContent != null &&
                    page == pageCount - 1 &&
                    page == pagerState.currentPage &&
                    page == pagerState.settledPage &&
                    !pagerState.isScrollInProgress &&
                    pagerState.currentPageOffsetFraction.absoluteValue <= HOME_SNAPPING_CAROUSEL_PAGE_SETTLE_EPSILON &&
                    !isDeleteRowShifting &&
                    !isUndoInsertShifting &&
                    !isCurrentUndoShiftAnimating &&
                    pendingAnimatedPage == null &&
                    resolvedPendingUndoPage == null
            val hideUndoReturningPage =
                undoNavigationTarget?.itemId == pageKey &&
                    actualItem != null &&
                    resolvedPendingUndoPage != null &&
                    !undoReturnPrimed &&
                    (
                        isCentered ||
                            (
                                page == resolvedPendingUndoPage &&
                                    previewSubstituteItem == null
                                )
                        )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha =
                            cardAlpha *
                                if (hideUndoReturningPage) 0f else 1f
                        translationX =
                            undoInsertGapOffsetX +
                                currentUndoShiftOffsetX +
                                (deleteRowShiftOffsetPages * pageWidthPx)
                        translationY = cardOffsetY
                    },
                contentAlignment = Alignment.Center
            ) {
                if (item != null) {
                    val renderedItem = previewSubstituteItem ?: item
                    val onDelete by rememberUpdatedState<(Float) -> Unit> { restoreOffsetY ->
                        actualItem?.let { onSwipeUp(it, restoreOffsetY) }
                    }
                    val undoAnimationTarget = if (
                        !useCurrentUndoPreShiftDisplay &&
                            actualItem != null &&
                            renderedItem.id == actualItem.id &&
                            undoNavigationTarget?.itemId == actualItem.id
                    ) {
                        undoNavigationTarget
                    } else {
                        null
                    }

                    HomeSnappingCarouselCard(
                        item = renderedItem,
                        undoAnimationTarget = undoAnimationTarget,
                        interactionLocked = interactionLocked,
                        upSwipeSpeedMultiplier = upSwipeSpeedMultiplier,
                        forceHidden = hiddenSourceItemId == renderedItem.id,
                        isCentered = isCentered,
                        quickOrganizeMode = quickOrganizeMode,
                        muteVideos = muteVideos,
                        lensyEnabled = lensyEnabled,
                        aiImageResultsEnabled = aiImageResultsEnabled,
                        colorCardEnabled = colorCardEnabled,
                        pauseMainVideos = pauseMainVideos,
                        allowPlayback =
                            isCentered &&
                                topCardPlaybackArmed &&
                                !pagerState.isScrollInProgress &&
                                !isDeleteRowShifting &&
                                !isUndoInsertShifting &&
                                !isCurrentUndoShiftAnimating,
                        isPagerScrolling =
                            pagerState.isScrollInProgress ||
                                isDeleteRowShifting ||
                                isUndoInsertShifting,
                        isFullScreenVideoOpen = isFullScreenVideoOpen,
                        onDeleteCommitStart = {},
                        onDeleteSound = onDeleteSound,
                        onDelete = onDelete,
                        onOpenDetail = { sourceBounds ->
                            actualItem?.let { onOpenDetail(it, sourceBounds) }
                        },
                        onOpenFullScreenVideo = onOpenFullScreenVideo,
                        onMuteVideosChange = onMuteVideosChange,
                        onJumpToDay = onJumpToDay,
                        onStartupSample = { sampleMs ->
                            actualItem?.let { onTopCardStartupSample(it, sampleMs) }
                        },
                        onCardBoundsChanged = onCenterCardBoundsChanged,
                        onImageBoundsChanged = onCenterCardImageBoundsChanged,
                        onAiOverlayStateChanged = onCenterAiOverlayStateChanged,
                        onUndoAnimationFinished = { token -> onUndoAnimationFinished?.invoke(token) }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = BiasAlignment(
                            horizontalBias = 0f,
                            verticalBias = carouselCardFrameSpec.biasY
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .width(settlementCardWidth + (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2))
                                .height(settlementCardHeight + (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(settlementCardWidth)
                                    .height(settlementCardHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                settlementPageContent?.invoke(
                                    if (isSettlementPageVisiblyActive) settlementCelebrationToken else 0L
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSnappingCarouselCard(
    item: MediaItem,
    undoAnimationTarget: UndoAnimationTarget?,
    interactionLocked: Boolean,
    upSwipeSpeedMultiplier: Float,
    forceHidden: Boolean,
    isCentered: Boolean,
    quickOrganizeMode: Boolean,
    muteVideos: Boolean,
    lensyEnabled: Boolean,
    aiImageResultsEnabled: Boolean,
    colorCardEnabled: Boolean,
    pauseMainVideos: Boolean,
    allowPlayback: Boolean,
    isPagerScrolling: Boolean,
    isFullScreenVideoOpen: Boolean,
    onDeleteCommitStart: () -> Unit,
    onDeleteSound: () -> Unit,
    onDelete: (Float) -> Unit,
    onOpenDetail: (Rect?) -> Unit,
    onOpenFullScreenVideo: (MediaItem, ExoPlayer) -> Unit,
    onMuteVideosChange: (Boolean) -> Unit,
    onJumpToDay: (String) -> Unit,
    onStartupSample: (Long) -> Unit,
    onCardBoundsChanged: (Rect) -> Unit,
    onImageBoundsChanged: (Rect) -> Unit,
    onAiOverlayStateChanged: (String?, Boolean) -> Unit,
    onUndoAnimationFinished: (Long) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val hostView = LocalView.current
    val rootWindowView = hostView.rootView
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val currentLang = LocalAppLanguage.current.code
    val deleteThresholdPx = with(density) { HOME_SNAPPING_CAROUSEL_DELETE_THRESHOLD.toPx() }
    val restoreOffsetPx = with(density) { HOME_SNAPPING_CAROUSEL_UNDO_RESTORE_OFFSET.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val undoRestoreOffscreenStartPx = with(density) {
        configuration.screenHeightDp.dp.toPx() +
            HOME_SNAPPING_CAROUSEL_UNDO_RESTORE_OFFSCREEN_EXTRA.toPx()
    }
    val resetAnimationSpec = remember(upSwipeSpeedMultiplier) {
        resolveSnappingCarouselUpReturnSpring(upSwipeSpeedMultiplier)
    }
    val currentUpSwipeSpeedMultiplier by rememberUpdatedState(upSwipeSpeedMultiplier)
    val currentResetAnimationSpec by rememberUpdatedState(resetAnimationSpec)
    val cardShadowElevation = if (isCentered) HOME_SNAPPING_CAROUSEL_CARD_SHADOW_ELEVATION else 6.dp
    val cardShadowAmbientColor = Color.Black.copy(alpha = if (isDark) {
        if (isCentered) 0.34f else 0.18f
    } else {
        if (isCentered) 0.14f else 0.07f
    })
    val cardShadowSpotColor = Color.Black.copy(alpha = if (isDark) {
        if (isCentered) 0.48f else 0.28f
    } else {
        if (isCentered) 0.18f else 0.1f
    })
    var isExpanded by remember(item.id) { mutableStateOf(false) }
    var isDeleteCommitting by remember(item.id) { mutableStateOf(false) }
    var isDraggingUp by remember(item.id) { mutableStateOf(false) }
    var dragOffsetY by remember(item.id) { mutableFloatStateOf(0f) }
    var cardBoundsInRoot by remember(item.id) { mutableStateOf<Rect?>(null) }
    var imageBoundsInRoot by remember(item.id) { mutableStateOf<Rect?>(null) }
    var isMuted by remember(item.id, muteVideos) { mutableStateOf(muteVideos) }
    var isVideoSeeking by remember(item.id) { mutableStateOf(false) }
    var livePlaybackCompleted by remember(item.id) { mutableStateOf(false) }
    var liveReplayToken by remember(item.id) { mutableLongStateOf(0L) }
    var isImageLongPressHoverActive by remember(item.id) { mutableStateOf(false) }
    var isImageLongPressHoverPresented by remember(item.id) { mutableStateOf(false) }
    var isImageLongPressHoverVisualReady by remember(item.id) { mutableStateOf(false) }
    var hoverBackdropBitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var suppressCardTapUntilUptimeMs by remember(item.id) { mutableLongStateOf(0L) }
    var preparedUndoToken by remember(item.id) { mutableLongStateOf(Long.MIN_VALUE) }
    var playedUndoToken by remember(item.id) { mutableLongStateOf(Long.MIN_VALUE) }
    val settleOffsetY = remember(item.id) { Animatable(0f) }
    val restoreOffsetY = remember(item.id) { Animatable(0f) }
    val restoreScale = remember(item.id) { Animatable(1f) }
    val restoreAlpha = remember(item.id) { Animatable(1f) }
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
    val isImageLongPressHoverVisualsActive =
        isCentered &&
            !isExpanded &&
            !isDraggingUp &&
            !isDeleteCommitting &&
            !isPagerScrolling &&
            isImageLongPressHoverPresented &&
            isImageLongPressHoverVisualReady
    val hoverVisuals = rememberHomeCardLongPressHoverVisuals(active = isImageLongPressHoverVisualsActive)
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
    val shouldPrefetchCardAnalysis =
        !isPagerScrolling &&
            !isDeleteCommitting &&
            !isDraggingUp
    val prefetchLightweightOnly = !isCentered
    val shouldTrackCenterCardUi =
        isCentered &&
            !isPagerScrolling &&
            !isDeleteCommitting &&
            !isDraggingUp

    val cardAnalysis = rememberCardAnalysisUiState(
        context = context,
        item = item,
        currentLang = currentLang,
        shouldAnalyze = shouldPrefetchCardAnalysis,
        deferHeavyCardWork = !shouldPrefetchCardAnalysis,
        prefetchLightweightOnly = prefetchLightweightOnly,
        isTopCard = isCentered,
        isExpanded = isExpanded,
        isCardDragging = isDraggingUp || isDeleteCommitting || isPagerScrolling,
        lensyEnabled = lensyEnabled,
        onAiOverlayStateChanged = if (isCentered) onAiOverlayStateChanged else null
    )
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

    LaunchedEffect(item.id) {
        isDeleteCommitting = false
        dragOffsetY = 0f
        preparedUndoToken = Long.MIN_VALUE
        playedUndoToken = Long.MIN_VALUE
        settleOffsetY.snapTo(0f)
        restoreOffsetY.snapTo(0f)
        restoreScale.snapTo(1f)
        restoreAlpha.snapTo(1f)
    }

    LaunchedEffect(isCentered) {
        if (!isCentered) {
            isExpanded = false
            dismissImageLongPressHover()
            onAiOverlayStateChanged(null, false)
        }
    }

    LaunchedEffect(
        colorCardEnabled,
        isCentered,
        isExpanded,
        isDraggingUp,
        isDeleteCommitting,
        isPagerScrolling,
        interactionLocked
    ) {
        if (
            !colorCardEnabled ||
            !isCentered ||
            isExpanded ||
            isDraggingUp ||
            isDeleteCommitting ||
            isPagerScrolling ||
            interactionLocked
        ) {
            dismissImageLongPressHover()
        }
    }

    LaunchedEffect(item.id, shouldTrackCenterCardUi) {
        if (!shouldTrackCenterCardUi) {
            onAiOverlayStateChanged(null, false)
        }
    }

    val renderedOffsetY = if (isDraggingUp) dragOffsetY else settleOffsetY.value
    val deleteProgress = ((-renderedOffsetY) / deleteThresholdPx).coerceIn(0f, 1f)
    val deleteOverlayAlpha = (deleteProgress * 1.15f).coerceIn(0f, 1f)
    val deleteShadowFade = if (isDraggingUp || isDeleteCommitting) {
        (1f - (deleteProgress * 2.2f)).coerceIn(0f, 1f)
    } else {
        1f
    }
    val effectiveCardShadowElevation = cardShadowElevation * deleteShadowFade
    val effectiveCardShadowAmbientColor =
        cardShadowAmbientColor.copy(alpha = cardShadowAmbientColor.alpha * deleteShadowFade)
    val effectiveCardShadowSpotColor =
        cardShadowSpotColor.copy(alpha = cardShadowSpotColor.alpha * deleteShadowFade)
    val shouldPlayUndoAnimation =
        undoAnimationTarget != null && undoAnimationTarget.token != playedUndoToken
    val hideUntilUndoPrepared =
        isCentered &&
            !isPagerScrolling &&
            shouldPlayUndoAnimation &&
            preparedUndoToken != undoAnimationTarget.token

    LaunchedEffect(isCentered, isPagerScrolling, item.id) {
        if (isCentered && !isPagerScrolling) return@LaunchedEffect
        restoreOffsetY.stop()
        restoreScale.stop()
        restoreAlpha.stop()
        restoreOffsetY.snapTo(0f)
        restoreScale.snapTo(1f)
        restoreAlpha.snapTo(1f)
    }

    LaunchedEffect(undoAnimationTarget?.token, isCentered, isPagerScrolling) {
        val target = undoAnimationTarget ?: return@LaunchedEffect
        if (!shouldPlayUndoAnimation || !isCentered || isPagerScrolling) return@LaunchedEffect
        val initialOffset = resolveSnappingCarouselUndoStartOffset(
            target = target,
            deleteThresholdPx = deleteThresholdPx,
            fallbackOffsetPx = restoreOffsetPx,
            offscreenStartOffsetPx = undoRestoreOffscreenStartPx
        )
        val restoreDistanceT = (
            (initialOffset.absoluteValue - restoreOffsetPx) /
                (deleteThresholdPx * 1.7f)
            ).coerceIn(0f, 1f)
        restoreOffsetY.stop()
        restoreScale.stop()
        restoreAlpha.stop()
        restoreOffsetY.snapTo(initialOffset)
        restoreScale.snapTo(0.988f - (restoreDistanceT * 0.02f))
        restoreAlpha.snapTo(0.95f - (restoreDistanceT * 0.08f))
        preparedUndoToken = target.token
        playedUndoToken = target.token
        coroutineScope {
            val interactionReleaseJob = launch {
                delay(HOME_SNAPPING_CAROUSEL_UNDO_INTERACTION_RELEASE_DELAY_MS)
                onUndoAnimationFinished(target.token)
            }
            val offsetJob = launch {
                restoreOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        stiffness = 420f - (restoreDistanceT * 84f),
                        dampingRatio = 0.84f
                    )
                )
            }
            val scaleJob = launch {
                restoreScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = HOME_SNAPPING_CAROUSEL_UNDO_RESTORE_DURATION_MS.toInt(),
                        easing = FastOutSlowInEasing
                    )
                )
            }
            val alphaJob = launch {
                restoreAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = (HOME_SNAPPING_CAROUSEL_UNDO_RESTORE_DURATION_MS * 0.78f).toInt(),
                        easing = FastOutSlowInEasing
                    )
                )
            }
            offsetJob.join()
            scaleJob.join()
            alphaJob.join()
            interactionReleaseJob.cancel()
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(
            0f,
            rememberHomeCarouselCardFrameSpec(quickOrganizeMode).biasY
        )
    ) {
        val carouselCardFrameSpec = rememberHomeCarouselCardFrameSpec(quickOrganizeMode)
        val shadowPaddingPx = with(density) { HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING.toPx() }
        val availableWidth = (maxWidth - (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2)).coerceAtLeast(0.dp)
        val availableHeight = (maxHeight - (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2)).coerceAtLeast(0.dp)
        val cardFrameSize = resolveHomeCardFrameSize(
            availableWidth = availableWidth,
            availableHeight = availableHeight,
            spec = carouselCardFrameSpec
        )
        val cardHeight = cardFrameSize.height
        val cardWidth = cardFrameSize.width
        val deleteExitTargetY = -(
            with(density) { cardHeight.toPx() } * 1.12f +
                (shadowPaddingPx * 2f)
            )

        Box(
            modifier = Modifier
                .width(cardWidth + (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2))
                .height(cardHeight + (HOME_SNAPPING_CAROUSEL_CARD_SHADOW_PADDING * 2))
                .graphicsLayer {
                    translationY = renderedOffsetY + restoreOffsetY.value
                    scaleX = (1f - (deleteProgress * 0.04f)) * restoreScale.value
                    scaleY = (1f - (deleteProgress * 0.018f)) * restoreScale.value
                    alpha =
                        (1f - (deleteProgress * 0.12f)) *
                            restoreAlpha.value *
                            if (hideUntilUndoPrepared) 0f else 1f
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .shadow(
                        elevation = effectiveCardShadowElevation,
                        shape = HOME_SNAPPING_CAROUSEL_CARD_SHAPE,
                        clip = false,
                        ambientColor = effectiveCardShadowAmbientColor,
                        spotColor = effectiveCardShadowSpotColor
                    )
                    .clip(HOME_SNAPPING_CAROUSEL_CARD_SHAPE)
                    .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
                    .border(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.Transparent,
                        HOME_SNAPPING_CAROUSEL_CARD_SHAPE
                    )
                    .onGloballyPositioned { coordinates ->
                        val nextBounds = coordinates.boundsInRoot()
                        cardBoundsInRoot = nextBounds
                        if (shouldTrackCenterCardUi) {
                            onCardBoundsChanged(nextBounds)
                        }
                    }
                    .pointerInput(isCentered, isExpanded, isDeleteCommitting, interactionLocked, item.id) {
                        if (!isCentered || isExpanded || isDeleteCommitting || interactionLocked || item.type == "video") return@pointerInput
                        detectTapGestures(
                            onTap = {
                                if (
                                    isImageLongPressHoverActive ||
                                    isImageLongPressHoverPresented ||
                                    SystemClock.uptimeMillis() < suppressCardTapUntilUptimeMs
                                ) {
                                    return@detectTapGestures
                                }
                                onOpenDetail(imageBoundsInRoot ?: cardBoundsInRoot)
                            }
                        )
                    }
                    .pointerInput(isCentered, isExpanded, isDeleteCommitting, interactionLocked, item.id) {
                        if (!isCentered || isExpanded || isDeleteCommitting || interactionLocked || isImageLongPressHoverPresented) return@pointerInput
                        detectVerticalDragGestures(
                            onDragStart = {
                                scope.launch {
                                    settleOffsetY.stop()
                                    restoreOffsetY.stop()
                                    restoreScale.stop()
                                    restoreAlpha.stop()
                                    restoreOffsetY.snapTo(0f)
                                    restoreScale.snapTo(1f)
                                    restoreAlpha.snapTo(1f)
                                }
                                dragOffsetY = settleOffsetY.value
                                isDraggingUp = true
                            },
                            onDragCancel = {
                                val startOffset = dragOffsetY
                                isDraggingUp = false
                                scope.launch {
                                    settleOffsetY.snapTo(startOffset)
                                    settleOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = currentResetAnimationSpec
                                    )
                                }
                            },
                            onDragEnd = {
                                val startOffset = dragOffsetY
                                val shouldDelete = -startOffset >= deleteThresholdPx
                                isDraggingUp = false
                                scope.launch {
                                    if (shouldDelete) {
                                        isDeleteCommitting = true
                                        onDeleteCommitStart()
                                        settleOffsetY.snapTo(startOffset)
                                        val exitOffset = minOf(startOffset, deleteExitTargetY)
                                        val exitDurationMs = resolveSnappingCarouselDeleteExitDurationMs(
                                            startOffset = startOffset,
                                            exitOffset = exitOffset,
                                            speedMultiplier = currentUpSwipeSpeedMultiplier
                                        )
                                        settleOffsetY.animateTo(
                                            targetValue = exitOffset,
                                            animationSpec = tween(
                                                durationMillis = exitDurationMs,
                                                easing = LinearEasing
                                            )
                                        )
                                        if (hapticsAllowed) {
                                            haptic.performHapticFeedback(
                                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                            )
                                        }
                                        onDeleteSound()
                                        onDelete(exitOffset)
                                    } else {
                                        settleOffsetY.snapTo(startOffset)
                                        settleOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = currentResetAnimationSpec
                                        )
                                    }
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0f && dragOffsetY >= 0f) {
                                    return@detectVerticalDragGestures
                                }
                                change.consume()
                                val liveDeleteProgress =
                                    ((-dragOffsetY) / deleteThresholdPx).coerceIn(0f, 1f)
                                val liveDragFollowFactor = resolveSnappingCarouselUpDragFactor(
                                    speedMultiplier = currentUpSwipeSpeedMultiplier,
                                    deleteProgress = liveDeleteProgress
                                )
                                dragOffsetY =
                                    (dragOffsetY + (dragAmount * liveDragFollowFactor)).coerceAtMost(0f)
                            }
                        )
                    }
            ) {
                DraggableCardFrontContent(
                    item = item,
                    currentItem = currentItem,
                    livePlaybackItem = livePlaybackItem,
                    stillImageRequest = rememberHomeCardStillImageRequest(item.id, item.uri, crossfade = false),
                    videoFrameRequest = rememberHomeCardVideoFrameImageRequest(item.id, item.uri, crossfade = false),
                    shouldRenderVideoPlayer = isCentered && allowPlayback && !isExpanded,
                    shouldPlayVideo = isCentered && allowPlayback && !pauseMainVideos && !isExpanded && !isDraggingUp && !isDeleteCommitting && !isVideoSeeking,
                    isCardDragging = isDraggingUp || isDeleteCommitting || isPagerScrolling,
                    dragPauseToken = 0L,
                    onStartupSample = onStartupSample,
                    onVideoSeekingChange = { isVideoSeeking = it },
                    onOpenFullScreenVideo = onOpenFullScreenVideo,
                    isFullScreenVideoOpen = isFullScreenVideoOpen,
                    isMuted = isMuted,
                    onMutedChange = { newState ->
                        isMuted = newState
                        onMuteVideosChange(newState)
                    },
                    livePlaybackArmed = allowPlayback,
                    livePlaybackCompleted = livePlaybackCompleted,
                    liveReplayToken = liveReplayToken,
                    onLivePlaybackCompletedChange = { livePlaybackCompleted = it },
                    onLiveReplayRequest = {
                        livePlaybackCompleted = false
                        liveReplayToken += 1L
                    },
                    imageLongPressHoverEnabled =
                        colorCardEnabled &&
                            isCentered &&
                            !isExpanded &&
                            !isDraggingUp &&
                            !isDeleteCommitting &&
                            !isPagerScrolling &&
                            !interactionLocked &&
                            item.type != "video",
                    longPressOverlaySourceImageAlpha =
                        if (
                            isCentered &&
                            !isExpanded &&
                            !isDraggingUp &&
                            !isDeleteCommitting &&
                            !isPagerScrolling &&
                            isImageLongPressHoverVisualsActive
                        ) {
                            (1f - hoverVisuals.imageOverlayAlpha).coerceIn(0f, 1f)
                        } else {
                            1f
                        },
                    imageLongPressHoverVisuals = hoverVisuals,
                    onImageLongPressHoverChange = { active ->
                        if (!isDraggingUp && !isDeleteCommitting && !isPagerScrolling && !interactionLocked) {
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
                    showMlLabelsOnHome = aiImageResultsEnabled,
                    enableFooterMarquee = isCentered && !isPagerScrolling && !isDraggingUp && !isDeleteCommitting && !isExpanded,
                    compactFooter = quickOrganizeMode,
                    trackImageBoundsInRoot = shouldTrackCenterCardUi,
                    forceImagePlaceholder = forceHidden,
                    onImageBoundsInRootChanged = { bounds ->
                        imageBoundsInRoot = bounds
                        if (shouldTrackCenterCardUi) {
                            onImageBoundsChanged(bounds)
                        }
                    },
                    onExpandDetails = {
                        if (!isCentered) return@DraggableCardFrontContent
                        isExpanded = true
                    }
                )

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
                imageBoundsInRoot = imageBoundsInRoot,
                windowSizePx = hoverWindowSizePx,
                isDark = isDark,
                onImageReady = {
                    if (isImageLongPressHoverPresented && !isImageLongPressHoverVisualReady) {
                        isImageLongPressHoverVisualReady = true
                    }
                },
                onDismissRequest = dismissImageLongPressHover
            )

            if (isCentered && deleteOverlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.12f + (deleteOverlayAlpha * 0.18f)))
                        .border(
                            1.dp,
                            Color(0xFFDC2626).copy(alpha = 0.18f + (deleteOverlayAlpha * 0.28f)),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC2626).copy(alpha = 0.84f + (deleteOverlayAlpha * 0.16f)),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(y = (-1).dp)
                        )
                        Text(
                            text = LocalAppStrings.current.delete,
                            color = Color(0xFF991B1B).copy(alpha = 0.78f + (deleteOverlayAlpha * 0.22f)),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                }
            }
        }
    }
}
