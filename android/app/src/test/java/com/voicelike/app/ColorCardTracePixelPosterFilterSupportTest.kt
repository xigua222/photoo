package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardTracePixelPosterFilterSupportTest {

    @Test
    fun `source color pixel poster returns sampled rgb directly`() {
        val params = ColorCardTracePixelPosterParams(
            enabled = true,
            style = ColorCardTracePixelPosterStyle.SourceColor,
            darkColor = 0xFF000000.toInt(),
            lightColor = 0xFFFFFFFF.toInt(),
            blockCountLongSide = 100f,
            threshold = 0.5f,
            contrast = 1f,
            noiseAmount = 0f,
            gridLineWidth = 0f,
            gridLineOpacity = 0f,
            detailBoost = 0f,
            shadowDetailBias = 0f,
            strength = 1f
        )

        val rgb = resolveColorCardTracePixelPosterBaseRgb(
            sampledColor = 0xFF3366CC.toInt(),
            blockX = 3,
            blockY = 4,
            params = params
        )

        assertEquals(0x33, rgb.red)
        assertEquals(0x66, rgb.green)
        assertEquals(0xCC, rgb.blue)
    }

    @Test
    fun `primary pixel presets use continuous duotone instead of block sampling`() {
        val candidatesById = buildColorCardTracePixelPosterCandidates().associateBy { candidate ->
            candidate.pixelPosterPreset?.id
        }

        val primaryYellow = requireNotNull(candidatesById["pixel-primary-yellow"]?.pixelPosterPreset)
        val primaryBlue = requireNotNull(candidatesById["pixel-primary-blue"]?.pixelPosterPreset)
        val primaryRed = requireNotNull(candidatesById["pixel-primary-red"]?.pixelPosterPreset)

        assertEquals(ColorCardTracePixelPosterStyle.ContinuousDuotone, primaryYellow.style)
        assertEquals(ColorCardTracePixelPosterStyle.ContinuousDuotone, primaryBlue.style)
        assertEquals(ColorCardTracePixelPosterStyle.ContinuousDuotone, primaryRed.style)
        assertTrue(primaryYellow.blockCountLongSide <= 1f)
        assertTrue(primaryBlue.blockCountLongSide <= 1f)
        assertTrue(primaryRed.blockCountLongSide <= 1f)
    }

    @Test
    fun `continuous duotone follows image tone and ignores block coordinate`() {
        val candidate = buildColorCardTracePixelPosterCandidates()
            .first { it.pixelPosterPreset?.id == "pixel-primary-yellow" }
        val params = resolveColorCardTracePixelPosterParams(candidate, paletteStrength = 1f)

        val shadow = resolveColorCardTracePixelPosterBaseRgb(
            sampledColor = 0xFF202020.toInt(),
            blockX = 2,
            blockY = 3,
            params = params
        )
        val highlight = resolveColorCardTracePixelPosterBaseRgb(
            sampledColor = 0xFFEAEAEA.toInt(),
            blockX = 9,
            blockY = 7,
            params = params
        )
        val sameToneDifferentBlock = resolveColorCardTracePixelPosterBaseRgb(
            sampledColor = 0xFF7A7A7A.toInt(),
            blockX = 1,
            blockY = 1,
            params = params
        )
        val sameToneDifferentBlockAgain = resolveColorCardTracePixelPosterBaseRgb(
            sampledColor = 0xFF7A7A7A.toInt(),
            blockX = 14,
            blockY = 11,
            params = params
        )

        assertTrue(highlight.red > shadow.red)
        assertTrue(highlight.green > shadow.green)
        assertTrue(highlight.blue > shadow.blue)
        assertEquals(sameToneDifferentBlock.red, sameToneDifferentBlockAgain.red)
        assertEquals(sameToneDifferentBlock.green, sameToneDifferentBlockAgain.green)
        assertEquals(sameToneDifferentBlock.blue, sameToneDifferentBlockAgain.blue)
    }
}
