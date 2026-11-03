package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

private const val COLOR_CARD_FILTER_FEED_DEMO_MANIFEST_ASSET =
    "color_card_filter_feed_demo_manifest.json"
private const val COLOR_CARD_FILTER_FEED_BOOTSTRAP_MANIFEST_ASSET =
    "color_card_filter_feed_bootstrap_manifest.json"
private const val COLOR_CARD_FILTER_FEED_PREVIEW_CACHE_DIR = "color_card_filter_feed_preview"
private const val COLOR_CARD_FILTER_FEED_PREVIEW_CACHE_VERSION = 10
private const val COLOR_CARD_FILTER_FEED_WARMUP_SIZE = 2
private const val COLOR_CARD_FILTER_FEED_PREVIEW_DISK_CACHE_MAX_FILES = 160
private const val COLOR_CARD_FILTER_FEED_RUNTIME_PREVIEW_MAX_SIDE = 256
private const val COLOR_CARD_FILTER_FEED_FAST_PREVIEW_MAX_SIDE = 192
private const val COLOR_CARD_FILTER_FEED_PREVIEW_MEMORY_MAX_ENTRIES = 128
private const val COLOR_CARD_FILTER_FEED_SHARED_PALETTE_PREVIEW_MEMORY_MAX_ENTRIES = 64
private const val COLOR_CARD_FILTER_FEED_DEMO_BITMAP_MEMORY_MAX_ENTRIES = 12
private const val COLOR_CARD_FILTER_FEED_PRIORITY_PREVIEW_BATCH_SIZE = 6
private const val COLOR_CARD_FILTER_FEED_APPROX_PREVIEW_BATCH_SIZE = 6
private const val COLOR_CARD_FILTER_FEED_STYLIZED_PREVIEW_BATCH_SIZE = 12
private const val COLOR_CARD_FILTER_FEED_HIGH_FIDELITY_BATCH_SIZE = 6
private val colorCardFilterFeedPreviewMemoryCache =
    LinkedHashMap<String, Bitmap>(64, 0.75f, true)
private val colorCardFilterFeedPreviewHighFidelityKeys = mutableSetOf<String>()
private val colorCardFilterFeedPreviewMemoryLock = Any()
private val colorCardFilterFeedSharedPalettePreviewMemoryCache =
    LinkedHashMap<String, Bitmap>(32, 0.75f, true)
private val colorCardFilterFeedSharedPalettePreviewMemoryLock = Any()
private val colorCardFilterFeedSharedPalettePreviewVersionState = mutableIntStateOf(0)
private val colorCardFilterFeedDemoBitmapMemoryCache =
    LinkedHashMap<String, Bitmap>(16, 0.75f, true)
private val colorCardFilterFeedDemoBitmapMemoryLock = Any()

internal data class ColorCardFilterFeedDemoAsset(
    val id: String,
    val assetName: String
)

internal data class ColorCardFilterFeedPreviewRequest(
    val previewKey: String,
    val candidate: ColorCardTraceFilterCandidate,
    val demoAssetName: String,
    val highFidelity: Boolean = false,
    val allowApproximate: Boolean = true,
    val preferStylizedPreview: Boolean = false,
    val runtimeGenerationEnabled: Boolean = true
)

internal data class ColorCardFilterFeedPreviewDebugInfo(
    val requestCount: Int = 0,
    val generatedCount: Int = 0,
    val lastStage: String = "idle",
    val currentPreviewKey: String? = null,
    val lastFailurePreviewKey: String? = null,
    val lastFailureReason: String? = null
)

internal data class ColorCardFilterFeedBootstrapPreviewAsset(
    val previewKey: String,
    val paletteId: String,
    val demoAssetName: String,
    val assetName: String
)

internal fun buildColorCardFilterFeedPreviewKey(
    candidate: ColorCardTraceFilterCandidate,
    demoAssetName: String
): String {
    val baseKey = "${candidate.paletteId}:$demoAssetName"
    return if (isColorCardTracePSeriesCandidate(candidate)) {
        "$baseKey:pseries-v1"
    } else {
        baseKey
    }
}

internal fun resolveColorCardFilterFeedAssignedDemoAssetNames(
    candidates: List<ColorCardTraceFilterCandidate>,
    demoAssetNames: List<String>,
    bootstrapAssets: List<ColorCardFilterFeedBootstrapPreviewAsset>
): Map<String, String> {
    val eligibleDemoAssetNames = demoAssetNames
        .filter(String::isNotBlank)
        .distinct()
    if (candidates.isEmpty() || eligibleDemoAssetNames.isEmpty()) return emptyMap()
    val eligibleDemoAssetNameSet = eligibleDemoAssetNames.toSet()
    val bootstrapDemoAssetNamesByPaletteId = bootstrapAssets
        .groupBy(ColorCardFilterFeedBootstrapPreviewAsset::paletteId)
        .mapValues { (_, assets) ->
            assets.firstNotNullOfOrNull { asset ->
                asset.demoAssetName.takeIf { it in eligibleDemoAssetNameSet }
            }
        }
    return buildMap {
        candidates.forEachIndexed { index, candidate ->
            put(
                candidate.paletteId,
                bootstrapDemoAssetNamesByPaletteId[candidate.paletteId]
                    ?: eligibleDemoAssetNames[index % eligibleDemoAssetNames.size]
            )
        }
    }
}

internal fun shouldColorCardFilterFeedForceRealtimePreview(
    candidate: ColorCardTraceFilterCandidate
): Boolean = candidate.kind == ColorCardTraceFilterKind.PixelPoster

