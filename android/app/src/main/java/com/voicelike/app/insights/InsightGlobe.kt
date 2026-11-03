package com.voicelike.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** One period cluster (0.25 degree cell); [weight] is its photo count. */
data class InsightGlobePoint(val latitude: Double, val longitude: Double, val weight: Int)

/** A named place shown as a small card next to its dot while it faces the viewer. */
data class InsightGlobeLabel(val latitude: Double, val longitude: Double, val title: String, val count: Int)

private const val INSIGHT_GLOBE_PITCH_LIMIT = 1.25f
private const val INSIGHT_GLOBE_ZOOM_MIN = 0.8f
private const val INSIGHT_GLOBE_ZOOM_MAX = 3.0f
private const val INSIGHT_GLOBE_PREVIEW_SPIN = -0.16f // rad/s; negative = surface drifts west to east
private const val INSIGHT_GLOBE_INTERACTIVE_SPIN = -0.10f
private const val INSIGHT_GLOBE_PREVIEW_FRAME_NS = 33_000_000L // preview redraws at ~30fps
private const val INSIGHT_GLOBE_RESET_NS = 520_000_000L
private const val INSIGHT_GLOBE_LABEL_MIN_DEPTH = 0.16f
private const val INSIGHT_GLOBE_TITLE_MAX_CHARS = 14
private const val INSIGHT_GLOBE_DEFAULT_LAT = 25.0
private const val INSIGHT_GLOBE_DEFAULT_LON = 105.0

/** Coastlines packed as unit vectors; built once per detail level and shared by every InsightGlobe. */
internal class InsightCoastlineGeometry(
    val xs: FloatArray,
    val ys: FloatArray,
    val zs: FloatArray,
    /** Line i spans [lineStarts[i], lineStarts[i + 1]). */
    val lineStarts: IntArray
) {
    val lineCount: Int get() = lineStarts.size - 1
}

internal object InsightGlobeGeometryCache {
    private val cache = ConcurrentHashMap<CoastlineDetail, InsightCoastlineGeometry>()

    /** Blocking (parses the asset through [CoastlineRepository] on first use); call off the main thread. */
    fun load(context: android.content.Context, detail: CoastlineDetail): InsightCoastlineGeometry {
        cache[detail]?.let { return it }
        val lines = CoastlineRepository.load(context.applicationContext, detail)
        val total = lines.sumOf { it.size }
        val xs = FloatArray(total)
        val ys = FloatArray(total)
        val zs = FloatArray(total)
        val starts = IntArray(lines.size + 1)
        var cursor = 0
        lines.forEachIndexed { index, line ->
            starts[index] = cursor
            line.forEach { (lat, lon) ->
                val latR = Math.toRadians(lat)
                val lonR = Math.toRadians(lon)
                val cosLat = cos(latR)
                xs[cursor] = (cosLat * cos(lonR)).toFloat()
                ys[cursor] = sin(latR).toFloat()
                zs[cursor] = (cosLat * sin(lonR)).toFloat()
                cursor++
            }
        }
        starts[lines.size] = cursor
        val geometry = InsightCoastlineGeometry(xs, ys, zs, starts)
        return cache.putIfAbsent(detail, geometry) ?: geometry
    }
}

/** Yaw/pitch that bring (lat, lon) to the centre of the disc, matching the projection below. */
private fun focusAngles(lat: Double, lon: Double): Pair<Float, Float> {
    val yaw = (Math.toRadians(lon) - PI / 2.0).toFloat()
    val pitch = Math.toRadians(lat).toFloat().coerceIn(-INSIGHT_GLOBE_PITCH_LIMIT, INSIGHT_GLOBE_PITCH_LIMIT)
    return yaw to pitch
}

