package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun ColorCardGradientDetailSheet(
    gradientSpec: ColorCardGradientSpec,
    eyebrow: String?,
    title: String?,
    useChineseFont: Boolean,
    panelVisible: Boolean,
    onGradientSpecChange: (ColorCardGradientSpec) -> Unit,
    onExportGradient: () -> Unit,
    onSaveImage: () -> Unit
) {
    val normalizedSpec = remember(gradientSpec) { normalizeColorCardGradientSpec(gradientSpec) }
    val paletteColors = remember(normalizedSpec) { colorCardGradientPaletteColors(normalizedSpec) }
    val contentColor = remember(paletteColors) {
        if (paletteColors.isEmpty()) {
            Color.White
        } else {
            val averageLuminance = paletteColors.map { Color(it).luminance() }.average().toFloat()
            if (averageLuminance > 0.58f) Color.Black else Color.White
        }
    }
    val secondaryText = contentColor.copy(alpha = 0.68f)
    val panelColor = if (contentColor == Color.White) {
        Color.Black.copy(alpha = 0.26f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }
    val panelBorderColor = contentColor.copy(alpha = 0.18f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .colorCardGradientBackground(normalizedSpec)
    ) {
        if (panelVisible) {
            val panelMaxHeight = if (maxHeight > 720.dp) 420.dp else maxHeight * 0.56f
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .heightIn(max = panelMaxHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .background(panelColor)
                    .border(1.dp, panelBorderColor, RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (!eyebrow.isNullOrBlank() || !title.isNullOrBlank()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            eyebrow?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    color = secondaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            title?.takeIf { it.isNotBlank() }?.let {
                                if (useChineseFont) {
                                    ChineseColorNameText(
                                        text = it,
                                        color = contentColor,
                                        textSizeSp = 28f,
                                        maxLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (shouldUseColorCardLeiko(it)) {
                                    LeikoText(
                                        text = it,
                                        color = contentColor,
                                        textSizeSp = 24f,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeightSp = 30f,
                                        maxLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = it,
                                        color = contentColor,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 30.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorCardGradientPanelActionChip(
                            label = "保存图片",
                            contentColor = contentColor,
                            onClick = onSaveImage
                        )
                        ColorCardGradientPanelActionChip(
                            label = "导出",
                            contentColor = contentColor,
                            onClick = onExportGradient
                        )
                    }
                }

                ColorCardGradientPreview(
                    gradientSpec = normalizedSpec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    borderColor = contentColor.copy(alpha = 0.24f),
                    showStopHandles = true,
                    handleColor = contentColor
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\u89d2\u5ea6",
                            color = secondaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${formatColorCardGradientAngle(normalizedSpec.angleDegrees)}\u00b0",
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Slider(
                        value = normalizedSpec.angleDegrees,
                        onValueChange = { angle ->
                            onGradientSpecChange(updateColorCardGradientAngle(normalizedSpec, angle))
                        },
                        valueRange = 0f..360f
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "\u6bd4\u4f8b",
                        color = secondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    normalizedSpec.stops.forEachIndexed { index, stop ->
                        ColorCardGradientStopEditorRow(
                            stop = stop,
                            stopIndex = index,
                            totalStops = normalizedSpec.stops.size,
                            contentColor = contentColor,
                            secondaryText = secondaryText,
                            onPositionChange = { position ->
                                onGradientSpecChange(
                                    updateColorCardGradientStopPosition(
                                        spec = normalizedSpec,
                                        stopIndex = index,
                                        positionPercent = position
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ColorCardGradientPreview(
    gradientSpec: ColorCardGradientSpec,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColor: Color = Color.Black.copy(alpha = 0.08f),
    showStopHandles: Boolean = false,
    handleColor: Color = Color.White
) {
    val normalizedSpec = remember(gradientSpec) { normalizeColorCardGradientSpec(gradientSpec) }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .colorCardGradientBackground(normalizedSpec)
            .border(1.dp, borderColor, shape)
    ) {
        if (showStopHandles) {
            val handleSize = 10.dp
            val handleSizePx = with(density) { handleSize.roundToPx() }
            val maxWidthPx = with(density) { maxWidth.roundToPx() }
            val topInsetPx = with(density) { 8.dp.roundToPx() }
            normalizedSpec.stops.forEach { stop ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                x = (((maxWidthPx - handleSizePx).coerceAtLeast(0)) *
                                    (stop.positionPercent / 100f)).roundToInt(),
                                y = topInsetPx
                            )
                        }
                        .size(handleSize)
                        .clip(CircleShape)
                        .background(Color(stop.colorInt))
                        .border(1.dp, handleColor.copy(alpha = 0.82f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ColorCardGradientStopEditorRow(
    stop: ColorCardGradientStop,
    stopIndex: Int,
    totalStops: Int,
    contentColor: Color,
    secondaryText: Color,
    onPositionChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(stop.colorInt))
                        .border(1.dp, contentColor.copy(alpha = 0.28f), CircleShape)
                )
                Text(
                    text = "\u8272\u70b9 ${stopIndex + 1} / $totalStops",
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${formatColorCardGradientPercent(stop.positionPercent)}%",
                color = secondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = stop.positionPercent,
            onValueChange = onPositionChange,
            valueRange = 0f..100f
        )
    }
}

@Composable
private fun ColorCardGradientPanelActionChip(
    label: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(contentColor.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

internal fun Modifier.colorCardGradientBackground(
    gradientSpec: ColorCardGradientSpec
): Modifier {
    val normalizedSpec = normalizeColorCardGradientSpec(gradientSpec)
    return this.drawWithCache {
        val brush = Brush.linearGradient(
            colorStops = normalizedSpec.stops.map { stop ->
                stop.positionPercent.coerceIn(0f, 100f) / 100f to Color(stop.colorInt)
            }.toTypedArray(),
            start = colorCardGradientStart(size = size, angleDegrees = normalizedSpec.angleDegrees),
            end = colorCardGradientEnd(size = size, angleDegrees = normalizedSpec.angleDegrees)
        )
        onDrawBehind {
            drawRect(brush = brush)
        }
    }
}

private fun colorCardGradientStart(size: Size, angleDegrees: Float): Offset {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = sin(radians).toFloat() * radius
    val dy = -cos(radians).toFloat() * radius
    return Offset(center.x - dx, center.y - dy)
}

private fun colorCardGradientEnd(size: Size, angleDegrees: Float): Offset {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = sin(radians).toFloat() * radius
    val dy = -cos(radians).toFloat() * radius
    return Offset(center.x + dx, center.y + dy)
}
