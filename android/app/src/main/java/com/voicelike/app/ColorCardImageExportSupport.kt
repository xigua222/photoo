package com.voicelike.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val COLOR_CARD_EXPORT_IMAGE_WIDTH = 1440
private const val COLOR_CARD_EXPORT_IMAGE_HEIGHT = 2560
private const val COLOR_CARD_EXPORT_REFERENCE_SCREEN_WIDTH_DP = 360f
private val COLOR_CARD_EXPORT_SCALE =
    COLOR_CARD_EXPORT_IMAGE_WIDTH.toFloat() / COLOR_CARD_EXPORT_REFERENCE_SCREEN_WIDTH_DP
private val COLOR_CARD_EXPORT_IMAGE_MARGIN = colorCardExportDesignPx(34f)
private val COLOR_CARD_EXPORT_IMAGE_CONTENT_WIDTH =
    COLOR_CARD_EXPORT_IMAGE_WIDTH.toFloat() - (COLOR_CARD_EXPORT_IMAGE_MARGIN * 2f)
private val COLOR_CARD_EXPORT_IMAGE_LOGO_WIDTH = colorCardExportDesignPx(30f)
private val COLOR_CARD_EXPORT_IMAGE_LOGO_MARGIN = colorCardExportDesignPx(24f)
private val COLOR_CARD_EXPORT_DETAIL_VERTICAL_PADDING = colorCardExportDesignPx(118f)
private val COLOR_CARD_EXPORT_DETAIL_BLOCK_SPACING = colorCardExportDesignPx(24f)
private val COLOR_CARD_EXPORT_DETAIL_BOTTOM_SPACER = colorCardExportDesignPx(92f)
private val COLOR_CARD_EXPORT_DETAIL_ICON_SIZE = colorCardExportDesignPx(17f)
private val COLOR_CARD_EXPORT_DETAIL_ICON_LABEL_GAP = colorCardExportDesignPx(8f)
private val COLOR_CARD_EXPORT_DETAIL_VALUE_TOP_GAP = colorCardExportDesignPx(6f)
private const val COLOR_CARD_EXPORT_IMAGE_FOLDER = "Pictures/Photoo/ColorCard"

internal data class ColorCardExportHeader(
    val eyebrow: String?,
    val title: String?,
    val useChineseFont: Boolean
)

internal sealed interface ColorCardImageExportSpec {
    val header: ColorCardExportHeader?

    data class All(
        val colors: List<Int>,
        override val header: ColorCardExportHeader?
    ) : ColorCardImageExportSpec

    data class Single(
        val colorInt: Int,
        val colorIndex: Int,
        val totalColors: Int,
        override val header: ColorCardExportHeader?
    ) : ColorCardImageExportSpec

    data class Gradient(
        val gradientSpec: ColorCardGradientSpec,
        override val header: ColorCardExportHeader?
    ) : ColorCardImageExportSpec
}

@Volatile
private var cachedColorCardExportLogoBitmap: Bitmap? = null

internal suspend fun saveColorCardExportImage(
    context: Context,
    spec: ColorCardImageExportSpec
): Uri? = withContext(Dispatchers.IO) {
    val bitmap = renderColorCardExportBitmap(context = context, spec = spec)
    try {
        saveColorCardBitmapToGallery(
            context = context,
            bitmap = bitmap,
            fileName = buildColorCardExportImageFileName(spec)
        )
    } finally {
        bitmap.recycle()
    }
}

private data class ColorCardExportMetricItem(
    val label: String,
    val value: String,
    val icon: ColorCardExportMetricIcon
)

private enum class ColorCardExportMetricIcon {
    Blend,
    VectorSquare,
    Rainbow,
    Contrast
}

private enum class ColorCardExportTextWeight(val value: Int) {
    Regular(400),
    Medium(500),
    SemiBold(600)
}

internal suspend fun saveColorCardRenderedBitmap(
    context: Context,
    bitmap: Bitmap,
    baseName: String,
    sourceUri: Uri? = null
): Uri? = withContext(Dispatchers.IO) {
    saveColorCardBitmapToGallery(
        context = context,
        bitmap = bitmap,
        fileName = buildColorCardRenderedImageFileName(baseName),
        sourceUri = sourceUri
    )
}

