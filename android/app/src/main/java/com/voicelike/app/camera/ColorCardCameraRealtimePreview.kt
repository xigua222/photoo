package com.voicelike.app

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

private const val COLOR_CARD_CAMERA_PREVIEW_WIDTH = 960
private const val COLOR_CARD_CAMERA_PREVIEW_HEIGHT = 1280
private const val COLOR_CARD_CAMERA_PREVIEW_STALL_TIMEOUT_MS = 1400L
private const val COLOR_CARD_CAMERA_PREVIEW_STALL_CHECK_INTERVAL_MS = 700L
private const val COLOR_CARD_CAMERA_PREVIEW_STALL_COOLDOWN_MS = 2200L
private const val COLOR_CARD_CAMERA_PREVIEW_TRANSFORM_GRACE_MS = 120L
private const val COLOR_CARD_CAMERA_PREVIEW_LOG_TAG = "ColorCardCameraPreview"
private const val COLOR_CARD_CAMERA_PREVIEW_PORTRAIT_DETAIL_SUPPRESSION_SCALE = 1.18f
private const val COLOR_CARD_CAMERA_PREVIEW_PORTRAIT_DETAIL_BLEND_SCALE = 0.84f
private const val COLOR_CARD_CAMERA_PREVIEW_PORTRAIT_SOFTNESS_SCALE = 1.10f
private const val COLOR_CARD_CAMERA_PREVIEW_DITHER_STRENGTH = 0.58f

internal data class ColorCardCameraPreviewPipelineDebugInfo(
    val sourceBufferWidth: Int = 0,
    val sourceBufferHeight: Int = 0,
    val sourceCropLeft: Int = 0,
    val sourceCropTop: Int = 0,
    val sourceCropRight: Int = 0,
    val sourceCropBottom: Int = 0,
    val sourceRotationDegrees: Int = 0,
    val outputWidth: Int = 0,
    val outputHeight: Int = 0,
    val previewCropLeft: Float = 0f,
    val previewCropTop: Float = 0f,
    val previewCropRight: Float = 1f,
    val previewCropBottom: Float = 1f,
    val surfaceTransformReady: Boolean = false,
    val hasReceivedCameraFrame: Boolean = false,
    val firstFrameDelivered: Boolean = false
) {
    fun summaryLines(): List<String> {
        val cropWidth = (sourceCropRight - sourceCropLeft).coerceAtLeast(0)
        val cropHeight = (sourceCropBottom - sourceCropTop).coerceAtLeast(0)
        return listOf(
            "src ${sourceBufferWidth}x${sourceBufferHeight} crop ${cropWidth}x${cropHeight}@${sourceCropLeft},${sourceCropTop} rot $sourceRotationDegrees",
            "out ${outputWidth}x${outputHeight} pv ${formatPreviewDebugFloat(previewCropLeft)},${formatPreviewDebugFloat(previewCropTop)} ${formatPreviewDebugFloat(previewCropRight)},${formatPreviewDebugFloat(previewCropBottom)}",
            "pipe xf=${if (surfaceTransformReady) "1" else "0"} frame=${if (hasReceivedCameraFrame) "1" else "0"} first=${if (firstFrameDelivered) "1" else "0"}"
        )
    }
}

@Composable
internal fun ColorCardCameraRealtimePreview(
    params: ColorCardTraceGpuParams,
    cropCoverage: Float,
    maxRenderFps: Int,
    onPreviewStalled: () -> Unit,
    onFirstFrameRendered: () -> Unit,
    onSourceAspectRatioChanged: (Float) -> Unit,
    onPreviewDebugInfoChanged: (ColorCardCameraPreviewPipelineDebugInfo) -> Unit,
    onViewReady: (ColorCardCameraRealtimePreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentView = remember { mutableStateOf<ColorCardCameraRealtimePreviewView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            currentView.value?.release()
            currentView.value = null
        }
    }
    AndroidView(
        factory = { context ->
            ColorCardCameraRealtimePreviewView(context).also {
                currentView.value = it
                onViewReady(it)
            }
        },
        update = { view ->
            currentView.value = view
            view.setParams(params)
            view.setCropCoverage(cropCoverage)
            view.setFramePacing(maxRenderFps)
            view.setOnPreviewStalled(onPreviewStalled)
            view.setOnFirstFrameRendered(onFirstFrameRendered)
            view.setOnSourceAspectRatioChanged(onSourceAspectRatioChanged)
            view.setOnPreviewDebugInfoChanged(onPreviewDebugInfoChanged)
            onViewReady(view)
        },
        modifier = modifier
    )
}

