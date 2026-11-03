package com.voicelike.app.keep

import org.junit.Assert.assertEquals
import org.junit.Test

class KeepGalleryDataSourceTest {

    @Test
    fun `consumed batch count stops before first extra resolved item`() {
        assertEquals(
            2,
            keepGalleryConsumedBatchCount(
                resolvedMask = listOf(true, true, true, true),
                remainingSlots = 2
            )
        )
    }

    @Test
    fun `consumed batch count can advance through stale ids after page is full`() {
        assertEquals(
            3,
            keepGalleryConsumedBatchCount(
                resolvedMask = listOf(true, false, false, true, true),
                remainingSlots = 1
            )
        )
    }

    @Test
    fun `consumed batch count drains pure stale tail even without remaining slots`() {
        assertEquals(
            3,
            keepGalleryConsumedBatchCount(
                resolvedMask = listOf(false, false, false),
                remainingSlots = 0
            )
        )
    }
}
