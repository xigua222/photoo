package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.max
import kotlin.math.roundToInt

private const val COLOR_CARD_CAMERA_FACE_DETECTION_MAX_SIDE = 960

internal suspend fun renderColorCardCameraFastBitmap(
    context: Context,
    sourceFile: File,
    filterState: ColorCardCameraFilterUiState,
    renderAdjustments: ColorCardCameraRenderAdjustments,
    maxSide: Int
): Bitmap? = withContext(Dispatchers.Default) {
    val sourceBitmap = decodeColorCardCameraFastSourceBitmap(sourceFile, maxSide) ?: return@withContext null
    try {
        renderColorCardCameraFastBitmapFromBitmap(
            context = context,
            sourceBitmap = sourceBitmap,
            filterState = filterState,
            renderAdjustments = renderAdjustments
        )
    } finally {
        if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
    }
}

internal suspend fun renderColorCardCameraFastBitmapFromBitmap(
    context: Context,
    sourceBitmap: Bitmap,
    filterState: ColorCardCameraFilterUiState,
    renderAdjustments: ColorCardCameraRenderAdjustments
): Bitmap? = withContext(Dispatchers.Default) {
    val faceDetectionBitmap = buildColorCardCameraFaceDetectionBitmap(sourceBitmap)
    val faceRegions = try {
        detectColorCardCameraFaceRegions(faceDetectionBitmap)
    } finally {
        if (faceDetectionBitmap !== sourceBitmap && !faceDetectionBitmap.isRecycled) {
            faceDetectionBitmap.recycle()
        }
    }
    val params = buildColorCardCameraRenderParams(
        filterState = filterState,
        sourceWidth = sourceBitmap.width,
        sourceHeight = sourceBitmap.height,
        faceRegions = faceRegions,
        portraitAdjustments = renderAdjustments.portraitAdjustments,
        renderAdjustments = renderAdjustments
    )
    val rendered = ColorCardCameraBitmapGpuRenderer.render(sourceBitmap, params) ?: return@withContext null
    if (
        filterState.filmGrainEnabled &&
        filterState.filmGrainEligible
    ) {
        val grainSourceBitmap = filterState.selectedFilmGrainBitmap
        val grainBitmap = if (grainSourceBitmap != null && !grainSourceBitmap.isRecycled) {
            grainSourceBitmap
        } else {
            filterState.selectedFilmGrainAsset?.assetName?.let {
                decodeColorCardTraceFilmGrainBitmap(context, it)
            }
        }
        if (grainBitmap != null) {
            try {
                return@withContext applyColorCardTraceFilmGrainOverlay(rendered, grainBitmap)
                    .also {
                        if (it !== rendered && !rendered.isRecycled) rendered.recycle()
                    }
            } finally {
                if (grainBitmap !== grainSourceBitmap && !grainBitmap.isRecycled) {
                    grainBitmap.recycle()
                }
            }
        }
    }
    rendered
}

private fun buildColorCardCameraFaceDetectionBitmap(sourceBitmap: Bitmap): Bitmap {
    val currentMaxSide = max(sourceBitmap.width, sourceBitmap.height).coerceAtLeast(1)
    if (currentMaxSide <= COLOR_CARD_CAMERA_FACE_DETECTION_MAX_SIDE) {
        return sourceBitmap
    }
    val scale = COLOR_CARD_CAMERA_FACE_DETECTION_MAX_SIDE.toFloat() / currentMaxSide.toFloat()
    return Bitmap.createScaledBitmap(
        sourceBitmap,
        (sourceBitmap.width * scale).roundToInt().coerceAtLeast(1),
        (sourceBitmap.height * scale).roundToInt().coerceAtLeast(1),
        true
    )
}

