package com.voicelike.app.stamp.cutout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import androidx.core.graphics.createBitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.roundToInt

private const val STAMP_AUTO_CUTOUT_MODEL_ASSET =
    "ml/stamp_cutout_u2netp_official_320_float32.tflite"
private const val STAMP_AUTO_CUTOUT_LOG_TAG = "StampAutoCutout"
private const val STAMP_AUTO_CUTOUT_FALLBACK_INPUT_SIDE = 320
private const val STAMP_AUTO_CUTOUT_MIN_THREADS = 2
private const val STAMP_AUTO_CUTOUT_MAX_THREADS = 4

private data class StampAutoCutoutModelSpec(
    val inputWidth: Int,
    val inputHeight: Int,
    val inputChannels: Int,
    val inputChannelFirst: Boolean,
    val inputType: DataType,
    val inputScale: Float,
    val inputZeroPoint: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val outputChannels: Int,
    val outputChannelFirst: Boolean,
    val outputType: DataType,
    val outputScale: Float,
    val outputZeroPoint: Int,
    val outputElementCount: Int
)

private data class StampAutoCutoutModelRuntime(
    val interpreter: Interpreter,
    val spec: StampAutoCutoutModelSpec
)

private data class StampAutoCutoutTensorGeometry(
    val width: Int,
    val height: Int,
    val channels: Int,
    val channelFirst: Boolean
)

private data class StampAutoCutoutModelInput(
    val bitmap: Bitmap,
    val contentLeft: Int,
    val contentTop: Int,
    val contentWidth: Int,
    val contentHeight: Int,
    val ownsBitmap: Boolean
)

internal object StampAutoCutoutModelSupport {
    private val lock = Any()
    @Volatile private var interpreter: Interpreter? = null
    @Volatile private var mappedModel: MappedByteBuffer? = null
    @Volatile private var modelSpec: StampAutoCutoutModelSpec? = null

    fun generateAutoCutout(
        context: Context,
        sourceBitmap: Bitmap
    ): Bitmap? {
        if (sourceBitmap.isRecycled || sourceBitmap.width <= 0 || sourceBitmap.height <= 0) return null
        val runtime = getModelRuntime(context) ?: return null
        return runCatching {
            synchronized(lock) {
                runInferenceLocked(
                    interpreter = runtime.interpreter,
                    spec = runtime.spec,
                    sourceBitmap = sourceBitmap
                )
            }
        }.onFailure { error ->
            Log.w(STAMP_AUTO_CUTOUT_LOG_TAG, "Auto cutout model inference failed", error)
        }.getOrNull()
    }

    fun warmUp(context: Context): Boolean =
        getModelRuntime(context) != null

    private fun getModelRuntime(context: Context): StampAutoCutoutModelRuntime? {
        val cachedInterpreter = interpreter
        val cachedSpec = modelSpec
        if (cachedInterpreter != null && cachedSpec != null) {
            return StampAutoCutoutModelRuntime(cachedInterpreter, cachedSpec)
        }
        synchronized(lock) {
            val safeCachedInterpreter = interpreter
            val safeCachedSpec = modelSpec
            if (safeCachedInterpreter != null && safeCachedSpec != null) {
                return StampAutoCutoutModelRuntime(safeCachedInterpreter, safeCachedSpec)
            }
            val model = mappedModel ?: mapModelFile(context)?.also {
                mappedModel = it
            } ?: return null
            val createdInterpreter = runCatching {
                Interpreter(
                    model,
                    Interpreter.Options().apply {
                        setUseXNNPACK(true)
                        setNumThreads(resolveInterpreterThreadCount())
                    }
                )
            }.getOrElse { error ->
                Log.w(STAMP_AUTO_CUTOUT_LOG_TAG, "Unable to create cutout interpreter", error)
                return null
            }
            val createdSpec = runCatching {
                buildModelSpec(createdInterpreter)
            }.getOrElse { error ->
                createdInterpreter.close()
                Log.w(STAMP_AUTO_CUTOUT_LOG_TAG, "Unable to resolve cutout model spec", error)
                return null
            }
            interpreter = createdInterpreter
            modelSpec = createdSpec
            Log.i(
                STAMP_AUTO_CUTOUT_LOG_TAG,
                "Cutout model ready input=${createdSpec.inputWidth}x${createdSpec.inputHeight}x${createdSpec.inputChannels}" +
                    " inputLayout=${if (createdSpec.inputChannelFirst) "NCHW" else "NHWC"}" +
                    " output=${createdSpec.outputWidth}x${createdSpec.outputHeight}x${createdSpec.outputChannels}" +
                    " outputLayout=${if (createdSpec.outputChannelFirst) "NCHW" else "NHWC"}"
            )
            return StampAutoCutoutModelRuntime(createdInterpreter, createdSpec)
        }
    }

