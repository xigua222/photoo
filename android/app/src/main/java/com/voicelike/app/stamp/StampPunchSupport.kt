package com.voicelike.app.stamp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import com.voicelike.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal enum class StampPunchStyle(
    val key: String,
    val overlayResId: Int,
    val aspectRatio: Float,
    val overlayScaleBoost: Float,
    val supportsPunchTemplate: Boolean = true
) {
    Se(
        key = "se",
        overlayResId = R.drawable.cutterse,
        aspectRatio = 558f / 956f,
        overlayScaleBoost = 1.45f
    ),
    One(
        key = "one",
        overlayResId = R.drawable.cutter1,
        aspectRatio = 923f / 1210f,
        overlayScaleBoost = 1.1f
    ),
    Two(
        key = "two",
        overlayResId = R.drawable.cutter2,
        aspectRatio = 752f / 1380f,
        overlayScaleBoost = 1.24f
    ),
    Three(
        key = "three",
        overlayResId = R.drawable.cutter3,
        aspectRatio = 752f / 1380f,
        overlayScaleBoost = 1.24f
    ),
    FrameCutout(
        key = "frame_cutout",
        overlayResId = R.drawable.cutter1,
        aspectRatio = 923f / 1210f,
        overlayScaleBoost = 1f,
        supportsPunchTemplate = false
    ),
    Four(
        key = "four",
        overlayResId = R.drawable.cutter4,
        aspectRatio = 460f / 576f,
        overlayScaleBoost = 1.2f
    );

    companion object {
        fun fromKey(key: String?): StampPunchStyle = entries.firstOrNull { it.key == key } ?: One
    }
}

internal val StampPunchStyle.isFrameCutoutMode: Boolean
    get() = this == StampPunchStyle.FrameCutout

internal val StampPunchStyle.normalizedWidthScale: Float
    get() = (
        sqrt((aspectRatio / StampPunchStyle.One.aspectRatio).toDouble())
        .toFloat()
        .coerceIn(0.84f, 1.05f) * overlayScaleBoost
    ).coerceIn(0.94f, 1.36f)

internal data class StampPunchTemplate(
    val style: StampPunchStyle,
    val overlayBitmap: Bitmap,
    val holeMaskBitmap: Bitmap,
    val holeBounds: RectF
)

private data class StampPunchMaskExtraction(
    val maskBitmap: Bitmap,
    val bounds: RectF
)

