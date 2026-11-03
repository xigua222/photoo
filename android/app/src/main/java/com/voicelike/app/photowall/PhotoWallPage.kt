package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// Photo wall page (iOS PhotoWallView): another way to organize, entered from the card above
// Deep Organize on the organize home. The wall holds the same photos as the home's "Organize N"
// button: in the current range, not organized yet and not pending delete. Throwing a photo away
// puts it in the same pending-delete queue as card sorting; the pending button at the top right and
// undo at the bottom behave as on the sorting page. The wall itself lives in PhotoWallEngine.

// Whether the wall was ever opened (the home entry stops showing NEW) and whether a photo was
// ever thrown (the bottom hint stops showing).
internal object PhotoWallPreferences {
    private const val PREFS_NAME = "photoo_photo_wall"
    private const val KEY_VISITED = "visited"
    private const val KEY_HINT_SEEN = "throw_hint_seen"
    private var loaded = false

    var visited by mutableStateOf(false)
        private set
    var hintSeen by mutableStateOf(false)
        private set

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        visited = prefs.getBoolean(KEY_VISITED, false)
        hintSeen = prefs.getBoolean(KEY_HINT_SEEN, false)
    }

    fun markVisited(context: Context) {
        if (visited) return
        visited = true
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VISITED, true)
            .apply()
    }

    fun markHintSeen(context: Context) {
        if (hintSeen) return
        hintSeen = true
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HINT_SEEN, true)
            .apply()
    }
}

// The same photos as the organize queue (range rules, skipping organized and pending delete), newest first.
internal fun selectPhotoWallItems(
    media: List<MediaItem>,
    pendingTrashIds: Set<Long>,
    processedIds: Set<Long>,
    hiddenFolders: Set<String>,
    criteria: MediaFilterCriteria,
): List<MediaItem> {
    val active = selectActiveCatalogMedia(media, pendingTrashIds, hiddenFolders)
    return applyMediaFilter(active, criteria)
        .filter { it.id !in processedIds }
        .sortedByDescending { it.dateAdded }
}

// The newest [limit] of already-filtered queue photos, for the home entry's little wall.
internal fun newestPhotoWallPreview(
    candidates: List<MediaItem>,
    processedIds: Set<Long>,
    limit: Int = PHOTO_WALL_PREVIEW_COUNT,
): List<MediaItem> {
    val picked = ArrayList<MediaItem>(limit + 1)
    for (item in candidates) {
        if (item.id in processedIds) continue
        if (picked.size == limit && item.dateAdded <= picked.last().dateAdded) continue
        var at = picked.size
        while (at > 0 && picked[at - 1].dateAdded < item.dateAdded) at -= 1
        picked.add(at, item)
        if (picked.size > limit) picked.removeAt(picked.lastIndex)
    }
    return picked
}

internal const val PHOTO_WALL_PREVIEW_COUNT = 9

private class PhotoWallViewerPresentation(
    val items: List<MediaItem>,
    val initialIndex: Int,
    val boundsById: Map<Long, Rect>,
)

