package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ComposePathEffect
import android.graphics.DashPathEffect
import android.graphics.DiscretePathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

internal enum class WatermarkSubjectOutlineLineStyle {
    Solid,
    Dashed,
    Symbol,
    Crayon,
    Chalk,
    Pencil,
    Marker,
    DoubleSketch
}

internal const val DEFAULT_WATERMARK_SUBJECT_OUTLINE_COLOR_INDEX = 7
internal const val DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO = 0.011f
internal const val MIN_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO = 0.004f
internal const val MAX_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO = 0.028f
internal const val DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO = 0f
internal const val MIN_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO = 0f
internal const val MAX_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO = 0.055f
internal const val DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT = "\u2726"
internal const val DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO = 0.034f
internal const val MIN_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO = 0.018f
internal const val MAX_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO = 0.085f

internal data class WatermarkSubjectOutlineConfig(
    val enabled: Boolean = false,
    val colorIndex: Int = DEFAULT_WATERMARK_SUBJECT_OUTLINE_COLOR_INDEX,
    val lineStyle: WatermarkSubjectOutlineLineStyle = WatermarkSubjectOutlineLineStyle.Solid,
    val widthRatio: Float = DEFAULT_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO,
    val outerBorderEnabled: Boolean = false,
    val offsetRatio: Float = DEFAULT_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO,
    val symbolText: String = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT,
    val symbolSizeRatio: Float = DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
) {
    fun normalized(): WatermarkSubjectOutlineConfig = copy(
        colorIndex = colorIndex.coerceAtLeast(0),
        widthRatio = widthRatio.coerceIn(
            MIN_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO,
            MAX_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO
        ),
        offsetRatio = offsetRatio.coerceIn(
            MIN_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO,
            MAX_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO
        ),
        symbolText = sanitizeWatermarkSubjectOutlineSymbolText(symbolText),
        symbolSizeRatio = symbolSizeRatio.coerceIn(
            MIN_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO,
            MAX_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO
        )
    )
}

internal fun watermarkSubjectOutlineSectionTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u4e3b\u4f53\u63cf\u8fb9"
    AppLanguage.TraditionalChinese -> "\u4e3b\u9ad4\u63cf\u908a"
    else -> "Subject Outline"
}

internal fun watermarkSubjectOutlineStyleLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7ebf\u6761\u6837\u5f0f"
    AppLanguage.TraditionalChinese -> "\u7dda\u689d\u6a23\u5f0f"
    else -> "Line Style"
}

internal fun watermarkSubjectOutlineSolidLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u5b9e\u7ebf"
    AppLanguage.TraditionalChinese -> "\u5be6\u7dda"
    else -> "Solid"
}

internal fun watermarkSubjectOutlineDashedLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u865a\u7ebf"
    AppLanguage.TraditionalChinese -> "\u865b\u7dda"
    else -> "Dashed"
}

internal fun watermarkSubjectOutlineSymbolLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7b26\u53f7"
    AppLanguage.TraditionalChinese -> "\u7b26\u865f"
    else -> "Symbol"
}

internal fun watermarkSubjectOutlineCrayonLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u8721\u7b14"
    AppLanguage.TraditionalChinese -> "\u881f\u7b46"
    else -> "Crayon"
}

internal fun watermarkSubjectOutlineChalkLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7c89\u7b14"
    AppLanguage.TraditionalChinese -> "\u7c89\u7b46"
    else -> "Chalk"
}

internal fun watermarkSubjectOutlinePencilLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u94c5\u7b14"
    AppLanguage.TraditionalChinese -> "\u925b\u7b46"
    else -> "Pencil"
}

internal fun watermarkSubjectOutlineMarkerLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u9a6c\u514b\u7b14"
    AppLanguage.TraditionalChinese -> "\u99ac\u514b\u7b46"
    else -> "Marker"
}

internal fun watermarkSubjectOutlineDoubleSketchLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u53cc\u7ebf\u624b\u7a3f"
    AppLanguage.TraditionalChinese -> "\u96d9\u7dda\u624b\u7a3f"
    else -> "Double Sketch"
}

internal fun watermarkSubjectOutlineThicknessLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7c97\u7ec6"
    AppLanguage.TraditionalChinese -> "\u7c97\u7d30"
    else -> "Thickness"
}

internal fun watermarkSubjectOutlineDistanceLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u5916\u6269\u8ddd\u79bb"
    AppLanguage.TraditionalChinese -> "\u5916\u64f4\u8ddd\u96e2"
    else -> "Outline Distance"
}

internal fun watermarkSubjectOutlineSymbolTextLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "Emoji / \u989c\u6587\u5b57"
    AppLanguage.TraditionalChinese -> "Emoji / \u984f\u6587\u5b57"
    else -> "Emoji / Text"
}

internal fun watermarkSubjectOutlineSymbolSizeLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7b26\u53f7\u5927\u5c0f"
    AppLanguage.TraditionalChinese -> "\u7b26\u865f\u5927\u5c0f"
    else -> "Symbol Size"
}

internal fun watermarkSubjectOutlineColorLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u63cf\u8fb9\u989c\u8272"
    AppLanguage.TraditionalChinese -> "\u63cf\u908a\u984f\u8272"
    else -> "Outline Color"
}

internal fun watermarkSubjectOutlineOuterBorderLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u56fe\u7247\u5916\u8fb9\u6846"
    AppLanguage.TraditionalChinese -> "\u5716\u7247\u5916\u908a\u6846"
    else -> "Image Border"
}

internal fun watermarkSubjectOutlineOuterBorderHint(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u5173\u95ed\u540e\u4e0d\u518d\u7ed9\u56fe\u7247\u52a0\u5916\u5c42\u8fb9\u6846"
    AppLanguage.TraditionalChinese -> "\u95dc\u9589\u5f8c\u4e0d\u518d\u7d66\u5716\u7247\u52a0\u5916\u5c64\u908a\u6846"
    else -> "Turn off the extra frame around the image"
}

internal fun watermarkSubjectOutlineOuterBorderColorLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u5916\u8fb9\u6846\u989c\u8272"
    AppLanguage.TraditionalChinese -> "\u5916\u908a\u6846\u984f\u8272"
    else -> "Border Color"
}

internal fun drawWatermarkSubjectOutline(
    canvas: Canvas,
    foreground: Bitmap,
    anchorRect: RectF,
    outlineColor: Int,
    config: WatermarkSubjectOutlineConfig
) {
    val safeConfig = config.normalized()
    if (!safeConfig.enabled) return
    if (foreground.isRecycled || anchorRect.width() <= 1f || anchorRect.height() <= 1f) return
    val minAnchorSide = minOf(anchorRect.width(), anchorRect.height()).coerceAtLeast(1f)
    val outlinePath = buildWatermarkSubjectOutlinePath(
        foreground = foreground,
        anchorRect = anchorRect,
        outwardOffset = minAnchorSide * safeConfig.offsetRatio
    ) ?: return
    val outlineStrokeWidth = (
        minAnchorSide * safeConfig.widthRatio
        ).coerceAtLeast(1.5f)
    canvas.save()
    canvas.clipRect(anchorRect)
    if (safeConfig.lineStyle == WatermarkSubjectOutlineLineStyle.Symbol) {
        drawWatermarkSubjectSymbolOutline(
            canvas = canvas,
            path = outlinePath,
            color = outlineColor,
            text = safeConfig.symbolText,
            textSize = (minAnchorSide * safeConfig.symbolSizeRatio).coerceAtLeast(10f)
        )
        canvas.restore()
        return
    }
    if (safeConfig.lineStyle.isWatermarkSubjectOutlineArtStyle()) {
        drawWatermarkSubjectArtOutline(
            canvas = canvas,
            path = outlinePath,
            color = outlineColor,
            strokeWidth = outlineStrokeWidth,
            lineStyle = safeConfig.lineStyle
        )
        canvas.restore()
        return
    }
    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = outlineColor
        style = Paint.Style.STROKE
        strokeWidth = outlineStrokeWidth
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = if (safeConfig.lineStyle == WatermarkSubjectOutlineLineStyle.Dashed) {
            DashPathEffect(
                floatArrayOf(outlineStrokeWidth * 2.25f, outlineStrokeWidth * 1.55f),
                0f
            )
        } else {
            null
        }
    }
    canvas.drawPath(outlinePath, outlinePaint)
    canvas.restore()
}

private fun WatermarkSubjectOutlineLineStyle.isWatermarkSubjectOutlineArtStyle(): Boolean = when (this) {
    WatermarkSubjectOutlineLineStyle.Crayon,
    WatermarkSubjectOutlineLineStyle.Chalk,
    WatermarkSubjectOutlineLineStyle.Pencil,
    WatermarkSubjectOutlineLineStyle.Marker,
    WatermarkSubjectOutlineLineStyle.DoubleSketch -> true
    WatermarkSubjectOutlineLineStyle.Solid,
    WatermarkSubjectOutlineLineStyle.Dashed,
    WatermarkSubjectOutlineLineStyle.Symbol -> false
}

private fun drawWatermarkSubjectArtOutline(
    canvas: Canvas,
    path: Path,
    color: Int,
    strokeWidth: Float,
    lineStyle: WatermarkSubjectOutlineLineStyle
) {
    when (lineStyle) {
        WatermarkSubjectOutlineLineStyle.Crayon -> drawWatermarkSubjectCrayonOutline(
            canvas = canvas,
            path = path,
            color = color,
            strokeWidth = strokeWidth
        )
        WatermarkSubjectOutlineLineStyle.Chalk -> drawWatermarkSubjectChalkOutline(
            canvas = canvas,
            path = path,
            color = color,
            strokeWidth = strokeWidth
        )
        WatermarkSubjectOutlineLineStyle.Pencil -> drawWatermarkSubjectPencilOutline(
            canvas = canvas,
            path = path,
            color = color,
            strokeWidth = strokeWidth
        )
        WatermarkSubjectOutlineLineStyle.Marker -> drawWatermarkSubjectMarkerOutline(
            canvas = canvas,
            path = path,
            color = color,
            strokeWidth = strokeWidth
        )
        WatermarkSubjectOutlineLineStyle.DoubleSketch -> drawWatermarkSubjectDoubleSketchOutline(
            canvas = canvas,
            path = path,
            color = color,
            strokeWidth = strokeWidth
        )
        WatermarkSubjectOutlineLineStyle.Solid,
        WatermarkSubjectOutlineLineStyle.Dashed,
        WatermarkSubjectOutlineLineStyle.Symbol -> Unit
    }
}

