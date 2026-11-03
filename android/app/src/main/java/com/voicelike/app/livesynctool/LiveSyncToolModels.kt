package com.voicelike.app.livesynctool

import android.net.Uri

internal enum class LiveSyncToolTargetFamily {
    SingleFileMotionPhoto,
    XiaomiMotionPhoto,
    VendorPair,
    OplusMotionPhoto
}

internal enum class LiveSyncToolSourceKind {
    Static,
    PairedLive,
    EmbeddedMotion
}

internal data class LiveSyncToolMediaItem(
    val id: Long,
    val uri: Uri,
    val type: String,
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val displayName: String = "",
    val bucketId: String = "",
    val bucketName: String = "",
    val duration: Long = 0L,
    val isLivePhoto: Boolean = false,
    val livePhotoShell: LiveSyncToolVendorPairShell? = null,
    val livePhotoVideoUri: Uri? = null,
    val livePhotoVideoId: Long? = null,
    val isMotionPhoto: Boolean = false,
    val motionPhotoVideoOffset: Long = 0L,
    val motionPhotoVideoLength: Long = 0L
)

internal data class LiveSyncToolScanCandidate(
    val mediaItem: LiveSyncToolMediaItem,
    val sourceKind: LiveSyncToolSourceKind,
    val targetFamily: LiveSyncToolTargetFamily,
    val targetReady: Boolean,
    val selectable: Boolean,
    val reason: String?
)

internal data class LiveSyncToolScanProgress(
    val processed: Int,
    val total: Int,
    val matched: Int
)

internal data class LiveSyncToolScanResumeToken(
    val lastProcessedMediaId: Long,
    val lastProcessedDateAdded: Long,
    val processedCount: Int,
    val totalCount: Int
)

internal data class LiveSyncToolConversionProgress(
    val processed: Int,
    val total: Int,
    val currentName: String
)

internal data class LiveSyncToolConversionFailure(
    val item: LiveSyncToolScanCandidate,
    val message: String
)

internal data class LiveSyncToolConvertedItem(
    val sourceItem: LiveSyncToolMediaItem,
    val sourceKind: LiveSyncToolSourceKind,
    val targetFamily: LiveSyncToolTargetFamily,
    val oldUris: List<Uri>,
    val newPhotoUri: Uri,
    val newVideoUri: Uri? = null,
    val displayName: String = sourceItem.displayName,
    val deletedOld: Boolean = false
)

internal data class LiveSyncToolConversionReport(
    val successes: List<LiveSyncToolConvertedItem>,
    val succeeded: Int,
    val failures: List<LiveSyncToolConversionFailure>
)