@Composable
internal fun PhotoWallPage(
    // Null while the queue is still being read.
    items: List<MediaItem>?,
    title: String,
    emptyMessage: String,
    pendingCount: Int,
    onBack: () -> Unit,
    onOpenPendingDelete: () -> Unit,
    onTrash: (MediaItem) -> Boolean,
    onRestore: (MediaItem) -> Unit,
    playDeleteSound: () -> Unit,
    playReturnSound: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val wallStrings = photoWallStrings(language)
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val scope = rememberCoroutineScope()

    val imageStore = remember { PhotoWallImageStore(context.applicationContext, scope) }
    val engine = remember { PhotoWallEngine(density) }
    var viewer by remember { mutableStateOf<PhotoWallViewerPresentation?>(null) }
    var wallOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var topBarHeight by remember { mutableIntStateOf(0) }

    val currentOnTrash by rememberUpdatedState(onTrash)
    val currentOnRestore by rememberUpdatedState(onRestore)
    val currentDelete by rememberUpdatedState(playDeleteSound)
    val currentReturn by rememberUpdatedState(playReturnSound)
    val currentHaptics by rememberUpdatedState(hapticsEnabled)

    LaunchedEffect(Unit) {
        PhotoWallPreferences.load(context)
        PhotoWallPreferences.markVisited(context)
    }
    DisposableEffect(engine) {
        engine.images = imageStore
        engine.onTrash = { item ->
            val accepted = currentOnTrash(item)
            if (accepted) PhotoWallPreferences.markHintSeen(context)
            accepted
        }
        engine.onRestore = { item -> currentOnRestore(item) }
        engine.onOpen = { list, index, rects ->
            val offset = wallOffsetInRoot
            viewer =
                PhotoWallViewerPresentation(
                    items = list,
                    initialIndex = index,
                    boundsById = rects.mapValues { (_, rect) -> rect.translate(offset) },
                )
        }
        engine.feedback =
            object : PhotoWallEngine.Feedback {
                override fun lift() = haptic(view, currentHaptics, HapticFeedbackConstants.LONG_PRESS)

                override fun armTick() =
                    haptic(
                        view,
                        currentHaptics,
                        if (Build.VERSION.SDK_INT >=
                            34
                        ) {
                            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
                        } else {
                            HapticFeedbackConstants.CLOCK_TICK
                        },
                    )

                override fun commit() =
                    haptic(
                        view,
                        currentHaptics,
                        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
                    )

                override fun snapBack() =
                    haptic(
                        view,
                        currentHaptics,
                        if (Build.VERSION.SDK_INT >=
                            34
                        ) {
                            HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
                        } else {
                            HapticFeedbackConstants.CLOCK_TICK
                        },
                    )

                override fun lightTap() = haptic(view, currentHaptics, HapticFeedbackConstants.KEYBOARD_TAP)

                override fun deleteSound() = currentDelete()

                override fun returnSound() = currentReturn()
            }
        onDispose {
            engine.onTrash = null
            engine.onRestore = null
            engine.onOpen = null
            engine.feedback = null
            imageStore.clear()
        }
    }
    LaunchedEffect(items) {
        if (items != null) engine.setItems(items)
    }
    // Frame loop: runs only while something on the wall moves.
    LaunchedEffect(engine) {
        for (signal in engine.wake) {
            while (engine.needsFrames()) {
                androidx.compose.runtime.withFrameNanos { engine.tick(it) }
            }
        }
    }

    val loaded = items != null
    val isEmpty = loaded && engine.remainingCount == 0
    val wallAlpha by animateFloatAsState(if (isEmpty) 0f else 1f, tween(250), label = "photo-wall-empty")

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        PhotoWallCanvas(
            engine = engine,
            imageStore = imageStore,
            isDark = isDark,
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { wallOffsetInRoot = it.positionInRoot() }
                    .alpha(wallAlpha)
                    .semantics { contentDescription = wallStrings.title },
        )

        // The wall runs under the top bar; a fade keeps the bar readable over the photos.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { topBarHeight.toDp() } + 28.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to background,
                            0.6f to background.copy(alpha = 0.94f),
                            1f to background.copy(alpha = 0f),
                        ),
                    ),
        )
        UnifiedSubpageTopBar(
            title = title,
            onBack = onBack,
            modifier =
                Modifier.onSizeChanged {
                    topBarHeight = it.height
                    engine.updateTopInset(it.height.toFloat())
                },
            trailing = {
                MainDeleteQueueButton(pendingCount = pendingCount, onClick = onOpenPendingDelete)
            },
        )

        AnimatedVisibility(
            visible = isEmpty,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            PhotoWallEmptyState(message = emptyMessage)
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = !PhotoWallPreferences.hintSeen && loaded && engine.remainingCount > 0,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
            ) {
                PhotoWallHint(text = wallStrings.hint, isDark = isDark)
            }
            AnimatedVisibility(
                visible = engine.canUndo,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.8f),
            ) {
                PhotoWallUndoButton(label = strings.undo, isDark = isDark, onClick = { engine.undo() })
            }
        }

        viewer?.let { presentation ->
            PhotoWallViewer(
                presentation = presentation,
                onSharedDismissStart = { id -> engine.hideNearestCopy(id) },
                onDismiss = {
                    viewer = null
                    engine.clearHidden()
                },
            )
        }
    }
}

