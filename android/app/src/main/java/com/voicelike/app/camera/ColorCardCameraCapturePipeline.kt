package com.voicelike.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

internal const val COLOR_CARD_CAMERA_FOLDER = "Pictures/Photoo/Camera"
private const val COLOR_CARD_CAMERA_RENDER_MAX_SIDE = 6144
private const val COLOR_CARD_CAMERA_RENDER_MAX_SIDE_BALANCED = 6144
private const val COLOR_CARD_CAMERA_RENDER_MAX_SIDE_COOL = 6144
private const val COLOR_CARD_CAMERA_CAPTURE_LOG_TAG = "ColorCardCameraCapture"

internal data class ColorCardCameraPendingRender(
    val id: Long,
    val rawFile: File?,
    val jpegFile: File,
    val burstJpegFiles: List<File>,
    val previewUri: String,
    val useFrontCamera: Boolean,
    val cropCoverage: Float,
    val enhancementCoverage: Float,
    val enhancementEnabled: Boolean,
    val requestedFocal35mmEq: Float?,
    val renderedFocal35mmEq: Float?,
    val renderAdjustments: ColorCardCameraRenderAdjustments
)

internal class ColorCardCameraRenderQueue(
    private val context: Context,
    private val onRenderQueued: (ColorCardCameraPendingRender) -> Unit,
    private val onRenderFinished: (Long, Uri?) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    fun enqueue(
        item: ColorCardCameraPendingRender,
        filterState: ColorCardCameraFilterUiState
    ) {
        onRenderQueued(item)
        scope.launch {
            var fusedSource: File? = null
            var rendered: Bitmap? = null
            var finalized: Bitmap? = null
            try {
                fusedSource = if (item.burstJpegFiles.size >= 2) {
                    val fusionOutput = createCameraTempFile(context, "burst_fused_", ".jpg")
                    runCatching {
                        fuseColorCardCameraBurstFrames(
                            context = context,
                            burstFiles = item.burstJpegFiles,
                            outputFile = fusionOutput
                        )
                    }.onFailure {
                        fusionOutput.takeIf(File::exists)?.delete()
                    }.getOrNull()
                } else {
                    null
                }
                val preferredSource = fusedSource ?: item.jpegFile
                rendered = runCatching {
                    renderColorCardCameraFastBitmap(
                        context = context,
                        sourceFile = preferredSource,
                        filterState = filterState,
                        renderAdjustments = item.renderAdjustments,
                        maxSide = resolveColorCardCameraRenderMaxSide()
                    )
                }.getOrNull()
                val finalCoverage = item.cropCoverage.coerceIn(0.2f, 1f)
                finalized = rendered?.let { bitmap ->
                    val targetWidth = bitmap.width
                    val targetHeight = bitmap.height
                    val shouldEnhance = item.enhancementEnabled && ColorCardCameraSuperResolution.shouldEnhance(
                        renderedFocal35mmEq = item.renderedFocal35mmEq,
                        coverage = item.enhancementCoverage
                    )
                    Log.i(
                        COLOR_CARD_CAMERA_CAPTURE_LOG_TAG,
                        "Render finalize focal=${item.renderedFocal35mmEq ?: -1f} cropCoverage=${item.cropCoverage} enhancementCoverage=${item.enhancementCoverage} source=${bitmap.width}x${bitmap.height} target=${targetWidth}x$targetHeight sr=$shouldEnhance"
                    )
                    val scaledOrEnhanced = if (shouldEnhance) {
                        val croppedBitmap = cropColorCardCameraBitmapRegion(
                            bitmap = bitmap,
                            coverage = finalCoverage
                        )
                        ColorCardCameraSuperResolution.upscaleCroppedBitmap(
                            context = context,
                            croppedBitmap = croppedBitmap,
                            renderedFocal35mmEq = item.renderedFocal35mmEq,
                            coverage = item.enhancementCoverage,
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
                    if (item.useFrontCamera) {
                        flipColorCardCameraBitmapHorizontally(scaledOrEnhanced)
                    } else {
                        scaledOrEnhanced
                    }
                }
                val outputUri = finalized?.let { bitmap ->
                    saveCameraRenderedBitmap(
                        context = context,
                        bitmap = bitmap,
                        id = item.id,
                        exifSourceFile = item.jpegFile,
                        renderedFocal35mmEq = item.renderedFocal35mmEq
                    )
                }
                onRenderFinished(item.id, outputUri)
            } catch (oom: OutOfMemoryError) {
                AppPerformanceGovernor.onCaughtOutOfMemory()
                Log.e(COLOR_CARD_CAMERA_CAPTURE_LOG_TAG, "Render queue OOM id=${item.id}", oom)
                onRenderFinished(item.id, null)
            } finally {
                if (rendered != null && rendered !== finalized && !rendered.isRecycled) {
                    rendered.recycle()
                }
                finalized?.takeIf { !it.isRecycled }?.recycle()
                fusedSource?.takeIf(File::exists)?.delete()
            }
        }
    }
}

private fun parseColorCardCameraExifRational(value: String?): Float? {
    val raw = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if ('/' !in raw) {
        return raw.toFloatOrNull()
    }
    val parts = raw.split('/')
    if (parts.size != 2) return null
    val numerator = parts[0].trim().toFloatOrNull() ?: return null
    val denominator = parts[1].trim().toFloatOrNull()?.takeIf { it != 0f } ?: return null
    return numerator / denominator
}

private fun resolveColorCardCameraRenderMaxSide(): Int {
    val baseMode = AppPerformanceGovernor.currentMode()
    val effectiveMode = if (AppPerformanceGovernor.isMemoryPressureHigh()) {
        when (baseMode) {
            AppPerformanceGovernor.PerfMode.NORMAL -> AppPerformanceGovernor.PerfMode.BALANCED
            AppPerformanceGovernor.PerfMode.BALANCED,
            AppPerformanceGovernor.PerfMode.COOL -> AppPerformanceGovernor.PerfMode.COOL
        }
    } else {
        baseMode
    }
    return when (effectiveMode) {
        AppPerformanceGovernor.PerfMode.NORMAL -> COLOR_CARD_CAMERA_RENDER_MAX_SIDE
        AppPerformanceGovernor.PerfMode.BALANCED -> COLOR_CARD_CAMERA_RENDER_MAX_SIDE_BALANCED
        AppPerformanceGovernor.PerfMode.COOL -> COLOR_CARD_CAMERA_RENDER_MAX_SIDE_COOL
    }
}

internal fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    if (image.format != ImageFormat.YUV_420_888) return null
    val decoded = yuv420888ToBitmap(image) ?: return null
    val rotationDegrees = image.imageInfo.rotationDegrees
    if (rotationDegrees == 0) {
        return decoded
    }
    val matrix = Matrix().apply {
        postRotate(rotationDegrees.toFloat())
    }
    val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (rotated !== decoded) {
        decoded.recycle()
    }
    return rotated
}

private fun yuv420888ToBitmap(image: ImageProxy): Bitmap? {
    val yPlane = image.planes.getOrNull(0) ?: return null
    val uPlane = image.planes.getOrNull(1) ?: return null
    val vPlane = image.planes.getOrNull(2) ?: return null
    val width = image.width
    val height = image.height
    val argb = IntArray(width * height)
    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    val uRowStride = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride
    val vRowStride = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride

    for (row in 0 until height) {
        val yRow = row * yRowStride
        val uvRow = (row / 2) * uRowStride
        val vvRow = (row / 2) * vRowStride
        for (col in 0 until width) {
            val y = yBuffer.get(yRow + col * yPixelStride).toInt() and 0xFF
            val u = (uBuffer.get(uvRow + (col / 2) * uPixelStride).toInt() and 0xFF) - 128
            val v = (vBuffer.get(vvRow + (col / 2) * vPixelStride).toInt() and 0xFF) - 128
            val r = (y + (1.370705f * v)).toInt().coerceIn(0, 255)
            val g = (y - (0.698001f * v) - (0.337633f * u)).toInt().coerceIn(0, 255)
            val b = (y + (1.732446f * u)).toInt().coerceIn(0, 255)
            argb[row * width + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
}

internal fun createCameraTempFile(
    context: Context,
    prefix: String,
    suffix: String
): File {
    val dir = File(context.cacheDir, "camera")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return File.createTempFile(prefix, suffix, dir)
}

internal fun buildCameraImageContentValues(
    displayName: String,
    relativePath: String = COLOR_CARD_CAMERA_FOLDER
): ContentValues {
    val nowSeconds = System.currentTimeMillis() / 1000L
    return ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        } else {
            @Suppress("DEPRECATION")
            put(
                MediaStore.Images.Media.DATA,
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    relativePath.removePrefix("Pictures/") + "/$displayName"
                ).absolutePath
            )
        }
    }
}

internal fun saveCameraRenderedBitmap(
    context: Context,
    bitmap: Bitmap,
    id: Long,
    exifSourceFile: File?,
    renderedFocal35mmEq: Float?,
    relativePath: String = COLOR_CARD_CAMERA_FOLDER
): Uri? {
    val displayName = buildColorCardCameraDisplayName(id)
    val resolver = context.contentResolver
    val uri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        buildCameraImageContentValues(displayName, relativePath)
    ) ?: return null
    return runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 98, output)
        }
        copyColorCardCameraExif(
            context = context,
            destinationUri = uri,
            sourceFile = exifSourceFile,
            bitmap = bitmap,
            renderedFocal35mmEq = renderedFocal35mmEq
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                },
                null,
                null
            )
        }
        uri
    }.getOrElse {
        resolver.delete(uri, null, null)
        null
    }
}

