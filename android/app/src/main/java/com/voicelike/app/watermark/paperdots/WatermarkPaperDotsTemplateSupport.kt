package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal data class WatermarkPaperDotsColorOption(
    val label: String,
    val composeColor: Color,
    val androidColor: Int
)

internal enum class WatermarkPaperDotsLayoutDirection(val title: String) {
    ImageTop("上图下纸"),
    ImageBottom("上纸下图"),
    ImageLeft("左图右纸"),
    ImageRight("左纸右图")
}

internal enum class WatermarkPaperDotsBackgroundMode(val title: String) {
    Solid("纯色"),
    DuoTone("双色条纹"),
    Photo("照片铺底")
}

internal enum class WatermarkPaperDotsDotShape(val title: String) {
    Circle("圆点"),
    Square("方块"),
    Diamond("菱形"),
    Hexagon("六边形"),
    Heart("爱心"),
    Star("星形"),
    Drop("水滴"),
    Snowflake("雪花"),
    Text("文字")
}

internal enum class WatermarkPaperDotsStrokeMode(val title: String) {
    AllFill("全部镂空"),
    Mix("混合"),
    AllStroke("全部描边")
}

internal data class WatermarkPaperDotsConfig(
    val layoutDirection: WatermarkPaperDotsLayoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
    val splitRatio: Float = 0.5f,
    val backgroundMode: WatermarkPaperDotsBackgroundMode = WatermarkPaperDotsBackgroundMode.Solid,
    val backgroundColorIndex: Int = DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_COLOR_INDEX,
    val backgroundSecondaryColorIndex: Int = DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_SECONDARY_COLOR_INDEX,
    val stripeSizeRatio: Float = DEFAULT_WATERMARK_PAPER_DOTS_STRIPE_SIZE_RATIO,
    val dotShape: WatermarkPaperDotsDotShape = WatermarkPaperDotsDotShape.Circle,
    val dotSize: Float = 28f,
    val dotVariance: Float = 32f,
    val dotCount: Int = 18,
    val randomSeed: Int = DEFAULT_WATERMARK_PAPER_DOTS_RANDOM_SEED,
    val strokeMode: WatermarkPaperDotsStrokeMode = WatermarkPaperDotsStrokeMode.AllFill,
    val strokeWidthRatio: Float = DEFAULT_WATERMARK_PAPER_DOTS_STROKE_WIDTH_RATIO,
    val strokeMixRatio: Float = DEFAULT_WATERMARK_PAPER_DOTS_STROKE_MIX_RATIO,
    val patternText: String = DEFAULT_WATERMARK_PAPER_DOTS_PATTERN_TEXT
)

private data class WatermarkPaperDotsStitchLayout(
    val canvasRect: RectF,
    val paperRect: RectF,
    val imageRect: RectF,
    val stripesVertical: Boolean,
    val outputWidth: Int,
    val outputHeight: Int
)

private data class WatermarkPaperDotsSample(
    val xRatio: Float,
    val yRatio: Float,
    val radiusRatio: Float,
    val rotation: Float,
    val starInnerRadiusRatio: Float,
    val renderMode: WatermarkPaperDotsRenderMode = WatermarkPaperDotsRenderMode.PhotoFill
)

private data class WatermarkPaperDotsSpec(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val rotation: Float,
    val starInnerRadiusRatio: Float,
    val renderMode: WatermarkPaperDotsRenderMode = WatermarkPaperDotsRenderMode.PhotoFill
)

private enum class WatermarkPaperDotsRenderMode {
    PhotoFill,
    Outline
}

private val WATERMARK_PAPER_DOTS_SNOWFLAKE_CELLS = listOf(
    0 to 6, 1 to 6, 2 to 6, 3 to 6, 4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6, 9 to 6, 10 to 6, 11 to 6, 12 to 6,
    6 to 0, 6 to 1, 6 to 2, 6 to 3, 6 to 5, 6 to 7, 6 to 9, 6 to 10, 6 to 11, 6 to 12,
    2 to 1, 3 to 2, 4 to 3, 5 to 4, 7 to 8, 8 to 9, 9 to 10, 10 to 11,
    2 to 11, 3 to 10, 4 to 9, 5 to 8, 7 to 4, 8 to 3, 9 to 2, 10 to 1,
    4 to 5, 4 to 7, 8 to 5, 8 to 7
)

