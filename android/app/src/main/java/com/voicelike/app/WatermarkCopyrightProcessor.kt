package com.voicelike.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.sqrt

private const val COPYRIGHT_TEMP_SUBDIR = "watermark_copyright_temp"
private const val COPYRIGHT_TEMP_MAX_COUNT = 36
private const val COPYRIGHT_TEMP_MAX_AGE_MS = 2L * 24L * 60L * 60L * 1000L
private const val COPYRIGHT_FINGERPRINT_PREF_KEY = "wm_copyright_fingerprint_v1"
private const val COPYRIGHT_FINGERPRINT_MAX_COUNT = 500
private const val COPYRIGHT_FINGERPRINT_MATCH_THRESHOLD = 6
private const val INVISIBLE_MARKER_COMMENT_PREFIX = "Photoo-Marker:"

data class WatermarkCopyrightPreparedOutput(
    val outputFile: File,
    val outputMimeType: String,
    val sourceUriForSave: Uri
)

data class WatermarkCopyrightVerifyResult(
    val metadataDetected: Boolean,
    val noAiDeclarationDetected: Boolean,
    val invisibleWatermarkConfidence: Float,
    val localFingerprintMatched: Boolean,
    val localFingerprintDistance: Int,
    val creatorSummary: String,
    val rightsSummary: String,
    val conclusion: String,
    val confidence: Float
)

suspend fun prepareWatermarkCopyrightOutput(
    context: Context,
    renderedFile: File,
    renderedMimeType: String,
    originalSourceUri: Uri?,
    settings: WatermarkCopyrightSettings
): WatermarkCopyrightPreparedOutput = withContext(Dispatchers.IO) {
    require(renderedFile.exists()) { "导出文件不存在" }
    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    val decoded = BitmapFactory.decodeFile(renderedFile.absolutePath, options)
        ?: throw IllegalStateException("无法读取导出图像")
    val normalizedMime = renderedMimeType.lowercase(Locale.ROOT)
    val forceJpeg = settings.invisibleWatermark
    val outputMime = if (forceJpeg) "image/jpeg" else if (normalizedMime == "image/png") "image/png" else "image/jpeg"
    val extension = if (outputMime == "image/png") "png" else "jpg"

    val outputBitmap = if (settings.invisibleWatermark && outputMime == "image/jpeg") {
        PhotooInvisibleWatermark.embed(decoded)
    } else {
        decoded
    }
    if (outputBitmap !== decoded) {
        decoded.recycle()
    }

    val tempDir = ensureCopyrightTempDir(context.cacheDir)
    val outputFile = File(tempDir, "wm_copyright_${System.currentTimeMillis()}.$extension")
    FileOutputStream(outputFile).use { stream ->
        val format = if (outputMime == "image/png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val quality = if (outputMime == "image/png") 100 else 100
        outputBitmap.compress(format, quality, stream)
    }

    val fingerprint = computeDHash64(outputBitmap)
    outputBitmap.recycle()

    copyPreservedExifMetadataToFile(
        resolver = context.contentResolver,
        sourceUri = originalSourceUri,
        targetFile = outputFile
    )
    applyCopyrightMetadata(outputFile, outputMime, settings)
    if (outputMime == "image/jpeg") {
        val markerId = java.lang.Long.toUnsignedString(fingerprint, 16)
        appendJpegComment(outputFile, "$INVISIBLE_MARKER_COMMENT_PREFIX$markerId")
    }
    registerLocalFingerprint(context, fingerprint)
    trimCopyrightTempFiles(tempDir)

    val sourceUriForSave = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
    )
    WatermarkCopyrightPreparedOutput(
        outputFile = outputFile,
        outputMimeType = outputMime,
        sourceUriForSave = sourceUriForSave
    )
}

