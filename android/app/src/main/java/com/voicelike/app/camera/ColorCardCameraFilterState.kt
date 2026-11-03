package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.voicelike.app.colorcard.portrait.ColorCardPortraitOptimizationParams
import com.voicelike.app.colorcard.portrait.COLOR_CARD_TRACE_PORTRAIT_DISABLED
import com.voicelike.app.colorcard.portrait.resolveColorCardPortraitOptimizationParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private const val COLOR_CARD_CAMERA_WHITE_BALANCE_COLOR_SCALE = 0.72f
private const val COLOR_CARD_CAMERA_EXPOSURE_BRIGHTNESS_SCALE = 0.40f
private const val COLOR_CARD_CAMERA_FILTER_LOAD_RETRY_COUNT = 3
private const val COLOR_CARD_CAMERA_FILTER_LOAD_RETRY_DELAY_MS = 180L

@Volatile
private var colorCardCameraBuiltinFilterCache: List<ColorCardBuiltinHaldFilterAsset>? = null

internal enum class ColorCardCameraLensMode(
    val label: String,
    val targetFocalMm: Float?
) {
    Native(label = "原生", targetFocalMm = null),
    Eq35(label = "35", targetFocalMm = 35f),
    Eq50(label = "50", targetFocalMm = 50f),
    Tele1(label = "T1", targetFocalMm = null),
    Tele2(label = "T2", targetFocalMm = null)
}

internal enum class ColorCardCameraFilterSeries(
    val label: String
) {
    Lut(label = "滤镜"),
    Style(label = "风格")
}

private data class ColorCardCameraStyleTemplateSpec(
    val id: String,
    val title: String,
    val colors: List<Int>,
    val baseColor: Float,
    val baseTone: Float,
    val baseBrightness: Float,
    val baseSaturation: Float,
    val baseSharpen: Float,
    val xColorScale: Float,
    val xSaturationScale: Float,
    val xDesaturatePenalty: Float,
    val yToneScale: Float,
    val yBrightnessScale: Float,
    val yShadowBias: Float,
    val mappingBlendScale: Float,
    val mappingLightnessScale: Float,
    val mappingChromaScale: Float,
    val defaultStrength: Float,
    val monochrome: Boolean = false
)

internal data class ColorCardCameraStyleTemplate(
    val candidate: ColorCardTraceFilterCandidate,
    val colorAxis: ColorCardTraceColorAxis,
    val baseColor: Float,
    val baseTone: Float,
    val baseBrightness: Float,
    val baseSaturation: Float,
    val baseSharpen: Float,
    val xColorScale: Float,
    val xSaturationScale: Float,
    val xDesaturatePenalty: Float,
    val yToneScale: Float,
    val yBrightnessScale: Float,
    val yShadowBias: Float,
    val mappingBlendScale: Float,
    val mappingLightnessScale: Float,
    val mappingChromaScale: Float,
    val defaultStrength: Float,
    val monochrome: Boolean
)

private const val COLOR_CARD_CAMERA_STYLE_X_RESPONSE_GAIN = 1.65f
private const val COLOR_CARD_CAMERA_STYLE_Y_RESPONSE_GAIN = 1.50f
private val COLOR_CARD_CAMERA_STYLE_SOURCE_COLORS = listOf(
    0xFFF5F4F1.toInt(),
    0xFFDED9D2.toInt(),
    0xFFBBB4AE.toInt(),
    0xFF8C8784.toInt(),
    0xFF585658.toInt(),
    0xFF1D1F23.toInt()
)
private const val COLOR_CARD_CAMERA_STYLE_PREVIEW_MAX_SIDE = 256
private const val COLOR_CARD_CAMERA_STYLE_PREVIEW_STRENGTH = 1.08f
private const val COLOR_CARD_CAMERA_STYLE_PREVIEW_BATCH_SIZE = 4

internal data class ColorCardCameraCapturedItem(
    val id: Long,
    val previewUri: String,
    val finalUri: String? = null,
    val pending: Boolean = true
)

internal data class ColorCardCameraRenderAdjustments(
    val whiteBalanceShift: Float = 0f,
    val exposureCompensation: Float = 0f,
    val previewEffect: ColorCardCameraPreviewEffect = ColorCardCameraPreviewEffect.None,
    val portraitAdjustments: ColorCardTracePortraitAdjustments = resolveColorCardCameraPortraitAdjustments()
)