internal val WATERMARK_PAPER_DOTS_COLOR_OPTIONS = listOf(
    WatermarkPaperDotsColorOption("米白", Color(0xFFF2EADF), AndroidColor.parseColor("#F2EADF")),
    WatermarkPaperDotsColorOption("杏沙", Color(0xFFE7D5BE), AndroidColor.parseColor("#E7D5BE")),
    WatermarkPaperDotsColorOption("雾粉", Color(0xFFE3C7C0), AndroidColor.parseColor("#E3C7C0")),
    WatermarkPaperDotsColorOption("苔绿", Color(0xFFC8D3C1), AndroidColor.parseColor("#C8D3C1")),
    WatermarkPaperDotsColorOption("雾蓝", Color(0xFFC5D2E6), AndroidColor.parseColor("#C5D2E6")),
    WatermarkPaperDotsColorOption("木炭", Color(0xFF211C18), AndroidColor.parseColor("#211C18")),
    WatermarkPaperDotsColorOption("纯白", Color(0xFFFFFFFF), AndroidColor.parseColor("#FFFFFF"))
)

internal const val DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_COLOR_INDEX = 0
internal const val DEFAULT_WATERMARK_PAPER_DOTS_BACKGROUND_SECONDARY_COLOR_INDEX = 1
internal const val DEFAULT_WATERMARK_PAPER_DOTS_RANDOM_SEED = 48219
internal const val DEFAULT_WATERMARK_PAPER_DOTS_STRIPE_SIZE_RATIO = 0.038f
internal const val DEFAULT_WATERMARK_PAPER_DOTS_STROKE_WIDTH_RATIO = 0.08f
internal const val DEFAULT_WATERMARK_PAPER_DOTS_STROKE_MIX_RATIO = 0.5f
internal const val DEFAULT_WATERMARK_PAPER_DOTS_PATTERN_TEXT = "WOW"

internal fun nextWatermarkPaperDotsSeed(): Int {
    val raw = System.currentTimeMillis().toInt() xor 0x51F15E5
    return if (raw == Int.MIN_VALUE) DEFAULT_WATERMARK_PAPER_DOTS_RANDOM_SEED else raw
}

internal fun WatermarkPaperDotsLayoutDirection.localizedTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> title
    AppLanguage.TraditionalChinese -> when (this) {
        WatermarkPaperDotsLayoutDirection.ImageTop -> "上圖下紙"
        WatermarkPaperDotsLayoutDirection.ImageBottom -> "上紙下圖"
        WatermarkPaperDotsLayoutDirection.ImageLeft -> "左圖右紙"
        WatermarkPaperDotsLayoutDirection.ImageRight -> "左紙右圖"
    }
    else -> when (this) {
        WatermarkPaperDotsLayoutDirection.ImageTop -> "Image Top"
        WatermarkPaperDotsLayoutDirection.ImageBottom -> "Paper Top"
        WatermarkPaperDotsLayoutDirection.ImageLeft -> "Image Left"
        WatermarkPaperDotsLayoutDirection.ImageRight -> "Paper Left"
    }
}

internal fun WatermarkPaperDotsBackgroundMode.localizedTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> title
    AppLanguage.TraditionalChinese -> when (this) {
        WatermarkPaperDotsBackgroundMode.Solid -> "純色"
        WatermarkPaperDotsBackgroundMode.DuoTone -> "雙色條紋"
        WatermarkPaperDotsBackgroundMode.Photo -> "照片鋪底"
    }
    else -> when (this) {
        WatermarkPaperDotsBackgroundMode.Solid -> "Solid"
        WatermarkPaperDotsBackgroundMode.DuoTone -> "Duotone"
        WatermarkPaperDotsBackgroundMode.Photo -> "Photo"
    }
}

