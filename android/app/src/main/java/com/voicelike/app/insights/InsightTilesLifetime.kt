package com.voicelike.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// On this day, all your years, camera timeline and the footer (iOS InsightLifetimeTiles.swift).

// MARK: - On this day

@Composable
internal fun InsightOnThisDayTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val haptic = LocalHapticFeedback.current
    val years = env.lifetime.onThisDay
    Column(
        modifier = modifier.fillMaxWidth().insightCard(colors),
        verticalArrangement = Arrangement.spacedBy(InsightLayout.titleToContent)
    ) {
        InsightTileHeader(s.insOnThisDayTitle, colors, env.state, s)
        val line = InsightNarrator.onThisDayLine(env.lifetime, s)
        if (line.isNotEmpty()) Text(line, style = InsightType.sentence, color = colors.ink)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            itemsIndexed(years, key = { _, entry -> entry.year }) { index, entry ->
                Column(
                    modifier = Modifier
                        .insightRise(revealed, delayMillis = 100 + min(index, 6) * 35, distance = 6.dp)
                        .insightClickable {
                            haptic.insightTap()
                            env.open(InsightDrill.Day(entry.dayKey))
                        },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val painter = rememberAsyncImagePainter(rememberInsightThumbRequest(entry.cover, 300))
                    Box(
                        Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accent)
                            .paint(painter, sizeToIntrinsics = false, contentScale = ContentScale.Crop)
                    )
                    Text(s.yearsAgo(entry.yearsAgo), style = InsightType.smBold, color = colors.ink, maxLines = 1)
                    Text("${entry.year}", style = InsightType.body, color = colors.ink3, maxLines = 1)
                }
            }
        }
    }
}

// MARK: - All your years

@Composable
internal fun InsightLifetimeTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val lifetime = env.lifetime
    val years = lifetime.yearTotals
    val span = if (years.isEmpty()) 0 else years.last().year - years.first().year + 1
    val compact = span <= 1
    val figures = buildList {
        add(lifetime.total to s.insLifetimeTotal)
        if (compact) {
            add(lifetime.activeDays to s.insLifetimeActiveDays)
            if (lifetime.distinctPlaces > 0) add(lifetime.distinctPlaces to s.insLifetimePlaces)
        } else {
            add(span to s.insLifetimeYears)
            add(lifetime.activeDays to s.insLifetimeActiveDays)
        }
    }
    Column(modifier = modifier.fillMaxWidth().insightCard(colors)) {
        InsightTileHeader(s.insChapterLifetime, colors, env.state, s, trailing = InsightTileTrailing.ChartStyle)
        val since = env.narrative.lifetimeSince.ifEmpty { InsightNarrator.lifetimeSince(lifetime, s) }
        if (since.isNotEmpty()) {
            Text(
                since,
                style = InsightType.body,
                color = colors.ink2,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .insightRise(revealed, delayMillis = 35, distance = 8.dp)
            )
        }
        Row(
            modifier = Modifier.padding(top = InsightLayout.contentGap).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            figures.forEachIndexed { index, (value, label) ->
                val shown = rollingValue(value, revealed)
                InsightFigure(
                    value = s.number(shown),
                    label = label,
                    colors = colors,
                    isZero = value == 0,
                    modifier = Modifier
                        .weight(1f)
                        .insightRise(revealed, delayMillis = 70 + index * 35, distance = 8.dp)
                )
            }
        }
        if (!compact) {
            LifetimeYearChart(env, revealed, Modifier.padding(top = InsightLayout.contentGap + 4.dp))
        }
    }
}

