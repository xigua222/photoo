package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val HOME_CARD_COLOR_PICKER_DEFAULT_COUNT = 5
private const val HOME_CARD_COLOR_PICKER_MIN_COUNT = 1
private const val HOME_CARD_COLOR_PICKER_MAX_COUNT = 10
private const val HOME_CARD_COLOR_PICKER_PREFS = "home_card_color_picker"
private val COLOR_CARD_DELTA_E_WHITE_LAB = colorCardLabFromRgb(255, 255, 255)
private val COLOR_CARD_DELTA_E_BLACK_LAB = colorCardLabFromRgb(0, 0, 0)
private const val HOME_CARD_COLOR_PICKER_BITMAP_MAX_SIDE = 720
private const val HOME_CARD_COLOR_PICKER_PRECISE_BITMAP_MAX_SIDE = 1600
private val HOME_CARD_COLOR_PICKER_PLACEHOLDERS = listOf(
    0xFFD9D9D9.toInt(),
    0xFFE5B79D.toInt(),
    0xFFD98761.toInt(),
    0xFF8A6A66.toInt(),
    0xFF567366.toInt()
)

@Immutable
internal data class HomeCardColorPickerPoint(
    val x: Float,
    val y: Float
)

internal data class HomeCardColorPickerState(
    val isLoading: Boolean,
    val isReady: Boolean,
    val isPreciseMode: Boolean,
    val points: List<HomeCardColorPickerPoint>,
    val colors: List<Int>,
    val pointCount: Int,
    val canAddPoint: Boolean,
    val canRemovePoint: Boolean,
    val refresh: () -> Unit,
    val addPoint: () -> Unit,
    val removePoint: () -> Unit,
    val movePointBy: (Int, Float, Float) -> Unit,
    val setPreciseMode: (Boolean) -> Unit,
    val save: () -> Unit
)

internal data class ColorCardMetrics(
    val hex: String,
    val hexa: String,
    val rgb: String,
    val rgba: String,
    val hsb: String,
    val hsl: String,
    val lab: String,
    val deltaE00OnWhite: String,
    val deltaE00OnBlack: String,
    val cmyk: String,
    val contrastOnWhite: String,
    val contrastOnBlack: String,
    val wcagOnWhite: String,
    val wcagOnBlack: String,
    val wcagRecommendedText: String
)

