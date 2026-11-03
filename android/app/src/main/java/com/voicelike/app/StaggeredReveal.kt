package com.voicelike.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun staggeredRevealModifier(
    index: Int,
    key: Any? = null, // External key to control state persistence (e.g. pageId)
    baseDelayMs: Int = 50,
    durationMs: Int = 400,
    offsetY: Float = 100f,
    skipAnimation: Boolean = false
): Modifier {
    if (skipAnimation) {
        return Modifier
    }

    // Use remember instead of rememberSaveable to reset animation on recomposition (page close/open)
    val actualKey = key ?: index
    var isVisible by remember(actualKey) { mutableStateOf(false) }

    LaunchedEffect(actualKey) {
        // Reset visibility when key changes
        isVisible = false
        delay((index * baseDelayMs).toLong())
        isVisible = true
    }

    // Declarative Animation States
    // Alpha: FastOutSlowIn for quick appearance
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = (durationMs * 0.8).toInt(), easing = FastOutSlowInEasing),
        label = "staggered_alpha"
    )

    // Offset: Spring physics for natural movement
    val offset by animateFloatAsState(
        targetValue = if (isVisible) 0f else offsetY,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow, // Softer spring
            dampingRatio = 0.8f // Slight bounce, not too much
        ),
        label = "staggered_offset"
    )

    // Scale: Grow from slightly smaller
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = 0.7f
        ),
        label = "staggered_scale"
    )

    return Modifier.graphicsLayer {
        this.alpha = alpha
        this.translationY = offset
        this.scaleX = scale
        this.scaleY = scale
    }
}
