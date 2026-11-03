package com.voicelike.app

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

data class MediaInsightSnapshot(
    val camera: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val keywords: List<String> = emptyList(),
    val keywordLanguage: String? = null,
    /** EXIF FocalLengthIn35mmFilm only (mm); null when absent. */
    val focal35: Float? = null,
    /** EXIF FNumber; null when absent. */
    val aperture: Float? = null,
    /** First EXIF ISO / PhotographicSensitivity value; null when absent. */
    val iso: Int? = null,
    /** EXIF ExposureTime in seconds; null when absent. */
    val exposure: Double? = null,
    /** The four shooting tags were read for this photo (even if all were absent). */
    val shootingScanned: Boolean = false,
    /** Camera + location EXIF read was attempted for this item. */
    val metadataScanned: Boolean = false
)

/** Shooting parameters read from one EXIF pass; every field already sanitized. */
internal data class ExifShootingValues(
    val focal35: Float?,
    val aperture: Float?,
    val iso: Int?,
    val exposure: Double?
)

/** Reads FocalLengthIn35mmFilm / FNumber / ISO / ExposureTime, dropping zero, negative and absurd values. */
internal fun readExifShootingValues(exif: ExifInterface): ExifShootingValues {
    val focal35 = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
        .takeIf { it in 1..5000 }
        ?.toFloat()
    val aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, -1.0)
        .takeIf { it.isFinite() && it in 0.5..128.0 }
        ?.toFloat()
    val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
        ?.substringBefore(',')
        ?.trim()
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 1.0 && it <= 10_000_000.0 }
        ?.toInt()
    val exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, -1.0)
        .takeIf { it.isFinite() && it > 0.0 && it <= 3600.0 }
    return ExifShootingValues(focal35 = focal35, aperture = aperture, iso = iso, exposure = exposure)
}

/**
 * Opens a stream suitable for EXIF parsing. On Android 10+ a plain MediaStore uri has its GPS
 * tags redacted unless the original is requested, which needs ACCESS_MEDIA_LOCATION. Only
 * MediaStore uris are upgraded; anything else (SAF documents, file uris, RAW from other providers)
 * and any refusal falls back to the plain uri.
 */
internal fun openExifInputStream(context: Context, uri: Uri): InputStream? {
    val resolver = context.contentResolver
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        uri.scheme == ContentResolver.SCHEME_CONTENT &&
        uri.authority == MediaStore.AUTHORITY &&
        uri.getQueryParameter("requireOriginal") == null &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    ) {
        val original = try {
            resolver.openInputStream(MediaStore.setRequireOriginal(uri))
        } catch (_: SecurityException) {
            null
        } catch (_: UnsupportedOperationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: FileNotFoundException) {
            null
        }
        if (original != null) return original
    }
    return resolver.openInputStream(uri)
}

object MediaInsightRepository {
    private const val PREFS_NAME = "photoo_prefs"
    private const val KEY_INSIGHTS = "media_insight_cache_v1"
    private const val CACHE_FILE_NAME = "media_insight_cache_v3.json"
    // Raised from 16MB when shooting fields were added so large libraries don't trip the guard.
    private const val MAX_CACHE_FILE_BYTES = 24L * 1024L * 1024L
    private const val SCAN_FLAG_SHOOTING = 1
    private const val SCAN_FLAG_METADATA = 2
    private const val PERSIST_DEBOUNCE_MS = 1200L
    private const val PERSIST_MIN_INTERVAL_MS = 2000L
    private const val PERSIST_DIRTY_THRESHOLD = 24

    private val gson = Gson()
    private val lock = Any()

    @Volatile
    private var loaded = false

