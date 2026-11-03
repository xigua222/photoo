package com.voicelike.app.widgetlikes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlin.math.max
import kotlin.math.roundToInt

object LikesWidgetBitmapLoader {
    private const val MIN_TARGET_PX = 480
    private const val MAX_TARGET_PX = 2200

    fun load(
        context: Context,
        uri: Uri,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Bitmap? {
        val safeWidth = targetWidthPx.coerceIn(MIN_TARGET_PX, MAX_TARGET_PX)
        val safeHeight = targetHeightPx.coerceIn(MIN_TARGET_PX, MAX_TARGET_PX)
        return runCatching {
            val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeWithImageDecoder(context, uri, safeWidth, safeHeight)
            } else {
                decodeWithBitmapFactory(context, uri, safeWidth, safeHeight)
            } ?: return@runCatching null
            centerCropToSize(decoded, safeWidth, safeHeight)
        }.getOrNull()
    }

    private fun decodeWithImageDecoder(
        context: Context,
        uri: Uri,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val sourceW = info.size.width.coerceAtLeast(1)
            val sourceH = info.size.height.coerceAtLeast(1)
            val fillScale = max(
                targetWidthPx.toFloat() / sourceW.toFloat(),
                targetHeightPx.toFloat() / sourceH.toFloat()
            ).coerceAtMost(1f)
            val decodeW = (sourceW * fillScale).roundToInt().coerceAtLeast(1)
            val decodeH = (sourceH * fillScale).roundToInt().coerceAtLeast(1)
            decoder.setTargetSize(decodeW, decodeH)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    }

    private fun decodeWithBitmapFactory(
        context: Context,
        uri: Uri,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetWidthPx, targetHeightPx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val heightRatio = srcHeight / reqHeight.toFloat()
        val widthRatio = srcWidth / reqWidth.toFloat()
        val ratio = max(1f, minOf(heightRatio, widthRatio))
        return ratio.toInt().coerceAtLeast(1)
    }

    private fun centerCropToSize(
        source: Bitmap,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Bitmap {
        val srcW = source.width.coerceAtLeast(1)
        val srcH = source.height.coerceAtLeast(1)
        val targetW = targetWidthPx.coerceAtLeast(1)
        val targetH = targetHeightPx.coerceAtLeast(1)

        if (srcW == targetW && srcH == targetH) return source

        val srcAspect = srcW.toFloat() / srcH.toFloat()
        val targetAspect = targetW.toFloat() / targetH.toFloat()

        val cropW: Int
        val cropH: Int
        val cropX: Int
        val cropY: Int
        if (srcAspect > targetAspect) {
            cropH = srcH
            cropW = (srcH * targetAspect).roundToInt().coerceIn(1, srcW)
            cropX = ((srcW - cropW) / 2f).roundToInt().coerceIn(0, srcW - cropW)
            cropY = 0
        } else {
            cropW = srcW
            cropH = (srcW / targetAspect).roundToInt().coerceIn(1, srcH)
            cropX = 0
            cropY = ((srcH - cropH) / 2f).roundToInt().coerceIn(0, srcH - cropH)
        }

        val cropped = Bitmap.createBitmap(source, cropX, cropY, cropW, cropH)
        val scaled = if (cropped.width != targetW || cropped.height != targetH) {
            Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
        } else {
            cropped
        }

        if (scaled !== cropped) {
            cropped.recycle()
        }
        if (source !== scaled && source !== cropped) {
            source.recycle()
        }
        return scaled
    }
}
