package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private const val WATERMARK_CHINESE_COLORS_ASSET = "watermark_chinese_colors.json"
private const val WATERMARK_CHINESE_COLOR_FONT_ASSET = "watermark_fonts/zhengqingke_chinese_colors_subset.ttf"
private const val WATERMARK_PALETTE_CACHE_MAX_ENTRIES = 6
private val WATERMARK_CHINESE_MONO_BLACK_SERIES_NAMES = setOf("獭见")
private val WATERMARK_CHINESE_MONO_BLACK_SERIES_KEYWORDS = listOf("黑", "玄", "墨", "乌", "煤", "黛", "皂", "缁")

@Volatile
private var cachedWatermarkChineseColors: List<WatermarkChineseColorEntry>? = null

private val cachedWatermarkChineseTypefaces = ConcurrentHashMap<String, Typeface>()
private val cachedWatermarkPalettesLock = Any()
private val cachedWatermarkPalettes =
    object : LinkedHashMap<String, WatermarkExtractedPalette>(WATERMARK_PALETTE_CACHE_MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, WatermarkExtractedPalette>?
        ): Boolean = size > WATERMARK_PALETTE_CACHE_MAX_ENTRIES
    }

internal enum class WatermarkColorPaletteStyle(
    val title: String,
    val subtitle: String
) {
    HexCard("色卡", "底部展示 5 色与 HEX"),
    EditorialStrip("刊页", "右侧竖栏排版"),
    PaletteNote("注记", "右侧色板与标题"),
    ChinesePalette("中国色其一", "匹配传统色谱"),
    ChinesePaletteMono("中国色其二", "单色边框题签")
    , MosaicGrid("马赛克矩形", "很多颜色，拼成一块")
    , CircleBands("圆形色带", "所有颜色，围成一个圆")
    , BottomGradientBar("底部渐变条", "所有颜色，在底部汇成渐变")
    , CenterStripes("横向色条", "颜色横向堆叠成一组色条")
    , CenterBlocks("块面拼贴", "小矩形一字排开，形成块面拼贴")
    , GlassDots("玻璃面板", "原图底部叠一块玻璃面板")
}

internal data class WatermarkColorPaletteVariantOption(
    val index: Int,
    val label: String
)

internal fun resolveWatermarkColorPaletteStyle(index: Int): WatermarkColorPaletteStyle {
    return WatermarkColorPaletteStyle.entries.getOrElse(
        index.coerceIn(0, WatermarkColorPaletteStyle.entries.lastIndex)
    ) { WatermarkColorPaletteStyle.HexCard }
}

internal fun resolveWatermarkColorPaletteStyleTitle(
    style: WatermarkColorPaletteStyle
): String = when (style) {
    WatermarkColorPaletteStyle.HexCard -> "色卡"
    WatermarkColorPaletteStyle.EditorialStrip -> "刊页"
    WatermarkColorPaletteStyle.PaletteNote -> "注记"
    WatermarkColorPaletteStyle.ChinesePalette -> "中国色其一"
    WatermarkColorPaletteStyle.ChinesePaletteMono -> "中国色其二"
    WatermarkColorPaletteStyle.MosaicGrid -> "马赛克矩形"
    WatermarkColorPaletteStyle.CircleBands -> "圆形色带"
    WatermarkColorPaletteStyle.BottomGradientBar -> "底部渐变条"
    WatermarkColorPaletteStyle.CenterStripes -> "横向色条"
    WatermarkColorPaletteStyle.CenterBlocks -> "块面拼贴"
    WatermarkColorPaletteStyle.GlassDots -> "玻璃面板"
}

internal fun resolveWatermarkColorPaletteStyleSubtitle(
    style: WatermarkColorPaletteStyle
): String = when (style) {
    WatermarkColorPaletteStyle.HexCard -> "底部展示 5 色与 HEX"
    WatermarkColorPaletteStyle.EditorialStrip -> "右侧竖栏排版"
    WatermarkColorPaletteStyle.PaletteNote -> "右侧色板与标题"
    WatermarkColorPaletteStyle.ChinesePalette -> "匹配传统中国色"
    WatermarkColorPaletteStyle.ChinesePaletteMono -> "单色边框题签"
    WatermarkColorPaletteStyle.MosaicGrid -> "很多颜色，拼成一块"
    WatermarkColorPaletteStyle.CircleBands -> "所有颜色，围成一个圆"
    WatermarkColorPaletteStyle.BottomGradientBar -> "所有颜色，在底部汇成渐变"
    WatermarkColorPaletteStyle.CenterStripes -> "颜色横向堆叠成一组色条"
    WatermarkColorPaletteStyle.CenterBlocks -> "块面拼贴"
    WatermarkColorPaletteStyle.GlassDots -> "玻璃面板"
}

internal fun resolveWatermarkColorPaletteVariantOptions(
    style: WatermarkColorPaletteStyle
): List<WatermarkColorPaletteVariantOption> = when (style) {
    WatermarkColorPaletteStyle.HexCard -> listOf(
        WatermarkColorPaletteVariantOption(0, "默认"),
        WatermarkColorPaletteVariantOption(1, "矩形"),
        WatermarkColorPaletteVariantOption(2, "爱心"),
        WatermarkColorPaletteVariantOption(3, "圆形"),
        WatermarkColorPaletteVariantOption(4, "四边形")
    )
    WatermarkColorPaletteStyle.EditorialStrip -> listOf(
        WatermarkColorPaletteVariantOption(0, "默认"),
        WatermarkColorPaletteVariantOption(1, "无边框")
    )
    WatermarkColorPaletteStyle.CircleBands -> listOf(
        WatermarkColorPaletteVariantOption(0, "连体"),
        WatermarkColorPaletteVariantOption(1, "断开")
    )
    WatermarkColorPaletteStyle.CenterStripes -> listOf(
        WatermarkColorPaletteVariantOption(0, "连体"),
        WatermarkColorPaletteVariantOption(1, "断开")
    )
    WatermarkColorPaletteStyle.CenterBlocks -> listOf(
        WatermarkColorPaletteVariantOption(0, "方块"),
        WatermarkColorPaletteVariantOption(1, "矩形"),
        WatermarkColorPaletteVariantOption(2, "爱心")
    )
    WatermarkColorPaletteStyle.GlassDots -> listOf(
        WatermarkColorPaletteVariantOption(0, "清透单行"),
        WatermarkColorPaletteVariantOption(1, "清透双行"),
        WatermarkColorPaletteVariantOption(2, "珍珠单行"),
        WatermarkColorPaletteVariantOption(3, "珍珠双行"),
        WatermarkColorPaletteVariantOption(4, "雾灰单行"),
        WatermarkColorPaletteVariantOption(5, "雾灰双行"),
        WatermarkColorPaletteVariantOption(6, "黑曜单行"),
        WatermarkColorPaletteVariantOption(7, "黑曜双行"),
        WatermarkColorPaletteVariantOption(8, "白瓷单行"),
        WatermarkColorPaletteVariantOption(9, "白瓷双行"),
        WatermarkColorPaletteVariantOption(10, "海蓝单行"),
        WatermarkColorPaletteVariantOption(11, "海蓝双行"),
        WatermarkColorPaletteVariantOption(12, "玫瑰单行"),
        WatermarkColorPaletteVariantOption(13, "玫瑰双行")
    )
    else -> emptyList()
}

internal fun resolveWatermarkColorTemplateCropAspectRatio(
    style: WatermarkColorPaletteStyle,
    outputWidth: Int,
    outputHeight: Int,
    variantIndex: Int = 0,
    showHexValue: Boolean = true,
    showRgbValue: Boolean = true
): Float {
    val width = outputWidth.coerceAtLeast(1).toFloat()
    val height = outputHeight.coerceAtLeast(1).toFloat()
    val rect = when (style) {
        WatermarkColorPaletteStyle.HexCard -> {
            val layout = resolveHexCardLayoutMetrics(
                width = outputWidth,
                height = outputHeight,
                swatchShape = resolveWatermarkPaletteBlockShape(variantIndex),
                showHexValue = showHexValue,
                showRgbValue = showRgbValue
            )
            RectF(
                width * 0.08f,
                height * 0.06f,
                width * 0.92f,
                (height - layout.bottomSectionHeight).coerceAtLeast(height * 0.60f)
            )
        }
        WatermarkColorPaletteStyle.EditorialStrip -> RectF(
            0f,
            0f,
            width * 0.72f,
            height
        )
        WatermarkColorPaletteStyle.PaletteNote -> RectF(
            width * 0.055f,
            height * 0.07f,
            width * 0.78f,
            height * 0.93f
        )
        WatermarkColorPaletteStyle.ChinesePalette -> RectF(
            width * 0.06f,
            height * 0.07f,
            width * 0.67f,
            height * 0.93f
        )
        WatermarkColorPaletteStyle.ChinesePaletteMono -> RectF(
            width * 0.065f,
            height * 0.065f,
            width * 0.73f,
            height * 0.935f
        )
        WatermarkColorPaletteStyle.BottomGradientBar -> RectF(
            0f,
            0f,
            width,
            (height - max(height * 0.145f, width * 0.065f)).coerceAtLeast(1f)
        )
        WatermarkColorPaletteStyle.MosaicGrid,
        WatermarkColorPaletteStyle.CircleBands,
        WatermarkColorPaletteStyle.CenterStripes,
        WatermarkColorPaletteStyle.CenterBlocks,
        WatermarkColorPaletteStyle.GlassDots -> RectF(
            0f,
            0f,
            width,
            height
        )
    }
    val aspect = rect.width() / rect.height().coerceAtLeast(1f)
    return if (aspect.isFinite() && aspect > 0f) aspect else width / height
}

private data class WatermarkHexCardLayoutMetrics(
    val outerPad: Float,
    val swatchGap: Float,
    val slotWidth: Float,
    val swatchSize: WatermarkPaletteBlockDimensions,
    val swatchTopOffset: Float,
    val bottomPadding: Float,
    val hexLabelHeight: Float,
    val rgbLabelHeight: Float
) {
    val bottomSectionHeight: Float
        get() = swatchTopOffset + swatchSize.height + hexLabelHeight + rgbLabelHeight + bottomPadding
}