private fun snapshotColorCardFilterFeedSharedPalettePreviewBitmaps(
    requests: List<ColorCardFilterFeedPreviewRequest>
): Map<String, Bitmap> = synchronized(colorCardFilterFeedSharedPalettePreviewMemoryLock) {
    buildMap {
        requests.forEach { request ->
            if (!shouldColorCardFilterFeedForceRealtimePreview(request.candidate)) return@forEach
            colorCardFilterFeedSharedPalettePreviewMemoryCache[request.candidate.paletteId]
                ?.takeUnless(Bitmap::isRecycled)
                ?.let { bitmap ->
                    put(request.previewKey, bitmap)
                }
        }
    }
}

internal fun cacheColorCardFilterFeedSharedPalettePreviewBitmaps(
    requests: List<ColorCardFilterFeedPreviewRequest>,
    generated: Map<String, Bitmap>
) {
    if (requests.isEmpty() || generated.isEmpty()) return
    var changed = false
    synchronized(colorCardFilterFeedSharedPalettePreviewMemoryLock) {
        requests.forEach { request ->
            if (!shouldColorCardFilterFeedForceRealtimePreview(request.candidate)) return@forEach
            val bitmap = generated[request.previewKey]
                ?.takeUnless(Bitmap::isRecycled)
                ?: return@forEach
            val paletteId = request.candidate.paletteId
            val existing = colorCardFilterFeedSharedPalettePreviewMemoryCache[paletteId]
            if (existing !== bitmap) {
                colorCardFilterFeedSharedPalettePreviewMemoryCache[paletteId] = bitmap
                changed = true
            }
        }
        while (
            colorCardFilterFeedSharedPalettePreviewMemoryCache.size >
                COLOR_CARD_FILTER_FEED_SHARED_PALETTE_PREVIEW_MEMORY_MAX_ENTRIES
        ) {
            val eldestEntry = colorCardFilterFeedSharedPalettePreviewMemoryCache.entries.firstOrNull() ?: break
            colorCardFilterFeedSharedPalettePreviewMemoryCache.remove(eldestEntry.key)
            changed = true
        }
    }
    if (changed) {
        colorCardFilterFeedSharedPalettePreviewVersionState.intValue += 1
    }
}

@Composable
internal fun rememberColorCardFilterFeedDemoAssets(): List<ColorCardFilterFeedDemoAsset> {
    val context = LocalContext.current
    val demoAssets by produceState(
        initialValue = emptyList<ColorCardFilterFeedDemoAsset>(),
        key1 = context
    ) {
        value = loadColorCardFilterFeedDemoAssets(context.applicationContext)
    }
    return demoAssets
}

@Composable
internal fun rememberColorCardFilterFeedBootstrapPreviewAssets(): List<ColorCardFilterFeedBootstrapPreviewAsset> {
    val context = LocalContext.current
    val bootstrapAssets by produceState(
        initialValue = emptyList<ColorCardFilterFeedBootstrapPreviewAsset>(),
        key1 = context
    ) {
        value = loadColorCardFilterFeedBootstrapPreviewAssets(context.applicationContext)
    }
    return bootstrapAssets
}

@Composable
internal fun rememberColorCardFilterFeedDemoBitmaps(
    assetNames: Set<String>
): Map<String, Bitmap> {
    val context = LocalContext.current
    val bitmaps by produceState(
        initialValue = emptyMap<String, Bitmap>(),
        key1 = context,
        key2 = assetNames
    ) {
        value = withContext(Dispatchers.IO) {
            buildMap {
                assetNames.forEach { assetName ->
                    decodeColorCardFilterFeedDemoBitmap(
                        context = context.applicationContext,
                        assetName = assetName
                    )?.let { bitmap ->
                        put(assetName, bitmap)
                    }
                }
            }
        }
    }
    DisposableEffect(bitmaps) { onDispose { } }
    return bitmaps
}

