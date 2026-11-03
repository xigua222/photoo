package com.voicelike.app.stamp

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.TextPaint
import android.text.TextUtils
import android.text.StaticLayout
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.voicelike.app.buildCameraImageContentValues
import com.voicelike.app.R
import com.voicelike.app.stamp.cutout.StampAutoCutoutModelSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal const val STAMP_EXPORT_IMAGE_FOLDER = "Pictures/Photoo/Stamp"
private val stampExportFileSequence = AtomicInteger(0)
private val stampCutoutFileSequence = AtomicInteger(0)
private const val STAMP_AUTO_CUTOUT_LOCAL_FOLDER = "stamp/cutout_auto"
private const val STAMP_AUTO_CUTOUT_CACHE_VERSION = 9
internal const val STAMP_NUMERIC_STYLE_FIXED_VALUE = "80"
internal const val STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT = "photoo post"
internal val STAMP_SKIN_BACKGROUND_COLOR_OPTIONS: List<Int> = listOf(
    0xFFFFFFFF.toInt(),
    0xFFE6F4FF.toInt(),
    0xFFFFD9E8.toInt(),
    0xFFE7FFD9.toInt(),
    0xFFFF6B6B.toInt(),
    0xFFFF9F43.toInt(),
    0xFFFFD166.toInt(),
    0xFF66D9E8.toInt(),
    0xFF4DABF7.toInt(),
    0xFF9775FA.toInt(),
    0xFFF783AC.toInt(),
    0xFF63E6BE.toInt()
)

internal enum class StampCalendarSkinStyle {
    Original,
    WhiteBorder,
    Editorial,
    Cutout
}

internal enum class StampCalendarImageEffect {
    None,
    Cartoon,
    InkSpread,
    TextureOverlay,
    ColorQuantize,
    Halftone,
    MisprintOffset
}

internal val STAMP_SKIN_IMAGE_EFFECT_OPTIONS: List<StampCalendarImageEffect> = listOf(
    StampCalendarImageEffect.None,
    StampCalendarImageEffect.Cartoon,
    StampCalendarImageEffect.InkSpread,
    StampCalendarImageEffect.TextureOverlay,
    StampCalendarImageEffect.ColorQuantize,
    StampCalendarImageEffect.Halftone,
    StampCalendarImageEffect.MisprintOffset
)

internal data class StampCalendarStampSkin(
    val style: StampCalendarSkinStyle = StampCalendarSkinStyle.Original,
    val numberLabel: String = "",
    val title: String = "",
    val subtitle: String = "",
    val cutoutAssetPath: String = "",
    val backgroundColorArgb: Int = 0,
    val imageEffect: StampCalendarImageEffect = StampCalendarImageEffect.None
)

internal fun resolveStampSkinDefaultBackgroundColor(
    style: StampCalendarSkinStyle
): Int = when (style) {
    StampCalendarSkinStyle.WhiteBorder -> 0xFFFFFFFF.toInt()
    StampCalendarSkinStyle.Editorial -> 0xFFFFFFFF.toInt()
    StampCalendarSkinStyle.Cutout -> 0xFFE6F4FF.toInt()
    StampCalendarSkinStyle.Original -> 0
}

internal fun normalizeStampSkinBackgroundColor(
    style: StampCalendarSkinStyle,
    requestedColor: Int
): Int {
    if (style == StampCalendarSkinStyle.Original) return 0
    return if (requestedColor in STAMP_SKIN_BACKGROUND_COLOR_OPTIONS) {
        requestedColor
    } else {
        resolveStampSkinDefaultBackgroundColor(style)
    }
}

internal fun StampCalendarStampSkin?.normalized(): StampCalendarStampSkin {
    val source = this ?: StampCalendarStampSkin()
    val normalizedEffect = source.imageEffect
    return when (source.style) {
        StampCalendarSkinStyle.Original -> StampCalendarStampSkin(
            style = StampCalendarSkinStyle.Original,
            imageEffect = normalizedEffect
        )
        StampCalendarSkinStyle.WhiteBorder -> StampCalendarStampSkin(
            style = StampCalendarSkinStyle.WhiteBorder,
            backgroundColorArgb = normalizeStampSkinBackgroundColor(
                style = StampCalendarSkinStyle.WhiteBorder,
                requestedColor = source.backgroundColorArgb
            ),
            imageEffect = normalizedEffect
        )
        StampCalendarSkinStyle.Editorial -> StampCalendarStampSkin(
            style = StampCalendarSkinStyle.Editorial,
            numberLabel = STAMP_NUMERIC_STYLE_FIXED_VALUE,
            title = source.title.trim().take(40),
            subtitle = "",
            backgroundColorArgb = normalizeStampSkinBackgroundColor(
                style = StampCalendarSkinStyle.Editorial,
                requestedColor = source.backgroundColorArgb
            ),
            imageEffect = normalizedEffect
        )
        StampCalendarSkinStyle.Cutout -> StampCalendarStampSkin(
            style = StampCalendarSkinStyle.Cutout,
            cutoutAssetPath = source.cutoutAssetPath.trim().take(320),
            backgroundColorArgb = normalizeStampSkinBackgroundColor(
                style = StampCalendarSkinStyle.Cutout,
                requestedColor = source.backgroundColorArgb
            ),
            imageEffect = normalizedEffect
        )
    }
}

internal fun StampCalendarStampSkin?.isOriginalStyle(): Boolean =
    this.normalized().let { skin ->
        skin.style == StampCalendarSkinStyle.Original &&
            skin.imageEffect == StampCalendarImageEffect.None
    }

internal fun isCircularStampBitmap(bitmap: Bitmap?): Boolean {
    if (bitmap == null || bitmap.isRecycled || bitmap.width <= 8 || bitmap.height <= 8) {
        return false
    }
    val width = bitmap.width
    val height = bitmap.height
    val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
    if (aspect !in 0.88f..1.12f) return false

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val cornerSize = (min(width, height) * 0.14f).roundToInt().coerceIn(3, min(width, height) / 2)
    var cornerSamples = 0
    var transparentCorners = 0
    fun sampleCorner(left: Int, top: Int) {
        for (y in top until (top + cornerSize).coerceAtMost(height)) {
            val rowOffset = y * width
            for (x in left until (left + cornerSize).coerceAtMost(width)) {
                cornerSamples += 1
                if ((pixels[rowOffset + x] ushr 24) <= 18) {
                    transparentCorners += 1
                }
            }
        }
    }
    sampleCorner(0, 0)
    sampleCorner(width - cornerSize, 0)
    sampleCorner(0, height - cornerSize)
    sampleCorner(width - cornerSize, height - cornerSize)
    if (cornerSamples <= 0) return false
    val transparentCornerRatio = transparentCorners.toFloat() / cornerSamples.toFloat()
    if (transparentCornerRatio < 0.62f) return false

    var opaqueCount = 0
    for (color in pixels) {
        if ((color ushr 24) > 18) {
            opaqueCount += 1
        }
    }
    val opaqueRatio = opaqueCount.toFloat() / pixels.size.toFloat().coerceAtLeast(1f)
    return opaqueRatio in 0.42f..0.9f
}

internal fun isReusableStampAutoCutoutAssetPath(path: String): Boolean {
    val file = File(path.trim())
    if (!file.exists()) return false
    return !file.name.startsWith("cutout_v") ||
        file.name.startsWith("cutout_v${STAMP_AUTO_CUTOUT_CACHE_VERSION}_")
}

internal data class StampThemePreset(
    val key: String,
    val label: String,
    val backgroundColor: Int,
    val borderColor: Int,
    val inkColor: Int,
    val accentColor: Int
)

internal data class StampRenderSpec(
    val title: String,
    val subtitle: String,
    val valueLabel: String,
    val serialLabel: String,
    val theme: StampThemePreset
)

internal suspend fun loadStampSourceBitmap(
    context: Context,
    uri: Uri,
    maxSide: Int,
    detachFromSourceCache: Boolean = false
): Bitmap? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data(uri)
        .size(maxSide)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request)
    val drawable = (result as? SuccessResult)?.drawable ?: return@withContext null
    val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
    if (!detachFromSourceCache) {
        return@withContext bitmap
    }
    runCatching {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull()?.takeUnless { it.isRecycled || it.width <= 0 || it.height <= 0 } ?: bitmap
}

internal fun loadStampOverlayBitmap(context: Context): Bitmap? =
    runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.stamp_one) }.getOrNull()

internal fun loadStampExportLogoBitmap(context: Context): Bitmap? =
    runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.logokt) }.getOrNull()

internal suspend fun ensureStampAutoCutoutAssetPath(
    context: Context,
    sourceUriString: String,
    existingPath: String
): String? = withContext(Dispatchers.IO) {
    val normalizedExistingPath = existingPath.trim()
    val sourceUri = runCatching { Uri.parse(sourceUriString) }.getOrNull()
        ?: return@withContext null
    val sourceFilePath = sourceUri
        .takeIf { it.scheme.equals("file", ignoreCase = true) }
        ?.path
        ?.let { path -> runCatching { File(path).canonicalPath }.getOrNull() }
    if (normalizedExistingPath.isNotEmpty()) {
        val existingFile = File(normalizedExistingPath)
        val existingCanonicalPath = runCatching { existingFile.canonicalPath }.getOrNull()
        val existingIsSourceFile = existingCanonicalPath != null && existingCanonicalPath == sourceFilePath
        val isCurrentVersionCache = isReusableStampAutoCutoutAssetPath(existingFile.absolutePath)
        if (existingFile.exists()) {
            val existingBitmap = runCatching {
                BitmapFactory.decodeFile(existingFile.absolutePath)
            }.getOrNull()?.takeUnless { it.isRecycled || it.width <= 0 || it.height <= 0 }
            val existingUsable = isUsableAutoCutoutForegroundBitmap(existingBitmap)
            if (existingBitmap != null && !existingBitmap.isRecycled) {
                existingBitmap.recycle()
            }
            if ((existingUsable && isCurrentVersionCache) || existingIsSourceFile) {
                return@withContext normalizedExistingPath
            }
        }
    }
    val sourceBitmap = loadStampSourceBitmap(
        context = context,
        uri = sourceUri,
        maxSide = 1440,
        detachFromSourceCache = true
    ) ?: return@withContext null
    val foregroundBitmap = extractStampAutoForegroundBitmap(
        source = sourceBitmap,
        context = context
    )
    if (foregroundBitmap == null) return@withContext null
    val storedPath = saveStampAutoCutoutBitmapToLocalStore(
        context = context,
        bitmap = foregroundBitmap
    )
    if (!foregroundBitmap.isRecycled) {
        foregroundBitmap.recycle()
    }
    val verifiedPath = storedPath?.takeIf { path ->
        val savedBitmap = runCatching {
            BitmapFactory.decodeFile(path)
        }.getOrNull()?.takeUnless { it.isRecycled || it.width <= 0 || it.height <= 0 }
        val usable = isUsableAutoCutoutForegroundBitmap(savedBitmap)
        if (savedBitmap != null && !savedBitmap.isRecycled) {
            savedBitmap.recycle()
        }
        usable
    }
    if (verifiedPath == null && storedPath != null) {
        runCatching { File(storedPath).delete() }
    }
    return@withContext verifiedPath
}

internal suspend fun generateStampAutoCutoutForegroundBitmap(
    context: Context,
    source: Bitmap
): Bitmap? = withContext(Dispatchers.Default) {
    extractStampAutoForegroundBitmap(
        source = source,
        context = context
    )
}

internal suspend fun warmUpStampAutoCutoutModel(context: Context): Boolean =
    withContext(Dispatchers.Default) {
        StampAutoCutoutModelSupport.warmUp(context)
    }

