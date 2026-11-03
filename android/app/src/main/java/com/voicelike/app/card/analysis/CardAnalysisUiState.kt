package com.voicelike.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.voicelike.app.smart.SmartAiRoastProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Stable
class CardAnalysisUiState internal constructor(
    initialItem: MediaItem,
    initialLabels: List<String>,
    initialAnalyzedLang: String?
) {
    var currentItem by mutableStateOf(initialItem)
        internal set

    var mlLabels by mutableStateOf(initialLabels)
        internal set

    internal var analyzedLang by mutableStateOf(initialAnalyzedLang)
    internal var exifLoaded by mutableStateOf(false)
    internal var aiOverlayText by mutableStateOf<String?>(null)
    internal var aiOverlayVisible by mutableStateOf(false)

    fun clearAiOverlay() {
        aiOverlayVisible = false
        aiOverlayText = null
    }
}

@Stable
data class CardAnalysisPresentation(
    val uiState: CardAnalysisUiState,
    val currentItem: MediaItem,
    val mlLabels: List<String>
)

private fun isResolvedCamera(camera: String?): Boolean {
    return !camera.isNullOrBlank() && camera != "--"
}

private fun isResolvedLocation(location: String?): Boolean {
    return !location.isNullOrBlank() && !location.equals("Unknown Location", ignoreCase = true)
}

private fun isResolvedDetailValue(value: String?): Boolean {
    return !value.isNullOrBlank() && value != "--"
}

private fun shouldLoadPhotoSourceMetadata(item: MediaItem, isExpanded: Boolean): Boolean {
    if (!isExpanded) {
        return !isResolvedLocation(item.location)
    }
    return !isResolvedCamera(item.camera) ||
        !isResolvedLocation(item.location) ||
        !isResolvedDetailValue(item.iso) ||
        !isResolvedDetailValue(item.shutter) ||
        !isResolvedDetailValue(item.focalLength) ||
        !isResolvedDetailValue(item.aperture)
}

private fun shouldLoadVideoSourceMetadata(item: MediaItem, isExpanded: Boolean): Boolean {
    if (!isExpanded) {
        return !isResolvedLocation(item.location)
    }
    return !isResolvedLocation(item.location) ||
        !isResolvedDetailValue(item.camera) ||
        !isResolvedDetailValue(item.iso) ||
        !isResolvedDetailValue(item.shutter) ||
        !isResolvedDetailValue(item.focalLength) ||
        !isResolvedDetailValue(item.aperture)
}

private fun shouldLoadSourceMetadata(item: MediaItem, isExpanded: Boolean): Boolean {
    return when (item.type) {
        "photo" -> shouldLoadPhotoSourceMetadata(item, isExpanded)
        "video" -> shouldLoadVideoSourceMetadata(item, isExpanded)
        else -> false
    }
}

private fun shouldResolveMotionPhoto(item: MediaItem): Boolean {
    return item.type == "photo" && !item.isLivePhoto && !item.isMotionPhoto
}

private fun mergeCachedInsightIntoItem(
    item: MediaItem,
    cachedInsight: MediaInsightSnapshot?
): MediaItem {
    if (cachedInsight == null) return item
    return when (item.type) {
        "photo" -> item.copy(
            camera = cachedInsight.camera.takeIf(::isResolvedCamera) ?: item.camera,
            location = cachedInsight.location.takeIf(::isResolvedLocation) ?: item.location
        )
        "video" -> item.copy(
            location = cachedInsight.location.takeIf(::isResolvedLocation) ?: item.location
        )
        else -> item
    }
}

private fun resolveDisplayLabels(
    uiState: CardAnalysisUiState,
    trustedCachedLabelsForCurrentLang: List<String>,
    fallbackCachedLabels: List<String>,
    currentLang: String
): List<String> {
    return if (uiState.mlLabels.isNotEmpty() && uiState.analyzedLang == currentLang) {
        uiState.mlLabels
    } else if (trustedCachedLabelsForCurrentLang.isNotEmpty()) {
        trustedCachedLabelsForCurrentLang
    } else {
        fallbackCachedLabels
    }
}

@Composable
private fun rememberObservedInsightVersion(mediaId: Long, enabled: Boolean): Long {
    val flow = remember(mediaId, enabled) {
        if (enabled) MediaInsightRepository.acquireVersionFor(mediaId) else null
    }
    DisposableEffect(mediaId, flow) {
        onDispose {
            if (flow != null) {
                MediaInsightRepository.releaseVersionObserver(mediaId)
            }
        }
    }
    val version by flow?.collectAsState() ?: rememberUpdatedState(0L)
    return version
}

