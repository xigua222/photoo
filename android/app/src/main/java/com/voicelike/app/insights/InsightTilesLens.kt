package com.voicelike.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Panorama
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RawOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Footprints, cameras, shooting settings, how you shoot, orientation and subjects
// (iOS InsightFootprintTile / InsightLensTiles / InsightShootingTile).

// MARK: - Footprints

@Composable
internal fun InsightFootprintTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val stats = env.stats
    val narrative = env.narrative
    val hasLocations = stats.locatedCount > 0 || stats.geoClusters.isNotEmpty()
    val showsGlobe = hasLocations || env.state.isScanning
    Column(modifier = modifier.fillMaxWidth().insightCard(colors)) {
        InsightTileHeader(s.insightsFootprint, colors, env.state, s)
        if (showsGlobe) {
            InsightFootprintGlobePreview(env, Modifier.padding(top = InsightLayout.titleToContent))
        }
        Column(
            modifier = Modifier.padding(top = if (showsGlobe) InsightLayout.contentGap else 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (narrative.placesLine.isNotEmpty()) {
                Text(narrative.placesLine, style = InsightType.sentence, color = colors.ink)
            }
            if (hasLocations) {
                narrative.placesSublines.filter { it.isNotEmpty() }.forEach { line ->
                    Text(line, style = InsightType.body, color = colors.ink2)
                }
            } else if (!showsGlobe) {
                Row(modifier = Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = colors.ink3, modifier = Modifier.padding(top = 2.dp).size(13.dp))
                    Text(s.noGeotaggedPhotosHint, style = InsightType.body, color = colors.ink3)
                }
            }
        }
        if (stats.placeTop.isNotEmpty()) {
            val shown = minOf(stats.placeTop.size, 5)
            val remaining = stats.placeDistinct - shown
            InsightRankedList(
                entries = stats.placeTop,
                total = stats.placeTotal,
                colors = colors,
                s = s,
                revealed = revealed,
                maxRows = 5,
                baseDelayMillis = 180,
                displayLabel = ::insightPlaceListLabel,
                moreTitle = if (remaining > 0) s.format(s.insMorePlacesFmt, s.number(remaining)) else null,
                onMore = { env.state.openFootprints() },
                modifier = Modifier.padding(top = 8.dp),
                onSelect = { env.open(InsightDrill.Place(it)) }
            )
        }
    }
}

/** Globe points / labels for one period's stats. */
internal fun insightGlobePoints(stats: InsightPeriodStats): List<InsightGlobePoint> =
    stats.geoClusters.map { InsightGlobePoint(it.latitude, it.longitude, it.count) }

internal fun insightGlobeLabels(stats: InsightPeriodStats, s: InsightStrings): List<InsightGlobeLabel> =
    stats.placePins.map { pin ->
        val city = insightCityName(pin.label, s.locale).ifEmpty { pin.label }
        val title = if (city.length > 14) city.take(13) + "…" else city
        InsightGlobeLabel(pin.latitude, pin.longitude, title, pin.count)
    }

/** Weighted centre of the clusters (initial focus of the globe). */
internal fun insightGlobeFocus(stats: InsightPeriodStats): Pair<Double, Double>? {
    val clusters = stats.geoClusters
    val total = clusters.sumOf { it.count }
    if (clusters.isEmpty() || total <= 0) return null
    val lat = clusters.sumOf { it.latitude * it.count } / total
    val lon = clusters.sumOf { it.longitude * it.count } / total
    return lat to lon
}

@Composable
private fun InsightFootprintGlobePreview(env: InsightTileEnv, modifier: Modifier) {
    val colors = env.colors
    val s = env.s
    val stats = env.stats
    val haptic = LocalHapticFeedback.current
    val points = remember(stats.geoClusters) { insightGlobePoints(stats) }
    val labels = remember(stats.placePins, s.language) { insightGlobeLabels(stats, s) }
    val focus = remember(stats.geoClusters) { insightGlobeFocus(stats) }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(shape)
            .semantics { contentDescription = s.openGlobe }
            .insightClickable {
                haptic.insightTap()
                env.state.openFootprints()
            }
    ) {
        InsightGlobe(
            points = points,
            labels = labels,
            interactive = false,
            modifier = Modifier.matchParentSize(),
            initialFocus = focus
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .heightIn(min = 28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.card)
                .border(0.5.dp, colors.border, RoundedCornerShape(9.dp))
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(s.openGlobe, style = InsightType.smBold, color = colors.ink, maxLines = 1)
            Icon(Icons.Outlined.NorthEast, contentDescription = null, tint = colors.ink, modifier = Modifier.size(11.dp))
        }
    }
}

// MARK: - Scan-aware message

