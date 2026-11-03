package com.voicelike.app.motionphoto

import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.voicelike.app.AppRuntimeLogStore
import com.voicelike.app.ColorCardTraceFilterState
import com.voicelike.app.MediaItem
import com.voicelike.app.MotionPhotoHelper
import com.voicelike.app.copyPreservedExifMetadataToFile
import com.voicelike.app.renderColorCardTraceBitmapForExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.min

internal data class MotionPhotoExportResult(
    val savedUri: Uri,
    override val displayName: String,
    override val previewBitmap: Bitmap,
    val session: MotionPhotoSessionPaths
) : DynamicPhotoExportResult {
    override val primarySavedUri: Uri
        get() = savedUri
}

internal data class VivoLivePhotoExportResult(
    val photoUri: Uri,
    val videoUri: Uri,
    override val displayName: String,
    override val previewBitmap: Bitmap,
    val session: MotionPhotoSessionPaths,
    val manifestFile: File
) : DynamicPhotoExportResult {
    override val primarySavedUri: Uri
        get() = photoUri
}

internal data class VendorLivePhotoExportResult(
    val photoUri: Uri,
    val videoUri: Uri,
    override val displayName: String,
    override val previewBitmap: Bitmap,
    val session: MotionPhotoSessionPaths,
    val manifestFile: File,
    val vendor: String
) : DynamicPhotoExportResult {
    override val primarySavedUri: Uri
        get() = photoUri
}

internal interface DynamicPhotoExportResult {
    val displayName: String
    val previewBitmap: Bitmap
    val primarySavedUri: Uri
}

internal object MotionPhotoExportCoordinator {
    private const val LOG_TAG = "MotionPhotoExport"
    private const val STANDARD_MIME_TYPE_EXTENSION_COLUMN = "standard_mime_type_extension"
    private const val STANDARD_MIME_TYPE_EXTENSION_MOTION_PHOTO = 2
    private const val MIN_DYNAMIC_RENDER_DURATION_MS = 250L
    private const val MAX_OPLUS_RENDER_FRAME_RATE = 30

    private data class DynamicVideoProfile(
        val durationMs: Long?,
        val width: Int?,
        val height: Int?,
        val frameRate: Int?,
        val bitrate: Int?,
        val mimeType: String?
    )

    suspend fun exportRenderedDynamicPhoto(
        context: Context,
        sourceItem: MediaItem,
        renderedCoverBitmap: Bitmap,
        traceState: ColorCardTraceFilterState,
        displayName: String,
        session: MotionPhotoSessionPaths,
        spec: MotionPhotoRenderSpec,
        onProgress: (String) -> Unit
    ): DynamicPhotoExportResult {
        return when (resolveDynamicPhotoExportContainerForCurrentDevice(sourceItem)) {
            DynamicPhotoExportContainer.SingleFileMotionPhoto ->
                exportRenderedMotionPhoto(
                    context = context,
                    sourceItem = sourceItem,
                    renderedCoverBitmap = renderedCoverBitmap,
                    traceState = traceState,
                    displayName = displayName,
                    session = session,
                    spec = spec,
                    onProgress = onProgress
                )
            DynamicPhotoExportContainer.SamsungMotionPhoto ->
                exportRenderedMotionPhoto(
                    context = context,
                    sourceItem = sourceItem,
                    renderedCoverBitmap = renderedCoverBitmap,
                    traceState = traceState,
                    displayName = displayName,
                    session = session,
                    spec = spec,
                    exportContainer = DynamicPhotoExportContainer.SamsungMotionPhoto,
                    onProgress = onProgress
                )
            DynamicPhotoExportContainer.OplusMotionPhoto ->
                exportRenderedMotionPhoto(
                    context = context,
                    sourceItem = sourceItem,
                    renderedCoverBitmap = renderedCoverBitmap,
                    traceState = traceState,
                    displayName = displayName,
                    session = session,
                    spec = spec,
                    exportContainer = DynamicPhotoExportContainer.OplusMotionPhoto,
                    onProgress = onProgress
                )
            DynamicPhotoExportContainer.XiaomiMotionPhoto ->
                exportRenderedMotionPhoto(
                    context = context,
                    sourceItem = sourceItem,
                    renderedCoverBitmap = renderedCoverBitmap,
                    traceState = traceState,
                    displayName = displayName,
                    session = session,
                    spec = spec,
                    exportContainer = DynamicPhotoExportContainer.XiaomiMotionPhoto,
                    onProgress = onProgress
                )
            DynamicPhotoExportContainer.VendorPair ->
                exportRenderedVendorLivePhoto(
                    context = context,
                    sourceItem = sourceItem,
                    renderedCoverBitmap = renderedCoverBitmap,
                    traceState = traceState,
                    displayName = displayName,
                    session = session,
                    spec = spec,
                    onProgress = onProgress
                )
        }
    }

    suspend fun export(
        context: Context,
        sourceFile: File?,
        fallbackBitmap: Bitmap?,
        displayName: String,
        session: MotionPhotoSessionPaths,
        spec: MotionPhotoRenderSpec,
        onProgress: (String) -> Unit
    ): MotionPhotoExportResult {
        onProgress("正在准备原图")
        val sourceBitmap = MotionPhotoFrameRenderer.decodeSourceBitmap(
            sourceFile = sourceFile,
            fallbackBitmap = fallbackBitmap,
            maxLongSide = spec.maxExportLongSide
        ) ?: error("Unable to decode source image")
        val grayscaleBitmap = MotionPhotoFrameRenderer.createGrayscaleBitmap(sourceBitmap)
        val exportContainer = resolveDynamicPhotoExportContainer(
            deviceBrand = Build.BRAND,
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            sourceIsLivePhoto = false,
            sourceIsMotionPhoto = false,
            deviceProduct = Build.PRODUCT,
            deviceDevice = Build.DEVICE,
            deviceDisplay = Build.DISPLAY
        )
        if (exportContainer == DynamicPhotoExportContainer.VendorPair) {
            val vendorResult = exportVendorLivePhoto(
                context = context,
                sourceFile = sourceFile,
                fallbackBitmap = fallbackBitmap,
                displayName = displayName,
                session = session,
                spec = spec,
                onProgress = onProgress
            )
            if (!grayscaleBitmap.isRecycled) grayscaleBitmap.recycle()
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
            return MotionPhotoExportResult(
                savedUri = vendorResult.primarySavedUri,
                displayName = vendorResult.displayName,
                previewBitmap = vendorResult.previewBitmap,
                session = vendorResult.session
            )
        }

        try {
            onProgress("正在生成封面")
            val coverBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
            val coverFile = File(session.tempDir, "cover.jpg")
            FileOutputStream(coverFile).use { output ->
                coverBitmap.compress(Bitmap.CompressFormat.JPEG, spec.jpegQuality, output)
            }

            onProgress("正在编码视频")
            val videoFile = File(session.tempDir, "motion.mp4")
            MotionPhotoVideoEncoder.encodeFrameSequenceToMp4(
                frameCount = spec.frameCount,
                frameProvider = { index ->
                    val progress = index.toFloat() / (spec.frameCount - 1).coerceAtLeast(1)
                    MotionPhotoFrameRenderer.renderFrame(
                        sourceBitmap = sourceBitmap,
                        grayscaleBitmap = grayscaleBitmap,
                        progress = progress,
                        revealPattern = spec.revealPattern,
                        revealSeed = spec.revealSeed
                    )
                },
                outputFile = videoFile,
                spec = spec
            )

            onProgress("正在封装 Motion Photo")
            val outputFile = File(
                session.outputDir,
                buildMotionPhotoFileName(displayName, exportContainer)
            )
            writeSingleFileMotionPhoto(
                context = context,
                exportContainer = exportContainer,
                jpegFile = coverFile,
                videoFile = videoFile,
                outputFile = outputFile,
                presentationTimestampUs = resolveMotionPhotoPresentationTimestampUs(spec.durationMs)
            )

            onProgress("正在保存到相册")
            val savedUri = saveMotionPhotoToGallery(
                context = context,
                outputFile = outputFile,
                displayName = outputFile.name,
                exportContainer = exportContainer
            )
            validateAndPersistMotionPhoto(
                context = context,
                savedUri = savedUri
            )

            return MotionPhotoExportResult(
                savedUri = savedUri,
                displayName = outputFile.name,
                previewBitmap = coverBitmap,
                session = session
            )
        } finally {
            if (!grayscaleBitmap.isRecycled) grayscaleBitmap.recycle()
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
        }
    }

