package com.voicelike.app

import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.lerp
import androidx.media3.ui.AspectRatioFrameLayout

enum class SharedPagedViewerOrientation {
    Horizontal,
    Vertical
}

private val FULL_SCREEN_CHROME_EDGE_PADDING = 32.dp
private val FULL_SCREEN_BADGE_TOP_PADDING = 46.dp
private const val FULL_SCREEN_LIVE_TRANSITION_SETTLE_PROGRESS = 0.02f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedPagedFullScreenImageViewer(
    items: List<MediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    enableSwipe: Boolean = true,
    sourceBoundsInRoot: Rect? = null,
    sourceBoundsByItemId: Map<Long, Rect> = emptyMap(),
    dismissSourceBoundsByItemId: Map<Long, Rect> = sourceBoundsByItemId,
    dismissRequestToken: Long = 0L,
    sharedDismissEnabled: Boolean = true,
    onPageChanged: (Int) -> Unit = {},
    onSharedDismissStart: ((Long) -> Unit)? = null,
    orientation: SharedPagedViewerOrientation = SharedPagedViewerOrientation.Horizontal,
    sharedTargetCornerRadius: Dp = 24.dp,
    sharedScaleClampMin: Float = 0.12f,
    modifier: Modifier = Modifier,
    chrome: @Composable BoxScope.(currentItem: MediaItem?, pagerState: PagerState, requestDismiss: () -> Unit) -> Unit = { _, _, _ -> },
    pageAlphaForTransition: (
        progress: Float,
        hasSharedTransition: Boolean,
        plainAlpha: Float,
        isDismissing: Boolean
    ) -> Float = { _, _, plainAlpha, _ -> plainAlpha },
    transitionOverlay: @Composable BoxScope.(
        currentItem: MediaItem?,
        sourceFrame: Rect?,
        containerSize: IntSize,
        openFraction: Float,
        progress: Float,
        hasSharedTransition: Boolean,
        isDismissing: Boolean
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    pageContent: @Composable (
        page: Int,
        item: MediaItem,
        requestDismiss: () -> Unit,
        sharedTransitionProgress: Float,
        hasSharedTransition: Boolean,
        onZoomStateChanged: (Boolean) -> Unit
    ) -> Unit
) {
    if (items.isEmpty()) return

    val scope = rememberCoroutineScope()
    val initialSourceItemId = remember(items, initialIndex) {
        items.getOrNull(initialIndex)?.id ?: items.firstOrNull()?.id
    }
    val initialSourceBounds = remember(sourceBoundsInRoot, sourceBoundsByItemId, initialSourceItemId) {
        initialSourceItemId?.let { sourceBoundsByItemId[it] } ?: sourceBoundsInRoot
    }
    val hasSharedBoundsTarget = remember(initialSourceBounds) {
        initialSourceBounds != null &&
            initialSourceBounds.width > 1f &&
            initialSourceBounds.height > 1f
    }
    val hasSharedTransition = hasSharedBoundsTarget
    val hasSharedDismissTransition = hasSharedTransition && sharedDismissEnabled
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
        initialPage = initialIndex.coerceIn(0, max(0, items.lastIndex)),
        pageCount = { items.size }
    )
    val horizontalPreviewFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapAnimationSpec = tween(
            durationMillis = 420,
            easing = FastOutSlowInEasing
        ),
        snapPositionalThreshold = 0.38f
    )

    val requestDismiss: () -> Unit = {
        if (isDismissing) {
            Unit
        } else if (!hasSharedDismissTransition) {
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
                onDismiss()
            }
        } else {
            isDismissing = true
            val currentPageItemId = items.getOrNull(pagerState.currentPage)?.id ?: items.firstOrNull()?.id
            val targetItemId = when {
                currentPageItemId != null &&
                    (
                        dismissSourceBoundsByItemId[currentPageItemId] != null ||
                            sourceBoundsByItemId[currentPageItemId] != null
                        ) -> currentPageItemId
                initialSourceItemId != null &&
                    (
                        dismissSourceBoundsByItemId[initialSourceItemId] != null ||
                            sourceBoundsByItemId[initialSourceItemId] != null
                        ) -> initialSourceItemId
                else -> currentPageItemId ?: initialSourceItemId
            }
            dismissTargetBounds = targetItemId?.let { itemId ->
                dismissSourceBoundsByItemId[itemId] ?: sourceBoundsByItemId[itemId]
            } ?: sourceBoundsInRoot ?: initialSourceBounds
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
                onDismiss()
            }
        }
    }

    LaunchedEffect(initialIndex, items.size) {
        if (items.isNotEmpty()) {
            pagerState.scrollToPage(initialIndex.coerceIn(0, items.lastIndex))
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
    LaunchedEffect(dismissRequestToken, isDismissing, items.size) {
        if (
            dismissRequestToken != 0L &&
            dismissRequestToken != consumedDismissRequestToken &&
            !isDismissing &&
            items.isNotEmpty()
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
        if (measuredContainerSize.width > 0 &&
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
        val activeSharedTransition = if (isDismissing) {
            hasSharedDismissTransition
        } else {
            hasSharedTransition
        }
        val dismissTarget = when {
            isDismissing -> dismissTargetBounds ?: sourceBoundsInRoot ?: initialSourceBounds
            else -> initialSourceBounds ?: sourceBoundsInRoot
        }
        val minSharedScale = sharedScaleClampMin.coerceIn(0.01f, 1f)
        val targetScaleX = if (dismissTarget != null && activeSharedTransition) {
            (dismissTarget.width / containerWidth).coerceIn(minSharedScale, 1f)
        } else {
            1f
        }
        val targetScaleY = if (dismissTarget != null && activeSharedTransition) {
            (dismissTarget.height / containerHeight).coerceIn(minSharedScale, 1f)
        } else {
            1f
        }
        val targetTranslationX = if (dismissTarget != null && activeSharedTransition) {
            dismissTarget.center.x - (containerWidth / 2f)
        } else {
            0f
        }
        val targetTranslationY = if (dismissTarget != null && activeSharedTransition) {
            dismissTarget.center.y - (containerHeight / 2f)
        } else {
            0f
        }
        val progress = when {
            !activeSharedTransition -> 0f
            isDismissing -> dismissProgress.value
            else -> enterProgress.value
        }
        val animatedScaleX = lerpFloat(1f, targetScaleX, progress)
        val animatedScaleY = lerpFloat(1f, targetScaleY, progress)
        val animatedTranslationX = lerpFloat(0f, targetTranslationX, progress)
        val animatedTranslationY = lerpFloat(0f, targetTranslationY, progress)
        val cornerProgress = sqrt((progress / 0.72f).coerceIn(0f, 1f))
        val animatedCorner = lerp(0.dp, sharedTargetCornerRadius, cornerProgress)
        val sharedScrimAlpha = lerpFloat(1f, 0f, progress)
        val plainAlpha = if (activeSharedTransition) 1f else plainDismissAlpha.value
        val pageAlpha = pageAlphaForTransition(progress, activeSharedTransition, plainAlpha, isDismissing)
        val plainScale = if (activeSharedTransition) 1f else lerpFloat(0.965f, 1f, plainDismissAlpha.value)
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
                    alpha = pageAlpha
                    clip = activeSharedTransition
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
                when (orientation) {
                    SharedPagedViewerOrientation.Horizontal -> {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = enableSwipe && currentPageAllowsSwipe,
                            flingBehavior = horizontalPreviewFlingBehavior,
                            key = { index -> items.getOrNull(index)?.id ?: index }
                        ) { page ->
                            items.getOrNull(page)?.let { item ->
                                pageContent(
                                    page,
                                    item,
                                    requestDismiss,
                                    progress,
                                    activeSharedTransition
                                ) { pageAllowsSwipe ->
                                    if (page == pagerState.currentPage) {
                                        currentPageAllowsSwipe = pageAllowsSwipe
                                        if (!pageAllowsSwipe && pagerState.isScrollInProgress) {
                                            scope.launch {
                                                pagerState.stopScroll()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SharedPagedViewerOrientation.Vertical -> {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = enableSwipe && currentPageAllowsSwipe,
                            key = { index -> items.getOrNull(index)?.id ?: index.toLong() }
                        ) { page ->
                            items.getOrNull(page)?.let { item ->
                                pageContent(
                                    page,
                                    item,
                                    requestDismiss,
                                    progress,
                                    activeSharedTransition
                                ) { pageAllowsSwipe ->
                                    if (page == pagerState.currentPage) {
                                        currentPageAllowsSwipe = pageAllowsSwipe
                                        if (!pageAllowsSwipe && pagerState.isScrollInProgress) {
                                            scope.launch {
                                                pagerState.stopScroll()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.matchParentSize()) {
            transitionOverlay(
                items.getOrNull(pagerState.currentPage),
                when {
                    isDismissing -> dismissTargetBounds ?: sourceBoundsInRoot ?: initialSourceBounds
                    else -> initialSourceBounds ?: sourceBoundsInRoot
                },
                measuredContainerSize,
                (1f - progress).coerceIn(0f, 1f),
                progress,
                activeSharedTransition,
                isDismissing
            )
        }

        val chromeReady = !isDismissing && (!hasSharedTransition || enterProgress.value <= 0.02f)
        AnimatedVisibility(
            visible = chromeReady,
            enter = fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = 40)),
            exit = fadeOut(animationSpec = tween(durationMillis = 90)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                chrome(
                    items.getOrNull(pagerState.currentPage),
                    pagerState,
                    requestDismiss
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimilarFullScreenImageViewer(
    items: List<MediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    enableSwipe: Boolean = true,
    isMuted: Boolean = true,
    sourceBoundsInRoot: Rect? = null,
    sourceBoundsByItemId: Map<Long, Rect> = emptyMap(),
    dismissSourceBoundsByItemId: Map<Long, Rect> = sourceBoundsByItemId,
    onSharedDismissStart: ((Long) -> Unit)? = null
) {
    var comparePressed by remember(items, initialIndex) { mutableStateOf(false) }
    val compareInteractionSource = remember { MutableInteractionSource() }

    SharedPagedFullScreenImageViewer(
        items = items,
        initialIndex = initialIndex,
        onDismiss = onDismiss,
        enableSwipe = enableSwipe,
        sourceBoundsInRoot = sourceBoundsInRoot,
        sourceBoundsByItemId = sourceBoundsByItemId,
        dismissSourceBoundsByItemId = dismissSourceBoundsByItemId,
        onSharedDismissStart = onSharedDismissStart,
        orientation = SharedPagedViewerOrientation.Horizontal,
        chrome = { currentItem, activePagerState, requestDismiss ->
            currentItem?.let { activeItem ->
                FullScreenMetadataBar(
                    item = activeItem,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp)
                        .bottomNavigationBarPadding(18.dp),
                    trailingAction = if (items.size > 1) {
                        {
                            val compareButtonBackground = if (comparePressed) {
                                Color.White.copy(alpha = 0.96f)
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            }
                            val compareButtonBorder = if (comparePressed) {
                                Color.White.copy(alpha = 0.96f)
                            } else {
                                Color.White.copy(alpha = 0.22f)
                            }
                            val compareIconTint = if (comparePressed) {
                                Color.Black
                            } else {
                                Color.White.copy(alpha = 0.92f)
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(compareButtonBackground)
                                    .border(1.dp, compareButtonBorder, RoundedCornerShape(10.dp))
                                    .pointerInput(activeItem.id, items.size) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            comparePressed = true
                                            try {
                                                waitForUpOrCancellation()
                                            } finally {
                                                comparePressed = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (comparePressed) {
                                        COLOR_CARD_TRACE_COMPARE_ICON_FILLED
                                    } else {
                                        COLOR_CARD_TRACE_COMPARE_ICON_OUTLINE
                                    },
                                    contentDescription = if (comparePressed) "正在对比" else "按住对比",
                                    tint = compareIconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        null
                    }
                )
            }

            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close",
                onClick = requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(FULL_SCREEN_CHROME_EDGE_PADDING),
                containerColor = Color.Black.copy(alpha = 0.3f),
                iconTint = Color.White
            )

            if (false && items.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 92.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    contentColor = Color.White,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                    onClick = {},
                    interactionSource = compareInteractionSource
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (comparePressed) "对比中" else "按住对比",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (items.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = FULL_SCREEN_BADGE_TOP_PADDING)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (items.size <= 10) {
                        repeat(items.size) { index ->
                            val isSelected = activePagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.5f))
                            )
                        }
                    } else {
                        Text(
                            text = "${activePagerState.currentPage + 1} / ${items.size}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { page, item, requestDismiss, sharedTransitionProgress, hasSharedTransition, onZoomStateChanged ->
        val compareItem = remember(page, items) {
            if (items.size <= 1) {
                null
            } else if (items.size == 2) {
                items[if (page == 0) 1 else 0]
            } else {
                items[(page + 1) % items.size]
            }
        }
        SimilarImageViewerPage(
            item = item,
            onDismiss = requestDismiss,
            isMuted = isMuted,
            allowLivePlayback = !hasSharedTransition ||
                sharedTransitionProgress <= FULL_SCREEN_LIVE_TRANSITION_SETTLE_PROGRESS,
            compareOverrideItem = if (comparePressed) compareItem else null,
            onZoomStateChanged = onZoomStateChanged
        )
    }
}

@Composable
fun FullScreenMetadataBar(
    item: MediaItem,
    reasonText: String? = null,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable BoxScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val dateTimeText = remember(item.dateAdded) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.dateAdded * 1000L))
    }
    val filePath = remember(item.path, item.uri) {
        item.path?.takeIf { it.isNotBlank() } ?: item.uri.toString()
    }
    val fileName = remember(filePath, item.displayName, item.id, item.type) {
        item.displayName.takeIf { it.isNotBlank() }
            ?: File(filePath).name.takeIf { it.isNotBlank() }
            ?: if (item.type == "video") {
                "video_${item.id}.mp4"
            } else {
                "image_${item.id}.jpg"
            }
    }
    val pathDescription = remember(filePath, item.bucketName) {
        val folderName = item.bucketName.takeIf { it.isNotBlank() }
        if (folderName == null || filePath.contains(folderName)) {
            filePath
        } else {
            "$folderName  $filePath"
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.70f),
        contentColor = Color.White
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(end = if (trailingAction != null) 44.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dateTimeText,
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!reasonText.isNullOrBlank()) {
                    Text(
                        text = reasonText,
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = fileName,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pathDescription,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatter.formatFileSize(context, item.size),
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailingAction?.invoke(this)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LegacySimilarFullScreenImageViewer(
    items: List<MediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    enableSwipe: Boolean = true, // Legacy parameter, now controlled internally
    isMuted: Boolean = true,
    sourceBoundsInRoot: Rect? = null,
    sourceBoundsByItemId: Map<Long, Rect> = emptyMap(),
    onSharedDismissStart: ((Long) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val initialSourceItemId = remember(items, initialIndex) {
        items.getOrNull(initialIndex)?.id ?: items.firstOrNull()?.id
    }
    val initialSourceBounds = remember(sourceBoundsInRoot, sourceBoundsByItemId, initialSourceItemId) {
        initialSourceItemId?.let { sourceBoundsByItemId[it] } ?: sourceBoundsInRoot
    }
    val hasSharedBoundsTarget = remember(initialSourceBounds) {
        initialSourceBounds != null &&
            initialSourceBounds.width > 1f &&
            initialSourceBounds.height > 1f
    }
    val hasSharedTransition = hasSharedBoundsTarget
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

    val pagerState = rememberPagerState(initialPage = initialIndex) {
        items.size
    }
    val compareInteractionSource = remember { MutableInteractionSource() }
    val comparePressed by compareInteractionSource.collectIsPressedAsState()
    var currentPageAllowsSwipe by remember { mutableStateOf(true) }

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
                onDismiss()
            }
        } else {
            isDismissing = true
            val sourceItemId = items.getOrNull(pagerState.currentPage)?.id ?: items.firstOrNull()?.id
            val targetBounds = sourceItemId?.let { sourceBoundsByItemId[it] } ?: sourceBoundsInRoot ?: initialSourceBounds
            dismissTargetBounds = targetBounds
            val capturedSize = latestContainerSize
            dismissContainerSize = if (capturedSize.width > 0 && capturedSize.height > 0) {
                capturedSize
            } else {
                null
            }
            if (sourceItemId != null) {
                onSharedDismissStart?.invoke(sourceItemId)
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
                onDismiss()
            }
        }
    }

    LaunchedEffect(transitionKey, hasSharedTransition) {
        dismissTargetBounds = null
        dismissContainerSize = null
        dismissProgress.snapTo(0f)
        isDismissing = false
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

    BackHandler(onBack = requestDismiss)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val measuredContainerSize = IntSize(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        )
        if (measuredContainerSize.width > 0 && measuredContainerSize.height > 0 &&
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
            (dismissTarget.width / containerWidth).coerceIn(0.12f, 1f)
        } else {
            1f
        }
        val targetScaleY = if (dismissTarget != null && hasSharedTransition) {
            (dismissTarget.height / containerHeight).coerceIn(0.12f, 1f)
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
        val animatedScaleX = lerpFloat(1f, targetScaleX, progress)
        val animatedScaleY = lerpFloat(1f, targetScaleY, progress)
        val animatedTranslationX = lerpFloat(0f, targetTranslationX, progress)
        val animatedTranslationY = lerpFloat(0f, targetTranslationY, progress)
        val cornerProgress = sqrt((progress / 0.72f).coerceIn(0f, 1f))
        val animatedCorner = lerp(0.dp, 24.dp, cornerProgress)
        val sharedScrimAlpha = lerpFloat(1f, 0f, progress)
        val plainAlpha = if (hasSharedTransition) 1f else plainDismissAlpha.value
        val plainScale = if (hasSharedTransition) 1f else lerpFloat(0.965f, 1f, plainDismissAlpha.value)
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = enableSwipe && currentPageAllowsSwipe,
                    key = { index -> items.getOrNull(index)?.id ?: index }
                ) { page ->
                    items.getOrNull(page)?.let { item ->
                        val compareItem = remember(page, items) {
                            if (items.size <= 1) null else {
                                if (items.size == 2) {
                                    items[if (page == 0) 1 else 0]
                                } else {
                                    items[(page + 1) % items.size]
                                }
                            }
                        }
                        SimilarImageViewerPage(
                            item = item,
                            onDismiss = requestDismiss,
                            isMuted = isMuted,
                            compareOverrideItem = if (comparePressed) compareItem else null,
                            onZoomStateChanged = { pageAllowsSwipe ->
                                if (page == pagerState.currentPage) {
                                    currentPageAllowsSwipe = pageAllowsSwipe
                                    if (!pageAllowsSwipe && pagerState.isScrollInProgress) {
                                        scope.launch {
                                            pagerState.stopScroll()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        val chromeReady = !isDismissing && (!hasSharedTransition || enterProgress.value <= 0.02f)
        AnimatedVisibility(
            visible = chromeReady,
            enter = fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = 40)),
            exit = fadeOut(animationSpec = tween(durationMillis = 90)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    onClick = requestDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(FULL_SCREEN_CHROME_EDGE_PADDING),
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    iconTint = Color.White
                )

                if (items.size > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 92.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        contentColor = Color.White,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        onClick = {},
                        interactionSource = compareInteractionSource
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (comparePressed) "对比中" else "按住对比",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (items.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .bottomNavigationBarPadding(32.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (items.size <= 10) {
                            repeat(items.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 10.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.5f))
                                )
                            }
                        } else {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${items.size}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimilarImageViewerPage(
    item: MediaItem,
    onDismiss: () -> Unit,
    isMuted: Boolean = true,
    allowLivePlayback: Boolean = true,
    compareOverrideItem: MediaItem? = null,
    onZoomStateChanged: (Boolean) -> Unit = {}
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val badgeBackground = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
    } else {
        Color.White.copy(alpha = 0.8f)
    }
    val badgeContent = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    var zoomAnimationJob by remember(item.id) { mutableStateOf<Job?>(null) }
    var livePlaybackCompleted by remember(item.id) { mutableStateOf(false) }
    var liveReplayToken by remember(item.id) { mutableLongStateOf(0L) }
    
    // Motion Photo / Live Photo Logic
    val context = LocalContext.current
    var currentItem by remember(item.id) { mutableStateOf(item) }
    val fullScreenSwipeDismissState = rememberFullScreenSwipeDismissState(onDismiss = onDismiss)
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
                scale = lerpFloat(startScale, targetScale, value)
                offset = Offset(
                    x = lerpFloat(startOffset.x, targetOffset.x, value),
                    y = lerpFloat(startOffset.y, targetOffset.y, value)
                )
                onZoomStateChanged(scale <= 1.01f)
            }
            scale = targetScale
            offset = targetOffset
            onZoomStateChanged(scale <= 1.01f)
            zoomAnimationJob = null
        }
    }

    LaunchedEffect(item) {
        stopZoomAnimation()
        scale = 1f
        offset = Offset.Zero
        onZoomStateChanged(true)
        currentItem = item
        livePlaybackCompleted = false
        liveReplayToken = 0L
        if (!currentItem.isMotionPhoto && !currentItem.isLivePhoto) {
            withContext(Dispatchers.IO) {
                val motionInfo = MotionPhotoHelper.detectAndPersist(context, item)
                if (motionInfo.isMotionPhoto) {
                    currentItem = currentItem.copy(
                        isMotionPhoto = true,
                        motionPhotoVideoOffset = motionInfo.videoOffset,
                        motionPhotoVideoLength = motionInfo.videoLength
                    )
                }
            }
        }
    }

    val isLiveType = compareOverrideItem == null &&
        ((currentItem.isLivePhoto && currentItem.livePhotoVideoUri != null) || currentItem.isMotionPhoto)
    val isLivePlaybackActive = isLiveType && !livePlaybackCompleted && allowLivePlayback
    val shouldAllowZoom = !isLivePlaybackActive

    LaunchedEffect(shouldAllowZoom) {
        if (!shouldAllowZoom) {
            stopZoomAnimation()
            scale = 1f
            offset = Offset.Zero
            onZoomStateChanged(true)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val constraints = constraints
        val containerSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val compareCandidate = compareOverrideItem
        var pendingTapJob by remember(item.id, currentItem.id, compareCandidate?.id) { mutableStateOf<Job?>(null) }
        var pendingTapPosition by remember(item.id, currentItem.id, compareCandidate?.id) { mutableStateOf<Offset?>(null) }
        val clearPendingTap: () -> Unit = {
            pendingTapJob?.cancel()
            pendingTapJob = null
            pendingTapPosition = null
        }
        DisposableEffect(item.id, currentItem.id, compareCandidate?.id) {
            onDispose {
                pendingTapJob?.cancel()
            }
        }
        LaunchedEffect(compareCandidate?.id, compareCandidate?.uri, containerSize) {
            val candidate = compareCandidate ?: return@LaunchedEffect
            if (containerSize.width <= 0 || containerSize.height <= 0) return@LaunchedEffect
            val request = buildFullscreenPreviewImageRequest(
                context = context,
                item = candidate,
                containerSize = containerSize
            )
            withContext(Dispatchers.IO) {
                runCatching { context.imageLoader.execute(request) }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fullScreenSwipeToDismiss(
                    enabled = shouldAllowZoom && scale <= 1.05f,
                    state = fullScreenSwipeDismissState
                )
                .pointerInput(item.id, currentItem.id, shouldAllowZoom, compareCandidate?.id, containerSize) {
                    val tapSlop = viewConfiguration.touchSlop
                    val doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        stopZoomAnimation()
                        var lastPosition = Offset.Unspecified
                        var tapCandidate = true
                        var handledTransform = false
                        var parentSwipeLocked = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressedCount = event.changes.count { it.pressed }
                            val primaryChange = event.changes.firstOrNull { it.pressed } ?: event.changes.firstOrNull()
                            if (primaryChange != null) {
                                lastPosition = primaryChange.position
                            }
                            if (pressedCount > 1) {
                                tapCandidate = false
                                handledTransform = true
                                clearPendingTap()
                                if (shouldAllowZoom) {
                                    if (!parentSwipeLocked) {
                                        onZoomStateChanged(false)
                                        parentSwipeLocked = true
                                    }
                                    fullScreenSwipeDismissState.snapToRest()
                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            change.consume()
                                        }
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
                                }
                            } else if (pressedCount == 1 && scale > 1f) {
                                val change = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                val pan = change.positionChange()
                                if (pan != Offset.Zero) {
                                    tapCandidate = false
                                    handledTransform = true
                                    clearPendingTap()
                                    val maxX = ((constraints.maxWidth * scale) - constraints.maxWidth) / 2f
                                    val maxY = ((constraints.maxHeight * scale) - constraints.maxHeight) / 2f
                                    val next = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                    val movedX = kotlin.math.abs(next.x - offset.x)
                                    val movedY = kotlin.math.abs(next.y - offset.y)
                                    offset = next
                                    if (movedX > 0.5f || movedY > 0.5f) {
                                        change.consume()
                                    }
                                }
                            } else if (primaryChange != null && lastPosition != Offset.Unspecified) {
                                val dx = lastPosition.x - down.position.x
                                val dy = lastPosition.y - down.position.y
                                if (sqrt(dx * dx + dy * dy) > tapSlop) {
                                    tapCandidate = false
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
                        if (!tapCandidate || handledTransform || lastPosition == Offset.Unspecified) {
                            return@awaitEachGesture
                        }
                        val pendingPosition = pendingTapPosition
                        if (pendingTapJob != null && pendingPosition != null) {
                            val dx = pendingPosition.x - lastPosition.x
                            val dy = pendingPosition.y - lastPosition.y
                            if (sqrt(dx * dx + dy * dy) <= tapSlop * 1.5f) {
                                clearPendingTap()
                                if (shouldAllowZoom) {
                                    val displayItem = compareOverrideItem ?: currentItem
                                    val mediaRect = fittedContentRect(
                                        containerSize = containerSize,
                                        mediaWidth = displayItem.width,
                                        mediaHeight = displayItem.height
                                    )
                                    if (mediaRect.contains(lastPosition)) {
                                        if (scale > 1.05f) {
                                            animateZoomTo(1f, Offset.Zero)
                                        } else {
                                            val targetScale = 2.5f
                                            val center = Offset(
                                                x = constraints.maxWidth / 2f,
                                                y = constraints.maxHeight / 2f
                                            )
                                            val rawTargetOffset = Offset(
                                                x = offset.x + (scale - targetScale) * (lastPosition.x - center.x),
                                                y = offset.y + (scale - targetScale) * (lastPosition.y - center.y)
                                            )
                                            val targetOffset = clampPanOffset(
                                                offset = rawTargetOffset,
                                                scale = targetScale,
                                                containerWidth = constraints.maxWidth,
                                                containerHeight = constraints.maxHeight
                                            )
                                            animateZoomTo(targetScale, targetOffset)
                                        }
                                    }
                                }
                                return@awaitEachGesture
                            }
                            clearPendingTap()
                        }
                        pendingTapPosition = lastPosition
                        pendingTapJob = scope.launch {
                            delay(doubleTapTimeoutMillis)
                            val displayItem = compareOverrideItem ?: currentItem
                            val mediaRect = fittedContentRect(
                                containerSize = containerSize,
                                mediaWidth = displayItem.width,
                                mediaHeight = displayItem.height
                            )
                            if (scale <= 1.05f && !mediaRect.contains(lastPosition)) {
                                onDismiss()
                            }
                            pendingTapJob = null
                            pendingTapPosition = null
                        }
                    }
                }
        ) {
            val displayItem = compareOverrideItem ?: currentItem
            val shouldRenderLiveVideo = isLiveType && !livePlaybackCompleted
            val imageRequest = remember(displayItem.id, displayItem.uri, containerSize) {
                buildFullscreenPreviewImageRequest(
                    context = context,
                    item = displayItem,
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
                        scaleX = scale * fullScreenSwipeDismissState.contentScale
                        scaleY = scale * fullScreenSwipeDismissState.contentScale
                        translationX = offset.x
                        translationY = offset.y + fullScreenSwipeDismissState.offsetY
                    }
            )

            if (shouldRenderLiveVideo) {
                VideoPlayer(
                    item = if (currentItem.isMotionPhoto) {
                        currentItem
                    } else {
                        item.copy(uri = currentItem.livePhotoVideoUri!!, mimeType = "video/mp4")
                    },
                    isMuted = isMuted,
                    shouldPlay = allowLivePlayback,
                    repeatOne = false,
                    playerResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    replayToken = liveReplayToken,
                    onPlaybackEnded = { livePlaybackCompleted = true }
                )
            }

            if (isLiveType) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = FULL_SCREEN_CHROME_EDGE_PADDING,
                            top = FULL_SCREEN_BADGE_TOP_PADDING
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBackground, RoundedCornerShape(4.dp))
                        .pointerInput(currentItem.id, livePlaybackCompleted) {
                            detectTapGestures {
                                livePlaybackCompleted = false
                                liveReplayToken += 1L
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (livePlaybackCompleted) "REPLAY" else "LIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeContent
                    )
                }
            } else {
                val badgeText = getFormatBadgeText(currentItem)
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = FULL_SCREEN_CHROME_EDGE_PADDING,
                                top = FULL_SCREEN_BADGE_TOP_PADDING
                            )
                            .background(badgeBackground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = badgeContent)
                    }
                }
            }
        }
    }
}

@Composable
fun MainFullScreenImageViewer(
    item: MediaItem,
    dayContextItems: List<MediaItem> = emptyList(),
    onJumpToDay: ((String) -> Unit)? = null,
    sourceBoundsInRoot: Rect? = null,
    dismissSourceBoundsInRoot: Rect? = null,
    onSharedDismissStart: ((Long) -> Unit)? = null,
    sharedTargetCornerRadius: Dp = 24.dp,
    sharedScaleClampMin: Float = 0.12f,
    showDayContextDrawer: Boolean = true,
    bottomOverlay: (@Composable BoxScope.(MediaItem) -> Unit)? = null,
    isMuted: Boolean = true,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val initialSourceBounds = remember(item.id) { sourceBoundsInRoot }
    val supportsSharedImageTransition = remember(
        item.id,
        item.type
    ) {
        item.type == "photo"
    }
    val hasSharedBoundsTarget = remember(initialSourceBounds) {
        initialSourceBounds != null &&
            initialSourceBounds.width > 1f &&
            initialSourceBounds.height > 1f
    }
    val hasSharedTransition = supportsSharedImageTransition && hasSharedBoundsTarget
    val enterProgress = remember(item.id, hasSharedTransition) {
        Animatable(if (hasSharedTransition) 1f else 0f)
    }
    val dismissProgress = remember(item.id) { Animatable(0f) }
    val plainDismissAlpha = remember(item.id) { Animatable(1f) }
    var isDismissing by remember(item.id) { mutableStateOf(false) }
    var dismissTargetBounds by remember(item.id) { mutableStateOf<Rect?>(null) }
    var latestContainerSize by remember(item.id) { mutableStateOf(IntSize.Zero) }
    var dismissContainerSize by remember(item.id) { mutableStateOf<IntSize?>(null) }

    val dayDrawerEnabled = showDayContextDrawer && dayContextItems.size > 1
    var activeItem by remember(item.id) { mutableStateOf(item) }
    var contextEntryItem by remember(item.id) { mutableStateOf(item) }
    var contextExpanded by remember(item.id) { mutableStateOf(false) }
    var contextDragInProgress by remember(item.id) { mutableStateOf(false) }
    var drawerDragEnabled by remember(item.id) { mutableStateOf(true) }
    val openContextDrawer: () -> Unit = {
        if (dayDrawerEnabled) {
            if (!contextExpanded) {
                contextEntryItem = activeItem
            }
            contextDragInProgress = false
            contextExpanded = true
        }
    }
    val closeContextDrawer: () -> Unit = {
        if (dayDrawerEnabled) {
            if (contextExpanded) {
                activeItem = contextEntryItem
            }
            contextDragInProgress = false
            contextExpanded = false
        } else {
            contextDragInProgress = false
            contextExpanded = false
        }
    }

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
                onDismiss()
            }
        } else {
            contextExpanded = false
            isDismissing = true
            dismissTargetBounds = dismissSourceBoundsInRoot ?: sourceBoundsInRoot ?: initialSourceBounds
            val capturedSize = latestContainerSize
            dismissContainerSize = if (capturedSize.width > 0 && capturedSize.height > 0) {
                capturedSize
            } else {
                null
            }
            onSharedDismissStart?.invoke(contextEntryItem.id)
            scope.launch {
                enterProgress.stop()
                dismissProgress.snapTo(0f)
                dismissProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
                // Ensure the final shared frame is drawn before tearing down the overlay.
                dismissProgress.snapTo(1f)
                withFrameNanos { }
                onDismiss()
            }
        }
    }

    LaunchedEffect(item.id, hasSharedTransition) {
        dismissTargetBounds = null
        dismissContainerSize = null
        dismissProgress.snapTo(0f)
        isDismissing = false
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

    BackHandler {
        if (dayDrawerEnabled && contextExpanded) {
            closeContextDrawer()
        } else {
            requestDismiss()
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val measuredContainerSize = IntSize(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        )
        if (measuredContainerSize.width > 0 && measuredContainerSize.height > 0 &&
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
            isDismissing -> dismissTargetBounds ?: dismissSourceBoundsInRoot ?: sourceBoundsInRoot ?: initialSourceBounds
            else -> initialSourceBounds ?: sourceBoundsInRoot
        }
        val minSharedScale = sharedScaleClampMin.coerceIn(0.01f, 1f)
        val targetScaleX = if (dismissTarget != null && hasSharedTransition) {
            (dismissTarget.width / containerWidth).coerceIn(minSharedScale, 1f)
        } else {
            1f
        }
        val targetScaleY = if (dismissTarget != null && hasSharedTransition) {
            (dismissTarget.height / containerHeight).coerceIn(minSharedScale, 1f)
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
        val animatedScaleX = lerpFloat(1f, targetScaleX, progress)
        val animatedScaleY = lerpFloat(1f, targetScaleY, progress)
        val animatedTranslationX = lerpFloat(0f, targetTranslationX, progress)
        val animatedTranslationY = lerpFloat(0f, targetTranslationY, progress)
        val cornerProgress = sqrt((progress / 0.72f).coerceIn(0f, 1f))
        val animatedCorner = lerp(0.dp, sharedTargetCornerRadius, cornerProgress)
        val sharedScrimAlpha = lerpFloat(1f, 0f, progress)
        val plainAlpha = if (hasSharedTransition) 1f else plainDismissAlpha.value
        val plainScale = if (hasSharedTransition) 1f else lerpFloat(0.965f, 1f, plainDismissAlpha.value)
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
        val chromeReady = !isDismissing &&
            (!hasSharedTransition || enterProgress.value <= FULL_SCREEN_LIVE_TRANSITION_SETTLE_PROGRESS)
        val allowLivePlayback = chromeReady &&
            !contextExpanded &&
            !contextDragInProgress

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
                MainImageViewerPage(
                    item = activeItem,
                    isMuted = isMuted,
                    allowLivePlayback = allowLivePlayback,
                    contextExpanded = dayDrawerEnabled && contextExpanded,
                    onDismiss = requestDismiss,
                    onRequestCollapseContext = {
                        if (dayDrawerEnabled) {
                            closeContextDrawer()
                        }
                    },
                    onDrawerDragEnabledChange = { enabled ->
                        if (dayDrawerEnabled) {
                            drawerDragEnabled = enabled
                            if (!enabled && contextExpanded) {
                                closeContextDrawer()
                            }
                        }
                    }
                )

                bottomOverlay?.invoke(this, activeItem)
            }
        }

        AnimatedVisibility(
            visible = chromeReady,
            enter = fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = 40)),
            exit = fadeOut(animationSpec = tween(durationMillis = 90)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (dayDrawerEnabled && contextExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                closeContextDrawer()
                            }
                    )
                }

                if (dayDrawerEnabled) {
                    FullScreenDayContextDrawer(
                        currentItem = activeItem,
                        contextItems = dayContextItems,
                        expanded = contextExpanded,
                        onExpandedChange = { expanded ->
                            if (expanded) {
                                openContextDrawer()
                            } else {
                                closeContextDrawer()
                            }
                        },
                        onDragActiveChange = { dragging ->
                            contextDragInProgress = dragging
                        },
                        onSelectItem = { selected ->
                            activeItem = selected
                        },
                        anchorItem = contextEntryItem,
                        dragEnabled = drawerDragEnabled,
                        onJumpToDay = onJumpToDay,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    onClick = requestDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(FULL_SCREEN_CHROME_EDGE_PADDING),
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    iconTint = Color.White
                )
            }
        }
    }
}

@Composable
fun MainImageViewerPage(
    item: MediaItem,
    isMuted: Boolean = true,
    allowLivePlayback: Boolean = true,
    contextExpanded: Boolean = false,
    onDismiss: () -> Unit,
    onRequestCollapseContext: () -> Unit = {},
    onDrawerDragEnabledChange: (Boolean) -> Unit = {}
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val badgeBackground = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
    } else {
        Color.White.copy(alpha = 0.8f)
    }
    val badgeContent = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var transformGestureActive by remember(item.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var zoomAnimationJob by remember(item.id) { mutableStateOf<Job?>(null) }
    var livePlaybackCompleted by remember(item.id) { mutableStateOf(false) }
    var liveReplayToken by remember(item.id) { mutableLongStateOf(0L) }
    val context = LocalContext.current
    var currentItem by remember(item.id) { mutableStateOf(item) }
    val fullScreenSwipeDismissState = rememberFullScreenSwipeDismissState(onDismiss = onDismiss)
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
                scale = lerpFloat(startScale, targetScale, value)
                offset = Offset(
                    x = lerpFloat(startOffset.x, targetOffset.x, value),
                    y = lerpFloat(startOffset.y, targetOffset.y, value)
                )
            }
            scale = targetScale
            offset = targetOffset
            zoomAnimationJob = null
        }
    }

    LaunchedEffect(item) {
        stopZoomAnimation()
        scale = 1f
        offset = Offset.Zero
        transformGestureActive = false
        currentItem = item
        livePlaybackCompleted = false
        liveReplayToken = 0L
        if (!currentItem.isMotionPhoto && !currentItem.isLivePhoto) {
            withContext(Dispatchers.IO) {
                val motionInfo = MotionPhotoHelper.detectAndPersist(context, item)
                if (motionInfo.isMotionPhoto) {
                    currentItem = currentItem.copy(
                        isMotionPhoto = true,
                        motionPhotoVideoOffset = motionInfo.videoOffset,
                        motionPhotoVideoLength = motionInfo.videoLength
                    )
                }
            }
        }
    }

    val isLiveType = (currentItem.isLivePhoto && currentItem.livePhotoVideoUri != null) || currentItem.isMotionPhoto
    val isLivePlaybackActive = isLiveType && !livePlaybackCompleted && allowLivePlayback
    val shouldAllowZoom = !isLivePlaybackActive

    LaunchedEffect(shouldAllowZoom) {
        if (!shouldAllowZoom) {
            stopZoomAnimation()
            scale = 1f
            offset = Offset.Zero
            transformGestureActive = false
        }
    }

    LaunchedEffect(scale, transformGestureActive) {
        onDrawerDragEnabledChange(!transformGestureActive && scale <= 1.05f)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val constraints = constraints
        val containerSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        var pendingTapJob by remember(item.id, currentItem.id) { mutableStateOf<Job?>(null) }
        var pendingTapPosition by remember(item.id, currentItem.id) { mutableStateOf<Offset?>(null) }
        val clearPendingTap: () -> Unit = {
            pendingTapJob?.cancel()
            pendingTapJob = null
            pendingTapPosition = null
        }
        DisposableEffect(item.id, currentItem.id) {
            onDispose {
                pendingTapJob?.cancel()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fullScreenSwipeToDismiss(
                    enabled = shouldAllowZoom && !contextExpanded && scale <= 1.05f,
                    state = fullScreenSwipeDismissState
                )
                .pointerInput(item.id, currentItem.id, shouldAllowZoom, contextExpanded, containerSize) {
                    val tapSlop = viewConfiguration.touchSlop
                    val doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        stopZoomAnimation()
                        var lastPosition = Offset.Unspecified
                        var tapCandidate = true
                        var handledTransform = false
                        var parentDragLocked = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressedCount = event.changes.count { it.pressed }
                            val primaryChange = event.changes.firstOrNull { it.pressed } ?: event.changes.firstOrNull()
                            if (primaryChange != null) {
                                lastPosition = primaryChange.position
                            }
                            if (pressedCount > 1) {
                                tapCandidate = false
                                handledTransform = true
                                clearPendingTap()
                                if (shouldAllowZoom) {
                                    if (!parentDragLocked) {
                                        transformGestureActive = true
                                        onDrawerDragEnabledChange(false)
                                        parentDragLocked = true
                                    }
                                    fullScreenSwipeDismissState.snapToRest()
                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            change.consume()
                                        }
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
                                }
                            } else if (pressedCount == 1 && scale > 1f) {
                                val change = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                val pan = change.positionChange()
                                if (pan != Offset.Zero) {
                                    tapCandidate = false
                                    handledTransform = true
                                    clearPendingTap()
                                    val maxX = ((constraints.maxWidth * scale) - constraints.maxWidth) / 2f
                                    val maxY = ((constraints.maxHeight * scale) - constraints.maxHeight) / 2f
                                    val next = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                    val movedX = kotlin.math.abs(next.x - offset.x)
                                    val movedY = kotlin.math.abs(next.y - offset.y)
                                    offset = next
                                    // Only consume when image actually moves.
                                    // At horizontal boundary, leave gesture to pager for page switch.
                                    if (movedX > 0.5f || movedY > 0.5f) {
                                        change.consume()
                                    }
                                }
                            } else if (primaryChange != null && lastPosition != Offset.Unspecified) {
                                val dx = lastPosition.x - down.position.x
                                val dy = lastPosition.y - down.position.y
                                if (sqrt(dx * dx + dy * dy) > tapSlop) {
                                    tapCandidate = false
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        if (scale <= 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        }
                        if (parentDragLocked) {
                            transformGestureActive = false
                            onDrawerDragEnabledChange(scale <= 1.05f)
                        }
                        if (!tapCandidate || handledTransform || lastPosition == Offset.Unspecified) {
                            return@awaitEachGesture
                        }
                        if (contextExpanded) {
                            clearPendingTap()
                            onRequestCollapseContext()
                            return@awaitEachGesture
                        }
                        val pendingPosition = pendingTapPosition
                        if (pendingTapJob != null && pendingPosition != null) {
                            val dx = pendingPosition.x - lastPosition.x
                            val dy = pendingPosition.y - lastPosition.y
                            if (sqrt(dx * dx + dy * dy) <= tapSlop * 1.5f) {
                                clearPendingTap()
                                if (shouldAllowZoom) {
                                    val mediaRect = fittedContentRect(
                                        containerSize = containerSize,
                                        mediaWidth = currentItem.width,
                                        mediaHeight = currentItem.height
                                    )
                                    if (mediaRect.contains(lastPosition)) {
                                        if (scale > 1.05f) {
                                            animateZoomTo(1f, Offset.Zero)
                                        } else {
                                            val targetScale = 2.5f
                                            val center = Offset(
                                                x = constraints.maxWidth / 2f,
                                                y = constraints.maxHeight / 2f
                                            )
                                            val rawTargetOffset = Offset(
                                                x = offset.x + (scale - targetScale) * (lastPosition.x - center.x),
                                                y = offset.y + (scale - targetScale) * (lastPosition.y - center.y)
                                            )
                                            val targetOffset = clampPanOffset(
                                                offset = rawTargetOffset,
                                                scale = targetScale,
                                                containerWidth = constraints.maxWidth,
                                                containerHeight = constraints.maxHeight
                                            )
                                            animateZoomTo(targetScale, targetOffset)
                                        }
                                    }
                                }
                                return@awaitEachGesture
                            }
                            clearPendingTap()
                        }
                        pendingTapPosition = lastPosition
                        pendingTapJob = scope.launch {
                            delay(doubleTapTimeoutMillis)
                            val mediaRect = fittedContentRect(
                                containerSize = containerSize,
                                mediaWidth = currentItem.width,
                                mediaHeight = currentItem.height
                            )
                            if (scale <= 1.05f && !mediaRect.contains(lastPosition)) {
                                onDismiss()
                            }
                            pendingTapJob = null
                            pendingTapPosition = null
                        }
                    }
                }
        ) {
            val shouldRenderLiveVideo = isLiveType && !livePlaybackCompleted
            val imageRequest = remember(currentItem.id, currentItem.uri, containerSize) {
                buildFullscreenPreviewImageRequest(
                    context = context,
                    item = currentItem,
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
                        scaleX = scale * fullScreenSwipeDismissState.contentScale
                        scaleY = scale * fullScreenSwipeDismissState.contentScale
                        translationX = offset.x
                        translationY = offset.y + fullScreenSwipeDismissState.offsetY
                    }
            )

            if (shouldRenderLiveVideo) {
                VideoPlayer(
                    item = if (currentItem.isMotionPhoto) {
                        currentItem
                    } else {
                        item.copy(uri = currentItem.livePhotoVideoUri!!, mimeType = "video/mp4")
                    },
                    isMuted = isMuted,
                    shouldPlay = allowLivePlayback,
                    repeatOne = false,
                    playerResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    replayToken = liveReplayToken,
                    onPlaybackEnded = { livePlaybackCompleted = true }
                )
            }

            if (isLiveType) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = FULL_SCREEN_CHROME_EDGE_PADDING,
                            top = FULL_SCREEN_BADGE_TOP_PADDING
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBackground, RoundedCornerShape(4.dp))
                        .pointerInput(currentItem.id, livePlaybackCompleted) {
                            detectTapGestures {
                                livePlaybackCompleted = false
                                liveReplayToken += 1L
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (livePlaybackCompleted) "REPLAY" else "LIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeContent
                    )
                }
            } else {
                val badgeText = getFormatBadgeText(currentItem)
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = FULL_SCREEN_CHROME_EDGE_PADDING,
                                top = FULL_SCREEN_BADGE_TOP_PADDING
                            )
                            .background(badgeBackground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = badgeContent)
                    }
                }
            }
        }
    }

}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + ((stop - start) * fraction)
}

private fun clampPanOffset(
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

private fun fittedContentRect(
    containerSize: IntSize,
    mediaWidth: Int,
    mediaHeight: Int
): Rect = calculateFittedContentRect(containerSize, mediaWidth, mediaHeight)