internal fun renderStampBitmap(
    source: Bitmap,
    spec: StampRenderSpec,
    outputWidth: Int,
    overlayBitmap: Bitmap? = null
): Bitmap {
    val width = outputWidth.coerceAtLeast(720)
    val height = (width * 1.24f).roundToInt()
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = spec.theme.backgroundColor
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = spec.theme.borderColor
        style = Paint.Style.STROKE
        strokeWidth = width * 0.008f
    }
    val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = spec.theme.inkColor
        textAlign = Paint.Align.LEFT
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = spec.theme.accentColor
    }
    val accentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.blendARGB(spec.theme.backgroundColor, 0xFFFFFFFF.toInt(), 0.92f)
        textAlign = Paint.Align.CENTER
        textSize = width * 0.05f
        isFakeBoldText = true
    }
    val cardInset = width * 0.048f
    val cardRect = RectF(cardInset, cardInset, width - cardInset, height - cardInset)
    val cardRadius = width * 0.065f
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, backgroundPaint)
    if (overlayBitmap != null) {
        drawStampOverlay(canvas, overlayBitmap, cardRect)
    } else {
        drawPerforation(canvas, cardRect, clearPaint)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)
    }

    val photoRect = RectF(
        cardRect.left + width * 0.095f,
        cardRect.top + height * 0.145f,
        cardRect.right - width * 0.095f,
        cardRect.top + height * 0.69f
    )
    val photoRadius = width * 0.04f
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(spec.theme.inkColor, 18)
    }
    canvas.drawRoundRect(
        RectF(
            photoRect.left,
            photoRect.top + width * 0.015f,
            photoRect.right,
            photoRect.bottom + width * 0.015f
        ),
        photoRadius,
        photoRadius,
        shadowPaint
    )
    drawCroppedBitmap(canvas, source, photoRect, photoRadius)

    inkPaint.textSize = width * 0.056f
    inkPaint.isFakeBoldText = true
    canvas.drawText(
        "PHOTOO POST",
        cardRect.left + width * 0.09f,
        cardRect.top + height * 0.11f,
        inkPaint
    )

    val stampBadgeRect = RectF(
        cardRect.right - width * 0.23f,
        cardRect.top + height * 0.055f,
        cardRect.right - width * 0.09f,
        cardRect.top + height * 0.17f
    )
    canvas.drawRoundRect(stampBadgeRect, width * 0.03f, width * 0.03f, accentPaint)
    canvas.drawText(
        spec.valueLabel,
        stampBadgeRect.centerX(),
        stampBadgeRect.centerY() + accentTextPaint.textSize * 0.34f,
        accentTextPaint
    )

    inkPaint.isFakeBoldText = true
    inkPaint.textSize = width * 0.073f
    canvas.drawText(
        spec.title.take(16).ifBlank { "Stamp" },
        cardRect.left + width * 0.09f,
        cardRect.top + height * 0.8f,
        inkPaint
    )

    inkPaint.isFakeBoldText = false
    inkPaint.textSize = width * 0.034f
    canvas.drawText(
        spec.subtitle,
        cardRect.left + width * 0.09f,
        cardRect.top + height * 0.855f,
        inkPaint
    )
    canvas.drawText(
        spec.serialLabel,
        cardRect.left + width * 0.09f,
        cardRect.bottom - height * 0.06f,
        inkPaint
    )

    val postmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(spec.theme.accentColor, if (spec.theme.key == "night") 172 else 118)
        style = Paint.Style.STROKE
        strokeWidth = width * 0.008f
    }
    val postmarkCenterX = cardRect.right - width * 0.19f
    val postmarkCenterY = cardRect.bottom - height * 0.12f
    val postmarkRadius = width * 0.095f
    canvas.drawCircle(postmarkCenterX, postmarkCenterY, postmarkRadius, postmarkPaint)
    canvas.drawCircle(postmarkCenterX, postmarkCenterY, postmarkRadius * 0.73f, postmarkPaint)
    inkPaint.textAlign = Paint.Align.CENTER
    inkPaint.textSize = width * 0.032f
    canvas.drawText("AIR MAIL", postmarkCenterX, postmarkCenterY + inkPaint.textSize * 0.34f, inkPaint)
    inkPaint.textAlign = Paint.Align.LEFT

    return bitmap
}

internal fun renderStampCameraCutoutPreviewBitmap(
    context: Context,
    foreground: Bitmap,
    outputWidth: Int = 1180
): Bitmap? {
    if (foreground.isRecycled || foreground.width <= 0 || foreground.height <= 0) return null
    return runCatching {
        val width = outputWidth.coerceAtLeast(720)
        val height = (width / StampPunchStyle.One.aspectRatio).roundToInt().coerceAtLeast(1)
        val carrier = createBitmap(width, height)
        val rendered = renderStampCardSkinBitmap(
            source = carrier,
            skin = StampCalendarStampSkin(
                style = StampCalendarSkinStyle.Cutout,
                backgroundColorArgb = resolveStampSkinDefaultBackgroundColor(
                    StampCalendarSkinStyle.Cutout
                )
            ),
            context = context,
            cutoutForegroundOverride = foreground
        )
        if (!carrier.isRecycled) {
            carrier.recycle()
        }
        rendered
    }.getOrNull()
}

internal fun renderStampCalendarDetailExportBitmap(
    context: Context? = null,
    stampBitmap: Bitmap,
    dateLabel: String,
    emoji: String? = null,
    note: String? = null,
    skin: StampCalendarStampSkin? = null,
    logoBitmap: Bitmap? = null,
    outputWidth: Int = 1440,
    backgroundColor: Int = 0xFFF6F2E7.toInt()
): Bitmap {
    val renderedStampBitmap = if (skin.isOriginalStyle()) {
        stampBitmap
    } else {
        renderStampCardSkinBitmap(
            source = stampBitmap,
            skin = skin.normalized(),
            categoryEmoji = emoji,
            context = context
        )
    }
    val width = outputWidth.coerceAtLeast(1080)
    val height = ((width * 4f) / 3f).roundToInt()
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)
    drawStampCalendarExportGrid(
        canvas = canvas,
        width = width,
        height = height,
        lineColor = 0xFFE1DAC9.toInt(),
        spacing = width / 17f
    )

    val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF181512.toInt()
        textAlign = Paint.Align.CENTER
        textSize = width * 0.071f
        isFakeBoldText = true
    }
    val infoTextSize = width * 0.041f
    val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF181512.toInt()
        textAlign = Paint.Align.LEFT
        textSize = infoTextSize
    }
    val notePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6F6258.toInt()
        textAlign = Paint.Align.LEFT
        textSize = infoTextSize
    }

    val maxStampWidth = width * 0.56f
    val maxStampHeight = height * 0.46f
    val stampScale = minOf(
        maxStampWidth / renderedStampBitmap.width.toFloat(),
        maxStampHeight / renderedStampBitmap.height.toFloat()
    )
    val drawWidth = renderedStampBitmap.width * stampScale
    val drawHeight = renderedStampBitmap.height * stampScale
    val stampCenterY = height * 0.34f
    val stampRect = RectF(
        (width - drawWidth) / 2f,
        stampCenterY - (drawHeight / 2f),
        (width + drawWidth) / 2f,
        stampCenterY + (drawHeight / 2f)
    )
    drawStampCalendarExportShadow(
        canvas = canvas,
        source = renderedStampBitmap,
        destination = stampRect,
        shadowColor = ColorUtils.setAlphaComponent(0xFF2E261C.toInt(), 76),
        blurRadius = width * 0.024f,
        offsetX = width * 0.012f,
        offsetY = height * 0.022f
    )
    drawStampCalendarExportShadow(
        canvas = canvas,
        source = renderedStampBitmap,
        destination = stampRect,
        shadowColor = ColorUtils.setAlphaComponent(0xFF3A3125.toInt(), 52),
        blurRadius = width * 0.008f,
        offsetX = width * 0.004f,
        offsetY = height * 0.008f
    )
    canvas.drawBitmap(renderedStampBitmap, null, stampRect, stampPaint)

    val dateBaseline = stampRect.bottom + (height * 0.08f)
    canvas.drawText(dateLabel, width / 2f, dateBaseline, datePaint)

    val emojiText = emoji?.trim()?.takeIf { it.isNotEmpty() }
    val noteText = note?.trim().orEmpty()
    val infoCenterY = dateBaseline + (height * 0.074f)
    val emojiMetrics = emojiPaint.fontMetrics
    val noteMetrics = notePaint.fontMetrics
    val emojiBaseline = infoCenterY - ((emojiMetrics.ascent + emojiMetrics.descent) / 2f)
    val noteBaseline = infoCenterY - ((noteMetrics.ascent + noteMetrics.descent) / 2f)
    val footerBaseline = height - (height * 0.088f)
    val footerGap = width * 0.018f
    val decodedLogoBitmap = logoBitmap?.takeUnless { it.isRecycled }
    val baseLogoHeight = infoTextSize * 1.75f
    val rawLogoWidth = decodedLogoBitmap?.let { bitmap ->
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
        baseLogoHeight * aspectRatio
    } ?: 0f
    val maxLogoWidth = infoTextSize * 3.8f
    val logoScale = if (rawLogoWidth > 0f) minOf(1f, maxLogoWidth / rawLogoWidth) else 1f
    val logoTargetWidth = rawLogoWidth * logoScale
    val logoTargetHeight = baseLogoHeight * logoScale
    val emojiWidth = if (emojiText != null) emojiPaint.measureText(emojiText) else 0f
    val hasEmoji = emojiText != null
    var leadingInfoWidth = 0f
    if (hasEmoji) {
        leadingInfoWidth += emojiWidth
        if (noteText.isNotEmpty()) {
            leadingInfoWidth += footerGap
        }
    }
    val noteAvailableWidth = (width * 0.8f) - leadingInfoWidth
    val ellipsizedNote = if (noteText.isNotEmpty()) {
        TextUtils.ellipsize(
            noteText,
            notePaint,
            noteAvailableWidth.coerceAtLeast(width * 0.18f),
            TextUtils.TruncateAt.END
        ).toString()
    } else {
        ""
    }
    val noteWidth = if (ellipsizedNote.isNotEmpty()) notePaint.measureText(ellipsizedNote) else 0f
    val hasNote = ellipsizedNote.isNotEmpty()
    var totalInfoWidth = 0f
    if (hasEmoji) {
        totalInfoWidth += emojiWidth
    }
    if (hasNote) {
        if (totalInfoWidth > 0f) {
            totalInfoWidth += footerGap
        }
        totalInfoWidth += noteWidth
    }
    val infoStartX = (width - totalInfoWidth) / 2f
    var infoCursorX = infoStartX
    if (hasEmoji) {
        canvas.drawText(emojiText, infoCursorX, emojiBaseline, emojiPaint)
        infoCursorX += emojiWidth
    }
    if (hasNote) {
        if (infoCursorX > infoStartX) {
            infoCursorX += footerGap
        }
        canvas.drawText(ellipsizedNote, infoCursorX, noteBaseline, notePaint)
    }

    if (decodedLogoBitmap != null) {
        val footerCenterY = footerBaseline + ((noteMetrics.ascent + noteMetrics.descent) / 2f)
        val logoTop = footerCenterY - (logoTargetHeight / 2f)
        val logoLeft = (width - logoTargetWidth) / 2f
        val logoRect = RectF(
            logoLeft,
            logoTop,
            logoLeft + logoTargetWidth,
            logoTop + logoTargetHeight
        )
        canvas.drawBitmap(decodedLogoBitmap, null, logoRect, stampPaint)
    }

    return bitmap
}

