package com.voicelike.app.colorcard.portrait

import com.voicelike.app.ColorCardTraceFaceRegion
import com.voicelike.app.ColorCardTraceFilterCandidate
import com.voicelike.app.ColorCardTraceFilterKind
import com.voicelike.app.ColorCardTracePanelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardPortraitOptimizationTest {

    @Test
    fun `portrait optimization stays disabled for pixel poster filters`() {
        val candidate = ColorCardTraceFilterCandidate(
            paletteId = "builtin_pixel:pixel-yellow",
            kind = ColorCardTraceFilterKind.PixelPoster,
            title = "#101P",
            sourceTag = "Pixel Poster",
            colors = listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
            score = -1.0
        )

        val params = resolveColorCardPortraitOptimizationParams(
            panelMode = ColorCardTracePanelMode.Filter,
            selectedCandidate = candidate,
            faceRegions = listOf(
                ColorCardTraceFaceRegion(
                    centerX = 0.5f,
                    centerY = 0.5f,
                    radiusX = 0.18f,
                    radiusY = 0.22f
                )
            )
        )

        assertFalse(params.enabled)
    }

    @Test
    fun `portrait optimization stays disabled for tiny faces`() {
        val params = resolveColorCardPortraitOptimizationParams(
            panelMode = ColorCardTracePanelMode.Trace,
            selectedCandidate = null,
            faceRegions = listOf(
                ColorCardTraceFaceRegion(
                    centerX = 0.5f,
                    centerY = 0.5f,
                    radiusX = 0.05f,
                    radiusY = 0.05f
                )
            )
        )

        assertFalse(params.enabled)
    }

    @Test
    fun `portrait optimization enables conservative values for reasonable face`() {
        val candidate = ColorCardTraceFilterCandidate(
            paletteId = "builtin_hald:cinematic",
            kind = ColorCardTraceFilterKind.HaldClut,
            title = "#001A",
            sourceTag = "Style",
            colors = listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
            score = -1.0
        )

        val params = resolveColorCardPortraitOptimizationParams(
            panelMode = ColorCardTracePanelMode.Filter,
            selectedCandidate = candidate,
            faceRegions = listOf(
                ColorCardTraceFaceRegion(
                    centerX = 0.5f,
                    centerY = 0.42f,
                    radiusX = 0.18f,
                    radiusY = 0.24f
                )
            )
        )

        assertTrue(params.enabled)
        assertTrue(params.lightnessLift in 2f..4.8f)
        assertTrue(params.saturationBoost in 0.015f..0.045f)
        assertTrue(params.sharpenRetention in 0.58f..1f)
    }

    @Test
    fun `portrait sharpen resolver only attenuates positive sharpening`() {
        val params = ColorCardPortraitOptimizationParams(
            enabled = true,
            lightnessLift = 2f,
            saturationBoost = 0.02f,
            sharpenRetention = 0.7f,
            softenAmount = 0f
        )

        assertEquals(0.79f, resolveColorCardPortraitSharpen(1f, 0.7f, params), 0.0001f)
        assertEquals(-0.5f, resolveColorCardPortraitSharpen(-0.5f, 1f, params), 0.0001f)
    }
}
