package com.voicelike.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.voicelike.app.smart.SmartAiTypingOverlay

@Composable
internal fun HomeCardStackArea(
    topCardUiState: HomeTopCardUiState,
    quickOrganizeMode: Boolean,
    fullCardMode: Boolean = false,
    backtrackModeEnabled: Boolean,
    canSwipeBacktrackPrevious: Boolean,
    displayMedia: List<MediaItem>,
    hiddenSourceItemId: Long? = null,
    catalogVerified: Boolean,
    isInitialLoad: Boolean,
    cardEntryOffsets: List<Animatable<Float, AnimationVector1D>>,
    cardEntryRotations: List<Animatable<Float, AnimationVector1D>>,
    cardResetToken: Int,
    isUndoAnimating: Boolean,
    undoAnimationTargets: List<UndoAnimationTarget>,
    homeInteractionPerfController: HomeInteractionPerfController,
    stagedHeavyWorkCards: Int,
    predictedSwipeIntent: SwipeIntent,
    rapidSwipeStreak: Int,
    muteVideos: Boolean,
    lensyEnabled: Boolean,
    aiImageResultsEnabled: Boolean,
    colorCardEnabled: Boolean,
    pauseMainVideos: Boolean,
    topCardPlaybackArmed: Boolean,
    isFullScreenVideoOpen: Boolean,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction,
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onSwipeUp: (MediaItem) -> Unit,
    onOpenDetail: (MediaItem, Rect?) -> Unit,
    onOpenFullScreenVideo: (MediaItem, ExoPlayer) -> Unit,
    onMuteVideosChange: (Boolean) -> Unit,
    onJumpToDay: (String) -> Unit,
    onTopCardStartupSample: (MediaItem, Long) -> Unit,
    playSwipeSound: () -> Unit,
    playDeleteSound: () -> Unit,
    playLikeSound: () -> Unit,
    folderDropTargets: List<FolderData>,
    onFolderDropTargetsChange: (List<FolderData>) -> Unit,
    folderDropGestureController: FolderDropGestureController,
    folderDropTargetRects: Map<String, Rect>,
    onFolderDropTargetRectsChange: (Map<String, Rect>) -> Unit,
    activeDropTargetId: String?,
    onActiveDropTargetIdChange: (String?) -> Unit,
    folderListState: LazyListState,
    mediaForMoveTargets: List<MediaItem>,
    folderUsageState: Map<String, Int>,
    folderLastUsed: Map<String, Long>,
    hiddenFoldersForMoveTargets: Set<String>,
    prioritizedFolders: Set<String>,
    userCreatedFolders: Set<String>,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onRequestNewFolder: () -> Unit,
    onQuickMoveToFolder: (MediaItem, FolderData) -> Boolean,
    // Free area for the photo-frame card box, measured from this area's top / bottom edges
    // (the navigation bar inset is added to the bottom one).
    cardAreaTopInset: Dp = HOME_CARD_AREA_DEFAULT_TOP_INSET,
    cardAreaBottomInset: Dp = HOME_CARD_AREA_DEFAULT_BOTTOM_INSET
) {
    val newPageStrings = getNewPageStrings(LocalAppLanguage.current)
    val backtrackIndicatorColor = Color(0xFF2563EB)
    val effectiveSwipeLeftAction = if (backtrackModeEnabled) SwipeAction.Keep else swipeLeftAction
    val effectiveSwipeRightAction = if (backtrackModeEnabled) SwipeAction.Keep else swipeRightAction
    val effectiveSwipeUpAction = if (backtrackModeEnabled) SwipeAction.Delete else swipeUpAction
    val leftIndicatorSpec = if (backtrackModeEnabled) {
        DraggableCardSwipeIndicatorSpec(
            label = newPageStrings.labBacktrackPreviousLabel,
            color = backtrackIndicatorColor,
            icon = Icons.Outlined.ArrowBack
        )
    } else {
        null
    }
    val rightIndicatorSpec = if (backtrackModeEnabled) {
        DraggableCardSwipeIndicatorSpec(
            label = newPageStrings.labBacktrackNextLabel,
            color = backtrackIndicatorColor,
            icon = Icons.Outlined.ArrowForward
        )
    } else {
        null
    }
    val isDragging = topCardUiState.isDragging
    val isFolderDropMode = topCardUiState.isFolderDropModeActive
    val topCardOffset = topCardUiState.topCardOffset
    val topCardPointerInRoot = topCardUiState.topCardPointerInRoot
    val topCardBoundsInRoot = topCardUiState.topCardBoundsInRoot
    val pageAiOverlayText = topCardUiState.pageAiOverlayText
    val pageAiOverlayVisible = topCardUiState.pageAiOverlayVisible
    val homeCardFrameSpec = rememberHomeStackCardFrameSpec(quickOrganizeMode)
    val quickOrganizeCardBiasY = homeCardFrameSpec.biasY
    val lensyOverlayHeightFraction = if (quickOrganizeMode) 0.56f else 0.6f
    val lensyOverlayOffsetY = if (quickOrganizeMode) (-28).dp else (-30).dp
    val currentActiveDropTargetId = rememberUpdatedState(activeDropTargetId)
    val currentFolderDropTargets = rememberUpdatedState(folderDropTargets)
    val currentOnRequestNewFolder = rememberUpdatedState(onRequestNewFolder)
    val currentOnQuickMoveToFolder = rememberUpdatedState(onQuickMoveToFolder)
    val onTopCardFolderDrop: (MediaItem) -> (() -> Unit)? = remember(topCardUiState) {
        { item: MediaItem ->
            val wasInDropMode = topCardUiState.isFolderDropModeActive
            val targetId = currentActiveDropTargetId.value

            if (!wasInDropMode || targetId == null) {
                topCardUiState.onDragComplete()
                null
            } else if (targetId == "new_folder") {
                {
                    topCardUiState.onDragComplete()
                    currentOnRequestNewFolder.value()
                }
            } else {
                currentFolderDropTargets.value
                    .find { it.id == targetId }
                    ?.let { targetFolder ->
                        {
                            topCardUiState.onDragComplete()
                            currentOnQuickMoveToFolder.value(item, targetFolder)
                            Unit
                        }
                    }
                    ?: run {
                        topCardUiState.onDragComplete()
                        null
                    }
            }
        }
    }

    HomeFolderDropEffects(
        isFolderDropMode = isFolderDropMode,
        mediaForMoveTargets = mediaForMoveTargets,
        folderUsageState = folderUsageState,
        folderLastUsed = folderLastUsed,
        hiddenFoldersForMoveTargets = hiddenFoldersForMoveTargets,
        prioritizedFolders = prioritizedFolders,
        userCreatedFolders = userCreatedFolders,
        currentTopCard = displayMedia.firstOrNull(),
        topCardOffset = topCardOffset,
        topCardPointerInRoot = topCardPointerInRoot,
        topCardBoundsInRoot = topCardBoundsInRoot,
        folderDropTargets = folderDropTargets,
        onFolderDropTargetsChange = onFolderDropTargetsChange,
        folderDropGestureController = folderDropGestureController,
        folderDropTargetRects = folderDropTargetRects,
        onFolderDropTargetRectsChange = onFolderDropTargetRectsChange,
        onActiveDropTargetIdChange = onActiveDropTargetIdChange,
        folderListState = folderListState,
        screenWidthPx = screenWidthPx,
        screenHeightPx = screenHeightPx
    )

    val isDark = isSystemInDarkTheme()
    val edgeGlowState = rememberHomeTopCardEdgeGlowState()
    // Cards cut to the photo, unless the user chose classic full cards (quick organize always uses them).
    val photoFrameMode = !quickOrganizeMode && !fullCardMode
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        val photoFrameGeometry = if (photoFrameMode) {
            remember(maxWidth, maxHeight, cardAreaTopInset, cardAreaBottomInset, navigationBarBottom) {
                resolveHomePhotoFrameStackGeometry(
                    areaWidth = maxWidth,
                    areaHeight = maxHeight,
                    topInset = cardAreaTopInset,
                    bottomInset = cardAreaBottomInset + navigationBarBottom
                )
            }
        } else {
            null
        }

        val glowLeft = organizeSwipeActionGlowColor(effectiveSwipeLeftAction, isDark)
        val glowRight = organizeSwipeActionGlowColor(effectiveSwipeRightAction, isDark)
        val glowTop = organizeSwipeActionGlowColor(effectiveSwipeUpAction, isDark)
        val splitGlowHost = LocalOrganizeEdgeGlowHost.current
        if (splitGlowHost != null) {
            // Split page: the glow is drawn behind both panes by OrganizeSplitLayout.
            SideEffect {
                splitGlowHost.state = edgeGlowState
                splitGlowHost.enabled = !isUndoAnimating
                splitGlowHost.leftColor = glowLeft
                splitGlowHost.rightColor = glowRight
                splitGlowHost.topColor = glowTop
            }
        } else {
            HomeTopCardEdgeGlowLayer(
                state = edgeGlowState,
                enabled = !isUndoAnimating,
                leftColor = glowLeft,
                rightColor = glowRight,
                topColor = glowTop
            )
        }

        HomeCardStackCards(
            topCardUiState = topCardUiState,
            quickOrganizeMode = quickOrganizeMode,
            backtrackModeEnabled = backtrackModeEnabled,
            canSwipeBacktrackPrevious = canSwipeBacktrackPrevious,
            displayMedia = displayMedia,
            hiddenSourceItemId = hiddenSourceItemId,
            catalogVerified = catalogVerified,
            isInitialLoad = isInitialLoad,
            isDragging = isDragging,
            cardEntryOffsets = cardEntryOffsets,
            cardEntryRotations = cardEntryRotations,
            cardResetToken = cardResetToken,
            isUndoAnimating = isUndoAnimating,
            undoAnimationTargets = undoAnimationTargets,
            homeInteractionPerfController = homeInteractionPerfController,
            stagedHeavyWorkCards = stagedHeavyWorkCards,
            predictedSwipeIntent = predictedSwipeIntent,
            rapidSwipeStreak = rapidSwipeStreak,
            muteVideos = muteVideos,
            lensyEnabled = lensyEnabled,
            aiImageResultsEnabled = aiImageResultsEnabled,
            colorCardEnabled = colorCardEnabled,
            pauseMainVideos = pauseMainVideos,
            topCardPlaybackArmed = topCardPlaybackArmed,
            isFullScreenVideoOpen = isFullScreenVideoOpen,
            swipeLeftAction = effectiveSwipeLeftAction,
            swipeRightAction = effectiveSwipeRightAction,
            swipeUpAction = effectiveSwipeUpAction,
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            onSwipeUp = onSwipeUp,
            onOpenDetail = onOpenDetail,
            onOpenFullScreenVideo = onOpenFullScreenVideo,
            onMuteVideosChange = onMuteVideosChange,
            onJumpToDay = onJumpToDay,
            onTopCardStartupSample = onTopCardStartupSample,
            playSwipeSound = playSwipeSound,
            playDeleteSound = playDeleteSound,
            playLikeSound = playLikeSound,
            leftIndicatorSpec = leftIndicatorSpec,
            rightIndicatorSpec = rightIndicatorSpec,
            onTopCardFolderDrop = onTopCardFolderDrop,
            photoFrameGeometry = photoFrameGeometry,
            edgeGlowState = edgeGlowState
        )

        val topPhotoItem = displayMedia.firstOrNull()
        if (
            photoFrameGeometry != null &&
            topPhotoItem != null &&
            lensyEnabled &&
            !pageAiOverlayText.isNullOrBlank()
        ) {
            // The AI overlay hangs 30dp above the top card's photo rect.
            val topPhotoSize = resolveHomeCardPhotoSize(
                boxWidth = photoFrameGeometry.boxWidth,
                boxHeight = photoFrameGeometry.boxHeight,
                aspectRatio = resolveHomeCardPhotoAspectRatio(topPhotoItem)
            )
            val photoLeft = (maxWidth - topPhotoSize.width) / 2f
            val photoTop = photoFrameGeometry.boxCenterY - (topPhotoSize.height / 2f)
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .offset(x = photoLeft, y = photoTop)
                        .width(topPhotoSize.width)
                        .height(topPhotoSize.height)
                ) {
                    SmartAiTypingOverlay(
                        fullText = pageAiOverlayText,
                        visible = pageAiOverlayVisible,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-6).dp, y = (-30).dp)
                    )
                }
            }
        } else if (lensyEnabled && !pageAiOverlayText.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = BiasAlignment(0f, quickOrganizeCardBiasY)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight(lensyOverlayHeightFraction)
                ) {
                    SmartAiTypingOverlay(
                        fullText = pageAiOverlayText,
                        visible = pageAiOverlayVisible,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-6).dp, y = lensyOverlayOffsetY)
                    )
                }
            }
        }

        FolderDropRow(
            visible = isFolderDropMode,
            folders = folderDropTargets,
            activeDropTargetId = activeDropTargetId,
            onFolderPositionsChanged = onFolderDropTargetRectsChange,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .bottomNavigationBarPadding(24.dp),
            pinnedFolderIds = prioritizedFolders,
            listState = folderListState
        )
    }
}

