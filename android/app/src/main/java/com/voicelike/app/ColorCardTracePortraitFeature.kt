package com.voicelike.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.colorcard.portrait.COLOR_CARD_TRACE_PORTRAIT_DISABLED
import com.voicelike.app.colorcard.portrait.ColorCardPortraitOptimizationParams
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

internal const val COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE = 0f

internal data class ColorCardTracePortraitAdjustments(
    val vitality: Float = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE,
    val softness: Float = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE,
    val detailSuppression: Float = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE
) {
    val hasAdjustments: Boolean
        get() = vitality > 0.0001f || softness > 0.0001f || detailSuppression > 0.0001f

    val isDefault: Boolean
        get() = approxEquals(vitality, COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE) &&
            approxEquals(softness, COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE) &&
            approxEquals(detailSuppression, COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE)
}

private const val COLOR_CARD_TRACE_PORTRAIT_EXTRA_LIGHTNESS = 10.8f
private const val COLOR_CARD_TRACE_PORTRAIT_EXTRA_SATURATION = 0.17f
private const val COLOR_CARD_TRACE_PORTRAIT_EXTRA_SOFTNESS = 1.18f
private const val COLOR_CARD_TRACE_PORTRAIT_EXTRA_DETAIL_SUPPRESSION = 1.12f
private const val COLOR_CARD_TRACE_PORTRAIT_VITALITY_RESPONSE_POWER = 0.72f
private const val COLOR_CARD_TRACE_PORTRAIT_SOFTNESS_RESPONSE_POWER = 0.56f
private const val COLOR_CARD_TRACE_PORTRAIT_DETAIL_RESPONSE_POWER = 0.60f

internal fun resolveColorCardTracePortraitFeatureAvailable(
    panelMode: ColorCardTracePanelMode,
    selectedCandidate: ColorCardTraceFilterCandidate?,
    faceRegions: List<ColorCardTraceFaceRegion>
): Boolean {
    if (faceRegions.isEmpty()) return false
    return when (panelMode) {
        ColorCardTracePanelMode.Filter -> when (selectedCandidate?.kind) {
            ColorCardTraceFilterKind.HaldClut,
            ColorCardTraceFilterKind.SelectiveColor -> true
            else -> false
        } && !isColorCardTraceBlackWhiteEraCandidate(selectedCandidate)
        ColorCardTracePanelMode.Trace -> selectedCandidate?.kind == ColorCardTraceFilterKind.Palette
        ColorCardTracePanelMode.Imitate -> selectedCandidate?.kind == ColorCardTraceFilterKind.Palette
        ColorCardTracePanelMode.Picker,
        ColorCardTracePanelMode.Recipe -> false
    }
}

internal fun resolveColorCardTracePortraitFeatureParams(
    enabled: Boolean,
    faceRegions: List<ColorCardTraceFaceRegion>,
    baseParams: ColorCardPortraitOptimizationParams,
    adjustments: ColorCardTracePortraitAdjustments
): ColorCardPortraitOptimizationParams {
    if (!enabled) {
        return COLOR_CARD_TRACE_PORTRAIT_DISABLED
    }
    val manualEnabled = faceRegions.isNotEmpty() && adjustments.hasAdjustments
    if (!manualEnabled) {
        return COLOR_CARD_TRACE_PORTRAIT_DISABLED
    }
    val vitalityResponse = colorCardTracePortraitResponseCurve(
        adjustments.vitality,
        COLOR_CARD_TRACE_PORTRAIT_VITALITY_RESPONSE_POWER
    )
    val softnessResponse = colorCardTracePortraitResponseCurve(
        adjustments.softness,
        COLOR_CARD_TRACE_PORTRAIT_SOFTNESS_RESPONSE_POWER
    )
    val detailResponse = colorCardTracePortraitResponseCurve(
        adjustments.detailSuppression,
        COLOR_CARD_TRACE_PORTRAIT_DETAIL_RESPONSE_POWER
    )

    return ColorCardPortraitOptimizationParams(
        enabled = true,
        lightnessLift = (vitalityResponse * (baseParams.lightnessLift + COLOR_CARD_TRACE_PORTRAIT_EXTRA_LIGHTNESS))
            .coerceIn(0f, 15.5f),
        saturationBoost = (vitalityResponse * (baseParams.saturationBoost + COLOR_CARD_TRACE_PORTRAIT_EXTRA_SATURATION))
            .coerceIn(0f, 0.38f),
        sharpenRetention = (
            1f - (detailResponse * COLOR_CARD_TRACE_PORTRAIT_EXTRA_DETAIL_SUPPRESSION)
            ).coerceIn(0.03f, 1f),
        softenAmount = (softnessResponse * COLOR_CARD_TRACE_PORTRAIT_EXTRA_SOFTNESS)
            .coerceIn(0f, 1.35f)
    )
}

private fun colorCardTracePortraitResponseCurve(
    value: Float,
    power: Float
): Float {
    val safeValue = value.coerceIn(0f, 1f)
    if (safeValue <= 0.0001f) return 0f
    return safeValue.toDouble()
        .pow(power.toDouble())
        .toFloat()
        .coerceIn(0f, 1f)
}

