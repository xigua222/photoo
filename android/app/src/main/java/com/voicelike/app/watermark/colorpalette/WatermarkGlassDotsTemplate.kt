package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal enum class WatermarkGlassDotsPanelTone {
    Clear,
    Pearl,
    Smoke,
    Black,
    White,
    Ocean,
    Rose
}

private data class WatermarkGlassDotsPanelStyle(
    val topColor: Int,
    val bottomColor: Int,
    val strokeColor: Int,
    val highlightAlpha: Int
)

internal fun resolveWatermarkGlassDotsLayoutVariantIndex(
    twoRows: Boolean,
    panelTone: WatermarkGlassDotsPanelTone
): Int = panelTone.ordinal * 2 + if (twoRows) 1 else 0

internal fun resolveWatermarkGlassDotsTwoRows(variantIndex: Int): Boolean = variantIndex.mod(2) == 1

internal fun normalizeWatermarkGlassDotsItemCount(rawCount: Int): Int {
    return rawCount.coerceIn(6, 10)
        .let { if (it % 2 == 0) it else it - 1 }
        .coerceIn(6, 10)
}

internal fun shouldForceWatermarkGlassDotsTwoRows(
    itemCount: Int,
    showHexValue: Boolean
): Boolean = showHexValue && normalizeWatermarkGlassDotsItemCount(itemCount) > 6

internal fun resolveEffectiveWatermarkGlassDotsTwoRows(
    variantIndex: Int,
    itemCount: Int,
    showHexValue: Boolean
): Boolean = resolveWatermarkGlassDotsTwoRows(variantIndex) ||
    shouldForceWatermarkGlassDotsTwoRows(itemCount = itemCount, showHexValue = showHexValue)

internal fun resolveEffectiveWatermarkGlassDotsVariantIndex(
    variantIndex: Int,
    itemCount: Int,
    showHexValue: Boolean
): Int {
    if (!shouldForceWatermarkGlassDotsTwoRows(itemCount = itemCount, showHexValue = showHexValue)) {
        return variantIndex
    }
    return resolveWatermarkGlassDotsLayoutVariantIndex(
        twoRows = true,
        panelTone = resolveWatermarkGlassDotsPanelTone(variantIndex)
    )
}

internal fun resolveWatermarkGlassDotsPanelTone(variantIndex: Int): WatermarkGlassDotsPanelTone {
    return WatermarkGlassDotsPanelTone.entries.getOrElse((variantIndex / 2).coerceAtLeast(0)) {
        WatermarkGlassDotsPanelTone.Clear
    }
}

internal fun resolveWatermarkGlassDotsPanelToneSwatchColor(tone: WatermarkGlassDotsPanelTone): Int = when (tone) {
    WatermarkGlassDotsPanelTone.Clear -> AndroidColor.parseColor("#F3F5F7")
    WatermarkGlassDotsPanelTone.Pearl -> AndroidColor.parseColor("#F5EBDD")
    WatermarkGlassDotsPanelTone.Smoke -> AndroidColor.parseColor("#C4CCD6")
    WatermarkGlassDotsPanelTone.Black -> AndroidColor.parseColor("#2B2E34")
    WatermarkGlassDotsPanelTone.White -> AndroidColor.parseColor("#FFFFFF")
    WatermarkGlassDotsPanelTone.Ocean -> AndroidColor.parseColor("#7DA7C7")
    WatermarkGlassDotsPanelTone.Rose -> AndroidColor.parseColor("#D8A3B2")
}

