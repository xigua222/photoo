package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.roundToInt

internal const val DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR = 1.16f
internal const val MIN_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR = 0.86f
internal const val MAX_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR = 1.68f

internal enum class WatermarkCutoutFrameAspectRatio(
    val label: String,
    val aspectRatio: Float
) {
    Square("1:1", 1f),
    FourThree("4:3", 4f / 3f),
    ThreeFour("3:4", 3f / 4f),
    FourFive("4:5", 4f / 5f),
    SixteenNine("16:9", 16f / 9f),
    NineSixteen("9:16", 9f / 16f)
}

internal val DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO = WatermarkCutoutFrameAspectRatio.FourThree

internal fun watermarkCutoutBreakoutPanelTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "冲破边框"
    AppLanguage.TraditionalChinese -> "衝破邊框"
    else -> "Breakout"
}

internal fun watermarkCutoutBreakoutBackgroundColorLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "背景颜色"
    AppLanguage.TraditionalChinese -> "背景顏色"
    else -> "Background Color"
}

internal fun watermarkCutoutBreakoutSubjectScaleLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "冲出力度"
    AppLanguage.TraditionalChinese -> "衝出力度"
    else -> "Breakout Lift"
}

internal fun watermarkCutoutBreakoutFrameRatioLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "外框比例"
    AppLanguage.TraditionalChinese -> "外框比例"
    else -> "Frame Ratio"
}

