package com.voicelike.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.GregorianCalendar

// Period tiles: period control, overview, quiet, busiest day + activity, calendar, rhythm,
// and the empty-library prologue (iOS InsightPeriodControl / InsightOverviewTiles /
// InsightCalendarTiles / InsightLifetimeTiles empty states).

/** Everything a tile needs; built once per result + theme + language. */
internal class InsightTileEnv(
    val state: InsightsUiState,
    val colors: InsightColors,
    val s: InsightStrings,
    val result: InsightsResult
) {
    val stats: InsightPeriodStats get() = result.stats
    val lifetime: InsightLifetimeStats get() = result.lifetime
    val narrative: InsightNarrative get() = result.narrative
    val period: InsightPeriod get() = result.period

    fun open(drill: InsightDrill, period: InsightPeriod = this.period) {
        state.openDrill(s.drillTitle(drill, period, result::cameraName), drill, period)
    }

    fun numberFormat(value: Int): String = s.number(value)
}

internal fun insightTodayValue(): Int {
    val c = Calendar.getInstance()
    return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
}

/** Thumbnail request for a media item (video frames through the video decoder). */
@Composable
internal fun rememberInsightThumbRequest(item: MediaItem, sizePx: Int): ImageRequest {
    val context = LocalContext.current
    return remember(item.id, item.uri, sizePx) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(sizePx)
            .crossfade(true)
            .apply { if (item.type == "video") decoderFactory(VideoFrameDecoder.Factory()) }
            .build()
    }
}

// MARK: - Period control

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InsightPeriodControl(
    state: InsightsUiState,
    colors: InsightColors,
    s: InsightStrings,
    lifetime: InsightLifetimeStats?,
    modifier: Modifier = Modifier
) {
    val period = state.period
    val haptic = LocalHapticFeedback.current
    val canPrev = lifetime == null || InsightStatsEngine.canStepPrev(period, lifetime)
    val canNext = InsightStatsEngine.canStepNext(period)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InsightSegmented(
            items = listOf(s.periodYear, s.periodMonth),
            selectedIndex = if (period.isYear) 0 else 1,
            colors = colors,
            modifier = Modifier.width(148.dp)
        ) { index ->
            state.selectMode(if (index == 0) InsightPeriodMode.Year else InsightPeriodMode.Month)
        }
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(colors.card)
                .border(0.5.dp, colors.border, RoundedCornerShape(15.dp))
                .padding(horizontal = 4.dp)
                .semantics { contentDescription = s.periodTitle(period) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeriodChevron(Icons.Outlined.ChevronLeft, canPrev, s.insPrevPeriod, colors) {
                haptic.insightTick()
                state.stepPeriod(-1)
            }
            Text(
                text = s.periodTitle(period),
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = colors.ink,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            PeriodChevron(Icons.Outlined.ChevronRight, canNext, s.insNextPeriod, colors) {
                haptic.insightTick()
                state.stepPeriod(1)
            }
        }
    }
}

@Composable
private fun PeriodChevron(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    label: String,
    colors: InsightColors,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.25f, tween(200), label = "chevron")
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 40.dp)
            .graphicsLayer { this.alpha = alpha }
            .semantics { contentDescription = label }
            .insightClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(20.dp))
    }
}

// MARK: - Overview

