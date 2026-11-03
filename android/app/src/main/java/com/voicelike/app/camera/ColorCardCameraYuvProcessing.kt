package com.voicelike.app

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import kotlin.math.pow

internal data class ColorCardCameraYuvFrame(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val yPlane: ByteArray,
    val uPlane: ByteArray,
    val vPlane: ByteArray,
    val yRowStride: Int,
    val yPixelStride: Int,
    val uRowStride: Int,
    val uPixelStride: Int,
    val vRowStride: Int,
    val vPixelStride: Int
)

internal fun copyColorCardCameraYuvFrame(image: ImageProxy): ColorCardCameraYuvFrame? {
    if (image.format != ImageFormat.YUV_420_888) return null
    val yPlane = image.planes.getOrNull(0) ?: return null
    val uPlane = image.planes.getOrNull(1) ?: return null
    val vPlane = image.planes.getOrNull(2) ?: return null
    return ColorCardCameraYuvFrame(
        width = image.width,
        height = image.height,
        rotationDegrees = image.imageInfo.rotationDegrees,
        yPlane = yPlane.buffer.toByteArray(),
        uPlane = uPlane.buffer.toByteArray(),
        vPlane = vPlane.buffer.toByteArray(),
        yRowStride = yPlane.rowStride,
        yPixelStride = yPlane.pixelStride,
        uRowStride = uPlane.rowStride,
        uPixelStride = uPlane.pixelStride,
        vRowStride = vPlane.rowStride,
        vPixelStride = vPlane.pixelStride
    )
}

internal fun colorCardCameraYuvFrameToBitmap(
    frame: ColorCardCameraYuvFrame,
    colorAdjustment: Float,
    colorAxis: ColorCardTraceColorAxis,
    toneAdjustment: Float,
    brightnessAdjustment: Float,
    saturationAdjustment: Float,
    sharpenAdjustment: Float
): Bitmap {
    val width = frame.width
    val height = frame.height
    val histogram = IntArray(256)
    for (row in 0 until height step 2) {
        val yRow = row * frame.yRowStride
        for (col in 0 until width step 2) {
            val y = frame.yPlane[yRow + (col * frame.yPixelStride)].toInt() and 0xFF
            histogram[y] += 1
        }
    }
    val sampleCount = histogram.sum().coerceAtLeast(1)
    val lowPercentile = (sampleCount * 0.01f).toInt()
    val highPercentile = (sampleCount * 0.99f).toInt()
    var cumulative = 0
    var lowY = 0
    var highY = 255
    for (index in histogram.indices) {
        cumulative += histogram[index]
        if (cumulative >= lowPercentile) {
            lowY = index
            break
        }
    }
    cumulative = 0
    for (index in histogram.indices) {
        cumulative += histogram[index]
        if (cumulative >= highPercentile) {
            highY = index
            break
        }
    }
    val dynamicRange = (highY - lowY).coerceAtLeast(24)
    val contrast = (1f + (toneAdjustment.coerceIn(-1f, 1f) * 0.22f)).coerceIn(0.72f, 1.28f)
    val brightnessLift = brightnessAdjustment.coerceIn(-1f, 1f) * 0.18f
    val positiveTone = toneAdjustment.coerceAtLeast(0f)
    val negativeTone = (-toneAdjustment).coerceAtLeast(0f)
    val saturationScale = (1f + (saturationAdjustment.coerceIn(-1f, 1f) * 0.28f)).coerceIn(0.72f, 1.30f)
    val safeColorAdjustment = colorAdjustment.coerceIn(-1f, 1f)
    val warmShift = safeColorAdjustment * colorAxis.bWeight * 18f
    val greenMagentaShift = safeColorAdjustment * colorAxis.aWeight * 12f
    val sharpenStrength = sharpenAdjustment.coerceIn(-1f, 1f)
    val mappedLuma = ByteArray(width * height)
    val argb = IntArray(width * height)

    for (row in 0 until height) {
        val yRow = row * frame.yRowStride
        for (col in 0 until width) {
            val yIndex = yRow + (col * frame.yPixelStride)
            val rawY = frame.yPlane[yIndex].toInt() and 0xFF
            var normalizedY = ((rawY - lowY).toFloat() / dynamicRange.toFloat()).coerceIn(0f, 1f)
            if (negativeTone > 0f) {
                normalizedY = normalizedY.pow(1f + (negativeTone * 0.42f))
                normalizedY += (1f - normalizedY) * negativeTone * 0.10f
            }
            if (positiveTone > 0f) {
                normalizedY = 1f - (1f - normalizedY).pow(1f + (positiveTone * 0.26f))
                normalizedY -= normalizedY * positiveTone * 0.04f
            }
            normalizedY = ((normalizedY - 0.5f) * contrast + 0.5f + brightnessLift).coerceIn(0f, 1f)
            mappedLuma[(row * width) + col] = (normalizedY * 255f).toInt().coerceIn(0, 255).toByte()
        }
    }

    for (row in 0 until height) {
        val uRow = (row / 2) * frame.uRowStride
        val vRow = (row / 2) * frame.vRowStride
        for (col in 0 until width) {
            val pixelIndex = (row * width) + col
            val uIndex = uRow + ((col / 2) * frame.uPixelStride)
            val vIndex = vRow + ((col / 2) * frame.vPixelStride)
            var mappedY = mappedLuma[pixelIndex].toInt() and 0xFF
            if (sharpenStrength != 0f && row in 1 until (height - 1) && col in 1 until (width - 1)) {
                val left = mappedLuma[pixelIndex - 1].toInt() and 0xFF
                val right = mappedLuma[pixelIndex + 1].toInt() and 0xFF
                val top = mappedLuma[pixelIndex - width].toInt() and 0xFF
                val bottom = mappedLuma[pixelIndex + width].toInt() and 0xFF
                val neighborAverage = (left + right + top + bottom) / 4f
                val detail = mappedY - neighborAverage
                mappedY = (mappedY + (detail * sharpenStrength * 0.85f)).toInt().coerceIn(0, 255)
            }
            val u = (
                (((frame.uPlane[uIndex].toInt() and 0xFF) - 128) + warmShift) *
                    saturationScale
                ).toInt()
            val v = (
                (((frame.vPlane[vIndex].toInt() and 0xFF) - 128) + greenMagentaShift) *
                    saturationScale
                ).toInt()
            val r = (mappedY + (1.370705f * v)).toInt().coerceIn(0, 255)
            val g = (mappedY - (0.698001f * v) - (0.337633f * u)).toInt().coerceIn(0, 255)
            val b = (mappedY + (1.732446f * u)).toInt().coerceIn(0, 255)
            argb[(row * width) + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    val decoded = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
    if (frame.rotationDegrees == 0) {
        return decoded
    }
    val matrix = Matrix().apply {
        postRotate(frame.rotationDegrees.toFloat())
    }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also { rotated ->
        if (rotated !== decoded && !decoded.isRecycled) {
            decoded.recycle()
        }
    }
}

private fun java.nio.ByteBuffer.toByteArray(): ByteArray {
    val copy = duplicate()
    copy.rewind()
    return ByteArray(copy.remaining()).also(copy::get)
}