    suspend fun exportRenderedMotionPhoto(
        context: Context,
        sourceItem: MediaItem,
        renderedCoverBitmap: Bitmap,
        traceState: ColorCardTraceFilterState,
        displayName: String,
        session: MotionPhotoSessionPaths,
        spec: MotionPhotoRenderSpec,
        exportContainer: DynamicPhotoExportContainer = DynamicPhotoExportContainer.SingleFileMotionPhoto,
        onProgress: (String) -> Unit
    ): MotionPhotoExportResult {
        var coverBitmapForFile: Bitmap? = null
        try {
            val oplusRoundTripPayload =
                if (exportContainer == DynamicPhotoExportContainer.OplusMotionPhoto) {
                    OplusMotionPhotoRoundTripSupport.extract(
                        context = context.applicationContext,
                        sourceItem = sourceItem
                    )
                } else {
                    null
                }
            coverBitmapForFile =
                if (exportContainer == DynamicPhotoExportContainer.OplusMotionPhoto) {
                    buildOplusCompatibleCoverBitmap(
                        context = context.applicationContext,
                        sourceItem = sourceItem,
                        renderedCoverBitmap = renderedCoverBitmap,
                        traceState = traceState,
                        roundTripPayload = oplusRoundTripPayload
                    )
                } else {
                    renderedCoverBitmap
                }
            onProgress("正在生成封面")
            val coverFile = File(session.tempDir, "cover.jpg")
            FileOutputStream(coverFile).use { output ->
                (coverBitmapForFile ?: renderedCoverBitmap)
                    .compress(Bitmap.CompressFormat.JPEG, spec.jpegQuality, output)
            }
            copyPreservedExifMetadataToFile(
                resolver = context.contentResolver,
                sourceUri = sourceItem.uri,
                targetFile = coverFile
            )
            val xiaomiRoundTripPayload =
                if (
                    exportContainer == DynamicPhotoExportContainer.XiaomiMotionPhoto
                ) {
                    val extractedPayload = if (sourceItem.isMotionPhoto) {
                        XiaomiMotionPhotoRoundTripSupport.extract(
                            context = context.applicationContext,
                            sourceItem = sourceItem
                        )
                    } else {
                        null
                    }
                    extractedPayload ?: XiaomiMotionPhotoRoundTripPayload(
                        presentationTimestampUs = null,
                        timeWatermark = null,
                        subImage = null,
                        preservedMetaBox = null,
                        primaryJpegShell = XiaomiMotionPhotoRoundTripSupport.extractPrimaryJpegShell(
                            context = context.applicationContext,
                            sourceItem = sourceItem
                        )
                    )
                } else {
                    null
                }
            val googleSamsungRoundTripPayload =
                if (
                    sourceItem.isMotionPhoto &&
                    (
                        exportContainer == DynamicPhotoExportContainer.SingleFileMotionPhoto ||
                            exportContainer == DynamicPhotoExportContainer.SamsungMotionPhoto
                        )
                ) {
                    GoogleSamsungMotionPhotoRoundTripSupport.extract(
                        context = context.applicationContext,
                        sourceItem = sourceItem
                    )
                } else {
                    null
                }

            onProgress("正在渲染动态内容")
            val dynamicVideoProfile =
                if (exportContainer == DynamicPhotoExportContainer.OplusMotionPhoto) {
                    resolveDynamicVideoProfile(
                        context = context,
                        sourceItem = sourceItem
                    )
                } else {
                    null
                }
            val effectiveSpec =
                if (exportContainer == DynamicPhotoExportContainer.OplusMotionPhoto) {
                    resolveOplusEffectiveRenderSpec(
                        baseSpec = spec,
                        dynamicVideoProfile = dynamicVideoProfile
                    )
                } else {
                    spec
                }
            val videoFile = File(session.tempDir, "motion.mp4")
            val preferredVideoMimeTypes =
                if (exportContainer == DynamicPhotoExportContainer.OplusMotionPhoto) {
                    resolvePreferredRenderedVideoMimeTypes(dynamicVideoProfile)
                } else {
                    listOf(MediaFormat.MIMETYPE_VIDEO_AVC)
                }
            renderTraceVideoToMp4(
                context = context,
                sourceItem = sourceItem,
                traceState = traceState,
                outputFile = videoFile,
                spec = effectiveSpec,
                preferredVideoMimeTypes = preferredVideoMimeTypes
            )
            val preparedVideoFile =
                if (exportContainer == DynamicPhotoExportContainer.XiaomiMotionPhoto) {
                    prepareXiaomiCompatibleVideo(
                        context = context,
                        sourceItem = sourceItem,
                        renderedVideoFile = videoFile,
                        session = session
                    )
                } else if (exportContainer == DynamicPhotoExportContainer.OplusMotionPhoto) {
                    prepareOplusCompatibleVideo(
                        context = context,
                        sourceItem = sourceItem,
                        renderedVideoFile = videoFile,
                        session = session,
                        roundTripPayload = oplusRoundTripPayload
                    )
                } else {
                    videoFile
                }

            onProgress("正在封装 Motion Photo")
            val outputFile = File(
                session.outputDir,
                buildMotionPhotoFileName(displayName, exportContainer)
            )
            writeSingleFileMotionPhoto(
                context = context,
                exportContainer = exportContainer,
                jpegFile = coverFile,
                videoFile = preparedVideoFile,
                outputFile = outputFile,
                presentationTimestampUs = xiaomiRoundTripPayload?.presentationTimestampUs
                    ?: resolveMotionPhotoPresentationTimestampUs(effectiveSpec.durationMs),
                xiaomiRoundTripPayload = xiaomiRoundTripPayload,
                oplusRoundTripPayload = oplusRoundTripPayload,
                googleSamsungRoundTripPayload = googleSamsungRoundTripPayload
            )

            onProgress("正在保存到相册")
            val savedUri = saveMotionPhotoToGallery(
                context = context,
                outputFile = outputFile,
                displayName = outputFile.name,
                exportContainer = exportContainer
            )
            validateAndPersistMotionPhoto(
                context = context,
                savedUri = savedUri
            )

            return MotionPhotoExportResult(
                savedUri = savedUri,
                displayName = outputFile.name,
                previewBitmap = renderedCoverBitmap,
                session = session
            )
        } catch (error: Throwable) {
            AppRuntimeLogStore.logError(
                context.applicationContext,
                LOG_TAG,
                "exportRenderedMotionPhoto failed " +
                    "container=$exportContainer " +
                    "sourceId=${sourceItem.id} " +
                    "sourceLive=${sourceItem.isLivePhoto} " +
                    "sourceMotion=${sourceItem.isMotionPhoto} " +
                    "displayName=$displayName",
                error
            )
            Log.e(
                LOG_TAG,
                "exportRenderedMotionPhoto failed " +
                    "container=$exportContainer " +
                    "sourceId=${sourceItem.id} " +
                    "sourceLive=${sourceItem.isLivePhoto} " +
                    "sourceMotion=${sourceItem.isMotionPhoto} " +
                    "displayName=$displayName",
                error
            )
            if (!renderedCoverBitmap.isRecycled) {
                renderedCoverBitmap.recycle()
            }
            throw error
        } finally {
            if (
                coverBitmapForFile != null &&
                coverBitmapForFile !== renderedCoverBitmap &&
                !coverBitmapForFile!!.isRecycled
            ) {
                coverBitmapForFile!!.recycle()
            }
        }
    }