    private fun resolveInterpreterThreadCount(): Int {
        val availableProcessors = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        return (availableProcessors / 2)
            .coerceIn(STAMP_AUTO_CUTOUT_MIN_THREADS, STAMP_AUTO_CUTOUT_MAX_THREADS)
    }

    private fun runInferenceLocked(
        interpreter: Interpreter,
        spec: StampAutoCutoutModelSpec,
        sourceBitmap: Bitmap
    ): Bitmap? {
        val modelInput = buildLetterboxedModelInput(
            sourceBitmap = sourceBitmap,
            targetWidth = spec.inputWidth.coerceAtLeast(1),
            targetHeight = spec.inputHeight.coerceAtLeast(1)
        ) ?: return null
        val inputBuffer = buildInputBuffer(
            bitmap = modelInput.bitmap,
            spec = spec
        ) ?: run {
            if (modelInput.ownsBitmap && !modelInput.bitmap.isRecycled) {
                modelInput.bitmap.recycle()
            }
            return null
        }
        val outputBuffer = ByteBuffer.allocateDirect(
            spec.outputElementCount.coerceAtLeast(1) * spec.outputType.byteSizeCompat()
        ).order(ByteOrder.nativeOrder())
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        val maskValues = decodeOutputMask(
            outputBuffer = outputBuffer,
            spec = spec
        ) ?: run {
            if (modelInput.ownsBitmap && !modelInput.bitmap.isRecycled) {
                modelInput.bitmap.recycle()
            }
            return null
        }
        if (modelInput.ownsBitmap && !modelInput.bitmap.isRecycled) {
            modelInput.bitmap.recycle()
        }
        val normalizedMask = normalizeAndOrientMask(
            values = maskValues,
            width = spec.outputWidth,
            height = spec.outputHeight
        ) ?: return null
        maybeFlipForegroundMaskInPlace(
            alpha = normalizedMask,
            width = spec.outputWidth,
            height = spec.outputHeight
        )
        simplifyForegroundMaskInPlace(
            alpha = normalizedMask,
            width = spec.outputWidth,
            height = spec.outputHeight
        )
        val smallMask = buildMaskBitmap(
            alpha = normalizedMask,
            width = spec.outputWidth,
            height = spec.outputHeight
        )
        val outputLeft = ((modelInput.contentLeft.toFloat() / spec.inputWidth.toFloat()) * spec.outputWidth.toFloat())
            .roundToInt()
            .coerceIn(0, (spec.outputWidth - 1).coerceAtLeast(0))
        val outputTop = ((modelInput.contentTop.toFloat() / spec.inputHeight.toFloat()) * spec.outputHeight.toFloat())
            .roundToInt()
            .coerceIn(0, (spec.outputHeight - 1).coerceAtLeast(0))
        val outputContentWidth = ((modelInput.contentWidth.toFloat() / spec.inputWidth.toFloat()) * spec.outputWidth.toFloat())
            .roundToInt()
            .coerceAtLeast(1)
            .coerceAtMost((spec.outputWidth - outputLeft).coerceAtLeast(1))
        val outputContentHeight = ((modelInput.contentHeight.toFloat() / spec.inputHeight.toFloat()) * spec.outputHeight.toFloat())
            .roundToInt()
            .coerceAtLeast(1)
            .coerceAtMost((spec.outputHeight - outputTop).coerceAtLeast(1))

        val contentMask = if (
            outputLeft == 0 &&
            outputTop == 0 &&
            outputContentWidth == smallMask.width &&
            outputContentHeight == smallMask.height
        ) {
            smallMask
        } else {
            runCatching {
                Bitmap.createBitmap(
                    smallMask,
                    outputLeft,
                    outputTop,
                    outputContentWidth,
                    outputContentHeight
                )
            }.getOrNull()?.also {
                if (!smallMask.isRecycled) {
                    smallMask.recycle()
                }
            } ?: smallMask
        }
        val fullMask = if (
            contentMask.width == sourceBitmap.width &&
            contentMask.height == sourceBitmap.height
        ) {
            contentMask
        } else {
            Bitmap.createScaledBitmap(
                contentMask,
                sourceBitmap.width.coerceAtLeast(1),
                sourceBitmap.height.coerceAtLeast(1),
                true
            ).also {
                if (contentMask !== smallMask && !contentMask.isRecycled) {
                    contentMask.recycle()
                } else if (contentMask === smallMask && !smallMask.isRecycled) {
                    smallMask.recycle()
                }
            }
        }
        val output = createBitmap(
            sourceBitmap.width.coerceAtLeast(1),
            sourceBitmap.height.coerceAtLeast(1)
        )
        val canvas = Canvas(output)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, null)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(fullMask, 0f, 0f, maskPaint)
        maskPaint.xfermode = null
        if (!fullMask.isRecycled) {
            fullMask.recycle()
        }
        return output
    }

    private fun simplifyForegroundMaskInPlace(
        alpha: FloatArray,
        width: Int,
        height: Int
    ) {
        if (alpha.isEmpty() || width <= 1 || height <= 1) return
        smoothAlphaMaskInPlace(
            alpha = alpha,
            width = width,
            height = height,
            passes = 1
        )
        recoverAttachedForegroundInPlace(
            alpha = alpha,
            width = width,
            height = height
        )
        clampAlphaEndpointsInPlace(
            alpha = alpha,
            low = 0.075f,
            high = 0.74f
        )
        fillSmallBackgroundHolesInPlace(
            alpha = alpha,
            width = width,
            height = height
        )
        suppressTinyForegroundSpecklesInPlace(
            alpha = alpha,
            width = width,
            height = height
        )
        smoothAlphaMaskInPlace(
            alpha = alpha,
            width = width,
            height = height,
            passes = 1
        )
        clampAlphaEndpointsInPlace(
            alpha = alpha,
            low = 0.10f,
            high = 0.88f
        )
        suppressBorderAttachedWeakForegroundInPlace(
            alpha = alpha,
            width = width,
            height = height
        )
    }

    private fun buildLetterboxedModelInput(
        sourceBitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): StampAutoCutoutModelInput? {
        if (
            sourceBitmap.isRecycled ||
            sourceBitmap.width <= 0 ||
            sourceBitmap.height <= 0 ||
            targetWidth <= 0 ||
            targetHeight <= 0
        ) {
            return null
        }
        if (sourceBitmap.width == targetWidth && sourceBitmap.height == targetHeight) {
            return StampAutoCutoutModelInput(
                bitmap = sourceBitmap,
                contentLeft = 0,
                contentTop = 0,
                contentWidth = targetWidth,
                contentHeight = targetHeight,
                ownsBitmap = false
            )
        }
        val scale = minOf(
            targetWidth.toFloat() / sourceBitmap.width.toFloat().coerceAtLeast(1f),
            targetHeight.toFloat() / sourceBitmap.height.toFloat().coerceAtLeast(1f)
        ).coerceAtLeast(0.0001f)
        val contentWidth = (sourceBitmap.width * scale).roundToInt()
            .coerceAtLeast(1)
            .coerceAtMost(targetWidth)
        val contentHeight = (sourceBitmap.height * scale).roundToInt()
            .coerceAtLeast(1)
            .coerceAtMost(targetHeight)
        val contentLeft = ((targetWidth - contentWidth) / 2f).roundToInt()
            .coerceIn(0, (targetWidth - contentWidth).coerceAtLeast(0))
        val contentTop = ((targetHeight - contentHeight) / 2f).roundToInt()
            .coerceIn(0, (targetHeight - contentHeight).coerceAtLeast(0))
        val output = createBitmap(targetWidth, targetHeight)
        val canvas = Canvas(output)
        canvas.drawColor(sampleBorderMeanColor(sourceBitmap))
        canvas.drawBitmap(
            sourceBitmap,
            null,
            android.graphics.Rect(
                contentLeft,
                contentTop,
                contentLeft + contentWidth,
                contentTop + contentHeight
            ),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        )
        return StampAutoCutoutModelInput(
            bitmap = output,
            contentLeft = contentLeft,
            contentTop = contentTop,
            contentWidth = contentWidth,
            contentHeight = contentHeight,
            ownsBitmap = true
        )
    }

    private fun sampleBorderMeanColor(sourceBitmap: Bitmap): Int {
        if (sourceBitmap.isRecycled || sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            return Color.BLACK
        }
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val border = (minOf(width, height) * 0.07f).roundToInt().coerceIn(1, (minOf(width, height) / 2).coerceAtLeast(1))
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var weightSum = 0L
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x >= border && x < width - border && y >= border && y < height - border) {
                    continue
                }
                val color = pixels[y * width + x]
                val alpha = (color ushr 24).coerceIn(0, 255)
                if (alpha <= 0) continue
                val weight = alpha.toLong()
                redSum += ((color shr 16) and 0xFF).toLong() * weight
                greenSum += ((color shr 8) and 0xFF).toLong() * weight
                blueSum += (color and 0xFF).toLong() * weight
                weightSum += weight
            }
        }
        if (weightSum <= 0L) {
            return Color.BLACK
        }
        val red = (redSum / weightSum).toInt().coerceIn(0, 255)
        val green = (greenSum / weightSum).toInt().coerceIn(0, 255)
        val blue = (blueSum / weightSum).toInt().coerceIn(0, 255)
        return Color.rgb(red, green, blue)
    }

    private fun buildInputBuffer(
        bitmap: Bitmap,
        spec: StampAutoCutoutModelSpec
    ): ByteBuffer? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val channels = spec.inputChannels.coerceIn(1, 4)
        val inputBuffer = ByteBuffer.allocateDirect(
            width * height * channels * spec.inputType.byteSizeCompat()
        ).order(ByteOrder.nativeOrder())
        fun channelValue(color: Int, channel: Int): Float {
            val rgbUnit = when (channel) {
                0 -> ((color shr 16) and 0xFF) / 255f
                1 -> ((color shr 8) and 0xFF) / 255f
                2 -> (color and 0xFF) / 255f
                else -> {
                    val red = (color shr 16) and 0xFF
                    val green = (color shr 8) and 0xFF
                    val blue = color and 0xFF
                    ((red + green + blue) / (3f * 255f)).coerceIn(0f, 1f)
                }
            }
            return when (channel) {
                0 -> (rgbUnit - 0.485f) / 0.229f
                1 -> (rgbUnit - 0.456f) / 0.224f
                2 -> (rgbUnit - 0.406f) / 0.225f
                else -> (rgbUnit - 0.5f) / 0.5f
            }
        }
        if (spec.inputChannelFirst) {
            for (channel in 0 until channels) {
                for (pixelIndex in pixels.indices) {
                    putValueToBuffer(
                        buffer = inputBuffer,
                        dataType = spec.inputType,
                        value = channelValue(pixels[pixelIndex], channel),
                        scale = spec.inputScale,
                        zeroPoint = spec.inputZeroPoint
                    )
                }
            }
        } else {
            for (pixelIndex in pixels.indices) {
                val color = pixels[pixelIndex]
                for (channel in 0 until channels) {
                    putValueToBuffer(
                        buffer = inputBuffer,
                        dataType = spec.inputType,
                        value = channelValue(color, channel),
                        scale = spec.inputScale,
                        zeroPoint = spec.inputZeroPoint
                    )
                }
            }
        }
        inputBuffer.rewind()
        return inputBuffer
    }

    private fun decodeOutputMask(
        outputBuffer: ByteBuffer,
        spec: StampAutoCutoutModelSpec
    ): FloatArray? {
        if (spec.outputWidth <= 0 || spec.outputHeight <= 0) return null
        val values = FloatArray(spec.outputElementCount.coerceAtLeast(1))
        when (spec.outputType) {
            DataType.FLOAT32 -> {
                for (index in values.indices) {
                    if (outputBuffer.remaining() < 4) break
                    values[index] = outputBuffer.float
                }
            }
            DataType.INT8 -> {
                for (index in values.indices) {
                    if (!outputBuffer.hasRemaining()) break
                    val quantized = outputBuffer.get().toInt()
                    values[index] = (quantized - spec.outputZeroPoint) * spec.outputScale
                }
            }
            DataType.UINT8 -> {
                for (index in values.indices) {
                    if (!outputBuffer.hasRemaining()) break
                    val quantized = outputBuffer.get().toInt() and 0xFF
                    values[index] = (quantized - spec.outputZeroPoint) * spec.outputScale
                }
            }
            else -> return null
        }
        val pixelCount = spec.outputWidth * spec.outputHeight
        val mask = FloatArray(pixelCount)
        val channelCount = spec.outputChannels.coerceAtLeast(1)
        if (channelCount == 1) {
            for (pixelIndex in 0 until pixelCount) {
                val sourceIndex = pixelIndex.coerceAtMost(values.lastIndex)
                mask[pixelIndex] = values[sourceIndex]
            }
            return mask
        }
        val channelStart = if (channelCount > 1) 1 else 0
        if (spec.outputChannelFirst) {
            for (pixelIndex in 0 until pixelCount) {
                var foregroundScore = Float.NEGATIVE_INFINITY
                for (channel in channelStart until channelCount) {
                    val sourceIndex = channel * pixelCount + pixelIndex
                    if (sourceIndex !in values.indices) continue
                    val score = values[sourceIndex]
                    if (score > foregroundScore) {
                        foregroundScore = score
                    }
                }
                if (!foregroundScore.isFinite()) {
                    foregroundScore = values[pixelIndex.coerceAtMost(values.lastIndex)]
                }
                mask[pixelIndex] = foregroundScore
            }
            return mask
        }
        for (pixelIndex in 0 until pixelCount) {
            val base = pixelIndex * channelCount
            if (base >= values.size) break
            var foregroundScore = values[(base + channelStart).coerceAtMost(values.lastIndex)]
            for (channel in (channelStart + 1) until channelCount) {
                val sourceIndex = base + channel
                if (sourceIndex >= values.size) break
                val score = values[sourceIndex]
                if (score > foregroundScore) {
                    foregroundScore = score
                }
            }
            mask[pixelIndex] = foregroundScore
        }
        return mask
    }

    private fun normalizeAndOrientMask(
        values: FloatArray,
        width: Int,
        height: Int
    ): FloatArray? {
        if (values.isEmpty() || width <= 0 || height <= 0) return null
        var minValue = Float.POSITIVE_INFINITY
        var maxValue = Float.NEGATIVE_INFINITY
        for (index in values.indices) {
            val value = values[index]
            if (value < minValue) minValue = value
            if (value > maxValue) maxValue = value
        }
        val range = maxValue - minValue
        if (range <= 1e-6f) return null
        val normalized = FloatArray(values.size)
        for (index in values.indices) {
            val unit = ((values[index] - minValue) / range).coerceIn(0f, 1f)
            val stretched = ((unit - 0.04f) / 0.96f).coerceIn(0f, 1f)
            normalized[index] = stretched * stretched * (3f - 2f * stretched)
        }
        return normalized
    }

    private fun maybeFlipForegroundMaskInPlace(
        alpha: FloatArray,
        width: Int,
        height: Int
    ) {
        if (width <= 1 || height <= 1) return
        val borderMean = computeBorderMean(alpha, width, height)
        val centerMean = computeCenterMean(alpha, width, height)
        val cornerMean = computeCornerMean(alpha, width, height)
        val borderStrongRatio = computeBorderStrongRatio(
            values = alpha,
            width = width,
            height = height,
            threshold = 0.6f
        )
        val centerStrongRatio = computeCenterStrongRatio(
            values = alpha,
            width = width,
            height = height,
            threshold = 0.6f
        )
        val shouldFlip = (
            borderMean > centerMean + 0.16f &&
                borderStrongRatio > centerStrongRatio + 0.18f &&
                borderStrongRatio > 0.28f &&
                centerStrongRatio < 0.22f
            ) || (
            cornerMean > centerMean + 0.22f &&
                borderStrongRatio > 0.42f &&
                centerStrongRatio < 0.36f
            )
        if (!shouldFlip) return
        for (index in alpha.indices) {
            alpha[index] = 1f - alpha[index]
        }
    }

    private fun recoverAttachedForegroundInPlace(
        alpha: FloatArray,
        width: Int,
        height: Int
    ) {
        if (alpha.isEmpty() || width <= 2 || height <= 2) return
        val total = width * height
        val strongThreshold = 0.52f
        val attachedThreshold = 0.17f
        var current = BooleanArray(total)
        for (index in 0 until total) {
            current[index] = alpha[index] >= strongThreshold
        }
        repeat(2) {
            val next = current.copyOf()
            for (y in 0 until height) {
                val top = (y - 1).coerceAtLeast(0)
                val bottom = (y + 1).coerceAtMost(height - 1)
                for (x in 0 until width) {
                    val index = y * width + x
                    if (next[index] || alpha[index] < attachedThreshold) continue
                    var touchesForeground = false
                    for (ny in top..bottom) {
                        val rowOffset = ny * width
                        for (nx in (x - 1).coerceAtLeast(0)..(x + 1).coerceAtMost(width - 1)) {
                            if (current[rowOffset + nx]) {
                                touchesForeground = true
                                break
                            }
                        }
                        if (touchesForeground) break
                    }
                    if (touchesForeground) {
                        next[index] = true
                    }
                }
            }
            current = next
        }
        for (index in 0 until total) {
            if (current[index] && alpha[index] < strongThreshold) {
                alpha[index] = maxOf(alpha[index], 0.46f)
            }
        }
    }

    private fun fillSmallBackgroundHolesInPlace(
        alpha: FloatArray,
        width: Int,
        height: Int
    ) {
        if (width <= 2 || height <= 2) return
        val threshold = 0.12f
        val total = width * height
        val foreground = BooleanArray(total)
        var foregroundCount = 0
        for (index in 0 until total) {
            val keep = alpha[index] >= threshold
            foreground[index] = keep
            if (keep) foregroundCount += 1
        }
        if (foregroundCount <= 0) return
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

        val holeQueue = IntArray(total)
        val holePixels = IntArray(total)
        for (seed in 0 until total) {
            if (visited[seed] || foreground[seed]) continue
            var holeHead = 0
            var holeTail = 0
            var holeSize = 0
            holeQueue[holeTail++] = seed
            visited[seed] = true
            while (holeHead < holeTail) {
                val current = holeQueue[holeHead++]
                holePixels[holeSize++] = current
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
                            holeQueue[holeTail++] = neighbor
                        }
                    }
                }
            }
            for (index in 0 until holeSize) {
                val pixelIndex = holePixels[index]
                alpha[pixelIndex] = 1f
            }
        }
    }

    private fun smoothAlphaMaskInPlace(
        alpha: FloatArray,
        width: Int,
        height: Int,
        passes: Int
    ) {
        if (passes <= 0 || width <= 1 || height <= 1) return
        var current = alpha
        var buffer = FloatArray(width * height)
        repeat(passes) {
            for (y in 0 until height) {
                val top = (y - 1).coerceAtLeast(0)
                val bottom = (y + 1).coerceAtMost(height - 1)
                for (x in 0 until width) {
                    val left = (x - 1).coerceAtLeast(0)
                    val right = (x + 1).coerceAtMost(width - 1)
                    var sum = 0f
                    var count = 0
                    for (ny in top..bottom) {
                        val rowOffset = ny * width
                        for (nx in left..right) {
                            sum += current[rowOffset + nx]
                            count += 1
                        }
                    }
                    buffer[y * width + x] = (sum / count.coerceAtLeast(1).toFloat())
                        .coerceIn(0f, 1f)
                }
            }
            val swap = current
            current = buffer
            buffer = swap
        }
        if (current !== alpha) {
            System.arraycopy(current, 0, alpha, 0, alpha.size)
        }
    }

    private fun clampAlphaEndpointsInPlace(
        alpha: FloatArray,
        low: Float,
        high: Float
    ) {
        if (alpha.isEmpty()) return
        val safeLow = low.coerceIn(0f, 1f)
        val safeHigh = high.coerceIn((safeLow + 0.0001f), 1f)
        val denom = (safeHigh - safeLow).coerceAtLeast(0.0001f)
        for (index in alpha.indices) {
            val value = alpha[index]
            alpha[index] = when {
                value <= safeLow -> 0f
                value >= safeHigh -> 1f
                else -> ((value - safeLow) / denom).coerceIn(0f, 1f)
            }
        }
    }

    private fun suppressTinyForegroundSpecklesInPlace(
        alpha: FloatArray,
        width: Int,
        height: Int
    ) {
        if (alpha.isEmpty() || width <= 2 || height <= 2) return
        val total = width * height
        val threshold = 0.24f
        val foreground = BooleanArray(total)
        var foregroundCount = 0
        for (index in 0 until total) {
            val keep = alpha[index] >= threshold
            foreground[index] = keep
            if (keep) foregroundCount += 1
        }
        if (foregroundCount <= 0) return
        val labels = IntArray(total) { -1 }
        val queue = IntArray(total)
        val componentSizes = ArrayList<Int>(24)
        var largestSize = 0
        for (seed in 0 until total) {
            if (!foreground[seed] || labels[seed] >= 0) continue
            val label = componentSizes.size
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
                        if (foreground[neighbor] && labels[neighbor] < 0) {
                            labels[neighbor] = label
                            queue[tail++] = neighbor
                        }
                    }
                }
            }
            componentSizes.add(size)
            if (size > largestSize) {
                largestSize = size
            }
        }
        if (componentSizes.isEmpty()) return
        val minByLargest = (largestSize * 0.0038f).roundToInt().coerceAtLeast(1)
        val minByTotal = (total * 0.0001f).roundToInt().coerceAtLeast(6)
        val minKeepSize = maxOf(minByLargest, minByTotal)
        if (largestSize <= minKeepSize) return
        for (index in 0 until total) {
            val label = labels[index]
            if (label < 0) continue
            if (componentSizes[label] < minKeepSize) {
                alpha[index] = 0f
            }
        }
    }

    private fun suppressBorderAttachedWeakForegroundInPlace(
        alpha: FloatArray,
        width: Int,
        height: Int
    ) {
        if (alpha.isEmpty() || width <= 2 || height <= 2) return
        val total = width * height
        val threshold = 0.42f
        val visited = BooleanArray(total)
        val queue = IntArray(total)
        var head = 0
        var tail = 0

        fun seed(index: Int) {
            if (index < 0 || index >= total) return
            if (visited[index] || alpha[index] > threshold) return
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
                    if (!visited[neighbor] && alpha[neighbor] <= threshold) {
                        visited[neighbor] = true
                        queue[tail++] = neighbor
                    }
                }
            }
        }

        for (index in 0 until total) {
            if (visited[index]) {
                alpha[index] = 0f
            }
        }
    }

    private fun buildMaskBitmap(
        alpha: FloatArray,
        width: Int,
        height: Int
    ): Bitmap {
        val pixels = IntArray(width * height)
        for (index in pixels.indices) {
            val alphaInt = (alpha[index].coerceIn(0f, 1f) * 255f).roundToInt()
                .coerceIn(0, 255)
            pixels[index] = Color.argb(alphaInt, 255, 255, 255)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun computeBorderMean(
        values: FloatArray,
        width: Int,
        height: Int
    ): Float {
        val maxBorder = (minOf(width, height) / 2).coerceAtLeast(1)
        val border = (minOf(width, height) * 0.08f).roundToInt().coerceIn(1, maxBorder)
        var sum = 0f
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < border || x >= width - border || y < border || y >= height - border) {
                    sum += values[y * width + x]
                    count += 1
                }
            }
        }
        if (count <= 0) return 0f
        return sum / count.toFloat()
    }

    private fun computeCenterMean(
        values: FloatArray,
        width: Int,
        height: Int
    ): Float {
        val left = (width * 0.2f).roundToInt().coerceIn(0, width - 1)
        val right = (width * 0.8f).roundToInt().coerceIn(left + 1, width)
        val top = (height * 0.2f).roundToInt().coerceIn(0, height - 1)
        val bottom = (height * 0.8f).roundToInt().coerceIn(top + 1, height)
        var sum = 0f
        var count = 0
        for (y in top until bottom) {
            val rowOffset = y * width
            for (x in left until right) {
                sum += values[rowOffset + x]
                count += 1
            }
        }
        if (count <= 0) return 0f
        return sum / count.toFloat()
    }

    private fun computeCornerMean(
        values: FloatArray,
        width: Int,
        height: Int
    ): Float {
        val cornerWidth = (width * 0.18f).roundToInt().coerceIn(1, width)
        val cornerHeight = (height * 0.18f).roundToInt().coerceIn(1, height)
        var sum = 0f
        var count = 0
        fun sample(left: Int, top: Int) {
            for (y in top until (top + cornerHeight).coerceAtMost(height)) {
                val rowOffset = y * width
                for (x in left until (left + cornerWidth).coerceAtMost(width)) {
                    sum += values[rowOffset + x]
                    count += 1
                }
            }
        }
        sample(0, 0)
        sample((width - cornerWidth).coerceAtLeast(0), 0)
        sample(0, (height - cornerHeight).coerceAtLeast(0))
        sample((width - cornerWidth).coerceAtLeast(0), (height - cornerHeight).coerceAtLeast(0))
        if (count <= 0) return 0f
        return sum / count.toFloat()
    }

    private fun computeBorderStrongRatio(
        values: FloatArray,
        width: Int,
        height: Int,
        threshold: Float
    ): Float {
        val maxBorder = (minOf(width, height) / 2).coerceAtLeast(1)
        val border = (minOf(width, height) * 0.08f).roundToInt().coerceIn(1, maxBorder)
        var count = 0
        var strong = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < border || x >= width - border || y < border || y >= height - border) {
                    count += 1
                    if (values[y * width + x] >= threshold) {
                        strong += 1
                    }
                }
            }
        }
        if (count <= 0) return 0f
        return strong.toFloat() / count.toFloat()
    }

    private fun computeCenterStrongRatio(
        values: FloatArray,
        width: Int,
        height: Int,
        threshold: Float
    ): Float {
        val left = (width * 0.2f).roundToInt().coerceIn(0, width - 1)
        val right = (width * 0.8f).roundToInt().coerceIn(left + 1, width)
        val top = (height * 0.2f).roundToInt().coerceIn(0, height - 1)
        val bottom = (height * 0.8f).roundToInt().coerceIn(top + 1, height)
        var count = 0
        var strong = 0
        for (y in top until bottom) {
            val rowOffset = y * width
            for (x in left until right) {
                count += 1
                if (values[rowOffset + x] >= threshold) {
                    strong += 1
                }
            }
        }
        if (count <= 0) return 0f
        return strong.toFloat() / count.toFloat()
    }

    private fun buildModelSpec(interpreter: Interpreter): StampAutoCutoutModelSpec {
        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()
        val inputGeometry = parseTensorGeometry(
            shape = inputShape,
            fallbackWidth = STAMP_AUTO_CUTOUT_FALLBACK_INPUT_SIDE,
            fallbackHeight = STAMP_AUTO_CUTOUT_FALLBACK_INPUT_SIDE,
            fallbackChannels = 3
        )
        val inputQuantization = inputTensor.quantizationParams()
        val inputScale = inputQuantization.scale.takeIf { it > 0f } ?: 1f
        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val outputGeometry = parseTensorGeometry(
            shape = outputShape,
            fallbackWidth = inputGeometry.width,
            fallbackHeight = inputGeometry.height,
            fallbackChannels = 1
        )
        val outputQuantization = outputTensor.quantizationParams()
        val outputScale = outputQuantization.scale.takeIf { it > 0f } ?: 1f
        val outputElementCount = outputShape
            .fold(1) { acc, value ->
                val safeValue = value.coerceAtLeast(1)
                (acc * safeValue).coerceAtLeast(1)
            }
        return StampAutoCutoutModelSpec(
            inputWidth = inputGeometry.width,
            inputHeight = inputGeometry.height,
            inputChannels = inputGeometry.channels,
            inputChannelFirst = inputGeometry.channelFirst,
            inputType = inputTensor.dataType(),
            inputScale = inputScale,
            inputZeroPoint = inputQuantization.zeroPoint,
            outputWidth = outputGeometry.width,
            outputHeight = outputGeometry.height,
            outputChannels = outputGeometry.channels,
            outputChannelFirst = outputGeometry.channelFirst,
            outputType = outputTensor.dataType(),
            outputScale = outputScale,
            outputZeroPoint = outputQuantization.zeroPoint,
            outputElementCount = outputElementCount
        )
    }

    private fun parseTensorGeometry(
        shape: IntArray,
        fallbackWidth: Int,
        fallbackHeight: Int,
        fallbackChannels: Int
    ): StampAutoCutoutTensorGeometry {
        val fallback = StampAutoCutoutTensorGeometry(
            width = fallbackWidth.coerceAtLeast(1),
            height = fallbackHeight.coerceAtLeast(1),
            channels = fallbackChannels.coerceAtLeast(1),
            channelFirst = false
        )
        if (shape.isEmpty()) return fallback

        fun geometryFromHwc(height: Int, width: Int, channels: Int): StampAutoCutoutTensorGeometry =
            StampAutoCutoutTensorGeometry(
                width = width.coerceAtLeast(1),
                height = height.coerceAtLeast(1),
                channels = channels.coerceIn(1, 4),
                channelFirst = false
            )

        fun geometryFromChw(channels: Int, height: Int, width: Int): StampAutoCutoutTensorGeometry =
            StampAutoCutoutTensorGeometry(
                width = width.coerceAtLeast(1),
                height = height.coerceAtLeast(1),
                channels = channels.coerceIn(1, 4),
                channelFirst = true
            )

        return when (shape.size) {
            4 -> {
                val d1 = shape[1].coerceAtLeast(1)
                val d2 = shape[2].coerceAtLeast(1)
                val d3 = shape[3].coerceAtLeast(1)
                when {
                    d3 <= 4 && d1 > 4 && d2 > 4 -> geometryFromHwc(
                        height = d1,
                        width = d2,
                        channels = d3
                    )
                    d1 <= 4 && d2 > 4 && d3 > 4 -> geometryFromChw(
                        channels = d1,
                        height = d2,
                        width = d3
                    )
                    else -> geometryFromHwc(
                        height = d1,
                        width = d2,
                        channels = d3
                    )
                }
            }
            3 -> {
                val d0 = shape[0].coerceAtLeast(1)
                val d1 = shape[1].coerceAtLeast(1)
                val d2 = shape[2].coerceAtLeast(1)
                when {
                    d2 <= 4 && d0 > 4 && d1 > 4 -> geometryFromHwc(
                        height = d0,
                        width = d1,
                        channels = d2
                    )
                    d0 <= 4 && d1 > 4 && d2 > 4 -> geometryFromChw(
                        channels = d0,
                        height = d1,
                        width = d2
                    )
                    d0 == 1 && d1 > 1 && d2 > 1 -> geometryFromHwc(
                        height = d1,
                        width = d2,
                        channels = 1
                    )
                    else -> geometryFromHwc(
                        height = d0,
                        width = d1,
                        channels = d2
                    )
                }
            }
            2 -> geometryFromHwc(
                height = shape[0].coerceAtLeast(1),
                width = shape[1].coerceAtLeast(1),
                channels = 1
            )
            else -> fallback
        }
    }

    private fun mapModelFile(context: Context): MappedByteBuffer? {
        return runCatching {
            context.assets.openFd(STAMP_AUTO_CUTOUT_MODEL_ASSET).use { afd ->
                FileInputStream(afd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
                }
            }
        }.getOrElse { error ->
            Log.w(
                STAMP_AUTO_CUTOUT_LOG_TAG,
                "Unable to map model asset $STAMP_AUTO_CUTOUT_MODEL_ASSET",
                error
            )
            null
        }
    }
}

private fun putValueToBuffer(
    buffer: ByteBuffer,
    dataType: DataType,
    value: Float,
    scale: Float,
    zeroPoint: Int
) {
    when (dataType) {
        DataType.FLOAT32 -> buffer.putFloat(value)
        DataType.INT8 -> {
            val safeScale = scale.takeIf { it > 0f } ?: 1f
            val quantized = (value / safeScale + zeroPoint).roundToInt().coerceIn(-128, 127)
            buffer.put(quantized.toByte())
        }
        DataType.UINT8 -> {
            val safeScale = scale.takeIf { it > 0f } ?: 1f
            val quantized = (value / safeScale + zeroPoint).roundToInt().coerceIn(0, 255)
            buffer.put(quantized.toByte())
        }
        else -> {
            buffer.putFloat(value)
        }
    }
}

private fun DataType.byteSizeCompat(): Int {
    return when (this) {
        DataType.FLOAT32 -> 4
        DataType.INT32 -> 4
        DataType.INT64 -> 8
        DataType.INT16 -> 2
        DataType.INT8 -> 1
        DataType.UINT8 -> 1
        else -> 4
    }
}
