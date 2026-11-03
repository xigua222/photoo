package com.voicelike.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.liquidglass.LocalLiquidGlassBackdrop
import com.voicelike.app.liquidglass.liquidGlassPanel
import com.voicelike.app.liquidglass.liquidGlassSurfaceColor

data class WatermarkCompactPanelTabSpec(
    val key: String,
    val title: String,
    val iconResId: Int? = null
)

@Composable
fun WatermarkCompactDockHeader(
    items: List<WatermarkCompactPanelTabSpec>,
    selectedKey: String,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onSelect: (String) -> Unit,
    containerColor: Color,
    activeBackgroundColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    fun performHaptic() {
        if (hapticsAllowed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val compactMode = items.size <= 2
    val tabArrangement = if (compactMode) {
        Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
    } else {
        Arrangement.spacedBy(3.dp)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .watermarkDockSurface(RoundedCornerShape(14.dp), containerColor, outlineColor)
                .clickable {
                    performHaptic()
                    onToggleCollapsed()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (collapsed) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (collapsed) "Expand panel" else "Collapse panel",
                tint = primaryTextColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .watermarkDockSurface(RoundedCornerShape(18.dp), containerColor, outlineColor)
                .padding(horizontal = 3.dp, vertical = 3.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = tabArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.key == selectedKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) activeBackgroundColor else Color.Transparent)
                        .clickable {
                            performHaptic()
                            onSelect(item.key)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.title,
                        color = if (selected) primaryTextColor else secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun WatermarkCompactPresetCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    containerColor: Color,
    selectedContainerColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) selectedContainerColor else containerColor,
        border = BorderStroke(1.dp, outlineColor),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (hapticsAllowed) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = secondaryTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun WatermarkCompactPanelTabs(
    items: List<WatermarkCompactPanelTabSpec>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    containerColor: Color,
    selectedContainerColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val selected = item.key == selectedKey
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selected) selectedContainerColor else containerColor,
                border = BorderStroke(1.dp, outlineColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (hapticsAllowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onSelect(item.key)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.iconResId != null) {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = null,
                            tint = if (selected) primaryTextColor else secondaryTextColor
                        )
                    }
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) primaryTextColor else secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun WatermarkCompactPanelSurface(
    title: String? = null,
    subtitle: String? = null,
    containerColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassBackdrop = LocalLiquidGlassBackdrop.current
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = if (glassBackdrop != null) Color.Transparent else containerColor,
        border = if (glassBackdrop != null) null else BorderStroke(1.dp, outlineColor),
        modifier = if (glassBackdrop != null) {
            modifier.liquidGlassPanel(glassBackdrop, shape, watermarkDockGlassSurfaceColor())
        } else {
            modifier
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTextColor
                )
            }
            if (!subtitle.isNullOrBlank()) {
                if (!title.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = secondaryTextColor
                )
            }
            if (!title.isNullOrBlank() || !subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun WatermarkCompactSwitchCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    containerColor: Color,
    selectedContainerColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (checked) selectedContainerColor else containerColor,
        border = BorderStroke(1.dp, outlineColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (trailingContent != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            trailingContent()
                        }
                    }
                    if (!supportingText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = supportingText,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = secondaryTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        if (hapticsAllowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onCheckedChange(it)
                    },
                    modifier = Modifier.scale(0.86f)
                )
            }
        }
    }
}

@Composable
fun WatermarkCompactSliderCard(
    title: String,
    containerColor: Color,
    outlineColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, outlineColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = primaryTextColor
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = secondaryTextColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

/** Dock chrome floats over the photo preview, so it turns to glass when the preview is recorded. */
@Composable
private fun Modifier.watermarkDockSurface(shape: Shape, containerColor: Color, outlineColor: Color): Modifier {
    val glassBackdrop = LocalLiquidGlassBackdrop.current
    return if (glassBackdrop != null) {
        clip(shape).liquidGlassPanel(glassBackdrop, shape, watermarkDockGlassSurfaceColor())
    } else {
        clip(shape).background(containerColor).border(1.dp, outlineColor, shape)
    }
}

@Composable
private fun watermarkDockGlassSurfaceColor(): Color =
    liquidGlassSurfaceColor(isLightTheme = MaterialTheme.colorScheme.background.luminance() >= 0.5f, alpha = 0.55f)
