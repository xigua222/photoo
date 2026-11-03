package com.voicelike.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.voicelike.app.i18n.FilterFeedStrings

private const val COLOR_CARD_FILTER_FEED_BASE_POOL_EXTRA_SIZE = 24
private const val COLOR_CARD_FILTER_FEED_REFRESH_MIN_DURATION_MS = 520L
private const val COLOR_CARD_FILTER_FEED_FIXED_ASSIGNMENT_SEED = 0xFEED1024L
private const val COLOR_CARD_FILTER_FEED_BLACK_WHITE_INTERVAL = 6
private const val COLOR_CARD_FILTER_FEED_INITIAL_CYCLE_COUNT = 2
private const val COLOR_CARD_FILTER_FEED_LOAD_MORE_THRESHOLD = 8
private const val COLOR_CARD_FILTER_FEED_BOUNDARY_AVOID_COUNT = 4
private const val COLOR_CARD_FILTER_FEED_RESET_DELAY_MS = 260L
private const val COLOR_CARD_FILTER_FEED_PREVIEW_HEIGHT_DP = 168
private const val COLOR_CARD_FILTER_FEED_HIGH_FIDELITY_INITIAL_ITEMS = 6
private const val COLOR_CARD_FILTER_FEED_HIGH_FIDELITY_GRID_AHEAD_ROWS = 2
private var colorCardFilterFeedStableSeed: Long = 0xC0110CA4L

internal data class ColorCardFilterFeedEntry(
    val key: String,
    val candidate: ColorCardTraceFilterCandidate,
    val demoAssetName: String
)

private data class ColorCardFilterFeedSource(
    val candidate: ColorCardTraceFilterCandidate,
    val demoAsset: ColorCardFilterFeedDemoAsset
)

internal data class ColorCardFilterFeedSeriesEntry(
    val entryIndex: Int,
    val entry: ColorCardFilterFeedEntry
)

internal data class ColorCardFilterFeedModernSeries(
    val key: String,
    val sectionKey: String,
    val entries: List<ColorCardFilterFeedSeriesEntry>
)

internal data class ColorCardFilterFeedModernUiState(
    val series: List<ColorCardFilterFeedModernSeries>,
    val previewBitmaps: Map<String, Bitmap>
)

private fun colorCardFilterFeedPreviewKey(
    candidate: ColorCardTraceFilterCandidate,
    demoAssetName: String
): String = buildColorCardFilterFeedPreviewKey(candidate, demoAssetName)

private fun colorCardFilterFeedPreviewKey(entry: ColorCardFilterFeedEntry): String =
    colorCardFilterFeedPreviewKey(entry.candidate, entry.demoAssetName)

@Composable
internal fun ColorCardFilterPreviewPage(
    title: String? = null,
    onBack: () -> Unit,
    onTryFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var listResetToken by rememberSaveable { mutableIntStateOf(0) }
    val listState = remember(listResetToken) { LazyListState() }
    val resolvedTitle = title ?: FilterFeedStrings.colorCardFilterFeedPreviewTitle(LocalAppLanguage.current)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ColorCardFilterPreviewTopBar(
            title = resolvedTitle,
            onBack = onBack
        )
        ColorCardFilterFeedTab(
            onTryFilter = onTryFilter,
            listState = listState,
            onFeedRefreshReset = {
                listResetToken += 1
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ColorCardFilterPreviewTopBar(
    title: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = LocalAppStrings.current.back,
                onClick = onBack,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                iconTint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp)
        )
    }
}

