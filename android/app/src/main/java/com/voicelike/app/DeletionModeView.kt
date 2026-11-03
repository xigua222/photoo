package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.ThumbUp

@Composable
fun DeletionModeView(
    currentMode: String, // "recycle_bin" or "direct_delete"
    onModeSelected: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit = onBack
) {
    val strings = LocalAppStrings.current
    val preferenceCopy = onboardingPreferenceCopy(LocalAppLanguage.current)
    val newPageStrings = getNewPageStrings(LocalAppLanguage.current)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val layoutDirection = LocalLayoutDirection.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    
    // Temporary state to hold selection before saving
    var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            UnifiedSubpageTopBar(
                title = preferenceCopy.organizeDeleteTitle,
                onBack = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }
            ) {
                TextButton(
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onModeSelected(selectedMode)
                        onSave()
                    }
                ) {
                    Text(
                        text = strings.done,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Reassurance Text (Moved to top)
                Text(
                    text = newPageStrings.deletionModeReassurance,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Side-by-Side Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max), // Ensure equal height
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Recycle Bin Mode Card (Left)
                    DeletionModeVerticalCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = strings.recycleBinModeTitle,
                        description = strings.recycleBinModeDesc,
                        icon = Icons.Outlined.RestoreFromTrash,
                        isSelected = selectedMode == "recycle_bin",
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedMode = "recycle_bin"
                        },
                        isDark = isDark,
                        accentColor = Color(0xFF2196F3), // Blue
                        isRecommended = true // Add recommendation
                    )

                    // Direct Delete Mode Card (Right)
                    DeletionModeVerticalCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = strings.directDeleteModeTitle,
                        description = strings.directDeleteModeDesc,
                        icon = Icons.Outlined.DeleteForever,
                        isSelected = selectedMode == "direct_delete",
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedMode = "direct_delete"
                        },
                        isDark = isDark,
                        accentColor = Color(0xFFEF4444) // Red
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DeletionModeVerticalCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    accentColor: Color,
    isRecommended: Boolean = false
) {
    val newPageStrings = getNewPageStrings(LocalAppLanguage.current)
    val borderColor = if (isSelected) accentColor else Color.Transparent
    val backgroundColor = if (isSelected) {
        accentColor.copy(alpha = 0.1f)
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F6)
    }
    val iconColor = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp
            )
        }

        // Recommended Badge
        if (isRecommended) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8.dp))
                    .background(Color(0xFFFFC107), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = newPageStrings.recommendedBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