internal class ColorCardCameraRealtimePreviewView(
    context: Context
) : FrameLayout(context), TextureView.SurfaceTextureListener {

    private val textureView = TextureView(context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        isOpaque = true
        surfaceTextureListener = this@ColorCardCameraRealtimePreviewView
    }
    private val renderer = ColorCardCameraRealtimeRenderer()
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var onPreviewStalled: (() -> Unit)? = null
    private var onFirstFrameRendered: (() -> Unit)? = null
    private var onSourceAspectRatioChanged: ((Float) -> Unit)? = null
    private var onPreviewDebugInfoChanged: ((ColorCardCameraPreviewPipelineDebugInfo) -> Unit)? = null

    val surfaceProvider = Preview.SurfaceProvider { request ->
        renderer.setSurfaceRequest(request, mainExecutor)
    }

    init {
        addView(textureView)
    }

    fun setParams(params: ColorCardTraceGpuParams) {
        renderer.setParams(params)
    }

    fun setCropCoverage(coverage: Float) {
        renderer.setCropCoverage(coverage)
    }

    fun setFramePacing(maxRenderFps: Int) {
        renderer.setFramePacing(maxRenderFps)
    }

    fun setOnPreviewStalled(listener: (() -> Unit)?) {
        onPreviewStalled = listener
        renderer.setOnPreviewStalled {
            mainExecutor.execute {
                onPreviewStalled?.invoke()
            }
        }
    }

    fun setOnFirstFrameRendered(listener: (() -> Unit)?) {
        onFirstFrameRendered = listener
        renderer.setOnFirstFrameRendered {
            mainExecutor.execute {
                onFirstFrameRendered?.invoke()
            }
        }
    }

    fun setOnSourceAspectRatioChanged(listener: (Float) -> Unit) {
        onSourceAspectRatioChanged = listener
        renderer.setOnSourceAspectRatioChanged { aspectRatio ->
            mainExecutor.execute {
                onSourceAspectRatioChanged?.invoke(aspectRatio)
            }
        }
    }

    fun setOnPreviewDebugInfoChanged(listener: (ColorCardCameraPreviewPipelineDebugInfo) -> Unit) {
        onPreviewDebugInfoChanged = listener
        renderer.setOnPreviewDebugInfoChanged { debugInfo ->
            mainExecutor.execute {
                onPreviewDebugInfoChanged?.invoke(debugInfo)
            }
        }
    }

    fun resetPreviewPipeline() {
        renderer.resetPreviewPipeline()
    }

    fun capturePreviewBitmap(
        targetWidth: Int = textureView.width,
        targetHeight: Int = textureView.height
    ): android.graphics.Bitmap? {
        val safeWidth = targetWidth.coerceAtLeast(1)
        val safeHeight = targetHeight.coerceAtLeast(1)
        return textureView.getBitmap(safeWidth, safeHeight)
    }

    fun mapTapToMeteringNormalized(
        normalizedX: Float,
        normalizedY: Float
    ): PointF {
        return renderer.mapTapToMeteringNormalized(normalizedX, normalizedY)
    }

    fun release() {
        renderer.release()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        textureView.surfaceTexture?.let { surface ->
            renderer.attachOutputSurface(surface, textureView.width, textureView.height)
        }
    }

    override fun onDetachedFromWindow() {
        renderer.detachOutputSurface()
        super.onDetachedFromWindow()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        renderer.attachOutputSurface(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer.resizeOutputSurface(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderer.detachOutputSurface()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
}

private class ColorCardCameraRealtimeRenderer {
    private val handlerThread = HandlerThread("ColorCardCameraRealtimeRenderer").apply { start() }
    private val renderHandler = Handler(handlerThread.looper)
    private val stateLock = Any()
    private val renderRunnable = Runnable {
        try {
            renderFrame()
        } finally {
            val shouldReschedule = synchronized(stateLock) {
                renderScheduled = false
                val dirty = renderDirty || cameraFrameAvailable
                renderDirty = false
                dirty
            }
            if (shouldReschedule && !released) {
                scheduleRender()
            }
        }
    }
    private val stallWatchdogRunnable = object : Runnable {
        override fun run() {
            if (released) return
            checkPreviewStall()
            if (shouldKeepWatchingPreviewStall()) {
                renderHandler.postDelayed(this, COLOR_CARD_CAMERA_PREVIEW_STALL_CHECK_INTERVAL_MS)
            } else {
                stallWatchdogScheduled = false
            }
        }
    }

    private var released = false
    private var outputSurfaceTexture: SurfaceTexture? = null
    private var outputWidth = 0
    private var outputHeight = 0
    private var pendingParams: ColorCardTraceGpuParams = buildEmptyCameraPreviewParams()
    private var cropCoverage = 1f

    private var pendingSurfaceRequest: SurfaceRequest? = null
    private var pendingSurfaceExecutor: Executor? = null
    private var activeSurfaceRequest: SurfaceRequest? = null
    private var sourceBufferWidth = COLOR_CARD_CAMERA_PREVIEW_WIDTH
    private var sourceBufferHeight = COLOR_CARD_CAMERA_PREVIEW_HEIGHT
    private var sourceCropRect = Rect(0, 0, COLOR_CARD_CAMERA_PREVIEW_WIDTH, COLOR_CARD_CAMERA_PREVIEW_HEIGHT)
    private var sourceRotationDegrees = 0
    private var pendingViewPortTransform = identityViewPortTransform()
    private var pendingPreviewSurfaceCrop = ColorCardCameraPreviewSurfaceCrop.Full
    private var cameraInputSurfaceTexture: SurfaceTexture? = null
    private var cameraInputSurface: Surface? = null
    private var cameraFrameAvailable = false
    private var hasReceivedCameraFrame = false
    private val cameraTransformMatrix = FloatArray(16)

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    private var programId = 0
    private var externalTextureId = 0
    private var haldTextureId = 0
    private var haldSecondaryTextureId = 0
    private var uploadedHaldBitmap: android.graphics.Bitmap? = null
    private var uploadedSecondaryHaldBitmap: android.graphics.Bitmap? = null

    private var positionLocation = -1
    private var texCoordLocation = -1
    private var textureUniformLocation = -1
    private var texMatrixLocation = -1
    private var cropCoverageLocation = -1
    private var viewPortTransformLocation = -1
    private var colorLocation = -1
    private var colorAxisLocation = -1
    private var sharpenLocation = -1
    private var toneLocation = -1
    private var saturationLocation = -1
    private var brightnessLocation = -1
    private var protectedFaceCountLocation = -1
    private var protectedFacesLocation = -1
    private var portraitEnabledLocation = -1
    private var portraitLightnessLiftLocation = -1
    private var portraitSaturationBoostLocation = -1
    private var portraitSharpenRetentionLocation = -1
    private var portraitSoftenAmountLocation = -1
    private var haldEnabledLocation = -1
    private var haldTextureUniformLocation = -1
    private var haldSecondaryTextureUniformLocation = -1
    private var haldLevelLocation = -1
    private var haldSecondaryLevelLocation = -1
    private var haldStrengthLocation = -1
    private var haldVariantMixLocation = -1
    private var blackMistEnabledLocation = -1
    private var blackMistStrengthLocation = -1
    private var sparkleStrengthLocation = -1
    private var dehazeStrengthLocation = -1
    private var blackMistSoftnessBiasLocation = -1
    private var blackMistBloomBiasLocation = -1
    private var blackMistVeilBiasLocation = -1
    private var blackMistHighlightBiasLocation = -1
    private var blackMistContrastBiasLocation = -1
    private var blackMistRadiusBiasLocation = -1
    private var blackMistHaloBiasLocation = -1
    private var blackMistAtmosphereBiasLocation = -1
    private var blackMistTintLocation = -1
    private var sourceAspectLocation = -1
    private var sourceTexelSizeLocation = -1
    private var minRenderIntervalMs = 0L
    private var lastRenderAtMs = 0L
    private var renderScheduled = false
    private var renderDirty = false
    private var stallWatchdogScheduled = false
    private var onPreviewStalled: (() -> Unit)? = null
    private var onFirstFrameRendered: (() -> Unit)? = null
    private var surfaceRequestStartedAtMs = 0L
    private var lastCameraFrameAtMs = 0L
    private var lastPresentedFrameAtMs = 0L
    private var lastPreviewStallAtMs = 0L
    private var firstFrameDelivered = false
    private var onSourceAspectRatioChanged: ((Float) -> Unit)? = null
    private var onPreviewDebugInfoChanged: ((ColorCardCameraPreviewPipelineDebugInfo) -> Unit)? = null
    private var lastDeliveredSourceAspectRatio = 0f
    private var lastDeliveredPreviewDebugInfo: ColorCardCameraPreviewPipelineDebugInfo? = null
    private var surfaceTransformReady = false

    fun setSurfaceRequest(request: SurfaceRequest, executor: Executor) {
        val supersededRequest = synchronized(stateLock) {
            val previousPending = pendingSurfaceRequest
            pendingSurfaceRequest = request
            pendingSurfaceExecutor = executor
            previousPending
        }
        if (supersededRequest != null && supersededRequest !== request) {
            runCatching { supersededRequest.clearTransformationInfoListener() }
            runCatching { supersededRequest.willNotProvideSurface() }
        }
        request.setTransformationInfoListener(executor) { info ->
            Log.d(
                COLOR_CARD_CAMERA_PREVIEW_LOG_TAG,
                "SurfaceRequest transform resolution=${request.resolution.width}x${request.resolution.height} " +
                    "crop=${info.cropRect.width()}x${info.cropRect.height()}@" +
                    "${info.cropRect.left},${info.cropRect.top} rotation=${info.rotationDegrees}"
            )
            updateViewPortCrop(
                request = request,
                requestResolution = request.resolution,
                cropRect = info.cropRect,
                rotationDegrees = info.rotationDegrees
            )
        }
        renderHandler.post {
            releaseCameraInputSurface(force = true)
            synchronized(stateLock) {
                sourceBufferWidth = request.resolution.width.coerceAtLeast(1)
                sourceBufferHeight = request.resolution.height.coerceAtLeast(1)
                surfaceTransformReady = false
            }
            dispatchPreviewDebugInfo(force = true)
            surfaceRequestStartedAtMs = SystemClock.uptimeMillis()
            lastCameraFrameAtMs = 0L
            lastPresentedFrameAtMs = 0L
            ensurePreviewStallWatchdog()
            ensureCameraInputSurface()
            scheduleRender()
        }
    }

    fun attachOutputSurface(surface: SurfaceTexture, width: Int, height: Int) {
        outputSurfaceTexture = surface
        outputWidth = width.coerceAtLeast(1)
        outputHeight = height.coerceAtLeast(1)
        renderHandler.post {
            recomputeViewPortCrop()
            dispatchPreviewDebugInfo(force = true)
            ensurePreviewStallWatchdog()
            ensureCameraInputSurface()
            scheduleRender()
        }
    }

    fun resizeOutputSurface(width: Int, height: Int) {
        outputWidth = width.coerceAtLeast(1)
        outputHeight = height.coerceAtLeast(1)
        renderHandler.post {
            recomputeViewPortCrop()
            dispatchPreviewDebugInfo(force = true)
            scheduleRender()
        }
    }

    fun detachOutputSurface() {
        outputSurfaceTexture = null
        outputWidth = 0
        outputHeight = 0
        renderHandler.post {
            dispatchPreviewDebugInfo(force = true)
            releaseEglSurface()
        }
    }

    fun setParams(params: ColorCardTraceGpuParams) {
        synchronized(stateLock) {
            pendingParams = params
        }
        scheduleRender()
    }

    fun setCropCoverage(coverage: Float) {
        cropCoverage = coverage.coerceIn(0.2f, 1f)
        scheduleRender()
    }

    fun setFramePacing(maxRenderFps: Int) {
        minRenderIntervalMs = if (maxRenderFps > 0) {
            (1000L / maxRenderFps.toLong()).coerceAtLeast(16L)
        } else {
            0L
        }
        scheduleRender()
    }

    fun setOnPreviewStalled(listener: (() -> Unit)?) {
        synchronized(stateLock) {
            onPreviewStalled = listener
        }
    }

    fun setOnFirstFrameRendered(listener: (() -> Unit)?) {
        synchronized(stateLock) {
            onFirstFrameRendered = listener
        }
    }

    fun setOnSourceAspectRatioChanged(listener: ((Float) -> Unit)?) {
        synchronized(stateLock) {
            onSourceAspectRatioChanged = listener
        }
        dispatchSourceAspectRatioIfNeeded(force = true)
    }

    fun setOnPreviewDebugInfoChanged(listener: ((ColorCardCameraPreviewPipelineDebugInfo) -> Unit)?) {
        synchronized(stateLock) {
            onPreviewDebugInfoChanged = listener
        }
        dispatchPreviewDebugInfo(force = true)
    }

    fun release() {
        if (released) return
        released = true
        val latch = CountDownLatch(1)
        renderHandler.post {
            try {
                releaseInternal()
            } finally {
                latch.countDown()
            }
        }
        latch.await(400, TimeUnit.MILLISECONDS)
        handlerThread.quitSafely()
    }

    private fun dispatchSourceAspectRatioIfNeeded(force: Boolean = false) {
        val listener: ((Float) -> Unit)?
        val aspectRatio: Float
        synchronized(stateLock) {
            if (!force && !surfaceTransformReady) {
                return
            }
            listener = onSourceAspectRatioChanged
            val width = sourceCropRect.width().coerceAtLeast(1)
            val height = sourceCropRect.height().coerceAtLeast(1)
            val normalizedRotation = ((sourceRotationDegrees % 360) + 360) % 360
            aspectRatio = when (normalizedRotation) {
                90, 270 -> height.toFloat() / width.toFloat()
                else -> width.toFloat() / height.toFloat()
            }
            if (!force && kotlin.math.abs(aspectRatio - lastDeliveredSourceAspectRatio) < 0.0001f) {
                return
            }
            lastDeliveredSourceAspectRatio = aspectRatio
        }
        listener?.invoke(aspectRatio)
    }

    private fun dispatchPreviewDebugInfo(force: Boolean = false) {
        val listener: ((ColorCardCameraPreviewPipelineDebugInfo) -> Unit)?
        val debugInfo: ColorCardCameraPreviewPipelineDebugInfo
        synchronized(stateLock) {
            debugInfo = ColorCardCameraPreviewPipelineDebugInfo(
                sourceBufferWidth = sourceBufferWidth,
                sourceBufferHeight = sourceBufferHeight,
                sourceCropLeft = sourceCropRect.left,
                sourceCropTop = sourceCropRect.top,
                sourceCropRight = sourceCropRect.right,
                sourceCropBottom = sourceCropRect.bottom,
                sourceRotationDegrees = sourceRotationDegrees,
                outputWidth = outputWidth,
                outputHeight = outputHeight,
                previewCropLeft = pendingPreviewSurfaceCrop.left,
                previewCropTop = pendingPreviewSurfaceCrop.top,
                previewCropRight = pendingPreviewSurfaceCrop.right,
                previewCropBottom = pendingPreviewSurfaceCrop.bottom,
                surfaceTransformReady = surfaceTransformReady,
                hasReceivedCameraFrame = hasReceivedCameraFrame,
                firstFrameDelivered = firstFrameDelivered
            )
            if (!force && debugInfo == lastDeliveredPreviewDebugInfo) {
                return
            }
            lastDeliveredPreviewDebugInfo = debugInfo
            listener = onPreviewDebugInfoChanged
        }
        listener?.invoke(debugInfo)
    }

    fun resetPreviewPipeline() {
        if (released) return
        renderHandler.post {
            releaseCameraInputSurface(force = true)
            scheduleRender()
        }
    }

    private fun scheduleRender() {
        if (released) return
        val delayMs: Long
        synchronized(stateLock) {
            if (renderScheduled) {
                renderDirty = true
                return
            }
            renderScheduled = true
            delayMs = if (minRenderIntervalMs > 0L) {
                (minRenderIntervalMs - (SystemClock.uptimeMillis() - lastRenderAtMs)).coerceAtLeast(0L)
            } else {
                0L
            }
        }
        renderHandler.postDelayed(renderRunnable, delayMs)
    }

    private fun renderFrame() {
        if (released) return
        runCatching {
            ensureCameraInputSurface()
            if (!ensureGlReady()) return
            val params: ColorCardTraceGpuParams
            val viewPortTransform: FloatArray
            synchronized(stateLock) {
                params = pendingParams
                viewPortTransform = pendingViewPortTransform.copyOf()
            }
            cameraInputSurfaceTexture ?: return

            if (cameraFrameAvailable) {
                cameraInputSurfaceTexture?.updateTexImage()
                cameraInputSurfaceTexture?.getTransformMatrix(cameraTransformMatrix)
                cameraFrameAvailable = false
                hasReceivedCameraFrame = true
                if (!firstFrameDelivered) {
                    firstFrameDelivered = true
                    synchronized(stateLock) { onFirstFrameRendered }?.invoke()
                    dispatchPreviewDebugInfo(force = true)
                }
            }

            if (!hasReceivedCameraFrame) {
                return
            }
            val waitingForStableTransform = synchronized(stateLock) {
                !surfaceTransformReady &&
                    surfaceRequestStartedAtMs > 0L &&
                    SystemClock.uptimeMillis() - surfaceRequestStartedAtMs < COLOR_CARD_CAMERA_PREVIEW_TRANSFORM_GRACE_MS
            }
            if (waitingForStableTransform) {
                return
            }

            GLES20.glViewport(0, 0, outputWidth, outputHeight)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            ensureProgram()
            ensureExternalTexture()
            uploadHaldTextures(params)
            val primaryHaldReady =
                params.haldEnabled &&
                    params.haldBitmap != null &&
                    !params.haldBitmap.isRecycled &&
                    haldTextureId != 0
            val secondaryHaldReady =
                primaryHaldReady &&
                    params.haldSecondaryBitmap != null &&
                    !params.haldSecondaryBitmap.isRecycled &&
                    params.haldSecondaryLevel > 0 &&
                    params.haldVariantMix > 0.0001f &&
                    haldSecondaryTextureId != 0

            GLES20.glUseProgram(programId)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)
            GLES20.glUniform1i(textureUniformLocation, 0)
            GLES20.glUniformMatrix4fv(texMatrixLocation, 1, false, cameraTransformMatrix, 0)
            GLES20.glUniform1f(cropCoverageLocation, cropCoverage)
            GLES20.glUniformMatrix4fv(viewPortTransformLocation, 1, false, viewPortTransform, 0)
            GLES20.glUniform1f(colorLocation, params.color.coerceIn(-1f, 1f))
            GLES20.glUniform2f(
                colorAxisLocation,
                params.colorAxisA.coerceIn(-1f, 1f),
                params.colorAxisB.coerceIn(-1f, 1f)
            )
            GLES20.glUniform1f(sharpenLocation, params.sharpen.coerceIn(-1f, 1f))
            GLES20.glUniform1f(toneLocation, params.tone.coerceIn(-1f, 1f))
            GLES20.glUniform1f(saturationLocation, params.saturation.coerceIn(-1f, 1f))
            GLES20.glUniform1f(brightnessLocation, params.brightness.coerceIn(-1f, 1f))
            GLES20.glUniform1i(
                protectedFaceCountLocation,
                params.protectedFaceCount.coerceIn(0, COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES)
            )
            GLES20.glUniform4fv(
                protectedFacesLocation,
                COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES,
                params.protectedFaces,
                0
            )
            GLES20.glUniform1i(portraitEnabledLocation, if (params.portraitEnabled) 1 else 0)
            GLES20.glUniform1f(
                portraitLightnessLiftLocation,
                params.portraitLightnessLift.coerceIn(0f, 12f)
            )
            GLES20.glUniform1f(
                portraitSaturationBoostLocation,
                params.portraitSaturationBoost.coerceIn(0f, 0.3f)
            )
            GLES20.glUniform1f(
                portraitSharpenRetentionLocation,
                params.portraitSharpenRetention.coerceIn(0f, 1f)
            )
            GLES20.glUniform1f(
                portraitSoftenAmountLocation,
                params.portraitSoftenAmount.coerceIn(0f, 1f)
            )
            GLES20.glUniform1i(haldEnabledLocation, if (primaryHaldReady) 1 else 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, if (primaryHaldReady) haldTextureId else 0)
            GLES20.glUniform1i(haldTextureUniformLocation, 1)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, if (secondaryHaldReady) haldSecondaryTextureId else 0)
            GLES20.glUniform1i(haldSecondaryTextureUniformLocation, 2)
            GLES20.glUniform1f(
                haldLevelLocation,
                if (primaryHaldReady) params.haldLevel.toFloat().coerceAtLeast(0f) else 0f
            )
            GLES20.glUniform1f(
                haldSecondaryLevelLocation,
                if (secondaryHaldReady) params.haldSecondaryLevel.toFloat().coerceAtLeast(0f) else 0f
            )
            GLES20.glUniform1f(
                haldStrengthLocation,
                if (primaryHaldReady) params.haldStrength.coerceIn(0f, 1f) else 0f
            )
            GLES20.glUniform1f(
                haldVariantMixLocation,
                if (secondaryHaldReady) params.haldVariantMix.coerceIn(0f, 1f) else 0f
            )
            GLES20.glUniform1i(blackMistEnabledLocation, if (params.blackMistEnabled) 1 else 0)
            GLES20.glUniform1f(blackMistStrengthLocation, params.blackMistStrength.coerceIn(0f, 1f))
            GLES20.glUniform1f(sparkleStrengthLocation, params.sparkleStrength.coerceIn(0f, 1f))
            GLES20.glUniform1f(dehazeStrengthLocation, params.dehazeStrength.coerceIn(0f, 1f))
            GLES20.glUniform1f(
                blackMistSoftnessBiasLocation,
                params.blackMistSoftnessBias.coerceIn(-0.2f, 0.2f)
            )
            GLES20.glUniform1f(
                blackMistBloomBiasLocation,
                params.blackMistBloomBias.coerceIn(-0.2f, 0.3f)
            )
            GLES20.glUniform1f(
                blackMistVeilBiasLocation,
                params.blackMistVeilBias.coerceIn(-0.03f, 0.05f)
            )
            GLES20.glUniform1f(
                blackMistHighlightBiasLocation,
                params.blackMistHighlightBias.coerceIn(-0.25f, 0.25f)
            )
            GLES20.glUniform1f(
                blackMistContrastBiasLocation,
                params.blackMistContrastBias.coerceIn(-0.4f, 0.4f)
            )
            GLES20.glUniform1f(
                blackMistRadiusBiasLocation,
                params.blackMistRadiusBias.coerceIn(-0.4f, 0.6f)
            )
            GLES20.glUniform1f(
                blackMistHaloBiasLocation,
                params.blackMistHaloBias.coerceIn(-0.5f, 0.6f)
            )
            GLES20.glUniform1f(
                blackMistAtmosphereBiasLocation,
                params.blackMistAtmosphereBias.coerceIn(-0.4f, 0.6f)
            )
            GLES20.glUniform3f(
                blackMistTintLocation,
                params.blackMistTintRed.coerceIn(0f, 1f),
                params.blackMistTintGreen.coerceIn(0f, 1f),
                params.blackMistTintBlue.coerceIn(0f, 1f)
            )
            GLES20.glUniform1f(sourceAspectLocation, params.sourceAspect.coerceAtLeast(0.0001f))
            GLES20.glUniform2f(
                sourceTexelSizeLocation,
                (params.sourceTexelWidth * cropCoverage).coerceAtLeast(0.000001f),
                (params.sourceTexelHeight * cropCoverage).coerceAtLeast(0.000001f)
            )

            COLOR_CARD_CAMERA_FULLSCREEN_QUAD.position(0)
            GLES20.glEnableVertexAttribArray(positionLocation)
            GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, COLOR_CARD_CAMERA_FULLSCREEN_VERTEX_STRIDE_BYTES, COLOR_CARD_CAMERA_FULLSCREEN_QUAD)
            COLOR_CARD_CAMERA_FULLSCREEN_QUAD.position(2)
            GLES20.glEnableVertexAttribArray(texCoordLocation)
            GLES20.glVertexAttribPointer(texCoordLocation, 2, GLES20.GL_FLOAT, false, COLOR_CARD_CAMERA_FULLSCREEN_VERTEX_STRIDE_BYTES, COLOR_CARD_CAMERA_FULLSCREEN_QUAD)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(positionLocation)
            GLES20.glDisableVertexAttribArray(texCoordLocation)

            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
            val renderCompletedAtMs = SystemClock.uptimeMillis()
            lastRenderAtMs = renderCompletedAtMs
            if (hasReceivedCameraFrame) {
                lastPresentedFrameAtMs = renderCompletedAtMs
            }
        }.onFailure { error ->
            Log.w(COLOR_CARD_CAMERA_PREVIEW_LOG_TAG, "Preview render failed, requesting recovery", error)
            notifyPreviewStalled()
        }
    }

    private fun uploadHaldTextures(params: ColorCardTraceGpuParams) {
        val primaryBitmap = params.haldBitmap?.takeUnless { it.isRecycled }
        if (primaryBitmap != null) {
            ensureHaldTexture()
            if (uploadedHaldBitmap !== primaryBitmap) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldTextureId)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, primaryBitmap, 0)
                uploadedHaldBitmap = primaryBitmap
            }
        } else {
            uploadedHaldBitmap = null
        }
        val secondaryBitmap = params.haldSecondaryBitmap?.takeUnless { it.isRecycled }
        if (secondaryBitmap != null) {
            ensureSecondaryHaldTexture()
            if (uploadedSecondaryHaldBitmap !== secondaryBitmap) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldSecondaryTextureId)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, secondaryBitmap, 0)
                uploadedSecondaryHaldBitmap = secondaryBitmap
            }
        } else {
            uploadedSecondaryHaldBitmap = null
        }
    }

    private fun ensureCameraInputSurface() {
        val request = synchronized(stateLock) { pendingSurfaceRequest } ?: return
        val executor = synchronized(stateLock) { pendingSurfaceExecutor } ?: return
        if (cameraInputSurface != null || outputSurfaceTexture == null) return
        if (!ensureGlReady()) return
        ensureExternalTexture()
        activeSurfaceRequest = request
        cameraInputSurfaceTexture = SurfaceTexture(externalTextureId).apply {
            setDefaultBufferSize(request.resolution.width, request.resolution.height)
            setOnFrameAvailableListener(
                {
                    lastCameraFrameAtMs = SystemClock.uptimeMillis()
                    cameraFrameAvailable = true
                    ensurePreviewStallWatchdog()
                    scheduleRender()
                },
                renderHandler
            )
        }
        cameraInputSurface = Surface(cameraInputSurfaceTexture)
        request.provideSurface(cameraInputSurface!!, executor) {
            renderHandler.post {
                releaseCameraInputSurface(request)
                ensureCameraInputSurface()
                scheduleRender()
            }
        }
        firstFrameDelivered = false
        synchronized(stateLock) {
            pendingSurfaceRequest = null
            pendingSurfaceExecutor = null
        }
    }

    private fun ensureGlReady(): Boolean {
        val targetSurface = outputSurfaceTexture ?: return false
        if (outputWidth <= 0 || outputHeight <= 0) return false
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, configCount, 0)
            eglConfig = configs[0]
            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        }
        if (eglSurface === EGL14.EGL_NO_SURFACE) {
            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, targetSurface, surfaceAttribs, 0)
        }
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        return true
    }

    private fun ensureProgram() {
        if (programId != 0) return
        val vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, COLOR_CARD_CAMERA_VERTEX_SHADER)
        val fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, COLOR_CARD_CAMERA_FRAGMENT_SHADER)
        programId = GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShaderId)
            GLES20.glAttachShader(program, fragmentShaderId)
            GLES20.glLinkProgram(program)
            GLES20.glDeleteShader(vertexShaderId)
            GLES20.glDeleteShader(fragmentShaderId)
        }
        positionLocation = GLES20.glGetAttribLocation(programId, "aPosition")
        texCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord")
        textureUniformLocation = GLES20.glGetUniformLocation(programId, "uTexture")
        texMatrixLocation = GLES20.glGetUniformLocation(programId, "uTexMatrix")
        cropCoverageLocation = GLES20.glGetUniformLocation(programId, "uCropCoverage")
        viewPortTransformLocation = GLES20.glGetUniformLocation(programId, "uViewPortTransform")
        colorLocation = GLES20.glGetUniformLocation(programId, "uColor")
        colorAxisLocation = GLES20.glGetUniformLocation(programId, "uColorAxis")
        sharpenLocation = GLES20.glGetUniformLocation(programId, "uSharpen")
        toneLocation = GLES20.glGetUniformLocation(programId, "uTone")
        saturationLocation = GLES20.glGetUniformLocation(programId, "uSaturation")
        brightnessLocation = GLES20.glGetUniformLocation(programId, "uBrightness")
        protectedFaceCountLocation = GLES20.glGetUniformLocation(programId, "uProtectedFaceCount")
        protectedFacesLocation = GLES20.glGetUniformLocation(programId, "uProtectedFaces")
        portraitEnabledLocation = GLES20.glGetUniformLocation(programId, "uPortraitEnabled")
        portraitLightnessLiftLocation = GLES20.glGetUniformLocation(programId, "uPortraitLightnessLift")
        portraitSaturationBoostLocation = GLES20.glGetUniformLocation(programId, "uPortraitSaturationBoost")
        portraitSharpenRetentionLocation = GLES20.glGetUniformLocation(programId, "uPortraitSharpenRetention")
        portraitSoftenAmountLocation = GLES20.glGetUniformLocation(programId, "uPortraitSoftenAmount")
        haldEnabledLocation = GLES20.glGetUniformLocation(programId, "uHaldEnabled")
        haldTextureUniformLocation = GLES20.glGetUniformLocation(programId, "uHaldTexture")
        haldSecondaryTextureUniformLocation = GLES20.glGetUniformLocation(programId, "uHaldTextureSecondary")
        haldLevelLocation = GLES20.glGetUniformLocation(programId, "uHaldLevel")
        haldSecondaryLevelLocation = GLES20.glGetUniformLocation(programId, "uHaldSecondaryLevel")
        haldStrengthLocation = GLES20.glGetUniformLocation(programId, "uHaldStrength")
        haldVariantMixLocation = GLES20.glGetUniformLocation(programId, "uHaldVariantMix")
        blackMistEnabledLocation = GLES20.glGetUniformLocation(programId, "uBlackMistEnabled")
        blackMistStrengthLocation = GLES20.glGetUniformLocation(programId, "uBlackMistStrength")
        sparkleStrengthLocation = GLES20.glGetUniformLocation(programId, "uSparkleStrength")
        dehazeStrengthLocation = GLES20.glGetUniformLocation(programId, "uDehazeStrength")
        blackMistSoftnessBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistSoftnessBias")
        blackMistBloomBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistBloomBias")
        blackMistVeilBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistVeilBias")
        blackMistHighlightBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistHighlightBias")
        blackMistContrastBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistContrastBias")
        blackMistRadiusBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistRadiusBias")
        blackMistHaloBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistHaloBias")
        blackMistAtmosphereBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistAtmosphereBias")
        blackMistTintLocation = GLES20.glGetUniformLocation(programId, "uBlackMistTint")
        sourceAspectLocation = GLES20.glGetUniformLocation(programId, "uSourceAspect")
        sourceTexelSizeLocation = GLES20.glGetUniformLocation(programId, "uSourceTexelSize")
    }

    private fun ensureExternalTexture() {
        if (externalTextureId != 0) return
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        externalTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun ensureHaldTexture() {
        if (haldTextureId != 0) return
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        haldTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun ensureSecondaryHaldTexture() {
        if (haldSecondaryTextureId != 0) return
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        haldSecondaryTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldSecondaryTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun releaseInternal() {
        stopPreviewStallWatchdog()
        releaseCameraInputSurface(force = true)
        releaseEglSurface()
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
        if (externalTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(externalTextureId), 0)
            externalTextureId = 0
        }
        if (haldTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(haldTextureId), 0)
            haldTextureId = 0
        }
        if (haldSecondaryTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(haldSecondaryTextureId), 0)
            haldSecondaryTextureId = 0
        }
        uploadedHaldBitmap = null
        uploadedSecondaryHaldBitmap = null
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
        }
    }

    private fun releaseCameraInputSurface(
        request: SurfaceRequest? = null,
        force: Boolean = false
    ) {
        if (!force) {
            val currentRequest = activeSurfaceRequest ?: return
            if (request != null && currentRequest !== request) {
                return
            }
        }
        activeSurfaceRequest?.clearTransformationInfoListener()
        activeSurfaceRequest = null
        cameraInputSurface?.release()
        cameraInputSurface = null
        cameraInputSurfaceTexture?.release()
        cameraInputSurfaceTexture = null
        cameraFrameAvailable = false
        hasReceivedCameraFrame = false
        firstFrameDelivered = false
        synchronized(stateLock) {
            surfaceTransformReady = false
            pendingPreviewSurfaceCrop = ColorCardCameraPreviewSurfaceCrop.Full
        }
        dispatchPreviewDebugInfo(force = true)
        surfaceRequestStartedAtMs = 0L
        lastCameraFrameAtMs = 0L
        lastPresentedFrameAtMs = 0L
        if (!shouldKeepWatchingPreviewStall()) {
            stopPreviewStallWatchdog()
        }
    }

    private fun releaseEglSurface() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY && eglSurface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun updateViewPortCrop(
        request: SurfaceRequest,
        requestResolution: android.util.Size,
        cropRect: Rect,
        rotationDegrees: Int
    ) {
        synchronized(stateLock) {
            val isCurrentRequest =
                activeSurfaceRequest === request || pendingSurfaceRequest === request
            if (!isCurrentRequest) {
                Log.d(
                    COLOR_CARD_CAMERA_PREVIEW_LOG_TAG,
                    "Ignoring stale transform for superseded surface request"
                )
                return
            }
            sourceBufferWidth = requestResolution.width.coerceAtLeast(1)
            sourceBufferHeight = requestResolution.height.coerceAtLeast(1)
            sourceCropRect = Rect(cropRect)
            sourceRotationDegrees = rotationDegrees
            surfaceTransformReady = true
        }
        recomputeViewPortCrop()
        dispatchSourceAspectRatioIfNeeded()
        dispatchPreviewDebugInfo(force = true)
        scheduleRender()
    }

    private fun recomputeViewPortCrop() {
        synchronized(stateLock) {
            pendingViewPortTransform = resolveViewPortTransform(
                sourceWidth = sourceBufferWidth,
                sourceHeight = sourceBufferHeight,
                cropRect = sourceCropRect,
                rotationDegrees = sourceRotationDegrees,
                outputWidth = outputWidth,
                outputHeight = outputHeight
            )
            pendingPreviewSurfaceCrop = resolvePreviewSurfaceCrop(
                sourceWidth = sourceBufferWidth,
                sourceHeight = sourceBufferHeight,
                cropRect = sourceCropRect,
                rotationDegrees = sourceRotationDegrees,
                outputWidth = outputWidth,
                outputHeight = outputHeight
            )
        }
        dispatchPreviewDebugInfo(force = true)
    }

    private fun resolveViewPortTransform(
        sourceWidth: Int,
        sourceHeight: Int,
        cropRect: Rect,
        rotationDegrees: Int,
        outputWidth: Int,
        outputHeight: Int
    ): FloatArray {
        val crop = resolvePreviewSurfaceCrop(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            cropRect = cropRect,
            rotationDegrees = rotationDegrees,
            outputWidth = outputWidth,
            outputHeight = outputHeight
        )
        return buildViewPortTransform(
            xFromX = crop.width,
            xFromY = 0f,
            xOffset = crop.left,
            yFromX = 0f,
            yFromY = crop.height,
            yOffset = crop.top
        )
    }

    private fun resolvePreviewSurfaceCrop(
        sourceWidth: Int,
        sourceHeight: Int,
        cropRect: Rect,
        rotationDegrees: Int,
        outputWidth: Int,
        outputHeight: Int
    ): ColorCardCameraPreviewSurfaceCrop {
        if (sourceWidth <= 0 || sourceHeight <= 0 || outputWidth <= 0 || outputHeight <= 0) {
            return ColorCardCameraPreviewSurfaceCrop.Full
        }
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val mappedCropRect = if (normalizedRotation == 90 || normalizedRotation == 180 || normalizedRotation == 270) {
            mapOrientedRectToSourceRect(
                left = (cropRect.left.toFloat() / sourceWidth.toFloat()).coerceIn(0f, 1f),
                top = (cropRect.top.toFloat() / sourceHeight.toFloat()).coerceIn(0f, 1f),
                right = (cropRect.right.toFloat() / sourceWidth.toFloat()).coerceIn(0f, 1f),
                bottom = (cropRect.bottom.toFloat() / sourceHeight.toFloat()).coerceIn(0f, 1f),
                rotationDegrees = normalizedRotation
            )
        } else {
            RectF(
                (cropRect.left.toFloat() / sourceWidth.toFloat()).coerceIn(0f, 1f),
                (cropRect.top.toFloat() / sourceHeight.toFloat()).coerceIn(0f, 1f),
                (cropRect.right.toFloat() / sourceWidth.toFloat()).coerceIn(0f, 1f),
                (cropRect.bottom.toFloat() / sourceHeight.toFloat()).coerceIn(0f, 1f)
            )
        }
        val safeLeft = mappedCropRect.left.coerceIn(0f, 1f)
        val safeTop = mappedCropRect.top.coerceIn(0f, 1f)
        val safeRight = mappedCropRect.right.coerceIn(safeLeft, 1f)
        val safeBottom = mappedCropRect.bottom.coerceIn(safeTop, 1f)
        return ColorCardCameraPreviewSurfaceCrop(
            left = safeLeft,
            top = safeTop,
            right = safeRight,
            bottom = safeBottom
        )
    }

    fun mapTapToMeteringNormalized(
        normalizedX: Float,
        normalizedY: Float
    ): PointF {
        synchronized(stateLock) {
            return pendingPreviewSurfaceCrop.mapNormalizedPoint(
                normalizedX = normalizedX,
                normalizedY = normalizedY,
                cropCoverage = cropCoverage
            )
        }
    }

    private fun buildViewPortTransform(
        xFromX: Float,
        xFromY: Float,
        xOffset: Float,
        yFromX: Float,
        yFromY: Float,
        yOffset: Float
    ): FloatArray = floatArrayOf(
        xFromX, yFromX, 0f, 0f,
        xFromY, yFromY, 0f, 0f,
        0f, 0f, 1f, 0f,
        xOffset, yOffset, 0f, 1f
    )

    private fun mapOrientedRectToSourceRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rotationDegrees: Int
    ): RectF {
        val topLeft = mapOrientedPointToSource(left, top, rotationDegrees)
        val topRight = mapOrientedPointToSource(right, top, rotationDegrees)
        val bottomLeft = mapOrientedPointToSource(left, bottom, rotationDegrees)
        val bottomRight = mapOrientedPointToSource(right, bottom, rotationDegrees)
        return RectF(
            minOf(topLeft.first, topRight.first, bottomLeft.first, bottomRight.first),
            minOf(topLeft.second, topRight.second, bottomLeft.second, bottomRight.second),
            maxOf(topLeft.first, topRight.first, bottomLeft.first, bottomRight.first),
            maxOf(topLeft.second, topRight.second, bottomLeft.second, bottomRight.second)
        )
    }

    private fun mapOrientedPointToSource(
        x: Float,
        y: Float,
        rotationDegrees: Int
    ): Pair<Float, Float> {
        return when (rotationDegrees) {
            90 -> (1f - y) to x
            180 -> (1f - x) to (1f - y)
            270 -> y to (1f - x)
            else -> x to y
        }
    }

    private fun identityViewPortTransform(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private fun ensurePreviewStallWatchdog() {
        if (stallWatchdogScheduled) return
        stallWatchdogScheduled = true
        renderHandler.postDelayed(
            stallWatchdogRunnable,
            COLOR_CARD_CAMERA_PREVIEW_STALL_CHECK_INTERVAL_MS
        )
    }

    private fun stopPreviewStallWatchdog() {
        stallWatchdogScheduled = false
        renderHandler.removeCallbacks(stallWatchdogRunnable)
    }

    private fun shouldKeepWatchingPreviewStall(): Boolean {
        return outputSurfaceTexture != null &&
            (pendingSurfaceRequest != null || activeSurfaceRequest != null || cameraInputSurface != null)
    }

    private fun checkPreviewStall() {
        if (!shouldKeepWatchingPreviewStall()) return
        val now = SystemClock.uptimeMillis()
        val lastFrameAt = lastCameraFrameAtMs
        val startedAt = surfaceRequestStartedAtMs
        if (startedAt <= 0L) return
        val lastPresentedAt = lastPresentedFrameAtMs
        val stalledBeforeFirstFrame =
            !firstFrameDelivered &&
                now - startedAt >= COLOR_CARD_CAMERA_PREVIEW_STALL_TIMEOUT_MS
        val stalledFrameDelivery =
            lastFrameAt > 0L &&
                now - lastFrameAt >= COLOR_CARD_CAMERA_PREVIEW_STALL_TIMEOUT_MS
        val stalledPresentation =
            lastFrameAt > 0L &&
                (
                    (lastPresentedAt <= 0L && now - startedAt >= COLOR_CARD_CAMERA_PREVIEW_STALL_TIMEOUT_MS) ||
                        (lastPresentedAt > 0L &&
                            now - lastPresentedAt >= COLOR_CARD_CAMERA_PREVIEW_STALL_TIMEOUT_MS &&
                            now - lastFrameAt <= COLOR_CARD_CAMERA_PREVIEW_STALL_CHECK_INTERVAL_MS * 2)
                    )
        if (!stalledBeforeFirstFrame && !stalledFrameDelivery && !stalledPresentation) return
        val stalledForMs = when {
            stalledPresentation && lastPresentedAt > 0L -> now - lastPresentedAt
            lastFrameAt > 0L -> now - lastFrameAt
            else -> now - startedAt
        }
        Log.w(
            COLOR_CARD_CAMERA_PREVIEW_LOG_TAG,
            "Preview frame stream stalled for ${stalledForMs}ms, requesting recovery"
        )
        notifyPreviewStalled()
    }

    private fun notifyPreviewStalled() {
        val callback = synchronized(stateLock) {
            val now = SystemClock.uptimeMillis()
            if (now - lastPreviewStallAtMs < COLOR_CARD_CAMERA_PREVIEW_STALL_COOLDOWN_MS) {
                null
            } else {
                lastPreviewStallAtMs = now
                onPreviewStalled
            }
        } ?: return
        callback.invoke()
    }
}

