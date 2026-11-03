package com.voicelike.app

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private const val COPYRIGHT_PREF_WRITE_INFO = "wm_copyright_write_info"
private const val COPYRIGHT_PREF_NO_AI = "wm_copyright_no_ai"
private const val COPYRIGHT_PREF_INVISIBLE = "wm_copyright_invisible"
private const val COPYRIGHT_PREF_AUTHOR = "wm_copyright_author"
private const val COPYRIGHT_PREF_EMAIL = "wm_copyright_email"
private const val COPYRIGHT_PREF_YEAR = "wm_copyright_year"
private const val COPYRIGHT_PREF_DISCLAIMER_ACK = "wm_copyright_disclaimer_ack"

data class WatermarkCopyrightSettings(
    val writeCopyrightInfo: Boolean,
    val noAiTrainingDeclaration: Boolean,
    val invisibleWatermark: Boolean,
    val authorName: String,
    val authorEmail: String,
    val year: Int
)

@Stable
class WatermarkCopyrightState internal constructor(
    private val prefs: SharedPreferences
) {
    var writeCopyrightInfo by mutableStateOf(prefs.getBoolean(COPYRIGHT_PREF_WRITE_INFO, false))
        private set
    var noAiTrainingDeclaration by mutableStateOf(prefs.getBoolean(COPYRIGHT_PREF_NO_AI, false))
        private set
    var invisibleWatermark by mutableStateOf(prefs.getBoolean(COPYRIGHT_PREF_INVISIBLE, false))
        private set
    var authorName by mutableStateOf(prefs.getString(COPYRIGHT_PREF_AUTHOR, "").orEmpty())
        private set
    var authorEmail by mutableStateOf(prefs.getString(COPYRIGHT_PREF_EMAIL, "").orEmpty())
        private set
    var yearText by mutableStateOf(
        prefs.getInt(COPYRIGHT_PREF_YEAR, Calendar.getInstance().get(Calendar.YEAR)).toString()
    )
        private set
    var disclaimerAcknowledged by mutableStateOf(prefs.getBoolean(COPYRIGHT_PREF_DISCLAIMER_ACK, false))
        private set

    fun updateWriteCopyrightInfo(value: Boolean) {
        writeCopyrightInfo = value
        prefs.edit().putBoolean(COPYRIGHT_PREF_WRITE_INFO, value).apply()
    }

    fun updateNoAiTrainingDeclaration(value: Boolean) {
        noAiTrainingDeclaration = value
        prefs.edit().putBoolean(COPYRIGHT_PREF_NO_AI, value).apply()
    }

    fun updateInvisibleWatermark(value: Boolean) {
        invisibleWatermark = value
        prefs.edit().putBoolean(COPYRIGHT_PREF_INVISIBLE, value).apply()
    }

    fun updateAuthorName(value: String) {
        authorName = value.trimStart().take(80)
        prefs.edit().putString(COPYRIGHT_PREF_AUTHOR, authorName).apply()
    }

    fun updateAuthorEmail(value: String) {
        authorEmail = value.trim().take(80)
        prefs.edit().putString(COPYRIGHT_PREF_EMAIL, authorEmail).apply()
    }

    fun updateYearText(value: String) {
        val digits = value.filter { it.isDigit() }.take(4)
        yearText = digits
        val parsed = digits.toIntOrNull()
        if (parsed != null && parsed in 1900..9999) {
            prefs.edit().putInt(COPYRIGHT_PREF_YEAR, parsed).apply()
        }
    }

    fun acknowledgeDisclaimer() {
        disclaimerAcknowledged = true
        prefs.edit().putBoolean(COPYRIGHT_PREF_DISCLAIMER_ACK, true).apply()
    }

    fun anyProtectionEnabled(): Boolean {
        return writeCopyrightInfo || noAiTrainingDeclaration || invisibleWatermark
    }

    fun currentSettings(): WatermarkCopyrightSettings {
        val defaultYear = Calendar.getInstance().get(Calendar.YEAR)
        val parsedYear = yearText.toIntOrNull()?.coerceIn(1900, 9999) ?: defaultYear
        return WatermarkCopyrightSettings(
            writeCopyrightInfo = writeCopyrightInfo,
            noAiTrainingDeclaration = noAiTrainingDeclaration,
            invisibleWatermark = invisibleWatermark,
            authorName = authorName.trim(),
            authorEmail = authorEmail.trim(),
            year = parsedYear
        )
    }
}

@Composable
fun rememberWatermarkCopyrightState(
    prefs: SharedPreferences
): WatermarkCopyrightState {
    return remember(prefs) { WatermarkCopyrightState(prefs) }
}

@Composable
fun WatermarkCopyrightPanel(
    state: WatermarkCopyrightState,
    showIdentityFields: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showNoAiTipDialog by remember { mutableStateOf(false) }
    val currentLanguage = LocalAppLanguage.current
    val newPageStrings = getNewPageStrings(currentLanguage)
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
    }
    val outlineColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, outlineColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = newPageStrings.copyrightDeclarationTitle,
                style = MaterialTheme.typography.titleSmall,
                color = primaryText
            )
            Text(
                text = newPageStrings.copyrightDeclarationDesc,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                ),
                color = secondaryText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(newPageStrings.copyrightWriteInfo, color = primaryText)
                Switch(
                    checked = state.writeCopyrightInfo,
                    onCheckedChange = { state.updateWriteCopyrightInfo(it) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(newPageStrings.copyrightNoAiDeclaration, color = primaryText)
                    IconButton(
                        onClick = { showNoAiTipDialog = true },
                        modifier = Modifier.height(22.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_no_ai_info),
                            contentDescription = newPageStrings.copyrightNoAiDialogTitle,
                            tint = secondaryText
                        )
                    }
                }
                Switch(
                    checked = state.noAiTrainingDeclaration,
                    onCheckedChange = { state.updateNoAiTrainingDeclaration(it) }
                )
            }
            if (showNoAiTipDialog) {
                AlertDialog(
                    onDismissRequest = { showNoAiTipDialog = false },
                    title = { Text(newPageStrings.copyrightNoAiDialogTitle) },
                    text = {
                        Text(
                            text = newPageStrings.copyrightNoAiDialogDesc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showNoAiTipDialog = false }) {
                            Text(newPageStrings.copyrightGotIt)
                        }
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(newPageStrings.copyrightInvisibleWatermark, color = primaryText)
                Switch(
                    checked = state.invisibleWatermark,
                    onCheckedChange = { state.updateInvisibleWatermark(it) }
                )
            }

            if (showIdentityFields) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = state.authorName,
                    onValueChange = { state.updateAuthorName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(newPageStrings.copyrightAuthorIdentity) },
                    placeholder = { Text(newPageStrings.copyrightAuthorPlaceholder) }
                )
                OutlinedTextField(
                    value = state.authorEmail,
                    onValueChange = { state.updateAuthorEmail(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(newPageStrings.copyrightEmailOptional) },
                    placeholder = { Text("name@example.com") }
                )
                OutlinedTextField(
                    value = state.yearText,
                    onValueChange = { state.updateYearText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(newPageStrings.copyrightYear) },
                    placeholder = { Text(Calendar.getInstance().get(Calendar.YEAR).toString()) }
                )
            } else {
                Text(
                    text = newPageStrings.copyrightConfigureHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )
            }
        }
    }
}