suspend fun verifyWatermarkCopyright(
    context: Context,
    uri: Uri
): WatermarkCopyrightVerifyResult = withContext(Dispatchers.IO) {
    val mime = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT).orEmpty()
    val exif = runCatching {
        context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
    }.getOrNull()
    val commentText = if (mime.contains("jpeg") || mime.contains("jpg")) {
        extractJpegComment(context, uri)
    } else {
        ""
    }
    val artist = exif?.getAttribute(ExifInterface.TAG_ARTIST).orEmpty()
    val copyright = exif?.getAttribute(ExifInterface.TAG_COPYRIGHT).orEmpty()
    val imageDesc = exif?.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION).orEmpty()
    val userComment = exif?.getAttribute(ExifInterface.TAG_USER_COMMENT).orEmpty()
    val xmpPacket = readXmpPacket(exif)
    val xmpAuthor = parseXmpFirstValue(xmpPacket, "dc:creator")
    val xmpRights = parseXmpFirstValue(xmpPacket, "dc:rights")
    val metadataDetected = listOf(artist, copyright, imageDesc, userComment, commentText, xmpAuthor, xmpRights).any { it.isNotBlank() }
    val noAiDeclarationDetected = listOf(copyright, imageDesc, userComment, commentText, xmpPacket).any {
        val lower = it.lowercase(Locale.ROOT)
        lower.contains("noai") ||
            lower.contains("ai training") ||
            lower.contains("data mining") ||
            lower.contains("模型训练") ||
            lower.contains("数据挖掘")
    }

    val markerDetected = commentText.contains(INVISIBLE_MARKER_COMMENT_PREFIX, ignoreCase = true)
    val bitmap = decodeBitmapForDetect(context, uri)
    val invisibleSignal = if (bitmap != null) PhotooInvisibleWatermark.detectConfidence(bitmap) else 0f
    val invisibleConfidence = maxOf(
        invisibleSignal,
        if (markerDetected) 0.95f else 0f
    ).coerceIn(0f, 1f)
    val fingerprint = bitmap?.let { computeDHash64(it) }
    bitmap?.recycle()
    val fingerprintDistance = fingerprint?.let { bestFingerprintDistance(context, it) } ?: Int.MAX_VALUE
    val localFingerprintMatched = fingerprintDistance <= COPYRIGHT_FINGERPRINT_MATCH_THRESHOLD

    val confidence = maxOf(
        invisibleConfidence,
        if (localFingerprintMatched) 0.72f else 0f,
        if (metadataDetected) 0.35f else 0f
    ).coerceIn(0f, 1f)
    val conclusion = when {
        invisibleConfidence >= 0.74f -> "高置信度：该图像很可能包含本 App 的隐形创作者标记。"
        localFingerprintMatched -> "中置信度：该图像与本机已登记作品指纹高度相似。"
        metadataDetected -> "低置信度：检测到版权声明元数据，但可能已被第三方篡改或重写。"
        else -> "未检测到明确的本 App 标记。"
    }
    WatermarkCopyrightVerifyResult(
        metadataDetected = metadataDetected,
        noAiDeclarationDetected = noAiDeclarationDetected,
        invisibleWatermarkConfidence = invisibleConfidence,
        localFingerprintMatched = localFingerprintMatched,
        localFingerprintDistance = if (fingerprintDistance == Int.MAX_VALUE) -1 else fingerprintDistance,
        creatorSummary = xmpAuthor.ifBlank { artist }.ifBlank { "未写入作者信息" },
        rightsSummary = xmpRights.ifBlank { copyright }.ifBlank { "未写入版权字段" },
        conclusion = conclusion,
        confidence = confidence
    )
}

private fun applyCopyrightMetadata(
    file: File,
    mimeType: String,
    settings: WatermarkCopyrightSettings
) {
    if (!file.exists()) return
    val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull() ?: return
    val year = settings.year
    val author = settings.authorName.ifBlank { "Photoo User" }
    val authorExifSafe = toExifAsciiSafe(author)
    val emailPart = settings.authorEmail.takeIf { it.isNotBlank() }?.let { " <$it>" }.orEmpty()
    val emailPartExifSafe = toExifAsciiSafe(emailPart)
    val copyrightText = "Copyright $year $author$emailPart. All rights reserved."
    val copyrightTextExifSafe = "Copyright $year $authorExifSafe$emailPartExifSafe. All rights reserved."
    val noAiText = "NoAI: Not for AI training / model fine-tuning / data mining."
    val description = buildString {
        if (settings.writeCopyrightInfo) append(copyrightText)
        if (settings.noAiTrainingDeclaration) {
            if (isNotEmpty()) append(" ")
            append(noAiText)
        }
    }
    val descriptionExifSafe = toExifAsciiSafe(description)
    if (settings.writeCopyrightInfo) {
        exif.setAttribute(ExifInterface.TAG_ARTIST, authorExifSafe)
        exif.setAttribute(ExifInterface.TAG_COPYRIGHT, copyrightTextExifSafe)
    }
    if (descriptionExifSafe.isNotBlank()) {
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, descriptionExifSafe)
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, descriptionExifSafe)
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Photoo")
    }
    resolveXmpTagKey()?.let { xmpTag ->
        val xmpPacket = buildXmpPacket(
            author = author,
            year = year,
            rightsText = copyrightText,
            includeNoAi = settings.noAiTrainingDeclaration
        )
        runCatching { exif.setAttribute(xmpTag, xmpPacket) }
    }
    runCatching { exif.saveAttributes() }

    if (mimeType == "image/jpeg" && description.isNotBlank()) {
        appendJpegComment(file, description)
    }
}

