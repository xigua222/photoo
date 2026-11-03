package com.voicelike.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// Hand-drawn insight charts (iOS InsightBarChart / calendar dot canvas / word cloud). Each chart
// is one Canvas: no per-bar composables, so the long page stays cheap with 50k+ items.

@Immutable
internal data class InsightBarDatum(
    val value: Int,
    /** Axis label (null = none). */
    val label: String?,
    /** Scrub callout name ("10月14日", "2019"). */
    val callout: String,
    val isFuture: Boolean = false
)

/** highlightIndex value meaning "no highlight" (sparse charts: everything greyscale, no peak value). */
internal const val INSIGHT_NO_HIGHLIGHT = -2

private val PEAK_LABEL_HEIGHT = 20.dp
private val AXIS_GAP = 6.dp
private val AXIS_HEIGHT = 16.dp

/**
 * Bar / line chart for a time series or bins. [highlightIndex] null picks the peak; tap reports
 * the index of a non-future bar with a value; [scrubbable] adds long-press scrubbing with a callout.
 */
@Composable
internal fun InsightBarChart(
    data: List<InsightBarDatum>,
    colors: InsightColors,
    style: InsightChartStyle,
    revealed: Boolean,
    numberFormat: (Int) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    barSpacing: Dp = 4.dp,
    highlightIndex: Int? = null,
    showsPeakValue: Boolean = true,
    scrubbable: Boolean = false,
    maxBarWidth: Dp = Dp.Infinity,
    growDelayMillis: Int = 0,
    onTap: ((Int) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val grow = rememberGrowProgress(revealed, growDelayMillis)
    val lineMorph by animateFloatAsState(
        targetValue = if (style == InsightChartStyle.Line) 1f else 0f,
        animationSpec = tween(320),
        label = "chart-style"
    )
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    var scrubActive by remember { mutableStateOf(false) }
    LaunchedEffect(scrubActive) {
        if (!scrubActive && scrubIndex != null) {
            delay(400)
            scrubIndex = null
        }
    }
    val currentData by rememberUpdatedState(data)
    val currentOnTap by rememberUpdatedState(onTap)

    val hasLabels = remember(data) { data.any { it.label != null } }
    val peak = remember(data, highlightIndex) {
        when {
            highlightIndex == INSIGHT_NO_HIGHLIGHT -> null
            highlightIndex != null -> highlightIndex.takeIf { it in data.indices }
            else -> {
                var best = -1
                var bestValue = 0
                data.forEachIndexed { i, d -> if (!d.isFuture && d.value > bestValue) { best = i; bestValue = d.value } }
                best.takeIf { it >= 0 }
            }
        }
    }
    val maxValue = remember(data) { max(data.maxOfOrNull { it.value } ?: 0, 1) }
    val summary = remember(data) {
        data.filter { !it.isFuture }.joinToString(", ") { "${it.callout} ${numberFormat(it.value)}" }
    }
    val totalHeight = height + if (hasLabels) AXIS_GAP + AXIS_HEIGHT else 0.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .semantics { contentDescription = summary }
            .pointerInput(scrubbable) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val count = currentData.size
                    if (count == 0) return@awaitEachGesture
                    fun indexAt(x: Float): Int = ((x / size.width.coerceAtLeast(1)) * count).toInt().coerceIn(0, count - 1)
                    val slop = viewConfiguration.touchSlop
                    // 1 = tap, 2 = cancelled (moved / consumed), null = held long enough to scrub
                    val outcome = withTimeoutOrNull(if (scrubbable) 200L else 60_000L) {
                        var r = 0
                        while (r == 0) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            r = when {
                                change == null -> 2
                                change.changedToUpIgnoreConsumed() -> if (change.isConsumed) 2 else 1
                                change.isConsumed -> 2
                                (change.position - down.position).getDistance() > slop -> 2
                                else -> 0
                            }
                        }
                        r
                    }
                    when (outcome) {
                        1 -> {
                            val index = indexAt(down.position.x)
                            val datum = currentData.getOrNull(index)
                            val tap = currentOnTap
                            if (tap != null && datum != null && !datum.isFuture && datum.value > 0) {
                                haptic.insightTap()
                                tap(index)
                            }
                        }
                        null -> if (scrubbable) {
                            scrubActive = true
                            fun update(x: Float) {
                                val index = indexAt(x)
                                if (currentData.getOrNull(index)?.isFuture == false && index != scrubIndex) {
                                    haptic.insightTick()
                                    scrubIndex = index
                                }
                            }
                            update(down.position.x)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                change.consume()
                                update(change.position.x)
                            }
                            scrubActive = false
                        }
                        else -> Unit
                    }
                }
            }
    ) {
        val n = data.size
        if (n == 0) return@Canvas
        val plotBottom = height.toPx()
        val plotHeight = max(plotBottom - if (showsPeakValue) PEAK_LABEL_HEIGHT.toPx() else 0f, 1f)
        val spacing = barSpacing.toPx()
        val slotWidth = max(2f, (size.width - spacing * (n - 1)) / n)
        val barWidth = min(slotWidth, maxBarWidth.toPx())
        fun centerX(i: Int) = i * (slotWidth + spacing) + slotWidth / 2
        fun valueY(v: Int) = plotBottom - plotHeight * v / maxValue
        val scrubbed = scrubIndex
        val accentColor = colors.accent
        val primary = colors.primary

        // Bars (fade out while morphing to the line)
        if (lineMorph < 1f) {
            val barAlpha = 1f - lineMorph
            scale(scaleX = 1f, scaleY = grow, pivot = Offset(0f, plotBottom)) {
                for (i in 0 until n) {
                    val d = data[i]
                    if (d.isFuture) continue
                    val h = if (d.value == 0) 3.dp.toPx() else max(6.dp.toPx(), plotHeight * d.value / maxValue)
                    val left = centerX(i) - barWidth / 2
                    val radius = min(barWidth / 2, 5.dp.toPx())
                    val highlighted = i == peak && scrubbed == null || i == scrubbed
                    drawRoundRect(
                        color = if (highlighted) primary else accentColor,
                        topLeft = Offset(left, plotBottom - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(radius, radius),
                        alpha = barAlpha
                    )
                }
            }
        }
        // Line
        if (lineMorph > 0f) {
            val lineColor = if (peak == null) colors.ink3 else primary
            val points = ArrayList<Offset>(n)
            for (i in 0 until n) if (!data[i].isFuture) points.add(Offset(centerX(i), valueY(data[i].value)))
            if (points.isNotEmpty()) {
                scale(scaleX = 1f, scaleY = grow, pivot = Offset(0f, plotBottom)) {
                    val area = insightMonotonePath(points, closedTo = plotBottom)
                    drawPath(
                        area,
                        Brush.verticalGradient(
                            listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0.02f)),
                            startY = plotBottom - plotHeight,
                            endY = plotBottom
                        ),
                        alpha = lineMorph
                    )
                    drawPath(
                        insightMonotonePath(points, closedTo = null),
                        lineColor,
                        alpha = lineMorph,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    if (scrubbed != null && data.getOrNull(scrubbed)?.isFuture == false) {
                        val x = centerX(scrubbed)
                        drawLine(
                            colors.ink3.copy(alpha = 0.6f),
                            Offset(x, plotBottom - plotHeight),
                            Offset(x, plotBottom),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                            alpha = lineMorph
                        )
                        drawPeakDot(Offset(x, valueY(data[scrubbed].value)), lineColor, colors.card, lineMorph)
                    }
                    if (peak != null && data[peak].value > 0) {
                        drawPeakDot(Offset(centerX(peak), valueY(data[peak].value)), lineColor, colors.card, lineMorph)
                    }
                }
            }
        }
        // Future slots: small dots on the baseline
        for (i in 0 until n) {
            if (!data[i].isFuture) continue
            val dot = min(slotWidth, 4.dp.toPx())
            drawCircle(accentColor, radius = dot / 2, center = Offset(centerX(i), plotBottom - 2.dp.toPx()))
        }
        // Peak value
        if (showsPeakValue && scrubbed == null && peak != null && data[peak].value > 0 && grow > 0.6f) {
            val layout = measurer.measure(numberFormat(data[peak].value), InsightType.axis.copy(color = primary))
            val barTop = plotBottom - max(6.dp.toPx(), plotHeight * data[peak].value / maxValue)
            val lineTop = valueY(data[peak].value) - 14.dp.toPx() + layout.size.height / 2f
            val top = barTop - 4.dp.toPx() + (lineTop - (barTop - 4.dp.toPx())) * lineMorph
            val x = (centerX(peak) - layout.size.width / 2f).coerceIn(0f, max(0f, size.width - layout.size.width))
            drawText(layout, topLeft = Offset(x, top - layout.size.height), alpha = ((grow - 0.6f) / 0.4f).coerceIn(0f, 1f))
        }
        // Axis labels
        if (hasLabels) {
            val top = plotBottom + AXIS_GAP.toPx()
            val slot = slotWidth + spacing
            for (i in 0 until n) {
                val label = data[i].label ?: continue
                val color = if (i == peak) colors.ink else colors.ink3
                var layout = measurer.measure(label, InsightType.axis.copy(color = color), maxLines = 1, softWrap = false)
                if (layout.size.width > slot && slot > 0) {
                    val factor = max(0.5f, slot / layout.size.width)
                    layout = measurer.measure(
                        label,
                        InsightType.axis.copy(color = color, fontSize = 13.sp * factor),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                val x = (centerX(i) - layout.size.width / 2f).coerceIn(0f, max(0f, size.width - layout.size.width))
                drawText(layout, topLeft = Offset(x, top))
            }
        }
        // Scrub callout
        if (scrubbed != null) {
            val d = data.getOrNull(scrubbed)
            if (d != null) {
                val layout = measurer.measure("${d.callout} ${numberFormat(d.value)}", InsightType.smBold.copy(color = colors.ink), maxLines = 1)
                val pillHeight = 26.dp.toPx()
                val pillWidth = layout.size.width + 18.dp.toPx()
                val cx = centerX(scrubbed).coerceIn(pillWidth / 2, max(pillWidth / 2, size.width - pillWidth / 2))
                val top = -8.dp.toPx() - pillHeight / 2
                val radius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
                drawRoundRect(colors.card, Offset(cx - pillWidth / 2, top), Size(pillWidth, pillHeight), radius)
                drawRoundRect(colors.border, Offset(cx - pillWidth / 2, top), Size(pillWidth, pillHeight), radius, style = Stroke(0.5.dp.toPx()))
                drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, top + (pillHeight - layout.size.height) / 2f))
            }
        }
    }
}