private fun buildColorCardCameraDisplayName(id: Long): String {
    val timestamp = SimpleDateFormat("yyMMddHHmm", Locale.US).format(Date())
    val suffix = (id % 10L).toString()
    return "PHOTOOCOLOR_${timestamp}${suffix}.jpg"
}

private fun copyColorCardCameraExif(
    context: Context,
    destinationUri: Uri,
    sourceFile: File?,
    bitmap: Bitmap,
    renderedFocal35mmEq: Float?
) {
    runCatching {
        val sourceExif = sourceFile
            ?.takeIf(File::exists)
            ?.let { ExifInterface(it.absolutePath) }
        context.contentResolver.openFileDescriptor(destinationUri, "rw")?.use { pfd ->
            val destinationExif = ExifInterface(pfd.fileDescriptor)
            COLOR_CARD_CAMERA_EXIF_TAGS.forEach { tag ->
                sourceExif?.getAttribute(tag)?.let { value ->
                    destinationExif.setAttribute(tag, value)
                }
            }
            destinationExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            destinationExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, bitmap.width.toString())
            destinationExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, bitmap.height.toString())
            destinationExif.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, bitmap.width.toString())
            destinationExif.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, bitmap.height.toString())
            applyColorCardCameraRenderedFocalExif(
                destinationExif = destinationExif,
                sourceExif = sourceExif,
                renderedFocal35mmEq = renderedFocal35mmEq
            )
            destinationExif.saveAttributes()
        }
    }
}

