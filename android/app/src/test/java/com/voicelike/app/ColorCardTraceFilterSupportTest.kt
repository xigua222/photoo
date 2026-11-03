package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.unit.IntSize
import com.voicelike.app.colorcard.portrait.COLOR_CARD_TRACE_PORTRAIT_DISABLED
import com.voicelike.app.colorcard.portrait.ColorCardPortraitOptimizationParams
import com.voicelike.app.colorcard.portrait.resolveColorCardPortraitSharpen

class ColorCardTraceFilterSupportTest {

    @Test
    fun `display ordering pushes p series late and separates them when possible`() {
        val ordered = orderColorCardTraceCandidateIdsForDisplay(
            listOf(
                "builtin_pixel:pixel-yellow",
                "builtin_pixel:pixel-red",
                "builtin_hald:cinematic",
                "builtin_hald:fg-cinebright",
                "builtin_hald:fj",
                "builtin_hald:classic-neg",
                "builtin_hald:provia",
                "builtin_hald:moewarm"
            )
        )

        assertFalse(isColorCardTracePSeriesCandidateId(ordered.first()))

        val firstPIndex = ordered.indexOfFirst(::isColorCardTracePSeriesCandidateId)
        assertTrue(firstPIndex >= 0)
        assertTrue(firstPIndex >= ordered.size / 2)

        ordered.zipWithNext().forEach { (left, right) ->
            assertFalse(
                isColorCardTracePSeriesCandidateId(left) &&
                    isColorCardTracePSeriesCandidateId(right)
            )
        }
    }

    @Test
    fun `cycle ordering avoids p series as first candidate when regular filters exist`() {
        val ordered = buildColorCardTraceCycleOrder(
            candidateIds = listOf(
                "builtin_pixel:pixel-yellow",
                "builtin_pixel:pixel-red",
                "builtin_hald:cinematic",
                "builtin_hald:fg-cinebright",
                "builtin_hald:fj"
            ),
            seed = 42L
        )

        assertFalse(isColorCardTracePSeriesCandidateId(ordered.first()))
    }

    @Test
    fun `preferred builtin candidate skips p series when possible`() {
        val preferred = preferredColorCardTraceBuiltinCandidateId(
            listOf(
                "builtin_pixel:pixel-yellow",
                "builtin_pixel:pixel-red",
                "builtin_hald:cinematic"
            )
        )

        assertEquals("builtin_hald:cinematic", preferred)
    }

    @Test
    fun `random builtin candidate skips p series when regular filters exist`() {
        val candidateId = randomColorCardTraceBuiltinCandidateId(
            candidateIds = listOf(
                "builtin_pixel:pixel-yellow",
                "builtin_pixel:pixel-red",
                "builtin_hald:cinematic",
                "builtin_hald:fj",
                "builtin_hald:classic-neg"
            ),
            seed = 42L
        )

        assertTrue(candidateId in setOf("builtin_hald:cinematic", "builtin_hald:fj", "builtin_hald:classic-neg"))
        assertFalse(isColorCardTracePSeriesCandidateId(candidateId.orEmpty()))
    }

    @Test
    fun `filter creative response is softened only in filter mode`() {
        val candidate = ColorCardTraceFilterCandidate(
            paletteId = "builtin_hald:cinematic",
            kind = ColorCardTraceFilterKind.HaldClut,
            title = "#001A",
            sourceTag = "Style",
            colors = listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
            score = -1.0
        )

        val filterResponse = resolveColorCardTraceFilterCreativeResponse(
            panelMode = ColorCardTracePanelMode.Filter,
            selectedCandidate = candidate,
            color = 1f,
            tone = 1f
        )
        val traceResponse = resolveColorCardTraceFilterCreativeResponse(
            panelMode = ColorCardTracePanelMode.Trace,
            selectedCandidate = candidate,
            color = 1f,
            tone = 1f
        )

        assertEquals(0.62f, filterResponse.first, 0.0001f)
        assertEquals(0.58f, filterResponse.second, 0.0001f)
        assertEquals(1f, traceResponse.first, 0.0001f)
        assertEquals(1f, traceResponse.second, 0.0001f)
    }

    @Test
    fun `effective face regions are disabled for p series filters`() {
        val faceRegions = listOf(
            ColorCardTraceFaceRegion(
                centerX = 0.5f,
                centerY = 0.5f,
                radiusX = 0.2f,
                radiusY = 0.2f
            )
        )
        val pCandidate = ColorCardTraceFilterCandidate(
            paletteId = "builtin_pixel:pixel-yellow",
            kind = ColorCardTraceFilterKind.PixelPoster,
            title = "#101P",
            sourceTag = "Pixel Poster",
            colors = listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
            score = -1.0
        )

        val resolved = resolveColorCardTraceEffectiveFaceRegions(
            panelMode = ColorCardTracePanelMode.Filter,
            selectedBuiltinCandidate = pCandidate,
            detectedFaceRegions = faceRegions
        )

        assertTrue(resolved.isEmpty())
    }

    @Test
    fun `xy pad visible detent centers match touch snapping`() {
        val padSize = IntSize(width = 220, height = 180)
        val geometry = resolveColorCardTraceAdjustmentPadGeometry(
            width = padSize.width.toFloat(),
            height = padSize.height.toFloat()
        )

        val center = resolveColorCardTraceAdjustmentPadDetentCenter(
            geometry = geometry,
            xIndex = 7,
            yIndex = 3
        )
        val snapped = snapColorCardTraceAdjustmentFromTouch(
            touchX = center.x,
            touchY = center.y,
            padSize = padSize
        )

        assertEquals(7, snapped.xIndex)
        assertEquals(3, snapped.yIndex)
        assertEquals(0.7f, snapped.normalizedX, 0.0001f)
        assertEquals(0.3f, snapped.normalizedY, 0.0001f)
    }

    @Test
    fun `portrait sharpen only reduces positive sharpen when enabled`() {
        val params = ColorCardPortraitOptimizationParams(
            enabled = true,
            lightnessLift = 2f,
            saturationBoost = 0.02f,
            sharpenRetention = 0.6f,
            softenAmount = 0f
        )

        val softened = resolveColorCardPortraitSharpen(1f, 1f, params)
        val untouchedNegative = resolveColorCardPortraitSharpen(-0.4f, 1f, params)
        val disabled = resolveColorCardPortraitSharpen(1f, 1f, COLOR_CARD_TRACE_PORTRAIT_DISABLED)

        assertEquals(0.6f, softened, 0.0001f)
        assertEquals(-0.4f, untouchedNegative, 0.0001f)
        assertEquals(1f, disabled, 0.0001f)
    }

    @Test
    fun `ai exit overlay stays hidden when replacement preview is ready`() {
        val shouldShow = shouldShowColorCardTraceAiExitOverlay(
            effectModeVisible = true,
            selectedCandidateKind = ColorCardTraceFilterKind.HaldClut,
            replacementPreviewAvailable = true
        )

        assertFalse(shouldShow)
    }

    @Test
    fun `ai exit overlay is only a fallback for non ai filters without replacement preview`() {
        val shouldShow = shouldShowColorCardTraceAiExitOverlay(
            effectModeVisible = true,
            selectedCandidateKind = ColorCardTraceFilterKind.HaldClut,
            replacementPreviewAvailable = false
        )

        assertTrue(shouldShow)
        assertFalse(
            shouldShowColorCardTraceAiExitOverlay(
                effectModeVisible = true,
                selectedCandidateKind = ColorCardTraceFilterKind.AiMnn,
                replacementPreviewAvailable = false
            )
        )
    }
}