private suspend fun decodeColorCardCameraFastSourceBitmap(
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
    val sampleSize = buildColorCardCameraSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
    val decoded = BitmapFactory.decodeFile(
        sourceFile.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    ) ?: return@withContext null
    val oriented = applyColorCardCameraFastExifOrientation(
        bitmap = decoded,
        orientation = readColorCardCameraFastExifOrientation(sourceFile)
    )
    val currentMax = max(oriented.width, oriented.height)
    if (currentMax <= maxSide) return@withContext oriented
    val scale = maxSide.toFloat() / currentMax.toFloat()
    val targetWidth = (oriented.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (oriented.height * scale).roundToInt().coerceAtLeast(1)
    Bitmap.createScaledBitmap(oriented, targetWidth, targetHeight, true).also {
        if (it !== oriented) oriented.recycle()
    }
}

private fun readColorCardCameraFastExifOrientation(sourceFile: File): Int {
    return runCatching {
        ExifInterface(sourceFile.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
}

private fun applyColorCardCameraFastExifOrientation(
    bitmap: Bitmap,
    orientation: Int
): Bitmap {
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
    if (rotated !== bitmap) {
        bitmap.recycle()
    }
    return rotated
}

private fun buildColorCardCameraSampleSize(
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

private object ColorCardCameraBitmapGpuRenderer {
    fun render(
        sourceBitmap: Bitmap,
        params: ColorCardTraceGpuParams
    ): Bitmap? {
        var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        var programId = 0
        var sourceTextureId = 0
        var haldTextureId = 0
        var haldSecondaryTextureId = 0

        return runCatching {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            EGL14.eglChooseConfig(
                eglDisplay,
                intArrayOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
                ),
                0,
                configs,
                0,
                1,
                configCount,
                0
            )
            val eglConfig = configs[0]
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0
            )
            eglSurface = EGL14.eglCreatePbufferSurface(
                eglDisplay,
                eglConfig,
                intArrayOf(
                    EGL14.EGL_WIDTH, sourceBitmap.width,
                    EGL14.EGL_HEIGHT, sourceBitmap.height,
                    EGL14.EGL_NONE
                ),
                0
            )
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            programId = createProgram(COLOR_CARD_CAMERA_BITMAP_VERTEX_SHADER, COLOR_CARD_CAMERA_BITMAP_FRAGMENT_SHADER)
            val positionLocation = GLES20.glGetAttribLocation(programId, "aPosition")
            val texCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord")
            val textureUniformLocation = GLES20.glGetUniformLocation(programId, "uTexture")
            val colorLocation = GLES20.glGetUniformLocation(programId, "uColor")
            val colorAxisLocation = GLES20.glGetUniformLocation(programId, "uColorAxis")
            val sharpenLocation = GLES20.glGetUniformLocation(programId, "uSharpen")
            val toneLocation = GLES20.glGetUniformLocation(programId, "uTone")
            val saturationLocation = GLES20.glGetUniformLocation(programId, "uSaturation")
            val brightnessLocation = GLES20.glGetUniformLocation(programId, "uBrightness")
            val protectedFaceCountLocation = GLES20.glGetUniformLocation(programId, "uProtectedFaceCount")
            val protectedFacesLocation = GLES20.glGetUniformLocation(programId, "uProtectedFaces")
            val portraitEnabledLocation = GLES20.glGetUniformLocation(programId, "uPortraitEnabled")
            val portraitLightnessLiftLocation = GLES20.glGetUniformLocation(programId, "uPortraitLightnessLift")
            val portraitSaturationBoostLocation = GLES20.glGetUniformLocation(programId, "uPortraitSaturationBoost")
            val portraitSharpenRetentionLocation = GLES20.glGetUniformLocation(programId, "uPortraitSharpenRetention")
            val portraitSoftenAmountLocation = GLES20.glGetUniformLocation(programId, "uPortraitSoftenAmount")
            val haldEnabledLocation = GLES20.glGetUniformLocation(programId, "uHaldEnabled")
            val haldTextureUniformLocation = GLES20.glGetUniformLocation(programId, "uHaldTexture")
            val haldSecondaryTextureUniformLocation = GLES20.glGetUniformLocation(programId, "uHaldTextureSecondary")
            val haldLevelLocation = GLES20.glGetUniformLocation(programId, "uHaldLevel")
            val haldSecondaryLevelLocation = GLES20.glGetUniformLocation(programId, "uHaldSecondaryLevel")
            val haldStrengthLocation = GLES20.glGetUniformLocation(programId, "uHaldStrength")
            val haldVariantMixLocation = GLES20.glGetUniformLocation(programId, "uHaldVariantMix")
            val blackMistEnabledLocation = GLES20.glGetUniformLocation(programId, "uBlackMistEnabled")
            val blackMistStrengthLocation = GLES20.glGetUniformLocation(programId, "uBlackMistStrength")
            val sparkleStrengthLocation = GLES20.glGetUniformLocation(programId, "uSparkleStrength")
            val dehazeStrengthLocation = GLES20.glGetUniformLocation(programId, "uDehazeStrength")
            val blackMistSoftnessBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistSoftnessBias")
            val blackMistBloomBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistBloomBias")
            val blackMistVeilBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistVeilBias")
            val blackMistHighlightBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistHighlightBias")
            val blackMistContrastBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistContrastBias")
            val blackMistRadiusBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistRadiusBias")
            val blackMistHaloBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistHaloBias")
            val blackMistAtmosphereBiasLocation = GLES20.glGetUniformLocation(programId, "uBlackMistAtmosphereBias")
            val blackMistTintLocation = GLES20.glGetUniformLocation(programId, "uBlackMistTint")
            val sourceAspectLocation = GLES20.glGetUniformLocation(programId, "uSourceAspect")
            val sourceTexelSizeLocation = GLES20.glGetUniformLocation(programId, "uSourceTexelSize")

            sourceTextureId = createTexture2d(linear = true)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTextureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, sourceBitmap, 0)

            if (params.haldBitmap != null) {
                haldTextureId = createTexture2d(linear = false)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldTextureId)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, params.haldBitmap, 0)
            }
            if (params.haldSecondaryBitmap != null) {
                haldSecondaryTextureId = createTexture2d(linear = false)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldSecondaryTextureId)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, params.haldSecondaryBitmap, 0)
            }

            GLES20.glViewport(0, 0, sourceBitmap.width, sourceBitmap.height)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(programId)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTextureId)
            GLES20.glUniform1i(textureUniformLocation, 0)
            GLES20.glUniform1f(colorLocation, params.color.coerceIn(-1f, 1f))
            GLES20.glUniform2f(colorAxisLocation, params.colorAxisA, params.colorAxisB)
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
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldTextureId)
            GLES20.glUniform1i(haldTextureUniformLocation, 1)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, haldSecondaryTextureId)
            GLES20.glUniform1i(haldSecondaryTextureUniformLocation, 2)
            GLES20.glUniform1f(haldLevelLocation, params.haldLevel.toFloat())
            GLES20.glUniform1f(haldSecondaryLevelLocation, params.haldSecondaryLevel.toFloat())
            GLES20.glUniform1f(haldStrengthLocation, params.haldStrength.coerceIn(0f, 1f))
            GLES20.glUniform1f(haldVariantMixLocation, params.haldVariantMix.coerceIn(0f, 1f))
            GLES20.glUniform1i(blackMistEnabledLocation, if (params.blackMistEnabled) 1 else 0)
            GLES20.glUniform1f(blackMistStrengthLocation, params.blackMistStrength.coerceIn(0f, 1f))
            GLES20.glUniform1f(sparkleStrengthLocation, params.sparkleStrength.coerceIn(0f, 1f))
            GLES20.glUniform1f(dehazeStrengthLocation, params.dehazeStrength.coerceIn(0f, 1f))
            GLES20.glUniform1f(blackMistSoftnessBiasLocation, params.blackMistSoftnessBias.coerceIn(-0.2f, 0.2f))
            GLES20.glUniform1f(blackMistBloomBiasLocation, params.blackMistBloomBias.coerceIn(-0.2f, 0.3f))
            GLES20.glUniform1f(blackMistVeilBiasLocation, params.blackMistVeilBias.coerceIn(-0.03f, 0.05f))
            GLES20.glUniform1f(blackMistHighlightBiasLocation, params.blackMistHighlightBias.coerceIn(-0.25f, 0.25f))
            GLES20.glUniform1f(blackMistContrastBiasLocation, params.blackMistContrastBias.coerceIn(-0.4f, 0.4f))
            GLES20.glUniform1f(blackMistRadiusBiasLocation, params.blackMistRadiusBias.coerceIn(-0.4f, 0.6f))
            GLES20.glUniform1f(blackMistHaloBiasLocation, params.blackMistHaloBias.coerceIn(-0.5f, 0.6f))
            GLES20.glUniform1f(blackMistAtmosphereBiasLocation, params.blackMistAtmosphereBias.coerceIn(-0.4f, 0.6f))
            GLES20.glUniform3f(
                blackMistTintLocation,
                params.blackMistTintRed.coerceIn(0f, 1f),
                params.blackMistTintGreen.coerceIn(0f, 1f),
                params.blackMistTintBlue.coerceIn(0f, 1f)
            )
            GLES20.glUniform1f(sourceAspectLocation, params.sourceAspect)
            GLES20.glUniform2f(sourceTexelSizeLocation, params.sourceTexelWidth, params.sourceTexelHeight)

            COLOR_CARD_CAMERA_BITMAP_QUAD.position(0)
            GLES20.glEnableVertexAttribArray(positionLocation)
            GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, COLOR_CARD_CAMERA_BITMAP_VERTEX_STRIDE_BYTES, COLOR_CARD_CAMERA_BITMAP_QUAD)
            COLOR_CARD_CAMERA_BITMAP_QUAD.position(2)
            GLES20.glEnableVertexAttribArray(texCoordLocation)
            GLES20.glVertexAttribPointer(texCoordLocation, 2, GLES20.GL_FLOAT, false, COLOR_CARD_CAMERA_BITMAP_VERTEX_STRIDE_BYTES, COLOR_CARD_CAMERA_BITMAP_QUAD)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            val buffer = ByteBuffer.allocateDirect(sourceBitmap.width * sourceBitmap.height * 4).order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(0, 0, sourceBitmap.width, sourceBitmap.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
            buffer.rewind()
            val pixels = IntArray(sourceBitmap.width * sourceBitmap.height)
            buffer.asIntBuffer().get(pixels)
            for (index in pixels.indices) {
                val color = pixels[index]
                val a = color and -0x1000000
                val b = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val r = color and 0xFF
                pixels[index] = a or (r shl 16) or (g shl 8) or b
            }
            MatrixFlip.vertical(
                Bitmap.createBitmap(
                    pixels,
                    sourceBitmap.width,
                    sourceBitmap.height,
                    Bitmap.Config.ARGB_8888
                )
            )
        }.getOrNull().also {
            if (sourceTextureId != 0) GLES20.glDeleteTextures(1, intArrayOf(sourceTextureId), 0)
            if (haldTextureId != 0) GLES20.glDeleteTextures(1, intArrayOf(haldTextureId), 0)
            if (haldSecondaryTextureId != 0) GLES20.glDeleteTextures(1, intArrayOf(haldSecondaryTextureId), 0)
            if (programId != 0) GLES20.glDeleteProgram(programId)
            if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface !== EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                if (eglContext !== EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
            }
        }
    }

    private fun createTexture2d(linear: Boolean): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, if (linear) GLES20.GL_LINEAR else GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, if (linear) GLES20.GL_LINEAR else GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textureId
    }

    private fun createProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
        val vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        return GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShaderId)
            GLES20.glAttachShader(program, fragmentShaderId)
            GLES20.glLinkProgram(program)
            GLES20.glDeleteShader(vertexShaderId)
            GLES20.glDeleteShader(fragmentShaderId)
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
        }
    }
}