internal fun WatermarkPaperDotsDotShape.localizedTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> title
    AppLanguage.TraditionalChinese -> when (this) {
        WatermarkPaperDotsDotShape.Circle -> "圓點"
        WatermarkPaperDotsDotShape.Square -> "方塊"
        WatermarkPaperDotsDotShape.Diamond -> "菱形"
        WatermarkPaperDotsDotShape.Hexagon -> "六邊形"
        WatermarkPaperDotsDotShape.Heart -> "愛心"
        WatermarkPaperDotsDotShape.Star -> "星形"
        WatermarkPaperDotsDotShape.Drop -> "水滴"
        WatermarkPaperDotsDotShape.Snowflake -> "雪花"
        WatermarkPaperDotsDotShape.Text -> "文字"
    }
    else -> when (this) {
        WatermarkPaperDotsDotShape.Circle -> "Circle"
        WatermarkPaperDotsDotShape.Square -> "Square"
        WatermarkPaperDotsDotShape.Diamond -> "Diamond"
        WatermarkPaperDotsDotShape.Hexagon -> "Hexagon"
        WatermarkPaperDotsDotShape.Heart -> "Heart"
        WatermarkPaperDotsDotShape.Star -> "Star"
        WatermarkPaperDotsDotShape.Drop -> "Drop"
        WatermarkPaperDotsDotShape.Snowflake -> "Snowflake"
        WatermarkPaperDotsDotShape.Text -> "Text"
    }
}

internal fun WatermarkPaperDotsStrokeMode.localizedTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> title
    AppLanguage.TraditionalChinese -> when (this) {
        WatermarkPaperDotsStrokeMode.AllFill -> "全部鏤空"
        WatermarkPaperDotsStrokeMode.Mix -> "混合"
        WatermarkPaperDotsStrokeMode.AllStroke -> "全部描邊"
    }
    else -> when (this) {
        WatermarkPaperDotsStrokeMode.AllFill -> "All Cutout"
        WatermarkPaperDotsStrokeMode.Mix -> "Mixed"
        WatermarkPaperDotsStrokeMode.AllStroke -> "All Outline"
    }
}

internal fun sanitizeWatermarkPaperDotsConfig(config: WatermarkPaperDotsConfig): WatermarkPaperDotsConfig {
    return config.copy(
        splitRatio = config.splitRatio.coerceIn(0f, 1f),
        backgroundColorIndex = config.backgroundColorIndex.coerceIn(0, WATERMARK_PAPER_DOTS_COLOR_OPTIONS.lastIndex),
        backgroundSecondaryColorIndex = config.backgroundSecondaryColorIndex.coerceIn(0, WATERMARK_PAPER_DOTS_COLOR_OPTIONS.lastIndex),
        stripeSizeRatio = config.stripeSizeRatio.coerceIn(0.015f, 0.12f),
        dotSize = config.dotSize.coerceIn(12f, 72f),
        dotVariance = config.dotVariance.coerceIn(0f, 100f),
        dotCount = config.dotCount.coerceIn(6, 60),
        strokeWidthRatio = config.strokeWidthRatio.coerceIn(0.02f, 0.25f),
        strokeMixRatio = config.strokeMixRatio.coerceIn(0f, 1f),
        patternText = sanitizeWatermarkPaperDotsPatternText(config.patternText)
    )
}

internal fun sanitizeWatermarkPaperDotsPatternText(input: String): String {
    val sanitized = input
        .filter { char -> !char.isISOControl() }
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(8)
    return sanitized.ifBlank { DEFAULT_WATERMARK_PAPER_DOTS_PATTERN_TEXT }
}

