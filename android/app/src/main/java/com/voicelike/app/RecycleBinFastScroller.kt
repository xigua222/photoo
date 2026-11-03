package com.voicelike.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun RecycleBinFastScroller(
    gridState: LazyGridState,
    totalItems: Int,
    rows: Int,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    
    // Calculate scroll progress (0f to 1f)
    // We use the first visible item index to estimate progress
    val firstVisibleItemIndex by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
    val layoutInfo by remember { derivedStateOf { gridState.layoutInfo } }
    
    // Total rows estimation
    // If rows passed is 0, avoid division by zero
    val safeRows = max(1, rows)
    
    // Current row index
    val currentRow = firstVisibleItemIndex / 3 // Assuming 3 columns
    
    // Viewport height in items (approximate)
    val viewportHeightItems = layoutInfo.visibleItemsInfo.size
    val viewportRows = (viewportHeightItems + 2) / 3
    
    // Max scrollable rows
    val maxScrollRows = max(0, safeRows - viewportRows)
    
    // Current progress
    val progress = if (maxScrollRows > 0) {
        (currentRow.toFloat() / maxScrollRows).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Animation for thumb visibility/alpha
    val alpha by animateFloatAsState(
        targetValue = if (isDragging || gridState.isScrollInProgress) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ScrollThumbAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .padding(vertical = 8.dp)
            // Handle Dragging
            .pointerInput(totalItems, safeRows) {
                detectVerticalDragGestures(
                    onDragStart = { _ -> isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    
                    // Calculate new progress based on drag
                    // We need the height of the track
                    val trackHeight = size.height.toFloat()
                    if (trackHeight > 0) {
                        val dragPercent = dragAmount / trackHeight
                        val currentScrollOffset = progress
                        val newProgress = (currentScrollOffset + dragPercent).coerceIn(0f, 1f)
                        
                        // Scroll to new position
                        val targetRow = (newProgress * maxScrollRows).toInt()
                        val targetIndex = (targetRow * 3).coerceIn(0, totalItems - 1)
                        
                        scope.launch {
                            gridState.scrollToItem(targetIndex)
                        }
                    }
                }
            }
    ) {
        // Scroll Thumb
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val thumbHeight = 48.dp
            
            val bias = (progress * 2) - 1
            
            Box(
                modifier = Modifier
                    .align(BiasAlignment(1f, bias))
                    .width(4.dp)
                    .height(thumbHeight)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            )
        }
    }
}
