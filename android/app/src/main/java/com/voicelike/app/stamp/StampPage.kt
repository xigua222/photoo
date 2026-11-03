package com.voicelike.app.stamp

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.LocalHapticsEnabled
import com.voicelike.app.LocalAppLanguage
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.UnifiedSubpageTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StampPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val appStrings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val strings = remember(appLanguage) { getStampPageStrings(appLanguage) }
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val themePresets = remember(strings) { buildStampThemePresets(strings) }
    val overlayBitmap = remember(context) { loadStampOverlayBitmap(context) }

    var title by rememberSaveable { mutableStateOf(strings.defaultTitle) }
    var selectedThemeKey by rememberSaveable { mutableStateOf(themePresets.first().key) }
    var sourceUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var sourceBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loadingSource by remember { mutableStateOf(false) }
    var renderingPreview by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    val selectedTheme = remember(selectedThemeKey, themePresets) {
        themePresets.firstOrNull { it.key == selectedThemeKey } ?: themePresets.first()
    }
    val selectedSourceUri = remember(sourceUriString) { sourceUriString?.let(Uri::parse) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            sourceUriString = uri.toString()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val pendingUri = pendingCaptureUriString?.let(Uri::parse)
        pendingCaptureUriString = null
        if (pendingUri == null) return@rememberLauncherForActivityResult
        finalizeStampCameraCapture(context, pendingUri, success)
        if (success) {
            sourceUriString = pendingUri.toString()
        }
    }

    val launchCamera: () -> Unit = {
        val captureUri = createStampCameraCaptureUri(context)
        if (captureUri == null) {
            Toast.makeText(context, strings.saveFailed, Toast.LENGTH_SHORT).show()
        } else {
            pendingCaptureUriString = captureUri.toString()
            cameraLauncher.launch(captureUri)
        }
    }

    LaunchedEffect(selectedSourceUri) {
        if (selectedSourceUri == null) {
            sourceBitmap = null
            previewBitmap = null
            return@LaunchedEffect
        }
        sourceBitmap = null
        previewBitmap = null
        loadingSource = true
        val loaded = loadStampSourceBitmap(context, selectedSourceUri, maxSide = 1600)
        if (loaded == null) {
            Toast.makeText(context, strings.openImageFailed, Toast.LENGTH_SHORT).show()
            sourceUriString = null
            sourceBitmap = null
            previewBitmap = null
        } else {
            sourceBitmap = loaded
        }
        loadingSource = false
    }

    LaunchedEffect(sourceBitmap, title, selectedTheme, overlayBitmap) {
        val activeSource = sourceBitmap ?: run {
            previewBitmap = null
            return@LaunchedEffect
        }
        renderingPreview = true
        previewBitmap = withContext(Dispatchers.Default) {
            renderStampBitmap(
                source = activeSource,
                spec = buildStampRenderSpec(
                    title = title,
                    theme = selectedTheme
                ),
                outputWidth = 1080,
                overlayBitmap = overlayBitmap
            )
        }
        renderingPreview = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = strings.pageTitle,
            onBack = onBack,
            backContentDescription = appStrings.back
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StampPreviewCard(
                previewBitmap = previewBitmap,
                loading = loadingSource || renderingPreview,
                title = strings.introTitle,
                body = strings.introBody,
                loadingText = strings.previewLoading
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StampActionButton(
                    label = strings.chooseImage,
                    icon = Icons.Outlined.Add,
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        imagePicker.launch("image/*")
                    },
                    modifier = Modifier.weight(1f)
                )
                StampActionButton(
                    label = strings.openCamera,
                    icon = Icons.Outlined.PhotoCamera,
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        launchCamera()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.styleLabel,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    themePresets.forEach { preset ->
                        StampThemeChip(
                            label = preset.label,
                            selected = preset.key == selectedTheme.key,
                            accentColor = Color(preset.accentColor),
                            onClick = {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedThemeKey = preset.key
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(18) },
                label = { Text(strings.titleLabel) },
                placeholder = { Text(strings.titlePlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )

            Text(
                text = strings.outputHint,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val actionButtonShape = RoundedCornerShape(18.dp)
                val actionButtonBorder = if (isDark) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
                } else {
                    Color.Black.copy(alpha = 0.08f)
                }
                Button(
                    onClick = {
                        val exportUri = selectedSourceUri
                        if (exportUri == null) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, strings.selectImageFirst, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            exporting = true
                            val savedUri = exportStampToGallery(
                                context = context,
                                sourceUri = exportUri,
                                title = title,
                                theme = selectedTheme,
                                overlayBitmap = overlayBitmap
                            )
                            exporting = false
                            if (savedUri != null) {
                                Toast.makeText(context, strings.saveSuccess, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, strings.saveFailed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !exporting,
                    shape = actionButtonShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(1.dp, actionButtonBorder, actionButtonShape)
                ) {
                    Text(if (exporting) strings.saveProgress else strings.save)
                }
                Button(
                    onClick = {
                        val exportUri = selectedSourceUri
                        if (exportUri == null) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, strings.selectImageFirst, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            exporting = true
                            val savedUri = exportStampToGallery(
                                context = context,
                                sourceUri = exportUri,
                                title = title,
                                theme = selectedTheme,
                                overlayBitmap = overlayBitmap
                            )
                            exporting = false
                            if (savedUri != null) {
                                shareStampImage(
                                    context = context,
                                    uri = savedUri,
                                    title = title.ifBlank { strings.pageTitle }
                                )
                            } else {
                                Toast.makeText(context, strings.saveFailed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !exporting,
                    shape = actionButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(1.dp, actionButtonBorder, actionButtonShape)
                ) {
                    Text(if (exporting) strings.shareProgress else strings.share)
                }
            }
        }
    }
}

@Composable
private fun StampPreviewCard(
    previewBitmap: android.graphics.Bitmap?,
    loading: Boolean,
    title: String,
    body: String,
    loadingText: String
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 22.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    )
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = body,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (loading) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.6.dp
                    )
                    Text(
                        text = loadingText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StampActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            }
        ),
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StampThemeChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) {
                    accentColor.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    accentColor.copy(alpha = 0.52f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                },
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accentColor)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.92f else 0.72f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

private suspend fun exportStampToGallery(
    context: Context,
    sourceUri: Uri,
    title: String,
    theme: StampThemePreset,
    overlayBitmap: android.graphics.Bitmap?
): Uri? = withContext(Dispatchers.IO) {
    val source = loadStampSourceBitmap(context, sourceUri, maxSide = 2200) ?: return@withContext null
    val rendered = renderStampBitmap(
        source = source,
        spec = buildStampRenderSpec(title = title, theme = theme),
        outputWidth = 1720,
        overlayBitmap = overlayBitmap
    )
    saveStampBitmapToGallery(context, rendered)
}

private fun buildStampRenderSpec(
    title: String,
    theme: StampThemePreset
): StampRenderSpec {
    val now = Date()
    val shortDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(now)
    val serial = SimpleDateFormat("'No.' yyyyMMdd-HHmm", Locale.getDefault()).format(now)
    val valueLabel = when (theme.key) {
        "airmail" -> "52c"
        "blossom" -> "44c"
        "night" -> "88c"
        else -> "36c"
    }
    return StampRenderSpec(
        title = title.ifBlank { "Stamp" },
        subtitle = shortDate,
        valueLabel = valueLabel,
        serialLabel = serial,
        theme = theme
    )
}
