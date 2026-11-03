package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorCardTraceBlackMistFilterSupportTest {

    @Test
    fun `black mist candidate keeps stable V code`() {
        val candidate = buildColorCardTraceBlackMistCandidate()

        assertEquals("#110V", candidate.title)
        assertEquals(ColorCardTraceFilterKind.BlackMist, candidate.kind)
    }
}
