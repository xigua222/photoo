package com.voicelike.app

import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import com.voicelike.app.livesynctool.LiveSyncToolPage

private enum class LabPage {
    Home,
    Globe,
    AlbumSettings,
    CopyrightVerify,
    LiveSyncTool
}

@Composable
fun LabFlowHost(
    showLab: Boolean,
    showLabGlobe: Boolean,
    showLabAlbumSettings: Boolean,
    showLabCopyrightVerify: Boolean,
    showLabLiveSyncTool: Boolean,
    showElectronicAlbumPlayback: Boolean,
    setShowLab: (Boolean) -> Unit,
    setShowLabGlobe: (Boolean) -> Unit,
    setShowLabAlbumSettings: (Boolean) -> Unit,
    setShowLabCopyrightVerify: (Boolean) -> Unit,
    setShowLabLiveSyncTool: (Boolean) -> Unit,
    setShowElectronicAlbumPlayback: (Boolean) -> Unit,
    activeCatalogMedia: List<MediaItem>,
    currentLanguage: AppLanguage,
    advancedTextureEnabled: Boolean,
    onAdvancedTextureEnabledChange: (Boolean) -> Unit,
    likedIds: Set<Long>,
    config: LabSlideshowConfig,
    onConfigChange: (LabSlideshowConfig) -> Unit,
    playbackQueue: List<MediaItem>,
    playbackMode: LabPlaybackMode,
    playbackForceLandscapeInFullscreen: Boolean,
    playbackDisplayMode: ElectronicAlbumDisplayMode,
    playbackWholeViewBlurBackgroundEnabled: Boolean,
    playbackIntervalMs: Long,
    playbackDurationMs: Long?,
    albumSettingsStandalone: Boolean = false,
    keepAlbumSettingsMountedDuringPlayback: Boolean = false,
    suppressStandaloneExitAnimation: Boolean = false,
    onAlbumFlowClosed: () -> Unit = {},
    onPlaybackPrepared: (
        queue: List<MediaItem>,
        mode: LabPlaybackMode,
        forceLandscapeInFullscreen: Boolean,
        displayMode: ElectronicAlbumDisplayMode,
        wholeViewBlurBackgroundEnabled: Boolean,
        intervalMs: Long,
        durationMs: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    val labFlowVisible =
        !showElectronicAlbumPlayback &&
            (
                showLab ||
                    showLabGlobe ||
                    showLabAlbumSettings ||
                    showLabCopyrightVerify ||
                    showLabLiveSyncTool
                )
    val folderOptions = remember(activeCatalogMedia) {
        activeCatalogMedia
            .asSequence()
            .map { it.bucketName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }
    val requestedPage = when {
        showLabLiveSyncTool -> LabPage.LiveSyncTool
        showLabCopyrightVerify -> LabPage.CopyrightVerify
        showLabAlbumSettings -> LabPage.AlbumSettings
        showLabGlobe -> LabPage.Globe
        showLab -> LabPage.Home
        else -> null
    }
    var retainedPage by remember { mutableStateOf(LabPage.Home) }
    LaunchedEffect(requestedPage) {
        requestedPage?.let { retainedPage = it }
    }
    val currentPage = requestedPage ?: retainedPage

    fun openPage(page: LabPage) {
        setShowLabGlobe(page == LabPage.Globe)
        setShowLabAlbumSettings(page == LabPage.AlbumSettings)
        setShowLabCopyrightVerify(page == LabPage.CopyrightVerify)
        setShowLabLiveSyncTool(page == LabPage.LiveSyncTool)
    }

    fun handleBack() {
        when (currentPage) {
            LabPage.Home -> setShowLab(false)
            LabPage.Globe -> setShowLabGlobe(false)
            LabPage.AlbumSettings -> {
                if (albumSettingsStandalone) {
                    onAlbumFlowClosed()
                } else {
                    setShowLabAlbumSettings(false)
                }
            }
            LabPage.CopyrightVerify -> setShowLabCopyrightVerify(false)
            LabPage.LiveSyncTool -> setShowLabLiveSyncTool(false)
        }
    }

    PageAnimatedVisibility(
        visible = labFlowVisible,
        enter = FluidTransitions.SheetEnter,
        exit = if (
            suppressStandaloneExitAnimation ||
            showElectronicAlbumPlayback
        ) {
            ExitTransition.None
        } else {
            FluidTransitions.SheetExit
        },
        modifier = Modifier.zIndex(42f)
    ) {
        PredictiveBackWrapper(
            isVisible = labFlowVisible,
            onBack = ::handleBack
        ) {
            when (currentPage) {
                LabPage.Home -> LabHomePage(
                    onOpenAlbumSettings = { openPage(LabPage.AlbumSettings) },
                    onOpenCopyrightVerify = { openPage(LabPage.CopyrightVerify) },
                    onOpenLiveSyncTool = { openPage(LabPage.LiveSyncTool) },
                    advancedTextureEnabled = advancedTextureEnabled,
                    onAdvancedTextureEnabledChange = onAdvancedTextureEnabledChange,
                    onBack = { setShowLab(false) }
                )

                LabPage.Globe -> GlobeViewPage(
                    allMedia = activeCatalogMedia,
                    onBack = { setShowLabGlobe(false) }
                )

                LabPage.AlbumSettings -> ElectronicAlbumSettingsPage(
                    folderOptions = folderOptions,
                    config = config,
                    onConfigChange = onConfigChange,
                    onStartSlideshow = { cfg ->
                        val queue = when (cfg.source) {
                            LabSlideshowSource.Favorites -> {
                                activeCatalogMedia.filter {
                                    likedIds.contains(it.id) &&
                                        it.type == "photo" &&
                                        !it.isLivePhoto &&
                                        !it.isMotionPhoto
                                }
                            }

                            LabSlideshowSource.Folder -> {
                                if (cfg.folderName.isBlank()) emptyList()
                                else activeCatalogMedia.filter {
                                    it.bucketName == cfg.folderName &&
                                        it.type == "photo" &&
                                        !it.isLivePhoto &&
                                        !it.isMotionPhoto
                                }
                            }
                        }
                        if (queue.isEmpty()) {
                            val tips = getNewPageStrings(currentLanguage).labEmptySourceToast
                            Toast.makeText(context, tips, Toast.LENGTH_SHORT).show()
                        } else {
                            onPlaybackPrepared(
                                // Shuffled once per start; the playback page keeps this order for the session.
                                if (cfg.shuffleEnabled) queue.shuffled() else queue,
                                cfg.playbackMode,
                                cfg.fullscreenLandscape,
                                cfg.displayMode,
                                cfg.wholeViewBlurBackgroundEnabled,
                                cfg.intervalSec.coerceIn(2, 600) * 1000L,
                                if (cfg.durationEnabled) cfg.durationMin.coerceIn(1, 180) * 60_000L else null
                            )
                            setShowLabGlobe(false)
                            if (!keepAlbumSettingsMountedDuringPlayback) {
                                setShowLabAlbumSettings(false)
                            }
                            setShowLabCopyrightVerify(false)
                            setShowLab(false)
                            setShowElectronicAlbumPlayback(true)
                        }
                    },
                    onBack = {
                        if (albumSettingsStandalone) {
                            onAlbumFlowClosed()
                        } else {
                            setShowLabAlbumSettings(false)
                        }
                    }
                )

                LabPage.CopyrightVerify -> LabCopyrightVerifyPage(
                    onBack = { setShowLabCopyrightVerify(false) }
                )

                LabPage.LiveSyncTool -> LiveSyncToolPage(
                    onBack = { setShowLabLiveSyncTool(false) }
                )
            }
        }
    }

    PageAnimatedVisibility(
        visible = showElectronicAlbumPlayback,
        enter = fadeIn(),
        exit = ExitTransition.None,
        modifier = Modifier.zIndex(43f)
    ) {
        PredictiveBackWrapper(
            isVisible = showElectronicAlbumPlayback,
            onBack = {
                if (albumSettingsStandalone) {
                    onAlbumFlowClosed()
                } else {
                    setShowElectronicAlbumPlayback(false)
                }
            }
        ) {
            ElectronicAlbumPlaybackPage(
                queue = playbackQueue,
                intervalMs = playbackIntervalMs,
                durationMs = playbackDurationMs,
                playbackMode = playbackMode,
                forceLandscapeInFullscreen = playbackForceLandscapeInFullscreen,
                displayMode = playbackDisplayMode,
                wholeViewBlurBackgroundEnabled = playbackWholeViewBlurBackgroundEnabled,
                onExit = {
                    if (albumSettingsStandalone) {
                        onAlbumFlowClosed()
                    } else {
                        setShowElectronicAlbumPlayback(false)
                    }
                }
            )
        }
    }
}
