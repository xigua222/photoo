package com.voicelike.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val DRAG_VELOCITY_SAMPLE_WINDOW_MS = 100L
private const val DRAG_VELOCITY_RETENTION_MS = 150L
private const val MAX_DRAG_VELOCITY_SAMPLES = 8
private const val PREVIEW_RETAIN_THRESHOLD = 0.05f
private const val PREVIEW_REVEAL_THRESHOLD_STICKY = 0.04f
private const val PREVIEW_REVEAL_THRESHOLD_FRESH = 0.08f
private const val GLOW_SUSTAIN_STRENGTH_THRESHOLD = 0.035f
private const val GLOW_DIRECT_REVEAL_STRENGTH_THRESHOLD = 0.11f
private const val GLOW_MIN_DISPLAY_STRENGTH = 0.08f
private const val GLOW_SHOW_VELOCITY_THRESHOLD = 560f
private const val GLOW_HIDE_VELOCITY_THRESHOLD = 760f
private const val GLOW_ARM_DURATION_MS = 44L
private const val GLOW_HIDE_HOLD_MS = 150L

@Composable
fun rememberDraggableCardGestureState(itemId: Long): DraggableCardGestureState {
    return remember(itemId) { DraggableCardGestureState() }
}

@Stable
class DraggableCardGestureState {
    var dragPauseToken by mutableLongStateOf(0L)
        private set
    var dragNetX by mutableFloatStateOf(0f)
        private set
    var dragNetY by mutableFloatStateOf(0f)
        private set
    var dragTravelX by mutableFloatStateOf(0f)
        private set
    var dragTravelY by mutableFloatStateOf(0f)
        private set
    var swipeDirectionLock by mutableStateOf(SwipeIntent.None)
        private set
    var swipeIntentPreview by mutableStateOf(SwipeIntent.None)
        private set
    var swipeIntentStrength by mutableFloatStateOf(0f)
        private set
    var glowPreviewVisible by mutableStateOf(false)
        private set
    var glowLowVelocitySinceMs by mutableLongStateOf(0L)
        private set
    var glowHidePendingSinceMs by mutableLongStateOf(0L)
        private set
    var glowTrackingIntent by mutableStateOf(SwipeIntent.None)
        private set
    var glowSmoothedVelocity by mutableFloatStateOf(0f)
        private set
    var pointerInRoot by mutableStateOf<Offset?>(null)
        private set

    private val dragVelocitySamples = ArrayDeque<Pair<Long, Offset>>()
    private var lastEmittedPreviewIntent = SwipeIntent.None
    private var lastEmittedPreviewStrength = 0f
    private var lastEmittedPreviewGlow = false

    fun beginDrag(onSwipeIntentPreview: ((SwipeIntent, Float, Boolean) -> Unit)?) {
        dragPauseToken += 1L
        dragNetX = 0f
        dragNetY = 0f
        dragTravelX = 0f
        dragTravelY = 0f
        dragVelocitySamples.clear()
        resetSwipeFeedback(onSwipeIntentPreview)
    }

    fun clearPointer() {
        pointerInRoot = null
    }

    fun setPointerFromTouch(
        cardPositionInRoot: Offset,
        touchOffset: Offset,
        onDragPointerUpdate: ((Float, Float) -> Unit)?
    ) {
        pointerInRoot = Offset(
            x = cardPositionInRoot.x + touchOffset.x,
            y = cardPositionInRoot.y + touchOffset.y
        )
        notifyPointerUpdate(onDragPointerUpdate)
    }

    fun applyDragDelta(
        dragAmount: Offset,
        currentOffsetX: Float,
        currentOffsetY: Float,
        resolvedOffsetY: Float,
        disableDownSwipeGestures: Boolean,
        dragGainXStart: Float,
        dragGainXRampDistancePx: Float,
        dragGainY: Float
    ): DraggableCardDragResult {
        val dampingFactor = if (disableDownSwipeGestures && dragAmount.y > 0f && resolvedOffsetY > 0f) {
            0.62f
        } else {
            1f
        }
        val isNearFolderDropMode =
            !disableDownSwipeGestures &&
                (resolvedOffsetY > (DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX * 0.42f) ||
                    dragNetY > (DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX * 0.42f))
        val horizontalDragGain = if (isNearFolderDropMode) {
            1f
        } else {
            val rampT = (abs(dragNetX) / dragGainXRampDistancePx).coerceIn(0f, 1f)
            dragGainXStart + ((1f - dragGainXStart) * rampT)
        }
        val effectiveDragGainY = if (isNearFolderDropMode && dragAmount.y > 0f) 1f else dragGainY
        val adjustedDragX = dragAmount.x * horizontalDragGain * dampingFactor
        val adjustedDragY = dragAmount.y * effectiveDragGainY * dampingFactor

        dragNetX += adjustedDragX
        dragNetY += adjustedDragY
        dragTravelX += abs(adjustedDragX)
        dragTravelY += abs(adjustedDragY)

        return DraggableCardDragResult(
            offsetX = currentOffsetX + adjustedDragX,
            offsetY = currentOffsetY + adjustedDragY
        )
    }