@Composable
internal fun ColorCardFilterFeedTab(
    onTryFilter: (String) -> Unit,
    listState: LazyListState,
    onFeedRefreshReset: () -> Unit,
    modernListStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (modernListStyle) {
        val modernUiState = rememberColorCardFilterFeedModernUiState()
        if (modernUiState.series.isEmpty()) {
            ColorCardFilterFeedLoadingState(modifier = modifier)
            return
        }

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                items(modernUiState.series, key = { it.key }) { series ->
                    ColorCardFilterFeedModernSeriesRow(
                        series = series,
                        previewBitmaps = modernUiState.previewBitmaps,
                        onTryFilter = onTryFilter
                    )
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val demoAssets = rememberColorCardFilterFeedDemoAssets()
    val bootstrapAssets = rememberColorCardFilterFeedBootstrapPreviewAssets()
    val bootstrapPreviewKeys = remember(bootstrapAssets) {
        bootstrapAssets.mapTo(mutableSetOf()) { it.previewKey }
    }
    var refreshSeed by rememberSaveable { mutableLongStateOf(colorCardFilterFeedStableSeed) }
    var isRefreshing by remember { mutableStateOf(false) }
    val builtinFilters by produceState(
        initialValue = emptyList<ColorCardBuiltinHaldFilterAsset>(),
        key1 = context
    ) {
        value = loadColorCardBuiltinHaldFilters(context.applicationContext)
    }
    val candidates = remember(builtinFilters) {
        buildColorCardTraceBuiltinCandidates(builtinFilters)
            .filterNot { candidate ->
                candidate.kind == ColorCardTraceFilterKind.AiMnn ||
                    candidate.kind == ColorCardTraceFilterKind.MotionPhotoEntry ||
                    candidate.paletteId == "builtin_creative:subject-frame"
            }
    }
    var loadedCycleCount by remember(candidates, demoAssets, refreshSeed) {
        mutableIntStateOf(COLOR_CARD_FILTER_FEED_INITIAL_CYCLE_COUNT)
    }
    val gridFeedEntries = remember(candidates, demoAssets, bootstrapAssets, refreshSeed, loadedCycleCount) {
        buildColorCardFilterFeedEntries(
            candidates = candidates,
            demoAssets = demoAssets,
            bootstrapAssets = bootstrapAssets,
            refreshSeed = refreshSeed,
            cycleCount = loadedCycleCount
        )
    }
    val feedEntries = gridFeedEntries
    val orderedFeedEntries = remember(feedEntries, bootstrapPreviewKeys, context) {
        feedEntries.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<ColorCardFilterFeedEntry>> { indexed ->
                    hasColorCardFilterFeedPreviewReady(
                        context = context.applicationContext,
                        previewKey = colorCardFilterFeedPreviewKey(indexed.value),
                        bootstrapPreviewKeys = bootstrapPreviewKeys
                    )
                }.thenBy { it.index }
            )
            .map { it.value }
    }
    val orderedFeedRows = remember(orderedFeedEntries) { orderedFeedEntries.chunked(2) }
    val highFidelityEntryIndices by remember(
        listState,
        orderedFeedEntries,
        orderedFeedRows
    ) {
        derivedStateOf {
            if (orderedFeedEntries.isEmpty()) {
                emptySet()
            } else {
                val initialBudget = COLOR_CARD_FILTER_FEED_HIGH_FIDELITY_INITIAL_ITEMS
                val initialIndices = (0 until minOf(
                    initialBudget,
                    orderedFeedEntries.size
                )).toSet()
                val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
                val firstVisibleRow = visibleIndices.minOrNull() ?: 0
                val lastVisibleRow = visibleIndices.maxOrNull() ?: 1
                val startIndex = (firstVisibleRow * 2).coerceAtMost(orderedFeedEntries.lastIndex)
                val endIndex = (((lastVisibleRow + COLOR_CARD_FILTER_FEED_HIGH_FIDELITY_GRID_AHEAD_ROWS + 1) * 2) - 1)
                    .coerceAtMost(orderedFeedEntries.lastIndex)
                buildSet {
                    addAll(initialIndices)
                    if (startIndex <= endIndex) {
                        for (index in startIndex..endIndex) {
                            add(index)
                        }
                    }
                }
            }
        }
    }
    val previewRequests = remember(orderedFeedEntries, highFidelityEntryIndices) {
        orderedFeedEntries.mapIndexed { index, entry ->
            val isPSeries = isColorCardTracePSeriesCandidate(entry.candidate)
            val forceRealtimePreview = shouldColorCardFilterFeedForceRealtimePreview(entry.candidate)
            ColorCardFilterFeedPreviewRequest(
                previewKey = colorCardFilterFeedPreviewKey(entry),
                candidate = entry.candidate,
                demoAssetName = entry.demoAssetName,
                highFidelity = isPSeries || forceRealtimePreview || index in highFidelityEntryIndices,
                allowApproximate = !isPSeries,
                preferStylizedPreview = false,
                runtimeGenerationEnabled = true
            )
        }
    }
    val previewBitmaps = rememberColorCardFilterFeedPreviewBitmaps(previewRequests)

    if (feedEntries.isEmpty()) {
        ColorCardFilterFeedLoadingState(modifier = modifier)
        return
    }

    val visibleFeedCount = orderedFeedRows.size
    val shouldLoadMore by remember(listState, visibleFeedCount) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val remainingThreshold = (COLOR_CARD_FILTER_FEED_LOAD_MORE_THRESHOLD / 2).coerceAtLeast(1)
            val triggerIndex = visibleFeedCount - remainingThreshold
            visibleFeedCount > 0 && lastVisibleIndex >= triggerIndex.coerceAtLeast(0)
        }
    }

    LaunchedEffect(shouldLoadMore, visibleFeedCount) {
        if (shouldLoadMore && visibleFeedCount > 0) {
            loadedCycleCount += 1
        }
    }

    val listContent: @Composable () -> Unit = {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (visibleFeedCount == 0) {
                item(key = "filter-feed-loading") {
                    ColorCardFilterFeedLoadingState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    )
                }
            }
            items(orderedFeedRows, key = { row -> row.joinToString("_") { it.key } }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { entry ->
                        val previewKey = colorCardFilterFeedPreviewKey(entry)
                        ColorCardFilterFeedCard(
                            entry = entry,
                            previewBitmap = previewBitmaps[previewKey],
                            onClick = { onTryFilter(entry.candidate.paletteId) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (isRefreshing) {
                item(key = "filter-feed-refreshing") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colorCardFilterFeedAccentColor(),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        onRefresh = {
            if (isRefreshing) return@PullToRefreshBox
            scope.launch {
                val refreshStartedAt = System.currentTimeMillis()
                isRefreshing = true
                delay(COLOR_CARD_FILTER_FEED_RESET_DELAY_MS)
                refreshSeed = Random.nextLong()
                colorCardFilterFeedStableSeed = refreshSeed
                onFeedRefreshReset()
                val elapsed = System.currentTimeMillis() - refreshStartedAt
                val remainingDelay =
                    (COLOR_CARD_FILTER_FEED_REFRESH_MIN_DURATION_MS - elapsed).coerceAtLeast(0L)
                delay(remainingDelay)
                isRefreshing = false
            }
        },
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                color = colorCardFilterFeedAccentColor()
            )
        }
    ) {
        listContent()
    }
}

@Composable
internal fun rememberColorCardFilterFeedModernUiState(): ColorCardFilterFeedModernUiState {
    val context = LocalContext.current
    val demoAssets = rememberColorCardFilterFeedDemoAssets()
    val bootstrapAssets = rememberColorCardFilterFeedBootstrapPreviewAssets()
    val shuffleSeed by rememberSaveable { mutableLongStateOf(Random.Default.nextLong()) }
    val builtinFilters by produceState(
        initialValue = emptyList<ColorCardBuiltinHaldFilterAsset>(),
        key1 = context
    ) {
        value = loadColorCardBuiltinHaldFilters(context.applicationContext)
    }
    val candidates = remember(builtinFilters) {
        buildColorCardTraceBuiltinCandidates(builtinFilters)
            .filterNot { candidate ->
                candidate.kind == ColorCardTraceFilterKind.AiMnn ||
                    candidate.kind == ColorCardTraceFilterKind.MotionPhotoEntry ||
                    candidate.paletteId == "builtin_creative:subject-frame"
            }
    }
    val modernFeedEntries = remember(candidates, demoAssets, bootstrapAssets) {
        buildColorCardFilterFeedModernEntries(
            candidates = candidates,
            demoAssets = demoAssets,
            bootstrapAssets = bootstrapAssets
        )
    }
    val modernSeries = remember(modernFeedEntries, shuffleSeed) {
        organizeColorCardFilterFeedModernSeries(
            buildColorCardFilterFeedModernSeries(modernFeedEntries),
            shuffleSeed = shuffleSeed
        )
    }
    val previewRequests = remember(modernFeedEntries) {
        modernFeedEntries.map { entry ->
            val isPSeries = isColorCardTracePSeriesCandidate(entry.candidate)
            val forceRealtimePreview = shouldColorCardFilterFeedForceRealtimePreview(entry.candidate)
            ColorCardFilterFeedPreviewRequest(
                previewKey = colorCardFilterFeedPreviewKey(entry),
                candidate = entry.candidate,
                demoAssetName = entry.demoAssetName,
                highFidelity = isPSeries || forceRealtimePreview,
                allowApproximate = forceRealtimePreview,
                preferStylizedPreview = !isPSeries && !forceRealtimePreview,
                runtimeGenerationEnabled = isPSeries || forceRealtimePreview
            )
        }
    }
    val previewBitmaps = rememberColorCardFilterFeedPreviewBitmaps(previewRequests)

    return ColorCardFilterFeedModernUiState(
        series = modernSeries,
        previewBitmaps = previewBitmaps
    )
}

@Composable
internal fun ColorCardFilterFeedModernSeriesRow(
    series: ColorCardFilterFeedModernSeries,
    previewBitmaps: Map<String, Bitmap>,
    onTryFilter: (String) -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = colorCardFilterFeedModernSeriesHeadlineWithEmoji(
                    appLanguage = appLanguage,
                    sectionKey = series.sectionKey
                ),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(series.entries, key = { it.entry.key }) { seriesEntry ->
                val entry = seriesEntry.entry
                val previewKey = colorCardFilterFeedPreviewKey(entry)
                ColorCardFilterFeedModernPresetCard(
                    entry = entry,
                    previewBitmap = previewBitmaps[previewKey],
                    onClick = { onTryFilter(entry.candidate.paletteId) }
                )
            }
        }
    }
}

