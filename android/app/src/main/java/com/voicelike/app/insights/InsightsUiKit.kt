package com.voicelike.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Insights design tokens and shared building blocks (iOS InsightTokens.swift).
// One accent colour only: MaterialTheme.colorScheme.primary (peaks, first rank, hero, heat);
// everything else is greyscale.

internal object InsightLayout {
    val margin = 16.dp
    val gap = 12.dp
    val padding = 18.dp
    val radius = 24.dp
    val titleToContent = 12.dp
    val contentGap = 16.dp
    val rowMinHeight = 40.dp
    val halfTileMinHeight = 168.dp
}

@Immutable
internal data class InsightColors(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val border: Color,
    val accent: Color,
    val chart4: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val primary: Color,
    val onPrimary: Color
) {
    /** Heat level 0...5: 0 greyscale, 1...5 primary at rising opacity. */
    fun heat(level: Int): Color {
        if (level <= 0) return accent
        return primary.copy(alpha = HEAT_ALPHAS[level.coerceAtMost(5)])
    }

    private companion object {
        val HEAT_ALPHAS = floatArrayOf(0f, 0.3f, 0.48f, 0.66f, 0.84f, 1f)
    }
}

@Composable
internal fun rememberInsightColors(): InsightColors {
    val scheme = MaterialTheme.colorScheme
    val background = scheme.surfaceVariant
    val isDark = background.luminance() < 0.5f
    val ink = scheme.onBackground
    return remember(isDark, background, ink, scheme.primary, scheme.onPrimary) {
        val ink2 = if (isDark) Color(0xFFA1A1AA) else Color(0xFF6B6B6B)
        InsightColors(
            isDark = isDark,
            background = background,
            card = if (isDark) Color(0xFF27272A) else Color.White,
            border = (if (isDark) Color(0xFF3F3F46) else Color(0xFFDCDCE3)).copy(alpha = 0.6f),
            accent = if (isDark) Color(0xFF3F3F46) else Color(0xFFDEDEE5),
            chart4 = if (isDark) Color(0xFF585858) else Color(0xFFA8A8A8),
            ink = ink,
            ink2 = ink2,
            ink3 = ink2.copy(alpha = 0.72f),
            primary = scheme.primary,
            onPrimary = scheme.onPrimary
        )
    }
}

// MARK: Typography (iOS sm 13 / base 17 / lg 24 / hero 34)

internal object InsightType {
    private const val TNUM = "tnum"
    val hero = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, fontFeatureSettings = TNUM)
    val tileFigure = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, fontFeatureSettings = TNUM)
    val figure = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TNUM)
    val tileTitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val sentence = TextStyle(fontSize = 17.sp, lineHeight = 23.sp)
    val body = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val rowLabel = TextStyle(fontSize = 17.sp)
    val rowValue = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TNUM)
    val axis = TextStyle(fontSize = 13.sp, fontFeatureSettings = TNUM)
    val smBold = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TNUM)
}

// MARK: Motion

internal val InsightRevealEasing = CubicBezierEasing(0.22f, 0.8f, 0.36f, 1f)

/** Fade + rise the first time [revealed] turns true; already-revealed content renders settled. */
@Composable
internal fun Modifier.insightRise(revealed: Boolean, delayMillis: Int = 0, distance: Dp = 16.dp): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 560, delayMillis = delayMillis, easing = InsightRevealEasing),
        label = "insight-rise"
    )
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * distance.toPx()
    }
}

/** Grow progress 0.02 -> 1 for bars and fills (spring like iOS CCMotion.wave). */
@Composable
internal fun rememberGrowProgress(revealed: Boolean, delayMillis: Int = 0): Float {
    val target = if (revealed) 1f else 0.02f
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = if (delayMillis > 0) {
            tween(durationMillis = 700, delayMillis = delayMillis, easing = InsightRevealEasing)
        } else {
            spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "insight-grow"
    )
    return progress
}

/**
 * Whether the tile [id] has played its entrance. The first time a tile is composed with data
 * ready it reveals, staggered 70 ms after any other tile revealing at the same moment.
 */
@Composable
internal fun rememberTileRevealed(state: InsightsUiState, id: String, ready: Boolean): Boolean {
    val revealed = state.revealedTiles[id] == true
    LaunchedEffect(id, ready, revealed) {
        if (ready && !revealed) {
            delay(state.nextRevealDelay())
            state.revealedTiles[id] = true
        }
    }
    return revealed
}

/** Integer that rolls from 0 when revealed (values ≥ 10), and morphs on later changes. */
@Composable
internal fun rollingValue(value: Int, revealed: Boolean): Int {
    val rolls = value >= 10
    val target = if (revealed || !rolls) value else 0
    val animated by androidx.compose.animation.core.animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700, delayMillis = if (revealed) 80 else 0, easing = InsightRevealEasing),
        label = "insight-roll"
    )
    return animated
}

