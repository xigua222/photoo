package com.voicelike.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log

private data class RawMediaEntry(
    val id: Long,
    val dateAddedSec: Long,
    val sortDateSec: Long,
    val type: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val displayName: String,
    val uri: Uri,
    val bucketId: String,
    val bucketName: String,
    val duration: Long,
    val path: String
)

private data class LivePhotoGroupKey(
    val bucketId: String,
    val stemName: String
)

private const val MEDIA_QUERY_PAGE_SIZE = 900
private const val MEDIA_BY_ID_QUERY_CHUNK_SIZE = 240
// RAW 扩展名白名单复用 MediaEligibility 的唯一清单（含 cr3/nrw/sr2 等）。
private val RAW_EXTENSIONS = RAW_PHOTO_EXTENSIONS.map { "%.$it" }

fun loadMedia(context: Context): List<MediaItem> {
    return runCatching {
        val items = ArrayList<MediaItem>(2048)
        var beforeDateAddedSec: Long? = null
        var beforeId: Long? = null
        var carryover = emptyList<RawMediaEntry>()

        while (true) {
            val rawPage = queryRawMediaEntries(
                context = context,
                beforeDateAddedSec = beforeDateAddedSec,
                beforeId = beforeId,
                limit = MEDIA_QUERY_PAGE_SIZE
            )
            if (rawPage.isEmpty()) {
                if (carryover.isNotEmpty()) {
                    items.addAll(buildMediaItems(carryover))
                }
                break
            }

            val combined = if (carryover.isEmpty()) {
                rawPage
            } else {
                ArrayList<RawMediaEntry>(carryover.size + rawPage.size).apply {
                    addAll(carryover)
                    addAll(rawPage)
                }
            }
            val pageBoundary = splitTrailingCarryover(
                rawItems = combined,
                allowCarryover = rawPage.size >= MEDIA_QUERY_PAGE_SIZE
            )
            if (pageBoundary.finalized.isNotEmpty()) {
                items.addAll(buildMediaItems(pageBoundary.finalized))
            }
            carryover = pageBoundary.carryover

            val lastRaw = rawPage.last()
            beforeDateAddedSec = lastRaw.dateAddedSec
            beforeId = lastRaw.id
            if (rawPage.size < MEDIA_QUERY_PAGE_SIZE) {
                if (carryover.isNotEmpty()) {
                    items.addAll(buildMediaItems(carryover))
                }
                break
            }
        }

        val resolvedItems = items
            .distinctBy { it.id }
            .sortedWith(compareByDescending<MediaItem> { it.dateAdded }.thenByDescending { it.id })
        MotionPhotoHelper.mergeCachedMotionStates(context, resolvedItems)
    }.getOrElse { error ->
        Log.e("VoiceLike", "Error loading media", error)
        AppRuntimeLogStore.logError(context, "VoiceLike", "Error loading media", error)
        emptyList()
    }
}

internal fun loadMediaPageInternal(
    context: Context,
    beforeDateAddedSec: Long?,
    beforeId: Long?,
    minDateAddedSec: Long? = null,
    limit: Int
): List<MediaItem> {
    val safeLimit = limit.coerceAtLeast(0)
    if (safeLimit == 0) return emptyList()

    return runCatching {
        val items = ArrayList<MediaItem>(safeLimit)
        var nextBeforeDateAddedSec = beforeDateAddedSec
        var nextBeforeId = beforeId
        var carryover = emptyList<RawMediaEntry>()

        while (items.size < safeLimit) {
            val rawFetchLimit = (safeLimit - items.size)
                .coerceAtLeast(96)
                .coerceAtMost(MEDIA_QUERY_PAGE_SIZE)
            val rawPage = queryRawMediaEntries(
                context = context,
                beforeDateAddedSec = nextBeforeDateAddedSec,
                beforeId = nextBeforeId,
                minDateAddedSec = minDateAddedSec,
                limit = rawFetchLimit
            )
            if (rawPage.isEmpty()) {
                if (carryover.isNotEmpty()) {
                    items.addAll(buildMediaItems(carryover))
                }
                break
            }

            val combined = if (carryover.isEmpty()) {
                rawPage
            } else {
                ArrayList<RawMediaEntry>(carryover.size + rawPage.size).apply {
                    addAll(carryover)
                    addAll(rawPage)
                }
            }
            val pageBoundary = splitTrailingCarryover(
                rawItems = combined,
                allowCarryover = rawPage.size >= rawFetchLimit
            )
            if (pageBoundary.finalized.isNotEmpty()) {
                items.addAll(buildMediaItems(pageBoundary.finalized))
            }
            carryover = pageBoundary.carryover

            val lastRaw = rawPage.last()
            nextBeforeDateAddedSec = lastRaw.dateAddedSec
            nextBeforeId = lastRaw.id
            if (rawPage.size < rawFetchLimit) {
                if (carryover.isNotEmpty()) {
                    items.addAll(buildMediaItems(carryover))
                }
                break
            }
        }

        val resolvedItems = items
            .distinctBy { it.id }
            .sortedWith(compareByDescending<MediaItem> { it.dateAdded }.thenByDescending { it.id })
            .take(safeLimit)
        MotionPhotoHelper.mergeCachedMotionStates(context, resolvedItems)
    }.getOrElse { error ->
        Log.e("VoiceLike", "Error loading media page", error)
        AppRuntimeLogStore.logError(context, "VoiceLike", "Error loading media page", error)
        emptyList()
    }
}