@Composable
internal fun ColorCardFilterFeedModernPresetCard(
    entry: ColorCardFilterFeedEntry,
    previewBitmap: Bitmap?,
    onClick: () -> Unit
) {
    val previewHeight = 148.dp
    val cardWidth = 112.dp
    val cardShape = RoundedCornerShape(12.dp)
    val cardInteractionSource = remember { MutableInteractionSource() }
    val previewInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = cardShape
                )
                .clickable(
                    interactionSource = previewInteractionSource,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null && !previewBitmap.isRecycled) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = entry.candidate.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ColorCardFilterFeedLoadingPreview(
                    colors = entry.candidate.colors,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.candidate.title,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun ColorCardFilterFeedCard(
    entry: ColorCardFilterFeedEntry,
    previewBitmap: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage = LocalAppLanguage.current
    val isDark = isSystemInDarkTheme()
    val cardBorderColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val cardInteractionSource = remember { MutableInteractionSource() }
    val previewInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                cardBorderColor,
                RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onClick
            )
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(COLOR_CARD_FILTER_FEED_PREVIEW_HEIGHT_DP.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(
                    Brush.verticalGradient(
                        colors = entry.candidate.colors
                            .ifEmpty { listOf(0xFFCBCBCB.toInt(), 0xFF8B8B8B.toInt()) }
                            .map(::Color)
                    )
                )
                .clickable(
                    interactionSource = previewInteractionSource,
                    onClick = onClick
                )
        ) {
            if (previewBitmap != null && !previewBitmap.isRecycled) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = entry.candidate.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ColorCardFilterFeedLoadingPreview(
                    colors = entry.candidate.colors,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.24f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = FilterFeedStrings.colorCardFilterFeedCategoryTag(appLanguage, entry.candidate),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.candidate.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = colorCardFilterFeedAccentColor(),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(16.dp)
                    )
                }
                FilterFeedStrings.getColorCardFilterFeedDescription(appLanguage, entry.candidate)?.let { description ->
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCardFilterFeedLoadingPreview(
    colors: List<Int>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = colors.ifEmpty {
                    listOf(0xFF3A3A3A.toInt(), 0xFF1F1F1F.toInt())
                }.map(::Color)
            )
        )
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp),
            color = Color.White.copy(alpha = 0.94f),
            strokeWidth = 2.2.dp,
            trackColor = Color.Black.copy(alpha = 0.16f)
        )
    }
}

@Composable
internal fun ColorCardFilterFeedLoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = colorCardFilterFeedAccentColor())
    }
}

