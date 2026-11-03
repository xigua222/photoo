package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.voicelike.app.colorcard.render.applyColorCardTraceAntiBanding
import com.voicelike.app.colorcard.render.resolveColorCardTraceAntiBandingMix
import kotlin.math.roundToInt

private const val COLOR_CARD_TRACE_BLACK_MIST_PALETTE_ID = "builtin_creative:black-mist"
private const val COLOR_CARD_TRACE_BLACK_MIST_TITLE = "#110V"
private const val COLOR_CARD_TRACE_BLACK_MIST_SOURCE_TAG = "Black Mist"
private const val COLOR_CARD_TRACE_BLACK_MIST_TINT = 0xFFF6D9EE.toInt()

internal fun buildColorCardTraceBlackMistCandidate(): ColorCardTraceFilterCandidate {
    return ColorCardTraceFilterCandidate(
        paletteId = COLOR_CARD_TRACE_BLACK_MIST_PALETTE_ID,
        kind = ColorCardTraceFilterKind.BlackMist,
        title = COLOR_CARD_TRACE_BLACK_MIST_TITLE,
        sourceTag = COLOR_CARD_TRACE_BLACK_MIST_SOURCE_TAG,
        colors = listOf(
            0xFFF8EBCF.toInt(),
            0xFFF6D9EE.toInt(),
            0xFF9A8FA2.toInt(),
            0xFF18171C.toInt()
        ),
        score = -1.0
    )
}

internal fun renderColorCardTraceBlackMistBitmap(
    sourceBitmap: Bitmap,
    color: Float,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    strength: Float
): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val detailAdjustedSourcePixels = applyColorCardTraceSourceDetailAdjustments(
        sourcePixels = sourcePixels,
        width = width,
        height = height,
        sharpen = sharpen
    )
    val safeStrength = strength.coerceIn(0f, 1f)
    val antiBandingMix = resolveColorCardTraceAntiBandingMix(
        color = color,
        tone = tone,
        saturation = saturation,
        brightness = brightness,
        paletteStrength = strength
    )
    val tintRed = AndroidColor.red(COLOR_CARD_TRACE_BLACK_MIST_TINT) / 255.0
    val tintGreen = AndroidColor.green(COLOR_CARD_TRACE_BLACK_MIST_TINT) / 255.0
    val tintBlue = AndroidColor.blue(COLOR_CARD_TRACE_BLACK_MIST_TINT) / 255.0

    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val sourceColor = detailAdjustedSourcePixels[rowOffset + x]
            val alpha = AndroidColor.alpha(sourceColor)
            if (alpha == 0) {
                outputPixels[rowOffset + x] = sourceColor
                continue
            }

            val centerRed = AndroidColor.red(sourceColor) / 255.0
            val centerGreen = AndroidColor.green(sourceColor) / 255.0
            val centerBlue = AndroidColor.blue(sourceColor) / 255.0
            val blurRgb = sampleColorCardTraceBlackMistBlur(
                pixels = detailAdjustedSourcePixels,
                width = width,
                height = height,
                x = x,
                y = y
            )
            val luma = colorCardTraceBlackMistLuma(centerRed, centerGreen, centerBlue)
            val highlight = smoothStepColorCardTraceBlackMist(0.56, 0.96, luma)
            val softnessMix = lerpColorCardTraceBlackMist(0.12, 0.26, safeStrength.toDouble())
            val bloomAmount = highlight * lerpColorCardTraceBlackMist(0.16, 0.42, safeStrength.toDouble())
            val veilLift = (1.0 - luma) * (0.015 + (safeStrength * 0.035))

            var workingRed = lerpColorCardTraceBlackMist(centerRed, blurRgb.red, softnessMix)
            var workingGreen = lerpColorCardTraceBlackMist(centerGreen, blurRgb.green, softnessMix)
            var workingBlue = lerpColorCardTraceBlackMist(centerBlue, blurRgb.blue, softnessMix)

            val glowRed = lerpColorCardTraceBlackMist(blurRgb.red, tintRed, 0.22 + (highlight * 0.18))
            val glowGreen = lerpColorCardTraceBlackMist(blurRgb.green, tintGreen, 0.18 + (highlight * 0.12))
            val glowBlue = lerpColorCardTraceBlackMist(blurRgb.blue, tintBlue, 0.28 + (highlight * 0.20))

            workingRed = (workingRed + (glowRed * bloomAmount) + veilLift).coerceIn(0.0, 1.0)
            workingGreen = (workingGreen + (glowGreen * bloomAmount) + veilLift).coerceIn(0.0, 1.0)
            workingBlue = (workingBlue + (glowBlue * bloomAmount) + veilLift).coerceIn(0.0, 1.0)

            val saturationBoost = 1.0 + (safeStrength * 0.10) + (highlight * safeStrength * 0.18)
            val saturatedRgb = boostColorCardTraceBlackMistSaturation(
                red = workingRed,
                green = workingGreen,
                blue = workingBlue,
                factor = saturationBoost
            )
            val dreamRgb = ColorCardTraceRgb(
                red = (saturatedRgb.red * 255.0).roundToInt().coerceIn(0, 255),
                green = (saturatedRgb.green * 255.0).roundToInt().coerceIn(0, 255),
                blue = (saturatedRgb.blue * 255.0).roundToInt().coerceIn(0, 255)
            )
            val finalRgb = applyColorCardTraceCreativeAdjustments(
                lab = colorCardTraceLabFromRgb(
                    red = dreamRgb.red,
                    green = dreamRgb.green,
                    blue = dreamRgb.blue
                ),
                color = color,
                tone = tone,
                saturation = saturation,
                brightness = brightness,
                colorAxis = colorAxis,
                chromaScale = 1f,
                tonalScale = 1f
            )
            val antiBandedRgb = applyColorCardTraceAntiBanding(
                rgb = finalRgb,
                x = x,
                y = y,
                width = width,
                height = height,
                referencePixels = sourcePixels,
                mix = antiBandingMix
            )
            outputPixels[rowOffset + x] = android.graphics.Color.argb(
                alpha,
                antiBandedRgb.red,
                antiBandedRgb.green,
                antiBandedRgb.blue
            )
        }
    }

    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

