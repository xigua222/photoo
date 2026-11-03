package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.view.TextureView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal data class ColorCardTraceGpuParams(
    val filterEnabled: Boolean,
    val anchorCount: Int,
    val sourceAnchors: FloatArray,
    val targetAnchors: FloatArray,
    val blendStrength: Float,
    val maxLightnessShift: Float,
    val maxChromaShift: Float,
    val color: Float,
    val colorAxisA: Float,
    val colorAxisB: Float,
    val sharpen: Float,
    val tone: Float,
    val saturation: Float,
    val brightness: Float,
    val protectedFaceCount: Int,
    val protectedFaces: FloatArray,
    val portraitEnabled: Boolean,
    val portraitLightnessLift: Float,
    val portraitSaturationBoost: Float,
    val portraitSharpenRetention: Float,
    val portraitSoftenAmount: Float,
    val haldEnabled: Boolean,
    val haldLevel: Int,
    val haldStrength: Float,
    val haldBitmap: Bitmap?,
    val haldSecondaryLevel: Int,
    val haldSecondaryBitmap: Bitmap?,
    val haldVariantMix: Float,
    val pixelPosterEnabled: Boolean,
    val pixelPosterUseSourceColor: Boolean,
    val pixelPosterContinuousTone: Boolean = false,
    val pixelPosterBlockCountLongSide: Float,
    val pixelPosterThreshold: Float,
    val pixelPosterContrast: Float,
    val pixelPosterNoiseAmount: Float,
    val pixelPosterGridLineWidth: Float,
    val pixelPosterGridLineOpacity: Float,
    val pixelPosterDetailBoost: Float,
    val pixelPosterShadowDetailBias: Float,
    val pixelPosterStrength: Float,
    val pixelPosterDarkRed: Float,
    val pixelPosterDarkGreen: Float,
    val pixelPosterDarkBlue: Float,
    val pixelPosterLightRed: Float,
    val pixelPosterLightGreen: Float,
    val pixelPosterLightBlue: Float,
    val subjectFrameEnabled: Boolean,
    val subjectFrameRect: FloatArray,
    val subjectFrameBorderThickness: Float,
    val subjectFrameStrength: Float,
    val subjectFrameBorderRed: Float,
    val subjectFrameBorderGreen: Float,
    val subjectFrameBorderBlue: Float,
    val blackMistEnabled: Boolean,
    val blackMistStrength: Float,
    val sparkleStrength: Float = 0f,
    val dehazeStrength: Float = 0f,
    val blackMistSoftnessBias: Float = 0f,
    val blackMistBloomBias: Float = 0f,
    val blackMistVeilBias: Float = 0f,
    val blackMistHighlightBias: Float = 0f,
    val blackMistContrastBias: Float = 0f,
    val blackMistRadiusBias: Float = 0f,
    val blackMistHaloBias: Float = 0f,
    val blackMistAtmosphereBias: Float = 0f,
    val blackMistTintRed: Float = 0.965f,
    val blackMistTintGreen: Float = 0.851f,
    val blackMistTintBlue: Float = 0.933f,
    val selectiveColorEnabled: Boolean = false,
    val selectiveColorTargetHue: Float = 0f,
    val selectiveColorHueRange: Float = 0f,
    val selectiveColorSaturationFloor: Float = 0f,
    val selectiveColorStrength: Float = 0f,
    val sourceAspect: Float,
    val sourceTexelWidth: Float,
    val sourceTexelHeight: Float
)

private val DEFAULT_COLOR_CARD_TRACE_GPU_PARAMS = ColorCardTraceGpuParams(
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
    pixelPosterContinuousTone = false,
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
    sparkleStrength = 0f,
    dehazeStrength = 0f,
    blackMistSoftnessBias = 0f,
    blackMistBloomBias = 0f,
    blackMistVeilBias = 0f,
    blackMistHighlightBias = 0f,
    blackMistContrastBias = 0f,
    blackMistTintRed = 0.965f,
    blackMistTintGreen = 0.851f,
    blackMistTintBlue = 0.933f,
    selectiveColorEnabled = false,
    selectiveColorTargetHue = 0f,
    selectiveColorHueRange = 0f,
    selectiveColorSaturationFloor = 0f,
    selectiveColorStrength = 0f,
    sourceAspect = 1f,
    sourceTexelWidth = 1f,
    sourceTexelHeight = 1f
)

@Composable
internal fun ColorCardTraceGpuPreview(
    sourceBitmap: Bitmap?,
    params: ColorCardTraceGpuParams,
    revealProgress: Float,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            ColorCardTraceGpuPreviewView(context)
        },
        update = { view ->
            view.setSourceBitmap(sourceBitmap)
            view.setParams(params)
            view.setRevealProgress(revealProgress)
        },
        modifier = modifier
    )
}

internal suspend fun renderColorCardTraceGpuBitmap(
    sourceBitmap: Bitmap,
    params: ColorCardTraceGpuParams
): Bitmap? = withContext(Dispatchers.Default) {
    if (sourceBitmap.isRecycled) return@withContext null
    ColorCardTraceGpuOffscreenRenderer().use { renderer ->
        renderer.render(sourceBitmap, params)
    }
}

private class ColorCardTraceGpuOffscreenRenderer : AutoCloseable {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    private var programId = 0
    private var textureId = 0
    private var haldTextureId = 0
    private var haldSecondaryTextureId = 0

    private var positionLocation = -1
    private var texCoordLocation = -1
    private var textureUniformLocation = -1
    private var haldTextureUniformLocation = -1
    private var haldSecondaryTextureUniformLocation = -1
    private var filterEnabledLocation = -1
    private var anchorCountLocation = -1
    private var sourceAnchorsLocation = -1
    private var targetAnchorsLocation = -1
    private var blendStrengthLocation = -1
    private var maxLightnessShiftLocation = -1
    private var maxChromaShiftLocation = -1
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
    private var haldLevelLocation = -1
    private var haldSecondaryLevelLocation = -1
    private var haldStrengthLocation = -1
    private var haldVariantMixLocation = -1
    private var pixelPosterEnabledLocation = -1
    private var pixelPosterUseSourceColorLocation = -1
    private var pixelPosterContinuousToneLocation = -1
    private var pixelPosterBlockCountLongSideLocation = -1
    private var pixelPosterThresholdLocation = -1
    private var pixelPosterContrastLocation = -1
    private var pixelPosterNoiseAmountLocation = -1
    private var pixelPosterGridLineWidthLocation = -1
    private var pixelPosterGridLineOpacityLocation = -1
    private var pixelPosterDetailBoostLocation = -1
    private var pixelPosterShadowDetailBiasLocation = -1
    private var pixelPosterStrengthLocation = -1
    private var pixelPosterDarkColorLocation = -1
    private var pixelPosterLightColorLocation = -1
    private var subjectFrameEnabledLocation = -1
    private var subjectFrameRectLocation = -1
    private var subjectFrameBorderThicknessLocation = -1
    private var subjectFrameStrengthLocation = -1
    private var subjectFrameBorderColorLocation = -1
    private var blackMistEnabledLocation = -1
    private var blackMistStrengthLocation = -1
    private var selectiveColorEnabledLocation = -1
    private var selectiveColorTargetHueLocation = -1
    private var selectiveColorHueRangeLocation = -1
    private var selectiveColorSaturationFloorLocation = -1
    private var selectiveColorStrengthLocation = -1
    private var sourceAspectLocation = -1
    private var sourceTexelSizeLocation = -1

    fun render(
        sourceBitmap: Bitmap,
        params: ColorCardTraceGpuParams
    ): Bitmap? {
        val width = sourceBitmap.width.coerceAtLeast(1)
        val height = sourceBitmap.height.coerceAtLeast(1)
        ensureGlReady(width, height, params)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        ensureTexture()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, sourceBitmap, 0)

