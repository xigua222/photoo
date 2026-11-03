package com.voicelike.app.livesynctool

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import java.nio.charset.StandardCharsets

internal data class OplusMotionPhotoRoundTripPayload(
    val presentationTimestampUs: Long?,
    val primaryPresentationTimestampUs: Long?,
    val motionPhotoOwner: String?,
    val motionPhotoEnable: Boolean,
    val hdrgmVersion: String?,
    val gainMapBytes: ByteArray,
    val preservedFtypBox: ByteArray?,
    val preservedWideBox: ByteArray?,
    val preservedMetaTrackBoxes: List<ByteArray>,
    val preservedMetaBox: ByteArray?,
    val preservedLpexBox: ByteArray?
)

internal object OplusMotionPhotoRoundTripSupport {
    private val motionTimestampRegex =
        Regex("""GCamera:MotionPhotoPresentationTimestampUs\s*=\s*['"](\d+)['"]""")
    private val primaryMotionTimestampRegex =
        Regex("""OpCamera:MotionPhotoPrimaryPresentationTimestampUs\s*=\s*['"](\d+)['"]""")
    private val motionPhotoOwnerRegex =
        Regex("""OpCamera:MotionPhotoOwner\s*=\s*['"]([^'"]+)['"]""")
    private val motionPhotoEnableRegex =
        Regex("""OpCamera:MotionPhotoEnable\s*=\s*['"]([^'"]+)['"]""")
    private val hdrgmVersionRegex = Regex("""hdrgm:Version\s*=\s*['"]([^'"]+)['"]""")
    private val containerItemRegex = Regex(
        """Item:Mime="([^"]+)"\s+Item:Semantic="([^"]+)"\s+Item:Length="(\d+)"(?:\s+Item:Padding="(\d+)")?"""
    )

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
        val lpexBox: ByteArray?
    )

    fun extract(
        context: Context,
        sourceItem: LiveSyncToolMediaItem
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

        val itemLengths = parseContainerItems(xmpString)
        val gainMapLength = itemLengths["GainMap"] ?: 0L
        val motionPhotoLength = itemLengths["MotionPhoto"] ?: 0L
        if (motionPhotoLength <= 0L) return null

        val fileSize = fileBytes.size.toLong()
        val primaryLength = fileSize - gainMapLength - motionPhotoLength
        if (primaryLength < 0L) return null

        val gainMapBytes = if (gainMapLength > 0L) {
            fileBytes.copyOfRange(
                primaryLength.toInt(),
                (primaryLength + gainMapLength).toInt()
            )
        } else {
            ByteArray(0)
        }
        val videoBytes = fileBytes.copyOfRange(
            (fileSize - motionPhotoLength).toInt(),
            fileSize.toInt()
        )
        val preservedVideoShell = extractPreservedVideoShell(videoBytes)

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
            motionPhotoOwner = motionPhotoOwnerRegex
                .find(xmpString)
                ?.groupValues
                ?.getOrNull(1),
            motionPhotoEnable = motionPhotoEnableRegex
                .find(xmpString)
                ?.groupValues
                ?.getOrNull(1)
                ?.equals("true", ignoreCase = true) == true,
            hdrgmVersion = hdrgmVersionRegex.find(xmpString)?.groupValues?.getOrNull(1),
            gainMapBytes = gainMapBytes,
            preservedFtypBox = preservedVideoShell.ftypBox,
            preservedWideBox = preservedVideoShell.wideBox,
            preservedMetaTrackBoxes = preservedVideoShell.metaTrackBoxes,
            preservedMetaBox = preservedVideoShell.metaBox,
            preservedLpexBox = preservedVideoShell.lpexBox
        )
    }

    private fun parseContainerItems(xmpString: String): Map<String, Long> {
        val result = LinkedHashMap<String, Long>()
        containerItemRegex.findAll(xmpString).forEach { match ->
            val semantic = match.groupValues[2]
            val length = match.groupValues[3].toLongOrNull() ?: 0L
            val padding = match.groupValues.getOrNull(4)?.toLongOrNull() ?: 0L
            result[semantic] = length + padding
        }
        return result
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
                lpexBox = null
            )
        val moovChildren = listChildBoxes(videoBytes, moovBox)
        val metaTrackBoxes = moovChildren
            .filter { it.type == "trak" && extractHandlerType(videoBytes, it) == "meta" }
            .map { videoBytes.copyOfRange(it.offset, it.endOffset) }
        val metaBox = moovChildren
            .firstOrNull { it.type == "meta" }
            ?.let { videoBytes.copyOfRange(it.offset, it.endOffset) }
        val lpexBox = moovChildren
            .firstOrNull { it.type == "lpex" }
            ?.let { videoBytes.copyOfRange(it.offset, it.endOffset) }
        return OplusVideoShell(
            ftypBox = ftypBox,
            wideBox = wideBox,
            metaTrackBoxes = metaTrackBoxes,
            metaBox = metaBox,
            lpexBox = lpexBox
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