private fun buildColorCardFilterFeedEntries(
    candidates: List<ColorCardTraceFilterCandidate>,
    demoAssets: List<ColorCardFilterFeedDemoAsset>,
    bootstrapAssets: List<ColorCardFilterFeedBootstrapPreviewAsset>,
    refreshSeed: Long,
    cycleCount: Int
): List<ColorCardFilterFeedEntry> {
    if (candidates.isEmpty() || demoAssets.isEmpty()) return emptyList()
    val safeCycleCount = cycleCount.coerceAtLeast(1)
    return buildList {
        var previousTailIds = emptyList<String>()
        repeat(safeCycleCount) { cycleIndex ->
            val cycleSeed = refreshSeed + (cycleIndex * 9973L)
            val cycleSources = buildColorCardFilterFeedCycleSources(
                candidates = candidates,
                demoAssets = demoAssets,
                bootstrapAssets = bootstrapAssets,
                cycleSeed = cycleSeed,
                previousTailIds = previousTailIds
            )
            cycleSources.forEachIndexed { indexInCycle, source ->
                val absoluteIndex = size
                add(
                    ColorCardFilterFeedEntry(
                        key = "filter-feed-${cycleIndex}-${source.candidate.paletteId}-${source.demoAsset.id}-$indexInCycle",
                        candidate = source.candidate,
                        demoAssetName = source.demoAsset.assetName
                    )
                )
            }
            previousTailIds = cycleSources
                .takeLast(COLOR_CARD_FILTER_FEED_BOUNDARY_AVOID_COUNT)
                .map { it.candidate.paletteId }
        }
    }
}

private fun buildColorCardFilterFeedModernSeries(
    orderedFeedEntries: List<ColorCardFilterFeedEntry>
): List<ColorCardFilterFeedModernSeries> {
    if (orderedFeedEntries.isEmpty()) return emptyList()
    val rawEntries = orderedFeedEntries.mapIndexed { index, entry ->
        resolveColorCardFilterFeedModernSeriesKey(entry.candidate) to
            ColorCardFilterFeedSeriesEntry(entryIndex = index, entry = entry)
    }
    val rawCounts = rawEntries.groupingBy { it.first }.eachCount()
    val groupedEntries = linkedMapOf<String, MutableList<ColorCardFilterFeedSeriesEntry>>()
    rawEntries.forEach { (rawKey, seriesEntry) ->
        val sectionKey = resolveColorCardFilterFeedEffectiveSeriesKey(
            rawKey = rawKey,
            rawCount = rawCounts[rawKey] ?: 0,
            rawCounts = rawCounts
        )
        groupedEntries.getOrPut(sectionKey) { mutableListOf() }
            .add(seriesEntry)
    }
    return buildList {
        groupedEntries.forEach { (key, entries) ->
            val chunkPlans = buildColorCardFilterFeedModernSeriesChunks(
                entryCount = entries.size,
                preferredChunkSize = colorCardFilterFeedModernSeriesChunkSize(key)
            )
            var startIndex = 0
            chunkPlans.forEachIndexed { chunkIndex, chunkSize ->
                val endIndex = (startIndex + chunkSize).coerceAtMost(entries.size)
                val chunk = entries.subList(startIndex, endIndex)
                add(
                    ColorCardFilterFeedModernSeries(
                        key = "$key:$chunkIndex",
                        sectionKey = key,
                        entries = chunk
                    )
                )
                startIndex = endIndex
            }
        }
    }
}

private fun organizeColorCardFilterFeedModernSeries(
    source: List<ColorCardFilterFeedModernSeries>,
    shuffleSeed: Long
): List<ColorCardFilterFeedModernSeries> {
    if (source.isEmpty()) return emptyList()
    val random = Random(shuffleSeed)
    return source
        .map { series -> series.copy(entries = series.entries.shuffled(random)) }
        .shuffled(random)
}

private fun buildColorCardFilterFeedModernEntries(
    candidates: List<ColorCardTraceFilterCandidate>,
    demoAssets: List<ColorCardFilterFeedDemoAsset>,
    bootstrapAssets: List<ColorCardFilterFeedBootstrapPreviewAsset>
): List<ColorCardFilterFeedEntry> {
    if (candidates.isEmpty() || demoAssets.isEmpty()) return emptyList()
    val preferredDemoAssets = demoAssets.filterNot { it.id == "watermark_template_demo" }
        .ifEmpty { demoAssets }
    val demoAssetsByName = demoAssets.associateBy { it.assetName }
    val bootstrapDemoByPaletteId = bootstrapAssets
        .groupBy { it.paletteId }
        .mapValues { (_, assets) ->
            assets.firstNotNullOfOrNull { asset ->
                demoAssetsByName[asset.demoAssetName]
            }
        }
    val fallbackDemoAssets = preferredDemoAssets.shuffled(Random(COLOR_CARD_FILTER_FEED_FIXED_ASSIGNMENT_SEED))
    return candidates.mapIndexed { index, candidate ->
        val assignedDemoAsset = bootstrapDemoByPaletteId[candidate.paletteId]
            ?: fallbackDemoAssets[index % fallbackDemoAssets.size]
        ColorCardFilterFeedEntry(
            key = "filter-modern-${candidate.paletteId}-${assignedDemoAsset.id}",
            candidate = candidate,
            demoAssetName = assignedDemoAsset.assetName
        )
    }
}

