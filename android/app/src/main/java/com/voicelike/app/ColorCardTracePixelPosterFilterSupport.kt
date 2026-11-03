package com.voicelike.app

import android.graphics.Bitmap
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val COLOR_CARD_TRACE_PIXEL_POSTER_FIXED_STRENGTH = 1f

internal enum class ColorCardTracePixelPosterStyle {
    DuotoneThreshold,
    ContinuousDuotone,
    SourceColor
}

internal data class ColorCardTracePixelPosterPreset(
    val id: String,
    val sourceTag: String,
    val style: ColorCardTracePixelPosterStyle,
    val darkColor: Int,
    val lightColor: Int,
    val blockCountLongSide: Float,
    val threshold: Float,
    val contrast: Float,
    val noiseAmount: Float,
    val gridLineWidth: Float = 0f,
    val gridLineOpacity: Float = 0f,
    val detailBoost: Float = 0f,
    val shadowDetailBias: Float = 0f,
    val previewColors: List<Int>
)

internal data class ColorCardTracePixelPosterParams(
    val enabled: Boolean,
    val style: ColorCardTracePixelPosterStyle,
    val darkColor: Int,
    val lightColor: Int,
    val blockCountLongSide: Float,
    val threshold: Float,
    val contrast: Float,
    val noiseAmount: Float,
    val gridLineWidth: Float,
    val gridLineOpacity: Float,
    val detailBoost: Float,
    val shadowDetailBias: Float,
    val strength: Float
)

internal data class ColorCardTracePixelPosterGrid(
    val columns: Int,
    val rows: Int
)

private val COLOR_CARD_TRACE_PIXEL_POSTER_DETAIL_SAMPLE_OFFSETS = floatArrayOf(
    0.18f, 0.18f,
    0.50f, 0.18f,
    0.82f, 0.18f,
    0.18f, 0.50f,
    0.50f, 0.50f,
    0.82f, 0.50f,
    0.18f, 0.82f,
    0.50f, 0.82f,
    0.82f, 0.82f
)