internal data class ColorCardCameraFilterUiState(
    val activeSeries: ColorCardCameraFilterSeries,
    val candidates: List<ColorCardTraceFilterCandidate>,
    val browserCandidates: List<ColorCardTraceFilterCandidate>,
    val selectedCandidate: ColorCardTraceFilterCandidate?,
    val renderSelectedCandidate: ColorCardTraceFilterCandidate?,
    val selectedStyleTemplate: ColorCardCameraStyleTemplate?,
    val selectedIndex: Int,
    val color: Float,
    val tone: Float,
    val sharpen: Float,
    val saturation: Float,
    val brightness: Float,
    val strength: Float,
    val colorAxis: ColorCardTraceColorAxis,
    val renderColorAxis: ColorCardTraceColorAxis,
    val haldBitmap: Bitmap?,
    val renderHaldBitmap: Bitmap?,
    val haldSecondaryBitmap: Bitmap?,
    val renderHaldSecondaryBitmap: Bitmap?,
    val haldVariantMix: Float,
    val renderHaldVariantMix: Float,
    val filmGrainEligible: Boolean,
    val filmGrainEnabled: Boolean,
    val filmGrainAssets: List<ColorCardTraceFilmGrainAsset>,
    val selectedFilmGrainAsset: ColorCardTraceFilmGrainAsset?,
    val selectedFilmGrainBitmap: Bitmap?,
    val stepFilterBy: (Int) -> Unit,
    val updateAdjustment: (Float, Float) -> Unit,
    val resetAdjustment: () -> Unit,
    val resetManualAdjustments: () -> Unit,
    val updateSharpen: (Float) -> Unit,
    val updateSaturation: (Float) -> Unit,
    val updateBrightness: (Float) -> Unit,
    val updateStrength: (Float) -> Unit,
    val selectCandidate: (String) -> Unit,
    val setSeries: (ColorCardCameraFilterSeries) -> Unit,
    val setFilmGrainEnabled: (Boolean) -> Unit,
    val selectFilmGrainAsset: (String) -> Unit,
    val refreshFilmGrainAsset: () -> Unit,
    val revealAnimationNonce: Int
) {
    fun buildPreviewParams(sourceBitmap: Bitmap?): ColorCardTraceGpuParams {
        val width = sourceBitmap?.width?.coerceAtLeast(1) ?: 1
        val height = sourceBitmap?.height?.coerceAtLeast(1) ?: 1
        return buildPreviewParams(width, height)
    }

    fun buildPreviewParams(
        sourceWidth: Int,
        sourceHeight: Int,
        faceRegions: List<ColorCardTraceFaceRegion> = emptyList(),
        portraitAdjustments: ColorCardTracePortraitAdjustments = resolveColorCardCameraPortraitAdjustments()
    ): ColorCardTraceGpuParams {
        val width = sourceWidth.coerceAtLeast(1)
        val height = sourceHeight.coerceAtLeast(1)
        val effectiveSelectedCandidate = when (activeSeries) {
            ColorCardCameraFilterSeries.Style -> selectedCandidate
            ColorCardCameraFilterSeries.Lut -> renderSelectedCandidate
        }
        val effectiveColorAxis = when (activeSeries) {
            ColorCardCameraFilterSeries.Style -> colorAxis
            ColorCardCameraFilterSeries.Lut -> renderColorAxis
        }
        val portraitParams = resolveColorCardCameraPortraitGpuParams(
            selectedCandidate = effectiveSelectedCandidate,
            faceRegions = faceRegions,
            portraitAdjustments = portraitAdjustments
        )
        selectedStyleTemplate?.let { template ->
            return buildColorCardCameraStyleGpuParams(
                template = template,
                color = color,
                tone = tone,
                sharpen = sharpen,
                saturation = saturation,
                brightness = brightness,
                strength = strength,
                faceRegions = faceRegions,
                portraitParams = portraitParams,
                sourceAspect = width.toFloat() / height.toFloat(),
                sourceTexelWidth = 1f / width.toFloat(),
                sourceTexelHeight = 1f / height.toFloat()
            )
        }
        return buildStandardFilterGpuParams(
            selectedCandidate = effectiveSelectedCandidate,
            haldBitmap = renderHaldBitmap,
            haldSecondaryBitmap = renderHaldSecondaryBitmap,
            haldVariantMix = renderHaldVariantMix,
            color = color,
            colorAxis = effectiveColorAxis,
            sharpen = sharpen,
            tone = tone,
            saturation = saturation,
            brightness = brightness,
            strength = strength,
            faceRegions = faceRegions,
            portraitParams = portraitParams,
            sourceAspect = width.toFloat() / height.toFloat(),
            sourceTexelWidth = 1f / width.toFloat(),
            sourceTexelHeight = 1f / height.toFloat()
        )
    }

    fun buildRenderState(
        previewBitmap: Bitmap?,
        faceRegions: List<ColorCardTraceFaceRegion>,
        portraitAdjustments: ColorCardTracePortraitAdjustments
    ): ColorCardTraceFilterState {
        val gpuParams = buildPreviewParams(previewBitmap)
        val base = buildPanelState()
        return ColorCardTraceFilterState(
            mode = base.mode,
            hasUserEdits = base.hasUserEdits,
            hasSavedFilters = base.hasSavedFilters,
            hasBuiltinFilters = base.hasBuiltinFilters,
            hasTraceFilters = base.hasTraceFilters,
            canSaveRenderedImage = base.canSaveRenderedImage,
            isAnalyzing = base.isAnalyzing,
            isRendering = base.isRendering,
            renderedPreviewBitmap = base.renderedPreviewBitmap,
            sourceColors = base.sourceColors,
            displayColors = base.displayColors,
            imitateTransferProfile = base.imitateTransferProfile,
            candidates = base.candidates,
            selectedCandidateIndex = base.selectedCandidateIndex,
            selectedCandidate = base.selectedCandidate,
            isOriginalSelected = base.isOriginalSelected,
            previewSourceBitmap = previewBitmap,
            faceRegions = faceRegions,
            subjectRect = base.subjectRect,
            gpuParams = gpuParams,
            portraitAvailable = faceRegions.isNotEmpty(),
            portraitAdjustments = portraitAdjustments,
            filmGrainEligible = base.filmGrainEligible,
            filmGrainEnabled = base.filmGrainEnabled,
            filmGrainAssets = base.filmGrainAssets,
            selectedFilmGrainAsset = base.selectedFilmGrainAsset,
            selectedFilmGrainBitmap = base.selectedFilmGrainBitmap,
            color = base.color,
            colorAxis = base.colorAxis,
            sharpen = base.sharpen,
            tone = base.tone,
            saturation = base.saturation,
            brightness = base.brightness,
            paletteStrength = base.paletteStrength,
            allowsManualAdjustment = base.allowsManualAdjustment,
            allowsStrengthAdjustment = base.allowsStrengthAdjustment,
            stepFilterBy = base.stepFilterBy,
            selectOriginal = base.selectOriginal,
            selectCandidate = base.selectCandidate,
            updateAdjustment = base.updateAdjustment,
            updateSharpen = base.updateSharpen,
            updateSaturation = base.updateSaturation,
            updateBrightness = base.updateBrightness,
            updatePaletteStrength = base.updatePaletteStrength,
            updatePortraitVitality = base.updatePortraitVitality,
            updatePortraitSoftness = base.updatePortraitSoftness,
            updatePortraitDetailSuppression = base.updatePortraitDetailSuppression,
            setFilmGrainEnabled = base.setFilmGrainEnabled,
            selectFilmGrainAsset = base.selectFilmGrainAsset,
            refreshFilmGrainAsset = base.refreshFilmGrainAsset,
            resetAdjustment = base.resetAdjustment,
            resetManualAdjustments = base.resetManualAdjustments,
            resetPortraitAdjustments = base.resetPortraitAdjustments
        )
    }

    fun buildPreviewState(
        previewBitmap: Bitmap?
    ): ColorCardTraceFilterState {
        val base = buildPanelState()
        return base.copy(
            previewSourceBitmap = previewBitmap,
            gpuParams = buildPreviewParams(previewBitmap)
        )
    }

    fun buildPanelState(): ColorCardTraceFilterState {
        val gpuParams = buildPreviewParams(null)
        return ColorCardTraceFilterState(
            mode = ColorCardTracePanelMode.Filter,
            hasUserEdits = true,
            hasSavedFilters = candidates.isNotEmpty(),
            hasBuiltinFilters = candidates.isNotEmpty(),
            hasTraceFilters = false,
            canSaveRenderedImage = selectedCandidate != null,
            isAnalyzing = candidates.isEmpty(),
            isRendering = selectedCandidate != null,
            renderedPreviewBitmap = null,
            sourceColors = emptyList(),
            displayColors = selectedCandidate?.colors.orEmpty(),
            imitateTransferProfile = null,
            candidates = candidates,
            selectedCandidateIndex = selectedIndex,
            selectedCandidate = selectedCandidate,
            isOriginalSelected = false,
            previewSourceBitmap = null,
            faceRegions = emptyList(),
            subjectRect = null,
            gpuParams = gpuParams,
            portraitAvailable = false,
            portraitAdjustments = ColorCardTracePortraitAdjustments(),
            filmGrainEligible = filmGrainEligible,
            filmGrainEnabled = filmGrainEnabled,
            filmGrainAssets = filmGrainAssets,
            selectedFilmGrainAsset = selectedFilmGrainAsset,
            selectedFilmGrainBitmap = selectedFilmGrainBitmap,
            color = color,
            colorAxis = colorAxis,
            sharpen = sharpen,
            tone = tone,
            saturation = saturation,
            brightness = brightness,
            paletteStrength = strength,
            allowsManualAdjustment = true,
            allowsStrengthAdjustment = selectedCandidate?.kind != ColorCardTraceFilterKind.PixelPoster,
            stepFilterBy = stepFilterBy,
            selectOriginal = {},
            selectCandidate = selectCandidate,
            updateAdjustment = updateAdjustment,
            updateSharpen = updateSharpen,
            updateSaturation = updateSaturation,
            updateBrightness = updateBrightness,
            updatePaletteStrength = updateStrength,
            updatePortraitVitality = {},
            updatePortraitSoftness = {},
            updatePortraitDetailSuppression = {},
            setFilmGrainEnabled = setFilmGrainEnabled,
            selectFilmGrainAsset = selectFilmGrainAsset,
            refreshFilmGrainAsset = refreshFilmGrainAsset,
            resetAdjustment = resetAdjustment,
            resetManualAdjustments = resetManualAdjustments,
            resetPortraitAdjustments = {}
        )
    }
}

internal fun buildColorCardCameraRenderParams(
    filterState: ColorCardCameraFilterUiState,
    sourceWidth: Int,
    sourceHeight: Int,
    faceRegions: List<ColorCardTraceFaceRegion> = emptyList(),
    portraitAdjustments: ColorCardTracePortraitAdjustments = resolveColorCardCameraPortraitAdjustments(),
    renderAdjustments: ColorCardCameraRenderAdjustments = ColorCardCameraRenderAdjustments()
): ColorCardTraceGpuParams {
    val baseParams = filterState.buildPreviewParams(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        faceRegions = faceRegions,
        portraitAdjustments = portraitAdjustments
    )
    val adjustedParams = baseParams.copy(
        color = (
            baseParams.color +
                (renderAdjustments.whiteBalanceShift * COLOR_CARD_CAMERA_WHITE_BALANCE_COLOR_SCALE)
            ).coerceIn(-1f, 1f),
        colorAxisA = if (renderAdjustments.whiteBalanceShift != 0f) 0f else baseParams.colorAxisA,
        colorAxisB = if (renderAdjustments.whiteBalanceShift != 0f) -1f else baseParams.colorAxisB,
        brightness = (
            baseParams.brightness +
                (renderAdjustments.exposureCompensation * COLOR_CARD_CAMERA_EXPOSURE_BRIGHTNESS_SCALE)
            ).coerceIn(-1f, 1f)
    )
    return applyColorCardCameraPreviewEffect(
        params = adjustedParams,
        effect = renderAdjustments.previewEffect
    )
}

