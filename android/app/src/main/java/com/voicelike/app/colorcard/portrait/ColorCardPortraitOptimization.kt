package com.voicelike.app.colorcard.portrait

import com.voicelike.app.ColorCardTraceFaceRegion
import com.voicelike.app.ColorCardTraceFilterCandidate
import com.voicelike.app.ColorCardTraceFilterKind
import com.voicelike.app.ColorCardTraceLab
import com.voicelike.app.ColorCardTracePanelMode

internal data class ColorCardPortraitOptimizationParams(
    val enabled: Boolean,
    val lightnessLift: Float,
    val saturationBoost: Float,
    val sharpenRetention: Float,
    val softenAmount: Float
)

internal val COLOR_CARD_TRACE_PORTRAIT_DISABLED = ColorCardPortraitOptimizationParams(
    enabled = false,
    lightnessLift = 0f,
    saturationBoost = 0f,
    sharpenRetention = 1f,
    softenAmount = 0f
)

private const val COLOR_CARD_TRACE_PORTRAIT_MIN_FACE_AREA = 0.0105f
private const val COLOR_CARD_TRACE_PORTRAIT_IDEAL_FACE_AREA = 0.032f
private const val COLOR_CARD_TRACE_PORTRAIT_MAX_LIGHTNESS_LIFT = 4.8f
private const val COLOR_CARD_TRACE_PORTRAIT_MAX_SATURATION_BOOST = 0.045f
private const val COLOR_CARD_TRACE_PORTRAIT_MIN_SHARPEN_RETENTION = 0.58f
private const val COLOR_CARD_TRACE_PORTRAIT_MAX_SUPPORTED_FACES = 3

internal fun resolveColorCardPortraitOptimizationParams(
    panelMode: ColorCardTracePanelMode,
    selectedCandidate: ColorCardTraceFilterCandidate?,
    faceRegions: List<ColorCardTraceFaceRegion>
): ColorCardPortraitOptimizationParams {
    if (faceRegions.isEmpty()) return COLOR_CARD_TRACE_PORTRAIT_DISABLED
    if (panelMode != ColorCardTracePanelMode.Filter && panelMode != ColorCardTracePanelMode.Trace) {
        return COLOR_CARD_TRACE_PORTRAIT_DISABLED
    }
    if (faceRegions.size > COLOR_CARD_TRACE_PORTRAIT_MAX_SUPPORTED_FACES) {
        return COLOR_CARD_TRACE_PORTRAIT_DISABLED
    }
    if (panelMode == ColorCardTracePanelMode.Filter) {
        when (selectedCandidate?.kind) {
            ColorCardTraceFilterKind.PixelPoster,
            ColorCardTraceFilterKind.SubjectFrame,
            ColorCardTraceFilterKind.BlackMist -> return COLOR_CARD_TRACE_PORTRAIT_DISABLED
            else -> Unit
        }
    }

    val largestFaceArea = faceRegions.maxOf { region ->
        (region.radiusX * region.radiusY).coerceAtLeast(0f)
    }
    if (largestFaceArea < COLOR_CARD_TRACE_PORTRAIT_MIN_FACE_AREA) {
        return COLOR_CARD_TRACE_PORTRAIT_DISABLED
    }

    val faceScale = ((largestFaceArea - COLOR_CARD_TRACE_PORTRAIT_MIN_FACE_AREA) /
        (COLOR_CARD_TRACE_PORTRAIT_IDEAL_FACE_AREA - COLOR_CARD_TRACE_PORTRAIT_MIN_FACE_AREA))
        .coerceIn(0f, 1f)
    val crowdPenalty = when (faceRegions.size) {
        1 -> 1f
        2 -> 0.78f
        3 -> 0.62f
        else -> 0f
    }
    val effectScale = (0.42f + (0.58f * faceScale)) * crowdPenalty
    if (effectScale <= 0.12f) return COLOR_CARD_TRACE_PORTRAIT_DISABLED

    return ColorCardPortraitOptimizationParams(
        enabled = true,
        lightnessLift = COLOR_CARD_TRACE_PORTRAIT_MAX_LIGHTNESS_LIFT * effectScale,
        saturationBoost = COLOR_CARD_TRACE_PORTRAIT_MAX_SATURATION_BOOST * effectScale,
        sharpenRetention = 1f - ((1f - COLOR_CARD_TRACE_PORTRAIT_MIN_SHARPEN_RETENTION) * effectScale),
        softenAmount = 0f
    )
}

internal fun applyColorCardPortraitOptimization(
    lab: ColorCardTraceLab,
    portraitMask: Float,
    params: ColorCardPortraitOptimizationParams
): ColorCardTraceLab {
    if (!params.enabled) return lab
    val safeMask = portraitMask.coerceIn(0f, 1f)
    if (safeMask <= 0.0001f) return lab

    val normalizedLightness = (lab.l / 100.0).toFloat().coerceIn(0f, 1f)
    val liftZone = colorCardPortraitLiftZone(normalizedLightness)
    if (liftZone <= 0.0001f) return lab

    val effectiveMask = safeMask * liftZone
    val saturationFactor = 1.0 + (params.saturationBoost * effectiveMask)
    return lab.copy(
        l = (lab.l + (params.lightnessLift * effectiveMask)).coerceIn(0.0, 100.0),
        a = lab.a * saturationFactor,
        b = lab.b * saturationFactor
    )
}

internal fun resolveColorCardPortraitSharpen(
    sharpen: Float,
    portraitMask: Float,
    params: ColorCardPortraitOptimizationParams
): Float {
    if (!params.enabled) return sharpen
    if (sharpen <= 0f) return sharpen
    val safeMask = portraitMask.coerceIn(0f, 1f)
    if (safeMask <= 0.0001f) return sharpen
    val retention = 1f - ((1f - params.sharpenRetention.coerceIn(0f, 1f)) * safeMask)
    return sharpen * retention
}

private fun colorCardPortraitLiftZone(normalizedLightness: Float): Float {
    val safeLightness = normalizedLightness.coerceIn(0f, 1f)
    return when {
        safeLightness <= 0.08f || safeLightness >= 0.90f -> 0f
        safeLightness < 0.26f -> colorCardPortraitSmoothStep(0.08f, 0.26f, safeLightness)
        safeLightness > 0.72f -> 1f - colorCardPortraitSmoothStep(0.72f, 0.90f, safeLightness)
        else -> 1f
    }
}

private fun colorCardPortraitSmoothStep(
    edge0: Float,
    edge1: Float,
    value: Float
): Float {
    if (edge0 == edge1) return if (value >= edge1) 1f else 0f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - (2f * t))
}
