package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val COLOR_CARD_IMITATE_OPTIMIZATION_MAX_SIDE = 96
private const val COLOR_CARD_IMITATE_OPTIMIZATION_PASSES = 2

internal data class ColorCardImitateLabMoments(
    val meanL: Double,
    val meanA: Double,
    val meanB: Double,
    val stdL: Double,
    val stdA: Double,
    val stdB: Double
)

internal data class ColorCardImitateTransferBand(
    val source: ColorCardImitateLabMoments,
    val target: ColorCardImitateLabMoments
)

internal data class ColorCardImitateToneCurve(
    val sourceStops: List<Double>,
    val targetStops: List<Double>
)

internal data class ColorCardImitateTransferProfile(
    val global: ColorCardImitateTransferBand,
    val shadows: ColorCardImitateTransferBand,
    val midtones: ColorCardImitateTransferBand,
    val highlights: ColorCardImitateTransferBand,
    val toneCurve: ColorCardImitateToneCurve
)

internal data class ColorCardImitateAnalysis(
    val paletteColors: List<Int>,
    val averageLuminance: Float,
    val contrast: Float,
    val averageSaturation: Float,
    val warmth: Float,
    val shadowColor: Int,
    val highlightColor: Int,
    val transferProfile: ColorCardImitateTransferProfile,
    val suggestedColor: Float,
    val suggestedTone: Float,
    val suggestedSaturation: Float,
    val suggestedBrightness: Float,
    val suggestedStrength: Float,
    val suggestedSharpen: Float,
    val summary: String,
    val signature: String
)

private data class ColorCardImitateImageStats(
    val paletteColors: List<Int>,
    val averageLuminance: Float,
    val contrast: Float,
    val averageSaturation: Float,
    val warmth: Float,
    val shadowColor: Int,
    val highlightColor: Int,
    val globalMoments: ColorCardImitateLabMoments,
    val shadowMoments: ColorCardImitateLabMoments,
    val midtoneMoments: ColorCardImitateLabMoments,
    val highlightMoments: ColorCardImitateLabMoments,
    val toneStops: List<Double>
)

private data class ColorCardImitatePaletteBucket(
    var weight: Double = 0.0,
    var sumRed: Double = 0.0,
    var sumGreen: Double = 0.0,
    var sumBlue: Double = 0.0
) {
    fun add(
        colorInt: Int,
        bucketWeight: Double
    ) {
        val safeWeight = bucketWeight.coerceAtLeast(0.0)
        if (safeWeight <= 0.0) return
        weight += safeWeight
        sumRed += AndroidColor.red(colorInt) * safeWeight
        sumGreen += AndroidColor.green(colorInt) * safeWeight
        sumBlue += AndroidColor.blue(colorInt) * safeWeight
    }

    fun averageColor(): Int {
        val safeWeight = weight.coerceAtLeast(0.0001)
        return AndroidColor.rgb(
            (sumRed / safeWeight).roundToInt().coerceIn(0, 255),
            (sumGreen / safeWeight).roundToInt().coerceIn(0, 255),
            (sumBlue / safeWeight).roundToInt().coerceIn(0, 255)
        )
    }
}

private data class ColorCardImitateOptimizationState(
    val color: Float,
    val tone: Float,
    val saturation: Float,
    val brightness: Float,
    val strength: Float,
    val sharpen: Float,
    val score: Double = Double.POSITIVE_INFINITY
)

private class ColorCardImitateMomentsBuilder {
    private var count = 0
    private var totalWeight = 0.0
    private var sumL = 0.0
    private var sumA = 0.0
    private var sumB = 0.0
    private var sumSqL = 0.0
    private var sumSqA = 0.0
    private var sumSqB = 0.0

    fun add(
        lab: ColorCardTraceLab,
        weight: Double = 1.0
    ) {
        val safeWeight = weight.coerceAtLeast(0.0001)
        count += 1
        totalWeight += safeWeight
        sumL += lab.l * safeWeight
        sumA += lab.a * safeWeight
        sumB += lab.b * safeWeight
        sumSqL += lab.l * lab.l * safeWeight
        sumSqA += lab.a * lab.a * safeWeight
        sumSqB += lab.b * lab.b * safeWeight
    }

