package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.max
import kotlin.math.roundToInt

private const val COLOR_CARD_TRACE_FILM_GRAIN_ASSET = "color_card_film_grain_textures.json"
internal const val COLOR_CARD_TRACE_FILM_GRAIN_BLEND_ALPHA = 0.24f

internal data class ColorCardTraceFilmGrainAsset(
    val id: String,
    val title: String,
    val assetName: String,
    val sortOrder: Int
)

internal suspend fun loadColorCardTraceFilmGrainAssets(
    context: Context
): List<ColorCardTraceFilmGrainAsset> = withContext(Dispatchers.IO) {
    val manifest = runCatching {
        context.assets.open(COLOR_CARD_TRACE_FILM_GRAIN_ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
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
            add(
                ColorCardTraceFilmGrainAsset(
                    id = id,
                    title = title,
                    assetName = assetName,
                    sortOrder = item.optInt("sortOrder", index)
                )
            )
        }
    }.sortedBy(ColorCardTraceFilmGrainAsset::sortOrder)
}

internal suspend fun decodeColorCardTraceFilmGrainBitmap(
    context: Context,
    assetName: String
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        context.assets.open(assetName).use { input ->
            BitmapFactory.decodeStream(input)
        }?.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull()
}

internal fun isColorCardTraceFilmGrainEligible(
    candidate: ColorCardTraceFilterCandidate?
): Boolean {
    if (candidate == null) return false
    if (candidate.kind == ColorCardTraceFilterKind.AiMnn) return true
    if (candidate.kind != ColorCardTraceFilterKind.HaldClut) return false
    return when (candidate.title.trim().lastOrNull()?.uppercaseChar()) {
        'C', 'F', 'M', 'B' -> true
        else -> false
    }
}

internal fun applyColorCardTraceFilmGrainOverlay(
    sourceBitmap: Bitmap,
    grainBitmap: Bitmap,
    alpha: Float = COLOR_CARD_TRACE_FILM_GRAIN_BLEND_ALPHA
): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    if (width <= 0 || height <= 0) {
        return sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    val safeAlpha = alpha.coerceIn(0f, 1f)
    if (safeAlpha <= 0.0001f) {
        return sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    if (grainBitmap.isRecycled) {
        return sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val scaledGrain = if (grainBitmap.width == width && grainBitmap.height == height) {
        grainBitmap
    } else {
        Bitmap.createScaledBitmap(grainBitmap, width, height, true)
    }
    if (scaledGrain.isRecycled) {
        return sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
    val grainPixels = IntArray(width * height)
    scaledGrain.getPixels(grainPixels, 0, width, 0, 0, width, height)

    for (index in sourcePixels.indices) {
        val colorInt = sourcePixels[index]
        val alphaChannel = AndroidColor.alpha(colorInt)
        if (alphaChannel == 0) {
            outputPixels[index] = colorInt
            continue
        }

        val sourceRed = AndroidColor.red(colorInt)
        val sourceGreen = AndroidColor.green(colorInt)
        val sourceBlue = AndroidColor.blue(colorInt)
        val baseLuma = (
            (sourceRed / 255.0) * 0.299 +
                (sourceGreen / 255.0) * 0.587 +
                (sourceBlue / 255.0) * 0.114
            ).coerceIn(0.0, 1.0)
        val lumaZone = filmGrainSmoothStep(0.10, 0.32, baseLuma) *
            (1.0 - filmGrainSmoothStep(0.88, 0.98, baseLuma))
        val pixelAlpha = (safeAlpha * (0.70f + (0.30f * lumaZone.toFloat()))).coerceIn(0f, 1f)

        val grainColor = grainPixels[index]
        val grainLuma = (
            (AndroidColor.red(grainColor) / 255.0) * 0.299 +
                (AndroidColor.green(grainColor) / 255.0) * 0.587 +
                (AndroidColor.blue(grainColor) / 255.0) * 0.114
            ).coerceIn(0.0, 1.0)
        val outputRed = filmGrainMixOverlay(sourceRed, grainLuma, pixelAlpha)
        val outputGreen = filmGrainMixOverlay(sourceGreen, grainLuma, pixelAlpha)
        val outputBlue = filmGrainMixOverlay(sourceBlue, grainLuma, pixelAlpha)
        outputPixels[index] = AndroidColor.argb(
            alphaChannel,
            outputRed,
            outputGreen,
            outputBlue
        )
    }

    if (scaledGrain !== grainBitmap && !scaledGrain.isRecycled) {
        scaledGrain.recycle()
    }

    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun filmGrainMixOverlay(
    baseChannel: Int,
    grainLuma: Double,
    alpha: Float
): Int {
    val base = (baseChannel / 255.0).coerceIn(0.0, 1.0)
    val overlay = if (base <= 0.5) {
        2.0 * base * grainLuma
    } else {
        1.0 - (2.0 * (1.0 - base) * (1.0 - grainLuma))
    }.coerceIn(0.0, 1.0)
    val mixed = base + ((overlay - base) * alpha.coerceIn(0f, 1f))
    return (mixed.coerceIn(0.0, 1.0) * 255.0).roundToInt().coerceIn(0, 255)
}

private fun filmGrainSmoothStep(
    edge0: Double,
    edge1: Double,
    value: Double
): Double {
    if (edge1 <= edge0) {
        return if (value >= edge1) 1.0 else 0.0
    }
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
    return t * t * (3.0 - (2.0 * t))
}

internal suspend fun renderColorCardTraceFilmGrainPreviewBitmap(
    context: Context,
    asset: ColorCardTraceFilmGrainAsset,
    sampleBitmap: Bitmap
): Bitmap? {
    val grainBitmap = decodeColorCardTraceFilmGrainBitmap(
        context = context,
        assetName = asset.assetName
    ) ?: return null
    return try {
        applyColorCardTraceFilmGrainOverlay(
            sourceBitmap = sampleBitmap,
            grainBitmap = grainBitmap
        )
    } finally {
        if (!grainBitmap.isRecycled) {
            grainBitmap.recycle()
        }
    }
}
