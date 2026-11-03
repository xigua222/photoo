package com.voicelike.app

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.runtime.MutableIntState
import android.content.Context
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.lerp

enum class LikesDisplayMode {
    TikTok,
    Gallery
}

fun LikesDisplayMode.toPrefValue(): String = when (this) {
    LikesDisplayMode.TikTok -> "tiktok"
    LikesDisplayMode.Gallery -> "gallery"
}

fun likesDisplayModeFromPref(value: String): LikesDisplayMode = when (value.lowercase()) {
    "gallery" -> LikesDisplayMode.Gallery
    else -> LikesDisplayMode.TikTok
}

@Composable
fun LikesDisplayModeToggleButton(
    mode: LikesDisplayMode,
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    UnifiedSubpageTopBarIconButton(
        imageVector = if (mode == LikesDisplayMode.TikTok) {
            Icons.Outlined.Apps
        } else {
            Icons.Outlined.SmartDisplay
        },
        contentDescription = if (mode == LikesDisplayMode.TikTok) {
            "Open gallery"
        } else {
            "Open TikTok feed"
        },
        onClick = onToggle,
        modifier = modifier,
        containerColor = if (isDark) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        } else {
            Color.Black.copy(alpha = 0.2f)
        },
        iconTint = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White
    )
}

const val LIKES_GALLERY_MIN_COLUMNS = 2
const val LIKES_GALLERY_MAX_COLUMNS = 10
const val LIKES_GALLERY_DEFAULT_COLUMNS = 2
// The column cap follows the screen: each column keeps at least this width.
private val LIKES_GALLERY_MIN_COLUMN_WIDTH = 64.dp
private const val LIKES_GALLERY_COLUMNS_KEY = "likes_gallery_columns"

/**
 * One column count shared by every likes masonry (the Likes page and the Mine page feed), so a
 * change in either place shows up in the other at once. Persisted in the app prefs.
 */
private object LikesGalleryColumnsStore {
    private var columns: MutableIntState? = null

    fun state(context: Context): MutableIntState = columns ?: mutableIntStateOf(
        context.getSharedPreferences("photoo_prefs", Context.MODE_PRIVATE)
            .getInt(LIKES_GALLERY_COLUMNS_KEY, LIKES_GALLERY_DEFAULT_COLUMNS)
            .coerceIn(LIKES_GALLERY_MIN_COLUMNS, LIKES_GALLERY_MAX_COLUMNS)
    ).also { columns = it }

