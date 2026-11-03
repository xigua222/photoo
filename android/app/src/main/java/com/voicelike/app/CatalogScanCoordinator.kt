package com.voicelike.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GlobalCatalogScanState(
    val signature: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val overallProgress: Float = 0f,
    val coreProgress: Float = 0f,
    val insightProgress: Float = 0f,
    val similarProgress: Float = 0f,
    val lowQualityProgress: Float = 0f,
    val completedStages: Set<String> = emptySet()
)

object CatalogScanCoordinator {
    private const val STAGE_CORE = "core"
    private const val STAGE_INSIGHT = "insight"
    private const val STAGE_SIMILAR = "similar"
    private const val STAGE_LOW_QUALITY = "low_quality"
    private const val CORE_STAGE_WEIGHT = 0.72f
    private const val INSIGHT_STAGE_WEIGHT = 0.16f
    private const val SIMILAR_STAGE_WEIGHT = 0.07f
    private const val LOW_QUALITY_STAGE_WEIGHT = 0.05f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(GlobalCatalogScanState())
    val state = _state.asStateFlow()

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var boundSignature: Long = 0L
    @Volatile
    private var boundMedia: List<MediaItem> = emptyList()
    @Volatile
    private var boundIgnoredIds: Set<Long> = emptySet()
    @Volatile
    private var hydratedSignature: Long = 0L

    private var stateJob: Job? = null

    fun bindCatalog(
        context: Context,
        signature: Long,
        media: List<MediaItem>,
        displayIgnoredIds: Set<Long> = emptySet()
    ) {
        appContext = context.applicationContext
        boundSignature = signature
        boundMedia = media.toList()
        boundIgnoredIds = HashSet(displayIgnoredIds)
        SimilarPhotoManager.setDisplayIgnoredIds(boundIgnoredIds)
        LowQualityManager.setDisplayIgnoredIds(boundIgnoredIds)
        ensureStateCollector()
    }

    fun ensureBackground() {
        ensureBoundSession(forceStart = true)
    }

    fun ensureForeground() {
        ensureBoundSession(forceStart = true)
    }

    fun pause() {
        MediaAnalysisManager.requestPause()
        SimilarPhotoManager.requestPause()
        LowQualityManager.requestPause()
        publishState()
    }

    fun resume() {
        MediaAnalysisManager.releasePause()
        SimilarPhotoManager.releasePause()
        LowQualityManager.releasePause()
        ensureBoundSession(forceStart = true)
        publishState()
    }

    fun rescan(displayIgnoredIdsOverride: Set<Long>? = null) {
        val context = appContext ?: return
        val signature = boundSignature
        val media = boundMedia.toList()
        if (signature == 0L || media.isEmpty()) return
        val reboundIgnoredIds = if (displayIgnoredIdsOverride != null) {
            HashSet<Long>(displayIgnoredIdsOverride)
        } else {
            HashSet<Long>(boundIgnoredIds)
        }

        scope.launch(Dispatchers.IO) {
            LowQualityManager.markCurrentStrategyApplied(context)
            flush()
            MediaCatalogSnapshotCache.clear(context)
            SimilarPhotoManager.resetSession(clearPersisted = true)
            LowQualityManager.resetSession(clearPersisted = true)
            MediaAnalysisManager.clearPersistentScanCaches(context)
            MediaAnalysisManager.clearKeywordFailureBackoff()
            MediaInsightRepository.clearExifFor(context, media.map { it.id })
            MediaInsightRepository.flush(context)
            AppRuntimeLogStore.logInfo(
                context,
                "CatalogScan",
                "Manual rescan cleared scan caches and restarted session mediaCount=${media.size}"
            )
            hydratedSignature = 0L
            withContext(Dispatchers.Main) {
                bindCatalog(context, signature, media, reboundIgnoredIds)
                ensureBoundSession(forceStart = true)
            }
        }
    }

    fun flush() {
        MediaAnalysisManager.flushPersistentCaches()
        SimilarPhotoManager.flushSnapshot()
        LowQualityManager.flushSnapshot()
        appContext?.let { MediaInsightRepository.flush(it) }
    }

    private fun ensureStateCollector() {
        if (stateJob?.isActive == true) return
        stateJob = scope.launch {
            while (true) {
                publishState()
                delay(240L)
            }
        }
    }

