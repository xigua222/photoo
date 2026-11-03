package com.voicelike.app

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileOperationManager {

    /**
     * Moves a list of media items to a destination folder.
     * Returns a list of Uris that require user permission (Android 10+).
     * If list is empty, all operations were successful or handled internally.
     */
    suspend fun moveMedia(
        context: Context,
        items: List<MediaItem>,
        destinationFolder: String // e.g. "Pictures/Vacation"
    ): IntentSender? {
        return withContext(Dispatchers.IO) {
            val urisToRequest = mutableListOf<Uri>()

            // Check if we have MANAGE_EXTERNAL_STORAGE (Android 11+)
            val hasManageStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                if (hasManageStorage) {
                    // Use File API directly for silent moves
                    val targetDir = File(android.os.Environment.getExternalStorageDirectory(), destinationFolder)
                    if (!targetDir.exists()) targetDir.mkdirs()

                    items.forEach { item ->
                        val sourcePath = getPathFromUri(context, item.uri)
                        if (sourcePath != null) {
                            val sourceFile = File(sourcePath)
                            val destFile = File(targetDir, sourceFile.name)
                            if (sourceFile.exists()) {
                                if (sourceFile.renameTo(destFile)) {
                                    scanFile(context, destFile.absolutePath)
                                    scanFile(context, sourceFile.absolutePath)
                                } else {
                                    // Fallback to copy/delete if rename fails (cross-volume)
                                    try {
                                        sourceFile.inputStream().use { input ->
                                            destFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        if (destFile.exists() && destFile.length() == sourceFile.length()) {
                                            sourceFile.delete()
                                            scanFile(context, destFile.absolutePath)
                                            scanFile(context, sourceFile.absolutePath)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                    return@withContext null // Silent success
                }

                // Fallback to MediaStore logic if no Manage Storage permission
                val failedUris = mutableListOf<Uri>()
                
                items.forEach { item ->
                    try {
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, destinationFolder)
                        }
                        context.contentResolver.update(item.uri, values, null, null)
                    } catch (e: Exception) {
                        if (e is RecoverableSecurityException) {
                            failedUris.add(item.uri)
                        } else {
                             failedUris.add(item.uri)
                        }
                    }
                }
                
                if (failedUris.isNotEmpty()) {
                    return@withContext MediaStore.createWriteRequest(context.contentResolver, failedUris).intentSender
                }
                
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 (API 29)
                // RELATIVE_PATH exists. Same logic, but RecoverableSecurityException handling is slightly different.
                // Usually we catch RecoverableSecurityException and launch the intent sender from it.
                // But for batch?
                // Android 10 doesn't support createWriteRequest for batch as nicely as R.
                // We might have to do it one by one or guide user.
                // For simplicity, let's try update.
                
                 items.forEach { item ->
                    try {
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, destinationFolder)
                        }
                        context.contentResolver.update(item.uri, values, null, null)
                    } catch (e: RecoverableSecurityException) {
                        // In Android 10, we get an intent sender from the exception
                        // We can't easily batch this. We might return the FIRST one.
                        // This is a known pain point in Android 10.
                        if (urisToRequest.isEmpty()) {
                             // We can only handle one request at a time efficiently or show a dialog saying "We need permission"
                             // But wait, createWriteRequest was added in API 30.
                             // For API 29, we are supposed to catch RecoverableSecurityException.
                             return@withContext e.userAction.actionIntent.intentSender
                        }
                    }
                }
            } else {
                // Android 9 and below (Legacy File API)
                // We need WRITE_EXTERNAL_STORAGE (already granted in Main Activity)
                // We move files manually.
                val targetDir = File(android.os.Environment.getExternalStorageDirectory(), destinationFolder)
                if (!targetDir.exists()) targetDir.mkdirs()
                
                items.forEach { item ->
                    val sourceFile = File(getPathFromUri(context, item.uri) ?: return@forEach)
                    val destFile = File(targetDir, sourceFile.name)
                    
                    if (sourceFile.exists()) {
                        if (sourceFile.renameTo(destFile)) {
                            // Success, scan to update MediaStore
                            scanFile(context, destFile.absolutePath)
                            // Optionally delete old record if renameTo didn't handle it (it usually does for file system, but MediaStore needs refresh)
                            // Actually renameTo keeps the file, just moves it.
                            // We should scan OLD path (to remove) and NEW path (to add)
                            scanFile(context, sourceFile.absolutePath)
                        } else {
                            // Try copy delete
                            try {
                                sourceFile.inputStream().use { input ->
                                    destFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (destFile.exists() && destFile.length() == sourceFile.length()) {
                                    sourceFile.delete()
                                    scanFile(context, destFile.absolutePath)
                                    scanFile(context, sourceFile.absolutePath)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            
            null // Success or partial success (legacy)
        }
    }
    
    // Helper for Legacy Path
    private fun getPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(idx)
            }
        }
        return null
    }
    
    fun scanFile(context: Context, path: String) {
        android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }
}
