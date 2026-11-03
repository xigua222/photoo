package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val WATERMARK_COLOR_VARIANT_GAPPED = 1
private const val WATERMARK_COLOR_VARIANT_BORDERLESS = 1
private const val WATERMARK_CENTER_BLOCK_VARIANT_RECTANGLE = 1
private const val WATERMARK_CENTER_BLOCK_VARIANT_HEART = 2
private const val WATERMARK_CENTER_BLOCK_VARIANT_CIRCLE = 3
private const val WATERMARK_CENTER_BLOCK_VARIANT_DIAMOND = 4

internal enum class WatermarkPaletteSeriesMode {
    Natural,
    Hue,
    LuminanceDescending
}

internal data class WatermarkOverlaySurface(
    val output: Bitmap,
    val canvas: Canvas,
    val photoRect: RectF
)

internal data class WatermarkPaletteBlockDimensions(
    val width: Float,
    val height: Float
)

// Keep tall/square shapes readable by scaling them down proportionally instead of stretching them.
internal fun resolveWatermarkPaletteBlockDimensions(
    shape: WatermarkPaletteBlockShape,
    preferredWidth: Float,
    maxHeight: Float
): WatermarkPaletteBlockDimensions {
    val safeWidth = preferredWidth.coerceAtLeast(1f)
    val safeMaxHeight = maxHeight.coerceAtLeast(1f)
    val heightRatio = when (shape) {
        WatermarkPaletteBlockShape.Rectangle -> 1.55f
        WatermarkPaletteBlockShape.Heart -> 1.0f
        else -> 1f
    }
    val preferredHeight = safeWidth * heightRatio
    val scale = min(1f, safeMaxHeight / preferredHeight)
    return WatermarkPaletteBlockDimensions(
        width = safeWidth * scale,
        height = preferredHeight * scale
    )
}

internal fun drawMosaicGridColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?
): Bitmap {
    val surface = createWatermarkOverlaySurface(source, fixedOutputSize)
    drawBitmapCenterCrop(surface.canvas, source, surface.photoRect)

    val colors = resolveWatermarkPaletteSeriesColors(
        palette = palette,
        desiredCount = 36,
        mode = WatermarkPaletteSeriesMode.Natural
    )
    val columns = 12
    val rows = 3
    val width = surface.photoRect.width() * 0.56f
    val gap = 0f
    val cellWidth = (width / columns).coerceAtLeast(6f)
    val cellHeight = cellWidth
    val height = cellHeight * rows
    val left = surface.photoRect.centerX() - width * 0.5f
    val top = min(
        surface.photoRect.top + surface.photoRect.height() * 0.73f,
        surface.photoRect.bottom - height - surface.photoRect.height() * 0.08f
    )

    repeat(rows) { row ->
        repeat(columns) { column ->
            val index = row * columns + column
            val rect = RectF(
                left + column * (cellWidth + gap),
                top + row * (cellHeight + gap),
                left + column * (cellWidth + gap) + cellWidth,
                top + row * (cellHeight + gap) + cellHeight
            )
            surface.canvas.drawRect(
                rect,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = applyAlpha(colors[index % colors.size], 242) }
            )
        }
    }
    return surface.output
}