private val COLOR_CARD_TRACE_PIXEL_POSTER_PRESETS = listOf(
    ColorCardTracePixelPosterPreset(
        id = "pixel-yellow",
        sourceTag = "Pixel Poster",
        style = ColorCardTracePixelPosterStyle.DuotoneThreshold,
        darkColor = 0xFF050505.toInt(),
        lightColor = 0xFFF1D24B.toInt(),
        blockCountLongSide = 92f,
        threshold = 0.56f,
        contrast = 2.45f,
        noiseAmount = 0f,
        previewColors = listOf(0xFF050505.toInt(), 0xFFF1D24B.toInt())
    ),
    ColorCardTracePixelPosterPreset(
        id = "pixel-red",
        sourceTag = "Pixel Poster",
        style = ColorCardTracePixelPosterStyle.DuotoneThreshold,
        darkColor = 0xFF050202.toInt(),
        lightColor = 0xFFF61B12.toInt(),
        blockCountLongSide = 96f,
        threshold = 0.54f,
        contrast = 2.70f,
        noiseAmount = 0f,
        previewColors = listOf(0xFF050202.toInt(), 0xFFF61B12.toInt())
    ),
    ColorCardTracePixelPosterPreset(
        id = "pixel-blue",
        sourceTag = "Pixel Poster",
        style = ColorCardTracePixelPosterStyle.DuotoneThreshold,
        darkColor = 0xFF03030A.toInt(),
        lightColor = 0xFF0919FF.toInt(),
        blockCountLongSide = 92f,
        threshold = 0.58f,
        contrast = 2.85f,
        noiseAmount = 0f,
        previewColors = listOf(0xFF03030A.toInt(), 0xFF0919FF.toInt())
    ),
    ColorCardTracePixelPosterPreset(
        id = "pixel-gray",
        sourceTag = "Pixel Poster",
        style = ColorCardTracePixelPosterStyle.DuotoneThreshold,
        darkColor = 0xFF050505.toInt(),
        lightColor = 0xFFF0ECE4.toInt(),
        blockCountLongSide = 104f,
        threshold = 0.53f,
        contrast = 3.10f,
        noiseAmount = 0f,
        previewColors = listOf(0xFF050505.toInt(), 0xFFF0ECE4.toInt())
    ),
    // Closer to the "threshold + multiply with primary color" Photoshop workflow.
    ColorCardTracePixelPosterPreset(
        id = "pixel-primary-yellow",
        sourceTag = "Primary Pixel",
        style = ColorCardTracePixelPosterStyle.ContinuousDuotone,
        darkColor = 0xFF171303.toInt(),
        lightColor = 0xFFF6D85B.toInt(),
        blockCountLongSide = 1f,
        threshold = 0.48f,
        contrast = 1.72f,
        noiseAmount = 0f,
        previewColors = listOf(0xFF171303.toInt(), 0xFFF6D85B.toInt())
    ),
    ColorCardTracePixelPosterPreset(
        id = "pixel-primary-blue",
        sourceTag = "Primary Pixel",
        style = ColorCardTracePixelPosterStyle.ContinuousDuotone,
        darkColor = 0xFF050A1D.toInt(),
        lightColor = 0xFF2D63FF.toInt(),
        blockCountLongSide = 1f,
        threshold = 0.50f,
        contrast = 1.78f,
        noiseAmount = 0f,
        previewColors = listOf(0xFF050A1D.toInt(), 0xFF2D63FF.toInt())
    ),
    ColorCardTracePixelPosterPreset(
        id = "pixel-primary-red",
        sourceTag = "Primary Pixel",
        style = ColorCardTracePixelPosterStyle.ContinuousDuotone,
        darkColor = 0xFF190607.toInt(),
        lightColor = 0xFFFF4A40.toInt(),
        blockCountLongSide = 1f,
        threshold = 0.49f,
        contrast = 1.70f,
        noiseAmount = 0f,
        previewColors = listOf(0xFF190607.toInt(), 0xFFFF4A40.toInt())
    ),
    ColorCardTracePixelPosterPreset(
        id = "pixel-original",
        sourceTag = "Original Pixel",
        style = ColorCardTracePixelPosterStyle.SourceColor,
        darkColor = 0xFF1A1A1A.toInt(),
        lightColor = 0xFFEAEAEA.toInt(),
        blockCountLongSide = 118f,
        threshold = 0.50f,
        contrast = 1f,
        noiseAmount = 0f,
        previewColors = listOf(
            0xFF1F2937.toInt(),
            0xFF9CA3AF.toInt(),
            0xFFF59E0B.toInt(),
            0xFF2563EB.toInt()
        )
    ),
    ColorCardTracePixelPosterPreset(
        id = "pixel-beads",
        sourceTag = "Beads Pixel",
        style = ColorCardTracePixelPosterStyle.SourceColor,
        darkColor = 0xFF111111.toInt(),
        lightColor = 0xFFF3F3F3.toInt(),
        blockCountLongSide = 34f,
        threshold = 0.50f,
        contrast = 1f,
        noiseAmount = 0f,
        gridLineWidth = 0.075f,
        gridLineOpacity = 0.72f,
        detailBoost = 0.62f,
        shadowDetailBias = 0.82f,
        previewColors = listOf(
            0xFF111111.toInt(),
            0xFFE85D4F.toInt(),
            0xFFF1C94B.toInt(),
            0xFF4B86E8.toInt()
        )
    )
)

private val COLOR_CARD_TRACE_DISABLED_PIXEL_POSTER_PARAMS = ColorCardTracePixelPosterParams(
    enabled = false,
    style = ColorCardTracePixelPosterStyle.DuotoneThreshold,
    darkColor = 0xFF000000.toInt(),
    lightColor = 0xFFFFFFFF.toInt(),
    blockCountLongSide = 1f,
    threshold = 0.5f,
    contrast = 1f,
    noiseAmount = 0f,
    gridLineWidth = 0f,
    gridLineOpacity = 0f,
    detailBoost = 0f,
    shadowDetailBias = 0f,
    strength = 0f
)

internal fun buildColorCardTracePixelPosterCandidates(): List<ColorCardTraceFilterCandidate> {
    val displayTitles = buildColorCardTracePixelPosterDisplayTitles()
    return COLOR_CARD_TRACE_PIXEL_POSTER_PRESETS.map { preset ->
        ColorCardTraceFilterCandidate(
            paletteId = colorCardTracePixelPosterPaletteId(preset),
            kind = ColorCardTraceFilterKind.PixelPoster,
            title = displayTitles.getValue(preset.id),
            sourceTag = preset.sourceTag,
            colors = buildColorCardTracePixelPosterPreviewPalette(preset),
            score = -1.0,
            pixelPosterPreset = preset
        )
    }
}