    suspend fun exportVivoLivePhoto(
        context: Context,
        sourceFile: File?,
        fallbackBitmap: Bitmap?,
        displayName: String,
        session: MotionPhotoSessionPaths,
        spec: MotionPhotoRenderSpec,
        onProgress: (String) -> Unit
    ): VivoLivePhotoExportResult {
        onProgress("正在准备原图")
        val sourceBitmap = MotionPhotoFrameRenderer.decodeSourceBitmap(
            sourceFile = sourceFile,
            fallbackBitmap = fallbackBitmap,
            maxLongSide = spec.maxExportLongSide
        ) ?: error("Unable to decode source image")
        val grayscaleBitmap = MotionPhotoFrameRenderer.createGrayscaleBitmap(sourceBitmap)

        try {
            onProgress("正在生成封面")
            val coverBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
            val coverFile = File(session.tempDir, "live_cover.jpg")
            FileOutputStream(coverFile).use { output ->
                coverBitmap.compress(Bitmap.CompressFormat.JPEG, spec.jpegQuality, output)
            }

            onProgress("正在编码视频")
            val videoFile = File(session.tempDir, "live_motion.mp4")
            MotionPhotoVideoEncoder.encodeFrameSequenceToMp4(
                frameCount = spec.frameCount,
                frameProvider = { index ->
                    val progress = index.toFloat() / (spec.frameCount - 1).coerceAtLeast(1)
                    MotionPhotoFrameRenderer.renderFrame(
                        sourceBitmap = sourceBitmap,
                        grayscaleBitmap = grayscaleBitmap,
                        progress = progress,
                        revealPattern = spec.revealPattern,
                        revealSeed = spec.revealSeed
                    )
                },
                outputFile = videoFile,
                spec = spec
            )

            onProgress("正在封装 vivo Live Photo")
            val pair = DynamicPhotoCompatWriter.writeVivoLivePhotoPair(
                jpegFile = coverFile,
                videoFile = videoFile,
                outputDir = session.outputDir,
                baseName = buildVivoLivePhotoBaseName(displayName)
            )

            onProgress("正在保存到相册")
            val saved = DynamicPhotoCompatWriter.saveVivoLivePhotoPairToGallery(
                context = context.applicationContext,
                pair = pair
            ) ?: error("Failed to save vivo Live Photo")

            return VivoLivePhotoExportResult(
                photoUri = saved.photoUri,
                videoUri = saved.videoUri,
                displayName = pair.photoFile.name,
                previewBitmap = coverBitmap,
                session = session,
                manifestFile = saved.manifestFile
            )
        } finally {
            if (!grayscaleBitmap.isRecycled) grayscaleBitmap.recycle()
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
        }
    }

    suspend fun exportVendorLivePhoto(
        context: Context,
        sourceFile: File?,
        fallbackBitmap: Bitmap?,
        displayName: String,
        session: MotionPhotoSessionPaths,
        spec: MotionPhotoRenderSpec,
        onProgress: (String) -> Unit
    ): VendorLivePhotoExportResult {
        onProgress("姝ｅ湪鍑嗗鍘熷浘")
        val sourceBitmap = MotionPhotoFrameRenderer.decodeSourceBitmap(
            sourceFile = sourceFile,
            fallbackBitmap = fallbackBitmap,
            maxLongSide = spec.maxExportLongSide
        ) ?: error("Unable to decode source image")
        val grayscaleBitmap = MotionPhotoFrameRenderer.createGrayscaleBitmap(sourceBitmap)

        try {
            onProgress("姝ｅ湪鐢熸垚灏侀潰")
            val coverBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
            val coverFile = File(session.tempDir, "vendor_live_cover.jpg")
            FileOutputStream(coverFile).use { output ->
                coverBitmap.compress(Bitmap.CompressFormat.JPEG, spec.jpegQuality, output)
            }

            onProgress("姝ｅ湪缂栫爜瑙嗛")
            val videoFile = File(session.tempDir, "vendor_live_motion.mp4")
            MotionPhotoVideoEncoder.encodeFrameSequenceToMp4(
                frameCount = spec.frameCount,
                frameProvider = { index ->
                    val progress = index.toFloat() / (spec.frameCount - 1).coerceAtLeast(1)
                    MotionPhotoFrameRenderer.renderFrame(
                        sourceBitmap = sourceBitmap,
                        grayscaleBitmap = grayscaleBitmap,
                        progress = progress,
                        revealPattern = spec.revealPattern,
                        revealSeed = spec.revealSeed
                    )
                },
                outputFile = videoFile,
                spec = spec
            )

            onProgress("姝ｅ湪灏佽 vendor Live Photo")
            val roundTripSpec = buildFallbackVendorRoundTripSpecForCurrentDevice()
            val pair = VendorLivePhotoRoundTripSupport.writeRoundTripPair(
                jpegFile = coverFile,
                videoFile = videoFile,
                outputDir = session.outputDir,
                baseName = buildVivoLivePhotoBaseName(displayName),
                spec = roundTripSpec
            )

            onProgress("姝ｅ湪淇濆瓨鍒扮浉鍐?")
            val saved = VendorLivePhotoRoundTripSupport.savePairToGallery(
                context = context.applicationContext,
                pair = pair
            ) ?: error("Failed to save vendor Live Photo")

            return VendorLivePhotoExportResult(
                photoUri = saved.photoUri,
                videoUri = saved.videoUri,
                displayName = pair.photoFile.name,
                previewBitmap = coverBitmap,
                session = session,
                manifestFile = saved.manifestFile,
                vendor = roundTripSpec.vendor
            )
        } finally {
            if (!grayscaleBitmap.isRecycled) grayscaleBitmap.recycle()
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
        }
    }

