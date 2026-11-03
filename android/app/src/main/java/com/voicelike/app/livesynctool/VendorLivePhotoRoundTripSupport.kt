package com.voicelike.app.livesynctool

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

internal data class VendorLivePhotoRoundTripSpec(
    val vendor: String,
    val sourceShell: LiveSyncToolVendorPairShell,
    val imageTrailerBytes: ByteArray,
    val videoTrailerBytes: ByteArray,
    val liveToken: String? = null,
    val preservedVideoMoovTrackBytes: ByteArray = ByteArray(0),
    val preservedVideoMoovUdtaBytes: ByteArray = ByteArray(0)
)

internal data class VendorLivePhotoPairWriteResult(
    val photoFile: File,
    val videoFile: File,
    val manifestFile: File
)

internal data class VendorLivePhotoSaveResult(
    val photoUri: Uri,
    val videoUri: Uri,
    val manifestFile: File
)

internal object VendorLivePhotoRoundTripSupport {
    private data class Mp4BoxInfo(
        val offset: Int,
        val size: Int,
        val headerSize: Int,
        val type: String
    ) {
        val endOffset: Int get() = offset + size
        val payloadOffset: Int get() = offset + headerSize
    }

    private data class HuaweiHarmonyMp4Shell(
        val metaTrackBoxBytes: ByteArray,
        val udtaBoxBytes: ByteArray
    )

    private val jpegEndMarker = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    private val vivoTrailerPrefix = "vivo{".toByteArray(StandardCharsets.UTF_8)
    private val huaweiMarkerPrefix = "HUAWEI".toByteArray(StandardCharsets.UTF_8)
    private val huaweiMovingPhotoMeta = "MovingPhotoMeta".toByteArray(StandardCharsets.UTF_8)
    private val openHarmonyMovingPhotoKey =
        "com.openharmony.timed_metadata.movingphoto".toByteArray(StandardCharsets.UTF_8)
    private val mebxSampleEntryType = "mebx".toByteArray(StandardCharsets.UTF_8)
    private val uuidBoxType = "uuid".toByteArray(StandardCharsets.UTF_8)
    private val vivoUuidUserType = "vivoMediaExtInfo".toByteArray(StandardCharsets.UTF_8)
    private val livePhotoTokenKey = "\"com.android.camera.livephoto\":\""
    private val cameraAlbumMarker = "cameralbum!".toByteArray(StandardCharsets.UTF_8)
    private val vivoTrailerSentinel = byteArrayOf(
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
        0x1B.toByte(), 0x2A.toByte(), 0x39.toByte(), 0x48.toByte(),
        0x57.toByte(), 0x66.toByte(), 0x75.toByte(), 0x84.toByte(),
        0x93.toByte(), 0xA2.toByte(), 0xB3.toByte()
    )
    private const val vivoExportVersion = 2014
    private const val vivoTokenSectionLength = 0x2F
    private const val vivoLiveTokenLength = 28