@Composable
internal fun rememberColorCardFilterFeedPreviewBitmaps(
    requests: List<ColorCardFilterFeedPreviewRequest>
): Map<String, Bitmap> {
    val context = LocalContext.current
    val sharedPalettePreviewVersion = colorCardFilterFeedSharedPalettePreviewVersionState.intValue
    val bootstrapAssets = rememberColorCardFilterFeedBootstrapPreviewAssets()
    val bootstrapAssetsByKey = remember(bootstrapAssets) {
        bootstrapAssets.associateBy { it.previewKey }
    }
    val requestKeys = remember(requests) { requests.map(ColorCardFilterFeedPreviewRequest::previewKey).toSet() }
    val previewBitmaps by produceState(
        initialValue = snapshotColorCardFilterFeedPreviewMemoryCache(requestKeys),
        context,
        sharedPalettePreviewVersion,
        requestKeys,
        requests,
        bootstrapAssetsByKey
    ) {
        if (requests.isEmpty()) {
            value = emptyMap()
            return@produceState
        }

        val appContext = context.applicationContext
        var current = snapshotColorCardFilterFeedPreviewMemoryCache(requestKeys)
        val sharedPaletteCached = snapshotColorCardFilterFeedSharedPalettePreviewBitmaps(requests)
        if (sharedPaletteCached.isNotEmpty()) {
            current = current + sharedPaletteCached
        }
        value = current

        val missingFromMemory = requests.filter { request ->
            current[request.previewKey]?.isRecycled != false
        }
        if (missingFromMemory.isNotEmpty()) {
            val bootstrapCached = loadColorCardFilterFeedBootstrapPreviewBitmaps(
                context = appContext,
                requests = missingFromMemory,
                bootstrapAssetsByKey = bootstrapAssetsByKey
            )
            if (bootstrapCached.isNotEmpty()) {
                current = current + bootstrapCached
                value = current
                cacheColorCardFilterFeedPreviewBitmaps(
                    generated = bootstrapCached,
                    highFidelity = true
                )
                cacheColorCardFilterFeedSharedPalettePreviewBitmaps(
                    requests = missingFromMemory,
                    generated = bootstrapCached
                )
            }
        }

        val missingFromBootstrap = requests.filter { request ->
            current[request.previewKey]?.isRecycled != false
        }
        if (missingFromBootstrap.isNotEmpty()) {
            val diskCached = loadColorCardFilterFeedPreviewBitmapsFromDisk(
                context = appContext,
                requests = missingFromBootstrap
            )
            if (diskCached.isNotEmpty()) {
                current = current + diskCached
                value = current
                cacheColorCardFilterFeedPreviewBitmaps(
                    generated = diskCached,
                    highFidelity = true
                )
                cacheColorCardFilterFeedSharedPalettePreviewBitmaps(
                    requests = missingFromBootstrap,
                    generated = diskCached
                )
            }
        }

        val requestsMissingPreview = requests.filter { request ->
            current[request.previewKey]?.isRecycled != false
        }
        if (requestsMissingPreview.isNotEmpty()) {
            requestsMissingPreview
                .filter(ColorCardFilterFeedPreviewRequest::runtimeGenerationEnabled)
                .filter { request ->
                    shouldColorCardFilterFeedForceRealtimePreview(request.candidate)
                }
                .chunked(COLOR_CARD_FILTER_FEED_PRIORITY_PREVIEW_BATCH_SIZE)
                .forEach { batch ->
                    val generatedBitmaps = buildColorCardFilterFeedPreviewBitmaps(
                        context = appContext,
                        requests = batch
                    )
                    if (generatedBitmaps.isNotEmpty()) {
                        current = current + generatedBitmaps
                        value = current
                        cacheColorCardFilterFeedPreviewBitmaps(
                            generated = generatedBitmaps,
                            highFidelity = true
                        )
                        cacheColorCardFilterFeedSharedPalettePreviewBitmaps(
                            requests = batch,
                            generated = generatedBitmaps
                        )
                        persistColorCardFilterFeedPreviewBitmaps(
                            context = appContext,
                            generated = generatedBitmaps
                        )
                    }
                }

            val requestsStillMissingAfterPriority = requests.filter { request ->
                current[request.previewKey]?.isRecycled != false
            }
            requestsStillMissingAfterPriority
                .filter(ColorCardFilterFeedPreviewRequest::runtimeGenerationEnabled)
                .filter(ColorCardFilterFeedPreviewRequest::preferStylizedPreview)
                .chunked(COLOR_CARD_FILTER_FEED_STYLIZED_PREVIEW_BATCH_SIZE)
                .forEach { batch ->
                    val stylizedBitmaps = buildColorCardFilterFeedPreviewBitmaps(
                        context = appContext,
                        requests = batch
                    )
                    if (stylizedBitmaps.isNotEmpty()) {
                        current = current + stylizedBitmaps
                        value = current
                        cacheColorCardFilterFeedPreviewBitmaps(
                            generated = stylizedBitmaps,
                            highFidelity = true
                        )
                        cacheColorCardFilterFeedSharedPalettePreviewBitmaps(
                            requests = batch,
                            generated = stylizedBitmaps
                        )
                        persistColorCardFilterFeedPreviewBitmaps(
                            context = appContext,
                            generated = stylizedBitmaps
                        )
                    }
                }

            val requestsStillMissingPreview = requests.filter { request ->
                current[request.previewKey]?.isRecycled != false
            }
            requestsStillMissingPreview
                .filter(ColorCardFilterFeedPreviewRequest::runtimeGenerationEnabled)
                .filter(ColorCardFilterFeedPreviewRequest::allowApproximate)
                .chunked(COLOR_CARD_FILTER_FEED_APPROX_PREVIEW_BATCH_SIZE)
                .forEach { batch ->
                    val approximateBitmaps = buildColorCardFilterFeedApproxPreviewBitmaps(
                        context = appContext,
                        requests = batch
                    )
                    if (approximateBitmaps.isNotEmpty()) {
                        current = current + approximateBitmaps
                        value = current
                        cacheColorCardFilterFeedPreviewBitmaps(
                            generated = approximateBitmaps,
                            highFidelity = false
                        )
                        cacheColorCardFilterFeedSharedPalettePreviewBitmaps(
                            requests = batch,
                            generated = approximateBitmaps
                        )
                    }
                }
        }

        val requestsNeedingHighFidelity = requests.filter { request ->
            request.runtimeGenerationEnabled &&
            request.highFidelity &&
                !isColorCardFilterFeedPreviewHighFidelityReady(
                    context = appContext,
                    previewKey = request.previewKey,
                    bootstrapAssetsByKey = bootstrapAssetsByKey
                )
        }
        if (requestsNeedingHighFidelity.isNotEmpty()) {
            requestsNeedingHighFidelity
                .chunked(COLOR_CARD_FILTER_FEED_HIGH_FIDELITY_BATCH_SIZE)
                .forEach { batch ->
                    val generatedBitmaps = buildColorCardFilterFeedPreviewBitmaps(
                        context = appContext,
                        requests = batch
                    )
                    if (generatedBitmaps.isEmpty()) return@forEach
                    current = current + generatedBitmaps
                    value = current
                    cacheColorCardFilterFeedPreviewBitmaps(
                        generated = generatedBitmaps,
                        highFidelity = true
                    )
                    cacheColorCardFilterFeedSharedPalettePreviewBitmaps(
                        requests = batch,
                        generated = generatedBitmaps
                    )
                    persistColorCardFilterFeedPreviewBitmaps(
                        context = appContext,
                        generated = generatedBitmaps
                    )
                }
        }
    }

    DisposableEffect(requestKeys) {
        onDispose { }
    }
    return previewBitmaps
}