private fun renderColorCardExportBitmap(
    context: Context,
    spec: ColorCardImageExportSpec
): Bitmap {
    val bitmap = Bitmap.createBitmap(
        COLOR_CARD_EXPORT_IMAGE_WIDTH,
        COLOR_CARD_EXPORT_IMAGE_HEIGHT,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    when (spec) {
        is ColorCardImageExportSpec.All -> drawAllColorCardExport(canvas = canvas, colors = spec.colors)
        is ColorCardImageExportSpec.Single -> {
            drawSingleColorCardExport(
                context = context,
                canvas = canvas,
                header = spec.header,
                colorInt = spec.colorInt,
                colorIndex = spec.colorIndex,
                totalColors = spec.totalColors
            )
        }
        is ColorCardImageExportSpec.Gradient -> {
            drawGradientColorCardExport(
                canvas = canvas,
                gradientSpec = spec.gradientSpec
            )
        }
    }
    if (spec !is ColorCardImageExportSpec.Gradient) {
        drawColorCardExportLogo(context = context, canvas = canvas)
    }
    return bitmap
}

private fun drawAllColorCardExport(
    canvas: Canvas,
    colors: List<Int>
) {
    val safeColors = colors.ifEmpty { listOf(0xFFBBBBBB.toInt()) }
    val blockHeight = COLOR_CARD_EXPORT_IMAGE_HEIGHT.toFloat() / safeColors.size
    val hexTextSize = when {
        safeColors.size >= 6 -> colorCardExportDesignPx(24f)
        else -> colorCardExportDesignPx(30f)
    }

    safeColors.forEachIndexed { index, colorInt ->
        val top = index * blockHeight
        val bottom = if (index == safeColors.lastIndex) {
            COLOR_CARD_EXPORT_IMAGE_HEIGHT.toFloat()
        } else {
            top + blockHeight
        }
        val blockRect = RectF(0f, top, COLOR_CARD_EXPORT_IMAGE_WIDTH.toFloat(), bottom)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colorInt
        }
        canvas.drawRect(blockRect, fillPaint)
        if (Color(colorInt).luminance() >= 0.985f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = AndroidColor.argb(52, 10, 10, 10)
            }
            canvas.drawRect(blockRect, borderPaint)
        }

        val textColor = colorCardContentColor(colorInt).toArgb()
        val textPaint = buildExportTextPaint(
            color = textColor,
            textSize = hexTextSize,
            weight = ColorCardExportTextWeight.SemiBold
        )
        val text = "#${colorCardMetrics(colorInt).hex}"
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val centerX = blockRect.centerX()
        val centerY = blockRect.centerY() - textBounds.exactCenterY()
        canvas.drawText(
            text,
            centerX - (textPaint.measureText(text) / 2f),
            centerY,
            textPaint
        )
    }
}

private fun drawSingleColorCardExport(
    context: Context,
    canvas: Canvas,
    header: ColorCardExportHeader?,
    colorInt: Int,
    colorIndex: Int,
    totalColors: Int
) {
    canvas.drawColor(colorInt)
    val contentTextColor = colorCardContentColor(colorInt).toArgb()
    val secondaryTextColor = colorCardExportWithAlpha(contentTextColor, 153)
    val metrics = colorCardMetrics(colorInt)
    val metricItems = listOf(
        ColorCardExportMetricItem(
            label = "CMYK",
            value = metrics.cmyk,
            icon = ColorCardExportMetricIcon.Blend
        ),
        ColorCardExportMetricItem(
            label = "RGB",
            value = metrics.rgb,
            icon = ColorCardExportMetricIcon.VectorSquare
        ),
        ColorCardExportMetricItem(
            label = "HSB",
            value = metrics.hsb,
            icon = ColorCardExportMetricIcon.Rainbow
        ),
        ColorCardExportMetricItem(
            label = "HEX",
            value = "#${metrics.hex}",
            icon = ColorCardExportMetricIcon.Contrast
        )
    )
    val fallbackHeaderText = "${colorIndex + 1} / $totalColors"
    val contentHeight = measureColorCardSingleExportContentHeight(
        context = context,
        header = header,
        fallbackText = fallbackHeaderText,
        contentTextColor = contentTextColor,
        secondaryTextColor = secondaryTextColor,
        metricItems = metricItems
    )
    var currentY = max(
        COLOR_CARD_EXPORT_DETAIL_VERTICAL_PADDING,
        COLOR_CARD_EXPORT_IMAGE_HEIGHT.toFloat() - COLOR_CARD_EXPORT_DETAIL_VERTICAL_PADDING - contentHeight
    )
    currentY = drawColorCardExportHeader(
        context = context,
        canvas = canvas,
        header = header,
        fallbackText = fallbackHeaderText,
        contentTextColor = contentTextColor,
        secondaryTextColor = secondaryTextColor,
        startY = currentY
    )
    if (metricItems.isNotEmpty()) {
        currentY += COLOR_CARD_EXPORT_DETAIL_BLOCK_SPACING
    }
    metricItems.forEachIndexed { index, item ->
        currentY += drawColorCardExportMetricBlock(
            canvas = canvas,
            item = item,
            contentTextColor = contentTextColor,
            secondaryTextColor = secondaryTextColor,
            top = currentY
        )
        if (index < metricItems.lastIndex) {
            currentY += COLOR_CARD_EXPORT_DETAIL_BLOCK_SPACING
        }
    }
}