internal fun drawWatermarkPaperDotsTemplate(
    source: Bitmap,
    config: WatermarkPaperDotsConfig,
    fixedOutputSize: Pair<Int, Int>?
): Bitmap {
    val safeConfig = sanitizeWatermarkPaperDotsConfig(config)
    val imageWidth = (fixedOutputSize?.first?.coerceAtLeast(1) ?: source.width).coerceAtLeast(1)
    val imageHeight = (fixedOutputSize?.second?.coerceAtLeast(1) ?: source.height).coerceAtLeast(1)
    val stitchLayout = resolveWatermarkPaperDotsStitchLayout(
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        layoutDirection = safeConfig.layoutDirection,
        splitRatio = safeConfig.splitRatio
    )
    val primaryColor = resolveWatermarkPaperDotsColor(safeConfig.backgroundColorIndex).androidColor
    val secondaryColor = resolveWatermarkPaperDotsColor(safeConfig.backgroundSecondaryColorIndex).androidColor
    val photoBackdrop = if (safeConfig.backgroundMode == WatermarkPaperDotsBackgroundMode.Photo) {
        createWatermarkPaperDotsPhotoBackdrop(source, stitchLayout.outputWidth, stitchLayout.outputHeight)
    } else {
        null
    }
    val samples = generateWatermarkPaperDotsSamples(safeConfig)
    val fullDots = materializeWatermarkPaperDots(samples, stitchLayout.canvasRect)
    val photoFillDots = fullDots.filter { it.renderMode == WatermarkPaperDotsRenderMode.PhotoFill }
    val outlineDots = fullDots.filter { it.renderMode == WatermarkPaperDotsRenderMode.Outline }
    val photoFillDotPaths = photoFillDots.map { spec ->
        createWatermarkPaperDotsPath(
            shape = safeConfig.dotShape,
            spec = spec,
            patternText = safeConfig.patternText
        )
    }
    val outlineDotPaths = outlineDots.map { spec ->
        createWatermarkPaperDotsOutlinePath(
            shape = safeConfig.dotShape,
            spec = spec,
            outlineWidthRatio = safeConfig.strokeWidthRatio,
            patternText = safeConfig.patternText
        )
    }
    val photoFillDotsPath = Path().apply {
        photoFillDotPaths.forEach(::addPath)
    }
    val outlineDotsPath = Path().apply {
        outlineDotPaths.forEach(::addPath)
    }
    val minCanvasSide = min(stitchLayout.canvasRect.width(), stitchLayout.canvasRect.height()).coerceAtLeast(1f)
    val holeShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.argb(12, 90, 74, 54)
        setShadowLayer(
            max(minCanvasSide * 0.0032f, 0.8f),
            0f,
            max(minCanvasSide * 0.0012f, 0.6f),
            AndroidColor.argb(24, 95, 77, 56)
        )
    }
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val projectedSourceRect = resolveWatermarkPaperDotsFitRect(source, stitchLayout.canvasRect)
    val output = Bitmap.createBitmap(
        stitchLayout.outputWidth.coerceAtLeast(1),
        stitchLayout.outputHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    try {
        drawWatermarkPaperDotsBackground(
            canvas = canvas,
            rect = stitchLayout.paperRect,
            config = safeConfig,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            stripesVertical = stitchLayout.stripesVertical,
            photoBackdrop = photoBackdrop
        )

        if (photoFillDotPaths.isNotEmpty() || outlineDotPaths.isNotEmpty()) {
            canvas.save()
            canvas.clipRect(stitchLayout.paperRect)
            if (photoFillDotPaths.isNotEmpty()) {
                canvas.save()
                canvas.clipPath(photoFillDotsPath)
                canvas.drawBitmap(source, null, projectedSourceRect, imagePaint)
                canvas.restore()
            }
            if (outlineDotPaths.isNotEmpty()) {
                canvas.save()
                canvas.clipPath(outlineDotsPath)
                canvas.drawBitmap(source, null, projectedSourceRect, imagePaint)
                canvas.restore()
            }
            canvas.restore()
        }

        canvas.drawBitmap(source, null, stitchLayout.imageRect, imagePaint)

        if (photoFillDotPaths.isNotEmpty()) {
            canvas.save()
            canvas.clipRect(stitchLayout.imageRect)
            photoFillDotPaths.forEach { dotPath ->
                canvas.drawPath(dotPath, holeShadowPaint)
            }
            canvas.save()
            canvas.clipPath(photoFillDotsPath)
            drawWatermarkPaperDotsBackground(
                canvas = canvas,
                rect = stitchLayout.canvasRect,
                config = safeConfig,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                stripesVertical = stitchLayout.stripesVertical,
                photoBackdrop = photoBackdrop
            )
            canvas.restore()
            canvas.restore()
        }

        if (outlineDotPaths.isNotEmpty()) {
            canvas.save()
            canvas.clipRect(stitchLayout.imageRect)
            canvas.save()
            canvas.clipPath(outlineDotsPath)
            drawWatermarkPaperDotsBackground(
                canvas = canvas,
                rect = stitchLayout.canvasRect,
                config = safeConfig,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                stripesVertical = stitchLayout.stripesVertical,
                photoBackdrop = photoBackdrop
            )
            canvas.restore()
            canvas.restore()
        }

        return output
    } finally {
        photoBackdrop?.recycle()
    }
}