internal fun renderStampCardSkinBitmap(
    source: Bitmap,
    skin: StampCalendarStampSkin,
    categoryEmoji: String? = null,
    context: Context? = null,
    cutoutForegroundOverride: Bitmap? = null
): Bitmap {
    if (source.isRecycled) return source
    val normalizedSkin = skin.normalized()
    val normalizedCategoryEmoji: String? = null
    val stylizedSource = applyStampImageEffectIfNeeded(
        source = source,
        effect = normalizedSkin.imageEffect
    )
    if (normalizedSkin.style == StampCalendarSkinStyle.Original) {
        return stylizedSource
    }
    val width = source.width.coerceAtLeast(1)
    val height = source.height.coerceAtLeast(1)
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val shellColor = normalizeStampSkinBackgroundColor(
        style = normalizedSkin.style,
        requestedColor = normalizedSkin.backgroundColorArgb
    ).takeIf { normalizedSkin.style != StampCalendarSkinStyle.Original } ?: 0xFFFFFFFF.toInt()
    val stampShellBitmap = if (normalizedSkin.style == StampCalendarSkinStyle.Cutout) {
        context?.let {
            createStandardStampShapeFillBitmap(
                context = it,
                width = width,
                height = height,
                fillColor = shellColor
            )
        } ?: createStampShapeFillBitmap(
            source = source,
            fillColor = shellColor
        )
    } else {
        createStampShapeFillBitmap(
            source = source,
            fillColor = shellColor
        )
    }
    if (stampShellBitmap != null) {
        canvas.drawBitmap(stampShellBitmap, 0f, 0f, null)
    } else {
        canvas.drawBitmap(source, 0f, 0f, null)
    }
    val opaqueBounds = findOpaqueBitmapBounds(stampShellBitmap ?: source)?.let { RectF(it) }
        ?: RectF(0f, 0f, width.toFloat(), height.toFloat())

    val contentBitmap = extractStampInnerContentBitmap(
        source = stylizedSource,
        insetFraction = 0.14f
    ) ?: stylizedSource

    val minOpaqueDimension = minOf(
        opaqueBounds.width(),
        opaqueBounds.height()
    ).coerceAtLeast(1f)
    val uniformBorder = (minOpaqueDimension * 0.1f)
        .coerceAtLeast(12f)
        .coerceAtMost(minOpaqueDimension * 0.42f)

    when (normalizedSkin.style) {
        StampCalendarSkinStyle.WhiteBorder -> {
            val photoRect = RectF(
                opaqueBounds.left + uniformBorder,
                opaqueBounds.top + uniformBorder,
                opaqueBounds.right - uniformBorder,
                opaqueBounds.bottom - uniformBorder
            )
            if (photoRect.width() > 2f && photoRect.height() > 2f) {
                drawBitmapContain(
                    canvas = canvas,
                    source = contentBitmap,
                    destination = photoRect
                )
            }
            if (!normalizedCategoryEmoji.isNullOrBlank()) {
                drawStampCategoryEmojiBadge(
                    canvas = canvas,
                    emoji = normalizedCategoryEmoji,
                    bounds = opaqueBounds,
                    borderHint = uniformBorder
                )
            }
        }
        StampCalendarSkinStyle.Editorial -> {
            val bottomBorder = (uniformBorder * 2.8f)
                .coerceAtLeast(minOpaqueDimension * 0.24f)
                .coerceAtMost(opaqueBounds.height() * 0.58f)
            val photoBottom = (opaqueBounds.bottom - bottomBorder)
                .coerceAtLeast(opaqueBounds.top + uniformBorder + 24f)
            val photoRect = RectF(
                opaqueBounds.left + uniformBorder,
                opaqueBounds.top + uniformBorder,
                opaqueBounds.right - uniformBorder,
                photoBottom
            )
            if (photoRect.width() > 2f && photoRect.height() > 2f) {
                drawBitmapContain(
                    canvas = canvas,
                    source = contentBitmap,
                    destination = photoRect
                )
            }

            val toothShortEdgeY = estimateStampBottomShortEdgeY(
                source = source,
                bounds = opaqueBounds
            )
            val textBandTop = photoRect.bottom
            val textBandBottom = toothShortEdgeY
                .coerceAtLeast(textBandTop + uniformBorder * 0.34f)
                .coerceAtMost(opaqueBounds.bottom - uniformBorder * 0.05f)
            val textBandHeight = (textBandBottom - textBandTop).coerceAtLeast(6f)
            val centerShiftUp = (uniformBorder * 0.06f).coerceAtMost(textBandHeight * 0.22f)
            val alignedCenterY = ((textBandTop + textBandBottom) * 0.5f - centerShiftUp)
                .coerceIn(textBandTop + 2f, textBandBottom - 2f)
            val textAreaHalfHeight = (textBandHeight * 0.44f).coerceAtLeast(4f)
            val bottomTextArea = RectF(
                photoRect.left,
                (alignedCenterY - textAreaHalfHeight).coerceAtLeast(textBandTop),
                photoRect.right,
                (alignedCenterY + textAreaHalfHeight).coerceAtMost(textBandBottom)
            )
            if (bottomTextArea.height() > 4f) {
                val numberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF1C1712.toInt()
                    textAlign = Paint.Align.LEFT
                    textSize = (minOpaqueDimension * 0.172f).coerceAtLeast(21f)
                    textScaleX = 0.84f
                    letterSpacing = -0.03f
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
                val numberText = STAMP_NUMERIC_STYLE_FIXED_VALUE
                val numberMaxWidth = (bottomTextArea.width() * 0.42f)
                    .coerceAtLeast(20f)
                val numberMaxHeight = (bottomTextArea.height() * 0.88f)
                    .coerceAtLeast(16f)
                fitTextPaintToBounds(
                    paint = numberPaint,
                    text = numberText,
                    maxWidth = numberMaxWidth,
                    maxHeight = numberMaxHeight,
                    minTextSize = 14f
                )
                val numberX = bottomTextArea.left + uniformBorder * 0.08f
                val numberMetrics = numberPaint.fontMetrics
                val numberBaseline =
                    bottomTextArea.centerY() - ((numberMetrics.ascent + numberMetrics.descent) * 0.5f)
                val numberDrawWidth = numberPaint.measureText(numberText)
                canvas.drawText(numberText, numberX, numberBaseline, numberPaint)

                val rightTextArea = RectF(
                    (numberX + numberDrawWidth + uniformBorder * 0.14f)
                        .coerceAtMost(bottomTextArea.right - uniformBorder * 0.08f),
                    bottomTextArea.top,
                    bottomTextArea.right - uniformBorder * 0.08f,
                    bottomTextArea.bottom
                )
                val rightTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF2A241C.toInt()
                    textSize = (minOpaqueDimension * 0.067f).coerceAtLeast(12f)
                    isFakeBoldText = true
                }
                drawRightAlignedWrappedTextBlock(
                    canvas = canvas,
                    text = normalizedSkin.title.ifBlank { STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT },
                    area = rightTextArea,
                    paint = rightTextPaint,
                    maxLines = 2
                )
            }
            if (!normalizedCategoryEmoji.isNullOrBlank()) {
                drawStampCategoryEmojiBadge(
                    canvas = canvas,
                    emoji = normalizedCategoryEmoji,
                    bounds = opaqueBounds,
                    borderHint = uniformBorder
                )
            }
        }
        StampCalendarSkinStyle.Cutout -> {
            val photoRect = RectF(
                opaqueBounds.left + uniformBorder,
                opaqueBounds.top + uniformBorder,
                opaqueBounds.right - uniformBorder,
                opaqueBounds.bottom - uniformBorder
            )
            if (photoRect.width() > 2f && photoRect.height() > 2f) {
                val foregroundBitmapFromFile = cutoutForegroundOverride
                    ?.takeUnless { it.isRecycled || it.width <= 0 || it.height <= 0 }
                    ?: normalizedSkin.cutoutAssetPath
                        .takeIf { it.isNotBlank() && isReusableStampAutoCutoutAssetPath(it) }
                        ?.let { path ->
                            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                        }
                        ?.takeUnless { it.isRecycled || it.width <= 0 || it.height <= 0 }
                if (!normalizedCategoryEmoji.isNullOrBlank()) {
                    drawStampCategoryEmojiBadge(
                        canvas = canvas,
                        emoji = normalizedCategoryEmoji,
                        bounds = opaqueBounds,
                        borderHint = uniformBorder
                    )
                }
                val rawForegroundBitmap = foregroundBitmapFromFile
                    ?.takeIf { isUsableAutoCutoutForegroundBitmap(it) }
                val generatedForegroundBitmap = rawForegroundBitmap?.let { raw ->
                    applyStampImageEffectIfNeeded(
                        source = raw,
                        effect = normalizedSkin.imageEffect
                    )
                }
                if (
                    rawForegroundBitmap != null &&
                    rawForegroundBitmap !== foregroundBitmapFromFile &&
                    rawForegroundBitmap !== generatedForegroundBitmap &&
                    !rawForegroundBitmap.isRecycled
                ) {
                    rawForegroundBitmap.recycle()
                }
                val foregroundBitmap = generatedForegroundBitmap
                    ?.takeIf { isUsableAutoCutoutForegroundBitmap(it) }
                if (foregroundBitmap != null) {
                    drawStickerCutoutContain(
                        canvas = canvas,
                        source = foregroundBitmap,
                        destination = photoRect
                    )
                } else {
                    drawBitmapContain(
                        canvas = canvas,
                        source = contentBitmap,
                        destination = photoRect
                    )
                }
                if (
                    generatedForegroundBitmap != null &&
                    generatedForegroundBitmap !== foregroundBitmapFromFile &&
                    !generatedForegroundBitmap.isRecycled
                ) {
                    generatedForegroundBitmap.recycle()
                }
                if (
                    foregroundBitmapFromFile != null &&
                    foregroundBitmapFromFile !== cutoutForegroundOverride &&
                    !foregroundBitmapFromFile.isRecycled
                ) {
                    foregroundBitmapFromFile.recycle()
                }
            }
        }
        StampCalendarSkinStyle.Original -> return stylizedSource
    }
    if (stampShellBitmap != null && !stampShellBitmap.isRecycled) {
        stampShellBitmap.recycle()
    }
    if (contentBitmap !== stylizedSource && !contentBitmap.isRecycled) {
        contentBitmap.recycle()
    }
    if (stylizedSource !== source && !stylizedSource.isRecycled) {
        stylizedSource.recycle()
    }
    return bitmap
}

private fun createStampShapeFillBitmap(
    source: Bitmap,
    fillColor: Int
): Bitmap? {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
    return runCatching {
        val filled = createBitmap(source.width, source.height)
        val canvas = Canvas(filled)
        canvas.drawBitmap(source, 0f, 0f, null)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), fillPaint)
        fillPaint.xfermode = null
        filled
    }.getOrNull()
}

private fun createStandardStampShapeFillBitmap(
    context: Context,
    width: Int,
    height: Int,
    fillColor: Int
): Bitmap? {
    val template = loadStampPunchTemplate(context, StampPunchStyle.One) ?: return null
    val shapeMask = try {
        createScaledStampShapeMask(
            template = template,
            targetWidth = width,
            targetHeight = height
        )
    } finally {
        if (!template.overlayBitmap.isRecycled) {
            template.overlayBitmap.recycle()
        }
        if (!template.holeMaskBitmap.isRecycled) {
            template.holeMaskBitmap.recycle()
        }
    } ?: return null
    return try {
        createAlphaShapeFillBitmap(shapeMask, fillColor)
    } finally {
        if (!shapeMask.isRecycled) {
            shapeMask.recycle()
        }
    }
}

private fun drawRightAlignedWrappedTextBlock(
    canvas: Canvas,
    text: String,
    area: RectF,
    paint: TextPaint,
    maxLines: Int
) {
    if (text.isBlank() || area.width() <= 1f || area.height() <= 1f) return
    val layout = StaticLayout.Builder
        .obtain(
            text,
            0,
            text.length,
            paint,
            area.width().roundToInt().coerceAtLeast(1)
        )
        .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
        .setIncludePad(false)
        .setLineSpacing(0f, 1.08f)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setMaxLines(maxLines.coerceAtLeast(1))
        .build()
    val offsetY = area.top + ((area.height() - layout.height) * 0.5f).coerceAtLeast(0f)
    canvas.save()
    canvas.translate(area.left, offsetY)
    layout.draw(canvas)
    canvas.restore()
}

private fun fitTextPaintToBounds(
    paint: TextPaint,
    text: String,
    maxWidth: Float,
    maxHeight: Float,
    minTextSize: Float
) {
    if (text.isBlank()) return
    if (maxWidth <= 1f || maxHeight <= 1f) return
    val floorSize = minTextSize.coerceAtLeast(8f)
    repeat(4) {
        val measuredWidth = paint.measureText(text).coerceAtLeast(1f)
        val metrics = paint.fontMetrics
        val measuredHeight = (metrics.descent - metrics.ascent).coerceAtLeast(1f)
        val widthScale = (maxWidth / measuredWidth).coerceAtLeast(0.01f)
        val heightScale = (maxHeight / measuredHeight).coerceAtLeast(0.01f)
        val scale = minOf(widthScale, heightScale, 1f)
        if (scale >= 0.995f) return
        val nextSize = (paint.textSize * scale).coerceAtLeast(floorSize)
        if (nextSize >= paint.textSize - 0.1f) return
        paint.textSize = nextSize
    }
}

private fun drawStampCategoryEmojiBadge(
    canvas: Canvas,
    emoji: String,
    bounds: RectF,
    borderHint: Float
) {
    if (emoji.isBlank()) return
    if (bounds.width() <= 2f || bounds.height() <= 2f) return
    val badgeSize = (min(bounds.width(), bounds.height()) * 0.2f)
        .coerceIn(30f, 96f)
    val inset = (borderHint * 0.22f).coerceIn(4f, 22f)
    val left = (bounds.left + inset).coerceAtMost(bounds.right - badgeSize)
    val top = (bounds.top + inset).coerceAtMost(bounds.bottom - badgeSize)
    val rect = RectF(
        left,
        top,
        left + badgeSize,
        top + badgeSize
    )
    val radius = badgeSize * 0.34f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), 236)
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (badgeSize * 0.045f).coerceIn(1.1f, 2.4f)
        color = ColorUtils.setAlphaComponent(0xFF1D1713.toInt(), 42)
    }
    canvas.drawRoundRect(rect, radius, radius, bgPaint)
    canvas.drawRoundRect(rect, radius, radius, strokePaint)
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = (badgeSize * 0.62f).coerceIn(18f, 58f)
    }
    val baseline = rect.centerY() - ((textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) * 0.5f)
    canvas.drawText(emoji, rect.centerX(), baseline, textPaint)
}