/** Weighted centre on the sphere (3D mean), so clusters on both sides of the antimeridian average correctly. */
private fun weightedCentre(points: List<InsightGlobePoint>): Pair<Double, Double>? {
    if (points.isEmpty()) return null
    var sx = 0.0
    var sy = 0.0
    var sz = 0.0
    points.forEach { p ->
        val w = max(1, p.weight).toDouble()
        val latR = Math.toRadians(p.latitude)
        val lonR = Math.toRadians(p.longitude)
        val cosLat = cos(latR)
        sx += w * cosLat * cos(lonR)
        sy += w * sin(latR)
        sz += w * cosLat * sin(lonR)
    }
    val len = sqrt(sx * sx + sy * sy + sz * sz)
    if (len < 1e-9) return points.first().latitude to points.first().longitude
    val lat = Math.toDegrees(kotlin.math.asin((sy / len).coerceIn(-1.0, 1.0)))
    val lon = Math.toDegrees(atan2(sz, sx))
    return lat to lon
}

private fun wrapAngle(a: Float): Float {
    var x = a % (2f * PI.toFloat())
    if (x > PI.toFloat()) x -= 2f * PI.toFloat()
    if (x < -PI.toFloat()) x += 2f * PI.toFloat()
    return x
}

private fun easeOutCubicInsight(t: Float): Float {
    val inv = 1f - t.coerceIn(0f, 1f)
    return 1f - inv * inv * inv
}

private fun truncateTitle(title: String): String {
    val trimmed = title.trim()
    val count = trimmed.codePointCount(0, trimmed.length)
    if (count <= INSIGHT_GLOBE_TITLE_MAX_CHARS) return trimmed
    val end = trimmed.offsetByCodePoints(0, INSIGHT_GLOBE_TITLE_MAX_CHARS - 1)
    return trimmed.substring(0, end).trimEnd() + "…"
}

/** All rotation/zoom state. Plain fields are frame-loop scratch; Compose state drives redraw. */
@Stable
private class InsightGlobeMotion(yaw: Float, pitch: Float, val spinSpeed: Float) {
    var yaw by mutableFloatStateOf(yaw)
    var pitch by mutableFloatStateOf(pitch)
    var zoom by mutableFloatStateOf(1f)

    /** True while something needs the frame loop (spin, inertia, reset animation). */
    var needsFrames by mutableStateOf(true)

    var spinning = true
    var velYaw = 0f
    var velPitch = 0f
    var touching = false

    var animStartNs = 0L
    var animating = false
    var fromYaw = 0f
    var fromPitch = 0f
    var fromZoom = 1f
    var toYaw = 0f
    var toPitch = 0f
    var toZoom = 1f

    var accumulatedNs = 0L

    fun onTouchStart() {
        touching = true
        spinning = false
        animating = false
        velYaw = 0f
        velPitch = 0f
    }

    fun onTouchEnd(flingYaw: Float, flingPitch: Float) {
        touching = false
        velYaw = flingYaw
        velPitch = flingPitch
        if (abs(velYaw) > 0.02f || abs(velPitch) > 0.02f) needsFrames = true
    }

    fun rotateBy(dx: Float, dy: Float, radiusPx: Float) {
        val r = radiusPx.coerceAtLeast(1f)
        yaw -= dx / r
        pitch = (pitch + dy / r).coerceIn(-INSIGHT_GLOBE_PITCH_LIMIT, INSIGHT_GLOBE_PITCH_LIMIT)
    }

    fun animateTo(targetYaw: Float, targetPitch: Float, targetZoom: Float) {
        fromYaw = yaw
        fromPitch = pitch
        fromZoom = zoom
        // Shortest way round.
        toYaw = yaw + wrapAngle(targetYaw - yaw)
        toPitch = targetPitch
        toZoom = targetZoom
        velYaw = 0f
        velPitch = 0f
        animStartNs = 0L
        animating = true
        needsFrames = true
    }

