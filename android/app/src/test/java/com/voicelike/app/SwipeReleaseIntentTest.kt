package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeReleaseIntentTest {
    private fun up(netY: Float, vY: Float) = resolveReleaseSwipeIntent(
        netXDp = 0f, netYDp = netY, velocityXDp = 0f, velocityYDp = vY,
        horizontalThresholdDp = 83f
    ).intent

    @Test
    fun quickShortFlickUpCommits() {
        assertEquals(SwipeIntent.Up, up(netY = -34f, vY = -949f))
    }

    @Test
    fun slowShortDragUpSnapsBack() {
        assertEquals(SwipeIntent.None, up(netY = -34f, vY = -120f))
    }

    @Test
    fun longSlowDragUpCommits() {
        assertEquals(SwipeIntent.Up, up(netY = -90f, vY = 0f))
    }

    @Test
    fun pullingBackDownVetoesTheSwipe() {
        assertEquals(SwipeIntent.None, up(netY = -120f, vY = 400f))
    }
}
