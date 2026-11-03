package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val HOME_CARD_TOP_ALIGN_THRESHOLD_RATIO = 1f / 7f

private data class HomeCardPhotoLayout(
    val imageBoundsInContainer: Rect,
    val alignment: Alignment
)

private fun resolveHomeCardPhotoLayout(
    containerWidthPx: Float,
    containerHeightPx: Float,
    mediaWidth: Int,
    mediaHeight: Int
): HomeCardPhotoLayout? {
    val safeContainerWidth = containerWidthPx.coerceAtLeast(1f)
    val safeContainerHeight = containerHeightPx.coerceAtLeast(1f)
    val safeMediaWidth = mediaWidth.coerceAtLeast(1).toFloat()
    val safeMediaHeight = mediaHeight.coerceAtLeast(1).toFloat()
    if (!safeContainerWidth.isFinite() || !safeContainerHeight.isFinite()) return null

    val mediaAspect = safeMediaWidth / safeMediaHeight
    val containerAspect = safeContainerWidth / safeContainerHeight
    val drawWidth: Float
    val drawHeight: Float
    if (mediaAspect > containerAspect) {
        drawWidth = safeContainerWidth
        drawHeight = drawWidth / mediaAspect
    } else {
        drawHeight = safeContainerHeight
        drawWidth = drawHeight * mediaAspect
    }

    val left = (safeContainerWidth - drawWidth) / 2f
    val centeredTop = (safeContainerHeight - drawHeight) / 2f
    val shouldAlignTop = centeredTop > 0f &&
        centeredTop < (drawHeight * HOME_CARD_TOP_ALIGN_THRESHOLD_RATIO)
    val top = if (shouldAlignTop) 0f else centeredTop
    return HomeCardPhotoLayout(
        imageBoundsInContainer = Rect(
            left = left,
            top = top,
            right = left + drawWidth,
            bottom = top + drawHeight
        ),
        alignment = if (shouldAlignTop) Alignment.TopCenter else Alignment.Center
    )
}

