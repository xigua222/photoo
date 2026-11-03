package com.voicelike.app

internal data class ColorCardCameraNormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float
        get() = (right - left).coerceAtLeast(0f)

    val height: Float
        get() = (bottom - top).coerceAtLeast(0f)

    val area: Float
        get() = (width * height).coerceAtLeast(0f)

    val aspectRatio: Float
        get() = width / height.coerceAtLeast(0.0001f)

    val centerX: Float
        get() = ((left + right) * 0.5f).coerceIn(0f, 1f)

    val centerY: Float
        get() = ((top + bottom) * 0.5f).coerceIn(0f, 1f)

    companion object {
        val Empty = ColorCardCameraNormalizedRect(0f, 0f, 0f, 0f)
    }
}

internal data class ColorCardCameraTrackedLandmark(
    val x: Float,
    val y: Float,
    val z: Float
)

internal data class ColorCardCameraBlendShapeScore(
    val name: String,
    val score: Float
)

internal data class ColorCardCameraTrackedFace(
    val bounds: ColorCardCameraNormalizedRect,
    val landmarks: List<ColorCardCameraTrackedLandmark>,
    val trackingId: Int? = null,
    val blendShapes: List<ColorCardCameraBlendShapeScore> = emptyList(),
    val transformationMatrix: FloatArray? = null
) {
    val boundsArea: Float
        get() = bounds.area

    val aspectRatio: Float
        get() = bounds.aspectRatio

    fun toProtectionRegion(): ColorCardTraceFaceRegion {
        return resolveColorCardTraceProtectionRegion(
            fallbackBounds = bounds.toTraceBounds(),
            contourBounds = landmarks.toTraceBounds()
        )
    }
}

internal data class ColorCardCameraTrackedFacesFrame(
    val timestampMs: Long,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val faces: List<ColorCardCameraTrackedFace>,
    val isHeld: Boolean = false,
    val stableFrameCount: Int = 0
) {
    val protectionFaceRegions: List<ColorCardTraceFaceRegion>
        get() = faces.map(ColorCardCameraTrackedFace::toProtectionRegion)

    companion object {
        val Empty = ColorCardCameraTrackedFacesFrame(
            timestampMs = 0L,
            sourceWidth = 0,
            sourceHeight = 0,
            faces = emptyList(),
            stableFrameCount = 0
        )
    }
}

internal data class ColorCardCameraFaceTrackingConfig(
    val maxFaces: Int = 1,
    val analysisIntervalMs: Long = 83L,
    val holdLastFaceMs: Long = 420L,
    val smoothingAlpha: Float = 0.34f,
    val minFaceSize: Float = 0.08f,
    val preferAccurateDetector: Boolean = false,
    val mlKitTrackingEnabled: Boolean = false,
    val mirrorHorizontally: Boolean = false
)

private fun ColorCardCameraNormalizedRect.toTraceBounds(): ColorCardTraceNormalizedBounds {
    return ColorCardTraceNormalizedBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}

private fun List<ColorCardCameraTrackedLandmark>.toTraceBounds(): ColorCardTraceNormalizedBounds? {
    if (size < 24) return null
    var left = 1f
    var top = 1f
    var right = 0f
    var bottom = 0f
    forEach { landmark ->
        left = minOf(left, landmark.x)
        top = minOf(top, landmark.y)
        right = maxOf(right, landmark.x)
        bottom = maxOf(bottom, landmark.y)
    }
    return ColorCardTraceNormalizedBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    ).takeIf { it.area > 0.0001f }
}
