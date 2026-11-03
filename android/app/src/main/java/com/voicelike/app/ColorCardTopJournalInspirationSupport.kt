package com.voicelike.app

import android.content.Context
import android.graphics.Color as AndroidColor

private const val COLOR_CARD_TOP_JOURNAL_ASSET = "color_card_top_journal_palettes.txt"

data class ColorCardTopJournalPaletteEntry(
    val index: Int,
    val colors: List<Int>
) {
    val title: String
        get() = "\u7b2c ${index.toString().padStart(3, '0')} \u7ec4"
}

private var cachedColorCardTopJournalEntries: List<ColorCardTopJournalPaletteEntry>? = null

internal fun loadColorCardTopJournalInspiration(context: Context): List<ColorCardTopJournalPaletteEntry> {
    cachedColorCardTopJournalEntries?.let { return it }
    synchronized(COLOR_CARD_TOP_JOURNAL_ASSET) {
        cachedColorCardTopJournalEntries?.let { return it }
        val loaded = runCatching {
            context.assets.open(COLOR_CARD_TOP_JOURNAL_ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.mapIndexedNotNull { index, rawLine ->
                    parseColorCardTopJournalEntry(
                        rawLine = rawLine,
                        index = index + 1
                    )
                }.toList()
            }
        }.getOrElse { emptyList() }
        cachedColorCardTopJournalEntries = loaded
        return loaded
    }
}

private fun parseColorCardTopJournalEntry(
    rawLine: String,
    index: Int
): ColorCardTopJournalPaletteEntry? {
    val colors = rawLine
        .split(',')
        .mapNotNull { token -> parseColorCardHexColor(token) }

    if (colors.size < 2) return null

    return ColorCardTopJournalPaletteEntry(
        index = index,
        colors = colors
    )
}

private fun parseColorCardHexColor(token: String): Int? {
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