internal fun drawCircleBandsColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?,
    variantIndex: Int,
    horizontalOffsetRatio: Float,
    verticalOffsetRatio: Float,
    itemCount: Int
): Bitmap {
    val surface = createWatermarkOverlaySurface(source, fixedOutputSize)
    drawBitmapCenterCrop(surface.canvas, source, surface.photoRect)

    val colorCount = resolveWatermarkRequestedOverlayCount(
        requestedCount = itemCount,
        minimumCount = 6
    )
    val colors = resolveWatermarkPaletteSeriesColors(
        palette = palette,
        desiredCount = colorCount,
        mode = WatermarkPaletteSeriesMode.LuminanceDescending
    )
    val circleSize = min(surface.photoRect.width(), surface.photoRect.height()) * 0.37f
    val circleMargin = min(surface.photoRect.width(), surface.photoRect.height()) * 0.03f
    val minCenterX = surface.photoRect.left + circleSize * 0.5f + circleMargin
    val maxCenterX = surface.photoRect.right - circleSize * 0.5f - circleMargin
    val minCenterY = surface.photoRect.top + circleSize * 0.5f + circleMargin
    val maxCenterY = surface.photoRect.bottom - circleSize * 0.5f - circleMargin
    val defaultCenterX = surface.photoRect.centerX()
    val defaultCenterY = surface.photoRect.top + surface.photoRect.height() * 0.30f
    val centerX = (
        defaultCenterX +
            horizontalOffsetRatio.coerceIn(-1f, 1f) *
            ((maxCenterX - minCenterX) * 0.5f).coerceAtLeast(0f)
        ).coerceIn(minCenterX, maxCenterX)
    val centerY = (
        defaultCenterY +
            verticalOffsetRatio.coerceIn(-1f, 1f) *
            ((maxCenterY - minCenterY) * 0.5f).coerceAtLeast(0f)
        ).coerceIn(minCenterY, maxCenterY)
    val circleRect = RectF(
        centerX - circleSize * 0.5f,
        centerY - circleSize * 0.5f,
        centerX + circleSize * 0.5f,
        centerY + circleSize * 0.5f
    )
    val isGapped = variantIndex.mod(2) == WATERMARK_COLOR_VARIANT_GAPPED
    val gap = if (isGapped) circleRect.height() * 0.036f else 0f
    val bandHeight = (
        (circleRect.height() - gap * (colors.size - 1)) / colors.size
        ).coerceAtLeast(circleRect.height() * 0.045f)
    val clipPath = Path().apply {
        addOval(circleRect, Path.Direction.CW)
    }

    surface.canvas.save()
    surface.canvas.clipPath(clipPath)
    colors.forEachIndexed { index, color ->
        val top = circleRect.top + index * (bandHeight + gap)
        val rect = RectF(circleRect.left, top, circleRect.right, top + bandHeight)
        surface.canvas.drawRect(
            rect,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = applyAlpha(color, 224) }
        )
    }
    surface.canvas.restore()

    return surface.output
}

internal fun drawBottomGradientBarColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?
): Bitmap {
    val width = fixedOutputSize?.first?.coerceAtLeast(1) ?: source.width.coerceAtLeast(1)
    val photoHeight = fixedOutputSize?.second?.coerceAtLeast(1) ?: source.height.coerceAtLeast(1)
    val barHeight = max(photoHeight * 0.145f, width * 0.065f).coerceAtMost(photoHeight * 0.22f)
    val outputHeight = if (fixedOutputSize != null) {
        photoHeight
    } else {
        (photoHeight + barHeight).roundToInt().coerceAtLeast(1)
    }
    val photoRect = if (fixedOutputSize != null) {
        RectF(0f, 0f, width.toFloat(), (outputHeight.toFloat() - barHeight).coerceAtLeast(1f))
    } else {
        RectF(0f, 0f, width.toFloat(), photoHeight.toFloat())
    }
    val barRect = RectF(
        0f,
        photoRect.bottom,
        width.toFloat(),
        outputHeight.toFloat()
    )
    val surface = createWatermarkOverlaySurface(
        outputWidth = width,
        outputHeight = outputHeight,
        photoRect = photoRect
    )
    val extensionBackground = retuneColor(
        palette.anchor.color,
        saturationMultiplier = 0.16f,
        targetValue = 0.97f
    )
    surface.canvas.drawColor(extensionBackground)
    drawBitmapCenterCrop(surface.canvas, source, surface.photoRect)

    val colors = resolveWatermarkPaletteSeriesColors(
        palette = palette,
        desiredCount = 10,
        mode = WatermarkPaletteSeriesMode.Hue
    )
    val shader = LinearGradient(
        barRect.left,
        barRect.centerY(),
        barRect.right,
        barRect.centerY(),
        colors.map { applyAlpha(it, 238) }.toIntArray(),
        FloatArray(colors.size) { index ->
            if (colors.size <= 1) 0f else index.toFloat() / (colors.size - 1).toFloat()
        },
        Shader.TileMode.CLAMP
    )
    surface.canvas.drawRect(
        barRect,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
    )
    surface.canvas.drawRect(
        barRect.left,
        barRect.top,
        barRect.right,
        barRect.top + max(barHeight * 0.04f, 1f),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = applyAlpha(AndroidColor.WHITE, 52) }
    )
    return surface.output
}

