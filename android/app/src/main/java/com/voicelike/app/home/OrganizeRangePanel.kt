package com.voicelike.app

import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GifBox
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.PanoramaHorizontal
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RawOn
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Organize range panel (iOS RangePickerView): three single-choice groups (time, album,
// kind) that all apply together. Only "Pick Date" goes deeper: month list, then one
// month's calendar. Hidden albums have their own list.

private sealed interface OrganizeRangeRoute {
    data object Root : OrganizeRangeRoute
    data object Months : OrganizeRangeRoute
    data class Month(val monthKey: String) : OrganizeRangeRoute
    data object HiddenAlbums : OrganizeRangeRoute
}

private val OrganizeRangeRoute.depth: Int
    get() = when (this) {
        OrganizeRangeRoute.Root -> 0
        OrganizeRangeRoute.Months, OrganizeRangeRoute.HiddenAlbums -> 1
        is OrganizeRangeRoute.Month -> 2
    }

@Composable
internal fun OrganizeRangePanel(
    initialCriteria: MediaFilterCriteria,
    source: List<MediaItem>,
    processedIds: Set<Long>,
    hiddenFolders: Set<String>,
    prioritizedFolders: Set<String>,
    onBack: () -> Unit,
    onOrganize: (MediaFilterCriteria) -> Unit,
    onHideFolder: (String) -> Unit,
    onUnhideFolder: (String) -> Unit,
    onTogglePinFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val strings = remember(language) { organizeRangeStrings(language) }
    val palette = organizeRangePalette()
    val layoutDirection = LocalLayoutDirection.current

    var pending by remember { mutableStateOf(initialCriteria) }
    var routes by remember { mutableStateOf(listOf<OrganizeRangeRoute>(OrganizeRangeRoute.Root)) }
    val route = routes.last()
    val rootScroll = rememberScrollState()

    // Previous facets stay on screen while the next ones are built.
    var facets by remember { mutableStateOf<OrganizeRangeFacets?>(null) }
    LaunchedEffect(pending, source, processedIds, hiddenFolders, prioritizedFolders) {
        val criteria = pending
        facets = withContext(Dispatchers.Default) {
            buildOrganizeRangeFacets(
                source = source,
                processedIds = processedIds,
                hiddenFolders = hiddenFolders,
                prioritizedFolders = prioritizedFolders,
                criteria = criteria
            )
        }
    }

    val goBack: () -> Unit = {
        if (routes.size > 1) routes = routes.dropLast(1) else onBack()
    }
    val push: (OrganizeRangeRoute) -> Unit = { routes = routes + it }
    val selectTimeAndClose: (MediaFilterCriteria) -> Unit = { next ->
        pending = next.withoutCollection()
        routes = listOf(OrganizeRangeRoute.Root)
    }

    BackHandler(onBack = goBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                val forward = targetState.depth >= initialState.depth
                val sign = (if (forward) 1 else -1) * (if (layoutDirection == LayoutDirection.Rtl) -1 else 1)
                (slideInHorizontally(tween(280)) { width -> sign * width / 3 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(tween(280)) { width -> -sign * width / 4 } + fadeOut(tween(180)))
            },
            label = "organize-range-route"
        ) { current ->
            when (current) {
                OrganizeRangeRoute.Root -> OrganizeRangeRootPage(
                    strings = strings,
                    language = language,
                    palette = palette,
                    pending = pending,
                    facets = facets,
                    scrollState = rootScroll,
                    onPendingChange = { pending = it.withoutCollection() },
                    onBack = onBack,
                    onOpenMonths = { push(OrganizeRangeRoute.Months) },
                    onOpenHiddenAlbums = { push(OrganizeRangeRoute.HiddenAlbums) },
                    onOrganize = { onOrganize(pending) },
                    onHideFolder = { name ->
                        if (pending.folder == name) pending = pending.copy(folder = "All").withoutCollection()
                        onHideFolder(name)
                    },
                    onTogglePinFolder = onTogglePinFolder
                )
                OrganizeRangeRoute.Months -> OrganizeRangeMonthsPage(
                    strings = strings,
                    language = language,
                    palette = palette,
                    pending = pending,
                    months = facets?.months.orEmpty(),
                    onBack = goBack,
                    onOpenMonth = { push(OrganizeRangeRoute.Month(it)) }
                )
                is OrganizeRangeRoute.Month -> OrganizeRangeMonthPage(
                    monthKey = current.monthKey,
                    strings = strings,
                    language = language,
                    palette = palette,
                    pending = pending,
                    source = source,
                    processedIds = processedIds,
                    hiddenFolders = hiddenFolders,
                    fallbackCount = facets?.months?.firstOrNull { it.monthKey == current.monthKey }?.remaining ?: 0,
                    onBack = goBack,
                    onSelect = selectTimeAndClose
                )
                OrganizeRangeRoute.HiddenAlbums -> OrganizeRangeHiddenAlbumsPage(
                    strings = strings,
                    language = language,
                    palette = palette,
                    albums = facets?.hiddenAlbums.orEmpty(),
                    onBack = goBack,
                    onUnhide = onUnhideFolder
                )
            }
        }
    }
}

