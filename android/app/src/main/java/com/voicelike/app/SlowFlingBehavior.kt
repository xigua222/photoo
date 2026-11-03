package com.voicelike.app

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberVelocityLimitedFlingBehavior(
    velocityScale: Float = 0.5f
): FlingBehavior {
    val baseBehavior = ScrollableDefaults.flingBehavior()
    val safeScale = velocityScale.coerceIn(0.15f, 1f)
    return remember(baseBehavior, safeScale) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                return with(baseBehavior) {
                    performFling(initialVelocity * safeScale)
                }
            }
        }
    }
}
