package com.voicelike.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun FullScreenDayContextDrawer(
    currentItem: MediaItem,
    contextItems: List<MediaItem>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDragActiveChange: (Boolean) -> Unit = {},
    onSelectItem: (MediaItem) -> Unit,
    dragEnabled: Boolean,
    onJumpToDay: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    anchorItem: MediaItem = currentItem
) {
    if (contextItems.size <= 1) return

    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val commonUiCopy = remember(appLanguage) { getCommonGlobalUiCopy(appLanguage) }
    val drawerShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    val dateLabel = remember(currentItem.dateAdded) {
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(currentItem.dateAdded * 1000L))
    }
    val dayKey = remember(currentItem.dateAdded) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentItem.dateAdded * 1000L))
    }
    val displayItems = remember(contextItems) {
        contextItems.distinctBy { it.id }
    }
    val indexById = remember(displayItems) {
        displayItems.mapIndexed { index, mediaItem -> mediaItem.id to index }.toMap()
    }
    val anchorIndex = remember(anchorItem.id, indexById) {
        indexById[anchorItem.id] ?: -1
    }
    val scope = rememberCoroutineScope()
    val expandedContentHeight = 212.dp
    val returnButtonSlotHeight = 44.dp
    val returnButtonVerticalShift = (-8).dp
    val maxBodyHeight by animateDpAsState(
        targetValue = if (expanded) expandedContentHeight else 0.dp,
        label = "full_screen_day_context_height"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(expanded, dragEnabled) {
                if (!dragEnabled) return@pointerInput
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        onDragActiveChange(true)
                    },
                    onDragCancel = {
                        onDragActiveChange(false)
                    },
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        onDragActiveChange(false)
                        if (abs(totalDrag) < 18f) {
                            onExpandedChange(!expanded)
                        } else if (totalDrag < 0f) {
                            onExpandedChange(true)
                        } else {
                            onExpandedChange(false)
                        }
                    }
                )
            },
        shape = drawerShape,
        color = Color.Black.copy(alpha = 0.72f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.48f))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.contextDrawerDateDashedUnderline(
                        enabled = currentItem.id != anchorItem.id,
                        color = Color.White.copy(alpha = 0.56f)
                    )
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = onJumpToDay != null,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onJumpToDay?.invoke(dayKey)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    TimeMachineBenchIcon(
                        modifier = Modifier.size(22.dp),
                        tint = Color.White.copy(alpha = 0.92f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (openColorCardCameraMediaInAlbum(context, currentItem)) {
                                Toast.makeText(
                                    context,
                                    commonUiCopy.openedInAlbum,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    commonUiCopy.cannotOpenAlbum,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = commonUiCopy.openInAlbum,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxBodyHeight)
                        .padding(top = 12.dp)
                ) {
                    val listState = rememberLazyListState()
                    val itemSize = 112.dp
                    val itemSpacing = 10.dp
                    val horizontalPadding = 20.dp
                    val showReturnButton by remember(listState, anchorIndex) {
                        derivedStateOf {
                            if (anchorIndex < 0) return@derivedStateOf false
                            val visibleItems = listState.layoutInfo.visibleItemsInfo
                            if (visibleItems.isEmpty()) return@derivedStateOf false
                            visibleItems.none { it.index == anchorIndex }
                        }
                    }
                    val returnButtonAlpha by animateFloatAsState(
                        targetValue = if (showReturnButton) 1f else 0f,
                        label = "full_screen_context_return_button_alpha"
                    )

                    LaunchedEffect(expanded, anchorItem.id, anchorIndex) {
                        if (expanded && displayItems.isNotEmpty() && anchorIndex >= 0) {
                            centerItemInViewport(
                                listState = listState,
                                index = anchorIndex,
                                animated = false
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        LazyRow(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = horizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                        ) {
                            itemsIndexed(
                                items = displayItems,
                                key = { _, item -> item.id }
                            ) { _, item ->
                                val isAnchor = item.id == anchorItem.id
                                val isCurrent = item.id == currentItem.id
                                val isPreviewSelected = isCurrent && !isAnchor
                                Box(
                                    modifier = Modifier
                                        .size(itemSize)
                                        .clip(RoundedCornerShape(20.dp))
                                        .contextDrawerThumbnailBorder(
                                            isAnchor = isAnchor,
                                            isPreviewSelected = isPreviewSelected
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                if (!isCurrent) {
                                                    onSelectItem(item)
                                                }
                                            }
                                        )
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(item.uri)
                                            .crossfade(false)
                                            .size(320)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )

                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                if (isCurrent || isAnchor) {
                                                    Color.Black.copy(alpha = 0.08f)
                                                } else {
                                                    Color.Black.copy(alpha = 0.18f)
                                                }
                                            )
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(returnButtonSlotHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .alpha(returnButtonAlpha)
                                    .offset(y = returnButtonVerticalShift)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.Black.copy(alpha = 0.40f))
                                    .clickable(
                                        enabled = showReturnButton,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (anchorIndex >= 0) {
                                            onSelectItem(displayItems[anchorIndex])
                                            scope.launch {
                                                centerItemInViewport(
                                                    listState = listState,
                                                    index = anchorIndex,
                                                    animated = true
                                                )
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "回到这张",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.contextDrawerDateDashedUnderline(
    enabled: Boolean,
    color: Color
): Modifier {
    if (!enabled) return this
    return drawWithCache {
        val strokeWidth = 1.dp.toPx().coerceAtLeast(1f)
        val dashPathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
            phase = 0f
        )
        onDrawWithContent {
            drawContent()
            val y = size.height - 1.dp.toPx()
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth,
                pathEffect = dashPathEffect
            )
        }
    }
}

private suspend fun centerItemInViewport(
    listState: LazyListState,
    index: Int,
    animated: Boolean
) {
    if (index < 0) return

    if (animated) {
        listState.animateScrollToItem(index)
    } else {
        listState.scrollToItem(index)
    }

    repeat(3) {
        val layoutInfo = listState.layoutInfo
        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
        val delta = itemCenter - viewportCenter
        if (abs(delta) <= 1f) return
        if (animated) {
            listState.animateScrollBy(delta)
        } else {
            listState.scrollBy(delta)
        }
    }
}

private fun Modifier.contextDrawerThumbnailBorder(
    isAnchor: Boolean,
    isPreviewSelected: Boolean
): Modifier = drawWithCache {
    val isHighlighted = isAnchor || isPreviewSelected
    val strokeWidth = if (isHighlighted) 1.5.dp.toPx() else 0.75.dp.toPx()
    val borderColor = if (isHighlighted) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.18f)
    val dashPathEffect = if (isPreviewSelected) {
        PathEffect.dashPathEffect(
            intervals = floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
            phase = 0f
        )
    } else {
        null
    }
    val inset = strokeWidth / 2f
    val cornerRadius = 20.dp.toPx()

    onDrawWithContent {
        drawContent()
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(inset, inset),
            size = Size(
                width = size.width - strokeWidth,
                height = size.height - strokeWidth
            ),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth, pathEffect = dashPathEffect)
        )
    }
}

internal fun buildSameDayPhotoContext(
    anchor: MediaItem,
    allMedia: List<MediaItem>
): List<MediaItem> {
    return buildFullScreenPhotoContext(
        anchor = anchor,
        allMedia = allMedia
    )
}

internal fun buildFullScreenPhotoTimeline(
    allMedia: List<MediaItem>
): List<MediaItem> {
    if (allMedia.isEmpty()) return emptyList()

    return allMedia.asSequence()
        .filter { it.type == "photo" }
        .distinctBy { it.id }
        .sortedBy { it.dateAdded }
        .toList()
}

internal fun buildFullScreenPhotoContext(
    anchor: MediaItem,
    allMedia: List<MediaItem>
): List<MediaItem> {
    val timeline = buildFullScreenPhotoTimeline(allMedia).toMutableList()
    if (anchor.type == "photo" && timeline.none { it.id == anchor.id }) {
        timeline.add(anchor)
        timeline.sortBy { it.dateAdded }
    }
    return timeline
}