private fun drawBitmapContain(
    canvas: Canvas,
    source: Bitmap,
    destination: RectF
) {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return
    if (destination.width() <= 0f || destination.height() <= 0f) return
    val scale = minOf(
        destination.width() / source.width.toFloat(),
        destination.height() / source.height.toFloat()
    )
    val drawWidth = source.width * scale
    val drawHeight = source.height * scale
    val left = destination.left + (destination.width() - drawWidth) * 0.5f
    val top = destination.top + (destination.height() - drawHeight) * 0.5f
    val drawRect = RectF(
        left,
        top,
        left + drawWidth,
        top + drawHeight
    )
    canvas.drawBitmap(source, null, drawRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
}

private fun drawStickerCutoutContain(
    canvas: Canvas,
    source: Bitmap,
    destination: RectF
) {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return
    if (destination.width() <= 0f || destination.height() <= 0f) return

    val trimmedSource = findOpaqueBitmapBounds(source)?.let { bounds ->
        runCatching {
            Bitmap.createBitmap(
                source,
                bounds.left,
                bounds.top,
                bounds.width().coerceAtLeast(1),
                bounds.height().coerceAtLeast(1)
            )
        }.getOrNull()
    } ?: source

    val stickerPadding = (minOf(trimmedSource.width, trimmedSource.height) * 0.14f)
        .roundToInt()
        .coerceIn(12, 220)
    val paddedSource = runCatching {
        val output = createBitmap(
            (trimmedSource.width + stickerPadding * 2).coerceAtLeast(1),
            (trimmedSource.height + stickerPadding * 2).coerceAtLeast(1)
        )
        Canvas(output).drawBitmap(
            trimmedSource,
            stickerPadding.toFloat(),
            stickerPadding.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        )
        output
    }.getOrNull() ?: trimmedSource

    val scale = minOf(
        destination.width() / paddedSource.width.toFloat().coerceAtLeast(1f),
        destination.height() / paddedSource.height.toFloat().coerceAtLeast(1f)
    )
    val drawWidth = (paddedSource.width * scale).roundToInt().coerceAtLeast(1)
    val drawHeight = (paddedSource.height * scale).roundToInt().coerceAtLeast(1)
    val left = destination.left + (destination.width() - drawWidth) * 0.5f
    val top = destination.top + (destination.height() - drawHeight) * 0.5f

    val scaledSource = if (paddedSource.width == drawWidth && paddedSource.height == drawHeight) {
        paddedSource
    } else {
        Bitmap.createScaledBitmap(paddedSource, drawWidth, drawHeight, true)
    }

    drawStickerWhiteBacking(
        canvas = canvas,
        source = scaledSource,
        left = left,
        top = top
    )
    canvas.drawBitmap(
        scaledSource,
        left,
        top,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    )

    if (scaledSource !== trimmedSource && !scaledSource.isRecycled) {
        scaledSource.recycle()
    }
    if (paddedSource !== trimmedSource && !paddedSource.isRecycled) {
        paddedSource.recycle()
    }
    if (trimmedSource !== source && !trimmedSource.isRecycled) {
        trimmedSource.recycle()
    }
}

private fun drawStickerWhiteBacking(
    canvas: Canvas,
    source: Bitmap,
    left: Float,
    top: Float
) {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFilterBitmap = true
    }

    val expansionRadius = (minOf(source.width, source.height) * 0.07f)
        .coerceAtLeast(5f)
        .coerceAtMost(48f)
    val solidWhiteShape = createSolidExpandedWhiteShapeBitmap(
        source = source,
        expansionRadius = expansionRadius,
        alphaThreshold = 20
    ) ?: createAlphaShapeFillBitmap(source, 0xFFFFFFFF.toInt())
    if (solidWhiteShape != null) {
        canvas.drawBitmap(solidWhiteShape, left, top, whitePaint)
        if (!solidWhiteShape.isRecycled) {
            solidWhiteShape.recycle()
        }
    }
}

private fun createAlphaShapeFillBitmap(
    source: Bitmap,
    fillColor: Int
): Bitmap? {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
    return runCatching {
        val output = createBitmap(source.width, source.height)
        val canvas = Canvas(output)
        canvas.drawBitmap(source, 0f, 0f, null)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), fillPaint)
        fillPaint.xfermode = null
        output
    }.getOrNull()
}

private fun createSolidExpandedWhiteShapeBitmap(
    source: Bitmap,
    expansionRadius: Float,
    alphaThreshold: Int
): Bitmap? {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
    val alphaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(expansionRadius.coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
    }
    val offset = IntArray(2)
    val blurredAlpha = runCatching { source.extractAlpha(alphaPaint, offset) }.getOrNull()
    alphaPaint.maskFilter = null
    if (blurredAlpha == null || blurredAlpha.isRecycled) return null

    val thresholded = thresholdAlphaBitmapToSolidWhite(
        source = blurredAlpha,
        threshold = alphaThreshold.coerceIn(1, 254)
    )
    val output = createBitmap(source.width, source.height)
    Canvas(output).drawBitmap(
        thresholded,
        offset[0].toFloat(),
        offset[1].toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    )
    if (!thresholded.isRecycled) {
        thresholded.recycle()
    }
    if (!blurredAlpha.isRecycled) {
        blurredAlpha.recycle()
    }
    return output
}

private fun thresholdAlphaBitmapToSolidWhite(
    source: Bitmap,
    threshold: Int
): Bitmap {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) {
        return createBitmap(1, 1)
    }
    val width = source.width
    val height = source.height
    val alphaPixels = IntArray(width * height)
    source.getPixels(alphaPixels, 0, width, 0, 0, width, height)
    var current = BooleanArray(width * height)
    var buffer = BooleanArray(width * height)
    for (index in alphaPixels.indices) {
        val alpha = alphaPixels[index] ushr 24
        current[index] = alpha >= threshold
    }
    repeat(2) {
        for (y in 0 until height) {
            val top = (y - 1).coerceAtLeast(0)
            val bottom = (y + 1).coerceAtMost(height - 1)
            for (x in 0 until width) {
                val left = (x - 1).coerceAtLeast(0)
                val right = (x + 1).coerceAtMost(width - 1)
                var onCount = 0
                var count = 0
                for (ny in top..bottom) {
                    val rowOffset = ny * width
                    for (nx in left..right) {
                        if (current[rowOffset + nx]) {
                            onCount += 1
                        }
                        count += 1
                    }
                }
                buffer[y * width + x] = onCount * 2 >= count
            }
        }
        val swap = current
        current = buffer
        buffer = swap
    }
    val outputPixels = IntArray(width * height)
    for (index in outputPixels.indices) {
        outputPixels[index] = if (current[index]) 0xFFFFFFFF.toInt() else 0
    }
    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun extractStampAutoForegroundBitmap(
    source: Bitmap,
    context: Context? = null
): Bitmap? {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
    val contentBitmap = extractStampAutoCutoutSourceBitmap(source) ?: source
    val modelOutput = if (context != null) {
        takeUsableAutoCutoutBitmapOrRecycle(
            StampAutoCutoutModelSupport.generateAutoCutout(
                context = context,
                sourceBitmap = contentBitmap
            )
        )
    } else {
        null
    }
    val rawOutput = modelOutput ?: takeUsableAutoCutoutBitmapOrRecycle(
        buildHeuristicStampAutoForegroundBitmap(contentBitmap)
    )
    if (contentBitmap !== source && !contentBitmap.isRecycled) {
        contentBitmap.recycle()
    }
    return finalizeSimpleAutoCutoutBitmap(rawOutput)
}

private fun extractStampAutoCutoutSourceBitmap(source: Bitmap): Bitmap? {
    if (source.isRecycled || source.width <= 1 || source.height <= 1) return null
    val insetFraction = if (hasTransparentStampPerforationEdge(source)) {
        0.06f
    } else {
        0f
    }
    return extractStampInnerContentBitmap(source, insetFraction = insetFraction)
}

private fun hasTransparentStampPerforationEdge(
    source: Bitmap,
    alphaThreshold: Int = 12
): Boolean {
    if (source.isRecycled || source.width <= 8 || source.height <= 8) return false
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val band = (minOf(width, height) * 0.08f).roundToInt().coerceIn(3, 48)
    var transparentCount = 0
    var sampledCount = 0
    var opaqueCount = 0

    fun sample(x: Int, y: Int) {
        val alpha = pixels[y * width + x] ushr 24
        sampledCount += 1
        if (alpha <= alphaThreshold) {
            transparentCount += 1
        } else {
            opaqueCount += 1
        }
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (x < band || x >= width - band || y < band || y >= height - band) {
                sample(x, y)
            }
        }
    }
    if (sampledCount <= 0 || opaqueCount <= 0) return false
    val transparentRatio = transparentCount.toFloat() / sampledCount.toFloat()
    return transparentRatio >= 0.12f
}

private fun takeUsableAutoCutoutBitmapOrRecycle(bitmap: Bitmap?): Bitmap? {
    if (bitmap == null || bitmap.isRecycled) return null
    if (isUsableAutoCutoutForegroundBitmap(bitmap)) {
        return bitmap
    }
    if (!bitmap.isRecycled) {
        bitmap.recycle()
    }
    return null
}

private fun finalizeSimpleAutoCutoutBitmap(bitmap: Bitmap?): Bitmap? {
    if (bitmap == null || bitmap.isRecycled) return null
    val editableBitmap = ensureMutableArgb8888Bitmap(bitmap) ?: bitmap
    if (editableBitmap !== bitmap && !bitmap.isRecycled) {
        bitmap.recycle()
    }
    if (editableBitmap.isMutable) {
        stabilizeForegroundAlphaInPlace(
            bitmap = editableBitmap,
            alphaThreshold = 12,
            closePasses = 1
        )
        fillEnclosedAlphaHolesInBitmapInPlace(
            bitmap = editableBitmap,
            alphaThreshold = 12,
            fillAlpha = 255
        )
    }
    if (!isUsableAutoCutoutForegroundBitmap(editableBitmap, alphaThreshold = 12)) {
        if (!editableBitmap.isRecycled) {
            editableBitmap.recycle()
        }
        return null
    }
    return editableBitmap
}

