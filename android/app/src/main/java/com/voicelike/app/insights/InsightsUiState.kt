package com.voicelike.app

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Insights tab state: period, chart style, computed statistics, sub-page routes and scroll state.
// It lives at activity level (rememberInsightsUiState), so it survives tab switches; the compute
// loop only runs while InsightsTabContent is composed (the tab is on screen).

/** Bar or line rendering of the time-series charts; one global preference. */
enum class InsightChartStyle { Bar, Line }

/** A full-screen sub-page above the tabs. [id] is unique per push (transition bookkeeping). */
sealed class InsightRoute {
    abstract val id: Long

    data class Footprints(override val id: Long, val period: InsightPeriod) : InsightRoute()

    /** [title] has no count; the page appends it once the items are resolved. */
    data class Drill(
        override val id: Long,
        val title: String,
        val drill: InsightDrill,
        val period: InsightPeriod
    ) : InsightRoute()
}

/** One finished computation; replaced as a whole so the page never mixes two results. */
@Immutable
internal class InsightsResult(
    val period: InsightPeriod,
    val lifetime: InsightLifetimeStats,
    val stats: InsightPeriodStats,
    val narrative: InsightNarrative,
    /** Raw camera label -> market name, for every camera the page shows. */
    val cameraNames: Map<String, String>,
    /** Raw AI prompt (lifetime numbers), built off the main thread. */
    val aiPrompt: String,
    /** The media list the result was computed from was empty. */
    val libraryEmpty: Boolean
) {
    fun cameraName(raw: String): String = cameraNames[raw] ?: insightCameraDisplayName(raw)
}

@Stable
class InsightsUiState internal constructor(private val appContext: Context) {

    // MARK: Period

    /** The period on screen. Starts at this year and moves to the smart default once. */
    var period: InsightPeriod by mutableStateOf(InsightStatsEngine.currentPeriod(InsightPeriodMode.Year))
        private set

    // MARK: Chart style

    var chartStyle: InsightChartStyle by mutableStateOf(loadChartStyle(appContext))
        private set

