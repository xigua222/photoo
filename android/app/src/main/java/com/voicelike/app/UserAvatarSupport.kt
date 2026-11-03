package com.voicelike.app

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val USER_AVATAR_TYPE_GIRL = "girl"
const val USER_AVATAR_TYPE_BOY = "boy"
const val USER_AVATAR_TYPE_CUSTOM = "custom"

private const val USER_AVATAR_DIR = "user_avatar"
private const val USER_AVATAR_FILE_PREFIX = "avatar"

fun resolveUserAvatarModel(
    avatarType: String,
    customAvatarPath: String?
): Any {
    val customFile = customAvatarPath
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf { it.exists() }
    return when {
        customFile != null -> customFile
        avatarType == USER_AVATAR_TYPE_BOY -> R.drawable.boy
        else -> R.drawable.girl
    }
}

@Composable
fun AvatarTypeSelectorRow(
    avatarType: String,
    onAvatarTypeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = avatarType == USER_AVATAR_TYPE_GIRL,
            onClick = { onAvatarTypeChange(USER_AVATAR_TYPE_GIRL) },
            label = { Text(strings.avatarGirl) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color.Black,
                selectedLabelColor = Color.White,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onBackground
            ),
            border = null,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(48.dp)
        )
        FilterChip(
            selected = avatarType == USER_AVATAR_TYPE_BOY,
            onClick = { onAvatarTypeChange(USER_AVATAR_TYPE_BOY) },
            label = { Text(strings.avatarBoy) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color.Black,
                selectedLabelColor = Color.White,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onBackground
            ),
            border = null,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(48.dp)
        )
    }
}

internal suspend fun persistUserAvatarImage(
    context: Context,
    sourceUri: Uri
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(sourceUri)?.lowercase(Locale.ROOT).orEmpty()
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
        val targetDir = File(context.filesDir, USER_AVATAR_DIR).apply { mkdirs() }
        val tempFile = File(targetDir, "$USER_AVATAR_FILE_PREFIX.tmp")
        val targetFile = File(
            targetDir,
            "${USER_AVATAR_FILE_PREFIX}_${System.currentTimeMillis()}.$extension"
        )
        resolver.openInputStream(sourceUri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            runCatching { tempFile.delete() }
        }
        targetDir.listFiles()?.forEach { file ->
            if (
                file != targetFile &&
                file.name.startsWith(USER_AVATAR_FILE_PREFIX)
            ) {
                runCatching { file.delete() }
            }
        }
        targetFile.absolutePath
    }.getOrNull()
}
