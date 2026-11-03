package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.voicelike.app.colorcard.portrait.COLOR_CARD_TRACE_PORTRAIT_DISABLED
import com.voicelike.app.colorcard.portrait.ColorCardPortraitOptimizationParams
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class ColorCardTraceSelectiveColorPreset(
    val id: String,
    val paletteId: String,
    val title: String,
    val sourceTag: String,
    val targetHue: Float,
    val hueRange: Float,
    val saturationFloor: Float,
    val previewColors: List<Int>,
    val softMistStrength: Float
)

internal data class ColorCardTraceSelectiveColorParams(
    val enabled: Boolean,
    val targetHue: Float,
    val hueRange: Float,
    val saturationFloor: Float,
    val strength: Float,
    val softMistStrength: Float
)

private data class ColorCardTraceSelectiveColorHsv(
    val hue: Double,
    val saturation: Double
)

private val COLOR_CARD_TRACE_SELECTIVE_COLOR_PRESETS = listOf(
    ColorCardTraceSelectiveColorPreset(
        id = "selective-red",
        paletteId = "builtin_creative:selective-red",
        title = "#111V",
        sourceTag = "Color Accent",
        targetHue = 0f,
        hueRange = 0.21f,
        saturationFloor = 0.04f,
        previewColors = listOf(
            0xFFE01E1E.toInt(),
            0xFF9F1111.toInt(),
            0xFFBCBCBC.toInt(),
            0xFF2A2A2A.toInt()
        ),
        softMistStrength = 0.24f
    ),
    ColorCardTraceSelectiveColorPreset(
        id = "selective-green",
        paletteId = "builtin_creative:selective-green",
        title = "#112V",
        sourceTag = "Color Accent",
        targetHue = (120f / 360f),
        hueRange = 0.18f,
        saturationFloor = 0.04f,
        previewColors = listOf(
            0xFF1FB856.toInt(),
            0xFF138541.toInt(),
            0xFFBCBCBC.toInt(),
            0xFF2A2A2A.toInt()
        ),
        softMistStrength = 0f
    ),
    ColorCardTraceSelectiveColorPreset(
        id = "selective-blue",
        paletteId = "builtin_creative:selective-blue",
        title = "#113V",
        sourceTag = "Color Accent",
        targetHue = (220f / 360f),
        hueRange = 0.18f,
        saturationFloor = 0.04f,
        previewColors = listOf(
            0xFF2E6BFF.toInt(),
            0xFF1D47B8.toInt(),
            0xFFBCBCBC.toInt(),
            0xFF2A2A2A.toInt()
        ),
        softMistStrength = 0f
    )
)

private val COLOR_CARD_TRACE_DISABLED_SELECTIVE_COLOR_PARAMS = ColorCardTraceSelectiveColorParams(
    enabled = false,
    targetHue = 0f,
    hueRange = 0f,
    saturationFloor = 0f,
    strength = 0f,
    softMistStrength = 0f
)

internal fun buildColorCardTraceSelectiveColorCandidates(): List<ColorCardTraceFilterCandidate> {
    return COLOR_CARD_TRACE_SELECTIVE_COLOR_PRESETS.map { preset ->
        ColorCardTraceFilterCandidate(
            paletteId = preset.paletteId,
            kind = ColorCardTraceFilterKind.SelectiveColor,
            title = preset.title,
            sourceTag = preset.sourceTag,
            colors = preset.previewColors,
            score = -1.0,
            selectiveColorPreset = preset
        )
    }
}

internal fun resolveColorCardTraceSelectiveColorParams(
    candidate: ColorCardTraceFilterCandidate?,
    paletteStrength: Float
): ColorCardTraceSelectiveColorParams {
    val preset = candidate?.selectiveColorPreset
        ?.takeIf { candidate.kind == ColorCardTraceFilterKind.SelectiveColor }
        ?: return COLOR_CARD_TRACE_DISABLED_SELECTIVE_COLOR_PARAMS
    val safeStrength = paletteStrength.coerceIn(0f, 1f)
    return ColorCardTraceSelectiveColorParams(
        enabled = safeStrength > 0.0001f,
        targetHue = preset.targetHue.coerceIn(0f, 1f),
        hueRange = preset.hueRange.coerceIn(0.01f, 0.5f),
        saturationFloor = preset.saturationFloor.coerceIn(0f, 1f),
        // This filter is meant to behave as "keep target color, gray everything else".
        // Do not fade back to the original image based on strength, or the effect reads as broken.
        strength = 1f,
        softMistStrength = (preset.softMistStrength * safeStrength).coerceIn(0f, 1f)
    )
}

