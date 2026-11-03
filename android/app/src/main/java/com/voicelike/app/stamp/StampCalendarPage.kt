package com.voicelike.app.stamp

import com.voicelike.app.liquidglass.LocalReferenceLiquidBottomTabAccentLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.CompositionLocalProvider
import com.voicelike.app.liquidglass.rememberLiquidGlassBackdrop
import com.voicelike.app.liquidglass.liquidGlassSurfaceColor
import com.voicelike.app.liquidglass.liquidGlassControl
import com.voicelike.app.liquidglass.ReferenceLiquidBottomTabs
import com.voicelike.app.liquidglass.ReferenceLiquidBottomTab
import com.voicelike.app.liquidglass.LocalLiquidGlassBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.Backdrop
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.drawable.toBitmap
import android.net.Uri
import android.widget.Toast
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.voicelike.app.AppLanguage
import com.voicelike.app.AppRuntimeLogStore
import com.voicelike.app.LocalHapticsEnabled
import com.voicelike.app.LocalAppLanguage
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.PreferencesManager
import com.voicelike.app.R
import com.voicelike.app.UnifiedSubpageTopBar
import com.voicelike.app.campfireSupporters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import java.text.DateFormatSymbols
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random
import androidx.compose.ui.zIndex

private enum class StampViewMode { Calendar, Timeline, Wall }

private val StampTimelineHourglassIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "StampTimelineHourglassIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f)
            curveTo(15.866f, 12f, 19f, 8.866f, 19f, 5f)
            horizontalLineTo(5f)
            curveTo(5f, 8.866f, 8.134f, 12f, 12f, 12f)
            close()
            moveTo(12f, 12f)
            curveTo(15.866f, 12f, 19f, 15.134f, 19f, 19f)
            horizontalLineTo(5f)
            curveTo(5f, 15.134f, 8.134f, 12f, 12f, 12f)
            close()
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 2f)
            horizontalLineTo(19f)
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 22f)
            horizontalLineTo(19f)
        }
    }.build()
}

private const val STAMP_STARTUP_MINE_PROFILE_PREFS_NAME = "startup_mine_profile_prefs"
private const val STAMP_STARTUP_MINE_USER_DISPLAY_NAME_KEY = "user_display_name"
private const val STAMP_STARTUP_MINE_DEFAULT_USER_NAME = "\u795E\u79D8\u7528\u6237"
private const val STAMP_CAMPFIRE_BADGE_ASSET =
    "file:///android_asset/campfire_supporters/supporter_default.webp"

internal data class StampCalendarCopy(
    val pageTitle: String,
    val camera: String,
    val tabCalendar: String,
    val tabTimeline: String = "Timeline",
    val tabWall: String,
    val monthWallTitle: String,
    val allFilterLabel: String,
    val emptyHint: String,
    val wallTitle: String,
    val emojiPickerTitle: String,
    val clearEmoji: String,
    val detailTitle: String,
    val notePlaceholder: String,
    val deleteStamp: String = "Delete stamp",
    val deleteConfirmTitle: String = "Delete this stamp?",
    val deleteConfirmBody: String = "This will remove it from the calendar and wall.",
    val deleteConfirmAction: String = "Delete",
    val deleteCancelAction: String = "Cancel",
    val thanksTitle: String,
    val thanksBody: String,
    val thanksConfirm: String,
    val dateChangeTitle: String = "Change date",
    val dateChangeConfirm: String = "Move",
    val dateChangeCancel: String = "Cancel",
    val weekLabels: List<String>
)

private data class StampSkinPanelCopy(
    val title: String,
    val originalLabel: String,
    val whiteBorderLabel: String,
    val editorialLabel: String,
    val cutoutLabel: String,
    val effectLabel: String,
    val effectNoneLabel: String,
    val effectCartoonLabel: String,
    val effectInkLabel: String,
    val effectTextureLabel: String,
    val effectQuantizeLabel: String,
    val effectHalftoneLabel: String,
    val effectMisprintLabel: String,
    val backgroundColorLabel: String,
    val numberFieldLabel: String,
    val titleFieldLabel: String,
    val subtitleFieldLabel: String,
    val numberPlaceholder: String,
    val titlePlaceholder: String,
    val subtitlePlaceholder: String,
    val confirmLabel: String,
    val cancelLabel: String,
    val campfireGateMessage: String,
    val campfireGateLearnLabel: String
)

internal data class StampDisplayItem(
    val day: Int,
    val dayKey: String,
    val uri: String,
    val emoji: String? = null,
    val note: String? = null,
    val wallRow: Int? = null
)

internal data class StampDetailState(
    val item: StampDisplayItem,
    val startX: Dp,
    val startY: Dp,
    val startWidth: Dp,
    val aspectRatio: Float,
    val startRotation: Float,
    val shadowElevation: Dp,
    val sourceCardPadding: Dp = 12.dp,
    val sourceShadowStrength: Float = 0.95f,
    val enableStackPreview: Boolean = false,
    val useBottomDockTransition: Boolean = false,
    val openedFromCalendarDay: Boolean = false,
    val sharedTransitionGroup: String? = null,
    val dayAllScrollOffset: Int = 0,
    val animateDayAllEntrance: Boolean = false
)

private data class StampWallItemSpec(
    val item: StampDisplayItem,
    val column: Int,
    val row: Int,
    val aspectRatio: Float,
    val widthFraction: Float,
    val xJitterFraction: Float,
    val yJitter: Dp,
    val rotation: Float
)

private data class StampDayAllGridPlacement(
    val uri: String,
    val aspectRatio: Float,
    val column: Int,
    val row: Int,
    val x: Dp,
    val y: Dp,
    val width: Dp
)

private data class PendingStampInsertion(
    val uri: String,
    val dayKey: String
)

private data class StampWheelOption(
    val value: Int,
    val label: String
)

private const val STAMP_CARD_ASPECT = 0.76f
private const val STAMP_SOURCE_CONTEXT_FALLBACK_MAX_SIDE = 4096
private val STAMP_WALL_CONTENT_BOTTOM_EXTRA_SPACE = 118.dp
private val STAMP_DETAIL_CARD_PADDING = 12.dp
private val STAMP_DIALOG_ACTION_BUTTON_HEIGHT = 48.dp
private val STAMP_DATE_PICKER_EXPANDED_HEIGHT = 468.dp
private val STAMP_DATE_PICKER_MIN_HEIGHT = 392.dp
private val STAMP_SKIN_PICKER_EXPANDED_HEIGHT_EDITORIAL = 580.dp
private val STAMP_SKIN_PICKER_EXPANDED_HEIGHT_CUSTOM = 546.dp
private val STAMP_SKIN_PICKER_EXPANDED_HEIGHT_DEFAULT = 504.dp
private val STAMP_SKIN_PICKER_MIN_HEIGHT = 400.dp
private val STAMP_DETAIL_PICKER_TRIGGER_WIDTH = 64.dp
private val STAMP_DETAIL_PICKER_TRIGGER_HEIGHT = 34.dp
private val STAMP_DETAIL_PICKER_ROW_BOTTOM_PADDING = 14.dp
private const val STAMP_SHARED_TRANSITION_LOG_TAG = "StampSharedTransition"
private const val STAMP_SKIN_PREVIEW_MEMORY_CACHE_MAX_ENTRIES = 48
private const val STAMP_WALL_PAGE_SIZE = 60
private val STAMP_WALL_LOAD_MORE_THRESHOLD = 420.dp
private val STAMP_SOURCE_APPEAR_EASING = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val STAMP_SOURCE_SCALE_EASING = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
private const val STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS = 430
private val STAMP_DETAIL_SHARED_TRANSITION_EASING = CubicBezierEasing(0.22f, 0.86f, 0.18f, 1f)
private val STAMP_DETAIL_SHARED_BOUNDS_TRANSFORM = BoundsTransform { _, _ ->
    tween(
        durationMillis = STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS,
        easing = STAMP_DETAIL_SHARED_TRANSITION_EASING
    )
}
private val STAMP_EMOJI_PRESETS = listOf(
    "\uD83C\uDFAE",
    "\uD83C\uDFAC",
    "\uD83C\uDF38",
    "\uD83C\uDF3F",
    "\uD83D\uDC31",
    "\uD83D\uDCDA",
    "\uD83D\uDCF7",
    "\uD83C\uDF0D",
    "\uD83C\uDF5C",
    "\uD83C\uDFB5",
    "\uD83C\uDFC3",
    "\uD83D\uDCBC",
    "\uD83D\uDCBB",
    "\u2708\uFE0F",
    "\u2615",
    "\uD83C\uDF27\uFE0F",
    "\uD83D\uDCA1",
    "\uD83C\uDF19",
    "\uD83C\uDFA8",
    "\uD83C\uDF06"
)
private val STAMP_EMOJI_MOODS = listOf(
    "\uD83D\uDE04",
    "\uD83E\uDD70",
    "\uD83D\uDE0C",
    "\uD83E\uDD2F",
    "\uD83D\uDE22"
)
private val stampAspectRatioMemoryCache = linkedMapOf<String, Float>()
private val stampSkinPreviewMemoryCache = linkedMapOf<String, Bitmap>()
private val stampCardSkinRenderDispatcher = Dispatchers.Default.limitedParallelism(2)

@Composable
internal fun stampAdaptiveColor(light: Color, dark: Color): Color {
    return if (isSystemInDarkTheme()) dark else light
}

private fun stampSharedElementKey(group: String, uri: String): String {
    return "stamp-shared:$group:$uri"
}

private fun dayGridSharedTransitionGroup(dayKey: String): String {
    return "stamp-day-grid:$dayKey"
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.rememberStampSharedElementModifier(
    group: String?,
    uri: String,
    visible: Boolean
): Modifier {
    if (group.isNullOrBlank()) return Modifier
    val sharedContentState = rememberSharedContentState(stampSharedElementKey(group, uri))
    return Modifier.sharedElementWithCallerManagedVisibility(
        sharedContentState = sharedContentState,
        visible = visible,
        boundsTransform = STAMP_DETAIL_SHARED_BOUNDS_TRANSFORM
    )
}
@Composable
fun StampCalendarPage(
    onBack: () -> Unit,
    onCampfireRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val appStrings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val copy = remember(appLanguage) {
        stampCalendarCopy(appLanguage).copy(
            weekLabels = localizedWeekLabels(appLanguage)
        )
    }
    val initialCalendarCollapsed = remember(context) { loadStampCalendarCollapsedPreference(context) }
    val initialWallMetaEnabled = remember(context) { loadStampCalendarWallMetaPreference(context) }

    var showCamera by rememberSaveable { mutableStateOf(false) }
    var currentMonthKey by rememberSaveable { mutableStateOf(stampCalendarMonthKey(Calendar.getInstance())) }
    var refreshNonce by remember { mutableIntStateOf(0) }
    var viewMode by rememberSaveable { mutableStateOf(StampViewMode.Calendar) }
    var calendarCollapsed by rememberSaveable { mutableStateOf(initialCalendarCollapsed) }
    var showWallMeta by rememberSaveable { mutableStateOf(initialWallMetaEnabled) }
    val calendarContentScrollState = rememberScrollState()
    var detailState by remember(currentMonthKey, refreshNonce) { mutableStateOf<StampDetailState?>(null) }
    var detailCloseSignal by remember { mutableIntStateOf(0) }
    var selectedEmojiFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showThanksDialog by rememberSaveable { mutableStateOf(false) }
    var renderThanksDialog by remember { mutableStateOf(false) }
    var thanksDialogOpenSignal by remember { mutableIntStateOf(0) }
    var optimisticStampBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    var sharedTransitionPreviewBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    var pendingStampInsertion by remember { mutableStateOf<PendingStampInsertion?>(null) }
    var stableUriSeedByUri by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val stampBoundsByUri = remember { mutableStateMapOf<String, Rect>() }
    val stampRotationByUri = remember { mutableStateMapOf<String, Float>() }
    val dayAllBoundsByUri = remember { mutableStateMapOf<String, Rect>() }
    val dayAllRotationByUri = remember { mutableStateMapOf<String, Float>() }
    var cameraButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var thanksTriggerBounds by remember { mutableStateOf<Rect?>(null) }
    var calendarRootSize by remember { mutableStateOf(IntSize.Zero) }
    var detailSharedTargetVisible by remember { mutableStateOf(false) }
    var detailSharedSourceCardHidden by remember { mutableStateOf(false) }
    var rawMonthEntries by remember { mutableStateOf<Map<Int, StampCalendarDayEntry>>(emptyMap()) }
    var rawAllEntries by remember { mutableStateOf<List<StampCalendarDayEntry>>(emptyList()) }
    var allEntriesLoaded by remember { mutableStateOf(false) }
    var allEntriesLoading by remember { mutableStateOf(false) }
    var allEntriesPrefetchReady by remember { mutableStateOf(false) }
    var monthWallRenderReady by remember { mutableStateOf(false) }
    var monthWallIdleWarmupReady by remember { mutableStateOf(false) }
    val rootScope = rememberCoroutineScope()
    val storeVersion by observeStampCalendarDataVersion().collectAsState()
    val cameraLaunchProgress by animateFloatAsState(
        targetValue = if (showCamera) 1f else 0f,
        animationSpec = tween(durationMillis = 860, easing = FastOutSlowInEasing),
        label = "stamp_camera_launch_progress"
    )

    BackHandler(enabled = detailState != null) {
        detailCloseSignal += 1
    }
    BackHandler(enabled = showCamera) {
        showCamera = false
    }
    BackHandler(enabled = showThanksDialog) {
        showThanksDialog = false
    }

    val handleStampSaveStarted: (Bitmap) -> String? = { bitmap ->
        val pendingUri = "stamp-pending://${System.currentTimeMillis()}_${bitmap.hashCode()}"
        val pendingDayKey = stampCalendarTodayKey()
        cacheStampAspectRatio(pendingUri, bitmapAspectRatio(bitmap))
        optimisticStampBitmaps = optimisticStampBitmaps + (pendingUri to bitmap)
        stableUriSeedByUri = stableUriSeedByUri + (pendingUri to pendingUri)
        pendingStampInsertion = PendingStampInsertion(
            uri = pendingUri,
            dayKey = pendingDayKey
        )
        showCamera = false
        pendingUri
    }
    val handleStampSaveFailed: (String?) -> Unit = { pendingSaveKey ->
        val pendingUri = pendingSaveKey?.takeIf { it.isNotBlank() }
            ?: pendingStampInsertion?.uri
        if (!pendingUri.isNullOrBlank()) {
            if (pendingStampInsertion?.uri == pendingUri) {
                pendingStampInsertion = null
            }
            optimisticStampBitmaps = optimisticStampBitmaps - pendingUri
            stableUriSeedByUri = stableUriSeedByUri - pendingUri
        }
    }
    val handleStampSaved: (Uri, Bitmap, String?) -> Unit = { uri, bitmap, pendingSaveKey ->
        val savedUriString = uri.toString()
        val savePendingUri = pendingSaveKey?.takeIf { it.isNotBlank() }
        val activePending = pendingStampInsertion
        val activePendingUri = activePending?.uri
        val detailHoldingPending = !savePendingUri.isNullOrBlank() && detailState?.item?.uri == savePendingUri
        val shouldReplacePendingKey = !savePendingUri.isNullOrBlank() &&
            activePendingUri == savePendingUri &&
            savePendingUri != savedUriString &&
            !detailHoldingPending
        cacheStampAspectRatio(savedUriString, bitmapAspectRatio(bitmap))
        optimisticStampBitmaps = (
            if (shouldReplacePendingKey) {
                optimisticStampBitmaps - savePendingUri
            } else {
                optimisticStampBitmaps
            }
            ) + (savedUriString to bitmap)
        val pendingSeed = savePendingUri?.let { stableUriSeedByUri[it] }
        stableUriSeedByUri = (
            if (shouldReplacePendingKey) {
                stableUriSeedByUri - savePendingUri
            } else {
                stableUriSeedByUri
            }
            ) + (savedUriString to (pendingSeed ?: savePendingUri ?: savedUriString))
        if (!savePendingUri.isNullOrBlank() && activePendingUri == savePendingUri) {
            pendingStampInsertion = PendingStampInsertion(
                uri = savedUriString,
                dayKey = activePending.dayKey
            )
        }
        if (detailHoldingPending) {
            detailState = detailState?.let { current ->
                current.copy(item = current.item.copy(uri = savedUriString))
            }
        }
    }

    LaunchedEffect(context, currentMonthKey, refreshNonce, storeVersion) {
        rawMonthEntries = withContext(Dispatchers.IO) {
            loadStampCalendarMonthEntries(context, currentMonthKey)
        }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val entries = loadAllStampCalendarEntries(context)
                val missingFileCount = entries.sumOf { entry ->
                    entry.uris.count { uri ->
                        val path = runCatching { Uri.parse(uri).path }.getOrNull()
                        path == null || !File(path).exists()
                    }
                }
                val message = "calendar scan entries=${entries.size} missingFiles=$missingFileCount"
                if (missingFileCount > 0) {
                    AppRuntimeLogStore.logError(context, "StampDiag", message)
                } else {
                    AppRuntimeLogStore.logInfo(context, "StampDiag", message)
                }
            }
        }
    }
    val monthEntries = remember(rawMonthEntries, pendingStampInsertion, currentMonthKey) {
        val baseEntries = rawMonthEntries.mapValues { (day, entry) ->
            entry.copy(
                selectedUri = entry.selectedUri?.takeIf { it in entry.uris }
                    ?: resolveRandomStampUri(entry.dayKey, entry.uris, day)
            )
        }
        mergePendingStampIntoMonthEntries(
            entries = baseEntries,
            pendingStamp = pendingStampInsertion,
            monthKey = currentMonthKey
        )
    }
    val hasMonthWallContent = remember(monthEntries) {
        monthEntries.values.any { entry -> entry.uris.isNotEmpty() }
    }
    val monthWallActivationThresholdPx = with(LocalDensity.current) {
        420.dp.roundToPx()
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(420)
        allEntriesPrefetchReady = true
    }
    LaunchedEffect(viewMode, hasMonthWallContent, calendarCollapsed) {
        if (
            viewMode != StampViewMode.Calendar ||
            monthWallRenderReady ||
            !hasMonthWallContent
        ) {
            return@LaunchedEffect
        }
        monthWallIdleWarmupReady = false
        withFrameNanos { }
        delay(if (calendarCollapsed) 420 else 920)
        if (viewMode == StampViewMode.Calendar && !monthWallRenderReady) {
            monthWallIdleWarmupReady = true
        }
    }
    LaunchedEffect(
        viewMode,
        hasMonthWallContent,
        calendarCollapsed,
        calendarContentScrollState.value,
        monthWallActivationThresholdPx,
        monthWallIdleWarmupReady
    ) {
        if (
            viewMode != StampViewMode.Calendar ||
            monthWallRenderReady ||
            !hasMonthWallContent
        ) {
            return@LaunchedEffect
        }
        val shouldActivateMonthWall =
            calendarCollapsed ||
                monthWallIdleWarmupReady ||
                calendarContentScrollState.value >= monthWallActivationThresholdPx
        if (!shouldActivateMonthWall) return@LaunchedEffect
        withFrameNanos { }
        monthWallRenderReady = true
    }
    val shouldLoadAllEntries = viewMode != StampViewMode.Calendar || allEntriesPrefetchReady
    LaunchedEffect(context, refreshNonce, storeVersion, shouldLoadAllEntries) {
        if (!shouldLoadAllEntries) return@LaunchedEffect
        allEntriesLoading = true
        try {
            rawAllEntries = withContext(Dispatchers.IO) {
                loadAllStampCalendarEntries(context)
            }
            allEntriesLoaded = true
        } finally {
            allEntriesLoading = false
        }
    }
    val allEntries = remember(rawAllEntries, pendingStampInsertion) {
        mergePendingStampIntoAllEntries(rawAllEntries, pendingStampInsertion)
    }
    val stampSkinByUri = remember(viewMode, monthEntries, allEntries) {
        buildMap {
            monthEntries.values.forEach { entry ->
                entry.skinByUri.forEach { (uri, skin) ->
                    put(uri, skin.normalized())
                }
            }
            if (viewMode != StampViewMode.Calendar) {
                allEntries.forEach { entry ->
                    entry.skinByUri.forEach { (uri, skin) ->
                        put(uri, skin.normalized())
                    }
                }
            }
        }
    }
    val allEmojiGroupsInWall = remember(viewMode, allEntries) {
        if (viewMode != StampViewMode.Wall) {
            emptyList()
        } else {
            allEntries
                .flatMap { it.emojiByUri.values }
                .distinct()
        }
    }
    val monthWallEntries = remember(monthEntries) {
        sortStampCalendarEntriesDescending(monthEntries.values)
    }
    val showAllEntriesLoading = remember(viewMode, allEntriesLoaded, allEntriesLoading, allEntries) {
        (viewMode == StampViewMode.Timeline || viewMode == StampViewMode.Wall) &&
            (!allEntriesLoaded || allEntriesLoading) &&
            allEntries.isEmpty()
    }
    LaunchedEffect(viewMode, selectedEmojiFilter, allEmojiGroupsInWall) {
        if (
            viewMode == StampViewMode.Wall &&
            selectedEmojiFilter != null &&
            selectedEmojiFilter !in allEmojiGroupsInWall
        ) {
            selectedEmojiFilter = null
        }
    }
    LaunchedEffect(rawMonthEntries, rawAllEntries, pendingStampInsertion) {
        val pending = pendingStampInsertion ?: return@LaunchedEffect
        val inMonthStore = rawMonthEntries.values.any { pending.uri in it.uris }
        val inAllStore = rawAllEntries.any { pending.uri in it.uris }
        if (inMonthStore || inAllStore) {
            pendingStampInsertion = null
        }
    }
    LaunchedEffect(showThanksDialog) {
        if (!showThanksDialog) {
            delay(520)
            if (!showThanksDialog) {
                renderThanksDialog = false
            }
        }
    }
    LaunchedEffect(thanksDialogOpenSignal) {
        if (thanksDialogOpenSignal > 0) {
            renderThanksDialog = true
            delay(28)
            showThanksDialog = true
        }
    }
    LaunchedEffect(detailState == null) {
        if (detailState == null) {
            detailSharedTargetVisible = false
            detailSharedSourceCardHidden = false
        }
    }
    val onSharedTransitionPreviewBitmapResolved: (String, Bitmap) -> Unit = { uri, bitmap ->
        bitmap.takeUnless { it.isRecycled }?.let { resolvedBitmap ->
            sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps.let { current ->
                val existing = current[uri]
                if (existing === resolvedBitmap) {
                    current
                } else {
                    linkedMapOf<String, Bitmap>().apply {
                        current.forEach { (key, value) ->
                            if (key != uri && !value.isRecycled) {
                                put(key, value)
                            }
                        }
                        put(uri, resolvedBitmap)
                        while (size > 6) {
                            remove(entries.first().key)
                        }
                    }
                }
            }
        }
    }
    val openDetail: (StampDetailState) -> Unit = { nextState ->
        detailState = nextState
        val nextSharedGroup = nextState.sharedTransitionGroup
        val hasSharedGroup = !nextSharedGroup.isNullOrBlank()
        if (!hasSharedGroup) {
            detailSharedTargetVisible = false
            detailSharedSourceCardHidden = false
        } else {
            detailSharedTargetVisible = false
            detailSharedSourceCardHidden = false
            rootScope.launch {
                repeat(2) {
                    withFrameNanos { }
                }
                if (detailState == nextState) {
                    detailSharedSourceCardHidden = true
                    detailSharedTargetVisible = true
                }
            }
        }
    }
    val retargetDetail: (StampDetailState) -> Unit = { nextState ->
        val nextMonthKey = nextState.item.dayKey.substringBeforeLast("-")
        if (nextMonthKey.isNotBlank() && nextMonthKey != currentMonthKey) {
            currentMonthKey = nextMonthKey
        }
        detailSharedTargetVisible = false
        detailSharedSourceCardHidden = false
        detailState = nextState
    }
    val closeDetailOverlay: () -> Unit = {
        detailSharedTargetVisible = false
        detailSharedSourceCardHidden = false
        detailState = null
        dayAllBoundsByUri.clear()
        dayAllRotationByUri.clear()
    }

            SharedTransitionLayout(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        stampAdaptiveColor(
                            light = Color(0xFFF6F2E7),
                            dark = MaterialTheme.colorScheme.background
                        )
                    )
                    .onSizeChanged { calendarRootSize = it }
            ) {
    val sharedTransitionScope = this
    val dockGlassBackdrop = rememberLiquidGlassBackdrop()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .then(if (dockGlassBackdrop != null) Modifier.layerBackdrop(dockGlassBackdrop) else Modifier)
                    .paperGridBackground(
                        backgroundColor = stampAdaptiveColor(
                            light = Color(0xFFF6F2E7),
                            dark = MaterialTheme.colorScheme.background
                        ),
                        lineColor = stampAdaptiveColor(
                            light = Color(0xFFE1DAC9).copy(alpha = 0.44f),
                            dark = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        ),
                        spacing = 54.dp
                    )
            ) {
            UnifiedSubpageTopBar(
                    title = "",
                    onBack = onBack,
                    backContentDescription = appStrings.back,
                    backButtonContainerColor = stampAdaptiveColor(
                        light = Color(0xFFFAFAFA).copy(alpha = 0.34f),
                        dark = Color(0xFF121212).copy(alpha = 0.36f)
                    ),
                    backButtonIconColor = stampAdaptiveColor(
                        light = Color(0xFF1E1A16),
                        dark = MaterialTheme.colorScheme.onSurface
                    ),
                    titleContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = copy.pageTitle,
                                color = stampAdaptiveColor(
                                    light = Color(0xFF181512),
                                    dark = MaterialTheme.colorScheme.onBackground
                                ),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Image(
                                painter = painterResource(id = R.drawable.is_credit),
                                contentDescription = "Credit",
                                modifier = Modifier
                                    .height(18.dp)
                                    .onGloballyPositioned { coordinates ->
                                        thanksTriggerBounds = coordinates.boundsInRoot()
                                    }
                                    .noRippleClickable {
                                        renderThanksDialog = true
                                        thanksDialogOpenSignal += 1
                                    }
                            )
                        }
                    },
                    trailing = {
                        Surface(
                            shape = CircleShape,
                            color = stampAdaptiveColor(
                                light = Color.White.copy(alpha = 0.88f),
                                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .noRippleClickable {
                                    val next = !showWallMeta
                                    showWallMeta = next
                                    updateStampCalendarWallMetaPreference(context, next)
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(
                                        id = if (showWallMeta) {
                                            R.drawable.ic_stamp_wall_meta_on
                                        } else {
                                            R.drawable.ic_stamp_wall_meta_off
                                        }
                                    ),
                                    contentDescription = if (showWallMeta) "Hide wall notes" else "Show wall notes",
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                )
            if (viewMode == StampViewMode.Calendar) {
                StampMonthHeader(
                        monthLabel = formatMonthLabel(currentMonthKey),
                        isCalendarCollapsed = calendarCollapsed,
                        onPrevious = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentMonthKey = shiftMonthKey(currentMonthKey, -1)
                        },
                        onNext = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentMonthKey = shiftMonthKey(currentMonthKey, 1)
                        },
                        onToggleCalendarCollapse = {
                            val nextCollapsed = !calendarCollapsed
                            calendarCollapsed = nextCollapsed
                            updateStampCalendarCollapsedPreference(context, nextCollapsed)
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
            when (viewMode) {
                StampViewMode.Calendar -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(calendarContentScrollState)
                    ) {
                        if (!calendarCollapsed) {
                            StampCalendarStage(
                                sharedTransitionScope = sharedTransitionScope,
                                sharedTransitionGroup = "calendar-month-$currentMonthKey",
                                detailSharedTargetVisible = detailSharedTargetVisible,
                                detailSharedSourceCardHidden = detailSharedSourceCardHidden,
                                appLanguage = appLanguage,
                                monthKey = currentMonthKey,
                                entries = monthEntries,
                                copy = copy,
                                optimisticStampBitmaps = optimisticStampBitmaps,
                                sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps,
                                stableSeedByUri = stableUriSeedByUri,
                                stampSkinByUri = stampSkinByUri,
                                sourceBoundsByUri = stampBoundsByUri,
                                sourceRotationByUri = stampRotationByUri,
                                activeDetailUri = detailState?.item?.uri,
                                activeDetailSharedGroup = detailState?.sharedTransitionGroup,
                                parentScrollOffsetPx = calendarContentScrollState.value.toFloat(),
                                onStampBoundsChange = { uri, bounds, rotation ->
                                    if (uri.isNotBlank()) {
                                        stampBoundsByUri[uri] = bounds
                                        stampRotationByUri[uri] = rotation
                                    }
                                },
                                onSharedTransitionPreviewBitmapResolved = onSharedTransitionPreviewBitmapResolved,
                                onOpenDetail = openDetail
                            )
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        if (monthWallRenderReady || monthWallEntries.isEmpty()) {
                            StampWallStage(
                                sharedTransitionScope = sharedTransitionScope,
                                sharedTransitionGroup = "month-wall-$currentMonthKey",
                                detailSharedTargetVisible = detailSharedTargetVisible,
                                detailSharedSourceCardHidden = detailSharedSourceCardHidden,
                                entries = monthWallEntries,
                                copy = copy,
                                title = copy.monthWallTitle,
                                optimisticStampBitmaps = optimisticStampBitmaps,
                                sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps,
                                stableSeedByUri = stableUriSeedByUri,
                                stampSkinByUri = stampSkinByUri,
                                sourceBoundsByUri = stampBoundsByUri,
                                sourceRotationByUri = stampRotationByUri,
                                selectedEmoji = null,
                                showEmojiFilters = false,
                                enableScroll = false,
                                showWallMeta = showWallMeta,
                                parentScrollOffsetPx = calendarContentScrollState.value.toFloat(),
                                onEmojiFilterChange = {},
                                activeDetailUri = detailState?.item?.uri,
                                activeDetailSharedGroup = detailState?.sharedTransitionGroup,
                                onStampBoundsChange = { uri, bounds, rotation ->
                                    if (uri.isNotBlank()) {
                                        stampBoundsByUri[uri] = bounds
                                        stampRotationByUri[uri] = rotation
                                    }
                                },
                                onSharedTransitionPreviewBitmapResolved = onSharedTransitionPreviewBitmapResolved,
                                onOpenDetail = openDetail,
                                haptic = haptic,
                                hapticsAllowed = hapticsAllowed
                            )
                        } else {
                            StampStageLoading(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp)
                            )
                        }
                    }
                }

                StampViewMode.Timeline -> {
                    if (showAllEntriesLoading) {
                        StampStageLoading(
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        StampTimelineStage(
                            sharedTransitionScope = sharedTransitionScope,
                            sharedTransitionGroup = "timeline-all",
                            detailSharedTargetVisible = detailSharedTargetVisible,
                            detailSharedSourceCardHidden = detailSharedSourceCardHidden,
                            entries = allEntries,
                            copy = copy,
                            optimisticStampBitmaps = optimisticStampBitmaps,
                            sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps,
                            stableSeedByUri = stableUriSeedByUri,
                            stampSkinByUri = stampSkinByUri,
                            sourceBoundsByUri = stampBoundsByUri,
                            sourceRotationByUri = stampRotationByUri,
                            activeDetailUri = detailState?.item?.uri,
                            activeDetailSharedGroup = detailState?.sharedTransitionGroup,
                            onStampBoundsChange = { uri, bounds, rotation ->
                                if (uri.isNotBlank()) {
                                    stampBoundsByUri[uri] = bounds
                                    stampRotationByUri[uri] = rotation
                                }
                            },
                            onSharedTransitionPreviewBitmapResolved = onSharedTransitionPreviewBitmapResolved,
                            onOpenDetail = openDetail,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                StampViewMode.Wall -> {
                    if (showAllEntriesLoading) {
                        StampStageLoading(
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        StampWallStage(
                            sharedTransitionScope = sharedTransitionScope,
                            sharedTransitionGroup = "all-wall",
                            detailSharedTargetVisible = detailSharedTargetVisible,
                            detailSharedSourceCardHidden = detailSharedSourceCardHidden,
                            entries = allEntries,
                            copy = copy,
                            title = null,
                            optimisticStampBitmaps = optimisticStampBitmaps,
                            sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps,
                            stableSeedByUri = stableUriSeedByUri,
                            stampSkinByUri = stampSkinByUri,
                            sourceBoundsByUri = stampBoundsByUri,
                            sourceRotationByUri = stampRotationByUri,
                            selectedEmoji = selectedEmojiFilter,
                            showEmojiFilters = true,
                            enableScroll = true,
                            showWallMeta = showWallMeta,
                            parentScrollOffsetPx = 0f,
                            onEmojiFilterChange = { selectedEmojiFilter = it },
                            activeDetailUri = detailState?.item?.uri,
                            activeDetailSharedGroup = detailState?.sharedTransitionGroup,
                            onStampBoundsChange = { uri, bounds, rotation ->
                                if (uri.isNotBlank()) {
                                    stampBoundsByUri[uri] = bounds
                                    stampRotationByUri[uri] = rotation
                                }
                            },
                            onSharedTransitionPreviewBitmapResolved = onSharedTransitionPreviewBitmapResolved,
                            onOpenDetail = openDetail,
                            haptic = haptic,
                            hapticsAllowed = hapticsAllowed,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(152.dp)
        ) {
            CompositionLocalProvider(LocalLiquidGlassBackdrop provides dockGlassBackdrop) {
                StampBottomDock(
                        copy = copy,
                        selectedMode = viewMode,
                        onModeSelected = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewMode = it
                        },
                        onOpenCamera = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCamera = true
                        },
                        onCameraButtonBoundsChange = { bounds -> cameraButtonBounds = bounds },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
            }
        }

        StampFullScreenDetailPage(
            sharedTransitionScope = sharedTransitionScope,
            detailSharedTargetVisible = detailSharedTargetVisible,
            detailSharedSourceCardHidden = detailSharedSourceCardHidden,
            detailState = detailState,
            requestCloseSignal = detailCloseSignal,
            copy = copy,
            optimisticStampBitmaps = optimisticStampBitmaps,
            sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps,
            stableSeedByUri = stableUriSeedByUri,
            stampSkinByUri = stampSkinByUri,
            sourceBoundsByUri = stampBoundsByUri,
            sourceRotationByUri = stampRotationByUri,
            dayAllBoundsByUri = dayAllBoundsByUri,
            dayAllRotationByUri = dayAllRotationByUri,
            onDayAllStampBoundsChange = { uri, bounds, rotation ->
                if (uri.isNotBlank()) {
                    if (dayAllBoundsByUri[uri] != bounds) {
                        dayAllBoundsByUri[uri] = bounds
                    }
                    if (dayAllRotationByUri[uri] != rotation) {
                        dayAllRotationByUri[uri] = rotation
                    }
                }
            },
            haptic = haptic,
            hapticsAllowed = hapticsAllowed,
            onSharedTargetVisibilityChange = { detailSharedTargetVisible = it },
            onSharedSourceCardHiddenChange = { detailSharedSourceCardHidden = it },
            onSharedTransitionPreviewBitmapResolved = onSharedTransitionPreviewBitmapResolved,
            onClose = closeDetailOverlay,
            onOpenDetail = openDetail,
            onRetargetDetail = retargetDetail,
            onCampfireRequested = onCampfireRequested,
            onDelete = { item ->
                removeStampFromCalendarDay(context, item.dayKey, item.uri)
                val stillReferenced = loadAllStampCalendarEntries(context)
                    .any { entry -> item.uri in entry.uris }
                if (!stillReferenced) {
                    removeStampSourceContext(context, item.uri, deleteSourceAsset = true)
                }
                stampBoundsByUri.remove(item.uri)
                stampRotationByUri.remove(item.uri)
                dayAllBoundsByUri.remove(item.uri)
                dayAllRotationByUri.remove(item.uri)
                optimisticStampBitmaps = optimisticStampBitmaps - item.uri
                sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps - item.uri
                detailSharedTargetVisible = false
                detailSharedSourceCardHidden = false
                detailState = null
                refreshNonce += 1
            },
            modifier = Modifier.align(Alignment.Center)
        )

        if (renderThanksDialog) {
            ThanksDialogPanel(
                visible = showThanksDialog,
                avatarUri = "file:///android_asset/jy_avatar.jpg",
                title = thanksDialogTitle(appLanguage),
                body = copy.thanksBody,
                confirmLabel = "OK",
                anchorBounds = thanksTriggerBounds,
                onDismiss = { showThanksDialog = false },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            )
        }
        if (showCamera) {
            StampCameraPage(
                onBack = { showCamera = false },
                onStampSaveStarted = handleStampSaveStarted,
                onStampSaveFailed = handleStampSaveFailed,
                onStampSaved = handleStampSaved,
                entryProgress = cameraLaunchProgress,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val reveal = ((cameraLaunchProgress - 0.18f) / 0.82f).coerceIn(0f, 1f)
                        val easedReveal = FastOutSlowInEasing.transform(reveal)
                        alpha = easedReveal
                        scaleX = 1.04f - (0.04f * easedReveal)
                        scaleY = 1.04f - (0.04f * easedReveal)
                    }
                    .zIndex(20f)
            )
            if (cameraLaunchProgress < 0.999f) {
                StampCameraLaunchReveal(
                    progress = cameraLaunchProgress,
                    originBounds = cameraButtonBounds,
                    containerSize = calendarRootSize,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(21f)
                )
            }
    }
    }
}
}
}


@Composable
private fun StampMonthHeader(
    monthLabel: String,
    isCalendarCollapsed: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleCalendarCollapse: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthLabel,
            color = stampAdaptiveColor(
                light = Color(0xFF131313),
                dark = MaterialTheme.colorScheme.onBackground
            ),
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        MonthArrowButton(Icons.Outlined.ChevronLeft, onPrevious)
        Spacer(Modifier.width(8.dp))
        MonthArrowButton(Icons.Outlined.ChevronRight, onNext)
        Spacer(Modifier.width(8.dp))
        MonthArrowButton(
            icon = Icons.Outlined.ChevronRight,
            onClick = onToggleCalendarCollapse,
            iconRotationDegrees = if (isCalendarCollapsed) 90f else -90f
        )
    }
}

@Composable
private fun MonthArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    iconRotationDegrees: Float = 0f
) {
    Surface(
        shape = CircleShape,
        color = stampAdaptiveColor(
            light = Color.White.copy(alpha = 0.88f),
            dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
        ),
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .noRippleClickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = stampAdaptiveColor(
                    light = Color(0xFF2C2923),
                    dark = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.graphicsLayer { rotationZ = iconRotationDegrees }
            )
        }
    }
}

@Composable
private fun StampStageLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.4.dp
        )
    }
}

@Composable
private fun StampCameraLaunchReveal(
    progress: Float,
    originBounds: Rect?,
    containerSize: IntSize,
    modifier: Modifier = Modifier
) {
    if (containerSize.width <= 0 || containerSize.height <= 0 || progress <= 0f) return

    val density = LocalDensity.current
    val width = containerSize.width.toFloat()
    val height = containerSize.height.toFloat()
    val fallbackRadius = with(density) { 32.dp.toPx() }
    val center = originBounds?.center ?: Offset(
        x = width * 0.5f,
        y = height - with(density) { 74.dp.toPx() }
    )
    val startRadius = ((originBounds?.width ?: 0f) * 0.5f).coerceAtLeast(fallbackRadius)
    val maxRadius = maxOf(
        hypot(center.x, center.y),
        hypot(width - center.x, center.y),
        hypot(center.x, height - center.y),
        hypot(width - center.x, height - center.y)
    ) + with(density) { 24.dp.toPx() }
    val easedProgress = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
    val overlayAlpha = if (easedProgress < 0.78f) {
        1f
    } else {
        (1f - ((easedProgress - 0.78f) / 0.22f)).coerceIn(0f, 1f)
    }
    if (overlayAlpha <= 0f) return

    Canvas(
        modifier = modifier
    ) {
        drawCircle(
            color = Color.Black.copy(alpha = overlayAlpha),
            radius = lerpFloat(startRadius, maxRadius, easedProgress),
            center = center
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StampCalendarStage(
    sharedTransitionScope: SharedTransitionScope,
    sharedTransitionGroup: String,
    detailSharedTargetVisible: Boolean,
    detailSharedSourceCardHidden: Boolean,
    appLanguage: AppLanguage,
    monthKey: String,
    entries: Map<Int, StampCalendarDayEntry>,
    copy: StampCalendarCopy,
    optimisticStampBitmaps: Map<String, Bitmap>,
    sharedTransitionPreviewBitmaps: Map<String, Bitmap>,
    stableSeedByUri: Map<String, String>,
    stampSkinByUri: Map<String, StampCalendarStampSkin>,
    sourceBoundsByUri: Map<String, Rect>,
    sourceRotationByUri: Map<String, Float>,
    activeDetailUri: String?,
    activeDetailSharedGroup: String?,
    parentScrollOffsetPx: Float,
    onStampBoundsChange: (String, Rect, Float) -> Unit,
    onSharedTransitionPreviewBitmapResolved: (String, Bitmap) -> Unit,
    onOpenDetail: (StampDetailState) -> Unit
) {
    val localContext = LocalContext.current
    val calendar = remember(monthKey) { parseMonthCalendar(monthKey) }
    val daysInMonth = remember(calendar) { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val weekStartDay = remember(appLanguage) { resolveCalendarWeekStartDay(appLanguage) }
    val startOffset = remember(calendar, weekStartDay) {
        ((calendar.get(Calendar.DAY_OF_WEEK) - weekStartDay + 7) % 7)
    }
    val density = LocalDensity.current
    var stageCardVisualsReady by remember(monthKey) { mutableStateOf(false) }
    LaunchedEffect(monthKey) {
        stageCardVisualsReady = false
        withFrameNanos { }
        delay(260)
        stageCardVisualsReady = true
    }
    val selectedUris = remember(entries) { entries.values.mapNotNull { it.selectedUri }.distinct() }
    val initialAspectMap = remember(selectedUris, optimisticStampBitmaps) {
        selectedUris.mapNotNull { uri ->
            resolveKnownStampAspectRatio(uri, optimisticStampBitmaps[uri])?.let { uri to it }
        }.toMap()
    }
    val stampAspectMap by produceState<Map<String, Float>>(
        initialValue = initialAspectMap,
        localContext,
        selectedUris,
        optimisticStampBitmaps
    ) {
        value = withContext(Dispatchers.IO) {
            selectedUris.associateWith { uri ->
                resolveKnownStampAspectRatio(uri, optimisticStampBitmaps[uri])
                    ?: readStampAspectRatio(localContext, uri)
            }
        }
    }

    Column(
        modifier = Modifier
            .paperGridBackground(
                backgroundColor = stampAdaptiveColor(
                    light = Color(0xFFF6F2E7),
                    dark = MaterialTheme.colorScheme.background
                ),
                lineColor = stampAdaptiveColor(
                    light = Color(0xFFE1DAC9).copy(alpha = 0.44f),
                    dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.11f)
                )
            )
            .padding(horizontal = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val containerWidth = maxWidth
            val horizontalGap = 4.dp
            val verticalGap = 10.dp
            val totalGap = horizontalGap * 6
            val cellWidth = (containerWidth - totalGap).coerceAtLeast(70.dp) / 7
            val cellHeight = cellWidth * 1.95f
            val rows = ((startOffset + daysInMonth + 6) / 7)
            val contentHeight = (cellHeight * rows) + (verticalGap * (rows - 1).coerceAtLeast(0))
            var stageOriginXPx by remember { mutableFloatStateOf(0f) }
            var stageOriginYInContentPx by remember { mutableFloatStateOf(0f) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(horizontalGap)
            ) {
                copy.weekLabels.forEach { label ->
                    val weekLabelFontSize = if (label.length > 3) 10.sp else 12.sp
                    Box(
                        modifier = Modifier.width(cellWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            textAlign = TextAlign.Center,
                            color = stampAdaptiveColor(
                                light = Color(0xFF777160),
                                dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f)
                            ),
                            fontSize = weekLabelFontSize,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .height(contentHeight)
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInRoot()
                        stageOriginXPx = bounds.left
                        stageOriginYInContentPx = bounds.top + parentScrollOffsetPx
                    }
            ) {
                for (day in 1..daysInMonth) {
                    val index = startOffset + day - 1
                    val row = index / 7
                    val column = index % 7
                    val entry = entries[day]
                    val item = entry?.selectedUri?.let { StampDisplayItem(day, entry.dayKey, it) }
                    val x = (cellWidth + horizontalGap) * column
                    val y = (cellHeight + verticalGap) * row
                    val isActiveSharedSource = activeDetailSharedGroup == sharedTransitionGroup &&
                        activeDetailUri != null &&
                        item?.uri == activeDetailUri
                    val sourceSharedVisible = !(
                        isActiveSharedSource &&
                            detailSharedTargetVisible
                        )
                    val sourceContentHidden = isActiveSharedSource &&
                        detailSharedSourceCardHidden
                    val sourceContentAlpha by animateFloatAsState(
                        targetValue = if (sourceContentHidden) 0f else 1f,
                        animationSpec = tween(
                            durationMillis = if (sourceContentHidden) 70 else 0,
                            easing = LinearEasing
                        ),
                        label = "stamp_calendar_source_alpha"
                    )
                    val hidden = activeDetailSharedGroup == sharedTransitionGroup &&
                        detailSharedTargetVisible &&
                        activeDetailUri != null &&
                        item?.uri == activeDetailUri
                    val itemSeedUri = item?.uri?.let { stableSeedByUri[it] ?: it }
                    val random = remember(day, itemSeedUri) {
                        Random(day * 917 + (itemSeedUri?.hashCode() ?: 0))
                    }
                    val stampAspect = item?.uri?.let { stampAspectMap[it] } ?: STAMP_CARD_ASPECT
                    val nearCircularStamp = stampAspect in 0.9f..1.12f
                    val stampAreaHeight = (cellHeight - 4.dp).coerceAtLeast(100.dp)
                    val maxStampHeight = (cellHeight + 4.dp).coerceAtLeast(106.dp)
                    val widthBoost = when {
                        nearCircularStamp -> 1.47f
                        stampAspect < 0.82f -> 1.39f
                        else -> 1.31f
                    }
                    val baseStampWidth = (cellWidth * 1.24f * widthBoost).coerceAtMost(cellWidth * 1.66f)
                    val stampWidth = baseStampWidth.coerceAtMost(maxStampHeight * stampAspect)
                    val stampHeight = (stampWidth / stampAspect).coerceAtMost(maxStampHeight)
                    val stampRotation = if (item != null) (random.nextFloat() - 0.5f) * 1.8f else 0f
                    val sourceCloseVisualProgress by animateFloatAsState(
                        targetValue = if (isActiveSharedSource) {
                            if (detailSharedTargetVisible) 0f else 1f
                        } else {
                            1f
                        },
                        animationSpec = tween(
                            durationMillis = STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS,
                            easing = STAMP_DETAIL_SHARED_TRANSITION_EASING
                        ),
                        label = "stamp_calendar_source_close_visual"
                    )
                    val sourceCardRotation = if (isActiveSharedSource) {
                        lerpFloat(0f, stampRotation, sourceCloseVisualProgress)
                    } else {
                        stampRotation
                    }
                    val stampTopGap = when {
                        nearCircularStamp -> 0.dp
                        stampAspect < 0.82f -> (-2).dp
                        else -> (-1).dp
                    }
                    val stampOffsetY = stampTopGap

                    Column(
                        modifier = Modifier
                            .offset(x = x, y = y)
                            .width(cellWidth)
                    ) {
                        Text(
                            text = day.toString(),
                            color = stampAdaptiveColor(
                                light = Color(0xFF3D392F),
                                dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.84f)
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 1.dp)
                                .graphicsLayer {
                                    alpha = if (hidden || sourceContentHidden) 0f else 1f
                                }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(stampAreaHeight)
                        ) {
                            if (item != null) {
                                val sourceSharedElementModifier = with(sharedTransitionScope) {
                                    rememberStampSharedElementModifier(
                                        group = sharedTransitionGroup,
                                        uri = item.uri,
                                        visible = sourceSharedVisible
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = stampOffsetY)
                                        .width(stampWidth)
                                        .aspectRatio(stampAspect)
                                        .then(sourceSharedElementModifier)
                                ) {
                                    StampCard(
                                        uri = item.uri,
                                        previewBitmap = resolveOptimisticStampPreviewBitmap(
                                            uri = item.uri,
                                            optimisticStampBitmaps = optimisticStampBitmaps,
                                            stableSeedByUri = stableSeedByUri
                                        ),
                                        skin = stampSkinByUri[item.uri],
                                        categoryEmoji = item.emoji,
                                        aspectRatio = stampAspect,
                                        rotation = sourceCardRotation,
                                        cardPadding = 3.dp,
                                        motionOptimized = !stageCardVisualsReady,
                                        entryRevealEnabled = false,
                                        onResolvedPreviewBitmap = if (isActiveSharedSource) {
                                            { bitmap ->
                                                onSharedTransitionPreviewBitmapResolved(item.uri, bitmap)
                                            }
                                        } else {
                                            null
                                        },
                                        onBoundsChange = { bounds ->
                                            onStampBoundsChange(item.uri, bounds, stampRotation)
                                        },
                                        onClick = {
                                            val stampOffsetX = (cellWidth - stampWidth) / 2
                                            val cardStartXPx = stageOriginXPx + with(density) {
                                                (x + stampOffsetX).toPx()
                                            }
                                            val cardStartYPx = (
                                                stageOriginYInContentPx +
                                                    with(density) { (y + stampOffsetY).toPx() }
                                                ) - parentScrollOffsetPx
                                            onOpenDetail(
                                                StampDetailState(
                                                    item = item,
                                                    startX = with(density) { cardStartXPx.toDp() },
                                                    startY = with(density) { cardStartYPx.toDp() },
                                                    startWidth = stampWidth,
                                                    aspectRatio = stampAspect,
                                                    startRotation = stampRotation,
                                                    shadowElevation = 0.dp,
                                                    sourceCardPadding = 2.dp,
                                                    sourceShadowStrength = 0.95f,
                                                    enableStackPreview = true,
                                                    useBottomDockTransition = true,
                                                    openedFromCalendarDay = true,
                                                    sharedTransitionGroup = null,
                                                    dayAllScrollOffset = 0,
                                                    animateDayAllEntrance = true
                                                ).withLiveSourceBounds(
                                                    bounds = sourceBoundsByUri[item.uri],
                                                    rotation = sourceRotationByUri[item.uri],
                                                    density = density
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                alpha = (if (hidden) 0f else 1f) * sourceContentAlpha
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StampWallStage(
    sharedTransitionScope: SharedTransitionScope,
    sharedTransitionGroup: String,
    detailSharedTargetVisible: Boolean,
    detailSharedSourceCardHidden: Boolean,
    entries: List<StampCalendarDayEntry>,
    copy: StampCalendarCopy,
    title: String?,
    optimisticStampBitmaps: Map<String, Bitmap>,
    sharedTransitionPreviewBitmaps: Map<String, Bitmap>,
    stableSeedByUri: Map<String, String>,
    stampSkinByUri: Map<String, StampCalendarStampSkin>,
    sourceBoundsByUri: Map<String, Rect>,
    sourceRotationByUri: Map<String, Float>,
    selectedEmoji: String?,
    showEmojiFilters: Boolean,
    enableScroll: Boolean,
    showWallMeta: Boolean,
    parentScrollOffsetPx: Float,
    onEmojiFilterChange: (String?) -> Unit,
    activeDetailUri: String?,
    activeDetailSharedGroup: String?,
    onStampBoundsChange: (String, Rect, Float) -> Unit,
    onSharedTransitionPreviewBitmapResolved: (String, Bitmap) -> Unit,
    onOpenDetail: (StampDetailState) -> Unit,
    haptic: HapticFeedback,
    hapticsAllowed: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var scrollViewportHeightPx by remember { mutableIntStateOf(0) }
    var wallTopInContentPx by remember { mutableFloatStateOf(0f) }
    var wallLeftInRootPx by remember { mutableStateOf(0f) }
    var stageCardVisualsReady by remember(sharedTransitionGroup, enableScroll, selectedEmoji) {
        mutableStateOf(false)
    }
    LaunchedEffect(sharedTransitionGroup, enableScroll, selectedEmoji) {
        stageCardVisualsReady = false
        withFrameNanos { }
        delay(if (enableScroll) 300 else 220)
        stageCardVisualsReady = true
    }
    val wallUris = remember(entries) {
        entries.flatMap { it.uris }.distinct()
    }
    val initialAspectMap = remember(wallUris, optimisticStampBitmaps) {
        wallUris.mapNotNull { uri ->
            resolveKnownStampAspectRatio(uri, optimisticStampBitmaps[uri])?.let { uri to it }
        }.toMap()
    }
    val stampAspectMap by produceState<Map<String, Float>>(
        initialValue = initialAspectMap,
        context,
        wallUris,
        optimisticStampBitmaps
    ) {
        value = withContext(Dispatchers.IO) {
            wallUris.associateWith { uri ->
                resolveKnownStampAspectRatio(uri, optimisticStampBitmaps[uri])
                    ?: readStampAspectRatio(context, uri)
            }
        }
    }
    val allEmojiGroups = remember(entries) {
        entries
            .flatMap { it.emojiByUri.values }
            .distinct()
            .sorted()
    }
    val wallItems = remember(entries, stampAspectMap, selectedEmoji, stableSeedByUri) {
        buildWallItems(
            entries = entries,
            aspectByUri = stampAspectMap,
            selectedEmoji = selectedEmoji,
            stableSeedByUri = stableSeedByUri
        )
    }
    val initialPageCount = remember(wallItems.size) { minOf(wallItems.size, STAMP_WALL_PAGE_SIZE) }
    var loadedWallItemCount by remember(enableScroll, selectedEmoji) {
        mutableIntStateOf(initialPageCount)
    }
    LaunchedEffect(enableScroll, wallItems.size, initialPageCount) {
        if (wallItems.isEmpty()) {
            loadedWallItemCount = 0
            return@LaunchedEffect
        }
        if (enableScroll) {
            loadedWallItemCount = loadedWallItemCount
                .coerceAtLeast(initialPageCount)
                .coerceAtMost(wallItems.size)
        } else {
            loadedWallItemCount = initialPageCount.coerceAtMost(wallItems.size)
            if (loadedWallItemCount < wallItems.size) {
                withFrameNanos { }
                delay(90)
                while (loadedWallItemCount < wallItems.size) {
                    loadedWallItemCount = minOf(
                        loadedWallItemCount + STAMP_WALL_PAGE_SIZE,
                        wallItems.size
                    )
                    if (loadedWallItemCount < wallItems.size) {
                        delay(16)
                    }
                }
            }
        }
    }
    val density = LocalDensity.current
    val isWallEmpty = wallItems.isEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .paperGridBackground(
                backgroundColor = stampAdaptiveColor(
                    light = Color(0xFFF6F2E7),
                    dark = MaterialTheme.colorScheme.background
                ),
                lineColor = stampAdaptiveColor(
                    light = Color(0xFFE1DAC9).copy(alpha = 0.44f),
                    dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.11f)
                )
            )
            .onSizeChanged { size ->
                if (enableScroll) {
                    scrollViewportHeightPx = size.height
                }
            }
            .then(
                if (enableScroll) Modifier.verticalScroll(scrollState) else Modifier
            )
            .padding(horizontal = 10.dp)
            .padding(bottom = 24.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = stampAdaptiveColor(
                    light = Color(0xFF181512),
                    dark = MaterialTheme.colorScheme.onBackground
                ),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 12.dp)
            )
        }
        if (showEmojiFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmojiFilterChip(
                    emoji = null,
                    allLabel = copy.allFilterLabel,
                    selected = selectedEmoji == null,
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEmojiFilterChange(null)
                    }
                )
                allEmojiGroups.forEach { emoji ->
                    EmojiFilterChip(
                        emoji = emoji,
                        allLabel = copy.allFilterLabel,
                        selected = selectedEmoji == emoji,
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEmojiFilterChange(if (selectedEmoji == emoji) null else emoji)
                        }
                    )
                }
            }
        }
        if (isWallEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    copy.emptyHint,
                    color = stampAdaptiveColor(
                        light = Color(0xFF7D7666),
                        dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f)
                    ),
                    fontSize = 15.sp
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
            val containerWidth = maxWidth
            val pagedWallItems = remember(enableScroll, wallItems, loadedWallItemCount) {
                wallItems.take(loadedWallItemCount.coerceAtMost(wallItems.size))
            }
            val placements = remember(pagedWallItems, containerWidth) {
                resolveWallPlacements(pagedWallItems, containerWidth)
            }
            val wallHeight = remember(placements) { resolveWallHeight(placements) }
            val wallHeightPx = with(density) { wallHeight.toPx() }
            val loadMoreThresholdPx = with(density) { STAMP_WALL_LOAD_MORE_THRESHOLD.toPx() }
            val canLoadMore = enableScroll && loadedWallItemCount < wallItems.size
            val shouldLoadMore by remember(
                canLoadMore,
                scrollState.value,
                scrollViewportHeightPx,
                wallHeightPx,
                loadMoreThresholdPx
            ) {
                derivedStateOf {
                    if (!canLoadMore || scrollViewportHeightPx <= 0) {
                        false
                    } else {
                        scrollState.value + scrollViewportHeightPx + loadMoreThresholdPx >= wallHeightPx
                    }
                }
            }
            LaunchedEffect(shouldLoadMore, canLoadMore, wallItems.size) {
                if (shouldLoadMore && canLoadMore) {
                    loadedWallItemCount = minOf(loadedWallItemCount + STAMP_WALL_PAGE_SIZE, wallItems.size)
                }
            }
            var knownPlacementUris by remember { mutableStateOf<Set<String>?>(null) }
            val dropTriggerByUri = remember { mutableStateMapOf<String, Int>() }
            val renderedPlacements = placements
            LaunchedEffect(placements) {
                val currentUris = placements.map { it.item.uri }.toSet()
                val previousUris = knownPlacementUris
                if (previousUris != null) {
                    val addedPendingUris = (currentUris - previousUris).filterTo(mutableSetOf()) { uri ->
                        uri.startsWith("stamp-pending://")
                    }
                    addedPendingUris.forEach { uri ->
                        dropTriggerByUri[uri] = (dropTriggerByUri[uri] ?: 0) + 1
                    }
                }
                knownPlacementUris = currentUris
                dropTriggerByUri.keys.retainAll(currentUris)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(wallHeight)
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInRoot()
                        wallLeftInRootPx = bounds.left
                        val currentTotalScrollPx =
                            (if (enableScroll) scrollState.value.toFloat() else 0f) + parentScrollOffsetPx
                        wallTopInContentPx = bounds.top + currentTotalScrollPx
                    }
                    .pointerInput(renderedPlacements, activeDetailUri) {
                        detectTapGestures { offset ->
                            val hitItem = findWallHitItem(
                                tapOffset = offset,
                                items = renderedPlacements,
                                hiddenUri = activeDetailUri,
                                density = density
                            ) ?: return@detectTapGestures
                            onOpenDetail(
                                buildDetailStateFromWallPlacement(
                                    placement = hitItem,
                                    wallLeftInRootPx = wallLeftInRootPx,
                                    wallTopInContentPx = wallTopInContentPx,
                                    totalVerticalScrollPx = (if (enableScroll) scrollState.value.toFloat() else 0f) +
                                        parentScrollOffsetPx,
                                    density = density
                                ).copy(
                                    sharedTransitionGroup = sharedTransitionGroup
                                ).withLiveSourceBounds(
                                    bounds = sourceBoundsByUri[hitItem.item.uri],
                                    rotation = sourceRotationByUri[hitItem.item.uri],
                                    density = density
                                )
                            )
                        }
                    }
            ) {
                renderedPlacements.forEach { item ->
                    key(item.item.uri) {
                        val isActiveSharedSource = activeDetailSharedGroup == sharedTransitionGroup &&
                            activeDetailUri != null &&
                            item.item.uri == activeDetailUri
                        val sourceSharedVisible = !(
                            isActiveSharedSource &&
                                detailSharedTargetVisible
                            )
                        val sourceContentHidden = isActiveSharedSource &&
                            detailSharedSourceCardHidden
                        val sourceContentAlpha by animateFloatAsState(
                            targetValue = if (sourceContentHidden) 0f else 1f,
                            animationSpec = tween(
                                durationMillis = if (sourceContentHidden) 70 else 0,
                                easing = LinearEasing
                            ),
                            label = "stamp_wall_source_alpha"
                        )
                        val hidden = activeDetailSharedGroup == sharedTransitionGroup &&
                            detailSharedTargetVisible &&
                            activeDetailUri != null &&
                            item.item.uri == activeDetailUri
                        val animatedX by animateDpAsState(
                            targetValue = item.startX,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "stamp_wall_x"
                        )
                        val animatedY by animateDpAsState(
                            targetValue = item.startY,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "stamp_wall_y"
                        )
                        val animatedWidth by animateDpAsState(
                            targetValue = item.startWidth,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "stamp_wall_width"
                        )
                        val animatedRotation by animateFloatAsState(
                            targetValue = item.startRotation,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "stamp_wall_rotation"
                        )
                        val sourceCloseVisualProgress by animateFloatAsState(
                            targetValue = if (isActiveSharedSource) {
                                if (detailSharedTargetVisible) 0f else 1f
                            } else {
                                1f
                            },
                            animationSpec = tween(
                                durationMillis = STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS,
                                easing = STAMP_DETAIL_SHARED_TRANSITION_EASING
                            ),
                            label = "stamp_wall_source_close_visual"
                        )
                        val sourceCardRotation = if (isActiveSharedSource) {
                            lerpFloat(0f, animatedRotation, sourceCloseVisualProgress)
                        } else {
                            animatedRotation
                        }
                        val dropTrigger = dropTriggerByUri[item.item.uri] ?: 0
                        val animatePendingDrop = remember(item.item.uri) {
                            item.item.uri.startsWith("stamp-pending://")
                        }
                        val shouldPlayDrop = dropTrigger > 0 || animatePendingDrop
                        var dropArrived by remember(item.item.uri, dropTrigger) {
                            mutableStateOf(!shouldPlayDrop)
                        }
                        LaunchedEffect(item.item.uri, dropTrigger, shouldPlayDrop) {
                            if (shouldPlayDrop) {
                                dropArrived = false
                                delay(16)
                                dropArrived = true
                            }
                        }
                        val dropOffsetY by animateDpAsState(
                            targetValue = if (dropArrived) 0.dp else (-82).dp,
                            animationSpec = tween(
                                durationMillis = 460,
                                easing = androidx.compose.animation.core.LinearOutSlowInEasing
                            ),
                            label = "stamp_wall_drop_y"
                        )
                        val dropScale by animateFloatAsState(
                            targetValue = if (dropArrived) 1f else 1.05f,
                            animationSpec = tween(
                                durationMillis = 420,
                                easing = androidx.compose.animation.core.LinearOutSlowInEasing
                            ),
                            label = "stamp_wall_drop_scale"
                        )
                        val dropAlpha by animateFloatAsState(
                            targetValue = if (dropArrived) 1f else 0f,
                            animationSpec = tween(durationMillis = 320),
                            label = "stamp_wall_drop_alpha"
                        )
                        val cardAlpha = if (hidden) 0f else dropAlpha
                        val noteTop = animatedY +
                            (animatedWidth / item.aspectRatio.coerceIn(0.55f, 1.45f)) +
                            3.dp
                        val sourceSharedElementModifier = with(sharedTransitionScope) {
                            rememberStampSharedElementModifier(
                                group = sharedTransitionGroup,
                                uri = item.item.uri,
                                visible = sourceSharedVisible
                            )
                        }
                        Box(
                            modifier = Modifier
                                .offset(x = animatedX, y = animatedY + dropOffsetY)
                                .width(animatedWidth)
                                .aspectRatio(item.aspectRatio)
                                .then(sourceSharedElementModifier)
                        ) {
                            StampCard(
                                uri = item.item.uri,
                                previewBitmap = resolveOptimisticStampPreviewBitmap(
                                    uri = item.item.uri,
                                    optimisticStampBitmaps = optimisticStampBitmaps,
                                    stableSeedByUri = stableSeedByUri
                                ),
                                skin = stampSkinByUri[item.item.uri],
                                categoryEmoji = item.item.emoji,
                                aspectRatio = item.aspectRatio,
                                rotation = sourceCardRotation,
                                motionOptimized = !stageCardVisualsReady,
                                entryRevealEnabled = stageCardVisualsReady && !isActiveSharedSource,
                                onResolvedPreviewBitmap = if (isActiveSharedSource) {
                                    { bitmap ->
                                        onSharedTransitionPreviewBitmapResolved(item.item.uri, bitmap)
                                    }
                                } else {
                                    null
                                },
                                onBoundsChange = { bounds ->
                                    onStampBoundsChange(item.item.uri, bounds, animatedRotation)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = cardAlpha * sourceContentAlpha
                                        scaleX = dropScale
                                        scaleY = dropScale
                                    }
                            )
                        }
                        if (showWallMeta && !item.item.note.isNullOrBlank()) {
                            Text(
                                text = item.item.note.orEmpty().trim().limitWallNoteChars(),
                                color = stampAdaptiveColor(
                                    light = Color(0xFF5F584E),
                                    dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                                ),
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .offset(x = animatedX, y = noteTop + dropOffsetY)
                                    .width(animatedWidth)
                                    .graphicsLayer { alpha = cardAlpha * sourceContentAlpha }
                            )
                        }
                    }
                }
            }
            }
        }
        Spacer(modifier = Modifier.height(STAMP_WALL_CONTENT_BOTTOM_EXTRA_SPACE))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StampFullScreenGridPage(
    sharedTransitionScope: SharedTransitionScope,
    detailSharedTargetVisible: Boolean,
    detailSharedSourceCardHidden: Boolean,
    detailState: StampDetailState,
    detailUris: List<String>,
    requestCloseSignal: Int,
    copy: StampCalendarCopy,
    optimisticStampBitmaps: Map<String, Bitmap>,
    sharedTransitionPreviewBitmaps: Map<String, Bitmap>,
    stableSeedByUri: Map<String, String>,
    stampSkinByUri: Map<String, StampCalendarStampSkin>,
    sharedTransitionGroup: String? = null,
    activeDetailUri: String? = null,
    activeDetailSharedGroup: String? = null,
    passiveUnderDetail: Boolean = false,
    onStampBoundsChange: (String, Rect, Float) -> Unit,
    onSharedTransitionPreviewBitmapResolved: (String, Bitmap) -> Unit,
    onClose: () -> Unit,
    onOpenDetail: (StampDetailState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val density = LocalDensity.current
    val calendarDataVersion by observeStampCalendarDataVersion().collectAsState()
    val detailMonthKey = remember(detailState.item.dayKey) {
        detailState.item.dayKey.substringBeforeLast("-")
    }
    val detailMonthEntries = remember(detailMonthKey, calendarDataVersion) {
        loadStampCalendarMonthEntries(context, detailMonthKey)
    }
    val gridDayEntry = remember(detailMonthEntries, detailState.item.day) {
        detailMonthEntries[detailState.item.day]
    }
    var handledCloseSignal by remember(detailState.item.dayKey) { mutableIntStateOf(requestCloseSignal) }
    var closing by remember(detailState.item.dayKey) { mutableStateOf(false) }
    var scrollViewportHeightPx by remember(detailState.item.dayKey) { mutableIntStateOf(0) }
    val fullDateLabel = remember(detailState.item.dayKey) {
        detailState.item.dayKey.replace("-", ".")
    }
    val tileBoundsByUri = remember(detailState.item.dayKey) { mutableStateMapOf<String, Rect>() }
    val scrollState = rememberScrollState(detailState.dayAllScrollOffset.coerceAtLeast(0))
    val gridTransitionProgress = remember(detailState.item.dayKey, passiveUnderDetail) {
        Animatable(if (detailState.animateDayAllEntrance && !passiveUnderDetail) 0f else 1f)
    }
    val initialAspectMap = remember(detailUris, optimisticStampBitmaps) {
        detailUris.mapNotNull { uri ->
            resolveKnownStampAspectRatio(uri, optimisticStampBitmaps[uri])?.let { uri to it }
        }.toMap()
    }
    val aspectByUri by produceState<Map<String, Float>>(
        initialValue = initialAspectMap,
        context,
        detailUris,
        optimisticStampBitmaps
    ) {
        value = withContext(Dispatchers.IO) {
            detailUris.associateWith { uri ->
                resolveKnownStampAspectRatio(uri, optimisticStampBitmaps[uri])
                    ?: readStampAspectRatio(context, uri)
            }
        }
    }
    val requestDismiss: () -> Unit = {
        if (!closing) {
            closing = true
        }
    }
    BackHandler(enabled = !passiveUnderDetail) {
        requestDismiss()
    }
    LaunchedEffect(requestCloseSignal, detailState.item.dayKey, passiveUnderDetail) {
        if (!passiveUnderDetail && requestCloseSignal != handledCloseSignal && !closing) {
            handledCloseSignal = requestCloseSignal
            requestDismiss()
        }
    }
    LaunchedEffect(detailState.item.dayKey, closing, passiveUnderDetail) {
        if (passiveUnderDetail) {
            gridTransitionProgress.snapTo(1f)
            return@LaunchedEffect
        }
        if (closing) return@LaunchedEffect
        val targetScrollOffset = detailState.dayAllScrollOffset.coerceAtLeast(0)
        if (scrollState.value != targetScrollOffset) {
            scrollState.scrollTo(targetScrollOffset)
        }
        if (detailState.animateDayAllEntrance) {
            gridTransitionProgress.snapTo(0f)
            gridTransitionProgress.animateTo(
                targetValue = 0.44f,
                animationSpec = tween(
                    durationMillis = 320,
                    easing = CubicBezierEasing(0.22f, 0.9f, 0.28f, 1f)
                )
            )
            gridTransitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = CubicBezierEasing(0.18f, 0.98f, 0.28f, 1f)
                )
            )
        } else {
            gridTransitionProgress.snapTo(1f)
        }
    }
    LaunchedEffect(closing, detailState.item.dayKey, passiveUnderDetail) {
        if (!passiveUnderDetail && closing) {
            gridTransitionProgress.animateTo(
                targetValue = 0.44f,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutSlowInEasing
                )
            )
            gridTransitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = CubicBezierEasing(0.4f, 0f, 0.8f, 0.2f)
                )
            )
            onClose()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                stampAdaptiveColor(
                    light = Color(0xFFF6F2E7),
                    dark = MaterialTheme.colorScheme.background
                )
            )
            .paperGridBackground(
                backgroundColor = stampAdaptiveColor(
                    light = Color(0xFFF6F2E7),
                    dark = MaterialTheme.colorScheme.background
                ),
                lineColor = stampAdaptiveColor(
                    light = Color(0xFFE1DAC9).copy(alpha = 0.44f),
                    dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.11f)
                )
            )
    ) {
        fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp {
            val clamped = fraction.coerceIn(0f, 1f)
            return start + ((end - start) * clamped)
        }

        fun stagedLerpDp(start: Dp, center: Dp, end: Dp, progress: Float): Dp {
            val split = 0.44f
            return if (progress <= split) {
                lerpDp(start, center, progress / split)
            } else {
                lerpDp(center, end, (progress - split) / (1f - split))
            }
        }

        fun stagedLerpFloat(start: Float, center: Float, end: Float, progress: Float): Float {
            val split = 0.44f
            return if (progress <= split) {
                lerpFloat(start, center, progress / split)
            } else {
                lerpFloat(center, end, (progress - split) / (1f - split))
            }
        }

        val tileGap = 14.dp
        val rowGap = 14.dp
        val horizontalPadding = 22.dp
        val contentTopPadding = 6.dp
        val contentBottomPadding = 112.dp
        val stageWidth = maxWidth
        val contentWidth = (maxWidth - (horizontalPadding * 2f)).coerceAtLeast(220.dp)
        val tileCardWidth = ((contentWidth - tileGap) / 2f).coerceAtLeast(128.dp)
        val rows = remember(detailUris) { detailUris.chunked(2) }
        val placements = remember(detailUris, rows, aspectByUri, tileCardWidth, tileGap) {
            val resolved = mutableListOf<StampDayAllGridPlacement>()
            var rowTop = contentTopPadding
            rows.forEachIndexed { rowIndex, rowUris ->
                val rowPlacements = rowUris.mapIndexed { columnIndex, uri ->
                    val aspectRatio = (aspectByUri[uri] ?: STAMP_CARD_ASPECT).coerceIn(0.55f, 1.45f)
                    StampDayAllGridPlacement(
                        uri = uri,
                        aspectRatio = aspectRatio,
                        column = columnIndex,
                        row = rowIndex,
                        x = if (columnIndex == 0) 0.dp else tileCardWidth + tileGap,
                        y = 0.dp,
                        width = tileCardWidth
                    )
                }
                val rowMaxHeight = rowPlacements.maxOfOrNull { item ->
                    tileCardWidth / item.aspectRatio
                } ?: 0.dp
                rowPlacements.forEach { item ->
                    val itemHeight = tileCardWidth / item.aspectRatio
                    resolved += item.copy(
                        y = rowTop + ((rowMaxHeight - itemHeight) / 2f)
                    )
                }
                rowTop += rowMaxHeight + rowGap
            }
            resolved
        }
        val finalContentBottom = placements.maxOfOrNull { placement ->
            placement.y + (placement.width / placement.aspectRatio)
        } ?: 0.dp
        val viewportHeight = if (scrollViewportHeightPx > 0) {
            with(density) { scrollViewportHeightPx.toDp() }
        } else {
            maxHeight * 0.72f
        }
        val scrollOffsetDp = with(density) { scrollState.value.toDp() }
        val contentHeight = (finalContentBottom + contentBottomPadding)
            .coerceAtLeast(viewportHeight)
        val transitionProgress = gridTransitionProgress.value.coerceIn(0f, 1f)
        val headerAlpha = if (passiveUnderDetail) {
            1f
        } else {
            ((transitionProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
        }
        val headerLift = 18.dp * (1f - headerAlpha)
        val contentAlpha = if (passiveUnderDetail) {
            1f
        } else {
            ((transitionProgress - 0.04f) / 0.22f).coerceIn(0f, 1f)
        }
        val gridInteractive = !passiveUnderDetail && transitionProgress >= 0.999f && !closing
        val enableGridWindowing = detailUris.size >= 14
        val gridViewportBuffer = 320.dp
        val visibleTop = scrollOffsetDp - gridViewportBuffer
        val visibleBottom = scrollOffsetDp + viewportHeight + gridViewportBuffer
        val stackCardWidth = (tileCardWidth * 1.04f)
            .coerceAtLeast(156.dp)
            .coerceAtMost((contentWidth * 0.78f).coerceAtLeast(156.dp))
        val startStackWidth = (stackCardWidth * 0.92f).coerceAtLeast(148.dp)
        val stackBaseX = ((contentWidth - stackCardWidth) / 2f).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = stampAdaptiveColor(
                        light = Color.White.copy(alpha = 0.88f),
                        dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                    ),
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = headerAlpha
                            translationY = with(density) { headerLift.toPx() }
                        }
                        .size(44.dp)
                        .clip(CircleShape)
                        .noRippleClickable(enabled = !closing, onClick = requestDismiss)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = stampAdaptiveColor(
                                light = Color(0xFF1C1915),
                                dark = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = headerAlpha
                        translationY = with(density) { headerLift.toPx() }
                    }
                ) {
                    Text(
                        text = dayAllTitle(appLanguage),
                        color = stampAdaptiveColor(
                            light = Color(0xFF181512),
                            dark = MaterialTheme.colorScheme.onBackground
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = fullDateLabel,
                        color = stampAdaptiveColor(
                            light = Color(0xFF181512),
                            dark = MaterialTheme.colorScheme.onBackground
                        ),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .onSizeChanged { size -> scrollViewportHeightPx = size.height }
                    .verticalScroll(scrollState, enabled = gridInteractive)
                    .padding(horizontal = horizontalPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(contentHeight)
                ) {
                    placements.forEachIndexed { index, placement ->
                        val aspectRatio = placement.aspectRatio
                        val depthIndex = (detailUris.size - 1 - index).coerceIn(0, 6)
                        val fanDirection = if (index % 2 == 0) -1f else 1f
                        val stackFanX = (12f * fanDirection + (((index % 3) - 1) * 4f)).dp
                        val stackDepthY = (depthIndex * 7).dp
                        val isEntryHead = placement.uri == detailState.item.uri
                        val defaultStartX = stackBaseX + (stackFanX * 0.46f)
                        val startX = defaultStartX
                        val centerX = stackBaseX + stackFanX
                        val centerHeight = stackCardWidth / aspectRatio
                        val defaultStartY = scrollOffsetDp + viewportHeight + 48.dp + stackDepthY + (index * 5).dp
                        val startY = defaultStartY
                        val centerY = scrollOffsetDp +
                            ((viewportHeight - centerHeight) / 2f).coerceAtLeast(80.dp) +
                            stackDepthY
                        val currentX = stagedLerpDp(startX, centerX, placement.x, transitionProgress)
                        val currentY = stagedLerpDp(startY, centerY, placement.y, transitionProgress)
                        val currentWidth = stagedLerpDp(
                            start = startStackWidth,
                            center = stackCardWidth,
                            end = placement.width,
                            progress = transitionProgress
                        )
                        val currentHeight = currentWidth / aspectRatio
                        if (
                            enableGridWindowing &&
                            gridInteractive &&
                            ((currentY + currentHeight) < visibleTop || currentY > visibleBottom)
                        ) {
                            return@forEachIndexed
                        }
                        val startRotation = fanDirection * (7.4f + ((index % 4) * 1.2f))
                        val centerRotation = startRotation * 0.58f
                        val currentRotation = stagedLerpFloat(
                            start = startRotation,
                            center = centerRotation,
                            end = 0f,
                            progress = transitionProgress
                        )
                        val shadowStrength = detailState.sourceShadowStrength
                        val isActiveSharedSource = activeDetailSharedGroup == sharedTransitionGroup &&
                            activeDetailUri != null &&
                            placement.uri == activeDetailUri
                        val sourceSharedVisible = !(
                            isActiveSharedSource &&
                                detailSharedTargetVisible
                            )
                        val sourceContentHidden = isActiveSharedSource &&
                            detailSharedSourceCardHidden
                        val sourceCloseVisualProgress = if (isActiveSharedSource) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = if (detailSharedTargetVisible) 0f else 1f,
                                animationSpec = tween(
                                    durationMillis = STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS,
                                    easing = STAMP_DETAIL_SHARED_TRANSITION_EASING
                                ),
                                label = "stamp_day_grid_source_close_visual"
                            )
                            animatedProgress
                        } else {
                            1f
                        }
                        val sourceCardRotation = if (isActiveSharedSource) {
                            lerpFloat(0f, currentRotation, sourceCloseVisualProgress)
                        } else {
                            currentRotation
                        }
                        val sourceContentAlpha = if (sourceContentHidden) {
                            val animatedAlpha by animateFloatAsState(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = 70,
                                    easing = LinearEasing
                                ),
                                label = "stamp_day_grid_source_alpha"
                            )
                            animatedAlpha
                        } else {
                            1f
                        }
                        val hidden = activeDetailSharedGroup == sharedTransitionGroup &&
                            detailSharedTargetVisible &&
                            activeDetailUri != null &&
                            placement.uri == activeDetailUri
                        val sourceSharedElementModifier = if (isActiveSharedSource) {
                            with(sharedTransitionScope) {
                                rememberStampSharedElementModifier(
                                    group = sharedTransitionGroup,
                                    uri = placement.uri,
                                    visible = sourceSharedVisible
                                )
                            }
                        } else {
                            Modifier
                        }
                        val targetBounds = if (gridInteractive) {
                            tileBoundsByUri[placement.uri]
                        } else {
                            null
                        }
                        Box(
                            modifier = Modifier
                                .offset(x = currentX, y = currentY)
                                .width(currentWidth)
                                .aspectRatio(aspectRatio)
                                .then(sourceSharedElementModifier)
                                .zIndex(
                                    if (transitionProgress < 0.62f) {
                                        if (isEntryHead) {
                                            (detailUris.size + 2).toFloat()
                                        } else {
                                            (detailUris.size - index).toFloat()
                                        }
                                    } else {
                                        1f
                                    }
                                )
                        ) {
                            StampCard(
                                uri = placement.uri,
                                previewBitmap = resolveOptimisticStampPreviewBitmap(
                                    uri = placement.uri,
                                    optimisticStampBitmaps = optimisticStampBitmaps,
                                    stableSeedByUri = stableSeedByUri
                                ),
                                skin = stampSkinByUri[placement.uri],
                                categoryEmoji = gridDayEntry?.emojiByUri?.get(placement.uri),
                                aspectRatio = aspectRatio,
                                rotation = sourceCardRotation,
                                shadowStrength = shadowStrength,
                                entryRevealEnabled = false,
                                onResolvedPreviewBitmap = if (isActiveSharedSource) {
                                    { bitmap ->
                                        onSharedTransitionPreviewBitmapResolved(placement.uri, bitmap)
                                    }
                                } else {
                                    null
                                },
                                onBoundsChange = if (passiveUnderDetail) {
                                    null
                                } else {
                                    { bounds ->
                                        if (tileBoundsByUri[placement.uri] != bounds) {
                                            tileBoundsByUri[placement.uri] = bounds
                                        }
                                        onStampBoundsChange(placement.uri, bounds, currentRotation)
                                    }
                                },
                                onClick = if (!gridInteractive) {
                                    null
                                } else {
                                    {
                                        val startXBounds = targetBounds?.let { with(density) { it.left.toDp() } }
                                            ?: ((stageWidth - tileCardWidth) / 2f)
                                        val startYBounds = targetBounds?.let { with(density) { it.top.toDp() } }
                                            ?: 220.dp
                                        val startWidthBounds = targetBounds?.let { with(density) { it.width.toDp() } }
                                            ?: tileCardWidth
                                        onOpenDetail(
                                            detailState.copy(
                                                item = detailState.item.copy(uri = placement.uri),
                                                startX = startXBounds,
                                                startY = startYBounds,
                                                startWidth = startWidthBounds,
                                                aspectRatio = aspectRatio,
                                                startRotation = 0f,
                                                shadowElevation = 0.dp,
                                                sourceCardPadding = 12.dp,
                                                sourceShadowStrength = shadowStrength,
                                                enableStackPreview = false,
                                                useBottomDockTransition = false,
                                                sharedTransitionGroup = dayGridSharedTransitionGroup(
                                                    detailState.item.dayKey
                                                ),
                                                dayAllScrollOffset = scrollState.value,
                                                animateDayAllEntrance = false
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = (if (hidden) 0f else contentAlpha) * sourceContentAlpha
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StampFullScreenDetailPage(
    sharedTransitionScope: SharedTransitionScope,
    detailSharedTargetVisible: Boolean,
    detailSharedSourceCardHidden: Boolean,
    detailState: StampDetailState?,
    requestCloseSignal: Int,
    copy: StampCalendarCopy,
    optimisticStampBitmaps: Map<String, Bitmap>,
    sharedTransitionPreviewBitmaps: Map<String, Bitmap>,
    stableSeedByUri: Map<String, String>,
    stampSkinByUri: Map<String, StampCalendarStampSkin>,
    sourceBoundsByUri: Map<String, Rect>,
    sourceRotationByUri: Map<String, Float>,
    dayAllBoundsByUri: Map<String, Rect>,
    dayAllRotationByUri: Map<String, Float>,
    onDayAllStampBoundsChange: (String, Rect, Float) -> Unit,
    haptic: HapticFeedback,
    hapticsAllowed: Boolean,
    onSharedTargetVisibilityChange: (Boolean) -> Unit,
    onSharedSourceCardHiddenChange: (Boolean) -> Unit,
    onSharedTransitionPreviewBitmapResolved: (String, Bitmap) -> Unit,
    onClose: () -> Unit,
    onOpenDetail: (StampDetailState) -> Unit,
    onRetargetDetail: (StampDetailState) -> Unit,
    onCampfireRequested: (() -> Unit)?,
    onDelete: (StampDisplayItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (detailState == null) return

    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val stampPageStrings = remember(appLanguage) { getStampPageStrings(appLanguage) }
    val skinPanelCopy = remember(appLanguage) { stampSkinPanelCopy(appLanguage) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val detailTransitionSession = remember(
        detailState.item.dayKey,
        detailState.item.uri,
        detailState.startX,
        detailState.startY,
        detailState.startWidth,
        detailState.enableStackPreview
    ) {
        Any()
    }
    var expanded by remember(detailTransitionSession) { mutableStateOf(false) }
    var closing by remember(detailTransitionSession) { mutableStateOf(false) }
    var sideCardsExpanded by remember(detailTransitionSession) { mutableStateOf(false) }
    var stackGridExpanded by remember(detailTransitionSession) { mutableStateOf(false) }
    val calendarDataVersion by observeStampCalendarDataVersion().collectAsState()
    val detailMonthKey = remember(detailState.item.dayKey) {
        detailState.item.dayKey.substringBeforeLast("-")
    }
    val detailMonthEntries = remember(detailMonthKey, calendarDataVersion) {
        loadStampCalendarMonthEntries(context, detailMonthKey)
    }
    val dayEntry = remember(detailMonthEntries, detailState.item.day) {
        detailMonthEntries[detailState.item.day]
    }
    val detailUris = remember(dayEntry, detailState.item.uri) {
        val primaryUri = detailState.item.uri.trim()
        val persistedUris = dayEntry?.uris
            ?.asSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.toList()
            .orEmpty()
        buildList {
            persistedUris.forEach { uri ->
                if (uri !in this) {
                    add(uri)
                }
            }
            if (primaryUri.isNotEmpty() && primaryUri !in this) {
                add(primaryUri)
            }
        }
    }
    if (detailState.enableStackPreview) {
        StampFullScreenGridPage(
            sharedTransitionScope = sharedTransitionScope,
            detailSharedTargetVisible = detailSharedTargetVisible,
            detailSharedSourceCardHidden = detailSharedSourceCardHidden,
            detailState = detailState,
            detailUris = detailUris,
            requestCloseSignal = requestCloseSignal,
            copy = copy,
            optimisticStampBitmaps = optimisticStampBitmaps,
            sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps,
            stableSeedByUri = stableSeedByUri,
            stampSkinByUri = stampSkinByUri,
            sharedTransitionGroup = dayGridSharedTransitionGroup(detailState.item.dayKey),
            activeDetailUri = detailState.item.uri,
            activeDetailSharedGroup = detailState.sharedTransitionGroup,
            onStampBoundsChange = onDayAllStampBoundsChange,
            onSharedTransitionPreviewBitmapResolved = onSharedTransitionPreviewBitmapResolved,
            onClose = onClose,
            onOpenDetail = onOpenDetail,
            modifier = modifier
        )
        return
    }
    val stackPreviewEnabled = detailState.enableStackPreview && detailUris.size > 1
    val showDetailMetadata = !stackPreviewEnabled
    val scope = rememberCoroutineScope()
    var activeStampUriState by remember(detailState.item.dayKey, detailState.item.uri) {
        mutableStateOf(detailState.item.uri)
    }
    LaunchedEffect(detailUris, detailState.item.uri) {
        if (activeStampUriState !in detailUris) {
            activeStampUriState = detailState.item.uri.takeIf { it in detailUris } ?: detailUris.first()
        }
    }
    val activeStampUri = activeStampUriState.takeIf { it in detailUris }
        ?: detailState.item.uri.takeIf { it in detailUris }
        ?: detailUris.first()
    val activeIndex = detailUris.indexOf(activeStampUri).coerceAtLeast(0)
    val previousStampUri = if (detailUris.size > 1) {
        detailUris[(activeIndex - 1 + detailUris.size) % detailUris.size]
    } else {
        null
    }
    val nextStampUri = if (detailUris.size > 1) {
        detailUris[(activeIndex + 1) % detailUris.size]
    } else {
        null
    }
    val previous2StampUri = if (detailUris.size > 3) {
        detailUris[(activeIndex - 2 + detailUris.size) % detailUris.size]
    } else {
        null
    }
    val next2StampUri = if (detailUris.size > 3) {
        detailUris[(activeIndex + 2) % detailUris.size]
    } else {
        null
    }
    var frozenDisplayUri by remember(detailTransitionSession) { mutableStateOf<String?>(null) }
    var returnToDayGridAfterClose by remember(detailTransitionSession) { mutableStateOf(false) }
    var swipeDirection by remember(detailTransitionSession) { mutableIntStateOf(-1) }
    var stackDragOffsetPx by remember(detailTransitionSession) { mutableFloatStateOf(0f) }
    var stackSettling by remember(detailTransitionSession) { mutableStateOf(false) }
    var detailCardMotionOptimized by remember(detailTransitionSession) { mutableStateOf(true) }
    val displayStampUri = frozenDisplayUri ?: activeStampUri
    var confirmDelete by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var renderDeleteDialog by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var deleteDialogOpenSignal by remember(detailState.item.dayKey, activeStampUri) { mutableIntStateOf(0) }
    var handledCloseSignal by remember(detailState.item.dayKey) { mutableIntStateOf(requestCloseSignal) }
    val initialNote = remember(dayEntry, activeStampUri, calendarDataVersion) {
        dayEntry?.let { entry ->
            entry.noteByUri[activeStampUri] ?: entry.note
        }.orEmpty()
    }
    var note by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(initialNote) }
    var savedNote by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(initialNote) }
    var selectedEmoji by remember(dayEntry, activeStampUri, calendarDataVersion) {
        mutableStateOf(dayEntry?.emojiByUri?.get(activeStampUri))
    }
    val cutoutShapeLocked = remember(dayEntry, activeStampUri, calendarDataVersion) {
        dayEntry?.cutoutLockedByUri?.get(activeStampUri) == true
    }
    val initialSkin = remember(dayEntry, activeStampUri, calendarDataVersion, cutoutShapeLocked) {
        val resolvedSkin = dayEntry?.skinByUri?.get(activeStampUri).normalized()
        if (!cutoutShapeLocked) {
            resolvedSkin
        } else {
            (resolvedSkin ?: StampCalendarStampSkin(style = StampCalendarSkinStyle.Cutout))
                .copy(
                    style = StampCalendarSkinStyle.Cutout,
                    numberLabel = "",
                    title = "",
                    subtitle = ""
                )
                .normalized()
        }
    }
    var savedSkin by remember(detailState.item.dayKey, activeStampUri, initialSkin) {
        mutableStateOf(initialSkin)
    }
    var draftSkin by remember(detailState.item.dayKey, activeStampUri, initialSkin) {
        mutableStateOf(initialSkin)
    }
    var showEmojiPanel by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var renderEmojiPanel by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var emojiPanelOpenSignal by remember(detailState.item.dayKey, activeStampUri) { mutableIntStateOf(0) }
    var showSkinPanel by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var renderSkinPanel by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var skinCutoutPreviewInFlight by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var skinConfirmInFlight by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var showSkinCampfireGateDialog by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var skinPanelOpenSignal by remember(detailState.item.dayKey, activeStampUri) { mutableIntStateOf(0) }
    var showDatePanel by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var renderDatePanel by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var datePanelOpenSignal by remember(detailState.item.dayKey, activeStampUri) { mutableIntStateOf(0) }
    var dateMoveInFlight by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var exportingDetailImage by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var isNoteEditing by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var noteFieldFocused by remember(detailState.item.dayKey, activeStampUri) { mutableStateOf(false) }
    var imeBecameVisibleSinceFocus by remember(detailState.item.dayKey, activeStampUri) {
        mutableStateOf(false)
    }
    var dateTriggerBounds by remember(detailState.item.dayKey, activeStampUri) {
        mutableStateOf<Rect?>(null)
    }
    var emojiTriggerBounds by remember(detailState.item.dayKey, activeStampUri) {
        mutableStateOf<Rect?>(null)
    }
    var skinTriggerBounds by remember(detailState.item.dayKey, activeStampUri) {
        mutableStateOf<Rect?>(null)
    }
    var deleteTriggerBounds by remember(detailState.item.dayKey, activeStampUri) {
        mutableStateOf<Rect?>(null)
    }
    var thanksTriggerBounds by remember(detailState.item.dayKey) {
        mutableStateOf<Rect?>(null)
    }
    var showSourceContext by remember(detailState.item.dayKey, activeStampUri) {
        mutableStateOf(false)
    }
    val sourceContextVersion by observeStampSourceContextVersion().collectAsState()
    val stampSourceContext = remember(activeStampUri, sourceContextVersion) {
        loadStampSourceContext(
            context = context,
            stampUri = activeStampUri
        )
    }
    val fullDateLabel = remember(detailState.item.dayKey) {
        detailState.item.dayKey.replace("-", ".")
    }
    val currentDetailDate = remember(detailState.item.dayKey) {
        parseStampCalendarDate(detailState.item.dayKey)
    }
    val isDetailFromCalendarDay = detailState.openedFromCalendarDay
    var dateLabelWidthPx by remember(detailState.item.dayKey) { mutableFloatStateOf(0f) }
    val showDayCoverAction = isDetailFromCalendarDay && detailState.useBottomDockTransition
    val isViewingTodayDay = remember(detailState.item.dayKey) {
        detailState.item.dayKey == stampCalendarTodayKey()
    }
    val isDayCoverSelected = remember(
        activeStampUri,
        showDayCoverAction,
        dayEntry?.selectedUri
    ) {
        showDayCoverAction && dayEntry?.selectedUri == activeStampUri
    }
    val dayCoverSelectionProgress by animateFloatAsState(
        targetValue = if (isDayCoverSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "stamp_today_cover_progress"
    )
    val dayCoverBgColor by animateColorAsState(
        targetValue = if (isDayCoverSelected) {
            Color(0xFF1F8B4C)
        } else {
            stampAdaptiveColor(
                light = Color.White.copy(alpha = 0.93f),
                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f)
            )
        },
        animationSpec = tween(durationMillis = 280),
        label = "stamp_today_cover_bg"
    )
    val dayCoverTextColor by animateColorAsState(
        targetValue = if (isDayCoverSelected) {
            Color.White
        } else {
            stampAdaptiveColor(
                light = Color(0xFF181512),
                dark = MaterialTheme.colorScheme.onSurface
            )
        },
        animationSpec = tween(durationMillis = 280),
        label = "stamp_today_cover_text"
    )
    val dateUnderlineColor = stampAdaptiveColor(
        light = Color(0xFF8E856F).copy(alpha = 0.72f),
        dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
    )
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val latestImeVisible by rememberUpdatedState(imeVisible)
    val persistCurrentNote: () -> Unit = {
        val finalNote = note.trimEnd()
        if (finalNote != savedNote) {
            updateStampCalendarStampNote(
                context = context,
                dayKey = detailState.item.dayKey,
                uriString = activeStampUri,
                note = finalNote
            )
            note = finalNote
            savedNote = finalNote
        }
    }
    val persistDraftSkin: () -> Unit = {
        val finalSkin = if (cutoutShapeLocked) {
            draftSkin.copy(
                style = StampCalendarSkinStyle.Cutout,
                numberLabel = "",
                title = "",
                subtitle = ""
            ).normalized()
        } else {
            draftSkin.normalized()
        }
        if (finalSkin != savedSkin) {
            updateStampCalendarStampSkin(
                context = context,
                dayKey = detailState.item.dayKey,
                uriString = activeStampUri,
                skin = finalSkin
            )
            if (cutoutShapeLocked) {
                updateStampCalendarStampCutoutLock(
                    context = context,
                    dayKey = detailState.item.dayKey,
                    uriString = activeStampUri,
                    locked = true
                )
            }
            savedSkin = finalSkin
            draftSkin = finalSkin
        }
    }
    val shouldReturnToDayGrid = isDetailFromCalendarDay && !detailState.enableStackPreview
    val currentDayGridSharedGroup = remember(detailState.item.dayKey) {
        dayGridSharedTransitionGroup(detailState.item.dayKey)
    }
    val reopenDayGrid: () -> Unit = {
        onOpenDetail(
            detailState.copy(
                item = detailState.item.copy(uri = activeStampUri),
                enableStackPreview = true,
                useBottomDockTransition = false,
                sharedTransitionGroup = null,
                animateDayAllEntrance = false
            )
        )
    }
    val detailSharedTransitionGroup = detailState.sharedTransitionGroup
    var sharedTargetFallbackVisible by remember(detailTransitionSession) { mutableStateOf(false) }
    val effectiveDetailSharedTargetVisible =
        detailSharedTargetVisible || (!closing && sharedTargetFallbackVisible)
    val navigateBackFromDetail: () -> Unit = navigate@{
        if (dateMoveInFlight) return@navigate
        showDatePanel = false
        renderDatePanel = false
        showSkinPanel = false
        renderSkinPanel = false
        if (shouldReturnToDayGrid) {
            persistCurrentNote()
            isNoteEditing = false
            noteFieldFocused = false
            imeBecameVisibleSinceFocus = false
            showEmojiPanel = false
            renderEmojiPanel = false
            confirmDelete = false
            renderDeleteDialog = false
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            returnToDayGridAfterClose = true
            closing = true
        } else if (!closing) {
            closing = true
        }
    }
    BackHandler(enabled = showDetailMetadata && showEmojiPanel) {
        showEmojiPanel = false
    }
    BackHandler(enabled = showDetailMetadata && showSkinPanel) {
        showSkinPanel = false
    }
    BackHandler(enabled = showDetailMetadata && showDatePanel) {
        showDatePanel = false
    }
    BackHandler(enabled = confirmDelete) {
        confirmDelete = false
    }
    BackHandler(enabled = showSourceContext) {
        showSourceContext = false
    }
    BackHandler(enabled = dateMoveInFlight) {
    }
    BackHandler(enabled = showDetailMetadata && isNoteEditing) {
        persistCurrentNote()
        isNoteEditing = false
        noteFieldFocused = false
        imeBecameVisibleSinceFocus = false
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }
    LaunchedEffect(detailTransitionSession) {
        expanded = false
        closing = false
        sideCardsExpanded = false
        stackGridExpanded = false
        frozenDisplayUri = null
        returnToDayGridAfterClose = false
        stackDragOffsetPx = 0f
        stackSettling = false
        detailCardMotionOptimized = true
        withFrameNanos { }
        expanded = true
    }
    LaunchedEffect(
        detailTransitionSession,
        detailSharedTransitionGroup,
        detailSharedTargetVisible,
        expanded,
        closing,
        displayStampUri
    ) {
        sharedTargetFallbackVisible = false
        if (detailSharedTransitionGroup.isNullOrBlank() || detailSharedTargetVisible || closing) {
            return@LaunchedEffect
        }
        repeat(2) {
            withFrameNanos { }
        }
        delay(180)
        if (!detailSharedTargetVisible && expanded && !closing) {
            Log.e(
                STAMP_SHARED_TRANSITION_LOG_TAG,
                "Shared detail target visibility missed; forcing fallback group=$detailSharedTransitionGroup uri=$displayStampUri"
            )
            sharedTargetFallbackVisible = true
        }
    }
    LaunchedEffect(closing, activeStampUri) {
        if (closing && frozenDisplayUri == null) {
            frozenDisplayUri = activeStampUri
        }
        if (!closing && frozenDisplayUri != null) {
            frozenDisplayUri = null
        }
    }
    LaunchedEffect(expanded, closing, stackPreviewEnabled, detailTransitionSession) {
        if (!expanded || closing || !stackPreviewEnabled) {
            sideCardsExpanded = false
            stackGridExpanded = false
            return@LaunchedEffect
        }
        sideCardsExpanded = !stackGridExpanded
    }
    LaunchedEffect(expanded, closing, detailTransitionSession) {
        if (!expanded || closing) {
            detailCardMotionOptimized = true
            return@LaunchedEffect
        }
        delay((STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS + 90).toLong())
        if (expanded && !closing) {
            detailCardMotionOptimized = false
        }
    }
    LaunchedEffect(stampSourceContext, activeStampUri) {
        if (stampSourceContext == null) {
            showSourceContext = false
        }
    }
    LaunchedEffect(showEmojiPanel, detailState.item.dayKey, activeStampUri, showDetailMetadata) {
        if (!showDetailMetadata) {
            renderEmojiPanel = false
            return@LaunchedEffect
        }
        if (showEmojiPanel) {
            renderEmojiPanel = true
        } else {
            delay(260)
            if (!showEmojiPanel) {
                renderEmojiPanel = false
            }
        }
    }
    LaunchedEffect(showSkinPanel, detailState.item.dayKey, activeStampUri, showDetailMetadata) {
        if (!showDetailMetadata) {
            renderSkinPanel = false
            skinCutoutPreviewInFlight = false
            skinConfirmInFlight = false
            return@LaunchedEffect
        }
        if (showSkinPanel) {
            renderSkinPanel = true
        } else {
            delay(300)
            if (!showSkinPanel) {
                renderSkinPanel = false
                draftSkin = savedSkin
                skinCutoutPreviewInFlight = false
                skinConfirmInFlight = false
            }
        }
    }
    LaunchedEffect(
        showSkinPanel,
        activeStampUri,
        draftSkin.style,
        draftSkin.cutoutAssetPath,
        showDetailMetadata
    ) {
        if (!showDetailMetadata || !showSkinPanel || draftSkin.style != StampCalendarSkinStyle.Cutout) {
            skinCutoutPreviewInFlight = false
            return@LaunchedEffect
        }
        if (activeStampUri.isBlank()) {
            skinCutoutPreviewInFlight = false
            return@LaunchedEffect
        }
        val existingPath = draftSkin.cutoutAssetPath.trim()
        if (existingPath.isNotEmpty() && isReusableStampAutoCutoutAssetPath(existingPath)) {
            skinCutoutPreviewInFlight = false
            return@LaunchedEffect
        }
        skinCutoutPreviewInFlight = true
        val activeUriSnapshot = activeStampUri
        val resolvedPath = ensureStampAutoCutoutAssetPath(
            context = context,
            sourceUriString = activeUriSnapshot,
            existingPath = draftSkin.cutoutAssetPath
        ).orEmpty()
        if (
            showSkinPanel &&
            activeStampUri == activeUriSnapshot &&
            draftSkin.style == StampCalendarSkinStyle.Cutout &&
            resolvedPath.isNotEmpty() &&
            resolvedPath != draftSkin.cutoutAssetPath
        ) {
            draftSkin = draftSkin.copy(cutoutAssetPath = resolvedPath).normalized()
        }
        skinCutoutPreviewInFlight = false
    }
    LaunchedEffect(showDatePanel, detailState.item.dayKey, activeStampUri, showDetailMetadata) {
        if (!showDetailMetadata) {
            renderDatePanel = false
            return@LaunchedEffect
        }
        if (showDatePanel) {
            renderDatePanel = true
        } else {
            delay(300)
            if (!showDatePanel) {
                renderDatePanel = false
            }
        }
    }
    LaunchedEffect(confirmDelete, detailState.item.dayKey, activeStampUri, showDetailMetadata) {
        if (!showDetailMetadata) {
            renderDeleteDialog = false
            confirmDelete = false
            return@LaunchedEffect
        }
        if (!confirmDelete) {
            delay(460)
            if (!confirmDelete) {
                renderDeleteDialog = false
            }
        }
    }
    LaunchedEffect(deleteDialogOpenSignal, showDetailMetadata) {
        if (!showDetailMetadata) return@LaunchedEffect
        if (deleteDialogOpenSignal > 0) {
            renderDeleteDialog = true
            delay(24)
            confirmDelete = true
        }
    }
    LaunchedEffect(
        activeStampUri,
        detailState.item.dayKey,
        detailState.enableStackPreview,
        dayEntry?.selectedUri,
        expanded,
        effectiveDetailSharedTargetVisible
    ) {
        if (!detailState.enableStackPreview) return@LaunchedEffect
        if (!expanded) return@LaunchedEffect
        if (!detailSharedTransitionGroup.isNullOrBlank() && !effectiveDetailSharedTargetVisible) {
            return@LaunchedEffect
        }
        if (activeStampUri !in detailUris) return@LaunchedEffect
        if (dayEntry?.selectedUri == activeStampUri) return@LaunchedEffect
        selectStampForCalendarDay(
            context = context,
            dayKey = detailState.item.dayKey,
            uriString = activeStampUri
        )
    }
    LaunchedEffect(emojiPanelOpenSignal, showDetailMetadata) {
        if (!showDetailMetadata) return@LaunchedEffect
        if (emojiPanelOpenSignal > 0) {
            delay(16)
            showEmojiPanel = true
        }
    }
    LaunchedEffect(skinPanelOpenSignal, showDetailMetadata) {
        if (!showDetailMetadata) return@LaunchedEffect
        if (skinPanelOpenSignal > 0) {
            if (dayEntry?.punchStyleByUri?.get(activeStampUri) == StampPunchStyle.Three.key) {
                showSkinPanel = false
                renderSkinPanel = false
                return@LaunchedEffect
            }
            delay(16)
            showSkinPanel = true
        }
    }
    LaunchedEffect(datePanelOpenSignal, showDetailMetadata) {
        if (!showDetailMetadata) return@LaunchedEffect
        if (datePanelOpenSignal > 0) {
            delay(16)
            showDatePanel = true
        }
    }
    LaunchedEffect(requestCloseSignal, detailState.item.dayKey) {
        if (requestCloseSignal != handledCloseSignal && !closing) {
            handledCloseSignal = requestCloseSignal
            navigateBackFromDetail()
        }
    }
    LaunchedEffect(closing, stackPreviewEnabled, detailTransitionSession) {
        if (closing) {
            stackDragOffsetPx = 0f
            stackSettling = false
            sideCardsExpanded = false
            stackGridExpanded = false
            val hasSharedGroup = !detailSharedTransitionGroup.isNullOrBlank()
            if (hasSharedGroup) {
                onSharedSourceCardHiddenChange(false)
                withFrameNanos { }
                onSharedTargetVisibilityChange(false)
            }
            expanded = false
            if (hasSharedGroup) {
                var sharedTransitionObserved = sharedTransitionScope.isTransitionActive
                var sharedTransitionProbeFrames = 0
                while (!sharedTransitionObserved && sharedTransitionProbeFrames < 10) {
                    withFrameNanos { }
                    sharedTransitionObserved = sharedTransitionScope.isTransitionActive
                    sharedTransitionProbeFrames += 1
                }
                if (sharedTransitionObserved) {
                    while (sharedTransitionScope.isTransitionActive) {
                        withFrameNanos { }
                    }
                    withFrameNanos { }
                } else {
                    delay(STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS.toLong())
                    withFrameNanos { }
                }
            } else {
                delay(STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS.toLong())
            }
            if (returnToDayGridAfterClose) {
                returnToDayGridAfterClose = false
                reopenDayGrid()
            } else {
                onClose()
            }
        }
    }
    LaunchedEffect(detailState.item.dayKey, activeStampUri, note, isNoteEditing, showDetailMetadata) {
        if (!showDetailMetadata) return@LaunchedEffect
        if (isNoteEditing || note == savedNote) return@LaunchedEffect
        updateStampCalendarStampNote(
            context = context,
            dayKey = detailState.item.dayKey,
            uriString = activeStampUri,
            note = note
        )
        savedNote = note
    }
    LaunchedEffect(noteFieldFocused, imeVisible, detailState.item.dayKey, activeStampUri, showDetailMetadata) {
        if (!showDetailMetadata) return@LaunchedEffect
        if (!noteFieldFocused) return@LaunchedEffect
        if (imeVisible) {
            imeBecameVisibleSinceFocus = true
            return@LaunchedEffect
        }
        if (!imeBecameVisibleSinceFocus) {
            return@LaunchedEffect
        }
        delay(90)
        val imeStillVisible = latestImeVisible
        if (!noteFieldFocused || imeStillVisible) {
            return@LaunchedEffect
        }
        persistCurrentNote()
        isNoteEditing = false
        noteFieldFocused = false
        imeBecameVisibleSinceFocus = false
        focusManager.clearFocus(force = true)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        val dynamicDetailAspect = (
            resolveKnownStampAspectRatio(
                displayStampUri,
                optimisticStampBitmaps[displayStampUri]
            ) ?: detailState.aspectRatio
            ).coerceIn(0.55f, 1.45f)
        val stackLockedAspect = remember(detailState.item.dayKey) {
            detailState.aspectRatio.coerceIn(0.55f, 1.45f)
        }
        val detailAspect = when {
            stackPreviewEnabled -> stackLockedAspect
            detailSharedTransitionGroup == currentDayGridSharedGroup -> dynamicDetailAspect
            !detailSharedTransitionGroup.isNullOrBlank() -> stackLockedAspect
            else -> dynamicDetailAspect
        }
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val horizontalPreferredWidth = if (stackPreviewEnabled) {
            (screenWidth * 0.66f).coerceAtMost(362.dp).coerceAtLeast(210.dp)
        } else {
            (screenWidth * 0.5f).coerceAtMost(280.dp).coerceAtLeast(156.dp)
        }
        val maxVisualHeight = if (stackPreviewEnabled) {
            (screenHeight * 0.42f).coerceAtLeast(220.dp)
        } else {
            (screenHeight * 0.28f).coerceAtLeast(150.dp)
        }
        val heightLimitedWidth = maxVisualHeight * detailAspect
        val targetWidth = minOf(horizontalPreferredWidth, heightLimitedWidth)
            .coerceAtMost(
                (screenWidth - if (stackPreviewEnabled) 42.dp else 72.dp)
                    .coerceAtLeast(if (stackPreviewEnabled) 210.dp else 156.dp)
            )
            .coerceAtLeast(if (stackPreviewEnabled) 210.dp else 156.dp)
        val targetHeight = targetWidth / detailAspect
        val targetX = (screenWidth - targetWidth) / 2
        val targetY = if (stackPreviewEnabled) {
            ((screenHeight - targetHeight) * 0.36f).coerceAtLeast(216.dp)
        } else {
            ((screenHeight - targetHeight) * 0.18f).coerceAtLeast(176.dp)
        }
        val useBottomDockTransition = detailState.useBottomDockTransition
        val dockCollapsedWidth = (targetWidth * 0.92f).coerceAtLeast(148.dp)
        val dockCollapsedHeight = dockCollapsedWidth / detailAspect
        val dockCollapsedX = (screenWidth - dockCollapsedWidth) / 2
        val dockCollapsedY = screenHeight + (dockCollapsedHeight * 0.08f) + 14.dp
        val closeAnchorUri = frozenDisplayUri ?: activeStampUri
        val collapsedCardUri = if (closing) closeAnchorUri else detailState.item.uri
        val preferredCollapsedBoundsByUri = if (isDetailFromCalendarDay) {
            dayAllBoundsByUri
        } else {
            emptyMap()
        }
        val preferredCollapsedRotationByUri = if (isDetailFromCalendarDay) {
            dayAllRotationByUri
        } else {
            emptyMap()
        }
        val collapsedSourceBounds = when {
            closing -> preferredCollapsedBoundsByUri[closeAnchorUri]
                ?: preferredCollapsedBoundsByUri[detailState.item.uri]
                ?: sourceBoundsByUri[closeAnchorUri]
                ?: sourceBoundsByUri[detailState.item.uri]
            else -> preferredCollapsedBoundsByUri[detailState.item.uri]
                ?: preferredCollapsedBoundsByUri[displayStampUri]
                ?: sourceBoundsByUri[detailState.item.uri]
                ?: sourceBoundsByUri[displayStampUri]
        }
        val mappedCollapsedX = collapsedSourceBounds?.let { bounds ->
            with(density) { bounds.left.toDp() }
        }
        val mappedCollapsedY = collapsedSourceBounds?.let { bounds ->
            with(density) { bounds.top.toDp() }
        }
        val mappedCollapsedWidth = collapsedSourceBounds?.let { bounds ->
            with(density) { bounds.width.toDp() }
        }?.coerceAtLeast(84.dp)
        val mappedCollapsedRotation = preferredCollapsedRotationByUri[closeAnchorUri]
            ?: preferredCollapsedRotationByUri[detailState.item.uri]
            ?: preferredCollapsedRotationByUri[displayStampUri]
            ?: sourceRotationByUri[closeAnchorUri]
            ?: sourceRotationByUri[detailState.item.uri]
            ?: sourceRotationByUri[displayStampUri]
        val shouldUseLiveCollapsedSource =
            !useBottomDockTransition && collapsedSourceBounds != null
        val collapsedX = when {
            useBottomDockTransition -> dockCollapsedX
            shouldUseLiveCollapsedSource -> mappedCollapsedX ?: detailState.startX
            else -> detailState.startX
        }
        val collapsedY = when {
            useBottomDockTransition -> dockCollapsedY
            shouldUseLiveCollapsedSource -> mappedCollapsedY ?: detailState.startY
            else -> detailState.startY
        }
        val collapsedWidth = when {
            useBottomDockTransition -> dockCollapsedWidth
            shouldUseLiveCollapsedSource -> mappedCollapsedWidth ?: detailState.startWidth.coerceAtLeast(84.dp)
            else -> detailState.startWidth.coerceAtLeast(84.dp)
        }
        val collapsedRotation = when {
            useBottomDockTransition -> 0f
            shouldUseLiveCollapsedSource -> mappedCollapsedRotation ?: detailState.startRotation
            else -> detailState.startRotation
        }
        val sharedCloseRevealsUnderlyingScene = closing && !detailSharedTransitionGroup.isNullOrBlank()
        val sharedCloseGhostMode = sharedCloseRevealsUnderlyingScene
        val sharedCardRotation by animateFloatAsState(
            targetValue = if (expanded) 0f else collapsedRotation,
            animationSpec = tween(
                durationMillis = STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS,
                easing = STAMP_DETAIL_SHARED_TRANSITION_EASING
            ),
            label = "stamp_fs_rotation"
        )
        val detailBackdropAlpha by animateFloatAsState(
            targetValue = if (expanded && !sharedCloseRevealsUnderlyingScene) 1f else 0f,
            animationSpec = tween(
                durationMillis = when {
                    expanded -> 260
                    sharedCloseRevealsUnderlyingScene -> 70
                    else -> 170
                },
                easing = if (expanded) {
                    STAMP_DETAIL_SHARED_TRANSITION_EASING
                } else {
                    LinearEasing
                }
            ),
            label = "stamp_fs_backdrop_alpha"
        )
        val detailSceneOverlayAlpha by animateFloatAsState(
            targetValue = if (expanded && !sharedCloseRevealsUnderlyingScene) 1f else 0f,
            animationSpec = tween(
                durationMillis = when {
                    expanded -> 250
                    sharedCloseRevealsUnderlyingScene -> 60
                    else -> 140
                },
                easing = if (expanded) {
                    FastOutSlowInEasing
                } else {
                    LinearEasing
                }
            ),
            label = "stamp_fs_scene_overlay_alpha"
        )
        val chromeAlpha by animateFloatAsState(
            targetValue = if (expanded && !sharedCloseRevealsUnderlyingScene) 1f else 0f,
            animationSpec = tween(
                durationMillis = when {
                    expanded -> 260
                    sharedCloseRevealsUnderlyingScene -> 80
                    else -> 240
                },
                easing = if (expanded) FastOutSlowInEasing else LinearEasing
            ),
            label = "stamp_fs_alpha"
        )
        val activeStampSkin = if (displayStampUri == activeStampUri) {
            savedSkin.normalized()
        } else {
            stampSkinByUri[displayStampUri].normalized()
        }
        val rawPreviewBitmap = resolveOptimisticStampPreviewBitmap(
            uri = displayStampUri,
            optimisticStampBitmaps = optimisticStampBitmaps,
            stableSeedByUri = stableSeedByUri
        )
        val activeRawPreviewBitmap = if (displayStampUri == activeStampUri) {
            rawPreviewBitmap
        } else {
            resolveOptimisticStampPreviewBitmap(
                uri = activeStampUri,
                optimisticStampBitmaps = optimisticStampBitmaps,
                stableSeedByUri = stableSeedByUri
            )
        }
        val activeStampPunchStyleKey = dayEntry?.punchStyleByUri?.get(activeStampUri)
        val activeStampIsCircular by produceState(
            initialValue = activeStampPunchStyleKey == StampPunchStyle.Three.key ||
                (activeStampPunchStyleKey == null && isCircularStampBitmap(activeRawPreviewBitmap)),
            context,
            activeStampUri,
            activeStampPunchStyleKey,
            activeRawPreviewBitmap
        ) {
            if (activeStampPunchStyleKey != null) {
                value = activeStampPunchStyleKey == StampPunchStyle.Three.key
                return@produceState
            }
            val preview = activeRawPreviewBitmap?.takeUnless { it.isRecycled }
            if (preview != null) {
                value = isCircularStampBitmap(preview)
                return@produceState
            }
            val decoded = runCatching {
                loadStampSourceBitmap(
                    context = context,
                    uri = Uri.parse(activeStampUri),
                    maxSide = 384
                )
            }.getOrNull()
            value = isCircularStampBitmap(decoded)
        }
        LaunchedEffect(activeStampIsCircular) {
            if (activeStampIsCircular) {
                showSkinPanel = false
                renderSkinPanel = false
                skinCutoutPreviewInFlight = false
                skinConfirmInFlight = false
            }
        }
        val effectiveActiveStampSkin = if (activeStampIsCircular) {
            StampCalendarStampSkin(style = StampCalendarSkinStyle.Original)
        } else {
            activeStampSkin
        }
        val previewBitmap = rawPreviewBitmap
        val exportDetailImage: () -> Unit = {
            if (!closing && !exportingDetailImage) {
                persistCurrentNote()
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                isNoteEditing = false
                noteFieldFocused = false
                imeBecameVisibleSinceFocus = false
                showSkinPanel = false
                val exportNote = note.trim()
                exportingDetailImage = true
                scope.launch {
                    val savedUri = withContext(Dispatchers.IO) {
                        val sourceBitmap = rawPreviewBitmap?.takeUnless { it.isRecycled }
                            ?: loadStampSourceBitmap(
                                context = context,
                                uri = Uri.parse(activeStampUri),
                                maxSide = 1800
                            )
                        val exportLogoBitmap = loadStampExportLogoBitmap(context)
                        val rendered = sourceBitmap?.let { bitmap ->
                            renderStampCalendarDetailExportBitmap(
                                context = context,
                                stampBitmap = bitmap,
                                dateLabel = fullDateLabel,
                                emoji = selectedEmoji,
                                note = exportNote,
                                skin = savedSkin,
                                logoBitmap = exportLogoBitmap
                            )
                        } ?: return@withContext null
                        saveStampBitmapToGallery(context, rendered)
                    }
                    exportingDetailImage = false
                    Toast.makeText(
                        context,
                        if (savedUri != null) stampPageStrings.saveSuccess else stampPageStrings.saveFailed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        val collapsedPreviewBitmap = resolveOptimisticStampPreviewBitmap(
            uri = collapsedCardUri,
            optimisticStampBitmaps = optimisticStampBitmaps,
            stableSeedByUri = stableSeedByUri
        )
        val sourceContextAvailable = stampSourceContext != null
        val sourceAspect = stampSourceContext?.sourceAspectRatio?.coerceIn(0.35f, 2.85f) ?: detailAspect
        val sourceMaxWidth = minOf(
            (screenWidth * 0.72f).coerceAtLeast(220.dp),
            (screenWidth - 108.dp).coerceAtLeast(220.dp)
        )
        val sourceMaxHeight = minOf(
            (screenHeight * 0.58f).coerceAtLeast(220.dp),
            (screenHeight - 248.dp).coerceAtLeast(220.dp)
        )
        var sourceTargetWidth = sourceMaxWidth
        var sourceTargetHeight = sourceTargetWidth / sourceAspect
        if (sourceTargetHeight > sourceMaxHeight) {
            sourceTargetHeight = sourceMaxHeight
            sourceTargetWidth = sourceTargetHeight * sourceAspect
        }
        val sourceTargetX = (screenWidth - sourceTargetWidth) / 2
        val sourceTargetY = (screenHeight - sourceTargetHeight) / 2
        val revealSourceContext = sourceContextAvailable && showSourceContext
        val sourceContextProgress by animateFloatAsState(
            targetValue = if (revealSourceContext) 1f else 0f,
            animationSpec = tween(
                durationMillis = 1900,
                easing = LinearEasing
            ),
            label = "stamp_source_context_progress"
        )
        val sourceContextActive =
            sourceContextAvailable && (showSourceContext || sourceContextProgress > 0.001f)
        val centerCardMotionOptimized = detailCardMotionOptimized && !sourceContextActive
        val detailPeripheralAlpha = (detailSceneOverlayAlpha * (1f - sourceContextProgress))
            .coerceIn(0f, 1f)
        val detailChromeAlpha = (chromeAlpha * detailPeripheralAlpha)
            .coerceIn(0f, 1f)
        val sharedGroupCardAlpha = if (detailSharedTransitionGroup.isNullOrBlank()) {
            detailSceneOverlayAlpha
        } else {
            if (effectiveDetailSharedTargetVisible) 1f else 0f
        }
        val animatedX = targetX
        val animatedY = targetY
        val animatedWidth = targetWidth
        val animatedRotation = sharedCardRotation
        val fallbackDetailCardSharedState =
            if (detailSharedTransitionGroup.isNullOrBlank()) {
                with(sharedTransitionScope) {
                    rememberSharedContentState("stamp-detail-card:$detailTransitionSession")
                }
            } else {
                null
            }
        val detailTargetSharedElementModifier = with(sharedTransitionScope) {
            rememberStampSharedElementModifier(
                group = detailSharedTransitionGroup,
                uri = displayStampUri,
                visible = effectiveDetailSharedTargetVisible
            )
        }
        val collapsedStampRect = with(density) {
            RectF(
                animatedX.toPx(),
                animatedY.toPx(),
                animatedX.toPx() + animatedWidth.toPx(),
                animatedY.toPx() + (animatedWidth / detailAspect).toPx()
            )
        }
        val expandedSourceRect = with(density) {
            RectF(
                sourceTargetX.toPx(),
                sourceTargetY.toPx(),
                sourceTargetX.toPx() + sourceTargetWidth.toPx(),
                sourceTargetY.toPx() + sourceTargetHeight.toPx()
            )
        }
        val retargetToDayKey: (String) -> Unit = { targetDayKey ->
            val parsedTargetDate = parseStampCalendarDate(targetDayKey)
            onRetargetDetail(
                detailState.copy(
                    item = detailState.item.copy(
                        day = parsedTargetDate.day,
                        dayKey = targetDayKey,
                        uri = activeStampUri,
                        emoji = selectedEmoji,
                        note = note.trim().takeIf { it.isNotEmpty() }
                    ),
                    startX = animatedX,
                    startY = animatedY,
                    startWidth = animatedWidth,
                    aspectRatio = detailAspect,
                    startRotation = 0f,
                    shadowElevation = 0.dp,
                    sourceCardPadding = STAMP_DETAIL_CARD_PADDING,
                    sourceShadowStrength = 0.96f,
                    enableStackPreview = false,
                    useBottomDockTransition = false,
                    sharedTransitionGroup = null,
                    dayAllScrollOffset = 0,
                    animateDayAllEntrance = false
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .noRippleClickable(enabled = !closing, onClick = {})
        ) {
            if (shouldReturnToDayGrid) {
                StampFullScreenGridPage(
                    sharedTransitionScope = sharedTransitionScope,
                    detailSharedTargetVisible = detailSharedTargetVisible,
                    detailSharedSourceCardHidden = detailSharedSourceCardHidden,
                    detailState = detailState.copy(
                        item = detailState.item.copy(uri = displayStampUri),
                        enableStackPreview = true,
                        sharedTransitionGroup = currentDayGridSharedGroup,
                        animateDayAllEntrance = false
                    ),
                    detailUris = detailUris,
                    requestCloseSignal = requestCloseSignal,
                    copy = copy,
                    optimisticStampBitmaps = optimisticStampBitmaps,
                    sharedTransitionPreviewBitmaps = sharedTransitionPreviewBitmaps,
                    stableSeedByUri = stableSeedByUri,
                    stampSkinByUri = stampSkinByUri,
                    sharedTransitionGroup = currentDayGridSharedGroup,
                    activeDetailUri = displayStampUri,
                    activeDetailSharedGroup = currentDayGridSharedGroup,
                    passiveUnderDetail = true,
                    onStampBoundsChange = onDayAllStampBoundsChange,
                    onSharedTransitionPreviewBitmapResolved = onSharedTransitionPreviewBitmapResolved,
                    onClose = {},
                    onOpenDetail = onOpenDetail,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (!sharedCloseGhostMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .paperGridBackground(
                            backgroundColor = stampAdaptiveColor(
                                light = Color(0xFFF6F2E7),
                                dark = MaterialTheme.colorScheme.background
                            ),
                            lineColor = stampAdaptiveColor(
                                light = Color(0xFFE1DAC9).copy(alpha = 0.44f),
                                dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.11f)
                            )
                        )
                        .graphicsLayer { alpha = detailBackdropAlpha }
                )
            }
            AnimatedVisibility(
                visible = !expanded && detailSharedTransitionGroup.isNullOrBlank(),
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                val collapsedFallbackSharedModifier = with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        sharedContentState = requireNotNull(fallbackDetailCardSharedState),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        boundsTransform = STAMP_DETAIL_SHARED_BOUNDS_TRANSFORM
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = collapsedX, y = collapsedY)
                        .width(collapsedWidth)
                        .aspectRatio(detailAspect)
                        .then(collapsedFallbackSharedModifier)
                        .zIndex(2.6f)
                ) {
                    StampCard(
                        uri = collapsedCardUri,
                        previewBitmap = collapsedPreviewBitmap,
                        skin = stampSkinByUri[collapsedCardUri],
                        categoryEmoji = if (collapsedCardUri == activeStampUri) {
                            selectedEmoji
                        } else {
                            dayEntry?.emojiByUri?.get(collapsedCardUri)
                        },
                        aspectRatio = detailAspect,
                        rotation = sharedCardRotation,
                        shadowStrength = 0.96f,
                        cardPadding = STAMP_DETAIL_CARD_PADDING,
                        motionOptimized = centerCardMotionOptimized,
                        entryRevealEnabled = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (!sharedCloseGhostMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .graphicsLayer { alpha = detailChromeAlpha },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = stampAdaptiveColor(
                            light = Color.White.copy(alpha = 0.88f),
                            dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .noRippleClickable(
                                enabled = !closing &&
                                    detailChromeAlpha > 0.01f &&
                                    !dateMoveInFlight
                            ) {
                                navigateBackFromDetail()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = stampAdaptiveColor(
                                    light = Color(0xFF1C1915),
                                    dark = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = copy.detailTitle,
                        color = stampAdaptiveColor(
                            light = Color(0xFF181512),
                            dark = MaterialTheme.colorScheme.onBackground
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = stampAdaptiveColor(
                                light = Color.White.copy(alpha = 0.88f),
                                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .graphicsLayer {
                                    alpha = if (exportingDetailImage) 0.5f else 1f
                                }
                                .noRippleClickable(
                                    enabled = !closing && detailChromeAlpha > 0.01f && !exportingDetailImage
                                ) {
                                    exportDetailImage()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = stampPageStrings.save,
                                    tint = stampAdaptiveColor(
                                        light = Color(0xFF1C1915),
                                        dark = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = stampAdaptiveColor(
                                light = Color.White.copy(alpha = 0.88f),
                                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .noRippleClickable(enabled = !closing && detailChromeAlpha > 0.01f) {
                                    renderDeleteDialog = true
                                    deleteDialogOpenSignal += 1
                                }
                                .onGloballyPositioned { coordinates ->
                                    deleteTriggerBounds = coordinates.boundsInRoot()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = copy.deleteStamp,
                                    tint = stampAdaptiveColor(
                                        light = Color(0xFF1C1915),
                                        dark = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val sideCardsReveal by animateFloatAsState(
                targetValue = if (
                    stackPreviewEnabled &&
                    sideCardsExpanded &&
                    !sourceContextActive &&
                    !stackGridExpanded
                ) 1f else 0f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "stamp_detail_side_reveal"
            )
            val stackGridProgress by animateFloatAsState(
                targetValue = if (stackPreviewEnabled && stackGridExpanded && !sourceContextActive) 1f else 0f,
                animationSpec = tween(durationMillis = 430, easing = FastOutSlowInEasing),
                label = "stamp_detail_stack_grid_progress"
            )
            val stackSceneAlpha = (1f - stackGridProgress).coerceIn(0f, 1f)
            val sideVisualSafeMargin = if (stackPreviewEnabled) 30.dp else 62.dp
            val maxStrideByBounds = (((screenWidth - animatedWidth) / 2) - sideVisualSafeMargin).coerceAtLeast(22.dp)
            val baseStride = (animatedWidth * if (stackPreviewEnabled) 0.62f else 0.34f)
                .coerceAtLeast(if (stackPreviewEnabled) 86.dp else 42.dp)
            val stackStrideDp = minOf(baseStride, maxStrideByBounds)
            val stackStridePx = with(density) { stackStrideDp.toPx().coerceAtLeast(1f) }
            val maxDragPx = stackStridePx * if (stackPreviewEnabled) 1.35f else 1.14f
            val dragThresholdPx = stackStridePx * if (stackPreviewEnabled) 0.3f else 0.38f
            val renderedDragOffsetPx = stackDragOffsetPx.coerceIn(-maxDragPx, maxDragPx)
            val dragOffsetDp = with(density) { renderedDragOffsetPx.toDp() }
            val dragNormalized = (renderedDragOffsetPx / stackStridePx).coerceIn(-1f, 1f)
            val leftSwipeProgress = (-dragNormalized).coerceIn(0f, 1f)
            val rightSwipeProgress = dragNormalized.coerceIn(0f, 1f)
            val dominantSwipeProgress = if (leftSwipeProgress > rightSwipeProgress) {
                leftSwipeProgress
            } else {
                rightSwipeProgress
            }
            val sideRestScale = 0.93f
            val sideRestAlpha = 0.72f
            val centerStackRotation = animatedRotation + ((6f * rightSwipeProgress) - (6f * leftSwipeProgress))
            val centerStackScale = if (stackPreviewEnabled && !sourceContextActive) {
                1f - ((1f - 0.98f) * dominantSwipeProgress)
            } else {
                1f
            }
            val centerStackAlpha = if (stackPreviewEnabled && !sourceContextActive) {
                1f - ((1f - 0.92f) * dominantSwipeProgress)
            } else {
                1f
            }
            val centerStackShadow = detailState.sourceShadowStrength
            val sharedCardPadding by animateDpAsState(
                targetValue = if (
                    !detailSharedTransitionGroup.isNullOrBlank() &&
                    !sourceContextActive
                ) {
                    if (expanded) {
                        STAMP_DETAIL_CARD_PADDING
                    } else {
                        detailState.sourceCardPadding
                    }
                } else {
                    STAMP_DETAIL_CARD_PADDING
                },
                animationSpec = tween(
                    durationMillis = STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS,
                    easing = STAMP_DETAIL_SHARED_TRANSITION_EASING
                ),
                label = "stamp_detail_shared_card_padding"
            )
            val sharedCardShadowStrength by animateFloatAsState(
                targetValue = if (
                    !detailSharedTransitionGroup.isNullOrBlank() &&
                    !sourceContextActive
                ) {
                    if (expanded) {
                        centerStackShadow
                    } else {
                        detailState.sourceShadowStrength
                    }
                } else {
                    centerStackShadow
                },
                animationSpec = tween(
                    durationMillis = STAMP_DETAIL_SHARED_TRANSITION_DURATION_MS,
                    easing = STAMP_DETAIL_SHARED_TRANSITION_EASING
                ),
                label = "stamp_detail_shared_card_shadow"
            )
            fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp {
                val clamped = fraction.coerceIn(0f, 1f)
                return start + ((end - start) * clamped)
            }
            val animateStackOffsetTo: suspend (Float) -> Unit = { target ->
                val start = stackDragOffsetPx.coerceIn(-maxDragPx, maxDragPx)
                val anim = Animatable(start)
                anim.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = 0.86f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) {
                    stackDragOffsetPx = value
                }
                stackDragOffsetPx = target
            }
            val settleStackTo: (Int) -> Unit = { direction ->
                if (
                    !stackPreviewEnabled ||
                    closing ||
                    stackSettling ||
                    detailUris.size <= 1 ||
                    stackGridExpanded ||
                    stackGridProgress > 0.01f
                ) {
                    Unit
                } else {
                    val normalized = if (direction >= 0) 1 else -1
                    swipeDirection = normalized
                    scope.launch {
                        stackSettling = true
                        val settleTarget = if (normalized > 0) -stackStridePx else stackStridePx
                        animateStackOffsetTo(settleTarget)
                        val currentIdx = detailUris.indexOf(activeStampUriState).takeIf { it >= 0 } ?: 0
                        val nextIndex = ((currentIdx + normalized) % detailUris.size + detailUris.size) % detailUris.size
                        activeStampUriState = detailUris[nextIndex]
                        stackDragOffsetPx = 0f
                        stackSettling = false
                    }
                }
            }
            val handleCenterTap = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                isNoteEditing = false
                noteFieldFocused = false
                imeBecameVisibleSinceFocus = false
                showEmojiPanel = false
                showSkinPanel = false
                if (!stackPreviewEnabled && stampSourceContext != null) {
                    showSourceContext = !showSourceContext
                }
            }
            val centerDragModifier = Modifier.pointerInput(
                detailUris,
                showSourceContext,
                sourceContextActive,
                stackPreviewEnabled,
                closing,
                stackSettling,
                stackGridExpanded,
                stackGridProgress,
                stackStridePx,
                dragThresholdPx,
                maxDragPx
            ) {
                if (
                    !stackPreviewEnabled ||
                    sourceContextActive ||
                    closing ||
                    stackGridExpanded ||
                    stackGridProgress > 0.01f
                ) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        if (stackSettling) return@detectHorizontalDragGestures
                        val next = (stackDragOffsetPx + dragAmount).coerceIn(-maxDragPx, maxDragPx)
                        stackDragOffsetPx = next
                        when {
                            next < -1f -> swipeDirection = 1
                            next > 1f -> swipeDirection = -1
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        if (stackSettling) return@detectHorizontalDragGestures
                        val finalOffset = stackDragOffsetPx
                        val direction = when {
                            finalOffset <= -dragThresholdPx -> 1
                            finalOffset >= dragThresholdPx -> -1
                            else -> 0
                        }
                        scope.launch {
                            stackSettling = true
                            if (direction == 0) {
                                animateStackOffsetTo(0f)
                            } else {
                                swipeDirection = direction
                                val settleTarget = if (direction > 0) -stackStridePx else stackStridePx
                                animateStackOffsetTo(settleTarget)
                                val currentIdx = detailUris.indexOf(activeStampUriState).takeIf { it >= 0 } ?: 0
                                val nextIndex = ((currentIdx + direction) % detailUris.size + detailUris.size) % detailUris.size
                                activeStampUriState = detailUris[nextIndex]
                                stackDragOffsetPx = 0f
                            }
                            stackSettling = false
                        }
                    },
                    onDragCancel = {
                        if (stackSettling) return@detectHorizontalDragGestures
                        scope.launch {
                            stackSettling = true
                            animateStackOffsetTo(0f)
                            stackSettling = false
                        }
                    }
                )
            }
            val centerCardModifier = Modifier
                .offset(
                    x = if (stackPreviewEnabled && !sourceContextActive) {
                        animatedX + dragOffsetDp
                    } else {
                        animatedX
                    },
                    y = animatedY
                )
                .width(animatedWidth)
                .aspectRatio(detailAspect)
                .zIndex(
                    if (
                        stackPreviewEnabled &&
                        !sourceContextActive &&
                        stackSceneAlpha > 0.02f &&
                        dominantSwipeProgress > 0.02f
                    ) {
                        2.2f
                    } else {
                        2.7f
                    }
                )
                .graphicsLayer {
                    alpha = centerStackAlpha * stackSceneAlpha * sharedGroupCardAlpha
                    scaleX = centerStackScale
                    scaleY = centerStackScale
                    clip = false
                }
                .then(
                    if (
                        stackPreviewEnabled &&
                        !sourceContextActive &&
                        !closing &&
                        stackSceneAlpha > 0.01f &&
                        !stackGridExpanded
                    ) {
                        centerDragModifier
                    } else {
                        Modifier
                    }
                )

            if (sourceContextActive && !sharedCloseGhostMode) {
                StampSourceContextExpansionScene(
                    stampUri = displayStampUri,
                    stampPreviewBitmap = rawPreviewBitmap,
                    skin = effectiveActiveStampSkin,
                    stampEmoji = selectedEmoji,
                    sourceContext = requireNotNull(stampSourceContext),
                    progress = sourceContextProgress,
                    collapsedStampRect = collapsedStampRect,
                    expandedSourceRect = expandedSourceRect,
                    collapsedRotation = animatedRotation,
                    stampAspectRatio = detailAspect,
                    onClick = handleCenterTap,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (!sharedCloseGhostMode && stackPreviewEnabled) {
                    val sideCardWidth = (animatedWidth * 0.88f).coerceAtLeast(136.dp)
                    val effectiveStride = stackStrideDp * sideCardsReveal
                    val sideBoost = stackStrideDp * 0.18f
                    val farOffset = stackStrideDp * 1.24f
                    val leftSlotX = animatedX - effectiveStride - sideBoost
                    val rightSlotX = animatedX + effectiveStride + sideBoost
                    val sideCardY = animatedY + (14.dp * (1f - sideCardsReveal))
                    val sideCardClickable = (
                        sideCardsReveal > 0.9f &&
                            !stackSettling &&
                            dominantSwipeProgress < 0.08f &&
                            stackSceneAlpha > 0.98f &&
                            !stackGridExpanded
                        )
                    val sideBaseAlpha = (sideRestAlpha * sideCardsReveal).coerceIn(0f, sideRestAlpha)
                    if (detailUris.size >= 3 && previousStampUri != null && nextStampUri != null) {
                        val leftX = when {
                            rightSwipeProgress > 0f -> lerpDp(leftSlotX, animatedX, rightSwipeProgress)
                            leftSwipeProgress > 0f -> lerpDp(leftSlotX, leftSlotX - farOffset, leftSwipeProgress)
                            else -> leftSlotX
                        }
                        val rightX = when {
                            leftSwipeProgress > 0f -> lerpDp(rightSlotX, animatedX, leftSwipeProgress)
                            rightSwipeProgress > 0f -> lerpDp(rightSlotX, rightSlotX + farOffset, rightSwipeProgress)
                            else -> rightSlotX
                        }
                        val leftAlpha = when {
                            rightSwipeProgress > 0f ->
                                (sideBaseAlpha + (0.18f * rightSwipeProgress)).coerceIn(0f, 1f)
                            leftSwipeProgress > 0f ->
                                (sideBaseAlpha * (1f - (0.92f * leftSwipeProgress))).coerceIn(0f, 1f)
                            else -> sideBaseAlpha
                        }
                        val rightAlpha = when {
                            leftSwipeProgress > 0f ->
                                (sideBaseAlpha + (0.18f * leftSwipeProgress)).coerceIn(0f, 1f)
                            rightSwipeProgress > 0f ->
                                (sideBaseAlpha * (1f - (0.92f * rightSwipeProgress))).coerceIn(0f, 1f)
                            else -> sideBaseAlpha
                        }
                        val leftScale = when {
                            rightSwipeProgress > 0f -> sideRestScale + ((1f - sideRestScale) * rightSwipeProgress)
                            else -> sideRestScale
                        }
                        val rightScale = when {
                            leftSwipeProgress > 0f -> sideRestScale + ((1f - sideRestScale) * leftSwipeProgress)
                            else -> sideRestScale
                        }
                        val leftCardRotation = if (rightSwipeProgress > 0f) {
                            -6f * (1f - rightSwipeProgress)
                        } else {
                            -6f
                        }
                        val rightCardRotation = if (leftSwipeProgress > 0f) {
                            6f * (1f - leftSwipeProgress)
                        } else {
                            6f
                        }
                        key(previousStampUri.hashCode()) {
                            StampCard(
                                uri = previousStampUri,
                                previewBitmap = resolveOptimisticStampPreviewBitmap(
                                    uri = previousStampUri,
                                    optimisticStampBitmaps = optimisticStampBitmaps,
                                    stableSeedByUri = stableSeedByUri
                                ),
                                skin = stampSkinByUri[previousStampUri],
                                categoryEmoji = previousStampUri?.let { dayEntry?.emojiByUri?.get(it) },
                                aspectRatio = detailAspect,
                                rotation = leftCardRotation,
                                shadowStrength = 0.92f,
                                entryRevealEnabled = false,
                                onClick = if (sideCardClickable) {
                                    { settleStackTo(-1) }
                                } else {
                                    null
                                },
                                modifier = Modifier
                                    .offset(x = leftX, y = sideCardY)
                                    .width(sideCardWidth)
                                    .aspectRatio(detailAspect)
                                    .zIndex(if (rightSwipeProgress > 0f) 3f else 1f)
                                    .graphicsLayer {
                                        alpha = leftAlpha * stackSceneAlpha * detailPeripheralAlpha
                                        scaleX = leftScale
                                        scaleY = leftScale
                                        clip = false
                                    }
                            )
                        }
                        key(nextStampUri.hashCode()) {
                            StampCard(
                                uri = nextStampUri,
                                previewBitmap = resolveOptimisticStampPreviewBitmap(
                                    uri = nextStampUri,
                                    optimisticStampBitmaps = optimisticStampBitmaps,
                                    stableSeedByUri = stableSeedByUri
                                ),
                                skin = stampSkinByUri[nextStampUri],
                                categoryEmoji = nextStampUri?.let { dayEntry?.emojiByUri?.get(it) },
                                aspectRatio = detailAspect,
                                rotation = rightCardRotation,
                                shadowStrength = 0.92f,
                                entryRevealEnabled = false,
                                onClick = if (sideCardClickable) {
                                    { settleStackTo(1) }
                                } else {
                                    null
                                },
                                modifier = Modifier
                                    .offset(x = rightX, y = sideCardY)
                                    .width(sideCardWidth)
                                    .aspectRatio(detailAspect)
                                    .zIndex(if (leftSwipeProgress > 0f) 3f else 1f)
                                    .graphicsLayer {
                                        alpha = rightAlpha * stackSceneAlpha * detailPeripheralAlpha
                                        scaleX = rightScale
                                        scaleY = rightScale
                                        clip = false
                                    }
                            )
                        }
                        if (leftSwipeProgress > 0f && next2StampUri != null) {
                            val fillerX = lerpDp(rightSlotX + farOffset, rightSlotX, leftSwipeProgress)
                            StampCard(
                                uri = next2StampUri,
                                previewBitmap = resolveOptimisticStampPreviewBitmap(
                                    uri = next2StampUri,
                                    optimisticStampBitmaps = optimisticStampBitmaps,
                                    stableSeedByUri = stableSeedByUri
                                ),
                                skin = stampSkinByUri[next2StampUri],
                                categoryEmoji = next2StampUri?.let { dayEntry?.emojiByUri?.get(it) },
                                aspectRatio = detailAspect,
                                rotation = 8f,
                                shadowStrength = 0.88f,
                                entryRevealEnabled = false,
                                onClick = null,
                                modifier = Modifier
                                    .offset(x = fillerX, y = sideCardY)
                                    .width(sideCardWidth)
                                    .aspectRatio(detailAspect)
                                    .zIndex(0.7f)
                                    .graphicsLayer {
                                        alpha = (
                                            (sideBaseAlpha * leftSwipeProgress).coerceIn(0f, 0.78f) *
                                                stackSceneAlpha *
                                                detailPeripheralAlpha
                                            )
                                        scaleX = 0.98f
                                        scaleY = 0.98f
                                        clip = false
                                    }
                            )
                        }
                        if (rightSwipeProgress > 0f && previous2StampUri != null) {
                            val fillerX = lerpDp(leftSlotX - farOffset, leftSlotX, rightSwipeProgress)
                            StampCard(
                                uri = previous2StampUri,
                                previewBitmap = resolveOptimisticStampPreviewBitmap(
                                    uri = previous2StampUri,
                                    optimisticStampBitmaps = optimisticStampBitmaps,
                                    stableSeedByUri = stableSeedByUri
                                ),
                                skin = stampSkinByUri[previous2StampUri],
                                categoryEmoji = previous2StampUri?.let { dayEntry?.emojiByUri?.get(it) },
                                aspectRatio = detailAspect,
                                rotation = -8f,
                                shadowStrength = 0.88f,
                                entryRevealEnabled = false,
                                onClick = null,
                                modifier = Modifier
                                    .offset(x = fillerX, y = sideCardY)
                                    .width(sideCardWidth)
                                    .aspectRatio(detailAspect)
                                    .zIndex(0.7f)
                                    .graphicsLayer {
                                        alpha = (
                                            (sideBaseAlpha * rightSwipeProgress).coerceIn(0f, 0.78f) *
                                                stackSceneAlpha *
                                                detailPeripheralAlpha
                                            )
                                        scaleX = 0.98f
                                        scaleY = 0.98f
                                        clip = false
                                    }
                            )
                        }
                    } else {
                        val alternateUri = detailUris.firstOrNull { it != activeStampUri }
                        val renderOnRight = when {
                            leftSwipeProgress > 0f -> true
                            rightSwipeProgress > 0f -> false
                            else -> swipeDirection < 0
                        }
                        val alternateX = when {
                            leftSwipeProgress > 0f -> lerpDp(rightSlotX, animatedX, leftSwipeProgress)
                            rightSwipeProgress > 0f -> lerpDp(leftSlotX, animatedX, rightSwipeProgress)
                            renderOnRight -> rightSlotX
                            else -> leftSlotX
                        }
                        val alternateProgress = if (leftSwipeProgress > 0f) {
                            leftSwipeProgress
                        } else {
                            rightSwipeProgress
                        }
                        StampCard(
                            uri = alternateUri,
                            previewBitmap = resolveOptimisticStampPreviewBitmap(
                                uri = alternateUri,
                                optimisticStampBitmaps = optimisticStampBitmaps,
                                stableSeedByUri = stableSeedByUri
                            ),
                            skin = alternateUri?.let { stampSkinByUri[it] },
                            categoryEmoji = alternateUri?.let { dayEntry?.emojiByUri?.get(it) },
                            aspectRatio = detailAspect,
                            rotation = if (renderOnRight) {
                                6f * (1f - alternateProgress)
                            } else {
                                -6f * (1f - alternateProgress)
                            },
                            shadowStrength = 1f,
                            entryRevealEnabled = false,
                            onClick = if (sideCardClickable) {
                                { settleStackTo(if (renderOnRight) 1 else -1) }
                            } else {
                                null
                            },
                            modifier = Modifier
                                .offset(
                                    x = alternateX,
                                    y = sideCardY
                                )
                                .width(sideCardWidth)
                                .aspectRatio(detailAspect)
                                .zIndex(if (alternateProgress > 0f) 3f else 1f)
                                .graphicsLayer {
                                    alpha = (
                                        (sideBaseAlpha + (0.18f * alternateProgress)).coerceIn(0f, 1f) *
                                            stackSceneAlpha *
                                            detailPeripheralAlpha
                                        )
                                    scaleX = 0.985f + (0.015f * alternateProgress)
                                    scaleY = 0.985f + (0.015f * alternateProgress)
                                    clip = false
                                }
                        )
                    }
                }
                if (
                    !sharedCloseGhostMode &&
                    stackPreviewEnabled &&
                    !sourceContextActive &&
                    stackGridProgress > 0.001f
                ) {
                    val gridColumns = 2
                    val gridRows = ((detailUris.size + (gridColumns - 1)) / gridColumns).coerceAtLeast(1)
                    val gridGapX = 16.dp
                    val gridGapY = 14.dp
                    val gridTopBase = animatedY + (animatedWidth / detailAspect) + 28.dp
                    val availableGridHeight = (screenHeight - gridTopBase - 104.dp).coerceAtLeast(132.dp)
                    val widthByColumns = ((screenWidth - 56.dp - gridGapX) / 2f).coerceAtLeast(112.dp)
                    val maxCardHeight = (
                        (availableGridHeight - (gridGapY * (gridRows - 1))).coerceAtLeast(96.dp)
                        ) / gridRows.toFloat()
                    val widthByHeight = (maxCardHeight * detailAspect).coerceAtLeast(96.dp)
                    val gridCardWidth = minOf(widthByColumns, widthByHeight).coerceAtLeast(96.dp)
                    val gridCardHeight = gridCardWidth / detailAspect
                    val totalGridWidth = (gridCardWidth * 2f) + gridGapX
                    val gridOriginX = (screenWidth - totalGridWidth) / 2f
                    val activeGridIndex = detailUris.indexOf(activeStampUriState).takeIf { it >= 0 } ?: 0
                    detailUris.forEachIndexed { index, uri ->
                        val column = index % gridColumns
                        val row = index / gridColumns
                        val targetGridX = gridOriginX + ((gridCardWidth + gridGapX) * column)
                        val targetGridY = gridTopBase + ((gridCardHeight + gridGapY) * row)
                        var relative = index - activeGridIndex
                        val halfSize = detailUris.size / 2
                        if (relative > halfSize) relative -= detailUris.size
                        if (relative < -halfSize) relative += detailUris.size
                        val clampedRelative = relative.coerceIn(-2, 2).toFloat()
                        val startStackX = animatedX + (stackStrideDp * clampedRelative)
                        val startStackY = animatedY + (10.dp * abs(clampedRelative))
                        val cardX = lerpDp(startStackX, targetGridX, stackGridProgress)
                        val cardY = lerpDp(startStackY, targetGridY, stackGridProgress)
                        val cardWidth = lerpDp(animatedWidth * 0.9f, gridCardWidth, stackGridProgress)
                        val initialRotation = when {
                            clampedRelative < 0f -> -6f
                            clampedRelative > 0f -> 6f
                            else -> centerStackRotation
                        }
                        val cardRotation = lerpFloat(initialRotation, 0f, stackGridProgress)
                        val cardScale = (0.98f + (0.02f * stackGridProgress)).coerceAtMost(1f)
                        StampCard(
                            uri = uri,
                            previewBitmap = resolveOptimisticStampPreviewBitmap(
                                uri = uri,
                                optimisticStampBitmaps = optimisticStampBitmaps,
                                stableSeedByUri = stableSeedByUri
                            ),
                            skin = stampSkinByUri[uri],
                            categoryEmoji = dayEntry?.emojiByUri?.get(uri),
                            aspectRatio = detailAspect,
                            rotation = cardRotation,
                            shadowStrength = 0.94f,
                            entryRevealEnabled = false,
                            onClick = if (
                                stackGridExpanded &&
                                stackGridProgress > 0.98f &&
                                !stackSettling &&
                                !closing
                            ) {
                                {
                                    activeStampUriState = uri
                                    stackGridExpanded = false
                                    stackDragOffsetPx = 0f
                                }
                            } else {
                                null
                            },
                            modifier = Modifier
                                .offset(x = cardX, y = cardY)
                                .width(cardWidth)
                                .aspectRatio(detailAspect)
                                .zIndex(3f + (stackGridProgress * 0.8f))
                                .graphicsLayer {
                                    alpha = stackGridProgress * detailPeripheralAlpha
                                    scaleX = cardScale
                                    scaleY = cardScale
                                    clip = false
                                }
                        )
                    }
                }
                if (detailSharedTransitionGroup.isNullOrBlank()) {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ) {
                        key(displayStampUri.hashCode()) {
                            val fallbackDetailCardSharedModifier = with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = requireNotNull(fallbackDetailCardSharedState),
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                    boundsTransform = STAMP_DETAIL_SHARED_BOUNDS_TRANSFORM
                                )
                            }
                            Box(
                                modifier = centerCardModifier
                                    .then(fallbackDetailCardSharedModifier)
                            ) {
                                StampCard(
                                    uri = displayStampUri,
                                    previewBitmap = previewBitmap,
                                    skin = if (displayStampUri == activeStampUri) {
                                        savedSkin
                                    } else {
                                        stampSkinByUri[displayStampUri]
                                    },
                                    categoryEmoji = if (displayStampUri == activeStampUri) selectedEmoji else dayEntry?.emojiByUri?.get(displayStampUri),
                                    aspectRatio = detailAspect,
                                    rotation = if (stackPreviewEnabled && !sourceContextActive) {
                                        centerStackRotation
                                    } else {
                                        animatedRotation
                                    },
                                    shadowStrength = sharedCardShadowStrength,
                                    cardPadding = sharedCardPadding,
                                    motionOptimized = centerCardMotionOptimized,
                                    entryRevealEnabled = false,
                                    onResolvedPreviewBitmap = if (detailSharedTransitionGroup.isNullOrBlank()) {
                                        null
                                    } else {
                                        { bitmap ->
                                            onSharedTransitionPreviewBitmapResolved(displayStampUri, bitmap)
                                        }
                                    },
                                    onClick = if (closing || (stackPreviewEnabled && stackSceneAlpha <= 0.02f)) {
                                        null
                                    } else {
                                        handleCenterTap
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else {
                    key(displayStampUri.hashCode()) {
                        Box(
                            modifier = centerCardModifier
                                .then(detailTargetSharedElementModifier)
                        ) {
                            StampCard(
                                uri = displayStampUri,
                                previewBitmap = previewBitmap,
                                skin = if (displayStampUri == activeStampUri) {
                                        savedSkin
                                    } else {
                                        stampSkinByUri[displayStampUri]
                                    },
                                    categoryEmoji = if (displayStampUri == activeStampUri) selectedEmoji else dayEntry?.emojiByUri?.get(displayStampUri),
                                    aspectRatio = detailAspect,
                                rotation = if (stackPreviewEnabled && !sourceContextActive) {
                                    centerStackRotation
                                } else {
                                    animatedRotation
                                },
                                shadowStrength = sharedCardShadowStrength,
                                cardPadding = sharedCardPadding,
                                motionOptimized = centerCardMotionOptimized,
                                entryRevealEnabled = false,
                                onResolvedPreviewBitmap = { bitmap ->
                                    onSharedTransitionPreviewBitmapResolved(displayStampUri, bitmap)
                                },
                                onClick = if (closing || (stackPreviewEnabled && stackSceneAlpha <= 0.02f)) {
                                    null
                                } else {
                                    handleCenterTap
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                if (
                    !sharedCloseGhostMode &&
                    stackPreviewEnabled &&
                    !sourceContextActive &&
                    detailChromeAlpha > 0.01f &&
                    !closing
                ) {
                    val stackButtonBaseY = animatedY + (animatedWidth / detailAspect) + 26.dp
                    val expandButtonVisible = stackGridProgress < 0.02f
                    val collapseButtonVisible = stackGridProgress > 0.02f
                    if (expandButtonVisible) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = stampAdaptiveColor(
                                light = Color.White.copy(alpha = 0.9f),
                                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                            ),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = stackButtonBaseY)
                                .graphicsLayer { alpha = detailChromeAlpha * (1f - stackGridProgress) }
                                .noRippleClickable {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    showEmojiPanel = false
                                    stackGridExpanded = true
                                }
                        ) {
                            Text(
                                text = "\u5c55\u5f00",
                                color = stampAdaptiveColor(
                                    light = Color(0xFF181512),
                                    dark = MaterialTheme.colorScheme.onSurface
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                            )
                        }
                    }
                    if (collapseButtonVisible) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = stampAdaptiveColor(
                                light = Color.White.copy(alpha = 0.92f),
                                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f)
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 18.dp)
                                .graphicsLayer { alpha = detailChromeAlpha * stackGridProgress }
                                .noRippleClickable {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    stackGridExpanded = false
                                }
                        ) {
                            Text(
                                text = "\u5806\u53e0",
                                color = stampAdaptiveColor(
                                    light = Color(0xFF181512),
                                    dark = MaterialTheme.colorScheme.onSurface
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
                if (
                    !sharedCloseGhostMode &&
                    showDetailMetadata &&
                    !sourceContextActive
                ) {
                    val detailInfoRowY = animatedY + (animatedWidth / detailAspect) + 18.dp
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = detailInfoRowY)
                            .graphicsLayer { alpha = detailChromeAlpha },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    dateTriggerBounds = coordinates.boundsInRoot()
                                }
                                .noRippleClickable(
                                    enabled = !closing &&
                                        detailChromeAlpha > 0.01f &&
                                        !sourceContextActive &&
                                        !dateMoveInFlight
                                ) {
                                    if (hapticsAllowed) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    persistCurrentNote()
                                    isNoteEditing = false
                                    noteFieldFocused = false
                                    imeBecameVisibleSinceFocus = false
                                    showEmojiPanel = false
                                    showSkinPanel = false
                                    renderEmojiPanel = false
                                    if (showDatePanel) {
                                        showDatePanel = false
                                    } else {
                                        renderDatePanel = true
                                        datePanelOpenSignal += 1
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = fullDateLabel,
                                color = stampAdaptiveColor(
                                    light = Color(0xFF181512),
                                    dark = MaterialTheme.colorScheme.onBackground
                                ),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.onSizeChanged { size ->
                                    dateLabelWidthPx = size.width.toFloat()
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .width(
                                        with(density) {
                                            if (dateLabelWidthPx > 0f) dateLabelWidthPx.toDp() else 92.dp
                                        }
                                    )
                                    .height(1.dp)
                                    .drawBehind {
                                        drawLine(
                                            color = dateUnderlineColor,
                                            start = Offset(0f, size.height / 2f),
                                            end = Offset(size.width, size.height / 2f),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                                        )
                                    }
                            )
                        }
                        if (showDayCoverAction) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = dayCoverBgColor,
                                modifier = Modifier
                                    .graphicsLayer {
                                        val scale = 1f + (0.025f * dayCoverSelectionProgress)
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .noRippleClickable(enabled = !closing && detailChromeAlpha > 0.01f) {
                                        if (!isDayCoverSelected) {
                                            if (hapticsAllowed) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            selectStampForCalendarDay(
                                                context = context,
                                                dayKey = detailState.item.dayKey,
                                                uriString = activeStampUri
                                            )
                                        }
                                    }
                                    .animateContentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            tint = dayCoverTextColor,
                                            modifier = Modifier.graphicsLayer {
                                                alpha = (1f - dayCoverSelectionProgress).coerceIn(0f, 1f)
                                            }
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = dayCoverTextColor,
                                            modifier = Modifier.graphicsLayer {
                                                alpha = dayCoverSelectionProgress
                                                val checkScale = 0.72f + (0.28f * dayCoverSelectionProgress)
                                                scaleX = checkScale
                                                scaleY = checkScale
                                            }
                                        )
                                    }
                                    Text(
                                        text = dayCoverLabel(appLanguage, isDayCoverSelected, isViewingTodayDay),
                                        color = dayCoverTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val noteBarBottomPadding by animateDpAsState(
                targetValue = if (isNoteEditing) 126.dp else 110.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "stamp_detail_note_bottom"
            )
            if (!sharedCloseGhostMode && showDetailMetadata) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = noteBarBottomPadding)
                        .graphicsLayer { alpha = detailChromeAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StampNoteField(
                        value = note,
                        placeholder = copy.notePlaceholder,
                        onValueChange = { nextValue -> note = nextValue },
                        onFocusChanged = { focused ->
                            noteFieldFocused = focused
                            isNoteEditing = focused
                            if (focused) {
                                showEmojiPanel = false
                                showSkinPanel = false
                                imeBecameVisibleSinceFocus = imeVisible
                            } else {
                                persistCurrentNote()
                                imeBecameVisibleSinceFocus = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    val saveButtonScale by animateFloatAsState(
                        targetValue = if (isNoteEditing) 1f else 0.88f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "stamp_detail_save_scale"
                    )
                    val saveButtonAlpha by animateFloatAsState(
                        targetValue = if (isNoteEditing) 1f else 0f,
                        animationSpec = tween(durationMillis = 160),
                        label = "stamp_detail_save_alpha"
                    )
                    val saveButtonSize by animateDpAsState(
                        targetValue = if (isNoteEditing) 44.dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "stamp_detail_save_size"
                    )
                    Surface(
                        shape = CircleShape,
                        color = stampAdaptiveColor(
                            light = Color.White.copy(alpha = 0.96f),
                            dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
                        ),
                        modifier = Modifier
                            .padding(start = if (isNoteEditing) 10.dp else 0.dp)
                            .size(saveButtonSize)
                            .graphicsLayer {
                                alpha = saveButtonAlpha
                                scaleX = saveButtonScale
                                scaleY = saveButtonScale
                            }
                            .clip(CircleShape)
                            .noRippleClickable(enabled = isNoteEditing) {
                                persistCurrentNote()
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                isNoteEditing = false
                                noteFieldFocused = false
                                imeBecameVisibleSinceFocus = false
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Save note",
                                tint = stampAdaptiveColor(
                                    light = Color(0xFF181512),
                                    dark = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                }
            }
            if (!sharedCloseGhostMode && showDetailMetadata) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            bottom = STAMP_DETAIL_PICKER_ROW_BOTTOM_PADDING
                        )
                        .graphicsLayer { alpha = detailChromeAlpha },
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 12.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                EmojiPickerTrigger(
                    emoji = selectedEmoji,
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        isNoteEditing = false
                        showSkinPanel = false
                        if (showEmojiPanel) {
                            showEmojiPanel = false
                        } else {
                            renderEmojiPanel = true
                            emojiPanelOpenSignal += 1
                        }
                    },
                    modifier = Modifier
                        .width(STAMP_DETAIL_PICKER_TRIGGER_WIDTH)
                        .onGloballyPositioned { coordinates ->
                            emojiTriggerBounds = coordinates.boundsInRoot()
                        }
                )
                if (!activeStampIsCircular) {
                    StampSkinPickerTrigger(
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            isNoteEditing = false
                            noteFieldFocused = false
                            imeBecameVisibleSinceFocus = false
                            showEmojiPanel = false
                            if (showSkinPanel) {
                                showSkinPanel = false
                            } else {
                                draftSkin = savedSkin
                                renderSkinPanel = true
                                skinPanelOpenSignal += 1
                            }
                        },
                        modifier = Modifier
                            .width(STAMP_DETAIL_PICKER_TRIGGER_WIDTH)
                            .onGloballyPositioned { coordinates ->
                                skinTriggerBounds = coordinates.boundsInRoot()
                            }
                    )
                }
                }
            }

            if (!sharedCloseGhostMode && showDetailMetadata && renderEmojiPanel) {
                EmojiPickerPanel(
                    visible = showEmojiPanel,
                    title = copy.emojiPickerTitle,
                    clearLabel = copy.clearEmoji,
                    selectedEmoji = selectedEmoji,
                    anchorBounds = emojiTriggerBounds,
                    onDismiss = { showEmojiPanel = false },
                    onEmojiSelected = { emoji ->
                        selectedEmoji = emoji
                        updateStampCalendarStampEmoji(
                            context = context,
                            dayKey = detailState.item.dayKey,
                            uriString = activeStampUri,
                            emoji = emoji
                        )
                        showEmojiPanel = false
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                )
            }
            if (!sharedCloseGhostMode && showDetailMetadata && renderSkinPanel && !activeStampIsCircular) {
                StampSkinPickerPanel(
                    visible = showSkinPanel,
                    copy = skinPanelCopy,
                    currentSkin = savedSkin,
                    draftSkin = draftSkin,
                    shapeLockedToCutout = cutoutShapeLocked,
                    previewUri = activeStampUri,
                    previewBitmap = rawPreviewBitmap,
                    previewEmoji = selectedEmoji,
                    previewCutoutPreparing = skinCutoutPreviewInFlight,
                    confirmInFlight = skinConfirmInFlight,
                    previewAspectRatio = detailAspect,
                    anchorBounds = skinTriggerBounds,
                    onDismiss = { showSkinPanel = false },
                    onDraftChange = {
                        if (!skinConfirmInFlight) {
                            val nextSkin = it.normalized()
                            if (cutoutShapeLocked && nextSkin.style != StampCalendarSkinStyle.Cutout) {
                                return@StampSkinPickerPanel
                            }
                            draftSkin = if (cutoutShapeLocked) {
                                nextSkin.copy(
                                    style = StampCalendarSkinStyle.Cutout,
                                    numberLabel = "",
                                    title = "",
                                    subtitle = ""
                                ).normalized()
                            } else {
                                nextSkin
                            }
                        }
                    },
                    onConfirm = {
                        if (skinConfirmInFlight) return@StampSkinPickerPanel
                        if (!isStampCampfireFeatureActive(context)) {
                            showSkinCampfireGateDialog = true
                            return@StampSkinPickerPanel
                        }
                        if (hapticsAllowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        skinConfirmInFlight = true
                        scope.launch {
                            try {
                                var finalDraftSkin = if (cutoutShapeLocked) {
                                    draftSkin.copy(
                                        style = StampCalendarSkinStyle.Cutout,
                                        numberLabel = "",
                                        title = "",
                                        subtitle = ""
                                    ).normalized()
                                } else {
                                    draftSkin.normalized()
                                }
                                if (finalDraftSkin.style == StampCalendarSkinStyle.Cutout) {
                                    val cutoutPath = ensureStampAutoCutoutAssetPath(
                                        context = context,
                                        sourceUriString = activeStampUri,
                                        existingPath = finalDraftSkin.cutoutAssetPath
                                    ).orEmpty()
                                    finalDraftSkin = finalDraftSkin.copy(cutoutAssetPath = cutoutPath).normalized()
                                }
                                draftSkin = finalDraftSkin
                                persistDraftSkin()
                                if (cutoutShapeLocked) {
                                    updateStampCalendarStampCutoutLock(
                                        context = context,
                                        dayKey = detailState.item.dayKey,
                                        uriString = activeStampUri,
                                        locked = true
                                    )
                                }
                                showSkinPanel = false
                            } finally {
                                skinConfirmInFlight = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                )
            }
            if (showSkinCampfireGateDialog) {
                StampSkinCampfireGateDialog(
                    message = skinPanelCopy.campfireGateMessage,
                    learnLabel = skinPanelCopy.campfireGateLearnLabel,
                    cancelLabel = skinPanelCopy.cancelLabel,
                    onLearnClick = {
                        showSkinCampfireGateDialog = false
                        showSkinPanel = false
                        onCampfireRequested?.invoke()
                    },
                    onDismiss = { showSkinCampfireGateDialog = false }
                )
            }
            if (!sharedCloseGhostMode && showDetailMetadata && renderDatePanel) {
                StampDatePickerPanel(
                    visible = showDatePanel,
                    title = copy.dateChangeTitle,
                    currentDayKey = detailState.item.dayKey,
                    openSignal = datePanelOpenSignal,
                    confirmLabel = resolveStampDateConfirmLabel(appLanguage, copy.dateChangeConfirm),
                    cancelLabel = copy.dateChangeCancel,
                    anchorBounds = dateTriggerBounds,
                    onDismiss = {
                        if (!dateMoveInFlight) {
                            showDatePanel = false
                        }
                    },
                    onConfirm = { targetDayKey ->
                        if (dateMoveInFlight) return@StampDatePickerPanel
                        if (targetDayKey == detailState.item.dayKey) {
                            showDatePanel = false
                            return@StampDatePickerPanel
                        }
                        if (hapticsAllowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        persistCurrentNote()
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        isNoteEditing = false
                        noteFieldFocused = false
                        imeBecameVisibleSinceFocus = false
                        showSkinPanel = false
                        dateMoveInFlight = true
                        scope.launch {
                            delay(280)
                            moveStampToCalendarDay(
                                context = context,
                                fromDayKey = detailState.item.dayKey,
                                toDayKey = targetDayKey,
                                uriString = activeStampUri
                            )
                            retargetToDayKey(targetDayKey)
                            showDatePanel = false
                            dateMoveInFlight = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                )
            }
        }

        if (!sharedCloseGhostMode && renderDeleteDialog) {
            DeleteConfirmPanel(
                visible = confirmDelete,
                title = copy.deleteConfirmTitle,
                body = copy.deleteConfirmBody,
                confirmLabel = copy.deleteConfirmAction,
                cancelLabel = copy.deleteCancelAction,
                anchorBounds = deleteTriggerBounds,
                onDismiss = { confirmDelete = false },
                onConfirm = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    confirmDelete = false
                    onDelete(detailState.item.copy(uri = activeStampUri))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            )
        }
    }
}

@Composable
private fun StampSourceContextExpansionScene(
    stampUri: String,
    stampPreviewBitmap: Bitmap?,
    skin: StampCalendarStampSkin? = null,
    stampEmoji: String? = null,
    sourceContext: StampSourceContextEntry,
    progress: Float,
    collapsedStampRect: RectF,
    expandedSourceRect: RectF,
    collapsedRotation: Float,
    stampAspectRatio: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clampedProgress = progress.coerceIn(0f, 1f)
    val sourceModel = remember(sourceContext.sourceUri) {
        runCatching { Uri.parse(sourceContext.sourceUri) }.getOrNull() ?: sourceContext.sourceUri
    }
    val sourcePainter = rememberAsyncImagePainter(model = sourceModel)
    val fallbackSourceBitmap by produceState<Bitmap?>(
        initialValue = null,
        context,
        sourceContext.sourceUri
    ) {
        value = withContext(Dispatchers.IO) {
            decodeStampSourceContextFileBitmap(sourceContext.sourceUri)
        }
    }
    val transitionProgress = clampedProgress
    val appearSplit = 0.08f
    val holdSplit = 0.61f
    val appearProgressRaw = (transitionProgress / appearSplit).coerceIn(0f, 1f)
    val appearProgress = STAMP_SOURCE_APPEAR_EASING.transform(appearProgressRaw)
    val scaleProgressRaw = ((transitionProgress - holdSplit) / (1f - holdSplit)).coerceIn(0f, 1f)
    val scaleProgress = STAMP_SOURCE_SCALE_EASING.transform(scaleProgressRaw)
    val stampLeftFraction = sourceContext.stampLeftFraction.coerceIn(0f, 1f)
    val stampTopFraction = sourceContext.stampTopFraction.coerceIn(0f, 1f)
    val stampRightFraction = sourceContext.stampRightFraction
        .coerceIn(stampLeftFraction + 0.0001f, 1f)
    val stampBottomFraction = sourceContext.stampBottomFraction
        .coerceIn(stampTopFraction + 0.0001f, 1f)
    val stampWidthFraction = (stampRightFraction - stampLeftFraction)
        .coerceAtLeast(0.0001f)
    val anchorSourceWidth = collapsedStampRect.width() / stampWidthFraction
    val anchorSourceHeight = anchorSourceWidth / sourceContext.sourceAspectRatio.coerceAtLeast(0.0001f)
    val anchorSourceRect = RectF(
        collapsedStampRect.left - (anchorSourceWidth * stampLeftFraction),
        collapsedStampRect.top - (anchorSourceHeight * stampTopFraction),
        collapsedStampRect.left - (anchorSourceWidth * stampLeftFraction) + anchorSourceWidth,
        collapsedStampRect.top - (anchorSourceHeight * stampTopFraction) + anchorSourceHeight
    )
    val currentSourceRect = remember(anchorSourceRect, expandedSourceRect, scaleProgress) {
        lerpStampContextRect(anchorSourceRect, expandedSourceRect, scaleProgress)
    }
    val stampRectFromSource = remember(
        currentSourceRect,
        stampLeftFraction,
        stampTopFraction,
        stampRightFraction,
        stampBottomFraction
    ) {
        RectF(
            currentSourceRect.left + (currentSourceRect.width() * stampLeftFraction),
            currentSourceRect.top + (currentSourceRect.height() * stampTopFraction),
            currentSourceRect.left + (currentSourceRect.width() * stampRightFraction),
            currentSourceRect.top + (currentSourceRect.height() * stampBottomFraction)
        )
    }
    val currentStampRect = remember(stampRectFromSource) {
        RectF(stampRectFromSource)
    }
    val revealSourceRect = remember(currentSourceRect) {
        RectF(currentSourceRect)
    }
    val sourceImageAlpha = if (transitionProgress <= appearSplit) {
        appearProgress.coerceIn(0f, 1f)
    } else {
        1f
    }
    val stampRotation = collapsedRotation * (1f - scaleProgress)
    val stampShadowStrength = (1.2f - (0.3f * scaleProgress)).coerceIn(0.82f, 1.22f)
    val stampOutlineStrength = (0.92f + (0.22f * scaleProgress)).coerceIn(0.9f, 1.25f)
    val density = LocalDensity.current
    val sourceTranslateX = 0f
    val sourceTranslateY = 0f
    val stampTranslateX = currentStampRect.left
    val stampTranslateY = currentStampRect.top
    val sourceShape = RoundedCornerShape(20.dp)
    val sourceReliefStrength = (1.06f + (0.74f * scaleProgress)).coerceIn(1.06f, 1.82f)
    val sourceCastShadowOffsetX = (3.1f * sourceReliefStrength).dp
    val sourceCastShadowOffsetY = (12.6f * sourceReliefStrength).dp
    val sourceCastShadowBlur = (19.8f * sourceReliefStrength).dp
    val sourceCastShadowAlpha = (0.41f * sourceReliefStrength).coerceIn(0f, 0.7f)
    val sourceContactShadowOffsetX = (1.55f * sourceReliefStrength).dp
    val sourceContactShadowOffsetY = (3.2f * sourceReliefStrength).dp
    val sourceContactShadowBlur = (3.9f * sourceReliefStrength).dp
    val sourceContactShadowAlpha = (0.3f * sourceReliefStrength).coerceIn(0f, 0.5f)
    val sourceEdgeShadeOffsetX = (1.02f * sourceReliefStrength).dp
    val sourceEdgeShadeOffsetY = (1.22f * sourceReliefStrength).dp
    val sourceEdgeShadeAlpha = (0.1f * sourceReliefStrength).coerceIn(0f, 0.2f)
    val sourceEdgeHighlightOffsetX = (-0.45f * sourceReliefStrength).dp
    val sourceEdgeHighlightOffsetY = (-0.52f * sourceReliefStrength).dp
    val sourceEdgeHighlightAlpha = (0.05f * sourceReliefStrength).coerceIn(0f, 0.1f)
    val sourceCastShadowFilter = remember(sourceCastShadowAlpha) {
        ColorFilter.tint(
            Color(0xFF2E261C).copy(alpha = sourceCastShadowAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val sourceContactShadowFilter = remember(sourceContactShadowAlpha) {
        ColorFilter.tint(
            Color(0xFF3A3125).copy(alpha = sourceContactShadowAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val sourceEdgeShadeFilter = remember(sourceEdgeShadeAlpha) {
        ColorFilter.tint(
            Color(0xFF211A13).copy(alpha = sourceEdgeShadeAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val sourceEdgeHighlightFilter = remember(sourceEdgeHighlightAlpha) {
        ColorFilter.tint(
            Color.White.copy(alpha = sourceEdgeHighlightAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val sourceFrameElevation = (18.dp * sourceReliefStrength)
    val sourceAmbientShadow = Color(0x38120A05)
    val sourceSpotShadow = Color(0x8F2A140B)

    Box(modifier = modifier) {
        if (transitionProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f * transitionProgress))
                    .noRippleClickable(onClick = onClick)
            )
        }
        if (sourceImageAlpha > 0f) {
            val revealWidthDp = with(density) { revealSourceRect.width().coerceAtLeast(1f).toDp() }
            val revealHeightDp = with(density) { revealSourceRect.height().coerceAtLeast(1f).toDp() }
            val sourceWidthDp = with(density) { currentSourceRect.width().coerceAtLeast(1f).toDp() }
            val sourceHeightDp = with(density) { currentSourceRect.height().coerceAtLeast(1f).toDp() }
            val sourceLayerModifier = Modifier
                .width(sourceWidthDp)
                .height(sourceHeightDp)
                .graphicsLayer {
                    translationX = sourceTranslateX
                    translationY = sourceTranslateY
                }
            val sourceModifier = Modifier
                .then(
                    if (sourceImageAlpha > 0.05f) {
                        Modifier.noRippleClickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .offset {
                    IntOffset(
                        x = revealSourceRect.left.roundToInt(),
                        y = revealSourceRect.top.roundToInt()
                    )
                }
                .width(revealWidthDp)
                .height(revealHeightDp)
                .graphicsLayer {
                    alpha = sourceImageAlpha
                }
                .shadow(
                    elevation = sourceFrameElevation,
                    shape = sourceShape,
                    clip = false,
                    ambientColor = sourceAmbientShadow,
                    spotColor = sourceSpotShadow
                )
                .clip(sourceShape)
                .clipToBounds()
            val fallbackBitmap = fallbackSourceBitmap?.takeUnless { it.isRecycled }
            Box(modifier = sourceModifier) {
                if (fallbackBitmap != null) {
                    Image(
                        bitmap = fallbackBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.Low,
                        colorFilter = sourceCastShadowFilter,
                        modifier = sourceLayerModifier
                            .offset(x = sourceCastShadowOffsetX, y = sourceCastShadowOffsetY)
                            .blur(sourceCastShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    )
                    Image(
                        bitmap = fallbackBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.Low,
                        colorFilter = sourceContactShadowFilter,
                        modifier = sourceLayerModifier
                            .offset(x = sourceContactShadowOffsetX, y = sourceContactShadowOffsetY)
                            .blur(sourceContactShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    )
                    Image(
                        bitmap = fallbackBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.Medium,
                        colorFilter = sourceEdgeShadeFilter,
                        modifier = sourceLayerModifier
                            .offset(x = sourceEdgeShadeOffsetX, y = sourceEdgeShadeOffsetY)
                    )
                    Image(
                        bitmap = fallbackBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.Medium,
                        colorFilter = sourceEdgeHighlightFilter,
                        modifier = sourceLayerModifier
                            .offset(x = sourceEdgeHighlightOffsetX, y = sourceEdgeHighlightOffsetY)
                    )
                    Image(
                        bitmap = fallbackBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.High,
                        modifier = sourceLayerModifier
                    )
                } else {
                    Image(
                        painter = sourcePainter,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        colorFilter = sourceCastShadowFilter,
                        modifier = sourceLayerModifier
                            .offset(x = sourceCastShadowOffsetX, y = sourceCastShadowOffsetY)
                            .blur(sourceCastShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    )
                    Image(
                        painter = sourcePainter,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        colorFilter = sourceContactShadowFilter,
                        modifier = sourceLayerModifier
                            .offset(x = sourceContactShadowOffsetX, y = sourceContactShadowOffsetY)
                            .blur(sourceContactShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    )
                    Image(
                        painter = sourcePainter,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        colorFilter = sourceEdgeShadeFilter,
                        modifier = sourceLayerModifier
                            .offset(x = sourceEdgeShadeOffsetX, y = sourceEdgeShadeOffsetY)
                    )
                    Image(
                        painter = sourcePainter,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        colorFilter = sourceEdgeHighlightFilter,
                        modifier = sourceLayerModifier
                            .offset(x = sourceEdgeHighlightOffsetX, y = sourceEdgeHighlightOffsetY)
                    )
                    Image(
                        painter = sourcePainter,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = sourceLayerModifier
                    )
                }
            }
        }

        StampCard(
            uri = stampUri,
            previewBitmap = stampPreviewBitmap,
            skin = skin,
            categoryEmoji = stampEmoji,
            aspectRatio = stampAspectRatio,
            rotation = stampRotation,
            shadowStrength = stampShadowStrength,
            outlineStrength = stampOutlineStrength,
            onClick = onClick,
            modifier = Modifier
                .width(with(density) { currentStampRect.width().toDp() })
                .aspectRatio(stampAspectRatio)
                .graphicsLayer {
                    translationX = stampTranslateX
                    translationY = stampTranslateY
                }
        )
    }
}

private fun decodeStampSourceContextFileBitmap(
    sourceUri: String
): Bitmap? {
    if (sourceUri.isBlank()) return null
    val parsed = runCatching { Uri.parse(sourceUri) }.getOrNull()
    val filePath = when (parsed?.scheme?.lowercase(Locale.US)) {
        "file" -> parsed.path
        null, "" -> sourceUri
        else -> null
    }?.takeIf { it.isNotBlank() } ?: return null
    val file = File(filePath)
    if (!file.exists() || !file.isFile) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = buildStampSourceContextSampleSize(
            sourceWidth = bounds.outWidth,
            sourceHeight = bounds.outHeight,
            maxSide = STAMP_SOURCE_CONTEXT_FALLBACK_MAX_SIDE
        )
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }.getOrNull()
}

private fun buildStampSourceContextSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    maxSide: Int
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0 || maxSide <= 0) return 1
    var sampleSize = 1
    var currentMax = maxOf(sourceWidth, sourceHeight)
    while (currentMax > maxSide) {
        sampleSize = (sampleSize shl 1).coerceAtMost(128)
        currentMax = maxOf(sourceWidth / sampleSize, sourceHeight / sampleSize)
        if (sampleSize >= 128) break
    }
    return sampleSize.coerceAtLeast(1)
}

private fun lerpStampContextRect(
    start: RectF,
    end: RectF,
    fraction: Float
): RectF {
    val clamped = fraction.coerceIn(0f, 1f)
    return RectF(
        start.left + ((end.left - start.left) * clamped),
        start.top + ((end.top - start.top) * clamped),
        start.right + ((end.right - start.right) * clamped),
        start.bottom + ((end.bottom - start.bottom) * clamped)
    )
}

@Composable
private fun StampNoteField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= 80) {
                onValueChange(input)
            }
        },
        textStyle = TextStyle(
            color = stampAdaptiveColor(
                light = Color(0xFF2B251C),
                dark = MaterialTheme.colorScheme.onBackground
            ),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.isFocused)
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        stampAdaptiveColor(
                            light = Color.White.copy(alpha = 0.42f),
                            dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = stampAdaptiveColor(
                            light = Color(0xFF9A927F),
                            dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
                        ),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun EmojiPickerTrigger(
    emoji: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val triggerShape = RoundedCornerShape(24.dp)
    val borderColor = stampAdaptiveColor(
        light = Color(0xFFB8AF9B),
        dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.34f)
    )
    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .clip(triggerShape)
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(STAMP_DETAIL_PICKER_TRIGGER_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji ?: "\uD83E\uDEE5", fontSize = 18.sp)
    }
}

@Composable
private fun StampSkinPickerTrigger(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val triggerShape = RoundedCornerShape(24.dp)
    val borderColor = stampAdaptiveColor(
        light = Color(0xFFC7BDA9),
        dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f)
    )
    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = 1.4.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .clip(triggerShape)
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(STAMP_DETAIL_PICKER_TRIGGER_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Apps,
            contentDescription = null,
            tint = stampAdaptiveColor(
                light = Color(0xFF4D463B),
                dark = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun StampSkinPickerPanel(
    visible: Boolean,
    copy: StampSkinPanelCopy,
    currentSkin: StampCalendarStampSkin,
    draftSkin: StampCalendarStampSkin,
    shapeLockedToCutout: Boolean,
    previewUri: String?,
    previewBitmap: Bitmap?,
    previewEmoji: String?,
    previewCutoutPreparing: Boolean,
    confirmInFlight: Boolean,
    previewAspectRatio: Float,
    anchorBounds: Rect?,
    onDismiss: () -> Unit,
    onDraftChange: (StampCalendarStampSkin) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "skin_picker_progress"
    )
    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible) 0.2f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "skin_picker_backdrop_alpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) 220 else 120,
            delayMillis = if (visible) 70 else 0
        ),
        label = "skin_picker_content_alpha"
    )
    val editorialMode = draftSkin.style == StampCalendarSkinStyle.Editorial
    val customBackgroundMode = draftSkin.style == StampCalendarSkinStyle.WhiteBorder ||
        draftSkin.style == StampCalendarSkinStyle.Editorial ||
        draftSkin.style == StampCalendarSkinStyle.Cutout
    val selectedImageEffect = draftSkin.imageEffect
    val selectedBackgroundColor = normalizeStampSkinBackgroundColor(
        style = draftSkin.style,
        requestedColor = draftSkin.backgroundColorArgb
    )
    val styleSwitchUnlocked = !shapeLockedToCutout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                stampAdaptiveColor(
                    light = Color(0xFF120F0B),
                    dark = Color.Black
                ).copy(alpha = backdropAlpha)
            )
            .noRippleClickable(enabled = visible && !confirmInFlight, onClick = onDismiss)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val collapsedWidthPx = (anchorBounds?.width
                ?: with(density) { 112.dp.toPx() })
                .coerceAtLeast(with(density) { STAMP_DETAIL_PICKER_TRIGGER_WIDTH.toPx() })
            val collapsedHeightPx = (anchorBounds?.height
                ?: with(density) { 48.dp.toPx() })
                .coerceAtLeast(with(density) { 36.dp.toPx() })
            val collapsedCenterX = anchorBounds?.center?.x
                ?: with(density) { maxWidth.toPx() * 0.5f }
            val collapsedCenterY = anchorBounds?.center?.y
                ?: with(density) { maxHeight.toPx() * 0.72f }
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val horizontalPaddingPx = with(density) { 14.dp.toPx() }
            val topSafePaddingPx = with(density) { 72.dp.toPx() }
            val bottomSafePaddingPx = with(density) { 24.dp.toPx() }
            val expandedWidthPx = minOf(
                with(density) { 368.dp.toPx() },
                (maxWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(with(density) { 300.dp.toPx() })
            )
            val desiredHeight = when {
                editorialMode -> STAMP_SKIN_PICKER_EXPANDED_HEIGHT_EDITORIAL
                customBackgroundMode -> STAMP_SKIN_PICKER_EXPANDED_HEIGHT_CUSTOM
                else -> STAMP_SKIN_PICKER_EXPANDED_HEIGHT_DEFAULT
            }
            val expandedHeightPx = minOf(
                with(density) { desiredHeight.toPx() },
                (maxHeightPx - topSafePaddingPx - bottomSafePaddingPx)
                    .coerceAtLeast(with(density) { STAMP_SKIN_PICKER_MIN_HEIGHT.toPx() })
            )
            val requestedExpandedTop = collapsedCenterY - expandedHeightPx - with(density) { 16.dp.toPx() }
            val expandedTop = requestedExpandedTop
                .coerceAtLeast(topSafePaddingPx)
                .coerceAtMost(
                    (maxHeightPx - expandedHeightPx - bottomSafePaddingPx)
                        .coerceAtLeast(topSafePaddingPx)
                )
            val expandedCenterX = maxWidthPx * 0.5f
            val expandedCenterY = expandedTop + expandedHeightPx * 0.5f
            val animatedCenterX = lerpFloat(collapsedCenterX, expandedCenterX, progress)
            val animatedCenterY = lerpFloat(collapsedCenterY, expandedCenterY, progress)
            val animatedWidthPx = lerpFloat(collapsedWidthPx, expandedWidthPx, progress)
            val animatedHeightPx = lerpFloat(collapsedHeightPx, expandedHeightPx, progress)
            val animatedLeftPx = (animatedCenterX - animatedWidthPx * 0.5f).coerceIn(
                horizontalPaddingPx,
                (maxWidthPx - animatedWidthPx - horizontalPaddingPx)
                    .coerceAtLeast(horizontalPaddingPx)
            )
            val animatedTopPx = (animatedCenterY - animatedHeightPx * 0.5f).coerceIn(
                topSafePaddingPx,
                (maxHeightPx - animatedHeightPx - bottomSafePaddingPx)
                    .coerceAtLeast(topSafePaddingPx)
            )
            val animatedCornerRadius = with(density) {
                lerpFloat(22.dp.toPx(), 30.dp.toPx(), progress).toDp()
            }
            val panelShape = RoundedCornerShape(animatedCornerRadius)
            val panelSurfaceColor = stampAdaptiveColor(
                light = Color(0xFFF8F2E6),
                dark = MaterialTheme.colorScheme.surface
            )
            val panelBorderColor = stampAdaptiveColor(
                light = Color(0xFFE1D8C6),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            )
            val contentLift by animateDpAsState(
                targetValue = if (visible) 0.dp else 8.dp,
                animationSpec = tween(durationMillis = 220),
                label = "skin_picker_content_lift"
            )
            val previewWidth = 126.dp
            val previewHeight = previewWidth / previewAspectRatio.coerceIn(0.55f, 1.45f)
            val previewSkin = if (
                draftSkin.style == StampCalendarSkinStyle.Cutout &&
                previewCutoutPreparing
            ) {
                currentSkin
            } else {
                draftSkin
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { animatedLeftPx.toDp() },
                        y = with(density) { animatedTopPx.toDp() }
                    )
                    .width(with(density) { animatedWidthPx.toDp() })
                    .height(with(density) { animatedHeightPx.toDp() })
                    .clip(panelShape)
                    .background(panelSurfaceColor)
                    .border(1.dp, panelBorderColor, panelShape)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .noRippleClickable { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentLift.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = copy.title,
                            color = stampAdaptiveColor(
                                light = Color(0xFF211C15),
                                dark = MaterialTheme.colorScheme.onSurface
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .width(previewWidth)
                                .height(previewHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            StampCard(
                                uri = previewUri,
                                previewBitmap = previewBitmap,
                                skin = previewSkin,
                                categoryEmoji = previewEmoji,
                                aspectRatio = previewAspectRatio,
                                rotation = 0f,
                                shadowStrength = 0.82f,
                                motionOptimized = true,
                                entryRevealEnabled = false,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (
                                draftSkin.style == StampCalendarSkinStyle.Cutout &&
                                previewCutoutPreparing
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.2.dp,
                                    color = stampAdaptiveColor(
                                        light = Color(0xFF1D1A15),
                                        dark = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StampSkinStyleChip(
                                label = copy.originalLabel,
                                selected = draftSkin.style == StampCalendarSkinStyle.Original,
                                enabled = styleSwitchUnlocked,
                                onClick = {
                                    onDraftChange(
                                        draftSkin.copy(
                                            style = StampCalendarSkinStyle.Original,
                                            numberLabel = "",
                                            title = "",
                                            subtitle = "",
                                            cutoutAssetPath = "",
                                            backgroundColorArgb = 0
                                        ).normalized()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StampSkinStyleChip(
                                label = copy.whiteBorderLabel,
                                selected = draftSkin.style == StampCalendarSkinStyle.WhiteBorder,
                                enabled = styleSwitchUnlocked,
                                onClick = {
                                    onDraftChange(
                                        draftSkin.copy(
                                            style = StampCalendarSkinStyle.WhiteBorder,
                                            backgroundColorArgb = normalizeStampSkinBackgroundColor(
                                                style = StampCalendarSkinStyle.WhiteBorder,
                                                requestedColor = draftSkin.backgroundColorArgb
                                            )
                                        ).normalized()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StampSkinStyleChip(
                                label = copy.editorialLabel,
                                selected = draftSkin.style == StampCalendarSkinStyle.Editorial,
                                enabled = styleSwitchUnlocked,
                                onClick = {
                                    onDraftChange(
                                        draftSkin.copy(
                                            style = StampCalendarSkinStyle.Editorial,
                                            title = draftSkin.title.ifBlank { STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT },
                                            subtitle = "",
                                            backgroundColorArgb = normalizeStampSkinBackgroundColor(
                                                style = StampCalendarSkinStyle.Editorial,
                                                requestedColor = draftSkin.backgroundColorArgb
                                            )
                                        ).normalized()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StampSkinStyleChip(
                                label = copy.cutoutLabel,
                                selected = draftSkin.style == StampCalendarSkinStyle.Cutout,
                                enabled = true,
                                onClick = {
                                    onDraftChange(
                                        draftSkin.copy(
                                            style = StampCalendarSkinStyle.Cutout,
                                            subtitle = "",
                                            numberLabel = "",
                                            backgroundColorArgb = normalizeStampSkinBackgroundColor(
                                                style = StampCalendarSkinStyle.Cutout,
                                                requestedColor = draftSkin.backgroundColorArgb
                                            )
                                        ).normalized()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        StampSkinEffectRow(
                            label = copy.effectLabel,
                            selectedEffect = selectedImageEffect,
                            copy = copy,
                            onEffectSelected = { effect ->
                                onDraftChange(
                                    draftSkin.copy(imageEffect = effect).normalized()
                                )
                            }
                        )
                        if (customBackgroundMode) {
                            StampSkinBackgroundColorRow(
                                label = copy.backgroundColorLabel,
                                selectedColor = selectedBackgroundColor,
                                onColorSelected = { selectedColorArgb ->
                                    onDraftChange(
                                        draftSkin.copy(
                                            backgroundColorArgb = selectedColorArgb
                                        ).normalized()
                                    )
                                }
                            )
                        }
                        if (editorialMode) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StampSkinTextField(
                                    label = copy.titleFieldLabel,
                                    value = draftSkin.title,
                                    placeholder = copy.titlePlaceholder,
                                    onValueChange = { value ->
                                        onDraftChange(
                                            draftSkin.copy(
                                                title = value.take(40),
                                                subtitle = ""
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = stampAdaptiveColor(
                                light = Color(0xFFF0EBE2),
                                dark = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(STAMP_DIALOG_ACTION_BUTTON_HEIGHT)
                                .border(
                                    1.dp,
                                    stampAdaptiveColor(
                                        light = Color(0xFFE1D5C1),
                                        dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .noRippleClickable(enabled = !confirmInFlight) { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = copy.cancelLabel,
                                    color = stampAdaptiveColor(
                                        light = Color(0xFF6B635A),
                                        dark = MaterialTheme.colorScheme.onSurface
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1F8B4C),
                            modifier = Modifier
                                .weight(1f)
                                .height(STAMP_DIALOG_ACTION_BUTTON_HEIGHT)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Color(0xFF1F8B4C).copy(alpha = 0.18f),
                                    spotColor = Color(0xFF1F8B4C).copy(alpha = 0.18f)
                                )
                                .graphicsLayer {
                                    alpha = if (confirmInFlight) 0.72f else 1f
                                }
                                .noRippleClickable(enabled = !confirmInFlight) { onConfirm() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (confirmInFlight) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = copy.confirmLabel,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(7.dp))
                                        CampfireBadgeIcon(
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    text = stampSkinStyleLabel(copy, currentSkin),
                    color = stampAdaptiveColor(
                        light = Color(0xFF181512),
                        dark = MaterialTheme.colorScheme.onBackground
                    ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            alpha = (1f - progress).coerceIn(0f, 1f)
                        }
                )
            }
        }
    }
}

@Composable
private fun StampSkinCampfireGateDialog(
    message: String,
    learnLabel: String,
    cancelLabel: String,
    onLearnClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onLearnClick) {
                Text(learnLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        }
    )
}

@Composable
private fun CampfireBadgeIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = STAMP_CAMPFIRE_BADGE_ASSET,
            contentDescription = "Campfire Plan",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

private fun isStampCampfireFeatureActive(context: Context): Boolean {
    val appPrefs = PreferencesManager(context.applicationContext)
    if (appPrefs.campfireSelfParticipationChecked) return true
    val userDisplayName = context
        .applicationContext
        .getSharedPreferences(STAMP_STARTUP_MINE_PROFILE_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(STAMP_STARTUP_MINE_USER_DISPLAY_NAME_KEY, STAMP_STARTUP_MINE_DEFAULT_USER_NAME)
        .orEmpty()
        .trim()
        .ifBlank { STAMP_STARTUP_MINE_DEFAULT_USER_NAME }
    return campfireSupporters.any { supporter ->
        supporter.idLabel.trim().equals(userDisplayName, ignoreCase = true)
    }
}

@Composable
private fun StampSkinStyleChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val chipColor = if (selected) {
        stampAdaptiveColor(
            light = Color(0xFFF7F0E1),
            dark = MaterialTheme.colorScheme.surfaceVariant
        )
    } else {
        stampAdaptiveColor(
            light = Color(0xFFF7F1E5),
            dark = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    }
    val chipBorderColor = if (selected) {
        stampAdaptiveColor(
            light = Color(0xFF181512),
            dark = MaterialTheme.colorScheme.onSurface
        )
    } else {
        stampAdaptiveColor(
            light = Color(0xFFE5DCCB),
            dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
        )
    }
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(chipColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = chipBorderColor,
                shape = shape
            )
            .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
            .noRippleClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = stampAdaptiveColor(
                light = Color(0xFF181512),
                dark = MaterialTheme.colorScheme.onSurface
            ),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StampSkinEffectRow(
    label: String,
    selectedEffect: StampCalendarImageEffect,
    copy: StampSkinPanelCopy,
    onEffectSelected: (StampCalendarImageEffect) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = STAMP_SKIN_IMAGE_EFFECT_OPTIONS
    val rows = remember(options) { options.chunked(3) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = stampAdaptiveColor(
                light = Color(0xFF6C6356),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            ),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { effect ->
                        StampSkinStyleChip(
                            label = stampSkinEffectLabel(copy, effect),
                            selected = selectedEffect == effect,
                            onClick = { onEffectSelected(effect) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StampSkinBackgroundColorRow(
    label: String,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = stampAdaptiveColor(
                light = Color(0xFF6C6356),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            ),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            STAMP_SKIN_BACKGROUND_COLOR_OPTIONS.forEach { colorArgb ->
                StampSkinBackgroundColorChip(
                    colorArgb = colorArgb,
                    selected = colorArgb == selectedColor,
                    onClick = { onColorSelected(colorArgb) }
                )
            }
        }
    }
}

@Composable
private fun StampSkinBackgroundColorChip(
    colorArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(colorArgb))
            .border(
                width = if (selected) 2.2.dp else 1.dp,
                color = if (selected) {
                    stampAdaptiveColor(
                        light = Color(0xFF181512),
                        dark = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    stampAdaptiveColor(
                        light = Color(0xFFD8CEBC),
                        dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f)
                    )
                },
                shape = CircleShape
            )
            .noRippleClickable(onClick = onClick)
    )
}

@Composable
private fun StampSkinTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = stampAdaptiveColor(
                light = Color(0xFF7B7363),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = stampAdaptiveColor(
                    light = Color(0xFF2B251C),
                    dark = MaterialTheme.colorScheme.onBackground
                ),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            stampAdaptiveColor(
                                light = Color.White.copy(alpha = 0.56f),
                                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f)
                            )
                        )
                        .border(
                            1.dp,
                            stampAdaptiveColor(
                                light = Color(0xFFE3D8C7),
                                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = stampAdaptiveColor(
                                light = Color(0xFF9A927F),
                                dark = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
                            ),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun EmojiPickerPanel(
    visible: Boolean,
    title: String,
    clearLabel: String,
    selectedEmoji: String?,
    anchorBounds: Rect?,
    onDismiss: () -> Unit,
    onEmojiSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "emoji_panel_progress"
    )
    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible) 0.2f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "emoji_panel_backdrop_alpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 220 else 120, delayMillis = if (visible) 70 else 0),
        label = "emoji_panel_content_alpha"
    )
    Box(
        modifier = modifier
            .background(
                stampAdaptiveColor(
                    light = Color(0xFF120F0B),
                    dark = Color.Black
                ).copy(alpha = backdropAlpha)
            )
            .noRippleClickable(enabled = visible, onClick = onDismiss)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val collapsedWidthPx = anchorBounds?.width
                ?: with(density) { 88.dp.toPx() }
            val collapsedHeightPx = anchorBounds?.height
                ?: with(density) { 50.dp.toPx() }
            val collapsedCenterX = anchorBounds?.center?.x
                ?: with(density) { maxWidth.toPx() * 0.5f }
            val collapsedCenterY = anchorBounds?.center?.y
                ?: with(density) { maxHeight.toPx() * 0.7f }
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val horizontalPaddingPx = with(density) { 14.dp.toPx() }
            val topSafePaddingPx = with(density) { 72.dp.toPx() }
            val bottomSafePaddingPx = with(density) { 22.dp.toPx() }
            val expandedWidthPx = minOf(
                with(density) { 348.dp.toPx() },
                (maxWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(with(density) { 260.dp.toPx() })
            )
            val emojiPanelTargetHeightPx = with(density) { 456.dp.toPx() }
            val expandedHeightPx = minOf(
                emojiPanelTargetHeightPx,
                (maxHeightPx - topSafePaddingPx - bottomSafePaddingPx)
                    .coerceAtLeast(with(density) { 320.dp.toPx() })
            )
            val requestedExpandedTop = collapsedCenterY - expandedHeightPx - with(density) { 18.dp.toPx() }
            val expandedTop = requestedExpandedTop
                .coerceAtLeast(topSafePaddingPx)
                .coerceAtMost(
                    (maxHeightPx - expandedHeightPx - bottomSafePaddingPx)
                        .coerceAtLeast(topSafePaddingPx)
                )
            val expandedCenterX = maxWidthPx * 0.5f
            val expandedCenterY = expandedTop + expandedHeightPx * 0.5f
            val animatedCenterX = lerpFloat(collapsedCenterX, expandedCenterX, progress)
            val animatedCenterY = lerpFloat(collapsedCenterY, expandedCenterY, progress)
            val animatedWidthPx = lerpFloat(collapsedWidthPx, expandedWidthPx, progress)
            val animatedHeightPx = lerpFloat(collapsedHeightPx, expandedHeightPx, progress)
            val animatedLeftPx = (animatedCenterX - animatedWidthPx * 0.5f).coerceIn(
                horizontalPaddingPx,
                (maxWidthPx - animatedWidthPx - horizontalPaddingPx)
                    .coerceAtLeast(horizontalPaddingPx)
            )
            val animatedTopPx = (animatedCenterY - animatedHeightPx * 0.5f).coerceIn(
                topSafePaddingPx,
                (maxHeightPx - animatedHeightPx - bottomSafePaddingPx)
                    .coerceAtLeast(topSafePaddingPx)
            )
            val animatedCornerRadius = with(density) {
                lerpFloat(24.dp.toPx(), 30.dp.toPx(), progress).toDp()
            }
            val panelShape = RoundedCornerShape(animatedCornerRadius)
            val panelSurfaceColor = stampAdaptiveColor(
                light = Color(0xFFF9F4E9),
                dark = MaterialTheme.colorScheme.surface
            )
            val panelBorderColor = stampAdaptiveColor(
                light = Color(0xFFE1D8C6),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            )
            val contentLift by animateDpAsState(
                targetValue = if (visible) 0.dp else 8.dp,
                animationSpec = tween(durationMillis = 220),
                label = "emoji_panel_content_lift"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { animatedLeftPx.toDp() },
                        y = with(density) { animatedTopPx.toDp() }
                    )
                    .width(with(density) { animatedWidthPx.toDp() })
                    .height(with(density) { animatedHeightPx.toDp() })
                    .clip(panelShape)
                    .background(panelSurfaceColor)
                    .border(
                        1.dp,
                        panelBorderColor,
                        panelShape
                    )
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 16.dp,
                        bottom = 18.dp
                    )
                    .noRippleClickable { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentLift.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        color = stampAdaptiveColor(
                            light = Color(0xFF211C15),
                            dark = MaterialTheme.colorScheme.onSurface
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    STAMP_EMOJI_PRESETS.chunked(5).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 10.dp,
                                alignment = Alignment.CenterHorizontally
                            )
                        ) {
                            rowItems.forEach { emoji ->
                                EmojiChoiceChip(
                                    emoji = emoji,
                                    selected = selectedEmoji == emoji,
                                    onClick = { onEmojiSelected(emoji) }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 10.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        STAMP_EMOJI_MOODS.forEach { emoji ->
                            EmojiChoiceChip(
                                emoji = emoji,
                                selected = selectedEmoji == emoji,
                                onClick = { onEmojiSelected(emoji) }
                            )
                        }
                    }
                    Text(
                        text = clearLabel,
                        color = stampAdaptiveColor(
                            light = Color(0xFF7B7363),
                            dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable { onEmojiSelected(null) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(72.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = stampAdaptiveColor(
                                light = Color.White.copy(alpha = 0.78f),
                                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = stampAdaptiveColor(
                                    light = Color(0xFFD8CEBC),
                                    dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                                )
                            ),
                            modifier = Modifier
                                .size(54.dp)
                                .noRippleClickable(onClick = onDismiss)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close group panel",
                                    tint = stampAdaptiveColor(
                                        light = Color(0xFF595247),
                                        dark = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = selectedEmoji ?: "\uD83E\uDEE5",
                    fontSize = 24.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            alpha = (1f - progress).coerceIn(0f, 1f)
                        }
                )
            }
        }
    }
}

@Composable
private fun StampDatePickerPanel(
    visible: Boolean,
    title: String,
    currentDayKey: String,
    openSignal: Int,
    confirmLabel: String,
    cancelLabel: String,
    anchorBounds: Rect?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val appLanguage = LocalAppLanguage.current
    val initialDate = remember(currentDayKey, openSignal) {
        parseStampCalendarDate(currentDayKey)
    }
    val yearOptions = remember(initialDate.year) {
        buildStampYearWheelOptions(initialDate.year)
    }
    val monthOptions = remember(appLanguage) {
        buildStampMonthWheelOptions(appLanguage)
    }
    var selectedYear by remember(currentDayKey, openSignal) { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember(currentDayKey, openSignal) { mutableIntStateOf(initialDate.month) }
    var selectedDay by remember(currentDayKey, openSignal) { mutableIntStateOf(initialDate.day) }
    LaunchedEffect(visible, currentDayKey, openSignal) {
        if (!visible) return@LaunchedEffect
        val openDate = parseStampCalendarDate(currentDayKey)
        selectedYear = openDate.year
        selectedMonth = openDate.month
        selectedDay = openDate.day
    }
    val daysInSelectedMonth = remember(selectedYear, selectedMonth) {
        stampCalendarDaysInMonth(selectedYear, selectedMonth)
    }
    val clampedSelectedDay = selectedDay.coerceIn(1, daysInSelectedMonth)
    LaunchedEffect(selectedYear, selectedMonth, daysInSelectedMonth) {
        if (selectedDay != clampedSelectedDay) {
            selectedDay = clampedSelectedDay
        }
    }
    val dayOptions = remember(selectedYear, selectedMonth, daysInSelectedMonth) {
        (1..daysInSelectedMonth).map { day ->
            StampWheelOption(
                value = day,
                label = String.format(Locale.US, "%02d", day)
            )
        }
    }
    val selectedDayKey = remember(selectedYear, selectedMonth, clampedSelectedDay) {
        stampCalendarDayKey(
            year = selectedYear,
            month = selectedMonth,
            day = clampedSelectedDay
        )
    }
    val selectedDateLabel = remember(appLanguage, selectedYear, selectedMonth, clampedSelectedDay) {
        formatStampCalendarHeadlineDate(
            language = appLanguage,
            year = selectedYear,
            month = selectedMonth,
            day = clampedSelectedDay
        )
    }
    val collapsedLabel = remember(currentDayKey) {
        currentDayKey.replace("-", ".")
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "date_picker_progress"
    )
    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible) 0.2f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "date_picker_backdrop_alpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) 220 else 120,
            delayMillis = if (visible) 70 else 0
        ),
        label = "date_picker_content_alpha"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                stampAdaptiveColor(
                    light = Color(0xFF120F0B),
                    dark = Color.Black
                ).copy(alpha = backdropAlpha)
            )
            .noRippleClickable(enabled = visible, onClick = onDismiss)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val collapsedWidthPx = (anchorBounds?.width
                ?: with(density) { 132.dp.toPx() })
                .coerceAtLeast(with(density) { 92.dp.toPx() })
            val collapsedHeightPx = (anchorBounds?.height
                ?: with(density) { 44.dp.toPx() })
                .coerceAtLeast(with(density) { 34.dp.toPx() })
            val collapsedCenterX = anchorBounds?.center?.x
                ?: with(density) { maxWidth.toPx() * 0.5f }
            val collapsedCenterY = anchorBounds?.center?.y
                ?: with(density) { maxHeight.toPx() * 0.34f }
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val horizontalPaddingPx = with(density) { 14.dp.toPx() }
            val topSafePaddingPx = with(density) { 68.dp.toPx() }
            val bottomSafePaddingPx = with(density) { 24.dp.toPx() }
            val expandedWidthPx = minOf(
                with(density) { 360.dp.toPx() },
                (maxWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(with(density) { 290.dp.toPx() })
            )
            val expandedHeightPx = minOf(
                with(density) { STAMP_DATE_PICKER_EXPANDED_HEIGHT.toPx() },
                (maxHeightPx - topSafePaddingPx - bottomSafePaddingPx)
                    .coerceAtLeast(with(density) { STAMP_DATE_PICKER_MIN_HEIGHT.toPx() })
            )
            val requestedExpandedTop = collapsedCenterY - (expandedHeightPx * 0.28f)
            val expandedTop = requestedExpandedTop
                .coerceAtLeast(topSafePaddingPx)
                .coerceAtMost(
                    (maxHeightPx - expandedHeightPx - bottomSafePaddingPx)
                        .coerceAtLeast(topSafePaddingPx)
                )
            val expandedCenterX = maxWidthPx * 0.5f
            val expandedCenterY = expandedTop + expandedHeightPx * 0.5f
            val animatedCenterX = lerpFloat(collapsedCenterX, expandedCenterX, progress)
            val animatedCenterY = lerpFloat(collapsedCenterY, expandedCenterY, progress)
            val animatedWidthPx = lerpFloat(collapsedWidthPx, expandedWidthPx, progress)
            val animatedHeightPx = lerpFloat(collapsedHeightPx, expandedHeightPx, progress)
            val animatedLeftPx = (animatedCenterX - animatedWidthPx * 0.5f).coerceIn(
                horizontalPaddingPx,
                (maxWidthPx - animatedWidthPx - horizontalPaddingPx)
                    .coerceAtLeast(horizontalPaddingPx)
            )
            val animatedTopPx = (animatedCenterY - animatedHeightPx * 0.5f).coerceIn(
                topSafePaddingPx,
                (maxHeightPx - animatedHeightPx - bottomSafePaddingPx)
                    .coerceAtLeast(topSafePaddingPx)
            )
            val animatedCornerRadius = with(density) {
                lerpFloat(22.dp.toPx(), 32.dp.toPx(), progress).toDp()
            }
            val panelShape = RoundedCornerShape(animatedCornerRadius)
            val panelSurfaceColor = stampAdaptiveColor(
                light = Color(0xFFF8F2E6),
                dark = MaterialTheme.colorScheme.surface
            )
            val panelBorderColor = stampAdaptiveColor(
                light = Color(0xFFE0D5C2),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            )
            val fallbackPanelShadow = stampAdaptiveColor(
                light = Color(0xFF3A3022).copy(alpha = 0.16f * progress.coerceIn(0f, 1f)),
                dark = Color.Black.copy(alpha = 0.28f * progress.coerceIn(0f, 1f))
            )
            val fallbackHeaderShape = RoundedCornerShape(18.dp)
            val contentLift by animateDpAsState(
                targetValue = if (visible) 0.dp else 8.dp,
                animationSpec = tween(durationMillis = 220),
                label = "date_picker_content_lift"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { animatedLeftPx.toDp() },
                        y = with(density) { animatedTopPx.toDp() }
                    )
                    .width(with(density) { animatedWidthPx.toDp() })
                    .height(with(density) { animatedHeightPx.toDp() })
                    .shadow(
                        elevation = 24.dp,
                        shape = panelShape,
                        ambientColor = fallbackPanelShadow,
                        spotColor = fallbackPanelShadow
                    )
                    .clip(panelShape)
                    .background(panelSurfaceColor)
                    .border(
                        1.dp,
                        panelBorderColor,
                        panelShape
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp)
                    .noRippleClickable { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentLift.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        color = stampAdaptiveColor(
                            light = Color(0xFF221C14),
                            dark = MaterialTheme.colorScheme.onSurface
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .clip(fallbackHeaderShape)
                            .background(
                                stampAdaptiveColor(
                                    light = Color(0xFFFFFBF3),
                                    dark = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = stampAdaptiveColor(
                                    light = Color(0xFFE7DCC8),
                                    dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                                ),
                                shape = fallbackHeaderShape
                            )
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = selectedDateLabel,
                            color = stampAdaptiveColor(
                                light = Color(0xFF181512),
                                dark = MaterialTheme.colorScheme.onSurface
                            ),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(232.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StampDateWheelColumn(
                            options = yearOptions,
                            selectedValue = selectedYear,
                            onValueChange = { selectedYear = it },
                            sessionKey = openSignal,
                            active = visible,
                            modifier = Modifier.weight(1.18f)
                        )
                        StampDateWheelColumn(
                            options = monthOptions,
                            selectedValue = selectedMonth,
                            onValueChange = { selectedMonth = it },
                            sessionKey = openSignal,
                            active = visible,
                            modifier = Modifier.weight(0.98f)
                        )
                        StampDateWheelColumn(
                            options = dayOptions,
                            selectedValue = clampedSelectedDay,
                            onValueChange = { selectedDay = it },
                            sessionKey = openSignal,
                            active = visible,
                            modifier = Modifier.weight(0.84f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = stampAdaptiveColor(
                                light = Color(0xFFF0EBE2),
                                dark = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(STAMP_DIALOG_ACTION_BUTTON_HEIGHT)
                                .border(
                                    width = 1.dp,
                                    color = stampAdaptiveColor(
                                        light = Color(0xFFE1D5C1),
                                        dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .noRippleClickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = cancelLabel,
                                    color = stampAdaptiveColor(
                                        light = Color(0xFF6B635A),
                                        dark = MaterialTheme.colorScheme.onSurface
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1F8B4C),
                            modifier = Modifier
                                .weight(1f)
                                .height(STAMP_DIALOG_ACTION_BUTTON_HEIGHT)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Color(0xFF1F8B4C).copy(alpha = 0.18f),
                                    spotColor = Color(0xFF1F8B4C).copy(alpha = 0.18f)
                                )
                                .noRippleClickable { onConfirm(selectedDayKey) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = confirmLabel,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = collapsedLabel,
                    color = stampAdaptiveColor(
                        light = Color(0xFF181512),
                        dark = MaterialTheme.colorScheme.onBackground
                    ),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            alpha = (1f - progress).coerceIn(0f, 1f)
                        }
                )
            }
        }
    }
}

@Composable
private fun StampDateWheelColumn(
    options: List<StampWheelOption>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    sessionKey: Int,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestSelectedValue by rememberUpdatedState(selectedValue)
    val itemHeight = 46.dp
    val columnShape = RoundedCornerShape(24.dp)
    val selectionShape = RoundedCornerShape(18.dp)
    val selectedIndex = options.indexOfFirst { it.value == selectedValue }
        .coerceAtLeast(0)
    val listState = remember(options, sessionKey) {
        LazyListState(firstVisibleItemIndex = selectedIndex)
    }
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centeredIndex by remember(listState, options, selectedIndex) {
        derivedStateOf {
            if (options.isEmpty()) {
                0
            } else {
                calculateStampWheelCenteredIndex(
                    state = listState,
                    fallback = selectedIndex
                ).coerceIn(0, options.lastIndex)
            }
        }
    }
    var userInteracted by remember(options, sessionKey) { mutableStateOf(false) }
    LaunchedEffect(selectedIndex, options.size, sessionKey, active, userInteracted) {
        if (!active || options.isEmpty() || userInteracted) return@LaunchedEffect
        if (
            listState.firstVisibleItemIndex != selectedIndex ||
            listState.firstVisibleItemScrollOffset != 0
        ) {
            listState.scrollToItem(selectedIndex)
        }
    }
    LaunchedEffect(listState.isScrollInProgress, sessionKey, active) {
        if (!active) return@LaunchedEffect
        if (listState.isScrollInProgress) {
            userInteracted = true
        }
    }
    LaunchedEffect(centeredIndex, options.size, sessionKey, active) {
        if (!active || options.isEmpty() || !userInteracted) return@LaunchedEffect
        val centeredValue = options[centeredIndex].value
        if (centeredValue != latestSelectedValue) {
            latestOnValueChange(centeredValue)
            if (hapticsAllowed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress, centeredIndex, options.size, sessionKey, active, userInteracted) {
        if (!active || options.isEmpty() || !userInteracted) return@LaunchedEffect
        if (listState.isScrollInProgress) return@LaunchedEffect
        if (
            listState.firstVisibleItemIndex != centeredIndex ||
            listState.firstVisibleItemScrollOffset != 0
        ) {
            listState.scrollToItem(centeredIndex)
        }
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(columnShape)
            .background(
                stampAdaptiveColor(
                    light = Color(0xFFF3EBDD),
                    dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f)
                )
            )
            .border(
                width = 1.dp,
                color = stampAdaptiveColor(
                    light = Color(0xFFE0D3BF),
                    dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                ),
                shape = columnShape
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .height(itemHeight)
                .clip(selectionShape)
                .background(
                    stampAdaptiveColor(
                        light = Color.White.copy(alpha = 0.96f),
                        dark = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
                    )
                )
                .border(
                    width = 1.dp,
                    color = stampAdaptiveColor(
                        light = Color(0xFFE2D4C2),
                        dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                    ),
                    shape = selectionShape
                )
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * 2)
        ) {
            items(options.size) { index ->
                val option = options[index]
                val selected = option.value == selectedValue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .noRippleClickable {
                            userInteracted = true
                            scope.launch {
                                listState.animateScrollToItem(index)
                            }
                            onValueChange(option.value)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        color = stampAdaptiveColor(
                            light = Color(0xFF181512),
                            dark = MaterialTheme.colorScheme.onSurface
                        ).copy(alpha = if (selected) 1f else 0.5f),
                        fontSize = if (selected) 18.sp else 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

private fun calculateStampWheelCenteredIndex(
    state: LazyListState,
    fallback: Int
): Int {
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return fallback
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return visibleItems.minByOrNull { item ->
        abs((item.offset + (item.size / 2)) - viewportCenter)
    }?.index ?: fallback
}

@Composable
private fun EmojiChoiceChip(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val chipColor = if (selected) {
        stampAdaptiveColor(
            light = Color(0xFFF7F0E1),
            dark = MaterialTheme.colorScheme.surfaceVariant
        )
    } else {
        stampAdaptiveColor(
            light = Color(0xFFF7F1E5),
            dark = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    }
    val chipBorderColor = if (selected) {
        stampAdaptiveColor(
            light = Color(0xFF181512),
            dark = MaterialTheme.colorScheme.onSurface
        )
    } else {
        stampAdaptiveColor(
            light = Color(0xFFE5DCCB),
            dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
        )
    }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(shape)
            .background(chipColor)
            .border(
                width = if (selected) 2.2.dp else 1.dp,
                color = chipBorderColor,
                shape = shape
            )
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 24.sp)
    }
}

@Composable
private fun DeleteConfirmPanel(
    visible: Boolean,
    title: String,
    body: String,
    confirmLabel: String,
    cancelLabel: String,
    anchorBounds: Rect?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val panelProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) {
            tween(
                durationMillis = 640,
                easing = CubicBezierEasing(0.14f, 0.9f, 0.2f, 1f)
            )
        } else {
            tween(
                durationMillis = 430,
                easing = CubicBezierEasing(0.46f, 0.02f, 0.24f, 1f)
            )
        },
        label = "delete_panel_progress"
    )
    val travelProgress = if (visible) {
        CubicBezierEasing(0.16f, 0.86f, 0.22f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.44f, 0.06f, 0.78f, 0.34f).transform(panelProgress)
    }
    val sizeProgress = if (visible) {
        CubicBezierEasing(0.2f, 0.86f, 0.22f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.48f, 0f, 0.74f, 0.22f).transform(panelProgress)
    }
    val shapeProgress = if (visible) {
        CubicBezierEasing(0.22f, 0.78f, 0.24f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.44f, 0f, 0.74f, 0.2f).transform(panelProgress)
    }
    val colorProgress = if (visible) {
        CubicBezierEasing(0.24f, 0.8f, 0.24f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.46f, 0f, 0.72f, 0.2f).transform(panelProgress)
    }
    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible) 0.2f else 0f,
        animationSpec = if (visible) {
            tween(
                durationMillis = 360,
                easing = CubicBezierEasing(0.24f, 0.84f, 0.26f, 1f)
            )
        } else {
            tween(durationMillis = 260, easing = LinearEasing)
        },
        label = "delete_panel_backdrop_alpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) {
            tween(
                durationMillis = 360,
                delayMillis = 220,
                easing = CubicBezierEasing(0.2f, 0.92f, 0.2f, 1f)
            )
        } else {
            tween(durationMillis = 160, easing = LinearEasing)
        },
        label = "delete_panel_content_alpha"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                stampAdaptiveColor(
                    light = Color(0xFF120F0B),
                    dark = Color.Black
                ).copy(alpha = backdropAlpha)
            )
            .noRippleClickable(enabled = visible, onClick = onDismiss)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val collapsedWidthPx = (anchorBounds?.width
                ?: with(density) { 44.dp.toPx() })
                .coerceAtLeast(with(density) { 20.dp.toPx() })
            val collapsedHeightPx = (anchorBounds?.height
                ?: with(density) { 44.dp.toPx() })
                .coerceAtLeast(with(density) { 20.dp.toPx() })
            val collapsedCenterX = anchorBounds?.center?.x
                ?: with(density) { maxWidth.toPx() * 0.5f }
            val collapsedCenterY = anchorBounds?.center?.y
                ?: with(density) { maxHeight.toPx() * 0.5f }
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val horizontalPaddingPx = with(density) { 14.dp.toPx() }
            val topSafePaddingPx = with(density) { 72.dp.toPx() }
            val bottomSafePaddingPx = with(density) { 22.dp.toPx() }
            val expandedWidthPx = minOf(
                with(density) { 332.dp.toPx() },
                (maxWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(with(density) { 280.dp.toPx() })
            )
            val expandedHeightPx = minOf(
                with(density) { 216.dp.toPx() },
                (maxHeightPx - topSafePaddingPx - bottomSafePaddingPx)
                    .coerceAtLeast(with(density) { 198.dp.toPx() })
            )
            val expandedCenterX = maxWidthPx * 0.5f
            val expandedTop = (maxHeightPx - expandedHeightPx) * 0.5f
            val expandedCenterY = expandedTop + expandedHeightPx * 0.5f
            val widthMidPx = lerpFloat(collapsedWidthPx, expandedWidthPx, 0.34f)
            val heightMidPx = lerpFloat(collapsedHeightPx, expandedHeightPx, 0.24f)
            val animatedWidthPx = stagedLerpFloat(
                start = collapsedWidthPx,
                midpoint = widthMidPx,
                end = expandedWidthPx,
                fraction = sizeProgress,
                split = 0.42f
            )
            val animatedHeightPx = stagedLerpFloat(
                start = collapsedHeightPx,
                midpoint = heightMidPx,
                end = expandedHeightPx,
                fraction = sizeProgress,
                split = 0.48f
            )
            val collapsedLeftPx = collapsedCenterX - collapsedWidthPx * 0.5f
            val collapsedTopPx = collapsedCenterY - collapsedHeightPx * 0.5f
            val expandedLeftPx = expandedCenterX - expandedWidthPx * 0.5f
            val expandedTopPx = expandedCenterY - expandedHeightPx * 0.5f
            val arcOffsetX = with(density) { 14.dp.toPx() } * if (expandedLeftPx >= collapsedLeftPx) 1f else -1f
            val controlLeftPx = collapsedLeftPx + ((expandedLeftPx - collapsedLeftPx) * 0.52f) + arcOffsetX
            val controlTopPx = collapsedTopPx + ((expandedTopPx - collapsedTopPx) * 0.38f) - with(density) { 20.dp.toPx() }
            val animatedLeftPx = quadraticBezierFloat(
                start = collapsedLeftPx,
                control = controlLeftPx,
                end = expandedLeftPx,
                fraction = travelProgress
            )
            val animatedTopPx = quadraticBezierFloat(
                start = collapsedTopPx,
                control = controlTopPx,
                end = expandedTopPx,
                fraction = travelProgress
            )
            val collapsedCornerRadiusPx = minOf(collapsedWidthPx, collapsedHeightPx) * 0.5f
            val expandedCornerRadiusPx = with(density) { 30.dp.toPx() }
            val baseCornerRadiusPx = lerpFloat(collapsedCornerRadiusPx, expandedCornerRadiusPx, shapeProgress)
            val shapeFlowPulse = triangularPulse(
                fraction = shapeProgress,
                center = if (visible) 0.42f else 0.58f,
                width = 0.86f
            )
            val horizontalBias = when {
                expandedLeftPx - collapsedLeftPx > 1f -> 0.34f
                expandedLeftPx - collapsedLeftPx < -1f -> -0.34f
                else -> 0f
            }
            val verticalBias = when {
                expandedTopPx - collapsedTopPx > 1f -> 0.22f
                expandedTopPx - collapsedTopPx < -1f -> -0.22f
                else -> 0f
            }
            val panelShape = fluidRoundedCornerShape(
                density = density,
                baseRadiusPx = baseCornerRadiusPx,
                pulse = shapeFlowPulse,
                horizontalBias = horizontalBias,
                verticalBias = verticalBias
            )
            val squishScaleX = 1f + (shapeFlowPulse * 0.038f)
            val squishScaleY = 1f - (shapeFlowPulse * 0.024f)
            val collapsedSurfaceColor = stampAdaptiveColor(
                light = Color.White.copy(alpha = 0.9f),
                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
            )
            val expandedSurfaceColor = stampAdaptiveColor(
                light = Color(0xFFF9F4E9),
                dark = MaterialTheme.colorScheme.surface
            )
            val collapsedBorderColor = stampAdaptiveColor(
                light = Color(0xFFD9CFBF),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
            )
            val expandedBorderColor = stampAdaptiveColor(
                light = Color(0xFFE1D8C6),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            )
            val panelSurfaceColor = lerp(collapsedSurfaceColor, expandedSurfaceColor, colorProgress)
            val panelBorderColor = lerp(collapsedBorderColor, expandedBorderColor, colorProgress)
            val panelShellAlpha = if (visible) {
                val reveal = ((panelProgress - 0.04f) / 0.34f).coerceIn(0f, 1f)
                CubicBezierEasing(0.18f, 0.86f, 0.24f, 1f).transform(reveal)
            } else {
                CubicBezierEasing(0.38f, 0f, 0.66f, 1f).transform(panelProgress)
            }
            val sourceVisualAlpha = if (visible) {
                val phase = (panelProgress / 0.14f).coerceIn(0f, 1f)
                1f - CubicBezierEasing(0.32f, 0f, 0.88f, 1f).transform(phase)
            } else {
                val phase = ((0.18f - panelProgress) / 0.18f).coerceIn(0f, 1f)
                CubicBezierEasing(0.06f, 0.74f, 0.2f, 1f).transform(phase)
            }
            val sourceUniformScale = minOf(
                (animatedWidthPx / collapsedWidthPx).coerceAtLeast(1f),
                (animatedHeightPx / collapsedHeightPx).coerceAtLeast(1f)
            )
            val sourceScale = if (visible) {
                val phase = (panelProgress / 0.14f).coerceIn(0f, 1f)
                val cappedTarget = sourceUniformScale.coerceAtMost(1.52f)
                lerpFloat(1f, cappedTarget, CubicBezierEasing(0.16f, 0.82f, 0.24f, 1f).transform(phase))
            } else {
                val phase = ((0.18f - panelProgress) / 0.18f).coerceIn(0f, 1f)
                val cappedTarget = sourceUniformScale.coerceAtMost(1.46f)
                lerpFloat(cappedTarget, 1f, CubicBezierEasing(0.2f, 0.72f, 0.2f, 1f).transform(phase))
            }
            val sourceWidthDp = with(density) { (collapsedWidthPx * sourceScale).toDp() }
            val sourceHeightDp = with(density) { (collapsedHeightPx * sourceScale).toDp() }
            val panelSurfaceColorWithAlpha = panelSurfaceColor.copy(alpha = panelSurfaceColor.alpha * panelShellAlpha)
            val panelBorderColorWithAlpha = panelBorderColor.copy(alpha = panelBorderColor.alpha * panelShellAlpha)
            val contentLift by animateDpAsState(
                targetValue = if (visible) 0.dp else 14.dp,
                animationSpec = if (visible) {
                    tween(
                        durationMillis = 340,
                        delayMillis = 170,
                        easing = CubicBezierEasing(0.22f, 0.88f, 0.24f, 1f)
                    )
                } else {
                    tween(
                        durationMillis = 190,
                        easing = CubicBezierEasing(0.4f, 0f, 0.7f, 0.2f)
                    )
                },
                label = "delete_panel_content_lift"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { animatedLeftPx.toDp() },
                        y = with(density) { animatedTopPx.toDp() }
                    )
                    .width(with(density) { animatedWidthPx.toDp() })
                    .height(with(density) { animatedHeightPx.toDp() })
                    .graphicsLayer {
                        scaleX = squishScaleX
                        scaleY = squishScaleY
                    }
                    .clip(panelShape)
                    .background(panelSurfaceColorWithAlpha)
                    .border(
                        1.dp,
                        panelBorderColorWithAlpha,
                        panelShape
                    )
                    .noRippleClickable { }
            ) {
                if (sourceVisualAlpha > 0.001f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(sourceWidthDp)
                            .height(sourceHeightDp)
                            .graphicsLayer { alpha = sourceVisualAlpha }
                    ) {
                        DeleteTriggerMorphSource(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 22.dp, vertical = 18.dp)
                        .graphicsLayer {
                            alpha = contentAlpha * panelShellAlpha
                            translationY = contentLift.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        color = stampAdaptiveColor(
                            light = Color(0xFF1C1915),
                            dark = MaterialTheme.colorScheme.onSurface
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = body,
                        color = stampAdaptiveColor(
                            light = Color(0xFF6B635A),
                            dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        ),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.weight(0.42f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = stampAdaptiveColor(
                                light = Color(0xFFF0EBE2),
                                dark = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(STAMP_DIALOG_ACTION_BUTTON_HEIGHT)
                                .noRippleClickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = cancelLabel,
                                    color = stampAdaptiveColor(
                                        light = Color(0xFF6B635A),
                                        dark = MaterialTheme.colorScheme.onSurface
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFB42318),
                            modifier = Modifier
                                .weight(1f)
                                .height(STAMP_DIALOG_ACTION_BUTTON_HEIGHT)
                                .noRippleClickable { onConfirm() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = confirmLabel,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiFilterChip(
    emoji: String?,
    allLabel: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val chipColor = stampAdaptiveColor(
        light = Color.White.copy(alpha = 0.9f),
        dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    )
    val chipBorderColor = if (selected) {
        stampAdaptiveColor(
            light = Color(0xFF181512),
            dark = MaterialTheme.colorScheme.onSurface
        )
    } else {
        stampAdaptiveColor(
            light = Color(0xFFD7CFBF),
            dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
        )
    }
    Box(
        modifier = Modifier
            .background(chipColor, shape)
            .border(
                width = if (selected) 2.1.dp else 1.dp,
                color = chipBorderColor,
                shape = shape
            )
            .noRippleClickable(onClick = onClick)
    ) {
        Text(
            text = emoji ?: allLabel,
            color = stampAdaptiveColor(
                light = Color(0xFF3B362D),
                dark = MaterialTheme.colorScheme.onSurface
            ),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}


@Composable
private fun ThanksDialogPanel(
    visible: Boolean,
    avatarUri: String,
    title: String,
    body: String,
    confirmLabel: String,
    anchorBounds: Rect?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val panelProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) {
            tween(
                durationMillis = 760,
                easing = CubicBezierEasing(0.12f, 0.9f, 0.2f, 1f)
            )
        } else {
            tween(
                durationMillis = 500,
                easing = CubicBezierEasing(0.46f, 0.02f, 0.24f, 1f)
            )
        },
        label = "thanks_panel_progress"
    )
    val travelProgress = if (visible) {
        CubicBezierEasing(0.14f, 0.88f, 0.2f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.42f, 0.08f, 0.78f, 0.34f).transform(panelProgress)
    }
    val widthProgress = if (visible) {
        CubicBezierEasing(0.18f, 0.9f, 0.24f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.46f, 0f, 0.74f, 0.22f).transform(panelProgress)
    }
    val heightProgress = if (visible) {
        CubicBezierEasing(0.26f, 0.78f, 0.24f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.48f, 0f, 0.74f, 0.24f).transform(panelProgress)
    }
    val shapeProgress = if (visible) {
        CubicBezierEasing(0.2f, 0.8f, 0.24f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.46f, 0f, 0.72f, 0.2f).transform(panelProgress)
    }
    val colorProgress = if (visible) {
        CubicBezierEasing(0.2f, 0.82f, 0.24f, 1f).transform(panelProgress)
    } else {
        CubicBezierEasing(0.46f, 0f, 0.72f, 0.2f).transform(panelProgress)
    }
    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible) 0.2f else 0f,
        animationSpec = if (visible) {
            tween(
                durationMillis = 420,
                easing = CubicBezierEasing(0.24f, 0.84f, 0.24f, 1f)
            )
        } else {
            tween(durationMillis = 290, easing = LinearEasing)
        },
        label = "thanks_panel_backdrop_alpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) {
            tween(
                durationMillis = 420,
                delayMillis = 290,
                easing = CubicBezierEasing(0.2f, 0.92f, 0.2f, 1f)
            )
        } else {
            tween(durationMillis = 170, easing = LinearEasing)
        },
        label = "thanks_panel_content_alpha"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                stampAdaptiveColor(
                    light = Color(0xFF120F0B),
                    dark = Color.Black
                ).copy(alpha = backdropAlpha)
            )
            .noRippleClickable(enabled = visible, onClick = onDismiss)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val collapsedWidthPx = (anchorBounds?.width
                ?: with(density) { 62.dp.toPx() })
                .coerceAtLeast(with(density) { 10.dp.toPx() })
            val collapsedHeightPx = (anchorBounds?.height
                ?: with(density) { 18.dp.toPx() })
                .coerceAtLeast(with(density) { 6.dp.toPx() })
            val collapsedCenterX = anchorBounds?.center?.x
                ?: with(density) { maxWidth.toPx() * 0.5f }
            val collapsedCenterY = anchorBounds?.center?.y
                ?: with(density) { maxHeight.toPx() * 0.14f }
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val horizontalPaddingPx = with(density) { 14.dp.toPx() }
            val topSafePaddingPx = with(density) { 72.dp.toPx() }
            val bottomSafePaddingPx = with(density) { 22.dp.toPx() }
            val expandedWidthPx = minOf(
                with(density) { 340.dp.toPx() },
                (maxWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(with(density) { 280.dp.toPx() })
            )
            val expandedHeightPx = minOf(
                with(density) { 388.dp.toPx() },
                (maxHeightPx - topSafePaddingPx - bottomSafePaddingPx)
                    .coerceAtLeast(with(density) { 310.dp.toPx() })
            )
            val expandedCenterX = maxWidthPx * 0.5f
            val expandedTop = (maxHeightPx - expandedHeightPx) * 0.5f
            val expandedCenterY = expandedTop + expandedHeightPx * 0.5f
            val widthMidPx = lerpFloat(collapsedWidthPx, expandedWidthPx, 0.44f)
            val heightMidPx = lerpFloat(collapsedHeightPx, expandedHeightPx, 0.26f)
            val animatedWidthPx = stagedLerpFloat(
                start = collapsedWidthPx,
                midpoint = widthMidPx,
                end = expandedWidthPx,
                fraction = widthProgress,
                split = 0.34f
            )
            val animatedHeightPx = stagedLerpFloat(
                start = collapsedHeightPx,
                midpoint = heightMidPx,
                end = expandedHeightPx,
                fraction = heightProgress,
                split = 0.56f
            )
            val collapsedLeftPx = collapsedCenterX - collapsedWidthPx * 0.5f
            val collapsedTopPx = collapsedCenterY - collapsedHeightPx * 0.5f
            val expandedLeftPx = expandedCenterX - expandedWidthPx * 0.5f
            val expandedTopPx = expandedCenterY - expandedHeightPx * 0.5f
            val horizontalDirection = when {
                expandedLeftPx - collapsedLeftPx > 2f -> 1f
                expandedLeftPx - collapsedLeftPx < -2f -> -1f
                else -> 1f
            }
            val controlLeftPx = collapsedLeftPx +
                ((expandedLeftPx - collapsedLeftPx) * 0.5f) +
                (with(density) { 16.dp.toPx() } * horizontalDirection)
            val controlTopPx = collapsedTopPx +
                ((expandedTopPx - collapsedTopPx) * 0.34f) -
                with(density) { 24.dp.toPx() }
            val animatedLeftPx = quadraticBezierFloat(
                start = collapsedLeftPx,
                control = controlLeftPx,
                end = expandedLeftPx,
                fraction = travelProgress
            )
            val animatedTopPx = quadraticBezierFloat(
                start = collapsedTopPx,
                control = controlTopPx,
                end = expandedTopPx,
                fraction = travelProgress
            )
            val collapsedCornerRadiusPx = minOf(
                with(density) { 7.dp.toPx() },
                minOf(collapsedWidthPx, collapsedHeightPx) * 0.45f
            )
            val expandedCornerRadiusPx = with(density) { 30.dp.toPx() }
            val baseCornerRadiusPx = lerpFloat(collapsedCornerRadiusPx, expandedCornerRadiusPx, shapeProgress)
            val shapeFlowPulse = triangularPulse(
                fraction = shapeProgress,
                center = if (visible) 0.48f else 0.56f,
                width = 0.92f
            )
            val horizontalBias = when {
                expandedLeftPx - collapsedLeftPx > 1f -> 0.28f
                expandedLeftPx - collapsedLeftPx < -1f -> -0.28f
                else -> 0f
            }
            val verticalBias = when {
                expandedTopPx - collapsedTopPx > 1f -> 0.26f
                expandedTopPx - collapsedTopPx < -1f -> -0.26f
                else -> 0f
            }
            val panelShape = fluidRoundedCornerShape(
                density = density,
                baseRadiusPx = baseCornerRadiusPx,
                pulse = shapeFlowPulse,
                horizontalBias = horizontalBias,
                verticalBias = verticalBias
            )
            val squishScaleX = 1f + (shapeFlowPulse * 0.048f)
            val squishScaleY = 1f - (shapeFlowPulse * 0.03f)
            val collapsedSurfaceColor = stampAdaptiveColor(
                light = Color(0xFF1A1713),
                dark = Color(0xFF231D16)
            )
            val expandedSurfaceColor = stampAdaptiveColor(
                light = Color(0xFFF9F4E9),
                dark = MaterialTheme.colorScheme.surface
            )
            val collapsedBorderColor = stampAdaptiveColor(
                light = Color(0xFF2D271E),
                dark = Color(0xFF3A3228)
            )
            val expandedBorderColor = stampAdaptiveColor(
                light = Color(0xFFE1D8C6),
                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            )
            val panelSurfaceColor = lerp(collapsedSurfaceColor, expandedSurfaceColor, colorProgress)
            val panelBorderColor = lerp(collapsedBorderColor, expandedBorderColor, colorProgress)
            val panelShellAlpha = if (visible) {
                val reveal = ((panelProgress - 0.07f) / 0.38f).coerceIn(0f, 1f)
                CubicBezierEasing(0.16f, 0.84f, 0.24f, 1f).transform(reveal)
            } else {
                CubicBezierEasing(0.38f, 0f, 0.66f, 1f).transform(panelProgress)
            }
            val sourceVisualAlpha = if (visible) {
                val phase = (panelProgress / 0.18f).coerceIn(0f, 1f)
                1f - CubicBezierEasing(0.32f, 0f, 0.9f, 1f).transform(phase)
            } else {
                val phase = ((0.22f - panelProgress) / 0.22f).coerceIn(0f, 1f)
                CubicBezierEasing(0.06f, 0.74f, 0.2f, 1f).transform(phase)
            }
            val sourceUniformScale = minOf(
                (animatedWidthPx / collapsedWidthPx).coerceAtLeast(1f),
                (animatedHeightPx / collapsedHeightPx).coerceAtLeast(1f)
            )
            val sourceScale = if (visible) {
                val phase = (panelProgress / 0.18f).coerceIn(0f, 1f)
                val cappedTarget = sourceUniformScale.coerceAtMost(1.42f)
                lerpFloat(1f, cappedTarget, CubicBezierEasing(0.16f, 0.8f, 0.24f, 1f).transform(phase))
            } else {
                val phase = ((0.22f - panelProgress) / 0.22f).coerceIn(0f, 1f)
                val cappedTarget = sourceUniformScale.coerceAtMost(1.34f)
                lerpFloat(cappedTarget, 1f, CubicBezierEasing(0.2f, 0.72f, 0.2f, 1f).transform(phase))
            }
            val sourceWidthDp = with(density) { (collapsedWidthPx * sourceScale).toDp() }
            val sourceHeightDp = with(density) { (collapsedHeightPx * sourceScale).toDp() }
            val panelSurfaceColorWithAlpha = panelSurfaceColor.copy(alpha = panelSurfaceColor.alpha * panelShellAlpha)
            val panelBorderColorWithAlpha = panelBorderColor.copy(alpha = panelBorderColor.alpha * panelShellAlpha)
            val contentLift by animateDpAsState(
                targetValue = if (visible) 0.dp else 16.dp,
                animationSpec = if (visible) {
                    tween(
                        durationMillis = 400,
                        delayMillis = 220,
                        easing = CubicBezierEasing(0.22f, 0.88f, 0.24f, 1f)
                    )
                } else {
                    tween(
                        durationMillis = 200,
                        easing = CubicBezierEasing(0.42f, 0f, 0.72f, 0.22f)
                    )
                },
                label = "thanks_panel_content_lift"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { animatedLeftPx.toDp() },
                        y = with(density) { animatedTopPx.toDp() }
                    )
                    .width(with(density) { animatedWidthPx.toDp() })
                    .height(with(density) { animatedHeightPx.toDp() })
                    .graphicsLayer {
                        scaleX = squishScaleX
                        scaleY = squishScaleY
                    }
                    .clip(panelShape)
                    .background(panelSurfaceColorWithAlpha)
                    .border(
                        1.dp,
                        panelBorderColorWithAlpha,
                        panelShape
                    )
                    .noRippleClickable { }
            ) {
                if (sourceVisualAlpha > 0.001f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(sourceWidthDp)
                            .height(sourceHeightDp)
                            .graphicsLayer { alpha = sourceVisualAlpha }
                    ) {
                        ThanksCreditTriggerMorphSource(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.TopStart)
                        .padding(horizontal = 22.dp, vertical = 22.dp)
                        .graphicsLayer {
                            alpha = contentAlpha * panelShellAlpha
                            translationY = contentLift.toPx()
                        },
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = title,
                            color = stampAdaptiveColor(
                                light = Color(0xFF1C1915),
                                dark = MaterialTheme.colorScheme.onSurface
                            ),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "jeongyoon.design avatar",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    stampAdaptiveColor(
                                        light = Color(0xFFE1D8C6),
                                        dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                                    ),
                                    CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = body,
                            color = stampAdaptiveColor(
                                light = Color(0xFF6B635A),
                                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            ),
                            fontSize = 16.sp,
                            lineHeight = 25.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = stampAdaptiveColor(
                                light = Color(0xFFF0EBE2),
                                dark = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .width(116.dp)
                                .height(48.dp)
                                .noRippleClickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = confirmLabel,
                                    color = stampAdaptiveColor(
                                        light = Color(0xFF6B635A),
                                        dark = MaterialTheme.colorScheme.onSurface
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteTriggerMorphSource(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = stampAdaptiveColor(
            light = Color.White.copy(alpha = 0.88f),
            dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
        ),
        modifier = modifier
            .clip(CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = stampAdaptiveColor(
                    light = Color(0xFF1C1915),
                    dark = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun ThanksCreditTriggerMorphSource(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.is_credit),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}

@Composable
internal fun StampCard(
    uri: String?,
    previewBitmap: Bitmap? = null,
    skin: StampCalendarStampSkin? = null,
    categoryEmoji: String? = null,
    aspectRatio: Float = STAMP_CARD_ASPECT,
    rotation: Float,
    shadowStrength: Float = 0.95f,
    outlineStrength: Float = 0f,
    cardPadding: Dp = 12.dp,
    motionOptimized: Boolean = false,
    entryRevealEnabled: Boolean = true,
    onResolvedPreviewBitmap: ((Bitmap) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onBoundsChange: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val localContext = LocalContext.current
    val density = LocalDensity.current
    val resolvedShadowStrength = shadowStrength.coerceIn(0f, 1.6f)
    val resolvedOutlineStrength = outlineStrength.coerceIn(0f, 1.4f)
    val rotationFactor = (rotation / 10f).coerceIn(-1f, 1f)
    val castShadowOffsetX = ((1.2f + (rotationFactor * 0.85f)) * resolvedShadowStrength).dp
    val castShadowOffsetY = ((4.8f + (abs(rotation) * 0.08f)) * resolvedShadowStrength).dp
    val castShadowBlur = (8.6f * resolvedShadowStrength).dp
    val castShadowAlpha = (0.28f * resolvedShadowStrength).coerceIn(0f, 0.42f)
    val contactShadowOffsetX = castShadowOffsetX * 0.42f
    val contactShadowOffsetY = (1.4f * resolvedShadowStrength).dp
    val contactShadowBlur = (1.2f * resolvedShadowStrength).dp
    val contactShadowAlpha = (0.2f * resolvedShadowStrength).coerceIn(0f, 0.3f)
    val edgeShadeOffsetX = (0.55f * resolvedShadowStrength).dp
    val edgeShadeOffsetY = (0.72f * resolvedShadowStrength).dp
    val edgeShadeAlpha = (0.13f * resolvedShadowStrength).coerceIn(0f, 0.19f)
    val edgeHighlightOffsetX = (-0.45f * resolvedShadowStrength).dp
    val edgeHighlightOffsetY = (-0.52f * resolvedShadowStrength).dp
    val edgeHighlightAlpha = (0.11f * resolvedShadowStrength).coerceIn(0f, 0.17f)
    val castShadowFilter = remember(castShadowAlpha) {
        ColorFilter.tint(
            Color(0xFF2E261C).copy(alpha = castShadowAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val contactShadowFilter = remember(contactShadowAlpha) {
        ColorFilter.tint(
            Color(0xFF3A3125).copy(alpha = contactShadowAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val edgeShadeFilter = remember(edgeShadeAlpha) {
        ColorFilter.tint(
            Color(0xFF211A13).copy(alpha = edgeShadeAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val edgeHighlightFilter = remember(edgeHighlightAlpha) {
        ColorFilter.tint(
            Color.White.copy(alpha = edgeHighlightAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val outlineSpread = (0.28f * resolvedOutlineStrength).dp
    val outlineBlur = (0.38f * resolvedOutlineStrength).dp
    val outlineAlpha = (0.48f * resolvedOutlineStrength).coerceIn(0f, 0.72f)
    val outlineFilter = remember(outlineAlpha) {
        ColorFilter.tint(
            Color.White.copy(alpha = outlineAlpha),
            blendMode = BlendMode.SrcIn
        )
    }
    val showSecondaryShadowLayers = !motionOptimized
    val showOutlineLayers = !motionOptimized && resolvedOutlineStrength > 0f
    val contentFilterQuality = if (motionOptimized) FilterQuality.Medium else FilterQuality.High
    val resolvedPreviewBitmap = previewBitmap?.takeUnless { it.isRecycled }
    val normalizedSkin = remember(skin) { skin.normalized() }
    val normalizedCategoryEmoji = remember(categoryEmoji) {
        categoryEmoji
            ?.trim()
            ?.take(8)
            ?.takeIf { it.isNotEmpty() }
    }
    val skinPreviewCacheKey = remember(uri, normalizedSkin, normalizedCategoryEmoji) {
        stampSkinPreviewCacheKey(
            uri = uri,
            skin = normalizedSkin,
            categoryEmoji = normalizedCategoryEmoji
        )
    }
    val cachedRenderedSkinBitmap = remember(skinPreviewCacheKey) {
        cachedStampSkinPreviewBitmap(skinPreviewCacheKey)
    }
    val remotePainter = if (
        uri != null &&
        resolvedPreviewBitmap == null &&
        normalizedSkin.isOriginalStyle()
    ) {
        rememberAsyncImagePainter(model = uri)
    } else {
        null
    }
    val remotePainterState = remotePainter?.state
    val remotePainterFailed = remotePainterState is AsyncImagePainter.State.Error
    val remotePreviewBitmap = remember(remotePainterState) {
        (remotePainterState as? AsyncImagePainter.State.Success)
            ?.result
            ?.drawable
            ?.toSharedTransitionPreviewBitmap()
    }
    val parsedUri = remember(uri) {
        uri?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
    }
    val sourcePreviewBitmap = resolvedPreviewBitmap ?: remotePreviewBitmap
    val decodedSourceBitmap by produceState<Bitmap?>(
        initialValue = null,
        localContext,
        parsedUri,
        sourcePreviewBitmap,
        normalizedSkin
    ) {
        if (sourcePreviewBitmap != null || normalizedSkin.isOriginalStyle()) {
            value = sourcePreviewBitmap
            return@produceState
        }
        val targetUri = parsedUri ?: return@produceState
        value = loadStampSourceBitmap(
            context = localContext,
            uri = targetUri,
            maxSide = 1440
        )
    }
    val effectiveSourceBitmap = sourcePreviewBitmap ?: decodedSourceBitmap
    val renderedPreviewBitmap by produceState<Bitmap?>(
        initialValue = if (normalizedSkin.isOriginalStyle()) {
            effectiveSourceBitmap
        } else {
            cachedRenderedSkinBitmap
        },
        effectiveSourceBitmap,
        normalizedSkin,
        skinPreviewCacheKey,
        normalizedCategoryEmoji
    ) {
        val cachedBitmap = cachedStampSkinPreviewBitmap(skinPreviewCacheKey)
        val sourceBitmap = effectiveSourceBitmap?.takeUnless { it.isRecycled }
        value = when {
            normalizedSkin.isOriginalStyle() -> sourceBitmap
            sourceBitmap == null -> cachedBitmap
            cachedBitmap != null -> cachedBitmap
            else -> withContext(stampCardSkinRenderDispatcher) {
                renderStampCardSkinBitmap(
                    source = sourceBitmap,
                    skin = normalizedSkin,
                    categoryEmoji = normalizedCategoryEmoji,
                    context = localContext
                )
            }.also { rendered -> cacheStampSkinPreviewBitmap(skinPreviewCacheKey, rendered) }
        }
    }
    var previewBitmapReported by remember(uri, normalizedSkin) { mutableStateOf(false) }
    LaunchedEffect(uri, normalizedSkin, onResolvedPreviewBitmap) {
        if (onResolvedPreviewBitmap == null) {
            previewBitmapReported = false
        }
    }
    LaunchedEffect(
        uri,
        renderedPreviewBitmap,
        effectiveSourceBitmap,
        normalizedSkin,
        onResolvedPreviewBitmap
    ) {
        val callback = onResolvedPreviewBitmap ?: return@LaunchedEffect
        if (previewBitmapReported) return@LaunchedEffect
        val transitionPreviewBitmap = if (normalizedSkin.isOriginalStyle()) {
            renderedPreviewBitmap ?: effectiveSourceBitmap
        } else {
            renderedPreviewBitmap
        } ?: return@LaunchedEffect
        callback(transitionPreviewBitmap)
        previewBitmapReported = true
    }
    val imageReady = renderedPreviewBitmap != null || remotePainter != null
    val shouldAnimateEntryReveal =
        entryRevealEnabled && normalizedSkin.isOriginalStyle()
    val contentRevealAlpha = if (shouldAnimateEntryReveal) {
        val animatedContentReveal by animateFloatAsState(
            targetValue = if (imageReady) 1f else 0f,
            animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing),
            label = "stamp_card_content_reveal"
        )
        animatedContentReveal
    } else {
        1f
    }
    val shadowRevealAlpha = if (shouldAnimateEntryReveal) {
        val animatedShadowReveal by animateFloatAsState(
            targetValue = if (imageReady) 1f else 0f,
            animationSpec = tween(durationMillis = 190, delayMillis = 34, easing = FastOutSlowInEasing),
            label = "stamp_card_shadow_reveal"
        )
        animatedShadowReveal
    } else {
        1f
    }
    Box(
        modifier = modifier
            .padding(horizontal = cardPadding, vertical = cardPadding)
            .onGloballyPositioned { coordinates ->
                val outer = coordinates.boundsInRoot()
                val insetX = with(density) { cardPadding.toPx() }
                val insetY = with(density) { cardPadding.toPx() }
                val left = outer.left + insetX
                val top = outer.top + insetY
                val right = (outer.right - insetX).coerceAtLeast(left + 1f)
                val bottom = (outer.bottom - insetY).coerceAtLeast(top + 1f)
                onBoundsChange?.invoke(
                    Rect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom
                    )
                )
            }
            .then(
                if (onClick != null) {
                    Modifier.noRippleClickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
        ) {
            val renderedBitmap = renderedPreviewBitmap
            if (renderedBitmap != null) {
                StampCardImageLayer(
                    previewBitmap = renderedBitmap,
                    colorFilter = castShadowFilter,
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = castShadowOffsetX, y = castShadowOffsetY)
                        .blur(castShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer { alpha = shadowRevealAlpha }
                )
                if (showSecondaryShadowLayers) {
                    StampCardImageLayer(
                        previewBitmap = renderedBitmap,
                        colorFilter = contactShadowFilter,
                        filterQuality = FilterQuality.Low,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = contactShadowOffsetX, y = contactShadowOffsetY)
                            .blur(contactShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = shadowRevealAlpha }
                    )
                    StampCardImageLayer(
                        previewBitmap = renderedBitmap,
                        colorFilter = edgeShadeFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = edgeShadeOffsetX, y = edgeShadeOffsetY)
                            .graphicsLayer { alpha = shadowRevealAlpha }
                    )
                    StampCardImageLayer(
                        previewBitmap = renderedBitmap,
                        colorFilter = edgeHighlightFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = edgeHighlightOffsetX, y = edgeHighlightOffsetY)
                            .graphicsLayer { alpha = shadowRevealAlpha }
                    )
                }
                if (showOutlineLayers) {
                    StampCardImageLayer(
                        previewBitmap = renderedBitmap,
                        colorFilter = outlineFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = outlineSpread, y = 0.dp)
                            .blur(outlineBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = contentRevealAlpha }
                    )
                    StampCardImageLayer(
                        previewBitmap = renderedBitmap,
                        colorFilter = outlineFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = (-outlineSpread), y = 0.dp)
                            .blur(outlineBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = contentRevealAlpha }
                    )
                    StampCardImageLayer(
                        previewBitmap = renderedBitmap,
                        colorFilter = outlineFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 0.dp, y = outlineSpread)
                            .blur(outlineBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = contentRevealAlpha }
                    )
                    StampCardImageLayer(
                        previewBitmap = renderedBitmap,
                        colorFilter = outlineFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 0.dp, y = (-outlineSpread))
                            .blur(outlineBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = contentRevealAlpha }
                    )
                }
                StampCardImageLayer(
                    previewBitmap = renderedBitmap,
                    colorFilter = null,
                    filterQuality = contentFilterQuality,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = contentRevealAlpha }
                )
            } else if (remotePainter != null && !remotePainterFailed) {
                StampCardPainterLayer(
                    painter = remotePainter,
                    colorFilter = castShadowFilter,
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = castShadowOffsetX, y = castShadowOffsetY)
                        .blur(castShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer { alpha = shadowRevealAlpha }
                )
                if (showSecondaryShadowLayers) {
                    StampCardPainterLayer(
                        painter = remotePainter,
                        colorFilter = contactShadowFilter,
                        filterQuality = FilterQuality.Low,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = contactShadowOffsetX, y = contactShadowOffsetY)
                            .blur(contactShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = shadowRevealAlpha }
                    )
                    StampCardPainterLayer(
                        painter = remotePainter,
                        colorFilter = edgeShadeFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = edgeShadeOffsetX, y = edgeShadeOffsetY)
                            .graphicsLayer { alpha = shadowRevealAlpha }
                    )
                    StampCardPainterLayer(
                        painter = remotePainter,
                        colorFilter = edgeHighlightFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = edgeHighlightOffsetX, y = edgeHighlightOffsetY)
                            .graphicsLayer { alpha = shadowRevealAlpha }
                    )
                }
                if (showOutlineLayers) {
                    StampCardPainterLayer(
                        painter = remotePainter,
                        colorFilter = outlineFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = outlineSpread, y = 0.dp)
                            .blur(outlineBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = contentRevealAlpha }
                    )
                    StampCardPainterLayer(
                        painter = remotePainter,
                        colorFilter = outlineFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = (-outlineSpread), y = 0.dp)
                            .blur(outlineBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = contentRevealAlpha }
                    )
                    StampCardPainterLayer(
                        painter = remotePainter,
                        colorFilter = outlineFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 0.dp, y = outlineSpread)
                            .blur(outlineBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = contentRevealAlpha }
                    )
                    StampCardPainterLayer(
                        painter = remotePainter,
                        colorFilter = outlineFilter,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 0.dp, y = (-outlineSpread))
                            .blur(outlineBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer { alpha = contentRevealAlpha }
                    )
                }
                StampCardPainterLayer(
                    painter = remotePainter,
                    colorFilter = null,
                    filterQuality = contentFilterQuality,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = contentRevealAlpha }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            stampAdaptiveColor(
                                light = Color(0xFFF0EADC),
                                dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun StampCardImageLayer(
    previewBitmap: Bitmap,
    colorFilter: ColorFilter?,
    filterQuality: FilterQuality = FilterQuality.High,
    modifier: Modifier = Modifier
) {
    if (previewBitmap.isRecycled || previewBitmap.width <= 0 || previewBitmap.height <= 0) {
        return
    }
    Image(
        bitmap = previewBitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = filterQuality,
        colorFilter = colorFilter,
        modifier = modifier
    )
}

@Composable
private fun StampCardPainterLayer(
    painter: Painter,
    colorFilter: ColorFilter?,
    filterQuality: FilterQuality = FilterQuality.High,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = colorFilter,
        modifier = modifier
    )
}

@Composable
private fun StampBottomDock(
    copy: StampCalendarCopy,
    selectedMode: StampViewMode,
    onModeSelected: (StampViewMode) -> Unit,
    onOpenCamera: () -> Unit,
    onCameraButtonBoundsChange: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(StampViewMode.Calendar, StampViewMode.Timeline, StampViewMode.Wall)
    }
    val selectedIndex = tabs.indexOf(selectedMode).coerceAtLeast(0)
    var requestedMode by remember { mutableStateOf(selectedMode) }
    LaunchedEffect(selectedIndex) {
        requestedMode = selectedMode
    }
    val requestDockSelection: (Int) -> Unit = { index ->
        val safeIndex = index.coerceIn(0, tabs.lastIndex)
        val targetMode = tabs[safeIndex]
        if (targetMode != requestedMode) {
            requestedMode = targetMode
            onModeSelected(targetMode)
        }
    }

    val glassBackdrop = LocalLiquidGlassBackdrop.current
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (glassBackdrop != null) {
                Color.Transparent
            } else {
                stampAdaptiveColor(
                    light = Color(0xFF17130E).copy(alpha = 0.88f),
                    dark = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                )
            },
            modifier = Modifier
                .size(64.dp)
                .onGloballyPositioned { coordinates ->
                    onCameraButtonBoundsChange(coordinates.boundsInRoot())
                }
                .clip(CircleShape)
                .then(
                    if (glassBackdrop != null) {
                        Modifier.liquidGlassControl(
                            backdrop = glassBackdrop,
                            shape = CircleShape,
                            surfaceColor = if (isLightTheme) Color(0xFF17130E).copy(alpha = 0.62f) else Color.White.copy(alpha = 0.12f)
                        )
                    } else {
                        Modifier
                    }
                )
                .noRippleClickable(onClick = onOpenCamera)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = copy.camera,
                    tint = stampAdaptiveColor(
                        light = Color(0xFFF2ECE2),
                        dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
                    ),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        if (glassBackdrop != null) {
            StampLiquidGlassDockTabs(
                backdrop = glassBackdrop,
                copy = copy,
                tabs = tabs,
                selectedIndex = selectedIndex,
                isLightTheme = isLightTheme,
                onSelect = requestDockSelection,
                modifier = Modifier.weight(1f)
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(35.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(35.dp))
                    .background(
                        stampAdaptiveColor(
                            light = Color(0xFFFCFAF4),
                            dark = MaterialTheme.colorScheme.surface
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = stampAdaptiveColor(
                            light = Color(0x1A181512),
                            dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                        ),
                        shape = RoundedCornerShape(35.dp)
                    )
                    .padding(8.dp)
            ) {
                val itemWidth = maxWidth / tabs.size
                val offset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "stamp_nav_offset_non_liquid"
                )
                Box(
                    modifier = Modifier
                        .offset(x = offset)
                        .width(itemWidth)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                )
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val selected = tab == selectedMode
                        val label = when (tab) {
                            StampViewMode.Calendar -> copy.tabCalendar
                            StampViewMode.Timeline -> copy.tabTimeline
                            StampViewMode.Wall -> copy.tabWall
                        }
                        val icon = when (tab) {
                            StampViewMode.Calendar -> Icons.Outlined.CalendarToday
                            StampViewMode.Timeline -> StampTimelineHourglassIcon
                            StampViewMode.Wall -> Icons.Outlined.Apps
                        }
                        val color = if (selected) {
                            stampAdaptiveColor(
                                light = Color(0xFF181512),
                                dark = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            stampAdaptiveColor(
                                light = Color(0xFF6B6558),
                                dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(26.dp))
                                .noRippleClickable { if (!selected) requestDockSelection(tabs.indexOf(tab)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = color,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    label,
                                    color = color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StampLiquidGlassDockTabs(
    backdrop: Backdrop,
    copy: StampCalendarCopy,
    tabs: List<StampViewMode>,
    selectedIndex: Int,
    isLightTheme: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedColor = stampAdaptiveColor(
        light = Color(0xFF181512),
        dark = MaterialTheme.colorScheme.onSurface
    )
    val idleColor = stampAdaptiveColor(
        light = Color(0xFF6B6558),
        dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    )
    ReferenceLiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = onSelect,
        backdrop = backdrop,
        tabsCount = tabs.size,
        modifier = modifier,
        containerShape = RoundedCornerShape(percent = 50),
        indicatorShape = RoundedCornerShape(percent = 50),
        containerColor = liquidGlassSurfaceColor(isLightTheme),
        accentColor = selectedColor,
        isLightTheme = isLightTheme
    ) {
        val accentLayer = LocalReferenceLiquidBottomTabAccentLayer.current
        tabs.forEachIndexed { index, tab ->
            val label = when (tab) {
                StampViewMode.Calendar -> copy.tabCalendar
                StampViewMode.Timeline -> copy.tabTimeline
                StampViewMode.Wall -> copy.tabWall
            }
            val icon = when (tab) {
                StampViewMode.Calendar -> Icons.Outlined.CalendarToday
                StampViewMode.Timeline -> StampTimelineHourglassIcon
                StampViewMode.Wall -> Icons.Outlined.Apps
            }
            val color = if (index == selectedIndex || accentLayer) selectedColor else idleColor
            ReferenceLiquidBottomTab(onClick = { onSelect(index) }) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    label,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

internal fun Modifier.paperGridBackground(
    backgroundColor: Color = Color(0xFFF6F2E7),
    lineColor: Color = Color(0xFFE1DAC9).copy(alpha = 0.44f),
    spacing: Dp = 54.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val resolvedBackgroundColor = if (isDark && backgroundColor == Color(0xFFF6F2E7)) {
        MaterialTheme.colorScheme.background
    } else {
        backgroundColor
    }
    val resolvedLineColor = if (isDark && lineColor == Color(0xFFE1DAC9).copy(alpha = 0.44f)) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    } else {
        lineColor
    }
    this
        .background(resolvedBackgroundColor)
        .drawWithCache {
        val drawSize = size
        val step = spacing.toPx().coerceAtLeast(8f)
        val verticalLines = buildList {
            var x = 0f
            while (x <= drawSize.width) {
                add(x)
                x += step
            }
        }
        val horizontalLines = buildList {
            var y = 0f
            while (y <= drawSize.height) {
                add(y)
                y += step
            }
        }
        onDrawBehind {
            verticalLines.forEach { x ->
                drawLine(resolvedLineColor, Offset(x, 0f), Offset(x, drawSize.height), 1f)
            }
            horizontalLines.forEach { y ->
                drawLine(resolvedLineColor, Offset(0f, y), Offset(drawSize.width, y), 1f)
            }
        }
    }
}

private fun String.limitWallNoteChars(maxChars: Int = 18): String {
    val trimmed = trim()
    if (trimmed.length <= maxChars) return trimmed
    return "${trimmed.take(maxChars)}..."
}

private fun buildWallItems(
    entries: List<StampCalendarDayEntry>,
    aspectByUri: Map<String, Float>,
    selectedEmoji: String?,
    stableSeedByUri: Map<String, String>
): List<StampWallItemSpec> {
    val wallItems = sortStampCalendarEntriesDescending(entries)
        .flatMap { entry ->
            entry.uris.asReversed().map { uri ->
                StampDisplayItem(
                    day = stampCalendarDayOfMonth(entry.dayKey),
                    dayKey = entry.dayKey,
                    uri = uri,
                    emoji = entry.emojiByUri[uri],
                    note = entry.noteByUri[uri]
                        ?: if (entry.selectedUri == uri) entry.note else null
                )
            }
        }
        .filter { selectedEmoji == null || it.emoji == selectedEmoji }
    val resolvedItems = mutableListOf<StampWallItemSpec>()
    wallItems.forEachIndexed { index, item ->
            val row = index / 3
            val rowBoundItem = item.copy(wallRow = row)
            val seedUri = stableSeedByUri[item.uri] ?: item.uri
            val random = Random(seedUri.hashCode() * 1297 + item.day * 3571 + index * 97)
            val itemAspect = (aspectByUri[item.uri] ?: STAMP_CARD_ASPECT).coerceIn(0.55f, 1.45f)
            val column = index % 3
            val baseWidthFraction = listOf(0.256f, 0.264f, 0.256f)[column]
            val nearCircularOrSquare = itemAspect in 0.9f..1.12f
            val widthBoost = if (nearCircularOrSquare) 1.09f else 1.055f
            resolvedItems += StampWallItemSpec(
                item = rowBoundItem,
                column = column,
                row = row,
                aspectRatio = itemAspect,
                widthFraction = (baseWidthFraction * widthBoost).coerceAtMost(0.292f),
                xJitterFraction = (random.nextFloat() - 0.5f) * 0.0018f,
                yJitter = 0.dp,
                rotation = resolveStampWallRotation(
                    row = row,
                    column = column,
                    random = random,
                    existingItems = resolvedItems
                )
            )
        }
    return resolvedItems
}

private fun resolveWallPlacements(
    items: List<StampWallItemSpec>,
    containerWidth: Dp
): List<StampDetailState> {
    val sortedRows = items.groupBy { it.row }.toSortedMap()
    val placements = mutableListOf<StampDetailState>()
    var rowTop = 10.dp
    sortedRows.values.forEach { rowItems ->
        val widthByItem = rowItems.associateWith { item ->
            (containerWidth * item.widthFraction).coerceIn(96.dp, 138.dp)
        }
        val rowMaxHeight = rowItems.maxOfOrNull { item ->
            widthByItem.getValue(item) / item.aspectRatio
        } ?: 0.dp
        rowItems.forEach { item ->
            val width = widthByItem.getValue(item)
            val baseX = when (item.column) {
                0 -> containerWidth * 0.029f
                1 -> containerWidth * 0.352f
                else -> containerWidth * 0.675f
            }
            val safeX = (baseX + (containerWidth * item.xJitterFraction))
                .coerceIn(12.dp, (containerWidth - width - 12.dp).coerceAtLeast(12.dp))
            val itemHeight = width / item.aspectRatio
            val centeredY = rowTop + ((rowMaxHeight - itemHeight) / 2f)
            val placement = StampDetailState(
                item = item.item,
                startX = safeX,
                startY = centeredY + item.yJitter,
                startWidth = width,
                aspectRatio = item.aspectRatio,
                startRotation = item.rotation,
                shadowElevation = 0.dp,
                sourceCardPadding = 12.dp,
                sourceShadowStrength = 0.95f
            )
            placements.add(placement)
        }
        rowTop += rowMaxHeight + 20.dp
    }
    return placements
}

private fun resolveStampWallRotation(
    row: Int,
    column: Int,
    random: Random,
    existingItems: List<StampWallItemSpec>
): Float {
    val immediateNeighbors = existingItems.filter { item ->
        (item.row == row && item.column == column - 1) ||
            (item.column == column && item.row == row - 1)
    }
    val rotationCandidates = listOf(-3.1f, -2.2f, -1.3f, 1.3f, 2.2f, 3.1f).shuffled(random)
    rotationCandidates.firstOrNull { candidate ->
        immediateNeighbors.none { neighbor ->
            haveMatchingWallTilt(candidate, neighbor.rotation)
        }
    }?.let { return it }
    return rotationCandidates.maxByOrNull { candidate ->
        immediateNeighbors.sumOf { neighbor ->
            abs(abs(candidate) - abs(neighbor.rotation)).toDouble() +
                if (candidate * neighbor.rotation < 0f) 1.0 else 0.0
        }
    } ?: rotationCandidates.first()
}

private fun haveMatchingWallTilt(first: Float, second: Float): Boolean {
    if (first == 0f || second == 0f) return false
    val sameDirection = first * second > 0f
    val closeMagnitude = abs(abs(first) - abs(second)) < 0.95f
    return sameDirection && closeMagnitude
}

private fun resolveWallHeight(items: List<StampDetailState>): Dp {
    val maxBottom = items.maxOfOrNull { it.startY + (it.startWidth / it.aspectRatio.coerceIn(0.55f, 1.45f)) } ?: 0.dp
    return (maxBottom + 38.dp).coerceAtLeast(520.dp)
}

private fun findWallHitItem(
    tapOffset: Offset,
    items: List<StampDetailState>,
    hiddenUri: String?,
    density: androidx.compose.ui.unit.Density
): StampDetailState? {
    return items.asReversed().firstOrNull { item ->
        if (hiddenUri != null && item.item.uri == hiddenUri) return@firstOrNull false
        with(density) {
            val left = item.startX.toPx()
            val top = item.startY.toPx()
            val width = item.startWidth.toPx()
            val height = width / item.aspectRatio.coerceIn(0.55f, 1.45f)
            tapOffset.x in left..(left + width) && tapOffset.y in top..(top + height)
        }
    }
}

private fun buildDetailStateFromWallPlacement(
    placement: StampDetailState,
    wallLeftInRootPx: Float,
    wallTopInContentPx: Float,
    totalVerticalScrollPx: Float,
    density: androidx.compose.ui.unit.Density
): StampDetailState {
    return placement.copy(
        startX = with(density) { (wallLeftInRootPx + placement.startX.toPx()).toDp() },
        startY = with(density) {
            (wallTopInContentPx - totalVerticalScrollPx + placement.startY.toPx()).toDp()
        }
    )
}

internal fun StampDetailState.withLiveSourceBounds(
    bounds: Rect?,
    rotation: Float?,
    density: androidx.compose.ui.unit.Density
): StampDetailState {
    if (bounds == null) return this
    return copy(
        startX = with(density) { bounds.left.toDp() },
        startY = with(density) { bounds.top.toDp() },
        startWidth = with(density) { bounds.width.toDp() }.coerceAtLeast(84.dp),
        startRotation = rotation ?: startRotation
    )
}

private fun mergePendingStampIntoMonthEntries(
    entries: Map<Int, StampCalendarDayEntry>,
    pendingStamp: PendingStampInsertion?,
    monthKey: String
): Map<Int, StampCalendarDayEntry> {
    val pending = pendingStamp ?: return entries
    if (!pending.dayKey.startsWith("$monthKey-")) {
        return entries
    }
    val day = stampCalendarDayOfMonth(pending.dayKey)
    val merged = mergePendingStampIntoEntry(
        entry = entries[day],
        dayKey = pending.dayKey,
        pendingUri = pending.uri
    )
    return entries + (day to merged)
}

private fun mergePendingStampIntoAllEntries(
    entries: List<StampCalendarDayEntry>,
    pendingStamp: PendingStampInsertion?
): List<StampCalendarDayEntry> {
    val pending = pendingStamp ?: return entries
    val index = entries.indexOfFirst { it.dayKey == pending.dayKey }
    val mergedList = if (index >= 0) {
        entries.toMutableList().apply {
            this[index] = mergePendingStampIntoEntry(
                entry = this[index],
                dayKey = pending.dayKey,
                pendingUri = pending.uri
            )
        }
    } else {
        (entries + mergePendingStampIntoEntry(
            entry = null,
            dayKey = pending.dayKey,
            pendingUri = pending.uri
        )).toMutableList()
    }
    return sortStampCalendarEntriesDescending(mergedList)
}

private fun mergePendingStampIntoEntry(
    entry: StampCalendarDayEntry?,
    dayKey: String,
    pendingUri: String
): StampCalendarDayEntry {
    if (entry == null) {
        return StampCalendarDayEntry(
            dayKey = dayKey,
            selectedUri = pendingUri,
            uris = listOf(pendingUri)
        )
    }
    val uris = buildList {
        entry.uris.forEach { add(it) }
        if (pendingUri !in this) {
            add(pendingUri)
        }
    }
    return entry.copy(
        selectedUri = pendingUri,
        uris = uris
    )
}

private fun isStampSelectedForDay(
    context: Context,
    dayKey: String,
    uri: String
): Boolean {
    return loadAllStampCalendarEntries(context)
        .firstOrNull { it.dayKey == dayKey }
        ?.selectedUri == uri
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float {
    return start + ((end - start) * fraction.coerceIn(0f, 1f))
}

private fun stagedLerpFloat(
    start: Float,
    midpoint: Float,
    end: Float,
    fraction: Float,
    split: Float = 0.42f
): Float {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val clampedSplit = split.coerceIn(0.1f, 0.9f)
    return if (clampedFraction <= clampedSplit) {
        val local = (clampedFraction / clampedSplit).coerceIn(0f, 1f)
        lerpFloat(start, midpoint, local)
    } else {
        val local = ((clampedFraction - clampedSplit) / (1f - clampedSplit)).coerceIn(0f, 1f)
        lerpFloat(midpoint, end, local)
    }
}

private fun quadraticBezierFloat(
    start: Float,
    control: Float,
    end: Float,
    fraction: Float
): Float {
    val t = fraction.coerceIn(0f, 1f)
    val oneMinusT = 1f - t
    return (oneMinusT * oneMinusT * start) +
        (2f * oneMinusT * t * control) +
        (t * t * end)
}

private fun triangularPulse(
    fraction: Float,
    center: Float = 0.5f,
    width: Float = 0.82f
): Float {
    val clampedCenter = center.coerceIn(0f, 1f)
    val halfWidth = (width.coerceIn(0.1f, 1f) * 0.5f).coerceAtLeast(0.01f)
    val distance = abs(fraction.coerceIn(0f, 1f) - clampedCenter)
    return (1f - (distance / halfWidth)).coerceIn(0f, 1f)
}

private fun fluidRoundedCornerShape(
    density: androidx.compose.ui.unit.Density,
    baseRadiusPx: Float,
    pulse: Float,
    horizontalBias: Float = 0f,
    verticalBias: Float = 0f
): RoundedCornerShape {
    val base = baseRadiusPx.coerceAtLeast(2f)
    val pulseFactor = pulse.coerceIn(0f, 1f)
    val horizontal = horizontalBias.coerceIn(-1f, 1f)
    val vertical = verticalBias.coerceIn(-1f, 1f)
    val amplitude = base * (0.48f * pulseFactor)

    fun radius(multiplier: Float): androidx.compose.ui.unit.Dp {
        val resolved = (base + (amplitude * multiplier)).coerceAtLeast(2f)
        return with(density) { resolved.toDp() }
    }

    return RoundedCornerShape(
        topStart = radius(0.46f + (0.22f * horizontal) - (0.12f * vertical)),
        topEnd = radius(0.16f - (0.18f * horizontal) - (0.14f * vertical)),
        bottomEnd = radius(0.52f - (0.22f * horizontal) + (0.14f * vertical)),
        bottomStart = radius(0.18f + (0.18f * horizontal) + (0.2f * vertical))
    )
}

private fun bitmapAspectRatio(bitmap: Bitmap?): Float? {
    if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
    return (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.55f, 1.45f)
}

private fun cacheStampAspectRatio(uri: String?, aspectRatio: Float?) {
    if (uri.isNullOrBlank() || aspectRatio == null || !aspectRatio.isFinite()) return
    stampAspectRatioMemoryCache[uri] = aspectRatio.coerceIn(0.55f, 1.45f)
}

private fun stampSkinPreviewCacheKey(
    uri: String?,
    skin: StampCalendarStampSkin,
    categoryEmoji: String? = null
): String? {
    val resolvedUri = uri?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (skin.isOriginalStyle()) return null
    val emojiKey = categoryEmoji
        ?.trim()
        ?.take(8)
        .orEmpty()
    return "$resolvedUri#${skin.hashCode()}#$emojiKey"
}

private fun cachedStampSkinPreviewBitmap(cacheKey: String?): Bitmap? {
    if (cacheKey.isNullOrBlank()) return null
    val bitmap = stampSkinPreviewMemoryCache.remove(cacheKey) ?: return null
    if (bitmap.isRecycled) return null
    stampSkinPreviewMemoryCache[cacheKey] = bitmap
    return bitmap
}

private fun cacheStampSkinPreviewBitmap(
    cacheKey: String?,
    bitmap: Bitmap?
) {
    if (cacheKey.isNullOrBlank()) return
    val resolvedBitmap = bitmap?.takeUnless { it.isRecycled } ?: return
    stampSkinPreviewMemoryCache[cacheKey] = resolvedBitmap
    while (stampSkinPreviewMemoryCache.size > STAMP_SKIN_PREVIEW_MEMORY_CACHE_MAX_ENTRIES) {
        val oldestKey = stampSkinPreviewMemoryCache.entries.firstOrNull()?.key ?: break
        stampSkinPreviewMemoryCache.remove(oldestKey)
    }
}

internal fun resolveKnownStampAspectRatio(
    uri: String?,
    previewBitmap: Bitmap?
): Float? {
    val previewAspect = bitmapAspectRatio(previewBitmap)
    if (previewAspect != null) {
        cacheStampAspectRatio(uri, previewAspect)
        return previewAspect
    }
    return uri?.let { stampAspectRatioMemoryCache[it] }
}

private fun resolveOptimisticStampPreviewBitmap(
    uri: String?,
    optimisticStampBitmaps: Map<String, Bitmap>,
    stableSeedByUri: Map<String, String>
): Bitmap? {
    if (uri.isNullOrBlank()) return null
    optimisticStampBitmaps[uri]
        ?.takeUnless { it.isRecycled }
        ?.let { return it }
    return optimisticStampBitmaps.entries.firstOrNull { (key, bitmap) ->
        !bitmap.isRecycled && stableSeedByUri[key] == uri
    }?.value
}

private fun Drawable.toSharedTransitionPreviewBitmap(maxSide: Int = 1200): Bitmap? {
    val sourceWidth = intrinsicWidth.takeIf { it > 0 } ?: return null
    val sourceHeight = intrinsicHeight.takeIf { it > 0 } ?: return null
    val scale = minOf(
        1f,
        maxSide.toFloat() / maxOf(sourceWidth, sourceHeight).toFloat()
    )
    val targetWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
    return runCatching {
        toBitmap(
            width = targetWidth,
            height = targetHeight,
            config = Bitmap.Config.ARGB_8888
        )
    }.getOrNull()
}

internal fun readStampAspectRatio(context: Context, uriString: String?): Float {
    if (uriString.isNullOrBlank()) return STAMP_CARD_ASPECT
    stampAspectRatioMemoryCache[uriString]?.let { return it }
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val parsed = runCatching { Uri.parse(uriString) }.getOrNull()
    runCatching {
        when (parsed?.scheme?.lowercase(Locale.US)) {
            "content" -> context.contentResolver.openInputStream(parsed)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            "file" -> BitmapFactory.decodeFile(parsed.path, options)
            else -> {
                val file = File(uriString)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath, options)
                } else if (parsed != null) {
                    context.contentResolver.openInputStream(parsed)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                }
            }
        }
    }
    val width = options.outWidth
    val height = options.outHeight
    if (width <= 0 || height <= 0) return STAMP_CARD_ASPECT
    return (width.toFloat() / height.toFloat())
        .coerceIn(0.55f, 1.45f)
        .also { cacheStampAspectRatio(uriString, it) }
}

private fun resolveRandomStampUri(dayKey: String, uris: List<String>, salt: Int): String? {
    if (uris.isEmpty()) return null
    val index = abs((dayKey.hashCode() * 31) + salt) % uris.size
    return uris[index]
}

private fun parseMonthCalendar(monthKey: String): Calendar {
    val parts = monthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
}

private fun shiftMonthKey(monthKey: String, delta: Int): String {
    val calendar = parseMonthCalendar(monthKey)
    calendar.add(Calendar.MONTH, delta)
    return stampCalendarMonthKey(calendar)
}

private fun appLanguageLocale(language: AppLanguage): Locale = when (language) {
    AppLanguage.English -> Locale.US
    AppLanguage.SimplifiedChinese -> Locale.SIMPLIFIED_CHINESE
    AppLanguage.TraditionalChinese -> Locale.TRADITIONAL_CHINESE
    AppLanguage.Korean -> Locale.KOREA
    AppLanguage.Japanese -> Locale.JAPAN
    AppLanguage.French -> Locale.FRANCE
    AppLanguage.German -> Locale.GERMANY
    AppLanguage.Italian -> Locale.ITALY
    AppLanguage.Portuguese -> Locale.forLanguageTag("pt-BR")
    AppLanguage.Spanish -> Locale.forLanguageTag("es-ES")
    AppLanguage.Russian -> Locale.forLanguageTag("ru-RU")
    AppLanguage.Thai -> Locale.forLanguageTag("th-TH")
    AppLanguage.Vietnamese -> Locale.forLanguageTag("vi-VN")
    AppLanguage.Indonesian -> Locale.forLanguageTag("id-ID")
    AppLanguage.Arabic -> Locale.forLanguageTag("ar-SA")
    AppLanguage.Ukrainian,
    AppLanguage.Polish,
    AppLanguage.Czech,
    AppLanguage.Dutch,
    AppLanguage.Swedish,
    AppLanguage.Danish,
    AppLanguage.Norwegian,
    AppLanguage.Finnish,
    AppLanguage.Greek,
    AppLanguage.Romanian,
    AppLanguage.Hungarian,
    AppLanguage.Turkish,
    AppLanguage.Malay,
    AppLanguage.Hebrew -> language.locale
}

private fun resolveCalendarWeekStartDay(language: AppLanguage): Int = when (language) {
    AppLanguage.SimplifiedChinese,
    AppLanguage.TraditionalChinese,
    AppLanguage.French,
    AppLanguage.German,
    AppLanguage.Italian,
    AppLanguage.Spanish,
    AppLanguage.Portuguese,
    AppLanguage.Russian,
    AppLanguage.Vietnamese,
    AppLanguage.Indonesian,
    AppLanguage.Ukrainian,
    AppLanguage.Polish,
    AppLanguage.Czech,
    AppLanguage.Dutch,
    AppLanguage.Swedish,
    AppLanguage.Danish,
    AppLanguage.Norwegian,
    AppLanguage.Finnish,
    AppLanguage.Greek,
    AppLanguage.Romanian,
    AppLanguage.Hungarian,
    AppLanguage.Turkish,
    AppLanguage.Malay -> Calendar.MONDAY
    AppLanguage.English,
    AppLanguage.Korean,
    AppLanguage.Japanese,
    AppLanguage.Thai,
    AppLanguage.Hebrew,
    AppLanguage.Arabic -> Calendar.SUNDAY
}

private fun localizedWeekLabels(language: AppLanguage): List<String> {
    val locale = appLanguageLocale(language)
    val weekStartDay = resolveCalendarWeekStartDay(language)
    val shortWeekdays = DateFormatSymbols(locale).shortWeekdays
    return (0 until 7).map { offset ->
        val dayOfWeek = (((weekStartDay - 1) + offset) % 7) + 1
        shortWeekdays.getOrNull(dayOfWeek)
            .orEmpty()
            .replace(".", "")
            .trim()
    }
}

private fun buildStampYearWheelOptions(_currentYear: Int): List<StampWheelOption> {
    val startYear = 1826
    val endYear = 2103
    return (startYear..endYear).map { year ->
        StampWheelOption(
            value = year,
            label = year.toString()
        )
    }
}

private fun buildStampMonthWheelOptions(language: AppLanguage): List<StampWheelOption> {
    val shortMonths = DateFormatSymbols(appLanguageLocale(language)).shortMonths
    return (1..12).map { month ->
        val fallbackLabel = String.format(Locale.US, "%02d", month)
        val localizedLabel = shortMonths.getOrNull(month - 1)
            .orEmpty()
            .replace(".", "")
            .trim()
            .ifEmpty { fallbackLabel }
        StampWheelOption(
            value = month,
            label = localizedLabel
        )
    }
}

private fun formatStampCalendarHeadlineDate(
    language: AppLanguage,
    year: Int,
    month: Int,
    day: Int
): String = when (language) {
    AppLanguage.SimplifiedChinese -> "${year}\u5e74${month}\u6708${day}\u65e5"
    AppLanguage.TraditionalChinese -> "${year}\u5e74${month}\u6708${day}\u65e5"
    AppLanguage.Japanese -> "${year}\u5e74${month}\u6708${day}\u65e5"
    AppLanguage.Korean -> "${year}\ub144 ${month}\uc6d4 ${day}\uc77c"
    else -> String.format(Locale.US, "%04d.%02d.%02d", year, month, day)
}

private fun formatMonthLabel(monthKey: String): String {
    val parts = monthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
    return String.format(Locale.US, "%04d.%02d", year, month)
}

private fun thanksDialogTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7279\u522b\u611f\u8c22"
    AppLanguage.TraditionalChinese -> "\u7279\u5225\u611f\u8b1d"
    AppLanguage.Japanese -> "\u7279\u5225\u306a\u611f\u8b1d"
    AppLanguage.Korean -> "\ud2b8\ub80c\ucf13 \uac10\uc0ac"
    AppLanguage.French -> "Remerciements sp\u00e9ciaux"
    AppLanguage.German -> "Besonderer Dank"
    AppLanguage.Italian -> "Ringraziamenti speciali"
    AppLanguage.Spanish -> "Agradecimientos especiales"
    AppLanguage.Portuguese -> "Agradecimentos especiais"
    AppLanguage.Russian -> "\u041e\u0441\u043e\u0431\u0430\u044f \u0431\u043b\u0430\u0433\u043e\u0434\u0430\u0440\u043d\u043e\u0441\u0442\u044c"
    AppLanguage.Thai -> "\u0e02\u0e2d\u0e04\u0e48\u0e32\u0e19\u0e34\u0e49\u0e07\u0e40\u0e16\u0e34\u0e14\u0e44\u0e1f"
    AppLanguage.Vietnamese -> "C\u1ea3m \u01a1n \u0111\u1eb7c bi\u1ec7t"
    AppLanguage.Indonesian -> "Terima Kasih Istimewa"
    AppLanguage.Arabic -> "\u0634\u0643\u0631 \u062e\u0627\u0635"
    AppLanguage.Ukrainian -> "Особлива подяка"
    AppLanguage.Polish -> "Specjalne podziękowania"
    AppLanguage.Czech -> "Zvláštní poděkování"
    AppLanguage.Dutch -> "Speciale dank"
    AppLanguage.Swedish -> "Särskilt tack"
    AppLanguage.Danish -> "Særlig tak"
    AppLanguage.Norwegian -> "Spesiell takk"
    AppLanguage.Finnish -> "Erityiskiitokset"
    AppLanguage.Greek -> "Ειδικές ευχαριστίες"
    AppLanguage.Romanian -> "Mulțumiri speciale"
    AppLanguage.Hungarian -> "Külön köszönet"
    AppLanguage.Turkish -> "Özel Teşekkür"
    AppLanguage.Malay -> "Terima Kasih Istimewa"
    AppLanguage.Hebrew -> "תודות מיוחדות"
    AppLanguage.English -> "Special Thanks"
}

private fun dayAllTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u5f53\u65e5\u6240\u6709"
    AppLanguage.TraditionalChinese -> "\u7576\u65e5\u6240\u6709"
    AppLanguage.Japanese -> "\u4eca\u65e5\u306e\u3059\u3079\u3066"
    AppLanguage.Korean -> "\uc624\ub298\uc758 \ubaa8\ub4e0 \uac74"
    AppLanguage.French -> "Tout d'aujourd'hui"
    AppLanguage.German -> "Alles von heute"
    AppLanguage.Italian -> "Tutto di oggi"
    AppLanguage.Spanish -> "Todo de hoy"
    AppLanguage.Portuguese -> "Tudo de hoje"
    AppLanguage.Russian -> "\u0412\u0441\u0451 \u0437\u0430 \u0441\u0435\u0433\u043e\u0434\u043d\u044f"
    AppLanguage.Thai -> "\u0e17\u0e38\u0e4c\u0e27\u0e31\u0e19\u0e19\u0e35\u0e49"
    AppLanguage.Vietnamese -> "T\u1ea5t c\u1ea3 ng\u00e0y h\u00f4m nay"
    AppLanguage.Indonesian -> "Semua hari ini"
    AppLanguage.Arabic -> "\u0643\u0644 \u0627\u0644\u064a\u0648\u0645"
    AppLanguage.Ukrainian -> "Усе за день"
    AppLanguage.Polish -> "Wszystko z dnia"
    AppLanguage.Czech -> "Vše z tohoto dne"
    AppLanguage.Dutch -> "Alles van de dag"
    AppLanguage.Swedish -> "Allt från dagen"
    AppLanguage.Danish -> "Alt fra dagen"
    AppLanguage.Norwegian -> "Alt fra dagen"
    AppLanguage.Finnish -> "Kaikki päivältä"
    AppLanguage.Greek -> "Όλα της ημέρας"
    AppLanguage.Romanian -> "Tot din zi"
    AppLanguage.Hungarian -> "A nap összes bélyege"
    AppLanguage.Turkish -> "Günün Tümü"
    AppLanguage.Malay -> "Semua pada Hari Itu"
    AppLanguage.Hebrew -> "הכול מהיום"
    AppLanguage.English -> "All Of The Day"
}

private fun dayCoverLabel(
    language: AppLanguage,
    selected: Boolean,
    isTodayDay: Boolean
): String = when (language) {
    AppLanguage.SimplifiedChinese -> when {
        selected && isTodayDay -> "\u5df2\u8bbe\u4e3a\u4eca\u65e5\u5c01\u9762"
        selected -> "\u5df2\u8bbe\u4e3a\u5f53\u65e5\u5c01\u9762"
        isTodayDay -> "\u8bbe\u4e3a\u4eca\u65e5\u5c01\u9762"
        else -> "\u8bbe\u4e3a\u5f53\u65e5\u5c01\u9762"
    }
    AppLanguage.TraditionalChinese -> when {
        selected && isTodayDay -> "\u5df2\u8a2d\u70ba\u4eca\u65e5\u5c01\u9762"
        selected -> "\u5df2\u8a2d\u70ba\u7576\u65e5\u5c01\u9762"
        isTodayDay -> "\u8a2d\u70ba\u4eca\u65e5\u5c01\u9762"
        else -> "\u8a2d\u70ba\u7576\u65e5\u5c01\u9762"
    }
    AppLanguage.Japanese -> when {
        selected && isTodayDay -> "\u4eca\u65e5\u306e\u30ab\u30d0\u30fc\u3092\u9078\u629e\u3057\u3066\u3044\u307e\u3059"
        selected -> "\u305d\u306e\u65e5\u306e\u30ab\u30d0\u30fc\u3092\u9078\u629e\u3057\u3066\u3044\u307e\u3059"
        isTodayDay -> "\u4eca\u65e5\u306e\u30ab\u30d0\u30fc\u306b\u8a2d\u5b9a"
        else -> "\u305d\u306e\u65e5\u306e\u30ab\u30d0\u30fc\u306b\u8a2d\u5b9a"
    }
    AppLanguage.Korean -> when {
        selected && isTodayDay -> "\uc624\ub298\uc758 \ucef4\ubc1c \uc120\ud0dd\ub428"
        selected -> "\uadf8\ub140\uc758 \ucef4\ubc1c \uc120\ud0dd\ub428"
        isTodayDay -> "\uc624\ub298\uc758 \ucef4\ubc1c\uc73c\ub85c \uc124\uc815"
        else -> "\uadf8\ub140\uc758 \ucef4\ubc1c\uc73c\ub85c \uc124\uc815"
    }
    AppLanguage.French -> when {
        selected && isTodayDay -> "Couverture d'aujourd'hui s\u00e9lectionn\u00e9e"
        selected -> "Couverture du jour s\u00e9lectionn\u00e9e"
        isTodayDay -> "D\u00e9finir comme couverture d'aujourd'hui"
        else -> "D\u00e9finir comme couverture du jour"
    }
    AppLanguage.German -> when {
        selected && isTodayDay -> "Heutiges Cover ausgew\u00e4hlt"
        selected -> "Tagescover ausgew\u00e4hlt"
        isTodayDay -> "Als heutiges Cover festlegen"
        else -> "Als Tagescover festlegen"
    }
    AppLanguage.Italian -> when {
        selected && isTodayDay -> "Copertina di oggi selezionata"
        selected -> "Copertina del giorno selezionata"
        isTodayDay -> "Imposta come copertina di oggi"
        else -> "Imposta come copertina del giorno"
    }
    AppLanguage.Spanish -> when {
        selected && isTodayDay -> "Portada de hoy seleccionada"
        selected -> "Portada del d\u00eda seleccionada"
        isTodayDay -> "Establecer como portada de hoy"
        else -> "Establecer como portada del d\u00eda"
    }
    AppLanguage.Portuguese -> when {
        selected && isTodayDay -> "Capa de hoje selecionada"
        selected -> "Capa do dia selecionada"
        isTodayDay -> "Definir como capa de hoje"
        else -> "Definir como capa do dia"
    }
    AppLanguage.Russian -> when {
        selected && isTodayDay -> "\u0412\u044b\u0431\u0440\u0430\u043d\u0430 \u043e\u0431\u043b\u043e\u0436\u043a\u0430 \u0441\u0435\u0433\u043e\u0434\u043d\u044f\u0448\u043d\u0435\u0433\u043e \u0434\u043d\u044f"
        selected -> "\u0412\u044b\u0431\u0440\u0430\u043d\u0430 \u043e\u0431\u043b\u043e\u0436\u043a\u0430 \u0434\u043d\u044f"
        isTodayDay -> "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u043a\u0430\u043a \u043e\u0431\u043b\u043e\u0436\u043a\u0443 \u0441\u0435\u0433\u043e\u0434\u043d\u044f"
        else -> "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u043a\u0430\u043a \u043e\u0431\u043b\u043e\u0436\u043a\u0443 \u0434\u043d\u044f"
    }
    AppLanguage.Thai -> when {
        selected && isTodayDay -> "\u0e40\u0e25\u0e37\u0e2d\u0e01\u0e2b\u0e19\u0e49\u0e32\u0e1b\u0e01\u0e02\u0e2d\u0e07\u0e27\u0e31\u0e19\u0e19\u0e35\u0e49\u0e41\u0e25\u0e49\u0e27"
        selected -> "\u0e40\u0e25\u0e37\u0e2d\u0e01\u0e2b\u0e19\u0e49\u0e32\u0e1b\u0e01\u0e1b\u0e23\u0e30\u0e08\u0e33\u0e27\u0e31\u0e19\u0e41\u0e25\u0e49\u0e27"
        isTodayDay -> "\u0e15\u0e31\u0e49\u0e07\u0e40\u0e1b\u0e47\u0e19\u0e2b\u0e19\u0e49\u0e32\u0e1b\u0e01\u0e02\u0e2d\u0e07\u0e27\u0e31\u0e19\u0e19\u0e35\u0e49"
        else -> "\u0e15\u0e31\u0e49\u0e07\u0e40\u0e1b\u0e47\u0e19\u0e2b\u0e19\u0e49\u0e32\u0e1b\u0e01\u0e1b\u0e23\u0e30\u0e08\u0e33\u0e27\u0e31\u0e19"
    }
    AppLanguage.Vietnamese -> when {
        selected && isTodayDay -> "\u0110\u00e3 ch\u1ecdn b\u00eca \u00e1p h\u00f4m nay"
        selected -> "\u0110\u00e3 ch\u1ecdn b\u00eca \u00e1p ng\u00e0y"
        isTodayDay -> "\u0110\u1eb7t l\u00e0m b\u00eca \u00e1p h\u00f4m nay"
        else -> "\u0110\u1eb7t l\u00e0m b\u00eca \u00e1p ng\u00e0y"
    }
    AppLanguage.Indonesian -> when {
        selected && isTodayDay -> "Sampul hari ini dipilih"
        selected -> "Sampul hari dipilih"
        isTodayDay -> "Jadikan sebagai sampul hari ini"
        else -> "Jadikan sebagai sampul hari"
    }
    AppLanguage.Arabic -> when {
        selected && isTodayDay -> "\u062a\u0645 \u062a\u062d\u062f\u064a\u062f \u063a\u0644\u0627\u0641 \u0627\u0644\u064a\u0648\u0645"
        selected -> "\u062a\u0645 \u062a\u062d\u062f\u064a\u062f \u063a\u0644\u0627\u0641 \u0627\u0644\u064a\u0648\u0645"
        isTodayDay -> "\u062a\u0639\u064a\u064a\u0646 \u063a\u0644\u0627\u0641 \u0627\u0644\u064a\u0648\u0645"
        else -> "\u062a\u0639\u064a\u064a\u0646 \u063a\u0644\u0627\u0641 \u0627\u0644\u064a\u0648\u0645"
    }
    AppLanguage.Ukrainian -> when {
        selected && isTodayDay -> "Обрано обкладинку сьогодні"
        selected -> "Обрано обкладинку дня"
        isTodayDay -> "Зробити обкладинкою сьогодні"
        else -> "Зробити обкладинкою дня"
    }
    AppLanguage.Polish -> when {
        selected && isTodayDay -> "Wybrano okładkę na dziś"
        selected -> "Wybrano okładkę dnia"
        isTodayDay -> "Ustaw jako okładkę na dziś"
        else -> "Ustaw jako okładkę dnia"
    }
    AppLanguage.Czech -> when {
        selected && isTodayDay -> "Vybrána dnešní obálka"
        selected -> "Vybrána obálka dne"
        isTodayDay -> "Nastavit jako dnešní obálku"
        else -> "Nastavit jako obálku dne"
    }
    AppLanguage.Dutch -> when {
        selected && isTodayDay -> "Cover van vandaag gekozen"
        selected -> "Dagcover gekozen"
        isTodayDay -> "Instellen als cover van vandaag"
        else -> "Instellen als dagcover"
    }
    AppLanguage.Swedish -> when {
        selected && isTodayDay -> "Dagens omslag valt"
        selected -> "Dagsomslag valt"
        isTodayDay -> "Ange som dagens omslag"
        else -> "Ange som dagsomslag"
    }
    AppLanguage.Danish -> when {
        selected && isTodayDay -> "Dagens cover valgt"
        selected -> "Dagscover valgt"
        isTodayDay -> "Angiv som dagens cover"
        else -> "Angiv som dagscover"
    }
    AppLanguage.Norwegian -> when {
        selected && isTodayDay -> "Dagens omslag valgt"
        selected -> "Dagsomslag valgt"
        isTodayDay -> "Angi som dagens omslag"
        else -> "Angi som dagsomslag"
    }
    AppLanguage.Finnish -> when {
        selected && isTodayDay -> "Päivän kansi valittu"
        selected -> "Päivän kansi valittu"
        isTodayDay -> "Aseta tämän päivän kanneksi"
        else -> "Aseta päivän kanneksi"
    }
    AppLanguage.Greek -> when {
        selected && isTodayDay -> "Επιλέχθηκε το εξώφυλλο σήμερα"
        selected -> "Επιλέχθηκε το εξώφυλλο ημέρας"
        isTodayDay -> "Ορισμός ως εξώφυλλο σήμερα"
        else -> "Ορισμός ως εξώφυλλο ημέρας"
    }
    AppLanguage.Romanian -> when {
        selected && isTodayDay -> "Coperta de azi selectată"
        selected -> "Coperta zilei selectată"
        isTodayDay -> "Setează ca copertă de azi"
        else -> "Setează ca copertă a zilei"
    }
    AppLanguage.Hungarian -> when {
        selected && isTodayDay -> "A mai borító kiválasztva"
        selected -> "A nap borítója kiválasztva"
        isTodayDay -> "Beállítás mai borítónak"
        else -> "Beállítás a nap borítójának"
    }
    AppLanguage.Turkish -> when {
        selected && isTodayDay -> "Bugünün Kapağı Seçildi"
        selected -> "Günün Kapağı Seçildi"
        isTodayDay -> "Bugünün Kapağı Yap"
        else -> "Günün Kapağı Yap"
    }
    AppLanguage.Malay -> when {
        selected && isTodayDay -> "Muka Depan Hari Ini Dipilih"
        selected -> "Muka Depan Hari Dipilih"
        isTodayDay -> "Tetapkan sebagai Muka Depan Hari Ini"
        else -> "Tetapkan sebagai Muka Depan Hari"
    }
    AppLanguage.Hebrew -> when {
        selected && isTodayDay -> "נבחר כריכת היום"
        selected -> "נבחר כריכת היום"
        isTodayDay -> "הגדרה ככריכת היום"
        else -> "הגדרה ככריכת היום"
    }
    AppLanguage.English -> when {
        selected && isTodayDay -> "Today's Cover Selected"
        selected -> "Day Cover Selected"
        isTodayDay -> "Set As Today's Cover"
        else -> "Set As Day Cover"
    }
}

private fun stampSkinStyleLabel(
    copy: StampSkinPanelCopy,
    skin: StampCalendarStampSkin
): String = when (skin.normalized().style) {
    StampCalendarSkinStyle.Original -> copy.originalLabel
    StampCalendarSkinStyle.WhiteBorder -> copy.whiteBorderLabel
    StampCalendarSkinStyle.Editorial -> copy.editorialLabel
    StampCalendarSkinStyle.Cutout -> copy.cutoutLabel
}

private fun stampSkinEffectLabel(
    copy: StampSkinPanelCopy,
    effect: StampCalendarImageEffect
): String = when (effect) {
    StampCalendarImageEffect.None -> copy.effectNoneLabel
    StampCalendarImageEffect.Cartoon -> copy.effectCartoonLabel
    StampCalendarImageEffect.InkSpread -> copy.effectInkLabel
    StampCalendarImageEffect.TextureOverlay -> copy.effectTextureLabel
    StampCalendarImageEffect.ColorQuantize -> copy.effectQuantizeLabel
    StampCalendarImageEffect.Halftone -> copy.effectHalftoneLabel
    StampCalendarImageEffect.MisprintOffset -> copy.effectMisprintLabel
}

private fun stampSkinPanelCopy(language: AppLanguage): StampSkinPanelCopy = when (language) {
    AppLanguage.SimplifiedChinese -> StampSkinPanelCopy(
        title = "\u66f4\u6362\u90ae\u7968\u6837\u5f0f",
        originalLabel = "\u539f\u6837",
        whiteBorderLabel = "\u8fb9\u7f18",
        editorialLabel = "\u6570\u503c",
        cutoutLabel = "\u62a0\u56fe",
        effectLabel = "\u98ce\u683c\u5316",
        effectNoneLabel = "\u65e0",
        effectCartoonLabel = "\u5361\u901a",
        effectInkLabel = "\u6cb9\u58a8\u6269\u6563",
        effectTextureLabel = "\u7eb9\u7406\u53e0\u52a0",
        effectQuantizeLabel = "\u989c\u8272\u91cf\u5316",
        effectHalftoneLabel = "\u534a\u8272\u8c03",
        effectMisprintLabel = "\u5957\u5370\u504f\u79fb",
        backgroundColorLabel = "\u80cc\u666f\u989c\u8272",
        numberFieldLabel = "\u6570\u503c\uff08\u56fa\u5b9a\uff09",
        titleFieldLabel = "\u53f3\u4fa7\u6587\u5b57",
        subtitleFieldLabel = "\u526f\u6807\u9898",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "\u5e94\u7528",
        cancelLabel = "\u53d6\u6d88",
        campfireGateMessage = "\u66f4\u6362\u90ae\u7968\u6837\u5f0f\u4e3a\u7bdd\u706b\u8ba1\u5212\u7528\u6237\u62a2\u5148\u4f53\u9a8c\u529f\u80fd",
        campfireGateLearnLabel = "\u53bb\u4e86\u89e3\u7bdd\u706b\u8ba1\u5212"
    )
    AppLanguage.TraditionalChinese -> StampSkinPanelCopy(
        title = "\u66f4\u63db\u90f5\u7968\u6a23\u5f0f",
        originalLabel = "\u539f\u6a23",
        whiteBorderLabel = "\u908a\u7de3",
        editorialLabel = "\u6578\u503c",
        cutoutLabel = "\u6473\u5716",
        effectLabel = "\u98a8\u683c\u5316",
        effectNoneLabel = "\u7121",
        effectCartoonLabel = "\u5361\u901a",
        effectInkLabel = "\u6cb9\u58a8\u64f4\u6563",
        effectTextureLabel = "\u7d0b\u7406\u758a\u52a0",
        effectQuantizeLabel = "\u984f\u8272\u91cf\u5316",
        effectHalftoneLabel = "\u534a\u8272\u8abf",
        effectMisprintLabel = "\u5957\u5370\u504f\u79fb",
        backgroundColorLabel = "\u80cc\u666f\u984f\u8272",
        numberFieldLabel = "\u6578\u503c\uff08\u56fa\u5b9a\uff09",
        titleFieldLabel = "\u53f3\u5074\u6587\u5b57",
        subtitleFieldLabel = "\u526f\u6a19\u984c",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "\u5957\u7528",
        cancelLabel = "\u53d6\u6d88",
        campfireGateMessage = "\u66f4\u63db\u90f5\u7968\u6a23\u5f0f\u70ba\u7bdd\u706b\u8a08\u5283\u7528\u6236\u6436\u5148\u9ad4\u9a57\u529f\u80fd",
        campfireGateLearnLabel = "\u53bb\u4e86\u89e3\u7bdd\u706b\u8a08\u5283"
    )
    AppLanguage.Ukrainian -> StampSkinPanelCopy(
        title = "Змінити стиль марки",
        originalLabel = "Оригінал",
        whiteBorderLabel = "Край",
        editorialLabel = "Номінал",
        cutoutLabel = "Автовирізання",
        effectLabel = "Стилізація",
        effectNoneLabel = "Немає",
        effectCartoonLabel = "Мультфільм",
        effectInkLabel = "Розпливання туші",
        effectTextureLabel = "Текстура",
        effectQuantizeLabel = "Квантування",
        effectHalftoneLabel = "Растр",
        effectMisprintLabel = "Зсув друку",
        backgroundColorLabel = "Колір тла",
        numberFieldLabel = "Номінал (фіксований)",
        titleFieldLabel = "Текст праворуч",
        subtitleFieldLabel = "Підзаголовок",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Застосувати",
        cancelLabel = "Скасувати",
        campfireGateMessage = "Зміна стилю марки є функцією раннього доступу для користувачів плану «Вогнище».",
        campfireGateLearnLabel = "Дізнатися про план «Вогнище»"
    )
    AppLanguage.Polish -> StampSkinPanelCopy(
        title = "Zmień styl znaczka",
        originalLabel = "Oryginał",
        whiteBorderLabel = "Krawędź",
        editorialLabel = "Nominał",
        cutoutLabel = "Automatyczne wycięcie",
        effectLabel = "Stylizacja",
        effectNoneLabel = "Brak",
        effectCartoonLabel = "Kreskówka",
        effectInkLabel = "Rozlany tusz",
        effectTextureLabel = "Faktura",
        effectQuantizeLabel = "Kwantyzacja",
        effectHalftoneLabel = "Półton",
        effectMisprintLabel = "Przesunięty druk",
        backgroundColorLabel = "Kolor tła",
        numberFieldLabel = "Nominał (stały)",
        titleFieldLabel = "Tekst po prawej",
        subtitleFieldLabel = "Podtytuł",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Zastosuj",
        cancelLabel = "Anuluj",
        campfireGateMessage = "Zmiana stylu znaczka to funkcja we wczesnym dostępie dla użytkowników planu Ognisko.",
        campfireGateLearnLabel = "Dowiedz się więcej o planie Ognisko"
    )
    AppLanguage.Czech -> StampSkinPanelCopy(
        title = "Změnit styl známky",
        originalLabel = "Originál",
        whiteBorderLabel = "Okraj",
        editorialLabel = "Hodnota",
        cutoutLabel = "Automatický výřez",
        effectLabel = "Stylizace",
        effectNoneLabel = "Žádná",
        effectCartoonLabel = "Kreslený styl",
        effectInkLabel = "Rozpíjející se inkoust",
        effectTextureLabel = "Textura",
        effectQuantizeLabel = "Kvantizace",
        effectHalftoneLabel = "Půltón",
        effectMisprintLabel = "Posunutý tisk",
        backgroundColorLabel = "Barva pozadí",
        numberFieldLabel = "Hodnota (pevná)",
        titleFieldLabel = "Text vpravo",
        subtitleFieldLabel = "Podtitul",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Použít",
        cancelLabel = "Zrušit",
        campfireGateMessage = "Změna stylu známky je funkce v předběžném přístupu pro uživatele plánu Táborák.",
        campfireGateLearnLabel = "Zjistit více o plánu Táborák"
    )
    AppLanguage.Dutch -> StampSkinPanelCopy(
        title = "Postzegelstijl wijzigen",
        originalLabel = "Origineel",
        whiteBorderLabel = "Rand",
        editorialLabel = "Waarde",
        cutoutLabel = "Automatisch uitknippen",
        effectLabel = "Stilering",
        effectNoneLabel = "Geen",
        effectCartoonLabel = "Cartoon",
        effectInkLabel = "Inktuitloop",
        effectTextureLabel = "Textuur",
        effectQuantizeLabel = "Kwantiseren",
        effectHalftoneLabel = "Halftoon",
        effectMisprintLabel = "Misdruk",
        backgroundColorLabel = "Achtergrondkleur",
        numberFieldLabel = "Waarde (vast)",
        titleFieldLabel = "Tekst rechts",
        subtitleFieldLabel = "Ondertitel",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Pas toe",
        cancelLabel = "Annuleer",
        campfireGateMessage = "Postzegelstijl wijzigen is een functie in vroege toegang voor gebruikers van het Kampvuur-abonnement.",
        campfireGateLearnLabel = "Meer over het Kampvuur-abonnement"
    )
    AppLanguage.Swedish -> StampSkinPanelCopy(
        title = "Ändra frimärkets stil",
        originalLabel = "Original",
        whiteBorderLabel = "Kant",
        editorialLabel = "Värde",
        cutoutLabel = "Automatiskt urklipp",
        effectLabel = "Stilisering",
        effectNoneLabel = "Ingen",
        effectCartoonLabel = "Tecknad",
        effectInkLabel = "Inkspridning",
        effectTextureLabel = "Textur",
        effectQuantizeLabel = "Kvantisering",
        effectHalftoneLabel = "Halvton",
        effectMisprintLabel = "Feltryck",
        backgroundColorLabel = "Bakgrundsfärg",
        numberFieldLabel = "Värde (fast)",
        titleFieldLabel = "Text till höger",
        subtitleFieldLabel = "Underrubrik",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Använd",
        cancelLabel = "Avbryt",
        campfireGateMessage = "Att ändra frimärkets stil är en tidig förhandsfunktion för användare av Lägereld-planen.",
        campfireGateLearnLabel = "Läs mer om Lägereld-planen"
    )
    AppLanguage.Danish -> StampSkinPanelCopy(
        title = "Skift frimærkets stil",
        originalLabel = "Original",
        whiteBorderLabel = "Kant",
        editorialLabel = "Værdi",
        cutoutLabel = "Automatisk frilægning",
        effectLabel = "Stilisering",
        effectNoneLabel = "Ingen",
        effectCartoonLabel = "Tegneserie",
        effectInkLabel = "Blækspredning",
        effectTextureLabel = "Tekstur",
        effectQuantizeLabel = "Kvantisering",
        effectHalftoneLabel = "Halvtone",
        effectMisprintLabel = "Fejltryk",
        backgroundColorLabel = "Baggrundsfarve",
        numberFieldLabel = "Værdi (fast)",
        titleFieldLabel = "Tekst til højre",
        subtitleFieldLabel = "Undertitel",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Anvend",
        cancelLabel = "Annuller",
        campfireGateMessage = "Skift af frimærkestil er en tidlig forhåndsfunktion for brugere af Lejrbål-planen.",
        campfireGateLearnLabel = "Læs om Lejrbål-planen"
    )
    AppLanguage.Norwegian -> StampSkinPanelCopy(
        title = "Endre frimerkestil",
        originalLabel = "Original",
        whiteBorderLabel = "Kant",
        editorialLabel = "Verdi",
        cutoutLabel = "Automatisk utklipp",
        effectLabel = "Stilisering",
        effectNoneLabel = "Ingen",
        effectCartoonLabel = "Tegneserie",
        effectInkLabel = "Blekkspredning",
        effectTextureLabel = "Tekstur",
        effectQuantizeLabel = "Kvantisering",
        effectHalftoneLabel = "Halvtone",
        effectMisprintLabel = "Feiltrykk",
        backgroundColorLabel = "Bakgrunnsfarge",
        numberFieldLabel = "Verdi (fast)",
        titleFieldLabel = "Tekst til høyre",
        subtitleFieldLabel = "Undertittel",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Bruk",
        cancelLabel = "Avbryt",
        campfireGateMessage = "Å endre frimerkestil er en funksjon i tidlig tilgang for brukere av Leirbål-planen.",
        campfireGateLearnLabel = "Les om Leirbål-planen"
    )
    AppLanguage.Finnish -> StampSkinPanelCopy(
        title = "Vaihda postimerkin tyyli",
        originalLabel = "Alkuperäinen",
        whiteBorderLabel = "Reuna",
        editorialLabel = "Arvo",
        cutoutLabel = "Automaattirajaus",
        effectLabel = "Tyylittely",
        effectNoneLabel = "Ei mitään",
        effectCartoonLabel = "Sarjakuva",
        effectInkLabel = "Musteen leviäminen",
        effectTextureLabel = "Rakenne",
        effectQuantizeLabel = "Kvantisointi",
        effectHalftoneLabel = "Rasteri",
        effectMisprintLabel = "Väärinpainatus",
        backgroundColorLabel = "Taustaväri",
        numberFieldLabel = "Arvo (kiinteä)",
        titleFieldLabel = "Oikean puolen teksti",
        subtitleFieldLabel = "Alaotsikko",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Käytä",
        cancelLabel = "Kumoa",
        campfireGateMessage = "Postimerkin tyylin vaihto on varhaisen käytön ominaisuus Nuotio-suunnitelman käyttäjille.",
        campfireGateLearnLabel = "Lue Nuotio-suunnitelmasta"
    )
    AppLanguage.Greek -> StampSkinPanelCopy(
        title = "Αλλαγή στυλ γραμματοσήμου",
        originalLabel = "Πρωτότυπο",
        whiteBorderLabel = "Άκρη",
        editorialLabel = "Αξία",
        cutoutLabel = "Αυτόματο ψαλίδισμα",
        effectLabel = "Στυλιζάρισμα",
        effectNoneLabel = "Κανένα",
        effectCartoonLabel = "Καρτούν",
        effectInkLabel = "Διάχυση μελανιού",
        effectTextureLabel = "Υφή",
        effectQuantizeLabel = "Κβαντοποίηση",
        effectHalftoneLabel = "Ημιτονικό",
        effectMisprintLabel = "Εκτύπωση εκτός ευθυγράμμισης",
        backgroundColorLabel = "Χρώμα φόντου",
        numberFieldLabel = "Αξία (σταθερή)",
        titleFieldLabel = "Κείμενο δεξιά",
        subtitleFieldLabel = "Υπότιτλος",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Εφαρμογή",
        cancelLabel = "Ακύρωση",
        campfireGateMessage = "Η αλλαγή στυλ γραμματοσήμου είναι λειτουργία πρώιμης πρόσβασης για χρήστες του προγράμματος Φωτιά του κάμπινγκ.",
        campfireGateLearnLabel = "Μάθετε για το πρόγραμμα Φωτιά του κάμπινγκ"
    )
    AppLanguage.Romanian -> StampSkinPanelCopy(
        title = "Schimbă stilul timbrului",
        originalLabel = "Original",
        whiteBorderLabel = "Margine",
        editorialLabel = "Valoare",
        cutoutLabel = "Decupare automată",
        effectLabel = "Stilizare",
        effectNoneLabel = "Fără",
        effectCartoonLabel = "Desen animat",
        effectInkLabel = "Difuzie de cerneală",
        effectTextureLabel = "Textură",
        effectQuantizeLabel = "Cuantizare",
        effectHalftoneLabel = "Semiton",
        effectMisprintLabel = "Tipar decalat",
        backgroundColorLabel = "Culoare de fundal",
        numberFieldLabel = "Valoare (fixă)",
        titleFieldLabel = "Text în dreapta",
        subtitleFieldLabel = "Subtitlu",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Aplică",
        cancelLabel = "Anulează",
        campfireGateMessage = "Schimbarea stilului timbrului este o funcție în acces timpuriu pentru utilizatorii planului Focul de tabără.",
        campfireGateLearnLabel = "Află despre planul Focul de tabără"
    )
    AppLanguage.Hungarian -> StampSkinPanelCopy(
        title = "Bélyegstílus módosítása",
        originalLabel = "Eredeti",
        whiteBorderLabel = "Szél",
        editorialLabel = "Érték",
        cutoutLabel = "Automatikus kivágás",
        effectLabel = "Stilizálás",
        effectNoneLabel = "Nincs",
        effectCartoonLabel = "Rajzfilm",
        effectInkLabel = "Tintaszóródás",
        effectTextureLabel = "Textúra",
        effectQuantizeLabel = "Kvantálás",
        effectHalftoneLabel = "Féltónus",
        effectMisprintLabel = "Elcsúszott nyomat",
        backgroundColorLabel = "Háttérszín",
        numberFieldLabel = "Érték (rögzített)",
        titleFieldLabel = "Jobb oldali szöveg",
        subtitleFieldLabel = "Alcím",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Alkalmaz",
        cancelLabel = "Mégsem",
        campfireGateMessage = "A bélyegstílus módosítása korai hozzáférésű funkció a Tábortűz csomag felhasználóinak.",
        campfireGateLearnLabel = "Tudj meg többet a Tábortűz csomagról"
    )
    AppLanguage.Turkish -> StampSkinPanelCopy(
        title = "Pul Stilini Değiştir",
        originalLabel = "Orijinal",
        whiteBorderLabel = "Kenar",
        editorialLabel = "Değer",
        cutoutLabel = "Otomatik Kesme",
        effectLabel = "Stilize",
        effectNoneLabel = "Yok",
        effectCartoonLabel = "Çizgi Film",
        effectInkLabel = "Mürekkep Yayılımı",
        effectTextureLabel = "Doku",
        effectQuantizeLabel = "Nicemleme",
        effectHalftoneLabel = "Yarım Ton",
        effectMisprintLabel = "Kaymış Baskı",
        backgroundColorLabel = "Arka Plan Rengi",
        numberFieldLabel = "Değer (Sabit)",
        titleFieldLabel = "Sağ Metin",
        subtitleFieldLabel = "Alt Başlık",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Uygula",
        cancelLabel = "İptal",
        campfireGateMessage = "Pul stilini değiştirmek, Kamp Ateşi Planı kullanıcıları için erken erişim özelliğidir.",
        campfireGateLearnLabel = "Kamp Ateşi Planı hakkında bilgi al"
    )
    AppLanguage.Malay -> StampSkinPanelCopy(
        title = "Tukar Gaya Setem",
        originalLabel = "Asal",
        whiteBorderLabel = "Tepi",
        editorialLabel = "Nilai",
        cutoutLabel = "Potongan Automatik",
        effectLabel = "Stilisasi",
        effectNoneLabel = "Tiada",
        effectCartoonLabel = "Kartun",
        effectInkLabel = "Sebaran Dakwat",
        effectTextureLabel = "Tekstur",
        effectQuantizeLabel = "Kuantisasi",
        effectHalftoneLabel = "Separuh Nada",
        effectMisprintLabel = "Cetakan Tersasar",
        backgroundColorLabel = "Warna Latar Belakang",
        numberFieldLabel = "Nilai (Tetap)",
        titleFieldLabel = "Teks Kanan",
        subtitleFieldLabel = "Sari Kata",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Guna",
        cancelLabel = "Batal",
        campfireGateMessage = "Menukar gaya setem ialah ciri akses awal untuk pengguna Pelan Unggun Api.",
        campfireGateLearnLabel = "Ketahui tentang Pelan Unggun Api"
    )
    AppLanguage.Hebrew -> StampSkinPanelCopy(
        title = "שינוי סגנון הבול",
        originalLabel = "מקורי",
        whiteBorderLabel = "שוליים",
        editorialLabel = "ערך",
        cutoutLabel = "חיתוך אוטומטי",
        effectLabel = "סגנון אמנותי",
        effectNoneLabel = "ללא",
        effectCartoonLabel = "קריקטורה",
        effectInkLabel = "פיזור דיו",
        effectTextureLabel = "מרקם",
        effectQuantizeLabel = "קוונטיזציה",
        effectHalftoneLabel = "חצי גוון",
        effectMisprintLabel = "הדפסה מוסטת",
        backgroundColorLabel = "צבע רקע",
        numberFieldLabel = "ערך (קבוע)",
        titleFieldLabel = "טקסט בצד ימין",
        subtitleFieldLabel = "כותרת משנה",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "החלה",
        cancelLabel = "ביטול",
        campfireGateMessage = "שינוי סגנון הבול הוא תכונה בגישה מוקדמת למשתמשי תוכנית המדורה.",
        campfireGateLearnLabel = "מידע על תוכנית המדורה"
    )
    else -> StampSkinPanelCopy(
        title = "Change Stamp Style",
        originalLabel = "Original",
        whiteBorderLabel = "Edge",
        editorialLabel = "Value",
        cutoutLabel = "Auto Cutout",
        effectLabel = "Stylization",
        effectNoneLabel = "None",
        effectCartoonLabel = "Cartoon",
        effectInkLabel = "Ink Spread",
        effectTextureLabel = "Texture",
        effectQuantizeLabel = "Quantize",
        effectHalftoneLabel = "Halftone",
        effectMisprintLabel = "Misprint",
        backgroundColorLabel = "Background Color",
        numberFieldLabel = "Value (Fixed)",
        titleFieldLabel = "Right Text",
        subtitleFieldLabel = "Subtitle",
        numberPlaceholder = STAMP_NUMERIC_STYLE_FIXED_VALUE,
        titlePlaceholder = STAMP_NUMERIC_STYLE_DEFAULT_RIGHT_TEXT,
        subtitlePlaceholder = "",
        confirmLabel = "Apply",
        cancelLabel = "Cancel",
        campfireGateMessage = "Changing stamp style is an early preview feature for Campfire Plan users.",
        campfireGateLearnLabel = "Learn about Campfire Plan"
    )
}

private fun resolveStampDateConfirmLabel(
    language: AppLanguage,
    fallback: String
): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u786e\u8ba4"
    AppLanguage.TraditionalChinese -> "\u78ba\u8a8d"
    else -> fallback.ifBlank { "Confirm" }
}

private fun stampCalendarCopy(language: AppLanguage): StampCalendarCopy = when (language) {
    AppLanguage.SimplifiedChinese -> StampCalendarCopy(
        pageTitle = "\u96c6\u90ae",
        camera = "\u6253\u5f00\u76f8\u673a",
        tabCalendar = "\u65e5\u5386",
        tabTimeline = "\u65f6\u95f4\u7ebf",
        tabWall = "\u5e73\u94fa",
        monthWallTitle = "\u672c\u6708",
        allFilterLabel = "\u6240\u6709",
        emptyHint = "\u8fd8\u6ca1\u6709\u90ae\u7968",
        wallTitle = "\u5168\u90e8\u5e73\u94fa",
        emojiPickerTitle = "\u7ed9\u8fd9\u5f20\u90ae\u7968\u9009\u62e9\u5206\u7ec4",
        clearEmoji = "\u6e05\u9664\u5206\u7ec4",
        detailTitle = "\u90ae\u7968\u8be6\u60c5",
        notePlaceholder = "\u968f\u624b\u8bb0\u5f55\u4e00\u4e0b",
        deleteStamp = "\u5220\u9664\u90ae\u7968",
        deleteConfirmTitle = "\u8981\u5220\u9664\u8fd9\u5f20\u90ae\u7968\u5417\uff1f",
        deleteConfirmBody = "\u5220\u9664\u540e\u4f1a\u540c\u65f6\u4ece\u65e5\u5386\u548c\u5e73\u94fa\u4e2d\u79fb\u9664\u3002",
        deleteConfirmAction = "\u5220\u9664",
        deleteCancelAction = "\u53d6\u6d88",
        thanksTitle = "jeongyoon.design",
        thanksBody = "\u7279\u522b\u611f\u8c22 jeongyoon.design \u7684 queensjournal \u521b\u610f\uff0c\u672c\u529f\u80fd\u53d7\u5176\u542f\u53d1\uff0c\u5e76\u6df1\u6df1\u611f\u6fc0\u5979\u6177\u6168\u5730\u5141\u8bb8\u6211\u4eec\u4f7f\u7528\u8fd9\u4e00\u521b\u610f\u3002",
        thanksConfirm = "OK",
        dateChangeTitle = "\u8c03\u6574\u65e5\u671f",
        dateChangeConfirm = "\u786e\u8ba4",
        dateChangeCancel = "\u53d6\u6d88",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.TraditionalChinese -> StampCalendarCopy(
        pageTitle = "\u96c6\u90f5",
        camera = "\u6253\u958b\u76f8\u6a5f",
        tabCalendar = "\u65e5\u66c6",
        tabTimeline = "\u6642\u9593\u7dda",
        tabWall = "\u5e73\u92ea",
        monthWallTitle = "\u672c\u6708",
        allFilterLabel = "\u6240\u6709",
        emptyHint = "\u9084\u6c92\u6709\u90f5\u7968",
        wallTitle = "\u5168\u90e8\u5e73\u92ea",
        emojiPickerTitle = "\u5e6b\u9019\u5f35\u90f5\u7968\u9078\u64c7\u5206\u7d44",
        clearEmoji = "\u6e05\u9664\u5206\u7d44",
        detailTitle = "\u90f5\u7968\u8a73\u60c5",
        notePlaceholder = "\u96a8\u624b\u8a18\u9304\u4e00\u4e0b",
        deleteStamp = "\u522a\u9664\u90f5\u7968",
        deleteConfirmTitle = "\u8981\u522a\u9664\u9019\u5f35\u90f5\u7968\u55ce\uff1f",
        deleteConfirmBody = "\u522a\u9664\u5f8c\u6703\u540c\u6642\u5f9e\u65e5\u66c6\u548c\u5e73\u92ea\u4e2d\u79fb\u9664\u3002",
        deleteConfirmAction = "\u522a\u9664",
        deleteCancelAction = "\u53d6\u6d88",
        thanksTitle = "jeongyoon.design",
        thanksBody = "\u7279\u5225\u611f\u8b1d jeongyoon.design \u7684 queensjournal \u5275\u610f\uff0c\u672c\u529f\u80fd\u53d7\u5176\u555f\u767c\uff0c\u4e26\u6df1\u6df1\u611f\u6fc0\u5979\u6177\u6168\u5730\u5141\u8a31\u6211\u5011\u4f7f\u7528\u9019\u4e00\u5275\u610f\u3002",
        thanksConfirm = "OK",
        dateChangeTitle = "\u8abf\u6574\u65e5\u671f",
        dateChangeConfirm = "\u78ba\u8a8d",
        dateChangeCancel = "\u53d6\u6d88",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Ukrainian -> StampCalendarCopy(
        pageTitle = "Календар марок",
        camera = "Відкрити камеру",
        tabCalendar = "Календар",
        tabTimeline = "Хронологія",
        tabWall = "Стіна",
        monthWallTitle = "Цього місяця",
        allFilterLabel = "Усі",
        emptyHint = "Марок ще немає",
        wallTitle = "Усі марки",
        emojiPickerTitle = "Згрупувати цю марку за емодзі",
        clearEmoji = "Очистити групу",
        detailTitle = "Деталі марки",
        notePlaceholder = "Напишіть нотатку до цього дня",
        deleteStamp = "Видалити марку",
        deleteConfirmTitle = "Видалити цю марку?",
        deleteConfirmBody = "Її буде видалено з календаря і стіни.",
        deleteConfirmAction = "Видалити",
        deleteCancelAction = "Скасувати",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Щиро дякуємо jeongyoon.design за концепцію queensjournal, яка надихнула цю функцію, і за щедрий дозвіл розвинути її.",
        thanksConfirm = "OK",
        dateChangeTitle = "Змінити дату",
        dateChangeConfirm = "Перемістити",
        dateChangeCancel = "Скасувати",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Polish -> StampCalendarCopy(
        pageTitle = "Kalendarz znaczków",
        camera = "Otwórz aparat",
        tabCalendar = "Kalendarz",
        tabTimeline = "Oś czasu",
        tabWall = "Ściana",
        monthWallTitle = "Ten miesiąc",
        allFilterLabel = "Wszystkie",
        emptyHint = "Brak znaczków",
        wallTitle = "Wszystkie znaczki",
        emojiPickerTitle = "Zgrupuj ten znaczek z emoji",
        clearEmoji = "Wyczyść grupę",
        detailTitle = "Szczegóły znaczka",
        notePlaceholder = "Dodaj notatkę do tego dnia",
        deleteStamp = "Usuń znaczek",
        deleteConfirmTitle = "Usunąć ten znaczek?",
        deleteConfirmBody = "Zostanie usunięty z kalendarza i ściany.",
        deleteConfirmAction = "Usuń",
        deleteCancelAction = "Anuluj",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Serdecznie dziękujemy jeongyoon.design za koncepcję queensjournal, która zainspirowała tę funkcję, oraz za hojną zgodę na jej rozwinięcie.",
        thanksConfirm = "OK",
        dateChangeTitle = "Zmień datę",
        dateChangeConfirm = "Przenieś",
        dateChangeCancel = "Anuluj",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Czech -> StampCalendarCopy(
        pageTitle = "Kalendář známek",
        camera = "Otevřít fotoaparát",
        tabCalendar = "Kalendář",
        tabTimeline = "Časová osa",
        tabWall = "Zeď",
        monthWallTitle = "Tento měsíc",
        allFilterLabel = "Vše",
        emptyHint = "Zatím žádné známky",
        wallTitle = "Všechny známky",
        emojiPickerTitle = "Seskupit tuto známku podle emoji",
        clearEmoji = "Vymazat skupinu",
        detailTitle = "Detail známky",
        notePlaceholder = "Napište poznámku k tomuto dni",
        deleteStamp = "Smazat známku",
        deleteConfirmTitle = "Smazat tuto známku?",
        deleteConfirmBody = "Bude odstraněna z kalendáře i ze zdi.",
        deleteConfirmAction = "Smazat",
        deleteCancelAction = "Zrušit",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Upřímně děkujeme jeongyoon.design za koncept queensjournal, který tuto funkci inspiroval, a za laskavé svolení na něm stavět.",
        thanksConfirm = "OK",
        dateChangeTitle = "Změnit datum",
        dateChangeConfirm = "Přesunout",
        dateChangeCancel = "Zrušit",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Dutch -> StampCalendarCopy(
        pageTitle = "Postzegelkalender",
        camera = "Camera openen",
        tabCalendar = "Kalender",
        tabTimeline = "Tijdlijn",
        tabWall = "Muur",
        monthWallTitle = "Deze maand",
        allFilterLabel = "Alles",
        emptyHint = "Nog geen postzegels",
        wallTitle = "Alle postzegels",
        emojiPickerTitle = "Groepeer deze postzegel met een emoji",
        clearEmoji = "Groep wissen",
        detailTitle = "Postzegeldetails",
        notePlaceholder = "Schrijf een notitie voor deze dag",
        deleteStamp = "Postzegel verwijderen",
        deleteConfirmTitle = "Deze postzegel verwijderen?",
        deleteConfirmBody = "Hij wordt uit de kalender en de muur verwijderd.",
        deleteConfirmAction = "Verwijder",
        deleteCancelAction = "Annuleer",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Hartelijk dank aan jeongyoon.design voor het queensjournal-concept dat deze functie inspireerde, en voor de ruimhartige toestemming om erop voort te bouwen.",
        thanksConfirm = "OK",
        dateChangeTitle = "Datum wijzigen",
        dateChangeConfirm = "Verplaats",
        dateChangeCancel = "Annuleer",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Swedish -> StampCalendarCopy(
        pageTitle = "Frimärkskalender",
        camera = "Öppna kameran",
        tabCalendar = "Kalender",
        tabTimeline = "Tidslinje",
        tabWall = "Vägg",
        monthWallTitle = "Den här månaden",
        allFilterLabel = "Alla",
        emptyHint = "Inga frimärken ännu",
        wallTitle = "Alla frimärken",
        emojiPickerTitle = "Gruppera det här frimärket med en emoji",
        clearEmoji = "Rensa grupp",
        detailTitle = "Frimärksdetaljer",
        notePlaceholder = "Skriv en anteckning för den här dagen",
        deleteStamp = "Radera frimärke",
        deleteConfirmTitle = "Radera det här frimärket?",
        deleteConfirmBody = "Det tas bort från kalendern och väggen.",
        deleteConfirmAction = "Radera",
        deleteCancelAction = "Avbryt",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Varmt tack till jeongyoon.design för queensjournal-konceptet som inspirerade den här funktionen, och för att vi generöst fick bygga vidare på det.",
        thanksConfirm = "OK",
        dateChangeTitle = "Ändra datum",
        dateChangeConfirm = "Flytta",
        dateChangeCancel = "Avbryt",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Danish -> StampCalendarCopy(
        pageTitle = "Frimærkekalender",
        camera = "Åbn kamera",
        tabCalendar = "Kalender",
        tabTimeline = "Tidslinje",
        tabWall = "Væg",
        monthWallTitle = "Denne måned",
        allFilterLabel = "Alle",
        emptyHint = "Ingen frimærker endnu",
        wallTitle = "Alle frimærker",
        emojiPickerTitle = "Gruppér dette frimærke med en emoji",
        clearEmoji = "Ryd gruppe",
        detailTitle = "Frimærkedetaljer",
        notePlaceholder = "Skriv en note til denne dag",
        deleteStamp = "Slet frimærke",
        deleteConfirmTitle = "Slet dette frimærke?",
        deleteConfirmBody = "Det fjernes fra kalenderen og væggen.",
        deleteConfirmAction = "Slet",
        deleteCancelAction = "Annuller",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Tak til jeongyoon.design for queensjournal-konceptet, der inspirerede denne funktion, og for generøst at lade os bygge videre på det.",
        thanksConfirm = "OK",
        dateChangeTitle = "Skift dato",
        dateChangeConfirm = "Flyt",
        dateChangeCancel = "Annuller",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Norwegian -> StampCalendarCopy(
        pageTitle = "Frimerkekalender",
        camera = "Åpne kamera",
        tabCalendar = "Kalender",
        tabTimeline = "Tidslinje",
        tabWall = "Vegg",
        monthWallTitle = "Denne måneden",
        allFilterLabel = "Alle",
        emptyHint = "Ingen frimerker ennå",
        wallTitle = "Alle frimerker",
        emojiPickerTitle = "Grupper dette frimerket med en emoji",
        clearEmoji = "Tøm gruppe",
        detailTitle = "Frimerkedetaljer",
        notePlaceholder = "Skriv et notat for denne dagen",
        deleteStamp = "Slett frimerke",
        deleteConfirmTitle = "Slette dette frimerket?",
        deleteConfirmBody = "Det fjernes fra kalenderen og veggen.",
        deleteConfirmAction = "Slett",
        deleteCancelAction = "Avbryt",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Hjertelig takk til jeongyoon.design for queensjournal-konseptet som inspirerte denne funksjonen, og for at vi generøst fikk bygge videre på det.",
        thanksConfirm = "OK",
        dateChangeTitle = "Endre dato",
        dateChangeConfirm = "Flytt",
        dateChangeCancel = "Avbryt",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Finnish -> StampCalendarCopy(
        pageTitle = "Postimerkkikalenteri",
        camera = "Avaa kamera",
        tabCalendar = "Kalenteri",
        tabTimeline = "Aikajana",
        tabWall = "Seinä",
        monthWallTitle = "Tämä kuukausi",
        allFilterLabel = "Kaikki",
        emptyHint = "Ei vielä postimerkkejä",
        wallTitle = "Kaikki postimerkit",
        emojiPickerTitle = "Ryhmittele tämä postimerkki emojilla",
        clearEmoji = "Tyhjennä ryhmä",
        detailTitle = "Postimerkin tiedot",
        notePlaceholder = "Kirjoita muistiinpano tälle päivälle",
        deleteStamp = "Poista postimerkki",
        deleteConfirmTitle = "Poistetaanko tämä postimerkki?",
        deleteConfirmBody = "Se poistetaan kalenterista ja seinältä.",
        deleteConfirmAction = "Poista",
        deleteCancelAction = "Kumoa",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Kiitämme sydämellisesti jeongyoon.designia queensjournal-konseptista, joka inspiroi tätä ominaisuutta, sekä anteliaasta luvasta rakentaa sen pohjalta.",
        thanksConfirm = "OK",
        dateChangeTitle = "Muuta päivämäärää",
        dateChangeConfirm = "Siirrä",
        dateChangeCancel = "Kumoa",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Greek -> StampCalendarCopy(
        pageTitle = "Ημερολόγιο γραμματοσήμων",
        camera = "Άνοιγμα κάμερας",
        tabCalendar = "Ημερολόγιο",
        tabTimeline = "Χρονολόγιο",
        tabWall = "Τοίχος",
        monthWallTitle = "Αυτός ο μήνας",
        allFilterLabel = "Όλα",
        emptyHint = "Δεν υπάρχουν γραμματόσημα ακόμη",
        wallTitle = "Όλα τα γραμματόσημα",
        emojiPickerTitle = "Ομαδοποίηση του γραμματοσήμου με emoji",
        clearEmoji = "Εκκαθάριση ομάδας",
        detailTitle = "Λεπτομέρειες γραμματοσήμου",
        notePlaceholder = "Γράψτε μια σημείωση για αυτή την ημέρα",
        deleteStamp = "Διαγραφή γραμματοσήμου",
        deleteConfirmTitle = "Διαγραφή αυτού του γραμματοσήμου;",
        deleteConfirmBody = "Θα αφαιρεθεί από το ημερολόγιο και τον τοίχο.",
        deleteConfirmAction = "Διαγραφή",
        deleteCancelAction = "Ακύρωση",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Ευχαριστούμε θερμά το jeongyoon.design για την ιδέα του queensjournal που ενέπνευσε αυτή τη λειτουργία και για τη γενναιόδωρη άδεια να την αξιοποιήσουμε.",
        thanksConfirm = "OK",
        dateChangeTitle = "Αλλαγή ημερομηνίας",
        dateChangeConfirm = "Μετακίνηση",
        dateChangeCancel = "Ακύρωση",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Romanian -> StampCalendarCopy(
        pageTitle = "Calendar de timbre",
        camera = "Deschide camera",
        tabCalendar = "Calendar",
        tabTimeline = "Cronologie",
        tabWall = "Perete",
        monthWallTitle = "Luna aceasta",
        allFilterLabel = "Toate",
        emptyHint = "Încă nu există timbre",
        wallTitle = "Toate timbrele",
        emojiPickerTitle = "Grupează acest timbru cu un emoji",
        clearEmoji = "Șterge grupul",
        detailTitle = "Detalii timbru",
        notePlaceholder = "Scrie o notiță pentru această zi",
        deleteStamp = "Șterge timbrul",
        deleteConfirmTitle = "Ștergi acest timbru?",
        deleteConfirmBody = "Va fi eliminat din calendar și de pe perete.",
        deleteConfirmAction = "Șterge",
        deleteCancelAction = "Anulează",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Mulțumim din suflet jeongyoon.design pentru conceptul queensjournal care a inspirat această funcție și pentru permisiunea generoasă de a construi pe baza lui.",
        thanksConfirm = "OK",
        dateChangeTitle = "Schimbă data",
        dateChangeConfirm = "Mută",
        dateChangeCancel = "Anulează",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Hungarian -> StampCalendarCopy(
        pageTitle = "Bélyegnaptár",
        camera = "Kamera megnyitása",
        tabCalendar = "Naptár",
        tabTimeline = "Idővonal",
        tabWall = "Fal",
        monthWallTitle = "Ez a hónap",
        allFilterLabel = "Mind",
        emptyHint = "Még nincs bélyeg",
        wallTitle = "Összes bélyeg",
        emojiPickerTitle = "Bélyeg csoportosítása emojival",
        clearEmoji = "Csoport törlése",
        detailTitle = "Bélyeg részletei",
        notePlaceholder = "Írj jegyzetet ehhez a naphoz",
        deleteStamp = "Bélyeg törlése",
        deleteConfirmTitle = "Törlöd ezt a bélyeget?",
        deleteConfirmBody = "Eltávolításra kerül a naptárból és a falról.",
        deleteConfirmAction = "Törlés",
        deleteCancelAction = "Mégsem",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Szívből köszönjük a jeongyoon.design-nak a queensjournal koncepciót, amely ezt a funkciót inspirálta, valamint a nagylelkű engedélyt, hogy építhettünk rá.",
        thanksConfirm = "OK",
        dateChangeTitle = "Dátum módosítása",
        dateChangeConfirm = "Áthelyezés",
        dateChangeCancel = "Mégsem",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Turkish -> StampCalendarCopy(
        pageTitle = "Pul Takvimi",
        camera = "Kamerayı aç",
        tabCalendar = "Takvim",
        tabTimeline = "Zaman Çizelgesi",
        tabWall = "Duvar",
        monthWallTitle = "Bu Ay",
        allFilterLabel = "Tümü",
        emptyHint = "Henüz pul yok",
        wallTitle = "Tüm Pullar",
        emojiPickerTitle = "Bu pulu bir emojiyle grupla",
        clearEmoji = "Grubu temizle",
        detailTitle = "Pul Ayrıntıları",
        notePlaceholder = "Bu gün için not yaz",
        deleteStamp = "Pulu sil",
        deleteConfirmTitle = "Bu pul silinsin mi?",
        deleteConfirmBody = "Takvimden ve duvardan kaldırılacak.",
        deleteConfirmAction = "Sil",
        deleteCancelAction = "İptal",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Bu özelliğe ilham veren queensjournal fikri için ve üzerine inşa etmemize cömertçe izin verdiği için jeongyoon.design'a içtenlikle teşekkür ederiz.",
        thanksConfirm = "OK",
        dateChangeTitle = "Tarihi değiştir",
        dateChangeConfirm = "Taşı",
        dateChangeCancel = "İptal",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Malay -> StampCalendarCopy(
        pageTitle = "Kalendar Setem",
        camera = "Buka kamera",
        tabCalendar = "Kalendar",
        tabTimeline = "Garis Masa",
        tabWall = "Dinding",
        monthWallTitle = "Bulan Ini",
        allFilterLabel = "Semua",
        emptyHint = "Belum ada setem",
        wallTitle = "Semua Setem",
        emojiPickerTitle = "Kumpulkan setem ini dengan emoji",
        clearEmoji = "Kosongkan kumpulan",
        detailTitle = "Butiran Setem",
        notePlaceholder = "Tulis nota untuk hari ini",
        deleteStamp = "Padam setem",
        deleteConfirmTitle = "Padam setem ini?",
        deleteConfirmBody = "Ia akan dialih keluar daripada kalendar dan dinding.",
        deleteConfirmAction = "Padam",
        deleteCancelAction = "Batal",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Setinggi-tinggi terima kasih kepada jeongyoon.design atas konsep queensjournal yang mengilhamkan ciri ini, dan atas kebenaran murah hati untuk membina daripadanya.",
        thanksConfirm = "OK",
        dateChangeTitle = "Tukar tarikh",
        dateChangeConfirm = "Alih",
        dateChangeCancel = "Batal",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Hebrew -> StampCalendarCopy(
        pageTitle = "לוח שנה של בולים",
        camera = "פתיחת המצלמה",
        tabCalendar = "לוח שנה",
        tabTimeline = "ציר זמן",
        tabWall = "קיר",
        monthWallTitle = "החודש",
        allFilterLabel = "הכול",
        emptyHint = "עדיין אין בולים",
        wallTitle = "כל הבולים",
        emojiPickerTitle = "קיבוץ הבול עם אימוג׳י",
        clearEmoji = "ניקוי הקבוצה",
        detailTitle = "פרטי הבול",
        notePlaceholder = "כתבו הערה ליום הזה",
        deleteStamp = "מחיקת בול",
        deleteConfirmTitle = "למחוק את הבול?",
        deleteConfirmBody = "הוא יוסר מלוח השנה ומהקיר.",
        deleteConfirmAction = "מחיקה",
        deleteCancelAction = "ביטול",
        thanksTitle = "jeongyoon.design",
        thanksBody = "תודה מקרב לב ל-jeongyoon.design על הרעיון של queensjournal שהיווה השראה לתכונה הזו, ועל הרשות הנדיבה לבנות עליו.",
        thanksConfirm = "OK",
        dateChangeTitle = "שינוי תאריך",
        dateChangeConfirm = "העברה",
        dateChangeCancel = "ביטול",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.English -> StampCalendarCopy(
        pageTitle = "Stamp Calendar",
        camera = "Open Camera",
        tabCalendar = "Calendar",
        tabTimeline = "Timeline",
        tabWall = "Wall",
        monthWallTitle = "This Month",
        allFilterLabel = "All",
        emptyHint = "No stamps yet",
        wallTitle = "All Stamps",
        emojiPickerTitle = "Group this stamp with an emoji",
        clearEmoji = "Clear group",
        detailTitle = "Stamp Detail",
        notePlaceholder = "Write a note for this day",
        deleteStamp = "Delete stamp",
        deleteConfirmTitle = "Delete this stamp?",
        deleteConfirmBody = "This will remove it from the calendar and wall.",
        deleteConfirmAction = "Delete",
        deleteCancelAction = "Cancel",
        thanksTitle = "jeongyoon.design",
        thanksBody = "With sincere thanks to jeongyoon.design for the queensjournal concept that inspired this feature, and for generously allowing us to build on it.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Korean -> StampCalendarCopy(
        pageTitle = "\uc9dc\uc77c",
        camera = "\uce99\uc790 \uc5f4\uae30",
        tabCalendar = "\uc77c\uc815",
        tabWall = "\ud3f4",
        monthWallTitle = "\uc774\ubd84",
        allFilterLabel = "\ubaa8\ub450",
        emptyHint = "\uc9dc\uc77c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4",
        wallTitle = "\ubaa8\ub4dc \ud3f4",
        emojiPickerTitle = "\uc774 \uc9dc\uc77c\uc5d0 \uc774\ubaa8\ud2b8 \uad50\uc721\ud558\uae30",
        clearEmoji = "\uad50\uc721 \uc9c0\uc6b0\uae30",
        detailTitle = "\uc9dc\uc77c \uc138\uc815",
        notePlaceholder = "\uc624\ub958\ub97c \uae30\ub85d\ud558\uc138\uc694",
        deleteStamp = "\uc9dc\uc77c \uc0ad\uc81c",
        deleteConfirmTitle = "\uc774 \uc9dc\uc77c\uc744 \uc0ad\uc81c\ud558\uc2dc\uaca0\uc2b5\ub2c8\uae4c?",
        deleteConfirmBody = "\uc774\ub294 \uc77c\uc815\uacfc \ud3f4\uc5d0\uc11c\ub3c4 \uc81c\uac70\ub429\ub2c8\ub2e4.",
        deleteConfirmAction = "\uc0ad\uc81c",
        deleteCancelAction = "\ucde8\uc18c",
        thanksTitle = "jeongyoon.design",
        thanksBody = "jeongyoon.design\uc5d0\uac8c queensjournal \uc799\uc0c1\uc744 \ubd80\uc5b4\ub4e0 \uc774 \uae30\ub2a5\uc744 \uac1c\ubc1c\ud588\uc744 \ub9fa\ud655 \ube44\uae30 \uacc4\uc18d\ud574\uc8fc\uc2dc\ub124\uc694. \uc774 \uc791\uc5c5\uc744 \uc0ac\uc6a9\ud558\ub294 \uad8c\ud55c\uc744 \ub0a0\uc544\uc8fc\uc2dc\ub294 \uadf8\ub807\uac8c \ud3c9\uac00\ud558\uc2dc\ub124\uc694.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Japanese -> StampCalendarCopy(
        pageTitle = "\u7d19\u90f5",
        camera = "\u30ab\u30e1\u30e9\u3092\u958b\u304f",
        tabCalendar = "\u30ab\u30ec\u30f3\u30c0\u30fc",
        tabWall = "\u30a6\u30a9\u30fc\u30eb",
        monthWallTitle = "\u4eca\u6708",
        allFilterLabel = "\u3059\u3079\u3066",
        emptyHint = "\u7d19\u90f5\u306f\u3042\u308a\u307e\u305b\u3093",
        wallTitle = "\u3059\u3079\u3066\u306e\u30a6\u30a9\u30fc\u30eb",
        emojiPickerTitle = "\u3053\u306e\u7d19\u90f5\u306b\u30a8\u30e2\u30b8\u30a2\u3092\u4e00\u7fa4\u5316",
        clearEmoji = "\u30b0\u30eb\u30fc\u30d7\u3092\u30af\u30ea\u30a2",
        detailTitle = "\u7d19\u90f5\u8a73\u7d30",
        notePlaceholder = "\u3053\u306e\u65e5\u306e\u5099\u5fd8\u9332\u3092\u66f8\u3044\u3066\u304f\u3060\u3055\u3044",
        deleteStamp = "\u7d19\u90f5\u3092\u524a\u9664",
        deleteConfirmTitle = "\u3053\u306e\u7d19\u90f5\u3092\u524a\u9664\u3057\u307e\u3059\u304b\uff1f",
        deleteConfirmBody = "\u30ab\u30ec\u30f3\u30c0\u30fc\u3068\u30a6\u30a9\u30fc\u30eb\u306e\u4e21\u65b9\u304b\u3089\u524a\u9664\u3055\u308c\u307e\u3059\u3002",
        deleteConfirmAction = "\u524a\u9664",
        deleteCancelAction = "\u30ad\u30e3\u30f3\u30bb\u30eb",
        thanksTitle = "jeongyoon.design",
        thanksBody = "\u3053\u306e\u6a5f\u80fd\u306e\u767a\u696d\u306b\u3042\u305f\u308a\u3001jeongyoon.design\u3055\u3093\u306equeensjournal\u30a2\u30a4\u30c7\u30a2\u306b\u6df1\u304f\u611f\u8b1d\u3057\u3066\u304a\u308a\u307e\u3059\u3002\u3053\u306e\u30a2\u30a4\u30c7\u30a2\u3092\u4f7f\u7528\u3055\u305b\u3066\u3044\u305f\u3060\u304d\u3001\u614e\u3057\u3044\u3051\u3069\u8cd3\u308b\u5fc5\u8981\u306f\u3042\u308a\u307e\u305b\u3093\u3002",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.French -> StampCalendarCopy(
        pageTitle = "Carnet de Timbres",
        camera = "Ouvrir l'appareil photo",
        tabCalendar = "Calendrier",
        tabWall = "Mur",
        monthWallTitle = "Ce mois",
        allFilterLabel = "Tout",
        emptyHint = "Pas encore de timbres",
        wallTitle = "Tous les timbres",
        emojiPickerTitle = "Grouper ce timbre avec un emoji",
        clearEmoji = "Effacer le groupe",
        detailTitle = "D闂備浇鍋愰崢褍鐣甸幓鐮砽 du timbre",
        notePlaceholder = "闂備礁鍚嬮幆宀勫箺椤＄珨re une note pour ce jour",
        deleteStamp = "Supprimer le timbre",
        deleteConfirmTitle = "Supprimer ce timbre ?",
        deleteConfirmBody = "Il sera retir闂?du calendrier et du mur.",
        deleteConfirmAction = "Supprimer",
        deleteCancelAction = "Annuler",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Nous adressons nos plus sinc闂備胶绮銊╃嵁瀹稿澃 remerciements 闂?jeongyoon.design pour le concept queensjournal qui a inspir闂?cette fonctionnalit闂? Nous lui sommes profond闂備浇鍋愰崢褍鐣风猾锕弔 reconnaissants pour sa g闂備浇鍋愰崢褍鐣烽崸妤佺厾闁绘挸楠哥粊绯磚se autorisation d'utiliser cette id闂備浇鍋愰崢褍鐣?",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.German -> StampCalendarCopy(
        pageTitle = "Briefmarken-Kalender",
        camera = "Kamera 闂備礁鎼鍡涙偤閻en",
        tabCalendar = "Kalender",
        tabWall = "Wand",
        monthWallTitle = "Dieser Monat",
        allFilterLabel = "Alle",
        emptyHint = "Noch keine Briefmarken",
        wallTitle = "Alle Briefmarken",
        emojiPickerTitle = "Diese Briefmarke mit einem Emoji gruppieren",
        clearEmoji = "Gruppe l闂備礁鎼鍡涱敆閻犵帴en",
        detailTitle = "Briefmarken-Detail",
        notePlaceholder = "Eine Notiz f闂備焦妞挎禍婊堝Υ?diesen Tag schreiben",
        deleteStamp = "Briefmarke l闂備礁鎼鍡涱敆閻犵帴en",
        deleteConfirmTitle = "Diese Briefmarke l闂備礁鎼鍡涱敆閻犵帴en?",
        deleteConfirmBody = "Sie wird aus dem Kalender und der Wand entfernt.",
        deleteConfirmAction = "L闂備礁鎼鍡涱敆閻犵帴en",
        deleteCancelAction = "Abbrechen",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Wir m闂備礁鎼鍡涙偤閻犵殞en unseren tiefsten Dank an jeongyoon.design f闂備焦妞挎禍婊堝Υ?das queensjournal-Konzept aussprechen, das diese Funktion inspiriert hat. Wir sind zutiefst dankbar f闂備焦妞挎禍婊堝Υ?ihre gro闂備胶鍘ч顓㈠箰閿熺姵鐓熸い蹇撳閳ь剚鐛檊e Genehmigung, diese Idee zu verwenden.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Italian -> StampCalendarCopy(
        pageTitle = "Calendario Francobolli",
        camera = "Apri Fotocamera",
        tabCalendar = "Calendario",
        tabWall = "Muro",
        monthWallTitle = "Questo Mese",
        allFilterLabel = "Tutto",
        emptyHint = "Nessun francobollo ancora",
        wallTitle = "Tutti i Francobolli",
        emojiPickerTitle = "Raggruppa questo francobollo con un emoji",
        clearEmoji = "Cancella gruppo",
        detailTitle = "Dettaglio Francobollo",
        notePlaceholder = "Scrivi una nota per questo giorno",
        deleteStamp = "Elimina francobollo",
        deleteConfirmTitle = "Eliminare questo francobollo?",
        deleteConfirmBody = "Sar闂?rimosso dal calendario e dal muro.",
        deleteConfirmAction = "Elimina",
        deleteCancelAction = "Annulla",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Esprimiamo la nostra pi闂?profonda gratitudine a jeongyoon.design per il concetto di queensjournal che ha ispirato questa funzione. Siamo profondamente grati per la sua generosa autorizzazione all'uso di questa idea.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Portuguese -> StampCalendarCopy(
        pageTitle = "Calend闂佽崵濮撮鎴犵不缁夊潵 de Selos",
        camera = "Abrir C闂備浇鍋愰崕銈嗘叏椤╁獰a",
        tabCalendar = "Calend闂佽崵濮撮鎴犵不缁夊潵",
        tabWall = "Parede",
        monthWallTitle = "Este M闂傚倷鐒︾€笛囶敆?",
        allFilterLabel = "Todos",
        emptyHint = "Ainda n闂備浇澹堥崑鎰扮嵁?h闂?selos",
        wallTitle = "Todos os Selos",
        emojiPickerTitle = "Agrupar este selo com um emoji",
        clearEmoji = "Limpar grupo",
        detailTitle = "Detalhe do Selo",
        notePlaceholder = "Escreva uma nota para este dia",
        deleteStamp = "Excluir selo",
        deleteConfirmTitle = "Excluir este selo?",
        deleteConfirmBody = "Ele ser闂?removido do calend闂佽崵濮撮鎴犵不缁夊潵 e da parede.",
        deleteConfirmAction = "Excluir",
        deleteCancelAction = "Cancelar",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Expressamos nossa mais profunda gratid闂備浇澹堥崑鎰扮嵁?a jeongyoon.design pelo conceito queensjournal que inspirou esta funcionalidade. Somos profundamente grate por sua generosa permiss闂備浇澹堥崑鎰扮嵁?para usar esta ideia.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Spanish -> StampCalendarCopy(
        pageTitle = "Calendario de Sellos",
        camera = "Abrir C闂佽崵濮撮鎴犵不閻㈩晿a",
        tabCalendar = "Calendario",
        tabWall = "Pared",
        monthWallTitle = "Este Mes",
        allFilterLabel = "Todo",
        emptyHint = "A闂備胶绮幖鈺佄?no hay sellos",
        wallTitle = "Todos los Sellos",
        emojiPickerTitle = "Agrupar este sello con un emoji",
        clearEmoji = "Borrar grupo",
        detailTitle = "Detalle del Sello",
        notePlaceholder = "Escribe una nota para este d闂傚倷鐒﹂崕鎶藉箺?",
        deleteStamp = "Eliminar sello",
        deleteConfirmTitle = "濠碉紕鍋涘﹢閬嶃€傞惂绯筸inar este sello?",
        deleteConfirmBody = "Se eliminar闂?del calendario y de la pared.",
        deleteConfirmAction = "Eliminar",
        deleteCancelAction = "Cancelar",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Expresamos nuestra m闂佽崵濮撮鎴犵不?profunda gratitud a jeongyoon.design por el concepto de queensjournal que inspir闂?esta funci闂佽崵濮甸崝妤呮偟? Le estamos profundamente agradecidos por su generoso permiso para usar esta idea.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Russian -> StampCalendarCopy(
        pageTitle = "\u041a\u0430\u043b\u0435\u043d\u0434\u0430\u0440\u044c \u043c\u0430\u0440\u043e\u043a",
        camera = "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043a\u0430\u043c\u0435\u0440\u0443",
        tabCalendar = "\u041a\u0430\u043b\u0435\u043d\u0434\u0430\u0440\u044c",
        tabWall = "\u0421\u0442\u0435\u043d\u0430",
        monthWallTitle = "\u042d\u0442\u043e\u0442 \u043c\u0435\u0441\u044f\u0446",
        allFilterLabel = "\u0412\u0441\u0435",
        emptyHint = "\u041c\u0430\u0440\u043e\u043a \u043f\u043e\u043a\u0430 \u043d\u0435\u0442",
        wallTitle = "\u0412\u0441\u0435 \u043c\u0430\u0440\u043a\u0438",
        emojiPickerTitle = "\u0421\u0433\u0440\u0443\u043f\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u044d\u0442\u043e\u0442 \u043c\u0430\u0440\u043a\u0443 \u0441 \u044d\u043c\u043e\u0434\u0437\u0438",
        clearEmoji = "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u0433\u0440\u0443\u043f\u043f\u0443",
        detailTitle = "\u0414\u0435\u0442\u0430\u043b\u0438 \u043c\u0430\u0440\u043a\u0438",
        notePlaceholder = "\u041d\u0430\u043f\u0438\u0441\u0430\u0442\u044c \u0437\u0430\u043f\u0438\u0441\u043a\u0443 \u043d\u0430 \u044d\u0442\u043e\u0442 \u0434\u0435\u043d\u044c",
        deleteStamp = "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u043c\u0430\u0440\u043a\u0443",
        deleteConfirmTitle = "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u044d\u0442\u0443 \u043c\u0430\u0440\u043a\u0443?",
        deleteConfirmBody = "\u041e\u043d\u0430 \u0431\u0443\u0434\u0435\u0442 \u0443\u0434\u0430\u043b\u0435\u043d\u0430 \u0438\u0437 \u043a\u0430\u043b\u0435\u043d\u0434\u0430\u0440\u044f \u0438 \u0441\u043e \u0441\u0442\u0435\u043d\u044b.",
        deleteConfirmAction = "\u0423\u0434\u0430\u043b\u0438\u0442\u044c",
        deleteCancelAction = "\u041e\u0442\u043c\u0435\u043d\u0430",
        thanksTitle = "jeongyoon.design",
        thanksBody = "\u041c\u044b \u0432\u044b\u0440\u0430\u0436\u0430\u0435\u043c \u0438\u0441\u043a\u0440\u0435\u043d\u043d\u044e\u044e \u0431\u043b\u0430\u0433\u043e\u0434\u0430\u0440\u043d\u043e\u0441\u0442\u044c jeongyoon.design \u0437\u0430 \u043a\u043e\u043d\u0446\u0435\u043f\u0446\u0438\u044e queensjournal, \u043a\u043e\u0442\u043e\u0440\u0430\u044f \u0432\u0434\u043e\u0445\u043d\u043e\u0432\u0438\u043b\u0430 \u044d\u0442\u0443 \u0444\u0443\u043d\u043a\u0446\u0438\u044e. \u041c\u044b \u0433\u043b\u0443\u0431\u043e\u043a\u043e \u043f\u0440\u0438\u0437\u043d\u0430\u0442\u0435\u043b\u044c\u043d\u044b \u0437\u0430 \u0435\u0435 \u0449\u0435\u0434\u0440\u043e\u0435 \u0440\u0430\u0437\u0440\u0435\u0448\u0435\u043d\u0438\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u044d\u0442\u0443 \u0438\u0434\u0435\u044e.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Thai -> StampCalendarCopy(
        pageTitle = "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e44\u0e17\u0e21\u0e47\u0e2d\u0e01\u0e4c",
        camera = "\u0e40\u0e1b\u0e34\u0e14\u0e01\u0e23\u0e30\u0e21\u0e32\u0e13\u0e30",
        tabCalendar = "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19",
        tabWall = "\u0e01\u0e33\u0e2b\u0e19\u0e14",
        monthWallTitle = "\u0e40\u0e14\u0e37\u0e2d\u0e19\u0e19\u0e35\u0e49",
        allFilterLabel = "\u0e17\u0e31\u0e49\u0e07\u0e2b\u0e21\u0e14",
        emptyHint = "\u0e22\u0e31\u0e07\u0e44\u0e21\u0e48\u0e21\u0e35\u0e44\u0e17\u0e22\u0e4c",
        wallTitle = "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e17\u0e31\u0e49\u0e07\u0e2b\u0e21\u0e14",
        emojiPickerTitle = "\u0e08\u0e31\u0e14\u0e40\u0e27\u0e34\u0e08\u0e19\u0e44\u0e17\u0e22\u0e4c\u0e19\u0e35\u0e49\u0e01\u0e31\u0e1a\u0e2d\u0e34\u0e42\u0e21\u0e08\u0e34",
        clearEmoji = "\u0e25\u0e1a\u0e01\u0e25\u0e38\u0e02\u0e2d\u0e1a\u0e04\u0e25\u0e38\u0e2d",
        detailTitle = "\u0e23\u0e32\u0e22\u0e25\u0e30\u0e40\u0e2d\u0e35\u0e22\u0e19\u0e44\u0e17\u0e22\u0e4c",
        notePlaceholder = "\u0e40\u0e1e\u0e34\u0e48\u0e19\u0e1a\u0e31\u0e19\u0e17\u0e35\u0e48\u0e2a\u0e33\u0e2b\u0e23\u0e31\u0e49\u0e07\u0e2b\u0e21\u0e14",
        deleteStamp = "\u0e25\u0e1a\u0e44\u0e17\u0e22\u0e4c",
        deleteConfirmTitle = "\u0e25\u0e1a\u0e44\u0e17\u0e22\u0e4c\u0e19\u0e35\u0e49\u0e43\u0e0a\u0e48\u0e44\u0e14\u0e49\u0e43\u0e0b\u0e48?",
        deleteConfirmBody = "\u0e44\u0e17\u0e22\u0e4c\u0e08\u0e30\u0e16\u0e39\u0e01\u0e32\u0e22\u0e08\u0e32\u0e01\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e41\u0e25\u0e30\u0e01\u0e33\u0e2b\u0e19\u0e14\u0e40\u0e14\u0e35\u0e22\u0e27\u0e01\u0e32\u0e25\u0e1a\u0e4c\u0e01\u0e23\u0e30\u0e1a\u0e34\u0e01\u0e4c",
        deleteConfirmAction = "\u0e25\u0e1a",
        deleteCancelAction = "\u0e22\u0e01\u0e40\u0e25\u0e34\u0e01",
        thanksTitle = "jeongyoon.design",
        thanksBody = "\u0e40\u0e17\u0e48\u0e32\u0e04\u0e27\u0e32\u0e19\u0e20\u0e39\u0e21\u0e34\u0e02\u0e2d\u0e1a jeongyoon.design \u0e2a\u0e33\u0e2b\u0e23\u0e31\u0e49\u0e07\u0e2b\u0e25\u0e31\u0e01\u0e2a\u0e23\u0e30\u0e1a\u0e32\u0e07 queensjournal \u0e17\u0e35\u0e48\u0e40\u0e1b\u0e47\u0e19\u0e41\u0e19\u0e48\u0e07\u0e01\u0e32\u0e23\u0e4c\u0e1a\u0e49\u0e31\u0e19\u0e01\u0e32\u0e23\u0e08\u0e31\u0e14\u0e41\u0e15\u0e48\u0e07\u0e19\u0e35\u0e49 \u0e41\u0e25\u0e30\u0e23\u0e39\u0e1b\u0e48\u0e32\u0e01\u0e25\u0e35\u0e44\u0e1e\u0e23\u0e30\u0e04\u0e37\u0e2d\u0e17\u0e35\u0e48\u0e21\u0e35\u0e01\u0e23\u0e30\u0e1a\u0e34\u0e01\u0e4c\u0e43\u0e2b\u0e49\u0e1e\u0e47\u0e2a\u0e21\u0e23\u0e14\u0e02\u0e2d\u0e07\u0e19\u0e35\u0e49\u0e43\u0e0a\u0e49\u0e44\u0e1b\u0e49\u0e48\u0e32\u0e07",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Vietnamese -> StampCalendarCopy(
        pageTitle = "Lich Suu Tem",
        camera = "Mo May Anh",
        tabCalendar = "Lich",
        tabWall = "Tuong",
        monthWallTitle = "Thang Nay",
        allFilterLabel = "Tat Ca",
        emptyHint = "Chua co tem",
        wallTitle = "Tat Ca Tem",
        emojiPickerTitle = "Gan emoji cho tem nay",
        clearEmoji = "Xoa emoji",
        detailTitle = "Chi Tiet Tem",
        notePlaceholder = "Viet ghi chu cho ngay nay",
        deleteStamp = "Xoa tem",
        deleteConfirmTitle = "Xoa tem nay?",
        deleteConfirmBody = "Tem se bi xoa khoi lich va tuong.",
        deleteConfirmAction = "Xoa",
        deleteCancelAction = "Huy",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Cam on jeongyoon.design va y tuong queensjournal da truyen cam hung cho tinh nang nay.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Indonesian -> StampCalendarCopy(
        pageTitle = "Kalender Prangko",
        camera = "Buka Kamera",
        tabCalendar = "Kalender",
        tabWall = "Dinding",
        monthWallTitle = "Bulan Ini",
        allFilterLabel = "Semua",
        emptyHint = "Belum ada prangko",
        wallTitle = "Semua Prangko",
        emojiPickerTitle = "Kelompokkan prangko ini dengan emoji",
        clearEmoji = "Hapus kelompok",
        detailTitle = "Detail Prangko",
        notePlaceholder = "Tulis catatan untuk hari ini",
        deleteStamp = "Hapus prangko",
        deleteConfirmTitle = "Hapus prangko ini?",
        deleteConfirmBody = "Ini akan dihapus dari kalender dan dinding.",
        deleteConfirmAction = "Hapus",
        deleteCancelAction = "Batal",
        thanksTitle = "jeongyoon.design",
        thanksBody = "Kami menyampaikan terima kasih terdalam kami kepada jeongyoon.design atas konsep queensjournal yang menginspirasi fitur ini. Kami sangat bersyukur atas izinnya yang dermawan untuk menggunakan ide ini.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
    AppLanguage.Arabic -> StampCalendarCopy(
        pageTitle = "\u062a\u0642\u0648\u064a\u0645 \u0627\u0644\u0639\u064a\u0627\u0631\u0627\u062a",
        camera = "\u0641\u062a\u062d \u0627\u0644\u0643\u0627\u0645\u064a\u0631\u0627",
        tabCalendar = "\u0627\u0644\u062a\u0642\u0648\u064a\u0645",
        tabWall = "\u0627\u0644\u062c\u062f\u0627\u0631",
        monthWallTitle = "\u0647\u0630\u0627 \u0627\u0644\u0634\u0647\u0631",
        allFilterLabel = "\u0627\u0644\u0643\u0644",
        emptyHint = "\u0644\u0627 \u062a\u0648\u062c\u062f \u0639\u064a\u0627\u0631\u0627\u062a \u0628\u0639\u062f",
        wallTitle = "\u062c\u0645\u064a\u0639 \u0627\u0644\u0639\u064a\u0627\u0631\u0627\u062a",
        emojiPickerTitle = "\u062c\u0645\u0639 \u0647\u0630\u0627 \u0627\u0644\u0639\u064a\u0627\u0631\u0629 \u0645\u0639 \u0625\u064a\u0645\u0648\u062c\u064a",
        clearEmoji = "\u0645\u0633\u062d \u0627\u0644\u0645\u062c\u0645\u0648\u0639\u0629",
        detailTitle = "\u062a\u0641\u0627\u0635\u064a\u0644 \u0627\u0644\u0639\u064a\u0627\u0631\u0629",
        notePlaceholder = "\u0627\u0643\u062a\u0628 \u0645\u0644\u0627\u062d\u0638\u0629 \u0644\u0647\u0630\u0627 \u0627\u0644\u064a\u0648\u0645",
        deleteStamp = "\u062d\u0630\u0641 \u0627\u0644\u0639\u064a\u0627\u0631\u0629",
        deleteConfirmTitle = "\u062d\u0630\u0641 \u0647\u0630\u0647 \u0627\u0644\u0639\u064a\u0627\u0631\u0629\u061f",
        deleteConfirmBody = "\u0633\u064a\u062a\u0645 \u0625\u0632\u0627\u0644\u062a\u0647\u0627 \u0645\u0646 \u0627\u0644\u062a\u0642\u0648\u064a\u0645 \u0648\u0627\u0644\u062c\u062f\u0627\u0631.",
        deleteConfirmAction = "\u062d\u0630\u0641",
        deleteCancelAction = "\u0625\u0644\u063a\u0627\u0621",
        thanksTitle = "jeongyoon.design",
        thanksBody = "\u0646\u0642\u062f\u0645 \u062e\u0627\u0644\u0635 \u0627\u0644\u0634\u0643\u0631 \u0644\u0640 jeongyoon.design \u0639\u0644\u0649 \u0645\u0641\u0647\u0648\u0645 queensjournal \u0627\u0644\u0630\u064a \u0644\u0647\u062f\u062a \u0647\u0630\u0647 \u0627\u0644\u0645\u064a\u0632\u0629. \u0646\u062d\u0646 \u0634\u0627\u0643\u0631\u064a\u0646 \u0628\u0633\u0628\u064a\u0644 \u0627\u0644\u0633\u0645\u0627\u062d\u0629 \u0627\u0644\u062a\u064a \u0648\u0641\u0631\u062a\u0647\u0627 \u0644\u0646\u0627 \u0628\u0627\u0633\u062a\u062e\u062f\u0627\u0645 \u0647\u0630\u0627 \u0627\u0644\u0641\u0643\u0631\u0629.",
        thanksConfirm = "OK",
        weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    )
}
