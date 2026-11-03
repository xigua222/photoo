package com.voicelike.app.motionphoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class DynamicPhotoCompatWriterOplusTest {

    @Test
    fun `oplus xmp emits strict compatibility markers`() {
        val xmp = DynamicPhotoCompatWriter.buildOplusMotionPhotoXmp(
            videoLength = 4719794L,
            gainMapLength = 0L,
            presentationTimestampUs = 1456000L,
            primaryPresentationTimestampUs = 0L,
            opCameraVideoLength = 4719604L,
            motionPhotoOwner = "weibo",
            motionPhotoEnable = true,
            hdrgmVersion = null
        )

        assertTrue(xmp.contains("""OpCamera:MotionPhotoPrimaryPresentationTimestampUs="0""""))
        assertTrue(xmp.contains("""OpCamera:MotionPhotoOwner="weibo""""))
        assertTrue(xmp.contains("""OpCamera:MotionPhotoEnable="True""""))
        assertTrue(xmp.contains("""OpCamera:VideoLength="4719604""""))
        assertTrue(xmp.contains("""Item:Semantic="MotionPhoto""""))
        assertTrue(xmp.contains("""Item:Padding="0""""))
    }

    @Test
    fun `oplus xmp preserves embedded original asset entries`() {
        val xmp = DynamicPhotoCompatWriter.buildOplusMotionPhotoXmp(
            videoLength = 7514542L,
            gainMapLength = 0L,
            presentationTimestampUs = 591000L,
            primaryPresentationTimestampUs = 2176265L,
            opCameraVideoLength = 7513743L,
            motionPhotoOwner = "oplus",
            motionPhotoEnable = true,
            hdrgmVersion = "1.0",
            preservedEmbeddedItems = listOf(
                OplusMotionPhotoEmbeddedItem(
                    mime = "image/jpeg",
                    semantic = "GainMap",
                    bytes = ByteArray(481320)
                ),
                OplusMotionPhotoEmbeddedItem(
                    mime = "image/jpeg",
                    semantic = "Original",
                    bytes = ByteArray(7409358)
                )
            )
        )

        assertTrue(xmp.contains("""hdrgm:Version="1.0""""))
        assertTrue(xmp.contains("""Item:Semantic="GainMap""""))
        assertTrue(xmp.contains("""Item:Length="481320""""))
        assertTrue(xmp.contains("""Item:Semantic="Original""""))
        assertTrue(xmp.contains("""Item:Length="7409358""""))
        assertTrue(xmp.contains("""Item:Semantic="MotionPhoto""""))
        assertTrue(xmp.contains("""Item:Length="7514542""""))
    }

    @Test
    fun `oplus quicktime shell defaults to qt brand with wide box`() {
        val ftyp = DynamicPhotoCompatWriter.buildOplusQuickTimeFtypBox()
        val wide = DynamicPhotoCompatWriter.buildOplusWideBox()

        assertEquals("ftyp", String(ftyp.copyOfRange(4, 8), StandardCharsets.ISO_8859_1))
        assertEquals("qt  ", String(ftyp.copyOfRange(8, 12), StandardCharsets.ISO_8859_1))
        assertEquals("wide", String(wide.copyOfRange(4, 8), StandardCharsets.ISO_8859_1))
    }
}