private fun resolveColorCardCameraPortraitGpuParams(
    selectedCandidate: ColorCardTraceFilterCandidate?,
    faceRegions: List<ColorCardTraceFaceRegion>,
    portraitAdjustments: ColorCardTracePortraitAdjustments
): ColorCardPortraitOptimizationParams {
    val portraitFaceRegions = if (portraitAdjustments.hasAdjustments) {
        filterColorCardCameraBeautyFaceRegions(faceRegions)
    } else {
        faceRegions
    }
    val portraitAvailable = resolveColorCardTracePortraitFeatureAvailable(
        panelMode = ColorCardTracePanelMode.Filter,
        selectedCandidate = selectedCandidate,
        faceRegions = portraitFaceRegions
    )
    val basePortraitParams = resolveColorCardPortraitOptimizationParams(
        panelMode = ColorCardTracePanelMode.Filter,
        selectedCandidate = selectedCandidate,
        faceRegions = portraitFaceRegions
    )
    return resolveColorCardTracePortraitFeatureParams(
        enabled = portraitAvailable,
        faceRegions = portraitFaceRegions,
        baseParams = basePortraitParams,
        adjustments = portraitAdjustments
    )
}

private data class ColorCardCameraCommittedLutState(
    val selectedCandidate: ColorCardTraceFilterCandidate? = null,
    val colorAxis: ColorCardTraceColorAxis = COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS,
    val haldBitmap: Bitmap? = null,
    val haldSecondaryBitmap: Bitmap? = null,
    val haldVariantMix: Float = 0f
)