    fun build(fallback: ColorCardImitateLabMoments? = null): ColorCardImitateLabMoments {
        if (count <= 0 || totalWeight <= 0.0001) {
            return fallback ?: ColorCardImitateLabMoments(
                meanL = 50.0,
                meanA = 0.0,
                meanB = 0.0,
                stdL = 12.0,
                stdA = 8.0,
                stdB = 8.0
            )
        }
        val safeCount = totalWeight
        val meanL = sumL / safeCount
        val meanA = sumA / safeCount
        val meanB = sumB / safeCount
        val varianceL = ((sumSqL / safeCount) - (meanL * meanL)).coerceAtLeast(0.0)
        val varianceA = ((sumSqA / safeCount) - (meanA * meanA)).coerceAtLeast(0.0)
        val varianceB = ((sumSqB / safeCount) - (meanB * meanB)).coerceAtLeast(0.0)
        return ColorCardImitateLabMoments(
            meanL = meanL,
            meanA = meanA,
            meanB = meanB,
            stdL = sqrt(varianceL).coerceAtLeast(1.6),
            stdA = sqrt(varianceA).coerceAtLeast(1.1),
            stdB = sqrt(varianceB).coerceAtLeast(1.1)
        )
    }
}

internal fun analyzeColorCardImitateStyle(
    sourceFile: File,
    referenceFile: File
): ColorCardImitateAnalysis? {
    val sourceBitmap = decodeColorCardImitateBitmap(sourceFile) ?: return null
    val referenceBitmap = decodeColorCardImitateBitmap(referenceFile) ?: return null
    return try {
        val sourceStats = computeColorCardImitateStats(sourceBitmap)
        val referenceStats = computeColorCardImitateStats(referenceBitmap)
        val baseAnalysis = buildColorCardImitateAnalysis(sourceStats, referenceStats)
        optimizeColorCardImitateAnalysis(
            sourceBitmap = sourceBitmap,
            sourceStats = sourceStats,
            referenceStats = referenceStats,
            baseAnalysis = baseAnalysis
        )
    } finally {
        if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
        if (!referenceBitmap.isRecycled) referenceBitmap.recycle()
    }
}

private fun decodeColorCardImitateBitmap(file: File): Bitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val longest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    var sampleSize = 1
    while ((longest / sampleSize) > 384) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    )?.copy(Bitmap.Config.ARGB_8888, false)
}