// MARK: Root

@Composable
private fun OrganizeRangeRootPage(
    strings: OrganizeRangeStrings,
    language: AppLanguage,
    palette: OrganizeRangePalette,
    pending: MediaFilterCriteria,
    facets: OrganizeRangeFacets?,
    scrollState: ScrollState,
    onPendingChange: (MediaFilterCriteria) -> Unit,
    onBack: () -> Unit,
    onOpenMonths: () -> Unit,
    onOpenHiddenAlbums: () -> Unit,
    onOrganize: () -> Unit,
    onHideFolder: (String) -> Unit,
    onTogglePinFolder: (String) -> Unit
) {
    val tick = rememberOrganizeRangeTick()
    val loaded = facets != null
    val data = facets ?: OrganizeRangeFacets.Empty
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var menuAlbum by remember { mutableStateOf<String?>(null) }

    // A pill that is not selected and would leave nothing to organize fades out.
    fun dims(selected: Boolean, count: Int?): Boolean = loaded && !selected && (count ?: 0) == 0

    val resetAction: (@Composable RowScope.() -> Unit)? = if (pending.isActive) {
        {
            Box(
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        tick()
                        onPendingChange(MediaFilterCriteria())
                    }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.reset,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primary,
                    maxLines = 1
                )
            }
        }
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        UnifiedSubpageTopBar(
            title = strings.title,
            onBack = {
                tick()
                onBack()
            },
            trailing = resetAction
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp + navBottom)
            ) {
                if (pending.isCollection) {
                    OrganizeRangeCollectionBanner(
                        title = pending.collectionTitle.orEmpty(),
                        subtitle = organizeSortingStrings(language).fromInsights,
                        clearLabel = organizeSortingStrings(language).clearRange,
                        palette = palette,
                        onClear = {
                            tick()
                            onPendingChange(pending.withoutCollection())
                        }
                    )
                }
                // Time
                val timeIsAll = pending.timePreset == OrganizeTimePreset.ALL &&
                    pending.month == "All" && pending.day == "All"
                val datePicked = pending.timePreset == OrganizeTimePreset.ALL &&
                    (pending.month != "All" || pending.day != "All")
                val randomPicked = pending.timePreset == OrganizeTimePreset.RANDOM_DAY
                val todaySelected = pending.timePreset == OrganizeTimePreset.TODAY
                val onThisDaySelected = pending.timePreset == OrganizeTimePreset.ON_THIS_DAY
                val timePills = listOf(
                    OrganizeRangePillSpec(
                        icon = Icons.Outlined.GridView,
                        title = strings.all,
                        selected = timeIsAll,
                        dimmed = false,
                        onClick = { onPendingChange(pending.withTime()) }
                    ),
                    OrganizeRangePillSpec(
                        icon = Icons.Outlined.WbSunny,
                        title = strings.today,
                        selected = todaySelected,
                        dimmed = dims(todaySelected, data.timeCounts[OrganizeTimePreset.TODAY]),
                        onClick = { onPendingChange(pending.withTime(OrganizeTimePreset.TODAY)) }
                    ),
                    OrganizeRangePillSpec(
                        icon = Icons.Outlined.History,
                        title = strings.onThisDay,
                        selected = onThisDaySelected,
                        dimmed = dims(onThisDaySelected, data.timeCounts[OrganizeTimePreset.ON_THIS_DAY]),
                        onClick = { onPendingChange(pending.withTime(OrganizeTimePreset.ON_THIS_DAY)) }
                    ),
                    // Random Day: once picked it shows the rolled day; tapping again rerolls.
                    OrganizeRangePillSpec(
                        icon = Icons.Outlined.Shuffle,
                        title = if (randomPicked && pending.day != "All") {
                            organizeDayLabel(pending.day, language, withYear = false)
                        } else {
                            strings.randomDay
                        },
                        selected = randomPicked,
                        dimmed = dims(randomPicked, data.timeCounts[OrganizeTimePreset.RANDOM_DAY]),
                        onClick = {
                            val day = pickRandomOrganizeDay(
                                data.randomDayCandidates,
                                if (randomPicked) pending.day else null
                            )
                            if (day != null) {
                                onPendingChange(pending.withTime(OrganizeTimePreset.RANDOM_DAY, day = day))
                            }
                        }
                    ),
                    OrganizeRangePillSpec(
                        icon = Icons.Outlined.CalendarMonth,
                        title = if (datePicked) {
                            organizeTimeTitle(pending, language, short = true) ?: strings.pickDate
                        } else {
                            strings.pickDate
                        },
                        selected = datePicked,
                        dimmed = false,
                        chevron = true,
                        onClick = onOpenMonths
                    )
                )
                OrganizeRangeSectionTitle(strings.sectionTime, palette)
                OrganizeRangeGrid(columns = 2, count = timePills.size) { index ->
                    OrganizeRangePill(spec = timePills[index], palette = palette)
                }

                // Album
                val albums = data.albums
                val showHiddenEntry = data.hiddenAlbums.isNotEmpty()
                val albumCount = 1 + albums.size + if (showHiddenEntry) 1 else 0
                OrganizeRangeSectionTitle(strings.sectionAlbum, palette)
                OrganizeRangeGrid(columns = 2, count = albumCount) { index ->
                    when {
                        index == 0 -> OrganizeRangePill(
                            spec = OrganizeRangePillSpec(
                                icon = Icons.Outlined.GridView,
                                title = strings.all,
                                selected = pending.folder == "All",
                                dimmed = false,
                                onClick = { onPendingChange(pending.copy(folder = "All")) }
                            ),
                            palette = palette
                        )
                        index <= albums.size -> {
                            val album = albums[index - 1]
                            val selected = pending.folder == album.name
                            Box {
                                OrganizeRangePill(
                                    spec = OrganizeRangePillSpec(
                                        icon = if (album.pinned) Icons.Outlined.Flag else Icons.Outlined.Folder,
                                        title = organizeAlbumTitle(album.name, language),
                                        selected = selected,
                                        dimmed = dims(selected, album.remaining),
                                        onClick = { onPendingChange(pending.copy(folder = album.name)) }
                                    ),
                                    palette = palette,
                                    onLongClick = { menuAlbum = album.name }
                                )
                                DropdownMenu(
                                    expanded = menuAlbum == album.name,
                                    onDismissRequest = { menuAlbum = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.hideAlbum) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.VisibilityOff, contentDescription = null)
                                        },
                                        onClick = {
                                            tick()
                                            menuAlbum = null
                                            onHideFolder(album.name)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (album.pinned) strings.unpinAlbum else strings.pinAlbum) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Flag, contentDescription = null)
                                        },
                                        onClick = {
                                            tick()
                                            menuAlbum = null
                                            onTogglePinFolder(album.name)
                                        }
                                    )
                                }
                            }
                        }
                        else -> OrganizeRangePill(
                            spec = OrganizeRangePillSpec(
                                icon = Icons.Outlined.VisibilityOff,
                                title = strings.hiddenAlbums,
                                selected = false,
                                dimmed = false,
                                chevron = true,
                                onClick = onOpenHiddenAlbums
                            ),
                            palette = palette
                        )
                    }
                }

                // Type
                val kinds = OrganizeKind.panelOrder
                OrganizeRangeSectionTitle(strings.sectionType, palette)
                OrganizeRangeGrid(columns = 3, count = kinds.size) { index ->
                    val kind = kinds[index]
                    val selected = pending.type == kind
                    OrganizeRangePill(
                        spec = OrganizeRangePillSpec(
                            icon = organizeKindIcon(kind),
                            title = organizeKindTitle(kind, language),
                            selected = selected,
                            dimmed = kind != OrganizeKind.ALL && dims(selected, data.kindCounts[kind]),
                            onClick = { onPendingChange(pending.copy(type = kind)) }
                        ),
                        palette = palette
                    )
                }
            }

            OrganizeRangeOrganizeBar(
                label = when {
                    !loaded -> null
                    data.total > 0 -> strings.organizeLabel(data.total)
                    else -> strings.nothingToOrganize
                },
                enabled = loaded && data.total > 0,
                palette = palette,
                navBottom = navBottom,
                onClick = {
                    tick()
                    onOrganize()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun OrganizeRangeOrganizeBar(
    label: String?,
    enabled: Boolean,
    palette: OrganizeRangePalette,
    navBottom: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = palette.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = 0f),
                    0.3f to background.copy(alpha = 0.94f),
                    1f to background
                )
            )
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 34.dp + navBottom)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .then(if (enabled) Modifier.bounceClick(scaleDown = 0.97f) else Modifier)
                .clip(CircleShape)
                .background(palette.primary)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (label == null) {
                CircularProgressIndicator(
                    color = palette.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

// MARK: Pick Date, level 2 (months)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OrganizeRangeMonthsPage(
    strings: OrganizeRangeStrings,
    language: AppLanguage,
    palette: OrganizeRangePalette,
    pending: MediaFilterCriteria,
    months: List<OrganizeMonthFacet>,
    onBack: () -> Unit,
    onOpenMonth: (String) -> Unit
) {
    val tick = rememberOrganizeRangeTick()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val selectedMonth = when {
        pending.timePreset == OrganizeTimePreset.ALL && pending.month != "All" -> pending.month
        (pending.timePreset == OrganizeTimePreset.ALL || pending.timePreset == OrganizeTimePreset.RANDOM_DAY) &&
            pending.day != "All" -> pending.day.take(7)
        else -> null
    }
    // months come newest first, so grouping keeps years newest first too.
    val years = remember(months) { months.groupBy { it.year }.toList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        UnifiedSubpageTopBar(
            title = strings.pickDate,
            onBack = {
                tick()
                onBack()
            }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 48.dp + navBottom)
        ) {
            years.forEach { (year, yearMonths) ->
                stickyHeader(key = "year-$year") {
                    Text(
                        text = year.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.foreground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.background)
                            .padding(start = 4.dp, top = 14.dp, bottom = 4.dp)
                    )
                }
                items(yearMonths, key = { it.monthKey }) { month ->
                    OrganizeRangeFolderRow(
                        title = organizeMonthCardLabel(month.month, language),
                        count = month.remaining,
                        previews = month.previews,
                        selected = selectedMonth == month.monthKey,
                        pinned = false,
                        enabled = month.remaining > 0,
                        disabledAlpha = 0.45f,
                        strings = strings,
                        palette = palette,
                        onClick = {
                            tick()
                            onOpenMonth(month.monthKey)
                        }
                    )
                }
            }
        }
    }
}

