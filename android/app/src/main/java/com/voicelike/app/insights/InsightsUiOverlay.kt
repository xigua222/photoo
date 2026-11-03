package com.voicelike.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Insights sub-pages above the tabs: the footprints globe page and the photo group (drill) page.

private class RenderedInsightRoute(val route: InsightRoute) {
    val visibility = MutableTransitionState(false).apply { targetState = true }
}

/**
 * Full-screen Insights sub-pages. Renders nothing without routes; handles back (pops one route)
 * and the push / pop transitions. Popped pages stay composed until their exit animation ends.
 */
@Composable
fun InsightsOverlayPages(
    state: InsightsUiState,
    allMedia: List<MediaItem>,
    likedIds: Set<Long>,
    onOrganize: (title: String, ids: Set<Long>) -> Unit,
    onOpenPhoto: (items: List<MediaItem>, index: Int) -> Unit
) {
    val routes = state.routes
    val rendered = remember { ArrayList<RenderedInsightRoute>() }
    val liveIds = routes.map { it.id }.toSet()
    routes.forEach { route ->
        if (rendered.none { it.route.id == route.id }) rendered.add(RenderedInsightRoute(route))
    }
    rendered.forEach { if (it.route.id !in liveIds) it.visibility.targetState = false }
    rendered.removeAll { it.route.id !in liveIds && it.visibility.isIdle && !it.visibility.currentState }
    if (rendered.isEmpty()) return

    BackHandler(enabled = routes.isNotEmpty()) { state.popRoute() }

    val language = LocalAppLanguage.current
    val s = remember(language) { insightStrings(language) }
    val colors = rememberInsightColors()

    Box(Modifier.fillMaxSize()) {
        rendered.toList().forEach { entry ->
            key(entry.route.id) {
                AnimatedVisibility(
                    visibleState = entry.visibility,
                    enter = FluidTransitions.ParallaxPushEnter,
                    exit = FluidTransitions.ParallaxPopExit
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(colors.background)
                            // Swallow touches so nothing underneath reacts.
                            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                    ) {
                        when (val route = entry.route) {
                            is InsightRoute.Footprints -> InsightFootprintsPage(state, route, colors, s)
                            is InsightRoute.Drill -> InsightDrillPage(
                                state, route, allMedia, likedIds, colors, s, onOrganize, onOpenPhoto
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Footprints page

@Composable
private fun InsightFootprintsPage(state: InsightsUiState, route: InsightRoute.Footprints, colors: InsightColors, s: InsightStrings) {
    val result = state.result
    val stats = result?.stats
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize()) {
        UnifiedSubpageTopBar(
            title = insightTitleJoin(s.periodTitle(route.period), s.insightsFootprint),
            onBack = { state.popRoute() }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = InsightLayout.margin, end = InsightLayout.margin, top = 4.dp, bottom = 40.dp + bottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (stats == null || stats.geoClusters.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier
                                .padding(bottom = 12.dp)
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.accent.copy(alpha = colors.accent.alpha * 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Map, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(26.dp))
                        }
                        Text(s.noGeotaggedPhotos, style = InsightType.sentence.copy(textAlign = TextAlign.Center), color = colors.ink)
                        Text(s.noGeotaggedPhotosHint, style = InsightType.body.copy(textAlign = TextAlign.Center), color = colors.ink2)
                    }
                }
            } else {
                item(key = "globe") {
                    val points = remember(stats.geoClusters) { insightGlobePoints(stats) }
                    val labels = remember(stats.placePins, s.language) { insightGlobeLabels(stats, s) }
                    val focus = remember(stats.geoClusters) { insightGlobeFocus(stats) }
                    InsightGlobe(
                        points = points,
                        labels = labels,
                        interactive = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .border(0.5.dp, colors.border, RoundedCornerShape(16.dp)),
                        initialFocus = focus
                    )
                }
            }
            if (stats != null && stats.placeTop.isNotEmpty() && result != null) {
                item(key = "places") {
                    Box(Modifier.fillMaxWidth().insightCard(colors)) {
                        InsightRankedList(
                            entries = stats.placeTop,
                            total = stats.placeTotal,
                            colors = colors,
                            s = s,
                            revealed = true,
                            maxRows = stats.placeTop.size,
                            displayLabel = ::insightPlaceListLabel,
                            onSelect = { label ->
                                val drill = InsightDrill.Place(label)
                                state.openDrill(s.drillTitle(drill, route.period, result::cameraName), drill, route.period)
                            }
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Drill (photo group) page

@Composable
private fun InsightDrillPage(
    state: InsightsUiState,
    route: InsightRoute.Drill,
    allMedia: List<MediaItem>,
    likedIds: Set<Long>,
    colors: InsightColors,
    s: InsightStrings,
    onOrganize: (title: String, ids: Set<Long>) -> Unit,
    onOpenPhoto: (items: List<MediaItem>, index: Int) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val items by produceState<List<MediaItem>?>(initialValue = null, route.id, allMedia, likedIds) {
        if (allMedia.isEmpty() && value != null) return@produceState
        value = withContext(Dispatchers.Default) {
            val snapshots = state.latestSnapshots
                ?: runCatching { MediaInsightRepository.snapshot(context) }.getOrDefault(emptyMap())
            InsightStatsEngine.drillItems(allMedia, snapshots, likedIds, route.period, route.drill)
        }
    }
    val current = items
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize()) {
        UnifiedSubpageTopBar(
            title = if (current == null) route.title else s.countTitle(route.title, current.size),
            onBack = { state.popRoute() },
            trailing = {
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.Style,
                    contentDescription = s.startOrganize,
                    enabled = !current.isNullOrEmpty(),
                    onClick = {
                        val list = current ?: return@UnifiedSubpageTopBarIconButton
                        if (list.isNotEmpty()) onOrganize(route.title, list.mapTo(LinkedHashSet(list.size)) { it.id })
                    }
                )
            }
        )
        when {
            current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp, color = colors.ink3)
            }
            current.isEmpty() -> Box(Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                Text(s.noPhotos, style = InsightType.sentence, color = colors.ink2)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 3.dp, end = 3.dp, top = 3.dp, bottom = 3.dp + bottom),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(current, key = { _, item -> item.id }, contentType = { _, _ -> "thumb" }) { index, item ->
                    InsightDrillThumb(item, colors) { onOpenPhoto(current, index) }
                }
            }
        }
    }
}

@Composable
private fun InsightDrillThumb(item: MediaItem, colors: InsightColors, onClick: () -> Unit) {
    val request = rememberInsightThumbRequest(item, 220)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(colors.accent)
            .insightClickable(onClick = onClick)
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (item.type == "video") {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}