private fun buildHeuristicStampAutoForegroundBitmap(
    contentBitmap: Bitmap
): Bitmap? {
    if (contentBitmap.isRecycled || contentBitmap.width <= 0 || contentBitmap.height <= 0) return null
    val downscaledMaxSide = 320f
    val scale = (downscaledMaxSide / max(contentBitmap.width, contentBitmap.height).toFloat())
        .coerceAtMost(1f)
        .coerceAtLeast(0.05f)
    val downscaled = if (scale < 0.999f) {
        Bitmap.createScaledBitmap(
            contentBitmap,
            (contentBitmap.width * scale).roundToInt().coerceAtLeast(1),
            (contentBitmap.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    } else {
        contentBitmap
    }
    val smallMask = buildAutoForegroundMask(downscaled)
    val fullMask = when {
        smallMask == null -> null
        smallMask.width == contentBitmap.width && smallMask.height == contentBitmap.height -> smallMask
        else -> Bitmap.createScaledBitmap(
            smallMask,
            contentBitmap.width,
            contentBitmap.height,
            true
        ).also {
            if (!smallMask.isRecycled) {
                smallMask.recycle()
            }
        }
    }
    if (fullMask == null) {
        if (downscaled !== contentBitmap && !downscaled.isRecycled) {
            downscaled.recycle()
        }
        return null
    }
    val output = createBitmap(contentBitmap.width, contentBitmap.height)
    val canvas = Canvas(output)
    canvas.drawBitmap(contentBitmap, 0f, 0f, null)
    val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    canvas.drawBitmap(fullMask, 0f, 0f, maskPaint)
    maskPaint.xfermode = null
    if (!fullMask.isRecycled) {
        fullMask.recycle()
    }
    if (downscaled !== contentBitmap && !downscaled.isRecycled) {
        downscaled.recycle()
    }
    if (!isUsableAutoCutoutForegroundBitmap(output)) {
        if (!output.isRecycled) {
            output.recycle()
        }
        return null
    }
    return output
}

private fun isUsableAutoCutoutForegroundBitmap(
    bitmap: Bitmap?,
    alphaThreshold: Int = 16
): Boolean {
    if (bitmap == null || bitmap.isRecycled || bitmap.width <= 1 || bitmap.height <= 1) {
        return false
    }
    val width = bitmap.width
    val height = bitmap.height
    val total = width * height
    val pixels = IntArray(total)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    var opaqueCount = 0
    for (index in pixels.indices) {
        if ((pixels[index] ushr 24) > alphaThreshold) {
            opaqueCount += 1
        }
    }
    val minOpaqueCount = (total * 0.009f).roundToInt().coerceAtLeast(24)
    if (opaqueCount < minOpaqueCount) return false
    val opaqueRatio = opaqueCount.toFloat() / total.toFloat().coerceAtLeast(1f)
    if (opaqueRatio >= 0.998f) return false
    val bounds = findOpaqueBitmapBounds(bitmap, alphaThreshold = alphaThreshold) ?: return false
    if (bounds.width() <= 4 || bounds.height() <= 4) return false
    val boundsRatio = (bounds.width().toFloat() * bounds.height().toFloat()) /
        total.toFloat().coerceAtLeast(1f)
    return boundsRatio >= 0.016f
}

private fun computeLargestOpaqueComponentRatio(
    foreground: BooleanArray,
    width: Int,
    height: Int,
    opaqueCount: Int
): Float {
    if (foreground.isEmpty() || width <= 0 || height <= 0 || opaqueCount <= 0) return 0f
    val visited = BooleanArray(foreground.size)
    val queue = IntArray(foreground.size)
    var largest = 0
    for (seed in foreground.indices) {
        if (!foreground[seed] || visited[seed]) continue
        var head = 0
        var tail = 0
        var componentSize = 0
        queue[tail++] = seed
        visited[seed] = true
        while (head < tail) {
            val current = queue[head++]
            componentSize += 1
            val x = current % width
            val y = current / width
            val top = (y - 1).coerceAtLeast(0)
            val bottom = (y + 1).coerceAtMost(height - 1)
            val left = (x - 1).coerceAtLeast(0)
            val right = (x + 1).coerceAtMost(width - 1)
            for (ny in top..bottom) {
                val rowOffset = ny * width
                for (nx in left..right) {
                    if (nx == x && ny == y) continue
                    val neighbor = rowOffset + nx
                    if (foreground[neighbor] && !visited[neighbor]) {
                        visited[neighbor] = true
                        queue[tail++] = neighbor
                    }
                }
            }
        }
        if (componentSize > largest) {
            largest = componentSize
        }
    }
    return largest.toFloat() / opaqueCount.toFloat().coerceAtLeast(1f)
}

private fun buildAutoForegroundMask(source: Bitmap): Bitmap? {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
    val width = source.width
    val height = source.height
    val total = width * height
    val pixels = IntArray(total)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val borderThickness = (minOf(width, height) / 15).coerceIn(2, 18)
    var borderCount = 0
    var borderRed = 0L
    var borderGreen = 0L
    var borderBlue = 0L

    fun sampleBorderPixel(index: Int) {
        val color = pixels[index]
        val alpha = color ushr 24
        if (alpha <= 12) return
        borderRed += (color shr 16) and 0xFF
        borderGreen += (color shr 8) and 0xFF
        borderBlue += color and 0xFF
        borderCount += 1
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (x < borderThickness || x >= width - borderThickness ||
                y < borderThickness || y >= height - borderThickness
            ) {
                sampleBorderPixel(y * width + x)
            }
        }
    }
    if (borderCount <= 0) return null

    val meanR = borderRed.toFloat() / borderCount
    val meanG = borderGreen.toFloat() / borderCount
    val meanB = borderBlue.toFloat() / borderCount

    val distanceMap = FloatArray(total)
    var borderDistanceSum = 0.0
    var borderDistanceCount = 0
    for (index in pixels.indices) {
        val color = pixels[index]
        val alpha = color ushr 24
        if (alpha <= 12) {
            distanceMap[index] = 0f
            continue
        }
        val r = ((color shr 16) and 0xFF).toFloat()
        val g = ((color shr 8) and 0xFF).toFloat()
        val b = (color and 0xFF).toFloat()
        val dr = r - meanR
        val dg = g - meanG
        val db = b - meanB
        val distance = (dr * dr) + (dg * dg) + (db * db)
        distanceMap[index] = distance
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (x < borderThickness || x >= width - borderThickness ||
                y < borderThickness || y >= height - borderThickness
            ) {
                val index = y * width + x
                if ((pixels[index] ushr 24) <= 12) continue
                borderDistanceSum += distanceMap[index]
                borderDistanceCount += 1
            }
        }
    }
    if (borderDistanceCount <= 0) return null
    val averageBorderDistance = (borderDistanceSum / borderDistanceCount).toFloat()
    val floodThreshold = (averageBorderDistance * 1.65f + 520f)
        .coerceIn(420f, 24000f)

    val background = BooleanArray(total)
    val visited = BooleanArray(total)
    val queue = ArrayDeque<Int>()

    fun seedBackground(index: Int) {
        if (index < 0 || index >= total) return
        if (visited[index]) return
        if ((pixels[index] ushr 24) <= 12) return
        if (distanceMap[index] > floodThreshold) return
        visited[index] = true
        queue.addLast(index)
    }

    for (x in 0 until width) {
        seedBackground(x)
        seedBackground((height - 1) * width + x)
    }
    for (y in 0 until height) {
        seedBackground(y * width)
        seedBackground(y * width + (width - 1))
    }

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        background[current] = true
        val x = current % width
        val y = current / width
        val top = (y - 1).coerceAtLeast(0)
        val bottom = (y + 1).coerceAtMost(height - 1)
        val left = (x - 1).coerceAtLeast(0)
        val right = (x + 1).coerceAtMost(width - 1)
        for (ny in top..bottom) {
            val rowOffset = ny * width
            for (nx in left..right) {
                if (nx == x && ny == y) continue
                val neighbor = rowOffset + nx
                if (!visited[neighbor] && (pixels[neighbor] ushr 24) > 12 && distanceMap[neighbor] <= floodThreshold) {
                    visited[neighbor] = true
                    queue.addLast(neighbor)
                }
            }
        }
    }

    val foregroundCandidate = BooleanArray(total)
    var foregroundCount = 0
    for (index in pixels.indices) {
        val alpha = pixels[index] ushr 24
        val keep = alpha > 12 && !background[index]
        foregroundCandidate[index] = keep
        if (keep) foregroundCount += 1
    }
    if (foregroundCount <= 0) return null

    val componentVisited = BooleanArray(total)
    val componentLabels = IntArray(total) { -1 }
    val componentQueue = ArrayDeque<Int>()
    val componentSizes = ArrayList<Int>(16)
    for (index in foregroundCandidate.indices) {
        if (!foregroundCandidate[index] || componentVisited[index]) continue
        val labelId = componentSizes.size
        var componentSize = 0
        componentQueue.addLast(index)
        componentVisited[index] = true
        componentLabels[index] = labelId
        while (componentQueue.isNotEmpty()) {
            val current = componentQueue.removeFirst()
            componentSize += 1
            val x = current % width
            val y = current / width
            val top = (y - 1).coerceAtLeast(0)
            val bottom = (y + 1).coerceAtMost(height - 1)
            val left = (x - 1).coerceAtLeast(0)
            val right = (x + 1).coerceAtMost(width - 1)
            for (ny in top..bottom) {
                val rowOffset = ny * width
                for (nx in left..right) {
                    if (nx == x && ny == y) continue
                    val neighbor = rowOffset + nx
                    if (foregroundCandidate[neighbor] && !componentVisited[neighbor]) {
                        componentVisited[neighbor] = true
                        componentLabels[neighbor] = labelId
                        componentQueue.addLast(neighbor)
                    }
                }
            }
        }
        componentSizes.add(componentSize)
    }
    if (componentSizes.isEmpty()) return null

    val largestComponentSize = componentSizes.maxOrNull()?.coerceAtLeast(1) ?: return null
    val minAbsoluteSize = (total * 0.0003f).roundToInt().coerceAtLeast(12)
    val minRelativeSize = (largestComponentSize * 0.009f).roundToInt().coerceAtLeast(minAbsoluteSize)
    val keepLabels = BooleanArray(componentSizes.size)
    var keptPixels = 0
    for (label in componentSizes.indices) {
        if (componentSizes[label] >= minRelativeSize) {
            keepLabels[label] = true
            keptPixels += componentSizes[label]
        }
    }
    val minKeepPixels = (total * 0.01f).roundToInt().coerceAtLeast(36)
    if (keptPixels < minKeepPixels) return null

    val outputPixels = IntArray(total)
    for (index in outputPixels.indices) {
        val label = componentLabels[index]
        val alpha = if (label >= 0 && keepLabels[label]) 255 else 0
        outputPixels[index] = Color.argb(alpha, 255, 255, 255)
    }
    return createBitmap(width, height).also { output ->
        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
    }
}

private fun ensureMutableArgb8888Bitmap(bitmap: Bitmap?): Bitmap? {
    if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
    if (bitmap.isMutable && bitmap.config == Bitmap.Config.ARGB_8888) return bitmap
    return runCatching {
        bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }.getOrNull()
}

private fun softenMaskAlphaInPlace(maskBitmap: Bitmap, passes: Int) {
    if (maskBitmap.isRecycled || passes <= 0) return
    val width = maskBitmap.width
    val height = maskBitmap.height
    if (width <= 1 || height <= 1) return
    var current = IntArray(width * height)
    var buffer = IntArray(width * height)
    maskBitmap.getPixels(current, 0, width, 0, 0, width, height)
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
                        sum += current[rowOffset + nx] ushr 24
                        count += 1
                    }
                }
                val alpha = (sum / count).coerceIn(0, 255)
                buffer[y * width + x] = Color.argb(alpha, 255, 255, 255)
            }
        }
        val swap = current
        current = buffer
        buffer = swap
    }
    maskBitmap.setPixels(current, 0, width, 0, 0, width, height)
}

