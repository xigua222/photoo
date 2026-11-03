package com.voicelike.app.keep

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun KeepGalleryMetadataDrawer(
    item: MediaItem,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    onTertiaryAction: () -> Unit,
    tertiaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.FavoriteBorder,
    tertiaryActionContentDescription: String,
    tertiaryActionLabel: String = tertiaryActionContentDescription,
    tertiaryActionTint: Color = Color(0xFFF59E0B),
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val navigationBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val metadata by produceState<KeepPreviewMetadata?>(
        initialValue = peekKeepPreviewMetadata(item.id),
        key1 = item.id,
        key2 = expanded
    ) {
        value = peekKeepPreviewMetadata(item.id)
        if (expanded && value == null) {
            value = loadKeepPreviewMetadata(context, item)
        }
    }
    val resolvedItem = metadata?.resolvedItem ?: item
    val dateText = remember(resolvedItem.dateAdded) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(resolvedItem.dateAdded * 1000L))
    }
    val fileName = metadata?.fileName ?: item.displayName.takeIf { it.isNotBlank() } ?: item.uri.lastPathSegment.orEmpty()
    val filePath = metadata?.filePath ?: item.path?.takeIf { it.isNotBlank() } ?: item.uri.toString()
    val sizeText = remember(resolvedItem.size) {
        Formatter.formatFileSize(context, resolvedItem.size)
    }
    val resolutionText = remember(resolvedItem.width, resolvedItem.height) {
        "${resolvedItem.width.coerceAtLeast(1)} × ${resolvedItem.height.coerceAtLeast(1)}"
    }
    val locationText = resolvedItem.location.takeIf {
        !it.equals(strings.unknownLocation, ignoreCase = true) && it != "Unknown Location"
    }
    val summaryRows = remember(
        resolvedItem.id,
        dateText,
        sizeText,
        resolutionText,
        fileName,
        filePath,
        metadata?.mimeType,
        locationText
    ) {
        buildList {
            add(KeepPreviewMetadataRow(strings.date, dateText))
            add(KeepPreviewMetadataRow(strings.size, sizeText))
            add(KeepPreviewMetadataRow(strings.resolution, resolutionText))
            add(KeepPreviewMetadataRow(strings.filename, fileName))
            locationText?.let { add(KeepPreviewMetadataRow(strings.location, it)) }
            metadata?.mimeType?.takeIf { it.isNotBlank() }?.let { add(KeepPreviewMetadataRow("MIME", it)) }
            add(KeepPreviewMetadataRow("Path", filePath))
            resolvedItem.camera.takeIf { it.isNotBlank() && it != "--" }?.let { add(KeepPreviewMetadataRow(strings.camera, it)) }
            resolvedItem.iso.takeIf { it.isNotBlank() && it != "--" }?.let { add(KeepPreviewMetadataRow(strings.iso, it)) }
            resolvedItem.shutter.takeIf { it.isNotBlank() && it != "--" }?.let { add(KeepPreviewMetadataRow(strings.shutter, it)) }
            resolvedItem.focalLength.takeIf { it.isNotBlank() && it != "--" }?.let { add(KeepPreviewMetadataRow(strings.focalLabel, it)) }
            resolvedItem.aperture.takeIf { it.isNotBlank() && it != "--" }?.let { add(KeepPreviewMetadataRow(strings.apertureLabel, it)) }
        }
    }
    val expandedBodyHeight by animateDpAsState(
        targetValue = if (expanded) 240.dp else 0.dp,
        label = "keep_preview_drawer_height"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .detectDrawerVerticalToggle(
                expanded = expanded,
                onExpandedChange = onExpandedChange
            ),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = Color.Black.copy(alpha = 0.74f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp + navigationBarBottomPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onExpandedChange(!expanded)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.48f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                KeepDrawerActionButton(
                    icon = Icons.Outlined.Delete,
                    label = strings.delete,
                    contentDescription = strings.delete,
                    tint = Color(0xFFF87171),
                    onClick = onDelete
                )
                KeepDrawerActionButton(
                    icon = Icons.Outlined.Share,
                    label = strings.share,
                    contentDescription = strings.share,
                    tint = Color.White,
                    onClick = onShare
                )
                KeepDrawerActionButton(
                    icon = tertiaryActionIcon,
                    label = tertiaryActionLabel,
                    contentDescription = tertiaryActionContentDescription,
                    tint = tertiaryActionTint,
                    onClick = onTertiaryAction
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = expandedBodyHeight)
                        .padding(top = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = fileName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KeepDrawerPill(text = sizeText)
                        KeepDrawerPill(text = resolutionText)
                    }
                    locationText?.let { location ->
                        Text(
                            text = location,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    summaryRows.forEach { row ->
                        KeepDrawerMetadataRow(row = row)
                    }

                    if (metadata == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (metadata?.detailRows?.isNotEmpty() == true) {
                        Text(
                            text = if (item.type == "photo") "EXIF" else "DETAIL",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        metadata!!.detailRows.forEach { row ->
                            KeepDrawerMetadataRow(row = row)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepDrawerPill(
    text: String
) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun KeepDrawerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 72.dp, max = 104.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KeepDrawerMetadataRow(
    row: KeepPreviewMetadataRow
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = row.label,
            color = Color.White.copy(alpha = 0.64f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = row.value,
            color = Color.White.copy(alpha = 0.94f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

private fun Modifier.detectDrawerVerticalToggle(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
): Modifier = pointerInput(expanded) {
    var totalDrag = 0f
    detectVerticalDragGestures(
        onDragStart = { totalDrag = 0f },
        onVerticalDrag = { _, dragAmount ->
            totalDrag += dragAmount
        },
        onDragEnd = {
            if (abs(totalDrag) < 18f) return@detectVerticalDragGestures
            onExpandedChange(totalDrag < 0f)
        }
    )
}
