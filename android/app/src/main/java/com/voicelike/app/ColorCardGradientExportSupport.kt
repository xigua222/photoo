package com.voicelike.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.round

internal fun buildColorCardGradientDetailText(
    title: String,
    sourceTag: String?,
    gradientSpec: ColorCardGradientSpec
): String {
    val normalizedSpec = normalizeColorCardGradientSpec(gradientSpec)
    return buildString {
        append("PHOTOO\u6e10\u53d8\u914d\u65b9")
        append('\n')
        append(title)
        sourceTag?.takeIf { it.isNotBlank() }?.let { tag ->
            append('\n')
            append(tag)
        }
        append('\n')
        append('\n')
        append("CSS")
        append('\n')
        append(buildColorCardGradientCss(normalizedSpec))
        append('\n')
        append('\n')
        append("PHOTOO JSON")
        append('\n')
        append(buildColorCardGradientRecipeJson(normalizedSpec))
    }
}

internal fun buildColorCardGradientCss(
    gradientSpec: ColorCardGradientSpec
): String {
    val normalizedSpec = normalizeColorCardGradientSpec(gradientSpec)
    val stopsText = normalizedSpec.stops.joinToString(", ") { stop ->
        "#${colorCardMetrics(stop.colorInt).hex} ${formatColorCardGradientPercent(stop.positionPercent)}%"
    }
    return "linear-gradient(${formatColorCardGradientAngle(normalizedSpec.angleDegrees)}deg, $stopsText)"
}

internal fun buildColorCardGradientRecipeJson(
    gradientSpec: ColorCardGradientSpec
): String {
    val normalizedSpec = normalizeColorCardGradientSpec(gradientSpec)
    val root = JSONObject()
        .put("type", "linear-gradient")
        .put("angle", formatColorCardGradientDecimal(normalizedSpec.angleDegrees))

    val stopsArray = JSONArray()
    normalizedSpec.stops.forEach { stop ->
        stopsArray.put(
            JSONObject()
                .put("hex", "#${colorCardMetrics(stop.colorInt).hex}")
                .put("position", formatColorCardGradientDecimal(stop.positionPercent))
        )
    }
    root.put("stops", stopsArray)
    return root.toString(2)
}

internal fun buildGradientInspirationPaletteDetailText(
    entry: ColorCardGradientInspirationEntry
): String {
    return buildString {
        append("\u6e10\u53d8\u7075\u611f ")
        append(entry.sourceName)
        append(" / ")
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
        append('\n')
        append('\n')
        append("CSS: ")
        append(buildColorCardGradientCss(entry.gradientSpec))
        append('\n')
        append('\n')
        append("PHOTOO JSON:")
        append('\n')
        append(buildColorCardGradientRecipeJson(entry.gradientSpec))
    }
}

internal fun buildGradientInspirationSingleColorDetailText(
    entry: ColorCardGradientInspirationEntry,
    colorIndex: Int
): String {
    val safeIndex = colorIndex.coerceIn(0, entry.colors.lastIndex)
    val colorInt = entry.colors[safeIndex]
    val metrics = colorCardMetrics(colorInt)
    return buildString {
        append("\u6e10\u53d8\u7075\u611f ")
        append(entry.sourceName)
        append(" / ")
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

internal fun formatColorCardGradientAngle(angleDegrees: Float): String {
    return formatColorCardGradientDecimal(
        normalizeColorCardGradientSpec(
            ColorCardGradientSpec(
                angleDegrees = angleDegrees,
                stops = listOf(
                    ColorCardGradientStop(0, 0f),
                    ColorCardGradientStop(0, 100f)
                )
            )
        ).angleDegrees
    )
}

internal fun formatColorCardGradientPercent(positionPercent: Float): String {
    return formatColorCardGradientDecimal(positionPercent.coerceIn(0f, 100f))
}

private fun formatColorCardGradientDecimal(value: Float): String {
    val rounded = round(value * 10f) / 10f
    return if (rounded == rounded.toInt().toFloat()) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}
