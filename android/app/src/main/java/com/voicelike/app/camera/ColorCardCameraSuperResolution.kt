package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val COLOR_CARD_CAMERA_SR_MODEL_ASSET = "ml/real_esrgan_general_x4v3.tflite"
private const val COLOR_CARD_CAMERA_SR_INPUT_SIZE = 128
private const val COLOR_CARD_CAMERA_SR_MODEL_SCALE = 4
private const val COLOR_CARD_CAMERA_SR_TILE_OVERLAP = 8
private const val COLOR_CARD_CAMERA_SR_TILE_STRIDE =
    COLOR_CARD_CAMERA_SR_INPUT_SIZE - (COLOR_CARD_CAMERA_SR_TILE_OVERLAP * 2)
private const val COLOR_CARD_CAMERA_SR_MIN_FOCAL = 34.5f
private const val COLOR_CARD_CAMERA_SR_MAX_FOCAL = 50.5f
private const val COLOR_CARD_CAMERA_SR_35_MIN_FOCAL = 34.5f
private const val COLOR_CARD_CAMERA_SR_35_MAX_FOCAL = 36.5f
private const val COLOR_CARD_CAMERA_SR_50_MIN_FOCAL = 48.5f
private const val COLOR_CARD_CAMERA_SR_50_MAX_FOCAL = 50.5f
private const val COLOR_CARD_CAMERA_SR_LOG_TAG = "ColorCardCameraSR"

private enum class ColorCardCameraSrBackend {
    GPU,
    NNAPI,
    CPU
}

private data class ColorCardCameraPixelBuffer(
    val width: Int,
    val height: Int,
    val pixels: IntArray
)

private data class ColorCardCameraSrProfile(
    val minBoost: Float,
    val maxBoost: Float,
    val maxPreparedSide: Int,
    val maxTileCount: Int,
    val blendAlphaNear: Float,
    val blendAlphaFar: Float,
    val residualAlphaNear: Float,
    val residualAlphaFar: Float,
    val maxCoverage: Float,
    val clarityNear: Float,
    val clarityFar: Float
)

private data class ColorCardCameraSrReferences(
    val originalReferenceTarget: ColorCardCameraPixelBuffer,
    val bicubicTarget: ColorCardCameraPixelBuffer,
    val prepared: ColorCardCameraPixelBuffer,
    val bicubicReference: ColorCardCameraPixelBuffer
)

internal object ColorCardCameraSuperResolution {
    private val lock = Any()
    @Volatile
    private var interpreter: Interpreter? = null
    @Volatile
    private var mappedModel: MappedByteBuffer? = null
    @Volatile
    private var gpuDelegate: GpuDelegate? = null
    @Volatile
    private var backend: ColorCardCameraSrBackend? = null

    fun shouldEnhance(
        renderedFocal35mmEq: Float?,
        coverage: Float
    ): Boolean {
        val focal = renderedFocal35mmEq ?: return false
        val profile = resolveColorCardCameraSrProfile(focal) ?: return false
        return focal in COLOR_CARD_CAMERA_SR_MIN_FOCAL..COLOR_CARD_CAMERA_SR_MAX_FOCAL &&
            coverage < profile.maxCoverage
    }