/** Empty-state sentence; while scanning it reads "正在读取…" with a 4dp progress bar. */
@Composable
private fun InsightScanAwareMessage(idleText: String, scanningText: String, colors: InsightColors) {
    val scan by CatalogScanCoordinator.state.collectAsState()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (scan.isRunning) scanningText else idleText, style = InsightType.sentence, color = colors.ink2)
        if (scan.isRunning) {
            val progress = if (scan.insightProgress > 0f) scan.insightProgress else scan.overallProgress
            InsightProgressBar(progress, colors, Modifier.widthIn(max = 160.dp), height = 4.dp)
        }
    }
}

// MARK: - Cameras

@Composable
internal fun InsightCamerasTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val stats = env.stats
    val narrative = env.narrative
    Column(
        modifier = modifier.fillMaxWidth().insightCard(colors),
        verticalArrangement = Arrangement.spacedBy(InsightLayout.titleToContent)
    ) {
        InsightTileHeader(s.insCamerasTitle, colors, env.state, s)
        if (stats.cameraTop.isEmpty()) {
            InsightScanAwareMessage(narrative.lensLine.ifEmpty { s.insLensNone }, s.insReadingDetails, colors)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (narrative.lensLine.isNotEmpty()) {
                    Text(narrative.lensLine, style = InsightType.sentence, color = colors.ink, modifier = Modifier.padding(bottom = 4.dp))
                }
                InsightRankedList(
                    entries = stats.cameraTop,
                    total = stats.cameraTotal,
                    colors = colors,
                    s = s,
                    revealed = revealed,
                    baseDelayMillis = 80,
                    displayLabel = env.result::cameraName,
                    onSelect = { env.open(InsightDrill.Camera(it)) }
                )
            }
        }
    }
}

// MARK: - Shooting settings