@Composable
private fun LifetimeYearChart(env: InsightTileEnv, revealed: Boolean, modifier: Modifier) {
    val years = env.lifetime.yearTotals
    val selectedYear = if (env.period.isYear) env.period.year else null
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth.value
        val count = years.size
        val labelHalf = 17f
        val rawSlot = width / count
        val edge = max(0f, labelHalf - rawSlot / 2)
        val fitSlot = (width - edge * 2) / count
        val scrolls = fitSlot < 10f
        val slot = if (scrolls) 10f else fitSlot
        val minGap = max(3f, (slot * 0.26f).roundToInt().toFloat())
        val bar = max(2f, min(28f, slot - minGap))
        val gap = slot - bar
        val every = max(1, ceil(36f / slot).toInt())
        val last = count - 1
        val data = remember(years, every) {
            years.mapIndexed { index, entry ->
                InsightBarDatum(entry.count, if ((last - index) % every == 0) "${entry.year}" else null, "${entry.year}")
            }
        }
        val highlight = selectedYear?.let { y -> years.indexOfFirst { it.year == y }.takeIf { it >= 0 } }
        val chartWidth = (slot * count - gap).coerceAtLeast(1f).dp
        val chart: @Composable () -> Unit = {
            InsightBarChart(
                data = data,
                colors = env.colors,
                style = env.state.chartStyle,
                revealed = revealed,
                numberFormat = env::numberFormat,
                height = 84.dp,
                barSpacing = gap.dp,
                highlightIndex = highlight,
                scrubbable = true,
                growDelayMillis = 160,
                modifier = Modifier
                    .padding(horizontal = (gap / 2).dp)
                    .width(chartWidth),
                onTap = { index -> years.getOrNull(index)?.let { env.state.jumpTo(InsightPeriod.ofYear(it.year, env.period.month)) } }
            )
        }
        if (scrolls) {
            val scroll = rememberScrollState()
            LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
            Row(Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = max(12f, labelHalf - slot / 2).dp)) {
                chart()
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = edge.dp)) { chart() }
        }
    }
}

// MARK: - Camera timeline

@Composable
internal fun InsightCameraTimelineTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val haptic = LocalHapticFeedback.current
    val rows = remember(env.lifetime.cameraTimeline) { env.lifetime.cameraTimeline.reversed() }
    Column(
        modifier = modifier.fillMaxWidth().insightCard(colors),
        verticalArrangement = Arrangement.spacedBy(InsightLayout.titleToContent)
    ) {
        InsightTileHeader(s.insCameraTimelineTitle, colors, env.state, s)
        Column {
            rows.forEachIndexed { index, entry ->
                val isFirst = index == 0
                val isLast = index == rows.lastIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .insightRise(revealed, delayMillis = 100 + min(index, 8) * 35, distance = 6.dp)
                        .insightClickable {
                            haptic.insightTap()
                            // The timeline row is about that year: drill into it, not the period on screen.
                            env.open(InsightDrill.Camera(entry.camera), InsightPeriod.ofYear(entry.year))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "${entry.year}",
                        style = InsightType.smBold,
                        color = if (isFirst) colors.primary else colors.ink2,
                        modifier = Modifier.width(42.dp)
                    )
                    Box(Modifier.width(12.dp).height(44.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .padding(top = if (isFirst) 22.dp else 0.dp, bottom = if (isLast) 22.dp else 0.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(colors.accent)
                        )
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isFirst) colors.primary else colors.card)
                                .border(1.5.dp, if (isFirst) colors.primary else colors.ink3, CircleShape)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            env.result.cameraName(entry.camera),
                            style = TextStyle(fontSize = 17.sp, fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal),
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(InsightNarrator.cameraYearShare(entry, s), style = InsightType.body, color = colors.ink3, maxLines = 1)
                    }
                }
            }
        }
    }
}

// MARK: - Footer