private fun drawGradientColorCardExport(
    canvas: Canvas,
    gradientSpec: ColorCardGradientSpec
) {
    val normalizedSpec = normalizeColorCardGradientSpec(gradientSpec)
    val shader = LinearGradient(
        colorCardGradientStartPoint(
            width = COLOR_CARD_EXPORT_IMAGE_WIDTH.toFloat(),
            height = COLOR_CARD_EXPORT_IMAGE_HEIGHT.toFloat(),
            angleDegrees = normalizedSpec.angleDegrees
        ).first,
        colorCardGradientStartPoint(
            width = COLOR_CARD_EXPORT_IMAGE_WIDTH.toFloat(),
            height = COLOR_CARD_EXPORT_IMAGE_HEIGHT.toFloat(),
            angleDegrees = normalizedSpec.angleDegrees
        ).second,
        colorCardGradientEndPoint(
            width = COLOR_CARD_EXPORT_IMAGE_WIDTH.toFloat(),
            height = COLOR_CARD_EXPORT_IMAGE_HEIGHT.toFloat(),
            angleDegrees = normalizedSpec.angleDegrees
        ).first,
        colorCardGradientEndPoint(
            width = COLOR_CARD_EXPORT_IMAGE_WIDTH.toFloat(),
            height = COLOR_CARD_EXPORT_IMAGE_HEIGHT.toFloat(),
            angleDegrees = normalizedSpec.angleDegrees
        ).second,
        normalizedSpec.stops.map { it.colorInt }.toIntArray(),
        normalizedSpec.stops.map { it.positionPercent.coerceIn(0f, 100f) / 100f }.toFloatArray(),
        Shader.TileMode.CLAMP
    )
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
    }
    canvas.drawRect(
        0f,
        0f,
        COLOR_CARD_EXPORT_IMAGE_WIDTH.toFloat(),
        COLOR_CARD_EXPORT_IMAGE_HEIGHT.toFloat(),
        backgroundPaint
    )
}

private fun drawColorCardExportLogo(
    context: Context,
    canvas: Canvas
) {
    val logoBitmap = loadColorCardExportLogoBitmap(context) ?: return
    val targetWidth = COLOR_CARD_EXPORT_IMAGE_LOGO_WIDTH
    val targetHeight = targetWidth * (logoBitmap.height.toFloat() / logoBitmap.width.toFloat())
    val left = COLOR_CARD_EXPORT_IMAGE_WIDTH - COLOR_CARD_EXPORT_IMAGE_LOGO_MARGIN - targetWidth
    val top = COLOR_CARD_EXPORT_IMAGE_HEIGHT - COLOR_CARD_EXPORT_IMAGE_LOGO_MARGIN - targetHeight
    val logoRect = RectF(left, top, left + targetWidth, top + targetHeight)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 232
    }
    canvas.drawBitmap(logoBitmap, null, logoRect, paint)
}

private fun loadColorCardExportLogoBitmap(context: Context): Bitmap? {
    cachedColorCardExportLogoBitmap?.let { return it }
    synchronized(ColorCardImageExportSupport::class.java) {
        cachedColorCardExportLogoBitmap?.let { return it }
        val loaded = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.color_card_export_logo)
        }.getOrNull()
        cachedColorCardExportLogoBitmap = loaded
        return loaded
    }
}

