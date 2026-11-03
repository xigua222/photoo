package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

internal data class ColorCardTraceSubjectRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

private const val COLOR_CARD_TRACE_SUBJECT_FRAME_PALETTE_ID = "builtin_creative:subject-frame"
private const val COLOR_CARD_TRACE_SUBJECT_FRAME_TITLE = "#109V"
private const val COLOR_CARD_TRACE_SUBJECT_FRAME_SOURCE_TAG = "Vision Frame"
private const val COLOR_CARD_TRACE_SUBJECT_FRAME_TARGET_AREA = 0.33f
internal const val COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_THICKNESS = 0.0085f
internal const val COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_COLOR = 0xFFF7F3EC.toInt()

internal fun buildColorCardTraceSubjectFrameCandidate(): ColorCardTraceFilterCandidate {
    return ColorCardTraceFilterCandidate(
        paletteId = COLOR_CARD_TRACE_SUBJECT_FRAME_PALETTE_ID,
        kind = ColorCardTraceFilterKind.SubjectFrame,
        title = COLOR_CARD_TRACE_SUBJECT_FRAME_TITLE,
        sourceTag = COLOR_CARD_TRACE_SUBJECT_FRAME_SOURCE_TAG,
        colors = listOf(
            COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_COLOR,
            0xFFB8B1A6.toInt(),
            0xFF6A665F.toInt(),
            0xFF232323.toInt()
        ),
        score = -1.0
    )
}

internal fun resolveColorCardTraceSubjectRect(
    sourceSessionId: Long,
    sourceAspect: Float,
    faceRegions: List<ColorCardTraceFaceRegion>
): ColorCardTraceSubjectRect {
    val fromFaces = faceRegions
        .takeIf { it.isNotEmpty() }
        ?.let(::resolveColorCardTraceFaceUnionRect)
        ?.let(::normalizeColorCardTraceSubjectRectArea)
    if (fromFaces != null) return fromFaces

    val random = Random(sourceSessionId xor 0x56D0C9A3L)
    val width = when {
        sourceAspect >= 1.35f -> 0.66f
        sourceAspect <= 0.82f -> 0.54f
        else -> 0.60f
    }
    val height = (COLOR_CARD_TRACE_SUBJECT_FRAME_TARGET_AREA / width)
        .coerceIn(0.42f, 0.72f)
    val centerX = lerpColorCardTraceSubjectRect(
        start = (width / 2f) + 0.06f,
        end = 1f - (width / 2f) - 0.06f,
        amount = random.nextFloat()
    ).coerceIn(width / 2f, 1f - (width / 2f))
    val centerY = lerpColorCardTraceSubjectRect(
        start = (height / 2f) + 0.06f,
        end = 1f - (height / 2f) - 0.06f,
        amount = (0.28f + (random.nextFloat() * 0.44f)).coerceIn(0f, 1f)
    ).coerceIn(height / 2f, 1f - (height / 2f))
    return buildColorCardTraceSubjectRect(
        centerX = centerX,
        centerY = centerY,
        width = width,
        height = height
    )
}