    suspend fun prepareRoundTripSpec(
        context: Context,
        sourceItem: LiveSyncToolMediaItem
    ): VendorLivePhotoRoundTripSpec? = withContext(Dispatchers.IO) {
        val sourceVideoUri = sourceItem.livePhotoVideoUri ?: return@withContext null
        val photoBytes = readAllBytes(context.contentResolver, sourceItem.uri) ?: return@withContext null
        val videoBytes = readAllBytes(context.contentResolver, sourceVideoUri) ?: return@withContext null

        val imageTrailerBytes = extractJpegTrailer(photoBytes)
        val videoTrailerBytes = extractMp4Trailer(videoBytes)
        val vivoUuidBoxBytes = extractVivoMp4UuidBox(videoBytes)
        val huaweiMotionMetadata = extractHuaweiHarmonyMotionMetadata(photoBytes, videoBytes)
        val huaweiMp4Shell = extractHuaweiHarmonyMp4Shell(videoBytes)
        if (
            imageTrailerBytes.isEmpty() &&
            videoTrailerBytes.isEmpty() &&
            vivoUuidBoxBytes == null &&
            huaweiMotionMetadata == null &&
            huaweiMp4Shell == null
        ) {
            return@withContext null
        }

        val vendor = when {
            imageTrailerBytes.indexOf(vivoTrailerPrefix) >= 0 ||
                vivoUuidBoxBytes != null -> "vivo"
            huaweiMotionMetadata != null || huaweiMp4Shell != null -> "huawei_harmony"
            else -> "preserved_shell"
        }
        val liveToken = extractLiveToken(imageTrailerBytes)
            ?: vivoUuidBoxBytes?.let(::extractLiveTokenFromVivoUuidBox)
            ?: extractLiveToken(videoTrailerBytes)

        VendorLivePhotoRoundTripSpec(
            vendor = vendor,
            sourceShell = sourceShellForVendor(vendor),
            imageTrailerBytes = imageTrailerBytes,
            videoTrailerBytes = videoTrailerBytes,
            liveToken = liveToken,
            preservedVideoMoovTrackBytes = huaweiMp4Shell?.metaTrackBoxBytes ?: ByteArray(0),
            preservedVideoMoovUdtaBytes = huaweiMp4Shell?.udtaBoxBytes ?: ByteArray(0)
        )
    }

    suspend fun inspectSourceShell(
        context: Context,
        sourceItem: LiveSyncToolMediaItem
    ): LiveSyncToolVendorPairShell = prepareRoundTripSpec(context, sourceItem)?.sourceShell
        ?: LiveSyncToolVendorPairShell.GenericPaired

    fun writeRoundTripPair(
        jpegFile: File,
        videoFile: File,
        outputDir: File,
        baseName: String,
        spec: VendorLivePhotoRoundTripSpec
    ): VendorLivePhotoPairWriteResult {
        require(jpegFile.exists()) { "Missing jpeg file" }
        require(videoFile.exists()) { "Missing video file" }

        outputDir.mkdirs()
        val normalizedBaseName = sanitizeBaseName(baseName).ifBlank {
            "LIVE_${System.currentTimeMillis()}"
        }
        val photoTarget = File(outputDir, "$normalizedBaseName.jpg")
        val videoTarget = File(outputDir, "$normalizedBaseName.mp4")
        val manifestTarget = File(outputDir, "$normalizedBaseName.livephoto.json")
        val pairedTimestamp = System.currentTimeMillis()
        val effectiveLiveToken = if (spec.vendor == "vivo") {
            buildVivoLiveToken(pairedTimestamp)
        } else {
            spec.liveToken
        }
        val imagePayload = if (spec.vendor == "vivo") {
            buildVivoJpegTrailer(effectiveLiveToken ?: buildVivoLiveToken(pairedTimestamp))
        } else {
            spec.imageTrailerBytes
        }
        val videoPayload = if (spec.vendor == "vivo") {
            buildVivoMp4UuidBox(effectiveLiveToken ?: buildVivoLiveToken(pairedTimestamp))
        } else {
            spec.videoTrailerBytes
        }

        FileOutputStream(photoTarget).use { output ->
            jpegFile.inputStream().use { input -> input.copyTo(output) }
            if (imagePayload.isNotEmpty()) {
                output.write(imagePayload)
            }
        }
        if (spec.vendor == "huawei_harmony") {
            val rewrittenBytes = injectHuaweiHarmonyMetadata(
                videoBytes = videoFile.readBytes(),
                spec = spec
            )
            FileOutputStream(videoTarget).use { output ->
                output.write(rewrittenBytes)
            }
        } else {
            FileOutputStream(videoTarget).use { output ->
                videoFile.inputStream().use { input -> input.copyTo(output) }
                if (videoPayload.isNotEmpty()) {
                    output.write(videoPayload)
                }
            }
        }

        photoTarget.setLastModified(pairedTimestamp)
        videoTarget.setLastModified(pairedTimestamp)

        val manifest = JSONObject()
            .put("vendor", spec.vendor)
            .put("format", "vendor_round_trip")
            .put("pairingStrategy", "same_basename")
            .put("baseName", normalizedBaseName)
            .put("photoFileName", photoTarget.name)
            .put("videoFileName", videoTarget.name)
            .put("pairedTimestampMs", pairedTimestamp)
            .put("liveToken", effectiveLiveToken ?: JSONObject.NULL)
            .put("imageTrailerLength", imagePayload.size)
            .put("videoTrailerLength", videoPayload.size)
        manifestTarget.writeText(manifest.toString(2))

        return VendorLivePhotoPairWriteResult(
            photoFile = photoTarget,
            videoFile = videoTarget,
            manifestFile = manifestTarget
        )
    }

