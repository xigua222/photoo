package com.voicelike.app.motionphoto

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.voicelike.app.MediaItem
import java.nio.charset.StandardCharsets

internal data class OplusMotionPhotoEmbeddedItem(
    val mime: String,
    val semantic: String,
    val bytes: ByteArray,
    val paddingBytes: ByteArray = ByteArray(0)
) {
    val totalLength: Long
        get() = bytes.size.toLong() + paddingBytes.size.toLong()
}

internal data class OplusMotionPhotoRoundTripPayload(
    val presentationTimestampUs: Long?,
    val primaryPresentationTimestampUs: Long?,
    val opCameraVideoLength: Long?,
    val opCameraVideoLengthDelta: Long?,
    val motionPhotoOwner: String?,
    val motionPhotoEnable: Boolean,
    val prefersAudioTrackFirst: Boolean,
    val hdrgmVersion: String?,
    val gainMapBytes: ByteArray,
    val preservedEmbeddedItems: List<OplusMotionPhotoEmbeddedItem>,
    val preservedExifApp1Segment: ByteArray?,
    val primaryWidth: Int?,
    val primaryHeight: Int?,
    val preservedPrimaryAppSegments: List<ByteArray>,
    val preservedFtypBox: ByteArray?,
    val preservedWideBox: ByteArray?,
    val preservedMetaTrackBoxes: List<ByteArray>,
    val preservedMetaBox: ByteArray?,
    val preservedLpexBox: ByteArray?,
    val preservedFreeBox: ByteArray?,
    val preservedTailBytes: ByteArray
)