private object ColorCardImageExportSupport

private fun measureColorCardSingleExportContentHeight(
    context: Context,
    header: ColorCardExportHeader?,
    fallbackText: String?,
    contentTextColor: Int,
    secondaryTextColor: Int,
    metricItems: List<ColorCardExportMetricItem>
): Float {
    var totalHeight = measureColorCardExportHeaderHeight(
        context = context,
        header = header,
        fallbackText = fallbackText,
        contentTextColor = contentTextColor,
        secondaryTextColor = secondaryTextColor
    )
    if (metricItems.isNotEmpty()) {
        if (totalHeight > 0f) {
            totalHeight += COLOR_CARD_EXPORT_DETAIL_BLOCK_SPACING
        }
        totalHeight += metricItems.sumOf { item ->
            measureColorCardExportMetricBlockHeight(
                item = item,
                contentTextColor = contentTextColor,
                secondaryTextColor = secondaryTextColor
            ).toDouble()
        }.toFloat()
        totalHeight += COLOR_CARD_EXPORT_DETAIL_BLOCK_SPACING * (metricItems.size - 1).coerceAtLeast(0)
    }
    totalHeight += COLOR_CARD_EXPORT_DETAIL_BOTTOM_SPACER
    return totalHeight
}

private fun drawColorCardExportHeader(
    context: Context,
    canvas: Canvas,
    header: ColorCardExportHeader?,
    fallbackText: String?,
    contentTextColor: Int,
    secondaryTextColor: Int,
    startY: Float
): Float {
    var currentY = startY

    header?.eyebrow?.takeIf { it.isNotBlank() }?.let { eyebrow ->
        val eyebrowPaint = buildExportTextPaint(
            color = secondaryTextColor,
            textSize = colorCardExportDesignPx(14f),
            weight = ColorCardExportTextWeight.Medium
        )
        currentY += drawParagraph(
            canvas = canvas,
            text = eyebrow,
            paint = eyebrowPaint,
            left = COLOR_CARD_EXPORT_IMAGE_MARGIN,
            top = currentY,
            width = COLOR_CARD_EXPORT_IMAGE_CONTENT_WIDTH
        )
        currentY += COLOR_CARD_EXPORT_DETAIL_BLOCK_SPACING
    }

    val title = header?.title?.takeIf { it.isNotBlank() } ?: fallbackText
    title?.let {
        val typeface = when {
            header?.useChineseFont == true -> loadColorCardChineseTypeface(context)
            shouldUseColorCardLeiko(it) -> loadColorCardLeikoTypeface(context)
            else -> null
        }
        val titlePaint = buildExportTextPaint(
            color = if (header?.title?.isNotBlank() == true) {
                contentTextColor
            } else {
                secondaryTextColor
            },
            textSize = if (header?.title?.isNotBlank() != true) {
                colorCardExportDesignPx(15f)
            } else if (header.useChineseFont) {
                colorCardExportDesignPx(34f)
            } else {
                colorCardExportDesignPx(30f)
            },
            weight = if (header?.title?.isNotBlank() != true || header.useChineseFont) {
                ColorCardExportTextWeight.Medium
            } else {
                ColorCardExportTextWeight.SemiBold
            },
            typeface = typeface
        )
        currentY += drawParagraph(
            canvas = canvas,
            text = it,
            paint = titlePaint,
            left = COLOR_CARD_EXPORT_IMAGE_MARGIN,
            top = currentY,
            width = COLOR_CARD_EXPORT_IMAGE_CONTENT_WIDTH
        )
    }

    return currentY
}

