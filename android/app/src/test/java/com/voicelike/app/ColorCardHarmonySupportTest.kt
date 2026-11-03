package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardHarmonySupportTest {

    @Test
    fun `complementary color of red is cyan family`() {
        val harmony = resolveColorCardHarmonySwatches(0xFFFF0000.toInt())

        assertEquals(
            "#00FFFF",
            harmony.first { it.label == COLOR_CARD_HARMONY_LABEL_COMPLEMENT }.hex
        )
    }

    @Test
    fun `analogous colors keep expected warm neighbors for red`() {
        val harmony = resolveColorCardHarmonySwatches(0xFFFF0000.toInt())

        val left = harmony.first { it.label == COLOR_CARD_HARMONY_LABEL_ANALOGOUS_A }.hex
        val right = harmony.first { it.label == COLOR_CARD_HARMONY_LABEL_ANALOGOUS_B }.hex

        assertEquals("#FF0080", left)
        assertEquals("#FF8000", right)
    }

    @Test
    fun `neutral colors still produce visible harmony swatches`() {
        val harmony = resolveColorCardHarmonySwatches(0xFF808080.toInt())

        assertTrue(harmony.all { it.hex.startsWith("#") && it.hex.length == 7 })
        assertTrue(harmony.distinctBy { it.hex }.size >= 3)
    }
}
