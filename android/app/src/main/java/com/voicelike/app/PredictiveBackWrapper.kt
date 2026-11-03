package com.voicelike.app

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlin.math.max
import kotlin.math.min

/**
 * A wrapper that handles the predictive back gesture.
 * It scales down the content based on the back gesture progress.
 * 
 * @param isVisible Whether the content is currently visible (driven by parent state).
 * @param onBack Callback when the back gesture is committed (completed).
 * @param content The composable content to wrap.
 */
@Composable
fun PredictiveBackWrapper(
    isVisible: Boolean,
    enableCustomPredictive: Boolean = true,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    // Progress of the back gesture (0f to 1f)
    var progress by remember { mutableFloatStateOf(0f) }
    
    // Whether the gesture is currently active
    var isBackGestureActive by remember { mutableStateOf(false) }

    // Use PredictiveBackHandler to intercept back events and drive animation
    if (enableCustomPredictive) {
        PredictiveBackHandler(enabled = isVisible) { progressFlow ->
            isBackGestureActive = true
            try {
                progressFlow.collect { event ->
                    progress = event.progress
                }
                // Gesture Completed (Committed)
                onBack()
            } catch (e: Exception) {
                // Gesture Cancelled
            } finally {
                isBackGestureActive = false
                progress = 0f
            }
        }
    }
    // When custom predictive handling is disabled (e.g. Android 14+ root pages),
    // still intercept back so callers can run their own navigation logic.
    BackHandler(enabled = isVisible && !enableCustomPredictive) {
        onBack()
    }

    // Apply transformation
    // We want the content to scale down slightly and maybe slide a bit
    val scale = if (enableCustomPredictive && isBackGestureActive) {
        // Map progress 0..1 to scale 1..0.9
        lerp(1f, 0.9f, progress)
    } else {
        1f
    }
    
    // Standard predictive back does NOT fade out the content
    val alpha = 1f
    
    val translationX = if (enableCustomPredictive && isBackGestureActive) {
        // Slight slide to right
        lerp(0f, 48f, progress) // Reduced translation to match system feel
    } else {
        0f
    }

    // Calculate dynamic corner radius
    // Standard Android behavior transitions from 0dp to around 32dp (or system dialog corner radius)
    val cornerRadius = if (enableCustomPredictive && isBackGestureActive) {
        lerp(0f, 32f, progress) // Interpolate to 32dp
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                this.translationX = translationX
                
                // Apply dynamic corner radius using graphicsLayer clip
                this.shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp)
                this.clip = true
            }
    ) {
        content()
    }
}