@Composable
internal fun rememberColorCardCameraFilterState(): ColorCardCameraFilterUiState {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val builtinHaldFilters by produceState(
        initialValue = emptyList<ColorCardBuiltinHaldFilterAsset>(),
        key1 = appContext
    ) {
        value = loadColorCardCameraBuiltinFiltersResilient(appContext)
    }
    val rawLutCandidates = remember(builtinHaldFilters) {
        buildStandardFilterCandidates(builtinHaldFilters)
    }
    val rawStyleTemplates = remember {
        buildColorCardCameraStyleTemplates()
    }
    val styleDisplayTitles = remember(rawStyleTemplates) {
        buildColorCardCameraFilterDisplayTitles(
            candidates = rawStyleTemplates.map(ColorCardCameraStyleTemplate::candidate)
        )
    }
    val lutCandidates = rawLutCandidates
    val styleTemplates = remember(rawStyleTemplates, styleDisplayTitles) {
        rawStyleTemplates.map { template ->
            template.copy(
                candidate = template.candidate.copy(
                    title = styleDisplayTitles[template.candidate.paletteId] ?: template.candidate.title
                )
            )
        }
    }
    val styleCandidates = remember(styleTemplates) {
        styleTemplates.map(ColorCardCameraStyleTemplate::candidate)
    }
    val filmGrainAssets by produceState(
        initialValue = emptyList<ColorCardTraceFilmGrainAsset>(),
        key1 = appContext
    ) {
        value = loadColorCardTraceFilmGrainAssets(appContext)
    }
    var activeSeries by remember { mutableStateOf(ColorCardCameraFilterSeries.Lut) }
    var selectedLutCandidateId by remember { mutableStateOf<String?>(null) }
    var selectedStyleCandidateId by remember(styleCandidates) {
        mutableStateOf(styleCandidates.firstOrNull()?.paletteId)
    }
    LaunchedEffect(lutCandidates) {
        val candidateIds = lutCandidates.map(ColorCardTraceFilterCandidate::paletteId)
        if (candidateIds.isEmpty()) {
            selectedLutCandidateId = null
        } else if (selectedLutCandidateId !in candidateIds) {
            selectedLutCandidateId = candidateIds[Random.nextInt(candidateIds.size)]
        }
    }
    LaunchedEffect(styleCandidates) {
        if (selectedStyleCandidateId !in styleCandidates.map(ColorCardTraceFilterCandidate::paletteId).toSet()) {
            selectedStyleCandidateId = styleCandidates.firstOrNull()?.paletteId
        }
    }
    var lutColor by remember { mutableFloatStateOf(0f) }
    var lutTone by remember { mutableFloatStateOf(0f) }
    var lutSharpen by remember { mutableFloatStateOf(0f) }
    var lutSaturation by remember { mutableFloatStateOf(0f) }
    var lutBrightness by remember { mutableFloatStateOf(0f) }
    var lutStrength by remember { mutableFloatStateOf(0.5f) }
    var styleColor by remember { mutableFloatStateOf(0f) }
    var styleTone by remember { mutableFloatStateOf(0f) }
    var styleSharpen by remember { mutableFloatStateOf(0f) }
    var styleSaturation by remember { mutableFloatStateOf(0f) }
    var styleBrightness by remember { mutableFloatStateOf(0f) }
    var styleStrength by remember { mutableFloatStateOf(0.62f) }
    var filmGrainEnabled by remember { mutableStateOf(false) }
    var selectedFilmGrainId by remember { mutableStateOf<String?>(null) }
    var revealAnimationNonce by remember { mutableStateOf(0) }
    val haldBitmapCache = remember { mutableStateMapOf<String, Bitmap>() }

    DisposableEffect(Unit) {
        onDispose {
            haldBitmapCache.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            haldBitmapCache.clear()
        }
    }

    val activeCandidates = remember(activeSeries, lutCandidates, styleCandidates) {
        when (activeSeries) {
            ColorCardCameraFilterSeries.Lut -> lutCandidates
            ColorCardCameraFilterSeries.Style -> styleCandidates
        }
    }
    val browserCandidates = remember(lutCandidates, styleCandidates) {
        lutCandidates + styleCandidates
    }
    val selectedCandidate = remember(
        activeSeries,
        lutCandidates,
        selectedLutCandidateId,
        styleCandidates,
        selectedStyleCandidateId
    ) {
        when (activeSeries) {
            ColorCardCameraFilterSeries.Lut ->
                lutCandidates.firstOrNull { it.paletteId == selectedLutCandidateId }
            ColorCardCameraFilterSeries.Style ->
                styleCandidates.firstOrNull { it.paletteId == selectedStyleCandidateId }
        }
    }
    val selectedStyleTemplate = remember(styleTemplates, selectedStyleCandidateId) {
        styleTemplates.firstOrNull { it.candidate.paletteId == selectedStyleCandidateId }
    }
    val selectedIndex = remember(activeCandidates, selectedCandidate?.paletteId) {
        activeCandidates.indexOfFirst { it.paletteId == selectedCandidate?.paletteId }.coerceAtLeast(0)
    }
    LaunchedEffect(activeSeries, selectedCandidate?.paletteId) {
        when (activeSeries) {
            ColorCardCameraFilterSeries.Lut -> {
                lutStrength = defaultColorCardTracePaletteStrength(selectedCandidate)
            }
            ColorCardCameraFilterSeries.Style -> {
                styleStrength = selectedStyleTemplate?.defaultStrength ?: 0.62f
            }
        }
    }
    val filmGrainEligible = remember(activeSeries, selectedCandidate) {
        activeSeries == ColorCardCameraFilterSeries.Lut &&
            isColorCardTraceFilmGrainEligible(selectedCandidate)
    }
    LaunchedEffect(filmGrainAssets) {
        if (selectedFilmGrainId == null) {
            selectedFilmGrainId = filmGrainAssets.firstOrNull()?.id
        }
    }
    val colorAxis = remember(activeSeries, selectedCandidate?.paletteId, selectedStyleTemplate) {
        when (activeSeries) {
            ColorCardCameraFilterSeries.Lut -> resolveColorCardTraceColorAxis(
                sourceColors = emptyList(),
                targetColors = selectedCandidate?.colors.orEmpty()
            )
            ColorCardCameraFilterSeries.Style ->
                selectedStyleTemplate?.colorAxis ?: COLOR_CARD_TRACE_DEFAULT_COLOR_AXIS
        }
    }
    val resolvedBlend = remember(selectedCandidate?.paletteId, lutStrength, activeSeries) {
        resolveColorCardTraceHaldBlend(
            candidate = selectedCandidate,
            paletteStrength = if (activeSeries == ColorCardCameraFilterSeries.Lut) lutStrength else 0f
        )
    }
    val selectedHaldBitmap by produceState<Bitmap?>(
        initialValue = null,
        context,
        activeSeries,
        resolvedBlend.primaryAssetName
    ) {
        if (activeSeries != ColorCardCameraFilterSeries.Lut) {
            value = null
            return@produceState
        }
        val assetName = resolvedBlend.primaryAssetName
        if (assetName.isNullOrBlank()) {
            value = null
            return@produceState
        }
        haldBitmapCache[assetName]?.let {
            value = it
            return@produceState
        }
        val decoded = decodeColorCardBuiltinHaldBitmap(
            context = context.applicationContext,
            assetName = assetName
        )
        if (decoded != null) {
            haldBitmapCache[assetName] = decoded
        }
        value = decoded
    }
    val secondaryHaldBitmap by produceState<Bitmap?>(
        initialValue = null,
        context,
        activeSeries,
        resolvedBlend.secondaryAssetName
    ) {
        if (activeSeries != ColorCardCameraFilterSeries.Lut) {
            value = null
            return@produceState
        }
        val assetName = resolvedBlend.secondaryAssetName
        if (assetName.isNullOrBlank()) {
            value = null
            return@produceState
        }
        haldBitmapCache[assetName]?.let {
            value = it
            return@produceState
        }
        val decoded = decodeColorCardBuiltinHaldBitmap(
            context = context.applicationContext,
            assetName = assetName
        )
        if (decoded != null) {
            haldBitmapCache[assetName] = decoded
        }
        value = decoded
    }
    val selectedFilmGrainAsset = remember(filmGrainAssets, selectedFilmGrainId) {
        filmGrainAssets.firstOrNull { it.id == selectedFilmGrainId } ?: filmGrainAssets.firstOrNull()
    }
    val selectedFilmGrainBitmap by produceState<Bitmap?>(
        initialValue = null,
        context,
        activeSeries,
        filmGrainEnabled,
        filmGrainEligible,
        selectedFilmGrainAsset?.assetName
    ) {
        if (activeSeries != ColorCardCameraFilterSeries.Lut) {
            value = null
            return@produceState
        }
        if (!filmGrainEnabled || !filmGrainEligible) {
            value = null
            return@produceState
        }
        val assetName = selectedFilmGrainAsset?.assetName ?: run {
            value = null
            return@produceState
        }
        value = decodeColorCardTraceFilmGrainBitmap(
            context = context.applicationContext,
            assetName = assetName
        )
    }
    var committedLutState by remember { mutableStateOf(ColorCardCameraCommittedLutState()) }
    val lutPrimaryReady = resolvedBlend.primaryAssetName.isNullOrBlank() || selectedHaldBitmap != null
    val lutSecondaryReady = resolvedBlend.secondaryAssetName.isNullOrBlank() || secondaryHaldBitmap != null
    val lutRenderAssetsReady = lutPrimaryReady && lutSecondaryReady
    LaunchedEffect(
        activeSeries,
        selectedCandidate?.paletteId,
        colorAxis,
        selectedHaldBitmap,
        secondaryHaldBitmap,
        resolvedBlend.variantMix,
        lutRenderAssetsReady
    ) {
        if (activeSeries != ColorCardCameraFilterSeries.Lut) return@LaunchedEffect
        if (!lutRenderAssetsReady) return@LaunchedEffect
        committedLutState = ColorCardCameraCommittedLutState(
            selectedCandidate = selectedCandidate,
            colorAxis = colorAxis,
            haldBitmap = selectedHaldBitmap,
            haldSecondaryBitmap = secondaryHaldBitmap,
            haldVariantMix = resolvedBlend.variantMix
        )
    }
    val renderSelectedCandidate = if (activeSeries == ColorCardCameraFilterSeries.Lut && !lutRenderAssetsReady) {
        committedLutState.selectedCandidate
    } else {
        selectedCandidate
    }
    val renderColorAxis = if (activeSeries == ColorCardCameraFilterSeries.Lut && !lutRenderAssetsReady) {
        committedLutState.colorAxis
    } else {
        colorAxis
    }
    val renderHaldBitmap = if (activeSeries == ColorCardCameraFilterSeries.Lut && !lutRenderAssetsReady) {
        committedLutState.haldBitmap
    } else {
        selectedHaldBitmap
    }
    val renderSecondaryHaldBitmap = if (activeSeries == ColorCardCameraFilterSeries.Lut && !lutRenderAssetsReady) {
        committedLutState.haldSecondaryBitmap
    } else {
        secondaryHaldBitmap
    }
    val renderHaldVariantMix = if (activeSeries == ColorCardCameraFilterSeries.Lut && !lutRenderAssetsReady) {
        committedLutState.haldVariantMix
    } else {
        resolvedBlend.variantMix
    }
    fun currentSelectionId(series: ColorCardCameraFilterSeries): String? {
        return when (series) {
            ColorCardCameraFilterSeries.Lut -> selectedLutCandidateId
            ColorCardCameraFilterSeries.Style -> selectedStyleCandidateId
        }
    }
    fun applySelection(
        series: ColorCardCameraFilterSeries,
        paletteId: String?,
        animateReveal: Boolean = true
    ) {
        val previousSeries = activeSeries
        val previousSelectionId = currentSelectionId(previousSeries)
        when (series) {
            ColorCardCameraFilterSeries.Lut -> {
                activeSeries = ColorCardCameraFilterSeries.Lut
                selectedLutCandidateId = paletteId
            }
            ColorCardCameraFilterSeries.Style -> {
                activeSeries = ColorCardCameraFilterSeries.Style
                selectedStyleCandidateId = paletteId
            }
        }
        val selectionChanged = previousSeries != series || previousSelectionId != paletteId
        if (animateReveal && selectionChanged) {
            revealAnimationNonce += 1
        }
    }
    return ColorCardCameraFilterUiState(
        activeSeries = activeSeries,
        candidates = activeCandidates,
        browserCandidates = browserCandidates,
        selectedCandidate = selectedCandidate,
        renderSelectedCandidate = renderSelectedCandidate,
        selectedStyleTemplate = if (activeSeries == ColorCardCameraFilterSeries.Style) selectedStyleTemplate else null,
        selectedIndex = selectedIndex,
        color = if (activeSeries == ColorCardCameraFilterSeries.Style) styleColor else lutColor,
        tone = if (activeSeries == ColorCardCameraFilterSeries.Style) styleTone else lutTone,
        sharpen = if (activeSeries == ColorCardCameraFilterSeries.Style) styleSharpen else lutSharpen,
        saturation = if (activeSeries == ColorCardCameraFilterSeries.Style) styleSaturation else lutSaturation,
        brightness = if (activeSeries == ColorCardCameraFilterSeries.Style) styleBrightness else lutBrightness,
        strength = if (activeSeries == ColorCardCameraFilterSeries.Style) styleStrength else lutStrength,
        colorAxis = colorAxis,
        renderColorAxis = renderColorAxis,
        haldBitmap = if (activeSeries == ColorCardCameraFilterSeries.Lut) selectedHaldBitmap else null,
        renderHaldBitmap = if (activeSeries == ColorCardCameraFilterSeries.Lut) renderHaldBitmap else null,
        haldSecondaryBitmap = if (activeSeries == ColorCardCameraFilterSeries.Lut) secondaryHaldBitmap else null,
        renderHaldSecondaryBitmap = if (activeSeries == ColorCardCameraFilterSeries.Lut) renderSecondaryHaldBitmap else null,
        haldVariantMix = if (activeSeries == ColorCardCameraFilterSeries.Lut) resolvedBlend.variantMix else 0f,
        renderHaldVariantMix = if (activeSeries == ColorCardCameraFilterSeries.Lut) renderHaldVariantMix else 0f,
        filmGrainEligible = filmGrainEligible,
        filmGrainEnabled = activeSeries == ColorCardCameraFilterSeries.Lut && filmGrainEnabled,
        filmGrainAssets = filmGrainAssets,
        selectedFilmGrainAsset = selectedFilmGrainAsset,
        selectedFilmGrainBitmap = selectedFilmGrainBitmap,
        stepFilterBy = { delta ->
            if (browserCandidates.isNotEmpty()) {
                val currentSelectedId = when (activeSeries) {
                    ColorCardCameraFilterSeries.Style -> selectedStyleCandidateId
                    ColorCardCameraFilterSeries.Lut -> selectedLutCandidateId
                }
                val currentIndex = browserCandidates.indexOfFirst { it.paletteId == currentSelectedId }
                    .takeIf { it >= 0 }
                    ?: 0
                val nextIndex = (currentIndex + delta).floorMod(browserCandidates.size)
                val nextPaletteId = browserCandidates[nextIndex].paletteId
                applySelection(
                    series = if (nextPaletteId.startsWith("camera_style:")) {
                        ColorCardCameraFilterSeries.Style
                    } else {
                        ColorCardCameraFilterSeries.Lut
                    },
                    paletteId = nextPaletteId
                )
            }
        },
        updateAdjustment = { normalizedX, normalizedY ->
            val nextColor = ((normalizedX.coerceIn(0f, 1f) - 0.5f) * 2f).coerceIn(-1f, 1f)
            val nextTone = ((0.5f - normalizedY.coerceIn(0f, 1f)) * 2f).coerceIn(-1f, 1f)
            if (activeSeries == ColorCardCameraFilterSeries.Style) {
                styleColor = nextColor
                styleTone = nextTone
            } else {
                lutColor = nextColor
                lutTone = nextTone
            }
        },
        resetAdjustment = {
            if (activeSeries == ColorCardCameraFilterSeries.Style) {
                styleColor = 0f
                styleTone = 0f
            } else {
                lutColor = 0f
                lutTone = 0f
            }
        },
        resetManualAdjustments = {
            if (activeSeries == ColorCardCameraFilterSeries.Style) {
                styleColor = 0f
                styleTone = 0f
                styleSharpen = 0f
                styleSaturation = 0f
                styleBrightness = 0f
                styleStrength = selectedStyleTemplate?.defaultStrength ?: 0.62f
            } else {
                lutColor = 0f
                lutTone = 0f
                lutSharpen = 0f
                lutSaturation = 0f
                lutBrightness = 0f
                lutStrength = defaultColorCardTracePaletteStrength(selectedCandidate)
            }
            filmGrainEnabled = false
            selectedFilmGrainId = filmGrainAssets.firstOrNull()?.id
        },
        updateSharpen = {
            if (activeSeries == ColorCardCameraFilterSeries.Style) {
                styleSharpen = it.coerceIn(-1f, 1f)
            } else {
                lutSharpen = it.coerceIn(-1f, 1f)
            }
        },
        updateSaturation = {
            if (activeSeries == ColorCardCameraFilterSeries.Style) {
                styleSaturation = it.coerceIn(-1f, 1f)
            } else {
                lutSaturation = it.coerceIn(-1f, 1f)
            }
        },
        updateBrightness = {
            if (activeSeries == ColorCardCameraFilterSeries.Style) {
                styleBrightness = it.coerceIn(-1f, 1f)
            } else {
                lutBrightness = it.coerceIn(-1f, 1f)
            }
        },
        updateStrength = {
            if (activeSeries == ColorCardCameraFilterSeries.Style) {
                styleStrength = it.coerceIn(0f, 1f)
            } else {
                lutStrength = it.coerceIn(0f, 1f)
            }
        },
        selectCandidate = { paletteId ->
            applySelection(
                series = if (paletteId.startsWith("camera_style:")) {
                    ColorCardCameraFilterSeries.Style
                } else {
                    ColorCardCameraFilterSeries.Lut
                },
                paletteId = paletteId
            )
        },
        setSeries = { series ->
            applySelection(
                series = series,
                paletteId = currentSelectionId(series)
            )
        },
        setFilmGrainEnabled = { enabled ->
            filmGrainEnabled = enabled && filmGrainEligible
            if (enabled && selectedFilmGrainId == null) {
                selectedFilmGrainId = filmGrainAssets.firstOrNull()?.id
            }
        },
        selectFilmGrainAsset = { assetId ->
            selectedFilmGrainId = assetId
        },
        refreshFilmGrainAsset = {
            if (filmGrainAssets.isNotEmpty()) {
                val currentIndex = filmGrainAssets.indexOfFirst { it.id == selectedFilmGrainId }
                val nextIndex = if (currentIndex >= 0) {
                    (currentIndex + 1) % filmGrainAssets.size
                } else {
                    0
                }
                selectedFilmGrainId = filmGrainAssets[nextIndex].id
            }
        },
        revealAnimationNonce = revealAnimationNonce
    )
}

