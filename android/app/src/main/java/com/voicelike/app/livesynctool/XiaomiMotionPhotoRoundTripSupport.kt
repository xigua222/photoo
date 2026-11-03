package com.voicelike.app.livesynctool

import android.content.Context

internal data class XiaomiMotionPhotoEmbeddedAsset(
    val tagName: String,
    val bytes: ByteArray,
    val attributes: LinkedHashMap<String, String>
)

internal data class XiaomiMotionPhotoRoundTripPayload(
    val presentationTimestampUs: Long?,
    val timeWatermark: XiaomiMotionPhotoEmbeddedAsset?,
    val subImage: XiaomiMotionPhotoEmbeddedAsset?,
    val preservedMetaBox: ByteArray?,
    val primaryJpegShell: XiaomiPrimaryJpegShell?
)

internal object XiaomiMotionPhotoRoundTripSupport {
    private val microVideoPresentationTimestampRegex =
        Regex("""GCamera:MicroVideoPresentationTimestampUs\s*=\s*['"](\d+)['"]""")
    private val microVideoOffsetRegex =
        Regex("""GCamera:MicroVideoOffset\s*=\s*['"](\d+)['"]""")
    private val miCameraXmpMetaRegex = Regex("""MiCamera:XMPMeta\s*=\s*['"]([^'"]+)['"]""")
    private val xmlAttributeRegex = Regex("""([A-Za-z0-9_:-]+)\s*=\s*"([^"]*)"""")

    fun extract(
        context: Context,
        sourceItem: LiveSyncToolMediaItem
    ): XiaomiMotionPhotoRoundTripPayload? {
        val fileBytes = runCatching {
            context.contentResolver.openInputStream(sourceItem.uri)?.use { input ->
                input.readBytes()
            }
        }.getOrNull() ?: return null

        val xmpString = runCatching {
            androidx.exifinterface.media.ExifInterface(fileBytes.inputStream())
                .getAttribute(androidx.exifinterface.media.ExifInterface.TAG_XMP)
                ?: androidx.exifinterface.media.ExifInterface(fileBytes.inputStream())
                    .getAttributeBytes(androidx.exifinterface.media.ExifInterface.TAG_XMP)
                    ?.let { String(it, Charsets.UTF_8) }
        }.getOrNull() ?: return null

        val payloadMeta = parseEmbeddedMeta(decodeHtml(miCameraXmpMetaRegex.find(xmpString)?.groupValues?.getOrNull(1)))
        val presentationTimestampUs = microVideoPresentationTimestampRegex
            .find(xmpString)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
        val microVideoOffset = microVideoOffsetRegex
            .find(xmpString)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()

        val timeWatermark = payloadMeta["timewatermark"]?.let { asset ->
            resolveAsset(fileBytes, "timewatermark", asset)
        }
        val subImage = payloadMeta["subimage"]?.let { asset ->
            resolveAsset(fileBytes, "subimage", asset)
        }
        val preservedMetaBox = extractMetaBox(fileBytes, microVideoOffset)
        val primaryJpegShell = XiaomiJpegShellSupport.extractPrimaryJpegShell(fileBytes)

        if (
            timeWatermark == null &&
            subImage == null &&
            presentationTimestampUs == null &&
            preservedMetaBox == null &&
            primaryJpegShell == null
        ) {
            return null
        }

        return XiaomiMotionPhotoRoundTripPayload(
            presentationTimestampUs = presentationTimestampUs,
            timeWatermark = timeWatermark,
            subImage = subImage,
            preservedMetaBox = preservedMetaBox,
            primaryJpegShell = primaryJpegShell
        )
    }

    fun extractPrimaryJpegShell(
        context: Context,
        sourceItem: LiveSyncToolMediaItem
    ): XiaomiPrimaryJpegShell? {
        val fileBytes = runCatching {
            context.contentResolver.openInputStream(sourceItem.uri)?.use { input ->
                input.readBytes()
            }
        }.getOrNull() ?: return null
        return XiaomiJpegShellSupport.extractPrimaryJpegShell(fileBytes)
    }

    private fun parseEmbeddedMeta(meta: String?): Map<String, LinkedHashMap<String, String>> {
        if (meta.isNullOrBlank()) return emptyMap()
        val result = LinkedHashMap<String, LinkedHashMap<String, String>>()
        val tagRegex = Regex("""<(timewatermark|subimage)\s+([^>]+)/>""")
        tagRegex.findAll(meta).forEach { match ->
            val tagName = match.groupValues[1]
            val attributeBlock = match.groupValues[2]
            val attributes = LinkedHashMap<String, String>()
            xmlAttributeRegex.findAll(attributeBlock).forEach { attr ->
                attributes[attr.groupValues[1]] = attr.groupValues[2]
            }
            result[tagName] = attributes
        }
        return result
    }

    private fun resolveAsset(
        fileBytes: ByteArray,
        tagName: String,
        attributes: LinkedHashMap<String, String>
    ): XiaomiMotionPhotoEmbeddedAsset? {
        val offsetFromEnd = attributes["offset"]?.toLongOrNull() ?: return null
        val length = attributes["length"]?.toLongOrNull() ?: return null
        if (offsetFromEnd <= 0L || length <= 0L) return null
        val start = fileBytes.size.toLong() - offsetFromEnd
        val end = start + length
        if (start < 0L || end > fileBytes.size.toLong()) return null
        return XiaomiMotionPhotoEmbeddedAsset(
            tagName = tagName,
            bytes = fileBytes.copyOfRange(start.toInt(), end.toInt()),
            attributes = LinkedHashMap(attributes)
        )
    }

    private fun decodeHtml(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
    }

    private fun extractMetaBox(
        fileBytes: ByteArray,
        microVideoOffset: Long?
    ): ByteArray? {
        val offsetFromEnd = microVideoOffset ?: return null
        if (offsetFromEnd <= 8L || offsetFromEnd > fileBytes.size.toLong()) return null
        val mp4Start = fileBytes.size - offsetFromEnd.toInt()
        if (mp4Start !in 0 until fileBytes.size) return null
        val mp4Bytes = fileBytes.copyOfRange(mp4Start, fileBytes.size)
        val moovBox = findTopLevelBox(mp4Bytes, "moov") ?: return null
        return listChildBoxes(mp4Bytes, moovBox)
            .firstOrNull { it.type == "meta" }
            ?.let { metaBox -> mp4Bytes.copyOfRange(metaBox.offset, metaBox.endOffset) }
    }

    private data class Mp4BoxInfo(
        val type: String,
        val offset: Int,
        val endOffset: Int,
        val payloadOffset: Int
    )

    private fun findTopLevelBox(bytes: ByteArray, type: String): Mp4BoxInfo? {
        return listBoxes(bytes, 0, bytes.size).firstOrNull { it.type == type }
    }

    private fun listChildBoxes(bytes: ByteArray, parent: Mp4BoxInfo): List<Mp4BoxInfo> {
        if (parent.type != "meta") {
            return listBoxes(bytes, parent.payloadOffset, parent.endOffset)
        }
        // Xiaomi samples in the repo omit the FullBox version/flags prefix on `meta`,
        // while some synthesized files include it. Support both layouts.
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
            val type = bytes.copyOfRange(cursor + 4, cursor + 8).toString(Charsets.ISO_8859_1)
            result += Mp4BoxInfo(
                type = type,
                offset = cursor,
                endOffset = nextCursor.toInt(),
                payloadOffset = cursor + headerSize
            )
            cursor = nextCursor.toInt()
        }
        return result
    }

    private fun readMp4BoxSize(bytes: ByteArray, offset: Int): Long? {
        if (offset + 8 > bytes.size) return null
        val size = readUnsignedInt(bytes, offset)
        return if (size == 1L) {
            if (offset + 16 > bytes.size) return null
            readUnsignedLong(bytes, offset + 8)
        } else {
            size
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
}
