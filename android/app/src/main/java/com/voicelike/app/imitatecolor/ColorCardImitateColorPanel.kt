package com.voicelike.app

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
internal fun ColorCardTraceImitatePanel(
    traceState: ColorCardTraceFilterState,
    isDark: Boolean,
    panelShape: RoundedCornerShape,
    backgroundColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    saveImageState: ColorCardTraceSaveImageState,
    referencePreviewUri: Uri?,
    referenceDisplayName: String?,
    referenceColors: List<Int>,
    referenceLoading: Boolean,
    comparePressed: Boolean,
    onPickReference: () -> Unit,
    onClearReference: () -> Unit,
    onComparePressedChange: (Boolean) -> Unit,
    onSaveImage: () -> Unit
) {
    val hasReference = referencePreviewUri != null
    val cardShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .clip(panelShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, panelShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "仿色",
                color = primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ColorCardTraceTextActionChip(
                label = if (hasReference) "更换" else "上传",
                enabled = !referenceLoading,
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                onClick = onPickReference
            )
            Spacer(modifier = Modifier.width(4.dp))
            ColorCardTraceCompareButton(
                pressed = comparePressed,
                enabled = traceState.previewSourceBitmap?.isRecycled == false,
                primaryText = primaryText,
                secondaryText = secondaryText,
                contentDescription = if (comparePressed) "正在查看原图" else "按住查看原图",
                onPressedChange = onComparePressedChange
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    shape = cardShape
                )
                .padding(10.dp)
        ) {
            when {
                referenceLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "正在解析参考图",
                            color = secondaryText,
                            fontSize = 12.sp
                        )
                    }
                }

                referencePreviewUri != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(referencePreviewUri)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = "参考图预览",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = referenceDisplayName?.ifBlank { "参考图" } ?: "参考图",
                                    color = primaryText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onClearReference,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "清除参考图",
                                    tint = secondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (referenceColors.isNotEmpty()) {
                            ColorCardTracePaletteStrip(
                                colors = referenceColors,
                                isDark = isDark
                            )
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "还没有参考图",
                            color = secondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (hasReference) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                ColorCardTraceTripleSliderGroup(
                    traceState = traceState,
                    paletteColors = traceState.displayColors,
                    isDark = isDark,
                    sliderHeight = 112.dp
                )
                ColorCardTraceAdjustmentPad(
                    color = traceState.color,
                    tone = traceState.tone,
                    colorAxis = traceState.colorAxis,
                    onAdjust = traceState.updateAdjustment,
                    isDark = isDark,
                    modifier = Modifier
                        .weight(1f)
                        .height(112.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ColorCardTraceStrengthSlider(
                value = traceState.paletteStrength,
                onValueChange = traceState.updatePaletteStrength,
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )
        }
    }
}
