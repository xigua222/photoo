package com.voicelike.app

import android.content.ContentResolver
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

private val PRESERVED_EXIF_EXCLUDED_FIELD_NAMES = setOf(
    "TAG_ORIENTATION",
    "TAG_IMAGE_WIDTH",
    "TAG_IMAGE_LENGTH",
    "TAG_PIXEL_X_DIMENSION",
    "TAG_PIXEL_Y_DIMENSION",
    "TAG_JPEG_INTERCHANGE_FORMAT",
    "TAG_JPEG_INTERCHANGE_FORMAT_LENGTH",
    "TAG_STRIP_OFFSETS",
    "TAG_STRIP_BYTE_COUNTS",
    "TAG_ROWS_PER_STRIP"
)

private val PRESERVED_EXIF_TAGS: List<String> by lazy {
    ExifInterface::class.java.fields
        .asSequence()
        .filter { field ->
            field.type == String::class.java &&
                field.name.startsWith("TAG_") &&
                field.name !in PRESERVED_EXIF_EXCLUDED_FIELD_NAMES &&
                !field.name.contains("THUMBNAIL")
        }
        .mapNotNull { field ->
            runCatching { field.get(null) as? String }.getOrNull()
        }
        .distinct()
        .toList()
}

internal fun openPreservedExifInterface(
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

internal fun copyPreservedExifMetadata(
    resolver: ContentResolver,
    sourceUri: Uri?,
    targetUri: Uri
) {
    if (sourceUri == null) return
    val sourceExif = openPreservedExifInterface(resolver, sourceUri) ?: return
    resolver.openFileDescriptor(targetUri, "rw")?.use { descriptor ->
        val targetExif = ExifInterface(descriptor.fileDescriptor)
        copyPreservedExifAttributes(sourceExif, targetExif)
    } ?: throw IllegalStateException("无法写入导出 EXIF")
}

internal fun copyPreservedExifMetadataToFile(
    resolver: ContentResolver,
    sourceUri: Uri?,
    targetFile: File
) {
    if (sourceUri == null || !targetFile.exists()) return
    val sourceExif = openPreservedExifInterface(resolver, sourceUri) ?: return
    val targetExif = ExifInterface(targetFile.absolutePath)
    copyPreservedExifAttributes(sourceExif, targetExif)
}

internal fun copyPreservedExifAttributes(
    sourceExif: ExifInterface,
    targetExif: ExifInterface
) {
    PRESERVED_EXIF_TAGS.forEach { tag ->
        sourceExif.getAttribute(tag)?.let { value ->
            targetExif.setAttribute(tag, value)
        }
    }
    targetExif.setAttribute(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL.toString()
    )
    targetExif.saveAttributes()
}
