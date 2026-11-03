package com.voicelike.app.livesynctool

import android.content.ContentResolver
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

internal object LiveSyncToolExifSupport {
    private val defaultExcludedTags = setOf(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_PIXEL_X_DIMENSION,
        ExifInterface.TAG_PIXEL_Y_DIMENSION,
        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
        ExifInterface.TAG_STRIP_OFFSETS,
        ExifInterface.TAG_STRIP_BYTE_COUNTS,
        ExifInterface.TAG_ROWS_PER_STRIP
    )

    private val copyTags: List<String> by lazy {
        ExifInterface::class.java.fields
            .filter { field -> field.type == String::class.java && field.name.startsWith("TAG_") }
            .mapNotNull { field -> runCatching { field.get(null) as? String }.getOrNull() }
            .filterNot { it in defaultExcludedTags }
            .distinct()
    }

    fun copyExifMetadataToFile(
        resolver: ContentResolver,
        sourceUri: Uri,
        targetFile: File,
        excludedTags: Set<String> = emptySet()
    ) {
        runCatching {
            val sourceExif = openExifInterface(resolver, sourceUri) ?: return
            val targetExif = ExifInterface(targetFile.absolutePath)
            copyExifAttributes(sourceExif, targetExif, excludedTags)
        }
    }

    private fun openExifInterface(
        resolver: ContentResolver,
        uri: Uri
    ): ExifInterface? {
        return runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor)
            }
        }.getOrNull() ?: runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream)
            }
        }.getOrNull()
    }

    private fun copyExifAttributes(
        sourceExif: ExifInterface,
        targetExif: ExifInterface,
        excludedTags: Set<String>
    ) {
        copyTags.forEach { tag ->
            if (tag in excludedTags) return@forEach
            val value = sourceExif.getAttribute(tag) ?: return@forEach
            runCatching { targetExif.setAttribute(tag, value) }
        }
        targetExif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )
        targetExif.saveAttributes()
    }
}