private fun haptic(
    view: View,
    enabled: Boolean,
    constant: Int,
) {
    if (!enabled) return
    view.performHapticFeedback(constant)
}

// MARK: Canvas

private const val LIFT_DELAY_MS = 260L
private const val GESTURE_UNDECIDED = 0
private const val GESTURE_PAN = 1
private const val GESTURE_PINCH = 2
private const val GESTURE_LIFT = 3

// Held still past the lift delay but not on a photo: no tap, a later move still pans.
private const val GESTURE_HELD = 4

@Composable
private fun PhotoWallCanvas(
    engine: PhotoWallEngine,
    imageStore: PhotoWallImageStore,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val destructive = MaterialTheme.colorScheme.error
    val painter =
        remember(isDark, density, destructive) {
            PhotoWallPainter(
                density = density,
                placeholder = if (isDark) 0xFF2A2A2E.toInt() else 0xFFE4E4EA.toInt(),
                border = if (isDark) Color.White.copy(alpha = 0.1f).toArgb() else Color.Black.copy(alpha = 0.06f).toArgb(),
                destructive = destructive.toArgb(),
            )
        }
    Canvas(
        modifier =
            modifier
                .onSizeChanged { engine.onSizeChanged(it.width.toFloat(), it.height.toFloat()) }
                .photoWallGestures(engine),
    ) {
        // Subscribe to wall motion and arriving images; everything else is plain engine state.
        engine.drawTick
        imageStore.version
        drawIntoCanvas { canvas -> painter.draw(canvas.nativeCanvas, engine, imageStore) }
    }
}

