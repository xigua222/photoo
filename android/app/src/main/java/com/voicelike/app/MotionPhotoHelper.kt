package com.voicelike.app

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap

object MotionPhotoHelper {

    private const val XMP_NAMESPACE_CAMERA = "http://ns.google.com/photos/1.0/camera/"
    private const val XMP_NAMESPACE_CONTAINER = "http://ns.google.com/photos/1.0/container/"
    private const val XMP_NAMESPACE_ITEM = "http://ns.google.com/photos/1.0/container/item/"
    private const val MAX_EXTRACTED_CACHE_FILES = 18
    private const val CACHE_PREFIX = "motion_cache_"
    private const val INDEX_FILE_NAME = "motion_photo_index_v2.json"
    private const val MAX_INDEX_FILE_BYTES = 16L * 1024L * 1024L
    private const val MAX_INDEX_ENTRIES = 120_000
    private const val MAX_MOTION_INFO_CACHE_ENTRIES = 2048
    private const val MAX_EXTRACTED_PATH_CACHE_ENTRIES = 96

    private val motionInfoCache = ConcurrentHashMap<String, MotionPhotoInfo>()
    private val extractedVideoPathCache = ConcurrentHashMap<String, String>()
    private val gson = Gson()
    private val indexLock = Any()
    private val preloadMutex = Mutex()
    @Volatile
    private var indexLoaded = false
    private var dirtyCount = 0
    private val persistentIndex = ConcurrentHashMap<Long, PersistEntry>()

    data class MotionPhotoInfo(
        val isMotionPhoto: Boolean,
        val videoOffset: Long = 0, // Offset from end of file (positive value) or absolute position? Usually length from end.
        val videoLength: Long = 0
    )

    private data class PersistEntry(
        val id: Long,
        val dateAdded: Long,
        val size: Long,
        val isMotionPhoto: Boolean,
        val videoOffset: Long,
        val videoLength: Long,
        val updatedAtMs: Long
    )