private fun resolveHexCardLayoutMetrics(
    width: Int,
    height: Int,
    swatchShape: WatermarkPaletteBlockShape,
    showHexValue: Boolean,
    showRgbValue: Boolean,
    swatchCount: Int = 5
): WatermarkHexCardLayoutMetrics {
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val baseDim = max(safeWidth, safeHeight).toFloat()
    val outerPad = safeWidth * 0.08f
    val swatchGap = safeWidth * 0.03f
    val slotWidth = (
        (safeWidth - outerPad * 2f - swatchGap * (swatchCount - 1)) / swatchCount
        ).coerceAtLeast(8f)
    val preferredSwatchWidth = (slotWidth * 0.82f).coerceAtLeast(8f)
    val swatchMaxHeight = when (swatchShape) {
        WatermarkPaletteBlockShape.Rectangle -> safeHeight * 0.15f
        WatermarkPaletteBlockShape.Heart -> safeHeight * 0.11f
        else -> safeHeight * 0.10f
    }
    val swatchSize = resolveWatermarkPaletteBlockDimensions(
        shape = swatchShape,
        preferredWidth = preferredSwatchWidth,
        maxHeight = swatchMaxHeight
    )
    val swatchTopOffset = if (showHexValue || showRgbValue) {
        max(safeHeight * 0.032f, 14f)
    } else {
        max(safeHeight * 0.044f, 16f)
    }
    val hexLabelHeight = if (showHexValue) {
        max(safeHeight * 0.018f, (baseDim * 0.024f).coerceAtLeast(10.5f) * 0.98f)
    } else {
        0f
    }
    val rgbBaseHeight = (baseDim * 0.0175f).coerceAtLeast(9f)
    val rgbLabelHeight = if (showRgbValue) {
        if (showHexValue) {
            max(safeHeight * 0.010f, rgbBaseHeight * 0.92f)
        } else {
            max(safeHeight * 0.018f, rgbBaseHeight * 0.98f)
        }
    } else {
        0f
    }
    val bottomPadding = max(safeHeight * 0.018f, 12f)
    return WatermarkHexCardLayoutMetrics(
        outerPad = outerPad,
        swatchGap = swatchGap,
        slotWidth = slotWidth,
        swatchSize = swatchSize,
        swatchTopOffset = swatchTopOffset,
        bottomPadding = bottomPadding,
        hexLabelHeight = hexLabelHeight,
        rgbLabelHeight = rgbLabelHeight
    )
}

internal fun drawWatermarkColorPaletteTemplate(
    context: Context,
    source: Bitmap,
    style: WatermarkColorPaletteStyle,
    fixedOutputSize: Pair<Int, Int>? = null,
    variantIndex: Int = 0,
    refreshNonce: Int = 0,
    horizontalOffsetRatio: Float = 0f,
    verticalOffsetRatio: Float = 0f,
    itemCount: Int = 0,
    showHexValue: Boolean = true,
    showRgbValue: Boolean = true,
    paletteOverride: WatermarkExtractedPalette? = null,
    paletteCacheKey: String? = null
): Bitmap {
    val basePalette = paletteOverride ?: extractWatermarkPalette(source, cacheKey = paletteCacheKey)
    val palette = deriveRefreshedWatermarkPalette(basePalette, refreshNonce)
    return when (style) {
        WatermarkColorPaletteStyle.HexCard -> drawHexCardColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize,
            variantIndex = variantIndex,
            showHexValue = showHexValue,
            showRgbValue = showRgbValue
        )
        WatermarkColorPaletteStyle.EditorialStrip -> drawEditorialStripColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize,
            variantIndex = variantIndex
        )
        WatermarkColorPaletteStyle.PaletteNote -> drawPaletteNoteColorTemplate(source, palette, fixedOutputSize)
        WatermarkColorPaletteStyle.ChinesePalette -> drawChinesePaletteColorTemplate(
            context = context,
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize
        )
        WatermarkColorPaletteStyle.ChinesePaletteMono -> drawChineseMonoColorTemplate(
            context = context,
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize,
            monoVariantIndex = variantIndex
        )
        WatermarkColorPaletteStyle.MosaicGrid -> drawMosaicGridColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize
        )
        WatermarkColorPaletteStyle.CircleBands -> drawCircleBandsColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize,
            variantIndex = variantIndex,
            horizontalOffsetRatio = horizontalOffsetRatio,
            verticalOffsetRatio = verticalOffsetRatio,
            itemCount = itemCount
        )
        WatermarkColorPaletteStyle.BottomGradientBar -> drawBottomGradientBarColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize
        )
        WatermarkColorPaletteStyle.CenterStripes -> drawCenterStripesColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize,
            variantIndex = variantIndex,
            itemCount = itemCount
        )
        WatermarkColorPaletteStyle.CenterBlocks -> drawCenterBlocksColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize,
            variantIndex = variantIndex,
            itemCount = itemCount
        )
        WatermarkColorPaletteStyle.GlassDots -> drawGlassDotsColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize,
            variantIndex = variantIndex,
            itemCount = itemCount,
            showHexValue = showHexValue
        )
    }
}

internal fun drawWatermarkFullColorTemplate(
    source: Bitmap,
    exif: WatermarkExifInfo,
    showTime: Boolean,
    showLocation: Boolean,
    noteText: String = "",
    watermarkTypeface: Typeface?,
    textScaleFactor: Float = 0.88f,
    backgroundColorOverride: Int? = null,
    useGradientBackground: Boolean = false,
    gradientColorsOverride: Pair<Int, Int>? = null,
    imageOnTop: Boolean = false,
    horizontalOffsetRatio: Float = 0f,
    verticalOffsetRatio: Float = 0f,
    fixedOutputSize: Pair<Int, Int>? = null
): Bitmap {
    val width = fixedOutputSize?.first?.coerceAtLeast(1) ?: source.width.coerceAtLeast(1)
    val height = fixedOutputSize?.second?.coerceAtLeast(1) ?: source.height.coerceAtLeast(1)
    val palette = if (backgroundColorOverride == null) extractWatermarkPalette(source) else null
    val gradientMidColor = gradientColorsOverride?.let { (startColor, endColor) ->
        mixColors(startColor, endColor, 0.5f)
    }
    val background = backgroundColorOverride ?: gradientMidColor ?: resolveFullColorBackgroundColor(
        palette ?: extractWatermarkPalette(source)
    )
    val primaryText = palette
        ?.let { resolveFullColorPrimaryTextColor(background, it) }
        ?: contrastTextColor(background)
    val secondaryText = applyAlpha(
        primaryText,
        if (relativeLuminance(background) >= 0.58f) 170 else 196
    )
    val dividerColor = applyAlpha(mixColors(primaryText, background, 0.88f), 62)
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val resolvedTextScale = textScaleFactor.coerceIn(0.7f, 1.2f)
    val topHeight = height * 0.5f
    val upperRect = RectF(0f, 0f, width.toFloat(), topHeight)
    val lowerRect = RectF(0f, topHeight, width.toFloat(), height.toFloat())
    val photoRect = if (imageOnTop) upperRect else lowerRect
    val colorRect = if (imageOnTop) lowerRect else upperRect

    canvas.drawColor(background)
    if (useGradientBackground) {
        val (gradientStart, gradientEnd) = gradientColorsOverride ?: resolveFullColorBackgroundGradient(background, palette)
        canvas.drawRect(
            colorRect,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    colorRect.left,
                    colorRect.top,
                    colorRect.right,
                    colorRect.bottom,
                    gradientStart,
                    gradientEnd,
                    Shader.TileMode.CLAMP
                )
            }
        )
    } else {
        canvas.drawRect(colorRect, Paint().apply { color = background })
    }
    drawBitmapCenterCrop(
        canvas = canvas,
        source = source,
        dstRect = photoRect,
        horizontalOffsetRatio = horizontalOffsetRatio,
        verticalOffsetRatio = verticalOffsetRatio
    )
    canvas.drawRect(
        0f,
        lowerRect.top - max(height * 0.0012f, 1f),
        width.toFloat(),
        lowerRect.top + max(height * 0.0012f, 1f),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dividerColor }
    )

    val resolvedNoteText = noteText.trim()
    val locationText = if (resolvedNoteText.isBlank() && showLocation) resolveFullColorLocationLine(exif) else ""
    val timeText = if (resolvedNoteText.isBlank() && showTime) resolveFullColorTimeLine(exif.dateTimeOriginal) else ""
    if (resolvedNoteText.isBlank() && locationText.isBlank() && timeText.isBlank()) {
        return output
    }

    val baseDim = max(width, height).toFloat()
    if (resolvedNoteText.isNotBlank()) {
        drawFullColorNoteText(
            canvas = canvas,
            targetRect = colorRect,
            text = resolvedNoteText,
            textColor = primaryText,
            shadowColor = if (relativeLuminance(background) >= 0.58f) {
                applyAlpha(AndroidColor.BLACK, 26)
            } else {
                applyAlpha(AndroidColor.BLACK, 42)
            },
            preferredTypeface = watermarkTypeface,
            textScaleFactor = resolvedTextScale,
            baseDim = baseDim
        )
        return output
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textAlign = Paint.Align.CENTER
        typeface = resolveWatermarkTextTypeface(
            preferredTypeface = watermarkTypeface,
            text = locationText.ifBlank { timeText },
            bold = true
        )
        isFakeBoldText = true
        letterSpacing = 0.06f
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textAlign = Paint.Align.CENTER
        typeface = resolveWatermarkTextTypeface(
            preferredTypeface = watermarkTypeface,
            text = timeText.ifBlank { locationText },
            bold = true
        )
        isFakeBoldText = true
        letterSpacing = 0.045f
    }
    val textShadow = if (relativeLuminance(background) >= 0.58f) {
        applyAlpha(AndroidColor.BLACK, 26)
    } else {
        applyAlpha(AndroidColor.BLACK, 42)
    }
    titlePaint.setShadowLayer(max(baseDim * 0.0018f, 0.7f), 0f, max(baseDim * 0.0009f, 0.4f), textShadow)
    subtitlePaint.setShadowLayer(max(baseDim * 0.0013f, 0.5f), 0f, max(baseDim * 0.0007f, 0.3f), textShadow)

    val textMaxWidth = width * 0.68f
    fitTextSize(
        paint = titlePaint,
        text = locationText.ifBlank { timeText },
        maxWidth = textMaxWidth,
        minSize = max(colorRect.height() * 0.042f, 12f) * resolvedTextScale,
        maxSize = max(colorRect.height() * 0.082f, 18f) * resolvedTextScale
    )
    fitTextSize(
        paint = subtitlePaint,
        text = timeText.ifBlank { locationText },
        maxWidth = textMaxWidth,
        minSize = max(colorRect.height() * 0.024f, 9f) * resolvedTextScale,
        maxSize = max(colorRect.height() * 0.046f, 12f) * resolvedTextScale
    )

    val titleMetrics = titlePaint.fontMetrics
    val subtitleMetrics = subtitlePaint.fontMetrics
    val titleHeight = titleMetrics.descent - titleMetrics.ascent
    val subtitleHeight = subtitleMetrics.descent - subtitleMetrics.ascent
    val lineGap = max(colorRect.height() * 0.022f, 6f)
    val blockHeight = when {
        locationText.isNotBlank() && timeText.isNotBlank() -> titleHeight + subtitleHeight + lineGap
        locationText.isNotBlank() -> titleHeight
        else -> subtitleHeight
    }
    val blockTop = colorRect.top + (colorRect.height() - blockHeight) * 0.5f - colorRect.height() * 0.025f
    val centerX = colorRect.centerX()

    when {
        locationText.isNotBlank() && timeText.isNotBlank() -> {
            val locationBaseline = blockTop - titleMetrics.ascent
            val timeBaseline = locationBaseline + titleMetrics.descent + lineGap - subtitleMetrics.ascent
            canvas.drawText(locationText, centerX, locationBaseline, titlePaint)
            canvas.drawText(timeText, centerX, timeBaseline, subtitlePaint)
        }
        locationText.isNotBlank() -> {
            val locationBaseline = colorRect.centerY() - (titleMetrics.ascent + titleMetrics.descent) * 0.5f
            canvas.drawText(locationText, centerX, locationBaseline, titlePaint)
        }
        timeText.isNotBlank() -> {
            val timeBaseline = colorRect.centerY() - (subtitleMetrics.ascent + subtitleMetrics.descent) * 0.5f
            canvas.drawText(timeText, centerX, timeBaseline, subtitlePaint)
        }
    }
    return output
}

