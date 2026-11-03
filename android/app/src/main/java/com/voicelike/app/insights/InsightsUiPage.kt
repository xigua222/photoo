package com.voicelike.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The Insights (洞察) tab: a LazyColumn with one item per tile, in the iOS order.
// Statistics are computed by InsightsUiState off the main thread; tiles only read the result.

/**
 * Content of the bottom-nav Insights tab. Leaves 132dp at the bottom for the floating nav bar;
 * content scrolls under the status bar, which is covered by the page background with a short fade.
 */
@Composable
fun InsightsTabContent(
    state: InsightsUiState,
    allMedia: List<MediaItem>,
    likedIds: Set<Long>,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val s = remember(language) { insightStrings(language) }
    val colors = rememberInsightColors()
    SideEffect { state.updateInputs(allMedia, likedIds, language) }
    LaunchedEffect(state) { state.runComputeLoop() }

    val result = state.result
    val env = remember(result, colors, s) { result?.let { InsightTileEnv(state, colors, s, it) } }
    val dim by animateFloatAsState(if (state.isRecomputing) 0.6f else 1f, tween(240), label = "recompute-dim")
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            state = state.listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = statusTop, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(InsightLayout.gap)
        ) {
            item(key = "title", contentType = "title") {
                Text(
                    text = s.tabInsights,
                    style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                )
            }
            when {
                env == null -> item(key = "loading", contentType = "loading") {
                    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp, color = colors.ink3)
                    }
                }
                env.lifetime.isEmpty -> item(key = "prologue", contentType = "prologue") {
                    InsightPrologueCard(colors, s, Modifier.padding(horizontal = InsightLayout.margin, vertical = 12.dp))
                }
                else -> insightTiles(env, dim)
            }
        }
        // Content scrolling up fades out under the status bar instead of running beneath its icons.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(colors.background)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(20.dp)
                .background(Brush.verticalGradient(listOf(colors.background, colors.background.copy(alpha = 0f))))
        )
    }
}

private fun LazyListScope.insightTiles(env: InsightTileEnv, dim: Float) {
    val state = env.state
    val stats = env.stats
    item(key = "period", contentType = "period") {
        InsightPeriodControl(
            state = state,
            colors = env.colors,
            s = env.s,
            lifetime = env.lifetime,
            modifier = Modifier
                .padding(horizontal = InsightLayout.margin)
                .padding(top = 6.dp, bottom = 4.dp)
        )
    }
    tile("overview", env, dim) { InsightOverviewTile(env, it) }
    if (stats.isEmpty) {
        tile("quiet", env, dim) { InsightQuietTile(env) }
    } else {
        item(key = "highlight", contentType = "half") {
            val highlight = rememberTileRevealed(state, "highlight", true)
            val activity = rememberTileRevealed(state, "activity", true)
            InsightHighlightActivityRow(
                env,
                highlight,
                activity,
                Modifier
                    .tileItem(this)
                    .graphicsLayer { alpha = dim }
                    .padding(horizontal = InsightLayout.margin)
                    .insightRise(highlight && activity)
            )
        }
        tile("calendar", env, dim) { InsightCalendarTile(env, it) }
        tile("rhythm", env, dim) { InsightRhythmTile(env, it) }
        tile("footprint", env, dim) { InsightFootprintTile(env, it) }
        tile("cameras", env, dim) { InsightCamerasTile(env, it) }
        tile("shooting", env, dim) { InsightShootingTile(env, it) }
        item(key = "formats", contentType = "half") {
            val formats = rememberTileRevealed(state, "formats", true)
            val orientation = rememberTileRevealed(state, "orientation", true)
            InsightFormatsOrientationRow(
                env,
                formats,
                orientation,
                Modifier
                    .tileItem(this)
                    .graphicsLayer { alpha = dim }
                    .padding(horizontal = InsightLayout.margin)
                    .insightRise(formats && orientation)
            )
        }
        tile("subjects", env, dim) { InsightSubjectsTile(env, it) }
    }
    if (env.lifetime.onThisDay.isNotEmpty()) {
        tile("onThisDay", env, 1f) { InsightOnThisDayTile(env, it) }
    }
    tile("lifetime", env, 1f) { InsightLifetimeTile(env, it) }
    if (env.lifetime.cameraTimeline.isNotEmpty()) {
        tile("cameraTimeline", env, 1f) { InsightCameraTimelineTile(env, it) }
    }
    item(key = "footer", contentType = "footer") {
        val revealed = rememberTileRevealed(state, "footer", true)
        InsightFooter(
            env,
            revealed,
            Modifier
                .tileItem(this)
                .padding(horizontal = InsightLayout.margin)
                .padding(top = 8.dp)
        )
    }
}

/** One full-width tile item: reveal gate + entrance + recompute dim. */
private fun LazyListScope.tile(
    id: String,
    env: InsightTileEnv,
    dim: Float,
    content: @Composable (revealed: Boolean) -> Unit
) {
    item(key = id, contentType = id) {
        val revealed = rememberTileRevealed(env.state, id, true)
        Box(
            Modifier
                .tileItem(this)
                .graphicsLayer { alpha = dim }
                .padding(horizontal = InsightLayout.margin)
                .insightRise(revealed)
        ) {
            content(revealed)
        }
    }
}

private fun Modifier.tileItem(scope: LazyItemScope): Modifier = with(scope) {
    this@tileItem.animateItem(
        fadeInSpec = tween(250),
        placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
        fadeOutSpec = tween(200)
    )
}