internal fun drawWatermarkCutoutBreakoutTemplate(
    source: Bitmap,
    signatureText: String,
    signatureTypeface: Typeface?,
    signatureColor: Int?,
    signatureHorizontalOffsetRatio: Float,
    signatureVerticalOffsetRatio: Float,
    signatureScaleFactor: Float,
    backgroundColor: Int,
    imageScaleFactor: Float,
    foreground: Bitmap?,
    style: WatermarkCutoutOverlayStyle,
    subjectScaleFactor: Float,
    frameAspectRatio: WatermarkCutoutFrameAspectRatio = DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO,
    fixedOutputSize: Pair<Int, Int>? = null
): Bitmap {
    val outerAspectRatio = frameAspectRatio.aspectRatio.coerceIn(0.35f, 2.4f)
    val outputWidth: Int
    val outputHeight: Int
    if (fixedOutputSize != null) {
        outputWidth = fixedOutputSize.first.coerceAtLeast(1)
        outputHeight = fixedOutputSize.second.coerceAtLeast(1)
    } else {
        val renderSourceSize = resolveSignatureRenderSourceSize(
            source = source,
            signatureText = signatureText,
            fixedOutputSize = fixedOutputSize
        )
        val baseMaxSide = max(
            renderSourceSize?.first ?: source.width,
            renderSourceSize?.second ?: source.height
        ).coerceAtLeast(1)
        if (outerAspectRatio >= 1f) {
            outputWidth = baseMaxSide
            outputHeight = (outputWidth / outerAspectRatio).roundToInt().coerceAtLeast(1)
        } else {
            outputHeight = baseMaxSide
            outputWidth = (outputHeight * outerAspectRatio).roundToInt().coerceAtLeast(1)
        }
    }

    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(
        if (backgroundColor == Color.WHITE) Color.parseColor("#111111") else backgroundColor
    )

    val baseDim = max(outputWidth, outputHeight).toFloat()
    val outerInset = (baseDim * 0.10f).coerceAtLeast(28f)
    val frameAspectRatio = 0.76f
    val maxFrameWidth = (outputWidth - outerInset * 2f).coerceAtLeast(baseDim * 0.28f)
    val maxFrameHeight = (outputHeight - outerInset * 2f).coerceAtLeast(baseDim * 0.42f)
    val frameHeight = minOf(
        outputHeight * 0.58f,
        maxFrameHeight,
        maxFrameWidth / frameAspectRatio
    )
    val frameWidth = (frameHeight * frameAspectRatio).coerceAtMost(maxFrameWidth)
    val frameCenterY = (outputHeight * 0.5f).coerceIn(
        outerInset + frameHeight / 2f,
        outputHeight - outerInset - frameHeight / 2f
    )
    val frameOuterRect = RectF(
        outputWidth / 2f - frameWidth / 2f,
        frameCenterY - frameHeight / 2f,
        outputWidth / 2f + frameWidth / 2f,
        frameCenterY + frameHeight / 2f
    )

    val topSideBorder = (frameOuterRect.width() * 0.027f).coerceAtLeast(5.5f)
    val bottomBorder = (frameOuterRect.width() * 0.143f).coerceAtLeast(topSideBorder + 18f)
    val maxBottomBorder = frameOuterRect.height() * 0.34f
    val safeBottomBorder = bottomBorder.coerceAtMost(maxBottomBorder)
    val photoContainerRect = RectF(
        frameOuterRect.left + topSideBorder,
        frameOuterRect.top + topSideBorder,
        frameOuterRect.right - topSideBorder,
        frameOuterRect.bottom - safeBottomBorder
    )

    val photoScale = imageScaleFactor.coerceIn(0.93f, 0.985f)
    val photoWidth = photoContainerRect.width() * photoScale
    val photoHeight = photoContainerRect.height() * photoScale
    val photoRect = RectF(
        photoContainerRect.centerX() - photoWidth / 2f,
        photoContainerRect.centerY() - photoHeight / 2f,
        photoContainerRect.centerX() + photoWidth / 2f,
        photoContainerRect.centerY() + photoHeight / 2f
    )
    val breakoutMetrics = resolveWatermarkCutoutBreakoutMetrics(style)
    val safeSubjectScale = subjectScaleFactor.coerceIn(
        MIN_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
        MAX_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR
    )
    val breakoutRect = ensureCutoutCompositionCoversAnchorRect(
        drawRect = scaleAndOffsetRectForCutout(
            rect = photoRect,
            scale = breakoutMetrics.scale * safeSubjectScale,
            offsetX = photoRect.width() * breakoutMetrics.offsetXRatio,
            offsetY = photoRect.height() * breakoutMetrics.offsetYRatio
        ),
        anchorRect = photoRect
    )
    drawWatermarkCutoutBreakoutBackground(
        canvas = canvas,
        source = source,
        destinationRect = breakoutRect,
        visibleRect = photoRect
    )
    drawWatermarkCutoutBreakoutTopAndSideMattes(
        canvas = canvas,
        frameOuterRect = frameOuterRect,
        photoWindowRect = photoRect
    )

    if (foreground != null && !foreground.isRecycled) {
        canvas.save()
        canvas.clipRect(
            0f,
            0f,
            outputWidth.toFloat(),
            frameOuterRect.bottom
        )
        drawWatermarkCutoutBreakoutForeground(
            canvas = canvas,
            source = source,
            foreground = foreground,
            visibleRect = photoRect,
            destinationRect = breakoutRect,
            style = style
        )
        canvas.restore()
    }
    drawWatermarkCutoutBreakoutBottomMatte(
        canvas = canvas,
        frameOuterRect = frameOuterRect,
        photoWindowRect = photoRect
    )

    if (signatureText.isNotBlank()) {
        val signatureHorizontalMargin = max(outerInset * 0.72f, topSideBorder * 1.8f)
        val signatureBottomMargin = max(outerInset * 0.58f, topSideBorder * 1.35f)
        val signatureTop = max(
            frameOuterRect.bottom + topSideBorder * 0.75f,
            outputHeight - max(baseDim * 0.18f, topSideBorder * 7.5f)
        )
        val signatureRect = RectF(
            signatureHorizontalMargin,
            signatureTop,
            outputWidth - signatureHorizontalMargin,
            outputHeight - signatureBottomMargin
        )
        drawSignatureBottomRight(
            canvas = canvas,
            targetRect = signatureRect,
            signatureText = signatureText,
            textColor = signatureColor ?: Color.argb(255, 38, 38, 38),
            signatureTypeface = signatureTypeface,
            horizontalOffsetRatio = signatureHorizontalOffsetRatio,
            verticalOffsetRatio = signatureVerticalOffsetRatio,
            sizeScaleFactor = signatureScaleFactor
        )
    }
    return output
}

private data class WatermarkCutoutBreakoutMetrics(
    val scale: Float,
    val offsetXRatio: Float,
    val offsetYRatio: Float
)

private data class WatermarkCutoutProcessedForeground(
    val bitmap: Bitmap
)

