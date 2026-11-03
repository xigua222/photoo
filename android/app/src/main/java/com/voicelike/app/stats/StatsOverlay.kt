package com.voicelike.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.helpcenter.getHelpCenterActionTitle

@Composable
fun StatsOverlay(
    stats: AppStats,
    totalPhotos: Int,
    likedCount: Int,
    deletionMode: String = "recycle_bin",
    onDismiss: () -> Unit,
    onProcessedClick: () -> Unit,
    onDeepOrganizeClick: () -> Unit,
    onShootingStatsClick: () -> Unit,
    onLikesClick: () -> Unit,
    onWatermarkFrameClick: () -> Unit,
    onColorCardClick: () -> Unit,
    showColorCardEntry: Boolean = true,
    onMonthlyClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLabClick: () -> Unit,
    onCampfireClick: () -> Unit,
    onHelpCenterClick: () -> Unit,
    totalSize: Long,
    shouldAnimate: Boolean = true,
    isSubPageOpen: Boolean = false,
    isFolderDropMode: Boolean,
    enableHaptics: Boolean = true
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .padding(top = 40.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            val labelStyle = TextStyle(
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
            val numberStyle = TextStyle(
                fontSize = 72.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            val unitStyle = TextStyle(
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onProcessedClick
                )
            ) {
                RollingText(
                    targetValue = stats.processed,
                    format = { it.toInt().toString() },
                    style = numberStyle,
                    color = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black.copy(alpha = 0.9f),
                    shouldAnimate = shouldAnimate,
                    stopAnimation = isSubPageOpen,
                    enableHaptics = enableHaptics
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = strings.totalProcessed,
                        style = labelStyle
                    )
                    Text(
                        text = ">",
                        style = labelStyle
                    )
                }
                if (totalPhotos > 0) {
                    Text(
                        text = "${String.format("%.1f", (stats.processed.toFloat() / totalPhotos) * 100)}% ${strings.ofGallery}",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.Start) {
                RollingText(
                    targetValue = stats.trashed,
                    format = { it.toInt().toString() },
                    style = numberStyle,
                    color = Color(0xFFEF4444),
                    shouldAnimate = shouldAnimate,
                    stopAnimation = isSubPageOpen,
                    enableHaptics = enableHaptics
                )
                Text(
                    text = strings.deleted,
                    style = labelStyle
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.Start) {
                RollingText(
                    targetValue = likedCount,
                    format = { it.toInt().toString() },
                    style = numberStyle,
                    color = Color(0xFFFF9800),
                    shouldAnimate = shouldAnimate,
                    stopAnimation = isSubPageOpen,
                    enableHaptics = enableHaptics
                )
                Text(
                    text = strings.likes ?: "Likes",
                    style = labelStyle
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isGB = stats.savedSize > 1024 * 1024 * 1024
            val spaceVal = if (isGB) {
                stats.savedSize / 1024.0 / 1024.0 / 1024.0
            } else {
                stats.savedSize / 1024.0 / 1024.0
            }
            val spaceUnit = if (isGB) "GB" else "MB"

            Column(horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.Bottom) {
                    RollingText(
                        targetValue = spaceVal,
                        format = { String.format("%.1f", it.toFloat()) },
                        style = numberStyle,
                        color = Color(0xFF3B82F6),
                        shouldAnimate = shouldAnimate,
                        stopAnimation = isSubPageOpen,
                        enableHaptics = enableHaptics
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = spaceUnit,
                        style = unitStyle,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Text(
                    text = strings.savedSpace,
                    style = labelStyle
                )
                if (totalSize > 0) {
                    val totalSizeGB = totalSize / 1024.0 / 1024.0 / 1024.0
                    Text(
                        text = String.format(
                            strings.savedSpaceRatioFormat,
                            String.format("%.1f", (stats.savedSize.toFloat() / totalSize) * 100),
                            String.format("%.1f", totalSizeGB)
                        ),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val timeSaved = stats.trashed * 5 + stats.skipped * 2
            val timeUnit = when {
                timeSaved < 60 -> "s"
                timeSaved < 3600 -> "m"
                else -> "h"
            }
            val timeValNumber: Double = when {
                timeSaved < 60 -> timeSaved.toDouble()
                timeSaved < 3600 -> (timeSaved / 60).toDouble()
                else -> timeSaved / 3600.0
            }
            val timeFormat: (Number) -> String = when {
                timeSaved < 3600 -> { { it.toInt().toString() } }
                else -> { { String.format("%.2f", it.toFloat()) } }
            }

            Column(horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.Bottom) {
                    RollingText(
                        targetValue = timeValNumber,
                        format = timeFormat,
                        style = numberStyle,
                        color = Color(0xFF22C55E),
                        shouldAnimate = shouldAnimate,
                        stopAnimation = isSubPageOpen,
                        enableHaptics = enableHaptics
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeUnit,
                        style = unitStyle,
                        color = Color(0xFF22C55E),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Text(
                    text = strings.timeSaved,
                    style = labelStyle
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isRecycleBinVisible = deletionMode != "direct_delete"
                val newPageStrings = getNewPageStrings(currentLang)
                val labTitle = newPageStrings.labEntryTitle

                val actionItems = buildList<StatsActionItem> {
                    add(
                        StatsActionItem(
                            icon = Icons.Outlined.CameraAlt,
                            title = strings.shootingStatsTitle,
                            onClick = onShootingStatsClick
                        )
                    )
                    add(
                        StatsActionItem(
                            icon = Icons.Outlined.AutoAwesome,
                            title = strings.deepOrganizeTitle ?: "Deep Organize",
                            onClick = onDeepOrganizeClick
                        )
                    )
                    add(
                        StatsActionItem(
                            icon = Icons.Outlined.FavoriteBorder,
                            title = strings.likes ?: "Likes",
                            onClick = onLikesClick
                        )
                    )
                    add(
                        StatsActionItem(
                            icon = Icons.Outlined.Settings,
                            title = strings.settings,
                            onClick = onSettingsClick
                        )
                    )
                    if (showColorCardEntry) {
                        add(
                            StatsActionItem(
                                icon = Icons.Outlined.Palette,
                                title = strings.statsStrings.colorCardTitle,
                                onClick = onColorCardClick
                            )
                        )
                    }
                    if (isRecycleBinVisible) {
                        add(
                            StatsActionItem(
                                icon = Icons.Outlined.Delete,
                                title = strings.recycleBinTitle ?: "Recycle Bin",
                                onClick = onRecycleBinClick
                            )
                        )
                    }
                    add(
                        StatsActionItem(
                            icon = Icons.Outlined.Science,
                            title = labTitle,
                            onClick = onLabClick
                        )
                    )
                    add(
                        StatsActionItem(
                            icon = Icons.Outlined.LocalFireDepartment,
                            title = getCampfireActionTitle(currentLang),
                            onClick = onCampfireClick
                        )
                    )
                    add(
                        StatsActionItem(
                            icon = Icons.Outlined.HelpOutline,
                            title = getHelpCenterActionTitle(currentLang),
                            onClick = onHelpCenterClick
                        )
                    )
                }
                actionItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowItems.forEach { item ->
                            StatsActionTile(
                                item = item,
                                isDark = isDark,
                                onClick = {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    item.onClick()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 24.dp)
                .size(64.dp)
                .clip(CircleShape)
                .bounceClick(onClick = { onDismiss() })
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private data class StatsActionItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit
)

@Composable
private fun StatsActionTile(
    item: StatsActionItem,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .bounceClick(onClick = onClick)
                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.Black.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f)
            )
        }
    }
}