private fun resolveWatermarkPaperDotsStitchLayout(
    imageWidth: Int,
    imageHeight: Int,
    layoutDirection: WatermarkPaperDotsLayoutDirection,
    splitRatio: Float
): WatermarkPaperDotsStitchLayout {
    val safeRatio = splitRatio.coerceIn(0.05f, 0.85f)
    val imageFraction = (1f - safeRatio).coerceAtLeast(0.15f)
    val extensionRatio = safeRatio / imageFraction
    val paperWidth = max(1, (imageWidth * extensionRatio).roundToInt())
    val paperHeight = max(1, (imageHeight * extensionRatio).roundToInt())
    return when (layoutDirection) {
        WatermarkPaperDotsLayoutDirection.ImageTop -> WatermarkPaperDotsStitchLayout(
            canvasRect = RectF(0f, 0f, imageWidth.toFloat(), (imageHeight + paperHeight).toFloat()),
            paperRect = RectF(0f, imageHeight.toFloat(), imageWidth.toFloat(), (imageHeight + paperHeight).toFloat()),
            imageRect = RectF(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat()),
            stripesVertical = false,
            outputWidth = imageWidth,
            outputHeight = imageHeight + paperHeight
        )
        WatermarkPaperDotsLayoutDirection.ImageBottom -> WatermarkPaperDotsStitchLayout(
            canvasRect = RectF(0f, 0f, imageWidth.toFloat(), (imageHeight + paperHeight).toFloat()),
            paperRect = RectF(0f, 0f, imageWidth.toFloat(), paperHeight.toFloat()),
            imageRect = RectF(0f, paperHeight.toFloat(), imageWidth.toFloat(), (paperHeight + imageHeight).toFloat()),
            stripesVertical = false,
            outputWidth = imageWidth,
            outputHeight = imageHeight + paperHeight
        )
        WatermarkPaperDotsLayoutDirection.ImageLeft -> WatermarkPaperDotsStitchLayout(
            canvasRect = RectF(0f, 0f, (imageWidth + paperWidth).toFloat(), imageHeight.toFloat()),
            paperRect = RectF(imageWidth.toFloat(), 0f, (imageWidth + paperWidth).toFloat(), imageHeight.toFloat()),
            imageRect = RectF(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat()),
            stripesVertical = true,
            outputWidth = imageWidth + paperWidth,
            outputHeight = imageHeight
        )
        WatermarkPaperDotsLayoutDirection.ImageRight -> WatermarkPaperDotsStitchLayout(
            canvasRect = RectF(0f, 0f, (imageWidth + paperWidth).toFloat(), imageHeight.toFloat()),
            paperRect = RectF(0f, 0f, paperWidth.toFloat(), imageHeight.toFloat()),
            imageRect = RectF(paperWidth.toFloat(), 0f, (paperWidth + imageWidth).toFloat(), imageHeight.toFloat()),
            stripesVertical = true,
            outputWidth = imageWidth + paperWidth,
            outputHeight = imageHeight
        )
    }
}

private fun drawWatermarkPaperDotsBackground(
    canvas: Canvas,
    rect: RectF,
    config: WatermarkPaperDotsConfig,
    primaryColor: Int,
    secondaryColor: Int,
    stripesVertical: Boolean,
    photoBackdrop: Bitmap?
) {
    when (config.backgroundMode) {
        WatermarkPaperDotsBackgroundMode.Solid -> {
            canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor })
        }
        WatermarkPaperDotsBackgroundMode.DuoTone -> {
            val stripeSize = (min(rect.width(), rect.height()) * config.stripeSizeRatio).coerceAtLeast(10f)
            val stripeCount = if (stripesVertical) {
                ceil(rect.width() / stripeSize).toInt().coerceAtLeast(1)
            } else {
                ceil(rect.height() / stripeSize).toInt().coerceAtLeast(1)
            }
            val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            repeat(stripeCount) { index ->
                stripePaint.color = if (index % 2 == 0) primaryColor else secondaryColor
                if (stripesVertical) {
                    val left = rect.left + index * stripeSize
                    canvas.drawRect(left, rect.top, min(rect.right, left + stripeSize), rect.bottom, stripePaint)
                } else {
                    val top = rect.top + index * stripeSize
                    canvas.drawRect(rect.left, top, rect.right, min(rect.bottom, top + stripeSize), stripePaint)
                }
            }
        }
        WatermarkPaperDotsBackgroundMode.Photo -> {
            photoBackdrop?.let { backdrop ->
                canvas.drawBitmap(backdrop, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } ?: canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor })
            canvas.drawRect(
                rect,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(
                        132,
                        AndroidColor.red(primaryColor),
                        AndroidColor.green(primaryColor),
                        AndroidColor.blue(primaryColor)
                    )
                }
            )
        }
    }
}