private fun queryRawMediaEntries(
    context: Context,
    beforeDateAddedSec: Long? = null,
    beforeId: Long? = null,
    minDateAddedSec: Long? = null,
    limit: Int
): List<RawMediaEntry> {
    val safeLimit = limit.coerceAtLeast(0)
    if (safeLimit == 0) return emptyList()

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()
    val rawConditions = RAW_EXTENSIONS.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?" }
    selectionParts += if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR $rawConditions) AND ${MediaStore.MediaColumns.IS_TRASHED}=0"
    } else {
        "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR $rawConditions)"
    }
    selectionArgs += MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
    selectionArgs += MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    selectionArgs += RAW_EXTENSIONS

    if (beforeDateAddedSec != null && beforeId != null) {
        selectionParts += "(${MediaStore.Files.FileColumns.DATE_ADDED} < ? OR (${MediaStore.Files.FileColumns.DATE_ADDED} = ? AND ${MediaStore.Files.FileColumns._ID} < ?))"
        selectionArgs += beforeDateAddedSec.toString()
        selectionArgs += beforeDateAddedSec.toString()
        selectionArgs += beforeId.toString()
    }
    if (minDateAddedSec != null) {
        selectionParts += "${MediaStore.Files.FileColumns.DATE_ADDED} >= ?"
        selectionArgs += minDateAddedSec.toString()
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        "datetaken"
    )
    val durationColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.MediaColumns.DURATION
    } else {
        null
    }
    val queryProjection = if (durationColumn != null) {
        projection + durationColumn + MediaStore.Files.FileColumns.DATA
    } else {
        projection + MediaStore.Files.FileColumns.DATA
    }

    return context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        queryProjection,
        selectionParts.joinToString(" AND "),
        selectionArgs.toTypedArray(),
        "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns._ID} DESC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateTakenCol = cursor.getColumnIndex("datetaken")
        val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val durationCol = if (durationColumn != null) cursor.getColumnIndex(durationColumn) else -1
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

        val rawItems = ArrayList<RawMediaEntry>(safeLimit.coerceAtLeast(32))
        while (cursor.moveToNext() && rawItems.size < safeLimit) {
            val id = cursor.getLong(idCol)
            val dateAdded = cursor.getLong(dateCol)
            val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
            val typeInt = cursor.getInt(typeCol)
            val type = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) "video" else "photo"
            rawItems.add(
                RawMediaEntry(
                    id = id,
                    dateAddedSec = dateAdded,
                    sortDateSec = resolvePreferredCaptureTimeSec(dateTaken, dateAdded),
                    type = type,
                    size = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol) ?: "",
                    displayName = cursor.getString(nameCol) ?: "",
                    uri = resolveMediaContentUri(id, typeInt),
                    bucketId = cursor.getString(bucketIdCol) ?: "unknown",
                    bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                    duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                    path = cursor.getString(pathCol) ?: ""
                )
            )
        }
        rawItems
    } ?: emptyList()
}

private data class RawPageBoundary(
    val finalized: List<RawMediaEntry>,
    val carryover: List<RawMediaEntry>
)

