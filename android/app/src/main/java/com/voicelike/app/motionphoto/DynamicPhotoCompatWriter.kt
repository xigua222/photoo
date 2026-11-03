package com.voicelike.app.motionphoto

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.voicelike.app.DeviceModelNameResolver
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

internal data class MotionPhotoCompatWriteResult(
    val outputFile: File,
    val embeddedVideoLength: Long,
    val samsungMarkerLength: Int
)

internal data class VivoLivePhotoPairWriteResult(
    val photoFile: File,
    val videoFile: File,
    val manifestFile: File
)

internal data class VivoLivePhotoSaveResult(
    val photoUri: Uri,
    val videoUri: Uri,
    val manifestFile: File
)

internal object DynamicPhotoCompatWriter {
    private const val jpegApp1Marker = 0xE1
    private const val jpegApp2Marker = 0xE2
    private const val jpegStartOfScanMarker = 0xDA
    private const val jpegEndOfImageMarker = 0xD9
    private const val mpfApp2TiffHeaderRelativeOffset = 8
    private const val oplusPrimaryMpfAttribute = 0x00030000L
    private val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(StandardCharsets.ISO_8859_1)
    private val exifHeader = "Exif\u0000\u0000".toByteArray(StandardCharsets.ISO_8859_1)

    private data class Mp4BoxInfo(
        val offset: Int,
        val size: Int,
        val headerSize: Int,
        val type: String
    ) {
        val endOffset: Int get() = offset + size
        val payloadOffset: Int get() = offset + headerSize
    }

    private data class JpegSegmentInfo(
        val startOffset: Int,
        val marker: Int,
        val totalLength: Int,
        val payloadOffset: Int,
        val payloadLength: Int
    )

    fun writeGoogleSamsungMotionPhoto(
        jpegFile: File,
        videoFile: File,
        outputFile: File,
        presentationTimestampUs: Long,
        roundTripPayload: GoogleSamsungMotionPhotoRoundTripPayload? = null
    ): MotionPhotoCompatWriteResult {
        require(jpegFile.exists()) { "Missing jpeg file" }
        require(videoFile.exists()) { "Missing video file" }

        outputFile.parentFile?.mkdirs()
        val seededJpeg = File.createTempFile(
            "motion_photo_seed_",
            ".jpg",
            outputFile.parentFile ?: jpegFile.parentFile
        )
        try {
            jpegFile.inputStream().use { input ->
                seededJpeg.outputStream().use { output -> input.copyTo(output) }
            }

            val motionPayloadLength = videoFile.length().coerceAtLeast(0L)
            writeGoogleMotionPhotoXmp(
                jpegFile = seededJpeg,
                offsetFromEnd = motionPayloadLength,
                videoLength = motionPayloadLength,
                primaryPadding = 0L,
                presentationTimestampUs = presentationTimestampUs,
                baseXmp = roundTripPayload?.sourceXmp
            )

            FileOutputStream(outputFile).use { output ->
                seededJpeg.inputStream().use { input -> input.copyTo(output) }
                videoFile.inputStream().use { input -> input.copyTo(output) }
            }

            return MotionPhotoCompatWriteResult(
                outputFile = outputFile,
                embeddedVideoLength = motionPayloadLength,
                samsungMarkerLength = 0
            )
        } finally {
            runCatching { seededJpeg.delete() }
        }
    }

    fun writeSamsungMotionPhoto(
        jpegFile: File,
        videoFile: File,
        outputFile: File,
        presentationTimestampUs: Long,
        roundTripPayload: GoogleSamsungMotionPhotoRoundTripPayload? = null
    ): MotionPhotoCompatWriteResult {
        require(jpegFile.exists()) { "Missing jpeg file" }
        require(videoFile.exists()) { "Missing video file" }

        outputFile.parentFile?.mkdirs()
        val seededJpeg = File.createTempFile(
            "samsung_motion_photo_seed_",
            ".jpg",
            outputFile.parentFile ?: jpegFile.parentFile
        )
        try {
            jpegFile.inputStream().use { input ->
                seededJpeg.outputStream().use { output -> input.copyTo(output) }
            }

            val samsungPayload = SamsungMotionPhotoSupport.buildJpegPayload(
                videoBytes = videoFile.readBytes(),
                motionPhotoVersion = roundTripPayload?.samsungMotionPhotoVersion ?: "mpv3"
            )
            writeGoogleMotionPhotoXmp(
                jpegFile = seededJpeg,
                offsetFromEnd = samsungPayload.motionPayloadLength.toLong(),
                videoLength = samsungPayload.motionPayloadLength.toLong(),
                primaryPadding = samsungPayload.primaryPaddingLength.toLong(),
                presentationTimestampUs = presentationTimestampUs,
                baseXmp = roundTripPayload?.sourceXmp
            )

            FileOutputStream(outputFile).use { output ->
                seededJpeg.inputStream().use { input -> input.copyTo(output) }
                output.write(samsungPayload.footerBytes)
            }

            return MotionPhotoCompatWriteResult(
                outputFile = outputFile,
                embeddedVideoLength = samsungPayload.motionPayloadLength.toLong(),
                samsungMarkerLength = samsungPayload.primaryPaddingLength
            )
        } finally {
            runCatching { seededJpeg.delete() }
        }
    }

    fun writeXiaomiMotionPhoto(
        context: Context,
        jpegFile: File,
        videoFile: File,
        outputFile: File,
        presentationTimestampUs: Long,
        roundTripPayload: XiaomiMotionPhotoRoundTripPayload? = null
    ): MotionPhotoCompatWriteResult {
        require(jpegFile.exists()) { "Missing jpeg file" }
        require(videoFile.exists()) { "Missing video file" }

        outputFile.parentFile?.mkdirs()
        val make = normalizeMetadataLabel(Build.MANUFACTURER, fallback = "Xiaomi")
        val model = Build.MODEL.orEmpty().ifBlank { "Xiaomi" }
        val marketName = DeviceModelNameResolver.resolveCameraLabel(context, Build.MODEL)
        val effectiveRoundTripPayload = completeXiaomiRoundTripPayload(
            jpegFile = jpegFile,
            roundTripPayload = roundTripPayload
        )
        val injectedVideoFile = File.createTempFile(
            "xiaomi_motion_video_",
            ".mp4",
            outputFile.parentFile ?: videoFile.parentFile
        )
        val xiaomiVideoFile = injectXiaomiMotionPhotoMp4Metadata(
            videoFile = videoFile,
            outputFile = injectedVideoFile,
            androidVersion = resolveAndroidVersionForMetadata(),
            manufacturer = make,
            model = model,
            marketName = marketName,
            preservedMetaBox = effectiveRoundTripPayload?.preservedMetaBox
        )
        val subImageBytes = effectiveRoundTripPayload?.subImage?.bytes ?: ByteArray(0)
        val timeWatermarkBytes = effectiveRoundTripPayload?.timeWatermark?.bytes ?: ByteArray(0)
        val xmpPacket = buildXiaomiMotionPhotoXmp(
            videoOffsetFromEnd = xiaomiVideoFile.length().coerceAtLeast(0L),
            presentationTimestampUs = presentationTimestampUs.coerceAtLeast(1L),
            roundTripPayload = effectiveRoundTripPayload,
            timeWatermarkLength = timeWatermarkBytes.size.toLong(),
            subImageLength = subImageBytes.size.toLong()
        )
        val embeddedVideoLength = xiaomiVideoFile.length() +
            timeWatermarkBytes.size.toLong() +
            subImageBytes.size.toLong()
        val primaryJpegBytes = XiaomiJpegShellSupport.rewritePrimaryJpeg(
            renderedJpegBytes = jpegFile.readBytes(),
            shell = effectiveRoundTripPayload?.primaryJpegShell,
            xmpPacket = xmpPacket,
            make = make,
            model = model,
            marketName = marketName.ifBlank { model }
        )

        try {
            FileOutputStream(outputFile).use { output ->
                output.write(primaryJpegBytes)
                output.write(subImageBytes)
                output.write(timeWatermarkBytes)
                xiaomiVideoFile.inputStream().use { input -> input.copyTo(output) }
            }
        } finally {
            runCatching { injectedVideoFile.delete() }
        }

        return MotionPhotoCompatWriteResult(
            outputFile = outputFile,
            embeddedVideoLength = embeddedVideoLength,
            samsungMarkerLength = 0
        )
    }

    private fun completeXiaomiRoundTripPayload(
        jpegFile: File,
        roundTripPayload: XiaomiMotionPhotoRoundTripPayload?
    ): XiaomiMotionPhotoRoundTripPayload? {
        val timeWatermark = roundTripPayload?.timeWatermark ?: buildSyntheticXiaomiTimeWatermark(jpegFile)
        val subImage = roundTripPayload?.subImage ?: buildSyntheticXiaomiSubImage(jpegFile)
        return if (roundTripPayload == null) {
            if (timeWatermark == null && subImage == null) {
                null
            } else {
                XiaomiMotionPhotoRoundTripPayload(
                    presentationTimestampUs = null,
                    timeWatermark = timeWatermark,
                    subImage = subImage,
                    preservedMetaBox = null,
                    primaryJpegShell = null
                )
            }
        } else {
            roundTripPayload.copy(
                timeWatermark = timeWatermark,
                subImage = subImage
            )
        }
    }

