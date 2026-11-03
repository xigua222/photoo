package com.voicelike.app

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

internal object MediaCatalogSnapshotCache {
    private const val CACHE_FILE_NAME = "media_catalog_snapshot_v2.json"
    private const val MAX_CACHED_ITEMS = 6000
    private const val PERSIST_DEBOUNCE_MS = 900L
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var pendingItems: List<MediaItem>? = null
    private var persistJob: Job? = null

    private data class Entry(
        val id: Long,
        val uri: String,
        val type: String,
        val dateAdded: Long,
        val size: Long,
        val width: Int,
        val height: Int,
        val mimeType: String,
        val displayName: String,
        val resolution: String,
        val isLivePhoto: Boolean,
        val livePhotoVideoUri: String?,
        val livePhotoVideoId: Long?,
        val path: String?,
        val bucketName: String,
        val duration: Long,
        val isMotionPhoto: Boolean,
        val motionPhotoVideoOffset: Long,
        val motionPhotoVideoLength: Long
    )

    fun load(context: Context): List<MediaItem> {
        return runCatching {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists()) return emptyList()
            val json = file.readText()
            val type = object : TypeToken<List<Entry>>() {}.type
            val entries: List<Entry> = gson.fromJson(json, type) ?: emptyList()
            val resolvedItems = entries.mapNotNull { entry ->
                runCatching {
                    MediaItem(
                        id = entry.id,
                        uri = Uri.parse(entry.uri),
                        type = entry.type,
                        dateAdded = entry.dateAdded,
                        size = entry.size,
                        width = entry.width,
                        height = entry.height,
                        mimeType = entry.mimeType,
                        displayName = entry.displayName,
                        resolution = entry.resolution,
                        isLivePhoto = entry.isLivePhoto,
                        livePhotoVideoUri = entry.livePhotoVideoUri?.let(Uri::parse),
                        livePhotoVideoId = entry.livePhotoVideoId,
                        path = entry.path,
                        bucketName = entry.bucketName,
                        duration = entry.duration,
                        isMotionPhoto = entry.isMotionPhoto,
                        motionPhotoVideoOffset = entry.motionPhotoVideoOffset,
                        motionPhotoVideoLength = entry.motionPhotoVideoLength
                    )
                }.getOrNull()
            }.sortedByDescending { it.dateAdded }
            MotionPhotoHelper.mergeCachedMotionStates(context, resolvedItems)
        }.getOrDefault(emptyList())
    }

    fun persist(context: Context, items: List<MediaItem>) {
        runCatching {
            val snapshot = items
                .sortedByDescending { it.dateAdded }
                .take(MAX_CACHED_ITEMS)
                .map { item ->
                    Entry(
                        id = item.id,
                        uri = item.uri.toString(),
                        type = item.type,
                        dateAdded = item.dateAdded,
                        size = item.size,
                        width = item.width,
                        height = item.height,
                        mimeType = item.mimeType,
                        displayName = item.displayName,
                        resolution = item.resolution,
                        isLivePhoto = item.isLivePhoto,
                        livePhotoVideoUri = item.livePhotoVideoUri?.toString(),
                        livePhotoVideoId = item.livePhotoVideoId,
                        path = item.path,
                        bucketName = item.bucketName,
                        duration = item.duration,
                        isMotionPhoto = item.isMotionPhoto,
                        motionPhotoVideoOffset = item.motionPhotoVideoOffset,
                        motionPhotoVideoLength = item.motionPhotoVideoLength
                    )
                }
            val file = File(context.filesDir, CACHE_FILE_NAME)
            val tmp = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            val json = gson.toJson(snapshot)
            tmp.writeText(json)
            if (!tmp.renameTo(file)) {
                file.writeText(json)
                runCatching { tmp.delete() }
            }
        }
    }

    fun schedulePersist(context: Context, items: List<MediaItem>) {
        val appContext = context.applicationContext
        synchronized(lock) {
            pendingItems = items.toList()
            persistJob?.cancel()
            persistJob = scope.launch {
                delay(PERSIST_DEBOUNCE_MS)
                val snapshot = synchronized(lock) {
                    val next = pendingItems
                    pendingItems = null
                    persistJob = null
                    next
                } ?: return@launch
                persist(appContext, snapshot)
            }
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            pendingItems = emptyList()
            persistJob?.cancel()
            persistJob = null
        }
        runCatching {
            File(context.filesDir, CACHE_FILE_NAME).delete()
            File(context.filesDir, "$CACHE_FILE_NAME.tmp").delete()
        }
    }

    fun isLikelyTruncated(itemCount: Int): Boolean {
        return itemCount >= MAX_CACHED_ITEMS
    }
}

