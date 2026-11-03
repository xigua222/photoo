package com.voicelike.app

import android.content.Context
import android.graphics.Color as AndroidColor
import org.json.JSONArray
import org.json.JSONObject

private const val COLOR_CARD_WEB_GRADIENTS_ASSET = "color_card_gradients_parsed.json"
private const val COLOR_CARD_COOLHUE_ASSET = "color_card_coolhue.json"
private const val COLOR_CARD_DEFAULT_GRADIENT_ANGLE = 135f
private const val COLOR_CARD_MIN_GRADIENT_STOP_DISTANCE = 0.5f

internal data class ColorCardGradientStop(
    val colorInt: Int,
    val positionPercent: Float
)

internal data class ColorCardGradientSpec(
    val angleDegrees: Float,
    val stops: List<ColorCardGradientStop>
)

internal data class ColorCardGradientInspirationEntry(
    val key: String,
    val index: Int,
    val title: String,
    val sourceName: String,
    val gradientSpec: ColorCardGradientSpec
) {
    val colors: List<Int>
        get() = colorCardGradientPaletteColors(gradientSpec)
}

private var cachedColorCardGradientEntries: List<ColorCardGradientInspirationEntry>? = null

internal fun loadColorCardGradientInspiration(context: Context): List<ColorCardGradientInspirationEntry> {
    cachedColorCardGradientEntries?.let { return it }
    synchronized(COLOR_CARD_WEB_GRADIENTS_ASSET) {
        cachedColorCardGradientEntries?.let { return it }
        val loaded = runCatching {
            val webGradients = context.assets.open(COLOR_CARD_WEB_GRADIENTS_ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> parseColorCardWebGradientEntries(JSONArray(reader.readText())) }
            val coolhueGradients = context.assets.open(COLOR_CARD_COOLHUE_ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> parseColorCardCoolhueEntries(JSONArray(reader.readText())) }
            webGradients + coolhueGradients
        }.getOrElse { emptyList() }
        cachedColorCardGradientEntries = loaded
        return loaded
    }
}

internal fun buildColorCardGradientSpecFromColors(
    colors: List<Int>,
    angleDegrees: Float = COLOR_CARD_DEFAULT_GRADIENT_ANGLE
): ColorCardGradientSpec? {
    val paletteColors = colors
        .take(10)
        .fold(ArrayList<Int>()) { acc, colorInt ->
            if (acc.lastOrNull() != colorInt) {
                acc += colorInt
            }
            acc
        }
    if (paletteColors.size < 2) return null

    val lastIndex = paletteColors.lastIndex.coerceAtLeast(1)
    val stops = paletteColors.mapIndexed { index, colorInt ->
        val position = if (index == lastIndex) {
            100f
        } else {
            (index.toFloat() / lastIndex.toFloat()) * 100f
        }
        ColorCardGradientStop(
            colorInt = colorInt,
            positionPercent = position
        )
    }
    return normalizeColorCardGradientSpec(
        ColorCardGradientSpec(
            angleDegrees = angleDegrees,
            stops = stops
        )
    )
}

internal fun normalizeColorCardGradientSpec(spec: ColorCardGradientSpec): ColorCardGradientSpec {
    if (spec.stops.size < 2) return spec.copy(angleDegrees = normalizeColorCardGradientAngle(spec.angleDegrees))

    val sortedStops = spec.stops
        .sortedBy { it.positionPercent }
        .mapIndexed { index, stop ->
            val minPosition = if (index == 0) {
                0f
            } else {
                (index * COLOR_CARD_MIN_GRADIENT_STOP_DISTANCE)
            }
            val remainingStops = spec.stops.lastIndex - index
            val maxPosition = 100f - (remainingStops * COLOR_CARD_MIN_GRADIENT_STOP_DISTANCE)
            stop.copy(positionPercent = stop.positionPercent.coerceIn(minPosition, maxPosition))
        }

    val normalizedStops = ArrayList<ColorCardGradientStop>(sortedStops.size)
    sortedStops.forEachIndexed { index, stop ->
        val minPosition = if (index == 0) {
            0f
        } else {
            normalizedStops.last().positionPercent + COLOR_CARD_MIN_GRADIENT_STOP_DISTANCE
        }
        val remainingStops = sortedStops.lastIndex - index
        val maxPosition = 100f - (remainingStops * COLOR_CARD_MIN_GRADIENT_STOP_DISTANCE)
        normalizedStops += stop.copy(
            positionPercent = stop.positionPercent.coerceIn(minPosition, maxPosition)
        )
    }

    return ColorCardGradientSpec(
        angleDegrees = normalizeColorCardGradientAngle(spec.angleDegrees),
        stops = normalizedStops
    )
}

