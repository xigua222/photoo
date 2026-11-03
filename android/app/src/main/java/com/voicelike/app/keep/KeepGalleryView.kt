package com.voicelike.app.keep

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.BoundedKeySet
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.MediaItem
import com.voicelike.app.UnifiedSubpageTopBar
import com.voicelike.app.bottomNavigationBarPadding
import com.voicelike.app.bounceClick
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val KEEP_GALLERY_MIN_INITIAL_PAGE_SIZE = 240
private const val KEEP_GALLERY_MIN_PAGE_SIZE = 120
private const val KEEP_GALLERY_MIN_INITIAL_BUFFER_COUNT = 360
private const val KEEP_GALLERY_MIN_SCROLL_BUFFER_COUNT = 240
private const val KEEP_GALLERY_MIN_PREFETCH_THRESHOLD = 96
private const val KEEP_GALLERY_INITIAL_PAGE_ROWS = 80
private const val KEEP_GALLERY_PAGE_ROWS = 40
private const val KEEP_GALLERY_INITIAL_BUFFER_ROWS = 120
private const val KEEP_GALLERY_SCROLL_BUFFER_ROWS = 72
private const val KEEP_GALLERY_PREFETCH_ROWS = 20
private const val KEEP_GALLERY_DEFAULT_COLUMNS = 6
private const val KEEP_GALLERY_MIN_COLUMNS = 2
private const val KEEP_GALLERY_MAX_COLUMNS = 6
private const val KEEP_GALLERY_THUMB_PREFETCH_BASE_MEDIA_COUNT = 96
private const val KEEP_GALLERY_THUMB_PREFETCH_BASE_RANGE_EXTRA = 240
private const val KEEP_GALLERY_THUMB_PREFETCH_ROWS = 28
private const val KEEP_GALLERY_PREVIEW_PREFETCH_THRESHOLD = 18

private val KEEP_GALLERY_GRID_PADDING = 10.dp
private val KEEP_GALLERY_GRID_SPACING = 4.dp
private val KEEP_GALLERY_FAST_SCROLL_GUTTER = 0.dp

private fun keepGalleryInitialPageSize(columns: Int): Int {
    return maxOf(
        KEEP_GALLERY_MIN_INITIAL_PAGE_SIZE,
        columns.coerceAtLeast(KEEP_GALLERY_MIN_COLUMNS) * KEEP_GALLERY_INITIAL_PAGE_ROWS
    )
}

private fun keepGalleryPageSize(columns: Int): Int {
    return maxOf(
        KEEP_GALLERY_MIN_PAGE_SIZE,
        columns.coerceAtLeast(KEEP_GALLERY_MIN_COLUMNS) * KEEP_GALLERY_PAGE_ROWS
    )
}

private fun keepGalleryInitialBufferCount(columns: Int): Int {
    return maxOf(
        KEEP_GALLERY_MIN_INITIAL_BUFFER_COUNT,
        columns.coerceAtLeast(KEEP_GALLERY_MIN_COLUMNS) * KEEP_GALLERY_INITIAL_BUFFER_ROWS
    )
}

private fun keepGalleryScrollBufferCount(columns: Int): Int {
    return maxOf(
        KEEP_GALLERY_MIN_SCROLL_BUFFER_COUNT,
        columns.coerceAtLeast(KEEP_GALLERY_MIN_COLUMNS) * KEEP_GALLERY_SCROLL_BUFFER_ROWS
    )
}

private fun keepGalleryPrefetchThreshold(columns: Int): Int {
    return maxOf(
        KEEP_GALLERY_MIN_PREFETCH_THRESHOLD,
        columns.coerceAtLeast(KEEP_GALLERY_MIN_COLUMNS) * KEEP_GALLERY_PREFETCH_ROWS
    )
}

