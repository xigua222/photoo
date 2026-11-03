package com.voicelike.app.stamp

import android.graphics.Bitmap
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.voicelike.app.LocalHapticsEnabled
import java.util.Locale
import kotlin.math.roundToInt

private const val TIMELINE_FALLBACK_ASPECT = 0.76f
private const val TIMELINE_PAGE_SIZE = 24
private const val TIMELINE_PAGE_PREFETCH_THRESHOLD = 6

private data class TimelineStackCardPlacement(
    val offsetX: androidx.compose.ui.unit.Dp,
    val offsetY: androidx.compose.ui.unit.Dp,
    val width: androidx.compose.ui.unit.Dp,
    val rotation: Float
)

private data class TimelineMonthAnchor(
    val isYearMarker: Boolean,
    val label: String,
    val firstItemIndex: Int
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun StampTimelineStage(
    sharedTransitionScope: SharedTransitionScope,
    sharedTransitionGroup: String,
    detailSharedTargetVisible: Boolean,
    detailSharedSourceCardHidden: Boolean,
    entries: List<StampCalendarDayEntry>,
    copy: StampCalendarCopy,
    optimisticStampBitmaps: Map<String, Bitmap>,
    sharedTransitionPreviewBitmaps: Map<String, Bitmap>,
    stableSeedByUri: Map<String, String>,
    stampSkinByUri: Map<String, StampCalendarStampSkin>,
    sourceBoundsByUri: Map<String, Rect>,
    sourceRotationByUri: Map<String, Float>,
    activeDetailUri: String?,
    activeDetailSharedGroup: String?,
    onStampBoundsChange: (String, Rect, Float) -> Unit,
    onSharedTransitionPreviewBitmapResolved: (String, Bitmap) -> Unit,
    onOpenDetail: (StampDetailState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val timelineEntries = remember(entries) {
        sortStampCalendarEntriesAscending(
            entries.filter { it.uris.isNotEmpty() }
        )
    }
    val monthAnchors = remember(timelineEntries) {
        buildTimelineMonthAnchors(timelineEntries)
    }
    var activeMonthAnchorIndex by remember(monthAnchors) {
        mutableIntStateOf(0)
    }
    var fastSwitchDragging by remember { mutableStateOf(false) }
    var fastSwitchLabel by remember { mutableStateOf<String?>(null) }
    var pagedEntryCount by remember(timelineEntries.size) {
        mutableIntStateOf(timelineEntries.size)
    }
    LaunchedEffect(timelineEntries.size) {
        pagedEntryCount = timelineEntries.size
        if (timelineEntries.isEmpty()) return@LaunchedEffect
        // Always open timeline at absolute bottom.
        withFrameNanos { }
        listState.scrollToItem(
            index = timelineEntries.lastIndex + 1,
            scrollOffset = Int.MAX_VALUE
        )
    }
    LaunchedEffect(listState, timelineEntries.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (
                    timelineEntries.isNotEmpty() &&
                    lastVisibleIndex >= (pagedEntryCount - TIMELINE_PAGE_PREFETCH_THRESHOLD) &&
                    pagedEntryCount < timelineEntries.size
                ) {
                    pagedEntryCount = minOf(pagedEntryCount + TIMELINE_PAGE_SIZE, timelineEntries.size)
                }
            }
    }
    LaunchedEffect(listState, monthAnchors, fastSwitchDragging) {
        if (monthAnchors.isEmpty()) {
            activeMonthAnchorIndex = 0
            return@LaunchedEffect
        }
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleItem ->
                if (!fastSwitchDragging) {
                    activeMonthAnchorIndex = resolveTimelineAnchorIndexForItem(
                        itemIndex = firstVisibleItem,
                        anchors = monthAnchors
                    )
                }
            }
    }
    val pagedTimelineEntries = remember(timelineEntries, pagedEntryCount) {
        timelineEntries.take(pagedEntryCount)
    }
    val pagedUris = remember(pagedTimelineEntries) {
        pagedTimelineEntries.flatMap { it.uris }.distinct()
    }
    val aspectByUri = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(pagedUris, context, optimisticStampBitmaps) {
        val missingUris = pagedUris.filter { uri -> !aspectByUri.containsKey(uri) }
        if (missingUris.isNotEmpty()) {
            val resolvedMap = withContext(Dispatchers.IO) {
                missingUris.associateWith { uri ->
                    resolveKnownStampAspectRatio(uri, optimisticStampBitmaps[uri])
                        ?: readStampAspectRatio(context, uri)
                }
            }
            resolvedMap.forEach { (uri, aspect) ->
                aspectByUri[uri] = aspect
            }
        }
        pagedUris.forEach { uri ->
            if (!aspectByUri.containsKey(uri)) {
                aspectByUri[uri] = (
                resolveKnownStampAspectRatio(uri, optimisticStampBitmaps[uri])
                    ?: readStampAspectRatio(context, uri)
                    )
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .paperGridBackground(
                backgroundColor = stampAdaptiveColor(
                    light = Color(0xFFF6F2E7),
                    dark = MaterialTheme.colorScheme.background
                ),
                lineColor = stampAdaptiveColor(
                    light = Color(0xFFE1DAC9).copy(alpha = 0.44f),
                    dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.11f)
                )
            )
    ) {
        val stageWidth = maxWidth
        val axisX = 30.dp
        val dateStartX = 56.dp
        val contentStartX = dateStartX + 2.dp
        val stackTopOffset = 44.dp
        val stackShift = 11.dp
        val baseCardWidth = ((stageWidth - contentStartX - 16.dp) * 0.62f).coerceIn(86.dp, 136.dp)
        val timelineLineColor = stampAdaptiveColor(
            light = Color(0xFFB9AE95).copy(alpha = 0.88f),
            dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.34f)
        )
        val timelineDotColor = stampAdaptiveColor(
            light = Color(0xFF6A5D4E),
            dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.84f)
        )
        val datePrimaryColor = stampAdaptiveColor(
            light = Color(0xFF292116),
            dark = MaterialTheme.colorScheme.onBackground
        )
        val dateSecondaryColor = stampAdaptiveColor(
            light = Color(0xFF6E6457),
            dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .drawBehind {
                    val x = axisX.toPx()
                    drawLine(
                        color = timelineLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
        ) {
            if (timelineEntries.isEmpty()) {
                item(key = "timeline-empty") {
                    Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp, bottom = 46.dp)
                ) {
                        Text(
                            text = copy.emptyHint,
                            color = dateSecondaryColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                        )
                    }
                }
            }

            items(
                items = pagedTimelineEntries,
                key = { entry -> "timeline-${entry.dayKey}" }
            ) { entry ->
                    val primaryUri = entry.selectedUri?.takeIf { it in entry.uris }
                        ?: entry.uris.lastOrNull()
                        ?: return@items
                    val day = stampCalendarDayOfMonth(entry.dayKey)
                    val yymmddLabel = timelineYyMmDdLabel(entry.dayKey)
                    val entryEmoji = entry.emojiByUri[primaryUri]
                    val entryNote = (
                        entry.noteByUri[primaryUri]
                            ?: if (entry.selectedUri == primaryUri) entry.note else null
                        )
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    val item = StampDisplayItem(
                        day = day,
                        dayKey = entry.dayKey,
                        uri = primaryUri,
                        emoji = entryEmoji,
                        note = entryNote
                    )
                    val itemSharedGroup: String? = sharedTransitionGroup
                    val stackUris = remember(entry.dayKey, primaryUri, entry.uris) {
                        buildList {
                            add(primaryUri)
                            entry.uris.asReversed().forEach { uri ->
                                if (uri != primaryUri && size < 4) {
                                    add(uri)
                                }
                            }
                        }
                    }
                    val stackDepth = (stackUris.size - 1).coerceAtLeast(0)
                    val primaryAspect = (aspectByUri[primaryUri] ?: TIMELINE_FALLBACK_ASPECT)
                        .coerceIn(0.55f, 1.45f)
                    val seed = (stableSeedByUri[primaryUri] ?: primaryUri).hashCode() * 37 +
                        entry.dayKey.hashCode()
                    val baseRotation = (((seed and Int.MAX_VALUE) % 420) / 100f) - 2.1f
                    val stackWidth = (baseCardWidth + 42.dp).coerceAtMost(
                        (stageWidth - contentStartX - 16.dp).coerceAtLeast(baseCardWidth)
                    )
                    val stackHeadWidth = baseCardWidth.coerceAtLeast(104.dp)
                    val stackPlacements = remember(
                        stackUris,
                        stackWidth,
                        stackHeadWidth,
                        baseRotation
                    ) {
                        buildTimelineStackPlacements(
                            uris = stackUris,
                            stackWidth = stackWidth,
                            headWidth = stackHeadWidth,
                            primaryRotation = baseRotation
                        )
                    }
                    val stackHeight = stackUris.maxOfOrNull { uri ->
                        val placement = stackPlacements.getValue(uri)
                        val aspect = (aspectByUri[uri] ?: primaryAspect).coerceIn(0.55f, 1.45f)
                        placement.offsetY + (placement.width / aspect)
                    } ?: (stackHeadWidth / primaryAspect)
                    val rowHeight = stackTopOffset + stackHeight + 14.dp
                    var topCardBounds by remember(primaryUri) { mutableStateOf<Rect?>(null) }
                    val hasSharedGroup = !itemSharedGroup.isNullOrBlank()
                    val isActiveSharedSource = hasSharedGroup &&
                        activeDetailSharedGroup == itemSharedGroup &&
                        activeDetailUri != null &&
                        item.uri == activeDetailUri
                    val sourceCardRotation = baseRotation

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = axisX - 4.8.dp, y = 13.dp)
                                .size(9.6.dp)
                                .background(color = timelineDotColor, shape = CircleShape)
                        )

                        Column(
                            modifier = Modifier
                                .offset(x = dateStartX)
                                .width((stageWidth - dateStartX - 16.dp).coerceAtLeast(58.dp))
                                .padding(top = 1.dp)
                        ) {
                            Text(
                                text = yymmddLabel,
                                color = datePrimaryColor,
                                fontSize = 24.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .offset(x = contentStartX, y = stackTopOffset)
                                .width(stackWidth)
                                .height(stackHeight)
                        ) {
                            for (depth in stackDepth downTo 0) {
                                val uri = stackUris[depth]
                                val isPrimary = depth == 0
                                val cardAspect = (aspectByUri[uri] ?: primaryAspect).coerceIn(0.55f, 1.45f)
                                val cardPlacement = stackPlacements.getValue(uri)
                                val cardWidth = cardPlacement.width
                                val cardRotation = if (isPrimary) sourceCardRotation else cardPlacement.rotation
                                val sourceSharedElementModifier = if (isPrimary && hasSharedGroup) {
                                    with(sharedTransitionScope) {
                                        rememberStampSharedElementModifier(
                                            group = itemSharedGroup,
                                            uri = item.uri,
                                            visible = !(
                                                isActiveSharedSource &&
                                                    detailSharedTargetVisible
                                                )
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                                val previewBitmap = resolveTimelinePreviewBitmap(
                                    uri = uri,
                                    sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps,
                                    optimisticStampBitmaps = optimisticStampBitmaps,
                                    stableSeedByUri = stableSeedByUri
                                )

                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = cardPlacement.offsetX,
                                            y = cardPlacement.offsetY
                                        )
                                        .width(cardWidth)
                                        .aspectRatio(cardAspect)
                                        .then(sourceSharedElementModifier)
                                ) {
                                    StampCard(
                                        uri = uri,
                                        previewBitmap = previewBitmap,
                                        skin = stampSkinByUri[uri],
                                        aspectRatio = cardAspect,
                                        rotation = cardRotation,
                                        cardPadding = if (isPrimary) 12.dp else 10.dp,
                                        entryRevealEnabled = false,
                                        onResolvedPreviewBitmap = if (isPrimary && isActiveSharedSource && hasSharedGroup) {
                                            { bitmap ->
                                                onSharedTransitionPreviewBitmapResolved(item.uri, bitmap)
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = if (isPrimary) {
                                            {
                                                val fallbackBounds = topCardBounds ?: sourceBoundsByUri[item.uri]
                                                onOpenDetail(
                                                    StampDetailState(
                                                        item = item,
                                                        startX = fallbackBounds?.let { with(density) { it.left.toDp() } }
                                                            ?: contentStartX,
                                                        startY = fallbackBounds?.let { with(density) { it.top.toDp() } }
                                                            ?: 220.dp,
                                                        startWidth = fallbackBounds
                                                            ?.let { with(density) { it.width.toDp() } }
                                                            ?.coerceAtLeast(84.dp)
                                                            ?: cardWidth,
                                                        aspectRatio = cardAspect,
                                                        startRotation = sourceCardRotation,
                                                        shadowElevation = 0.dp,
                                                        sourceCardPadding = 12.dp,
                                                        sourceShadowStrength = 0.95f,
                                                        enableStackPreview = true,
                                                        useBottomDockTransition = false,
                                                        openedFromCalendarDay = true,
                                                        sharedTransitionGroup = null,
                                                        dayAllScrollOffset = 0,
                                                        animateDayAllEntrance = true
                                                    ).withLiveSourceBounds(
                                                        bounds = sourceBoundsByUri[item.uri],
                                                        rotation = sourceRotationByUri[item.uri],
                                                        density = density
                                                    )
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onBoundsChange = if (isPrimary) {
                                            { bounds ->
                                                topCardBounds = bounds
                                                onStampBoundsChange(item.uri, bounds, sourceCardRotation)
                                            }
                                        } else {
                                            null
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { alpha = 1f }
                                    )
                                }
                            }
                        }
                    }
                }
            item(key = "timeline-bottom-space") {
                Spacer(modifier = Modifier.height(118.dp))
            }
            if (pagedEntryCount < timelineEntries.size) {
                item(key = "timeline-pagination-marker") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                }
            }
        }
        if (monthAnchors.size > 1 && timelineEntries.isNotEmpty()) {
            val lineColor = stampAdaptiveColor(
                light = Color(0xFF3F3830).copy(alpha = 0.56f),
                dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
            )
            val lineActiveColor = stampAdaptiveColor(
                light = Color(0xFF262019),
                dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
            )
            val railLabelSurface = stampAdaptiveColor(
                light = Color(0xFFFCF7EC),
                dark = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
            val railLabelColor = stampAdaptiveColor(
                light = Color(0xFF2D2418),
                dark = MaterialTheme.colorScheme.onSurface
            )
            val railHeight = (maxHeight * 0.44f).coerceIn(194.dp, 336.dp)
            val railWidth = 54.dp
            val railTopInset = 8.dp
            val railBottomInset = 8.dp
            val activeProgress = if (monthAnchors.size <= 1) {
                0f
            } else {
                (activeMonthAnchorIndex.toFloat() / monthAnchors.lastIndex.toFloat()).coerceIn(0f, 1f)
            }
            var railHeightPx by remember(monthAnchors.size) {
                mutableIntStateOf(0)
            }
            var handledAnchorIndex by remember(monthAnchors.size) {
                mutableIntStateOf(-1)
            }

            fun jumpToAnchor(anchorIndex: Int) {
                val safeIndex = anchorIndex.coerceIn(0, monthAnchors.lastIndex)
                if (handledAnchorIndex == safeIndex) return
                handledAnchorIndex = safeIndex
                activeMonthAnchorIndex = safeIndex
                if (fastSwitchDragging && hapticsAllowed) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                val anchor = monthAnchors[safeIndex]
                fastSwitchLabel = anchor.label
                scope.launch {
                    if (anchor.firstItemIndex >= pagedEntryCount) {
                        pagedEntryCount = timelineEntries.size
                        withFrameNanos { }
                    }
                    listState.scrollToItem(anchor.firstItemIndex)
                }
            }

            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.CenterEnd)
                    .padding(end = 0.dp)
                    .width(railWidth + 82.dp)
                    .height(railHeight)
            ) {
                if (!fastSwitchLabel.isNullOrBlank()) {
                    Text(
                        text = fastSwitchLabel.orEmpty(),
                        color = railLabelColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.CenterStart)
                            .offset(y = (railHeight * (activeProgress - 0.5f)).coerceIn((-railHeight / 2.35f), (railHeight / 2.35f)))
                            .background(
                                color = railLabelSurface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                Canvas(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.CenterEnd)
                        .width(railWidth)
                        .height(railHeight)
                        .onSizeChanged { railHeightPx = it.height }
                        .pointerInput(monthAnchors, railHeightPx, pagedEntryCount, timelineEntries.size) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    fastSwitchDragging = true
                                    val index = resolveTimelineFastSwitchIndexFromTouch(
                                        touchY = offset.y,
                                        railHeightPx = railHeightPx,
                                        topInsetPx = with(density) { railTopInset.toPx() },
                                        bottomInsetPx = with(density) { railBottomInset.toPx() },
                                        anchorCount = monthAnchors.size
                                    )
                                    jumpToAnchor(index)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val index = resolveTimelineFastSwitchIndexFromTouch(
                                        touchY = change.position.y,
                                        railHeightPx = railHeightPx,
                                        topInsetPx = with(density) { railTopInset.toPx() },
                                        bottomInsetPx = with(density) { railBottomInset.toPx() },
                                        anchorCount = monthAnchors.size
                                    )
                                    jumpToAnchor(index)
                                },
                                onDragEnd = {
                                    fastSwitchDragging = false
                                    handledAnchorIndex = -1
                                    fastSwitchLabel = null
                                },
                                onDragCancel = {
                                    fastSwitchDragging = false
                                    handledAnchorIndex = -1
                                    fastSwitchLabel = null
                                }
                            )
                        }
                ) {
                    val lineThickness = with(density) { 3.4.dp.toPx() }
                    val yearLineLength = with(density) { 22.dp.toPx() }
                    val monthLineLength = with(density) { 11.dp.toPx() }
                    val topInset = with(density) { railTopInset.toPx() }
                    val bottomInset = with(density) { railBottomInset.toPx() }
                    val rightEdgeX = size.width - with(density) { 1.dp.toPx() }
                    val drawableHeight = (size.height - topInset - bottomInset).coerceAtLeast(1f)

                    monthAnchors.forEachIndexed { index, anchor ->
                        val markerProgress = if (monthAnchors.lastIndex <= 0) {
                            0f
                        } else {
                            index.toFloat() / monthAnchors.lastIndex.toFloat()
                        }
                        val markerY = topInset + (drawableHeight * markerProgress)
                        val markerLength = if (anchor.isYearMarker) yearLineLength else monthLineLength
                        val markerColor = if (index == activeMonthAnchorIndex) {
                            lineActiveColor
                        } else {
                            lineColor
                        }
                        drawLine(
                            color = markerColor,
                            start = Offset(
                                x = rightEdgeX - markerLength,
                                y = markerY
                            ),
                            end = Offset(
                                x = rightEdgeX,
                                y = markerY
                            ),
                            strokeWidth = lineThickness,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

private fun resolveTimelinePreviewBitmap(
    uri: String,
    sharedTransitionPreviewBitmaps: Map<String, Bitmap>,
    optimisticStampBitmaps: Map<String, Bitmap>,
    stableSeedByUri: Map<String, String>
): Bitmap? {
    optimisticStampBitmaps[uri]
        ?.takeUnless { it.isRecycled }
        ?.let { return it }
    return optimisticStampBitmaps.entries.firstOrNull { (key, bitmap) ->
        !bitmap.isRecycled && stableSeedByUri[key] == uri
    }?.value
}

private fun timelineYyMmDdLabel(dayKey: String): String {
    val parsed = parseStampCalendarDate(dayKey)
    val twoDigitYear = ((parsed.year % 100) + 100) % 100
    return String.format(
        Locale.US,
        "%02d/%02d/%02d",
        twoDigitYear,
        parsed.month,
        parsed.day
    )
}

private fun buildTimelineMonthAnchors(
    entries: List<StampCalendarDayEntry>
): List<TimelineMonthAnchor> {
    val firstItemIndexByMonth = linkedMapOf<String, Int>()
    entries.forEachIndexed { index, entry ->
        val parsed = parseStampCalendarDate(entry.dayKey)
        val monthKey = String.format(Locale.US, "%04d-%02d", parsed.year, parsed.month)
        firstItemIndexByMonth.putIfAbsent(monthKey, index)
    }
    var previousYear: Int? = null
    return firstItemIndexByMonth.map { (monthKey, firstItemIndex) ->
        val year = monthKey.substringBefore("-").toIntOrNull() ?: 0
        val month = monthKey.substringAfter("-").toIntOrNull() ?: 1
        val isYearMarker = previousYear == null || previousYear != year
        previousYear = year
        TimelineMonthAnchor(
            isYearMarker = isYearMarker,
            label = String.format(Locale.US, "%04d/%02d", year, month),
            firstItemIndex = firstItemIndex
        )
    }
}

private fun resolveTimelineAnchorIndexForItem(
    itemIndex: Int,
    anchors: List<TimelineMonthAnchor>
): Int {
    if (anchors.isEmpty()) return 0
    val safeItemIndex = itemIndex.coerceAtLeast(0)
    val resolved = anchors.indexOfLast { anchor ->
        safeItemIndex >= anchor.firstItemIndex
    }
    return if (resolved >= 0) resolved else 0
}

private fun resolveTimelineFastSwitchIndexFromTouch(
    touchY: Float,
    railHeightPx: Int,
    topInsetPx: Float,
    bottomInsetPx: Float,
    anchorCount: Int
): Int {
    if (anchorCount <= 1 || railHeightPx <= 0) return 0
    val top = topInsetPx.coerceAtLeast(0f)
    val bottom = bottomInsetPx.coerceAtLeast(0f)
    val minY = top
    val maxY = (railHeightPx.toFloat() - bottom).coerceAtLeast(minY)
    val clampedY = touchY.coerceIn(minY, maxY)
    val trackHeight = (maxY - minY).coerceAtLeast(1f)
    val fraction = ((clampedY - minY) / trackHeight).coerceIn(0f, 1f)
    return (fraction * (anchorCount - 1)).roundToInt().coerceIn(0, anchorCount - 1)
}

private fun buildTimelineStackPlacements(
    uris: List<String>,
    stackWidth: androidx.compose.ui.unit.Dp,
    headWidth: androidx.compose.ui.unit.Dp,
    primaryRotation: Float
): Map<String, TimelineStackCardPlacement> {
    if (uris.isEmpty()) return emptyMap()
    val baseX = ((stackWidth - headWidth) / 2f).coerceAtLeast(0.dp)
    val minWidth = (headWidth * 0.82f).coerceAtLeast(92.dp)
    return buildMap {
        uris.forEachIndexed { index, uri ->
            val isPrimary = index == 0
            val fanDirection = if (index % 2 == 0) -1f else 1f
            val fanX = (12f * fanDirection + (((index % 3) - 1) * 4f)).dp
            val cardWidth = if (isPrimary) {
                headWidth
            } else {
                (headWidth - (index * 6).dp).coerceAtLeast(minWidth)
            }
            val maxX = (stackWidth - cardWidth).coerceAtLeast(0.dp)
            val offsetX = if (isPrimary) {
                baseX
            } else {
                (baseX + fanX).coerceIn(0.dp, maxX)
            }
            val offsetY = if (isPrimary) 0.dp else (index * 7).dp
            val rotation = if (isPrimary) {
                primaryRotation
            } else {
                fanDirection * (7.4f + (((index - 1) % 4) * 1.2f))
            }
            put(
                uri,
                TimelineStackCardPlacement(
                    offsetX = offsetX,
                    offsetY = offsetY,
                    width = cardWidth,
                    rotation = rotation
                )
            )
        }
    }
}