private fun drawWatermarkSubjectCrayonOutline(
    canvas: Canvas,
    path: Path,
    color: Int,
    strokeWidth: Float
) {
    val dryBreaks = DashPathEffect(
        floatArrayOf(
            strokeWidth * 1.12f,
            strokeWidth * 0.46f,
            strokeWidth * 0.48f,
            strokeWidth * 0.32f
        ),
        strokeWidth * 0.37f
    )
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 210,
        strokeWidth = strokeWidth * 1.55f,
        pathEffect = ComposePathEffect(
            dryBreaks,
            DiscretePathEffect(strokeWidth * 1.25f, strokeWidth * 0.78f)
        )
    )
    drawWatermarkSubjectTranslatedStroke(
        canvas = canvas,
        path = path,
        dx = strokeWidth * 0.32f,
        dy = -strokeWidth * 0.22f,
        color = color,
        alpha = 118,
        strokeWidth = strokeWidth * 0.92f,
        pathEffect = DiscretePathEffect(strokeWidth * 0.72f, strokeWidth * 0.5f)
    )
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 78,
        strokeWidth = strokeWidth * 2.05f,
        pathEffect = DashPathEffect(floatArrayOf(strokeWidth * 0.22f, strokeWidth * 0.58f), 0f)
    )
    drawWatermarkSubjectPathSpeckles(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 86,
        strokeWidth = strokeWidth,
        spacingMultiplier = 0.95f,
        minRadiusMultiplier = 0.08f,
        maxRadiusMultiplier = 0.28f,
        normalJitterMultiplier = 1.15f,
        tangentJitterMultiplier = 0.72f,
        phase = 0.17f
    )
}

private fun drawWatermarkSubjectChalkOutline(
    canvas: Canvas,
    path: Path,
    color: Int,
    strokeWidth: Float
) {
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 72,
        strokeWidth = strokeWidth * 3.1f,
        pathEffect = DiscretePathEffect(strokeWidth * 2.1f, strokeWidth * 1.15f),
        maskFilter = BlurMaskFilter(strokeWidth * 0.72f, BlurMaskFilter.Blur.NORMAL)
    )
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 190,
        strokeWidth = strokeWidth * 1.65f,
        pathEffect = ComposePathEffect(
            DashPathEffect(floatArrayOf(strokeWidth * 0.58f, strokeWidth * 0.34f), strokeWidth * 0.18f),
            DiscretePathEffect(strokeWidth * 1.45f, strokeWidth * 0.82f)
        )
    )
    drawWatermarkSubjectPathSpeckles(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 132,
        strokeWidth = strokeWidth,
        spacingMultiplier = 0.55f,
        minRadiusMultiplier = 0.07f,
        maxRadiusMultiplier = 0.22f,
        normalJitterMultiplier = 2.4f,
        tangentJitterMultiplier = 0.95f,
        phase = 0.31f
    )
    drawWatermarkSubjectPathSpeckles(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 68,
        strokeWidth = strokeWidth,
        spacingMultiplier = 0.82f,
        minRadiusMultiplier = 0.15f,
        maxRadiusMultiplier = 0.38f,
        normalJitterMultiplier = 3.2f,
        tangentJitterMultiplier = 1.3f,
        phase = 0.67f
    )
}

private fun drawWatermarkSubjectPencilOutline(
    canvas: Canvas,
    path: Path,
    color: Int,
    strokeWidth: Float
) {
    val pencilWidth = (strokeWidth * 0.56f).coerceAtLeast(1f)
    drawWatermarkSubjectTranslatedStroke(
        canvas = canvas,
        path = path,
        dx = -pencilWidth * 1.25f,
        dy = pencilWidth * 0.54f,
        color = color,
        alpha = 108,
        strokeWidth = pencilWidth * 0.95f,
        pathEffect = DiscretePathEffect(strokeWidth * 0.85f, strokeWidth * 0.34f)
    )
    drawWatermarkSubjectTranslatedStroke(
        canvas = canvas,
        path = path,
        dx = pencilWidth * 0.88f,
        dy = -pencilWidth * 0.44f,
        color = color,
        alpha = 164,
        strokeWidth = pencilWidth * 0.72f,
        pathEffect = DiscretePathEffect(strokeWidth * 0.68f, strokeWidth * 0.22f)
    )
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 104,
        strokeWidth = pencilWidth * 0.42f,
        pathEffect = DashPathEffect(floatArrayOf(strokeWidth * 0.78f, strokeWidth * 0.5f), strokeWidth * 0.27f)
    )
    drawWatermarkSubjectSketchTicks(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 84,
        strokeWidth = pencilWidth * 0.42f,
        spacingMultiplier = 2.8f,
        tickLengthMultiplier = 1.65f,
        phase = 0.41f
    )
}