private fun resolveHomeCardPhotoBoundsInRoot(
    containerBoundsInRoot: Rect,
    mediaWidth: Int,
    mediaHeight: Int
): Rect? {
    val layout = resolveHomeCardPhotoLayout(
        containerWidthPx = containerBoundsInRoot.width,
        containerHeightPx = containerBoundsInRoot.height,
        mediaWidth = mediaWidth,
        mediaHeight = mediaHeight
    ) ?: return null
    val imageBounds = layout.imageBoundsInContainer
    return Rect(
        left = containerBoundsInRoot.left + imageBounds.left,
        top = containerBoundsInRoot.top + imageBounds.top,
        right = containerBoundsInRoot.left + imageBounds.right,
        bottom = containerBoundsInRoot.top + imageBounds.bottom
    )
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
private fun CardMuteToggleButton(
    isMuted: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val language = LocalAppLanguage.current
    val commonUiCopy = remember(language) { getCommonGlobalUiCopy(language) }
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .background(
                if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f),
                CircleShape
            )
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
    ) {
        Icon(
            imageVector = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
            contentDescription = commonUiCopy.mute,
            tint = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/** LIVE / GIF / SVG / RAW tag: card-coloured, 26dp tall, hairline border (iOS CardMediaTypeBadge). */
@Composable
private fun CardMediaTypeBadge(
    title: String,
    isDark: Boolean,
    showLiveIcon: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    val foreground = organizeForegroundColor(isDark)
    Row(
        modifier = Modifier
            .height(26.dp)
            .background(organizeCardSurfaceColor(isDark), shape)
            .border(0.5.dp, organizeBorderColor(isDark).copy(alpha = 0.8f), shape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showLiveIcon) {
            Icon(
                imageVector = Icons.Outlined.MotionPhotosOn,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = title,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            color = foreground,
            maxLines = 1,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

/** Small round info button laid on the photo: white glyph on a 32% black disc, 44dp hit area. */
@Composable
private fun CardPhotoInfoButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color.Black.copy(alpha = 0.32f), CircleShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
internal fun DraggableCardFrontContent(
    item: MediaItem,
    currentItem: MediaItem,
    livePlaybackItem: MediaItem,
    stillImageRequest: ImageRequest,
    videoFrameRequest: ImageRequest,
    shouldRenderVideoPlayer: Boolean,
    shouldPlayVideo: Boolean,
    isCardDragging: Boolean,
    dragPauseToken: Long,
    onStartupSample: ((Long) -> Unit)?,
    onVideoSeekingChange: (Boolean) -> Unit,
    onOpenFullScreenVideo: ((MediaItem, ExoPlayer) -> Unit)?,
    isFullScreenVideoOpen: Boolean,
    isMuted: Boolean,
    onMutedChange: (Boolean) -> Unit,
    livePlaybackArmed: Boolean,
    livePlaybackCompleted: Boolean,
    liveReplayToken: Long,
    onLivePlaybackCompletedChange: (Boolean) -> Unit,
    onLiveReplayRequest: () -> Unit,
    imageLongPressHoverEnabled: Boolean,
    longPressOverlaySourceImageAlpha: Float,
    imageLongPressHoverVisuals: HomeCardLongPressHoverVisuals,
    onImageLongPressHoverChange: (Boolean) -> Unit,
    mlLabels: List<String>,
    showMlLabelsOnHome: Boolean,
    enableFooterMarquee: Boolean = true,
    compactFooter: Boolean = false,
    footerSecondaryText: String? = null,
    footerSecondaryTextColor: Color = Color.Unspecified,
    trackImageBoundsInRoot: Boolean = true,
    forceImagePlaceholder: Boolean = false,
    onImageBoundsInRootChanged: (Rect) -> Unit = {},
    onExpandDetails: () -> Unit,
    photoFrameMode: Boolean = false,
    onMediaAspectRatioResolved: ((Float) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val language = LocalAppLanguage.current
    val commonUiCopy = remember(language) { getCommonGlobalUiCopy(language) }
    val density = LocalDensity.current
    val mediaPlaceholderColor = if (photoFrameMode) {
        organizePanelColor(isDark)
    } else if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(0xFFF3F4F6)
    }
    val isLivePhotoCard = (item.isLivePhoto && item.livePhotoVideoUri != null) || currentItem.isMotionPhoto
    val currentOnImageBoundsInRootChanged = rememberUpdatedState(onImageBoundsInRootChanged)
    val currentOnMediaAspectRatioResolved = rememberUpdatedState(onMediaAspectRatioResolved)
    val fallbackMediaWidth = currentItem.width.takeIf { it > 0 } ?: item.width
    val fallbackMediaHeight = currentItem.height.takeIf { it > 0 } ?: item.height
    var mediaContainerBoundsInRoot by remember(item.id, currentItem.id, trackImageBoundsInRoot) {
        mutableStateOf<Rect?>(null)
    }
    var renderedPhotoIntrinsicSize by remember(item.id, currentItem.id) { mutableStateOf<IntSize?>(null) }
    val resolvedMediaWidth = renderedPhotoIntrinsicSize
        ?.width
        ?.takeIf { it > 0 }
        ?: fallbackMediaWidth
    val resolvedMediaHeight = renderedPhotoIntrinsicSize
        ?.height
        ?.takeIf { it > 0 }
        ?: fallbackMediaHeight
    val reportDecodedSize: (Int, Int) -> Unit = { width, height ->
        if (width > 0 && height > 0) {
            currentOnMediaAspectRatioResolved.value?.invoke(width.toFloat() / height.toFloat())
        }
    }

    LaunchedEffect(
        trackImageBoundsInRoot,
        item.id,
        item.type,
        isLivePhotoCard,
        mediaContainerBoundsInRoot,
        resolvedMediaWidth,
        resolvedMediaHeight
    ) {
        if (!trackImageBoundsInRoot) return@LaunchedEffect
        val containerBounds = mediaContainerBoundsInRoot ?: return@LaunchedEffect
        val imageBounds = if (item.type == "photo" || isLivePhotoCard) {
            resolveHomeCardPhotoBoundsInRoot(
                containerBoundsInRoot = containerBounds,
                mediaWidth = resolvedMediaWidth,
                mediaHeight = resolvedMediaHeight
            ) ?: containerBounds
        } else {
            containerBounds
        }
        currentOnImageBoundsInRootChanged.value(imageBounds)
    }

    val mediaBlock: @Composable (Modifier) -> Unit = { mediaModifier ->
        Box(
            modifier = mediaModifier
                .homeCardImageLongPressHoverGesture(
                    enabled = imageLongPressHoverEnabled,
                    onHoverActiveChange = onImageLongPressHoverChange
                )
                .onGloballyPositioned { coordinates ->
                    if (trackImageBoundsInRoot) {
                        mediaContainerBoundsInRoot = layoutBoundsInRootUnclipped(coordinates)
                    }
                }
                .background(mediaPlaceholderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = longPressOverlaySourceImageAlpha.coerceIn(0f, 1f)
                    }
            ) {
                if (item.type == "video") {
                    if (shouldRenderVideoPlayer) {
                        if (photoFrameMode && HomeCardPhotoAspectCache.get(item.id) == null) {
                            // Poster under the player: resolves the true frame aspect for the photo rect.
                            AsyncImage(
                                model = videoFrameRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onSuccess = { state ->
                                    reportDecodedSize(
                                        state.result.drawable.intrinsicWidth,
                                        state.result.drawable.intrinsicHeight
                                    )
                                }
                            )
                        }
                        VideoPlayer(
                            item = item,
                            isMuted = isMuted,
                            shouldPlay = shouldPlayVideo,
                            repeatOne = true,
                            isCardDragging = isCardDragging,
                            freezeFrame = isCardDragging,
                            pauseToken = dragPauseToken,
                            onStartupSample = onStartupSample,
                            onSeekStateChange = onVideoSeekingChange,
                            onFullScreen = { player -> onOpenFullScreenVideo?.invoke(item, player) },
                            isFullScreenOpen = isFullScreenVideoOpen
                        )

                        if (shouldPlayVideo) {
                            CardMuteToggleButton(
                                isMuted = isMuted,
                                isDark = isDark,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .clip(CircleShape),
                                onToggle = { onMutedChange(!isMuted) }
                            )
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(mediaPlaceholderColor)
                        ) {
                            AsyncImage(
                                model = videoFrameRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onSuccess = { state ->
                                    reportDecodedSize(
                                        state.result.drawable.intrinsicWidth,
                                        state.result.drawable.intrinsicHeight
                                    )
                                }
                            )
                            Icon(
                                imageVector = Icons.Outlined.PlayCircleOutline,
                                contentDescription = commonUiCopy.video,
                                tint = if (photoFrameMode) {
                                    Color.White.copy(alpha = 0.85f)
                                } else if (isDark) {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                } else {
                                    Color.Black.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                } else if (isLivePhotoCard) {
                    val shouldRenderLiveVideo =
                        livePlaybackArmed &&
                            !livePlaybackCompleted &&
                            (shouldPlayVideo || isCardDragging)
                    val shouldPlayLiveVideo = shouldPlayVideo && livePlaybackArmed && !livePlaybackCompleted
                    BoxWithConstraints(
                        Modifier
                            .fillMaxSize()
                            .background(mediaPlaceholderColor)
                    ) {
                        val livePhotoLayout = remember(
                            constraints.maxWidth,
                            constraints.maxHeight,
                            resolvedMediaWidth,
                            resolvedMediaHeight
                        ) {
                            resolveHomeCardPhotoLayout(
                                containerWidthPx = constraints.maxWidth.toFloat(),
                                containerHeightPx = constraints.maxHeight.toFloat(),
                                mediaWidth = resolvedMediaWidth,
                                mediaHeight = resolvedMediaHeight
                            )
                        }
                        val liveMediaModifier = if (livePhotoLayout != null) {
                            with(density) {
                                Modifier
                                    .size(
                                        width = livePhotoLayout.imageBoundsInContainer.width.toDp(),
                                        height = livePhotoLayout.imageBoundsInContainer.height.toDp()
                                    )
                                    .align(livePhotoLayout.alignment)
                            }
                        } else {
                            Modifier.fillMaxSize()
                        }
                        AsyncImage(
                            model = stillImageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center,
                            modifier = liveMediaModifier,
                            onSuccess = { state ->
                                val intrinsicWidth = state.result.drawable.intrinsicWidth
                                val intrinsicHeight = state.result.drawable.intrinsicHeight
                                val nextSize = if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                                    IntSize(intrinsicWidth, intrinsicHeight)
                                } else {
                                    null
                                }
                                if (renderedPhotoIntrinsicSize != nextSize) {
                                    renderedPhotoIntrinsicSize = nextSize
                                }
                                reportDecodedSize(intrinsicWidth, intrinsicHeight)
                            },
                            onError = {
                                if (renderedPhotoIntrinsicSize != null) {
                                    renderedPhotoIntrinsicSize = null
                                }
                            }
                        )

                        if (shouldRenderLiveVideo) {
                            VideoPlayer(
                                item = livePlaybackItem,
                                isMuted = isMuted,
                                modifier = liveMediaModifier,
                                shouldPlay = shouldPlayLiveVideo,
                                repeatOne = false,
                                isCardDragging = isCardDragging,
                                freezeFrame = isCardDragging,
                                pauseToken = dragPauseToken,
                                controlsEnabled = !isCardDragging,
                                onStartupSample = onStartupSample,
                                playerResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                                onSeekStateChange = onVideoSeekingChange,
                                onFullScreen = { player -> onOpenFullScreenVideo?.invoke(item, player) },
                                isFullScreenOpen = isFullScreenVideoOpen,
                                replayToken = liveReplayToken,
                                onPlaybackEnded = { onLivePlaybackCompletedChange(true) }
                            )
                        }

                        if (!photoFrameMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onLiveReplayRequest() }
                                    .background(
                                        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black
                                )
                            }
                        }

                        if (shouldPlayLiveVideo) {
                            // Photo-frame mode keeps the LIVE tag top-end, so the live mute moves top-start.
                            CardMuteToggleButton(
                                isMuted = isMuted,
                                isDark = isDark,
                                modifier = Modifier
                                    .align(if (photoFrameMode) Alignment.TopStart else Alignment.TopEnd)
                                    .padding(16.dp),
                                onToggle = { onMutedChange(!isMuted) }
                            )
                        }
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(mediaPlaceholderColor)
                    ) {
                        val photoLayout = remember(
                            constraints.maxWidth,
                            constraints.maxHeight,
                            resolvedMediaWidth,
                            resolvedMediaHeight
                        ) {
                            resolveHomeCardPhotoLayout(
                                containerWidthPx = constraints.maxWidth.toFloat(),
                                containerHeightPx = constraints.maxHeight.toFloat(),
                                mediaWidth = resolvedMediaWidth,
                                mediaHeight = resolvedMediaHeight
                            )
                        }

                        AsyncImage(
                            model = stillImageRequest,
                            contentDescription = null,
                            // The photo rect already has the photo's aspect: fill it, never letterbox.
                            contentScale = if (photoFrameMode) ContentScale.Crop else ContentScale.Fit,
                            alignment = if (photoFrameMode) {
                                Alignment.Center
                            } else {
                                photoLayout?.alignment ?: Alignment.Center
                            },
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = { state ->
                                val intrinsicWidth = state.result.drawable.intrinsicWidth
                                val intrinsicHeight = state.result.drawable.intrinsicHeight
                                val nextSize = if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                                    IntSize(intrinsicWidth, intrinsicHeight)
                                } else {
                                    null
                                }
                                if (renderedPhotoIntrinsicSize != nextSize) {
                                    renderedPhotoIntrinsicSize = nextSize
                                }
                                reportDecodedSize(intrinsicWidth, intrinsicHeight)
                            },
                            onError = {
                                if (renderedPhotoIntrinsicSize != null) {
                                    renderedPhotoIntrinsicSize = null
                                }
                            }
                        )

                        val badgeText = if (photoFrameMode) null else getFormatBadgeText(item)
                        if (badgeText != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .background(
                                        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black
                                )
                            }
                        }
                    }
                }
            }

            if (forceImagePlaceholder) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(mediaPlaceholderColor)
                )
            }
        }
    }

    if (photoFrameMode) {
        Box(Modifier.fillMaxSize()) {
            mediaBlock(Modifier.fillMaxSize())
            if (!forceImagePlaceholder) {
                DraggableCardPhotoFrameOverlays(
                    item = item,
                    isLivePhotoCard = isLivePhotoCard,
                    isDark = isDark,
                    overlayAlpha = longPressOverlaySourceImageAlpha,
                    mlLabels = mlLabels,
                    showMlLabels = showMlLabelsOnHome,
                    infoContentDescription = commonUiCopy.info,
                    aiContentDescription = commonUiCopy.ai,
                    onLiveReplayRequest = onLiveReplayRequest,
                    onExpandDetails = onExpandDetails
                )
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        mediaBlock(Modifier.weight(1f).fillMaxWidth())

        val date = Date(item.dateAdded * 1000)
        val format = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val footerTextStyle = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
        val footerBaseColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
        val footerScrimColor = if (isDark) {
            Color.Black.copy(alpha = imageLongPressHoverVisuals.footerScrimAlpha)
        } else {
            Color.White.copy(alpha = imageLongPressHoverVisuals.footerScrimAlpha * 1.1f)
        }
        val footerHeight = if (compactFooter) 72.dp else 84.dp
        val footerHorizontalPadding = if (compactFooter) 18.dp else 20.dp
        val footerSpacing = if (compactFooter) 2.dp else 4.dp
        val footerMetaFontSize = if (compactFooter) 11.sp else 12.sp
        val footerMetaLetterSpacing = if (compactFooter) 0.8.sp else 1.sp
        val footerIconSize = if (compactFooter) 11.dp else 12.dp
        val footerInfoButtonSize = if (compactFooter) 28.dp else 30.dp
        val footerInfoIconSize = if (compactFooter) 15.dp else 16.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(footerHeight)
                .background(footerBaseColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .homeCardFooterHoverEffect(imageLongPressHoverVisuals)
                    .padding(horizontal = footerHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(footerSpacing, Alignment.CenterVertically),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 12.dp)
                ) {
                    val resolvedSecondaryTextColor = if (footerSecondaryTextColor != Color.Unspecified) {
                        footerSecondaryTextColor
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
                    }
                    Text(
                        text = format.format(date),
                        fontFamily = FontFamily.Monospace,
                        fontSize = footerMetaFontSize,
                        lineHeight = footerMetaFontSize,
                        letterSpacing = footerMetaLetterSpacing,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                        style = footerTextStyle
                    )
                    if (!footerSecondaryText.isNullOrBlank()) {
                        Text(
                            text = footerSecondaryText,
                            fontSize = footerMetaFontSize,
                            lineHeight = footerMetaFontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = resolvedSecondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            style = footerTextStyle
                        )
                    } else if (currentItem.location != "Unknown Location") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = commonUiCopy.location,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.size(footerIconSize)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentItem.location,
                                fontSize = footerMetaFontSize,
                                lineHeight = footerMetaFontSize,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                maxLines = 1,
                                softWrap = false,
                                modifier = if (enableFooterMarquee) {
                                    Modifier
                                        .weight(1f)
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                            spacing = MarqueeSpacing(24.dp),
                                            velocity = 28.dp
                                        )
                                } else {
                                    Modifier.weight(1f)
                                },
                                style = footerTextStyle
                            )
                        }
                    }
                    if (showMlLabelsOnHome && mlLabels.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = commonUiCopy.ai,
                                tint = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color(0xFF8B5CF6).copy(alpha = 0.8f),
                                modifier = Modifier.size(footerIconSize)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mlLabels.joinToString(" / "),
                                fontSize = footerMetaFontSize,
                                lineHeight = footerMetaFontSize,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color(0xFF8B5CF6).copy(alpha = 0.8f),
                                maxLines = 1,
                                softWrap = false,
                                modifier = if (enableFooterMarquee) {
                                    Modifier
                                        .weight(1f)
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                            spacing = MarqueeSpacing(24.dp),
                                            velocity = 28.dp
                                        )
                                } else {
                                    Modifier.weight(1f)
                                },
                                style = footerTextStyle
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onExpandDetails,
                    modifier = Modifier.size(footerInfoButtonSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.08f else 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = commonUiCopy.info,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(footerInfoIconSize)
                    )
                }
            }

            if (imageLongPressHoverVisuals.footerScrimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(footerScrimColor)
                )
            }
        }
    }
}