    fun checkMotionPhoto(context: Context, uri: Uri): MotionPhotoInfo {
        val cacheKey = uri.toString()
        motionInfoCache[cacheKey]?.let { return it }

        var result = MotionPhotoInfo(false)
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                if (exif.hasAttribute(ExifInterface.TAG_XMP)) {
                    val xmpString = exif.getAttribute(ExifInterface.TAG_XMP)
                        ?: exif.getAttributeBytes(ExifInterface.TAG_XMP)?.let { String(it, Charsets.UTF_8) }
                    if (!xmpString.isNullOrBlank()) {
                        result = parseXmp(xmpString)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        motionInfoCache[cacheKey] = result
        pruneMotionInfoCacheIfNeeded()
        return result
    }

    fun trimTransientCaches() {
        motionInfoCache.clear()
        extractedVideoPathCache.clear()
    }

    fun getCachedMotionPhoto(context: Context, item: MediaItem): MotionPhotoInfo? {
        ensureIndexLoaded(context)
        val entry = persistentIndex[item.id] ?: return null
        if (entry.dateAdded != item.dateAdded || entry.size != item.size) return null
        return MotionPhotoInfo(entry.isMotionPhoto, entry.videoOffset, entry.videoLength)
    }

    fun detectAndPersist(context: Context, item: MediaItem): MotionPhotoInfo {
        ensureIndexLoaded(context)
        val cached = getCachedMotionPhoto(context, item)
        if (cached != null) return cached
        val info = checkMotionPhoto(context, item.uri)
        updateIndex(item, info)
        persistIndex(context, force = false)
        return info
    }

    fun mergeCachedMotionStates(context: Context, items: List<MediaItem>): List<MediaItem> {
        if (items.isEmpty()) return items
        ensureIndexLoaded(context)

        var mergedItems: MutableList<MediaItem>? = null
        items.forEachIndexed { index, item ->
            if (item.type != "photo" || item.isLivePhoto || item.isMotionPhoto) return@forEachIndexed
            val entry = persistentIndex[item.id] ?: return@forEachIndexed
            if (entry.dateAdded != item.dateAdded || entry.size != item.size || !entry.isMotionPhoto) {
                return@forEachIndexed
            }

            if (mergedItems == null) {
                val createdItems = items.toMutableList()
                createdItems[index] = item.copy(
                    isMotionPhoto = true,
                    motionPhotoVideoOffset = entry.videoOffset,
                    motionPhotoVideoLength = entry.videoLength
                )
                mergedItems = createdItems
                return@forEachIndexed
            }
            mergedItems[index] = item.copy(
                isMotionPhoto = true,
                motionPhotoVideoOffset = entry.videoOffset,
                motionPhotoVideoLength = entry.videoLength
            )
        }

        return mergedItems ?: items
    }

    suspend fun preloadMotionCache(
        context: Context,
        items: List<MediaItem>,
        maxCount: Int = 180,
        timeBudgetMs: Long = 3500L
    ): Map<Long, MotionPhotoInfo> {
        if (items.isEmpty() || maxCount <= 0 || timeBudgetMs <= 0L) return emptyMap()
        return preloadMutex.withLock {
            ensureIndexLoaded(context)
            val startedAt = System.currentTimeMillis()
            val detected = LinkedHashMap<Long, MotionPhotoInfo>()
            var checked = 0

            val candidates = items.asSequence()
                .filter { it.type == "photo" }
                .filter { !it.isLivePhoto }
                .toList()

            for (item in candidates) {
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed >= timeBudgetMs) break
                if (checked >= maxCount) break

                val cached = getCachedMotionPhoto(context, item)
                if (cached != null) {
                    if (cached.isMotionPhoto) detected[item.id] = cached
                    continue
                }

                val info = checkMotionPhoto(context, item.uri)
                updateIndex(item, info)
                if (info.isMotionPhoto) detected[item.id] = info
                checked++

                if (checked % 24 == 0) {
                    persistIndex(context, force = false)
                }
            }

            persistIndex(context, force = true)
            detected
        }
    }

    private fun parseXmp(xmp: String): MotionPhotoInfo {
        try {
            val microVideoOffsetRegex = "MicroVideoOffset\\s*=\\s*['\\\"](\\d+)['\\\"]".toRegex(RegexOption.IGNORE_CASE)
            val matchOld = microVideoOffsetRegex.find(xmp)
            if (matchOld != null) {
                val offset = matchOld.groupValues[1].toLongOrNull() ?: 0L
                if (offset > 0) {
                    return MotionPhotoInfo(true, offset, 0) // Old format defines offset from end
                }
            }

            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmp))

            var eventType = parser.eventType
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val semantic = getAttributeValueFlexible(parser, "Semantic")
                    if (semantic == "MotionPhoto") {
                        val lengthStr = getAttributeValueFlexible(parser, "Length")
                        val paddingStr = getAttributeValueFlexible(parser, "Padding")
                        val length = lengthStr?.toLongOrNull() ?: 0L
                        val padding = paddingStr?.toLongOrNull() ?: 0L
                        if (length > 0) {
                            return MotionPhotoInfo(true, length + padding, length)
                        }
                    }
                }
                eventType = parser.next()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return MotionPhotoInfo(false)
    }

    private fun getAttributeValueFlexible(parser: XmlPullParser, localName: String): String? {
        val direct = parser.getAttributeValue(null, localName)
        if (direct != null) return direct
        val namespaced = parser.getAttributeValue(XMP_NAMESPACE_ITEM, localName)
        if (namespaced != null) return namespaced
        for (index in 0 until parser.attributeCount) {
            val name = parser.getAttributeName(index)
            if (name == localName || name.endsWith(":$localName")) {
                return parser.getAttributeValue(index)
            }
        }
        return null
    }

    /**
     * Extracts the video from a Motion Photo to a temporary file.
     */
    fun extractVideo(context: Context, uri: Uri, info: MotionPhotoInfo): File? {
        if (!info.isMotionPhoto || info.videoOffset <= 0) return null

        val key = buildExtractCacheKey(uri, info)
        extractedVideoPathCache[key]?.let { cachedPath ->
            val cachedFile = File(cachedPath)
            if (cachedFile.exists() && cachedFile.length() > 0L) return cachedFile
            extractedVideoPathCache.remove(key)
        }

        val stableHash = key.hashCode().toLong() and 0xffffffffL
        val cacheFile = File(context.cacheDir, "$CACHE_PREFIX${stableHash.toString(16)}.mp4")
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            extractedVideoPathCache[key] = cacheFile.absolutePath
            pruneExtractedPathCacheIfNeeded()
            cacheFile.setLastModified(System.currentTimeMillis())
            return cacheFile
        }

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fileSize = pfd.statSize
                val startPosition = fileSize - info.videoOffset

                if (startPosition < 0 || startPosition >= fileSize) return null

                FileInputStream(pfd.fileDescriptor).use { input ->
                    input.channel.position(startPosition)

                    FileOutputStream(cacheFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int = 0
                        var totalRead: Long = 0
                        val limit = if (info.videoLength > 0) info.videoLength else info.videoOffset

                        while (totalRead < limit && input.read(buffer).also { bytesRead = it } != -1) {
                            val toWrite = if (totalRead + bytesRead > limit) {
                                (limit - totalRead).toInt()
                            } else {
                                bytesRead
                            }
                            output.write(buffer, 0, toWrite)
                            totalRead += toWrite
                        }
                    }
                }
            }
            trimExtractedMotionPayloadToMp4(cacheFile)
            if (cacheFile.exists() && cacheFile.length() > 0L) {
                extractedVideoPathCache[key] = cacheFile.absolutePath
                pruneExtractedPathCacheIfNeeded()
                trimExtractedCache(context.cacheDir)
                return cacheFile
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            if (cacheFile.exists()) cacheFile.delete()
            return null
        }
    }

    private fun buildExtractCacheKey(uri: Uri, info: MotionPhotoInfo): String {
        return "${uri}|${info.videoOffset}|${info.videoLength}"
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

    private fun ensureIndexLoaded(context: Context) {
        if (indexLoaded) return
        synchronized(indexLock) {
            if (indexLoaded) return
            runCatching {
                val file = File(context.filesDir, INDEX_FILE_NAME)
                if (!file.exists()) return@runCatching
                if (file.length() > MAX_INDEX_FILE_BYTES) {
                    runCatching { file.delete() }
                    return@runCatching
                }
                val type = object : TypeToken<List<PersistEntry>>() {}.type
                val entries: List<PersistEntry> = file.reader().buffered().use { reader ->
                    gson.fromJson(reader, type) ?: emptyList()
                }
                entries.forEach { persistentIndex[it.id] = it }
            }
            indexLoaded = true
        }
    }

    private fun updateIndex(item: MediaItem, info: MotionPhotoInfo) {
        val entry = PersistEntry(
            id = item.id,
            dateAdded = item.dateAdded,
            size = item.size,
            isMotionPhoto = info.isMotionPhoto,
            videoOffset = info.videoOffset,
            videoLength = info.videoLength,
            updatedAtMs = System.currentTimeMillis()
        )
        persistentIndex[item.id] = entry
        dirtyCount++
    }

    private fun persistIndex(context: Context, force: Boolean) {
        if (!force && dirtyCount < 24) return
        runCatching {
            val snapshot = if (persistentIndex.size <= MAX_INDEX_ENTRIES) {
                persistentIndex.values.toList()
            } else {
                persistentIndex.values
                    .sortedByDescending { it.updatedAtMs }
                    .take(MAX_INDEX_ENTRIES)
            }
            File(context.filesDir, INDEX_FILE_NAME).writer().buffered().use { writer ->
                gson.toJson(snapshot, writer)
            }
            dirtyCount = 0
        }.onFailure { error ->
            if (error is OutOfMemoryError) {
                MemorySafetyMode.markOomCrash(context)
                AppPerformanceGovernor.onCaughtOutOfMemory()
                runCatching { System.gc() }
            }
        }
    }

    private fun pruneMotionInfoCacheIfNeeded() {
        val overflow = motionInfoCache.size - MAX_MOTION_INFO_CACHE_ENTRIES
        if (overflow <= 0) return
        motionInfoCache.keys
            .asSequence()
            .take(overflow.coerceAtLeast(MAX_MOTION_INFO_CACHE_ENTRIES / 8))
            .toList()
            .forEach { motionInfoCache.remove(it) }
    }

    private fun pruneExtractedPathCacheIfNeeded() {
        val overflow = extractedVideoPathCache.size - MAX_EXTRACTED_PATH_CACHE_ENTRIES
        if (overflow <= 0) return
        extractedVideoPathCache.keys
            .asSequence()
            .take(overflow.coerceAtLeast(MAX_EXTRACTED_PATH_CACHE_ENTRIES / 6))
            .toList()
            .forEach { extractedVideoPathCache.remove(it) }
    }

    private fun trimExtractedCache(cacheDir: File) {
        val files = cacheDir.listFiles { _, name ->
            name.startsWith(CACHE_PREFIX) && name.endsWith(".mp4")
        } ?: return

        if (files.size <= MAX_EXTRACTED_CACHE_FILES) return

        files.sortedByDescending { it.lastModified() }
            .drop(MAX_EXTRACTED_CACHE_FILES)
            .forEach { it.delete() }

        val validPaths = cacheDir.listFiles { _, name ->
            name.startsWith(CACHE_PREFIX) && name.endsWith(".mp4")
        }?.map { it.absolutePath }?.toSet() ?: emptySet()
        val staleKeys = extractedVideoPathCache.entries
            .filter { it.value !in validPaths }
            .map { it.key }
        staleKeys.forEach { extractedVideoPathCache.remove(it) }
    }
}
