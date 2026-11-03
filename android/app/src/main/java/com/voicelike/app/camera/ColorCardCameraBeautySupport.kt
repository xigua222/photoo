package com.voicelike.app

private const val COLOR_CARD_CAMERA_BEAUTY_MIN_STABLE_FRAMES = 4
private const val COLOR_CARD_CAMERA_BEAUTY_MIN_CONTOUR_POINTS = 80
private const val COLOR_CARD_CAMERA_BEAUTY_MIN_BOUNDS_AREA = 0.031f
private const val COLOR_CARD_CAMERA_BEAUTY_MAX_BOUNDS_AREA = 0.58f
private const val COLOR_CARD_CAMERA_BEAUTY_MIN_REGION_AREA = 0.0105f
private const val COLOR_CARD_CAMERA_BEAUTY_MAX_REGION_AREA = 0.22f
private const val COLOR_CARD_CAMERA_BEAUTY_MIN_FACE_ASPECT = 0.52f
private const val COLOR_CARD_CAMERA_BEAUTY_MAX_FACE_ASPECT = 1.34f
private const val COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_X = 0.04f
private const val COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_Y = 0.05f

internal data class ColorCardCameraBeautyAvailability(
    val ready: Boolean,
    val reason: String
)

internal fun isColorCardCameraBeautyReady(
    trackedFacesFrame: ColorCardCameraTrackedFacesFrame
): Boolean {
    return resolveColorCardCameraBeautyAvailability(trackedFacesFrame).ready
}

internal fun resolveColorCardCameraBeautyAvailability(
    trackedFacesFrame: ColorCardCameraTrackedFacesFrame
): ColorCardCameraBeautyAvailability {
    if (trackedFacesFrame.isHeld) {
        return ColorCardCameraBeautyAvailability(
            ready = false,
            reason = "held_face"
        )
    }
    if (trackedFacesFrame.faces.isEmpty()) {
        return ColorCardCameraBeautyAvailability(
            ready = false,
            reason = "no_face"
        )
    }
    if (trackedFacesFrame.faces.size > 1) {
        return ColorCardCameraBeautyAvailability(
            ready = false,
            reason = "multi_face"
        )
    }
    if (trackedFacesFrame.stableFrameCount < COLOR_CARD_CAMERA_BEAUTY_MIN_STABLE_FRAMES) {
        return ColorCardCameraBeautyAvailability(
            ready = false,
            reason = "unstable_${trackedFacesFrame.stableFrameCount}"
        )
    }
    val face = trackedFacesFrame.faces.single()
    return if (face.qualifiesForBeauty()) {
        ColorCardCameraBeautyAvailability(
            ready = true,
            reason = "ready"
        )
    } else {
        ColorCardCameraBeautyAvailability(
            ready = false,
            reason = "face_rejected"
        )
    }
}

internal fun filterColorCardCameraBeautyFaceRegions(
    faceRegions: List<ColorCardTraceFaceRegion>
): List<ColorCardTraceFaceRegion> {
    val face = faceRegions.singleOrNull() ?: return emptyList()
    return if (face.qualifiesForBeauty()) {
        listOf(face)
    } else {
        emptyList()
    }
}

private fun ColorCardCameraTrackedFace.qualifiesForBeauty(): Boolean {
    if (landmarks.size < COLOR_CARD_CAMERA_BEAUTY_MIN_CONTOUR_POINTS) return false
    if (boundsArea !in COLOR_CARD_CAMERA_BEAUTY_MIN_BOUNDS_AREA..COLOR_CARD_CAMERA_BEAUTY_MAX_BOUNDS_AREA) {
        return false
    }
    if (aspectRatio !in COLOR_CARD_CAMERA_BEAUTY_MIN_FACE_ASPECT..COLOR_CARD_CAMERA_BEAUTY_MAX_FACE_ASPECT) {
        return false
    }
    return bounds.left >= COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_X &&
        bounds.top >= COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_Y &&
        bounds.right <= 1f - COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_X &&
        bounds.bottom <= 1f - COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_Y
}

private fun ColorCardTraceFaceRegion.qualifiesForBeauty(): Boolean {
    val regionArea = (radiusX * radiusY).coerceAtLeast(0f)
    if (regionArea !in COLOR_CARD_CAMERA_BEAUTY_MIN_REGION_AREA..COLOR_CARD_CAMERA_BEAUTY_MAX_REGION_AREA) {
        return false
    }
    val aspectRatio = radiusX / radiusY.coerceAtLeast(0.0001f)
    if (aspectRatio !in COLOR_CARD_CAMERA_BEAUTY_MIN_FACE_ASPECT..COLOR_CARD_CAMERA_BEAUTY_MAX_FACE_ASPECT) {
        return false
    }
    return (centerX - radiusX) >= COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_X &&
        (centerY - radiusY) >= COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_Y &&
        (centerX + radiusX) <= 1f - COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_X &&
        (centerY + radiusY) <= 1f - COLOR_CARD_CAMERA_BEAUTY_EDGE_MARGIN_Y
}