internal fun drawGlassDotsColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?,
    variantIndex: Int,
    itemCount: Int,
    showHexValue: Boolean
): Bitmap {
    val surface = createWatermarkOverlaySurface(source, fixedOutputSize)
    drawBitmapCenterCrop(surface.canvas, source, surface.photoRect)

    val colors = resolveGlassDotsPaletteColors(
        palette = palette,
        desiredCount = normalizeWatermarkGlassDotsItemCount(itemCount)
    )
    val twoRows = resolveEffectiveWatermarkGlassDotsTwoRows(
        variantIndex = variantIndex,
        itemCount = colors.size,
        showHexValue = showHexValue
    )
    val panelTone = resolveWatermarkGlassDotsPanelTone(variantIndex)
    val normalizedItemCount = colors.size.coerceIn(6, 10)
    val rowItemCounts = if (twoRows) {
        listOf(normalizedItemCount / 2, normalizedItemCount / 2)
    } else {
        listOf(normalizedItemCount)
    }
    val rows = rowItemCounts.size
    val columns = rowItemCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val photoWidth = surface.photoRect.width()
    val photoHeight = surface.photoRect.height()
    val minPhotoSide = min(photoWidth, photoHeight)
    val horizontalInset = photoWidth * 0.050f
    val panelBottomInset = photoHeight * 0.032f
    val panelWidth = (photoWidth - horizontalInset * 2f).coerceAtLeast(1f)

    var dotDiameter = if (twoRows) {
        minPhotoSide * if (showHexValue) 0.073f else 0.070f
    } else {
        minPhotoSide * if (showHexValue) 0.080f else 0.074f
    }.coerceAtLeast(if (showHexValue) 22f else 20f)
    val targetGapRatio = if (twoRows) 0.34f else 0.24f
    val minGapRatio = if (twoRows) 0.20f else 0.14f
    var sidePadding = (panelWidth * if (twoRows) 0.055f else 0.05f)
        .coerceAtLeast(dotDiameter * if (twoRows) 0.24f else 0.20f)
    val widthFactor = columns + (columns - 1) * targetGapRatio
    val maxDotDiameter = ((panelWidth - sidePadding * 2f) / widthFactor).coerceAtLeast(14f)
    dotDiameter = min(dotDiameter, maxDotDiameter)
    sidePadding = sidePadding.coerceAtMost((panelWidth - dotDiameter * columns) * 0.5f)
    var columnGap = if (columns > 1) {
        (panelWidth - sidePadding * 2f - columns * dotDiameter) / (columns - 1)
    } else {
        0f
    }
    val minColumnGap = dotDiameter * minGapRatio
    if (columns > 1 && columnGap < minColumnGap) {
        dotDiameter = ((panelWidth - sidePadding * 2f) / (columns + (columns - 1) * minGapRatio))
            .coerceAtLeast(14f)
        columnGap = ((panelWidth - sidePadding * 2f - columns * dotDiameter) / (columns - 1)).coerceAtLeast(minColumnGap)
    }
    val contentWidth = columns * dotDiameter + (columns - 1) * columnGap

    val rowGap = if (twoRows) {
        if (showHexValue) dotDiameter * 0.34f else dotDiameter * 0.38f
    } else {
        0f
    }
    val labelGap = if (showHexValue) dotDiameter * 0.16f else 0f
    val labelTextSize = if (showHexValue) (dotDiameter * 0.27f).coerceAtLeast(11.5f) else 0f
    val labelRowHeight = if (showHexValue) labelTextSize * 1.58f else 0f
    val rowBlockHeight = dotDiameter + if (showHexValue) (labelGap + labelRowHeight) else 0f
    val contentHeight = rows * rowBlockHeight + (rows - 1) * rowGap
    val panelPaddingTop = max(photoHeight * 0.012f, dotDiameter * 0.18f)
    val panelPaddingBottom = max(photoHeight * 0.013f, dotDiameter * if (showHexValue) 0.22f else 0.18f)
    val panelHeight = max(
        contentHeight + panelPaddingTop + panelPaddingBottom,
        photoHeight * if (showHexValue) 0.238f else if (twoRows) 0.19f else 0.15f
    )
    val panelRect = RectF(
        surface.photoRect.left + horizontalInset,
        surface.photoRect.bottom - panelHeight - panelBottomInset,
        surface.photoRect.right - horizontalInset,
        surface.photoRect.bottom - panelBottomInset
    )
    val cornerRadius = min(panelRect.width(), panelRect.height()) * 0.18f
    val panelClipPath = Path().apply {
        addRoundRect(panelRect, cornerRadius, cornerRadius, Path.Direction.CW)
    }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = applyAlpha(AndroidColor.BLACK, if (panelTone == WatermarkGlassDotsPanelTone.Black) 42 else 22)
        setShadowLayer(
            dotDiameter * 0.26f,
            0f,
            dotDiameter * 0.08f,
            applyAlpha(AndroidColor.BLACK, if (panelTone == WatermarkGlassDotsPanelTone.Black) 62 else 36)
        )
    }
    surface.canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, shadowPaint)

    renderSoftwareGlassPanelBackdrop(
        source = surface.output,
        panelRect = panelRect,
        cornerRadius = cornerRadius,
        panelTone = panelTone
    )?.let { glassBackdrop ->
        surface.canvas.drawBitmap(
            glassBackdrop,
            panelRect.left,
            panelRect.top,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        )
        glassBackdrop.recycle()
    }

    val panelStyle = when (panelTone) {
        WatermarkGlassDotsPanelTone.Clear -> WatermarkGlassDotsPanelStyle(
            applyAlpha(mixColors(AndroidColor.WHITE, palette.anchor.color, 0.985f), 20),
            applyAlpha(mixColors(AndroidColor.WHITE, palette.primary.color, 0.992f), 8),
            applyAlpha(AndroidColor.WHITE, 88),
            52
        )
        WatermarkGlassDotsPanelTone.Pearl -> WatermarkGlassDotsPanelStyle(
            applyAlpha(AndroidColor.parseColor("#FFF8F0"), 48),
            applyAlpha(mixColors(AndroidColor.parseColor("#F1E3D4"), palette.anchor.color, 0.965f), 18),
            applyAlpha(AndroidColor.WHITE, 92),
            54
        )
        WatermarkGlassDotsPanelTone.Smoke -> WatermarkGlassDotsPanelStyle(
            applyAlpha(AndroidColor.parseColor("#DCE3EA"), 36),
            applyAlpha(AndroidColor.parseColor("#B2BBC7"), 22),
            applyAlpha(AndroidColor.WHITE, 88),
            48
        )
        WatermarkGlassDotsPanelTone.Black -> WatermarkGlassDotsPanelStyle(
            applyAlpha(mixColors(AndroidColor.WHITE, palette.anchor.color, 0.95f), 20),
            applyAlpha(AndroidColor.BLACK, 54),
            applyAlpha(AndroidColor.WHITE, 84),
            42
        )
        WatermarkGlassDotsPanelTone.White -> WatermarkGlassDotsPanelStyle(
            applyAlpha(AndroidColor.WHITE, 42),
            applyAlpha(mixColors(AndroidColor.WHITE, palette.anchor.color, 0.985f), 16),
            applyAlpha(AndroidColor.WHITE, 96),
            56
        )
        WatermarkGlassDotsPanelTone.Ocean -> WatermarkGlassDotsPanelStyle(
            applyAlpha(AndroidColor.parseColor("#CDE7F4"), 42),
            applyAlpha(AndroidColor.parseColor("#6F90AA"), 22),
            applyAlpha(AndroidColor.WHITE, 90),
            50
        )
        WatermarkGlassDotsPanelTone.Rose -> WatermarkGlassDotsPanelStyle(
            applyAlpha(AndroidColor.parseColor("#F6DEE7"), 44),
            applyAlpha(AndroidColor.parseColor("#C596A7"), 24),
            applyAlpha(AndroidColor.WHITE, 90),
            50
        )
    }
    val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            panelRect.left,
            panelRect.top,
            panelRect.left,
            panelRect.bottom,
            panelStyle.topColor,
            panelStyle.bottomColor,
            Shader.TileMode.CLAMP
        )
    }
    surface.canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, glassPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = max(min(panelRect.width(), panelRect.height()) * 0.008f, 0.9f)
        color = panelStyle.strokeColor
    }
    surface.canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, strokePaint)

    val highlightRect = RectF(
        panelRect.left + strokePaint.strokeWidth,
        panelRect.top + strokePaint.strokeWidth,
        panelRect.right - strokePaint.strokeWidth,
        panelRect.top + panelRect.height() * 0.46f
    )
    val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            highlightRect.left,
            highlightRect.top,
            highlightRect.left,
            highlightRect.bottom,
            applyAlpha(AndroidColor.WHITE, panelStyle.highlightAlpha),
            applyAlpha(AndroidColor.WHITE, 0),
            Shader.TileMode.CLAMP
        )
    }
    val topEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            panelRect.left,
            panelRect.top,
            panelRect.left,
            panelRect.top + panelRect.height() * 0.18f,
            applyAlpha(AndroidColor.WHITE, if (panelTone == WatermarkGlassDotsPanelTone.Black) 62 else 86),
            applyAlpha(AndroidColor.WHITE, 0),
            Shader.TileMode.CLAMP
        )
    }
    surface.canvas.save()
    surface.canvas.clipPath(panelClipPath)
    surface.canvas.drawRoundRect(
        highlightRect,
        cornerRadius * 0.92f,
        cornerRadius * 0.92f,
        highlightPaint
    )
    surface.canvas.drawRect(panelRect, topEdgePaint)
    surface.canvas.restore()

    val startY = panelRect.centerY() - contentHeight * 0.5f
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (panelTone == WatermarkGlassDotsPanelTone.White) {
            applyAlpha(AndroidColor.BLACK, 178)
        } else {
            applyAlpha(AndroidColor.WHITE, 224)
        }
        textAlign = Paint.Align.CENTER
        textSize = labelTextSize
        letterSpacing = 0.02f
        setShadowLayer(
            dotDiameter * 0.06f,
            0f,
            dotDiameter * 0.03f,
            applyAlpha(AndroidColor.BLACK, 42)
        )
    }
    colors.forEachIndexed { index, color ->
        val row = index / columns
        val column = index % columns
        val rowColumns = rowItemCounts[row].coerceAtLeast(1)
        val rowContentWidth = rowColumns * dotDiameter + (rowColumns - 1) * columnGap
        val rowStartX = panelRect.left + ((panelWidth - rowContentWidth) * 0.5f)
        val cellLeft = rowStartX + column * (dotDiameter + columnGap)
        val rowTop = startY + row * (rowBlockHeight + rowGap)
        val cellTop = rowTop
        val rect = RectF(
            cellLeft,
            cellTop,
            cellLeft + dotDiameter,
            cellTop + dotDiameter
        )
        surface.canvas.drawOval(
            rect,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = applyAlpha(AndroidColor.BLACK, 58)
                setShadowLayer(
                    dotDiameter * 0.16f,
                    0f,
                    dotDiameter * 0.08f,
                    applyAlpha(AndroidColor.BLACK, 76)
                )
            }
        )
        surface.canvas.drawOval(
            rect,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
            }
        )
        surface.canvas.drawOval(
            rect,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(dotDiameter * 0.045f, 1f)
                this.color = if (relativeLuminance(color) > 0.90f) {
                    applyAlpha(AndroidColor.BLACK, 42)
                } else {
                    applyAlpha(AndroidColor.WHITE, 68)
                }
            }
        )
        if (showHexValue) {
            val baseline = rowTop + dotDiameter + labelGap - labelPaint.fontMetrics.ascent
            surface.canvas.drawText(
                "#%02x%02x%02x".format(
                    (color shr 16) and 0xFF,
                    (color shr 8) and 0xFF,
                    color and 0xFF
                ),
                rect.centerX(),
                baseline,
                labelPaint
            )
        }
    }

    return surface.output
}

