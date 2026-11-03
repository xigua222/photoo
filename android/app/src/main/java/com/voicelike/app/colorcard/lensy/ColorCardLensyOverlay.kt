package com.voicelike.app.colorcard.lensy

import android.graphics.Color as AndroidColor
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.RenderMode

private val ColorCardLensyBubbleColor = androidx.compose.ui.graphics.Color(0xFFF6F2EA)
private val ColorCardLensyTextColor = androidx.compose.ui.graphics.Color(0xFF241C14)

@Composable
internal fun ColorCardLensyOverlay(
    message: String?,
    onDismissRequest: (() -> Unit)? = null,
    textMaxWidth: Dp = 260.dp,
    modifier: Modifier = Modifier
) {
    val visible = !message.isNullOrBlank()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 3 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 3 }),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AndroidView(
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = (-1).dp, y = (-2).dp),
                factory = { context ->
                    LottieAnimationView(context).apply {
                        setAnimation("sparkles loops.json")
                        repeatCount = LottieDrawable.INFINITE
                        repeatMode = LottieDrawable.RESTART
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setRenderMode(RenderMode.SOFTWARE)
                        setSafeMode(true)
                        setBackgroundColor(AndroidColor.TRANSPARENT)
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
            Text(
                text = message.orEmpty(),
                color = ColorCardLensyTextColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = textMaxWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorCardLensyBubbleColor.copy(alpha = 0.94f))
                    .clickable(
                        enabled = visible && onDismissRequest != null,
                        onClick = { onDismissRequest?.invoke() }
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}