private fun measureColorCardExportHeaderHeight(
    context: Context,
    header: ColorCardExportHeader?,
    fallbackText: String?,
    contentTextColor: Int,
    secondaryTextColor: Int
): Float {
    var totalHeight = 0f
    header?.eyebrow?.takeIf { it.isNotBlank() }?.let { eyebrow ->
        val eyebrowPaint = buildExportTextPaint(
            color = secondaryTextColor,
            textSize = colorCardExportDesignPx(14f),
            weight = ColorCardExportTextWeight.Medium
        )
        totalHeight += measureParagraphHeight(
            text = eyebrow,
            paint = eyebrowPaint,
            width = COLOR_CARD_EXPORT_IMAGE_CONTENT_WIDTH
        )
        totalHeight += COLOR_CARD_EXPORT_DETAIL_BLOCK_SPACING
    }
    val title = header?.title?.takeIf { it.isNotBlank() } ?: fallbackText
    title?.let {
        val typeface = when {
            header?.useChineseFont == true -> loadColorCardChineseTypeface(context)
            shouldUseColorCardLeiko(it) -> loadColorCardLeikoTypeface(context)
            else -> null
        }
        val titlePaint = buildExportTextPaint(
            color = if (header?.title?.isNotBlank() == true) {
                contentTextColor
            } else {
                secondaryTextColor
            },
            textSize = if (header?.title?.isNotBlank() != true) {
                colorCardExportDesignPx(15f)
            } else if (header.useChineseFont) {
                colorCardExportDesignPx(34f)
            } else {
                colorCardExportDesignPx(30f)
            },
            weight = if (header?.title?.isNotBlank() != true || header.useChineseFont) {
                ColorCardExportTextWeight.Medium
            } else {
                ColorCardExportTextWeight.SemiBold
            },
            typeface = typeface
        )
        totalHeight += measureParagraphHeight(
            text = it,
            paint = titlePaint,
            width = COLOR_CARD_EXPORT_IMAGE_CONTENT_WIDTH
        )
    }
    return totalHeight
}

private fun drawColorCardExportMetricBlock(
    canvas: Canvas,
    item: ColorCardExportMetricItem,
    contentTextColor: Int,
    secondaryTextColor: Int,
    top: Float
): Float {
    val labelPaint = buildExportTextPaint(
        color = secondaryTextColor,
        textSize = colorCardExportDesignPx(18f),
        weight = ColorCardExportTextWeight.Medium
    )
    val valuePaint = buildExportTextPaint(
        color = contentTextColor,
        textSize = colorCardExportDesignPx(28f),
        weight = ColorCardExportTextWeight.SemiBold
    )
    val labelRowHeight = max(
        COLOR_CARD_EXPORT_DETAIL_ICON_SIZE,
        labelPaint.fontMetrics.run { bottom - top }
    )
    val iconRect = RectF(
        COLOR_CARD_EXPORT_IMAGE_MARGIN,
        top + ((labelRowHeight - COLOR_CARD_EXPORT_DETAIL_ICON_SIZE) * 0.5f),
        COLOR_CARD_EXPORT_IMAGE_MARGIN + COLOR_CARD_EXPORT_DETAIL_ICON_SIZE,
        top + ((labelRowHeight - COLOR_CARD_EXPORT_DETAIL_ICON_SIZE) * 0.5f) +
            COLOR_CARD_EXPORT_DETAIL_ICON_SIZE
    )
    drawColorCardExportMetricIcon(
        canvas = canvas,
        icon = item.icon,
        rect = iconRect,
        color = secondaryTextColor
    )
    val labelBaseline = top + ((labelRowHeight - (labelPaint.fontMetrics.bottom - labelPaint.fontMetrics.top)) * 0.5f) - labelPaint.fontMetrics.top
    canvas.drawText(
        item.label,
        iconRect.right + COLOR_CARD_EXPORT_DETAIL_ICON_LABEL_GAP,
        labelBaseline,
        labelPaint
    )
    val valueTop = top + labelRowHeight + COLOR_CARD_EXPORT_DETAIL_VALUE_TOP_GAP
    val valueHeight = drawParagraph(
        canvas = canvas,
        text = item.value,
        paint = valuePaint,
        left = COLOR_CARD_EXPORT_IMAGE_MARGIN,
        top = valueTop,
        width = COLOR_CARD_EXPORT_IMAGE_CONTENT_WIDTH
    )
    return labelRowHeight + COLOR_CARD_EXPORT_DETAIL_VALUE_TOP_GAP + valueHeight
}