internal fun resolveColorCardTracePixelPosterParams(
    candidate: ColorCardTraceFilterCandidate?,
    paletteStrength: Float
): ColorCardTracePixelPosterParams {
    val preset = candidate?.pixelPosterPreset
        ?.takeIf { candidate.kind == ColorCardTraceFilterKind.PixelPoster }
        ?: return COLOR_CARD_TRACE_DISABLED_PIXEL_POSTER_PARAMS
    return ColorCardTracePixelPosterParams(
        enabled = true,
        style = preset.style,
        darkColor = preset.darkColor,
        lightColor = preset.lightColor,
        blockCountLongSide = preset.blockCountLongSide,
        threshold = preset.threshold.coerceIn(0f, 1f),
        contrast = preset.contrast.coerceAtLeast(0.1f),
        noiseAmount = preset.noiseAmount.coerceIn(0f, 1f),
        gridLineWidth = preset.gridLineWidth.coerceIn(0f, 0.45f),
        gridLineOpacity = preset.gridLineOpacity.coerceIn(0f, 1f),
        detailBoost = preset.detailBoost.coerceIn(0f, 1f),
        shadowDetailBias = preset.shadowDetailBias.coerceIn(0f, 1f),
        strength = COLOR_CARD_TRACE_PIXEL_POSTER_FIXED_STRENGTH
    )
}

internal fun resolveColorCardTracePixelPosterGrid(
    sourceWidth: Int,
    sourceHeight: Int,
    blockCountLongSide: Float
): ColorCardTracePixelPosterGrid {
    val safeWidth = sourceWidth.coerceAtLeast(1)
    val safeHeight = sourceHeight.coerceAtLeast(1)
    val safeLongSideBlocks = blockCountLongSide.roundToInt().coerceAtLeast(1)
    return if (safeWidth >= safeHeight) {
        val aspect = safeWidth.toFloat() / safeHeight.toFloat()
        ColorCardTracePixelPosterGrid(
            columns = min(safeLongSideBlocks, safeWidth),
            rows = min(
                max((safeLongSideBlocks / aspect).roundToInt(), 1),
                safeHeight
            )
        )
    } else {
        val aspect = safeHeight.toFloat() / safeWidth.toFloat()
        ColorCardTracePixelPosterGrid(
            columns = min(
                max((safeLongSideBlocks / aspect).roundToInt(), 1),
                safeWidth
            ),
            rows = min(safeLongSideBlocks, safeHeight)
        )
    }
}