// MARK: Haptics

internal fun HapticFeedback.insightTap() = performHapticFeedback(HapticFeedbackType.ContextClick)

internal fun HapticFeedback.insightTick() = performHapticFeedback(HapticFeedbackType.SegmentTick)

// MARK: Card

internal fun Modifier.insightCard(colors: InsightColors, padding: Dp = InsightLayout.padding): Modifier {
    val shape = RoundedCornerShape(InsightLayout.radius)
    val shadowed = if (colors.isDark) this else this.shadow(
        elevation = 6.dp,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.05f),
        spotColor = Color.Black.copy(alpha = 0.08f)
    )
    return shadowed
        .clip(shape)
        .background(colors.card)
        .border(0.5.dp, colors.border, shape)
        .padding(padding)
}

/** Clickable without ripple (the page uses haptics + a press-free look like iOS CCNeoPressStyle). */
@Composable
internal fun Modifier.insightClickable(enabled: Boolean = true, role: Role = Role.Button, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(enabled = enabled, interactionSource = source, indication = null, role = role, onClick = onClick)
}

// MARK: Header

internal sealed class InsightTileTrailing {
    data class Link(val title: String, val onClick: () -> Unit) : InsightTileTrailing()
    data object ChartStyle : InsightTileTrailing()
}

@Composable
internal fun InsightTileHeader(
    title: String,
    colors: InsightColors,
    state: InsightsUiState,
    s: InsightStrings,
    modifier: Modifier = Modifier,
    titleColor: Color = colors.ink2,
    trailing: InsightTileTrailing? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = InsightType.tileTitle,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        when (trailing) {
            is InsightTileTrailing.Link -> InsightLinkPill(trailing.title, colors, onClick = trailing.onClick)
            InsightTileTrailing.ChartStyle -> InsightChartStyleToggle(state, colors, s)
            null -> Unit
        }
    }
}

@Composable
internal fun InsightLinkPill(title: String, colors: InsightColors, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(colors.card)
            .border(0.5.dp, colors.border, RoundedCornerShape(11.dp))
            .insightClickable {
                haptic.insightTap()
                onClick()
            }
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, style = InsightType.smBold, color = colors.ink, maxLines = 1)
        Icon(Icons.Outlined.NorthEast, contentDescription = null, tint = colors.ink, modifier = Modifier.size(12.dp))
    }
}

// MARK: Segmented controls

/** Track tint for segmented controls. */
private fun segmentTrack(colors: InsightColors): Color =
    if (colors.isDark) Color(0xFF3F3F46).copy(alpha = 0.7f) else Color(0xFFEBEBF0)

/**
 * Text segmented control (Year / Month, shooting params). Equal-width segments; the selected one
 * sits on a card-coloured pill that slides.
 */