@Composable
internal fun InsightOverviewTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val stats = env.stats
    val narrative = env.narrative
    val haptic = LocalHapticFeedback.current
    val isEmpty = stats.isEmpty
    val bars = remember(stats.chart, s) {
        stats.chart.buckets.map { b ->
            InsightBarDatum(
                value = b.count,
                label = b.label,
                callout = if (b.key.length == 7) s.monthLabel(b.key) else s.dayLabel(b.key),
                isFuture = b.isFuture
            )
        }
    }
    Column(modifier = modifier.fillMaxWidth().insightCard(colors)) {
        InsightTileHeader(s.photosAndVideos, colors, env.state, s, trailing = InsightTileTrailing.ChartStyle)
        val hero = rollingValue(stats.total, revealed)
        Text(
            text = s.number(hero),
            style = InsightType.hero,
            color = if (isEmpty) colors.ink3 else colors.primary,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "${s.photosAndVideos}, ${s.periodTitle(env.period)}, ${s.number(stats.total)}" }
                .insightClickable(enabled = !isEmpty) {
                    haptic.insightTap()
                    env.open(InsightDrill.Period)
                }
        )
        if (narrative.comparison.isNotEmpty()) {
            ComparisonLine(narrative, colors, Modifier.padding(top = 2.dp))
        }
        if (!isEmpty) {
            InsightBarChart(
                data = bars,
                colors = colors,
                style = env.state.chartStyle,
                revealed = revealed,
                numberFormat = env::numberFormat,
                height = 110.dp,
                barSpacing = if (env.period.isYear) 8.dp else 3.dp,
                scrubbable = true,
                growDelayMillis = 120,
                modifier = Modifier.padding(top = 22.dp),
                onTap = { index -> stats.chart.drillFor(index)?.let { env.open(it) } }
            )
            Box(
                Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.border)
            )
            Row(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .insightRise(revealed, delayMillis = 200, distance = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightFigure(s.number(stats.photos), s.photos, colors, Modifier.weight(1f), isZero = stats.photos == 0) {
                    env.open(InsightDrill.Photos)
                }
                InsightFigure(s.number(stats.videos), s.videos, colors, Modifier.weight(1f), isZero = stats.videos == 0) {
                    env.open(InsightDrill.Videos)
                }
                InsightFigure(s.number(stats.screenshots), s.screenshots, colors, Modifier.weight(1f), isZero = stats.screenshots == 0) {
                    env.open(InsightDrill.Screenshots)
                }
            }
        }
    }
}

@Composable
private fun ComparisonLine(narrative: InsightNarrative, colors: InsightColors, modifier: Modifier = Modifier) {
    val glyph = when (narrative.trend) {
        InsightTrend.Up -> Icons.AutoMirrored.Outlined.TrendingUp to colors.primary
        InsightTrend.Down -> Icons.AutoMirrored.Outlined.TrendingDown to colors.ink2
        InsightTrend.Flat -> Icons.Outlined.DragHandle to colors.ink3
        InsightTrend.None -> null
    }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (glyph != null) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(glyph.second.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(glyph.first, contentDescription = null, tint = glyph.second, modifier = Modifier.size(11.dp))
            }
        }
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.ink2)) { append(narrative.comparison) }
            if (narrative.comparisonDetail.isNotEmpty()) {
                append("  ")
                withStyle(SpanStyle(color = colors.ink3)) { append(narrative.comparisonDetail) }
            }
        }
        Text(text, style = InsightType.body, modifier = Modifier.weight(1f))
    }
}

// MARK: - Quiet period

