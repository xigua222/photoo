package com.voicelike.app.motionphoto

internal enum class MotionPhotoRevealPattern {
    SinglePoint,
    DualPoint,
    TriplePoint
}

internal data class MotionPhotoRenderSpec(
    val durationMs: Long = MotionPhotoFrameRenderer.DEFAULT_EFFECT_DURATION_MS,
    val frameRate: Int = 24,
    val maxExportLongSide: Int = 1440,
    val jpegQuality: Int = 92,
    val videoBitrate: Int = 8_000_000,
    val revealPattern: MotionPhotoRevealPattern = MotionPhotoRevealPattern.SinglePoint,
    val revealSeed: Long = 0L
) {
    val frameCount: Int
        get() = ((durationMs * frameRate) / 1000L).toInt().coerceAtLeast(2)

    companion object {
        fun dynamicSave(
            revealPattern: MotionPhotoRevealPattern = MotionPhotoRevealPattern.SinglePoint,
            revealSeed: Long = 0L
        ): MotionPhotoRenderSpec {
            return MotionPhotoRenderSpec(
                durationMs = 2000L,
                frameRate = 20,
                maxExportLongSide = 1080,
                jpegQuality = 90,
                videoBitrate = 6_000_000,
                revealPattern = revealPattern,
                revealSeed = revealSeed
            )
        }
    }
}
