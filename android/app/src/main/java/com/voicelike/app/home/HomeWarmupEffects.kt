package com.voicelike.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

private const val HOME_WARMUP_LOOKAHEAD_COUNT = 4

@Composable
internal fun HomeWarmupEffects(
    context: Context,
    displayMedia: List<MediaItem>,
    currentLanguageCode: String,
    catalogVerified: Boolean,
    hasLoadedOnce: Boolean,
    isInitialLoad: Boolean,
    loading: Boolean,
    allowHomeWarmups: Boolean,
    homeInteractionPerfController: HomeInteractionPerfController
) {
    val visibleWarmupIds = remember {
        androidx.compose.runtime.derivedStateOf {
            buildMediaIdSet(
                displayMedia
                    .drop(1)
                    .take(HOME_WARMUP_LOOKAHEAD_COUNT)
            )
        }
    }

    LaunchedEffect(visibleWarmupIds.value, currentLanguageCode, catalogVerified, hasLoadedOnce, isInitialLoad, loading, homeInteractionPerfController.stableEpoch, allowHomeWarmups) {
        if (!catalogVerified || !hasLoadedOnce) return@LaunchedEffect
        if (isInitialLoad || loading) return@LaunchedEffect
        if (!allowHomeWarmups) return@LaunchedEffect
        val warmTargets = displayMedia
            .asSequence()
            .drop(1)
            .filter { it.type == "photo" || it.type == "video" }
            .take(HOME_WARMUP_LOOKAHEAD_COUNT)
            .toList()
        if (warmTargets.isEmpty()) return@LaunchedEffect

        CardInsightPreloader.syncWindow(context, warmTargets, currentLanguageCode)
    }
}
