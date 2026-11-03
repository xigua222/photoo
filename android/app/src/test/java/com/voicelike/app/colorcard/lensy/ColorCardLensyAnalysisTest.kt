package com.voicelike.app.colorcard.lensy

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardLensyAnalysisTest {

    @Test
    fun `assessment prefers combined highlight and shadow warning`() {
        val assessment = buildColorCardLensyAssessment(
            ColorCardLensyFrameStats(
                pixelCount = 10_000,
                luminanceHistogram = histogram(
                    0 to 180,
                    2 to 110,
                    8 to 130,
                    250 to 400,
                    254 to 380,
                    255 to 380,
                    128 to 8_420
                ),
                redHistogram = histogram(128 to 10_000),
                greenHistogram = histogram(128 to 10_000),
                blueHistogram = histogram(128 to 10_000),
                highlightClipRatio = 0.080f,
                shadowClipRatio = 0.042f,
                redHighClipRatio = 0.080f,
                greenHighClipRatio = 0.080f,
                blueHighClipRatio = 0.080f,
                redLowClipRatio = 0.042f,
                greenLowClipRatio = 0.042f,
                blueLowClipRatio = 0.042f,
                saturationOverflowRatio = 0.010f
            )
        )

        assertNotNull(assessment?.message)
        assertTrue(assessment?.message?.contains("先") == true || assessment?.message?.contains("建议") == true)
    }

    @Test
    fun `assessment stays quiet for balanced frame`() {
        val assessment = buildColorCardLensyAssessment(
            ColorCardLensyFrameStats(
                pixelCount = 10_000,
                luminanceHistogram = histogram(
                    24 to 500,
                    52 to 1_200,
                    96 to 2_100,
                    128 to 2_300,
                    172 to 2_000,
                    210 to 1_500,
                    236 to 400
                ),
                redHistogram = histogram(100 to 10_000),
                greenHistogram = histogram(110 to 10_000),
                blueHistogram = histogram(120 to 10_000),
                highlightClipRatio = 0.0004f,
                shadowClipRatio = 0.0003f,
                redHighClipRatio = 0.0004f,
                greenHighClipRatio = 0.0004f,
                blueHighClipRatio = 0.0004f,
                redLowClipRatio = 0.0003f,
                greenLowClipRatio = 0.0003f,
                blueLowClipRatio = 0.0003f,
                saturationOverflowRatio = 0.010f
            )
        )

        assertNull(assessment)
    }

    @Test
    fun `assessment stays quiet for moderate highlight pressure`() {
        val assessment = buildColorCardLensyAssessment(
            ColorCardLensyFrameStats(
                pixelCount = 10_000,
                luminanceHistogram = histogram(
                    24 to 300,
                    64 to 1_600,
                    128 to 5_500,
                    220 to 1_900,
                    250 to 320,
                    254 to 190,
                    255 to 190
                ),
                redHistogram = histogram(128 to 10_000),
                greenHistogram = histogram(128 to 10_000),
                blueHistogram = histogram(128 to 10_000),
                highlightClipRatio = 0.029f,
                shadowClipRatio = 0.002f,
                redHighClipRatio = 0.029f,
                greenHighClipRatio = 0.029f,
                blueHighClipRatio = 0.029f,
                redLowClipRatio = 0.002f,
                greenLowClipRatio = 0.002f,
                blueLowClipRatio = 0.002f,
                saturationOverflowRatio = 0.020f
            )
        )

        assertNull(assessment)
    }

    private fun histogram(vararg entries: Pair<Int, Int>): IntArray {
        val histogram = IntArray(256)
        entries.forEach { (value, count) ->
            histogram[value] = count
        }
        return histogram
    }
}