internal fun hasColorCardFilterFeedPreviewReady(
    context: Context,
    previewKey: String,
    bootstrapPreviewKeys: Set<String>
): Boolean {
    if (bootstrapPreviewKeys.contains(previewKey)) return true
    synchronized(colorCardFilterFeedPreviewMemoryLock) {
        val cached = colorCardFilterFeedPreviewMemoryCache[previewKey]
        if (cached != null && !cached.isRecycled) return true
    }
    val cacheFile = colorCardFilterFeedPreviewDiskCacheFile(context, previewKey)
    return cacheFile.exists() && cacheFile.length() > 0L
}

@Composable
internal fun rememberColorCardFilterFeedPreviewDebugInfo(
    requests: List<ColorCardFilterFeedPreviewRequest>
): ColorCardFilterFeedPreviewDebugInfo {
    val context = LocalContext.current
    val debugInfo by produceState(
        initialValue = ColorCardFilterFeedPreviewDebugInfo(requestCount = requests.size),
        key1 = context,
        key2 = requests
    ) {
        if (requests.isEmpty()) {
            value = ColorCardFilterFeedPreviewDebugInfo(lastStage = "empty")
            return@produceState
        }

        var generatedCount = 0
        value = ColorCardFilterFeedPreviewDebugInfo(
            requestCount = requests.size,
            generatedCount = 0,
            lastStage = "start",
            currentPreviewKey = requests.firstOrNull()?.previewKey
        )
        requests.forEach { request ->
            value = value.copy(
                currentPreviewKey = request.previewKey,
                lastStage = "decode"
            )
            val decodedDemoBitmap = runCatching {
                decodeColorCardFilterFeedDemoBitmap(
                    context = context.applicationContext,
                    assetName = request.demoAssetName
                )
            }.getOrElse { error ->
                value = value.copy(
                    lastStage = "decode_failed",
                    lastFailurePreviewKey = request.previewKey,
                    lastFailureReason = error.message ?: error::class.java.simpleName
                )
                null
            }
            if (decodedDemoBitmap == null) {
                value = value.copy(
                    lastStage = "decode_null",
                    lastFailurePreviewKey = request.previewKey,
                    lastFailureReason = request.demoAssetName
                )
                return@forEach
            }

            value = value.copy(lastStage = "render")
            val generatedBitmap = try {
                buildColorCardFilterFeedPreviewBitmap(
                    context = context.applicationContext,
                    request = request
                )
            } catch (error: Throwable) {
                value = value.copy(
                    lastStage = "render_failed",
                    lastFailurePreviewKey = request.previewKey,
                    lastFailureReason = error.message ?: error::class.java.simpleName
                )
                null
            }
            if (generatedBitmap == null) {
                value = value.copy(
                    lastStage = "render_null",
                    lastFailurePreviewKey = request.previewKey,
                    lastFailureReason = value.lastFailureReason ?: "returned null"
                )
                return@forEach
            }
            generatedBitmap.recycle()
            generatedCount += 1
            value = value.copy(
                generatedCount = generatedCount,
                lastStage = "generated"
            )
        }
        value = value.copy(
            currentPreviewKey = null,
            lastStage = "done"
        )
    }
    return debugInfo
}

@Composable
internal fun ColorCardFilterFeedWarmup() {
    val context = LocalContext.current
    val demoAssets = rememberColorCardFilterFeedDemoAssets()
    val bootstrapAssets = rememberColorCardFilterFeedBootstrapPreviewAssets()
    val builtinFilters by produceState(
        initialValue = emptyList<ColorCardBuiltinHaldFilterAsset>(),
        key1 = context
    ) {
        value = loadColorCardBuiltinHaldFilters(context.applicationContext)
    }
    val warmupRequests = remember(builtinFilters, demoAssets, bootstrapAssets) {
        if (demoAssets.isEmpty()) {
            emptyList()
        } else {
            val bootstrapPreviewKeys = bootstrapAssets.mapTo(mutableSetOf()) {
                it.previewKey
            }
            val candidates = buildColorCardTraceBuiltinCandidates(builtinFilters)
                .filterNot { candidate ->
                    candidate.kind == ColorCardTraceFilterKind.AiMnn ||
                        candidate.kind == ColorCardTraceFilterKind.MotionPhotoEntry ||
                        isColorCardTraceBlackWhiteEraCandidate(candidate) ||
                        isColorCardTracePSeriesCandidate(candidate) ||
                        candidate.paletteId == "builtin_creative:subject-frame"
                }
            val shuffledCandidates = candidates.shuffled(Random(0xC0110CA4))
            val shuffledDemoAssets = demoAssets.shuffled(Random(0xFEED2026))
            List(minOf(COLOR_CARD_FILTER_FEED_WARMUP_SIZE, shuffledCandidates.size * shuffledDemoAssets.size)) { index ->
                val candidate = shuffledCandidates[index % shuffledCandidates.size]
                val demoAsset = shuffledDemoAssets[(index + (index / shuffledDemoAssets.size.coerceAtLeast(1))) % shuffledDemoAssets.size]
                ColorCardFilterFeedPreviewRequest(
                    previewKey = "${candidate.paletteId}:${demoAsset.assetName}",
                    candidate = candidate,
                    demoAssetName = demoAsset.assetName
                )
            }.distinctBy { it.previewKey }
                .filterNot { it.previewKey in bootstrapPreviewKeys }
                .take(COLOR_CARD_FILTER_FEED_WARMUP_SIZE)
        }
    }
    rememberColorCardFilterFeedPreviewBitmaps(warmupRequests)
}

