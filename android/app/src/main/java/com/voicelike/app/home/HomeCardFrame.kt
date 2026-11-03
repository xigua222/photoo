package com.voicelike.app

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor

internal data class HomeCardFrameSpec(
    val aspectRatio: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val biasY: Float,
    val maxWidth: Dp = 520.dp,
    val maxHeight: Dp = 860.dp
)

internal data class HomeCardFrameSize(
    val width: Dp,
    val height: Dp
)

internal const val HOME_CARD_BASE_ASPECT_RATIO = 0.60f
internal const val HOME_CARD_DEFAULT_BIAS_Y = -0.12f
internal const val HOME_CARD_QUICK_ORGANIZE_BIAS_Y = -0.29f
internal const val HOME_CARD_QUICK_ORGANIZE_STACK_ASPECT_RATIO = 0.62f
internal const val HOME_CARD_QUICK_ORGANIZE_STACK_BIAS_Y = -0.33f

internal fun rememberHomeStackCardFrameSpec(
    quickOrganizeMode: Boolean
): HomeCardFrameSpec {
    return HomeCardFrameSpec(
        aspectRatio = if (quickOrganizeMode) {
            HOME_CARD_QUICK_ORGANIZE_STACK_ASPECT_RATIO
        } else {
            HOME_CARD_BASE_ASPECT_RATIO
        },
        widthFraction = if (quickOrganizeMode) 0.75f else 0.8f,
        heightFraction = if (quickOrganizeMode) 0.84f else 0.92f,
        biasY = if (quickOrganizeMode) {
            HOME_CARD_QUICK_ORGANIZE_STACK_BIAS_Y
        } else {
            HOME_CARD_DEFAULT_BIAS_Y
        }
    )
}

internal fun rememberHomeCarouselCardFrameSpec(
    quickOrganizeMode: Boolean
): HomeCardFrameSpec {
    return HomeCardFrameSpec(
        aspectRatio = HOME_CARD_BASE_ASPECT_RATIO,
        widthFraction = 1f,
        heightFraction = if (quickOrganizeMode) 0.88f else 0.92f,
        biasY = if (quickOrganizeMode) HOME_CARD_QUICK_ORGANIZE_BIAS_Y else HOME_CARD_DEFAULT_BIAS_Y
    )
}

internal fun resolveHomeCarouselPageWidthFraction(quickOrganizeMode: Boolean): Float {
    return if (quickOrganizeMode) 0.75f else 0.78f
}

internal fun resolveHomeCardFrameSize(
    availableWidth: Dp,
    availableHeight: Dp,
    spec: HomeCardFrameSpec
): HomeCardFrameSize {
    val widthCap = spec.maxWidth
    val heightCap = (availableHeight * spec.heightFraction).coerceAtMost(spec.maxHeight)
    val baseCardWidth = (availableWidth * spec.widthFraction).coerceAtMost(widthCap)
    val baseCardHeight = if (spec.aspectRatio > 0f && spec.aspectRatio.isFinite()) {
        baseCardWidth / spec.aspectRatio
    } else {
        heightCap
    }
    return if (baseCardHeight > heightCap && spec.aspectRatio > 0f && spec.aspectRatio.isFinite()) {
        HomeCardFrameSize(
            width = heightCap * spec.aspectRatio,
            height = heightCap
        )
    } else {
        HomeCardFrameSize(
            width = baseCardWidth,
            height = baseCardHeight
        )
    }
}

// Photo-frame card geometry (iOS PhotoCardGeometry / CardSortingView.adaptiveCardSize parity).
internal val HOME_CARD_PHOTO_CORNER_RADIUS = 20.dp
/** Smallest grab area per axis for a thin photo, capped to the card box. */
internal val HOME_CARD_MIN_TOUCH_SIZE = 160.dp
internal const val HOME_CARD_STACK_SCALE_STEP = 0.04f
internal val HOME_CARD_STACK_OFFSET_STEP = 15.dp
internal const val HOME_CARD_STACK_MAX_EFFECTIVE_INDEX = 2
internal const val HOME_CARD_ADAPTIVE_MIN_ASPECT_RATIO = 0.5f
internal const val HOME_CARD_ADAPTIVE_MAX_ASPECT_RATIO = 0.75f
internal val HOME_CARD_ADAPTIVE_HORIZONTAL_INSET = 32.dp
internal val HOME_CARD_ADAPTIVE_STACK_ROOM = 10.dp