@Composable
internal fun InsightFooter(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    var resetJob by remember { mutableStateOf<Job?>(null) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InsightCoverageCard(env, Modifier.padding(bottom = 24.dp).insightRise(revealed, distance = 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .insightRise(revealed, distance = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.primary)
                .insightClickable {
                    haptic.insightTap()
                    if (copyToClipboard(context, env.result.aiPrompt)) {
                        copied = true
                        Toast.makeText(context, s.insAskAiCopied, Toast.LENGTH_SHORT).show()
                        resetJob?.cancel()
                        resetJob = scope.launch {
                            delay(2000)
                            copied = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = copied,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                label = "ask-ai"
            ) { isCopied ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (isCopied) Icons.Outlined.Check else Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        if (isCopied) s.insAskAiCopied else s.insAskAiCta,
                        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        color = colors.onPrimary,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            s.insAskAiHint,
            style = InsightType.body.copy(textAlign = TextAlign.Center),
            color = colors.ink3,
            modifier = Modifier
                .padding(top = 10.dp)
                .insightRise(revealed, delayMillis = 35, distance = 8.dp)
        )
        InsightScanStatusLine(colors, s, Modifier.padding(top = 22.dp))
    }
}

private fun copyToClipboard(context: Context, text: String): Boolean = runCatching {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText("Photoo", text))
    true
}.getOrDefault(false)

@Composable
private fun InsightCoverageCard(env: InsightTileEnv, modifier: Modifier) {
    val colors = env.colors
    val s = env.s
    val coverage = env.lifetime.coverage
    Column(
        modifier = modifier.fillMaxWidth().insightCard(colors),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        InsightTileHeader(s.insCoverageTitle, colors, env.state, s)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CoverageRow(s.insCoverageCamera, coverage.ratio(coverage.withCamera), colors, s)
            CoverageRow(s.insCoverageLocation, coverage.ratio(coverage.withLocation), colors, s)
            CoverageRow(s.insCoverageTags, coverage.ratio(coverage.withTags), colors, s)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(InsightNarrator.coverageBasis(env.lifetime, s), style = InsightType.body, color = colors.ink2)
            InsightNarrator.coveragePending(env.lifetime, s)?.let {
                Text(it, style = InsightType.body, color = colors.ink2)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary.copy(alpha = 0.08f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.padding(top = 1.dp).size(16.dp))
            Text(s.insCoverageBiasNote, style = InsightType.body, color = colors.ink)
        }
    }
}

@Composable
private fun CoverageRow(title: String, ratio: Double, colors: InsightColors, s: InsightStrings) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = InsightType.body, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(76.dp))
        InsightProgressBar(ratio.toFloat(), colors, Modifier.weight(1f), height = 6.dp)
        Text(
            s.percent(ratio),
            style = InsightType.smBold.copy(textAlign = TextAlign.End),
            color = colors.ink,
            maxLines = 1,
            modifier = Modifier.width(44.dp)
        )
    }
}

/**
 * Scan status: "正在读取拍摄信息（42%）" + 2dp hairline while reading, "扫描已暂停" when paused,
 * "分析完成" for 1.5 s after a scan that did real work; nothing otherwise.
 */
@Composable
private fun InsightScanStatusLine(colors: InsightColors, s: InsightStrings, modifier: Modifier) {
    val scan by CatalogScanCoordinator.state.collectAsState()
    val progress = scan.overallProgress.coerceIn(0f, 1f)
    val reading = scan.isRunning && progress > 0f && progress < 0.999f
    var sawWork by remember { mutableStateOf(false) }
    var showDone by remember { mutableStateOf(false) }
    LaunchedEffect(reading, scan.isRunning, progress >= 0.999f) {
        if (reading) {
            sawWork = true
            showDone = false
        } else if (sawWork) {
            sawWork = false
            if (progress >= 0.999f) {
                showDone = true
                delay(1500)
                showDone = false
            }
        } else if (showDone) {
            delay(1500)
            showDone = false
        }
    }
    val phase = when {
        reading -> 1
        showDone -> 2
        else -> 0
    }
    AnimatedContent(
        targetState = phase,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        modifier = modifier,
        label = "scan-status"
    ) { p ->
        when (p) {
            1 -> Column(
                Modifier.padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    if (scan.isPaused) s.scanPaused else s.scanningLine(progress.toDouble()),
                    style = InsightType.body.copy(fontFeatureSettings = "tnum"),
                    color = colors.ink2
                )
                InsightProgressBar(progress, colors, Modifier.width(132.dp), height = 2.dp, minFill = 2.dp)
            }
            2 -> Row(
                Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(14.dp))
                Text(s.insScanDone, style = InsightType.body, color = colors.ink2)
            }
            else -> Spacer(Modifier.heightIn(min = 0.dp))
        }
    }
}