private fun drawWatermarkCutoutBreakoutBackground(
    canvas: Canvas,
    source: Bitmap,
    destinationRect: RectF,
    visibleRect: RectF
) {
    if (source.isRecycled || visibleRect.width() <= 1f || visibleRect.height() <= 1f) return
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    val sourceRect = computeCenterCropSrcRectForCutout(source, visibleRect)
    canvas.save()
    canvas.clipRect(visibleRect)
    canvas.drawBitmap(
        source,
        sourceRect,
        destinationRect,
        bitmapPaint
    )
    canvas.restore()
}

private fun drawWatermarkCutoutBreakoutTopAndSideMattes(
    canvas: Canvas,
    frameOuterRect: RectF,
    photoWindowRect: RectF
) {
    val framePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = false
    }
    val seamOverlap = resolveCutoutFrameSeamOverlap(frameOuterRect)
    canvas.drawRect(
        frameOuterRect.left,
        frameOuterRect.top,
        frameOuterRect.right,
        (photoWindowRect.top + seamOverlap).coerceAtMost(photoWindowRect.bottom),
        framePaint
    )
    canvas.drawRect(
        frameOuterRect.left,
        photoWindowRect.top,
        (photoWindowRect.left + seamOverlap).coerceAtMost(photoWindowRect.right),
        photoWindowRect.bottom,
        framePaint
    )
    canvas.drawRect(
        (photoWindowRect.right - seamOverlap).coerceAtLeast(photoWindowRect.left),
        photoWindowRect.top,
        frameOuterRect.right,
        photoWindowRect.bottom,
        framePaint
    )
}

private fun drawWatermarkCutoutBreakoutBottomMatte(
    canvas: Canvas,
    frameOuterRect: RectF,
    photoWindowRect: RectF
) {
    val top = photoWindowRect.bottom.coerceIn(frameOuterRect.top, frameOuterRect.bottom)
    if (top >= frameOuterRect.bottom) return
    val seamOverlap = resolveCutoutFrameSeamOverlap(frameOuterRect)
    val framePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = false
    }
    val occluderTop = (top - seamOverlap).coerceAtLeast(frameOuterRect.top)
    canvas.drawRect(
        frameOuterRect.left,
        occluderTop,
        frameOuterRect.right,
        frameOuterRect.bottom,
        framePaint
    )
}

private fun drawWatermarkCutoutBreakoutForeground(
    canvas: Canvas,
    source: Bitmap,
    foreground: Bitmap,
    visibleRect: RectF,
    destinationRect: RectF,
    style: WatermarkCutoutOverlayStyle
) {
    if (
        source.isRecycled ||
        foreground.isRecycled ||
        visibleRect.width() <= 1f ||
        visibleRect.height() <= 1f
    ) return

    val sourceRect = computeCenterCropSrcRectForCutout(source, visibleRect)
    val foregroundRect = computeCenterCropSrcRectForCutout(foreground, visibleRect)
    val processedForeground = buildWatermarkCutoutBreakoutForegroundBitmap(
        source = source,
        sourceRect = sourceRect,
        foreground = foreground,
        foregroundRect = foregroundRect
    ) ?: return

    try {
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        drawWatermarkCutoutBreakoutForegroundShadow(
            canvas = canvas,
            foreground = processedForeground.bitmap,
            destinationRect = destinationRect,
            style = style
        )
        canvas.drawBitmap(
            processedForeground.bitmap,
            null,
            destinationRect,
            bitmapPaint
        )
    } finally {
        if (!processedForeground.bitmap.isRecycled) {
            processedForeground.bitmap.recycle()
        }
    }
}

private fun drawWatermarkCutoutBreakoutForegroundShadow(
    canvas: Canvas,
    foreground: Bitmap,
    destinationRect: RectF,
    style: WatermarkCutoutOverlayStyle
) {
    if (foreground.isRecycled) return
    val shadowAlpha = when (style) {
        WatermarkCutoutOverlayStyle.None -> 0
        WatermarkCutoutOverlayStyle.CleanLift -> 28
        WatermarkCutoutOverlayStyle.SoftShadow -> 54
        WatermarkCutoutOverlayStyle.WarmGallery -> 34
        WatermarkCutoutOverlayStyle.OffsetLift -> 44
    }
    if (shadowAlpha <= 0) return
    val blur = (destinationRect.width() * 0.022f).coerceIn(4f, 24f)
    val offsetY = (destinationRect.height() * 0.018f).coerceIn(3f, 18f)
    val offsetX = when (style) {
        WatermarkCutoutOverlayStyle.OffsetLift -> destinationRect.width() * 0.018f
        WatermarkCutoutOverlayStyle.SoftShadow -> destinationRect.width() * 0.01f
        else -> 0f
    }
    val shadowRect = RectF(destinationRect).apply {
        offset(offsetX, offsetY)
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        alpha = shadowAlpha
        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
    }
    canvas.drawBitmap(foreground, null, shadowRect, shadowPaint)
}