private fun renderSoftwareGlassPanelBackdrop(
    source: Bitmap,
    panelRect: RectF,
    cornerRadius: Float,
    panelTone: WatermarkGlassDotsPanelTone
): Bitmap? {
    if (source.isRecycled || source.width <= 1 || source.height <= 1) return null
    val panelLeft = floor(panelRect.left).toInt().coerceIn(0, source.width - 1)
    val panelTop = floor(panelRect.top).toInt().coerceIn(0, source.height - 1)
    val panelRight = ceil(panelRect.right).toInt().coerceIn(panelLeft + 1, source.width)
    val panelBottom = ceil(panelRect.bottom).toInt().coerceIn(panelTop + 1, source.height)
    val panelWidth = panelRight - panelLeft
    val panelHeight = panelBottom - panelTop
    val minPanelSide = min(panelWidth, panelHeight).toFloat()
    val blurRadius = (minPanelSide * 0.05f).roundToInt().coerceIn(6, 24)
    val refractionInset = (minPanelSide * 0.08f).roundToInt().coerceIn(6, 28)
    val samplePad = blurRadius * 3 + refractionInset
    val sampleLeft = (panelLeft - samplePad).coerceAtLeast(0)
    val sampleTop = (panelTop - samplePad).coerceAtLeast(0)
    val sampleRight = (panelRight + samplePad).coerceAtMost(source.width)
    val sampleBottom = (panelBottom + samplePad).coerceAtMost(source.height)
    val sampleWidth = sampleRight - sampleLeft
    val sampleHeight = sampleBottom - sampleTop
    if (sampleWidth <= 1 || sampleHeight <= 1) return null

    val samplePixels = IntArray(sampleWidth * sampleHeight)
    source.getPixels(samplePixels, 0, sampleWidth, sampleLeft, sampleTop, sampleWidth, sampleHeight)
    val blurredPixels = boxBlurPixels(samplePixels, sampleWidth, sampleHeight, blurRadius, passes = 3)
    val outputPixels = IntArray(panelWidth * panelHeight)
    val radius = cornerRadius.coerceIn(0f, minPanelSide * 0.5f)
    val edgeBand = (radius * 0.52f).coerceAtLeast(minPanelSide * 0.10f)
    val refractionStrength = minPanelSide * 0.045f
    val isDarkGlass = panelTone == WatermarkGlassDotsPanelTone.Black

    for (y in 0 until panelHeight) {
        for (x in 0 until panelWidth) {
            val lx = x + 0.5f
            val ly = y + 0.5f
            val coverage = roundedRectCoverage(lx, ly, panelWidth.toFloat(), panelHeight.toFloat(), radius)
            if (coverage <= 0f) continue

            val signedDistance = roundedRectSignedDistance(
                x = lx,
                y = ly,
                width = panelWidth.toFloat(),
                height = panelHeight.toFloat(),
                radius = radius
            )
            val edgeWeight = (1f - ((-signedDistance).coerceAtLeast(0f) / edgeBand)).coerceIn(0f, 1f)
            val normal = roundedRectNormal(lx, ly, panelWidth.toFloat(), panelHeight.toFloat(), radius)
            val centerX = (lx - panelWidth * 0.5f) / (panelWidth * 0.5f).coerceAtLeast(1f)
            val centerY = (ly - panelHeight * 0.5f) / (panelHeight * 0.5f).coerceAtLeast(1f)
            val radial = sqrt(centerX * centerX + centerY * centerY).coerceIn(0f, 1.35f)
            val lensWeight = (radial * radial * 0.16f).coerceIn(0f, 0.20f)
            val displacement = refractionStrength * (edgeWeight * edgeWeight * 0.72f + lensWeight)
            val sourceX = panelLeft + x + normal.first * displacement + centerX * refractionStrength * 0.08f
            val sourceY = panelTop + y + normal.second * displacement + centerY * refractionStrength * 0.05f
            val sampleX = sourceX - sampleLeft
            val sampleY = sourceY - sampleTop
            val base = sampleBilinear(blurredPixels, sampleWidth, sampleHeight, sampleX, sampleY)
            var red = AndroidColor.red(base)
            var green = AndroidColor.green(base)
            var blue = AndroidColor.blue(base)
            val edgeLift = (edgeWeight * if (isDarkGlass) 10f else 16f).roundToInt()
            val centerLift = ((1f - radial.coerceIn(0f, 1f)) * if (isDarkGlass) 2f else 4f).roundToInt()
            val edgeShade = (edgeWeight * edgeWeight * if (isDarkGlass) 12f else 6f).roundToInt()
            red = (red + edgeLift + centerLift - edgeShade).coerceIn(0, 255)
            green = (green + edgeLift + centerLift - edgeShade).coerceIn(0, 255)
            blue = (blue + edgeLift + centerLift - edgeShade).coerceIn(0, 255)
            outputPixels[y * panelWidth + x] = AndroidColor.argb(
                (coverage * 255f).roundToInt().coerceIn(0, 255),
                red,
                green,
                blue
            )
        }
    }

    return Bitmap.createBitmap(panelWidth, panelHeight, Bitmap.Config.ARGB_8888).apply {
        setPixels(outputPixels, 0, panelWidth, 0, 0, panelWidth, panelHeight)
    }
}