@Composable
internal fun InsightShootingTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val shooting = env.stats.shooting
    val available = shooting.available
    val param = if (env.state.shootingParam in available) env.state.shootingParam else available.firstOrNull() ?: InsightShootingParam.Focal
    val distribution = shooting.distribution(param)
    Column(modifier = modifier.fillMaxWidth().insightCard(colors)) {
        InsightTileHeader(s.insShootingTitle, colors, env.state, s)
        Spacer(Modifier.height(InsightLayout.titleToContent))
        if (distribution == null) {
            Text(InsightNarrator.shootingEmpty(env.state.isScanning, s), style = InsightType.body, color = colors.ink2)
        } else {
            InsightSegmented(
                items = available.map { s.shootingParamTitle(it) },
                selectedIndex = available.indexOf(param).coerceAtLeast(0),
                colors = colors,
                height = 34.dp,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            ) { index -> env.state.shootingParam = available[index] }
            Column(
                modifier = Modifier.padding(top = InsightLayout.contentGap),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val headline = InsightNarrator.shootingHeadline(distribution, s)
                if (headline.isNotEmpty()) Text(headline, style = InsightType.sentence, color = colors.ink)
                InsightNarrator.shootingDetail(distribution, shooting, s)?.let {
                    Text(it, style = InsightType.body, color = colors.ink2)
                }
            }
            val bars = remember(distribution) {
                distribution.counts.mapIndexed { bin, count ->
                    InsightBarDatum(count, param.axisLabel(bin), param.rangeLabel(bin))
                }
            }
            InsightBarChart(
                data = bars,
                colors = colors,
                style = InsightChartStyle.Bar,
                revealed = revealed,
                numberFormat = env::numberFormat,
                height = 96.dp,
                barSpacing = 10.dp,
                growDelayMillis = 100,
                modifier = Modifier.padding(top = InsightLayout.contentGap + 4.dp),
                onTap = { bin -> env.open(InsightDrill.Shooting(param, bin)) }
            )
            Text(
                InsightNarrator.shootingBasis(distribution, s),
                style = InsightType.body,
                color = colors.ink3,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

// MARK: - How you shoot + orientation

@Composable
internal fun InsightFormatsOrientationRow(
    env: InsightTileEnv,
    formatsRevealed: Boolean,
    orientationRevealed: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(InsightLayout.gap)
    ) {
        InsightFormatsTile(env, formatsRevealed, Modifier.weight(1f).fillMaxHeight())
        InsightOrientationTile(env, orientationRevealed, Modifier.weight(1f).fillMaxHeight())
    }
}

private fun formatIcon(format: InsightFormat): ImageVector = when (format) {
    InsightFormat.Live -> Icons.Outlined.MotionPhotosOn
    InsightFormat.Panorama -> Icons.Outlined.Panorama
    InsightFormat.Raw -> Icons.Outlined.RawOn
    InsightFormat.Animated -> Icons.Outlined.Gif
    InsightFormat.ScreenRecording -> Icons.Outlined.RadioButtonChecked
}

@Composable
private fun InsightFormatsTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier) {
    val colors = env.colors
    val s = env.s
    val haptic = LocalHapticFeedback.current
    val rows = env.stats.formats.filter { it.count > 0 }.take(4)
    Column(
        modifier = modifier
            .heightIn(min = InsightLayout.halfTileMinHeight)
            .insightCard(colors)
    ) {
        InsightTileHeader(s.insFormatsTitle, colors, env.state, s)
        val lead = rows.firstOrNull()
        if (lead != null) {
            val shown = rollingValue(lead.count, revealed)
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .semantics { contentDescription = "${s.formatTitle(lead.format)}, ${s.number(lead.count)}" }
                    .insightClickable {
                        haptic.insightTap()
                        env.open(InsightDrill.Format(lead.format))
                    },
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(s.number(shown), style = InsightType.tileFigure, color = colors.ink, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(formatIcon(lead.format), contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
                    Text(s.formatTitle(lead.format), style = InsightType.smBold, color = colors.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (rows.size > 1) {
                Column(Modifier.padding(top = 10.dp)) {
                    rows.drop(1).forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 32.dp)
                                .insightRise(revealed, delayMillis = 140 + index * 35, distance = 8.dp)
                                .insightClickable {
                                    haptic.insightTap()
                                    env.open(InsightDrill.Format(row.format))
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(formatIcon(row.format), contentDescription = null, tint = colors.ink2, modifier = Modifier.size(15.dp))
                            Text(s.formatTitle(row.format), style = InsightType.body, color = colors.ink, maxLines = 2, modifier = Modifier.weight(1f))
                            Text(s.number(row.count), style = InsightType.rowValue, color = colors.ink2, maxLines = 1)
                        }
                    }
                }
            }
        } else {
            Column(Modifier.padding(top = InsightLayout.titleToContent), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(InsightFormat.Live, InsightFormat.Panorama, InsightFormat.Raw, InsightFormat.Animated).forEach {
                        Icon(formatIcon(it), contentDescription = null, tint = colors.ink3.copy(alpha = colors.ink3.alpha * 0.55f), modifier = Modifier.size(17.dp))
                    }
                }
                Text(s.insFormatsNone, style = InsightType.body, color = colors.ink2)
            }
        }
        Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
        if (env.narrative.videoLine.isNotEmpty()) {
            Text(env.narrative.videoLine, style = InsightType.body, color = colors.ink3, maxLines = 2)
        }
    }
}

private class OrientationEntry(
    val orientation: InsightOrientation,
    val title: String,
    val count: Int,
    val share: Double,
    val isDominant: Boolean,
    val order: Int
)

@Composable
private fun InsightOrientationTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier) {
    val colors = env.colors
    val s = env.s
    val split = env.stats.orientation
    val total = split.total
    val dominant = split.dominant
    val entries = InsightOrientation.entries.mapIndexed { index, o ->
        OrientationEntry(o, s.orientationTitle(o), split.count(o), split.share(o), o == dominant, index)
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .heightIn(min = InsightLayout.halfTileMinHeight)
            .insightCard(colors)
    ) {
        InsightTileHeader(s.insOrientationTitle, colors, env.state, s)
        Spacer(Modifier.weight(1f).heightIn(min = 4.dp + InsightLayout.titleToContent))
        // Column layout when every label fits a third of the tile, else rows. Width comes from the
        // screen (the tile is half of it), not BoxWithConstraints: this tile sits in an
        // IntrinsicSize row, where SubcomposeLayout cannot be measured.
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val tileInner = (screenWidth - InsightLayout.margin * 2 - InsightLayout.gap) / 2 - InsightLayout.padding * 2
        val columnWidth = (tileInner - 20.dp) / 3
        val widest = remember(entries.map { it.title }, s.language) {
            val titleWidth = entries.maxOf { measurer.widthOf(it.title, InsightType.body) }
            val percentWidth = measurer.widthOf(s.percent(1.0), InsightType.figure)
            maxOf(titleWidth, percentWidth)
        }
        val fitsColumns = with(density) { widest.toDp() } <= columnWidth
        if (fitsColumns) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                entries.forEach { entry ->
                    OrientationColumn(env, entry, total > 0, revealed, Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                entries.forEach { entry -> OrientationRow(env, entry, total > 0, revealed) }
            }
        }
    }
}

