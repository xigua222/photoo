package com.voicelike.app.motionphoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class SamsungMotionPhotoSupportTest {

    @Test
    fun `samsung jpeg payload emits sefh trailer markers`() {
        val videoBytes = byteArrayOf(1, 2, 3, 4, 5, 6)

        val payload = SamsungMotionPhotoSupport.buildJpegPayload(videoBytes)
        val text = String(payload.footerBytes, StandardCharsets.ISO_8859_1)

        assertTrue(text.contains("MotionPhoto_Data"))
        assertTrue(text.contains("MotionPhoto_Version"))
        assertTrue(text.contains("SEFH"))
        assertTrue(text.contains("SEFT"))
        assertTrue(text.contains("mpv3"))
        assertTrue(
            payload.footerBytes
                .copyOfRange(payload.primaryPaddingLength, payload.primaryPaddingLength + videoBytes.size)
                .contentEquals(videoBytes)
        )
    }

    @Test
    fun `samsung payload reports non zero primary padding and motion length`() {
        val payload = SamsungMotionPhotoSupport.buildJpegPayload(byteArrayOf(9, 8, 7, 6))

        assertTrue(payload.primaryPaddingLength > 0)
        assertTrue(payload.motionPayloadLength > 4)
        assertEquals(payload.footerBytes.size - payload.primaryPaddingLength, payload.motionPayloadLength)
    }

    @Test
    fun `samsung payload can preserve source version marker`() {
        val videoBytes = byteArrayOf(3, 1, 4, 1, 5, 9)

        val payload = SamsungMotionPhotoSupport.buildJpegPayload(
            videoBytes = videoBytes,
            motionPhotoVersion = "mpv2___.___."
        )
        val text = String(payload.footerBytes, StandardCharsets.ISO_8859_1)

        assertTrue(text.contains("mpv2___.___."))
        assertTrue(
            payload.footerBytes
                .copyOfRange(payload.primaryPaddingLength, payload.primaryPaddingLength + videoBytes.size)
                .contentEquals(videoBytes)
        )
    }
}