private fun resolveCutoutFrameSeamOverlap(frameOuterRect: RectF): Float =
    (frameOuterRect.width() * 0.004f).coerceIn(2f, 6f)

private fun resolveWatermarkCutoutBreakoutMetrics(
    style: WatermarkCutoutOverlayStyle
): WatermarkCutoutBreakoutMetrics = when (style) {
    WatermarkCutoutOverlayStyle.None -> WatermarkCutoutBreakoutMetrics(
        scale = 1f,
        offsetXRatio = 0f,
        offsetYRatio = 0f
    )
    WatermarkCutoutOverlayStyle.CleanLift -> WatermarkCutoutBreakoutMetrics(
        scale = 1.2f,
        offsetXRatio = 0f,
        offsetYRatio = -0.15f
    )
    WatermarkCutoutOverlayStyle.SoftShadow -> WatermarkCutoutBreakoutMetrics(
        scale = 1.22f,
        offsetXRatio = 0.014f,
        offsetYRatio = -0.16f
    )
    WatermarkCutoutOverlayStyle.WarmGallery -> WatermarkCutoutBreakoutMetrics(
        scale = 1.18f,
        offsetXRatio = -0.016f,
        offsetYRatio = -0.142f
    )
    WatermarkCutoutOverlayStyle.OffsetLift -> WatermarkCutoutBreakoutMetrics(
        scale = 1.24f,
        offsetXRatio = 0.028f,
        offsetYRatio = -0.17f
    )
}

private fun scaleAndOffsetRectForCutout(
    rect: RectF,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): RectF {
    val safeScale = scale.coerceAtLeast(0.01f)
    val scaledWidth = rect.width() * safeScale
    val scaledHeight = rect.height() * safeScale
    val centerX = rect.centerX() + offsetX
    val centerY = rect.centerY() + offsetY
    return RectF(
        centerX - scaledWidth / 2f,
        centerY - scaledHeight / 2f,
        centerX + scaledWidth / 2f,
        centerY + scaledHeight / 2f
    )
}

private fun buildWatermarkCutoutBreakoutForegroundBitmap(
    source: Bitmap,
    sourceRect: Rect,
    foreground: Bitmap,
    foregroundRect: Rect
): WatermarkCutoutProcessedForeground? {
    val cropWidth = minOf(sourceRect.width(), foregroundRect.width()).coerceAtLeast(1)
    val cropHeight = minOf(sourceRect.height(), foregroundRect.height()).coerceAtLeast(1)
    val normalizedSourceRect = normalizeCutoutCropRect(sourceRect, source.width, source.height, cropWidth, cropHeight)
    val normalizedForegroundRect = normalizeCutoutCropRect(
        foregroundRect,
        foreground.width,
        foreground.height,
        cropWidth,
        cropHeight
    )
    val total = cropWidth * cropHeight
    val sourcePixels = IntArray(total)
    val foregroundPixels = IntArray(total)
    source.getPixels(
        sourcePixels,
        0,
        cropWidth,
        normalizedSourceRect.left,
        normalizedSourceRect.top,
        cropWidth,
        cropHeight
    )
    foreground.getPixels(
        foregroundPixels,
        0,
        cropWidth,
        normalizedForegroundRect.left,
        normalizedForegroundRect.top,
        cropWidth,
        cropHeight
    )

    val originalAlpha = IntArray(total)
    var hasVisibleForeground = false
    for (index in 0 until total) {
        val alpha = foregroundPixels[index] ushr 24
        originalAlpha[index] = alpha
        if (alpha >= 14) {
            hasVisibleForeground = true
        }
    }
    if (!hasVisibleForeground) return null

    val featheredAlpha = originalAlpha.copyOf()
    softenCutoutAlphaValuesInPlace(
        alpha = featheredAlpha,
        width = cropWidth,
        height = cropHeight,
        passes = resolveCutoutMaskSofteningPasses(cropWidth, cropHeight)
    )

    val outputPixels = IntArray(total)
    for (index in 0 until total) {
        val finalAlpha = maxOf(originalAlpha[index], featheredAlpha[index]).coerceIn(0, 255)
        if (finalAlpha <= 0) continue
        val sourcePixel = sourcePixels[index]
        outputPixels[index] = Color.argb(
            finalAlpha,
            Color.red(sourcePixel),
            Color.green(sourcePixel),
            Color.blue(sourcePixel)
        )
    }

    val outputBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888).apply {
        setPixels(outputPixels, 0, cropWidth, 0, 0, cropWidth, cropHeight)
    }
    return WatermarkCutoutProcessedForeground(
        bitmap = outputBitmap
    )
}

