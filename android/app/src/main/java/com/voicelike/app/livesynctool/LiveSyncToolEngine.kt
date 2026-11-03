package com.voicelike.app.livesynctool

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object LiveSyncToolEngine {
    private const val standardMimeTypeExtensionColumn = "standard_mime_type_extension"
    private const val standardMimeTypeExtensionMotionPhoto = 2

    suspend fun convertSelected(
        context: Context,
        items: List<LiveSyncToolScanCandidate>,
        onProgress: (LiveSyncToolConversionProgress) -> Unit
    ): LiveSyncToolConversionReport = withContext(Dispatchers.IO) {
        val failures = ArrayList<LiveSyncToolConversionFailure>()
        val successes = ArrayList<LiveSyncToolConvertedItem>()
        var successCount = 0

        items.forEachIndexed { index, candidate ->
            onProgress(
                LiveSyncToolConversionProgress(
                    processed = index,
                    total = items.size,
                    currentName = candidate.mediaItem.displayName.ifBlank { "未命名照片" }
                )
            )
            try {
                val converted = convertSingle(context, candidate)
                successes += converted
                successCount += 1
            } catch (error: Throwable) {
                failures += LiveSyncToolConversionFailure(
                    item = candidate,
                    message = error.message ?: "转换失败"
                )
            }
        }

        onProgress(
            LiveSyncToolConversionProgress(
                processed = items.size,
                total = items.size,
                currentName = ""
            )
        )

        LiveSyncToolConversionReport(
            successes = successes,
            succeeded = successCount,
            failures = failures
        )
    }

    private suspend fun convertSingle(
        context: Context,
        candidate: LiveSyncToolScanCandidate
    ): LiveSyncToolConvertedItem {
        require(candidate.selectable) { candidate.reason ?: "当前项目不可转换" }
        require(candidate.targetFamily != LiveSyncToolTargetFamily.OplusMotionPhoto) {
            "OPPO 路线预留中，暂不开放"
        }

        val sessionDir = File(
            File(context.cacheDir, "live_sync_tool"),
            "job_${candidate.mediaItem.id}_${System.currentTimeMillis()}"
        ).apply { mkdirs() }

        try {
            val photoFile = preparePhotoFile(context, candidate.mediaItem, sessionDir)
            val videoFile = prepareVideoFile(context, candidate.mediaItem, sessionDir)
                ?: error("找不到可恢复的视频流")

            return when (candidate.targetFamily) {
                LiveSyncToolTargetFamily.SingleFileMotionPhoto -> {
                    ensureMp4Video(context, candidate.mediaItem.livePhotoVideoUri, videoFile)
                    val outputFile = File(sessionDir, buildMotionPhotoDisplayName(candidate.mediaItem))
                    DynamicPhotoCompatWriter.writeGoogleSamsungMotionPhoto(
                        jpegFile = photoFile,
                        videoFile = videoFile,
                        outputFile = outputFile
                    )
                    LiveSyncToolExifSupport.copyExifMetadataToFile(
                        resolver = context.contentResolver,
                        sourceUri = candidate.mediaItem.uri,
                        targetFile = outputFile,
                        excludedTags = setOf(ExifInterface.TAG_XMP)
                    )
                    val savedUri = saveMotionPhotoToGallery(
                        context = context,
                        outputFile = outputFile,
                        displayName = outputFile.name,
                        targetFamily = candidate.targetFamily
                    )
                    buildConvertedItem(candidate, newPhotoUri = savedUri)
                }

                LiveSyncToolTargetFamily.XiaomiMotionPhoto -> {
                    ensureMp4Video(context, candidate.mediaItem.livePhotoVideoUri, videoFile)
                    val outputFile = File(sessionDir, buildXiaomiDisplayName(candidate.mediaItem))
                    val roundTripPayload = candidate.mediaItem
                        .takeIf { it.isMotionPhoto }
                        ?.let { XiaomiMotionPhotoRoundTripSupport.extract(context, it) }
                        ?: XiaomiMotionPhotoRoundTripPayload(
                            presentationTimestampUs = null,
                            timeWatermark = null,
                            subImage = null,
                            preservedMetaBox = null,
                            primaryJpegShell = XiaomiMotionPhotoRoundTripSupport.extractPrimaryJpegShell(
                                context,
                                candidate.mediaItem
                            )
                        )
                    DynamicPhotoCompatWriter.writeXiaomiMotionPhoto(
                        context = context,
                        jpegFile = photoFile,
                        videoFile = videoFile,
                        outputFile = outputFile,
                        presentationTimestampUs = resolvePresentationTimestampUs(
                            context = context,
                            item = candidate.mediaItem,
                            videoFile = videoFile,
                            roundTripPayload = roundTripPayload
                        ),
                        roundTripPayload = roundTripPayload
                    )
                    val savedUri = saveMotionPhotoToGallery(
                        context = context,
                        outputFile = outputFile,
                        displayName = outputFile.name,
                        targetFamily = candidate.targetFamily
                    )
                    buildConvertedItem(candidate, newPhotoUri = savedUri)
                }

                LiveSyncToolTargetFamily.VendorPair -> {
                    val sourceSpec = VendorLivePhotoRoundTripSupport.prepareRoundTripSpec(
                        context = context,
                        sourceItem = candidate.mediaItem
                    )
                    val roundTripSpec = resolveTargetVendorRoundTripSpec(sourceSpec)
                    val pair = VendorLivePhotoRoundTripSupport.writeRoundTripPair(
                        jpegFile = photoFile,
                        videoFile = videoFile,
                        outputDir = sessionDir,
                        baseName = buildVendorPairBaseName(candidate.mediaItem),
                        spec = roundTripSpec
                    )
                    LiveSyncToolExifSupport.copyExifMetadataToFile(
                        resolver = context.contentResolver,
                        sourceUri = candidate.mediaItem.uri,
                        targetFile = pair.photoFile
                    )
                    val saved = VendorLivePhotoRoundTripSupport.savePairToGallery(
                        context = context,
                        pair = pair,
                        relativePath = "Pictures/Photoo/LivePhoto"
                    ) ?: error("系统相册写入失败")
                    buildConvertedItem(
                        candidate = candidate,
                        newPhotoUri = saved.photoUri,
                        newVideoUri = saved.videoUri
                    )
                }

                LiveSyncToolTargetFamily.OplusMotionPhoto -> {
                    error("OPPO 路线预留中，暂不开放")
                }
            }
        } finally {
            sessionDir.deleteRecursively()
        }
    }

    private fun buildConvertedItem(
        candidate: LiveSyncToolScanCandidate,
        newPhotoUri: Uri,
        newVideoUri: Uri? = null
    ): LiveSyncToolConvertedItem {
        return LiveSyncToolConvertedItem(
            sourceItem = candidate.mediaItem,
            sourceKind = candidate.sourceKind,
            targetFamily = candidate.targetFamily,
            oldUris = buildList {
                add(candidate.mediaItem.uri)
                candidate.mediaItem.livePhotoVideoUri?.let { add(it) }
            },
            newPhotoUri = newPhotoUri,
            newVideoUri = newVideoUri,
            displayName = candidate.mediaItem.displayName
        )
    }

    private fun preparePhotoFile(
        context: Context,
        item: LiveSyncToolMediaItem,
        sessionDir: File
    ): File {
        val targetFile = File(sessionDir, "source.jpg")
        val mime = item.mimeType.lowercase(Locale.ROOT)
        return if (mime.contains("jpeg") || mime.contains("jpg")) {
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                FileOutputStream(targetFile).use { output -> input.copyTo(output) }
            } ?: error("读取原图失败")
            targetFile
        } else {
            val bitmap = decodeBitmap(context, item.uri) ?: error("暂不支持当前图片格式")
            FileOutputStream(targetFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }
            LiveSyncToolExifSupport.copyExifMetadataToFile(
                resolver = context.contentResolver,
                sourceUri = item.uri,
                targetFile = targetFile,
                excludedTags = setOf(ExifInterface.TAG_XMP)
            )
            targetFile
        }
    }

    private fun prepareVideoFile(
        context: Context,
        item: LiveSyncToolMediaItem,
        sessionDir: File
    ): File? {
        val outputFile = File(sessionDir, "source.mp4")
        item.livePhotoVideoUri?.let { videoUri ->
            context.contentResolver.openInputStream(videoUri)?.use { input ->
                FileOutputStream(outputFile).use { output -> input.copyTo(output) }
            } ?: return null
            return outputFile.takeIf { it.exists() && it.length() > 0L }
        }
        if (!item.isMotionPhoto) return null
        return LiveSyncToolMotionSupport.extractVideo(
            context = context,
            uri = item.uri,
            info = LiveSyncToolMotionSupport.MotionPhotoInfo(
                isMotionPhoto = true,
                videoOffset = item.motionPhotoVideoOffset,
                videoLength = item.motionPhotoVideoLength
            ),
            outputFile = outputFile
        )
    }

    private fun saveMotionPhotoToGallery(
        context: Context,
        outputFile: File,
        displayName: String,
        targetFamily: LiveSyncToolTargetFamily
    ): Uri {
        val resolver = context.contentResolver
        val nowMs = System.currentTimeMillis()
        val nowSeconds = nowMs / 1000
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
            put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
            put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
            put(standardMimeTypeExtensionColumn, standardMimeTypeExtensionMotionPhoto)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    when (targetFamily) {
                        LiveSyncToolTargetFamily.XiaomiMotionPhoto -> "DCIM/Camera"
                        else -> "Pictures/Photoo/MotionPhoto"
                    }
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = insertIntoMediaStore(resolver, values) ?: error("系统相册创建文件失败")
        runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                outputFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("系统相册写入失败")

            runCatching {
                resolver.update(
                    uri,
                    ContentValues().apply {
                        put(standardMimeTypeExtensionColumn, standardMimeTypeExtensionMotionPhoto)
                    },
                    null,
                    null
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }
        }.getOrElse { error ->
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
        return uri
    }

    private fun insertIntoMediaStore(
        resolver: android.content.ContentResolver,
        values: ContentValues
    ): Uri? {
        return try {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (error: IllegalArgumentException) {
            if (error.message?.contains(standardMimeTypeExtensionColumn, ignoreCase = true) != true) {
                throw error
            }
            values.remove(standardMimeTypeExtensionColumn)
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }

    private fun ensureMp4Video(
        context: Context,
        videoUri: Uri?,
        videoFile: File
    ) {
        val type = videoUri?.let { context.contentResolver.getType(it) }.orEmpty()
        if (type.isNotBlank() && !type.equals("video/mp4", ignoreCase = true)) {
            error("暂不支持非 MP4 的动态视频")
        }
        if (!videoFile.extension.equals("mp4", ignoreCase = true)) {
            error("暂不支持当前视频格式")
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }.getOrNull()
        } else {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    }

    private fun resolvePresentationTimestampUs(
        context: Context,
        item: LiveSyncToolMediaItem,
        videoFile: File,
        roundTripPayload: XiaomiMotionPhotoRoundTripPayload?
    ): Long {
        roundTripPayload?.presentationTimestampUs?.takeIf { it > 0L }?.let { return it }
        val durationMs = item.duration.takeIf { it > 0L }
            ?: resolveVideoDurationMs(context, item.livePhotoVideoUri)
            ?: resolveVideoDurationMs(videoFile)
            ?: 1500L
        return (durationMs.coerceAtLeast(1L) * 600L).coerceAtLeast(1L)
    }

    private fun resolveVideoDurationMs(
        context: Context,
        videoUri: Uri?
    ): Long? {
        if (videoUri == null) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun resolveVideoDurationMs(
        videoFile: File
    ): Long? {
        if (!videoFile.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun buildFallbackVendorRoundTripSpecForCurrentDevice(): VendorLivePhotoRoundTripSpec {
        val targetShell = LiveSyncToolRouting.resolveVendorPairShell()
        val vendor = when (targetShell) {
            LiveSyncToolVendorPairShell.Vivo -> "vivo"
            LiveSyncToolVendorPairShell.HuaweiHarmony -> "paired_live_photo"
            LiveSyncToolVendorPairShell.GenericPaired -> "paired_live_photo"
        }
        return VendorLivePhotoRoundTripSpec(
            vendor = vendor,
            sourceShell = targetShell,
            imageTrailerBytes = ByteArray(0),
            videoTrailerBytes = ByteArray(0)
        )
    }

    private fun resolveTargetVendorRoundTripSpec(
        sourceSpec: VendorLivePhotoRoundTripSpec?
    ): VendorLivePhotoRoundTripSpec {
        val targetShell = LiveSyncToolRouting.resolveVendorPairShell()
        return when (targetShell) {
            LiveSyncToolVendorPairShell.Vivo -> VendorLivePhotoRoundTripSpec(
                vendor = "vivo",
                sourceShell = LiveSyncToolVendorPairShell.Vivo,
                imageTrailerBytes = ByteArray(0),
                videoTrailerBytes = ByteArray(0),
                liveToken = sourceSpec?.liveToken
            )
            LiveSyncToolVendorPairShell.HuaweiHarmony -> {
                if (sourceSpec?.sourceShell == LiveSyncToolVendorPairShell.HuaweiHarmony) {
                    sourceSpec.copy(vendor = "huawei_harmony", sourceShell = LiveSyncToolVendorPairShell.HuaweiHarmony)
                } else {
                    buildFallbackVendorRoundTripSpecForCurrentDevice()
                }
            }
            LiveSyncToolVendorPairShell.GenericPaired -> buildFallbackVendorRoundTripSpecForCurrentDevice()
        }
    }

    private fun buildMotionPhotoDisplayName(item: LiveSyncToolMediaItem): String {
        val baseName = item.displayName.substringBeforeLast(".").ifBlank { "motion_photo" }
        val prefixedBaseName = if (baseName.startsWith("MVIMG_", ignoreCase = true)) {
            baseName
        } else {
            "MVIMG_$baseName"
        }
        val normalized = if (prefixedBaseName.endsWith("MP", ignoreCase = true)) {
            prefixedBaseName
        } else {
            "${prefixedBaseName}_MP"
        }
        return "$normalized.jpg"
    }

    private fun buildXiaomiDisplayName(item: LiveSyncToolMediaItem): String {
        val baseName = item.displayName.substringBeforeLast(".").removeSuffix("_MP").removeSuffix("_mp")
        if (baseName.startsWith("MVIMG_", ignoreCase = true) && baseName.length > "MVIMG_".length) {
            return "$baseName.jpg"
        }
        return "MVIMG_${timestampStem()}.jpg"
    }

    private fun buildVendorPairBaseName(item: LiveSyncToolMediaItem): String {
        val baseName = item.displayName.substringBeforeLast(".").ifBlank { "live_photo" }
        return if (baseName.startsWith("IMG_", ignoreCase = true)) {
            baseName
        } else {
            "IMG_$baseName"
        }
    }

    private fun timestampStem(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return formatter.format(Date(System.currentTimeMillis()))
    }
}