// Free area between the top action rail (48dp padding + 40dp row + room for the AI overlay)
// and the bottom controls (60dp padding + ~40dp buttons + gap); the navigation bar is added on top.
internal val HOME_CARD_AREA_DEFAULT_TOP_INSET = 116.dp
internal val HOME_CARD_AREA_DEFAULT_BOTTOM_INSET = 114.dp

internal val HOME_CARD_ENTRY_ROTATION_SEEDS = floatArrayOf(-16f, 12f, -10f, 18f, -14f, 9f)

internal fun resolveHomeAdaptiveCardBoxSize(
    areaWidth: Dp,
    areaHeight: Dp
): HomeCardFrameSize {
    var width = (areaWidth - HOME_CARD_ADAPTIVE_HORIZONTAL_INSET).coerceAtLeast(0.dp)
    var height = (areaHeight - HOME_CARD_ADAPTIVE_STACK_ROOM).coerceAtLeast(0.dp)
    if (width > 0.dp && height > 0.dp) {
        val aspect = width / height
        if (aspect > HOME_CARD_ADAPTIVE_MAX_ASPECT_RATIO) {
            width = height * HOME_CARD_ADAPTIVE_MAX_ASPECT_RATIO
        } else if (aspect < HOME_CARD_ADAPTIVE_MIN_ASPECT_RATIO) {
            height = width / HOME_CARD_ADAPTIVE_MIN_ASPECT_RATIO
        }
    }
    return HomeCardFrameSize(
        width = floor(width.value).dp,
        height = floor(height.value).dp
    )
}

/** Photo rect size: the photo fitted (aspect preserved) inside the card box. */
internal fun resolveHomeCardPhotoSize(
    boxWidth: Dp,
    boxHeight: Dp,
    aspectRatio: Float?
): HomeCardFrameSize {
    if (
        aspectRatio == null ||
        aspectRatio <= 0f ||
        !aspectRatio.isFinite() ||
        boxWidth <= 0.dp ||
        boxHeight <= 0.dp
    ) {
        return HomeCardFrameSize(width = boxWidth, height = boxHeight)
    }
    var width = boxWidth
    var height = boxWidth / aspectRatio
    if (height > boxHeight) {
        height = boxHeight
        width = boxHeight * aspectRatio
    }
    return HomeCardFrameSize(width = width, height = height)
}

/** How far the deepest back card peeks below the top card (stack translate minus scale pull-back). */
internal fun resolveHomeCardStackPeek(cardHeight: Dp): Dp {
    val eff = HOME_CARD_STACK_MAX_EFFECTIVE_INDEX.toFloat()
    val peek = (HOME_CARD_STACK_OFFSET_STEP * eff) -
        (cardHeight * (eff * HOME_CARD_STACK_SCALE_STEP / 2f))
    return peek.coerceAtLeast(0.dp)
}

/** Back-card photo must never be wider or taller than the top card's photo. */
internal fun resolveHomeCardStackScaleCap(
    topPhoto: HomeCardFrameSize,
    backPhoto: HomeCardFrameSize
): Float {
    if (backPhoto.width <= 0.dp || backPhoto.height <= 0.dp) return 1f
    return minOf(1f, topPhoto.width / backPhoto.width, topPhoto.height / backPhoto.height)
}

internal fun resolveMediaItemFallbackAspectRatio(item: MediaItem): Float? {
    if (item.width <= 0 || item.height <= 0) return null
    return item.width.toFloat() / item.height.toFloat()
}

/** Decoded aspect when known (respects EXIF rotation), else MediaStore dimensions. Reads snapshot state. */
internal fun resolveHomeCardPhotoAspectRatio(item: MediaItem): Float? {
    return HomeCardPhotoAspectCache.get(item.id) ?: resolveMediaItemFallbackAspectRatio(item)
}

/**
 * Small in-memory cache of decoded photo aspect ratios keyed by media id, so a card keeps
 * its photo rect when it is promoted or recomposed. Main-thread only.
 */
internal object HomeCardPhotoAspectCache {
    private const val MAX_ENTRIES = 384
    private val aspects = mutableStateMapOf<Long, Float>()
    private val insertionOrder = ArrayDeque<Long>()

    fun get(id: Long): Float? = aspects[id]

    fun put(id: Long, aspectRatio: Float) {
        if (aspectRatio <= 0f || !aspectRatio.isFinite()) return
        val previous = aspects[id]
        if (previous != null && abs(previous - aspectRatio) < 0.002f) return
        if (previous == null) {
            insertionOrder.addLast(id)
            while (insertionOrder.size > MAX_ENTRIES) {
                aspects.remove(insertionOrder.removeFirst())
            }
        }
        aspects[id] = aspectRatio
    }
}
