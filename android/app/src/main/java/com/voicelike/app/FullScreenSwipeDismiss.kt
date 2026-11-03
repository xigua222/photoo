package com.voicelike.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val FULL_SCREEN_SWIPE_DIRECTION_RATIO = 1.15f
private const val FULL_SCREEN_SWIPE_ACTIVATION_SLOP_MULTIPLIER = 1.35f

@Stable
class FullScreenSwipeDismissState internal constructor(
    private val scope: CoroutineScope,
    private val dismissDistancePx: Float,
    private val onDismiss: () -> Unit
) {
    private var settleJob by mutableStateOf<Job?>(null)
    private var gestureHandlingEnabled: Boolean = true
    var offsetY by mutableFloatStateOf(0f)
        private set

    fun setGestureHandlingEnabled(enabled: Boolean) {
        gestureHandlingEnabled = enabled
    }

    fun isGestureHandlingEnabled(): Boolean = gestureHandlingEnabled

    private fun cancelSettle() {
        settleJob?.cancel()
        settleJob = null
    }

    val progress: Float
        get() = (abs(offsetY) / dismissDistancePx).coerceIn(0f, 1f)

    val contentScale: Float
        get() = lerpSwipeFloat(1f, 0.9f, progress)

    fun snapBy(deltaY: Float) {
        cancelSettle()
        offsetY = (offsetY + deltaY).coerceAtLeast(0f)
    }

    fun settle() {
        val startOffset = offsetY
        val shouldDismiss = startOffset >= dismissDistancePx
        cancelSettle()
        settleJob = scope.launch {
            val animatable = Animatable(startOffset)
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = if (shouldDismiss) 150 else 180,
                    easing = FastOutSlowInEasing
                )
            ) {
                offsetY = value
            }
            if (shouldDismiss) {
                onDismiss()
            }
            settleJob = null
        }
    }

    fun reset() {
        if (offsetY <= 0.5f) {
            cancelSettle()
            offsetY = 0f
            return
        }
        cancelSettle()
        settleJob = scope.launch {
            val animatable = Animatable(offsetY)
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 140,
                    easing = FastOutSlowInEasing
                )
            ) {
                offsetY = value
            }
            settleJob = null
        }
    }

    fun snapToRest() {
        cancelSettle()
        offsetY = 0f
    }
}

@Composable
fun rememberFullScreenSwipeDismissState(
    onDismiss: () -> Unit
): FullScreenSwipeDismissState {
    val scope = rememberCoroutineScope()
    val dismissDistancePx = with(LocalDensity.current) { 120.dp.toPx() }
    val latestOnDismiss = rememberUpdatedState(onDismiss)
    return remember(scope, dismissDistancePx) {
        FullScreenSwipeDismissState(
            scope = scope,
            dismissDistancePx = dismissDistancePx,
            onDismiss = { latestOnDismiss.value() }
        )
    }
}

fun Modifier.fullScreenSwipeToDismiss(
    enabled: Boolean,
    state: FullScreenSwipeDismissState
): Modifier {
    state.setGestureHandlingEnabled(enabled)
    return pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!state.isGestureHandlingEnabled()) {
                waitForUpOrCancellation()
                return@awaitEachGesture
            }
            var total = Offset.Zero
            var dragStarted = false
            var cancelled = false
            var cancelledByMultitouch = false
            val activationSlop = viewConfiguration.touchSlop * FULL_SCREEN_SWIPE_ACTIVATION_SLOP_MULTIPLIER

            do {
                val event = awaitPointerEvent()
                if (!state.isGestureHandlingEnabled()) {
                    cancelled = true
                    break
                }
                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount > 1) {
                    cancelled = true
                    cancelledByMultitouch = true
                    break
                }
                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (change == null || !change.pressed) break

                val delta = change.positionChange()
                total += delta

                if (!dragStarted) {
                    val reachedDownwardDrag = total.y > activationSlop &&
                        total.y > abs(total.x) * FULL_SCREEN_SWIPE_DIRECTION_RATIO
                    val reachedHorizontalIntent = abs(total.x) > activationSlop &&
                        abs(total.x) > abs(total.y) * FULL_SCREEN_SWIPE_DIRECTION_RATIO
                    val reachedUpwardIntent = total.y < -activationSlop

                    when {
                        reachedHorizontalIntent || reachedUpwardIntent -> {
                            cancelled = true
                            break
                        }

                        reachedDownwardDrag -> {
                            dragStarted = true
                        }

                        else -> continue
                    }
                }

                if (delta != Offset.Zero) {
                    state.snapBy(delta.y)
                    change.consumeAllChanges()
                }
            } while (true)

            if (dragStarted && !cancelled) {
                state.settle()
            } else if (cancelled) {
                if (cancelledByMultitouch) {
                    state.snapToRest()
                } else {
                    state.reset()
                }
            }
        }
    }
}

private fun lerpSwipeFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + ((stop - start) * fraction)
}