    fun savePairToGallery(
        context: Context,
        pair: VendorLivePhotoPairWriteResult,
        relativePath: String = "Pictures/Photoo/LivePhoto"
    ): VendorLivePhotoSaveResult? {
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

        return VendorLivePhotoSaveResult(
            photoUri = photoUri,
            videoUri = videoUri,
            manifestFile = pair.manifestFile
        )
    }

    private fun readAllBytes(
        resolver: ContentResolver,
        uri: Uri
    ): ByteArray? {
        return resolver.openInputStream(uri)?.use { it.readBytes() }
    }

    private fun extractJpegTrailer(bytes: ByteArray): ByteArray {
        val endIndex = bytes.lastIndexOfSequence(jpegEndMarker)
        if (endIndex < 0) return ByteArray(0)
        val trailerStart = endIndex + jpegEndMarker.size
        if (trailerStart >= bytes.size) return ByteArray(0)
        return bytes.copyOfRange(trailerStart, bytes.size)
    }

    private fun extractMp4Trailer(bytes: ByteArray): ByteArray {
        val logicalEnd = computeMp4LogicalEnd(bytes)
        if (logicalEnd <= 0 || logicalEnd >= bytes.size) return ByteArray(0)
        return bytes.copyOfRange(logicalEnd, bytes.size)
    }

    private fun extractVivoMp4UuidBox(bytes: ByteArray): ByteArray? {
        var cursor = 0
        while (cursor + 8 <= bytes.size) {
            val boxSize = readMp4BoxSize(bytes, cursor) ?: break
            if (boxSize < 8L) break
            val nextCursor = cursor + boxSize
            if (nextCursor > bytes.size.toLong()) break
            if (bytes.copyOfRange(cursor + 4, cursor + 8).contentEquals(uuidBoxType)) {
                if (boxSize >= 24L && cursor + 24 <= bytes.size) {
                    val userType = bytes.copyOfRange(cursor + 8, cursor + 24)
                    if (userType.contentEquals(vivoUuidUserType)) {
                        return bytes.copyOfRange(cursor, nextCursor.toInt())
                    }
                }
            }
            cursor = nextCursor.toInt()
        }
        return null
    }

    private fun extractHuaweiHarmonyMotionMetadata(
        photoBytes: ByteArray,
        videoBytes: ByteArray
    ): ByteArray? {
        val imageTrailer = extractJpegTrailer(photoBytes)
        val hasHuaweiImageShell =
            imageTrailer.indexOf(huaweiMarkerPrefix) >= 0 &&
                imageTrailer.indexOf(huaweiMovingPhotoMeta) >= 0
        val hasHarmonyVideoMetadata = videoBytes.indexOf(openHarmonyMovingPhotoKey) >= 0
        if (!hasHuaweiImageShell && !hasHarmonyVideoMetadata) return null
        return imageTrailer.takeIf { it.isNotEmpty() } ?: ByteArray(0)
    }

    private fun extractHuaweiHarmonyMp4Shell(bytes: ByteArray): HuaweiHarmonyMp4Shell? {
        val moovBox = findTopLevelBox(bytes, "moov") ?: return null
        val moovChildren = listChildBoxes(bytes, moovBox)
        val metaTrackBox = moovChildren
            .firstOrNull { it.type == "trak" && isHuaweiHarmonyMetaTrack(bytes, it) }
            ?.let { bytes.copyOfRange(it.offset, it.endOffset) }
            ?: ByteArray(0)
        val udtaBox = moovChildren
            .firstOrNull { it.type == "udta" }
            ?.let { bytes.copyOfRange(it.offset, it.endOffset) }
            ?: ByteArray(0)
        if (metaTrackBox.isEmpty() && udtaBox.isEmpty()) return null
        return HuaweiHarmonyMp4Shell(
            metaTrackBoxBytes = metaTrackBox,
            udtaBoxBytes = udtaBox
        )
    }

