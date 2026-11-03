package com.voicelike.app.livesynctool

import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class XiaomiPrimaryJpegShell(
    val exifTemplate: XiaomiExifTemplate?
)

internal object XiaomiJpegShellSupport {
    private val exifHeader = byteArrayOf(
        'E'.code.toByte(),
        'x'.code.toByte(),
        'i'.code.toByte(),
        'f'.code.toByte(),
        0,
        0
    )
    private val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.ISO_8859_1)
    private const val TAG_IMAGE_WIDTH = 0x0100
    private const val TAG_IMAGE_LENGTH = 0x0101
    private const val TAG_MAKE = 0x010F
    private const val TAG_MODEL = 0x0110
    private const val TAG_ORIENTATION = 0x0112
    private const val TAG_DATE_TIME = 0x0132
    private const val TAG_XML_PACKET = 0x02BC
    private const val TAG_EXIF_IFD = 0x8769
    private const val TAG_DATE_TIME_ORIGINAL = 0x9003
    private const val TAG_DATE_TIME_DIGITIZED = 0x9004
    private const val TAG_OFFSET_TIME_ORIGINAL = 0x9010
    private const val TAG_OFFSET_TIME_DIGITIZED = 0x9011
    private const val TAG_PIXEL_X_DIMENSION = 0xA002
    private const val TAG_PIXEL_Y_DIMENSION = 0xA003
    private const val TAG_THUMBNAIL_OFFSET = 0x0201
    private const val TAG_THUMBNAIL_LENGTH = 0x0202
    private const val TAG_XIAOMI_JSON = 0x9999
    private const val TAG_XIAOMI_MARKET_NAME = 0x9A00

    private const val TYPE_BYTE = 1
    private const val TYPE_ASCII = 2
    private const val TYPE_SHORT = 3
    private const val TYPE_LONG = 4

    private const val DEFAULT_XIAOMI_JSON =
        """{"mirror":false,"sensorType":"rear","Hdr":"auto","OpMode":36869,"smallPicture":false,"AIScene":0,"filterId":66048,"zoomMultiple":1}"""

    fun extractPrimaryJpegShell(fileBytes: ByteArray): XiaomiPrimaryJpegShell? {
        val exifPayload = extractExifPayload(fileBytes) ?: return null
        return XiaomiPrimaryJpegShell(
            exifTemplate = parseExifTemplate(exifPayload)
        )
    }

    fun rewritePrimaryJpeg(
        renderedJpegBytes: ByteArray,
        shell: XiaomiPrimaryJpegShell?,
        xmpPacket: String,
        make: String,
        model: String,
        marketName: String
    ): ByteArray {
        val dimensions = parseJpegDimensions(renderedJpegBytes)
        val width = dimensions?.first ?: 0
        val height = dimensions?.second ?: 0
        val exifSegment = buildExifSegment(
            template = shell?.exifTemplate,
            xmpPacket = xmpPacket,
            width = width,
            height = height,
            make = make,
            model = model,
            marketName = marketName
        )
        val xmpSegment = buildApp1Segment(xmpHeader + xmpPacket.toByteArray(Charsets.UTF_8))
        val jpegCore = stripLeadingMetadataSegments(renderedJpegBytes)
        return ByteArrayOutputStream(
            2 + exifSegment.size + xmpSegment.size + jpegCore.size
        ).apply {
            write(0xFF)
            write(0xD8)
            write(exifSegment)
            write(xmpSegment)
            write(jpegCore)
        }.toByteArray()
    }

    private fun buildExifSegment(
        template: XiaomiExifTemplate?,
        xmpPacket: String,
        width: Int,
        height: Int,
        make: String,
        model: String,
        marketName: String
    ): ByteArray {
        val normalizedTemplate = (template?.deepCopy() ?: buildSyntheticTemplate())
            .withUpdatedFields(
                xmpPacket = xmpPacket,
                width = width,
                height = height,
                make = make,
                model = model,
                marketName = marketName
            )
        return buildApp1Segment(exifHeader + buildExifTiff(normalizedTemplate))
    }

    private fun buildSyntheticTemplate(): XiaomiExifTemplate {
        val dateTimeBytes = asciiData(buildExifDateTimeString())
        val offsetBytes = asciiData("+08:00")
        val xiaomiJsonBytes = asciiData(DEFAULT_XIAOMI_JSON)
        val xiaomiLabelBytes = asciiData("Xiaomi")
        val exifIfd = XiaomiExifIfd(
            entries = mutableListOf(
                XiaomiExifEntry(
                    tag = TAG_DATE_TIME_ORIGINAL,
                    type = TYPE_ASCII,
                    count = dateTimeBytes.size,
                    data = dateTimeBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_DATE_TIME_DIGITIZED,
                    type = TYPE_ASCII,
                    count = dateTimeBytes.size,
                    data = dateTimeBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_OFFSET_TIME_ORIGINAL,
                    type = TYPE_ASCII,
                    count = offsetBytes.size,
                    data = offsetBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_OFFSET_TIME_DIGITIZED,
                    type = TYPE_ASCII,
                    count = offsetBytes.size,
                    data = offsetBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_XIAOMI_JSON,
                    type = TYPE_ASCII,
                    count = xiaomiJsonBytes.size,
                    data = xiaomiJsonBytes.copyOf()
                )
            )
        )
        val ifd0 = XiaomiExifIfd(
            entries = mutableListOf(
                XiaomiExifEntry(
                    tag = TAG_MAKE,
                    type = TYPE_ASCII,
                    count = xiaomiLabelBytes.size,
                    data = xiaomiLabelBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_MODEL,
                    type = TYPE_ASCII,
                    count = xiaomiLabelBytes.size,
                    data = xiaomiLabelBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_ORIENTATION,
                    type = TYPE_SHORT,
                    count = 1,
                    data = shortData(1, XiaomiByteOrder.BigEndian)
                ),
                XiaomiExifEntry(
                    tag = TAG_DATE_TIME,
                    type = TYPE_ASCII,
                    count = dateTimeBytes.size,
                    data = dateTimeBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_XML_PACKET,
                    type = TYPE_BYTE,
                    count = 0,
                    data = ByteArray(0)
                ),
                XiaomiExifEntry(
                    tag = TAG_XIAOMI_JSON,
                    type = TYPE_ASCII,
                    count = xiaomiJsonBytes.size,
                    data = xiaomiJsonBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_XIAOMI_MARKET_NAME,
                    type = TYPE_ASCII,
                    count = xiaomiLabelBytes.size,
                    data = xiaomiLabelBytes.copyOf()
                ),
                XiaomiExifEntry(
                    tag = TAG_EXIF_IFD,
                    type = TYPE_LONG,
                    count = 1,
                    data = ByteArray(0),
                    childIfd = exifIfd
                )
            )
        )
        return XiaomiExifTemplate(
            byteOrder = XiaomiByteOrder.BigEndian,
            ifd0 = ifd0,
            ifd1 = null,
            thumbnailBytes = null
        )
    }

    private fun XiaomiExifTemplate.withUpdatedFields(
        xmpPacket: String,
        width: Int,
        height: Int,
        make: String,
        model: String,
        marketName: String
    ): XiaomiExifTemplate {
        val working = deepCopy()
        val xmlPacketBytes = xmpPacket.toByteArray(Charsets.UTF_8)
        upsertEntry(
            directory = working.ifd0,
            tag = TAG_IMAGE_WIDTH,
            type = TYPE_LONG,
            data = longData(width.coerceAtLeast(0), working.byteOrder)
        )
        upsertEntry(
            directory = working.ifd0,
            tag = TAG_IMAGE_LENGTH,
            type = TYPE_LONG,
            data = longData(height.coerceAtLeast(0), working.byteOrder)
        )
        upsertEntry(
            directory = working.ifd0,
            tag = TAG_MAKE,
            type = TYPE_ASCII,
            data = asciiData(make.ifBlank { "Xiaomi" })
        )
        upsertEntry(
            directory = working.ifd0,
            tag = TAG_MODEL,
            type = TYPE_ASCII,
            data = asciiData(model.ifBlank { "Xiaomi" })
        )
        upsertEntry(
            directory = working.ifd0,
            tag = TAG_ORIENTATION,
            type = TYPE_SHORT,
            data = shortData(1, working.byteOrder)
        )
        upsertEntry(
            directory = working.ifd0,
            tag = TAG_XML_PACKET,
            type = TYPE_BYTE,
            data = xmlPacketBytes
        )
        upsertEntry(
            directory = working.ifd0,
            tag = TAG_XIAOMI_MARKET_NAME,
            type = TYPE_ASCII,
            data = asciiData(marketName.ifBlank { model.ifBlank { "Xiaomi" } })
        )
        if (findEntry(working.ifd0, TAG_XIAOMI_JSON) == null) {
            upsertEntry(
                directory = working.ifd0,
                tag = TAG_XIAOMI_JSON,
                type = TYPE_ASCII,
                data = asciiData(DEFAULT_XIAOMI_JSON)
            )
        }

        val exifIfd = findOrCreateExifIfd(working)
        upsertEntry(
            directory = exifIfd,
            tag = TAG_PIXEL_X_DIMENSION,
            type = TYPE_LONG,
            data = longData(width.coerceAtLeast(0), working.byteOrder)
        )
        upsertEntry(
            directory = exifIfd,
            tag = TAG_PIXEL_Y_DIMENSION,
            type = TYPE_LONG,
            data = longData(height.coerceAtLeast(0), working.byteOrder)
        )
        upsertEntry(
            directory = exifIfd,
            tag = TAG_XIAOMI_MARKET_NAME,
            type = TYPE_ASCII,
            data = asciiData(marketName.ifBlank { model.ifBlank { "Xiaomi" } })
        )
        if (findEntry(exifIfd, TAG_XIAOMI_JSON) == null) {
            upsertEntry(
                directory = exifIfd,
                tag = TAG_XIAOMI_JSON,
                type = TYPE_ASCII,
                data = asciiData(DEFAULT_XIAOMI_JSON)
            )
        }
        return working
    }

    private fun findOrCreateExifIfd(template: XiaomiExifTemplate): XiaomiExifIfd {
        findEntry(template.ifd0, TAG_EXIF_IFD)?.let { existing ->
            existing.childIfd?.let { return it }
            val created = XiaomiExifIfd(mutableListOf())
            existing.childIfd = created
            existing.type = TYPE_LONG
            existing.count = 1
            existing.data = ByteArray(4)
            return created
        }
        val created = XiaomiExifIfd(mutableListOf())
        template.ifd0.entries += XiaomiExifEntry(
            tag = TAG_EXIF_IFD,
            type = TYPE_LONG,
            count = 1,
            data = ByteArray(0),
            childIfd = created
        )
        return created
    }

    private fun upsertEntry(
        directory: XiaomiExifIfd,
        tag: Int,
        type: Int,
        data: ByteArray
    ) {
        val existing = findEntry(directory, tag)
        if (existing != null) {
            existing.type = type
            existing.data = data
            existing.count = if (type == TYPE_ASCII || type == TYPE_BYTE) data.size else {
                val unitSize = typeUnitSize(type)
                if (unitSize <= 0) 1 else (data.size / unitSize).coerceAtLeast(1)
            }
            if (tag != TAG_EXIF_IFD) {
                existing.childIfd = null
            }
            return
        }
        directory.entries += XiaomiExifEntry(
            tag = tag,
            type = type,
            count = if (type == TYPE_ASCII || type == TYPE_BYTE) data.size else {
                val unitSize = typeUnitSize(type)
                if (unitSize <= 0) 1 else (data.size / unitSize).coerceAtLeast(1)
            },
            data = data
        )
    }

    private fun buildExifTiff(template: XiaomiExifTemplate): ByteArray {
        val writer = TiffByteWriter(template.byteOrder)
        writer.writeByteOrderHeader()
        writer.writeShort(42)
        writer.writeInt(8)
        writeIfd(
            writer = writer,
            directory = template.ifd0,
            nextDirectory = template.ifd1,
            thumbnailBytes = template.thumbnailBytes
        )
        return writer.toByteArray()
    }

    private fun writeIfd(
        writer: TiffByteWriter,
        directory: XiaomiExifIfd,
        nextDirectory: XiaomiExifIfd?,
        thumbnailBytes: ByteArray?
    ) {
        val preparedEntries = prepareEntriesForWrite(
            directory = directory,
            thumbnailBytes = thumbnailBytes,
            byteOrder = writer.byteOrder
        )
        writer.writeShort(preparedEntries.size)
        val deferredActions = ArrayList<() -> Unit>()
        preparedEntries.forEach { entry ->
            writer.writeShort(entry.tag)
            writer.writeShort(entry.type)
            writer.writeInt(entry.count)
            when {
                entry.childIfd != null -> {
                    val childIfd = entry.childIfd ?: return@forEach
                    val patchPosition = writer.reserveInt()
                    deferredActions += {
                        writer.patchInt(patchPosition, writer.size)
                        writeIfd(
                            writer = writer,
                            directory = childIfd,
                            nextDirectory = null,
                            thumbnailBytes = null
                        )
                    }
                }
                entry.tag == TAG_THUMBNAIL_OFFSET && thumbnailBytes != null -> {
                    val patchPosition = writer.reserveInt()
                    deferredActions += {
                        writer.patchInt(patchPosition, writer.size)
                        writer.writeBytes(thumbnailBytes)
                    }
                }
                needsOffset(entry) -> {
                    val patchPosition = writer.reserveInt()
                    deferredActions += {
                        writer.patchInt(patchPosition, writer.size)
                        writer.writeBytes(entry.data)
                    }
                }
                else -> {
                    writer.writeInlineValue(entry.data)
                }
            }
        }
        val nextPatchPosition = writer.reserveInt()
        deferredActions.forEach { action -> action() }
        if (nextDirectory != null) {
            writer.patchInt(nextPatchPosition, writer.size)
            writeIfd(
                writer = writer,
                directory = nextDirectory,
                nextDirectory = null,
                thumbnailBytes = thumbnailBytes
            )
        } else {
            writer.patchInt(nextPatchPosition, 0)
        }
    }

    private fun prepareEntriesForWrite(
        directory: XiaomiExifIfd,
        thumbnailBytes: ByteArray?,
        byteOrder: XiaomiByteOrder
    ): List<XiaomiExifEntry> {
        val output = ArrayList<XiaomiExifEntry>(directory.entries.size)
        directory.entries.forEach { entry ->
            if ((entry.tag == TAG_THUMBNAIL_OFFSET || entry.tag == TAG_THUMBNAIL_LENGTH) && thumbnailBytes == null) {
                return@forEach
            }
            if (entry.tag == TAG_THUMBNAIL_LENGTH && thumbnailBytes != null) {
                output += entry.deepCopy().also {
                    it.type = TYPE_LONG
                    it.count = 1
                    it.data = longData(thumbnailBytes.size, byteOrder)
                }
                return@forEach
            }
            output += entry.deepCopy()
        }
        return output
    }

    private fun needsOffset(entry: XiaomiExifEntry): Boolean {
        if (entry.tag == TAG_THUMBNAIL_OFFSET) return true
        return entry.data.size > 4
    }

    private fun parseExifTemplate(exifPayload: ByteArray): XiaomiExifTemplate? {
        if (exifPayload.size < 8) return null
        val byteOrder = when {
            exifPayload[0] == 'M'.code.toByte() && exifPayload[1] == 'M'.code.toByte() ->
                XiaomiByteOrder.BigEndian
            exifPayload[0] == 'I'.code.toByte() && exifPayload[1] == 'I'.code.toByte() ->
                XiaomiByteOrder.LittleEndian
            else -> return null
        }
        val reader = TiffByteReader(exifPayload, byteOrder)
        val ifd0Offset = reader.readInt(4)
        if (ifd0Offset <= 0 || ifd0Offset >= exifPayload.size) return null
        val visited = HashSet<Int>()
        val (ifd0, nextIfdOffset) = parseIfd(reader, ifd0Offset, visited) ?: return null
        val ifd1 = if (nextIfdOffset > 0 && nextIfdOffset < exifPayload.size) {
            parseIfd(reader, nextIfdOffset, visited)?.first
        } else {
            null
        }
        val thumbnailBytes = ifd1?.let { parseThumbnailBytes(reader, it) }
        return XiaomiExifTemplate(
            byteOrder = byteOrder,
            ifd0 = ifd0,
            ifd1 = ifd1,
            thumbnailBytes = thumbnailBytes
        )
    }

    private fun parseIfd(
        reader: TiffByteReader,
        offset: Int,
        visited: MutableSet<Int>
    ): Pair<XiaomiExifIfd, Int>? {
        if (!visited.add(offset)) return null
        if (offset + 2 > reader.bytes.size) return null
        val entryCount = reader.readShort(offset)
        val entries = ArrayList<XiaomiExifEntry>(entryCount)
        var cursor = offset + 2
        repeat(entryCount) {
            if (cursor + 12 > reader.bytes.size) return null
            val tag = reader.readShort(cursor)
            val type = reader.readShort(cursor + 2)
            val count = reader.readInt(cursor + 4)
            val valueOffsetPosition = cursor + 8
            val rawData = readEntryData(reader, type, count, valueOffsetPosition)
            val childIfd =
                if (tag == TAG_EXIF_IFD && rawData.size == 4) {
                    val childOffset = reader.readInt(valueOffsetPosition)
                    parseIfd(reader, childOffset, visited)?.first
                } else {
                    null
                }
            entries += XiaomiExifEntry(
                tag = tag,
                type = type,
                count = count,
                data = rawData,
                childIfd = childIfd
            )
            cursor += 12
        }
        val nextIfdOffset = if (cursor + 4 <= reader.bytes.size) {
            reader.readInt(cursor)
        } else {
            0
        }
        return XiaomiExifIfd(entries) to nextIfdOffset
    }

    private fun parseThumbnailBytes(
        reader: TiffByteReader,
        directory: XiaomiExifIfd
    ): ByteArray? {
        val offsetEntry = findEntry(directory, TAG_THUMBNAIL_OFFSET) ?: return null
        val lengthEntry = findEntry(directory, TAG_THUMBNAIL_LENGTH) ?: return null
        if (offsetEntry.data.size < 4 || lengthEntry.data.size < 4) return null
        val offset = reader.readInt(offsetEntry.data, 0)
        val length = reader.readInt(lengthEntry.data, 0)
        if (offset <= 0 || length <= 0 || offset + length > reader.bytes.size) return null
        return reader.bytes.copyOfRange(offset, offset + length)
    }

    private fun readEntryData(
        reader: TiffByteReader,
        type: Int,
        count: Int,
        valueOffsetPosition: Int
    ): ByteArray {
        val totalSize = (typeUnitSize(type) * count).coerceAtLeast(0)
        if (totalSize <= 4) {
            return reader.bytes.copyOfRange(valueOffsetPosition, valueOffsetPosition + 4)
                .copyOf(totalSize.coerceAtLeast(0))
        }
        val dataOffset = reader.readInt(valueOffsetPosition)
        if (dataOffset < 0 || dataOffset + totalSize > reader.bytes.size) {
            return ByteArray(0)
        }
        return reader.bytes.copyOfRange(dataOffset, dataOffset + totalSize)
    }

    private fun extractExifPayload(bytes: ByteArray): ByteArray? {
        if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return null
        var cursor = 2
        while (cursor + 4 <= bytes.size) {
            if (bytes[cursor] != 0xFF.toByte()) {
                cursor += 1
                continue
            }
            val markerStart = cursor
            while (cursor < bytes.size && bytes[cursor] == 0xFF.toByte()) {
                cursor += 1
            }
            if (cursor >= bytes.size) break
            val marker = bytes[cursor].toInt() and 0xFF
            cursor += 1
            if (marker == 0xDA || marker == 0xD9) break
            if (cursor + 2 > bytes.size) break
            val length = ((bytes[cursor].toInt() and 0xFF) shl 8) or
                (bytes[cursor + 1].toInt() and 0xFF)
            if (length < 2 || markerStart + 2 + length > bytes.size) break
            if (marker == 0xE1) {
                val payload = bytes.copyOfRange(cursor + 2, markerStart + 2 + length)
                if (payload.size >= exifHeader.size &&
                    payload.copyOfRange(0, exifHeader.size).contentEquals(exifHeader)
                ) {
                    return payload.copyOfRange(exifHeader.size, payload.size)
                }
            }
            cursor = markerStart + 2 + length
        }
        return null
    }

    private fun parseJpegDimensions(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return null
        var cursor = 2
        while (cursor + 4 <= bytes.size) {
            if (bytes[cursor] != 0xFF.toByte()) {
                cursor += 1
                continue
            }
            while (cursor < bytes.size && bytes[cursor] == 0xFF.toByte()) {
                cursor += 1
            }
            if (cursor >= bytes.size) break
            val marker = bytes[cursor].toInt() and 0xFF
            cursor += 1
            if (marker == 0xD9 || marker == 0xDA) break
            if (cursor + 2 > bytes.size) break
            val length = ((bytes[cursor].toInt() and 0xFF) shl 8) or
                (bytes[cursor + 1].toInt() and 0xFF)
            if (length < 2 || cursor + length > bytes.size) break
            if (marker in setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)) {
                if (cursor + 7 > bytes.size) break
                val height = ((bytes[cursor + 3].toInt() and 0xFF) shl 8) or
                    (bytes[cursor + 4].toInt() and 0xFF)
                val width = ((bytes[cursor + 5].toInt() and 0xFF) shl 8) or
                    (bytes[cursor + 6].toInt() and 0xFF)
                return width to height
            }
            cursor += length
        }
        return null
    }

    private fun stripLeadingMetadataSegments(bytes: ByteArray): ByteArray {
        if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return bytes
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
            if (marker == 0xD9 || marker == 0xDA) {
                return bytes.copyOfRange(markerStart, bytes.size)
            }
            if (cursor + 2 > bytes.size) break
            val length = ((bytes[cursor].toInt() and 0xFF) shl 8) or
                (bytes[cursor + 1].toInt() and 0xFF)
            if (length < 2 || markerStart + 2 + length > bytes.size) break
            val isMetadataMarker = marker in 0xE0..0xEF || marker == 0xFE
            if (!isMetadataMarker) {
                return bytes.copyOfRange(markerStart, bytes.size)
            }
            cursor = markerStart + 2 + length
        }
        return bytes.copyOfRange(2, bytes.size)
    }

    private fun buildApp1Segment(payload: ByteArray): ByteArray {
        require(payload.size + 2 < 65536) { "APP1 payload too large" }
        return ByteArrayOutputStream(payload.size + 4).apply {
            write(0xFF)
            write(0xE1)
            val length = payload.size + 2
            write((length ushr 8) and 0xFF)
            write(length and 0xFF)
            write(payload)
        }.toByteArray()
    }

    private fun asciiData(value: String): ByteArray = (value + '\u0000').toByteArray(Charsets.UTF_8)

    private fun shortData(value: Int, byteOrder: XiaomiByteOrder): ByteArray {
        val writer = TiffByteWriter(byteOrder)
        writer.writeShort(value)
        return writer.toByteArray()
    }

    private fun longData(value: Int, byteOrder: XiaomiByteOrder): ByteArray {
        val writer = TiffByteWriter(byteOrder)
        writer.writeInt(value)
        return writer.toByteArray()
    }

    private fun buildExifDateTimeString(): String {
        val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        return formatter.format(Date())
    }

    private fun findEntry(directory: XiaomiExifIfd, tag: Int): XiaomiExifEntry? {
        return directory.entries.firstOrNull { it.tag == tag }
    }

    private fun typeUnitSize(type: Int): Int {
        return when (type) {
            TYPE_BYTE, TYPE_ASCII -> 1
            TYPE_SHORT -> 2
            TYPE_LONG -> 4
            5, 10 -> 8
            7, 9 -> 4
            else -> 1
        }
    }
}