    private val cache = java.util.concurrent.ConcurrentHashMap<Long, MediaInsightSnapshot>()
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dirtyCount = 0
    private var lastPersistAtMs = 0L
    private var persistJob: Job? = null
    private val _version = MutableStateFlow(0L)
    val version = _version.asStateFlow()
    private val itemVersions = java.util.concurrent.ConcurrentHashMap<Long, MutableStateFlow<Long>>()
    private val itemVersionObserverCounts = HashMap<Long, Int>()
    private var lastVersionEmitAtMs = 0L
    private val prewarmInFlight = AtomicBoolean(false)

    private data class InsightPersistEntry(
        val id: Long,
        val camera: String?,
        val location: String?,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val keywords: List<String>?,
        val keywordLanguage: String? = null,
        // Added later: short keys, null/absent when unknown (Gson skips nulls), so old files load
        // unchanged and the file grows as little as possible.
        @SerializedName("f35") val focal35: Float? = null,
        @SerializedName("ap") val aperture: Float? = null,
        @SerializedName("iso") val iso: Int? = null,
        @SerializedName("exp") val exposure: Float? = null,
        @SerializedName("sc") val scanFlags: Int? = null
    )

    private fun MediaInsightSnapshot.toPersistEntry(id: Long): InsightPersistEntry {
        val flags = (if (shootingScanned) SCAN_FLAG_SHOOTING else 0) or
            (if (metadataScanned) SCAN_FLAG_METADATA else 0)
        return InsightPersistEntry(
            id = id,
            camera = camera,
            location = location,
            latitude = latitude,
            longitude = longitude,
            keywords = keywords,
            keywordLanguage = keywordLanguage,
            focal35 = focal35?.takeIf { it.isFinite() },
            aperture = aperture?.takeIf { it.isFinite() },
            iso = iso,
            exposure = exposure?.takeIf { it.isFinite() }?.toFloat(),
            scanFlags = flags.takeIf { it != 0 }
        )
    }

    private fun InsightPersistEntry.toSnapshot(): MediaInsightSnapshot {
        // Gson bypasses Kotlin defaults, so any field may come back null from old files.
        val safeKeywords: List<String> = keywords ?: emptyList()
        val flags = scanFlags ?: 0
        return MediaInsightSnapshot(
            camera = camera,
            location = location,
            latitude = latitude,
            longitude = longitude,
            keywords = safeKeywords,
            keywordLanguage = keywordLanguage,
            focal35 = focal35?.takeIf { it.isFinite() && it > 0f },
            aperture = aperture?.takeIf { it.isFinite() && it > 0f },
            iso = iso?.takeIf { it > 0 },
            // Float -> String -> Double keeps 1/120 as 0.008333334 instead of 0.00833333376...
            exposure = exposure?.takeIf { it.isFinite() && it > 0f }?.toString()?.toDoubleOrNull(),
            shootingScanned = (flags and SCAN_FLAG_SHOOTING) != 0,
            // Entries written before the flag existed only got camera/location from an EXIF read.
            metadataScanned = (flags and SCAN_FLAG_METADATA) != 0 || camera != null || location != null
        )
    }

    private fun markChangedLocked() {
        dirtyCount++
        val now = System.currentTimeMillis()
        if (now - lastVersionEmitAtMs >= 350L || dirtyCount % 12 == 0) {
            _version.value = _version.value + 1L
            lastVersionEmitAtMs = now
        }
    }

    private fun markItemChangedLocked(mediaId: Long) {
        itemVersions[mediaId]?.let { flow ->
            flow.value = flow.value + 1L
        }
        markChangedLocked()
    }

    private fun buildPersistEntriesLocked(now: Long): List<InsightPersistEntry>? {
        if (dirtyCount == 0) return null
        val snapshot = cache.entries.map { (id, value) -> id to value }
        dirtyCount = 0
        lastPersistAtMs = now
        return snapshot.map { (id, snapshot) -> snapshot.toPersistEntry(id) }
    }

