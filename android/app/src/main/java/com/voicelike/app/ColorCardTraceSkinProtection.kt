package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.hypot

internal const val COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES = 4
internal const val COLOR_CARD_TRACE_SKIN_PROTECTION_PALETTE_RECOVERY = 0.64f
internal const val COLOR_CARD_TRACE_SKIN_PROTECTION_CREATIVE_RECOVERY = 0.58f
internal const val COLOR_CARD_TRACE_SKIN_PROTECTION_TONAL_RECOVERY = 0.22f
private const val COLOR_CARD_TRACE_PORTRAIT_DETAIL_FACE_FLOOR = 0.70f
private const val COLOR_CARD_TRACE_PORTRAIT_SOFTNESS_FACE_FLOOR = 0.84f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CONTOUR_BLEND = 0.72f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CONTOUR_MIN_POINTS = 24
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_FALLBACK_WIDTH_SCALE = 1.24f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_FALLBACK_HEIGHT_SCALE = 1.36f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CONTOUR_WIDTH_SCALE = 1.10f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CONTOUR_HEIGHT_SCALE = 1.18f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_RADIUS_X_SCALE = 0.84f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_RADIUS_Y_SCALE = 0.76f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_CENTER_Y_OFFSET_SCALE = 0.06f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_MIN_RADIUS_X = 0.065f
private const val COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_MIN_RADIUS_Y = 0.085f

internal data class ColorCardTraceFaceRegion(
    val centerX: Float,
    val centerY: Float,
    val radiusX: Float,
    val radiusY: Float
)

internal data class ColorCardTraceNormalizedBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float
        get() = (right - left).coerceAtLeast(0f)

    val height: Float
        get() = (bottom - top).coerceAtLeast(0f)

    val area: Float
        get() = (width * height).coerceAtLeast(0f)

    val centerX: Float
        get() = ((left + right) * 0.5f).coerceIn(0f, 1f)

    val centerY: Float
        get() = ((top + bottom) * 0.5f).coerceIn(0f, 1f)
}

