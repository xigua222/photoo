package com.voicelike.app.livesynctool

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal data class LiveSyncToolSnapshot(
    val targetFamily: LiveSyncToolTargetFamily,
    val candidates: List<LiveSyncToolSnapshotCandidate>,
    val selectedIds: Set<Long>,
    val progress: LiveSyncToolScanProgress,
    val updatedAtMs: Long,
    val scanInFlight: Boolean,
    val resumeToken: LiveSyncToolScanResumeToken? = null,
    val version: Int = 2
)

internal data class LiveSyncToolSnapshotCandidate(
    val mediaId: Long,
    val mediaUri: String,
    val mediaType: String,
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val displayName: String,
    val bucketId: String,
    val bucketName: String,
    val duration: Long,
    val isLivePhoto: Boolean,
    val livePhotoShell: String?,
    val livePhotoVideoUri: String?,
    val livePhotoVideoId: Long?,
    val isMotionPhoto: Boolean,
    val motionPhotoVideoOffset: Long,
    val motionPhotoVideoLength: Long,
    val sourceKind: LiveSyncToolSourceKind,
    val targetFamily: LiveSyncToolTargetFamily,
    val targetReady: Boolean,
    val selectable: Boolean,
    val reason: String?
)

internal object LiveSyncToolSnapshotStore {
    private const val fileName = "live_sync_tool_snapshot_v1.json"
    private const val tempFileName = "live_sync_tool_snapshot_v1.json.tmp"