private fun drawFullColorNoteText(
    canvas: Canvas,
    targetRect: RectF,
    text: String,
    textColor: Int,
    shadowColor: Int,
    preferredTypeface: Typeface?,
    textScaleFactor: Float,
    baseDim: Float
) {
    val layoutWidth = (targetRect.width() * 0.70f).roundToInt().coerceAtLeast(1)
    val maxHeight = targetRect.height() * 0.64f
    val minTextSize = max(targetRect.height() * 0.030f, 10f) * textScaleFactor
    val maxTextSize = max(targetRect.height() * 0.068f, 16f) * textScaleFactor
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = maxTextSize
        typeface = resolveWatermarkTextTypeface(
            preferredTypeface = preferredTypeface,
            text = text,
            bold = true
        )
        isFakeBoldText = true
        letterSpacing = 0.025f
        setShadowLayer(max(baseDim * 0.0016f, 0.6f), 0f, max(baseDim * 0.0008f, 0.35f), shadowColor)
    }
    var layout = buildFullColorNoteLayout(text, paint, layoutWidth)
    while (layout.height > maxHeight && paint.textSize > minTextSize) {
        paint.textSize = (paint.textSize * 0.92f).coerceAtLeast(minTextSize)
        layout = buildFullColorNoteLayout(text, paint, layoutWidth)
    }

    val left = targetRect.centerX() - layoutWidth / 2f
    val top = targetRect.centerY() - layout.height / 2f - targetRect.height() * 0.012f
    canvas.save()
    canvas.translate(left, top)
    layout.draw(canvas)
    canvas.restore()
}

private fun buildFullColorNoteLayout(
    text: String,
    paint: TextPaint,
    width: Int
): StaticLayout {
    return StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.12f)
        .setIncludePad(false)
        .build()
}

internal data class WatermarkPaletteSwatch(
    val color: Int,
    val population: Int,
    val hue: Float,
    val saturation: Float,
    val value: Float
) {
    val hexLabel: String
        get() = "#%02X%02X%02X".format(
            (color shr 16) and 0xFF,
            (color shr 8) and 0xFF,
            color and 0xFF
        )

    val rgbLabel: String
        get() = "R${(color shr 16) and 0xFF} G${(color shr 8) and 0xFF} B${color and 0xFF}"
}

internal data class WatermarkExtractedPalette(
    val anchor: WatermarkPaletteSwatch,
    val primary: WatermarkPaletteSwatch,
    val accent: WatermarkPaletteSwatch,
    val deep: WatermarkPaletteSwatch,
    val contrast: WatermarkPaletteSwatch,
    val ordered: List<WatermarkPaletteSwatch>,
    val expanded: List<WatermarkPaletteSwatch>
)

private data class WatermarkChineseColorEntry(
    val name: String,
    val color: Int,
    val hexLabel: String,
    val rgbLabel: String,
    val cmykLabel: String,
    val hue: Float,
    val saturation: Float,
    val value: Float,
    val lab: DoubleArray,
    val isDark: Boolean,
    val isNeutral: Boolean
)

private data class WatermarkMatchedChineseColor(
    val seed: WatermarkPaletteSwatch,
    val entry: WatermarkChineseColorEntry
)

private data class MutablePaletteBucket(
    var count: Int = 0,
    var sumR: Long = 0,
    var sumG: Long = 0,
    var sumB: Long = 0
) {
    fun add(color: Int) {
        count += 1
        sumR += (color shr 16) and 0xFF
        sumG += (color shr 8) and 0xFF
        sumB += color and 0xFF
    }

    fun toSwatch(): WatermarkPaletteSwatch {
        val divisor = count.coerceAtLeast(1)
        val r = (sumR / divisor).toInt().coerceIn(0, 255)
        val g = (sumG / divisor).toInt().coerceIn(0, 255)
        val b = (sumB / divisor).toInt().coerceIn(0, 255)
        val color = AndroidColor.rgb(r, g, b)
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(r, g, b, hsv)
        return WatermarkPaletteSwatch(
            color = color,
            population = count,
            hue = hsv[0],
            saturation = hsv[1],
            value = hsv[2]
        )
    }
}

private fun extractWatermarkPalette(
    source: Bitmap,
    cacheKey: String? = null
): WatermarkExtractedPalette {
    cacheKey?.let { key ->
        synchronized(cachedWatermarkPalettesLock) {
            cachedWatermarkPalettes[key]
        }?.let { return it }
    }
    val sampled = downsampleForPalette(source, maxSide = 144)
    val pixels = IntArray(sampled.width * sampled.height)
    sampled.getPixels(pixels, 0, sampled.width, 0, 0, sampled.width, sampled.height)

    val buckets = linkedMapOf<Int, MutablePaletteBucket>()
    pixels.forEach { color ->
        val alpha = AndroidColor.alpha(color)
        if (alpha < 220) return@forEach
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(r, g, b, hsv)
        if (hsv[2] <= 0.03f) return@forEach
        if (hsv[2] >= 0.985f && hsv[1] <= 0.05f) return@forEach
        val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
        buckets.getOrPut(key) { MutablePaletteBucket() }.add(AndroidColor.rgb(r, g, b))
    }

    if (sampled !== source) {
        sampled.recycle()
    }

    val candidates = buckets.values
        .asSequence()
        .filter { it.count > 0 }
        .map { it.toSwatch() }
        .sortedByDescending { it.population }
        .toList()
        .ifEmpty { listOf(fallbackAverageSwatch(source)) }

    val filtered = mutableListOf<WatermarkPaletteSwatch>()
    for (candidate in candidates) {
        if (filtered.size >= 18) break
        if (filtered.none { paletteDistance(it, candidate) < 0.10f }) {
            filtered += candidate
        }
    }

    val dominant = filtered.maxByOrNull { it.population } ?: fallbackAverageSwatch(source)
    val anchor = filtered
        .filter { it.saturation <= 0.18f && it.value >= 0.35f }
        .maxByOrNull { it.population.toFloat() * (1.25f - abs(it.value - 0.82f)) }
        ?: deriveSoftNeutral(dominant)

    val primary = filtered
        .filter { paletteDistance(it, anchor) >= 0.08f }
        .maxByOrNull { it.population.toFloat() * (0.95f + it.saturation * 0.65f) }
        ?: dominant

    val accent = filtered
        .filter { minPaletteDistance(it, listOf(anchor, primary)) >= 0.11f }
        .maxByOrNull {
            it.population.toFloat() * 0.35f +
                it.saturation * 105f +
                paletteDistance(it, anchor) * 38f
        }
        ?: deriveAccent(primary)

    val deep = filtered
        .filter { it.value < 0.62f && minPaletteDistance(it, listOf(anchor, primary)) >= 0.08f }
        .maxByOrNull {
            it.population.toFloat() * 0.45f +
                (1f - it.value) * 120f +
                it.saturation * 30f
        }
        ?: deriveDeep(primary)

    val contrast = filtered
        .filter { minPaletteDistance(it, listOf(anchor, primary, accent, deep)) >= 0.12f }
        .maxByOrNull {
            it.population.toFloat() * 0.40f +
                minPaletteDistance(it, listOf(primary, accent, deep)) * 135f
        }
        ?: deriveContrast(primary, anchor)

    val ordered = mutableListOf<WatermarkPaletteSwatch>()
    appendDistinctSwatch(ordered, anchor)
    appendDistinctSwatch(ordered, primary)
    appendDistinctSwatch(ordered, accent)
    appendDistinctSwatch(ordered, deep)
    appendDistinctSwatch(ordered, contrast)
    filtered.forEach { appendDistinctSwatch(ordered, it) }

    val fallbackVariants = listOf(
        deriveSoftNeutral(anchor),
        deriveAccent(primary),
        deriveDeep(accent),
        deriveContrast(primary, anchor),
        deriveSoftened(primary)
    )
    fallbackVariants.forEach { appendDistinctSwatch(ordered, it) }

    while (ordered.size < 5) {
        appendDistinctSwatch(ordered, deriveSoftened(ordered.lastOrNull() ?: dominant))
    }
    val displayOrdered = normalizePaletteForDisplay(
        ordered = ordered,
        fallbackCandidates = fallbackVariants + listOf(
            deriveSoftened(anchor),
            deriveSoftened(accent),
            deriveSoftNeutral(primary)
        ),
        targetCount = 5
    )
    val expandedOrdered = normalizePaletteForDisplay(
        ordered = ordered + filtered,
        fallbackCandidates = fallbackVariants + listOf(
            deriveSoftened(anchor),
            deriveSoftened(accent),
            deriveSoftNeutral(primary),
            deriveAccent(accent),
            deriveSoftened(contrast),
            deriveDeep(primary)
        ),
        targetCount = 12
    )

    val palette = WatermarkExtractedPalette(
        anchor = displayOrdered[0],
        primary = displayOrdered.getOrElse(1) { displayOrdered[0] },
        accent = displayOrdered.getOrElse(2) { displayOrdered[0] },
        deep = displayOrdered.getOrElse(3) { displayOrdered.last() },
        contrast = displayOrdered.getOrElse(4) { displayOrdered.last() },
        ordered = displayOrdered.take(5),
        expanded = expandedOrdered
    )
    cacheKey?.let { key ->
        synchronized(cachedWatermarkPalettesLock) {
            cachedWatermarkPalettes[key] = palette
        }
    }
    return palette
}

