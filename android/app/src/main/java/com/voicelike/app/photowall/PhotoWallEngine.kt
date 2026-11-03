package com.voicelike.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Photo wall engine (iOS PhotoWallCanvasView). The organize queue is laid out as a wall with no
// edges: newest first into the shortest column, repeating horizontally by one cycle width and
// vertically per column by that column's own height, so dragging never reaches an end and photos
// repeat. Odd horizontal cycles are shifted down by half a column cycle so two copies of a photo
// never sit side by side. Press and hold lifts a photo, flinging it away marks it for deletion;
// the rest of the column closes the gap and the photo's other copies fade out. Undo flies it back
// along the way it left, onto the copy nearest the middle of the screen.
//
// A tile on the wall is (photo, horizontal cycle kx, vertical cycle ky of its column); one cycle of
// one column is a strip. Each column has a vertical phase (the top of cycle 0): when a photo leaves
// or comes back the column's cycle gets shorter or longer and the phase moves with it, so the top
// half of the cycle under the finger stays put and only the photos below close up or make room.
//
// Frame budget: nothing is composed per tile. Every frame (or pan event) the visible range of each
// strip is found by binary search and the page draws exactly those tiles in one Canvas; panning and
// zooming only change origin and scale. Image resolution follows the scale once the wall settles.
// Coordinates: canvas units are screen pixels at scale 1; origin is the canvas point at the view's
// top-left corner.
internal class PhotoWallEngine(
    private val density: Float,
) {
    interface Feedback {
        fun lift()

        fun armTick()

        fun commit()

        fun snapBack()

        fun lightTap()

        fun deleteSound()

        fun returnSound()
    }

    // Asked at the moment of the throw: only when the photo really went into pending delete does it fly away.
    var onTrash: ((MediaItem) -> Boolean)? = null
    var onRestore: ((MediaItem) -> Unit)? = null

    // Remaining items, the opened index, and the on-screen rect (view coordinates) of every visible
    // item's nearest copy so the viewer can fly back to whichever page is showing when it closes.
    var onOpen: ((items: List<MediaItem>, index: Int, rects: Map<Long, Rect>) -> Unit)? = null
    var feedback: Feedback? = null
    var images: PhotoWallImageStore? = null

    // Height of the top bar laid over the wall: the first row starts below it.
    var topInset = 0f
        private set

    // Until the wall is first dragged, pinched or touched, a late top bar measurement re-places the first screen.
    private var hasUserMoved = false

    // Bumped whenever the wall needs to be drawn again.
    var drawTick by mutableIntStateOf(0)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var remainingCount by mutableIntStateOf(0)
        private set

    // Asks the frame loop for frames until the wall is idle again.
    val wake = Channel<Unit>(Channel.CONFLATED)

    var items: List<MediaItem> = emptyList()
        private set
    private var layout = PhotoWallLayout(emptyList(), 1f, density)
    private val removed = HashSet<Int>()
    private var phase = FloatArray(0)
    private var viewWidth = 0f
    private var viewHeight = 0f
    private var layoutWidth = 0f
    private var layoutHeight = 0f
    var originX = 0f
        private set
    var originY = 0f
        private set
    var scale = 1f
        private set
    private var hasPlacedInitialView = false
    private var needsLayoutBuild = false

    // Bumped per batch of photos: callbacks of animations started before no longer touch the new batch.
    private var generation = 0

    // Motion
    private var lastTickNanos = 0L
    private var framesRequested = false
    private var velocityX = 0f
    private var velocityY = 0f
    private var originAnimation: OriginAnimation? = null
    private var scaleAnimation: ScaleAnimation? = null
    private val columnTransitions = HashMap<Int, ColumnTransition>()
    private val columnProgress = HashMap<Int, Float>()
    private var isPinching = false
    private var isPanning = false
    private var lastPinchFocus = Offset.Zero

    // A press that stops a fling only stops it; it never opens a photo.
    private var swallowTap = false

    // Lift and throw
    private var lifted: Floating? = null
    val floating = ArrayList<Floating>()
    val ghosts = ArrayList<Ghost>()
    private val history = ArrayList<Removal>()

    // Tiles in hand, snapping back or flying back: drawn by their floating copy, not by the wall.
    private val detached = ArrayList<Instance>()

    // The photo being brought back: its other copies on the wall fade in instead of popping up.
    var arrivingItem = -1
        private set
    var arrivingStartNanos = 0L
        private set

    // The copy the full-screen viewer opened from or is closing onto.
    var hiddenInstance: Instance? = null

    // Dims the wall while a photo is lifted (applied per tile, never as a group layer).
    var contentAlpha = 1f
        private set
    private var contentAlphaTarget = 1f

    // Tiles drawn this frame (canvas frames), in parallel arrays to avoid per-frame allocation.
    var visibleCount = 0
        private set
    var visibleItem = IntArray(256)
        private set
    var visibleKx = IntArray(256)
        private set
    var visibleKy = IntArray(256)
        private set
    var visibleX = FloatArray(256)
        private set
    var visibleY = FloatArray(256)
        private set
    var visibleW = FloatArray(256)
        private set
    var visibleH = FloatArray(256)
        private set

    data class Instance(
        val item: Int,
        val kx: Int,
        val ky: Int,
    )

    private class OriginAnimation(
        val fromX: Float,
        val fromY: Float,
        val toX: Float,
        val toY: Float,
        val start: Long,
        val durationNanos: Long,
        var completion: (() -> Unit)?,
    )

    private class ScaleAnimation(
        val from: Float,
        val to: Float,
        val focus: Offset,
        val start: Long,
        val durationNanos: Long,
    )

    // A column closing up or making room: every tile slides from its old top to the new one while
    // the column's cycle height and phase move along.
    private class ColumnTransition(
        val fromY: HashMap<Int, Float>,
        val fromPhase: Float,
        val fromPeriod: Float,
        val start: Long,
    )

    private class Removal(
        val item: Int,
        val position: Int,
        // Where and at what angle the photo left the screen: undo flies it back from there.
        val exitX: Float,
        val exitY: Float,
        val exitRotation: Float,
    )

    // A photo drawn above the wall in screen coordinates: in hand, flying away, snapping back or
    // flying back. Size is in canvas units; on screen it is size x wall scale x [mul].
    class Floating(
        val item: Int,
        val width: Float,
        val height: Float,
        var key: Instance?,
    ) {
        var cx = 0f
        var cy = 0f
        var rotation = 0f
        var mul = 1f
        var tint = 0f
        var tintTarget = 0f
        var lifted = true
        internal var mode = MODE_FOLLOW
        internal var vx = 0f
        internal var vy = 0f
        internal var vr = 0f
        internal var vm = 0f
        internal var targetX = 0f
        internal var targetY = 0f
        internal var targetRotation = 0f
        internal var targetMul = 1f
        internal var stiffness = 300f
        internal var damping = 0.72f
        internal var fromX = 0f
        internal var fromY = 0f
        internal var fromRotation = 0f
        internal var linearStart = 0L
        internal var linearDuration = 1L
        internal var homeX = 0f
        internal var homeY = 0f
        internal var touchStartX = 0f
        internal var touchStartY = 0f
        internal var armed = false
        internal var onDone: (() -> Unit)? = null
    }

    // A copy of a thrown-away photo shrinking and fading where it stood.
    class Ghost(
        val item: Int,
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val start: Long,
    )

    // MARK: Data

    // A new batch (first open, or the library / pending delete changed elsewhere): position and scale
    // are kept. The same photos the wall already shows (say the thrown ones were deleted for real
    // from the pending page) do not rebuild anything.
    fun setItems(newItems: List<MediaItem>) {
        if (hasPlacedInitialView && sameIds(newItems, remainingItems())) return
        cancelLift(animated = false)
        floating.clear()
        ghosts.clear()
        columnTransitions.clear()
        columnProgress.clear()
        history.clear()
        removed.clear()
        detached.clear()
        arrivingItem = -1
        hiddenInstance = null
        contentAlpha = 1f
        contentAlphaTarget = 1f
        generation += 1
        items = newItems
        publishState()
        needsLayoutBuild = true
        rebuildLayout()
    }

    private fun sameIds(
        a: List<MediaItem>,
        b: List<MediaItem>,
    ): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) if (a[i].id != b[i].id) return false
        return true
    }

    fun remainingItems(): List<MediaItem> = items.indices.filter { it !in removed }.map { items[it] }

    private fun publishState() {
        canUndo = history.isNotEmpty()
        remainingCount = items.size - removed.size
    }

    fun onSizeChanged(
        width: Float,
        height: Float,
    ) {
        if (width <= 0f || height <= 0f) return
        viewWidth = width
        viewHeight = height
        if (needsLayoutBuild || layoutWidth == 0f) {
            rebuildLayout()
        } else if (width != layoutWidth || height != layoutHeight) {
            // New window size (rotation, a foldable opening): the wall is not rebuilt and the part
            // of it at the middle of the screen stays there.
            val center = canvasPoint(layoutWidth / 2f, layoutHeight / 2f)
            layoutWidth = width
            layoutHeight = height
            originX = center.x - width / 2f / scale
            originY = center.y - height / 2f / scale
            applyTransform()
        }
    }

    private fun rebuildLayout() {
        if (viewWidth <= 0f || viewHeight <= 0f) return
        needsLayoutBuild = false
        layoutWidth = viewWidth
        layoutHeight = viewHeight
        layout = PhotoWallLayout(items.map(::displaySize), viewWidth / viewHeight, density)
        phase = FloatArray(layout.columnCount)
        if (!hasPlacedInitialView && items.isNotEmpty()) {
            hasPlacedInitialView = true
            placeInitialView()
        }
        scale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        applyTransform()
    }

    private fun displaySize(item: MediaItem): Pair<Int, Int> {
        val aspect = resolveMediaItemFallbackAspectRatio(item) ?: return 0 to 0
        return 1000 to (1000f / aspect).roundToInt()
    }

    // First screen: scale 1, the newest photos in the top-left corner and the first row right below
    // the top bar; older ones to the right and further down. The left edge cuts exactly at the right
    // edge of the previous cycle's last column, so no sliver of it shows.
    fun updateTopInset(value: Float) {
        if (value == topInset) return
        topInset = value
        if (hasPlacedInitialView && !hasUserMoved) {
            placeInitialView()
            applyTransform()
        }
    }

    private fun placeInitialView() {
        scale = 1f
        originX = -layout.gap
        originY = -(topInset + 6f * density)
    }

    // MARK: Geometry

    private fun canvasPoint(
        x: Float,
        y: Float,
    ) = Offset(originX + x / scale, originY + y / scale)

    private fun screenX(canvasX: Float) = (canvasX - originX) * scale

    private fun screenY(canvasY: Float) = (canvasY - originY) * scale

    private fun columnOfStrip(strip: Int): Int = strip - photoWallFloorDiv(strip, layout.columnCount) * layout.columnCount

    private fun stripX(strip: Int): Float =
        photoWallFloorDiv(strip, layout.columnCount) * layout.periodWidth + layout.columnX[columnOfStrip(strip)]

    // Odd horizontal cycles shift down by half a column cycle: repeating only by whole cycle widths
    // puts a photo's next copy at the same height one cycle over, which is obvious when there are
    // few photos. Shifted, the nearest copy sits diagonally, about 1.5 times as far.
    private fun cycleShift(kx: Int): Float = if (kx and 1 == 0) 0f else 0.5f

    // Where a tile ends up once its column has settled.
    private fun canvasFrame(key: Instance): Rect {
        val column = layout.itemColumn[key.item]
        val x = key.kx * layout.periodWidth + layout.columnX[column]
        val y = phase[column] + (key.ky + cycleShift(key.kx)) * layout.columnPeriod[column] + layout.itemY[key.item]
        return Rect(x, y, x + layout.columnWidth[column], y + layout.itemHeight[key.item])
    }

    private fun animatedY(
        item: Int,
        column: Int,
    ): Float {
        val target = layout.itemY[item]
        val transition = columnTransitions[column] ?: return target
        val p = columnProgress[column] ?: return target
        val from = transition.fromY[item] ?: target
        return from + (target - from) * p
    }

    private fun animatedPhase(column: Int): Float {
        val target = phase[column]
        val transition = columnTransitions[column] ?: return target
        val p = columnProgress[column] ?: return target
        return transition.fromPhase + (target - transition.fromPhase) * p
    }

    private fun animatedPeriod(column: Int): Float {
        val target = layout.columnPeriod[column]
        val transition = columnTransitions[column] ?: return target
        val p = columnProgress[column] ?: return target
        return transition.fromPeriod + (target - transition.fromPeriod) * p
    }

    private fun updateTransitionProgress(now: Long) {
        columnProgress.clear()
        val iterator = columnTransitions.entries.iterator()
        while (iterator.hasNext()) {
            val (column, transition) = iterator.next()
            val t = (now - transition.start).toFloat() / COLUMN_TRANSITION_NANOS
            if (t >= 1f) {
                iterator.remove()
            } else {
                columnProgress[column] = criticalSpring(max(t, 0f))
            }
        }
    }

    // Corner radius on screen follows the scale, between 4 and 24 dp.
    val screenCornerRadius: Float get() = (14f * scale).coerceIn(4f, 24f) * density

    // MARK: Visible tiles

    // Moves the wall and recomputes which tiles are on screen; the only per-frame work while dragging.
    private fun applyTransform() {
        refreshVisible()
        drawTick += 1
    }

    private fun refreshVisible() {
        visibleCount = 0
        if (layout.columnCount == 0 || layout.periodWidth <= 0f || viewWidth <= 0f) return
        val store = images
        store?.beginPass()
        // A margin past the screen edges: tiles and their images are ready before they scroll in.
        val marginX = viewWidth * 0.2f / scale
        val marginY = viewHeight * 0.2f / scale
        val minX = originX - marginX
        val maxX = originX + viewWidth / scale + marginX
        val minY = originY - marginY
        val maxY = originY + viewHeight / scale + marginY
        val idle = isIdle()
        val count = layout.columnCount
        val period = layout.periodWidth
        val firstCycle = floor(minX / period).toInt()
        val first = firstCycle * count + firstColumnEndingAfter(minX - firstCycle * period)
        val lastCycle = floor(maxX / period).toInt()
        val last = lastCycle * count + lastColumnStartingBefore(maxX - lastCycle * period)
        var strip = first
        while (strip <= last) {
            val column = columnOfStrip(strip)
            val kx = photoWallFloorDiv(strip, count)
            val list = layout.columnItems(column)
            val n = list.size
            if (n > 0) {
                val x = stripX(strip)
                val width = layout.columnWidth[column]
                val periodY = animatedPeriod(column)
                val base = animatedPhase(column) + cycleShift(kx) * periodY
                val start = firstRowEndingAfter(column, list, base, periodY, minY, maxY)
                var u = start
                while (true) {
                    val ky = photoWallFloorDiv(u, n)
                    val item = list[u - ky * n]
                    val top = base + ky * periodY + animatedY(item, column)
                    if (top > maxY) break
                    if (!isDetached(item, kx, ky)) {
                        appendVisible(item, kx, ky, x, top, width, layout.itemHeight[item])
                        store?.want(items[item], bucketFor(width * scale), allowUpgrade = idle)
                    }
                    u += 1
                }
            }
            strip += 1
        }
        store?.endPass()
    }

    // First column of a cycle whose right edge passes x (none = column count, i.e. the next cycle's first).
    private fun firstColumnEndingAfter(x: Float): Int {
        var lo = 0
        var hi = layout.columnCount
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (layout.columnX[mid] + layout.columnWidth[mid] < x) lo = mid + 1 else hi = mid
        }
        return lo
    }

    // Last column of a cycle whose left edge is not past x (x is never negative, so column 0 counts).
    private fun lastColumnStartingBefore(x: Float): Int {
        var lo = 0
        var hi = layout.columnCount
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (layout.columnX[mid] <= x) lo = mid + 1 else hi = mid
        }
        return lo - 1
    }

    // Rows of a strip are numbered across vertical cycles (row p of cycle ky = ky x count + p), so a
    // larger number is always lower down; binary search for the first row whose bottom passes minY.
    private fun firstRowEndingAfter(
        column: Int,
        list: List<Int>,
        base: Float,
        period: Float,
        minY: Float,
        maxY: Float,
    ): Int {
        val n = list.size

        fun top(u: Int): Float {
            val ky = photoWallFloorDiv(u, n)
            return base + ky * period + animatedY(list[u - ky * n], column)
        }

        fun height(u: Int): Float = layout.itemHeight[list[u - photoWallFloorDiv(u, n) * n]]
        var lo = (floor((minY - base) / period).toInt() - 1) * n
        var hi = (floor((maxY - base) / period).toInt() + 2) * n
        while (lo < hi) {
            val mid = photoWallFloorDiv(lo + hi, 2)
            if (top(mid) + height(mid) < minY) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun appendVisible(
        item: Int,
        kx: Int,
        ky: Int,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
    ) {
        if (visibleCount == visibleItem.size) {
            val size = visibleItem.size * 2
            visibleItem = visibleItem.copyOf(size)
            visibleKx = visibleKx.copyOf(size)
            visibleKy = visibleKy.copyOf(size)
            visibleX = visibleX.copyOf(size)
            visibleY = visibleY.copyOf(size)
            visibleW = visibleW.copyOf(size)
            visibleH = visibleH.copyOf(size)
        }
        val i = visibleCount
        visibleItem[i] = item
        visibleKx[i] = kx
        visibleKy[i] = ky
        visibleX[i] = x
        visibleY[i] = y
        visibleW[i] = w
        visibleH[i] = h
        visibleCount = i + 1
    }

    private fun isDetached(
        item: Int,
        kx: Int,
        ky: Int,
    ): Boolean {
        for (key in detached) if (key.item == item && key.kx == kx && key.ky == ky) return true
        return false
    }

    fun isHidden(
        item: Int,
        kx: Int,
        ky: Int,
    ): Boolean {
        val hidden = hiddenInstance ?: return false
        return hidden.item == item && hidden.kx == kx && hidden.ky == ky
    }

    // Image size in steps: zooming does not swap images every frame; once it settles the visible
    // tiles get sharp ones.
    private fun bucketFor(screenWidthPx: Float): Int {
        for (bucket in IMAGE_BUCKETS) if (screenWidthPx <= bucket * 1.1f) return bucket
        return IMAGE_BUCKETS.last()
    }

    private fun isIdle(): Boolean = velocityX == 0f && velocityY == 0f && originAnimation == null && scaleAnimation == null && !isPinching

    private fun instanceAt(
        x: Float,
        y: Float,
    ): Instance? {
        // Pressing in the narrow gap between two photos still picks the nearest one.
        val point = canvasPoint(x, y)
        val tolerance = 6f * density / scale
        var best: Instance? = null
        var bestDistance = Float.MAX_VALUE
        for (i in 0 until visibleCount) {
            val left = visibleX[i]
            val top = visibleY[i]
            val dx = max(max(left - point.x, 0f), point.x - (left + visibleW[i]))
            val dy = max(max(top - point.y, 0f), point.y - (top + visibleH[i]))
            val distance = hypot(dx, dy)
            if (distance == 0f) return Instance(visibleItem[i], visibleKx[i], visibleKy[i])
            if (distance <= tolerance && distance < bestDistance) {
                best = Instance(visibleItem[i], visibleKx[i], visibleKy[i])
                bestDistance = distance
            }
        }
        return best
    }

    // MARK: Frames

    private fun requestFrames() {
        framesRequested = true
        wake.trySend(Unit)
    }

    fun needsFrames(): Boolean =
        framesRequested ||
            velocityX != 0f ||
            velocityY != 0f ||
            originAnimation != null ||
            scaleAnimation != null ||
            columnTransitions.isNotEmpty() ||
            ghosts.isNotEmpty() ||
            floating.any(::isMoving) ||
            arrivingItem >= 0 ||
            contentAlpha != contentAlphaTarget

    // A photo held still under the finger needs no frames once its lift has settled.
    private fun isMoving(tile: Floating): Boolean =
        tile.mode != MODE_FOLLOW ||
            tile.tint != tile.tintTarget ||
            abs(tile.mul - LIFT_MUL) > 0.001f ||
            abs(tile.vm) > 0.01f

    fun tick(now: Long) {
        framesRequested = false
        if (lastTickNanos == 0L || now - lastTickNanos > 200_000_000L) lastTickNanos = now - 16_666_667L
        val dt = ((now - lastTickNanos).coerceIn(0L, 33_333_333L)) / 1e9f
        lastTickNanos = now

        val origin = originAnimation
        val scaling = scaleAnimation
        if (origin != null) {
            val t = min(1f, (now - origin.start).toFloat() / origin.durationNanos)
            val e = easeOutCubic(t)
            originX = origin.fromX + (origin.toX - origin.fromX) * e
            originY = origin.fromY + (origin.toY - origin.fromY) * e
            if (t >= 1f) {
                originAnimation = null
                val completion = origin.completion
                origin.completion = null
                completion?.invoke()
            }
        } else if (scaling != null) {
            val t = min(1f, (now - scaling.start).toFloat() / scaling.durationNanos)
            setScale(scaling.from + (scaling.to - scaling.from) * easeOutCubic(t), scaling.focus)
            if (t >= 1f) scaleAnimation = null
        } else if (velocityX != 0f || velocityY != 0f) {
            // UIScrollView's normal deceleration rate per millisecond; the wall has no edge, so it
            // glides until it stops.
            val decay = DECELERATION_PER_MS.pow(dt * 1000f)
            originX -= velocityX * dt / scale
            originY -= velocityY * dt / scale
            velocityX *= decay
            velocityY *= decay
            if (hypot(velocityX, velocityY) < 8f * density) {
                velocityX = 0f
                velocityY = 0f
            }
        }

        updateTransitionProgress(now)
        stepFloating(now, dt)
        ghosts.removeAll { now - it.start >= GHOST_NANOS }
        if (arrivingItem >= 0 && now - arrivingStartNanos > ARRIVING_TOTAL_NANOS && floating.none { it.item == arrivingItem }) {
            arrivingItem = -1
        }
        if (contentAlpha != contentAlphaTarget) {
            val step = dt / 0.25f
            contentAlpha =
                if (contentAlpha < contentAlphaTarget) {
                    min(contentAlphaTarget, contentAlpha + step)
                } else {
                    max(contentAlphaTarget, contentAlpha - step)
                }
        }
        // Once the wall is idle again this pass also swaps in sharper images for the new scale.
        applyTransform()
    }

    private fun stepFloating(
        now: Long,
        dt: Float,
    ) {
        if (floating.isEmpty()) return
        val finished = ArrayList<Floating>(0)
        for (tile in floating) {
            if (tile.tint != tile.tintTarget) {
                val step = dt / 0.18f
                tile.tint =
                    if (tile.tint < tile.tintTarget) min(tile.tintTarget, tile.tint + step) else max(tile.tintTarget, tile.tint - step)
            }
            when (tile.mode) {
                MODE_FOLLOW -> {
                    // Lift: the scale springs up while the position follows the finger directly.
                    val (m, vm) = springStep(tile.mul, tile.vm, LIFT_MUL, tile.stiffness, tile.damping, dt)
                    tile.mul = m
                    tile.vm = vm
                }
                MODE_LINEAR -> {
                    val t = min(1f, (now - tile.linearStart).toFloat() / tile.linearDuration)
                    tile.cx = tile.fromX + (tile.targetX - tile.fromX) * t
                    tile.cy = tile.fromY + (tile.targetY - tile.fromY) * t
                    tile.rotation = tile.fromRotation + (tile.targetRotation - tile.fromRotation) * t
                    if (t >= 1f) finished.add(tile)
                }
                MODE_SPRING -> {
                    // The way home follows the wall, which may still be moving.
                    tile.key?.let { key ->
                        val frame = canvasFrame(key)
                        tile.targetX = screenX(frame.center.x)
                        tile.targetY = screenY(frame.center.y)
                    }
                    val (x, vx) = springStep(tile.cx, tile.vx, tile.targetX, tile.stiffness, tile.damping, dt)
                    val (y, vy) = springStep(tile.cy, tile.vy, tile.targetY, tile.stiffness, tile.damping, dt)
                    val (r, vr) = springStep(tile.rotation, tile.vr, tile.targetRotation, tile.stiffness, tile.damping, dt)
                    val (m, vm) = springStep(tile.mul, tile.vm, tile.targetMul, tile.stiffness, tile.damping, dt)
                    tile.cx = x
                    tile.vx = vx
                    tile.cy = y
                    tile.vy = vy
                    tile.rotation = r
                    tile.vr = vr
                    tile.mul = m
                    tile.vm = vm
                    val settledPosition =
                        abs(x - tile.targetX) < 0.5f &&
                            abs(y - tile.targetY) < 0.5f &&
                            abs(vx) < 20f &&
                            abs(vy) < 20f
                    val settledShape = abs(r - tile.targetRotation) < 0.002f && abs(m - tile.targetMul) < 0.002f
                    if (settledPosition && settledShape && tile.tint == tile.tintTarget) finished.add(tile)
                }
            }
        }
        for (tile in finished) {
            floating.remove(tile)
            tile.onDone?.invoke()
            tile.onDone = null
        }
    }

    // MARK: Gestures

    // Finger down: a fling stops; if it was still fast this press does not count as a tap.
    fun touchDown() {
        hasUserMoved = true
        if (hypot(velocityX, velocityY) > 60f * density) swallowTap = true
        velocityX = 0f
        velocityY = 0f
        if (originAnimation?.completion == null) originAnimation = null
    }

    fun touchUp() {
        swallowTap = false
    }

    fun beginPan() {
        if (lifted != null) return
        isPanning = true
        velocityX = 0f
        velocityY = 0f
        originAnimation = null
    }

    fun panBy(
        dx: Float,
        dy: Float,
    ) {
        if (lifted != null || isPinching) return
        originX -= dx / scale
        originY -= dy / scale
        applyTransform()
    }

    fun endPan(
        vx: Float,
        vy: Float,
    ) {
        isPanning = false
        if (isPinching || lifted != null) return
        velocityX = if (vx.isFinite()) vx else 0f
        velocityY = if (vy.isFinite()) vy else 0f
        requestFrames()
    }

    fun beginPinch(focus: Offset) {
        if (lifted != null) return
        isPinching = true
        isPanning = false
        velocityX = 0f
        velocityY = 0f
        originAnimation = null
        scaleAnimation = null
        lastPinchFocus = focus
    }

    // The piece of wall under the fingers follows them (moving both fingers pans too). Past the
    // limits the pinch gets heavier; pinching back follows normally.
    fun pinch(
        focus: Offset,
        factor: Float,
    ) {
        if (!isPinching || lifted != null || factor <= 0f || !factor.isFinite()) return
        val anchor = canvasPoint(lastPinchFocus.x, lastPinchFocus.y)
        val raw = scale * factor
        val pushingOut = (raw > MAX_SCALE && factor > 1f) || (raw < MIN_SCALE && factor < 1f)
        scale = if (pushingOut) scale * factor.pow(0.3f) else raw
        originX = anchor.x - focus.x / scale
        originY = anchor.y - focus.y / scale
        lastPinchFocus = focus
        applyTransform()
    }

    fun endPinch() {
        if (!isPinching) return
        isPinching = false
        val target = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        if (abs(target - scale) > 0.001f) {
            scaleAnimation = ScaleAnimation(scale, target, lastPinchFocus, System.nanoTime(), 320_000_000L)
        }
        requestFrames()
    }

    private fun setScale(
        newScale: Float,
        focus: Offset,
    ) {
        val anchor = canvasPoint(focus.x, focus.y)
        scale = newScale
        originX = anchor.x - focus.x / scale
        originY = anchor.y - focus.y / scale
    }

    fun tap(
        x: Float,
        y: Float,
    ) {
        if (swallowTap || lifted != null) return
        val key = instanceAt(x, y) ?: return
        open(key)
    }

    private fun open(key: Instance) {
        val remaining = items.indices.filter { it !in removed }
        val position = remaining.indexOf(key.item)
        if (position < 0) return
        // A photo can be on the wall several times: the viewer opens from the copy that was tapped
        // and closes onto the visible copy nearest the middle for every other page.
        val rects = HashMap<Long, Rect>()
        val distances = HashMap<Long, Float>()
        val midX = viewWidth / 2f
        val midY = viewHeight / 2f
        for (i in 0 until visibleCount) {
            val left = screenX(visibleX[i])
            val top = screenY(visibleY[i])
            val rect = Rect(left, top, left + visibleW[i] * scale, top + visibleH[i] * scale)
            if (rect.right < 0f || rect.left > viewWidth || rect.bottom < 0f || rect.top > viewHeight) continue
            val id = items[visibleItem[i]].id
            val distance = hypot(rect.center.x - midX, rect.center.y - midY)
            if (distance < (distances[id] ?: Float.MAX_VALUE)) {
                distances[id] = distance
                rects[id] = rect
            }
        }
        val frame = canvasFrame(key)
        val left = screenX(frame.left)
        val top = screenY(frame.top)
        rects[items[key.item].id] = Rect(left, top, left + frame.width * scale, top + frame.height * scale)
        hiddenInstance = key
        drawTick += 1
        onOpen?.invoke(remaining.map { items[it] }, position, rects)
    }

    // The viewer is closing onto [itemId]: hide the copy it lands on until it is closed.
    fun hideNearestCopy(itemId: Long) {
        val midX = viewWidth / 2f
        val midY = viewHeight / 2f
        var best: Instance? = null
        var bestDistance = Float.MAX_VALUE
        for (i in 0 until visibleCount) {
            if (items[visibleItem[i]].id != itemId) continue
            val cx = screenX(visibleX[i] + visibleW[i] / 2f)
            val cy = screenY(visibleY[i] + visibleH[i] / 2f)
            val distance = hypot(cx - midX, cy - midY)
            if (distance < bestDistance) {
                bestDistance = distance
                best = Instance(visibleItem[i], visibleKx[i], visibleKy[i])
            }
        }
        val current = hiddenInstance
        if (current != null && items.getOrNull(current.item)?.id == itemId) return
        hiddenInstance = best
        drawTick += 1
    }

    fun clearHidden() {
        hiddenInstance = null
        drawTick += 1
    }

    // MARK: Lift & throw

    // Not while a fling is still going, while dragging or pinching, or off the photos.
    fun canLift(
        x: Float,
        y: Float,
    ): Boolean = lifted == null && !isPinching && !isPanning && velocityX == 0f && velocityY == 0f && instanceAt(x, y) != null

    fun beginLift(
        x: Float,
        y: Float,
    ) {
        if (lifted != null) return
        val key = instanceAt(x, y) ?: return
        velocityX = 0f
        velocityY = 0f
        originAnimation = null
        val frame = canvasFrame(key)
        detached.add(key)
        val tile =
            Floating(key.item, frame.width, frame.height, key).apply {
                cx = screenX(frame.center.x)
                cy = screenY(frame.center.y)
                homeX = cx
                homeY = cy
                touchStartX = x
                touchStartY = y
                mode = MODE_FOLLOW
                stiffness = 300f
                damping = 0.72f
            }
        floating.add(tile)
        lifted = tile
        contentAlphaTarget = 0.72f
        feedback?.lift()
        applyTransform()
        requestFrames()
    }

    fun dragLift(
        x: Float,
        y: Float,
    ) {
        val tile = lifted ?: return
        val dx = x - tile.touchStartX
        val dy = y - tile.touchStartY
        tile.cx = tile.homeX + dx
        tile.cy = tile.homeY + dy
        // Dragging sideways tilts it a little, like holding a print by its corner.
        tile.rotation = (dx / (520f * density)).coerceIn(-0.32f, 0.32f)
        val armed = hypot(dx, dy) > THROW_DISTANCE_DP * density
        if (armed != tile.armed) {
            tile.armed = armed
            if (armed) feedback?.armTick()
            tile.tintTarget = if (armed) 1f else 0f
        }
        drawTick += 1
        requestFrames()
    }

    fun releaseLift(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
    ) {
        val tile = lifted ?: return
        val dx = x - tile.touchStartX
        val dy = y - tile.touchStartY
        val speed = hypot(vx, vy)
        val distance = hypot(dx, dy)
        val flicked = speed > THROW_SPEED_DP * density && (vx * dx + vy * dy >= 0f || distance < 20f * density)
        if (!(distance > THROW_DISTANCE_DP * density || flicked)) {
            cancelLift(animated = true)
            return
        }
        if (onTrash?.invoke(items[tile.item]) != true) {
            cancelLift(animated = true)
            return
        }
        throwAway(tile, vx, vy, dx, dy)
    }

    private fun throwAway(
        tile: Floating,
        vx: Float,
        vy: Float,
        dx: Float,
        dy: Float,
    ) {
        lifted = null
        val key = tile.key ?: return
        feedback?.commit()
        feedback?.deleteSound()

        // Direction follows the hand: a fast flick by its velocity, a slow drag by where it went.
        var dirX = if (hypot(vx, vy) > 300f * density) vx else dx
        var dirY = if (hypot(vx, vy) > 300f * density) vy else dy
        if (hypot(dirX, dirY) < 1f) {
            dirX = 0f
            dirY = -1f
        }
        val length = hypot(dirX, dirY)
        dirX /= length
        dirY /= length
        val tileSize = max(tile.width, tile.height) * scale
        val travel = hypot(viewWidth, viewHeight) + tileSize
        val speed = hypot(vx, vy).coerceIn(1600f * density, 4200f * density)
        val exitX = tile.cx + dirX * travel
        val exitY = tile.cy + dirY * travel
        val spin = (if (dirX >= 0f) 1f else -1f) * 0.5f

        fadeOutCopies(key.item)
        val position = removeFromWall(key)
        // No longer on the wall: undo during the flight brings back a different tile, this one flies on.
        detached.remove(key)
        history.add(Removal(key.item, position ?: 0, exitX, exitY, tile.rotation + spin))
        publishState()
        contentAlphaTarget = 1f

        tile.key = null
        tile.mode = MODE_LINEAR
        tile.fromX = tile.cx
        tile.fromY = tile.cy
        tile.fromRotation = tile.rotation
        tile.targetX = exitX
        tile.targetY = exitY
        tile.targetRotation = tile.rotation + spin
        tile.linearStart = System.nanoTime()
        tile.linearDuration = ((travel / speed) * 1e9f).toLong().coerceAtLeast(1L)
        applyTransform()
        requestFrames()
    }

    // The thrown photo's other copies shrink and fade where they stand; their columns then close up.
    private fun fadeOutCopies(item: Int) {
        val now = System.nanoTime()
        for (i in 0 until visibleCount) {
            if (visibleItem[i] != item) continue
            ghosts.add(Ghost(item, visibleX[i], visibleY[i], visibleW[i], visibleH[i], now))
        }
    }

    // Takes a photo off the wall: its column slides from the old layout to the new one, and the phase
    // moves so the top half of the cycle it left stays where it is.
    private fun removeFromWall(key: Instance): Int? {
        val column = layout.itemColumn[key.item]
        beginColumnTransition(column)
        val delta = layout.itemHeight[key.item] + layout.gap
        val position = layout.remove(key.item)
        phase[column] += (key.ky + cycleShift(key.kx)) * delta
        removed.add(key.item)
        requestFrames()
        return position
    }

    private fun beginColumnTransition(column: Int) {
        val now = System.nanoTime()
        updateTransitionProgress(now)
        val fromY = HashMap<Int, Float>()
        for (item in layout.columnItems(column)) fromY[item] = animatedY(item, column)
        columnTransitions[column] = ColumnTransition(fromY, animatedPhase(column), animatedPeriod(column), now)
        columnProgress[column] = 0f
    }

    fun cancelLift(animated: Boolean) {
        val tile = lifted ?: return
        lifted = null
        val key = tile.key
        val batch = generation
        val finish = {
            if (key != null) detached.remove(key)
            if (batch == generation) applyTransform()
        }
        contentAlphaTarget = 1f
        if (!animated || key == null) {
            floating.remove(tile)
            contentAlpha = 1f
            finish()
            return
        }
        feedback?.snapBack()
        tile.tintTarget = 0f
        tile.mode = MODE_SPRING
        tile.targetRotation = 0f
        tile.targetMul = 1f
        tile.stiffness = 150f
        tile.damping = 0.78f
        tile.vx = 0f
        tile.vy = 0f
        tile.lifted = true
        tile.onDone = finish
        requestFrames()
    }

    // MARK: Undo

    // Brings back the photo thrown last: back into its old place in the column, flying onto the copy
    // nearest the middle of the screen. When that copy is off screen the wall moves there first.
    fun undo() {
        if (lifted != null || history.isEmpty()) return
        val last = history.removeAt(history.lastIndex)
        val index = last.item
        onRestore?.invoke(items[index])
        val column = layout.itemColumn[index]
        beginColumnTransition(column)
        val oldPeriod = layout.columnPeriod[column]
        layout.insert(index, last.position)
        val height = layout.itemHeight[index]
        val width = layout.columnWidth[column]
        val center = canvasPoint(viewWidth / 2f, viewHeight / 2f)
        val kx = ((center.x - layout.columnX[column] - width / 2f) / layout.periodWidth).roundToInt()
        val shift = cycleShift(kx)
        val ky: Int
        if (oldPeriod > 0f) {
            // Nearest cycle by the old cycle height, then move the phase so its top half stays put.
            ky = ((center.y - phase[column] - shift * oldPeriod - layout.itemY[index] - height / 2f) / oldPeriod).roundToInt()
            phase[column] -= (ky + shift) * (height + layout.gap)
        } else {
            // The column was empty: land in the row at the middle of the screen.
            ky = 0
            phase[column] = (center.y - height / 2f - shift * layout.columnPeriod[column]).roundToInt().toFloat()
        }
        val key = Instance(index, kx, ky)
        removed.remove(index)
        detached.add(key)
        arrivingItem = index
        arrivingStartNanos = System.nanoTime()
        publishState()

        val frame = canvasFrame(key)
        val targetX = screenX(frame.center.x)
        val targetY = screenY(frame.center.y)
        val inset = topInset * 0.5f
        val visible = targetX in 0f..viewWidth && targetY in inset..(viewHeight - inset)
        if (!visible) {
            val destinationX = frame.center.x - viewWidth / 2f / scale
            val destinationY = frame.center.y - viewHeight / 2f / scale
            val batch = generation
            velocityX = 0f
            velocityY = 0f
            originAnimation =
                OriginAnimation(originX, originY, destinationX, destinationY, System.nanoTime(), 380_000_000L) {
                    if (batch == generation) flyBack(key, last)
                }
        } else {
            flyBack(key, last)
        }
        feedback?.lightTap()
        applyTransform()
        requestFrames()
    }

    private fun flyBack(
        key: Instance,
        removal: Removal,
    ) {
        val frame = canvasFrame(key)
        val batch = generation
        val tile =
            Floating(key.item, frame.width, frame.height, key).apply {
                cx = removal.exitX
                cy = removal.exitY
                rotation = removal.exitRotation
                mul = LIFT_MUL
                mode = MODE_SPRING
                targetRotation = 0f
                targetMul = 1f
                stiffness = 120f
                damping = 0.84f
                lifted = true
            }
        tile.onDone = {
            if (batch == generation) {
                if (arrivingItem == key.item) arrivingItem = -1
                detached.remove(key)
                applyTransform()
            }
        }
        floating.add(tile)
        feedback?.returnSound()
        requestFrames()
    }

    companion object {
        const val MIN_SCALE = 0.4f
        const val MAX_SCALE = 2.6f
        const val LIFT_MUL = 1.07f
        private const val THROW_DISTANCE_DP = 110f
        private const val THROW_SPEED_DP = 700f
        private const val DECELERATION_PER_MS = 0.998f
        private const val COLUMN_TRANSITION_NANOS = 500_000_000f
        const val GHOST_NANOS = 220_000_000L
        const val ARRIVING_DELAY_NANOS = 80_000_000L
        const val ARRIVING_FADE_NANOS = 300_000_000L
        private const val ARRIVING_TOTAL_NANOS = ARRIVING_DELAY_NANOS + ARRIVING_FADE_NANOS
        val IMAGE_BUCKETS = intArrayOf(224, 320, 512, 768, 1024)

        internal const val MODE_FOLLOW = 0
        internal const val MODE_SPRING = 1
        internal const val MODE_LINEAR = 2

        private fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)

        // Normalized critically damped spring: fast start, no overshoot; closing up reads as a push.
        fun criticalSpring(t: Float): Float {
            val x = 9f * t
            return min(1f, 1f - (1f + x) * exp(-x))
        }

        // One semi-implicit Euler step of a damped spring, sub-stepped so stiff springs stay stable.
        private fun springStep(
            x: Float,
            v: Float,
            target: Float,
            stiffness: Float,
            damping: Float,
            dt: Float,
        ): Pair<Float, Float> {
            var position = x
            var velocity = v
            val c = 2f * damping * sqrt(stiffness)
            val steps = max(1, (dt / (1f / 240f)).toInt())
            val h = dt / steps
            repeat(steps) {
                val a = -stiffness * (position - target) - c * velocity
                velocity += a * h
                position += velocity * h
            }
            return position to velocity
        }
    }
}
