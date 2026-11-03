package com.voicelike.app.colorcard.render

import com.voicelike.app.ColorCardTraceRgb
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val COLOR_CARD_TRACE_ANTI_BANDING_MAX_DELTA = 1.05f
internal const val COLOR_CARD_TRACE_ANTI_BANDING_MAX_DELTA_NORMALIZED =
    COLOR_CARD_TRACE_ANTI_BANDING_MAX_DELTA / 255f

internal fun resolveColorCardTraceAntiBandingMix(
    color: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    paletteStrength: Float
): Float {
    val tonalPressure = max(abs(tone), abs(brightness) * 0.92f)
    val chromaPressure = max(abs(color) * 0.52f, abs(saturation) * 0.44f)
    val mappingPressure = paletteStrength.coerceIn(0f, 1f) * 0.48f
    val combinedPressure = max(tonalPressure, max(chromaPressure, mappingPressure))
    return ((combinedPressure - 0.18f) / 0.62f).coerceIn(0f, 1f)
}

internal fun applyColorCardTraceAntiBanding(
    rgb: ColorCardTraceRgb,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    referencePixels: IntArray,
    mix: Float
): ColorCardTraceRgb {
    val safeMix = mix.coerceIn(0f, 1f)
    if (safeMix <= 0.0001f || width <= 1 || height <= 1 || referencePixels.isEmpty()) return rgb

    val index = ((y.coerceIn(0, height - 1) * width) + x.coerceIn(0, width - 1))
        .coerceIn(0, referencePixels.lastIndex)
    val center = referencePixels[index]
    val left = referencePixels[(y * width) + (x - 1).coerceAtLeast(0)]
    val right = referencePixels[(y * width) + (x + 1).coerceAtMost(width - 1)]
    val up = referencePixels[(y - 1).coerceAtLeast(0) * width + x]
    val down = referencePixels[(y + 1).coerceAtMost(height - 1) * width + x]

    val centerLuma = colorCardTraceAntiBandingLuma(center)
    val maxDiff = max(
        max(abs(centerLuma - colorCardTraceAntiBandingLuma(left)), abs(centerLuma - colorCardTraceAntiBandingLuma(right))),
        max(abs(centerLuma - colorCardTraceAntiBandingLuma(up)), abs(centerLuma - colorCardTraceAntiBandingLuma(down)))
    )
    val smoothness = 1f - colorCardTraceAntiBandingSmoothStep(0.012f, 0.05f, maxDiff)
    if (smoothness <= 0.001f) return rgb

    val lumaZone = colorCardTraceAntiBandingSoftRange(
        value = centerLuma,
        outerStart = 0.03f,
        innerStart = 0.10f,
        innerEnd = 0.94f,
        outerEnd = 0.985f
    )
    if (lumaZone <= 0.001f) return rgb

    val amount = safeMix * smoothness * lumaZone
    if (amount <= 0.001f) return rgb

    val noise = colorCardTraceAntiBandingTriangularNoise(x, y)
    val delta = (noise * COLOR_CARD_TRACE_ANTI_BANDING_MAX_DELTA * amount).roundToInt()
    if (delta == 0) return rgb

    return ColorCardTraceRgb(
        red = (rgb.red + delta).coerceIn(0, 255),
        green = (rgb.green + delta).coerceIn(0, 255),
        blue = (rgb.blue + delta).coerceIn(0, 255)
    )
}

private fun colorCardTraceAntiBandingLuma(colorInt: Int): Float {
    val red = ((colorInt ushr 16) and 0xFF) / 255f
    val green = ((colorInt ushr 8) and 0xFF) / 255f
    val blue = (colorInt and 0xFF) / 255f
    return (0.299f * red) + (0.587f * green) + (0.114f * blue)
}

private fun colorCardTraceAntiBandingTriangularNoise(
    x: Int,
    y: Int
): Float {
    val left = colorCardTraceAntiBandingHash(x.toFloat() + 0.17f, y.toFloat() + 0.61f)
    val right = colorCardTraceAntiBandingHash(x.toFloat() + 7.31f, y.toFloat() + 3.47f)
    return (left + right - 1f).coerceIn(-1f, 1f)
}

private fun colorCardTraceAntiBandingHash(
    x: Float,
    y: Float
): Float {
    val seed = sin((x * 12.9898f) + (y * 78.233f) + 37.719f) * 43758.5453f
    return (seed - kotlin.math.floor(seed)).toFloat().coerceIn(0f, 1f)
}

private fun colorCardTraceAntiBandingSoftRange(
    value: Float,
    outerStart: Float,
    innerStart: Float,
    innerEnd: Float,
    outerEnd: Float
): Float {
    if (value <= outerStart || value >= outerEnd) return 0f
    if (value in innerStart..innerEnd) return 1f
    return if (value < innerStart) {
        colorCardTraceAntiBandingSmoothStep(outerStart, innerStart, value)
    } else {
        1f - colorCardTraceAntiBandingSmoothStep(innerEnd, outerEnd, value)
    }
}

private fun colorCardTraceAntiBandingSmoothStep(
    edge0: Float,
    edge1: Float,
    value: Float
): Float {
    if (edge0 == edge1) return if (value >= edge1) 1f else 0f
    val normalized = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return normalized * normalized * (3f - (2f * normalized))
}