private fun deriveRefreshedWatermarkPalette(
    base: WatermarkExtractedPalette,
    refreshNonce: Int
): WatermarkExtractedPalette {
    if (refreshNonce <= 0) return base

    val candidatePool = buildList {
        addAll(base.expanded)
        addAll(base.ordered)
        add(base.anchor)
        add(base.primary)
        add(base.accent)
        add(base.deep)
        add(base.contrast)
    }.distinctBy { it.color }

    if (candidatePool.size <= 1) return base

    val rotation = refreshNonce.mod(candidatePool.size)
    val rotated = candidatePool.drop(rotation) + candidatePool.take(rotation)
    val fallbackCandidates = buildList {
        addAll(candidatePool)
        add(deriveSoftNeutral(base.anchor))
        add(deriveAccent(base.primary))
        add(deriveDeep(base.accent))
        add(deriveContrast(base.primary, base.anchor))
        add(deriveSoftened(base.primary))
    }

    val displayOrdered = normalizePaletteForDisplay(
        ordered = rotated,
        fallbackCandidates = fallbackCandidates,
        targetCount = 5
    )
    val expandedOrdered = normalizePaletteForDisplay(
        ordered = rotated + candidatePool,
        fallbackCandidates = fallbackCandidates,
        targetCount = 12
    )

    return WatermarkExtractedPalette(
        anchor = displayOrdered[0],
        primary = displayOrdered.getOrElse(1) { displayOrdered[0] },
        accent = displayOrdered.getOrElse(2) { displayOrdered[0] },
        deep = displayOrdered.getOrElse(3) { displayOrdered.last() },
        contrast = displayOrdered.getOrElse(4) { displayOrdered.last() },
        ordered = displayOrdered.take(5),
        expanded = expandedOrdered
    )
}

private fun drawHexCardColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?,
    variantIndex: Int,
    showHexValue: Boolean,
    showRgbValue: Boolean
): Bitmap {
    val width: Int
    val height: Int
    val photoRect: RectF
    val swatchShape = resolveWatermarkPaletteBlockShape(variantIndex)
    if (fixedOutputSize != null) {
        width = fixedOutputSize.first.coerceAtLeast(1)
        height = fixedOutputSize.second.coerceAtLeast(1)
        val layout = resolveHexCardLayoutMetrics(
            width = width,
            height = height,
            swatchShape = swatchShape,
            showHexValue = showHexValue,
            showRgbValue = showRgbValue
        )
        photoRect = RectF(
            width * 0.08f,
            height * 0.06f,
            width * 0.92f,
            (height - layout.bottomSectionHeight).coerceAtLeast(height * 0.60f)
        )
    } else {
        val baseDim = max(source.width, source.height)
        val horizontalPad = (baseDim * 0.075f).roundToInt().coerceAtLeast(24)
        val topPad = (baseDim * 0.06f).roundToInt().coerceAtLeast(18)
        width = source.width + horizontalPad * 2
        val estimatedHeight = source.height + topPad + (baseDim * 0.20f).roundToInt().coerceAtLeast(96)
        val layout = resolveHexCardLayoutMetrics(
            width = width,
            height = estimatedHeight,
            swatchShape = swatchShape,
            showHexValue = showHexValue,
            showRgbValue = showRgbValue
        )
        height = source.height + topPad + layout.bottomSectionHeight.roundToInt()
        photoRect = RectF(
            horizontalPad.toFloat(),
            topPad.toFloat(),
            (width - horizontalPad).toFloat(),
            (topPad + source.height).toFloat()
        )
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val surface = retuneColor(palette.anchor.color, saturationMultiplier = 0.18f, targetValue = 0.97f)
    val primaryText = contrastTextColor(surface)
    val secondaryText = applyAlpha(primaryText, 162)
    val divider = mixColors(surface, palette.deep.color, 0.12f)
    val radius = min(photoRect.width(), photoRect.height()) * 0.06f
    val swatches = palette.ordered
    val layout = resolveHexCardLayoutMetrics(
        width = width,
        height = height,
        swatchShape = swatchShape,
        showHexValue = showHexValue,
        showRgbValue = showRgbValue,
        swatchCount = swatches.size
    )
    val swatchTop = photoRect.bottom + layout.swatchTopOffset
    val baseDim = max(width, height).toFloat()

    canvas.drawColor(surface)
    val photoStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (baseDim * 0.0022f).coerceAtLeast(1f)
        color = divider
    }
    drawRoundedCenterCrop(canvas, source, photoRect, radius)
    canvas.drawRoundRect(photoRect, radius, radius, photoStrokePaint)

    val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        textSize = (baseDim * 0.024f).coerceAtLeast(10.5f)
    }
    val rgbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textAlign = Paint.Align.CENTER
        textSize = (baseDim * 0.0175f).coerceAtLeast(9f)
    }
    hexPaint.textSize = resolveSharedTextSize(
        texts = swatches.map { it.hexLabel },
        paint = hexPaint,
        maxWidth = layout.swatchSize.width,
        minSize = 10f,
        maxSize = (baseDim * 0.024f).coerceAtLeast(10.5f)
    )
    rgbPaint.textSize = resolveSharedTextSize(
        texts = swatches.map { it.rgbLabel },
        paint = rgbPaint,
        maxWidth = layout.swatchSize.width,
        minSize = 8f,
        maxSize = (baseDim * 0.0175f).coerceAtLeast(9f)
    )
    val hexLabelHeight = if (showHexValue) {
        max(height * 0.018f, hexPaint.textSize * 0.98f)
    } else {
        0f
    }
    val rgbLabelHeight = if (showRgbValue) {
        if (showHexValue) {
            max(height * 0.010f, rgbPaint.textSize * 0.92f)
        } else {
            max(height * 0.018f, rgbPaint.textSize * 0.98f)
        }
    } else {
        0f
    }
    val swatchSize = layout.swatchSize
    val swatchHeight = swatchSize.height
    val hexTextTop = swatchTop + swatchHeight + hexLabelHeight
    val rgbTextTop = if (showHexValue) {
        hexTextTop + rgbLabelHeight
    } else {
        swatchTop + swatchHeight + rgbLabelHeight
    }

    swatches.forEachIndexed { index, swatch ->
        val slotLeft = layout.outerPad + index * (layout.slotWidth + layout.swatchGap)
        val left = slotLeft + (layout.slotWidth - swatchSize.width) * 0.5f
        val rect = RectF(left, swatchTop, left + swatchSize.width, swatchTop + swatchHeight)
        drawWatermarkPaletteBlock(
            canvas = canvas,
            rect = rect,
            color = swatch.color,
            shape = swatchShape
        )
        if (relativeLuminance(swatch.color) > 0.92f && swatchShape != WatermarkPaletteBlockShape.Heart) {
            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = mixColors(primaryText, surface, 0.55f)
                strokeWidth = (baseDim * 0.0018f).coerceAtLeast(1f)
            }
            when (swatchShape) {
                WatermarkPaletteBlockShape.Default,
                WatermarkPaletteBlockShape.Rectangle -> {
                    val swatchRadius = min(rect.width(), rect.height()) * if (swatchShape == WatermarkPaletteBlockShape.Rectangle) 0.03f else 0.08f
                    canvas.drawRoundRect(rect, swatchRadius, swatchRadius, outlinePaint)
                }
                WatermarkPaletteBlockShape.StraightRectangle -> canvas.drawRect(rect, outlinePaint)
                WatermarkPaletteBlockShape.Circle -> canvas.drawOval(rect, outlinePaint)
                WatermarkPaletteBlockShape.Diamond -> {
                    val diamondPath = Path().apply {
                        moveTo(rect.centerX(), rect.top)
                        lineTo(rect.right, rect.centerY())
                        lineTo(rect.centerX(), rect.bottom)
                        lineTo(rect.left, rect.centerY())
                        close()
                    }
                    canvas.drawPath(diamondPath, outlinePaint)
                }
                WatermarkPaletteBlockShape.Heart -> Unit
            }
        }
        if (showHexValue) {
            canvas.drawText(swatch.hexLabel, rect.centerX(), hexTextTop, hexPaint)
        }
        if (showRgbValue) {
            canvas.drawText(swatch.rgbLabel, rect.centerX(), rgbTextTop, rgbPaint)
        }
    }
    return output
}

private fun drawEditorialStripColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?,
    variantIndex: Int
): Bitmap {
    val width: Int
    val height: Int
    val photoRect: RectF
    val panelRect: RectF
    val borderless = variantIndex == 1
    if (fixedOutputSize != null) {
        width = fixedOutputSize.first.coerceAtLeast(1)
        height = fixedOutputSize.second.coerceAtLeast(1)
        val panelGap = if (borderless) 0f else width * 0.014f
        val panelLeft = width * 0.72f + panelGap
        photoRect = RectF(0f, 0f, width * 0.72f, height.toFloat())
        panelRect = RectF(panelLeft, 0f, width.toFloat(), height.toFloat())
    } else {
        val baseDim = max(source.width, source.height)
        val panelGap = if (borderless) {
            0
        } else {
            (baseDim * 0.02f).roundToInt().coerceAtLeast(10)
        }
        val panelWidth = (baseDim * 0.28f).roundToInt().coerceAtLeast((source.width * 0.24f).roundToInt())
        width = source.width + panelGap + panelWidth
        height = source.height
        photoRect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        panelRect = RectF((source.width + panelGap).toFloat(), 0f, width.toFloat(), height.toFloat())
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val background = retuneColor(palette.deep.color, saturationMultiplier = 0.65f, targetValue = 0.23f)
    val dividerColor = mixColors(background, AndroidColor.WHITE, 0.94f)
    val swatches = palette.ordered
    val chipGap = if (borderless) 0f else height * 0.02f
    val chipHeight = ((panelRect.height() - chipGap * (swatches.size - 1)) / swatches.size).coerceAtLeast(12f)

    canvas.drawColor(background)
    drawBitmapCenterCrop(canvas, source, photoRect)

    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dividerColor }
    if (!borderless) {
        canvas.drawRect(photoRect.right, 0f, panelRect.left, height.toFloat(), dividerPaint)
    }

    swatches.forEachIndexed { index, swatch ->
        val top = panelRect.top + index * (chipHeight + chipGap)
        val rect = RectF(panelRect.left, top, panelRect.right, top + chipHeight)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = swatch.color }
        canvas.drawRect(rect, fillPaint)
        if (!borderless && index < swatches.lastIndex) {
            canvas.drawRect(rect.left, rect.bottom, rect.right, rect.bottom + chipGap, dividerPaint)
        }
    }
    return output
}