internal object ColorCardTraceSkinProtectionDetector {
    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.08f)
        .enableTracking()
        .build()

    private val detector by lazy { FaceDetection.getClient(detectorOptions) }

    suspend fun detectFaceRegions(bitmap: Bitmap): List<ColorCardTraceFaceRegion> {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return emptyList()
        return withContext(Dispatchers.Default) {
            try {
                val faces = detector.process(InputImage.fromBitmap(bitmap, 0)).await()
                buildFaceRegions(
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height,
                    faces = faces
                )
            } catch (_: CancellationException) {
                emptyList()
            } catch (_: OutOfMemoryError) {
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

internal fun packColorCardTraceGpuFaceRegions(
    faceRegions: List<ColorCardTraceFaceRegion>
): FloatArray {
    val packed = FloatArray(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES * 4)
    faceRegions
        .take(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES)
        .forEachIndexed { index, region ->
            val coreRegion = region.toSkinProtectionCoreRegion()
            val offset = index * 4
            packed[offset] = coreRegion.centerX.coerceIn(0f, 1f)
            packed[offset + 1] = coreRegion.centerY.coerceIn(0f, 1f)
            packed[offset + 2] = coreRegion.radiusX.coerceIn(0.01f, 1f)
            packed[offset + 3] = coreRegion.radiusY.coerceIn(0.01f, 1f)
        }
    return packed
}

internal fun colorCardTraceSkinProtectionAmount(
    normalizedX: Float,
    normalizedY: Float,
    red: Int,
    green: Int,
    blue: Int,
    faceRegions: List<ColorCardTraceFaceRegion>
): Float {
    if (faceRegions.isEmpty()) return 0f
    val faceMask = colorCardTraceFaceProtectionMask(
        normalizedX = normalizedX,
        normalizedY = normalizedY,
        faceRegions = faceRegions
    )
    if (faceMask <= 0.001f) return 0f
    val skinMask = colorCardTraceSkinToneMask(
        red = red,
        green = green,
        blue = blue
    )
    if (skinMask <= 0.001f) return 0f
    return (faceMask * skinMask).coerceIn(0f, 1f)
}

internal fun colorCardTracePortraitDetailAmount(
    normalizedX: Float,
    normalizedY: Float,
    red: Int,
    green: Int,
    blue: Int,
    faceRegions: List<ColorCardTraceFaceRegion>
): Float {
    if (faceRegions.isEmpty()) return 0f
    val faceMask = colorCardTraceFaceProtectionMask(
        normalizedX = normalizedX,
        normalizedY = normalizedY,
        faceRegions = faceRegions
    )
    if (faceMask <= 0.001f) return 0f
    val skinMask = colorCardTraceSkinToneMask(
        red = red,
        green = green,
        blue = blue
    )
    return (
        faceMask *
            (COLOR_CARD_TRACE_PORTRAIT_DETAIL_FACE_FLOOR +
                ((1f - COLOR_CARD_TRACE_PORTRAIT_DETAIL_FACE_FLOOR) * skinMask))
        ).coerceIn(0f, 1f)
}

internal fun colorCardTracePortraitSoftnessAmount(
    normalizedX: Float,
    normalizedY: Float,
    red: Int,
    green: Int,
    blue: Int,
    faceRegions: List<ColorCardTraceFaceRegion>
): Float {
    if (faceRegions.isEmpty()) return 0f
    val faceMask = colorCardTraceFaceProtectionMask(
        normalizedX = normalizedX,
        normalizedY = normalizedY,
        faceRegions = faceRegions
    )
    if (faceMask <= 0.001f) return 0f
    val skinMask = colorCardTraceSkinToneMask(
        red = red,
        green = green,
        blue = blue
    )
    return (
        faceMask *
            (COLOR_CARD_TRACE_PORTRAIT_SOFTNESS_FACE_FLOOR +
                ((1f - COLOR_CARD_TRACE_PORTRAIT_SOFTNESS_FACE_FLOOR) * skinMask))
        ).coerceIn(0f, 1f)
}

internal fun colorCardTraceSkinToneMask(
    red: Int,
    green: Int,
    blue: Int
): Float {
    val redChannel = (red.coerceIn(0, 255) / 255f)
    val greenChannel = (green.coerceIn(0, 255) / 255f)
    val blueChannel = (blue.coerceIn(0, 255) / 255f)
    val luma = (0.299f * redChannel) + (0.587f * greenChannel) + (0.114f * blueChannel)
    val cb = 0.5f - (0.168736f * redChannel) - (0.331264f * greenChannel) + (0.5f * blueChannel)
    val cr = 0.5f + (0.5f * redChannel) - (0.418688f * greenChannel) - (0.081312f * blueChannel)
    val spread = maxOf(redChannel, maxOf(greenChannel, blueChannel)) -
        minOf(redChannel, minOf(greenChannel, blueChannel))
    val detailMask = colorCardTraceSoftRange(
        value = spread,
        outerStart = 0.010f,
        innerStart = 0.032f,
        innerEnd = 0.82f,
        outerEnd = 0.995f
    )
    val lumaMask = colorCardTraceSoftRange(
        value = luma,
        outerStart = 0.02f,
        innerStart = 0.06f,
        innerEnd = 0.96f,
        outerEnd = 0.995f
    )
    val cbMask = colorCardTraceSoftRange(
        value = cb,
        outerStart = 0.17f,
        innerStart = 0.23f,
        innerEnd = 0.49f,
        outerEnd = 0.58f
    )
    val crMask = colorCardTraceSoftRange(
        value = cr,
        outerStart = 0.38f,
        innerStart = 0.44f,
        innerEnd = 0.74f,
        outerEnd = 0.84f
    )
    val redGreenBias = colorCardTraceSoftRange(
        value = redChannel - greenChannel,
        outerStart = -0.10f,
        innerStart = -0.03f,
        innerEnd = 0.38f,
        outerEnd = 0.62f
    )
    val redBlueBias = colorCardTraceSoftRange(
        value = redChannel - blueChannel,
        outerStart = -0.06f,
        innerStart = 0.00f,
        innerEnd = 0.60f,
        outerEnd = 0.88f
    )
    val chromaMask = ((cbMask * 0.48f) + (crMask * 0.52f)).coerceIn(0f, 1f)
    val warmthMask = ((redGreenBias * 0.56f) + (redBlueBias * 0.44f)).coerceIn(0f, 1f)
    val confidence = (
        (chromaMask * 0.38f) +
            (warmthMask * 0.24f) +
            (detailMask * 0.18f) +
            (lumaMask * 0.20f)
        ).coerceIn(0f, 1f)
    val gate = (
        (minOf(chromaMask, warmthMask) * 0.42f) +
            (maxOf(chromaMask, warmthMask) * 0.58f)
        ).coerceIn(0f, 1f)
    return (
        confidence *
            (0.58f + (0.42f * gate))
        ).coerceIn(0f, 1f)
}

internal fun colorCardTraceFaceProtectionMask(
    normalizedX: Float,
    normalizedY: Float,
    faceRegions: List<ColorCardTraceFaceRegion>
): Float {
    if (faceRegions.isEmpty()) return 0f
    val safeX = normalizedX.coerceIn(0f, 1f)
    val safeY = normalizedY.coerceIn(0f, 1f)
    return faceRegions.maxOfOrNull { region ->
        val radiusX = (region.radiusX * COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_RADIUS_X_SCALE)
            .coerceIn(COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_MIN_RADIUS_X, 1f)
        val radiusY = (region.radiusY * COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_RADIUS_Y_SCALE)
            .coerceIn(COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_MIN_RADIUS_Y, 1f)
        val centerX = region.centerX.coerceIn(radiusX, 1f - radiusX)
        val centerY = (region.centerY + (region.radiusY * COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_CENTER_Y_OFFSET_SCALE))
            .coerceIn(radiusY, 1f - radiusY)
        val dx = (safeX - centerX) / radiusX
        val dy = (safeY - centerY) / radiusY
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        when {
            distance <= 0.46f -> 1f
            distance >= 1.08f -> 0f
            else -> 1f - colorCardTraceSmoothStep(0.46f, 1.08f, distance)
        }
    } ?: 0f
}

private fun ColorCardTraceFaceRegion.toSkinProtectionCoreRegion(): ColorCardTraceFaceRegion {
    val radiusX = (radiusX * COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_RADIUS_X_SCALE)
        .coerceIn(COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_MIN_RADIUS_X, 1f)
    val radiusY = (radiusY * COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_RADIUS_Y_SCALE)
        .coerceIn(COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_MIN_RADIUS_Y, 1f)
    return ColorCardTraceFaceRegion(
        centerX = centerX.coerceIn(radiusX, 1f - radiusX),
        centerY = (centerY + (radiusY / COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_RADIUS_Y_SCALE) *
            COLOR_CARD_TRACE_SKIN_PROTECTION_CORE_CENTER_Y_OFFSET_SCALE)
            .coerceIn(radiusY, 1f - radiusY),
        radiusX = radiusX,
        radiusY = radiusY
    )
}

private fun buildFaceRegions(
    bitmapWidth: Int,
    bitmapHeight: Int,
    faces: List<Face>
): List<ColorCardTraceFaceRegion> {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || faces.isEmpty()) return emptyList()
    val safeWidth = bitmapWidth.toFloat()
    val safeHeight = bitmapHeight.toFloat()
    return faces
        .sortedByDescending { face ->
            val box = face.boundingBox
            box.width().coerceAtLeast(1) * box.height().coerceAtLeast(1)
        }
        .mapNotNull { face ->
            val box: Rect = face.boundingBox
            if (box.width() <= 0 || box.height() <= 0) return@mapNotNull null
            val fallbackBounds = ColorCardTraceNormalizedBounds(
                left = (box.left / safeWidth).coerceIn(0f, 1f),
                top = (box.top / safeHeight).coerceIn(0f, 1f),
                right = (box.right / safeWidth).coerceIn(0f, 1f),
                bottom = (box.bottom / safeHeight).coerceIn(0f, 1f)
            )
            val contourBounds = face.toContourBounds(safeWidth, safeHeight)
            resolveColorCardTraceProtectionRegion(
                fallbackBounds = fallbackBounds,
                contourBounds = contourBounds
            )
        }
        .take(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES)
}

internal fun resolveColorCardTraceProtectionRegion(
    fallbackBounds: ColorCardTraceNormalizedBounds,
    contourBounds: ColorCardTraceNormalizedBounds? = null
): ColorCardTraceFaceRegion {
    val safeFallbackBounds = fallbackBounds.normalized()
    val hasContourBounds = contourBounds?.area ?: 0f > 0.0001f
    val workingBounds = if (hasContourBounds) {
        safeFallbackBounds.blendTo(
            target = contourBounds!!.normalized(),
            amount = COLOR_CARD_TRACE_SKIN_PROTECTION_CONTOUR_BLEND
        )
    } else {
        safeFallbackBounds
    }
    val widthScale = if (hasContourBounds) {
        COLOR_CARD_TRACE_SKIN_PROTECTION_CONTOUR_WIDTH_SCALE
    } else {
        COLOR_CARD_TRACE_SKIN_PROTECTION_FALLBACK_WIDTH_SCALE
    }
    val heightScale = if (hasContourBounds) {
        COLOR_CARD_TRACE_SKIN_PROTECTION_CONTOUR_HEIGHT_SCALE
    } else {
        COLOR_CARD_TRACE_SKIN_PROTECTION_FALLBACK_HEIGHT_SCALE
    }
    return ColorCardTraceFaceRegion(
        centerX = workingBounds.centerX,
        centerY = workingBounds.centerY,
        radiusX = ((workingBounds.width * widthScale) * 0.5f).coerceIn(0.09f, 0.52f),
        radiusY = ((workingBounds.height * heightScale) * 0.5f).coerceIn(0.12f, 0.56f)
    )
}

private fun ColorCardTraceNormalizedBounds.normalized(): ColorCardTraceNormalizedBounds {
    val safeLeft = left.coerceIn(0f, 1f)
    val safeTop = top.coerceIn(0f, 1f)
    val safeRight = right.coerceIn(0f, 1f)
    val safeBottom = bottom.coerceIn(0f, 1f)
    return ColorCardTraceNormalizedBounds(
        left = minOf(safeLeft, safeRight),
        top = minOf(safeTop, safeBottom),
        right = maxOf(safeLeft, safeRight),
        bottom = maxOf(safeTop, safeBottom)
    )
}

private fun ColorCardTraceNormalizedBounds.blendTo(
    target: ColorCardTraceNormalizedBounds,
    amount: Float
): ColorCardTraceNormalizedBounds {
    val t = amount.coerceIn(0f, 1f)
    return ColorCardTraceNormalizedBounds(
        left = left + ((target.left - left) * t),
        top = top + ((target.top - top) * t),
        right = right + ((target.right - right) * t),
        bottom = bottom + ((target.bottom - bottom) * t)
    ).normalized()
}

private fun Face.toContourBounds(
    safeWidth: Float,
    safeHeight: Float
): ColorCardTraceNormalizedBounds? {
    val contourPoints = allContours
        .asSequence()
        .flatMap { contour -> contour.points.asSequence() }
        .toList()
    if (contourPoints.size < COLOR_CARD_TRACE_SKIN_PROTECTION_CONTOUR_MIN_POINTS) return null
    var left = 1f
    var top = 1f
    var right = 0f
    var bottom = 0f
    contourPoints.forEach { point ->
        val normalizedX = (point.x / safeWidth).coerceIn(0f, 1f)
        val normalizedY = (point.y / safeHeight).coerceIn(0f, 1f)
        left = minOf(left, normalizedX)
        top = minOf(top, normalizedY)
        right = maxOf(right, normalizedX)
        bottom = maxOf(bottom, normalizedY)
    }
    return ColorCardTraceNormalizedBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    ).takeIf { it.area > 0.0001f }
}

private fun colorCardTraceSoftRange(
    value: Float,
    outerStart: Float,
    innerStart: Float,
    innerEnd: Float,
    outerEnd: Float
): Float {
    if (value <= outerStart || value >= outerEnd) return 0f
    if (value in innerStart..innerEnd) return 1f
    return if (value < innerStart) {
        colorCardTraceSmoothStep(outerStart, innerStart, value)
    } else {
        1f - colorCardTraceSmoothStep(innerEnd, outerEnd, value)
    }
}

private fun colorCardTraceSmoothStep(
    edge0: Float,
    edge1: Float,
    value: Float
): Float {
    if (edge0 == edge1) return if (value >= edge1) 1f else 0f
    val normalized = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return normalized * normalized * (3f - (2f * normalized))
}
