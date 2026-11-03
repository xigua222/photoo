package com.voicelike.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TOP_CARD_POINTER_BUCKET_PX = 16f
private const val TOP_CARD_BOUNDS_BUCKET_PX = 4f
private const val TOP_CARD_OFFSET_BUCKET_PX = 10f
private const val TOP_CARD_OFFSET_UPDATE_MIN_DISTANCE_PX = 16f
private const val TOP_CARD_SWIPE_STRENGTH_BUCKET = 0.04f
private const val TOP_CARD_FOLDER_DROP_EXIT_THRESHOLD_RATIO = 0.82f

@Composable
fun rememberHomeTopCardUiState(
    topCardId: Long?,
    lensyEnabled: Boolean
): HomeTopCardUiState {
    val state = remember { HomeTopCardUiState() }

    LaunchedEffect(topCardId) {
        state.resetForTopCard()
    }
    LaunchedEffect(lensyEnabled) {
        if (!lensyEnabled) {
            state.clearAiOverlay()
        }
    }

    return state
}

@Stable
class HomeTopCardUiState {
    var topCardOffset by mutableStateOf(Offset.Zero)
        private set
    var topCardPointerInRoot by mutableStateOf<Offset?>(null)
        private set
    var topCardBoundsInRoot by mutableStateOf<Rect?>(null)
        private set
    var topCardImageBoundsInRoot by mutableStateOf<Rect?>(null)
        private set
    var topCardSwipeIntent by mutableStateOf(SwipeIntent.None)
        private set
    var topCardSwipeStrength by mutableFloatStateOf(0f)
        private set
    var isDragging by mutableStateOf(false)
        private set
    var edgeGlowVisible by mutableStateOf(false)
        private set
    var pageAiOverlayText by mutableStateOf<String?>(null)
        private set
    var pageAiOverlayVisible by mutableStateOf(false)
        private set
    var isFolderDropModeActive by mutableStateOf(false)
        private set

    fun resetForTopCard() {
        topCardOffset = Offset.Zero
        topCardPointerInRoot = null
        topCardBoundsInRoot = null
        topCardImageBoundsInRoot = null
        topCardSwipeIntent = SwipeIntent.None
        topCardSwipeStrength = 0f
        isDragging = false
        edgeGlowVisible = false
        pageAiOverlayText = null
        pageAiOverlayVisible = false
        isFolderDropModeActive = false
    }

    fun clearAiOverlay() {
        pageAiOverlayText = null
        pageAiOverlayVisible = false
    }

    fun onDragStart(onStarted: () -> Unit) {
        onStarted()
        isDragging = true
        edgeGlowVisible = false
        isFolderDropModeActive = false
    }

    fun onDragCancel() {
        isDragging = false
        isFolderDropModeActive = false
        clearSwipePreview()
    }

    fun onDragComplete() {
        isDragging = false
        isFolderDropModeActive = false
        clearSwipePreview()
    }

    fun updateDragOffset(
        x: Float,
        y: Float,
        quickOrganizeMode: Boolean,
        downSwipeMoveThresholdPx: Float
    ) {
        val nextFolderDropMode = when {
            !isDragging || quickOrganizeMode -> false
            isFolderDropModeActive -> y > (downSwipeMoveThresholdPx * TOP_CARD_FOLDER_DROP_EXIT_THRESHOLD_RATIO)
            else -> y > downSwipeMoveThresholdPx
        }
        if (nextFolderDropMode) {
            val nextOffset = Offset(
                quantizeCoordinate(x, TOP_CARD_OFFSET_BUCKET_PX),
                quantizeCoordinate(y, TOP_CARD_OFFSET_BUCKET_PX)
            )
            val dx = nextOffset.x - topCardOffset.x
            val dy = nextOffset.y - topCardOffset.y
            if (
                topCardOffset == Offset.Zero ||
                (dx * dx) + (dy * dy) >=
                    (TOP_CARD_OFFSET_UPDATE_MIN_DISTANCE_PX * TOP_CARD_OFFSET_UPDATE_MIN_DISTANCE_PX)
            ) {
                topCardOffset = nextOffset
            }
        } else if (topCardOffset != Offset.Zero) {
            topCardOffset = Offset.Zero
        }
        if (isFolderDropModeActive != nextFolderDropMode) {
            isFolderDropModeActive = nextFolderDropMode
        }
    }

