package com.voicelike.app.motionphoto

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLUtils
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.SingleFrameGlShaderProgram
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.voicelike.app.COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES
import com.voicelike.app.COLOR_CARD_TRACE_VERTEX_SHADER
import com.voicelike.app.ColorCardTraceFilterKind
import com.voicelike.app.ColorCardTraceFilterState
import com.voicelike.app.ColorCardTraceGpuParams
import com.voicelike.app.MediaItem
import com.voicelike.app.MotionPhotoHelper
import com.voicelike.app.buildColorCardTraceFragmentShader
import com.voicelike.app.resolveColorCardTraceGpuShaderFeatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
internal object ColorCardTraceVideoGpuExportSupport {
    fun supports(traceState: ColorCardTraceFilterState): Boolean {
        val candidateKind = traceState.selectedCandidate?.kind
        if (traceState.filmGrainEnabled && traceState.filmGrainEligible) return false
        return candidateKind != ColorCardTraceFilterKind.Palette &&
            candidateKind != ColorCardTraceFilterKind.AiMnn &&
            candidateKind != ColorCardTraceFilterKind.MotionPhotoEntry
    }

    suspend fun export(
        context: Context,
        sourceItem: MediaItem,
        traceState: ColorCardTraceFilterState,
        outputFile: File,
        spec: MotionPhotoRenderSpec
    ): Boolean {
        val sourceUri = resolveSourceUri(context, sourceItem) ?: return false
        return transformVideo(
            context = context.applicationContext,
            sourceUri = sourceUri,
            traceState = traceState,
            outputFile = outputFile,
            spec = spec
        )
    }

    private fun resolveSourceUri(
        context: Context,
        sourceItem: MediaItem
    ): Uri? {
        sourceItem.livePhotoVideoUri?.let { return it }
        if (!sourceItem.isMotionPhoto) return null
        val extractedFile = MotionPhotoHelper.extractVideo(
            context = context,
            uri = sourceItem.uri,
            info = MotionPhotoHelper.MotionPhotoInfo(
                isMotionPhoto = true,
                videoOffset = sourceItem.motionPhotoVideoOffset,
                videoLength = sourceItem.motionPhotoVideoLength
            )
        ) ?: return null
        return Uri.fromFile(extractedFile)
    }

    private suspend fun transformVideo(
        context: Context,
        sourceUri: Uri,
        traceState: ColorCardTraceFilterState,
        outputFile: File,
        spec: MotionPhotoRenderSpec
    ): Boolean = withContext(Dispatchers.Main) {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) {
            outputFile.delete()
        }
        val effect = ColorCardTraceGlEffect(
            baseParams = traceState.gpuParams,
            maxExportLongSide = spec.maxExportLongSide
        )
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setEncoderFactory(
                DefaultEncoderFactory.Builder(context)
                    .setEnableFallback(true)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder()
                            .setBitrate(spec.videoBitrate)
                            .setiFrameIntervalSeconds(1f)
                            .build()
                    )
                    .build()
            )
            .build()
        val editedMediaItem = EditedMediaItem.Builder(ExoMediaItem.fromUri(sourceUri))
            .setRemoveAudio(true)
            .setFrameRate(spec.frameRate)
            .setEffects(Effects(emptyList(), listOf<Effect>(effect)))
            .build()

        suspendCancellableCoroutine { continuation ->
            val listener = object : Transformer.Listener {
                override fun onCompleted(
                    composition: androidx.media3.transformer.Composition,
                    exportResult: ExportResult
                ) {
                    transformer.removeListener(this)
                    continuation.resume(outputFile.exists() && outputFile.length() > 0L)
                }

                override fun onError(
                    composition: androidx.media3.transformer.Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    transformer.removeListener(this)
                    runCatching { outputFile.delete() }
                    continuation.resume(false)
                }
            }
            try {
                transformer.addListener(listener)
                transformer.start(editedMediaItem, outputFile.absolutePath)
            } catch (_: Throwable) {
                transformer.removeListener(listener)
                runCatching { outputFile.delete() }
                continuation.resume(false)
            }
            continuation.invokeOnCancellation {
                transformer.removeListener(listener)
                transformer.cancel()
                runCatching { outputFile.delete() }
            }
        }
    }.let { success ->
        if (!success) {
            runCatching { outputFile.delete() }
            return@let false
        }
        waitForGeneratedVideoFile(outputFile)
    }

    private suspend fun waitForGeneratedVideoFile(outputFile: File): Boolean {
        repeat(20) { attempt ->
            if (outputFile.exists() && outputFile.length() > 0L) {
                return true
            }
            delay(if (attempt < 5) 30L else 60L)
        }
        runCatching { outputFile.delete() }
        return false
    }
}