    suspend fun exportRenderedVivoLivePhoto(
        context: Context,
        sourceItem: MediaItem,
        renderedCoverBitmap: Bitmap,
        traceState: ColorCardTraceFilterState,
        displayName: String,
        session: MotionPhotoSessionPaths,
        spec: MotionPhotoRenderSpec,
        onProgress: (String) -> Unit
    ): VivoLivePhotoExportResult {
        try {
            onProgress("正在生成封面")
            val coverFile = File(session.tempDir, "live_cover.jpg")
            FileOutputStream(coverFile).use { output ->
                renderedCoverBitmap.compress(Bitmap.CompressFormat.JPEG, spec.jpegQuality, output)
            }

            onProgress("正在渲染动态内容")
            val videoFile = File(session.tempDir, "live_motion.mp4")
            renderTraceVideoToMp4(
                context = context,
                sourceItem = sourceItem,
                traceState = traceState,
                outputFile = videoFile,
                spec = spec
            )

            onProgress("正在封装 vivo Live Photo")
            val pair = DynamicPhotoCompatWriter.writeVivoLivePhotoPair(
                jpegFile = coverFile,
                videoFile = videoFile,
                outputDir = session.outputDir,
                baseName = buildVivoLivePhotoBaseName(displayName)
            )

            onProgress("正在保存到相册")
            val saved = DynamicPhotoCompatWriter.saveVivoLivePhotoPairToGallery(
                context = context.applicationContext,
                pair = pair
            ) ?: error("Failed to save vivo Live Photo")

            return VivoLivePhotoExportResult(
                photoUri = saved.photoUri,
                videoUri = saved.videoUri,
                displayName = pair.photoFile.name,
                previewBitmap = renderedCoverBitmap,
                session = session,
                manifestFile = saved.manifestFile
            )
        } catch (error: Throwable) {
            if (!renderedCoverBitmap.isRecycled) {
                renderedCoverBitmap.recycle()
            }
            throw error
        }
    }

    suspend fun exportRenderedVendorLivePhoto(
        context: Context,
        sourceItem: MediaItem,
        renderedCoverBitmap: Bitmap,
        traceState: ColorCardTraceFilterState,
        displayName: String,
        session: MotionPhotoSessionPaths,
        spec: MotionPhotoRenderSpec,
        onProgress: (String) -> Unit
    ): VendorLivePhotoExportResult {
        try {
            onProgress("正在生成封面")
            val coverFile = File(session.tempDir, "vendor_live_cover.jpg")
            FileOutputStream(coverFile).use { output ->
                renderedCoverBitmap.compress(Bitmap.CompressFormat.JPEG, spec.jpegQuality, output)
            }

            onProgress("正在渲染动态内容")
            val videoFile = File(session.tempDir, "vendor_live_motion.mp4")
            renderTraceVideoToMp4(
                context = context,
                sourceItem = sourceItem,
                traceState = traceState,
                outputFile = videoFile,
                spec = spec
            )

            onProgress("正在保留原始 Live 外壳")
            val roundTripSpec = VendorLivePhotoRoundTripSupport.prepareRoundTripSpec(
                context = context.applicationContext,
                sourceItem = sourceItem
            ) ?: buildFallbackVendorRoundTripSpecForCurrentDevice()
            val pair = VendorLivePhotoRoundTripSupport.writeRoundTripPair(
                jpegFile = coverFile,
                videoFile = videoFile,
                outputDir = session.outputDir,
                baseName = buildVivoLivePhotoBaseName(displayName),
                spec = roundTripSpec
            )

            onProgress("正在保存到相册")
            val saved = VendorLivePhotoRoundTripSupport.savePairToGallery(
                context = context.applicationContext,
                pair = pair
            ) ?: error("Failed to save vendor Live Photo")

            return VendorLivePhotoExportResult(
                photoUri = saved.photoUri,
                videoUri = saved.videoUri,
                displayName = pair.photoFile.name,
                previewBitmap = renderedCoverBitmap,
                session = session,
                manifestFile = saved.manifestFile,
                vendor = roundTripSpec.vendor
            )
        } catch (error: Throwable) {
            if (!renderedCoverBitmap.isRecycled) {
                renderedCoverBitmap.recycle()
            }
            throw error
        }
    }

    private fun buildFallbackVendorRoundTripSpecForCurrentDevice(): VendorLivePhotoRoundTripSpec {
        val vendor = when (resolveVendorPairExportShellForCurrentDevice()) {
            VendorPairExportShell.Vivo -> "vivo"
            VendorPairExportShell.HuaweiHarmony -> "huawei_harmony"
            VendorPairExportShell.GenericPaired -> "paired_live_photo"
        }
        return VendorLivePhotoRoundTripSpec(
            vendor = vendor,
            imageTrailerBytes = ByteArray(0),
            videoTrailerBytes = ByteArray(0)
        )
    }

