package com.voicelike.app.keep

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import com.voicelike.app.MediaItem
import com.voicelike.app.formatVideoAudioTrack
import com.voicelike.app.formatVideoBitrate
import com.voicelike.app.formatVideoCodec
import com.voicelike.app.formatVideoDuration
import com.voicelike.app.formatVideoFps
import com.voicelike.app.loadPhotoCardMetadata
import com.voicelike.app.loadVideoCardMetadata
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class KeepPreviewMetadataRow(
    val label: String,
    val value: String
)

internal data class KeepPreviewMetadata(
    val resolvedItem: MediaItem,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val detailRows: List<KeepPreviewMetadataRow>
)

private const val KEEP_PREVIEW_METADATA_CACHE_SIZE = 96

private val KEEP_EXIF_EXCLUDED_FIELD_NAMES = setOf(
    "TAG_JPEG_INTERCHANGE_FORMAT",
    "TAG_JPEG_INTERCHANGE_FORMAT_LENGTH",
    "TAG_MAKER_NOTE",
    "TAG_ORIENTATION",
    "TAG_PIXEL_X_DIMENSION",
    "TAG_PIXEL_Y_DIMENSION",
    "TAG_ROWS_PER_STRIP",
    "TAG_STRIP_BYTE_COUNTS",
    "TAG_STRIP_OFFSETS",
    "TAG_THUMBNAIL_IMAGE_LENGTH",
    "TAG_THUMBNAIL_IMAGE_WIDTH",
    "TAG_USER_COMMENT",
    "TAG_XMP"
)

private val KEEP_EXIF_DETAIL_TAGS: List<Pair<String, String>> by lazy {
    ExifInterface::class.java.fields
        .filter { field ->
            field.type == String::class.java &&
                field.name.startsWith("TAG_") &&
                field.name !in KEEP_EXIF_EXCLUDED_FIELD_NAMES
        }
        .mapNotNull { field ->
            val tag = runCatching { field.get(null) as? String }.getOrNull() ?: return@mapNotNull null
            formatExifFieldLabel(field.name.removePrefix("TAG_")) to tag
        }
        .distinctBy { it.second }
        .sortedBy { it.first }
}

private object KeepPreviewMetadataCache {
    private val cache = object : LruCache<Long, KeepPreviewMetadata>(KEEP_PREVIEW_METADATA_CACHE_SIZE) {}

    fun peek(id: Long): KeepPreviewMetadata? = cache.get(id)

    fun put(metadata: KeepPreviewMetadata) {
        cache.put(metadata.resolvedItem.id, metadata)
    }
}

internal fun peekKeepPreviewMetadata(itemId: Long): KeepPreviewMetadata? {
    return KeepPreviewMetadataCache.peek(itemId)
}

internal suspend fun loadKeepPreviewMetadata(
    context: Context,
    item: MediaItem
): KeepPreviewMetadata = withContext(Dispatchers.IO) {
    KeepPreviewMetadataCache.peek(item.id)?.let { return@withContext it }

    val resolvedItem = if (item.type == "video") {
        loadVideoCardMetadata(context, item) ?: item
    } else {
        loadPhotoCardMetadata(context, item) ?: item
    }
    val filePath = item.path?.takeIf { it.isNotBlank() } ?: item.uri.toString()
    val fileName = item.displayName.takeIf { it.isNotBlank() }
        ?: File(item.path ?: "").name.takeIf { it.isNotBlank() }
        ?: if (item.type == "video") "video_${item.id}" else "image_${item.id}"
    val metadata = KeepPreviewMetadata(
        resolvedItem = resolvedItem,
        fileName = fileName,
        filePath = filePath,
        mimeType = item.mimeType,
        detailRows = if (item.type == "video") {
            loadKeepVideoMetadataRows(item, resolvedItem)
        } else {
            loadKeepPhotoExifRows(context, item)
        }
    )
    KeepPreviewMetadataCache.put(metadata)
    metadata
}

private fun loadKeepPhotoExifRows(
    context: Context,
    item: MediaItem
): List<KeepPreviewMetadataRow> {
    val exif = openKeepExifInterface(context, item) ?: return emptyList()
    return KEEP_EXIF_DETAIL_TAGS.mapNotNull { (label, tag) ->
        exif.getAttribute(tag)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { value -> KeepPreviewMetadataRow(label = label, value = value) }
    }
}

private fun loadKeepVideoMetadataRows(
    item: MediaItem,
    resolvedItem: MediaItem
): List<KeepPreviewMetadataRow> {
    val retriever = MediaMetadataRetriever()
    return try {
        if (item.path != null) {
            retriever.setDataSource(item.path)
        } else {
            return fallbackVideoMetadataRows(item, resolvedItem)
        }
        buildList {
            addVideoRow("Codec", formatVideoCodec(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: item.mimeType))
            addVideoRow("Bitrate", formatVideoBitrate(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)))
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            addVideoRow("Duration", formatVideoDuration(duration))
            addVideoRow(
                "Frame Rate",
                formatVideoFps(
                    captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE),
                    frameCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT),
                    durationMs = duration
                )
            )
            addVideoRow("Audio", formatVideoAudioTrack(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)))
            addVideoRow("MIME", item.mimeType)
        }
    } catch (_: Throwable) {
        fallbackVideoMetadataRows(item, resolvedItem)
    } finally {
        runCatching { retriever.release() }
    }
}

private fun fallbackVideoMetadataRows(
    item: MediaItem,
    resolvedItem: MediaItem
): List<KeepPreviewMetadataRow> {
    return buildList {
        addVideoRow("Codec", resolvedItem.camera)
        addVideoRow("Bitrate", resolvedItem.iso)
        addVideoRow("Duration", resolvedItem.focalLength)
        addVideoRow("Frame Rate", resolvedItem.shutter)
        addVideoRow("Audio", resolvedItem.aperture)
        addVideoRow("MIME", item.mimeType)
    }
}

private fun MutableList<KeepPreviewMetadataRow>.addVideoRow(
    label: String,
    value: String?
) {
    value?.takeIf { it.isNotBlank() && it != "--" }?.let {
        add(KeepPreviewMetadataRow(label = label, value = it))
    }
}

private fun openKeepExifInterface(
    context: Context,
    item: MediaItem
): ExifInterface? {
    return runCatching {
        context.contentResolver.openFileDescriptor(item.uri, "r")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor)
        }
    }.getOrNull() ?: runCatching {
        context.contentResolver.openInputStream(item.uri)?.use { stream ->
            ExifInterface(stream)
        }
    }.getOrNull()
}

private fun formatExifFieldLabel(rawName: String): String {
    return rawName
        .lowercase(Locale.US)
        .split('_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            when {
                part.length <= 3 -> part.uppercase(Locale.US)
                else -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
        }
}