private fun buildXmpPacket(
    author: String,
    year: Int,
    rightsText: String,
    includeNoAi: Boolean
): String {
    val usage = if (includeNoAi) {
        "Not for AI training / model fine-tuning / data mining."
    } else {
        "All rights reserved."
    }
    val safeAuthor = escapeXml(author)
    val safeRights = escapeXml(rightsText)
    val safeUsage = escapeXml(usage)
    return """
        <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Photoo">
          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description rdf:about=""
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:xmpRights="http://ns.adobe.com/xap/1.0/rights/">
              <dc:creator>
                <rdf:Seq><rdf:li>$safeAuthor</rdf:li></rdf:Seq>
              </dc:creator>
              <dc:rights>
                <rdf:Alt><rdf:li xml:lang="x-default">$safeRights</rdf:li></rdf:Alt>
              </dc:rights>
              <xmpRights:Marked>True</xmpRights:Marked>
              <xmpRights:UsageTerms>
                <rdf:Alt><rdf:li xml:lang="x-default">$safeUsage</rdf:li></rdf:Alt>
              </xmpRights:UsageTerms>
            </rdf:Description>
          </rdf:RDF>
        </x:xmpmeta>
    """.trimIndent()
}

private fun resolveXmpTagKey(): String? {
    return runCatching {
        ExifInterface::class.java.getField("TAG_XMP").get(null) as String
    }.getOrNull()
}

private fun toExifAsciiSafe(text: String): String {
    if (text.isBlank()) return ""
    val normalized = text.trim()
    val safe = buildString(normalized.length) {
        normalized.forEach { ch ->
            append(
                when (ch.code) {
                    in 0x20..0x7E -> ch
                    0x0A, 0x0D, 0x09 -> ' '
                    else -> '?'
                }
            )
        }
    }.replace(Regex("\\s+"), " ").trim()
    return safe
}

private fun escapeXml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private fun unescapeXml(text: String): String {
    return text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}

private fun readXmpPacket(exif: ExifInterface?): String {
    if (exif == null) return ""
    val xmpTag = resolveXmpTagKey() ?: return ""
    val asString = exif.getAttribute(xmpTag).orEmpty()
    if (asString.isNotBlank()) return asString
    return runCatching { exif.getAttributeBytes(xmpTag) }
        .getOrNull()
        ?.let { bytes -> runCatching { String(bytes, Charsets.UTF_8) }.getOrDefault("") }
        .orEmpty()
}

private fun parseXmpFirstValue(xmpPacket: String, tagName: String): String {
    if (xmpPacket.isBlank()) return ""
    val blockRegex = Regex("<$tagName\\b[^>]*>([\\s\\S]*?)</$tagName>", RegexOption.IGNORE_CASE)
    val block = blockRegex.find(xmpPacket)?.groupValues?.getOrNull(1).orEmpty()
    if (block.isBlank()) return ""
    val liRegex = Regex("<rdf:li\\b[^>]*>([\\s\\S]*?)</rdf:li>", RegexOption.IGNORE_CASE)
    val value = liRegex.find(block)?.groupValues?.getOrNull(1).orEmpty().trim()
    return unescapeXml(value)
}

private fun ensureCopyrightTempDir(cacheRoot: File): File {
    val dir = File(cacheRoot, COPYRIGHT_TEMP_SUBDIR)
    if (!dir.exists()) {
        runCatching { dir.mkdirs() }
    }
    return dir
}

private fun trimCopyrightTempFiles(tempDir: File) {
    val files = tempDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: return
    val now = System.currentTimeMillis()
    files.forEachIndexed { index, file ->
        val expired = now - file.lastModified() > COPYRIGHT_TEMP_MAX_AGE_MS
        if (expired || index >= COPYRIGHT_TEMP_MAX_COUNT) {
            runCatching { file.delete() }
        }
    }
}