private fun DrawScope.drawPeakDot(center: Offset, color: Color, ring: Color, alpha: Float) {
    drawCircle(color, radius = 4.dp.toPx(), center = center, alpha = alpha)
    drawCircle(ring, radius = 4.dp.toPx() - 0.75.dp.toPx(), center = center, alpha = alpha, style = Stroke(1.5.dp.toPx()))
}

/**
 * Monotone cubic (Fritsch–Carlson): passes through every point and never overshoots between two,
 * so the curve never draws a peak higher than the data. [closedTo] closes it into an area.
 */
internal fun insightMonotonePath(points: List<Offset>, closedTo: Float?): Path {
    val path = Path()
    val first = points.firstOrNull() ?: return path
    path.moveTo(first.x, first.y)
    val n = points.size
    if (n > 1) {
        val slopes = FloatArray(n - 1)
        for (i in 0 until n - 1) {
            val dx = points[i + 1].x - points[i].x
            slopes[i] = if (dx == 0f) 0f else (points[i + 1].y - points[i].y) / dx
        }
        val tangents = FloatArray(n)
        tangents[0] = slopes[0]
        tangents[n - 1] = slopes[n - 2]
        for (i in 1 until n - 1) {
            val a = slopes[i - 1]
            val b = slopes[i]
            tangents[i] = if (a * b <= 0f) 0f else 2 * a * b / (a + b)
        }
        for (i in 0 until n - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val dx = (p1.x - p0.x) / 3
            path.cubicTo(p0.x + dx, p0.y + tangents[i] * dx, p1.x - dx, p1.y - tangents[i + 1] * dx, p1.x, p1.y)
        }
    }
    if (closedTo != null) {
        val last = points.last()
        path.lineTo(last.x, closedTo)
        path.lineTo(first.x, closedTo)
        path.close()
    }
    return path
}

