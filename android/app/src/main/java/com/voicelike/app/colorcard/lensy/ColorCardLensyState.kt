package com.voicelike.app.colorcard.lensy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.voicelike.app.ColorCardTraceFilterState
import com.voicelike.app.ColorCardTracePanelMode
import com.voicelike.app.renderColorCardTraceDiagnosticBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val COLOR_CARD_LENSY_ANALYSIS_DEBOUNCE_MS = 260L

@Composable
internal fun rememberColorCardLensyAssessment(
    enabled: Boolean,
    effectPanelMode: ColorCardTracePanelMode,
    traceState: ColorCardTraceFilterState
): ColorCardLensyAssessment? {
    val context = LocalContext.current
    var assessment by remember(traceState.previewSourceBitmap, traceState.mode) {
        mutableStateOf<ColorCardLensyAssessment?>(null)
    }
    val analysisKey = remember(
        enabled,
        effectPanelMode,
        traceState.mode,
        traceState.selectedCandidate?.paletteId,
        traceState.color,
        traceState.sharpen,
        traceState.tone,
        traceState.saturation,
        traceState.brightness,
        traceState.paletteStrength,
        traceState.isRendering,
        traceState.sourceColors,
        traceState.displayColors
    ) {
        buildString {
            append(enabled)
            append('|')
            append(effectPanelMode.name)
            append('|')
            append(traceState.mode.name)
            append('|')
            append(traceState.selectedCandidate?.paletteId.orEmpty())
            append('|')
            append(traceState.color)
            append('|')
            append(traceState.sharpen)
            append('|')
            append(traceState.tone)
            append('|')
            append(traceState.saturation)
            append('|')
            append(traceState.brightness)
            append('|')
            append(traceState.paletteStrength)
            append('|')
            append(traceState.isRendering)
            append('|')
            append(traceState.sourceColors.joinToString(","))
            append('|')
            append(traceState.displayColors.joinToString(","))
        }
    }

    LaunchedEffect(analysisKey) {
        if (
            !enabled ||
            effectPanelMode == ColorCardTracePanelMode.Picker ||
            effectPanelMode == ColorCardTracePanelMode.Recipe ||
            !traceState.isRendering ||
            !traceState.canSaveRenderedImage
        ) {
            assessment = null
            return@LaunchedEffect
        }

        delay(COLOR_CARD_LENSY_ANALYSIS_DEBOUNCE_MS)

        val diagnosticBitmap = renderColorCardTraceDiagnosticBitmap(
            context = context.applicationContext,
            traceState = traceState
        )
        assessment = try {
            diagnosticBitmap?.let { bitmap ->
                withContext(Dispatchers.Default) {
                    analyzeColorCardLensyBitmap(bitmap)
                }
            }
        } finally {
            diagnosticBitmap
                ?.takeIf { bitmap ->
                    !bitmap.isRecycled &&
                        bitmap !== traceState.previewSourceBitmap
                }
                ?.recycle()
        }
    }

    return assessment
}