private fun computeColorCardImitateStats(bitmap: Bitmap): ColorCardImitateImageStats {
    val width = bitmap.width.coerceAtLeast(1)
    val height = bitmap.height.coerceAtLeast(1)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val luminanceHistogram = DoubleArray(256)
    val shadowBuckets = DoubleArray(12)
    val highlightBuckets = DoubleArray(12)
    val paletteBuckets = linkedMapOf<Int, ColorCardImitatePaletteBucket>()
    val globalMoments = ColorCardImitateMomentsBuilder()
    val shadowMoments = ColorCardImitateMomentsBuilder()
    val midtoneMoments = ColorCardImitateMomentsBuilder()
    val highlightMoments = ColorCardImitateMomentsBuilder()
    var luminanceSum = 0.0
    var saturationSum = 0.0
    var warmthSum = 0.0
    var tonalWeightSum = 0.0

    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val colorInt = pixels[rowOffset + x]
            if (AndroidColor.alpha(colorInt) <= 8) continue

            val red = AndroidColor.red(colorInt)
            val green = AndroidColor.green(colorInt)
            val blue = AndroidColor.blue(colorInt)
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(colorInt, hsv)
            val lab = colorCardTraceLabFromRgb(red = red, green = green, blue = blue)
            val luminance = (lab.l / 100.0).toFloat().coerceIn(0f, 1f)
            val luminanceIndex = (luminance * 255f).roundToInt().coerceIn(0, 255)
            val centerWeight = resolveColorCardImitateCenterWeight(
                x = x,
                y = y,
                width = width,
                height = height
            )
            val tonalWeight = resolveColorCardImitateTonalWeight(
                luminance = luminance,
                saturation = hsv[1],
                centerWeight = centerWeight
            )
            val paletteWeight = resolveColorCardImitatePaletteWeight(
                luminance = luminance,
                saturation = hsv[1],
                lab = lab,
                centerWeight = centerWeight
            )
            val warmth = (((red - blue).toFloat() / 255f) * 0.65f) +
                (((green - blue).toFloat() / 255f) * 0.18f)

            luminanceHistogram[luminanceIndex] += tonalWeight
            luminanceSum += luminance * tonalWeight
            saturationSum += hsv[1] * tonalWeight
            warmthSum += warmth * tonalWeight
            tonalWeightSum += tonalWeight
            globalMoments.add(lab, tonalWeight)

            if (paletteWeight > 0.02) {
                val quantized = quantizeColorCardImitateColor(colorInt)
                paletteBuckets.getOrPut(quantized) { ColorCardImitatePaletteBucket() }
                    .add(colorInt, paletteWeight)
            }

            val hueBucket = (((hsv[0] / 360f) * shadowBuckets.size).toInt()).coerceIn(0, shadowBuckets.lastIndex)
            when {
                luminance <= 0.32f -> {
                    shadowBuckets[hueBucket] += paletteWeight.coerceAtLeast(0.06)
                    shadowMoments.add(lab, tonalWeight * (0.72 + (hsv[1] * 0.52)))
                }
                luminance >= 0.70f -> {
                    highlightBuckets[hueBucket] += paletteWeight.coerceAtLeast(0.05)
                    highlightMoments.add(lab, tonalWeight * (0.70 + (hsv[1] * 0.48)))
                }
                else -> {
                    midtoneMoments.add(lab, tonalWeight * (0.82 + (hsv[1] * 0.36)))
                }
            }
        }
    }

    val safeWeightSum = tonalWeightSum.coerceAtLeast(0.0001)
    val p10 = percentileFromHistogram(luminanceHistogram, safeWeightSum, 0.10f) / 255f
    val p90 = percentileFromHistogram(luminanceHistogram, safeWeightSum, 0.90f) / 255f
    val averageLuminance = (luminanceSum / safeWeightSum).toFloat().coerceIn(0f, 1f)
    val contrast = (p90 - p10).coerceIn(0f, 1f)
    val averageSaturation = (saturationSum / safeWeightSum).toFloat().coerceIn(0f, 1f)
    val warmth = (warmthSum / safeWeightSum).toFloat().coerceIn(-1f, 1f)
    val paletteColors = extractColorCardImitatePalette(paletteBuckets)
    val globalBuilt = globalMoments.build()

    return ColorCardImitateImageStats(
        paletteColors = paletteColors,
        averageLuminance = averageLuminance,
        contrast = contrast,
        averageSaturation = averageSaturation,
        warmth = warmth,
        shadowColor = resolveDominantBucketColor(
            buckets = shadowBuckets,
            luminance = 0.22f,
            saturation = max(0.24f, averageSaturation * 0.85f)
        ),
        highlightColor = resolveDominantBucketColor(
            buckets = highlightBuckets,
            luminance = 0.88f,
            saturation = max(0.14f, averageSaturation * 0.55f)
        ),
        globalMoments = globalBuilt,
        shadowMoments = shadowMoments.build(globalBuilt),
        midtoneMoments = midtoneMoments.build(globalBuilt),
        highlightMoments = highlightMoments.build(globalBuilt),
        toneStops = listOf(
            0.0,
            percentileFromHistogram(luminanceHistogram, safeWeightSum, 0.05f) / 255.0,
            percentileFromHistogram(luminanceHistogram, safeWeightSum, 0.25f) / 255.0,
            percentileFromHistogram(luminanceHistogram, safeWeightSum, 0.50f) / 255.0,
            percentileFromHistogram(luminanceHistogram, safeWeightSum, 0.75f) / 255.0,
            percentileFromHistogram(luminanceHistogram, safeWeightSum, 0.95f) / 255.0,
            1.0
        )
    )
}