@Composable
private fun HomeCardStackCards(
    topCardUiState: HomeTopCardUiState,
    quickOrganizeMode: Boolean,
    backtrackModeEnabled: Boolean,
    canSwipeBacktrackPrevious: Boolean,
    displayMedia: List<MediaItem>,
    hiddenSourceItemId: Long? = null,
    catalogVerified: Boolean,
    isInitialLoad: Boolean,
    isDragging: Boolean,
    cardEntryOffsets: List<Animatable<Float, AnimationVector1D>>,
    cardEntryRotations: List<Animatable<Float, AnimationVector1D>>,
    cardResetToken: Int,
    isUndoAnimating: Boolean,
    undoAnimationTargets: List<UndoAnimationTarget>,
    homeInteractionPerfController: HomeInteractionPerfController,
    stagedHeavyWorkCards: Int,
    predictedSwipeIntent: SwipeIntent,
    rapidSwipeStreak: Int,
    muteVideos: Boolean,
    lensyEnabled: Boolean,
    aiImageResultsEnabled: Boolean,
    colorCardEnabled: Boolean,
    pauseMainVideos: Boolean,
    topCardPlaybackArmed: Boolean,
    isFullScreenVideoOpen: Boolean,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction,
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onSwipeUp: (MediaItem) -> Unit,
    onOpenDetail: (MediaItem, Rect?) -> Unit,
    onOpenFullScreenVideo: (MediaItem, ExoPlayer) -> Unit,
    onMuteVideosChange: (Boolean) -> Unit,
    onJumpToDay: (String) -> Unit,
    onTopCardStartupSample: (MediaItem, Long) -> Unit,
    playSwipeSound: () -> Unit,
    playDeleteSound: () -> Unit,
    playLikeSound: () -> Unit,
    leftIndicatorSpec: DraggableCardSwipeIndicatorSpec?,
    rightIndicatorSpec: DraggableCardSwipeIndicatorSpec?,
    onTopCardFolderDrop: (MediaItem) -> (() -> Unit)?,
    photoFrameGeometry: HomePhotoFrameStackGeometry? = null,
    edgeGlowState: HomeTopCardEdgeGlowState? = null
) {
    val homeCardFrameSpec = rememberHomeStackCardFrameSpec(quickOrganizeMode)
    val quickOrganizeCardBiasY = homeCardFrameSpec.biasY
    val interactionStableEpoch = homeInteractionPerfController.stableEpoch
    val shouldCompactStack = isDragging || isUndoAnimating
    val interactionCooling =
        interactionStableEpoch >= 0 &&
            (shouldCompactStack || homeInteractionPerfController.shouldDeferNonCriticalWork())
    val idleStackRenderCount = (stagedHeavyWorkCards + 2)
        .coerceAtLeast(STEADY_STACK_RENDER_COUNT)
        .coerceAtMost(ENTRY_STACK_RENDER_COUNT)
    val cooledStackRenderCount = idleStackRenderCount
        .coerceAtMost(ENTRY_STACK_RENDER_COUNT - 1)
        .coerceAtLeast(STEADY_STACK_RENDER_COUNT + 1)
    val activeStackRenderCount = when {
        isInitialLoad -> ENTRY_STACK_RENDER_COUNT
        // Photo-frame stack keeps one covered card behind slot 2 so release-time promotion never pops a card in.
        isDragging -> if (photoFrameGeometry != null) STEADY_STACK_RENDER_COUNT + 1 else STEADY_STACK_RENDER_COUNT
        isUndoAnimating && displayMedia.size > 1 -> 2
        interactionCooling -> cooledStackRenderCount
        else -> idleStackRenderCount
    }
    val visibleItems = displayMedia.take(activeStackRenderCount).reversed()
    val predictionBias = when {
        rapidSwipeStreak >= 5 -> 0.10f
        rapidSwipeStreak >= 3 -> 0.075f
        rapidSwipeStreak >= 2 -> 0.05f
        else -> 0f
    }
    // The stack promotes on release (when the fling starts), not when the exit animation ends.
    var exitingTopItemId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(cardResetToken) { exitingTopItemId = null }
    val promoteForExit = exitingTopItemId != null && exitingTopItemId == displayMedia.firstOrNull()?.id
    val visualTopItem = if (promoteForExit) displayMedia.getOrNull(1) else displayMedia.firstOrNull()
    val visualTopPhotoSize = if (photoFrameGeometry != null && visualTopItem != null) {
        resolveHomeCardPhotoSize(
            boxWidth = photoFrameGeometry.boxWidth,
            boxHeight = photoFrameGeometry.boxHeight,
            aspectRatio = resolveHomeCardPhotoAspectRatio(visualTopItem)
        )
    } else {
        null
    }
    // Entry rotation rides the same spring as the entry translation (deal-in, iOS parity).
    val cardEntryStartOffsets = remember(cardEntryOffsets) { cardEntryOffsets.map { it.value } }

    visibleItems.forEachIndexed { index, item ->
        val isTopCard = index == visibleItems.lastIndex
        val rawStackIndex = visibleItems.lastIndex - index
        val shouldAnalyze = catalogVerified && rawStackIndex <= 1
        val deferCardHeavyWork = when {
            !catalogVerified -> true
            isInitialLoad -> true
            !shouldAnalyze -> true
            shouldCompactStack -> true
            rawStackIndex > 1 && interactionCooling -> true
            else -> false
        }
        val entranceTranslationY =
            if (isInitialLoad) cardEntryOffsets.getOrElse(index) { Animatable(0f) }.value else 0f
        val entryStartOffset = cardEntryStartOffsets.getOrElse(index) { 0f }
        val entranceRotation = when {
            !isInitialLoad -> 0f
            photoFrameGeometry != null && entryStartOffset > 1f ->
                HOME_CARD_ENTRY_ROTATION_SEEDS[rawStackIndex.coerceIn(0, HOME_CARD_ENTRY_ROTATION_SEEDS.lastIndex)] *
                    (entranceTranslationY / entryStartOffset)
            else -> cardEntryRotations.getOrElse(index) { Animatable(0f) }.value
        }
        val visualStackIndex = if (promoteForExit && !isTopCard) {
            (rawStackIndex - 1).coerceAtLeast(0)
        } else {
            rawStackIndex
        }
        val stackScaleCap = if (visualStackIndex > 0 && photoFrameGeometry != null && visualTopPhotoSize != null) {
            resolveHomeCardStackScaleCap(
                topPhoto = visualTopPhotoSize,
                backPhoto = resolveHomeCardPhotoSize(
                    boxWidth = photoFrameGeometry.boxWidth,
                    boxHeight = photoFrameGeometry.boxHeight,
                    aspectRatio = resolveHomeCardPhotoAspectRatio(item)
                )
            )
        } else {
            1f
        }
        val undoTarget = undoAnimationTargets.lastOrNull { it.itemId == item.id }
        val isEntryAnimating = isInitialLoad && entranceTranslationY != 0f
        // Keep visible stack promotion animated even during rapid swipes.
        // Heavy card work is already deferred elsewhere; snapping the stack here hurts perceived smoothness more than it helps.
        val disableStackAnimation = false

        key(item.id, cardResetToken, quickOrganizeMode) {
            Box(
                modifier = Modifier.graphicsLayer {
                    translationY = entranceTranslationY
                    rotationZ = entranceRotation
                }
            ) {
                DraggableCard(
                    item = item,
                    forceHidden = hiddenSourceItemId == item.id,
                    isTopCard = isTopCard,
                    stackIndex = visualStackIndex,
                    muteVideos = muteVideos,
                    lensyEnabled = lensyEnabled,
                    showMlLabelsOnHome = aiImageResultsEnabled,
                    colorCardLongPressEnabled = colorCardEnabled,
                    compactFooter = quickOrganizeMode,
                    pauseMainVideos = pauseMainVideos,
                    topCardPlaybackArmed = topCardPlaybackArmed,
                    shouldAnalyze = shouldAnalyze,
                    deferHeavyCardWork = deferCardHeavyWork,
                    predictedSwipeIntent = predictedSwipeIntent,
                    predictionBias = predictionBias,
                    blockVideoDuringEntry = isEntryAnimating,
                    disableStackAnimation = disableStackAnimation,
                    suppressActionIndicators = undoTarget != null,
                    undoDirection = undoTarget?.direction,
                    undoToken = undoTarget?.token,
                    leftAction = swipeLeftAction,
                    rightAction = swipeRightAction,
                    upAction = swipeUpAction,
                    onSwipeLeft = {
                        onSwipeLeft(item)
                        exitingTopItemId = null
                    },
                    onSwipeRight = {
                        onSwipeRight(item)
                        exitingTopItemId = null
                    },
                    onSwipeUp = {
                        onSwipeUp(item)
                        exitingTopItemId = null
                    },
                    onDetail = { onOpenDetail(item, null) },
                    onDetailWithSourceBounds = { sourceBounds -> onOpenDetail(item, sourceBounds) },
                    onFullScreenVideo = onOpenFullScreenVideo,
                    onMuteChange = onMuteVideosChange,
                    onJumpToDay = onJumpToDay,
                    onStartupSample = if (isTopCard) { sampleMs ->
                        onTopCardStartupSample(item, sampleMs)
                    } else null,
                    isFullScreenVideoOpen = isFullScreenVideoOpen,
                    cardBiasY = photoFrameGeometry?.cardBiasY ?: quickOrganizeCardBiasY,
                    disableDownSwipeGestures = quickOrganizeMode,
                    cardWidthFraction = homeCardFrameSpec.widthFraction,
                    cardHeightFraction = homeCardFrameSpec.heightFraction,
                    cardAspectRatio = homeCardFrameSpec.aspectRatio,
                    cardMaxWidth = homeCardFrameSpec.maxWidth,
                    cardMaxHeight = homeCardFrameSpec.maxHeight,
                    allowSwipeLeft = true,
                    allowSwipeRight = !backtrackModeEnabled || canSwipeBacktrackPrevious,
                    allowSwipeUp = true,
                    swipeTiltEnabled = !backtrackModeEnabled,
                    leftIndicatorSpec = leftIndicatorSpec,
                    rightIndicatorSpec = rightIndicatorSpec,
                    onSwipeSound = playSwipeSound,
                    onDeleteSound = playDeleteSound,
                    onLikeSound = playLikeSound,
                    onDragStart = {
                        topCardUiState.onDragStart {
                            homeInteractionPerfController.onDragStart()
                        }
                    },
                    onDragCancel = {
                        topCardUiState.onDragCancel()
                    },
                    onDragUpdate = if (isTopCard) { x, y ->
                        val shouldTrackFolderDropDrag =
                            !quickOrganizeMode &&
                                (
                                    topCardUiState.isFolderDropModeActive ||
                                        y > DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX
                                    )
                        if (shouldTrackFolderDropDrag) {
                            topCardUiState.updateDragOffset(
                                x = x,
                                y = y,
                                quickOrganizeMode = quickOrganizeMode,
                                downSwipeMoveThresholdPx = DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX
                            )
                        }
                    } else null,
                    onDragPointerUpdate = if (isTopCard) { x, y ->
                        if (topCardUiState.isFolderDropModeActive) {
                            topCardUiState.updatePointerInRoot(x, y)
                        }
                    } else null,
                    onTopCardBoundsChanged = if (isTopCard) { bounds ->
                        topCardUiState.updateBounds(bounds)
                    } else null,
                    onTopCardImageBoundsChanged = if (isTopCard) { bounds ->
                        topCardUiState.updateImageBounds(bounds)
                    } else null,
                    onAiOverlayStateChanged = if (isTopCard) { text, visible ->
                        topCardUiState.updateAiOverlay(
                            text = text,
                            visible = visible,
                            lensyEnabled = lensyEnabled
                        )
                    } else null,
                    onSwipeIntentPreview = if (isTopCard) { intent, strength, allowGlow ->
                        topCardUiState.updateSwipePreview(
                            intent = intent,
                            strength = strength,
                            allowGlow = allowGlow
                        )
                    } else null,
                    onDragVelocityUpdate = null,
                    onDragEnd = if (isTopCard) {
                        { _, _ -> onTopCardFolderDrop(item) }
                    } else {
                        null
                    },
                    photoFrameMode = photoFrameGeometry != null,
                    cardBoxWidth = photoFrameGeometry?.boxWidth ?: Dp.Unspecified,
                    cardBoxHeight = photoFrameGeometry?.boxHeight ?: Dp.Unspecified,
                    stackScaleCap = stackScaleCap,
                    onSwipeGlowFrame = if (isTopCard && edgeGlowState != null) { x, y, thresholdPx, active ->
                        if (active) {
                            edgeGlowState.onDragFrame(
                                offsetXPx = x,
                                offsetYPx = y,
                                horizontalThresholdPx = thresholdPx,
                                folderDropThresholdPx = if (quickOrganizeMode) {
                                    Float.POSITIVE_INFINITY
                                } else {
                                    DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX
                                }
                            )
                        } else {
                            edgeGlowState.release()
                        }
                    } else null,
                    onSwipeCommitStart = if (isTopCard) { _ ->
                        exitingTopItemId = item.id
                    } else null
                )
            }
        }
    }
}

