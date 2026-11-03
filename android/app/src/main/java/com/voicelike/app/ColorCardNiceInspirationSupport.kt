package com.voicelike.app

import android.content.Context
import android.graphics.Color as AndroidColor
import org.json.JSONArray

private const val COLOR_CARD_NICE_ASSET = "color_card_nice_palettes.json"

data class ColorCardNicePaletteEntry(
    val index: Int,
    val colors: List<Int>
) {
    val title: String
        get() = "NICE ${index.toString().padStart(3, '0')}"
}

private var cachedColorCardNiceEntries: List<ColorCardNicePaletteEntry>? = null

internal fun loadColorCardNiceInspiration(context: Context): List<ColorCardNicePaletteEntry> {
    cachedColorCardNiceEntries?.let { return it }
    synchronized(COLOR_CARD_NICE_ASSET) {
        cachedColorCardNiceEntries?.let { return it }
        val loaded = runCatching {
            context.assets.open(COLOR_CARD_NICE_ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
                parseColorCardNiceEntries(JSONArray(reader.readText()))
            }
        }.getOrElse { emptyList() }
        cachedColorCardNiceEntries = loaded
        return loaded
    }
}

private fun parseColorCardNiceEntries(root: JSONArray): List<ColorCardNicePaletteEntry> {
    val result = ArrayList<ColorCardNicePaletteEntry>(root.length())
    for (index in 0 until root.length()) {
        val paletteArray = root.optJSONArray(index) ?: continue
        val colors = ArrayList<Int>(paletteArray.length())
        for (colorIndex in 0 until paletteArray.length()) {
            val token = paletteArray.optString(colorIndex)
            parseColorCardNiceHexColor(token)?.let(colors::add)
        }
        if (colors.size < 2) continue
        result += ColorCardNicePaletteEntry(
            index = index + 1,
            colors = colors
        )
    }
    return result
}

private fun parseColorCardNiceHexColor(token: String): Int? {
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
