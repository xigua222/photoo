package com.voicelike.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val alpha: Float = 1f,
    val rotation: Float = 0f,
    val rotationSpeed: Float = 0f
)

@Composable
fun CelebrationConfetti(
    intensity: Int = 100,
    triggerToken: Long = 1L,
    initialBurstProgress: Float = 0f,
    burstCenterPx: Offset? = null,
    burstCenterXFraction: Float = 0.5f,
    burstCenterYFraction: Float = 0.5f
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val particles = remember(intensity) {
        val colors = listOf(
            Color(0xFFEF4444),
            Color(0xFF3B82F6),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFF8B5CF6),
            Color(0xFFEC4899)
        )
        List(intensity) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (Math.random() * 15 + 10).toFloat() * if (intensity > 100) 1.5f else 1f
            ConfettiParticle(
                x = 0f,
                y = 0f,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed - 15).toFloat(),
                color = colors.random(),
                rotation = (Math.random() * 360).toFloat(),
                rotationSpeed = (Math.random() * 10 - 5).toFloat()
            )
        }
    }

    val time = remember { Animatable(0f) }

    LaunchedEffect(triggerToken, intensity) {
        if (triggerToken == 0L) return@LaunchedEffect
        if (hapticsAllowed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        val totalDuration = if (intensity > 100) 4000 else 2500
        val totalProgress = if (intensity > 100) 4f else 2.5f
        val startProgress = initialBurstProgress.coerceIn(0f, totalProgress)
        val remainingDuration = (
            totalDuration * (1f - (startProgress / totalProgress))
        ).roundToInt().coerceAtLeast(1)
        time.snapTo(startProgress)
        time.animateTo(
            targetValue = totalProgress,
            animationSpec = tween(
                durationMillis = remainingDuration,
                easing = LinearEasing
            )
        )
    }

    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = {
            val centerX = burstCenterPx?.x?.coerceIn(0f, size.width)
                ?: (size.width * burstCenterXFraction.coerceIn(0f, 1f))
            val centerY = burstCenterPx?.y?.coerceIn(0f, size.height)
                ?: (size.height * burstCenterYFraction.coerceIn(0f, 1f))
            val elapsed = time.value

            particles.forEach { particle ->
                val dx = particle.vx * elapsed * 40
                val dy = particle.vy * elapsed * 40 + 0.5f * 60f * elapsed * elapsed * 40
                val x = centerX + dx
                val y = centerY + dy
                val rotation = particle.rotation + particle.rotationSpeed * elapsed * 10
                val alpha = (particle.alpha - elapsed / if (intensity > 100) 4f else 2.5f).coerceIn(0f, 1f)

                if (y < size.height && alpha > 0f) {
                    rotate(rotation, pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                        drawRect(
                            color = particle.color.copy(alpha = alpha),
                            topLeft = androidx.compose.ui.geometry.Offset(x - 8f, y - 4f),
                            size = androidx.compose.ui.geometry.Size(16f, 8f)
                        )
                    }
                }
            }
        }
    )
}
