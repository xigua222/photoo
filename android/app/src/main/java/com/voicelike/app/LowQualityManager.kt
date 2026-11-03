package com.voicelike.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

object LowQualityManager {
    private const val FOUND_ITEMS_EMIT_INTERVAL_MS = 240L
    private const val FOUND_ITEMS_EMIT_BATCH_SIZE = 3
    private const val ENHANCED_QUALITY_METRICS_VERSION = 2

    private val screenshotKeywords = listOf(
        "screenshot",
        "screen_shot",
        "screen-shot",
        "screen shot",
        "screen capture",
        "screencap",
        "截屏",
        "截图",
        "屏幕截图",
        "屏幕截屏",
        "螢幕截圖",
        "スクリーンショット",
        "스크린샷"
    )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress

    private val _foundItems = MutableStateFlow<List<Pair<MediaItem, String>>>(emptyList())
    val foundItems: StateFlow<List<Pair<MediaItem, String>>> = _foundItems

    private val _newlyScannedIds = MutableStateFlow<Set<Long>>(emptySet())
    val newlyScannedIds: StateFlow<Set<Long>> = _newlyScannedIds

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pauseRequests = AtomicInteger(0)

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var currentMediaSnapshot: List<MediaItem> = emptyList()
    @Volatile
    private var currentCatalogSignature: Long = 0L

    private val currentIgnoredIds = java.util.Collections.synchronizedSet(HashSet<Long>())
    private val rawReasonById = LinkedHashMap<Long, String>()
    private val rawScannedIds = HashSet<Long>()

    fun requestPause() {
        pauseRequests.incrementAndGet()
        _isPaused.value = pauseRequests.get() > 0
    }

    fun releasePause() {
        if (pauseRequests.get() > 0) {
            pauseRequests.decrementAndGet()
        }
        _isPaused.value = pauseRequests.get() > 0
        if (_isPaused.value) return
        publishDisplayState()
    }

    fun setDisplayIgnoredIds(ids: Set<Long>) {
        synchronized(currentIgnoredIds) {
            currentIgnoredIds.clear()
            currentIgnoredIds.addAll(ids)
        }
        publishDisplayState()
    }