private suspend fun loadColorCardCameraBuiltinFiltersResilient(
    context: Context
): List<ColorCardBuiltinHaldFilterAsset> {
    val cached = colorCardCameraBuiltinFilterCache
    if (!cached.isNullOrEmpty()) {
        return cached
    }

    var loaded = emptyList<ColorCardBuiltinHaldFilterAsset>()
    repeat(COLOR_CARD_CAMERA_FILTER_LOAD_RETRY_COUNT) { attempt ->
        loaded = loadColorCardBuiltinHaldFilters(context)
        if (loaded.isNotEmpty()) {
            colorCardCameraBuiltinFilterCache = loaded
            return loaded
        }
        if (attempt < COLOR_CARD_CAMERA_FILTER_LOAD_RETRY_COUNT - 1) {
            delay(COLOR_CARD_CAMERA_FILTER_LOAD_RETRY_DELAY_MS)
        }
    }
    return colorCardCameraBuiltinFilterCache.orEmpty()
}

private fun Int.floorMod(size: Int): Int {
    if (size <= 0) return 0
    val mod = this % size
    return if (mod < 0) mod + size else mod
}

private fun buildStandardFilterCandidates(
    builtinHaldFilters: List<ColorCardBuiltinHaldFilterAsset>
): List<ColorCardTraceFilterCandidate> {
    return filterColorCardTraceBuiltinHaldFilters(builtinHaldFilters)
        .filterNot { it.canonicalId.startsWith("black-white-era-") }
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
}