private fun buildColorCardFilterFeedCycleSources(
    candidates: List<ColorCardTraceFilterCandidate>,
    demoAssets: List<ColorCardFilterFeedDemoAsset>,
    bootstrapAssets: List<ColorCardFilterFeedBootstrapPreviewAsset>,
    cycleSeed: Long,
    previousTailIds: List<String> = emptyList()
): List<ColorCardFilterFeedSource> {
    if (candidates.isEmpty() || demoAssets.isEmpty()) return emptyList()
    val orderingRandom = Random(cycleSeed)
    val assignmentRandom = Random(cycleSeed xor COLOR_CARD_FILTER_FEED_FIXED_ASSIGNMENT_SEED)
    val orderedCandidates = buildColorCardFilterFeedOrderedCandidates(candidates, orderingRandom)
    val adjustedCandidates = reorderColorCardFilterFeedBoundaryCandidates(
        candidates = orderedCandidates,
        previousTailIds = previousTailIds,
        tieBreakerSeed = cycleSeed
    )
    val demoAssetsByName = demoAssets.associateBy(ColorCardFilterFeedDemoAsset::assetName)
    val bootstrapDemoByPaletteId = bootstrapAssets
        .groupBy(ColorCardFilterFeedBootstrapPreviewAsset::paletteId)
        .mapValues { (_, assets) ->
            assets.firstNotNullOfOrNull { asset ->
                demoAssetsByName[asset.demoAssetName]
            }
        }
    val shuffledDemoAssets = demoAssets.shuffled(assignmentRandom)
    return adjustedCandidates.mapIndexedNotNull { index, candidate ->
        (bootstrapDemoByPaletteId[candidate.paletteId]
            ?: shuffledDemoAssets.getOrNull(index % shuffledDemoAssets.size))?.let { demoAsset ->
            ColorCardFilterFeedSource(
                candidate = candidate,
                demoAsset = demoAsset
            )
        }
    }
}

private fun buildColorCardFilterFeedOrderedCandidates(
    candidates: List<ColorCardTraceFilterCandidate>,
    random: Random
): List<ColorCardTraceFilterCandidate> {
    if (candidates.isEmpty()) return emptyList()
    val blackWhiteCandidates = candidates
        .filter(::isColorCardTraceBlackWhiteEraCandidate)
        .shuffled(random)
    val colorCandidates = candidates
        .filterNot(::isColorCardTraceBlackWhiteEraCandidate)
        .shuffled(random)
        .toMutableList()
    if (blackWhiteCandidates.isEmpty()) return colorCandidates
    if (colorCandidates.isEmpty()) return blackWhiteCandidates

    val ordered = mutableListOf<ColorCardTraceFilterCandidate>()
    var colorIndex = 0
    var bwIndex = 0
    while (colorIndex < colorCandidates.size || bwIndex < blackWhiteCandidates.size) {
        repeat(COLOR_CARD_FILTER_FEED_BLACK_WHITE_INTERVAL) {
            if (colorIndex < colorCandidates.size) {
                ordered += colorCandidates[colorIndex]
                colorIndex += 1
            }
        }
        if (bwIndex < blackWhiteCandidates.size) {
            ordered += blackWhiteCandidates[bwIndex]
            bwIndex += 1
        }
        if (colorIndex >= colorCandidates.size && bwIndex < blackWhiteCandidates.size) {
            while (bwIndex < blackWhiteCandidates.size) {
                ordered += blackWhiteCandidates[bwIndex]
                bwIndex += 1
            }
        }
    }
    return ordered
}

private fun reorderColorCardFilterFeedBoundaryCandidates(
    candidates: List<ColorCardTraceFilterCandidate>,
    previousTailIds: List<String>,
    tieBreakerSeed: Long
): List<ColorCardTraceFilterCandidate> {
    if (candidates.size <= 1 || previousTailIds.isEmpty()) return candidates
    val boundaryIds = previousTailIds.takeLast(COLOR_CARD_FILTER_FEED_BOUNDARY_AVOID_COUNT)
    val boundaryWindow = minOf(boundaryIds.size, candidates.size, COLOR_CARD_FILTER_FEED_BOUNDARY_AVOID_COUNT)
    if (boundaryWindow <= 0) return candidates

    var bestOffset = 0
    var bestScore = Int.MAX_VALUE
    val preferredOffset = (tieBreakerSeed and Int.MAX_VALUE.toLong()).toInt() % candidates.size

    for (offset in candidates.indices) {
        var score = 0
        for (index in 0 until boundaryWindow) {
            val candidateId = candidates[(offset + index) % candidates.size].paletteId
            val boundaryDistance = boundaryIds.indexOfLast { it == candidateId }
            if (boundaryDistance >= 0) {
                score += (boundaryWindow - index) * (boundaryIds.size - boundaryDistance)
            }
        }
        val tieBreaker = (offset - preferredOffset + candidates.size) % candidates.size
        val finalScore = (score * 1000) + tieBreaker
        if (finalScore < bestScore) {
            bestScore = finalScore
            bestOffset = offset
        }
    }

    if (bestOffset == 0) return candidates
    return List(candidates.size) { index ->
        candidates[(bestOffset + index) % candidates.size]
    }
}