internal fun loadStampPunchTemplate(
    context: Context,
    style: StampPunchStyle
): StampPunchTemplate? {
    if (!style.supportsPunchTemplate) return null
    val decodedOverlay = runCatching {
        BitmapFactory.decodeResource(context.resources, style.overlayResId)
            ?.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull() ?: return null
    if (decodedOverlay.isRecycled) {
        return null
    }
    val overlay = smoothStampOverlayBitmap(decodedOverlay)
    if (overlay.isRecycled) return null
    val extraction = extractTransparentHoleMask(overlay) ?: return null
    return StampPunchTemplate(
        style = style,
        overlayBitmap = overlay,
        holeMaskBitmap = extraction.maskBitmap,
        holeBounds = extraction.bounds
    )
}

internal fun loadStampPunchOverlayPreviewBitmap(
    context: Context,
    style: StampPunchStyle
): Bitmap? {
    if (!style.supportsPunchTemplate) return null
    val source = runCatching {
        BitmapFactory.decodeResource(context.resources, style.overlayResId)
            ?.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull() ?: return null
    if (source.isRecycled) return null
    return smoothStampOverlayBitmap(source)
}

internal suspend fun decodeStampPunchSourceBitmap(
    sourceFile: File,
    maxSide: Int
): Bitmap? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching {
            val source = ImageDecoder.createSource(sourceFile)
            return@withContext ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                val srcWidth = info.size.width.coerceAtLeast(1)
                val srcHeight = info.size.height.coerceAtLeast(1)
                val srcMaxSide = max(srcWidth, srcHeight)
                if (srcMaxSide > maxSide) {
                    val scale = maxSide.toFloat() / srcMaxSide.toFloat()
                    decoder.setTargetSize(
                        (srcWidth * scale).roundToInt().coerceAtLeast(1),
                        (srcHeight * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        }.getOrNull()?.let { return@withContext it }
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    val sampleSize = buildStampPunchSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
    val decoded = BitmapFactory.decodeFile(
        sourceFile.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    ) ?: return@withContext null

    val oriented = applyStampPunchExifOrientation(
        bitmap = decoded,
        orientation = readStampPunchExifOrientation(sourceFile)
    )
    val currentMax = max(oriented.width, oriented.height)
    if (currentMax <= maxSide) return@withContext oriented

    val scale = maxSide.toFloat() / currentMax.toFloat()
    val targetWidth = (oriented.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (oriented.height * scale).roundToInt().coerceAtLeast(1)
    Bitmap.createScaledBitmap(oriented, targetWidth, targetHeight, true).also {
        if (it !== oriented && !oriented.isRecycled) {
            oriented.recycle()
        }
    }
}

internal fun renderStampPunchBitmap(
    source: Bitmap,
    template: StampPunchTemplate,
    outputLongSide: Int = 3360,
    sourceViewportOverride: RectF? = null
): Bitmap {
    if (source.isRecycled) {
        throw IllegalArgumentException("source bitmap is recycled")
    }
    val holeMaskBitmap = template.holeMaskBitmap
    if (holeMaskBitmap.isRecycled) {
        throw IllegalArgumentException("holeMaskBitmap is recycled")
    }

    val holeAspect = (template.holeBounds.width() / template.holeBounds.height())
        .takeIf { it.isFinite() && it > 0f }
        ?: 1f
    val targetShapeLongSide = outputLongSide.coerceAtLeast(1080)
    val referenceArea = targetShapeLongSide.toFloat() * targetShapeLongSide.toFloat() * StampPunchStyle.One.aspectRatio
    val shapeWidth = sqrt(referenceArea * holeAspect).roundToInt().coerceAtLeast(1)
    val shapeHeight = (shapeWidth / holeAspect).roundToInt().coerceAtLeast(1)

    val outputWidth = shapeWidth
    val outputHeight = shapeHeight
    val outputBitmap = createBitmap(outputWidth, outputHeight)
    val canvas = Canvas(outputBitmap)
    val sourceViewport = sourceViewportOverride?.let { RectF(it) } ?: RectF(
        (template.holeBounds.left / template.overlayBitmap.width.toFloat()) * source.width,
        (template.holeBounds.top / template.overlayBitmap.height.toFloat()) * source.height,
        (template.holeBounds.right / template.overlayBitmap.width.toFloat()) * source.width,
        (template.holeBounds.bottom / template.overlayBitmap.height.toFloat()) * source.height
    )
    sourceViewport.left = sourceViewport.left.coerceIn(0f, source.width.toFloat())
    sourceViewport.top = sourceViewport.top.coerceIn(0f, source.height.toFloat())
    sourceViewport.right = sourceViewport.right.coerceIn(sourceViewport.left + 1f, source.width.toFloat())
    sourceViewport.bottom = sourceViewport.bottom.coerceIn(sourceViewport.top + 1f, source.height.toFloat())

    val outerMask = createScaledStampShapeMask(
        template = template,
        targetWidth = shapeWidth,
        targetHeight = shapeHeight
    ) ?: throw IllegalStateException("failed to create outerMask")
    if (outerMask.isRecycled) {
        throw IllegalStateException("failed to create outerMask")
    }
    if (template.style == StampPunchStyle.Three) {
        hardenCircularStampMaskInPlace(outerMask)
    }
    try {
        val imageLayer = createBitmap(shapeWidth, shapeHeight)
        try {
            val imageCanvas = Canvas(imageLayer)
            drawStampPunchCroppedBitmap(
                canvas = imageCanvas,
                source = source,
                sourceViewport = sourceViewport,
                destination = RectF(0f, 0f, shapeWidth.toFloat(), shapeHeight.toFloat())
            )
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                isFilterBitmap = false
            }
            imageCanvas.drawBitmap(outerMask, 0f, 0f, maskPaint)
            canvas.drawBitmap(imageLayer, 0f, 0f, null)
        } finally {
            if (!imageLayer.isRecycled) {
                imageLayer.recycle()
            }
        }
    } finally {
        if (!outerMask.isRecycled) {
            outerMask.recycle()
        }
    }
    clearFullyTransparentPixelsInPlace(outputBitmap)

    return outputBitmap
}

internal fun clearFullyTransparentPixelsInPlace(bitmap: Bitmap) {
    if (bitmap.isRecycled || !bitmap.hasAlpha()) return
    val width = bitmap.width.coerceAtLeast(1)
    val height = bitmap.height.coerceAtLeast(1)
    val row = IntArray(width)
    for (y in 0 until height) {
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        var changed = false
        for (x in 0 until width) {
            val color = row[x]
            if ((color ushr 24) == 0 && color != 0) {
                row[x] = 0
                changed = true
            }
        }
        if (changed) {
            bitmap.setPixels(row, 0, width, 0, y, width, 1)
        }
    }
}

internal fun createScaledStampShapeMask(
    template: StampPunchTemplate,
    targetWidth: Int,
    targetHeight: Int
): Bitmap? {
    val width = targetWidth.coerceAtLeast(1)
    val height = targetHeight.coerceAtLeast(1)
    val holeMaskBitmap = template.holeMaskBitmap
    if (holeMaskBitmap.isRecycled) return null
    val holeLeft = template.holeBounds.left.roundToInt().coerceIn(0, holeMaskBitmap.width - 1)
    val holeTop = template.holeBounds.top.roundToInt().coerceIn(0, holeMaskBitmap.height - 1)
    val holeWidth = template.holeBounds.width().roundToInt().coerceAtLeast(1)
        .coerceAtMost(holeMaskBitmap.width - holeLeft)
    val holeHeight = template.holeBounds.height().roundToInt().coerceAtLeast(1)
        .coerceAtMost(holeMaskBitmap.height - holeTop)
    val croppedMask = runCatching {
        Bitmap.createBitmap(
            holeMaskBitmap,
            holeLeft,
            holeTop,
            holeWidth,
            holeHeight
        )
    }.getOrNull() ?: return null
    return try {
        Bitmap.createScaledBitmap(croppedMask, width, height, true)
    } finally {
        if (!croppedMask.isRecycled) {
            croppedMask.recycle()
        }
    }
}

internal fun hardenCircularStampMaskInPlace(
    mask: Bitmap,
    alphaThreshold: Int = 200
) {
    if (mask.isRecycled) return
    val width = mask.width.coerceAtLeast(1)
    val height = mask.height.coerceAtLeast(1)
    val size = width * height
    val pixels = IntArray(size)
    mask.getPixels(pixels, 0, width, 0, 0, width, height)
    val threshold = alphaThreshold.coerceIn(1, 254)
    var changed = false
    for (index in 0 until size) {
        val color = pixels[index]
        val alpha = color ushr 24
        val hardenedAlpha = if (alpha >= threshold) 255 else 0
        if (hardenedAlpha != alpha) {
            pixels[index] = (color and 0x00FFFFFF) or (hardenedAlpha shl 24)
            changed = true
        }
    }
    if (changed) {
        mask.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

private fun readStampPunchExifOrientation(sourceFile: File): Int {
    return runCatching {
        ExifInterface(sourceFile.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
}

private fun applyStampPunchExifOrientation(
    bitmap: Bitmap,
    orientation: Int
): Bitmap {
    if (bitmap.isRecycled) {
        throw IllegalArgumentException("bitmap is recycled")
    }
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
    if (rotated !== bitmap && !bitmap.isRecycled) {
        bitmap.recycle()
    }
    return rotated
}

private fun buildStampPunchSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    maxSide: Int
): Int {
    var sampleSize = 1
    var currentWidth = sourceWidth
    var currentHeight = sourceHeight
    while (max(currentWidth, currentHeight) > maxSide * 2) {
        sampleSize *= 2
        currentWidth = (sourceWidth / sampleSize).coerceAtLeast(1)
        currentHeight = (sourceHeight / sampleSize).coerceAtLeast(1)
    }
    return sampleSize.coerceAtLeast(1)
}

private fun extractTransparentHoleMask(
    overlay: Bitmap,
    alphaThreshold: Int = 8
): StampPunchMaskExtraction? {
    if (overlay.isRecycled) {
        return null
    }
    val width = overlay.width
    val height = overlay.height
    if (width <= 0 || height <= 0) return null

    val total = width * height
    val pixels = IntArray(total)
    overlay.getPixels(pixels, 0, width, 0, 0, width, height)
    val transparent = BooleanArray(total)
    for (index in 0 until total) {
        transparent[index] = (pixels[index] ushr 24) <= alphaThreshold
    }

    val visited = BooleanArray(total)
    val queue = IntArray(total)
    var head = 0
    var tail = 0

    fun enqueue(index: Int) {
        if (index !in 0 until total) return
        if (!transparent[index] || visited[index]) return
        visited[index] = true
        queue[tail++] = index
    }

    for (x in 0 until width) {
        enqueue(x)
        enqueue(((height - 1) * width) + x)
    }
    for (y in 0 until height) {
        enqueue(y * width)
        enqueue((y * width) + width - 1)
    }

    while (head < tail) {
        val index = queue[head++]
        val x = index % width
        val y = index / width
        if (x > 0) enqueue(index - 1)
        if (x < width - 1) enqueue(index + 1)
        if (y > 0) enqueue(index - width)
        if (y < height - 1) enqueue(index + width)
    }

    val component = IntArray(total)
    var bestComponent = IntArray(0)
    var bestCount = 0
    var bestMinX = width
    var bestMinY = height
    var bestMaxX = -1
    var bestMaxY = -1

    fun visitComponent(start: Int) {
        var componentHead = 0
        var componentTail = 0
        var componentCount = 0
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        visited[start] = true
        queue[0] = start
        componentTail = 1

        while (componentHead < componentTail) {
            val index = queue[componentHead++]
            component[componentCount++] = index
            val x = index % width
            val y = index / width
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y

            val left = index - 1
            val right = index + 1
            val up = index - width
            val down = index + width
            if (x > 0 && transparent[left] && !visited[left]) {
                visited[left] = true
                queue[componentTail++] = left
            }
            if (x < width - 1 && transparent[right] && !visited[right]) {
                visited[right] = true
                queue[componentTail++] = right
            }
            if (y > 0 && transparent[up] && !visited[up]) {
                visited[up] = true
                queue[componentTail++] = up
            }
            if (y < height - 1 && transparent[down] && !visited[down]) {
                visited[down] = true
                queue[componentTail++] = down
            }
        }

        if (componentCount > bestCount) {
            bestCount = componentCount
            bestComponent = component.copyOf(componentCount)
            bestMinX = minX
            bestMinY = minY
            bestMaxX = maxX
            bestMaxY = maxY
        }
    }

    for (index in 0 until total) {
        if (transparent[index] && !visited[index]) {
            visitComponent(index)
        }
    }

    if (bestCount <= 0 || bestMaxX < bestMinX || bestMaxY < bestMinY) return null

    val componentPixels = BooleanArray(total)
    bestComponent.forEach { index ->
        if (index in 0 until total) {
            componentPixels[index] = true
        }
    }
    val featherPixels = BooleanArray(total)
    bestComponent.forEach { index ->
        val centerX = index % width
        val centerY = index / width
        for (offsetY in -2..2) {
            for (offsetX in -2..2) {
                if ((offsetX * offsetX) + (offsetY * offsetY) > 5) continue
                val x = centerX + offsetX
                val y = centerY + offsetY
                if (x !in 0 until width || y !in 0 until height) continue
                featherPixels[(y * width) + x] = true
            }
        }
    }
    val maskPixels = IntArray(total)
    for (index in 0 until total) {
        if (!featherPixels[index]) continue
        val sourceAlpha = pixels[index] ushr 24
        val rawMaskAlpha = (255 - sourceAlpha).coerceIn(0, 255)
        val maskAlpha = when {
            componentPixels[index] -> 255
            rawMaskAlpha >= 208 -> 220
            rawMaskAlpha >= 148 -> 164
            rawMaskAlpha >= 96 -> 92
            else -> 0
        }
        if (maskAlpha != 0) {
            maskPixels[index] = Color.argb(maskAlpha, 255, 255, 255)
        }
    }
    val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    maskBitmap.setPixels(maskPixels, 0, width, 0, 0, width, height)

    return StampPunchMaskExtraction(
        maskBitmap = maskBitmap,
        bounds = RectF(
            bestMinX.toFloat(),
            bestMinY.toFloat(),
            (bestMaxX + 1).toFloat(),
            (bestMaxY + 1).toFloat()
        )
    )
}

private fun drawStampPunchCroppedBitmap(
    canvas: Canvas,
    source: Bitmap,
    sourceViewport: RectF,
    destination: RectF
) {
    if (source.isRecycled) {
        return
    }
    val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val scale = max(
        destination.width() / sourceViewport.width(),
        destination.height() / sourceViewport.height()
    )
    val dx = destination.left - (sourceViewport.left * scale) + (destination.width() - sourceViewport.width() * scale) * 0.5f
    val dy = destination.top - (sourceViewport.top * scale) + (destination.height() - sourceViewport.height() * scale) * 0.5f
    shader.setLocalMatrix(Matrix().apply {
        setScale(scale, scale)
        postTranslate(dx, dy)
    })
    val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
        isFilterBitmap = true
        isDither = true
    }
    canvas.drawRect(destination, photoPaint)
}

private fun smoothStampOverlayBitmap(
    source: Bitmap
): Bitmap {
    if (source.isRecycled) return source
    val upscaled = Bitmap.createScaledBitmap(
        source,
        (source.width * 2).coerceAtLeast(1),
        (source.height * 2).coerceAtLeast(1),
        true
    )
    if (upscaled.isRecycled) return source
    val smoothed = Bitmap.createScaledBitmap(
        upscaled,
        source.width,
        source.height,
        true
    )
    if (!upscaled.isRecycled) {
        upscaled.recycle()
    }
    if (smoothed !== source && !source.isRecycled) {
        source.recycle()
    }
    return smoothed
}
