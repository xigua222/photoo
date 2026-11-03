package com.voicelike.app

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.LruCache
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class ElectronicAlbumSmartFrame(
    val horizontalBias: Float,
    val verticalBias: Float,
    val driftRangeX: Float,
    val driftRangeY: Float,
    val extraScale: Float
)

private sealed interface ElectronicAlbumSmartFrameCacheValue {
    data class Hit(val frame: ElectronicAlbumSmartFrame) : ElectronicAlbumSmartFrameCacheValue
    data object Miss : ElectronicAlbumSmartFrameCacheValue
}

object ElectronicAlbumSmartFraming {
    private const val DEFAULT_MAX_SIDE_PX = 960
    private const val LOW_RAM_MAX_SIDE_PX = 640

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.08f)
        .enableTracking()
        .build()

    private val detector by lazy { FaceDetection.getClient(detectorOptions) }
    private val cacheMutex = Mutex()
    private val inFlightMutex = Mutex()
    private var cache = LruCache<Long, ElectronicAlbumSmartFrameCacheValue>(192)
    private val inFlightIds = LinkedHashSet<Long>()

    fun peek(itemId: Long): ElectronicAlbumSmartFrame? = synchronized(this) {
        when (val value = cache.get(itemId)) {
            is ElectronicAlbumSmartFrameCacheValue.Hit -> value.frame
            ElectronicAlbumSmartFrameCacheValue.Miss, null -> null
        }
    }

    fun hasCached(itemId: Long): Boolean = synchronized(this) {
        cache.get(itemId) != null
    }

    suspend fun warmUp(context: Context, item: MediaItem) {
        if (item.type != "photo" || item.id <= 0L) return
        if (hasCached(item.id)) return
        if (!markInFlight(item.id)) return
        try {
            val result = analyzeInternal(context, item)
            cacheMutex.withLock {
                cache.put(
                    item.id,
                    result?.let { ElectronicAlbumSmartFrameCacheValue.Hit(it) }
                        ?: ElectronicAlbumSmartFrameCacheValue.Miss
                )
            }
        } finally {
            unmarkInFlight(item.id)
        }
    }

    private suspend fun markInFlight(itemId: Long): Boolean = inFlightMutex.withLock {
        if (inFlightIds.contains(itemId)) {
            false
        } else {
            inFlightIds.add(itemId)
            true
        }
    }

    private suspend fun unmarkInFlight(itemId: Long) = inFlightMutex.withLock {
        inFlightIds.remove(itemId)
    }

    private suspend fun analyzeInternal(context: Context, item: MediaItem): ElectronicAlbumSmartFrame? {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                bitmap = decodeBitmapForSmartFraming(context, item.uri) ?: return@withContext null
                val image = InputImage.fromBitmap(bitmap, 0)
                val faces = detector.process(image).await()
                buildFrame(bitmap.width, bitmap.height, faces)
            } catch (_: CancellationException) {
                null
            } catch (_: OutOfMemoryError) {
                null
            } catch (_: Exception) {
                null
            } finally {
                bitmap?.recycle()
            }
        }
    }

    private fun decodeBitmapForSmartFraming(context: Context, uri: android.net.Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        val maxSideLimit = if (isLowRamDevice(context) || MemorySafetyMode.isOomFallbackActive(context)) {
            LOW_RAM_MAX_SIDE_PX
        } else {
            DEFAULT_MAX_SIDE_PX
        }

        var inSampleSize = 1
        var maxSide = maxOf(width, height)
        while (maxSide > maxSideLimit) {
            inSampleSize *= 2
            maxSide /= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun buildFrame(bitmapWidth: Int, bitmapHeight: Int, faces: List<Face>): ElectronicAlbumSmartFrame? {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || faces.isEmpty()) return null

        var totalWeight = 0f
        var weightedCenterX = 0f
        var weightedCenterY = 0f
        var unionLeft = bitmapWidth.toFloat()
        var unionTop = bitmapHeight.toFloat()
        var unionRight = 0f
        var unionBottom = 0f

        faces.forEach { face ->
            val box: Rect = face.boundingBox
            val width = box.width().coerceAtLeast(1)
            val height = box.height().coerceAtLeast(1)
            val areaWeight = (width * height).toFloat()
            val centerX = box.exactCenterX()
            val centerY = box.exactCenterY()
            totalWeight += areaWeight
            weightedCenterX += centerX * areaWeight
            weightedCenterY += centerY * areaWeight
            unionLeft = minOf(unionLeft, box.left.toFloat())
            unionTop = minOf(unionTop, box.top.toFloat())
            unionRight = maxOf(unionRight, box.right.toFloat())
            unionBottom = maxOf(unionBottom, box.bottom.toFloat())
        }

        if (totalWeight <= 0f) return null

        val centerX = weightedCenterX / totalWeight
        val centerY = weightedCenterY / totalWeight
        val horizontalBias = (((centerX / bitmapWidth.toFloat()) - 0.5f) * 2f).coerceIn(-0.82f, 0.82f)
        val verticalBias = (((centerY / bitmapHeight.toFloat()) - 0.5f) * 2f).coerceIn(-0.72f, 0.72f)

        val subjectWidthRatio = ((unionRight - unionLeft) / bitmapWidth.toFloat()).coerceIn(0.08f, 1f)
        val subjectHeightRatio = ((unionBottom - unionTop) / bitmapHeight.toFloat()).coerceIn(0.08f, 1f)
        val driftRangeX = ((1f - subjectWidthRatio) * 0.18f).coerceIn(0f, 0.16f)
        val driftRangeY = ((1f - subjectHeightRatio) * 0.14f).coerceIn(0f, 0.12f)
        val extraScale = when {
            subjectWidthRatio < 0.22f && subjectHeightRatio < 0.22f -> 1.2f
            subjectWidthRatio < 0.35f && subjectHeightRatio < 0.35f -> 1.14f
            else -> 1.08f
        }

        return ElectronicAlbumSmartFrame(
            horizontalBias = horizontalBias,
            verticalBias = verticalBias,
            driftRangeX = driftRangeX,
            driftRangeY = driftRangeY,
            extraScale = extraScale
        )
    }

    private fun isLowRamDevice(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return manager?.isLowRamDevice == true
    }
}

private fun maxOf(a: Float, b: Float): Float = if (a >= b) a else b
