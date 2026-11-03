package com.voicelike.app.livesynctool

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader

internal object LiveSyncToolMotionSupport {
    data class MotionPhotoInfo(
        val isMotionPhoto: Boolean,
        val videoOffset: Long = 0L,
        val videoLength: Long = 0L
    )

    private const val xmpNamespaceItem = "http://ns.google.com/photos/1.0/container/item/"

    fun checkMotionPhoto(context: Context, uri: Uri): MotionPhotoInfo {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val xmp = exif.getAttribute(ExifInterface.TAG_XMP)
                    ?: exif.getAttributeBytes(ExifInterface.TAG_XMP)?.let { String(it, Charsets.UTF_8) }
                parseXmp(xmp)
            }
        }.getOrNull() ?: MotionPhotoInfo(false)
    }

    fun extractVideo(
        context: Context,
        uri: Uri,
        info: MotionPhotoInfo,
        outputFile: File
    ): File? {
        if (!info.isMotionPhoto || info.videoOffset <= 0L) return null
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fileSize = pfd.statSize
                val startPosition = fileSize - info.videoOffset
                if (startPosition < 0L || startPosition >= fileSize) return null

                FileInputStream(pfd.fileDescriptor).use { input ->
                    input.channel.position(startPosition)
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var totalRead = 0L
                        val limit = if (info.videoLength > 0L) info.videoLength else info.videoOffset
                        while (totalRead < limit) {
                            val bytesToRead = minOf(buffer.size.toLong(), limit - totalRead).toInt()
                            val read = input.read(buffer, 0, bytesToRead)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            totalRead += read
                        }
                    }
                }
            }
            trimExtractedMotionPayloadToMp4(outputFile)
            outputFile.takeIf { it.exists() && it.length() > 0L }
        }.getOrNull()
    }

    private fun parseXmp(xmp: String?): MotionPhotoInfo {
        if (xmp.isNullOrBlank()) return MotionPhotoInfo(false)
        val microVideoOffsetRegex =
            "MicroVideoOffset\\s*=\\s*['\\\"](\\d+)['\\\"]".toRegex(RegexOption.IGNORE_CASE)
        microVideoOffsetRegex.find(xmp)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { offset ->
            if (offset > 0L) return MotionPhotoInfo(true, offset, 0L)
        }

        return runCatching {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmp))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val semantic = getAttributeValueFlexible(parser, "Semantic")
                    if (semantic == "MotionPhoto") {
                        val length = getAttributeValueFlexible(parser, "Length")?.toLongOrNull() ?: 0L
                        val padding = getAttributeValueFlexible(parser, "Padding")?.toLongOrNull() ?: 0L
                        if (length > 0L) {
                            return@runCatching MotionPhotoInfo(true, length + padding, length)
                        }
                    }
                }
                eventType = parser.next()
            }
            MotionPhotoInfo(false)
        }.getOrDefault(MotionPhotoInfo(false))
    }

    private fun getAttributeValueFlexible(
        parser: XmlPullParser,
        localName: String
    ): String? {
        val direct = parser.getAttributeValue(null, localName)
        if (direct != null) return direct
        val namespaced = parser.getAttributeValue(xmpNamespaceItem, localName)
        if (namespaced != null) return namespaced
        for (index in 0 until parser.attributeCount) {
            val name = parser.getAttributeName(index)
            if (name == localName || name.endsWith(":$localName")) {
                return parser.getAttributeValue(index)
            }
        }
        return null
    }

    private fun trimExtractedMotionPayloadToMp4(file: File) {
        runCatching {
            if (!file.exists() || file.length() < 8L) return
            val bytes = file.readBytes()
            var cursor = 0
            var lastValidEnd = 0
            while (cursor + 8 <= bytes.size) {
                val boxSize = readMp4BoxSize(bytes, cursor) ?: break
                if (boxSize < 8L) break
                val nextCursor = cursor + boxSize
                if (nextCursor > bytes.size) break
                lastValidEnd = nextCursor.toInt()
                cursor = nextCursor.toInt()
            }
            if (lastValidEnd in 8 until bytes.size) {
                file.writeBytes(bytes.copyOf(lastValidEnd))
            }
        }
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
