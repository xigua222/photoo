package com.voicelike.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

const val DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER = 1.7f
const val DEFAULT_UP_SWIPE_SPEED_MULTIPLIER = 1.3f
const val MIN_SWIPE_SPEED_MULTIPLIER = 0.5f
const val MAX_SWIPE_SPEED_MULTIPLIER = 2.0f

fun normalizeSwipeSpeedMultiplier(value: Float): Float {
    val clamped = value.coerceIn(MIN_SWIPE_SPEED_MULTIPLIER, MAX_SWIPE_SPEED_MULTIPLIER)
    return (clamped * 10f).roundToInt() / 10f
}

fun swipeSpeedMultiplierToTenths(value: Float): Int {
    return (normalizeSwipeSpeedMultiplier(value) * 10f).roundToInt()
}

fun swipeSpeedTenthsToMultiplier(
    value: Int,
    defaultValue: Float
): Float {
    return normalizeSwipeSpeedMultiplier(
        if (value in 5..20) value / 10f else defaultValue
    )
}

object SwipeMotionPreferences {
    var horizontalSpeedMultiplier by mutableFloatStateOf(DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER)
        private set

    var upSpeedMultiplier by mutableFloatStateOf(DEFAULT_UP_SWIPE_SPEED_MULTIPLIER)
        private set

    fun update(horizontal: Float, up: Float) {
        horizontalSpeedMultiplier = normalizeSwipeSpeedMultiplier(horizontal)
        upSpeedMultiplier = normalizeSwipeSpeedMultiplier(up)
    }

    fun restoreDefaults() {
        update(
            horizontal = DEFAULT_HORIZONTAL_SWIPE_SPEED_MULTIPLIER,
            up = DEFAULT_UP_SWIPE_SPEED_MULTIPLIER
        )
    }
}
