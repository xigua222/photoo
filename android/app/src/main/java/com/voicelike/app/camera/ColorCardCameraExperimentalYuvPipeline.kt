package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.ImageFormat
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService

internal const val COLOR_CARD_CAMERA_YUV_EXPERIMENTAL_FOLDER = "Pictures/Photoo/CameraYuvExperimental"
private val COLOR_CARD_CAMERA_YUV_PALETTE_TARGET_LIGHTNESS = listOf(0.94f, 0.80f, 0.65f, 0.48f, 0.28f, 0.10f)

internal enum class ColorCardCameraCapturePipelineMode(
    val label: String
) {
    LegacyJpeg("JPG"),
    ExperimentalYuv("YUV")
}

internal sealed interface ColorCardCameraCaptureResult {
    data class LegacyJpeg(
        val rawFile: File?,
        val jpegFile: File,
        val burstJpegFiles: List<File>
    ) : ColorCardCameraCaptureResult

    data class ExperimentalYuv(
        val yuvFrame: ColorCardCameraYuvFrame,
        val previewFile: File
    ) : ColorCardCameraCaptureResult
}

internal class ColorCardCameraExperimentalYuvRenderQueue(
    private val context: Context,
    private val onRenderQueued: (Long, String) -> Unit,
    private val onRenderFinished: (Long, android.net.Uri?) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    fun enqueue(
        id: Long,
        previewFile: File,
        yuvFrame: ColorCardCameraYuvFrame,
        useFrontCamera: Boolean,
        cropCoverage: Float,
        enhancementCoverage: Float,
        renderedFocal35mmEq: Float?,
        renderAdjustments: ColorCardCameraRenderAdjustments,
        filterState: ColorCardCameraFilterUiState
    ) {
        onRenderQueued(id, android.net.Uri.fromFile(previewFile).toString())
        scope.launch {
            try {
                val yuvPreparedBitmap = colorCardCameraYuvFrameToBitmap(
                    frame = yuvFrame,
                    colorAdjustment = (
                        filterState.color +
                            (renderAdjustments.whiteBalanceShift * 0.72f)
                        ).coerceIn(-1f, 1f),
                    colorAxis = filterState.renderColorAxis,
                    toneAdjustment = filterState.tone,
                    brightnessAdjustment = (
                        filterState.brightness +
                            (renderAdjustments.exposureCompensation * 0.40f)
                        ).coerceIn(-1f, 1f),
                    saturationAdjustment = filterState.saturation,
                    sharpenAdjustment = filterState.sharpen
                )
                val (preGlStyledBitmap, yuvPaletteNeutralizedState) = applyColorCardCameraYuvPaletteStage(
                    sourceBitmap = yuvPreparedBitmap,
                    filterState = filterState
                )
                val rendered = renderColorCardCameraFastBitmapFromBitmap(
                    context = context,
                    sourceBitmap = preGlStyledBitmap,
                    filterState = yuvPaletteNeutralizedState.copy(
                        color = 0f,
                        tone = 0f,
                        brightness = 0f,
                        saturation = 0f,
                        sharpen = 0f
                    ),
                    renderAdjustments = renderAdjustments.copy(
                        whiteBalanceShift = 0f,
                        exposureCompensation = 0f
                    )
                )
                val finalCoverage = cropCoverage.coerceIn(0.2f, 1f)
                val finalized = rendered?.let { bitmap ->
                    val targetWidth = bitmap.width
                    val targetHeight = bitmap.height
                    val scaledOrEnhanced = if (
                        ColorCardCameraSuperResolution.shouldEnhance(
                            renderedFocal35mmEq = renderedFocal35mmEq,
                            coverage = enhancementCoverage
                        )
                    ) {
                        val croppedBitmap = cropColorCardCameraBitmapRegion(
                            bitmap = bitmap,
                            coverage = finalCoverage
                        )
                        ColorCardCameraSuperResolution.upscaleCroppedBitmap(
                            context = context,
                            croppedBitmap = croppedBitmap,
                            renderedFocal35mmEq = renderedFocal35mmEq,
                            coverage = enhancementCoverage,
                            targetWidth = targetWidth,
                            targetHeight = targetHeight,
                            consumeSourceBitmap = true
                        ) ?: scaleColorCardCameraBitmap(croppedBitmap, targetWidth, targetHeight)
                    } else {
                        transformColorCardCameraBitmapRegion(
                            bitmap = bitmap,
                            coverage = finalCoverage,
                            targetWidth = targetWidth,
                            targetHeight = targetHeight
                        )
                    }
                    if (useFrontCamera) {
                        flipColorCardCameraBitmapHorizontally(scaledOrEnhanced)
                    } else {
                        scaledOrEnhanced
                    }
                }
                val uri = finalized?.let { bitmap ->
                    saveCameraRenderedBitmap(
                        context = context,
                        bitmap = bitmap,
                        id = id,
                        exifSourceFile = previewFile,
                        renderedFocal35mmEq = renderedFocal35mmEq,
                        relativePath = COLOR_CARD_CAMERA_YUV_EXPERIMENTAL_FOLDER
                    )
                }
                if (rendered != null && rendered !== finalized && !rendered.isRecycled) {
                    rendered.recycle()
                }
                if (finalized != null && !finalized.isRecycled) {
                    finalized.recycle()
                }
                onRenderFinished(id, uri)
            } catch (oom: OutOfMemoryError) {
                AppPerformanceGovernor.onCaughtOutOfMemory()
                onRenderFinished(id, null)
            } finally {
                previewFile.takeIf(File::exists)?.delete()
            }
        }
    }
}