private fun splitTrailingCarryover(
    rawItems: List<RawMediaEntry>,
    allowCarryover: Boolean
): RawPageBoundary {
    if (!allowCarryover || rawItems.isEmpty()) {
        return RawPageBoundary(finalized = rawItems, carryover = emptyList())
    }

    val trailingKey = rawItems.last().groupKey()
    var splitIndex = rawItems.size - 1
    while (splitIndex > 0 && rawItems[splitIndex - 1].groupKey() == trailingKey) {
        splitIndex -= 1
    }
    if (splitIndex == 0) {
        return RawPageBoundary(finalized = rawItems, carryover = emptyList())
    }
    return RawPageBoundary(
        finalized = rawItems.subList(0, splitIndex).toList(),
        carryover = rawItems.subList(splitIndex, rawItems.size).toList()
    )
}

private fun buildMediaItems(rawItems: List<RawMediaEntry>): List<MediaItem> {
    if (rawItems.isEmpty()) return emptyList()

    val grouped = LinkedHashMap<LivePhotoGroupKey, MutableList<RawMediaEntry>>(rawItems.size)
    rawItems.forEach { item ->
        grouped.getOrPut(item.groupKey()) { mutableListOf() }.add(item)
    }

    val items = ArrayList<MediaItem>(rawItems.size)
    grouped.values.forEach { group ->
        var firstPhoto: RawMediaEntry? = null
        var firstVideo: RawMediaEntry? = null
        group.forEach { entry ->
            if (firstPhoto == null && entry.type == "photo") {
                firstPhoto = entry
            } else if (firstVideo == null && entry.type == "video") {
                firstVideo = entry
            }
        }

        val photo = firstPhoto
        val video = firstVideo
        if (photo != null && video != null) {
            items.add(
                photo.toMediaItem(
                    combinedSize = photo.size + video.size,
                    isLivePhoto = true,
                    livePhotoVideoUri = video.uri,
                    livePhotoVideoId = video.id,
                    duration = video.duration
                )
            )
            group.forEach { entry ->
                if (entry.id == photo.id || entry.id == video.id) return@forEach
                items.add(entry.toMediaItem())
            }
        } else {
            group.forEach { entry ->
                items.add(entry.toMediaItem())
            }
        }
    }

    items.sortWith(compareByDescending<MediaItem> { it.dateAdded }.thenByDescending { it.id })
    return items
}

private fun RawMediaEntry.toMediaItem(
    combinedSize: Long = size,
    isLivePhoto: Boolean = false,
    livePhotoVideoUri: Uri? = null,
    livePhotoVideoId: Long? = null,
    duration: Long = this.duration
): MediaItem {
    return MediaItem(
        id = id,
        uri = uri,
        type = type,
        dateAdded = sortDateSec,
        size = combinedSize,
        width = width,
        height = height,
        mimeType = mimeType,
        displayName = displayName,
        resolution = "${width}x${height}",
        isLivePhoto = isLivePhoto,
        livePhotoVideoUri = livePhotoVideoUri,
        livePhotoVideoId = livePhotoVideoId,
        bucketName = bucketName,
        duration = duration,
        path = path
    )
}

private fun RawMediaEntry.groupKey(): LivePhotoGroupKey {
    return LivePhotoGroupKey(
        bucketId = bucketId,
        stemName = displayName.substringBeforeLast(".")
    )
}

private fun resolveMediaContentUri(id: Long, typeInt: Int): Uri {
    return when (typeInt) {
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        else ->
            ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
    }
}

fun loadMediaDateAddedByIds(
    context: Context,
    ids: Collection<Long>
): Map<Long, Long> {
    if (ids.isEmpty()) return emptyMap()

    val distinctIds = LinkedHashSet<Long>(ids.size).apply {
        ids.forEach { add(it) }
    }.toList()
    val result = LinkedHashMap<Long, Long>(distinctIds.size)

    distinctIds.chunked(MEDIA_BY_ID_QUERY_CHUNK_SIZE).forEach { chunk ->
        val placeholders = List(chunk.size) { "?" }.joinToString(",")
        val selectionParts = mutableListOf<String>()
        selectionParts += "(${MediaStore.Files.FileColumns._ID} IN ($placeholders))"
        selectionParts += "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            selectionParts += "${MediaStore.MediaColumns.IS_TRASHED}=0"
        }
        val selectionArgs = buildList {
            chunk.forEach { add(it.toString()) }
            add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        }
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_ADDED,
            "datetaken"
        )

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selectionParts.joinToString(" AND "),
            selectionArgs.toTypedArray(),
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val dateTakenCol = cursor.getColumnIndex("datetaken")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateAdded = cursor.getLong(dateCol)
                val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                result[id] = resolvePreferredCaptureTimeSec(dateTaken, dateAdded)
            }
        }
    }

    return result
}