    private fun schedulePersist(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (persistJob?.isActive == true) return
            persistJob = persistScope.launch {
                while (true) {
                    val waitMs = synchronized(lock) {
                        if (dirtyCount == 0) {
                            persistJob = null
                            return@launch
                        }
                        val sinceLastPersist = System.currentTimeMillis() - lastPersistAtMs
                        val requiredGap = if (dirtyCount >= PERSIST_DIRTY_THRESHOLD) {
                            0L
                        } else {
                            PERSIST_MIN_INTERVAL_MS - sinceLastPersist
                        }
                        maxOf(PERSIST_DEBOUNCE_MS, requiredGap).coerceAtLeast(0L)
                    }
                    if (waitMs > 0L) delay(waitMs)

                    val entries = synchronized(lock) {
                        buildPersistEntriesLocked(System.currentTimeMillis())
                    } ?: continue

                    val success = runCatching {
                        writeEntriesToFile(appContext, entries)
                    }.isSuccess

                    if (!success) {
                        if (AppPerformanceGovernor.isRealtimeMemoryPressureHigh()) {
                            runCatching { System.gc() }
                        }
                        synchronized(lock) {
                            markChangedLocked()
                            lastPersistAtMs = 0L
                        }
                    }
                }
            }
        }
    }

    private fun persistNowBlocking(context: Context) {
        val appContext = context.applicationContext
        runBlocking(Dispatchers.IO) {
            val pendingJob = synchronized(lock) {
                persistJob?.also { persistJob = null }
            }
            pendingJob?.cancel()

            while (true) {
                val entries = synchronized(lock) {
                    buildPersistEntriesLocked(System.currentTimeMillis())
                } ?: return@runBlocking

                val success = runCatching {
                    writeEntriesToFile(appContext, entries)
                }.isSuccess

                if (success) {
                    val hasPendingChanges = synchronized(lock) { dirtyCount > 0 }
                    if (!hasPendingChanges) return@runBlocking
                } else {
                    if (AppPerformanceGovernor.isRealtimeMemoryPressureHigh()) {
                        runCatching { System.gc() }
                    }
                    synchronized(lock) {
                        markChangedLocked()
                        lastPersistAtMs = 0L
                    }
                    delay(160L)
                }
            }
        }
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            runCatching {
                val fileEntries = readEntriesFromFile(context)
                val entries = if (fileEntries != null) {
                    fileEntries
                } else {
                    val migrated = readEntriesFromPrefs(context)
                    if (migrated.isNotEmpty()) {
                        writeEntriesToFile(context, migrated)
                    }
                    migrated
                }
                cache.clear()
                entries.forEach { entry ->
                    cache[entry.id] = entry.toSnapshot()
                }
            }
            _version.value = _version.value + 1L
            loaded = true
        }
    }

    private fun persist(context: Context, force: Boolean) {
        if (force) {
            persistNowBlocking(context)
        } else {
            schedulePersist(context)
        }
    }

    private fun readEntriesFromFile(context: Context): List<InsightPersistEntry>? {
        val file = File(context.filesDir, CACHE_FILE_NAME)
        if (!file.exists()) return null
        if (file.length() > MAX_CACHE_FILE_BYTES) {
            runCatching { file.delete() }
            return emptyList()
        }
        val type = object : TypeToken<List<InsightPersistEntry>>() {}.type
        return file.reader().buffered().use { reader ->
            gson.fromJson<List<InsightPersistEntry>>(reader, type) ?: emptyList()
        }
    }

    private fun readEntriesFromPrefs(context: Context): List<InsightPersistEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_INSIGHTS, null) ?: return emptyList()
        val type = object : TypeToken<List<InsightPersistEntry>>() {}.type
        return gson.fromJson<List<InsightPersistEntry>>(json, type) ?: emptyList()
    }

    private fun writeEntriesToFile(context: Context, entries: List<InsightPersistEntry>) {
        val file = File(context.filesDir, CACHE_FILE_NAME)
        val tmp = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
        try {
            writeEntriesStreaming(tmp, entries)
            if (!tmp.renameTo(file)) {
                writeEntriesStreaming(file, entries)
                runCatching { tmp.delete() }
            }
        } catch (oom: OutOfMemoryError) {
            MemorySafetyMode.markOomCrash(context)
            AppPerformanceGovernor.onCaughtOutOfMemory()
            runCatching { tmp.delete() }
            throw oom
        }
    }

    private fun writeEntriesStreaming(file: File, entries: List<InsightPersistEntry>) {
        file.writer().buffered().use { writer ->
            gson.toJson(entries, writer)
        }
    }

    fun prewarm(context: Context) {
        if (loaded) return
        if (!prewarmInFlight.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        Thread {
            try {
                ensureLoaded(appContext)
            } finally {
                prewarmInFlight.set(false)
            }
        }.start()
    }

    fun snapshot(context: Context): Map<Long, MediaInsightSnapshot> {
        ensureLoaded(context)
        return java.util.concurrent.ConcurrentHashMap(cache)
    }

    fun get(context: Context, mediaId: Long): MediaInsightSnapshot? {
        ensureLoaded(context)
        return cache[mediaId]
    }

    fun versionFor(mediaId: Long): StateFlow<Long> {
        val flow = itemVersions.getOrPut(mediaId) { MutableStateFlow(0L) }
        return flow.asStateFlow()
    }

    fun acquireVersionFor(mediaId: Long): StateFlow<Long> {
        synchronized(lock) {
            val flow = itemVersions.getOrPut(mediaId) { MutableStateFlow(0L) }
            itemVersionObserverCounts[mediaId] = (itemVersionObserverCounts[mediaId] ?: 0) + 1
            return flow.asStateFlow()
        }
    }

    fun releaseVersionObserver(mediaId: Long) {
        synchronized(lock) {
            val currentCount = itemVersionObserverCounts[mediaId] ?: return
            if (currentCount <= 1) {
                itemVersionObserverCounts.remove(mediaId)
                itemVersions.remove(mediaId)
            } else {
                itemVersionObserverCounts[mediaId] = currentCount - 1
            }
        }
    }

    fun getMany(context: Context, mediaIds: Collection<Long>): Map<Long, MediaInsightSnapshot> {
        ensureLoaded(context)
        if (mediaIds.isEmpty()) return emptyMap()
        val result = LinkedHashMap<Long, MediaInsightSnapshot>(mediaIds.size)
        mediaIds.forEach { id ->
            cache[id]?.let { result[id] = it }
        }
        return result
    }

    fun upsertKeywords(context: Context, mediaId: Long, keywords: List<String>, languageCode: String? = null) {
        ensureLoaded(context)
        synchronized(lock) {
            val existing = cache[mediaId] ?: MediaInsightSnapshot()
            val normalized = keywords.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val normalizedLanguage = languageCode?.trim()?.takeIf { it.isNotBlank() } ?: existing.keywordLanguage
            if (normalized.isNotEmpty()) {
                if (existing.keywords != normalized || existing.keywordLanguage != normalizedLanguage) {
                    cache[mediaId] = existing.copy(
                        keywords = normalized,
                        keywordLanguage = normalizedLanguage
                    )
                    markItemChangedLocked(mediaId)
                }
            } else if (existing.keywords.isEmpty()) {
                cache[mediaId] = existing
            }
        }
        persist(context, force = false)
    }

    fun upsertExif(
        context: Context,
        mediaId: Long,
        camera: String?,
        location: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        focal35: Float? = null,
        aperture: Float? = null,
        iso: Int? = null,
        exposure: Double? = null,
        shootingScanned: Boolean = false,
        metadataScanned: Boolean = false
    ) {
        ensureLoaded(context)
        val parsed = parseLatLonTextLocal(location)
        val resolvedLat = when {
            latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0 -> latitude
            parsed != null -> parsed.first
            else -> null
        }
        val resolvedLon = when {
            latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0 -> longitude
            parsed != null -> parsed.second
            else -> null
        }
        synchronized(lock) {
            val existing = cache[mediaId] ?: MediaInsightSnapshot()
            val next = existing.copy(
                camera = camera ?: existing.camera,
                location = location ?: existing.location,
                latitude = resolvedLat ?: existing.latitude,
                longitude = resolvedLon ?: existing.longitude,
                focal35 = focal35?.takeIf { it.isFinite() && it > 0f } ?: existing.focal35,
                aperture = aperture?.takeIf { it.isFinite() && it > 0f } ?: existing.aperture,
                iso = iso?.takeIf { it > 0 } ?: existing.iso,
                exposure = exposure?.takeIf { it.isFinite() && it > 0.0 } ?: existing.exposure,
                shootingScanned = shootingScanned || existing.shootingScanned,
                metadataScanned = metadataScanned || existing.metadataScanned
            )
            if (next != existing) {
                cache[mediaId] = next
                markItemChangedLocked(mediaId)
            }
        }
        persist(context, force = false)
    }

    /** Writes only the shooting tags (and marks them read) without touching camera/location. */
    internal fun upsertShooting(
        context: Context,
        mediaId: Long,
        values: ExifShootingValues,
        metadataScanned: Boolean = false
    ) {
        upsertExif(
            context = context,
            mediaId = mediaId,
            camera = null,
            location = null,
            focal35 = values.focal35,
            aperture = values.aperture,
            iso = values.iso,
            exposure = values.exposure,
            shootingScanned = true,
            metadataScanned = metadataScanned
        )
    }

    fun upsertLocation(
        context: Context,
        mediaId: Long,
        location: String?,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        ensureLoaded(context)
        val normalizedLocation = location?.trim()
        val parsed = parseLatLonTextLocal(normalizedLocation)
        val explicitCoords = if (
            latitude != null &&
            longitude != null &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0
        ) {
            latitude to longitude
        } else {
            null
        }
        val resolvedCoords = explicitCoords ?: parsed
        val humanFromParsed = parsed?.let {
            val human = resolveLocationLabel(context, it.first, it.second)
            human.takeIf { label -> !label.equals("Unknown Location", ignoreCase = true) }
        }
        synchronized(lock) {
            val existing = cache[mediaId] ?: MediaInsightSnapshot()
            val resolvedLocation = when {
                normalizedLocation.isNullOrBlank() -> existing.location
                parsed != null -> humanFromParsed ?: existing.location ?: normalizedLocation
                else -> normalizedLocation
            }
            val next = existing.copy(
                location = resolvedLocation,
                latitude = resolvedCoords?.first ?: existing.latitude,
                longitude = resolvedCoords?.second ?: existing.longitude
            )
            if (next != existing) {
                cache[mediaId] = next
                markItemChangedLocked(mediaId)
            }
        }
        persist(context, force = false)
    }

    fun clearExifFor(context: Context, mediaIds: Collection<Long>) {
        ensureLoaded(context)
        if (mediaIds.isEmpty()) return
        synchronized(lock) {
            mediaIds.forEach { mediaId ->
                val existing = cache[mediaId] ?: return@forEach
                val next = existing.copy(
                    camera = null,
                    location = null,
                    latitude = null,
                    longitude = null,
                    focal35 = null,
                    aperture = null,
                    iso = null,
                    exposure = null,
                    shootingScanned = false,
                    metadataScanned = false
                )
                if (next.keywords.isEmpty() && next.keywordLanguage.isNullOrBlank()) {
                    if (cache.remove(mediaId) != null) {
                        markItemChangedLocked(mediaId)
                    }
                } else if (next != existing) {
                    cache[mediaId] = next
                    markItemChangedLocked(mediaId)
                }
            }
        }
        persist(context, force = false)
    }

    fun flush(context: Context) {
        ensureLoaded(context)
        persist(context, force = true)
    }
}
