package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val COLOR_CARD_CAMERA_BURST_MAX_SIDE = 2048
private const val COLOR_CARD_CAMERA_BURST_PYRAMID_LEVELS = 4
private const val COLOR_CARD_CAMERA_BURST_SEARCH_RADIUS = 6
private const val COLOR_CARD_CAMERA_BURST_MIN_ALIGNMENT_CONFIDENCE = 0.12f
private const val COLOR_CARD_CAMERA_BURST_REFERENCE_WEIGHT = 1.2f

private data class ColorCardCameraFusionFrame(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val pixels: IntArray,
    val luma: IntArray,
    val alignmentConfidence: Float = 1f
)

private data class ColorCardCameraAlignmentResult(
    val offsetX: Int,
    val offsetY: Int,
    val confidence: Float
)

internal suspend fun fuseColorCardCameraBurstFrames(
    context: Context,
    burstFiles: List<File>,
    outputFile: File
): File? = withContext(Dispatchers.Default) {
    if (burstFiles.size < 2) return@withContext null
    val decodedFrames = burstFiles.mapNotNull { file ->
        decodeColorCardCameraFusionBitmap(file)?.let { bitmap ->
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            ColorCardCameraFusionFrame(
                bitmap = bitmap,
                width = bitmap.width,
                height = bitmap.height,
                pixels = pixels,
                luma = buildColorCardCameraLumaBuffer(pixels)
            )
        }
    }
    if (decodedFrames.size < 2) {
        decodedFrames.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
        return@withContext null
    }

    val reference = decodedFrames[decodedFrames.size / 2]
    val aligned = mutableListOf(reference)
    decodedFrames.forEach { frame ->
        if (frame === reference) return@forEach
        val alignment = estimateColorCardCameraFrameOffset(reference, frame)
        aligned += shiftColorCardCameraFusionFrame(frame, alignment)
    }

    val fusedPixels = fuseColorCardCameraFramesWithWeightedLuma(aligned, reference)
    val fusedBitmap = Bitmap.createBitmap(reference.width, reference.height, Bitmap.Config.ARGB_8888)
    fusedBitmap.setPixels(fusedPixels, 0, reference.width, 0, 0, reference.width, reference.height)
    runCatching {
        FileOutputStream(outputFile).use { stream ->
            fusedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }
        outputFile
    }.getOrNull().also {
        fusedBitmap.recycle()
        decodedFrames.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
        aligned.filter { candidate -> decodedFrames.none { it.bitmap === candidate.bitmap } }
            .forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
    }
}

