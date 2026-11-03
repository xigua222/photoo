package com.voicelike.app

import kotlin.math.abs
import kotlin.math.hypot

internal class ColorCardCameraFaceTrackingStabilizer(
    private val config: ColorCardCameraFaceTrackingConfig = ColorCardCameraFaceTrackingConfig()
) {
    private var lastStableFrame: ColorCardCameraTrackedFacesFrame = ColorCardCameraTrackedFacesFrame.Empty

    fun stabilize(frame: ColorCardCameraTrackedFacesFrame): ColorCardCameraTrackedFacesFrame {
        val incomingFace = frame.faces.firstOrNull()
        val previousFace = lastStableFrame.faces.firstOrNull()

        if (incomingFace == null) {
            val shouldHold =
                previousFace != null &&
                    lastStableFrame.timestampMs > 0L &&
                    frame.timestampMs - lastStableFrame.timestampMs <= config.holdLastFaceMs
            if (!shouldHold) {
                lastStableFrame = frame.copy(
                    faces = emptyList(),
                    isHeld = false,
                    stableFrameCount = 0
                )
                return lastStableFrame
            }
            return lastStableFrame.copy(timestampMs = frame.timestampMs, isHeld = true)
        }

        if (previousFace == null || frame.timestampMs <= lastStableFrame.timestampMs) {
            lastStableFrame = frame.copy(
                faces = listOf(incomingFace),
                isHeld = false,
                stableFrameCount = 1
            )
            return lastStableFrame
        }

        val deltaMs = (frame.timestampMs - lastStableFrame.timestampMs).coerceAtLeast(1L)
        val adaptiveAlpha = (config.smoothingAlpha + (deltaMs / 240f)).coerceIn(0.24f, 0.68f)
        val isContinuousFace = incomingFace.isContinuousWith(previousFace)
        val resolvedFace = if (isContinuousFace) {
            incomingFace.smoothWith(previousFace, adaptiveAlpha)
        } else {
            incomingFace
        }
        lastStableFrame = frame.copy(
            faces = listOf(resolvedFace),
            isHeld = false,
            stableFrameCount = if (isContinuousFace) {
                lastStableFrame.stableFrameCount + 1
            } else {
                1
            }
        )
        return lastStableFrame
    }

    fun reset(): ColorCardCameraTrackedFacesFrame {
        lastStableFrame = ColorCardCameraTrackedFacesFrame.Empty
        return lastStableFrame
    }
}

private fun ColorCardCameraTrackedFace.smoothWith(
    previous: ColorCardCameraTrackedFace,
    alpha: Float
): ColorCardCameraTrackedFace {
    return copy(
        bounds = bounds.smoothWith(previous.bounds, alpha),
        landmarks = landmarks.smoothWith(previous.landmarks, alpha),
        transformationMatrix = transformationMatrix.smoothWith(previous.transformationMatrix, alpha)
    )
}

private fun ColorCardCameraTrackedFace.isContinuousWith(
    previous: ColorCardCameraTrackedFace
): Boolean {
    if (
        trackingId != null &&
        previous.trackingId != null &&
        trackingId != previous.trackingId
    ) {
        return false
    }
    val centerDistance = hypot(
        bounds.centerX - previous.bounds.centerX,
        bounds.centerY - previous.bounds.centerY
    )
    val previousArea = previous.boundsArea.coerceAtLeast(0.0001f)
    val areaRatio = boundsArea.coerceAtLeast(0.0001f) / previousArea
    val aspectDelta = abs(aspectRatio - previous.aspectRatio)
    return centerDistance <= 0.18f &&
        areaRatio in 0.48f..2.1f &&
        aspectDelta <= 0.42f
}

private fun ColorCardCameraNormalizedRect.smoothWith(
    previous: ColorCardCameraNormalizedRect,
    alpha: Float
): ColorCardCameraNormalizedRect {
    return ColorCardCameraNormalizedRect(
        left = previous.left.lerpTo(left, alpha),
        top = previous.top.lerpTo(top, alpha),
        right = previous.right.lerpTo(right, alpha),
        bottom = previous.bottom.lerpTo(bottom, alpha)
    )
}

private fun List<ColorCardCameraTrackedLandmark>.smoothWith(
    previous: List<ColorCardCameraTrackedLandmark>,
    alpha: Float
): List<ColorCardCameraTrackedLandmark> {
    if (size != previous.size || isEmpty()) return this
    return indices.map { index ->
        val current = this[index]
        val earlier = previous[index]
        ColorCardCameraTrackedLandmark(
            x = earlier.x.lerpTo(current.x, alpha),
            y = earlier.y.lerpTo(current.y, alpha),
            z = earlier.z.lerpTo(current.z, alpha)
        )
    }
}

private fun FloatArray?.smoothWith(
    previous: FloatArray?,
    alpha: Float
): FloatArray? {
    if (this == null || previous == null || size != previous.size) return this?.copyOf()
    return FloatArray(size) { index ->
        previous[index].lerpTo(this[index], alpha)
    }
}

private fun Float.lerpTo(target: Float, alpha: Float): Float {
    return this + ((target - this) * alpha.coerceIn(0f, 1f))
}