    /** Advances one frame; returns false once nothing is moving any more. */
    fun step(nowNs: Long, dtNs: Long, preview: Boolean): Boolean {
        val dt = dtNs.coerceAtMost(50_000_000L) / 1_000_000_000f
        if (animating) {
            if (animStartNs == 0L) animStartNs = nowNs
            val t = ((nowNs - animStartNs).toFloat() / INSIGHT_GLOBE_RESET_NS).coerceIn(0f, 1f)
            val e = easeOutCubicInsight(t)
            yaw = fromYaw + (toYaw - fromYaw) * e
            pitch = fromPitch + (toPitch - fromPitch) * e
            zoom = fromZoom + (toZoom - fromZoom) * e
            if (t >= 1f) {
                animating = false
                yaw = wrapAngle(yaw)
            }
            return animating || spinning
        }
        if (touching) return true
        if (spinning) {
            if (preview) {
                // Throttle the cheap preview spin: state only changes ~30 times a second.
                accumulatedNs += dtNs
                if (accumulatedNs < INSIGHT_GLOBE_PREVIEW_FRAME_NS) return true
                val stepDt = accumulatedNs.coerceAtMost(100_000_000L) / 1_000_000_000f
                accumulatedNs = 0L
                yaw = wrapAngle(yaw + spinSpeed * stepDt)
            } else {
                yaw = wrapAngle(yaw + spinSpeed * dt)
            }
            return true
        }
        if (abs(velYaw) < 0.01f && abs(velPitch) < 0.01f) {
            velYaw = 0f
            velPitch = 0f
            return false
        }
        yaw = wrapAngle(yaw + velYaw * dt)
        pitch = (pitch + velPitch * dt).coerceIn(-INSIGHT_GLOBE_PITCH_LIMIT, INSIGHT_GLOBE_PITCH_LIMIT)
        val damping = exp(-3.6f * dt)
        velYaw *= damping
        velPitch *= damping
        return true
    }
}

/** Per-point data precomputed once per [InsightGlobe] points list, plus per-frame scratch arrays. */
private class InsightGlobeDots(points: List<InsightGlobePoint>) {
    val count = points.size
    val ux = FloatArray(count)
    val uy = FloatArray(count)
    val uz = FloatArray(count)
    /** sqrt(w / max) in 0..1. */
    val strength = FloatArray(count)

    // Per-frame scratch, reused.
    val sx = FloatArray(count)
    val sy = FloatArray(count)
    val depth = FloatArray(count)
    val order = IntArray(count)

    init {
        val maxWeight = max(1, points.maxOfOrNull { it.weight } ?: 1).toFloat()
        points.forEachIndexed { i, p ->
            val latR = Math.toRadians(p.latitude)
            val lonR = Math.toRadians(p.longitude)
            val cosLat = cos(latR)
            ux[i] = (cosLat * cos(lonR)).toFloat()
            uy[i] = sin(latR).toFloat()
            uz[i] = (cosLat * sin(lonR)).toFloat()
            strength[i] = sqrt((max(0, p.weight).toFloat() / maxWeight).coerceIn(0f, 1f))
        }
    }
}

private class InsightGlobeLabelData(
    val ux: Float,
    val uy: Float,
    val uz: Float,
    /** sqrt(w / max) of the dot it belongs to, or -1 when no matching dot was passed. */
    val dotStrength: Float,
    val title: TextLayoutResult,
    val count: TextLayoutResult
)

/**
 * Reusable dark globe with period clusters and up to three place labels.
 *
 * - `interactive = false`: compact preview, no gestures, slow auto-rotation from [initialFocus].
 * - `interactive = true`: drag to rotate (with inertia), pinch to zoom, double-tap to re-centre.
 *   It spins slowly until the first touch.
 *
 * The frame loop only runs while the globe is on screen, the lifecycle is RESUMED and something is
 * actually moving. Coastlines load off the main thread through the cache shared with GlobeViewPage.
 * Clips itself to a 16dp rounded rect and paints its own dark background.
 */
