package com.voicelike.app

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class WatermarkSubjectOutlinePanelState(
    val outerBorderEnabled: Boolean,
    val borderColorIndex: Int,
    val borderCornerMode: BorderCornerMode,
    val borderImageScaleFactorDraft: Float,
    val lineStyle: WatermarkSubjectOutlineLineStyle,
    val offsetRatioDraft: Float,
    val widthRatioDraft: Float,
    val symbolText: String,
    val symbolSizeRatioDraft: Float,
    val outlineColorIndex: Int
)

internal data class WatermarkSubjectOutlinePanelColors(
    val panelBackground: Color,
    val panelSelectedBackground: Color,
    val cardOutlineColor: Color,
    val primaryTextColor: Color,
    val secondaryTextColor: Color,
    val isDark: Boolean
)

internal data class WatermarkSubjectOutlinePanelLabels(
    val borderCorner: String,
    val imageRatio: String
)

internal data class WatermarkSubjectOutlinePanelCallbacks(
    val onOuterBorderEnabledChange: (Boolean) -> Unit,
    val onBorderColorIndexChange: (Int) -> Unit,
    val onBorderCornerModeChange: (BorderCornerMode) -> Unit,
    val onBorderImageScaleFactorDraftChange: (Float) -> Unit,
    val onBorderImageScaleFactorCommit: () -> Unit,
    val onLineStyleChange: (WatermarkSubjectOutlineLineStyle) -> Unit,
    val onOffsetRatioDraftChange: (Float) -> Unit,
    val onOffsetRatioCommit: () -> Unit,
    val onWidthRatioDraftChange: (Float) -> Unit,
    val onWidthRatioCommit: () -> Unit,
    val onSymbolTextChange: (String) -> Unit,
    val onSymbolSizeRatioDraftChange: (Float) -> Unit,
    val onSymbolSizeRatioCommit: () -> Unit,
    val onOutlineColorIndexChange: (Int) -> Unit
)

@Composable
internal fun WatermarkSubjectOutlinePanel(
    language: AppLanguage,
    labels: WatermarkSubjectOutlinePanelLabels,
    state: WatermarkSubjectOutlinePanelState,
    colors: WatermarkSubjectOutlinePanelColors,
    callbacks: WatermarkSubjectOutlinePanelCallbacks
) {
    Text(
        text = watermarkSubjectOutlineSectionTitle(language),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = colors.primaryTextColor
    )
    Spacer(modifier = Modifier.height(8.dp))
    WatermarkCompactSwitchCard(
        title = watermarkSubjectOutlineOuterBorderLabel(language),
        checked = state.outerBorderEnabled,
        onCheckedChange = callbacks.onOuterBorderEnabledChange,
        containerColor = colors.panelBackground,
        selectedContainerColor = colors.panelSelectedBackground,
        outlineColor = colors.cardOutlineColor,
        primaryTextColor = colors.primaryTextColor,
        secondaryTextColor = colors.secondaryTextColor,
        supportingText = watermarkSubjectOutlineOuterBorderHint(language)
    )
    if (state.outerBorderEnabled) {
        WatermarkSubjectOutlineOuterBorderControls(
            language = language,
            labels = labels,
            state = state,
            colors = colors,
            callbacks = callbacks
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = watermarkSubjectOutlineStyleLabel(language),
        fontSize = 12.sp,
        color = colors.primaryTextColor
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WatermarkSubjectOutlineLineStyle.entries.forEach { lineStyle ->
            WatermarkSubjectOutlineFilterChip(
                selected = state.lineStyle == lineStyle,
                onClick = { callbacks.onLineStyleChange(lineStyle) },
                label = lineStyle.localizedOutlineLabel(language),
                colors = colors
            )
        }
    }

    WatermarkSubjectOutlineSlider(
        label = watermarkSubjectOutlineDistanceLabel(language),
        value = state.offsetRatioDraft,
        onValueChange = callbacks.onOffsetRatioDraftChange,
        onValueChangeFinished = callbacks.onOffsetRatioCommit,
        valueRange = MIN_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO..MAX_WATERMARK_SUBJECT_OUTLINE_OFFSET_RATIO,
        colors = colors
    )

    if (state.lineStyle != WatermarkSubjectOutlineLineStyle.Symbol) {
        WatermarkSubjectOutlineSlider(
            label = watermarkSubjectOutlineThicknessLabel(language),
            value = state.widthRatioDraft,
            onValueChange = callbacks.onWidthRatioDraftChange,
            onValueChangeFinished = callbacks.onWidthRatioCommit,
            valueRange = MIN_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO..MAX_WATERMARK_SUBJECT_OUTLINE_WIDTH_RATIO,
            colors = colors
        )
    } else {
        WatermarkSubjectOutlineSymbolControls(
            language = language,
            state = state,
            colors = colors,
            callbacks = callbacks
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = watermarkSubjectOutlineColorLabel(language),
        fontSize = 12.sp,
        color = colors.primaryTextColor
    )
    Spacer(modifier = Modifier.height(6.dp))
    WatermarkSubjectOutlineColorRow(
        selectedIndex = state.outlineColorIndex,
        colors = colors,
        onSelect = callbacks.onOutlineColorIndexChange
    )
}

@Composable
private fun WatermarkSubjectOutlineOuterBorderControls(
    language: AppLanguage,
    labels: WatermarkSubjectOutlinePanelLabels,
    state: WatermarkSubjectOutlinePanelState,
    colors: WatermarkSubjectOutlinePanelColors,
    callbacks: WatermarkSubjectOutlinePanelCallbacks
) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = watermarkSubjectOutlineOuterBorderColorLabel(language),
        fontSize = 12.sp,
        color = colors.primaryTextColor
    )
    Spacer(modifier = Modifier.height(6.dp))
    WatermarkSubjectOutlineColorRow(
        selectedIndex = state.borderColorIndex,
        colors = colors,
        onSelect = callbacks.onBorderColorIndexChange
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(labels.borderCorner, fontSize = 12.sp, color = colors.primaryTextColor)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BorderCornerMode.entries.forEach { mode ->
            WatermarkSubjectOutlineFilterChip(
                selected = state.borderCornerMode == mode,
                onClick = { callbacks.onBorderCornerModeChange(mode) },
                label = mode.localizedTitle(language),
                colors = colors
            )
        }
    }
    WatermarkSubjectOutlineSlider(
        label = labels.imageRatio,
        value = state.borderImageScaleFactorDraft,
        onValueChange = callbacks.onBorderImageScaleFactorDraftChange,
        onValueChangeFinished = callbacks.onBorderImageScaleFactorCommit,
        valueRange = 0.78f..1f,
        colors = colors
    )
}