private fun stabilizeForegroundAlphaInPlace(
    bitmap: Bitmap,
    alphaThreshold: Int,
    closePasses: Int
) {
    if (bitmap.isRecycled || bitmap.width <= 2 || bitmap.height <= 2) return
    val width = bitmap.width
    val height = bitmap.height
    val total = width * height
    val pixels = IntArray(total)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val safeThreshold = alphaThreshold.coerceIn(1, 254)
    var mask = BooleanArray(total)
    for (index in 0 until total) {
        mask[index] = (pixels[index] ushr 24) > safeThreshold
    }
    repeat(closePasses.coerceIn(0, 4)) {
        mask = dilateBinaryMask(mask, width, height)
        mask = erodeBinaryMask(mask, width, height)
    }
    removeTinyForegroundSpecklesFromMaskInPlace(
        mask = mask,
        width = width,
        height = height
    )
    var changed = false
    for (index in 0 until total) {
        val originAlpha = pixels[index] ushr 24
        if (mask[index]) {
            val newAlpha = if (originAlpha > safeThreshold) originAlpha else 255
            if (newAlpha != originAlpha) {
                pixels[index] = (newAlpha shl 24) or (pixels[index] and 0x00FFFFFF)
                changed = true
            }
        } else if (originAlpha != 0) {
            pixels[index] = pixels[index] and 0x00FFFFFF
            changed = true
        }
    }
    if (changed) {
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

private fun removeTinyForegroundSpecklesFromMaskInPlace(
    mask: BooleanArray,
    width: Int,
    height: Int
) {
    if (mask.isEmpty() || width <= 2 || height <= 2) return
    val total = width * height
    val labels = IntArray(total) { -1 }
    val queue = IntArray(total)
    val sizes = ArrayList<Int>(16)
    var largestSize = 0

    for (seed in 0 until total) {
        if (!mask[seed] || labels[seed] >= 0) continue
        val label = sizes.size
        var head = 0
        var tail = 0
        var size = 0
        queue[tail++] = seed
        labels[seed] = label
        while (head < tail) {
            val current = queue[head++]
            size += 1
            val x = current % width
            val y = current / width
            val top = (y - 1).coerceAtLeast(0)
            val bottom = (y + 1).coerceAtMost(height - 1)
            val left = (x - 1).coerceAtLeast(0)
            val right = (x + 1).coerceAtMost(width - 1)
            for (ny in top..bottom) {
                val rowOffset = ny * width
                for (nx in left..right) {
                    if (nx == x && ny == y) continue
                    val neighbor = rowOffset + nx
                    if (mask[neighbor] && labels[neighbor] < 0) {
                        labels[neighbor] = label
                        queue[tail++] = neighbor
                    }
                }
            }
        }
        sizes.add(size)
        if (size > largestSize) {
            largestSize = size
        }
    }
    if (sizes.isEmpty()) return
    val minByLargest = (largestSize * 0.004f).roundToInt().coerceAtLeast(1)
    val minByTotal = (total * 0.00012f).roundToInt().coerceAtLeast(6)
    val minKeepSize = max(minByLargest, minByTotal)
    for (index in 0 until total) {
        val label = labels[index]
        if (label >= 0 && sizes[label] < minKeepSize) {
            mask[index] = false
        }
    }
}

private fun dilateBinaryMask(
    input: BooleanArray,
    width: Int,
    height: Int
): BooleanArray {
    val output = BooleanArray(input.size)
    for (y in 0 until height) {
        val top = (y - 1).coerceAtLeast(0)
        val bottom = (y + 1).coerceAtMost(height - 1)
        for (x in 0 until width) {
            val left = (x - 1).coerceAtLeast(0)
            val right = (x + 1).coerceAtMost(width - 1)
            var on = false
            for (ny in top..bottom) {
                val rowOffset = ny * width
                for (nx in left..right) {
                    if (input[rowOffset + nx]) {
                        on = true
                        break
                    }
                }
                if (on) break
            }
            output[y * width + x] = on
        }
    }
    return output
}

private fun erodeBinaryMask(
    input: BooleanArray,
    width: Int,
    height: Int
): BooleanArray {
    val output = BooleanArray(input.size)
    for (y in 0 until height) {
        val top = (y - 1).coerceAtLeast(0)
        val bottom = (y + 1).coerceAtMost(height - 1)
        for (x in 0 until width) {
            val left = (x - 1).coerceAtLeast(0)
            val right = (x + 1).coerceAtMost(width - 1)
            var on = true
            for (ny in top..bottom) {
                val rowOffset = ny * width
                for (nx in left..right) {
                    if (!input[rowOffset + nx]) {
                        on = false
                        break
                    }
                }
                if (!on) break
            }
            output[y * width + x] = on
        }
    }
    return output
}

private fun fillEnclosedAlphaHolesInBitmapInPlace(
    bitmap: Bitmap,
    alphaThreshold: Int,
    fillAlpha: Int
) {
    if (bitmap.isRecycled || bitmap.width <= 2 || bitmap.height <= 2) return
    val width = bitmap.width
    val height = bitmap.height
    val total = width * height
    val pixels = IntArray(total)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val safeThreshold = alphaThreshold.coerceIn(1, 254)
    val safeFillAlpha = fillAlpha.coerceIn(0, 255)
    val foreground = BooleanArray(total)
    for (index in pixels.indices) {
        foreground[index] = (pixels[index] ushr 24) > safeThreshold
    }

    val visited = BooleanArray(total)
    val queue = IntArray(total)
    var head = 0
    var tail = 0

    fun seed(index: Int) {
        if (index < 0 || index >= total) return
        if (visited[index] || foreground[index]) return
        visited[index] = true
        queue[tail++] = index
    }

    for (x in 0 until width) {
        seed(x)
        seed((height - 1) * width + x)
    }
    for (y in 0 until height) {
        seed(y * width)
        seed(y * width + (width - 1))
    }

    while (head < tail) {
        val current = queue[head++]
        val x = current % width
        val y = current / width
        val top = (y - 1).coerceAtLeast(0)
        val bottom = (y + 1).coerceAtMost(height - 1)
        val left = (x - 1).coerceAtLeast(0)
        val right = (x + 1).coerceAtMost(width - 1)
        for (ny in top..bottom) {
            val rowOffset = ny * width
            for (nx in left..right) {
                if (nx == x && ny == y) continue
                val neighbor = rowOffset + nx
                if (!visited[neighbor] && !foreground[neighbor]) {
                    visited[neighbor] = true
                    queue[tail++] = neighbor
                }
            }
        }
    }

    var changed = false
    for (index in pixels.indices) {
        if (!foreground[index] && !visited[index]) {
            pixels[index] = (safeFillAlpha shl 24) or (pixels[index] and 0x00FFFFFF)
            changed = true
        }
    }
    if (changed) {
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

private fun buildStampAutoCutoutFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    val sequence = stampCutoutFileSequence.getAndIncrement()
        .let { value -> if (value == Int.MAX_VALUE) stampCutoutFileSequence.getAndSet(0) else value }
        .and(0x0FFF)
    return "cutout_v${STAMP_AUTO_CUTOUT_CACHE_VERSION}_${timestamp}_${sequence}.png"
}

private fun saveStampAutoCutoutBitmapToLocalStore(
    context: Context,
    bitmap: Bitmap
): String? = runCatching {
    val outputDir = File(context.filesDir, STAMP_AUTO_CUTOUT_LOCAL_FOLDER)
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val outputFile = File(outputDir, buildStampAutoCutoutFileName())
    FileOutputStream(outputFile).use { stream ->
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
            throw IllegalStateException("Failed to save auto cutout bitmap")
        }
    }
    outputFile.absolutePath
}.getOrNull()

private fun drawStampCalendarExportShadow(
    canvas: Canvas,
    source: Bitmap,
    destination: RectF,
    shadowColor: Int,
    blurRadius: Float,
    offsetX: Float,
    offsetY: Float
) {
    val scaledWidth = destination.width().roundToInt().coerceAtLeast(1)
    val scaledHeight = destination.height().roundToInt().coerceAtLeast(1)
    val scaledBitmap = if (source.width == scaledWidth && source.height == scaledHeight) {
        source
    } else {
        Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
    }
    val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(blurRadius.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
    }
    val offset = IntArray(2)
    val alphaBitmap = scaledBitmap.extractAlpha(blurPaint, offset)
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = shadowColor
    }
    canvas.drawBitmap(
        alphaBitmap,
        destination.left + offset[0] + offsetX,
        destination.top + offset[1] + offsetY,
        shadowPaint
    )
    if (alphaBitmap !== scaledBitmap && !alphaBitmap.isRecycled) {
        alphaBitmap.recycle()
    }
    if (scaledBitmap !== source && !scaledBitmap.isRecycled) {
        scaledBitmap.recycle()
    }
}

private fun drawStampCalendarExportGrid(
    canvas: Canvas,
    width: Int,
    height: Int,
    lineColor: Int,
    spacing: Float = 84f
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(lineColor, 112)
        strokeWidth = 1f
    }
    var x = 0f
    while (x <= width.toFloat()) {
        canvas.drawLine(x, 0f, x, height.toFloat(), paint)
        x += spacing
    }
    var y = 0f
    while (y <= height.toFloat()) {
        canvas.drawLine(0f, y, width.toFloat(), y, paint)
        y += spacing
    }
}

internal fun buildStampExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    val sequence = stampExportFileSequence.getAndIncrement()
        .let { value -> if (value == Int.MAX_VALUE) stampExportFileSequence.getAndSet(0) else value }
        .and(0x0FFF)
    return "photoo_stamp_${timestamp}_${sequence}.png"
}

internal fun saveStampBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String = buildStampExportFileName()
): Uri? {
    val resolver = context.contentResolver
    val nowSeconds = System.currentTimeMillis() / 1000L
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, STAMP_EXPORT_IMAGE_FOLDER)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    runCatching {
        resolver.openOutputStream(savedUri)?.use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw IllegalStateException("Failed to encode stamp")
            }
        } ?: throw IllegalStateException("Failed to open output stream")
    }.onFailure {
        runCatching { resolver.delete(savedUri, null, null) }
        return null
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.update(
            savedUri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        )
    }
    return savedUri
}

internal fun encodeStampBitmapToPngBytes(bitmap: Bitmap): ByteArray? {
    if (bitmap.isRecycled) return null
    return runCatching {
        ByteArrayOutputStream((bitmap.width * bitmap.height / 4).coerceAtLeast(32 * 1024)).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw IllegalStateException("Failed to encode stamp")
            }
            output.toByteArray()
        }
    }.getOrNull()
}

internal fun saveStampBytesToGallery(
    context: Context,
    encodedBytes: ByteArray,
    fileName: String = buildStampExportFileName()
): Uri? {
    val resolver = context.contentResolver
    val nowSeconds = System.currentTimeMillis() / 1000L
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, STAMP_EXPORT_IMAGE_FOLDER)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    runCatching {
        resolver.openOutputStream(savedUri)?.use { output ->
            output.write(encodedBytes)
            output.flush()
        } ?: throw IllegalStateException("Failed to open gallery output stream")
    }.onFailure {
        android.util.Log.e("StampRender", "saveStampBytesToGallery failed", it)
        runCatching { resolver.delete(savedUri, null, null) }
        return null
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.update(
            savedUri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        )
    }
    return savedUri
}

internal fun saveStampFromLocalUriToGallery(
    context: Context,
    localUri: Uri,
    fileName: String = buildStampExportFileName()
): Uri? {
    val resolver = context.contentResolver
    val nowSeconds = System.currentTimeMillis() / 1000L
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, STAMP_EXPORT_IMAGE_FOLDER)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    runCatching {
        openStampInputStream(context, localUri)?.use { input ->
            resolver.openOutputStream(savedUri)?.use { output ->
                input.copyTo(output)
                output.flush()
            } ?: throw IllegalStateException("Failed to open gallery output stream")
        } ?: throw IllegalStateException("Failed to open local stamp input stream")
    }.onFailure {
        android.util.Log.e("StampRender", "saveStampFromLocalUriToGallery failed", it)
        runCatching { resolver.delete(savedUri, null, null) }
        return null
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.update(
            savedUri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        )
    }
    return savedUri
}

internal fun saveStampBitmapToLocalStore(
    context: Context,
    bitmap: Bitmap,
    fileName: String = buildStampExportFileName()
): Uri? {
    val encodedBytes = encodeStampBitmapToPngBytes(bitmap) ?: return null
    return saveStampBytesToLocalStore(context, encodedBytes, fileName)
}

internal fun saveStampBytesToLocalStore(
    context: Context,
    encodedBytes: ByteArray,
    fileName: String = buildStampExportFileName()
): Uri? {
    val outputDir = File(context.filesDir, "stamp_calendar")
    if (!outputDir.exists() && !outputDir.mkdirs()) {
        return null
    }
    val outputFile = File(outputDir, fileName)
    return runCatching {
        FileOutputStream(outputFile).use { output ->
            output.write(encodedBytes)
            output.flush()
            // On disk before the calendar index starts pointing at it: a crash right after a
            // save must not leave an indexed but empty stamp file.
            output.fd.sync()
        }
        Uri.fromFile(outputFile)
    }.getOrNull()
}

internal fun createStampCameraCaptureUri(context: Context): Uri? {
    val displayName = "photoo_stamp_source_${
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }.jpg"
    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        buildCameraImageContentValues(
            displayName = displayName,
            relativePath = STAMP_EXPORT_IMAGE_FOLDER
        )
    )
}

internal fun finalizeStampCameraCapture(
    context: Context,
    uri: Uri,
    keep: Boolean
) {
    val resolver = context.contentResolver
    if (!keep) {
        resolver.delete(uri, null, null)
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        )
    }
}