    private suspend fun renderTraceVideoToMp4(
        context: Context,
        sourceItem: MediaItem,
        traceState: ColorCardTraceFilterState,
        outputFile: File,
        spec: MotionPhotoRenderSpec,
        preferredVideoMimeTypes: List<String> = listOf(MediaFormat.MIMETYPE_VIDEO_AVC)
    ) {
        val prefersNonAvcVideo = preferredVideoMimeTypes.any {
            !it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)
        }
        if (!prefersNonAvcVideo && ColorCardTraceVideoGpuExportSupport.supports(traceState)) {
            val exported = runCatching {
                ColorCardTraceVideoGpuExportSupport.export(
                    context = context,
                    sourceItem = sourceItem,
                    traceState = traceState,
                    outputFile = outputFile,
                    spec = spec
                )
            }.getOrDefault(false)
            if (exported) return
        }
        renderTraceVideoToMp4Cpu(
            context = context,
            sourceItem = sourceItem,
            traceState = traceState,
            outputFile = outputFile,
            spec = spec,
            preferredVideoMimeTypes = preferredVideoMimeTypes
        )
    }

    private suspend fun renderTraceVideoToMp4Cpu(
        context: Context,
        sourceItem: MediaItem,
        traceState: ColorCardTraceFilterState,
        outputFile: File,
        spec: MotionPhotoRenderSpec,
        preferredVideoMimeTypes: List<String> = listOf(MediaFormat.MIMETYPE_VIDEO_AVC)
    ) = withContext(Dispatchers.Default) {
        val dynamicVideoSource = resolveDynamicVideoSource(context, sourceItem)
            ?: error("Unable to resolve source video")
        val retriever = MediaMetadataRetriever()
        try {
            when (dynamicVideoSource) {
                is DynamicVideoSource.FileSource -> retriever.setDataSource(dynamicVideoSource.file.absolutePath)
                is DynamicVideoSource.UriSource -> retriever.setDataSource(context, dynamicVideoSource.uri)
            }
            val sourceDurationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(1L)
                ?: spec.durationMs
            val sourceWidth = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?.coerceAtLeast(1)
            val sourceHeight = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?.coerceAtLeast(1)
            val sourceRotation = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?.mod(360)
                ?: 0
            val sampledDurationUs = min(sourceDurationMs, spec.durationMs).coerceAtLeast(250L) * 1000L
            val exportFrameSize = resolveDynamicExportFrameSize(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                sourceRotation = if (preferredVideoMimeTypes.firstOrNull()
                    .equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true)
                ) {
                    0
                } else {
                    sourceRotation
                },
                maxLongSide = spec.maxExportLongSide
            )
            MotionPhotoVideoEncoder.encodeFrameSequenceToMp4(
                frameCount = spec.frameCount,
                frameProvider = { index ->
                    val timeUs = if (spec.frameCount <= 1) {
                        0L
                    } else {
                        ((index.toLong() * sampledDurationUs) / (spec.frameCount - 1))
                            .coerceIn(0L, sampledDurationUs)
                    }
                    val sourceFrame = decodeDynamicVideoFrame(
                        retriever = retriever,
                        timeUs = timeUs,
                        exportFrameSize = exportFrameSize
                    ) ?: error("Unable to decode source video frame")
                    try {
                        renderColorCardTraceBitmapForExport(
                            context = context,
                            sourceBitmap = sourceFrame,
                            traceState = traceState
                        )
                    } finally {
                        if (!sourceFrame.isRecycled) {
                            sourceFrame.recycle()
                        }
                    }
                },
                outputFile = outputFile,
                spec = spec,
                preferredVideoMimeTypes = preferredVideoMimeTypes
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun prepareXiaomiCompatibleVideo(
        context: Context,
        sourceItem: MediaItem,
        renderedVideoFile: File,
        session: MotionPhotoSessionPaths
    ): File {
        val sourceVideo = resolveDynamicVideoSource(context, sourceItem) ?: return renderedVideoFile
        val muxedFile = File(session.tempDir, "motion_xiaomi_muxed.mp4")
        val merged = mergeRenderedVideoWithSourceAudio(
            context = context,
            renderedVideoFile = renderedVideoFile,
            sourceVideo = sourceVideo,
            outputFile = muxedFile
        )
        return if (merged) muxedFile else renderedVideoFile
    }

    private fun prepareOplusCompatibleVideo(
        context: Context,
        sourceItem: MediaItem,
        renderedVideoFile: File,
        session: MotionPhotoSessionPaths,
        roundTripPayload: OplusMotionPhotoRoundTripPayload?
    ): File {
        val sourceVideo = resolveDynamicVideoSource(context, sourceItem) ?: return renderedVideoFile
        val muxedFile = File(session.tempDir, "motion_oplus_muxed.mp4")
        val merged = mergeRenderedVideoWithSourceAudio(
            context = context,
            renderedVideoFile = renderedVideoFile,
            sourceVideo = sourceVideo,
            outputFile = muxedFile,
            preferAudioTrackFirst = roundTripPayload?.prefersAudioTrackFirst == true
        )
        return if (merged) muxedFile else renderedVideoFile
    }

    private fun mergeRenderedVideoWithSourceAudio(
        context: Context,
        renderedVideoFile: File,
        sourceVideo: DynamicVideoSource,
        outputFile: File,
        preferAudioTrackFirst: Boolean = false
    ): Boolean {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var failureReason: String? = null
        return runCatching {
            videoExtractor.setDataSource(renderedVideoFile.absolutePath)
            when (sourceVideo) {
                is DynamicVideoSource.FileSource ->
                    audioExtractor.setDataSource(sourceVideo.file.absolutePath)
                is DynamicVideoSource.UriSource ->
                    context.contentResolver.openAssetFileDescriptor(sourceVideo.uri, "r")?.use { afd ->
                        audioExtractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    } ?: run {
                        failureReason = "Unable to open source audio descriptor"
                        return@runCatching false
                    }
            }

            val videoTrackIndex = findTrackIndex(videoExtractor, "video/") ?: run {
                failureReason = "Rendered video has no video track"
                return@runCatching false
            }
            val audioTrackIndex = findTrackIndex(audioExtractor, "audio/") ?: run {
                failureReason = "Source dynamic photo has no audio track"
                return@runCatching false
            }
            videoExtractor.selectTrack(videoTrackIndex)
            audioExtractor.selectTrack(audioTrackIndex)

            val muxerAudioTrack: Int
            val muxerVideoTrack: Int
            if (preferAudioTrackFirst) {
                muxerAudioTrack = muxer.addTrack(audioExtractor.getTrackFormat(audioTrackIndex))
                muxerVideoTrack = muxer.addTrack(videoExtractor.getTrackFormat(videoTrackIndex))
            } else {
                muxerVideoTrack = muxer.addTrack(videoExtractor.getTrackFormat(videoTrackIndex))
                muxerAudioTrack = muxer.addTrack(audioExtractor.getTrackFormat(audioTrackIndex))
            }
            muxer.start()
            muxerStarted = true

            val durationUs = resolveVideoDurationUs(renderedVideoFile)
            copyExtractorTrack(
                extractor = videoExtractor,
                muxer = muxer,
                muxerTrackIndex = muxerVideoTrack,
                durationLimitUs = Long.MAX_VALUE
            )
            copyExtractorTrack(
                extractor = audioExtractor,
                muxer = muxer,
                muxerTrackIndex = muxerAudioTrack,
                durationLimitUs = durationUs
            )
            true
        }.getOrElse { error ->
            AppRuntimeLogStore.logError(
                context.applicationContext,
                LOG_TAG,
                "mergeRenderedVideoWithSourceAudio failed " +
                    "rendered=${renderedVideoFile.name} output=${outputFile.name}",
                error
            )
            Log.w(
                LOG_TAG,
                "mergeRenderedVideoWithSourceAudio failed " +
                    "rendered=${renderedVideoFile.name} output=${outputFile.name}",
                error
            )
            runCatching { outputFile.delete() }
            false
        }.also { merged ->
            if (!merged && failureReason != null) {
                AppRuntimeLogStore.logInfo(
                    context.applicationContext,
                    LOG_TAG,
                    "mergeRenderedVideoWithSourceAudio fallback: $failureReason " +
                        "rendered=${renderedVideoFile.name} output=${outputFile.name}"
                )
                Log.w(
                    LOG_TAG,
                    "mergeRenderedVideoWithSourceAudio fallback: $failureReason " +
                        "rendered=${renderedVideoFile.name} output=${outputFile.name}"
                )
                runCatching { outputFile.delete() }
            }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
        }
    }

    private fun resolveVideoDurationUs(videoFile: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(1L)
                ?: 1L
            durationMs * 1000L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun resolvePreferredRenderedVideoMimeTypes(
        dynamicVideoProfile: DynamicVideoProfile?
    ): List<String> {
        val sourceMimeType = dynamicVideoProfile?.mimeType
        return if (sourceMimeType.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true)) {
            listOf(MediaFormat.MIMETYPE_VIDEO_HEVC, MediaFormat.MIMETYPE_VIDEO_AVC)
        } else {
            listOf(MediaFormat.MIMETYPE_VIDEO_AVC)
        }
    }

    private fun resolveOplusEffectiveRenderSpec(
        baseSpec: MotionPhotoRenderSpec,
        dynamicVideoProfile: DynamicVideoProfile?
    ): MotionPhotoRenderSpec {
        if (dynamicVideoProfile == null) return baseSpec
        val sourceDurationMs = dynamicVideoProfile.durationMs
            ?.coerceAtLeast(MIN_DYNAMIC_RENDER_DURATION_MS)
        val sourceFrameRate = dynamicVideoProfile.frameRate
            ?.coerceIn(1, MAX_OPLUS_RENDER_FRAME_RATE)
        val sourceLongSide = maxOf(
            dynamicVideoProfile.width ?: 0,
            dynamicVideoProfile.height ?: 0
        ).takeIf { it > 0 }
        val sourceBitrate = dynamicVideoProfile.bitrate
            ?.coerceAtLeast(baseSpec.videoBitrate)
        return baseSpec.copy(
            durationMs = sourceDurationMs ?: baseSpec.durationMs,
            frameRate = sourceFrameRate ?: baseSpec.frameRate,
            maxExportLongSide = maxOf(baseSpec.maxExportLongSide, sourceLongSide ?: 0),
            videoBitrate = sourceBitrate ?: baseSpec.videoBitrate
        )
    }

    private fun findTrackIndex(extractor: MediaExtractor, mimePrefix: String): Int? {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(android.media.MediaFormat.KEY_MIME)
            if (mime?.startsWith(mimePrefix, ignoreCase = true) == true) {
                return index
            }
        }
        return null
    }

    private fun resolveDynamicVideoProfile(
        context: Context,
        sourceItem: MediaItem
    ): DynamicVideoProfile? {
        val sourceVideo = resolveDynamicVideoSource(context, sourceItem) ?: return null
        val extractor = MediaExtractor()
        val retriever = MediaMetadataRetriever()
        return try {
            when (sourceVideo) {
                is DynamicVideoSource.FileSource -> {
                    extractor.setDataSource(sourceVideo.file.absolutePath)
                    retriever.setDataSource(sourceVideo.file.absolutePath)
                }
                is DynamicVideoSource.UriSource ->
                    context.contentResolver.openAssetFileDescriptor(sourceVideo.uri, "r")?.use { afd ->
                        extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    } ?: return null
            }
            val videoTrackIndex = findTrackIndex(extractor, "video/") ?: return null
            val videoFormat = extractor.getTrackFormat(videoTrackIndex)
            DynamicVideoProfile(
                durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.coerceAtLeast(MIN_DYNAMIC_RENDER_DURATION_MS),
                width = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()
                    ?.coerceAtLeast(1),
                height = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()
                    ?.coerceAtLeast(1),
                frameRate = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    ?.toFloatOrNull()
                    ?.let { kotlin.math.round(it).toInt() }
                    ?: if (videoFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
                    } else {
                        null
                    }
                    ?.coerceIn(1, MAX_OPLUS_RENDER_FRAME_RATE),
                bitrate = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toIntOrNull()
                    ?.coerceAtLeast(1)
                    ?: if (videoFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        videoFormat.getInteger(MediaFormat.KEY_BIT_RATE).coerceAtLeast(1)
                    } else {
                        null
                    },
                mimeType = videoFormat.getString(MediaFormat.KEY_MIME)
            )
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { extractor.release() }
            runCatching { retriever.release() }
        }
    }

    private fun copyExtractorTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        muxerTrackIndex: Int,
        durationLimitUs: Long
    ) {
        val maxBufferSize = (0 until extractor.trackCount)
            .mapNotNull { index ->
                extractor.getTrackFormat(index).let { format ->
                    if (format.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        format.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
                    } else {
                        null
                    }
                }
            }
            .maxOrNull()
            ?.coerceAtLeast(256 * 1024)
            ?: (256 * 1024)
        val buffer = ByteBuffer.allocate(maxBufferSize)
        val bufferInfo = android.media.MediaCodec.BufferInfo()
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            val sampleTimeUs = extractor.sampleTime
            if (sampleTimeUs < 0L || sampleTimeUs > durationLimitUs) break
            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = sampleTimeUs
            bufferInfo.flags = extractor.sampleFlags
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }
    }

    private fun resolveDynamicVideoSource(
        context: Context,
        sourceItem: MediaItem
    ): DynamicVideoSource? {
        sourceItem.livePhotoVideoUri?.let { liveUri ->
            return DynamicVideoSource.UriSource(liveUri)
        }
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
        return DynamicVideoSource.FileSource(extractedFile)
    }

    private fun decodeDynamicVideoFrame(
        retriever: MediaMetadataRetriever,
        timeUs: Long,
        exportFrameSize: ExportFrameSize?
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && exportFrameSize != null) {
            retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST,
                exportFrameSize.width,
                exportFrameSize.height
            )?.let { return it }
            retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                exportFrameSize.width,
                exportFrameSize.height
            )?.let { return it }
        }
        return retriever.getFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST
        ) ?: retriever.getFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
    }

    private fun resolveDynamicExportFrameSize(
        sourceWidth: Int?,
        sourceHeight: Int?,
        sourceRotation: Int,
        maxLongSide: Int
    ): ExportFrameSize? {
        val rawWidth = sourceWidth ?: return null
        val rawHeight = sourceHeight ?: return null
        val rotated = sourceRotation == 90 || sourceRotation == 270
        val width = if (rotated) rawHeight else rawWidth
        val height = if (rotated) rawWidth else rawHeight
        if (width <= 0 || height <= 0) return null
        val longSide = maxOf(width, height).coerceAtLeast(1)
        val scale = minOf(1f, maxLongSide.coerceAtLeast(240).toFloat() / longSide.toFloat())
        return ExportFrameSize(
            width = ((width * scale).toInt()).coerceAtLeast(2).toEven(),
            height = ((height * scale).toInt()).coerceAtLeast(2).toEven()
        )
    }

    private suspend fun buildOplusCompatibleCoverBitmap(
        context: Context,
        sourceItem: MediaItem,
        renderedCoverBitmap: Bitmap,
        traceState: ColorCardTraceFilterState,
        roundTripPayload: OplusMotionPhotoRoundTripPayload?
    ): Bitmap {
        val targetWidth = roundTripPayload?.primaryWidth?.coerceAtLeast(2) ?: return renderedCoverBitmap
        val targetHeight = roundTripPayload.primaryHeight?.coerceAtLeast(2) ?: return renderedCoverBitmap
        if (renderedCoverBitmap.width == targetWidth && renderedCoverBitmap.height == targetHeight) {
            return renderedCoverBitmap
        }
        val targetLongSide = maxOf(targetWidth, targetHeight)
        val rerendered = withContext(Dispatchers.Default) {
            decodeBitmapFromUriForOplusCover(
                context = context,
                uri = sourceItem.uri,
                maxLongSide = targetLongSide
            )?.let { sourceBitmap ->
                try {
                    renderColorCardTraceBitmapForExport(
                        context = context.applicationContext,
                        sourceBitmap = sourceBitmap,
                        traceState = traceState
                    )
                } finally {
                    if (!sourceBitmap.isRecycled) {
                        sourceBitmap.recycle()
                    }
                }
            }
        }
        rerendered?.let { bitmap ->
            return if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also { scaled ->
                    if (scaled !== bitmap && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
        }
        return Bitmap.createScaledBitmap(renderedCoverBitmap, targetWidth, targetHeight, true)
    }

    private fun decodeBitmapFromUriForOplusCover(
        context: Context,
        uri: Uri,
        maxLongSide: Int
    ): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null
        val longSide = maxOf(width, height).coerceAtLeast(1)
        var sampleSize = 1
        while (longSide / sampleSize > maxLongSide.coerceAtLeast(240) * 2) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private sealed interface DynamicVideoSource {
        data class FileSource(val file: File) : DynamicVideoSource
        data class UriSource(val uri: Uri) : DynamicVideoSource
    }

    private data class ExportFrameSize(
        val width: Int,
        val height: Int
    )

    private fun Int.toEven(): Int = if (this % 2 == 0) this else (this - 1).coerceAtLeast(2)

    private fun writeSingleFileMotionPhoto(
        context: Context,
        exportContainer: DynamicPhotoExportContainer,
        jpegFile: File,
        videoFile: File,
        outputFile: File,
        presentationTimestampUs: Long,
        xiaomiRoundTripPayload: XiaomiMotionPhotoRoundTripPayload? = null,
        oplusRoundTripPayload: OplusMotionPhotoRoundTripPayload? = null,
        googleSamsungRoundTripPayload: GoogleSamsungMotionPhotoRoundTripPayload? = null
    ) {
        when (exportContainer) {
            DynamicPhotoExportContainer.SamsungMotionPhoto -> {
                DynamicPhotoCompatWriter.writeSamsungMotionPhoto(
                    jpegFile = jpegFile,
                    videoFile = videoFile,
                    outputFile = outputFile,
                    presentationTimestampUs = googleSamsungRoundTripPayload?.presentationTimestampUs
                        ?: presentationTimestampUs,
                    roundTripPayload = googleSamsungRoundTripPayload
                )
            }
            DynamicPhotoExportContainer.OplusMotionPhoto -> {
                DynamicPhotoCompatWriter.writeOplusMotionPhoto(
                    jpegFile = jpegFile,
                    videoFile = videoFile,
                    outputFile = outputFile,
                    presentationTimestampUs = oplusRoundTripPayload?.presentationTimestampUs
                        ?: presentationTimestampUs,
                    roundTripPayload = oplusRoundTripPayload
                )
            }
            DynamicPhotoExportContainer.XiaomiMotionPhoto -> {
                DynamicPhotoCompatWriter.writeXiaomiMotionPhoto(
                    context = context,
                    jpegFile = jpegFile,
                    videoFile = videoFile,
                    outputFile = outputFile,
                    presentationTimestampUs = presentationTimestampUs,
                    roundTripPayload = xiaomiRoundTripPayload
                )
            }
            DynamicPhotoExportContainer.SingleFileMotionPhoto,
            DynamicPhotoExportContainer.VendorPair -> {
                DynamicPhotoCompatWriter.writeGoogleSamsungMotionPhoto(
                    jpegFile = jpegFile,
                    videoFile = videoFile,
                    outputFile = outputFile,
                    presentationTimestampUs = googleSamsungRoundTripPayload?.presentationTimestampUs
                        ?: presentationTimestampUs,
                    roundTripPayload = googleSamsungRoundTripPayload
                )
            }
        }
    }

    private fun resolveMotionPhotoPresentationTimestampUs(durationMs: Long): Long {
        val safeDurationMs = durationMs.coerceAtLeast(1L)
        return (safeDurationMs * 600L).coerceAtLeast(1L)
    }

    private fun buildMotionPhotoFileName(
        displayName: String,
        exportContainer: DynamicPhotoExportContainer
    ): String {
        if (exportContainer == DynamicPhotoExportContainer.OplusMotionPhoto) {
            val baseName = displayName.substringBeforeLast('.').ifBlank { "" }
            return if (
                baseName.startsWith("IMG", ignoreCase = true) &&
                baseName.length > "IMG".length
            ) {
                "$baseName.jpg"
            } else {
                "IMG${buildCompactTimestampFileStem()}.jpg"
            }
        }
        if (exportContainer == DynamicPhotoExportContainer.XiaomiMotionPhoto) {
            val rawBaseName = displayName.substringBeforeLast('.').ifBlank { "" }
            val baseName = rawBaseName.removeSuffix("_MP").removeSuffix("_mp")
            return if (
                baseName.startsWith("MVIMG_", ignoreCase = true) &&
                baseName.length > "MVIMG_".length
            ) {
                "$baseName.jpg"
            } else {
                "MVIMG_${buildTimestampFileStem()}.jpg"
            }
        }
        val baseName = displayName.substringBeforeLast('.').ifBlank { "motion_photo" }
        val prefixedBaseName = if (baseName.startsWith("MVIMG_", ignoreCase = true)) {
            baseName
        } else {
            "MVIMG_${baseName}"
        }
        val normalizedBaseName = if (prefixedBaseName.endsWith("MP", ignoreCase = true)) {
            prefixedBaseName
        } else {
            "${prefixedBaseName}_MP"
        }
        return "$normalizedBaseName.jpg"
    }

    private fun buildTimestampFileStem(): String {
        val formatter = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
        return formatter.format(java.util.Date(System.currentTimeMillis()))
    }

    private fun buildCompactTimestampFileStem(): String {
        val formatter = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
        return formatter.format(java.util.Date(System.currentTimeMillis()))
    }

    private fun buildVivoLivePhotoBaseName(displayName: String): String {
        val baseName = displayName.substringBeforeLast('.').ifBlank { "live_photo" }
        return if (baseName.startsWith("IMG_", ignoreCase = true)) {
            baseName
        } else {
            "IMG_$baseName"
        }
    }

    private fun saveMotionPhotoToGallery(
        context: Context,
        outputFile: File,
        displayName: String,
        exportContainer: DynamicPhotoExportContainer
    ): Uri {
        val resolver = context.contentResolver
        val nowSeconds = System.currentTimeMillis() / 1000
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
            put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(
                STANDARD_MIME_TYPE_EXTENSION_COLUMN,
                STANDARD_MIME_TYPE_EXTENSION_MOTION_PHOTO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    resolveMotionPhotoGalleryRelativePath(exportContainer)
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = insertMotionPhotoIntoMediaStore(
            context = context.applicationContext,
            resolver = resolver,
            values = values,
            displayName = displayName,
            exportContainer = exportContainer,
            outputFile = outputFile
        ) ?: run {
            AppRuntimeLogStore.logError(
                context.applicationContext,
                LOG_TAG,
                "MediaStore insert returned null " +
                    "displayName=$displayName exportContainer=$exportContainer " +
                    "output=${outputFile.absolutePath}"
            )
            Log.e(
                LOG_TAG,
                "MediaStore insert returned null " +
                    "displayName=$displayName exportContainer=$exportContainer output=${outputFile.absolutePath}"
            )
            throw MotionPhotoExportException("系统相册创建文件失败")
        }
        runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                outputFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Unable to open gallery output stream")
            applyMotionPhotoGalleryHints(
                resolver = resolver,
                uri = uri
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }
        }.onFailure { error ->
            AppRuntimeLogStore.logError(
                context.applicationContext,
                LOG_TAG,
                "saveMotionPhotoToGallery failed " +
                    "displayName=$displayName exportContainer=$exportContainer " +
                    "output=${outputFile.absolutePath} uri=$uri",
                error
            )
            Log.e(
                LOG_TAG,
                "saveMotionPhotoToGallery failed " +
                    "displayName=$displayName exportContainer=$exportContainer " +
                    "output=${outputFile.absolutePath} uri=$uri",
                error
            )
            runCatching { resolver.delete(uri, null, null) }
            throw MotionPhotoExportException(
                message = if (error is SecurityException) {
                    "没有相册访问权限"
                } else {
                    "系统相册写入失败"
                },
                cause = error
            )
        }
        return uri
    }

    private fun insertMotionPhotoIntoMediaStore(
        context: Context,
        resolver: android.content.ContentResolver,
        values: ContentValues,
        displayName: String,
        exportContainer: DynamicPhotoExportContainer,
        outputFile: File
    ): Uri? {
        return try {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (error: IllegalArgumentException) {
            if (!error.isUnsupportedStandardMimeTypeExtension()) {
                throw error
            }
            AppRuntimeLogStore.logInfo(
                context,
                LOG_TAG,
                "MediaStore insert rejected $STANDARD_MIME_TYPE_EXTENSION_COLUMN; " +
                    "retrying without hint displayName=$displayName " +
                    "exportContainer=$exportContainer output=${outputFile.absolutePath}"
            )
            Log.w(
                LOG_TAG,
                "MediaStore insert rejected $STANDARD_MIME_TYPE_EXTENSION_COLUMN; " +
                    "retrying without hint displayName=$displayName " +
                    "exportContainer=$exportContainer output=${outputFile.absolutePath}",
                error
            )
            values.remove(STANDARD_MIME_TYPE_EXTENSION_COLUMN)
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }

    internal fun resolveMotionPhotoGalleryRelativePath(
        exportContainer: DynamicPhotoExportContainer
    ): String {
        return when (exportContainer) {
            DynamicPhotoExportContainer.OplusMotionPhoto -> "DCIM/Camera"
            DynamicPhotoExportContainer.XiaomiMotionPhoto -> "DCIM/Camera"
            else -> "Pictures/Photoo/MotionPhoto"
        }
    }

    private fun applyMotionPhotoGalleryHints(
        resolver: android.content.ContentResolver,
        uri: Uri
    ) {
        runCatching {
            resolver.update(
                uri,
                ContentValues().apply {
                    put(
                        STANDARD_MIME_TYPE_EXTENSION_COLUMN,
                        STANDARD_MIME_TYPE_EXTENSION_MOTION_PHOTO
                    )
                },
                null,
                null
            )
        }
    }

    private fun Throwable.isUnsupportedStandardMimeTypeExtension(): Boolean {
        return generateSequence(this) { it.cause }.any { error ->
            error is IllegalArgumentException &&
                error.message?.contains(STANDARD_MIME_TYPE_EXTENSION_COLUMN, ignoreCase = true) == true
        }
    }

    private fun validateAndPersistMotionPhoto(
        context: Context,
        savedUri: Uri
    ) {
        val mediaItem = querySavedMediaItem(context, savedUri) ?: return
        MotionPhotoHelper.detectAndPersist(context, mediaItem)
    }

    private fun querySavedMediaItem(
        context: Context,
        savedUri: Uri
    ): MediaItem? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        return context.contentResolver.query(savedUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
            val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
            val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)).orEmpty()
            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)).orEmpty()
            MediaItem(
                id = id,
                uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                type = "photo",
                dateAdded = dateAdded,
                size = size,
                width = width,
                height = height,
                mimeType = mimeType,
                displayName = displayName,
                path = null
            )
        }
    }
}