    fun upscaleCroppedBitmap(
        context: Context,
        croppedBitmap: Bitmap,
        renderedFocal35mmEq: Float?,
        coverage: Float,
        targetWidth: Int,
        targetHeight: Int,
        consumeSourceBitmap: Boolean = false
    ): Bitmap? {
        if (croppedBitmap.width <= 0 || croppedBitmap.height <= 0) return null

        val desiredScale = max(
            targetWidth.toFloat() / croppedBitmap.width.coerceAtLeast(1).toFloat(),
            targetHeight.toFloat() / croppedBitmap.height.coerceAtLeast(1).toFloat()
        )
        val srProfile = resolveColorCardCameraSrProfile(renderedFocal35mmEq)
            ?: return pixelBufferToBitmap(bitmapToPixelBuffer(croppedBitmap))
        var source: ColorCardCameraPixelBuffer? = bitmapToPixelBuffer(croppedBitmap)
        if (consumeSourceBitmap && !croppedBitmap.isRecycled) {
            croppedBitmap.recycle()
        }
        val effectiveDesiredScale = max(desiredScale, srProfile.minBoost)
        Log.i(
            COLOR_CARD_CAMERA_SR_LOG_TAG,
            "SR engaged focal=${renderedFocal35mmEq ?: -1f} coverage=$coverage target=${targetWidth}x$targetHeight desiredScale=$effectiveDesiredScale"
        )
        val conservativeBoost = effectiveDesiredScale.coerceIn(
            srProfile.minBoost,
            srProfile.maxBoost
        )
        val references = buildColorCardCameraSrReferences(
            source = source ?: return null,
            coverage = coverage,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            srProfile = srProfile,
            conservativeBoost = conservativeBoost
        )
        source = null
        val interpreter = getInterpreter(context)
        var srPixels = interpreter?.let {
            runCatching { upscaleWithTiles(it, references.prepared) }
                .onFailure { error -> Log.w(COLOR_CARD_CAMERA_SR_LOG_TAG, "SR inference failed", error) }
                .getOrNull()
        }

        val blendedBoost = if (srPixels != null) {
            val alpha = when {
                effectiveDesiredScale >= 1.8f -> srProfile.blendAlphaFar
                else -> srProfile.blendAlphaNear
            }
            blendPixelBuffersInPlace(
                base = references.bicubicReference,
                detail = srPixels,
                alpha = alpha
            )
        } else {
            references.bicubicReference
        }
        srPixels = null

        val modelTarget = if (blendedBoost.width == targetWidth && blendedBoost.height == targetHeight) {
            blendedBoost
        } else {
            resamplePixelBuffer(blendedBoost, targetWidth, targetHeight)
        }
        val finalized = mergeSuperResolutionResidualInPlace(
            base = references.originalReferenceTarget,
            model = modelTarget,
            reference = references.bicubicTarget,
            alpha = if (effectiveDesiredScale >= 1.8f) srProfile.residualAlphaFar else srProfile.residualAlphaNear
        )
        val clarityBoost = if (effectiveDesiredScale >= 1.8f) {
            srProfile.clarityFar
        } else {
            srProfile.clarityNear
        }
        val clarified = applyClarityBoost(finalized, clarityBoost)
        return pixelBufferToBitmap(clarified)
    }

    private fun buildColorCardCameraSrReferences(
        source: ColorCardCameraPixelBuffer,
        coverage: Float,
        targetWidth: Int,
        targetHeight: Int,
        srProfile: ColorCardCameraSrProfile,
        conservativeBoost: Float
    ): ColorCardCameraSrReferences {
        val denoised = applyAdaptiveDenoise(source, coverage)
        val originalReferenceTarget = if (denoised.width == targetWidth && denoised.height == targetHeight) {
            denoised
        } else {
            resamplePixelBuffer(denoised, targetWidth, targetHeight)
        }
        val bicubicTarget = if (source.width == targetWidth && source.height == targetHeight) {
            source
        } else {
            resamplePixelBuffer(source, targetWidth, targetHeight)
        }
        val boostedTargetWidth = min(
            targetWidth,
            (denoised.width * conservativeBoost).roundToInt().coerceAtLeast(1)
        )
        val boostedTargetHeight = min(
            targetHeight,
            (denoised.height * conservativeBoost).roundToInt().coerceAtLeast(1)
        )
        val preparedScale = resolvePreparedScale(
            sourceWidth = denoised.width,
            sourceHeight = denoised.height,
            desiredBoost = conservativeBoost,
            maxPreparedSide = srProfile.maxPreparedSide,
            maxTileCount = srProfile.maxTileCount
        )
        val prepared = if (preparedScale >= 0.98f) {
            denoised
        } else {
            resamplePixelBuffer(
                denoised,
                (denoised.width * preparedScale).roundToInt().coerceAtLeast(1),
                (denoised.height * preparedScale).roundToInt().coerceAtLeast(1)
            )
        }
        val bicubicReference = if (denoised.width == boostedTargetWidth && denoised.height == boostedTargetHeight) {
            denoised
        } else {
            resamplePixelBuffer(denoised, boostedTargetWidth, boostedTargetHeight)
        }
        return ColorCardCameraSrReferences(
            originalReferenceTarget = originalReferenceTarget,
            bicubicTarget = bicubicTarget,
            prepared = prepared,
            bicubicReference = bicubicReference
        )
    }