internal enum class XiaomiByteOrder {
    BigEndian,
    LittleEndian
}

internal data class XiaomiExifTemplate(
    val byteOrder: XiaomiByteOrder,
    val ifd0: XiaomiExifIfd,
    val ifd1: XiaomiExifIfd?,
    val thumbnailBytes: ByteArray?
) {
    fun deepCopy(): XiaomiExifTemplate {
        return XiaomiExifTemplate(
            byteOrder = byteOrder,
            ifd0 = ifd0.deepCopy(),
            ifd1 = ifd1?.deepCopy(),
            thumbnailBytes = thumbnailBytes?.copyOf()
        )
    }
}

internal data class XiaomiExifIfd(
    val entries: MutableList<XiaomiExifEntry>
) {
    fun deepCopy(): XiaomiExifIfd {
        return XiaomiExifIfd(entries.mapTo(mutableListOf()) { it.deepCopy() })
    }
}

internal data class XiaomiExifEntry(
    val tag: Int,
    var type: Int,
    var count: Int,
    var data: ByteArray,
    var childIfd: XiaomiExifIfd? = null
) {
    fun deepCopy(): XiaomiExifEntry {
        return XiaomiExifEntry(
            tag = tag,
            type = type,
            count = count,
            data = data.copyOf(),
            childIfd = childIfd?.deepCopy()
        )
    }
}

