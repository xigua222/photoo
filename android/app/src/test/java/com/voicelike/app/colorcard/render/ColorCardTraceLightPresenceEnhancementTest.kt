package com.voicelike.app.colorcard.render

import com.voicelike.app.ColorCardTraceRgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardTraceLightPresenceEnhancementTest {

    @Test
    fun `light presence enhancement leaves low luminance pixels unchanged`() {
        val rgb = ColorCardTraceRgb(40, 52, 64)
        val pixels = IntArray(25) { 0xFF283440.toInt() }

        val enhanced = applyColorCardTraceLightPresenceEnhancement(
            rgb = rgb,
            x = 2,
            y = 2,
            width = 5,
            height = 5,
            referencePixels = pixels
        )

        assertEquals(rgb, enhanced)
    }

    @Test
    fun `light presence enhancement gently boosts bright local highlights`() {
        val rgb = ColorCardTraceRgb(180, 210, 238)
        val pixels = IntArray(25) { 0xFF9BC4E8.toInt() }.also { buffer ->
            buffer[(2 * 5) + 2] = 0xFFCBE8FF.toInt()
        }

        val enhanced = applyColorCardTraceLightPresenceEnhancement(
            rgb = rgb,
            x = 2,
            y = 2,
            width = 5,
            height = 5,
            referencePixels = pixels
        )

        assertTrue(enhanced.red >= rgb.red)
        assertTrue(enhanced.green >= rgb.green)
        assertTrue(enhanced.blue >= rgb.blue)
    }

    @Test
    fun `light presence enhancement preserves hue ratio when not clipping`() {
        val rgb = ColorCardTraceRgb(140, 175, 210)
        val pixels = IntArray(25) { 0xFF9BBDE0.toInt() }

        val enhanced = applyColorCardTraceLightPresenceEnhancement(
            rgb = rgb,
            x = 2,
            y = 2,
            width = 5,
            height = 5,
            referencePixels = pixels
        )

        assertTrue(enhanced.red >= rgb.red)
        val originalRatio = rgb.red.toFloat() / rgb.green.toFloat()
        val enhancedRatio = enhanced.red.toFloat() / enhanced.green.toFloat()
        assertEquals(originalRatio, enhancedRatio, 0.01f)
    }
}
