package com.voicelike.app

import android.graphics.Color as AndroidColor

data class ColorCardNordPaletteEntry(
    val index: Int,
    val title: String,
    val subtitle: String,
    val colors: List<Int>
)

internal fun loadColorCardNordInspiration(): List<ColorCardNordPaletteEntry> {
    return listOf(
        ColorCardNordPaletteEntry(
            index = 1,
            title = "Polar Night",
            subtitle = "4 \u7ec4\u6df1\u8272\u57fa\u5e95\uff0c\u9002\u5408\u80cc\u666f\u4e0e\u7a33\u5b9a\u5bf9\u6bd4",
            colors = listOf(
                nordColor("#2E3440"),
                nordColor("#3B4252"),
                nordColor("#434C5E"),
                nordColor("#4C566A")
            )
        ),
        ColorCardNordPaletteEntry(
            index = 2,
            title = "Snow Storm",
            subtitle = "3 \u7ec4\u4eae\u8272\u57fa\u5e95\uff0c\u9002\u5408\u6587\u5b57\u4e0e\u4eae\u9762 UI",
            colors = listOf(
                nordColor("#D8DEE9"),
                nordColor("#E5E9F0"),
                nordColor("#ECEFF4")
            )
        ),
        ColorCardNordPaletteEntry(
            index = 3,
            title = "Frost",
            subtitle = "4 \u7ec4\u84dd\u7cfb\u4e3b\u8272\uff0c\u662f Nord \u6700\u5178\u578b\u7684 accent \u533a",
            colors = listOf(
                nordColor("#8FBCBB"),
                nordColor("#88C0D0"),
                nordColor("#81A1C1"),
                nordColor("#5E81AC")
            )
        ),
        ColorCardNordPaletteEntry(
            index = 4,
            title = "Aurora",
            subtitle = "5 \u7ec4\u7f8e\u5149\u8272\uff0c\u7528\u4e8e\u72b6\u6001\u8272\u4e0e\u5f3a\u70c8\u70b9\u7f00",
            colors = listOf(
                nordColor("#BF616A"),
                nordColor("#D08770"),
                nordColor("#EBCB8B"),
                nordColor("#A3BE8C"),
                nordColor("#B48EAD")
            )
        )
    )
}

private fun nordColor(hex: String): Int {
    return AndroidColor.parseColor(hex)
}
