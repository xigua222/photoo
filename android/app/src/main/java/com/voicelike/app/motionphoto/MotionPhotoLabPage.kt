package com.voicelike.app.motionphoto

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.UnifiedSubpageTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot

internal const val MOTION_PHOTO_FILTER_ENTRY_ID = "builtin_motion_photo:color-awakening"
private const val MOTION_PHOTO_PREVIEW_MAX_LONG_SIDE = 720

@Composable
internal fun MotionPhotoLabPage(
    sourceBitmap: Bitmap?,
    sourceFile: File?,
    displayName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val state = rememberMotionPhotoLabState(context)
    var revealPattern by remember { mutableStateOf(MotionPhotoRevealPattern.SinglePoint) }
    val spec = remember(revealPattern) { MotionPhotoRenderSpec(revealPattern = revealPattern) }
    val previewSourceBitmap by produceState<Bitmap?>(initialValue = null, sourceFile, sourceBitmap) {
        value = withContext(Dispatchers.Default) {
            MotionPhotoFrameRenderer.decodeSourceBitmap(
                sourceFile = sourceFile,
                fallbackBitmap = sourceBitmap,
                maxLongSide = MOTION_PHOTO_PREVIEW_MAX_LONG_SIDE
            )
        }
    }
    val grayscalePreviewBitmap by produceState<Bitmap?>(initialValue = null, previewSourceBitmap) {
        val safeSource = previewSourceBitmap?.takeIf { !it.isRecycled } ?: run {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            MotionPhotoFrameRenderer.createGrayscaleBitmap(safeSource)
        }
    }
    val revealProgress = remember { Animatable(0f) }
    var exportProgressLabel by remember { mutableStateOf<String?>(null) }
    var exportResult by remember { mutableStateOf<MotionPhotoExportResult?>(null) }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(previewSourceBitmap, revealPattern) {
        while (previewSourceBitmap != null && !previewSourceBitmap!!.isRecycled) {
            revealProgress.snapTo(0f)
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = spec.durationMs.toInt(), easing = FastOutSlowInEasing)
            )
            delay(MotionPhotoFrameRenderer.EFFECT_LOOP_PAUSE_MS)
        }
    }

    val surface = MaterialTheme.colorScheme.background
    val panel = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    val border = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = primaryText.copy(alpha = 0.68f)
    val sessionPath = state.uiState.preparedSession?.sessionDir?.absolutePath

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surface,
                        surface.copy(alpha = 0.98f),
                        surface.copy(alpha = 0.96f)
                    )
                )
            )
    ) {
        UnifiedSubpageTopBar(
            title = "Motion Photo",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "彩色苏醒",
                color = primaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "用户看到的是一个创意滤镜，底层则是独立的 motionphoto 文件夹、独立导出链路和单独的 Motion Photo 封装。",
                color = secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            MotionPhotoRevealPatternPicker(
                selectedPattern = revealPattern,
                enabled = exportProgressLabel == null,
                onPatternSelected = { revealPattern = it }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(panel)
                    .border(1.dp, border, RoundedCornerShape(28.dp))
                    .padding(12.dp)
            ) {
                when {
                    previewSourceBitmap != null && grayscalePreviewBitmap != null -> {
                        MotionPhotoRevealPreview(
                            colorBitmap = previewSourceBitmap!!,
                            grayscaleBitmap = grayscalePreviewBitmap!!,
                            revealProgress = revealProgress.value,
                            revealPattern = revealPattern,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
            MotionPhotoActionPanel(
                sessionPath = sessionPath,
                exportProgressLabel = exportProgressLabel,
                exportedUri = exportResult?.savedUri,
                isExporting = exportProgressLabel != null,
                onPrepareWorkspace = {
                    state.prepareWorkspace(displayName)
                    Toast.makeText(context, "已创建独立工作目录", Toast.LENGTH_SHORT).show()
                },
                onExport = {
                    if (exportProgressLabel != null) return@MotionPhotoActionPanel
                    scope.launch {
                        val session = state.uiState.preparedSession ?: state.prepareWorkspace(displayName)
                        runCatching {
                            exportProgressLabel = "正在准备导出"
                            withContext(Dispatchers.Default) {
                                MotionPhotoExportCoordinator.export(
                                    context = context.applicationContext,
                                    sourceFile = sourceFile,
                                    fallbackBitmap = previewSourceBitmap ?: sourceBitmap,
                                    displayName = displayName,
                                    session = session,
                                    spec = spec,
                                    onProgress = { progress ->
                                        scope.launch {
                                            exportProgressLabel = progress
                                        }
                                    }
                                )
                            }
                        }.onSuccess { result ->
                            exportResult = result
                            Toast.makeText(context, "Motion Photo 已保存", Toast.LENGTH_SHORT).show()
                        }.onFailure { error ->
                            Toast.makeText(
                                context,
                                error.toDynamicPhotoExportToastMessage("Motion Photo"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        exportProgressLabel = null
                    }
                },
                onShare = {
                    exportResult?.let { result ->
                        shareMotionPhoto(context, result.savedUri, result.displayName)
                    }
                }
            )
            exportResult?.let { result ->
                MotionPhotoExportResultCard(
                    result = result,
                    primaryText = primaryText,
                    secondaryText = secondaryText
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MotionPhotoRevealPreview(
    colorBitmap: Bitmap,
    grayscaleBitmap: Bitmap,
    revealProgress: Float,
    revealPattern: MotionPhotoRevealPattern,
    modifier: Modifier = Modifier
) {
    val colorImage = remember(colorBitmap) { colorBitmap.asImageBitmap() }
    val grayscaleImage = remember(grayscaleBitmap) { grayscaleBitmap.asImageBitmap() }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.08f))
    ) {
        drawImage(grayscaleImage, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
        val radius = MotionPhotoFrameRenderer.resolveRevealRadius(
            width = size.width,
            height = size.height,
            progress = revealProgress
        )
        val revealAnchors = MotionPhotoFrameRenderer.resolveRevealAnchors(revealPattern)
        val revealPath = Path().apply {
            revealAnchors.forEach { anchor ->
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = (size.width * anchor.xFraction) - radius,
                        top = (size.height * anchor.yFraction) - radius,
                        right = (size.width * anchor.xFraction) + radius,
                        bottom = (size.height * anchor.yFraction) + radius
                    )
                )
            }
        }
        clipPath(revealPath) {
            drawImage(colorImage, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
        }
        revealAnchors.forEach { anchor ->
            drawCircle(
                color = Color.White.copy(alpha = 0.72f),
                center = Offset(size.width * anchor.xFraction, size.height * anchor.yFraction),
                radius = radius,
                style = Stroke(width = size.minDimension * 0.01f)
            )
        }
    }
}

@Composable
private fun MotionPhotoRevealPatternPicker(
    selectedPattern: MotionPhotoRevealPattern,
    enabled: Boolean,
    onPatternSelected: (MotionPhotoRevealPattern) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "苏醒方案",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MotionPhotoRevealPatternChip(
                label = "1 点",
                selected = selectedPattern == MotionPhotoRevealPattern.SinglePoint,
                enabled = enabled,
                onClick = { onPatternSelected(MotionPhotoRevealPattern.SinglePoint) }
            )
            MotionPhotoRevealPatternChip(
                label = "2 点",
                selected = selectedPattern == MotionPhotoRevealPattern.DualPoint,
                enabled = enabled,
                onClick = { onPatternSelected(MotionPhotoRevealPattern.DualPoint) }
            )
            MotionPhotoRevealPatternChip(
                label = "3 点",
                selected = selectedPattern == MotionPhotoRevealPattern.TriplePoint,
                enabled = enabled,
                onClick = { onPatternSelected(MotionPhotoRevealPattern.TriplePoint) }
            )
        }
    }
}

@Composable
private fun MotionPhotoRevealPatternChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val primaryText = MaterialTheme.colorScheme.onBackground
    val backgroundColor = when {
        selected -> primaryText.copy(alpha = 0.16f)
        enabled -> primaryText.copy(alpha = 0.08f)
        else -> primaryText.copy(alpha = 0.04f)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = primaryText.copy(alpha = if (selected) 0.22f else 0.10f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) primaryText else primaryText.copy(alpha = 0.42f),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun MotionPhotoActionPanel(
    sessionPath: String?,
    exportProgressLabel: String?,
    exportedUri: Uri?,
    isExporting: Boolean,
    onPrepareWorkspace: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit
) {
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = primaryText.copy(alpha = 0.68f)
    val panelColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    val borderColor = primaryText.copy(alpha = 0.10f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(panelColor)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "独立文件夹",
            color = primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = exportProgressLabel ?: sessionPath ?: "会在 session / temp / frames / output 下单独管理本次 Motion Photo 任务。",
            color = secondaryText,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MotionPhotoActionChip(
                label = "创建工作目录",
                enabled = !isExporting,
                onClick = onPrepareWorkspace
            )
            MotionPhotoActionChip(
                label = if (isExporting) "导出中..." else "导出 Motion Photo",
                enabled = !isExporting,
                showProgress = isExporting,
                onClick = onExport
            )
            if (exportedUri != null) {
                MotionPhotoActionChip(
                    label = "分享",
                    enabled = !isExporting,
                    onClick = onShare
                )
            }
        }
    }
}

@Composable
private fun MotionPhotoActionChip(
    label: String,
    enabled: Boolean,
    showProgress: Boolean = false,
    onClick: () -> Unit
) {
    val primaryText = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(primaryText.copy(alpha = if (enabled) 0.08f else 0.04f))
            .border(1.dp, primaryText.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.8.dp,
                    color = primaryText.copy(alpha = 0.72f)
                )
            }
            Text(
                text = label,
                color = if (enabled) primaryText else primaryText.copy(alpha = 0.42f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MotionPhotoExportResultCard(
    result: MotionPhotoExportResult,
    primaryText: Color,
    secondaryText: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
            .border(1.dp, primaryText.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "导出结果",
            color = primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Image(
            bitmap = result.previewBitmap.asImageBitmap(),
            contentDescription = result.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Text(
            text = result.displayName,
            color = primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = result.savedUri.toString(),
            color = secondaryText,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

private fun shareMotionPhoto(
    context: Context,
    uri: Uri,
    displayName: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, displayName)
        putExtra(Intent.EXTRA_SUBJECT, displayName)
        clipData = ClipData.newUri(context.contentResolver, displayName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