@Composable
internal fun rememberHomeCardColorPickerState(
    active: Boolean,
    item: MediaItem,
    sourceMediaId: Long? = item.id,
    persistenceKey: String? = null
): HomeCardColorPickerState {
    val context = LocalContext.current
    val store = remember(context) { ColorCardStore.get(context) }
    val prefs = remember(context, persistenceKey) {
        persistenceKey?.let {
            context.applicationContext.getSharedPreferences(
                HOME_CARD_COLOR_PICKER_PREFS,
                Context.MODE_PRIVATE
            )
        }
    }
    val persistedPointsKey = remember(persistenceKey) {
        persistenceKey?.let { "points:$it" }
    }
    val preciseModeKey = remember(persistenceKey) {
        persistenceKey?.let { "precise_mode:$it" }
    }
    var samplingBitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var samplingBitmapMaxSide by remember(item.id) { mutableIntStateOf(0) }
    var isLoadingBitmap by remember(item.id) { mutableStateOf(false) }
    var refreshIndex by remember(item.id) { mutableIntStateOf(0) }
    var isPreciseMode by remember(item.id, preciseModeKey) {
        mutableStateOf(preciseModeKey?.let { key -> prefs?.getBoolean(key, false) } ?: false)
    }
    val initialPoints = remember(item.id, persistedPointsKey) {
        persistedPointsKey
            ?.let { key -> prefs?.getString(key, null) }
            ?.let(::decodePersistedHomeCardColorPickerPoints)
            ?.takeIf { it.size in HOME_CARD_COLOR_PICKER_MIN_COUNT..HOME_CARD_COLOR_PICKER_MAX_COUNT }
            ?: generateHomeCardColorPickerPoints(
                seed = item.id,
                count = HOME_CARD_COLOR_PICKER_DEFAULT_COUNT
            )
    }
    var points by remember(item.id, persistedPointsKey) {
        mutableStateOf(
            initialPoints
        )
    }

    val desiredSamplingBitmapMaxSide = if (isPreciseMode) {
        HOME_CARD_COLOR_PICKER_PRECISE_BITMAP_MAX_SIDE
    } else {
        HOME_CARD_COLOR_PICKER_BITMAP_MAX_SIDE
    }

    LaunchedEffect(item.id, item.uri, active, desiredSamplingBitmapMaxSide) {
        if (!active || isLoadingBitmap) return@LaunchedEffect
        if (
            samplingBitmap != null &&
            samplingBitmapMaxSide == desiredSamplingBitmapMaxSide
        ) {
            return@LaunchedEffect
        }
        isLoadingBitmap = true
        val decoded = decodeHomeCardColorPickerBitmap(
            context = context.applicationContext,
            uri = item.uri,
            maxSide = desiredSamplingBitmapMaxSide
        )
        if (active) {
            samplingBitmap?.takeIf { it !== decoded && !it.isRecycled }?.recycle()
            samplingBitmap = decoded
            samplingBitmapMaxSide = if (decoded != null) desiredSamplingBitmapMaxSide else 0
        } else {
            decoded?.takeIf { !it.isRecycled }?.recycle()
        }
        isLoadingBitmap = false
    }

    LaunchedEffect(persistedPointsKey, points) {
        val key = persistedPointsKey ?: return@LaunchedEffect
        prefs?.edit()?.putString(key, encodePersistedHomeCardColorPickerPoints(points))?.apply()
    }

    LaunchedEffect(preciseModeKey, isPreciseMode) {
        val key = preciseModeKey ?: return@LaunchedEffect
        prefs?.edit()?.putBoolean(key, isPreciseMode)?.apply()
    }

    DisposableEffect(item.id) {
        onDispose {
            samplingBitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    val sampledColors = remember(samplingBitmap, points, isPreciseMode) {
        samplingBitmap?.let { bitmap ->
            points.map { point ->
                sampleHomeCardColor(
                    bitmap = bitmap,
                    point = point,
                    preciseMode = isPreciseMode
                )
            }
        } ?: buildHomeCardColorPickerPlaceholderColors(points.size)
    }
    val canSave = samplingBitmap != null && sampledColors.size == points.size && points.isNotEmpty()
    val refreshPoints = {
        refreshIndex += 1
        points = generateHomeCardColorPickerPoints(
            seed = item.id + (refreshIndex.toLong() * 1_009L),
            count = points.size
        )
    }
    val addPoint = {
        if (points.size < HOME_CARD_COLOR_PICKER_MAX_COUNT) {
            points = points + generateHomeCardColorPickerPoint(
                seed = item.id + (refreshIndex.toLong() * 1_009L) + (points.size.toLong() * 7_919L),
                existing = points
            )
        }
    }
    val removePoint = {
        if (points.size > HOME_CARD_COLOR_PICKER_MIN_COUNT) {
            points = points.dropLast(1)
        }
    }
    val movePointBy: (Int, Float, Float) -> Unit = { index, deltaX, deltaY ->
        if (index in points.indices) {
            points = points.mapIndexed { pointIndex, point ->
                if (pointIndex != index) {
                    point
                } else {
                    point.copy(
                        x = (point.x + deltaX).coerceIn(0.08f, 0.92f),
                        y = (point.y + deltaY).coerceIn(0.08f, 0.92f)
                    )
                }
            }
        }
    }
    val savePalette: () -> Unit = {
        if (canSave) {
            val colorsToSave = sampledColors.take(points.size)
            val saved = store.savePaletteBlocking(
                colors = colorsToSave,
                sourceMediaId = sourceMediaId
            )
            Toast.makeText(
                context,
                if (saved != null) "\u5df2\u4fdd\u5b58\u5230\u8272\u5361" else "\u4fdd\u5b58\u5931\u8d25",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    return HomeCardColorPickerState(
        isLoading = isLoadingBitmap && !canSave,
        isReady = canSave,
        isPreciseMode = isPreciseMode,
        points = points,
        colors = sampledColors.take(points.size),
        pointCount = points.size,
        canAddPoint = points.size < HOME_CARD_COLOR_PICKER_MAX_COUNT,
        canRemovePoint = points.size > HOME_CARD_COLOR_PICKER_MIN_COUNT,
        refresh = refreshPoints,
        addPoint = addPoint,
        removePoint = removePoint,
        movePointBy = movePointBy,
        setPreciseMode = { isEnabled -> isPreciseMode = isEnabled },
        save = savePalette
    )
}

private fun encodePersistedHomeCardColorPickerPoints(
    points: List<HomeCardColorPickerPoint>
): String {
    return points.joinToString(separator = ";") { point ->
        "${point.x},${point.y}"
    }
}

private fun decodePersistedHomeCardColorPickerPoints(
    rawValue: String
): List<HomeCardColorPickerPoint>? {
    val points = rawValue
        .split(';')
        .mapNotNull { entry ->
            val parts = entry.split(',')
            if (parts.size != 2) return@mapNotNull null
            val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
            val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
            HomeCardColorPickerPoint(
                x = x.coerceIn(0.08f, 0.92f),
                y = y.coerceIn(0.08f, 0.92f)
            )
        }
    return points.takeIf { it.isNotEmpty() }
}

@Composable
internal fun BoxScope.HomeCardColorPickerImageOverlay(
    state: HomeCardColorPickerState,
    modifier: Modifier = Modifier
) {
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val latestMovePointBy = rememberUpdatedState(state.movePointBy)
    val touchTargetSize = 42.dp
    val handleSize = 24.dp
    val centerDotSize = 7.dp
    val touchTargetSizePx = with(density) { touchTargetSize.roundToPx() }

    Box(
        modifier = modifier
            .matchParentSize()
            .onSizeChanged { overlaySize = it }
    ) {
        state.points.forEachIndexed { index, point ->
            val fillColor = Color(state.colors.getOrElse(index) {
                HOME_CARD_COLOR_PICKER_PLACEHOLDERS[index % HOME_CARD_COLOR_PICKER_PLACEHOLDERS.size]
            })
            Box(
                modifier = Modifier
                    .offsetForPickerPoint(
                        point = point,
                        overlaySize = overlaySize,
                        handleSizePx = touchTargetSizePx
                    )
                    .size(touchTargetSize)
                    .pointerInput(overlaySize.width, overlaySize.height, index) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )
                            if (overlaySize.width <= 0 || overlaySize.height <= 0) {
                                down.consume()
                                return@awaitEachGesture
                            }
                            val pointerId = down.id
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                val dragDelta = change.position - change.previousPosition
                                if (dragDelta.x != 0f || dragDelta.y != 0f) {
                                    latestMovePointBy.value(
                                        index,
                                        dragDelta.x / overlaySize.width.toFloat(),
                                        dragDelta.y / overlaySize.height.toFloat()
                                    )
                                }
                                val stillPressed = change.pressed
                                change.consume()
                                if (!stillPressed) {
                                    break
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(handleSize)
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.18f),
                            spotColor = Color.Black.copy(alpha = 0.28f)
                        )
                        .clip(CircleShape)
                        .background(fillColor)
                        .border(2.dp, Color.White.copy(alpha = 0.96f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(centerDotSize)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.96f))
                    )
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.HomeCardColorPickerFooterOverlayLegacy(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    state: HomeCardColorPickerState,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (!active) return

    val panelShape = RoundedCornerShape(24.dp)
    val backgroundColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    } else {
        Color.White.copy(alpha = 0.74f)
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF171717)
    val secondaryText = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    } else {
        Color(0xFF626262)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 34.dp)
                .widthIn(max = 420.dp)
                .clip(panelShape)
                .background(backgroundColor)
                .border(1.dp, borderColor, panelShape)
                .pointerInput(active) {
                    if (!active) return@pointerInput
                    detectTapGestures(onTap = {})
                }
                .graphicsLayer {
                    alpha = visuals.footerContentAlpha
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = state.refresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "閸掗攱鏌婇懝鑼仯",
                        tint = primaryText,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = state.save,
                    enabled = state.isReady,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "娣囨繂鐡ㄩ懝鎻掑幢",
                        tint = if (state.isReady) primaryText else secondaryText,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = if (isDark) 0.04f else 0.26f)),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                state.colors.forEach { colorInt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(colorInt))
                    )
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.HomeCardColorPickerFooterOverlay(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    state: HomeCardColorPickerState,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (!active) return

    val panelShape = RoundedCornerShape(24.dp)
    val backgroundColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    } else {
        Color.White.copy(alpha = 0.74f)
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF171717)
    val secondaryText = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    } else {
        Color(0xFF626262)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 34.dp)
                .widthIn(max = 420.dp)
                .clip(panelShape)
                .background(backgroundColor)
                .border(1.dp, borderColor, panelShape)
                .pointerInput(active) {
                    if (!active) return@pointerInput
                    detectTapGestures(onTap = {})
                }
                .graphicsLayer {
                    alpha = visuals.footerContentAlpha
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                HomeCardColorPickerCountStepper(
                    pointCount = state.pointCount,
                    canAddPoint = state.canAddPoint,
                    canRemovePoint = state.canRemovePoint,
                    onAddPoint = state.addPoint,
                    onRemovePoint = state.removePoint,
                    contentColor = primaryText,
                    secondaryText = secondaryText,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = state.refresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "\u5237\u65b0\u53d6\u8272\u70b9",
                        tint = primaryText,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = state.save,
                    enabled = state.isReady,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "\u4fdd\u5b58\u8272\u5361",
                        tint = if (state.isReady) primaryText else secondaryText,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = if (isDark) 0.04f else 0.26f)),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                state.colors.forEach { colorInt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(colorInt))
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCardColorPickerCountStepper(
    pointCount: Int,
    canAddPoint: Boolean,
    canRemovePoint: Boolean,
    onAddPoint: () -> Unit,
    onRemovePoint: () -> Unit,
    contentColor: Color,
    secondaryText: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) {
                    Color.White.copy(alpha = 0.06f)
                } else {
                    Color.Black.copy(alpha = 0.045f)
                }
            )
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onRemovePoint,
            enabled = canRemovePoint,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = "\u51cf\u5c11\u53d6\u8272\u70b9",
                tint = if (canRemovePoint) contentColor else secondaryText,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = pointCount.toString(),
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        IconButton(
            onClick = onAddPoint,
            enabled = canAddPoint,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "\u589e\u52a0\u53d6\u8272\u70b9",
                tint = if (canAddPoint) contentColor else secondaryText,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

internal fun colorCardMetrics(colorInt: Int): ColorCardMetrics {
    val alpha = AndroidColor.alpha(colorInt)
    val red = AndroidColor.red(colorInt)
    val green = AndroidColor.green(colorInt)
    val blue = AndroidColor.blue(colorInt)

    val hsv = colorCardHsvFromRgb(red = red, green = green, blue = blue)
    val h = hsv.first.roundToInt()
    val s = hsv.second.roundToInt()
    val v = hsv.third.roundToInt()
    val hsl = colorCardHslFromRgb(red = red, green = green, blue = blue)
    val lab = colorCardLabFromRgb(red = red, green = green, blue = blue)
    val deltaE00OnWhite = colorCardDeltaE00(lab, COLOR_CARD_DELTA_E_WHITE_LAB)
    val deltaE00OnBlack = colorCardDeltaE00(lab, COLOR_CARD_DELTA_E_BLACK_LAB)

    val rf = red / 255f
    val gf = green / 255f
    val bf = blue / 255f
    val key = 1f - max(rf, max(gf, bf))
    val cyan: Int
    val magenta: Int
    val yellow: Int
    val black: Int
    if (key >= 0.999f) {
        cyan = 0
        magenta = 0
        yellow = 0
        black = 100
    } else {
        cyan = (((1f - rf - key) / (1f - key)) * 100f).roundToInt().coerceIn(0, 100)
        magenta = (((1f - gf - key) / (1f - key)) * 100f).roundToInt().coerceIn(0, 100)
        yellow = (((1f - bf - key) / (1f - key)) * 100f).roundToInt().coerceIn(0, 100)
        black = (key * 100f).roundToInt().coerceIn(0, 100)
    }

    val alphaRatio = alpha / 255f
    val contrastOnWhite = colorCardContrastRatio(
        foregroundColorInt = colorInt,
        backgroundColorInt = AndroidColor.WHITE
    )
    val contrastOnBlack = colorCardContrastRatio(
        foregroundColorInt = colorInt,
        backgroundColorInt = AndroidColor.BLACK
    )
    val prefersBlackText = contrastOnBlack >= contrastOnWhite
    val recommendedContrast = if (prefersBlackText) contrastOnBlack else contrastOnWhite

    return ColorCardMetrics(
        hex = String.format(Locale.US, "%02X%02X%02X", red, green, blue),
        hexa = String.format(Locale.US, "%02X%02X%02X%02X", red, green, blue, alpha),
        rgb = "$red $green $blue",
        rgba = String.format(Locale.US, "%d %d %d %.2f", red, green, blue, alphaRatio),
        hsb = "$h $s $v",
        hsl = "${hsl.first.roundToInt()} ${hsl.second.roundToInt()} ${hsl.third.roundToInt()}",
        lab = String.format(Locale.US, "%.1f %.1f %.1f", lab.first, lab.second, lab.third),
        deltaE00OnWhite = String.format(Locale.US, "%.2f", deltaE00OnWhite),
        deltaE00OnBlack = String.format(Locale.US, "%.2f", deltaE00OnBlack),
        cmyk = "$cyan $magenta $yellow $black",
        contrastOnWhite = String.format(Locale.US, "%.2f:1", contrastOnWhite),
        contrastOnBlack = String.format(Locale.US, "%.2f:1", contrastOnBlack),
        wcagOnWhite = colorCardWcagLabelSummary(contrastOnWhite),
        wcagOnBlack = colorCardWcagLabelSummary(contrastOnBlack),
        wcagRecommendedText = buildString {
            append(if (prefersBlackText) "\u9ed1\u5b57" else "\u767d\u5b57")
            append(" \u00b7 ")
            append(colorCardWcagLabelSummary(recommendedContrast))
        }
    )
}

private fun colorCardHsvFromRgb(
    red: Int,
    green: Int,
    blue: Int
): Triple<Float, Float, Float> {
    val rf = red / 255f
    val gf = green / 255f
    val bf = blue / 255f
    val maxChannel = max(rf, max(gf, bf))
    val minChannel = min(rf, min(gf, bf))
    val delta = maxChannel - minChannel
    val hue = when {
        delta == 0f -> 0f
        maxChannel == rf -> (((gf - bf) / delta) % 6f) * 60f
        maxChannel == gf -> (((bf - rf) / delta) + 2f) * 60f
        else -> (((rf - gf) / delta) + 4f) * 60f
    }.let { if (it < 0f) it + 360f else it }
    val saturation = if (maxChannel == 0f) 0f else (delta / maxChannel) * 100f
    val value = maxChannel * 100f
    return Triple(hue, saturation, value)
}

private fun colorCardHslFromRgb(
    red: Int,
    green: Int,
    blue: Int
): Triple<Float, Float, Float> {
    val rf = red / 255f
    val gf = green / 255f
    val bf = blue / 255f
    val maxChannel = max(rf, max(gf, bf))
    val minChannel = min(rf, min(gf, bf))
    val delta = maxChannel - minChannel
    val lightness = (maxChannel + minChannel) / 2f
    val hue = when {
        delta == 0f -> 0f
        maxChannel == rf -> (((gf - bf) / delta) % 6f) * 60f
        maxChannel == gf -> (((bf - rf) / delta) + 2f) * 60f
        else -> (((rf - gf) / delta) + 4f) * 60f
    }.let { if (it < 0f) it + 360f else it }
    val saturation = if (delta == 0f) {
        0f
    } else {
        (delta / (1f - kotlin.math.abs(2f * lightness - 1f))) * 100f
    }
    return Triple(hue, saturation, lightness * 100f)
}

private fun colorCardLabFromRgb(
    red: Int,
    green: Int,
    blue: Int
): Triple<Double, Double, Double> {
    val rf = colorCardLinearizeSrgb(red / 255.0)
    val gf = colorCardLinearizeSrgb(green / 255.0)
    val bf = colorCardLinearizeSrgb(blue / 255.0)

    val x = (rf * 0.4124564) + (gf * 0.3575761) + (bf * 0.1804375)
    val y = (rf * 0.2126729) + (gf * 0.7151522) + (bf * 0.0721750)
    val z = (rf * 0.0193339) + (gf * 0.1191920) + (bf * 0.9503041)

    val xPivot = colorCardLabPivot(x / 0.95047)
    val yPivot = colorCardLabPivot(y / 1.00000)
    val zPivot = colorCardLabPivot(z / 1.08883)

    val l = (116.0 * yPivot) - 16.0
    val a = 500.0 * (xPivot - yPivot)
    val b = 200.0 * (yPivot - zPivot)
    return Triple(l, a, b)
}

private fun colorCardLinearizeSrgb(channel: Double): Double {
    return if (channel <= 0.04045) {
        channel / 12.92
    } else {
        Math.pow((channel + 0.055) / 1.055, 2.4)
    }
}

private fun colorCardLabPivot(value: Double): Double {
    return if (value > 0.008856) {
        Math.cbrt(value)
    } else {
        (7.787 * value) + (16.0 / 116.0)
    }
}

private fun colorCardDeltaE00(
    lab1: Triple<Double, Double, Double>,
    lab2: Triple<Double, Double, Double>
): Double {
    val (l1, a1, b1) = lab1
    val (l2, a2, b2) = lab2
    val avgL = (l1 + l2) / 2.0
    val c1 = sqrt((a1 * a1) + (b1 * b1))
    val c2 = sqrt((a2 * a2) + (b2 * b2))
    val avgC = (c1 + c2) / 2.0
    val avgCPow7 = avgC.pow(7.0)
    val g = 0.5 * (1.0 - sqrt(avgCPow7 / (avgCPow7 + 25.0.pow(7.0))))
    val a1Prime = (1.0 + g) * a1
    val a2Prime = (1.0 + g) * a2
    val c1Prime = sqrt((a1Prime * a1Prime) + (b1 * b1))
    val c2Prime = sqrt((a2Prime * a2Prime) + (b2 * b2))
    val avgCPrime = (c1Prime + c2Prime) / 2.0
    val h1Prime = colorCardLabHueAngle(b1, a1Prime)
    val h2Prime = colorCardLabHueAngle(b2, a2Prime)
    val deltaLPrime = l2 - l1
    val deltaCPrime = c2Prime - c1Prime
    val deltaHAngle = when {
        c1Prime == 0.0 || c2Prime == 0.0 -> 0.0
        h2Prime - h1Prime > 180.0 -> h2Prime - h1Prime - 360.0
        h2Prime - h1Prime < -180.0 -> h2Prime - h1Prime + 360.0
        else -> h2Prime - h1Prime
    }
    val deltaHPrime =
        2.0 * sqrt(c1Prime * c2Prime) * sin(Math.toRadians(deltaHAngle / 2.0))
    val avgHPrime = when {
        c1Prime == 0.0 || c2Prime == 0.0 -> h1Prime + h2Prime
        abs(h1Prime - h2Prime) > 180.0 && h1Prime + h2Prime < 360.0 -> {
            (h1Prime + h2Prime + 360.0) / 2.0
        }
        abs(h1Prime - h2Prime) > 180.0 -> {
            (h1Prime + h2Prime - 360.0) / 2.0
        }
        else -> (h1Prime + h2Prime) / 2.0
    }
    val t = 1.0 -
        (0.17 * cos(Math.toRadians(avgHPrime - 30.0))) +
        (0.24 * cos(Math.toRadians(2.0 * avgHPrime))) +
        (0.32 * cos(Math.toRadians((3.0 * avgHPrime) + 6.0))) -
        (0.20 * cos(Math.toRadians((4.0 * avgHPrime) - 63.0)))
    val avgLDelta = (avgL - 50.0).pow(2.0)
    val sL = 1.0 + ((0.015 * avgLDelta) / sqrt(20.0 + avgLDelta))
    val sC = 1.0 + (0.045 * avgCPrime)
    val sH = 1.0 + (0.015 * avgCPrime * t)
    val deltaTheta = 30.0 * exp(-((avgHPrime - 275.0) / 25.0).pow(2.0))
    val rC = 2.0 * sqrt(avgCPrime.pow(7.0) / (avgCPrime.pow(7.0) + 25.0.pow(7.0)))
    val rT = -sin(Math.toRadians(2.0 * deltaTheta)) * rC
    val lightnessTerm = deltaLPrime / sL
    val chromaTerm = deltaCPrime / sC
    val hueTerm = deltaHPrime / sH
    return sqrt(
        (lightnessTerm * lightnessTerm) +
            (chromaTerm * chromaTerm) +
            (hueTerm * hueTerm) +
            (rT * chromaTerm * hueTerm)
    )
}

private fun colorCardLabHueAngle(b: Double, a: Double): Double {
    val angle = Math.toDegrees(atan2(b, a))
    return if (angle >= 0.0) angle else angle + 360.0
}

private fun colorCardRelativeLuminance(colorInt: Int): Double {
    val r = colorCardLinearizeSrgb(AndroidColor.red(colorInt) / 255.0)
    val g = colorCardLinearizeSrgb(AndroidColor.green(colorInt) / 255.0)
    val b = colorCardLinearizeSrgb(AndroidColor.blue(colorInt) / 255.0)
    return (0.2126 * r) + (0.7152 * g) + (0.0722 * b)
}

private fun colorCardContrastRatio(
    foregroundColorInt: Int,
    backgroundColorInt: Int
): Double {
    val foregroundLuminance = colorCardRelativeLuminance(foregroundColorInt)
    val backgroundLuminance = colorCardRelativeLuminance(backgroundColorInt)
    val lighter = max(foregroundLuminance, backgroundLuminance)
    val darker = min(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun colorCardWcagLabelSummary(contrastRatio: Double): String {
    val normal = when {
        contrastRatio >= 7.0 -> "\u6b63\u6587 AAA"
        contrastRatio >= 4.5 -> "\u6b63\u6587 AA"
        else -> "\u6b63\u6587 Fail"
    }
    val large = when {
        contrastRatio >= 4.5 -> "\u5927\u5b57 AAA"
        contrastRatio >= 3.0 -> "\u5927\u5b57 AA"
        else -> "\u5927\u5b57 Fail"
    }
    return "$normal / $large"
}

internal fun colorCardContentColor(backgroundColorInt: Int): Color {
    val background = Color(backgroundColorInt)
    return if (background.luminance() > 0.58f) {
        Color.Black
    } else {
        Color.White
    }
}

private suspend fun decodeHomeCardColorPickerBitmap(
    context: android.content.Context,
    uri: Uri,
    maxSide: Int
): Bitmap? {
    return withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(maxSide)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        val drawable = (result as? SuccessResult)?.drawable ?: return@withContext null
        val sourceWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxSide
        val sourceHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxSide
        val scale = max(sourceWidth, sourceHeight).toFloat() / maxSide.toFloat()
        val width = if (scale > 1f) {
            (sourceWidth / scale).roundToInt()
        } else {
            sourceWidth
        }.coerceAtLeast(1)
        val height = if (scale > 1f) {
            (sourceHeight / scale).roundToInt()
        } else {
            sourceHeight
        }.coerceAtLeast(1)
        drawable.toBitmap(
            width = width,
            height = height,
            config = Bitmap.Config.ARGB_8888
        )
    }
}

private fun buildHomeCardColorPickerPlaceholderColors(count: Int): List<Int> {
    val safeCount = count.coerceIn(
        HOME_CARD_COLOR_PICKER_MIN_COUNT,
        HOME_CARD_COLOR_PICKER_MAX_COUNT
    )
    return List(safeCount) { index ->
        HOME_CARD_COLOR_PICKER_PLACEHOLDERS[index % HOME_CARD_COLOR_PICKER_PLACEHOLDERS.size]
    }
}

private fun generateHomeCardColorPickerPoints(
    seed: Long,
    count: Int
): List<HomeCardColorPickerPoint> {
    val random = Random(seed)
    val generated = mutableListOf<HomeCardColorPickerPoint>()
    var requiredDistance = 0.22f
    repeat(count.coerceIn(HOME_CARD_COLOR_PICKER_MIN_COUNT, HOME_CARD_COLOR_PICKER_MAX_COUNT)) {
        var accepted: HomeCardColorPickerPoint? = null
        var attempts = 0
        while (attempts < 48 && accepted == null) {
            val candidate = HomeCardColorPickerPoint(
                x = random.nextFloat() * 0.70f + 0.15f,
                y = random.nextFloat() * 0.68f + 0.14f
            )
            if (generated.none { distanceSquared(it, candidate) < requiredDistance * requiredDistance }) {
                accepted = candidate
            }
            attempts += 1
        }
        if (accepted == null) {
            requiredDistance *= 0.84f
            accepted = HomeCardColorPickerPoint(
                x = random.nextFloat() * 0.72f + 0.14f,
                y = random.nextFloat() * 0.70f + 0.13f
            )
        }
        generated += accepted
    }
    return generated
}

private fun generateHomeCardColorPickerPoint(
    seed: Long,
    existing: List<HomeCardColorPickerPoint>
): HomeCardColorPickerPoint {
    val random = Random(seed)
    var requiredDistance = when {
        existing.size >= 8 -> 0.12f
        existing.size >= 6 -> 0.15f
        existing.size >= 4 -> 0.18f
        else -> 0.22f
    }
    repeat(4) {
        var attempts = 0
        while (attempts < 48) {
            val candidate = HomeCardColorPickerPoint(
                x = random.nextFloat() * 0.70f + 0.15f,
                y = random.nextFloat() * 0.68f + 0.14f
            )
            if (existing.none { distanceSquared(it, candidate) < requiredDistance * requiredDistance }) {
                return candidate
            }
            attempts += 1
        }
        requiredDistance *= 0.82f
    }
    return HomeCardColorPickerPoint(
        x = random.nextFloat() * 0.72f + 0.14f,
        y = random.nextFloat() * 0.70f + 0.13f
    )
}

private fun sampleHomeCardColor(
    bitmap: Bitmap,
    point: HomeCardColorPickerPoint,
    preciseMode: Boolean
): Int {
    val safeWidth = bitmap.width.coerceAtLeast(1)
    val safeHeight = bitmap.height.coerceAtLeast(1)
    val centerX = (point.x * (safeWidth - 1)).roundToInt().coerceIn(0, safeWidth - 1)
    val centerY = (point.y * (safeHeight - 1)).roundToInt().coerceIn(0, safeHeight - 1)
    if (preciseMode) {
        return bitmap.getPixel(centerX, centerY)
    }
    val radius = min(max(2, min(safeWidth, safeHeight) / 42), 10)

    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0L
    for (y in (centerY - radius)..(centerY + radius)) {
        for (x in (centerX - radius)..(centerX + radius)) {
            if (x !in 0 until safeWidth || y !in 0 until safeHeight) continue
            val pixel = bitmap.getPixel(x, y)
            red += AndroidColor.red(pixel)
            green += AndroidColor.green(pixel)
            blue += AndroidColor.blue(pixel)
            count += 1L
        }
    }
    if (count == 0L) {
        return bitmap.getPixel(centerX, centerY)
    }
    return AndroidColor.rgb(
        (red / count).toInt().coerceIn(0, 255),
        (green / count).toInt().coerceIn(0, 255),
        (blue / count).toInt().coerceIn(0, 255)
    )
}

private fun distanceSquared(
    first: HomeCardColorPickerPoint,
    second: HomeCardColorPickerPoint
): Float {
    val dx = first.x - second.x
    val dy = first.y - second.y
    return (dx * dx) + (dy * dy)
}

private fun Modifier.offsetForPickerPoint(
    point: HomeCardColorPickerPoint,
    overlaySize: IntSize,
    handleSizePx: Int
): Modifier {
    return this.then(
        Modifier.offset {
            IntOffset(
                x = ((point.x * overlaySize.width) - (handleSizePx / 2f)).roundToInt(),
                y = ((point.y * overlaySize.height) - (handleSizePx / 2f)).roundToInt()
            )
        }
    )
}

@Composable
internal fun HomeCardColorPickerPreciseModeRow(
    isPreciseMode: Boolean,
    onPreciseModeChange: (Boolean) -> Unit,
    primaryText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "精准模式",
                color = primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isPreciseMode) {
                    "单点吸管采样，适合取边缘和细节。"
                } else {
                    "区域均值更稳，适合提炼代表色。"
                },
                color = secondaryText,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isPreciseMode,
            onCheckedChange = onPreciseModeChange
        )
    }
}