@OptIn(UnstableApi::class)
private class ColorCardTraceGlEffect(
    private val baseParams: ColorCardTraceGpuParams,
    private val maxExportLongSide: Int
) : GlEffect {
    override fun toGlShaderProgram(
        context: Context,
        useHdr: Boolean
    ): GlShaderProgram {
        return ColorCardTraceSingleFrameShaderProgram(
            baseParams = baseParams,
            maxExportLongSide = maxExportLongSide
        )
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

@OptIn(UnstableApi::class)
private class ColorCardTraceSingleFrameShaderProgram(
    private val baseParams: ColorCardTraceGpuParams,
    private val maxExportLongSide: Int
) : SingleFrameGlShaderProgram(false) {
    private val glProgram = GlProgram(
        COLOR_CARD_TRACE_VERTEX_SHADER,
        buildColorCardTraceFragmentShader(resolveColorCardTraceGpuShaderFeatures(baseParams))
    )
    private val shaderFeatures = resolveColorCardTraceGpuShaderFeatures(baseParams)
    private var configuredParams = baseParams
    private var outputWidth = 0
    private var outputHeight = 0
    private var haldTextureId = 0
    private var haldSecondaryTextureId = 0

    init {
        glProgram.setBufferAttribute("aPosition", NORMALIZED_DEVICE_COORDINATES, 2)
        glProgram.setBufferAttribute("aTexCoord", TEXTURE_COORDINATES, 2)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        val safeInputWidth = inputWidth.coerceAtLeast(2)
        val safeInputHeight = inputHeight.coerceAtLeast(2)
        val scale = minOf(
            1f,
            maxExportLongSide.coerceAtLeast(240).toFloat() /
                maxOf(safeInputWidth, safeInputHeight).toFloat()
        )
        outputWidth = ((safeInputWidth * scale).toInt()).coerceAtLeast(2).toEven()
        outputHeight = ((safeInputHeight * scale).toInt()).coerceAtLeast(2).toEven()
        configuredParams = baseParams.copy(
            sourceAspect = safeInputWidth.toFloat() / safeInputHeight.toFloat(),
            sourceTexelWidth = 1f / safeInputWidth.toFloat(),
            sourceTexelHeight = 1f / safeInputHeight.toFloat()
        )
        ensureOptionalTextures()
        return Size(outputWidth, outputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        GlUtil.checkGlError()
        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)
        glProgram.use()
        glProgram.setSamplerTexIdUniform("uTexture", inputTexId, 0)
        if (shaderFeatures.hald && haldTextureId != 0) {
            glProgram.setSamplerTexIdUniform("uHaldTexture", haldTextureId, 1)
            glProgram.setSamplerTexIdUniform(
                "uHaldTextureSecondary",
                if (haldSecondaryTextureId != 0) haldSecondaryTextureId else haldTextureId,
                2
            )
        }
        applyParams(configuredParams)
        glProgram.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError()
    }

    override fun release() {
        runCatching { glProgram.delete() }
        if (haldTextureId != 0) {
            runCatching { GlUtil.deleteTexture(haldTextureId) }
            haldTextureId = 0
        }
        if (haldSecondaryTextureId != 0) {
            runCatching { GlUtil.deleteTexture(haldSecondaryTextureId) }
            haldSecondaryTextureId = 0
        }
        super.release()
    }

    private fun ensureOptionalTextures() {
        val primaryHaldBitmap = configuredParams.haldBitmap
        if (shaderFeatures.hald && primaryHaldBitmap != null && haldTextureId == 0) {
            haldTextureId = createTextureFromBitmap(primaryHaldBitmap, GLES20.GL_NEAREST)
        }
        val secondaryHaldBitmap = configuredParams.haldSecondaryBitmap
        if (
            shaderFeatures.hald &&
            secondaryHaldBitmap != null &&
            haldSecondaryTextureId == 0
        ) {
            haldSecondaryTextureId = createTextureFromBitmap(
                secondaryHaldBitmap,
                GLES20.GL_NEAREST
            )
        }
    }

    private fun createTextureFromBitmap(
        bitmap: Bitmap,
        filter: Int
    ): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GlUtil.checkGlError()
        return textureId
    }

    private fun applyParams(params: ColorCardTraceGpuParams) {
        if (shaderFeatures.palette) {
            glProgram.setIntUniform("uFilterEnabled", if (params.filterEnabled) 1 else 0)
            glProgram.setIntUniform(
                "uAnchorCount",
                params.anchorCount.coerceIn(0, com.voicelike.app.COLOR_CARD_TRACE_FILTER_MAX_ANCHORS)
            )
            glProgram.setFloatsUniform("uSourceAnchors", params.sourceAnchors)
            glProgram.setFloatsUniform("uTargetAnchors", params.targetAnchors)
            glProgram.setFloatUniform("uBlendStrength", params.blendStrength.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uMaxLightnessShift", params.maxLightnessShift.coerceAtLeast(0f))
            glProgram.setFloatUniform("uMaxChromaShift", params.maxChromaShift.coerceAtLeast(0f))
        }
        glProgram.setFloatUniform("uColor", params.color.coerceIn(-1f, 1f))
        glProgram.setFloatsUniform(
            "uColorAxis",
            floatArrayOf(params.colorAxisA.coerceIn(-1f, 1f), params.colorAxisB.coerceIn(-1f, 1f))
        )
        glProgram.setFloatUniform("uSharpen", params.sharpen.coerceIn(-1f, 1f))
        glProgram.setFloatUniform("uTone", params.tone.coerceIn(-1f, 1f))
        glProgram.setFloatUniform("uSaturation", params.saturation.coerceIn(-1f, 1f))
        glProgram.setFloatUniform("uBrightness", params.brightness.coerceIn(-1f, 1f))
        glProgram.setIntUniform(
            "uProtectedFaceCount",
            params.protectedFaceCount.coerceIn(0, COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES)
        )
        glProgram.setFloatsUniform("uProtectedFaces", params.protectedFaces)
        if (shaderFeatures.portrait) {
            glProgram.setIntUniform("uPortraitEnabled", if (params.portraitEnabled) 1 else 0)
            glProgram.setFloatUniform("uPortraitLightnessLift", params.portraitLightnessLift.coerceIn(0f, 12f))
            glProgram.setFloatUniform("uPortraitSaturationBoost", params.portraitSaturationBoost.coerceIn(0f, 0.3f))
            glProgram.setFloatUniform("uPortraitSharpenRetention", params.portraitSharpenRetention.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uPortraitSoftenAmount", params.portraitSoftenAmount.coerceIn(0f, 1f))
        }
        if (shaderFeatures.hald) {
            glProgram.setIntUniform("uHaldEnabled", if (params.haldEnabled && params.haldBitmap != null) 1 else 0)
            glProgram.setFloatUniform("uHaldLevel", params.haldLevel.toFloat().coerceAtLeast(0f))
            glProgram.setFloatUniform("uHaldSecondaryLevel", params.haldSecondaryLevel.toFloat().coerceAtLeast(0f))
            glProgram.setFloatUniform("uHaldStrength", params.haldStrength.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uHaldVariantMix", params.haldVariantMix.coerceIn(0f, 1f))
        }
        if (shaderFeatures.pixelPoster) {
            glProgram.setIntUniform("uPixelPosterEnabled", if (params.pixelPosterEnabled) 1 else 0)
            glProgram.setIntUniform("uPixelPosterUseSourceColor", if (params.pixelPosterUseSourceColor) 1 else 0)
            glProgram.setIntUniform("uPixelPosterContinuousTone", if (params.pixelPosterContinuousTone) 1 else 0)
            glProgram.setFloatUniform("uPixelPosterBlockCountLongSide", params.pixelPosterBlockCountLongSide.coerceAtLeast(0f))
            glProgram.setFloatUniform("uPixelPosterThreshold", params.pixelPosterThreshold.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uPixelPosterContrast", params.pixelPosterContrast.coerceAtLeast(0f))
            glProgram.setFloatUniform("uPixelPosterNoiseAmount", params.pixelPosterNoiseAmount.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uPixelPosterGridLineWidth", params.pixelPosterGridLineWidth.coerceIn(0f, 0.45f))
            glProgram.setFloatUniform("uPixelPosterGridLineOpacity", params.pixelPosterGridLineOpacity.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uPixelPosterDetailBoost", params.pixelPosterDetailBoost.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uPixelPosterShadowDetailBias", params.pixelPosterShadowDetailBias.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uPixelPosterStrength", params.pixelPosterStrength.coerceIn(0f, 1f))
            glProgram.setFloatsUniform(
                "uPixelPosterDarkColor",
                floatArrayOf(params.pixelPosterDarkRed, params.pixelPosterDarkGreen, params.pixelPosterDarkBlue)
            )
            glProgram.setFloatsUniform(
                "uPixelPosterLightColor",
                floatArrayOf(params.pixelPosterLightRed, params.pixelPosterLightGreen, params.pixelPosterLightBlue)
            )
        }
        if (shaderFeatures.subjectFrame) {
            glProgram.setIntUniform("uSubjectFrameEnabled", if (params.subjectFrameEnabled) 1 else 0)
            glProgram.setFloatsUniform("uSubjectFrameRect", params.subjectFrameRect)
            glProgram.setFloatUniform("uSubjectFrameBorderThickness", params.subjectFrameBorderThickness.coerceAtLeast(0f))
            glProgram.setFloatUniform("uSubjectFrameStrength", params.subjectFrameStrength.coerceIn(0f, 1f))
            glProgram.setFloatsUniform(
                "uSubjectFrameBorderColor",
                floatArrayOf(params.subjectFrameBorderRed, params.subjectFrameBorderGreen, params.subjectFrameBorderBlue)
            )
        }
        if (shaderFeatures.blackMist) {
            glProgram.setIntUniform("uBlackMistEnabled", if (params.blackMistEnabled) 1 else 0)
            glProgram.setFloatUniform("uBlackMistStrength", params.blackMistStrength.coerceIn(0f, 1f))
        }
        if (shaderFeatures.selectiveColor) {
            glProgram.setIntUniform("uSelectiveColorEnabled", if (params.selectiveColorEnabled) 1 else 0)
            glProgram.setFloatUniform("uSelectiveColorTargetHue", params.selectiveColorTargetHue.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uSelectiveColorHueRange", params.selectiveColorHueRange.coerceIn(0f, 0.5f))
            glProgram.setFloatUniform("uSelectiveColorSaturationFloor", params.selectiveColorSaturationFloor.coerceIn(0f, 1f))
            glProgram.setFloatUniform("uSelectiveColorStrength", params.selectiveColorStrength.coerceIn(0f, 1f))
        }
        glProgram.setFloatUniform("uSourceAspect", params.sourceAspect.coerceAtLeast(0.0001f))
        glProgram.setFloatsUniform(
            "uSourceTexelSize",
            floatArrayOf(
                params.sourceTexelWidth.coerceAtLeast(0.000001f),
                params.sourceTexelHeight.coerceAtLeast(0.000001f)
            )
        )
    }

    private fun Int.toEven(): Int = if (this % 2 == 0) this else (this - 1).coerceAtLeast(2)
}

private val NORMALIZED_DEVICE_COORDINATES = floatArrayOf(
    -1f, -1f,
    1f, -1f,
    -1f, 1f,
    1f, 1f
)

private val TEXTURE_COORDINATES = floatArrayOf(
    0f, 1f,
    1f, 1f,
    0f, 0f,
    1f, 0f
)