@Composable
fun rememberCardAnalysisUiState(
    context: Context,
    item: MediaItem,
    currentLang: String,
    shouldAnalyze: Boolean,
    deferHeavyCardWork: Boolean,
    prefetchLightweightOnly: Boolean = false,
    isTopCard: Boolean,
    isExpanded: Boolean,
    isCardDragging: Boolean,
    lensyEnabled: Boolean,
    onAiOverlayStateChanged: ((String?, Boolean) -> Unit)? = null
): CardAnalysisPresentation {
    val shouldObserveInsightChanges = shouldAnalyze || isExpanded || isTopCard
    val insightVersion = rememberObservedInsightVersion(
        mediaId = item.id,
        enabled = shouldObserveInsightChanges
    )
    val cachedInsight = remember(item.id, insightVersion, shouldObserveInsightChanges) {
        MediaInsightRepository.get(context, item.id)
    }
    val trustedCachedLabelsForCurrentLang = remember(item.id, currentLang, insightVersion) {
        if (MediaAnalysisManager.hasCachedKeywordsForLanguage(cachedInsight, item.id, currentLang)) {
            cachedInsight?.keywords.orEmpty()
        } else {
            emptyList()
        }
    }
    val fallbackCachedLabels = remember(item.id, insightVersion) {
        cachedInsight?.keywords.orEmpty()
    }
    val initialCardItem = remember(item, cachedInsight) {
        mergeCachedInsightIntoItem(item, cachedInsight)
    }
    val state = remember(item.id) {
        CardAnalysisUiState(
            initialItem = initialCardItem,
            initialLabels = trustedCachedLabelsForCurrentLang.ifEmpty { fallbackCachedLabels },
            initialAnalyzedLang = currentLang.takeIf { trustedCachedLabelsForCurrentLang.isNotEmpty() }
        )
    }
    val displayItem = remember(state.currentItem, cachedInsight) {
        mergeCachedInsightIntoItem(state.currentItem, cachedInsight)
    }
    val currentDisplayItem by rememberUpdatedState(displayItem)
    val displayMlLabels = remember(
        state.mlLabels,
        state.analyzedLang,
        trustedCachedLabelsForCurrentLang,
        fallbackCachedLabels,
        currentLang
    ) {
        resolveDisplayLabels(
            uiState = state,
            trustedCachedLabelsForCurrentLang = trustedCachedLabelsForCurrentLang,
            fallbackCachedLabels = fallbackCachedLabels,
            currentLang = currentLang
        )
    }
    val recentAiOverlayTexts = remember(item.id) { kotlin.collections.ArrayDeque<String>() }

    LaunchedEffect(item.id, currentLang, shouldAnalyze, insightVersion, trustedCachedLabelsForCurrentLang) {
        val cached = cachedInsight ?: return@LaunchedEffect
        if (item.type == "photo") {
            if (!cached.camera.isNullOrBlank() || !cached.location.isNullOrBlank()) {
                state.currentItem = state.currentItem.copy(
                    camera = cached.camera.takeIf(::isResolvedCamera) ?: state.currentItem.camera,
                    location = cached.location.takeIf(::isResolvedLocation) ?: state.currentItem.location
                )
            }
            if (trustedCachedLabelsForCurrentLang.isNotEmpty()) {
                state.mlLabels = trustedCachedLabelsForCurrentLang
                state.analyzedLang = currentLang
            }
        } else if (item.type == "video") {
            val resolvedLocation = cached.location.takeIf(::isResolvedLocation)
            if (resolvedLocation != null) {
                state.currentItem = state.currentItem.copy(
                    location = resolvedLocation
                )
            }
        }
    }

    LaunchedEffect(shouldAnalyze, item.id, currentLang, deferHeavyCardWork, displayMlLabels) {
        if (deferHeavyCardWork) return@LaunchedEffect
        if (shouldAnalyze && item.type == "photo") {
            if (displayMlLabels.isEmpty()) {
                val labels = MediaAnalysisManager.resolveKeywordsForUi(
                    context = context,
                    item = item,
                    languageCode = currentLang
                )
                if (labels.isNotEmpty()) {
                    state.mlLabels = labels
                    state.analyzedLang = currentLang
                }
            }
        }
    }

    LaunchedEffect(
        shouldAnalyze,
        item.id,
        deferHeavyCardWork,
        currentDisplayItem.location
    ) {
        if (deferHeavyCardWork || !shouldAnalyze) return@LaunchedEffect
        val currentLocation = currentDisplayItem.location
        val hasResolvedLocation =
            currentLocation.isNotBlank() &&
                !currentLocation.equals("Unknown Location", ignoreCase = true)
        if (hasResolvedLocation) return@LaunchedEffect

        val resolvedLocation = MediaAnalysisManager.temporaryResolveLocation(
            context = context,
            item = item
        )
        if (!resolvedLocation.isNullOrBlank() &&
            !resolvedLocation.equals("Unknown Location", ignoreCase = true)
        ) {
            state.currentItem = state.currentItem.copy(location = resolvedLocation)
        }
    }

    LaunchedEffect(
        item.id,
        shouldAnalyze,
        deferHeavyCardWork,
        prefetchLightweightOnly,
        currentDisplayItem.isLivePhoto,
        currentDisplayItem.isMotionPhoto
    ) {
        if (deferHeavyCardWork || prefetchLightweightOnly || !shouldAnalyze) return@LaunchedEffect

        val motionTarget = currentDisplayItem
        if (!shouldResolveMotionPhoto(motionTarget)) return@LaunchedEffect

        val motionInfo = withContext(Dispatchers.IO) {
            MotionPhotoHelper.detectAndPersist(context, motionTarget)
        }
        if (motionInfo.isMotionPhoto) {
            state.currentItem = state.currentItem.copy(
                isMotionPhoto = true,
                motionPhotoVideoOffset = motionInfo.videoOffset,
                motionPhotoVideoLength = motionInfo.videoLength
            )
        }
    }

    LaunchedEffect(item.id, isTopCard, isExpanded, isCardDragging, displayMlLabels, lensyEnabled) {
        val labelsForRoast = displayMlLabels
        if (!lensyEnabled || !isTopCard || isExpanded || isCardDragging || labelsForRoast.isEmpty()) {
            state.clearAiOverlay()
            return@LaunchedEffect
        }

        state.clearAiOverlay()
        delay(SmartAiRoastProvider.randomIdleDelayMs())

        if (!lensyEnabled || !isTopCard || isExpanded || isCardDragging || labelsForRoast.isEmpty()) {
            return@LaunchedEffect
        }

        val roastLabels = resolveCardRoastLabels(context, labelsForRoast)
        state.aiOverlayText = SmartAiRoastProvider.pick(roastLabels, recentAiOverlayTexts)
        state.aiOverlayVisible = true
    }

    LaunchedEffect(isTopCard, state.aiOverlayText, state.aiOverlayVisible) {
        if (!isTopCard) return@LaunchedEffect
        onAiOverlayStateChanged?.invoke(state.aiOverlayText, state.aiOverlayVisible)
    }

    LaunchedEffect(item.id, deferHeavyCardWork, prefetchLightweightOnly, shouldAnalyze, isExpanded, insightVersion) {
        if (deferHeavyCardWork || prefetchLightweightOnly || !shouldAnalyze || state.exifLoaded) return@LaunchedEffect
        if (!isExpanded) {
            delay(180L)
            if (deferHeavyCardWork || prefetchLightweightOnly || !shouldAnalyze || state.exifLoaded) return@LaunchedEffect
        }

        val sourceLoadTarget = currentDisplayItem
        if (!shouldLoadSourceMetadata(sourceLoadTarget, isExpanded)) return@LaunchedEffect

        if (item.type == "photo") {
            val resolvedItem = withContext(Dispatchers.IO) {
                loadPhotoCardMetadata(context, item)
            }
            if (resolvedItem != null) {
                state.currentItem = resolvedItem
                state.exifLoaded = true
            }
        } else if (item.type == "video") {
            val resolvedItem = withContext(Dispatchers.IO) {
                loadVideoCardMetadata(context, item)
            }
            if (resolvedItem != null) {
                state.currentItem = resolvedItem
            }
            state.exifLoaded = true
        }
    }

    return remember(state, displayItem, displayMlLabels) {
        CardAnalysisPresentation(
            uiState = state,
            currentItem = displayItem,
            mlLabels = displayMlLabels
        )
    }
}