private fun applyColorCardCameraYuvPaletteStage(
    sourceBitmap: Bitmap,
    filterState: ColorCardCameraFilterUiState
): Pair<Bitmap, ColorCardCameraFilterUiState> {
    val sourcePalette = extractColorCardCameraYuvSourcePalette(sourceBitmap)
    val styleTemplate = filterState.selectedStyleTemplate
    if (styleTemplate != null) {
        val remapped = renderColorCardTracePalettePreviewBitmap(
            sourceBitmap = sourceBitmap,
            sourceColors = sourcePalette,
            targetColors = styleTemplate.candidate.colors,
            color = 0f,
            colorAxis = styleTemplate.colorAxis,
            sharpen = 0f,
            tone = 0f,
            saturation = 0f,
            brightness = 0f,
            paletteStrength = filterState.strength
        )
        if (remapped !== sourceBitmap && !sourceBitmap.isRecycled) {
            sourceBitmap.recycle()
        }
        return remapped to filterState.copy(
            selectedStyleTemplate = null
        )
    }
    val selectedCandidate = filterState.renderSelectedCandidate
    if (
        selectedCandidate != null &&
        selectedCandidate.kind == ColorCardTraceFilterKind.HaldClut &&
        selectedCandidate.colors.isNotEmpty()
    ) {
        val remapped = renderColorCardTracePalettePreviewBitmap(
            sourceBitmap = sourceBitmap,
            sourceColors = sourcePalette,
            targetColors = selectedCandidate.colors,
            color = 0f,
            colorAxis = filterState.renderColorAxis,
            sharpen = 0f,
            tone = 0f,
            saturation = 0f,
            brightness = 0f,
            paletteStrength = (filterState.strength * 0.88f).coerceIn(0f, 1f)
        )
        if (remapped !== sourceBitmap && !sourceBitmap.isRecycled) {
            sourceBitmap.recycle()
        }
        return remapped to filterState.copy(
            selectedCandidate = selectedCandidate.copy(colors = emptyList()),
            renderSelectedCandidate = selectedCandidate.copy(colors = emptyList()),
            haldBitmap = null,
            renderHaldBitmap = null,
            haldSecondaryBitmap = null,
            renderHaldSecondaryBitmap = null,
            haldVariantMix = 0f
            ,
            renderHaldVariantMix = 0f
        )
    }
    return sourceBitmap to filterState
}

private data class ColorCardCameraYuvPaletteBucket(
    var weight: Float = 0f,
    var redSum: Float = 0f,
    var greenSum: Float = 0f,
    var blueSum: Float = 0f,
    var lightnessSum: Float = 0f
) {
    fun add(colorInt: Int, weight: Float, lightness: Float) {
        this.weight += weight
        redSum += AndroidColor.red(colorInt) * weight
        greenSum += AndroidColor.green(colorInt) * weight
        blueSum += AndroidColor.blue(colorInt) * weight
        lightnessSum += lightness * weight
    }

    fun averageColor(): Int {
        val safeWeight = weight.coerceAtLeast(0.0001f)
        return AndroidColor.rgb(
            (redSum / safeWeight).toInt().coerceIn(0, 255),
            (greenSum / safeWeight).toInt().coerceIn(0, 255),
            (blueSum / safeWeight).toInt().coerceIn(0, 255)
        )
    }

    fun averageLightness(): Float {
        return lightnessSum / weight.coerceAtLeast(0.0001f)
    }
}