private fun drawPaletteNoteColorTemplate(
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?
): Bitmap {
    val width: Int
    val height: Int
    val photoRect: RectF
    val swatchPanelRect: RectF
    if (fixedOutputSize != null) {
        width = fixedOutputSize.first.coerceAtLeast(1)
        height = fixedOutputSize.second.coerceAtLeast(1)
        photoRect = RectF(width * 0.055f, height * 0.07f, width * 0.78f, height * 0.93f)
        swatchPanelRect = RectF(width * 0.81f, height * 0.22f, width * 0.94f, height * 0.73f)
    } else {
        val baseDim = max(source.width, source.height)
        val sidePad = (baseDim * 0.06f).roundToInt().coerceAtLeast(18)
        val topPad = (baseDim * 0.07f).roundToInt().coerceAtLeast(18)
        val bottomPad = (baseDim * 0.08f).roundToInt().coerceAtLeast(26)
        val gap = (baseDim * 0.04f).roundToInt().coerceAtLeast(12)
        val panelWidth = (baseDim * 0.16f).roundToInt().coerceAtLeast((source.width * 0.13f).roundToInt())
        width = source.width + sidePad * 2 + gap + panelWidth
        height = source.height + topPad + bottomPad
        photoRect = RectF(
            sidePad.toFloat(),
            topPad.toFloat(),
            (sidePad + source.width).toFloat(),
            (topPad + source.height).toFloat()
        )
        swatchPanelRect = RectF(
            photoRect.right + gap,
            photoRect.top + photoRect.height() * 0.16f,
            photoRect.right + gap + panelWidth,
            photoRect.top + photoRect.height() * 0.70f
        )
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val surface = retuneColor(palette.anchor.color, saturationMultiplier = 0.22f, targetValue = 0.95f)
    val swatches = palette.ordered
    val swatchGap = swatchPanelRect.height() * 0.06f
    val swatchHeight = ((swatchPanelRect.height() - swatchGap * (swatches.size - 1)) / swatches.size).coerceAtLeast(10f)

    canvas.drawColor(surface)
    drawBitmapCenterCrop(canvas, source, photoRect)

    swatches.forEachIndexed { index, swatch ->
        val top = swatchPanelRect.top + index * (swatchHeight + swatchGap)
        val rect = RectF(swatchPanelRect.left, top, swatchPanelRect.right, top + swatchHeight)
        canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = swatch.color })
    }
    return output
}

private fun drawChinesePaletteColorTemplate(
    context: Context,
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?
): Bitmap {
    val matchedColors = matchWatermarkChinesePalette(context, palette)
    if (matchedColors.isEmpty()) {
        return drawHexCardColorTemplate(
            source = source,
            palette = palette,
            fixedOutputSize = fixedOutputSize,
            variantIndex = 0,
            showHexValue = true,
            showRgbValue = true
        )
    }

    val width: Int
    val height: Int
    val photoRect: RectF
    val panelRect: RectF
    if (fixedOutputSize != null) {
        width = fixedOutputSize.first.coerceAtLeast(1)
        height = fixedOutputSize.second.coerceAtLeast(1)
        photoRect = RectF(width * 0.06f, height * 0.07f, width * 0.67f, height * 0.93f)
        panelRect = RectF(width * 0.72f, height * 0.09f, width * 0.94f, height * 0.91f)
    } else {
        val baseDim = max(source.width, source.height)
        val sidePad = (baseDim * 0.06f).roundToInt().coerceAtLeast(18)
        val topPad = (baseDim * 0.07f).roundToInt().coerceAtLeast(20)
        val bottomPad = (baseDim * 0.08f).roundToInt().coerceAtLeast(24)
        val panelGap = (baseDim * 0.05f).roundToInt().coerceAtLeast(16)
        val panelWidth = (baseDim * 0.33f).roundToInt().coerceAtLeast((source.width * 0.28f).roundToInt())
        width = source.width + sidePad * 2 + panelGap + panelWidth
        height = source.height + topPad + bottomPad
        photoRect = RectF(
            sidePad.toFloat(),
            topPad.toFloat(),
            (sidePad + source.width).toFloat(),
            (topPad + source.height).toFloat()
        )
        panelRect = RectF(
            photoRect.right + panelGap,
            topPad.toFloat(),
            photoRect.right + panelGap + panelWidth,
            (height - bottomPad).toFloat()
        )
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paperBase = AndroidColor.rgb(245, 239, 229)
    val surface = mixColors(
        paperBase,
        retuneColor(palette.anchor.color, saturationMultiplier = 0.16f, targetValue = 0.95f),
        0.18f
    )
    val primaryText = AndroidColor.argb(255, 43, 35, 28)
    val secondaryText = applyAlpha(primaryText, 154)
    val divider = mixColors(primaryText, surface, 0.84f)
    val baseDim = max(width, height).toFloat()
    val photoRadius = min(photoRect.width(), photoRect.height()) * 0.045f
    val outerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (baseDim * 0.0022f).coerceAtLeast(1f)
        color = divider
    }

    canvas.drawColor(surface)
    drawRoundedCenterCrop(canvas, source, photoRect, photoRadius)
    canvas.drawRoundRect(photoRect, photoRadius, photoRadius, outerStrokePaint)

    val chineseTypeface = loadWatermarkChineseColorTypeface(context)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textAlign = Paint.Align.LEFT
        textSize = min((baseDim * 0.041f).coerceAtLeast(18f), panelRect.height() * 0.10f)
        typeface = chineseTypeface ?: Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        isFakeBoldText = true
    }
    fitTextSize(
        paint = titlePaint,
        text = "中国色",
        maxWidth = panelRect.width(),
        minSize = 16f,
        maxSize = titlePaint.textSize
    )
    val titleY = panelRect.top - titlePaint.ascent()
    canvas.drawText("中国色", panelRect.left, titleY, titlePaint)
    val titleGap = max(min(baseDim * 0.018f, panelRect.height() * 0.03f), 6f)
    val rowTop = titleY + titlePaint.descent() + titleGap
    val rowGap = max(min(baseDim * 0.012f, panelRect.height() * 0.018f), 4f)
    val totalGapHeight = rowGap * (matchedColors.size - 1)
    val availableHeight = (panelRect.bottom - rowTop - totalGapHeight).coerceAtLeast(matchedColors.size.toFloat())
    val rowHeight = (availableHeight / matchedColors.size).coerceAtLeast(1f)
    val rowRadius = rowHeight * 0.14f
    val rowHorizontalPad = max(panelRect.width() * 0.06f, 8f)
    val textGap = max(panelRect.width() * 0.05f, 10f)
    val swatchWidth = min(panelRect.width() * 0.20f, rowHeight * 0.62f).coerceAtLeast(12f)
    val textMaxWidth = (
        panelRect.width() - rowHorizontalPad * 2f - swatchWidth - textGap
        ).coerceAtLeast(32f)
    val nameMaxSize = min((baseDim * 0.025f).coerceAtLeast(12f), rowHeight * 0.28f).coerceAtLeast(1f)
    val metaMaxSize = min((baseDim * 0.0128f).coerceAtLeast(8f), rowHeight * 0.15f).coerceAtLeast(1f)

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textAlign = Paint.Align.LEFT
        textSize = nameMaxSize
        typeface = chineseTypeface ?: Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        isFakeBoldText = true
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textAlign = Paint.Align.LEFT
        textSize = metaMaxSize
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        letterSpacing = 0.015f
    }
    namePaint.textSize = resolveSharedTextSize(
        texts = matchedColors.map { it.entry.name },
        paint = namePaint,
        maxWidth = textMaxWidth,
        minSize = min(9f, nameMaxSize),
        maxSize = nameMaxSize
    )
    metaPaint.textSize = resolveSharedTextSize(
        texts = matchedColors.flatMap { listOf(it.entry.rgbLabel, it.entry.cmykLabel) },
        paint = metaPaint,
        maxWidth = textMaxWidth,
        minSize = min(6.5f, metaMaxSize),
        maxSize = metaMaxSize
    )

    matchedColors.forEachIndexed { index, item ->
        val top = rowTop + index * (rowHeight + rowGap)
        val rowRect = RectF(panelRect.left, top, panelRect.right, top + rowHeight)
        val rowFill = mixColors(surface, item.entry.color, 0.09f)
        canvas.drawRoundRect(
            rowRect,
            rowRadius,
            rowRadius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rowFill }
        )
        canvas.drawRoundRect(rowRect, rowRadius, rowRadius, outerStrokePaint)

        val swatchRect = RectF(
            rowRect.left + rowHorizontalPad,
            rowRect.top + rowHeight * 0.18f,
            rowRect.left + rowHorizontalPad + swatchWidth,
            rowRect.bottom - rowHeight * 0.18f
        )
        val swatchRadius = swatchRect.width() * 0.18f
        canvas.drawRoundRect(
            swatchRect,
            swatchRadius,
            swatchRadius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = item.entry.color }
        )
        if (relativeLuminance(item.entry.color) > 0.92f) {
            canvas.drawRoundRect(
                swatchRect,
                swatchRadius,
                swatchRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (baseDim * 0.0018f).coerceAtLeast(1f)
                    color = divider
                }
            )
        }

        val textX = swatchRect.right + textGap
        val interLineGap = max(rowHeight * 0.028f, 1f)
        val totalTextHeight = namePaint.fontSpacing + metaPaint.fontSpacing * 2f + interLineGap * 2f
        val minTextTop = rowRect.top + rowHeight * 0.08f
        val maxTextTop = rowRect.bottom - totalTextHeight - rowHeight * 0.08f
        val textTop = if (maxTextTop >= minTextTop) {
            (rowRect.centerY() - totalTextHeight * 0.5f).coerceIn(minTextTop, maxTextTop)
        } else {
            rowRect.top + max((rowHeight - totalTextHeight) * 0.5f, 0f)
        }
        val nameBaseline = textTop - namePaint.ascent()
        val rgbBaseline = nameBaseline + namePaint.descent() + interLineGap - metaPaint.ascent()
        val cmykBaseline = rgbBaseline + metaPaint.descent() + interLineGap - metaPaint.ascent()
        canvas.drawText(item.entry.name, textX, nameBaseline, namePaint)
        canvas.drawText(item.entry.rgbLabel, textX, rgbBaseline, metaPaint)
        canvas.drawText(item.entry.cmykLabel, textX, cmykBaseline, metaPaint)
    }
    return output
}