    fun updatePreviewDuringDrag(
        uptimeMillis: Long,
        position: Offset,
        dragAmount: Offset,
        cardPositionInRoot: Offset,
        currentOffsetY: Float,
        disableDownSwipeGestures: Boolean,
        adaptiveVerticalSwipeThresholdPx: Float,
        adaptiveHorizontalSwipeThresholdPx: Float,
        predictedSwipeIntent: SwipeIntent,
        predictionBias: Float,
        allowSwipeLeft: Boolean,
        allowSwipeRight: Boolean,
        allowSwipeUp: Boolean,
        onDragVelocityUpdate: ((Float) -> Unit)?,
        onSwipeIntentPreview: ((SwipeIntent, Float, Boolean) -> Unit)?,
        onDragPointerUpdate: ((Float, Float) -> Unit)?
    ) {
        dragVelocitySamples.addLast(uptimeMillis to position)
        while (dragVelocitySamples.size > MAX_DRAG_VELOCITY_SAMPLES) {
            dragVelocitySamples.removeFirst()
        }

        val velocitySample = computeInstantVelocitySample(uptimeMillis)
        glowSmoothedVelocity = if (velocitySample.hasMeasuredVelocity) {
            if (glowSmoothedVelocity <= 0f) {
                velocitySample.velocity
            } else {
                (glowSmoothedVelocity * 0.72f) + (velocitySample.velocity * 0.28f)
            }
        } else {
            0f
        }
        onDragVelocityUpdate?.invoke(glowSmoothedVelocity)

        val isFolderModeNow = !disableDownSwipeGestures &&
            currentOffsetY > DOWN_SWIPE_MOVE_MODE_THRESHOLD_PX
        val previewResult = resolvePreviewSwipeIntent(
            netX = dragNetX,
            netY = dragNetY,
            travelX = dragTravelX,
            travelY = dragTravelY,
            isFolderMode = isFolderModeNow,
            directionLock = SwipeIntent.None,
            verticalThresholdPx = adaptiveVerticalSwipeThresholdPx,
            horizontalThresholdPx = adaptiveHorizontalSwipeThresholdPx,
            predictedDirection = predictedSwipeIntent,
            predictionBias = predictionBias,
            allowSwipeLeft = allowSwipeLeft,
            allowSwipeRight = allowSwipeRight,
            allowSwipeUp = allowSwipeUp
        )
        val previewIntent = previewResult.intent
        val previewStrength = previewResult.strength
        val revealThreshold = if (previewIntent != SwipeIntent.None && previewIntent == swipeIntentPreview) {
            PREVIEW_REVEAL_THRESHOLD_STICKY
        } else {
            PREVIEW_REVEAL_THRESHOLD_FRESH
        }

        swipeDirectionLock = if (previewIntent != SwipeIntent.None && previewStrength >= PREVIEW_RETAIN_THRESHOLD) {
            previewIntent
        } else {
            SwipeIntent.None
        }

        val stickyGlowIntent = if (
            glowPreviewVisible &&
            glowTrackingIntent != SwipeIntent.None &&
            (previewIntent == glowTrackingIntent || previewIntent == SwipeIntent.None)
        ) {
            glowTrackingIntent
        } else {
            previewIntent
        }

        val badgeIntent = if (
            stickyGlowIntent != SwipeIntent.None &&
            (
                previewStrength >= revealThreshold ||
                    (glowPreviewVisible &&
                        stickyGlowIntent == glowTrackingIntent &&
                        previewStrength >= GLOW_SUSTAIN_STRENGTH_THRESHOLD)
                )
        ) {
            stickyGlowIntent
        } else {
            SwipeIntent.None
        }
        val badgeStrength = if (badgeIntent == SwipeIntent.None) {
            0f
        } else {
            max(
                previewStrength,
                if (glowPreviewVisible && badgeIntent == glowTrackingIntent) {
                    GLOW_SUSTAIN_STRENGTH_THRESHOLD
                } else {
                    0f
                }
            )
        }

        if (badgeIntent == SwipeIntent.None || !velocitySample.hasMeasuredVelocity) {
            glowLowVelocitySinceMs = 0L
            if (!glowPreviewVisible && badgeIntent == SwipeIntent.None) {
                glowTrackingIntent = SwipeIntent.None
            }
        } else {
            if (glowTrackingIntent != badgeIntent) {
                glowTrackingIntent = badgeIntent
                glowLowVelocitySinceMs = 0L
            }
            val armVelocityThreshold = if (glowPreviewVisible) {
                GLOW_HIDE_VELOCITY_THRESHOLD
            } else {
                GLOW_SHOW_VELOCITY_THRESHOLD
            }
            if (glowSmoothedVelocity <= armVelocityThreshold) {
                if (glowLowVelocitySinceMs == 0L) {
                    glowLowVelocitySinceMs = uptimeMillis
                }
            } else {
                glowLowVelocitySinceMs = 0L
            }
            glowHidePendingSinceMs = 0L
        }

        val allowGlow = if (badgeIntent == SwipeIntent.None) {
            if (glowPreviewVisible && glowHidePendingSinceMs == 0L) {
                glowHidePendingSinceMs = uptimeMillis
            }
            glowPreviewVisible &&
                glowHidePendingSinceMs != 0L &&
                (uptimeMillis - glowHidePendingSinceMs) < GLOW_HIDE_HOLD_MS
        } else if (!velocitySample.hasMeasuredVelocity) {
            val shouldShowDirectly =
                badgeStrength >= GLOW_DIRECT_REVEAL_STRENGTH_THRESHOLD ||
                    (glowPreviewVisible && badgeStrength >= GLOW_SUSTAIN_STRENGTH_THRESHOLD)
            if (shouldShowDirectly) {
                glowHidePendingSinceMs = 0L
            }
            shouldShowDirectly
        } else if (glowPreviewVisible) {
            val keepGlow =
                glowTrackingIntent == badgeIntent &&
                    glowSmoothedVelocity <= GLOW_HIDE_VELOCITY_THRESHOLD &&
                    badgeStrength >= GLOW_SUSTAIN_STRENGTH_THRESHOLD
            if (keepGlow) {
                glowHidePendingSinceMs = 0L
                true
            } else {
                if (glowHidePendingSinceMs == 0L) {
                    glowHidePendingSinceMs = uptimeMillis
                }
                (uptimeMillis - glowHidePendingSinceMs) < GLOW_HIDE_HOLD_MS
            }
        } else {
            val armed =
                glowTrackingIntent == badgeIntent &&
                    glowLowVelocitySinceMs != 0L &&
                    (uptimeMillis - glowLowVelocitySinceMs) >= GLOW_ARM_DURATION_MS
            if (armed) {
                glowHidePendingSinceMs = 0L
            }
            armed
        }

        if (!allowGlow) {
            glowTrackingIntent = SwipeIntent.None
            glowHidePendingSinceMs = 0L
        }

        val glowIntent = if (allowGlow) {
            when {
                badgeIntent != SwipeIntent.None -> badgeIntent
                glowTrackingIntent != SwipeIntent.None -> glowTrackingIntent
                else -> SwipeIntent.None
            }
        } else {
            SwipeIntent.None
        }
        val glowStrength = if (glowIntent == SwipeIntent.None) {
            0f
        } else {
            max(
                if (badgeIntent == glowIntent) {
                    badgeStrength
                } else {
                    swipeIntentStrength
                },
                GLOW_MIN_DISPLAY_STRENGTH
            )
        }

        glowPreviewVisible = allowGlow
        swipeIntentPreview = badgeIntent
        swipeIntentStrength = badgeStrength
        emitPreviewIfChanged(onSwipeIntentPreview, glowIntent, glowStrength, allowGlow)

        pointerInRoot = pointerInRoot?.let { current ->
            Offset(
                x = current.x + dragAmount.x,
                y = current.y + dragAmount.y
            )
        } ?: Offset(
            x = cardPositionInRoot.x + position.x,
            y = cardPositionInRoot.y + position.y
        )
        notifyPointerUpdate(onDragPointerUpdate)
    }

