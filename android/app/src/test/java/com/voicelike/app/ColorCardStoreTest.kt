package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardStoreTest {

    @Test
    fun `alphabet suffix expands after z`() {
        assertEquals("A", encodeColorCardAlphabetSuffix(0))
        assertEquals("Z", encodeColorCardAlphabetSuffix(25))
        assertEquals("AA", encodeColorCardAlphabetSuffix(26))
        assertEquals("AB", encodeColorCardAlphabetSuffix(27))
        assertEquals("AZ", encodeColorCardAlphabetSuffix(51))
        assertEquals("BA", encodeColorCardAlphabetSuffix(52))
    }

    @Test
    fun `normalize user collected palettes assigns yymmdd plus alphabet titles`() {
        val palettes = listOf(
            SavedColorCardPalette(
                id = "older",
                createdAt = 1_741_824_000_000L,
                sourceMediaId = 1L,
                colors = listOf(0xFF112233.toInt())
            ),
            SavedColorCardPalette(
                id = "newer",
                createdAt = 1_741_824_100_000L,
                sourceMediaId = null,
                colors = listOf(0xFF445566.toInt())
            ),
            SavedColorCardPalette(
                id = "named",
                createdAt = 1_741_824_200_000L,
                sourceMediaId = null,
                colors = listOf(0xFF778899.toInt()),
                title = "Keep"
            )
        )

        val normalized = normalizeUserCollectedPaletteTitles(palettes)

        assertEquals("250313A", normalized[0].title)
        assertEquals("250313B", normalized[1].title)
        assertEquals("Keep", normalized[2].title)
    }

    @Test
    fun `only untitled local palettes receive auto names`() {
        assertTrue(
            shouldAutoNameUserCollectedPalette(
                title = null,
                sourceTag = null,
                favoriteKey = null,
                gradientSpec = null
            )
        )
        assertFalse(
            shouldAutoNameUserCollectedPalette(
                title = "Manual",
                sourceTag = null,
                favoriteKey = null,
                gradientSpec = null
            )
        )
        assertFalse(
            shouldAutoNameUserCollectedPalette(
                title = null,
                sourceTag = "NICE",
                favoriteKey = null,
                gradientSpec = null
            )
        )
    }
}
