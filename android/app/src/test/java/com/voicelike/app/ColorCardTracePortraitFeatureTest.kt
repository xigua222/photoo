package com.voicelike.app

import com.voicelike.app.colorcard.portrait.ColorCardPortraitOptimizationParams
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardTracePortraitFeatureTest {

    @Test
    fun `portrait feature params respond strongly at mid slider values`() {
        val params = resolveColorCardTracePortraitFeatureParams(
            enabled = true,
            faceRegions = listOf(
                ColorCardTraceFaceRegion(
                    centerX = 0.5f,
                    centerY = 0.5f,
                    radiusX = 0.2f,
                    radiusY = 0.24f
                )
            ),
            baseParams = ColorCardPortraitOptimizationParams(
                enabled = true,
                lightnessLift = 3.2f,
                saturationBoost = 0.03f,
                sharpenRetention = 0.72f,
                softenAmount = 0f
            ),
            adjustments = ColorCardTracePortraitAdjustments(
                vitality = 0.5f,
                softness = 0.5f,
                detailSuppression = 0.5f
            )
        )

        assertTrue(params.enabled)
        assertTrue(params.lightnessLift > 7.0f)
        assertTrue(params.saturationBoost > 0.10f)
        assertTrue(params.softenAmount > 0.75f)
        assertTrue(params.sharpenRetention < 0.35f)
    }
}