private fun colorCardFilterFeedDescription(candidate: ColorCardTraceFilterCandidate): String? {
    return when (candidate.paletteId) {
        "builtin_hald:cinematic" -> "收住艳色，拉开明暗，画面更有电影氛围。"
        "builtin_hald:cinema3" -> "偏冷偏沉一点，夜景和街景会更有味道。"
        "builtin_hald:cinematicx-rec-709" -> "整体更干净均衡，适合自然的影视感表达。"
        "builtin_hald:bleach-bypass" -> "高反差、低饱和，带一点冷硬和克制感。"
        "builtin_hald:classic-memory-astia" -> "柔粉和暖光更明显，像把日常留成一张旧相纸。"
        "builtin_hald:classic-memory-bleach-bypass" -> "冷静、克制、略带褪色感，像老电影里的安静片段。"
        "builtin_hald:classic-memory-classic-chrome" -> "颜色压住一点，街头和纪实画面会更有复古故事感。"
        "builtin_hald:classic-memory-classic-neg" -> "旧街景的味道更浓，张扬被收住，只留下耐看的情绪。"
        "builtin_hald:classic-memory-eterna" -> "更轻、更柔，也更像一段被轻轻封存的回忆。"
        "builtin_hald:classic-memory-nostalgic-neg" -> "暖调里带一点旧时空气，日常画面会更有回忆滤镜感。"
        "builtin_hald:classic-memory-pro-neg-hi" -> "层次利落但不生硬，拍人像会有种旧照片的清爽感。"
        "builtin_hald:classic-memory-pro-neg-std" -> "自然松弛，带一点年代感，适合生活记录和轻肖像。"
        "builtin_hald:classic-memory-provia" -> "通透均衡里多了一层怀旧感，拿来拍日常很稳。"
        "builtin_hald:classic-memory-reala-ace" -> "清润、明净，又带一点老底片般的温柔光泽。"
        "builtin_hald:classic-memory-velvia" -> "鲜亮依旧，但更像旧旅行胶卷里被晒过的风景。"
        "builtin_hald:classic-neg" -> "有种复古街头照片的气质，耐看又不过分张扬。"
        "builtin_hald:eterna" -> "柔和、安静，适合人像和氛围场景。"
        "builtin_hald:nostalgic-neg" -> "暖一点，旧一点，像带着回忆感的日常片段。"
        "builtin_hald:pro-neg-hi" -> "层次更利落，肤色更清爽，拍人会很舒服。"
        "builtin_hald:pro-neg-std" -> "自然、柔和、耐看，适合轻肖像和生活记录。"
        "builtin_hald:provia" -> "颜色通透均衡，大多数场景都不容易出错。"
        "builtin_hald:reala-ace" -> "清透里带一点柔润，日常和风景都很合适。"
        "builtin_hald:fuj-07-velvia" -> "鲜艳、醒目，画面冲击力会更强。"
        "builtin_hald:film-sim-5219" -> "暗部氛围更浓，整体更有电影胶片感。"
        "builtin_hald:film-sim-c200" -> "明快轻松，很适合白天街拍和生活感画面。"
        "builtin_hald:film-sim-ektar100" -> "颜色更鲜明，风景和综合色彩场景会更出片。"
        "builtin_hald:film-sim-pro400h" -> "柔和通透，拍人像会显得轻盈干净。"
        "builtin_hald:film-sim-tx400" -> "黑白层次更直接，画面有种纪实摄影的味道。"
        "builtin_hald:polaroid-669" -> "带一点即时成像照片的复古气质。"
        "builtin_hald:polaroid-690" -> "偏暖偏柔，适合人像和生活片段。"
        "builtin_hald:polaroid-polachrome" -> "颜色更特别一些，画面会带一点实验感。"
        "builtin_hald:polaroid-px-680-warm" -> "怀旧感更明显，整体也更柔和。"
        else -> null
    }
}

private fun colorCardFilterFeedCategoryTag(
    appLanguage: AppLanguage,
    candidate: ColorCardTraceFilterCandidate
): String {
    val title = when (resolveColorCardFilterFeedSectionKey(candidate)) {
        "AI" -> when (appLanguage) {
            AppLanguage.SimplifiedChinese -> "AI调色"
            AppLanguage.TraditionalChinese -> "AI調色"
            else -> "AI"
        }
        "creative_visual" -> when (appLanguage) {
            AppLanguage.SimplifiedChinese -> "创意视觉"
            AppLanguage.TraditionalChinese -> "創意視覺"
            else -> "Creative"
        }
        "F" -> when (appLanguage) {
            AppLanguage.SimplifiedChinese -> "复古胶卷"
            AppLanguage.TraditionalChinese -> "復古膠卷"
            else -> "Retro Film"
        }
        "C" -> when (appLanguage) {
            AppLanguage.SimplifiedChinese -> "电影感"
            AppLanguage.TraditionalChinese -> "電影感"
            else -> "Cinema"
        }
        else -> when (appLanguage) {
            AppLanguage.SimplifiedChinese -> "风格滤镜"
            AppLanguage.TraditionalChinese -> "風格濾鏡"
            else -> "Style"
        }
    }
    return "#$title"
}

private fun colorCardFilterFeedHasLeadingEmoji(title: String): Boolean {
    val trimmed = title.trimStart()
    if (trimmed.isEmpty()) return false
    val firstCodePoint = trimmed.codePointAt(0)
    return Character.getType(firstCodePoint) == Character.OTHER_SYMBOL.toInt()
}

internal fun colorCardFilterFeedModernSeriesHeadlineWithEmoji(
    appLanguage: AppLanguage,
    sectionKey: String
): String {
    val rawHeadline = FilterFeedStrings.colorCardFilterFeedModernSeriesHeadline(
        appLanguage = appLanguage,
        sectionKey = sectionKey
    ).trim()
    if (rawHeadline.isEmpty()) return rawHeadline
    if (colorCardFilterFeedHasLeadingEmoji(rawHeadline)) return rawHeadline
    return "${colorCardFilterFeedModernSeriesEmoji(sectionKey)} $rawHeadline"
}