internal fun updateColorCardGradientAngle(
    spec: ColorCardGradientSpec,
    angleDegrees: Float
): ColorCardGradientSpec {
    return spec.copy(angleDegrees = normalizeColorCardGradientAngle(angleDegrees))
}

internal fun updateColorCardGradientStopPosition(
    spec: ColorCardGradientSpec,
    stopIndex: Int,
    positionPercent: Float
): ColorCardGradientSpec {
    if (stopIndex !in spec.stops.indices) return spec
    val currentStops = spec.stops
    val minPosition = if (stopIndex == 0) {
        0f
    } else {
        currentStops[stopIndex - 1].positionPercent + COLOR_CARD_MIN_GRADIENT_STOP_DISTANCE
    }
    val maxPosition = if (stopIndex == currentStops.lastIndex) {
        100f
    } else {
        currentStops[stopIndex + 1].positionPercent - COLOR_CARD_MIN_GRADIENT_STOP_DISTANCE
    }
    val clampedPosition = positionPercent.coerceIn(minPosition, maxPosition)
    return spec.copy(
        stops = currentStops.mapIndexed { index, stop ->
            if (index == stopIndex) {
                stop.copy(positionPercent = clampedPosition)
            } else {
                stop
            }
        }
    )
}

internal fun colorCardGradientPaletteColors(
    spec: ColorCardGradientSpec
): List<Int> {
    return spec.stops.fold(ArrayList<Int>()) { acc, stop ->
        if (acc.lastOrNull() != stop.colorInt) {
            acc += stop.colorInt
        }
        acc
    }
}

private fun parseColorCardWebGradientEntries(root: JSONArray): List<ColorCardGradientInspirationEntry> {
    val result = ArrayList<ColorCardGradientInspirationEntry>(root.length())
    for (index in 0 until root.length()) {
        val entryObject = root.optJSONObject(index) ?: continue
        val title = entryObject.optString("name").trim().ifBlank {
            "WebGradients ${(index + 1).toString().padStart(3, '0')}"
        }
        val gradientArray = entryObject.optJSONArray("gradient") ?: continue
        val stops = ArrayList<ColorCardGradientStop>(gradientArray.length())
        for (stopIndex in 0 until gradientArray.length()) {
            val stopObject = gradientArray.optJSONObject(stopIndex) ?: continue
            val colorInt = parseColorCardGradientHexColor(stopObject.optString("color")) ?: continue
            val position = stopObject.optDouble("pos", Double.NaN)
            if (position.isNaN()) continue
            stops += ColorCardGradientStop(
                colorInt = colorInt,
                positionPercent = position.toFloat()
            )
        }
        if (stops.size < 2) continue

        val entryIndex = entryObject.optString("index").trim().toIntOrNull() ?: (index + 1)
        result += ColorCardGradientInspirationEntry(
            key = "gradient-web-$entryIndex-$title",
            index = entryIndex,
            title = title,
            sourceName = "WebGradients",
            gradientSpec = normalizeColorCardGradientSpec(
                ColorCardGradientSpec(
                    angleDegrees = entryObject.optDouble("deg", COLOR_CARD_DEFAULT_GRADIENT_ANGLE.toDouble()).toFloat(),
                    stops = stops
                )
            )
        )
    }
    return result
}

private fun parseColorCardCoolhueEntries(root: JSONArray): List<ColorCardGradientInspirationEntry> {
    val result = ArrayList<ColorCardGradientInspirationEntry>(root.length())
    for (index in 0 until root.length()) {
        val colorArray = root.optJSONArray(index) ?: continue
        val colors = ArrayList<Int>(colorArray.length())
        for (colorIndex in 0 until colorArray.length()) {
            parseColorCardGradientHexColor(colorArray.optString(colorIndex))?.let(colors::add)
        }
        val gradientSpec = buildColorCardGradientSpecFromColors(colors) ?: continue
        val entryIndex = index + 1
        result += ColorCardGradientInspirationEntry(
            key = "gradient-coolhue-$entryIndex",
            index = entryIndex,
            title = "coolhue ${entryIndex.toString().padStart(3, '0')}",
            sourceName = "coolhue",
            gradientSpec = gradientSpec
        )
    }
    return result
}

private fun parseColorCardGradientHexColor(token: String): Int? {
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

private fun normalizeColorCardGradientAngle(angleDegrees: Float): Float {
    val normalized = angleDegrees % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}