    private fun injectHuaweiHarmonyMetadata(
        videoBytes: ByteArray,
        spec: VendorLivePhotoRoundTripSpec
    ): ByteArray {
        if (spec.preservedVideoMoovTrackBytes.isEmpty() && spec.preservedVideoMoovUdtaBytes.isEmpty()) {
            return videoBytes
        }
        val moovBox = findTopLevelBox(videoBytes, "moov") ?: return videoBytes
        val moovChildren = listChildBoxes(videoBytes, moovBox)
        val existingTrackIds = moovChildren
            .filter { it.type == "trak" }
            .mapNotNull { extractTrackId(videoBytes, it) }
        val videoTrackId = moovChildren
            .firstOrNull { it.type == "trak" && extractHandlerType(videoBytes, it) == "vide" }
            ?.let { extractTrackId(videoBytes, it) }
            ?: existingTrackIds.firstOrNull()
            ?: 1
        val metaTrackId = ((existingTrackIds.maxOrNull() ?: 0) + 1).coerceAtLeast(1)
        val patchedMetaTrack = if (spec.preservedVideoMoovTrackBytes.isNotEmpty()) {
            patchHuaweiHarmonyMetaTrack(
                trackBytes = spec.preservedVideoMoovTrackBytes,
                newTrackId = metaTrackId,
                referencedVideoTrackId = videoTrackId
            )
        } else {
            ByteArray(0)
        }

        val rebuiltChildren = ArrayList<ByteArray>(moovChildren.size + 2)
        moovChildren.forEach { child ->
            when {
                child.type == "mvhd" -> {
                    val nextTrackId = maxOf(metaTrackId + 1, readMvhdNextTrackId(videoBytes, child))
                    rebuiltChildren += patchMvhdNextTrackId(videoBytes, child, nextTrackId)
                }
                child.type == "trak" && extractHandlerType(videoBytes, child) == "meta" -> Unit
                child.type == "udta" && spec.preservedVideoMoovUdtaBytes.isNotEmpty() -> Unit
                else -> rebuiltChildren += videoBytes.copyOfRange(child.offset, child.endOffset)
            }
        }
        if (patchedMetaTrack.isNotEmpty()) {
            rebuiltChildren += patchedMetaTrack
        }
        if (spec.preservedVideoMoovUdtaBytes.isNotEmpty()) {
            rebuiltChildren += spec.preservedVideoMoovUdtaBytes
        }

        val newMoovPayload = combineByteArrays(rebuiltChildren)
        val newMoovBox = buildMp4Box("moov", newMoovPayload)
        val prefix = videoBytes.copyOfRange(0, moovBox.offset)
        val suffix = videoBytes.copyOfRange(moovBox.endOffset, videoBytes.size)
        return buildByteArray(prefix.size + newMoovBox.size + suffix.size) {
            append(prefix)
            append(newMoovBox)
            append(suffix)
        }
    }

