package com.voicelike.app

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropOriginal
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun WatermarkCropPage(
    isDark: Boolean,
    preset: SocialCropPreset?,
    cropAspectRatio: Float,
    bitmap: Bitmap?,
    centerX: Float,
    centerY: Float,
    zoom: Float,
    isWorking: Boolean,
    presets: List<SocialCropPreset> = emptyList(),
    canUndo: Boolean = false,
    onUndo: () -> Unit = {},
    onTransform: (Float, Float, Float) -> Unit,
    onFreeModeResize: (Float, Float, Float, Float, Boolean) -> Unit = { _, _, _, _, _ -> },
    onReset: () -> Unit = {},
    onPresetChange: (SocialCropPreset) -> Unit = {},
    onFreeAspectSelected: () -> Unit = {},
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("裁切数据不可用", fontSize = 13.sp, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onCancel) {
                Text("返回")
            }
        }
        return
    }

    val presetKey = preset?.key ?: "free"
    var editorSize by remember(presetKey, bitmap) { mutableStateOf(IntSize.Zero) }
    val centerXState = rememberUpdatedState(centerX)
    val centerYState = rememberUpdatedState(centerY)
    val zoomState = rememberUpdatedState(zoom)
    val isFreeMode = preset == null
    val safeAspect = cropAspectRatio.coerceAtLeast(0.01f)
    val accentColor = if (isDark) Color(0xFFFFD85A) else Color(0xFFFFD247)
    val scrimColor = Color.Black.copy(alpha = if (isDark) 0.82f else 0.76f)
    val gridColor = Color.White.copy(alpha = 0.34f)
    val dockColor = Color.Black.copy(alpha = if (isDark) 0.54f else 0.50f)
    val dockBorderColor = Color.White.copy(alpha = if (isDark) 0.16f else 0.18f)
    val chipColor = Color.White.copy(alpha = if (isDark) 0.09f else 0.11f)
    val chipBorderColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.14f)
    val titleColor = Color.White.copy(alpha = 0.96f)
    val subtitleColor = Color.White.copy(alpha = 0.64f)
    val density = LocalDensity.current
    val editorBottomInset = if (presets.isEmpty()) 116.dp else 188.dp
    val editorHorizontalInsetPx = with(density) { 12.dp.toPx() }
    val editorTopInsetPx = with(density) { 12.dp.toPx() }
    val editorBottomInsetPx = with(density) { editorBottomInset.toPx() }
    val currentLayout = computeCropInteractionLayout(
        size = editorSize,
        aspectRatio = safeAspect,
        horizontalInset = editorHorizontalInsetPx,
        topInset = editorTopInsetPx,
        bottomInset = editorBottomInsetPx
    )
    val currentCropWindow = resolveCropBitmapWindow(
        sourceWidth = bitmap.width,
        sourceHeight = bitmap.height,
        aspectRatio = safeAspect,
        centerX = centerX,
        centerY = centerY,
        zoom = zoom
    )
    val currentLayoutState = rememberUpdatedState(currentLayout)
    val currentCropWindowState = rememberUpdatedState(currentCropWindow)
    val currentFreeModeResizeState = rememberUpdatedState(onFreeModeResize)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { editorSize = it }
                .pointerInput(
                    presetKey,
                    cropAspectRatio,
                    bitmap,
                    editorSize,
                    editorHorizontalInsetPx,
                    editorTopInsetPx,
                    editorBottomInsetPx
                ) {
                    detectTransformGestures { _, pan, scale, _ ->
                        val layout = currentLayoutState.value
                        if (layout.cropRect.width <= 0f || layout.cropRect.height <= 0f) return@detectTransformGestures
                        val currentWindow = currentCropWindowState.value
                        val nextZoom = (zoomState.value * scale).coerceIn(1f, 8f)
                        val (baseW, baseH) = computeCropBaseWindow(bitmap.width, bitmap.height, safeAspect)
                        val cropW = (baseW / nextZoom).coerceAtLeast(1f)
                        val cropH = (baseH / nextZoom).coerceAtLeast(1f)
                        val scalePxPerBitmap = layout.cropRect.width / cropW
                        var nextCenterX = currentWindow.centerX - pan.x / scalePxPerBitmap
                        var nextCenterY = currentWindow.centerY - pan.y / scalePxPerBitmap
                        val clamped = clampCropCenterForPage(
                            sourceWidth = bitmap.width.toFloat(),
                            sourceHeight = bitmap.height.toFloat(),
                            cropWidth = cropW,
                            cropHeight = cropH,
                            centerX = nextCenterX,
                            centerY = nextCenterY
                        )
                        nextCenterX = clamped.first
                        nextCenterY = clamped.second
                        onTransform(nextCenterX, nextCenterY, nextZoom)
                    }
                }
                .pointerInput(presetKey, bitmap) {
                    detectTapGestures(
                        onDoubleTap = { onReset() }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val layout = currentLayout
                if (layout.cropRect.width <= 0f || layout.cropRect.height <= 0f) return@Canvas
                val cropW = currentCropWindow.width
                val cropH = currentCropWindow.height
                val cropLeft = currentCropWindow.left
                val cropTop = currentCropWindow.top
                val scalePxPerBitmap = layout.cropRect.width / cropW
                val imageLeft = layout.cropRect.left - cropLeft * scalePxPerBitmap
                val imageTop = layout.cropRect.top - cropTop * scalePxPerBitmap
                val imageWidth = bitmap.width * scalePxPerBitmap
                val imageHeight = bitmap.height * scalePxPerBitmap

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = androidx.compose.ui.unit.IntOffset(
                        imageLeft.roundToInt(),
                        imageTop.roundToInt()
                    ),
                    dstSize = IntSize(
                        imageWidth.roundToInt().coerceAtLeast(1),
                        imageHeight.roundToInt().coerceAtLeast(1)
                    )
                )

                drawCropScrim(layout.cropRect, scrimColor)
                drawRoundRect(
                    color = accentColor,
                    topLeft = layout.cropRect.topLeft,
                    size = layout.cropRect.size,
                    cornerRadius = CornerRadius(layout.cornerRadius, layout.cornerRadius),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCropGrid(layout.cropRect, layout.cornerRadius, gridColor)
                drawCropHandles(layout.cropRect, accentColor)
            }

            if (isFreeMode && currentLayout.cropRect.width > 0f && currentLayout.cropRect.height > 0f) {
                FreeCropResizeEdgesOverlay(
                    layout = currentLayout,
                    accentColor = accentColor,
                    enabled = !isWorking,
                    onEdgeDrag = { edge, dragAmount, recordUndo ->
                        val resizedWindow = resizeFreeCropWindowFromEdge(
                            currentWindow = currentCropWindowState.value,
                            edge = edge,
                            dragAmountPx = dragAmount,
                            layout = currentLayoutState.value,
                            sourceWidth = bitmap.width,
                            sourceHeight = bitmap.height
                        )
                        val nextAspectRatio = (resizedWindow.width / resizedWindow.height).coerceAtLeast(0.01f)
                        val nextZoom = deriveCropZoomForWindow(
                            sourceWidth = bitmap.width,
                            sourceHeight = bitmap.height,
                            cropWidth = resizedWindow.width,
                            cropHeight = resizedWindow.height
                        )
                        currentFreeModeResizeState.value(
                            nextAspectRatio,
                            resizedWindow.centerX,
                            resizedWindow.centerY,
                            nextZoom,
                            recordUndo
                        )
                    }
                )
                FreeCropResizeHandlesOverlay(
                    layout = currentLayout,
                    accentColor = accentColor,
                    enabled = !isWorking,
                    onCornerDrag = { corner, dragAmount, recordUndo ->
                        val resizedWindow = resizeFreeCropWindowFromCorner(
                            currentWindow = currentCropWindowState.value,
                            corner = corner,
                            dragAmountPx = dragAmount,
                            layout = currentLayoutState.value,
                            sourceWidth = bitmap.width,
                            sourceHeight = bitmap.height
                        )
                        val nextAspectRatio = (resizedWindow.width / resizedWindow.height).coerceAtLeast(0.01f)
                        val nextZoom = deriveCropZoomForWindow(
                            sourceWidth = bitmap.width,
                            sourceHeight = bitmap.height,
                            cropWidth = resizedWindow.width,
                            cropHeight = resizedWindow.height
                        )
                        currentFreeModeResizeState.value(
                            nextAspectRatio,
                            resizedWindow.centerX,
                            resizedWindow.centerY,
                            nextZoom,
                            recordUndo
                        )
                    }
                )
            }
        }

        BottomCropBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            dockColor = dockColor,
            dockBorderColor = dockBorderColor,
            chipColor = chipColor,
            chipBorderColor = chipBorderColor,
            titleColor = titleColor,
            accentColor = accentColor,
            isWorking = isWorking,
            canUndo = canUndo,
            onCancel = onCancel,
            onUndo = onUndo,
            onReset = onReset,
            onSave = onSave,
            showPresets = true
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CropPresetCard(
                    title = "\u81EA\u7531",
                    ratioLabel = "\u539F\u56FE",
                    selected = preset == null,
                    onClick = onFreeAspectSelected,
                    accentColor = accentColor,
                    cardColor = chipColor,
                    borderColor = chipBorderColor,
                    titleColor = titleColor,
                    subtitleColor = subtitleColor
                )
                presets.forEach { option ->
                    CropPresetCard(
                        title = option.platformLabel,
                        ratioLabel = option.ratioLabel,
                        selected = option.key == preset?.key,
                        onClick = { onPresetChange(option) },
                        accentColor = accentColor,
                        cardColor = chipColor,
                        borderColor = chipBorderColor,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomCropBar(
    modifier: Modifier = Modifier,
    dockColor: Color,
    dockBorderColor: Color,
    chipColor: Color,
    chipBorderColor: Color,
    titleColor: Color,
    accentColor: Color,
    isWorking: Boolean,
    canUndo: Boolean,
    onCancel: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    showPresets: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .bottomNavigationBarPadding(18.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = dockColor,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, dockBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CropActionChip(
                        icon = Icons.Outlined.Close,
                        label = "取消",
                        onClick = onCancel,
                        enabled = !isWorking,
                        containerColor = chipColor,
                        borderColor = chipBorderColor,
                        contentColor = titleColor
                    )
                    CropActionChip(
                        icon = Icons.AutoMirrored.Outlined.Undo,
                        label = "重置",
                        onClick = if (canUndo) onUndo else onReset,
                        enabled = !isWorking,
                        containerColor = chipColor,
                        borderColor = chipBorderColor,
                        contentColor = titleColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onSave,
                        enabled = !isWorking,
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF1B1B1B),
                            disabledContainerColor = accentColor.copy(alpha = 0.42f),
                            disabledContentColor = Color(0xFF1B1B1B).copy(alpha = 0.56f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isWorking) "处理中" else "应用",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (showPresets) {
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun CropActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    containerColor: Color,
    borderColor: Color,
    contentColor: Color
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CropPresetCard(
    title: String,
    ratioLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    cardColor: Color,
    borderColor: Color,
    titleColor: Color,
    subtitleColor: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accentColor.copy(alpha = 0.16f) else cardColor,
        modifier = Modifier
            .widthIn(min = 82.dp)
            .heightIn(min = 62.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else borderColor
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = cropPresetIcon(ratioLabel),
                contentDescription = null,
                tint = if (selected) accentColor else titleColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1
            )
            Text(
                text = ratioLabel,
                fontSize = 11.sp,
                color = if (selected) accentColor else subtitleColor
            )
        }
    }
}

private fun cropPresetIcon(ratioLabel: String) = when (ratioLabel) {
    "1:1" -> Icons.Outlined.CropSquare
    "3:4", "9:16" -> Icons.Outlined.CropPortrait
    else -> Icons.Outlined.CropOriginal
}

private data class CropInteractionLayout(
    val cropRect: Rect,
    val cornerRadius: Float
)

private data class CropBitmapWindow(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val width: Float,
    val height: Float,
    val centerX: Float,
    val centerY: Float
)

private enum class CropResizeCorner {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
}

private enum class CropResizeEdge {
    Left,
    Top,
    Right,
    Bottom
}

private fun computeCropInteractionLayout(
    size: IntSize,
    aspectRatio: Float,
    horizontalInset: Float = 0f,
    topInset: Float = 0f,
    bottomInset: Float = 0f
): CropInteractionLayout {
    if (size.width <= 0 || size.height <= 0) {
        return CropInteractionLayout(Rect.Zero, 0f)
    }
    val safeLeft = horizontalInset.coerceAtLeast(0f)
    val safeTop = topInset.coerceAtLeast(0f)
    val safeRight = (size.width.toFloat() - horizontalInset).coerceAtLeast(safeLeft + 1f)
    val safeBottom = (size.height.toFloat() - bottomInset).coerceAtLeast(safeTop + 1f)
    val availableWidth = (safeRight - safeLeft).coerceAtLeast(1f)
    val availableHeight = (safeBottom - safeTop).coerceAtLeast(1f)
    val frameWidthByWidth = availableWidth * 0.94f
    val frameWidthByHeight = availableHeight * 0.88f * aspectRatio
    val frameWidth = min(frameWidthByWidth, frameWidthByHeight).coerceAtLeast(1f)
    val frameHeight = (frameWidth / aspectRatio).coerceAtLeast(1f)
    val left = safeLeft + (availableWidth - frameWidth) / 2f
    val top = safeTop + (availableHeight - frameHeight) / 2f
    return CropInteractionLayout(
        cropRect = Rect(left, top, left + frameWidth, top + frameHeight),
        cornerRadius = min(frameWidth, frameHeight) * 0.012f
    )
}

private fun resolveCropBitmapWindow(
    sourceWidth: Int,
    sourceHeight: Int,
    aspectRatio: Float,
    centerX: Float,
    centerY: Float,
    zoom: Float
): CropBitmapWindow {
    val safeZoom = zoom.coerceIn(1f, 8f)
    val (baseW, baseH) = computeCropBaseWindow(sourceWidth, sourceHeight, aspectRatio.coerceAtLeast(0.01f))
    val cropW = (baseW / safeZoom).coerceAtLeast(1f)
    val cropH = (baseH / safeZoom).coerceAtLeast(1f)
    val clampedCenter = clampCropCenterForPage(
        sourceWidth = sourceWidth.toFloat(),
        sourceHeight = sourceHeight.toFloat(),
        cropWidth = cropW,
        cropHeight = cropH,
        centerX = centerX,
        centerY = centerY
    )
    return CropBitmapWindow(
        left = clampedCenter.first - cropW / 2f,
        top = clampedCenter.second - cropH / 2f,
        right = clampedCenter.first + cropW / 2f,
        bottom = clampedCenter.second + cropH / 2f,
        width = cropW,
        height = cropH,
        centerX = clampedCenter.first,
        centerY = clampedCenter.second
    )
}

private fun deriveCropZoomForWindow(
    sourceWidth: Int,
    sourceHeight: Int,
    cropWidth: Float,
    cropHeight: Float
): Float {
    val safeWidth = cropWidth.coerceAtLeast(1f)
    val safeHeight = cropHeight.coerceAtLeast(1f)
    val safeAspect = (safeWidth / safeHeight).coerceAtLeast(0.01f)
    val (baseW, _) = computeCropBaseWindow(sourceWidth, sourceHeight, safeAspect)
    return (baseW / safeWidth).coerceIn(1f, 8f)
}

private fun resizeFreeCropWindowFromCorner(
    currentWindow: CropBitmapWindow,
    corner: CropResizeCorner,
    dragAmountPx: Offset,
    layout: CropInteractionLayout,
    sourceWidth: Int,
    sourceHeight: Int
): CropBitmapWindow {
    if (layout.cropRect.width <= 0f || currentWindow.width <= 0f) {
        return currentWindow
    }
    val scalePxPerBitmap = layout.cropRect.width / currentWindow.width
    if (scalePxPerBitmap <= 0f || !scalePxPerBitmap.isFinite()) {
        return currentWindow
    }
    val bitmapPerUiPx = 1f / scalePxPerBitmap
    val dx = dragAmountPx.x * bitmapPerUiPx
    val dy = dragAmountPx.y * bitmapPerUiPx
    val minCropEdge = maxOf(48f, min(sourceWidth.toFloat(), sourceHeight.toFloat()) * 0.10f)

    var left = currentWindow.left
    var top = currentWindow.top
    var right = currentWindow.right
    var bottom = currentWindow.bottom

    when (corner) {
        CropResizeCorner.TopLeft -> {
            left = (left + dx).coerceIn(0f, right - minCropEdge)
            top = (top + dy).coerceIn(0f, bottom - minCropEdge)
        }
        CropResizeCorner.TopRight -> {
            right = (right + dx).coerceIn(left + minCropEdge, sourceWidth.toFloat())
            top = (top + dy).coerceIn(0f, bottom - minCropEdge)
        }
        CropResizeCorner.BottomLeft -> {
            left = (left + dx).coerceIn(0f, right - minCropEdge)
            bottom = (bottom + dy).coerceIn(top + minCropEdge, sourceHeight.toFloat())
        }
        CropResizeCorner.BottomRight -> {
            right = (right + dx).coerceIn(left + minCropEdge, sourceWidth.toFloat())
            bottom = (bottom + dy).coerceIn(top + minCropEdge, sourceHeight.toFloat())
        }
    }

    val width = (right - left).coerceAtLeast(minCropEdge)
    val height = (bottom - top).coerceAtLeast(minCropEdge)
    return CropBitmapWindow(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height,
        width = width,
        height = height,
        centerX = left + width / 2f,
        centerY = top + height / 2f
    )
}

private fun resizeFreeCropWindowFromEdge(
    currentWindow: CropBitmapWindow,
    edge: CropResizeEdge,
    dragAmountPx: Offset,
    layout: CropInteractionLayout,
    sourceWidth: Int,
    sourceHeight: Int
): CropBitmapWindow {
    if (layout.cropRect.width <= 0f || currentWindow.width <= 0f) {
        return currentWindow
    }
    val scalePxPerBitmap = layout.cropRect.width / currentWindow.width
    if (scalePxPerBitmap <= 0f || !scalePxPerBitmap.isFinite()) {
        return currentWindow
    }
    val bitmapPerUiPx = 1f / scalePxPerBitmap
    val dx = dragAmountPx.x * bitmapPerUiPx
    val dy = dragAmountPx.y * bitmapPerUiPx
    val minCropEdge = maxOf(48f, min(sourceWidth.toFloat(), sourceHeight.toFloat()) * 0.10f)

    var left = currentWindow.left
    var top = currentWindow.top
    var right = currentWindow.right
    var bottom = currentWindow.bottom

    when (edge) {
        CropResizeEdge.Left -> {
            left = (left + dx).coerceIn(0f, right - minCropEdge)
        }
        CropResizeEdge.Top -> {
            top = (top + dy).coerceIn(0f, bottom - minCropEdge)
        }
        CropResizeEdge.Right -> {
            right = (right + dx).coerceIn(left + minCropEdge, sourceWidth.toFloat())
        }
        CropResizeEdge.Bottom -> {
            bottom = (bottom + dy).coerceIn(top + minCropEdge, sourceHeight.toFloat())
        }
    }

    val width = (right - left).coerceAtLeast(minCropEdge)
    val height = (bottom - top).coerceAtLeast(minCropEdge)
    return CropBitmapWindow(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height,
        width = width,
        height = height,
        centerX = left + width / 2f,
        centerY = top + height / 2f
    )
}

private fun DrawScope.drawCropScrim(cropRect: Rect, scrimColor: Color) {
    if (cropRect == Rect.Zero) return
    drawRect(scrimColor, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, cropRect.top))
    drawRect(
        scrimColor,
        topLeft = Offset(0f, cropRect.bottom),
        size = androidx.compose.ui.geometry.Size(size.width, size.height - cropRect.bottom)
    )
    drawRect(
        scrimColor,
        topLeft = Offset(0f, cropRect.top),
        size = androidx.compose.ui.geometry.Size(cropRect.left, cropRect.height)
    )
    drawRect(
        scrimColor,
        topLeft = Offset(cropRect.right, cropRect.top),
        size = androidx.compose.ui.geometry.Size(size.width - cropRect.right, cropRect.height)
    )
}

private fun DrawScope.drawCropGrid(cropRect: Rect, cornerRadius: Float, color: Color) {
    val oneThirdWidth = cropRect.width / 3f
    val oneThirdHeight = cropRect.height / 3f
    repeat(2) { index ->
        val x = cropRect.left + oneThirdWidth * (index + 1)
        drawLine(
            color = color,
            start = Offset(x, cropRect.top + cornerRadius * 0.4f),
            end = Offset(x, cropRect.bottom - cornerRadius * 0.4f),
            strokeWidth = 1.dp.toPx()
        )
    }
    repeat(2) { index ->
        val y = cropRect.top + oneThirdHeight * (index + 1)
        drawLine(
            color = color,
            start = Offset(cropRect.left + cornerRadius * 0.4f, y),
            end = Offset(cropRect.right - cornerRadius * 0.4f, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawCropHandles(cropRect: Rect, color: Color) {
    val handleLength = min(cropRect.width, cropRect.height) * 0.08f
    val stroke = 4.dp.toPx()
    val inset = stroke / 2f
    val left = cropRect.left + inset
    val right = cropRect.right - inset
    val top = cropRect.top + inset
    val bottom = cropRect.bottom - inset

    drawLine(color, Offset(left, top + handleLength), Offset(left, top), stroke)
    drawLine(color, Offset(left, top), Offset(left + handleLength, top), stroke)

    drawLine(color, Offset(right - handleLength, top), Offset(right, top), stroke)
    drawLine(color, Offset(right, top), Offset(right, top + handleLength), stroke)

    drawLine(color, Offset(left, bottom - handleLength), Offset(left, bottom), stroke)
    drawLine(color, Offset(left, bottom), Offset(left + handleLength, bottom), stroke)

    drawLine(color, Offset(right - handleLength, bottom), Offset(right, bottom), stroke)
    drawLine(color, Offset(right, bottom - handleLength), Offset(right, bottom), stroke)
}

@Composable
private fun FreeCropResizeHandlesOverlay(
    layout: CropInteractionLayout,
    accentColor: Color,
    enabled: Boolean,
    onCornerDrag: (CropResizeCorner, Offset, Boolean) -> Unit
) {
    val density = LocalDensity.current
    val handleTouchTargetPx = with(density) { 44.dp.roundToPx() }
    val handleVisualSize = 14.dp
    val currentOnCornerDrag = rememberUpdatedState(onCornerDrag)

    fun handleCenter(corner: CropResizeCorner): Offset = when (corner) {
        CropResizeCorner.TopLeft -> layout.cropRect.topLeft
        CropResizeCorner.TopRight -> Offset(layout.cropRect.right, layout.cropRect.top)
        CropResizeCorner.BottomLeft -> Offset(layout.cropRect.left, layout.cropRect.bottom)
        CropResizeCorner.BottomRight -> Offset(layout.cropRect.right, layout.cropRect.bottom)
    }

    CropResizeCorner.entries.forEach { corner ->
        var recordedUndoForGesture by remember(corner, layout.cropRect) { mutableStateOf(false) }
        val center = handleCenter(corner)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (center.x - (handleTouchTargetPx / 2f)).roundToInt(),
                        y = (center.y - (handleTouchTargetPx / 2f)).roundToInt()
                    )
                }
                .size(with(density) { handleTouchTargetPx.toDp() })
                .pointerInput(corner, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            recordedUndoForGesture = false
                        },
                        onDragCancel = {
                            recordedUndoForGesture = false
                        },
                        onDragEnd = {
                            recordedUndoForGesture = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnCornerDrag.value(corner, Offset(dragAmount.x, dragAmount.y), !recordedUndoForGesture)
                            recordedUndoForGesture = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black.copy(alpha = 0.18f)),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.size(handleVisualSize)
            ) {}
        }
    }
}

@Composable
private fun FreeCropResizeEdgesOverlay(
    layout: CropInteractionLayout,
    accentColor: Color,
    enabled: Boolean,
    onEdgeDrag: (CropResizeEdge, Offset, Boolean) -> Unit
) {
    val density = LocalDensity.current
    val edgeTouchThicknessPx = with(density) { 36.dp.roundToPx() }
    val edgeVisualThickness = 4.dp
    val currentOnEdgeDrag = rememberUpdatedState(onEdgeDrag)

    data class EdgeSpec(
        val edge: CropResizeEdge,
        val center: Offset,
        val widthPx: Float,
        val heightPx: Float
    )

    val cropRect = layout.cropRect
    val edgeSpecs = listOf(
        EdgeSpec(
            edge = CropResizeEdge.Top,
            center = Offset(cropRect.center.x, cropRect.top),
            widthPx = cropRect.width * 0.45f,
            heightPx = edgeTouchThicknessPx.toFloat()
        ),
        EdgeSpec(
            edge = CropResizeEdge.Bottom,
            center = Offset(cropRect.center.x, cropRect.bottom),
            widthPx = cropRect.width * 0.45f,
            heightPx = edgeTouchThicknessPx.toFloat()
        ),
        EdgeSpec(
            edge = CropResizeEdge.Left,
            center = Offset(cropRect.left, cropRect.center.y),
            widthPx = edgeTouchThicknessPx.toFloat(),
            heightPx = cropRect.height * 0.45f
        ),
        EdgeSpec(
            edge = CropResizeEdge.Right,
            center = Offset(cropRect.right, cropRect.center.y),
            widthPx = edgeTouchThicknessPx.toFloat(),
            heightPx = cropRect.height * 0.45f
        )
    )

    edgeSpecs.forEach { spec ->
        var recordedUndoForGesture by remember(spec.edge, cropRect) { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (spec.center.x - (spec.widthPx / 2f)).roundToInt(),
                        y = (spec.center.y - (spec.heightPx / 2f)).roundToInt()
                    )
                }
                .width(with(density) { spec.widthPx.toDp() })
                .height(with(density) { spec.heightPx.toDp() })
                .pointerInput(spec.edge, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            recordedUndoForGesture = false
                        },
                        onDragCancel = {
                            recordedUndoForGesture = false
                        },
                        onDragEnd = {
                            recordedUndoForGesture = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnEdgeDrag.value(spec.edge, Offset(dragAmount.x, dragAmount.y), !recordedUndoForGesture)
                            recordedUndoForGesture = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accentColor.copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.18f)),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .width(
                        if (spec.edge == CropResizeEdge.Left || spec.edge == CropResizeEdge.Right) {
                            edgeVisualThickness
                        } else {
                            with(density) { (spec.widthPx * 0.55f).toDp() }
                        }
                    )
                    .height(
                        if (spec.edge == CropResizeEdge.Top || spec.edge == CropResizeEdge.Bottom) {
                            edgeVisualThickness
                        } else {
                            with(density) { (spec.heightPx * 0.55f).toDp() }
                        }
                    )
            ) {}
        }
    }
}

private fun computeCropBaseWindow(sourceWidth: Int, sourceHeight: Int, targetAspect: Float): Pair<Float, Float> {
    val safeAspect = targetAspect.coerceAtLeast(0.01f)
    val sourceAspect = sourceWidth.toFloat() / sourceHeight.toFloat().coerceAtLeast(1f)
    return if (sourceAspect >= safeAspect) {
        val h = sourceHeight.toFloat()
        val w = h * safeAspect
        w to h
    } else {
        val w = sourceWidth.toFloat()
        val h = w / safeAspect
        w to h
    }
}

private fun clampCropCenterForPage(
    sourceWidth: Float,
    sourceHeight: Float,
    cropWidth: Float,
    cropHeight: Float,
    centerX: Float,
    centerY: Float
): Pair<Float, Float> {
    val halfW = cropWidth / 2f
    val halfH = cropHeight / 2f
    val clampedX = centerX.coerceIn(halfW, sourceWidth - halfW)
    val clampedY = centerY.coerceIn(halfH, sourceHeight - halfH)
    return clampedX to clampedY
}