@Composable
fun InsightGlobe(
    points: List<InsightGlobePoint>,
    labels: List<InsightGlobeLabel>,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    initialFocus: Pair<Double, Double>? = null
) {
    val context = LocalContext.current
    val detail = if (interactive) CoastlineDetail.Fine else CoastlineDetail.Coarse
    val coastline by produceState<InsightCoastlineGeometry?>(initialValue = null, detail) {
        value = withContext(Dispatchers.IO) {
            runCatching { InsightGlobeGeometryCache.load(context, detail) }.getOrNull()
        }
    }

    val focus = remember(initialFocus, points) {
        initialFocus?.takeIf { (lat, lon) -> lat in -90.0..90.0 && lon in -180.0..180.0 }
            ?: weightedCentre(points)
            ?: (INSIGHT_GLOBE_DEFAULT_LAT to INSIGHT_GLOBE_DEFAULT_LON)
    }
    val motion = remember(interactive) {
        val (yaw, pitch) = focusAngles(focus.first, focus.second)
        InsightGlobeMotion(
            yaw = yaw,
            pitch = pitch,
            spinSpeed = if (interactive) INSIGHT_GLOBE_INTERACTIVE_SPIN else INSIGHT_GLOBE_PREVIEW_SPIN
        )
    }
    // Re-centre smoothly when the caller's focus changes (e.g. a new period); first value is applied above.
    var appliedFocus by remember(motion) { mutableStateOf(focus) }
    LaunchedEffect(focus, motion) {
        if (focus != appliedFocus) {
            appliedFocus = focus
            val (yaw, pitch) = focusAngles(focus.first, focus.second)
            motion.animateTo(yaw, pitch, if (interactive) motion.zoom else 1f)
        }
    }

    val dots = remember(points) { InsightGlobeDots(points) }
    val textMeasurer = rememberTextMeasurer(cacheSize = 8)
    val titleStyle = remember {
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
    }
    val countStyle = remember {
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
    }
    val labelData = remember(labels, points, textMeasurer) {
        val maxWeight = max(1, points.maxOfOrNull { it.weight } ?: 1).toFloat()
        val numberFormat = NumberFormat.getIntegerInstance()
        labels
            .filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 && it.title.isNotBlank() }
            .sortedByDescending { it.count }
            .take(3)
            .map { label ->
                val latR = Math.toRadians(label.latitude)
                val lonR = Math.toRadians(label.longitude)
                val cosLat = cos(latR)
                val nearest = points.minByOrNull { p ->
                    val dLat = p.latitude - label.latitude
                    var dLon = abs(p.longitude - label.longitude)
                    if (dLon > 180.0) dLon = 360.0 - dLon
                    dLat * dLat + dLon * dLon
                }?.takeIf { p ->
                    var dLon = abs(p.longitude - label.longitude)
                    if (dLon > 180.0) dLon = 360.0 - dLon
                    abs(p.latitude - label.latitude) < 0.6 && dLon < 0.6
                }
                InsightGlobeLabelData(
                    ux = (cosLat * cos(lonR)).toFloat(),
                    uy = sin(latR).toFloat(),
                    uz = (cosLat * sin(lonR)).toFloat(),
                    dotStrength = nearest?.let { sqrt((max(0, it.weight) / maxWeight).coerceIn(0f, 1f)) } ?: -1f,
                    title = textMeasurer.measure(
                        text = truncateTitle(label.title),
                        style = titleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    ),
                    count = textMeasurer.measure(
                        text = numberFormat.format(label.count.coerceAtLeast(0)),
                        style = countStyle,
                        maxLines = 1,
                        softWrap = false
                    )
                )
            }
    }

    val primary = MaterialTheme.colorScheme.primary
    val accent = remember(primary) {
        // The app's primary is black/white; only use it when it is a real, bright colour.
        val maxC = max(primary.red, max(primary.green, primary.blue))
        val minC = min(primary.red, min(primary.green, primary.blue))
        if (maxC - minC > 0.25f && primary.luminance() > 0.3f) primary else Color(0xFF9DFF6B)
    }

    // Lifecycle + on-screen gating for the frame loop.
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { owner, _ ->
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var onScreen by remember { mutableStateOf(true) }
    val running = resumed && onScreen && motion.needsFrames
    LaunchedEffect(running, motion) {
        if (!running) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            val keepGoing = withFrameNanos { now ->
                val dt = if (last == 0L) 0L else now - last
                last = now
                motion.step(now, dt, preview = !interactive)
            }
            if (!keepGoing) {
                motion.needsFrames = false
                break
            }
        }
    }

    val path = remember { Path() }
    val labelRects = remember { FloatArray(4 * 3) }
    val shape = remember { RoundedCornerShape(16.dp) }

    val gestureModifier = if (interactive) {
        Modifier
            .pointerInput(motion) {
                detectTapGestures(
                    onDoubleTap = {
                        val (yaw, pitch) = focusAngles(appliedFocus.first, appliedFocus.second)
                        motion.animateTo(yaw, pitch, 1f)
                    }
                )
            }
            .pointerInput(motion) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    motion.onTouchStart()
                    motion.needsFrames = false
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var dragging = false
                    var multiTouch = false
                    var slopTravel = 0f
                    val slop = viewConfiguration.touchSlop
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount == 0) break
                        val radiusPx = min(size.width, size.height) * 0.42f * motion.zoom
                        if (pressedCount >= 2) {
                            multiTouch = true
                            dragging = true
                            val zoomChange = event.calculateZoom()
                            if (zoomChange.isFinite() && zoomChange > 0f && zoomChange != 1f) {
                                motion.zoom = (motion.zoom * zoomChange)
                                    .coerceIn(INSIGHT_GLOBE_ZOOM_MIN, INSIGHT_GLOBE_ZOOM_MAX)
                            }
                            val pan = event.calculatePan()
                            motion.rotateBy(pan.x, pan.y, radiusPx)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else {
                            val change = event.changes.firstOrNull { it.pressed } ?: break
                            val delta = change.positionChange()
                            if (!dragging) {
                                slopTravel += delta.getDistance()
                                if (slopTravel > slop) dragging = true
                            }
                            if (dragging) {
                                motion.rotateBy(delta.x, delta.y, radiusPx)
                                change.consume()
                            }
                            if (!multiTouch) tracker.addPosition(change.uptimeMillis, change.position)
                        }
                    }
                    var flingYaw = 0f
                    var flingPitch = 0f
                    if (dragging && !multiTouch) {
                        val v = tracker.calculateVelocity()
                        val radiusPx = (min(size.width, size.height) * 0.42f * motion.zoom).coerceAtLeast(1f)
                        flingYaw = (-v.x / radiusPx).coerceIn(-6f, 6f)
                        flingPitch = (v.y / radiusPx).coerceIn(-4f, 4f)
                    }
                    motion.onTouchEnd(flingYaw, flingPitch)
                }
            }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .clip(shape)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val visible = bounds.width > 1f && bounds.height > 1f
                if (visible != onScreen) onScreen = visible
            }
            .then(gestureModifier)
    ) {
        drawInsightGlobe(
            yaw = motion.yaw,
            pitch = motion.pitch,
            zoom = motion.zoom,
            compact = !interactive,
            coastline = coastline,
            path = path,
            dots = dots,
            labels = labelData,
            labelRects = labelRects,
            accent = accent
        )
    }
}