private data class ColorCardTraceBlackMistRgb(
    val red: Double,
    val green: Double,
    val blue: Double
)

private fun sampleColorCardTraceBlackMistBlur(
    pixels: IntArray,
    width: Int,
    height: Int,
    x: Int,
    y: Int
): ColorCardTraceBlackMistRgb {
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    var weightSum = 0.0
    fun sample(dx: Int, dy: Int, weight: Double) {
        val sampledX = (x + dx).coerceIn(0, width - 1)
        val sampledY = (y + dy).coerceIn(0, height - 1)
        val color = pixels[(sampledY * width) + sampledX]
        red += (AndroidColor.red(color) / 255.0) * weight
        green += (AndroidColor.green(color) / 255.0) * weight
        blue += (AndroidColor.blue(color) / 255.0) * weight
        weightSum += weight
    }
    sample(0, 0, 4.0)
    sample(-1, 0, 1.5)
    sample(1, 0, 1.5)
    sample(0, -1, 1.5)
    sample(0, 1, 1.5)
    sample(-2, 0, 0.75)
    sample(2, 0, 0.75)
    sample(0, -2, 0.75)
    sample(0, 2, 0.75)
    sample(-1, -1, 1.0)
    sample(1, -1, 1.0)
    sample(-1, 1, 1.0)
    sample(1, 1, 1.0)
    val safeWeightSum = weightSum.coerceAtLeast(0.0001)
    return ColorCardTraceBlackMistRgb(
        red = (red / safeWeightSum).coerceIn(0.0, 1.0),
        green = (green / safeWeightSum).coerceIn(0.0, 1.0),
        blue = (blue / safeWeightSum).coerceIn(0.0, 1.0)
    )
}

private fun boostColorCardTraceBlackMistSaturation(
    red: Double,
    green: Double,
    blue: Double,
    factor: Double
): ColorCardTraceBlackMistRgb {
    val luma = colorCardTraceBlackMistLuma(red, green, blue)
    return ColorCardTraceBlackMistRgb(
        red = (luma + ((red - luma) * factor)).coerceIn(0.0, 1.0),
        green = (luma + ((green - luma) * factor)).coerceIn(0.0, 1.0),
        blue = (luma + ((blue - luma) * factor)).coerceIn(0.0, 1.0)
    )
}

private fun colorCardTraceBlackMistLuma(
    red: Double,
    green: Double,
    blue: Double
): Double {
    return (red * 0.299) + (green * 0.587) + (blue * 0.114)
}

private fun smoothStepColorCardTraceBlackMist(
    edge0: Double,
    edge1: Double,
    value: Double
): Double {
    if (edge0 == edge1) return if (value >= edge1) 1.0 else 0.0
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
    return t * t * (3.0 - (2.0 * t))
}

private fun lerpColorCardTraceBlackMist(
    start: Double,
    end: Double,
    amount: Double
): Double {
    return start + ((end - start) * amount.coerceIn(0.0, 1.0))
}