@Composable
internal fun ColorCardTracePortraitModeButton(
    selected: Boolean,
    primaryText: Color,
    secondaryText: Color,
    activeBackground: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (selected) activeBackground else Color.Transparent,
                    RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Face,
                contentDescription = "\u4eba\u50cf",
                tint = if (selected) primaryText else secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun ColorCardTracePortraitPanel(
    adjustments: ColorCardTracePortraitAdjustments,
    faceCount: Int,
    isDark: Boolean,
    panelShape: RoundedCornerShape,
    backgroundColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onVitalityChange: (Float) -> Unit,
    onSoftnessChange: (Float) -> Unit,
    onDetailSuppressionChange: (Float) -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit
) {
    val description = if (faceCount > 1) {
        "\u5df2\u8bc6\u522b $faceCount \u5f20\u4eba\u8138\uff0c\u8c03\u6574\u4f1a\u7edf\u4e00\u8986\u76d6\u6240\u6709\u4eba\u50cf\u533a\u57df\u3002"
    } else {
        "\u4ec5\u4f18\u5316\u5f53\u524d\u4eba\u50cf\u533a\u57df\uff0c\u4e0d\u6539\u52a8\u73b0\u6709\u6ee4\u955c\u548c\u8ffd\u8272\u53c2\u6570\u3002"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, panelShape)
            .border(1.dp, borderColor, panelShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "\u4eba\u50cf",
                    color = primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = secondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorCardTracePortraitActionButton(
                    label = "\u8fd8\u539f",
                    enabled = !adjustments.isDefault,
                    isDark = isDark,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onClick = onReset
                )
                ColorCardTracePortraitActionButton(
                    label = "\u786e\u8ba4",
                    enabled = true,
                    isDark = isDark,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onClick = onConfirm
                )
            }
        }

        ColorCardTracePortraitSliderRow(
            label = "\u6c14\u8272",
            value = adjustments.vitality,
            primaryText = primaryText,
            secondaryText = secondaryText,
            onValueChange = onVitalityChange
        )
        ColorCardTracePortraitSliderRow(
            label = "\u67d4\u548c",
            value = adjustments.softness,
            primaryText = primaryText,
            secondaryText = secondaryText,
            onValueChange = onSoftnessChange
        )
        ColorCardTracePortraitSliderRow(
            label = "\u538b\u9510",
            value = adjustments.detailSuppression,
            primaryText = primaryText,
            secondaryText = secondaryText,
            onValueChange = onDetailSuppressionChange
        )
    }
}

private fun approxEquals(left: Float, right: Float): Boolean {
    return kotlin.math.abs(left - right) <= 0.0001f
}

@Composable
private fun ColorCardTracePortraitActionButton(
    label: String,
    enabled: Boolean,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.045f),
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) primaryText else secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ColorCardTracePortraitSliderRow(
    label: String,
    value: Float,
    primaryText: Color,
    secondaryText: Color,
    onValueChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    var lastHapticStep by remember(label) {
        mutableIntStateOf((value.coerceIn(0f, 1f) * 10f).roundToInt())
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = ((value.coerceIn(0f, 1f) * 10f).roundToInt()).toString(),
                color = secondaryText,
                fontSize = 11.sp
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { raw ->
                val snappedStep = (raw.coerceIn(0f, 1f) * 10f).roundToInt()
                if (snappedStep != lastHapticStep) {
                    lastHapticStep = snappedStep
                    if (hapticsAllowed) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                onValueChange(snappedStep / 10f)
            },
            valueRange = 0f..1f
        )
    }
}

@Composable
internal fun BoxScope.ColorCardTracePortraitFacesOverlay(
    faceRegions: List<ColorCardTraceFaceRegion>,
    modifier: Modifier = Modifier
) {
    if (faceRegions.isEmpty()) return

    Canvas(
        modifier = modifier.matchParentSize()
    ) {
        val strokeWidth = 2.2.dp.toPx()
        val shadowWidth = strokeWidth + 1.6.dp.toPx()
        val minCorner = 18.dp.toPx()
        val maxCorner = 34.dp.toPx()

        faceRegions.forEach { region ->
            val left = ((region.centerX - region.radiusX).coerceIn(0f, 1f) * size.width)
            val top = ((region.centerY - region.radiusY).coerceIn(0f, 1f) * size.height)
            val right = ((region.centerX + region.radiusX).coerceIn(0f, 1f) * size.width)
            val bottom = ((region.centerY + region.radiusY).coerceIn(0f, 1f) * size.height)
            val width = (right - left).coerceAtLeast(1f)
            val height = (bottom - top).coerceAtLeast(1f)
            val corner = min(max(min(width, height) * 0.22f, minCorner), maxCorner)

            drawPortraitFaceCorners(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                corner = corner,
                strokeWidth = shadowWidth,
                color = Color.Black.copy(alpha = 0.22f)
            )
            drawPortraitFaceCorners(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                corner = corner,
                strokeWidth = strokeWidth,
                color = Color.White.copy(alpha = 0.96f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPortraitFaceCorners(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    corner: Float,
    strokeWidth: Float,
    color: Color
) {
    fun line(start: Offset, end: Offset) {
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }

    line(Offset(left, top), Offset(left + corner, top))
    line(Offset(left, top), Offset(left, top + corner))

    line(Offset(right, top), Offset(right - corner, top))
    line(Offset(right, top), Offset(right, top + corner))

    line(Offset(left, bottom), Offset(left + corner, bottom))
    line(Offset(left, bottom), Offset(left, bottom - corner))

    line(Offset(right, bottom), Offset(right - corner, bottom))
    line(Offset(right, bottom), Offset(right, bottom - corner))
}