private fun DrawScope.drawInsightGlobe(
    yaw: Float,
    pitch: Float,
    zoom: Float,
    compact: Boolean,
    coastline: InsightCoastlineGeometry?,
    path: Path,
    dots: InsightGlobeDots,
    labels: List<InsightGlobeLabelData>,
    labelRects: FloatArray,
    accent: Color
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val radius = min(w, h) * (if (compact) 0.43f else 0.42f) * zoom
    // Dots follow the globe's on-screen size (not the zoom), so the small preview gets small dots.
    val dotBaseRadius = min(w, h) * 0.42f
    val cx = w / 2f
    val cy = h / 2f
    val center = Offset(cx, cy)

    // Space backdrop.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF070C1A), Color(0xFF02040A)),
            startY = 0f,
            endY = h
        )
    )
    // Atmosphere glow.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF5E9BFF).copy(alpha = 0.26f),
                Color(0xFF5E9BFF).copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.38f
        ),
        radius = radius * 1.38f,
        center = center
    )
    // Ocean, lit from the upper left.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF173058), Color(0xFF0B1A33), Color(0xFF030814)),
            center = Offset(cx - radius * 0.32f, cy - radius * 0.38f),
            radius = radius * 1.45f
        ),
        radius = radius,
        center = center
    )

    val cosYaw = cos(yaw)
    val sinYaw = sin(yaw)
    val cosPitch = cos(pitch)
    val sinPitch = sin(pitch)

    // Coastlines: one reused Path, rebuilt only when this draw runs (i.e. when rotation changed).
    if (coastline != null && coastline.lineCount > 0) {
        path.rewind()
        val xs = coastline.xs
        val ys = coastline.ys
        val zs = coastline.zs
        val starts = coastline.lineStarts
        for (line in 0 until coastline.lineCount) {
            var started = false
            for (i in starts[line] until starts[line + 1]) {
                val x = xs[i]
                val y = ys[i]
                val z = zs[i]
                val x1 = x * cosYaw + z * sinYaw
                val z1 = -x * sinYaw + z * cosYaw
                val y2 = y * cosPitch - z1 * sinPitch
                val z2 = y * sinPitch + z1 * cosPitch
                if (z2 > 0f) {
                    val px = cx - x1 * radius
                    val py = cy - y2 * radius
                    if (started) {
                        path.lineTo(px, py)
                    } else {
                        path.moveTo(px, py)
                        started = true
                    }
                } else {
                    started = false
                }
            }
        }
        drawPath(
            path = path,
            color = Color(0xFFDDE9FF).copy(alpha = if (compact) 0.62f else 0.72f),
            style = Stroke(
                width = (if (compact) 0.85.dp else 1.dp).toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // Limb darkening + rim.
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.72f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.38f),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color(0xFFC8DBFF).copy(alpha = 0.34f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )

    // Dots, back to front.
    var visibleCount = 0
    for (i in 0 until dots.count) {
        val x = dots.ux[i]
        val y = dots.uy[i]
        val z = dots.uz[i]
        val x1 = x * cosYaw + z * sinYaw
        val z1 = -x * sinYaw + z * cosYaw
        val y2 = y * cosPitch - z1 * sinPitch
        val z2 = y * sinPitch + z1 * cosPitch
        if (z2 <= 0.02f) continue
        dots.sx[i] = cx - x1 * radius
        dots.sy[i] = cy - y2 * radius
        dots.depth[i] = z2
        // Insertion sort by depth ascending (<= 40 items, no allocation).
        var j = visibleCount
        while (j > 0 && dots.depth[dots.order[j - 1]] > z2) {
            dots.order[j] = dots.order[j - 1]
            j--
        }
        dots.order[j] = i
        visibleCount++
    }
    val ringWidth = (if (compact) 0.8f else 1f).dp.toPx()
    for (k in 0 until visibleCount) {
        val i = dots.order[k]
        val depth = dots.depth[i]
        val fade = ((depth - 0.02f) / 0.25f).coerceIn(0f, 1f)
        val r = dotRadiusPx(dots.strength[i], dotBaseRadius) * (0.72f + 0.28f * depth)
        val c = Offset(dots.sx[i], dots.sy[i])
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.32f * fade), Color.Transparent),
                center = c,
                radius = r * 1.8f
            ),
            radius = r * 1.8f,
            center = c
        )
        drawCircle(color = accent.copy(alpha = 0.92f * fade), radius = r, center = c)
        drawCircle(
            color = Color.White.copy(alpha = 0.9f * fade),
            radius = r,
            center = c,
            style = Stroke(width = ringWidth)
        )
    }

    drawLabels(labels, labelRects, cx, cy, radius, dotBaseRadius, cosYaw, sinYaw, cosPitch, sinPitch)
}