private fun buildColorCardCameraStyleTemplates(): List<ColorCardCameraStyleTemplate> {
    return listOf(
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:cool_rose",
            title = "冷调玫瑰色",
            colors = listOf(0xFFF5F3FD.toInt(), 0xFFE0D7F1.toInt(), 0xFFBFC3EA.toInt(), 0xFF7C8AC5.toInt()),
            baseColor = 0.34f,
            baseTone = 0.10f,
            baseBrightness = 0.12f,
            baseSaturation = 0.04f,
            baseSharpen = 0.03f,
            xColorScale = 0.30f,
            xSaturationScale = 0.22f,
            xDesaturatePenalty = 0.36f,
            yToneScale = 0.22f,
            yBrightnessScale = 0.22f,
            yShadowBias = 0.05f,
            mappingBlendScale = 1.06f,
            mappingLightnessScale = 1.04f,
            mappingChromaScale = 1.18f,
            defaultStrength = 0.72f
        ),
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:neutral",
            title = "中性",
            colors = listOf(0xFFF4F2EF.toInt(), 0xFFE1E5E8.toInt(), 0xFFC8D3DE.toInt(), 0xFF94A8B7.toInt()),
            baseColor = 0.10f,
            baseTone = 0.03f,
            baseBrightness = 0.08f,
            baseSaturation = 0.02f,
            baseSharpen = 0.04f,
            xColorScale = 0.16f,
            xSaturationScale = 0.20f,
            xDesaturatePenalty = 0.28f,
            yToneScale = 0.18f,
            yBrightnessScale = 0.20f,
            yShadowBias = 0.05f,
            mappingBlendScale = 0.90f,
            mappingLightnessScale = 0.84f,
            mappingChromaScale = 0.78f,
            defaultStrength = 0.48f
        ),
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:rose_gold",
            title = "玫瑰金色",
            colors = listOf(0xFFFFF3F1.toInt(), 0xFFF0D8D1.toInt(), 0xFFDFAEA8.toInt(), 0xFFB97A7E.toInt()),
            baseColor = 0.42f,
            baseTone = -0.01f,
            baseBrightness = 0.08f,
            baseSaturation = 0.11f,
            baseSharpen = 0.03f,
            xColorScale = 0.34f,
            xSaturationScale = 0.28f,
            xDesaturatePenalty = 0.26f,
            yToneScale = 0.14f,
            yBrightnessScale = 0.16f,
            yShadowBias = 0.03f,
            mappingBlendScale = 1.08f,
            mappingLightnessScale = 0.96f,
            mappingChromaScale = 1.14f,
            defaultStrength = 0.74f
        ),
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:gold",
            title = "金色",
            colors = listOf(0xFFFFF3CF.toInt(), 0xFFF4D58B.toInt(), 0xFFDBB060.toInt(), 0xFFA77420.toInt()),
            baseColor = 0.22f,
            baseTone = 0.12f,
            baseBrightness = 0.16f,
            baseSaturation = 0.16f,
            baseSharpen = 0.05f,
            xColorScale = 0.24f,
            xSaturationScale = 0.30f,
            xDesaturatePenalty = 0.22f,
            yToneScale = 0.24f,
            yBrightnessScale = 0.26f,
            yShadowBias = 0.04f,
            mappingBlendScale = 1.14f,
            mappingLightnessScale = 1.06f,
            mappingChromaScale = 1.20f,
            defaultStrength = 0.80f
        ),
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:amber",
            title = "琥珀色",
            colors = listOf(0xFFF2D7A7.toInt(), 0xFFD59B54.toInt(), 0xFFAA6733.toInt(), 0xFF5A3520.toInt()),
            baseColor = 0.30f,
            baseTone = -0.10f,
            baseBrightness = -0.03f,
            baseSaturation = 0.14f,
            baseSharpen = 0.02f,
            xColorScale = 0.30f,
            xSaturationScale = 0.22f,
            xDesaturatePenalty = 0.20f,
            yToneScale = 0.30f,
            yBrightnessScale = 0.12f,
            yShadowBias = 0.15f,
            mappingBlendScale = 1.18f,
            mappingLightnessScale = 0.88f,
            mappingChromaScale = 1.26f,
            defaultStrength = 0.82f
        ),
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:quiet",
            title = "安静",
            colors = listOf(0xFFECE7E0.toInt(), 0xFFD0C6BE.toInt(), 0xFFA59992.toInt(), 0xFF69635E.toInt()),
            baseColor = -0.10f,
            baseTone = -0.08f,
            baseBrightness = 0.02f,
            baseSaturation = -0.24f,
            baseSharpen = -0.06f,
            xColorScale = 0.10f,
            xSaturationScale = 0.14f,
            xDesaturatePenalty = 0.20f,
            yToneScale = 0.20f,
            yBrightnessScale = 0.14f,
            yShadowBias = 0.10f,
            mappingBlendScale = 0.98f,
            mappingLightnessScale = 0.86f,
            mappingChromaScale = 0.70f,
            defaultStrength = 0.58f
        ),
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:dazzling",
            title = "耀目",
            colors = listOf(0xFFFFF4BB.toInt(), 0xFFFFD15E.toInt(), 0xFFFF8B53.toInt(), 0xFFF45138.toInt()),
            baseColor = 0.18f,
            baseTone = 0.18f,
            baseBrightness = 0.10f,
            baseSaturation = 0.24f,
            baseSharpen = 0.08f,
            xColorScale = 0.22f,
            xSaturationScale = 0.30f,
            xDesaturatePenalty = 0.18f,
            yToneScale = 0.28f,
            yBrightnessScale = 0.18f,
            yShadowBias = 0.12f,
            mappingBlendScale = 1.20f,
            mappingLightnessScale = 1.02f,
            mappingChromaScale = 1.28f,
            defaultStrength = 0.72f
        ),
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:contrast_pop",
            title = "反差色",
            colors = listOf(0xFFEFEAF9.toInt(), 0xFFC8BFE8.toInt(), 0xFF6774C4.toInt(), 0xFF1B244A.toInt()),
            baseColor = 0.16f,
            baseTone = 0.22f,
            baseBrightness = -0.06f,
            baseSaturation = 0.14f,
            baseSharpen = 0.09f,
            xColorScale = 0.20f,
            xSaturationScale = 0.24f,
            xDesaturatePenalty = 0.20f,
            yToneScale = 0.30f,
            yBrightnessScale = 0.16f,
            yShadowBias = 0.14f,
            mappingBlendScale = 1.12f,
            mappingLightnessScale = 0.92f,
            mappingChromaScale = 1.08f,
            defaultStrength = 0.70f
        ),
        ColorCardCameraStyleTemplateSpec(
            id = "camera_style:mono_contrast",
            title = "黑白对比",
            colors = listOf(0xFFFFFFFF.toInt(), 0xFFD8D8D8.toInt(), 0xFF7A7A7A.toInt(), 0xFF141414.toInt()),
            baseColor = 0f,
            baseTone = 0.26f,
            baseBrightness = -0.02f,
            baseSaturation = -0.90f,
            baseSharpen = 0.08f,
            xColorScale = 0f,
            xSaturationScale = 0.10f,
            xDesaturatePenalty = 0.02f,
            yToneScale = 0.34f,
            yBrightnessScale = 0.16f,
            yShadowBias = 0.18f,
            mappingBlendScale = 1.16f,
            mappingLightnessScale = 1.08f,
            mappingChromaScale = 0.08f,
            defaultStrength = 0.76f,
            monochrome = true
        )
    ).map { spec ->
        val candidate = ColorCardTraceFilterCandidate(
            paletteId = spec.id,
            kind = ColorCardTraceFilterKind.HaldClut,
            title = spec.title,
            sourceTag = "camera_style_series",
            colors = spec.colors,
            score = -1.0
        )
        ColorCardCameraStyleTemplate(
            candidate = candidate,
            colorAxis = resolveColorCardTraceColorAxis(emptyList(), spec.colors),
            baseColor = spec.baseColor,
            baseTone = spec.baseTone,
            baseBrightness = spec.baseBrightness,
            baseSaturation = spec.baseSaturation,
            baseSharpen = spec.baseSharpen,
            xColorScale = spec.xColorScale,
            xSaturationScale = spec.xSaturationScale,
            xDesaturatePenalty = spec.xDesaturatePenalty,
            yToneScale = spec.yToneScale,
            yBrightnessScale = spec.yBrightnessScale,
            yShadowBias = spec.yShadowBias,
            mappingBlendScale = spec.mappingBlendScale,
            mappingLightnessScale = spec.mappingLightnessScale,
            mappingChromaScale = spec.mappingChromaScale,
            defaultStrength = spec.defaultStrength,
            monochrome = spec.monochrome
        )
    }
}