private class TiffByteReader(
    val bytes: ByteArray,
    private val byteOrder: XiaomiByteOrder
) {
    fun readShort(offset: Int): Int {
        return if (byteOrder == XiaomiByteOrder.BigEndian) {
            ((bytes[offset].toInt() and 0xFF) shl 8) or
                (bytes[offset + 1].toInt() and 0xFF)
        } else {
            (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8)
        }
    }

    fun readInt(offset: Int): Int = readInt(bytes, offset)

    fun readInt(source: ByteArray, offset: Int): Int {
        return if (byteOrder == XiaomiByteOrder.BigEndian) {
            ((source[offset].toInt() and 0xFF) shl 24) or
                ((source[offset + 1].toInt() and 0xFF) shl 16) or
                ((source[offset + 2].toInt() and 0xFF) shl 8) or
                (source[offset + 3].toInt() and 0xFF)
        } else {
            (source[offset].toInt() and 0xFF) or
                ((source[offset + 1].toInt() and 0xFF) shl 8) or
                ((source[offset + 2].toInt() and 0xFF) shl 16) or
                ((source[offset + 3].toInt() and 0xFF) shl 24)
        }
    }
}

private class TiffByteWriter(
    val byteOrder: XiaomiByteOrder
) {
    private val bytes = ArrayList<Byte>(4096)

    val size: Int
        get() = bytes.size

    fun writeByteOrderHeader() {
        if (byteOrder == XiaomiByteOrder.BigEndian) {
            writeByte('M'.code)
            writeByte('M'.code)
        } else {
            writeByte('I'.code)
            writeByte('I'.code)
        }
    }

    fun writeShort(value: Int) {
        if (byteOrder == XiaomiByteOrder.BigEndian) {
            writeByte((value ushr 8) and 0xFF)
            writeByte(value and 0xFF)
        } else {
            writeByte(value and 0xFF)
            writeByte((value ushr 8) and 0xFF)
        }
    }

    fun writeInt(value: Int) {
        if (byteOrder == XiaomiByteOrder.BigEndian) {
            writeByte((value ushr 24) and 0xFF)
            writeByte((value ushr 16) and 0xFF)
            writeByte((value ushr 8) and 0xFF)
            writeByte(value and 0xFF)
        } else {
            writeByte(value and 0xFF)
            writeByte((value ushr 8) and 0xFF)
            writeByte((value ushr 16) and 0xFF)
            writeByte((value ushr 24) and 0xFF)
        }
    }

    fun reserveInt(): Int {
        val position = size
        writeInt(0)
        return position
    }

    fun patchInt(position: Int, value: Int) {
        val raw = ByteArray(4)
        if (byteOrder == XiaomiByteOrder.BigEndian) {
            raw[0] = ((value ushr 24) and 0xFF).toByte()
            raw[1] = ((value ushr 16) and 0xFF).toByte()
            raw[2] = ((value ushr 8) and 0xFF).toByte()
            raw[3] = (value and 0xFF).toByte()
        } else {
            raw[0] = (value and 0xFF).toByte()
            raw[1] = ((value ushr 8) and 0xFF).toByte()
            raw[2] = ((value ushr 16) and 0xFF).toByte()
            raw[3] = ((value ushr 24) and 0xFF).toByte()
        }
        repeat(4) { index ->
            bytes[position + index] = raw[index]
        }
    }

    fun writeInlineValue(value: ByteArray) {
        val inline = ByteArray(4)
        value.copyInto(inline, endIndex = minOf(value.size, 4))
        writeBytes(inline)
    }

    fun writeBytes(value: ByteArray) {
        value.forEach { bytes += it }
    }

    fun writeByte(value: Int) {
        bytes += value.toByte()
    }

    fun toByteArray(): ByteArray = ByteArray(bytes.size) { index -> bytes[index] }
}