internal fun renderColorCardTracePixelPosterBitmap(
    sourceBitmap: Bitmap,
    candidate: ColorCardTraceFilterCandidate,
    color: Float,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    strength: Float,
    faceRegions: List<ColorCardTraceFaceRegion>
): Bitmap {
    val params = resolveColorCardTracePixelPosterParams(candidate, strength)
    if (!params.enabled) {
        return sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val detailAdjustedSourcePixels = applyColorCardTraceSourceDetailAdjustments(
        sourcePixels = sourcePixels,
        width = width,
        height = height,
        sharpen = sharpen
    )

    val usesBlockSampling = params.style != ColorCardTracePixelPosterStyle.ContinuousDuotone
    val grid = if (usesBlockSampling) {
        resolveColorCardTracePixelPosterGrid(
            sourceWidth = width,
            sourceHeight = height,
            blockCountLongSide = params.blockCountLongSide
        )
    } else {
        ColorCardTracePixelPosterGrid(columns = width.coerceAtLeast(1), rows = height.coerceAtLeast(1))
    }
    val cellWidth = if (usesBlockSampling) {
        width.toFloat() / grid.columns.toFloat()
    } else {
        1f
    }
    val cellHeight = if (usesBlockSampling) {
        height.toFloat() / grid.rows.toFloat()
    } else {
        1f
    }
    val sampleXByColumn = if (usesBlockSampling) {
        IntArray(grid.columns) { column ->
            (((column + 0.5f) * cellWidth).roundToInt()).coerceIn(0, width - 1)
        }
    } else {
        IntArray(0)
    }
    val sampleYByRow = if (usesBlockSampling) {
        IntArray(grid.rows) { row ->
            (((row + 0.5f) * cellHeight).roundToInt()).coerceIn(0, height - 1)
        }
    } else {
        IntArray(0)
    }
    val blockColumnByX = if (usesBlockSampling) {
        IntArray(width) { x ->
            min(floor((x / cellWidth).toDouble()).toInt(), grid.columns - 1).coerceAtLeast(0)
        }
    } else {
        IntArray(0)
    }
    val blockRowByY = if (usesBlockSampling) {
        IntArray(height) { y ->
            min(floor((y / cellHeight).toDouble()).toInt(), grid.rows - 1).coerceAtLeast(0)
        }
    } else {
        IntArray(0)
    }

    val xNormalizer = (width - 1).coerceAtLeast(1).toFloat()
    val yNormalizer = (height - 1).coerceAtLeast(1).toFloat()
    for (y in 0 until height) {
        val blockY = if (usesBlockSampling) blockRowByY[y] else y
        val sampleY = if (usesBlockSampling) sampleYByRow[blockY] else y
        val normalizedY = if (height <= 1) 0.5f else y / yNormalizer
        val rowOffset = y * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val sourceColor = detailAdjustedSourcePixels[index]
            val alpha = colorCardTracePixelPosterAlpha(sourceColor)
            if (alpha == 0) {
                outputPixels[index] = sourceColor
                continue
            }

            val sourceRed = colorCardTracePixelPosterRed(sourceColor)
            val sourceGreen = colorCardTracePixelPosterGreen(sourceColor)
            val sourceBlue = colorCardTracePixelPosterBlue(sourceColor)
            val protectionAmount = colorCardTraceSkinProtectionAmount(
                normalizedX = if (width <= 1) 0.5f else x / xNormalizer,
                normalizedY = normalizedY,
                red = sourceRed,
                green = sourceGreen,
                blue = sourceBlue,
                faceRegions = faceRegions
            )
            val paletteEffectScale = 1f - (
                protectionAmount * COLOR_CARD_TRACE_SKIN_PROTECTION_PALETTE_RECOVERY
                )
            val chromaScale = 1f - (
                protectionAmount * COLOR_CARD_TRACE_SKIN_PROTECTION_CREATIVE_RECOVERY
                )
            val tonalScale = 1f - (
                protectionAmount * COLOR_CARD_TRACE_SKIN_PROTECTION_TONAL_RECOVERY
                )
            val blockX = if (usesBlockSampling) blockColumnByX[x] else x
            val sampleX = if (usesBlockSampling) sampleXByColumn[blockX] else x
            val sampledColor = if (usesBlockSampling) {
                detailAdjustedSourcePixels[(sampleY * width) + sampleX]
            } else {
                sourceColor
            }
            val posterBaseRgb = if (
                params.style == ColorCardTracePixelPosterStyle.SourceColor &&
                usesBlockSampling &&
                params.detailBoost > 0f
            ) {
                resolveColorCardTracePixelPosterDetailedSourceRgb(
                    sourcePixels = detailAdjustedSourcePixels,
                    width = width,
                    height = height,
                    blockX = blockX,
                    blockY = blockY,
                    grid = grid,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    params = params
                )
            } else {
                resolveColorCardTracePixelPosterBaseRgb(
                    sampledColor = sampledColor,
                    blockX = blockX,
                    blockY = blockY,
                    params = params
                )
            }
            val gridMask = if (usesBlockSampling) {
                resolveColorCardTracePixelPosterGridMask(
                    x = x,
                    y = y,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    lineWidth = params.gridLineWidth
                )
            } else {
                0f
            }
            val workingStrength = (params.strength * paletteEffectScale).coerceIn(0f, 1f)
            val baseRgbWithoutGrid = ColorCardTraceRgb(
                red = colorCardTracePixelPosterLerpChannel(sourceRed, posterBaseRgb.red, workingStrength),
                green = colorCardTracePixelPosterLerpChannel(sourceGreen, posterBaseRgb.green, workingStrength),
                blue = colorCardTracePixelPosterLerpChannel(sourceBlue, posterBaseRgb.blue, workingStrength)
            )
            val adjustedRgb = applyColorCardTraceCreativeAdjustments(
                lab = colorCardTraceLabFromRgb(
                    red = baseRgbWithoutGrid.red,
                    green = baseRgbWithoutGrid.green,
                    blue = baseRgbWithoutGrid.blue
                ),
                color = color,
                tone = tone,
                saturation = saturation,
                brightness = brightness,
                colorAxis = colorAxis,
                chromaScale = chromaScale,
                tonalScale = tonalScale
            )
            val finalRgb = applyColorCardTracePixelPosterGridOverlay(
                rgb = adjustedRgb,
                gridMask = gridMask,
                opacity = params.gridLineOpacity
            )
            outputPixels[index] = colorCardTracePixelPosterArgb(
                alpha,
                finalRgb.red,
                finalRgb.green,
                finalRgb.blue
            )
        }
    }

    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun resolveColorCardTracePixelPosterGridMask(
    x: Int,
    y: Int,
    cellWidth: Float,
    cellHeight: Float,
    lineWidth: Float
): Float {
    if (lineWidth <= 0f || cellWidth <= 1f || cellHeight <= 1f) return 0f
    val localX = ((x + 0.5f) / cellWidth) - floor((x + 0.5f) / cellWidth)
    val localY = ((y + 0.5f) / cellHeight) - floor((y + 0.5f) / cellHeight)
    val minimumPreviewPixels = 1.15f
    val edgeWidth = max(
        lineWidth,
        max(
            minimumPreviewPixels / cellWidth,
            minimumPreviewPixels / cellHeight
        )
    ).coerceIn(0f, 0.49f)
    val verticalLine = localX <= edgeWidth || localX >= (1f - edgeWidth)
    val horizontalLine = localY <= edgeWidth || localY >= (1f - edgeWidth)
    return if (verticalLine || horizontalLine) 1f else 0f
}

private fun applyColorCardTracePixelPosterGridOverlay(
    rgb: ColorCardTraceRgb,
    gridMask: Float,
    opacity: Float
): ColorCardTraceRgb {
    val strength = (gridMask * opacity).coerceIn(0f, 1f)
    if (strength <= 0f) return rgb
    return ColorCardTraceRgb(
        red = colorCardTracePixelPosterLerpChannel(rgb.red, 0, strength),
        green = colorCardTracePixelPosterLerpChannel(rgb.green, 0, strength),
        blue = colorCardTracePixelPosterLerpChannel(rgb.blue, 0, strength)
    )
}

private fun resolveColorCardTracePixelPosterDetailedSourceRgb(
    sourcePixels: IntArray,
    width: Int,
    height: Int,
    blockX: Int,
    blockY: Int,
    grid: ColorCardTracePixelPosterGrid,
    cellWidth: Float,
    cellHeight: Float,
    params: ColorCardTracePixelPosterParams
): ColorCardTraceRgb {
    var sumRed = 0f
    var sumGreen = 0f
    var sumBlue = 0f
    var sumLuma = 0f
    var minLuma = Float.POSITIVE_INFINITY
    var maxLuma = Float.NEGATIVE_INFINITY
    var darkestColor = 0

    var index = 0
    while (index < COLOR_CARD_TRACE_PIXEL_POSTER_DETAIL_SAMPLE_OFFSETS.size) {
        val offsetX = COLOR_CARD_TRACE_PIXEL_POSTER_DETAIL_SAMPLE_OFFSETS[index]
        val offsetY = COLOR_CARD_TRACE_PIXEL_POSTER_DETAIL_SAMPLE_OFFSETS[index + 1]
        val sampleX = (((blockX + offsetX) * cellWidth).roundToInt()).coerceIn(0, width - 1)
        val sampleY = (((blockY + offsetY) * cellHeight).roundToInt()).coerceIn(0, height - 1)
        val sampleColor = sourcePixels[(sampleY * width) + sampleX]
        val red = colorCardTracePixelPosterRed(sampleColor)
        val green = colorCardTracePixelPosterGreen(sampleColor)
        val blue = colorCardTracePixelPosterBlue(sampleColor)
        val luma = (
            (red / 255f) * 0.299f +
                (green / 255f) * 0.587f +
                (blue / 255f) * 0.114f
            ).coerceIn(0f, 1f)
        sumRed += red.toFloat()
        sumGreen += green.toFloat()
        sumBlue += blue.toFloat()
        sumLuma += luma
        if (luma < minLuma) {
            minLuma = luma
            darkestColor = sampleColor
        }
        if (luma > maxLuma) {
            maxLuma = luma
        }
        index += 2
    }

    val sampleCount = (COLOR_CARD_TRACE_PIXEL_POSTER_DETAIL_SAMPLE_OFFSETS.size / 2).toFloat()
        .coerceAtLeast(1f)
    val averageRed = (sumRed / sampleCount).roundToInt().coerceIn(0, 255)
    val averageGreen = (sumGreen / sampleCount).roundToInt().coerceIn(0, 255)
    val averageBlue = (sumBlue / sampleCount).roundToInt().coerceIn(0, 255)
    val averageLuma = (sumLuma / sampleCount).coerceIn(0f, 1f)
    val contrastRange = (maxLuma - minLuma).coerceIn(0f, 1f)
    val edgeResponse = smoothStepColorCardTracePixelPoster(
        edge0 = 0.10f,
        edge1 = 0.34f,
        value = contrastRange
    )
    val shadowResponse = smoothStepColorCardTracePixelPoster(
        edge0 = 0.04f,
        edge1 = 0.24f,
        value = (averageLuma - minLuma).coerceIn(0f, 1f)
    ) * params.shadowDetailBias
    val detailMix = (params.detailBoost * edgeResponse * shadowResponse)
        .coerceIn(0f, 1f)

    return ColorCardTraceRgb(
        red = colorCardTracePixelPosterLerpChannel(
            averageRed,
            colorCardTracePixelPosterRed(darkestColor),
            detailMix
        ),
        green = colorCardTracePixelPosterLerpChannel(
            averageGreen,
            colorCardTracePixelPosterGreen(darkestColor),
            detailMix
        ),
        blue = colorCardTracePixelPosterLerpChannel(
            averageBlue,
            colorCardTracePixelPosterBlue(darkestColor),
            detailMix
        )
    )
}

private fun buildColorCardTracePixelPosterDisplayTitles(): Map<String, String> {
    return COLOR_CARD_TRACE_PIXEL_POSTER_PRESETS.associate { preset ->
        preset.id to resolveColorCardTracePixelPosterDisplayCode(preset.id)
    }.toMap()
}

private fun colorCardTracePixelPosterPaletteId(
    preset: ColorCardTracePixelPosterPreset
): String = "builtin_pixel:${preset.id}"

private fun buildColorCardTracePixelPosterPreviewPalette(
    preset: ColorCardTracePixelPosterPreset
): List<Int> {
    return listOf(
        *preset.previewColors.toTypedArray()
    )
}

internal fun resolveColorCardTracePixelPosterBaseRgb(
    sampledColor: Int,
    blockX: Int,
    blockY: Int,
    params: ColorCardTracePixelPosterParams
): ColorCardTraceRgb {
    if (params.style == ColorCardTracePixelPosterStyle.SourceColor) {
        return ColorCardTraceRgb(
            red = colorCardTracePixelPosterRed(sampledColor),
            green = colorCardTracePixelPosterGreen(sampledColor),
            blue = colorCardTracePixelPosterBlue(sampledColor)
        )
    }
    if (params.style == ColorCardTracePixelPosterStyle.ContinuousDuotone) {
        val lightMix = resolveColorCardTracePixelPosterContinuousLightMix(
            sampledColor = sampledColor,
            params = params
        )
        return ColorCardTraceRgb(
            red = colorCardTracePixelPosterLerpChannel(
                colorCardTracePixelPosterRed(params.darkColor),
                colorCardTracePixelPosterRed(params.lightColor),
                lightMix
            ),
            green = colorCardTracePixelPosterLerpChannel(
                colorCardTracePixelPosterGreen(params.darkColor),
                colorCardTracePixelPosterGreen(params.lightColor),
                lightMix
            ),
            blue = colorCardTracePixelPosterLerpChannel(
                colorCardTracePixelPosterBlue(params.darkColor),
                colorCardTracePixelPosterBlue(params.lightColor),
                lightMix
            )
        )
    }
    val red = colorCardTracePixelPosterRed(sampledColor)
    val green = colorCardTracePixelPosterGreen(sampledColor)
    val blue = colorCardTracePixelPosterBlue(sampledColor)
    val luma = (
        (red / 255f) * 0.299f +
            (green / 255f) * 0.587f +
            (blue / 255f) * 0.114f
        ).coerceIn(0f, 1f)
    val contrasted = (((luma - 0.5f) * params.contrast) + 0.5f).coerceIn(0f, 1f)
    val thresholdNoise = (
        (colorCardTracePixelPosterNoise(blockX, blockY) - 0.5f) * params.noiseAmount
        )
    val thresholdValue = (contrasted + thresholdNoise).coerceIn(0f, 1f)
    val lightMix = if (thresholdValue >= params.threshold) 1f else 0f
    return ColorCardTraceRgb(
        red = colorCardTracePixelPosterLerpChannel(
            colorCardTracePixelPosterRed(params.darkColor),
            colorCardTracePixelPosterRed(params.lightColor),
            lightMix
        ),
        green = colorCardTracePixelPosterLerpChannel(
            colorCardTracePixelPosterGreen(params.darkColor),
            colorCardTracePixelPosterGreen(params.lightColor),
            lightMix
        ),
        blue = colorCardTracePixelPosterLerpChannel(
            colorCardTracePixelPosterBlue(params.darkColor),
            colorCardTracePixelPosterBlue(params.lightColor),
            lightMix
        )
    )
}

internal fun resolveColorCardTracePixelPosterContinuousLightMix(
    sampledColor: Int,
    params: ColorCardTracePixelPosterParams
): Float {
    val red = colorCardTracePixelPosterRed(sampledColor)
    val green = colorCardTracePixelPosterGreen(sampledColor)
    val blue = colorCardTracePixelPosterBlue(sampledColor)
    val luma = (
        (red / 255f) * 0.299f +
            (green / 255f) * 0.587f +
            (blue / 255f) * 0.114f
        ).coerceIn(0f, 1f)
    val maxChannel = max(red, max(green, blue))
    val minChannel = min(red, min(green, blue))
    val saturation = ((maxChannel - minChannel) / 255f).coerceIn(0f, 1f)
    val softContrast = 1f + ((params.contrast.coerceAtLeast(0.1f) - 1f) * 0.42f)
    val remappedLuma = (((luma - 0.5f) * softContrast) + 0.5f).coerceIn(0f, 1f)
    val shadowEdge = (params.threshold - 0.18f).coerceIn(0f, 1f)
    val highlightEdge = (params.threshold + 0.18f).coerceIn(0f, 1f)
    val toneMix = smoothStepColorCardTracePixelPoster(
        edge0 = shadowEdge,
        edge1 = highlightEdge,
        value = remappedLuma
    )
    val vibranceLift = saturation * 0.12f
    return ((toneMix * (0.90f + vibranceLift)) + (luma * 0.10f))
        .coerceIn(0f, 1f)
}

private fun colorCardTracePixelPosterLerpChannel(
    start: Int,
    end: Int,
    amount: Float
): Int {
    return (start + ((end - start) * amount.coerceIn(0f, 1f))).roundToInt().coerceIn(0, 255)
}

private fun colorCardTracePixelPosterNoise(
    blockX: Int,
    blockY: Int
): Float {
    val seed = sin((blockX * 12.9898) + (blockY * 78.233) + 37.719) * 43758.5453
    return (seed - floor(seed)).toFloat().coerceIn(0f, 1f)
}

private fun colorCardTracePixelPosterArgb(
    alpha: Int,
    red: Int,
    green: Int,
    blue: Int
): Int {
    return ((alpha.coerceIn(0, 255) and 0xFF) shl 24) or
        ((red.coerceIn(0, 255) and 0xFF) shl 16) or
        ((green.coerceIn(0, 255) and 0xFF) shl 8) or
        (blue.coerceIn(0, 255) and 0xFF)
}

private fun smoothStepColorCardTracePixelPoster(
    edge0: Float,
    edge1: Float,
    value: Float
): Float {
    if (edge1 <= edge0) {
        return if (value >= edge1) 1f else 0f
    }
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - (2f * t))
}

private fun colorCardTracePixelPosterAlpha(color: Int): Int = (color ushr 24) and 0xFF

private fun colorCardTracePixelPosterRed(color: Int): Int = (color ushr 16) and 0xFF

private fun colorCardTracePixelPosterGreen(color: Int): Int = (color ushr 8) and 0xFF

private fun colorCardTracePixelPosterBlue(color: Int): Int = color and 0xFF