        if (params.haldEnabled && params.haldBitmap != null) {
            ensureHaldTexture()
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldTextureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, params.haldBitmap, 0)
        }
        if (params.haldEnabled && params.haldSecondaryBitmap != null) {
            ensureSecondaryHaldTexture()
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldSecondaryTextureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, params.haldSecondaryBitmap, 0)
        }

        GLES20.glUseProgram(programId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniformLocation, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            if (params.haldEnabled && params.haldBitmap != null) haldTextureId else 0
        )
        GLES20.glUniform1i(haldTextureUniformLocation, 1)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            if (params.haldEnabled && params.haldSecondaryBitmap != null) haldSecondaryTextureId else 0
        )
        GLES20.glUniform1i(haldSecondaryTextureUniformLocation, 2)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        applyParams(params)

        FULLSCREEN_QUAD.position(0)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            FULLSCREEN_VERTEX_STRIDE_BYTES,
            FULLSCREEN_QUAD
        )
        FULLSCREEN_QUAD.position(2)
        GLES20.glEnableVertexAttribArray(texCoordLocation)
        GLES20.glVertexAttribPointer(
            texCoordLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            FULLSCREEN_VERTEX_STRIDE_BYTES,
            FULLSCREEN_QUAD
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glDisableVertexAttribArray(texCoordLocation)

        val pixelBuffer = IntBuffer.allocate(width * height)
        GLES20.glReadPixels(
            0,
            0,
            width,
            height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            pixelBuffer
        )
        val rgbaPixels = pixelBuffer.array()
        val argbPixels = IntArray(width * height)
        for (y in 0 until height) {
            val srcRowOffset = y * width
            val dstRowOffset = (height - 1 - y) * width
            for (x in 0 until width) {
                val pixel = rgbaPixels[srcRowOffset + x]
                val red = pixel and 0xFF
                val green = (pixel ushr 8) and 0xFF
                val blue = (pixel ushr 16) and 0xFF
                val alpha = (pixel ushr 24) and 0xFF
                argbPixels[dstRowOffset + x] =
                    (alpha shl 24) or
                        (red shl 16) or
                        (green shl 8) or
                        blue
            }
        }
        return Bitmap.createBitmap(argbPixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyParams(params: ColorCardTraceGpuParams) {
        GLES20.glUniform1i(filterEnabledLocation, if (params.filterEnabled) 1 else 0)
        GLES20.glUniform1i(anchorCountLocation, params.anchorCount.coerceIn(0, COLOR_CARD_TRACE_FILTER_MAX_ANCHORS))
        GLES20.glUniform3fv(sourceAnchorsLocation, COLOR_CARD_TRACE_FILTER_MAX_ANCHORS, params.sourceAnchors, 0)
        GLES20.glUniform3fv(targetAnchorsLocation, COLOR_CARD_TRACE_FILTER_MAX_ANCHORS, params.targetAnchors, 0)
        GLES20.glUniform1f(blendStrengthLocation, params.blendStrength.coerceIn(0f, 1f))
        GLES20.glUniform1f(maxLightnessShiftLocation, params.maxLightnessShift.coerceAtLeast(0f))
        GLES20.glUniform1f(maxChromaShiftLocation, params.maxChromaShift.coerceAtLeast(0f))
        GLES20.glUniform1f(colorLocation, params.color.coerceIn(-1f, 1f))
        GLES20.glUniform2f(colorAxisLocation, params.colorAxisA.coerceIn(-1f, 1f), params.colorAxisB.coerceIn(-1f, 1f))
        GLES20.glUniform1f(sharpenLocation, params.sharpen.coerceIn(-1f, 1f))
        GLES20.glUniform1f(toneLocation, params.tone.coerceIn(-1f, 1f))
        GLES20.glUniform1f(saturationLocation, params.saturation.coerceIn(-1f, 1f))
        GLES20.glUniform1f(brightnessLocation, params.brightness.coerceIn(-1f, 1f))
        GLES20.glUniform1i(protectedFaceCountLocation, params.protectedFaceCount.coerceIn(0, COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES))
        GLES20.glUniform4fv(protectedFacesLocation, COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES, params.protectedFaces, 0)
        GLES20.glUniform1i(portraitEnabledLocation, if (params.portraitEnabled) 1 else 0)
        GLES20.glUniform1f(portraitLightnessLiftLocation, params.portraitLightnessLift.coerceIn(0f, 12f))
        GLES20.glUniform1f(portraitSaturationBoostLocation, params.portraitSaturationBoost.coerceIn(0f, 0.3f))
        GLES20.glUniform1f(portraitSharpenRetentionLocation, params.portraitSharpenRetention.coerceIn(0f, 1f))
        GLES20.glUniform1f(portraitSoftenAmountLocation, params.portraitSoftenAmount.coerceIn(0f, 1f))
        GLES20.glUniform1i(haldEnabledLocation, if (params.haldEnabled && params.haldBitmap != null) 1 else 0)
        GLES20.glUniform1f(haldLevelLocation, params.haldLevel.toFloat().coerceAtLeast(0f))
        GLES20.glUniform1f(haldSecondaryLevelLocation, params.haldSecondaryLevel.toFloat().coerceAtLeast(0f))
        GLES20.glUniform1f(haldStrengthLocation, params.haldStrength.coerceIn(0f, 1f))
        GLES20.glUniform1f(haldVariantMixLocation, params.haldVariantMix.coerceIn(0f, 1f))
        GLES20.glUniform1i(pixelPosterEnabledLocation, if (params.pixelPosterEnabled) 1 else 0)
        GLES20.glUniform1i(pixelPosterUseSourceColorLocation, if (params.pixelPosterUseSourceColor) 1 else 0)
        GLES20.glUniform1i(pixelPosterContinuousToneLocation, if (params.pixelPosterContinuousTone) 1 else 0)
        GLES20.glUniform1f(pixelPosterBlockCountLongSideLocation, params.pixelPosterBlockCountLongSide.coerceAtLeast(0f))
        GLES20.glUniform1f(pixelPosterThresholdLocation, params.pixelPosterThreshold.coerceIn(0f, 1f))
        GLES20.glUniform1f(pixelPosterContrastLocation, params.pixelPosterContrast.coerceAtLeast(0f))
        GLES20.glUniform1f(pixelPosterNoiseAmountLocation, params.pixelPosterNoiseAmount.coerceIn(0f, 1f))
        GLES20.glUniform1f(pixelPosterGridLineWidthLocation, params.pixelPosterGridLineWidth.coerceIn(0f, 0.45f))
        GLES20.glUniform1f(pixelPosterGridLineOpacityLocation, params.pixelPosterGridLineOpacity.coerceIn(0f, 1f))
        GLES20.glUniform1f(pixelPosterDetailBoostLocation, params.pixelPosterDetailBoost.coerceIn(0f, 1f))
        GLES20.glUniform1f(pixelPosterShadowDetailBiasLocation, params.pixelPosterShadowDetailBias.coerceIn(0f, 1f))
        GLES20.glUniform1f(pixelPosterStrengthLocation, params.pixelPosterStrength.coerceIn(0f, 1f))
        GLES20.glUniform3f(pixelPosterDarkColorLocation, params.pixelPosterDarkRed.coerceIn(0f, 1f), params.pixelPosterDarkGreen.coerceIn(0f, 1f), params.pixelPosterDarkBlue.coerceIn(0f, 1f))
        GLES20.glUniform3f(pixelPosterLightColorLocation, params.pixelPosterLightRed.coerceIn(0f, 1f), params.pixelPosterLightGreen.coerceIn(0f, 1f), params.pixelPosterLightBlue.coerceIn(0f, 1f))
        GLES20.glUniform1i(subjectFrameEnabledLocation, if (params.subjectFrameEnabled) 1 else 0)
        GLES20.glUniform4fv(subjectFrameRectLocation, 1, params.subjectFrameRect, 0)
        GLES20.glUniform1f(subjectFrameBorderThicknessLocation, params.subjectFrameBorderThickness.coerceAtLeast(0f))
        GLES20.glUniform1f(subjectFrameStrengthLocation, params.subjectFrameStrength.coerceIn(0f, 1f))
        GLES20.glUniform3f(subjectFrameBorderColorLocation, params.subjectFrameBorderRed.coerceIn(0f, 1f), params.subjectFrameBorderGreen.coerceIn(0f, 1f), params.subjectFrameBorderBlue.coerceIn(0f, 1f))
        GLES20.glUniform1i(blackMistEnabledLocation, if (params.blackMistEnabled) 1 else 0)
        GLES20.glUniform1f(blackMistStrengthLocation, params.blackMistStrength.coerceIn(0f, 1f))
        GLES20.glUniform1i(selectiveColorEnabledLocation, if (params.selectiveColorEnabled) 1 else 0)
        GLES20.glUniform1f(selectiveColorTargetHueLocation, params.selectiveColorTargetHue.coerceIn(0f, 1f))
        GLES20.glUniform1f(selectiveColorHueRangeLocation, params.selectiveColorHueRange.coerceIn(0f, 0.5f))
        GLES20.glUniform1f(selectiveColorSaturationFloorLocation, params.selectiveColorSaturationFloor.coerceIn(0f, 1f))
        GLES20.glUniform1f(selectiveColorStrengthLocation, params.selectiveColorStrength.coerceIn(0f, 1f))
        GLES20.glUniform1f(sourceAspectLocation, params.sourceAspect.coerceAtLeast(0.0001f))
        GLES20.glUniform2f(sourceTexelSizeLocation, params.sourceTexelWidth.coerceAtLeast(0.000001f), params.sourceTexelHeight.coerceAtLeast(0.000001f))
    }

    private fun ensureGlReady(width: Int, height: Int, params: ColorCardTraceGpuParams) {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(eglDisplay !== EGL14.EGL_NO_DISPLAY) { "Unable to obtain EGL display" }
            val version = IntArray(2)
            check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "Unable to initialize EGL" }
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
            )
            check(EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, configCount, 0) && configCount[0] > 0) {
                "Unable to choose EGL config"
            }
            eglConfig = configs[0]
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0
            )
            check(eglContext !== EGL14.EGL_NO_CONTEXT) { "Unable to create EGL context" }
        }

        if (eglSurface === EGL14.EGL_NO_SURFACE) {
            eglSurface = EGL14.eglCreatePbufferSurface(
                eglDisplay,
                eglConfig,
                intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE),
                0
            )
            check(eglSurface !== EGL14.EGL_NO_SURFACE) { "Unable to create EGL pbuffer surface" }
        }

        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            "Unable to make EGL context current"
        }

        if (programId == 0) {
            val shaderFeatures = resolveColorCardTraceGpuShaderFeatures(params)
            val featureShaderSource = buildColorCardTraceFragmentShader(shaderFeatures)
            programId = try {
                createProgram(
                    COLOR_CARD_TRACE_VERTEX_SHADER,
                    featureShaderSource
                )
            } catch (throwable: IllegalStateException) {
                Log.w(
                    "ColorCardTraceGpu",
                    "Feature shader compile failed in offscreen renderer; falling back to legacy shader. features=$shaderFeatures",
                    throwable
                )
                createProgram(
                    COLOR_CARD_TRACE_VERTEX_SHADER,
                    buildLegacyColorCardTraceFragmentShader()
                )
            }
            positionLocation = GLES20.glGetAttribLocation(programId, "aPosition")
            texCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord")
            textureUniformLocation = GLES20.glGetUniformLocation(programId, "uTexture")
            haldTextureUniformLocation = GLES20.glGetUniformLocation(programId, "uHaldTexture")
            haldSecondaryTextureUniformLocation = GLES20.glGetUniformLocation(programId, "uHaldTextureSecondary")
            filterEnabledLocation = GLES20.glGetUniformLocation(programId, "uFilterEnabled")
            anchorCountLocation = GLES20.glGetUniformLocation(programId, "uAnchorCount")
            sourceAnchorsLocation = GLES20.glGetUniformLocation(programId, "uSourceAnchors")
            targetAnchorsLocation = GLES20.glGetUniformLocation(programId, "uTargetAnchors")
            blendStrengthLocation = GLES20.glGetUniformLocation(programId, "uBlendStrength")
            maxLightnessShiftLocation = GLES20.glGetUniformLocation(programId, "uMaxLightnessShift")
            maxChromaShiftLocation = GLES20.glGetUniformLocation(programId, "uMaxChromaShift")
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
            haldLevelLocation = GLES20.glGetUniformLocation(programId, "uHaldLevel")
            haldSecondaryLevelLocation = GLES20.glGetUniformLocation(programId, "uHaldSecondaryLevel")
            haldStrengthLocation = GLES20.glGetUniformLocation(programId, "uHaldStrength")
            haldVariantMixLocation = GLES20.glGetUniformLocation(programId, "uHaldVariantMix")
            pixelPosterEnabledLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterEnabled")
            pixelPosterUseSourceColorLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterUseSourceColor")
            pixelPosterContinuousToneLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterContinuousTone")
            pixelPosterBlockCountLongSideLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterBlockCountLongSide")
            pixelPosterThresholdLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterThreshold")
            pixelPosterContrastLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterContrast")
            pixelPosterNoiseAmountLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterNoiseAmount")
            pixelPosterGridLineWidthLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterGridLineWidth")
            pixelPosterGridLineOpacityLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterGridLineOpacity")
            pixelPosterDetailBoostLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterDetailBoost")
            pixelPosterShadowDetailBiasLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterShadowDetailBias")
            pixelPosterStrengthLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterStrength")
            pixelPosterDarkColorLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterDarkColor")
            pixelPosterLightColorLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterLightColor")
            subjectFrameEnabledLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameEnabled")
            subjectFrameRectLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameRect")
            subjectFrameBorderThicknessLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameBorderThickness")
            subjectFrameStrengthLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameStrength")
            subjectFrameBorderColorLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameBorderColor")
            blackMistEnabledLocation = GLES20.glGetUniformLocation(programId, "uBlackMistEnabled")
            blackMistStrengthLocation = GLES20.glGetUniformLocation(programId, "uBlackMistStrength")
            selectiveColorEnabledLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorEnabled")
            selectiveColorTargetHueLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorTargetHue")
            selectiveColorHueRangeLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorHueRange")
            selectiveColorSaturationFloorLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorSaturationFloor")
            selectiveColorStrengthLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorStrength")
            sourceAspectLocation = GLES20.glGetUniformLocation(programId, "uSourceAspect")
            sourceTexelSizeLocation = GLES20.glGetUniformLocation(programId, "uSourceTexelSize")
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun ensureTexture() {
        if (textureId == 0) {
            textureId = createTexture(GLES20.GL_LINEAR)
        }
    }

    private fun ensureHaldTexture() {
        if (haldTextureId == 0) {
            haldTextureId = createTexture(GLES20.GL_NEAREST)
        }
    }

    private fun ensureSecondaryHaldTexture() {
        if (haldSecondaryTextureId == 0) {
            haldSecondaryTextureId = createTexture(GLES20.GL_NEAREST)
        }
    }

    private fun createTexture(filter: Int): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val id = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return id
    }

    private fun createProgram(
        vertexShaderSource: String,
        fragmentShaderSource: String
    ): Int {
        val vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        return GLES20.glCreateProgram().also { program ->
            check(program != 0) { "Unable to create GL program" }
            GLES20.glAttachShader(program, vertexShaderId)
            GLES20.glAttachShader(program, fragmentShaderId)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val infoLog = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                error("Unable to link GL program: $infoLog")
            }
            GLES20.glDeleteShader(vertexShaderId)
            GLES20.glDeleteShader(fragmentShaderId)
        }
    }

    private fun compileShader(
        type: Int,
        source: String
    ): Int {
        return GLES20.glCreateShader(type).also { shader ->
            check(shader != 0) { "Unable to create GL shader" }
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val infoLog = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("Unable to compile GL shader: $infoLog")
            }
        }
    }

    override fun close() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY && eglSurface !== EGL14.EGL_NO_SURFACE && eglContext !== EGL14.EGL_NO_CONTEXT) {
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        }
        if (textureId != 0) GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        if (haldTextureId != 0) GLES20.glDeleteTextures(1, intArrayOf(haldTextureId), 0)
        if (haldSecondaryTextureId != 0) GLES20.glDeleteTextures(1, intArrayOf(haldSecondaryTextureId), 0)
        if (programId != 0) GLES20.glDeleteProgram(programId)
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface !== EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
            }
            if (eglContext !== EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
            }
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }
}

