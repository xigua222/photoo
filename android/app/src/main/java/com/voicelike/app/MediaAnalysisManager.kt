package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import android.util.JsonWriter
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt
import org.json.JSONObject

/**
 * Unified Media Analysis Manager.
 * Performs single-pass analysis for Similar Photos, Low Quality, and Smart Organize.
 * This centralized service reduces resource usage by loading bitmaps once and running all checks.
 */
object MediaAnalysisManager {
    private const val QUALITY_METRICS_VERSION = 2

    data class CardPreviewInsights(
        val location: String,
        val motionInfo: MotionPhotoHelper.MotionPhotoInfo?
    )

    private const val ANALYZE_TARGET_MAX_SIDE_PX = 1600
    private const val ANALYZE_TARGET_MAX_PIXELS = 1_800_000
    private const val CONSERVATIVE_ANALYZE_TARGET_MAX_SIDE_PX = 1024
    private const val CONSERVATIVE_ANALYZE_TARGET_MAX_PIXELS = 900_000
    private const val MAX_WARMED_KEYWORD_LANG_ENTRIES = 4096
    private const val MAX_FAILED_KEYWORD_ANALYSIS_ENTRIES = 4096
    private const val URI_UNRESOLVED_RETRY_BACKOFF_MS = 10L * 60L * 1000L
    private const val CACHE_SAVE_DEBOUNCE_MS = 1200L
    private const val PREFETCH_SAVE_DEBOUNCE_MS = 1500L
    private const val PERIODIC_ANALYSIS_CACHE_FLUSH_INTERVAL = 36
    private const val PERIODIC_PREFETCH_CACHE_FLUSH_INTERVAL = 48

