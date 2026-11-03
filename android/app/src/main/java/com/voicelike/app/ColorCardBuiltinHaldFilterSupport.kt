package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.pow
import kotlin.math.roundToInt

private const val COLOR_CARD_BUILTIN_HALD_FILTERS_ASSET = "color_card_builtin_hald_filters.json"

internal data class ColorCardBuiltinHaldFilterAsset(
    val id: String,
    val title: String,
    val sourceTag: String?,
    val assetName: String,
    val haldLevel: Int,
    val previewColors: List<Int>,
    val sortOrder: Int,
    val canonicalId: String,
    val displayCode: String,
    val familyLabel: String? = null,
    val variantLabel: String? = null,
    val variantValue: Int? = null
)

internal suspend fun loadColorCardBuiltinHaldFilters(
    context: Context
): List<ColorCardBuiltinHaldFilterAsset> = withContext(Dispatchers.IO) {
    val manifest = runCatching {
        context.assets.open(COLOR_CARD_BUILTIN_HALD_FILTERS_ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }.getOrElse {
        return@withContext emptyList()
    }

    val root = runCatching { JSONArray(manifest) }.getOrElse {
        return@withContext emptyList()
    }

    buildList {
        for (index in 0 until root.length()) {
            val item = root.optJSONObject(index) ?: continue
            val id = item.optString("id").trim()
            val title = item.optString("title").trim()
            val assetName = item.optString("assetName").trim()
            if (id.isBlank() || title.isBlank() || assetName.isBlank()) continue

            val haldLevel = item.optInt("haldLevel").takeIf { it > 0 }
                ?: detectColorCardHaldLevel(context, assetName)
                ?: continue
            val resolvedSourceTag = item.optString("sourceTag").trim().ifBlank { null }
            val previewColors = item.optJSONArray("previewColors")
                ?.let(::parseColorCardPreviewColors)
                ?.takeIf { it.isNotEmpty() }
                ?: buildColorCardBuiltinHaldPreviewColors(
                    context = context,
                    assetName = assetName,
                    haldLevel = haldLevel
                )
            val identity = resolveColorCardBuiltinHaldIdentity(
                rawId = id,
                title = title,
                sourceTag = resolvedSourceTag
            )
            val normalizedSourceTag = when {
                identity.canonicalId.startsWith("classic-memory-") -> "经典记忆"
                identity.canonicalId.startsWith("black-white-era-") -> "黑白时代"
                else -> identity.sourceTag
            }

            add(
                ColorCardBuiltinHaldFilterAsset(
                    id = id,
                    title = title,
                    sourceTag = normalizedSourceTag,
                    assetName = assetName,
                    haldLevel = haldLevel,
                    previewColors = previewColors,
                    sortOrder = item.optInt("sortOrder", index),
                    canonicalId = identity.canonicalId,
                    displayCode = identity.displayCode,
                    familyLabel = identity.familyLabel,
                    variantLabel = identity.variantLabel,
                    variantValue = identity.variantValue
                )
            )
        }
    }.sortedBy { it.sortOrder }
}

internal suspend fun decodeColorCardBuiltinHaldBitmap(
    context: Context,
    assetName: String
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        context.assets.open(assetName).use { input ->
            BitmapFactory.decodeStream(input)
        }?.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull()
}

private fun detectColorCardHaldLevel(
    context: Context,
    assetName: String
): Int? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    runCatching {
        context.assets.open(assetName).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }.getOrNull() ?: return null

    val width = options.outWidth
    val height = options.outHeight
    if (width <= 0 || width != height) return null
    val level = width.toDouble().pow(1.0 / 3.0).roundToInt()
    return level.takeIf { it > 0 && (it * it * it) == width }
}

private fun buildColorCardBuiltinHaldPreviewColors(
    context: Context,
    assetName: String,
    haldLevel: Int
): List<Int> {
    val bitmap = decodeColorCardBuiltinHaldBitmapBlocking(context, assetName) ?: return emptyList()
    return listOf(
        sampleColorCardHaldBitmap(bitmap, haldLevel, 0.22f, 0.18f, 0.16f),
        sampleColorCardHaldBitmap(bitmap, haldLevel, 0.48f, 0.42f, 0.38f),
        sampleColorCardHaldBitmap(bitmap, haldLevel, 0.80f, 0.66f, 0.56f),
        sampleColorCardHaldBitmap(bitmap, haldLevel, 0.30f, 0.52f, 0.72f),
        sampleColorCardHaldBitmap(bitmap, haldLevel, 0.90f, 0.92f, 0.94f)
    )
}

