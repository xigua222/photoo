package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardTraceSubjectFrameFilterSupportTest {

    @Test
    fun `subject frame candidate keeps stable V code`() {
        val candidate = buildColorCardTraceSubjectFrameCandidate()

        assertEquals("#109V", candidate.title)
        assertEquals(ColorCardTraceFilterKind.SubjectFrame, candidate.kind)
    }

    @Test
    fun `subject rect normalizes around one third area from faces`() {
        val rect = resolveColorCardTraceSubjectRect(
            sourceSessionId = 42L,
            sourceAspect = 1.2f,
            faceRegions = listOf(
                ColorCardTraceFaceRegion(
                    centerX = 0.52f,
                    centerY = 0.44f,
                    radiusX = 0.10f,
                    radiusY = 0.12f
                )
            )
        )

        val area = rect.width * rect.height
        assertTrue(area in 0.24f..0.40f)
        assertTrue(rect.left >= 0f && rect.top >= 0f)
        assertTrue(rect.right <= 1f && rect.bottom <= 1f)
    }
}