private fun measureColorCardExportMetricBlockHeight(
    item: ColorCardExportMetricItem,
    contentTextColor: Int,
    secondaryTextColor: Int
): Float {
    val labelPaint = buildExportTextPaint(
        color = secondaryTextColor,
        textSize = colorCardExportDesignPx(18f),
        weight = ColorCardExportTextWeight.Medium
    )
    val valuePaint = buildExportTextPaint(
        color = contentTextColor,
        textSize = colorCardExportDesignPx(28f),
        weight = ColorCardExportTextWeight.SemiBold
    )
    val labelRowHeight = max(
        COLOR_CARD_EXPORT_DETAIL_ICON_SIZE,
        labelPaint.fontMetrics.run { bottom - top }
    )
    return labelRowHeight + COLOR_CARD_EXPORT_DETAIL_VALUE_TOP_GAP + measureParagraphHeight(
        text = item.value,
        paint = valuePaint,
        width = COLOR_CARD_EXPORT_IMAGE_CONTENT_WIDTH
    )
}

private fun buildExportTextPaint(
    color: Int,
    textSize: Float,
    weight: ColorCardExportTextWeight = ColorCardExportTextWeight.Regular,
    typeface: Typeface? = null
): TextPaint {
    val baseTypeface = typeface ?: Typeface.SANS_SERIF
    return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        this.typeface = resolveColorCardExportTypeface(baseTypeface, weight)
        isFakeBoldText = Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
            weight != ColorCardExportTextWeight.Regular
        clearShadowLayer()
    }
}

private fun resolveColorCardExportTypeface(
    baseTypeface: Typeface,
    weight: ColorCardExportTextWeight
): Typeface {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Typeface.create(baseTypeface, weight.value, false)
    } else {
        Typeface.create(
            baseTypeface,
            if (weight == ColorCardExportTextWeight.Regular) Typeface.NORMAL else Typeface.BOLD
        )
    }
}

private fun colorCardExportWithAlpha(
    color: Int,
    alpha: Int
): Int = AndroidColor.argb(
    alpha.coerceIn(0, 255),
    AndroidColor.red(color),
    AndroidColor.green(color),
    AndroidColor.blue(color)
)

private fun drawColorCardExportMetricIcon(
    canvas: Canvas,
    icon: ColorCardExportMetricIcon,
    rect: RectF,
    color: Int
) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (min(rect.width(), rect.height()) * (2f / 24f)).coerceAtLeast(1.5f)
        this.color = color
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun px(value: Float): Float = rect.left + (rect.width() * (value / 24f))
    fun py(value: Float): Float = rect.top + (rect.height() * (value / 24f))
    fun drawCircle(centerX: Float, centerY: Float, radius: Float) {
        canvas.drawCircle(px(centerX), py(centerY), rect.width() * (radius / 24f), strokePaint)
    }

    when (icon) {
        ColorCardExportMetricIcon.Blend -> {
            drawCircle(centerX = 9f, centerY = 9f, radius = 7f)
            drawCircle(centerX = 15f, centerY = 15f, radius = 7f)
        }
        ColorCardExportMetricIcon.Contrast -> {
            drawCircle(centerX = 12f, centerY = 12f, radius = 10f)
            val halfPath = Path().apply {
                moveTo(px(12f), py(18f))
                arcTo(
                    RectF(px(6f), py(6f), px(18f), py(18f)),
                    90f,
                    -180f,
                    false
                )
                lineTo(px(12f), py(6f))
                lineTo(px(12f), py(18f))
                close()
            }
            canvas.drawPath(halfPath, strokePaint)
        }
        ColorCardExportMetricIcon.Rainbow -> {
            canvas.drawArc(RectF(px(2f), py(7f), px(22f), py(27f)), 180f, 180f, false, strokePaint)
            canvas.drawArc(RectF(px(6f), py(11f), px(18f), py(23f)), 180f, 180f, false, strokePaint)
            canvas.drawArc(RectF(px(10f), py(15f), px(14f), py(19f)), 180f, 180f, false, strokePaint)
        }
        ColorCardExportMetricIcon.VectorSquare -> {
            val cornerRadius = rect.width() * (1f / 24f)
            canvas.drawRoundRect(RectF(px(17f), py(17f), px(22f), py(22f)), cornerRadius, cornerRadius, strokePaint)
            canvas.drawRoundRect(RectF(px(17f), py(2f), px(22f), py(7f)), cornerRadius, cornerRadius, strokePaint)
            canvas.drawRoundRect(RectF(px(2f), py(17f), px(7f), py(22f)), cornerRadius, cornerRadius, strokePaint)
            canvas.drawRoundRect(RectF(px(2f), py(2f), px(7f), py(7f)), cornerRadius, cornerRadius, strokePaint)
            val topPath = Path().apply {
                moveTo(px(7f), py(4.5f))
                quadTo(px(12f), py(3f), px(17f), py(4.5f))
            }
            val bottomPath = Path().apply {
                moveTo(px(7f), py(19.5f))
                quadTo(px(12f), py(21f), px(17f), py(19.5f))
            }
            val leftPath = Path().apply {
                moveTo(px(4.5f), py(7f))
                quadTo(px(3f), py(12f), px(4.5f), py(17f))
            }
            val rightPath = Path().apply {
                moveTo(px(19.5f), py(7f))
                quadTo(px(21f), py(12f), px(19.5f), py(17f))
            }
            canvas.drawPath(topPath, strokePaint)
            canvas.drawPath(bottomPath, strokePaint)
            canvas.drawPath(leftPath, strokePaint)
            canvas.drawPath(rightPath, strokePaint)
        }
    }
}