private suspend fun loadColorCardFilterFeedDemoAssets(
    context: Context
): List<ColorCardFilterFeedDemoAsset> = withContext(Dispatchers.IO) {
    val manifest = runCatching {
        context.assets.open(COLOR_CARD_FILTER_FEED_DEMO_MANIFEST_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }.getOrElse { return@withContext emptyList() }

    val root = runCatching { JSONArray(manifest) }.getOrElse {
        return@withContext emptyList()
    }

    buildList {
        for (index in 0 until root.length()) {
            val item = root.optJSONObject(index) ?: continue
            val id = item.optString("id").trim()
            val assetName = item.optString("assetName").trim()
            if (id.isBlank() || assetName.isBlank()) continue
            add(
                ColorCardFilterFeedDemoAsset(
                    id = id,
                    assetName = assetName
                )
            )
        }
    }
}

private suspend fun loadColorCardFilterFeedBootstrapPreviewAssets(
    context: Context
): List<ColorCardFilterFeedBootstrapPreviewAsset> = withContext(Dispatchers.IO) {
    val manifest = runCatching {
        context.assets.open(COLOR_CARD_FILTER_FEED_BOOTSTRAP_MANIFEST_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }.getOrElse { return@withContext emptyList() }

    val root = runCatching { JSONArray(manifest) }.getOrElse {
        return@withContext emptyList()
    }

    buildList {
        for (index in 0 until root.length()) {
            val item = root.optJSONObject(index) ?: continue
            val paletteId = item.optString("paletteId").trim()
            val demoAssetName = item.optString("demoAssetName").trim()
            val assetName = item.optString("assetName").trim()
            if (paletteId.isBlank() || demoAssetName.isBlank() || assetName.isBlank()) continue
            add(
                ColorCardFilterFeedBootstrapPreviewAsset(
                    previewKey = "$paletteId:$demoAssetName",
                    paletteId = paletteId,
                    demoAssetName = demoAssetName,
                    assetName = assetName
                )
            )
        }
    }
}

private fun snapshotColorCardFilterFeedPreviewMemoryCache(
    previewKeys: Set<String>
): Map<String, Bitmap> = synchronized(colorCardFilterFeedPreviewMemoryLock) {
    buildMap {
        previewKeys.forEach { previewKey ->
            colorCardFilterFeedPreviewMemoryCache[previewKey]
                ?.takeIf { !it.isRecycled }
                ?.let { put(previewKey, it) }
        }
    }
}

private fun cacheColorCardFilterFeedPreviewBitmaps(
    generated: Map<String, Bitmap>,
    highFidelity: Boolean
) {
    if (generated.isEmpty()) return
    synchronized(colorCardFilterFeedPreviewMemoryLock) {
        generated.forEach { (previewKey, bitmap) ->
            val existing = colorCardFilterFeedPreviewMemoryCache[previewKey]
            val existingIsHighFidelity = colorCardFilterFeedPreviewHighFidelityKeys.contains(previewKey)
            if (!highFidelity && existing != null && !existing.isRecycled && existingIsHighFidelity) {
                return@forEach
            }
            colorCardFilterFeedPreviewMemoryCache[previewKey] = bitmap
            if (highFidelity) {
                colorCardFilterFeedPreviewHighFidelityKeys += previewKey
            } else {
                colorCardFilterFeedPreviewHighFidelityKeys -= previewKey
            }
        }
        while (colorCardFilterFeedPreviewMemoryCache.size > COLOR_CARD_FILTER_FEED_PREVIEW_MEMORY_MAX_ENTRIES) {
            val eldestEntry = colorCardFilterFeedPreviewMemoryCache.entries.firstOrNull() ?: break
            colorCardFilterFeedPreviewMemoryCache.remove(eldestEntry.key)
            colorCardFilterFeedPreviewHighFidelityKeys.remove(eldestEntry.key)
        }
    }
}

private fun isColorCardFilterFeedPreviewMemoryHighFidelity(
    previewKey: String
): Boolean = synchronized(colorCardFilterFeedPreviewMemoryLock) {
    val bitmap = colorCardFilterFeedPreviewMemoryCache[previewKey]
    bitmap != null && !bitmap.isRecycled && colorCardFilterFeedPreviewHighFidelityKeys.contains(previewKey)
}

private fun isColorCardFilterFeedPreviewHighFidelityReady(
    context: Context,
    previewKey: String,
    bootstrapAssetsByKey: Map<String, ColorCardFilterFeedBootstrapPreviewAsset>
): Boolean {
    if (bootstrapAssetsByKey.containsKey(previewKey)) return true
    if (isColorCardFilterFeedPreviewMemoryHighFidelity(previewKey)) return true
    val cacheFile = colorCardFilterFeedPreviewDiskCacheFile(context, previewKey)
    return cacheFile.exists() && cacheFile.length() > 0L
}

private fun colorCardFilterFeedPreviewDiskCacheFile(
    context: Context,
    previewKey: String
): File {
    val digest = MessageDigest.getInstance("MD5")
        .digest("$COLOR_CARD_FILTER_FEED_PREVIEW_CACHE_VERSION:$previewKey".toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
    return File(
        File(context.cacheDir, COLOR_CARD_FILTER_FEED_PREVIEW_CACHE_DIR).apply { mkdirs() },
        "$digest.webp"
    )
}

private suspend fun loadColorCardFilterFeedPreviewBitmapsFromDisk(
    context: Context,
    requests: List<ColorCardFilterFeedPreviewRequest>
): Map<String, Bitmap> = withContext(Dispatchers.IO) {
    buildMap {
        requests.forEach { request ->
            val cacheFile = colorCardFilterFeedPreviewDiskCacheFile(context, request.previewKey)
            if (!cacheFile.exists() || cacheFile.length() <= 0L) return@forEach
            runCatching {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            }.getOrNull()
                ?.takeIf { !it.isRecycled }
                ?.let { decoded ->
                    val safeBitmap = if (decoded.config == Bitmap.Config.ARGB_8888) {
                        decoded
                    } else {
                        decoded.copy(Bitmap.Config.ARGB_8888, false).also {
                            if (it !== decoded) decoded.recycle()
                        }
                    }
                    put(request.previewKey, safeBitmap)
                }
        }
    }
}

private suspend fun loadColorCardFilterFeedBootstrapPreviewBitmaps(
    context: Context,
    requests: List<ColorCardFilterFeedPreviewRequest>,
    bootstrapAssetsByKey: Map<String, ColorCardFilterFeedBootstrapPreviewAsset>
): Map<String, Bitmap> = withContext(Dispatchers.IO) {
    buildMap {
        requests.forEach { request ->
            val bootstrapAsset = bootstrapAssetsByKey[request.previewKey] ?: return@forEach
            runCatching {
                context.assets.open(bootstrapAsset.assetName).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
                ?.takeIf { !it.isRecycled }
                ?.let { decoded ->
                    val safeBitmap = if (decoded.config == Bitmap.Config.ARGB_8888) {
                        decoded
                    } else {
                        decoded.copy(Bitmap.Config.ARGB_8888, false).also {
                            if (it !== decoded) decoded.recycle()
                        }
                    }
                    put(request.previewKey, safeBitmap)
                }
        }
    }
}

private suspend fun persistColorCardFilterFeedPreviewBitmaps(
    context: Context,
    generated: Map<String, Bitmap>
) = withContext(Dispatchers.IO) {
    generated.forEach { (previewKey, bitmap) ->
        if (bitmap.isRecycled) return@forEach
        val cacheFile = colorCardFilterFeedPreviewDiskCacheFile(context, previewKey)
        runCatching {
            FileOutputStream(cacheFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 94, output)
            }
            cacheFile.setLastModified(System.currentTimeMillis())
        }
    }
    pruneColorCardFilterFeedPreviewDiskCache(context)
}

private suspend fun buildColorCardFilterFeedApproxPreviewBitmaps(
    context: Context,
    requests: List<ColorCardFilterFeedPreviewRequest>
): Map<String, Bitmap> = coroutineScope {
    requests.map { request ->
        async(Dispatchers.Default) {
            request.previewKey to buildColorCardFilterFeedApproxPreviewBitmap(
                context = context,
                request = request
            )
        }
    }.awaitAll()
        .mapNotNull { (previewKey, bitmap) -> bitmap?.let { previewKey to it } }
        .toMap()
}

private suspend fun buildColorCardFilterFeedPreviewBitmaps(
    context: Context,
    requests: List<ColorCardFilterFeedPreviewRequest>
): Map<String, Bitmap> = coroutineScope {
    requests.map { request ->
        async(Dispatchers.Default) {
            request.previewKey to buildColorCardFilterFeedPreviewBitmap(
                context = context,
                request = request
            )
        }
    }.awaitAll()
        .mapNotNull { (previewKey, bitmap) -> bitmap?.let { previewKey to it } }
        .toMap()
}

private suspend fun buildColorCardFilterFeedApproxPreviewBitmap(
    context: Context,
    request: ColorCardFilterFeedPreviewRequest
): Bitmap? = withContext(Dispatchers.Default) {
    val sourceBitmap = decodeColorCardFilterFeedDemoBitmap(
        context = context,
        assetName = request.demoAssetName,
        maxSidePx = COLOR_CARD_FILTER_FEED_FAST_PREVIEW_MAX_SIDE
    ) ?: return@withContext null
    runCatching {
        renderColorCardTraceFeedApproxPreviewBitmap(
            sourceBitmap = sourceBitmap,
            candidate = request.candidate
        )
    }.getOrElse {
        sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
}

private suspend fun buildColorCardFilterFeedPreviewBitmap(
    context: Context,
    request: ColorCardFilterFeedPreviewRequest
): Bitmap? = withContext(Dispatchers.Default) {
    if (request.preferStylizedPreview) {
        buildColorCardFilterFeedStylizedPreviewBitmap(
            context = context,
            request = request
        )?.let { return@withContext it }
    }
    val originalBitmap = decodeColorCardFilterFeedDemoBitmap(
        context = context,
        assetName = request.demoAssetName
    ) ?: return@withContext null
    val filteredBitmap = runCatching {
        renderColorCardTraceFeedPreviewBitmap(
            context = context,
            sourceBitmap = originalBitmap,
            candidate = request.candidate
        )
    }.getOrNull()
    if (filteredBitmap != null) {
        if (filteredBitmap.config == Bitmap.Config.ARGB_8888) {
            filteredBitmap
        } else {
            filteredBitmap.copy(Bitmap.Config.ARGB_8888, false).also { copied ->
                if (copied !== filteredBitmap && !filteredBitmap.isRecycled) {
                    filteredBitmap.recycle()
                }
            }
        }
    } else {
        runCatching {
            renderColorCardTraceFeedApproxPreviewBitmap(
                sourceBitmap = originalBitmap,
                candidate = request.candidate
            )
        }.getOrElse {
            originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
    }
}

private suspend fun buildColorCardFilterFeedStylizedPreviewBitmap(
    context: Context,
    request: ColorCardFilterFeedPreviewRequest
): Bitmap? = withContext(Dispatchers.Default) {
    val sourceBitmap = decodeColorCardFilterFeedDemoBitmap(
        context = context,
        assetName = request.demoAssetName
    ) ?: return@withContext null
    when (request.candidate.kind) {
        ColorCardTraceFilterKind.HaldClut -> {
            applyColorCardFilterFeedPaletteTonePreview(
                sourceBitmap = sourceBitmap,
                previewColors = request.candidate.colors,
                paletteId = request.candidate.paletteId
            )
        }
        ColorCardTraceFilterKind.BlackMist -> {
            applyColorCardFilterFeedBlackMistPreview(sourceBitmap)
        }
        ColorCardTraceFilterKind.SelectiveColor -> {
            applyColorCardFilterFeedSelectiveColorPreview(sourceBitmap)
        }
        else -> null
    }
}

private fun applyColorCardFilterFeedPaletteTonePreview(
    sourceBitmap: Bitmap,
    previewColors: List<Int>,
    paletteId: String
): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val palette = previewColors.ifEmpty {
        listOf(0xFF202020.toInt(), 0xFF686868.toInt(), 0xFFB8B8B8.toInt(), 0xFFF0F0F0.toInt())
    }
    val loweredId = paletteId.lowercase()
    val contrast = when {
        loweredId.contains("tx400") || loweredId.contains("bleach") -> 1.34
        loweredId.contains("cine") || loweredId.contains("cinema") -> 1.20
        else -> 1.12
    }
    val saturationBoost = when {
        loweredId.contains("tx400") || loweredId.contains("converted") -> 0.58
        loweredId.contains("velvia") || loweredId.contains("ektar") -> 0.98
        else -> 0.84
    }

    sourcePixels.indices.forEach { index ->
        val argb = sourcePixels[index]
        val alpha = android.graphics.Color.alpha(argb)
        val red = android.graphics.Color.red(argb)
        val green = android.graphics.Color.green(argb)
        val blue = android.graphics.Color.blue(argb)
        val luma = ((red * 0.299) + (green * 0.587) + (blue * 0.114)) / 255.0
        val contrasted = ((((luma - 0.5) * contrast) + 0.5).coerceIn(0.0, 1.0))
        val scaledIndex = contrasted * (palette.size - 1).coerceAtLeast(0)
        val leftIndex = scaledIndex.toInt().coerceIn(0, palette.lastIndex)
        val rightIndex = (leftIndex + 1).coerceAtMost(palette.lastIndex)
        val mix = (scaledIndex - leftIndex).coerceIn(0.0, 1.0)
        val leftColor = palette[leftIndex]
        val rightColor = palette[rightIndex]
        val toneRed =
            (android.graphics.Color.red(leftColor) * (1.0 - mix)) + (android.graphics.Color.red(rightColor) * mix)
        val toneGreen =
            (android.graphics.Color.green(leftColor) * (1.0 - mix)) + (android.graphics.Color.green(rightColor) * mix)
        val toneBlue =
            (android.graphics.Color.blue(leftColor) * (1.0 - mix)) + (android.graphics.Color.blue(rightColor) * mix)
        val gray = luma * 255.0
        val boostedRed = gray + ((toneRed - gray) * saturationBoost)
        val boostedGreen = gray + ((toneGreen - gray) * saturationBoost)
        val boostedBlue = gray + ((toneBlue - gray) * saturationBoost)
        outputPixels[index] = android.graphics.Color.argb(
            alpha,
            boostedRed.toInt().coerceIn(0, 255),
            boostedGreen.toInt().coerceIn(0, 255),
            boostedBlue.toInt().coerceIn(0, 255)
        )
    }

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(outputPixels, 0, width, 0, 0, width, height)
    }
}

private fun applyColorCardFilterFeedSelectiveColorPreview(
    sourceBitmap: Bitmap
): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    sourcePixels.indices.forEach { index ->
        val argb = sourcePixels[index]
        val alpha = android.graphics.Color.alpha(argb)
        val red = android.graphics.Color.red(argb)
        val green = android.graphics.Color.green(argb)
        val blue = android.graphics.Color.blue(argb)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(red, green, blue, hsv)
        val hue = hsv[0] / 360f
        val sat = hsv[1].toDouble()
        val hueDistance = min(abs(hue - 0f).toDouble(), 1.0 - abs(hue - 0f).toDouble())
        val accentMask = if (hueDistance <= 0.12 && sat >= 0.08) 1.0 else 0.0
        val gray = ((red * 0.299) + (green * 0.587) + (blue * 0.114)).toInt().coerceIn(0, 255)
        outputPixels[index] = android.graphics.Color.argb(
            alpha,
            ((gray * (1.0 - accentMask)) + ((red * 1.10) * accentMask)).toInt().coerceIn(0, 255),
            ((gray * (1.0 - accentMask)) + ((green * 0.92) * accentMask)).toInt().coerceIn(0, 255),
            ((gray * (1.0 - accentMask)) + ((blue * 0.92) * accentMask)).toInt().coerceIn(0, 255)
        )
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(outputPixels, 0, width, 0, 0, width, height)
    }
}

private fun applyColorCardFilterFeedBlackMistPreview(
    sourceBitmap: Bitmap
): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val tintRed = 246.0
    val tintGreen = 217.0
    val tintBlue = 238.0

    fun pixelAt(x: Int, y: Int): Int {
        val safeX = x.coerceIn(0, width - 1)
        val safeY = y.coerceIn(0, height - 1)
        return sourcePixels[(safeY * width) + safeX]
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = (y * width) + x
            val argb = sourcePixels[index]
            val alpha = android.graphics.Color.alpha(argb)
            val red = android.graphics.Color.red(argb)
            val green = android.graphics.Color.green(argb)
            val blue = android.graphics.Color.blue(argb)
            var blurRed = 0.0
            var blurGreen = 0.0
            var blurBlue = 0.0
            var weightSum = 0.0
            for (dy in -2..2) {
                for (dx in -2..2) {
                    val weight = when {
                        dx == 0 && dy == 0 -> 4.0
                        abs(dx) + abs(dy) <= 2 -> 1.5
                        else -> 0.75
                    }
                    val sample = pixelAt(x + dx, y + dy)
                    blurRed += android.graphics.Color.red(sample) * weight
                    blurGreen += android.graphics.Color.green(sample) * weight
                    blurBlue += android.graphics.Color.blue(sample) * weight
                    weightSum += weight
                }
            }
            blurRed /= max(0.0001, weightSum)
            blurGreen /= max(0.0001, weightSum)
            blurBlue /= max(0.0001, weightSum)
            val luma = ((red * 0.299) + (green * 0.587) + (blue * 0.114)) / 255.0
            val highlight = ((luma - 0.56) / 0.40).coerceIn(0.0, 1.0)
            val softnessMix = 0.22
            val bloomAmount = highlight * 0.32
            val veilLift = (1.0 - luma) * 0.04 * 255.0
            val glowRed = (blurRed * 0.70) + (tintRed * 0.30)
            val glowGreen = (blurGreen * 0.76) + (tintGreen * 0.24)
            val glowBlue = (blurBlue * 0.64) + (tintBlue * 0.36)
            outputPixels[index] = android.graphics.Color.argb(
                alpha,
                ((red * (1.0 - softnessMix)) + (blurRed * softnessMix) + (glowRed * bloomAmount) + veilLift)
                    .toInt().coerceIn(0, 255),
                ((green * (1.0 - softnessMix)) + (blurGreen * softnessMix) + (glowGreen * bloomAmount) + veilLift)
                    .toInt().coerceIn(0, 255),
                ((blue * (1.0 - softnessMix)) + (blurBlue * softnessMix) + (glowBlue * bloomAmount) + veilLift)
                    .toInt().coerceIn(0, 255)
            )
        }
    }

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(outputPixels, 0, width, 0, 0, width, height)
    }
}