// Area still grows with the photo count (radius ~ sqrt), within 2.5–7.5 dp.
private fun DrawScope.dotRadiusPx(strength: Float, baseRadius: Float): Float =
    (baseRadius * (0.016f + 0.028f * strength)).coerceIn(2.5.dp.toPx(), 7.5.dp.toPx())

private fun DrawScope.drawLabels(
    labels: List<InsightGlobeLabelData>,
    placed: FloatArray,
    cx: Float,
    cy: Float,
    radius: Float,
    dotBaseRadius: Float,
    cosYaw: Float,
    sinYaw: Float,
    cosPitch: Float,
    sinPitch: Float
) {
    if (labels.isEmpty()) return
    val padH = 7.dp.toPx()
    val padV = 4.dp.toPx()
    val gap = 4.dp.toPx()
    val offset = 6.dp.toPx()
    val inset = 4.dp.toPx()
    val corner = CornerRadius(8.dp.toPx())
    val shadowDy = 1.dp.toPx()
    // Placed card rects as (left, top, right, bottom) quadruples; reused scratch, no allocation.
    var placedCount = 0

    for (label in labels) {
        val x1 = label.ux * cosYaw + label.uz * sinYaw
        val z1 = -label.ux * sinYaw + label.uz * cosYaw
        val y2 = label.uy * cosPitch - z1 * sinPitch
        val z2 = label.uy * sinPitch + z1 * cosPitch
        if (z2 <= INSIGHT_GLOBE_LABEL_MIN_DEPTH) continue
        val px = cx - x1 * radius
        val py = cy - y2 * radius
        if (px < 0f || px > size.width || py < 0f || py > size.height) continue
        val alpha = ((z2 - INSIGHT_GLOBE_LABEL_MIN_DEPTH) / 0.2f).coerceIn(0f, 1f)
        val dotR = if (label.dotStrength >= 0f) {
            dotRadiusPx(label.dotStrength, dotBaseRadius) * (0.72f + 0.28f * z2)
        } else {
            2.5.dp.toPx()
        }
        val titleSize = label.title.size
        val countSize = label.count.size
        val cardW = padH * 2 + titleSize.width + gap + countSize.width
        val cardH = padV * 2 + max(titleSize.height, countSize.height)

        var chosenL = Float.NaN
        var chosenT = 0f
        for (candidate in 0 until 4) {
            val l: Float
            val t: Float
            when (candidate) {
                0 -> { l = px + dotR + offset; t = py - cardH / 2f } // right
                1 -> { l = px - dotR - offset - cardW; t = py - cardH / 2f } // left
                2 -> { l = px - cardW / 2f; t = py - dotR - offset - cardH } // above
                else -> { l = px - cardW / 2f; t = py + dotR + offset } // below
            }
            val r = l + cardW
            val b = t + cardH
            if (l < inset || t < inset || r > size.width - inset || b > size.height - inset) continue
            var overlaps = false
            for (q in 0 until placedCount) {
                val o = q * 4
                if (l < placed[o + 2] && r > placed[o] && t < placed[o + 3] && b > placed[o + 1]) {
                    overlaps = true
                    break
                }
            }
            if (overlaps) continue
            chosenL = l
            chosenT = t
            break
        }
        if (chosenL.isNaN()) continue

        val topLeft = Offset(chosenL, chosenT)
        val cardSize = Size(cardW, cardH)
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f * alpha),
            topLeft = Offset(chosenL, chosenT + shadowDy),
            size = cardSize,
            cornerRadius = corner
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.96f * alpha),
            topLeft = topLeft,
            size = cardSize,
            cornerRadius = corner
        )
        val textTop = chosenT + padV
        drawText(
            textLayoutResult = label.title,
            topLeft = Offset(chosenL + padH, textTop + (cardH - padV * 2 - titleSize.height) / 2f),
            alpha = alpha
        )
        drawText(
            textLayoutResult = label.count,
            topLeft = Offset(
                chosenL + padH + titleSize.width + gap,
                textTop + (cardH - padV * 2 - countSize.height) / 2f
            ),
            alpha = alpha
        )

        if (placedCount * 4 + 3 < placed.size) {
            val o = placedCount * 4
            placed[o] = chosenL
            placed[o + 1] = chosenT
            placed[o + 2] = chosenL + cardW
            placed[o + 3] = chosenT + cardH
            placedCount++
        }
    }
}