private fun extractColorCardCameraYuvSourcePalette(bitmap: Bitmap): List<Int> {
    val width = bitmap.width.coerceAtLeast(1)
    val height = bitmap.height.coerceAtLeast(1)
    val stepX = (width / 72).coerceAtLeast(1)
    val stepY = (height / 72).coerceAtLeast(1)
    val buckets = linkedMapOf<Int, ColorCardCameraYuvPaletteBucket>()
    var averageRed = 0f
    var averageGreen = 0f
    var averageBlue = 0f
    var averageLightness = 0f
    var totalWeight = 0f

    for (y in 0 until height step stepY) {
        for (x in 0 until width step stepX) {
            val colorInt = bitmap.getPixel(x, y)
            val alpha = AndroidColor.alpha(colorInt)
            if (alpha < 18) continue
            val red = AndroidColor.red(colorInt)
            val green = AndroidColor.green(colorInt)
            val blue = AndroidColor.blue(colorInt)
            val hsv = FloatArray(3)
            AndroidColor.RGBToHSV(red, green, blue, hsv)
            val lightness = ((red + green + blue) / (255f * 3f)).coerceIn(0f, 1f)
            val saturation = hsv[1].coerceIn(0f, 1f)
            val weight = (
                0.52f +
                    (saturation * 0.78f) +
                    ((1f - kotlin.math.abs(lightness - 0.52f)) * 0.34f)
                ).coerceAtLeast(0.08f)
            averageRed += red * weight
            averageGreen += green * weight
            averageBlue += blue * weight
            averageLightness += lightness * weight
            totalWeight += weight
            val quantized = quantizeColorCardCameraYuvPaletteColor(colorInt)
            buckets.getOrPut(quantized) { ColorCardCameraYuvPaletteBucket() }
                .add(colorInt = colorInt, weight = weight, lightness = lightness)
        }
    }

    if (buckets.isEmpty()) {
        return listOf(
            0xFFF5F4F1.toInt(),
            0xFFDED9D2.toInt(),
            0xFFBBB4AE.toInt(),
            0xFF8C8784.toInt(),
            0xFF585658.toInt(),
            0xFF1D1F23.toInt()
        )
    }

    val averageColor = AndroidColor.rgb(
        (averageRed / totalWeight.coerceAtLeast(0.0001f)).toInt().coerceIn(0, 255),
        (averageGreen / totalWeight.coerceAtLeast(0.0001f)).toInt().coerceIn(0, 255),
        (averageBlue / totalWeight.coerceAtLeast(0.0001f)).toInt().coerceIn(0, 255)
    )
    val rankedBuckets = buckets.values
        .filter { it.weight > 0.20f }
        .sortedByDescending { it.weight }

    val palette = buildList {
        COLOR_CARD_CAMERA_YUV_PALETTE_TARGET_LIGHTNESS.forEach { targetLightness ->
            val bestBucket = rankedBuckets.maxByOrNull { bucket ->
                val weightScore = bucket.weight * (0.78f + bucket.averageLightness())
                val lightnessPenalty = kotlin.math.abs(bucket.averageLightness() - targetLightness) * 1.65f
                val neutralityBonus = resolveColorCardCameraYuvPaletteChromaBonus(bucket.averageColor())
                weightScore + neutralityBonus - lightnessPenalty
            }
            val candidate = bestBucket?.averageColor()
                ?: deriveColorCardCameraYuvFallbackTone(averageColor, targetLightness)
            if (none { existing -> colorCardCameraYuvPaletteDistance(existing, candidate) < 26f }) {
                add(candidate)
            } else {
                val fallback = deriveColorCardCameraYuvFallbackTone(averageColor, targetLightness)
                if (none { existing -> colorCardCameraYuvPaletteDistance(existing, fallback) < 18f }) {
                    add(fallback)
                }
            }
        }
    }.toMutableList()

    while (palette.size < COLOR_CARD_CAMERA_YUV_PALETTE_TARGET_LIGHTNESS.size) {
        val targetLightness = COLOR_CARD_CAMERA_YUV_PALETTE_TARGET_LIGHTNESS[palette.size]
        palette += deriveColorCardCameraYuvFallbackTone(averageColor, targetLightness)
    }
    return palette.take(COLOR_CARD_CAMERA_YUV_PALETTE_TARGET_LIGHTNESS.size)
}

