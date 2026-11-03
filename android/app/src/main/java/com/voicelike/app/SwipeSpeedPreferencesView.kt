package com.voicelike.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwipeSpeedPreferencesView(
    horizontalSpeedMultiplier: Float,
    upSpeedMultiplier: Float,
    onHorizontalSpeedMultiplierChange: (Float) -> Unit,
    onUpSpeedMultiplierChange: (Float) -> Unit,
    onRestoreDefaults: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var tempHorizontalSpeedTenths by remember(horizontalSpeedMultiplier) {
        mutableIntStateOf(swipeSpeedMultiplierToTenths(horizontalSpeedMultiplier))
    }
    var tempUpSpeedTenths by remember(upSpeedMultiplier) {
        mutableIntStateOf(swipeSpeedMultiplierToTenths(upSpeedMultiplier))
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            UnifiedSubpageTopBar(
                title = strings.swipeSpeed,
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SwipeSpeedSliderCard(
                    title = "${strings.swipeLeft} / ${strings.swipeRight}",
                    valueTenths = tempHorizontalSpeedTenths,
                    isDark = isDark,
                    onValueChange = { valueTenths ->
                        if (valueTenths != tempHorizontalSpeedTenths) {
                            tempHorizontalSpeedTenths = valueTenths
                            if (hapticsAllowed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onValueChangeFinished = {
                        val resolvedMultiplier =
                            swipeSpeedTenthsToMultiplier(
                                value = tempHorizontalSpeedTenths,
                                defaultValue = horizontalSpeedMultiplier
                            )
                        if (resolvedMultiplier != horizontalSpeedMultiplier) {
                            onHorizontalSpeedMultiplierChange(resolvedMultiplier)
                        }
                    }
                )

                SwipeSpeedSliderCard(
                    title = strings.swipeUp,
                    valueTenths = tempUpSpeedTenths,
                    isDark = isDark,
                    onValueChange = { valueTenths ->
                        if (valueTenths != tempUpSpeedTenths) {
                            tempUpSpeedTenths = valueTenths
                            if (hapticsAllowed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onValueChangeFinished = {
                        val resolvedMultiplier =
                            swipeSpeedTenthsToMultiplier(
                                value = tempUpSpeedTenths,
                                defaultValue = upSpeedMultiplier
                            )
                        if (resolvedMultiplier != upSpeedMultiplier) {
                            onUpSpeedMultiplierChange(resolvedMultiplier)
                        }
                    }
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val restoreText = getNewPageStrings(LocalAppLanguage.current).restoreDefaultsText
                    TextButton(
                        onClick = {
                            tempHorizontalSpeedTenths =
                                swipeSpeedMultiplierToTenths(DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER)
                            tempUpSpeedTenths =
                                swipeSpeedMultiplierToTenths(DEFAULT_UP_SWIPE_SPEED_MULTIPLIER)
                            onRestoreDefaults()
                            if (hapticsAllowed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    ) {
                        Text(
                            text = restoreText,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SwipeSpeedSliderCard(
    title: String,
    valueTenths: Int,
    isDark: Boolean,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val value = valueTenths.toFloat()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .advancedTextureCard(shape = RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = String.format("%.1fx", valueTenths / 10f),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0.5x",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    text = "2.0x",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Slider(
                value = value,
                onValueChange = { rawValue ->
                    onValueChange(rawValue.toInt().coerceIn(5, 20))
                },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 5f..20f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                    activeTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                    inactiveTrackColor = if (isDark) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    } else {
                        Color.Gray.copy(alpha = 0.3f)
                    },
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
        }
    }
}
