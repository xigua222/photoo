package com.voicelike.app.keep

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import com.voicelike.app.BoundedKeySet
import com.voicelike.app.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val KEEP_GALLERY_THUMB_CACHE_VERSION = "v2"
private const val KEEP_GALLERY_THUMB_SIZE_2_COL = 1024
private const val KEEP_GALLERY_THUMB_SIZE_3_COL = 768
private const val KEEP_GALLERY_THUMB_SIZE_4_COL = 512
private const val KEEP_GALLERY_THUMB_SIZE_5_COL = 320
private const val KEEP_GALLERY_THUMB_SIZE_6_COL = 224
private const val KEEP_GALLERY_BITMAP_CACHE_SIZE_KB = 128 * 1024
private const val KEEP_GALLERY_ULTRA_DETAIL_MIN_RATIO = 0.9f
private const val KEEP_GALLERY_HIGH_DETAIL_MIN_RATIO = 0.82f
private const val KEEP_GALLERY_DEFAULT_MIN_RATIO = 0.66f
private const val KEEP_GALLERY_HIGH_QUALITY_SOURCE_BUCKET_MIN = KEEP_GALLERY_THUMB_SIZE_4_COL

private val KEEP_GALLERY_THUMB_BUCKET_ORDER = intArrayOf(
    KEEP_GALLERY_THUMB_SIZE_2_COL,
    KEEP_GALLERY_THUMB_SIZE_3_COL,
    KEEP_GALLERY_THUMB_SIZE_4_COL,
    KEEP_GALLERY_THUMB_SIZE_5_COL,
    KEEP_GALLERY_THUMB_SIZE_6_COL,
)

private val keepGalleryBitmapCache = object : LruCache<String, Bitmap>(KEEP_GALLERY_BITMAP_CACHE_SIZE_KB) {
    override fun sizeOf(key: String, value: Bitmap): Int {
        return (value.allocationByteCount / 1024).coerceAtLeast(1)
    }
}

fun keepGalleryThumbSizeBucket(columns: Int): Int {
    return when {
        columns <= 2 -> KEEP_GALLERY_THUMB_SIZE_2_COL
        columns <= 3 -> KEEP_GALLERY_THUMB_SIZE_3_COL
        columns <= 4 -> KEEP_GALLERY_THUMB_SIZE_4_COL
        columns <= 5 -> KEEP_GALLERY_THUMB_SIZE_5_COL
        else -> KEEP_GALLERY_THUMB_SIZE_6_COL
    }
}

fun keepGalleryThumbCacheKey(
    item: MediaItem,
    sizeBucket: Int
): String = "keep_gallery_${KEEP_GALLERY_THUMB_CACHE_VERSION}_${item.type}_${item.id}_$sizeBucket"

fun peekBestKeepGalleryThumbBitmap(
    item: MediaItem,
    preferredSizeBucket: Int
): Bitmap? {
    synchronized(keepGalleryBitmapCache) {
        keepGalleryBitmapCache.get(
            keepGalleryThumbCacheKey(item, preferredSizeBucket)
        )?.let { return it }

        // Prefer equal-or-larger buckets when zooming in so we don't upscale tiny cached tiles.
        for (index in KEEP_GALLERY_THUMB_BUCKET_ORDER.lastIndex downTo 0) {
            val candidateBucket = KEEP_GALLERY_THUMB_BUCKET_ORDER[index]
            if (candidateBucket <= preferredSizeBucket) continue
            keepGalleryBitmapCache.get(
                keepGalleryThumbCacheKey(item, candidateBucket)
            )?.let { return it }
        }

        val minAcceptableRatio = when {
            preferredSizeBucket >= KEEP_GALLERY_THUMB_SIZE_3_COL -> KEEP_GALLERY_ULTRA_DETAIL_MIN_RATIO
            preferredSizeBucket >= KEEP_GALLERY_THUMB_SIZE_4_COL -> KEEP_GALLERY_HIGH_DETAIL_MIN_RATIO
            else -> KEEP_GALLERY_DEFAULT_MIN_RATIO
        }
        for (candidateBucket in KEEP_GALLERY_THUMB_BUCKET_ORDER) {
            if (candidateBucket >= preferredSizeBucket) continue
            if (candidateBucket.toFloat() / preferredSizeBucket.toFloat() < minAcceptableRatio) {
                break
            }
            keepGalleryBitmapCache.get(
                keepGalleryThumbCacheKey(item, candidateBucket)
            )?.let { return it }
        }
        if (preferredSizeBucket >= KEEP_GALLERY_THUMB_SIZE_4_COL) {
            return null
        }
    }
    return null
}

fun buildKeepGalleryThumbRequest(
    context: Context,
    item: MediaItem,
    sizeBucket: Int
): ImageRequest {
    val cacheKey = keepGalleryThumbCacheKey(item, sizeBucket)
    val builder = ImageRequest.Builder(context)
        .data(item.uri)
        .size(sizeBucket)
        .crossfade(false)
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .allowHardware(false)
        .bitmapConfig(keepGalleryBitmapConfig(sizeBucket))
    if (shouldPreferHighQualitySourceDecode(item, sizeBucket)) {
        builder.precision(Precision.EXACT)
    }
    if (item.type == "video") {
        builder.decoderFactory(VideoFrameDecoder.Factory())
    }
    return builder.build()
}

