package com.voicelike.app

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun loadColorCardCameraSavedMedia(
    context: Context
): List<MediaItem> = withContext(Dispatchers.IO) {
    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()
    selectionParts += "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
    selectionArgs += MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        selectionParts += "${MediaStore.MediaColumns.IS_TRASHED}=0"
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        selectionParts += "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        selectionArgs += "${COLOR_CARD_CAMERA_FOLDER.removeSuffix("/")}%"
    } else {
        selectionParts += "${MediaStore.Files.FileColumns.DATA} LIKE ?"
        selectionArgs += "%/Pictures/${COLOR_CARD_CAMERA_FOLDER.removePrefix("Pictures/").trimStart('/')}/%"
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATA,
        "datetaken"
    )
    context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        projection,
        selectionParts.joinToString(" AND "),
        selectionArgs.toTypedArray(),
        "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns._ID} DESC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
        val dateTakenCol = cursor.getColumnIndex("datetaken")
        buildList {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateAdded = cursor.getLong(dateCol)
                val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                add(
                    MediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                        type = "photo",
                        dateAdded = resolvePreferredCaptureTimeSec(dateTaken, dateAdded),
                        size = cursor.getLong(sizeCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        mimeType = cursor.getString(mimeCol).orEmpty(),
                        displayName = cursor.getString(nameCol).orEmpty(),
                        resolution = "${cursor.getInt(widthCol)}x${cursor.getInt(heightCol)}",
                        bucketName = cursor.getString(bucketNameCol).orEmpty(),
                        path = cursor.getString(pathCol)
                    )
                )
            }
        }
    } ?: emptyList()
}

internal fun buildColorCardCameraPreviewItems(
    capturedItems: List<ColorCardCameraCapturedItem>,
    savedItems: List<MediaItem>
): List<MediaItem> {
    val savedByUri = savedItems.associateBy { it.uri.toString() }
    val seenUris = LinkedHashSet<String>(capturedItems.size + savedItems.size)
    val merged = ArrayList<MediaItem>(capturedItems.size + savedItems.size)

    capturedItems.forEach { captured ->
        val candidateUri = captured.finalUri ?: captured.previewUri
        val item = savedByUri[candidateUri] ?: buildColorCardCameraPreviewMediaItem(candidateUri)
        if (seenUris.add(item.uri.toString())) {
            merged += item
        }
    }
    savedItems.forEach { item ->
        if (seenUris.add(item.uri.toString())) {
            merged += item
        }
    }
    return merged
}

internal fun deleteColorCardCameraMedia(
    context: Context,
    item: MediaItem
): Boolean {
    val deleted = runCatching {
        context.contentResolver.delete(item.uri, null, null) > 0
    }.getOrDefault(false)
    if (deleted) {
        return true
    }

    val path = item.path?.takeIf { it.isNotBlank() } ?: return false
    return runCatching { java.io.File(path).delete() }.getOrDefault(false)
}

internal fun findColorCardCameraPreviewIndex(
    items: List<MediaItem>,
    anchorUri: String?
): Int {
    if (items.isEmpty()) return 0
    if (anchorUri.isNullOrBlank()) return 0
    return items.indexOfFirst { it.uri.toString() == anchorUri }
        .takeIf { it >= 0 }
        ?: 0
}

internal fun nextColorCardCameraPreviewAnchorUri(
    items: List<MediaItem>,
    removedUri: String
): String? {
    val removedIndex = items.indexOfFirst { it.uri.toString() == removedUri }
    if (removedIndex < 0) return items.firstOrNull()?.uri?.toString()
    return items.getOrNull(removedIndex + 1)?.uri?.toString()
        ?: items.getOrNull(removedIndex - 1)?.uri?.toString()
}

internal fun MediaItem.isColorCardCameraSavedItem(): Boolean {
    return uri.scheme == "content" && id > 0L
}

internal fun openColorCardCameraMediaInAlbum(
    context: Context,
    item: MediaItem
): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(
            item.uri,
            if (item.mimeType.isNotBlank()) item.mimeType else "image/*"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent.resolveActivity(context.packageManager) == null) return false
    context.startActivity(intent)
    return true
}