private class ColorCardTraceGpuPreviewView(
    context: Context
) : FrameLayout(context), TextureView.SurfaceTextureListener {

    private val textureView = TextureView(context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        isOpaque = false
        surfaceTextureListener = this@ColorCardTraceGpuPreviewView
    }
    private var renderer: ColorCardTraceGpuRenderer? = null
    private var revealProgress = 0f
    private var currentBitmap: Bitmap? = null
    private var currentParams: ColorCardTraceGpuParams = DEFAULT_COLOR_CARD_TRACE_GPU_PARAMS

    init {
        clipChildren = true
        clipToPadding = true
        addView(textureView)
        ensureRenderer()
    }

    fun setSourceBitmap(bitmap: Bitmap?) {
        if (currentBitmap === bitmap) return
        currentBitmap = bitmap
        ensureRenderer()
        renderer?.setSourceBitmap(bitmap)
    }

    fun setParams(params: ColorCardTraceGpuParams) {
        if (
            currentParams.filterEnabled == params.filterEnabled &&
            currentParams.anchorCount == params.anchorCount &&
            currentParams.blendStrength == params.blendStrength &&
            currentParams.maxLightnessShift == params.maxLightnessShift &&
            currentParams.maxChromaShift == params.maxChromaShift &&
            currentParams.color == params.color &&
            currentParams.colorAxisA == params.colorAxisA &&
            currentParams.colorAxisB == params.colorAxisB &&
            currentParams.sharpen == params.sharpen &&
            currentParams.tone == params.tone &&
            currentParams.saturation == params.saturation &&
            currentParams.brightness == params.brightness &&
            currentParams.protectedFaceCount == params.protectedFaceCount &&
            currentParams.portraitEnabled == params.portraitEnabled &&
            currentParams.portraitLightnessLift == params.portraitLightnessLift &&
            currentParams.portraitSaturationBoost == params.portraitSaturationBoost &&
            currentParams.portraitSharpenRetention == params.portraitSharpenRetention &&
            currentParams.portraitSoftenAmount == params.portraitSoftenAmount &&
            currentParams.haldEnabled == params.haldEnabled &&
            currentParams.haldLevel == params.haldLevel &&
            currentParams.haldStrength == params.haldStrength &&
            currentParams.haldSecondaryLevel == params.haldSecondaryLevel &&
            currentParams.haldVariantMix == params.haldVariantMix &&
            currentParams.pixelPosterEnabled == params.pixelPosterEnabled &&
            currentParams.pixelPosterUseSourceColor == params.pixelPosterUseSourceColor &&
            currentParams.pixelPosterContinuousTone == params.pixelPosterContinuousTone &&
            currentParams.pixelPosterBlockCountLongSide == params.pixelPosterBlockCountLongSide &&
            currentParams.pixelPosterThreshold == params.pixelPosterThreshold &&
            currentParams.pixelPosterContrast == params.pixelPosterContrast &&
            currentParams.pixelPosterNoiseAmount == params.pixelPosterNoiseAmount &&
            currentParams.pixelPosterStrength == params.pixelPosterStrength &&
            currentParams.pixelPosterDarkRed == params.pixelPosterDarkRed &&
            currentParams.pixelPosterDarkGreen == params.pixelPosterDarkGreen &&
            currentParams.pixelPosterDarkBlue == params.pixelPosterDarkBlue &&
            currentParams.pixelPosterLightRed == params.pixelPosterLightRed &&
            currentParams.pixelPosterLightGreen == params.pixelPosterLightGreen &&
            currentParams.pixelPosterLightBlue == params.pixelPosterLightBlue &&
            currentParams.subjectFrameEnabled == params.subjectFrameEnabled &&
            currentParams.subjectFrameBorderThickness == params.subjectFrameBorderThickness &&
            currentParams.subjectFrameStrength == params.subjectFrameStrength &&
            currentParams.subjectFrameBorderRed == params.subjectFrameBorderRed &&
            currentParams.subjectFrameBorderGreen == params.subjectFrameBorderGreen &&
            currentParams.subjectFrameBorderBlue == params.subjectFrameBorderBlue &&
            currentParams.blackMistEnabled == params.blackMistEnabled &&
            currentParams.blackMistStrength == params.blackMistStrength &&
            currentParams.selectiveColorEnabled == params.selectiveColorEnabled &&
            currentParams.selectiveColorTargetHue == params.selectiveColorTargetHue &&
            currentParams.selectiveColorHueRange == params.selectiveColorHueRange &&
            currentParams.selectiveColorSaturationFloor == params.selectiveColorSaturationFloor &&
            currentParams.selectiveColorStrength == params.selectiveColorStrength &&
            currentParams.sourceAspect == params.sourceAspect &&
            currentParams.sourceTexelWidth == params.sourceTexelWidth &&
            currentParams.sourceTexelHeight == params.sourceTexelHeight &&
            currentParams.haldBitmap === params.haldBitmap &&
            currentParams.haldSecondaryBitmap === params.haldSecondaryBitmap &&
            currentParams.subjectFrameRect.contentEquals(params.subjectFrameRect) &&
            currentParams.protectedFaces.contentEquals(params.protectedFaces) &&
            currentParams.sourceAnchors.contentEquals(params.sourceAnchors) &&
            currentParams.targetAnchors.contentEquals(params.targetAnchors)
        ) {
            return
        }
        currentParams = params
        ensureRenderer()
        renderer?.setParams(params)
    }

    fun setRevealProgress(progress: Float) {
        val safeProgress = progress.coerceIn(0f, 1f)
        if (safeProgress == revealProgress) return
        revealProgress = safeProgress
        updateRevealClipBounds()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateRevealClipBounds()
    }

    override fun onDetachedFromWindow() {
        renderer?.release()
        renderer = null
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureRenderer()
        textureView.surfaceTexture?.let { surface ->
            renderer?.attachSurface(
                surface = surface,
                width = textureView.width.coerceAtLeast(1),
                height = textureView.height.coerceAtLeast(1)
            )
        }
        renderer?.requestRender()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility != View.VISIBLE) return
        ensureRenderer()
        textureView.surfaceTexture?.let { surface ->
            renderer?.attachSurface(
                surface = surface,
                width = textureView.width.coerceAtLeast(1),
                height = textureView.height.coerceAtLeast(1)
            )
        }
        renderer?.requestRender()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) return
        ensureRenderer()
        textureView.surfaceTexture?.let { surface ->
            renderer?.attachSurface(
                surface = surface,
                width = textureView.width.coerceAtLeast(1),
                height = textureView.height.coerceAtLeast(1)
            )
        }
        renderer?.requestRender()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        ensureRenderer()
        renderer?.attachSurface(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer?.resizeSurface(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderer?.detachSurface()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun updateRevealClipBounds() {
        // TextureView clipping has proven fragile here and can hide the whole realtime
        // preview while the original image underneath remains visible. Keep the reveal
        // band in Compose, but avoid view-level clipping on the preview surface itself.
        clipBounds = null
        textureView.clipBounds = null
    }

    private fun ensureRenderer() {
        if (renderer != null) return
        renderer = ColorCardTraceGpuRenderer().also { created ->
            created.setSourceBitmap(currentBitmap)
            created.setParams(currentParams)
        }
    }
}

private class ColorCardTraceGpuRenderer {

    private val handlerThread = HandlerThread("ColorCardTraceGpuRenderer").apply { start() }
    private val renderHandler = Handler(handlerThread.looper)
    private val stateLock = Any()
    private val renderRunnable = Runnable { renderFrame() }

    @Volatile
    private var released = false

    @Volatile
    private var failed = false
    private var surfaceTexture: SurfaceTexture? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var pendingBitmap: Bitmap? = null
    private var pendingParams: ColorCardTraceGpuParams = DEFAULT_COLOR_CARD_TRACE_GPU_PARAMS
    private var uploadedBitmap: Bitmap? = null
    private var uploadedHaldBitmap: Bitmap? = null
    private var uploadedSecondaryHaldBitmap: Bitmap? = null

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    private var programId = 0
    private var programShaderFeatures: ColorCardTraceGpuShaderFeatures? = null
    private var programUsesLegacyShader = false
    private var textureId = 0
    private var haldTextureId = 0
    private var haldSecondaryTextureId = 0

    private var positionLocation = -1
    private var texCoordLocation = -1
    private var textureUniformLocation = -1
    private var haldTextureUniformLocation = -1
    private var haldSecondaryTextureUniformLocation = -1
    private var filterEnabledLocation = -1
    private var anchorCountLocation = -1
    private var sourceAnchorsLocation = -1
    private var targetAnchorsLocation = -1
    private var blendStrengthLocation = -1
    private var maxLightnessShiftLocation = -1
    private var maxChromaShiftLocation = -1
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
    private var haldLevelLocation = -1
    private var haldSecondaryLevelLocation = -1
    private var haldStrengthLocation = -1
    private var haldVariantMixLocation = -1
    private var pixelPosterEnabledLocation = -1
    private var pixelPosterUseSourceColorLocation = -1
    private var pixelPosterContinuousToneLocation = -1
    private var pixelPosterBlockCountLongSideLocation = -1
    private var pixelPosterThresholdLocation = -1
    private var pixelPosterContrastLocation = -1
    private var pixelPosterNoiseAmountLocation = -1
    private var pixelPosterStrengthLocation = -1
    private var pixelPosterDarkColorLocation = -1
    private var pixelPosterLightColorLocation = -1
    private var subjectFrameEnabledLocation = -1
    private var subjectFrameRectLocation = -1
    private var subjectFrameBorderThicknessLocation = -1
    private var subjectFrameStrengthLocation = -1
    private var subjectFrameBorderColorLocation = -1
    private var blackMistEnabledLocation = -1
    private var blackMistStrengthLocation = -1
    private var selectiveColorEnabledLocation = -1
    private var selectiveColorTargetHueLocation = -1
    private var selectiveColorHueRangeLocation = -1
    private var selectiveColorSaturationFloorLocation = -1
    private var selectiveColorStrengthLocation = -1
    private var sourceAspectLocation = -1
    private var sourceTexelSizeLocation = -1

    private fun debugLog(index: String, message: String) {
        if (!BuildConfig.DEBUG) return
        Log.d("ColorCardTraceGpuDbg", "[$index] $message")
    }

    private fun summarizeBitmap(bitmap: Bitmap?): String {
        return when {
            bitmap == null -> "bitmap=null"
            bitmap.isRecycled -> "bitmap=recycled"
            else -> "bitmap=${bitmap.width}x${bitmap.height}"
        }
    }

    private fun summarizeParams(params: ColorCardTraceGpuParams): String {
        return buildString {
            append("filter=").append(params.filterEnabled)
            append(", anchors=").append(params.anchorCount)
            append(", hald=").append(params.haldEnabled && params.haldBitmap != null)
            append(", pixel=").append(params.pixelPosterEnabled)
            append(", subject=").append(params.subjectFrameEnabled)
            append(", mist=").append(params.blackMistEnabled)
            append(", selective=").append(params.selectiveColorEnabled)
            append(", color=").append(params.color)
            append(", axis=(").append(params.colorAxisA).append(", ").append(params.colorAxisB).append(')')
            append(", sharpen=").append(params.sharpen)
            append(", tone=").append(params.tone)
            append(", sat=").append(params.saturation)
            append(", bright=").append(params.brightness)
            append(", faces=").append(params.protectedFaceCount)
            append(", sourceAspect=").append(params.sourceAspect)
        }
    }

    fun setSourceBitmap(bitmap: Bitmap?) {
        synchronized(stateLock) {
            pendingBitmap = bitmap
        }
        debugLog("D01", "setSourceBitmap ${summarizeBitmap(bitmap)}")
        failed = false
        scheduleRender()
    }

    fun setParams(params: ColorCardTraceGpuParams) {
        synchronized(stateLock) {
            pendingParams = params.copy(
                sourceAnchors = params.sourceAnchors.copyOf(),
                targetAnchors = params.targetAnchors.copyOf(),
                protectedFaces = params.protectedFaces.copyOf(),
                subjectFrameRect = params.subjectFrameRect.copyOf()
            )
        }
        debugLog("D02", "setParams ${summarizeParams(params)}")
        failed = false
        scheduleRender()
    }

    fun attachSurface(surface: SurfaceTexture, width: Int, height: Int) {
        if (released) return
        renderHandler.post {
            debugLog("D03", "attachSurface ${width}x$height")
            failed = false
            surfaceTexture = surface
            surfaceWidth = width
            surfaceHeight = height
            releaseEglSurface()
            scheduleRender()
        }
    }

    fun resizeSurface(width: Int, height: Int) {
        if (released) return
        renderHandler.post {
            debugLog("D04", "resizeSurface ${width}x$height")
            failed = false
            surfaceWidth = width
            surfaceHeight = height
            scheduleRender()
        }
    }

    fun detachSurface() {
        if (released) return
        renderHandler.post {
            debugLog("D05", "detachSurface")
            renderHandler.removeCallbacks(renderRunnable)
            surfaceTexture = null
            surfaceWidth = 0
            surfaceHeight = 0
            releaseEglSurface()
        }
    }

    fun requestRender() {
        if (released) return
        renderHandler.post {
            debugLog("D05R", "requestRender")
            failed = false
            scheduleRender()
        }
    }

    fun release() {
        if (released) return
        released = true
        debugLog("D06", "release requested")
        val latch = CountDownLatch(1)
        if (handlerThread.isAlive) {
            renderHandler.post {
                try {
                    renderHandler.removeCallbacks(renderRunnable)
                    releaseInternal()
                } finally {
                    latch.countDown()
                }
            }
            if (Thread.currentThread() !== handlerThread) {
                latch.await(400, TimeUnit.MILLISECONDS)
            }
            handlerThread.quitSafely()
        }
    }

    private fun scheduleRender() {
        if (released) {
            debugLog("D07", "scheduleRender skipped: released")
            return
        }
        if (failed) {
            debugLog("D08", "scheduleRender skipped: failed=true")
            return
        }
        renderHandler.removeCallbacks(renderRunnable)
        renderHandler.post(renderRunnable)
    }

    private fun renderFrame() {
        if (released) {
            debugLog("D09", "renderFrame skipped: released")
            return
        }
        if (failed) {
            debugLog("D10", "renderFrame skipped: failed=true")
            return
        }
        runCatching {
            renderFrameOrThrow()
        }.onFailure { throwable ->
            failed = true
            val sourceBitmap = synchronized(stateLock) { pendingBitmap }
            val params = synchronized(stateLock) { pendingParams }
            debugLog(
                "D99",
                "render failure surface=${surfaceWidth}x$surfaceHeight, ${summarizeBitmap(sourceBitmap)}, ${summarizeParams(params)}, surfaceTexture=${surfaceTexture != null}"
            )
            Log.e(
                "ColorCardTraceGpu",
                "Failed to render realtime trace preview",
                throwable
            )
            releaseInternal()
        }
    }

    private fun renderFrameOrThrow() {
        val sourceBitmap = synchronized(stateLock) { pendingBitmap }
        val params = synchronized(stateLock) { pendingParams }
        if (!ensureGlReady(params)) return
        debugLog(
            "D11",
            "renderFrameOrThrow surface=${surfaceWidth}x$surfaceHeight, ${summarizeBitmap(sourceBitmap)}, ${summarizeParams(params)}"
        )

        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (sourceBitmap == null) {
            debugLog("D12", "source bitmap null, swapping cleared frame")
            swapBuffersOrThrow()
            return
        }

        ensureTexture()
        if (uploadedBitmap !== sourceBitmap) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, sourceBitmap, 0)
            uploadedBitmap = sourceBitmap
        }
        if (params.haldEnabled && params.haldBitmap != null) {
            ensureHaldTexture()
            if (uploadedHaldBitmap !== params.haldBitmap) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldTextureId)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, params.haldBitmap, 0)
                uploadedHaldBitmap = params.haldBitmap
            }
        }
        if (params.haldEnabled && params.haldSecondaryBitmap != null) {
            ensureSecondaryHaldTexture()
            if (uploadedSecondaryHaldBitmap !== params.haldSecondaryBitmap) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldSecondaryTextureId)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, params.haldSecondaryBitmap, 0)
                uploadedSecondaryHaldBitmap = params.haldSecondaryBitmap
            }
        }

        GLES20.glUseProgram(programId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniformLocation, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            if (params.haldEnabled && params.haldBitmap != null) haldTextureId else 0
        )
        GLES20.glUniform1i(haldTextureUniformLocation, 1)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            if (params.haldEnabled && params.haldSecondaryBitmap != null) haldSecondaryTextureId else 0
        )
        GLES20.glUniform1i(haldSecondaryTextureUniformLocation, 2)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(filterEnabledLocation, if (params.filterEnabled) 1 else 0)
        GLES20.glUniform1i(anchorCountLocation, params.anchorCount.coerceIn(0, COLOR_CARD_TRACE_FILTER_MAX_ANCHORS))
        GLES20.glUniform3fv(sourceAnchorsLocation, COLOR_CARD_TRACE_FILTER_MAX_ANCHORS, params.sourceAnchors, 0)
        GLES20.glUniform3fv(targetAnchorsLocation, COLOR_CARD_TRACE_FILTER_MAX_ANCHORS, params.targetAnchors, 0)
        GLES20.glUniform1f(blendStrengthLocation, params.blendStrength.coerceIn(0f, 1f))
        GLES20.glUniform1f(maxLightnessShiftLocation, params.maxLightnessShift.coerceAtLeast(0f))
        GLES20.glUniform1f(maxChromaShiftLocation, params.maxChromaShift.coerceAtLeast(0f))
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
        GLES20.glUniform1i(haldEnabledLocation, if (params.haldEnabled && params.haldBitmap != null) 1 else 0)
        GLES20.glUniform1f(haldLevelLocation, params.haldLevel.toFloat().coerceAtLeast(0f))
        GLES20.glUniform1f(haldSecondaryLevelLocation, params.haldSecondaryLevel.toFloat().coerceAtLeast(0f))
        GLES20.glUniform1f(haldStrengthLocation, params.haldStrength.coerceIn(0f, 1f))
        GLES20.glUniform1f(haldVariantMixLocation, params.haldVariantMix.coerceIn(0f, 1f))
        GLES20.glUniform1i(pixelPosterEnabledLocation, if (params.pixelPosterEnabled) 1 else 0)
        GLES20.glUniform1i(pixelPosterUseSourceColorLocation, if (params.pixelPosterUseSourceColor) 1 else 0)
        GLES20.glUniform1i(pixelPosterContinuousToneLocation, if (params.pixelPosterContinuousTone) 1 else 0)
        GLES20.glUniform1f(
            pixelPosterBlockCountLongSideLocation,
            params.pixelPosterBlockCountLongSide.coerceAtLeast(0f)
        )
        GLES20.glUniform1f(pixelPosterThresholdLocation, params.pixelPosterThreshold.coerceIn(0f, 1f))
        GLES20.glUniform1f(pixelPosterContrastLocation, params.pixelPosterContrast.coerceAtLeast(0f))
        GLES20.glUniform1f(pixelPosterNoiseAmountLocation, params.pixelPosterNoiseAmount.coerceIn(0f, 1f))
        GLES20.glUniform1f(pixelPosterStrengthLocation, params.pixelPosterStrength.coerceIn(0f, 1f))
        GLES20.glUniform3f(
            pixelPosterDarkColorLocation,
            params.pixelPosterDarkRed.coerceIn(0f, 1f),
            params.pixelPosterDarkGreen.coerceIn(0f, 1f),
            params.pixelPosterDarkBlue.coerceIn(0f, 1f)
        )
        GLES20.glUniform3f(
            pixelPosterLightColorLocation,
            params.pixelPosterLightRed.coerceIn(0f, 1f),
            params.pixelPosterLightGreen.coerceIn(0f, 1f),
            params.pixelPosterLightBlue.coerceIn(0f, 1f)
        )
        GLES20.glUniform1i(subjectFrameEnabledLocation, if (params.subjectFrameEnabled) 1 else 0)
        GLES20.glUniform4fv(subjectFrameRectLocation, 1, params.subjectFrameRect, 0)
        GLES20.glUniform1f(subjectFrameBorderThicknessLocation, params.subjectFrameBorderThickness.coerceAtLeast(0f))
        GLES20.glUniform1f(subjectFrameStrengthLocation, params.subjectFrameStrength.coerceIn(0f, 1f))
        GLES20.glUniform3f(
            subjectFrameBorderColorLocation,
            params.subjectFrameBorderRed.coerceIn(0f, 1f),
            params.subjectFrameBorderGreen.coerceIn(0f, 1f),
            params.subjectFrameBorderBlue.coerceIn(0f, 1f)
        )
        GLES20.glUniform1i(blackMistEnabledLocation, if (params.blackMistEnabled) 1 else 0)
        GLES20.glUniform1f(blackMistStrengthLocation, params.blackMistStrength.coerceIn(0f, 1f))
        GLES20.glUniform1i(selectiveColorEnabledLocation, if (params.selectiveColorEnabled) 1 else 0)
        GLES20.glUniform1f(selectiveColorTargetHueLocation, params.selectiveColorTargetHue.coerceIn(0f, 1f))
        GLES20.glUniform1f(selectiveColorHueRangeLocation, params.selectiveColorHueRange.coerceIn(0f, 0.5f))
        GLES20.glUniform1f(
            selectiveColorSaturationFloorLocation,
            params.selectiveColorSaturationFloor.coerceIn(0f, 1f)
        )
        GLES20.glUniform1f(selectiveColorStrengthLocation, params.selectiveColorStrength.coerceIn(0f, 1f))
        GLES20.glUniform1f(sourceAspectLocation, params.sourceAspect.coerceAtLeast(0.0001f))
        GLES20.glUniform2f(
            sourceTexelSizeLocation,
            params.sourceTexelWidth.coerceAtLeast(0.000001f),
            params.sourceTexelHeight.coerceAtLeast(0.000001f)
        )

        FULLSCREEN_QUAD.position(0)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            FULLSCREEN_VERTEX_STRIDE_BYTES,
            FULLSCREEN_QUAD
        )
        FULLSCREEN_QUAD.position(2)
        GLES20.glEnableVertexAttribArray(texCoordLocation)
        GLES20.glVertexAttribPointer(
            texCoordLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            FULLSCREEN_VERTEX_STRIDE_BYTES,
            FULLSCREEN_QUAD
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glDisableVertexAttribArray(texCoordLocation)

        swapBuffersOrThrow()
        debugLog("D13", "frame drawn and swapped")
    }

    private fun ensureGlReady(params: ColorCardTraceGpuParams): Boolean {
        val targetSurfaceTexture = surfaceTexture ?: run {
            debugLog("D14", "ensureGlReady skipped: surfaceTexture=null")
            return false
        }
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            debugLog("D15", "ensureGlReady skipped: invalid surface size ${surfaceWidth}x$surfaceHeight")
            return false
        }

        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            debugLog("D16", "creating EGL display/context")
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(eglDisplay !== EGL14.EGL_NO_DISPLAY) { "Unable to obtain EGL display" }
            val version = IntArray(2)
            check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                "Unable to initialize EGL"
            }

            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
            )
            check(
                EGL14.eglChooseConfig(
                    eglDisplay,
                    attribList,
                    0,
                    configs,
                    0,
                    configs.size,
                    configCount,
                    0
                ) && configCount[0] > 0
            ) {
                "Unable to choose EGL config"
            }
            eglConfig = configs[0]
            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT,
                contextAttribs,
                0
            )
            check(eglContext !== EGL14.EGL_NO_CONTEXT) { "Unable to create EGL context" }
            debugLog("D17", "EGL context ready")
        }

        if (eglSurface === EGL14.EGL_NO_SURFACE) {
            debugLog("D18", "creating EGL window surface")
            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay,
                eglConfig,
                targetSurfaceTexture,
                intArrayOf(EGL14.EGL_NONE),
                0
            )
            check(eglSurface !== EGL14.EGL_NO_SURFACE) { "Unable to create EGL surface" }
        }

        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            "Unable to make EGL context current"
        }
        debugLog("D19", "EGL current")

        val shaderFeatures = resolveColorCardTraceGpuShaderFeatures(params)
        if (programId == 0 || (!programUsesLegacyShader && programShaderFeatures != shaderFeatures)) {
            debugLog("D20", "creating shader program")
            if (programId != 0) {
                GLES20.glDeleteProgram(programId)
                programId = 0
            }
            val featureShaderSource = buildColorCardTraceFragmentShader(shaderFeatures)
            val usedLegacyShader = try {
                programId = createProgram(
                    vertexShaderSource = COLOR_CARD_TRACE_VERTEX_SHADER,
                    fragmentShaderSource = featureShaderSource
                )
                false
            } catch (throwable: IllegalStateException) {
                debugLog(
                    "D20F",
                    "feature shader failed; falling back to legacy shader for $shaderFeatures"
                )
                Log.w(
                    "ColorCardTraceGpu",
                    "Feature shader compile failed in realtime renderer; falling back to legacy shader. features=$shaderFeatures",
                    throwable
                )
                programId = createProgram(
                    vertexShaderSource = COLOR_CARD_TRACE_VERTEX_SHADER,
                    fragmentShaderSource = buildLegacyColorCardTraceFragmentShader()
                )
                true
            }
            programShaderFeatures = if (usedLegacyShader) null else shaderFeatures
            programUsesLegacyShader = usedLegacyShader
            positionLocation = GLES20.glGetAttribLocation(programId, "aPosition")
            texCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord")
            textureUniformLocation = GLES20.glGetUniformLocation(programId, "uTexture")
            haldTextureUniformLocation = GLES20.glGetUniformLocation(programId, "uHaldTexture")
            haldSecondaryTextureUniformLocation = GLES20.glGetUniformLocation(programId, "uHaldTextureSecondary")
            filterEnabledLocation = GLES20.glGetUniformLocation(programId, "uFilterEnabled")
            anchorCountLocation = GLES20.glGetUniformLocation(programId, "uAnchorCount")
            sourceAnchorsLocation = GLES20.glGetUniformLocation(programId, "uSourceAnchors")
            targetAnchorsLocation = GLES20.glGetUniformLocation(programId, "uTargetAnchors")
            blendStrengthLocation = GLES20.glGetUniformLocation(programId, "uBlendStrength")
            maxLightnessShiftLocation = GLES20.glGetUniformLocation(programId, "uMaxLightnessShift")
            maxChromaShiftLocation = GLES20.glGetUniformLocation(programId, "uMaxChromaShift")
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
            haldLevelLocation = GLES20.glGetUniformLocation(programId, "uHaldLevel")
            haldSecondaryLevelLocation = GLES20.glGetUniformLocation(programId, "uHaldSecondaryLevel")
            haldStrengthLocation = GLES20.glGetUniformLocation(programId, "uHaldStrength")
            haldVariantMixLocation = GLES20.glGetUniformLocation(programId, "uHaldVariantMix")
            pixelPosterEnabledLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterEnabled")
            pixelPosterUseSourceColorLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterUseSourceColor")
            pixelPosterContinuousToneLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterContinuousTone")
            pixelPosterBlockCountLongSideLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterBlockCountLongSide")
            pixelPosterThresholdLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterThreshold")
            pixelPosterContrastLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterContrast")
            pixelPosterNoiseAmountLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterNoiseAmount")
            pixelPosterStrengthLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterStrength")
            pixelPosterDarkColorLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterDarkColor")
            pixelPosterLightColorLocation = GLES20.glGetUniformLocation(programId, "uPixelPosterLightColor")
            subjectFrameEnabledLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameEnabled")
            subjectFrameRectLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameRect")
            subjectFrameBorderThicknessLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameBorderThickness")
            subjectFrameStrengthLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameStrength")
            subjectFrameBorderColorLocation = GLES20.glGetUniformLocation(programId, "uSubjectFrameBorderColor")
            blackMistEnabledLocation = GLES20.glGetUniformLocation(programId, "uBlackMistEnabled")
            blackMistStrengthLocation = GLES20.glGetUniformLocation(programId, "uBlackMistStrength")
            selectiveColorEnabledLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorEnabled")
            selectiveColorTargetHueLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorTargetHue")
            selectiveColorHueRangeLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorHueRange")
            selectiveColorSaturationFloorLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorSaturationFloor")
            selectiveColorStrengthLocation = GLES20.glGetUniformLocation(programId, "uSelectiveColorStrength")
            sourceAspectLocation = GLES20.glGetUniformLocation(programId, "uSourceAspect")
            sourceTexelSizeLocation = GLES20.glGetUniformLocation(programId, "uSourceTexelSize")
            debugLog("D21", "shader program ready id=$programId")
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        return true
    }

    private fun ensureTexture() {
        if (textureId != 0) return
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    private fun ensureHaldTexture() {
        if (haldTextureId != 0) return
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        haldTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldTextureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    private fun ensureSecondaryHaldTexture() {
        if (haldSecondaryTextureId != 0) return
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        haldSecondaryTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldSecondaryTextureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    private fun swapBuffersOrThrow() {
        debugLog("D22", "swapBuffers")
        check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) { "Unable to swap EGL buffers" }
    }

    private fun releaseInternal() {
        debugLog("D23", "releaseInternal")
        releaseGlObjects()
        releaseEglSurface()
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            if (eglContext !== EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
            }
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglConfig = null
        surfaceTexture = null
        uploadedBitmap = null
        uploadedHaldBitmap = null
        uploadedSecondaryHaldBitmap = null
    }

    private fun releaseGlObjects() {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY || eglContext === EGL14.EGL_NO_CONTEXT) return
        if (eglSurface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        }
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        if (haldTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(haldTextureId), 0)
            haldTextureId = 0
        }
        if (haldSecondaryTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(haldSecondaryTextureId), 0)
            haldSecondaryTextureId = 0
        }
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
            programShaderFeatures = null
            programUsesLegacyShader = false
        }
    }

    private fun releaseEglSurface() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY && eglSurface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    private fun createProgram(
        vertexShaderSource: String,
        fragmentShaderSource: String
    ): Int {
        val vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        return GLES20.glCreateProgram().also { program ->
            check(program != 0) { "Unable to create GL program" }
            GLES20.glAttachShader(program, vertexShaderId)
            GLES20.glAttachShader(program, fragmentShaderId)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val infoLog = GLES20.glGetProgramInfoLog(program)
                debugLog("D24", "program link failed: $infoLog")
                GLES20.glDeleteProgram(program)
                error("Unable to link GL program: $infoLog")
            }
            GLES20.glDeleteShader(vertexShaderId)
            GLES20.glDeleteShader(fragmentShaderId)
        }
    }

    private fun compileShader(
        type: Int,
        source: String
    ): Int {
        return GLES20.glCreateShader(type).also { shader ->
            check(shader != 0) { "Unable to create GL shader" }
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val infoLog = GLES20.glGetShaderInfoLog(shader)
                debugLog(
                    "D25",
                    "shader compile failed type=$type: $infoLog"
                )
                GLES20.glDeleteShader(shader)
                error("Unable to compile GL shader: $infoLog")
            }
        }
    }
}