private fun boxBlurPixels(
    source: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    passes: Int
): IntArray {
    if (radius <= 0 || width <= 1 || height <= 1) return source.copyOf()
    var current = source.copyOf()
    val temp = IntArray(width * height)
    repeat(passes.coerceAtLeast(1)) {
        boxBlurHorizontal(current, temp, width, height, radius)
        boxBlurVertical(temp, current, width, height, radius)
    }
    return current
}

private fun boxBlurHorizontal(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int) {
    val divisor = radius * 2 + 1
    for (y in 0 until height) {
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        val row = y * width
        for (i in -radius..radius) {
            val color = source[row + i.coerceIn(0, width - 1)]
            sumA += AndroidColor.alpha(color)
            sumR += AndroidColor.red(color)
            sumG += AndroidColor.green(color)
            sumB += AndroidColor.blue(color)
        }
        for (x in 0 until width) {
            target[row + x] = AndroidColor.argb(sumA / divisor, sumR / divisor, sumG / divisor, sumB / divisor)
            val removeColor = source[row + (x - radius).coerceIn(0, width - 1)]
            val addColor = source[row + (x + radius + 1).coerceIn(0, width - 1)]
            sumA += AndroidColor.alpha(addColor) - AndroidColor.alpha(removeColor)
            sumR += AndroidColor.red(addColor) - AndroidColor.red(removeColor)
            sumG += AndroidColor.green(addColor) - AndroidColor.green(removeColor)
            sumB += AndroidColor.blue(addColor) - AndroidColor.blue(removeColor)
        }
    }
}