internal fun shareStampImage(
    context: Context,
    uri: Uri,
    title: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_SUBJECT, title)
        clipData = ClipData.newUri(context.contentResolver, title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun drawPerforation(
    canvas: Canvas,
    rect: RectF,
    clearPaint: Paint
) {
    val horizontalRadius = rect.width() * 0.028f
    val verticalRadius = rect.width() * 0.026f
    val horizontalCount = max(8, (rect.width() / (horizontalRadius * 1.85f)).roundToInt())
    val verticalCount = max(10, (rect.height() / (verticalRadius * 1.85f)).roundToInt())
    val horizontalGap = rect.width() / horizontalCount
    val verticalGap = rect.height() / verticalCount

    for (index in 0..horizontalCount) {
        val x = rect.left + index * horizontalGap
        canvas.drawCircle(x, rect.top, horizontalRadius, clearPaint)
        canvas.drawCircle(x, rect.bottom, horizontalRadius, clearPaint)
    }
    for (index in 0..verticalCount) {
        val y = rect.top + index * verticalGap
        canvas.drawCircle(rect.left, y, verticalRadius, clearPaint)
        canvas.drawCircle(rect.right, y, verticalRadius, clearPaint)
    }
}

private fun applyStampImageEffectIfNeeded(
    source: Bitmap,
    effect: StampCalendarImageEffect
): Bitmap {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return source
    if (effect == StampCalendarImageEffect.None) return source
    return when (effect) {
        StampCalendarImageEffect.None -> source
        StampCalendarImageEffect.Cartoon -> applyCartoonEffectBitmap(source) ?: source
        StampCalendarImageEffect.InkSpread -> applyInkSpreadEffectBitmap(source) ?: source
        StampCalendarImageEffect.TextureOverlay -> applyTextureOverlayEffectBitmap(source) ?: source
        StampCalendarImageEffect.ColorQuantize -> applyColorQuantizeEffectBitmap(source) ?: source
        StampCalendarImageEffect.Halftone -> applyHalftoneEffectBitmap(source) ?: source
        StampCalendarImageEffect.MisprintOffset -> applyMisprintOffsetEffectBitmap(source) ?: source
    }
}

private fun applyColorQuantizeEffectBitmap(source: Bitmap): Bitmap? = runCatching {
    val width = source.width
    val height = source.height
    val total = width * height
    val input = IntArray(total)
    val output = IntArray(total)
    source.getPixels(input, 0, width, 0, 0, width, height)
    val levels = 4
    val step = 255f / (levels - 1).toFloat()
    for (index in 0 until total) {
        val color = input[index]
        val alpha = color ushr 24
        if (alpha <= 0) {
            output[index] = 0
            continue
        }
        val r = ((color shr 16) and 0xFF)
        val g = ((color shr 8) and 0xFF)
        val b = (color and 0xFF)
        val gray = ((r * 77) + (g * 150) + (b * 29)) / 256f
        val saturatedR = gray + (r - gray) * 1.24f
        val saturatedG = gray + (g - gray) * 1.24f
        val saturatedB = gray + (b - gray) * 1.24f
        val contrastedR = ((saturatedR - 127.5f) * 1.14f + 127.5f + 4f).coerceIn(0f, 255f)
        val contrastedG = ((saturatedG - 127.5f) * 1.14f + 127.5f + 2f).coerceIn(0f, 255f)
        val contrastedB = ((saturatedB - 127.5f) * 1.14f + 127.5f - 2f).coerceIn(0f, 255f)
        val qr = (((contrastedR / step).roundToInt()) * step).roundToInt().coerceIn(0, 255)
        val qg = (((contrastedG / step).roundToInt()) * step).roundToInt().coerceIn(0, 255)
        val qb = (((contrastedB / step).roundToInt()) * step).roundToInt().coerceIn(0, 255)
        val outR = (qr * 1.04f + 2f).roundToInt().coerceIn(0, 255)
        val outG = (qg * 1.01f + 1f).roundToInt().coerceIn(0, 255)
        val outB = (qb * 0.95f - 3f).roundToInt().coerceIn(0, 255)
        output[index] = Color.argb(alpha, outR, outG, outB)
    }
    Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}.getOrNull()

private fun applyCartoonEffectBitmap(source: Bitmap): Bitmap? = runCatching {
    val width = source.width
    val height = source.height
    val total = width * height
    val input = IntArray(total)
    val quantized = IntArray(total)
    val output = IntArray(total)
    val luma = IntArray(total)
    val edgeStrength = IntArray(total)
    source.getPixels(input, 0, width, 0, 0, width, height)
    val levels = 5
    val step = 255f / (levels - 1).toFloat()
    for (index in 0 until total) {
        val color = input[index]
        val alpha = color ushr 24
        if (alpha <= 0) {
            quantized[index] = 0
            luma[index] = 0
            continue
        }
        val x = index % width
        val y = index / width
        var weightedR = 0f
        var weightedG = 0f
        var weightedB = 0f
        var weightSum = 0f
        val top = (y - 1).coerceAtLeast(0)
        val bottom = (y + 1).coerceAtMost(height - 1)
        val left = (x - 1).coerceAtLeast(0)
        val right = (x + 1).coerceAtMost(width - 1)
        for (ny in top..bottom) {
            val dy = kotlin.math.abs(ny - y)
            val rowOffset = ny * width
            for (nx in left..right) {
                val sample = input[rowOffset + nx]
                val sampleAlpha = sample ushr 24
                if (sampleAlpha <= 0) continue
                val dx = kotlin.math.abs(nx - x)
                val weight = when {
                    dx == 0 && dy == 0 -> 4f
                    dx + dy == 1 -> 2.25f
                    else -> 1f
                }
                weightedR += ((sample shr 16) and 0xFF) * weight
                weightedG += ((sample shr 8) and 0xFF) * weight
                weightedB += (sample and 0xFF) * weight
                weightSum += weight
            }
        }
        val smoothR = if (weightSum > 0f) (weightedR / weightSum).roundToInt() else ((color shr 16) and 0xFF)
        val smoothG = if (weightSum > 0f) (weightedG / weightSum).roundToInt() else ((color shr 8) and 0xFF)
        val smoothB = if (weightSum > 0f) (weightedB / weightSum).roundToInt() else (color and 0xFF)
        val gray = ((smoothR * 77) + (smoothG * 150) + (smoothB * 29)) / 256f
        val boostedR = ((gray + (smoothR - gray) * 1.22f - 127.5f) * 1.12f + 127.5f + 2f)
            .coerceIn(0f, 255f)
        val boostedG = ((gray + (smoothG - gray) * 1.22f - 127.5f) * 1.12f + 127.5f)
            .coerceIn(0f, 255f)
        val boostedB = ((gray + (smoothB - gray) * 1.22f - 127.5f) * 1.12f + 127.5f - 3f)
            .coerceIn(0f, 255f)
        val qr = (((boostedR / step).roundToInt()) * step).roundToInt().coerceIn(0, 255)
        val qg = (((boostedG / step).roundToInt()) * step).roundToInt().coerceIn(0, 255)
        val qb = (((boostedB / step).roundToInt()) * step).roundToInt().coerceIn(0, 255)
        quantized[index] = Color.argb(alpha, qr, qg, qb)
        luma[index] = ((smoothR * 77) + (smoothG * 150) + (smoothB * 29)) shr 8
    }
    for (y in 0 until height) {
        val rowOffset = y * width
        val upRowOffset = max(y - 1, 0) * width
        val downRowOffset = min(y + 1, height - 1) * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val alpha = quantized[index] ushr 24
            if (alpha <= 0) {
                edgeStrength[index] = 0
                continue
            }
            val left = luma[rowOffset + max(x - 1, 0)]
            val right = luma[rowOffset + min(x + 1, width - 1)]
            val up = luma[upRowOffset + x]
            val down = luma[downRowOffset + x]
            val diagUpLeft = luma[upRowOffset + max(x - 1, 0)]
            val diagUpRight = luma[upRowOffset + min(x + 1, width - 1)]
            val diagDownLeft = luma[downRowOffset + max(x - 1, 0)]
            val diagDownRight = luma[downRowOffset + min(x + 1, width - 1)]
            val gradientX = kotlin.math.abs(right - left) + kotlin.math.abs(diagDownRight - diagUpLeft)
            val gradientY = kotlin.math.abs(down - up) + kotlin.math.abs(diagDownLeft - diagUpRight)
            edgeStrength[index] = gradientX + gradientY
        }
    }
    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val color = quantized[index]
            val alpha = color ushr 24
            if (alpha <= 0) {
                output[index] = 0
                continue
            }
            var edge = edgeStrength[index]
            if (x > 0) edge = max(edge, edgeStrength[index - 1])
            if (x < width - 1) edge = max(edge, edgeStrength[index + 1])
            if (y > 0) edge = max(edge, edgeStrength[index - width])
            if (y < height - 1) edge = max(edge, edgeStrength[index + width])
            if (edge >= 98) {
                val edgeTone = (30f - (edge - 98) * 0.19f).roundToInt().coerceIn(6, 32)
                output[index] = Color.argb(alpha, edgeTone, edgeTone, edgeTone)
            } else if (edge >= 74) {
                val blend = ((edge - 74) / 24f).coerceIn(0f, 1f) * 0.6f
                val baseR = (color shr 16) and 0xFF
                val baseG = (color shr 8) and 0xFF
                val baseB = color and 0xFF
                val edgeTone = 34f
                val outR = (baseR * (1f - blend) + edgeTone * blend).roundToInt().coerceIn(0, 255)
                val outG = (baseG * (1f - blend) + edgeTone * blend).roundToInt().coerceIn(0, 255)
                val outB = (baseB * (1f - blend) + edgeTone * blend).roundToInt().coerceIn(0, 255)
                output[index] = Color.argb(alpha, outR, outG, outB)
            } else {
                output[index] = color
            }
        }
    }
    Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}.getOrNull()

private fun applyInkSpreadEffectBitmap(source: Bitmap): Bitmap? = runCatching {
    val width = source.width
    val height = source.height
    val total = width * height
    val input = IntArray(total)
    val output = IntArray(total)
    val luma = IntArray(total)
    source.getPixels(input, 0, width, 0, 0, width, height)
    fun colorAt(x: Int, y: Int): Int {
        val safeX = x.coerceIn(0, width - 1)
        val safeY = y.coerceIn(0, height - 1)
        return input[safeY * width + safeX]
    }
    for (index in 0 until total) {
        val color = input[index]
        val alpha = color ushr 24
        if (alpha <= 0) {
            luma[index] = 0
            continue
        }
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        luma[index] = ((r * 77) + (g * 150) + (b * 29)) shr 8
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = y * width + x
            val color = input[index]
            val alpha = color ushr 24
            if (alpha <= 0) {
                output[index] = 0
                continue
            }
            val srcR = (color shr 16) and 0xFF
            val srcG = (color shr 8) and 0xFF
            val srcB = color and 0xFF
            var rSum = 0f
            var gSum = 0f
            var bSum = 0f
            var weightSum = 0f
            for (ny in max(0, y - 2)..min(height - 1, y + 2)) {
                val dy = kotlin.math.abs(ny - y)
                for (nx in max(0, x - 2)..min(width - 1, x + 2)) {
                    val sample = colorAt(nx, ny)
                    val sampleAlpha = sample ushr 24
                    if (sampleAlpha <= 0) continue
                    val dx = kotlin.math.abs(nx - x)
                    val weight = when (max(dx, dy)) {
                        0 -> 4f
                        1 -> 2.3f
                        else -> 1.1f
                    }
                    rSum += ((sample shr 16) and 0xFF) * weight
                    gSum += ((sample shr 8) and 0xFF) * weight
                    bSum += (sample and 0xFF) * weight
                    weightSum += weight
                }
            }
            val avgR = if (weightSum > 0f) (rSum / weightSum).roundToInt() else srcR
            val avgG = if (weightSum > 0f) (gSum / weightSum).roundToInt() else srcG
            val avgB = if (weightSum > 0f) (bSum / weightSum).roundToInt() else srcB

            val leftNear = colorAt(x - 1, y)
            val leftFar = colorAt(x - 2, y)
            val up = colorAt(x, y - 1)
            val upLeft = colorAt(x - 1, y - 1)
            val bleedR = (((leftNear shr 16) and 0xFF) +
                ((leftFar shr 16) and 0xFF) +
                ((up shr 16) and 0xFF) +
                ((upLeft shr 16) and 0xFF)) / 4
            val bleedG = (((leftNear shr 8) and 0xFF) +
                ((leftFar shr 8) and 0xFF) +
                ((up shr 8) and 0xFF) +
                ((upLeft shr 8) and 0xFF)) / 4
            val bleedB = ((leftNear and 0xFF) + (leftFar and 0xFF) + (up and 0xFF) + (upLeft and 0xFF)) / 4

            var mixedR = (srcR * 0.22f + avgR * 0.44f + bleedR * 0.34f + 7f).roundToInt()
            var mixedG = (srcG * 0.22f + avgG * 0.44f + bleedG * 0.34f + 3f).roundToInt()
            var mixedB = (srcB * 0.22f + avgB * 0.44f + bleedB * 0.34f - 4f).roundToInt()

            val rowOffset = y * width
            val upRowOffset = max(y - 1, 0) * width
            val downRowOffset = min(y + 1, height - 1) * width
            val leftLuma = luma[rowOffset + max(0, x - 1)]
            val rightLuma = luma[rowOffset + min(width - 1, x + 1)]
            val upLuma = luma[upRowOffset + x]
            val downLuma = luma[downRowOffset + x]
            val diagUpLeft = luma[upRowOffset + max(0, x - 1)]
            val diagDownRight = luma[downRowOffset + min(width - 1, x + 1)]
            val edge = kotlin.math.abs(rightLuma - leftLuma) +
                kotlin.math.abs(downLuma - upLuma) +
                kotlin.math.abs(diagDownRight - diagUpLeft)
            val contourDarken = (edge * 0.58f).roundToInt().coerceIn(0, 110)
            val shadowBias = ((255 - ((srcR + srcG + srcB) / 3)) * 0.14f).roundToInt().coerceIn(0, 36)
            mixedR = (mixedR - contourDarken + 12 - shadowBias / 2).coerceIn(0, 255)
            mixedG = (mixedG - contourDarken + 6 - shadowBias / 2).coerceIn(0, 255)
            mixedB = (mixedB - contourDarken - shadowBias).coerceIn(0, 255)
            output[index] = Color.argb(alpha, mixedR, mixedG, mixedB)
        }
    }
    Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}.getOrNull()