private const val FULLSCREEN_VERTEX_STRIDE_BYTES = 4 * 4

private val FULLSCREEN_QUAD: FloatBuffer = ByteBuffer
    .allocateDirect(4 * 4 * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
        put(
            floatArrayOf(
                -1f, -1f, 0f, 1f,
                1f, -1f, 1f, 1f,
                -1f, 1f, 0f, 0f,
                1f, 1f, 1f, 0f
            )
        )
        position(0)
    }

internal data class ColorCardTraceGpuShaderFeatures(
    val palette: Boolean,
    val portrait: Boolean,
    val hald: Boolean,
    val pixelPoster: Boolean,
    val subjectFrame: Boolean,
    val blackMist: Boolean,
    val selectiveColor: Boolean
)

internal fun resolveColorCardTraceGpuShaderFeatures(
    params: ColorCardTraceGpuParams
): ColorCardTraceGpuShaderFeatures {
    val portraitEnabled = params.portraitEnabled &&
        params.protectedFaceCount > 0 &&
        (
            params.portraitLightnessLift > 0.0001f ||
                params.portraitSaturationBoost > 0.0001f ||
                params.portraitSoftenAmount > 0.0001f ||
                params.portraitSharpenRetention < 0.9999f
            )
    return ColorCardTraceGpuShaderFeatures(
        palette = params.filterEnabled && params.anchorCount > 0,
        portrait = portraitEnabled,
        hald = params.haldEnabled && params.haldBitmap != null && params.haldLevel > 0,
        pixelPoster = params.pixelPosterEnabled &&
            (params.pixelPosterContinuousTone || params.pixelPosterBlockCountLongSide >= 1f),
        subjectFrame = params.subjectFrameEnabled,
        blackMist = params.blackMistEnabled,
        selectiveColor = params.selectiveColorEnabled && params.selectiveColorHueRange > 0.0001f
    )
}