    fun updatePointerInRoot(x: Float, y: Float) {
        if (!isFolderDropModeActive) {
            if (topCardPointerInRoot != null) {
                topCardPointerInRoot = null
            }
            return
        }
        val nextPointer = Offset(
            quantizeCoordinate(x, TOP_CARD_POINTER_BUCKET_PX),
            quantizeCoordinate(y, TOP_CARD_POINTER_BUCKET_PX)
        )
        if (topCardPointerInRoot != nextPointer) {
            topCardPointerInRoot = nextPointer
        }
    }

    fun clearPointerInRoot() {
        topCardPointerInRoot = null
    }

    fun updateBounds(bounds: Rect) {
        // Keep tracking top-card bounds even outside folder-drop mode.
        // Shared fullscreen dismiss remapping depends on this being continuously available.
        val quantizedBounds = Rect(
            left = quantizeCoordinate(bounds.left, TOP_CARD_BOUNDS_BUCKET_PX),
            top = quantizeCoordinate(bounds.top, TOP_CARD_BOUNDS_BUCKET_PX),
            right = quantizeCoordinate(bounds.right, TOP_CARD_BOUNDS_BUCKET_PX),
            bottom = quantizeCoordinate(bounds.bottom, TOP_CARD_BOUNDS_BUCKET_PX)
        )
        if (topCardBoundsInRoot != quantizedBounds) {
            topCardBoundsInRoot = quantizedBounds
        }
    }

    fun updateImageBounds(bounds: Rect) {
        val quantizedBounds = Rect(
            left = quantizeCoordinate(bounds.left, TOP_CARD_BOUNDS_BUCKET_PX),
            top = quantizeCoordinate(bounds.top, TOP_CARD_BOUNDS_BUCKET_PX),
            right = quantizeCoordinate(bounds.right, TOP_CARD_BOUNDS_BUCKET_PX),
            bottom = quantizeCoordinate(bounds.bottom, TOP_CARD_BOUNDS_BUCKET_PX)
        )
        if (topCardImageBoundsInRoot != quantizedBounds) {
            topCardImageBoundsInRoot = quantizedBounds
        }
    }

    private fun quantizeCoordinate(value: Float, stepPx: Float): Float {
        if (stepPx <= 1f) return value
        return ((value / stepPx).roundToInt() * stepPx)
    }

    fun updateAiOverlay(
        text: String?,
        visible: Boolean,
        lensyEnabled: Boolean
    ) {
        if (!lensyEnabled) {
            clearAiOverlay()
            return
        }
        if (!text.isNullOrBlank() && pageAiOverlayText != text) {
            pageAiOverlayText = text
        }
        val nextVisible = visible && !pageAiOverlayText.isNullOrBlank()
        if (pageAiOverlayVisible != nextVisible) {
            pageAiOverlayVisible = nextVisible
        }
    }

    fun updateSwipePreview(
        intent: SwipeIntent,
        strength: Float,
        allowGlow: Boolean
    ) {
        val displayStrength = if (allowGlow && intent != SwipeIntent.None) {
            strength.coerceAtLeast(0.08f)
        } else {
            0f
        }
        val bucketedStrength =
            ((displayStrength / TOP_CARD_SWIPE_STRENGTH_BUCKET).roundToInt() * TOP_CARD_SWIPE_STRENGTH_BUCKET)
                .coerceIn(0f, 1f)
        if (topCardSwipeIntent != intent) {
            topCardSwipeIntent = intent
        }
        if (abs(topCardSwipeStrength - bucketedStrength) >= (TOP_CARD_SWIPE_STRENGTH_BUCKET * 0.9f)) {
            topCardSwipeStrength = bucketedStrength
        }
        val nextEdgeGlowVisible = allowGlow && intent != SwipeIntent.None
        if (edgeGlowVisible != nextEdgeGlowVisible) {
            edgeGlowVisible = nextEdgeGlowVisible
        }
    }

    private fun clearSwipePreview() {
        topCardPointerInRoot = null
        topCardSwipeIntent = SwipeIntent.None
        topCardSwipeStrength = 0f
        edgeGlowVisible = false
    }
}