private fun boxBlurVertical(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int) {
    val divisor = radius * 2 + 1
    for (x in 0 until width) {
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (i in -radius..radius) {
            val color = source[i.coerceIn(0, height - 1) * width + x]
            sumA += AndroidColor.alpha(color)
            sumR += AndroidColor.red(color)
            sumG += AndroidColor.green(color)
            sumB += AndroidColor.blue(color)
        }
        for (y in 0 until height) {
            target[y * width + x] = AndroidColor.argb(sumA / divisor, sumR / divisor, sumG / divisor, sumB / divisor)
            val removeColor = source[(y - radius).coerceIn(0, height - 1) * width + x]
            val addColor = source[(y + radius + 1).coerceIn(0, height - 1) * width + x]
            sumA += AndroidColor.alpha(addColor) - AndroidColor.alpha(removeColor)
            sumR += AndroidColor.red(addColor) - AndroidColor.red(removeColor)
            sumG += AndroidColor.green(addColor) - AndroidColor.green(removeColor)
            sumB += AndroidColor.blue(addColor) - AndroidColor.blue(removeColor)
        }
    }
}

private fun sampleBilinear(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
    val clampedX = x.coerceIn(0f, (width - 1).toFloat())
    val clampedY = y.coerceIn(0f, (height - 1).toFloat())
    val x0 = floor(clampedX).toInt()
    val y0 = floor(clampedY).toInt()
    val x1 = (x0 + 1).coerceAtMost(width - 1)
    val y1 = (y0 + 1).coerceAtMost(height - 1)
    val tx = clampedX - x0
    val ty = clampedY - y0
    val c00 = pixels[y0 * width + x0]
    val c10 = pixels[y0 * width + x1]
    val c01 = pixels[y1 * width + x0]
    val c11 = pixels[y1 * width + x1]
    fun channel(selector: (Int) -> Int): Int {
        val top = selector(c00) * (1f - tx) + selector(c10) * tx
        val bottom = selector(c01) * (1f - tx) + selector(c11) * tx
        return (top * (1f - ty) + bottom * ty).roundToInt().coerceIn(0, 255)
    }
    return AndroidColor.argb(
        channel(AndroidColor::alpha),
        channel(AndroidColor::red),
        channel(AndroidColor::green),
        channel(AndroidColor::blue)
    )
}