internal object OplusMotionPhotoRoundTripSupport {
    private const val jpegApp1Marker = 0xE1
    private const val jpegApp2Marker = 0xE2
    private const val jpegStartOfScanMarker = 0xDA
    private const val jpegEndOfImageMarker = 0xD9
    private val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.ISO_8859_1)
    private val exifHeader = "Exif\u0000\u0000".toByteArray(Charsets.ISO_8859_1)
    private val mpfHeader = "MPF\u0000".toByteArray(Charsets.ISO_8859_1)

    private val motionTimestampRegex =
        Regex("""GCamera:MotionPhotoPresentationTimestampUs\s*=\s*['"](\d+)['"]""")
    private val primaryMotionTimestampRegex =
        Regex("""OpCamera:MotionPhotoPrimaryPresentationTimestampUs\s*=\s*['"](\d+)['"]""")
    private val motionPhotoOwnerRegex =
        Regex("""OpCamera:MotionPhotoOwner\s*=\s*['"]([^'"]+)['"]""")
    private val opCameraVideoLengthRegex =
        Regex("""OpCamera:VideoLength\s*=\s*['"](\d+)['"]""")
    private val motionPhotoEnableRegex =
        Regex("""OpCamera:MotionPhotoEnable\s*=\s*['"]([^'"]+)['"]""")
    private val hdrgmVersionRegex = Regex("""hdrgm:Version\s*=\s*['"]([^'"]+)['"]""")
    private val containerItemRegex = Regex(
        """Item:Mime="([^"]+)"\s+Item:Semantic="([^"]+)"\s+Item:Length="(\d+)"(?:\s+Item:Padding="(\d+)")?"""
    )

    private data class OplusContainerItem(
        val mime: String,
        val semantic: String,
        val length: Long,
        val padding: Long
    ) {
        val totalLength: Long get() = length + padding
    }

    private data class Mp4BoxInfo(
        val offset: Int,
        val size: Int,
        val headerSize: Int,
        val type: String
    ) {
        val endOffset: Int get() = offset + size
        val payloadOffset: Int get() = offset + headerSize
    }

    private data class OplusVideoShell(
        val ftypBox: ByteArray?,
        val wideBox: ByteArray?,
        val metaTrackBoxes: List<ByteArray>,
        val metaBox: ByteArray?,
        val lpexBox: ByteArray?,
        val freeBox: ByteArray?,
        val prefersAudioTrackFirst: Boolean
    )

    private data class JpegSegmentInfo(
        val startOffset: Int,
        val marker: Int,
        val totalLength: Int,
        val payloadOffset: Int,
        val payloadLength: Int
    )

    fun extract(
        context: Context,
        sourceItem: MediaItem
    ): OplusMotionPhotoRoundTripPayload? {
        val fileBytes = runCatching {
            context.contentResolver.openInputStream(sourceItem.uri)?.use { input ->
                input.readBytes()
            }
        }.getOrNull() ?: return null

        val xmpString = runCatching {
            ExifInterface(fileBytes.inputStream()).getAttribute(ExifInterface.TAG_XMP)
                ?: ExifInterface(fileBytes.inputStream())
                    .getAttributeBytes(ExifInterface.TAG_XMP)
                    ?.let { String(it, Charsets.UTF_8) }
        }.getOrNull() ?: return null

        val containerItems = parseContainerItems(xmpString)
        val extractedSegments = extractEmbeddedSegments(fileBytes, containerItems) ?: return null
        val primaryJpegBytes = fileBytes.copyOfRange(0, extractedSegments.primaryLength.toInt())
        val opCameraVideoLength = opCameraVideoLengthRegex
            .find(xmpString)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
        val gainMapBytes = extractedSegments.preservedEmbeddedItems
            .firstOrNull { it.semantic == "GainMap" }
            ?.bytes
            ?: ByteArray(0)
        val motionPhotoBytes = extractedSegments.motionPhotoBytes
        val effectiveVideoLength = opCameraVideoLength
            ?.takeIf { it in 0L..motionPhotoBytes.size.toLong() }
        val videoBytes = effectiveVideoLength
            ?.let { motionPhotoBytes.copyOfRange(0, it.toInt()) }
            ?: motionPhotoBytes
        val preservedVideoShell = extractPreservedVideoShell(videoBytes)
        val preservedTailBytes = effectiveVideoLength
            ?.let { motionPhotoBytes.copyOfRange(it.toInt(), motionPhotoBytes.size) }
            ?: ByteArray(0)
        val primaryDimensions = resolvePrimaryJpegDimensions(primaryJpegBytes)

        return OplusMotionPhotoRoundTripPayload(
            presentationTimestampUs = motionTimestampRegex
                .find(xmpString)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull(),
            primaryPresentationTimestampUs = primaryMotionTimestampRegex
                .find(xmpString)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull(),
            opCameraVideoLength = effectiveVideoLength,
            opCameraVideoLengthDelta = effectiveVideoLength
                ?.let { motionPhotoBytes.size.toLong() - it }
                ?.takeIf { it >= 0L },
            motionPhotoOwner = motionPhotoOwnerRegex
                .find(xmpString)
                ?.groupValues
                ?.getOrNull(1),
            motionPhotoEnable = motionPhotoEnableRegex
                .find(xmpString)
                ?.groupValues
                ?.getOrNull(1)
                ?.equals("true", ignoreCase = true) == true,
            prefersAudioTrackFirst = preservedVideoShell.prefersAudioTrackFirst,
            hdrgmVersion = hdrgmVersionRegex.find(xmpString)?.groupValues?.getOrNull(1),
            gainMapBytes = gainMapBytes,
            preservedEmbeddedItems = extractedSegments.preservedEmbeddedItems,
            preservedExifApp1Segment = extractExifApp1Segment(primaryJpegBytes),
            primaryWidth = primaryDimensions?.first,
            primaryHeight = primaryDimensions?.second,
            preservedPrimaryAppSegments = extractPassthroughPrimaryAppSegments(primaryJpegBytes),
            preservedFtypBox = preservedVideoShell.ftypBox,
            preservedWideBox = preservedVideoShell.wideBox,
            preservedMetaTrackBoxes = preservedVideoShell.metaTrackBoxes,
            preservedMetaBox = preservedVideoShell.metaBox,
            preservedLpexBox = preservedVideoShell.lpexBox,
            preservedFreeBox = preservedVideoShell.freeBox,
            preservedTailBytes = preservedTailBytes
        )
    }

    private data class ExtractedEmbeddedSegments(
        val preservedEmbeddedItems: List<OplusMotionPhotoEmbeddedItem>,
        val motionPhotoBytes: ByteArray,
        val primaryLength: Long
    )

    private fun parseContainerItems(xmpString: String): List<OplusContainerItem> {
        return containerItemRegex.findAll(xmpString).map { match ->
            OplusContainerItem(
                mime = match.groupValues[1],
                semantic = match.groupValues[2],
                length = match.groupValues[3].toLongOrNull() ?: 0L,
                padding = match.groupValues.getOrNull(4)?.toLongOrNull() ?: 0L
            )
        }.toList()
    }

    private fun extractEmbeddedSegments(
        fileBytes: ByteArray,
        containerItems: List<OplusContainerItem>
    ): ExtractedEmbeddedSegments? {
        val appendedItems = containerItems.filterNot { item ->
            item.semantic.equals("Primary", ignoreCase = true) && item.totalLength == 0L
        }
        val motionItem = appendedItems.lastOrNull {
            it.semantic.equals("MotionPhoto", ignoreCase = true)
        } ?: return null
        if (motionItem.length <= 0L) return null

        val appendedLength = appendedItems.sumOf { it.totalLength }
        val primaryLength = fileBytes.size.toLong() - appendedLength
        if (primaryLength < 0L) return null

        val preservedEmbeddedItems = ArrayList<OplusMotionPhotoEmbeddedItem>(appendedItems.size)
        var motionPhotoBytes: ByteArray? = null
        var cursor = primaryLength.toInt()
        appendedItems.forEach { item ->
            val nextCursor = cursor + item.totalLength.toInt()
            if (cursor !in 0..fileBytes.size || nextCursor !in 0..fileBytes.size || nextCursor < cursor) {
                return null
            }
            val dataEnd = cursor + item.length.toInt()
            if (dataEnd !in cursor..nextCursor) return null
            val dataBytes = fileBytes.copyOfRange(cursor, dataEnd)
            val paddingBytes = if (item.padding > 0L) {
                fileBytes.copyOfRange(dataEnd, nextCursor)
            } else {
                ByteArray(0)
            }
            if (item.semantic.equals("MotionPhoto", ignoreCase = true)) {
                motionPhotoBytes = concatBytes(dataBytes, paddingBytes)
            } else {
                preservedEmbeddedItems += OplusMotionPhotoEmbeddedItem(
                    mime = item.mime,
                    semantic = item.semantic,
                    bytes = dataBytes,
                    paddingBytes = paddingBytes
                )
            }
            cursor = nextCursor
        }

        return ExtractedEmbeddedSegments(
            preservedEmbeddedItems = preservedEmbeddedItems,
            motionPhotoBytes = motionPhotoBytes ?: return null,
            primaryLength = primaryLength
        )
    }

    private fun concatBytes(first: ByteArray, second: ByteArray): ByteArray {
        if (second.isEmpty()) return first
        return ByteArray(first.size + second.size).also { combined ->
            System.arraycopy(first, 0, combined, 0, first.size)
            System.arraycopy(second, 0, combined, first.size, second.size)
        }
    }

    private fun extractPassthroughPrimaryAppSegments(primaryJpegBytes: ByteArray): List<ByteArray> {
        return listJpegHeaderSegments(primaryJpegBytes)
            .filter { segment -> isAppMarker(segment.marker) }
            .filterNot { segment ->
                isExifApp1Segment(primaryJpegBytes, segment) ||
                    isXmpApp1Segment(primaryJpegBytes, segment) ||
                    isMpfApp2Segment(primaryJpegBytes, segment)
            }
            .map { segment ->
                primaryJpegBytes.copyOfRange(
                    segment.startOffset,
                    segment.startOffset + segment.totalLength
                )
            }
    }

    private fun extractExifApp1Segment(primaryJpegBytes: ByteArray): ByteArray? {
        val exifSegment = listJpegHeaderSegments(primaryJpegBytes)
            .firstOrNull { segment -> isExifApp1Segment(primaryJpegBytes, segment) }
            ?: return null
        return primaryJpegBytes.copyOfRange(
            exifSegment.startOffset,
            exifSegment.startOffset + exifSegment.totalLength
        )
    }

    private fun resolvePrimaryJpegDimensions(primaryJpegBytes: ByteArray): Pair<Int, Int>? {
        val sofSegment = listJpegHeaderSegments(primaryJpegBytes)
            .firstOrNull { segment ->
                isStartOfFrameMarker(segment.marker) && segment.payloadLength >= 5
            }
            ?: return null
        val height = ((primaryJpegBytes[sofSegment.payloadOffset + 1].toInt() and 0xFF) shl 8) or
            (primaryJpegBytes[sofSegment.payloadOffset + 2].toInt() and 0xFF)
        val width = ((primaryJpegBytes[sofSegment.payloadOffset + 3].toInt() and 0xFF) shl 8) or
            (primaryJpegBytes[sofSegment.payloadOffset + 4].toInt() and 0xFF)
        if (width <= 0 || height <= 0) return null
        return width to height
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
            if (marker == jpegStartOfScanMarker || marker == jpegEndOfImageMarker) break
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

    private fun isStartOfFrameMarker(marker: Int): Boolean {
        return marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC
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

    private fun isMpfApp2Segment(bytes: ByteArray, segment: JpegSegmentInfo): Boolean {
        return segment.marker == jpegApp2Marker &&
            segment.payloadLength >= mpfHeader.size &&
            bytes.copyOfRange(
                segment.payloadOffset,
                segment.payloadOffset + mpfHeader.size
            ).contentEquals(mpfHeader)
    }

    private fun extractPreservedVideoShell(videoBytes: ByteArray): OplusVideoShell {
        val ftypBox = findTopLevelBox(videoBytes, "ftyp")
            ?.let { videoBytes.copyOfRange(it.offset, it.endOffset) }
        val wideBox = findTopLevelBox(videoBytes, "wide")
            ?.let { videoBytes.copyOfRange(it.offset, it.endOffset) }
        val moovBox = findTopLevelBox(videoBytes, "moov")
            ?: return OplusVideoShell(
                ftypBox = ftypBox,
                wideBox = wideBox,
                metaTrackBoxes = emptyList(),
                metaBox = null,
                lpexBox = null,
                freeBox = findTopLevelBox(videoBytes, "free")
                    ?.let { videoBytes.copyOfRange(it.offset, it.endOffset) },
                prefersAudioTrackFirst = false
            )
        val moovChildren = listChildBoxes(videoBytes, moovBox)
        val avTrackOrder = moovChildren
            .filter { it.type == "trak" }
            .mapNotNull { child ->
                when (extractHandlerType(videoBytes, child)) {
                    "soun" -> "audio"
                    "vide" -> "video"
                    else -> null
                }
            }
        val metaTrackBoxes = moovChildren
            .filter { it.type == "trak" && extractHandlerType(videoBytes, it) == "meta" }
            .map { videoBytes.copyOfRange(it.offset, it.endOffset) }
        val metaBox = moovChildren
            .firstOrNull { it.type == "meta" }
            ?.let { videoBytes.copyOfRange(it.offset, it.endOffset) }
        val lpexBox = moovChildren
            .firstOrNull { it.type == "lpex" }
            ?.let { videoBytes.copyOfRange(it.offset, it.endOffset) }
        val freeBox = findTopLevelBox(videoBytes, "free")
            ?.let { videoBytes.copyOfRange(it.offset, it.endOffset) }
        return OplusVideoShell(
            ftypBox = ftypBox,
            wideBox = wideBox,
            metaTrackBoxes = metaTrackBoxes,
            metaBox = metaBox,
            lpexBox = lpexBox,
            freeBox = freeBox,
            prefersAudioTrackFirst = avTrackOrder.indexOf("audio").let { audioIndex ->
                val videoIndex = avTrackOrder.indexOf("video")
                audioIndex >= 0 && (videoIndex < 0 || audioIndex < videoIndex)
            }
        )
    }

    private fun findTopLevelBox(bytes: ByteArray, type: String): Mp4BoxInfo? {
        return listBoxes(bytes, 0, bytes.size).firstOrNull { it.type == type }
    }

    private fun listChildBoxes(bytes: ByteArray, parent: Mp4BoxInfo): List<Mp4BoxInfo> {
        val childStart = if (parent.type == "meta") parent.payloadOffset + 4 else parent.payloadOffset
        return listBoxes(bytes, childStart, parent.endOffset)
    }

    private fun extractHandlerType(bytes: ByteArray, trackBox: Mp4BoxInfo): String? {
        val mdia = listChildBoxes(bytes, trackBox).firstOrNull { it.type == "mdia" } ?: return null
        val hdlr = listChildBoxes(bytes, mdia).firstOrNull { it.type == "hdlr" } ?: return null
        val handlerOffset = hdlr.payloadOffset + 8
        if (handlerOffset + 4 > hdlr.endOffset) return null
        return bytes.copyOfRange(handlerOffset, handlerOffset + 4)
            .toString(StandardCharsets.ISO_8859_1)
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
}