    fun updateChartStyle(style: InsightChartStyle) {
        if (style == chartStyle) return
        chartStyle = style
        runCatching {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CHART_STYLE, if (style == InsightChartStyle.Line) "line" else "bar")
                .apply()
        }
    }

    // MARK: Results

    internal var result: InsightsResult? by mutableStateOf(null)
        private set

    /** A recompute has been running for more than 250 ms: period tiles dim. */
    internal var isRecomputing: Boolean by mutableStateOf(false)
        private set

    /** CatalogScanCoordinator is running (scan-aware empty states). */
    internal var isScanning: Boolean by mutableStateOf(CatalogScanCoordinator.state.value.isRunning)
        private set

    /** Latest snapshot map, reused by the drill page so it does not copy the cache again. */
    @Volatile
    internal var latestSnapshots: Map<Long, MediaInsightSnapshot>? = null
        private set

    // MARK: Page UI state

    val listState: LazyListState = LazyListState()

    /** Shooting tile segment. */
    internal var shootingParam: InsightShootingParam by mutableStateOf(InsightShootingParam.Focal)

    /** Tiles that already played their entrance (never replayed). */
    internal val revealedTiles = mutableStateMapOf<String, Boolean>()
    private var nextRevealSlot = 0L

    /** Delay before the next tile may reveal, so tiles appearing together stagger by 70 ms. */
    internal fun nextRevealDelay(): Long {
        val now = SystemClock.uptimeMillis()
        val slot = maxOf(now, nextRevealSlot)
        nextRevealSlot = slot + REVEAL_STAGGER_MS
        return slot - now
    }

    // MARK: Routes

    private val routeStack = mutableStateListOf<InsightRoute>()
    private var routeCounter = 0L

    val routes: List<InsightRoute> get() = routeStack

    internal fun openFootprints() {
        routeStack.add(InsightRoute.Footprints(++routeCounter, period))
    }

    internal fun openDrill(title: String, drill: InsightDrill, period: InsightPeriod = this.period) {
        routeStack.add(InsightRoute.Drill(++routeCounter, title, drill, period))
    }

    /** Pops the top sub-page. */
    fun popRoute() {
        if (routeStack.isNotEmpty()) routeStack.removeAt(routeStack.lastIndex)
    }

    /** Pops every sub-page at once (e.g. before switching to the Organize tab). */
    fun clearRoutes() {
        routeStack.clear()
    }

    // MARK: Period changes (user)

    internal fun selectMode(mode: InsightPeriodMode) {
        if (mode == period.mode) return
        InsightDefaultPeriodOnce.markUserTouched()
        val lifetime = result?.lifetime
        val next = period.withMode(mode)
        period = if (lifetime != null) InsightStatsEngine.clamp(next, lifetime) else next
    }

    internal fun stepPeriod(offset: Int) {
        val lifetime = result?.lifetime ?: InsightLifetimeStats()
        val next = InsightStatsEngine.step(period, offset, lifetime)
        if (next == period) return
        InsightDefaultPeriodOnce.markUserTouched()
        period = next
    }

    internal fun jumpTo(target: InsightPeriod) {
        InsightDefaultPeriodOnce.markUserTouched()
        val lifetime = result?.lifetime
        period = if (lifetime != null) InsightStatsEngine.clamp(target, lifetime) else target
    }

    // MARK: Inputs

    private var inputMedia: List<MediaItem> by mutableStateOf(emptyList())
    private var inputLiked: Set<Long> by mutableStateOf(emptySet())
    private var inputLanguage: AppLanguage by mutableStateOf(AppLanguage.English)
    private var dataTick by mutableIntStateOf(0)
    /** Repository version the current snapshot map was read at. */
    @Volatile
    private var seenVersion = -1L

    /**
     * The activity rebuilds the lists whenever the tab becomes visible; an equal list (same item
     * instances, same liked set) keeps the old identity so returning to the tab does not recompute.
     */
    internal fun updateInputs(allMedia: List<MediaItem>, likedIds: Set<Long>, language: AppLanguage) {
        if (allMedia !== inputMedia && !sameItems(allMedia, inputMedia)) inputMedia = allMedia
        if (likedIds !== inputLiked && likedIds != inputLiked) inputLiked = likedIds
        if (language != inputLanguage) inputLanguage = language
    }

    private fun sameItems(a: List<MediaItem>, b: List<MediaItem>): Boolean {
        if (a.size != b.size || a.isEmpty()) return false
        for (i in a.indices) if (a[i] !== b[i]) return false
        return true
    }

    private data class ComputeKey(
        val media: List<MediaItem>,
        val liked: Set<Long>,
        val language: AppLanguage,
        val period: InsightPeriod,
        val dataTick: Int,
        val scanning: Boolean
    ) {
        // Lists are compared by identity: equality on 50k items would be a full scan per frame.
        override fun equals(other: Any?): Boolean = other is ComputeKey &&
            other.media === media && other.liked === liked && other.language == language &&
            other.period == period && other.dataTick == dataTick && other.scanning == scanning

        override fun hashCode(): Int = System.identityHashCode(media) * 31 + period.hashCode() + dataTick
    }

    /**
     * Runs while the tab is composed: watches the repository version (throttled to 10 s while
     * scanning), the scan state and the inputs, and recomputes off the main thread.
     */
    internal suspend fun runComputeLoop() = coroutineScope {
        var lastBump = 0L
        var pendingBump: Job? = null
        fun bump() {
            pendingBump?.cancel()
            pendingBump = null
            lastBump = SystemClock.uptimeMillis()
            dataTick++
        }

        // Anything written while the tab was away: one fresh read when it comes back.
        if (result != null && MediaInsightRepository.version.value != seenVersion) bump()

        launch {
            MediaInsightRepository.version.drop(1).collect {
                if (!CatalogScanCoordinator.state.value.isRunning) {
                    bump()
                } else if (pendingBump == null) {
                    val wait = (SCAN_THROTTLE_MS - (SystemClock.uptimeMillis() - lastBump)).coerceAtLeast(0L)
                    pendingBump = launch {
                        delay(wait)
                        pendingBump = null
                        lastBump = SystemClock.uptimeMillis()
                        dataTick++
                    }
                }
            }
        }
        launch {
            CatalogScanCoordinator.state.map { it.isRunning }.distinctUntilChanged().collect { running ->
                if (running != isScanning) {
                    isScanning = running
                    if (!running) bump()
                }
            }
        }

        var lastKey: ComputeKey? = null
        var cachedSnapshots: Map<Long, MediaInsightSnapshot>? = null
        var cachedSnapshotTick = -1
        var cachedSnapshotLanguage: AppLanguage? = null
        var cachedLifetime: InsightLifetimeStats? = null
        var lifetimeKey: Triple<Any, Any, Int>? = null

        snapshotFlow { ComputeKey(inputMedia, inputLiked, inputLanguage, period, dataTick, isScanning) }
            .collectLatest { key ->
                if (key == lastKey) return@collectLatest
                // The activity passes an empty list while the tab is hidden: keep the last result.
                val previous = result
                if (key.media.isEmpty() && previous != null && !previous.libraryEmpty) return@collectLatest

                val dimJob = launch {
                    delay(DIM_AFTER_MS)
                    isRecomputing = true
                }
                try {
                    val computed = withContext(Dispatchers.Default) {
                        val snapshots = if (cachedSnapshots != null && cachedSnapshotTick == key.dataTick &&
                            cachedSnapshotLanguage == key.language
                        ) {
                            cachedSnapshots!!
                        } else {
                            seenVersion = MediaInsightRepository.version.value
                            runCatching { MediaInsightRepository.snapshot(appContext) }.getOrDefault(emptyMap())
                                .let { insightSnapshotsForLanguage(it, key.language.code) }
                                .also {
                                    cachedSnapshots = it
                                    cachedSnapshotTick = key.dataTick
                                    cachedSnapshotLanguage = key.language
                                }
                        }
                        latestSnapshots = snapshots
                        val lk = Triple<Any, Any, Int>(IdentityBox(key.media), IdentityBox(key.liked), key.dataTick)
                        val lifetime = cachedLifetime?.takeIf { lifetimeKey == lk }
                            ?: InsightStatsEngine.computeLifetime(key.media, snapshots, key.liked).also {
                                cachedLifetime = it
                                lifetimeKey = lk
                            }
                        val resolvedPeriod = InsightDefaultPeriodOnce.take(lifetime)
                            ?: InsightStatsEngine.clamp(key.period, lifetime)
                        val stats = InsightStatsEngine.computePeriod(key.media, snapshots, key.liked, resolvedPeriod, lifetime)
                        val strings = insightStrings(key.language)
                        val names = HashMap<String, String>()
                        fun name(raw: String): String = names.getOrPut(raw) { resolveCameraName(appContext, raw) }
                        stats.cameraTop.forEach { name(it.id) }
                        lifetime.cameraTimeline.forEach { name(it.camera) }
                        lifetime.cameraTop5.forEach { name(it.id) }
                        val narrative = InsightNarrator.narrate(stats, lifetime, key.scanning, strings, ::name)
                        val prompt = InsightNarrator.aiPrompt(lifetime, strings, ::name)
                        InsightsResult(
                            period = resolvedPeriod,
                            lifetime = lifetime,
                            stats = stats,
                            narrative = narrative,
                            cameraNames = names,
                            aiPrompt = prompt,
                            libraryEmpty = key.media.isEmpty()
                        )
                    }
                    if (computed.period != period) period = computed.period
                    result = computed
                    lastKey = key.copy(period = computed.period)
                } finally {
                    dimJob.cancel()
                    isRecomputing = false
                }
            }
    }

    private class IdentityBox(val value: Any) {
        override fun equals(other: Any?): Boolean = other is IdentityBox && other.value === value
        override fun hashCode(): Int = System.identityHashCode(value)
    }

    internal companion object {
        const val PREFS_NAME = "photoo_prefs"
        const val KEY_CHART_STYLE = "insight_chart_style"
        const val SCAN_THROTTLE_MS = 10_000L
        const val DIM_AFTER_MS = 250L
        const val REVEAL_STAGGER_MS = 70L

        fun loadChartStyle(context: Context): InsightChartStyle = runCatching {
            val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CHART_STYLE, "bar")
            if (raw == "line") InsightChartStyle.Line else InsightChartStyle.Bar
        }.getOrDefault(InsightChartStyle.Bar)

        fun resolveCameraName(context: Context, raw: String): String {
            val resolved = runCatching { DeviceModelNameResolver.resolveCameraLabel(context, raw) }.getOrNull()
            return resolved?.takeIf { it.isNotBlank() && it != "--" } ?: insightCameraDisplayName(raw)
        }
    }
}

/** True while a sub-page (footprints / photo group) is on screen. */
val InsightsUiState.hasOverlay: Boolean get() = routes.isNotEmpty()

@Composable
fun rememberInsightsUiState(): InsightsUiState {
    val context = LocalContext.current.applicationContext
    return remember { InsightsUiState(context) }
}


// ML Kit tags are stored in the language they were recognised in; only count the ones in the
// current app language so the subject cloud never mixes languages.
internal fun insightSnapshotsForLanguage(
    snapshots: Map<Long, MediaInsightSnapshot>,
    languageCode: String
): Map<Long, MediaInsightSnapshot> {
    if (snapshots.isEmpty()) return snapshots
    var changed = false
    val result = HashMap<Long, MediaInsightSnapshot>(snapshots.size * 2)
    for ((id, snapshot) in snapshots) {
        val language = snapshot.keywordLanguage
        if (snapshot.keywords.isNotEmpty() && language != null && language != languageCode) {
            result[id] = snapshot.copy(keywords = emptyList())
            changed = true
        } else {
            result[id] = snapshot
        }
    }
    return if (changed) result else snapshots
}
