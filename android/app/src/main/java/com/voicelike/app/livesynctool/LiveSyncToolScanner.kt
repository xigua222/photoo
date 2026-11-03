package com.voicelike.app.livesynctool

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal object LiveSyncToolScanner {
    private data class RawMediaEntry(
        val id: Long,
        val dateAddedSec: Long,
        val type: String,
        val size: Long,
        val width: Int,
        val height: Int,
        val mimeType: String,
        val displayName: String,
        val uri: Uri,
        val bucketId: String,
        val bucketName: String,
        val duration: Long
    )

    private data class LiveGroupKey(
        val bucketId: String,
        val stemName: String
    )

    suspend fun scan(
        context: Context,
        initialCandidates: List<LiveSyncToolScanCandidate> = emptyList(),
        resumeToken: LiveSyncToolScanResumeToken? = null,
        onProgress: (LiveSyncToolScanProgress) -> Unit,
        onCheckpoint: (
            List<LiveSyncToolScanCandidate>,
            LiveSyncToolScanProgress,
            Boolean,
            LiveSyncToolScanResumeToken?
        ) -> Unit = { _, _, _, _ -> }
    ): List<LiveSyncToolScanCandidate> = withContext(Dispatchers.IO) {
        val targetFamily = LiveSyncToolRouting.resolveTargetFamily()
        val rawEntries = queryRawMediaEntries(context)
        val grouped = rawEntries.groupBy { it.groupKey() }
        val photos = rawEntries.filter { it.type == "photo" }
        val startIndex = resolveResumeStartIndex(photos, resumeToken)
        val resultsById = linkedMapOf<Long, LiveSyncToolScanCandidate>().apply {
            initialCandidates.forEach { put(it.mediaItem.id, it) }
        }
        var latestResumeToken = resumeToken.takeIf { startIndex > 0 }

        photos.drop(startIndex).forEachIndexed { offset, photo ->
            coroutineContext.ensureActive()
            val index = startIndex + offset
            val pairGroup = grouped[photo.groupKey()].orEmpty()
            val pairedVideo = pairGroup.firstOrNull { it.type == "video" }
            val motionInfo = LiveSyncToolMotionSupport.checkMotionPhoto(context, photo.uri)
            val livePhotoShell = if (pairedVideo != null) {
                VendorLivePhotoRoundTripSupport.inspectSourceShell(
                    context = context,
                    sourceItem = LiveSyncToolMediaItem(
                        id = photo.id,
                        uri = photo.uri,
                        type = photo.type,
                        dateAdded = photo.dateAddedSec,
                        size = photo.size + pairedVideo.size,
                        width = photo.width,
                        height = photo.height,
                        mimeType = photo.mimeType,
                        displayName = photo.displayName,
                        bucketId = photo.bucketId,
                        bucketName = photo.bucketName,
                        duration = pairedVideo.duration,
                        isLivePhoto = true,
                        livePhotoVideoUri = pairedVideo.uri,
                        livePhotoVideoId = pairedVideo.id
                    )
                )
            } else {
                null
            }
            val mediaItem = LiveSyncToolMediaItem(
                id = photo.id,
                uri = photo.uri,
                type = photo.type,
                dateAdded = photo.dateAddedSec,
                size = photo.size + (pairedVideo?.size ?: 0L),
                width = photo.width,
                height = photo.height,
                mimeType = photo.mimeType,
                displayName = photo.displayName,
                bucketId = photo.bucketId,
                bucketName = photo.bucketName,
                duration = pairedVideo?.duration ?: 0L,
                isLivePhoto = pairedVideo != null,
                livePhotoShell = livePhotoShell,
                livePhotoVideoUri = pairedVideo?.uri,
                livePhotoVideoId = pairedVideo?.id,
                isMotionPhoto = motionInfo.isMotionPhoto,
                motionPhotoVideoOffset = motionInfo.videoOffset,
                motionPhotoVideoLength = motionInfo.videoLength
            )
            val sourceKind = when {
                pairedVideo != null -> LiveSyncToolSourceKind.PairedLive
                motionInfo.isMotionPhoto -> LiveSyncToolSourceKind.EmbeddedMotion
                else -> LiveSyncToolSourceKind.Static
            }
            if (sourceKind != LiveSyncToolSourceKind.Static) {
                val targetReady = isTargetReady(context, mediaItem, targetFamily)
                val selectable = !targetReady && targetFamily != LiveSyncToolTargetFamily.OplusMotionPhoto
                val reason = when {
                    targetReady -> "已经是${LiveSyncToolRouting.targetLabel(targetFamily)}"
                    targetFamily == LiveSyncToolTargetFamily.OplusMotionPhoto -> "OPPO 路线预留中，暂不开放"
                    else -> null
                }
                if (!targetReady || targetFamily == LiveSyncToolTargetFamily.OplusMotionPhoto) {
                    resultsById[mediaItem.id] = LiveSyncToolScanCandidate(
                        mediaItem = mediaItem,
                        sourceKind = sourceKind,
                        targetFamily = targetFamily,
                        targetReady = targetReady,
                        selectable = selectable,
                        reason = reason
                    )
                }
            }
            if ((index + 1) % 12 == 0 || index == photos.lastIndex) {
                latestResumeToken = LiveSyncToolScanResumeToken(
                    lastProcessedMediaId = photo.id,
                    lastProcessedDateAdded = photo.dateAddedSec,
                    processedCount = index + 1,
                    totalCount = photos.size
                )
                val progress = LiveSyncToolScanProgress(
                    processed = index + 1,
                    total = photos.size,
                    matched = resultsById.values.count { !it.targetReady }
                )
                onProgress(progress)
                onCheckpoint(
                    resultsById.values.toList(),
                    progress,
                    index == photos.lastIndex,
                    if (index == photos.lastIndex) null else latestResumeToken
                )
            }
        }

        resultsById.values.sortedByDescending { it.mediaItem.dateAdded }
    }

    private fun resolveResumeStartIndex(
        photos: List<RawMediaEntry>,
        resumeToken: LiveSyncToolScanResumeToken?
    ): Int {
        if (resumeToken == null) return 0
        val resumeIndex = photos.indexOfFirst {
            it.id == resumeToken.lastProcessedMediaId &&
                it.dateAddedSec == resumeToken.lastProcessedDateAdded
        }
        return if (resumeIndex >= 0) {
            (resumeIndex + 1).coerceAtMost(photos.size)
        } else {
            0
        }
    }

    private fun isTargetReady(
        context: Context,
        item: LiveSyncToolMediaItem,
        targetFamily: LiveSyncToolTargetFamily
    ): Boolean {
        return when (targetFamily) {
            LiveSyncToolTargetFamily.VendorPair ->
                item.isLivePhoto &&
                    item.livePhotoVideoUri != null &&
                    item.livePhotoShell == LiveSyncToolRouting.resolveVendorPairShell()
            LiveSyncToolTargetFamily.SingleFileMotionPhoto ->
                item.isMotionPhoto
            LiveSyncToolTargetFamily.XiaomiMotionPhoto ->
                item.isMotionPhoto && XiaomiMotionPhotoRoundTripSupport.extract(context, item) != null
            LiveSyncToolTargetFamily.OplusMotionPhoto ->
                false
        }
    }

    private fun queryRawMediaEntries(context: Context): List<RawMediaEntry> {
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DATE_ADDED)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.WIDTH)
            add(MediaStore.Files.FileColumns.HEIGHT)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Images.Media.BUCKET_ID)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            add("datetaken")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.DURATION)
            }
        }.toTypedArray()

        val selection = buildString {
            append("(")
            append(MediaStore.Files.FileColumns.MEDIA_TYPE)
            append("=? OR ")
            append(MediaStore.Files.FileColumns.MEDIA_TYPE)
            append("=?)")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                append(" AND ")
                append(MediaStore.MediaColumns.IS_TRASHED)
                append("=0")
            }
        }
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        return context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns._ID} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val durationCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
            val items = ArrayList<RawMediaEntry>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mediaType = cursor.getInt(typeCol)
                val type = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) "video" else "photo"
                items += RawMediaEntry(
                    id = id,
                    dateAddedSec = cursor.getLong(dateCol),
                    type = type,
                    size = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol).orEmpty(),
                    displayName = cursor.getString(nameCol).orEmpty(),
                    uri = resolveMediaContentUri(id, mediaType),
                    bucketId = cursor.getString(bucketIdCol).orEmpty(),
                    bucketName = cursor.getString(bucketNameCol).orEmpty(),
                    duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                )
            }
            items
        } ?: emptyList()
    }

    private fun RawMediaEntry.groupKey(): LiveGroupKey {
        return LiveGroupKey(
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
}