internal fun drawCenterStripesColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?,
    variantIndex: Int,
    itemCount: Int
): Bitmap {
    val surface = createWatermarkOverlaySurface(source, fixedOutputSize)
    drawBitmapCenterCrop(surface.canvas, source, surface.photoRect)

    val colorCount = resolveWatermarkRequestedOverlayCount(
        requestedCount = itemCount,
        minimumCount = 7
    )
    val colors = resolveWatermarkPaletteSeriesColors(
        palette = palette,
        desiredCount = colorCount,
        mode = WatermarkPaletteSeriesMode.LuminanceDescending
    )
    val stripeWidth = surface.photoRect.width() * 0.42f
    val isGapped = variantIndex.mod(2) == WATERMARK_COLOR_VARIANT_GAPPED
    val gap = if (isGapped) max(surface.photoRect.height() * 0.018f, 6f) else 0f
    val stripeHeight = (
        (surface.photoRect.height() * 0.54f - gap * (colors.size - 1)) / colors.size
        ).coerceAtLeast(max(surface.photoRect.height() * 0.018f, 8f))
    val totalHeight = stripeHeight * colors.size + gap * (colors.size - 1)
    val left = surface.photoRect.centerX() - stripeWidth * 0.5f
    val top = surface.photoRect.centerY() - totalHeight * 0.5f

    colors.forEachIndexed { index, color ->
        val rect = RectF(
            left,
            top + index * (stripeHeight + gap),
            left + stripeWidth,
            top + index * (stripeHeight + gap) + stripeHeight
        )
        drawWatermarkPaletteBlock(
            canvas = surface.canvas,
            rect = rect,
            color = applyAlpha(color, 214),
            shape = if (isGapped) WatermarkPaletteBlockShape.Default else WatermarkPaletteBlockShape.StraightRectangle
        )
    }
    return surface.output
}

internal fun drawCenterBlocksColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?,
    variantIndex: Int,
    itemCount: Int
): Bitmap {
    val surface = createWatermarkOverlaySurface(source, fixedOutputSize)
    drawBitmapCenterCrop(surface.canvas, source, surface.photoRect)

    val shape = resolveWatermarkPaletteBlockShape(variantIndex)
    val minimumCount = if (shape == WatermarkPaletteBlockShape.Heart) 6 else 7
    val colorCount = resolveWatermarkRequestedOverlayCount(
        requestedCount = itemCount,
        minimumCount = minimumCount
    )
    val colors = resolveWatermarkPaletteSeriesColors(
        palette = palette,
        desiredCount = colorCount,
        mode = WatermarkPaletteSeriesMode.Natural
    )
    val totalWidth = if (shape == WatermarkPaletteBlockShape.Heart) {
        surface.photoRect.width() * 0.54f
    } else {
        surface.photoRect.width() * 0.58f
    }
    val gap = if (shape == WatermarkPaletteBlockShape.Heart) {
        max(surface.photoRect.width() * 0.012f, 4f)
    } else {
        max(surface.photoRect.width() * 0.008f, 3f)
    }
    val blockWidth = ((totalWidth - gap * (colors.size - 1)) / colors.size).coerceAtLeast(8f)
    val maxBlockHeight = when (shape) {
        WatermarkPaletteBlockShape.Rectangle -> surface.photoRect.height() * 0.24f
        WatermarkPaletteBlockShape.Heart -> surface.photoRect.height() * 0.20f
        else -> surface.photoRect.height() * 0.16f
    }
    val blockSize = resolveWatermarkPaletteBlockDimensions(
        shape = shape,
        preferredWidth = blockWidth,
        maxHeight = maxBlockHeight
    )
    val left = surface.photoRect.centerX() - totalWidth * 0.5f
    val top = surface.photoRect.centerY() - blockSize.height * 0.5f

    colors.forEachIndexed { index, color ->
        val slotLeft = left + index * (blockWidth + gap)
        val blockLeft = slotLeft + (blockWidth - blockSize.width) * 0.5f
        val rect = RectF(
            blockLeft,
            top,
            blockLeft + blockSize.width,
            top + blockSize.height
        )
        drawWatermarkPaletteBlock(
            canvas = surface.canvas,
            rect = rect,
            color = applyAlpha(color, 240),
            shape = shape
        )
    }
    return surface.output
}

