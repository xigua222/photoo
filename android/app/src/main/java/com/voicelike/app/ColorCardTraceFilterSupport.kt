package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.voicelike.app.colorcard.portrait.COLOR_CARD_TRACE_PORTRAIT_DISABLED
import com.voicelike.app.colorcard.portrait.ColorCardPortraitOptimizationParams
import com.voicelike.app.colorcard.portrait.applyColorCardPortraitOptimization
import com.voicelike.app.colorcard.portrait.resolveColorCardPortraitOptimizationParams
import com.voicelike.app.colorcard.portrait.resolveColorCardPortraitSharpen
import com.voicelike.app.colorcard.render.applyColorCardTraceAntiBanding
import com.voicelike.app.colorcard.render.applyColorCardTraceLightPresenceEnhancement
import com.voicelike.app.colorcard.render.resolveColorCardTraceAntiBandingMix
import com.voicelike.app.colorcard.lensy.ColorCardLensyOverlay
import com.voicelike.app.motionphoto.MOTION_PHOTO_FILTER_ENTRY_ID
import com.voicelike.app.motionphoto.MotionPhotoFrameRenderer
import com.voicelike.app.motionphoto.MotionPhotoRevealPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val COLOR_CARD_TRACE_TAG = "ColorCardTrace"
private val COLOR_CARD_TRACE_TOP_BAR_BUTTON_HEIGHT = 40.dp
private val COLOR_CARD_TRACE_TOP_BAR_TOP_PADDING = 12.dp
private val COLOR_CARD_TRACE_TOP_BAR_LENSY_CLEARANCE = 8.dp
private val COLOR_CARD_TRACE_LENSY_OUTSIDE_GAP = 10.dp
private val COLOR_CARD_TRACE_LENSY_ESTIMATED_HEIGHT = 56.dp

@Composable
internal fun rememberColorCardTraceAdjustedImageBounds(
    bounds: Rect?
): Rect? {
    val density = LocalDensity.current
    val statusBarInsetPx = WindowInsets.statusBars.getTop(density)

    return remember(bounds, density, statusBarInsetPx) {
        bounds?.let {
            resolveColorCardTraceAdjustedImageBounds(
                bounds = it,
                density = density,
                statusBarInsetPx = statusBarInsetPx
            )
        }
    }
}

internal fun resolveColorCardTraceAdjustedImageBounds(
    bounds: Rect,
    density: androidx.compose.ui.unit.Density,
    statusBarInsetPx: Int
): Rect {
    if (bounds.width <= 1f || bounds.height <= 1f) return bounds

    val requiredImageTop = statusBarInsetPx.toFloat() + with(density) {
        COLOR_CARD_TRACE_TOP_BAR_TOP_PADDING.toPx() +
            COLOR_CARD_TRACE_TOP_BAR_BUTTON_HEIGHT.toPx() +
            COLOR_CARD_TRACE_TOP_BAR_LENSY_CLEARANCE.toPx() +
            COLOR_CARD_TRACE_LENSY_ESTIMATED_HEIGHT.toPx() +
            COLOR_CARD_TRACE_LENSY_OUTSIDE_GAP.toPx()
    }
    val offsetY = (requiredImageTop - bounds.top).coerceAtLeast(0f)
    if (offsetY <= 0.5f) return bounds

    return Rect(
        left = bounds.left,
        top = bounds.top + offsetY,
        right = bounds.right,
        bottom = bounds.bottom + offsetY
    )
}
private const val COLOR_CARD_TRACE_FILTER_GPU_MAX_SIDE = 1280
private const val COLOR_CARD_TRACE_FILTER_SAVE_MAX_SIDE = 2560
private const val COLOR_CARD_TRACE_BEADS_PREVIEW_MAX_SIDE = 2048
private const val COLOR_CARD_TRACE_AI_PREVIEW_MAX_SIDE = 960
private const val COLOR_CARD_TRACE_AI_QUICK_PREVIEW_MAX_SIDE = 512
private const val COLOR_CARD_TRACE_AI_EXPORT_MAX_SIDE = 1920
private const val COLOR_CARD_TRACE_FACE_DETECTION_MAX_SIDE = 960
private const val COLOR_CARD_TRACE_AI_PREVIEW_THREAD_COUNT = 2
private const val COLOR_CARD_TRACE_AI_EXPORT_THREAD_COUNT = 1
private const val COLOR_CARD_TRACE_AI_PREVIEW_DEBOUNCE_MS = 96L
private const val COLOR_CARD_TRACE_FILTER_RESOURCE_PREFETCH_DELAY_MS = 180L
private const val COLOR_CARD_TRACE_PREVIEW_WARMUP_DELAY_MS = 120L
private const val COLOR_CARD_TRACE_TRACE_CANDIDATE_PREFETCH_DELAY_MS = 160L
private const val COLOR_CARD_TRACE_BUILTIN_PREVIEW_BATCH_SIZE = 12
private const val COLOR_CARD_TRACE_FILTER_REVEAL_DURATION_MS = 620
private const val COLOR_CARD_TRACE_AI_EXIT_HOLD_MS = 180L
private const val COLOR_CARD_TRACE_RENDERED_PREVIEW_EXIT_HOLD_MS = 120L
private const val COLOR_CARD_TRACE_FILTER_PREVIEW_MAX_SIDE = 224
private const val COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_STRENGTH = 0.92f
private const val COLOR_CARD_TRACE_FEED_PREVIEW_STRENGTH = 0.5f
private const val COLOR_CARD_TRACE_BUILTIN_PREVIEW_DISK_CACHE_DIR = "color_card_filter_previews"
private const val COLOR_CARD_TRACE_BUILTIN_PREVIEW_DISK_CACHE_VERSION = 6
private const val COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_SATURATION = 0.10f
private const val COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_BRIGHTNESS = 0.03f
private const val COLOR_CARD_TRACE_FOLLOW_BIAS_SCALE = 0.8f
private const val COLOR_CARD_TRACE_FILTER_PREVIEW_SAMPLE_WEBP = "color_card_filter_preview_sample.webp"
private const val COLOR_CARD_TRACE_FILTER_PREVIEW_SAMPLE_PNG = "color_card_filter_preview_sample.png"
private val COLOR_CARD_TRACE_FILTER_PREVIEW_EXCLUDED_FEED_DEMO_IDS = setOf(
    "watermark_template_demo",
    "sample_12"
)
internal const val COLOR_CARD_TRACE_FILTER_MAX_ANCHORS = 8
private val COLOR_CARD_TRACE_AI_FILTER_PANEL_MIN_HEIGHT = 244.dp
internal val COLOR_CARD_TRACE_CAMERA_FILTER_PANEL_STABLE_MIN_HEIGHT = 244.dp
private const val COLOR_CARD_TRACE_ADJUSTMENT_DETENTS = 11
private const val COLOR_CARD_TRACE_ADJUSTMENT_GRID_COLUMNS = 11
private const val COLOR_CARD_TRACE_ADJUSTMENT_GRID_ROWS = 11
private const val COLOR_CARD_TRACE_ADJUSTMENT_GRID_WIDTH_FRACTION = 0.72f
private const val COLOR_CARD_TRACE_ADJUSTMENT_GRID_HEIGHT_FRACTION = 0.62f
private const val COLOR_CARD_TRACE_BRIGHTNESS_SHIFT = 18.0
private const val COLOR_CARD_TRACE_COLOR_SHIFT = 16.0
private const val COLOR_CARD_TRACE_SATURATION_SCALE = 0.75
private const val COLOR_CARD_TRACE_FILTER_COLOR_RESPONSE_SCALE = 0.62f
private const val COLOR_CARD_TRACE_FILTER_TONE_RESPONSE_SCALE = 0.58f
internal const val COLOR_CARD_TRACE_SHARPEN_DETAIL_SCALE = 3.45
internal const val COLOR_CARD_TRACE_SOFTEN_BLEND_SCALE = 0.82
private const val COLOR_CARD_TRACE_BUILTIN_NAME_SEED = 2026031301L
private val COLOR_CARD_TRACE_HIDDEN_BUILTIN_FILTER_IDS = setOf(
    "presetpro-rich-tines",
    "presetpro-santorini",
    "generate-by-resolve-2",
    "generate-by-resolve-3"
)
private val COLOR_CARD_TRACE_HIDDEN_BUILTIN_FILTER_TITLES = setOf(
    "#947B",
    "#946B",
    "#152B",
    "#153B"
)
private val COLOR_CARD_TRACE_AI_PLACEHOLDER_COLORS = listOf(
    0xFF14181C.toInt(),
    0xFF30475E.toInt(),
    0xFF5BC0BE.toInt(),
    0xFFF3F4F6.toInt()
)
private val COLOR_CARD_TRACE_FILM_KEYWORDS = listOf(
    "fuji",
    "fuj",
    "velvia",
    "astia",
    "classic chrome",
    "classic neg",
    "eterna",
    "nostalgic neg",
    "pro neg",
    "provia",
    "reala ace",
    "polaroid",
    "negative",
    "polachrome"
)
private val COLOR_CARD_TRACE_CINEMA_KEYWORDS = listOf(
    "cinematic",
    "cinema",
    "cine",
    "bleach bypass"
)
private val COLOR_CARD_TRACE_FILM_FILTER_IDS = setOf("fj", "cc", "cc1", "cc2")
private val colorCardTraceBuiltinPreviewBitmapCacheLock = Any()
private val colorCardTraceBuiltinPreviewBitmapCache = mutableMapOf<String, Bitmap>()
private var colorCardTraceBuiltinPreviewLastFeedRefreshSignature: String? = null

internal fun isColorCardTraceBlackWhiteEraCandidate(
    candidate: ColorCardTraceFilterCandidate?
): Boolean = candidate?.title?.trim()?.endsWith("B", ignoreCase = true) == true

internal fun defaultColorCardTracePaletteStrength(
    candidate: ColorCardTraceFilterCandidate?
): Float = if (isColorCardTraceBlackWhiteEraCandidate(candidate)) 1f else 0.5f

internal enum class ColorCardTracePanelMode {
    Picker,
    Filter,
    Trace,
    Imitate,
    Recipe
}

internal enum class ColorCardTraceSaveImageState {
    Idle,
    Saving,
    Saved
}

@Immutable
internal enum class ColorCardTraceFilterKind {
    Palette,
    HaldClut,
    AiMnn,
    PixelPoster,
    SelectiveColor,
    SubjectFrame,
    BlackMist,
    MotionPhotoEntry
}

@Immutable
internal data class ColorCardTraceFilterCandidate(
    val paletteId: String,
    val kind: ColorCardTraceFilterKind,
    val title: String,
    val sourceTag: String?,
    val colors: List<Int>,
    val score: Double,
    val haldAssetName: String? = null,
    val haldLevel: Int = 0,
    val haldVariants: List<ColorCardTraceHaldVariant> = emptyList(),
    val mnnFilterAsset: MNNFilterAsset? = null,
    val pixelPosterPreset: ColorCardTracePixelPosterPreset? = null,
    val selectiveColorPreset: ColorCardTraceSelectiveColorPreset? = null
)

internal data class ColorCardTraceFilterState(
    val mode: ColorCardTracePanelMode,
    val hasUserEdits: Boolean,
    val hasSavedFilters: Boolean,
    val hasBuiltinFilters: Boolean,
    val hasTraceFilters: Boolean,
    val canSaveRenderedImage: Boolean,
    val isAnalyzing: Boolean,
    val isRendering: Boolean,
    val renderedPreviewBitmap: Bitmap?,
    val sourceColors: List<Int>,
    val displayColors: List<Int>,
    val imitateTransferProfile: ColorCardImitateTransferProfile?,
    val candidates: List<ColorCardTraceFilterCandidate>,
    val selectedCandidateIndex: Int,
    val selectedCandidate: ColorCardTraceFilterCandidate?,
    val isOriginalSelected: Boolean,
    val previewSourceBitmap: Bitmap?,
    val faceRegions: List<ColorCardTraceFaceRegion>,
    val subjectRect: ColorCardTraceSubjectRect?,
    val gpuParams: ColorCardTraceGpuParams,
    val portraitAvailable: Boolean,
    val portraitAdjustments: ColorCardTracePortraitAdjustments,
    val filmGrainEligible: Boolean,
    val filmGrainEnabled: Boolean,
    val filmGrainAssets: List<ColorCardTraceFilmGrainAsset>,
    val selectedFilmGrainAsset: ColorCardTraceFilmGrainAsset?,
    val selectedFilmGrainBitmap: Bitmap?,
    val motionPhotoRevealPattern: MotionPhotoRevealPattern = MotionPhotoRevealPattern.SinglePoint,
    val color: Float,
    val colorAxis: ColorCardTraceColorAxis,
    val sharpen: Float,
    val tone: Float,
    val saturation: Float,
    val brightness: Float,
    val paletteStrength: Float,
    val allowsManualAdjustment: Boolean,
    val allowsStrengthAdjustment: Boolean,
    val stepFilterBy: (Int) -> Unit,
    val selectOriginal: () -> Unit,
    val selectCandidate: (String) -> Unit,
    val updateAdjustment: (Float, Float) -> Unit,
    val updateSharpen: (Float) -> Unit,
    val updateSaturation: (Float) -> Unit,
    val updateBrightness: (Float) -> Unit,
    val updatePaletteStrength: (Float) -> Unit,
    val updatePortraitVitality: (Float) -> Unit,
    val updatePortraitSoftness: (Float) -> Unit,
    val updatePortraitDetailSuppression: (Float) -> Unit,
    val setFilmGrainEnabled: (Boolean) -> Unit,
    val selectFilmGrainAsset: (String) -> Unit,
    val refreshFilmGrainAsset: () -> Unit,
    val updateMotionPhotoRevealPattern: (MotionPhotoRevealPattern) -> Unit = {},
    val resetAdjustment: () -> Unit,
    val resetManualAdjustments: () -> Unit,
    val resetPortraitAdjustments: () -> Unit
)

internal data class ColorCardTraceCycleStep(
    val order: List<String>,
    val index: Int,
    val selectedId: String
)

internal data class ColorCardTraceLab(
    val l: Double,
    val a: Double,
    val b: Double
)

internal data class ColorCardTraceRgb(
    val red: Int,
    val green: Int,
    val blue: Int
)

private data class ColorCardTraceMappingProfile(
    val blendStrength: Double,
    val maxLightnessShift: Double,
    val maxChromaShift: Double
)

private data class ColorCardImitatePixelGuard(
    val lightnessTransferScale: Double,
    val chromaTransferScale: Double,
    val paletteMappingScale: Double
)

private data class ColorCardTraceAnchorMapping(
    val sourceAnchors: List<ColorCardTraceLab>,
    val targetAnchors: List<ColorCardTraceLab>,
    val sourceCentroid: ColorCardTraceLab,
    val targetCentroid: ColorCardTraceLab
)

internal data class ColorCardTraceGpuMapping(
    val anchorCount: Int,
    val sourceAnchors: FloatArray,
    val targetAnchors: FloatArray,
    val blendStrength: Float,
    val maxLightnessShift: Float,
    val maxChromaShift: Float
)

internal data class ColorCardTraceAdjustmentDetent(
    val normalizedX: Float,
    val normalizedY: Float,
    val xIndex: Int,
    val yIndex: Int
)

private data class ColorCardTraceVerticalDetent(
    val normalizedValue: Float,
    val index: Int
)

internal data class ColorCardTraceAdjustmentPadGeometry(
    val startX: Float,
    val startY: Float,
    val stepX: Float,
    val stepY: Float,
    val columns: Int,
    val rows: Int
)

internal fun isColorCardTracePSeriesCandidateId(candidateId: String): Boolean {
    return candidateId.startsWith("builtin_pixel:") ||
        candidateId.startsWith("builtin_hald:presetpro-")
}

internal fun isColorCardTraceAiMnnCandidateId(candidateId: String): Boolean {
    return candidateId.startsWith("ai_mnn:")
}

internal fun isColorCardTraceAiMnnCandidate(
    candidate: ColorCardTraceFilterCandidate?
): Boolean = candidate?.kind == ColorCardTraceFilterKind.AiMnn

internal fun isColorCardTraceBuiltinSwitcherExcludedCandidate(
    candidate: ColorCardTraceFilterCandidate?
): Boolean {
    return isColorCardTraceAiMnnCandidate(candidate) ||
        isColorCardTraceBlackWhiteEraCandidate(candidate) ||
        candidate?.kind == ColorCardTraceFilterKind.MotionPhotoEntry
}

internal fun resolveColorCardTraceBuiltinSwitcherCandidates(
    candidates: List<ColorCardTraceFilterCandidate>
): List<ColorCardTraceFilterCandidate> {
    return candidates
        .filterNot(::isColorCardTraceBuiltinSwitcherExcludedCandidate)
        .ifEmpty { candidates }
}

internal fun orderColorCardTraceCandidateIdsForDisplay(
    candidateIds: List<String>
): List<String> {
    val distinctIds = candidateIds.distinct()
    if (distinctIds.size <= 1) return distinctIds

    val pSeriesIds = distinctIds.filter(::isColorCardTracePSeriesCandidateId)
    if (pSeriesIds.isEmpty()) return distinctIds

    val regularIds = distinctIds.filterNot(::isColorCardTracePSeriesCandidateId)
    if (regularIds.isEmpty()) return distinctIds

    val leadRegularCount = (regularIds.size - pSeriesIds.size)
        .coerceIn(1, regularIds.size)
    val tailRegularIds = ArrayDeque(regularIds.drop(leadRegularCount))
    val tailPSeriesIds = ArrayDeque(pSeriesIds)

    return buildList(distinctIds.size) {
        addAll(regularIds.take(leadRegularCount))
        while (tailRegularIds.isNotEmpty() || tailPSeriesIds.isNotEmpty()) {
            tailRegularIds.removeFirstOrNull()?.let(::add)
            tailPSeriesIds.removeFirstOrNull()?.let(::add)
        }
    }
}

internal fun isColorCardTracePSeriesCandidate(
    candidate: ColorCardTraceFilterCandidate
): Boolean {
    return isColorCardTracePSeriesCandidateId(candidate.paletteId)
}

internal fun preferredColorCardTraceBuiltinCandidateId(
    candidateIds: List<String>
): String? {
    if (candidateIds.isEmpty()) return null
    return candidateIds.firstOrNull { candidateId ->
        !isColorCardTracePSeriesCandidateId(candidateId)
    } ?: candidateIds.first()
}

internal fun randomColorCardTraceBuiltinCandidateId(
    candidateIds: List<String>,
    seed: Long
): String? {
    val distinctIds = candidateIds.distinct()
    if (distinctIds.isEmpty()) return null
    val preferredIds = distinctIds.filterNot(::isColorCardTracePSeriesCandidateId)
        .ifEmpty { distinctIds }
    return preferredIds.shuffled(Random(seed)).firstOrNull()
}

internal fun resolveColorCardTraceFilterCreativeResponse(
    panelMode: ColorCardTracePanelMode,
    selectedCandidate: ColorCardTraceFilterCandidate?,
    color: Float,
    tone: Float
): Pair<Float, Float> {
    if (panelMode != ColorCardTracePanelMode.Filter || selectedCandidate == null) {
        return color.coerceIn(-1f, 1f) to tone.coerceIn(-1f, 1f)
    }
    return (
        color.coerceIn(-1f, 1f) * COLOR_CARD_TRACE_FILTER_COLOR_RESPONSE_SCALE
        ) to (
        tone.coerceIn(-1f, 1f) * COLOR_CARD_TRACE_FILTER_TONE_RESPONSE_SCALE
        )
}

internal fun resolveColorCardTraceEffectiveFaceRegions(
    panelMode: ColorCardTracePanelMode,
    selectedBuiltinCandidate: ColorCardTraceFilterCandidate?,
    detectedFaceRegions: List<ColorCardTraceFaceRegion>
): List<ColorCardTraceFaceRegion> {
    return if (
        panelMode == ColorCardTracePanelMode.Filter &&
        selectedBuiltinCandidate != null &&
        (
            isColorCardTracePSeriesCandidate(selectedBuiltinCandidate) ||
                isColorCardTraceBlackWhiteEraCandidate(selectedBuiltinCandidate)
            )
    ) {
        emptyList()
    } else {
        detectedFaceRegions
    }
}

internal fun orderColorCardTraceBuiltinCandidatesForDisplay(
    candidates: List<ColorCardTraceFilterCandidate>
): List<ColorCardTraceFilterCandidate> {
    if (candidates.size <= 1) return candidates

    val pSeriesCandidates = candidates.filter(::isColorCardTracePSeriesCandidate)
    val mSeriesCandidates = candidates.filter(::isColorCardTraceMSeriesCandidate)
    val regularCandidates = candidates.filterNot {
        isColorCardTracePSeriesCandidate(it) || isColorCardTraceMSeriesCandidate(it)
    }
    if (pSeriesCandidates.isEmpty() && mSeriesCandidates.isEmpty()) return candidates
    if (regularCandidates.isEmpty()) {
        return mSeriesCandidates + pSeriesCandidates
    }

    val leadRegularCount = (regularCandidates.size - mSeriesCandidates.size - pSeriesCandidates.size)
        .coerceIn(1, regularCandidates.size)
    val tailRegularCandidates = ArrayDeque(regularCandidates.drop(leadRegularCount))
    val tailMSeriesCandidates = ArrayDeque(mSeriesCandidates)
    val tailPSeriesCandidates = ArrayDeque(pSeriesCandidates)

    return buildList(candidates.size) {
        addAll(regularCandidates.take(leadRegularCount))
        while (
            tailRegularCandidates.isNotEmpty() ||
            tailMSeriesCandidates.isNotEmpty() ||
            tailPSeriesCandidates.isNotEmpty()
        ) {
            tailRegularCandidates.removeFirstOrNull()?.let(::add)
            tailMSeriesCandidates.removeFirstOrNull()?.let(::add)
            tailPSeriesCandidates.removeFirstOrNull()?.let(::add)
        }
    }
}

private fun isColorCardTraceMSeriesCandidate(
    candidate: ColorCardTraceFilterCandidate
): Boolean = candidate.kind == ColorCardTraceFilterKind.HaldClut &&
    candidate.title.trim().lastOrNull()?.uppercaseChar() == 'M'

private fun snapshotColorCardTraceBuiltinPreviewBitmapCache(
    candidateIds: Set<String>
): Map<String, Bitmap> = synchronized(colorCardTraceBuiltinPreviewBitmapCacheLock) {
    buildMap {
        candidateIds.forEach { paletteId ->
            colorCardTraceBuiltinPreviewBitmapCache[paletteId]
                ?.takeIf { bitmap -> !bitmap.isRecycled }
                ?.let { bitmap -> put(paletteId, bitmap) }
        }
    }
}

private fun cacheColorCardTraceBuiltinPreviewBitmaps(
    generated: Map<String, Bitmap>
) {
    if (generated.isEmpty()) return
    synchronized(colorCardTraceBuiltinPreviewBitmapCacheLock) {
        generated.forEach { (paletteId, bitmap) ->
            colorCardTraceBuiltinPreviewBitmapCache[paletteId]
                ?.takeIf { existing -> existing !== bitmap && !existing.isRecycled }
                ?.recycle()
            colorCardTraceBuiltinPreviewBitmapCache[paletteId] = bitmap
        }
    }
}

private fun recycleColorCardTraceBitmap(bitmap: Bitmap?) {
    bitmap?.takeIf { !it.isRecycled }?.recycle()
}

private fun copyColorCardTraceBitmap(bitmap: Bitmap?): Bitmap? {
    val source = bitmap?.takeIf { !it.isRecycled } ?: return null
    return runCatching {
        source.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull()
}

@Composable
internal fun rememberColorCardTraceFilterState(
    active: Boolean,
    panelMode: ColorCardTracePanelMode,
    sourceUri: Uri,
    sourceFile: File? = null,
    sourceSessionId: Long,
    sourceColors: List<Int>,
    savedPalettes: List<SavedColorCardPalette>,
    imitateReferenceColors: List<Int> = emptyList(),
    imitateReferenceLabel: String? = null,
    imitateTransferProfile: ColorCardImitateTransferProfile? = null,
    imitateAnalysis: ColorCardImitateAnalysis? = null,
    imitateReferenceLoading: Boolean = false,
    initialBuiltinPaletteId: String? = null
): ColorCardTraceFilterState {
    val context = LocalContext.current
    val normalizedInitialBuiltinPaletteId = remember(sourceSessionId, initialBuiltinPaletteId) {
        initialBuiltinPaletteId?.trim()?.takeIf { it.isNotEmpty() }
    }
    var shouldPrefetchFilterResources by remember(sourceSessionId) { mutableStateOf(false) }
    val normalizedSourceColors = remember(sourceColors) {
        sourceColors.take(10)
    }
    val normalizedImitateReferenceColors = remember(imitateReferenceColors, imitateAnalysis?.signature) {
        (imitateAnalysis?.paletteColors ?: imitateReferenceColors).take(10)
    }
    val resolvedImitateTransferProfile = imitateAnalysis?.transferProfile ?: imitateTransferProfile
    val imitateDefaultColor = imitateAnalysis?.suggestedColor ?: 0f
    val imitateDefaultSharpen = imitateAnalysis?.suggestedSharpen ?: 0f
    val imitateDefaultTone = imitateAnalysis?.suggestedTone ?: 0f
    val imitateDefaultSaturation = imitateAnalysis?.suggestedSaturation ?: 0f
    val imitateDefaultBrightness = imitateAnalysis?.suggestedBrightness ?: 0f
    val imitateDefaultStrength = imitateAnalysis?.suggestedStrength ?: 0.6f
    LaunchedEffect(active, panelMode, sourceSessionId, shouldPrefetchFilterResources) {
        if (!active || shouldPrefetchFilterResources) return@LaunchedEffect
        if (panelMode == ColorCardTracePanelMode.Filter) {
            shouldPrefetchFilterResources = true
            return@LaunchedEffect
        }
        delay(COLOR_CARD_TRACE_FILTER_RESOURCE_PREFETCH_DELAY_MS)
        if (active && !shouldPrefetchFilterResources) {
            shouldPrefetchFilterResources = true
        }
    }
    var builtinHaldFiltersLoaded by remember(sourceSessionId) { mutableStateOf(false) }
    val builtinHaldFilters by produceState(
        initialValue = emptyList<ColorCardBuiltinHaldFilterAsset>(),
        key1 = context,
        key2 = shouldPrefetchFilterResources
    ) {
        if (!shouldPrefetchFilterResources) {
            builtinHaldFiltersLoaded = false
            value = emptyList()
            return@produceState
        }
        builtinHaldFiltersLoaded = false
        value = loadColorCardBuiltinHaldFilters(context.applicationContext)
        builtinHaldFiltersLoaded = true
    }
    var aiMnnSeriesLoaded by remember(sourceSessionId) { mutableStateOf(false) }
    val aiMnnSeries by produceState(
        initialValue = MNNFilterSeries(id = "ai-mnn-series", title = "AI 调色", filters = emptyList()),
        key1 = context,
        key2 = shouldPrefetchFilterResources
    ) {
        if (!shouldPrefetchFilterResources) {
            aiMnnSeriesLoaded = false
            value = MNNFilterSeries(id = "ai-mnn-series", title = "AI 调色", filters = emptyList())
            return@produceState
        }
        aiMnnSeriesLoaded = false
        value = loadMNNFilterSeriesSafe(context.applicationContext)
        aiMnnSeriesLoaded = true
    }
    var filmGrainAssetsLoaded by remember(sourceSessionId) { mutableStateOf(false) }
    val filmGrainAssets by produceState(
        initialValue = emptyList<ColorCardTraceFilmGrainAsset>(),
        key1 = context,
        key2 = shouldPrefetchFilterResources
    ) {
        if (!shouldPrefetchFilterResources) {
            filmGrainAssetsLoaded = false
            value = emptyList()
            return@produceState
        }
        filmGrainAssetsLoaded = false
        value = loadColorCardTraceFilmGrainAssets(context.applicationContext)
        filmGrainAssetsLoaded = true
    }
    val aiMnnCandidates = remember(aiMnnSeries) {
        buildColorCardTraceAiMnnCandidates(series = aiMnnSeries)
    }
    val builtinCandidates = remember(builtinHaldFilters, aiMnnCandidates) {
        buildColorCardTraceBuiltinCandidates(
            builtinHaldFilters = builtinHaldFilters,
            aiMnnCandidates = aiMnnCandidates
        )
    }
    val hasSavedFilters = remember(savedPalettes) {
        savedPalettes.any { it.colors.isNotEmpty() }
    }
    val shouldComputeTraceCandidates = active && normalizedSourceColors.isNotEmpty()
    var traceCandidatesLoading by remember(sourceSessionId) {
        mutableStateOf(
            normalizedSourceColors.isNotEmpty() &&
                (panelMode == ColorCardTracePanelMode.Trace || panelMode == ColorCardTracePanelMode.Imitate)
        )
    }
    var traceCandidates by remember(sourceSessionId) {
        mutableStateOf(emptyList<ColorCardTraceFilterCandidate>())
    }
    LaunchedEffect(
        sourceSessionId,
        shouldComputeTraceCandidates,
        normalizedSourceColors,
        savedPalettes,
        panelMode
    ) {
        if (!shouldComputeTraceCandidates) {
            traceCandidatesLoading = false
            traceCandidates = emptyList()
            return@LaunchedEffect
        }
        if (
            panelMode != ColorCardTracePanelMode.Trace &&
            panelMode != ColorCardTracePanelMode.Imitate
        ) {
            delay(COLOR_CARD_TRACE_TRACE_CANDIDATE_PREFETCH_DELAY_MS)
        }
        traceCandidatesLoading = true
        val sourceColorsSnapshot = normalizedSourceColors.toList()
        val savedPalettesSnapshot = savedPalettes.toList()
        traceCandidates = withContext(Dispatchers.Default) {
            rankColorCardTraceFilterCandidates(
                sourceColors = sourceColorsSnapshot,
                savedPalettes = savedPalettesSnapshot
            )
        }
        traceCandidatesLoading = false
    }
    val filterResourcesLoading = shouldPrefetchFilterResources &&
        (!builtinHaldFiltersLoaded || !aiMnnSeriesLoaded || !filmGrainAssetsLoaded)
    val imitateCandidates = remember(
        normalizedImitateReferenceColors,
        imitateReferenceLabel
    ) {
        if (normalizedImitateReferenceColors.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ColorCardTraceFilterCandidate(
                    paletteId = "imitate:reference",
                    kind = ColorCardTraceFilterKind.Palette,
                    title = imitateReferenceLabel?.trim().takeUnless { it.isNullOrBlank() } ?: "参考图风格",
                    sourceTag = "仿色",
                    colors = normalizedImitateReferenceColors,
                    score = -1.0
                )
            )
        }
    }
    var selectedBuiltinFilterId by remember(sourceSessionId) {
        mutableStateOf(normalizedInitialBuiltinPaletteId)
    }
    var selectedTraceFilterId by remember(sourceSessionId) { mutableStateOf<String?>(null) }
    var builtinCycleOrder by remember(sourceSessionId) { mutableStateOf(emptyList<String>()) }
    var builtinCycleIndex by remember(sourceSessionId) { mutableStateOf(-1) }
    var builtinSelectionExplicit by remember(sourceSessionId) {
        mutableStateOf(normalizedInitialBuiltinPaletteId != null)
    }
    var traceCycleOrder by remember(sourceSessionId) { mutableStateOf(emptyList<String>()) }
    var traceCycleIndex by remember(sourceSessionId) { mutableStateOf(-1) }
    var filterColor by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var filterSharpen by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var filterTone by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var filterSaturation by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var filterBrightness by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var filterStrength by remember(sourceSessionId) { mutableFloatStateOf(0.5f) }
    var filterMotionPhotoRevealPattern by remember(sourceSessionId) {
        mutableStateOf(MotionPhotoRevealPattern.SinglePoint)
    }
    var traceColor by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var traceSharpen by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var traceTone by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var traceSaturation by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var traceBrightness by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var traceStrength by remember(sourceSessionId) { mutableFloatStateOf(0.5f) }
    var imitateColor by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var imitateSharpen by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var imitateTone by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var imitateSaturation by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var imitateBrightness by remember(sourceSessionId) { mutableFloatStateOf(0f) }
    var imitateStrength by remember(sourceSessionId) { mutableFloatStateOf(0.6f) }
    var appliedImitateAnalysisSignature by remember(sourceSessionId) { mutableStateOf<String?>(null) }
    var portraitVitality by remember(sourceSessionId) {
        mutableFloatStateOf(COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE)
    }
    var portraitSoftness by remember(sourceSessionId) {
        mutableFloatStateOf(COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE)
    }
    var portraitDetailSuppression by remember(sourceSessionId) {
        mutableFloatStateOf(COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE)
    }
    var filmGrainEnabled by remember(sourceSessionId) { mutableStateOf(false) }
    var selectedFilmGrainId by remember(sourceSessionId) { mutableStateOf<String?>(null) }
    var remainingFilmGrainQueue by remember(sourceSessionId) { mutableStateOf(emptyList<String>()) }
    var hasUserEdits by remember(sourceSessionId) { mutableStateOf(false) }
    var previewSourceBitmap by remember(sourceSessionId) { mutableStateOf<Bitmap?>(null) }
    var aiBasePreviewBitmap by remember(sourceSessionId) { mutableStateOf<Bitmap?>(null) }
    var renderedPreviewBitmap by remember(sourceSessionId) { mutableStateOf<Bitmap?>(null) }
    var deferredPreviewRecycleBitmaps by remember { mutableStateOf(emptyList<Bitmap>()) }
    var protectedFaceRegions by remember(sourceSessionId) {
        mutableStateOf(emptyList<ColorCardTraceFaceRegion>())
    }
    val haldBitmapCache = remember(sourceSessionId) { mutableStateMapOf<String, Bitmap>() }
    val filmGrainBitmapCache = remember(sourceSessionId) { mutableStateMapOf<String, Bitmap>() }
    val aiPreviewBitmapCache = remember(sourceSessionId) { mutableStateMapOf<String, Bitmap>() }
    val mnnFilterEngine = remember(context.applicationContext) {
        MNNFilterEngine(context.applicationContext)
    }
    fun deferPreviewBitmapRecycle(bitmap: Bitmap?, protected: Set<Bitmap?> = emptySet()) {
        val candidate = bitmap?.takeIf { !it.isRecycled }
        deferredPreviewRecycleBitmaps
            .filter { deferred -> protected.none { it === deferred } }
            .forEach(::recycleColorCardTraceBitmap)
        deferredPreviewRecycleBitmaps = candidate
            ?.takeIf { protected.none { protectedBitmap -> protectedBitmap === it } }
            ?.let(::listOf)
            ?: emptyList()
    }
    fun deferPreviewBitmapRecycleBatch(bitmaps: Iterable<Bitmap?>, protected: Set<Bitmap?> = emptySet()) {
        deferredPreviewRecycleBitmaps
            .filter { deferred -> protected.none { it === deferred } }
            .forEach(::recycleColorCardTraceBitmap)
        deferredPreviewRecycleBitmaps = bitmaps
            .filterNotNull()
            .filter { bitmap ->
                !bitmap.isRecycled && protected.none { protectedBitmap -> protectedBitmap === bitmap }
            }
            .distinctBy(System::identityHashCode)
    }
    DisposableEffect(Unit) {
        onDispose {
            deferredPreviewRecycleBitmaps.forEach(::recycleColorCardTraceBitmap)
        }
    }

    LaunchedEffect(filmGrainAssets) {
        if (filmGrainAssets.isEmpty()) {
            selectedFilmGrainId = null
            remainingFilmGrainQueue = emptyList()
            return@LaunchedEffect
        }
        val validIds = filmGrainAssets.map(ColorCardTraceFilmGrainAsset::id).toSet()
        val resolvedSelectedId = selectedFilmGrainId?.takeIf(validIds::contains) ?: filmGrainAssets.first().id
        selectedFilmGrainId = resolvedSelectedId
        remainingFilmGrainQueue = remainingFilmGrainQueue.filter(validIds::contains).ifEmpty {
            filmGrainAssets
                .map(ColorCardTraceFilmGrainAsset::id)
                .filterNot { it == resolvedSelectedId }
                .shuffled(Random(sourceSessionId xor resolvedSelectedId.hashCode().toLong()))
        }
    }

    LaunchedEffect(active, panelMode, sourceSessionId, sourceUri) {
        if (!active) return@LaunchedEffect
        if (previewSourceBitmap != null) return@LaunchedEffect
        if (panelMode == ColorCardTracePanelMode.Picker) {
            delay(COLOR_CARD_TRACE_PREVIEW_WARMUP_DELAY_MS)
            if (!active || previewSourceBitmap != null) return@LaunchedEffect
        }

        previewSourceBitmap = decodeColorCardTracePreviewBitmap(
            context = context.applicationContext,
            uri = sourceUri,
            maxSide = COLOR_CARD_TRACE_FILTER_GPU_MAX_SIDE
        )
    }
    LaunchedEffect(active, sourceSessionId, previewSourceBitmap) {
        if (!active) return@LaunchedEffect
        val bitmap = previewSourceBitmap ?: return@LaunchedEffect
        protectedFaceRegions = withContext(Dispatchers.Default) {
            val detectionBitmap = buildColorCardTraceFaceDetectionBitmap(bitmap)
            try {
                ColorCardTraceSkinProtectionDetector.detectFaceRegions(detectionBitmap)
            } finally {
                if (detectionBitmap !== bitmap && !detectionBitmap.isRecycled) {
                    detectionBitmap.recycle()
                }
            }
        }
    }
    LaunchedEffect(sourceSessionId, imitateAnalysis?.signature) {
        val analysis = imitateAnalysis
        if (analysis == null) {
            appliedImitateAnalysisSignature = null
            return@LaunchedEffect
        }
        if (appliedImitateAnalysisSignature == analysis.signature) {
            return@LaunchedEffect
        }
        imitateColor = imitateDefaultColor
        imitateSharpen = imitateDefaultSharpen
        imitateTone = imitateDefaultTone
        imitateSaturation = imitateDefaultSaturation
        imitateBrightness = imitateDefaultBrightness
        imitateStrength = imitateDefaultStrength
        appliedImitateAnalysisSignature = analysis.signature
    }

    LaunchedEffect(
        builtinCandidates,
        panelMode,
        filterResourcesLoading,
        normalizedInitialBuiltinPaletteId
    ) {
        if (
            panelMode == ColorCardTracePanelMode.Filter &&
            filterResourcesLoading &&
            !builtinSelectionExplicit &&
            normalizedInitialBuiltinPaletteId == null
        ) {
            selectedBuiltinFilterId = null
            builtinCycleOrder = emptyList()
            builtinCycleIndex = -1
            return@LaunchedEffect
        }
        val candidateIds = builtinCandidates.map(ColorCardTraceFilterCandidate::paletteId)
        if (candidateIds.isEmpty()) {
            if (normalizedInitialBuiltinPaletteId == null) {
                selectedBuiltinFilterId = null
                builtinCycleOrder = emptyList()
                builtinCycleIndex = -1
                builtinSelectionExplicit = false
            }
            return@LaunchedEffect
        }
        val requestedInitialId = normalizedInitialBuiltinPaletteId?.takeIf(candidateIds::contains)
        val shouldHonorInitialSelection = panelMode == ColorCardTracePanelMode.Filter &&
            requestedInitialId != null &&
            (
                !builtinSelectionExplicit ||
                    selectedBuiltinFilterId == null ||
                    selectedBuiltinFilterId != requestedInitialId
                )
        val shouldAutoRandomize = panelMode == ColorCardTracePanelMode.Filter &&
            !builtinSelectionExplicit &&
            requestedInitialId == null
        val autoSeed = Random.nextLong()
        val anchorId = when {
            shouldHonorInitialSelection -> requestedInitialId
            shouldAutoRandomize -> {
                randomColorCardTraceBuiltinCandidateId(candidateIds, autoSeed) ?: candidateIds.first()
            }
            else -> {
                selectedBuiltinFilterId
                    ?.takeIf(candidateIds::contains)
                    ?: requestedInitialId
                    ?: randomColorCardTraceBuiltinCandidateId(candidateIds, autoSeed)
                    ?: candidateIds.first()
            }
        }
        val cycleOrder = if (
            !shouldAutoRandomize &&
            builtinCycleOrder.size == candidateIds.size &&
            builtinCycleOrder.toSet() == candidateIds.toSet() &&
            anchorId in builtinCycleOrder
        ) {
            builtinCycleOrder
        } else {
            buildColorCardTraceCycleOrder(
                candidateIds = candidateIds,
                anchorId = anchorId,
                seed = Random.nextLong()
            )
        }
        builtinCycleOrder = cycleOrder
        builtinCycleIndex = cycleOrder.indexOf(anchorId).coerceAtLeast(0)
        selectedBuiltinFilterId = anchorId
        if (shouldHonorInitialSelection) {
            val requestedCandidate = builtinCandidates.firstOrNull { it.paletteId == anchorId }
            filterStrength = defaultColorCardTracePaletteStrength(requestedCandidate)
            builtinSelectionExplicit = true
        }
    }
    LaunchedEffect(traceCandidates) {
        val candidateIds = traceCandidates.map(ColorCardTraceFilterCandidate::paletteId)
        if (candidateIds.isEmpty()) {
            selectedTraceFilterId = null
            traceCycleOrder = emptyList()
            traceCycleIndex = -1
            return@LaunchedEffect
        }
        val anchorId = selectedTraceFilterId
            ?.takeIf(candidateIds::contains)
            ?: candidateIds.first()
        val cycleOrder = if (
            traceCycleOrder.size == candidateIds.size &&
            traceCycleOrder.toSet() == candidateIds.toSet() &&
            anchorId in traceCycleOrder
        ) {
            traceCycleOrder
        } else {
            buildColorCardTraceCycleOrder(
                candidateIds = candidateIds,
                anchorId = anchorId,
                seed = Random.nextLong()
            )
        }
        traceCycleOrder = cycleOrder
        traceCycleIndex = cycleOrder.indexOf(anchorId).coerceAtLeast(0)
        selectedTraceFilterId = anchorId
    }

    val selectedBuiltinIndex = remember(builtinCandidates, selectedBuiltinFilterId) {
        builtinCandidates.indexOfFirst { it.paletteId == selectedBuiltinFilterId }
            .takeIf { it >= 0 }
            ?: -1
    }
    val selectedBuiltinCandidate = remember(builtinCandidates, selectedBuiltinIndex) {
        selectedBuiltinIndex
            .takeIf { it >= 0 }
            ?.let(builtinCandidates::getOrNull)
    }
    val selectedTraceIndex = remember(traceCandidates, selectedTraceFilterId) {
        traceCandidates.indexOfFirst { it.paletteId == selectedTraceFilterId }
            .takeIf { it >= 0 }
            ?: -1
    }
    val selectedTraceCandidate = remember(traceCandidates, selectedTraceIndex) {
        selectedTraceIndex
            .takeIf { it >= 0 }
            ?.let(traceCandidates::getOrNull)
    }
    val selectedImitateCandidate = remember(imitateCandidates) {
        imitateCandidates.firstOrNull()
    }
    DisposableEffect(sourceSessionId) {
        onDispose {
            deferPreviewBitmapRecycleBatch(
                bitmaps = listOf(previewSourceBitmap, aiBasePreviewBitmap, renderedPreviewBitmap),
                protected = emptySet()
            )
            haldBitmapCache.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            haldBitmapCache.clear()
            filmGrainBitmapCache.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            filmGrainBitmapCache.clear()
            aiPreviewBitmapCache.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            aiPreviewBitmapCache.clear()
        }
    }
    DisposableEffect(mnnFilterEngine) {
        onDispose {
            mnnFilterEngine.close()
        }
    }
    val resolvedSelectedHaldBlend = remember(selectedBuiltinCandidate?.paletteId, filterStrength) {
        resolveColorCardTraceHaldBlend(
            candidate = selectedBuiltinCandidate,
            paletteStrength = filterStrength
        )
    }
    LaunchedEffect(
        active,
        resolvedSelectedHaldBlend.primaryAssetName,
        resolvedSelectedHaldBlend.secondaryAssetName
    ) {
        if (!active) return@LaunchedEffect
        val requiredAssetNames = listOfNotNull(
            resolvedSelectedHaldBlend.primaryAssetName,
            resolvedSelectedHaldBlend.secondaryAssetName
        )
        requiredAssetNames.forEach { assetName ->
            val cachedBitmap = haldBitmapCache[assetName]
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                return@forEach
            }
            decodeColorCardBuiltinHaldBitmap(
                context = context.applicationContext,
                assetName = assetName
            )?.let { decoded ->
                haldBitmapCache[assetName]?.takeIf { it !== decoded && !it.isRecycled }?.recycle()
                haldBitmapCache[assetName] = decoded
            }
        }
    }
    val matchedPrimaryHaldBitmap = resolvedSelectedHaldBlend.primaryAssetName
        ?.let(haldBitmapCache::get)
        ?.takeIf { !it.isRecycled }
    val matchedSecondaryHaldBitmap = resolvedSelectedHaldBlend.secondaryAssetName
        ?.let(haldBitmapCache::get)
        ?.takeIf { !it.isRecycled }
    val selectedFilmGrainAsset = remember(filmGrainAssets, selectedFilmGrainId) {
        filmGrainAssets.firstOrNull { it.id == selectedFilmGrainId } ?: filmGrainAssets.firstOrNull()
    }
    val buildFilmGrainRefreshQueue: (String?) -> List<String> = { currentId ->
        filmGrainAssets
            .map(ColorCardTraceFilmGrainAsset::id)
            .filterNot { it == currentId }
            .shuffled(Random(System.nanoTime()))
    }
    val selectFilmGrainIdAndResetQueue: (String) -> Unit = { assetId ->
        if (filmGrainAssets.any { it.id == assetId }) {
            selectedFilmGrainId = assetId
            remainingFilmGrainQueue = buildFilmGrainRefreshQueue(assetId)
        }
    }
    val refreshFilmGrainSelection: () -> Unit = {
        if (filmGrainAssets.isEmpty()) {
            Unit
        } else {
            val currentId = selectedFilmGrainId
            val nextQueue = remainingFilmGrainQueue.ifEmpty {
                buildFilmGrainRefreshQueue(currentId)
            }
            val nextId = nextQueue.firstOrNull() ?: filmGrainAssets.firstOrNull()?.id
            if (nextId != null) {
                selectedFilmGrainId = nextId
                remainingFilmGrainQueue = nextQueue.drop(1)
            }
        }
    }
    val filmGrainEligible = remember(panelMode, selectedBuiltinCandidate?.paletteId, selectedBuiltinCandidate?.title) {
        panelMode == ColorCardTracePanelMode.Filter && isColorCardTraceFilmGrainEligible(selectedBuiltinCandidate)
    }
    LaunchedEffect(active, filmGrainEligible, filmGrainEnabled, selectedFilmGrainAsset?.assetName) {
        if (!active || !filmGrainEligible || !filmGrainEnabled) return@LaunchedEffect
        val asset = selectedFilmGrainAsset ?: return@LaunchedEffect
        val cachedBitmap = filmGrainBitmapCache[asset.assetName]
        if (cachedBitmap != null && !cachedBitmap.isRecycled) return@LaunchedEffect
        decodeColorCardTraceFilmGrainBitmap(
            context = context.applicationContext,
            assetName = asset.assetName
        )?.let { decoded ->
            filmGrainBitmapCache[asset.assetName]?.takeIf { it !== decoded && !it.isRecycled }?.recycle()
            filmGrainBitmapCache[asset.assetName] = decoded
        }
    }
    val selectedFilmGrainBitmap = selectedFilmGrainAsset
        ?.let { asset -> filmGrainBitmapCache[asset.assetName] }
        ?.takeIf { !it.isRecycled }
    LaunchedEffect(
        active,
        panelMode,
        previewSourceBitmap,
        selectedBuiltinCandidate?.paletteId
    ) {
        val bitmap = previewSourceBitmap
        val candidate = selectedBuiltinCandidate
        if (
            !active ||
            panelMode != ColorCardTracePanelMode.Filter ||
            bitmap == null ||
            bitmap.isRecycled ||
            candidate?.kind != ColorCardTraceFilterKind.AiMnn
        ) {
            aiBasePreviewBitmap = null
            return@LaunchedEffect
        }

        delay(COLOR_CARD_TRACE_AI_PREVIEW_DEBOUNCE_MS)
        val previewFilterAsset = candidate.mnnFilterAsset?.copy(
            threadCount = COLOR_CARD_TRACE_AI_PREVIEW_THREAD_COUNT
        ) ?: run {
            aiBasePreviewBitmap = null
            return@LaunchedEffect
        }
        val cachedRendered = aiPreviewBitmapCache[candidate.paletteId]?.takeIf { !it.isRecycled }
        if (cachedRendered != null) {
            aiBasePreviewBitmap = cachedRendered
            return@LaunchedEffect
        }
        suspend fun renderAiPreviewBitmap(maxSide: Int): Bitmap? {
            val previewBitmap = withContext(Dispatchers.Default) {
                scaleColorCardTraceBitmapForAiPreview(bitmap, maxSide)
            }
            return try {
                runCatching {
                    mnnFilterEngine.render(previewBitmap, previewFilterAsset)
                }.onFailure { throwable ->
                    Log.e(
                        COLOR_CARD_TRACE_TAG,
                        "AI preview render failed for ${candidate.paletteId}, maxSide=$maxSide",
                        throwable
                    )
                }.getOrNull()
            } finally {
                if (previewBitmap !== bitmap && !previewBitmap.isRecycled) {
                    previewBitmap.recycle()
                }
            }
        }

        val quickRendered = renderAiPreviewBitmap(COLOR_CARD_TRACE_AI_QUICK_PREVIEW_MAX_SIDE)
        if (quickRendered != null) {
            aiBasePreviewBitmap?.let { existing ->
                if (
                    existing !== quickRendered &&
                    !isColorCardTraceCachedPreviewBitmap(aiPreviewBitmapCache, existing) &&
                    !existing.isRecycled
                ) {
                    deferPreviewBitmapRecycle(existing, setOf(renderedPreviewBitmap))
                }
            }
            aiBasePreviewBitmap = quickRendered
        }

        val rendered = runCatching {
            renderAiPreviewBitmap(COLOR_CARD_TRACE_AI_PREVIEW_MAX_SIDE)
        }.onFailure { throwable ->
                Log.e(
                    COLOR_CARD_TRACE_TAG,
                    "AI refined preview render failed for ${candidate.paletteId}",
                    throwable
                )
            }.getOrNull()

        rendered?.let { freshBitmap ->
            aiPreviewBitmapCache[candidate.paletteId]?.takeIf { it !== freshBitmap && !it.isRecycled }?.recycle()
            aiPreviewBitmapCache[candidate.paletteId] = freshBitmap
        }
        aiBasePreviewBitmap?.let { existing ->
            if (
                existing !== rendered &&
                !isColorCardTraceCachedPreviewBitmap(aiPreviewBitmapCache, existing) &&
                !existing.isRecycled
            ) {
                deferPreviewBitmapRecycle(existing, setOf(renderedPreviewBitmap))
            }
        }
        aiBasePreviewBitmap = rendered
    }
    LaunchedEffect(
        active,
        panelMode,
        selectedBuiltinCandidate?.kind
    ) {
        val aiPreviewActive = active &&
            panelMode == ColorCardTracePanelMode.Filter &&
            selectedBuiltinCandidate?.kind == ColorCardTraceFilterKind.AiMnn
        if (aiPreviewActive) {
            return@LaunchedEffect
        }

        runCatching {
            mnnFilterEngine.switchFilter(null)
        }.onFailure { throwable ->
            Log.w(
                COLOR_CARD_TRACE_TAG,
                "Failed to release AI filter runtime after leaving AI preview",
                throwable
            )
        }

        val protectedBitmaps = setOf(renderedPreviewBitmap)
        val staleAiBitmaps = buildList {
            add(aiBasePreviewBitmap)
            addAll(aiPreviewBitmapCache.values)
        }.distinctBy { bitmap ->
            bitmap?.let(System::identityHashCode) ?: 0
        }
        aiBasePreviewBitmap = null
        aiPreviewBitmapCache.clear()
        deferPreviewBitmapRecycleBatch(
            bitmaps = staleAiBitmaps,
            protected = protectedBitmaps
        )
    }

    LaunchedEffect(
        active,
        panelMode,
        aiBasePreviewBitmap,
        selectedBuiltinCandidate?.paletteId,
        selectedFilmGrainAsset?.assetName,
        selectedFilmGrainBitmap,
        filmGrainEnabled,
        filmGrainEligible
    ) {
        val baseBitmap = aiBasePreviewBitmap
        val candidate = selectedBuiltinCandidate
        if (
            !active ||
            panelMode != ColorCardTracePanelMode.Filter ||
            baseBitmap == null ||
            baseBitmap.isRecycled ||
            candidate?.kind != ColorCardTraceFilterKind.AiMnn
        ) {
            if (
                active &&
                panelMode == ColorCardTracePanelMode.Filter &&
                isColorCardTraceBeadsPixelCandidate(candidate)
            ) {
                return@LaunchedEffect
            }
            val staleRendered = renderedPreviewBitmap
            // Reveal the next GPU preview immediately when leaving AI mode.
            if (renderedPreviewBitmap === staleRendered) {
                renderedPreviewBitmap = null
            }
            staleRendered?.let { existing ->
                if (existing !== aiBasePreviewBitmap && !existing.isRecycled) {
                    deferPreviewBitmapRecycle(existing, setOf(aiBasePreviewBitmap))
                }
            }
            return@LaunchedEffect
        }

        val finalBitmap = if (filmGrainEligible && filmGrainEnabled) {
            val cachedGrainBitmap = selectedFilmGrainBitmap
            val resolvedGrainBitmap = cachedGrainBitmap ?: selectedFilmGrainAsset?.let { asset ->
                decodeColorCardTraceFilmGrainBitmap(
                    context = context.applicationContext,
                    assetName = asset.assetName
                )
            }
            if (resolvedGrainBitmap != null) {
                try {
                    withContext(Dispatchers.Default) {
                        applyColorCardTraceFilmGrainOverlay(
                            sourceBitmap = baseBitmap,
                            grainBitmap = resolvedGrainBitmap
                        )
                    }
                } finally {
                    if (resolvedGrainBitmap !== cachedGrainBitmap && !resolvedGrainBitmap.isRecycled) {
                        resolvedGrainBitmap.recycle()
                    }
                }
            } else {
                baseBitmap
            }
        } else {
            baseBitmap
        }

        renderedPreviewBitmap?.let { existing ->
            if (existing !== finalBitmap && existing !== aiBasePreviewBitmap && !existing.isRecycled) {
                deferPreviewBitmapRecycle(existing, setOf(aiBasePreviewBitmap, finalBitmap))
            }
        }
        renderedPreviewBitmap = finalBitmap
    }

    val activeCandidates = remember(panelMode, builtinCandidates, traceCandidates) {
        when (panelMode) {
            ColorCardTracePanelMode.Filter -> builtinCandidates
            ColorCardTracePanelMode.Trace -> traceCandidates
            ColorCardTracePanelMode.Imitate -> imitateCandidates
            ColorCardTracePanelMode.Picker -> emptyList()
            ColorCardTracePanelMode.Recipe -> emptyList()
        }
    }
    val selectedCandidateIndex = when (panelMode) {
        ColorCardTracePanelMode.Filter -> selectedBuiltinIndex
        ColorCardTracePanelMode.Trace -> selectedTraceIndex
        ColorCardTracePanelMode.Imitate -> selectedImitateCandidate?.let { 0 } ?: -1
        ColorCardTracePanelMode.Picker -> -1
        ColorCardTracePanelMode.Recipe -> -1
    }
    val selectedCandidate = when (panelMode) {
        ColorCardTracePanelMode.Filter -> selectedBuiltinCandidate
        ColorCardTracePanelMode.Trace -> selectedTraceCandidate
        ColorCardTracePanelMode.Imitate -> selectedImitateCandidate
        ColorCardTracePanelMode.Picker -> null
        ColorCardTracePanelMode.Recipe -> null
    }
    val sharpen = when (panelMode) {
        ColorCardTracePanelMode.Filter -> filterSharpen
        ColorCardTracePanelMode.Trace -> traceSharpen
        ColorCardTracePanelMode.Imitate -> imitateSharpen
        ColorCardTracePanelMode.Picker -> 0f
        ColorCardTracePanelMode.Recipe -> 0f
    }
    val tone = when (panelMode) {
        ColorCardTracePanelMode.Filter -> filterTone
        ColorCardTracePanelMode.Trace -> traceTone
        ColorCardTracePanelMode.Imitate -> imitateTone
        ColorCardTracePanelMode.Picker -> 0f
        ColorCardTracePanelMode.Recipe -> 0f
    }
    val color = when (panelMode) {
        ColorCardTracePanelMode.Filter -> filterColor
        ColorCardTracePanelMode.Trace -> traceColor
        ColorCardTracePanelMode.Imitate -> imitateColor
        ColorCardTracePanelMode.Picker -> 0f
        ColorCardTracePanelMode.Recipe -> 0f
    }
    val paletteStrength = when {
        panelMode == ColorCardTracePanelMode.Filter &&
            selectedBuiltinCandidate?.kind == ColorCardTraceFilterKind.PixelPoster -> {
            COLOR_CARD_TRACE_PIXEL_POSTER_FIXED_STRENGTH
        }
        else -> when (panelMode) {
            ColorCardTracePanelMode.Filter -> filterStrength
            ColorCardTracePanelMode.Trace -> traceStrength
            ColorCardTracePanelMode.Imitate -> imitateStrength
            ColorCardTracePanelMode.Picker -> 0f
            ColorCardTracePanelMode.Recipe -> 0f
        }
    }
    val allowsManualAdjustment = when (panelMode) {
        ColorCardTracePanelMode.Filter -> {
            !isColorCardTraceAiMnnCandidate(selectedBuiltinCandidate) &&
                selectedBuiltinCandidate?.kind != ColorCardTraceFilterKind.MotionPhotoEntry
        }
        ColorCardTracePanelMode.Trace -> true
        ColorCardTracePanelMode.Imitate -> true
        ColorCardTracePanelMode.Picker -> false
        ColorCardTracePanelMode.Recipe -> false
    }
    val allowsStrengthAdjustment = when (panelMode) {
        ColorCardTracePanelMode.Filter -> {
            !isColorCardTraceAiMnnCandidate(selectedBuiltinCandidate) &&
                selectedBuiltinCandidate?.kind != ColorCardTraceFilterKind.PixelPoster &&
                selectedBuiltinCandidate?.kind != ColorCardTraceFilterKind.MotionPhotoEntry
        }
        ColorCardTracePanelMode.Trace -> true
        ColorCardTracePanelMode.Imitate -> true
        ColorCardTracePanelMode.Picker -> false
        ColorCardTracePanelMode.Recipe -> false
    }
    val resolvedCreativeColor = when (panelMode) {
        ColorCardTracePanelMode.Filter -> filterColor
        ColorCardTracePanelMode.Trace -> traceColor
        ColorCardTracePanelMode.Imitate -> imitateColor
        ColorCardTracePanelMode.Picker -> 0f
        ColorCardTracePanelMode.Recipe -> 0f
    }
    val (effectiveCreativeColor, effectiveCreativeTone) = remember(
        panelMode,
        selectedBuiltinCandidate?.paletteId,
        selectedTraceCandidate?.paletteId,
        resolvedCreativeColor,
        tone
    ) {
        resolveColorCardTraceFilterCreativeResponse(
            panelMode = panelMode,
            selectedCandidate = when (panelMode) {
                ColorCardTracePanelMode.Filter -> selectedBuiltinCandidate
                ColorCardTracePanelMode.Trace -> selectedTraceCandidate
                ColorCardTracePanelMode.Imitate -> selectedImitateCandidate
                ColorCardTracePanelMode.Picker -> null
                ColorCardTracePanelMode.Recipe -> null
            },
            color = resolvedCreativeColor,
            tone = tone
        )
    }
    val effectiveFaceRegions = remember(panelMode, selectedBuiltinCandidate?.paletteId, protectedFaceRegions) {
        resolveColorCardTraceEffectiveFaceRegions(
            panelMode = panelMode,
            selectedBuiltinCandidate = selectedBuiltinCandidate,
            detectedFaceRegions = protectedFaceRegions
        )
    }
    val portraitAdjustments = remember(
        portraitVitality,
        portraitSoftness,
        portraitDetailSuppression
    ) {
        ColorCardTracePortraitAdjustments(
            vitality = portraitVitality,
            softness = portraitSoftness,
            detailSuppression = portraitDetailSuppression
        )
    }
    val portraitAvailable = remember(
        panelMode,
        selectedBuiltinCandidate?.paletteId,
        selectedTraceCandidate?.paletteId,
        effectiveFaceRegions
    ) {
        resolveColorCardTracePortraitFeatureAvailable(
            panelMode = panelMode,
            selectedCandidate = when (panelMode) {
                ColorCardTracePanelMode.Filter -> selectedBuiltinCandidate
                ColorCardTracePanelMode.Trace -> selectedTraceCandidate
                ColorCardTracePanelMode.Imitate -> selectedImitateCandidate
                ColorCardTracePanelMode.Picker,
                ColorCardTracePanelMode.Recipe -> null
            },
            faceRegions = effectiveFaceRegions
        )
    }
    val basePortraitParams = remember(
        panelMode,
        selectedBuiltinCandidate?.paletteId,
        selectedTraceCandidate?.paletteId,
        selectedImitateCandidate?.paletteId,
        effectiveFaceRegions
    ) {
        resolveColorCardPortraitOptimizationParams(
            panelMode = when (panelMode) {
                ColorCardTracePanelMode.Filter -> ColorCardTracePanelMode.Filter
                ColorCardTracePanelMode.Trace -> ColorCardTracePanelMode.Trace
                ColorCardTracePanelMode.Imitate -> ColorCardTracePanelMode.Trace
                ColorCardTracePanelMode.Picker,
                ColorCardTracePanelMode.Recipe -> ColorCardTracePanelMode.Picker
            },
            selectedCandidate = when (panelMode) {
                ColorCardTracePanelMode.Filter -> selectedBuiltinCandidate
                ColorCardTracePanelMode.Trace -> selectedTraceCandidate
                ColorCardTracePanelMode.Imitate -> selectedImitateCandidate
                ColorCardTracePanelMode.Picker,
                ColorCardTracePanelMode.Recipe -> null
            },
            faceRegions = effectiveFaceRegions
        )
    }
    val portraitParams = remember(
        portraitAvailable,
        basePortraitParams,
        effectiveFaceRegions,
        portraitAdjustments
    ) {
        resolveColorCardTracePortraitFeatureParams(
            enabled = portraitAvailable,
            faceRegions = effectiveFaceRegions,
            baseParams = basePortraitParams,
            adjustments = portraitAdjustments
        )
    }
    val colorAxis = remember(
        panelMode,
        normalizedSourceColors,
        selectedBuiltinCandidate?.paletteId,
        selectedBuiltinCandidate?.colors,
        selectedTraceCandidate?.paletteId,
        selectedTraceCandidate?.colors,
        selectedImitateCandidate?.paletteId,
        selectedImitateCandidate?.colors
    ) {
        resolveColorCardTraceColorAxis(
            sourceColors = normalizedSourceColors,
            targetColors = when (panelMode) {
                ColorCardTracePanelMode.Filter -> selectedBuiltinCandidate?.colors.orEmpty()
                ColorCardTracePanelMode.Trace -> selectedTraceCandidate?.colors.orEmpty()
                ColorCardTracePanelMode.Imitate -> selectedImitateCandidate?.colors.orEmpty()
                ColorCardTracePanelMode.Picker -> normalizedSourceColors
                ColorCardTracePanelMode.Recipe -> normalizedSourceColors
            }
        )
    }
    val subjectRect = remember(sourceSessionId, previewSourceBitmap, effectiveFaceRegions) {
        resolveColorCardTraceSubjectRect(
            sourceSessionId = sourceSessionId,
            sourceAspect = previewSourceBitmap?.let { bitmap ->
                bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat()
            } ?: 1f,
            faceRegions = effectiveFaceRegions
        )
    }
    val gpuParams = remember(
        panelMode,
        normalizedSourceColors,
        selectedBuiltinCandidate?.paletteId,
        selectedTraceCandidate?.paletteId,
        selectedImitateCandidate?.paletteId,
        resolvedSelectedHaldBlend.primaryAssetName,
        resolvedSelectedHaldBlend.secondaryAssetName,
        resolvedSelectedHaldBlend.variantMix,
        matchedPrimaryHaldBitmap,
        matchedSecondaryHaldBitmap,
        effectiveCreativeColor,
        filterSharpen,
        effectiveCreativeTone,
        filterSaturation,
        filterBrightness,
        filterStrength,
        if (panelMode == ColorCardTracePanelMode.Trace) traceColor else effectiveCreativeColor,
        traceSharpen,
        if (panelMode == ColorCardTracePanelMode.Trace) traceTone else effectiveCreativeTone,
        traceSaturation,
        traceBrightness,
        traceStrength,
        if (panelMode == ColorCardTracePanelMode.Imitate) imitateColor else effectiveCreativeColor,
        imitateSharpen,
        if (panelMode == ColorCardTracePanelMode.Imitate) imitateTone else effectiveCreativeTone,
        imitateSaturation,
        imitateBrightness,
        imitateStrength,
        colorAxis,
        portraitParams,
        previewSourceBitmap,
        effectiveFaceRegions,
        subjectRect
    ) {
        buildColorCardTraceGpuParams(
            panelMode = panelMode,
            sourceColors = when (panelMode) {
                ColorCardTracePanelMode.Trace,
                ColorCardTracePanelMode.Imitate -> normalizedSourceColors
                else -> emptyList()
            },
            selectedCandidate = when (panelMode) {
                ColorCardTracePanelMode.Filter -> selectedBuiltinCandidate
                ColorCardTracePanelMode.Trace -> selectedTraceCandidate
                ColorCardTracePanelMode.Imitate -> selectedImitateCandidate
                ColorCardTracePanelMode.Picker -> null
                ColorCardTracePanelMode.Recipe -> null
            },
            selectedHaldBitmap = if (panelMode == ColorCardTracePanelMode.Filter) {
                matchedPrimaryHaldBitmap
            } else {
                null
            },
            secondaryHaldBitmap = if (panelMode == ColorCardTracePanelMode.Filter) {
                matchedSecondaryHaldBitmap
            } else {
                null
            },
            haldVariantMix = if (panelMode == ColorCardTracePanelMode.Filter) {
                resolvedSelectedHaldBlend.variantMix
            } else {
                0f
            },
            color = when (panelMode) {
                ColorCardTracePanelMode.Filter -> effectiveCreativeColor
                ColorCardTracePanelMode.Trace -> resolvedCreativeColor
                ColorCardTracePanelMode.Imitate -> resolvedCreativeColor
                ColorCardTracePanelMode.Picker -> effectiveCreativeColor
                ColorCardTracePanelMode.Recipe -> effectiveCreativeColor
            },
            sharpen = sharpen,
            tone = when (panelMode) {
                ColorCardTracePanelMode.Filter -> effectiveCreativeTone
                else -> tone
            },
            saturation = when (panelMode) {
                ColorCardTracePanelMode.Filter -> filterSaturation
                ColorCardTracePanelMode.Trace -> traceSaturation
                ColorCardTracePanelMode.Imitate -> imitateSaturation
                ColorCardTracePanelMode.Picker -> 0f
                ColorCardTracePanelMode.Recipe -> 0f
            },
            brightness = when (panelMode) {
                ColorCardTracePanelMode.Filter -> filterBrightness
                ColorCardTracePanelMode.Trace -> traceBrightness
                ColorCardTracePanelMode.Imitate -> imitateBrightness
                ColorCardTracePanelMode.Picker -> 0f
                ColorCardTracePanelMode.Recipe -> 0f
            },
            colorAxis = colorAxis,
            paletteStrength = paletteStrength,
            faceRegions = effectiveFaceRegions,
            portraitParams = portraitParams,
            subjectRect = subjectRect,
            sourceAspect = previewSourceBitmap?.let { bitmap ->
                bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat()
            } ?: 1f,
            sourceTexelWidth = previewSourceBitmap?.let { bitmap ->
                1f / bitmap.width.coerceAtLeast(1).toFloat()
            } ?: 1f,
            sourceTexelHeight = previewSourceBitmap?.let { bitmap ->
                1f / bitmap.height.coerceAtLeast(1).toFloat()
            } ?: 1f
        )
    }
    LaunchedEffect(
        active,
        panelMode,
        sourceUri,
        selectedBuiltinCandidate?.paletteId,
        effectiveCreativeColor,
        filterSharpen,
        effectiveCreativeTone,
        filterSaturation,
        filterBrightness,
        filterStrength,
        colorAxis,
        portraitParams,
        effectiveFaceRegions,
        subjectRect,
        filmGrainEnabled,
        filmGrainEligible,
        selectedFilmGrainAsset?.assetName,
        selectedFilmGrainBitmap
    ) {
        val candidate = selectedBuiltinCandidate
        if (
            !active ||
            panelMode != ColorCardTracePanelMode.Filter ||
            !isColorCardTraceBeadsPixelCandidate(candidate)
        ) {
            if (
                active &&
                panelMode == ColorCardTracePanelMode.Filter &&
                candidate?.kind == ColorCardTraceFilterKind.AiMnn
            ) {
                return@LaunchedEffect
            }
            val staleRendered = renderedPreviewBitmap
            val keepLastBeadsFrameBriefly = active &&
                panelMode != ColorCardTracePanelMode.Picker &&
                staleRendered != null &&
                !staleRendered.isRecycled
            if (keepLastBeadsFrameBriefly) {
                delay(COLOR_CARD_TRACE_RENDERED_PREVIEW_EXIT_HOLD_MS)
            }
            if (renderedPreviewBitmap === staleRendered) {
                renderedPreviewBitmap = null
            }
            staleRendered?.let { existing ->
                if (existing !== aiBasePreviewBitmap && !existing.isRecycled) {
                    deferPreviewBitmapRecycle(existing, setOf(aiBasePreviewBitmap))
                }
            }
            return@LaunchedEffect
        }

        val highResSource = decodeColorCardTracePreviewBitmap(
            context = context.applicationContext,
            uri = sourceUri,
            maxSide = COLOR_CARD_TRACE_BEADS_PREVIEW_MAX_SIDE
        )
        val resolvedHighResSource = sourceFile?.takeIf(File::exists)?.let { file ->
            decodeColorCardTraceRenderSourceBitmap(
                sourceFile = file,
                maxSide = COLOR_CARD_TRACE_BEADS_PREVIEW_MAX_SIDE
            )
        } ?: highResSource ?: return@LaunchedEffect

        try {
            val highResParams = buildColorCardTraceGpuParams(
                panelMode = ColorCardTracePanelMode.Filter,
                sourceColors = emptyList(),
                selectedCandidate = candidate,
                selectedHaldBitmap = null,
                secondaryHaldBitmap = null,
                haldVariantMix = 0f,
                color = effectiveCreativeColor,
                sharpen = filterSharpen,
                tone = effectiveCreativeTone,
                saturation = filterSaturation,
                brightness = filterBrightness,
                colorAxis = colorAxis,
                paletteStrength = filterStrength,
                faceRegions = effectiveFaceRegions,
                portraitParams = portraitParams,
                subjectRect = subjectRect,
                sourceAspect = resolvedHighResSource.width.toFloat() / resolvedHighResSource.height.coerceAtLeast(1).toFloat(),
                sourceTexelWidth = 1f / resolvedHighResSource.width.coerceAtLeast(1).toFloat(),
                sourceTexelHeight = 1f / resolvedHighResSource.height.coerceAtLeast(1).toFloat()
            )
            val highResRendered = renderColorCardTraceGpuBitmap(
                sourceBitmap = resolvedHighResSource,
                params = highResParams
            ) ?: return@LaunchedEffect

            val finalBitmap = if (filmGrainEligible && filmGrainEnabled) {
                val cachedGrainBitmap = selectedFilmGrainBitmap
                val resolvedGrainBitmap = cachedGrainBitmap ?: selectedFilmGrainAsset?.let { asset ->
                    decodeColorCardTraceFilmGrainBitmap(
                        context = context.applicationContext,
                        assetName = asset.assetName
                    )
                }
                if (resolvedGrainBitmap != null) {
                    try {
                        withContext(Dispatchers.Default) {
                            applyColorCardTraceFilmGrainOverlay(
                                sourceBitmap = highResRendered,
                                grainBitmap = resolvedGrainBitmap
                            )
                        }.also {
                            if (it !== highResRendered && !highResRendered.isRecycled) {
                                highResRendered.recycle()
                            }
                        }
                    } finally {
                        if (resolvedGrainBitmap !== cachedGrainBitmap && !resolvedGrainBitmap.isRecycled) {
                            resolvedGrainBitmap.recycle()
                        }
                    }
                } else {
                    highResRendered
                }
            } else {
                highResRendered
            }

            renderedPreviewBitmap?.let { existing ->
                if (existing !== finalBitmap && existing !== aiBasePreviewBitmap && !existing.isRecycled) {
                    deferPreviewBitmapRecycle(existing, setOf(aiBasePreviewBitmap, finalBitmap))
                }
            }
            renderedPreviewBitmap = finalBitmap
        } finally {
            if (resolvedHighResSource !== highResSource && !resolvedHighResSource.isRecycled) {
                resolvedHighResSource.recycle()
            }
            if (highResSource != null && !highResSource.isRecycled) {
                highResSource.recycle()
            }
        }
    }
    val displayColors = when (panelMode) {
        ColorCardTracePanelMode.Filter -> selectedBuiltinCandidate?.colors.orEmpty()
        ColorCardTracePanelMode.Trace -> selectedTraceCandidate?.colors ?: normalizedSourceColors
        ColorCardTracePanelMode.Imitate -> selectedImitateCandidate?.colors ?: normalizedImitateReferenceColors
        ColorCardTracePanelMode.Picker -> normalizedSourceColors
        ColorCardTracePanelMode.Recipe -> normalizedSourceColors
    }
    val saturation = when (panelMode) {
        ColorCardTracePanelMode.Filter -> filterSaturation
        ColorCardTracePanelMode.Trace -> traceSaturation
        ColorCardTracePanelMode.Imitate -> imitateSaturation
        ColorCardTracePanelMode.Picker -> 0f
        ColorCardTracePanelMode.Recipe -> 0f
    }
    val brightness = when (panelMode) {
        ColorCardTracePanelMode.Filter -> filterBrightness
        ColorCardTracePanelMode.Trace -> traceBrightness
        ColorCardTracePanelMode.Imitate -> imitateBrightness
        ColorCardTracePanelMode.Picker -> 0f
        ColorCardTracePanelMode.Recipe -> 0f
    }
    val hasActiveFilters = when (panelMode) {
        ColorCardTracePanelMode.Filter -> builtinCandidates.isNotEmpty()
        ColorCardTracePanelMode.Trace -> traceCandidates.isNotEmpty()
        ColorCardTracePanelMode.Imitate -> imitateCandidates.isNotEmpty()
        ColorCardTracePanelMode.Picker -> hasSavedFilters
        ColorCardTracePanelMode.Recipe -> hasSavedFilters
    }
    val stepFilterBy: (Int) -> Unit = { delta ->
        if (activeCandidates.isNotEmpty()) {
            when (panelMode) {
                ColorCardTracePanelMode.Filter -> {
                    val cycleCandidates = resolveColorCardTraceBuiltinSwitcherCandidates(activeCandidates)
                    val cycleCandidateIds = cycleCandidates.map(ColorCardTraceFilterCandidate::paletteId)
                    if (cycleCandidateIds.isNotEmpty()) {
                        val cycleOrder = builtinCycleOrder.filter(cycleCandidateIds::contains)
                        val currentSelectedId = selectedBuiltinFilterId?.takeIf(cycleCandidateIds::contains)
                        if (currentSelectedId == null) {
                            val nextOrder = if (
                                cycleOrder.size == cycleCandidateIds.size &&
                                cycleOrder.toSet() == cycleCandidateIds.toSet()
                            ) {
                                cycleOrder
                            } else {
                                buildColorCardTraceCycleOrder(
                                    candidateIds = cycleCandidateIds,
                                    anchorId = cycleCandidateIds.first(),
                                    seed = Random.nextLong()
                                )
                            }
                            val fallbackIndex = when {
                                delta < 0 -> nextOrder.lastIndex
                                else -> 0
                            }
                            nextOrder.getOrNull(fallbackIndex)?.let { fallbackSelectedId ->
                                val steppedCandidate = cycleCandidates.firstOrNull { it.paletteId == fallbackSelectedId }
                                builtinCycleOrder = nextOrder
                                builtinCycleIndex = fallbackIndex
                                selectedBuiltinFilterId = fallbackSelectedId
                                filterStrength = defaultColorCardTracePaletteStrength(steppedCandidate)
                                builtinSelectionExplicit = true
                                hasUserEdits = true
                            }
                        } else {
                            stepColorCardTraceCycle(
                                candidateIds = cycleCandidateIds,
                                currentOrder = cycleOrder,
                                currentIndex = builtinCycleIndex,
                                currentSelectedId = currentSelectedId,
                                delta = delta,
                                seed = Random.nextLong()
                            )?.let { step ->
                                val steppedCandidate = cycleCandidates.firstOrNull { it.paletteId == step.selectedId }
                                builtinCycleOrder = step.order
                                builtinCycleIndex = step.index
                                selectedBuiltinFilterId = step.selectedId
                                filterStrength = defaultColorCardTracePaletteStrength(steppedCandidate)
                                builtinSelectionExplicit = true
                                hasUserEdits = true
                            }
                        }
                    }
                }
                ColorCardTracePanelMode.Trace -> {
                    stepColorCardTraceCycle(
                        candidateIds = activeCandidates.map(ColorCardTraceFilterCandidate::paletteId),
                        currentOrder = traceCycleOrder,
                        currentIndex = traceCycleIndex,
                        currentSelectedId = selectedTraceFilterId,
                        delta = delta,
                        seed = Random.nextLong()
                    )?.let { step ->
                        traceCycleOrder = step.order
                        traceCycleIndex = step.index
                        selectedTraceFilterId = step.selectedId
                        hasUserEdits = true
                    }
                }
                ColorCardTracePanelMode.Imitate -> Unit
                ColorCardTracePanelMode.Picker -> Unit
                ColorCardTracePanelMode.Recipe -> Unit
            }
        }
    }
    val updateAdjustment: (Float, Float) -> Unit = { normalizedX, normalizedY ->
        val nextColor = ((normalizedX.coerceIn(0f, 1f) - 0.5f) * 2f).coerceIn(-1f, 1f)
        val nextTone = ((0.5f - normalizedY.coerceIn(0f, 1f)) * 2f).coerceIn(-1f, 1f)
        when (panelMode) {
            ColorCardTracePanelMode.Filter -> {
                if (!isColorCardTraceAiMnnCandidate(selectedBuiltinCandidate)) {
                    filterColor = nextColor
                    filterTone = nextTone
                    builtinSelectionExplicit = true
                    hasUserEdits = true
                }
            }
            ColorCardTracePanelMode.Trace -> {
                traceColor = nextColor
                traceTone = nextTone
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Imitate -> {
                imitateColor = nextColor
                imitateTone = nextTone
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Picker -> Unit
            ColorCardTracePanelMode.Recipe -> Unit
        }
    }
    val updateSharpen: (Float) -> Unit = { value ->
        when (panelMode) {
            ColorCardTracePanelMode.Filter -> {
                if (!isColorCardTraceAiMnnCandidate(selectedBuiltinCandidate)) {
                    filterSharpen = value.coerceIn(-1f, 1f)
                    builtinSelectionExplicit = true
                    hasUserEdits = true
                }
            }
            ColorCardTracePanelMode.Trace -> {
                traceSharpen = value.coerceIn(-1f, 1f)
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Imitate -> {
                imitateSharpen = value.coerceIn(-1f, 1f)
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Picker -> Unit
            ColorCardTracePanelMode.Recipe -> Unit
        }
    }
    val updateSaturation: (Float) -> Unit = { value ->
        when (panelMode) {
            ColorCardTracePanelMode.Filter -> {
                if (!isColorCardTraceAiMnnCandidate(selectedBuiltinCandidate)) {
                    filterSaturation = value.coerceIn(-1f, 1f)
                    builtinSelectionExplicit = true
                    hasUserEdits = true
                }
            }
            ColorCardTracePanelMode.Trace -> {
                traceSaturation = value.coerceIn(-1f, 1f)
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Imitate -> {
                imitateSaturation = value.coerceIn(-1f, 1f)
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Picker -> Unit
            ColorCardTracePanelMode.Recipe -> Unit
        }
    }
    val updateBrightness: (Float) -> Unit = { value ->
        when (panelMode) {
            ColorCardTracePanelMode.Filter -> {
                if (!isColorCardTraceAiMnnCandidate(selectedBuiltinCandidate)) {
                    filterBrightness = value.coerceIn(-1f, 1f)
                    builtinSelectionExplicit = true
                    hasUserEdits = true
                }
            }
            ColorCardTracePanelMode.Trace -> {
                traceBrightness = value.coerceIn(-1f, 1f)
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Imitate -> {
                imitateBrightness = value.coerceIn(-1f, 1f)
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Picker -> Unit
            ColorCardTracePanelMode.Recipe -> Unit
        }
    }
    val updatePaletteStrength: (Float) -> Unit = { value ->
        when (panelMode) {
            ColorCardTracePanelMode.Filter -> {
                if (!isColorCardTraceAiMnnCandidate(selectedBuiltinCandidate)) {
                    filterStrength = value.coerceIn(0f, 1f)
                    builtinSelectionExplicit = true
                    hasUserEdits = true
                }
            }
            ColorCardTracePanelMode.Trace -> {
                traceStrength = value.coerceIn(0f, 1f)
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Imitate -> {
                imitateStrength = value.coerceIn(0f, 1f)
                hasUserEdits = true
            }
            ColorCardTracePanelMode.Picker -> Unit
            ColorCardTracePanelMode.Recipe -> Unit
        }
    }
    val updatePortraitVitality: (Float) -> Unit = { value ->
        portraitVitality = value.coerceIn(0f, 1f)
        hasUserEdits = true
    }
    val updatePortraitSoftness: (Float) -> Unit = { value ->
        portraitSoftness = value.coerceIn(0f, 1f)
        hasUserEdits = true
    }
    val updatePortraitDetailSuppression: (Float) -> Unit = { value ->
        portraitDetailSuppression = value.coerceIn(0f, 1f)
        hasUserEdits = true
    }

    return ColorCardTraceFilterState(
        mode = panelMode,
        hasUserEdits = hasUserEdits,
        hasSavedFilters = hasActiveFilters,
        hasBuiltinFilters = builtinCandidates.isNotEmpty(),
        hasTraceFilters = traceCandidates.isNotEmpty() || imitateCandidates.isNotEmpty(),
        canSaveRenderedImage = when (panelMode) {
            ColorCardTracePanelMode.Filter -> selectedBuiltinCandidate != null
            ColorCardTracePanelMode.Trace -> selectedTraceCandidate != null && normalizedSourceColors.isNotEmpty()
            ColorCardTracePanelMode.Imitate -> {
                selectedImitateCandidate != null &&
                    normalizedSourceColors.isNotEmpty() &&
                    normalizedImitateReferenceColors.isNotEmpty()
            }
            ColorCardTracePanelMode.Picker -> false
            ColorCardTracePanelMode.Recipe -> false
        },
        isAnalyzing = active && when (panelMode) {
            ColorCardTracePanelMode.Filter -> filterResourcesLoading
            ColorCardTracePanelMode.Trace -> normalizedSourceColors.isEmpty() || traceCandidatesLoading
            ColorCardTracePanelMode.Imitate -> {
                normalizedSourceColors.isEmpty() || imitateReferenceLoading
            }
            else -> false
        },
        isRendering = previewSourceBitmap != null && when (panelMode) {
            ColorCardTracePanelMode.Filter -> when (selectedBuiltinCandidate?.kind) {
                ColorCardTraceFilterKind.HaldClut -> matchedPrimaryHaldBitmap != null
                ColorCardTraceFilterKind.AiMnn -> renderedPreviewBitmap != null
                ColorCardTraceFilterKind.PixelPoster -> true
                ColorCardTraceFilterKind.SelectiveColor -> true
                ColorCardTraceFilterKind.SubjectFrame -> true
                ColorCardTraceFilterKind.BlackMist -> true
                ColorCardTraceFilterKind.MotionPhotoEntry -> true
                else -> false
            }
            ColorCardTracePanelMode.Trace -> selectedTraceCandidate != null && normalizedSourceColors.isNotEmpty()
            ColorCardTracePanelMode.Imitate -> {
                selectedImitateCandidate != null &&
                    normalizedSourceColors.isNotEmpty() &&
                    normalizedImitateReferenceColors.isNotEmpty()
            }
            ColorCardTracePanelMode.Picker -> false
            ColorCardTracePanelMode.Recipe -> false
        },
        renderedPreviewBitmap = renderedPreviewBitmap,
        sourceColors = normalizedSourceColors,
        displayColors = displayColors,
        imitateTransferProfile = resolvedImitateTransferProfile,
        candidates = activeCandidates,
        selectedCandidateIndex = selectedCandidateIndex,
        selectedCandidate = selectedCandidate,
        isOriginalSelected = false,
        previewSourceBitmap = previewSourceBitmap,
        faceRegions = effectiveFaceRegions,
        subjectRect = subjectRect,
        gpuParams = gpuParams,
        portraitAvailable = portraitAvailable,
        portraitAdjustments = portraitAdjustments,
        filmGrainEligible = filmGrainEligible,
        filmGrainEnabled = filmGrainEligible && filmGrainEnabled,
        filmGrainAssets = filmGrainAssets,
        selectedFilmGrainAsset = selectedFilmGrainAsset,
        selectedFilmGrainBitmap = if (filmGrainEligible && filmGrainEnabled) selectedFilmGrainBitmap else null,
        motionPhotoRevealPattern = filterMotionPhotoRevealPattern,
        color = color,
        colorAxis = colorAxis,
        sharpen = sharpen,
        tone = tone,
        saturation = saturation,
        brightness = brightness,
        paletteStrength = paletteStrength,
        allowsManualAdjustment = allowsManualAdjustment,
        allowsStrengthAdjustment = allowsStrengthAdjustment,
        stepFilterBy = stepFilterBy,
        selectOriginal = {},
        selectCandidate = { paletteId ->
            when (panelMode) {
                ColorCardTracePanelMode.Filter -> {
                    if (builtinCandidates.any { it.paletteId == paletteId }) {
                        val selectedBuiltinCandidate = builtinCandidates.firstOrNull { it.paletteId == paletteId }
                        val cycleOrder = buildColorCardTraceCycleOrder(
                            candidateIds = builtinCandidates.map(ColorCardTraceFilterCandidate::paletteId),
                            anchorId = paletteId,
                            seed = Random.nextLong()
                        )
                        builtinCycleOrder = cycleOrder
                        builtinCycleIndex = cycleOrder.indexOf(paletteId).coerceAtLeast(0)
                        selectedBuiltinFilterId = paletteId
                        filterStrength = defaultColorCardTracePaletteStrength(selectedBuiltinCandidate)
                        builtinSelectionExplicit = true
                        hasUserEdits = true
                    }
                }
                ColorCardTracePanelMode.Trace -> {
                    if (traceCandidates.any { it.paletteId == paletteId }) {
                        val cycleOrder = buildColorCardTraceCycleOrder(
                            candidateIds = traceCandidates.map(ColorCardTraceFilterCandidate::paletteId),
                            anchorId = paletteId,
                            seed = Random.nextLong()
                        )
                        traceCycleOrder = cycleOrder
                        traceCycleIndex = cycleOrder.indexOf(paletteId).coerceAtLeast(0)
                        selectedTraceFilterId = paletteId
                        hasUserEdits = true
                    }
                }
                ColorCardTracePanelMode.Imitate -> Unit
                ColorCardTracePanelMode.Picker -> Unit
                ColorCardTracePanelMode.Recipe -> Unit
            }
        },
        updateAdjustment = updateAdjustment,
        updateSharpen = updateSharpen,
        updateSaturation = updateSaturation,
        updateBrightness = updateBrightness,
        updatePaletteStrength = updatePaletteStrength,
        updatePortraitVitality = updatePortraitVitality,
        updatePortraitSoftness = updatePortraitSoftness,
        updatePortraitDetailSuppression = updatePortraitDetailSuppression,
        setFilmGrainEnabled = { enabled ->
            filmGrainEnabled = enabled
            if (enabled) {
                if (selectedFilmGrainId == null) {
                    filmGrainAssets.firstOrNull()?.id?.let(selectFilmGrainIdAndResetQueue)
                } else {
                    remainingFilmGrainQueue = buildFilmGrainRefreshQueue(selectedFilmGrainId)
                }
            }
            if (panelMode == ColorCardTracePanelMode.Filter) {
                builtinSelectionExplicit = true
                hasUserEdits = true
            }
        },
        selectFilmGrainAsset = { assetId ->
            if (filmGrainAssets.any { it.id == assetId }) {
                selectFilmGrainIdAndResetQueue(assetId)
                if (panelMode == ColorCardTracePanelMode.Filter) {
                    builtinSelectionExplicit = true
                    hasUserEdits = true
                }
            }
        },
        refreshFilmGrainAsset = {
            refreshFilmGrainSelection()
            if (panelMode == ColorCardTracePanelMode.Filter) {
                builtinSelectionExplicit = true
                hasUserEdits = true
            }
        },
        updateMotionPhotoRevealPattern = { revealPattern ->
            if (panelMode == ColorCardTracePanelMode.Filter) {
                filterMotionPhotoRevealPattern = revealPattern
                builtinSelectionExplicit = true
                hasUserEdits = true
            }
        },
        resetAdjustment = {
            when (panelMode) {
                ColorCardTracePanelMode.Filter -> {
                    filterColor = 0f
                    filterSharpen = 0f
                    filterTone = 0f
                    filterSaturation = 0f
                    filterBrightness = 0f
                    filterStrength = defaultColorCardTracePaletteStrength(selectedBuiltinCandidate)
                    filterMotionPhotoRevealPattern = MotionPhotoRevealPattern.SinglePoint
                }
                ColorCardTracePanelMode.Trace -> {
                    traceColor = 0f
                    traceSharpen = 0f
                    traceTone = 0f
                    traceSaturation = 0f
                    traceBrightness = 0f
                    traceStrength = 0.5f
                }
                ColorCardTracePanelMode.Imitate -> {
                    imitateColor = imitateDefaultColor
                    imitateSharpen = imitateDefaultSharpen
                    imitateTone = imitateDefaultTone
                    imitateSaturation = imitateDefaultSaturation
                    imitateBrightness = imitateDefaultBrightness
                    imitateStrength = imitateDefaultStrength
                }
                ColorCardTracePanelMode.Picker -> Unit
                ColorCardTracePanelMode.Recipe -> Unit
            }
        },
        resetManualAdjustments = {
            when (panelMode) {
                ColorCardTracePanelMode.Filter -> {
                    filterColor = 0f
                    filterSharpen = 0f
                    filterTone = 0f
                    filterSaturation = 0f
                    filterBrightness = 0f
                    filterStrength = defaultColorCardTracePaletteStrength(selectedBuiltinCandidate)
                    filterMotionPhotoRevealPattern = MotionPhotoRevealPattern.SinglePoint
                    filmGrainEnabled = false
                    val defaultFilmGrainId = filmGrainAssets.firstOrNull()?.id
                    if (defaultFilmGrainId == null) {
                        selectedFilmGrainId = null
                        remainingFilmGrainQueue = emptyList()
                    } else {
                        selectFilmGrainIdAndResetQueue(defaultFilmGrainId)
                    }
                }
                ColorCardTracePanelMode.Trace -> {
                    traceColor = 0f
                    traceSharpen = 0f
                    traceTone = 0f
                    traceSaturation = 0f
                    traceBrightness = 0f
                    traceStrength = 0.5f
                }
                ColorCardTracePanelMode.Imitate -> {
                    imitateColor = imitateDefaultColor
                    imitateSharpen = imitateDefaultSharpen
                    imitateTone = imitateDefaultTone
                    imitateSaturation = imitateDefaultSaturation
                    imitateBrightness = imitateDefaultBrightness
                    imitateStrength = imitateDefaultStrength
                }
                ColorCardTracePanelMode.Picker -> Unit
                ColorCardTracePanelMode.Recipe -> Unit
            }
        },
        resetPortraitAdjustments = {
            hasUserEdits = hasUserEdits || !portraitAdjustments.isDefault
            portraitVitality = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE
            portraitSoftness = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE
            portraitDetailSuppression = COLOR_CARD_TRACE_PORTRAIT_DEFAULT_VALUE
        }
    )
}

@Composable
internal fun BoxScope.ColorCardTraceFilterImageOverlay(
    traceState: ColorCardTraceFilterState,
    colorPickerState: HomeCardColorPickerState,
    panelMode: ColorCardTracePanelMode,
    comparePressed: Boolean = false,
    showPortraitFaces: Boolean = false,
    showLiveBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val revealProgress = remember { Animatable(0f) }
    val revealBandWidthPx = with(density) { 112.dp.toPx() }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var lastAiRenderedPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiExitOverlayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val effectModeVisible = panelMode != ColorCardTracePanelMode.Picker
    val renderedPreviewBitmap = traceState.renderedPreviewBitmap?.takeIf { !it.isRecycled }
    val selectedCandidateKind = traceState.selectedCandidate?.kind
    val aiExitOverlayAlpha by animateFloatAsState(
        targetValue = if (aiExitOverlayBitmap != null) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "colorCardTraceAiExitOverlayAlpha"
    )
    DisposableEffect(Unit) {
        onDispose {
            recycleColorCardTraceBitmap(aiExitOverlayBitmap)
        }
    }
    val isPixelPosterPreview = effectModeVisible &&
        selectedCandidateKind == ColorCardTraceFilterKind.PixelPoster
    val isMotionPhotoPreview = effectModeVisible &&
        panelMode == ColorCardTracePanelMode.Filter &&
        selectedCandidateKind == ColorCardTraceFilterKind.MotionPhotoEntry
    val motionPhotoPreviewSource = traceState.previewSourceBitmap?.takeIf { !it.isRecycled }
    val motionPhotoGrayscaleBitmap by produceState<Bitmap?>(
        initialValue = null,
        motionPhotoPreviewSource,
        isMotionPhotoPreview
    ) {
        if (!isMotionPhotoPreview || motionPhotoPreviewSource == null) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            MotionPhotoFrameRenderer.createGrayscaleBitmap(motionPhotoPreviewSource)
        }
    }
    val revealAnimationKey = remember(panelMode, traceState.selectedCandidate?.paletteId) {
        "${panelMode.name}:${traceState.selectedCandidate?.paletteId.orEmpty()}"
    }
    val replacementPreviewAvailable = remember(
        comparePressed,
        renderedPreviewBitmap,
        traceState.previewSourceBitmap
    ) {
        comparePressed ||
            (renderedPreviewBitmap?.isRecycled == false) ||
            (traceState.previewSourceBitmap?.isRecycled == false)
    }
    val shouldShowAiExitOverlay = remember(
        effectModeVisible,
        selectedCandidateKind,
        replacementPreviewAvailable
    ) {
        shouldShowColorCardTraceAiExitOverlay(
            effectModeVisible = effectModeVisible,
            selectedCandidateKind = selectedCandidateKind,
            replacementPreviewAvailable = replacementPreviewAvailable
        )
    }
    var motionPhotoRevealSeed by remember(revealAnimationKey, traceState.motionPhotoRevealPattern) {
        mutableStateOf<Long>(0L)
    }
    LaunchedEffect(renderedPreviewBitmap, selectedCandidateKind) {
        if (
            selectedCandidateKind == ColorCardTraceFilterKind.AiMnn &&
            renderedPreviewBitmap != null &&
            !renderedPreviewBitmap.isRecycled
        ) {
            lastAiRenderedPreviewBitmap = renderedPreviewBitmap
            recycleColorCardTraceBitmap(aiExitOverlayBitmap)
            aiExitOverlayBitmap = null
        }
    }
    LaunchedEffect(shouldShowAiExitOverlay, lastAiRenderedPreviewBitmap) {
        val lastAiBitmap = lastAiRenderedPreviewBitmap
        if (
            !shouldShowAiExitOverlay ||
            lastAiBitmap == null ||
            lastAiBitmap.isRecycled
        ) {
            recycleColorCardTraceBitmap(aiExitOverlayBitmap)
            aiExitOverlayBitmap = null
            return@LaunchedEffect
        }
        if (aiExitOverlayBitmap != null && !aiExitOverlayBitmap!!.isRecycled) {
            return@LaunchedEffect
        }
        val overlayBitmap = withContext(Dispatchers.Default) {
            copyColorCardTraceBitmap(lastAiBitmap)
        }
        recycleColorCardTraceBitmap(aiExitOverlayBitmap)
        aiExitOverlayBitmap = overlayBitmap
    }
    LaunchedEffect(aiExitOverlayBitmap) {
        val overlayBitmap = aiExitOverlayBitmap
            ?.takeIf { !it.isRecycled }
            ?: return@LaunchedEffect
        delay(COLOR_CARD_TRACE_AI_EXIT_HOLD_MS)
        if (aiExitOverlayBitmap === overlayBitmap) {
            recycleColorCardTraceBitmap(aiExitOverlayBitmap)
            aiExitOverlayBitmap = null
        }
    }

    LaunchedEffect(effectModeVisible, traceState.previewSourceBitmap, revealAnimationKey, isMotionPhotoPreview) {
        if (!effectModeVisible) {
            revealProgress.snapTo(0f)
            return@LaunchedEffect
        }
        val previewBitmap = traceState.previewSourceBitmap ?: run {
            revealProgress.snapTo(0f)
            return@LaunchedEffect
        }
        revealProgress.stop()
        revealProgress.snapTo(0f)
        if (!previewBitmap.isRecycled) {
            if (isMotionPhotoPreview) {
                while (true) {
                    motionPhotoRevealSeed = Random.nextLong()
                    revealProgress.snapTo(0f)
                    revealProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = MotionPhotoFrameRenderer.DEFAULT_EFFECT_DURATION_MS.toInt(),
                            easing = FastOutSlowInEasing
                        )
                    )
                    delay(MotionPhotoFrameRenderer.EFFECT_LOOP_PAUSE_MS)
                }
            } else {
                revealProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = COLOR_CARD_TRACE_FILTER_REVEAL_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .matchParentSize()
            .onSizeChanged { previewSize = it }
    ) {
        if (!isMotionPhotoPreview && effectModeVisible) {
            ColorCardTraceGpuPreview(
                sourceBitmap = traceState.previewSourceBitmap,
                params = traceState.gpuParams,
                revealProgress = revealProgress.value,
                modifier = Modifier.matchParentSize()
            )
        }

        if (isMotionPhotoPreview && motionPhotoPreviewSource != null && motionPhotoGrayscaleBitmap != null) {
            MotionPhotoEntryPreviewOverlay(
                colorBitmap = motionPhotoPreviewSource,
                grayscaleBitmap = motionPhotoGrayscaleBitmap!!,
                revealProgress = revealProgress.value,
                revealPattern = traceState.motionPhotoRevealPattern,
                revealSeed = motionPhotoRevealSeed,
                modifier = Modifier.matchParentSize()
            )
        } else if (renderedPreviewBitmap != null && effectModeVisible) {
            Image(
                bitmap = renderedPreviewBitmap.asImageBitmap(),
                contentDescription = "滤镜预览",
                contentScale = ContentScale.Crop,
                filterQuality = if (isPixelPosterPreview) FilterQuality.None else FilterQuality.Low,
                modifier = Modifier.matchParentSize()
            )
        }

        if (
            renderedPreviewBitmap == null &&
            !comparePressed &&
            effectModeVisible &&
            panelMode == ColorCardTracePanelMode.Filter &&
            traceState.filmGrainEnabled &&
            traceState.filmGrainEligible &&
            traceState.selectedFilmGrainBitmap != null &&
            !traceState.selectedFilmGrainBitmap.isRecycled
        ) {
            val grainImageBitmap = traceState.selectedFilmGrainBitmap.asImageBitmap()
            Canvas(modifier = Modifier.matchParentSize()) {
                drawImage(
                    image = grainImageBitmap,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    alpha = COLOR_CARD_TRACE_FILM_GRAIN_BLEND_ALPHA,
                    blendMode = BlendMode.Overlay
                )
            }
        }

        if (
            comparePressed &&
            effectModeVisible &&
            traceState.previewSourceBitmap != null &&
            !traceState.previewSourceBitmap.isRecycled
        ) {
            Image(
                bitmap = traceState.previewSourceBitmap.asImageBitmap(),
                contentDescription = "原图预览",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        if (
            !comparePressed &&
            effectModeVisible &&
            shouldShowAiExitOverlay &&
            aiExitOverlayBitmap != null &&
            !aiExitOverlayBitmap!!.isRecycled &&
            aiExitOverlayAlpha > 0.001f
        ) {
            Image(
                bitmap = aiExitOverlayBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = aiExitOverlayAlpha,
                modifier = Modifier.matchParentSize()
            )
        }

        if (
            !isMotionPhotoPreview &&
            effectModeVisible &&
            traceState.previewSourceBitmap != null &&
            revealProgress.value in 0.001f..0.999f &&
            previewSize.width > 0 &&
            previewSize.height > 0
        ) {
            val travelDistance = previewSize.width.toFloat() + (revealBandWidthPx * 2f)
            val bandLeft = (-revealBandWidthPx) + (travelDistance * revealProgress.value)
            val visibleLeft = bandLeft.coerceIn(0f, previewSize.width.toFloat())
            val bandRight = (bandLeft + revealBandWidthPx).coerceIn(0f, previewSize.width.toFloat())
            val bandWidth = (bandRight - visibleLeft).coerceAtLeast(0f)

            if (bandWidth > 0f) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = visibleLeft.roundToInt(),
                                y = 0
                            )
                        }
                        .width(with(density) { bandWidth.toDp() })
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        if (panelMode == ColorCardTracePanelMode.Picker) {
            HomeCardColorPickerImageOverlay(state = colorPickerState)
        }

        if (showPortraitFaces && traceState.faceRegions.isNotEmpty()) {
            ColorCardTracePortraitFacesOverlay(faceRegions = traceState.faceRegions)
        }

        if (showLiveBadge) {
            DynamicLiveBadge(
                isDark = androidx.compose.foundation.isSystemInDarkTheme(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }

    }
}

@Composable
internal fun BoxScope.ColorCardTraceLensyFloatingOverlay(
    message: String?,
    imageBoundsInRoot: Rect?,
    windowSizePx: IntSize,
    onDismissRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (message.isNullOrBlank()) return

    val density = LocalDensity.current
    val sideMarginPx = with(density) { 16.dp.roundToPx() }
    val topMarginPx = with(density) { 18.dp.roundToPx() }
    val insideInsetPx = with(density) { 14.dp.roundToPx() }
    val outsideGapPx = with(density) { 10.dp.roundToPx() }
    val closeButtonEndPaddingPx = with(density) { 16.dp.roundToPx() }
    val closeButtonSizePx = with(density) { 40.dp.roundToPx() }
    val closeButtonGapPx = with(density) { 12.dp.roundToPx() }
    val lensyAvatarWidthPx = with(density) { 36.dp.roundToPx() }
    val lensyAvatarGapPx = with(density) { 6.dp.roundToPx() }
    val lensyTextDefaultMaxWidthPx = with(density) { 260.dp.roundToPx() }
    val estimatedWidthPx = with(density) { 304.dp.roundToPx() }
    val estimatedHeightPx = with(density) { 56.dp.roundToPx() }
    val safeWindowWidth = windowSizePx.width.coerceAtLeast(1)
    val safeWindowHeight = windowSizePx.height.coerceAtLeast(1)
    val bounds = imageBoundsInRoot
    val maxX = (safeWindowWidth - estimatedWidthPx - sideMarginPx).coerceAtLeast(sideMarginPx)
    val insideYMax = (safeWindowHeight - estimatedHeightPx - sideMarginPx).coerceAtLeast(topMarginPx)
    val outsideY = bounds?.top?.roundToInt()?.minus(estimatedHeightPx + outsideGapPx) ?: Int.MIN_VALUE
    val preferOutsideTop = bounds != null && outsideY >= topMarginPx
    val topAvailableWidthPx = (
        safeWindowWidth -
            sideMarginPx -
            closeButtonEndPaddingPx -
            closeButtonSizePx -
            closeButtonGapPx
        ).coerceAtLeast(1)
    val outsideTextMaxWidthPx = (
        topAvailableWidthPx -
            lensyAvatarWidthPx -
            lensyAvatarGapPx
        ).coerceIn(1, lensyTextDefaultMaxWidthPx)
    val targetX = if (preferOutsideTop) {
        sideMarginPx
    } else if (bounds != null) {
        (bounds.left.roundToInt() + insideInsetPx).coerceIn(sideMarginPx, maxX)
    } else {
        sideMarginPx
    }
    val targetY = if (preferOutsideTop) {
        outsideY
    } else {
        ((bounds?.top?.roundToInt() ?: topMarginPx) + insideInsetPx).coerceIn(topMarginPx, insideYMax)
    }

    ColorCardLensyOverlay(
        message = message,
        onDismissRequest = onDismissRequest,
        textMaxWidth = if (preferOutsideTop) {
            with(density) { outsideTextMaxWidthPx.toDp() }
        } else {
            260.dp
        },
        modifier = modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(targetX, targetY) }
    )
}

@Composable
internal fun BoxScope.ColorCardTraceFilterFooterOverlay(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    colorPickerState: HomeCardColorPickerState,
    traceState: ColorCardTraceFilterState,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (!active) return

    val localContext = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val traceCopy = remember(appLanguage) { getColorCardTraceCopy(appLanguage) }
    val panelShape = RoundedCornerShape(24.dp)
    val backgroundColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    } else {
        Color.White.copy(alpha = 0.76f)
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
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
                .padding(horizontal = 24.dp, vertical = 30.dp)
                .widthIn(max = 440.dp)
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
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = colorCardTraceFilterPrimaryTitle(traceState, traceState.mode),
                        color = primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = colorCardTraceFilterSecondaryTitle(traceState, traceState.mode),
                        color = secondaryText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                ColorCardTraceFilterCountStepper(
                    pointCount = colorPickerState.pointCount,
                    canAddPoint = colorPickerState.canAddPoint,
                    canRemovePoint = colorPickerState.canRemovePoint,
                    onAddPoint = colorPickerState.addPoint,
                    onRemovePoint = colorPickerState.removePoint,
                    contentColor = primaryText,
                    secondaryText = secondaryText,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = colorPickerState.refresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "刷新取色点",
                        tint = primaryText,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = colorPickerState.save,
                    enabled = colorPickerState.isReady,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "保存色卡",
                        tint = if (colorPickerState.isReady) primaryText else secondaryText,
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
                traceState.displayColors.forEach { colorInt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(colorInt))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorCardTraceAdjustmentPad(
                    color = traceState.color,
                    tone = traceState.tone,
                    colorAxis = traceState.colorAxis,
                    onAdjust = traceState.updateAdjustment,
                    isDark = isDark
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "X 轴沿当前风格推进色彩，Y 轴推拉影调。",
                        color = secondaryText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDark) {
                                    Color.White.copy(alpha = 0.06f)
                                } else {
                                    Color.Black.copy(alpha = 0.045f)
                                }
                            )
                            .clickable(onClick = traceState.resetAdjustment)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "回中",
                            color = primaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.ColorCardTraceFilterDeckOverlay(
    active: Boolean,
    visuals: HomeCardLongPressHoverVisuals,
    colorPickerState: HomeCardColorPickerState,
    traceState: ColorCardTraceFilterState,
    isDark: Boolean,
    panelMode: ColorCardTracePanelMode,
    effectPanelMode: ColorCardTracePanelMode,
    recipes: List<SavedColorCardRecipe>,
    saveImageState: ColorCardTraceSaveImageState,
    comparePressed: Boolean = false,
    portraitPanelExpanded: Boolean,
    onPanelModeChange: (ColorCardTracePanelMode) -> Unit,
    onComparePressedChange: (Boolean) -> Unit = {},
    onPortraitPanelExpandedChange: (Boolean) -> Unit,
    onSaveImage: () -> Unit,
    onSaveRecipe: () -> Unit,
    onApplyRecipe: (SavedColorCardRecipe) -> Unit,
    onShareRecipe: (SavedColorCardRecipe) -> Unit,
    onDeleteRecipe: (SavedColorCardRecipe) -> Unit,
    onImportRecipes: () -> Unit,
    onExportAllRecipes: () -> Unit,
    onMotionPhotoEntryClick: () -> Unit,
    imitateReferencePreviewUri: Uri? = null,
    imitateReferenceDisplayName: String? = null,
    imitateReferenceColors: List<Int> = emptyList(),
    imitateReferenceLoading: Boolean = false,
    onPickImitateReference: () -> Unit = {},
    onClearImitateReference: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!active) return

    val localContext = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val traceCopy = remember(appLanguage) { getColorCardTraceCopy(appLanguage) }
    val panelShape = RoundedCornerShape(24.dp)
    val backgroundColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    } else {
        Color.White.copy(alpha = 0.76f)
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF171717)
    val secondaryText = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    } else {
        Color(0xFF626262)
    }
    val builtinPreviewBitmaps = rememberColorCardTraceBuiltinPreviewBitmaps(
        enabled = active && effectPanelMode == ColorCardTracePanelMode.Filter,
        candidates = if (effectPanelMode == ColorCardTracePanelMode.Filter) {
            traceState.candidates
        } else {
            emptyList()
        }
    )
    val filmGrainPreviewBitmaps = rememberColorCardTraceFilmGrainPreviewBitmaps(
        enabled = false,
        assets = emptyList()
    )
    val recentFilterUsageStore = remember(localContext) { ColorCardRecentFilterUsageStore.get(localContext) }
    val favoriteFilterStore = remember(localContext) { ColorCardFavoriteFilterStore.get(localContext) }
    val recentFilterIds by recentFilterUsageStore.topFilterIds.collectAsState()
    val favoriteFilterIds by favoriteFilterStore.favoriteFilterIds.collectAsState()
    var expandedFilterPicker by remember { mutableStateOf(false) }
    var expandedFilmGrainPicker by remember { mutableStateOf(false) }
    var showFilterPreviewPage by remember { mutableStateOf(false) }
    var collapsedEffectPanel by remember { mutableStateOf(false) }
    LaunchedEffect(panelMode, traceState.candidates) {
        if (
            (
                panelMode != ColorCardTracePanelMode.Filter &&
                    panelMode != ColorCardTracePanelMode.Trace
                ) ||
            traceState.candidates.isEmpty()
        ) {
            expandedFilterPicker = false
            expandedFilmGrainPicker = false
            showFilterPreviewPage = false
        }
    }
    LaunchedEffect(traceState.portraitAvailable, panelMode) {
        if (!traceState.portraitAvailable) {
            onPortraitPanelExpandedChange(false)
        }
        if (
            panelMode == ColorCardTracePanelMode.Picker ||
            panelMode == ColorCardTracePanelMode.Recipe
        ) {
            onPortraitPanelExpandedChange(false)
        }
    }
    LaunchedEffect(
        effectPanelMode,
        traceState.filmGrainEligible,
        traceState.filmGrainEnabled,
        traceState.filmGrainAssets
    ) {
        if (
            effectPanelMode != ColorCardTracePanelMode.Filter ||
            !traceState.filmGrainEligible ||
            !traceState.filmGrainEnabled ||
            traceState.filmGrainAssets.isEmpty()
        ) {
            expandedFilmGrainPicker = false
        }
    }
    LaunchedEffect(panelMode, expandedFilterPicker, expandedFilmGrainPicker) {
        if (
            panelMode == ColorCardTracePanelMode.Picker ||
                panelMode == ColorCardTracePanelMode.Recipe ||
                expandedFilterPicker ||
                expandedFilmGrainPicker
        ) {
            collapsedEffectPanel = false
        }
    }
    LaunchedEffect(expandedFilterPicker, expandedFilmGrainPicker) {
        if (expandedFilterPicker || expandedFilmGrainPicker) {
            onPortraitPanelExpandedChange(false)
        }
    }
    BackHandler(
        enabled = showFilterPreviewPage || expandedFilterPicker || expandedFilmGrainPicker || portraitPanelExpanded
    ) {
        when {
            showFilterPreviewPage -> {
                showFilterPreviewPage = false
                expandedFilterPicker = false
            }
            expandedFilmGrainPicker -> expandedFilmGrainPicker = false
            expandedFilterPicker -> expandedFilterPicker = false
            portraitPanelExpanded -> onPortraitPanelExpandedChange(false)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = !expandedFilterPicker && !expandedFilmGrainPicker,
            enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / 5 },
            exit = fadeOut() + slideOutVertically { fullHeight -> fullHeight / 5 },
            label = "colorCardTraceDeckCollapsed"
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 30.dp)
                    .widthIn(max = 440.dp)
                    .graphicsLayer {
                        alpha = visuals.footerContentAlpha
                    },
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (panelMode == ColorCardTracePanelMode.Picker) {
                    ColorCardTraceModeSelector(
                        panelMode = panelMode,
                        isDark = isDark,
                        collapsed = false,
                        onToggleCollapsed = null,
                        portraitButton = null,
                        onModeChange = onPanelModeChange,
                        traceCopy = traceCopy
                    )
                    ColorCardTracePickerPanel(
                        colorPickerState = colorPickerState,
                        pickerColors = colorPickerState.colors,
                        isDark = isDark,
                        panelShape = panelShape,
                        backgroundColor = backgroundColor,
                        borderColor = borderColor,
                        primaryText = primaryText,
                        secondaryText = secondaryText
                    )
                } else if (panelMode == ColorCardTracePanelMode.Recipe) {
                    ColorCardTraceModeSelector(
                        panelMode = panelMode,
                        isDark = isDark,
                        collapsed = false,
                        onToggleCollapsed = null,
                        portraitButton = null,
                        onModeChange = onPanelModeChange,
                        traceCopy = traceCopy
                    )
                    ColorCardTraceRecipePanel(
                        recipes = recipes,
                        isDark = isDark,
                        panelShape = panelShape,
                        backgroundColor = backgroundColor,
                        borderColor = borderColor,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onApplyRecipe = onApplyRecipe,
                        onShareRecipe = onShareRecipe,
                        onDeleteRecipe = onDeleteRecipe,
                        onImportRecipes = onImportRecipes,
                        onExportAllRecipes = onExportAllRecipes
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorCardTraceModeSelector(
                            panelMode = panelMode,
                            isDark = isDark,
                            collapsed = collapsedEffectPanel,
                            onToggleCollapsed = {
                                if (portraitPanelExpanded) {
                                    onPortraitPanelExpandedChange(false)
                                } else {
                                    collapsedEffectPanel = !collapsedEffectPanel
                                }
                            },
                            portraitButton = if (traceState.portraitAvailable) {
                                {
                                    ColorCardTracePortraitModeButton(
                                        selected = portraitPanelExpanded,
                                        primaryText = primaryText,
                                        secondaryText = secondaryText,
                                        activeBackground = activeBackgroundForModeSelector(isDark),
                                        containerColor = containerColorForModeSelector(isDark),
                                        borderColor = borderColorForModeSelector(isDark),
                                        onClick = {
                                            if (portraitPanelExpanded) {
                                                onPortraitPanelExpandedChange(false)
                                            } else {
                                                collapsedEffectPanel = false
                                                onPortraitPanelExpandedChange(true)
                                            }
                                        }
                                    )
                                }
                            } else {
                                null
                            },
                            onModeChange = onPanelModeChange,
                            traceCopy = traceCopy
                        )
                        AnimatedVisibility(
                            visible = portraitPanelExpanded && traceState.portraitAvailable,
                            enter = expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 120)),
                            exit = shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 100)),
                            label = "colorCardTracePortraitPanel"
                        ) {
                            ColorCardTracePortraitPanel(
                                adjustments = traceState.portraitAdjustments,
                                faceCount = traceState.faceRegions.size,
                                isDark = isDark,
                                panelShape = panelShape,
                                backgroundColor = backgroundColor,
                                borderColor = borderColor,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onVitalityChange = traceState.updatePortraitVitality,
                                onSoftnessChange = traceState.updatePortraitSoftness,
                                onDetailSuppressionChange = traceState.updatePortraitDetailSuppression,
                                onReset = traceState.resetPortraitAdjustments,
                                onConfirm = {
                                    onPortraitPanelExpandedChange(false)
                                }
                            )
                        }
                        AnimatedVisibility(
                            visible = !collapsedEffectPanel && !portraitPanelExpanded,
                            enter = expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 110)),
                            exit = shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 100)),
                            label = "colorCardTraceEffectPanelBody"
                        ) {
                            AnimatedContent(
                                targetState = panelMode,
                                transitionSpec = {
                                    if (targetState.ordinal >= initialState.ordinal) {
                                        (slideInHorizontally { fullWidth -> fullWidth } + fadeIn())
                                            .togetherWith(slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
                                    } else {
                                        (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn())
                                            .togetherWith(slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
                                    }
                                },
                                label = "colorCardTraceFooterPanel"
                            ) { currentMode ->
                                when (currentMode) {
                                    ColorCardTracePanelMode.Picker -> Unit
                                    ColorCardTracePanelMode.Filter -> ColorCardTraceLutPanel(
                                        traceState = traceState,
                                        isDark = isDark,
                                        panelShape = panelShape,
                                        backgroundColor = backgroundColor,
                                        borderColor = borderColor,
                                        primaryText = primaryText,
                                        secondaryText = secondaryText,
                                        saveImageState = saveImageState,
                                        comparePressed = comparePressed,
                                        onSaveRecipe = onSaveRecipe,
                                        onComparePressedChange = onComparePressedChange,
                                        onExpandBrowser = {
                                            if (traceState.candidates.isNotEmpty()) {
                                                expandedFilterPicker = true
                                            }
                                        },
                                        onToggleFilmGrain = { enabled ->
                                            traceState.setFilmGrainEnabled(enabled)
                                            if (!enabled) {
                                                expandedFilmGrainPicker = false
                                            }
                                        },
                                        onOpenFilmGrainBrowser = {
                                            if (traceState.filmGrainEnabled && traceState.filmGrainAssets.isNotEmpty()) {
                                                traceState.refreshFilmGrainAsset()
                                            }
                                        },
                                        onSaveImage = onSaveImage
                                    )
                                    ColorCardTracePanelMode.Trace -> ColorCardTraceTracePanel(
                                        traceState = traceState,
                                        isDark = isDark,
                                        panelShape = panelShape,
                                        backgroundColor = backgroundColor,
                                        borderColor = borderColor,
                                        primaryText = primaryText,
                                        secondaryText = secondaryText,
                                        saveImageState = saveImageState,
                                        comparePressed = comparePressed,
                                        onSaveRecipe = onSaveRecipe,
                                        onComparePressedChange = onComparePressedChange,
                                        onExpandBrowser = {
                                            if (traceState.candidates.isNotEmpty()) {
                                                expandedFilterPicker = true
                                            }
                                        },
                                        onSaveImage = onSaveImage
                                    )
                                    ColorCardTracePanelMode.Imitate -> ColorCardTraceImitatePanel(
                                        traceState = traceState,
                                        isDark = isDark,
                                        panelShape = panelShape,
                                        backgroundColor = backgroundColor,
                                        borderColor = borderColor,
                                        primaryText = primaryText,
                                        secondaryText = secondaryText,
                                        saveImageState = saveImageState,
                                        comparePressed = comparePressed,
                                        referencePreviewUri = imitateReferencePreviewUri,
                                        referenceDisplayName = imitateReferenceDisplayName,
                                        referenceColors = imitateReferenceColors,
                                        referenceLoading = imitateReferenceLoading,
                                        onPickReference = onPickImitateReference,
                                        onClearReference = onClearImitateReference,
                                        onComparePressedChange = onComparePressedChange,
                                        onSaveImage = onSaveImage
                                    )
                                    ColorCardTracePanelMode.Recipe -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expandedFilterPicker &&
                (
                    effectPanelMode == ColorCardTracePanelMode.Filter ||
                        effectPanelMode == ColorCardTracePanelMode.Trace
                    ),
            enter = fadeIn(tween(durationMillis = 220)) + slideInVertically(
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) { fullHeight -> fullHeight },
            exit = fadeOut(tween(durationMillis = 180)) + slideOutVertically(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ) { fullHeight -> fullHeight },
            label = "colorCardTraceExpandedFilterBrowser"
        ) {
            ColorCardTraceExpandedFilterBrowser(
                traceState = traceState,
                panelMode = effectPanelMode,
                recentFilterIds = recentFilterIds,
                favoriteFilterIds = favoriteFilterIds,
                previewBitmaps = builtinPreviewBitmaps,
                isDark = isDark,
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                primaryText = primaryText,
                secondaryText = secondaryText,
                footerAlpha = visuals.footerContentAlpha,
                onOpenPreview = if (effectPanelMode == ColorCardTracePanelMode.Filter) {
                    {
                        expandedFilterPicker = false
                        showFilterPreviewPage = true
                    }
                } else {
                    null
                },
                onDismiss = { expandedFilterPicker = false },
                onCandidateSelected = { paletteId ->
                    if (effectPanelMode == ColorCardTracePanelMode.Filter) {
                        recentFilterUsageStore.recordExpandedPickerSelection(paletteId)
                    }
                    traceState.selectCandidate(paletteId)
                    expandedFilterPicker = false
                },
                onCandidateFavoriteToggle = { paletteId ->
                    favoriteFilterStore.toggle(paletteId)
                }
            )
        }

        if (showFilterPreviewPage && effectPanelMode == ColorCardTracePanelMode.Filter) {
            Dialog(
                onDismissRequest = {
                    showFilterPreviewPage = false
                    expandedFilterPicker = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                ColorCardFilterPreviewPage(
                    onBack = {
                        showFilterPreviewPage = false
                        expandedFilterPicker = false
                    },
                    onTryFilter = { paletteId ->
                        recentFilterUsageStore.recordExpandedPickerSelection(paletteId)
                        traceState.selectCandidate(paletteId)
                        showFilterPreviewPage = false
                        expandedFilterPicker = false
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = expandedFilmGrainPicker &&
                effectPanelMode == ColorCardTracePanelMode.Filter &&
                traceState.filmGrainEnabled &&
                traceState.filmGrainEligible,
            enter = fadeIn(tween(durationMillis = 220)) + slideInVertically(
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) { fullHeight -> fullHeight },
            exit = fadeOut(tween(durationMillis = 180)) + slideOutVertically(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ) { fullHeight -> fullHeight },
            label = "colorCardTraceExpandedFilmGrainBrowser"
        ) {
            ColorCardTraceExpandedFilmGrainBrowser(
                assets = traceState.filmGrainAssets,
                selectedAssetId = traceState.selectedFilmGrainAsset?.id,
                previewBitmaps = filmGrainPreviewBitmaps,
                isDark = isDark,
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                primaryText = primaryText,
                footerAlpha = visuals.footerContentAlpha,
                onDismiss = { expandedFilmGrainPicker = false },
                onAssetSelected = { assetId ->
                    traceState.selectFilmGrainAsset(assetId)
                    expandedFilmGrainPicker = false
                }
            )
        }
    }
}

@Composable
private fun ColorCardTraceModeSelector(
    panelMode: ColorCardTracePanelMode,
    isDark: Boolean,
    collapsed: Boolean,
    onToggleCollapsed: (() -> Unit)?,
    portraitButton: (@Composable () -> Unit)?,
    onModeChange: (ColorCardTracePanelMode) -> Unit,
    traceCopy: ColorCardTraceCopy
) {
    val containerColor = containerColorForModeSelector(isDark)
    val borderColor = borderColorForModeSelector(isDark)
    val activeBackground = activeBackgroundForModeSelector(isDark)
    val primaryText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF171717)
    val secondaryText = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
    } else {
        Color(0xFF666666)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        onToggleCollapsed?.let { toggle ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable(onClick = toggle)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (collapsed) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (collapsed) "展开面板" else "收起面板",
                    tint = primaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        portraitButton?.invoke()
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(containerColor)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 3.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf(
                ColorCardTracePanelMode.Picker to traceCopy.panelModePicker,
                ColorCardTracePanelMode.Filter to traceCopy.panelModeFilter,
                ColorCardTracePanelMode.Trace to traceCopy.panelModeTrace,
                ColorCardTracePanelMode.Imitate to traceCopy.panelModeImitate
            ).forEach { (mode, label) ->
                val selected = mode == panelMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) activeBackground else Color.Transparent)
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) primaryText else secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun containerColorForModeSelector(isDark: Boolean): Color {
    return if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.84f)
    }
}

@Composable
private fun borderColorForModeSelector(isDark: Boolean): Color {
    return if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
}

private fun activeBackgroundForModeSelector(isDark: Boolean): Color {
    return if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
}

@Composable
private fun ColorCardTracePickerPanel(
    colorPickerState: HomeCardColorPickerState,
    pickerColors: List<Int>,
    isDark: Boolean,
    panelShape: RoundedCornerShape,
    backgroundColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color
) {
    Column(
        modifier = Modifier
            .clip(panelShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, panelShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {})
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "\u53d6\u8272\u9762\u677f",
                modifier = Modifier.weight(1f),
                color = primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(10.dp))
            ColorCardTraceFilterCountStepper(
                pointCount = colorPickerState.pointCount,
                canAddPoint = colorPickerState.canAddPoint,
                canRemovePoint = colorPickerState.canRemovePoint,
                onAddPoint = colorPickerState.addPoint,
                onRemovePoint = colorPickerState.removePoint,
                contentColor = primaryText,
                secondaryText = secondaryText,
                isDark = isDark
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = colorPickerState.refresh,
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
                onClick = colorPickerState.save,
                enabled = colorPickerState.isReady,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = "\u4fdd\u5b58\u8272\u5361",
                    tint = if (colorPickerState.isReady) primaryText else secondaryText,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HomeCardColorPickerPreciseModeRow(
            isPreciseMode = colorPickerState.isPreciseMode,
            onPreciseModeChange = colorPickerState.setPreciseMode,
            primaryText = primaryText,
            secondaryText = secondaryText
        )

        Spacer(modifier = Modifier.height(10.dp))

        ColorCardTracePaletteStrip(
            colors = pickerColors,
            isDark = isDark
        )
    }
}

@Composable
internal fun ColorCardTraceLutPanel(
    traceState: ColorCardTraceFilterState,
    isDark: Boolean,
    panelShape: RoundedCornerShape,
    backgroundColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    saveImageState: ColorCardTraceSaveImageState,
    comparePressed: Boolean,
    onSaveRecipe: () -> Unit,
    onComparePressedChange: (Boolean) -> Unit,
    onExpandBrowser: () -> Unit,
    onToggleFilmGrain: (Boolean) -> Unit,
    onOpenFilmGrainBrowser: () -> Unit,
    onSaveImage: () -> Unit,
    showSaveRecipeAction: Boolean = false,
    showSaveImageAction: Boolean = true,
    adjustmentPadContinuousInput: Boolean = false,
    adjustmentPadUseFullArea: Boolean = false,
    showTripleSliders: Boolean = true,
    modifier: Modifier = Modifier
) {
    val secondaryTitle = colorCardTraceFilterSecondaryTitle(traceState, ColorCardTracePanelMode.Filter)
    val showFilmGrainControls = traceState.filmGrainEligible && traceState.filmGrainAssets.isNotEmpty()
    val showAiTitleCapsule = isColorCardTraceAiMnnCandidate(traceState.selectedCandidate)
    val showMotionPhotoPatternControls =
        traceState.selectedCandidate?.kind == ColorCardTraceFilterKind.MotionPhotoEntry
    Column(
        modifier = modifier
            .clip(panelShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, panelShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {})
            }
            .then(
                if (showAiTitleCapsule) {
                    Modifier.heightIn(min = COLOR_CARD_TRACE_AI_FILTER_PANEL_MIN_HEIGHT)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = colorCardTraceFilterPrimaryTitle(traceState, ColorCardTracePanelMode.Filter),
                        color = primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showAiTitleCapsule) {
                        Box(
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
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AI",
                                style = TextStyle(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF9966),
                                            Color(0xFFFF5E62),
                                            Color(0xFFFFC371)
                                        )
                                    )
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (secondaryTitle.isNotBlank()) {
                    Text(
                        text = secondaryTitle,
                        color = secondaryText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            val switcherCandidates = remember(traceState.mode, traceState.candidates) {
                when (traceState.mode) {
                    ColorCardTracePanelMode.Filter ->
                        resolveColorCardTraceBuiltinSwitcherCandidates(traceState.candidates)
                    else -> traceState.candidates
                }
            }
            ColorCardTraceIconActionButton(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "重置调整",
                enabled = true,
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                onClick = traceState.resetManualAdjustments
            )
            Spacer(modifier = Modifier.width(6.dp))
            ColorCardTraceFilterSwitcher(
                candidateCount = switcherCandidates.size,
                selectedIndex = switcherCandidates.indexOfFirst {
                    it.paletteId == traceState.selectedCandidate?.paletteId
                }.coerceAtLeast(0),
                onPrevious = { traceState.stepFilterBy(-1) },
                onNext = { traceState.stepFilterBy(1) },
                contentColor = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                showIndexText = false
            )
            Spacer(modifier = Modifier.width(4.dp))
            ColorCardTraceTextActionChip(
                label = "展开",
                enabled = traceState.candidates.isNotEmpty(),
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                onClick = onExpandBrowser
            )
            if (showSaveImageAction) {
                Spacer(modifier = Modifier.width(4.dp))
                ColorCardTraceCompareButton(
                    pressed = comparePressed,
                    enabled = traceState.previewSourceBitmap?.isRecycled == false,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    contentDescription = if (comparePressed) "正在查看原图" else "按住查看原图",
                    onPressedChange = onComparePressedChange
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showMotionPhotoPatternControls) {
            MotionPhotoRevealPatternInlinePanel(
                selectedPattern = traceState.motionPhotoRevealPattern,
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                onPatternSelected = traceState.updateMotionPhotoRevealPattern
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (traceState.allowsManualAdjustment) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (showTripleSliders) {
                    ColorCardTraceTripleSliderGroup(
                        traceState = traceState,
                        paletteColors = traceState.displayColors,
                        isDark = isDark,
                        sliderHeight = 112.dp
                    )
                }
                ColorCardTraceAdjustmentPad(
                    color = traceState.color,
                    tone = traceState.tone,
                    colorAxis = traceState.colorAxis,
                    onAdjust = traceState.updateAdjustment,
                    isDark = isDark,
                    continuousInput = adjustmentPadContinuousInput,
                    useFullArea = adjustmentPadUseFullArea,
                    modifier = Modifier
                        .weight(1f)
                        .height(112.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (traceState.allowsStrengthAdjustment) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showFilmGrainControls) {
                    ColorCardTraceFilmGrainInlineControls(
                        enabled = traceState.filmGrainEnabled,
                        selectedLabel = traceState.selectedFilmGrainAsset?.title,
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onToggle = onToggleFilmGrain,
                        onRefresh = onOpenFilmGrainBrowser
                    )
                }
                ColorCardTraceStrengthSlider(
                    value = traceState.paletteStrength,
                    onValueChange = traceState.updatePaletteStrength,
                    isDark = isDark,
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorCardTraceTracePanel(
    traceState: ColorCardTraceFilterState,
    isDark: Boolean,
    panelShape: RoundedCornerShape,
    backgroundColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    saveImageState: ColorCardTraceSaveImageState,
    comparePressed: Boolean,
    onSaveRecipe: () -> Unit,
    onComparePressedChange: (Boolean) -> Unit,
    onExpandBrowser: () -> Unit,
    onSaveImage: () -> Unit
) {
    var showEmptyPaletteHelp by remember { mutableStateOf(false) }
    val appLanguage = LocalAppLanguage.current
    val traceCopy = remember(appLanguage) { getColorCardTraceCopy(appLanguage) }
    val isTracePaletteUnavailable = traceState.candidates.isEmpty() && !traceState.isAnalyzing
    val secondaryTitle = colorCardTraceFilterSecondaryTitle(traceState, ColorCardTracePanelMode.Trace)
    Column(
        modifier = Modifier
            .clip(panelShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, panelShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {})
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = colorCardTraceFilterPrimaryTitle(traceState, ColorCardTracePanelMode.Trace),
                        color = primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isTracePaletteUnavailable) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = "追色说明",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { showEmptyPaletteHelp = true }
                        )
                    }
                }
                if (secondaryTitle.isNotBlank()) {
                    Text(
                        text = secondaryTitle,
                        color = secondaryText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            val switcherCandidates = remember(traceState.mode, traceState.candidates) {
                when (traceState.mode) {
                    ColorCardTracePanelMode.Filter ->
                        resolveColorCardTraceBuiltinSwitcherCandidates(traceState.candidates)
                    else -> traceState.candidates
                }
            }
            ColorCardTraceFilterSwitcher(
                candidateCount = switcherCandidates.size,
                selectedIndex = switcherCandidates.indexOfFirst {
                    it.paletteId == traceState.selectedCandidate?.paletteId
                }.coerceAtLeast(0),
                onPrevious = { traceState.stepFilterBy(-1) },
                onNext = { traceState.stepFilterBy(1) },
                contentColor = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                showIndexText = false
            )
            Spacer(modifier = Modifier.width(4.dp))
            ColorCardTraceTextActionChip(
                label = "展开",
                enabled = traceState.candidates.isNotEmpty(),
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                onClick = onExpandBrowser
            )
            Spacer(modifier = Modifier.width(4.dp))
            ColorCardTraceCompareButton(
                pressed = comparePressed,
                enabled = traceState.previewSourceBitmap?.isRecycled == false,
                primaryText = primaryText,
                secondaryText = secondaryText,
                contentDescription = if (comparePressed) "正在查看原图" else "按住查看原图",
                onPressedChange = onComparePressedChange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ColorCardTracePaletteStrip(
            colors = traceState.displayColors,
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            ColorCardTraceTripleSliderGroup(
                traceState = traceState,
                paletteColors = traceState.displayColors,
                isDark = isDark,
                sliderHeight = 112.dp
            )
            ColorCardTraceAdjustmentPad(
                color = traceState.color,
                tone = traceState.tone,
                colorAxis = traceState.colorAxis,
                onAdjust = traceState.updateAdjustment,
                isDark = isDark,
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ColorCardTraceStrengthSlider(
            value = traceState.paletteStrength,
            onValueChange = traceState.updatePaletteStrength,
            isDark = isDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        )
    }

    if (showEmptyPaletteHelp) {
        AlertDialog(
            onDismissRequest = { showEmptyPaletteHelp = false },
            title = { Text(traceCopy.traceHelpTitle) },
            text = {
                Text(
                    text = traceCopy.traceHelpMessage,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showEmptyPaletteHelp = false }) {
                    Text(traceCopy.gotIt)
                }
            }
        )
    }
}

@Composable
private fun ColorCardTraceRecipePanel(
    recipes: List<SavedColorCardRecipe>,
    isDark: Boolean,
    panelShape: RoundedCornerShape,
    backgroundColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onApplyRecipe: (SavedColorCardRecipe) -> Unit,
    onShareRecipe: (SavedColorCardRecipe) -> Unit,
    onDeleteRecipe: (SavedColorCardRecipe) -> Unit,
    onImportRecipes: () -> Unit,
    onExportAllRecipes: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(panelShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, panelShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {})
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "配方",
                modifier = Modifier.weight(1f),
                color = primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            ColorCardTraceTextActionChip(
                label = "导入",
                enabled = true,
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                onClick = onImportRecipes
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有保存过配方",
                    color = secondaryText,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    ColorCardTraceRecipeRow(
                        recipe = recipe,
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onApply = { onApplyRecipe(recipe) },
                        onShare = { onShareRecipe(recipe) },
                        onDelete = { onDeleteRecipe(recipe) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCardTraceRecipeRow(
    recipe: SavedColorCardRecipe,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val rowShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = recipe.title,
                    color = primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (recipe.mode == ColorCardRecipeMode.Filter) "滤镜" else "追色")
                        recipe.sourceTag?.takeIf { it.isNotBlank() }?.let {
                            append(" · ")
                            append(it)
                        }
                    },
                    color = secondaryText,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            ColorCardTraceTextActionChip(
                label = "应用",
                enabled = true,
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark,
                onClick = onApply
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "分享配方",
                    tint = primaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除配方",
                    tint = secondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        ColorCardTracePaletteStrip(
            colors = recipe.colors,
            isDark = isDark,
            modifier = Modifier.height(18.dp)
        )
    }
}

@Composable
internal fun rememberColorCardTraceBuiltinPreviewBitmaps(
    enabled: Boolean,
    candidates: List<ColorCardTraceFilterCandidate>
): Map<String, Bitmap> {
    if (!enabled || candidates.isEmpty()) return emptyMap()
    val context = LocalContext.current
    val feedPreviewDemoAssetNames = rememberColorCardTracePreviewFeedDemoAssetNames()
    val bootstrapAssets = rememberColorCardFilterFeedBootstrapPreviewAssets()
    val assignedFeedAssetNamesByPaletteId = remember(
        candidates,
        feedPreviewDemoAssetNames,
        bootstrapAssets
    ) {
        resolveColorCardFilterFeedAssignedDemoAssetNames(
            candidates = candidates,
            demoAssetNames = feedPreviewDemoAssetNames,
            bootstrapAssets = bootstrapAssets
        )
    }
    val sharedFeedPreviewRequests = remember(candidates, assignedFeedAssetNamesByPaletteId) {
        candidates.mapNotNull { candidate ->
            val demoAssetName = assignedFeedAssetNamesByPaletteId[candidate.paletteId]
                ?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            ColorCardFilterFeedPreviewRequest(
                previewKey = buildColorCardFilterFeedPreviewKey(candidate, demoAssetName),
                candidate = candidate,
                demoAssetName = demoAssetName,
                highFidelity = true,
                allowApproximate = true,
                preferStylizedPreview = false,
                runtimeGenerationEnabled = true
            )
        }
    }
    val sharedFeedPreviewBitmaps = rememberColorCardFilterFeedPreviewBitmaps(sharedFeedPreviewRequests)
    val reusedPreviewBitmaps = remember(sharedFeedPreviewRequests, sharedFeedPreviewBitmaps) {
        buildMap {
            sharedFeedPreviewRequests.forEach { request ->
                sharedFeedPreviewBitmaps[request.previewKey]
                    ?.takeUnless(Bitmap::isRecycled)
                    ?.let { bitmap ->
                        put(request.candidate.paletteId, bitmap)
                    }
            }
        }
    }
    if (sharedFeedPreviewRequests.isNotEmpty()) {
        return reusedPreviewBitmaps
    }
    val candidateIds = remember(candidates) {
        candidates.map(ColorCardTraceFilterCandidate::paletteId).toSet()
    }
    val previewBitmaps = remember(candidateIds) {
        mutableStateMapOf<String, Bitmap>().apply {
            putAll(snapshotColorCardTraceBuiltinPreviewBitmapCache(candidateIds))
        }
    }

    LaunchedEffect(enabled, candidates, candidateIds, feedPreviewDemoAssetNames) {
        previewBitmaps.putAll(snapshotColorCardTraceBuiltinPreviewBitmapCache(candidateIds))
        val missingFromMemory = candidates.filter { candidate ->
            previewBitmaps[candidate.paletteId]?.isRecycled != false
        }
        if (missingFromMemory.isNotEmpty()) {
            val diskCached = loadColorCardTraceBuiltinPreviewBitmapsFromDisk(
                context = context.applicationContext,
                candidates = missingFromMemory
            )
            if (diskCached.isNotEmpty()) {
                previewBitmaps.putAll(diskCached)
                cacheColorCardTraceBuiltinPreviewBitmaps(diskCached)
            }
        }
        val missingCandidates = candidates.filter { candidate ->
            previewBitmaps[candidate.paletteId]?.isRecycled != false
        }
        if (missingCandidates.isNotEmpty()) {
            missingCandidates
                .chunked(COLOR_CARD_TRACE_BUILTIN_PREVIEW_BATCH_SIZE)
                .forEachIndexed { batchIndex, candidateBatch ->
                    val generated = buildColorCardTraceBuiltinPreviewBitmaps(
                        context = context.applicationContext,
                        candidates = candidateBatch,
                        feedDemoAssetNames = feedPreviewDemoAssetNames,
                        assignmentOffset = batchIndex * COLOR_CARD_TRACE_BUILTIN_PREVIEW_BATCH_SIZE
                    )
                    if (generated.isEmpty()) return@forEachIndexed
                    generated.forEach { (paletteId, bitmap) ->
                        previewBitmaps[paletteId]
                            ?.takeIf { existing -> existing !== bitmap && !existing.isRecycled }
                            ?.recycle()
                        previewBitmaps[paletteId] = bitmap
                    }
                    cacheColorCardTraceBuiltinPreviewBitmaps(generated)
                    persistColorCardTraceBuiltinPreviewBitmaps(
                        context = context.applicationContext,
                        generated = generated
                    )
                    yield()
                }
        }

        val feedRefreshSignature = feedPreviewDemoAssetNames
            .takeIf { it.isNotEmpty() }
            ?.let { demoAssetNames ->
                "feed:${candidateIds.hashCode()}:${demoAssetNames.hashCode()}"
            }
        val shouldForceFeedRefresh = feedRefreshSignature?.let { signature ->
            synchronized(colorCardTraceBuiltinPreviewBitmapCacheLock) {
                if (colorCardTraceBuiltinPreviewLastFeedRefreshSignature == signature) {
                    false
                } else {
                    colorCardTraceBuiltinPreviewLastFeedRefreshSignature = signature
                    true
                }
            }
        } == true
        if (!shouldForceFeedRefresh) return@LaunchedEffect

        candidates
            .chunked(COLOR_CARD_TRACE_BUILTIN_PREVIEW_BATCH_SIZE)
            .forEachIndexed { batchIndex, candidateBatch ->
                val regenerated = buildColorCardTraceBuiltinPreviewBitmaps(
                    context = context.applicationContext,
                    candidates = candidateBatch,
                    feedDemoAssetNames = feedPreviewDemoAssetNames,
                    assignmentOffset = batchIndex * COLOR_CARD_TRACE_BUILTIN_PREVIEW_BATCH_SIZE
                )
                if (regenerated.isEmpty()) return@forEachIndexed
                regenerated.forEach { (paletteId, bitmap) ->
                    previewBitmaps[paletteId]
                        ?.takeIf { existing -> existing !== bitmap && !existing.isRecycled }
                        ?.recycle()
                    previewBitmaps[paletteId] = bitmap
                }
                cacheColorCardTraceBuiltinPreviewBitmaps(regenerated)
                persistColorCardTraceBuiltinPreviewBitmaps(
                    context = context.applicationContext,
                    generated = regenerated
                )
                yield()
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmaps.clear()
        }
    }
    return previewBitmaps
}

@Composable
internal fun rememberColorCardTracePreviewFeedDemoAssetNames(): List<String> {
    val context = LocalContext.current
    val feedDemoAssets = rememberColorCardFilterFeedDemoAssets()
    val manifestDemoAssetNames = remember(feedDemoAssets) {
        colorCardTracePreviewFeedDemoAssetNames(feedDemoAssets)
    }
    val listedDemoAssetNames by produceState(
        initialValue = emptyList<String>(),
        key1 = context
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.applicationContext.assets
                    .list("color_card_filter_feed_demo")
                    ?.map { name -> "color_card_filter_feed_demo/$name" }
                    .orEmpty()
            }.getOrDefault(emptyList())
                .filterNot { assetName ->
                    isColorCardTracePreviewFeedDemoExcluded(
                        demoAssetId = null,
                        assetName = assetName
                    )
                }
                .distinct()
        }
    }
    return remember(manifestDemoAssetNames, listedDemoAssetNames) {
        (manifestDemoAssetNames + listedDemoAssetNames)
            .filter(String::isNotBlank)
            .distinct()
    }
}

private fun colorCardTracePreviewFeedDemoAssetNames(
    demoAssets: List<ColorCardFilterFeedDemoAsset>
): List<String> {
    return demoAssets
        .filterNot { demoAsset ->
            isColorCardTracePreviewFeedDemoExcluded(
                demoAssetId = demoAsset.id,
                assetName = demoAsset.assetName
            )
        }
        .map(ColorCardFilterFeedDemoAsset::assetName)
        .filter(String::isNotBlank)
        .distinct()
}

private fun isColorCardTracePreviewFeedDemoExcluded(
    demoAssetId: String?,
    assetName: String
): Boolean {
    val normalizedAssetName = assetName.trim()
    if (normalizedAssetName.isBlank()) return true
    val fileName = normalizedAssetName.substringAfterLast('/')
    val inferredId = fileName.substringBeforeLast('.', fileName)
    val normalizedId = demoAssetId
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: inferredId
    return normalizedId in COLOR_CARD_TRACE_FILTER_PREVIEW_EXCLUDED_FEED_DEMO_IDS ||
        normalizedId.contains("cat", ignoreCase = true) ||
        inferredId.contains("cat", ignoreCase = true) ||
        normalizedAssetName.contains("cat", ignoreCase = true) ||
        normalizedAssetName.contains("watermark_template_demo", ignoreCase = true)
}

@Composable
private fun rememberColorCardTraceFilmGrainPreviewBitmaps(
    enabled: Boolean,
    assets: List<ColorCardTraceFilmGrainAsset>
): Map<String, Bitmap> {
    val context = LocalContext.current
    val feedPreviewDemoAssetNames = rememberColorCardTracePreviewFeedDemoAssetNames()
    val previewBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(enabled, assets, feedPreviewDemoAssetNames) {
        if (!enabled || assets.isEmpty()) return@LaunchedEffect
        val missingAssets = assets.filter { asset ->
            previewBitmaps[asset.id]?.isRecycled != false
        }
        if (missingAssets.isEmpty()) return@LaunchedEffect
        val appContext = context.applicationContext
        val feedSourceBitmaps = if (feedPreviewDemoAssetNames.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                buildMap<String, Bitmap> {
                    feedPreviewDemoAssetNames.forEach { assetName ->
                        decodeColorCardFilterFeedDemoBitmap(
                            context = appContext,
                            assetName = assetName,
                            maxSidePx = COLOR_CARD_TRACE_FILTER_PREVIEW_MAX_SIDE
                        )?.let { bitmap ->
                            put(assetName, bitmap)
                        }
                    }
                }
            }
        } else {
            emptyMap()
        }
        val feedSourceEntries = feedSourceBitmaps.entries.toList()
        val fallbackSampleBitmap = if (feedSourceEntries.isEmpty()) {
            decodeColorCardTraceFilterPreviewSampleBitmap(
                context = appContext,
                maxSide = COLOR_CARD_TRACE_FILTER_PREVIEW_MAX_SIDE
            )
        } else {
            null
        }
        if (feedSourceEntries.isEmpty() && fallbackSampleBitmap == null) return@LaunchedEffect
        val sourceOrderByAssetId = assets
            .mapIndexed { index, asset -> asset.id to index }
            .toMap()
        try {
            val generated = withContext(Dispatchers.Default) {
                buildMap {
                    missingAssets.forEachIndexed { index, asset ->
                        val sourceBitmap = if (feedSourceEntries.isNotEmpty()) {
                            val sourceIndexSeed = sourceOrderByAssetId[asset.id] ?: index
                            val sourceIndex = ((sourceIndexSeed % feedSourceEntries.size) + feedSourceEntries.size) %
                                feedSourceEntries.size
                            feedSourceEntries[sourceIndex].value
                        } else {
                            fallbackSampleBitmap ?: return@forEachIndexed
                        }
                        renderColorCardTraceFilmGrainPreviewBitmap(
                            context = appContext,
                            asset = asset,
                            sampleBitmap = sourceBitmap
                        )?.let { bitmap ->
                            put(asset.id, bitmap)
                        }
                    }
                }
            }
            generated.forEach { (assetId, bitmap) ->
                previewBitmaps[assetId]
                    ?.takeIf { existing -> existing !== bitmap && !existing.isRecycled }
                    ?.recycle()
                previewBitmaps[assetId] = bitmap
            }
        } finally {
            fallbackSampleBitmap
                ?.takeIf { !it.isRecycled }
                ?.recycle()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmaps.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            previewBitmaps.clear()
        }
    }
    return previewBitmaps
}

@Composable
internal fun ColorCardTraceExpandedFilterBrowser(
    traceState: ColorCardTraceFilterState,
    panelMode: ColorCardTracePanelMode,
    recentFilterIds: List<String>,
    favoriteFilterIds: List<String>,
    previewBitmaps: Map<String, Bitmap>,
    isDark: Boolean,
    backgroundColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    footerAlpha: Float,
    onOpenPreview: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onCandidateSelected: (String) -> Unit,
    onCandidateFavoriteToggle: (String) -> Unit
) {
    val density = LocalDensity.current
    val navigationBarBottomPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val panelBottomPadding = 18.dp + navigationBarBottomPadding
    val gridBottomPadding = 16.dp + navigationBarBottomPadding
    val context = LocalContext.current
    val tabStore = remember(context) { ColorCardFilterBrowserTabStore.get(context) }
    val persistedTabKey by tabStore.selectedTabKey.collectAsState()
    val favoriteCandidates = remember(panelMode, favoriteFilterIds, traceState.candidates) {
        if (panelMode != ColorCardTracePanelMode.Filter) {
            emptyList()
        } else {
            val candidatesById = traceState.candidates.associateBy(ColorCardTraceFilterCandidate::paletteId)
            favoriteFilterIds
                .mapNotNull(candidatesById::get)
                .filterNot(::isColorCardTraceAiMnnCandidate)
        }
    }
    val recentCandidates = remember(panelMode, recentFilterIds, traceState.candidates) {
        if (panelMode != ColorCardTracePanelMode.Filter) {
            emptyList()
        } else {
            val candidatesById = traceState.candidates.associateBy(ColorCardTraceFilterCandidate::paletteId)
            recentFilterIds
                .mapNotNull(candidatesById::get)
                .filterNot(::isColorCardTraceAiMnnCandidate)
                .take(5)
        }
    }
    val filterSections = remember(traceState.candidates) {
        if (traceState.candidates.isEmpty()) {
            emptyList()
        } else {
            buildColorCardTraceExpandedFilterSections(traceState.candidates)
        }
    }
    val filterTabs = remember(panelMode, filterSections) {
        if (panelMode != ColorCardTracePanelMode.Filter) {
            emptyList()
        } else {
            buildList {
                add(ColorCardTraceFilterBrowserTab(key = COLOR_CARD_FILTER_TAB_COMMON, title = "常用"))
                addAll(filterSections.map { section ->
                    ColorCardTraceFilterBrowserTab(key = section.key, title = section.title)
                })
            }
        }
    }
    val hasCommonTabContent = favoriteCandidates.isNotEmpty() || recentCandidates.isNotEmpty()
    val normalizedPersistedTabKey = remember(persistedTabKey) {
        normalizeColorCardTraceBrowserTabKey(persistedTabKey)
    }
    var selectedTabKey by remember(panelMode, filterTabs, hasCommonTabContent, normalizedPersistedTabKey) {
        mutableStateOf(
            resolveColorCardTraceExpandedBrowserTabKey(
                panelMode = panelMode,
                filterTabs = filterTabs,
                currentTabKey = "",
                persistedTabKey = normalizedPersistedTabKey,
                hasCommonTabContent = hasCommonTabContent
            )
        )
    }
    LaunchedEffect(panelMode, normalizedPersistedTabKey, filterTabs, hasCommonTabContent) {
        val resolved = resolveColorCardTraceExpandedBrowserTabKey(
            panelMode = panelMode,
            filterTabs = filterTabs,
            currentTabKey = selectedTabKey,
            persistedTabKey = normalizedPersistedTabKey,
            hasCommonTabContent = hasCommonTabContent
        )
        if (resolved != selectedTabKey) {
            selectedTabKey = resolved
        }
    }
    LaunchedEffect(panelMode, selectedTabKey) {
        if (panelMode == ColorCardTracePanelMode.Filter && selectedTabKey.isNotBlank()) {
            tabStore.setSelectedTabKey(selectedTabKey)
        }
    }
    val selectedSection = remember(selectedTabKey, filterSections) {
        filterSections.firstOrNull { it.key == selectedTabKey }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = panelBottomPadding)
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .fillMaxHeight(0.69f)
                .graphicsLayer {
                    alpha = footerAlpha
                }
                .clip(RoundedCornerShape(32.dp))
                .background(backgroundColor)
                .border(1.dp, borderColor, RoundedCornerShape(32.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                }
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (panelMode == ColorCardTracePanelMode.Trace) "选择色卡" else "选择滤镜",
                        color = primaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                onOpenPreview?.let { openPreview ->
                    ColorCardTraceTextActionChip(
                        label = "查看预览",
                        enabled = true,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        isDark = isDark,
                        onClick = openPreview
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                ColorCardTraceTextActionChip(
                    label = "收起",
                    enabled = true,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    isDark = isDark,
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (traceState.candidates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (panelMode == ColorCardTracePanelMode.Trace) "暂无可用色卡" else "暂无可用滤镜",
                        color = secondaryText,
                        fontSize = 13.sp
                    )
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val gridColumns = when (panelMode) {
                        ColorCardTracePanelMode.Trace -> 3
                        else -> if (maxWidth >= 520.dp) 6 else 5
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (panelMode == ColorCardTracePanelMode.Filter && filterTabs.isNotEmpty()) {
                            ScrollableTabRow(
                                selectedTabIndex = filterTabs.indexOfFirst { it.key == selectedTabKey }.coerceAtLeast(0),
                                modifier = Modifier.fillMaxWidth(),
                                edgePadding = 0.dp,
                                divider = {},
                                containerColor = Color.Transparent,
                                contentColor = primaryText
                            ) {
                                filterTabs.forEach { tab ->
                                    val selected = tab.key == selectedTabKey
                                    Tab(
                                        selected = selected,
                                        onClick = { selectedTabKey = tab.key },
                                        text = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (selected) primaryText else secondaryText
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = gridBottomPadding),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (panelMode == ColorCardTracePanelMode.Filter && selectedTabKey == COLOR_CARD_FILTER_TAB_COMMON) {
                                if (favoriteCandidates.isNotEmpty()) {
                                    item(
                                        key = "favorite-filters",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        ColorCardTraceFavoriteFilterSection(
                                            candidates = favoriteCandidates,
                                            selectedPaletteId = traceState.selectedCandidate?.paletteId,
                                            favoritePaletteIds = favoriteFilterIds.toSet(),
                                            previewBitmaps = previewBitmaps,
                                            gridColumns = gridColumns,
                                            isDark = isDark,
                                            primaryText = primaryText,
                                            onCandidateSelected = onCandidateSelected,
                                            onCandidateFavoriteToggle = onCandidateFavoriteToggle
                                        )
                                    }
                                }
                                if (recentCandidates.isNotEmpty()) {
                                    item(
                                        key = "recent-filters",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        ColorCardTraceRecentFilterSection(
                                            candidates = recentCandidates,
                                            selectedPaletteId = traceState.selectedCandidate?.paletteId,
                                            favoritePaletteIds = favoriteFilterIds.toSet(),
                                            previewBitmaps = previewBitmaps,
                                            isDark = isDark,
                                            primaryText = primaryText,
                                            onCandidateSelected = onCandidateSelected,
                                            onCandidateFavoriteToggle = onCandidateFavoriteToggle
                                        )
                                    }
                                }
                            }
                            if (panelMode == ColorCardTracePanelMode.Trace) {
                                items(items = traceState.candidates, key = { it.paletteId }) { candidate ->
                                    ColorCardTraceExpandedTraceCard(
                                        candidate = candidate,
                                        selected = traceState.selectedCandidate?.paletteId == candidate.paletteId,
                                        isDark = isDark,
                                        primaryText = primaryText,
                                        secondaryText = secondaryText,
                                        onClick = { onCandidateSelected(candidate.paletteId) }
                                    )
                                }
                            } else if (selectedTabKey != COLOR_CARD_FILTER_TAB_COMMON) {
                                selectedSection?.candidates.orEmpty().let { sectionCandidates ->
                                    items(items = sectionCandidates, key = { it.paletteId }) { candidate ->
                                        ColorCardTraceExpandedFilterCard(
                                            candidate = candidate,
                                            previewBitmap = previewBitmaps[candidate.paletteId],
                                            selected = traceState.selectedCandidate?.paletteId == candidate.paletteId,
                                            favorited = favoriteFilterIds.contains(candidate.paletteId),
                                            isDark = isDark,
                                            primaryText = primaryText,
                                            onClick = { onCandidateSelected(candidate.paletteId) },
                                            onLongClick = { onCandidateFavoriteToggle(candidate.paletteId) }
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
}

@Composable
internal fun ColorCardTraceCameraFilterPanel(
    traceState: ColorCardTraceFilterState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val panelShape = RoundedCornerShape(24.dp)
    val backgroundColor = Color(0xFF121314).copy(alpha = 0.96f)
    val borderColor = Color.White.copy(alpha = 0.10f)
    val primaryText = Color.White.copy(alpha = 0.96f)
    val secondaryText = Color.White.copy(alpha = 0.56f)
    val previewBitmaps = rememberColorCardTraceBuiltinPreviewBitmaps(
        enabled = true,
        candidates = traceState.candidates
    )
    val recentFilterUsageStore = remember(context) { ColorCardRecentFilterUsageStore.get(context) }
    val favoriteFilterStore = remember(context) { ColorCardFavoriteFilterStore.get(context) }
    val recentFilterIds by recentFilterUsageStore.topFilterIds.collectAsState()
    val favoriteFilterIds by favoriteFilterStore.favoriteFilterIds.collectAsState()
    var expandedFilterPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.heightIn(min = COLOR_CARD_TRACE_CAMERA_FILTER_PANEL_STABLE_MIN_HEIGHT)
    ) {
        ColorCardTraceLutPanel(
            traceState = traceState,
            isDark = true,
            panelShape = panelShape,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            saveImageState = ColorCardTraceSaveImageState.Idle,
            comparePressed = false,
            onSaveRecipe = {},
            onComparePressedChange = {},
            onExpandBrowser = {
                if (traceState.candidates.isNotEmpty()) {
                    expandedFilterPicker = true
                }
            },
            onToggleFilmGrain = traceState.setFilmGrainEnabled,
            onOpenFilmGrainBrowser = traceState.refreshFilmGrainAsset,
            onSaveImage = {},
            showSaveRecipeAction = false,
            showSaveImageAction = false,
            adjustmentPadContinuousInput = true,
            adjustmentPadUseFullArea = true
        )

        if (expandedFilterPicker) {
            Dialog(
                onDismissRequest = { expandedFilterPicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                ColorCardTraceExpandedFilterBrowser(
                    traceState = traceState,
                    panelMode = ColorCardTracePanelMode.Filter,
                    recentFilterIds = recentFilterIds,
                    favoriteFilterIds = favoriteFilterIds,
                    previewBitmaps = previewBitmaps,
                    isDark = true,
                    backgroundColor = backgroundColor,
                    borderColor = borderColor,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    footerAlpha = 1f,
                    onOpenPreview = null,
                    onDismiss = { expandedFilterPicker = false },
                    onCandidateSelected = { paletteId ->
                        recentFilterUsageStore.recordExpandedPickerSelection(paletteId)
                        traceState.selectCandidate(paletteId)
                        expandedFilterPicker = false
                    },
                    onCandidateFavoriteToggle = { paletteId ->
                        favoriteFilterStore.toggle(paletteId)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorCardTraceExpandedFilmGrainBrowser(
    assets: List<ColorCardTraceFilmGrainAsset>,
    selectedAssetId: String?,
    previewBitmaps: Map<String, Bitmap>,
    isDark: Boolean,
    backgroundColor: Color,
    borderColor: Color,
    primaryText: Color,
    footerAlpha: Float,
    onDismiss: () -> Unit,
    onAssetSelected: (String) -> Unit
) {
    val density = LocalDensity.current
    val navigationBarBottomPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val panelBottomPadding = 18.dp + navigationBarBottomPadding
    val gridBottomPadding = 16.dp + navigationBarBottomPadding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = panelBottomPadding)
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .fillMaxHeight(0.69f)
                .graphicsLayer {
                    alpha = footerAlpha
                }
                .clip(RoundedCornerShape(32.dp))
                .background(backgroundColor)
                .border(1.dp, borderColor, RoundedCornerShape(32.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                }
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择颗粒样式",
                    color = primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ColorCardTraceTextActionChip(
                    label = "收起",
                    enabled = true,
                    primaryText = primaryText,
                    secondaryText = primaryText.copy(alpha = 0.72f),
                    isDark = isDark,
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = gridBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = assets, key = { it.id }) { asset ->
                    ColorCardTraceExpandedFilmGrainCard(
                        asset = asset,
                        previewBitmap = previewBitmaps[asset.id],
                        selected = selectedAssetId == asset.id,
                        isDark = isDark,
                        primaryText = primaryText,
                        onClick = { onAssetSelected(asset.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCardTraceFavoriteFilterSection(
    candidates: List<ColorCardTraceFilterCandidate>,
    selectedPaletteId: String?,
    favoritePaletteIds: Set<String>,
    previewBitmaps: Map<String, Bitmap>,
    gridColumns: Int,
    isDark: Boolean,
    primaryText: Color,
    title: String = "喜爱滤镜",
    onCandidateSelected: (String) -> Unit,
    onCandidateFavoriteToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            candidates.chunked(gridColumns).forEach { rowCandidates ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCandidates.forEach { candidate ->
                        Box(modifier = Modifier.weight(1f)) {
                            ColorCardTraceExpandedFilterCard(
                                candidate = candidate,
                                previewBitmap = previewBitmaps[candidate.paletteId],
                                selected = selectedPaletteId == candidate.paletteId,
                                favorited = favoritePaletteIds.contains(candidate.paletteId),
                                isDark = isDark,
                                primaryText = primaryText,
                                onClick = { onCandidateSelected(candidate.paletteId) },
                                onLongClick = { onCandidateFavoriteToggle(candidate.paletteId) }
                            )
                        }
                    }
                    repeat((gridColumns - rowCandidates.size).coerceAtLeast(0)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorCardTraceRecentFilterSection(
    candidates: List<ColorCardTraceFilterCandidate>,
    selectedPaletteId: String?,
    favoritePaletteIds: Set<String>,
    previewBitmaps: Map<String, Bitmap>,
    isDark: Boolean,
    primaryText: Color,
    onCandidateSelected: (String) -> Unit,
    onCandidateFavoriteToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "近期使用",
            color = primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { index ->
                val candidate = candidates.getOrNull(index)
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    if (candidate != null) {
                        ColorCardTraceExpandedFilterCard(
                            candidate = candidate,
                            previewBitmap = previewBitmaps[candidate.paletteId],
                            selected = selectedPaletteId == candidate.paletteId,
                            favorited = favoritePaletteIds.contains(candidate.paletteId),
                            isDark = isDark,
                            primaryText = primaryText,
                            onClick = { onCandidateSelected(candidate.paletteId) },
                            onLongClick = { onCandidateFavoriteToggle(candidate.paletteId) }
                        )
                    } else {
                        ColorCardTraceExpandedFilterPlaceholderCard(
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorCardTraceExpandedFilmGrainCard(
    asset: ColorCardTraceFilmGrainAsset,
    previewBitmap: Bitmap?,
    selected: Boolean,
    isDark: Boolean,
    primaryText: Color,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    val outlineColor = if (selected) {
        primaryText.copy(alpha = 0.28f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val fillColor = if (selected) {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color.Black.copy(alpha = 0.025f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(fillColor)
            .border(1.dp, outlineColor, cardShape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                )
        ) {
            if (previewBitmap != null && !previewBitmap.isRecycled) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = asset.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(
                    color = primaryText,
                    strokeWidth = 1.6.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(18.dp)
                )
            }
        }

        Text(
            text = asset.title,
            color = primaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColorCardTraceExpandedFilterSectionHeader(
    title: String,
    isDark: Boolean,
    primaryText: Color
) {
    val dividerColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.dp)
                .background(dividerColor, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun ColorCardTraceExpandedFilterPlaceholderCard(
    isDark: Boolean
) {
    val cardShape = RoundedCornerShape(8.dp)
    val previewShape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
    val labelShape = RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Transparent, cardShape)
            .padding(1.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.07f),
                        previewShape
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        Color.White.copy(alpha = if (isDark) 0.90f else 0.88f),
                        labelShape
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ColorCardTraceExpandedFilterCard(
    candidate: ColorCardTraceFilterCandidate,
    previewBitmap: Bitmap?,
    selected: Boolean,
    favorited: Boolean,
    isDark: Boolean,
    primaryText: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(8.dp)
    val previewShape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
    val labelShape = RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp)
    val selectionOutline = if (selected) Color.Black.copy(alpha = 0.92f) else Color.Transparent
    val previewBase = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.Black.copy(alpha = 0.07f)
    }
    val previewScrim = if (selected) {
        Color.Black.copy(alpha = if (isDark) 0.04f else 0.03f)
    } else {
        Color.Transparent
    }
    val labelColor = if (isDark) {
        Color.Black.copy(alpha = if (selected) 0.92f else 0.84f)
    } else {
        primaryText.copy(alpha = 0.9f)
    }
    val plateFillColor = if (selected) {
        Color.White.copy(alpha = if (isDark) 0.95f else 0.92f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.90f)
    } else {
        Color.White.copy(alpha = 0.84f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 1.dp,
                color = selectionOutline,
                shape = cardShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(1.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(previewBase, previewShape)
            ) {
                if (previewBitmap != null && !previewBitmap.isRecycled) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = candidate.title,
                        contentScale = ContentScale.Crop,
                        filterQuality = if (candidate.kind == ColorCardTraceFilterKind.PixelPoster) {
                            FilterQuality.None
                        } else {
                            FilterQuality.Low
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(
                        color = primaryText,
                        strokeWidth = 1.6.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(18.dp)
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(previewScrim)
                    )
                }
                if (favorited) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 1.dp, bottom = 1.dp)
                            .size(11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "喜爱滤镜",
                            tint = Color.White,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(plateFillColor, labelShape)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = candidate.title,
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

}

@Composable
private fun MotionPhotoEntryPreviewOverlay(
    colorBitmap: Bitmap,
    grayscaleBitmap: Bitmap,
    revealProgress: Float,
    revealPattern: MotionPhotoRevealPattern,
    revealSeed: Long,
    modifier: Modifier = Modifier
) {
    val colorImage = remember(colorBitmap) { colorBitmap.asImageBitmap() }
    val grayscaleImage = remember(grayscaleBitmap) { grayscaleBitmap.asImageBitmap() }
    Canvas(modifier = modifier) {
        drawImage(grayscaleImage, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
        val radius = MotionPhotoFrameRenderer.resolveRevealRadius(
            width = size.width,
            height = size.height,
            progress = revealProgress
        )
        val revealPath = androidx.compose.ui.graphics.Path().apply {
            MotionPhotoFrameRenderer.resolveRevealAnchors(
                revealPattern = revealPattern,
                revealSeed = revealSeed
            ).forEach { anchor ->
                addOval(
                    Rect(
                        left = (size.width * anchor.xFraction) - radius,
                        top = (size.height * anchor.yFraction) - radius,
                        right = (size.width * anchor.xFraction) + radius,
                        bottom = (size.height * anchor.yFraction) + radius
                    )
                )
            }
        }
        clipPath(revealPath) {
            drawImage(colorImage, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
        }
        MotionPhotoFrameRenderer.resolveRevealAnchors(
            revealPattern = revealPattern,
            revealSeed = revealSeed
        ).forEach { anchor ->
            drawCircle(
                color = Color.White.copy(alpha = 0.72f),
                center = Offset(size.width * anchor.xFraction, size.height * anchor.yFraction),
                radius = radius,
                style = Stroke(width = size.minDimension * 0.01f)
            )
        }
    }
}

@Composable
private fun MotionPhotoRevealPatternInlinePanel(
    selectedPattern: MotionPhotoRevealPattern,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean,
    onPatternSelected: (MotionPhotoRevealPattern) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "苏醒方案",
            color = primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "点位会随机出现，并同步向外扩展。",
            color = secondaryText,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MotionPhotoRevealPatternInlineChip(
                label = "单点",
                selected = selectedPattern == MotionPhotoRevealPattern.SinglePoint,
                primaryText = primaryText,
                isDark = isDark,
                onClick = { onPatternSelected(MotionPhotoRevealPattern.SinglePoint) }
            )
            MotionPhotoRevealPatternInlineChip(
                label = "双点",
                selected = selectedPattern == MotionPhotoRevealPattern.DualPoint,
                primaryText = primaryText,
                isDark = isDark,
                onClick = { onPatternSelected(MotionPhotoRevealPattern.DualPoint) }
            )
            MotionPhotoRevealPatternInlineChip(
                label = "三点",
                selected = selectedPattern == MotionPhotoRevealPattern.TriplePoint,
                primaryText = primaryText,
                isDark = isDark,
                onClick = { onPatternSelected(MotionPhotoRevealPattern.TriplePoint) }
            )
        }
    }
}

@Composable
private fun MotionPhotoRevealPatternInlineChip(
    label: String,
    selected: Boolean,
    primaryText: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        primaryText.copy(alpha = if (isDark) 0.18f else 0.12f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.Black.copy(alpha = 0.045f)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = primaryText.copy(alpha = if (selected) 0.24f else 0.10f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = primaryText,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

private fun colorCardTraceBuiltinPreviewDiskCacheFile(
    context: Context,
    paletteId: String
): File {
    val digest = MessageDigest.getInstance("MD5")
        .digest("$COLOR_CARD_TRACE_BUILTIN_PREVIEW_DISK_CACHE_VERSION:$paletteId".toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
    return File(
        File(context.cacheDir, COLOR_CARD_TRACE_BUILTIN_PREVIEW_DISK_CACHE_DIR).apply { mkdirs() },
        "$digest.png"
    )
}

private suspend fun loadColorCardTraceBuiltinPreviewBitmapsFromDisk(
    context: Context,
    candidates: List<ColorCardTraceFilterCandidate>
): Map<String, Bitmap> = withContext(Dispatchers.IO) {
    buildMap {
        candidates.forEach { candidate ->
            val cacheFile = colorCardTraceBuiltinPreviewDiskCacheFile(context, candidate.paletteId)
            if (!cacheFile.exists() || cacheFile.length() <= 0L) return@forEach
            runCatching {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            }.getOrNull()
                ?.takeIf { !it.isRecycled }
                ?.let { decoded ->
                    val safeBitmap = if (decoded.config == Bitmap.Config.ARGB_8888) {
                        decoded
                    } else {
                        decoded.copy(Bitmap.Config.ARGB_8888, false).also {
                            if (it !== decoded) decoded.recycle()
                        }
                    }
                    put(candidate.paletteId, safeBitmap)
                }
        }
    }
}

private suspend fun persistColorCardTraceBuiltinPreviewBitmaps(
    context: Context,
    generated: Map<String, Bitmap>
) = withContext(Dispatchers.IO) {
    generated.forEach { (paletteId, bitmap) ->
        if (bitmap.isRecycled) return@forEach
        val cacheFile = colorCardTraceBuiltinPreviewDiskCacheFile(context, paletteId)
        runCatching {
            FileOutputStream(cacheFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }
}

@Composable
private fun ColorCardTraceExpandedTraceCard(
    candidate: ColorCardTraceFilterCandidate,
    selected: Boolean,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    val outlineColor = if (selected) {
        primaryText.copy(alpha = 0.28f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val fillColor = if (selected) {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color.Black.copy(alpha = 0.025f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(cardShape)
            .background(fillColor)
            .border(1.dp, outlineColor, cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = candidate.title,
            color = primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ColorCardTracePaletteStrip(
            colors = candidate.colors,
            isDark = isDark,
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
internal fun ColorCardTraceTripleSliderGroup(
    traceState: ColorCardTraceFilterState,
    paletteColors: List<Int>,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    sliderHeight: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        ColorCardTraceVerticalAdjustment(
            icon = COLOR_CARD_TRACE_SHARPEN_ICON,
            contentDescription = "锐化",
            value = traceState.sharpen,
            colors = paletteColors,
            isDark = isDark,
            onValueChange = traceState.updateSharpen,
            modifier = Modifier.height(sliderHeight)
        )
        ColorCardTraceVerticalAdjustment(
            icon = COLOR_CARD_TRACE_SATURATION_ICON,
            contentDescription = "饱和度",
            value = traceState.saturation,
            colors = paletteColors,
            isDark = isDark,
            onValueChange = traceState.updateSaturation,
            modifier = Modifier.height(sliderHeight)
        )
        ColorCardTraceVerticalAdjustment(
            icon = COLOR_CARD_TRACE_BRIGHTNESS_ICON,
            contentDescription = "亮度",
            value = traceState.brightness,
            colors = paletteColors,
            isDark = isDark,
            onValueChange = traceState.updateBrightness,
            modifier = Modifier.height(sliderHeight)
        )
    }
}

@Composable
private fun ColorCardTraceEffectDock(
    traceState: ColorCardTraceFilterState,
    isDark: Boolean,
    collapsed: Boolean,
    primaryText: Color,
    secondaryText: Color,
    backgroundColor: Color,
    borderColor: Color,
    onToggleCollapsed: () -> Unit
) {
    val dockShape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(dockShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, dockShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(
            onClick = onToggleCollapsed,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(
                    if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.045f)
                )
        ) {
            Icon(
                imageVector = if (collapsed) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (collapsed) "展开面板" else "收起面板",
                tint = primaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        ColorCardTraceTripleSliderGroup(
            traceState = traceState,
            paletteColors = traceState.displayColors,
            isDark = isDark,
            modifier = Modifier.weight(1f),
            sliderHeight = 96.dp
        )
    }
}

@Composable
internal fun ColorCardTracePaletteStrip(
    colors: List<Int>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val stripShape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .then(modifier)
            .clip(stripShape)
            .background(
                if (isDark) {
                    Color.White.copy(alpha = 0.07f)
                } else {
                    Color.Black.copy(alpha = 0.10f)
                },
                stripShape
            ),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        val safeColors = if (colors.isEmpty()) {
            listOf(0xFFD9D9D9.toInt())
        } else {
            colors
        }
        safeColors.forEach { colorInt ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(colorInt))
            )
        }
    }
}

@Composable
private fun ColorCardTraceSaveRecipeConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val traceCopy = remember(appLanguage) { getColorCardTraceCopy(appLanguage) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(traceCopy.saveCurrentRecipeTitle) },
        text = { Text(traceCopy.saveCurrentRecipeConfirm) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(traceCopy.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(traceCopy.cancel)
            }
        }
    )
}

@Composable
internal fun ColorCardTraceFilterSwitcher(
    candidateCount: Int,
    selectedIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    contentColor: Color,
    secondaryText: Color,
    isDark: Boolean,
    showIndexText: Boolean = true
) {
    val canStep = candidateCount > 1
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
            onClick = onPrevious,
            enabled = canStep,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "\u4e0a\u4e00\u4e2a\u6ee4\u955c",
                tint = if (canStep) contentColor else secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }
        if (showIndexText) {
            Text(
                text = buildString {
                    append((selectedIndex + 1).coerceAtLeast(1))
                    append("/")
                    append(candidateCount.coerceAtLeast(1))
                },
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(
            onClick = onNext,
            enabled = canStep,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "\u4e0b\u4e00\u4e2a\u6ee4\u955c",
                tint = if (canStep) contentColor else secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private val COLOR_CARD_TRACE_COMPACT_CHIP_TEXT_STYLE =
    TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

@Composable
internal fun ColorCardTraceTextActionChip(
    label: String,
    enabled: Boolean,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) {
                    Color.White.copy(alpha = 0.10f)
                } else {
                    Color.Black.copy(alpha = 0.045f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = COLOR_CARD_TRACE_COMPACT_CHIP_TEXT_STYLE,
            color = if (enabled) primaryText else secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
internal fun ColorCardTraceIconActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) {
                    Color.White.copy(alpha = 0.10f)
                } else {
                    Color.Black.copy(alpha = 0.045f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) primaryText else secondaryText,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun ColorCardTraceFilterStatusChip(
    state: ColorCardTraceFilterState,
    panelMode: ColorCardTracePanelMode,
    modifier: Modifier = Modifier
) {
    val title = colorCardTraceFilterPrimaryTitle(state, panelMode)
    val subtitle = colorCardTraceFilterSecondaryTitle(state, panelMode)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.24f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColorCardTraceFilterCountStepper(
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
                contentDescription = "减少取色点",
                tint = if (canRemovePoint) contentColor else secondaryText,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = pointCount.toString(),
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(
            onClick = onAddPoint,
            enabled = canAddPoint,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "增加取色点",
                tint = if (canAddPoint) contentColor else secondaryText,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
internal fun ColorCardTraceAdjustmentPad(
    color: Float,
    tone: Float,
    colorAxis: ColorCardTraceColorAxis,
    onAdjust: (Float, Float) -> Unit,
    isDark: Boolean,
    continuousInput: Boolean = false,
    useFullArea: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var lastSnappedPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val indicatorSize = 18.dp
    val density = LocalDensity.current
    val indicatorSizePx = with(density) { indicatorSize.roundToPx() }
    val normalizedX = ((color.coerceIn(-1f, 1f) + 1f) / 2f).coerceIn(0f, 1f)
    val normalizedY = (0.5f - (tone.coerceIn(-1f, 1f) / 2f)).coerceIn(0f, 1f)
    val snappedXDetent = remember(normalizedX) { snapColorCardTraceDetent(normalizedX) }
    val snappedYDetent = remember(normalizedY) { snapColorCardTraceDetent(normalizedY) }
    val adjustmentFieldBitmap = remember(padSize, colorAxis) {
        renderColorCardTraceAdjustmentFieldBitmap(
            width = padSize.width,
            height = padSize.height,
            colorAxis = colorAxis
        )
    }
    val padGeometry = remember(padSize, useFullArea) {
        resolveColorCardTraceAdjustmentPadGeometry(
            width = padSize.width.toFloat(),
            height = padSize.height.toFloat(),
            useFullArea = useFullArea
        )
    }
    val thumbCenter = remember(
        padGeometry,
        snappedXDetent.index,
        snappedYDetent.index,
        normalizedX,
        normalizedY,
        continuousInput
    ) {
        if (continuousInput) {
            androidx.compose.ui.geometry.Offset(
                x = padGeometry.startX + (padGeometry.stepX * ((padGeometry.columns - 1) * normalizedX.coerceIn(0f, 1f))),
                y = padGeometry.startY + (padGeometry.stepY * ((padGeometry.rows - 1) * normalizedY.coerceIn(0f, 1f)))
            )
        } else {
            resolveColorCardTraceAdjustmentPadDetentCenter(
                geometry = padGeometry,
                xIndex = snappedXDetent.index,
                yIndex = snappedYDetent.index
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(if (isDark) Color(0xFF161616) else Color(0xFFF2F2F2))
            .border(
                width = 1.dp,
                color = if (isDark) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    Color.Black.copy(alpha = 0.07f)
                },
                shape = RoundedCornerShape(26.dp)
            )
            .onSizeChanged { padSize = it }
            .pointerInput(padSize) {
                awaitEachGesture {
                    if (padSize.width <= 0 || padSize.height <= 0) return@awaitEachGesture
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val snappedDown = if (continuousInput) {
                        resolveColorCardTraceContinuousAdjustmentFromTouch(
                            touchX = down.position.x,
                            touchY = down.position.y,
                            padSize = padSize,
                            useFullArea = useFullArea
                        )
                    } else {
                        snapColorCardTraceAdjustmentFromTouch(
                            touchX = down.position.x,
                            touchY = down.position.y,
                            padSize = padSize,
                            useFullArea = useFullArea
                        )
                    }
                    onAdjust(snappedDown.normalizedX, snappedDown.normalizedY)
                    if (!continuousInput && lastSnappedPoint != (snappedDown.xIndex to snappedDown.yIndex)) {
                        lastSnappedPoint = snappedDown.xIndex to snappedDown.yIndex
                        if (hapticsAllowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                    down.consume()
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        val snappedPoint = if (continuousInput) {
                            resolveColorCardTraceContinuousAdjustmentFromTouch(
                                touchX = change.position.x,
                                touchY = change.position.y,
                                padSize = padSize,
                                useFullArea = useFullArea
                            )
                        } else {
                            snapColorCardTraceAdjustmentFromTouch(
                            touchX = change.position.x,
                            touchY = change.position.y,
                            padSize = padSize,
                            useFullArea = useFullArea
                            )
                        }
                        onAdjust(snappedPoint.normalizedX, snappedPoint.normalizedY)
                        if (!continuousInput && lastSnappedPoint != (snappedPoint.xIndex to snappedPoint.yIndex)) {
                            lastSnappedPoint = snappedPoint.xIndex to snappedPoint.yIndex
                            if (hapticsAllowed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                        val stillPressed = change.pressed
                        change.consume()
                        if (!stillPressed) break
                    }
                }
            }
    ) {
        adjustmentFieldBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotRadius = size.minDimension * 0.0105f
            val geometry = resolveColorCardTraceAdjustmentPadGeometry(
                width = size.width,
                height = size.height
            )
            repeat(geometry.rows) { row ->
                repeat(geometry.columns) { column ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.42f),
                        radius = dotRadius,
                        center = resolveColorCardTraceAdjustmentPadDetentCenter(
                            geometry = geometry,
                            xIndex = column,
                            yIndex = row
                        )
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (thumbCenter.x.roundToInt() - (indicatorSizePx / 2))
                            .coerceIn(0, (padSize.width - indicatorSizePx).coerceAtLeast(0)),
                        y = (thumbCenter.y.roundToInt() - (indicatorSizePx / 2))
                            .coerceIn(0, (padSize.height - indicatorSizePx).coerceAtLeast(0))
                    )
                }
                .size(indicatorSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.98f))
                .border(2.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
        )
    }
}

@Composable
private fun ColorCardTraceVerticalAdjustment(
    icon: ImageVector,
    contentDescription: String,
    value: Float,
    colors: List<Int>,
    isDark: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }
    var lastDetent by remember { mutableStateOf<Int?>(null) }
    val knobSize = 14.dp
    val density = LocalDensity.current
    val knobSizePx = with(density) { knobSize.roundToPx() }
    val normalizedValue = ((value.coerceIn(-1f, 1f) + 1f) / 2f).coerceIn(0f, 1f)

    Box(
        modifier = modifier.width(30.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF4F4F4F),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(15.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 18.dp, bottom = 6.dp)
                .fillMaxHeight()
                .width(20.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(999.dp)
                )
                .onSizeChanged { sliderSize = it }
                .pointerInput(sliderSize) {
                    awaitEachGesture {
                        if (sliderSize.height <= 0) return@awaitEachGesture
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val snappedDown = snapColorCardTraceVerticalValueFromTouch(
                            touchY = down.position.y,
                            sliderHeight = sliderSize.height
                        )
                        onValueChange(colorCardTraceNormalizedToSigned(snappedDown.normalizedValue))
                        if (lastDetent != snappedDown.index) {
                            lastDetent = snappedDown.index
                            if (hapticsAllowed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                        down.consume()
                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            val snappedValue = snapColorCardTraceVerticalValueFromTouch(
                                touchY = change.position.y,
                                sliderHeight = sliderSize.height
                            )
                            onValueChange(colorCardTraceNormalizedToSigned(snappedValue.normalizedValue))
                            if (lastDetent != snappedValue.index) {
                                lastDetent = snappedValue.index
                                if (hapticsAllowed) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            val stillPressed = change.pressed
                            change.consume()
                            if (!stillPressed) break
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((sliderSize.width - knobSizePx) / 2).coerceAtLeast(0),
                            y = resolveColorCardTraceCenteredThumbOffset(
                                trackSize = sliderSize.height,
                                thumbSize = knobSizePx,
                                fraction = 1f - normalizedValue
                            )
                        )
                    }
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.98f))
                    .border(2.dp, Color.Black.copy(alpha = 0.10f), CircleShape)
            )
        }
    }
}

@Composable
internal fun ColorCardTraceSaveImageButton(
    saveState: ColorCardTraceSaveImageState,
    enabled: Boolean,
    primaryText: Color,
    secondaryText: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val tint = if (enabled || saveState != ColorCardTraceSaveImageState.Idle) {
        primaryText
    } else {
        secondaryText
    }
    IconButton(
        onClick = onClick,
        enabled = enabled && saveState == ColorCardTraceSaveImageState.Idle,
        modifier = Modifier.size(32.dp)
    ) {
        when (saveState) {
            ColorCardTraceSaveImageState.Idle -> {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(17.dp)
                )
            }
            ColorCardTraceSaveImageState.Saving -> {
                CircularProgressIndicator(
                    color = primaryText,
                    strokeWidth = 1.8.dp,
                    modifier = Modifier.size(16.dp)
                )
            }
            ColorCardTraceSaveImageState.Saved -> {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = contentDescription,
                    tint = primaryText,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

private val COLOR_CARD_TRACE_SHARPEN_ICON: ImageVector by lazy {
    ImageVector.Builder(
        name = "ColorCardTraceSharpen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes("M21 3.6V20.4C21 20.7314 20.7314 21 20.4 21H3.6C3.26863 21 3 20.7314 3 20.4V3.6C3 3.26863 3.26863 3 3.6 3H20.4C20.7314 3 21 3.26863 21 3.6Z"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M10 17.6586C10.6256 17.8797 11.2987 18 12 18C15.3137 18 18 15.3137 18 12C18 9.38756 16.3304 7.16508 14 6.34141M10 17.6586C7.66962 16.8349 6 14.6124 6 12C6 8.68629 8.68629 6 12 6C12.7013 6 13.3744 6.12031 14 6.34141M10 17.6586L14 6.34141"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
    }.build()
}

private val COLOR_CARD_TRACE_BRIGHTNESS_ICON: ImageVector by lazy {
    ImageVector.Builder(
        name = "ColorCardTraceBrightness",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes("M12 18C15.3137 18 18 15.3137 18 12C18 8.68629 15.3137 6 12 6C8.68629 6 6 8.68629 6 12C6 15.3137 8.68629 18 12 18Z"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M22 12L23 12"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M12 2V1"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M12 23V22"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M20 20L19 19"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M20 4L19 5"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M4 20L5 19"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M4 4L5 5"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M1 12L2 12"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
    }.build()
}

private val COLOR_CARD_TRACE_SATURATION_ICON: ImageVector by lazy {
    ImageVector.Builder(
        name = "ColorCardTraceSaturation",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes("M2.63596 10.2927L9.70703 3.22168L18.1923 11.707L11.1212 18.778C10.3402 19.5591 9.07387 19.5591 8.29282 18.778L2.63596 13.1212C1.85492 12.3401 1.85492 11.0738 2.63596 10.2927Z"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M8.29297 1.80762L9.70718 3.22183"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M19.9991 15C19.9991 15 22.9991 17.9934 22.9994 19.8865C22.9997 21.5422 21.6552 22.8865 19.9997 22.8865C18.3442 22.8865 17.012 21.5422 17 19.8865C17.0098 17.9924 19.9991 15 19.9991 15Z"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1.5f,
            pathFillType = PathFillType.EvenOdd
        )
    }.build()
}

internal val COLOR_CARD_TRACE_COMPARE_ICON_OUTLINE: ImageVector by lazy {
    ImageVector.Builder(
        name = "ColorCardTraceCompareOutline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        listOf(
            "M8.00563 20.005L8.01674 19.995",
            "M4.00563 20.005L4.01674 19.995",
            "M4.00563 16.005L4.01674 15.995",
            "M4.00563 12.005L4.01674 11.995",
            "M4.00563 8.005L4.01674 7.995",
            "M4.00563 4.005L4.01674 3.995",
            "M8.00563 4.005L8.01674 3.995",
            "M12.0059 20.005H20.0059V4.005H12.0059V20.005Z"
        ).forEach { pathData ->
            addPath(
                pathData = addPathNodes(pathData),
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            )
        }
    }.build()
}

internal val COLOR_CARD_TRACE_COMPARE_ICON_FILLED: ImageVector by lazy {
    ImageVector.Builder(
        name = "ColorCardTraceCompareFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        listOf(
            "M8.00514 20.0049L8.01625 19.9949",
            "M4.00514 20.0049L4.01625 19.9949",
            "M4.00514 16.0049L4.01625 15.9949",
            "M4.00514 12.0049L4.01625 11.9949",
            "M4.00514 8.00488L4.01625 7.99488",
            "M4.00514 4.00488L4.01625 3.99488",
            "M8.00514 4.00488L8.01625 3.99488"
        ).forEach { pathData ->
            addPath(
                pathData = addPathNodes(pathData),
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            )
        }
        addPath(
            pathData = addPathNodes("M12.0049 20.0049H20.0049V4.00488H12.0049V20.0049Z"),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
    }.build()
}

@Composable
private fun ColorCardTraceFilmGrainInlineControls(
    enabled: Boolean,
    selectedLabel: String?,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onToggle: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    val containerShape = RoundedCornerShape(999.dp)
    val baseFill = if (enabled) {
        if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.03f)
    }
    val borderColor = if (enabled) {
        primaryText.copy(alpha = 0.18f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .widthIn(min = 48.dp)
                .clip(containerShape)
                .background(baseFill)
                .border(1.dp, borderColor, containerShape)
                .clickable { onToggle(!enabled) }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "颗粒",
                style = COLOR_CARD_TRACE_COMPACT_CHIP_TEXT_STYLE,
                color = if (enabled) primaryText else secondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        if (enabled) {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(containerShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        containerShape
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLabel ?: "--",
                        style = COLOR_CARD_TRACE_COMPACT_CHIP_TEXT_STYLE,
                        color = primaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "刷新颗粒",
                        tint = primaryText,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(onClick = onRefresh)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ColorCardTraceStrengthSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }
    val knobRadius = 9.dp
    val knobRadiusPx = with(LocalDensity.current) { knobRadius.roundToPx() }
    val knobSizePx = knobRadiusPx * 2

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
            )
            .border(
                width = 1.dp,
                color = if (isDark) {
                    Color.White.copy(alpha = 0.10f)
                } else {
                    Color.Black.copy(alpha = 0.06f)
                },
                shape = RoundedCornerShape(999.dp)
            )
            .onSizeChanged { sliderSize = it }
            .pointerInput(sliderSize) {
                awaitEachGesture {
                    if (sliderSize.width <= 0) return@awaitEachGesture
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    updateColorCardTraceStrengthFromTouch(
                        touchX = down.position.x,
                        sliderWidth = sliderSize.width,
                        onValueChange = onValueChange
                    )
                    down.consume()
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        updateColorCardTraceStrengthFromTouch(
                            touchX = change.position.x,
                            sliderWidth = sliderSize.width,
                            onValueChange = onValueChange
                        )
                        val stillPressed = change.pressed
                        change.consume()
                        if (!stillPressed) break
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = resolveColorCardTraceCenteredThumbOffset(
                            trackSize = sliderSize.width,
                            thumbSize = knobSizePx,
                            fraction = value
                        ),
                        y = 0
                    )
                }
                .align(Alignment.CenterStart)
                .size(knobRadius * 2)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.98f))
                .border(2.dp, Color.Black.copy(alpha = 0.10f), CircleShape)
        )
    }
}

@Composable
private fun ColorCardTraceAdjustmentMetrics(
    panelMode: ColorCardTracePanelMode,
    tone: Float,
    color: Float,
    paletteStrength: Float,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean
) {
    val toneLabel = "影调"
    val colorLabel = "色彩"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ColorCardTraceMetricChip(
            label = toneLabel,
            value = formatColorCardTraceSignedPercent(tone),
            primaryText = primaryText,
            secondaryText = secondaryText,
            isDark = isDark,
            modifier = Modifier.width(88.dp)
        )
        ColorCardTraceMetricChip(
            label = colorLabel,
            value = formatColorCardTraceSignedPercent(color),
            primaryText = primaryText,
            secondaryText = secondaryText,
            isDark = isDark,
            modifier = Modifier.width(88.dp)
        )
        ColorCardTraceMetricChip(
            label = "强度",
            value = (paletteStrength.coerceIn(0f, 1f) * 100f).roundToInt().toString(),
            primaryText = primaryText,
            secondaryText = secondaryText,
            isDark = isDark,
            modifier = Modifier.width(88.dp)
        )
    }
}

@Composable
private fun ColorCardTraceMetricChip(
    label: String,
    value: String,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDark) {
                    Color.White.copy(alpha = 0.05f)
                } else {
                    Color.Black.copy(alpha = 0.04f)
                }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = secondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private suspend fun decodeColorCardTracePreviewBitmap(
    context: Context,
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

internal suspend fun decodeColorCardTraceFilterPreviewSampleBitmap(
    context: Context,
    maxSide: Int
): Bitmap? = withContext(Dispatchers.IO) {
    listOf(
        COLOR_CARD_TRACE_FILTER_PREVIEW_SAMPLE_WEBP,
        COLOR_CARD_TRACE_FILTER_PREVIEW_SAMPLE_PNG
    ).firstNotNullOfOrNull { assetName ->
        runCatching {
            context.assets.open(assetName).use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()?.let { bitmap ->
            val currentMaxSide = max(bitmap.width, bitmap.height).coerceAtLeast(1)
            if (currentMaxSide <= maxSide) {
                if (bitmap.config == Bitmap.Config.ARGB_8888) {
                    bitmap
                } else {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false).also {
                        if (it !== bitmap) {
                            bitmap.recycle()
                        }
                    }
                }
            } else {
                val scale = maxSide.toFloat() / currentMaxSide.toFloat()
                val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                if (scaled !== bitmap) {
                    bitmap.recycle()
                }
                if (scaled.config == Bitmap.Config.ARGB_8888) {
                    scaled
                } else {
                    scaled.copy(Bitmap.Config.ARGB_8888, false).also {
                        if (it !== scaled) {
                            scaled.recycle()
                        }
                    }
                }
            }
        }
    }
}

internal suspend fun renderColorCardTraceFeedPreviewBitmap(
    context: Context,
    sourceBitmap: Bitmap,
    candidate: ColorCardTraceFilterCandidate
): Bitmap? = withContext(Dispatchers.Default) {
    renderColorCardTraceRealtimePreviewBitmap(
        context = context,
        sourceBitmap = sourceBitmap,
        candidate = candidate
    )?.let { return@withContext it }

    when (candidate.kind) {
        ColorCardTraceFilterKind.HaldClut -> {
            val primaryAssetName = candidate.haldAssetName ?: return@withContext null
            val haldBitmap = decodeColorCardBuiltinHaldBitmap(
                context = context,
                assetName = primaryAssetName
            ) ?: return@withContext null
            try {
                renderColorCardHaldFilterBitmap(
                    sourceBitmap = sourceBitmap,
                    haldBitmap = haldBitmap,
                    haldLevel = candidate.haldLevel,
                    secondaryHaldBitmap = null,
                    secondaryHaldLevel = 0,
                    haldVariantMix = 0f,
                    color = 0f,
                    colorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
                    sharpen = 0f,
                    tone = 0f,
                    saturation = 0f,
                    brightness = 0f,
                    strength = defaultColorCardTracePaletteStrength(candidate),
                    faceRegions = emptyList(),
                    portraitParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED
                )
            } finally {
                if (!haldBitmap.isRecycled) {
                    haldBitmap.recycle()
                }
            }
        }
        ColorCardTraceFilterKind.PixelPoster -> {
            renderColorCardTracePixelPosterBitmap(
                sourceBitmap = sourceBitmap,
                candidate = candidate,
                color = 0f,
                colorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
                sharpen = 0f,
                tone = 0f,
                saturation = 0f,
                brightness = 0f,
                strength = defaultColorCardTracePaletteStrength(candidate),
                faceRegions = emptyList()
            )
        }
        ColorCardTraceFilterKind.SelectiveColor -> {
            renderColorCardTraceSelectiveColorBitmap(
                sourceBitmap = sourceBitmap,
                candidate = candidate,
                color = 0f,
                colorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
                sharpen = 0f,
                tone = 0f,
                saturation = 0f,
                brightness = 0f,
                strength = defaultColorCardTracePaletteStrength(candidate),
                faceRegions = emptyList(),
                portraitParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED
            )
        }
        ColorCardTraceFilterKind.SubjectFrame -> {
            renderColorCardTraceSubjectFrameBitmap(
                sourceBitmap = sourceBitmap,
                subjectRect = resolveColorCardTraceSubjectRect(
                    sourceSessionId = candidate.paletteId.hashCode().toLong(),
                    sourceAspect = sourceBitmap.width.toFloat() / sourceBitmap.height.coerceAtLeast(1).toFloat(),
                    faceRegions = emptyList()
                ),
                color = 0f,
                colorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
                sharpen = 0f,
                tone = 0f,
                saturation = 0f,
                brightness = 0f,
                strength = defaultColorCardTracePaletteStrength(candidate)
            )
        }
        ColorCardTraceFilterKind.BlackMist -> {
            renderColorCardTraceBlackMistBitmap(
                sourceBitmap = sourceBitmap,
                color = 0f,
                colorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
                sharpen = 0f,
                tone = 0f,
                saturation = 0f,
                brightness = 0f,
                strength = defaultColorCardTracePaletteStrength(candidate)
            )
        }
        ColorCardTraceFilterKind.MotionPhotoEntry -> null
        ColorCardTraceFilterKind.AiMnn -> null
        ColorCardTraceFilterKind.Palette -> null
    }
}

internal suspend fun renderColorCardTraceRealtimePreviewBitmap(
    context: Context,
    sourceBitmap: Bitmap,
    candidate: ColorCardTraceFilterCandidate,
    color: Float = 0f,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    sharpen: Float = 0f,
    tone: Float = 0f,
    saturation: Float = 0f,
    brightness: Float = 0f,
    paletteStrength: Float = defaultColorCardTracePaletteStrength(candidate),
    faceRegions: List<ColorCardTraceFaceRegion> = emptyList(),
    portraitParams: ColorCardPortraitOptimizationParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED,
    subjectRect: ColorCardTraceSubjectRect? = null,
    sourceColors: List<Int> = emptyList()
): Bitmap? {
    if (sourceBitmap.isRecycled) return null
    if (
        candidate.kind == ColorCardTraceFilterKind.Palette ||
        candidate.kind == ColorCardTraceFilterKind.AiMnn ||
        candidate.kind == ColorCardTraceFilterKind.MotionPhotoEntry
    ) {
        return null
    }

    var haldBitmap: Bitmap? = null
    var secondaryHaldBitmap: Bitmap? = null
    var haldVariantMix = 0f
    try {
        if (candidate.kind == ColorCardTraceFilterKind.HaldClut) {
            val resolvedBlend = resolveColorCardTraceHaldBlend(
                candidate = candidate,
                paletteStrength = paletteStrength
            )
            val primaryAssetName = resolvedBlend.primaryAssetName ?: return null
            haldBitmap = decodeColorCardBuiltinHaldBitmap(
                context = context,
                assetName = primaryAssetName
            ) ?: return null
            secondaryHaldBitmap = resolvedBlend.secondaryAssetName
                ?.let { assetName ->
                    decodeColorCardBuiltinHaldBitmap(
                        context = context,
                        assetName = assetName
                    )
                }
            haldVariantMix = if (secondaryHaldBitmap == null) 0f else resolvedBlend.variantMix
        }

        val safeWidth = sourceBitmap.width.coerceAtLeast(1)
        val safeHeight = sourceBitmap.height.coerceAtLeast(1)
        val gpuParams = buildColorCardTraceGpuParams(
            panelMode = ColorCardTracePanelMode.Filter,
            sourceColors = sourceColors,
            selectedCandidate = candidate,
            selectedHaldBitmap = haldBitmap,
            secondaryHaldBitmap = secondaryHaldBitmap,
            haldVariantMix = haldVariantMix,
            color = color,
            sharpen = sharpen,
            tone = tone,
            saturation = saturation,
            brightness = brightness,
            colorAxis = colorAxis,
            paletteStrength = paletteStrength,
            faceRegions = faceRegions,
            portraitParams = portraitParams,
            subjectRect = subjectRect,
            sourceAspect = safeWidth.toFloat() / safeHeight.toFloat(),
            sourceTexelWidth = 1f / safeWidth.toFloat(),
            sourceTexelHeight = 1f / safeHeight.toFloat()
        )
        val gpuBitmap = renderColorCardTraceGpuBitmap(
            sourceBitmap = sourceBitmap,
            params = gpuParams
        ) ?: return null
        if (candidate.kind != ColorCardTraceFilterKind.HaldClut) {
            return gpuBitmap
        }
        return applyColorCardTraceHaldPostProcessing(
            sourceBitmap = sourceBitmap,
            renderedBitmap = gpuBitmap,
            color = color,
            tone = tone,
            saturation = saturation,
            brightness = brightness,
            paletteStrength = paletteStrength
        )
    } finally {
        secondaryHaldBitmap?.takeIf { !it.isRecycled }?.recycle()
        haldBitmap?.takeIf { !it.isRecycled }?.recycle()
    }
}

internal fun renderColorCardTraceFeedApproxPreviewBitmap(
    sourceBitmap: Bitmap,
    candidate: ColorCardTraceFilterCandidate
): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val palette = candidate.colors.ifEmpty {
        listOf(
            0xFF202020.toInt(),
            0xFF686868.toInt(),
            0xFFB8B8B8.toInt(),
            0xFFF0F0F0.toInt()
        )
    }
    val paletteSize = palette.size.coerceAtLeast(1)
    val safeStrength = COLOR_CARD_TRACE_FEED_PREVIEW_STRENGTH.coerceIn(0f, 1f).toDouble()
    val (contrast, saturationBoost, softness, accentBias) = when (candidate.kind) {
        ColorCardTraceFilterKind.BlackMist -> listOf(1.08, 0.72, 0.18, 0.0)
        ColorCardTraceFilterKind.SelectiveColor -> listOf(1.20, 0.96, 0.0, 0.42)
        ColorCardTraceFilterKind.PixelPoster -> listOf(1.30, 1.0, 0.0, 0.0)
        ColorCardTraceFilterKind.SubjectFrame -> listOf(1.06, 0.82, 0.0, 0.0)
        else -> when (candidate.title.lastOrNull()?.uppercaseChar()) {
            'F' -> listOf(1.16, 0.92, 0.0, 0.0)
            'C' -> listOf(1.22, 0.84, 0.0, 0.0)
            else -> listOf(1.10, 0.78, 0.0, 0.0)
        }
    }

    for (index in sourcePixels.indices) {
        val sourceColor = sourcePixels[index]
        val alpha = android.graphics.Color.alpha(sourceColor)
        if (alpha == 0) {
            outputPixels[index] = sourceColor
            continue
        }
        val red = android.graphics.Color.red(sourceColor)
        val green = android.graphics.Color.green(sourceColor)
        val blue = android.graphics.Color.blue(sourceColor)
        val luma = ((red * 0.299) + (green * 0.587) + (blue * 0.114)) / 255.0
        val contrasted = ((((luma - 0.5) * contrast) + 0.5).coerceIn(0.0, 1.0))
        val scaledIndex = contrasted * (paletteSize - 1).coerceAtLeast(0)
        val leftIndex = scaledIndex.toInt().coerceIn(0, paletteSize - 1)
        val rightIndex = (leftIndex + 1).coerceAtMost(paletteSize - 1)
        val mix = (scaledIndex - leftIndex).coerceIn(0.0, 1.0)
        val leftColor = palette[leftIndex]
        val rightColor = palette[rightIndex]
        val toneRed =
            (android.graphics.Color.red(leftColor) * (1.0 - mix)) + (android.graphics.Color.red(rightColor) * mix)
        val toneGreen =
            (android.graphics.Color.green(leftColor) * (1.0 - mix)) + (android.graphics.Color.green(rightColor) * mix)
        val toneBlue =
            (android.graphics.Color.blue(leftColor) * (1.0 - mix)) + (android.graphics.Color.blue(rightColor) * mix)
        val gray = luma * 255.0
        val mappedRed = gray + ((toneRed - gray) * saturationBoost)
        val mappedGreen = gray + ((toneGreen - gray) * saturationBoost)
        val mappedBlue = gray + ((toneBlue - gray) * saturationBoost)
        val softenedRed = mappedRed + ((255.0 - mappedRed) * softness * (1.0 - luma))
        val softenedGreen = mappedGreen + ((255.0 - mappedGreen) * softness * (1.0 - luma))
        val softenedBlue = mappedBlue + ((255.0 - mappedBlue) * softness * (1.0 - luma))
        val accentMask = if (accentBias > 0.0) {
            val maxChannel = maxOf(red, green, blue) / 255.0
            val minChannel = minOf(red, green, blue) / 255.0
            ((maxChannel - minChannel) * accentBias).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val selectiveRed = gray + ((red - gray) * accentMask)
        val selectiveGreen = gray + ((green - gray) * accentMask)
        val selectiveBlue = gray + ((blue - gray) * accentMask)
        val blendedRed = (softenedRed * (1.0 - accentMask)) + (selectiveRed * accentMask)
        val blendedGreen = (softenedGreen * (1.0 - accentMask)) + (selectiveGreen * accentMask)
        val blendedBlue = (softenedBlue * (1.0 - accentMask)) + (selectiveBlue * accentMask)
        val finalRed = (red * (1.0 - safeStrength)) + (blendedRed * safeStrength)
        val finalGreen = (green * (1.0 - safeStrength)) + (blendedGreen * safeStrength)
        val finalBlue = (blue * (1.0 - safeStrength)) + (blendedBlue * safeStrength)
        outputPixels[index] = android.graphics.Color.argb(
            alpha,
            finalRed.toInt().coerceIn(0, 255),
            finalGreen.toInt().coerceIn(0, 255),
            finalBlue.toInt().coerceIn(0, 255)
        )
    }

    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

private suspend fun buildColorCardTraceBuiltinPreviewBitmaps(
    context: Context,
    candidates: List<ColorCardTraceFilterCandidate>,
    feedDemoAssetNames: List<String> = emptyList(),
    assignmentOffset: Int = 0
): Map<String, Bitmap> {
    val manifestFeedDemoAssetNames = feedDemoAssetNames
        .asSequence()
        .filter { it.isNotBlank() }
        .filterNot { assetName ->
            isColorCardTracePreviewFeedDemoExcluded(
                demoAssetId = null,
                assetName = assetName
            )
        }
        .distinct()
        .toList()
    val listedFeedDemoAssetNames = runCatching {
        context.assets
            .list("color_card_filter_feed_demo")
            ?.map { name -> "color_card_filter_feed_demo/$name" }
            .orEmpty()
    }.getOrDefault(emptyList())
        .filterNot { assetName ->
            isColorCardTracePreviewFeedDemoExcluded(
                demoAssetId = null,
                assetName = assetName
            )
        }
        .distinct()
    val eligibleFeedDemoAssetNames = (manifestFeedDemoAssetNames + listedFeedDemoAssetNames)
        .filter(String::isNotBlank)
        .distinct()
    val feedSourceBitmaps = if (eligibleFeedDemoAssetNames.isNotEmpty()) {
        withContext(Dispatchers.IO) {
            buildMap<String, Bitmap> {
                eligibleFeedDemoAssetNames.forEach { assetName ->
                    decodeColorCardFilterFeedDemoBitmap(
                        context = context,
                        assetName = assetName,
                        maxSidePx = COLOR_CARD_TRACE_FILTER_PREVIEW_MAX_SIDE
                    )?.let { bitmap ->
                        put(assetName, bitmap)
                    }
                }
            }
        }
    } else {
        emptyMap()
    }
    val feedSourceEntries = feedSourceBitmaps.entries.toList()
    val fallbackSampleBitmap = if (feedSourceEntries.isEmpty()) {
        decodeColorCardTraceFilterPreviewSampleBitmap(
            context = context,
            maxSide = COLOR_CARD_TRACE_FILTER_PREVIEW_MAX_SIDE
        )
    } else {
        null
    }
    if (feedSourceEntries.isEmpty() && fallbackSampleBitmap == null) return emptyMap()

    return try {
        withContext(Dispatchers.Default) {
            buildMap {
                candidates.forEachIndexed { index, candidate ->
                    val sourceBitmap = if (feedSourceEntries.isNotEmpty()) {
                        val sourceIndex = ((assignmentOffset + index) % feedSourceEntries.size)
                            .let { if (it < 0) it + feedSourceEntries.size else it }
                        feedSourceEntries[sourceIndex].value
                    } else {
                        fallbackSampleBitmap ?: return@forEachIndexed
                    }
                    when (candidate.kind) {
                        ColorCardTraceFilterKind.HaldClut -> {
                            val assetName = candidate.haldAssetName ?: return@forEachIndexed
                            val haldBitmap = decodeColorCardBuiltinHaldBitmap(
                                context = context,
                                assetName = assetName
                            ) ?: return@forEachIndexed
                            try {
                                put(
                                    candidate.paletteId,
                                    renderColorCardHaldFilterBitmap(
                                        sourceBitmap = sourceBitmap,
                                        haldBitmap = haldBitmap,
                                        haldLevel = candidate.haldLevel,
                                        secondaryHaldBitmap = null,
                                        secondaryHaldLevel = 0,
                                        haldVariantMix = 0f,
                                        color = 0f,
                                        colorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
                                        sharpen = 0f,
                                        tone = 0f,
                                        saturation = COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_SATURATION,
                                        brightness = COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_BRIGHTNESS,
                                        strength = COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_STRENGTH,
                                        faceRegions = emptyList(),
                                        portraitParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED
                                    )
                                )
                            } finally {
                                haldBitmap.recycle()
                            }
                        }
                        ColorCardTraceFilterKind.PixelPoster -> {
                                put(
                                    candidate.paletteId,
                                    renderColorCardTracePixelPosterBitmap(
                                        sourceBitmap = sourceBitmap,
                                        candidate = candidate,
                                        color = 0f,
                                        sharpen = 0f,
                                        tone = 0f,
                                    saturation = 0f,
                                    brightness = 0f,
                                    strength = COLOR_CARD_TRACE_PIXEL_POSTER_FIXED_STRENGTH,
                                    faceRegions = emptyList()
                                )
                            )
                        }
                        ColorCardTraceFilterKind.SelectiveColor -> {
                                put(
                                    candidate.paletteId,
                                    renderColorCardTraceSelectiveColorBitmap(
                                        sourceBitmap = sourceBitmap,
                                        candidate = candidate,
                                        color = 0f,
                                        sharpen = 0f,
                                        tone = 0f,
                                    saturation = 0f,
                                    brightness = 0f,
                                    strength = 1f
                                )
                            )
                        }
                        ColorCardTraceFilterKind.SubjectFrame -> {
                                put(
                                    candidate.paletteId,
                                    renderColorCardTraceSubjectFrameBitmap(
                                        sourceBitmap = sourceBitmap,
                                        subjectRect = resolveColorCardTraceSubjectRect(
                                            sourceSessionId = 0L,
                                            sourceAspect =
                                                sourceBitmap.width.toFloat() /
                                                    sourceBitmap.height.coerceAtLeast(1).toFloat(),
                                            faceRegions = emptyList()
                                        ),
                                        color = 0f,
                                        sharpen = 0f,
                                    tone = 0f,
                                    saturation = 0f,
                                    brightness = 0f,
                                    strength = 1f
                                )
                            )
                        }
                        ColorCardTraceFilterKind.BlackMist -> {
                                put(
                                    candidate.paletteId,
                                    renderColorCardTraceBlackMistBitmap(
                                        sourceBitmap = sourceBitmap,
                                        color = 0f,
                                        sharpen = 0f,
                                        tone = 0f,
                                        saturation = COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_SATURATION,
                                    brightness = COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_BRIGHTNESS,
                                    strength = COLOR_CARD_TRACE_FILTER_PREVIEW_ICON_STRENGTH
                                )
                            )
                        }
                        ColorCardTraceFilterKind.MotionPhotoEntry -> {
                            put(
                                candidate.paletteId,
                                sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            )
                        }
                        ColorCardTraceFilterKind.AiMnn -> {
                            put(
                                candidate.paletteId,
                                sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            )
                        }
                        ColorCardTraceFilterKind.Palette -> Unit
                    }
                }
            }
        }
    } finally {
        fallbackSampleBitmap
            ?.takeIf { !it.isRecycled }
            ?.recycle()
    }
}

internal fun buildColorCardTraceBuiltinCandidates(
    builtinHaldFilters: List<ColorCardBuiltinHaldFilterAsset>,
    aiMnnCandidates: List<ColorCardTraceFilterCandidate> = emptyList()
): List<ColorCardTraceFilterCandidate> {
    val pixelPosterCandidates = buildColorCardTracePixelPosterCandidates()
    val selectiveColorCandidates = buildColorCardTraceSelectiveColorCandidates()
    val subjectFrameCandidate = buildColorCardTraceSubjectFrameCandidate()
    val blackMistCandidate = buildColorCardTraceBlackMistCandidate()
    val motionPhotoEntryCandidate = buildColorCardTraceMotionPhotoEntryCandidate()
    val filteredFilters = filterColorCardTraceBuiltinHaldFilters(builtinHaldFilters)
    val haldCandidates = filteredFilters
        .groupBy(ColorCardBuiltinHaldFilterAsset::canonicalId)
        .values
        .sortedBy { group -> group.minOf(ColorCardBuiltinHaldFilterAsset::sortOrder) }
        .map { group ->
            val variants = buildColorCardTraceHaldVariants(group)
            val lead = group.first()
            val defaultVariant = variants.minByOrNull { abs(it.position) } ?: variants.first()
            ColorCardTraceFilterCandidate(
                paletteId = "builtin_hald:${lead.canonicalId}",
                kind = ColorCardTraceFilterKind.HaldClut,
                title = lead.displayCode,
                sourceTag = lead.sourceTag,
                colors = defaultVariant.previewColors,
                score = -1.0,
                haldAssetName = defaultVariant.assetName,
                haldLevel = defaultVariant.haldLevel,
                haldVariants = variants
            )
    }
    return orderColorCardTraceBuiltinCandidatesForDisplay(
        pixelPosterCandidates +
            subjectFrameCandidate +
            blackMistCandidate +
            selectiveColorCandidates +
            haldCandidates +
            motionPhotoEntryCandidate +
            aiMnnCandidates
    )
}

internal fun renderColorCardTracePalettePreviewBitmap(
    sourceBitmap: Bitmap,
    sourceColors: List<Int>,
    targetColors: List<Int>,
    color: Float,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    paletteStrength: Float
): Bitmap {
    return renderColorCardTraceFilterBitmap(
        sourceBitmap = sourceBitmap,
        sourceColors = sourceColors,
        targetColors = targetColors,
        color = color,
        colorAxis = colorAxis,
        sharpen = sharpen,
        tone = tone,
        saturation = saturation,
        brightness = brightness,
        paletteStrength = paletteStrength,
        faceRegions = emptyList(),
        portraitParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED
    )
}

private fun buildColorCardTraceAiMnnCandidates(
    series: MNNFilterSeries
): List<ColorCardTraceFilterCandidate> {
    return series.filters.map { filter ->
        ColorCardTraceFilterCandidate(
            paletteId = "ai_mnn:${filter.id}",
            kind = ColorCardTraceFilterKind.AiMnn,
            title = filter.title,
            sourceTag = series.title,
            colors = COLOR_CARD_TRACE_AI_PLACEHOLDER_COLORS,
            score = -1.0,
            mnnFilterAsset = filter
        )
    }
}

internal fun filterColorCardTraceBuiltinHaldFilters(
    builtinHaldFilters: List<ColorCardBuiltinHaldFilterAsset>
): List<ColorCardBuiltinHaldFilterAsset> {
    return builtinHaldFilters.filterNot { filter ->
        filter.id in COLOR_CARD_TRACE_HIDDEN_BUILTIN_FILTER_IDS ||
            filter.displayCode in COLOR_CARD_TRACE_HIDDEN_BUILTIN_FILTER_TITLES
    }
}

internal fun buildColorCardTraceBuiltinDisplayTitles(
    builtinHaldFilters: List<ColorCardBuiltinHaldFilterAsset>
): Map<String, String> {
    val shuffledNumbers = (100..999).shuffled(Random(COLOR_CARD_TRACE_BUILTIN_NAME_SEED))
    return builtinHaldFilters.mapIndexed { index, filter ->
        val suffix = classifyColorCardTraceBuiltinFilterSuffix(filter)
        filter.id to "#${shuffledNumbers[index]}$suffix"
    }.toMap()
}

internal fun classifyColorCardTraceBuiltinFilterSuffix(
    filter: ColorCardBuiltinHaldFilterAsset
): Char {
    val token = buildString {
        append(filter.id)
        append(' ')
        append(filter.title)
    }.lowercase(Locale.ROOT)
    return when {
        filter.id.startsWith("classic-memory-") -> 'M'
        filter.id.startsWith("black-white-era-") -> 'B'
        filter.id in COLOR_CARD_TRACE_FILM_FILTER_IDS -> 'F'
        COLOR_CARD_TRACE_FILM_KEYWORDS.any(token::contains) -> 'F'
        COLOR_CARD_TRACE_CINEMA_KEYWORDS.any(token::contains) -> 'C'
        else -> 'A'
    }
}

private fun rankColorCardTraceFilterCandidates(
    sourceColors: List<Int>,
    savedPalettes: List<SavedColorCardPalette>
): List<ColorCardTraceFilterCandidate> {
    return savedPalettes.asSequence()
        .mapNotNull { palette ->
            val colors = palette.colors.take(10)
            if (colors.isEmpty()) return@mapNotNull null
            ColorCardTraceFilterCandidate(
                paletteId = palette.id,
                kind = ColorCardTraceFilterKind.Palette,
                title = palette.title
                    ?.takeIf { it.isNotBlank() }
                    ?: palette.sourceTag?.takeIf { it.isNotBlank() }
                    ?: "未命名色卡",
                sourceTag = palette.sourceTag,
                colors = colors,
                score = scoreColorCardTraceFilterMatch(
                    sourceColors = sourceColors,
                    targetColors = colors
                ),
                haldAssetName = null,
                haldLevel = 0
            )
        }
        .sortedBy { it.score }
        .toList()
}

private fun scoreColorCardTraceFilterMatch(
    sourceColors: List<Int>,
    targetColors: List<Int>
): Double {
    val mapping = buildColorCardTraceAnchorMapping(
        sourceColors = sourceColors,
        targetColors = targetColors
    ) ?: return Double.MAX_VALUE

    val pairCost = mapping.sourceAnchors.zip(mapping.targetAnchors).map { (source, target) ->
        colorCardTraceAnchorPairCost(source, target)
    }.average()
    val centroidCost = colorCardTraceDeltaE00(mapping.sourceCentroid, mapping.targetCentroid)
    val sourceSpread = colorCardTracePaletteSpread(mapping.sourceAnchors, mapping.sourceCentroid)
    val targetSpread = colorCardTracePaletteSpread(mapping.targetAnchors, mapping.targetCentroid)
    return pairCost +
        (centroidCost * 0.22) +
        (abs(sourceSpread - targetSpread) * 0.08)
}

private fun scoreColorCardImitateFilterMatch(
    sourceColors: List<Int>,
    targetColors: List<Int>
): Double {
    val mapping = buildColorCardImitateAnchorMapping(
        sourceColors = sourceColors,
        targetColors = targetColors
    ) ?: return Double.MAX_VALUE

    val pairCost = mapping.sourceAnchors.zip(mapping.targetAnchors).map { (source, target) ->
        val deltaL = (source.l - target.l) * 0.30
        val deltaA = source.a - target.a
        val deltaB = source.b - target.b
        sqrt((deltaL * deltaL) + (deltaA * deltaA) + (deltaB * deltaB))
    }.average()
    val centroidCost = colorCardTraceDeltaE00(mapping.sourceCentroid, mapping.targetCentroid)
    val sourceSpread = colorCardTracePaletteSpread(mapping.sourceAnchors, mapping.sourceCentroid)
    val targetSpread = colorCardTracePaletteSpread(mapping.targetAnchors, mapping.targetCentroid)
    return pairCost +
        (centroidCost * 0.12) +
        (abs(sourceSpread - targetSpread) * 0.06)
}

private fun deriveColorCardImitateMappingProfile(
    matchScore: Double
): ColorCardTraceMappingProfile {
    val normalizedMismatch = ((matchScore - 5.5) / 24.0).coerceIn(0.0, 1.0)
    val similarity = 1.0 - normalizedMismatch
    return ColorCardTraceMappingProfile(
        blendStrength = lerpColorCardTrace(0.34, 0.60, similarity),
        maxLightnessShift = lerpColorCardTrace(4.0, 9.0, similarity),
        maxChromaShift = lerpColorCardTrace(16.0, 28.0, similarity)
    )
}

private fun buildColorCardImitateAnchorMapping(
    sourceColors: List<Int>,
    targetColors: List<Int>
): ColorCardTraceAnchorMapping? {
    val preparedSource = prepareColorCardTracePaletteAnchors(sourceColors).sortedBy { it.l }
    val preparedTarget = prepareColorCardTracePaletteAnchors(targetColors).sortedBy { it.l }
    if (preparedSource.isEmpty() || preparedTarget.isEmpty()) return null

    val anchorCount = max(preparedSource.size, preparedTarget.size)
        .coerceIn(3, COLOR_CARD_TRACE_FILTER_MAX_ANCHORS)
    val sourceAnchors = resampleColorCardTraceLabs(sourceColors, anchorCount).sortedBy { it.l }
    val targetAnchors = resampleColorCardTraceLabs(targetColors, anchorCount).sortedBy { it.l }
    val lastIndex = (anchorCount - 1).coerceAtLeast(1)
    val stabilizedTargetAnchors = sourceAnchors.zip(targetAnchors).mapIndexed { index, (source, target) ->
        val edgeWeight = if (lastIndex <= 0) {
            0.0
        } else {
            abs((index.toDouble() / lastIndex.toDouble()) - 0.5) * 2.0
        }
        val lightnessBlend = lerpColorCardTrace(0.14, 0.06, edgeWeight)
        val targetChroma = sqrt((target.a * target.a) + (target.b * target.b))
        val chromaBlend = when {
            targetChroma < 10.0 -> 0.72
            targetChroma < 24.0 -> 0.84
            else -> 0.92
        }
        ColorCardTraceLab(
            l = lerpColorCardTrace(source.l, target.l, lightnessBlend),
            a = lerpColorCardTrace(source.a, target.a, chromaBlend),
            b = lerpColorCardTrace(source.b, target.b, chromaBlend)
        )
    }
    return ColorCardTraceAnchorMapping(
        sourceAnchors = sourceAnchors,
        targetAnchors = stabilizedTargetAnchors,
        sourceCentroid = colorCardTracePaletteCentroid(sourceAnchors),
        targetCentroid = colorCardTracePaletteCentroid(stabilizedTargetAnchors)
    )
}

private fun buildColorCardTraceGpuParams(
    panelMode: ColorCardTracePanelMode,
    sourceColors: List<Int>,
    selectedCandidate: ColorCardTraceFilterCandidate?,
    selectedHaldBitmap: Bitmap?,
    secondaryHaldBitmap: Bitmap?,
    haldVariantMix: Float,
    color: Float,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    colorAxis: ColorCardTraceColorAxis,
    paletteStrength: Float,
    faceRegions: List<ColorCardTraceFaceRegion>,
    portraitParams: ColorCardPortraitOptimizationParams,
    subjectRect: ColorCardTraceSubjectRect?,
    sourceAspect: Float,
    sourceTexelWidth: Float,
    sourceTexelHeight: Float
): ColorCardTraceGpuParams {
    if (selectedCandidate?.kind == ColorCardTraceFilterKind.PixelPoster) {
        val pixelPosterParams = resolveColorCardTracePixelPosterParams(
            candidate = selectedCandidate,
            paletteStrength = paletteStrength
        )
        return ColorCardTraceGpuParams(
            filterEnabled = false,
            anchorCount = 0,
            sourceAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            targetAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            blendStrength = 0f,
            maxLightnessShift = 0f,
            maxChromaShift = 0f,
            color = color.coerceIn(-1f, 1f),
            colorAxisA = colorAxis.aWeight,
            colorAxisB = colorAxis.bWeight,
            sharpen = sharpen.coerceIn(-1f, 1f),
            tone = tone.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            brightness = brightness.coerceIn(-1f, 1f),
            protectedFaceCount = faceRegions.size.coerceAtMost(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES),
            protectedFaces = packColorCardTraceGpuFaceRegions(faceRegions),
            portraitEnabled = portraitParams.enabled,
            portraitLightnessLift = portraitParams.lightnessLift,
            portraitSaturationBoost = portraitParams.saturationBoost,
            portraitSharpenRetention = portraitParams.sharpenRetention,
            portraitSoftenAmount = portraitParams.softenAmount,
            haldEnabled = false,
            haldLevel = 0,
            haldStrength = 0f,
            haldBitmap = null,
            haldSecondaryLevel = 0,
            haldSecondaryBitmap = null,
            haldVariantMix = 0f,
            pixelPosterEnabled = pixelPosterParams.enabled,
            pixelPosterUseSourceColor = pixelPosterParams.style == ColorCardTracePixelPosterStyle.SourceColor,
            pixelPosterContinuousTone = pixelPosterParams.style == ColorCardTracePixelPosterStyle.ContinuousDuotone,
            pixelPosterBlockCountLongSide = if (
                pixelPosterParams.style == ColorCardTracePixelPosterStyle.ContinuousDuotone
            ) {
                0f
            } else {
                pixelPosterParams.blockCountLongSide
            },
            pixelPosterThreshold = pixelPosterParams.threshold,
            pixelPosterContrast = pixelPosterParams.contrast,
            pixelPosterNoiseAmount = pixelPosterParams.noiseAmount,
            pixelPosterGridLineWidth = pixelPosterParams.gridLineWidth,
            pixelPosterGridLineOpacity = pixelPosterParams.gridLineOpacity,
            pixelPosterDetailBoost = pixelPosterParams.detailBoost,
            pixelPosterShadowDetailBias = pixelPosterParams.shadowDetailBias,
            pixelPosterStrength = pixelPosterParams.strength,
            pixelPosterDarkRed = AndroidColor.red(pixelPosterParams.darkColor) / 255f,
            pixelPosterDarkGreen = AndroidColor.green(pixelPosterParams.darkColor) / 255f,
            pixelPosterDarkBlue = AndroidColor.blue(pixelPosterParams.darkColor) / 255f,
            pixelPosterLightRed = AndroidColor.red(pixelPosterParams.lightColor) / 255f,
            pixelPosterLightGreen = AndroidColor.green(pixelPosterParams.lightColor) / 255f,
            pixelPosterLightBlue = AndroidColor.blue(pixelPosterParams.lightColor) / 255f,
            subjectFrameEnabled = false,
            subjectFrameRect = floatArrayOf(0f, 0f, 0f, 0f),
            subjectFrameBorderThickness = 0f,
            subjectFrameStrength = 0f,
            subjectFrameBorderRed = 0f,
            subjectFrameBorderGreen = 0f,
            subjectFrameBorderBlue = 0f,
            blackMistEnabled = false,
            blackMistStrength = 0f,
            sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
            sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
            sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
        )
    }
    if (selectedCandidate?.kind == ColorCardTraceFilterKind.SelectiveColor) {
        val selectiveColorParams = resolveColorCardTraceSelectiveColorParams(
            candidate = selectedCandidate,
            paletteStrength = paletteStrength
        )
        return ColorCardTraceGpuParams(
            filterEnabled = false,
            anchorCount = 0,
            sourceAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            targetAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            blendStrength = 0f,
            maxLightnessShift = 0f,
            maxChromaShift = 0f,
            color = color.coerceIn(-1f, 1f),
            colorAxisA = colorAxis.aWeight,
            colorAxisB = colorAxis.bWeight,
            sharpen = sharpen.coerceIn(-1f, 1f),
            tone = tone.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            brightness = brightness.coerceIn(-1f, 1f),
            protectedFaceCount = faceRegions.size.coerceAtMost(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES),
            protectedFaces = packColorCardTraceGpuFaceRegions(faceRegions),
            portraitEnabled = portraitParams.enabled,
            portraitLightnessLift = portraitParams.lightnessLift,
            portraitSaturationBoost = portraitParams.saturationBoost,
            portraitSharpenRetention = portraitParams.sharpenRetention,
            portraitSoftenAmount = portraitParams.softenAmount,
            haldEnabled = false,
            haldLevel = 0,
            haldStrength = 0f,
            haldBitmap = null,
            haldSecondaryLevel = 0,
            haldSecondaryBitmap = null,
            haldVariantMix = 0f,
            pixelPosterEnabled = false,
            pixelPosterUseSourceColor = false,
            pixelPosterBlockCountLongSide = 0f,
            pixelPosterThreshold = 0f,
            pixelPosterContrast = 0f,
            pixelPosterNoiseAmount = 0f,
            pixelPosterGridLineWidth = 0f,
            pixelPosterGridLineOpacity = 0f,
            pixelPosterDetailBoost = 0f,
            pixelPosterShadowDetailBias = 0f,
            pixelPosterStrength = 0f,
            pixelPosterDarkRed = 0f,
            pixelPosterDarkGreen = 0f,
            pixelPosterDarkBlue = 0f,
            pixelPosterLightRed = 0f,
            pixelPosterLightGreen = 0f,
            pixelPosterLightBlue = 0f,
            subjectFrameEnabled = false,
            subjectFrameRect = floatArrayOf(0f, 0f, 0f, 0f),
            subjectFrameBorderThickness = 0f,
            subjectFrameStrength = 0f,
            subjectFrameBorderRed = 0f,
            subjectFrameBorderGreen = 0f,
            subjectFrameBorderBlue = 0f,
            blackMistEnabled = selectiveColorParams.softMistStrength > 0.0001f,
            blackMistStrength = selectiveColorParams.softMistStrength,
            selectiveColorEnabled = selectiveColorParams.enabled,
            selectiveColorTargetHue = selectiveColorParams.targetHue,
            selectiveColorHueRange = selectiveColorParams.hueRange,
            selectiveColorSaturationFloor = selectiveColorParams.saturationFloor,
            selectiveColorStrength = selectiveColorParams.strength,
            sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
            sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
            sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
        )
    }
    if (selectedCandidate?.kind == ColorCardTraceFilterKind.SubjectFrame) {
        val resolvedSubjectRect = subjectRect ?: resolveColorCardTraceSubjectRect(
            sourceSessionId = 0L,
            sourceAspect = sourceAspect,
            faceRegions = faceRegions
        )
        return ColorCardTraceGpuParams(
            filterEnabled = false,
            anchorCount = 0,
            sourceAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            targetAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            blendStrength = 0f,
            maxLightnessShift = 0f,
            maxChromaShift = 0f,
            color = color.coerceIn(-1f, 1f),
            colorAxisA = colorAxis.aWeight,
            colorAxisB = colorAxis.bWeight,
            sharpen = sharpen.coerceIn(-1f, 1f),
            tone = tone.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            brightness = brightness.coerceIn(-1f, 1f),
            protectedFaceCount = faceRegions.size.coerceAtMost(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES),
            protectedFaces = packColorCardTraceGpuFaceRegions(faceRegions),
            portraitEnabled = portraitParams.enabled,
            portraitLightnessLift = portraitParams.lightnessLift,
            portraitSaturationBoost = portraitParams.saturationBoost,
            portraitSharpenRetention = portraitParams.sharpenRetention,
            portraitSoftenAmount = portraitParams.softenAmount,
            haldEnabled = false,
            haldLevel = 0,
            haldStrength = 0f,
            haldBitmap = null,
            haldSecondaryLevel = 0,
            haldSecondaryBitmap = null,
            haldVariantMix = 0f,
            pixelPosterEnabled = false,
            pixelPosterUseSourceColor = false,
            pixelPosterBlockCountLongSide = 0f,
            pixelPosterThreshold = 0f,
            pixelPosterContrast = 0f,
            pixelPosterNoiseAmount = 0f,
            pixelPosterGridLineWidth = 0f,
            pixelPosterGridLineOpacity = 0f,
            pixelPosterDetailBoost = 0f,
            pixelPosterShadowDetailBias = 0f,
            pixelPosterStrength = 0f,
            pixelPosterDarkRed = 0f,
            pixelPosterDarkGreen = 0f,
            pixelPosterDarkBlue = 0f,
            pixelPosterLightRed = 0f,
            pixelPosterLightGreen = 0f,
            pixelPosterLightBlue = 0f,
            subjectFrameEnabled = true,
            subjectFrameRect = floatArrayOf(
                resolvedSubjectRect.left,
                resolvedSubjectRect.top,
                resolvedSubjectRect.right,
                resolvedSubjectRect.bottom
            ),
            subjectFrameBorderThickness = COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_THICKNESS,
            subjectFrameStrength = paletteStrength.coerceIn(0f, 1f),
            subjectFrameBorderRed = AndroidColor.red(COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_COLOR) / 255f,
            subjectFrameBorderGreen = AndroidColor.green(COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_COLOR) / 255f,
            subjectFrameBorderBlue = AndroidColor.blue(COLOR_CARD_TRACE_SUBJECT_FRAME_BORDER_COLOR) / 255f,
            blackMistEnabled = false,
            blackMistStrength = 0f,
            sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
            sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
            sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
        )
    }
    if (selectedCandidate?.kind == ColorCardTraceFilterKind.BlackMist) {
        return ColorCardTraceGpuParams(
            filterEnabled = false,
            anchorCount = 0,
            sourceAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            targetAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            blendStrength = 0f,
            maxLightnessShift = 0f,
            maxChromaShift = 0f,
            color = color.coerceIn(-1f, 1f),
            colorAxisA = colorAxis.aWeight,
            colorAxisB = colorAxis.bWeight,
            sharpen = sharpen.coerceIn(-1f, 1f),
            tone = tone.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            brightness = brightness.coerceIn(-1f, 1f),
            protectedFaceCount = faceRegions.size.coerceAtMost(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES),
            protectedFaces = packColorCardTraceGpuFaceRegions(faceRegions),
            portraitEnabled = portraitParams.enabled,
            portraitLightnessLift = portraitParams.lightnessLift,
            portraitSaturationBoost = portraitParams.saturationBoost,
            portraitSharpenRetention = portraitParams.sharpenRetention,
            portraitSoftenAmount = portraitParams.softenAmount,
            haldEnabled = false,
            haldLevel = 0,
            haldStrength = 0f,
            haldBitmap = null,
            haldSecondaryLevel = 0,
            haldSecondaryBitmap = null,
            haldVariantMix = 0f,
            pixelPosterEnabled = false,
            pixelPosterUseSourceColor = false,
            pixelPosterBlockCountLongSide = 0f,
            pixelPosterThreshold = 0f,
            pixelPosterContrast = 0f,
            pixelPosterNoiseAmount = 0f,
            pixelPosterGridLineWidth = 0f,
            pixelPosterGridLineOpacity = 0f,
            pixelPosterDetailBoost = 0f,
            pixelPosterShadowDetailBias = 0f,
            pixelPosterStrength = 0f,
            pixelPosterDarkRed = 0f,
            pixelPosterDarkGreen = 0f,
            pixelPosterDarkBlue = 0f,
            pixelPosterLightRed = 0f,
            pixelPosterLightGreen = 0f,
            pixelPosterLightBlue = 0f,
            subjectFrameEnabled = false,
            subjectFrameRect = floatArrayOf(0f, 0f, 0f, 0f),
            subjectFrameBorderThickness = 0f,
            subjectFrameStrength = 0f,
            subjectFrameBorderRed = 0f,
            subjectFrameBorderGreen = 0f,
            subjectFrameBorderBlue = 0f,
            blackMistEnabled = true,
            blackMistStrength = paletteStrength.coerceIn(0f, 1f),
            sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
            sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
            sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
        )
    }
    if (selectedCandidate?.kind == ColorCardTraceFilterKind.HaldClut) {
        val resolvedHaldBlend = resolveColorCardTraceHaldBlend(
            candidate = selectedCandidate,
            paletteStrength = paletteStrength
        )
        return ColorCardTraceGpuParams(
            filterEnabled = false,
            anchorCount = 0,
            sourceAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            targetAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            blendStrength = 0f,
            maxLightnessShift = 0f,
            maxChromaShift = 0f,
            color = color.coerceIn(-1f, 1f),
            colorAxisA = colorAxis.aWeight,
            colorAxisB = colorAxis.bWeight,
            sharpen = sharpen.coerceIn(-1f, 1f),
            tone = tone.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            brightness = brightness.coerceIn(-1f, 1f),
            protectedFaceCount = faceRegions.size.coerceAtMost(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES),
            protectedFaces = packColorCardTraceGpuFaceRegions(faceRegions),
            portraitEnabled = portraitParams.enabled,
            portraitLightnessLift = portraitParams.lightnessLift,
            portraitSaturationBoost = portraitParams.saturationBoost,
            portraitSharpenRetention = portraitParams.sharpenRetention,
            portraitSoftenAmount = portraitParams.softenAmount,
            haldEnabled = selectedHaldBitmap != null && (resolvedHaldBlend.primaryHaldLevel > 0),
            haldLevel = resolvedHaldBlend.primaryHaldLevel.coerceAtLeast(0),
            haldStrength = paletteStrength.coerceIn(0f, 1f),
            haldBitmap = selectedHaldBitmap,
            haldSecondaryLevel = resolvedHaldBlend.secondaryHaldLevel.coerceAtLeast(0),
            haldSecondaryBitmap = secondaryHaldBitmap,
            haldVariantMix = haldVariantMix.coerceIn(0f, 1f),
            pixelPosterEnabled = false,
            pixelPosterUseSourceColor = false,
            pixelPosterBlockCountLongSide = 0f,
            pixelPosterThreshold = 0f,
            pixelPosterContrast = 0f,
            pixelPosterNoiseAmount = 0f,
            pixelPosterGridLineWidth = 0f,
            pixelPosterGridLineOpacity = 0f,
            pixelPosterDetailBoost = 0f,
            pixelPosterShadowDetailBias = 0f,
            pixelPosterStrength = 0f,
            pixelPosterDarkRed = 0f,
            pixelPosterDarkGreen = 0f,
            pixelPosterDarkBlue = 0f,
            pixelPosterLightRed = 0f,
            pixelPosterLightGreen = 0f,
            pixelPosterLightBlue = 0f,
            subjectFrameEnabled = false,
            subjectFrameRect = floatArrayOf(0f, 0f, 0f, 0f),
            subjectFrameBorderThickness = 0f,
            subjectFrameStrength = 0f,
            subjectFrameBorderRed = 0f,
            subjectFrameBorderGreen = 0f,
            subjectFrameBorderBlue = 0f,
            blackMistEnabled = false,
            blackMistStrength = 0f,
            sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
            sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
            sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
        )
    }

    val targetColors = selectedCandidate?.colors
    val matchScore = selectedCandidate?.score
    if (sourceColors.isEmpty() || targetColors.isNullOrEmpty()) {
        return ColorCardTraceGpuParams(
            filterEnabled = false,
            anchorCount = 0,
            sourceAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            targetAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            blendStrength = 0f,
            maxLightnessShift = 0f,
            maxChromaShift = 0f,
            color = 0f,
            colorAxisA = colorAxis.aWeight,
            colorAxisB = colorAxis.bWeight,
            sharpen = 0f,
            tone = 0f,
            saturation = 0f,
            brightness = 0f,
            protectedFaceCount = faceRegions.size.coerceAtMost(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES),
            protectedFaces = packColorCardTraceGpuFaceRegions(faceRegions),
            portraitEnabled = false,
            portraitLightnessLift = 0f,
            portraitSaturationBoost = 0f,
            portraitSharpenRetention = 1f,
            portraitSoftenAmount = 0f,
            haldEnabled = false,
            haldLevel = 0,
            haldStrength = 0f,
            haldBitmap = null,
            haldSecondaryLevel = 0,
            haldSecondaryBitmap = null,
            haldVariantMix = 0f,
            pixelPosterEnabled = false,
            pixelPosterUseSourceColor = false,
            pixelPosterBlockCountLongSide = 0f,
            pixelPosterThreshold = 0f,
            pixelPosterContrast = 0f,
            pixelPosterNoiseAmount = 0f,
            pixelPosterGridLineWidth = 0f,
            pixelPosterGridLineOpacity = 0f,
            pixelPosterDetailBoost = 0f,
            pixelPosterShadowDetailBias = 0f,
            pixelPosterStrength = 0f,
            pixelPosterDarkRed = 0f,
            pixelPosterDarkGreen = 0f,
            pixelPosterDarkBlue = 0f,
            pixelPosterLightRed = 0f,
            pixelPosterLightGreen = 0f,
            pixelPosterLightBlue = 0f,
            subjectFrameEnabled = false,
            subjectFrameRect = floatArrayOf(0f, 0f, 0f, 0f),
            subjectFrameBorderThickness = 0f,
            subjectFrameStrength = 0f,
            subjectFrameBorderRed = 0f,
            subjectFrameBorderGreen = 0f,
            subjectFrameBorderBlue = 0f,
            blackMistEnabled = false,
            blackMistStrength = 0f,
            sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
            sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
            sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
        )
    }

    val anchorMapping = if (panelMode == ColorCardTracePanelMode.Imitate) {
        buildColorCardImitateAnchorMapping(
            sourceColors = sourceColors,
            targetColors = targetColors
        )
    } else {
        buildColorCardTraceAnchorMapping(
            sourceColors = sourceColors,
            targetColors = targetColors
        )
    }
    val sourceAnchors = anchorMapping?.sourceAnchors.orEmpty()
    val targetAnchors = anchorMapping?.targetAnchors.orEmpty()
    if (sourceAnchors.isEmpty() || targetAnchors.isEmpty()) {
        return ColorCardTraceGpuParams(
            filterEnabled = false,
            anchorCount = 0,
            sourceAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            targetAnchors = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
            blendStrength = 0f,
            maxLightnessShift = 0f,
            maxChromaShift = 0f,
            color = 0f,
            colorAxisA = colorAxis.aWeight,
            colorAxisB = colorAxis.bWeight,
            sharpen = 0f,
            tone = 0f,
            saturation = 0f,
            brightness = 0f,
            protectedFaceCount = faceRegions.size.coerceAtMost(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES),
            protectedFaces = packColorCardTraceGpuFaceRegions(faceRegions),
            portraitEnabled = false,
            portraitLightnessLift = 0f,
            portraitSaturationBoost = 0f,
            portraitSharpenRetention = 1f,
            portraitSoftenAmount = 0f,
            haldEnabled = false,
            haldLevel = 0,
            haldStrength = 0f,
            haldBitmap = null,
            haldSecondaryLevel = 0,
            haldSecondaryBitmap = null,
            haldVariantMix = 0f,
            pixelPosterEnabled = false,
            pixelPosterUseSourceColor = false,
            pixelPosterBlockCountLongSide = 0f,
            pixelPosterThreshold = 0f,
            pixelPosterContrast = 0f,
            pixelPosterNoiseAmount = 0f,
            pixelPosterGridLineWidth = 0f,
            pixelPosterGridLineOpacity = 0f,
            pixelPosterDetailBoost = 0f,
            pixelPosterShadowDetailBias = 0f,
            pixelPosterStrength = 0f,
            pixelPosterDarkRed = 0f,
            pixelPosterDarkGreen = 0f,
            pixelPosterDarkBlue = 0f,
            pixelPosterLightRed = 0f,
            pixelPosterLightGreen = 0f,
            pixelPosterLightBlue = 0f,
            subjectFrameEnabled = false,
            subjectFrameRect = floatArrayOf(0f, 0f, 0f, 0f),
            subjectFrameBorderThickness = 0f,
            subjectFrameStrength = 0f,
            subjectFrameBorderRed = 0f,
            subjectFrameBorderGreen = 0f,
            subjectFrameBorderBlue = 0f,
            blackMistEnabled = false,
            blackMistStrength = 0f,
            sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
            sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
            sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
        )
    }
    val anchorCount = min(sourceAnchors.size, targetAnchors.size)
    val resolvedMatchScore = if (panelMode == ColorCardTracePanelMode.Imitate) {
        scoreColorCardImitateFilterMatch(
            sourceColors = sourceColors,
            targetColors = targetColors
        )
    } else {
        matchScore ?: scoreColorCardTraceFilterMatch(
            sourceColors = sourceColors,
            targetColors = targetColors
        )
    }
    val mappingProfile = if (panelMode == ColorCardTracePanelMode.Imitate) {
        deriveColorCardImitateMappingProfile(resolvedMatchScore)
    } else {
        deriveColorCardTraceMappingProfile(resolvedMatchScore)
    }

    val safePaletteStrength = paletteStrength.coerceIn(0f, 1f)
    return ColorCardTraceGpuParams(
        filterEnabled = true,
        anchorCount = anchorCount,
        sourceAnchors = packColorCardTraceGpuAnchors(sourceAnchors),
        targetAnchors = packColorCardTraceGpuAnchors(targetAnchors),
        blendStrength = (mappingProfile.blendStrength * safePaletteStrength).toFloat(),
        maxLightnessShift = (mappingProfile.maxLightnessShift * safePaletteStrength).toFloat(),
        maxChromaShift = (mappingProfile.maxChromaShift * safePaletteStrength).toFloat(),
        color = color.coerceIn(-1f, 1f),
        colorAxisA = colorAxis.aWeight,
        colorAxisB = colorAxis.bWeight,
        sharpen = sharpen.coerceIn(-1f, 1f),
        tone = tone.coerceIn(-1f, 1f),
        saturation = saturation.coerceIn(-1f, 1f),
        brightness = brightness.coerceIn(-1f, 1f),
        protectedFaceCount = faceRegions.size.coerceAtMost(COLOR_CARD_TRACE_SKIN_PROTECTION_MAX_FACES),
        protectedFaces = packColorCardTraceGpuFaceRegions(faceRegions),
        portraitEnabled = portraitParams.enabled,
        portraitLightnessLift = portraitParams.lightnessLift,
        portraitSaturationBoost = portraitParams.saturationBoost,
        portraitSharpenRetention = portraitParams.sharpenRetention,
        portraitSoftenAmount = portraitParams.softenAmount,
        haldEnabled = false,
        haldLevel = 0,
        haldStrength = 0f,
        haldBitmap = null,
        haldSecondaryLevel = 0,
        haldSecondaryBitmap = null,
        haldVariantMix = 0f,
        pixelPosterEnabled = false,
        pixelPosterUseSourceColor = false,
        pixelPosterBlockCountLongSide = 0f,
        pixelPosterThreshold = 0f,
        pixelPosterContrast = 0f,
        pixelPosterNoiseAmount = 0f,
        pixelPosterGridLineWidth = 0f,
        pixelPosterGridLineOpacity = 0f,
        pixelPosterDetailBoost = 0f,
        pixelPosterShadowDetailBias = 0f,
        pixelPosterStrength = 0f,
        pixelPosterDarkRed = 0f,
        pixelPosterDarkGreen = 0f,
        pixelPosterDarkBlue = 0f,
        pixelPosterLightRed = 0f,
        pixelPosterLightGreen = 0f,
        pixelPosterLightBlue = 0f,
        subjectFrameEnabled = false,
        subjectFrameRect = floatArrayOf(0f, 0f, 0f, 0f),
        subjectFrameBorderThickness = 0f,
        subjectFrameStrength = 0f,
        subjectFrameBorderRed = 0f,
        subjectFrameBorderGreen = 0f,
        subjectFrameBorderBlue = 0f,
        blackMistEnabled = false,
        blackMistStrength = 0f,
        sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
        sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
        sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
    )
}

internal suspend fun renderColorCardTraceModeBitmap(
    context: Context,
    sourceFile: File,
    traceState: ColorCardTraceFilterState,
    maxSide: Int = COLOR_CARD_TRACE_FILTER_SAVE_MAX_SIDE
): Bitmap? {
    if (!traceState.canSaveRenderedImage) return null

    return withContext(Dispatchers.Default) {
        val sourceBitmap = decodeColorCardTraceRenderSourceBitmap(
            sourceFile = sourceFile,
            maxSide = maxSide
        ) ?: return@withContext null

        try {
            renderColorCardTraceBitmapForState(
                context = context.applicationContext,
                sourceBitmap = sourceBitmap,
                traceState = traceState
            )
        } finally {
            sourceBitmap.recycle()
        }
    }
}

internal suspend fun renderColorCardTraceBitmapForExport(
    context: Context,
    sourceBitmap: Bitmap,
    traceState: ColorCardTraceFilterState
): Bitmap {
    val workingBitmap = if (sourceBitmap.config == Bitmap.Config.ARGB_8888) {
        sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
    val rendered = runCatching {
        renderColorCardTraceBitmapForState(
            context = context.applicationContext,
            sourceBitmap = workingBitmap,
            traceState = traceState
        )
    }.getOrNull()
    if (rendered != null) {
        if (rendered !== workingBitmap && !workingBitmap.isRecycled) {
            workingBitmap.recycle()
        }
        return rendered
    }
    return workingBitmap
}

internal suspend fun renderColorCardTraceDiagnosticBitmap(
    context: Context,
    traceState: ColorCardTraceFilterState,
    maxSide: Int = 112
): Bitmap? {
    if (!traceState.canSaveRenderedImage) return null
    val previewBitmap = traceState.previewSourceBitmap
        ?.takeIf { bitmap -> !bitmap.isRecycled }
        ?: return null

    return withContext(Dispatchers.Default) {
        val workingBitmap = scaleColorCardTraceBitmapForDiagnostics(
            sourceBitmap = previewBitmap,
            maxSide = maxSide
        )
        try {
            renderColorCardTraceBitmapForState(
                context = context.applicationContext,
                sourceBitmap = workingBitmap,
                traceState = traceState
            )
        } finally {
            if (workingBitmap !== previewBitmap && !workingBitmap.isRecycled) {
                workingBitmap.recycle()
            }
        }
    }
}

private suspend fun renderColorCardTraceBitmapForState(
    context: Context,
    sourceBitmap: Bitmap,
    traceState: ColorCardTraceFilterState
): Bitmap? {
    return when (traceState.mode) {
        ColorCardTracePanelMode.Picker -> null
        ColorCardTracePanelMode.Recipe -> null
        ColorCardTracePanelMode.Filter -> renderColorCardTraceFilterModeBitmap(
            context = context,
            sourceBitmap = sourceBitmap,
            traceState = traceState
        )
        ColorCardTracePanelMode.Trace,
        ColorCardTracePanelMode.Imitate -> {
            val candidate = traceState.selectedCandidate
                ?.takeIf { it.kind == ColorCardTraceFilterKind.Palette }
                ?: return null
            val portraitAvailable = resolveColorCardTracePortraitFeatureAvailable(
                panelMode = traceState.mode,
                selectedCandidate = candidate,
                faceRegions = traceState.faceRegions
            )
            val basePortraitParams = resolveColorCardPortraitOptimizationParams(
                panelMode = traceState.mode,
                selectedCandidate = candidate,
                faceRegions = traceState.faceRegions
            )
            val portraitParams = resolveColorCardTracePortraitFeatureParams(
                enabled = portraitAvailable,
                faceRegions = traceState.faceRegions,
                baseParams = basePortraitParams,
                adjustments = traceState.portraitAdjustments
            )
            renderColorCardTraceFilterBitmap(
                sourceBitmap = sourceBitmap,
                sourceColors = traceState.sourceColors,
                targetColors = candidate.colors,
                distributionTransfer = if (traceState.mode == ColorCardTracePanelMode.Imitate) {
                    traceState.imitateTransferProfile
                } else {
                    null
                },
                color = traceState.color,
                colorAxis = traceState.colorAxis,
                sharpen = traceState.sharpen,
                tone = traceState.tone,
                saturation = traceState.saturation,
                brightness = traceState.brightness,
                paletteStrength = traceState.paletteStrength,
                faceRegions = traceState.faceRegions,
                portraitParams = portraitParams
            )
        }
    }
}

private fun scaleColorCardTraceBitmapForDiagnostics(
    sourceBitmap: Bitmap,
    maxSide: Int
): Bitmap {
    val safeMaxSide = maxSide.coerceAtLeast(24)
    val currentMaxSide = max(sourceBitmap.width, sourceBitmap.height).coerceAtLeast(1)
    if (currentMaxSide <= safeMaxSide && sourceBitmap.config == Bitmap.Config.ARGB_8888) {
        return sourceBitmap
    }
    val scale = safeMaxSide.toFloat() / currentMaxSide.toFloat()
    val targetWidth = if (currentMaxSide <= safeMaxSide) {
        sourceBitmap.width
    } else {
        (sourceBitmap.width * scale).roundToInt().coerceAtLeast(1)
    }
    val targetHeight = if (currentMaxSide <= safeMaxSide) {
        sourceBitmap.height
    } else {
        (sourceBitmap.height * scale).roundToInt().coerceAtLeast(1)
    }
    val scaled = if (
        targetWidth != sourceBitmap.width ||
        targetHeight != sourceBitmap.height
    ) {
        Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true)
    } else {
        sourceBitmap
    }
    if (scaled.config == Bitmap.Config.ARGB_8888) {
        return scaled
    }
    return scaled.copy(Bitmap.Config.ARGB_8888, false).also { copied ->
        if (scaled !== sourceBitmap && copied !== scaled) {
            scaled.recycle()
        }
    }
}

private suspend fun decodeColorCardTraceRenderSourceBitmap(
    sourceFile: File,
    maxSide: Int
): Bitmap? = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    val safeMaxSide = maxSide.coerceAtLeast(1)
    val largestSourceSide = max(bounds.outWidth, bounds.outHeight)
    val sampleSize = buildColorCardTraceDecodeSampleSize(
        sourceWidth = bounds.outWidth,
        sourceHeight = bounds.outHeight,
        maxSide = safeMaxSide
    )
    val decoded = BitmapFactory.decodeFile(
        sourceFile.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    ) ?: return@withContext null
    val oriented = applyColorCardTraceExifOrientation(
        bitmap = decoded,
        orientation = readColorCardTraceExifOrientation(sourceFile)
    )

    if (largestSourceSide <= safeMaxSide && max(oriented.width, oriented.height) <= safeMaxSide) {
        return@withContext oriented
    }

    val currentLargestSide = max(oriented.width, oriented.height).coerceAtLeast(1)
    if (currentLargestSide <= safeMaxSide) {
        return@withContext oriented
    }

    val scale = safeMaxSide.toFloat() / currentLargestSide.toFloat()
    val targetWidth = (oriented.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (oriented.height * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(oriented, targetWidth, targetHeight, true)
    if (scaled !== oriented) {
        oriented.recycle()
    }
    scaled
}

private fun readColorCardTraceExifOrientation(sourceFile: File): Int {
    return runCatching {
        ExifInterface(sourceFile.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
}

private fun applyColorCardTraceExifOrientation(
    bitmap: Bitmap,
    orientation: Int
): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) {
        bitmap.recycle()
    }
    return rotated
}

private fun buildColorCardTraceDecodeSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    maxSide: Int
): Int {
    var sampleSize = 1
    var currentWidth = sourceWidth.coerceAtLeast(1)
    var currentHeight = sourceHeight.coerceAtLeast(1)
    while (max(currentWidth, currentHeight) > maxSide * 2) {
        sampleSize *= 2
        currentWidth = (sourceWidth / sampleSize).coerceAtLeast(1)
        currentHeight = (sourceHeight / sampleSize).coerceAtLeast(1)
    }
    return sampleSize.coerceAtLeast(1)
}

private fun packColorCardTraceGpuAnchors(
    anchors: List<ColorCardTraceLab>
): FloatArray {
    val packed = FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3)
    val fallback = anchors.lastOrNull() ?: ColorCardTraceLab(50.0, 0.0, 0.0)
    repeat(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS) { index ->
        val anchor = anchors.getOrNull(index) ?: fallback
        val baseIndex = index * 3
        packed[baseIndex] = anchor.l.toFloat()
        packed[baseIndex + 1] = anchor.a.toFloat()
        packed[baseIndex + 2] = anchor.b.toFloat()
    }
    return packed
}

private fun deriveColorCardTraceMappingProfile(
    matchScore: Double
): ColorCardTraceMappingProfile {
    // Candidate quality should only tune guard rails, not collapse the effect to near-zero.
    val normalizedMismatch = ((matchScore - 8.0) / 34.0).coerceIn(0.0, 1.0)
    val similarity = 1.0 - normalizedMismatch
    return ColorCardTraceMappingProfile(
        blendStrength = lerpColorCardTrace(0.42, 0.84, similarity),
        maxLightnessShift = lerpColorCardTrace(14.0, 32.0, similarity),
        maxChromaShift = lerpColorCardTrace(18.0, 38.0, similarity)
    )
}

private fun applyColorCardTraceMapping(
    inputLab: ColorCardTraceLab,
    sourceAnchors: List<ColorCardTraceLab>,
    targetAnchors: List<ColorCardTraceLab>,
    mappingProfile: ColorCardTraceMappingProfile,
    effectScale: Double = 1.0
): ColorCardTraceLab {
    if (sourceAnchors.isEmpty() || targetAnchors.isEmpty()) return inputLab
    val safeEffectScale = effectScale.coerceIn(0.0, 1.0)
    if (safeEffectScale <= 0.0001) return inputLab

    val inputChroma = sqrt((inputLab.a * inputLab.a) + (inputLab.b * inputLab.b))
    var weightedDeltaL = 0.0
    var weightedDeltaA = 0.0
    var weightedDeltaB = 0.0
    var weightSum = 0.0

    sourceAnchors.zip(targetAnchors).forEach { (sourceAnchor, targetAnchor) ->
        val deltaL = targetAnchor.l - sourceAnchor.l
        val deltaA = targetAnchor.a - sourceAnchor.a
        val deltaB = targetAnchor.b - sourceAnchor.b
        val distanceL = (inputLab.l - sourceAnchor.l) * 0.78
        val distanceA = inputLab.a - sourceAnchor.a
        val distanceB = inputLab.b - sourceAnchor.b
        val distanceSquared =
            (distanceL * distanceL) +
                (distanceA * distanceA) +
                (distanceB * distanceB)
        val weight = 1.0 / (distanceSquared + 144.0)
        weightedDeltaL += deltaL * weight
        weightedDeltaA += deltaA * weight
        weightedDeltaB += deltaB * weight
        weightSum += weight
    }

    val safeWeightSum = weightSum.takeIf { it > 0.0 } ?: 1.0
    val averagedDeltaL = weightedDeltaL / safeWeightSum
    val averagedDeltaA = weightedDeltaA / safeWeightSum
    val averagedDeltaB = weightedDeltaB / safeWeightSum
    val chromaInfluence = when {
        inputChroma < 8.0 -> 0.58
        inputChroma < 24.0 -> 0.78
        else -> 0.96
    }
    val lightnessInfluence = if (inputChroma < 8.0) 0.90 else 0.98
    val blendStrength = mappingProfile.blendStrength * safeEffectScale
    val maxLightnessShift = mappingProfile.maxLightnessShift * safeEffectScale
    val maxChromaShift = mappingProfile.maxChromaShift * safeEffectScale
    val mappedLab = ColorCardTraceLab(
        l = (
            inputLab.l + (averagedDeltaL * lightnessInfluence)
            ).coerceIn(0.0, 100.0),
        a = inputLab.a + (averagedDeltaA * chromaInfluence),
        b = inputLab.b + (averagedDeltaB * chromaInfluence)
    )
    val softenedLab = ColorCardTraceLab(
        l = lerpColorCardTrace(inputLab.l, mappedLab.l, blendStrength),
        a = lerpColorCardTrace(inputLab.a, mappedLab.a, blendStrength),
        b = lerpColorCardTrace(inputLab.b, mappedLab.b, blendStrength)
    )
    return ColorCardTraceLab(
        l = (
            inputLab.l +
                (softenedLab.l - inputLab.l)
                    .coerceIn(-maxLightnessShift, maxLightnessShift)
            ).coerceIn(0.0, 100.0),
        a = inputLab.a +
            (softenedLab.a - inputLab.a)
                .coerceIn(-maxChromaShift, maxChromaShift),
        b = inputLab.b +
            (softenedLab.b - inputLab.b)
                .coerceIn(-maxChromaShift, maxChromaShift)
    )
}

internal fun renderColorCardImitatePreviewBitmapForOptimization(
    sourceBitmap: Bitmap,
    sourceColors: List<Int>,
    targetColors: List<Int>,
    transferProfile: ColorCardImitateTransferProfile,
    color: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    sharpen: Float,
    paletteStrength: Float
): Bitmap {
    return renderColorCardTraceFilterBitmap(
        sourceBitmap = sourceBitmap,
        sourceColors = sourceColors,
        targetColors = targetColors,
        distributionTransfer = transferProfile,
        color = color,
        sharpen = sharpen,
        tone = tone,
        saturation = saturation,
        brightness = brightness,
        paletteStrength = paletteStrength,
        faceRegions = emptyList(),
        portraitParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED
    )
}

internal fun buildColorCardTraceGpuMapping(
    sourceColors: List<Int>,
    targetColors: List<Int>,
    paletteStrength: Float,
    blendStrengthScale: Float = 1f,
    lightnessShiftScale: Float = 1f,
    chromaShiftScale: Float = 1f
): ColorCardTraceGpuMapping? {
    val anchorMapping = buildColorCardTraceAnchorMapping(
        sourceColors = sourceColors,
        targetColors = targetColors
    ) ?: return null
    val resolvedMatchScore = scoreColorCardTraceFilterMatch(
        sourceColors = sourceColors,
        targetColors = targetColors
    )
    val mappingProfile = deriveColorCardTraceMappingProfile(resolvedMatchScore)
    val safeStrength = paletteStrength.coerceIn(0f, 1f)
    val sourceAnchors = anchorMapping.sourceAnchors
    val targetAnchors = anchorMapping.targetAnchors
    val anchorCount = min(
        sourceAnchors.size,
        targetAnchors.size
    ).coerceIn(0, COLOR_CARD_TRACE_FILTER_MAX_ANCHORS)
    if (anchorCount == 0) return null
    return ColorCardTraceGpuMapping(
        anchorCount = anchorCount,
        sourceAnchors = packColorCardTraceGpuAnchors(sourceAnchors),
        targetAnchors = packColorCardTraceGpuAnchors(targetAnchors),
        blendStrength = (
            mappingProfile.blendStrength *
                safeStrength *
                blendStrengthScale.coerceAtLeast(0f)
            ).toFloat(),
        maxLightnessShift = (
            mappingProfile.maxLightnessShift *
                safeStrength *
                lightnessShiftScale.coerceAtLeast(0f)
            ).toFloat(),
        maxChromaShift = (
            mappingProfile.maxChromaShift *
                safeStrength *
                chromaShiftScale.coerceAtLeast(0f)
            ).toFloat()
    )
}

private fun renderColorCardTraceFilterBitmap(
    sourceBitmap: Bitmap,
    sourceColors: List<Int>,
    targetColors: List<Int>,
    distributionTransfer: ColorCardImitateTransferProfile? = null,
    color: Float,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    paletteStrength: Float,
    faceRegions: List<ColorCardTraceFaceRegion>,
    portraitParams: ColorCardPortraitOptimizationParams
): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val detailAdjustedSourcePixels = applyColorCardTraceSourceDetailAdjustments(
        sourcePixels = sourcePixels,
        width = width,
        height = height,
        sharpen = sharpen,
        faceRegions = faceRegions,
        portraitParams = portraitParams
    )

    val anchorMapping = if (distributionTransfer != null) {
        buildColorCardImitateAnchorMapping(
            sourceColors = sourceColors,
            targetColors = targetColors
        )
    } else {
        buildColorCardTraceAnchorMapping(
            sourceColors = sourceColors,
            targetColors = targetColors
        )
    }
    val sourceAnchors = anchorMapping?.sourceAnchors.orEmpty()
    val targetAnchors = anchorMapping?.targetAnchors.orEmpty()
    if (sourceAnchors.isEmpty() || targetAnchors.isEmpty()) {
        return sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
    val baseProfile = if (distributionTransfer != null) {
        deriveColorCardImitateMappingProfile(
            scoreColorCardImitateFilterMatch(
                sourceColors = sourceColors,
                targetColors = targetColors
            )
        )
    } else {
        deriveColorCardTraceMappingProfile(
            scoreColorCardTraceFilterMatch(
                sourceColors = sourceColors,
                targetColors = targetColors
            )
        )
    }
    val safeStrength = paletteStrength.coerceIn(0f, 1f).toDouble()
    val effectiveSourceAnchors = if (distributionTransfer != null) {
        sourceAnchors.map { anchor ->
            applyColorCardImitateDistributionTransfer(
                inputLab = anchor,
                profile = distributionTransfer,
                lightnessStrength = (safeStrength * 0.94).coerceIn(0.0, 1.0),
                chromaStrength = (safeStrength * 0.72).coerceIn(0.0, 1.0)
            )
        }
    } else {
        sourceAnchors
    }
    val antiBandingMix = resolveColorCardTraceAntiBandingMix(
        color = color,
        tone = tone,
        saturation = saturation,
        brightness = brightness,
        paletteStrength = paletteStrength
    )
    val mappingProfile = baseProfile.copy(
        blendStrength = baseProfile.blendStrength * safeStrength,
        maxLightnessShift = baseProfile.maxLightnessShift * safeStrength,
        maxChromaShift = baseProfile.maxChromaShift * safeStrength
    )

    val xNormalizer = (width - 1).coerceAtLeast(1).toFloat()
    val yNormalizer = (height - 1).coerceAtLeast(1).toFloat()
    for (y in 0 until height) {
        val normalizedY = if (height <= 1) 0.5f else y / yNormalizer
        val rowOffset = y * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val colorInt = detailAdjustedSourcePixels[index]
            val alpha = AndroidColor.alpha(colorInt)
            if (alpha == 0) {
                outputPixels[index] = colorInt
                continue
            }

            val sourceRed = AndroidColor.red(colorInt)
            val sourceGreen = AndroidColor.green(colorInt)
            val sourceBlue = AndroidColor.blue(colorInt)
            val protectionAmount = colorCardTraceSkinProtectionAmount(
                normalizedX = if (width <= 1) 0.5f else x / xNormalizer,
                normalizedY = normalizedY,
                red = sourceRed,
                green = sourceGreen,
                blue = sourceBlue,
                faceRegions = faceRegions
            )
            val paletteEffectScale = 1.0 - (
                protectionAmount.toDouble() *
                    COLOR_CARD_TRACE_SKIN_PROTECTION_PALETTE_RECOVERY.toDouble()
                )
            val chromaScale = 1f - (
                protectionAmount * COLOR_CARD_TRACE_SKIN_PROTECTION_CREATIVE_RECOVERY
                )
            val tonalScale = 1f - (
                protectionAmount * COLOR_CARD_TRACE_SKIN_PROTECTION_TONAL_RECOVERY
                )
            val inputLab = colorCardTraceLabFromRgb(
                red = sourceRed,
                green = sourceGreen,
                blue = sourceBlue
            )
            val imitatePixelGuard = distributionTransfer?.let {
                resolveColorCardImitatePixelGuard(
                    inputLab = inputLab,
                    sourceAnchors = sourceAnchors
                )
            }
            val distributionTransferredLab = distributionTransfer?.let { profile ->
                applyColorCardImitateDistributionTransfer(
                    inputLab = inputLab,
                    profile = profile,
                    lightnessStrength = (
                        paletteEffectScale *
                            safeStrength *
                            (imitatePixelGuard?.lightnessTransferScale ?: 1.0)
                        ).coerceIn(0.0, 1.0),
                    chromaStrength = (
                        paletteEffectScale *
                            safeStrength *
                            0.74 *
                            (imitatePixelGuard?.chromaTransferScale ?: 1.0)
                        ).coerceIn(0.0, 1.0)
                )
            } ?: inputLab
            val mappedLab = applyColorCardTraceMapping(
                inputLab = distributionTransferredLab,
                sourceAnchors = effectiveSourceAnchors,
                targetAnchors = targetAnchors,
                mappingProfile = mappingProfile,
                effectScale = paletteEffectScale * (imitatePixelGuard?.paletteMappingScale ?: 1.0)
            )
            val adjustedRgb = applyColorCardTraceCreativeAdjustments(
                lab = mappedLab,
                color = color,
                tone = tone,
                saturation = saturation,
                brightness = brightness,
                colorAxis = colorAxis,
                chromaScale = chromaScale,
                tonalScale = tonalScale,
                portraitMask = protectionAmount,
                portraitParams = portraitParams
            )
            val enhancedRgb = applyColorCardTraceLightPresenceEnhancement(
                rgb = adjustedRgb,
                x = x,
                y = y,
                width = width,
                height = height,
                referencePixels = sourcePixels
            )
            val finalRgb = applyColorCardTraceAntiBanding(
                rgb = enhancedRgb,
                x = x,
                y = y,
                width = width,
                height = height,
                referencePixels = sourcePixels,
                mix = antiBandingMix
            )
            outputPixels[index] = AndroidColor.argb(
                alpha,
                finalRgb.red,
                finalRgb.green,
                finalRgb.blue
            )
        }
    }

    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun applyColorCardImitateDistributionTransfer(
    inputLab: ColorCardTraceLab,
    profile: ColorCardImitateTransferProfile,
    lightnessStrength: Double,
    chromaStrength: Double = lightnessStrength
): ColorCardTraceLab {
    val safeLightnessStrength = lightnessStrength.coerceIn(0.0, 1.0)
    val safeChromaStrength = chromaStrength.coerceIn(0.0, 1.0)
    if (safeLightnessStrength <= 0.0001 && safeChromaStrength <= 0.0001) return inputLab

    val normalizedLightness = (inputLab.l / 100.0).coerceIn(0.0, 1.0)
    val shadowWeight = ((0.42 - normalizedLightness) / 0.32).coerceIn(0.0, 1.0)
    val highlightWeight = ((normalizedLightness - 0.58) / 0.32).coerceIn(0.0, 1.0)
    val midtoneWeight = (1.0 - max(shadowWeight, highlightWeight)).coerceIn(0.0, 1.0)
    val weightSum = (shadowWeight + midtoneWeight + highlightWeight).coerceAtLeast(0.0001)

    val globalTransferred = applyColorCardImitateTransferBand(inputLab, profile.global)
    val shadowTransferred = applyColorCardImitateTransferBand(inputLab, profile.shadows)
    val midtoneTransferred = applyColorCardImitateTransferBand(inputLab, profile.midtones)
    val highlightTransferred = applyColorCardImitateTransferBand(inputLab, profile.highlights)
    val curveTransferredLightness = applyColorCardImitateToneCurve(
        normalizedLightness = normalizedLightness,
        curve = profile.toneCurve
    ) * 100.0

    val tonalTransferred = ColorCardTraceLab(
        l = (
            (shadowTransferred.l * shadowWeight) +
                (midtoneTransferred.l * midtoneWeight) +
                (highlightTransferred.l * highlightWeight)
            ) / weightSum,
        a = (
            (shadowTransferred.a * shadowWeight) +
                (midtoneTransferred.a * midtoneWeight) +
                (highlightTransferred.a * highlightWeight)
            ) / weightSum,
        b = (
            (shadowTransferred.b * shadowWeight) +
                (midtoneTransferred.b * midtoneWeight) +
                (highlightTransferred.b * highlightWeight)
            ) / weightSum
    )
    val fusedTransferred = ColorCardTraceLab(
        l = lerpColorCardTrace(
            lerpColorCardTrace(globalTransferred.l, tonalTransferred.l, 0.62),
            curveTransferredLightness,
            0.48
        ),
        a = lerpColorCardTrace(globalTransferred.a, tonalTransferred.a, 0.62),
        b = lerpColorCardTrace(globalTransferred.b, tonalTransferred.b, 0.62)
    )
    return ColorCardTraceLab(
        l = lerpColorCardTrace(
            inputLab.l,
            fusedTransferred.l.coerceIn(0.0, 100.0),
            safeLightnessStrength
        ),
        a = lerpColorCardTrace(inputLab.a, fusedTransferred.a, safeChromaStrength),
        b = lerpColorCardTrace(inputLab.b, fusedTransferred.b, safeChromaStrength)
    )
}

private fun resolveColorCardImitatePixelGuard(
    inputLab: ColorCardTraceLab,
    sourceAnchors: List<ColorCardTraceLab>
): ColorCardImitatePixelGuard {
    val chroma = sqrt((inputLab.a * inputLab.a) + (inputLab.b * inputLab.b))
    val neutralScale = resolveColorCardImitateNeutralScale(chroma)
    val highlightScale = resolveColorCardImitateHighlightScale(inputLab.l)
    val shadowScale = resolveColorCardImitateShadowScale(inputLab.l)
    val anchorAffinity = resolveColorCardImitateAnchorAffinity(
        inputLab = inputLab,
        sourceAnchors = sourceAnchors
    )
    val lightnessTransferScale = (
        (0.70 + (neutralScale * 0.30)) *
            (0.82 + (highlightScale * 0.18)) *
            (0.88 + (shadowScale * 0.12))
        ).coerceIn(0.62, 1.0)
    val chromaTransferScale = (
        neutralScale *
            (0.34 + (highlightScale * 0.66)) *
            (0.70 + (shadowScale * 0.30)) *
            (0.58 + (anchorAffinity * 0.42))
        ).coerceIn(0.12, 1.0)
    val paletteMappingScale = (
        neutralScale *
            (0.28 + (highlightScale * 0.72)) *
            (0.62 + (shadowScale * 0.38)) *
            (0.34 + (anchorAffinity * 0.66))
        ).coerceIn(0.06, 1.0)
    return ColorCardImitatePixelGuard(
        lightnessTransferScale = lightnessTransferScale,
        chromaTransferScale = chromaTransferScale,
        paletteMappingScale = paletteMappingScale
    )
}

private fun resolveColorCardImitateNeutralScale(
    chroma: Double
): Double {
    return when {
        chroma <= 6.0 -> 0.22
        chroma <= 14.0 -> lerpColorCardTrace(0.22, 0.52, (chroma - 6.0) / 8.0)
        chroma <= 28.0 -> lerpColorCardTrace(0.52, 1.0, (chroma - 14.0) / 14.0)
        else -> 1.0
    }
}

@Composable
internal fun ColorCardTraceCompareButton(
    pressed: Boolean,
    enabled: Boolean,
    primaryText: Color,
    secondaryText: Color,
    contentDescription: String,
    onPressedChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val contentTint = when {
        !enabled -> secondaryText.copy(alpha = 0.62f)
        pressed -> primaryText
        else -> primaryText.copy(alpha = 0.86f)
    }
    val containerColor = when {
        pressed -> primaryText.copy(alpha = 0.18f)
        enabled -> primaryText.copy(alpha = 0.09f)
        else -> secondaryText.copy(alpha = 0.06f)
    }
    val borderColor = when {
        pressed -> primaryText.copy(alpha = 0.30f)
        enabled -> primaryText.copy(alpha = 0.14f)
        else -> secondaryText.copy(alpha = 0.08f)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressedChange(true)
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        onPressedChange(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (pressed) {
                COLOR_CARD_TRACE_COMPARE_ICON_FILLED
            } else {
                COLOR_CARD_TRACE_COMPARE_ICON_OUTLINE
            },
            contentDescription = contentDescription,
            tint = contentTint,
            modifier = Modifier.size(18.dp)
        )
        if (!enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
internal fun BoxScope.ColorCardTraceEditingTopBar(
    isDark: Boolean,
    panelMode: ColorCardTracePanelMode,
    saveState: ColorCardTraceSaveImageState,
    saveEnabled: Boolean,
    saveMenuExpanded: Boolean = false,
    saveMenuActions: List<ColorCardTraceSaveMenuAction> = emptyList(),
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSaveMenuDismiss: () -> Unit = {},
    onSaveMenuAction: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val iconTint = if (isDark) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val saveLabel = if (panelMode == ColorCardTracePanelMode.Picker) {
        colorCardStrings.saveColorCard
    } else {
        colorCardStrings.saveImage
    }

    if (saveMenuExpanded && saveMenuActions.isNotEmpty()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(24f)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onSaveMenuDismiss
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 60.dp, end = 16.dp)
                    .widthIn(min = 124.dp, max = 164.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(18.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerColor.copy(alpha = 0.995f))
                    .border(
                        width = 1.dp,
                        color = iconTint.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(vertical = 4.dp)
            ) {
                saveMenuActions.forEachIndexed { index, action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSaveMenuDismiss()
                                onSaveMenuAction(action.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = action.title,
                                color = iconTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            action.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                                Text(
                                    text = subtitle,
                                    color = iconTint.copy(alpha = 0.62f),
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                    if (index < saveMenuActions.lastIndex) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .height(1.dp)
                                .background(iconTint.copy(alpha = 0.05f))
                        )
                    }
                }
            }
        }
    }

    Row(
        modifier = modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .zIndex(25f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnifiedSubpageTopBarIconButton(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = LocalAppStrings.current.back,
            onClick = onBack,
            containerColor = containerColor,
            iconTint = iconTint
        )
        Box {
            ColorCardTraceTopBarSaveButton(
                saveState = saveState,
                saveEnabled = saveEnabled,
                containerColor = containerColor,
                contentColor = iconTint,
                label = saveLabel,
                onClick = onSave
            )
        }
    }
}

@Immutable
internal data class ColorCardTraceSaveMenuAction(
    val id: String,
    val title: String,
    val subtitle: String? = null
)

@Composable
private fun ColorCardTraceTopBarSaveButton(
    saveState: ColorCardTraceSaveImageState,
    saveEnabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    label: String,
    onClick: () -> Unit
) {
    val enabled = saveEnabled && saveState == ColorCardTraceSaveImageState.Idle
    val visualEnabled = saveEnabled || saveState != ColorCardTraceSaveImageState.Idle
    val alpha = if (visualEnabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 72.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (visualEnabled) containerColor else containerColor.copy(alpha = 0.7f))
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (saveState) {
            ColorCardTraceSaveImageState.Idle -> {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = alpha),
                    modifier = Modifier.size(20.dp)
                )
            }
            ColorCardTraceSaveImageState.Saving -> {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 1.8.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
            ColorCardTraceSaveImageState.Saved -> {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = label,
            color = contentColor.copy(alpha = alpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun isColorCardTraceBeadsPixelCandidate(
    candidate: ColorCardTraceFilterCandidate?
): Boolean {
    return candidate?.kind == ColorCardTraceFilterKind.PixelPoster &&
        candidate.pixelPosterPreset?.id == "pixel-beads"
}

private fun resolveColorCardImitateHighlightScale(
    lightness: Double
): Double {
    return when {
        lightness <= 68.0 -> 1.0
        lightness >= 95.0 -> 0.40
        else -> lerpColorCardTrace(1.0, 0.40, (lightness - 68.0) / 27.0)
    }
}

private fun resolveColorCardImitateShadowScale(
    lightness: Double
): Double {
    return when {
        lightness >= 18.0 -> 1.0
        lightness <= 4.0 -> 0.72
        else -> lerpColorCardTrace(0.72, 1.0, (lightness - 4.0) / 14.0)
    }
}

private fun resolveColorCardImitateAnchorAffinity(
    inputLab: ColorCardTraceLab,
    sourceAnchors: List<ColorCardTraceLab>
): Double {
    if (sourceAnchors.isEmpty()) return 1.0
    val nearestDistance = sourceAnchors.minOf { anchor ->
        sqrt(colorCardTraceLabDistanceSquared(inputLab, anchor))
    }
    return when {
        nearestDistance <= 10.0 -> 1.0
        nearestDistance >= 46.0 -> 0.24
        else -> lerpColorCardTrace(1.0, 0.24, (nearestDistance - 10.0) / 36.0)
    }
}

private fun applyColorCardImitateToneCurve(
    normalizedLightness: Double,
    curve: ColorCardImitateToneCurve
): Double {
    val sourceStops = curve.sourceStops
    val targetStops = curve.targetStops
    if (sourceStops.size < 2 || sourceStops.size != targetStops.size) {
        return normalizedLightness.coerceIn(0.0, 1.0)
    }
    val value = normalizedLightness.coerceIn(0.0, 1.0)
    if (value <= sourceStops.first()) return targetStops.first().coerceIn(0.0, 1.0)
    if (value >= sourceStops.last()) return targetStops.last().coerceIn(0.0, 1.0)

    sourceStops.windowed(size = 2).forEachIndexed { index, pair ->
        val start = pair[0]
        val end = pair[1]
        if (value <= end) {
            val span = (end - start).takeIf { kotlin.math.abs(it) > 0.00001 } ?: 1.0
            val t = ((value - start) / span).coerceIn(0.0, 1.0)
            return lerpColorCardTrace(targetStops[index], targetStops[index + 1], t).coerceIn(0.0, 1.0)
        }
    }
    return value
}

private fun applyColorCardImitateTransferBand(
    inputLab: ColorCardTraceLab,
    band: ColorCardImitateTransferBand
): ColorCardTraceLab {
    fun remap(value: Double, sourceMean: Double, sourceStd: Double, targetMean: Double, targetStd: Double): Double {
        val safeSourceStd = sourceStd.coerceAtLeast(1.0)
        val safeTargetStd = targetStd.coerceAtLeast(1.0)
        val scale = (safeTargetStd / safeSourceStd).coerceIn(0.6, 1.75)
        return targetMean + ((value - sourceMean) * scale)
    }

    return ColorCardTraceLab(
        l = remap(
            value = inputLab.l,
            sourceMean = band.source.meanL,
            sourceStd = band.source.stdL,
            targetMean = band.target.meanL,
            targetStd = band.target.stdL
        ).coerceIn(0.0, 100.0),
        a = remap(
            value = inputLab.a,
            sourceMean = band.source.meanA,
            sourceStd = band.source.stdA,
            targetMean = band.target.meanA,
            targetStd = band.target.stdA
        ).coerceIn(-128.0, 128.0),
        b = remap(
            value = inputLab.b,
            sourceMean = band.source.meanB,
            sourceStd = band.source.stdB,
            targetMean = band.target.meanB,
            targetStd = band.target.stdB
        ).coerceIn(-128.0, 128.0)
    )
}

private fun renderColorCardHaldFilterBitmap(
    sourceBitmap: Bitmap,
    haldBitmap: Bitmap,
    haldLevel: Int,
    secondaryHaldBitmap: Bitmap?,
    secondaryHaldLevel: Int,
    haldVariantMix: Float,
    color: Float,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    strength: Float,
    faceRegions: List<ColorCardTraceFaceRegion>,
    portraitParams: ColorCardPortraitOptimizationParams
): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val sourcePixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val detailAdjustedSourcePixels = applyColorCardTraceSourceDetailAdjustments(
        sourcePixels = sourcePixels,
        width = width,
        height = height,
        sharpen = sharpen,
        faceRegions = faceRegions,
        portraitParams = portraitParams
    )
    val safeStrength = strength.coerceIn(0f, 1f)
    val antiBandingMix = resolveColorCardTraceAntiBandingMix(
        color = color,
        tone = tone,
        saturation = saturation,
        brightness = brightness,
        paletteStrength = strength
    )

    val xNormalizer = (width - 1).coerceAtLeast(1).toFloat()
    val yNormalizer = (height - 1).coerceAtLeast(1).toFloat()
    for (y in 0 until height) {
        val normalizedY = if (height <= 1) 0.5f else y / yNormalizer
        val rowOffset = y * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val colorInt = detailAdjustedSourcePixels[index]
            val alpha = AndroidColor.alpha(colorInt)
            if (alpha == 0) {
                outputPixels[index] = colorInt
                continue
            }

            val sourceRed = AndroidColor.red(colorInt)
            val sourceGreen = AndroidColor.green(colorInt)
            val sourceBlue = AndroidColor.blue(colorInt)
            val protectionAmount = colorCardTraceSkinProtectionAmount(
                normalizedX = if (width <= 1) 0.5f else x / xNormalizer,
                normalizedY = normalizedY,
                red = sourceRed,
                green = sourceGreen,
                blue = sourceBlue,
                faceRegions = faceRegions
            )
            val paletteEffectScale = 1.0 - (
                protectionAmount.toDouble() *
                    COLOR_CARD_TRACE_SKIN_PROTECTION_PALETTE_RECOVERY.toDouble()
                )
            val chromaScale = 1f - (
                protectionAmount * COLOR_CARD_TRACE_SKIN_PROTECTION_CREATIVE_RECOVERY
                )
            val tonalScale = 1f - (
                protectionAmount * COLOR_CARD_TRACE_SKIN_PROTECTION_TONAL_RECOVERY
                )
            val workingStrength = safeStrength * paletteEffectScale.toFloat()
            val primaryHaldColor = sampleColorCardHaldBitmap(
                bitmap = haldBitmap,
                haldLevel = haldLevel,
                red = sourceRed / 255f,
                green = sourceGreen / 255f,
                blue = sourceBlue / 255f
            )
            val haldColor = if (
                secondaryHaldBitmap != null &&
                secondaryHaldLevel > 0 &&
                haldVariantMix > 0.0001f
            ) {
                val secondaryColor = sampleColorCardHaldBitmap(
                    bitmap = secondaryHaldBitmap,
                    haldLevel = secondaryHaldLevel,
                    red = sourceRed / 255f,
                    green = sourceGreen / 255f,
                    blue = sourceBlue / 255f
                )
                AndroidColor.rgb(
                    lerpColorCardTrace(
                        AndroidColor.red(primaryHaldColor).toDouble(),
                        AndroidColor.red(secondaryColor).toDouble(),
                        haldVariantMix.toDouble()
                    ).roundToInt().coerceIn(0, 255),
                    lerpColorCardTrace(
                        AndroidColor.green(primaryHaldColor).toDouble(),
                        AndroidColor.green(secondaryColor).toDouble(),
                        haldVariantMix.toDouble()
                    ).roundToInt().coerceIn(0, 255),
                    lerpColorCardTrace(
                        AndroidColor.blue(primaryHaldColor).toDouble(),
                        AndroidColor.blue(secondaryColor).toDouble(),
                        haldVariantMix.toDouble()
                    ).roundToInt().coerceIn(0, 255)
                )
            } else {
                primaryHaldColor
            }
            val blendedRgb = ColorCardTraceRgb(
                red = lerpColorCardTrace(
                    sourceRed.toDouble(),
                    AndroidColor.red(haldColor).toDouble(),
                    workingStrength.toDouble()
                ).roundToInt().coerceIn(0, 255),
                green = lerpColorCardTrace(
                    sourceGreen.toDouble(),
                    AndroidColor.green(haldColor).toDouble(),
                    workingStrength.toDouble()
                ).roundToInt().coerceIn(0, 255),
                blue = lerpColorCardTrace(
                    sourceBlue.toDouble(),
                    AndroidColor.blue(haldColor).toDouble(),
                    workingStrength.toDouble()
                ).roundToInt().coerceIn(0, 255)
            )
            val blendedLab = colorCardTraceLabFromRgb(
                red = blendedRgb.red,
                green = blendedRgb.green,
                blue = blendedRgb.blue
            )
            val adjustedRgb = applyColorCardTraceCreativeAdjustments(
                lab = blendedLab,
                color = color,
                tone = tone,
                saturation = saturation,
                brightness = brightness,
                colorAxis = colorAxis,
                chromaScale = chromaScale,
                tonalScale = tonalScale,
                portraitMask = protectionAmount,
                portraitParams = portraitParams
            )
            val enhancedRgb = applyColorCardTraceLightPresenceEnhancement(
                rgb = adjustedRgb,
                x = x,
                y = y,
                width = width,
                height = height,
                referencePixels = sourcePixels
            )
            val finalRgb = applyColorCardTraceAntiBanding(
                rgb = enhancedRgb,
                x = x,
                y = y,
                width = width,
                height = height,
                referencePixels = sourcePixels,
                mix = antiBandingMix
            )
            outputPixels[index] = AndroidColor.argb(
                alpha,
                finalRgb.red,
                finalRgb.green,
                finalRgb.blue
            )
        }
    }

    return Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun buildColorCardTraceAnchorMapping(
    sourceColors: List<Int>,
    targetColors: List<Int>
): ColorCardTraceAnchorMapping? {
    val sourceLabs = prepareColorCardTracePaletteAnchors(sourceColors)
    val targetLabs = prepareColorCardTracePaletteAnchors(targetColors)
    if (sourceLabs.isEmpty() || targetLabs.isEmpty()) return null

    val sourceCentroid = colorCardTracePaletteCentroid(sourceLabs)
    val targetCentroid = colorCardTracePaletteCentroid(targetLabs)

    return if (sourceLabs.size <= targetLabs.size) {
        val match = matchColorCardTraceAnchorSubset(
            leftAnchors = sourceLabs,
            rightAnchors = targetLabs
        ) ?: return null
        ColorCardTraceAnchorMapping(
            sourceAnchors = match.first,
            targetAnchors = match.second,
            sourceCentroid = sourceCentroid,
            targetCentroid = targetCentroid
        )
    } else {
        val match = matchColorCardTraceAnchorSubset(
            leftAnchors = targetLabs,
            rightAnchors = sourceLabs
        ) ?: return null
        ColorCardTraceAnchorMapping(
            sourceAnchors = match.second,
            targetAnchors = match.first,
            sourceCentroid = sourceCentroid,
            targetCentroid = targetCentroid
        )
    }
}

private fun prepareColorCardTracePaletteAnchors(
    colors: List<Int>
): List<ColorCardTraceLab> {
    val labs = colors
        .distinct()
        .map { colorInt ->
            colorCardTraceLabFromRgb(
                red = AndroidColor.red(colorInt),
                green = AndroidColor.green(colorInt),
                blue = AndroidColor.blue(colorInt)
            )
        }
    if (labs.size <= COLOR_CARD_TRACE_FILTER_MAX_ANCHORS) {
        return labs
    }
    return selectDiverseColorCardTraceAnchors(
        anchors = labs,
        limit = COLOR_CARD_TRACE_FILTER_MAX_ANCHORS
    )
}

private fun selectDiverseColorCardTraceAnchors(
    anchors: List<ColorCardTraceLab>,
    limit: Int
): List<ColorCardTraceLab> {
    if (anchors.size <= limit) return anchors

    val centroid = colorCardTracePaletteCentroid(anchors)
    val selected = mutableListOf(
        anchors.maxByOrNull { candidate ->
            colorCardTraceLabDistanceSquared(candidate, centroid)
        } ?: anchors.first()
    )

    while (selected.size < limit) {
        val nextAnchor = anchors
            .asSequence()
            .filterNot(selected::contains)
            .maxByOrNull { candidate ->
                selected.minOf { chosen ->
                    colorCardTraceLabDistanceSquared(candidate, chosen)
                }
            }
            ?: break
        selected += nextAnchor
    }
    return selected
}

private fun matchColorCardTraceAnchorSubset(
    leftAnchors: List<ColorCardTraceLab>,
    rightAnchors: List<ColorCardTraceLab>
): Pair<List<ColorCardTraceLab>, List<ColorCardTraceLab>>? {
    if (leftAnchors.isEmpty() || rightAnchors.isEmpty()) return null
    if (leftAnchors.size > rightAnchors.size) return null

    val memo = HashMap<Long, Double>()
    val choice = HashMap<Long, Int>()

    fun solve(leftIndex: Int, usedMask: Int): Double {
        if (leftIndex >= leftAnchors.size) return 0.0
        val key = (leftIndex.toLong() shl 32) or (usedMask.toLong() and 0xffffffffL)
        memo[key]?.let { return it }

        var bestCost = Double.MAX_VALUE
        var bestRightIndex = -1
        rightAnchors.indices.forEach { rightIndex ->
            val bit = 1 shl rightIndex
            if ((usedMask and bit) != 0) return@forEach
            val cost = colorCardTraceAnchorPairCost(
                leftAnchors[leftIndex],
                rightAnchors[rightIndex]
            ) + solve(leftIndex + 1, usedMask or bit)
            if (cost < bestCost) {
                bestCost = cost
                bestRightIndex = rightIndex
            }
        }

        if (bestRightIndex >= 0) {
            choice[key] = bestRightIndex
        }
        memo[key] = bestCost
        return bestCost
    }

    solve(leftIndex = 0, usedMask = 0)

    var usedMask = 0
    val matchedLeft = ArrayList<ColorCardTraceLab>(leftAnchors.size)
    val matchedRight = ArrayList<ColorCardTraceLab>(leftAnchors.size)
    leftAnchors.indices.forEach { leftIndex ->
        val key = (leftIndex.toLong() shl 32) or (usedMask.toLong() and 0xffffffffL)
        val rightIndex = choice[key] ?: return null
        matchedLeft += leftAnchors[leftIndex]
        matchedRight += rightAnchors[rightIndex]
        usedMask = usedMask or (1 shl rightIndex)
    }
    return matchedLeft to matchedRight
}

private fun colorCardTraceAnchorPairCost(
    source: ColorCardTraceLab,
    target: ColorCardTraceLab
): Double {
    val sourceChroma = sqrt((source.a * source.a) + (source.b * source.b))
    val targetChroma = sqrt((target.a * target.a) + (target.b * target.b))
    return colorCardTraceDeltaE00(source, target) +
        (abs(sourceChroma - targetChroma) * 0.10) +
        (abs(source.l - target.l) * 0.06)
}

private fun colorCardTracePaletteCentroid(
    anchors: List<ColorCardTraceLab>
): ColorCardTraceLab {
    if (anchors.isEmpty()) return ColorCardTraceLab(50.0, 0.0, 0.0)
    return ColorCardTraceLab(
        l = anchors.map { it.l }.average(),
        a = anchors.map { it.a }.average(),
        b = anchors.map { it.b }.average()
    )
}

private fun colorCardTracePaletteSpread(
    anchors: List<ColorCardTraceLab>,
    centroid: ColorCardTraceLab
): Double {
    if (anchors.isEmpty()) return 0.0
    return anchors
        .map { anchor -> sqrt(colorCardTraceLabDistanceSquared(anchor, centroid)) }
        .average()
}

private fun colorCardTraceLabDistanceSquared(
    left: ColorCardTraceLab,
    right: ColorCardTraceLab
): Double {
    val deltaL = (left.l - right.l) * 0.78
    val deltaA = left.a - right.a
    val deltaB = left.b - right.b
    return (deltaL * deltaL) + (deltaA * deltaA) + (deltaB * deltaB)
}

private fun resampleColorCardTraceLabs(
    colors: List<Int>,
    count: Int
): List<ColorCardTraceLab> {
    if (colors.isEmpty() || count <= 0) return emptyList()
    val sortedLabs = colors.map { colorInt ->
        colorCardTraceLabFromRgb(
            red = AndroidColor.red(colorInt),
            green = AndroidColor.green(colorInt),
            blue = AndroidColor.blue(colorInt)
        )
    }.sortedBy { it.l }

    if (sortedLabs.size == 1) {
        return List(count) { sortedLabs.first() }
    }

    val safeCount = count.coerceAtLeast(1)
    if (safeCount == 1) return listOf(sortedLabs.first())

    return List(safeCount) { index ->
        sampleColorCardTraceLab(
            anchors = sortedLabs,
            normalizedPosition = index / (safeCount - 1f)
        )
    }
}

private fun sampleColorCardTraceLab(
    anchors: List<ColorCardTraceLab>,
    normalizedPosition: Float
): ColorCardTraceLab {
    if (anchors.isEmpty()) return ColorCardTraceLab(50.0, 0.0, 0.0)
    if (anchors.size == 1) return anchors.first()
    val safePosition = normalizedPosition.coerceIn(0f, 1f)
    val scaledIndex = safePosition * (anchors.lastIndex)
    val lowerIndex = scaledIndex.toInt().coerceIn(0, anchors.lastIndex)
    val upperIndex = (lowerIndex + 1).coerceAtMost(anchors.lastIndex)
    val fraction = (scaledIndex - lowerIndex).coerceIn(0f, 1f).toDouble()
    val lower = anchors[lowerIndex]
    val upper = anchors[upperIndex]
    return ColorCardTraceLab(
        l = lerpColorCardTrace(lower.l, upper.l, fraction),
        a = lerpColorCardTrace(lower.a, upper.a, fraction),
        b = lerpColorCardTrace(lower.b, upper.b, fraction)
    )
}

private fun colorCardTraceLightnessPosition(
    lightness: Double,
    anchors: List<ColorCardTraceLab>
): Float {
    if (anchors.isEmpty()) return 0f
    if (anchors.size == 1) {
        return (lightness / 100.0).coerceIn(0.0, 1.0).toFloat()
    }
    if (lightness <= anchors.first().l) return 0f
    if (lightness >= anchors.last().l) return 1f

    val lastIndex = anchors.lastIndex.toDouble()
    anchors.windowed(size = 2).forEachIndexed { index, pair ->
        val start = pair[0].l
        val end = pair[1].l
        if (lightness <= end) {
            val span = (end - start).takeIf { abs(it) > 0.0001 } ?: 1.0
            val local = ((lightness - start) / span).coerceIn(0.0, 1.0)
            return ((index + local) / lastIndex).toFloat()
        }
    }
    return 1f
}

private fun applyColorCardTraceToneCurve(
    normalizedLightness: Double,
    tone: Float
): Double {
    val safeLightness = normalizedLightness.coerceIn(0.0, 1.0)
    val amount = tone.coerceIn(-1f, 1f).toDouble()
    return if (amount >= 0.0) {
        val sCurve = safeLightness * safeLightness * (3.0 - (2.0 * safeLightness))
        lerpColorCardTrace(safeLightness, sCurve, amount)
    } else {
        val flattened = 0.5 + ((safeLightness - 0.5) * (1.0 + (amount * 0.58)))
        lerpColorCardTrace(safeLightness, flattened, -amount)
    }.coerceIn(0.0, 1.0)
}

internal fun applyColorCardTraceSourceDetailAdjustments(
    sourcePixels: IntArray,
    width: Int,
    height: Int,
    sharpen: Float,
    faceRegions: List<ColorCardTraceFaceRegion> = emptyList(),
    portraitParams: ColorCardPortraitOptimizationParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED
): IntArray {
    val safeSharpen = sharpen.coerceIn(-1f, 1f)
    if (width <= 0 || height <= 0 || sourcePixels.isEmpty()) {
        return sourcePixels
    }
    val detailSuppressionAmount = (1f - portraitParams.sharpenRetention.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    val portraitDetailEnabled = portraitParams.enabled &&
        faceRegions.isNotEmpty() &&
        (detailSuppressionAmount > 0.0001f)
    val portraitSoftnessEnabled = portraitParams.enabled &&
        faceRegions.isNotEmpty() &&
        portraitParams.softenAmount.coerceIn(0f, 1.35f) > 0.0001f
    if (abs(safeSharpen) <= 0.0001f && !portraitDetailEnabled && !portraitSoftnessEnabled) {
        return sourcePixels
    }
    val outputPixels = IntArray(sourcePixels.size)
    val xNormalizer = (width - 1).coerceAtLeast(1).toFloat()
    val yNormalizer = (height - 1).coerceAtLeast(1).toFloat()
    for (y in 0 until height) {
        val normalizedY = if (height <= 1) 0.5f else y / yNormalizer
        val rowOffset = y * width
        val upOffset = max(y - 1, 0) * width
        val downOffset = min(y + 1, height - 1) * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val center = sourcePixels[index]
            val alpha = AndroidColor.alpha(center)
            if (alpha == 0) {
                outputPixels[index] = center
                continue
            }

            val left = sourcePixels[rowOffset + max(x - 1, 0)]
            val right = sourcePixels[rowOffset + min(x + 1, width - 1)]
            val up = sourcePixels[upOffset + x]
            val down = sourcePixels[downOffset + x]

            fun blur(channel: (Int) -> Int): Double {
                return (
                    (channel(center) * 4.0) +
                        channel(left) +
                        channel(right) +
                        channel(up) +
                        channel(down)
                    ) / 8.0
            }

            val blurredRed = blur(AndroidColor::red)
            val blurredGreen = blur(AndroidColor::green)
            val blurredBlue = blur(AndroidColor::blue)

            val detailMask = if (portraitDetailEnabled) {
                colorCardTracePortraitDetailAmount(
                    normalizedX = if (width <= 1) 0.5f else x / xNormalizer,
                    normalizedY = normalizedY,
                    red = AndroidColor.red(center),
                    green = AndroidColor.green(center),
                    blue = AndroidColor.blue(center),
                    faceRegions = faceRegions
                )
            } else {
                0f
            }
            val effectiveSharpen = if (portraitDetailEnabled && safeSharpen > 0f) {
                val retainedSharpen = if (safeSharpen > 0f) {
                    resolveColorCardPortraitSharpen(
                        sharpen = safeSharpen,
                        portraitMask = detailMask,
                        params = portraitParams
                    )
                } else {
                    safeSharpen
                }
                retainedSharpen.coerceIn(-1f, 1f)
            } else {
                safeSharpen
            }
            var red = applyColorCardTraceSourceDetailChannel(
                center = AndroidColor.red(center).toDouble(),
                blurred = blurredRed,
                sharpen = effectiveSharpen
            )
            var green = applyColorCardTraceSourceDetailChannel(
                center = AndroidColor.green(center).toDouble(),
                blurred = blurredGreen,
                sharpen = effectiveSharpen
            )
            var blue = applyColorCardTraceSourceDetailChannel(
                center = AndroidColor.blue(center).toDouble(),
                blurred = blurredBlue,
                sharpen = effectiveSharpen
            )
            if (portraitDetailEnabled && detailMask > 0.0001f) {
                val detailLuma = (
                    abs(AndroidColor.red(center).toDouble() - blurredRed) * 0.299 +
                        abs(AndroidColor.green(center).toDouble() - blurredGreen) * 0.587 +
                        abs(AndroidColor.blue(center).toDouble() - blurredBlue) * 0.114
                    ).coerceAtLeast(0.0)
                val edgeMask = colorCardTraceSoftStep(
                    edge0 = 4.0,
                    edge1 = 18.0,
                    value = detailLuma
                )
                val suppressionMix = (
                    detailSuppressionAmount *
                        detailMask *
                        edgeMask *
                        1.72f
                    ).coerceIn(0f, 1f)
                if (suppressionMix > 0.0001f) {
                    red = lerpColorCardTrace(
                        red.toDouble(),
                        blurredRed,
                        (suppressionMix * 0.96f).toDouble()
                    ).roundToInt().coerceIn(0, 255)
                    green = lerpColorCardTrace(
                        green.toDouble(),
                        blurredGreen,
                        (suppressionMix * 0.96f).toDouble()
                    ).roundToInt().coerceIn(0, 255)
                    blue = lerpColorCardTrace(
                        blue.toDouble(),
                        blurredBlue,
                        (suppressionMix * 0.96f).toDouble()
                    ).roundToInt().coerceIn(0, 255)
                }
            }
            if (portraitSoftnessEnabled) {
                val softnessMask = colorCardTracePortraitSoftnessAmount(
                    normalizedX = if (width <= 1) 0.5f else x / xNormalizer,
                    normalizedY = normalizedY,
                    red = AndroidColor.red(center),
                    green = AndroidColor.green(center),
                    blue = AndroidColor.blue(center),
                    faceRegions = faceRegions
                )
                val softnessMix = (
                    portraitParams.softenAmount.coerceIn(0f, 1.35f) *
                        softnessMask *
                        1.82f
                    )
                    .coerceIn(0f, 1f)
                if (softnessMix > 0.0001f) {
                    red = lerpColorCardTrace(
                        red.toDouble(),
                        applyColorCardTracePortraitSoftBlurChannel(
                            sourcePixels = sourcePixels,
                            width = width,
                            height = height,
                            x = x,
                            y = y,
                            channel = AndroidColor::red
                        ),
                        softnessMix.toDouble()
                    ).roundToInt().coerceIn(0, 255)
                    green = lerpColorCardTrace(
                        green.toDouble(),
                        applyColorCardTracePortraitSoftBlurChannel(
                            sourcePixels = sourcePixels,
                            width = width,
                            height = height,
                            x = x,
                            y = y,
                            channel = AndroidColor::green
                        ),
                        softnessMix.toDouble()
                    ).roundToInt().coerceIn(0, 255)
                    blue = lerpColorCardTrace(
                        blue.toDouble(),
                        applyColorCardTracePortraitSoftBlurChannel(
                            sourcePixels = sourcePixels,
                            width = width,
                            height = height,
                            x = x,
                            y = y,
                            channel = AndroidColor::blue
                        ),
                        softnessMix.toDouble()
                    ).roundToInt().coerceIn(0, 255)
                }
            }
            outputPixels[index] = AndroidColor.argb(alpha, red, green, blue)
        }
    }
    return outputPixels
}

private fun applyColorCardTraceSourceDetailChannel(
    center: Double,
    blurred: Double,
    sharpen: Float
): Int {
    val safeSharpen = sharpen.coerceIn(-1f, 1f).toDouble()
    val adjusted = if (safeSharpen >= 0.0) {
        center + ((center - blurred) * (safeSharpen * COLOR_CARD_TRACE_SHARPEN_DETAIL_SCALE))
    } else {
        lerpColorCardTrace(center, blurred, -safeSharpen * COLOR_CARD_TRACE_SOFTEN_BLEND_SCALE)
    }
    return adjusted.roundToInt().coerceIn(0, 255)
}

private fun applyColorCardTracePortraitSoftBlurChannel(
    sourcePixels: IntArray,
    width: Int,
    height: Int,
    x: Int,
    y: Int,
    channel: (Int) -> Int
): Double {
    fun sample(sampleX: Int, sampleY: Int): Double {
        val clampedX = sampleX.coerceIn(0, width - 1)
        val clampedY = sampleY.coerceIn(0, height - 1)
        return channel(sourcePixels[(clampedY * width) + clampedX]).toDouble()
    }

    val center = sample(x, y) * 4.0
    val crossNear = (
        sample(x - 1, y) +
            sample(x + 1, y) +
            sample(x, y - 1) +
            sample(x, y + 1)
        ) * 1.42
    val crossFar = (
        sample(x - 2, y) +
            sample(x + 2, y) +
            sample(x, y - 2) +
            sample(x, y + 2)
        ) * 0.82
    val diagonals = (
        sample(x - 1, y - 1) +
            sample(x + 1, y - 1) +
            sample(x - 1, y + 1) +
            sample(x + 1, y + 1)
        ) * 0.94
    val weight = 4.0 + (4.0 * 1.42) + (4.0 * 0.82) + (4.0 * 0.94)
    return (center + crossNear + crossFar + diagonals) / weight
}

private fun colorCardTraceSoftStep(
    edge0: Double,
    edge1: Double,
    value: Double
): Float {
    if (edge0 == edge1) return if (value >= edge1) 1f else 0f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
    return (t * t * (3.0 - (2.0 * t))).toFloat()
}

internal fun applyColorCardTraceCreativeAdjustments(
    lab: ColorCardTraceLab,
    color: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    chromaScale: Float = 1f,
    tonalScale: Float = 1f,
    portraitMask: Float = 0f,
    portraitParams: ColorCardPortraitOptimizationParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED
): ColorCardTraceRgb {
    val safeChromaScale = chromaScale.coerceIn(0f, 1f)
    val safeTonalScale = tonalScale.coerceIn(0f, 1f)
    val protectedTone = tone.coerceIn(-1f, 1f) * safeTonalScale
    val protectedBrightness = brightness.coerceIn(-1f, 1f) * safeTonalScale
    val protectedColor = color.coerceIn(-1f, 1f) * safeChromaScale
    val protectedSaturation = saturation.coerceIn(-1f, 1f) * safeChromaScale
    var adjustedLab = lab
    if (abs(protectedTone) > 0.0001f) {
        adjustedLab = adjustedLab.copy(
            l = (applyColorCardTraceToneCurve(adjustedLab.l / 100.0, protectedTone) * 100.0)
                .coerceIn(0.0, 100.0)
        )
    }
    if (abs(protectedBrightness) > 0.0001f) {
        adjustedLab = adjustedLab.copy(
            l = (adjustedLab.l + (protectedBrightness * COLOR_CARD_TRACE_BRIGHTNESS_SHIFT))
                .coerceIn(0.0, 100.0)
        )
    }
    if (abs(protectedColor) > 0.0001f) {
        val safeColorAxis = if (
            abs(colorAxis.aWeight) > 0.0001f ||
                abs(colorAxis.bWeight) > 0.0001f
        ) {
            colorAxis
        } else {
            COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS
        }
        val colorShift = protectedColor * COLOR_CARD_TRACE_COLOR_SHIFT
        adjustedLab = adjustedLab.copy(
            a = adjustedLab.a + (safeColorAxis.aWeight * colorShift),
            b = adjustedLab.b + (safeColorAxis.bWeight * colorShift)
        )
    }
    if (abs(protectedSaturation) > 0.0001f) {
        val safeSaturation = protectedSaturation.toDouble()
        val saturationFactor = if (safeSaturation >= 0.0) {
            1.0 + (safeSaturation * COLOR_CARD_TRACE_SATURATION_SCALE)
        } else {
            1.0 + (safeSaturation * 0.68)
        }.coerceAtLeast(0.12)
        adjustedLab = adjustedLab.copy(
            a = adjustedLab.a * saturationFactor,
            b = adjustedLab.b * saturationFactor
        )
    }
    adjustedLab = applyColorCardPortraitOptimization(
        lab = adjustedLab,
        portraitMask = portraitMask,
        params = portraitParams
    )
    return colorCardTraceLabToRgb(adjustedLab)
}

internal fun colorCardTraceLabFromRgb(
    red: Int,
    green: Int,
    blue: Int
): ColorCardTraceLab {
    val rf = colorCardTraceLinearizeSrgb(red / 255.0)
    val gf = colorCardTraceLinearizeSrgb(green / 255.0)
    val bf = colorCardTraceLinearizeSrgb(blue / 255.0)

    val x = (rf * 0.4124564) + (gf * 0.3575761) + (bf * 0.1804375)
    val y = (rf * 0.2126729) + (gf * 0.7151522) + (bf * 0.0721750)
    val z = (rf * 0.0193339) + (gf * 0.1191920) + (bf * 0.9503041)

    val xPivot = colorCardTraceLabPivot(x / 0.95047)
    val yPivot = colorCardTraceLabPivot(y / 1.00000)
    val zPivot = colorCardTraceLabPivot(z / 1.08883)

    return ColorCardTraceLab(
        l = (116.0 * yPivot) - 16.0,
        a = 500.0 * (xPivot - yPivot),
        b = 200.0 * (yPivot - zPivot)
    )
}

private fun colorCardTraceLabToRgb(
    lab: ColorCardTraceLab
): ColorCardTraceRgb {
    val fy = (lab.l + 16.0) / 116.0
    val fx = (lab.a / 500.0) + fy
    val fz = fy - (lab.b / 200.0)

    val x = 0.95047 * colorCardTraceInverseLabPivot(fx)
    val y = 1.00000 * colorCardTraceInverseLabPivot(fy)
    val z = 1.08883 * colorCardTraceInverseLabPivot(fz)

    val rLinear = (x * 3.2404542) + (y * -1.5371385) + (z * -0.4985314)
    val gLinear = (x * -0.9692660) + (y * 1.8760108) + (z * 0.0415560)
    val bLinear = (x * 0.0556434) + (y * -0.2040259) + (z * 1.0572252)

    return ColorCardTraceRgb(
        red = colorCardTraceDelinearizeSrgb(rLinear),
        green = colorCardTraceDelinearizeSrgb(gLinear),
        blue = colorCardTraceDelinearizeSrgb(bLinear)
    )
}

private fun colorCardTraceLinearizeSrgb(channel: Double): Double {
    return if (channel <= 0.04045) {
        channel / 12.92
    } else {
        ((channel + 0.055) / 1.055).pow(2.4)
    }
}

private fun colorCardTraceDelinearizeSrgb(channel: Double): Int {
    val safeChannel = channel.coerceIn(0.0, 1.0)
    val srgb = if (safeChannel <= 0.0031308) {
        safeChannel * 12.92
    } else {
        (1.055 * safeChannel.pow(1.0 / 2.4)) - 0.055
    }
    return (srgb * 255.0).roundToInt().coerceIn(0, 255)
}

private fun colorCardTraceLabPivot(value: Double): Double {
    return if (value > 0.008856) {
        Math.cbrt(value)
    } else {
        (7.787 * value) + (16.0 / 116.0)
    }
}

private fun colorCardTraceInverseLabPivot(value: Double): Double {
    val cube = value.pow(3.0)
    return if (cube > 0.008856) {
        cube
    } else {
        (value - (16.0 / 116.0)) / 7.787
    }
}

private fun colorCardTraceDeltaE00(
    lab1: ColorCardTraceLab,
    lab2: ColorCardTraceLab
): Double {
    val avgL = (lab1.l + lab2.l) / 2.0
    val c1 = sqrt((lab1.a * lab1.a) + (lab1.b * lab1.b))
    val c2 = sqrt((lab2.a * lab2.a) + (lab2.b * lab2.b))
    val avgC = (c1 + c2) / 2.0
    val avgCPow7 = avgC.pow(7.0)
    val g = 0.5 * (1.0 - sqrt(avgCPow7 / (avgCPow7 + 25.0.pow(7.0))))
    val a1Prime = (1.0 + g) * lab1.a
    val a2Prime = (1.0 + g) * lab2.a
    val c1Prime = sqrt((a1Prime * a1Prime) + (lab1.b * lab1.b))
    val c2Prime = sqrt((a2Prime * a2Prime) + (lab2.b * lab2.b))
    val avgCPrime = (c1Prime + c2Prime) / 2.0
    val h1Prime = colorCardTraceLabHueAngle(lab1.b, a1Prime)
    val h2Prime = colorCardTraceLabHueAngle(lab2.b, a2Prime)
    val deltaLPrime = lab2.l - lab1.l
    val deltaCPrime = c2Prime - c1Prime
    val deltaHAngle = when {
        c1Prime == 0.0 || c2Prime == 0.0 -> 0.0
        h2Prime - h1Prime > 180.0 -> h2Prime - h1Prime - 360.0
        h2Prime - h1Prime < -180.0 -> h2Prime - h1Prime + 360.0
        else -> h2Prime - h1Prime
    }
    val deltaHPrime = 2.0 * sqrt(c1Prime * c2Prime) * sin(Math.toRadians(deltaHAngle / 2.0))
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

private fun colorCardTraceLabHueAngle(
    b: Double,
    a: Double
): Double {
    val angle = Math.toDegrees(atan2(b, a))
    return if (angle >= 0.0) angle else angle + 360.0
}

private fun colorCardTraceFilterPrimaryTitle(
    state: ColorCardTraceFilterState,
    panelMode: ColorCardTracePanelMode
): String {
    return when (panelMode) {
        ColorCardTracePanelMode.Picker -> "取色面板"
        ColorCardTracePanelMode.Filter -> when {
            state.isAnalyzing -> "正在准备滤镜"
            !state.hasBuiltinFilters -> "暂无滤镜"
            state.selectedCandidate != null -> state.selectedCandidate.title
            else -> "滤镜"
        }
        ColorCardTracePanelMode.Trace -> when {
            state.isAnalyzing -> "正在分析取色点"
            state.selectedCandidate != null -> state.selectedCandidate.title
            state.candidates.isEmpty() -> "当前无色卡可用"
            else -> "追色"
        }
        ColorCardTracePanelMode.Imitate -> when {
            state.isAnalyzing -> "正在解析参考图"
            state.selectedCandidate != null -> "仿色"
            else -> "仿色"
        }
        ColorCardTracePanelMode.Recipe -> "配方"
    }
}

private fun colorCardTraceFilterSecondaryTitle(
    state: ColorCardTraceFilterState,
    panelMode: ColorCardTracePanelMode
): String {
    return when (panelMode) {
        ColorCardTracePanelMode.Picker -> "拖动取色点，保存到“我的”后可用于追色。"
        ColorCardTracePanelMode.Filter -> when {
            state.hasBuiltinFilters -> ""
            else -> ""
        }
        ColorCardTracePanelMode.Trace -> when {
            !state.hasTraceFilters -> ""
            state.isAnalyzing -> ""
            state.selectedCandidate != null -> ""
            else -> ""
        }
        ColorCardTracePanelMode.Imitate -> when {
            state.isAnalyzing -> "上传参考图后会自动解析色彩风格。"
            state.selectedCandidate != null -> "上传参考图并把当前图片映射到对应色彩风格。"
            else -> "上传参考图并把当前图片映射到对应色彩风格。"
        }
        ColorCardTracePanelMode.Recipe -> "导入、导出并复用你的滤镜和追色参数。"
    }
}

private const val COLOR_CARD_TRACE_EMPTY_PALETTE_HELP_MESSAGE = """
追色 是指：让当前目标照片的整体色调，尽可能接近您已保存或已收藏的色卡风格。

使用该功能前，请先满足以下任一条件：

已保存过色卡
已在 色卡灵感 页面收藏过部分色卡，若您还没有色卡，建议优先使用滤镜模式。
"""

private fun colorCardTraceFilterCategoryLabel(
    candidate: ColorCardTraceFilterCandidate
): String? {
    return colorCardTraceFilterCategoryTitle(
        resolveColorCardTraceExpandedFilterSectionKey(candidate)
    )
}

private fun scaleColorCardTraceBitmapForAiPreview(
    sourceBitmap: Bitmap,
    maxSide: Int = COLOR_CARD_TRACE_AI_PREVIEW_MAX_SIDE
): Bitmap = scaleColorCardTraceBitmapForDiagnostics(
    sourceBitmap = sourceBitmap,
    maxSide = maxSide
)

private fun buildColorCardTraceFaceDetectionBitmap(
    sourceBitmap: Bitmap
): Bitmap {
    val currentMaxSide = max(sourceBitmap.width, sourceBitmap.height).coerceAtLeast(1)
    if (currentMaxSide <= COLOR_CARD_TRACE_FACE_DETECTION_MAX_SIDE) {
        return sourceBitmap
    }
    val scale = COLOR_CARD_TRACE_FACE_DETECTION_MAX_SIDE.toFloat() / currentMaxSide.toFloat()
    return Bitmap.createScaledBitmap(
        sourceBitmap,
        (sourceBitmap.width * scale).roundToInt().coerceAtLeast(1),
        (sourceBitmap.height * scale).roundToInt().coerceAtLeast(1),
        true
    )
}

private suspend fun renderColorCardTraceFilterModeBitmap(
    context: Context,
    sourceBitmap: Bitmap,
    traceState: ColorCardTraceFilterState
): Bitmap? {
    val candidate = traceState.selectedCandidate ?: return null
    val portraitAvailable = resolveColorCardTracePortraitFeatureAvailable(
        panelMode = traceState.mode,
        selectedCandidate = candidate,
        faceRegions = traceState.faceRegions
    )
    val basePortraitParams = resolveColorCardPortraitOptimizationParams(
        panelMode = traceState.mode,
        selectedCandidate = candidate,
        faceRegions = traceState.faceRegions
    )
    val portraitParams = resolveColorCardTracePortraitFeatureParams(
        enabled = portraitAvailable,
        faceRegions = traceState.faceRegions,
        baseParams = basePortraitParams,
        adjustments = traceState.portraitAdjustments
    )
    val (effectiveColor, effectiveTone) = resolveColorCardTraceFilterCreativeResponse(
        panelMode = traceState.mode,
        selectedCandidate = candidate,
        color = traceState.color,
        tone = traceState.tone
    )

    val renderedBitmap = renderColorCardTraceFilterModeBitmapGpu(
        context = context,
        sourceBitmap = sourceBitmap,
        traceState = traceState,
        candidate = candidate,
        portraitParams = portraitParams,
        effectiveColor = effectiveColor,
        effectiveTone = effectiveTone
    ) ?: renderColorCardTraceFilterModeBitmapCpu(
        context = context,
        sourceBitmap = sourceBitmap,
        traceState = traceState,
        candidate = candidate,
        portraitParams = portraitParams,
        effectiveColor = effectiveColor,
        effectiveTone = effectiveTone
    )

    if (
        renderedBitmap != null &&
        traceState.filmGrainEnabled &&
        traceState.filmGrainEligible
    ) {
        val cachedGrainBitmap = traceState.selectedFilmGrainBitmap
        val resolvedGrainBitmap = cachedGrainBitmap ?: traceState.selectedFilmGrainAsset
            ?.let { asset ->
                decodeColorCardTraceFilmGrainBitmap(
                    context = context,
                    assetName = asset.assetName
                )
            }
        if (resolvedGrainBitmap == null) {
            return renderedBitmap
        }
        try {
            return applyColorCardTraceFilmGrainOverlay(
                sourceBitmap = renderedBitmap,
                grainBitmap = resolvedGrainBitmap
            ).also {
                if (it !== renderedBitmap && !renderedBitmap.isRecycled) {
                    renderedBitmap.recycle()
                }
            }
        } finally {
            if (resolvedGrainBitmap !== cachedGrainBitmap && !resolvedGrainBitmap.isRecycled) {
                resolvedGrainBitmap.recycle()
            }
        }
    }

    return renderedBitmap
}

private suspend fun renderColorCardTraceFilterModeBitmapGpu(
    context: Context,
    sourceBitmap: Bitmap,
    traceState: ColorCardTraceFilterState,
    candidate: ColorCardTraceFilterCandidate,
    portraitParams: ColorCardPortraitOptimizationParams,
    effectiveColor: Float,
    effectiveTone: Float
): Bitmap? {
    if (
        candidate.kind == ColorCardTraceFilterKind.Palette ||
        candidate.kind == ColorCardTraceFilterKind.AiMnn
    ) return null

    var haldBitmap: Bitmap? = null
    var secondaryHaldBitmap: Bitmap? = null
    var haldVariantMix = 0f
    try {
        if (candidate.kind == ColorCardTraceFilterKind.HaldClut) {
            val resolvedBlend = resolveColorCardTraceHaldBlend(
                candidate = candidate,
                paletteStrength = traceState.paletteStrength
            )
            val primaryAssetName = resolvedBlend.primaryAssetName ?: return null
            haldBitmap = decodeColorCardBuiltinHaldBitmap(
                context = context,
                assetName = primaryAssetName
            ) ?: return null
            secondaryHaldBitmap = resolvedBlend.secondaryAssetName
                ?.let { assetName ->
                    decodeColorCardBuiltinHaldBitmap(
                        context = context,
                        assetName = assetName
                    )
                }
            haldVariantMix = resolvedBlend.variantMix
        }

        val width = sourceBitmap.width.coerceAtLeast(1)
        val height = sourceBitmap.height.coerceAtLeast(1)
        val gpuParams = buildColorCardTraceGpuParams(
            panelMode = traceState.mode,
            sourceColors = traceState.sourceColors,
            selectedCandidate = candidate,
            selectedHaldBitmap = haldBitmap,
            secondaryHaldBitmap = secondaryHaldBitmap,
            haldVariantMix = haldVariantMix,
            color = effectiveColor,
            sharpen = traceState.sharpen,
            tone = effectiveTone,
            saturation = traceState.saturation,
            brightness = traceState.brightness,
            colorAxis = traceState.colorAxis,
            paletteStrength = traceState.paletteStrength,
            faceRegions = traceState.faceRegions,
            portraitParams = portraitParams,
            subjectRect = traceState.subjectRect,
            sourceAspect = width.toFloat() / height.toFloat(),
            sourceTexelWidth = 1f / width.toFloat(),
            sourceTexelHeight = 1f / height.toFloat()
        )
        val gpuBitmap = renderColorCardTraceGpuBitmap(
            sourceBitmap = sourceBitmap,
            params = gpuParams
        ) ?: return null

        if (candidate.kind != ColorCardTraceFilterKind.HaldClut) {
            return gpuBitmap
        }

        return applyColorCardTraceHaldPostProcessing(
            sourceBitmap = sourceBitmap,
            renderedBitmap = gpuBitmap,
            color = effectiveColor,
            tone = effectiveTone,
            saturation = traceState.saturation,
            brightness = traceState.brightness,
            paletteStrength = traceState.paletteStrength
        )
    } finally {
        secondaryHaldBitmap?.takeIf { !it.isRecycled }?.recycle()
        haldBitmap?.takeIf { !it.isRecycled }?.recycle()
    }
}

private suspend fun renderColorCardTraceFilterModeBitmapCpu(
    context: Context,
    sourceBitmap: Bitmap,
    traceState: ColorCardTraceFilterState,
    candidate: ColorCardTraceFilterCandidate,
    portraitParams: ColorCardPortraitOptimizationParams,
    effectiveColor: Float,
    effectiveTone: Float
): Bitmap? {
    return when (candidate.kind) {
        ColorCardTraceFilterKind.HaldClut -> {
            val resolvedBlend = resolveColorCardTraceHaldBlend(
                candidate = candidate,
                paletteStrength = traceState.paletteStrength
            )
            val primaryAssetName = resolvedBlend.primaryAssetName ?: return null
            val haldBitmap = decodeColorCardBuiltinHaldBitmap(
                context = context,
                assetName = primaryAssetName
            ) ?: return null
            val secondaryHaldBitmap = resolvedBlend.secondaryAssetName
                ?.let { assetName ->
                    decodeColorCardBuiltinHaldBitmap(
                        context = context,
                        assetName = assetName
                    )
                }
            try {
                renderColorCardHaldFilterBitmap(
                    sourceBitmap = sourceBitmap,
                    haldBitmap = haldBitmap,
                    haldLevel = resolvedBlend.primaryHaldLevel,
                    secondaryHaldBitmap = secondaryHaldBitmap,
                    secondaryHaldLevel = resolvedBlend.secondaryHaldLevel,
                    haldVariantMix = resolvedBlend.variantMix,
                    color = effectiveColor,
                    colorAxis = traceState.colorAxis,
                    sharpen = traceState.sharpen,
                    tone = effectiveTone,
                    saturation = traceState.saturation,
                    brightness = traceState.brightness,
                    strength = traceState.paletteStrength,
                    faceRegions = traceState.faceRegions,
                    portraitParams = portraitParams
                )
            } finally {
                secondaryHaldBitmap?.takeIf { !it.isRecycled }?.recycle()
                haldBitmap.recycle()
            }
        }
        ColorCardTraceFilterKind.PixelPoster -> {
            renderColorCardTracePixelPosterBitmap(
                sourceBitmap = sourceBitmap,
                candidate = candidate,
                color = effectiveColor,
                colorAxis = traceState.colorAxis,
                sharpen = traceState.sharpen,
                tone = effectiveTone,
                saturation = traceState.saturation,
                brightness = traceState.brightness,
                strength = traceState.paletteStrength,
                faceRegions = traceState.faceRegions
            )
        }
        ColorCardTraceFilterKind.SelectiveColor -> {
            renderColorCardTraceSelectiveColorBitmap(
                sourceBitmap = sourceBitmap,
                candidate = candidate,
                color = effectiveColor,
                colorAxis = traceState.colorAxis,
                sharpen = traceState.sharpen,
                tone = effectiveTone,
                saturation = traceState.saturation,
                brightness = traceState.brightness,
                strength = traceState.paletteStrength,
                faceRegions = traceState.faceRegions,
                portraitParams = portraitParams
            )
        }
        ColorCardTraceFilterKind.SubjectFrame -> {
            renderColorCardTraceSubjectFrameBitmap(
                sourceBitmap = sourceBitmap,
                subjectRect = traceState.subjectRect ?: resolveColorCardTraceSubjectRect(
                    sourceSessionId = 0L,
                    sourceAspect = sourceBitmap.width.toFloat() / sourceBitmap.height.coerceAtLeast(1).toFloat(),
                    faceRegions = traceState.faceRegions
                ),
                color = effectiveColor,
                colorAxis = traceState.colorAxis,
                sharpen = traceState.sharpen,
                tone = effectiveTone,
                saturation = traceState.saturation,
                brightness = traceState.brightness,
                strength = traceState.paletteStrength
            )
        }
        ColorCardTraceFilterKind.BlackMist -> {
            renderColorCardTraceBlackMistBitmap(
                sourceBitmap = sourceBitmap,
                color = effectiveColor,
                colorAxis = traceState.colorAxis,
                sharpen = traceState.sharpen,
                tone = effectiveTone,
                saturation = traceState.saturation,
                brightness = traceState.brightness,
                strength = traceState.paletteStrength
            )
        }
        ColorCardTraceFilterKind.MotionPhotoEntry -> null
        ColorCardTraceFilterKind.AiMnn -> {
            val filterAsset = candidate.mnnFilterAsset
                ?.copy(threadCount = COLOR_CARD_TRACE_AI_EXPORT_THREAD_COUNT)
                ?: return null
            val aiSourceBitmap = scaleColorCardTraceBitmapForDiagnostics(
                sourceBitmap = sourceBitmap,
                maxSide = COLOR_CARD_TRACE_AI_EXPORT_MAX_SIDE
            )
            try {
                MNNFilterEngine(context).use { engine ->
                    engine.render(aiSourceBitmap, filterAsset)
                }
            } finally {
                if (aiSourceBitmap !== sourceBitmap && !aiSourceBitmap.isRecycled) {
                    aiSourceBitmap.recycle()
                }
            }
        }
        ColorCardTraceFilterKind.Palette -> null
    }
}

private fun applyColorCardTraceHaldPostProcessing(
    sourceBitmap: Bitmap,
    renderedBitmap: Bitmap,
    color: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    paletteStrength: Float
): Bitmap {
    val width = renderedBitmap.width
    val height = renderedBitmap.height
    val referencePixels = IntArray(width * height)
    val renderedPixels = IntArray(width * height)
    val outputPixels = IntArray(width * height)
    sourceBitmap.getPixels(referencePixels, 0, width, 0, 0, width, height)
    renderedBitmap.getPixels(renderedPixels, 0, width, 0, 0, width, height)
    val antiBandingMix = resolveColorCardTraceAntiBandingMix(
        color = color,
        tone = tone,
        saturation = saturation,
        brightness = brightness,
        paletteStrength = paletteStrength
    )
    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val index = rowOffset + x
            val colorInt = renderedPixels[index]
            val alpha = AndroidColor.alpha(colorInt)
            if (alpha == 0) {
                outputPixels[index] = colorInt
                continue
            }
            val enhancedRgb = applyColorCardTraceLightPresenceEnhancement(
                rgb = ColorCardTraceRgb(
                    red = AndroidColor.red(colorInt),
                    green = AndroidColor.green(colorInt),
                    blue = AndroidColor.blue(colorInt)
                ),
                x = x,
                y = y,
                width = width,
                height = height,
                referencePixels = referencePixels
            )
            val finalRgb = applyColorCardTraceAntiBanding(
                rgb = enhancedRgb,
                x = x,
                y = y,
                width = width,
                height = height,
                referencePixels = referencePixels,
                mix = antiBandingMix
            )
            outputPixels[index] = AndroidColor.argb(
                alpha,
                finalRgb.red,
                finalRgb.green,
                finalRgb.blue
            )
        }
    }
    val postProcessed = Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
    if (!renderedBitmap.isRecycled) {
        renderedBitmap.recycle()
    }
    return postProcessed
}

private data class ColorCardTraceExpandedFilterSection(
    val key: String,
    val title: String,
    val candidates: List<ColorCardTraceFilterCandidate>
)

private data class ColorCardTraceFilterBrowserTab(
    val key: String,
    val title: String
)

private const val COLOR_CARD_FILTER_TAB_COMMON = "common"
private const val COLOR_CARD_FILTER_TAB_AI = "AI"
private const val COLOR_CARD_FILTER_TAB_CREATIVE_VISUAL = "creative_visual"
private const val COLOR_CARD_FILTER_TAB_STYLE = "A"
private const val COLOR_CARD_FILTER_TAB_RETRO = "F"
private const val COLOR_CARD_FILTER_TAB_CINEMA = "C"
private const val COLOR_CARD_FILTER_TAB_BLACK_WHITE = "B"

private fun resolveColorCardTraceExpandedBrowserTabKey(
    panelMode: ColorCardTracePanelMode,
    filterTabs: List<ColorCardTraceFilterBrowserTab>,
    currentTabKey: String,
    persistedTabKey: String,
    hasCommonTabContent: Boolean
): String {
    if (panelMode != ColorCardTracePanelMode.Filter || filterTabs.isEmpty()) return ""

    fun isValidTab(key: String): Boolean {
        if (key.isBlank()) return false
        if (key == COLOR_CARD_FILTER_TAB_COMMON && !hasCommonTabContent) return false
        return filterTabs.any { it.key == key }
    }

    if (isValidTab(currentTabKey)) return currentTabKey
    if (isValidTab(persistedTabKey)) return persistedTabKey

    val preferredFallbacks = listOf(
        COLOR_CARD_FILTER_TAB_RETRO,
        COLOR_CARD_FILTER_TAB_STYLE,
        COLOR_CARD_FILTER_TAB_AI,
        COLOR_CARD_FILTER_TAB_CREATIVE_VISUAL,
        COLOR_CARD_FILTER_TAB_CINEMA,
        COLOR_CARD_FILTER_TAB_BLACK_WHITE,
        COLOR_CARD_FILTER_TAB_COMMON
    )
    preferredFallbacks.firstOrNull(::isValidTab)?.let { return it }

    return filterTabs.firstOrNull { tab -> isValidTab(tab.key) }?.key.orEmpty()
}

private fun buildColorCardTraceExpandedFilterSections(
    candidates: List<ColorCardTraceFilterCandidate>
): List<ColorCardTraceExpandedFilterSection> {
    if (candidates.isEmpty()) return emptyList()
    val orderedCategoryKeys = listOf(
        COLOR_CARD_FILTER_TAB_RETRO,
        COLOR_CARD_FILTER_TAB_AI,
        COLOR_CARD_FILTER_TAB_CREATIVE_VISUAL,
        COLOR_CARD_FILTER_TAB_STYLE,
        COLOR_CARD_FILTER_TAB_CINEMA,
        COLOR_CARD_FILTER_TAB_BLACK_WHITE
    )
    val categorized = linkedMapOf<String, MutableList<ColorCardTraceFilterCandidate>>()
    candidates.forEach { candidate ->
        val categoryKey = resolveColorCardTraceExpandedFilterSectionKey(candidate)
        categorized.getOrPut(categoryKey) { mutableListOf() } += candidate
    }
    return buildList {
        orderedCategoryKeys.forEach { key ->
            val sectionCandidates = categorized[key].orEmpty()
            if (sectionCandidates.isEmpty()) return@forEach
            add(
                ColorCardTraceExpandedFilterSection(
                    key = key,
                    title = colorCardTraceFilterCategoryTitle(key),
                    candidates = reorderColorCardTraceSectionCandidates(
                        sectionKey = key,
                        candidates = sectionCandidates
                    )
                )
            )
        }
    }
}

private fun resolveColorCardTraceExpandedFilterSectionKey(
    candidate: ColorCardTraceFilterCandidate
): String {
    if (isColorCardTraceAiMnnCandidate(candidate)) {
        return COLOR_CARD_FILTER_TAB_AI
    }
    if (candidate.kind == ColorCardTraceFilterKind.MotionPhotoEntry) {
        return COLOR_CARD_FILTER_TAB_CREATIVE_VISUAL
    }
    return when (candidate.title.trim().lastOrNull()?.uppercaseChar()) {
        'P', 'V' -> COLOR_CARD_FILTER_TAB_CREATIVE_VISUAL
        'J', 'A' -> COLOR_CARD_FILTER_TAB_STYLE
        'M', 'F' -> COLOR_CARD_FILTER_TAB_RETRO
        'C' -> COLOR_CARD_FILTER_TAB_CINEMA
        'B' -> COLOR_CARD_FILTER_TAB_BLACK_WHITE
        else -> COLOR_CARD_FILTER_TAB_STYLE
    }
}

private fun reorderColorCardTraceSectionCandidates(
    sectionKey: String,
    candidates: List<ColorCardTraceFilterCandidate>
): List<ColorCardTraceFilterCandidate> {
    if (sectionKey != COLOR_CARD_FILTER_TAB_RETRO || candidates.size <= 1) {
        return candidates
    }
    val filmCandidates = candidates.filter { candidate ->
        candidate.title.trim().lastOrNull()?.uppercaseChar() == 'F'
    }
    val memoryCandidates = candidates.filter { candidate ->
        candidate.title.trim().lastOrNull()?.uppercaseChar() == 'M'
    }
    if (filmCandidates.isEmpty() || memoryCandidates.isEmpty()) {
        return candidates
    }
    return buildList(candidates.size) {
        val filmQueue = ArrayDeque(filmCandidates)
        val memoryQueue = ArrayDeque(memoryCandidates)
        while (filmQueue.isNotEmpty() || memoryQueue.isNotEmpty()) {
            filmQueue.removeFirstOrNull()?.let(::add)
            memoryQueue.removeFirstOrNull()?.let(::add)
        }
    }
}

private fun isColorCardTraceCachedPreviewBitmap(
    cache: Map<String, Bitmap>,
    bitmap: Bitmap?
): Boolean {
    if (bitmap == null) return false
    return cache.values.any { it === bitmap }
}

internal fun shouldShowColorCardTraceAiExitOverlay(
    effectModeVisible: Boolean,
    selectedCandidateKind: ColorCardTraceFilterKind?,
    replacementPreviewAvailable: Boolean
): Boolean {
    return effectModeVisible &&
        selectedCandidateKind != null &&
        selectedCandidateKind != ColorCardTraceFilterKind.AiMnn &&
        !replacementPreviewAvailable
}

private fun colorCardTraceFilterCategoryTitle(
    key: String
): String {
    return when (key) {
        COLOR_CARD_FILTER_TAB_AI -> "AI调色"
        COLOR_CARD_FILTER_TAB_BLACK_WHITE -> "黑白时代"
        COLOR_CARD_FILTER_TAB_CREATIVE_VISUAL -> "创意视觉"
        COLOR_CARD_FILTER_TAB_STYLE -> "风格滤镜"
        COLOR_CARD_FILTER_TAB_RETRO -> "复古胶卷"
        COLOR_CARD_FILTER_TAB_CINEMA -> "电影感"
        else -> "风格滤镜"
    }
}

private fun normalizeColorCardTraceBrowserTabKey(
    key: String
): String {
    return when (key.trim()) {
        "P", "V", COLOR_CARD_FILTER_TAB_CREATIVE_VISUAL -> COLOR_CARD_FILTER_TAB_CREATIVE_VISUAL
        "J", "A", COLOR_CARD_FILTER_TAB_STYLE -> COLOR_CARD_FILTER_TAB_STYLE
        "M", "F", COLOR_CARD_FILTER_TAB_RETRO -> COLOR_CARD_FILTER_TAB_RETRO
        "C", COLOR_CARD_FILTER_TAB_CINEMA -> COLOR_CARD_FILTER_TAB_CINEMA
        "B", COLOR_CARD_FILTER_TAB_BLACK_WHITE -> COLOR_CARD_FILTER_TAB_BLACK_WHITE
        "AI", COLOR_CARD_FILTER_TAB_AI -> COLOR_CARD_FILTER_TAB_AI
        else -> COLOR_CARD_FILTER_TAB_COMMON
    }
}

private fun formatColorCardTraceSignedPercent(
    value: Float
): String {
    val percent = (value.coerceIn(-1f, 1f) * 100f).roundToInt()
    return if (percent > 0) "+$percent" else percent.toString()
}

internal fun resolveColorCardTraceFollowBiasMultiplier(
    bias: Float
): Float {
    return (1f + (bias.coerceIn(-1f, 1f) * COLOR_CARD_TRACE_FOLLOW_BIAS_SCALE))
        .coerceIn(0.2f, 1.8f)
}

private fun wrapColorCardTraceIndex(
    index: Int,
    size: Int
): Int {
    if (size <= 0) return 0
    val mod = index % size
    return if (mod >= 0) mod else mod + size
}

internal fun buildColorCardTraceCycleOrder(
    candidateIds: List<String>,
    seed: Long,
    anchorId: String? = null,
    avoidFirstId: String? = null
): List<String> {
    val ids = candidateIds.distinct()
    if (ids.isEmpty()) return emptyList()

    if (anchorId != null && anchorId in ids) {
        return buildList(ids.size) {
            add(anchorId)
            addAll(
                orderColorCardTraceCandidateIdsForDisplay(
                    ids.filterNot { it == anchorId }.shuffled(Random(seed))
                )
            )
        }
    }

    val shuffled = orderColorCardTraceCandidateIdsForDisplay(
        ids.shuffled(Random(seed))
    ).toMutableList()
    if (shuffled.size > 1 && isColorCardTracePSeriesCandidateId(shuffled.first())) {
        val swapIndex = shuffled.indexOfFirst { candidateId ->
            !isColorCardTracePSeriesCandidateId(candidateId)
        }
        if (swapIndex > 0) {
            val first = shuffled.first()
            shuffled[0] = shuffled[swapIndex]
            shuffled[swapIndex] = first
        }
    }
    if (avoidFirstId != null && shuffled.size > 1 && shuffled.firstOrNull() == avoidFirstId) {
        val swapIndex = shuffled.indexOfFirst { it != avoidFirstId }
        if (swapIndex > 0) {
            val first = shuffled.first()
            shuffled[0] = shuffled[swapIndex]
            shuffled[swapIndex] = first
        }
    }
    return shuffled
}

internal fun stepColorCardTraceCycle(
    candidateIds: List<String>,
    currentOrder: List<String>,
    currentIndex: Int,
    currentSelectedId: String?,
    delta: Int,
    seed: Long
): ColorCardTraceCycleStep? {
    val ids = candidateIds.distinct()
    if (ids.isEmpty()) return null

    val selectedId = currentSelectedId?.takeIf(ids::contains) ?: ids.first()
    val normalizedOrder = if (
        currentOrder.size == ids.size &&
        currentOrder.toSet() == ids.toSet() &&
        selectedId in currentOrder
    ) {
        currentOrder
    } else {
        buildColorCardTraceCycleOrder(
            candidateIds = ids,
            seed = seed,
            anchorId = selectedId
        )
    }
    val startIndex = normalizedOrder.indexOf(selectedId).takeIf { it >= 0 }
        ?: currentIndex.coerceIn(0, normalizedOrder.lastIndex)

    if (delta == 0) {
        return ColorCardTraceCycleStep(
            order = normalizedOrder,
            index = startIndex,
            selectedId = normalizedOrder[startIndex]
        )
    }

    if (delta < 0) {
        val previousIndex = if (startIndex <= 0) normalizedOrder.lastIndex else startIndex - 1
        return ColorCardTraceCycleStep(
            order = normalizedOrder,
            index = previousIndex,
            selectedId = normalizedOrder[previousIndex]
        )
    }

    if (startIndex < normalizedOrder.lastIndex) {
        val nextIndex = startIndex + 1
        return ColorCardTraceCycleStep(
            order = normalizedOrder,
            index = nextIndex,
            selectedId = normalizedOrder[nextIndex]
        )
    }

    val nextOrder = buildColorCardTraceCycleOrder(
        candidateIds = ids,
        seed = seed,
        avoidFirstId = selectedId
    )
    return ColorCardTraceCycleStep(
        order = nextOrder,
        index = 0,
        selectedId = nextOrder.first()
    )
}

internal fun resolveColorCardTraceAdjustmentPadGeometry(
    width: Float,
    height: Float,
    useFullArea: Boolean = false
): ColorCardTraceAdjustmentPadGeometry {
    val columns = COLOR_CARD_TRACE_ADJUSTMENT_GRID_COLUMNS.coerceAtLeast(2)
    val rows = COLOR_CARD_TRACE_ADJUSTMENT_GRID_ROWS.coerceAtLeast(2)
    val usableWidth = width.coerceAtLeast(0f) * if (useFullArea) 1f else COLOR_CARD_TRACE_ADJUSTMENT_GRID_WIDTH_FRACTION
    val usableHeight = height.coerceAtLeast(0f) * if (useFullArea) 1f else COLOR_CARD_TRACE_ADJUSTMENT_GRID_HEIGHT_FRACTION
    return ColorCardTraceAdjustmentPadGeometry(
        startX = (width - usableWidth) / 2f,
        startY = (height - usableHeight) / 2f,
        stepX = usableWidth / (columns - 1),
        stepY = usableHeight / (rows - 1),
        columns = columns,
        rows = rows
    )
}

internal fun resolveColorCardTraceAdjustmentPadDetentCenter(
    geometry: ColorCardTraceAdjustmentPadGeometry,
    xIndex: Int,
    yIndex: Int
): androidx.compose.ui.geometry.Offset {
    val safeX = xIndex.coerceIn(0, geometry.columns - 1)
    val safeY = yIndex.coerceIn(0, geometry.rows - 1)
    return androidx.compose.ui.geometry.Offset(
        x = geometry.startX + (geometry.stepX * safeX),
        y = geometry.startY + (geometry.stepY * safeY)
    )
}

internal fun snapColorCardTraceAdjustmentFromTouch(
    touchX: Float,
    touchY: Float,
    padSize: IntSize,
    useFullArea: Boolean = false
): ColorCardTraceAdjustmentDetent {
    val geometry = resolveColorCardTraceAdjustmentPadGeometry(
        width = padSize.width.toFloat(),
        height = padSize.height.toFloat(),
        useFullArea = useFullArea
    )
    val snappedX = snapColorCardTraceAxisDetentFromTouch(
        touch = touchX,
        start = geometry.startX,
        step = geometry.stepX,
        detents = geometry.columns
    )
    val snappedY = snapColorCardTraceAxisDetentFromTouch(
        touch = touchY,
        start = geometry.startY,
        step = geometry.stepY,
        detents = geometry.rows
    )
    return ColorCardTraceAdjustmentDetent(
        normalizedX = snappedX.normalizedValue,
        normalizedY = snappedY.normalizedValue,
        xIndex = snappedX.index,
        yIndex = snappedY.index
    )
}

internal fun resolveColorCardTraceContinuousAdjustmentFromTouch(
    touchX: Float,
    touchY: Float,
    padSize: IntSize,
    useFullArea: Boolean = false
): ColorCardTraceAdjustmentDetent {
    val geometry = resolveColorCardTraceAdjustmentPadGeometry(
        width = padSize.width.toFloat(),
        height = padSize.height.toFloat(),
        useFullArea = useFullArea
    )
    val maxX = geometry.startX + (geometry.stepX * (geometry.columns - 1))
    val maxY = geometry.startY + (geometry.stepY * (geometry.rows - 1))
    val normalizedX = if (maxX > geometry.startX) {
        ((touchX - geometry.startX) / (maxX - geometry.startX)).coerceIn(0f, 1f)
    } else {
        0.5f
    }
    val normalizedY = if (maxY > geometry.startY) {
        ((touchY - geometry.startY) / (maxY - geometry.startY)).coerceIn(0f, 1f)
    } else {
        0.5f
    }
    return ColorCardTraceAdjustmentDetent(
        normalizedX = normalizedX,
        normalizedY = normalizedY,
        xIndex = (normalizedX * (geometry.columns - 1)).roundToInt().coerceIn(0, geometry.columns - 1),
        yIndex = (normalizedY * (geometry.rows - 1)).roundToInt().coerceIn(0, geometry.rows - 1)
    )
}

private fun snapColorCardTraceAxisDetentFromTouch(
    touch: Float,
    start: Float,
    step: Float,
    detents: Int
): ColorCardTraceVerticalDetent {
    if (detents <= 1 || step <= 0f) {
        return ColorCardTraceVerticalDetent(normalizedValue = 0.5f, index = 0)
    }
    val lastIndex = detents - 1
    val rawIndex = ((touch - start) / step).roundToInt().coerceIn(0, lastIndex)
    return ColorCardTraceVerticalDetent(
        normalizedValue = rawIndex.toFloat() / lastIndex.toFloat(),
        index = rawIndex
    )
}

private fun updateColorCardTraceStrengthFromTouch(
    touchX: Float,
    sliderWidth: Int,
    onValueChange: (Float) -> Unit
) {
    val normalized = if (sliderWidth > 0) {
        (touchX / sliderWidth.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    onValueChange(normalized)
}

internal fun normalizeColorCardTraceGradientColors(
    colors: List<Int>,
    fallbackColors: List<Int>
): List<Int> {
    return when {
        colors.size >= 2 -> colors
        colors.size == 1 -> listOf(colors.first(), colors.first())
        fallbackColors.size >= 2 -> fallbackColors
        fallbackColors.size == 1 -> listOf(fallbackColors.first(), fallbackColors.first())
        else -> listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
    }
}

internal fun resolveColorCardTraceCenteredThumbOffset(
    trackSize: Int,
    thumbSize: Int,
    fraction: Float
): Int {
    if (trackSize <= 0 || thumbSize <= 0) return 0
    val maxOffset = (trackSize - thumbSize).coerceAtLeast(0)
    val centeredOffset = ((fraction.coerceIn(0f, 1f) * trackSize).roundToInt() - (thumbSize / 2))
    return centeredOffset.coerceIn(0, maxOffset)
}

private fun snapColorCardTraceVerticalValueFromTouch(
    touchY: Float,
    sliderHeight: Int
): ColorCardTraceVerticalDetent {
    val normalized = if (sliderHeight > 0) {
        (1f - (touchY / sliderHeight.toFloat())).coerceIn(0f, 1f)
    } else {
        0.5f
    }
    return snapColorCardTraceDetent(normalized)
}

private fun snapColorCardTraceDetent(
    normalized: Float
): ColorCardTraceVerticalDetent {
    val safeNormalized = normalized.coerceIn(0f, 1f)
    val stepCount = (COLOR_CARD_TRACE_ADJUSTMENT_DETENTS - 1).coerceAtLeast(1)
    val index = (safeNormalized * stepCount).roundToInt().coerceIn(0, stepCount)
    return ColorCardTraceVerticalDetent(
        normalizedValue = index.toFloat() / stepCount.toFloat(),
        index = index
    )
}

private fun colorCardTraceNormalizedToSigned(
    normalized: Float
): Float = ((normalized.coerceIn(0f, 1f) - 0.5f) * 2f).coerceIn(-1f, 1f)

private fun isColorCardTraceNearPickerHandle(
    touchX: Float,
    touchY: Float,
    points: List<HomeCardColorPickerPoint>,
    overlaySize: IntSize,
    touchRadiusPx: Float
): Boolean {
    if (overlaySize.width <= 0 || overlaySize.height <= 0) return false
    return points.any { point ->
        val pointX = point.x * overlaySize.width.toFloat()
        val pointY = point.y * overlaySize.height.toFloat()
        hypot((touchX - pointX).toDouble(), (touchY - pointY).toDouble()) <= touchRadiusPx
    }
}

private fun lerpColorCardTrace(
    start: Double,
    end: Double,
    fraction: Double
): Double {
    return start + ((end - start) * fraction)
}
