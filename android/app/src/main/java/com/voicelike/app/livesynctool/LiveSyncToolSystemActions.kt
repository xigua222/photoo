package com.voicelike.app.livesynctool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

internal object LiveSyncToolSystemActions {
    fun openInSystemGallery(
        context: Context,
        uri: Uri,
        mimeType: String = "image/*"
    ): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(intent)
        return true
    }

    fun buildDeleteIntentSender(
        context: Context,
        uris: List<Uri>
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    } else {
        null
    }

    fun deleteDirectly(
        context: Context,
        uris: List<Uri>
    ): Boolean {
        return uris.all { uri ->
            runCatching { context.contentResolver.delete(uri, null, null) > 0 }
                .getOrDefault(false)
        }
    }
}