internal fun renderColorCardTraceSelectiveColorBitmap(
    sourceBitmap: Bitmap,
    candidate: ColorCardTraceFilterCandidate,
    color: Float,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    strength: Float,
    faceRegions: List<ColorCardTraceFaceRegion> = emptyList(),
    portraitParams: ColorCardPortraitOptimizationParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED
): Bitmap {
    val params = resolveColorCardTraceSelectiveColorParams(candidate, strength)
    if (!params.enabled) {
        return sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val detailAdjustedSourcePixels = applyColorCardTraceSourceDetailAdjustments(
        sourcePixels = sourcePixels,
        width = width,
        height = height,
        sharpen = sharpen,
        faceRegions = faceRegions,
        portraitParams = portraitParams
    )

    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val sourceColor = detailAdjustedSourcePixels[rowOffset + x]
            val alpha = AndroidColor.alpha(sourceColor)
            if (alpha == 0) {
                outputPixels[rowOffset + x] = sourceColor
                continue
            }

            val baseRgb = ColorCardTraceRgb(
                red = AndroidColor.red(sourceColor),
                green = AndroidColor.green(sourceColor),
                blue = AndroidColor.blue(sourceColor)
            )
            val softenedRgb = if (params.softMistStrength > 0.0001f) {
                val blurredRgb = sampleColorCardTraceSelectiveSoftMistBlur(
                    pixels = detailAdjustedSourcePixels,
                    width = width,
                    height = height,
                    x = x,
                    y = y
                )
                applyColorCardTraceSelectiveSoftMistRgb(
                    baseRgb = baseRgb,
                    blurredRgb = blurredRgb,
                    strength = params.softMistStrength
                )
            } else {
                baseRgb
            }
            val adjustedRgb = applyColorCardTraceCreativeAdjustments(
                lab = colorCardTraceLabFromRgb(
                    red = softenedRgb.red,
                    green = softenedRgb.green,
                    blue = softenedRgb.blue
                ),
                color = color,
                tone = tone,
                saturation = saturation,
                brightness = brightness,
                colorAxis = colorAxis,
                chromaScale = 1f,
                tonalScale = 1f,
                portraitMask = colorCardTraceSkinProtectionAmount(
                    normalizedX = if (width <= 1) 0.5f else x / (width - 1).coerceAtLeast(1).toFloat(),
                    normalizedY = if (height <= 1) 0.5f else y / (height - 1).coerceAtLeast(1).toFloat(),
                    red = softenedRgb.red,
                    green = softenedRgb.green,
                    blue = softenedRgb.blue,
                    faceRegions = faceRegions
                ),
                portraitParams = portraitParams
            )
            val finalRgb = applyColorCardTraceSelectiveColorRgb(adjustedRgb, params)
            outputPixels[rowOffset + x] = AndroidColor.argb(
                alpha,
                finalRgb.red,
                finalRgb.green,
                finalRgb.blue
            )
        }
    }

    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

internal fun applyColorCardTraceSelectiveColorRgb(
    rgb: ColorCardTraceRgb,
    params: ColorCardTraceSelectiveColorParams
): ColorCardTraceRgb {
    if (!params.enabled || params.strength <= 0.0001f) return rgb

    val colorMask = resolveColorCardTraceSelectiveColorMask(rgb, params)
    val gray = (
        (rgb.red / 255.0) * 0.299 +
            (rgb.green / 255.0) * 0.587 +
            (rgb.blue / 255.0) * 0.114
        ) * 255.0
    val safeStrength = params.strength.coerceIn(0f, 1f).toDouble()
    val desaturationAmount = ((1.0 - colorMask) * (0.66 + (0.26 * safeStrength))).coerceIn(0.0, 1.0)
    val isolatedRgb = ColorCardTraceRgb(
        red = lerpColorCardTraceSelectiveColor(rgb.red.toDouble(), gray, desaturationAmount)
            .roundToInt()
            .coerceIn(0, 255),
        green = lerpColorCardTraceSelectiveColor(rgb.green.toDouble(), gray, desaturationAmount)
            .roundToInt()
            .coerceIn(0, 255),
        blue = lerpColorCardTraceSelectiveColor(rgb.blue.toDouble(), gray, desaturationAmount)
            .roundToInt()
            .coerceIn(0, 255)
    )
    val accentMask = smoothStepColorCardTraceSelectiveColor(
        edge0 = 0.18,
        edge1 = 0.90,
        value = colorMask
    )
    val accentBoost = accentMask * (0.08 + (0.10 * safeStrength))
    val boostedRgb = boostColorCardTraceSelectiveColorSaturation(
        rgb = isolatedRgb,
        factor = 1.0 + accentBoost
    )
    val lightLift = accentBoost * 0.08
    return ColorCardTraceRgb(
        red = (boostedRgb.red + ((255.0 - boostedRgb.red) * lightLift))
            .roundToInt()
            .coerceIn(0, 255),
        green = (boostedRgb.green + ((255.0 - boostedRgb.green) * lightLift))
            .roundToInt()
            .coerceIn(0, 255),
        blue = (boostedRgb.blue + ((255.0 - boostedRgb.blue) * lightLift))
            .roundToInt()
            .coerceIn(0, 255)
    )
}

