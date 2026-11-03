package com.voicelike.app

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

@Stable
object AdvancedTextureModeState {
    var enabled by mutableStateOf(false)
}

val LocalAdvancedTextureEnabled = staticCompositionLocalOf { false }

// Backdrop rendering can crash RenderThread on some GPU/ROM combinations.
// Keep advanced texture style enabled, but gate native backdrop path globally for stability.
private fun supportsNativeBackdrop(): Boolean = false

@Composable
fun ProvideAdvancedTexture(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val canUseBackdrop = enabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        supportsNativeBackdrop()
    val sceneBackdrop = if (canUseBackdrop) rememberLayerBackdrop() else null
    CompositionLocalProvider(
        LocalAdvancedTextureEnabled provides enabled
    ) {
        val rootModifier = if (sceneBackdrop != null) {
            Modifier.layerBackdrop(sceneBackdrop)
        } else {
            Modifier
        }
        Box(modifier = rootModifier) {
            content()
            if (canUseBackdrop && sceneBackdrop != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                        backdrop = sceneBackdrop,
                        shape = { RectangleShape },
                        effects = {
                            vibrancy()
                            blur(radius = 18f)
                        },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.06f)) }
                    )
                )
            }
            if (enabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.03f)
                                )
                            )
                        )
                )
            }
        }
    }
}

fun Modifier.advancedTextureCard(
    shape: Shape,
    tint: Color = Color.White.copy(alpha = 0.14f),
    borderWidth: Dp = 0.8.dp
): Modifier = composed {
    if (!LocalAdvancedTextureEnabled.current) return@composed this

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.42f),
            Color.White.copy(alpha = 0.12f)
        )
    )
    val fallbackSurface = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.24f),
            tint.copy(alpha = 0.72f),
            Color.Black.copy(alpha = 0.08f)
        )
    )
    return@composed clip(shape)
        .background(fallbackSurface)
        .drawWithCache {
            val topGlow = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.24f),
                    Color.Transparent
                ),
                center = Offset(x = size.width * 0.2f, y = size.height * 0.12f),
                radius = size.maxDimension * 0.9f
            )
            val topSheen = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                start = Offset.Zero,
                end = Offset(x = 0f, y = size.height * 0.6f)
            )
            onDrawWithContent {
                drawContent()
                drawRect(topGlow)
                drawRect(topSheen)
                // Subtle hue lift gives stronger "glass" feel without native backdrop.
                drawRect(Color.White.copy(alpha = 0.05f), blendMode = BlendMode.Screen)
            }
        }
        .border(borderWidth, borderBrush, shape)
}

fun Modifier.advancedTextureButton(
    shape: Shape,
    tint: Color = Color.White.copy(alpha = 0.10f),
    borderWidth: Dp = 0.7.dp
): Modifier = advancedTextureCard(
    shape = shape,
    tint = tint,
    borderWidth = borderWidth
)