@Composable
internal fun InsightQuietTile(env: InsightTileEnv, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val narrative = env.narrative
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth().insightCard(colors)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.insQuietTitle, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                Text(narrative.quietBody.ifEmpty { s.insQuietBodyNone }, style = InsightType.body, color = colors.ink2)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent.copy(alpha = colors.accent.alpha * 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(18.dp))
            }
        }
        val target = narrative.quietJumpPeriod
        if (target != null && narrative.quietJumpTitle.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = InsightLayout.contentGap)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary)
                    .insightClickable {
                        haptic.insightTap()
                        env.state.jumpTo(target)
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(narrative.quietJumpTitle, style = InsightType.smBold, color = colors.onPrimary, maxLines = 1)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// MARK: - Busiest day + activity

@Composable
internal fun InsightHighlightActivityRow(
    env: InsightTileEnv,
    highlightRevealed: Boolean,
    activityRevealed: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(InsightLayout.gap)
    ) {
        InsightHighlightTile(env, highlightRevealed, Modifier.weight(1f).fillMaxHeight())
        InsightActivityTile(env, activityRevealed, Modifier.weight(1f).fillMaxHeight())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightHighlightTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier) {
    val colors = env.colors
    val s = env.s
    val stats = env.stats
    val haptic = LocalHapticFeedback.current
    val dayKey = stats.busiestDayKey
    val dateText = dayKey?.let { s.monthDay(it) }
    val countText = s.shots(stats.busiestDayCount)
    val cover = stats.cover
    val shape = RoundedCornerShape(InsightLayout.radius)
    val clickModifier = Modifier
        .semantics { contentDescription = listOfNotNull(s.insBusiestDayLabel, dateText, if (dateText != null) countText else null).joinToString(", ") }
        .insightClickable(enabled = dayKey != null) {
            haptic.insightTap()
            dayKey?.let { env.open(InsightDrill.Day(it)) }
        }
    if (cover != null) {
        val painter = rememberAsyncImagePainter(rememberInsightThumbRequest(cover, 480))
        val appear by animateFloatAsState(if (revealed) 1f else 0f, tween(900, easing = InsightRevealEasing), label = "cover")
        Box(
            modifier = modifier
                .heightIn(min = InsightLayout.halfTileMinHeight)
                .then(
                    if (colors.isDark) Modifier else Modifier.shadow(
                        elevation = 6.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                )
                .clip(shape)
                .background(colors.accent)
                .then(clickModifier)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = appear
                        val scale = 1.04f - 0.04f * appear
                        scaleX = scale
                        scaleY = scale
                    }
                    .paint(painter, sizeToIntrinsics = false, contentScale = ContentScale.Crop)
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.26f),
                            0.3f to Color.Transparent,
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.42f)
                        )
                    )
            )
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = s.insBusiestDayLabel,
                    style = InsightType.tileTitle.copy(shadow = Shadow(Color.Black.copy(alpha = 0.3f), blurRadius = 12f)),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(InsightLayout.padding)
                        .heightIn(min = 30.dp)
                )
                Spacer(Modifier.weight(1f))
                if (dateText != null) {
                    FlowRow(
                        modifier = Modifier
                            .padding(12.dp)
                            .insightRise(revealed, delayMillis = 250, distance = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PhotoChip(dateText)
                        PhotoChip(countText)
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .heightIn(min = InsightLayout.halfTileMinHeight)
                .insightCard(colors)
                .then(clickModifier)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                InsightTileHeader(s.insBusiestDayLabel, colors, env.state, s, Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent.copy(alpha = colors.accent.alpha * 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.weight(1f).heightIn(min = 14.dp))
            Text(dateText ?: "—", style = InsightType.tileFigure, color = if (dateText == null) colors.ink3 else colors.ink, maxLines = 1)
            if (dateText != null) {
                Text(countText, style = InsightType.body, color = colors.ink3, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun PhotoChip(text: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 26.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.Black.copy(alpha = 0.46f))
            .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = InsightType.smBold, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun InsightActivityTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier) {
    val colors = env.colors
    val s = env.s
    val stats = env.stats
    val elapsed = stats.elapsedDays
    val grow = rememberGrowProgress(revealed, delayMillis = 160)
    Column(
        modifier = modifier
            .heightIn(min = InsightLayout.halfTileMinHeight)
            .insightCard(colors)
    ) {
        InsightTileHeader(s.activeDays, colors, env.state, s)
        val shown = rollingValue(stats.activeDays, revealed)
        Row(
            modifier = Modifier
                .padding(top = 6.dp)
                .semantics {
                    contentDescription = if (elapsed > 0) {
                        s.format(s.insActiveDaysFmt, s.number(elapsed), s.number(stats.activeDays))
                    } else {
                        s.number(stats.activeDays)
                    }
                },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(s.number(shown), style = InsightType.tileFigure, color = colors.ink, maxLines = 1)
            if (elapsed > 0) {
                Text(
                    "/ ${s.number(elapsed)}",
                    style = InsightType.figure,
                    color = colors.ink3,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        if (elapsed > 0) {
            InsightProgressBar(
                fraction = stats.activeDays.toFloat() / elapsed.coerceAtLeast(1),
                colors = colors,
                height = 6.dp,
                grow = grow,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
        val streak = stats.longestStreak?.length ?: 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .insightRise(revealed, delayMillis = 180, distance = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InsightFigure(
                value = if (streak > 0) s.days(streak) else "—",
                label = s.insStreakLabel,
                colors = colors,
                modifier = Modifier.weight(1f),
                isZero = streak == 0
            )
            InsightFigure(
                value = s.number(stats.liked),
                label = s.liked,
                colors = colors,
                modifier = Modifier.weight(1f),
                isZero = stats.liked == 0
            ) { env.open(InsightDrill.Liked) }
        }
    }
}

// MARK: - Activity calendar

@Composable
internal fun InsightCalendarTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val stats = env.stats
    val period = env.period
    val today = if (env.lifetime.todayDayValue > 0) env.lifetime.todayDayValue else insightTodayValue()
    val model = remember(period.year, stats.heatmap, stats.heatThresholds, env.lifetime.firstDayValue, today) {
        InsightCalendarYearModel.build(period.year, stats.heatmap, stats.heatThresholds, env.lifetime.firstDayValue, today)
    }
    val select: (Int) -> Unit = { dayValue ->
        env.open(InsightDrill.Day("%04d-%02d-%02d".format(java.util.Locale.US, dayValue / 10000, (dayValue / 100) % 100, dayValue % 100)))
    }
    Column(
        modifier = modifier.fillMaxWidth().insightCard(colors),
        verticalArrangement = Arrangement.spacedBy(InsightLayout.titleToContent)
    ) {
        InsightTileHeader(s.activityHeatmap, colors, env.state, s)
        if (period.isYear) {
            val labels = remember(s.language) {
                DateFormatSymbols.getInstance(s.locale).shortMonths.take(12).mapIndexed { i, name -> name.ifEmpty { "${i + 1}" } }
            }
            InsightCalendarYearMatrix(model, labels, colors, revealed, onSelectDay = select)
        } else {
            val first = s.firstWeekday()
            val blanks = remember(period.year, period.month, first) {
                val weekday = GregorianCalendar(period.year, period.month - 1, 1).get(Calendar.DAY_OF_WEEK)
                (weekday - first + 7) % 7
            }
            val headers = remember(s.language, first) {
                val names = s.veryShortWeekdayNames()
                (0 until 7).map { names[(first - 1 + it) % 7] }
            }
            InsightCalendarMonthGrid(model, period.month, blanks, headers, colors, revealed, onSelectDay = select)
        }
        val showsToday = period.containsDay(today)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showsToday) {
                Box(
                    Modifier
                        .size(10.dp)
                        .border(1.2.dp, colors.ink.copy(alpha = 0.75f), CircleShape)
                )
                Text(s.today, style = InsightType.axis, color = colors.ink3, maxLines = 1, modifier = Modifier.padding(start = 1.dp))
            }
            Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
            Text(s.legendLess, style = InsightType.axis, color = colors.ink3, maxLines = 1, modifier = Modifier.padding(end = 2.dp))
            for (level in 1..5) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.heat(level))
                )
            }
            Text(s.legendMore, style = InsightType.axis, color = colors.ink3, maxLines = 1, modifier = Modifier.padding(start = 2.dp))
        }
    }
}

// MARK: - Rhythm

@Composable
internal fun InsightRhythmTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val stats = env.stats
    val narrative = env.narrative
    val sample = stats.weekdayCounts.sum()
    val isSparse = sample < InsightNarrator.RHYTHM_MIN_ITEMS || narrative.rhythmLine == s.insRhythmSparse
    val headline = if (isSparse) s.insRhythmSparse else narrative.rhythmLine
    val chartHeight = if (isSparse) 44.dp else 84.dp
    val first = s.firstWeekday()
    val weekdayBars = remember(stats.weekdayCounts, s.language, first) {
        val short = s.veryShortWeekdayNames()
        val names = s.shortWeekdayNames()
        (0 until 7).map { offset ->
            val index = (first - 1 + offset) % 7
            InsightBarDatum(stats.weekdayCounts.getOrElse(index) { 0 }, short.getOrNull(index), names.getOrElse(index) { "" })
        }
    }
    val hourBars = remember(stats.hourCounts) {
        (0 until 24).map { hour ->
            InsightBarDatum(
                stats.hourCounts.getOrElse(hour) { 0 },
                if (hour % 6 == 0) "$hour" else null,
                "%02d:00".format(java.util.Locale.US, hour)
            )
        }
    }
    val dim by animateFloatAsState(if (isSparse) 0.45f else 1f, tween(300), label = "rhythm-dim")
    Column(modifier = modifier.fillMaxWidth().insightCard(colors)) {
        InsightTileHeader(s.insChapterRhythm, colors, env.state, s, trailing = InsightTileTrailing.ChartStyle)
        Spacer(Modifier.height(InsightLayout.titleToContent))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (headline.isNotEmpty()) Text(headline, style = InsightType.sentence, color = colors.ink)
            if (narrative.rhythmSubline.isNotEmpty()) Text(narrative.rhythmSubline, style = InsightType.body, color = colors.ink2)
        }
        BoxWithConstraints(
            Modifier
                .padding(top = InsightLayout.contentGap + 4.dp)
                .fillMaxWidth()
                .graphicsLayer { alpha = dim }
        ) {
            val stacked = maxWidth + InsightLayout.padding * 2 < 330.dp
            val week: @Composable (Modifier) -> Unit = { m ->
                RhythmColumn(s.insWeekLabel, colors, m) {
                    InsightBarChart(
                        data = weekdayBars,
                        colors = colors,
                        style = env.state.chartStyle,
                        revealed = revealed,
                        numberFormat = env::numberFormat,
                        height = chartHeight,
                        barSpacing = 5.dp,
                        highlightIndex = if (isSparse) INSIGHT_NO_HIGHLIGHT else null,
                        showsPeakValue = !isSparse,
                        growDelayMillis = 80,
                        onTap = if (isSparse) null else { index -> env.open(InsightDrill.Weekday((first - 1 + index) % 7 + 1)) }
                    )
                }
            }
            val hour: @Composable (Modifier) -> Unit = { m ->
                RhythmColumn(s.insHourLabel, colors, m) {
                    InsightBarChart(
                        data = hourBars,
                        colors = colors,
                        style = env.state.chartStyle,
                        revealed = revealed,
                        numberFormat = env::numberFormat,
                        height = chartHeight,
                        barSpacing = 2.dp,
                        highlightIndex = if (isSparse) INSIGHT_NO_HIGHLIGHT else null,
                        showsPeakValue = !isSparse,
                        growDelayMillis = 180,
                        onTap = if (isSparse) null else { index -> env.open(InsightDrill.Daypart(InsightDaypart.of(index))) }
                    )
                }
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    week(Modifier.fillMaxWidth())
                    hour(Modifier.fillMaxWidth())
                }
            } else {
                val leading = (maxWidth - 20.dp) * 0.36f
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    week(Modifier.width(leading))
                    hour(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RhythmColumn(caption: String, colors: InsightColors, modifier: Modifier, chart: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(caption, style = InsightType.body, color = colors.ink3, maxLines = 1)
        chart()
    }
}

// MARK: - Empty library

@Composable
internal fun InsightPrologueCard(colors: InsightColors, s: InsightStrings, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .insightCard(colors, padding = 0.dp)
            .padding(start = 28.dp, end = 28.dp, top = 36.dp, bottom = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = colors.accent.alpha * 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(34.dp))
        }
        Text(
            s.insEmptyTitle,
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            color = colors.ink,
            modifier = Modifier.padding(top = 22.dp)
        )
        Text(
            s.insEmptyBody,
            style = InsightType.body.copy(textAlign = TextAlign.Center, lineHeight = 20.sp),
            color = colors.ink2,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