    fun set(context: Context, value: Int) {
        val clamped = value.coerceIn(LIKES_GALLERY_MIN_COLUMNS, LIKES_GALLERY_MAX_COLUMNS)
        state(context).intValue = clamped
        context.getSharedPreferences("photoo_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt(LIKES_GALLERY_COLUMNS_KEY, clamped)
            .apply()
    }
}

/** The shared column count, clamped to what the current window fits. */
@Composable
fun rememberLikesGalleryColumns(): Int {
    val context = LocalContext.current
    val stored = remember(context) { LikesGalleryColumnsStore.state(context.applicationContext) }
    return stored.intValue.coerceIn(LIKES_GALLERY_MIN_COLUMNS, rememberLikesGalleryMaxColumns())
}

fun setLikesGalleryColumns(context: Context, columns: Int) {
    LikesGalleryColumnsStore.set(context.applicationContext, columns)
}

@Composable
fun rememberLikesGalleryMaxColumns(): Int {
    val density = LocalDensity.current
    val windowWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    return (windowWidth / LIKES_GALLERY_MIN_COLUMN_WIDTH).toInt()
        .coerceIn(LIKES_GALLERY_MIN_COLUMNS, LIKES_GALLERY_MAX_COLUMNS)
}

internal fun likesGallerySpacing(columns: Int): Dp = when {
    columns <= 2 -> 8.dp
    columns <= 4 -> 4.dp
    else -> 2.dp
}

internal fun likesGalleryCornerRadius(columns: Int): Dp = when {
    columns <= 2 -> 18.dp
    columns == 3 -> 12.dp
    columns == 4 -> 8.dp
    else -> 4.dp
}

/**
 * Two-finger pinch on a likes masonry: pinch in for more columns, out for fewer. Watches ahead of
 * the grid so one finger still scrolls and taps; every further 20% of pinch steps one column.
 */
@Composable
fun Modifier.likesGalleryPinchToResize(): Modifier {
    val context = LocalContext.current
    val currentColumns by rememberUpdatedState(rememberLikesGalleryColumns())
    val currentMaxColumns by rememberUpdatedState(rememberLikesGalleryMaxColumns())
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var zoom = 1f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    event.changes.forEach { if (it.positionChange() != Offset.Zero) it.consume() }
                    zoom *= event.calculateZoom()
                    if (zoom < 0.8f || zoom > 1.25f) {
                        val next = (if (zoom < 0.8f) currentColumns + 1 else currentColumns - 1)
                            .coerceIn(LIKES_GALLERY_MIN_COLUMNS, currentMaxColumns)
                        zoom = 1f
                        if (next != currentColumns) setLikesGalleryColumns(context, next)
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * Picks the likes masonry column count from the counts this screen fits. [plain] draws a bare
 * grey icon for light section headers; otherwise a 40dp chrome circle showing the current count.
 */
@Composable
fun LikesGalleryColumnsButton(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    plain: Boolean = false
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val shownColumns = rememberLikesGalleryColumns()
    val maxColumns = rememberLikesGalleryMaxColumns()
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        if (plain) {
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = true }
                    .padding(start = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = likesGalleryColumnsLabel(shownColumns, language),
                    tint = Color(0xFFBFBFBF),
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f)
                    )
                    .clickable { expanded = true }
                    .semantics { contentDescription = likesGalleryColumnsLabel(shownColumns, language) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$shownColumns",
                    color = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        PhotooSelectMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            width = 156.dp
        ) {
            (LIKES_GALLERY_MIN_COLUMNS..maxColumns).forEach { count ->
                PhotooSelectMenuItem(
                    label = likesGalleryColumnsLabel(count, language),
                    selected = count == shownColumns,
                    onClick = {
                        expanded = false
                        if (count != shownColumns) setLikesGalleryColumns(context, count)
                    }
                )
            }
        }
    }
}

private fun likesGalleryColumnsLabel(count: Int, language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "$count 列"
    AppLanguage.TraditionalChinese -> "$count 欄"
    AppLanguage.Japanese -> "$count 列"
    AppLanguage.Korean -> "${count}열"
    AppLanguage.French -> "$count colonnes"
    AppLanguage.German -> "$count Spalten"
    AppLanguage.Italian -> "$count colonne"
    AppLanguage.Spanish -> "$count columnas"
    AppLanguage.Portuguese -> "$count colunas"
    AppLanguage.Russian -> "Столбцов: $count"
    AppLanguage.Thai -> "$count คอลัมน์"
    AppLanguage.Vietnamese -> "$count cột"
    AppLanguage.Indonesian -> "$count kolom"
    AppLanguage.Arabic -> "الأعمدة: $count"
    AppLanguage.Ukrainian -> "Стовпців: $count"
    AppLanguage.Polish -> "Kolumny: $count"
    AppLanguage.Czech -> "Sloupců: $count"
    AppLanguage.Dutch -> "Kolommen: $count"
    AppLanguage.Swedish -> "Kolumner: $count"
    AppLanguage.Danish -> "Kolonner: $count"
    AppLanguage.Norwegian -> "Kolonner: $count"
    AppLanguage.Finnish -> "Sarakkeita: $count"
    AppLanguage.Greek -> "Στήλες: $count"
    AppLanguage.Romanian -> "Coloane: $count"
    AppLanguage.Hungarian -> "Oszlopok: $count"
    AppLanguage.Turkish -> "Sütun: $count"
    AppLanguage.Malay -> "Lajur: $count"
    AppLanguage.Hebrew -> "עמודות: $count"
    AppLanguage.English -> "$count columns"
}

/** Masonry of the liked items at their real proportions, in the shared column count. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LikesGalleryContent(
    items: List<MediaItem>,
    contentPadding: PaddingValues,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    hiddenSourceItemId: Long? = null,
    onItemBoundsMeasured: (Long, Rect) -> Unit = { _, _ -> },
    onItemMediaSizeMeasured: (Long, IntSize) -> Unit = { _, _ -> },
    onItemClick: (MediaItem, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = rememberLikesGalleryColumns()
    val spacing = likesGallerySpacing(columns)
    val cornerRadius = likesGalleryCornerRadius(columns)
    val currentOnItemMediaSizeMeasured by rememberUpdatedState(onItemMediaSizeMeasured)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = state,
        modifier = modifier
            .fillMaxSize()
            .likesGalleryPinchToResize(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalItemSpacing = spacing,
        contentPadding = contentPadding
    ) {
        items(items, key = { it.id }) { item ->
            LikesGalleryItemCard(
                item = item,
                hidden = hiddenSourceItemId == item.id,
                cornerRadius = cornerRadius,
                onItemBoundsMeasured = onItemBoundsMeasured,
                onItemMediaSizeMeasured = currentOnItemMediaSizeMeasured,
                onClick = onItemClick,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
fun LikesGalleryItemCard(
    item: MediaItem,
    hidden: Boolean,
    cornerRadius: Dp = 18.dp,
    onItemBoundsMeasured: (Long, Rect) -> Unit = { _, _ -> },
    onItemMediaSizeMeasured: (Long, IntSize) -> Unit = { _, _ -> },
    onClick: (MediaItem, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnItemMediaSizeMeasured by rememberUpdatedState(onItemMediaSizeMeasured)
    val aspectRatio = item.galleryAspectRatio()
    var cardBounds by remember(item.id) { mutableStateOf(Rect.Zero) }
    val sourceAlpha = if (hidden) 0f else 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(cornerRadius))
            .onGloballyPositioned { coordinates ->
                cardBounds = layoutBoundsInRootUnclipped(coordinates)
                onItemBoundsMeasured(item.id, cardBounds)
            }
            .graphicsLayer {
                alpha = sourceAlpha
            }
            .clickable { onClick(item, cardBounds) }
    ) {
        val imageRequestBuilder = ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(false)
        if (item.type == "video") {
            imageRequestBuilder.decoderFactory(VideoFrameDecoder.Factory())
        }
        AsyncImage(
            model = imageRequestBuilder.build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onSuccess = { state ->
                val intrinsicWidth = state.result.drawable.intrinsicWidth
                val intrinsicHeight = state.result.drawable.intrinsicHeight
                if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                    // The decoded size honours EXIF rotation, which MediaStore dimensions don't.
                    HomeCardPhotoAspectCache.put(item.id, intrinsicWidth.toFloat() / intrinsicHeight.toFloat())
                    currentOnItemMediaSizeMeasured(item.id, IntSize(intrinsicWidth, intrinsicHeight))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.06f))
        )
        if (item.type == "video") {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircleOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
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

@Composable
fun LikesGalleryPreviewOverlay(
    visible: Boolean,
    items: List<MediaItem>,
    startIndex: Int,
    sourceBoundsInRoot: Rect? = null,
    sourceBoundsByItemId: Map<Long, Rect> = emptyMap(),
    mediaSizeByItemId: Map<Long, IntSize> = emptyMap(),
    dismissRequestToken: Long = 0L,
    sharedDismissEnabled: Boolean = true,
    danmakuEnabled: Boolean,
    commentsProvider: (Long) -> List<String>,
    onPageChanged: (Int) -> Unit,
    onSharedDismissStart: ((Long) -> Unit)? = null,
    onDismissRequest: () -> Unit,
    chromeContent: @Composable BoxScope.(MediaItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!visible || items.isEmpty()) return

    // Only the page in view plays video; neighbours keep their still frame.
    var activePage by remember(items) { mutableIntStateOf(startIndex) }
    SharedPagedFullScreenImageViewer(
        items = items,
        initialIndex = startIndex,
        onDismiss = onDismissRequest,
        sourceBoundsInRoot = sourceBoundsInRoot,
        sourceBoundsByItemId = sourceBoundsByItemId,
        dismissRequestToken = dismissRequestToken,
        sharedDismissEnabled = sharedDismissEnabled,
        onPageChanged = { page ->
            activePage = page
            onPageChanged(page)
        },
        onSharedDismissStart = onSharedDismissStart,
        orientation = SharedPagedViewerOrientation.Vertical,
        pageAlphaForTransition = { progress, hasSharedTransition, plainAlpha, isDismissing ->
            if (!hasSharedTransition || !isDismissing) {
                plainAlpha
            } else {
                0f
            }
        },
        transitionOverlay = { currentItem, sourceFrame, containerSize, openFraction, progress, hasSharedTransition, isDismissing ->
            if (
                hasSharedTransition &&
                isDismissing &&
                currentItem != null &&
                sourceFrame != null &&
                sourceFrame.width > 1f &&
                sourceFrame.height > 1f
            ) {
                LikesSharedMorphPhoto(
                    item = currentItem,
                    sourceFrame = sourceFrame,
                    containerSize = containerSize,
                    mediaSizeOverride = mediaSizeByItemId[currentItem.id],
                    openFraction = openFraction,
                    sourceCornerRadius = 18.dp,
                    targetCornerRadius = 0.dp,
                    alpha = 1f
                )
            }
        },
        chrome = { currentItem, _, requestDismiss ->
            currentItem?.let { chromeContent(it) }
            IconButton(
                onClick = requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 12.dp)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        modifier = modifier
    ) { page, item, requestDismiss, sharedTransitionProgress, hasSharedTransition, onZoomStateChanged ->
        LikesGalleryPreviewPage(
            item = item,
            danmakuEnabled = danmakuEnabled,
            commentsProvider = commentsProvider,
            onDismiss = requestDismiss,
            sharedTransitionProgress = sharedTransitionProgress,
            hasSharedTransition = hasSharedTransition,
            isActivePage = page == activePage,
            onZoomStateChanged = onZoomStateChanged
        )
    }
}

@Composable
private fun LegacyLikesGalleryPreviewOverlay(
    visible: Boolean,
    items: List<MediaItem>,
    startIndex: Int,
    sourceBoundsInRoot: Rect? = null,
    sourceBoundsByItemId: Map<Long, Rect> = emptyMap(),
    dismissRequestToken: Long = 0L,
    danmakuEnabled: Boolean,
    commentsProvider: (Long) -> List<String>,
    onPageChanged: (Int) -> Unit,
    onSharedDismissStart: ((Long) -> Unit)? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible || items.isEmpty()) return

    val scope = rememberCoroutineScope()
    val initialSourceItemId = remember(items, startIndex) {
        items.getOrNull(startIndex)?.id ?: items.firstOrNull()?.id
    }
    val initialSourceBounds = remember(sourceBoundsInRoot, sourceBoundsByItemId, initialSourceItemId) {
        initialSourceItemId?.let { sourceBoundsByItemId[it] } ?: sourceBoundsInRoot
    }
    val hasSharedTransition = remember(initialSourceBounds) {
        initialSourceBounds != null &&
            initialSourceBounds.width > 1f &&
            initialSourceBounds.height > 1f
    }
    val transitionKey = items.firstOrNull()?.id ?: -1L
    val enterProgress = remember(transitionKey, hasSharedTransition) {
        Animatable(if (hasSharedTransition) 1f else 0f)
    }
    val dismissProgress = remember(transitionKey) { Animatable(0f) }
    val plainDismissAlpha = remember(transitionKey) { Animatable(1f) }
    var isDismissing by remember(transitionKey) { mutableStateOf(false) }
    var dismissTargetBounds by remember(transitionKey) { mutableStateOf<Rect?>(null) }
    var latestContainerSize by remember(transitionKey) { mutableStateOf(IntSize.Zero) }
    var dismissContainerSize by remember(transitionKey) { mutableStateOf<IntSize?>(null) }
    var currentPageAllowsSwipe by remember(transitionKey) { mutableStateOf(true) }
    var consumedDismissRequestToken by remember(transitionKey) { mutableLongStateOf(0L) }
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, max(0, items.lastIndex)),
        pageCount = { items.size }
    )

    val requestDismiss: () -> Unit = {
        if (isDismissing) {
            Unit
        } else if (!hasSharedTransition) {
            isDismissing = true
            scope.launch {
                plainDismissAlpha.stop()
                plainDismissAlpha.snapTo(1f)
                plainDismissAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                )
                plainDismissAlpha.snapTo(0f)
                withFrameNanos { }
                onDismissRequest()
            }
        } else {
            isDismissing = true
            val currentPageItemId = items.getOrNull(pagerState.currentPage)?.id ?: items.firstOrNull()?.id
            val targetItemId = when {
                currentPageItemId != null && sourceBoundsByItemId[currentPageItemId] != null -> currentPageItemId
                initialSourceItemId != null && sourceBoundsByItemId[initialSourceItemId] != null -> initialSourceItemId
                else -> currentPageItemId ?: initialSourceItemId
            }
            val targetBounds = targetItemId?.let { sourceBoundsByItemId[it] } ?: initialSourceBounds
            dismissTargetBounds = targetBounds
            val capturedSize = latestContainerSize
            dismissContainerSize = if (capturedSize.width > 0 && capturedSize.height > 0) {
                capturedSize
            } else {
                null
            }
            if (targetItemId != null) {
                onSharedDismissStart?.invoke(targetItemId)
            }
            scope.launch {
                enterProgress.stop()
                dismissProgress.snapTo(0f)
                dismissProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
                dismissProgress.snapTo(1f)
                withFrameNanos { }
                onDismissRequest()
            }
        }
    }

    LaunchedEffect(visible, startIndex, items.size) {
        if (visible && items.isNotEmpty()) {
            pagerState.scrollToPage(startIndex.coerceIn(0, items.lastIndex))
        }
    }
    LaunchedEffect(pagerState.currentPage, items.size) {
        if (items.isNotEmpty()) {
            onPageChanged(pagerState.currentPage.coerceIn(0, items.lastIndex))
            currentPageAllowsSwipe = true
        }
    }
    LaunchedEffect(transitionKey, hasSharedTransition) {
        dismissTargetBounds = null
        dismissContainerSize = null
        dismissProgress.snapTo(0f)
        isDismissing = false
        consumedDismissRequestToken = 0L
        if (!hasSharedTransition) {
            plainDismissAlpha.snapTo(0f)
            plainDismissAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            enterProgress.snapTo(0f)
            return@LaunchedEffect
        }
        plainDismissAlpha.snapTo(1f)
        if (enterProgress.value <= 0f) {
            enterProgress.snapTo(1f)
        } else {
            enterProgress.stop()
        }
        enterProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(dismissRequestToken, visible, isDismissing) {
        if (
            visible &&
            dismissRequestToken != 0L &&
            dismissRequestToken != consumedDismissRequestToken &&
            !isDismissing
        ) {
            consumedDismissRequestToken = dismissRequestToken
            requestDismiss()
        }
    }

    BackHandler(onBack = requestDismiss)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val measuredContainerSize = IntSize(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        )
        if (
            measuredContainerSize.width > 0 &&
            measuredContainerSize.height > 0 &&
            latestContainerSize != measuredContainerSize
        ) {
            latestContainerSize = measuredContainerSize
        }
        val effectiveContainerSize = if (isDismissing) {
            dismissContainerSize ?: measuredContainerSize
        } else {
            measuredContainerSize
        }
        val containerWidth = effectiveContainerSize.width.toFloat().coerceAtLeast(1f)
        val containerHeight = effectiveContainerSize.height.toFloat().coerceAtLeast(1f)
        val dismissTarget = when {
            isDismissing -> dismissTargetBounds ?: initialSourceBounds
            else -> initialSourceBounds
        }
        val targetScaleX = if (dismissTarget != null && hasSharedTransition) {
            (dismissTarget.width / containerWidth).coerceIn(0.03f, 1f)
        } else {
            1f
        }
        val targetScaleY = if (dismissTarget != null && hasSharedTransition) {
            (dismissTarget.height / containerHeight).coerceIn(0.03f, 1f)
        } else {
            1f
        }
        val targetTranslationX = if (dismissTarget != null && hasSharedTransition) {
            dismissTarget.center.x - (containerWidth / 2f)
        } else {
            0f
        }
        val targetTranslationY = if (dismissTarget != null && hasSharedTransition) {
            dismissTarget.center.y - (containerHeight / 2f)
        } else {
            0f
        }
        val progress = when {
            !hasSharedTransition -> 0f
            isDismissing -> dismissProgress.value
            else -> enterProgress.value
        }
        val animatedScaleX = likesLerpFloat(1f, targetScaleX, progress)
        val animatedScaleY = likesLerpFloat(1f, targetScaleY, progress)
        val animatedTranslationX = likesLerpFloat(0f, targetTranslationX, progress)
        val animatedTranslationY = likesLerpFloat(0f, targetTranslationY, progress)
        val cornerProgress = sqrt((progress / 0.72f).coerceIn(0f, 1f))
        val animatedCorner = lerp(0.dp, 18.dp, cornerProgress)
        val sharedScrimAlpha = likesLerpFloat(1f, 0f, progress)
        val plainAlpha = if (hasSharedTransition) 1f else plainDismissAlpha.value
        val plainScale = if (hasSharedTransition) 1f else likesLerpFloat(0.965f, 1f, plainDismissAlpha.value)
        val scrimAlpha = sharedScrimAlpha * plainAlpha
        val contentUniformScale = max(animatedScaleX, animatedScaleY)
        val contentCompensationScaleX = if (animatedScaleX > 0f) {
            contentUniformScale / animatedScaleX
        } else {
            1f
        }
        val contentCompensationScaleY = if (animatedScaleY > 0f) {
            contentUniformScale / animatedScaleY
        } else {
            1f
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = animatedScaleX * plainScale
                    scaleY = animatedScaleY * plainScale
                    translationX = animatedTranslationX
                    translationY = animatedTranslationY
                    alpha = plainAlpha
                    clip = hasSharedTransition
                    shape = RoundedCornerShape(animatedCorner)
                }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = contentCompensationScaleX
                        scaleY = contentCompensationScaleY
                    }
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = currentPageAllowsSwipe && !isDismissing,
                    key = { index ->
                        items.getOrNull(index)?.id ?: index.toLong()
                    }
                ) { page ->
                    val item = items.getOrNull(page) ?: return@VerticalPager
                    LikesGalleryPreviewPage(
                        item = item,
                        danmakuEnabled = danmakuEnabled,
                        commentsProvider = commentsProvider,
                        onDismiss = requestDismiss,
                        sharedTransitionProgress = progress,
                        hasSharedTransition = hasSharedTransition,
                        onZoomStateChanged = { pageAllowsSwipe ->
                            if (page == pagerState.currentPage) {
                                currentPageAllowsSwipe = pageAllowsSwipe
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LikesGalleryPreviewPage(
    item: MediaItem,
    danmakuEnabled: Boolean,
    commentsProvider: (Long) -> List<String>,
    onDismiss: () -> Unit,
    sharedTransitionProgress: Float = 0f,
    hasSharedTransition: Boolean = false,
    isActivePage: Boolean = false,
    onZoomStateChanged: (Boolean) -> Unit
) {
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var zoomAnimationJob by remember(item.id) { mutableStateOf<Job?>(null) }
    val stopZoomAnimation: () -> Unit = {
        zoomAnimationJob?.cancel()
        zoomAnimationJob = null
    }
    val animateZoomTo: (Float, Offset) -> Unit = { targetScale, targetOffset ->
        stopZoomAnimation()
        zoomAnimationJob = scope.launch {
            val startScale = scale
            val startOffset = offset
            val progress = Animatable(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) {
                scale = likesLerpFloat(startScale, targetScale, value)
                offset = Offset(
                    x = likesLerpFloat(startOffset.x, targetOffset.x, value),
                    y = likesLerpFloat(startOffset.y, targetOffset.y, value)
                )
                onZoomStateChanged(scale <= 1.01f)
            }
            scale = targetScale
            offset = targetOffset
            onZoomStateChanged(scale <= 1.01f)
            zoomAnimationJob = null
        }
    }

    LaunchedEffect(item.id) {
        stopZoomAnimation()
        scale = 1f
        offset = Offset.Zero
        onZoomStateChanged(true)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val constraints = constraints
        val containerSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(item.id, containerSize) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val mediaRect = galleryFittedContentRect(
                                containerSize = containerSize,
                                mediaWidth = item.width,
                                mediaHeight = item.height
                            )
                            if (!mediaRect.contains(tapOffset)) return@detectTapGestures
                            if (scale > 1.05f) {
                                animateZoomTo(1f, Offset.Zero)
                            } else {
                                val targetScale = 2.5f
                                val center = Offset(
                                    x = constraints.maxWidth / 2f,
                                    y = constraints.maxHeight / 2f
                                )
                                val rawTargetOffset = Offset(
                                    x = offset.x + (scale - targetScale) * (tapOffset.x - center.x),
                                    y = offset.y + (scale - targetScale) * (tapOffset.y - center.y)
                                )
                                val targetOffset = likesClampPanOffset(
                                    offset = rawTargetOffset,
                                    scale = targetScale,
                                    containerWidth = constraints.maxWidth,
                                    containerHeight = constraints.maxHeight
                                )
                                animateZoomTo(targetScale, targetOffset)
                            }
                        },
                        onTap = { tapOffset ->
                            val mediaRect = galleryFittedContentRect(
                                containerSize = containerSize,
                                mediaWidth = item.width,
                                mediaHeight = item.height
                            )
                            if (scale <= 1.05f && !mediaRect.contains(tapOffset)) {
                                onDismiss()
                            }
                        }
                    )
                }
                .pointerInput(item.id, isActivePage) {
                    // The player covers a playing video page; zooming the poster under it would only
                    // lock paging with nothing visible changing.
                    if (item.type == "video" && isActivePage) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        stopZoomAnimation()
                        var parentSwipeLocked = false
                        do {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount > 1) {
                                if (!parentSwipeLocked) {
                                    onZoomStateChanged(false)
                                    parentSwipeLocked = true
                                }
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                val maxX = ((constraints.maxWidth * newScale) - constraints.maxWidth) / 2f
                                val maxY = ((constraints.maxHeight * newScale) - constraints.maxHeight) / 2f
                                scale = newScale
                                if (newScale > 1f) {
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                                event.changes.forEach { change ->
                                    if (change.positionChange() != Offset.Zero) {
                                        change.consumeAllChanges()
                                    }
                                }
                            } else if (pressedCount == 1 && scale > 1f) {
                                val change = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                val pan = change.positionChange()
                                if (pan != Offset.Zero) {
                                    val maxX = ((constraints.maxWidth * scale) - constraints.maxWidth) / 2f
                                    val maxY = ((constraints.maxHeight * scale) - constraints.maxHeight) / 2f
                                    val next = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                    val movedX = abs(next.x - offset.x)
                                    val movedY = abs(next.y - offset.y)
                                    offset = next
                                    if (movedX > 0.5f || movedY > 0.5f) {
                                        change.consumeAllChanges()
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        if (scale <= 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        }
                        if (parentSwipeLocked) {
                            onZoomStateChanged(scale <= 1.01f)
                        } else if (scale <= 1.01f) {
                            onZoomStateChanged(true)
                        }
                    }
                }
        ) {
            val chromeVisible = !hasSharedTransition || sharedTransitionProgress <= 0.03f
            val imageRequest = remember(item.id, item.uri, containerSize) {
                buildFullscreenPreviewImageRequest(
                    context = context,
                    item = item,
                    containerSize = containerSize
                )
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
            // The still frame stays underneath as the poster; the player takes over once the page settles.
            if (item.type == "video" && isActivePage && chromeVisible) {
                FullScreenVideoPlayer(
                    item = item,
                    isMuted = false,
                    onMuteChange = {},
                    onDismiss = {},
                    dismissEnabled = false,
                    enableOpenCloseTransition = false,
                    showCloseButton = false,
                    showTopBar = false,
                    showMuteButton = false,
                    showCenterPlayPause = false,
                    tapTogglesPlayPause = true,
                    onTapOutsideVideo = onDismiss,
                    controlsBottomPadding = 72.dp
                )
            }
            if (danmakuEnabled && chromeVisible) {
                DanmakuOverlay(
                    comments = commentsProvider(item.id)
                )
            }
            if (chromeVisible) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                ) {
                    if (item.location != "Unknown Location") {
                        Text(
                            text = item.location,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(item.dateAdded * 1000))
                    Text(
                        text = dateText,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// Real proportions, only reined in for long screenshots and panoramas so one tile can't take over.
private fun MediaItem.galleryAspectRatio(): Float =
    (resolveHomeCardPhotoAspectRatio(this) ?: 1f).coerceIn(0.45f, 2.2f)

@Composable
private fun LikesSharedMorphPhoto(
    item: MediaItem,
    sourceFrame: Rect,
    containerSize: IntSize,
    mediaSizeOverride: IntSize?,
    openFraction: Float,
    sourceCornerRadius: Dp,
    targetCornerRadius: Dp,
    alpha: Float
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val context = LocalContext.current
    val imageRequest = remember(item.id, item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(false)
            .build()
    }
    val resolvedMediaWidth = mediaSizeOverride
        ?.width
        ?.takeIf { it > 0 }
        ?: item.width
    val resolvedMediaHeight = mediaSizeOverride
        ?.height
        ?.takeIf { it > 0 }
        ?: item.height
    val targetFrame = remember(containerSize, item.id, resolvedMediaWidth, resolvedMediaHeight) {
        galleryFittedContentRect(
            containerSize = containerSize,
            mediaWidth = resolvedMediaWidth,
            mediaHeight = resolvedMediaHeight
        )
    }
    val mediaWidthPx = resolvedMediaWidth.coerceAtLeast(1).toFloat()
    val mediaHeightPx = resolvedMediaHeight.coerceAtLeast(1).toFloat()
    val moveFraction = FastOutSlowInEasing.transform(openFraction.coerceIn(0f, 1f))
    val fitFraction = FastOutSlowInEasing.transform(
        ((openFraction - 0.18f) / 0.82f).coerceIn(0f, 1f)
    )
    val sourceScale = max(
        sourceFrame.width / mediaWidthPx,
        sourceFrame.height / mediaHeightPx
    )
    val targetScale = min(
        targetFrame.width / mediaWidthPx,
        targetFrame.height / mediaHeightPx
    )
    val currentFrame = likesLerpRect(sourceFrame, targetFrame, moveFraction)
    val interpolatedScale = likesLerpFloat(sourceScale, targetScale, fitFraction)
    val coverScaleForCurrentFrame = max(
        currentFrame.width / mediaWidthPx,
        currentFrame.height / mediaHeightPx
    )
    val currentScale = max(interpolatedScale, coverScaleForCurrentFrame)
    val currentCenter = Offset(
        x = likesLerpFloat(sourceFrame.center.x, targetFrame.center.x, moveFraction),
        y = likesLerpFloat(sourceFrame.center.y, targetFrame.center.y, moveFraction)
    )
    val drawWidth = mediaWidthPx * currentScale
    val drawHeight = mediaHeightPx * currentScale
    val currentCornerRadius = lerp(sourceCornerRadius, targetCornerRadius, moveFraction)
    val frameWidthDp = with(density) { currentFrame.width.toDp() }
    val frameHeightDp = with(density) { currentFrame.height.toDp() }
    val drawWidthDp = with(density) { drawWidth.toDp() }
    val drawHeightDp = with(density) { drawHeight.toDp() }
    val drawLeftInFrame = currentCenter.x - (drawWidth / 2f) - currentFrame.left
    val drawTopInFrame = currentCenter.y - (drawHeight / 2f) - currentFrame.top

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = alpha
                }
                .offset {
                    IntOffset(
                        x = currentFrame.left.roundToInt(),
                        y = currentFrame.top.roundToInt()
                    )
                }
                .size(
                    width = frameWidthDp,
                    height = frameHeightDp
                )
                .clip(RoundedCornerShape(currentCornerRadius))
                .background(Color.Black.copy(alpha = 0.06f))
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = drawLeftInFrame.roundToInt(),
                            y = drawTopInFrame.roundToInt()
                        )
                    }
                    .size(
                        width = drawWidthDp,
                        height = drawHeightDp
                    )
            )
        }
    }
}

private fun likesLerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + ((stop - start) * fraction)
}

private fun likesLerpRect(start: Rect, stop: Rect, fraction: Float): Rect {
    return Rect(
        left = likesLerpFloat(start.left, stop.left, fraction),
        top = likesLerpFloat(start.top, stop.top, fraction),
        right = likesLerpFloat(start.right, stop.right, fraction),
        bottom = likesLerpFloat(start.bottom, stop.bottom, fraction)
    )
}

private fun likesClampPanOffset(
    offset: Offset,
    scale: Float,
    containerWidth: Int,
    containerHeight: Int
): Offset {
    if (scale <= 1f) return Offset.Zero
    val maxX = ((containerWidth * scale) - containerWidth) / 2f
    val maxY = ((containerHeight * scale) - containerHeight) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

private fun galleryFittedContentRect(
    containerSize: IntSize,
    mediaWidth: Int,
    mediaHeight: Int
): Rect {
    val cw = containerSize.width.toFloat().coerceAtLeast(1f)
    val ch = containerSize.height.toFloat().coerceAtLeast(1f)
    val safeW = mediaWidth.coerceAtLeast(1).toFloat()
    val safeH = mediaHeight.coerceAtLeast(1).toFloat()
    val mediaAspect = safeW / safeH
    val containerAspect = cw / ch
    val drawW: Float
    val drawH: Float
    if (mediaAspect > containerAspect) {
        drawW = cw
        drawH = cw / mediaAspect
    } else {
        drawH = ch
        drawW = ch * mediaAspect
    }
    val left = (cw - drawW) / 2f
    val top = (ch - drawH) / 2f
    return Rect(left, top, left + drawW, top + drawH)
}