private fun keepGalleryThumbPrefetchMediaCount(columns: Int): Int {
    if (columns <= 2) return 12
    if (columns == 3) return 18
    if (columns == 4) return 28
    if (columns == 5) return 48
    return maxOf(
        KEEP_GALLERY_THUMB_PREFETCH_BASE_MEDIA_COUNT,
        columns.coerceAtLeast(KEEP_GALLERY_MIN_COLUMNS) * KEEP_GALLERY_THUMB_PREFETCH_ROWS
    )
}

private fun keepGalleryThumbPrefetchRangeExtra(columns: Int): Int {
    return maxOf(
        KEEP_GALLERY_THUMB_PREFETCH_BASE_RANGE_EXTRA,
        columns.coerceAtLeast(KEEP_GALLERY_MIN_COLUMNS) * KEEP_GALLERY_THUMB_PREFETCH_ROWS * 2
    )
}

private sealed interface KeepGalleryGridEntry {
    val key: String

    data class Header(
        val label: String
    ) : KeepGalleryGridEntry {
        override val key: String = "header:$label"
    }

    data class Media(
        val item: MediaItem
    ) : KeepGalleryGridEntry {
        override val key: String = "media:${item.id}"
    }
}

private data class KeepGalleryGridModel(
    val entries: List<KeepGalleryGridEntry>,
    val mediaIndexByEntryIndex: IntArray,
    val entryIndexByMediaIndex: IntArray
)