    fun resolveReleaseVelocity(
        trackerVelocityX: Float,
        trackerVelocityY: Float,
        nowMs: Long
    ): DraggableCardReleaseVelocity {
        while (dragVelocitySamples.isNotEmpty() && nowMs - dragVelocitySamples.first().first > DRAG_VELOCITY_RETENTION_MS) {
            dragVelocitySamples.removeFirst()
        }
        val smoothedVelocity = if (dragVelocitySamples.size >= 2) {
            val first = dragVelocitySamples.first()
            val last = dragVelocitySamples.last()
            val dtMs = (last.first - first.first).coerceAtLeast(1L)
            Offset(
                x = (last.second.x - first.second.x) * 1000f / dtMs.toFloat(),
                y = (last.second.y - first.second.y) * 1000f / dtMs.toFloat()
            )
        } else {
            Offset.Zero
        }
        val velocityX = if (smoothedVelocity == Offset.Zero) {
            trackerVelocityX
        } else {
            (smoothedVelocity.x * 0.78f) + (trackerVelocityX * 0.22f)
        }
        val velocityY = if (smoothedVelocity == Offset.Zero) {
            trackerVelocityY
        } else {
            (smoothedVelocity.y * 0.78f) + (trackerVelocityY * 0.22f)
        }
        return DraggableCardReleaseVelocity(
            x = velocityX,
            y = velocityY,
            speed = sqrt((velocityX * velocityX) + (velocityY * velocityY))
        )
    }

