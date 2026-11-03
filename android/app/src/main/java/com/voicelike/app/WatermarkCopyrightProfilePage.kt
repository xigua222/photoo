package com.voicelike.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun WatermarkCopyrightProfilePage(
    authorName: String,
    authorEmail: String,
    yearText: String,
    onCancel: () -> Unit,
    onSave: (author: String, email: String, year: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var authorDraft by remember(authorName) { mutableStateOf(sanitizeAuthorInput(authorName)) }
    var emailDraft by remember(authorEmail) { mutableStateOf(authorEmail) }
    var yearDraft by remember(yearText) { mutableStateOf(yearText.filter { it.isDigit() }.take(4)) }
    var authorError by remember { mutableStateOf<String?>(null) }
    var yearError by remember { mutableStateOf<String?>(null) }

    val currentLanguage = LocalAppLanguage.current
    val newPageStrings = getNewPageStrings(currentLanguage)
    val strings = LocalAppStrings.current
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
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
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, outlineColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = newPageStrings.copyrightConfigurePageTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = primaryText
                )
                Text(
                    text = newPageStrings.copyrightConfigurePageDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )
                OutlinedTextField(
                    value = authorDraft,
                    onValueChange = {
                        val sanitized = sanitizeAuthorInput(it)
                        authorDraft = sanitized
                        authorError = if (it.any { ch -> !isAllowedAuthorChar(ch) }) {
                            newPageStrings.copyrightAuthorError
                        } else {
                            null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = authorError != null,
                    label = { Text(newPageStrings.copyrightAuthor) },
                    placeholder = { Text(newPageStrings.copyrightAuthorPlaceholder) },
                    supportingText = {
                        val message = authorError
                        if (!message.isNullOrBlank()) {
                            Text(message)
                        }
                    }
                )
                OutlinedTextField(
                    value = emailDraft,
                    onValueChange = { emailDraft = it.trim().take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(newPageStrings.copyrightEmailOptional) },
                    placeholder = { Text("name@example.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = yearDraft,
                    onValueChange = {
                        yearDraft = it.filter { ch -> ch.isDigit() }.take(4)
                        if (yearError != null) yearError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(newPageStrings.copyrightYear) },
                    placeholder = { Text(Calendar.getInstance().get(Calendar.YEAR).toString()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = yearError != null,
                    supportingText = {
                        val message = yearError
                        if (!message.isNullOrBlank()) {
                            Text(message)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = {
                        val normalizedAuthor = sanitizeAuthorInput(authorDraft).trim()
                        if (normalizedAuthor != authorDraft.trim()) {
                            authorDraft = normalizedAuthor
                            authorError = newPageStrings.copyrightAuthorError
                            return@Button
                        }
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val parsedYear = yearDraft.toIntOrNull()
                        val validYear = when {
                            parsedYear == null -> currentYear
                            parsedYear in 1900..9999 -> parsedYear
                            else -> null
                        }
                        if (validYear == null) {
                            yearError = newPageStrings.copyrightYearRangeError
                            return@Button
                        }
                        authorError = null
                        onSave(normalizedAuthor, emailDraft, validYear)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.done)
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryText)
                ) {
                    Text(strings.cancel)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun isAllowedAuthorChar(ch: Char): Boolean {
    return (ch.isLetterOrDigit() && ch.code < 128) || ch == ' '
}

private fun sanitizeAuthorInput(input: String): String {
    return input
        .filter(::isAllowedAuthorChar)
        .replace(Regex("\\s+"), " ")
        .trimStart()
        .take(80)
}
