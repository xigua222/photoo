package com.voicelike.app.keep

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val KEEP_FAST_SCROLL_TOUCH_WIDTH = 34.dp
private val KEEP_FAST_SCROLL_THUMB_WIDTH = 14.dp
private val KEEP_FAST_SCROLL_THUMB_HEIGHT = 54.dp
private val KEEP_FAST_SCROLL_VERTICAL_PADDING = 6.dp
private val KEEP_FAST_SCROLL_END_PADDING = 3.dp

@Composable
fun KeepGalleryFastScroller(
    progress: Float,
    isScrollActive: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { KEEP_FAST_SCROLL_THUMB_HEIGHT.toPx() }

    val settledProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (isScrollActive) 70 else 120),
        label = "KeepGalleryFastScrollerProgress"
    )
    val thumbAlpha by animateFloatAsState(
        targetValue = when {
            isScrollActive -> 0.26f
            else -> 0.16f
        },
        animationSpec = tween(durationMillis = 120),
        label = "KeepGalleryFastScrollerThumbAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(KEEP_FAST_SCROLL_TOUCH_WIDTH)
            .padding(vertical = KEEP_FAST_SCROLL_VERTICAL_PADDING)
    ) {
        val availableHeightPx = (constraints.maxHeight.toFloat() - thumbHeightPx).coerceAtLeast(1f)
        val thumbOffsetY = (availableHeightPx * settledProgress).roundToInt()
        val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = thumbAlpha)

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(x = 0, y = thumbOffsetY) }
                .padding(end = KEEP_FAST_SCROLL_END_PADDING),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(KEEP_FAST_SCROLL_THUMB_WIDTH)
                    .height(KEEP_FAST_SCROLL_THUMB_HEIGHT)
                    .clip(RoundedCornerShape(999.dp))
                    .background(thumbColor)
            )
        }
    }
}