internal object CatalogIndexRepository {
    private var lastFullSyncMs: Long = 0L

    fun loadAll(context: Context): List<MediaItem> = loadMedia(context)
    fun loadRecent(context: Context, limit: Int): List<MediaItem> = loadAll(context).take(limit.coerceAtLeast(0))
    fun loadRange(context: Context, offset: Int, limit: Int): List<MediaItem> {
        return loadAll(context).drop(offset.coerceAtLeast(0)).take(limit.coerceAtLeast(0))
    }
    fun countAll(context: Context): Int = queryMediaTotalCount(context)
    fun totalSizeBytes(context: Context): Long = loadAll(context).sumOf { it.size }
    fun removeIds(@Suppress("UNUSED_PARAMETER") context: Context, @Suppress("UNUSED_PARAMETER") ids: Set<Long>) = Unit
    fun upsertAll(@Suppress("UNUSED_PARAMETER") context: Context, @Suppress("UNUSED_PARAMETER") items: List<MediaItem>) = Unit
    fun latestDateAdded(context: Context): Long = loadAll(context).firstOrNull()?.dateAdded ?: 0L
    fun getLastFullSyncMs(@Suppress("UNUSED_PARAMETER") context: Context): Long = lastFullSyncMs
    fun setLastFullSyncMs(@Suppress("UNUSED_PARAMETER") context: Context, timestampMs: Long) {
        lastFullSyncMs = timestampMs
    }
    fun replaceAllByCursorPaging(
        @Suppress("UNUSED_PARAMETER") context: Context,
        pageSize: Int,
        pageLoader: (beforeDateAddedSec: Long?, beforeId: Long?, limit: Int) -> List<MediaItem>
    ): Int {
        var total = 0
        var cursorDate: Long? = null
        var cursorId: Long? = null
        val safePage = pageSize.coerceIn(300, 8000)
        while (true) {
            val page = pageLoader(cursorDate, cursorId, safePage)
            if (page.isEmpty()) break
            total += page.size
            val last = page.last()
            cursorDate = last.dateAdded
            cursorId = last.id
            if (page.size < safePage) break
        }
        return total
    }
}

fun queryMediaTotalCount(context: Context): Int {
    val rawExtensions = RAW_PHOTO_EXTENSIONS.map { "%.$it" }
    val rawConditions = rawExtensions.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?" }
    val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR $rawConditions) AND ${MediaStore.MediaColumns.IS_TRASHED}=0"
    } else {
        "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR $rawConditions)"
    }
    val selectionArgs = (
        listOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        ) + rawExtensions
        ).toTypedArray()
    return runCatching {
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(MediaStore.Files.FileColumns._ID),
            selection,
            selectionArgs,
            null
        )?.use { it.count.coerceAtLeast(0) } ?: 0
    }.getOrDefault(0)
}

fun loadMediaPageAfterCursor(
    context: Context,
    beforeDateAddedSec: Long?,
    beforeId: Long?,
    limit: Int
): List<MediaItem> {
    return loadMediaPageInternal(
        context = context,
        beforeDateAddedSec = beforeDateAddedSec,
        beforeId = beforeId,
        limit = limit
    )
}

fun loadMediaSince(context: Context, minDateAddedSec: Long, limit: Int = 6000): List<MediaItem> {
    if (minDateAddedSec <= 0L) return emptyList()
    val safeLimit = limit.coerceAtLeast(0)
    if (safeLimit == 0) return emptyList()
    return loadMediaPageInternal(
        context = context,
        beforeDateAddedSec = null,
        beforeId = null,
        minDateAddedSec = minDateAddedSec,
        limit = safeLimit
    )
}

internal fun resolvePreferredCaptureTimeSec(dateTakenMs: Long, dateAddedSec: Long): Long {
    return if (dateTakenMs > 0L) dateTakenMs / 1000L else dateAddedSec
}

fun getFormatBadgeText(item: MediaItem): String? {
    val mime = item.mimeType.lowercase(Locale.getDefault())
    val name = item.displayName.lowercase(Locale.getDefault())
    return when {
        mime.contains("gif") || name.endsWith(".gif") -> "GIF"
        mime.contains("svg") || name.endsWith(".svg") -> "SVG"
        mime.contains("raw") || isRawPhotoFileName(name) -> "RAW"
        else -> null
    }
}
