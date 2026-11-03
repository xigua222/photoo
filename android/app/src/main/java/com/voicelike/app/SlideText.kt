package com.voicelike.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A Text composable that animates its value like a typewriter / odometer.
 * It slides up/down to the new number without haptic feedback.
 */
@Composable
fun SlideText(
    targetValue: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val countString = targetValue.toString()
    var previousString by remember { mutableStateOf(countString) }
    
    Row(modifier = modifier) {
        countString.forEachIndexed { index, char ->
            val oldChar = previousString.getOrNull(index) ?: char
            val direction = if (char > oldChar) 1 else -1
            
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    slideInVertically { height -> height * direction } togetherWith
                    slideOutVertically { height -> -height * direction }
                }
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style,
                    color = color
                )
            }
        }
    }
    SideEffect {
        previousString = countString
    }
}