private fun resolveColorCardTraceSelectiveColorMask(
    rgb: ColorCardTraceRgb,
    params: ColorCardTraceSelectiveColorParams
): Double {
    val hsv = colorCardTraceSelectiveColorToHsv(rgb.red, rgb.green, rgb.blue)
    val hueDistance = min(
        abs(hsv.hue - params.targetHue.toDouble()),
        1.0 - abs(hsv.hue - params.targetHue.toDouble())
    )
    val hueMask = 1.0 - smoothStepColorCardTraceSelectiveColor(
        edge0 = (params.hueRange * 0.72f).toDouble(),
        edge1 = (params.hueRange * 1.16f).coerceAtMost(0.5f).toDouble(),
        value = hueDistance
    )
    val satMask = smoothStepColorCardTraceSelectiveColor(
        edge0 = (params.saturationFloor * 0.55f).toDouble(),
        edge1 = (params.saturationFloor + 0.10f).coerceAtMost(1f).toDouble(),
        value = hsv.saturation
    )
    return (hueMask * satMask).coerceIn(0.0, 1.0)
}

private data class ColorCardTraceSelectiveColorBoostedRgb(
    val red: Double,
    val green: Double,
    val blue: Double
)

private fun boostColorCardTraceSelectiveColorSaturation(
    rgb: ColorCardTraceRgb,
    factor: Double
): ColorCardTraceSelectiveColorBoostedRgb {
    val gray = (
        (rgb.red / 255.0) * 0.299 +
            (rgb.green / 255.0) * 0.587 +
            (rgb.blue / 255.0) * 0.114
        ) * 255.0
    return ColorCardTraceSelectiveColorBoostedRgb(
        red = (gray + ((rgb.red - gray) * factor)).coerceIn(0.0, 255.0),
        green = (gray + ((rgb.green - gray) * factor)).coerceIn(0.0, 255.0),
        blue = (gray + ((rgb.blue - gray) * factor)).coerceIn(0.0, 255.0)
    )
}

private fun sampleColorCardTraceSelectiveSoftMistBlur(
    pixels: IntArray,
    width: Int,
    height: Int,
    x: Int,
    y: Int
): ColorCardTraceRgb {
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    var weightSum = 0.0

    fun sample(dx: Int, dy: Int, weight: Double) {
        val sampledX = (x + dx).coerceIn(0, width - 1)
        val sampledY = (y + dy).coerceIn(0, height - 1)
        val color = pixels[(sampledY * width) + sampledX]
        red += AndroidColor.red(color) * weight
        green += AndroidColor.green(color) * weight
        blue += AndroidColor.blue(color) * weight
        weightSum += weight
    }

    sample(0, 0, 4.0)
    sample(-1, 0, 1.6)
    sample(1, 0, 1.6)
    sample(0, -1, 1.6)
    sample(0, 1, 1.6)
    sample(-1, -1, 0.85)
    sample(1, -1, 0.85)
    sample(-1, 1, 0.85)
    sample(1, 1, 0.85)

    val safeWeightSum = weightSum.coerceAtLeast(0.0001)
    return ColorCardTraceRgb(
        red = (red / safeWeightSum).roundToInt().coerceIn(0, 255),
        green = (green / safeWeightSum).roundToInt().coerceIn(0, 255),
        blue = (blue / safeWeightSum).roundToInt().coerceIn(0, 255)
    )
}