private fun drawWatermarkSubjectMarkerOutline(
    canvas: Canvas,
    path: Path,
    color: Int,
    strokeWidth: Float
) {
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 70,
        strokeWidth = strokeWidth * 3.35f,
        maskFilter = BlurMaskFilter(strokeWidth * 0.24f, BlurMaskFilter.Blur.NORMAL)
    )
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 118,
        strokeWidth = strokeWidth * 2.12f
    )
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 146,
        strokeWidth = strokeWidth * 0.72f,
        pathEffect = DashPathEffect(floatArrayOf(strokeWidth * 2.4f, strokeWidth * 1.2f), strokeWidth * 0.35f)
    )
    drawWatermarkSubjectTranslatedStroke(
        canvas = canvas,
        path = path,
        dx = strokeWidth * 0.46f,
        dy = strokeWidth * 0.2f,
        color = color,
        alpha = 64,
        strokeWidth = strokeWidth * 1.45f
    )
}

private fun drawWatermarkSubjectDoubleSketchOutline(
    canvas: Canvas,
    path: Path,
    color: Int,
    strokeWidth: Float
) {
    drawWatermarkSubjectTranslatedStroke(
        canvas = canvas,
        path = path,
        dx = -strokeWidth * 0.95f,
        dy = strokeWidth * 0.72f,
        color = color,
        alpha = 192,
        strokeWidth = strokeWidth * 0.86f,
        pathEffect = DiscretePathEffect(strokeWidth * 1.05f, strokeWidth * 0.36f)
    )
    drawWatermarkSubjectTranslatedStroke(
        canvas = canvas,
        path = path,
        dx = strokeWidth * 1.05f,
        dy = -strokeWidth * 0.76f,
        color = color,
        alpha = 150,
        strokeWidth = strokeWidth * 0.72f,
        pathEffect = DiscretePathEffect(strokeWidth * 0.95f, strokeWidth * 0.28f)
    )
    drawWatermarkSubjectSketchTicks(
        canvas = canvas,
        path = path,
        color = color,
        alpha = 78,
        strokeWidth = strokeWidth * 0.34f,
        spacingMultiplier = 3.6f,
        tickLengthMultiplier = 2.2f,
        phase = 0.58f
    )
}

private fun drawWatermarkSubjectTranslatedStroke(
    canvas: Canvas,
    path: Path,
    dx: Float,
    dy: Float,
    color: Int,
    alpha: Int,
    strokeWidth: Float,
    pathEffect: PathEffect? = null
) {
    canvas.save()
    canvas.translate(dx, dy)
    drawWatermarkSubjectArtStroke(
        canvas = canvas,
        path = path,
        color = color,
        alpha = alpha,
        strokeWidth = strokeWidth,
        pathEffect = pathEffect
    )
    canvas.restore()
}

private fun drawWatermarkSubjectArtStroke(
    canvas: Canvas,
    path: Path,
    color: Int,
    alpha: Int,
    strokeWidth: Float,
    pathEffect: PathEffect? = null,
    maskFilter: BlurMaskFilter? = null
) {
    val baseAlpha = (color ushr 24).coerceIn(0, 255)
    val effectiveAlpha = ((baseAlpha * alpha) / 255).coerceIn(0, 255)
    val opaqueColor = color or (0xFF shl 24)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = opaqueColor
        this.alpha = effectiveAlpha
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth.coerceAtLeast(1f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        this.pathEffect = pathEffect
        this.maskFilter = maskFilter
    }
    canvas.drawPath(path, paint)
}