private fun Modifier.photoWallGestures(engine: PhotoWallEngine): Modifier =
    pointerInput(engine) {
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            engine.touchDown()
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var mode = GESTURE_UNDECIDED
            var pointerId = down.id
            var last = down.position
            val liftAt = SystemClock.uptimeMillis() + LIFT_DELAY_MS
            try {
                while (true) {
                    val event: PointerEvent? =
                        if (mode == GESTURE_UNDECIDED) {
                            val wait = liftAt - SystemClock.uptimeMillis()
                            if (wait <= 0L) null else withTimeoutOrNull(wait) { awaitPointerEvent() }
                        } else {
                            awaitPointerEvent()
                        }
                    if (event == null) {
                        // Held still long enough: lift the photo under the finger.
                        if (engine.canLift(last.x, last.y)) {
                            mode = GESTURE_LIFT
                            engine.beginLift(last.x, last.y)
                            tracker.resetTracking()
                            tracker.addPosition(SystemClock.uptimeMillis(), last)
                        } else {
                            mode = GESTURE_HELD
                        }
                        continue
                    }
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) {
                        val up = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.first()
                        tracker.addPosition(up.uptimeMillis, up.position)
                        when (mode) {
                            GESTURE_UNDECIDED -> engine.tap(up.position.x, up.position.y)
                            GESTURE_PAN -> {
                                val v = tracker.calculateVelocity()
                                engine.endPan(v.x, v.y)
                            }
                            GESTURE_PINCH -> engine.endPinch()
                            GESTURE_LIFT -> {
                                val v = tracker.calculateVelocity()
                                engine.releaseLift(up.position.x, up.position.y, v.x, v.y)
                            }
                        }
                        event.changes.forEach { it.consume() }
                        break
                    }
                    if (mode != GESTURE_LIFT && pressed.size >= 2) {
                        val centroid = event.calculateCentroid(useCurrent = true)
                        if (mode != GESTURE_PINCH) {
                            mode = GESTURE_PINCH
                            engine.beginPinch(centroid)
                        } else {
                            engine.pinch(centroid, event.calculateZoom())
                        }
                        event.changes.forEach { it.consume() }
                        continue
                    }
                    val change = pressed.firstOrNull { it.id == pointerId } ?: pressed.first().also { pointerId = it.id }
                    when (mode) {
                        GESTURE_UNDECIDED, GESTURE_HELD -> {
                            if ((change.position - down.position).getDistance() > touchSlop) {
                                mode = GESTURE_PAN
                                engine.beginPan()
                                tracker.resetTracking()
                                tracker.addPosition(change.uptimeMillis, change.position)
                                val delta = change.positionChange()
                                engine.panBy(delta.x, delta.y)
                            }
                        }
                        GESTURE_PAN -> {
                            tracker.addPosition(change.uptimeMillis, change.position)
                            val delta = change.positionChange()
                            engine.panBy(delta.x, delta.y)
                        }
                        GESTURE_PINCH -> {
                            // Down to one finger: the pinch settles and that finger keeps dragging.
                            engine.endPinch()
                            mode = GESTURE_PAN
                            engine.beginPan()
                            tracker.resetTracking()
                            tracker.addPosition(change.uptimeMillis, change.position)
                        }
                        GESTURE_LIFT -> {
                            tracker.addPosition(change.uptimeMillis, change.position)
                            engine.dragLift(change.position.x, change.position.y)
                        }
                    }
                    last = change.position
                    change.consume()
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                engine.cancelLift(animated = true)
                engine.endPinch()
                throw cancelled
            } finally {
                engine.touchUp()
            }
        }
    }