// MARK: - Activity calendar

/** Per-day cell model for one year: index = (month - 1) * 31 + (day - 1). */
@Immutable
internal class InsightCalendarYearModel(
    val year: Int,
    /** 0 = no such day, 1 = before library start / future, 2 = in range. */
    val phase: ByteArray,
    val level: ByteArray,
    val count: IntArray,
    val todayIndex: Int
) {
    fun monthHasRange(month0: Int): Boolean = (0 until 31).any { phase[month0 * 31 + it] == PHASE_IN_RANGE }
    fun monthHasPhotos(month0: Int): Boolean = (0 until 31).any { count[month0 * 31 + it] > 0 }

    companion object {
        const val PHASE_NONE: Byte = 0
        const val PHASE_OUT: Byte = 1
        const val PHASE_IN_RANGE: Byte = 2

        fun build(year: Int, heatmap: Map<String, Int>, thresholds: List<Int>, firstDay: Int?, today: Int): InsightCalendarYearModel {
            val phase = ByteArray(12 * 31)
            val level = ByteArray(12 * 31)
            val count = IntArray(12 * 31)
            for ((key, value) in heatmap) {
                val dv = InsightStatsEngine.parseDayKey(key) ?: continue
                if (dv / 10000 != year) continue
                count[((dv / 100) % 100 - 1) * 31 + dv % 100 - 1] = value
            }
            var todayIndex = -1
            for (m in 1..12) {
                for (d in 1..insightDaysInMonth(year, m)) {
                    val index = (m - 1) * 31 + d - 1
                    val dv = year * 10000 + m * 100 + d
                    val inRange = (firstDay == null || dv >= firstDay) && dv <= today
                    phase[index] = if (inRange) PHASE_IN_RANGE else PHASE_OUT
                    if (!inRange) count[index] = 0
                    level[index] = InsightStatsEngine.heatLevel(count[index], thresholds).toByte()
                    if (dv == today) todayIndex = index
                }
            }
            return InsightCalendarYearModel(year, phase, level, count, todayIndex)
        }
    }
}

