package com.voicelike.app.motionphoto

import java.nio.charset.StandardCharsets

internal data class SamsungMotionPhotoPayload(
    val footerBytes: ByteArray,
    val primaryPaddingLength: Int,
    val motionPayloadLength: Int
)

internal object SamsungMotionPhotoSupport {
    private const val SAMSUNG_SEFH_VERSION = 107

    private val motionPhotoDataTagId = byteArrayOf(0x00, 0x00, 0x30, 0x0A)
    private val motionPhotoVersionTagId = byteArrayOf(0x00, 0x00, 0x31, 0x0A)
    private val samsungTagOrder = listOf(
        "MotionPhoto_Data",
        "MotionPhoto_Version"
    )
    private val samsungTagIds = mapOf(
        "MotionPhoto_Data" to motionPhotoDataTagId,
        "MotionPhoto_Version" to motionPhotoVersionTagId
    )

    fun buildJpegPayload(
        videoBytes: ByteArray,
        motionPhotoVersion: String = "mpv3"
    ): SamsungMotionPhotoPayload {
        val tags = linkedMapOf(
            "MotionPhoto_Version" to motionPhotoVersion.toByteArray(StandardCharsets.UTF_8),
            "MotionPhoto_Data" to videoBytes
        )
        val tagData = ArrayList<ByteArray>(tags.size)
        val tagOffsets = LinkedHashMap<String, Int>()
        val tagLengths = LinkedHashMap<String, Int>()

        samsungTagOrder.forEach { tag ->
            val value = tags[tag] ?: return@forEach
            val tagId = samsungTagIds.getValue(tag)
            val tagBytes = buildByteArray(8 + tag.length + value.size) {
                append(tagId)
                append(intToLittleEndian(tag.length))
                append(tag.toByteArray(StandardCharsets.UTF_8))
                append(value)
            }
            tagLengths[tag] = tagBytes.size
            for (precedingTag in samsungTagOrder) {
                if (precedingTag !in tags) continue
                tagOffsets[precedingTag] = tagBytes.size + (tagOffsets[precedingTag] ?: 0)
                if (precedingTag == tag) break
            }
            tagData += tagBytes
        }

        val sefh = buildSamsungSefh(tagOffsets, tagLengths)
        val footerBytes = buildByteArray(tagData.sumOf { it.size } + sefh.size) {
            tagData.forEach(::append)
            append(sefh)
        }
        val primaryPaddingLength = samsungTagOrder
            .takeWhile { it != "MotionPhoto_Data" }
            .sumOf { tag ->
                val value = tags[tag] ?: return@sumOf 0
                4 + 4 + tag.length + value.size
            } + (4 + 4 + "MotionPhoto_Data".length)
        val motionPayloadLength = footerBytes.size - primaryPaddingLength

        return SamsungMotionPhotoPayload(
            footerBytes = footerBytes,
            primaryPaddingLength = primaryPaddingLength,
            motionPayloadLength = motionPayloadLength
        )
    }

    private fun buildSamsungSefh(
        tagOffsets: Map<String, Int>,
        tagLengths: Map<String, Int>
    ): ByteArray {
        val entryBytes = buildByteArray {
            samsungTagOrder.forEach { tag ->
                val offset = tagOffsets[tag] ?: return@forEach
                val length = tagLengths[tag] ?: return@forEach
                append(samsungTagIds.getValue(tag))
                append(intToLittleEndian(offset))
                append(intToLittleEndian(length))
            }
        }
        val body = buildByteArray(12 + entryBytes.size) {
            append("SEFH".toByteArray(StandardCharsets.UTF_8))
            append(intToLittleEndian(SAMSUNG_SEFH_VERSION))
            append(intToLittleEndian(tagOffsets.size))
            append(entryBytes)
        }
        return buildByteArray(body.size + 8) {
            append(body)
            append(intToLittleEndian(body.size))
            append("SEFT".toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun intToLittleEndian(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte()
        )
    }

    private fun buildByteArray(capacity: Int = 0, block: ByteArrayBuilder.() -> Unit): ByteArray {
        return ByteArrayBuilder(capacity).apply(block).toByteArray()
    }

    private class ByteArrayBuilder(initialCapacity: Int) {
        private val chunks = ArrayList<ByteArray>().apply {
            if (initialCapacity > 0) ensureCapacity(8)
        }
        private var totalSize = 0

        fun append(bytes: ByteArray) {
            if (bytes.isEmpty()) return
            chunks += bytes
            totalSize += bytes.size
        }

        fun toByteArray(): ByteArray {
            val result = ByteArray(totalSize)
            var offset = 0
            chunks.forEach { chunk ->
                chunk.copyInto(result, destinationOffset = offset)
                offset += chunk.size
            }
            return result
        }
    }
}