private fun drawWatermarkSubjectPathSpeckles(
    canvas: Canvas,
    path: Path,
    color: Int,
    alpha: Int,
    strokeWidth: Float,
    spacingMultiplier: Float,
    minRadiusMultiplier: Float,
    maxRadiusMultiplier: Float,
    normalJitterMultiplier: Float,
    tangentJitterMultiplier: Float,
    phase: Float
) {
    val baseAlpha = (color ushr 24).coerceIn(0, 255)
    val effectiveAlpha = ((baseAlpha * alpha) / 255).coerceIn(0, 255)
    if (effectiveAlpha <= 0) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color or (0xFF shl 24)
        this.alpha = effectiveAlpha
        style = Paint.Style.FILL
    }
    val measure = PathMeasure(path, false)
    val pos = FloatArray(2)
    val tan = FloatArray(2)
    var contourIndex = 0
    do {
        val length = measure.length
        if (length <= 1f) continue
        val spacing = (strokeWidth * spacingMultiplier).coerceAtLeast(2.2f)
        var distance = spacing * phase.coerceIn(0f, 1f)
        var pointIndex = 0
        while (distance < length) {
            if (measure.getPosTan(distance, pos, tan)) {
                val seed = contourIndex * 4099 + pointIndex * 131
                val tangentScale = strokeWidth * tangentJitterMultiplier
                val normalScale = strokeWidth * normalJitterMultiplier
                val tangentOffset = (watermarkSubjectOutlineNoise(seed) - 0.5f) * tangentScale
                val normalOffset = (watermarkSubjectOutlineNoise(seed + 17) - 0.5f) * normalScale
                val tangentLength = sqrt(tan[0] * tan[0] + tan[1] * tan[1]).coerceAtLeast(0.0001f)
                val tx = tan[0] / tangentLength
                val ty = tan[1] / tangentLength
                val nx = -ty
                val ny = tx
                val radiusNoise = watermarkSubjectOutlineNoise(seed + 37)
                val radius = strokeWidth * (
                    minRadiusMultiplier + (maxRadiusMultiplier - minRadiusMultiplier) * radiusNoise
                    )
                canvas.drawCircle(
                    pos[0] + tx * tangentOffset + nx * normalOffset,
                    pos[1] + ty * tangentOffset + ny * normalOffset,
                    radius.coerceAtLeast(0.6f),
                    paint
                )
            }
            distance += spacing
            pointIndex += 1
        }
        contourIndex += 1
    } while (measure.nextContour())
}

private fun drawWatermarkSubjectSketchTicks(
    canvas: Canvas,
    path: Path,
    color: Int,
    alpha: Int,
    strokeWidth: Float,
    spacingMultiplier: Float,
    tickLengthMultiplier: Float,
    phase: Float
) {
    val baseAlpha = (color ushr 24).coerceIn(0, 255)
    val effectiveAlpha = ((baseAlpha * alpha) / 255).coerceIn(0, 255)
    if (effectiveAlpha <= 0) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color or (0xFF shl 24)
        this.alpha = effectiveAlpha
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth.coerceAtLeast(0.8f)
        strokeCap = Paint.Cap.ROUND
    }
    val measure = PathMeasure(path, false)
    val pos = FloatArray(2)
    val tan = FloatArray(2)
    var contourIndex = 0
    do {
        val length = measure.length
        if (length <= 1f) continue
        val spacing = (strokeWidth * spacingMultiplier).coerceAtLeast(10f)
        var distance = spacing * phase.coerceIn(0f, 1f)
        var tickIndex = 0
        while (distance < length) {
            if (measure.getPosTan(distance, pos, tan)) {
                val seed = contourIndex * 7919 + tickIndex * 97
                val tangentLength = sqrt(tan[0] * tan[0] + tan[1] * tan[1]).coerceAtLeast(0.0001f)
                val tx = tan[0] / tangentLength
                val ty = tan[1] / tangentLength
                val nx = -ty
                val ny = tx
                val side = if (watermarkSubjectOutlineNoise(seed) > 0.5f) 1f else -1f
                val halfLength = strokeWidth * tickLengthMultiplier * (
                    0.65f + watermarkSubjectOutlineNoise(seed + 19) * 0.7f
                    )
                val centerShift = (watermarkSubjectOutlineNoise(seed + 41) - 0.5f) * strokeWidth * 1.8f
                val cx = pos[0] + nx * centerShift
                val cy = pos[1] + ny * centerShift
                canvas.drawLine(
                    cx - nx * halfLength * 0.25f - tx * halfLength * 0.22f * side,
                    cy - ny * halfLength * 0.25f - ty * halfLength * 0.22f * side,
                    cx + nx * halfLength * side,
                    cy + ny * halfLength * side,
                    paint
                )
            }
            distance += spacing
            tickIndex += 1
        }
        contourIndex += 1
    } while (measure.nextContour())
}

private fun watermarkSubjectOutlineNoise(seed: Int): Float {
    var value = seed * 1103515245 + 12345
    value = value xor (value ushr 16)
    value *= 224682251
    value = value xor (value ushr 13)
    return (value and 0xFFFF).toFloat() / 65535f
}

private data class WatermarkOutlinePoint(
    val x: Int,
    val y: Int
)

private data class WatermarkOutlineFloatPoint(
    val x: Float,
    val y: Float
)

private const val WATERMARK_SUBJECT_OUTLINE_ALPHA_THRESHOLD = 28
private const val WATERMARK_SUBJECT_OUTLINE_MASK_MAX_SIDE = 384