/** Year: month labels + 12 x 31 dot grid, revealed by a left-to-right wipe. */
@Composable
internal fun InsightCalendarYearMatrix(
    model: InsightCalendarYearModel,
    monthLabels: List<String>,
    colors: InsightColors,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    onSelectDay: (dayValue: Int) -> Unit
) {
    val measurer = rememberTextMeasurer(cacheSize = 24)
    val density = LocalDensity.current
    val rowPitch = 17.dp
    val wipe = remember { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(revealed) {
        if (revealed && wipe.value < 1f) wipe.animateTo(1f, tween(1000, delayMillis = 80, easing = InsightRevealEasing))
    }
    val labelLayouts = remember(monthLabels, model, colors) {
        monthLabels.mapIndexed { index, label ->
            val color = when {
                !model.monthHasRange(index) -> colors.ink3.copy(alpha = colors.ink3.alpha * 0.5f)
                model.monthHasPhotos(index) -> colors.ink2
                else -> colors.ink3
            }
            measurer.measure(label, InsightType.axis.copy(color = color), maxLines = 1, softWrap = false)
        }
    }
    val labelWidth = labelLayouts.maxOfOrNull { it.size.width } ?: 0
    val gridStart = with(density) { labelWidth + 10.dp.toPx() }
    val currentModel by rememberUpdatedState(model)
    val currentSelect by rememberUpdatedState(onSelectDay)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(rowPitch * 12)
            .pointerInput(gridStart) {
                detectTapGestures { offset ->
                    val gridWidth = size.width - gridStart
                    if (gridWidth <= 0f || offset.x < gridStart) return@detectTapGestures
                    val column = ((offset.x - gridStart) / (gridWidth / 31)).toInt()
                    val row = (offset.y / (size.height / 12f)).toInt()
                    if (row !in 0 until 12 || column !in 0 until 31) return@detectTapGestures
                    val m = currentModel
                    val index = row * 31 + column
                    if (m.phase[index] == InsightCalendarYearModel.PHASE_IN_RANGE && m.count[index] > 0) {
                        currentSelect(m.year * 10000 + (row + 1) * 100 + column + 1)
                    }
                }
            }
    ) {
        val pitch = size.height / 12f
        labelLayouts.forEachIndexed { index, layout ->
            drawText(layout, topLeft = Offset(0f, index * pitch + (pitch - layout.size.height) / 2f))
        }
        val gridWidth = size.width - gridStart
        if (gridWidth <= 0f) return@Canvas
        val columnPitch = gridWidth / 31f
        val dot = min(columnPitch, pitch) * 0.8f
        val idle = dot * 0.66f
        val faint = colors.accent.copy(alpha = colors.accent.alpha * 0.45f)
        clipRect(right = gridStart + gridWidth * 1.05f * wipe.value) {
            for (index in 0 until 12 * 31) {
                val phase = model.phase[index]
                if (phase == InsightCalendarYearModel.PHASE_NONE) continue
                val cx = gridStart + (index % 31 + 0.5f) * columnPitch
                val cy = (index / 31 + 0.5f) * pitch
                if (phase == InsightCalendarYearModel.PHASE_OUT) {
                    drawCircle(faint, radius = idle / 2, center = Offset(cx, cy))
                } else {
                    val level = model.level[index].toInt()
                    drawCircle(colors.heat(level), radius = (if (level == 0) idle else dot) / 2, center = Offset(cx, cy))
                }
                if (index == model.todayIndex) {
                    drawCircle(
                        colors.ink.copy(alpha = 0.75f),
                        radius = (dot + 4.5.dp.toPx()) / 2,
                        center = Offset(cx, cy),
                        style = Stroke(1.2.dp.toPx())
                    )
                }
            }
        }
    }
}