private fun createWatermarkPaperDotsPhotoBackdrop(source: Bitmap, width: Int, height: Int): Bitmap {
    val downScale = 0.12f
    val firstWidth = max(24, (source.width * downScale).roundToInt())
    val firstHeight = max(24, (source.height * downScale).roundToInt())
    val small = Bitmap.createScaledBitmap(source, firstWidth, firstHeight, true)
    val stretched = Bitmap.createScaledBitmap(small, width, height, true)
    small.recycle()
    return stretched
}

private fun generateWatermarkPaperDotsSamples(
    config: WatermarkPaperDotsConfig
): List<WatermarkPaperDotsSample> {
    val random = Random(config.randomSeed)
    val normalizedRadius = (config.dotSize / 800f).coerceIn(0.018f, 0.12f)
    val varianceRatio = (config.dotVariance / 100f) * 0.65f
    val samples = mutableListOf<WatermarkPaperDotsSample>()
    val attemptLimit = max(120, config.dotCount * 80)
    var attempts = 0
    while (samples.size < config.dotCount && attempts < attemptLimit) {
        attempts += 1
        val sizeScale = (1f + ((random.nextFloat() * 2f) - 1f) * varianceRatio).coerceIn(0.58f, 1.54f)
        val radius = normalizedRadius * sizeScale
        val xRatio = radius + random.nextFloat() * (1f - radius * 2f)
        val yRatio = radius + random.nextFloat() * (1f - radius * 2f)
        val overlaps = samples.any { existing ->
            val dx = existing.xRatio - xRatio
            val dy = existing.yRatio - yRatio
            val minDistance = (existing.radiusRatio + radius) * 0.72f
            (dx * dx) + (dy * dy) <= minDistance * minDistance
        }
        if (!overlaps) {
            samples += WatermarkPaperDotsSample(
                xRatio = xRatio,
                yRatio = yRatio,
                radiusRatio = radius,
                rotation = random.nextFloat() * 360f,
                starInnerRadiusRatio = 0.42f + random.nextFloat() * 0.16f,
                renderMode = resolveWatermarkPaperDotsRenderMode(config, random)
            )
        }
    }
    if (samples.size == config.dotCount) return samples

    val gridColumns = ceil(sqrt(config.dotCount.toFloat())).toInt().coerceAtLeast(1)
    val gridRows = ceil(config.dotCount.toFloat() / gridColumns.toFloat()).toInt().coerceAtLeast(1)
    val fallback = samples.toMutableList()
    while (fallback.size < config.dotCount) {
        val index = fallback.size
        val column = index % gridColumns
        val row = index / gridColumns
        val xRatio = ((column + 0.5f) / gridColumns).coerceIn(0.12f, 0.88f)
        val yRatio = ((row + 0.5f) / gridRows).coerceIn(0.12f, 0.88f)
        fallback += WatermarkPaperDotsSample(
            xRatio = xRatio,
            yRatio = yRatio,
            radiusRatio = normalizedRadius,
            rotation = (index * 17f) % 360f,
            starInnerRadiusRatio = 0.5f,
            renderMode = resolveWatermarkPaperDotsRenderMode(config, random)
        )
    }
    return fallback.take(config.dotCount)
}

private fun materializeWatermarkPaperDots(
    samples: List<WatermarkPaperDotsSample>,
    rect: RectF
): List<WatermarkPaperDotsSpec> {
    if (rect.width() <= 0f || rect.height() <= 0f) return emptyList()
    val minSide = min(rect.width(), rect.height()).coerceAtLeast(1f)
    return samples.map { sample ->
        val radius = (minSide * sample.radiusRatio).coerceIn(minSide * 0.02f, minSide * 0.16f)
        WatermarkPaperDotsSpec(
            centerX = (rect.left + rect.width() * sample.xRatio).coerceIn(rect.left + radius, rect.right - radius),
            centerY = (rect.top + rect.height() * sample.yRatio).coerceIn(rect.top + radius, rect.bottom - radius),
            radius = radius,
            rotation = sample.rotation,
            starInnerRadiusRatio = sample.starInnerRadiusRatio,
            renderMode = sample.renderMode
        )
    }
}