private fun normalizeCutoutCropRect(
    rect: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int,
    cropWidth: Int,
    cropHeight: Int
): Rect {
    val safeLeft = rect.left.coerceIn(0, (bitmapWidth - cropWidth).coerceAtLeast(0))
    val safeTop = rect.top.coerceIn(0, (bitmapHeight - cropHeight).coerceAtLeast(0))
    return Rect(
        safeLeft,
        safeTop,
        (safeLeft + cropWidth).coerceAtMost(bitmapWidth),
        (safeTop + cropHeight).coerceAtMost(bitmapHeight)
    )
}

private fun resolveCutoutMaskSofteningPasses(width: Int, height: Int): Int = when (max(width, height)) {
    in 0..719 -> 1
    in 720..1279 -> 2
    else -> 3
}

private fun ensureCutoutCompositionCoversAnchorRect(
    drawRect: RectF,
    anchorRect: RectF
): RectF {
    var shiftX = 0f
    if (drawRect.left > anchorRect.left) {
        shiftX = anchorRect.left - drawRect.left
    } else if (drawRect.right < anchorRect.right) {
        shiftX = anchorRect.right - drawRect.right
    }
    var shiftY = 0f
    if (drawRect.top > anchorRect.top) {
        shiftY = anchorRect.top - drawRect.top
    } else if (drawRect.bottom < anchorRect.bottom) {
        shiftY = anchorRect.bottom - drawRect.bottom
    }
    return RectF(
        drawRect.left + shiftX,
        drawRect.top + shiftY,
        drawRect.right + shiftX,
        drawRect.bottom + shiftY
    )
}

private fun softenCutoutAlphaValuesInPlace(
    alpha: IntArray,
    width: Int,
    height: Int,
    passes: Int
) {
    if (alpha.isEmpty() || width <= 1 || height <= 1 || passes <= 0) return
    var current = alpha
    var buffer = IntArray(alpha.size)
    repeat(passes) {
        for (y in 0 until height) {
            val top = (y - 1).coerceAtLeast(0)
            val bottom = (y + 1).coerceAtMost(height - 1)
            for (x in 0 until width) {
                val left = (x - 1).coerceAtLeast(0)
                val right = (x + 1).coerceAtMost(width - 1)
                var sum = 0
                var count = 0
                for (ny in top..bottom) {
                    val rowOffset = ny * width
                    for (nx in left..right) {
                        sum += current[rowOffset + nx]
                        count += 1
                    }
                }
                buffer[y * width + x] = (sum / count).coerceIn(0, 255)
            }
        }
        val swap = current
        current = buffer
        buffer = swap
    }
    if (current !== alpha) {
        current.copyInto(alpha)
    }
}

private fun computeCenterCropSrcRectForCutout(
    source: Bitmap,
    dstRect: RectF
): Rect {
    val srcAspect = source.width.toFloat() / source.height.toFloat()
    val dstAspect = dstRect.width() / dstRect.height()
    return if (srcAspect > dstAspect) {
        val cropWidth = (source.height * dstAspect).roundToInt().coerceAtLeast(1)
        val left = ((source.width - cropWidth) / 2f).roundToInt().coerceAtLeast(0)
        Rect(left, 0, (left + cropWidth).coerceAtMost(source.width), source.height)
    } else {
        val cropHeight = (source.width / dstAspect).roundToInt().coerceAtLeast(1)
        val top = ((source.height - cropHeight) / 2f).roundToInt().coerceAtLeast(0)
        Rect(0, top, source.width, (top + cropHeight).coerceAtMost(source.height))
    }
}
