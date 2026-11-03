package com.voicelike.app

import android.content.Context
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import coil.request.ImageRequest
import coil.size.Precision
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ULTRA_LONG_IMAGE_RATIO_THRESHOLD = 2.8f
private const val ULTRA_LONG_IMAGE_ZOOM_DETAIL_FACTOR = 2.5f
private const val ULTRA_LONG_IMAGE_LONG_EDGE_CAP = 8192

fun MediaItem.isUltraLongImageForPreview(): Boolean {
    if (type == "video") return false
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val longEdge = max(safeWidth, safeHeight).toFloat()
    val shortEdge = min(safeWidth, safeHeight).toFloat()
    return shortEdge > 0f && (longEdge / shortEdge) >= ULTRA_LONG_IMAGE_RATIO_THRESHOLD
}

fun buildFullscreenPreviewImageRequest(
    context: Context,
    item: MediaItem,
    containerSize: IntSize
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(item.uri)
        .crossfade(false)

    if (!item.isUltraLongImageForPreview()) {
        return builder.build()
    }

    val targetSize = calculateUltraLongDecodeTargetSize(
        containerSize = containerSize,
        mediaWidth = item.width,
        mediaHeight = item.height
    ) ?: return builder.build()

    return builder
        .size(targetSize.width, targetSize.height)
        .precision(Precision.EXACT)
        .build()
}

private fun calculateUltraLongDecodeTargetSize(
    containerSize: IntSize,
    mediaWidth: Int,
    mediaHeight: Int
): IntSize? {
    if (containerSize.width <= 0 || containerSize.height <= 0) return null

    val fittedRect = calculateFittedContentRect(
        containerSize = containerSize,
        mediaWidth = mediaWidth,
        mediaHeight = mediaHeight
    )

    val sourceWidth = mediaWidth.coerceAtLeast(1)
    val sourceHeight = mediaHeight.coerceAtLeast(1)
    val requestedWidth = (fittedRect.width * ULTRA_LONG_IMAGE_ZOOM_DETAIL_FACTOR)
        .roundToInt()
        .coerceAtLeast(containerSize.width)
        .coerceAtMost(sourceWidth)
    val requestedHeight = (fittedRect.height * ULTRA_LONG_IMAGE_ZOOM_DETAIL_FACTOR)
        .roundToInt()
        .coerceAtLeast(containerSize.height)
        .coerceAtMost(sourceHeight)

    if (requestedWidth <= 0 || requestedHeight <= 0) return null

    val longEdge = max(requestedWidth, requestedHeight)
    if (longEdge <= ULTRA_LONG_IMAGE_LONG_EDGE_CAP) {
        return IntSize(requestedWidth, requestedHeight)
    }

    val downscale = ULTRA_LONG_IMAGE_LONG_EDGE_CAP.toFloat() / longEdge.toFloat()
    return IntSize(
        width = max(1, (requestedWidth * downscale).roundToInt()),
        height = max(1, (requestedHeight * downscale).roundToInt())
    )
}

fun calculateFittedContentRect(
    containerSize: IntSize,
    mediaWidth: Int,
    mediaHeight: Int
): Rect {
    val cw = containerSize.width.toFloat().coerceAtLeast(1f)
    val ch = containerSize.height.toFloat().coerceAtLeast(1f)
    val safeW = mediaWidth.coerceAtLeast(1).toFloat()
    val safeH = mediaHeight.coerceAtLeast(1).toFloat()
    val mediaAspect = safeW / safeH
    val containerAspect = cw / ch
    val drawW: Float
    val drawH: Float
    if (mediaAspect > containerAspect) {
        drawW = cw
        drawH = cw / mediaAspect
    } else {
        drawH = ch
        drawW = ch * mediaAspect
    }
    val left = (cw - drawW) / 2f
    val top = (ch - drawH) / 2f
    return Rect(left, top, left + drawW, top + drawH)
}
