package com.voicelike.app.colorcard.render

import com.voicelike.app.ColorCardTraceRgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardTraceAntiBandingTest {

    @Test
    fun `anti banding mix stays zero for neutral adjustments`() {
        val mix = resolveColorCardTraceAntiBandingMix(
            color = 0f,
            tone = 0f,
            saturation = 0f,
            brightness = 0f,
            paletteStrength = 0f
        )

        assertEquals(0f, mix, 0.0001f)
    }

    @Test
    fun `anti banding mix rises for stronger grading pressure`() {
        val mix = resolveColorCardTraceAntiBandingMix(
            color = 0.3f,
            tone = 0.85f,
            saturation = 0.4f,
            brightness = 0.35f,
            paletteStrength = 0.8f
        )

        assertTrue(mix > 0.5f)
    }

    @Test
    fun `anti banding perturbs smooth gradients but leaves textured edges alone`() {
        val width = 8
        val height = 8
        val smoothPixels = IntArray(width * height) { 0xFF88B8FF.toInt() }
        val edgedPixels = IntArray(width * height) { index ->
            if ((index % width) < (width / 2)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val rgb = ColorCardTraceRgb(136, 184, 255)
        val smoothChanged = buildList {
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    add(
                        applyColorCardTraceAntiBanding(
                            rgb = rgb,
                            x = x,
                            y = y,
                            width = width,
                            height = height,
                            referencePixels = smoothPixels,
                            mix = 1f
                        )
                    )
                }
            }
        }.any { it != rgb }
        val edgedStable = buildList {
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    add(
                        applyColorCardTraceAntiBanding(
                            rgb = rgb,
                            x = x,
                            y = y,
                            width = width,
                            height = height,
                            referencePixels = edgedPixels,
                            mix = 1f
                        )
                    )
                }
            }
        }.all { it == rgb }

        assertTrue(smoothChanged)
        assertTrue(edgedStable)
    }
}