private fun decodeColorCardBuiltinHaldBitmapBlocking(
    context: Context,
    assetName: String
): Bitmap? {
    return runCatching {
        context.assets.open(assetName).use { input ->
            BitmapFactory.decodeStream(input)
        }?.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull()
}

private fun parseColorCardPreviewColors(
    array: JSONArray
): List<Int> {
    val colors = ArrayList<Int>(array.length())
    for (index in 0 until array.length()) {
        when (val raw = array.opt(index)) {
            is Number -> colors += raw.toInt()
            is String -> {
                parseColorCardPreviewColorToken(raw)?.let(colors::add)
            }
        }
    }
    return colors
}

private fun parseColorCardPreviewColorToken(
    token: String
): Int? {
    val normalized = token.trim()
    if (normalized.isBlank()) return null
    val safeToken = if (normalized.startsWith("#")) normalized else "#$normalized"
    return runCatching { AndroidColor.parseColor(safeToken) }.getOrNull()
}

internal fun sampleColorCardHaldBitmap(
    bitmap: Bitmap,
    haldLevel: Int,
    red: Float,
    green: Float,
    blue: Float
): Int {
    val cubeSide = haldLevel * haldLevel
    val imageSide = haldLevel * cubeSide
    if (
        cubeSide <= 1 ||
        bitmap.width != imageSide ||
        bitmap.height != imageSide
    ) {
        return AndroidColor.rgb(
            (red.coerceIn(0f, 1f) * 255f).roundToInt(),
            (green.coerceIn(0f, 1f) * 255f).roundToInt(),
            (blue.coerceIn(0f, 1f) * 255f).roundToInt()
        )
    }

    val r = red.coerceIn(0f, 1f) * (cubeSide - 1)
    val g = green.coerceIn(0f, 1f) * (cubeSide - 1)
    val b = blue.coerceIn(0f, 1f) * (cubeSide - 1)

    val r0 = r.toInt().coerceIn(0, cubeSide - 1)
    val g0 = g.toInt().coerceIn(0, cubeSide - 1)
    val b0 = b.toInt().coerceIn(0, cubeSide - 1)
    val r1 = (r0 + 1).coerceAtMost(cubeSide - 1)
    val g1 = (g0 + 1).coerceAtMost(cubeSide - 1)
    val b1 = (b0 + 1).coerceAtMost(cubeSide - 1)

    val rf = (r - r0).coerceIn(0f, 1f)
    val gf = (g - g0).coerceIn(0f, 1f)
    val bf = (b - b0).coerceIn(0f, 1f)

    fun sampleDiscrete(rIndex: Int, gIndex: Int, bIndex: Int): Int {
        val linearIndex = rIndex + (gIndex * cubeSide) + (bIndex * cubeSide * cubeSide)
        val x = linearIndex % imageSide
        val y = linearIndex / imageSide
        return bitmap.getPixel(x, y)
    }

    val c000 = sampleDiscrete(r0, g0, b0)
    val c100 = sampleDiscrete(r1, g0, b0)
    val c010 = sampleDiscrete(r0, g1, b0)
    val c110 = sampleDiscrete(r1, g1, b0)
    val c001 = sampleDiscrete(r0, g0, b1)
    val c101 = sampleDiscrete(r1, g0, b1)
    val c011 = sampleDiscrete(r0, g1, b1)
    val c111 = sampleDiscrete(r1, g1, b1)

    fun channel(color: Int, getter: (Int) -> Int): Float = getter(color) / 255f
    fun lerp(start: Float, end: Float, amount: Float): Float = start + ((end - start) * amount)

    fun trilinear(getter: (Int) -> Int): Int {
        val c00 = lerp(channel(c000, getter), channel(c100, getter), rf)
        val c10 = lerp(channel(c010, getter), channel(c110, getter), rf)
        val c01 = lerp(channel(c001, getter), channel(c101, getter), rf)
        val c11 = lerp(channel(c011, getter), channel(c111, getter), rf)
        val c0 = lerp(c00, c10, gf)
        val c1 = lerp(c01, c11, gf)
        return (lerp(c0, c1, bf) * 255f).roundToInt().coerceIn(0, 255)
    }

    return AndroidColor.rgb(
        trilinear(AndroidColor::red),
        trilinear(AndroidColor::green),
        trilinear(AndroidColor::blue)
    )
}