    private fun buildSyntheticXiaomiTimeWatermark(
        jpegFile: File
    ): XiaomiMotionPhotoEmbeddedAsset? {
        val sourceBitmap = decodeXiaomiAuxBitmap(jpegFile) ?: return null
        return try {
            val width = 871
            val height = 119
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val srcTop = (sourceBitmap.height * 0.72f).toInt().coerceIn(0, sourceBitmap.height - 1)
            val srcRect = Rect(
                0,
                srcTop,
                sourceBitmap.width,
                sourceBitmap.height
            )
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(sourceBitmap, srcRect, dstRect, null)
            canvas.drawColor(Color.argb(112, 0, 0, 0))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 42f
            }
            canvas.drawText("Shot on Xiaomi", 28f, 78f, paint)
            val bytes = bitmap.toJpegBytes(90)
            bitmap.recycle()
            XiaomiMotionPhotoEmbeddedAsset(
                tagName = "timewatermark",
                bytes = bytes,
                attributes = linkedMapOf(
                    "offset" to "0",
                    "length" to bytes.size.toString(),
                    "width" to width.toString(),
                    "height" to height.toString(),
                    "paddingx" to "150",
                    "paddingy" to "124",
                    "isLTR" to "true"
                )
            )
        } finally {
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
        }
    }

    private fun buildSyntheticXiaomiSubImage(
        jpegFile: File
    ): XiaomiMotionPhotoEmbeddedAsset? {
        val sourceBitmap = decodeXiaomiAuxBitmap(jpegFile) ?: return null
        return try {
            val width = 120
            val height = 872
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val srcLeft = (sourceBitmap.width * 0.84f).toInt().coerceIn(0, sourceBitmap.width - 1)
            val srcRect = Rect(
                srcLeft,
                0,
                sourceBitmap.width,
                sourceBitmap.height
            )
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(sourceBitmap, srcRect, dstRect, null)
            val bytes = bitmap.toJpegBytes(90)
            bitmap.recycle()
            XiaomiMotionPhotoEmbeddedAsset(
                tagName = "subimage",
                bytes = bytes,
                attributes = linkedMapOf(
                    "offset" to "0",
                    "length" to bytes.size.toString(),
                    "paddingx" to "4380",
                    "paddingy" to "150",
                    "width" to width.toString(),
                    "height" to height.toString(),
                    "rotation" to "90"
                )
            )
        } finally {
            if (!sourceBitmap.isRecycled) sourceBitmap.recycle()
        }
    }

    private fun decodeXiaomiAuxBitmap(jpegFile: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(jpegFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sampleSize > 1600) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(jpegFile.absolutePath, options)
    }

    private fun Bitmap.toJpegBytes(quality: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }

    fun writeOplusMotionPhoto(
        jpegFile: File,
        videoFile: File,
        outputFile: File,
        presentationTimestampUs: Long,
        roundTripPayload: OplusMotionPhotoRoundTripPayload? = null
    ): MotionPhotoCompatWriteResult {
        require(jpegFile.exists()) { "Missing jpeg file" }
        require(videoFile.exists()) { "Missing video file" }

        outputFile.parentFile?.mkdirs()
        val seededJpeg = File.createTempFile(
            "oplus_motion_photo_seed_",
            ".jpg",
            outputFile.parentFile ?: jpegFile.parentFile
        )
        try {
            jpegFile.inputStream().use { input ->
                seededJpeg.outputStream().use { output -> input.copyTo(output) }
            }

            val preservedEmbeddedItems = roundTripPayload?.preservedEmbeddedItems
                ?.takeIf { it.isNotEmpty() }
                ?: roundTripPayload?.gainMapBytes
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { bytes ->
                        listOf(
                            OplusMotionPhotoEmbeddedItem(
                                mime = "image/jpeg",
                                semantic = "GainMap",
                                bytes = bytes
                            )
                        )
                    }
                ?: emptyList()
            val mpfEmbeddedItems = preservedEmbeddedItems.filter { item ->
                item.mime.equals("image/jpeg", ignoreCase = true)
            }
            val injectedVideoFile = File.createTempFile(
                "oplus_motion_video_",
                ".mp4",
                outputFile.parentFile ?: videoFile.parentFile
            )
            val oplusVideoFile = injectOplusMotionPhotoMp4Metadata(
                videoFile = videoFile,
                outputFile = injectedVideoFile,
                androidVersion = resolveAndroidVersionForMetadata(),
                preservedFtypBox = roundTripPayload?.preservedFtypBox,
                preservedWideBox = roundTripPayload?.preservedWideBox,
                preservedMetaTrackBoxes = roundTripPayload?.preservedMetaTrackBoxes.orEmpty(),
                preservedMetaBox = roundTripPayload?.preservedMetaBox,
                preservedLpexBox = roundTripPayload?.preservedLpexBox,
                preservedFreeBox = roundTripPayload?.preservedFreeBox
            )
            writeOplusMotionPhotoXmp(
                jpegFile = seededJpeg,
                videoLength = oplusVideoFile.length().coerceAtLeast(0L) +
                    (roundTripPayload?.preservedTailBytes?.size?.toLong() ?: 0L),
                gainMapLength = roundTripPayload?.gainMapBytes?.size?.toLong() ?: 0L,
                presentationTimestampUs = presentationTimestampUs.coerceAtLeast(1L),
                primaryPresentationTimestampUs = roundTripPayload?.primaryPresentationTimestampUs ?: 0L,
                opCameraVideoLength = oplusVideoFile.length().coerceAtLeast(0L),
                motionPhotoOwner = roundTripPayload?.motionPhotoOwner?.ifBlank { "oplus" } ?: "oplus",
                motionPhotoEnable = roundTripPayload?.motionPhotoEnable ?: true,
                hdrgmVersion = roundTripPayload?.hdrgmVersion,
                preservedEmbeddedItems = preservedEmbeddedItems
            )
            injectOplusMpfIntoPrimaryJpeg(
                jpegFile = seededJpeg,
                embeddedImageItems = mpfEmbeddedItems,
                preservedExifApp1Segment = roundTripPayload?.preservedExifApp1Segment,
                preservedAppSegments = roundTripPayload?.preservedPrimaryAppSegments.orEmpty()
            )
            val preservedTailBytes = roundTripPayload?.preservedTailBytes ?: ByteArray(0)
            val embeddedVideoLength = preservedEmbeddedItems.sumOf { it.totalLength } +
                oplusVideoFile.length() +
                preservedTailBytes.size.toLong()

            try {
                FileOutputStream(outputFile).use { output ->
                    seededJpeg.inputStream().use { input -> input.copyTo(output) }
                    preservedEmbeddedItems.forEach { item ->
                        output.write(item.bytes)
                        if (item.paddingBytes.isNotEmpty()) {
                            output.write(item.paddingBytes)
                        }
                    }
                    oplusVideoFile.inputStream().use { input -> input.copyTo(output) }
                    if (preservedTailBytes.isNotEmpty()) {
                        output.write(preservedTailBytes)
                    }
                }
            } finally {
                runCatching { injectedVideoFile.delete() }
            }

            return MotionPhotoCompatWriteResult(
                outputFile = outputFile,
                embeddedVideoLength = embeddedVideoLength,
                samsungMarkerLength = 0
            )
        } finally {
            runCatching { seededJpeg.delete() }
        }
    }

    private fun injectOplusMpfIntoPrimaryJpeg(
        jpegFile: File,
        embeddedImageItems: List<OplusMotionPhotoEmbeddedItem>,
        preservedExifApp1Segment: ByteArray? = null,
        preservedAppSegments: List<ByteArray> = emptyList()
    ) {
        if (!jpegFile.exists() || embeddedImageItems.isEmpty()) return
        val originalBytes = jpegFile.readBytes()
        val rewritten = injectOplusMpfIntoPrimaryJpegBytes(
            jpegBytes = originalBytes,
            embeddedImageItems = embeddedImageItems,
            preservedExifApp1Segment = preservedExifApp1Segment,
            preservedAppSegments = preservedAppSegments
        )
        if (rewritten.contentEquals(originalBytes)) return
        FileOutputStream(jpegFile).use { output ->
            output.write(rewritten)
        }
    }

    private fun injectOplusMpfIntoPrimaryJpegBytes(
        jpegBytes: ByteArray,
        embeddedImageItems: List<OplusMotionPhotoEmbeddedItem>,
        preservedExifApp1Segment: ByteArray? = null,
        preservedAppSegments: List<ByteArray> = emptyList()
    ): ByteArray {
        if (jpegBytes.size < 4 || embeddedImageItems.isEmpty()) return jpegBytes
        val sanitizedJpegBytes = removeExistingJpegMpfApp2Segment(jpegBytes)
        val segments = listJpegHeaderSegments(sanitizedJpegBytes)
        if (segments.isEmpty()) return sanitizedJpegBytes
        val rebuiltApp1Segments = buildList {
            preservedExifApp1Segment?.let(::add) ?: run {
                segments.firstOrNull { segment ->
                    isExifApp1Segment(sanitizedJpegBytes, segment)
                }?.let { segment ->
                    add(
                        sanitizedJpegBytes.copyOfRange(
                            segment.startOffset,
                            segment.startOffset + segment.totalLength
                        )
                    )
                }
            }
            segments.filter { segment ->
                isXmpApp1Segment(sanitizedJpegBytes, segment)
            }.forEach { segment ->
                add(
                    sanitizedJpegBytes.copyOfRange(
                        segment.startOffset,
                        segment.startOffset + segment.totalLength
                    )
                )
            }
        }
        val coreOffset = segments
            .firstOrNull { !isAppMarker(it.marker) }
            ?.startOffset
            ?: sanitizedJpegBytes.size
        val passthroughSegments = preservedAppSegments.takeIf { it.isNotEmpty() }
            ?: segments
                .filter { segment -> isAppMarker(segment.marker) && segment.marker != jpegApp1Marker }
                .map { segment ->
                    sanitizedJpegBytes.copyOfRange(
                        segment.startOffset,
                        segment.startOffset + segment.totalLength
                    )
                }
        val app1Length = rebuiltApp1Segments.sumOf { it.size }.toLong()
        val imageEntryCount = 1 + embeddedImageItems.count { item ->
            item.mime.equals("image/jpeg", ignoreCase = true)
        }
        val finalPrimaryLength = 2L +
            app1Length +
            calculateOplusMpfApp2SegmentTotalLength(imageEntryCount) +
            passthroughSegments.sumOf { it.size }.toLong() +
            (sanitizedJpegBytes.size - coreOffset).toLong()
        val mpfSegment = buildOplusMpfApp2Segment(
            primaryJpegLength = finalPrimaryLength,
            mpfSegmentStart = 2 + app1Length.toInt(),
            embeddedImageItems = embeddedImageItems
        )
        return buildByteArray(finalPrimaryLength.toInt()) {
            add(0xFF.toByte())
            add(0xD8.toByte())
            rebuiltApp1Segments.forEach { append(it) }
            append(mpfSegment)
            passthroughSegments.forEach { append(it) }
            append(sanitizedJpegBytes.copyOfRange(coreOffset, sanitizedJpegBytes.size))
        }
    }

    fun writeVivoLivePhotoPair(
        jpegFile: File,
        videoFile: File,
        outputDir: File,
        baseName: String
    ): VivoLivePhotoPairWriteResult {
        require(jpegFile.exists()) { "Missing jpeg file" }
        require(videoFile.exists()) { "Missing video file" }

        outputDir.mkdirs()
        val normalizedBaseName = sanitizeBaseName(baseName).ifBlank {
            "LIVE_${System.currentTimeMillis()}"
        }
        val photoTarget = File(outputDir, "$normalizedBaseName.jpg")
        val videoTarget = File(outputDir, "$normalizedBaseName.mp4")
        val manifestTarget = File(outputDir, "$normalizedBaseName.livephoto.json")

        jpegFile.inputStream().use { input ->
            photoTarget.outputStream().use { output -> input.copyTo(output) }
        }
        videoFile.inputStream().use { input ->
            videoTarget.outputStream().use { output -> input.copyTo(output) }
        }

        val pairedTimestamp = System.currentTimeMillis()
        photoTarget.setLastModified(pairedTimestamp)
        videoTarget.setLastModified(pairedTimestamp)

        val manifest = JSONObject()
            .put("vendor", "vivo")
            .put("format", "paired_live_photo")
            .put("pairingStrategy", "same_basename")
            .put("baseName", normalizedBaseName)
            .put("photoFileName", photoTarget.name)
            .put("videoFileName", videoTarget.name)
            .put("pairedTimestampMs", pairedTimestamp)
            .put(
                "note",
                "Legacy best-effort vivo export path kept for compatibility; active UI export now prefers single-file Motion Photo."
            )
        manifestTarget.writeText(manifest.toString(2))

        return VivoLivePhotoPairWriteResult(
            photoFile = photoTarget,
            videoFile = videoTarget,
            manifestFile = manifestTarget
        )
    }

    fun saveVivoLivePhotoPairToGallery(
        context: Context,
        pair: VivoLivePhotoPairWriteResult,
        relativePath: String = "Pictures/Photoo/LivePhoto"
    ): VivoLivePhotoSaveResult? {
        val resolver = context.contentResolver
        val nowMs = System.currentTimeMillis()
        val nowSeconds = nowMs / 1000
        val imageValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, pair.photoFile.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
            put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
            put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val videoValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, pair.videoFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, nowSeconds)
            put(MediaStore.Video.Media.DATE_MODIFIED, nowSeconds)
            put(MediaStore.Video.Media.DATE_TAKEN, nowMs)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val photoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues) ?: return null
        val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)
        if (videoUri == null) {
            runCatching { resolver.delete(photoUri, null, null) }
            return null
        }

        runCatching {
            copyFileToUri(resolver, pair.photoFile, photoUri)
            copyFileToUri(resolver, pair.videoFile, videoUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    photoUri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
                resolver.update(
                    videoUri,
                    ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }
        }.onFailure {
            runCatching { resolver.delete(photoUri, null, null) }
            runCatching { resolver.delete(videoUri, null, null) }
            return null
        }

        return VivoLivePhotoSaveResult(
            photoUri = photoUri,
            videoUri = videoUri,
            manifestFile = pair.manifestFile
        )
    }

    private fun writeGoogleMotionPhotoXmp(
        jpegFile: File,
        offsetFromEnd: Long,
        videoLength: Long,
        primaryPadding: Long,
        presentationTimestampUs: Long = -1L,
        baseXmp: String? = null
    ) {
        val exif = ExifInterface(jpegFile.absolutePath)
        exif.setAttribute(
            ExifInterface.TAG_XMP,
            buildGoogleMotionPhotoXmp(
                offsetFromEnd = offsetFromEnd,
                videoLength = videoLength,
                primaryPadding = primaryPadding,
                presentationTimestampUs = presentationTimestampUs,
                baseXmp = baseXmp
            )
        )
        exif.saveAttributes()
    }

    private fun writeXiaomiMotionPhotoXmp(
        jpegFile: File,
        videoOffsetFromEnd: Long,
        presentationTimestampUs: Long,
        roundTripPayload: XiaomiMotionPhotoRoundTripPayload?,
        timeWatermarkLength: Long,
        subImageLength: Long
    ) {
        val exif = ExifInterface(jpegFile.absolutePath)
        exif.setAttribute(
            ExifInterface.TAG_XMP,
            buildXiaomiMotionPhotoXmp(
                videoOffsetFromEnd = videoOffsetFromEnd,
                presentationTimestampUs = presentationTimestampUs,
                roundTripPayload = roundTripPayload,
                timeWatermarkLength = timeWatermarkLength,
                subImageLength = subImageLength
            )
        )
        exif.saveAttributes()
    }

    private fun writeOplusMotionPhotoXmp(
        jpegFile: File,
        videoLength: Long,
        gainMapLength: Long,
        presentationTimestampUs: Long,
        primaryPresentationTimestampUs: Long,
        opCameraVideoLength: Long,
        motionPhotoOwner: String,
        motionPhotoEnable: Boolean,
        hdrgmVersion: String?,
        preservedEmbeddedItems: List<OplusMotionPhotoEmbeddedItem> = emptyList()
    ) {
        val exif = ExifInterface(jpegFile.absolutePath)
        exif.setAttribute(
            ExifInterface.TAG_XMP,
            buildOplusMotionPhotoXmp(
                videoLength = videoLength,
                gainMapLength = gainMapLength,
                presentationTimestampUs = presentationTimestampUs,
                primaryPresentationTimestampUs = primaryPresentationTimestampUs,
                opCameraVideoLength = opCameraVideoLength,
                motionPhotoOwner = motionPhotoOwner,
                motionPhotoEnable = motionPhotoEnable,
                hdrgmVersion = hdrgmVersion,
                preservedEmbeddedItems = preservedEmbeddedItems
            )
        )
        exif.saveAttributes()
    }

    internal fun buildGoogleMotionPhotoXmp(
        offsetFromEnd: Long,
        videoLength: Long,
        primaryPadding: Long,
        presentationTimestampUs: Long,
        baseXmp: String? = null
    ): String {
        mergeGoogleMotionPhotoXmp(
            sourceXmp = baseXmp,
            offsetFromEnd = offsetFromEnd,
            videoLength = videoLength,
            primaryPadding = primaryPadding,
            presentationTimestampUs = presentationTimestampUs
        )?.let { return it }
        return """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description
                  rdf:about=""
                  xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                  xmlns:OpCamera="http://ns.oplus.com/photos/1.0/camera/"
                  xmlns:Container="http://ns.google.com/photos/1.0/container/"
                  xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
                  xmlns:GContainer="http://ns.google.com/photos/1.0/container/"
                  xmlns:GContainerItem="http://ns.google.com/photos/1.0/container/item/"
                  GCamera:MotionPhoto="1"
                  GCamera:MotionPhotoVersion="1"
                  GCamera:MotionPhotoPresentationTimestampUs="$presentationTimestampUs"
                  GCamera:MicroVideo="1"
                  GCamera:MicroVideoVersion="1"
                  GCamera:MicroVideoOffset="$offsetFromEnd"
                  GCamera:MicroVideoPresentationTimestampUs="0"
                  OpCamera:MotionPhotoPrimaryPresentationTimestampUs="0"
                  OpCamera:MotionPhotoOwner="oplus"
                  OpCamera:OLivePhotoVersion="2"
                  OpCamera:VideoLength="$offsetFromEnd">
                  <Container:Directory>
                    <rdf:Seq>
                      <rdf:li
                        rdf:parseType="Resource"
                        Item:Mime="image/jpeg"
                        Item:Semantic="Primary"
                        Item:Length="0"
                        Item:Padding="$primaryPadding" />
                      <rdf:li
                        rdf:parseType="Resource"
                        Item:Mime="video/mp4"
                        Item:Semantic="MotionPhoto"
                        Item:Length="$videoLength"
                        Item:Padding="0" />
                    </rdf:Seq>
                  </Container:Directory>
                  <GContainer:Directory>
                    <rdf:Seq>
                      <rdf:li
                        rdf:parseType="Resource"
                        GContainerItem:Mime="image/jpeg"
                        GContainerItem:Semantic="Primary"
                        GContainerItem:Length="0"
                        GContainerItem:Padding="$primaryPadding" />
                      <rdf:li
                        rdf:parseType="Resource"
                        GContainerItem:Mime="video/mp4"
                        GContainerItem:Semantic="MotionPhoto"
                        GContainerItem:Length="$videoLength"
                        GContainerItem:Padding="0" />
                    </rdf:Seq>
                  </GContainer:Directory>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }

    private fun mergeGoogleMotionPhotoXmp(
        sourceXmp: String?,
        offsetFromEnd: Long,
        videoLength: Long,
        primaryPadding: Long,
        presentationTimestampUs: Long
    ): String? {
        val originalXmp = sourceXmp?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val descriptionMatch = Regex("""<rdf:Description\b[^>]*?/?\s*>""", RegexOption.DOT_MATCHES_ALL)
            .find(originalXmp)
            ?: return null
        val sanitizedXmp = originalXmp
            .replace(
                Regex("""<Container:Directory>.*?</Container:Directory>""", RegexOption.DOT_MATCHES_ALL),
                ""
            )
            .replace(
                Regex("""<GContainer:Directory>.*?</GContainer:Directory>""", RegexOption.DOT_MATCHES_ALL),
                ""
            )
        val originalDescriptionTag = descriptionMatch.value
        val rebuiltDescriptionTag = buildGoogleMotionPhotoDescriptionTag(
            sourceTag = originalDescriptionTag,
            offsetFromEnd = offsetFromEnd,
            presentationTimestampUs = presentationTimestampUs
        )
        val motionDirectoryBlock = buildGoogleMotionPhotoDirectoryBlock(
            videoLength = videoLength,
            primaryPadding = primaryPadding
        )
        return if (originalDescriptionTag.trimEnd().endsWith("/>")) {
            sanitizedXmp.replaceFirst(
                originalDescriptionTag,
                openXmlTag(rebuiltDescriptionTag) + "$motionDirectoryBlock</rdf:Description>"
            )
        } else {
            sanitizedXmp
                .replaceFirst(originalDescriptionTag, rebuiltDescriptionTag)
                .replaceFirst("</rdf:Description>", "$motionDirectoryBlock</rdf:Description>")
        }
    }

    private fun buildGoogleMotionPhotoDescriptionTag(
        sourceTag: String,
        offsetFromEnd: Long,
        presentationTimestampUs: Long
    ): String {
        var tag = sourceTag
            .replace(Regex("""\s+GCamera:MotionPhoto\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+GCamera:MotionPhotoVersion\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+GCamera:MotionPhotoPresentationTimestampUs\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+GCamera:MicroVideo\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+GCamera:MicroVideoVersion\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+GCamera:MicroVideoOffset\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+GCamera:MicroVideoPresentationTimestampUs\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+OpCamera:MotionPhotoPrimaryPresentationTimestampUs\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+OpCamera:MotionPhotoOwner\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+OpCamera:OLivePhotoVersion\s*=\s*['"][^'"]*['"]"""), "")
            .replace(Regex("""\s+OpCamera:VideoLength\s*=\s*['"][^'"]*['"]"""), "")
        tag = ensureXmlNamespace(tag, "GCamera", "http://ns.google.com/photos/1.0/camera/")
        tag = ensureXmlNamespace(tag, "OpCamera", "http://ns.oplus.com/photos/1.0/camera/")
        tag = ensureXmlNamespace(tag, "Container", "http://ns.google.com/photos/1.0/container/")
        tag = ensureXmlNamespace(tag, "Item", "http://ns.google.com/photos/1.0/container/item/")
        tag = ensureXmlNamespace(tag, "GContainer", "http://ns.google.com/photos/1.0/container/")
        tag = ensureXmlNamespace(tag, "GContainerItem", "http://ns.google.com/photos/1.0/container/item/")
        tag = setXmlAttribute(tag, "GCamera:MotionPhoto", "1")
        tag = setXmlAttribute(tag, "GCamera:MotionPhotoVersion", "1")
        tag = setXmlAttribute(tag, "GCamera:MotionPhotoPresentationTimestampUs", presentationTimestampUs.toString())
        tag = setXmlAttribute(tag, "GCamera:MicroVideo", "1")
        tag = setXmlAttribute(tag, "GCamera:MicroVideoVersion", "1")
        tag = setXmlAttribute(tag, "GCamera:MicroVideoOffset", offsetFromEnd.toString())
        tag = setXmlAttribute(tag, "GCamera:MicroVideoPresentationTimestampUs", "0")
        tag = setXmlAttribute(tag, "OpCamera:MotionPhotoPrimaryPresentationTimestampUs", "0")
        tag = setXmlAttribute(tag, "OpCamera:MotionPhotoOwner", "oplus")
        tag = setXmlAttribute(tag, "OpCamera:OLivePhotoVersion", "2")
        return setXmlAttribute(tag, "OpCamera:VideoLength", offsetFromEnd.toString())
    }

    private fun buildGoogleMotionPhotoDirectoryBlock(
        videoLength: Long,
        primaryPadding: Long
    ): String {
        return """
                  <Container:Directory>
                    <rdf:Seq>
                      <rdf:li
                        rdf:parseType="Resource"
                        Item:Mime="image/jpeg"
                        Item:Semantic="Primary"
                        Item:Length="0"
                        Item:Padding="$primaryPadding" />
                      <rdf:li
                        rdf:parseType="Resource"
                        Item:Mime="video/mp4"
                        Item:Semantic="MotionPhoto"
                        Item:Length="$videoLength"
                        Item:Padding="0" />
                    </rdf:Seq>
                  </Container:Directory>
                  <GContainer:Directory>
                    <rdf:Seq>
                      <rdf:li
                        rdf:parseType="Resource"
                        GContainerItem:Mime="image/jpeg"
                        GContainerItem:Semantic="Primary"
                        GContainerItem:Length="0"
                        GContainerItem:Padding="$primaryPadding" />
                      <rdf:li
                        rdf:parseType="Resource"
                        GContainerItem:Mime="video/mp4"
                        GContainerItem:Semantic="MotionPhoto"
                        GContainerItem:Length="$videoLength"
                        GContainerItem:Padding="0" />
                    </rdf:Seq>
                  </GContainer:Directory>
        """.trimIndent()
    }

    private fun ensureXmlNamespace(
        sourceTag: String,
        prefix: String,
        uri: String
    ): String {
        if (Regex("""\bxmlns:$prefix\s*=""").containsMatchIn(sourceTag)) return sourceTag
        return insertIntoXmlTag(sourceTag, "\n                  xmlns:$prefix=\"$uri\"")
    }

    private fun setXmlAttribute(
        sourceTag: String,
        name: String,
        value: String
    ): String {
        val attributeRegex = Regex("""\s+$name\s*=\s*['"][^'"]*['"]""")
        val replacement = "\n                  $name=\"$value\""
        return if (attributeRegex.containsMatchIn(sourceTag)) {
            sourceTag.replace(attributeRegex, replacement)
        } else {
            insertIntoXmlTag(sourceTag, replacement)
        }
    }

    private fun insertIntoXmlTag(
        sourceTag: String,
        insertion: String
    ): String {
        val selfClosingIndex = sourceTag.lastIndexOf("/>")
        if (selfClosingIndex >= 0) {
            return sourceTag.substring(0, selfClosingIndex) +
                insertion +
                sourceTag.substring(selfClosingIndex)
        }
        val closingIndex = sourceTag.lastIndexOf('>')
        if (closingIndex < 0) return sourceTag + insertion
        return sourceTag.substring(0, closingIndex) + insertion + sourceTag.substring(closingIndex)
    }

    private fun openXmlTag(sourceTag: String): String {
        val selfClosingIndex = sourceTag.lastIndexOf("/>")
        return if (selfClosingIndex >= 0) {
            sourceTag.substring(0, selfClosingIndex) + ">"
        } else {
            sourceTag
        }
    }

    private fun buildXiaomiMotionPhotoXmp(
        videoOffsetFromEnd: Long,
        presentationTimestampUs: Long,
        roundTripPayload: XiaomiMotionPhotoRoundTripPayload?,
        timeWatermarkLength: Long,
        subImageLength: Long
    ): String {
        val embeddedXmpMeta = escapeXmlAttribute(
            buildXiaomiEmbeddedXmpMeta(
                roundTripPayload = roundTripPayload,
                videoOffsetFromEnd = videoOffsetFromEnd,
                timeWatermarkLength = timeWatermarkLength,
                subImageLength = subImageLength
            )
        )
        return """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description
                  rdf:about=""
                  xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                  xmlns:MiCamera="http://ns.xiaomi.com/photos/1.0/camera/"
                  GCamera:MicroVideoVersion="1"
                  GCamera:MicroVideo="1"
                  GCamera:MicroVideoOffset="$videoOffsetFromEnd"
                  GCamera:MicroVideoPresentationTimestampUs="$presentationTimestampUs"
                  MiCamera:XMPMeta="$embeddedXmpMeta"/>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }

    internal fun buildOplusMotionPhotoXmp(
        videoLength: Long,
        gainMapLength: Long,
        presentationTimestampUs: Long,
        primaryPresentationTimestampUs: Long,
        opCameraVideoLength: Long,
        motionPhotoOwner: String,
        motionPhotoEnable: Boolean,
        hdrgmVersion: String?,
        preservedEmbeddedItems: List<OplusMotionPhotoEmbeddedItem> = emptyList()
    ): String {
        val effectiveEmbeddedItems = if (preservedEmbeddedItems.isNotEmpty()) {
            preservedEmbeddedItems.map { item ->
                OplusEmbeddedItemDescriptor(
                    mime = item.mime,
                    semantic = item.semantic,
                    length = item.bytes.size.toLong(),
                    padding = item.paddingBytes.size.toLong()
                )
            }
        } else if (gainMapLength > 0L) {
            listOf(
                OplusEmbeddedItemDescriptor(
                    mime = "image/jpeg",
                    semantic = "GainMap",
                    length = gainMapLength,
                    padding = 0L
                )
            )
        } else {
            emptyList()
        }
        val hasGainMap = effectiveEmbeddedItems.any {
            it.semantic.equals("GainMap", ignoreCase = true)
        }
        val hdrgmNamespace = if (hasGainMap || !hdrgmVersion.isNullOrBlank()) {
            """xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/""""
        } else {
            ""
        }
        val hdrgmAttribute = if (hasGainMap || !hdrgmVersion.isNullOrBlank()) {
            """hdrgm:Version="${escapeXmlAttribute(hdrgmVersion.orEmpty().ifBlank { "1.0" })}""""
        } else {
            ""
        }
        val motionPhotoEnableAttribute = if (motionPhotoEnable) {
            """OpCamera:MotionPhotoEnable="True""""
        } else {
            ""
        }
        val preservedDirectoryItems = effectiveEmbeddedItems.joinToString("\n") { item ->
            buildOplusContainerItemXml(
                mime = item.mime,
                semantic = item.semantic,
                length = item.length,
                padding = item.padding
            )
        }
        val embeddedDirectoryBlock = if (preservedDirectoryItems.isNotBlank()) {
            "\n$preservedDirectoryItems"
        } else {
            ""
        }
        return """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description
                  rdf:about=""
                  xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                  xmlns:OpCamera="http://ns.oplus.com/photos/1.0/camera/"
                  xmlns:Container="http://ns.google.com/photos/1.0/container/"
                  xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
                  $hdrgmNamespace
                  $hdrgmAttribute
                  GCamera:MotionPhoto="1"
                  GCamera:MotionPhotoVersion="1"
                  GCamera:MotionPhotoPresentationTimestampUs="$presentationTimestampUs"
                  OpCamera:MotionPhotoPrimaryPresentationTimestampUs="$primaryPresentationTimestampUs"
                  OpCamera:MotionPhotoOwner="${escapeXmlAttribute(motionPhotoOwner)}"
                  OpCamera:OLivePhotoVersion="2"
                  OpCamera:VideoLength="$opCameraVideoLength"
                  $motionPhotoEnableAttribute>
                  <Container:Directory>
                    <rdf:Seq>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="image/jpeg"
                          Item:Semantic="Primary"
                          Item:Length="0"
                          Item:Padding="0"/>
                      </rdf:li>
                      $embeddedDirectoryBlock
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="video/mp4"
                          Item:Semantic="MotionPhoto"
                          Item:Length="$videoLength"
                          Item:Padding="0"/>
                      </rdf:li>
                    </rdf:Seq>
                  </Container:Directory>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }

    private fun buildOplusContainerItemXml(
        mime: String,
        semantic: String,
        length: Long,
        padding: Long
    ): String {
        return """
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="${escapeXmlAttribute(mime)}"
                          Item:Semantic="${escapeXmlAttribute(semantic)}"
                          Item:Length="$length"
                          Item:Padding="$padding"/>
                      </rdf:li>
        """.trimIndent()
    }

    private data class OplusEmbeddedItemDescriptor(
        val mime: String,
        val semantic: String,
        val length: Long,
        val padding: Long
    )

    private fun buildOplusMpfApp2Segment(
        primaryJpegLength: Long,
        mpfSegmentStart: Int,
        embeddedImageItems: List<OplusMotionPhotoEmbeddedItem>
    ): ByteArray {
        val imageItems = embeddedImageItems.filter { item ->
            item.mime.equals("image/jpeg", ignoreCase = true)
        }
        if (imageItems.isEmpty()) return ByteArray(0)

        val entryCount = 1 + imageItems.size
        val ifdEntryCount = 3
        val mpEntryOffset = 8 + 2 + (ifdEntryCount * 12) + 4
        val tiffPayload = buildByteArray(64 + entryCount * 16) {
            append("MM".toByteArray(StandardCharsets.ISO_8859_1))
            append(unsignedShortToByteArray(0x002A))
            append(intToByteArray(8))
            append(unsignedShortToByteArray(ifdEntryCount))
            append(buildMpfIfdEntry(tag = 0xB000, type = 7, count = 4, valueBytes = "0100".toByteArray(StandardCharsets.ISO_8859_1)))
            append(buildMpfIfdEntry(tag = 0xB001, type = 4, count = 1, valueBytes = intToByteArray(entryCount)))
            append(buildMpfIfdEntry(tag = 0xB002, type = 7, count = entryCount * 16, valueBytes = intToByteArray(mpEntryOffset)))
            append(intToByteArray(0))
            append(
                buildOplusMpfEntries(
                    primaryJpegLength = primaryJpegLength,
                    mpfSegmentStart = mpfSegmentStart,
                    imageItems = imageItems
                )
            )
        }
        val payload = buildByteArray(4 + tiffPayload.size) {
            append("MPF\u0000".toByteArray(StandardCharsets.ISO_8859_1))
            append(tiffPayload)
        }
        return buildByteArray(payload.size + 4) {
            add(0xFF.toByte())
            add(jpegApp2Marker.toByte())
            append(unsignedShortToByteArray(payload.size + 2))
            append(payload)
        }
    }

    private fun buildMpfIfdEntry(
        tag: Int,
        type: Int,
        count: Int,
        valueBytes: ByteArray
    ): ByteArray {
        require(valueBytes.size == 4) { "MPF IFD inline value must be 4 bytes" }
        return buildByteArray(12) {
            append(unsignedShortToByteArray(tag))
            append(unsignedShortToByteArray(type))
            append(intToByteArray(count))
            append(valueBytes)
        }
    }

    private fun buildOplusMpfEntries(
        primaryJpegLength: Long,
        mpfSegmentStart: Int,
        imageItems: List<OplusMotionPhotoEmbeddedItem>
    ): ByteArray {
        val mpfTiffOffset = mpfSegmentStart + mpfApp2TiffHeaderRelativeOffset
        var cursor = primaryJpegLength
        return buildByteArray(imageItems.size * 16 + 16) {
            append(buildMpfEntry(attribute = oplusPrimaryMpfAttribute, size = primaryJpegLength, dataOffset = 0L))
            imageItems.forEach { item ->
                val dataOffset = cursor - mpfTiffOffset
                append(
                    buildMpfEntry(
                        attribute = 0L,
                        size = item.bytes.size.toLong(),
                        dataOffset = dataOffset
                    )
                )
                cursor += item.totalLength
            }
        }
    }

    private fun buildMpfEntry(
        attribute: Long,
        size: Long,
        dataOffset: Long,
        dependentImage1: Int = 0,
        dependentImage2: Int = 0
    ): ByteArray {
        require(attribute in 0L..0xffffffffL) { "Invalid MPF attribute" }
        require(size in 0L..0xffffffffL) { "Invalid MPF image size" }
        require(dataOffset in 0L..0xffffffffL) { "Invalid MPF data offset" }
        return buildByteArray(16) {
            append(intToByteArray(attribute.toInt()))
            append(intToByteArray(size.toInt()))
            append(intToByteArray(dataOffset.toInt()))
            append(unsignedShortToByteArray(dependentImage1))
            append(unsignedShortToByteArray(dependentImage2))
        }
    }

    private fun calculateOplusMpfApp2SegmentTotalLength(entryCount: Int): Long {
        val ifdEntryCount = 3
        val payloadSize = 4 + 8 + 2 + (ifdEntryCount * 12) + 4 + (entryCount * 16)
        return (2 + 2 + payloadSize).toLong()
    }

    private fun listJpegHeaderSegments(bytes: ByteArray): List<JpegSegmentInfo> {
        if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) {
            return emptyList()
        }
        val result = ArrayList<JpegSegmentInfo>()
        var cursor = 2
        while (cursor + 4 <= bytes.size) {
            if (bytes[cursor] != 0xFF.toByte()) break
            val markerStart = cursor
            while (cursor < bytes.size && bytes[cursor] == 0xFF.toByte()) {
                cursor += 1
            }
            if (cursor >= bytes.size) break
            val marker = bytes[cursor].toInt() and 0xFF
            cursor += 1
            if (marker == jpegStartOfScanMarker || marker == jpegEndOfImageMarker) {
                break
            }
            if (cursor + 2 > bytes.size) break
            val segmentLength = ((bytes[cursor].toInt() and 0xFF) shl 8) or
                (bytes[cursor + 1].toInt() and 0xFF)
            if (segmentLength < 2) break
            val segmentEnd = cursor + segmentLength
            if (segmentEnd > bytes.size) break
            result += JpegSegmentInfo(
                startOffset = markerStart,
                marker = marker,
                totalLength = 2 + segmentLength,
                payloadOffset = cursor + 2,
                payloadLength = segmentLength - 2
            )
            cursor = segmentEnd
        }
        return result
    }

    private fun isAppMarker(marker: Int): Boolean {
        return marker in 0xE0..0xEF
    }

    private fun isExifApp1Segment(bytes: ByteArray, segment: JpegSegmentInfo): Boolean {
        return segment.marker == jpegApp1Marker &&
            segment.payloadLength >= exifHeader.size &&
            bytes.copyOfRange(
                segment.payloadOffset,
                segment.payloadOffset + exifHeader.size
            ).contentEquals(exifHeader)
    }

    private fun isXmpApp1Segment(bytes: ByteArray, segment: JpegSegmentInfo): Boolean {
        return segment.marker == jpegApp1Marker &&
            segment.payloadLength >= xmpHeader.size &&
            bytes.copyOfRange(
                segment.payloadOffset,
                segment.payloadOffset + xmpHeader.size
            ).contentEquals(xmpHeader)
    }

    private fun removeExistingJpegMpfApp2Segment(bytes: ByteArray): ByteArray {
        val segments = listJpegHeaderSegments(bytes)
        val mpfSegment = segments.firstOrNull { segment ->
            segment.marker == jpegApp2Marker &&
                segment.payloadLength >= 4 &&
                bytes.copyOfRange(segment.payloadOffset, segment.payloadOffset + 4)
                    .toString(StandardCharsets.ISO_8859_1) == "MPF\u0000"
        } ?: return bytes
        return buildByteArray(bytes.size - mpfSegment.totalLength) {
            append(bytes.copyOfRange(0, mpfSegment.startOffset))
            append(bytes.copyOfRange(mpfSegment.startOffset + mpfSegment.totalLength, bytes.size))
        }
    }

    private fun unsignedShortToByteArray(value: Int): ByteArray {
        require(value in 0..0xffff) { "Value out of unsigned short range" }
        return byteArrayOf(
            ((value ushr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private data class Mp4FileBoxInfo(
        val offset: Long,
        val size: Long,
        val headerSize: Int,
        val type: String
    ) {
        val endOffset: Long get() = offset + size
    }

    private fun buildXiaomiEmbeddedXmpMeta(
        roundTripPayload: XiaomiMotionPhotoRoundTripPayload?,
        videoOffsetFromEnd: Long,
        timeWatermarkLength: Long,
        subImageLength: Long
    ): String {
        val embeddedTags = ArrayList<String>(2)
        roundTripPayload?.timeWatermark?.let { asset ->
            embeddedTags += renderXiaomiEmbeddedAssetTag(
                asset = asset,
                offsetFromEnd = videoOffsetFromEnd + timeWatermarkLength,
                length = timeWatermarkLength
            )
        } ?: embeddedTags.add(
            """<timewatermark offset="0" length="0" width="0" height="0" paddingx="0" paddingy="0" isLTR="true" />"""
        )
        roundTripPayload?.subImage?.let { asset ->
            embeddedTags += renderXiaomiEmbeddedAssetTag(
                asset = asset,
                offsetFromEnd = videoOffsetFromEnd + timeWatermarkLength + subImageLength,
                length = subImageLength
            )
        } ?: embeddedTags.add(
            """<subimage offset="0" length="0" paddingx="0" paddingy="0" width="0" height="0" rotation="0" />"""
        )
        return """<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>${embeddedTags.joinToString("")}"""
    }

    private fun renderXiaomiEmbeddedAssetTag(
        asset: XiaomiMotionPhotoEmbeddedAsset,
        offsetFromEnd: Long,
        length: Long
    ): String {
        val attributes = LinkedHashMap(asset.attributes)
        attributes["offset"] = offsetFromEnd.toString()
        attributes["length"] = length.coerceAtLeast(0L).toString()
        val renderedAttributes = attributes.entries.joinToString(" ") { (key, value) ->
            "$key=\"${escapeXmlAttribute(value)}\""
        }
        return "<${asset.tagName} $renderedAttributes />"
    }

    private fun escapeXmlAttribute(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "&apos;")
    }

    private fun resolveAndroidVersionForMetadata(): String {
        val release = Build.VERSION.RELEASE.orEmpty().substringBefore('.').trim()
        return release.ifBlank { Build.VERSION.SDK_INT.toString() }
    }

    private fun normalizeMetadataLabel(value: String?, fallback: String): String {
        val normalized = value.orEmpty().trim()
        if (normalized.isBlank()) return fallback
        return normalized.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }

    private fun injectXiaomiMotionPhotoMp4Metadata(
        videoFile: File,
        outputFile: File,
        androidVersion: String,
        manufacturer: String,
        model: String,
        marketName: String,
        preservedMetaBox: ByteArray? = null
    ): File {
        val xiaomiMetaBox = preservedMetaBox ?: buildXiaomiMetaBox(
            androidVersion = androidVersion,
            manufacturer = manufacturer,
            model = model,
            marketName = marketName.ifBlank { model }
        )
        val rewritten = rewriteMp4WithInjectedMoov(
            inputFile = videoFile,
            outputFile = outputFile
        ) { moovBytes ->
            val moovBox = findTopLevelBox(moovBytes, "moov") ?: return@rewriteMp4WithInjectedMoov null
            val moovChildren = listChildBoxes(moovBytes, moovBox)
            if (moovChildren.isEmpty()) return@rewriteMp4WithInjectedMoov null

            val rebuiltChildren = ArrayList<ByteArray>(moovChildren.size + 1)
            var inserted = false
            moovChildren.forEach { child ->
                if (child.type == "meta") return@forEach
                rebuiltChildren += moovBytes.copyOfRange(child.offset, child.endOffset)
                if (!inserted && child.type == "mvhd") {
                    rebuiltChildren += xiaomiMetaBox
                    inserted = true
                }
            }
            if (!inserted) {
                rebuiltChildren.add(0, xiaomiMetaBox)
            }
            buildMp4AsciiBox("moov", combineByteArrays(rebuiltChildren))
        }
        return if (rewritten) outputFile else videoFile
    }

    private fun injectOplusMotionPhotoMp4Metadata(
        videoFile: File,
        outputFile: File,
        androidVersion: String,
        preservedFtypBox: ByteArray?,
        preservedWideBox: ByteArray?,
        preservedMetaTrackBoxes: List<ByteArray>,
        preservedMetaBox: ByteArray?,
        preservedLpexBox: ByteArray?,
        preservedFreeBox: ByteArray?
    ): File {
        val metaBox = preservedMetaBox ?: buildOplusMetaBox(androidVersion)
        val sourceBytes = videoFile.readBytes()
        val mdatBox = findTopLevelBox(sourceBytes, "mdat") ?: return videoFile
        val moovBox = findTopLevelBox(sourceBytes, "moov") ?: return videoFile
        val moovBytes = sourceBytes.copyOfRange(moovBox.offset, moovBox.endOffset)
        val rebuiltMoov = rewriteOplusMotionPhotoMoov(
            moovBytes = moovBytes,
            metaBox = metaBox,
            preservedMetaTrackBoxes = preservedMetaTrackBoxes,
            preservedLpexBox = preservedLpexBox
        ) ?: return videoFile

        val targetFtypBox = preservedFtypBox?.takeIf { it.isNotEmpty() } ?: buildOplusQuickTimeFtypBox()
        val useQuickTimeLayout = isQuickTimeFtypBox(targetFtypBox)
        val targetWideBox = if (useQuickTimeLayout) {
            preservedWideBox?.takeIf { it.isNotEmpty() } ?: buildOplusWideBox()
        } else {
            preservedWideBox?.takeIf { it.isNotEmpty() }
        }
        val oldMdatPayloadOffset = mdatBox.payloadOffset
        val newPrefixSize = targetFtypBox.size + (targetWideBox?.size ?: 0)
        val preservedFreePayload = preservedFreeBox?.takeIf { it.isNotEmpty() }
        val newMdatPayloadOffset = if (useQuickTimeLayout) {
            newPrefixSize + mdatBox.headerSize
        } else {
            newPrefixSize + rebuiltMoov.size + (preservedFreePayload?.size ?: 0) + mdatBox.headerSize
        }
        val patchedMoov = patchMoovChunkOffsets(
            moovBytes = rebuiltMoov,
            delta = (newMdatPayloadOffset - oldMdatPayloadOffset).toLong()
        ) ?: return videoFile
        val mdatBytes = sourceBytes.copyOfRange(mdatBox.offset, mdatBox.endOffset)

        FileOutputStream(outputFile).use { output ->
            output.write(targetFtypBox)
            if (targetWideBox != null && targetWideBox.isNotEmpty()) {
                output.write(targetWideBox)
            }
            if (useQuickTimeLayout) {
                output.write(mdatBytes)
                output.write(patchedMoov)
            } else {
                output.write(patchedMoov)
                preservedFreePayload?.let(output::write)
                output.write(mdatBytes)
            }
        }
        return outputFile
    }

    private fun rewriteOplusMotionPhotoMoov(
        moovBytes: ByteArray,
        metaBox: ByteArray,
        preservedMetaTrackBoxes: List<ByteArray>,
        preservedLpexBox: ByteArray?
    ): ByteArray? {
        val moovBox = findTopLevelBox(moovBytes, "moov") ?: return null
        val moovChildren = listChildBoxes(moovBytes, moovBox)
        if (moovChildren.isEmpty()) return null

        val existingTrackIds = moovChildren
            .filter { it.type == "trak" }
            .mapNotNull { extractTrackId(moovBytes, it) }
        val videoTrackId = moovChildren
            .firstOrNull { it.type == "trak" && extractHandlerType(moovBytes, it) == "vide" }
            ?.let { extractTrackId(moovBytes, it) }
            ?: existingTrackIds.firstOrNull()
            ?: 1
        var nextTrackId = ((existingTrackIds.maxOrNull() ?: 0) + 1).coerceAtLeast(1)
        val patchedMetaTracks = preservedMetaTrackBoxes.map { trackBytes ->
            patchPreservedMetaTrack(
                trackBytes = trackBytes,
                newTrackId = nextTrackId++,
                referencedVideoTrackId = videoTrackId
            )
        }

        val rebuiltChildren = ArrayList<ByteArray>(
            moovChildren.size + patchedMetaTracks.size + 2
        )
        var insertedMetaBox = false
        moovChildren.forEach { child ->
            when {
                child.type == "mvhd" -> {
                    val nextTrackIdValue = maxOf(
                        nextTrackId,
                        readMvhdNextTrackId(moovBytes, child)
                    )
                    rebuiltChildren += patchMvhdNextTrackId(
                        bytes = moovBytes,
                        mvhdBox = child,
                        nextTrackId = nextTrackIdValue
                    )
                    rebuiltChildren += metaBox
                    insertedMetaBox = true
                }
                child.type == "meta" || child.type == "lpex" -> Unit
                child.type == "trak" && extractHandlerType(moovBytes, child) == "meta" -> Unit
                else -> rebuiltChildren += moovBytes.copyOfRange(child.offset, child.endOffset)
            }
        }
        patchedMetaTracks.forEach { rebuiltChildren += it }
        if (!insertedMetaBox) {
            rebuiltChildren.add(0, metaBox)
        }
        preservedLpexBox?.let { rebuiltChildren += it }
        return buildMp4AsciiBox("moov", combineByteArrays(rebuiltChildren))
    }

    private fun rewriteMp4WithInjectedMoov(
        inputFile: File,
        outputFile: File,
        rewriteMoov: (ByteArray) -> ByteArray?
    ): Boolean {
        if (!inputFile.exists()) return false
        val moovBox = findTopLevelBox(inputFile, "moov") ?: return false
        val mdatBox = findTopLevelBox(inputFile, "mdat")
        val moovBytes = ByteArray(moovBox.size.toInt())
        RandomAccessFile(inputFile, "r").use { raf ->
            raf.seek(moovBox.offset)
            raf.readFully(moovBytes)
        }
        val rebuiltMoov = rewriteMoov(moovBytes)?.let { candidate ->
            val sizeDelta = candidate.size - moovBytes.size
            if (
                sizeDelta != 0 &&
                mdatBox != null &&
                moovBox.offset < mdatBox.offset
            ) {
                patchMoovChunkOffsets(candidate, sizeDelta.toLong()) ?: return false
            } else {
                candidate
            }
        } ?: return false
        FileInputStream(inputFile).channel.use { inputChannel ->
            FileOutputStream(outputFile).channel.use { outputChannel ->
                transferChannelRange(
                    inputChannel = inputChannel,
                    outputChannel = outputChannel,
                    startPosition = 0L,
                    byteCount = moovBox.offset
                )
                outputChannel.write(ByteBuffer.wrap(rebuiltMoov))
                transferChannelRange(
                    inputChannel = inputChannel,
                    outputChannel = outputChannel,
                    startPosition = moovBox.endOffset,
                    byteCount = (inputFile.length() - moovBox.endOffset).coerceAtLeast(0L)
                )
            }
        }
        return true
    }

    private fun patchMoovChunkOffsets(
        moovBytes: ByteArray,
        delta: Long
    ): ByteArray? {
        if (delta == 0L) return moovBytes
        val patched = moovBytes.copyOf()
        val moovBox = findTopLevelBox(patched, "moov") ?: return null
        return if (patchChunkOffsetsInBox(patched, moovBox, delta)) {
            patched
        } else {
            null
        }
    }

    private fun patchChunkOffsetsInBox(
        bytes: ByteArray,
        box: Mp4BoxInfo,
        delta: Long
    ): Boolean {
        listChildBoxes(bytes, box).forEach { child ->
            when (child.type) {
                "stco" -> if (!patchStcoOffsets(bytes, child, delta)) return false
                "co64" -> if (!patchCo64Offsets(bytes, child, delta)) return false
                in mp4ContainerTypes -> if (!patchChunkOffsetsInBox(bytes, child, delta)) return false
            }
        }
        return true
    }

    private fun patchStcoOffsets(
        bytes: ByteArray,
        box: Mp4BoxInfo,
        delta: Long
    ): Boolean {
        val entryCountOffset = box.payloadOffset + 4
        if (entryCountOffset + 4 > box.endOffset) return false
        val entryCount = readUnsignedInt(bytes, entryCountOffset).toInt()
        var cursor = entryCountOffset + 4
        repeat(entryCount) {
            if (cursor + 4 > box.endOffset) return false
            val currentOffset = readUnsignedInt(bytes, cursor)
            val patchedOffset = currentOffset + delta
            if (patchedOffset !in 0L..0xffffffffL) return false
            writeUnsignedInt(bytes, cursor, patchedOffset.toInt())
            cursor += 4
        }
        return true
    }

    private fun patchCo64Offsets(
        bytes: ByteArray,
        box: Mp4BoxInfo,
        delta: Long
    ): Boolean {
        val entryCountOffset = box.payloadOffset + 4
        if (entryCountOffset + 4 > box.endOffset) return false
        val entryCount = readUnsignedInt(bytes, entryCountOffset).toInt()
        var cursor = entryCountOffset + 4
        repeat(entryCount) {
            if (cursor + 8 > box.endOffset) return false
            val currentOffset = readUnsignedLong(bytes, cursor)
            val patchedOffset = currentOffset + delta
            if (patchedOffset < 0L) return false
            writeUnsignedLong(bytes, cursor, patchedOffset)
            cursor += 8
        }
        return true
    }

    private fun transferChannelRange(
        inputChannel: java.nio.channels.FileChannel,
        outputChannel: java.nio.channels.FileChannel,
        startPosition: Long,
        byteCount: Long
    ) {
        var copied = 0L
        while (copied < byteCount) {
            val transferred = inputChannel.transferTo(
                startPosition + copied,
                byteCount - copied,
                outputChannel
            )
            if (transferred <= 0L) break
            copied += transferred
        }
    }

    private fun buildXiaomiMetaBox(
        androidVersion: String,
        manufacturer: String,
        model: String,
        marketName: String
    ): ByteArray {
        val keyNames = listOf(
            "com.android.version",
            "com.android.manufacturer",
            "com.android.model",
            "com.xiaomi.product.marketname",
            "com.video.file.type"
        )
        val ilstEntries = listOf(
            buildIlstItemBox(1, buildDataBox(1, androidVersion.toByteArray(StandardCharsets.UTF_8))),
            buildIlstItemBox(2, buildDataBox(1, manufacturer.toByteArray(StandardCharsets.UTF_8))),
            buildIlstItemBox(3, buildDataBox(1, model.toByteArray(StandardCharsets.UTF_8))),
            buildIlstItemBox(4, buildDataBox(1, marketName.toByteArray(StandardCharsets.UTF_8))),
            buildIlstItemBox(5, buildDataBox(67, byteArrayOf(0, 0, 0, 0)))
        )
        val payload = buildByteArray {
            append(buildMdtaHandlerBox())
            append(buildKeysBox(keyNames))
            append(buildMp4AsciiBox("ilst", combineByteArrays(ilstEntries)))
        }
        return buildMp4AsciiBox("meta", payload)
    }

    private fun buildOplusMetaBox(
        androidVersion: String
    ): ByteArray {
        val payload = buildByteArray {
            append(intToByteArray(0))
            append(buildMdtaHandlerBox())
            append(buildKeysBox(listOf("com.android.version")))
            append(
                buildMp4AsciiBox(
                    "ilst",
                    buildIlstItemBox(
                        1,
                        buildDataBox(1, androidVersion.toByteArray(StandardCharsets.UTF_8))
                    )
                )
            )
        }
        return buildMp4AsciiBox("meta", payload)
    }

    internal fun buildOplusQuickTimeFtypBox(): ByteArray {
        val payload = buildByteArray {
            append("qt  ".toByteArray(StandardCharsets.ISO_8859_1))
            append(intToByteArray(0))
            append("qt  ".toByteArray(StandardCharsets.ISO_8859_1))
        }
        return buildMp4AsciiBox("ftyp", payload)
    }

    internal fun buildOplusWideBox(): ByteArray {
        return buildMp4AsciiBox("wide", ByteArray(0))
    }

    private fun buildMdtaHandlerBox(): ByteArray {
        val payload = buildByteArray {
            append(intToByteArray(0))
            append(intToByteArray(0))
            append("mdta".toByteArray(StandardCharsets.ISO_8859_1))
            append(ByteArray(12))
            append(byteArrayOf(0))
        }
        return buildMp4AsciiBox("hdlr", payload)
    }

    private fun buildKeysBox(keys: List<String>): ByteArray {
        val payload = buildByteArray {
            append(intToByteArray(0))
            append(intToByteArray(keys.size))
            keys.forEach { key ->
                val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
                append(intToByteArray(8 + keyBytes.size))
                append("mdta".toByteArray(StandardCharsets.ISO_8859_1))
                append(keyBytes)
            }
        }
        return buildMp4AsciiBox("keys", payload)
    }

    private fun buildIlstItemBox(index: Int, payload: ByteArray): ByteArray {
        return buildMp4RawBox(intToByteArray(index), payload)
    }

    private fun buildDataBox(type: Int, payload: ByteArray): ByteArray {
        val dataPayload = buildByteArray {
            append(intToByteArray(type))
            append(intToByteArray(0))
            append(payload)
        }
        return buildMp4AsciiBox("data", dataPayload)
    }

    private fun buildMp4AsciiBox(type: String, payload: ByteArray): ByteArray {
        require(type.length == 4) { "MP4 box type must be 4 chars" }
        return buildMp4RawBox(type.toByteArray(StandardCharsets.ISO_8859_1), payload)
    }

    private fun buildMp4RawBox(typeBytes: ByteArray, payload: ByteArray): ByteArray {
        val boxSize = 8 + payload.size
        return buildByteArray(boxSize) {
            append(intToByteArray(boxSize))
            append(typeBytes)
            append(payload)
        }
    }

    private fun findTopLevelBox(bytes: ByteArray, type: String): Mp4BoxInfo? {
        return listBoxes(bytes, 0, bytes.size).firstOrNull { it.type == type }
    }

    private fun findTopLevelBox(file: File, type: String): Mp4FileBoxInfo? {
        val fileLength = file.length()
        if (fileLength < 8L) return null
        RandomAccessFile(file, "r").use { raf ->
            var cursor = 0L
            while (cursor + 8L <= fileLength) {
                raf.seek(cursor)
                val size32 = raf.readInt().toLong() and 0xffffffffL
                val typeBytes = ByteArray(4)
                raf.readFully(typeBytes)
                val headerSize: Int
                val size = when {
                    size32 == 0L -> {
                        headerSize = 8
                        fileLength - cursor
                    }
                    size32 == 1L -> {
                        headerSize = 16
                        raf.readLong().toULong().toLong()
                    }
                    else -> {
                        headerSize = 8
                        size32
                    }
                }
                if (size < headerSize || cursor + size > fileLength) break
                val boxType = typeBytes.toString(StandardCharsets.ISO_8859_1)
                if (boxType == type) {
                    return Mp4FileBoxInfo(
                        offset = cursor,
                        size = size,
                        headerSize = headerSize,
                        type = boxType
                    )
                }
                cursor += size
            }
        }
        return null
    }

    private fun listChildBoxes(bytes: ByteArray, parent: Mp4BoxInfo): List<Mp4BoxInfo> {
        if (parent.type != "meta") {
            return listBoxes(bytes, parent.payloadOffset, parent.endOffset)
        }
        return listBoxes(bytes, parent.payloadOffset, parent.endOffset)
            .takeIf { it.isNotEmpty() }
            ?: listBoxes(bytes, (parent.payloadOffset + 4).coerceAtMost(parent.endOffset), parent.endOffset)
    }

    private fun listBoxes(bytes: ByteArray, start: Int, end: Int): List<Mp4BoxInfo> {
        val result = ArrayList<Mp4BoxInfo>()
        var cursor = start
        while (cursor + 8 <= end) {
            val boxSize = readMp4BoxSize(bytes, cursor) ?: break
            if (boxSize < 8L) break
            val nextCursor = cursor + boxSize
            if (nextCursor > end.toLong()) break
            val headerSize = if (readUnsignedInt(bytes, cursor) == 1L) 16 else 8
            val type = bytes.copyOfRange(cursor + 4, cursor + 8)
                .toString(StandardCharsets.ISO_8859_1)
            result += Mp4BoxInfo(
                offset = cursor,
                size = boxSize.toInt(),
                headerSize = headerSize,
                type = type
            )
            cursor = nextCursor.toInt()
        }
        return result
    }

    private fun readMp4BoxSize(bytes: ByteArray, offset: Int): Long? {
        if (offset + 8 > bytes.size) return null
        val size32 = readUnsignedInt(bytes, offset)
        return when {
            size32 == 0L -> (bytes.size - offset).toLong()
            size32 == 1L -> {
                if (offset + 16 > bytes.size) return null
                val high = readUnsignedInt(bytes, offset + 8)
                val low = readUnsignedInt(bytes, offset + 12)
                (high shl 32) or low
            }
            else -> size32
        }
    }

    private fun readUnsignedInt(bytes: ByteArray, offset: Int): Long {
        return ((bytes[offset].toLong() and 0xffL) shl 24) or
            ((bytes[offset + 1].toLong() and 0xffL) shl 16) or
            ((bytes[offset + 2].toLong() and 0xffL) shl 8) or
            (bytes[offset + 3].toLong() and 0xffL)
    }

    private fun readUnsignedLong(bytes: ByteArray, offset: Int): Long {
        var result = 0L
        repeat(8) { index ->
            result = (result shl 8) or (bytes[offset + index].toLong() and 0xffL)
        }
        return result
    }

    private fun writeUnsignedInt(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = ((value ushr 24) and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        bytes[offset + 2] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 3] = (value and 0xFF).toByte()
    }

    private fun writeUnsignedLong(bytes: ByteArray, offset: Int, value: Long) {
        bytes[offset] = ((value ushr 56) and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 48) and 0xFF).toByte()
        bytes[offset + 2] = ((value ushr 40) and 0xFF).toByte()
        bytes[offset + 3] = ((value ushr 32) and 0xFF).toByte()
        bytes[offset + 4] = ((value ushr 24) and 0xFF).toByte()
        bytes[offset + 5] = ((value ushr 16) and 0xFF).toByte()
        bytes[offset + 6] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 7] = (value and 0xFF).toByte()
    }

    private fun extractHandlerType(bytes: ByteArray, trackBox: Mp4BoxInfo): String? {
        val hdlr = findNestedBox(bytes, trackBox, "mdia", "hdlr") ?: return null
        val handlerOffset = hdlr.payloadOffset + 8
        if (handlerOffset + 4 > hdlr.endOffset) return null
        return bytes.copyOfRange(handlerOffset, handlerOffset + 4)
            .toString(StandardCharsets.ISO_8859_1)
    }

    private fun extractTrackId(bytes: ByteArray, trackBox: Mp4BoxInfo): Int? {
        val tkhd = listChildBoxes(bytes, trackBox).firstOrNull { it.type == "tkhd" } ?: return null
        val version = bytes[tkhd.payloadOffset].toInt() and 0xFF
        val trackIdOffset = if (version == 1) tkhd.payloadOffset + 20 else tkhd.payloadOffset + 12
        if (trackIdOffset + 4 > tkhd.endOffset) return null
        return readUnsignedInt(bytes, trackIdOffset).toInt()
    }

    private fun patchPreservedMetaTrack(
        trackBytes: ByteArray,
        newTrackId: Int,
        referencedVideoTrackId: Int
    ): ByteArray {
        val patched = trackBytes.copyOf()
        val trackBox = Mp4BoxInfo(0, patched.size, 8, "trak")
        val tkhd = listChildBoxes(patched, trackBox).firstOrNull { it.type == "tkhd" }
        if (tkhd != null) {
            val version = patched[tkhd.payloadOffset].toInt() and 0xFF
            val trackIdOffset = if (version == 1) tkhd.payloadOffset + 20 else tkhd.payloadOffset + 12
            if (trackIdOffset + 4 <= tkhd.endOffset) {
                writeUnsignedInt(patched, trackIdOffset, newTrackId)
            }
        }
        val tref = listChildBoxes(patched, trackBox).firstOrNull { it.type == "tref" }
        if (tref != null) {
            listChildBoxes(patched, tref)
                .firstOrNull { it.type == "cdsc" }
                ?.let { cdsc ->
                    val entryOffset = cdsc.payloadOffset
                    if (entryOffset + 4 <= cdsc.endOffset) {
                        writeUnsignedInt(patched, entryOffset, referencedVideoTrackId)
                    }
                }
        }
        return patched
    }

    private fun readMvhdNextTrackId(bytes: ByteArray, mvhdBox: Mp4BoxInfo): Int {
        val valueOffset = mvhdBox.endOffset - 4
        if (valueOffset < mvhdBox.payloadOffset || valueOffset + 4 > bytes.size) return 0
        return readUnsignedInt(bytes, valueOffset).toInt()
    }

    private fun patchMvhdNextTrackId(
        bytes: ByteArray,
        mvhdBox: Mp4BoxInfo,
        nextTrackId: Int
    ): ByteArray {
        val patched = bytes.copyOfRange(mvhdBox.offset, mvhdBox.endOffset)
        val valueOffset = patched.size - 4
        if (valueOffset >= mvhdBox.headerSize) {
            writeUnsignedInt(patched, valueOffset, nextTrackId)
        }
        return patched
    }

    private fun findNestedBox(bytes: ByteArray, root: Mp4BoxInfo, vararg path: String): Mp4BoxInfo? {
        var current: Mp4BoxInfo? = root
        path.forEach { type ->
            current = current?.let { parent ->
                listChildBoxes(bytes, parent).firstOrNull { it.type == type }
            }
            if (current == null) return null
        }
        return current
    }

    private fun isQuickTimeFtypBox(boxBytes: ByteArray): Boolean {
        if (boxBytes.size < 16) return false
        if (boxBytes.copyOfRange(4, 8).toString(StandardCharsets.ISO_8859_1) != "ftyp") return false
        return boxBytes.copyOfRange(8, 12).toString(StandardCharsets.ISO_8859_1) == "qt  "
    }

    private val mp4ContainerTypes = setOf(
        "moov",
        "trak",
        "mdia",
        "minf",
        "stbl",
        "edts",
        "dinf",
        "udta",
        "meta",
        "mvex",
        "moof",
        "traf"
    )

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun combineByteArrays(parts: List<ByteArray>): ByteArray {
        val totalSize = parts.sumOf { it.size }
        return buildByteArray(totalSize) {
            parts.forEach { append(it) }
        }
    }

    private fun buildByteArray(
        initialCapacity: Int = 256,
        block: MutableList<Byte>.() -> Unit
    ): ByteArray {
        val buffer = ArrayList<Byte>(initialCapacity)
        buffer.block()
        return ByteArray(buffer.size) { index -> buffer[index] }
    }

    private fun MutableList<Byte>.append(bytes: ByteArray) {
        bytes.forEach { add(it) }
    }

    private fun sanitizeBaseName(baseName: String): String {
        return buildString(baseName.length) {
            baseName.forEach { ch ->
                when {
                    ch.isLetterOrDigit() || ch == '_' || ch == '-' -> append(ch)
                    ch.isWhitespace() -> append('_')
                }
            }
        }.trim('_')
    }

    private fun copyFileToUri(
        resolver: ContentResolver,
        sourceFile: File,
        targetUri: Uri
    ) {
        resolver.openOutputStream(targetUri)?.use { output ->
            sourceFile.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Unable to open output stream for $targetUri")
    }
}
