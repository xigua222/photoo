package com.voicelike.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatColorCardTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy.MM.dd  HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun ensurePhotooColorCardCopyPrefix(text: String): String {
    val trimmed = text.trim()
    val prefix = "【photoo色卡】"
    if (trimmed.startsWith(prefix)) {
        return trimmed
    }
    if (trimmed.isBlank()) {
        return prefix
    }
    return buildString {
        append(prefix)
        append('\n')
        append('\n')
        append(trimmed)
    }
}

internal fun buildPaletteDetailText(palette: SavedColorCardPalette): String {
    return buildString {
        append("PHOTOO\u8272\u5361")
        if (!palette.sourceTag.isNullOrBlank() || !palette.title.isNullOrBlank()) {
            append('\n')
            append(
                listOfNotNull(
                    palette.sourceTag?.takeIf { it.isNotBlank() },
                    palette.title?.takeIf { it.isNotBlank() }
                ).joinToString(" ")
            )
        }
        append('\n')
        append("\u4fdd\u5b58\u65f6\u95f4: ")
        append(formatColorCardTimestamp(palette.createdAt))
        append('\n')
        append('\n')
        palette.colors.forEachIndexed { index, colorInt ->
            val metrics = colorCardMetrics(colorInt)
            append(index + 1)
            append(". #")
            append(metrics.hex)
            append('\n')
            append("RGB: ")
            append(metrics.rgb)
            append('\n')
            append("HSB: ")
            append(metrics.hsb)
            append('\n')
            append("CMYK: ")
            append(metrics.cmyk)
            if (index < palette.colors.lastIndex) {
                append('\n')
                append('\n')
            }
        }
        palette.gradientSpec?.let { gradientSpec ->
            append('\n')
            append('\n')
            append("CSS: ")
            append(buildColorCardGradientCss(gradientSpec))
            append('\n')
            append('\n')
            append("PHOTOO JSON:")
            append('\n')
            append(buildColorCardGradientRecipeJson(gradientSpec))
        }
    }
}

internal fun buildSingleColorDetailText(
    palette: SavedColorCardPalette,
    colorIndex: Int
): String {
    val safeIndex = colorIndex.coerceIn(0, palette.colors.lastIndex)
    val colorInt = palette.colors[safeIndex]
    val metrics = colorCardMetrics(colorInt)
    return buildString {
        append("PHOTOO\u8272\u5361\u989c\u8272 ")
        append(safeIndex + 1)
        append(" / ")
        append(palette.colors.size)
        if (!palette.sourceTag.isNullOrBlank() || !palette.title.isNullOrBlank()) {
            append('\n')
            append(
                listOfNotNull(
                    palette.sourceTag?.takeIf { it.isNotBlank() },
                    palette.title?.takeIf { it.isNotBlank() }
                ).joinToString(" ")
            )
        }
        append('\n')
        append("\u4fdd\u5b58\u65f6\u95f4: ")
        append(formatColorCardTimestamp(palette.createdAt))
        append('\n')
        append('\n')
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
    }
}

internal fun buildChineseColorDetailText(
    entry: ColorCardChineseInspirationEntry
): String {
    val metrics = colorCardMetrics(entry.color)
    return buildString {
        append(entry.sourceTag.ifBlank { COLOR_CARD_CHINESE_TRADITIONAL_SOURCE_TAG })
        append(": ")
        append(entry.name)
        append('\n')
        append('\n')
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
    }
}

internal fun buildTopJournalPaletteDetailText(
    entry: ColorCardTopJournalPaletteEntry
): String {
    return buildString {
        append("\u8ddf\u7740\u9876\u520a\u5b66\u914d\u8272 ")
        append(entry.title)
        append('\n')
        append('\n')
        entry.colors.forEachIndexed { index, colorInt ->
            val metrics = colorCardMetrics(colorInt)
            append(index + 1)
            append(". #")
            append(metrics.hex)
            append('\n')
            append("RGB: ")
            append(metrics.rgb)
            append('\n')
            append("HSB: ")
            append(metrics.hsb)
            append('\n')
            append("CMYK: ")
            append(metrics.cmyk)
            if (index < entry.colors.lastIndex) {
                append('\n')
                append('\n')
            }
        }
    }
}

internal fun buildTopJournalSingleColorDetailText(
    entry: ColorCardTopJournalPaletteEntry,
    colorIndex: Int
): String {
    val safeIndex = colorIndex.coerceIn(0, entry.colors.lastIndex)
    val colorInt = entry.colors[safeIndex]
    val metrics = colorCardMetrics(colorInt)
    return buildString {
        append("\u8ddf\u7740\u9876\u520a\u5b66\u914d\u8272 ")
        append(entry.title)
        append('\n')
        append('\n')
        append("\u989c\u8272 ")
        append(safeIndex + 1)
        append(" / ")
        append(entry.colors.size)
        append('\n')
        append('\n')
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
    }
}