private fun roundedRectCoverage(x: Float, y: Float, width: Float, height: Float, radius: Float): Float {
    val distance = roundedRectSignedDistance(x, y, width, height, radius)
    return (-distance + 1.25f).coerceIn(0f, 2.5f) / 2.5f
}

private fun roundedRectNormal(x: Float, y: Float, width: Float, height: Float, radius: Float): Pair<Float, Float> {
    val dx = roundedRectSignedDistance(x + 1f, y, width, height, radius) -
        roundedRectSignedDistance(x - 1f, y, width, height, radius)
    val dy = roundedRectSignedDistance(x, y + 1f, width, height, radius) -
        roundedRectSignedDistance(x, y - 1f, width, height, radius)
    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
    return dx / length to dy / length
}

private fun roundedRectSignedDistance(x: Float, y: Float, width: Float, height: Float, radius: Float): Float {
    val halfWidth = width * 0.5f
    val halfHeight = height * 0.5f
    val qx = abs(x - halfWidth) - (halfWidth - radius)
    val qy = abs(y - halfHeight) - (halfHeight - radius)
    val outsideX = max(qx, 0f)
    val outsideY = max(qy, 0f)
    val outside = sqrt(outsideX * outsideX + outsideY * outsideY)
    val inside = min(max(qx, qy), 0f)
    return outside + inside - radius
}

