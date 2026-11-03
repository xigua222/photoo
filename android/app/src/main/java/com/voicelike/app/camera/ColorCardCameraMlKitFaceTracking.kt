package com.voicelike.app

import android.graphics.Bitmap
import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val COLOR_CARD_CAMERA_FACE_TRACKING_LOG_TAG = "ColorCardFaceTracking"

internal class ColorCardCameraFaceTrackingPipeline(
    context: Context,
    private val config: ColorCardCameraFaceTrackingConfig = ColorCardCameraFaceTrackingConfig(),
    private val onFrame: (ColorCardCameraTrackedFacesFrame) -> Unit
) : ImageAnalysis.Analyzer, Closeable {
    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private val stabilizer = ColorCardCameraFaceTrackingStabilizer(config)
    private val closed = AtomicBoolean(false)
    private val enabled = AtomicBoolean(true)
    private val inFlight = AtomicBoolean(false)
    private val mirrorHorizontally = AtomicBoolean(config.mirrorHorizontally)
    private val requestLock = Any()
    private var activeImageProxy: ImageProxy? = null
    private var activeSourceWidth = 0
    private var activeSourceHeight = 0
    private var activeTimestampMs = 0L
    private var lastSubmittedTimestampMs = 0L

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(
            if (config.preferAccurateDetector) {
                FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
            } else {
                FaceDetectorOptions.PERFORMANCE_MODE_FAST
            }
        )
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(config.minFaceSize.coerceIn(0.03f, 0.4f))
        .apply {
            if (config.mlKitTrackingEnabled) {
                enableTracking()
            }
        }
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    override fun analyze(image: ImageProxy) {
        if (closed.get() || !enabled.get()) {
            image.close()
            return
        }
        val mediaImage = image.image ?: run {
            image.close()
            return
        }
        val timestampMs = TimeUnit.NANOSECONDS.toMillis(image.imageInfo.timestamp)
        val rotationDegrees = image.imageInfo.rotationDegrees
        val orientedSourceWidth = if (rotationDegrees % 180 == 0) image.width else image.height
        val orientedSourceHeight = if (rotationDegrees % 180 == 0) image.height else image.width
        if (!beginSubmission(
                timestampMs = timestampMs,
                sourceWidth = orientedSourceWidth,
                sourceHeight = orientedSourceHeight,
                imageProxy = image
            )
        ) {
            image.close()
            return
        }
        processInputImage(
            inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        )
    }

    fun submitBitmap(
        bitmap: Bitmap,
        timestampMs: Long
    ) {
        if (closed.get() || !enabled.get() || bitmap.isRecycled) {
            return
        }
        if (!beginSubmission(
                timestampMs = timestampMs,
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                imageProxy = null
            )
        ) {
            return
        }
        processInputImage(
            inputImage = InputImage.fromBitmap(bitmap, 0)
        )
    }

    fun setEnabled(value: Boolean) {
        enabled.set(value)
        if (!value) {
            reset()
        }
    }

    fun setMirrorHorizontally(value: Boolean) {
        mirrorHorizontally.set(value)
    }

    fun reset() {
        releaseActiveInputs()
        inFlight.set(false)
        dispatchFrame(stabilizer.reset())
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        reset()
        detector.close()
    }

    private fun beginSubmission(
        timestampMs: Long,
        sourceWidth: Int,
        sourceHeight: Int,
        imageProxy: ImageProxy?
    ): Boolean {
        if (timestampMs - lastSubmittedTimestampMs < config.analysisIntervalMs) {
            return false
        }
        if (!inFlight.compareAndSet(false, true)) {
            return false
        }
        synchronized(requestLock) {
            activeImageProxy = imageProxy
            activeSourceWidth = sourceWidth
            activeSourceHeight = sourceHeight
            activeTimestampMs = timestampMs
            lastSubmittedTimestampMs = timestampMs
        }
        return true
    }

    private fun processInputImage(
        inputImage: InputImage
    ) {
        runCatching {
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val frame = mapDetectedFaces(faces)
                    releaseActiveInputs()
                    inFlight.set(false)
                    dispatchFrame(stabilizer.stabilize(frame))
                }
                .addOnFailureListener { throwable ->
                    releaseActiveInputs()
                    inFlight.set(false)
                    Log.e(COLOR_CARD_CAMERA_FACE_TRACKING_LOG_TAG, "ML Kit face tracking error", throwable)
                }
        }.onFailure { throwable ->
            releaseActiveInputs()
            inFlight.set(false)
            Log.e(
                COLOR_CARD_CAMERA_FACE_TRACKING_LOG_TAG,
                "ML Kit face tracking dispatch failed",
                throwable
            )
        }
    }

    private fun mapDetectedFaces(
        faces: List<Face>
    ): ColorCardCameraTrackedFacesFrame {
        val snapshot = synchronized(requestLock) {
            Triple(activeTimestampMs, activeSourceWidth, activeSourceHeight)
        }
        val sourceWidth = snapshot.second.coerceAtLeast(1)
        val sourceHeight = snapshot.third.coerceAtLeast(1)
        val trackedFaces = faces
            .sortedByDescending { detectedFace ->
                val box = detectedFace.boundingBox
                box.width().coerceAtLeast(1) * box.height().coerceAtLeast(1)
            }
            .take(config.maxFaces.coerceAtLeast(1))
            .map { detectedFace ->
                mapDetectedFace(
                    face = detectedFace,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight
                )
            }
        return ColorCardCameraTrackedFacesFrame(
            timestampMs = snapshot.first,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            faces = trackedFaces
        )
    }

    private fun mapDetectedFace(
        face: Face,
        sourceWidth: Int,
        sourceHeight: Int
    ): ColorCardCameraTrackedFace {
        val normalizedLandmarks = face.allContours
            .asSequence()
            .flatMap { contour -> contour.points.asSequence() }
            .map { point ->
                val normalizedX = (point.x / sourceWidth.toFloat()).coerceIn(0f, 1f)
                ColorCardCameraTrackedLandmark(
                    x = if (mirrorHorizontally.get()) 1f - normalizedX else normalizedX,
                    y = (point.y / sourceHeight.toFloat()).coerceIn(0f, 1f),
                    z = 0f
                )
            }
            .toList()
        val normalizedBounds = face.boundingBox.toNormalizedRect(sourceWidth, sourceHeight)
            .mirrorHorizontally(mirrorHorizontally.get())
            .takeIf { it.area > 0.0001f }
            ?: resolveFaceBounds(normalizedLandmarks)
        return ColorCardCameraTrackedFace(
            bounds = normalizedBounds,
            landmarks = normalizedLandmarks,
            trackingId = face.trackingId
        )
    }

    private fun dispatchFrame(frame: ColorCardCameraTrackedFacesFrame) {
        if (closed.get()) return
        mainExecutor.execute {
            if (!closed.get()) {
                onFrame(frame)
            }
        }
    }

    private fun releaseActiveInputs() {
        synchronized(requestLock) {
            activeImageProxy?.close()
            activeImageProxy = null
            activeSourceWidth = 0
            activeSourceHeight = 0
            activeTimestampMs = 0L
        }
    }
}

