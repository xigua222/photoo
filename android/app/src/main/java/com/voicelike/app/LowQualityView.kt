package com.voicelike.app

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class LowQualityFullScreenSession(
    val item: MediaItem,
    val reasonKey: String,
    val sourceBoundsInRoot: Rect?,
    val sourceCardBoundsAtOpen: Rect?
)

private data class LowQualityReasonFilterOption(
    val key: String?,
    val label: String,
    val count: Int
)

private val LOW_QUALITY_REASON_DISPLAY_ORDER = listOf(
    "blur",
    "underexposed",
    "overexposed",
    "closedEyes",
    "smallFile",
    "lowResolution",
    "solidColor"
)

@Composable
fun LowQualityView(
    items: List<Pair<MediaItem, String>>,
    isScanning: Boolean,
    progress: Float,
    pendingTrashCount: Int,
    canUndo: Boolean,
    undoAnimationTarget: UndoAnimationTarget? = null,
    muteVideos: Boolean,
    swipeLeftAction: SwipeAction = SwipeAction.Delete,
    swipeRightAction: SwipeAction = SwipeAction.Keep,
    swipeUpAction: SwipeAction = SwipeAction.Keep,
    onClose: () -> Unit,
    onSwipe: (MediaItem, String, SwipeAction, SwipeDirection) -> Unit,
    onUndo: () -> Unit,
    onViewTrash: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    var hiddenSourceItemId by remember { mutableStateOf<Long?>(null) }
    var fullScreenSession by remember { mutableStateOf<LowQualityFullScreenSession?>(null) }
    var fullScreenDismissSettling by remember { mutableStateOf(false) }
    var topCardBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var topCardImageBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var topChromeHeightPx by remember { mutableIntStateOf(0) }
    var bottomChromeHeightPx by remember { mutableIntStateOf(0) }
    var showReasonFilterMenu by remember { mutableStateOf(false) }
    var selectedReasonKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(items.firstOrNull()?.first?.id) {
        if (hiddenSourceItemId != null && items.none { it.first.id == hiddenSourceItemId }) {
            hiddenSourceItemId = null
        }
    }

    if (items.isEmpty() && isScanning) {
        ScanStagePlaceholder(
            title = strings.scanningWait ?: strings.scanning,
            subtitle = strings.scanningDescriptionDetailed
                ?: "This may take 1-3 minutes. You can exit and let it run in the background.",
            isScanning = true,
            progress = progress,
            actionLabel = strings.exit,
            onAction = onClose
        )
        return
    }

    if (items.isEmpty() && !isScanning) {
        ScanStagePlaceholder(
            title = strings.noLowQualityFound,
            subtitle = strings.lowQualityDone,
            isScanning = false,
            progress = 1f,
            actionLabel = strings.done,
            onAction = onClose
        )
        return
    }

    val reasonKeyByItemId = remember(items) {
        items.associate { it.first.id to it.second }
    }
    val presentReasonKeys = remember(items) {
        items.asSequence()
            .map { it.second }
            .distinct()
            .toSet()
    }
    val availableReasonKeys = remember(presentReasonKeys) {
        LOW_QUALITY_REASON_DISPLAY_ORDER.filter { presentReasonKeys.contains(it) } +
            presentReasonKeys
                .filterNot { LOW_QUALITY_REASON_DISPLAY_ORDER.contains(it) }
                .sorted()
    }
    val effectiveSelectedReasonKey = selectedReasonKey?.takeIf { availableReasonKeys.contains(it) }
    LaunchedEffect(selectedReasonKey, effectiveSelectedReasonKey) {
        if (selectedReasonKey != effectiveSelectedReasonKey) {
            selectedReasonKey = effectiveSelectedReasonKey
        }
    }
    val filteredItems = remember(items, effectiveSelectedReasonKey) {
        if (effectiveSelectedReasonKey == null) {
            items
        } else {
            items.filter { it.second == effectiveSelectedReasonKey }
        }
    }
    val currentTopCardId = remember(filteredItems) {
        filteredItems.firstOrNull()?.first?.id
    }
    val displayCountText = remember(filteredItems.size, items.size, effectiveSelectedReasonKey) {
        if (effectiveSelectedReasonKey == null) {
            strings.lowQualityCountLabel(filteredItems.size)
        } else {
            strings.lowQualityFilteredCountLabel(filteredItems.size, items.size)
        }
    }
    val reasonCounts = remember(items) {
        items.groupingBy { it.second }.eachCount()
    }
    val reasonFilterOptions = buildList {
        add(
            LowQualityReasonFilterOption(
                key = null,
                label = strings.filterAll,
                count = items.size
            )
        )
        availableReasonKeys.forEach { reasonKey ->
            add(
                LowQualityReasonFilterOption(
                    key = reasonKey,
                    label = lowQualityReasonLabel(strings, reasonKey),
                    count = reasonCounts[reasonKey] ?: 0
                )
            )
        }
    }
    val selectedReasonLabel = effectiveSelectedReasonKey?.let { lowQualityReasonLabel(strings, it) } ?: strings.filterAll

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        val fallbackTopChromeHeight = 164.dp
        val fallbackBottomChromeHeight = 92.dp
        val measuredTopChromeHeight = if (topChromeHeightPx > 0) {
            with(density) { topChromeHeightPx.toDp() }
        } else {
            fallbackTopChromeHeight
        }
        val measuredBottomChromeHeight = if (bottomChromeHeightPx > 0) {
            with(density) { bottomChromeHeightPx.toDp() }
        } else {
            fallbackBottomChromeHeight
        }
        val containerHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val cardHeightPx = containerHeightPx * 0.6f
        val cardTravelRangePx = (containerHeightPx - cardHeightPx).coerceAtLeast(1f)
        val defaultCardBiasY = -0.12f
        val defaultCardTopPx = cardTravelRangePx * ((defaultCardBiasY + 1f) / 2f)
        // Low-quality header has one extra count line compared with the home screen,
        // so keep the stack noticeably lower to avoid covering that text.
        val additionalCardDropPx = with(density) { 32.dp.toPx() }
        val topSafeGapPx = with(density) { 22.dp.toPx() }
        val bottomSafeGapPx = with(density) { 12.dp.toPx() }
        val minCardTopPx = (with(density) { measuredTopChromeHeight.toPx() } + topSafeGapPx).coerceAtLeast(0f)
        val maxCardTopPx = (
            containerHeightPx - with(density) { measuredBottomChromeHeight.toPx() } - bottomSafeGapPx - cardHeightPx
        ).coerceIn(0f, cardTravelRangePx)
        val resolvedCardTopPx = if (minCardTopPx <= maxCardTopPx) {
            (defaultCardTopPx + additionalCardDropPx).coerceIn(minCardTopPx, maxCardTopPx)
        } else {
            minCardTopPx.coerceAtMost(cardTravelRangePx)
        }
        val lowQualityCardBiasY = (((resolvedCardTopPx / cardTravelRangePx) * 2f) - 1f).coerceIn(-1f, 1f)

        AnimatedVisibility(
            visible = !fullScreenDismissSettling,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp)
                .onSizeChanged { topChromeHeightPx = it.height }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.lowQuality,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(visible = pendingTrashCount > 0) {
                            LowQualityHeaderTrashButton(
                                count = pendingTrashCount,
                                onClick = onViewTrash
                            )
                        }

                        LowQualityHeaderIconButton(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            onClick = onClose
                        )
                    }
                }

                Box {
                    LowQualityReasonFilterButton(
                        label = selectedReasonLabel,
                        isActive = effectiveSelectedReasonKey != null,
                        expanded = showReasonFilterMenu,
                        onClick = { showReasonFilterMenu = !showReasonFilterMenu }
                    )
                    PhotooSelectMenu(
                        expanded = showReasonFilterMenu,
                        onDismissRequest = { showReasonFilterMenu = false },
                        offset = DpOffset(0.dp, 0.dp)
                    ) {
                        reasonFilterOptions.forEach { option ->
                            PhotooSelectMenuItem(
                                label = "${option.label} (${option.count})",
                                selected = option.key == effectiveSelectedReasonKey,
                                onClick = {
                                    selectedReasonKey = option.key
                                    showReasonFilterMenu = false
                                },
                                checkTint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFB91C1C)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayCountText,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.54f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val visibleItems = filteredItems.take(3).reversed()
            visibleItems.forEachIndexed { index, pair ->
                val (item, reasonKey) = pair
                val isTopCard = index == visibleItems.lastIndex
                val stackIndex = visibleItems.lastIndex - index
                val undoTarget = if (isTopCard) {
                    undoAnimationTarget?.takeIf { it.itemId == item.id }
                } else {
                    null
                }
                key(item.id, swipeLeftAction, swipeRightAction, swipeUpAction) {
                    DraggableCard(
                        item = item,
                        forceHidden = hiddenSourceItemId == item.id,
                        isTopCard = isTopCard,
                        stackIndex = stackIndex,
                        muteVideos = muteVideos,
                        lensyEnabled = false,
                        showMlLabelsOnHome = false,
                        suppressActionIndicators = undoTarget != null,
                        undoDirection = undoTarget?.direction,
                        undoToken = undoTarget?.token,
                        footerSecondaryText = formatLowQualityReason(
                            strings = strings,
                            reasonKey = reasonKey,
                            fileSizeText = if (isLowQualitySizeReason(reasonKey)) {
                                Formatter.formatFileSize(context, item.size)
                            } else {
                                null
                            }
                        ),
                        footerSecondaryTextColor = if (isDark) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
                        } else {
                            Color(0xFFDC2626)
                        },
                        pauseMainVideos = false,
                        shouldAnalyze = stackIndex <= 1,
                        deferHeavyCardWork = stackIndex > 1,
                        disableDownSwipeGestures = true,
                        cardBiasY = lowQualityCardBiasY,
                        leftAction = swipeLeftAction,
                        rightAction = swipeRightAction,
                        upAction = swipeUpAction,
                        onTopCardBoundsChanged = if (isTopCard) { bounds ->
                            topCardBoundsInRoot = bounds
                        } else {
                            null
                        },
                        onTopCardImageBoundsChanged = if (isTopCard) { bounds ->
                            topCardImageBoundsInRoot = bounds
                        } else {
                            null
                        },
                        onSwipeLeft = {
                            onSwipe(item, reasonKey, swipeLeftAction, SwipeDirection.Left)
                        },
                        onSwipeRight = {
                            onSwipe(item, reasonKey, swipeRightAction, SwipeDirection.Right)
                        },
                        onSwipeUp = {
                            onSwipe(item, reasonKey, swipeUpAction, SwipeDirection.Up)
                        },
                        onDetail = {
                            fullScreenSession = LowQualityFullScreenSession(
                                item = item,
                                reasonKey = reasonKey,
                                sourceBoundsInRoot = null,
                                sourceCardBoundsAtOpen = if (isTopCard) topCardBoundsInRoot else null
                            )
                            fullScreenDismissSettling = false
                            hiddenSourceItemId = null
                        },
                        onDetailWithSourceBounds = { sourceBounds ->
                            fullScreenSession = LowQualityFullScreenSession(
                                item = item,
                                reasonKey = reasonKey,
                                sourceBoundsInRoot = sourceBounds,
                                sourceCardBoundsAtOpen = if (isTopCard) topCardBoundsInRoot else null
                            )
                            fullScreenDismissSettling = false
                            hiddenSourceItemId = null
                        },
                        onJumpToDay = null
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .onSizeChanged { bottomChromeHeightPx = it.height },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = canUndo && !fullScreenDismissSettling,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Button(
                    onClick = onUndo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        } else {
                            Color.White.copy(alpha = 0.9f)
                        },
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(30.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = strings.undo,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        strings.undo,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        var currentFullScreenSession by remember { mutableStateOf<LowQualityFullScreenSession?>(null) }
        LaunchedEffect(fullScreenSession) {
            fullScreenSession?.let { session ->
                currentFullScreenSession = session
            }
        }
        val activeFullScreenSession = fullScreenSession ?: currentFullScreenSession
        val adjustedDismissSourceBounds = remember(
            activeFullScreenSession?.sourceBoundsInRoot,
            activeFullScreenSession?.sourceCardBoundsAtOpen,
            topCardBoundsInRoot,
            topCardImageBoundsInRoot,
            activeFullScreenSession?.item?.id,
            currentTopCardId
        ) {
            val session = activeFullScreenSession ?: return@remember null
            val baseBounds = session.sourceBoundsInRoot ?: return@remember null
            val sourceItemMatchesTopCard = currentTopCardId != null && session.item.id == currentTopCardId
            if (
                sourceItemMatchesTopCard &&
                topCardImageBoundsInRoot != null &&
                topCardImageBoundsInRoot!!.width > 1f &&
                topCardImageBoundsInRoot!!.height > 1f
            ) {
                topCardImageBoundsInRoot
            } else if (
                sourceItemMatchesTopCard &&
                session.sourceCardBoundsAtOpen != null &&
                topCardBoundsInRoot != null &&
                shouldRemapLowQualityBoundsByCardDelta(
                    openCardBounds = session.sourceCardBoundsAtOpen,
                    currentCardBounds = topCardBoundsInRoot!!
                )
            ) {
                remapLowQualityBoundsByCardDelta(
                    sourceBounds = baseBounds,
                    openCardBounds = session.sourceCardBoundsAtOpen,
                    currentCardBounds = topCardBoundsInRoot!!
                )
            } else {
                baseBounds
            }
        }
        val sharedOpenSourceBounds = remember(activeFullScreenSession?.sourceBoundsInRoot, adjustedDismissSourceBounds) {
            activeFullScreenSession?.sourceBoundsInRoot ?: adjustedDismissSourceBounds
        }
        val shouldUseSharedOpenTransition = remember(activeFullScreenSession, sharedOpenSourceBounds) {
            val session = activeFullScreenSession ?: return@remember false
            val bounds = sharedOpenSourceBounds
            session.item.type == "photo" &&
                bounds != null &&
                bounds.width > 1f &&
                bounds.height > 1f
        }
        PageAnimatedVisibility(
            visible = fullScreenSession != null,
            enter = if (shouldUseSharedOpenTransition) {
                androidx.compose.animation.EnterTransition.None
            } else {
                fadeIn()
            },
            exit = androidx.compose.animation.ExitTransition.None,
            restartContentOnOpen = false
        ) {
            activeFullScreenSession?.let { session ->
                MainFullScreenImageViewer(
                    item = session.item,
                    sourceBoundsInRoot = sharedOpenSourceBounds,
                    dismissSourceBoundsInRoot = adjustedDismissSourceBounds,
                    onSharedDismissStart = { sourceItemId ->
                        fullScreenDismissSettling = true
                        showReasonFilterMenu = false
                        hiddenSourceItemId = sourceItemId
                    },
                    showDayContextDrawer = false,
                    bottomOverlay = { activeItem ->
                        FullScreenMetadataBar(
                            item = activeItem,
                            reasonText = (reasonKeyByItemId[activeItem.id] ?: session.reasonKey)?.let { reasonKey ->
                                formatLowQualityReason(
                                    strings = strings,
                                    reasonKey = reasonKey,
                                    fileSizeText = if (isLowQualitySizeReason(reasonKey)) {
                                        Formatter.formatFileSize(context, activeItem.size)
                                    } else {
                                        null
                                    }
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 14.dp)
                                .bottomNavigationBarPadding(18.dp)
                        )
                    },
                    isMuted = muteVideos,
                    onDismiss = {
                        if (hiddenSourceItemId != null) {
                            fullScreenSession = null
                            scope.launch {
                                withFrameNanos { }
                                if (fullScreenSession == null) {
                                    hiddenSourceItemId = null
                                    fullScreenDismissSettling = false
                                    currentFullScreenSession = null
                                }
                            }
                        } else {
                            fullScreenSession = null
                            hiddenSourceItemId = null
                            fullScreenDismissSettling = false
                            currentFullScreenSession = null
                        }
                    }
                )
            }
        }
    }
}

private fun isLowQualitySizeReason(reasonKey: String): Boolean {
    return reasonKey == "smallFile" || reasonKey == "lowResolution"
}

private fun lowQualityReasonLabel(
    strings: LocalizedStrings,
    reasonKey: String
): String {
    return when (reasonKey) {
        "blur" -> strings.blur
        "underexposed" -> strings.underexposed
        "overexposed" -> strings.overexposed
        "smallFile" -> strings.smallFile
        "lowResolution" -> strings.lowResolution
        "closedEyes" -> strings.closedEyes
        "solidColor" -> strings.solidColor
        else -> reasonKey
    }
}

private fun formatLowQualityReason(
    strings: LocalizedStrings,
    reasonKey: String,
    fileSizeText: String?
): String {
    val baseReason = lowQualityReasonLabel(strings, reasonKey)
    return if (isLowQualitySizeReason(reasonKey) && !fileSizeText.isNullOrBlank()) {
        "$baseReason ($fileSizeText)"
    } else {
        baseReason
    }
}

private fun shouldRemapLowQualityBoundsByCardDelta(
    openCardBounds: Rect,
    currentCardBounds: Rect
): Boolean {
    val openWidth = openCardBounds.width.coerceAtLeast(1f)
    val openHeight = openCardBounds.height.coerceAtLeast(1f)
    val scaleX = currentCardBounds.width / openWidth
    val scaleY = currentCardBounds.height / openHeight
    if (scaleX !in 0.7f..1.35f || scaleY !in 0.7f..1.35f) {
        return false
    }
    val dx = currentCardBounds.center.x - openCardBounds.center.x
    val dy = currentCardBounds.center.y - openCardBounds.center.y
    val centerDistance = kotlin.math.sqrt((dx * dx) + (dy * dy))
    val maxReasonableShift = kotlin.math.max(openWidth, openHeight) * 0.65f + 96f
    return centerDistance <= maxReasonableShift
}

private fun remapLowQualityBoundsByCardDelta(
    sourceBounds: Rect,
    openCardBounds: Rect,
    currentCardBounds: Rect
): Rect {
    val openWidth = openCardBounds.width.coerceAtLeast(1f)
    val openHeight = openCardBounds.height.coerceAtLeast(1f)
    val scaleX = currentCardBounds.width / openWidth
    val scaleY = currentCardBounds.height / openHeight
    val openCenter = openCardBounds.center
    val currentCenter = currentCardBounds.center
    return Rect(
        left = currentCenter.x + ((sourceBounds.left - openCenter.x) * scaleX),
        top = currentCenter.y + ((sourceBounds.top - openCenter.y) * scaleY),
        right = currentCenter.x + ((sourceBounds.right - openCenter.x) * scaleX),
        bottom = currentCenter.y + ((sourceBounds.bottom - openCenter.y) * scaleY)
    )
}

@Composable
private fun LowQualityHeaderTrashButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
    } else {
        Color(0xFFFEF2F2)
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.24f)
    } else {
        Color(0xFFFECACA)
    }
    val contentColor = if (isDark) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        Color(0xFFDC2626)
    }
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "Trash queue",
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = count.toString(),
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LowQualityHeaderIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }
    UnifiedSubpageTopBarIconButton(
        imageVector = imageVector,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier
            .size(36.dp),
        containerColor = containerColor,
        iconTint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
    )
}

@Composable
private fun LowQualityReasonFilterButton(
    label: String,
    isActive: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isActive) {
        if (isDark) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        } else {
            Color(0xFFFEF2F2)
        }
    } else if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }
    val borderColor = if (isActive) {
        if (isDark) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.34f)
        } else {
            Color(0xFFFCA5A5)
        }
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    }
    val textColor = if (isActive) {
        if (isDark) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            Color(0xFFB91C1C)
        }
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f)
    }

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
    }
}
