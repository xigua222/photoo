package com.voicelike.app

internal const val UP_SWIPE_THRESHOLD_PX = 160f
internal const val HORIZONTAL_SWIPE_THRESHOLD_PX = 160f

enum class SwipeIntent {
    None,
    Left,
    Right,
    Up
}

internal data class SwipeIntentResult(
    val intent: SwipeIntent,
    val strength: Float
)

internal fun swipeIntentFromDirection(direction: SwipeDirection): SwipeIntent {
    return when (direction) {
        SwipeDirection.Left -> SwipeIntent.Left
        SwipeDirection.Right -> SwipeIntent.Right
        SwipeDirection.Up -> SwipeIntent.Up
        SwipeDirection.Down -> SwipeIntent.None
    }
}

internal fun resolveSwipeIntent(
    netX: Float,
    netY: Float,
    travelX: Float,
    travelY: Float,
    velocityX: Float = 0f,
    velocityY: Float = 0f,
    isFolderMode: Boolean = false,
    directionLock: SwipeIntent = SwipeIntent.None,
    verticalThresholdPx: Float = UP_SWIPE_THRESHOLD_PX,
    horizontalThresholdPx: Float = HORIZONTAL_SWIPE_THRESHOLD_PX,
    predictedDirection: SwipeIntent = SwipeIntent.None,
    predictionBias: Float = 0f,
    allowSwipeLeft: Boolean = true,
    allowSwipeRight: Boolean = true,
    allowSwipeUp: Boolean = true
): SwipeIntentResult {
    val dominantAxisRatio = 1.03f
    val velocityAxisRatio = 1.05f
    val minAxisConfidence = 0.40f
    val verticalFlingThreshold = 520f
    val horizontalFlingThreshold = 520f

    val absNetX = kotlin.math.abs(netX)
    val absNetY = kotlin.math.abs(netY)
    val pathX = travelX.coerceAtLeast(1f)
    val pathY = travelY.coerceAtLeast(1f)
    val effectivePathX = minOf(pathX, absNetX * 1.7f + horizontalThresholdPx * 0.35f)
    val effectivePathY = minOf(pathY, absNetY * 1.7f + verticalThresholdPx * 0.35f)
    val axisConfidenceX = (absNetX / effectivePathX.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val axisConfidenceY = (absNetY / effectivePathY.coerceAtLeast(1f)).coerceIn(0f, 1f)

    val upwardByDistance = netY < -verticalThresholdPx &&
        absNetY >= absNetX * dominantAxisRatio &&
        axisConfidenceY >= minAxisConfidence
    val upwardByVelocity = velocityY < -verticalFlingThreshold &&
        kotlin.math.abs(velocityY) >= kotlin.math.abs(velocityX) * velocityAxisRatio &&
        netY < -24f

    val leftByDistance = netX < -horizontalThresholdPx &&
        absNetX >= absNetY * dominantAxisRatio &&
        axisConfidenceX >= minAxisConfidence
    val leftByVelocity = velocityX < -horizontalFlingThreshold &&
        kotlin.math.abs(velocityX) >= kotlin.math.abs(velocityY) * velocityAxisRatio &&
        netX < -24f

    val rightByDistance = netX > horizontalThresholdPx &&
        absNetX >= absNetY * dominantAxisRatio &&
        axisConfidenceX >= minAxisConfidence
    val rightByVelocity = velocityX > horizontalFlingThreshold &&
        kotlin.math.abs(velocityX) >= kotlin.math.abs(velocityY) * velocityAxisRatio &&
        netX > 24f

    val upDistanceScore = ((-netY - verticalThresholdPx) / verticalThresholdPx).coerceIn(0f, 1f)
    val leftDistanceScore = ((-netX - horizontalThresholdPx) / horizontalThresholdPx).coerceIn(0f, 1f)
    val rightDistanceScore = ((netX - horizontalThresholdPx) / horizontalThresholdPx).coerceIn(0f, 1f)
    val upVelocityScore = ((-velocityY - verticalFlingThreshold) / verticalFlingThreshold).coerceIn(0f, 1f)
    val leftVelocityScore = ((-velocityX - horizontalFlingThreshold) / horizontalFlingThreshold).coerceIn(0f, 1f)
    val rightVelocityScore = ((velocityX - horizontalFlingThreshold) / horizontalFlingThreshold).coerceIn(0f, 1f)

    var upScore = if (allowSwipeUp && (upwardByDistance || upwardByVelocity)) {
        maxOf(upDistanceScore, upVelocityScore) * axisConfidenceY
    } else {
        -1f
    }
    var leftScore = if (allowSwipeLeft && !isFolderMode && (leftByDistance || leftByVelocity)) {
        maxOf(leftDistanceScore, leftVelocityScore) * axisConfidenceX
    } else {
        -1f
    }
    var rightScore = if (allowSwipeRight && !isFolderMode && (rightByDistance || rightByVelocity)) {
        maxOf(rightDistanceScore, rightVelocityScore) * axisConfidenceX
    } else {
        -1f
    }

    if (predictionBias > 0f) {
        when (predictedDirection) {
            SwipeIntent.Up -> if (upScore > 0f) upScore += predictionBias
            SwipeIntent.Left -> if (leftScore > 0f) leftScore += predictionBias
            SwipeIntent.Right -> if (rightScore > 0f) rightScore += predictionBias
            SwipeIntent.None -> Unit
        }
    }

    val candidates = buildList {
        if (upScore > 0f) add(SwipeIntent.Up to upScore)
        if (leftScore > 0f) add(SwipeIntent.Left to leftScore)
        if (rightScore > 0f) add(SwipeIntent.Right to rightScore)
    }

    val bestIntent = candidates.maxByOrNull { it.second }?.first ?: SwipeIntent.None
    val bestScore = candidates.maxOfOrNull { it.second } ?: 0f
    val lockScore = when (directionLock) {
        SwipeIntent.Up -> upScore
        SwipeIntent.Left -> leftScore
        SwipeIntent.Right -> rightScore
        SwipeIntent.None -> -1f
    }
    val switchBias = 0.10f
    val intent = if (
        directionLock != SwipeIntent.None &&
        lockScore > 0f &&
        bestIntent != SwipeIntent.None &&
        bestIntent != directionLock &&
        bestScore < lockScore + switchBias
    ) {
        directionLock
    } else {
        bestIntent
    }

    val strength = when (intent) {
        SwipeIntent.Up -> ((-netY) / verticalThresholdPx).coerceIn(0f, 1f)
        SwipeIntent.Left -> ((-netX) / horizontalThresholdPx).coerceIn(0f, 1f)
        SwipeIntent.Right -> ((netX) / horizontalThresholdPx).coerceIn(0f, 1f)
        SwipeIntent.None -> 0f
    }
    return SwipeIntentResult(intent = intent, strength = strength)
}

internal fun resolvePreviewSwipeIntent(
    netX: Float,
    netY: Float,
    travelX: Float,
    travelY: Float,
    isFolderMode: Boolean = false,
    directionLock: SwipeIntent = SwipeIntent.None,
    verticalThresholdPx: Float = UP_SWIPE_THRESHOLD_PX,
    horizontalThresholdPx: Float = HORIZONTAL_SWIPE_THRESHOLD_PX,
    predictedDirection: SwipeIntent = SwipeIntent.None,
    predictionBias: Float = 0f,
    allowSwipeLeft: Boolean = true,
    allowSwipeRight: Boolean = true,
    allowSwipeUp: Boolean = true
): SwipeIntentResult {
    val previewVerticalThresholdPx = (verticalThresholdPx * 0.38f).coerceAtLeast(28f)
    val previewHorizontalThresholdPx = (horizontalThresholdPx * 0.38f).coerceAtLeast(28f)
    return resolveSwipeIntent(
        netX = netX,
        netY = netY,
        travelX = travelX,
        travelY = travelY,
        velocityX = 0f,
        velocityY = 0f,
        isFolderMode = isFolderMode,
        directionLock = directionLock,
        verticalThresholdPx = previewVerticalThresholdPx,
        horizontalThresholdPx = previewHorizontalThresholdPx,
        predictedDirection = predictedDirection,
        predictionBias = predictionBias * 0.45f,
        allowSwipeLeft = allowSwipeLeft,
        allowSwipeRight = allowSwipeRight,
        allowSwipeUp = allowSwipeUp
    )
}

internal const val SWIPE_COMMIT_THRESHOLD_FRACTION = 0.22f
internal const val SWIPE_COMMIT_THRESHOLD_MIN_DP = 80f
internal const val SWIPE_COMMIT_THRESHOLD_MAX_DP = 130f
internal const val SWIPE_COMMIT_VERTICAL_RATIO = 0.88f
// iOS uses 36pt; Android drags start after the touch slop and drop the final up-event delta,
// so a quick flick reports ~8dp less travel than the finger moved.
private const val SWIPE_FLICK_MIN_DISTANCE_DP = 28f
private const val SWIPE_FLICK_MIN_VELOCITY_DP = 450f
private const val SWIPE_PULL_BACK_VETO_VELOCITY_DP = 150f
private const val SWIPE_INTENT_PREDICT_MIN_DP = 30f

/** Slow-drag commit distance: 22% of the card box short side, clamped to 80..130dp. */
internal fun resolveSwipeCommitThresholdDp(cardWidthDp: Float, cardHeightDp: Float): Float {
    return (minOf(cardWidthDp, cardHeightDp) * SWIPE_COMMIT_THRESHOLD_FRACTION)
        .coerceIn(SWIPE_COMMIT_THRESHOLD_MIN_DP, SWIPE_COMMIT_THRESHOLD_MAX_DP)
}

/**
 * Release decision (iOS CardStackEngine parity), all values in dp and dp/s:
 * pull-back veto first, then position commit, then a same-direction flick.
 * Up wins over horizontal when |dy| > |dx|. Down never commits here (folder drop handles it).
 */
internal fun resolveReleaseSwipeIntent(
    netXDp: Float,
    netYDp: Float,
    velocityXDp: Float,
    velocityYDp: Float,
    horizontalThresholdDp: Float,
    verticalThresholdDp: Float = horizontalThresholdDp * SWIPE_COMMIT_VERTICAL_RATIO,
    isFolderMode: Boolean = false,
    allowSwipeLeft: Boolean = true,
    allowSwipeRight: Boolean = true,
    allowSwipeUp: Boolean = true
): SwipeIntentResult {
    if (isFolderMode) return SwipeIntentResult(SwipeIntent.None, 0f)
    val absX = kotlin.math.abs(netXDp)
    val absY = kotlin.math.abs(netYDp)

    val signX = if (netXDp > 0f) 1f else -1f
    val pullBackH = netXDp != 0f && velocityXDp * signX < -SWIPE_PULL_BACK_VETO_VELOCITY_DP
    val flickH = absX > SWIPE_FLICK_MIN_DISTANCE_DP &&
        kotlin.math.abs(velocityXDp) > SWIPE_FLICK_MIN_VELOCITY_DP &&
        velocityXDp * netXDp > 0f
    val pastH = !pullBackH && (absX > horizontalThresholdDp || flickH)

    val pullBackUp = netYDp < 0f && velocityYDp > SWIPE_PULL_BACK_VETO_VELOCITY_DP
    val flickUp = netYDp < -SWIPE_FLICK_MIN_DISTANCE_DP && velocityYDp < -SWIPE_FLICK_MIN_VELOCITY_DP
    val pastUp = !pullBackUp && (netYDp < -verticalThresholdDp || flickUp)

    if (!pastH && !pastUp) return SwipeIntentResult(SwipeIntent.None, 0f)

    val horizontalIntent = if (netXDp > 0f) SwipeIntent.Right else SwipeIntent.Left
    val horizontalAllowed = if (horizontalIntent == SwipeIntent.Right) allowSwipeRight else allowSwipeLeft
    val preferred = if (pastUp && absY > absX) SwipeIntent.Up else horizontalIntent
    val intent = when {
        preferred == SwipeIntent.Up && allowSwipeUp -> SwipeIntent.Up
        preferred == SwipeIntent.Up && pastH && horizontalAllowed -> horizontalIntent
        preferred != SwipeIntent.Up && horizontalAllowed -> horizontalIntent
        preferred != SwipeIntent.Up && pastUp && allowSwipeUp -> SwipeIntent.Up
        else -> SwipeIntent.None
    }
    return SwipeIntentResult(intent = intent, strength = if (intent == SwipeIntent.None) 0f else 1f)
}

/** Which way the swipe intent tag points while dragging. */
internal enum class SwipeIntentBadgeDirection {
    None,
    Left,
    Right,
    Up,
    Down
}

internal fun predictSwipeIntentBadgeDirection(
    netXDp: Float,
    netYDp: Float,
    allowDown: Boolean = true
): SwipeIntentBadgeDirection {
    val absX = kotlin.math.abs(netXDp)
    val absY = kotlin.math.abs(netYDp)
    if (absY > absX * SWIPE_COMMIT_VERTICAL_RATIO && absY > SWIPE_INTENT_PREDICT_MIN_DP) {
        return if (netYDp < 0f) {
            SwipeIntentBadgeDirection.Up
        } else if (allowDown) {
            SwipeIntentBadgeDirection.Down
        } else {
            SwipeIntentBadgeDirection.None
        }
    }
    if (absX > SWIPE_INTENT_PREDICT_MIN_DP) {
        return if (netXDp > 0f) SwipeIntentBadgeDirection.Right else SwipeIntentBadgeDirection.Left
    }
    return SwipeIntentBadgeDirection.None
}