private fun colorCardFilterFeedModernSeriesEmoji(
    sectionKey: String
): String = when (sectionKey) {
    "cinema_clean", "cinema_drama" -> "\uD83C\uDFAC"
    "film_roll" -> "\uD83C\uDF9E\uFE0F"
    "fuji_color" -> "\uD83C\uDF3F"
    "memory" -> "\uD83D\uDD70\uFE0F"
    "nostalgia" -> "\uD83C\uDF06"
    "portrait" -> "\uD83C\uDF24\uFE0F"
    "polaroid" -> "\uD83D\uDCF8"
    "mono_mist", "mono_doc" -> "\u26AB"
    "accent" -> "\uD83C\uDFA8"
    "pixel_style" -> "\uD83C\uDFAE"
    "vivid_color" -> "\uD83C\uDF08"
    else -> "\u2728"
}

private fun resolveColorCardFilterFeedModernSeriesKey(
    candidate: ColorCardTraceFilterCandidate
): String {
    val paletteId = candidate.paletteId.removePrefix("builtin_hald:")
    return when {
        candidate.kind == ColorCardTraceFilterKind.BlackMist ||
            paletteId.startsWith("black-white-era-") -> "mono_mist"
        paletteId.contains("tx400") -> "mono_doc"
        paletteId.startsWith("polaroid-") -> "polaroid"
        paletteId.startsWith("classic-memory-") -> "memory"
        paletteId in setOf("classic-neg", "nostalgic-neg", "eterna") -> "nostalgia"
        paletteId.startsWith("cinematic") ||
            paletteId.startsWith("fg-cine") -> "cinema_clean"
        paletteId.startsWith("cinema") ||
            paletteId.contains("bleach") ||
            paletteId.contains("resolve") -> "cinema_drama"
        paletteId.startsWith("film-sim-") ||
            paletteId in setOf("film-boost-10", "film-tone-3", "print-film-2") -> "film_roll"
        paletteId in setOf("astia", "provia", "reala-ace", "velvia", "fuj-07-velvia", "fj") -> "fuji_color"
        paletteId.startsWith("pro-neg") ||
            paletteId.startsWith("j-fresh-bright-") -> "portrait"
        candidate.kind == ColorCardTraceFilterKind.SelectiveColor ||
            paletteId.startsWith("cc") ||
            paletteId == "converted-with-color" -> "accent"
        candidate.kind == ColorCardTraceFilterKind.PixelPoster -> "pixel_style"
        isColorCardTracePSeriesCandidate(candidate) ||
            paletteId in setOf("amy", "cms", "claire", "hklike", "moewarm", "moeot", "ogs") -> "vivid_color"
        else -> "daily"
    }
}

private fun colorCardFilterFeedModernSeriesChunkSize(
    seriesKey: String
): Int = when (seriesKey) {
    "pixel_style", "vivid_color", "accent" -> 5
    else -> 6
}

private fun resolveColorCardFilterFeedEffectiveSeriesKey(
    rawKey: String,
    rawCount: Int,
    rawCounts: Map<String, Int>
): String {
    if (rawCount >= 2) return rawKey
    val fallbackKeys = colorCardFilterFeedModernSeriesFallbackKeys(rawKey)
    return fallbackKeys.firstOrNull { fallbackKey ->
        fallbackKey == "daily" || (rawCounts[fallbackKey] ?: 0) >= 2
    } ?: "daily"
}

private fun colorCardFilterFeedModernSeriesFallbackKeys(
    rawKey: String
): List<String> = when (rawKey) {
    "cinema_clean" -> listOf("cinema_drama", "daily")
    "cinema_drama" -> listOf("cinema_clean", "daily")
    "film_roll" -> listOf("nostalgia", "daily")
    "fuji_color" -> listOf("portrait", "vivid_color", "daily")
    "memory" -> listOf("nostalgia", "polaroid", "daily")
    "nostalgia" -> listOf("memory", "film_roll", "daily")
    "portrait" -> listOf("fuji_color", "daily")
    "polaroid" -> listOf("memory", "nostalgia", "daily")
    "mono_mist" -> listOf("mono_doc", "daily")
    "mono_doc" -> listOf("mono_mist", "daily")
    "accent" -> listOf("vivid_color", "daily")
    "pixel_style" -> listOf("vivid_color", "daily")
    "vivid_color" -> listOf("accent", "daily")
    else -> listOf("daily")
}

private fun buildColorCardFilterFeedModernSeriesChunks(
    entryCount: Int,
    preferredChunkSize: Int
): List<Int> {
    if (entryCount <= 0) return emptyList()
    if (entryCount <= 8) return listOf(entryCount)

    val minChunkSize = 4
    val maxChunkSize = 8
    val targetChunkSize = preferredChunkSize.coerceIn(5, 7)
    val chunkCount = ((entryCount + targetChunkSize - 1) / targetChunkSize).coerceAtLeast(1)
    val baseSize = entryCount / chunkCount
    val remainder = entryCount % chunkCount
    val chunks = MutableList(chunkCount) { index ->
        baseSize + if (index < remainder) 1 else 0
    }

    for (index in chunks.indices.reversed()) {
        if (chunks[index] in minChunkSize..maxChunkSize) continue
        while (chunks[index] < minChunkSize) {
            val donorIndex = chunks.indexOfFirst { it > minChunkSize }
            if (donorIndex == -1) break
            chunks[donorIndex] -= 1
            chunks[index] += 1
        }
        while (chunks[index] > maxChunkSize) {
            val receiverIndex = chunks.indexOfFirst { it < maxChunkSize }
            if (receiverIndex == -1 || receiverIndex == index) break
            chunks[receiverIndex] += 1
            chunks[index] -= 1
        }
    }
    return chunks.filter { it > 0 }
}

