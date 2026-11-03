package com.voicelike.app.motionphoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicPhotoCompatWriterGoogleTest {

    @Test
    fun `google motion photo xmp preserves existing metadata while refreshing motion fields`() {
        val mergedXmp = DynamicPhotoCompatWriter.buildGoogleMotionPhotoXmp(
            offsetFromEnd = 456L,
            videoLength = 456L,
            primaryPadding = 12L,
            presentationTimestampUs = 789L,
            baseXmp = """
                <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description
                      rdf:about=""
                      xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                      xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                      xmp:CreatorTool="Samsung Camera"
                      GCamera:MotionPhoto="0"
                      GCamera:MicroVideoOffset="99">
                      <xmp:Label>favorite</xmp:Label>
                      <Container:Directory>
                        <rdf:Seq />
                      </Container:Directory>
                    </rdf:Description>
                  </rdf:RDF>
                </x:xmpmeta>
            """.trimIndent()
        )

        assertTrue(mergedXmp.contains("""xmp:CreatorTool="Samsung Camera""""))
        assertTrue(mergedXmp.contains("<xmp:Label>favorite</xmp:Label>"))
        assertTrue(mergedXmp.contains("""GCamera:MotionPhoto="1""""))
        assertTrue(mergedXmp.contains("""GCamera:MotionPhotoPresentationTimestampUs="789""""))
        assertTrue(mergedXmp.contains("""GCamera:MicroVideoOffset="456""""))
        assertTrue(mergedXmp.contains("""Item:Padding="12""""))
        assertEquals(1, "<Container:Directory>".toRegex().findAll(mergedXmp).count())
        assertEquals(1, "<GContainer:Directory>".toRegex().findAll(mergedXmp).count())
    }
}