internal fun buildColorCardTraceFragmentShader(
    features: ColorCardTraceGpuShaderFeatures
): String {
    val featureHeader = """
#define FEATURE_PALETTE ${if (features.palette) 1 else 0}
#define FEATURE_PORTRAIT ${if (features.portrait) 1 else 0}
#define FEATURE_HALD ${if (features.hald) 1 else 0}
#define FEATURE_PIXEL_POSTER ${if (features.pixelPoster) 1 else 0}
#define FEATURE_SUBJECT_FRAME ${if (features.subjectFrame) 1 else 0}
#define FEATURE_BLACK_MIST ${if (features.blackMist) 1 else 0}
#define FEATURE_SELECTIVE_COLOR ${if (features.selectiveColor) 1 else 0}
""".trimIndent()
    return "$featureHeader\n$COLOR_CARD_TRACE_FRAGMENT_SHADER_TEMPLATE"
}

private fun buildLegacyColorCardTraceFragmentShader(): String =
    COLOR_CARD_TRACE_FRAGMENT_SHADER_TEMPLATE
        .lineSequence()
        .filterNot { line ->
            val trimmed = line.trim()
            trimmed.startsWith("#if FEATURE_") || trimmed == "#endif"
        }
        .joinToString("\n")

internal val COLOR_CARD_TRACE_VERTEX_SHADER = """
attribute vec2 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;

void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
    vTexCoord = aTexCoord;
}
""".trimIndent()