@Composable
internal fun InsightSegmented(
    items: List<String>,
    selectedIndex: Int,
    colors: InsightColors,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    fontSize: TextUnit = 15.sp,
    onSelect: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val radius = height * 0.38f
    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(segmentTrack(colors))
            .padding(3.dp)
    ) {
        val count = items.size.coerceAtLeast(1)
        val segment = maxWidth / count
        val offset by animateDpAsState(
            targetValue = segment * selectedIndex.coerceIn(0, count - 1),
            animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
            label = "segment"
        )
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .shadow(if (colors.isDark) 0.dp else 2.dp, RoundedCornerShape(radius - 3.dp), clip = false)
                .clip(RoundedCornerShape(radius - 3.dp))
                .background(if (colors.isDark) Color(0xFF52525B) else Color.White)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            items.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(if (selected) colors.ink else colors.ink2, label = "segment-text")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .insightClickable(role = Role.Tab) {
                            if (!selected) {
                                haptic.insightTick()
                                onSelect(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/** Icon segmented control used for the bar / line chart toggle (76 x 30). */
@Composable
internal fun InsightIconSegmented(
    icons: List<Pair<ImageVector, String>>,
    selectedIndex: Int,
    colors: InsightColors,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val height = 30.dp
    val radius = height * 0.38f
    Box(
        modifier = modifier
            .width(76.dp)
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(segmentTrack(colors))
            .padding(2.dp)
    ) {
        val segment = 72.dp / icons.size
        val offset by animateDpAsState(
            targetValue = segment * selectedIndex,
            animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
            label = "icon-segment"
        )
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(radius - 2.dp))
                .background(if (colors.isDark) Color(0xFF52525B) else Color.White)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            icons.forEachIndexed { index, (icon, label) ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { contentDescription = label }
                        .insightClickable(role = Role.Tab) {
                            if (!selected) {
                                haptic.insightTick()
                                onSelect(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = if (selected) colors.ink else colors.ink3, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
internal fun InsightChartStyleToggle(state: InsightsUiState, colors: InsightColors, s: InsightStrings) {
    InsightIconSegmented(
        icons = listOf(Icons.Outlined.BarChart to s.chartStyleBar, Icons.AutoMirrored.Outlined.ShowChart to s.chartStyleLine),
        selectedIndex = if (state.chartStyle == InsightChartStyle.Line) 1 else 0,
        colors = colors
    ) { index ->
        state.updateChartStyle(if (index == 1) InsightChartStyle.Line else InsightChartStyle.Bar)
    }
}

// MARK: Figures

/** Small figure + label (17 bold / 13), optionally tappable. */
@Composable
internal fun InsightFigure(
    value: String,
    label: String,
    colors: InsightColors,
    modifier: Modifier = Modifier,
    isZero: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier.then(
            if (onClick != null && !isZero) {
                Modifier.insightClickable {
                    haptic.insightTap()
                    onClick()
                }
            } else {
                Modifier
            }
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = InsightType.figure, color = if (isZero) colors.ink3 else colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = InsightType.body, color = colors.ink3, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Thin capsule progress bar. */
@Composable
internal fun InsightProgressBar(
    fraction: Float,
    colors: InsightColors,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    track: Color = colors.accent,
    fill: Color = colors.primary,
    grow: Float = 1f,
    minFill: Dp = height
) {
    // Drawn, not laid out: safe inside IntrinsicSize rows (no SubcomposeLayout).
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(track, cornerRadius = radius)
                val clamped = fraction.coerceIn(0f, 1f)
                if (clamped > 0f) {
                    val width = (size.width * clamped).coerceAtLeast(minFill.toPx()).coerceAtMost(size.width) * grow
                    drawRoundRect(fill, size = Size(width, size.height), cornerRadius = radius)
                }
            }
    )
}

// MARK: Ranked list

/**
 * Ranking rows: rank column (first in primary), label, count, share, 4dp bar underneath
 * (first primary, others chart4). Optional "N more" row.
 */
@Composable
internal fun InsightRankedList(
    entries: List<InsightRankedEntry>,
    total: Int,
    colors: InsightColors,
    s: InsightStrings,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    maxRows: Int = 5,
    baseDelayMillis: Int = 0,
    displayLabel: (String) -> String = { it },
    moreTitle: String? = null,
    onMore: (() -> Unit)? = null,
    onSelect: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val rows = entries.take(maxRows)
    val maxCount = (rows.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, entry ->
            val isFirst = index == 0
            val delay = baseDelayMillis + (index * 35).coerceAtMost(360)
            val grow = rememberGrowProgress(revealed, delayMillis = delay + 80)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = InsightLayout.rowMinHeight)
                    .insightRise(revealed, delayMillis = delay, distance = 8.dp)
                    .insightClickable {
                        haptic.insightTap()
                        onSelect(entry.id)
                    }
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${index + 1}",
                        style = InsightType.rowValue,
                        color = if (isFirst) colors.primary else colors.ink3,
                        modifier = Modifier.width(22.dp)
                    )
                    Text(
                        text = displayLabel(entry.id),
                        style = InsightType.rowLabel,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(s.number(entry.count), style = InsightType.rowValue, color = colors.ink2, maxLines = 1)
                    if (total > 0) {
                        Text(
                            text = s.percent(entry.count.toDouble() / total),
                            style = InsightType.axis,
                            color = colors.ink3,
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            modifier = Modifier.widthIn(min = 40.dp)
                        )
                    }
                }
                InsightProgressBar(
                    fraction = entry.count.toFloat() / maxCount,
                    colors = colors,
                    height = 4.dp,
                    track = colors.accent.copy(alpha = colors.accent.alpha * 0.6f),
                    fill = if (isFirst) colors.primary else colors.chart4,
                    grow = grow,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
        }
        if (moreTitle != null && onMore != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = InsightLayout.rowMinHeight)
                    .insightRise(revealed, delayMillis = baseDelayMillis + rows.size * 35, distance = 8.dp)
                    .insightClickable {
                        haptic.insightTap()
                        onMore()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(moreTitle, style = InsightType.smBold, color = colors.primary)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
            }
        }
    }
}

// MARK: Labels

/**
 * Place list label: drops repeated parts ("Beijing, Beijing", "北京市, 北京" -> first part only),
 * keeps the normalised label otherwise (iOS InsightFootprintTile.listLabel).
 */
internal fun insightPlaceListLabel(raw: String): String {
    val parts = raw.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size <= 1) return parts.firstOrNull() ?: raw
    fun core(value: String): String {
        var v = value.lowercase()
        while (v.endsWith("市") || v.endsWith("省")) v = v.dropLast(1)
        return v
    }
    val unique = ArrayList<String>()
    for (part in parts) if (unique.none { core(it) == core(part) }) unique.add(part)
    return unique.joinToString(", ")
}