    fun applyResolvedIntent(
        intent: SwipeIntent,
        strength: Float,
        onSwipeIntentPreview: ((SwipeIntent, Float, Boolean) -> Unit)?
    ) {
        swipeDirectionLock = SwipeIntent.None
        glowPreviewVisible = false
        glowLowVelocitySinceMs = 0L
        glowHidePendingSinceMs = 0L
        glowTrackingIntent = SwipeIntent.None
        glowSmoothedVelocity = 0f
        swipeIntentPreview = intent
        swipeIntentStrength = strength
        emitPreviewIfChanged(onSwipeIntentPreview, intent, strength, false)
    }

    fun resetSwipeFeedback(onSwipeIntentPreview: ((SwipeIntent, Float, Boolean) -> Unit)?) {
        swipeIntentPreview = SwipeIntent.None
        swipeIntentStrength = 0f
        glowPreviewVisible = false
        glowLowVelocitySinceMs = 0L
        glowHidePendingSinceMs = 0L
        glowTrackingIntent = SwipeIntent.None
        glowSmoothedVelocity = 0f
        swipeDirectionLock = SwipeIntent.None
        emitPreviewIfChanged(onSwipeIntentPreview, SwipeIntent.None, 0f, false)
    }

    private fun computeInstantVelocitySample(nowMs: Long): DraggableVelocitySample {
        val recentList = dragVelocitySamples
            .reversed()
            .takeWhile { nowMs - it.first <= DRAG_VELOCITY_SAMPLE_WINDOW_MS }
            .toList()
        if (recentList.size < 2) {
            return DraggableVelocitySample(velocity = 0f, hasMeasuredVelocity = false)
        }
        val newest = recentList.first()
        val oldest = recentList.last()
        val dtMs = (newest.first - oldest.first).coerceAtLeast(1L)
        val dx = newest.second.x - oldest.second.x
        val dy = newest.second.y - oldest.second.y
        return DraggableVelocitySample(
            velocity = sqrt((dx * dx) + (dy * dy)) * 1000f / dtMs,
            hasMeasuredVelocity = true
        )
    }

    private fun notifyPointerUpdate(onDragPointerUpdate: ((Float, Float) -> Unit)?) {
        pointerInRoot?.let { onDragPointerUpdate?.invoke(it.x, it.y) }
    }

    private fun emitPreviewIfChanged(
        onSwipeIntentPreview: ((SwipeIntent, Float, Boolean) -> Unit)?,
        intent: SwipeIntent,
        strength: Float,
        allowGlow: Boolean
    ) {
        val bucketedStrength = if (allowGlow && intent != SwipeIntent.None) {
            ((strength / 0.01f).roundToInt() * 0.01f).coerceIn(0f, 1f)
        } else {
            0f
        }
        if (
            lastEmittedPreviewIntent == intent &&
            lastEmittedPreviewGlow == allowGlow &&
            abs(lastEmittedPreviewStrength - bucketedStrength) < 0.009f
        ) {
            return
        }
        lastEmittedPreviewIntent = intent
        lastEmittedPreviewStrength = bucketedStrength
        lastEmittedPreviewGlow = allowGlow
        onSwipeIntentPreview?.invoke(intent, strength, allowGlow)
    }
}

data class DraggableCardDragResult(
    val offsetX: Float,
    val offsetY: Float
)

data class DraggableCardReleaseVelocity(
    val x: Float,
    val y: Float,
    val speed: Float
)

private data class DraggableVelocitySample(
    val velocity: Float,
    val hasMeasuredVelocity: Boolean
)