private fun drawChineseMonoColorTemplate(
    context: Context,
    source: Bitmap,
    palette: WatermarkExtractedPalette,
    fixedOutputSize: Pair<Int, Int>?,
    monoVariantIndex: Int
): Bitmap {
    val matchedColor = resolveChineseMonoColor(
        context = context,
        palette = palette,
        variantIndex = monoVariantIndex
    )
        ?: return drawChinesePaletteColorTemplate(context, source, palette, fixedOutputSize)

    val width: Int
    val height: Int
    val photoRect: RectF
    val labelAreaLeft: Float
    val labelAreaRight: Float
    val labelAreaTop: Float
    val labelAreaBottom: Float
    if (fixedOutputSize != null) {
        width = fixedOutputSize.first.coerceAtLeast(1)
        height = fixedOutputSize.second.coerceAtLeast(1)
        val leftPad = width * 0.065f
        val topPad = height * 0.065f
        val rightPad = width * 0.055f
        val panelGap = width * 0.04f
        val sidePanelWidth = width * 0.18f
        photoRect = RectF(
            leftPad,
            topPad,
            width - rightPad - sidePanelWidth - panelGap,
            height - topPad
        )
        labelAreaLeft = photoRect.right + panelGap
        labelAreaRight = width - rightPad
        labelAreaTop = topPad
        labelAreaBottom = height - topPad
    } else {
        val baseDim = max(source.width, source.height)
        val leftPad = (baseDim * 0.06f).roundToInt().coerceAtLeast(18)
        val topPad = (baseDim * 0.06f).roundToInt().coerceAtLeast(18)
        val rightPad = (baseDim * 0.05f).roundToInt().coerceAtLeast(16)
        val panelGap = (baseDim * 0.04f).roundToInt().coerceAtLeast(14)
        val sidePanelWidth = (baseDim * 0.20f).roundToInt().coerceAtLeast((source.width * 0.18f).roundToInt())
        width = source.width + leftPad + panelGap + sidePanelWidth + rightPad
        height = source.height + topPad * 2
        photoRect = RectF(
            leftPad.toFloat(),
            topPad.toFloat(),
            (leftPad + source.width).toFloat(),
            (topPad + source.height).toFloat()
        )
        labelAreaLeft = photoRect.right + panelGap
        labelAreaRight = (width - rightPad).toFloat()
        labelAreaTop = topPad.toFloat()
        labelAreaBottom = (height - topPad).toFloat()
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val baseDim = max(width, height).toFloat()
    val frameColor = matchedColor.color
    val photoRadius = min(photoRect.width(), photoRect.height()) * 0.038f
    val imageStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (baseDim * 0.0022f).coerceAtLeast(1f)
        color = applyAlpha(AndroidColor.WHITE, 112)
    }
    val labelMaxWidth = (labelAreaRight - labelAreaLeft).coerceAtLeast(baseDim * 0.14f)
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textAlign = Paint.Align.LEFT
        textSize = (baseDim * 0.064f).coerceAtLeast(28f)
        typeface = loadWatermarkChineseColorTypeface(context) ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)
        isFakeBoldText = true
        setShadowLayer(
            (baseDim * 0.006f).coerceAtLeast(2f),
            0f,
            (baseDim * 0.002f).coerceAtLeast(1f),
            applyAlpha(AndroidColor.BLACK, 82)
        )
    }
    fitTextSize(
        paint = namePaint,
        text = matchedColor.name,
        maxWidth = labelMaxWidth,
        minSize = 18f,
        maxSize = namePaint.textSize
    )

    canvas.drawColor(frameColor)
    drawRoundedCenterCrop(canvas, source, photoRect, photoRadius)
    canvas.drawRoundRect(photoRect, photoRadius, photoRadius, imageStroke)

    val labelHeight = labelAreaBottom - labelAreaTop
    val nameBaseline = labelAreaBottom - max(labelHeight * 0.11f, baseDim * 0.042f)
    canvas.drawText(matchedColor.name, labelAreaLeft, nameBaseline, namePaint)
    return output
}

private fun matchWatermarkChinesePalette(
    context: Context,
    palette: WatermarkExtractedPalette
): List<WatermarkMatchedChineseColor> {
    val catalog = loadWatermarkChineseColors(context)
    if (catalog.isEmpty()) return emptyList()

    val seeds = buildChinesePaletteSeedPool(palette)
    val result = mutableListOf<WatermarkMatchedChineseColor>()
    val usedEntries = mutableListOf<WatermarkChineseColorEntry>()
    var darkCount = 0

    for (seed in seeds) {
        if (result.size >= 5) break
        val match = findBestChineseColorMatch(seed, catalog, usedEntries, darkCount) ?: continue
        result += WatermarkMatchedChineseColor(seed = seed, entry = match)
        usedEntries += match
        if (match.isDark) {
            darkCount += 1
        }
    }

    if (result.size < 5) {
        val fallbackSeed = seeds.firstOrNull() ?: palette.anchor
        val fallbackLab = colorToLab(fallbackSeed.color)
        val fillPool = catalog.sortedBy { candidate ->
            labDistance(fallbackLab, candidate.lab) +
                if (usedEntries.any { labDistance(it.lab, candidate.lab) < 7.5 }) 48.0 else 0.0 +
                if (darkCount >= 1 && candidate.isDark) 60.0 else 0.0
        }
        for (candidate in fillPool) {
            if (result.size >= 5) break
            if (darkCount >= 1 && candidate.isDark) continue
            if (usedEntries.any { labDistance(it.lab, candidate.lab) < 7.5 }) continue
            result += WatermarkMatchedChineseColor(seed = fallbackSeed, entry = candidate)
            usedEntries += candidate
            if (candidate.isDark) {
                darkCount += 1
            }
        }
    }

    return result.take(5)
}

private fun resolveChineseMonoColor(
    context: Context,
    palette: WatermarkExtractedPalette,
    variantIndex: Int
): WatermarkChineseColorEntry? {
    val candidates = resolveChineseMonoColorCandidates(context, palette)
    if (candidates.isEmpty()) return null
    val index = variantIndex.mod(candidates.size)
    return candidates[index]
}

private fun resolveChineseMonoColorCandidates(
    context: Context,
    palette: WatermarkExtractedPalette
): List<WatermarkChineseColorEntry> {
    val catalog = loadWatermarkChineseColors(context)
    if (catalog.isEmpty()) return emptyList()

    val seeds = listOf(
        palette.primary,
        deriveSoftened(palette.primary),
        palette.accent,
        deriveAccent(palette.primary),
        deriveSoftened(palette.accent),
        palette.anchor,
        deriveSoftNeutral(palette.primary),
        deriveContrast(palette.primary, palette.anchor)
    )
    val primaryLab = colorToLab(palette.primary.color)
    val preferredCatalog = catalog.filterNot(::isBlackSeriesChineseMonoCandidate).ifEmpty { catalog }
    val result = mutableListOf<WatermarkChineseColorEntry>()

    seeds.forEach { seed ->
        val candidatesForSeed = preferredCatalog
            .sortedBy { candidate -> chineseMonoCandidateScore(seed, candidate) }
            .take(12)
        candidatesForSeed.forEach { candidate ->
            if (result.size >= 6) return@forEach
            if (result.none { labDistance(it.lab, candidate.lab) < 9.0 }) {
                result += candidate
            }
        }
    }

    if (result.size < 4) {
        preferredCatalog
            .sortedBy { candidate -> labDistance(primaryLab, candidate.lab) + if (isBlackSeriesChineseMonoCandidate(candidate)) 160.0 else 0.0 }
            .forEach { candidate ->
                if (result.size >= 6) return@forEach
                if (result.none { labDistance(it.lab, candidate.lab) < 9.0 }) {
                    result += candidate
                }
            }
    }

    return result
        .sortedBy { candidate ->
            labDistance(primaryLab, candidate.lab) +
                if (candidate.saturation >= 0.16f) -4.0 else 0.0 +
                if (isBlackSeriesChineseMonoCandidate(candidate)) 160.0 else 0.0
        }
        .take(6)
}

private fun buildChinesePaletteSeedPool(palette: WatermarkExtractedPalette): List<WatermarkPaletteSwatch> {
    val seeds = mutableListOf<WatermarkPaletteSwatch>()
    palette.ordered.forEach { appendDistinctSwatch(seeds, it) }
    listOf(
        deriveSoftNeutral(palette.anchor),
        deriveAccent(palette.primary),
        deriveSoftened(palette.primary),
        deriveContrast(palette.accent, palette.anchor),
        deriveSoftNeutral(palette.contrast),
        deriveSoftened(palette.anchor)
    ).forEach { appendDistinctSwatch(seeds, it) }
    return seeds
}

private fun findBestChineseMonoColorMatch(
    seed: WatermarkPaletteSwatch,
    catalog: List<WatermarkChineseColorEntry>
): WatermarkChineseColorEntry? {
    val seedLab = colorToLab(seed.color)
    return catalog.minByOrNull { candidate ->
        chineseMonoCandidateScore(seed = seed, candidate = candidate)
    }
}

private fun chineseMonoCandidateScore(
    seed: WatermarkPaletteSwatch,
    candidate: WatermarkChineseColorEntry
): Double {
    val seedLab = colorToLab(seed.color)
    var score = labDistance(seedLab, candidate.lab)
    score += abs(seed.saturation - candidate.saturation) * 18.0
    score += abs(seed.value - candidate.value) * 16.0
    val luminance = relativeLuminance(candidate.color)
    if (luminance > 0.72f) {
        score += 38.0
    } else if (luminance > 0.60f) {
        score += 18.0
    }
    if (luminance < 0.18f) {
        score += 72.0
    } else if (luminance < 0.26f) {
        score += 26.0
    }
    if (candidate.value < 0.34f) {
        score += 92.0
    } else if (candidate.value < 0.42f) {
        score += 34.0
    }
    if (seed.saturation >= 0.25f && candidate.isNeutral) {
        score += 8.0
    }
    if (seed.saturation <= 0.16f && !candidate.isNeutral) {
        score += 4.0
    }
    if (candidate.saturation >= 0.18f) {
        score -= 5.0
    }
    if (isBlackSeriesChineseMonoCandidate(candidate)) {
        score += 180.0
    }
    return score
}