@Composable
internal fun rememberColorCardCameraExpandedPreviewBitmaps(
    enabled: Boolean,
    candidates: List<ColorCardTraceFilterCandidate>
): Map<String, Bitmap> {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val styleCandidates = remember(candidates) {
        candidates.filter { it.paletteId.startsWith("camera_style:") }
    }
    val builtinCandidates = remember(candidates) {
        candidates.filterNot { it.paletteId.startsWith("camera_style:") }
    }
    val styleTemplatesByPaletteId = remember {
        buildColorCardCameraStyleTemplates().associateBy { it.candidate.paletteId }
    }
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
    val builtinPreviewRequests = remember(enabled, builtinCandidates, assignedFeedAssetNamesByPaletteId) {
        if (!enabled) {
            emptyList()
        } else {
            builtinCandidates.mapNotNull { candidate ->
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
    }
    val sharedBuiltinPreviewBitmaps = rememberColorCardFilterFeedPreviewBitmaps(builtinPreviewRequests)
    val reusedBuiltinPreviewBitmapsByPaletteId = remember(
        builtinPreviewRequests,
        sharedBuiltinPreviewBitmaps
    ) {
        buildMap {
            builtinPreviewRequests.forEach { request ->
                sharedBuiltinPreviewBitmaps[request.previewKey]
                    ?.takeUnless(Bitmap::isRecycled)
                    ?.let { bitmap ->
                        put(request.candidate.paletteId, bitmap)
                    }
            }
        }
    }
    val assignedFeedBitmaps = rememberColorCardFilterFeedDemoBitmaps(
        assetNames = if (enabled) {
            styleCandidates.mapNotNull { candidate ->
                assignedFeedAssetNamesByPaletteId[candidate.paletteId]
            }.toSet()
        } else {
            emptySet()
        }
    )
    val fallbackPreviewBitmap = rememberColorCardCameraSharedPreviewBitmap(enabled = enabled)
    val renderedPreviewSignature = remember(
        styleCandidates,
        assignedFeedAssetNamesByPaletteId,
        assignedFeedBitmaps,
        fallbackPreviewBitmap
    ) {
        buildString {
            styleCandidates.forEach { candidate ->
                val assetName = assignedFeedAssetNamesByPaletteId[candidate.paletteId].orEmpty()
                append(candidate.paletteId)
                append('@')
                append(assetName)
                append(':')
                append(if (assetName.isNotBlank() && assignedFeedBitmaps[assetName]?.isRecycled == false) '1' else '0')
                append('|')
            }
            append("fallback:")
            append(if (fallbackPreviewBitmap?.isRecycled == false) '1' else '0')
        }
    }
    val renderedPreviewBitmaps = remember(renderedPreviewSignature) {
        mutableStateMapOf<String, Bitmap>()
    }
    val haldBitmapCache = remember { mutableMapOf<String, Bitmap>() }

    LaunchedEffect(
        enabled,
        styleCandidates,
        appContext,
        styleTemplatesByPaletteId,
        assignedFeedAssetNamesByPaletteId,
        assignedFeedBitmaps,
        fallbackPreviewBitmap,
        renderedPreviewBitmaps
    ) {
        if (!enabled || styleCandidates.isEmpty()) return@LaunchedEffect
        val missingCandidates = styleCandidates.filter { candidate ->
            renderedPreviewBitmaps[candidate.paletteId]?.isRecycled != false
        }
        if (missingCandidates.isEmpty()) return@LaunchedEffect

        missingCandidates
            .chunked(COLOR_CARD_CAMERA_STYLE_PREVIEW_BATCH_SIZE)
            .forEach { candidateBatch ->
                val generated = buildMap<String, Bitmap> {
                    candidateBatch.forEach { candidate ->
                        val sourceBitmap = assignedFeedAssetNamesByPaletteId[candidate.paletteId]
                            ?.let { assetName ->
                                assignedFeedBitmaps[assetName]?.takeUnless { it.isRecycled }
                            }
                            ?: fallbackPreviewBitmap?.takeUnless { it.isRecycled }
                            ?: return@forEach
                        val renderedBitmap = runCatching {
                            renderColorCardCameraExpandedPreviewBitmap(
                                context = appContext,
                                candidate = candidate,
                                sourceBitmap = sourceBitmap,
                                styleTemplate = styleTemplatesByPaletteId[candidate.paletteId],
                                haldBitmapCache = haldBitmapCache
                            )
                        }.getOrNull()
                        val finalBitmap = renderedBitmap ?: runCatching {
                            renderColorCardTraceFeedApproxPreviewBitmap(
                                sourceBitmap = sourceBitmap,
                                candidate = candidate
                            )
                        }.getOrNull()
                        finalBitmap?.let { bitmap ->
                            put(candidate.paletteId, bitmap)
                        }
                    }
                }
                generated.forEach { (paletteId, bitmap) ->
                    renderedPreviewBitmaps[paletteId]
                        ?.takeIf { existing -> existing !== bitmap && !existing.isRecycled }
                        ?.recycle()
                    renderedPreviewBitmaps[paletteId] = bitmap
                }
                yield()
            }
    }

    DisposableEffect(haldBitmapCache) {
        onDispose {
            haldBitmapCache.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            haldBitmapCache.clear()
        }
    }

    DisposableEffect(renderedPreviewBitmaps) {
        onDispose {
            renderedPreviewBitmaps.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            renderedPreviewBitmaps.clear()
        }
    }

    return buildMap {
        putAll(reusedBuiltinPreviewBitmapsByPaletteId)
        putAll(renderedPreviewBitmaps)
    }
}

private suspend fun renderColorCardCameraExpandedPreviewBitmap(
    context: Context,
    candidate: ColorCardTraceFilterCandidate,
    sourceBitmap: Bitmap,
    styleTemplate: ColorCardCameraStyleTemplate?,
    haldBitmapCache: MutableMap<String, Bitmap>
): Bitmap? {
    if (sourceBitmap.isRecycled) return null
    if (candidate.paletteId.startsWith("camera_style:")) {
        val template = styleTemplate ?: return null
        val previewState = buildColorCardCameraStylePreviewState(template)
        return renderColorCardTraceGpuBitmap(
            sourceBitmap = sourceBitmap,
            params = previewState.buildPreviewParams(sourceBitmap)
        )
    }
    return renderColorCardCameraBuiltinPreviewBitmap(
        context = context,
        candidate = candidate,
        sourceBitmap = sourceBitmap,
        haldBitmapCache = haldBitmapCache
    )
}

private suspend fun renderColorCardCameraBuiltinPreviewBitmap(
    context: Context,
    candidate: ColorCardTraceFilterCandidate,
    sourceBitmap: Bitmap,
    haldBitmapCache: MutableMap<String, Bitmap>
): Bitmap? {
    if (candidate.kind != ColorCardTraceFilterKind.HaldClut) {
        return renderColorCardTraceFeedPreviewBitmap(
            context = context,
            sourceBitmap = sourceBitmap,
            candidate = candidate
        )
    }
    val safeWidth = sourceBitmap.width.coerceAtLeast(1)
    val safeHeight = sourceBitmap.height.coerceAtLeast(1)
    val previewStrength = defaultColorCardTracePaletteStrength(candidate)
    val resolvedBlend = resolveColorCardTraceHaldBlend(
        candidate = candidate,
        paletteStrength = previewStrength
    )
    val primaryHaldBitmap = loadColorCardCameraPreviewHaldBitmap(
        context = context,
        assetName = resolvedBlend.primaryAssetName,
        haldBitmapCache = haldBitmapCache
    )
    if (!resolvedBlend.primaryAssetName.isNullOrBlank() && primaryHaldBitmap == null) {
        return renderColorCardTraceFeedPreviewBitmap(
            context = context,
            sourceBitmap = sourceBitmap,
            candidate = candidate
        )
    }
    val secondaryHaldBitmap = loadColorCardCameraPreviewHaldBitmap(
        context = context,
        assetName = resolvedBlend.secondaryAssetName,
        haldBitmapCache = haldBitmapCache
    )
    val effectiveVariantMix = if (secondaryHaldBitmap == null) 0f else resolvedBlend.variantMix
    val previewParams = buildStandardFilterGpuParams(
        selectedCandidate = candidate,
        haldBitmap = primaryHaldBitmap,
        haldSecondaryBitmap = secondaryHaldBitmap,
        haldVariantMix = effectiveVariantMix,
        color = 0f,
        colorAxis = resolveColorCardTraceColorAxis(
            sourceColors = emptyList(),
            targetColors = candidate.colors
        ),
        sharpen = 0f,
        tone = 0f,
        saturation = 0f,
        brightness = 0f,
        strength = previewStrength,
        faceRegions = emptyList(),
        portraitParams = COLOR_CARD_TRACE_PORTRAIT_DISABLED,
        sourceAspect = safeWidth.toFloat() / safeHeight.toFloat(),
        sourceTexelWidth = 1f / safeWidth.toFloat(),
        sourceTexelHeight = 1f / safeHeight.toFloat()
    )
    return renderColorCardTraceGpuBitmap(
        sourceBitmap = sourceBitmap,
        params = previewParams
    )
}

private suspend fun loadColorCardCameraPreviewHaldBitmap(
    context: Context,
    assetName: String?,
    haldBitmapCache: MutableMap<String, Bitmap>
): Bitmap? {
    if (assetName.isNullOrBlank()) return null
    haldBitmapCache[assetName]
        ?.takeUnless { it.isRecycled }
        ?.let { return it }
    haldBitmapCache.remove(assetName)
    val decoded = decodeColorCardBuiltinHaldBitmap(
        context = context,
        assetName = assetName
    ) ?: return null
    haldBitmapCache[assetName] = decoded
    return decoded
}

@Composable
private fun rememberColorCardCameraSharedPreviewBitmap(
    enabled: Boolean
): Bitmap? {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(enabled, context) {
        if (!enabled) return@LaunchedEffect
        if (previewBitmap?.isRecycled == false) return@LaunchedEffect
        previewBitmap = decodeColorCardTraceFilterPreviewSampleBitmap(
            context = context.applicationContext,
            maxSide = COLOR_CARD_CAMERA_STYLE_PREVIEW_MAX_SIDE
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }
    return previewBitmap?.takeUnless(Bitmap::isRecycled)
}

private fun buildColorCardCameraFilterDisplayTitles(
    candidates: List<ColorCardTraceFilterCandidate>
): Map<String, String> {
    return candidates.mapIndexed { index, candidate ->
        candidate.paletteId to String.format(Locale.US, "#%03dA", index + 101)
    }.toMap()
}

private fun buildColorCardCameraStylePreviewState(
    template: ColorCardCameraStyleTemplate
): ColorCardCameraFilterUiState {
    return ColorCardCameraFilterUiState(
        activeSeries = ColorCardCameraFilterSeries.Style,
        candidates = listOf(template.candidate),
        browserCandidates = listOf(template.candidate),
        selectedCandidate = template.candidate,
        renderSelectedCandidate = template.candidate,
        selectedStyleTemplate = template,
        selectedIndex = 0,
        color = 0f,
        tone = 0f,
        sharpen = 0f,
        saturation = 0f,
        brightness = 0f,
        strength = template.defaultStrength,
        colorAxis = template.colorAxis,
        renderColorAxis = template.colorAxis,
        haldBitmap = null,
        renderHaldBitmap = null,
        haldSecondaryBitmap = null,
        renderHaldSecondaryBitmap = null,
        haldVariantMix = 0f,
        renderHaldVariantMix = 0f,
        filmGrainEligible = false,
        filmGrainEnabled = false,
        filmGrainAssets = emptyList(),
        selectedFilmGrainAsset = null,
        selectedFilmGrainBitmap = null,
        stepFilterBy = {},
        updateAdjustment = { _, _ -> },
        resetAdjustment = {},
        resetManualAdjustments = {},
        updateSharpen = {},
        updateSaturation = {},
        updateBrightness = {},
        updateStrength = {},
        selectCandidate = {},
        setSeries = {},
        setFilmGrainEnabled = {},
        selectFilmGrainAsset = {},
        refreshFilmGrainAsset = {},
        revealAnimationNonce = 0
    )
}

private fun buildStandardFilterGpuParams(
    selectedCandidate: ColorCardTraceFilterCandidate?,
    haldBitmap: Bitmap?,
    haldSecondaryBitmap: Bitmap?,
    haldVariantMix: Float,
    color: Float,
    colorAxis: ColorCardTraceColorAxis,
    sharpen: Float,
    tone: Float,
    saturation: Float,
    brightness: Float,
    strength: Float,
    faceRegions: List<ColorCardTraceFaceRegion>,
    portraitParams: ColorCardPortraitOptimizationParams,
    sourceAspect: Float,
    sourceTexelWidth: Float,
    sourceTexelHeight: Float
): ColorCardTraceGpuParams {
    val resolvedHaldBlend = resolveColorCardTraceHaldBlend(
        candidate = selectedCandidate,
        paletteStrength = strength
    )
    val safePrimaryHaldBitmap = haldBitmap?.takeUnless { it.isRecycled }
    val safeSecondaryHaldBitmap = haldSecondaryBitmap?.takeUnless { it.isRecycled }
    val primaryHaldEnabled = safePrimaryHaldBitmap != null && resolvedHaldBlend.primaryHaldLevel > 0
    val secondaryHaldEnabled =
        primaryHaldEnabled &&
            safeSecondaryHaldBitmap != null &&
            resolvedHaldBlend.secondaryHaldLevel > 0 &&
            haldVariantMix > 0.0001f
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
        haldEnabled = primaryHaldEnabled,
        haldLevel = if (primaryHaldEnabled) resolvedHaldBlend.primaryHaldLevel else 0,
        haldStrength = if (primaryHaldEnabled) strength.coerceIn(0f, 1f) else 0f,
        haldBitmap = safePrimaryHaldBitmap,
        haldSecondaryLevel = if (secondaryHaldEnabled) resolvedHaldBlend.secondaryHaldLevel else 0,
        haldSecondaryBitmap = if (secondaryHaldEnabled) safeSecondaryHaldBitmap else null,
        haldVariantMix = if (secondaryHaldEnabled) haldVariantMix.coerceIn(0f, 1f) else 0f,
        pixelPosterEnabled = false,
        pixelPosterUseSourceColor = false,
        pixelPosterContinuousTone = false,
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
        selectiveColorEnabled = false,
        selectiveColorTargetHue = 0f,
        selectiveColorHueRange = 0f,
        selectiveColorSaturationFloor = 0f,
        selectiveColorStrength = 0f,
        sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
        sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
        sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
    )
}

private fun buildColorCardCameraStyleGpuParams(
    template: ColorCardCameraStyleTemplate,
    color: Float,
    tone: Float,
    sharpen: Float,
    saturation: Float,
    brightness: Float,
    strength: Float,
    faceRegions: List<ColorCardTraceFaceRegion>,
    portraitParams: ColorCardPortraitOptimizationParams,
    sourceAspect: Float,
    sourceTexelWidth: Float,
    sourceTexelHeight: Float
): ColorCardTraceGpuParams {
    val x = (color.coerceIn(-1f, 1f) * COLOR_CARD_CAMERA_STYLE_X_RESPONSE_GAIN).coerceIn(-1f, 1f)
    val y = (tone.coerceIn(-1f, 1f) * COLOR_CARD_CAMERA_STYLE_Y_RESPONSE_GAIN).coerceIn(-1f, 1f)
    val styleStrength = resolveColorCardCameraStyleStrength(strength)
    val mapping = buildColorCardTraceGpuMapping(
        sourceColors = COLOR_CARD_CAMERA_STYLE_SOURCE_COLORS,
        targetColors = template.candidate.colors,
        paletteStrength = styleStrength,
        blendStrengthScale = template.mappingBlendScale * (1f + (max(0f, x) * 0.10f)),
        lightnessShiftScale = template.mappingLightnessScale * (1f + (max(0f, y) * 0.08f)),
        chromaShiftScale = template.mappingChromaScale * (
            1f +
                (max(0f, x) * 0.14f) +
                (max(0f, -x) * 0.04f)
            )
    )
    val brightnessTarget = (
        template.baseBrightness +
            (y * template.yBrightnessScale) +
            (max(0f, y) * 0.04f) +
            (brightness.coerceIn(-1f, 1f) * 0.28f)
        ).coerceIn(-1f, 1f)
    val toneTarget = (
        template.baseTone +
            (y * template.yToneScale) -
            (max(0f, -y) * template.yShadowBias)
        ).coerceIn(-1f, 1f)
    val saturationTarget = if (template.monochrome) {
        (-0.92f + (max(0f, x) * template.xSaturationScale)).coerceIn(-1f, -0.12f)
    } else {
        (
            template.baseSaturation +
                (x * template.xSaturationScale) -
                (max(0f, -x) * template.xDesaturatePenalty) +
                (saturation.coerceIn(-1f, 1f) * 0.26f)
            ).coerceIn(-1f, 1f)
    }
    val colorTarget = if (template.monochrome) {
        0f
    } else {
        (template.baseColor + (x * template.xColorScale)).coerceIn(-1f, 1f)
    }
    val sharpenTarget = (
        template.baseSharpen +
            (max(0f, x) * 0.04f) +
            (max(0f, y) * 0.03f) +
            (sharpen.coerceIn(-1f, 1f) * 0.22f)
        ).coerceIn(-1f, 1f)
    return ColorCardTraceGpuParams(
        filterEnabled = mapping != null,
        anchorCount = mapping?.anchorCount ?: 0,
        sourceAnchors = mapping?.sourceAnchors ?: FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
        targetAnchors = mapping?.targetAnchors ?: FloatArray(COLOR_CARD_TRACE_FILTER_MAX_ANCHORS * 3),
        blendStrength = mapping?.blendStrength ?: 0f,
        maxLightnessShift = mapping?.maxLightnessShift ?: 0f,
        maxChromaShift = mapping?.maxChromaShift ?: 0f,
        color = interpolateColorCardCameraStyleValue(colorTarget, styleStrength),
        colorAxisA = template.colorAxis.aWeight,
        colorAxisB = template.colorAxis.bWeight,
        sharpen = interpolateColorCardCameraStyleValue(sharpenTarget, styleStrength),
        tone = interpolateColorCardCameraStyleValue(toneTarget, styleStrength),
        saturation = interpolateColorCardCameraStyleValue(saturationTarget, styleStrength),
        brightness = interpolateColorCardCameraStyleValue(brightnessTarget, styleStrength),
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
        pixelPosterContinuousTone = false,
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
        selectiveColorEnabled = false,
        selectiveColorTargetHue = 0f,
        selectiveColorHueRange = 0f,
        selectiveColorSaturationFloor = 0f,
        selectiveColorStrength = 0f,
        sourceAspect = sourceAspect.coerceAtLeast(0.0001f),
        sourceTexelWidth = sourceTexelWidth.coerceAtLeast(0.0000001f),
        sourceTexelHeight = sourceTexelHeight.coerceAtLeast(0.0000001f)
    )
}

private fun interpolateColorCardCameraStyleValue(
    target: Float,
    strength: Float
): Float {
    return (target.coerceIn(-1f, 1f) * strength.coerceIn(0f, 1f)).coerceIn(-1f, 1f)
}

private fun resolveColorCardCameraStyleStrength(
    strength: Float
): Float {
    val clamped = strength.coerceIn(0f, 1f)
    return kotlin.math.sqrt(clamped)
        .coerceIn(0f, 1f)
}

internal suspend fun detectColorCardCameraFaceRegions(
    bitmap: Bitmap?
): List<ColorCardTraceFaceRegion> {
    if (bitmap == null || bitmap.isRecycled) return emptyList()
    return withContext(Dispatchers.Default) {
        ColorCardTraceSkinProtectionDetector.detectFaceRegions(bitmap)
    }
}

internal fun resolveColorCardCameraPortraitAdjustments(
    beautyEnabled: Boolean = false
): ColorCardTracePortraitAdjustments {
    if (!beautyEnabled) {
        return ColorCardTracePortraitAdjustments(
            vitality = 0f,
            softness = 0f,
            detailSuppression = 0f
        )
    }
    return ColorCardTracePortraitAdjustments(
        vitality = 0.26f,
        softness = 0.15f,
        detailSuppression = 0.11f
    )
}

internal suspend fun decodeBitmapFromFile(
    context: Context,
    uriString: String
): Bitmap? = withContext(Dispatchers.IO) {
    val uri = android.net.Uri.parse(uriString)
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input)?.copy(Bitmap.Config.ARGB_8888, false)
        }
    }.getOrNull()
}
