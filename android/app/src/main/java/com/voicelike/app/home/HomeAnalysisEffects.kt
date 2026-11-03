package com.voicelike.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private const val HOME_SCAN_KEEPALIVE_ACTIVE_POLL_MS = 1800L
private const val HOME_SCAN_KEEPALIVE_SCANNING_POLL_MS = 3200L
private const val HOME_SCAN_KEEPALIVE_PAUSED_POLL_MS = 6500L
private const val HOME_SCAN_KEEPALIVE_DEFERRED_POLL_MS = 2200L
private const val HOME_SCAN_KEEPALIVE_DONE_POLL_MS = 12000L
private const val HOME_SCAN_STARTUP_DEFER_MS = 900L
private const val HOME_SCAN_IDLE_BOOTSTRAP_DEFER_MS = 15000L
private const val HOME_ANALYSIS_COMPLETE_THRESHOLD = 0.999f

private fun hasPendingCatalogAnalysis(state: GlobalCatalogScanState): Boolean {
    return state.overallProgress < HOME_ANALYSIS_COMPLETE_THRESHOLD || state.isRunning
}

private fun isCatalogAnalysisRunning(state: GlobalCatalogScanState): Boolean {
    return state.isRunning
}

@Composable
fun CatalogBackgroundAnalysisEffects(
    context: Context,
    prefs: PreferencesManager,
    restoreCatalogMedia: List<MediaItem>,
    scanCatalogMedia: List<MediaItem>,
    displayIgnoredIds: Set<Long>,
    catalogVerified: Boolean = true,
    hasLoadedOnce: Boolean,
    loading: Boolean,
    isInitialLoad: Boolean,
    allowCatalogBackgroundWork: Boolean,
    allowCatalogBootstrap: Boolean,
    firstInteractiveAtMs: Long,
    catalogBootstrapDeferMs: Long = HOME_SCAN_STARTUP_DEFER_MS
) {
    val coordinatorState by CatalogScanCoordinator.state.collectAsState()
    var lastGlobalScanSignature by remember { mutableLongStateOf(0L) }
    var lastCompletedScanSignature by remember { mutableLongStateOf(0L) }
    val restoreCatalogSignature by produceState(initialValue = 0L, key1 = restoreCatalogMedia, key2 = catalogVerified) {
        if (!catalogVerified || restoreCatalogMedia.isEmpty()) {
            value = 0L
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            buildMediaScanSignature(restoreCatalogMedia, 0)
        }
    }
    val globalScanSignature by produceState(initialValue = 0L, key1 = scanCatalogMedia, key2 = catalogVerified) {
        if (!catalogVerified) {
            value = 0L
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            buildMediaScanSignature(scanCatalogMedia, 0)
        }
    }
    val activeCatalogMedia = if (scanCatalogMedia.isNotEmpty()) scanCatalogMedia else restoreCatalogMedia
    val activeCatalogSignature = if (globalScanSignature != 0L) globalScanSignature else restoreCatalogSignature

    val currentRestoreCatalogMedia by rememberUpdatedState(restoreCatalogMedia)
    val currentScanCatalogMedia by rememberUpdatedState(scanCatalogMedia)
    val currentActiveCatalogMedia by rememberUpdatedState(activeCatalogMedia)
    val currentHasLoadedOnce by rememberUpdatedState(hasLoadedOnce)
    val currentLoading by rememberUpdatedState(loading)
    val currentIsInitialLoad by rememberUpdatedState(isInitialLoad)
    val currentCatalogVerified by rememberUpdatedState(catalogVerified)
    val currentAllowCatalogBackgroundWork by rememberUpdatedState(allowCatalogBackgroundWork)
    val currentDisplayIgnoredIds by rememberUpdatedState(displayIgnoredIds)
    val currentCoordinatorState by rememberUpdatedState(coordinatorState)
    val completionSignature = activeCatalogSignature
    val currentSignatureFullyAnalyzed =
        completionSignature != 0L &&
            coordinatorState.overallProgress >= HOME_ANALYSIS_COMPLETE_THRESHOLD &&
            !coordinatorState.isRunning

    LaunchedEffect(completionSignature, currentSignatureFullyAnalyzed) {
        if (currentSignatureFullyAnalyzed) {
            lastCompletedScanSignature = completionSignature
        }
    }

    suspend fun bindGlobalCatalogAnalysis(media: List<MediaItem>, signature: Long) {
        if (!catalogVerified || media.isEmpty() || signature == 0L) return
        CatalogScanCoordinator.bindCatalog(
            context = context,
            signature = signature,
            media = media,
            displayIgnoredIds = currentDisplayIgnoredIds
        )
    }

    suspend fun startGlobalCatalogAnalysis(media: List<MediaItem>, signature: Long) {
        if (!catalogVerified || media.isEmpty() || signature == 0L) return
        bindGlobalCatalogAnalysis(media, signature)
        if (AppPerformanceGovernor.shouldPauseNonCriticalWork()) return
        if (coordinatorState.overallProgress >= HOME_ANALYSIS_COMPLETE_THRESHOLD && !coordinatorState.isRunning) {
            lastCompletedScanSignature = signature
            return
        }
        if (signature != 0L) {
            lastGlobalScanSignature = signature
        }
        CatalogScanCoordinator.ensureBackground()
    }

    LaunchedEffect(
        restoreCatalogSignature,
        globalScanSignature,
        restoreCatalogMedia,
        scanCatalogMedia,
        catalogVerified,
        hasLoadedOnce,
        loading,
        isInitialLoad
    ) {
        if (!catalogVerified || !hasLoadedOnce || loading || isInitialLoad) return@LaunchedEffect
        if (activeCatalogSignature == 0L) return@LaunchedEffect
        if (activeCatalogMedia.isEmpty()) return@LaunchedEffect

        bindGlobalCatalogAnalysis(activeCatalogMedia, activeCatalogSignature)
    }

    LaunchedEffect(
        restoreCatalogSignature,
        globalScanSignature,
        restoreCatalogMedia,
        scanCatalogMedia,
        catalogVerified,
        hasLoadedOnce,
        loading,
        isInitialLoad,
        allowCatalogBackgroundWork,
        allowCatalogBootstrap,
        coordinatorState.isPaused
    ) {
        if (!catalogVerified || !hasLoadedOnce || loading || isInitialLoad) return@LaunchedEffect
        if (!allowCatalogBackgroundWork) return@LaunchedEffect
        if (!allowCatalogBootstrap) return@LaunchedEffect
        if (coordinatorState.isPaused) return@LaunchedEffect
        if (AppPerformanceGovernor.shouldPauseNonCriticalWork()) return@LaunchedEffect
        if (activeCatalogSignature == 0L) return@LaunchedEffect
        if (activeCatalogSignature == lastGlobalScanSignature) return@LaunchedEffect

        if (firstInteractiveAtMs > 0L) {
            val startupElapsed = android.os.SystemClock.elapsedRealtime() - firstInteractiveAtMs
            val requestedDelayMs = catalogBootstrapDeferMs.coerceAtLeast(HOME_SCAN_STARTUP_DEFER_MS)
            val extraDelayMs = (requestedDelayMs - startupElapsed).coerceAtLeast(0L)
            if (extraDelayMs > 0L) {
                delay(extraDelayMs)
            }
        }

        if (activeCatalogMedia.isNotEmpty()) {
            startGlobalCatalogAnalysis(activeCatalogMedia, activeCatalogSignature)
        }
    }

    LaunchedEffect(activeCatalogSignature) {
        if (activeCatalogSignature == 0L) return@LaunchedEffect

        var retryBackoffStep = 0
        while (isActive) {
            val blockedByGovernor = AppPerformanceGovernor.shouldPauseNonCriticalWork()
            val baseCanRun =
                currentCatalogVerified &&
                currentHasLoadedOnce &&
                    !currentLoading &&
                    !currentIsInitialLoad &&
                    currentAllowCatalogBackgroundWork
            val canRun = baseCanRun && !blockedByGovernor && !currentCoordinatorState.isPaused
            val isCompletedForCurrentSignature =
                activeCatalogSignature != 0L && activeCatalogSignature == lastCompletedScanSignature
            val hasScanSessionForCurrentSignature =
                activeCatalogSignature != 0L && activeCatalogSignature == lastGlobalScanSignature
            val hasStartedSomeAnalysis =
                currentCoordinatorState.isRunning ||
                    currentCoordinatorState.overallProgress > 0f
            val isRunning = isCatalogAnalysisRunning(currentCoordinatorState)
            val hasPending = if (isCompletedForCurrentSignature && !isRunning) {
                false
            } else {
                hasPendingCatalogAnalysis(currentCoordinatorState)
            }

            if (
                canRun &&
                hasPending &&
                (allowCatalogBootstrap || hasScanSessionForCurrentSignature || hasStartedSomeAnalysis) &&
                !isRunning
            ) {
                val nextMedia = if (currentScanCatalogMedia.isNotEmpty()) currentScanCatalogMedia else currentRestoreCatalogMedia
                startGlobalCatalogAnalysis(nextMedia, activeCatalogSignature)
                retryBackoffStep = 0
            }

            val pollDelayMs = when {
                blockedByGovernor -> HOME_SCAN_KEEPALIVE_DEFERRED_POLL_MS
                !baseCanRun -> HOME_SCAN_KEEPALIVE_PAUSED_POLL_MS
                !hasPending -> HOME_SCAN_KEEPALIVE_DONE_POLL_MS
                isRunning -> HOME_SCAN_KEEPALIVE_SCANNING_POLL_MS
                else -> {
                    retryBackoffStep = (retryBackoffStep + 1).coerceAtMost(4)
                    HOME_SCAN_KEEPALIVE_ACTIVE_POLL_MS + (retryBackoffStep * 700L)
                }
            }
            delay(pollDelayMs)
        }
    }
}

internal fun resolveCatalogBootstrapDelayMs(demandActive: Boolean): Long {
    return if (demandActive) {
        HOME_SCAN_STARTUP_DEFER_MS
    } else {
        HOME_SCAN_IDLE_BOOTSTRAP_DEFER_MS
    }
}