private fun buildColorCardImitateAnalysis(
    sourceStats: ColorCardImitateImageStats,
    referenceStats: ColorCardImitateImageStats
): ColorCardImitateAnalysis {
    val brightnessDelta = (referenceStats.averageLuminance - sourceStats.averageLuminance).coerceIn(-0.24f, 0.24f)
    val contrastDelta = (referenceStats.contrast - sourceStats.contrast).coerceIn(-0.34f, 0.34f)
    val saturationDelta = (referenceStats.averageSaturation - sourceStats.averageSaturation).coerceIn(-0.42f, 0.42f)
    val warmthDelta = (referenceStats.warmth - sourceStats.warmth).coerceIn(-1f, 1f)
    val transferProfile = ColorCardImitateTransferProfile(
        global = ColorCardImitateTransferBand(sourceStats.globalMoments, referenceStats.globalMoments),
        shadows = ColorCardImitateTransferBand(sourceStats.shadowMoments, referenceStats.shadowMoments),
        midtones = ColorCardImitateTransferBand(sourceStats.midtoneMoments, referenceStats.midtoneMoments),
        highlights = ColorCardImitateTransferBand(sourceStats.highlightMoments, referenceStats.highlightMoments),
        toneCurve = ColorCardImitateToneCurve(
            sourceStops = sourceStats.toneStops,
            targetStops = referenceStats.toneStops
        )
    )
    val strength = (
        0.62f +
            (abs(warmthDelta) * 0.12f) +
            (abs(saturationDelta) * 0.20f) +
            (abs(contrastDelta) * 0.22f) +
            (abs(brightnessDelta) * 0.14f)
        ).coerceIn(0.58f, 0.94f)
    val sharpen = (
        ((referenceStats.contrast - sourceStats.contrast) * 0.16f) +
            if (referenceStats.contrast > sourceStats.contrast) 0.018f else 0f
        ).coerceIn(-0.08f, 0.14f)
    val suggestedColor = (
        (warmthDelta * 0.16f) +
            (saturationDelta * 0.05f)
        ).coerceIn(-0.12f, 0.12f)

    return ColorCardImitateAnalysis(
        paletteColors = referenceStats.paletteColors,
        averageLuminance = referenceStats.averageLuminance,
        contrast = referenceStats.contrast,
        averageSaturation = referenceStats.averageSaturation,
        warmth = referenceStats.warmth,
        shadowColor = referenceStats.shadowColor,
        highlightColor = referenceStats.highlightColor,
        transferProfile = transferProfile,
        suggestedColor = suggestedColor,
        suggestedTone = (contrastDelta * 0.30f).coerceIn(-0.20f, 0.20f),
        suggestedSaturation = (saturationDelta * 0.36f).coerceIn(-0.24f, 0.24f),
        suggestedBrightness = (brightnessDelta * 0.28f).coerceIn(-0.20f, 0.20f),
        suggestedStrength = strength,
        suggestedSharpen = sharpen,
        summary = describeColorCardImitateMood(referenceStats),
        signature = buildString {
            append(referenceStats.paletteColors.joinToString(","))
            append('|')
            append((referenceStats.averageLuminance * 1000f).roundToInt())
            append('|')
            append((referenceStats.contrast * 1000f).roundToInt())
            append('|')
            append((referenceStats.averageSaturation * 1000f).roundToInt())
            append('|')
            append((referenceStats.warmth * 1000f).roundToInt())
            append('|')
            append(referenceStats.toneStops.joinToString(",") { ((it * 1000.0).roundToInt()).toString() })
        }
    )
}