    private fun ensureBoundSession(forceStart: Boolean) {
        val context = appContext ?: return
        val signature = boundSignature
        val media = boundMedia.toList()
        if (signature == 0L || media.isEmpty()) {
            publishState()
            return
        }

        scope.launch {
            if (hydratedSignature != signature) {
                hydrateBoundSession(context, media, signature, boundIgnoredIds)
                hydratedSignature = signature
            } else {
                SimilarPhotoManager.setDisplayIgnoredIds(boundIgnoredIds)
                LowQualityManager.setDisplayIgnoredIds(boundIgnoredIds)
            }

            if (!forceStart) {
                publishState()
                return@launch
            }

            MediaAnalysisManager.startScan(context, media)
            MediaAnalysisManager.startInsightScan(context, media)

            val standardPhotos = media.filter(::shouldAnalyzeAsStandardPhoto)
            SimilarPhotoManager.startScan(
                context = context,
                allMedia = standardPhotos,
                ignoredIds = boundIgnoredIds,
                catalogSignature = signature
            )
            LowQualityManager.startScan(
                context = context,
                items = standardPhotos,
                ignoredIds = boundIgnoredIds,
                catalogSignature = signature
            )
            publishState()
        }
    }

    private suspend fun hydrateBoundSession(
        context: Context,
        media: List<MediaItem>,
        signature: Long,
        ignoredIds: Set<Long>
    ) {
        MediaAnalysisManager.hydrateProgressFromCache(context, media)
        MediaAnalysisManager.hydrateInsightProgressFromCache(context, media)
        val standardPhotos = media.filter(::shouldAnalyzeAsStandardPhoto)
        SimilarPhotoManager.hydrateFromCache(
            context = context,
            allMedia = standardPhotos,
            ignoredIds = ignoredIds,
            catalogSignature = signature
        )
        LowQualityManager.hydrateFromCache(
            items = standardPhotos,
            ignoredIds = ignoredIds,
            catalogSignature = signature,
            context = context
        )
    }

    private fun publishState() {
        val coreProgress = MediaAnalysisManager.progress.value.coerceIn(0f, 1f)
        val insightProgress = MediaAnalysisManager.insightProgress.value.coerceIn(0f, 1f)
        val similarProgress = SimilarPhotoManager.scanProgress.value.coerceIn(0f, 1f)
        val lowQualityProgress = LowQualityManager.scanProgress.value.coerceIn(0f, 1f)
        val isPaused =
            MediaAnalysisManager.isPaused.value ||
                SimilarPhotoManager.isPaused.value ||
                LowQualityManager.isPaused.value
        val completedStages = LinkedHashSet<String>(4).apply {
            if (coreProgress >= 0.999f && !MediaAnalysisManager.isScanning.value) add(STAGE_CORE)
            if (insightProgress >= 0.999f && !MediaAnalysisManager.isInsightScanning.value) add(STAGE_INSIGHT)
            if (similarProgress >= 0.999f && !SimilarPhotoManager.isScanning.value) add(STAGE_SIMILAR)
            if (lowQualityProgress >= 0.999f && !LowQualityManager.isScanning.value) add(STAGE_LOW_QUALITY)
        }
        val running =
            MediaAnalysisManager.isScanning.value ||
                MediaAnalysisManager.isInsightScanning.value ||
                SimilarPhotoManager.isScanning.value ||
                LowQualityManager.isScanning.value
        val overallProgress =
            (
                (coreProgress * CORE_STAGE_WEIGHT) +
                    (insightProgress * INSIGHT_STAGE_WEIGHT) +
                    (similarProgress * SIMILAR_STAGE_WEIGHT) +
                    (lowQualityProgress * LOW_QUALITY_STAGE_WEIGHT)
                ).coerceIn(0f, 1f)
        _state.value = GlobalCatalogScanState(
            signature = boundSignature,
            isRunning = running,
            isPaused = isPaused,
            overallProgress = overallProgress,
            coreProgress = coreProgress,
            insightProgress = insightProgress,
            similarProgress = similarProgress,
            lowQualityProgress = lowQualityProgress,
            completedStages = completedStages
        )
    }
}
