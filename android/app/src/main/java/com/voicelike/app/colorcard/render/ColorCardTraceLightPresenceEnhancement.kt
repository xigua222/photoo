package com.voicelike.app.colorcard.render

import com.voicelike.app.ColorCardTraceRgb
import kotlin.math.max

internal fun applyColorCardTraceLightPresenceEnhancement(
    rgb: ColorCardTraceRgb,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    referencePixels: IntArray
): ColorCardTraceRgb {
    if (width <= 1 || height <= 1 || referencePixels.isEmpty()) return rgb

    val currentLuma = colorCardTraceLightPresenceLuma(rgb.red, rgb.green, rgb.blue)
    if (currentLuma <= 0.36f) return rgb

    val centerLuma = colorCardTraceLightPresenceLuma(referencePixels[(y * width) + x])
    val blurredLuma = colorCardTraceLightPresenceBlurredLuma(
        x = x,
        y = y,
        width = width,
        height = height,
        referencePixels = referencePixels
    )
    val localHighlight = (centerLuma - blurredLuma).coerceAtLeast(0f)
    val transparencyMask = colorCardTraceLightPresenceTransparencyMask(currentLuma)
    val highlightMask = colorCardTraceLightPresenceHighlightMask(currentLuma)
    val extremeMask = colorCardTraceLightPresenceExtremeMask(currentLuma)
    val lift = (
        (transparencyMask * (0.0045f + (localHighlight * 0.020f))) +
            (highlightMask * (0.004f + (localHighlight * 0.014f)))
        ).coerceAtMost(0.022f)
    val rolloff = (
        extremeMask * (0.006f + (0.006f * (1f - localHighlight.coerceIn(0f, 1f))))
        ).coerceAtMost(0.012f)
    val targetLuma = (currentLuma + lift - rolloff)
        .coerceIn((currentLuma - 0.01f).coerceAtLeast(0f), 1f)
    if (targetLuma == currentLuma) return rgb

    val ratio = targetLuma / max(currentLuma, 0.0001f)
    return ColorCardTraceRgb(
        red = (rgb.red * ratio).toInt().coerceIn(0, 255),
        green = (rgb.green * ratio).toInt().coerceIn(0, 255),
        blue = (rgb.blue * ratio).toInt().coerceIn(0, 255)
    )
}

private fun colorCardTraceLightPresenceBlurredLuma(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    referencePixels: IntArray
): Float {
    val rowOffset = y * width
    val upOffset = (y - 1).coerceAtLeast(0) * width
    val downOffset = (y + 1).coerceAtMost(height - 1) * width
    val center = referencePixels[rowOffset + x]
    val left = referencePixels[rowOffset + (x - 1).coerceAtLeast(0)]
    val right = referencePixels[rowOffset + (x + 1).coerceAtMost(width - 1)]
    val up = referencePixels[upOffset + x]
    val down = referencePixels[downOffset + x]
    return (
        (colorCardTraceLightPresenceLuma(center) * 4f) +
            colorCardTraceLightPresenceLuma(left) +
            colorCardTraceLightPresenceLuma(right) +
            colorCardTraceLightPresenceLuma(up) +
            colorCardTraceLightPresenceLuma(down)
        ) / 8f
}

private fun colorCardTraceLightPresenceTransparencyMask(luma: Float): Float {
    return colorCardTraceLightPresenceSmoothStep(0.38f, 0.72f, luma) *
        (1f - colorCardTraceLightPresenceSmoothStep(0.82f, 0.95f, luma))
}

private fun colorCardTraceLightPresenceHighlightMask(luma: Float): Float {
    return colorCardTraceLightPresenceSmoothStep(0.58f, 0.92f, luma) *
        (1f - colorCardTraceLightPresenceSmoothStep(0.93f, 0.995f, luma))
}

private fun colorCardTraceLightPresenceExtremeMask(luma: Float): Float {
    return colorCardTraceLightPresenceSmoothStep(0.92f, 0.995f, luma)
}

private fun colorCardTraceLightPresenceLuma(colorInt: Int): Float {
    return colorCardTraceLightPresenceLuma(
        red = (colorInt ushr 16) and 0xFF,
        green = (colorInt ushr 8) and 0xFF,
        blue = colorInt and 0xFF
    )
}

private fun colorCardTraceLightPresenceLuma(
    red: Int,
    green: Int,
    blue: Int
): Float {
    return (
        ((red.coerceIn(0, 255) / 255f) * 0.299f) +
            ((green.coerceIn(0, 255) / 255f) * 0.587f) +
            ((blue.coerceIn(0, 255) / 255f) * 0.114f)
        ).coerceIn(0f, 1f)
}

private fun colorCardTraceLightPresenceSmoothStep(
    edge0: Float,
    edge1: Float,
    value: Float
): Float {
    if (edge0 == edge1) return if (value >= edge1) 1f else 0f
    val normalized = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return normalized * normalized * (3f - (2f * normalized))
}
