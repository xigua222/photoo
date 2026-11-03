package com.voicelike.app

import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCardTraceColorAxisSupportTest {

    @Test
    fun `resolved color axis follows warm target direction`() {
        val axis = resolveColorCardTraceColorAxis(
            sourceColors = listOf(
                0xFF787878.toInt(),
                0xFF9A9A9A.toInt(),
                0xFFC0C0C0.toInt()
            ),
            targetColors = listOf(
                0xFFE6A15B.toInt(),
                0xFFD6763D.toInt(),
                0xFF8D4A2E.toInt()
            )
        )

        assertTrue(axis.bWeight > 0.35f)
        assertEquals(1f, hypot(axis.aWeight.toDouble(), axis.bWeight.toDouble()).toFloat(), 0.0001f)
    }

    @Test
    fun `adjustment field sample shifts along color axis horizontally`() {
        val left = resolveColorCardTraceAdjustmentFieldSample(
            normalizedX = 0f,
            normalizedY = 0.5f,
            colorAxis = ColorCardTraceColorAxis(aWeight = 0f, bWeight = 1f)
        )
        val right = resolveColorCardTraceAdjustmentFieldSample(
            normalizedX = 1f,
            normalizedY = 0.5f,
            colorAxis = ColorCardTraceColorAxis(aWeight = 0f, bWeight = 1f)
        )

        val leftLab = colorCardTraceLabFromRgb(left.red, left.green, left.blue)
        val rightLab = colorCardTraceLabFromRgb(right.red, right.green, right.blue)

        assertTrue(rightLab.b > leftLab.b + 8.0)
        assertTrue(abs(rightLab.a - leftLab.a) < 12.0)
    }

    @Test
    fun `adjustment field sample shifts tone vertically`() {
        val top = resolveColorCardTraceAdjustmentFieldSample(
            normalizedX = 0.5f,
            normalizedY = 0f,
            colorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS
        )
        val bottom = resolveColorCardTraceAdjustmentFieldSample(
            normalizedX = 0.5f,
            normalizedY = 1f,
            colorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS
        )

        val topLab = colorCardTraceLabFromRgb(top.red, top.green, top.blue)
        val bottomLab = colorCardTraceLabFromRgb(bottom.red, bottom.green, bottom.blue)

        assertTrue(topLab.l > bottomLab.l + 12.0)
    }
}