@Composable
fun KeepGalleryView(
    store: KeepGalleryStore,
    onBack: () -> Unit,
    onShare: (MediaItem) -> Unit,
    onLike: (MediaItem) -> Unit,
    onDeleteQueued: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storeMeta by store.metaState.collectAsState()
    val dataSource = remember(store, context) { KeepGalleryDataSource(context = context) }
    val prefetchedThumbKeys = remember { BoundedKeySet<String>(2048) }
    val gridState = rememberLazyGridState()
    val loadedItems = remember { mutableStateListOf<MediaItem>() }
    val cardBoundsById = remember { mutableStateMapOf<Long, Rect>() }

    var snapshotEntries by remember { mutableStateOf<List<KeepIndexEntry>>(emptyList()) }
    var nextOffset by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var loadingInitial by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var previewAnchorItemId by remember { mutableStateOf<Long?>(null) }
    var hiddenPreviewSourceId by remember { mutableStateOf<Long?>(null) }
    val persistedGridColumns = remember(context) {
        loadKeepGalleryGridColumns(
            context = context,
            defaultValue = KEEP_GALLERY_DEFAULT_COLUMNS,
            minValue = KEEP_GALLERY_MIN_COLUMNS,
            maxValue = KEEP_GALLERY_MAX_COLUMNS
        )
    }
    var gridColumns by rememberSaveable {
        mutableIntStateOf(
            persistedGridColumns.coerceIn(
                KEEP_GALLERY_MIN_COLUMNS,
                KEEP_GALLERY_MAX_COLUMNS
            )
        )
    }
    var isPinchGestureActive by remember { mutableStateOf(false) }
    val monthFormatter = remember { SimpleDateFormat("yyyy.MM", Locale.getDefault()) }
    val gridModel = remember(loadedItems.size) {
        buildKeepGalleryGridModel(loadedItems, monthFormatter)
    }
    val gridEntries = gridModel.entries
    val previewItems = remember(loadedItems.size) { loadedItems.toList() }
    val previewStartIndex = remember(previewAnchorItemId, previewItems) {
        previewItems.indexOfFirst { it.id == previewAnchorItemId }.coerceAtLeast(0)
    }
    val thumbnailSizeBucket = remember(gridColumns) {
        keepGalleryThumbSizeBucket(gridColumns)
    }
    val pageSize = remember(gridColumns) { keepGalleryPageSize(gridColumns) }
    val initialPageSize = remember(gridColumns) { keepGalleryInitialPageSize(gridColumns) }
    val initialBufferCount = remember(gridColumns) { keepGalleryInitialBufferCount(gridColumns) }
    val scrollBufferCount = remember(gridColumns) { keepGalleryScrollBufferCount(gridColumns) }
    val prefetchThreshold = remember(gridColumns) { keepGalleryPrefetchThreshold(gridColumns) }
    val thumbPrefetchMediaCount = remember(gridColumns) { keepGalleryThumbPrefetchMediaCount(gridColumns) }
    val thumbPrefetchRangeExtra = remember(gridColumns) { keepGalleryThumbPrefetchRangeExtra(gridColumns) }
    val displayedCount = remember(storeMeta.isLoaded, snapshotEntries.size, storeMeta.count) {
        if (storeMeta.isLoaded) snapshotEntries.size else storeMeta.count
    }
    val topVisibleMediaIndex by remember(gridState, gridModel.mediaIndexByEntryIndex, snapshotEntries.size) {
        derivedStateOf {
            resolveTopVisibleKeepMediaIndex(
                gridState = gridState,
                mediaIndexByEntryIndex = gridModel.mediaIndexByEntryIndex
            )
        }
    }
    val fastScrollProgress by remember(topVisibleMediaIndex, snapshotEntries.size) {
        derivedStateOf {
            if (snapshotEntries.size > 1 && topVisibleMediaIndex >= 0) {
                (topVisibleMediaIndex.toFloat() / snapshotEntries.lastIndex.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    suspend fun loadNextPage(limit: Int = pageSize): Boolean {
        if (loadingMore || !storeMeta.isLoaded || nextOffset >= snapshotEntries.size) return false
        loadingMore = true
        return try {
            val page = dataSource.loadPage(
                snapshot = snapshotEntries,
                offset = nextOffset,
                limit = limit
            )
            if (page.staleIds.isNotEmpty()) {
                store.removeIds(page.staleIds)
            }
            if (page.items.isNotEmpty()) {
                loadedItems.addAll(page.items)
            }
            nextOffset = page.nextOffset
            hasMore = page.hasMore
            loadingInitial = false
            page.items.isNotEmpty() || page.staleIds.isNotEmpty() || !page.hasMore
        } catch (error: Throwable) {
            Log.e("KeepGalleryView", "Failed to load keep page", error)
            loadingInitial = false
            hasMore = false
            false
        } finally {
            loadingMore = false
        }
    }

    suspend fun ensureBufferedMediaCount(targetCount: Int) {
        val safeTarget = targetCount.coerceIn(0, snapshotEntries.size)
        while (loadedItems.size < safeTarget && hasMore && !loadingMore) {
            val beforeOffset = nextOffset
            val remaining = (safeTarget - loadedItems.size).coerceAtLeast(0)
            val desiredLimit = maxOf(pageSize, remaining.coerceAtMost(pageSize * 3))
            val changed = loadNextPage(limit = desiredLimit)
            if (!changed && beforeOffset == nextOffset) break
        }
    }

    fun removeLocalKeepItem(item: MediaItem) {
        val loadedSnapshot = loadedItems.toList()
        val loadedIndex = loadedSnapshot.indexOfFirst { it.id == item.id }
        val nextAnchorId = loadedSnapshot.getOrNull(loadedIndex + 1)?.id
            ?: loadedSnapshot.getOrNull(loadedIndex - 1)?.id
        val snapshotIndex = snapshotEntries.indexOfFirst { it.id == item.id }
        if (snapshotIndex >= 0) {
            val updatedSnapshot = snapshotEntries.toMutableList()
            updatedSnapshot.removeAt(snapshotIndex)
            snapshotEntries = updatedSnapshot
            if (snapshotIndex < nextOffset) {
                nextOffset = (nextOffset - 1).coerceAtLeast(0)
            }
            hasMore = nextOffset < updatedSnapshot.size
        }
        if (loadedIndex >= 0) {
            loadedItems.removeAt(loadedIndex)
        }
        cardBoundsById.remove(item.id)
        if (previewAnchorItemId == item.id) {
            if (nextAnchorId != null) {
                previewAnchorItemId = nextAnchorId
            } else if (hasMore && !loadingMore) {
                scope.launch {
                    loadNextPage()
                    previewAnchorItemId = loadedItems.getOrNull(
                        loadedIndex.coerceAtMost(loadedItems.lastIndex)
                    )?.id
                }
            } else {
                previewAnchorItemId = null
            }
        }
    }

    LaunchedEffect(storeMeta.isLoaded, storeMeta.version, storeMeta.count, initialPageSize, initialBufferCount) {
        if (!storeMeta.isLoaded) return@LaunchedEffect
        if (snapshotEntries.size == storeMeta.count && (storeMeta.count == 0 || loadedItems.isNotEmpty())) {
            return@LaunchedEffect
        }
        loadingInitial = true
        val latestSnapshot = store.snapshotEntries()
        snapshotEntries = latestSnapshot
        loadedItems.clear()
        cardBoundsById.clear()
        prefetchedThumbKeys.clear()
        nextOffset = 0
        hasMore = latestSnapshot.isNotEmpty()
        if (latestSnapshot.isEmpty()) {
            loadingInitial = false
        } else {
            loadNextPage(limit = initialPageSize.coerceAtMost(latestSnapshot.size))
            ensureBufferedMediaCount(
                initialBufferCount.coerceAtMost(snapshotEntries.size)
            )
        }
    }

    LaunchedEffect(gridColumns, snapshotEntries.size, loadedItems.size, hasMore, initialBufferCount) {
        if (snapshotEntries.isEmpty() || loadedItems.isEmpty() || !hasMore) return@LaunchedEffect
        ensureBufferedMediaCount(initialBufferCount.coerceAtMost(snapshotEntries.size))
    }

    LaunchedEffect(gridColumns) {
        val clamped = gridColumns.coerceIn(KEEP_GALLERY_MIN_COLUMNS, KEEP_GALLERY_MAX_COLUMNS)
        if (clamped != gridColumns) {
            gridColumns = clamped
        } else {
            persistKeepGalleryGridColumns(
                context = context,
                value = clamped,
                minValue = KEEP_GALLERY_MIN_COLUMNS,
                maxValue = KEEP_GALLERY_MAX_COLUMNS
            )
        }
    }

    LaunchedEffect(loadedItems.size, thumbnailSizeBucket, thumbPrefetchMediaCount) {
        if (loadedItems.isEmpty()) return@LaunchedEffect
        prefetchKeepGalleryThumbs(
            context = context,
            items = loadedItems.take(thumbPrefetchMediaCount),
            sizeBucket = thumbnailSizeBucket,
            prefetchedKeys = prefetchedThumbKeys,
            maxCount = thumbPrefetchMediaCount
        )
    }

    LaunchedEffect(previewAnchorItemId, previewItems.size) {
        if (previewItems.isEmpty()) {
            previewAnchorItemId = null
        } else if (previewAnchorItemId != null && previewItems.none { it.id == previewAnchorItemId }) {
            previewAnchorItemId = previewItems.first().id
        }
    }

    LaunchedEffect(gridState, snapshotEntries.size, loadedItems.size, hasMore, scrollBufferCount, prefetchThreshold, pageSize, gridModel.mediaIndexByEntryIndex) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.collect { lastVisibleIndex ->
            if (!hasMore || lastVisibleIndex < 0 || gridEntries.isEmpty()) return@collect
            val visibleMediaIndex = gridModel.mediaIndexByEntryIndex
                .getOrNull(lastVisibleIndex.coerceIn(0, gridEntries.lastIndex))
                ?: -1
            if (visibleMediaIndex >= 0) {
                ensureBufferedMediaCount(
                    (visibleMediaIndex + 1 + scrollBufferCount).coerceAtMost(snapshotEntries.size)
                )
            }
            if (lastVisibleIndex >= gridEntries.lastIndex - prefetchThreshold) {
                loadNextPage(limit = pageSize)
            }
        }
    }

    LaunchedEffect(gridState, gridEntries.size, thumbnailSizeBucket, thumbPrefetchMediaCount, thumbPrefetchRangeExtra) {
        snapshotFlow {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: -1
            val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: -1
            firstVisibleIndex to lastVisibleIndex
        }.collect { (firstVisibleIndex, lastVisibleIndex) ->
            if (lastVisibleIndex < 0 || gridEntries.isEmpty()) return@collect
            val prefetchItems = collectKeepGalleryPrefetchItems(
                entries = gridEntries,
                firstVisibleIndex = firstVisibleIndex,
                lastVisibleIndex = lastVisibleIndex,
                maxMediaCount = thumbPrefetchMediaCount,
                rangeExtra = thumbPrefetchRangeExtra
            )
            prefetchKeepGalleryThumbs(
                context = context,
                items = prefetchItems,
                sizeBucket = thumbnailSizeBucket,
                prefetchedKeys = prefetchedThumbKeys,
                maxCount = thumbPrefetchMediaCount
            )
        }
    }

    LaunchedEffect(previewAnchorItemId, previewItems.size, hasMore, loadingMore) {
        val anchorId = previewAnchorItemId ?: return@LaunchedEffect
        val previewIndex = previewItems.indexOfFirst { it.id == anchorId }
        if (
            previewIndex >= 0 &&
            hasMore &&
            !loadingMore &&
            previewIndex >= previewItems.lastIndex - KEEP_GALLERY_PREVIEW_PREFETCH_THRESHOLD
        ) {
            loadNextPage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF4F4F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            UnifiedSubpageTopBar(
                title = strings.skip,
                onBack = onBack,
                trailing = {
                    Surface(
                        color = if (isDark) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color.Black.copy(alpha = 0.06f)
                        },
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = displayedCount.toString(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (!storeMeta.isLoaded || (loadingInitial && loadedItems.isEmpty() && storeMeta.count > 0)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (storeMeta.count == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "0",
                            fontSize = 56.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.skip,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                var localZoomAccumulator = 1f
                                var multiTouchDetected = false
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount > 1) {
                                        multiTouchDetected = true
                                        isPinchGestureActive = true
                                        val zoomChange = event.calculateZoom()
                                        if (zoomChange != 1f) {
                                            localZoomAccumulator *= zoomChange
                                            when {
                                                localZoomAccumulator <= 0.92f &&
                                                    gridColumns < KEEP_GALLERY_MAX_COLUMNS -> {
                                                    gridColumns += 1
                                                    localZoomAccumulator = 1f
                                                }
                                                localZoomAccumulator >= 1.08f &&
                                                    gridColumns > KEEP_GALLERY_MIN_COLUMNS -> {
                                                    gridColumns -= 1
                                                    localZoomAccumulator = 1f
                                                }
                                            }
                                        }
                                    }
                                    if (multiTouchDetected) {
                                        event.changes.forEach { change ->
                                            if (change.positionChange() != Offset.Zero) {
                                                change.consume()
                                            }
                                        }
                                    }
                                    if (event.changes.none { it.pressed }) {
                                        break
                                    }
                                }
                                isPinchGestureActive = false
                            }
                        }
                ) {
                    KeepGalleryGrid(
                        entries = gridEntries,
                        gridState = gridState,
                        gridColumns = gridColumns,
                        thumbnailSizeBucket = thumbnailSizeBucket,
                        userScrollEnabled = !isPinchGestureActive,
                        hiddenSourceItemId = hiddenPreviewSourceId,
                        onItemBoundsMeasured = { itemId, bounds ->
                            cardBoundsById[itemId] = bounds
                        },
                        onItemClick = { item, _ ->
                            previewAnchorItemId = item.id
                        }
                    )

                    if (loadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .bottomNavigationBarPadding(24.dp)
                                .size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }

                    KeepGalleryFastScroller(
                        progress = fastScrollProgress,
                        isScrollActive = gridState.isScrollInProgress || loadingMore,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        previewAnchorItemId?.let { anchorId ->
            if (previewItems.isNotEmpty()) {
                KeepGalleryPreviewOverlay(
                    items = previewItems,
                    startIndex = previewStartIndex,
                    sourceBoundsInRoot = cardBoundsById[anchorId],
                    sourceBoundsByItemId = cardBoundsById,
                    onActiveItemChanged = { activeItem ->
                        previewAnchorItemId = activeItem.id
                    },
                    onSharedDismissStart = { sourceItemId ->
                        hiddenPreviewSourceId = sourceItemId
                    },
                    onShare = onShare,
                    onTertiaryAction = { item ->
                        removeLocalKeepItem(item)
                        onLike(item)
                    },
                    tertiaryActionContentDescription = strings.likes,
                    tertiaryActionLabel = strings.likes,
                    onDelete = { item ->
                        removeLocalKeepItem(item)
                        onDeleteQueued(item)
                    },
                    onDismissRequest = {
                        if (hiddenPreviewSourceId != null) {
                            previewAnchorItemId = null
                            scope.launch {
                                withFrameNanos { }
                                if (previewAnchorItemId == null) {
                                    hiddenPreviewSourceId = null
                                }
                            }
                        } else {
                            previewAnchorItemId = null
                            hiddenPreviewSourceId = null
                        }
                    }
                )
            }
        }
    }
}

private fun resolveTopVisibleKeepMediaIndex(
    gridState: LazyGridState,
    mediaIndexByEntryIndex: IntArray
): Int {
    val visibleItems = gridState.layoutInfo.visibleItemsInfo
    visibleItems.forEach { info ->
        val mediaIndex = mediaIndexByEntryIndex.getOrNull(info.index) ?: -1
        if (mediaIndex >= 0) {
            return mediaIndex
        }
    }
    val firstVisibleIndex = gridState.firstVisibleItemIndex
    return mediaIndexByEntryIndex.getOrNull(firstVisibleIndex) ?: -1
}

@Composable
private fun KeepGalleryGrid(
    entries: List<KeepGalleryGridEntry>,
    gridState: LazyGridState,
    gridColumns: Int,
    thumbnailSizeBucket: Int,
    userScrollEnabled: Boolean,
    hiddenSourceItemId: Long?,
    onItemBoundsMeasured: (Long, Rect) -> Unit,
    onItemClick: (MediaItem, Rect) -> Unit
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        state = gridState,
        userScrollEnabled = userScrollEnabled,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(KEEP_GALLERY_GRID_SPACING),
        verticalArrangement = Arrangement.spacedBy(KEEP_GALLERY_GRID_SPACING),
        contentPadding = PaddingValues(
            start = KEEP_GALLERY_GRID_PADDING,
            end = KEEP_GALLERY_GRID_PADDING + KEEP_GALLERY_FAST_SCROLL_GUTTER,
            top = 4.dp,
            bottom = 24.dp
        )
    ) {
        items(
            items = entries,
            key = { it.key },
            span = { entry ->
                when (entry) {
                    is KeepGalleryGridEntry.Header -> GridItemSpan(maxLineSpan)
                    is KeepGalleryGridEntry.Media -> GridItemSpan(1)
                }
            }
        ) { entry ->
            when (entry) {
                is KeepGalleryGridEntry.Header -> {
                    Text(
                        text = entry.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp, start = 2.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                is KeepGalleryGridEntry.Media -> {
                    val item = entry.item
                    var cardBounds by remember(item.id) { mutableStateOf(Rect.Zero) }
                    val sourceAlpha = if (hiddenSourceItemId == item.id) 0f else 1f
                    val thumbnailBitmap by produceState<Bitmap?>(
                        initialValue = peekBestKeepGalleryThumbBitmap(
                            item = item,
                            preferredSizeBucket = thumbnailSizeBucket
                        ),
                        key1 = item.id,
                        key2 = thumbnailSizeBucket
                    ) {
                        value = loadKeepGalleryThumbBitmap(
                            context = context,
                            item = item,
                            sizeBucket = thumbnailSizeBucket
                        ) ?: value
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5.dp))
                            .onGloballyPositioned { coordinates ->
                                cardBounds = layoutBoundsInRootUnclipped(coordinates)
                                onItemBoundsMeasured(item.id, cardBounds)
                            }
                            .graphicsLayer {
                                alpha = sourceAlpha
                            }
                            .bounceClick(onClick = { onItemClick(item, cardBounds) })
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.08f))
                        ) {
                            thumbnailBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        if (item.type == "video") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(if (gridColumns >= 8) 16.dp else 18.dp)
                                    .background(Color.Black.copy(alpha = 0.44f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(if (gridColumns >= 8) 10.dp else 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildKeepGalleryGridModel(
    items: List<MediaItem>,
    monthFormatter: SimpleDateFormat
): KeepGalleryGridModel {
    if (items.isEmpty()) {
        return KeepGalleryGridModel(
            entries = emptyList(),
            mediaIndexByEntryIndex = IntArray(0),
            entryIndexByMediaIndex = IntArray(0)
        )
    }
    val entries = ArrayList<KeepGalleryGridEntry>(items.size + 16)
    val mediaIndexByEntryIndex = ArrayList<Int>(items.size + 16)
    val entryIndexByMediaIndex = IntArray(items.size)
    var lastMonthLabel: String? = null
    items.forEachIndexed { mediaIndex, item ->
        val monthLabel = monthFormatter.format(Date(item.dateAdded * 1000L))
        if (monthLabel != lastMonthLabel) {
            entries += KeepGalleryGridEntry.Header(monthLabel)
            mediaIndexByEntryIndex += -1
            lastMonthLabel = monthLabel
        }
        entryIndexByMediaIndex[mediaIndex] = entries.size
        entries += KeepGalleryGridEntry.Media(item)
        mediaIndexByEntryIndex += mediaIndex
    }
    return KeepGalleryGridModel(
        entries = entries,
        mediaIndexByEntryIndex = mediaIndexByEntryIndex.toIntArray(),
        entryIndexByMediaIndex = entryIndexByMediaIndex
    )
}

private fun gridEntryIndexForMediaIndex(
    items: List<MediaItem>,
    monthFormatter: SimpleDateFormat,
    mediaIndex: Int
): Int {
    if (items.isEmpty()) return 0
    val safeMediaIndex = mediaIndex.coerceIn(0, items.lastIndex)
    var entryIndex = -1
    var lastMonthLabel: String? = null
    for (index in 0..safeMediaIndex) {
        val monthLabel = monthFormatter.format(Date(items[index].dateAdded * 1000L))
        if (monthLabel != lastMonthLabel) {
            entryIndex += 1
            lastMonthLabel = monthLabel
        }
        entryIndex += 1
    }
    return entryIndex.coerceAtLeast(0)
}

private fun collectKeepGalleryPrefetchItems(
    entries: List<KeepGalleryGridEntry>,
    firstVisibleIndex: Int,
    lastVisibleIndex: Int,
    maxMediaCount: Int,
    rangeExtra: Int
): List<MediaItem> {
    if (entries.isEmpty() || lastVisibleIndex < 0 || maxMediaCount <= 0) return emptyList()
    val startIndex = (firstVisibleIndex - (rangeExtra / 4)).coerceAtLeast(0)
    val endIndex = (lastVisibleIndex + rangeExtra).coerceAtMost(entries.lastIndex)
    val items = ArrayList<MediaItem>(maxMediaCount)
    for (index in startIndex..endIndex) {
        val entry = entries[index]
        if (entry is KeepGalleryGridEntry.Media) {
            items += entry.item
            if (items.size >= maxMediaCount) break
        }
    }
    return items
}

private fun layoutBoundsInRootUnclipped(coordinates: LayoutCoordinates): Rect {
    val topLeft = coordinates.positionInRoot()
    val size = coordinates.size
    return Rect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height
    )
}