private fun buildWatermarkSubjectOutlinePath(
    foreground: Bitmap,
    anchorRect: RectF,
    outwardOffset: Float
): Path? {
    val workBitmap = createWatermarkSubjectOutlineWorkBitmap(foreground)
    return try {
        val mask = extractLargestWatermarkSubjectMask(
            bitmap = workBitmap,
            alphaThreshold = WATERMARK_SUBJECT_OUTLINE_ALPHA_THRESHOLD
        ) ?: return null
        val outlineLoop = buildWatermarkSubjectOutlineLoop(
            mask = mask,
            width = workBitmap.width,
            height = workBitmap.height
        ) ?: return null
        val sourceRect = computeWatermarkSubjectOutlineCenterCropSourceRect(
            bitmapWidth = workBitmap.width,
            bitmapHeight = workBitmap.height,
            destinationRect = anchorRect
        )
        val mappedPoints = outlineLoop.map { point ->
            WatermarkOutlineFloatPoint(
                x = anchorRect.left + (
                    ((point.x.toFloat() - sourceRect.left) / sourceRect.width())
                    * anchorRect.width()
                    ),
                y = anchorRect.top + (
                    ((point.y.toFloat() - sourceRect.top) / sourceRect.height())
                    * anchorRect.height()
                    )
            )
        }
        val outlinePoints = if (outwardOffset > 0.1f) {
            offsetWatermarkSubjectOutlinePoints(mappedPoints, outwardOffset)
        } else {
            mappedPoints
        }
        buildWatermarkSubjectOutlineSmoothPath(outlinePoints)
    } finally {
        if (workBitmap !== foreground && !workBitmap.isRecycled) {
            workBitmap.recycle()
        }
    }
}

internal fun sanitizeWatermarkSubjectOutlineSymbolText(input: String): String {
    val sanitized = input
        .filter { char -> !char.isISOControl() }
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(12)
    return sanitized.ifBlank { DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT }
}

internal fun isWatermarkSubjectOutlineDefaultSymbolText(input: String): Boolean {
    val sanitized = sanitizeWatermarkSubjectOutlineSymbolText(input)
    return sanitized == DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT || sanitized == "\u2605"
}

private fun offsetWatermarkSubjectOutlinePoints(
    points: List<WatermarkOutlineFloatPoint>,
    distance: Float
): List<WatermarkOutlineFloatPoint> {
    if (points.size < 3 || distance <= 0f) return points
    val centerX = points.sumOf { it.x.toDouble() }.toFloat() / points.size.toFloat()
    val centerY = points.sumOf { it.y.toDouble() }.toFloat() / points.size.toFloat()
    return points.map { point ->
        val dx = point.x - centerX
        val dy = point.y - centerY
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
        WatermarkOutlineFloatPoint(
            x = point.x + (dx / length) * distance,
            y = point.y + (dy / length) * distance
        )
    }
}

private fun drawWatermarkSubjectSymbolOutline(
    canvas: Canvas,
    path: Path,
    color: Int,
    text: String,
    textSize: Float
) {
    val safeText = sanitizeWatermarkSubjectOutlineSymbolText(text)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        this.textSize = textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val pathMeasure = PathMeasure(path, false)
    val length = pathMeasure.length
    if (length <= 1f) return
    val textWidth = textPaint.measureText(safeText).coerceAtLeast(textSize)
    val spacing = max(textWidth * 1.35f, textSize * 1.55f)
    val pos = FloatArray(2)
    val tan = FloatArray(2)
    val baselineOffset = -((textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) * 0.5f)
    var distance = spacing * 0.5f
    while (distance < length) {
        if (pathMeasure.getPosTan(distance, pos, tan)) {
            val angle = Math.toDegrees(atan2(tan[1], tan[0]).toDouble()).toFloat()
            canvas.save()
            canvas.rotate(angle, pos[0], pos[1])
            canvas.drawText(safeText, pos[0], pos[1] + baselineOffset, textPaint)
            canvas.restore()
        }
        distance += spacing
    }
}

private fun createWatermarkSubjectOutlineWorkBitmap(source: Bitmap): Bitmap {
    if (source.width <= 0 || source.height <= 0) return source
    val maxSide = max(source.width, source.height)
    if (maxSide <= WATERMARK_SUBJECT_OUTLINE_MASK_MAX_SIDE) {
        return source
    }
    val scale = WATERMARK_SUBJECT_OUTLINE_MASK_MAX_SIDE.toFloat() / maxSide.toFloat()
    return Bitmap.createScaledBitmap(
        source,
        (source.width * scale).toInt().coerceAtLeast(1),
        (source.height * scale).toInt().coerceAtLeast(1),
        true
    )
}