private data class ColorCardCameraPreviewSurfaceCrop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float
        get() = (right - left).coerceAtLeast(0.0001f)

    val height: Float
        get() = (bottom - top).coerceAtLeast(0.0001f)

    fun mapNormalizedPoint(
        normalizedX: Float,
        normalizedY: Float,
        cropCoverage: Float
    ): PointF {
        val safeCropCoverage = cropCoverage.coerceIn(0.2f, 1f)
        val inset = (1f - safeCropCoverage) * 0.5f
        val safeX = normalizedX.coerceIn(0f, 1f)
        val safeY = normalizedY.coerceIn(0f, 1f)
        return PointF(
            (left + (width * (inset + (safeX * safeCropCoverage)))).coerceIn(0f, 1f),
            (top + (height * (inset + (safeY * safeCropCoverage)))).coerceIn(0f, 1f)
        )
    }

    companion object {
        val Full = ColorCardCameraPreviewSurfaceCrop(
            left = 0f,
            top = 0f,
            right = 1f,
            bottom = 1f
        )
    }
}

private fun formatPreviewDebugFloat(value: Float): String {
    return String.format(java.util.Locale.US, "%.3f", value)
}

private fun buildEmptyCameraPreviewParams(): ColorCardTraceGpuParams =
    ColorCardTraceGpuParams(
        filterEnabled = false,
        anchorCount = 0,
        sourceAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
        targetAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
        blendStrength = 0f,
        maxLightnessShift = 0f,
        maxChromaShift = 0f,
        color = 0f,
        colorAxisA = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS.aWeight,
        colorAxisB = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS.bWeight,
        sharpen = 0f,
        tone = 0f,
        saturation = 0f,
        brightness = 0f,
        protectedFaceCount = 0,
        protectedFaces = FloatArray(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES * 4),
        portraitEnabled = false,
        portraitLightnessLift = 0f,
        portraitSaturationBoost = 0f,
        portraitSharpenRetention = 1f,
        portraitSoftenAmount = 0f,
        haldEnabled = false,
        haldLevel = 0,
        haldStrength = 0f,
        haldBitmap = null,
        haldSecondaryLevel = 0,
        haldSecondaryBitmap = null,
        haldVariantMix = 0f,
        pixelPosterEnabled = false,
        pixelPosterUseSourceColor = false,
        pixelPosterBlockCountLongSide = 0f,
        pixelPosterThreshold = 0f,
        pixelPosterContrast = 0f,
        pixelPosterNoiseAmount = 0f,
        pixelPosterGridLineWidth = 0f,
        pixelPosterGridLineOpacity = 0f,
        pixelPosterDetailBoost = 0f,
        pixelPosterShadowDetailBias = 0f,
        pixelPosterStrength = 0f,
        pixelPosterDarkRed = 0f,
        pixelPosterDarkGreen = 0f,
        pixelPosterDarkBlue = 0f,
        pixelPosterLightRed = 0f,
        pixelPosterLightGreen = 0f,
        pixelPosterLightBlue = 0f,
        subjectFrameEnabled = false,
        subjectFrameRect = floatArrayOf(0f, 0f, 0f, 0f),
        subjectFrameBorderThickness = 0f,
        subjectFrameStrength = 0f,
        subjectFrameBorderRed = 0f,
        subjectFrameBorderGreen = 0f,
        subjectFrameBorderBlue = 0f,
        blackMistEnabled = false,
        blackMistStrength = 0f,
        sourceAspect = COLOR_CARD_CAMERA_PREVIEW_WIDTH.toFloat() / COLOR_CARD_CAMERA_PREVIEW_HEIGHT.toFloat(),
        sourceTexelWidth = 1f / COLOR_CARD_CAMERA_PREVIEW_WIDTH.toFloat(),
        sourceTexelHeight = 1f / COLOR_CARD_CAMERA_PREVIEW_HEIGHT.toFloat()
    )