private fun applyColorCardCameraRenderedFocalExif(
    destinationExif: ExifInterface,
    sourceExif: ExifInterface?,
    renderedFocal35mmEq: Float?
) {
    val safeRenderedFocal35mmEq = renderedFocal35mmEq
        ?.takeIf { it.isFinite() && it > 0f }
        ?: return
    destinationExif.setAttribute(
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        safeRenderedFocal35mmEq.roundToInt().coerceAtLeast(1).toString()
    )
    val sourceFocalLength = parseColorCardCameraExifRational(
        sourceExif?.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
    )
    val sourceFocal35mmEq = sourceExif
        ?.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
        ?.takeIf { it > 0 }
        ?.toFloat()
    if (sourceFocalLength != null && sourceFocal35mmEq != null && sourceFocal35mmEq > 0f) {
        val scaledFocalLength = (sourceFocalLength * (safeRenderedFocal35mmEq / sourceFocal35mmEq))
            .coerceAtLeast(0.1f)
        destinationExif.setAttribute(
            ExifInterface.TAG_FOCAL_LENGTH,
            formatColorCardCameraExifRational(scaledFocalLength)
        )
    }
    destinationExif.setAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO, "1")
}

private fun formatColorCardCameraExifRational(value: Float): String {
    val safeValue = value.coerceAtLeast(0f)
    val denominator = 1000
    val numerator = (safeValue * denominator).roundToInt().coerceAtLeast(1)
    return "$numerator/$denominator"
}

