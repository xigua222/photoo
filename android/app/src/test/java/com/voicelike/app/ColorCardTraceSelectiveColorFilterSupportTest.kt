package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ColorCardTraceSelectiveColorFilterSupportTest {

    @Test
    fun `selective color candidates keep stable V titles`() {
        val candidatesById = buildColorCardTraceSelectiveColorCandidates().associateBy { it.paletteId }

        assertEquals("#111V", candidatesById["builtin_creative:selective-red"]?.title)
        assertEquals("#112V", candidatesById["builtin_creative:selective-green"]?.title)
    }

    @Test
    fun `selective color keeps target hues vivid and softens non target hues`() {
        val redCandidate = buildColorCardTraceSelectiveColorCandidates()
            .first { it.paletteId == "builtin_creative:selective-red" }
        val greenCandidate = buildColorCardTraceSelectiveColorCandidates()
            .first { it.paletteId == "builtin_creative:selective-green" }
        val redParams = resolveColorCardTraceSelectiveColorParams(redCandidate, paletteStrength = 1f)
        val greenParams = resolveColorCardTraceSelectiveColorParams(greenCandidate, paletteStrength = 1f)

        val warmRed = ColorCardTraceRgb(180, 72, 68)
        val leafyGreen = ColorCardTraceRgb(72, 170, 88)
        val coolBlue = ColorCardTraceRgb(32, 92, 220)

        val boostedRed = applyColorCardTraceSelectiveColorRgb(warmRed, redParams)
        val boostedGreen = applyColorCardTraceSelectiveColorRgb(leafyGreen, greenParams)
        val mutedBlue = applyColorCardTraceSelectiveColorRgb(coolBlue, redParams)
        val spread: (ColorCardTraceRgb) -> Int = { rgb ->
            max(rgb.red, max(rgb.green, rgb.blue)) - min(rgb.red, min(rgb.green, rgb.blue))
        }

        assertTrue(boostedRed.red > boostedRed.green)
        assertTrue(boostedRed.red > boostedRed.blue)
        assertTrue(spread(boostedRed) >= spread(warmRed))
        assertTrue(boostedGreen.green > boostedGreen.red)
        assertTrue(boostedGreen.green > boostedGreen.blue)
        assertTrue(spread(boostedGreen) >= spread(leafyGreen))
        assertTrue(spread(mutedBlue) < 40)
        assertEquals(1f, redParams.strength, 0.0001f)
        assertTrue(redParams.softMistStrength > 0f)
        assertEquals(0f, greenParams.softMistStrength, 0.0001f)
        assertTrue(redParams.enabled)
        assertTrue(greenParams.enabled)
    }
}