private fun drawParagraph(
    canvas: Canvas,
    text: String,
    paint: TextPaint,
    left: Float,
    top: Float,
    width: Float
): Int {
    if (text.isBlank()) return 0
    val layout = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width.roundToInt().coerceAtLeast(1))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()
    canvas.save()
    canvas.translate(left, top)
    layout.draw(canvas)
    canvas.restore()
    return layout.height
}

private fun measureParagraphHeight(
    text: String,
    paint: TextPaint,
    width: Float
): Float {
    if (text.isBlank()) return 0f
    return StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width.roundToInt().coerceAtLeast(1))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()
        .height
        .toFloat()
}

private fun colorCardExportDesignPx(value: Float): Float = value * COLOR_CARD_EXPORT_SCALE

private fun colorCardGradientStartPoint(
    width: Float,
    height: Float,
    angleDegrees: Float
): Pair<Float, Float> {
    val centerX = width / 2f
    val centerY = height / 2f
    val radius = hypot(width.toDouble(), height.toDouble()).toFloat() / 2f
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = sin(radians).toFloat() * radius
    val dy = -cos(radians).toFloat() * radius
    return (centerX - dx) to (centerY - dy)
}

private fun colorCardGradientEndPoint(
    width: Float,
    height: Float,
    angleDegrees: Float
): Pair<Float, Float> {
    val centerX = width / 2f
    val centerY = height / 2f
    val radius = hypot(width.toDouble(), height.toDouble()).toFloat() / 2f
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = sin(radians).toFloat() * radius
    val dy = -cos(radians).toFloat() * radius
    return (centerX + dx) to (centerY + dy)
}

private fun buildColorCardExportImageFileName(
    spec: ColorCardImageExportSpec
): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val suffix = when (spec) {
        is ColorCardImageExportSpec.All -> "all"
        is ColorCardImageExportSpec.Single -> "single"
        is ColorCardImageExportSpec.Gradient -> "gradient"
    }
    return "photoo_color_card_${suffix}_$timestamp.png"
}

private fun buildColorCardRenderedImageFileName(
    baseName: String
): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val sanitizedBaseName = baseName
        .substringBeforeLast('.')
        .replace(Regex("[^A-Za-z0-9_-]+"), "_")
        .trim('_')
        .ifBlank { "image" }
    return "photoo_filter_${sanitizedBaseName}_$timestamp.png"
}

private fun saveColorCardBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    sourceUri: Uri? = null
): Uri? {
    val resolver = context.contentResolver
    val nowSeconds = System.currentTimeMillis() / 1000
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, COLOR_CARD_EXPORT_IMAGE_FOLDER)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    runCatching {
        resolver.openOutputStream(savedUri)?.use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw IllegalStateException("色卡输出失败")
            }
        } ?: throw IllegalStateException("输出流打开失败")
    }.onFailure {
        runCatching { resolver.delete(savedUri, null, null) }
        return null
    }
    if (sourceUri != null) {
        runCatching {
            copyPreservedExifMetadata(
                resolver = resolver,
                sourceUri = sourceUri,
                targetUri = savedUri
            )
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val pendingValues = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(savedUri, pendingValues, null, null)
    }
    return savedUri
}
