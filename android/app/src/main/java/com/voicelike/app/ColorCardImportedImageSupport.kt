package com.voicelike.app

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ColorCardImportedImageSource(
    val sessionId: Long,
    val uri: Uri,
    val tempFile: File,
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sourceMediaItem: MediaItem? = null,
    val initialPanelMode: ColorCardTracePanelMode? = null,
    val initialPaletteId: String? = null
)

internal suspend fun importColorCardEditingImage(
    context: Context,
    sourceUri: Uri
): ColorCardImportedImageSource = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val tempDir = File(context.cacheDir, "color_card_imports").apply { mkdirs() }
    val sessionId = System.currentTimeMillis()
    val displayName = queryColorCardPickedImageDisplayName(resolver, sourceUri).ifBlank {
        "image_${sessionId}.jpg"
    }
    val mimeType = resolver.getType(sourceUri)?.lowercase(Locale.ROOT).orEmpty()
    val extension = displayName.substringAfterLast('.', "").ifBlank {
        when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }
    val tempFile = File(tempDir, "color_card_${sessionId}.$extension")
    resolver.openInputStream(sourceUri)?.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IllegalStateException("\u8bfb\u53d6\u56fe\u7247\u5931\u8d25")

    val importedUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(tempFile.absolutePath, boundsOptions)
    val decodedWidth = boundsOptions.outWidth.coerceAtLeast(0)
    val decodedHeight = boundsOptions.outHeight.coerceAtLeast(0)
    val resolvedSourceMediaItem = resolvePickedMediaItem(context, sourceUri)
        ?: resolveImportedMotionPhotoFallback(
            context = context,
            sourceUri = sourceUri,
            displayName = displayName,
            mimeType = mimeType,
            fileSize = tempFile.length(),
            width = decodedWidth,
            height = decodedHeight
        )

    ColorCardImportedImageSource(
        sessionId = sessionId,
        uri = importedUri,
        tempFile = tempFile,
        displayName = displayName,
        mimeType = mimeType.ifBlank { "image/*" },
        width = decodedWidth,
        height = decodedHeight,
        sourceMediaItem = resolvedSourceMediaItem
    )
}

internal fun cleanupColorCardEditingImageSource(source: ColorCardImportedImageSource?) {
    if (source == null) return
    runCatching { source.tempFile.delete() }
}

internal fun ColorCardImportedImageSource.toImportedMediaItem(): MediaItem {
    val resolvedWidth = width.takeIf { it > 0 } ?: sourceMediaItem?.width ?: 0
    val resolvedHeight = height.takeIf { it > 0 } ?: sourceMediaItem?.height ?: 0
    val resolutionText = if (resolvedWidth > 0 && resolvedHeight > 0) {
        "${resolvedWidth}x$resolvedHeight"
    } else {
        ""
    }
    return MediaItem(
        id = -sessionId,
        uri = uri,
        type = "photo",
        dateAdded = sourceMediaItem?.dateAdded ?: System.currentTimeMillis() / 1000L,
        size = tempFile.length(),
        width = resolvedWidth,
        height = resolvedHeight,
        mimeType = mimeType.ifBlank { sourceMediaItem?.mimeType ?: "image/*" },
        displayName = displayName,
        resolution = resolutionText,
        isLivePhoto = sourceMediaItem?.isLivePhoto == true,
        livePhotoVideoUri = sourceMediaItem?.livePhotoVideoUri,
        livePhotoVideoId = sourceMediaItem?.livePhotoVideoId,
        path = tempFile.absolutePath,
        bucketName = sourceMediaItem?.bucketName.orEmpty(),
        duration = sourceMediaItem?.duration ?: 0L,
        isMotionPhoto = sourceMediaItem?.isMotionPhoto == true,
        motionPhotoVideoOffset = sourceMediaItem?.motionPhotoVideoOffset ?: 0L,
        motionPhotoVideoLength = sourceMediaItem?.motionPhotoVideoLength ?: 0L
    )
}

private fun queryColorCardPickedImageDisplayName(
    resolver: ContentResolver,
    uri: Uri
): String {
    return runCatching {
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(displayNameIndex).orEmpty()
            } else {
                ""
            }
        }
    }.getOrNull().orEmpty()
}

private fun resolveImportedMotionPhotoFallback(
    context: Context,
    sourceUri: Uri,
    displayName: String,
    mimeType: String,
    fileSize: Long,
    width: Int,
    height: Int
): MediaItem? {
    val motionInfo = MotionPhotoHelper.checkMotionPhoto(context, sourceUri)
    if (!motionInfo.isMotionPhoto) return null

    return MediaItem(
        id = Long.MIN_VALUE,
        uri = sourceUri,
        type = "photo",
        dateAdded = System.currentTimeMillis() / 1000L,
        size = fileSize,
        width = width,
        height = height,
        mimeType = mimeType.ifBlank { "image/*" },
        displayName = displayName,
        resolution = if (width > 0 && height > 0) "${width}x$height" else "",
        isMotionPhoto = true,
        motionPhotoVideoOffset = motionInfo.videoOffset,
        motionPhotoVideoLength = motionInfo.videoLength,
        path = null
    )
}
