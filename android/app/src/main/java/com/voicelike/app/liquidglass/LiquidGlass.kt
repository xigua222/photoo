package com.voicelike.app.liquidglass

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

/** Lens refraction needs RuntimeShader (API 33); below that glass would read as a flat tint. */
fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** The user's liquid glass setting, already combined with [isLiquidGlassSupported]. */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }

/**
 * What floating chrome on the current screen refracts. A screen records its content with
 * `Modifier.layerBackdrop(backdrop)` and provides the backdrop to the chrome drawn above it;
 * content inside the recorded node must never read it, or it would sample itself.
 */
val LocalLiquidGlassBackdrop = compositionLocalOf<Backdrop?> { null }

/** A backdrop for a screen's content, or null when liquid glass is off. */
@Composable
fun rememberLiquidGlassBackdrop(): LayerBackdrop? =
    if (LocalLiquidGlassEnabled.current) rememberLayerBackdrop() else null

/** Glass for a floating control: the Kyant catalog's LiquidButton material. */
fun Modifier.liquidGlassControl(
    backdrop: Backdrop,
    shape: Shape,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(2.dp.toPx())
        lens(12.dp.toPx(), 24.dp.toPx())
    },
    highlight = { Highlight.Default },
    // No drop shadow: floating chrome often sits inside clipping layers (e.g. reveal blurs),
    // where the soft shadow gets cut into a visible box.
    shadow = null,
    onDrawSurface = {
        if (tint.isSpecified) {
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(tint.copy(alpha = 0.75f))
        }
        if (surfaceColor.isSpecified) drawRect(surfaceColor)
    }
)

/** Glass for a floating bar or panel: the Kyant catalog's LiquidBottomTabs container material. */
fun Modifier.liquidGlassPanel(
    backdrop: Backdrop,
    shape: Shape,
    surfaceColor: Color
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(8.dp.toPx())
        lens(24.dp.toPx(), 24.dp.toPx())
    },
    shadow = null,
    onDrawSurface = { drawRect(surfaceColor) }
)

/** Surface colour that keeps glass legible over busy content; raise [alpha] for dense controls. */
fun liquidGlassSurfaceColor(isLightTheme: Boolean, alpha: Float = 0.4f): Color =
    if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = alpha) else Color(0xFF121212).copy(alpha = alpha)
