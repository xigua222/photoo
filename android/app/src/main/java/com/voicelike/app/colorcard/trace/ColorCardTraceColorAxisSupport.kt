package com.voicelike.app

import android.graphics.Bitmap
import kotlin.math.hypot

internal data class ColorCardTraceColorAxis(
    val aWeight: Float,
    val bWeight: Float
)

internal val COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS = ColorCardTraceColorAxis(
    aWeight = 1f,
    bWeight = 0f
)

private const val COLOR_CARD_TRACE_COLOR_AXIS_TARGET_BLEND = 0.28f
private const val COLOR_CARD_TRACE_COLOR_AXIS_MIN_VECTOR = 3.2f
private val COLOR_CARD_TRACE_ADJUSTMENT_FIELD_BASE_LAB = ColorCardTraceLab(
    l = 72.0,
    a = 0.0,
    b = 0.0
)

internal fun resolveColorCardTraceColorAxis(
    sourceColors: List<Int>,
    targetColors: List<Int>
): ColorCardTraceColorAxis {
    val sourceCentroid = resolveColorCardTraceColorAxisCentroid(sourceColors)
    val targetCentroid = resolveColorCardTraceColorAxisCentroid(targetColors)

    var axisA = 0f
    var axisB = 0f

    if (sourceCentroid != null && targetCentroid != null) {
        val deltaA = (targetCentroid.a - sourceCentroid.a).toFloat()
        val deltaB = (targetCentroid.b - sourceCentroid.b).toFloat()
        axisA = (deltaA * (1f - COLOR_CARD_TRACE_COLOR_AXIS_TARGET_BLEND)) +
            (targetCentroid.a.toFloat() * COLOR_CARD_TRACE_COLOR_AXIS_TARGET_BLEND)
        axisB = (deltaB * (1f - COLOR_CARD_TRACE_COLOR_AXIS_TARGET_BLEND)) +
            (targetCentroid.b.toFloat() * COLOR_CARD_TRACE_COLOR_AXIS_TARGET_BLEND)
    } else if (targetCentroid != null) {
        axisA = targetCentroid.a.toFloat()
        axisB = targetCentroid.b.toFloat()
    }

    if (hypot(axisA.toDouble(), axisB.toDouble()).toFloat() < COLOR_CARD_TRACE_COLOR_AXIS_MIN_VECTOR) {
        axisA = targetCentroid?.a?.toFloat() ?: axisA
        axisB = targetCentroid?.b?.toFloat() ?: axisB
    }

    return normalizeColorCardTraceColorAxis(axisA, axisB)
}

internal fun resolveColorCardTraceAdjustmentFieldSample(
    normalizedX: Float,
    normalizedY: Float,
    colorAxis: ColorCardTraceColorAxis
): ColorCardTraceRgb {
    val signedColor = ((normalizedX.coerceIn(0f, 1f) - 0.5f) * 2f).coerceIn(-1f, 1f)
    val signedTone = ((0.5f - normalizedY.coerceIn(0f, 1f)) * 2f).coerceIn(-1f, 1f)
    return applyColorCardTraceCreativeAdjustments(
        lab = COLOR_CARD_TRACE_ADJUSTMENT_FIELD_BASE_LAB,
        color = signedColor,
        tone = signedTone,
        saturation = 0f,
        brightness = 0f,
        colorAxis = colorAxis
    )
}

internal fun renderColorCardTraceAdjustmentFieldBitmap(
    width: Int,
    height: Int,
    colorAxis: ColorCardTraceColorAxis
): Bitmap? {
    if (width <= 0 || height <= 0) return null
    val pixels = IntArray(width * height)
    val xNormalizer = (width - 1).coerceAtLeast(1).toFloat()
    val yNormalizer = (height - 1).coerceAtLeast(1).toFloat()

    for (y in 0 until height) {
        val normalizedY = if (height <= 1) 0.5f else y / yNormalizer
        val rowOffset = y * width
        for (x in 0 until width) {
            val normalizedX = if (width <= 1) 0.5f else x / xNormalizer
            val rgb = resolveColorCardTraceAdjustmentFieldSample(
                normalizedX = normalizedX,
                normalizedY = normalizedY,
                colorAxis = colorAxis
            )
            pixels[rowOffset + x] = (255 shl 24) or
                (rgb.red.coerceIn(0, 255) shl 16) or
                (rgb.green.coerceIn(0, 255) shl 8) or
                rgb.blue.coerceIn(0, 255)
        }
    }

    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun normalizeColorCardTraceColorAxis(
    aWeight: Float,
    bWeight: Float
): ColorCardTraceColorAxis {
    val magnitude = hypot(aWeight.toDouble(), bWeight.toDouble()).toFloat()
    if (magnitude <= 0.0001f) {
        return COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS
    }
    return ColorCardTraceColorAxis(
        aWeight = aWeight / magnitude,
        bWeight = bWeight / magnitude
    )
}

private fun resolveColorCardTraceColorAxisCentroid(
    colors: List<Int>
): ColorCardTraceLab? {
    if (colors.isEmpty()) return null

    var sumL = 0.0
    var sumA = 0.0
    var sumB = 0.0
    var count = 0

    colors.forEach { colorInt ->
        val lab = colorCardTraceLabFromRgb(
            red = (colorInt ushr 16) and 0xFF,
            green = (colorInt ushr 8) and 0xFF,
            blue = colorInt and 0xFF
        )
        sumL += lab.l
        sumA += lab.a
        sumB += lab.b
        count += 1
    }

    if (count <= 0) return null

    return ColorCardTraceLab(
        l = sumL / count.toDouble(),
        a = sumA / count.toDouble(),
        b = sumB / count.toDouble()
    )
}