    fun load(context: Context): LiveSyncToolSnapshot? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null
        return runCatching {
            val json = file.readText()
            if (json.isBlank()) return null
            parseSnapshot(JSONObject(json))
        }.getOrElse {
            clear(context)
            null
        }
    }

    fun save(context: Context, snapshot: LiveSyncToolSnapshot) {
        runCatching {
            val file = File(context.filesDir, fileName)
            val temp = File(context.filesDir, tempFileName)
            val json = snapshot.toJson().toString()
            temp.writeText(json)
            if (!temp.renameTo(file)) {
                file.writeText(json)
                runCatching { temp.delete() }
            }
        }
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, fileName).delete() }
        runCatching { File(context.filesDir, tempFileName).delete() }
    }

    fun toSnapshot(
        targetFamily: LiveSyncToolTargetFamily,
        candidates: List<LiveSyncToolScanCandidate>,
        selectedIds: Set<Long>,
        progress: LiveSyncToolScanProgress,
        scanInFlight: Boolean,
        resumeToken: LiveSyncToolScanResumeToken? = null
    ): LiveSyncToolSnapshot {
        return LiveSyncToolSnapshot(
            targetFamily = targetFamily,
            candidates = candidates.map { it.toSnapshotCandidate() },
            selectedIds = selectedIds,
            progress = progress,
            updatedAtMs = System.currentTimeMillis(),
            scanInFlight = scanInFlight,
            resumeToken = resumeToken
        )
    }

    fun restoreCandidates(snapshot: LiveSyncToolSnapshot): List<LiveSyncToolScanCandidate> {
        return snapshot.candidates.mapNotNull { candidate ->
            runCatching { candidate.toScanCandidate() }.getOrNull()
        }
    }

    private fun LiveSyncToolSnapshot.toJson(): JSONObject {
        return JSONObject()
            .put("targetFamily", targetFamily.name)
            .put("candidates", JSONArray().apply {
                candidates.forEach { put(it.toJson()) }
            })
            .put("selectedIds", JSONArray().apply {
                selectedIds.forEach { put(it) }
            })
            .put("progress", JSONObject()
                .put("processed", progress.processed)
                .put("total", progress.total)
                .put("matched", progress.matched))
            .put("updatedAtMs", updatedAtMs)
            .put("scanInFlight", scanInFlight)
            .put(
                "resumeToken",
                resumeToken?.let {
                    JSONObject()
                        .put("lastProcessedMediaId", it.lastProcessedMediaId)
                        .put("lastProcessedDateAdded", it.lastProcessedDateAdded)
                        .put("processedCount", it.processedCount)
                        .put("totalCount", it.totalCount)
                } ?: JSONObject.NULL
            )
            .put("version", version)
    }

    private fun LiveSyncToolSnapshotCandidate.toJson(): JSONObject {
        return JSONObject()
            .put("mediaId", mediaId)
            .put("mediaUri", mediaUri)
            .put("mediaType", mediaType)
            .put("dateAdded", dateAdded)
            .put("size", size)
            .put("width", width)
            .put("height", height)
            .put("mimeType", mimeType)
            .put("displayName", displayName)
            .put("bucketId", bucketId)
            .put("bucketName", bucketName)
            .put("duration", duration)
            .put("isLivePhoto", isLivePhoto)
            .put("livePhotoShell", livePhotoShell ?: JSONObject.NULL)
            .put("livePhotoVideoUri", livePhotoVideoUri ?: JSONObject.NULL)
            .put("livePhotoVideoId", livePhotoVideoId ?: JSONObject.NULL)
            .put("isMotionPhoto", isMotionPhoto)
            .put("motionPhotoVideoOffset", motionPhotoVideoOffset)
            .put("motionPhotoVideoLength", motionPhotoVideoLength)
            .put("sourceKind", sourceKind.name)
            .put("targetFamily", targetFamily.name)
            .put("targetReady", targetReady)
            .put("selectable", selectable)
            .put("reason", reason ?: JSONObject.NULL)
    }

    private fun parseSnapshot(json: JSONObject): LiveSyncToolSnapshot {
        val targetFamily = enumValueOf<LiveSyncToolTargetFamily>(json.getString("targetFamily"))
        val candidatesArray = json.optJSONArray("candidates") ?: JSONArray()
        val candidates = buildList {
            for (index in 0 until candidatesArray.length()) {
                add(parseCandidate(candidatesArray.getJSONObject(index)))
            }
        }
        val selectedIdsArray = json.optJSONArray("selectedIds") ?: JSONArray()
        val selectedIds = buildSet {
            for (index in 0 until selectedIdsArray.length()) {
                add(selectedIdsArray.optLong(index))
            }
        }
        val progressJson = json.optJSONObject("progress") ?: JSONObject()
        val resumeTokenJson = json.optJSONObject("resumeToken")
        return LiveSyncToolSnapshot(
            targetFamily = targetFamily,
            candidates = candidates,
            selectedIds = selectedIds,
            progress = LiveSyncToolScanProgress(
                processed = progressJson.optInt("processed"),
                total = progressJson.optInt("total"),
                matched = progressJson.optInt("matched")
            ),
            updatedAtMs = json.optLong("updatedAtMs"),
            scanInFlight = json.optBoolean("scanInFlight"),
            resumeToken = resumeTokenJson?.let {
                LiveSyncToolScanResumeToken(
                    lastProcessedMediaId = it.optLong("lastProcessedMediaId"),
                    lastProcessedDateAdded = it.optLong("lastProcessedDateAdded"),
                    processedCount = it.optInt("processedCount"),
                    totalCount = it.optInt("totalCount")
                )
            },
            version = json.optInt("version", 1)
        )
    }

    private fun parseCandidate(json: JSONObject): LiveSyncToolSnapshotCandidate {
        return LiveSyncToolSnapshotCandidate(
            mediaId = json.getLong("mediaId"),
            mediaUri = json.getString("mediaUri"),
            mediaType = json.optString("mediaType"),
            dateAdded = json.optLong("dateAdded"),
            size = json.optLong("size"),
            width = json.optInt("width"),
            height = json.optInt("height"),
            mimeType = json.optString("mimeType"),
            displayName = json.optString("displayName"),
            bucketId = json.optString("bucketId"),
            bucketName = json.optString("bucketName"),
            duration = json.optLong("duration"),
            isLivePhoto = json.optBoolean("isLivePhoto"),
            livePhotoShell = json.optNullableString("livePhotoShell"),
            livePhotoVideoUri = json.optNullableString("livePhotoVideoUri"),
            livePhotoVideoId = json.optNullableLong("livePhotoVideoId"),
            isMotionPhoto = json.optBoolean("isMotionPhoto"),
            motionPhotoVideoOffset = json.optLong("motionPhotoVideoOffset"),
            motionPhotoVideoLength = json.optLong("motionPhotoVideoLength"),
            sourceKind = enumValueOf<LiveSyncToolSourceKind>(json.getString("sourceKind")),
            targetFamily = enumValueOf<LiveSyncToolTargetFamily>(json.getString("targetFamily")),
            targetReady = json.optBoolean("targetReady"),
            selectable = json.optBoolean("selectable"),
            reason = json.optNullableString("reason")
        )
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (isNull(key) || !has(key)) null else optString(key)
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        return if (isNull(key) || !has(key)) null else optLong(key)
    }

    private fun LiveSyncToolScanCandidate.toSnapshotCandidate(): LiveSyncToolSnapshotCandidate {
        return LiveSyncToolSnapshotCandidate(
            mediaId = mediaItem.id,
            mediaUri = mediaItem.uri.toString(),
            mediaType = mediaItem.type,
            dateAdded = mediaItem.dateAdded,
            size = mediaItem.size,
            width = mediaItem.width,
            height = mediaItem.height,
            mimeType = mediaItem.mimeType,
            displayName = mediaItem.displayName,
            bucketId = mediaItem.bucketId,
            bucketName = mediaItem.bucketName,
            duration = mediaItem.duration,
            isLivePhoto = mediaItem.isLivePhoto,
            livePhotoShell = mediaItem.livePhotoShell?.name,
            livePhotoVideoUri = mediaItem.livePhotoVideoUri?.toString(),
            livePhotoVideoId = mediaItem.livePhotoVideoId,
            isMotionPhoto = mediaItem.isMotionPhoto,
            motionPhotoVideoOffset = mediaItem.motionPhotoVideoOffset,
            motionPhotoVideoLength = mediaItem.motionPhotoVideoLength,
            sourceKind = sourceKind,
            targetFamily = targetFamily,
            targetReady = targetReady,
            selectable = selectable,
            reason = reason
        )
    }

    private fun LiveSyncToolSnapshotCandidate.toScanCandidate(): LiveSyncToolScanCandidate {
        return LiveSyncToolScanCandidate(
            mediaItem = LiveSyncToolMediaItem(
                id = mediaId,
                uri = Uri.parse(mediaUri),
                type = mediaType,
                dateAdded = dateAdded,
                size = size,
                width = width,
                height = height,
                mimeType = mimeType,
                displayName = displayName,
                bucketId = bucketId,
                bucketName = bucketName,
                duration = duration,
                isLivePhoto = isLivePhoto,
                livePhotoShell = livePhotoShell?.let(::enumValueOf),
                livePhotoVideoUri = livePhotoVideoUri?.let(Uri::parse),
                livePhotoVideoId = livePhotoVideoId,
                isMotionPhoto = isMotionPhoto,
                motionPhotoVideoOffset = motionPhotoVideoOffset,
                motionPhotoVideoLength = motionPhotoVideoLength
            ),
            sourceKind = sourceKind,
            targetFamily = targetFamily,
            targetReady = targetReady,
            selectable = selectable,
            reason = reason
        )
    }
}