private fun decodeBitmapForDetect(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        val maxDim = maxOf(options.outWidth, options.outHeight).coerceAtLeast(1)
        val sample = when {
            maxDim > 3600 -> 4
            maxDim > 2200 -> 2
            else -> 1
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }.getOrNull()
}

private fun computeDHash64(bitmap: Bitmap): Long {
    val width = 9
    val height = 8
    val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
    var result = 0L
    var bitIndex = 0
    for (y in 0 until height) {
        for (x in 0 until width - 1) {
            val left = scaled.getPixel(x, y)
            val right = scaled.getPixel(x + 1, y)
            val leftGray = (AndroidColor.red(left) * 30 + AndroidColor.green(left) * 59 + AndroidColor.blue(left) * 11) / 100
            val rightGray = (AndroidColor.red(right) * 30 + AndroidColor.green(right) * 59 + AndroidColor.blue(right) * 11) / 100
            if (leftGray > rightGray) {
                result = result or (1L shl bitIndex)
            }
            bitIndex += 1
        }
    }
    scaled.recycle()
    return result
}

private fun registerLocalFingerprint(context: Context, hash: Long) {
    val prefs = context.getSharedPreferences("photoo_prefs", Context.MODE_PRIVATE)
    val existing = decodeFingerprintEntries(prefs.getString(COPYRIGHT_FINGERPRINT_PREF_KEY, null))
    val hashStr = java.lang.Long.toUnsignedString(hash, 16)
    val deduped = existing.filterNot { it.hash == hashStr }.toMutableList()
    deduped.add(0, FingerprintEntry(hash = hashStr, ts = System.currentTimeMillis()))
    while (deduped.size > COPYRIGHT_FINGERPRINT_MAX_COUNT) {
        deduped.removeAt(deduped.lastIndex)
    }
    prefs.edit().putString(COPYRIGHT_FINGERPRINT_PREF_KEY, encodeFingerprintEntries(deduped)).apply()
}

private fun bestFingerprintDistance(context: Context, hash: Long): Int {
    val prefs = context.getSharedPreferences("photoo_prefs", Context.MODE_PRIVATE)
    val existing = decodeFingerprintEntries(prefs.getString(COPYRIGHT_FINGERPRINT_PREF_KEY, null))
    if (existing.isEmpty()) return Int.MAX_VALUE
    var minDistance = Int.MAX_VALUE
    existing.forEach { entry ->
        val candidate = runCatching { java.lang.Long.parseUnsignedLong(entry.hash, 16) }.getOrNull() ?: return@forEach
        val distance = java.lang.Long.bitCount(candidate xor hash)
        if (distance < minDistance) minDistance = distance
    }
    return minDistance
}

private data class FingerprintEntry(
    val hash: String,
    val ts: Long
)

private fun encodeFingerprintEntries(items: List<FingerprintEntry>): String {
    val array = JSONArray()
    items.forEach { entry ->
        val obj = JSONObject()
        obj.put("h", entry.hash)
        obj.put("t", entry.ts)
        array.put(obj)
    }
    return array.toString()
}

private fun decodeFingerprintEntries(raw: String?): List<FingerprintEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    val array = runCatching { JSONArray(raw) }.getOrElse { return emptyList() }
    val list = mutableListOf<FingerprintEntry>()
    for (index in 0 until array.length()) {
        val obj = array.optJSONObject(index) ?: continue
        val hash = obj.optString("h", "").trim()
        if (hash.isBlank()) continue
        list += FingerprintEntry(hash = hash, ts = obj.optLong("t", 0L))
    }
    return list
}

private object PhotooInvisibleWatermark {
    private const val SEED = 0x6f6f746f
    private const val GRID_X = 28
    private const val GRID_Y = 28
    private const val DELTA = 5