internal suspend fun decodeColorCardFilterFeedDemoBitmap(
    context: Context,
    assetName: String,
    maxSidePx: Int = COLOR_CARD_FILTER_FEED_RUNTIME_PREVIEW_MAX_SIDE
): Bitmap? = withContext(Dispatchers.IO) {
    val cacheKey = "$assetName@$maxSidePx"
    snapshotColorCardFilterFeedDemoBitmapMemoryCache(cacheKey)
        ?.let { return@withContext it }

    val assetBytes = runCatching {
        context.assets.open(assetName).use { input -> input.readBytes() }
    }.getOrNull() ?: return@withContext null

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(assetBytes, 0, assetBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    val sampleSize = buildColorCardFilterFeedSampleSize(
        width = bounds.outWidth,
        height = bounds.outHeight,
        maxSide = maxSidePx
    )
    runCatching {
        BitmapFactory.decodeByteArray(
            assetBytes,
            0,
            assetBytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }.getOrNull()?.let { bitmap ->
        val safeBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false).also {
                if (it !== bitmap) {
                    bitmap.recycle()
                }
            }
        }
        cacheColorCardFilterFeedDemoBitmap(cacheKey, safeBitmap)
        safeBitmap
    }
}

private fun snapshotColorCardFilterFeedDemoBitmapMemoryCache(
    cacheKey: String
): Bitmap? = synchronized(colorCardFilterFeedDemoBitmapMemoryLock) {
    colorCardFilterFeedDemoBitmapMemoryCache[cacheKey]
        ?.takeIf { !it.isRecycled }
}