// MARK: Pick Date, level 3 (one month's calendar)

@Composable
private fun OrganizeRangeMonthPage(
    monthKey: String,
    strings: OrganizeRangeStrings,
    language: AppLanguage,
    palette: OrganizeRangePalette,
    pending: MediaFilterCriteria,
    source: List<MediaItem>,
    processedIds: Set<Long>,
    hiddenFolders: Set<String>,
    fallbackCount: Int,
    onBack: () -> Unit,
    onSelect: (MediaFilterCriteria) -> Unit
) {
    val tick = rememberOrganizeRangeTick()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val days by produceState<List<OrganizeDayFacet>?>(
        null,
        monthKey,
        pending.folder,
        pending.type,
        source,
        processedIds,
        hiddenFolders
    ) {
        val criteria = pending
        value = withContext(Dispatchers.Default) {
            buildOrganizeMonthDays(
                source = source,
                processedIds = processedIds,
                hiddenFolders = hiddenFolders,
                criteria = criteria,
                monthKey = monthKey
            )
        }
    }
    val dayFacets = days
    val byDay = remember(dayFacets) { dayFacets.orEmpty().associateBy { it.day } }
    val monthCount = dayFacets?.sumOf { it.remaining } ?: fallbackCount
    val topDays = remember(dayFacets) {
        dayFacets.orEmpty()
            .sortedWith(compareByDescending<OrganizeDayFacet> { it.remaining }.thenBy { it.day })
            .take(3)
    }

    val year = monthKey.take(4).toIntOrNull() ?: 2000
    val month = monthKey.drop(5).take(2).toIntOrNull()?.coerceIn(1, 12) ?: 1
    val layout = remember(year, month, language) { organizeCalendarLayout(year, month, language) }

    val selectedDay = if (
        (pending.timePreset == OrganizeTimePreset.ALL || pending.timePreset == OrganizeTimePreset.RANDOM_DAY) &&
        pending.day.startsWith("$monthKey-")
    ) {
        pending.day.takeLast(2).toIntOrNull()
    } else {
        null
    }
    val wholeMonthSelected = pending.timePreset == OrganizeTimePreset.ALL &&
        pending.month == monthKey && pending.day == "All"
    fun dayKey(day: Int): String = "%s-%02d".format(Locale.US, monthKey, day)
    val pickDay: (Int) -> Unit = { day ->
        tick()
        onSelect(pending.withTime(day = dayKey(day)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        UnifiedSubpageTopBar(
            title = organizeMonthLabel(monthKey, language),
            onBack = {
                tick()
                onBack()
            }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 40.dp + navBottom)
        ) {
            // Whole month
            val wholeMonthEnabled = monthCount > 0
            val capsule = RoundedCornerShape(25.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .then(if (wholeMonthEnabled) Modifier.bounceClick(scaleDown = 0.97f) else Modifier)
                    .then(
                        if (wholeMonthSelected) {
                            Modifier
                                .clip(capsule)
                                .background(palette.primary.copy(alpha = 0.10f))
                                .border(1.5.dp, palette.primary.copy(alpha = 0.55f), capsule)
                        } else {
                            Modifier.organizeRangeGroupCard(capsule, palette)
                        }
                    )
                    .clickable(
                        enabled = wholeMonthEnabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        tick()
                        onSelect(pending.withTime(month = monthKey))
                    }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = if (wholeMonthSelected) palette.primary else palette.foreground.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.wholeMonth,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (wholeMonthSelected) palette.primary else palette.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.photosCount(monthCount),
                    fontSize = 13.sp,
                    color = if (wholeMonthSelected) palette.primary else palette.muted,
                    maxLines = 1
                )
            }

            // Calendar
            Column(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .organizeRangeGroupCard(RoundedCornerShape(24.dp), palette)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    layout.weekdaySymbols.forEach { symbol ->
                        Text(
                            text = symbol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.muted,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                val cellCount = layout.leadingOffset + layout.daysInMonth
                val rows = (cellCount + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (column in 0 until 7) {
                                val day = row * 7 + column - layout.leadingOffset + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    if (day in 1..layout.daysInMonth) {
                                        OrganizeRangeDayCell(
                                            day = day,
                                            facet = byDay[day],
                                            selected = selectedDay == day,
                                            palette = palette,
                                            onClick = { pickDay(day) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Busiest days
            if (topDays.isNotEmpty()) {
                Text(
                    text = strings.busiestDays,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.muted,
                    modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .organizeRangeGroupCard(RoundedCornerShape(24.dp), palette)
                ) {
                    topDays.forEachIndexed { index, entry ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 74.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(palette.cardBorder)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pickDay(entry.day) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.slot)
                            ) {
                                OrganizeRangeThumbnail(
                                    item = entry.cover,
                                    sizePx = 160,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = organizeDayLabel(entry.dayKey, language, withYear = false),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.foreground,
                                    maxLines = 1
                                )
                                Text(
                                    text = strings.photosCount(entry.remaining),
                                    fontSize = 13.sp,
                                    color = palette.muted,
                                    maxLines = 1
                                )
                            }
                            if (selectedDay == entry.day) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = palette.muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Day cell: 1:1 rounded square, that day's newest photo under a 28% black wash with
// the day number in white; days without photos left to organize are muted text.
@Composable
private fun OrganizeRangeDayCell(
    day: Int,
    facet: OrganizeDayFacet?,
    selected: Boolean,
    palette: OrganizeRangePalette,
    onClick: () -> Unit
) {
    if (facet == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = day.toString(),
                fontSize = 13.sp,
                color = palette.muted.copy(alpha = palette.muted.alpha * 0.6f),
                maxLines = 1,
                softWrap = false
            )
        }
        return
    }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .bounceClick(scaleDown = 0.94f)
            .clip(shape)
            .background(palette.slot)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        OrganizeRangeThumbnail(item = facet.cover, sizePx = 200, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )
        Text(
            text = day.toString(),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 2f), blurRadius = 4f)
            ),
            maxLines = 1,
            softWrap = false
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.5.dp, palette.primary, shape)
            )
        }
    }
}

private data class OrganizeCalendarLayout(
    val daysInMonth: Int,
    val leadingOffset: Int,
    val weekdaySymbols: List<String>
)

// Leading blanks and weekday header follow the device's first day of the week.
private fun organizeCalendarLayout(year: Int, month: Int, language: AppLanguage): OrganizeCalendarLayout {
    val firstWeekday = Calendar.getInstance(Locale.getDefault()).firstDayOfWeek
    val calendar = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingOffset = (calendar.get(Calendar.DAY_OF_WEEK) - firstWeekday + 7) % 7
    val locale = organizeRangeLocale(language)
    val formatter = runCatching { SimpleDateFormat("EEEEE", locale) }.getOrNull()
    val symbolCalendar = Calendar.getInstance().apply { clear() }
    val symbols = (0 until 7).map { offset ->
        val weekday = (firstWeekday - 1 + offset) % 7 + 1
        // 2023-01-01 was a Sunday.
        symbolCalendar.clear()
        symbolCalendar.set(2023, Calendar.JANUARY, weekday)
        val text = formatter?.let { runCatching { it.format(symbolCalendar.time) }.getOrNull() }.orEmpty()
        if (text.isBlank()) weekday.toString() else text
    }
    return OrganizeCalendarLayout(daysInMonth, leadingOffset, symbols)
}

// MARK: Hidden albums

@Composable
private fun OrganizeRangeHiddenAlbumsPage(
    strings: OrganizeRangeStrings,
    language: AppLanguage,
    palette: OrganizeRangePalette,
    albums: List<OrganizeAlbumFacet>,
    onBack: () -> Unit,
    onUnhide: (String) -> Unit
) {
    val tick = rememberOrganizeRangeTick()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var confirmName by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        UnifiedSubpageTopBar(
            title = strings.hiddenAlbums,
            onBack = {
                tick()
                onBack()
            }
        )
        if (albums.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(palette.slot),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = palette.muted,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.noHiddenAlbums,
                    fontSize = 15.sp,
                    color = palette.muted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 48.dp + navBottom)
            ) {
                items(albums, key = { it.name }) { album ->
                    OrganizeRangeFolderRow(
                        title = organizeAlbumTitle(album.name, language),
                        count = album.remaining,
                        previews = album.previews,
                        selected = false,
                        pinned = album.pinned,
                        enabled = true,
                        strings = strings,
                        palette = palette,
                        onClick = {
                            tick()
                            confirmName = album.name
                        }
                    )
                }
            }
        }
    }

    val name = confirmName
    if (name != null) {
        AlertDialog(
            onDismissRequest = { confirmName = null },
            containerColor = palette.dialog,
            title = { Text(strings.unhideTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.unhideMessage(organizeAlbumTitle(name, language))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        tick()
                        confirmName = null
                        onUnhide(name)
                    }
                ) {
                    Text(strings.confirm, color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmName = null }) {
                    Text(strings.cancel, color = palette.muted)
                }
            }
        )
    }
}

