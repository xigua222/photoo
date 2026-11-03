package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardRecipeSupportTest {

    @Test
    fun `shared recipe text round trips through parser`() {
        val recipe = SavedColorCardRecipe(
            id = "recipe-1",
            createdAt = 1_740_000_000_000L,
            mode = ColorCardRecipeMode.Trace,
            title = "追色 · #321A",
            paletteId = "palette-1",
            candidateKind = ColorCardTraceFilterKind.Palette.name,
            candidateTitle = "#321A",
            sourceTag = "自定义色卡",
            colors = listOf(0xFF112233.toInt(), 0xFF445566.toInt()),
            color = 0.2f,
            tone = -0.1f,
            sharpen = 0.3f,
            saturation = 0.1f,
            brightness = -0.2f,
            paletteStrength = 0.7f,
            portraitVitality = 0.4f,
            portraitSoftness = 0.2f,
            portraitDetailSuppression = 0.3f
        )

        val sharedText = buildColorCardRecipeShareText(listOf(recipe))
        val parsed = parseSharedColorCardRecipes(sharedText)

        assertEquals(1, parsed.size)
        assertEquals(recipe.mode, parsed.first().mode)
        assertEquals(recipe.title, parsed.first().title)
        assertEquals(recipe.candidateTitle, parsed.first().candidateTitle)
        assertEquals(recipe.colors, parsed.first().colors)
    }

    @Test
    fun `parser tolerates copied token whitespace`() {
        val recipe = SavedColorCardRecipe(
            id = "recipe-2",
            createdAt = 1_740_000_000_001L,
            mode = ColorCardRecipeMode.Filter,
            title = "滤镜 · #654F",
            paletteId = "builtin_hald:cinematic",
            candidateKind = ColorCardTraceFilterKind.HaldClut.name,
            candidateTitle = "#654F",
            sourceTag = "内置 LUT",
            colors = listOf(0xFF000000.toInt()),
            color = 0f,
            tone = 0f,
            sharpen = 0f,
            saturation = 0f,
            brightness = 0f,
            paletteStrength = 0.5f
        )

        val sharedText = buildColorCardRecipeShareText(listOf(recipe))
        val paddedText = sharedText.replace(
            "PHOTOO_RECIPE_V1:",
            "PHOTOO_RECIPE_V1:\n"
        )

        val parsed = parseSharedColorCardRecipes(paddedText)

        assertEquals(1, parsed.size)
        assertEquals(recipe.title, parsed.first().title)
    }

    @Test
    fun `shared recipe text keeps visible summary single entry`() {
        val recipe = SavedColorCardRecipe(
            id = "recipe-3",
            createdAt = 1_740_000_000_002L,
            mode = ColorCardRecipeMode.Filter,
            title = "滤镜 · #777A",
            paletteId = "builtin_hald:cinematic",
            candidateKind = ColorCardTraceFilterKind.HaldClut.name,
            candidateTitle = "#777A",
            sourceTag = "内置 LUT",
            colors = listOf(0xFF000000.toInt()),
            color = 0f,
            tone = 0f,
            sharpen = 0f,
            saturation = 0f,
            brightness = 0f,
            paletteStrength = 0.5f
        )

        val sharedText = buildColorCardRecipeShareText(listOf(recipe))

        assertTrue(sharedText.contains("PHOTOO_RECIPE_V1:"))
        assertTrue(sharedText.contains(recipe.title))
    }

    @Test
    fun `parser accepts branded copied recipe text`() {
        val recipe = SavedColorCardRecipe(
            id = "recipe-4",
            createdAt = 1_740_000_000_003L,
            mode = ColorCardRecipeMode.Trace,
            title = "追色 路 #998A",
            paletteId = "palette-998a",
            candidateKind = ColorCardTraceFilterKind.Palette.name,
            candidateTitle = "#998A",
            sourceTag = "自定义色卡",
            colors = listOf(0xFF123456.toInt(), 0xFF345678.toInt()),
            color = 0.15f,
            tone = -0.2f,
            sharpen = 0.05f,
            saturation = 0.18f,
            brightness = -0.1f,
            paletteStrength = 0.62f
        )

        val brandedText = ensurePhotooColorCardBrand(
            buildColorCardRecipeShareText(listOf(recipe))
        )

        val parsed = parseSharedColorCardRecipes(brandedText)

        assertEquals(1, parsed.size)
        assertEquals(recipe.title, parsed.first().title)
        assertEquals(recipe.colors, parsed.first().colors)
    }

    @Test
    fun `ensure brand keeps recipe share text unchanged`() {
        val recipe = SavedColorCardRecipe(
            id = "recipe-4b",
            createdAt = 1_740_000_000_003L,
            mode = ColorCardRecipeMode.Trace,
            title = "追色 · #998A",
            paletteId = "palette-998a",
            candidateKind = ColorCardTraceFilterKind.Palette.name,
            candidateTitle = "#998A",
            sourceTag = "自定义色卡",
            colors = listOf(0xFF123456.toInt(), 0xFF345678.toInt()),
            color = 0.15f,
            tone = -0.2f,
            sharpen = 0.05f,
            saturation = 0.18f,
            brightness = -0.1f,
            paletteStrength = 0.62f
        )

        val sharedText = buildColorCardRecipeShareText(listOf(recipe))

        assertEquals(sharedText, ensurePhotooColorCardBrand(sharedText))
    }

    @Test
    fun `parser accepts legacy obfuscated release token`() {
        val sharedText = """
            PHOTOO 配方
            数量: 1

            滤镜 · #234F
            模式: 滤镜
            目标: #234F · 内置 Hald

            PHOTOO_RECIPE_V1:eyJhIjpbeyJhIjoiRmlsdGVyIiwiYiI6Iua7pOmVnCDCtyAjMjM0RiIsImMiOiJidWlsdGluX2hhbGQ6Y2MyIiwiZCI6IkhhbGRDbHV0IiwiZSI6IiMyMzRGIiwiZiI6IuWGhee9riBIYWxkIiwiZyI6Wy0xMjcwMjE2NCwtNzE3NjU3OSwtMzYyNTMwNSwtMTIwMjMxMTksLTIzNjgyODNdLCJoIjowLjAsImkiOjAuMCwiaiI6MC4wLCJrIjowLjAsImwiOjAuMCwibSI6MC41LCJuIjowLjAsIm8iOjAuMCwicCI6MC4wLCJxIjpmYWxzZSwiciI6ImZpbG0tZ3JhaW4tY29yZS0wMSJ9XX0
        """.trimIndent()

        val parsed = parseSharedColorCardRecipes(sharedText)

        assertEquals(1, parsed.size)
        assertEquals(ColorCardRecipeMode.Filter, parsed.first().mode)
        assertEquals("builtin_hald:cc2", parsed.first().paletteId)
        assertEquals("#234F", parsed.first().candidateTitle)
    }

    @Test
    fun `parser tolerates extra footer after token`() {
        val recipe = SavedColorCardRecipe(
            id = "recipe-5",
            createdAt = 1_740_000_000_004L,
            mode = ColorCardRecipeMode.Filter,
            title = "滤镜 路 #443B",
            paletteId = "builtin_hald:cinematic",
            candidateKind = ColorCardTraceFilterKind.HaldClut.name,
            candidateTitle = "#443B",
            sourceTag = "内置 LUT",
            colors = listOf(0xFF000000.toInt()),
            color = 0f,
            tone = 0f,
            sharpen = 0f,
            saturation = 0f,
            brightness = 0f,
            paletteStrength = 0.5f
        )

        val sharedText = buildColorCardRecipeShareText(listOf(recipe)) + "\n\nShared via PHOTOO"

        val parsed = parseSharedColorCardRecipes(sharedText)

        assertEquals(1, parsed.size)
        assertEquals(recipe.title, parsed.first().title)
    }
}