private fun createWatermarkPaperDotsPath(
    shape: WatermarkPaperDotsDotShape,
    spec: WatermarkPaperDotsSpec,
    patternText: String
): Path {
    return Path().apply {
        when (shape) {
            WatermarkPaperDotsDotShape.Circle -> {
                addOval(
                    RectF(
                        spec.centerX - spec.radius,
                        spec.centerY - spec.radius,
                        spec.centerX + spec.radius,
                        spec.centerY + spec.radius
                    ),
                    Path.Direction.CW
                )
            }
            WatermarkPaperDotsDotShape.Square -> {
                val half = spec.radius
                val rotation = Math.toRadians(spec.rotation.toDouble()).toFloat()
                val points = listOf(
                    -half to -half,
                    half to -half,
                    half to half,
                    -half to half
                ).map { (x, y) ->
                    rotateWatermarkPaperDotsPoint(spec.centerX, spec.centerY, x, y, rotation)
                }
                moveTo(points.first().first, points.first().second)
                points.drop(1).forEach { (x, y) -> lineTo(x, y) }
                close()
            }
            WatermarkPaperDotsDotShape.Diamond -> {
                val rotation = Math.toRadians((spec.rotation + 45f).toDouble()).toFloat()
                val points = listOf(
                    0f to -spec.radius,
                    spec.radius to 0f,
                    0f to spec.radius,
                    -spec.radius to 0f
                ).map { (x, y) ->
                    rotateWatermarkPaperDotsPoint(spec.centerX, spec.centerY, x, y, rotation)
                }
                moveTo(points.first().first, points.first().second)
                points.drop(1).forEach { (x, y) -> lineTo(x, y) }
                close()
            }
            WatermarkPaperDotsDotShape.Hexagon -> {
                repeat(6) { index ->
                    val angle = Math.toRadians((spec.rotation - 30f + index * 60f).toDouble())
                    val x = spec.centerX + cos(angle).toFloat() * spec.radius
                    val y = spec.centerY + sin(angle).toFloat() * spec.radius
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            WatermarkPaperDotsDotShape.Heart -> {
                val top = spec.centerY - spec.radius * 0.12f
                moveTo(spec.centerX, spec.centerY + spec.radius * 0.96f)
                cubicTo(
                    spec.centerX - spec.radius * 1.08f,
                    spec.centerY + spec.radius * 0.34f,
                    spec.centerX - spec.radius * 1.12f,
                    top - spec.radius * 0.34f,
                    spec.centerX,
                    top
                )
                cubicTo(
                    spec.centerX + spec.radius * 1.12f,
                    top - spec.radius * 0.34f,
                    spec.centerX + spec.radius * 1.08f,
                    spec.centerY + spec.radius * 0.34f,
                    spec.centerX,
                    spec.centerY + spec.radius * 0.96f
                )
                close()
            }
            WatermarkPaperDotsDotShape.Star -> {
                repeat(10) { index ->
                    val radius = if (index % 2 == 0) spec.radius else spec.radius * spec.starInnerRadiusRatio
                    val angle = Math.toRadians((spec.rotation - 90f + index * 36f).toDouble())
                    val x = spec.centerX + cos(angle).toFloat() * radius
                    val y = spec.centerY + sin(angle).toFloat() * radius
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            WatermarkPaperDotsDotShape.Drop -> {
                moveTo(spec.centerX, spec.centerY - spec.radius * 1.18f)
                cubicTo(
                    spec.centerX - spec.radius * 0.24f,
                    spec.centerY - spec.radius * 0.96f,
                    spec.centerX - spec.radius * 1.08f,
                    spec.centerY + spec.radius * 0.24f,
                    spec.centerX,
                    spec.centerY + spec.radius * 0.98f
                )
                cubicTo(
                    spec.centerX + spec.radius * 1.08f,
                    spec.centerY + spec.radius * 0.24f,
                    spec.centerX + spec.radius * 0.24f,
                    spec.centerY - spec.radius * 0.96f,
                    spec.centerX,
                    spec.centerY - spec.radius * 1.18f
                )
                close()
            }
            WatermarkPaperDotsDotShape.Snowflake -> {
                val cell = (spec.radius * 2f) / 13f
                val left = spec.centerX - spec.radius
                val top = spec.centerY - spec.radius
                WATERMARK_PAPER_DOTS_SNOWFLAKE_CELLS.forEach { (row, column) ->
                    addRect(
                        left + column * cell,
                        top + row * cell,
                        left + (column + 1) * cell,
                        top + (row + 1) * cell,
                        Path.Direction.CW
                    )
                }
            }
            WatermarkPaperDotsDotShape.Text -> {
                addPath(createWatermarkPaperDotsTextPath(spec, patternText))
            }
        }
    }
}

private fun createWatermarkPaperDotsOutlinePath(
    shape: WatermarkPaperDotsDotShape,
    spec: WatermarkPaperDotsSpec,
    outlineWidthRatio: Float,
    patternText: String
): Path {
    val outerPath = createWatermarkPaperDotsPath(shape, spec, patternText)
    val inset = (spec.radius * outlineWidthRatio.coerceIn(0.02f, 0.25f) * 1.45f)
        .coerceIn(1f, spec.radius * 0.72f)
    val innerRadius = (spec.radius - inset).coerceAtLeast(spec.radius * 0.28f)
    if (innerRadius >= spec.radius - 0.5f) return outerPath
    val innerPath = createWatermarkPaperDotsPath(shape, spec.copy(radius = innerRadius), patternText)
    return Path().apply {
        fillType = Path.FillType.EVEN_ODD
        addPath(outerPath)
        addPath(innerPath)
    }
}

private fun createWatermarkPaperDotsTextPath(
    spec: WatermarkPaperDotsSpec,
    patternText: String
): Path {
    val safeText = sanitizeWatermarkPaperDotsPatternText(patternText)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = (spec.radius * 1.42f).coerceAtLeast(12f)
        isFakeBoldText = true
    }
    val widthLimit = (spec.radius * 2.12f).coerceAtLeast(16f)
    val measuredWidth = textPaint.measureText(safeText).coerceAtLeast(1f)
    if (measuredWidth > widthLimit) {
        textPaint.textSize *= widthLimit / measuredWidth
    }
    val baseline = spec.centerY - ((textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) * 0.5f)
    val path = Path()
    textPaint.getTextPath(safeText, 0, safeText.length, spec.centerX, baseline, path)
    if (spec.rotation == 0f) return path
    return Path().apply {
        val matrix = Matrix().apply {
            setRotate(spec.rotation, spec.centerX, spec.centerY)
        }
        path.transform(matrix, this)
    }
}

private fun rotateWatermarkPaperDotsPoint(
    centerX: Float,
    centerY: Float,
    x: Float,
    y: Float,
    rotationRadians: Float
): Pair<Float, Float> {
    val rotatedX = x * cos(rotationRadians) - y * sin(rotationRadians)
    val rotatedY = x * sin(rotationRadians) + y * cos(rotationRadians)
    return centerX + rotatedX to centerY + rotatedY
}

private fun resolveWatermarkPaperDotsColor(index: Int): WatermarkPaperDotsColorOption {
    return WATERMARK_PAPER_DOTS_COLOR_OPTIONS.getOrElse(index.coerceIn(0, WATERMARK_PAPER_DOTS_COLOR_OPTIONS.lastIndex)) {
        WATERMARK_PAPER_DOTS_COLOR_OPTIONS.first()
    }
}

private fun resolveWatermarkPaperDotsRenderMode(
    config: WatermarkPaperDotsConfig,
    random: Random
): WatermarkPaperDotsRenderMode {
    return when (config.strokeMode) {
        WatermarkPaperDotsStrokeMode.AllFill -> WatermarkPaperDotsRenderMode.PhotoFill
        WatermarkPaperDotsStrokeMode.AllStroke -> WatermarkPaperDotsRenderMode.Outline
        WatermarkPaperDotsStrokeMode.Mix -> {
            val outlineThreshold = config.strokeMixRatio.coerceIn(0f, 1f)
            if (random.nextFloat() < outlineThreshold) {
                WatermarkPaperDotsRenderMode.Outline
            } else {
                WatermarkPaperDotsRenderMode.PhotoFill
            }
        }
    }
}

private fun resolveWatermarkPaperDotsFitRect(source: Bitmap, dstRect: RectF): RectF {
    val sourceWidth = source.width.toFloat().coerceAtLeast(1f)
    val sourceHeight = source.height.toFloat().coerceAtLeast(1f)
    val scale = max(dstRect.width() / sourceWidth, dstRect.height() / sourceHeight).coerceAtLeast(0.0001f)
    val fittedWidth = sourceWidth * scale
    val fittedHeight = sourceHeight * scale
    val left = dstRect.centerX() - fittedWidth * 0.5f
    val top = dstRect.centerY() - fittedHeight * 0.5f
    return RectF(left, top, left + fittedWidth, top + fittedHeight)
}