private object MatrixFlip {
    fun vertical(bitmap: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix().apply { postScale(1f, -1f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false).also {
            if (it !== bitmap && !bitmap.isRecycled) bitmap.recycle()
        }
    }
}

private const val COLOR_CARD_CAMERA_BITMAP_VERTEX_STRIDE_BYTES = 4 * 4

private val COLOR_CARD_CAMERA_BITMAP_QUAD: FloatBuffer = ByteBuffer
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

private val COLOR_CARD_CAMERA_BITMAP_VERTEX_SHADER = """
attribute vec2 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
    vTexCoord = aTexCoord;
}
""".trimIndent()

private val COLOR_CARD_CAMERA_BITMAP_FRAGMENT_SHADER = """
precision highp float;
uniform sampler2D uTexture;
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

const int MAX_PROTECTED_FACES = ${COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES};

float faceProtectionMask(vec2 uv);
float skinToneMask(vec3 rgb);
float skinProtectionAmount(vec3 rgb, vec2 uv);
float portraitDetailAmount(vec3 rgb, vec2 uv);
float portraitSoftnessAmount(vec3 rgb, vec2 uv);

vec2 clampSourceUv(vec2 uv) {
  return clamp(uv, vec2(0.001), vec2(0.999));
}

vec3 sampleSource(vec2 uv) {
  return texture2D(uTexture, clampSourceUv(uv)).rgb;
}

float srgbToLinear(float channel) { if (channel <= 0.04045) return channel / 12.92; return pow((channel + 0.055) / 1.055, 2.4); }
float linearToSrgb(float channel) { float c = clamp(channel, 0.0, 1.0); if (c <= 0.0031308) return c * 12.92; return (1.055 * pow(c, 1.0 / 2.4)) - 0.055; }
float labPivot(float value) { if (value > 0.008856) return pow(value, 1.0 / 3.0); return (7.787 * value) + (16.0 / 116.0); }
float inverseLabPivot(float value) { float cube = value * value * value; if (cube > 0.008856) return cube; return (value - (16.0 / 116.0)) / 7.787; }
vec3 rgbToLab(vec3 rgb) {
  float rf = srgbToLinear(rgb.r); float gf = srgbToLinear(rgb.g); float bf = srgbToLinear(rgb.b);
  float x = (rf * 0.4124564) + (gf * 0.3575761) + (bf * 0.1804375);
  float y = (rf * 0.2126729) + (gf * 0.7151522) + (bf * 0.0721750);
  float z = (rf * 0.0193339) + (gf * 0.1191920) + (bf * 0.9503041);
  float xp = labPivot(x / 0.95047); float yp = labPivot(y / 1.00000); float zp = labPivot(z / 1.08883);
  return vec3((116.0 * yp) - 16.0, 500.0 * (xp - yp), 200.0 * (yp - zp));
}
vec3 labToRgb(vec3 lab) {
  float fy = (lab.x + 16.0) / 116.0; float fx = (lab.y / 500.0) + fy; float fz = fy - (lab.z / 200.0);
  float x = 0.95047 * inverseLabPivot(fx); float y = 1.00000 * inverseLabPivot(fy); float z = 1.08883 * inverseLabPivot(fz);
  float r = (x * 3.2404542) + (y * -1.5371385) + (z * -0.4985314);
  float g = (x * -0.9692660) + (y * 1.8760108) + (z * 0.0415560);
  float b = (x * 0.0556434) + (y * -0.2040259) + (z * 1.0572252);
  return vec3(linearToSrgb(r), linearToSrgb(g), linearToSrgb(b));
}
float applyToneCurve(float lightness, float tone) {
  float l = clamp(lightness, 0.0, 1.0); float amount = clamp(tone, -1.0, 1.0);
  if (amount >= 0.0) { float sCurve = l * l * (3.0 - (2.0 * l)); return mix(l, sCurve, amount); }
  float flattened = 0.5 + ((l - 0.5) * (1.0 + (amount * 0.58))); return mix(l, flattened, -amount);
}
vec3 sampleHaldCell(sampler2D haldTexture, float rIndex, float gIndex, float bIndex, float cubeSide, float imageSide) {
  float linearIndex = rIndex + (gIndex * cubeSide) + (bIndex * cubeSide * cubeSide);
  float x = mod(linearIndex, imageSide);
  float y = floor(linearIndex / imageSide);
  vec2 uv = vec2((x + 0.5) / imageSide, (y + 0.5) / imageSide);
  return texture2D(haldTexture, uv).rgb;
}
vec3 applySingleHaldClut(sampler2D haldTexture, float haldLevel, vec3 rgb) {
  float safeLevel = max(haldLevel, 2.0); float cubeSide = safeLevel * safeLevel; float imageSide = safeLevel * cubeSide;
  float scaledR = clamp(rgb.r, 0.0, 1.0) * (cubeSide - 1.0);
  float scaledG = clamp(rgb.g, 0.0, 1.0) * (cubeSide - 1.0);
  float scaledB = clamp(rgb.b, 0.0, 1.0) * (cubeSide - 1.0);
  float r0 = floor(scaledR); float g0 = floor(scaledG); float b0 = floor(scaledB);
  float r1 = min(r0 + 1.0, cubeSide - 1.0); float g1 = min(g0 + 1.0, cubeSide - 1.0); float b1 = min(b0 + 1.0, cubeSide - 1.0);
  float rf = fract(scaledR); float gf = fract(scaledG); float bf = fract(scaledB);
  vec3 c000 = sampleHaldCell(haldTexture, r0, g0, b0, cubeSide, imageSide);
  vec3 c100 = sampleHaldCell(haldTexture, r1, g0, b0, cubeSide, imageSide);
  vec3 c010 = sampleHaldCell(haldTexture, r0, g1, b0, cubeSide, imageSide);
  vec3 c110 = sampleHaldCell(haldTexture, r1, g1, b0, cubeSide, imageSide);
  vec3 c001 = sampleHaldCell(haldTexture, r0, g0, b1, cubeSide, imageSide);
  vec3 c101 = sampleHaldCell(haldTexture, r1, g0, b1, cubeSide, imageSide);
  vec3 c011 = sampleHaldCell(haldTexture, r0, g1, b1, cubeSide, imageSide);
  vec3 c111 = sampleHaldCell(haldTexture, r1, g1, b1, cubeSide, imageSide);
  vec3 c00 = mix(c000, c100, rf); vec3 c10 = mix(c010, c110, rf); vec3 c01 = mix(c001, c101, rf); vec3 c11 = mix(c011, c111, rf);
  vec3 c0 = mix(c00, c10, gf); vec3 c1 = mix(c01, c11, gf); return mix(c0, c1, bf);
}
vec3 applyCreativeAdjustments(
  vec3 rgb,
  float color,
  float tone,
  float saturation,
  float brightness
) {
  vec3 lab = rgbToLab(rgb);
  vec2 safeAxis = dot(uColorAxis, uColorAxis) > 0.0001 ? normalize(uColorAxis) : vec2(1.0, 0.0);
  if (abs(tone) > 0.0001) lab.x = clamp(applyToneCurve(lab.x / 100.0, tone) * 100.0, 0.0, 100.0);
  if (abs(brightness) > 0.0001) lab.x = clamp(lab.x + (brightness * 18.0), 0.0, 100.0);
  if (abs(color) > 0.0001) { float shift = color * 16.0; lab.y += safeAxis.x * shift; lab.z += safeAxis.y * shift; }
  if (abs(saturation) > 0.0001) { float sat = saturation >= 0.0 ? (1.0 + (saturation * 0.75)) : (1.0 + (saturation * 0.68)); lab.yz *= max(sat, 0.12); }
  return labToRgb(lab);
}
float portraitLiftZone(float normalizedLightness) {
  float safeLightness = clamp(normalizedLightness, 0.0, 1.0);
  if (safeLightness <= 0.08 || safeLightness >= 0.90) return 0.0;
  if (safeLightness < 0.26) return smoothstep(0.08, 0.26, safeLightness);
  if (safeLightness > 0.72) return 1.0 - smoothstep(0.72, 0.90, safeLightness);
  return 1.0;
}
vec3 applyPortraitAdjustments(vec3 rgb, float portraitMask) {
  if (uPortraitEnabled == 0 || portraitMask <= 0.0001) return rgb;
  vec3 adjustedLab = rgbToLab(rgb);
  float liftZone = portraitLiftZone(adjustedLab.x / 100.0);
  if (liftZone <= 0.0001) return rgb;
  float effectiveMask = clamp(portraitMask * liftZone, 0.0, 1.0);
  adjustedLab.x = clamp(adjustedLab.x + (uPortraitLightnessLift * effectiveMask), 0.0, 100.0);
  float saturationFactor = 1.0 + (uPortraitSaturationBoost * effectiveMask);
  adjustedLab.yz *= max(saturationFactor, 0.12);
  return labToRgb(adjustedLab);
}
vec3 applySourceDetailAdjust(vec2 uv, vec3 centerRgb) {
  float safeSharpen = clamp(uSharpen, -1.0, 1.0);
  float detailSuppressionAmount =
      (uPortraitEnabled != 0) && (uProtectedFaceCount > 0)
          ? (1.0 - clamp(uPortraitSharpenRetention, 0.0, 1.0))
          : 0.0;
  bool portraitDetailEnabled = detailSuppressionAmount > 0.0001;
  if (abs(safeSharpen) <= 0.0001 && !portraitDetailEnabled) return centerRgb;
  vec2 texel = max(uSourceTexelSize, vec2(0.000001));
  vec3 left = texture2D(uTexture, clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb;
  vec3 right = texture2D(uTexture, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb;
  vec3 up = texture2D(uTexture, clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0))).rgb;
  vec3 down = texture2D(uTexture, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb;
  vec3 blurred = ((centerRgb * 4.0) + left + right + up + down) / 8.0;
  float effectiveSharpen = safeSharpen;
  if (portraitDetailEnabled) {
    float portraitProtection = portraitDetailAmount(centerRgb, uv);
    if (portraitProtection > 0.0001 && safeSharpen > 0.0) {
      effectiveSharpen = safeSharpen * mix(1.0, clamp(uPortraitSharpenRetention, 0.0, 1.0), portraitProtection);
    }
  }
  vec3 detailAdjusted = effectiveSharpen >= 0.0
      ? clamp(centerRgb + ((centerRgb - blurred) * (effectiveSharpen * ${COLOR_CARD_TRACE_SHARPEN_DETAIL_SCALE})), 0.0, 1.0)
      : mix(centerRgb, blurred, -effectiveSharpen * ${COLOR_CARD_TRACE_SOFTEN_BLEND_SCALE});
  if (portraitDetailEnabled) {
    float portraitProtection = portraitDetailAmount(centerRgb, uv);
    if (portraitProtection > 0.0001) {
      float detailLuma = abs(dot(centerRgb - blurred, vec3(0.299, 0.587, 0.114)));
      float edgeMask = smoothstep(0.016, 0.070, detailLuma);
      float suppressionMix = clamp(detailSuppressionAmount * portraitProtection * edgeMask * 1.72, 0.0, 1.0);
      if (suppressionMix > 0.0001) {
        detailAdjusted = mix(detailAdjusted, blurred, suppressionMix * 0.96);
      }
    }
  }
  return detailAdjusted;
}
vec3 sampleSourceWithDetail(vec2 uv) {
  vec3 centerRgb = texture2D(uTexture, uv).rgb;
  return applySourceDetailAdjust(uv, centerRgb);
}
float softRange(float value, float outerStart, float innerStart, float innerEnd, float outerEnd) {
  if (value <= outerStart || value >= outerEnd) return 0.0;
  if (value >= innerStart && value <= innerEnd) return 1.0;
  if (value < innerStart) return smoothstep(outerStart, innerStart, value);
  return 1.0 - smoothstep(innerEnd, outerEnd, value);
}
float faceProtectionMask(vec2 uv) {
  float strongest = 0.0;
  for (int i = 0; i < MAX_PROTECTED_FACES; i++) {
    if (i >= uProtectedFaceCount) break;
    vec4 region = uProtectedFaces[i];
    vec2 radii = max(region.zw, vec2(0.01));
    vec2 delta = (uv - region.xy) / radii;
    float distance = length(delta);
    float mask = distance <= 0.56 ? 1.0 : (distance >= 1.24 ? 0.0 : (1.0 - smoothstep(0.56, 1.24, distance)));
    strongest = max(strongest, mask);
  }
  return strongest;
}
float skinToneMask(vec3 rgb) {
  float luma = (0.299 * rgb.r) + (0.587 * rgb.g) + (0.114 * rgb.b);
  float cb = 0.5 - (0.168736 * rgb.r) - (0.331264 * rgb.g) + (0.5 * rgb.b);
  float cr = 0.5 + (0.5 * rgb.r) - (0.418688 * rgb.g) - (0.081312 * rgb.b);
  float spread = max(max(rgb.r, rgb.g), rgb.b) - min(min(rgb.r, rgb.g), rgb.b);
  float detailMask = softRange(spread, 0.015, 0.045, 0.78, 0.995);
  float lumaMask = softRange(luma, 0.03, 0.08, 0.94, 0.99);
  float cbMask = softRange(cb, 0.20, 0.26, 0.46, 0.54);
  float crMask = softRange(cr, 0.42, 0.48, 0.70, 0.80);
  float redGreenBias = softRange(rgb.r - rgb.g, -0.06, -0.01, 0.36, 0.60);
  float redBlueBias = softRange(rgb.r - rgb.b, -0.02, 0.03, 0.58, 0.86);
  return clamp(
      cbMask *
          crMask *
          detailMask *
          lumaMask *
          (0.68 + (0.32 * redGreenBias)) *
          (0.68 + (0.32 * redBlueBias)),
      0.0,
      1.0
  );
}
float skinProtectionAmount(vec3 rgb, vec2 uv) {
  if (uProtectedFaceCount <= 0) return 0.0;
  return clamp(faceProtectionMask(uv) * skinToneMask(rgb), 0.0, 1.0);
}
float portraitDetailAmount(vec3 rgb, vec2 uv) {
  if (uProtectedFaceCount <= 0) return 0.0;
  float faceMask = faceProtectionMask(uv);
  if (faceMask <= 0.0001) return 0.0;
  float skinMask = skinToneMask(rgb);
  return clamp(faceMask * (0.70 + (0.30 * skinMask)), 0.0, 1.0);
}
float portraitSoftnessAmount(vec3 rgb, vec2 uv) {
  if (uProtectedFaceCount <= 0) return 0.0;
  float faceMask = faceProtectionMask(uv);
  if (faceMask <= 0.0001) return 0.0;
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
  if (uPortraitEnabled == 0 || uPortraitSoftenAmount <= 0.0001 || portraitMask <= 0.0001) return rgb;
  float softenMix = clamp(uPortraitSoftenAmount * portraitMask * 1.58, 0.0, 1.0);
  if (softenMix <= 0.0001) return rgb;
  return mix(rgb, samplePortraitSoftBlur(uv), softenMix);
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
  vec3 sourceRgb = sampleSource(uv);
  vec3 rgb = sampleSourceWithDetail(uv);
  bool portraitEnabled = (uPortraitEnabled != 0) && (uProtectedFaceCount > 0);
  float protectionAmount = skinProtectionAmount(sourceRgb, uv);
  if (portraitEnabled && uPortraitSoftenAmount > 0.0001) {
    float softnessAmount = portraitSoftnessAmount(sourceRgb, uv);
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
  gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), 1.0);
}
""".trimIndent()