private fun extractLargestWatermarkSubjectMask(
    bitmap: Bitmap,
    alphaThreshold: Int
): BooleanArray? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 1 || height <= 1) return null
    val total = width * height
    val pixels = IntArray(total)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val opaqueMask = BooleanArray(total)
    var opaqueCount = 0
    for (index in 0 until total) {
        val isOpaque = (pixels[index] ushr 24) >= alphaThreshold
        opaqueMask[index] = isOpaque
        if (isOpaque) {
            opaqueCount += 1
        }
    }
    if (opaqueCount < 24) return null
    bridgeWatermarkSubjectOutlineDiagonals(opaqueMask, width, height)
    val componentIds = IntArray(total)
    val queue = IntArray(total)
    var nextComponentId = 0
    var largestComponentId = 0
    var largestComponentSize = 0
    for (startIndex in 0 until total) {
        if (!opaqueMask[startIndex] || componentIds[startIndex] != 0) continue
        nextComponentId += 1
        componentIds[startIndex] = nextComponentId
        var head = 0
        var tail = 0
        queue[tail++] = startIndex
        var componentSize = 0
        while (head < tail) {
            val index = queue[head++]
            componentSize += 1
            val x = index % width
            val y = index / width
            for (offsetY in -1..1) {
                for (offsetX in -1..1) {
                    if (offsetX == 0 && offsetY == 0) continue
                    val nextX = x + offsetX
                    val nextY = y + offsetY
                    if (nextX !in 0 until width || nextY !in 0 until height) continue
                    val nextIndex = nextY * width + nextX
                    if (!opaqueMask[nextIndex] || componentIds[nextIndex] != 0) continue
                    componentIds[nextIndex] = nextComponentId
                    queue[tail++] = nextIndex
                }
            }
        }
        if (componentSize > largestComponentSize) {
            largestComponentId = nextComponentId
            largestComponentSize = componentSize
        }
    }
    if (largestComponentId == 0 || largestComponentSize < 24) return null
    return BooleanArray(total) { index -> componentIds[index] == largestComponentId }
}

private fun bridgeWatermarkSubjectOutlineDiagonals(
    mask: BooleanArray,
    width: Int,
    height: Int
) {
    if (width < 2 || height < 2) return
    val bridgeMask = BooleanArray(mask.size)
    for (y in 0 until (height - 1)) {
        for (x in 0 until (width - 1)) {
            val topLeftIndex = y * width + x
            val topRightIndex = topLeftIndex + 1
            val bottomLeftIndex = topLeftIndex + width
            val bottomRightIndex = bottomLeftIndex + 1
            val topLeft = mask[topLeftIndex]
            val topRight = mask[topRightIndex]
            val bottomLeft = mask[bottomLeftIndex]
            val bottomRight = mask[bottomRightIndex]
            if (topLeft && bottomRight && !topRight && !bottomLeft) {
                bridgeMask[topRightIndex] = true
                bridgeMask[bottomLeftIndex] = true
            } else if (!topLeft && topRight && bottomLeft && !bottomRight) {
                bridgeMask[topLeftIndex] = true
                bridgeMask[bottomRightIndex] = true
            }
        }
    }
    for (index in bridgeMask.indices) {
        if (bridgeMask[index]) {
            mask[index] = true
        }
    }
}

private fun buildWatermarkSubjectOutlineLoop(
    mask: BooleanArray,
    width: Int,
    height: Int
): List<WatermarkOutlinePoint>? {
    if (width <= 0 || height <= 0) return null
    val outgoingByPoint = linkedMapOf<Long, MutableList<WatermarkOutlinePoint>>()
    val unusedEdges = linkedSetOf<Long>()
    fun addEdge(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int
    ) {
        val start = WatermarkOutlinePoint(startX, startY)
        val end = WatermarkOutlinePoint(endX, endY)
        outgoingByPoint.getOrPut(packWatermarkOutlinePoint(start)) { mutableListOf() }.add(end)
        unusedEdges.add(packWatermarkOutlineEdge(start, end))
    }
    fun isFilled(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        return mask[y * width + x]
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (!isFilled(x, y)) continue
            if (!isFilled(x, y - 1)) addEdge(x, y, x + 1, y)
            if (!isFilled(x + 1, y)) addEdge(x + 1, y, x + 1, y + 1)
            if (!isFilled(x, y + 1)) addEdge(x + 1, y + 1, x, y + 1)
            if (!isFilled(x - 1, y)) addEdge(x, y + 1, x, y)
        }
    }

    var largestLoop: List<WatermarkOutlinePoint>? = null
    var largestArea = 0f
    while (unusedEdges.isNotEmpty()) {
        val firstEdge = unusedEdges.first()
        val start = unpackWatermarkOutlineEdgeStart(firstEdge)
        val end = unpackWatermarkOutlineEdgeEnd(firstEdge)
        val tracedLoop = traceWatermarkSubjectOutlineLoop(
            start = start,
            firstEnd = end,
            outgoingByPoint = outgoingByPoint,
            unusedEdges = unusedEdges
        ) ?: continue
        val simplifiedLoop = simplifyWatermarkSubjectOutlineLoop(tracedLoop)
        if (simplifiedLoop.size < 3) continue
        val area = abs(computeWatermarkSubjectOutlineArea(simplifiedLoop))
        if (area > largestArea) {
            largestArea = area
            largestLoop = simplifiedLoop
        }
    }
    return largestLoop
}

