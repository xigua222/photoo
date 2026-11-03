package com.voicelike.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class SimilarSplitCompareSession(
    val items: List<MediaItem>
)

@Composable
fun SimilarSplitCompareView(
    items: List<MediaItem>,
    selectedItemId: Long?,
    onSelectedItemChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onKeepSelected: () -> Unit,
    onKeepAll: () -> Unit,
    onDeleteAll: () -> Unit
) {
    if (items.size < 2) return

    BackHandler(onBack = onDismiss)

    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val secondaryButtonBorderColor = Color.White.copy(alpha = 0.24f)
    val itemKey = remember(items) { items.take(2).joinToString(separator = "-") { it.id.toString() } }
    var sharedScale by remember(itemKey) { mutableFloatStateOf(1f) }
    var sharedOffset by remember(itemKey) { mutableStateOf(Offset.Zero) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "全屏对比",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.take(2).forEachIndexed { index, item ->
                    SimilarSplitComparePane(
                        item = item,
                        isSelected = selectedItemId == item.id,
                        sharedScale = sharedScale,
                        sharedOffset = sharedOffset,
                        onTransformChange = { scale, offset ->
                            sharedScale = scale
                            sharedOffset = offset
                        },
                        onSelect = { onSelectedItemChange(item.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onKeepSelected,
                    enabled = selectedItemId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        contentColor = if (isDark) Color.Black else Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(strings.keepSelected, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onKeepAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .advancedTextureButton(shape = RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, secondaryButtonBorderColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(strings.keepAll, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onDeleteAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7F1D1D),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .advancedTextureButton(shape = RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.deleteAll, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarSplitComparePane(
    item: MediaItem,
    isSelected: Boolean,
    sharedScale: Float,
    sharedOffset: Offset,
    onTransformChange: (Float, Offset) -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    var zoomAnimationJob by remember(item.id) { mutableStateOf<Job?>(null) }
    val currentSharedScaleState = rememberUpdatedState(sharedScale)
    val currentSharedOffsetState = rememberUpdatedState(sharedOffset)
    val currentOnTransformChangeState = rememberUpdatedState(onTransformChange)
    val currentOnSelectState = rememberUpdatedState(onSelect)

    DisposableEffect(item.id) {
        onDispose {
            zoomAnimationJob?.cancel()
        }
    }

    val stopZoomAnimation: () -> Unit = {
        zoomAnimationJob?.cancel()
        zoomAnimationJob = null
    }
    val animateZoomTo: (Float, Offset) -> Unit = { targetScale, targetOffset ->
        stopZoomAnimation()
        zoomAnimationJob = scope.launch {
            val startScale = currentSharedScaleState.value
            val startOffset = currentSharedOffsetState.value
            val progress = Animatable(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) {
                currentOnTransformChangeState.value(
                    compareLerpFloat(startScale, targetScale, value),
                    Offset(
                        x = compareLerpFloat(startOffset.x, targetOffset.x, value),
                        y = compareLerpFloat(startOffset.y, targetOffset.y, value)
                    )
                )
            }
            currentOnTransformChangeState.value(targetScale, targetOffset)
            zoomAnimationJob = null
        }
    }
    val currentAnimateZoomToState = rememberUpdatedState(animateZoomTo)
    val paneBorderColor = when {
        isSelected -> Color(0xFF60A5FA)
        else -> Color.White.copy(alpha = 0.24f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0E0E11),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = paneBorderColor
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0E0E11))
        ) {
            val containerSize = IntSize(constraints.maxWidth, constraints.maxHeight)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTapGestures(
                            onPress = {
                                if (tryAwaitRelease()) {
                                    currentOnSelectState.value()
                                }
                            },
                            onDoubleTap = { tapOffset ->
                                val mediaRect = compareFittedContentRect(
                                    containerSize = containerSize,
                                    mediaWidth = item.width,
                                    mediaHeight = item.height
                                )
                                if (!mediaRect.contains(tapOffset)) return@detectTapGestures
                                val currentScale = currentSharedScaleState.value
                                val currentOffset = currentSharedOffsetState.value
                                if (currentScale > 1.05f) {
                                    currentAnimateZoomToState.value(1f, Offset.Zero)
                                } else {
                                    val targetScale = 2.5f
                                    val center = Offset(
                                        x = constraints.maxWidth / 2f,
                                        y = constraints.maxHeight / 2f
                                    )
                                    val rawTargetOffset = Offset(
                                        x = currentOffset.x + (currentScale - targetScale) * (tapOffset.x - center.x),
                                        y = currentOffset.y + (currentScale - targetScale) * (tapOffset.y - center.y)
                                    )
                                    val targetOffset = compareClampPanOffset(
                                        offset = rawTargetOffset,
                                        scale = targetScale,
                                        containerWidth = constraints.maxWidth,
                                        containerHeight = constraints.maxHeight
                                    )
                                    currentAnimateZoomToState.value(targetScale, targetOffset)
                                }
                            }
                        )
                    }
                    .pointerInput(item.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            stopZoomAnimation()
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount > 1) {
                                    val currentScale = currentSharedScaleState.value
                                    val currentOffset = currentSharedOffsetState.value
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                                    val nextOffset = if (newScale > 1f) {
                                        compareClampPanOffset(
                                            offset = Offset(
                                                x = currentOffset.x + pan.x,
                                                y = currentOffset.y + pan.y
                                            ),
                                            scale = newScale,
                                            containerWidth = constraints.maxWidth,
                                            containerHeight = constraints.maxHeight
                                        )
                                    } else {
                                        Offset.Zero
                                    }
                                    currentOnTransformChangeState.value(newScale, nextOffset)
                                    event.changes.forEach { change ->
                                        if (change.positionChange() != Offset.Zero) {
                                            change.consumeAllChanges()
                                        }
                                    }
                                } else if (pressedCount == 1 && currentSharedScaleState.value > 1f) {
                                    val currentScale = currentSharedScaleState.value
                                    val currentOffset = currentSharedOffsetState.value
                                    val change = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                    val pan = change.positionChange()
                                    if (pan != Offset.Zero) {
                                        val next = compareClampPanOffset(
                                            offset = Offset(
                                                x = currentOffset.x + pan.x,
                                                y = currentOffset.y + pan.y
                                            ),
                                            scale = currentScale,
                                            containerWidth = constraints.maxWidth,
                                            containerHeight = constraints.maxHeight
                                        )
                                        val movedX = kotlin.math.abs(next.x - currentOffset.x)
                                        val movedY = kotlin.math.abs(next.y - currentOffset.y)
                                        currentOnTransformChangeState.value(currentScale, next)
                                        if (movedX > 0.5f || movedY > 0.5f) {
                                            change.consumeAllChanges()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = sharedScale
                            scaleY = sharedScale
                            translationX = sharedOffset.x
                            translationY = sharedOffset.y
                        }
                )

                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2563EB),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "已选择",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun compareLerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + ((stop - start) * fraction)
}

private fun compareClampPanOffset(
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

private fun compareFittedContentRect(
    containerSize: IntSize,
    mediaWidth: Int,
    mediaHeight: Int
): Rect {
    val containerWidth = containerSize.width.toFloat().coerceAtLeast(1f)
    val containerHeight = containerSize.height.toFloat().coerceAtLeast(1f)
    val safeWidth = mediaWidth.coerceAtLeast(1).toFloat()
    val safeHeight = mediaHeight.coerceAtLeast(1).toFloat()
    val mediaAspect = safeWidth / safeHeight
    val containerAspect = containerWidth / containerHeight
    val drawWidth: Float
    val drawHeight: Float
    if (mediaAspect > containerAspect) {
        drawWidth = containerWidth
        drawHeight = containerWidth / mediaAspect
    } else {
        drawHeight = containerHeight
        drawWidth = containerHeight * mediaAspect
    }
    val left = (containerWidth - drawWidth) / 2f
    val top = (containerHeight - drawHeight) / 2f
    return Rect(left, top, left + drawWidth, top + drawHeight)
}