private fun colorCardFilterFeedModernSeriesHeadlineKey(
    seriesKey: String,
    chunkIndex: Int,
    totalEntries: Int,
    chunkSize: Int
): String {
    val hasMultipleChunks = totalEntries > chunkSize
    val headlineOptions = when (seriesKey) {
        "cinema_clean" -> listOf(
            "\uD83C\uDFAC \u538b\u4f4f\u989c\u8272\u7684\u5e72\u51c0\u7535\u5f71\u611f",
            "\uD83C\uDFAC \u66f4\u51b7\u4e00\u70b9\u7684\u5f71\u50cf\u6c14\u8d28"
        )
        "cinema_drama" -> listOf(
            "\uD83C\uDFAC \u53cd\u5dee\u66f4\u91cd\u7684\u620f\u5267\u6c14\u6c1b",
            "\uD83C\uDFAC \u66f4\u6709\u60c5\u7eea\u5f20\u529b\u7684\u4e00\u7ec4"
        )
        "film_roll" -> listOf(
            "\uD83C\uDF9E\uFE0F \u80f6\u5377\u663e\u8272\u548c\u65c5\u884c\u8c03\u5b50",
            "\uD83C\uDF9E\uFE0F \u9897\u7c92\u611f\u66f4\u660e\u663e\u7684\u80f6\u5377\u5473"
        )
        "fuji_color" -> listOf(
            "\uD83C\uDF3F \u5bcc\u58eb\u90a3\u79cd\u900f\u4eae\u663e\u8272",
            "\uD83C\uDF3F \u66f4\u9c9c\u4eae\u66f4\u901a\u900f\u7684\u4e00\u7ec4"
        )
        "memory" -> listOf(
            "\uD83D\uDD70\uFE0F \u65e7\u76f8\u7eb8\u548c\u5c01\u5b58\u56de\u5fc6",
            "\uD83D\uDD70\uFE0F \u66f4\u50cf\u8001\u7167\u7247\u7684\u8f7b\u65e7\u611f"
        )
        "nostalgia" -> listOf(
            "\uD83C\uDF06 \u677e\u5f1b\u65e7\u611f\u548c\u65e5\u5e38\u60c5\u7eea",
            "\uD83C\uDF06 \u6696\u4e00\u70b9\u65e7\u4e00\u70b9\u7684\u6c1b\u56f4"
        )
        "portrait" -> listOf(
            "\uD83C\uDF24\uFE0F \u66f4\u6e05\u900f\u66f4\u67d4\u548c\u7684\u4e00\u7ec4",
            "\uD83C\uDF24\uFE0F \u8f7b\u4e00\u70b9\u4eae\u4e00\u70b9\u7684\u611f\u89c9"
        )
        "polaroid" -> listOf(
            "\uD83D\uDCF8 \u62cd\u7acb\u5f97\u90a3\u79cd\u5373\u65f6\u611f",
            "\uD83D\uDCF8 \u66f4\u6696\u66f4\u8f7b\u7684\u76f8\u7eb8\u5473\u9053"
        )
        "mono_mist" -> listOf(
            "\u26AB \u96fe\u9762\u9ed1\u767d\u548c\u8f7b\u9897\u7c92",
            "\u26AB \u66f4\u5b89\u9759\u7684\u7070\u5ea6\u6c14\u6c1b"
        )
        "mono_doc" -> listOf(
            "\u26AB \u7eaa\u5b9e\u9ed1\u767d\u548c\u76f4\u63a5\u5c42\u6b21",
            "\u26AB \u66f4\u786c\u6717\u4e00\u70b9\u7684\u9ed1\u767d\u8d28\u611f"
        )
        "accent" -> listOf(
            "\uD83C\uDFA8 \u504f\u8272\u70b9\u63d0\u4eae\u4e00\u70b9",
            "\uD83C\uDFA8 \u8ba9\u989c\u8272\u8df3\u51fa\u6765\u7684\u4e00\u7ec4"
        )
        "pixel_style" -> listOf(
            "\uD83D\uDDF8\uFE0F \u50cf\u7d20\u611f\u548c\u5757\u9762\u98ce\u683c",
            "\uD83D\uDDF8\uFE0F \u66f4\u56fe\u5f62\u5316\u7684\u753b\u9762\u8d28\u611f"
        )
        "vivid_color" -> listOf(
            "\uD83C\uDF08 \u66f4\u592e\u5f20\u66f4\u8df3\u7684\u8272\u5f69",
            "\uD83C\uDF08 \u7279\u522b\u4e00\u70b9\u7684\u989c\u8272\u73a9\u6cd5"
        )
        else -> listOf(
            "\uD83C\uDFA8 \u65e5\u5e38\u767e\u642d\u8c03\u8272",
            "\uD83C\uDFA8 \u7a33\u7a33\u5730\u63d0\u4e00\u4e0b\u753b\u9762"
        )
    }
    return if (hasMultipleChunks) {
        headlineOptions[chunkIndex % headlineOptions.size]
    } else {
        headlineOptions.first()
    }
}

private fun resolveColorCardFilterFeedSectionKey(
    candidate: ColorCardTraceFilterCandidate
): String {
    if (candidate.kind == ColorCardTraceFilterKind.AiMnn) return "AI"
    if (candidate.kind == ColorCardTraceFilterKind.MotionPhotoEntry) return "creative_visual"
    return when (candidate.title.trim().lastOrNull()?.uppercaseChar()) {
        'P', 'V' -> "creative_visual"
        'J', 'A' -> "A"
        'M', 'F' -> "F"
        'C' -> "C"
        'B' -> "B"
        else -> "A"
    }
}

internal fun colorCardFilterFeedAccentColor(): Color {
    return Color(0xFF9C6B44)
}