private fun optimizeColorCardImitateAnalysis(
    sourceBitmap: Bitmap,
    sourceStats: ColorCardImitateImageStats,
    referenceStats: ColorCardImitateImageStats,
    baseAnalysis: ColorCardImitateAnalysis
): ColorCardImitateAnalysis {
    val sourcePalette = sourceStats.paletteColors.take(8)
    val targetPalette = referenceStats.paletteColors.take(8)
    if (sourcePalette.isEmpty() || targetPalette.isEmpty()) {
        return baseAnalysis
    }
    val optimizationBitmap = buildColorCardImitateOptimizationBitmap(
        sourceBitmap = sourceBitmap,
        maxSide = COLOR_CARD_IMITATE_OPTIMIZATION_MAX_SIDE
    )
    val baselineState = ColorCardImitateOptimizationState(
        color = baseAnalysis.suggestedColor,
        tone = baseAnalysis.suggestedTone,
        saturation = baseAnalysis.suggestedSaturation,
        brightness = baseAnalysis.suggestedBrightness,
        strength = baseAnalysis.suggestedStrength,
        sharpen = baseAnalysis.suggestedSharpen
    )
    val scoreCache = HashMap<String, Double>()

    fun evaluate(state: ColorCardImitateOptimizationState): Double {
        val cacheKey = buildColorCardImitateOptimizationKey(state)
        scoreCache[cacheKey]?.let { return it }
        val renderedBitmap = renderColorCardImitatePreviewBitmapForOptimization(
            sourceBitmap = optimizationBitmap,
            sourceColors = sourcePalette,
            targetColors = targetPalette,
            transferProfile = baseAnalysis.transferProfile,
            color = state.color,
            tone = state.tone,
            saturation = state.saturation,
            brightness = state.brightness,
            sharpen = state.sharpen,
            paletteStrength = state.strength
        )
        val renderedStats = try {
            computeColorCardImitateStats(renderedBitmap)
        } finally {
            if (!renderedBitmap.isRecycled) renderedBitmap.recycle()
        }
        return scoreColorCardImitateOptimizationResult(
            renderedStats = renderedStats,
            referenceStats = referenceStats,
            candidate = state,
            baseline = baselineState
        ).also { scoreCache[cacheKey] = it }
    }

    var best = baselineState.copy(score = evaluate(baselineState))
    try {
        repeat(COLOR_CARD_IMITATE_OPTIMIZATION_PASSES) { pass ->
            val scale = if (pass == 0) 1f else 0.55f
            best = searchColorCardImitateDimension(
                current = best,
                deltas = listOf((-0.045f * scale), 0f, (0.045f * scale)),
                mutate = { state, delta ->
                    state.copy(color = (state.color + delta).coerceIn(-0.16f, 0.16f))
                },
                evaluate = ::evaluate
            )
            best = searchColorCardImitateDimension(
                current = best,
                deltas = listOf((-0.07f * scale), 0f, (0.07f * scale)),
                mutate = { state, delta ->
                    state.copy(tone = (state.tone + delta).coerceIn(-0.24f, 0.24f))
                },
                evaluate = ::evaluate
            )
            best = searchColorCardImitateDimension(
                current = best,
                deltas = listOf((-0.08f * scale), 0f, (0.08f * scale)),
                mutate = { state, delta ->
                    state.copy(saturation = (state.saturation + delta).coerceIn(-0.28f, 0.28f))
                },
                evaluate = ::evaluate
            )
            best = searchColorCardImitateDimension(
                current = best,
                deltas = listOf((-0.06f * scale), 0f, (0.06f * scale)),
                mutate = { state, delta ->
                    state.copy(brightness = (state.brightness + delta).coerceIn(-0.22f, 0.22f))
                },
                evaluate = ::evaluate
            )
            best = searchColorCardImitateDimension(
                current = best,
                deltas = listOf((-0.10f * scale), 0f, (0.10f * scale)),
                mutate = { state, delta ->
                    state.copy(strength = (state.strength + delta).coerceIn(0.50f, 0.96f))
                },
                evaluate = ::evaluate
            )
        }
        best = searchColorCardImitateDimension(
            current = best,
            deltas = listOf(-0.04f, 0f, 0.04f),
            mutate = { state, delta ->
                state.copy(sharpen = (state.sharpen + delta).coerceIn(-0.10f, 0.18f))
            },
            evaluate = ::evaluate
        )
        return baseAnalysis.copy(
            suggestedColor = best.color,
            suggestedTone = best.tone,
            suggestedSaturation = best.saturation,
            suggestedBrightness = best.brightness,
            suggestedStrength = best.strength,
            suggestedSharpen = best.sharpen
        )
    } finally {
        if (optimizationBitmap !== sourceBitmap && !optimizationBitmap.isRecycled) {
            optimizationBitmap.recycle()
        }
    }
}

