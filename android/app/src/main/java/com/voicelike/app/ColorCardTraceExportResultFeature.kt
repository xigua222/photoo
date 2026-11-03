package com.voicelike.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max

private const val COLOR_CARD_TRACE_EXPORT_PREVIEW_MAX_SIDE = 1440

internal data class ColorCardTraceExportSuccessPayload(
    val beforeBitmap: Bitmap,
    val afterBitmap: Bitmap,
    val savedUri: Uri,
    val displayName: String,
    val copyLabel: String?,
    val copyText: String?
)

internal fun buildColorCardTraceExportSuccessPayload(
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap,
    savedUri: Uri,
    displayName: String,
    recipe: SavedColorCardRecipe?
): ColorCardTraceExportSuccessPayload {
    val afterPreview = scaleColorCardTraceExportPreviewBitmap(afterBitmap)
    val beforePreview = beforeBitmap
        ?.takeIf { !it.isRecycled }
        ?.let { bitmap ->
            if (bitmap.width == afterPreview.width && bitmap.height == afterPreview.height) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                Bitmap.createScaledBitmap(bitmap, afterPreview.width, afterPreview.height, true)
            }
        }
        ?: afterPreview.copy(Bitmap.Config.ARGB_8888, false)
    return ColorCardTraceExportSuccessPayload(
        beforeBitmap = beforePreview,
        afterBitmap = afterPreview,
        savedUri = savedUri,
        displayName = displayName,
        copyLabel = recipe?.mode?.let { mode ->
            when (mode) {
                ColorCardRecipeMode.Filter -> "\u590d\u5236\u6ee4\u955c"
                ColorCardRecipeMode.Trace -> "\u590d\u5236\u8272\u5361"
            }
        },
        copyText = recipe?.let { savedRecipe ->
            when (savedRecipe.mode) {
                ColorCardRecipeMode.Filter -> savedRecipe.candidateTitle.ifBlank { savedRecipe.title }
                ColorCardRecipeMode.Trace -> buildPaletteDetailText(
                    SavedColorCardPalette(
                        id = savedRecipe.id,
                        createdAt = savedRecipe.createdAt,
                        sourceMediaId = null,
                        colors = savedRecipe.colors,
                        title = savedRecipe.candidateTitle.ifBlank { savedRecipe.title },
                        sourceTag = savedRecipe.sourceTag
                    )
                )
            }
        }
    )
}

internal fun ColorCardTraceExportSuccessPayload.release() {
    if (!beforeBitmap.isRecycled) {
        beforeBitmap.recycle()
    }
    if (!afterBitmap.isRecycled) {
        afterBitmap.recycle()
    }
}

internal fun shareColorCardRenderedImage(
    context: Context,
    uri: Uri,
    displayName: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, displayName)
        putExtra(Intent.EXTRA_SUBJECT, displayName)
        clipData = ClipData.newUri(context.contentResolver, displayName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Share")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    }
}

@Composable
internal fun ColorCardTraceExportSuccessDialog(
    payload: ColorCardTraceExportSuccessPayload,
    onBackToEditing: () -> Unit,
    onChangeImage: () -> Unit,
    onExitEditing: () -> Unit,
    onCopyRecipe: () -> Unit,
    onShareImage: () -> Unit
) {
    BackHandler(onBack = onBackToEditing)

    val accentColor = Color(0xFFDC2626)
    val configuration = LocalConfiguration.current
    val maxContentWidth = minOf(configuration.screenWidthDp.dp - 32.dp, 460.dp)
    val imageHeight = minOf((configuration.screenHeightDp * 0.5f).dp, 480.dp)
    val isDark = isSystemInDarkTheme()
    val previewBackground = if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
    }
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onBackToEditing,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            UnifiedSubpageTopBar(
                title = "",
                onBack = onBackToEditing,
                titleColor = primaryText,
                backButtonContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                backButtonIconColor = primaryText
            ) {
                ColorCardTraceExportExitAction(
                    accentColor = accentColor,
                    onClick = onExitEditing
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = maxContentWidth)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "导出成功！",
                        color = primaryText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    ColorCardTraceExportBeforeAfterPreview(
                        beforeBitmap = payload.beforeBitmap,
                        afterBitmap = payload.afterBitmap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = imageHeight)
                            .clip(RoundedCornerShape(22.dp))
                            .background(previewBackground)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!payload.copyText.isNullOrBlank()) {
                            ColorCardTraceExportActionButton(
                                label = payload.copyLabel ?: "\u590d\u5236\u914d\u65b9",
                                enabled = true,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                modifier = Modifier.weight(1f),
                                onClick = onCopyRecipe
                            )
                        }
                        ColorCardTraceExportActionButton(
                            label = "\u5206\u4eab\u65b0\u56fe\u7247",
                            enabled = true,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            modifier = if (payload.copyText.isNullOrBlank()) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier.weight(1f)
                            },
                            onClick = onShareImage
                        )
                    }

                    ColorCardTraceExportActionButton(
                        label = "\u66f4\u6362\u56fe\u7247",
                        enabled = true,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onChangeImage
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ColorCardTraceExportExitAction(
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "\u9000\u51fa\u8c03\u8272",
            tint = accentColor
        )
        Text(
            text = "\u9000\u51fa\u8c03\u8272",
            color = accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun ColorCardTraceExportBeforeAfterPreview(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var revealFraction by remember(beforeBitmap, afterBitmap) { mutableFloatStateOf(0.5f) }
    Box(
        modifier = modifier.pointerInput(beforeBitmap, afterBitmap) {
            detectDragGestures(
                onDragStart = { startOffset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    revealFraction = (startOffset.x / width).coerceIn(0f, 1f)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    revealFraction = (revealFraction + (dragAmount.x / width)).coerceIn(0f, 1f)
                }
            )
        }
    ) {
        Image(
            bitmap = afterBitmap.asImageBitmap(),
            contentDescription = "after",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            bitmap = beforeBitmap.asImageBitmap(),
            contentDescription = "before",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = size.width * revealFraction) {
                        this@drawWithContent.drawContent()
                    }
                }
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dividerX = size.width * revealFraction
            val handleRadius = max(12f, size.minDimension * 0.048f)
            drawLine(
                color = Color.White.copy(alpha = 0.92f),
                start = Offset(dividerX, 0f),
                end = Offset(dividerX, size.height),
                strokeWidth = max(2f, size.width * 0.0042f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = handleRadius,
                center = Offset(dividerX, size.height / 2f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.12f),
                radius = handleRadius,
                center = Offset(dividerX, size.height / 2f),
                style = Stroke(width = max(1.5f, size.minDimension * 0.005f))
            )
        }
        val labelBackground = Color.Black.copy(alpha = 0.48f)
        Text(
            text = "BEFORE",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(labelBackground)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Text(
            text = "AFTER",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(labelBackground)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ColorCardTraceExportActionButton(
    label: String,
    enabled: Boolean,
    primaryText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        secondaryText.copy(alpha = 0.10f)
    }
    val contentColor = if (enabled) primaryText else secondaryText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun scaleColorCardTraceExportPreviewBitmap(bitmap: Bitmap): Bitmap {
    if (bitmap.isRecycled) return bitmap
    val width = bitmap.width.coerceAtLeast(1)
    val height = bitmap.height.coerceAtLeast(1)
    val longestSide = max(width, height)
    if (longestSide <= COLOR_CARD_TRACE_EXPORT_PREVIEW_MAX_SIDE) {
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
    val scale = COLOR_CARD_TRACE_EXPORT_PREVIEW_MAX_SIDE.toFloat() / longestSide.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
