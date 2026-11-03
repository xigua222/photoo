package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorCardBuiltinFilterRegistryTest {

    @Test
    fun `primary pixel poster variants keep stable display codes`() {
        assertEquals("#105P", resolveColorCardTracePixelPosterDisplayCode("pixel-primary-yellow"))
        assertEquals("#106P", resolveColorCardTracePixelPosterDisplayCode("pixel-primary-blue"))
        assertEquals("#107P", resolveColorCardTracePixelPosterDisplayCode("pixel-primary-red"))
        assertEquals("#108P", resolveColorCardTracePixelPosterDisplayCode("pixel-original"))
    }

    @Test
    fun `film simulation titles collapse into stable family identity`() {
        val identity = resolveColorCardBuiltinHaldIdentity(
            rawId = "c200-1",
            title = "C200+1",
            sourceTag = "内置 Hald"
        )

        assertEquals("film-sim-c200", identity.canonicalId)
        assertEquals("#202F", identity.displayCode)
        assertEquals("胶片模拟", identity.sourceTag)
        assertEquals("C200", identity.familyLabel)
        assertEquals("+1", identity.variantLabel)
        assertEquals(1, identity.variantValue)
    }

    @Test
    fun `j series filters keep independent stable display codes`() {
        val identity = resolveColorCardBuiltinHaldIdentity(
            rawId = "j-fresh-bright-3",
            title = "清新透亮",
            sourceTag = "清新透亮"
        )

        assertEquals("j-fresh-bright-3", identity.canonicalId)
        assertEquals("#503J", identity.displayCode)
        assertEquals("清新透亮", identity.sourceTag)
        assertNull(identity.familyLabel)
        assertNull(identity.variantLabel)
    }

    @Test
    fun `implicit hald blend picks standard at midpoint and interpolates upward near high strength`() {
        val candidate = ColorCardTraceFilterCandidate(
            paletteId = "builtin_hald:film-sim-c200",
            kind = ColorCardTraceFilterKind.HaldClut,
            title = "#202F",
            sourceTag = "胶片模拟",
            colors = emptyList(),
            score = -1.0,
            haldVariants = listOf(
                ColorCardTraceHaldVariant("minus-2.webp", 8, emptyList(), -1f, "-2"),
                ColorCardTraceHaldVariant("minus-1.webp", 8, emptyList(), -0.5f, "-1"),
                ColorCardTraceHaldVariant("standard.webp", 8, emptyList(), 0f, "S"),
                ColorCardTraceHaldVariant("plus-1.webp", 8, emptyList(), 0.5f, "+1"),
                ColorCardTraceHaldVariant("plus-2.webp", 8, emptyList(), 1f, "+2")
            )
        )

        val midpoint = resolveColorCardTraceHaldBlend(candidate, paletteStrength = 0.5f)
        assertEquals("standard.webp", midpoint.primaryAssetName)
        assertNull(midpoint.secondaryAssetName)
        assertEquals(0f, midpoint.variantMix, 0.0001f)

        val upper = resolveColorCardTraceHaldBlend(candidate, paletteStrength = 0.75f)
        assertEquals("plus-1.webp", upper.primaryAssetName)
        assertNull(upper.secondaryAssetName)
        assertEquals(0f, upper.variantMix, 0.0001f)

        val between = resolveColorCardTraceHaldBlend(candidate, paletteStrength = 0.625f)
        assertEquals("standard.webp", between.primaryAssetName)
        assertEquals("plus-1.webp", between.secondaryAssetName)
        assertEquals(0.5f, between.variantMix, 0.0001f)
    }
}