    private fun computeMp4LogicalEnd(bytes: ByteArray): Int {
        var cursor = 0
        var lastValidEnd = 0
        while (cursor + 8 <= bytes.size) {
            val boxSize = readMp4BoxSize(bytes, cursor) ?: break
            if (boxSize < 8L) break
            val nextCursor = cursor + boxSize
            if (nextCursor > bytes.size.toLong()) break
            lastValidEnd = nextCursor.toInt()
            cursor = nextCursor.toInt()
        }
        return lastValidEnd
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

    private fun findTopLevelBox(bytes: ByteArray, type: String): Mp4BoxInfo? {
        return listBoxes(bytes, 0, bytes.size).firstOrNull { it.type == type }
    }

    private fun listChildBoxes(bytes: ByteArray, parent: Mp4BoxInfo): List<Mp4BoxInfo> {
        val childStart = when (parent.type) {
            "meta" -> parent.payloadOffset + 4
            else -> parent.payloadOffset
        }
        return listBoxes(bytes, childStart, parent.endOffset)
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
            val type = bytes.copyOfRange(cursor + 4, cursor + 8).toString(StandardCharsets.ISO_8859_1)
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

    private fun isHuaweiHarmonyMetaTrack(bytes: ByteArray, trackBox: Mp4BoxInfo): Boolean {
        if (extractHandlerType(bytes, trackBox) != "meta") return false
        val stsd = findNestedBox(bytes, trackBox, "mdia", "minf", "stbl", "stsd") ?: return false
        val stsdBytes = bytes.copyOfRange(stsd.offset, stsd.endOffset)
        return stsdBytes.indexOf(mebxSampleEntryType) >= 0 && stsdBytes.indexOf(openHarmonyMovingPhotoKey) >= 0
    }

    private fun extractHandlerType(bytes: ByteArray, trackBox: Mp4BoxInfo): String? {
        val hdlr = findNestedBox(bytes, trackBox, "mdia", "hdlr") ?: return null
        val handlerOffset = hdlr.payloadOffset + 8
        if (handlerOffset + 4 > hdlr.endOffset) return null
        return bytes.copyOfRange(handlerOffset, handlerOffset + 4).toString(StandardCharsets.ISO_8859_1)
    }

    private fun extractTrackId(bytes: ByteArray, trackBox: Mp4BoxInfo): Int? {
        val tkhd = listChildBoxes(bytes, trackBox).firstOrNull { it.type == "tkhd" } ?: return null
        val version = bytes[tkhd.payloadOffset].toInt() and 0xFF
        val trackIdOffset = if (version == 1) {
            tkhd.payloadOffset + 20
        } else {
            tkhd.payloadOffset + 12
        }
        if (trackIdOffset + 4 > tkhd.endOffset) return null
        return readUnsignedInt(bytes, trackIdOffset).toInt()
    }

    private fun patchHuaweiHarmonyMetaTrack(
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

    private fun patchMvhdNextTrackId(bytes: ByteArray, mvhdBox: Mp4BoxInfo, nextTrackId: Int): ByteArray {
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

    private fun readUnsignedInt(bytes: ByteArray, offset: Int): Long {
        return ((bytes[offset].toLong() and 0xffL) shl 24) or
            ((bytes[offset + 1].toLong() and 0xffL) shl 16) or
            ((bytes[offset + 2].toLong() and 0xffL) shl 8) or
            (bytes[offset + 3].toLong() and 0xffL)
    }

    private fun extractLiveToken(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val text = bytes.toString(StandardCharsets.ISO_8859_1)
        val startIndex = text.indexOf(livePhotoTokenKey)
        if (startIndex < 0) return null
        val valueStart = startIndex + livePhotoTokenKey.length
        val valueEnd = text.indexOf('"', valueStart)
        if (valueEnd <= valueStart) return null
        return text.substring(valueStart, valueEnd)
    }

    private fun extractLiveTokenFromVivoUuidBox(bytes: ByteArray): String? {
        if (bytes.size <= 24) return null
        return extractLiveToken(bytes.copyOfRange(24, bytes.size))
    }

    private fun ByteArray.indexOf(needle: ByteArray): Int {
        if (needle.isEmpty() || size < needle.size) return -1
        for (i in 0..(size - needle.size)) {
            var matched = true
            for (j in needle.indices) {
                if (this[i + j] != needle[j]) {
                    matched = false
                    break
                }
            }
            if (matched) return i
        }
        return -1
    }

    private fun ByteArray.lastIndexOfSequence(needle: ByteArray): Int {
        if (needle.isEmpty() || size < needle.size) return -1
        for (i in (size - needle.size) downTo 0) {
            var matched = true
            for (j in needle.indices) {
                if (this[i + j] != needle[j]) {
                    matched = false
                    break
                }
            }
            if (matched) return i
        }
        return -1
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

    private fun buildVivoJpegTrailer(liveToken: String): ByteArray {
        return buildVivoMetadataPayload(liveToken)
    }

    private fun buildVivoMp4UuidBox(liveToken: String): ByteArray {
        val payload = buildVivoMetadataPayload(liveToken)
        val boxSize = 8 + vivoUuidUserType.size + payload.size
        return buildByteArray(boxSize) {
            append(intToByteArray(boxSize))
            append(uuidBoxType)
            append(vivoUuidUserType)
            append(payload)
        }
    }

    private fun buildVivoMetadataPayload(liveToken: String): ByteArray {
        val normalizedToken = normalizeVivoLiveToken(liveToken)
        val jsonBytes = buildVivoMetadataJson(normalizedToken).toByteArray(StandardCharsets.UTF_8)
        return buildByteArray(
            4 + jsonBytes.size + 4 + cameraAlbumMarker.size + 4 + normalizedToken.length + vivoTrailerSentinel.size
        ) {
            append("vivo".toByteArray(StandardCharsets.UTF_8))
            append(jsonBytes)
            append(intToByteArray(jsonBytes.size))
            append(cameraAlbumMarker)
            append(intToByteArray(vivoTokenSectionLength))
            append(normalizedToken.toByteArray(StandardCharsets.UTF_8))
            append(vivoTrailerSentinel)
        }
    }

    private fun buildVivoMetadataJson(liveToken: String): String {
        return "{\"com.android.camera.livephoto\":\"$liveToken\",\"version\":$vivoExportVersion}"
    }

    private fun buildVivoLiveToken(timestampMs: Long): String {
        return timestampMs.toString().padEnd(vivoLiveTokenLength, '0').take(vivoLiveTokenLength)
    }

    private fun normalizeVivoLiveToken(liveToken: String): String {
        return liveToken.filter { it.isLetterOrDigit() }.padEnd(vivoLiveTokenLength, '0').take(vivoLiveTokenLength)
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            ((value ushr 24) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun writeUnsignedInt(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = ((value ushr 24) and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        bytes[offset + 2] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 3] = (value and 0xFF).toByte()
    }

    private fun combineByteArrays(chunks: List<ByteArray>): ByteArray {
        val totalSize = chunks.sumOf { it.size }
        return buildByteArray(totalSize) {
            chunks.forEach(::append)
        }
    }

    private fun buildMp4Box(type: String, payload: ByteArray): ByteArray {
        require(type.length == 4) { "MP4 box type must be 4 chars" }
        return buildByteArray(8 + payload.size) {
            append(intToByteArray(8 + payload.size))
            append(type.toByteArray(StandardCharsets.ISO_8859_1))
            append(payload)
        }
    }

    private inline fun buildByteArray(capacity: Int, block: ByteArrayOutputBuilder.() -> Unit): ByteArray {
        return ByteArrayOutputBuilder(capacity).apply(block).toByteArray()
    }

    private class ByteArrayOutputBuilder(initialCapacity: Int) {
        private val chunks = ArrayList<ByteArray>()
        private var totalSize = 0

        init {
            if (initialCapacity > 0) {
                chunks.ensureCapacity(8)
            }
        }

        fun append(bytes: ByteArray) {
            if (bytes.isEmpty()) return
            chunks += bytes
            totalSize += bytes.size
        }

        fun toByteArray(): ByteArray {
            val result = ByteArray(totalSize)
            var offset = 0
            for (chunk in chunks) {
                chunk.copyInto(result, destinationOffset = offset)
                offset += chunk.size
            }
            return result
        }
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

    private fun sourceShellForVendor(vendor: String): LiveSyncToolVendorPairShell {
        return when (vendor) {
            "vivo" -> LiveSyncToolVendorPairShell.Vivo
            "huawei_harmony" -> LiveSyncToolVendorPairShell.HuaweiHarmony
            else -> LiveSyncToolVendorPairShell.GenericPaired
        }
    }
}
