package com.voicelike.app

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrganizeSplitTest {
    @Test
    fun phonesNeverSplitEvenSideways() {
        assertNull(organizeSplit(411.dp, 915.dp, null, null, null))
        assertNull(organizeSplit(915.dp, 411.dp, null, null, null))
    }

    @Test
    fun largePortraitWindowsDoNotSplit() {
        assertNull(organizeSplit(700.dp, 840.dp, null, null, null))
    }

    @Test
    fun foldSetsThePanes() {
        val split = organizeSplit(840.dp, 700.dp, 420.dp, 420.dp, 700.dp)!!
        assertEquals(420.dp, split.leading)
        assertEquals(0.dp, split.gap)
        assertEquals(420.dp, split.trailing)
    }

    @Test
    fun withoutAFoldSplitAtTheCenterlineWithAGutter() {
        val split = organizeSplit(1280.dp, 800.dp, null, null, null)!!
        assertEquals(624.dp, split.leading)
        assertEquals(32.dp, split.gap)
        assertEquals(624.dp, split.trailing)
    }

    @Test
    fun aShortOrEdgeTouchingFeatureIsIgnored() {
        val short = organizeSplit(1280.dp, 800.dp, 600.dp, 610.dp, 100.dp)!!
        assertEquals(624.dp, short.leading)
        val atEdge = organizeSplit(1280.dp, 800.dp, 0.dp, 10.dp, 800.dp)!!
        assertEquals(624.dp, atEdge.leading)
    }
}