fun loadMediaByIds(
    context: Context,
    ids: List<Long>
): List<MediaItem> {
    if (ids.isEmpty()) return emptyList()

    val distinctIds = LinkedHashSet<Long>(ids.size).apply {
        ids.forEach { add(it) }
    }.toList()
    val resultById = LinkedHashMap<Long, MediaItem>(distinctIds.size)

    distinctIds.chunked(MEDIA_BY_ID_QUERY_CHUNK_SIZE).forEach { chunk ->
        val chunkItems = queryRawMediaEntriesByIds(context, chunk)
            .let(::buildMediaItems)
            .associateBy { it.id }
        chunk.forEach { id ->
            chunkItems[id]?.let { resultById[id] = it }
        }
    }

    return MotionPhotoHelper.mergeCachedMotionStates(
        context = context,
        items = distinctIds.mapNotNull { id -> resultById[id] }
    )
}

fun resolvePickedMediaItem(
    context: Context,
    sourceUri: Uri
): MediaItem? {
    val baseEntry = resolveRawMediaEntryForPickedUri(context, sourceUri) ?: return null
    val groupedItems = queryRawMediaEntriesByGroupKey(context, baseEntry.groupKey())
        .let(::buildMediaItems)
    val resolvedBaseItem = groupedItems.firstOrNull { it.id == baseEntry.id } ?: baseEntry.toMediaItem()
    if (resolvedBaseItem.type != "photo" || resolvedBaseItem.isLivePhoto || resolvedBaseItem.isMotionPhoto) {
        return resolvedBaseItem
    }

    val motionInfo = MotionPhotoHelper.detectAndPersist(context, resolvedBaseItem)
    return if (motionInfo.isMotionPhoto) {
        resolvedBaseItem.copy(
            isMotionPhoto = true,
            motionPhotoVideoOffset = motionInfo.videoOffset,
            motionPhotoVideoLength = motionInfo.videoLength
        )
    } else {
        resolvedBaseItem
    }
}

private fun resolveRawMediaEntryForPickedUri(
    context: Context,
    sourceUri: Uri
): RawMediaEntry? {
    val directId = runCatching { ContentUris.parseId(sourceUri) }
        .getOrNull()
        ?.takeIf { it > 0L }
    if (directId != null) {
        return queryRawMediaEntriesByIds(context, listOf(directId)).firstOrNull()
    }

    val documentId = runCatching {
        if (DocumentsContract.isDocumentUri(context, sourceUri)) {
            DocumentsContract.getDocumentId(sourceUri)
        } else {
            null
        }
    }.getOrNull()
    val documentBackedId = documentId
        ?.substringAfter(':', "")
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
    if (documentBackedId != null) {
        return queryRawMediaEntriesByIds(context, listOf(documentBackedId)).firstOrNull()
    }

    val queriedId = runCatching {
        context.contentResolver.query(
            sourceUri,
            arrayOf(MediaStore.MediaColumns._ID),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            if (idColumn >= 0 && cursor.moveToFirst()) {
                cursor.getLong(idColumn)
            } else {
                null
            }
        }
    }.getOrNull()
        ?.takeIf { it > 0L }
    if (queriedId != null) {
        return queryRawMediaEntriesByIds(context, listOf(queriedId)).firstOrNull()
    }

    return null
}