/** Month: weekday header + 7-column calendar of circles with the date inside. */
@Composable
internal fun InsightCalendarMonthGrid(
    model: InsightCalendarYearModel,
    month: Int,
    leadingBlanks: Int,
    weekdayHeaders: List<String>,
    colors: InsightColors,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    onSelectDay: (dayValue: Int) -> Unit
) {
    val measurer = rememberTextMeasurer(cacheSize = 96)
    val days = insightDaysInMonth(model.year, month)
    val slots = leadingBlanks + days
    val rows = (slots + 6) / 7
    val rowSpacing = 4.dp
    val wave = remember(model.year, month) { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(revealed, model.year, month) {
        if (revealed && wave.value < 1f) wave.animateTo(1f, tween(820, easing = LinearEasing))
    }
    val currentModel by rememberUpdatedState(model)
    val currentSelect by rememberUpdatedState(onSelectDay)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            weekdayHeaders.forEach { symbol ->
                Text(
                    text = symbol,
                    style = InsightType.axis,
                    color = colors.ink3,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cellWidth = maxWidth / 7
            val cellHeight = cellWidth * 0.84f
            val gridHeight = cellHeight * rows + rowSpacing * (rows - 1).coerceAtLeast(0)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .pointerInput(leadingBlanks, month) {
                        detectTapGestures { offset ->
                            val cw = size.width / 7f
                            val ch = cw * 0.84f
                            val column = (offset.x / cw).toInt()
                            val row = (offset.y / (ch + rowSpacing.toPx())).toInt()
                            if (column !in 0 until 7 || row < 0) return@detectTapGestures
                            val day = row * 7 + column - leadingBlanks + 1
                            if (day !in 1..days) return@detectTapGestures
                            val m = currentModel
                            val index = (month - 1) * 31 + day - 1
                            if (m.phase[index] == InsightCalendarYearModel.PHASE_IN_RANGE && m.count[index] > 0) {
                                currentSelect(m.year * 10000 + month * 100 + day)
                            }
                        }
                    }
            ) {
                val cw = size.width / 7f
                val ch = cw * 0.84f
                val gap = rowSpacing.toPx()
                val diameter = min(cw, ch) - 7.dp.toPx()
                val faint = colors.accent.copy(alpha = colors.accent.alpha * 0.45f)
                for (day in 1..days) {
                    val slot = leadingBlanks + day - 1
                    val row = slot / 7
                    val column = slot % 7
                    val index = (month - 1) * 31 + day - 1
                    val phase = model.phase[index]
                    val level = model.level[index].toInt()
                    val delay = min((row + column) * 0.028f, 0.32f)
                    val local = ((wave.value * 0.82f - delay) / 0.5f).coerceIn(0f, 1f)
                    val eased = InsightRevealEasing.transform(local)
                    if (eased <= 0f) continue
                    val center = Offset((column + 0.5f) * cw, row * (ch + gap) + ch / 2)
                    val radius = diameter / 2 * (0.55f + 0.45f * eased)
                    val inRange = phase == InsightCalendarYearModel.PHASE_IN_RANGE
                    drawCircle(if (inRange) colors.heat(level) else faint, radius = radius, center = center, alpha = eased)
                    val numberColor = when {
                        !inRange -> colors.ink3.copy(alpha = colors.ink3.alpha * 0.6f)
                        level == 0 -> colors.ink2
                        level >= 3 -> colors.onPrimary
                        else -> colors.ink
                    }
                    val layout = measurer.measure(
                        "$day",
                        TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", color = numberColor),
                        maxLines = 1
                    )
                    drawText(
                        layout,
                        topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
                        alpha = eased
                    )
                    if (index == model.todayIndex) {
                        drawCircle(
                            colors.ink.copy(alpha = 0.75f),
                            radius = diameter / 2 + 3.5.dp.toPx(),
                            center = center,
                            style = Stroke(1.5.dp.toPx()),
                            alpha = eased
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Word cloud

private class CloudLayout(val frames: List<Rect>, val bounds: Rect)

private object CloudMemo {
    private val memo = LinkedHashMap<String, CloudLayout>()

    @Synchronized
    fun get(key: String): CloudLayout? = memo[key]

    @Synchronized
    fun put(key: String, layout: CloudLayout) {
        if (memo.size > 32) memo.clear()
        memo[key] = layout
    }
}

/** Places padded word boxes on an elliptical Archimedean spiral; the first word sits in the centre. */
private fun arrangeCloud(sizes: List<Size>, width: Float, spacing: Float, unit: Float): CloudLayout {
    val half = width / 2
    val placed = ArrayList<Rect>(sizes.size)
    for (size in sizes) {
        val w = min(size.width + spacing, width)
        val h = size.height + spacing
        var frame = Rect(-w / 2, -h / 2, w / 2, h / 2)
        if (placed.isNotEmpty()) {
            var angle = 0f
            var found = false
            for (step in 0 until 12000) {
                angle += 0.1f
                val radius = 2.2f * unit * angle
                val cx = radius * cos(angle) * 2f
                val cy = radius * sin(angle)
                val candidate = Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
                if (candidate.left < -half || candidate.right > half) continue
                if (placed.none { it.overlaps(candidate) }) {
                    frame = candidate
                    found = true
                    break
                }
            }
            if (!found) {
                val bottom = placed.maxOf { it.bottom }
                frame = Rect(-w / 2, bottom, w / 2, bottom + h)
            }
        }
        placed.add(frame)
    }
    var bounds = placed.first()
    for (r in placed) bounds = Rect(min(bounds.left, r.left), min(bounds.top, r.top), max(bounds.right, r.right), max(bounds.bottom, r.bottom))
    return CloudLayout(placed, bounds)
}

/**
 * Word cloud (≤ 24 words): size 12 -> 28 sp by rank (pow 2.2), bold top 5, colour rank 0 primary,
 * 1–4 ink, 5–11 ink2, rest ink3. Layout runs off the main thread and is memoised.
 */
@Composable
internal fun InsightWordCloud(
    words: List<String>,
    colors: InsightColors,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    onTap: (Int) -> Unit
) {
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val density = LocalDensity.current
    val layouts: List<TextLayoutResult> = remember(words, colors) {
        val count = words.size
        words.mapIndexed { rank, word ->
            val t = if (count > 1) 1f - rank.toFloat() / (count - 1) else 1f
            val fontSize = 12f + 16f * t.toDouble().pow(2.2).toFloat()
            val weight = when {
                rank < 5 -> FontWeight.Bold
                rank < 12 -> FontWeight.SemiBold
                else -> FontWeight.Medium
            }
            val color = when (rank) {
                0 -> colors.primary
                in 1..4 -> colors.ink
                in 5..11 -> colors.ink2
                else -> colors.ink3
            }
            measurer.measure(word, TextStyle(fontSize = fontSize.sp, fontWeight = weight, color = color), maxLines = 1, softWrap = false)
        }
    }
    val appear = remember { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(revealed) {
        if (revealed && appear.value < 1f) appear.animateTo(1f, tween(900, delayMillis = 80, easing = LinearEasing))
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val padPx = with(density) { 2.dp.toPx() }
        val spacingPx = with(density) { 6.dp.toPx() }
        val unit = density.density
        val sizes = remember(layouts) { layouts.map { Size(it.size.width + padPx * 2, it.size.height.toFloat()) } }
        val key = remember(sizes, widthPx) {
            "${widthPx.toInt()}|" + sizes.joinToString(",") { "${it.width.toInt()}x${it.height.toInt()}" }
        }
        val cloud by produceState(initialValue = CloudMemo.get(key), key) {
            if (value == null && sizes.isNotEmpty()) {
                value = withContext(Dispatchers.Default) {
                    arrangeCloud(sizes, widthPx, spacingPx, unit).also { CloudMemo.put(key, it) }
                }
            }
        }
        val current = cloud
        if (current == null) {
            Spacer(Modifier.height(160.dp))
        } else {
            val heightDp = with(density) { current.bounds.height.toDp() }
            val currentTap by rememberUpdatedState(onTap)
            Canvas(
                modifier = Modifier
                    .width(maxWidth)
                    .height(heightDp)
                    .semantics { contentDescription = words.joinToString(", ") }
                    .pointerInput(current) {
                        detectTapGestures { offset ->
                            val dx = size.width / 2f - current.bounds.center.x
                            val dy = -current.bounds.top
                            val hit = current.frames.indexOfFirst { it.translate(dx, dy).contains(offset) }
                            if (hit >= 0) currentTap(hit)
                        }
                    }
            ) {
                val dx = size.width / 2f - current.bounds.center.x
                val dy = -current.bounds.top
                translate(dx, dy) {
                    layouts.forEachIndexed { index, layout ->
                        val frame = current.frames.getOrNull(index) ?: return@forEachIndexed
                        val order = min(index, 12)
                        val local = ((appear.value * 0.9f - order * 0.04f) / 0.4f).coerceIn(0f, 1f)
                        val eased = InsightRevealEasing.transform(local)
                        if (eased <= 0f) return@forEachIndexed
                        drawText(
                            layout,
                            topLeft = Offset(
                                frame.center.x - layout.size.width / 2f,
                                frame.center.y - layout.size.height / 2f + (1f - eased) * 6.dp.toPx()
                            ),
                            alpha = eased
                        )
                    }
                }
            }
        }
    }
}

/** Measures how wide [text] renders in [style] (px). */
internal fun TextMeasurer.widthOf(text: String, style: TextStyle): Int =
    measure(text, style, maxLines = 1, softWrap = false).size.width
