package com.voicelike.app

import android.content.Context
import android.graphics.Color as AndroidColor
import org.json.JSONObject

private const val COLOR_CARD_PALETTEER_ASSET = "color_card_paletteer_palettes_d.json"

data class ColorCardPaletteerPaletteEntry(
    val index: Int,
    val collectionName: String,
    val paletteName: String,
    val colors: List<Int>,
    val sourceTag: String = "#\u7075\u611f",
    val feedCycleStride: Int = 1
)

private var cachedColorCardPaletteerEntries: List<ColorCardPaletteerPaletteEntry>? = null

internal fun loadColorCardPaletteerInspiration(context: Context): List<ColorCardPaletteerPaletteEntry> {
    cachedColorCardPaletteerEntries?.let { return it }
    synchronized(COLOR_CARD_PALETTEER_ASSET) {
        cachedColorCardPaletteerEntries?.let { return it }
        val loaded = runCatching {
            context.assets.open(COLOR_CARD_PALETTEER_ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
                parseColorCardPaletteerEntries(JSONObject(reader.readText()))
            }
        }.getOrElse { emptyList() }
        cachedColorCardPaletteerEntries = loaded
        return loaded
    }
}

private fun parseColorCardPaletteerEntries(root: JSONObject): List<ColorCardPaletteerPaletteEntry> {
    val result = ArrayList<ColorCardPaletteerPaletteEntry>()
    val collectionNames = root.keys().asSequence().toList().sorted()
    var nextIndex = 1
    for (collectionName in collectionNames) {
        val paletteGroup = root.optJSONObject(collectionName) ?: continue
        val paletteNames = paletteGroup.keys().asSequence().toList().sorted()
        for (paletteName in paletteNames) {
            val paletteArray = paletteGroup.optJSONArray(paletteName) ?: continue
            val colors = ArrayList<Int>(paletteArray.length())
            for (colorIndex in 0 until paletteArray.length()) {
                val colorToken = paletteArray.optString(colorIndex)
                parsePaletteerHexColor(colorToken)?.let(colors::add)
            }
            if (colors.size < 2) continue
            result += ColorCardPaletteerPaletteEntry(
                index = nextIndex++,
                collectionName = collectionName,
                paletteName = paletteName,
                colors = colors
            )
        }
    }
    return result
}

private fun parsePaletteerHexColor(token: String): Int? {
    val normalized = token.trim()
        .removePrefix("#")
        .uppercase()

    if (normalized.length != 6 || normalized.any { it !in '0'..'9' && it !in 'A'..'F' }) {
        return null
    }

    return runCatching {
        AndroidColor.parseColor("#$normalized")
    }.getOrNull()
}