// Draws the wall with the platform canvas: one rounded-rect shader draw per photo, so even a
// zoomed-out wall of a few hundred tiles stays cheap.
private class PhotoWallPainter(
    private val density: Float,
    private val placeholder: Int,
    private val border: Int,
    private val destructive: Int,
) {
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val rect = RectF()
    private val badges = HashMap<String, Bitmap>()
    private val trashBadge: Bitmap by lazy { drawTrashBadge() }

    fun draw(
        canvas: android.graphics.Canvas,
        engine: PhotoWallEngine,
        store: PhotoWallImageStore,
    ) {
        val items = engine.items
        if (items.isEmpty()) return
        val scale = engine.scale
        val now = System.nanoTime()
        val radius = engine.screenCornerRadius / scale
        val hairline = 0.5f * density / scale
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(-engine.originX, -engine.originY)
        for (i in 0 until engine.visibleCount) {
            val index = engine.visibleItem[i]
            if (engine.isHidden(index, engine.visibleKx[i], engine.visibleKy[i])) continue
            var alpha = engine.contentAlpha
            if (index == engine.arrivingItem) {
                val t =
                    (
                        (now - engine.arrivingStartNanos - PhotoWallEngine.ARRIVING_DELAY_NANOS).toFloat() /
                            PhotoWallEngine.ARRIVING_FADE_NANOS
                    ).coerceIn(0f, 1f)
                alpha *= 1f - (1f - t).pow(2)
            }
            val item = items[index]
            drawTile(
                canvas,
                store,
                item,
                engine.visibleX[i],
                engine.visibleY[i],
                engine.visibleW[i],
                engine.visibleH[i],
                radius,
                hairline,
                alpha,
                scale,
            )
        }
        for (ghost in engine.ghosts) {
            val t = ((now - ghost.start).toFloat() / PhotoWallEngine.GHOST_NANOS).coerceIn(0f, 1f)
            val e = t * t
            val shrink = 1f - 0.14f * e
            val w = ghost.w * shrink
            val h = ghost.h * shrink
            val item = items.getOrNull(ghost.item) ?: continue
            drawTile(
                canvas,
                store,
                item,
                ghost.x + (ghost.w - w) / 2f,
                ghost.y + (ghost.h - h) / 2f,
                w,
                h,
                radius,
                hairline,
                engine.contentAlpha * (1f - e),
                scale,
                showBadge = false,
            )
        }
        canvas.restore()

        // Photos in hand or in flight, in screen coordinates above the wall.
        for (tile in engine.floating) {
            val item = items.getOrNull(tile.item) ?: continue
            val k = scale * tile.mul
            canvas.save()
            canvas.translate(tile.cx, tile.cy)
            canvas.rotate(Math.toDegrees(tile.rotation.toDouble()).toFloat())
            canvas.scale(k, k)
            val left = -tile.width / 2f
            val top = -tile.height / 2f
            val tileRadius = engine.screenCornerRadius / scale
            if (tile.lifted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                shadowPaint.color = placeholder
                shadowPaint.setShadowLayer(18f * density / k, 0f, 10f * density / k, 0x47000000)
                rect.set(left, top, left + tile.width, top + tile.height)
                canvas.drawRoundRect(rect, tileRadius, tileRadius, shadowPaint)
            }
            drawTile(canvas, store, item, left, top, tile.width, tile.height, tileRadius, 0.5f * density / k, 1f, k)
            if (tile.tint > 0f) {
                // Past the throw distance: a red veil and a trash can in the middle; letting go deletes.
                fillPaint.color = destructive
                fillPaint.alpha = (0.28f * 255f * tile.tint).toInt()
                rect.set(left, top, left + tile.width, top + tile.height)
                canvas.drawRoundRect(rect, tileRadius, tileRadius, fillPaint)
                val side = min(48f * density / k, tile.width * 0.6f)
                rect.set(-side / 2f, -side / 2f, side / 2f, side / 2f)
                badgePaint.alpha = (255f * tile.tint).toInt()
                canvas.drawBitmap(trashBadge, null, rect, badgePaint)
                badgePaint.alpha = 255
            }
            canvas.restore()
        }
    }

    private fun drawTile(
        canvas: android.graphics.Canvas,
        store: PhotoWallImageStore,
        item: MediaItem,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        hairline: Float,
        alpha: Float,
        scale: Float,
        showBadge: Boolean = true,
    ) {
        if (alpha <= 0.003f) return
        val alphaByte = (alpha * 255f).toInt().coerceIn(0, 255)
        rect.set(x, y, x + w, y + h)
        val entry = store.peek(item.id)
        if (entry == null) {
            fillPaint.color = placeholder
            fillPaint.alpha = (alphaByte * android.graphics.Color.alpha(placeholder) / 255f).toInt()
            canvas.drawRoundRect(rect, radius, radius, fillPaint)
        } else {
            val bitmap = entry.bitmap
            val bw = bitmap.width.toFloat().coerceAtLeast(1f)
            val bh = bitmap.height.toFloat().coerceAtLeast(1f)
            val fit = max(w / bw, h / bh)
            matrix.setScale(fit, fit)
            matrix.postTranslate(x + (w - bw * fit) / 2f, y + (h - bh * fit) / 2f)
            // Before API 26 a shader is mutated in place, so copies of one photo drawn in the same
            // frame would share the last matrix: give each draw its own shader there.
            val shader =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    entry.shader
                } else {
                    BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                }
            shader.setLocalMatrix(matrix)
            imagePaint.shader = shader
            imagePaint.alpha = alphaByte
            canvas.drawRoundRect(rect, radius, radius, imagePaint)
            imagePaint.shader = null
        }
        borderPaint.color = border
        borderPaint.alpha = (alphaByte * android.graphics.Color.alpha(border) / 255f).toInt()
        borderPaint.strokeWidth = hairline
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
        if (showBadge) drawBadge(canvas, item, x, y, w, h, scale, alphaByte)
    }

    // Video duration and LIVE badges keep their size on screen: on a zoomed-out wall they shrink a
    // little (down to 0.6), on a zoomed-in one they do not grow.
    private fun drawBadge(
        canvas: android.graphics.Canvas,
        item: MediaItem,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        scale: Float,
        alphaByte: Int,
    ) {
        val badge = badgeFor(item) ?: return
        val k = scale.coerceIn(0.6f, 1f) / scale
        val bw = badge.width * k
        val bh = badge.height * k
        val inset = 5f * density / scale
        if (bw + inset * 2f > w || bh + inset * 2f > h) return
        rect.set(x + w - inset - bw, y + h - inset - bh, x + w - inset, y + h - inset)
        badgePaint.alpha = alphaByte
        canvas.drawBitmap(badge, null, rect, badgePaint)
        badgePaint.alpha = 255
    }

    private fun badgeFor(item: MediaItem): Bitmap? {
        val text =
            when {
                item.type == "video" -> durationText(item.duration)
                item.isMotionPhoto || item.isLivePhoto -> "LIVE"
                else -> return null
            }
        val key = "${item.type}|$text"
        return badges.getOrPut(key) { drawBadgeBitmap(isVideo = item.type == "video", text = text) }
    }

    private fun durationText(durationMs: Long): String {
        if (durationMs <= 0L) return ""
        val total = durationMs / 1000L
        val h = total / 3600L
        val m = (total % 3600L) / 60L
        val s = total % 60L
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
    }

    // Half-transparent black capsule with a glyph and the text, as on the browse grids.
    private fun drawBadgeBitmap(
        isVideo: Boolean,
        text: String,
    ): Bitmap {
        val height = 22f * density
        val padding = 7f * density
        val spacing = 3f * density
        val glyph = 9f * density
        val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 12f * density
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.025f
            }
        val textWidth = if (text.isEmpty()) 0f else textPaint.measureText(text)
        val width = padding * 2f + glyph + if (text.isEmpty()) 0f else spacing + textWidth
        val bitmap = Bitmap.createBitmap(kotlin.math.ceil(width).toInt(), kotlin.math.ceil(height).toInt(), Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x73000000 }
        c.drawRoundRect(RectF(0f, 0f, width, height), height / 2f, height / 2f, paint)
        paint.color = android.graphics.Color.WHITE
        val gx = padding
        val gy = (height - glyph) / 2f
        if (isVideo) {
            val path =
                Path().apply {
                    moveTo(gx + glyph * 0.12f, gy)
                    lineTo(gx + glyph, gy + glyph / 2f)
                    lineTo(gx + glyph * 0.12f, gy + glyph)
                    close()
                }
            c.drawPath(path, paint)
        } else {
            // Live glyph: a dot inside a ring.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.4f * density
            c.drawCircle(gx + glyph / 2f, height / 2f, glyph / 2f - paint.strokeWidth / 2f, paint)
            paint.style = Paint.Style.FILL
            c.drawCircle(gx + glyph / 2f, height / 2f, glyph * 0.2f, paint)
        }
        if (text.isNotEmpty()) {
            val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            c.drawText(text, gx + glyph + spacing, baseline, textPaint)
        }
        return bitmap
    }

    // Red disc with a white trash can, drawn once.
    private fun drawTrashBadge(): Bitmap {
        val side = 48f * density
        val bitmap = Bitmap.createBitmap(side.toInt(), side.toInt(), Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = destructive }
        c.drawCircle(side / 2f, side / 2f, side / 2f, paint)
        val u = side / 48f
        val cx = side / 2f
        val cy = side / 2f
        paint.color = android.graphics.Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f * u
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        c.drawLine(cx - 9f * u, cy - 6f * u, cx + 9f * u, cy - 6f * u, paint)
        val handle =
            Path().apply {
                moveTo(cx - 3.5f * u, cy - 6f * u)
                lineTo(cx - 3.5f * u, cy - 9f * u)
                lineTo(cx + 3.5f * u, cy - 9f * u)
                lineTo(cx + 3.5f * u, cy - 6f * u)
            }
        c.drawPath(handle, paint)
        val body =
            Path().apply {
                moveTo(cx - 7f * u, cy - 6f * u)
                lineTo(cx - 6f * u, cy + 9f * u)
                lineTo(cx + 6f * u, cy + 9f * u)
                lineTo(cx + 7f * u, cy - 6f * u)
            }
        c.drawPath(body, paint)
        c.drawLine(cx - 2.2f * u, cy - 2f * u, cx - 2.2f * u, cy + 5f * u, paint)
        c.drawLine(cx + 2.2f * u, cy - 2f * u, cx + 2.2f * u, cy + 5f * u, paint)
        return bitmap
    }
}