internal fun buildNicePaletteDetailText(
    entry: ColorCardNicePaletteEntry
): String {
    return buildString {
        append("NICE ")
        append(entry.title.removePrefix("NICE "))
        append('\n')
        append('\n')
        entry.colors.forEachIndexed { index, colorInt ->
            val metrics = colorCardMetrics(colorInt)
            append(index + 1)
            append(". #")
            append(metrics.hex)
            append('\n')
            append("RGB: ")
            append(metrics.rgb)
            append('\n')
            append("HSB: ")
            append(metrics.hsb)
            append('\n')
            append("CMYK: ")
            append(metrics.cmyk)
            if (index < entry.colors.lastIndex) {
                append('\n')
                append('\n')
            }
        }
    }
}

internal fun buildNiceSingleColorDetailText(
    entry: ColorCardNicePaletteEntry,
    colorIndex: Int
): String {
    val safeIndex = colorIndex.coerceIn(0, entry.colors.lastIndex)
    val colorInt = entry.colors[safeIndex]
    val metrics = colorCardMetrics(colorInt)
    return buildString {
        append(entry.title)
        append('\n')
        append('\n')
        append("\u989c\u8272 ")
        append(safeIndex + 1)
        append(" / ")
        append(entry.colors.size)
        append('\n')
        append('\n')
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
    }
}

internal fun buildNordPaletteDetailText(
    entry: ColorCardNordPaletteEntry
): String {
    return buildString {
        append("Nord ")
        append(entry.title)
        append('\n')
        append(entry.subtitle)
        append('\n')
        append('\n')
        entry.colors.forEachIndexed { index, colorInt ->
            val metrics = colorCardMetrics(colorInt)
            append(index + 1)
            append(". #")
            append(metrics.hex)
            append('\n')
            append("RGB: ")
            append(metrics.rgb)
            append('\n')
            append("HSB: ")
            append(metrics.hsb)
            append('\n')
            append("CMYK: ")
            append(metrics.cmyk)
            if (index < entry.colors.lastIndex) {
                append('\n')
                append('\n')
            }
        }
    }
}

internal fun buildPaletteerPaletteDetailText(
    entry: ColorCardPaletteerPaletteEntry
): String {
    return buildString {
        append("灵感 ")
        append(entry.collectionName)
        append(" / ")
        append(entry.paletteName)
        append('\n')
        append('\n')
        entry.colors.forEachIndexed { index, colorInt ->
            val metrics = colorCardMetrics(colorInt)
            append(index + 1)
            append(". #")
            append(metrics.hex)
            append('\n')
            append("RGB: ")
            append(metrics.rgb)
            append('\n')
            append("HSB: ")
            append(metrics.hsb)
            append('\n')
            append("CMYK: ")
            append(metrics.cmyk)
            if (index < entry.colors.lastIndex) {
                append('\n')
                append('\n')
            }
        }
    }
}

internal fun buildNordSingleColorDetailText(
    entry: ColorCardNordPaletteEntry,
    colorIndex: Int
): String {
    val safeIndex = colorIndex.coerceIn(0, entry.colors.lastIndex)
    val colorInt = entry.colors[safeIndex]
    val metrics = colorCardMetrics(colorInt)
    return buildString {
        append("Nord ")
        append(entry.title)
        append('\n')
        append(entry.subtitle)
        append('\n')
        append('\n')
        append("\u989c\u8272 ")
        append(safeIndex + 1)
        append(" / ")
        append(entry.colors.size)
        append('\n')
        append('\n')
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
    }
}

internal fun buildPaletteerSingleColorDetailText(
    entry: ColorCardPaletteerPaletteEntry,
    colorIndex: Int
): String {
    val safeIndex = colorIndex.coerceIn(0, entry.colors.lastIndex)
    val colorInt = entry.colors[safeIndex]
    val metrics = colorCardMetrics(colorInt)
    return buildString {
        append("灵感 ")
        append(entry.collectionName)
        append(" / ")
        append(entry.paletteName)
        append('\n')
        append('\n')
        append("\u989c\u8272 ")
        append(safeIndex + 1)
        append(" / ")
        append(entry.colors.size)
        append('\n')
        append('\n')
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
    }
}

internal fun copyColorCardText(
    context: Context,
    label: String,
    text: String,
    successMessage: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(
        ClipData.newPlainText(
            label,
            ensurePhotooColorCardCopyPrefix(text)
        )
    )
    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
}

internal fun shareColorCardText(
    context: Context,
    chooserTitle: String,
    subject: String,
    text: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, ensurePhotooColorCardBrand(text))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

internal fun ensurePhotooColorCardBrand(text: String): String {
    val trimmed = text.trim()
    if (
        trimmed.contains("PHOTOO 配方", ignoreCase = true) ||
        trimmed.contains("PHOTOO色卡", ignoreCase = true) ||
        trimmed.contains("PHOTOO_RECIPE_V1:", ignoreCase = true)
    ) {
        return trimmed
    }
    if (trimmed.isBlank()) {
        return "PHOTOO色卡"
    }
    return buildString {
        append("PHOTOO色卡")
        append('\n')
        append('\n')
        append(trimmed)
    }
}