internal fun createWatermarkOverlaySurface(
    source: Bitmap,
    fixedOutputSize: Pair<Int, Int>?
): WatermarkOverlaySurface {
    val width = fixedOutputSize?.first?.coerceAtLeast(1) ?: source.width.coerceAtLeast(1)
    val height = fixedOutputSize?.second?.coerceAtLeast(1) ?: source.height.coerceAtLeast(1)
    return createWatermarkOverlaySurface(
        outputWidth = width,
        outputHeight = height,
        photoRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    )
}

internal fun createWatermarkOverlaySurface(
    outputWidth: Int,
    outputHeight: Int,
    photoRect: RectF
): WatermarkOverlaySurface {
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    return WatermarkOverlaySurface(
        output = output,
        canvas = Canvas(output),
        photoRect = photoRect
    )
}

internal fun resolveWatermarkPaletteSeriesColors(
    palette: WatermarkExtractedPalette,
    desiredCount: Int,
    mode: WatermarkPaletteSeriesMode
): List<Int> {
    val baseSwatches = (if (palette.expanded.isNotEmpty()) palette.expanded else palette.ordered)
        .ifEmpty { listOf(palette.anchor, palette.primary, palette.accent, palette.deep, palette.contrast) }
    val orderedSwatches = when (mode) {
        WatermarkPaletteSeriesMode.Natural -> baseSwatches
        WatermarkPaletteSeriesMode.Hue -> baseSwatches.sortedBy { it.hue }
        WatermarkPaletteSeriesMode.LuminanceDescending -> baseSwatches.sortedByDescending {
            relativeLuminance(it.color)
        }
    }
    val colors = mutableListOf<Int>()
    orderedSwatches.forEach { colors += it.color }
    orderedSwatches.forEach { swatch ->
        appendDistinctColor(colors, retuneColor(swatch.color, saturationMultiplier = 0.90f, targetValue = min(0.94f, max(0.38f, swatch.value * 1.08f))))
        appendDistinctColor(colors, retuneColor(swatch.color, saturationMultiplier = 1.10f, targetValue = min(0.88f, max(0.34f, swatch.value * 0.92f + 0.04f))))
    }
    orderedSwatches.zipWithNext().forEach { (first, second) ->
        appendDistinctColor(colors, mixColors(first.color, second.color, 0.5f))
    }

    if (colors.isEmpty()) {
        colors += AndroidColor.WHITE
    }
    val seedColors = colors.toList()
    while (colors.size < desiredCount) {
        colors += seedColors[(colors.size - seedColors.size).mod(seedColors.size.coerceAtLeast(1))]
    }
    return colors.take(desiredCount)
}

private fun appendDistinctColor(target: MutableList<Int>, color: Int) {
    if (target.none { paletteColorDistance(it, color) < 24f }) {
        target += color
    }
}

private fun paletteColorDistance(first: Int, second: Int): Float {
    val dr = AndroidColor.red(first) - AndroidColor.red(second)
    val dg = AndroidColor.green(first) - AndroidColor.green(second)
    val db = AndroidColor.blue(first) - AndroidColor.blue(second)
    return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
}