private const val COLOR_CARD_CAMERA_FULLSCREEN_VERTEX_STRIDE_BYTES = 4 * 4

private val COLOR_CARD_CAMERA_FULLSCREEN_QUAD: FloatBuffer = ByteBuffer
    .allocateDirect(4 * 4 * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
        put(
            floatArrayOf(
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                -1f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f
            )
        )
        position(0)
    }

private val COLOR_CARD_CAMERA_VERTEX_SHADER = """
attribute vec2 aPosition;
attribute vec2 aTexCoord;
uniform mat4 uTexMatrix;
uniform float uCropCoverage;
uniform mat4 uViewPortTransform;
varying vec2 vTexCoord;
varying vec2 vMaskCoord;

void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
    vec2 cropped = ((aTexCoord - 0.5) * uCropCoverage) + 0.5;
    vec2 viewPortCropped = (uViewPortTransform * vec4(cropped, 0.0, 1.0)).xy;
    vMaskCoord = viewPortCropped;
    vTexCoord = (uTexMatrix * vec4(viewPortCropped, 0.0, 1.0)).xy;
}
""".trimIndent()

private val COLOR_CARD_CAMERA_FRAGMENT_SHADER = """
#extension GL_OES_EGL_image_external : require
precision highp float;

uniform samplerExternalOES uTexture;
uniform sampler2D uHaldTexture;
uniform sampler2D uHaldTextureSecondary;
uniform float uColor;
uniform vec2 uColorAxis;
uniform float uSharpen;
uniform float uTone;
uniform float uSaturation;
uniform float uBrightness;
uniform int uProtectedFaceCount;
uniform vec4 uProtectedFaces[${COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES}];
uniform int uPortraitEnabled;
uniform float uPortraitLightnessLift;
uniform float uPortraitSaturationBoost;
uniform float uPortraitSharpenRetention;
uniform float uPortraitSoftenAmount;
uniform int uHaldEnabled;
uniform float uHaldLevel;
uniform float uHaldSecondaryLevel;
uniform float uHaldStrength;
uniform float uHaldVariantMix;
uniform int uBlackMistEnabled;
uniform float uBlackMistStrength;
uniform float uSparkleStrength;
uniform float uDehazeStrength;
uniform float uBlackMistSoftnessBias;
uniform float uBlackMistBloomBias;
uniform float uBlackMistVeilBias;
uniform float uBlackMistHighlightBias;
uniform float uBlackMistContrastBias;
uniform float uBlackMistRadiusBias;
uniform float uBlackMistHaloBias;
uniform float uBlackMistAtmosphereBias;
uniform vec3 uBlackMistTint;
uniform float uSourceAspect;
uniform vec2 uSourceTexelSize;
varying vec2 vTexCoord;
varying vec2 vMaskCoord;

const int MAX_PROTECTED_FACES = ${COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES};

float faceProtectionMask(vec2 uv);
float skinToneMask(vec3 rgb);
float skinProtectionAmount(vec3 rgb, vec2 uv);
float portraitDetailAmount(vec3 rgb, vec2 maskUv);
float portraitSoftnessAmount(vec3 rgb, vec2 maskUv);

vec2 clampSourceUv(vec2 uv) {
    return clamp(uv, vec2(0.001), vec2(0.999));
}

vec3 sampleSource(vec2 uv) {
    return texture2D(uTexture, clampSourceUv(uv)).rgb;
}

float srgbToLinear(float channel) {
    if (channel <= 0.04045) return channel / 12.92;
    return pow((channel + 0.055) / 1.055, 2.4);
}

float linearToSrgb(float channel) {
    float safeChannel = clamp(channel, 0.0, 1.0);
    if (safeChannel <= 0.0031308) return safeChannel * 12.92;
    return (1.055 * pow(safeChannel, 1.0 / 2.4)) - 0.055;
}

float labPivot(float value) {
    if (value > 0.008856) return pow(value, 1.0 / 3.0);
    return (7.787 * value) + (16.0 / 116.0);
}

float inverseLabPivot(float value) {
    float cube = value * value * value;
    if (cube > 0.008856) return cube;
    return (value - (16.0 / 116.0)) / 7.787;
}

vec3 rgbToLab(vec3 rgb) {
    float rf = srgbToLinear(rgb.r);
    float gf = srgbToLinear(rgb.g);
    float bf = srgbToLinear(rgb.b);
    float x = (rf * 0.4124564) + (gf * 0.3575761) + (bf * 0.1804375);
    float y = (rf * 0.2126729) + (gf * 0.7151522) + (bf * 0.0721750);
    float z = (rf * 0.0193339) + (gf * 0.1191920) + (bf * 0.9503041);
    float xPivot = labPivot(x / 0.95047);
    float yPivot = labPivot(y / 1.00000);
    float zPivot = labPivot(z / 1.08883);
    return vec3((116.0 * yPivot) - 16.0, 500.0 * (xPivot - yPivot), 200.0 * (yPivot - zPivot));
}

vec3 labToRgb(vec3 lab) {
    float fy = (lab.x + 16.0) / 116.0;
    float fx = (lab.y / 500.0) + fy;
    float fz = fy - (lab.z / 200.0);
    float x = 0.95047 * inverseLabPivot(fx);
    float y = 1.00000 * inverseLabPivot(fy);
    float z = 1.08883 * inverseLabPivot(fz);
    float rLinear = (x * 3.2404542) + (y * -1.5371385) + (z * -0.4985314);
    float gLinear = (x * -0.9692660) + (y * 1.8760108) + (z * 0.0415560);
    float bLinear = (x * 0.0556434) + (y * -0.2040259) + (z * 1.0572252);
    return vec3(linearToSrgb(rLinear), linearToSrgb(gLinear), linearToSrgb(bLinear));
}

float applyToneCurve(float lightness, float tone) {
    float safeLightness = clamp(lightness, 0.0, 1.0);
    float amount = clamp(tone, -1.0, 1.0);
    if (amount >= 0.0) {
        float sCurve = safeLightness * safeLightness * (3.0 - (2.0 * safeLightness));
        return mix(safeLightness, sCurve, amount);
    }
    float flattened = 0.5 + ((safeLightness - 0.5) * (1.0 + (amount * 0.58)));
    return mix(safeLightness, flattened, -amount);
}

vec3 applyCreativeAdjustments(
    vec3 rgb,
    float color,
    float tone,
    float saturation,
    float brightness
) {
    vec3 adjustedLab = rgbToLab(rgb);
    vec2 safeColorAxis = dot(uColorAxis, uColorAxis) > 0.0001 ? normalize(uColorAxis) : vec2(1.0, 0.0);
    if (abs(tone) > 0.0001) adjustedLab.x = clamp(applyToneCurve(adjustedLab.x / 100.0, tone) * 100.0, 0.0, 100.0);
    if (abs(brightness) > 0.0001) adjustedLab.x = clamp(adjustedLab.x + (brightness * 18.0), 0.0, 100.0);
    if (abs(color) > 0.0001) {
        float colorShift = color * 16.0;
        adjustedLab.y = adjustedLab.y + (safeColorAxis.x * colorShift);
        adjustedLab.z = adjustedLab.z + (safeColorAxis.y * colorShift);
    }
    if (abs(saturation) > 0.0001) {
        float satFactor = saturation >= 0.0 ? (1.0 + (saturation * 0.75)) : (1.0 + (saturation * 0.68));
        adjustedLab.yz *= max(satFactor, 0.12);
    }
    return labToRgb(adjustedLab);
}

float portraitLiftZone(float normalizedLightness) {
    float safeLightness = clamp(normalizedLightness, 0.0, 1.0);
    if (safeLightness <= 0.08 || safeLightness >= 0.90) {
        return 0.0;
    }
    if (safeLightness < 0.26) {
        return smoothstep(0.08, 0.26, safeLightness);
    }
    if (safeLightness > 0.72) {
        return 1.0 - smoothstep(0.72, 0.90, safeLightness);
    }
    return 1.0;
}

vec3 sampleHaldCell(sampler2D haldTexture, float rIndex, float gIndex, float bIndex, float cubeSide, float imageSide) {
    float linearIndex = rIndex + (gIndex * cubeSide) + (bIndex * cubeSide * cubeSide);
    float x = mod(linearIndex, imageSide);
    float y = floor(linearIndex / imageSide);
    vec2 uv = vec2(
        (x + 0.5) / imageSide,
        (y + 0.5) / imageSide
    );
    return texture2D(haldTexture, uv).rgb;
}

vec3 applySingleHaldClut(sampler2D haldTexture, float haldLevel, vec3 rgb) {
    float safeLevel = max(haldLevel, 2.0);
    float cubeSide = safeLevel * safeLevel;
    float imageSide = safeLevel * cubeSide;
    float scaledR = clamp(rgb.r, 0.0, 1.0) * (cubeSide - 1.0);
    float scaledG = clamp(rgb.g, 0.0, 1.0) * (cubeSide - 1.0);
    float scaledB = clamp(rgb.b, 0.0, 1.0) * (cubeSide - 1.0);
    float r0 = floor(scaledR);
    float g0 = floor(scaledG);
    float b0 = floor(scaledB);
    float r1 = min(r0 + 1.0, cubeSide - 1.0);
    float g1 = min(g0 + 1.0, cubeSide - 1.0);
    float b1 = min(b0 + 1.0, cubeSide - 1.0);
    float rf = fract(scaledR);
    float gf = fract(scaledG);
    float bf = fract(scaledB);
    vec3 c000 = sampleHaldCell(haldTexture, r0, g0, b0, cubeSide, imageSide);
    vec3 c100 = sampleHaldCell(haldTexture, r1, g0, b0, cubeSide, imageSide);
    vec3 c010 = sampleHaldCell(haldTexture, r0, g1, b0, cubeSide, imageSide);
    vec3 c110 = sampleHaldCell(haldTexture, r1, g1, b0, cubeSide, imageSide);
    vec3 c001 = sampleHaldCell(haldTexture, r0, g0, b1, cubeSide, imageSide);
    vec3 c101 = sampleHaldCell(haldTexture, r1, g0, b1, cubeSide, imageSide);
    vec3 c011 = sampleHaldCell(haldTexture, r0, g1, b1, cubeSide, imageSide);
    vec3 c111 = sampleHaldCell(haldTexture, r1, g1, b1, cubeSide, imageSide);
    vec3 c00 = mix(c000, c100, rf);
    vec3 c10 = mix(c010, c110, rf);
    vec3 c01 = mix(c001, c101, rf);
    vec3 c11 = mix(c011, c111, rf);
    vec3 c0 = mix(c00, c10, gf);
    vec3 c1 = mix(c01, c11, gf);
    return mix(c0, c1, bf);
}

vec3 applyPortraitAdjustments(vec3 rgb, float portraitMask) {
    if (uPortraitEnabled == 0 || portraitMask <= 0.0001) {
        return rgb;
    }
    vec3 adjustedLab = rgbToLab(rgb);
    float liftZone = portraitLiftZone(adjustedLab.x / 100.0);
    if (liftZone <= 0.0001) {
        return rgb;
    }
    float effectiveMask = clamp(portraitMask * liftZone, 0.0, 1.0);
    adjustedLab.x = clamp(adjustedLab.x + (uPortraitLightnessLift * effectiveMask), 0.0, 100.0);
    float saturationFactor = 1.0 + (uPortraitSaturationBoost * effectiveMask);
    adjustedLab.yz *= max(saturationFactor, 0.12);
    return labToRgb(adjustedLab);
}

vec3 applySourceDetailAdjust(vec2 uv, vec2 maskUv, vec3 centerRgb) {
    float safeSharpen = clamp(uSharpen, -1.0, 1.0);
    float detailSuppressionAmount =
        (uPortraitEnabled != 0) && (uProtectedFaceCount > 0)
            ? (1.0 - clamp(uPortraitSharpenRetention, 0.0, 1.0))
            : 0.0;
    bool portraitDetailEnabled = detailSuppressionAmount > 0.0001;
    if (abs(safeSharpen) <= 0.0001 && !portraitDetailEnabled) {
        return centerRgb;
    }
    vec2 texel = max(uSourceTexelSize, vec2(0.000001));
    vec3 left = texture2D(uTexture, clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb;
    vec3 right = texture2D(uTexture, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb;
    vec3 up = texture2D(uTexture, clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0))).rgb;
    vec3 down = texture2D(uTexture, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb;
    vec3 blurred = ((centerRgb * 4.0) + left + right + up + down) / 8.0;
    float effectiveSharpen = safeSharpen;
    if (portraitDetailEnabled) {
        float portraitProtection = portraitDetailAmount(centerRgb, maskUv);
        if (portraitProtection > 0.0001 && safeSharpen > 0.0) {
            effectiveSharpen = safeSharpen * mix(
                1.0,
                clamp(uPortraitSharpenRetention, 0.0, 1.0),
                portraitProtection
            );
        }
    }
    vec3 detailAdjusted = effectiveSharpen >= 0.0
        ? clamp(
            centerRgb + ((centerRgb - blurred) * (effectiveSharpen * ${COLOR_CARD_TRACE_SHARPEN_DETAIL_SCALE})),
            0.0,
            1.0
        )
        : mix(centerRgb, blurred, -effectiveSharpen * ${COLOR_CARD_TRACE_SOFTEN_BLEND_SCALE});
    if (portraitDetailEnabled) {
        float portraitProtection = portraitDetailAmount(centerRgb, maskUv);
        if (portraitProtection > 0.0001) {
            float detailLuma = abs(dot(centerRgb - blurred, vec3(0.299, 0.587, 0.114)));
            float edgeMask = smoothstep(0.016, 0.070, detailLuma);
            float suppressionMix = clamp(
                detailSuppressionAmount * portraitProtection * edgeMask *
                    ${COLOR_CARD_CAMERA_PREVIEW_PORTRAIT_DETAIL_SUPPRESSION_SCALE},
                0.0,
                1.0
            );
            if (suppressionMix > 0.0001) {
                detailAdjusted = mix(
                    detailAdjusted,
                    blurred,
                    suppressionMix * ${COLOR_CARD_CAMERA_PREVIEW_PORTRAIT_DETAIL_BLEND_SCALE}
                );
            }
        }
    }
    return detailAdjusted;
}

vec3 sampleSourceWithDetail(vec2 uv, vec2 maskUv) {
    vec3 centerRgb = texture2D(uTexture, uv).rgb;
    return applySourceDetailAdjust(uv, maskUv, centerRgb);
}

float softRange(
    float value,
    float outerStart,
    float innerStart,
    float innerEnd,
    float outerEnd
) {
    if (value <= outerStart || value >= outerEnd) {
        return 0.0;
    }
    if (value >= innerStart && value <= innerEnd) {
        return 1.0;
    }
    if (value < innerStart) {
        return smoothstep(outerStart, innerStart, value);
    }
    return 1.0 - smoothstep(innerEnd, outerEnd, value);
}

float faceProtectionMask(vec2 uv) {
    float strongest = 0.0;
    for (int i = 0; i < MAX_PROTECTED_FACES; i++) {
        if (i >= uProtectedFaceCount) {
            break;
        }
        vec4 region = uProtectedFaces[i];
        vec2 radii = max(region.zw, vec2(0.01));
        vec2 delta = (uv - region.xy) / radii;
        float distance = length(delta);
        float mask = distance <= 0.46
            ? 1.0
            : (distance >= 1.08 ? 0.0 : (1.0 - smoothstep(0.46, 1.08, distance)));
        strongest = max(strongest, mask);
    }
    return strongest;
}

float skinToneMask(vec3 rgb) {
    float luma = (0.299 * rgb.r) + (0.587 * rgb.g) + (0.114 * rgb.b);
    float cb = 0.5 - (0.168736 * rgb.r) - (0.331264 * rgb.g) + (0.5 * rgb.b);
    float cr = 0.5 + (0.5 * rgb.r) - (0.418688 * rgb.g) - (0.081312 * rgb.b);
    float spread = max(max(rgb.r, rgb.g), rgb.b) - min(min(rgb.r, rgb.g), rgb.b);
    float detailMask = softRange(spread, 0.010, 0.032, 0.82, 0.995);
    float lumaMask = softRange(luma, 0.02, 0.06, 0.96, 0.995);
    float cbMask = softRange(cb, 0.17, 0.23, 0.49, 0.58);
    float crMask = softRange(cr, 0.38, 0.44, 0.74, 0.84);
    float redGreenBias = softRange(rgb.r - rgb.g, -0.10, -0.03, 0.38, 0.62);
    float redBlueBias = softRange(rgb.r - rgb.b, -0.06, 0.0, 0.60, 0.88);
    float chromaMask = clamp((cbMask * 0.48) + (crMask * 0.52), 0.0, 1.0);
    float warmthMask = clamp((redGreenBias * 0.56) + (redBlueBias * 0.44), 0.0, 1.0);
    float confidence = clamp(
        (chromaMask * 0.38) +
            (warmthMask * 0.24) +
            (detailMask * 0.18) +
            (lumaMask * 0.20),
        0.0,
        1.0
    );
    float gate = clamp(
        (min(chromaMask, warmthMask) * 0.42) +
            (max(chromaMask, warmthMask) * 0.58),
        0.0,
        1.0
    );
    return clamp(confidence * (0.58 + (0.42 * gate)), 0.0, 1.0);
}

float skinProtectionAmount(vec3 rgb, vec2 uv) {
    if (uProtectedFaceCount <= 0) {
        return 0.0;
    }
    return clamp(faceProtectionMask(uv) * skinToneMask(rgb), 0.0, 1.0);
}

float portraitDetailAmount(vec3 rgb, vec2 maskUv) {
    if (uProtectedFaceCount <= 0) {
        return 0.0;
    }
    float faceMask = faceProtectionMask(maskUv);
    if (faceMask <= 0.0001) {
        return 0.0;
    }
    float skinMask = skinToneMask(rgb);
    return clamp(faceMask * (0.70 + (0.30 * skinMask)), 0.0, 1.0);
}

float portraitSoftnessAmount(vec3 rgb, vec2 maskUv) {
    if (uProtectedFaceCount <= 0) {
        return 0.0;
    }
    float faceMask = faceProtectionMask(maskUv);
    if (faceMask <= 0.0001) {
        return 0.0;
    }
    float skinMask = skinToneMask(rgb);
    return clamp(faceMask * (0.84 + (0.16 * skinMask)), 0.0, 1.0);
}

vec3 samplePortraitSoftBlur(vec2 uv) {
    vec2 texel = max(uSourceTexelSize, vec2(0.000001));
    vec3 center = texture2D(uTexture, uv).rgb * 3.8;
    vec3 crossNear = (
        texture2D(uTexture, clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb
    ) * 1.32;
    vec3 diagonals = (
        texture2D(uTexture, clamp(uv + vec2(-texel.x, -texel.y), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(texel.x, -texel.y), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(-texel.x, texel.y), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(texel.x, texel.y), vec2(0.0), vec2(1.0))).rgb
    ) * 0.92;
    return clamp((center + crossNear + diagonals) / 12.96, 0.0, 1.0);
}

vec3 applyPortraitSoftness(vec2 uv, vec3 rgb, float portraitMask) {
    if (uPortraitEnabled == 0 || uPortraitSoftenAmount <= 0.0001 || portraitMask <= 0.0001) {
        return rgb;
    }
    float softenMix = clamp(
        uPortraitSoftenAmount * portraitMask *
            ${COLOR_CARD_CAMERA_PREVIEW_PORTRAIT_SOFTNESS_SCALE},
        0.0,
        1.0
    );
    if (softenMix <= 0.0001) {
        return rgb;
    }
    return mix(rgb, samplePortraitSoftBlur(uv), softenMix);
}

float previewDitherNoise(vec2 seed) {
    return fract(sin(dot(seed, vec2(127.1, 311.7))) * 43758.5453123);
}

vec3 applyPreviewDither(vec3 rgb) {
    float noiseA = previewDitherNoise(gl_FragCoord.xy);
    float noiseB = previewDitherNoise(gl_FragCoord.yx + vec2(19.19, 7.73));
    float triangularNoise = (noiseA + noiseB) - 1.0;
    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
    float amplitude =
        mix(
            ${COLOR_CARD_CAMERA_PREVIEW_DITHER_STRENGTH} / 255.0,
            (${COLOR_CARD_CAMERA_PREVIEW_DITHER_STRENGTH} * 0.56) / 255.0,
            smoothstep(0.08, 0.92, luma)
        );
    return clamp(rgb + vec3(triangularNoise * amplitude), 0.0, 1.0);
}

vec3 applyBlackMist(vec2 uv, vec3 rgb) {
    if (uBlackMistEnabled == 0) return rgb;
    float safeStrength = clamp(uBlackMistStrength, 0.0, 1.0);
    vec2 texel = max(uSourceTexelSize, vec2(0.000001));
    float radiusScale = clamp(1.0 + uBlackMistRadiusBias, 0.65, 1.85);
    float haloBoost = clamp(1.0 + uBlackMistHaloBias, 0.35, 1.9);
    float atmosphere = clamp(uBlackMistAtmosphereBias, -0.4, 0.6);
    vec3 blurred =
        sampleSource(clamp(uv + vec2(-texel.x * 2.0 * radiusScale, 0.0), vec2(0.0), vec2(1.0))) * 0.10 +
        sampleSource(clamp(uv + vec2(texel.x * 2.0 * radiusScale, 0.0), vec2(0.0), vec2(1.0))) * 0.10 +
        sampleSource(clamp(uv + vec2(0.0, -texel.y * 2.0 * radiusScale), vec2(0.0), vec2(1.0))) * 0.10 +
        sampleSource(clamp(uv + vec2(0.0, texel.y * 2.0 * radiusScale), vec2(0.0), vec2(1.0))) * 0.10 +
        sampleSource(clamp(uv + vec2(-texel.x * radiusScale, -texel.y * radiusScale), vec2(0.0), vec2(1.0))) * 0.14 +
        sampleSource(clamp(uv + vec2(texel.x * radiusScale, -texel.y * radiusScale), vec2(0.0), vec2(1.0))) * 0.14 +
        sampleSource(clamp(uv + vec2(-texel.x * radiusScale, texel.y * radiusScale), vec2(0.0), vec2(1.0))) * 0.14 +
        sampleSource(clamp(uv + vec2(texel.x * radiusScale, texel.y * radiusScale), vec2(0.0), vec2(1.0))) * 0.14 +
        rgb * 0.14;
    vec3 bloomBlur =
        sampleSource(clamp(uv + vec2(-texel.x * 4.0 * radiusScale, 0.0), vec2(0.0), vec2(1.0))) * 0.12 +
        sampleSource(clamp(uv + vec2(texel.x * 4.0 * radiusScale, 0.0), vec2(0.0), vec2(1.0))) * 0.12 +
        sampleSource(clamp(uv + vec2(0.0, -texel.y * 4.0 * radiusScale), vec2(0.0), vec2(1.0))) * 0.12 +
        sampleSource(clamp(uv + vec2(0.0, texel.y * 4.0 * radiusScale), vec2(0.0), vec2(1.0))) * 0.12 +
        sampleSource(clamp(uv + vec2(-texel.x * 3.0 * radiusScale, -texel.y * 3.0 * radiusScale), vec2(0.0), vec2(1.0))) * 0.13 +
        sampleSource(clamp(uv + vec2(texel.x * 3.0 * radiusScale, -texel.y * 3.0 * radiusScale), vec2(0.0), vec2(1.0))) * 0.13 +
        sampleSource(clamp(uv + vec2(-texel.x * 3.0 * radiusScale, texel.y * 3.0 * radiusScale), vec2(0.0), vec2(1.0))) * 0.13 +
        sampleSource(clamp(uv + vec2(texel.x * 3.0 * radiusScale, texel.y * 3.0 * radiusScale), vec2(0.0), vec2(1.0))) * 0.13;
    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
    float highlightStart = clamp(0.52 + uBlackMistHighlightBias, 0.18, 0.90);
    float highlight = smoothstep(highlightStart, 0.98, luma);
    float highlightShoulder = smoothstep(
        max(0.0, highlightStart - mix(0.12, 0.24, safeStrength) - (atmosphere * 0.10)),
        min(1.0, highlightStart + 0.10 + (atmosphere * 0.12)),
        luma);
    float edgeEnergy = clamp(length(rgb - blurred) * (2.4 + (safeStrength * 1.2)), 0.0, 1.0);
    float softnessMix = clamp(mix(0.10, 0.30, safeStrength) + uBlackMistSoftnessBias, 0.03, 0.60);
    float haloMask = clamp(
        (highlightShoulder * (0.66 + (atmosphere * 0.18))) +
        (highlight * edgeEnergy * (0.38 + (0.22 * haloBoost))),
        0.0,
        1.0);
    float bloomAmount = haloMask * clamp(mix(0.16, 0.42, safeStrength) + uBlackMistBloomBias, 0.0, 0.85);
    float veilLift =
        (1.0 - luma) * (1.0 - (highlightShoulder * (0.84 - (atmosphere * 0.18)))) *
        clamp((0.004 + (safeStrength * 0.010)) + uBlackMistVeilBias + max(0.0, atmosphere) * 0.010, 0.0, 0.06);
    vec3 softened = mix(rgb, blurred, softnessMix);
    vec3 glowBase = mix(
        blurred,
        bloomBlur,
        clamp(0.46 + (highlightShoulder * 0.12) + (uBlackMistRadiusBias * 0.20), 0.28, 0.86));
    vec3 glow = mix(glowBase, uBlackMistTint, 0.12 + (highlightShoulder * 0.18));
    vec3 dreamRgb = clamp(softened + (glow * bloomAmount) + vec3(veilLift), 0.0, 1.0);
    dreamRgb = clamp(((dreamRgb - 0.5) * (1.0 + uBlackMistContrastBias)) + 0.5, 0.0, 1.0);
    return mix(rgb, dreamRgb, 0.60 + (safeStrength * 0.16));
}

vec3 applyDehaze(vec2 uv, vec3 rgb) {
    float strength = clamp(uDehazeStrength, 0.0, 1.0);
    if (strength < 0.001) return rgb;
    vec2 texel = max(uSourceTexelSize, vec2(0.000001));
    vec3 nearBlur =
        sampleSource(clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0))) * 0.16 +
        sampleSource(clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))) * 0.16 +
        sampleSource(clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0))) * 0.16 +
        sampleSource(clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))) * 0.16 +
        rgb * 0.36;
    vec3 farBlur =
        sampleSource(clamp(uv + vec2(-texel.x * 3.0, 0.0), vec2(0.0), vec2(1.0))) * 0.11 +
        sampleSource(clamp(uv + vec2(texel.x * 3.0, 0.0), vec2(0.0), vec2(1.0))) * 0.11 +
        sampleSource(clamp(uv + vec2(0.0, -texel.y * 3.0), vec2(0.0), vec2(1.0))) * 0.11 +
        sampleSource(clamp(uv + vec2(0.0, texel.y * 3.0), vec2(0.0), vec2(1.0))) * 0.11 +
        sampleSource(clamp(uv + vec2(-texel.x * 2.0, -texel.y * 2.0), vec2(0.0), vec2(1.0))) * 0.10 +
        sampleSource(clamp(uv + vec2(texel.x * 2.0, -texel.y * 2.0), vec2(0.0), vec2(1.0))) * 0.10 +
        sampleSource(clamp(uv + vec2(-texel.x * 2.0, texel.y * 2.0), vec2(0.0), vec2(1.0))) * 0.10 +
        sampleSource(clamp(uv + vec2(texel.x * 2.0, texel.y * 2.0), vec2(0.0), vec2(1.0))) * 0.10 +
        rgb * 0.16;
    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
    float hazeEstimate = clamp(dot(farBlur - nearBlur, vec3(0.299, 0.587, 0.114)) + ((1.0 - luma) * 0.08), 0.0, 0.22);
    float localContrast = clamp(length(rgb - nearBlur) * 1.8, 0.0, 1.0);
    float recovery = strength * (0.42 + (0.28 * (1.0 - localContrast)));
    vec3 clarified = clamp(rgb + ((rgb - farBlur) * recovery * 0.92), 0.0, 1.0);
    clarified = clamp(((clarified - 0.5) * (1.0 + (recovery * 0.14))) + 0.5, 0.0, 1.0);
    vec3 antiVeil = mix(vec3(1.0), vec3(0.82, 0.86, 0.92), hazeEstimate * recovery);
    clarified *= antiVeil;
    float darkRecover = hazeEstimate * recovery * 0.18;
    clarified = max(clarified - vec3(darkRecover * 0.25), vec3(0.0));
    return mix(rgb, clarified, strength);
}

float hashColorCardSparkle(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

vec3 applySparkle(vec2 uv, vec3 rgb) {
    float strength = clamp(uSparkleStrength, 0.0, 1.0);
    if (strength < 0.001) return rgb;
    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
    float highlight = smoothstep(0.68, 0.98, luma);
    vec2 grid = vec2(42.0, 74.0);
    vec2 scaledUv = uv * grid;
    vec2 cell = floor(scaledUv);
    vec2 local = fract(scaledUv) - 0.5;
    float seed = hashColorCardSparkle(cell);
    float sparkleGate = step(0.84, seed) * highlight;
    vec2 star = abs(local);
    float cross = exp(-star.x * 36.0) + exp(-star.y * 36.0);
    float diamond = exp(-(star.x + star.y) * 24.0);
    float burst = (cross * 0.55) + (diamond * 0.45);
    float twinkle = 0.82 + (0.18 * sin((uv.x * 93.0) + (uv.y * 57.0) + (seed * 6.2831)));
    float sparkle = sparkleGate * burst * twinkle * strength * 0.22;
    vec3 tint = vec3(1.0, 0.97, 0.92);
    return clamp(rgb + (tint * sparkle), 0.0, 1.0);
}

void main() {
    vec2 uv = clampSourceUv(vTexCoord);
    vec2 maskUv = clampSourceUv(vMaskCoord);
    vec3 sourceRgb = sampleSource(uv);
    vec3 rgb = sampleSourceWithDetail(uv, maskUv);
    bool portraitEnabled = (uPortraitEnabled != 0) && (uProtectedFaceCount > 0);
    float protectionAmount = skinProtectionAmount(sourceRgb, maskUv);
    if (portraitEnabled && uPortraitSoftenAmount > 0.0001) {
        float softnessAmount = portraitSoftnessAmount(sourceRgb, maskUv);
        if (softnessAmount > 0.0001) {
            rgb = applyPortraitSoftness(uv, rgb, softnessAmount);
        }
    }
    float paletteProtectionScale =
        1.0 - (protectionAmount * ${COLOR_CARD_TRACE_SKIN_PROTECTION_PALETTE_RECOVERY});
    float creativeChromaScale =
        1.0 - (protectionAmount * ${COLOR_CARD_TRACE_SKIN_PROTECTION_CREATIVE_RECOVERY});
    float creativeTonalScale =
        1.0 - (protectionAmount * ${COLOR_CARD_TRACE_SKIN_PROTECTION_TONAL_RECOVERY});
    if (uHaldEnabled == 1) {
        vec3 primary = applySingleHaldClut(uHaldTexture, uHaldLevel, rgb);
        vec3 filtered = primary;
        if (uHaldSecondaryLevel > 0.0 && uHaldVariantMix > 0.0001) {
            vec3 secondary = applySingleHaldClut(uHaldTextureSecondary, uHaldSecondaryLevel, rgb);
            filtered = mix(primary, secondary, clamp(uHaldVariantMix, 0.0, 1.0));
        }
        rgb = mix(rgb, filtered, clamp(uHaldStrength * paletteProtectionScale, 0.0, 1.0));
    }
    rgb = applyCreativeAdjustments(
        rgb,
        uColor * creativeChromaScale,
        uTone * creativeTonalScale,
        uSaturation * creativeChromaScale,
        uBrightness * creativeTonalScale
    );
    if (
        portraitEnabled &&
        protectionAmount > 0.0001 &&
        (
            uPortraitLightnessLift > 0.0001 ||
                uPortraitSaturationBoost > 0.0001 ||
                uPortraitSoftenAmount > 0.0001 ||
                uPortraitSharpenRetention < 0.9999
            )
    ) {
        rgb = applyPortraitAdjustments(rgb, protectionAmount);
    }
    rgb = applyDehaze(uv, rgb);
    rgb = applyBlackMist(uv, rgb);
    rgb = applySparkle(uv, rgb);
    rgb = applyPreviewDither(rgb);
    gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), 1.0);
}
""".trimIndent()