/** Card box and its placement inside the stack area (iOS CardSortingView / CardStackEngine parity). */
internal data class HomePhotoFrameStackGeometry(
    val boxWidth: Dp,
    val boxHeight: Dp,
    // Box centre measured from the area top: free-area centre raised by half the stack peek.
    val boxCenterY: Dp,
    val cardBiasY: Float
)

internal fun resolveHomePhotoFrameStackGeometry(
    areaWidth: Dp,
    areaHeight: Dp,
    topInset: Dp,
    bottomInset: Dp
): HomePhotoFrameStackGeometry {
    val freeHeight = (areaHeight - topInset - bottomInset).coerceAtLeast(0.dp)
    val box = resolveHomeAdaptiveCardBoxSize(areaWidth = areaWidth, areaHeight = freeHeight)
    val peek = resolveHomeCardStackPeek(box.height)
    val centerY = topInset + (freeHeight / 2f) - (peek / 2f)
    val slack = areaHeight - box.height
    val bias = if (slack > 0.dp) {
        ((((centerY - (box.height / 2f)) * 2f) / slack) - 1f).coerceIn(-1f, 1f)
    } else {
        0f
    }
    return HomePhotoFrameStackGeometry(
        boxWidth = box.width,
        boxHeight = box.height,
        boxCenterY = centerY,
        cardBiasY = bias
    )
}