// MARK: Chrome

@Composable
private fun PhotoWallHint(
    text: String,
    isDark: Boolean,
) {
    val shape = RoundedCornerShape(18.dp)
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .padding(horizontal = 32.dp)
                .background(if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f), shape)
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), shape)
                .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

// Same look as the undo on the sorting pages; the only button on the wall, so it carries its label.
@Composable
private fun PhotoWallUndoButton(
    label: String,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
        shape = RoundedCornerShape(30.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = Modifier.bounceClick(),
    ) {
        Icon(
            imageVector = Icons.Outlined.Replay,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun PhotoWallEmptyState(message: String) {
    val tint = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(92.dp)
                    .background(tint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(38.dp))
        }
        Text(
            text = message,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

// MARK: Viewer

@Composable
private fun PhotoWallViewer(
    presentation: PhotoWallViewerPresentation,
    onSharedDismissStart: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialItem = presentation.items.getOrNull(presentation.initialIndex)
    SharedPagedFullScreenImageViewer(
        items = presentation.items,
        initialIndex = presentation.initialIndex,
        onDismiss = onDismiss,
        sourceBoundsInRoot = initialItem?.let { presentation.boundsById[it.id] },
        sourceBoundsByItemId = presentation.boundsById,
        onSharedDismissStart = onSharedDismissStart,
        orientation = SharedPagedViewerOrientation.Horizontal,
        sharedTargetCornerRadius = 8.dp,
        sharedScaleClampMin = 0.04f,
        chrome = { _, _, requestDismiss ->
            IconButton(
                onClick = requestDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape),
            ) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = LocalAppStrings.current.back, tint = Color.White)
            }
        },
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) { _, item, requestDismiss, _, _, onZoomStateChanged ->
        if (item.type == "video") {
            PhotoWallVideoPage(item = item, onDismiss = requestDismiss, onZoomStateChanged = onZoomStateChanged)
        } else {
            SimilarImageViewerPage(
                item = item,
                onDismiss = requestDismiss,
                isMuted = false,
                allowLivePlayback = true,
                compareOverrideItem = null,
                onZoomStateChanged = onZoomStateChanged,
            )
        }
    }
}

@Composable
private fun PhotoWallVideoPage(
    item: MediaItem,
    onDismiss: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit,
) {
    var isMuted by rememberSaveable(item.id) { mutableStateOf(false) }
    val dismissState = rememberFullScreenSwipeDismissState(onDismiss = onDismiss)
    LaunchedEffect(item.id) { onZoomStateChanged(true) }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .fullScreenSwipeToDismiss(enabled = true, state = dismissState)
                .graphicsLayer {
                    translationY = dismissState.offsetY
                    scaleX = dismissState.contentScale
                    scaleY = dismissState.contentScale
                },
    ) {
        FullScreenVideoPlayer(
            item = item,
            isMuted = isMuted,
            onMuteChange = { isMuted = it },
            onDismiss = {},
            dismissEnabled = false,
            enableOpenCloseTransition = false,
            showCloseButton = false,
            showTopBar = false,
            controlsBottomPadding = 150.dp,
        )
    }
}
