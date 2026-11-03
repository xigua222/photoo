package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun buildColorCardMetricInfoTextCompactForSharingV2(
    colorInt: Int,
    metrics: ColorCardMetrics
): String {
    val harmonySwatches = resolveColorCardHarmonySwatches(colorInt)
    return buildString {
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("HEXA: #")
        append(metrics.hexa)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("RGBA: ")
        append(metrics.rgba)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("HSL: ")
        append(metrics.hsl)
        append('\n')
        append("LAB: ")
        append(metrics.lab)
        append('\n')
        append("\u0394E00 \u00b7 \u767d\u5e95: ")
        append(metrics.deltaE00OnWhite)
        append('\n')
        append("\u0394E00 \u00b7 \u9ed1\u5e95: ")
        append(metrics.deltaE00OnBlack)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
        append('\n')
        append('\n')
        append(COLOR_CARD_HARMONY_SECTION_TITLE)
        append(':')
        harmonySwatches.forEach { swatch ->
            append('\n')
            append(swatch.label)
            append(": ")
            append(swatch.hex)
        }
        append('\n')
        append('\n')
        append("\u5bf9\u6bd4\u5ea6 \u00b7 \u767d\u5e95: ")
        append(metrics.contrastOnWhite)
        append('\n')
        append("WCAG \u00b7 \u767d\u5e95: ")
        append(metrics.wcagOnWhite)
        append('\n')
        append("\u5bf9\u6bd4\u5ea6 \u00b7 \u9ed1\u5e95: ")
        append(metrics.contrastOnBlack)
        append('\n')
        append("WCAG \u00b7 \u9ed1\u5e95: ")
        append(metrics.wcagOnBlack)
        append('\n')
        append("\u63a8\u8350\u6587\u5b57: ")
        append(metrics.wcagRecommendedText)
    }
}

@Composable
internal fun ColorCardMetricInfoDialogCompactV2(
    colorInt: Int,
    metrics: ColorCardMetrics,
    onDismissRequest: () -> Unit,
    onCopy: () -> Unit
) {
    val language = LocalAppLanguage.current
    val commonUiCopy = remember(language) { getCommonGlobalUiCopy(language) }
    val harmonySwatches = remember(colorInt) { resolveColorCardHarmonySwatches(colorInt) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = commonUiCopy.details)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ColorCardMetricInfoRowV2(label = "HEX", value = "#${metrics.hex}")
                ColorCardMetricInfoRowV2(label = "HEXA", value = "#${metrics.hexa}")
                ColorCardMetricInfoRowV2(label = "RGB", value = metrics.rgb)
                ColorCardMetricInfoRowV2(label = "RGBA", value = metrics.rgba)
                ColorCardMetricInfoRowV2(label = "HSB", value = metrics.hsb)
                ColorCardMetricInfoRowV2(label = "HSL", value = metrics.hsl)
                ColorCardMetricInfoRowV2(label = "LAB", value = metrics.lab)
                ColorCardMetricInfoRowV2(
                    label = "\u0394E00 \u00b7 \u767d\u5e95",
                    value = metrics.deltaE00OnWhite
                )
                ColorCardMetricInfoRowV2(
                    label = "\u0394E00 \u00b7 \u9ed1\u5e95",
                    value = metrics.deltaE00OnBlack
                )
                ColorCardMetricInfoRowV2(label = "CMYK", value = metrics.cmyk)
                ColorCardHarmonyInfoSectionV2(swatches = harmonySwatches)
                ColorCardMetricInfoRowV2(
                    label = "\u5bf9\u6bd4\u5ea6 \u00b7 \u767d\u5e95",
                    value = metrics.contrastOnWhite
                )
                ColorCardMetricInfoRowV2(
                    label = "WCAG \u00b7 \u767d\u5e95",
                    value = metrics.wcagOnWhite
                )
                ColorCardMetricInfoRowV2(
                    label = "\u5bf9\u6bd4\u5ea6 \u00b7 \u9ed1\u5e95",
                    value = metrics.contrastOnBlack
                )
                ColorCardMetricInfoRowV2(
                    label = "WCAG \u00b7 \u9ed1\u5e95",
                    value = metrics.wcagOnBlack
                )
                ColorCardMetricInfoRowV2(
                    label = "\u63a8\u8350\u6587\u5b57",
                    value = metrics.wcagRecommendedText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text(text = commonUiCopy.copyAll)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = commonUiCopy.close)
            }
        }
    )
}

@Composable
private fun ColorCardHarmonyInfoSectionV2(
    swatches: List<ColorCardHarmonySwatch>
) {
    if (swatches.isEmpty()) return
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = COLOR_CARD_HARMONY_SECTION_TITLE,
            color = secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        swatches.forEach { swatch ->
            val swatchShape = RoundedCornerShape(4.dp)
            val showBorder = Color(swatch.colorInt).luminance() > 0.92f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(swatchShape)
                        .background(Color(swatch.colorInt))
                        .then(
                            if (showBorder) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                    shape = swatchShape
                                )
                            } else {
                                Modifier
                            }
                        )
                )
                Text(
                    text = swatch.label,
                    color = secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(68.dp)
                )
                Text(
                    text = swatch.hex,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ColorCardMetricInfoRowV2(
    label: String,
    value: String
) {
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
