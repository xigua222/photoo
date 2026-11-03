package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ColorCardStoreSnapshotTest {

    @Test
    fun snapshotSelectionPrefersNonEmptyMirrorOverEmptyFile() {
        val mirrored = listOf(
            palette(id = "mirror", createdAt = 20L, colors = listOf(0xFF112233.toInt()))
        )

        val selected = selectColorCardPaletteSnapshot(
            filePalettes = emptyList(),
            mirroredPalettes = mirrored
        )

        assertEquals(mirrored, selected)
    }

    @Test
    fun snapshotSelectionPrefersNewerSnapshot() {
        val olderFile = listOf(
            palette(id = "file", createdAt = 20L, colors = listOf(0xFF112233.toInt()))
        )
        val newerMirror = listOf(
            palette(id = "mirror", createdAt = 30L, colors = listOf(0xFF445566.toInt()))
        )

        val selected = selectColorCardPaletteSnapshot(
            filePalettes = olderFile,
            mirroredPalettes = newerMirror
        )

        assertEquals(newerMirror, selected)
    }

    @Test
    fun codecRoundTripPreservesGradientAndFavoriteFields() {
        val original = listOf(
            SavedColorCardPalette(
                id = "gradient",
                createdAt = 42L,
                sourceMediaId = 7L,
                colors = listOf(0xFF112233.toInt(), 0xFF445566.toInt()),
                title = "Test",
                sourceTag = "我的",
                useChineseFont = true,
                favoriteKey = "fav-key",
                gradientSpec = ColorCardGradientSpec(
                    angleDegrees = 135f,
                    stops = listOf(
                        ColorCardGradientStop(colorInt = 0xFF112233.toInt(), positionPercent = 0f),
                        ColorCardGradientStop(colorInt = 0xFF445566.toInt(), positionPercent = 100f)
                    )
                )
            )
        )

        val parsed = parseColorCardStoreSnapshot(serializeColorCardStoreSnapshot(original))

        assertNotNull(parsed.firstOrNull()?.gradientSpec)
        assertEquals(original, parsed)
    }

    private fun palette(
        id: String,
        createdAt: Long,
        colors: List<Int>
    ): SavedColorCardPalette {
        return SavedColorCardPalette(
            id = id,
            createdAt = createdAt,
            sourceMediaId = null,
            colors = colors
        )
    }
}