private fun queryRawMediaEntriesByGroupKey(
    context: Context,
    groupKey: LivePhotoGroupKey
): List<RawMediaEntry> {
    if (groupKey.stemName.isBlank()) return emptyList()

    val selectionParts = mutableListOf<String>()
    selectionParts += "${MediaStore.Images.Media.BUCKET_ID}=?"
    selectionParts += "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
    selectionParts += "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? ESCAPE '\\'"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        selectionParts += "${MediaStore.MediaColumns.IS_TRASHED}=0"
    }
    val selectionArgs = arrayOf(
        groupKey.bucketId,
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        "${escapeSqlLikeArgument(groupKey.stemName)}.%"
    )
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        "datetaken"
    )
    val durationColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.MediaColumns.DURATION
    } else {
        null
    }
    val queryProjection = if (durationColumn != null) {
        projection + durationColumn + MediaStore.Files.FileColumns.DATA
    } else {
        projection + MediaStore.Files.FileColumns.DATA
    }

    return context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        queryProjection,
        selectionParts.joinToString(" AND "),
        selectionArgs,
        "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns._ID} DESC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateTakenCol = cursor.getColumnIndex("datetaken")
        val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val durationCol = if (durationColumn != null) cursor.getColumnIndex(durationColumn) else -1
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

        val rawItems = ArrayList<RawMediaEntry>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val dateAdded = cursor.getLong(dateCol)
            val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
            val typeInt = cursor.getInt(typeCol)
            val entry = RawMediaEntry(
                id = id,
                dateAddedSec = dateAdded,
                sortDateSec = resolvePreferredCaptureTimeSec(dateTaken, dateAdded),
                type = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) "video" else "photo",
                size = cursor.getLong(sizeCol),
                width = cursor.getInt(widthCol),
                height = cursor.getInt(heightCol),
                mimeType = cursor.getString(mimeCol) ?: "",
                displayName = cursor.getString(nameCol) ?: "",
                uri = resolveMediaContentUri(id, typeInt),
                bucketId = cursor.getString(bucketIdCol) ?: "unknown",
                bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                path = cursor.getString(pathCol) ?: ""
            )
            if (entry.groupKey() == groupKey) {
                rawItems += entry
            }
        }
        rawItems
    } ?: emptyList()
}

private fun escapeSqlLikeArgument(value: String): String {
    return buildString(value.length + 8) {
        value.forEach { char ->
            when (char) {
                '\\', '%', '_' -> {
                    append('\\')
                    append(char)
                }
                else -> append(char)
            }
        }
    }
}

private fun queryRawMediaEntriesByIds(
    context: Context,
    ids: List<Long>
): List<RawMediaEntry> {
    if (ids.isEmpty()) return emptyList()

    val placeholders = List(ids.size) { "?" }.joinToString(",")
    val selectionParts = mutableListOf<String>()
    selectionParts += "(${MediaStore.Files.FileColumns._ID} IN ($placeholders))"
    selectionParts += "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        selectionParts += "${MediaStore.MediaColumns.IS_TRASHED}=0"
    }
    val selectionArgs = buildList {
        ids.forEach { add(it.toString()) }
        add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
        add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
    }
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        "datetaken"
    )
    val durationColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.MediaColumns.DURATION
    } else {
        null
    }
    val queryProjection = if (durationColumn != null) {
        projection + durationColumn + MediaStore.Files.FileColumns.DATA
    } else {
        projection + MediaStore.Files.FileColumns.DATA
    }

    return context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        queryProjection,
        selectionParts.joinToString(" AND "),
        selectionArgs.toTypedArray(),
        null
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateTakenCol = cursor.getColumnIndex("datetaken")
        val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val durationCol = if (durationColumn != null) cursor.getColumnIndex(durationColumn) else -1
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

        val rawItems = ArrayList<RawMediaEntry>(ids.size)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val dateAdded = cursor.getLong(dateCol)
            val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
            val typeInt = cursor.getInt(typeCol)
            val type = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) "video" else "photo"
            rawItems += RawMediaEntry(
                id = id,
                dateAddedSec = dateAdded,
                sortDateSec = resolvePreferredCaptureTimeSec(dateTaken, dateAdded),
                type = type,
                size = cursor.getLong(sizeCol),
                width = cursor.getInt(widthCol),
                height = cursor.getInt(heightCol),
                mimeType = cursor.getString(mimeCol) ?: "",
                displayName = cursor.getString(nameCol) ?: "",
                uri = resolveMediaContentUri(id, typeInt),
                bucketId = cursor.getString(bucketIdCol) ?: "unknown",
                bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                path = cursor.getString(pathCol) ?: ""
            )
        }
        rawItems
    } ?: emptyList()
}
