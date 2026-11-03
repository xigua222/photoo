package com.voicelike.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ColorCardTemporaryImage(
    val uri: Uri,
    val tempFile: File,
    val displayName: String
)

internal suspend fun importColorCardTemporaryImage(
    context: Context,
    sourceUri: Uri,
    tempPrefix: String = "color_card_reference"
): ColorCardTemporaryImage = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(sourceUri)
    val displayName = resolveColorCardTemporaryImageDisplayName(
        context = context,
        sourceUri = sourceUri,
        fallbackPrefix = tempPrefix
    )
    val extension = buildColorCardTemporaryImageExtension(
        displayName = displayName,
        mimeType = mimeType
    )
    val tempFile = File.createTempFile(tempPrefix, extension, context.cacheDir)
    resolver.openInputStream(sourceUri)?.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: error("无法读取参考图")
    ColorCardTemporaryImage(
        uri = sourceUri,
        tempFile = tempFile,
        displayName = displayName
    )
}

internal fun cleanupColorCardTemporaryImage(
    image: ColorCardTemporaryImage?
) {
    image?.tempFile?.takeIf(File::exists)?.delete()
}

private fun resolveColorCardTemporaryImageDisplayName(
    context: Context,
    sourceUri: Uri,
    fallbackPrefix: String
): String {
    val resolver = context.contentResolver
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    resolver.query(sourceUri, projection, null, null, null)?.use { cursor ->
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(columnIndex)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
    }
    return "${fallbackPrefix}_${System.currentTimeMillis()}.jpg"
}

private fun buildColorCardTemporaryImageExtension(
    displayName: String,
    mimeType: String?
): String {
    val displayExtension = displayName.substringAfterLast('.', "").trim().lowercase()
    if (displayExtension.isNotBlank() && displayExtension.length <= 5) {
        return ".${displayExtension}"
    }
    val mimeExtension = mimeType
        ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        ?.trim()
        ?.lowercase()
    if (!mimeExtension.isNullOrBlank()) {
        return ".${mimeExtension}"
    }
    return ".jpg"
}
