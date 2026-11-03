package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardTraceSkinProtectionTest {

    @Test
    fun skinToneMaskPrefersLikelySkinColors() {
        val lightSkin = colorCardTraceSkinToneMask(red = 0xD9, green = 0xA0, blue = 0x7A)
        val darkSkin = colorCardTraceSkinToneMask(red = 0x6A, green = 0x43, blue = 0x2D)
        val skyBlue = colorCardTraceSkinToneMask(red = 0x4A, green = 0x90, blue = 0xE2)
        val leafGreen = colorCardTraceSkinToneMask(red = 0x3E, green = 0x8E, blue = 0x41)
        val neutralGray = colorCardTraceSkinToneMask(red = 0x88, green = 0x88, blue = 0x88)

        // The mask was retuned to be permissive: neutral/desaturated tones keep
        // partial protection so shadowed skin does not halo. Assert the relative
        // ordering instead of tight absolute thresholds.
        assertTrue(lightSkin > 0.8f)
        assertTrue(darkSkin > 0.8f)
        assertTrue(skyBlue < 0.3f)
        assertTrue(leafGreen < lightSkin)
        assertTrue(neutralGray < lightSkin)
    }

    @Test
    fun faceProtectionMaskFallsOffOutsideFaceEllipse() {
        val region = ColorCardTraceFaceRegion(
            centerX = 0.5f,
            centerY = 0.5f,
            radiusX = 0.2f,
            radiusY = 0.25f
        )

        val centerMask = colorCardTraceFaceProtectionMask(
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            faceRegions = listOf(region)
        )
        val edgeMask = colorCardTraceFaceProtectionMask(
            normalizedX = 0.66f,
            normalizedY = 0.5f,
            faceRegions = listOf(region)
        )
        val outsideMask = colorCardTraceFaceProtectionMask(
            normalizedX = 0.9f,
            normalizedY = 0.5f,
            faceRegions = listOf(region)
        )

        assertEquals(1f, centerMask, 0.0001f)
        assertTrue(edgeMask in 0f..1f)
        assertTrue(outsideMask < 0.01f)
    }

    @Test
    fun protectionAmountRequiresFaceAndSkinSignals() {
        val region = ColorCardTraceFaceRegion(
            centerX = 0.5f,
            centerY = 0.5f,
            radiusX = 0.2f,
            radiusY = 0.25f
        )

        val protectedSkin = colorCardTraceSkinProtectionAmount(
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            red = 0xB9,
            green = 0x79,
            blue = 0x58,
            faceRegions = listOf(region)
        )
        val outsideFace = colorCardTraceSkinProtectionAmount(
            normalizedX = 0.9f,
            normalizedY = 0.5f,
            red = 0xB9,
            green = 0x79,
            blue = 0x58,
            faceRegions = listOf(region)
        )
        val nonSkinInsideFace = colorCardTraceSkinProtectionAmount(
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            red = 0x4A,
            green = 0x90,
            blue = 0xE2,
            faceRegions = listOf(region)
        )

        assertTrue(protectedSkin > 0.5f)
        assertTrue(outsideFace < 0.01f)
        assertTrue(nonSkinInsideFace < 0.3f)
        assertTrue(nonSkinInsideFace < protectedSkin)
    }

    @Test
    fun contourInformedProtectionRegionStaysCloserToFaceCore() {
        val fallbackBounds = ColorCardTraceNormalizedBounds(
            left = 0.24f,
            top = 0.18f,
            right = 0.72f,
            bottom = 0.86f
        )
        val contourBounds = ColorCardTraceNormalizedBounds(
            left = 0.31f,
            top = 0.26f,
            right = 0.63f,
            bottom = 0.72f
        )

        val region = resolveColorCardTraceProtectionRegion(
            fallbackBounds = fallbackBounds,
            contourBounds = contourBounds
        )

        assertTrue(region.centerX in 0.45f..0.55f)
        assertTrue(region.centerY in 0.42f..0.58f)
        assertTrue(region.radiusX < 0.24f)
        assertTrue(region.radiusY < 0.36f)
    }
}