private fun traceWatermarkSubjectOutlineLoop(
    start: WatermarkOutlinePoint,
    firstEnd: WatermarkOutlinePoint,
    outgoingByPoint: Map<Long, List<WatermarkOutlinePoint>>,
    unusedEdges: MutableSet<Long>
): List<WatermarkOutlinePoint>? {
    val loop = mutableListOf(start)
    var current = start
    var next = firstEnd
    var guard = 0
    while (guard < 20000) {
        val edgeKey = packWatermarkOutlineEdge(current, next)
        if (!unusedEdges.remove(edgeKey)) {
            return null
        }
        if (next == start) {
            return loop
        }
        loop += next
        val nextCandidates = outgoingByPoint[packWatermarkOutlinePoint(next)].orEmpty()
        val followingPoint = nextCandidates.firstOrNull { candidate ->
            unusedEdges.contains(packWatermarkOutlineEdge(next, candidate))
        } ?: return null
        current = next
        next = followingPoint
        guard += 1
    }
    return null
}

private fun simplifyWatermarkSubjectOutlineLoop(
    points: List<WatermarkOutlinePoint>
): List<WatermarkOutlinePoint> {
    if (points.size < 4) return points.distinct()
    val cleaned = points.toMutableList()
    var changed = true
    while (changed && cleaned.size > 3) {
        changed = false
        var index = 0
        while (index < cleaned.size) {
            val currentSize = cleaned.size
            val previous = cleaned[(index - 1 + currentSize) % currentSize]
            val current = cleaned[index]
            val next = cleaned[(index + 1) % currentSize]
            if (current == next || current == previous) {
                cleaned.removeAt(index)
                changed = true
                continue
            }
            val isVertical = previous.x == current.x && current.x == next.x
            val isHorizontal = previous.y == current.y && current.y == next.y
            if (isVertical || isHorizontal) {
                cleaned.removeAt(index)
                changed = true
                continue
            }
            index += 1
        }
    }
    return cleaned
}

private fun buildWatermarkSubjectOutlineSmoothPath(
    points: List<WatermarkOutlineFloatPoint>
): Path? {
    if (points.size < 3) return null
    val path = Path()
    val startMidpoint = midpointWatermarkOutline(points.last(), points.first())
    path.moveTo(startMidpoint.x, startMidpoint.y)
    points.forEachIndexed { index, current ->
        val next = points[(index + 1) % points.size]
        val midpoint = midpointWatermarkOutline(current, next)
        path.quadTo(current.x, current.y, midpoint.x, midpoint.y)
    }
    path.close()
    return path
}

private fun midpointWatermarkOutline(
    first: WatermarkOutlineFloatPoint,
    second: WatermarkOutlineFloatPoint
): WatermarkOutlineFloatPoint = WatermarkOutlineFloatPoint(
    x = (first.x + second.x) * 0.5f,
    y = (first.y + second.y) * 0.5f
)

private fun computeWatermarkSubjectOutlineCenterCropSourceRect(
    bitmapWidth: Int,
    bitmapHeight: Int,
    destinationRect: RectF
): RectF {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || destinationRect.width() <= 0f || destinationRect.height() <= 0f) {
        return RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())
    }
    val sourceAspect = bitmapWidth.toFloat() / bitmapHeight.toFloat()
    val destinationAspect = destinationRect.width() / destinationRect.height()
    return if (sourceAspect > destinationAspect) {
        val croppedWidth = bitmapHeight * destinationAspect
        val left = (bitmapWidth - croppedWidth) * 0.5f
        RectF(left, 0f, left + croppedWidth, bitmapHeight.toFloat())
    } else {
        val croppedHeight = bitmapWidth / destinationAspect
        val top = (bitmapHeight - croppedHeight) * 0.5f
        RectF(0f, top, bitmapWidth.toFloat(), top + croppedHeight)
    }
}

private fun computeWatermarkSubjectOutlineArea(
    points: List<WatermarkOutlinePoint>
): Float {
    if (points.size < 3) return 0f
    var doubledArea = 0f
    points.forEachIndexed { index, current ->
        val next = points[(index + 1) % points.size]
        doubledArea += current.x * next.y.toFloat() - next.x * current.y.toFloat()
    }
    return doubledArea * 0.5f
}

private fun packWatermarkOutlinePoint(point: WatermarkOutlinePoint): Long =
    (point.x.toLong() shl 32) or (point.y.toLong() and 0xFFFFFFFFL)

private fun packWatermarkOutlineEdge(
    start: WatermarkOutlinePoint,
    end: WatermarkOutlinePoint
): Long =
    (start.x.toLong() shl 48) or
        ((start.y.toLong() and 0xFFFFL) shl 32) or
        ((end.x.toLong() and 0xFFFFL) shl 16) or
        (end.y.toLong() and 0xFFFFL)

private fun unpackWatermarkOutlineEdgeStart(edgeKey: Long): WatermarkOutlinePoint =
    WatermarkOutlinePoint(
        x = ((edgeKey ushr 48) and 0xFFFFL).toInt(),
        y = ((edgeKey ushr 32) and 0xFFFFL).toInt()
    )

private fun unpackWatermarkOutlineEdgeEnd(edgeKey: Long): WatermarkOutlinePoint =
    WatermarkOutlinePoint(
        x = ((edgeKey ushr 16) and 0xFFFFL).toInt(),
        y = (edgeKey and 0xFFFFL).toInt()
    )