private fun resolveGlassDotsPaletteColors(
    palette: WatermarkExtractedPalette,
    desiredCount: Int
): List<Int> {
    val candidates = mutableListOf<Int>()
    candidates += resolveWatermarkPaletteSeriesColors(
        palette = palette,
        desiredCount = desiredCount * 3,
        mode = WatermarkPaletteSeriesMode.Natural
    )
    listOf(
        palette.primary.color,
        palette.accent.color,
        palette.contrast.color,
        mixColors(palette.primary.color, palette.accent.color, 0.5f),
        mixColors(palette.anchor.color, palette.accent.color, 0.45f),
        retuneColor(palette.primary.color, saturationMultiplier = 1.05f, targetValue = 0.70f),
        retuneColor(palette.accent.color, saturationMultiplier = 0.96f, targetValue = 0.76f)
    ).forEach { candidates += it }

    val result = mutableListOf<Int>()
    var darkCount = 0
    candidates.forEach { color ->
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color, hsv)
        val isDeepBlack = relativeLuminance(color) < 0.07f || (hsv[2] < 0.18f && hsv[1] < 0.24f)
        if (isDeepBlack && darkCount >= 1) return@forEach
        if (result.none { glassDotsColorDistance(it, color) < 28f }) {
            result += if (isDeepBlack) {
                darkCount += 1
                retuneColor(color, saturationMultiplier = 1.12f, targetValue = 0.24f)
            } else {
                color
            }
        }
    }
    while (result.size < desiredCount) {
        val fallback = mixColors(
            palette.primary.color,
            palette.accent.color,
            ((result.size % desiredCount).toFloat() / desiredCount.toFloat()).coerceIn(0.18f, 0.82f)
        )
        if (result.none { glassDotsColorDistance(it, fallback) < 24f }) {
            result += fallback
        } else {
            result += retuneColor(fallback, saturationMultiplier = 1.08f, targetValue = 0.62f)
        }
    }
    return result.take(desiredCount)
}

private fun glassDotsColorDistance(first: Int, second: Int): Float {
    val dr = AndroidColor.red(first) - AndroidColor.red(second)
    val dg = AndroidColor.green(first) - AndroidColor.green(second)
    val db = AndroidColor.blue(first) - AndroidColor.blue(second)
    return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
}