    data class AnalysisResult(
        val id: Long,
        val dHash: Long = 0,
        val pHash: Long = 0,
        val blurVariance: Double = 0.0,
        val exposureAvg: Int = 128,
        val solidColorStdDev: Double = 100.0,
        val shadowClipRatio: Float = 0f,
        val highlightClipRatio: Float = 0f,
        val qualityMetricsVersion: Int = 1,
        val faceCount: Int = 0,
        val maxFaceSize: Float = 0f,
        val hasClosedEyes: Boolean = false,
        val isMotionPhoto: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class PrefetchState(
        val sizes: Map<Long, Long> = emptyMap(),
        val videoThumbs: Set<Long> = emptySet()
    )

    private data class DecodeConfig(
        val maxSidePx: Int,
        val maxPixels: Int,
        val preferredConfig: Bitmap.Config
    )

    private data class KeywordFailureEntry(
        val fingerprint: String,
        val failedAtMs: Long,
        val failureKind: ImageAnalyzer.AnalyzeFailureKind
    )

    private const val CACHE_FILE = "media_analysis_cache_v2.json"
    private const val ATTEMPTED_IDS_FILE = "media_analysis_attempted_ids.json"
    private const val PREFETCH_CACHE_FILE = "media_prefetch_cache_v2.json"
    private const val MAX_ANALYSIS_CACHE_FILE_BYTES = 24L * 1024L * 1024L
    private const val MAX_ATTEMPTED_IDS_FILE_BYTES = 4L * 1024L * 1024L
    private const val MAX_PREFETCH_CACHE_FILE_BYTES = 6L * 1024L * 1024L
    private val gson = Gson()
    
    // In-memory cache
    private val analysisCache = ConcurrentHashMap<Long, AnalysisResult>()
    private val attemptedPhotoIds = ConcurrentHashMap.newKeySet<Long>()
    private val sizeCache = ConcurrentHashMap<Long, Long>()
    private val videoThumbCache = ConcurrentHashMap<Long, Boolean>()
    private val warmedKeywordLangById = ConcurrentHashMap<Long, String>()
    private val pendingKeywordJobs = ConcurrentHashMap<String, Deferred<List<String>>>()
    private val recentKeywordFailures = ConcurrentHashMap<String, KeywordFailureEntry>()
    private var isCacheLoaded = false
    private var isPrefetchLoaded = false
    private val prefetchCacheLock = Any()
    private val cacheLoadMutex = Mutex()
    private val prefetchLoadMutex = Mutex()
    
    // State
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _isInsightScanning = MutableStateFlow(false)
    val isInsightScanning = _isInsightScanning.asStateFlow()

    private val _insightProgress = MutableStateFlow(0f)
    val insightProgress = _insightProgress.asStateFlow()
    
    private val _scanRatePerMinute = MutableStateFlow(0f)
    val scanRatePerMinute = _scanRatePerMinute.asStateFlow()
    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var scanJob: Job? = null
    private var insightJob: Job? = null
    private var cachePersistJob: Job? = null
    private var prefetchPersistJob: Job? = null
    private var appContext: Context? = null
    private var scanStartTimeMs: Long = 0L
    private var lastRateUpdateMs: Long = 0L
    private var smoothedRatePerMinute: Float = 0f

    // Pause mechanism to prevent OOM during heavy operations (e.g. video playback)
    private val pauseRequests = AtomicInteger(0)

    fun requestPause() {
        pauseRequests.incrementAndGet()
        _isPaused.value = pauseRequests.get() > 0
    }

    fun releasePause() {
        if (pauseRequests.get() > 0) {
            pauseRequests.decrementAndGet()
        }
        _isPaused.value = pauseRequests.get() > 0
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (!isCacheLoaded) {
            scope.launch {
                ensurePersistentCachesLoaded(context)
            }
        }
        if (!isPrefetchLoaded) {
            scope.launch {
                ensurePersistentCachesLoaded(context)
            }
        }
    }

    private suspend fun ensurePersistentCachesLoaded(context: Context) {
        val appCtx = context.applicationContext
        cacheLoadMutex.withLock {
            if (!isCacheLoaded) {
                withContext(Dispatchers.IO) {
                    loadCache(appCtx)
                }
            }
        }
        prefetchLoadMutex.withLock {
            if (!isPrefetchLoaded) {
                withContext(Dispatchers.IO) {
                    loadPrefetchCache(appCtx)
                }
            }
        }
    }

    suspend fun hydrateProgressFromCache(
        context: Context,
        items: List<MediaItem>
    ): Float {
        ensurePersistentCachesLoaded(context)
        val photos = items.filter { it.type == "photo" }
        val restoredProgress = if (photos.isEmpty()) {
            1f
        } else {
            photos.count { isPhotoSettled(it.id) }.toFloat() / photos.size.toFloat()
        }.coerceIn(0f, 1f)
        if (!_isScanning.value) {
            _progress.value = restoredProgress
            if (restoredProgress >= 0.999f) {
                _scanRatePerMinute.value = 0f
            }
        }
        return restoredProgress
    }

    suspend fun hydrateInsightProgressFromCache(
        context: Context,
        items: List<MediaItem>
    ): Float {
        ensurePersistentCachesLoaded(context)
        val eligibleItems = items.filter { it.type == "photo" || it.type == "video" }
        val snapshot = MediaInsightRepository.getMany(context, eligibleItems.map { it.id })
        val restoredProgress = if (eligibleItems.isEmpty()) {
            1f
        } else {
            eligibleItems.count { item -> hasSettledInsightForScan(item, snapshot[item.id]) }
                .toFloat() / eligibleItems.size.toFloat()
        }.coerceIn(0f, 1f)
        if (!_isInsightScanning.value) {
            _insightProgress.value = restoredProgress
        }
        return restoredProgress
    }

    fun trimTransientCaches() {
        warmedKeywordLangById.clear()
        pendingKeywordJobs.entries.removeAll { (_, job) -> job.isCompleted || job.isCancelled }
        pruneKeywordFailureCacheIfNeeded()
        runCatching { appContext?.imageLoader?.memoryCache?.clear() }
    }

    fun clearKeywordFailureBackoff() {
        recentKeywordFailures.clear()
    }

    private fun pruneWarmedKeywordLangCacheIfNeeded() {
        val overflow = warmedKeywordLangById.size - MAX_WARMED_KEYWORD_LANG_ENTRIES
        if (overflow <= 0) return
        warmedKeywordLangById.keys
            .asSequence()
            .take(overflow.coerceAtLeast(MAX_WARMED_KEYWORD_LANG_ENTRIES / 8))
            .toList()
            .forEach { warmedKeywordLangById.remove(it) }
    }

    private fun buildKeywordJobKey(mediaId: Long, languageCode: String): String {
        return "$mediaId#$languageCode"
    }

    private fun buildKeywordFailureFingerprint(item: MediaItem): String {
        return buildString(64) {
            append(item.id)
            append('|')
            append(item.dateAdded)
            append('|')
            append(item.size)
            append('|')
            append(item.uri)
        }
    }

    private fun keywordFailureBackoffMs(kind: ImageAnalyzer.AnalyzeFailureKind): Long {
        return when (kind) {
            ImageAnalyzer.AnalyzeFailureKind.URI_UNRESOLVED -> URI_UNRESOLVED_RETRY_BACKOFF_MS
        }
    }

    private fun shouldSkipKeywordAnalysisForFailure(item: MediaItem, languageCode: String): Boolean {
        val jobKey = buildKeywordJobKey(item.id, languageCode)
        val failure = recentKeywordFailures[jobKey] ?: return false
        val currentFingerprint = buildKeywordFailureFingerprint(item)
        if (failure.fingerprint != currentFingerprint) {
            recentKeywordFailures.remove(jobKey, failure)
            return false
        }
        val elapsed = System.currentTimeMillis() - failure.failedAtMs
        if (elapsed >= keywordFailureBackoffMs(failure.failureKind)) {
            recentKeywordFailures.remove(jobKey, failure)
            return false
        }
        return true
    }

    private fun clearKeywordFailure(item: MediaItem, languageCode: String) {
        recentKeywordFailures.remove(buildKeywordJobKey(item.id, languageCode))
    }

    private fun recordKeywordFailure(
        item: MediaItem,
        languageCode: String,
        failureKind: ImageAnalyzer.AnalyzeFailureKind
    ) {
        recentKeywordFailures[buildKeywordJobKey(item.id, languageCode)] = KeywordFailureEntry(
            fingerprint = buildKeywordFailureFingerprint(item),
            failedAtMs = System.currentTimeMillis(),
            failureKind = failureKind
        )
        pruneKeywordFailureCacheIfNeeded()
    }

    private fun pruneKeywordFailureCacheIfNeeded() {
        val overflow = recentKeywordFailures.size - MAX_FAILED_KEYWORD_ANALYSIS_ENTRIES
        if (overflow <= 0) return
        recentKeywordFailures.entries
            .asSequence()
            .sortedBy { it.value.failedAtMs }
            .take(overflow.coerceAtLeast(MAX_FAILED_KEYWORD_ANALYSIS_ENTRIES / 8))
            .map { it.key }
            .toList()
            .forEach { recentKeywordFailures.remove(it) }
    }

    fun hasWarmedKeywordsForLanguage(mediaId: Long, languageCode: String): Boolean {
        val lang = languageCode.trim()
        if (lang.isBlank()) return false
        return warmedKeywordLangById[mediaId] == lang
    }

    fun hasCachedKeywordsForLanguage(
        snapshot: MediaInsightSnapshot?,
        mediaId: Long,
        languageCode: String
    ): Boolean {
        val lang = languageCode.trim()
        if (lang.isBlank()) return false
        if (snapshot == null || snapshot.keywords.isEmpty()) return false
        return snapshot.keywordLanguage == lang || warmedKeywordLangById[mediaId] == lang
    }

    private fun loadCache(context: Context) {
        try {
            val file = File(context.filesDir, CACHE_FILE)
            if (file.exists() && ensureCacheFileWithinLimit(context, file, MAX_ANALYSIS_CACHE_FILE_BYTES, "load_cache")) {
                val loaded = readAnalysisCacheFromFile(file)
                analysisCache.clear()
                analysisCache.putAll(loaded)
            }
            loadAttemptedIds(context)
            isCacheLoaded = true
        } catch (oom: OutOfMemoryError) {
            activateConservativeScanMode(context, "load_cache", oom)
            analysisCache.clear()
            attemptedPhotoIds.clear()
            runCatching { File(context.filesDir, CACHE_FILE).delete() }
            runCatching { File(context.filesDir, ATTEMPTED_IDS_FILE).delete() }
            isCacheLoaded = true
        } catch (e: Exception) {
            Log.e("MediaAnalysis", "Error loading cache", e)
            AppRuntimeLogStore.logError(context, "MediaAnalysis", "Error loading cache", e)
            analysisCache.clear()
            attemptedPhotoIds.clear()
            runCatching { File(context.filesDir, CACHE_FILE).delete() }
            runCatching { File(context.filesDir, ATTEMPTED_IDS_FILE).delete() }
            isCacheLoaded = true
        }
    }

    private fun saveCache(context: Context) {
        try {
            val file = File(context.filesDir, CACHE_FILE)
            val tmp = File(context.filesDir, "$CACHE_FILE.tmp")
            writeAnalysisCacheAtomic(tmp)
            if (!tmp.renameTo(file)) {
                writeAnalysisCacheAtomic(file)
                runCatching { tmp.delete() }
            }
            saveAttemptedIds(context)
        } catch (oom: OutOfMemoryError) {
            activateConservativeScanMode(context, "save_cache", oom)
            runCatching { File(context.filesDir, "$CACHE_FILE.tmp").delete() }
        } catch (e: Exception) {
            Log.e("MediaAnalysis", "Error saving cache", e)
            AppRuntimeLogStore.logError(context, "MediaAnalysis", "Error saving cache", e)
        }
    }

    private fun loadAttemptedIds(context: Context) {
        val file = File(context.filesDir, ATTEMPTED_IDS_FILE)
        attemptedPhotoIds.clear()
        if (!file.exists()) return
        runCatching {
            if (!ensureCacheFileWithinLimit(context, file, MAX_ATTEMPTED_IDS_FILE_BYTES, "load_attempted_ids")) {
                return@runCatching
            }
            val type = object : TypeToken<List<Long>>() {}.type
            val loaded: List<Long> = file.reader().buffered().use { reader ->
                gson.fromJson(reader, type) ?: emptyList()
            }
            attemptedPhotoIds.addAll(loaded)
        }.onFailure {
            runCatching { file.delete() }
        }
    }

    private fun saveAttemptedIds(context: Context) {
        val file = File(context.filesDir, ATTEMPTED_IDS_FILE)
        val tmp = File(context.filesDir, "$ATTEMPTED_IDS_FILE.tmp")
        try {
            writeAttemptedIdsAtomic(tmp)
            if (!tmp.renameTo(file)) {
                writeAttemptedIdsAtomic(file)
                runCatching { tmp.delete() }
            }
        } catch (oom: OutOfMemoryError) {
            activateConservativeScanMode(context, "save_attempted_ids", oom)
            runCatching { tmp.delete() }
        }
    }

    private fun loadPrefetchCache(context: Context) {
        synchronized(prefetchCacheLock) {
            try {
                val file = File(context.filesDir, PREFETCH_CACHE_FILE)
                if (file.exists() && ensureCacheFileWithinLimit(context, file, MAX_PREFETCH_CACHE_FILE_BYTES, "load_prefetch_cache")) {
                    val loaded = runCatching {
                        readPrefetchStateFromFile(file)
                    }.getOrElse {
                        file.reader().buffered().use { reader ->
                            parsePrefetchStateLenient(reader.readText())
                        }
                    }
                    sizeCache.clear()
                    videoThumbCache.clear()
                    sizeCache.putAll(loaded.sizes)
                    loaded.videoThumbs.forEach { videoThumbCache[it] = true }
                }
                Unit
            } catch (oom: OutOfMemoryError) {
                activateConservativeScanMode(context, "load_prefetch_cache", oom)
                sizeCache.clear()
                videoThumbCache.clear()
                runCatching { File(context.filesDir, PREFETCH_CACHE_FILE).delete() }
            } catch (e: Exception) {
                Log.e("MediaAnalysis", "Error loading prefetch cache", e)
                AppRuntimeLogStore.logError(context, "MediaAnalysis", "Error loading prefetch cache", e)
                // Cache is optional. Clear corrupted state and stop repeated retries/spam.
                sizeCache.clear()
                videoThumbCache.clear()
                runCatching { File(context.filesDir, PREFETCH_CACHE_FILE).delete() }
            } finally {
                isPrefetchLoaded = true
            }
        }
    }

    private fun savePrefetchCache(context: Context) {
        synchronized(prefetchCacheLock) {
            try {
                val state = PrefetchState(
                    sizes = sizeCache.toMap(),
                    videoThumbs = videoThumbCache.keys.toSet()
                )
                writePrefetchCacheAtomic(context, state)
            } catch (oom: OutOfMemoryError) {
                activateConservativeScanMode(context, "save_prefetch_cache", oom)
            } catch (e: Exception) {
                Log.e("MediaAnalysis", "Error saving prefetch cache", e)
                AppRuntimeLogStore.logError(context, "MediaAnalysis", "Error saving prefetch cache", e)
            }
        }
    }

    private fun scheduleCacheSave(context: Context, force: Boolean = false) {
        val appCtx = context.applicationContext
        if (force) {
            cachePersistJob?.cancel()
            cachePersistJob = null
            saveCache(appCtx)
            return
        }
        cachePersistJob?.cancel()
        cachePersistJob = scope.launch(Dispatchers.IO) {
            delay(CACHE_SAVE_DEBOUNCE_MS)
            saveCache(appCtx)
        }
    }

    private fun schedulePrefetchCacheSave(context: Context, force: Boolean = false) {
        val appCtx = context.applicationContext
        if (force) {
            prefetchPersistJob?.cancel()
            prefetchPersistJob = null
            savePrefetchCache(appCtx)
            return
        }
        prefetchPersistJob?.cancel()
        prefetchPersistJob = scope.launch(Dispatchers.IO) {
            delay(PREFETCH_SAVE_DEBOUNCE_MS)
            savePrefetchCache(appCtx)
        }
    }

    fun flushPersistentCaches(context: Context? = appContext) {
        val appCtx = context?.applicationContext ?: return
        cachePersistJob?.cancel()
        cachePersistJob = null
        prefetchPersistJob?.cancel()
        prefetchPersistJob = null
        saveCache(appCtx)
        savePrefetchCache(appCtx)
    }

    private fun writePrefetchCacheAtomic(context: Context, state: PrefetchState) {
        val file = File(context.filesDir, PREFETCH_CACHE_FILE)
        val tmp = File(context.filesDir, "$PREFETCH_CACHE_FILE.tmp")
        writePrefetchStateToFile(tmp, state)
        if (!tmp.renameTo(file)) {
            writePrefetchStateToFile(file, state)
            runCatching { tmp.delete() }
        }
    }

    private fun parsePrefetchStateLenient(json: String): PrefetchState {
        val root = JSONObject(json)
        val sizes = linkedMapOf<Long, Long>()
        val sizesObj = root.optJSONObject("sizes")
        if (sizesObj != null) {
            val keys = sizesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = key.toLongOrNull() ?: continue
                sizes[id] = sizesObj.optLong(key, 0L)
            }
        }
        val thumbs = linkedSetOf<Long>()
        val arr = root.optJSONArray("videoThumbs")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val id = when (val raw = arr.opt(i)) {
                    is Number -> raw.toLong()
                    is String -> raw.toLongOrNull()
                    else -> null
                }
                if (id != null) thumbs.add(id)
            }
        }
        return PrefetchState(sizes = sizes, videoThumbs = thumbs)
    }

    fun getAnalysis(id: Long): AnalysisResult? {
        return analysisCache[id]
    }

    fun isPhotoSettled(id: Long): Boolean {
        return analysisCache.containsKey(id) || attemptedPhotoIds.contains(id)
    }

    private fun writeAnalysisCacheAtomic(file: File) {
        JsonWriter(FileWriter(file, false).buffered()).use { writer ->
            writer.beginObject()
            analysisCache.forEach { (key, value) ->
                writer.name(key.toString())
                writer.beginObject()
                writer.name("id").value(value.id)
                writer.name("dh").value(value.dHash)
                writer.name("ph").value(value.pHash)
                writer.name("bv").value(value.blurVariance)
                writer.name("ea").value(value.exposureAvg.toLong())
                writer.name("sc").value(value.solidColorStdDev)
                writer.name("sr").value(value.shadowClipRatio.toDouble())
                writer.name("hr").value(value.highlightClipRatio.toDouble())
                writer.name("qv").value(value.qualityMetricsVersion.toLong())
                writer.name("fc").value(value.faceCount.toLong())
                writer.name("mf").value(value.maxFaceSize.toDouble())
                writer.name("ce").value(value.hasClosedEyes)
                writer.name("mp").value(value.isMotionPhoto)
                writer.name("ts").value(value.timestamp)
                writer.endObject()
            }
            writer.endObject()
        }
    }

    private fun readAnalysisCacheFromFile(file: File): Map<Long, AnalysisResult> {
        return file.reader().buffered().use { fileReader ->
            JsonReader(fileReader).use { reader ->
                reader.isLenient = true
                val parsed = linkedMapOf<Long, AnalysisResult>()
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return@use parsed
                }
                reader.beginObject()
                while (reader.hasNext()) {
                    val mapKey = reader.nextName()
                    val fallbackId = mapKey.toLongOrNull()
                    if (fallbackId == null) {
                        reader.skipValue()
                        continue
                    }
                    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                        reader.skipValue()
                        continue
                    }
                    val parsedEntry = readAnalysisEntry(reader, fallbackId)
                    if (parsedEntry != null) {
                        parsed[fallbackId] = parsedEntry
                    }
                }
                reader.endObject()
                parsed
            }
        }
    }

    private fun writeAttemptedIdsAtomic(file: File) {
        JsonWriter(FileWriter(file, false).buffered()).use { writer ->
            writer.beginArray()
            attemptedPhotoIds
                .toList()
                .sorted()
                .forEach { id ->
                    writer.value(id)
                }
            writer.endArray()
        }
    }

    private fun writePrefetchStateToFile(file: File, state: PrefetchState) {
        FileWriter(file, false).buffered().use { writer ->
            gson.toJson(state, writer)
        }
    }

    private fun readPrefetchStateFromFile(file: File): PrefetchState {
        val type = object : TypeToken<PrefetchState>() {}.type
        return file.reader().buffered().use { reader ->
            gson.fromJson(reader, type) ?: PrefetchState()
        }
    }

    private fun parseAnalysisCacheLenient(json: String): Map<Long, AnalysisResult> {
        val root = JSONObject(json)
        val parsed = linkedMapOf<Long, AnalysisResult>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val mapKey = keys.next()
            val fallbackId = mapKey.toLongOrNull() ?: continue
            val entry = root.optJSONObject(mapKey) ?: continue
            val result = parseAnalysisEntry(entry, fallbackId) ?: continue
            parsed[fallbackId] = result
        }
        return parsed
    }

    private fun parseAnalysisEntry(entry: JSONObject, fallbackId: Long): AnalysisResult? {
        return when {
            entry.has("id") || entry.has("dh") || entry.has("ph") || entry.has("bv") -> {
                AnalysisResult(
                    id = optLong(entry, "id") ?: fallbackId,
                    dHash = optLong(entry, "dh", "dHash") ?: 0L,
                    pHash = optLong(entry, "ph", "pHash") ?: 0L,
                    blurVariance = optDouble(entry, "bv", "blurVariance") ?: 0.0,
                    exposureAvg = optInt(entry, "ea", "exposureAvg") ?: 128,
                    solidColorStdDev = optDouble(entry, "sc", "solidColorStdDev") ?: 100.0,
                    shadowClipRatio = (optDouble(entry, "sr", "shadowClipRatio") ?: 0.0).toFloat(),
                    highlightClipRatio = (optDouble(entry, "hr", "highlightClipRatio") ?: 0.0).toFloat(),
                    qualityMetricsVersion = optInt(entry, "qv", "qualityMetricsVersion") ?: 1,
                    faceCount = optInt(entry, "fc", "faceCount") ?: 0,
                    maxFaceSize = (optDouble(entry, "mf", "maxFaceSize") ?: 0.0).toFloat(),
                    hasClosedEyes = optBoolean(entry, "ce", "hasClosedEyes") ?: false,
                    isMotionPhoto = optBoolean(entry, "mp", "isMotionPhoto") ?: false,
                    timestamp = optLong(entry, "ts", "timestamp") ?: System.currentTimeMillis()
                )
            }

            entry.has("k") -> {
                // Obfuscated cache from a build that already included pHash.
                AnalysisResult(
                    id = optLong(entry, "a") ?: fallbackId,
                    dHash = optLong(entry, "b") ?: 0L,
                    pHash = optLong(entry, "c") ?: 0L,
                    blurVariance = optDouble(entry, "d") ?: 0.0,
                    exposureAvg = optInt(entry, "e") ?: 128,
                    solidColorStdDev = optDouble(entry, "f") ?: 100.0,
                    shadowClipRatio = 0f,
                    highlightClipRatio = 0f,
                    qualityMetricsVersion = 1,
                    faceCount = optInt(entry, "g") ?: 0,
                    maxFaceSize = (optDouble(entry, "h") ?: 0.0).toFloat(),
                    hasClosedEyes = optBoolean(entry, "i") ?: false,
                    isMotionPhoto = optBoolean(entry, "j") ?: false,
                    timestamp = optLong(entry, "k") ?: System.currentTimeMillis()
                )
            }

            entry.has("j") || entry.has("a") -> {
                // Legacy obfuscated cache before pHash was added.
                AnalysisResult(
                    id = optLong(entry, "a") ?: fallbackId,
                    dHash = optLong(entry, "b") ?: 0L,
                    pHash = 0L,
                    blurVariance = optDouble(entry, "c") ?: 0.0,
                    exposureAvg = optInt(entry, "d") ?: 128,
                    solidColorStdDev = optDouble(entry, "e") ?: 100.0,
                    shadowClipRatio = 0f,
                    highlightClipRatio = 0f,
                    qualityMetricsVersion = 1,
                    faceCount = optInt(entry, "f") ?: 0,
                    maxFaceSize = (optDouble(entry, "g") ?: 0.0).toFloat(),
                    hasClosedEyes = optBoolean(entry, "h") ?: false,
                    isMotionPhoto = optBoolean(entry, "i") ?: false,
                    timestamp = optLong(entry, "j") ?: System.currentTimeMillis()
                )
            }

            else -> null
        }
    }

    private fun optLong(entry: JSONObject, vararg names: String): Long? {
        for (name in names) {
            if (!entry.has(name)) continue
            val value = entry.opt(name)
            val parsed = when (value) {
                is Number -> value.toLong()
                is String -> value.toDoubleOrNull()?.toLong()
                else -> null
            }
            if (parsed != null) return parsed
        }
        return null
    }

    private fun optInt(entry: JSONObject, vararg names: String): Int? {
        for (name in names) {
            if (!entry.has(name)) continue
            val value = entry.opt(name)
            val parsed = when (value) {
                is Number -> value.toInt()
                is String -> value.toDoubleOrNull()?.toInt()
                else -> null
            }
            if (parsed != null) return parsed
        }
        return null
    }

    private fun optDouble(entry: JSONObject, vararg names: String): Double? {
        for (name in names) {
            if (!entry.has(name)) continue
            val value = entry.opt(name)
            val parsed = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
            if (parsed != null) return parsed
        }
        return null
    }

    private fun optBoolean(entry: JSONObject, vararg names: String): Boolean? {
        for (name in names) {
            if (!entry.has(name)) continue
            val value = entry.opt(name)
            val parsed = when (value) {
                is Boolean -> value
                is Number -> value.toInt() != 0
                is String -> when {
                    value.equals("true", ignoreCase = true) -> true
                    value.equals("false", ignoreCase = true) -> false
                    value.toIntOrNull() != null -> value.toInt() != 0
                    else -> null
                }
                else -> null
            }
            if (parsed != null) return parsed
        }
        return null
    }

    suspend fun ensureSimilarityHashes(item: MediaItem): AnalysisResult? {
        if (item.type != "photo") return analysisCache[item.id]
        val context = appContext ?: return analysisCache[item.id]
        if (!isCacheLoaded) {
            withContext(Dispatchers.IO) { loadCache(context) }
        }

        val existing = analysisCache[item.id] ?: return null
        if (existing.dHash != 0L && existing.pHash != 0L) return existing

        val hashes = computeSimilarityHashes(context, item) ?: return existing
        val updated = existing.copy(
            dHash = if (existing.dHash != 0L) existing.dHash else hashes.first,
            pHash = hashes.second
        )
        analysisCache[item.id] = updated
        scheduleCacheSave(context)
        return updated
    }

    fun getEffectiveSize(item: MediaItem): Long {
        val cached = sizeCache[item.id]
        if (cached == null || cached <= 0L) return item.size
        return maxOf(item.size, cached)
    }

    suspend fun warmVideoThumbs(context: Context, items: List<MediaItem>, maxCount: Int = 40) {
        withContext(Dispatchers.IO) {
            items.asSequence()
                .filter { it.type == "video" }
                .take(maxCount)
                .forEach { prefetchVideoThumb(context, it) }
        }
    }

    fun getLowQualityCount(): Int {
        return analysisCache.values.count { 
            // Criteria for Low Quality:
            // Blur > 500 (approx), or Exposure < 50 or > 200, or Closed Eyes
            // Need to match LowQualityView logic
            // Assuming standard thresholds:
            val isBlurry = it.blurVariance < 100.0 
            val isDark = it.exposureAvg < 40
            val isBright = it.exposureAvg > 215
            val isSolid = it.solidColorStdDev < 10.0
            
            isBlurry || isDark || isBright || isSolid
        }
    }

    /**
     * Starts a background scan for all media items.
     * Skips items that are already analyzed.
     */
    fun startScan(context: Context, items: List<MediaItem>) {
        if (_isScanning.value || scanJob?.isActive == true) return

        appContext = context.applicationContext
        scanJob = scope.launch {
            // Set lowest thread priority for minimal impact
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

            _isScanning.value = true
            scanStartTimeMs = System.currentTimeMillis()
            lastRateUpdateMs = scanStartTimeMs
            smoothedRatePerMinute = 0f
            _scanRatePerMinute.value = 0f

            val photos = items.filter { it.type == "photo" }
            val total = photos.size
            if (total == 0) {
                _isScanning.value = false
                _progress.value = 1f
                return@launch
            }

            try {
                ensurePersistentCachesLoaded(context)

                // Calculate initial progress based on cache
                val initialAnalyzed = photos.count { isPhotoSettled(it.id) }
                _progress.value = initialAnalyzed.toFloat() / total

                val toAnalyzeIds = photos.filter { !isPhotoSettled(it.id) }.map { it.id }.toSet()
                val needsPrefetch = items.any(::shouldPrefetchInGlobalScan)
                if (toAnalyzeIds.isEmpty() && !needsPrefetch) {
                    _isScanning.value = false
                    _progress.value = 1f
                    return@launch
                }

                var analyzedCount = 0
                val prefetchJob = if (needsPrefetch) {
                    launch(Dispatchers.IO) {
                        var prefetchedCount = 0
                        for (item in items) {
                            if (!isActive) break

                            // Pause if requested or globally throttled by governor.
                            while (isActive && (pauseRequests.get() > 0 || AppPerformanceGovernor.shouldPauseNonCriticalWork())) {
                                delay(AppPerformanceGovernor.scaledDelay(400))
                            }

                            if (!shouldPrefetchInGlobalScan(item)) {
                                continue
                            }

                            if (AppPerformanceGovernor.isMemoryPressureHigh()) {
                                relieveMemoryPressure(context, aggressive = true)
                            }

                            val prefetched = ensureGlobalScanPrefetchForItem(context, item)
                            if (prefetched) {
                                prefetchedCount++
                                if (prefetchedCount % PERIODIC_PREFETCH_CACHE_FLUSH_INTERVAL == 0) {
                                    schedulePrefetchCacheSave(context)
                                }
                            }
                            delay(AppPerformanceGovernor.scaledDelay(8))
                        }
                    }
                } else {
                    null
                }

                val photoItems = photos.filter { toAnalyzeIds.contains(it.id) }
                if (photoItems.isNotEmpty()) {
                    val coreCount = Runtime.getRuntime().availableProcessors()
                    var offset = 0
                    while (offset < photoItems.size) {
                        if (!isActive) break

                        // Pause if requested or globally throttled by governor.
                        while (isActive && (pauseRequests.get() > 0 || AppPerformanceGovernor.shouldPauseNonCriticalWork())) {
                            delay(AppPerformanceGovernor.scaledDelay(400))
                        }

                        if (AppPerformanceGovernor.isMemoryPressureHigh()) {
                            relieveMemoryPressure(context, aggressive = true)
                            delay(AppPerformanceGovernor.scaledDelay(160))
                        }

                        val parallelism = effectiveAnalysisParallelism(context, coreCount)
                        val dispatcher = Dispatchers.Default.limitedParallelism(parallelism)
                        val batchSize = effectiveAnalysisBatchSize(context, parallelism)
                        val endExclusive = (offset + batchSize).coerceAtMost(photoItems.size)
                        val chunk = photoItems.subList(offset, endExclusive)
                        offset = endExclusive

                        val batchStart = System.currentTimeMillis()
                        val results = if (parallelism <= 1) {
                            chunk.map { item -> item to analyzeItem(context, item) }
                        } else {
                            chunk.map { item ->
                                async(dispatcher) { item to analyzeItem(context, item) }
                            }.awaitAll()
                        }
                        for ((item, result) in results) {
                            attemptedPhotoIds.add(item.id)
                            if (result != null) {
                                analysisCache[item.id] = result
                            }
                            analyzedCount++
                            _progress.value = (initialAnalyzed + analyzedCount).toFloat() / total
                            val now = System.currentTimeMillis()
                            if (now - lastRateUpdateMs >= 1000L) {
                                val elapsedMinutes = maxOf(0.25f, (now - scanStartTimeMs) / 60000f)
                                val rawRate = analyzedCount / elapsedMinutes
                                smoothedRatePerMinute = if (smoothedRatePerMinute == 0f) {
                                    rawRate
                                } else {
                                    smoothedRatePerMinute * 0.7f + rawRate * 0.3f
                                }
                                _scanRatePerMinute.value = smoothedRatePerMinute
                                lastRateUpdateMs = now
                            }
                            if (analyzedCount % PERIODIC_ANALYSIS_CACHE_FLUSH_INTERVAL == 0) {
                                scheduleCacheSave(context)
                            } else if (AppPerformanceGovernor.isMemoryPressureHigh()) {
                                scheduleCacheSave(context, force = true)
                            }
                        }
                        val batchElapsed = System.currentTimeMillis() - batchStart
                        val perfMode = AppPerformanceGovernor.currentMode()
                        val targetPerItemMs = when (perfMode) {
                            AppPerformanceGovernor.PerfMode.NORMAL -> if (parallelism > 1) 16L else 24L
                            AppPerformanceGovernor.PerfMode.BALANCED -> 30L
                            AppPerformanceGovernor.PerfMode.COOL -> 55L
                        }
                        val targetBatchMs = targetPerItemMs * chunk.size
                        val maxExtraDelay = when (perfMode) {
                            AppPerformanceGovernor.PerfMode.NORMAL -> 40L
                            AppPerformanceGovernor.PerfMode.BALANCED -> 70L
                            AppPerformanceGovernor.PerfMode.COOL -> 120L
                        }
                        val extraDelay = (targetBatchMs - batchElapsed).coerceAtLeast(0L).coerceAtMost(maxExtraDelay)
                        if (extraDelay > 0L) {
                            delay(AppPerformanceGovernor.scaledDelay(extraDelay))
                        }
                    }
                }

                // Core analysis phase finished. Keep optional enrichment in background path,
                // but do not block feature pages that only depend on analysis results.
                val analyzedNow = photos.count { analysisCache.containsKey(it.id) }
                if (analyzedNow >= total) {
                    _progress.value = 1f
                    _isScanning.value = false
                    _scanRatePerMinute.value = 0f
                }

                prefetchJob?.join()
                flushPersistentCaches(context)
            } catch (oom: OutOfMemoryError) {
                activateConservativeScanMode(context, "core_scan", oom)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
                _progress.value = settledProgressFor(photos)
                _scanRatePerMinute.value = 0f
            }
        }
    }

    private fun readAnalysisEntry(reader: JsonReader, fallbackId: Long): AnalysisResult? {
        var idValue: Long? = null
        var modernDHashValue: Long? = null
        var modernPHashValue: Long? = null
        var modernBlurVarianceValue: Double? = null
        var modernExposureAvgValue: Int? = null
        var modernSolidColorStdDevValue: Double? = null
        var modernShadowClipRatioValue: Float? = null
        var modernHighlightClipRatioValue: Float? = null
        var modernQualityMetricsVersionValue: Int? = null
        var modernFaceCountValue: Int? = null
        var modernMaxFaceSizeValue: Float? = null
        var modernHasClosedEyesValue: Boolean? = null
        var modernIsMotionPhotoValue: Boolean? = null
        var modernTimestampValue: Long? = null

        var legacyA: Long? = null
        var legacyB: Long? = null
        var legacyCNumber: Double? = null
        var legacyDNumber: Double? = null
        var legacyENumber: Double? = null
        var legacyFNumber: Double? = null
        var legacyGNumber: Double? = null
        var legacyHNumber: Double? = null
        var legacyHBoolean: Boolean? = null
        var legacyIBoolean: Boolean? = null
        var legacyJBoolean: Boolean? = null
        var legacyJLong: Long? = null
        var legacyK: Long? = null
        var sawModernShape = false
        var sawObfuscatedShape = false

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> {
                    idValue = readLongValue(reader)
                    sawModernShape = true
                }
                "dh", "dHash" -> {
                    modernDHashValue = readLongValue(reader)
                    sawModernShape = true
                }
                "ph", "pHash" -> {
                    modernPHashValue = readLongValue(reader)
                    sawModernShape = true
                }
                "bv", "blurVariance" -> {
                    modernBlurVarianceValue = readDoubleValue(reader)
                    sawModernShape = true
                }
                "ea", "exposureAvg" -> {
                    modernExposureAvgValue = readIntValue(reader)
                    sawModernShape = true
                }
                "sc", "solidColorStdDev" -> {
                    modernSolidColorStdDevValue = readDoubleValue(reader)
                    sawModernShape = true
                }
                "sr", "shadowClipRatio" -> {
                    modernShadowClipRatioValue = readDoubleValue(reader)?.toFloat()
                    sawModernShape = true
                }
                "hr", "highlightClipRatio" -> {
                    modernHighlightClipRatioValue = readDoubleValue(reader)?.toFloat()
                    sawModernShape = true
                }
                "qv", "qualityMetricsVersion" -> {
                    modernQualityMetricsVersionValue = readIntValue(reader)
                    sawModernShape = true
                }
                "fc", "faceCount" -> {
                    modernFaceCountValue = readIntValue(reader)
                    sawModernShape = true
                }
                "mf", "maxFaceSize" -> {
                    modernMaxFaceSizeValue = readDoubleValue(reader)?.toFloat()
                    sawModernShape = true
                }
                "ce", "hasClosedEyes" -> {
                    modernHasClosedEyesValue = readBooleanValue(reader)
                    sawModernShape = true
                }
                "mp", "isMotionPhoto" -> {
                    modernIsMotionPhotoValue = readBooleanCompatValue(reader)
                    sawModernShape = true
                }
                "ts", "timestamp" -> {
                    modernTimestampValue = readLongValue(reader)
                    sawModernShape = true
                }
                "a" -> legacyA = readLongValue(reader).also { sawObfuscatedShape = true }
                "b" -> legacyB = readLongValue(reader).also { sawObfuscatedShape = true }
                "c" -> legacyCNumber = readDoubleValue(reader).also { sawObfuscatedShape = true }
                "d" -> legacyDNumber = readDoubleValue(reader).also { sawObfuscatedShape = true }
                "e" -> legacyENumber = readDoubleValue(reader).also { sawObfuscatedShape = true }
                "f" -> legacyFNumber = readDoubleValue(reader).also { sawObfuscatedShape = true }
                "g" -> legacyGNumber = readDoubleValue(reader).also { sawObfuscatedShape = true }
                "h" -> {
                    when (reader.peek()) {
                        JsonToken.BOOLEAN -> legacyHBoolean = reader.nextBoolean()
                        JsonToken.STRING -> {
                            val raw = reader.nextString()
                            legacyHBoolean = when {
                                raw.equals("true", ignoreCase = true) -> true
                                raw.equals("false", ignoreCase = true) -> false
                                else -> null
                            }
                            if (legacyHBoolean == null) {
                                legacyHNumber = raw.toDoubleOrNull()
                            }
                        }
                        else -> legacyHNumber = readDoubleValue(reader)
                    }
                    sawObfuscatedShape = true
                }
                "i" -> legacyIBoolean = readBooleanValue(reader).also { sawObfuscatedShape = true }
                "j" -> {
                    when (reader.peek()) {
                        JsonToken.BOOLEAN -> legacyJBoolean = reader.nextBoolean()
                        JsonToken.STRING -> {
                            val raw = reader.nextString()
                            legacyJBoolean = when {
                                raw.equals("true", ignoreCase = true) -> true
                                raw.equals("false", ignoreCase = true) -> false
                                else -> null
                            }
                            if (legacyJBoolean == null) {
                                legacyJLong = raw.toDoubleOrNull()?.toLong()
                            }
                        }
                        else -> legacyJLong = readLongValue(reader)
                    }
                    sawObfuscatedShape = true
                }
                "k" -> legacyK = readLongValue(reader).also { sawObfuscatedShape = true }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return when {
            sawModernShape -> AnalysisResult(
                id = idValue ?: fallbackId,
                dHash = modernDHashValue ?: 0L,
                pHash = modernPHashValue ?: 0L,
                blurVariance = modernBlurVarianceValue ?: 0.0,
                exposureAvg = modernExposureAvgValue ?: 128,
                solidColorStdDev = modernSolidColorStdDevValue ?: 100.0,
                shadowClipRatio = modernShadowClipRatioValue ?: 0f,
                highlightClipRatio = modernHighlightClipRatioValue ?: 0f,
                qualityMetricsVersion = modernQualityMetricsVersionValue ?: 1,
                faceCount = modernFaceCountValue ?: 0,
                maxFaceSize = modernMaxFaceSizeValue ?: 0f,
                hasClosedEyes = modernHasClosedEyesValue ?: false,
                isMotionPhoto = modernIsMotionPhotoValue ?: false,
                timestamp = modernTimestampValue ?: System.currentTimeMillis()
            )

            sawObfuscatedShape && legacyK != null -> AnalysisResult(
                id = legacyA ?: fallbackId,
                dHash = legacyB ?: 0L,
                pHash = legacyCNumber?.toLong() ?: 0L,
                blurVariance = legacyDNumber ?: 0.0,
                exposureAvg = legacyENumber?.toInt() ?: 128,
                solidColorStdDev = legacyFNumber ?: 100.0,
                shadowClipRatio = 0f,
                highlightClipRatio = 0f,
                qualityMetricsVersion = 1,
                faceCount = legacyGNumber?.toInt() ?: 0,
                maxFaceSize = legacyHNumber?.toFloat() ?: 0f,
                hasClosedEyes = legacyIBoolean ?: false,
                isMotionPhoto = legacyJBoolean ?: false,
                timestamp = legacyK
            )

            sawObfuscatedShape -> AnalysisResult(
                id = legacyA ?: fallbackId,
                dHash = legacyB ?: 0L,
                pHash = 0L,
                blurVariance = legacyCNumber ?: 0.0,
                exposureAvg = legacyDNumber?.toInt() ?: 128,
                solidColorStdDev = legacyENumber ?: 100.0,
                shadowClipRatio = 0f,
                highlightClipRatio = 0f,
                qualityMetricsVersion = 1,
                faceCount = legacyFNumber?.toInt() ?: 0,
                maxFaceSize = legacyGNumber?.toFloat() ?: 0f,
                hasClosedEyes = legacyHBoolean ?: false,
                isMotionPhoto = legacyIBoolean ?: false,
                timestamp = legacyJLong ?: System.currentTimeMillis()
            )

            else -> null
        }
    }

    private fun readLongValue(reader: JsonReader): Long? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.NUMBER -> runCatching { reader.nextLong() }.getOrElse {
                runCatching { reader.nextDouble().toLong() }.getOrNull()
            }
            JsonToken.STRING -> reader.nextString().toDoubleOrNull()?.toLong()
            JsonToken.BOOLEAN -> if (reader.nextBoolean()) 1L else 0L
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    private fun readIntValue(reader: JsonReader): Int? {
        return readLongValue(reader)?.toInt()
    }

    private fun readDoubleValue(reader: JsonReader): Double? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.NUMBER -> runCatching { reader.nextDouble() }.getOrNull()
            JsonToken.STRING -> reader.nextString().toDoubleOrNull()
            JsonToken.BOOLEAN -> if (reader.nextBoolean()) 1.0 else 0.0
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    private fun readBooleanValue(reader: JsonReader): Boolean? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NUMBER -> runCatching { reader.nextInt() != 0 }.getOrElse {
                runCatching { reader.nextDouble() != 0.0 }.getOrNull()
            }
            JsonToken.STRING -> {
                when (val raw = reader.nextString().trim().lowercase()) {
                    "true", "1" -> true
                    "false", "0" -> false
                    else -> null
                }
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    private fun readBooleanCompatValue(reader: JsonReader): Boolean? {
        return readBooleanValue(reader)
    }

    private fun ensureCacheFileWithinLimit(
        context: Context,
        file: File,
        maxBytes: Long,
        source: String
    ): Boolean {
        if (!file.exists()) return false
        val length = file.length()
        if (length <= maxBytes) return true
        val error = IllegalStateException("cache file too large: ${file.name} ($length > $maxBytes)")
        Log.w("MediaAnalysis", "Discarding oversized cache file ${file.name}: $length")
        AppRuntimeLogStore.logError(context, "MediaAnalysis/$source", "Discarding oversized cache file", error)
        runCatching { file.delete() }
        runCatching { File(file.parentFile, "${file.name}.tmp").delete() }
        return false
    }

    fun startInsightScan(context: Context, items: List<MediaItem>) {
        if (_isInsightScanning.value || insightJob?.isActive == true) return

        insightJob = scope.launch {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            _isInsightScanning.value = true

            try {
                ensurePersistentCachesLoaded(context)
                if (MemorySafetyMode.shouldReduceBackgroundWork(context)) {
                    while (isActive && _isScanning.value) {
                        delay(AppPerformanceGovernor.scaledDelay(500))
                    }
                }
                val eligibleItems = items.filter { it.type == "photo" || it.type == "video" }
                if (eligibleItems.isEmpty()) {
                    _insightProgress.value = 1f
                    return@launch
                }

                val initialSnapshot = MediaInsightRepository.getMany(context, eligibleItems.map { it.id })
                var completed = eligibleItems.count { item ->
                    hasSettledInsightForScan(item, initialSnapshot[item.id])
                }
                val total = eligibleItems.size
                _insightProgress.value = (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)

                val pending = eligibleItems.filterNot { item ->
                    hasSettledInsightForScan(item, initialSnapshot[item.id])
                }
                if (pending.isEmpty()) {
                    _insightProgress.value = 1f
                    return@launch
                }

                pending.forEachIndexed { index, item ->
                    while (isActive && (pauseRequests.get() > 0 || AppPerformanceGovernor.shouldPauseNonCriticalWork())) {
                        delay(AppPerformanceGovernor.scaledDelay(350))
                    }

                    runCatching { ensurePrefetchForItem(context, item) }
                    runCatching { upsertExifInsightIfMissing(context, item) }
                    completed += 1
                    _insightProgress.value = (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    if ((index + 1) % 48 == 0) {
                        MediaInsightRepository.flush(context)
                    }
                    delay(AppPerformanceGovernor.scaledDelay(8))
                }
                MediaInsightRepository.flush(context)
                _insightProgress.value = 1f
            } catch (oom: OutOfMemoryError) {
                activateConservativeScanMode(context, "insight_scan", oom)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isInsightScanning.value = false
            }
        }
    }
    
    fun stopScan() {
        scanJob?.cancel()
        insightJob?.cancel()
        _isScanning.value = false
        _isInsightScanning.value = false
        _scanRatePerMinute.value = 0f
    }

    fun resetInsightProgress() {
        insightJob?.cancel()
        _isInsightScanning.value = false
        _insightProgress.value = 0f
    }

    fun resetAnalysisData(context: Context? = appContext) {
        stopScan()
        pauseRequests.set(0)
        _isPaused.value = false
        _progress.value = 0f
        _insightProgress.value = 0f
        _scanRatePerMinute.value = 0f
        cachePersistJob?.cancel()
        cachePersistJob = null
        prefetchPersistJob?.cancel()
        prefetchPersistJob = null
        analysisCache.clear()
        attemptedPhotoIds.clear()
        warmedKeywordLangById.clear()
        recentKeywordFailures.clear()
        val appCtx = context ?: return
        scope.launch(Dispatchers.IO) {
            saveCache(appCtx)
            runCatching { File(appCtx.filesDir, ATTEMPTED_IDS_FILE).delete() }
        }
    }

    fun clearPersistentScanCaches(context: Context) {
        val appCtx = context.applicationContext
        stopScan()
        pauseRequests.set(0)
        _isPaused.value = false
        _progress.value = 0f
        _insightProgress.value = 0f
        _scanRatePerMinute.value = 0f
        scanStartTimeMs = 0L
        lastRateUpdateMs = 0L
        smoothedRatePerMinute = 0f
        appContext = appCtx
        cachePersistJob?.cancel()
        cachePersistJob = null
        prefetchPersistJob?.cancel()
        prefetchPersistJob = null
        analysisCache.clear()
        attemptedPhotoIds.clear()
        sizeCache.clear()
        videoThumbCache.clear()
        warmedKeywordLangById.clear()
        pendingKeywordJobs.values.forEach { job -> job.cancel() }
        pendingKeywordJobs.clear()
        recentKeywordFailures.clear()
        isCacheLoaded = false
        isPrefetchLoaded = false
        deletePersistentScanFile(appCtx, CACHE_FILE)
        deletePersistentScanFile(appCtx, ATTEMPTED_IDS_FILE)
        deletePersistentScanFile(appCtx, PREFETCH_CACHE_FILE)
    }

    /**
     * Shared best-effort metadata warmup for visible surfaces.
     * Global scan ownership uses [startInsightScan]; this path stays lightweight and on-demand.
     */
    suspend fun prefetchUnifiedInsights(
        context: Context,
        items: List<MediaItem>,
        languageCode: String?,
        includeKeywords: Boolean,
        maxCount: Int,
        timeBudgetMs: Long,
        respectGlobalPause: Boolean = true
    ): Map<Long, MotionPhotoHelper.MotionPhotoInfo> {
        if (items.isEmpty() || maxCount <= 0 || timeBudgetMs <= 0L) return emptyMap()

        val startedAt = System.currentTimeMillis()
        val normalizedLang = languageCode?.trim().orEmpty()
        val detectedMotion = LinkedHashMap<Long, MotionPhotoHelper.MotionPhotoInfo>()
        var processed = 0

        withContext(Dispatchers.IO) {
            for (item in items.asSequence().take(maxCount)) {
                if (processed >= maxCount) break
                if (System.currentTimeMillis() - startedAt >= timeBudgetMs) break
                while (pauseRequests.get() > 0 || (respectGlobalPause && AppPerformanceGovernor.shouldPauseNonCriticalWork())) {
                    delay(AppPerformanceGovernor.scaledDelay(350))
                }

                runCatching { ensurePrefetchForItem(context, item) }
                runCatching { upsertExifInsightIfMissing(context, item) }
                if (item.type == "photo" && !item.isLivePhoto) {
                    runCatching {
                        MotionPhotoHelper.detectAndPersist(context, item)
                    }.getOrNull()?.takeIf { it.isMotionPhoto }?.let { detectedMotion[item.id] = it }
                }

                if (includeKeywords && normalizedLang.isNotBlank() && item.type == "photo") {
                    val snapshot = MediaInsightRepository.get(context, item.id)
                    if (!hasCachedKeywordsForLanguage(snapshot, item.id, normalizedLang)) {
                        analyzeKeywordsShared(context, item, normalizedLang)
                    }
                }

                processed++
                delay(AppPerformanceGovernor.scaledDelay(18))
            }
        }
        return detectedMotion
    }

    /**
     * Independent fallback parse when unified cache is still missing.
     * This is lightweight and does not require the global scan to finish.
     */
    suspend fun temporaryResolveLocation(context: Context, item: MediaItem): String? {
        return withContext(Dispatchers.IO) {
            var latLonForCache: Pair<Double, Double>? = null
            val resolved = runCatching {
                if (item.type == "video") {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, item.uri)
                        val latLon = parseVideoLocationMetadataLocal(
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                        )
                        latLonForCache = latLon
                        if (latLon != null) {
                            val label = resolveLocationLabelLocal(context, latLon.first, latLon.second)
                            if (!label.equals("Unknown Location", ignoreCase = true)) {
                                label
                            } else {
                                "${String.format("%.4f", latLon.first)}, ${String.format("%.4f", latLon.second)}"
                            }
                        } else {
                            null
                        }
                    } finally {
                        runCatching { retriever.release() }
                    }
                } else {
                    openExifInputStream(context, item.uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val latLong = exif.latLong
                        if (latLong != null &&
                            (kotlin.math.abs(latLong[0]) > 0.0001 || kotlin.math.abs(latLong[1]) > 0.0001)
                        ) {
                            latLonForCache = latLong[0] to latLong[1]
                            val label = resolveLocationLabelLocal(context, latLong[0], latLong[1])
                            if (!label.equals("Unknown Location", ignoreCase = true)) {
                                label
                            } else {
                                "${String.format("%.4f", latLong[0])}, ${String.format("%.4f", latLong[1])}"
                            }
                        } else {
                            null
                        }
                    }
                }
            }.getOrNull()
            if (!resolved.isNullOrBlank() || latLonForCache != null) {
                MediaInsightRepository.upsertLocation(
                    context = context,
                    mediaId = item.id,
                    location = resolved,
                    latitude = latLonForCache?.first,
                    longitude = latLonForCache?.second
                )
            }
            resolved
        }
    }

    fun temporaryDetectMotion(context: Context, item: MediaItem): MotionPhotoHelper.MotionPhotoInfo {
        return MotionPhotoHelper.detectAndPersist(context, item)
    }

    /**
     * Temporary fallback for highly visible UI (detail/bottom panel):
     * run lightweight on-demand keyword extraction when unified cache is still empty.
     */
    suspend fun temporaryAnalyzeKeywords(
        context: Context,
        item: MediaItem,
        languageCode: String
    ): List<String> {
        if (item.type != "photo") return emptyList()
        val lang = languageCode.trim()
        if (lang.isBlank()) return emptyList()
        return analyzeKeywordsShared(context, item, lang)
    }

    suspend fun resolveKeywordsForUi(
        context: Context,
        item: MediaItem,
        languageCode: String
    ): List<String> {
        if (item.type != "photo") return emptyList()
        val lang = languageCode.trim()
        if (lang.isBlank()) return emptyList()

        val cached = MediaInsightRepository.get(context, item.id)
        val cachedKeywords = cached?.keywords.orEmpty()
        if (hasCachedKeywordsForLanguage(cached, item.id, lang)) {
            return cachedKeywords
        }
        return analyzeKeywordsShared(context, item, lang)
    }

    suspend fun resolveCardPreviewInsights(
        context: Context,
        item: MediaItem,
        timeBudgetMs: Long = 900L
    ): CardPreviewInsights {
        val motionDetected = prefetchUnifiedInsights(
            context = context,
            items = listOf(item),
            languageCode = null,
            includeKeywords = false,
            maxCount = 1,
            timeBudgetMs = timeBudgetMs
        )
        val insight = MediaInsightRepository.get(context, item.id)
        val motionInfo = if (item.type == "photo") {
            motionDetected[item.id] ?: temporaryDetectMotion(context, item)
        } else {
            null
        }
        val location = insight?.location
            ?: temporaryResolveLocation(context, item)
            ?: "Unknown Location"
        return CardPreviewInsights(
            location = location,
            motionInfo = motionInfo
        )
    }

    private suspend fun analyzeKeywordsShared(
        context: Context,
        item: MediaItem,
        languageCode: String
    ): List<String> {
        if (item.type != "photo") return emptyList()
        val lang = languageCode.trim()
        if (lang.isBlank()) return emptyList()

        val cached = MediaInsightRepository.get(context, item.id)
        val cachedKeywords = cached?.keywords.orEmpty()
        if (hasCachedKeywordsForLanguage(cached, item.id, lang)) {
            clearKeywordFailure(item, lang)
            return cachedKeywords
        }

        if (shouldSkipKeywordAnalysisForFailure(item, lang)) {
            return emptyList()
        }

        val jobKey = buildKeywordJobKey(item.id, lang)
        val existingJob = pendingKeywordJobs[jobKey]
        if (existingJob != null) {
            return runCatching { existingJob.await() }.getOrDefault(emptyList())
        }

        val newJob = scope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                val analysis = runCatching { ImageAnalyzer.analyzeDetailed(context, item.uri, lang) }
                    .getOrDefault(ImageAnalyzer.AnalyzeResult(emptyList()))
                val labels = analysis.labels
                if (labels.isNotEmpty()) {
                    clearKeywordFailure(item, lang)
                    MediaInsightRepository.upsertKeywords(context, item.id, labels, lang)
                    warmedKeywordLangById[item.id] = lang
                    pruneWarmedKeywordLangCacheIfNeeded()
                } else if (analysis.failureKind != null) {
                    recordKeywordFailure(item, lang, analysis.failureKind)
                } else {
                    clearKeywordFailure(item, lang)
                }
                labels
            } finally {
                pendingKeywordJobs.remove(jobKey)
            }
        }

        val activeJob = pendingKeywordJobs.putIfAbsent(jobKey, newJob) ?: newJob
        if (activeJob === newJob) {
            newJob.start()
        } else {
            newJob.cancel()
        }

        return runCatching { activeJob.await() }.getOrDefault(emptyList())
    }

    private fun isCoordinateText(text: String): Boolean {
        val match = Regex("""\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*""").matchEntire(text) ?: return false
        val lat = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return false
        val lon = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return false
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }

    private suspend fun ensurePrefetchForItem(context: Context, item: MediaItem): Boolean {
        var updated = false
        if (!sizeCache.containsKey(item.id)) {
            val computed = computeMediaSizeBytes(context, item)
            sizeCache[item.id] = if (computed > 0) computed else item.size
            updated = true
        }
        if (item.type == "video" && !videoThumbCache.containsKey(item.id)) {
            val ok = prefetchVideoThumb(context, item)
            if (ok) {
                videoThumbCache[item.id] = true
                updated = true
            }
        }
        return updated
    }

    private suspend fun computeMediaSizeBytes(context: Context, item: MediaItem): Long {
        return withContext(Dispatchers.IO) {
            if (item.type != "photo") return@withContext item.size
            if (!item.isLivePhoto) return@withContext item.size
            try {
                context.contentResolver.query(
                    item.uri,
                    arrayOf(android.provider.MediaStore.Images.Media.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idx = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.SIZE)
                    if (cursor.moveToFirst()) cursor.getLong(idx) else item.size
                } ?: item.size
            } catch (_: Exception) {
                item.size
            }
        }
    }

    private suspend fun prefetchVideoThumb(context: Context, item: MediaItem): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val conservative = MemorySafetyMode.shouldReduceBackgroundWork(context)
                val request = ImageRequest.Builder(context)
                    .data(item.uri)
                    .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                    .size(256)
                    .memoryCacheKey("video_thumb_${item.id}_list")
                    .diskCacheKey("video_thumb_${item.id}_list")
                    .memoryCachePolicy(if (conservative) CachePolicy.DISABLED else CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                result is SuccessResult
            } catch (_: OutOfMemoryError) {
                // Skip this item and keep scan alive under memory pressure.
                activateConservativeScanMode(context, "prefetch_video_thumb")
                false
            } catch (_: Exception) {
                false
            }
        }
    }

    private suspend fun analyzeItem(context: Context, item: MediaItem): AnalysisResult? {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            var scaled100: Bitmap? = null
            var scaled9x8: Bitmap? = null
            var scaled32: Bitmap? = null

            try {
                // Decode with real bounds every time so very large photos do not inflate heap unexpectedly.
                bitmap = decodeBitmapForAnalysis(context, item)
                val decoded = bitmap ?: return@withContext null

                // 2. Compute dHash (Requires 9x8)
                scaled9x8 = Bitmap.createScaledBitmap(decoded, 9, 8, true)
                val dHash = dHash(scaled9x8 ?: return@withContext null)

                // 3. Compute pHash (32x32 grayscale DCT)
                scaled32 = Bitmap.createScaledBitmap(decoded, 32, 32, true)
                val pHash = pHash(scaled32 ?: return@withContext null)

                // 4. Compute Stats (Requires approx 100x100 for speed)
                scaled100 = Bitmap.createScaledBitmap(decoded, 100, 100, true)
                val statsBitmap = scaled100 ?: return@withContext null
                val luminanceStats = getLuminanceStats(statsBitmap)
                val blurVar = getBlurVariance(statsBitmap)

                AnalysisResult(
                    id = item.id,
                    dHash = dHash,
                    pHash = pHash,
                    blurVariance = blurVar,
                    exposureAvg = luminanceStats.average,
                    solidColorStdDev = luminanceStats.stdDev,
                    shadowClipRatio = luminanceStats.shadowClipRatio,
                    highlightClipRatio = luminanceStats.highlightClipRatio,
                    qualityMetricsVersion = QUALITY_METRICS_VERSION,
                    faceCount = 0,
                    maxFaceSize = 0f,
                    hasClosedEyes = false,
                    isMotionPhoto = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } catch (_: OutOfMemoryError) {
                // Protect deep-organize scan from process crashes on huge files.
                activateConservativeScanMode(context, "analyze_item")
                null
            } finally {
                scaled9x8?.recycle()
                scaled32?.recycle()
                scaled100?.recycle()
                bitmap?.recycle()
            }
        }
    }

    private suspend fun computeSimilarityHashes(context: Context, item: MediaItem): Pair<Long, Long>? {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            var scaled9x8: Bitmap? = null
            var scaled32: Bitmap? = null

            try {
                bitmap = decodeBitmapForAnalysis(context, item)
                val decoded = bitmap ?: return@withContext null

                scaled9x8 = Bitmap.createScaledBitmap(decoded, 9, 8, true)
                scaled32 = Bitmap.createScaledBitmap(decoded, 32, 32, true)
                dHash(scaled9x8 ?: return@withContext null) to
                    pHash(scaled32 ?: return@withContext null)
            } catch (_: Exception) {
                null
            } catch (_: OutOfMemoryError) {
                activateConservativeScanMode(context, "similarity_hash")
                null
            } finally {
                scaled9x8?.recycle()
                scaled32?.recycle()
                bitmap?.recycle()
            }
        }
    }

    private data class LuminanceStats(
        val average: Int,
        val stdDev: Double,
        val shadowClipRatio: Float,
        val highlightClipRatio: Float
    )

    private fun computeAnalyzeInSampleSize(width: Int, height: Int, maxSidePx: Int, maxPixels: Int): Int {
        var sample = 1
        while (true) {
            val sampledWidth = (width / sample).coerceAtLeast(1)
            val sampledHeight = (height / sample).coerceAtLeast(1)
            val maxSide = maxOf(sampledWidth, sampledHeight)
            val pixels = sampledWidth.toLong() * sampledHeight.toLong()
            val tooLargeSide = maxSide > maxSidePx
            val tooLargePixels = pixels > maxPixels
            if (!tooLargeSide && !tooLargePixels) break
            sample *= 2
            if (sample >= 64) break
        }
        return sample.coerceAtLeast(1)
    }

    private fun currentDecodeConfig(context: Context): DecodeConfig {
        val conservative =
            MemorySafetyMode.shouldUseConservativeMemory(context) ||
                AppPerformanceGovernor.isMemoryPressureHigh()
        return if (conservative) {
            DecodeConfig(
                maxSidePx = CONSERVATIVE_ANALYZE_TARGET_MAX_SIDE_PX,
                maxPixels = CONSERVATIVE_ANALYZE_TARGET_MAX_PIXELS,
                preferredConfig = Bitmap.Config.RGB_565
            )
        } else {
            DecodeConfig(
                maxSidePx = ANALYZE_TARGET_MAX_SIDE_PX,
                maxPixels = ANALYZE_TARGET_MAX_PIXELS,
                preferredConfig = Bitmap.Config.ARGB_8888
            )
        }
    }

    private suspend fun decodeBitmapForAnalysis(context: Context, item: MediaItem): Bitmap? {
        return withContext(Dispatchers.IO) {
            val config = currentDecodeConfig(context)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val options = BitmapFactory.Options().apply {
                inSampleSize = computeAnalyzeInSampleSize(
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    maxSidePx = config.maxSidePx,
                    maxPixels = config.maxPixels
                )
                inJustDecodeBounds = false
                inPreferredConfig = config.preferredConfig
                inDither = config.preferredConfig == Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }
    }

    private fun effectiveAnalysisParallelism(context: Context, availableCores: Int): Int {
        val base = AppPerformanceGovernor.analysisParallelism(availableCores)
        return if (MemorySafetyMode.shouldReduceBackgroundWork(context)) {
            1
        } else {
            base.coerceAtLeast(1)
        }
    }

    private fun effectiveAnalysisBatchSize(context: Context, parallelism: Int): Int {
        return if (MemorySafetyMode.shouldReduceBackgroundWork(context)) {
            1
        } else {
            (parallelism * 3).coerceIn(2, 8)
        }
    }

    private fun shouldPrefetchInGlobalScan(item: MediaItem): Boolean {
        return item.type == "photo" && item.isLivePhoto && !sizeCache.containsKey(item.id)
    }

    private suspend fun ensureGlobalScanPrefetchForItem(context: Context, item: MediaItem): Boolean {
        if (!shouldPrefetchInGlobalScan(item)) return false
        var updated = false
        if (!sizeCache.containsKey(item.id)) {
            val computed = computeMediaSizeBytes(context, item)
            sizeCache[item.id] = if (computed > 0) computed else item.size
            updated = true
        }
        return updated
    }

    private fun settledProgressFor(photos: List<MediaItem>): Float {
        if (photos.isEmpty()) return 1f
        return (photos.count { isPhotoSettled(it.id) }.toFloat() / photos.size.toFloat()).coerceIn(0f, 1f)
    }

    private fun relieveMemoryPressure(context: Context, aggressive: Boolean) {
        runCatching { context.imageLoader.memoryCache?.clear() }
        trimTransientCaches()
        SimilarPhotoManager.trimTransientCaches()
        if (aggressive) {
            runCatching { System.gc() }
        }
    }

    private fun activateConservativeScanMode(
        context: Context,
        source: String,
        throwable: Throwable? = null
    ) {
        MemorySafetyMode.activateOomFallback(context)
        AppPerformanceGovernor.onCaughtOutOfMemory()
        relieveMemoryPressure(context, aggressive = true)
        if (throwable != null) {
            AppRuntimeLogStore.logError(context, "MediaAnalysis/$source", "Scan OOM fallback activated", throwable)
        }
    }

    private fun deletePersistentScanFile(context: Context, fileName: String) {
        runCatching { File(context.filesDir, fileName).delete() }
        runCatching { File(context.filesDir, "$fileName.tmp").delete() }
    }

    // --- Helpers ---

    private fun dHash(bitmap: Bitmap): Long {
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = bitmap.getPixel(x, y)
                val right = bitmap.getPixel(x + 1, y)
                val leftGray = (android.graphics.Color.red(left) + android.graphics.Color.green(left) + android.graphics.Color.blue(left)) / 3
                val rightGray = (android.graphics.Color.red(right) + android.graphics.Color.green(right) + android.graphics.Color.blue(right)) / 3
                
                if (leftGray > rightGray) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }
        return hash
    }

    private fun pHash(bitmap: Bitmap): Long {
        val width = bitmap.width
        val height = bitmap.height
        if (width != 32 || height != 32) return 0L

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val values = Array(height) { DoubleArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                values[y][x] = (0.299 * r) + (0.587 * g) + (0.114 * b)
            }
        }

        val coeffs = Array(8) { DoubleArray(8) }
        for (u in 0 until 8) {
            for (v in 0 until 8) {
                var sum = 0.0
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        sum += values[y][x] *
                            kotlin.math.cos(((2 * x + 1) * u * Math.PI) / 64.0) *
                            kotlin.math.cos(((2 * y + 1) * v * Math.PI) / 64.0)
                    }
                }
                val alphaU = if (u == 0) 1.0 / kotlin.math.sqrt(2.0) else 1.0
                val alphaV = if (v == 0) 1.0 / kotlin.math.sqrt(2.0) else 1.0
                coeffs[u][v] = 0.25 * alphaU * alphaV * sum
            }
        }

        val lowFreq = DoubleArray(63)
        var idx = 0
        for (u in 0 until 8) {
            for (v in 0 until 8) {
                if (u == 0 && v == 0) continue
                lowFreq[idx++] = coeffs[u][v]
            }
        }
        val median = lowFreq.sorted()[lowFreq.size / 2]

        var hash = 0L
        var bit = 0
        for (u in 0 until 8) {
            for (v in 0 until 8) {
                if (u == 0 && v == 0) continue
                if (coeffs[u][v] > median) {
                    hash = hash or (1L shl bit)
                }
                bit++
            }
        }
        // Use the top bit for DC-vs-median sign to preserve 64 total bits.
        if (coeffs[0][0] > median) {
            hash = hash or (1L shl 63)
        }
        return hash
    }

    private fun getLuminanceStats(bitmap: Bitmap): LuminanceStats {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val count = pixels.size.coerceAtLeast(1)
        var sum = 0L
        var sqSum = 0L
        var shadowPixels = 0
        var highlightPixels = 0

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            sum += luminance
            sqSum += luminance.toLong() * luminance.toLong()
            if (luminance <= 24) shadowPixels++
            if (luminance >= 245) highlightPixels++
        }

        val mean = sum.toDouble() / count.toDouble()
        val variance = ((sqSum.toDouble() / count.toDouble()) - (mean * mean)).coerceAtLeast(0.0)
        return LuminanceStats(
            average = mean.toInt(),
            stdDev = sqrt(variance),
            shadowClipRatio = shadowPixels.toFloat() / count.toFloat(),
            highlightClipRatio = highlightPixels.toFloat() / count.toFloat()
        )
    }

    private fun getAverageLuminance(bitmap: Bitmap): Int {
        var totalLum = 0L
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalLum += (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        return (totalLum / pixels.size).toInt()
    }
    
    private fun getSolidColorStdDev(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var sum = 0L
        var sqSum = 0L
        val count = pixels.size

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            sum += lum
            sqSum += (lum * lum).toLong()
        }

        val mean = sum.toDouble() / count
        val variance = (sqSum.toDouble() / count) - (mean * mean)
        return sqrt(variance)
    }

    private fun getBlurVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        
        var variance = 0.0
        var mean = 0.0
        var count = 0
        val laplacian = IntArray((width - 2) * (height - 2))
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val val0 = gray[idx]
                val val1 = gray[idx - 1]
                val val2 = gray[idx + 1]
                val val3 = gray[idx - width]
                val val4 = gray[idx + width]
                
                val lap = val1 + val2 + val3 + val4 - 4 * val0
                laplacian[count++] = lap
                mean += lap
            }
        }
        
        mean /= count
        for (l in laplacian) {
            variance += (l - mean) * (l - mean)
        }
        return variance / count
    }

    private fun upsertExifInsightIfMissing(context: Context, item: MediaItem) {
        val cached = MediaInsightRepository.get(context, item.id)
        val hasCachedCoordinates = cached?.latitude != null && cached.longitude != null
        val hasValidCachedLocation = cached?.location
            ?.trim()
            ?.let { it.isNotBlank() && !it.equals("Unknown Location", ignoreCase = true) }
            ?: false
        val hasValidCachedCamera = cached?.camera
            ?.trim()
            ?.let { it.isNotBlank() && it != "--" }
            ?: false
        val cameraAndLocationSettled = hasCachedCoordinates && hasValidCachedLocation && hasValidCachedCamera
        val isVideo = item.type == "video"
        // Photos settled before shooting tags existed get revisited exactly once for them.
        val shootingSettled = isVideo || cached?.shootingScanned == true
        if (cameraAndLocationSettled && shootingSettled) return

        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, item.uri)
                val latLon = parseVideoLocationMetadataLocal(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                )
                val location = resolveLocationLabelLocal(context, latLon?.first, latLon?.second)
                MediaInsightRepository.upsertExif(
                    context = context,
                    mediaId = item.id,
                    camera = null,
                    location = location,
                    latitude = latLon?.first,
                    longitude = latLon?.second,
                    metadataScanned = true
                )
            } finally {
                runCatching { retriever.release() }
            }
            return
        }

        openExifInputStream(context, item.uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val shooting = readExifShootingValues(exif)
            if (cameraAndLocationSettled) {
                // Camera + location already good: only add the shooting tags, never re-geocode.
                MediaInsightRepository.upsertShooting(context, item.id, shooting, metadataScanned = true)
                return@use
            }
            val cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
            val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
            val latLong = exif.latLong
            val hasCoord = latLong != null &&
                (kotlin.math.abs(latLong[0]) > 0.0001 || kotlin.math.abs(latLong[1]) > 0.0001)
            val camera = if (cameraMake.isNotEmpty() || cameraModel.isNotEmpty()) {
                "$cameraMake $cameraModel".trim()
            } else {
                "--"
            }
            val location = if (hasCoord) {
                val label = resolveLocationLabelLocal(context, latLong!![0], latLong[1])
                if (!label.equals("Unknown Location", ignoreCase = true)) {
                    label
                } else {
                    "Unknown Location"
                }
            } else {
                "Unknown Location"
            }
            MediaInsightRepository.upsertExif(
                context = context,
                mediaId = item.id,
                // Don't let a placeholder overwrite a value another path already resolved.
                camera = camera.takeUnless { it == "--" && hasValidCachedCamera },
                location = location.takeUnless {
                    it.equals("Unknown Location", ignoreCase = true) && hasValidCachedLocation
                },
                latitude = if (hasCoord) latLong?.get(0) else null,
                longitude = if (hasCoord) latLong?.get(1) else null,
                focal35 = shooting.focal35,
                aperture = shooting.aperture,
                iso = shooting.iso,
                exposure = shooting.exposure,
                shootingScanned = true,
                metadataScanned = true
            )
        }
    }

    private fun parseVideoLocationMetadataLocal(raw: String?): Pair<Double, Double>? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        val match = Regex("([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?).*").find(text) ?: return null
        val lat = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return null
        val lon = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    private fun resolveLocationLabelLocal(context: Context, lat: Double?, lon: Double?): String {
        return LocationLabelResolver.resolve(context, lat, lon, fallbackToCoordinates = false)
    }

    private fun hasSettledInsightForScan(
        item: MediaItem,
        snapshot: MediaInsightSnapshot?
    ): Boolean {
        if (snapshot == null) return false
        return when (item.type) {
            "photo" -> snapshot.shootingScanned &&
                !snapshot.camera.isNullOrBlank() &&
                !snapshot.location.isNullOrBlank()
            "video" -> !snapshot.location.isNullOrBlank()
            else -> true
        }
    }
}