private fun keepGalleryBitmapConfig(sizeBucket: Int): Bitmap.Config {
    return when {
        sizeBucket >= KEEP_GALLERY_THUMB_SIZE_3_COL -> Bitmap.Config.ARGB_8888
        sizeBucket >= KEEP_GALLERY_THUMB_SIZE_4_COL -> Bitmap.Config.ARGB_8888
        else -> Bitmap.Config.RGB_565
    }
}

suspend fun loadKeepGalleryThumbBitmap(
    context: Context,
    item: MediaItem,
    sizeBucket: Int
): Bitmap? {
    val appContext = context.applicationContext
    val cacheKey = keepGalleryThumbCacheKey(item, sizeBucket)
    synchronized(keepGalleryBitmapCache) {
        keepGalleryBitmapCache.get(cacheKey)?.let { cached ->
            if (!shouldPreferHighQualitySourceDecode(item, sizeBucket) || isBitmapSufficientForBucket(cached, sizeBucket)) {
                return cached
            }
        }
    }

    return withContext(Dispatchers.IO) {
        synchronized(keepGalleryBitmapCache) {
            keepGalleryBitmapCache.get(cacheKey)?.let { cached ->
                if (!shouldPreferHighQualitySourceDecode(item, sizeBucket) || isBitmapSufficientForBucket(cached, sizeBucket)) {
                    return@withContext cached
                }
            }
        }

        val bitmap = loadPreferredKeepGalleryThumbnail(
            context = appContext,
            item = item,
            sizeBucket = sizeBucket
        )

        if (bitmap != null) {
            synchronized(keepGalleryBitmapCache) {
                keepGalleryBitmapCache.put(cacheKey, bitmap)
            }
        }
        bitmap
    }
}

private suspend fun loadPreferredKeepGalleryThumbnail(
    context: Context,
    item: MediaItem,
    sizeBucket: Int
): Bitmap? {
    val preferSourceDecode = shouldPreferHighQualitySourceDecode(item, sizeBucket)
    if (preferSourceDecode) {
        val sourceBitmap = loadCoilKeepGalleryThumbnail(
            context = context,
            item = item,
            sizeBucket = sizeBucket
        )
        if (sourceBitmap != null) {
            return sourceBitmap
        }
    }

    val platformBitmap = loadPlatformKeepGalleryThumbnail(
        context = context,
        item = item,
        sizeBucket = sizeBucket
    )
    if (platformBitmap != null && (!preferSourceDecode || isBitmapSufficientForBucket(platformBitmap, sizeBucket))) {
        return platformBitmap
    }

    return loadCoilKeepGalleryThumbnail(
        context = context,
        item = item,
        sizeBucket = sizeBucket
    ) ?: platformBitmap
}

private fun shouldPreferHighQualitySourceDecode(
    item: MediaItem,
    sizeBucket: Int
): Boolean {
    return item.type == "photo" && sizeBucket >= KEEP_GALLERY_HIGH_QUALITY_SOURCE_BUCKET_MIN
}

private fun isBitmapSufficientForBucket(
    bitmap: Bitmap,
    sizeBucket: Int
): Boolean {
    val minDimension = minOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val minimumRequiredDimension = when {
        sizeBucket >= KEEP_GALLERY_THUMB_SIZE_2_COL -> (sizeBucket * 0.94f).toInt()
        sizeBucket >= KEEP_GALLERY_THUMB_SIZE_3_COL -> (sizeBucket * 0.91f).toInt()
        sizeBucket >= KEEP_GALLERY_THUMB_SIZE_4_COL -> (sizeBucket * 0.88f).toInt()
        else -> (sizeBucket * 0.78f).toInt()
    }.coerceAtLeast(1)
    return minDimension >= minimumRequiredDimension
}

private fun loadPlatformKeepGalleryThumbnail(
    context: Context,
    item: MediaItem,
    sizeBucket: Int
): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    return runCatching {
        context.contentResolver.loadThumbnail(item.uri, Size(sizeBucket, sizeBucket), null)
    }.getOrNull()
}

private suspend fun loadCoilKeepGalleryThumbnail(
    context: Context,
    item: MediaItem,
    sizeBucket: Int
): Bitmap? {
    return runCatching {
        val result = context.imageLoader.execute(
            buildKeepGalleryThumbRequest(
                context = context,
                item = item,
                sizeBucket = sizeBucket
            )
        )
        (result as? SuccessResult)?.drawable?.toBitmap()
    }.getOrNull()
}

suspend fun prefetchKeepGalleryThumbs(
    context: Context,
    items: List<MediaItem>,
    sizeBucket: Int,
    prefetchedKeys: BoundedKeySet<String>,
    maxCount: Int = 24
) {
    if (items.isEmpty() || maxCount <= 0) return
    val appContext = context.applicationContext
    withContext(Dispatchers.IO) {
        var prefetchedCount = 0
        items.forEach { item ->
            if (prefetchedCount >= maxCount) return@withContext
            val cacheKey = keepGalleryThumbCacheKey(item, sizeBucket)
            synchronized(keepGalleryBitmapCache) {
                if (keepGalleryBitmapCache.get(cacheKey) != null) return@forEach
            }
            if (prefetchedKeys.contains(cacheKey)) return@forEach
            prefetchedKeys.add(cacheKey)
            val thumbnail = loadKeepGalleryThumbBitmap(
                context = appContext,
                item = item,
                sizeBucket = sizeBucket
            )
            if (thumbnail != null) {
                prefetchedCount += 1
            }
        }
    }
}
