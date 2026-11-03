package com.voicelike.app

import android.content.Context
import android.net.Uri
import java.io.File

data class WatermarkExportPayload(
    val outputFile: File,
    val outputMimeType: String,
    val exifSourceUri: Uri?,
    val preserveEmbeddedMetadata: Boolean
)

private fun WatermarkCopyrightSettings.isAnyEnabled(): Boolean {
    return writeCopyrightInfo || noAiTrainingDeclaration || invisibleWatermark
}

suspend fun buildWatermarkExportPayload(
    context: Context,
    renderedFile: File,
    renderedMimeType: String,
    originalSourceUri: Uri?,
    copyrightSettings: WatermarkCopyrightSettings
): WatermarkExportPayload {
    if (!copyrightSettings.isAnyEnabled()) {
        return WatermarkExportPayload(
            outputFile = renderedFile,
            outputMimeType = renderedMimeType,
            exifSourceUri = originalSourceUri,
            preserveEmbeddedMetadata = false
        )
    }
    val prepared = prepareWatermarkCopyrightOutput(
        context = context,
        renderedFile = renderedFile,
        renderedMimeType = renderedMimeType,
        originalSourceUri = originalSourceUri,
        settings = copyrightSettings
    )
    return WatermarkExportPayload(
        outputFile = prepared.outputFile,
        outputMimeType = prepared.outputMimeType,
        exifSourceUri = prepared.sourceUriForSave,
        preserveEmbeddedMetadata = true
    )
}