private fun quantizeColorCardCameraYuvPaletteColor(colorInt: Int): Int {
    fun quantize(channel: Int): Int {
        val step = 24
        return ((channel / step) * step + (step / 2)).coerceIn(0, 255)
    }
    return AndroidColor.rgb(
        quantize(AndroidColor.red(colorInt)),
        quantize(AndroidColor.green(colorInt)),
        quantize(AndroidColor.blue(colorInt))
    )
}

private fun deriveColorCardCameraYuvFallbackTone(
    baseColor: Int,
    targetLightness: Float
): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(baseColor, hsv)
    val tunedSaturation = (hsv[1] * 0.72f).coerceIn(0.06f, 0.82f)
    return AndroidColor.HSVToColor(
        floatArrayOf(
            hsv[0],
            tunedSaturation,
            targetLightness.coerceIn(0.06f, 0.98f)
        )
    )
}

private fun resolveColorCardCameraYuvPaletteChromaBonus(colorInt: Int): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(colorInt, hsv)
    return when {
        hsv[1] <= 0.08f -> 0.04f
        hsv[1] >= 0.34f -> 0.12f
        else -> 0.04f + ((hsv[1] - 0.08f) / 0.26f) * 0.08f
    }
}

private fun colorCardCameraYuvPaletteDistance(first: Int, second: Int): Float {
    val deltaRed = (AndroidColor.red(first) - AndroidColor.red(second)).toFloat()
    val deltaGreen = (AndroidColor.green(first) - AndroidColor.green(second)).toFloat()
    val deltaBlue = (AndroidColor.blue(first) - AndroidColor.blue(second)).toFloat()
    return kotlin.math.sqrt(
        (deltaRed * deltaRed) +
            (deltaGreen * deltaGreen) +
            (deltaBlue * deltaBlue)
    )
}

internal fun configureColorCardCameraExperimentalYuvCapture(
    builder: ImageCapture.Builder,
    enabled: Boolean
) {
    if (enabled) {
        builder.setBufferFormat(ImageFormat.YUV_420_888)
    }
}

internal fun takeColorCardCameraExperimentalYuvPicture(
    context: Context,
    imageCapture: ImageCapture,
    camera: Camera?,
    useFrontCamera: Boolean,
    flashEnabled: Boolean,
    executor: ExecutorService,
    onComplete: (ColorCardCameraCaptureResult.ExperimentalYuv) -> Unit,
    onError: () -> Unit
) {
    val useTorchAssist = flashEnabled &&
        !useFrontCamera &&
        runCatching { camera?.cameraInfo?.hasFlashUnit() == true }.getOrDefault(false)
    if (useTorchAssist) {
        runCatching {
            camera?.cameraControl?.enableTorch(true)
            Thread.sleep(140L)
        }
    }
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = imageProxyToBitmap(image) ?: run {
                        onError()
                        return
                    }
                    val yuvFrame = copyColorCardCameraYuvFrame(image) ?: run {
                        bitmap.takeIf { !it.isRecycled }?.recycle()
                        onError()
                        return
                    }
                    val previewFile = createCameraTempFile(
                        context = context,
                        prefix = "yuv_capture_",
                        suffix = ".jpg"
                    )
                    persistColorCardCameraExperimentalPreviewBitmap(bitmap, previewFile)
                    onComplete(
                        ColorCardCameraCaptureResult.ExperimentalYuv(
                            yuvFrame = yuvFrame,
                            previewFile = previewFile
                        )
                    )
                } finally {
                    image.close()
                    if (useTorchAssist) {
                        runCatching { camera?.cameraControl?.enableTorch(false) }
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                if (useTorchAssist) {
                    runCatching { camera?.cameraControl?.enableTorch(false) }
                }
                onError()
            }
        }
    )
}

private fun persistColorCardCameraExperimentalPreviewBitmap(
    bitmap: Bitmap,
    destination: File
) {
    FileOutputStream(destination).use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 98, output)
    }
}