internal fun drawWatermarkPaletteBlock(
    canvas: Canvas,
    rect: RectF,
    color: Int,
    shape: WatermarkPaletteBlockShape = WatermarkPaletteBlockShape.Default
) {
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = max(min(rect.width(), rect.height()) * 0.05f, 1f)
        this.color = applyAlpha(AndroidColor.BLACK, 32)
    }
    when (shape) {
        WatermarkPaletteBlockShape.Default,
        WatermarkPaletteBlockShape.Rectangle -> {
            val radius = min(rect.width(), rect.height()) * if (shape == WatermarkPaletteBlockShape.Rectangle) 0.03f else 0.08f
            canvas.drawRoundRect(rect, radius, radius, fillPaint)
            if (relativeLuminance(color) > 0.92f) {
                canvas.drawRoundRect(rect, radius, radius, outlinePaint)
            }
        }
        WatermarkPaletteBlockShape.StraightRectangle -> {
            canvas.drawRect(rect, fillPaint)
            if (relativeLuminance(color) > 0.92f) {
                canvas.drawRect(rect, outlinePaint)
            }
        }
        WatermarkPaletteBlockShape.Heart -> {
            val heartPath = buildWatermarkHeartPath(rect)
            canvas.drawPath(heartPath, fillPaint)
            if (relativeLuminance(color) > 0.92f) {
                canvas.drawPath(heartPath, outlinePaint)
            }
        }
        WatermarkPaletteBlockShape.Circle -> {
            canvas.drawOval(rect, fillPaint)
            if (relativeLuminance(color) > 0.92f) {
                canvas.drawOval(rect, outlinePaint)
            }
        }
        WatermarkPaletteBlockShape.Diamond -> {
            val diamondPath = Path().apply {
                moveTo(rect.centerX(), rect.top)
                lineTo(rect.right, rect.centerY())
                lineTo(rect.centerX(), rect.bottom)
                lineTo(rect.left, rect.centerY())
                close()
            }
            canvas.drawPath(diamondPath, fillPaint)
            if (relativeLuminance(color) > 0.92f) {
                canvas.drawPath(diamondPath, outlinePaint)
            }
        }
    }
}

internal enum class WatermarkPaletteBlockShape {
    Default,
    StraightRectangle,
    Rectangle,
    Heart,
    Circle,
    Diamond
}

internal fun resolveWatermarkPaletteBlockShape(
    variantIndex: Int
): WatermarkPaletteBlockShape = when (variantIndex) {
    WATERMARK_CENTER_BLOCK_VARIANT_RECTANGLE -> WatermarkPaletteBlockShape.Rectangle
    WATERMARK_CENTER_BLOCK_VARIANT_HEART -> WatermarkPaletteBlockShape.Heart
    WATERMARK_CENTER_BLOCK_VARIANT_CIRCLE -> WatermarkPaletteBlockShape.Circle
    WATERMARK_CENTER_BLOCK_VARIANT_DIAMOND -> WatermarkPaletteBlockShape.Diamond
    else -> WatermarkPaletteBlockShape.Default
}

private fun buildWatermarkHeartPath(rect: RectF): Path {
    val minX = 4f
    val minY = 8f
    val pathWidth = 40f
    val pathHeight = 34.3262f
    fun scaleX(value: Float): Float = rect.left + rect.width() * ((value - minX) / pathWidth)
    fun scaleY(value: Float): Float = rect.top + rect.height() * ((value - minY) / pathHeight)
    return Path().apply {
        moveTo(scaleX(15f), scaleY(8f))
        cubicTo(
            scaleX(8.92487f),
            scaleY(8f),
            scaleX(4f),
            scaleY(12.9249f),
            scaleX(4f),
            scaleY(19f)
        )
        cubicTo(
            scaleX(4f),
            scaleY(30f),
            scaleX(17f),
            scaleY(40f),
            scaleX(24f),
            scaleY(42.3262f)
        )
        cubicTo(
            scaleX(31f),
            scaleY(40f),
            scaleX(44f),
            scaleY(30f),
            scaleX(44f),
            scaleY(19f)
        )
        cubicTo(
            scaleX(44f),
            scaleY(12.9249f),
            scaleX(39.0751f),
            scaleY(8f),
            scaleX(33f),
            scaleY(8f)
        )
        cubicTo(
            scaleX(29.2797f),
            scaleY(8f),
            scaleX(25.9907f),
            scaleY(9.8469f),
            scaleX(24f),
            scaleY(12.6738f)
        )
        cubicTo(
            scaleX(22.0093f),
            scaleY(9.8469f),
            scaleX(18.7203f),
            scaleY(8f),
            scaleX(15f),
            scaleY(8f)
        )
        close()
    }
}

private fun resolveWatermarkRequestedOverlayCount(
    requestedCount: Int,
    minimumCount: Int
): Int = requestedCount.coerceIn(minimumCount, 10)
