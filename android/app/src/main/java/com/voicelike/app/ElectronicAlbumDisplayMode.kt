package com.voicelike.app

import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.layout.ContentScale

enum class ElectronicAlbumDisplayMode {
    Smart,
    ScaleToFitCenter,
    CropToFitCenter,
    ZoomIn,
    ZoomOut
}

data class ElectronicAlbumDisplaySpec(
    val contentScale: ContentScale,
    val alignment: Alignment,
    val extraScale: Float = 1f
)

fun ElectronicAlbumDisplayMode.label(strings: NewPageStrings): String = when (this) {
    ElectronicAlbumDisplayMode.Smart -> strings.labDisplayModeSmart
    ElectronicAlbumDisplayMode.ScaleToFitCenter -> strings.labDisplayModeScaleToFitCenter
    ElectronicAlbumDisplayMode.CropToFitCenter -> strings.labDisplayModeCropToFitCenter
    ElectronicAlbumDisplayMode.ZoomIn -> strings.labDisplayModeZoomIn
    ElectronicAlbumDisplayMode.ZoomOut -> strings.labDisplayModeZoomOut
}

fun ElectronicAlbumDisplayMode.toPreferenceValue(): String = when (this) {
    ElectronicAlbumDisplayMode.Smart -> "smart"
    ElectronicAlbumDisplayMode.ScaleToFitCenter -> "scale_fit_center"
    ElectronicAlbumDisplayMode.CropToFitCenter -> "crop_fit_center"
    ElectronicAlbumDisplayMode.ZoomIn -> "zoom_in"
    ElectronicAlbumDisplayMode.ZoomOut -> "zoom_out"
}

fun electronicAlbumDisplayModeFromPreference(value: String): ElectronicAlbumDisplayMode = when (value) {
    "smart", "focus" -> ElectronicAlbumDisplayMode.Smart
    "crop_fit_center" -> ElectronicAlbumDisplayMode.CropToFitCenter
    "zoom", "zoom_in" -> ElectronicAlbumDisplayMode.ZoomIn
    "zoom_out" -> ElectronicAlbumDisplayMode.ZoomOut
    "scale_fit_center" -> ElectronicAlbumDisplayMode.ScaleToFitCenter
    else -> ElectronicAlbumDisplayMode.Smart
}

fun resolveElectronicAlbumDisplaySpec(
    mode: ElectronicAlbumDisplayMode,
    item: MediaItem,
    containerRatio: Float,
    animatedBias: Float,
    motionProgress: Float = 0f,
    smartFrame: ElectronicAlbumSmartFrame? = null
): ElectronicAlbumDisplaySpec {
    val imageRatio = if (item.width > 0 && item.height > 0) {
        item.width.toFloat() / item.height.toFloat()
    } else {
        containerRatio
    }
    val needsHorizontalPan = imageRatio > containerRatio * 1.03f
    val needsVerticalPan = imageRatio < containerRatio * 0.97f
    val clampedMotionProgress = motionProgress.coerceIn(0f, 1f)
    return when (mode) {
        ElectronicAlbumDisplayMode.Smart -> {
            if (smartFrame != null) {
                ElectronicAlbumDisplaySpec(
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(
                        horizontalBias = (smartFrame.horizontalBias + animatedBias * smartFrame.driftRangeX).coerceIn(-1f, 1f),
                        verticalBias = (smartFrame.verticalBias + animatedBias * smartFrame.driftRangeY).coerceIn(-1f, 1f)
                    ),
                    extraScale = smartFrame.extraScale
                )
            } else {
                val alignment = when {
                    needsHorizontalPan -> BiasAlignment(horizontalBias = animatedBias * 0.72f, verticalBias = 0f)
                    needsVerticalPan -> BiasAlignment(horizontalBias = 0f, verticalBias = animatedBias * 0.58f)
                    else -> Alignment.Center
                }
                ElectronicAlbumDisplaySpec(
                    contentScale = ContentScale.Crop,
                    alignment = alignment,
                    extraScale = if (needsHorizontalPan || needsVerticalPan) 1.06f else 1.02f
                )
            }
        }

        ElectronicAlbumDisplayMode.ScaleToFitCenter -> ElectronicAlbumDisplaySpec(
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        ElectronicAlbumDisplayMode.CropToFitCenter -> {
            val alignment = when {
                needsHorizontalPan -> BiasAlignment(horizontalBias = animatedBias * 0.94f, verticalBias = 0f)
                needsVerticalPan -> BiasAlignment(horizontalBias = 0f, verticalBias = animatedBias * 0.82f)
                else -> Alignment.Center
            }
            ElectronicAlbumDisplaySpec(
                contentScale = ContentScale.Crop,
                alignment = alignment
            )
        }

        ElectronicAlbumDisplayMode.ZoomIn,
        ElectronicAlbumDisplayMode.ZoomOut -> {
            val hasLargeCropMargin = needsHorizontalPan || needsVerticalPan
            val minScale = if (hasLargeCropMargin) 1.03f else 1.01f
            val maxScale = if (hasLargeCropMargin) 1.2f else 1.17f
            val extraScale = when (mode) {
                ElectronicAlbumDisplayMode.ZoomIn -> minScale + ((maxScale - minScale) * clampedMotionProgress)
                ElectronicAlbumDisplayMode.ZoomOut -> maxScale - ((maxScale - minScale) * clampedMotionProgress)
            }
            ElectronicAlbumDisplaySpec(
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                extraScale = extraScale
            )
        }
    }
}