private fun buildColorCardImitateOptimizationBitmap(
    sourceBitmap: Bitmap,
    maxSide: Int
): Bitmap {
    val safeMaxSide = maxSide.coerceAtLeast(48)
    val currentMaxSide = max(sourceBitmap.width, sourceBitmap.height).coerceAtLeast(1)
    if (currentMaxSide <= safeMaxSide) {
        return sourceBitmap
    }
    val scale = safeMaxSide.toFloat() / currentMaxSide.toFloat()
    val targetWidth = (sourceBitmap.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (sourceBitmap.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true)
}

private fun searchColorCardImitateDimension(
    current: ColorCardImitateOptimizationState,
    deltas: List<Float>,
    mutate: (ColorCardImitateOptimizationState, Float) -> ColorCardImitateOptimizationState,
    evaluate: (ColorCardImitateOptimizationState) -> Double
): ColorCardImitateOptimizationState {
    var best = current
    deltas.forEach { delta ->
        val candidate = mutate(current, delta)
        val score = evaluate(candidate)
        if (score + 0.000001 < best.score) {
            best = candidate.copy(score = score)
        }
    }
    return best
}

private fun buildColorCardImitateOptimizationKey(
    state: ColorCardImitateOptimizationState
): String {
    return listOf(
        (state.color * 1000f).roundToInt(),
        (state.tone * 1000f).roundToInt(),
        (state.saturation * 1000f).roundToInt(),
        (state.brightness * 1000f).roundToInt(),
        (state.strength * 1000f).roundToInt(),
        (state.sharpen * 1000f).roundToInt()
    ).joinToString(separator = "|")
}

private fun scoreColorCardImitateOptimizationResult(
    renderedStats: ColorCardImitateImageStats,
    referenceStats: ColorCardImitateImageStats,
    candidate: ColorCardImitateOptimizationState,
    baseline: ColorCardImitateOptimizationState
): Double {
    val toneCurveDiff = renderedStats.toneStops.zip(referenceStats.toneStops)
        .map { (rendered, reference) -> abs(rendered - reference) }
        .average()
    val paletteDiff = colorCardImitatePaletteDistance(
        left = renderedStats.paletteColors,
        right = referenceStats.paletteColors
    )
    val shadowDiff = colorCardImitateLabDistance(renderedStats.shadowColor, referenceStats.shadowColor)
    val highlightDiff = colorCardImitateLabDistance(renderedStats.highlightColor, referenceStats.highlightColor)
    val parameterPenalty =
        (abs(candidate.color - baseline.color) * 0.14f) +
            (abs(candidate.tone - baseline.tone) * 0.08f) +
            (abs(candidate.saturation - baseline.saturation) * 0.08f) +
            (abs(candidate.brightness - baseline.brightness) * 0.08f) +
            (abs(candidate.strength - baseline.strength) * 0.12f) +
            (abs(candidate.sharpen - baseline.sharpen) * 0.05f)
    val aggressionPenalty =
        (abs(candidate.color) * 0.04f) +
            (abs(candidate.tone) * 0.025f) +
            (abs(candidate.saturation) * 0.025f) +
            (abs(candidate.brightness) * 0.02f)

    return (
        (abs(renderedStats.averageLuminance - referenceStats.averageLuminance) * 1.55f) +
            (abs(renderedStats.contrast - referenceStats.contrast) * 1.85f) +
            (abs(renderedStats.averageSaturation - referenceStats.averageSaturation) * 1.45f) +
            (abs(renderedStats.warmth - referenceStats.warmth) * 0.95f) +
            (toneCurveDiff * 1.10) +
            (paletteDiff * 0.022) +
            (shadowDiff * 0.006) +
            (highlightDiff * 0.005) +
            parameterPenalty +
            aggressionPenalty
        )
}

private fun describeColorCardImitateMood(stats: ColorCardImitateImageStats): String {
    val temperature = when {
        stats.warmth >= 0.12f -> "偏暖"
        stats.warmth <= -0.12f -> "偏冷"
        else -> "中性"
    }
    val contrast = when {
        stats.contrast >= 0.62f -> "高反差"
        stats.contrast <= 0.32f -> "低反差"
        else -> "中等反差"
    }
    val saturation = when {
        stats.averageSaturation >= 0.55f -> "高饱和"
        stats.averageSaturation <= 0.24f -> "低饱和"
        else -> "中等饱和"
    }
    return "$temperature · $contrast · $saturation"
}

private fun extractColorCardImitatePalette(
    buckets: Map<Int, ColorCardImitatePaletteBucket>
): List<Int> {
    val rankedColors = buckets.values
        .asSequence()
        .filter { it.weight > 0.08 }
        .map { bucket -> bucket.averageColor() to bucket.weight }
        .sortedByDescending { it.second }
        .map { it.first }
        .toList()

    val palette = buildList {
        rankedColors.forEach { color ->
            if (none { existing -> colorCardImitateLabDistance(existing, color) < 18.0 }) {
                add(color)
            }
            if (size >= 8) return@forEach
        }
    }
    if (palette.isNotEmpty()) {
        return palette
    }
    val fallback = buckets.values.maxByOrNull { it.weight }?.averageColor()
    return listOf(fallback ?: 0xFFD9D9D9.toInt())
}

private fun quantizeColorCardImitateColor(colorInt: Int): Int {
    fun quantize(channel: Int): Int {
        val step = 24
        return ((channel / step) * step + (step / 2)).coerceIn(0, 255)
    }
    return AndroidColor.rgb(
        quantize(AndroidColor.red(colorInt)),
        quantize(AndroidColor.green(colorInt)),
        quantize(AndroidColor.blue(colorInt))
    )
}

private fun colorCardImitateLabDistance(
    first: Int,
    second: Int
): Double {
    val firstLab = colorCardTraceLabFromRgb(
        red = AndroidColor.red(first),
        green = AndroidColor.green(first),
        blue = AndroidColor.blue(first)
    )
    val secondLab = colorCardTraceLabFromRgb(
        red = AndroidColor.red(second),
        green = AndroidColor.green(second),
        blue = AndroidColor.blue(second)
    )
    val deltaL = (firstLab.l - secondLab.l) * 0.75
    val deltaA = firstLab.a - secondLab.a
    val deltaB = firstLab.b - secondLab.b
    return sqrt((deltaL * deltaL) + (deltaA * deltaA) + (deltaB * deltaB))
}

private fun colorCardImitatePaletteDistance(
    left: List<Int>,
    right: List<Int>
): Double {
    val normalizedLeft = left.take(6)
    val normalizedRight = right.take(6)
    if (normalizedLeft.isEmpty() || normalizedRight.isEmpty()) return 0.0

    fun averageMinimumDistance(
        source: List<Int>,
        target: List<Int>
    ): Double {
        return source.map { sourceColor ->
            target.minOf { targetColor ->
                colorCardImitateLabDistance(sourceColor, targetColor)
            }
        }.average()
    }

    return (
        averageMinimumDistance(normalizedLeft, normalizedRight) +
            averageMinimumDistance(normalizedRight, normalizedLeft)
        ) / 2.0
}

private fun resolveDominantBucketColor(
    buckets: DoubleArray,
    luminance: Float,
    saturation: Float
): Int {
    val bestIndex = buckets.indices.maxByOrNull { index -> buckets[index] } ?: 0
    val hue = (bestIndex.toFloat() + 0.5f) / buckets.size.toFloat() * 360f
    return AndroidColor.HSVToColor(
        floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0.08f, 0.78f),
            luminance.coerceIn(0.10f, 0.96f)
        )
    )
}