private fun decodeColorCardCameraFusionBitmap(sourceFile: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val sampleSize = buildColorCardCameraFusionSampleSize(bounds.outWidth, bounds.outHeight)
    val decoded = BitmapFactory.decodeFile(
        sourceFile.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    ) ?: return null
    val oriented = applyColorCardCameraFusionExifOrientation(
        bitmap = decoded,
        orientation = runCatching {
            ExifInterface(sourceFile.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    )
    val maxSide = max(oriented.width, oriented.height)
    if (maxSide <= COLOR_CARD_CAMERA_BURST_MAX_SIDE) return oriented
    val scale = COLOR_CARD_CAMERA_BURST_MAX_SIDE.toFloat() / maxSide.toFloat()
    return Bitmap.createScaledBitmap(
        oriented,
        (oriented.width * scale).roundToInt().coerceAtLeast(1),
        (oriented.height * scale).roundToInt().coerceAtLeast(1),
        true
    ).also {
        if (it !== oriented && !oriented.isRecycled) oriented.recycle()
    }
}

private fun buildColorCardCameraFusionSampleSize(
    sourceWidth: Int,
    sourceHeight: Int
): Int {
    var sampleSize = 1
    var currentWidth = sourceWidth
    var currentHeight = sourceHeight
    while (max(currentWidth, currentHeight) > COLOR_CARD_CAMERA_BURST_MAX_SIDE * 2) {
        sampleSize *= 2
        currentWidth = (sourceWidth / sampleSize).coerceAtLeast(1)
        currentHeight = (sourceHeight / sampleSize).coerceAtLeast(1)
    }
    return sampleSize
}

private fun applyColorCardCameraFusionExifOrientation(
    bitmap: Bitmap,
    orientation: Int
): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}

private fun buildColorCardCameraLumaBuffer(
    pixels: IntArray
): IntArray {
    val output = IntArray(pixels.size)
    for (index in pixels.indices) {
        val pixel = pixels[index]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        output[index] = (r * 77 + g * 150 + b * 29) shr 8
    }
    return output
}

private fun estimateColorCardCameraFrameOffset(
    reference: ColorCardCameraFusionFrame,
    candidate: ColorCardCameraFusionFrame
): ColorCardCameraAlignmentResult {
    var offsetX = 0
    var offsetY = 0
    var bestErrorOverall = Long.MAX_VALUE
    repeat(COLOR_CARD_CAMERA_BURST_PYRAMID_LEVELS) { level ->
        val scale = 1 shl (COLOR_CARD_CAMERA_BURST_PYRAMID_LEVELS - level - 1)
        val refWidth = max(1, reference.width / scale)
        val refHeight = max(1, reference.height / scale)
        val candWidth = max(1, candidate.width / scale)
        val candHeight = max(1, candidate.height / scale)
        val refDown = downsampleColorCardCameraLuma(reference.luma, reference.width, reference.height, refWidth, refHeight)
        val candDown = downsampleColorCardCameraLuma(candidate.luma, candidate.width, candidate.height, candWidth, candHeight)
        val predictedX = offsetX / scale
        val predictedY = offsetY / scale
        var bestError = Long.MAX_VALUE
        var secondBestError = Long.MAX_VALUE
        var bestX = predictedX
        var bestY = predictedY
        for (dy in -COLOR_CARD_CAMERA_BURST_SEARCH_RADIUS..COLOR_CARD_CAMERA_BURST_SEARCH_RADIUS) {
            for (dx in -COLOR_CARD_CAMERA_BURST_SEARCH_RADIUS..COLOR_CARD_CAMERA_BURST_SEARCH_RADIUS) {
                val testX = predictedX + dx
                val testY = predictedY + dy
                val error = computeColorCardCameraAlignmentError(
                    reference = refDown,
                    candidate = candDown,
                    width = min(refWidth, candWidth),
                    height = min(refHeight, candHeight),
                    offsetX = testX,
                    offsetY = testY
                )
                if (error < bestError) {
                    secondBestError = bestError
                    bestError = error
                    bestX = testX
                    bestY = testY
                } else if (error < secondBestError) {
                    secondBestError = error
                }
            }
        }
        offsetX = bestX * scale
        offsetY = bestY * scale
        if (level == COLOR_CARD_CAMERA_BURST_PYRAMID_LEVELS - 1) {
            bestErrorOverall = bestError
            if (secondBestError < Long.MAX_VALUE) {
                val margin = (secondBestError - bestError).coerceAtLeast(0L).toFloat()
                val normalizedError = (bestError.toFloat() / 18f).coerceAtLeast(0f)
                val normalizedMargin = (margin / 12f).coerceAtLeast(0f)
                val confidence = ((1.15f - normalizedError) + normalizedMargin).coerceIn(
                    COLOR_CARD_CAMERA_BURST_MIN_ALIGNMENT_CONFIDENCE,
                    1f
                )
                return ColorCardCameraAlignmentResult(offsetX, offsetY, confidence)
            }
        }
    }
    val fallbackConfidence = (1.1f - (bestErrorOverall.toFloat() / 18f))
        .coerceIn(COLOR_CARD_CAMERA_BURST_MIN_ALIGNMENT_CONFIDENCE, 1f)
    return ColorCardCameraAlignmentResult(offsetX, offsetY, fallbackConfidence)
}

private fun downsampleColorCardCameraLuma(
    source: IntArray,
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int
): IntArray {
    if (sourceWidth == targetWidth && sourceHeight == targetHeight) return source
    val output = IntArray(targetWidth * targetHeight)
    val xScale = sourceWidth.toFloat() / targetWidth.toFloat()
    val yScale = sourceHeight.toFloat() / targetHeight.toFloat()
    for (y in 0 until targetHeight) {
        val srcY = min(sourceHeight - 1, ((y + 0.5f) * yScale).toInt())
        val rowBase = srcY * sourceWidth
        for (x in 0 until targetWidth) {
            val srcX = min(sourceWidth - 1, ((x + 0.5f) * xScale).toInt())
            output[y * targetWidth + x] = source[rowBase + srcX]
        }
    }
    return output
}

private fun computeColorCardCameraAlignmentError(
    reference: IntArray,
    candidate: IntArray,
    width: Int,
    height: Int,
    offsetX: Int,
    offsetY: Int
): Long {
    val safeLeft = max(0, -offsetX)
    val safeTop = max(0, -offsetY)
    val safeRight = min(width, width - offsetX)
    val safeBottom = min(height, height - offsetY)
    if (safeRight - safeLeft < 24 || safeBottom - safeTop < 24) return Long.MAX_VALUE
    var error = 0L
    var samples = 0
    var y = safeTop
    while (y < safeBottom) {
        val refRow = y * width
        val candRow = (y + offsetY) * width
        var x = safeLeft
        while (x < safeRight) {
            error += abs(reference[refRow + x] - candidate[candRow + x + offsetX]).toLong()
            samples += 1
            x += 2
        }
        y += 2
    }
    return if (samples <= 0) Long.MAX_VALUE else error / samples
}

private fun shiftColorCardCameraFusionFrame(
    source: ColorCardCameraFusionFrame,
    alignment: ColorCardCameraAlignmentResult
): ColorCardCameraFusionFrame {
    val offsetX = alignment.offsetX
    val offsetY = alignment.offsetY
    if (offsetX == 0 && offsetY == 0) return source
    val shiftedPixels = IntArray(source.width * source.height)
    for (y in 0 until source.height) {
        val srcY = (y + offsetY).coerceIn(0, source.height - 1)
        val srcRow = srcY * source.width
        val dstRow = y * source.width
        for (x in 0 until source.width) {
            val srcX = (x + offsetX).coerceIn(0, source.width - 1)
            shiftedPixels[dstRow + x] = source.pixels[srcRow + srcX]
        }
    }
    val shiftedBitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    shiftedBitmap.setPixels(shiftedPixels, 0, source.width, 0, 0, source.width, source.height)
    return ColorCardCameraFusionFrame(
        bitmap = shiftedBitmap,
        width = source.width,
        height = source.height,
        pixels = shiftedPixels,
        luma = buildColorCardCameraLumaBuffer(shiftedPixels),
        alignmentConfidence = alignment.confidence
    )
}

private fun fuseColorCardCameraFramesWithWeightedLuma(
    frames: List<ColorCardCameraFusionFrame>,
    reference: ColorCardCameraFusionFrame
): IntArray {
    val width = reference.width
    val height = reference.height
    val output = IntArray(width * height)
    val confidenceMaps = frames.map { frame ->
        if (frame === reference) {
            FloatArray(width * height) { COLOR_CARD_CAMERA_BURST_REFERENCE_WEIGHT }
        } else {
            buildColorCardCameraContinuousConfidenceMap(reference, frame)
        }
    }
    for (index in output.indices) {
        val referencePixel = reference.pixels[index]
        val refR = (referencePixel shr 16) and 0xFF
        val refG = (referencePixel shr 8) and 0xFF
        val refB = referencePixel and 0xFF
        val refY = reference.luma[index].toFloat()
        val refCb = ((-43 * refR - 85 * refG + 128 * refB) shr 8) + 128
        val refCr = ((128 * refR - 107 * refG - 21 * refB) shr 8) + 128

        var weightedLuma = 0f
        var weightSum = 0f
        frames.forEachIndexed { frameIndex, frame ->
            val weight = confidenceMaps[frameIndex][index]
            if (weight <= 0.001f) return@forEachIndexed
            val candidateLuma = frame.luma[index].toFloat()
            weightedLuma += candidateLuma * weight
            weightSum += weight
        }
        if (weightSum <= 0.001f) {
            output[index] = referencePixel
        } else {
            val fusedY = (weightedLuma / weightSum).roundToInt().coerceIn(0, 255)
            output[index] = convertColorCardCameraYcbcrToArgb(
                y = fusedY,
                cb = refCb.coerceIn(0, 255),
                cr = refCr.coerceIn(0, 255)
            )
        }
    }
    return output
}

private fun buildColorCardCameraContinuousConfidenceMap(
    reference: ColorCardCameraFusionFrame,
    candidate: ColorCardCameraFusionFrame
): FloatArray {
    val map = FloatArray(reference.width * reference.height)
    for (index in map.indices) {
        val diff = abs(candidate.luma[index] - reference.luma[index]).toFloat()
        val localConfidence = smoothstepColorCardCameraConfidence(
            value = diff,
            stableEdge = 2.5f,
            cutoffEdge = 20f
        )
        val confidence = (candidate.alignmentConfidence * localConfidence)
            .coerceIn(0f, 0.72f)
        map[index] = confidence
    }
    return blurColorCardCameraConfidenceMap(
        source = map,
        width = reference.width,
        height = reference.height
    )
}

private fun smoothstepColorCardCameraConfidence(
    value: Float,
    stableEdge: Float,
    cutoffEdge: Float
): Float {
    if (value <= stableEdge) return 1f
    if (value >= cutoffEdge) return 0f
    val t = ((value - stableEdge) / (cutoffEdge - stableEdge)).coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    return 1f - smooth
}

private fun blurColorCardCameraConfidenceMap(
    source: FloatArray,
    width: Int,
    height: Int
): FloatArray {
    val temp = FloatArray(source.size)
    val output = FloatArray(source.size)
    for (y in 0 until height) {
        val rowBase = y * width
        for (x in 0 until width) {
            val left = source[rowBase + (x - 1).coerceAtLeast(0)]
            val center = source[rowBase + x]
            val right = source[rowBase + (x + 1).coerceAtMost(width - 1)]
            temp[rowBase + x] = (left + center * 2f + right) * 0.25f
        }
    }
    for (y in 0 until height) {
        val upY = (y - 1).coerceAtLeast(0)
        val downY = (y + 1).coerceAtMost(height - 1)
        for (x in 0 until width) {
            val up = temp[upY * width + x]
            val center = temp[y * width + x]
            val down = temp[downY * width + x]
            output[y * width + x] = (up + center * 2f + down) * 0.25f
        }
    }
    return output
}

private fun convertColorCardCameraYcbcrToArgb(
    y: Int,
    cb: Int,
    cr: Int
): Int {
    val centeredCb = cb - 128
    val centeredCr = cr - 128
    val r = (y + 1.402f * centeredCr).roundToInt().coerceIn(0, 255)
    val g = (y - 0.344136f * centeredCb - 0.714136f * centeredCr).roundToInt().coerceIn(0, 255)
    val b = (y + 1.772f * centeredCb).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}