/** Badges, info button and ML chip laid directly on the photo (no footer in photo-frame mode). */
@Composable
private fun BoxScope.DraggableCardPhotoFrameOverlays(
    item: MediaItem,
    isLivePhotoCard: Boolean,
    isDark: Boolean,
    overlayAlpha: Float,
    mlLabels: List<String>,
    showMlLabels: Boolean,
    infoContentDescription: String,
    aiContentDescription: String,
    onLiveReplayRequest: () -> Unit,
    onExpandDetails: () -> Unit
) {
    val isVideo = item.type == "video"
    val alphaModifier = Modifier.graphicsLayer { alpha = overlayAlpha.coerceIn(0f, 1f) }
    val formatBadgeText = if (isVideo || isLivePhotoCard) null else getFormatBadgeText(item)

    if (!isVideo && isLivePhotoCard) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .then(alphaModifier)
                .padding(top = 7.dp, end = 7.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onLiveReplayRequest() }
                .padding(9.dp)
        ) {
            CardMediaTypeBadge(title = "LIVE", isDark = isDark, showLiveIcon = true)
        }
    } else if (formatBadgeText != null) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .then(alphaModifier)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            CardMediaTypeBadge(title = formatBadgeText, isDark = isDark)
        }
    }

    if (showMlLabels && mlLabels.isNotEmpty() && !isVideo) {
        val chipShape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .then(alphaModifier)
                .padding(start = 12.dp, end = 58.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.32f), chipShape)
                    .border(0.5.dp, Color.White.copy(alpha = 0.22f), chipShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = aiContentDescription,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = mlLabels.joinToString(" / "),
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }
        }
    }

    // Video controls own the bottom edge and the top-end corner, so its info button sits top-start.
    CardPhotoInfoButton(
        contentDescription = infoContentDescription,
        modifier = Modifier
            .align(if (isVideo) Alignment.TopStart else Alignment.BottomEnd)
            .then(alphaModifier),
        onClick = onExpandDetails
    )
}