private fun percentileFromHistogram(
    histogram: DoubleArray,
    pixelWeight: Double,
    percentile: Float
): Int {
    val clampedPercentile = percentile.coerceIn(0f, 1f)
    val threshold = (pixelWeight * clampedPercentile.toDouble()).coerceAtLeast(0.0)
    var cumulative = 0.0
    histogram.forEachIndexed { index, count ->
        cumulative += count
        if (cumulative >= threshold) {
            return index
        }
    }
    return histogram.lastIndex
}

private fun resolveColorCardImitateCenterWeight(
    x: Int,
    y: Int,
    width: Int,
    height: Int
): Double {
    val normalizedX = if (width <= 1) 0.5 else x.toDouble() / (width - 1).toDouble()
    val normalizedY = if (height <= 1) 0.5 else y.toDouble() / (height - 1).toDouble()
    val dx = normalizedX - 0.5
    val dy = normalizedY - 0.5
    val distance = (sqrt((dx * dx) + (dy * dy)) / 0.70710678118).coerceIn(0.0, 1.0)
    return (1.18 - (distance * 0.34)).coerceIn(0.82, 1.18)
}

private fun resolveColorCardImitateTonalWeight(
    luminance: Float,
    saturation: Float,
    centerWeight: Double
): Double {
    val extremePenalty = when {
        luminance <= 0.018f || luminance >= 0.988f -> 0.52
        luminance <= 0.04f || luminance >= 0.97f -> 0.76
        else -> 1.0
    }
    val saturationBoost = 0.88 + (saturation.coerceIn(0f, 1f) * 0.22f)
    return (centerWeight * extremePenalty * saturationBoost).coerceIn(0.28, 1.85)
}

private fun resolveColorCardImitatePaletteWeight(
    luminance: Float,
    saturation: Float,
    lab: ColorCardTraceLab,
    centerWeight: Double
): Double {
    val chroma = sqrt((lab.a * lab.a) + (lab.b * lab.b))
    val chromaBoost = 0.30 + ((chroma / 48.0).coerceIn(0.0, 1.0) * 1.05)
    val saturationBoost = 0.26 + (saturation.coerceIn(0f, 1f) * 0.94f)
    val neutralPenalty = if (saturation < 0.10f && luminance in 0.08f..0.92f) 0.24 else 1.0
    val extremePenalty = when {
        luminance <= 0.03f || luminance >= 0.97f -> 0.52
        else -> 1.0
    }
    return (centerWeight * chromaBoost * saturationBoost * neutralPenalty * extremePenalty)
        .coerceIn(0.0, 2.8)
}