private val COLOR_CARD_CAMERA_EXIF_TAGS = arrayOf(
    ExifInterface.TAG_APERTURE_VALUE,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_BITS_PER_SAMPLE,
    ExifInterface.TAG_BRIGHTNESS_VALUE,
    ExifInterface.TAG_CFA_PATTERN,
    ExifInterface.TAG_COLOR_SPACE,
    ExifInterface.TAG_COMPONENTS_CONFIGURATION,
    ExifInterface.TAG_COMPRESSION,
    ExifInterface.TAG_CONTRAST,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DEFAULT_CROP_SIZE,
    ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
    ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
    ExifInterface.TAG_EXIF_VERSION,
    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_EXPOSURE_INDEX,
    ExifInterface.TAG_EXPOSURE_MODE,
    ExifInterface.TAG_EXPOSURE_PROGRAM,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_FILE_SOURCE,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_FLASHPIX_VERSION,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
    ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
    ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
    ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_GAIN_CONTROL,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_AREA_INFORMATION,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_DEST_BEARING,
    ExifInterface.TAG_GPS_DEST_BEARING_REF,
    ExifInterface.TAG_GPS_DEST_DISTANCE,
    ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
    ExifInterface.TAG_GPS_DEST_LATITUDE,
    ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LONGITUDE,
    ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
    ExifInterface.TAG_GPS_DIFFERENTIAL,
    ExifInterface.TAG_GPS_DOP,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_MAP_DATUM,
    ExifInterface.TAG_GPS_MEASURE_MODE,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_SATELLITES,
    ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_SPEED_REF,
    ExifInterface.TAG_GPS_STATUS,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_TRACK,
    ExifInterface.TAG_GPS_TRACK_REF,
    ExifInterface.TAG_GPS_VERSION_ID,
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_ISO_SPEED_RATINGS,
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MAX_APERTURE_VALUE,
    ExifInterface.TAG_METERING_MODE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_OECF,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
    ExifInterface.TAG_SATURATION,
    ExifInterface.TAG_SCENE_CAPTURE_TYPE,
    ExifInterface.TAG_SCENE_TYPE,
    ExifInterface.TAG_SENSING_METHOD,
    ExifInterface.TAG_SHARPNESS,
    ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_SUBFILE_TYPE,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_X_RESOLUTION,
    ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
    ExifInterface.TAG_Y_CB_CR_POSITIONING,
    ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
    ExifInterface.TAG_Y_RESOLUTION
)

internal val colorCardCameraCaptureId = AtomicLong(1L)

internal fun colorCardCameraLensCoverage(
    lensMode: ColorCardCameraLensMode
): Float = when (lensMode) {
    ColorCardCameraLensMode.Native -> 1f
    ColorCardCameraLensMode.Eq35 -> 0.68f
    ColorCardCameraLensMode.Eq50 -> 0.48f
    ColorCardCameraLensMode.Tele1 -> 0.28f
    ColorCardCameraLensMode.Tele2 -> 0.20f
}

internal fun cropColorCardCameraBitmapRegion(
    bitmap: Bitmap,
    coverage: Float
): Bitmap {
    val safeCoverage = coverage.coerceIn(0.2f, 1f)
    if (safeCoverage >= 0.999f) return bitmap
    val cropWidth = (bitmap.width * safeCoverage).toInt().coerceIn(1, bitmap.width)
    val cropHeight = (bitmap.height * safeCoverage).toInt().coerceIn(1, bitmap.height)
    val left = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
    val top = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
    val cropped = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    if (cropped !== bitmap && !bitmap.isRecycled) {
        bitmap.recycle()
    }
    return cropped
}

internal fun scaleColorCardCameraBitmap(
    bitmap: Bitmap,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    if (bitmap.width == targetWidth && bitmap.height == targetHeight) return bitmap
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also { scaled ->
        if (scaled !== bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

internal fun transformColorCardCameraBitmapRegion(
    bitmap: Bitmap,
    coverage: Float,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    val safeCoverage = coverage.coerceIn(0.2f, 1f)
    if (safeCoverage >= 0.999f && bitmap.width == targetWidth && bitmap.height == targetHeight) {
        return bitmap
    }
    val cropWidth = (bitmap.width * safeCoverage).roundToInt().coerceIn(1, bitmap.width)
    val cropHeight = (bitmap.height * safeCoverage).roundToInt().coerceIn(1, bitmap.height)
    val left = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
    val top = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
    if (cropWidth == targetWidth && cropHeight == targetHeight) {
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight).also { transformed ->
            if (transformed !== bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
    val matrix = Matrix().apply {
        postScale(
            targetWidth.toFloat() / cropWidth.toFloat().coerceAtLeast(1f),
            targetHeight.toFloat() / cropHeight.toFloat().coerceAtLeast(1f)
        )
    }
    return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight, matrix, true).also { transformed ->
        if (transformed !== bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

internal fun flipColorCardCameraBitmapHorizontally(
    bitmap: Bitmap
): Bitmap {
    if (bitmap.isRecycled) {
        throw IllegalArgumentException("bitmap is recycled")
    }
    val matrix = Matrix().apply { postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f) }
    val flipped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (flipped === bitmap) {
        return flipped
    }
    if (!bitmap.isRecycled) {
        bitmap.recycle()
    }
    return flipped
}