    fun startScan(
        context: Context,
        items: List<MediaItem>,
        ignoredIds: Set<Long>,
        catalogSignature: Long = 0L
    ) {
        if (_isScanning.value) return

        appContext = context.applicationContext
        scanJob?.cancel()
        MediaAnalysisManager.init(context)
        currentMediaSnapshot = items.filter(::shouldAnalyzeAsStandardPhoto)
        currentCatalogSignature = catalogSignature.takeIf { it != 0L } ?: buildMediaScanSignature(currentMediaSnapshot, 0)
        setDisplayIgnoredIds(ignoredIds)

        scanJob = scope.launch {
            _isScanning.value = true
            try {
                val hydrated = loadHydratedState(
                    context = context,
                    items = currentMediaSnapshot
                )
                replaceRawState(hydrated.reasonById, hydrated.scannedIds)
                publishDisplayState()
                _newlyScannedIds.value = emptySet()

                val eligibleItems = currentMediaSnapshot
                val total = eligibleItems.size
                if (total == 0) {
                    _scanProgress.value = 1f
                    _isScanning.value = false
                    return@launch
                }

                var completedCount = eligibleItems.count { rawScannedIds.contains(it.id) }
                _scanProgress.value = (completedCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)

                val candidates = eligibleItems.filter { !rawScannedIds.contains(it.id) }
                if (candidates.isEmpty()) {
                    _scanProgress.value = 1f
                    _isScanning.value = false
                    persistSnapshot(isFinal = true)
                    return@launch
                }

                val newlyScanned = mutableSetOf<Long>()
                var stableRounds = 0
                var lastPublishedFoundSize = _foundItems.value.size
                var lastFoundPublishAtMs = System.currentTimeMillis()

                fun publishFoundState(force: Boolean = false) {
                    val now = System.currentTimeMillis()
                    val shouldPublish =
                        force ||
                            (_foundItems.value.size - lastPublishedFoundSize) >= FOUND_ITEMS_EMIT_BATCH_SIZE ||
                            (now - lastFoundPublishAtMs) >= FOUND_ITEMS_EMIT_INTERVAL_MS
                    if (!shouldPublish) return
                    publishDisplayState()
                    _newlyScannedIds.value = newlyScanned.toSet()
                    lastPublishedFoundSize = _foundItems.value.size
                    lastFoundPublishAtMs = now
                }

                while (isActive) {
                    while (isActive && (pauseRequests.get() > 0 || AppPerformanceGovernor.shouldPauseNonCriticalWork())) {
                        delay(AppPerformanceGovernor.scaledDelay(500))
                    }

                    val completedBeforeRound = completedCount

                    for (item in candidates) {
                        if (rawScannedIds.contains(item.id)) {
                            continue
                        }

                        val staticReason = checkStaticQuality(item)
                        if (staticReason != null) {
                            rawReasonById[item.id] = staticReason
                            rawScannedIds.add(item.id)
                            newlyScanned.add(item.id)
                            completedCount++
                            publishFoundState()
                            continue
                        }

                        val analysis = MediaAnalysisManager.getAnalysis(item.id)
                        if (analysis != null) {
                            checkAnalysisQuality(analysis, item)?.let { reason ->
                                rawReasonById[item.id] = reason
                            }
                            rawScannedIds.add(item.id)
                            newlyScanned.add(item.id)
                            completedCount++
                            publishFoundState()
                        }
                    }

                    val previousRoundProgress = _scanProgress.value
                    val progress = (completedCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    _scanProgress.value = progress

                    if (completedCount >= total) break

                    if (progress <= previousRoundProgress + 0.0001f && completedCount == completedBeforeRound) {
                        stableRounds = (stableRounds + 1).coerceAtMost(8)
                    } else {
                        stableRounds = 0
                    }

                    if (!MediaAnalysisManager.isScanning.value && completedCount < total) {
                        candidates.forEach { item ->
                            if (!rawScannedIds.contains(item.id) && MediaAnalysisManager.isPhotoSettled(item.id)) {
                                rawScannedIds.add(item.id)
                                newlyScanned.add(item.id)
                                completedCount++
                            }
                        }
                        _scanProgress.value = (completedCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        break
                    }

                    val basePoll = 500L + (stableRounds * 200L)
                    delay(AppPerformanceGovernor.scaledDelay(basePoll.coerceAtMost(2200L)))
                }

                publishFoundState(force = true)
                _scanProgress.value = (completedCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                persistSnapshot(isFinal = true)
            } catch (_: OutOfMemoryError) {
                MemorySafetyMode.activateOomFallback(context)
                AppPerformanceGovernor.onCaughtOutOfMemory()
            } catch (_: Exception) {
            } finally {
                _isScanning.value = false
            }
        }
    }

    suspend fun hydrateFromCache(
        items: List<MediaItem>,
        ignoredIds: Set<Long>,
        catalogSignature: Long = 0L,
        context: Context? = appContext
    ) {
        if (_isScanning.value) return
        currentMediaSnapshot = items.filter(::shouldAnalyzeAsStandardPhoto)
        currentCatalogSignature = catalogSignature.takeIf { it != 0L } ?: buildMediaScanSignature(currentMediaSnapshot, 0)
        setDisplayIgnoredIds(ignoredIds)

        val hydrated = loadHydratedState(
            context = context,
            items = currentMediaSnapshot
        )
        replaceRawState(hydrated.reasonById, hydrated.scannedIds)
        publishDisplayState()
        _newlyScannedIds.value = emptySet()
        _scanProgress.value = if (hydrated.isFinal) 1f else calculateProgress(currentMediaSnapshot)
        _isScanning.value = false
    }

    fun stopScan() {
        if (_isScanning.value) {
            scanJob?.cancel()
            _isScanning.value = false
        }
    }

    fun flushSnapshot() {
        persistSnapshot(isFinal = true)
    }

    fun resetSession(clearPersisted: Boolean = false) {
        stopScan()
        pauseRequests.set(0)
        _isPaused.value = false
        _scanProgress.value = 0f
        _foundItems.value = emptyList()
        _newlyScannedIds.value = emptySet()
        currentMediaSnapshot = emptyList()
        currentCatalogSignature = 0L
        synchronized(currentIgnoredIds) {
            currentIgnoredIds.clear()
        }
        replaceRawState(emptyMap(), emptySet())
        if (clearPersisted) {
            appContext?.let {
                LowQualityScanSnapshotStore.clear(it)
            }
        }
    }

    fun removeResult(id: Long) {
        _foundItems.value = _foundItems.value.filter { it.first.id != id }
    }

    fun restoreResult(item: MediaItem, reason: String) {
        val current = _foundItems.value.filter { it.first.id != item.id }
        _foundItems.value = listOf(item to reason) + current
    }

    fun markCurrentStrategyApplied(context: Context) {
        appContext = context.applicationContext
        LowQualityStrategyGate.markCurrentStrategyApplied(appContext!!)
    }

    private fun checkStaticQuality(item: MediaItem): String? {
        if (usesCurrentStrategy() && isLikelyScreenshot(item)) return null
        if (item.size < 50 * 1024) return "smallFile"

        if (item.width > 0 && item.height > 0) {
            val pixels = item.width * item.height
            if (pixels < 500_000) return "lowResolution"
        }
        return null
    }

    private fun checkAnalysisQuality(analysis: MediaAnalysisManager.AnalysisResult, item: MediaItem): String? {
        val useCurrentStrategy =
            usesCurrentStrategy() &&
                analysis.qualityMetricsVersion >= ENHANCED_QUALITY_METRICS_VERSION

        if (useCurrentStrategy) {
            if (isLikelyScreenshot(item, analysis)) return null
            if (analysis.solidColorStdDev < 5.0) return "solidColor"
            classifyExposureWithCurrentStrategy(analysis)?.let { return it }
            if (analysis.blurVariance < 100) return "blur"
            return null
        }

        if (analysis.solidColorStdDev < 5.0) return "solidColor"

        val isLegacyScreenshot = item.displayName.contains("Screenshot", ignoreCase = true) ||
            item.bucketName.contains("Screenshot", ignoreCase = true)

        if (!isLegacyScreenshot) {
            if (analysis.exposureAvg < 40) return "underexposed"
            if (analysis.exposureAvg > 220) return "overexposed"
            if (analysis.blurVariance < 100) return "blur"
        }

        return null
    }

    private data class HydratedState(
        val reasonById: Map<Long, String>,
        val scannedIds: Set<Long>,
        val isFinal: Boolean
    )

    private fun loadHydratedState(
        context: Context?,
        items: List<MediaItem>
    ): HydratedState {
        val effectiveItems = items.filter(::shouldAnalyzeAsStandardPhoto)
        if (effectiveItems.isEmpty()) {
            return HydratedState(emptyMap(), emptySet(), isFinal = true)
        }

        val validIds = effectiveItems.mapTo(HashSet(effectiveItems.size)) { it.id }
        val snapshot = context?.let { LowQualityScanSnapshotStore.load(it) }
        if (snapshot != null && snapshot.signature == currentCatalogSignature && snapshot.isFinal) {
            val filteredReasons = snapshot.reasonById.filterKeys { validIds.contains(it) }
            val filteredScanned = snapshot.scannedIds.filterTo(HashSet()) { validIds.contains(it) }
            return HydratedState(filteredReasons, filteredScanned, isFinal = true)
        }
        return HydratedState(emptyMap(), emptySet(), isFinal = false)
    }

    private fun replaceRawState(reasonById: Map<Long, String>, scannedIds: Set<Long>) {
        rawReasonById.clear()
        rawReasonById.putAll(reasonById)
        rawScannedIds.clear()
        rawScannedIds.addAll(scannedIds)
    }

    private fun calculateProgress(items: List<MediaItem>): Float {
        if (items.isEmpty()) return 1f
        return (items.count { rawScannedIds.contains(it.id) }.toFloat() / items.size.toFloat()).coerceIn(0f, 1f)
    }

    private fun publishDisplayState() {
        val ignoredIds = synchronized(currentIgnoredIds) { HashSet(currentIgnoredIds) }
        val visible = currentMediaSnapshot
            .asSequence()
            .mapNotNull { item ->
                val reason = rawReasonById[item.id] ?: return@mapNotNull null
                if (ignoredIds.contains(item.id)) return@mapNotNull null
                item to reason
            }
            .toList()
        _foundItems.value = visible
    }

    private fun persistSnapshot(isFinal: Boolean) {
        val context = appContext ?: return
        if (currentCatalogSignature == 0L) return
        if (!isFinal) return
        val scanSettled = _scanProgress.value >= 0.999f
        if (!scanSettled) return
        val snapshot = LowQualityScanSnapshot(
            signature = currentCatalogSignature,
            reasonById = LinkedHashMap(rawReasonById),
            scannedIds = LinkedHashSet(rawScannedIds),
            updatedAtMs = System.currentTimeMillis(),
            isFinal = true,
            version = LowQualityStrategyGate.resolvedStrategyVersion(context)
        )
        LowQualityScanSnapshotStore.save(context, snapshot)
    }

    private fun usesCurrentStrategy(): Boolean {
        return LowQualityStrategyGate.resolvedStrategyVersion(appContext) >=
            LowQualityStrategyGate.CURRENT_STRATEGY_VERSION
    }

    private fun isLikelyScreenshot(
        item: MediaItem,
        analysis: MediaAnalysisManager.AnalysisResult? = null
    ): Boolean {
        val metadata = buildString {
            append(item.displayName)
            append(' ')
            append(item.bucketName)
            append(' ')
            append(item.path.orEmpty())
        }.lowercase(Locale.ROOT)

        if (screenshotKeywords.any(metadata::contains)) return true
        if (analysis == null) return false

        val isPngLike = item.mimeType.contains("png", ignoreCase = true) ||
            item.displayName.endsWith(".png", ignoreCase = true)
        val looksLikeUiCapture =
            isPngLike &&
                analysis.highlightClipRatio >= 0.55f &&
                analysis.shadowClipRatio >= 0.004f &&
                analysis.solidColorStdDev >= 18.0 &&
                analysis.blurVariance >= 120.0
        return looksLikeUiCapture
    }

    private fun classifyExposureWithCurrentStrategy(
        analysis: MediaAnalysisManager.AnalysisResult
    ): String? {
        val average = analysis.exposureAvg
        val contrast = analysis.solidColorStdDev
        val shadowRatio = analysis.shadowClipRatio
        val highlightRatio = analysis.highlightClipRatio

        val underexposed =
            (average <= 18 && shadowRatio >= 0.82f && highlightRatio <= 0.03f && contrast <= 42.0) ||
                (average <= 34 && shadowRatio >= 0.74f && highlightRatio <= 0.04f && contrast <= 34.0) ||
                (average <= 44 && shadowRatio >= 0.88f && highlightRatio <= 0.02f && contrast <= 24.0)
        if (underexposed) return "underexposed"

        val overexposed =
            (average >= 244 && highlightRatio >= 0.82f && shadowRatio <= 0.03f && contrast <= 42.0) ||
                (average >= 228 && highlightRatio >= 0.74f && shadowRatio <= 0.04f && contrast <= 34.0) ||
                (average >= 214 && highlightRatio >= 0.88f && shadowRatio <= 0.02f && contrast <= 24.0)
        if (overexposed) return "overexposed"

        return null
    }
}
