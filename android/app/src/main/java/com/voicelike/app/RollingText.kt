package com.voicelike.app

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle

/**
 * A Text composable that animates its value from 0 to targetValue.
 * Triggers haptic feedback during animation.
 */
@Composable
fun RollingText(
    targetValue: Number,
    format: (Number) -> String = { it.toString() },
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMillis: Int = 1500,
    shouldAnimate: Boolean = true,
    stopAnimation: Boolean = false,
    enableHaptics: Boolean = true
) {
    if (!shouldAnimate || stopAnimation) {
        Text(
            text = format(targetValue),
            style = style,
            color = color,
            modifier = modifier
        )
        return
    }

    val view = LocalView.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val animatedValue = remember { Animatable(0f) }
    val targetFloat = targetValue.toFloat()
    
    // Dynamic duration based on value magnitude to avoid long vibration for small numbers
    val dynamicDuration = remember(targetFloat, durationMillis) {
        if (targetFloat < 50) {
            // Map 0-50 range to 300ms-1000ms
            (targetFloat * 20).toInt().coerceIn(300, 1000)
        } else {
            durationMillis
        }
    }
    
    // Reset animation if target changes significantly or on first composition
    LaunchedEffect(targetFloat) {
        animatedValue.snapTo(0f)
        animatedValue.animateTo(
            targetValue = targetFloat,
            animationSpec = tween(durationMillis = dynamicDuration, easing = FastOutSlowInEasing)
        )
    }

    // Haptic feedback logic
    var lastHapticTime by remember { mutableLongStateOf(0L) }
    val currentFloat = animatedValue.value
    
    // Only trigger haptics if we are actually animating (value != target) AND not stopped AND haptics enabled
    val isAnimating = currentFloat != targetFloat && enableHaptics && hapticsAllowed

    LaunchedEffect(currentFloat) {
        if (isAnimating) {
            val now = System.currentTimeMillis()
            // Throttle: Max 1 vibration every 60ms (approx 16fps)
            // This prevents "buzzing" and keeps distinct ticks
            if (now - lastHapticTime >= 60) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                lastHapticTime = now
            }
        }
    }

    Text(
        text = format(currentFloat),
        style = style,
        color = color,
        modifier = modifier
    )
}