@Composable
private fun OrientationColumn(env: InsightTileEnv, entry: OrientationEntry, hasData: Boolean, revealed: Boolean, modifier: Modifier) {
    val colors = env.colors
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .semantics { contentDescription = "${entry.title}, ${env.s.percent(entry.share)}" }
            .insightClickable(enabled = entry.count > 0) {
                haptic.insightTap()
                env.open(InsightDrill.Orientation(entry.orientation))
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OrientationGlyph(entry, colors, hasData, revealed, Modifier.widthIn(max = 52.dp).fillMaxWidth(), radius = 7.dp, line = 1.5.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            OrientationPercent(env, entry, hasData, revealed, InsightType.figure, if (entry.isDominant) colors.ink else colors.ink2)
            Text(entry.title, style = InsightType.body, color = colors.ink3, maxLines = 1)
        }
    }
}

@Composable
private fun OrientationRow(env: InsightTileEnv, entry: OrientationEntry, hasData: Boolean, revealed: Boolean) {
    val colors = env.colors
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .semantics { contentDescription = "${entry.title}, ${env.s.percent(entry.share)}" }
            .insightClickable(enabled = entry.count > 0) {
                haptic.insightTap()
                env.open(InsightDrill.Orientation(entry.orientation))
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.BottomCenter) {
            val scale = if (entry.orientation == InsightOrientation.Square) 0.86f else 1f
            val glyphModifier = when (entry.orientation) {
                InsightOrientation.Vertical -> Modifier.size(width = 13.5.dp, height = 18.dp)
                InsightOrientation.Horizontal -> Modifier.size(width = 18.dp, height = 13.5.dp)
                InsightOrientation.Square -> Modifier.size(18.dp * scale)
            }
            OrientationGlyph(entry, colors, hasData, revealed, glyphModifier, radius = 3.5.dp, line = 1.25.dp, fixedSize = true)
        }
        Text(entry.title, style = InsightType.body, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        OrientationPercent(env, entry, hasData, revealed, InsightType.rowValue, if (entry.isDominant) colors.ink else colors.ink2)
    }
}

@Composable
private fun OrientationPercent(
    env: InsightTileEnv,
    entry: OrientationEntry,
    hasData: Boolean,
    revealed: Boolean,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color
) {
    val shown by animateFloatAsState(
        targetValue = if (revealed) entry.share.toFloat() else 0f,
        animationSpec = tween(700, delayMillis = 180 + entry.order * 70, easing = InsightRevealEasing),
        label = "orientation-percent"
    )
    Text(if (hasData) env.s.percent(shown.toDouble()) else "—", style = style, color = color, maxLines = 1)
}

@Composable
private fun OrientationGlyph(
    entry: OrientationEntry,
    colors: InsightColors,
    hasData: Boolean,
    revealed: Boolean,
    modifier: Modifier,
    radius: Dp,
    line: Dp,
    fixedSize: Boolean = false
) {
    val aspect = when (entry.orientation) {
        InsightOrientation.Vertical -> 3f / 4f
        InsightOrientation.Horizontal -> 4f / 3f
        InsightOrientation.Square -> 1f
    }
    val fill = rememberGrowProgress(revealed, delayMillis = 120 + entry.order * 70)
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .then(if (fixedSize) Modifier else Modifier.aspectRatio(aspect))
            .graphicsLayer { alpha = if (hasData) 1f else 0.5f }
            .clip(shape)
            .background(colors.accent.copy(alpha = colors.accent.alpha * 0.6f))
            .border(line, if (entry.isDominant) colors.primary else colors.ink3.copy(alpha = colors.ink3.alpha * 0.6f), shape),
        contentAlignment = Alignment.BottomCenter
    ) {
        val fraction = (entry.share.toFloat() * fill).coerceIn(0f, 1f)
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = fraction
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(if (entry.isDominant) colors.primary else colors.chart4)
            )
        }
    }
}

// MARK: - Subjects

@Composable
internal fun InsightSubjectsTile(env: InsightTileEnv, revealed: Boolean, modifier: Modifier = Modifier) {
    val colors = env.colors
    val s = env.s
    val haptic = LocalHapticFeedback.current
    val keywords = env.stats.keywordTop.take(24)
    val words = remember(keywords) { keywords.map { insightKeywordDisplayName(it.id) } }
    Column(
        modifier = modifier.fillMaxWidth().insightCard(colors),
        verticalArrangement = Arrangement.spacedBy(InsightLayout.titleToContent)
    ) {
        InsightTileHeader(s.insSubjectsTitle, colors, env.state, s)
        if (keywords.isEmpty()) {
            InsightScanAwareMessage(s.noTagData, s.aiTagScanning, colors)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (env.narrative.keywordsLine.isNotEmpty()) {
                    Text(env.narrative.keywordsLine, style = InsightType.sentence, color = colors.ink)
                }
                InsightWordCloud(words, colors, revealed) { index ->
                    keywords.getOrNull(index)?.let {
                        haptic.insightTap()
                        env.open(InsightDrill.Keyword(it.id))
                    }
                }
            }
        }
    }
}