private fun applyColorCardTraceSelectiveSoftMistRgb(
    baseRgb: ColorCardTraceRgb,
    blurredRgb: ColorCardTraceRgb,
    strength: Float
): ColorCardTraceRgb {
    val safeStrength = strength.coerceIn(0f, 1f).toDouble()
    if (safeStrength <= 0.0001) return baseRgb

    val baseRed = baseRgb.red / 255.0
    val baseGreen = baseRgb.green / 255.0
    val baseBlue = baseRgb.blue / 255.0
    val blurRed = blurredRgb.red / 255.0
    val blurGreen = blurredRgb.green / 255.0
    val blurBlue = blurredRgb.blue / 255.0
    val luma = colorCardTraceSelectiveColorLuma(baseRed, baseGreen, baseBlue)
    val highlight = smoothStepColorCardTraceSelectiveColor(0.54, 0.92, luma)
    val softnessMix = 0.04 + (safeStrength * 0.08)
    val glowAmount = highlight * (0.03 + (safeStrength * 0.10))
    val hazeLift = 0.008 + (safeStrength * 0.020)
    val softenedRed = lerpColorCardTraceSelectiveColor(baseRed, blurRed, softnessMix)
    val softenedGreen = lerpColorCardTraceSelectiveColor(baseGreen, blurGreen, softnessMix)
    val softenedBlue = lerpColorCardTraceSelectiveColor(baseBlue, blurBlue, softnessMix)
    val glowRed = lerpColorCardTraceSelectiveColor(blurRed, 0.98, 0.18 + (highlight * 0.08))
    val glowGreen = lerpColorCardTraceSelectiveColor(blurGreen, 0.95, 0.14 + (highlight * 0.06))
    val glowBlue = lerpColorCardTraceSelectiveColor(blurBlue, 0.97, 0.18 + (highlight * 0.08))
    return ColorCardTraceRgb(
        red = ((softenedRed + (glowRed * glowAmount) + hazeLift).coerceIn(0.0, 1.0) * 255.0)
            .roundToInt()
            .coerceIn(0, 255),
        green = ((softenedGreen + (glowGreen * glowAmount) + hazeLift).coerceIn(0.0, 1.0) * 255.0)
            .roundToInt()
            .coerceIn(0, 255),
        blue = ((softenedBlue + (glowBlue * glowAmount) + hazeLift).coerceIn(0.0, 1.0) * 255.0)
            .roundToInt()
            .coerceIn(0, 255)
    )
}

private fun colorCardTraceSelectiveColorToHsv(
    red: Int,
    green: Int,
    blue: Int
): ColorCardTraceSelectiveColorHsv {
    val r = red / 255.0
    val g = green / 255.0
    val b = blue / 255.0
    val maxChannel = max(r, max(g, b))
    val minChannel = min(r, min(g, b))
    val delta = maxChannel - minChannel
    if (delta <= 0.0000001) {
        return ColorCardTraceSelectiveColorHsv(hue = 0.0, saturation = 0.0)
    }
    val hue = when (maxChannel) {
        r -> ((g - b) / delta).let { if (it < 0.0) it + 6.0 else it }
        g -> ((b - r) / delta) + 2.0
        else -> ((r - g) / delta) + 4.0
    } / 6.0
    val saturation = if (maxChannel <= 0.0000001) 0.0 else delta / maxChannel
    return ColorCardTraceSelectiveColorHsv(
        hue = hue.coerceIn(0.0, 1.0),
        saturation = saturation.coerceIn(0.0, 1.0)
    )
}

private fun lerpColorCardTraceSelectiveColor(
    start: Double,
    end: Double,
    amount: Double
): Double {
    return start + ((end - start) * amount.coerceIn(0.0, 1.0))
}

private fun colorCardTraceSelectiveColorLuma(
    red: Double,
    green: Double,
    blue: Double
): Double {
    return (red * 0.299) + (green * 0.587) + (blue * 0.114)
}

private fun smoothStepColorCardTraceSelectiveColor(
    edge0: Double,
    edge1: Double,
    value: Double
): Double {
    if (edge1 <= edge0) {
        return if (value >= edge1) 1.0 else 0.0
    }
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
    return t * t * (3.0 - (2.0 * t))
}