@Composable
private fun WatermarkSubjectOutlineSymbolControls(
    language: AppLanguage,
    state: WatermarkSubjectOutlinePanelState,
    colors: WatermarkSubjectOutlinePanelColors,
    callbacks: WatermarkSubjectOutlinePanelCallbacks
) {
    var symbolTextDraft by rememberSaveable { mutableStateOf<String?>(null) }
    val isDefaultPlaceholder = isWatermarkSubjectOutlineDefaultSymbolText(state.symbolText)
    val displaySymbolText = symbolTextDraft ?: if (isDefaultPlaceholder) "" else state.symbolText

    LaunchedEffect(state.symbolText) {
        if (symbolTextDraft != null && state.symbolText != sanitizeWatermarkSubjectOutlineSymbolText(symbolTextDraft.orEmpty())) {
            symbolTextDraft = null
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = displaySymbolText,
        onValueChange = { value ->
            symbolTextDraft = value
            callbacks.onSymbolTextChange(value)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = {
            Text(
                text = watermarkSubjectOutlineSymbolTextLabel(language),
                color = colors.secondaryTextColor
            )
        },
        placeholder = {
            Text(
                text = state.symbolText.takeIf { it.isNotBlank() }
                    ?: DEFAULT_WATERMARK_SUBJECT_OUTLINE_SYMBOL_TEXT,
                color = colors.secondaryTextColor.copy(alpha = 0.72f)
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
    WatermarkSubjectOutlineSlider(
        label = watermarkSubjectOutlineSymbolSizeLabel(language),
        value = state.symbolSizeRatioDraft,
        onValueChange = callbacks.onSymbolSizeRatioDraftChange,
        onValueChangeFinished = callbacks.onSymbolSizeRatioCommit,
        valueRange = MIN_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO..MAX_WATERMARK_SUBJECT_OUTLINE_SYMBOL_SIZE_RATIO,
        colors = colors
    )
}

@Composable
private fun WatermarkSubjectOutlineSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    colors: WatermarkSubjectOutlinePanelColors
) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = label,
        fontSize = 12.sp,
        color = colors.primaryTextColor
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange
    )
}

@Composable
private fun WatermarkSubjectOutlineColorRow(
    selectedIndex: Int,
    colors: WatermarkSubjectOutlinePanelColors,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FLOATING_COLOR_OPTIONS.forEachIndexed { index, option ->
            WatermarkSubjectOutlineFilterChip(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                label = option.label,
                leadingIcon = {
                    WatermarkSubjectOutlineColorSwatch(
                        color = option.compose,
                        showBorder = option.android == AndroidColor.WHITE,
                        colors = colors
                    )
                },
                colors = colors
            )
        }
    }
}

@Composable
private fun WatermarkSubjectOutlineFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    colors: WatermarkSubjectOutlinePanelColors,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.panelBackground,
            labelColor = colors.primaryTextColor,
            iconColor = colors.primaryTextColor.copy(alpha = 0.78f),
            selectedContainerColor = colors.panelSelectedBackground,
            selectedLabelColor = colors.primaryTextColor,
            selectedLeadingIconColor = colors.primaryTextColor
        )
    )
}

@Composable
private fun WatermarkSubjectOutlineColorSwatch(
    color: Color,
    showBorder: Boolean,
    colors: WatermarkSubjectOutlinePanelColors
) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = 0.8.dp,
                        color = if (colors.isDark) {
                            Color.White.copy(alpha = 0.56f)
                        } else {
                            Color.Black.copy(alpha = 0.36f)
                        },
                        shape = RoundedCornerShape(5.dp)
                    )
                } else {
                    Modifier
                }
            )
            .background(color)
    )
}

private fun WatermarkSubjectOutlineLineStyle.localizedOutlineLabel(
    language: AppLanguage
): String = when (this) {
    WatermarkSubjectOutlineLineStyle.Solid -> watermarkSubjectOutlineSolidLabel(language)
    WatermarkSubjectOutlineLineStyle.Dashed -> watermarkSubjectOutlineDashedLabel(language)
    WatermarkSubjectOutlineLineStyle.Symbol -> watermarkSubjectOutlineSymbolLabel(language)
    WatermarkSubjectOutlineLineStyle.Crayon -> watermarkSubjectOutlineCrayonLabel(language)
    WatermarkSubjectOutlineLineStyle.Chalk -> watermarkSubjectOutlineChalkLabel(language)
    WatermarkSubjectOutlineLineStyle.Pencil -> watermarkSubjectOutlinePencilLabel(language)
    WatermarkSubjectOutlineLineStyle.Marker -> watermarkSubjectOutlineMarkerLabel(language)
    WatermarkSubjectOutlineLineStyle.DoubleSketch -> watermarkSubjectOutlineDoubleSketchLabel(language)
}
