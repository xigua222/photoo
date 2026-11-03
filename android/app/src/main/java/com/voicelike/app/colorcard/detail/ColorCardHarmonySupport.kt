package com.voicelike.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal const val COLOR_CARD_HARMONY_SECTION_TITLE = "\u76f8\u5173\u914d\u8272"
internal const val COLOR_CARD_HARMONY_LABEL_COMPLEMENT = "\u5bf9\u6bd4\u8272"
internal const val COLOR_CARD_HARMONY_LABEL_ANALOGOUS_A = "\u90bb\u8fd1\u8272 A"
internal const val COLOR_CARD_HARMONY_LABEL_ANALOGOUS_B = "\u90bb\u8fd1\u8272 B"
internal const val COLOR_CARD_HARMONY_LABEL_TRIAD_A = "\u4e09\u89d2\u8272 A"
internal const val COLOR_CARD_HARMONY_LABEL_TRIAD_B = "\u4e09\u89d2\u8272 B"

internal data class ColorCardHarmonySwatch(
    val label: String,
    val colorInt: Int
) {
    val hex: String
        get() = "#%02X%02X%02X".format(
            (colorInt ushr 16) and 0xFF,
            (colorInt ushr 8) and 0xFF,
            colorInt and 0xFF
        )
}

internal fun resolveColorCardHarmonySwatches(
    colorInt: Int
): List<ColorCardHarmonySwatch> {
    val base = colorCardHarmonyHsvFromColorInt(colorInt)
    val harmonyBase = normalizeColorCardHarmonyBase(base)
    return listOf(
        ColorCardHarmonySwatch(
            COLOR_CARD_HARMONY_LABEL_COMPLEMENT,
            colorCardHarmonyColorFromHsv(harmonyBase.shifted(180f))
        ),
        ColorCardHarmonySwatch(
            COLOR_CARD_HARMONY_LABEL_ANALOGOUS_A,
            colorCardHarmonyColorFromHsv(harmonyBase.shifted(-30f))
        ),
        ColorCardHarmonySwatch(
            COLOR_CARD_HARMONY_LABEL_ANALOGOUS_B,
            colorCardHarmonyColorFromHsv(harmonyBase.shifted(30f))
        ),
        ColorCardHarmonySwatch(
            COLOR_CARD_HARMONY_LABEL_TRIAD_A,
            colorCardHarmonyColorFromHsv(harmonyBase.shifted(-120f))
        ),
        ColorCardHarmonySwatch(
            COLOR_CARD_HARMONY_LABEL_TRIAD_B,
            colorCardHarmonyColorFromHsv(harmonyBase.shifted(120f))
        )
    )
}

private data class ColorCardHarmonyHsv(
    val hue: Float,
    val saturation: Float,
    val value: Float
) {
    fun shifted(deltaHue: Float): ColorCardHarmonyHsv {
        val shiftedHue = ((hue + deltaHue) % 360f + 360f) % 360f
        return copy(hue = shiftedHue)
    }
}

private fun normalizeColorCardHarmonyBase(
    hsv: ColorCardHarmonyHsv
): ColorCardHarmonyHsv {
    val boostedSaturation = when {
        hsv.saturation >= 16f -> hsv.saturation
        hsv.saturation >= 6f -> 24f
        else -> 32f
    }
    val stabilizedValue = when {
        hsv.value < 20f -> 38f
        else -> hsv.value
    }
    return hsv.copy(
        saturation = boostedSaturation.coerceIn(0f, 100f),
        value = stabilizedValue.coerceIn(0f, 100f)
    )
}

private fun colorCardHarmonyHsvFromColorInt(
    colorInt: Int
): ColorCardHarmonyHsv {
    val red = (colorInt ushr 16) and 0xFF
    val green = (colorInt ushr 8) and 0xFF
    val blue = colorInt and 0xFF
    val rf = red / 255f
    val gf = green / 255f
    val bf = blue / 255f
    val maxChannel = max(rf, max(gf, bf))
    val minChannel = min(rf, min(gf, bf))
    val delta = maxChannel - minChannel
    val hue = when {
        delta == 0f -> 0f
        maxChannel == rf -> (((gf - bf) / delta) % 6f) * 60f
        maxChannel == gf -> (((bf - rf) / delta) + 2f) * 60f
        else -> (((rf - gf) / delta) + 4f) * 60f
    }.let { if (it < 0f) it + 360f else it }
    val saturation = if (maxChannel == 0f) 0f else (delta / maxChannel) * 100f
    val value = maxChannel * 100f
    return ColorCardHarmonyHsv(
        hue = hue,
        saturation = saturation,
        value = value
    )
}

private fun colorCardHarmonyColorFromHsv(
    hsv: ColorCardHarmonyHsv
): Int {
    val hue = ((hsv.hue % 360f) + 360f) % 360f
    val saturation = (hsv.saturation / 100f).coerceIn(0f, 1f)
    val value = (hsv.value / 100f).coerceIn(0f, 1f)

    val chroma = value * saturation
    val hueSection = hue / 60f
    val x = chroma * (1f - abs((hueSection % 2f) - 1f))
    val m = value - chroma

    val (rPrime, gPrime, bPrime) = when {
        hueSection < 1f -> Triple(chroma, x, 0f)
        hueSection < 2f -> Triple(x, chroma, 0f)
        hueSection < 3f -> Triple(0f, chroma, x)
        hueSection < 4f -> Triple(0f, x, chroma)
        hueSection < 5f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }

    val red = ((rPrime + m) * 255f).roundToInt().coerceIn(0, 255)
    val green = ((gPrime + m) * 255f).roundToInt().coerceIn(0, 255)
    val blue = ((bPrime + m) * 255f).roundToInt().coerceIn(0, 255)
    return (255 shl 24) or (red shl 16) or (green shl 8) or blue
}
