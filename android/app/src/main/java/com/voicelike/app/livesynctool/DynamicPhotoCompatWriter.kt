package com.voicelike.app.livesynctool

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
    private data class Mp4BoxInfo(
        val offset: Int,
        val size: Int,
        val headerSize: Int,
        val type: String
    ) {
        val endOffset: Int get() = offset + size
        val payloadOffset: Int get() = offset + headerSize
    }

    fun writeGoogleSamsungMotionPhoto(
        jpegFile: File,
        videoFile: File,
        outputFile: File
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
                primaryPadding = 0L
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
        val marketName = Build.MODEL.orEmpty().ifBlank { model }
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
            val srcRect = Rect(0, srcTop, sourceBitmap.width, sourceBitmap.height)
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
            val srcRect = Rect(srcLeft, 0, sourceBitmap.width, sourceBitmap.height)
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

            val gainMapBytes = roundTripPayload?.gainMapBytes ?: ByteArray(0)
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
                preservedLpexBox = roundTripPayload?.preservedLpexBox
            )
            writeOplusMotionPhotoXmp(
                jpegFile = seededJpeg,
                videoLength = oplusVideoFile.length().coerceAtLeast(0L),
                gainMapLength = gainMapBytes.size.toLong(),
                presentationTimestampUs = presentationTimestampUs.coerceAtLeast(1L),
                primaryPresentationTimestampUs = roundTripPayload?.primaryPresentationTimestampUs ?: 0L,
                motionPhotoOwner = roundTripPayload?.motionPhotoOwner?.ifBlank { "oplus" } ?: "oplus",
                motionPhotoEnable = roundTripPayload?.motionPhotoEnable ?: true,
                hdrgmVersion = roundTripPayload?.hdrgmVersion
            )
            val embeddedVideoLength = gainMapBytes.size.toLong() + oplusVideoFile.length()

            try {
                FileOutputStream(outputFile).use { output ->
                    seededJpeg.inputStream().use { input -> input.copyTo(output) }
                    output.write(gainMapBytes)
                    oplusVideoFile.inputStream().use { input -> input.copyTo(output) }
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
        primaryPadding: Long
    ) {
        val exif = ExifInterface(jpegFile.absolutePath)
        exif.setAttribute(
            ExifInterface.TAG_XMP,
            buildGoogleMotionPhotoXmp(
                offsetFromEnd = offsetFromEnd,
                videoLength = videoLength,
                primaryPadding = primaryPadding
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
        motionPhotoOwner: String,
        motionPhotoEnable: Boolean,
        hdrgmVersion: String?
    ) {
        val exif = ExifInterface(jpegFile.absolutePath)
        exif.setAttribute(
            ExifInterface.TAG_XMP,
            buildOplusMotionPhotoXmp(
                videoLength = videoLength,
                gainMapLength = gainMapLength,
                presentationTimestampUs = presentationTimestampUs,
                primaryPresentationTimestampUs = primaryPresentationTimestampUs,
                motionPhotoOwner = motionPhotoOwner,
                motionPhotoEnable = motionPhotoEnable,
                hdrgmVersion = hdrgmVersion
            )
        )
        exif.saveAttributes()
    }

    private fun buildGoogleMotionPhotoXmp(
        offsetFromEnd: Long,
        videoLength: Long,
        primaryPadding: Long
    ): String {
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
                  GCamera:MotionPhotoPresentationTimestampUs="-1"
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
        motionPhotoOwner: String,
        motionPhotoEnable: Boolean,
        hdrgmVersion: String?
    ): String {
        val hdrgmNamespace = if (gainMapLength > 0L || !hdrgmVersion.isNullOrBlank()) {
            """xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/""""
        } else {
            ""
        }
        val hdrgmAttribute = if (gainMapLength > 0L || !hdrgmVersion.isNullOrBlank()) {
            """hdrgm:Version="${escapeXmlAttribute(hdrgmVersion.orEmpty().ifBlank { "1.0" })}""""
        } else {
            ""
        }
        val motionPhotoEnableAttribute = if (motionPhotoEnable) {
            """OpCamera:MotionPhotoEnable="True""""
        } else {
            ""
        }
        val gainMapDirectoryItem = if (gainMapLength > 0L) {
            """
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="image/jpeg"
                          Item:Semantic="GainMap"
                          Item:Length="$gainMapLength"
                          Item:Padding="0"/>
                      </rdf:li>
            """.trimIndent()
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
                  OpCamera:VideoLength="$videoLength"
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
                      $gainMapDirectoryItem
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="video/mp4"
                          Item:Semantic="MotionPhoto"
                          Item:Length="$videoLength"/>
                      </rdf:li>
                    </rdf:Seq>
                  </Container:Directory>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
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
        preservedLpexBox: ByteArray?
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

        val targetFtypBox = preservedFtypBox
            ?.takeIf(::isQuickTimeFtypBox)
            ?: buildOplusQuickTimeFtypBox()
        val targetWideBox = preservedWideBox?.takeIf { it.isNotEmpty() } ?: buildOplusWideBox()
        val oldMdatPayloadOffset = mdatBox.payloadOffset
        val newPrefixSize = targetFtypBox.size + targetWideBox.size
        val newMdatPayloadOffset = newPrefixSize + mdatBox.headerSize
        val patchedMoov = patchMoovChunkOffsets(
            moovBytes = rebuiltMoov,
            delta = (newMdatPayloadOffset - oldMdatPayloadOffset).toLong()
        ) ?: return videoFile
        val mdatBytes = sourceBytes.copyOfRange(mdatBox.offset, mdatBox.endOffset)

        FileOutputStream(outputFile).use { output ->
            output.write(targetFtypBox)
            if (targetWideBox.isNotEmpty()) {
                output.write(targetWideBox)
            }
            output.write(mdatBytes)
            output.write(patchedMoov)
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
                }
                child.type == "meta" || child.type == "lpex" -> Unit
                child.type == "trak" && extractHandlerType(moovBytes, child) == "meta" -> Unit
                else -> rebuiltChildren += moovBytes.copyOfRange(child.offset, child.endOffset)
            }
        }
        patchedMetaTracks.forEach { rebuiltChildren += it }
        rebuiltChildren += metaBox
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