// MARK: Folder row (album / month card)

@Composable
private fun OrganizeRangeFolderRow(
    title: String,
    count: Int,
    previews: List<MediaItem>,
    selected: Boolean,
    pinned: Boolean,
    enabled: Boolean,
    strings: OrganizeRangeStrings,
    palette: OrganizeRangePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabledAlpha: Float = 0.45f
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else disabledAlpha)
            .then(if (enabled) Modifier.bounceClick(scaleDown = 0.98f) else Modifier)
            .organizeRangeGroupCard(shape, palette)
            .then(
                if (selected) Modifier.border(1.5.dp, palette.primary.copy(alpha = 0.55f), shape) else Modifier
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) palette.primary else palette.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (count > 0) strings.photosCount(count) else strings.albumDone,
                    fontSize = 13.sp,
                    color = palette.muted,
                    maxLines = 1,
                    softWrap = false
                )
                if (pinned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = strings.pinAlbum,
                        tint = palette.muted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 7 square slots: up to 6 thumbnails, then "+N" when there are more.
        val shown = minOf(previews.size, 6)
        val hasOverflow = count > 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (slot in 0 until 7) {
                val isThumb = slot < shown
                val isOverflow = slot == shown && hasOverflow
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isThumb || isOverflow) palette.slot else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isThumb) {
                        OrganizeRangeThumbnail(item = previews[slot], sizePx = 120, modifier = Modifier.fillMaxSize())
                    } else if (isOverflow) {
                        if (previews.size > 6) {
                            OrganizeRangeThumbnail(item = previews[6], sizePx = 120, modifier = Modifier.fillMaxSize())
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                        )
                        val extra = (count - 6).toString()
                        Text(
                            text = "+$extra",
                            fontSize = if (extra.length > 3) 10.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// MARK: Pill

private data class OrganizeRangePillSpec(
    val icon: ImageVector,
    val title: String,
    val selected: Boolean,
    val dimmed: Boolean,
    val chevron: Boolean = false,
    val onClick: () -> Unit
)

// Equal-width pill, content start-aligned so icons in a column line up.
@Composable
private fun OrganizeRangePill(
    spec: OrganizeRangePillSpec,
    palette: OrganizeRangePalette,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val tick = rememberOrganizeRangeTick()
    val hapticsAllowed = LocalHapticsEnabled.current
    val shape = RoundedCornerShape(23.dp)
    val tint = if (spec.selected) palette.primary else palette.foreground.copy(alpha = 0.75f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .alpha(if (spec.dimmed) 0.38f else 1f)
            .then(if (!spec.dimmed) Modifier.bounceClick(scaleDown = 0.96f) else Modifier)
            .then(
                if (spec.selected) {
                    Modifier
                        .clip(shape)
                        .background(palette.primary.copy(alpha = 0.10f))
                        .border(1.5.dp, palette.primary.copy(alpha = 0.55f), shape)
                } else {
                    Modifier.organizeRangeGroupCard(shape, palette)
                }
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !spec.dimmed,
                onLongClick = onLongClick,
                hapticFeedbackEnabled = hapticsAllowed,
                onClick = {
                    tick()
                    spec.onClick()
                }
            )
            .padding(start = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        // Shrinks to 0.7x before truncating, like iOS minimumScaleFactor.
        val titleColor = if (spec.selected) palette.primary else palette.foreground
        BasicText(
            text = spec.title,
            style = TextStyle(
                fontWeight = if (spec.selected) FontWeight.Bold else FontWeight.Normal,
                color = titleColor
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.MiddleEllipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
            modifier = Modifier.weight(1f)
        )
        if (spec.chevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = if (spec.selected) palette.primary else palette.muted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun OrganizeRangeSectionTitle(text: String, palette: OrganizeRangePalette) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = palette.muted,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun OrganizeRangeGrid(
    columns: Int,
    count: Int,
    item: @Composable (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowStart in 0 until count step columns) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (column in 0 until columns) {
                    val index = rowStart + column
                    Box(modifier = Modifier.weight(1f)) {
                        if (index < count) item(index)
                    }
                }
            }
        }
    }
}

private fun organizeKindIcon(kind: String): ImageVector = when (kind) {
    OrganizeKind.PHOTO -> Icons.Outlined.Image
    OrganizeKind.LIVE -> Icons.Outlined.MotionPhotosOn
    OrganizeKind.VIDEO -> Icons.Outlined.Videocam
    OrganizeKind.SCREENSHOT -> Icons.Outlined.CropFree
    OrganizeKind.SCREEN_RECORDING -> Icons.Outlined.RadioButtonChecked
    OrganizeKind.PANORAMA -> Icons.Outlined.PanoramaHorizontal
    OrganizeKind.ANIMATED -> Icons.Outlined.GifBox
    OrganizeKind.RAW -> Icons.Outlined.RawOn
    else -> Icons.Outlined.GridView
}

// MARK: Thumbnails

@Composable
private fun OrganizeRangeThumbnail(item: MediaItem, sizePx: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(item.id, item.uri, item.type, sizePx) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .apply { if (item.type == "video") decoderFactory(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .size(sizePx)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

// MARK: Styling

private data class OrganizeRangePalette(
    val isDark: Boolean,
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val cardFill: Color,
    val cardBorder: Color,
    val slot: Color,
    val dialog: Color
)

@Composable
private fun organizeRangePalette(): OrganizeRangePalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return OrganizeRangePalette(
        isDark = isDark,
        background = scheme.background,
        foreground = scheme.onBackground,
        muted = scheme.onBackground.copy(alpha = 0.5f),
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        cardFill = scheme.surface,
        cardBorder = scheme.onBackground.copy(alpha = if (isDark) 0.12f else 0.08f),
        slot = scheme.surfaceVariant,
        dialog = if (isDark) scheme.surfaceVariant else scheme.surface
    )
}

// "Group card": surface fill, hairline border, soft shadow in light mode.
private fun Modifier.organizeRangeGroupCard(shape: Shape, palette: OrganizeRangePalette): Modifier =
    this
        .then(
            if (palette.isDark) {
                Modifier
            } else {
                Modifier.shadow(
                    elevation = 3.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
            }
        )
        .clip(shape)
        .background(palette.cardFill)
        .border(0.5.dp, palette.cardBorder, shape)

@Composable
private fun rememberOrganizeRangeTick(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    return remember(haptic, hapticsAllowed) {
        { if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
}


// The photo set brought in from Insights, with a cross to drop back to the normal range.
@Composable
private fun OrganizeRangeCollectionBanner(
    title: String,
    subtitle: String,
    clearLabel: String,
    palette: OrganizeRangePalette,
    onClear: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .organizeRangeGroupCard(shape, palette)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, fontSize = 13.sp, color = palette.muted, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = clearLabel,
                tint = palette.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