private fun isBlackSeriesChineseMonoCandidate(candidate: WatermarkChineseColorEntry): Boolean {
    val luminance = relativeLuminance(candidate.color)
    val name = candidate.name
    return candidate.isDark ||
        candidate.value < 0.40f ||
        luminance < 0.20f ||
        name in WATERMARK_CHINESE_MONO_BLACK_SERIES_NAMES ||
        WATERMARK_CHINESE_MONO_BLACK_SERIES_KEYWORDS.any(name::contains)
}

private fun findBestChineseColorMatch(
    seed: WatermarkPaletteSwatch,
    catalog: List<WatermarkChineseColorEntry>,
    usedEntries: List<WatermarkChineseColorEntry>,
    darkCount: Int
): WatermarkChineseColorEntry? {
    val seedLab = colorToLab(seed.color)
    return catalog.minByOrNull { candidate ->
        var score = labDistance(seedLab, candidate.lab)
        score += abs(seed.saturation - candidate.saturation) * 18.0
        score += abs(seed.value - candidate.value) * 14.0
        if (seed.saturation <= 0.18f && !candidate.isNeutral) score += 7.0
        if (seed.saturation >= 0.28f && candidate.isNeutral) score += 10.0
        if (darkCount >= 1 && candidate.isDark) score += 60.0
        usedEntries.forEach { used ->
            val distance = labDistance(used.lab, candidate.lab)
            if (distance < 5.0) {
                score += 64.0
            } else if (distance < 8.5) {
                score += 18.0
            }
            val hueDelta = circularHueDelta(used.hue, candidate.hue)
            if (hueDelta < 10f && abs(used.value - candidate.value) < 0.08f) {
                score += 8.0
            }
        }
        score
    }
}

private fun loadWatermarkChineseColors(context: Context): List<WatermarkChineseColorEntry> {
    cachedWatermarkChineseColors?.let { return it }
    synchronized(WATERMARK_CHINESE_COLORS_ASSET) {
        cachedWatermarkChineseColors?.let { return it }
        val loaded = runCatching {
            context.assets.open(WATERMARK_CHINESE_COLORS_ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
                parseWatermarkChineseColors(JSONObject(reader.readText()))
            }
        }.getOrElse { emptyList() }
        cachedWatermarkChineseColors = loaded
        return loaded
    }
}

private fun parseWatermarkChineseColors(root: JSONObject): List<WatermarkChineseColorEntry> {
    val array = root.optJSONArray("colors") ?: return emptyList()
    val result = ArrayList<WatermarkChineseColorEntry>(array.length())
    for (index in 0 until array.length()) {
        val obj = array.optJSONObject(index) ?: continue
        val name = obj.optString("name").trim()
        val rgbObject = obj.optJSONObject("rgb")
        val cmykObject = obj.optJSONObject("cmyk")
        if (name.isBlank() || rgbObject == null || cmykObject == null) continue
        val r = rgbObject.optInt("r", -1).coerceIn(0, 255)
        val g = rgbObject.optInt("g", -1).coerceIn(0, 255)
        val b = rgbObject.optInt("b", -1).coerceIn(0, 255)
        val c = cmykObject.optInt("c", 0).coerceIn(0, 100)
        val m = cmykObject.optInt("m", 0).coerceIn(0, 100)
        val y = cmykObject.optInt("y", 0).coerceIn(0, 100)
        val k = cmykObject.optInt("k", 0).coerceIn(0, 100)
        val color = AndroidColor.rgb(r, g, b)
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(r, g, b, hsv)
        result += WatermarkChineseColorEntry(
            name = name,
            color = color,
            hexLabel = obj.optString("hex").trim().ifBlank { "#%02X%02X%02X".format(r, g, b) },
            rgbLabel = "RGB $r $g $b",
            cmykLabel = "CMYK $c $m $y $k",
            hue = hsv[0],
            saturation = hsv[1],
            value = hsv[2],
            lab = colorToLab(color),
            isDark = hsv[2] <= 0.34f || relativeLuminance(color) <= 0.085f,
            isNeutral = hsv[1] <= 0.14f || (max(r, max(g, b)) - min(r, min(g, b))) <= 14
        )
    }
    return result
}

private fun loadWatermarkChineseColorTypeface(context: Context): Typeface? {
    cachedWatermarkChineseTypefaces[WATERMARK_CHINESE_COLOR_FONT_ASSET]?.let { return it }
    val loaded = runCatching {
        Typeface.createFromAsset(context.assets, WATERMARK_CHINESE_COLOR_FONT_ASSET)
    }.getOrNull() ?: return null
    cachedWatermarkChineseTypefaces.putIfAbsent(WATERMARK_CHINESE_COLOR_FONT_ASSET, loaded)
    return cachedWatermarkChineseTypefaces[WATERMARK_CHINESE_COLOR_FONT_ASSET] ?: loaded
}

private fun appendDistinctSwatch(target: MutableList<WatermarkPaletteSwatch>, candidate: WatermarkPaletteSwatch) {
    if (target.none { paletteDistance(it, candidate) < 0.08f }) {
        target += candidate
    }
}

private fun normalizePaletteForDisplay(
    ordered: List<WatermarkPaletteSwatch>,
    fallbackCandidates: List<WatermarkPaletteSwatch>,
    targetCount: Int = 5
): List<WatermarkPaletteSwatch> {
    val safeTargetCount = targetCount.coerceAtLeast(1)
    val maxDarkCount = when {
        safeTargetCount <= 5 -> 1
        safeTargetCount <= 10 -> 2
        else -> 3
    }
    val result = mutableListOf<WatermarkPaletteSwatch>()
    var darkCount = 0
    val candidatePool = ordered + fallbackCandidates
    for (candidate in candidatePool) {
        if (result.size >= safeTargetCount) break
        if (darkCount >= maxDarkCount && isTooDarkPaletteSwatch(candidate)) {
            continue
        }
        if (result.none { paletteDistance(it, candidate) < 0.08f }) {
            result += candidate
            if (isTooDarkPaletteSwatch(candidate)) {
                darkCount += 1
            }
        }
    }

    if (result.size < 5) {
        val nonDarkBackupPool = buildList {
            addAll(candidatePool.filterNot(::isTooDarkPaletteSwatch))
            val nonDarkSeeds = ordered.filterNot(::isTooDarkPaletteSwatch)
            val firstSeed = nonDarkSeeds.firstOrNull() ?: ordered.firstOrNull()
            val secondSeed = nonDarkSeeds.getOrNull(1) ?: firstSeed
            val thirdSeed = nonDarkSeeds.getOrNull(2) ?: secondSeed
            if (firstSeed != null) {
                add(deriveSoftNeutral(firstSeed))
                add(deriveSoftened(firstSeed))
                add(deriveAccent(firstSeed))
            }
            if (secondSeed != null && firstSeed != null) {
                add(deriveContrast(secondSeed, firstSeed))
                add(deriveSoftened(secondSeed))
            }
            if (thirdSeed != null) {
                add(deriveAccent(thirdSeed))
            }
        }
        for (candidate in nonDarkBackupPool) {
            if (result.size >= safeTargetCount) break
            if (isTooDarkPaletteSwatch(candidate)) continue
            if (result.none { paletteDistance(it, candidate) < 0.08f }) {
                result += candidate
            }
        }
    }

    while (result.size < safeTargetCount) {
        val seed = result.firstOrNull { !isTooDarkPaletteSwatch(it) } ?: ordered.firstOrNull()
        val fallback = seed?.let(::deriveSoftNeutral) ?: break
        if (result.none { paletteDistance(it, fallback) < 0.08f }) {
            result += fallback
        } else {
            break
        }
    }

    return result.take(safeTargetCount)
}

private fun minPaletteDistance(
    candidate: WatermarkPaletteSwatch,
    selected: List<WatermarkPaletteSwatch>
): Float {
    if (selected.isEmpty()) return 1f
    return selected.minOf { paletteDistance(candidate, it) }
}

private fun paletteDistance(
    first: WatermarkPaletteSwatch,
    second: WatermarkPaletteSwatch
): Float {
    val hueDelta = circularHueDelta(first.hue, second.hue) / 180f
    val satDelta = abs(first.saturation - second.saturation)
    val valueDelta = abs(first.value - second.value)
    val rgbDelta = rgbDistance(first.color, second.color)
    return hueDelta * 0.32f + satDelta * 0.22f + valueDelta * 0.18f + rgbDelta * 0.28f
}

private fun circularHueDelta(a: Float, b: Float): Float {
    val raw = abs(a - b)
    return min(raw, 360f - raw)
}

private fun rgbDistance(first: Int, second: Int): Float {
    val dr = ((AndroidColor.red(first) - AndroidColor.red(second)) / 255f).pow(2)
    val dg = ((AndroidColor.green(first) - AndroidColor.green(second)) / 255f).pow(2)
    val db = ((AndroidColor.blue(first) - AndroidColor.blue(second)) / 255f).pow(2)
    return kotlin.math.sqrt((dr + dg + db) / 3f)
}