private fun Rect.toNormalizedRect(
    sourceWidth: Int,
    sourceHeight: Int
): ColorCardCameraNormalizedRect {
    val safeWidth = sourceWidth.coerceAtLeast(1).toFloat()
    val safeHeight = sourceHeight.coerceAtLeast(1).toFloat()
    return ColorCardCameraNormalizedRect(
        left = (left / safeWidth).coerceIn(0f, 1f),
        top = (top / safeHeight).coerceIn(0f, 1f),
        right = (right / safeWidth).coerceIn(0f, 1f),
        bottom = (bottom / safeHeight).coerceIn(0f, 1f)
    )
}

private fun ColorCardCameraNormalizedRect.mirrorHorizontally(
    enabled: Boolean
): ColorCardCameraNormalizedRect {
    if (!enabled) return this
    return ColorCardCameraNormalizedRect(
        left = (1f - right).coerceIn(0f, 1f),
        top = top,
        right = (1f - left).coerceIn(0f, 1f),
        bottom = bottom
    )
}

private fun resolveFaceBounds(
    landmarks: List<ColorCardCameraTrackedLandmark>
): ColorCardCameraNormalizedRect {
    if (landmarks.isEmpty()) return ColorCardCameraNormalizedRect.Empty
    var left = 1f
    var top = 1f
    var right = 0f
    var bottom = 0f
    landmarks.forEach { landmark ->
        left = minOf(left, landmark.x)
        top = minOf(top, landmark.y)
        right = maxOf(right, landmark.x)
        bottom = maxOf(bottom, landmark.y)
    }
    return ColorCardCameraNormalizedRect(
        left = left.coerceIn(0f, 1f),
        top = top.coerceIn(0f, 1f),
        right = right.coerceIn(0f, 1f),
        bottom = bottom.coerceIn(0f, 1f)
    )
}