private val COLOR_CARD_TRACE_FRAGMENT_SHADER_TEMPLATE = """
precision highp float;

const int MAX_ANCHORS = ${COLOR_CARD_TRACE_FILTER_MAX_ANCHORS};
const int MAX_PROTECTED_FACES = ${COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES};

uniform sampler2D uTexture;
#if FEATURE_HALD
uniform sampler2D uHaldTexture;
uniform sampler2D uHaldTextureSecondary;
#endif
#if FEATURE_PALETTE
uniform int uFilterEnabled;
uniform int uAnchorCount;
uniform vec3 uSourceAnchors[MAX_ANCHORS];
uniform vec3 uTargetAnchors[MAX_ANCHORS];
uniform float uBlendStrength;
uniform float uMaxLightnessShift;
uniform float uMaxChromaShift;
#endif
uniform float uColor;
uniform vec2 uColorAxis;
uniform float uSharpen;
uniform float uTone;
uniform float uSaturation;
uniform float uBrightness;
uniform int uProtectedFaceCount;
uniform vec4 uProtectedFaces[MAX_PROTECTED_FACES];
#if FEATURE_PORTRAIT
uniform int uPortraitEnabled;
uniform float uPortraitLightnessLift;
uniform float uPortraitSaturationBoost;
uniform float uPortraitSharpenRetention;
uniform float uPortraitSoftenAmount;
#endif
#if FEATURE_HALD
uniform int uHaldEnabled;
uniform float uHaldLevel;
uniform float uHaldSecondaryLevel;
uniform float uHaldStrength;
uniform float uHaldVariantMix;
#endif
#if FEATURE_PIXEL_POSTER
uniform int uPixelPosterEnabled;
uniform int uPixelPosterUseSourceColor;
uniform int uPixelPosterContinuousTone;
uniform float uPixelPosterBlockCountLongSide;
uniform float uPixelPosterThreshold;
uniform float uPixelPosterContrast;
uniform float uPixelPosterNoiseAmount;
uniform float uPixelPosterGridLineWidth;
uniform float uPixelPosterGridLineOpacity;
uniform float uPixelPosterDetailBoost;
uniform float uPixelPosterShadowDetailBias;
uniform float uPixelPosterStrength;
uniform vec3 uPixelPosterDarkColor;
uniform vec3 uPixelPosterLightColor;
uniform float uSourceAspect;
#endif
#if FEATURE_SUBJECT_FRAME
uniform int uSubjectFrameEnabled;
uniform vec4 uSubjectFrameRect;
uniform float uSubjectFrameBorderThickness;
uniform float uSubjectFrameStrength;
uniform vec3 uSubjectFrameBorderColor;
#endif
#if FEATURE_BLACK_MIST
uniform int uBlackMistEnabled;
uniform float uBlackMistStrength;
#endif
#if FEATURE_SELECTIVE_COLOR
uniform int uSelectiveColorEnabled;
uniform float uSelectiveColorTargetHue;
uniform float uSelectiveColorHueRange;
uniform float uSelectiveColorSaturationFloor;
uniform float uSelectiveColorStrength;
#endif
uniform vec2 uSourceTexelSize;

varying vec2 vTexCoord;

float faceProtectionMask(vec2 uv);
float skinToneMask(vec3 rgb);
float skinProtectionAmount(vec3 rgb, vec2 uv);
#if FEATURE_PORTRAIT
float portraitDetailAmount(vec3 rgb, vec2 uv);
float portraitSoftnessAmount(vec3 rgb, vec2 uv);
#endif

float srgbToLinear(float channel) {
    if (channel <= 0.04045) {
        return channel / 12.92;
    }
    return pow((channel + 0.055) / 1.055, 2.4);
}

float linearToSrgb(float channel) {
    float safeChannel = clamp(channel, 0.0, 1.0);
    if (safeChannel <= 0.0031308) {
        return safeChannel * 12.92;
    }
    return (1.055 * pow(safeChannel, 1.0 / 2.4)) - 0.055;
}

float labPivot(float value) {
    if (value > 0.008856) {
        return pow(value, 1.0 / 3.0);
    }
    return (7.787 * value) + (16.0 / 116.0);
}

float inverseLabPivot(float value) {
    float cube = value * value * value;
    if (cube > 0.008856) {
        return cube;
    }
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

    return vec3(
        (116.0 * yPivot) - 16.0,
        500.0 * (xPivot - yPivot),
        200.0 * (yPivot - zPivot)
    );
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

    return vec3(
        linearToSrgb(rLinear),
        linearToSrgb(gLinear),
        linearToSrgb(bLinear)
    );
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
    vec2 safeColorAxis = dot(uColorAxis, uColorAxis) > 0.0001
        ? normalize(uColorAxis)
        : vec2(1.0, 0.0);
    if (abs(tone) > 0.0001) {
        adjustedLab.x = clamp(applyToneCurve(adjustedLab.x / 100.0, tone) * 100.0, 0.0, 100.0);
    }
    if (abs(brightness) > 0.0001) {
        adjustedLab.x = clamp(adjustedLab.x + (brightness * 18.0), 0.0, 100.0);
    }
    if (abs(color) > 0.0001) {
        float colorShift = color * 16.0;
        adjustedLab.y = adjustedLab.y + (safeColorAxis.x * colorShift);
        adjustedLab.z = adjustedLab.z + (safeColorAxis.y * colorShift);
    }
    if (abs(saturation) > 0.0001) {
        float satFactor = saturation >= 0.0
            ? (1.0 + (saturation * 0.75))
            : (1.0 + (saturation * 0.68));
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

#if FEATURE_PORTRAIT
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
#endif

vec3 applySourceDetailAdjust(vec2 uv, vec3 centerRgb) {
    float safeSharpen = clamp(uSharpen, -1.0, 1.0);
    float detailSuppressionAmount = 0.0;
    bool portraitDetailEnabled = false;
#if FEATURE_PORTRAIT
    detailSuppressionAmount = (uPortraitEnabled != 0) && (uProtectedFaceCount > 0)
        ? (1.0 - clamp(uPortraitSharpenRetention, 0.0, 1.0))
        : 0.0;
    portraitDetailEnabled = detailSuppressionAmount > 0.0001;
#endif
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
#if FEATURE_PORTRAIT
    if (portraitDetailEnabled) {
        float portraitProtection = portraitDetailAmount(centerRgb, uv);
        if (portraitProtection > 0.0001) {
            if (safeSharpen > 0.0) {
                effectiveSharpen = safeSharpen * mix(1.0, clamp(uPortraitSharpenRetention, 0.0, 1.0), portraitProtection);
            }
        }
    }
#endif
    vec3 detailAdjusted = effectiveSharpen >= 0.0
        ? clamp(centerRgb + ((centerRgb - blurred) * (effectiveSharpen * ${COLOR_CARD_TRACE_SHARPEN_DETAIL_SCALE})), 0.0, 1.0)
        : mix(centerRgb, blurred, -effectiveSharpen * ${COLOR_CARD_TRACE_SOFTEN_BLEND_SCALE});
#if FEATURE_PORTRAIT
    if (portraitDetailEnabled) {
        float portraitProtection = portraitDetailAmount(centerRgb, uv);
        if (portraitProtection > 0.0001) {
            float detailLuma = abs(dot(centerRgb - blurred, vec3(0.299, 0.587, 0.114)));
            float edgeMask = smoothstep(0.016, 0.070, detailLuma);
            float suppressionMix = clamp(
                detailSuppressionAmount * portraitProtection * edgeMask * 1.72,
                0.0,
                1.0
            );
            if (suppressionMix > 0.0001) {
                detailAdjusted = mix(detailAdjusted, blurred, suppressionMix * 0.96);
            }
        }
    }
#endif
    return detailAdjusted;
}

vec3 sampleSourceWithDetail(vec2 uv) {
    vec3 centerRgb = texture2D(uTexture, uv).rgb;
    return applySourceDetailAdjust(uv, centerRgb);
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

float portraitDetailAmount(vec3 rgb, vec2 uv) {
    if (uProtectedFaceCount <= 0) {
        return 0.0;
    }
    float faceMask = faceProtectionMask(uv);
    if (faceMask <= 0.0001) {
        return 0.0;
    }
    float skinMask = skinToneMask(rgb);
    return clamp(faceMask * (0.70 + (0.30 * skinMask)), 0.0, 1.0);
}

float portraitSoftnessAmount(vec3 rgb, vec2 uv) {
    if (uProtectedFaceCount <= 0) {
        return 0.0;
    }
    float faceMask = faceProtectionMask(uv);
    if (faceMask <= 0.0001) {
        return 0.0;
    }
    float skinMask = skinToneMask(rgb);
    return clamp(faceMask * (0.84 + (0.16 * skinMask)), 0.0, 1.0);
}

#if FEATURE_PORTRAIT
vec3 samplePortraitSoftBlur(vec2 uv) {
    vec2 texel = max(uSourceTexelSize, vec2(0.000001));
    vec3 center = texture2D(uTexture, uv).rgb * 4.0;
    vec3 crossNear = (
        texture2D(uTexture, clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb
    ) * 1.42;
    vec3 crossFar = (
        texture2D(uTexture, clamp(uv + vec2(-texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(0.0, -texel.y * 2.0), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(0.0, texel.y * 2.0), vec2(0.0), vec2(1.0))).rgb
    ) * 0.82;
    vec3 diagonals = (
        texture2D(uTexture, clamp(uv + vec2(-texel.x, -texel.y), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(texel.x, -texel.y), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(-texel.x, texel.y), vec2(0.0), vec2(1.0))).rgb +
        texture2D(uTexture, clamp(uv + vec2(texel.x, texel.y), vec2(0.0), vec2(1.0))).rgb
    ) * 0.94;
    return clamp((center + crossNear + crossFar + diagonals) / 16.72, 0.0, 1.0);
}

vec3 applyPortraitSoftness(vec2 uv, vec3 rgb, float portraitMask) {
    if (uPortraitEnabled == 0 || uPortraitSoftenAmount <= 0.0001 || portraitMask <= 0.0001) {
        return rgb;
    }
    float softenMix = clamp(uPortraitSoftenAmount * portraitMask * 1.82, 0.0, 1.0);
    if (softenMix <= 0.0001) {
        return rgb;
    }
    return mix(rgb, samplePortraitSoftBlur(uv), softenMix);
}
#endif

#if FEATURE_PALETTE
vec3 sourceAnchorAt(int index) {
    vec3 result = uSourceAnchors[0];
    for (int i = 0; i < MAX_ANCHORS; i++) {
        if (i == index) {
            result = uSourceAnchors[i];
        }
    }
    return result;
}

vec3 targetAnchorAt(int index) {
    vec3 result = uTargetAnchors[0];
    for (int i = 0; i < MAX_ANCHORS; i++) {
        if (i == index) {
            result = uTargetAnchors[i];
        }
    }
    return result;
}
#endif

#if FEATURE_HALD
vec2 haldUv(float linearIndex, float imageSide) {
    float x = mod(linearIndex, imageSide);
    float y = floor(linearIndex / imageSide);
    return vec2(
        (x + 0.5) / imageSide,
        (y + 0.5) / imageSide
    );
}

vec3 sampleHaldCell(sampler2D haldTexture, float rIndex, float gIndex, float bIndex, float cubeSide, float imageSide) {
    float linearIndex = rIndex + (gIndex * cubeSide) + (bIndex * cubeSide * cubeSide);
    return texture2D(haldTexture, haldUv(linearIndex, imageSide)).rgb;
}

vec3 applySingleHaldClut(sampler2D haldTexture, float haldLevel, vec3 rgb) {
    if (haldLevel < 1.0) {
        return rgb;
    }
    float cubeSide = haldLevel * haldLevel;
    if (cubeSide <= 1.0) {
        return rgb;
    }
    float imageSide = haldLevel * cubeSide;
    vec3 scaled = clamp(rgb, 0.0, 1.0) * (cubeSide - 1.0);
    vec3 index0 = floor(scaled);
    vec3 index1 = min(index0 + 1.0, vec3(cubeSide - 1.0));
    vec3 fraction = clamp(scaled - index0, 0.0, 1.0);

    vec3 c000 = sampleHaldCell(haldTexture, index0.x, index0.y, index0.z, cubeSide, imageSide);
    vec3 c100 = sampleHaldCell(haldTexture, index1.x, index0.y, index0.z, cubeSide, imageSide);
    vec3 c010 = sampleHaldCell(haldTexture, index0.x, index1.y, index0.z, cubeSide, imageSide);
    vec3 c110 = sampleHaldCell(haldTexture, index1.x, index1.y, index0.z, cubeSide, imageSide);
    vec3 c001 = sampleHaldCell(haldTexture, index0.x, index0.y, index1.z, cubeSide, imageSide);
    vec3 c101 = sampleHaldCell(haldTexture, index1.x, index0.y, index1.z, cubeSide, imageSide);
    vec3 c011 = sampleHaldCell(haldTexture, index0.x, index1.y, index1.z, cubeSide, imageSide);
    vec3 c111 = sampleHaldCell(haldTexture, index1.x, index1.y, index1.z, cubeSide, imageSide);

    vec3 c00 = mix(c000, c100, fraction.x);
    vec3 c10 = mix(c010, c110, fraction.x);
    vec3 c01 = mix(c001, c101, fraction.x);
    vec3 c11 = mix(c011, c111, fraction.x);
    vec3 c0 = mix(c00, c10, fraction.y);
    vec3 c1 = mix(c01, c11, fraction.y);
    return mix(c0, c1, fraction.z);
}

vec3 applyHaldClut(vec3 rgb) {
    if (uHaldEnabled == 0 || uHaldLevel < 1.0) {
        return rgb;
    }
    vec3 primary = applySingleHaldClut(uHaldTexture, uHaldLevel, rgb);
    if (uHaldSecondaryLevel < 1.0 || uHaldVariantMix <= 0.0001) {
        return primary;
    }
    vec3 secondary = applySingleHaldClut(uHaldTextureSecondary, uHaldSecondaryLevel, rgb);
    return mix(primary, secondary, clamp(uHaldVariantMix, 0.0, 1.0));
}
#endif

#if FEATURE_PIXEL_POSTER
float hash12(vec2 value) {
    float h = dot(value, vec2(12.9898, 78.233));
    return fract(sin(h + 37.719) * 43758.5453);
}

vec2 pixelPosterGrid() {
    float safeLongSide = max(uPixelPosterBlockCountLongSide, 1.0);
    float safeAspect = max(uSourceAspect, 0.0001);
    if (safeAspect >= 1.0) {
        return vec2(safeLongSide, max(safeLongSide / safeAspect, 1.0));
    }
    return vec2(max(safeLongSide * safeAspect, 1.0), safeLongSide);
}

vec2 pixelPosterBlockCoord(vec2 uv, vec2 grid) {
    vec2 safeUv = clamp(uv, vec2(0.0), vec2(0.999999));
    return floor(safeUv * grid);
}

vec2 pixelPosterSampleUv(vec2 uv) {
    vec2 grid = pixelPosterGrid();
    vec2 blockCoord = pixelPosterBlockCoord(uv, grid);
    return (blockCoord + 0.5) / grid;
}

float pixelPosterGridMask(vec2 uv, vec2 grid) {
    if (uPixelPosterGridLineWidth <= 0.0) {
        return 0.0;
    }
    vec2 local = fract(clamp(uv, vec2(0.0), vec2(0.999999)) * grid);
    vec2 texel = max(uSourceTexelSize, vec2(0.000001));
    float minimumPreviewPixels = 1.15;
    float edgeWidth = max(
        uPixelPosterGridLineWidth,
        max(
            minimumPreviewPixels * texel.x * grid.x,
            minimumPreviewPixels * texel.y * grid.y
        )
    );
    edgeWidth = clamp(edgeWidth, 0.0, 0.49);
    bool verticalLine = local.x <= edgeWidth || local.x >= (1.0 - edgeWidth);
    bool horizontalLine = local.y <= edgeWidth || local.y >= (1.0 - edgeWidth);
    return (verticalLine || horizontalLine) ? 1.0 : 0.0;
}

vec3 applyPixelPosterGridOverlay(vec3 rgb, float gridMask) {
    float overlay = clamp(gridMask * uPixelPosterGridLineOpacity, 0.0, 1.0);
    return mix(rgb, vec3(0.0), overlay);
}

vec3 samplePixelPosterDetailedSourceColor(vec2 grid, vec2 blockCoord) {
    vec2 cellSize = 1.0 / grid;
    vec2 blockOrigin = blockCoord / grid;
    vec3 sample00 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.18, 0.18)));
    vec3 sample10 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.50, 0.18)));
    vec3 sample20 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.82, 0.18)));
    vec3 sample01 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.18, 0.50)));
    vec3 sample11 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.50, 0.50)));
    vec3 sample21 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.82, 0.50)));
    vec3 sample02 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.18, 0.82)));
    vec3 sample12 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.50, 0.82)));
    vec3 sample22 = sampleSourceWithDetail(blockOrigin + (cellSize * vec2(0.82, 0.82)));

    vec3 averageRgb = (
        sample00 + sample10 + sample20 +
        sample01 + sample11 + sample21 +
        sample02 + sample12 + sample22
    ) / 9.0;

    float luma00 = dot(sample00, vec3(0.299, 0.587, 0.114));
    float luma10 = dot(sample10, vec3(0.299, 0.587, 0.114));
    float luma20 = dot(sample20, vec3(0.299, 0.587, 0.114));
    float luma01 = dot(sample01, vec3(0.299, 0.587, 0.114));
    float luma11 = dot(sample11, vec3(0.299, 0.587, 0.114));
    float luma21 = dot(sample21, vec3(0.299, 0.587, 0.114));
    float luma02 = dot(sample02, vec3(0.299, 0.587, 0.114));
    float luma12 = dot(sample12, vec3(0.299, 0.587, 0.114));
    float luma22 = dot(sample22, vec3(0.299, 0.587, 0.114));

    float minLuma = luma00;
    vec3 darkestRgb = sample00;
    if (luma10 < minLuma) { minLuma = luma10; darkestRgb = sample10; }
    if (luma20 < minLuma) { minLuma = luma20; darkestRgb = sample20; }
    if (luma01 < minLuma) { minLuma = luma01; darkestRgb = sample01; }
    if (luma11 < minLuma) { minLuma = luma11; darkestRgb = sample11; }
    if (luma21 < minLuma) { minLuma = luma21; darkestRgb = sample21; }
    if (luma02 < minLuma) { minLuma = luma02; darkestRgb = sample02; }
    if (luma12 < minLuma) { minLuma = luma12; darkestRgb = sample12; }
    if (luma22 < minLuma) { minLuma = luma22; darkestRgb = sample22; }

    float maxLuma = max(
        max(max(luma00, luma10), max(luma20, luma01)),
        max(max(luma11, luma21), max(max(luma02, luma12), luma22))
    );
    float averageLuma = (
        luma00 + luma10 + luma20 +
        luma01 + luma11 + luma21 +
        luma02 + luma12 + luma22
    ) / 9.0;
    float edgeResponse = smoothstep(0.10, 0.34, clamp(maxLuma - minLuma, 0.0, 1.0));
    float shadowResponse = smoothstep(0.04, 0.24, clamp(averageLuma - minLuma, 0.0, 1.0)) * uPixelPosterShadowDetailBias;
    float detailMix = clamp(uPixelPosterDetailBoost * edgeResponse * shadowResponse, 0.0, 1.0);
    return mix(averageRgb, darkestRgb, detailMix);
}

float continuousPixelPosterLightMix(vec3 sampled) {
    float luma = dot(sampled, vec3(0.299, 0.587, 0.114));
    float maxChannel = max(max(sampled.r, sampled.g), sampled.b);
    float minChannel = min(min(sampled.r, sampled.g), sampled.b);
    float saturation = clamp(maxChannel - minChannel, 0.0, 1.0);
    float softContrast = 1.0 + ((max(uPixelPosterContrast, 0.1) - 1.0) * 0.42);
    float remappedLuma = clamp(((luma - 0.5) * softContrast) + 0.5, 0.0, 1.0);
    float shadowEdge = clamp(uPixelPosterThreshold - 0.18, 0.0, 1.0);
    float highlightEdge = clamp(uPixelPosterThreshold + 0.18, 0.0, 1.0);
    float toneMix = smoothstep(shadowEdge, highlightEdge, remappedLuma);
    float vibranceLift = saturation * 0.12;
    return clamp((toneMix * (0.90 + vibranceLift)) + (luma * 0.10), 0.0, 1.0);
}

vec3 applyPixelPoster(vec2 uv) {
    vec2 grid = pixelPosterGrid();
    vec2 blockCoord = pixelPosterBlockCoord(uv, grid);
    vec3 sampled = sampleSourceWithDetail(
        uPixelPosterContinuousTone != 0 ? uv : pixelPosterSampleUv(uv)
    );
    if (uPixelPosterUseSourceColor != 0) {
        if (uPixelPosterDetailBoost > 0.0 && uPixelPosterContinuousTone == 0) {
            return samplePixelPosterDetailedSourceColor(grid, blockCoord);
        }
        return sampled;
    }
    if (uPixelPosterContinuousTone != 0) {
        float lightMix = continuousPixelPosterLightMix(sampled);
        return mix(uPixelPosterDarkColor, uPixelPosterLightColor, lightMix);
    }
    float luma = dot(sampled, vec3(0.299, 0.587, 0.114));
    float contrasted = clamp(((luma - 0.5) * max(uPixelPosterContrast, 0.1)) + 0.5, 0.0, 1.0);
    float thresholdNoise = (hash12(blockCoord) - 0.5) * uPixelPosterNoiseAmount;
    float lightMix = step(uPixelPosterThreshold, clamp(contrasted + thresholdNoise, 0.0, 1.0));
    return mix(uPixelPosterDarkColor, uPixelPosterLightColor, lightMix);
}
#endif

#if FEATURE_SUBJECT_FRAME
bool subjectFrameInside(vec2 uv) {
    return uv.x >= uSubjectFrameRect.x &&
        uv.x <= uSubjectFrameRect.z &&
        uv.y >= uSubjectFrameRect.y &&
        uv.y <= uSubjectFrameRect.w;
}

bool subjectFrameBorder(vec2 uv) {
    if (!subjectFrameInside(uv)) {
        return false;
    }
    float horizontalDistance = min(uv.x - uSubjectFrameRect.x, uSubjectFrameRect.z - uv.x);
    float verticalDistance = min(uv.y - uSubjectFrameRect.y, uSubjectFrameRect.w - uv.y);
    return horizontalDistance <= uSubjectFrameBorderThickness ||
        verticalDistance <= uSubjectFrameBorderThickness;
}

vec3 applySubjectFrame(vec2 uv, vec3 rgb) {
    if (uSubjectFrameEnabled == 0) {
        return rgb;
    }
    float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
    vec3 grayscaleRgb = vec3(gray);
    vec3 outsideRgb = grayscaleRgb;
    if (subjectFrameBorder(uv)) {
        return mix(outsideRgb, uSubjectFrameBorderColor, clamp(0.42 + (uSubjectFrameStrength * 0.50), 0.0, 1.0));
    }
    if (subjectFrameInside(uv)) {
        return rgb;
    }
    return outsideRgb;
}
#endif

#if FEATURE_BLACK_MIST
vec3 boostBlackMistSaturation(vec3 rgb, float factor) {
    float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
    return clamp(vec3(gray) + ((rgb - vec3(gray)) * factor), 0.0, 1.0);
}
#endif

#if FEATURE_SELECTIVE_COLOR
vec3 rgbToHsv(vec3 c) {
    vec4 k = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, k.wz), vec4(c.gb, k.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 0.0000001;
    return vec3(
        abs(q.z + ((q.w - q.y) / ((6.0 * d) + e))),
        d / (q.x + e),
        q.x
    );
}

vec3 applySelectiveColor(vec3 rgb) {
    if (uSelectiveColorEnabled == 0 || uSelectiveColorStrength <= 0.0001 || uSelectiveColorHueRange <= 0.0001) {
        return rgb;
    }
    vec3 hsv = rgbToHsv(rgb);
    float hueDistance = abs(hsv.x - uSelectiveColorTargetHue);
    hueDistance = min(hueDistance, 1.0 - hueDistance);
    float hueMask = 1.0 - smoothstep(uSelectiveColorHueRange * 0.72, min(uSelectiveColorHueRange * 1.16, 0.5), hueDistance);
    float satMask = smoothstep(
        uSelectiveColorSaturationFloor * 0.55,
        min(uSelectiveColorSaturationFloor + 0.10, 1.0),
        hsv.y
    );
    float colorMask = clamp(hueMask * satMask, 0.0, 1.0);
    float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
    float safeStrength = clamp(uSelectiveColorStrength, 0.0, 1.0);
    float desaturationAmount = clamp((1.0 - colorMask) * (0.66 + (0.26 * safeStrength)), 0.0, 1.0);
    vec3 isolated = mix(rgb, vec3(gray), desaturationAmount);
    float accentMask = smoothstep(0.18, 0.90, colorMask);
    float accentBoost = accentMask * (0.08 + (0.10 * safeStrength));
    float isolatedGray = dot(isolated, vec3(0.299, 0.587, 0.114));
    vec3 boosted = clamp(vec3(isolatedGray) + ((isolated - vec3(isolatedGray)) * (1.0 + accentBoost)), 0.0, 1.0);
    float lightLift = accentBoost * 0.08;
    return clamp(boosted + ((vec3(1.0) - boosted) * lightLift), 0.0, 1.0);
}
#endif

#if FEATURE_BLACK_MIST
vec3 sampleBlackMistBlur(vec2 uv) {
    vec2 texel = max(uSourceTexelSize, vec2(0.000001));
    vec3 center = sampleSourceWithDetail(uv) * 4.0;
    vec3 crossNear =
        sampleSourceWithDetail(clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0))) * 1.5 +
        sampleSourceWithDetail(clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))) * 1.5 +
        sampleSourceWithDetail(clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0))) * 1.5 +
        sampleSourceWithDetail(clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))) * 1.5;
    vec3 crossFar =
        sampleSourceWithDetail(clamp(uv + vec2(-texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))) * 0.75 +
        sampleSourceWithDetail(clamp(uv + vec2(texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))) * 0.75 +
        sampleSourceWithDetail(clamp(uv + vec2(0.0, -texel.y * 2.0), vec2(0.0), vec2(1.0))) * 0.75 +
        sampleSourceWithDetail(clamp(uv + vec2(0.0, texel.y * 2.0), vec2(0.0), vec2(1.0))) * 0.75;
    vec3 diagonals =
        sampleSourceWithDetail(clamp(uv + vec2(-texel.x, -texel.y), vec2(0.0), vec2(1.0))) +
        sampleSourceWithDetail(clamp(uv + vec2(texel.x, -texel.y), vec2(0.0), vec2(1.0))) +
        sampleSourceWithDetail(clamp(uv + vec2(-texel.x, texel.y), vec2(0.0), vec2(1.0))) +
        sampleSourceWithDetail(clamp(uv + vec2(texel.x, texel.y), vec2(0.0), vec2(1.0)));
    return clamp((center + crossNear + crossFar + diagonals) / 15.0, 0.0, 1.0);
}

vec3 applyBlackMist(vec2 uv, vec3 rgb) {
    if (uBlackMistEnabled == 0) {
        return rgb;
    }
    float safeStrength = clamp(uBlackMistStrength, 0.0, 1.0);
    vec3 blurred = sampleBlackMistBlur(uv);
    float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
    float highlight = smoothstep(0.56, 0.96, luma);
    float softnessMix = mix(0.12, 0.26, safeStrength);
    float bloomAmount = highlight * mix(0.16, 0.42, safeStrength);
    float veilLift = (1.0 - luma) * (0.015 + (safeStrength * 0.035));
    vec3 softened = mix(rgb, blurred, softnessMix);
    vec3 tint = vec3(0.965, 0.851, 0.933);
    vec3 glow = vec3(
        mix(blurred.r, tint.r, 0.22 + (highlight * 0.18)),
        mix(blurred.g, tint.g, 0.18 + (highlight * 0.12)),
        mix(blurred.b, tint.b, 0.28 + (highlight * 0.20))
    );
    vec3 dreamRgb = clamp(softened + (glow * bloomAmount) + vec3(veilLift), 0.0, 1.0);
    float saturationBoost = 1.0 + (safeStrength * 0.10) + (highlight * safeStrength * 0.18);
    return boostBlackMistSaturation(dreamRgb, saturationBoost);
}
#endif

void main() {
    vec4 sourceColor = texture2D(uTexture, vTexCoord);
    if (sourceColor.a <= 0.0) {
        gl_FragColor = sourceColor;
        return;
    }

    bool paletteEnabled = false;
#if FEATURE_PALETTE
    paletteEnabled = (uFilterEnabled != 0) && (uAnchorCount >= 1);
#endif
    bool haldEnabled = false;
#if FEATURE_HALD
    haldEnabled = (uHaldEnabled != 0) && (uHaldLevel >= 1.0);
#endif
    bool pixelPosterEnabled = false;
#if FEATURE_PIXEL_POSTER
    pixelPosterEnabled = (uPixelPosterEnabled != 0) &&
        ((uPixelPosterContinuousTone != 0) || (uPixelPosterBlockCountLongSide >= 1.0));
#endif
    bool subjectFrameEnabled = false;
#if FEATURE_SUBJECT_FRAME
    subjectFrameEnabled = (uSubjectFrameEnabled != 0);
#endif
    bool blackMistEnabled = false;
#if FEATURE_BLACK_MIST
    blackMistEnabled = (uBlackMistEnabled != 0);
#endif
    bool selectiveColorEnabled = false;
#if FEATURE_SELECTIVE_COLOR
    selectiveColorEnabled = (uSelectiveColorEnabled != 0) && (uSelectiveColorHueRange > 0.0001);
#endif
    bool portraitEnabled = false;
#if FEATURE_PORTRAIT
    portraitEnabled = (uPortraitEnabled != 0) && (uProtectedFaceCount > 0);
#endif
    bool colorEnabled = abs(uColor) > 0.0001;
    bool sharpenEnabled = abs(uSharpen) > 0.0001;
    bool toneEnabled = abs(uTone) > 0.0001;
    bool saturationEnabled = abs(uSaturation) > 0.0001;
    bool brightnessEnabled = abs(uBrightness) > 0.0001;
    bool portraitToneEnabled = false;
#if FEATURE_PORTRAIT
    portraitToneEnabled = portraitEnabled &&
        (
            (uPortraitLightnessLift > 0.0001) ||
                (uPortraitSaturationBoost > 0.0001) ||
                (uPortraitSoftenAmount > 0.0001) ||
                (uPortraitSharpenRetention < 0.9999)
            );
#endif
    if (!paletteEnabled && !haldEnabled && !pixelPosterEnabled && !subjectFrameEnabled && !blackMistEnabled && !selectiveColorEnabled && !portraitToneEnabled && !colorEnabled && !sharpenEnabled && !toneEnabled && !saturationEnabled && !brightnessEnabled) {
        gl_FragColor = sourceColor;
        return;
    }

    vec3 workingRgb = sampleSourceWithDetail(vTexCoord);
    float protectionAmount = skinProtectionAmount(sourceColor.rgb, vTexCoord);
    float softnessAmount = 0.0;
#if FEATURE_PORTRAIT
    softnessAmount = portraitEnabled ? portraitSoftnessAmount(sourceColor.rgb, vTexCoord) : 0.0;
    if (portraitEnabled && uPortraitSoftenAmount > 0.0001 && softnessAmount > 0.0001) {
        workingRgb = applyPortraitSoftness(vTexCoord, workingRgb, softnessAmount);
    }
#endif
    float paletteProtectionScale = 1.0 - (protectionAmount * ${COLOR_CARD_TRACE_SKIN_PROTECTION_PALETTE_RECOVERY});
    float creativeChromaScale = 1.0 - (protectionAmount * ${COLOR_CARD_TRACE_SKIN_PROTECTION_CREATIVE_RECOVERY});
    float creativeTonalScale = 1.0 - (protectionAmount * ${COLOR_CARD_TRACE_SKIN_PROTECTION_TONAL_RECOVERY});

#if FEATURE_PALETTE
    if (paletteEnabled) {
        vec3 inputLab = rgbToLab(workingRgb);
        float inputChroma = length(inputLab.yz);
        float weightedDeltaL = 0.0;
        float weightedDeltaA = 0.0;
        float weightedDeltaB = 0.0;
        float weightSum = 0.0;

        for (int i = 0; i < MAX_ANCHORS; i++) {
            if (i >= uAnchorCount) {
                break;
            }
            vec3 sourceAnchor = sourceAnchorAt(i);
            vec3 targetAnchor = targetAnchorAt(i);
            float deltaL = targetAnchor.x - sourceAnchor.x;
            float deltaA = targetAnchor.y - sourceAnchor.y;
            float deltaB = targetAnchor.z - sourceAnchor.z;
            float distanceL = (inputLab.x - sourceAnchor.x) * 0.78;
            float distanceA = inputLab.y - sourceAnchor.y;
            float distanceB = inputLab.z - sourceAnchor.z;
            float distanceSquared =
                (distanceL * distanceL) +
                (distanceA * distanceA) +
                (distanceB * distanceB);
            float weight = 1.0 / (distanceSquared + 144.0);
            weightedDeltaL += deltaL * weight;
            weightedDeltaA += deltaA * weight;
            weightedDeltaB += deltaB * weight;
            weightSum += weight;
        }

        float safeWeightSum = max(weightSum, 0.0001);
        float averagedDeltaL = weightedDeltaL / safeWeightSum;
        float averagedDeltaA = weightedDeltaA / safeWeightSum;
        float averagedDeltaB = weightedDeltaB / safeWeightSum;
        float chromaInfluence = inputChroma < 8.0
            ? 0.58
            : (inputChroma < 24.0 ? 0.78 : 0.96);
        float lightnessInfluence = inputChroma < 8.0 ? 0.90 : 0.98;

        vec3 mappedLab = vec3(
            clamp(inputLab.x + (averagedDeltaL * lightnessInfluence), 0.0, 100.0),
            inputLab.y + (averagedDeltaA * chromaInfluence),
            inputLab.z + (averagedDeltaB * chromaInfluence)
        );
        float protectedBlendStrength = clamp(uBlendStrength * paletteProtectionScale, 0.0, 1.0);
        float protectedMaxLightnessShift = uMaxLightnessShift * paletteProtectionScale;
        float protectedMaxChromaShift = uMaxChromaShift * paletteProtectionScale;
        vec3 softenedLab = mix(inputLab, mappedLab, protectedBlendStrength);
        softenedLab.x = clamp(
            inputLab.x + clamp(softenedLab.x - inputLab.x, -protectedMaxLightnessShift, protectedMaxLightnessShift),
            0.0,
            100.0
        );
        softenedLab.y = inputLab.y + clamp(
            softenedLab.y - inputLab.y,
            -protectedMaxChromaShift,
            protectedMaxChromaShift
        );
        softenedLab.z = inputLab.z + clamp(
            softenedLab.z - inputLab.z,
            -protectedMaxChromaShift,
            protectedMaxChromaShift
        );
        workingRgb = labToRgb(softenedLab);
    }
#endif

#if FEATURE_HALD
    if (haldEnabled) {
        vec3 haldRgb = applyHaldClut(workingRgb);
        workingRgb = mix(workingRgb, haldRgb, clamp(uHaldStrength * paletteProtectionScale, 0.0, 1.0));
    }
#endif

#if FEATURE_PIXEL_POSTER
    if (pixelPosterEnabled) {
        vec3 pixelPosterRgb = applyPixelPoster(vTexCoord);
        workingRgb = mix(
            workingRgb,
            pixelPosterRgb,
            clamp(uPixelPosterStrength * paletteProtectionScale, 0.0, 1.0)
        );
    }
#endif

#if FEATURE_BLACK_MIST
    if (blackMistEnabled) {
        workingRgb = applyBlackMist(vTexCoord, workingRgb);
    }
#endif

    if (colorEnabled || toneEnabled || saturationEnabled || brightnessEnabled) {
        workingRgb = applyCreativeAdjustments(
            workingRgb,
            uColor * creativeChromaScale,
            uTone * creativeTonalScale,
            uSaturation * creativeChromaScale,
            uBrightness * creativeTonalScale
        );
    }

#if FEATURE_PORTRAIT
    if (portraitToneEnabled && protectionAmount > 0.0001) {
        workingRgb = applyPortraitAdjustments(workingRgb, protectionAmount);
    }
#endif

#if FEATURE_SELECTIVE_COLOR
    if (selectiveColorEnabled) {
        workingRgb = applySelectiveColor(workingRgb);
    }
#endif

#if FEATURE_SUBJECT_FRAME
    if (subjectFrameEnabled) {
        workingRgb = applySubjectFrame(vTexCoord, workingRgb);
    }
#endif

#if FEATURE_PIXEL_POSTER
    if (pixelPosterEnabled && uPixelPosterGridLineOpacity > 0.0 && uPixelPosterGridLineWidth > 0.0) {
        workingRgb = applyPixelPosterGridOverlay(
            workingRgb,
            pixelPosterGridMask(vTexCoord, pixelPosterGrid())
        );
    }
#endif

    gl_FragColor = vec4(clamp(workingRgb, 0.0, 1.0), sourceColor.a);
}
""".trimIndent()