    fun embed(source: Bitmap): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        val width = mutable.width
        val height = mutable.height
        if (width < 96 || height < 96) return mutable
        val points = samplingPoints(width, height)
        points.forEachIndexed { index, pair ->
            val sign = if (pseudo(index) and 1 == 0) -1 else 1
            val delta = sign * DELTA
            applyDelta(mutable, pair.first, pair.second, delta)
        }
        return mutable
    }

    fun detectConfidence(source: Bitmap): Float {
        val width = source.width
        val height = source.height
        if (width < 96 || height < 96) return 0f
        val points = samplingPoints(width, height)
        var corr = 0.0
        var energy = 0.0
        points.forEachIndexed { index, pair ->
            val sign = if (pseudo(index) and 1 == 0) -1 else 1
            val pixel = source.getPixel(pair.first, pair.second)
            val luma = (AndroidColor.red(pixel) * 0.299 + AndroidColor.green(pixel) * 0.587 + AndroidColor.blue(pixel) * 0.114) - 127.5
            corr += sign * luma
            energy += luma * luma
        }
        val score = (corr / sqrt((energy * points.size).coerceAtLeast(1.0))).toFloat()
        return ((score - 0.02f) / 0.055f).coerceIn(0f, 1f)
    }

    private fun samplingPoints(width: Int, height: Int): List<Pair<Int, Int>> {
        val marginX = (width * 0.08f).toInt().coerceAtLeast(8)
        val marginY = (height * 0.08f).toInt().coerceAtLeast(8)
        val usableW = (width - marginX * 2).coerceAtLeast(8)
        val usableH = (height - marginY * 2).coerceAtLeast(8)
        val list = ArrayList<Pair<Int, Int>>(GRID_X * GRID_Y)
        for (gy in 0 until GRID_Y) {
            for (gx in 0 until GRID_X) {
                val x = marginX + (usableW * gx) / (GRID_X - 1)
                val y = marginY + (usableH * gy) / (GRID_Y - 1)
                list += x.coerceIn(1, width - 2) to y.coerceIn(1, height - 2)
            }
        }
        return list
    }

    private fun applyDelta(bitmap: Bitmap, cx: Int, cy: Int, delta: Int) {
        for (dy in -1..1) {
            for (dx in -1..1) {
                val x = (cx + dx).coerceIn(0, bitmap.width - 1)
                val y = (cy + dy).coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(x, y)
                val a = AndroidColor.alpha(pixel)
                val r = (AndroidColor.red(pixel) + delta).coerceIn(0, 255)
                val g = (AndroidColor.green(pixel) + delta).coerceIn(0, 255)
                val b = (AndroidColor.blue(pixel) + delta).coerceIn(0, 255)
                bitmap.setPixel(x, y, AndroidColor.argb(a, r, g, b))
            }
        }
    }

    private fun pseudo(index: Int): Int {
        var x = index * 1103515245 + 12345 + SEED
        x = x xor (x ushr 16)
        x *= 0x45d9f3b
        x = x xor (x ushr 16)
        return x
    }
}

private fun extractJpegComment(context: Context, uri: Uri): String {
    val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return ""
    if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return ""
    var index = 2
    val comments = mutableListOf<String>()
    while (index + 3 < bytes.size) {
        if (bytes[index] != 0xFF.toByte()) {
            index += 1
            continue
        }
        val marker = bytes[index + 1].toInt() and 0xFF
        if (marker == 0xDA || marker == 0xD9) break
        if (index + 4 > bytes.size) break
        val length = ((bytes[index + 2].toInt() and 0xFF) shl 8) or (bytes[index + 3].toInt() and 0xFF)
        if (length < 2 || index + 2 + length > bytes.size) break
        if (marker == 0xFE) {
            val start = index + 4
            val end = index + 2 + length
            val text = runCatching { String(bytes.copyOfRange(start, end), Charsets.UTF_8) }.getOrDefault("")
            if (text.isNotBlank()) {
                comments += text
            }
        }
        index += 2 + length
    }
    return comments.joinToString("\n")
}

private fun appendJpegComment(file: File, comment: String) {
    if (!file.exists() || comment.isBlank()) return
    val bytes = runCatching { file.readBytes() }.getOrNull() ?: return
    if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return
    if (runCatching { String(bytes, Charsets.ISO_8859_1).contains(comment) }.getOrDefault(false)) return
    val payload = comment.toByteArray(Charsets.UTF_8)
    if (payload.isEmpty() || payload.size >= 65533) return
    val length = payload.size + 2
    val segment = ByteArrayOutputStream(payload.size + 4).apply {
        write(0xFF)
        write(0xFE)
        write((length ushr 8) and 0xFF)
        write(length and 0xFF)
        write(payload)
    }.toByteArray()
    val output = ByteArrayOutputStream(bytes.size + segment.size)
    output.write(bytes, 0, 2)
    output.write(segment)
    output.write(bytes, 2, bytes.size - 2)
    runCatching { file.writeBytes(output.toByteArray()) }
}
