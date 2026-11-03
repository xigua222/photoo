package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardCameraBeautySupportTest {

    @Test
    fun `beauty availability is ready for stable eligible face`() {
        val frame = ColorCardCameraTrackedFacesFrame(
            timestampMs = 100L,
            sourceWidth = 960,
            sourceHeight = 1280,
            faces = listOf(buildEligibleTrackedFace()),
            stableFrameCount = 4
        )

        val availability = resolveColorCardCameraBeautyAvailability(frame)

        assertTrue(availability.ready)
        assertEquals("ready", availability.reason)
        assertTrue(isColorCardCameraBeautyReady(frame))
    }

    @Test
    fun `beauty availability explains why held frame is rejected`() {
        val frame = ColorCardCameraTrackedFacesFrame(
            timestampMs = 100L,
            sourceWidth = 960,
            sourceHeight = 1280,
            faces = listOf(buildEligibleTrackedFace()),
            isHeld = true,
            stableFrameCount = 6
        )

        val availability = resolveColorCardCameraBeautyAvailability(frame)

        assertFalse(availability.ready)
        assertEquals("held_face", availability.reason)
        assertFalse(isColorCardCameraBeautyReady(frame))
    }

    private fun buildEligibleTrackedFace(): ColorCardCameraTrackedFace {
        val landmarks = List(80) { index ->
            val offset = index / 1000f
            ColorCardCameraTrackedLandmark(
                x = 0.40f + offset,
                y = 0.35f + offset,
                z = 0f
            )
        }
        return ColorCardCameraTrackedFace(
            bounds = ColorCardCameraNormalizedRect(
                left = 0.30f,
                top = 0.20f,
                right = 0.50f,
                bottom = 0.50f
            ),
            landmarks = landmarks,
            trackingId = 1
        )
    }
}
