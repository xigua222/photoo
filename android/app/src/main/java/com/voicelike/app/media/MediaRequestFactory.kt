package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_MEDIA_REQUEST_SIZE = 1200
private const val HOME_CARD_MEDIA_REQUEST_SIZE = 1024
private const val HOME_CARD_PRESSURE_MEDIA_REQUEST_SIZE = 960
private const val HOME_CARD_CONSERVATIVE_MEDIA_REQUEST_SIZE = 768
private const val HOME_CARD_CRITICAL_MEDIA_REQUEST_SIZE = 640
private const val HOME_CARD_HOVER_MEDIA_REQUEST_SIZE = 1600
private const val HOME_CARD_HOVER_PRESSURE_MEDIA_REQUEST_SIZE = 1280
private const val SIMILAR_COMPARE_MEDIA_REQUEST_SIZE = 1200

@Composable
private fun rememberHomeCardRequestRefreshToken(): Int {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeToken by remember(lifecycleOwner) { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeToken += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return resumeToken
}

private fun buildStillImageRequest(
    context: Context,
    uri: Uri,
    size: Int,
    crossfade: Boolean,
    allowHardware: Boolean,
    bitmapConfig: Bitmap.Config?,
    cacheKey: String? = null
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(uri)
        .size(size)
        .crossfade(crossfade)
        .allowHardware(allowHardware)
    if (!cacheKey.isNullOrBlank()) {
        builder.memoryCacheKey(cacheKey)
        builder.diskCacheKey(cacheKey)
    }
    if (bitmapConfig != null) {
        builder.bitmapConfig(bitmapConfig)
    }
    return builder.build()
}

private fun buildVideoFrameImageRequest(
    context: Context,
    uri: Uri,
    size: Int,
    crossfade: Boolean,
    allowHardware: Boolean,
    cacheKey: String? = null
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(uri)
        .decoderFactory(VideoFrameDecoder.Factory())
        .size(size)
        .crossfade(crossfade)
        .allowHardware(allowHardware)
    if (!cacheKey.isNullOrBlank()) {
        builder.memoryCacheKey(cacheKey)
        builder.diskCacheKey(cacheKey)
    }
    return builder.build()
}

@Composable
fun rememberStillImageRequest(
    uri: Uri,
    size: Int = DEFAULT_MEDIA_REQUEST_SIZE,
    crossfade: Boolean = true,
    allowHardware: Boolean = true,
    bitmapConfig: Bitmap.Config? = null
): ImageRequest {
    val context = LocalContext.current
    return remember(context, uri, size, crossfade, allowHardware, bitmapConfig) {
        buildStillImageRequest(
            context = context,
            uri = uri,
            size = size,
            crossfade = crossfade,
            allowHardware = allowHardware,
            bitmapConfig = bitmapConfig
        )
    }
}

@Composable
fun rememberVideoFrameImageRequest(
    uri: Uri,
    size: Int = DEFAULT_MEDIA_REQUEST_SIZE,
    crossfade: Boolean = true,
    allowHardware: Boolean = true
): ImageRequest {
    val context = LocalContext.current
    return remember(context, uri, size, crossfade, allowHardware) {
        buildVideoFrameImageRequest(
            context = context,
            uri = uri,
            size = size,
            crossfade = crossfade,
            allowHardware = allowHardware
        )
    }
}

fun buildHomeCardStillImageRequest(
    context: Context,
    uri: Uri,
    itemId: Long? = null,
    crossfade: Boolean = true
): ImageRequest {
    val memoryPressureHigh = AppPerformanceGovernor.isRealtimeMemoryPressureHigh()
    val size = when {
        memoryPressureHigh -> HOME_CARD_CRITICAL_MEDIA_REQUEST_SIZE
        else -> HOME_CARD_MEDIA_REQUEST_SIZE
    }
    val cacheKey = itemId?.let { "home_card_still_${it}_${size}_${if (memoryPressureHigh) "sw" else "hw"}" }
    return buildStillImageRequest(
        context = context,
        uri = uri,
        size = size,
        crossfade = crossfade,
        allowHardware = !memoryPressureHigh,
        bitmapConfig = if (memoryPressureHigh) Bitmap.Config.RGB_565 else null,
        cacheKey = cacheKey
    )
}

fun buildHomeCardVideoFrameImageRequest(
    context: Context,
    uri: Uri,
    itemId: Long? = null,
    crossfade: Boolean = true
): ImageRequest {
    val memoryPressureHigh = AppPerformanceGovernor.isRealtimeMemoryPressureHigh()
    val size = when {
        memoryPressureHigh -> HOME_CARD_CRITICAL_MEDIA_REQUEST_SIZE
        else -> HOME_CARD_PRESSURE_MEDIA_REQUEST_SIZE
    }
    val cacheKey = itemId?.let { "home_card_video_${it}_${size}_${if (memoryPressureHigh) "sw" else "hw"}" }
    return buildVideoFrameImageRequest(
        context = context,
        uri = uri,
        size = size,
        crossfade = crossfade,
        allowHardware = !memoryPressureHigh,
        cacheKey = cacheKey
    )
}

fun buildSimilarCompareImageRequest(
    context: Context,
    item: MediaItem
): ImageRequest {
    val cacheKey = "similar_compare_${item.id}"
    return when (item.type) {
        "video" -> buildVideoFrameImageRequest(
            context = context,
            uri = item.uri,
            size = SIMILAR_COMPARE_MEDIA_REQUEST_SIZE,
            crossfade = false,
            allowHardware = false,
            cacheKey = cacheKey
        )

        else -> buildStillImageRequest(
            context = context,
            uri = item.uri,
            size = SIMILAR_COMPARE_MEDIA_REQUEST_SIZE,
            crossfade = false,
            allowHardware = false,
            bitmapConfig = null,
            cacheKey = cacheKey
        )
    }
}

suspend fun prefetchHomeCardMedia(context: Context, item: MediaItem) {
    val appContext = context.applicationContext
    withContext(Dispatchers.IO) {
        val request = when (item.type) {
            "video" -> buildHomeCardVideoFrameImageRequest(
                context = appContext,
                uri = item.uri,
                itemId = item.id,
                crossfade = false
            )
            else -> buildHomeCardStillImageRequest(
                context = appContext,
                uri = item.uri,
                itemId = item.id,
                crossfade = false
            )
        }
        runCatching { appContext.imageLoader.execute(request) }
    }
}

suspend fun prefetchSimilarCompareMedia(
    context: Context,
    items: List<MediaItem>,
    maxCount: Int = 2
) {
    if (items.isEmpty() || maxCount <= 0) return
    val appContext = context.applicationContext
    withContext(Dispatchers.IO) {
        items.take(maxCount).forEach { item ->
            val request = buildSimilarCompareImageRequest(
                context = appContext,
                item = item
            )
            runCatching { appContext.imageLoader.execute(request) }
        }
    }
}

@Composable
fun rememberHomeCardStillImageRequest(
    itemId: Long?,
    uri: Uri,
    crossfade: Boolean = true
): ImageRequest {
    val context = LocalContext.current
    val refreshToken = rememberHomeCardRequestRefreshToken()
    val memoryPressureHigh = AppPerformanceGovernor.isRealtimeMemoryPressureHigh()
    return remember(context, itemId, uri, crossfade, memoryPressureHigh, refreshToken) {
        buildHomeCardStillImageRequest(
            context = context,
            itemId = itemId,
            uri = uri,
            crossfade = crossfade
        )
    }
}

@Composable
fun rememberHomeCardHoverStillImageRequest(
    itemId: Long?,
    uri: Uri,
    crossfade: Boolean = false
): ImageRequest {
    val context = LocalContext.current
    val refreshToken = rememberHomeCardRequestRefreshToken()
    val memoryPressureHigh = AppPerformanceGovernor.isRealtimeMemoryPressureHigh()
    return remember(context, itemId, uri, crossfade, memoryPressureHigh, refreshToken) {
        buildHomeCardHoverStillImageRequest(
            context = context,
            uri = uri,
            itemId = itemId,
            crossfade = crossfade
        )
    }
}

fun buildHomeCardHoverStillImageRequest(
    context: Context,
    uri: Uri,
    itemId: Long? = null,
    crossfade: Boolean = false
): ImageRequest {
    val memoryPressureHigh = AppPerformanceGovernor.isRealtimeMemoryPressureHigh()
    val size = if (memoryPressureHigh) {
        HOME_CARD_HOVER_PRESSURE_MEDIA_REQUEST_SIZE
    } else {
        HOME_CARD_HOVER_MEDIA_REQUEST_SIZE
    }
    val cacheKey = itemId?.let {
        "home_card_hover_still_${it}_${size}_${if (memoryPressureHigh) "sw" else "hw"}"
    }
    return buildStillImageRequest(
        context = context,
        uri = uri,
        size = size,
        crossfade = crossfade,
        allowHardware = !memoryPressureHigh,
        bitmapConfig = null,
        cacheKey = cacheKey
    )
}

@Composable
fun rememberHomeCardVideoFrameImageRequest(
    itemId: Long?,
    uri: Uri,
    crossfade: Boolean = true
): ImageRequest {
    val context = LocalContext.current
    val refreshToken = rememberHomeCardRequestRefreshToken()
    val memoryPressureHigh = AppPerformanceGovernor.isRealtimeMemoryPressureHigh()
    return remember(context, itemId, uri, crossfade, memoryPressureHigh, refreshToken) {
        buildHomeCardVideoFrameImageRequest(
            context = context,
            itemId = itemId,
            uri = uri,
            crossfade = crossfade
        )
    }
}