private fun colorToLab(color: Int): DoubleArray {
    fun pivotRgb(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    fun pivotXyz(value: Double): Double {
        return if (value > 0.008856) value.pow(1.0 / 3.0) else (7.787 * value) + (16.0 / 116.0)
    }

    val r = pivotRgb(AndroidColor.red(color))
    val g = pivotRgb(AndroidColor.green(color))
    val b = pivotRgb(AndroidColor.blue(color))
    val x = r * 0.4124 + g * 0.3576 + b * 0.1805
    val y = r * 0.2126 + g * 0.7152 + b * 0.0722
    val z = r * 0.0193 + g * 0.1192 + b * 0.9505
    val fx = pivotXyz(x / 0.95047)
    val fy = pivotXyz(y / 1.00000)
    val fz = pivotXyz(z / 1.08883)
    return doubleArrayOf(
        (116.0 * fy) - 16.0,
        500.0 * (fx - fy),
        200.0 * (fy - fz)
    )
}

private fun labDistance(first: DoubleArray, second: DoubleArray): Double {
    val dl = first[0] - second[0]
    val da = first[1] - second[1]
    val db = first[2] - second[2]
    return kotlin.math.sqrt(dl * dl + da * da + db * db)
}

private fun deriveSoftNeutral(source: WatermarkPaletteSwatch): WatermarkPaletteSwatch {
    return copySwatchWithColor(
        source,
        retuneColor(source.color, saturationMultiplier = 0.18f, targetValue = max(0.84f, source.value * 1.15f))
    )
}

private fun deriveAccent(source: WatermarkPaletteSwatch): WatermarkPaletteSwatch {
    return copySwatchWithColor(
        source,
        retuneColor(source.color, saturationMultiplier = 1.28f, targetValue = max(0.72f, source.value))
    )
}

private fun deriveDeep(source: WatermarkPaletteSwatch): WatermarkPaletteSwatch {
    return copySwatchWithColor(
        source,
        retuneColor(source.color, saturationMultiplier = 0.95f, targetValue = min(0.44f, source.value * 0.68f))
    )
}

private fun deriveContrast(
    source: WatermarkPaletteSwatch,
    anchor: WatermarkPaletteSwatch
): WatermarkPaletteSwatch {
    val sourceHsv = FloatArray(3)
    AndroidColor.colorToHSV(source.color, sourceHsv)
    sourceHsv[0] = (sourceHsv[0] + 42f) % 360f
    sourceHsv[1] = max(0.22f, sourceHsv[1] * 0.72f)
    sourceHsv[2] = max(0.30f, min(0.58f, (sourceHsv[2] + anchor.value) * 0.5f))
    return copySwatchWithColor(source, AndroidColor.HSVToColor(sourceHsv))
}

private fun deriveSoftened(source: WatermarkPaletteSwatch): WatermarkPaletteSwatch {
    return copySwatchWithColor(
        source,
        retuneColor(source.color, saturationMultiplier = 0.72f, targetValue = max(0.68f, source.value * 1.06f))
    )
}

private fun isTooDarkPaletteSwatch(swatch: WatermarkPaletteSwatch): Boolean {
    return swatch.value <= 0.34f || relativeLuminance(swatch.color) <= 0.085f
}

private fun copySwatchWithColor(source: WatermarkPaletteSwatch, color: Int): WatermarkPaletteSwatch {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color, hsv)
    return WatermarkPaletteSwatch(
        color = color,
        population = source.population,
        hue = hsv[0],
        saturation = hsv[1],
        value = hsv[2]
    )
}

internal fun retuneColor(color: Int, saturationMultiplier: Float, targetValue: Float): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color, hsv)
    hsv[1] = (hsv[1] * saturationMultiplier).coerceIn(0.02f, 0.96f)
    hsv[2] = targetValue.coerceIn(0.08f, 0.98f)
    return AndroidColor.HSVToColor(hsv)
}

private fun fallbackAverageSwatch(source: Bitmap): WatermarkPaletteSwatch {
    val scaled = downsampleForPalette(source, maxSide = 24)
    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var count = 0
    pixels.forEach { color ->
        if (AndroidColor.alpha(color) < 220) return@forEach
        sumR += AndroidColor.red(color)
        sumG += AndroidColor.green(color)
        sumB += AndroidColor.blue(color)
        count += 1
    }
    if (scaled !== source) {
        scaled.recycle()
    }
    val divisor = count.coerceAtLeast(1)
    val color = AndroidColor.rgb(
        (sumR / divisor).toInt().coerceIn(0, 255),
        (sumG / divisor).toInt().coerceIn(0, 255),
        (sumB / divisor).toInt().coerceIn(0, 255)
    )
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color, hsv)
    return WatermarkPaletteSwatch(
        color = color,
        population = count,
        hue = hsv[0],
        saturation = hsv[1],
        value = hsv[2]
    )
}

private fun downsampleForPalette(source: Bitmap, maxSide: Int): Bitmap {
    val largest = max(source.width, source.height).coerceAtLeast(1)
    if (largest <= maxSide) return source
    val scale = maxSide / largest.toFloat()
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, width, height, true)
}

internal fun contrastTextColor(background: Int): Int {
    return if (relativeLuminance(background) >= 0.58f) {
        AndroidColor.argb(255, 20, 20, 20)
    } else {
        AndroidColor.argb(255, 248, 248, 245)
    }
}

internal fun relativeLuminance(color: Int): Float {
    fun channel(value: Int): Float {
        val v = value / 255f
        return if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    }
    val r = channel(AndroidColor.red(color))
    val g = channel(AndroidColor.green(color))
    val b = channel(AndroidColor.blue(color))
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

internal fun mixColors(first: Int, second: Int, amount: Float): Int {
    val ratio = amount.coerceIn(0f, 1f)
    val inverse = 1f - ratio
    return AndroidColor.argb(
        (AndroidColor.alpha(first) * inverse + AndroidColor.alpha(second) * ratio).roundToInt(),
        (AndroidColor.red(first) * inverse + AndroidColor.red(second) * ratio).roundToInt(),
        (AndroidColor.green(first) * inverse + AndroidColor.green(second) * ratio).roundToInt(),
        (AndroidColor.blue(first) * inverse + AndroidColor.blue(second) * ratio).roundToInt()
    )
}

internal fun applyAlpha(color: Int, alpha: Int): Int {
    return AndroidColor.argb(
        alpha.coerceIn(0, 255),
        AndroidColor.red(color),
        AndroidColor.green(color),
        AndroidColor.blue(color)
    )
}

internal fun drawBitmapCenterCrop(canvas: Canvas, source: Bitmap, dstRect: RectF) {
    if (dstRect.width() <= 1f || dstRect.height() <= 1f) return
    val srcAspect = source.width.toFloat() / source.height.toFloat().coerceAtLeast(1f)
    val dstAspect = dstRect.width() / dstRect.height().coerceAtLeast(1f)
    val srcRect = if (srcAspect > dstAspect) {
        val cropWidth = (source.height * dstAspect).roundToInt().coerceAtLeast(1).coerceAtMost(source.width)
        val left = ((source.width - cropWidth) / 2f).roundToInt().coerceAtLeast(0)
        android.graphics.Rect(left, 0, (left + cropWidth).coerceAtMost(source.width), source.height)
    } else {
        val cropHeight = (source.width / dstAspect).roundToInt().coerceAtLeast(1).coerceAtMost(source.height)
        val top = ((source.height - cropHeight) / 2f).roundToInt().coerceAtLeast(0)
        android.graphics.Rect(0, top, source.width, (top + cropHeight).coerceAtMost(source.height))
    }
    canvas.drawBitmap(source, srcRect, dstRect, Paint(Paint.ANTI_ALIAS_FLAG))
}

internal fun drawRoundedCenterCrop(canvas: Canvas, source: Bitmap, rect: RectF, radius: Float) {
    val path = Path().apply {
        addRoundRect(rect, radius, radius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(path)
    drawBitmapCenterCrop(canvas, source, rect)
    canvas.restore()
}

private fun fitTextSize(
    paint: Paint,
    text: String,
    maxWidth: Float,
    minSize: Float,
    maxSize: Float
) {
    if (text.isBlank() || maxWidth <= 0f) return
    var low = minSize
    var high = maxSize
    var best = minSize
    repeat(10) {
        val mid = (low + high) * 0.5f
        paint.textSize = mid
        if (paint.measureText(text) <= maxWidth) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }
    paint.textSize = best.coerceIn(minSize, maxSize)
}

private fun resolveFullColorBackgroundColor(palette: WatermarkExtractedPalette): Int {
    val preferAnchor =
        palette.anchor.saturation <= 0.14f &&
            palette.anchor.population >= (palette.primary.population * 0.72f).roundToInt()
    val seed = if (preferAnchor) palette.anchor else palette.primary
    val targetValue = when {
        preferAnchor && seed.value <= 0.36f -> 0.26f
        preferAnchor -> (seed.value * 0.90f).coerceIn(0.26f, 0.68f)
        seed.value >= 0.82f -> 0.88f
        seed.value <= 0.34f -> 0.42f
        else -> (seed.value * 0.94f).coerceIn(0.42f, 0.88f)
    }
    val saturationMultiplier = if (preferAnchor) 0.78f else 0.42f
    return retuneColor(seed.color, saturationMultiplier = saturationMultiplier, targetValue = targetValue)
}

private fun resolveFullColorBackgroundGradient(
    background: Int,
    palette: WatermarkExtractedPalette?
): Pair<Int, Int> {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(background, hsv)
    val startBase = palette?.let { mixColors(background, it.contrast.color, 0.14f) } ?: background
    val endBase = palette?.let { mixColors(background, it.deep.color, 0.22f) } ?: background
    val startColor = retuneColor(
        startBase,
        saturationMultiplier = if (palette == null) 0.98f else 0.90f,
        targetValue = (hsv[2] * 1.12f + 0.05f).coerceIn(0.32f, 0.96f)
    )
    val endColor = retuneColor(
        endBase,
        saturationMultiplier = if (palette == null) 1.08f else 1.14f,
        targetValue = (hsv[2] * 0.72f).coerceIn(0.16f, 0.78f)
    )
    return startColor to endColor
}

private fun resolveFullColorPrimaryTextColor(
    background: Int,
    palette: WatermarkExtractedPalette
): Int {
    return if (relativeLuminance(background) >= 0.58f) {
        retuneColor(palette.deep.color, saturationMultiplier = 0.42f, targetValue = 0.21f)
    } else {
        retuneColor(palette.contrast.color, saturationMultiplier = 0.18f, targetValue = 0.90f)
    }
}

private fun resolveFullColorLocationLine(exif: WatermarkExifInfo): String {
    val raw = exif.cityLocation.trim()
    if (raw.isBlank()) return ""
    val normalized = raw
        .replace('，', ',')
        .substringBefore(',')
        .trim()
        .ifBlank { raw }
    return if (normalized.any { it in 'a'..'z' || it in 'A'..'Z' }) {
        normalized.uppercase(Locale.US)
    } else {
        normalized
    }
}

private fun resolveFullColorTimeLine(raw: String): String {
    val normalizedRaw = raw.trim()
    if (normalizedRaw.isBlank()) return ""
    val patterns = listOf(
        "yyyy:MM:dd HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy:MM:dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm",
        "h:mm a",
        "hh:mm a"
    )
    patterns.forEach { pattern ->
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(normalizedRaw)
        }.getOrNull()
        if (parsed != null) {
            return SimpleDateFormat("h:mm a", Locale.US).format(parsed).uppercase(Locale.US)
        }
    }
    return normalizedRaw.uppercase(Locale.getDefault())
}

private fun resolveSharedTextSize(
    texts: List<String>,
    paint: Paint,
    maxWidth: Float,
    minSize: Float,
    maxSize: Float
): Float {
    if (texts.isEmpty() || maxWidth <= 0f) return maxSize.coerceAtLeast(minSize)
    var resolved = maxSize
    texts.forEach { text ->
        fitTextSize(
            paint = paint,
            text = text,
            maxWidth = maxWidth,
            minSize = minSize,
            maxSize = maxSize
        )
        resolved = min(resolved, paint.textSize)
    }
    return resolved.coerceIn(minSize, maxSize)
}