private fun cacheColorCardFilterFeedDemoBitmap(
    cacheKey: String,
    bitmap: Bitmap
) {
    synchronized(colorCardFilterFeedDemoBitmapMemoryLock) {
        colorCardFilterFeedDemoBitmapMemoryCache[cacheKey] = bitmap
        while (colorCardFilterFeedDemoBitmapMemoryCache.size > COLOR_CARD_FILTER_FEED_DEMO_BITMAP_MEMORY_MAX_ENTRIES) {
            val eldestEntry = colorCardFilterFeedDemoBitmapMemoryCache.entries.firstOrNull() ?: break
            colorCardFilterFeedDemoBitmapMemoryCache.remove(eldestEntry.key)
        }
    }
}

private fun buildColorCardFilterFeedSampleSize(
    width: Int,
    height: Int,
    maxSide: Int
): Int {
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val safeMaxSide = maxSide.coerceAtLeast(1)
    var sampleSize = 1
    var currentMaxSide = maxOf(safeWidth, safeHeight)
    while (currentMaxSide > safeMaxSide * 2) {
        sampleSize *= 2
        currentMaxSide /= 2
    }
    return sampleSize.coerceAtLeast(1)
}

private fun pruneColorCardFilterFeedPreviewDiskCache(context: Context) {
    val cacheDir = File(context.cacheDir, COLOR_CARD_FILTER_FEED_PREVIEW_CACHE_DIR)
    val files = cacheDir.listFiles()
        ?.filter { it.isFile }
        ?.sortedByDescending { it.lastModified() }
        ?: return
    if (files.size <= COLOR_CARD_FILTER_FEED_PREVIEW_DISK_CACHE_MAX_FILES) return
    files.drop(COLOR_CARD_FILTER_FEED_PREVIEW_DISK_CACHE_MAX_FILES).forEach { file ->
        runCatching { file.delete() }
    }
}