private fun applyTextureOverlayEffectBitmap(source: Bitmap): Bitmap? = runCatching {
    val width = source.width
    val height = source.height
    val total = width * height
    val input = IntArray(total)
    val output = IntArray(total)
    source.getPixels(input, 0, width, 0, 0, width, height)
    fun hash(x: Int, y: Int): Int {
        var n = x * 374761393 + y * 668265263
        n = (n xor (n ushr 13)) * 1274126177
        return n xor (n ushr 16)
    }
    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val color = input[index]
            val alpha = color ushr 24
            if (alpha <= 0) {
                output[index] = 0
                continue
            }
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val noise = ((hash(x, y) and 0xFF) - 128) / 128f
            val fiber = kotlin.math.sin(x * 0.15f + y * 0.095f) * 0.55f
            val toneShift = (noise * 11f + fiber * 13f).roundToInt()
            val outR = (r + toneShift + 5).coerceIn(0, 255)
            val outG = (g + toneShift + 2).coerceIn(0, 255)
            val outB = (b + toneShift - 2).coerceIn(0, 255)
            output[index] = Color.argb(alpha, outR, outG, outB)
        }
    }
    Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}.getOrNull()

private fun applyHalftoneEffectBitmap(source: Bitmap): Bitmap? = runCatching {
    val width = source.width
    val height = source.height
    val total = width * height
    val input = IntArray(total)
    val output = IntArray(total)
    source.getPixels(input, 0, width, 0, 0, width, height)
    val cell = (min(width, height) / 56).coerceIn(4, 10)
    for (cellTop in 0 until height step cell) {
        val cellBottom = min(height, cellTop + cell)
        for (cellLeft in 0 until width step cell) {
            val cellRight = min(width, cellLeft + cell)
            var sumR = 0
            var sumG = 0
            var sumB = 0
            var sumA = 0
            var count = 0
            for (y in cellTop until cellBottom) {
                val rowOffset = y * width
                for (x in cellLeft until cellRight) {
                    val color = input[rowOffset + x]
                    val alpha = color ushr 24
                    if (alpha <= 0) continue
                    sumA += alpha
                    sumR += (color shr 16) and 0xFF
                    sumG += (color shr 8) and 0xFF
                    sumB += color and 0xFF
                    count += 1
                }
            }
            if (count <= 0) {
                continue
            }
            val avgA = (sumA / count).coerceIn(0, 255)
            val avgR = (sumR / count).coerceIn(0, 255)
            val avgG = (sumG / count).coerceIn(0, 255)
            val avgB = (sumB / count).coerceIn(0, 255)
            val luma = ((avgR * 77) + (avgG * 150) + (avgB * 29)) / 256f
            val radiusRatio = (1f - luma / 255f).coerceIn(0f, 1f)
            val centerX = (cellLeft + cellRight - 1) * 0.5f
            val centerY = (cellTop + cellBottom - 1) * 0.5f
            val maxRadius = (min(cellRight - cellLeft, cellBottom - cellTop) * 0.5f)
            val dotRadius = maxRadius * radiusRatio
            val backgroundR = (avgR * 0.9f + 24f).roundToInt().coerceIn(0, 255)
            val backgroundG = (avgG * 0.9f + 24f).roundToInt().coerceIn(0, 255)
            val backgroundB = (avgB * 0.9f + 24f).roundToInt().coerceIn(0, 255)
            val dotR = (avgR * 0.38f).roundToInt().coerceIn(0, 255)
            val dotG = (avgG * 0.38f).roundToInt().coerceIn(0, 255)
            val dotB = (avgB * 0.38f).roundToInt().coerceIn(0, 255)
            for (y in cellTop until cellBottom) {
                val rowOffset = y * width
                for (x in cellLeft until cellRight) {
                    val srcColor = input[rowOffset + x]
                    val srcAlpha = srcColor ushr 24
                    if (srcAlpha <= 0) {
                        output[rowOffset + x] = 0
                        continue
                    }
                    val dx = x - centerX
                    val dy = y - centerY
                    val inDot = (dx * dx + dy * dy) <= (dotRadius * dotRadius)
                    output[rowOffset + x] = if (inDot) {
                        Color.argb(srcAlpha, dotR, dotG, dotB)
                    } else {
                        Color.argb(srcAlpha, backgroundR, backgroundG, backgroundB)
                    }
                }
            }
        }
    }
    Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}.getOrNull()

private fun applyMisprintOffsetEffectBitmap(source: Bitmap): Bitmap? = runCatching {
    val width = source.width
    val height = source.height
    val total = width * height
    val input = IntArray(total)
    val output = IntArray(total)
    source.getPixels(input, 0, width, 0, 0, width, height)
    val offset = (min(width, height) / 210).coerceIn(2, 5)
    val verticalOffset = (offset * 0.75f).roundToInt().coerceAtLeast(1)
    val ghostOffset = (offset * 2).coerceIn(3, 8)

    fun sample(x: Int, y: Int): Int {
        val safeX = x.coerceIn(0, width - 1)
        val safeY = y.coerceIn(0, height - 1)
        return input[safeY * width + safeX]
    }

    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val base = input[index]
            val alpha = base ushr 24
            if (alpha <= 0) {
                output[index] = 0
                continue
            }
            val baseR = (base shr 16) and 0xFF
            val baseG = (base shr 8) and 0xFF
            val baseB = base and 0xFF
            val rSample = sample(x - offset, y + verticalOffset)
            val gSample = sample(x + offset, y - verticalOffset)
            val bSample = sample(x - verticalOffset, y - offset)
            val ghostSample = sample(x + ghostOffset, y - ghostOffset)
            var r = (((rSample shr 16) and 0xFF) * 0.76f +
                baseR * 0.24f +
                ((ghostSample shr 16) and 0xFF) * 0.08f + 14f)
                .roundToInt()
            var g = (((gSample shr 8) and 0xFF) * 0.78f +
                baseG * 0.22f +
                ((ghostSample shr 8) and 0xFF) * 0.08f + 8f)
                .roundToInt()
            var b = ((bSample and 0xFF) * 0.78f +
                baseB * 0.22f +
                (ghostSample and 0xFF) * 0.10f + 14f)
                .roundToInt()
            r = (((r - 127.5f) * 1.08f) + 127.5f).roundToInt().coerceIn(0, 255)
            g = (((g - 127.5f) * 1.06f) + 127.5f).roundToInt().coerceIn(0, 255)
            b = (((b - 127.5f) * 1.08f) + 127.5f).roundToInt().coerceIn(0, 255)
            output[index] = Color.argb(alpha, r, g, b)
        }
    }
    Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}.getOrNull()

private fun drawCroppedBitmap(
    canvas: Canvas,
    source: Bitmap,
    destination: RectF,
    radius: Float
) {
    val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val scale = max(
        destination.width() / source.width.toFloat(),
        destination.height() / source.height.toFloat()
    )
    val dx = destination.left + (destination.width() - source.width * scale) * 0.5f
    val dy = destination.top + (destination.height() - source.height * scale) * 0.5f
    shader.setLocalMatrix(Matrix().apply {
        setScale(scale, scale)
        postTranslate(dx, dy)
    })
    val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
    }
    canvas.drawRoundRect(destination, radius, radius, photoPaint)
}

private fun drawStampOverlay(
    canvas: Canvas,
    overlay: Bitmap,
    destination: RectF
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    canvas.drawBitmap(
        overlay,
        null,
        android.graphics.RectF(destination.left, destination.top, destination.right, destination.bottom),
        paint
    )
}

private fun extractStampInnerContentBitmap(
    source: Bitmap,
    insetFraction: Float
): Bitmap? {
    if (source.isRecycled || source.width <= 1 || source.height <= 1) return null
    val alphaBounds = findOpaqueBitmapBounds(source) ?: return null
    val insetX = (alphaBounds.width() * insetFraction).roundToInt()
        .coerceAtMost((alphaBounds.width() - 2).coerceAtLeast(0) / 2)
    val insetY = (alphaBounds.height() * insetFraction).roundToInt()
        .coerceAtMost((alphaBounds.height() - 2).coerceAtLeast(0) / 2)
    val left = (alphaBounds.left + insetX).coerceIn(0, source.width - 1)
    val top = (alphaBounds.top + insetY).coerceIn(0, source.height - 1)
    val right = (alphaBounds.right - insetX).coerceIn(left + 1, source.width)
    val bottom = (alphaBounds.bottom - insetY).coerceIn(top + 1, source.height)
    val cropWidth = (right - left).coerceAtLeast(1)
    val cropHeight = (bottom - top).coerceAtLeast(1)
    return runCatching {
        Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    }.getOrNull()
}

private fun findOpaqueBitmapBounds(
    source: Bitmap,
    alphaThreshold: Int = 12
): Rect? {
    val width = source.width
    val height = source.height
    if (width <= 0 || height <= 0) return null
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    pixels.forEachIndexed { index, color ->
        val alpha = color ushr 24
        if (alpha <= alphaThreshold) return@forEachIndexed
        val x = index % width
        val y = index / width
        if (x < left) left = x
        if (x > right) right = x
        if (y < top) top = y
        if (y > bottom) bottom = y
    }
    if (right < left || bottom < top) return null
    return Rect(left, top, right + 1, bottom + 1)
}

private fun estimateStampBottomShortEdgeY(
    source: Bitmap,
    bounds: RectF,
    alphaThreshold: Int = 12
): Float {
    if (source.isRecycled || source.width <= 1 || source.height <= 1) {
        return bounds.bottom
    }
    val left = bounds.left.roundToInt().coerceIn(0, source.width - 1)
    val right = bounds.right.roundToInt().coerceIn(left + 1, source.width)
    val top = bounds.top.roundToInt().coerceIn(0, source.height - 1)
    val bottom = bounds.bottom.roundToInt().coerceIn(top + 1, source.height)
    val bandWidth = (right - left).coerceAtLeast(1)
    val bandHeight = (bottom - top).coerceAtLeast(1)
    val searchDepth = (bandHeight * 0.24f).roundToInt()
        .coerceIn(8, bandHeight.coerceAtLeast(8))
    val searchStartY = bottom - 1
    val searchEndY = (bottom - searchDepth).coerceAtLeast(top)
    val rowCoverageThreshold = 0.86f
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

    var foundY = -1
    for (y in searchStartY downTo searchEndY) {
        val rowOffset = y * source.width
        var opaqueCount = 0
        for (x in left until right) {
            if ((pixels[rowOffset + x] ushr 24) > alphaThreshold) {
                opaqueCount += 1
            }
        }
        val rowCoverage = opaqueCount.toFloat() / bandWidth.toFloat().coerceAtLeast(1f)
        if (rowCoverage >= rowCoverageThreshold) {
            foundY = y
            break
        }
    }

    val fallbackY = (searchStartY - (searchDepth * 0.45f).roundToInt()).coerceAtLeast(searchEndY)
    val resolvedY = if (foundY >= 0) foundY else fallbackY
    return resolvedY.toFloat().coerceIn((top + 1).toFloat(), (bottom - 1).toFloat())
}

private fun openStampInputStream(context: Context, uri: Uri): InputStream? {
    val resolver = context.contentResolver
    return when (uri.scheme) {
        "content" -> resolver.openInputStream(uri)
        "file" -> uri.path?.let { path ->
            runCatching { FileInputStream(path) }.getOrNull()
        }
        else -> {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                runCatching { FileInputStream(path) }.getOrNull()
                    ?: resolver.openInputStream(uri)
            } else {
                resolver.openInputStream(uri)
            }
        }
    }
}