    private fun getInterpreter(context: Context): Interpreter? {
        interpreter?.let { return it }
        synchronized(lock) {
            interpreter?.let { return it }
            val model = mappedModel ?: mapModelFile(context, COLOR_CARD_CAMERA_SR_MODEL_ASSET)?.also {
                mappedModel = it
            } ?: return null

            val gpuCompatibility = CompatibilityList()
            if (gpuCompatibility.isDelegateSupportedOnThisDevice) {
                runCatching {
                    val delegate = GpuDelegate()
                    val options = Interpreter.Options().apply {
                        addDelegate(delegate)
                        setAllowBufferHandleOutput(true)
                    }
                    val created = Interpreter(model, options)
                    gpuDelegate = delegate
                    backend = ColorCardCameraSrBackend.GPU
                    Log.i(COLOR_CARD_CAMERA_SR_LOG_TAG, "Using GPU delegate for SR")
                    created
                }.getOrNull()?.let {
                    interpreter = it
                    return it
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching {
                    val options = Interpreter.Options().apply {
                        setUseNNAPI(true)
                        setNumThreads(2)
                    }
                    val created = Interpreter(model, options)
                    backend = ColorCardCameraSrBackend.NNAPI
                    Log.i(COLOR_CARD_CAMERA_SR_LOG_TAG, "Using NNAPI backend for SR")
                    created
                }.getOrNull()?.let {
                    interpreter = it
                    return it
                }
            }

            val options = Interpreter.Options().apply {
                setUseXNNPACK(true)
                setNumThreads(2)
            }
            return Interpreter(model, options).also {
                interpreter = it
                backend = ColorCardCameraSrBackend.CPU
                Log.i(COLOR_CARD_CAMERA_SR_LOG_TAG, "Using CPU/XNNPACK backend for SR")
            }
        }
    }

    private fun resolvePreparedScale(
        sourceWidth: Int,
        sourceHeight: Int,
        desiredBoost: Float,
        maxPreparedSide: Int,
        maxTileCount: Int
    ): Float {
        var scale = min(
            desiredBoost / COLOR_CARD_CAMERA_SR_MODEL_SCALE.toFloat(),
            maxPreparedSide.toFloat() /
                max(sourceWidth, sourceHeight).coerceAtLeast(1).toFloat()
        ).coerceIn(0.16f, 1f)
        repeat(8) {
            val preparedWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
            val preparedHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
            val tileCount = estimateTileCount(preparedWidth, preparedHeight)
            if (tileCount <= maxTileCount) {
                return scale
            }
            scale = (scale * 0.88f).coerceAtLeast(0.16f)
        }
        return scale
    }

    private fun resolveColorCardCameraSrProfile(
        renderedFocal35mmEq: Float?
    ): ColorCardCameraSrProfile? {
        val focal = renderedFocal35mmEq ?: return null
        return when {
            focal in COLOR_CARD_CAMERA_SR_35_MIN_FOCAL..COLOR_CARD_CAMERA_SR_35_MAX_FOCAL ->
                ColorCardCameraSrProfile(
                    minBoost = 1.38f,
                    maxBoost = 1.60f,
                    maxPreparedSide = 384,
                    maxTileCount = 8,
                    blendAlphaNear = 0.46f,
                    blendAlphaFar = 0.34f,
                    residualAlphaNear = 0.68f,
                    residualAlphaFar = 0.50f,
                    maxCoverage = 0.98f,
                    clarityNear = 0.34f,
                    clarityFar = 0.26f
                )
            focal in COLOR_CARD_CAMERA_SR_50_MIN_FOCAL..COLOR_CARD_CAMERA_SR_50_MAX_FOCAL ->
                ColorCardCameraSrProfile(
                    minBoost = 1.56f,
                    maxBoost = 1.86f,
                    maxPreparedSide = 448,
                    maxTileCount = 12,
                    blendAlphaNear = 0.54f,
                    blendAlphaFar = 0.40f,
                    residualAlphaNear = 0.78f,
                    residualAlphaFar = 0.58f,
                    maxCoverage = 1.01f,
                    clarityNear = 0.46f,
                    clarityFar = 0.34f
                )
            else -> null
        }
    }

    private fun estimateTileCount(
        width: Int,
        height: Int
    ): Int {
        val xTiles = ceil(width / COLOR_CARD_CAMERA_SR_TILE_STRIDE.toFloat()).toInt().coerceAtLeast(1)
        val yTiles = ceil(height / COLOR_CARD_CAMERA_SR_TILE_STRIDE.toFloat()).toInt().coerceAtLeast(1)
        return xTiles * yTiles
    }

    private fun mapModelFile(context: Context, assetName: String): MappedByteBuffer? {
        return runCatching {
            context.assets.openFd(assetName).use { afd ->
                FileInputStream(afd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
                }
            }
        }.getOrElse {
            Log.w(COLOR_CARD_CAMERA_SR_LOG_TAG, "Unable to map model asset $assetName", it)
            null
        }
    }

    private fun bitmapToPixelBuffer(bitmap: Bitmap): ColorCardCameraPixelBuffer {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return ColorCardCameraPixelBuffer(bitmap.width, bitmap.height, pixels)
    }

    private fun pixelBufferToBitmap(buffer: ColorCardCameraPixelBuffer): Bitmap {
        return Bitmap.createBitmap(buffer.pixels, buffer.width, buffer.height, Bitmap.Config.ARGB_8888)
    }

    private fun resamplePixelBuffer(
        source: ColorCardCameraPixelBuffer,
        targetWidth: Int,
        targetHeight: Int
    ): ColorCardCameraPixelBuffer {
        if (source.width == targetWidth && source.height == targetHeight) return source
        val output = IntArray(targetWidth * targetHeight)
        val xScale = source.width.toFloat() / targetWidth.toFloat()
        val yScale = source.height.toFloat() / targetHeight.toFloat()
        for (y in 0 until targetHeight) {
            val srcY = ((y + 0.5f) * yScale - 0.5f).coerceIn(0f, (source.height - 1).toFloat())
            val y0 = srcY.toInt()
            val y1 = (y0 + 1).coerceAtMost(source.height - 1)
            val fy = srcY - y0
            val row0 = y0 * source.width
            val row1 = y1 * source.width
            for (x in 0 until targetWidth) {
                val srcX = ((x + 0.5f) * xScale - 0.5f).coerceIn(0f, (source.width - 1).toFloat())
                val x0 = srcX.toInt()
                val x1 = (x0 + 1).coerceAtMost(source.width - 1)
                val fx = srcX - x0
                val c00 = source.pixels[row0 + x0]
                val c10 = source.pixels[row0 + x1]
                val c01 = source.pixels[row1 + x0]
                val c11 = source.pixels[row1 + x1]
                output[y * targetWidth + x] = bilinearInterpolateColor(c00, c10, c01, c11, fx, fy)
            }
        }
        return ColorCardCameraPixelBuffer(targetWidth, targetHeight, output)
    }

    private fun bilinearInterpolateColor(
        c00: Int,
        c10: Int,
        c01: Int,
        c11: Int,
        fx: Float,
        fy: Float
    ): Int {
        val w00 = (1f - fx) * (1f - fy)
        val w10 = fx * (1f - fy)
        val w01 = (1f - fx) * fy
        val w11 = fx * fy
        val r = (((c00 shr 16) and 0xFF) * w00 + ((c10 shr 16) and 0xFF) * w10 +
            ((c01 shr 16) and 0xFF) * w01 + ((c11 shr 16) and 0xFF) * w11).roundToInt()
        val g = (((c00 shr 8) and 0xFF) * w00 + ((c10 shr 8) and 0xFF) * w10 +
            ((c01 shr 8) and 0xFF) * w01 + ((c11 shr 8) and 0xFF) * w11).roundToInt()
        val b = ((c00 and 0xFF) * w00 + (c10 and 0xFF) * w10 +
            (c01 and 0xFF) * w01 + (c11 and 0xFF) * w11).roundToInt()
        return (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    private fun upscaleWithTiles(
        interpreter: Interpreter,
        source: ColorCardCameraPixelBuffer
    ): ColorCardCameraPixelBuffer {
        val sourceWidth = source.width
        val sourceHeight = source.height
        val stride = COLOR_CARD_CAMERA_SR_TILE_STRIDE
        val outputWidth = sourceWidth * COLOR_CARD_CAMERA_SR_MODEL_SCALE
        val outputHeight = sourceHeight * COLOR_CARD_CAMERA_SR_MODEL_SCALE
        val outputPixels = IntArray(outputWidth * outputHeight)

        val xTiles = ceil(sourceWidth / stride.toFloat()).toInt().coerceAtLeast(1)
        val yTiles = ceil(sourceHeight / stride.toFloat()).toInt().coerceAtLeast(1)
        val inputBuffer = ByteBuffer.allocateDirect(
            COLOR_CARD_CAMERA_SR_INPUT_SIZE * COLOR_CARD_CAMERA_SR_INPUT_SIZE * 3 * 4
        ).order(ByteOrder.nativeOrder())
        val outputBuffer = ByteBuffer.allocateDirect(
            COLOR_CARD_CAMERA_SR_INPUT_SIZE * COLOR_CARD_CAMERA_SR_MODEL_SCALE *
                COLOR_CARD_CAMERA_SR_INPUT_SIZE * COLOR_CARD_CAMERA_SR_MODEL_SCALE * 3 * 4
        ).order(ByteOrder.nativeOrder())

        for (tileY in 0 until yTiles) {
            val contentTop = tileY * stride
            val actualContentTop = min(contentTop, max(sourceHeight - stride, 0))
            val actualContentHeight = min(stride, sourceHeight - actualContentTop)
            for (tileX in 0 until xTiles) {
                val contentLeft = tileX * stride
                val actualContentLeft = min(contentLeft, max(sourceWidth - stride, 0))
                val actualContentWidth = min(stride, sourceWidth - actualContentLeft)
                fillInputTile(
                    source = source,
                    contentLeft = actualContentLeft,
                    contentTop = actualContentTop,
                    inputBuffer = inputBuffer
                )
                outputBuffer.rewind()
                interpreter.run(inputBuffer, outputBuffer)
                outputBuffer.rewind()
                writeOutputTile(
                    outputBuffer = outputBuffer,
                    outputPixels = outputPixels,
                    outputWidth = outputWidth,
                    contentLeft = actualContentLeft,
                    contentTop = actualContentTop,
                    contentWidth = actualContentWidth,
                    contentHeight = actualContentHeight
                )
            }
        }

        return ColorCardCameraPixelBuffer(outputWidth, outputHeight, outputPixels)
    }

    private fun fillInputTile(
        source: ColorCardCameraPixelBuffer,
        contentLeft: Int,
        contentTop: Int,
        inputBuffer: ByteBuffer
    ) {
        inputBuffer.rewind()
        val tileStartX = contentLeft - COLOR_CARD_CAMERA_SR_TILE_OVERLAP
        val tileStartY = contentTop - COLOR_CARD_CAMERA_SR_TILE_OVERLAP
        for (y in 0 until COLOR_CARD_CAMERA_SR_INPUT_SIZE) {
            val srcY = (tileStartY + y).coerceIn(0, source.height - 1)
            val rowBase = srcY * source.width
            for (x in 0 until COLOR_CARD_CAMERA_SR_INPUT_SIZE) {
                val srcX = (tileStartX + x).coerceIn(0, source.width - 1)
                val pixel = source.pixels[rowBase + srcX]
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                inputBuffer.putFloat((pixel and 0xFF) / 255f)
            }
        }
        inputBuffer.rewind()
    }

    private fun writeOutputTile(
        outputBuffer: ByteBuffer,
        outputPixels: IntArray,
        outputWidth: Int,
        contentLeft: Int,
        contentTop: Int,
        contentWidth: Int,
        contentHeight: Int
    ) {
        val tileOutputSize = COLOR_CARD_CAMERA_SR_INPUT_SIZE * COLOR_CARD_CAMERA_SR_MODEL_SCALE
        val overlapOutput = COLOR_CARD_CAMERA_SR_TILE_OVERLAP * COLOR_CARD_CAMERA_SR_MODEL_SCALE
        for (y in 0 until tileOutputSize) {
            for (x in 0 until tileOutputSize) {
                val r = (outputBuffer.float * 255f).roundToInt().coerceIn(0, 255)
                val g = (outputBuffer.float * 255f).roundToInt().coerceIn(0, 255)
                val b = (outputBuffer.float * 255f).roundToInt().coerceIn(0, 255)
                val keepX = x in overlapOutput until (overlapOutput + contentWidth * COLOR_CARD_CAMERA_SR_MODEL_SCALE)
                val keepY = y in overlapOutput until (overlapOutput + contentHeight * COLOR_CARD_CAMERA_SR_MODEL_SCALE)
                if (!keepX || !keepY) continue
                val destX = (contentLeft * COLOR_CARD_CAMERA_SR_MODEL_SCALE) + (x - overlapOutput)
                val destY = (contentTop * COLOR_CARD_CAMERA_SR_MODEL_SCALE) + (y - overlapOutput)
                val destIndex = (destY * outputWidth) + destX
                if (destIndex in outputPixels.indices) {
                    outputPixels[destIndex] =
                        (0xFF shl 24) or
                            (r shl 16) or
                            (g shl 8) or
                            b
                }
            }
        }
    }

    private fun blendPixelBuffersInPlace(
        base: ColorCardCameraPixelBuffer,
        detail: ColorCardCameraPixelBuffer,
        alpha: Float
    ): ColorCardCameraPixelBuffer {
        if (base.width != detail.width || base.height != detail.height) return base
        for (index in base.pixels.indices) {
            val basePixel = base.pixels[index]
            val detailPixel = detail.pixels[index]
            val baseR = (basePixel shr 16) and 0xFF
            val baseG = (basePixel shr 8) and 0xFF
            val baseB = basePixel and 0xFF
            val detailR = (detailPixel shr 16) and 0xFF
            val detailG = (detailPixel shr 8) and 0xFF
            val detailB = detailPixel and 0xFF
            val mixedR = (baseR + ((detailR - baseR) * alpha)).roundToInt().coerceIn(0, 255)
            val mixedG = (baseG + ((detailG - baseG) * alpha)).roundToInt().coerceIn(0, 255)
            val mixedB = (baseB + ((detailB - baseB) * alpha)).roundToInt().coerceIn(0, 255)
            base.pixels[index] = (0xFF shl 24) or (mixedR shl 16) or (mixedG shl 8) or mixedB
        }
        return base
    }

    private fun applyAdaptiveDenoise(
        source: ColorCardCameraPixelBuffer,
        coverage: Float
    ): ColorCardCameraPixelBuffer {
        if (source.width < 3 || source.height < 3) return source
        val output = IntArray(source.pixels.size)
        val denoiseTier = when {
            coverage <= 0.42f -> 2
            coverage <= 0.62f -> 1
            else -> 0
        }

        fun red(pixel: Int) = (pixel shr 16) and 0xFF
        fun green(pixel: Int) = (pixel shr 8) and 0xFF
        fun blue(pixel: Int) = pixel and 0xFF
        fun luma(pixel: Int): Int = (red(pixel) * 77 + green(pixel) * 150 + blue(pixel) * 29) shr 8

        for (y in 0 until source.height) {
            val yUp = (y - 1).coerceAtLeast(0)
            val yDown = (y + 1).coerceAtMost(source.height - 1)
            for (x in 0 until source.width) {
                val xLeft = (x - 1).coerceAtLeast(0)
                val xRight = (x + 1).coerceAtMost(source.width - 1)
                val centerIndex = y * source.width + x
                val center = source.pixels[centerIndex]
                val centerLuma = luma(center)
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var weightSum = 0
                for (sampleY in yUp..yDown) {
                    val rowBase = sampleY * source.width
                    for (sampleX in xLeft..xRight) {
                        val sample = source.pixels[rowBase + sampleX]
                        val diff = kotlin.math.abs(luma(sample) - centerLuma)
                        val weight = when {
                            diff <= 6 -> if (denoiseTier >= 2) 6 else 5
                            diff <= 12 -> if (denoiseTier >= 1) 4 else 3
                            diff <= 20 -> if (denoiseTier >= 2) 2 else 1
                            diff <= 28 && denoiseTier >= 2 -> 1
                            else -> 0
                        }
                        if (weight <= 0) continue
                        sumR += red(sample) * weight
                        sumG += green(sample) * weight
                        sumB += blue(sample) * weight
                        weightSum += weight
                    }
                }
                if (weightSum == 0) {
                    output[centerIndex] = center
                    continue
                }
                val denoisedR = sumR / weightSum
                val denoisedG = sumG / weightSum
                val denoisedB = sumB / weightSum
                output[centerIndex] =
                    (0xFF shl 24) or
                        (denoisedR.coerceIn(0, 255) shl 16) or
                        (denoisedG.coerceIn(0, 255) shl 8) or
                        denoisedB.coerceIn(0, 255)
            }
        }
        return ColorCardCameraPixelBuffer(source.width, source.height, output)
    }

    private fun mergeSuperResolutionResidualInPlace(
        base: ColorCardCameraPixelBuffer,
        model: ColorCardCameraPixelBuffer,
        reference: ColorCardCameraPixelBuffer,
        alpha: Float
    ): ColorCardCameraPixelBuffer {
        if (
            base.width != model.width ||
            base.height != model.height ||
            base.width != reference.width ||
            base.height != reference.height
        ) {
            return base
        }
        for (index in model.pixels.indices) {
            val basePixel = base.pixels[index]
            val modelPixel = model.pixels[index]
            val referencePixel = reference.pixels[index]
            val baseR = (basePixel shr 16) and 0xFF
            val baseG = (basePixel shr 8) and 0xFF
            val baseB = basePixel and 0xFF
            val modelR = (modelPixel shr 16) and 0xFF
            val modelG = (modelPixel shr 8) and 0xFF
            val modelB = modelPixel and 0xFF
            val referenceR = (referencePixel shr 16) and 0xFF
            val referenceG = (referencePixel shr 8) and 0xFF
            val referenceB = referencePixel and 0xFF
            val residualR = modelR - referenceR
            val residualG = modelG - referenceG
            val residualB = modelB - referenceB
            val residualMagnitude = maxOf(kotlin.math.abs(residualR), kotlin.math.abs(residualG), kotlin.math.abs(residualB))
            val adaptiveAlpha = when {
                residualMagnitude < 4 -> alpha * 0.75f
                residualMagnitude < 9 -> alpha * 0.9f
                else -> alpha
            }
            val mixedR = (baseR + (residualR * adaptiveAlpha)).roundToInt().coerceIn(0, 255)
            val mixedG = (baseG + (residualG * adaptiveAlpha)).roundToInt().coerceIn(0, 255)
            val mixedB = (baseB + (residualB * adaptiveAlpha)).roundToInt().coerceIn(0, 255)
            model.pixels[index] = (0xFF shl 24) or (mixedR shl 16) or (mixedG shl 8) or mixedB
        }
        return model
    }

    private fun applyClarityBoost(
        source: ColorCardCameraPixelBuffer,
        amount: Float
    ): ColorCardCameraPixelBuffer {
        if (amount <= 0.0001f || source.width < 3 || source.height < 3) return source
        val output = IntArray(source.pixels.size)

        fun red(pixel: Int) = (pixel shr 16) and 0xFF
        fun green(pixel: Int) = (pixel shr 8) and 0xFF
        fun blue(pixel: Int) = pixel and 0xFF

        for (y in 0 until source.height) {
            val yUp = (y - 1).coerceAtLeast(0)
            val yDown = (y + 1).coerceAtMost(source.height - 1)
            for (x in 0 until source.width) {
                val xLeft = (x - 1).coerceAtLeast(0)
                val xRight = (x + 1).coerceAtMost(source.width - 1)
                val centerIndex = y * source.width + x
                val center = source.pixels[centerIndex]

                var blurR = 0
                var blurG = 0
                var blurB = 0
                var weight = 0
                for (sampleY in yUp..yDown) {
                    val rowBase = sampleY * source.width
                    for (sampleX in xLeft..xRight) {
                        val sample = source.pixels[rowBase + sampleX]
                        blurR += red(sample)
                        blurG += green(sample)
                        blurB += blue(sample)
                        weight += 1
                    }
                }
                val avgR = blurR / weight.coerceAtLeast(1)
                val avgG = blurG / weight.coerceAtLeast(1)
                val avgB = blurB / weight.coerceAtLeast(1)
                val centerR = red(center)
                val centerG = green(center)
                val centerB = blue(center)
                val centerLuma = (centerR * 77 + centerG * 150 + centerB * 29) shr 8
                val avgLuma = (avgR * 77 + avgG * 150 + avgB * 29) shr 8
                val localContrast = kotlin.math.abs(centerLuma - avgLuma)
                val adaptiveAmount = when {
                    localContrast >= 18 -> amount * 1.24f
                    localContrast >= 10 -> amount
                    localContrast >= 5 -> amount * 0.72f
                    else -> amount * 0.42f
                }
                val boostedR = (centerR + ((centerR - avgR) * adaptiveAmount)).roundToInt().coerceIn(0, 255)
                val boostedG = (centerG + ((centerG - avgG) * adaptiveAmount)).roundToInt().coerceIn(0, 255)
                val boostedB = (centerB + ((centerB - avgB) * adaptiveAmount)).roundToInt().coerceIn(0, 255)
                output[centerIndex] =
                    (0xFF shl 24) or
                        (boostedR shl 16) or
                        (boostedG shl 8) or
                        boostedB
            }
        }
        return ColorCardCameraPixelBuffer(source.width, source.height, output)
    }
}