internal fun renderColorCardTraceSubjectFrameBitmap(
    sourceBitmap: Bitmap,
    subjectRect: ColorCardTraceSubjectRect,
    color: Float,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    strength: Float
): Bitmap {
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
    val safeStrength = strength.coerceIn(0f, 1f)
    val borderMix = (0.42f + (safeStrength * 0.50f)).coerceIn(0f, 1f)
    val safeWidth = (width - 1).coerceAtLeast(1).toFloat()
    val safeHeight = (height - 1).coerceAtLeast(1).toFloat()

    for (y in 0 until height) {
        val normalizedY = if (height <= 1) 0.5f else y / safeHeight
        val rowOffset = y * width
        for (x in 0 until width) {
            val normalizedX = if (width <= 1) 0.5f else x / safeWidth
            val sourceColor = detailAdjustedSourcePixels[rowOffset + x]
            val alpha = AndroidColor.alpha(sourceColor)
            if (alpha == 0) {
                outputPixels[rowOffset + x] = sourceColor
                continue
            }

            val adjustedRgb = applyColorCardTraceCreativeAdjustments(
                lab = colorCardTraceLabFromRgb(
                    red = AndroidColor.red(sourceColor),
                    green = AndroidColor.green(sourceColor),
                    blue = AndroidColor.blue(sourceColor)
                ),
                color = color,
                tone = tone,
                saturation = saturation,
                brightness = brightness,
                colorAxis = colorAxis,
                chromaScale = 1f,
                tonalScale = 1f
            )
            val grayscaleLuma = (
                (adjustedRgb.red / 255f) * 0.299f +
                    (adjustedRgb.green / 255f) * 0.587f +
                    (adjustedRgb.blue / 255f) * 0.114f
                )
            val grayscale = (grayscaleLuma * 255f).roundToInt().coerceIn(0, 255)
            val outsideRgb = ColorCardTraceRgb(
                red = grayscale,
                green = grayscale,
                blue = grayscale
            )
            val onBorder = isColorCardTraceSubjectRectBorder(normalizedX, normalizedY, subjectRect)
            val insideSubject = isColorCardTraceSubjectRectInside(normalizedX, normalizedY, subjectRect)
            val finalRgb = when {
                onBorder -> ColorCardTraceRgb(
                    red = lerpColorCardTraceSubjectChannel(outsideRgb.red, AndroidColor.red(COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_COLOR), borderMix),
                    green = lerpColorCardTraceSubjectChannel(outsideRgb.green, AndroidColor.green(COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_COLOR), borderMix),
                    blue = lerpColorCardTraceSubjectChannel(outsideRgb.blue, AndroidColor.blue(COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_COLOR), borderMix)
                )
                insideSubject -> adjustedRgb
                else -> outsideRgb
            }
            outputPixels[rowOffset + x] = colorCardTraceSubjectArgb(alpha, finalRgb.red, finalRgb.green, finalRgb.blue)
        }
    }

    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun resolveColorCardTraceFaceUnionRect(
    faceRegions: List<ColorCardTraceFaceRegion>
): ColorCardTraceSubjectRect {
    var left = 1f
    var top = 1f
    var right = 0f
    var bottom = 0f
    faceRegions.forEach { region ->
        left = min(left, region.centerX - region.radiusX)
        top = min(top, region.centerY - region.radiusY)
        right = max(right, region.centerX + region.radiusX)
        bottom = max(bottom, region.centerY + region.radiusY)
    }
    return ColorCardTraceSubjectRect(
        left = left.coerceIn(0f, 1f),
        top = top.coerceIn(0f, 1f),
        right = right.coerceIn(0f, 1f),
        bottom = bottom.coerceIn(0f, 1f)
    )
}

private fun normalizeColorCardTraceSubjectRectArea(
    rect: ColorCardTraceSubjectRect
): ColorCardTraceSubjectRect {
    val width = rect.width.coerceAtLeast(0.18f)
    val height = rect.height.coerceAtLeast(0.18f)
    val scale = sqrt(COLOR_CARD_TRACE_SUBJECT_FRAME_TARGET_AREA / (width * height).coerceAtLeast(0.01f))
    return buildColorCardTraceSubjectRect(
        centerX = rect.centerX,
        centerY = rect.centerY,
        width = (width * scale).coerceIn(0.42f, 0.82f),
        height = (height * scale).coerceIn(0.42f, 0.82f)
    )
}

private fun buildColorCardTraceSubjectRect(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float
): ColorCardTraceSubjectRect {
    val safeWidth = width.coerceIn(0.18f, 0.88f)
    val safeHeight = height.coerceIn(0.18f, 0.88f)
    val left = (centerX - (safeWidth / 2f)).coerceIn(0f, 1f - safeWidth)
    val top = (centerY - (safeHeight / 2f)).coerceIn(0f, 1f - safeHeight)
    return ColorCardTraceSubjectRect(
        left = left,
        top = top,
        right = (left + safeWidth).coerceIn(0f, 1f),
        bottom = (top + safeHeight).coerceIn(0f, 1f)
    )
}

private fun isColorCardTraceSubjectRectInside(
    normalizedX: Float,
    normalizedY: Float,
    rect: ColorCardTraceSubjectRect
): Boolean {
    return normalizedX in rect.left..rect.right && normalizedY in rect.top..rect.bottom
}

private fun isColorCardTraceSubjectRectBorder(
    normalizedX: Float,
    normalizedY: Float,
    rect: ColorCardTraceSubjectRect
): Boolean {
    if (!isColorCardTraceSubjectRectInside(normalizedX, normalizedY, rect)) return false
    val horizontalDistance = min(normalizedX - rect.left, rect.right - normalizedX)
    val verticalDistance = min(normalizedY - rect.top, rect.bottom - normalizedY)
    return horizontalDistance <= COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_THICKNESS ||
        verticalDistance <= COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_THICKNESS
}

private fun lerpColorCardTraceSubjectRect(
    start: Float,
    end: Float,
    amount: Float
): Float {
    return start + ((end - start) * amount.coerceIn(0f, 1f))
}

private fun lerpColorCardTraceSubjectChannel(
    start: Int,
    end: Int,
    amount: Float
): Int {
    return (start + ((end - start) * amount.coerceIn(0f, 1f)))
        .roundToInt()
        .coerceIn(0, 255)
}

private fun colorCardTraceSubjectArgb(
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
