package com.voicelike.app.smart

import android.graphics.Color as AndroidColor
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import kotlinx.coroutines.delay

@Composable
fun SmartAiTypingOverlay(
    fullText: String?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (fullText.isNullOrBlank()) return
    val finalText = remember(fullText) { ensureSentenceEnding(fullText) }

    var typedCount by remember(finalText) { mutableStateOf(0) }
    LaunchedEffect(finalText, visible) {
        if (!visible) return@LaunchedEffect
        typedCount = 0
        delay(320L) // Let the assistant animation lead slightly before typing.
        while (typedCount < finalText.length && visible) {
            typedCount += 1
            delay(24L)
        }
    }

    val shownText = finalText.take(typedCount)
    val cursorTransition = rememberInfiniteTransition(label = "smart_ai_cursor")
    val cursorBlinkTyping by cursorTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smart_ai_cursor_alpha_typing"
    )
    val cursorBlink = if (typedCount >= finalText.length) 0f else cursorBlinkTyping

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            animationSpec = tween(220),
            initialOffsetY = { it / 3 }
        ),
        exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
            animationSpec = tween(220),
            targetOffsetY = { -it / 4 }
        )
    ) {
        Box(
            modifier = modifier
                .wrapContentWidth(unbounded = true)
                .offset(y = (-5).dp)
                .padding(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier.wrapContentWidth(unbounded = true),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 52.dp)
                        .offset(x = (-3).dp, y = (-4).dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier
                            .size(52.dp)
                            .padding(1.dp),
                        factory = { context ->
                            LottieAnimationView(context).apply {
                                setAnimation("sparkles loops.json")
                                repeatCount = LottieDrawable.INFINITE
                                repeatMode = LottieDrawable.RESTART
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                setRenderMode(RenderMode.SOFTWARE)
                                setSafeMode(true)
                                clipToOutline = false
                                setBackgroundColor(AndroidColor.TRANSPARENT)
                                setFailureListener {
                                    android.util.Log.e("SmartAiTypingOverlay", "Failed to load AI assistant animation", it)
                                }
                                playAnimation()
                            }
                        },
                        update = { view ->
                            if (visible) {
                                if (!view.isAnimating) {
                                    view.playAnimation()
                                }
                            } else {
                                view.pauseAnimation()
                            }
                        }
                    )
                }
                Text(
                    text = shownText + if (cursorBlink > 0.78f) "|" else "",
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .padding(start = 42.dp)
                        .offset(x = 0.dp, y = (-3).dp)
                        .wrapContentWidth(unbounded = true)
                )
            }
        }
    }
}

private fun ensureSentenceEnding(text: String): String {
    val trimmed = text.trimEnd()
    if (trimmed.isEmpty()) return trimmed
    val last = trimmed.last()
    val hasEnding = last == '.' ||
        last == '!' ||
        last == '?' ||
        last == '\u3002' ||
        last == '\uFF01' ||
        last == '\uFF1F' ||
        last == '\u2026'
    if (hasEnding) return trimmed

    val hasCjk = trimmed.any { ch ->
        (ch.code in 0x4E00..0x9FFF) || (ch.code in 0x3400..0x4DBF)
    }
    val period = if (hasCjk) "\u3002" else "."
    return trimmed + period
}
