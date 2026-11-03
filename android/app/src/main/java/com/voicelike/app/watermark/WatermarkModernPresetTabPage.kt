package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val WATERMARK_MODERN_PREVIEW_CACHE_VERSION = 21
private const val WATERMARK_MODERN_PREVIEW_MAX_SIDE = 640
private const val WATERMARK_MODERN_PREVIEW_WIDTH = 300
private const val WATERMARK_MODERN_PREVIEW_HEIGHT = 400
private const val WATERMARK_MODERN_PREVIEW_DEFAULT_ASPECT_RATIO = 0.75f
private const val WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO = 0.8125f
private const val WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO = 0.875f
private const val WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO = 1f
private const val WATERMARK_MODERN_PREVIEW_SOFT_WIDE_ASPECT_RATIO = 1.12f
private const val WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO = 1.3333334f
private const val WATERMARK_MODERN_PREVIEW_EXTRA_WIDE_ASPECT_RATIO = 1.4444444f
private const val WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO = 1f
private const val WATERMARK_MODERN_STATIC_PREVIEW_ASSET_DIR = "watermark_modern_previews"
private val WATERMARK_MODERN_PREVIEW_DEFAULT_SIZE =
    WATERMARK_MODERN_PREVIEW_WIDTH to WATERMARK_MODERN_PREVIEW_HEIGHT
private val WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE = 520 to 640
private val WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE = 560 to 640
private val WATERMARK_MODERN_PREVIEW_SQUARE_SIZE = 420 to 420
private val WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE = 640 to 640
private val WATERMARK_MODERN_PREVIEW_SOFT_WIDE_SIZE = 448 to 400
private val WATERMARK_MODERN_PREVIEW_WIDE_SIZE = 480 to 360
private val WATERMARK_MODERN_PREVIEW_CHINESE_WIDE_SIZE = 640 to 444
private val WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE = 1280 to 640
private val WATERMARK_MODERN_PREVIEW_CACHE_LOCK = Any()
private val WATERMARK_MODERN_PREVIEW_BITMAP_CACHE = linkedMapOf<String, Bitmap>()

internal data class WatermarkModernPresetSpec(
    val key: String,
    val template: WatermarkTemplatePreset,
    val zhLabel: String,
    val enLabel: String,
    val previewAspectRatio: Float = WATERMARK_MODERN_PREVIEW_DEFAULT_ASPECT_RATIO,
    val previewOutputSize: Pair<Int, Int> = WATERMARK_MODERN_PREVIEW_DEFAULT_SIZE,
    val showLogoWatermark: Boolean = true,
    val showModelWatermark: Boolean = true,
    val showDateWatermark: Boolean = true,
    val showExifWatermark: Boolean = true,
    val showLocationWatermark: Boolean = false,
    val showCityLocationWatermark: Boolean = true,
    val fullColorUseGradientBackground: Boolean = false,
    val fullColorGradientPresetIndex: Int = -1,
    val frameSideTopBorderRatio: Float = 0.018f,
    val frameLayoutMode: WatermarkFrameLayoutMode = WatermarkFrameLayoutMode.Center,
    val logoScaleFactor: Float = 1f,
    val frameBackgroundColorIndex: Int = 8,
    val thinBorderColorIndex: Int = 8,
    val roundedBorderColorIndex: Int = 8,
    val borderCornerMode: BorderCornerMode = BorderCornerMode.Straight,
    val blurImageCornerRadiusRatio: Float = 0.055f,
    val borderImageScaleFactor: Float = 1f,
    val floatingBackgroundColorIndex: Int = 8,
    val floatingStrokeColorIndex: Int = 0,
    val floatingImageCornerRadiusRatio: Float = 0.010f,
    val filmFrameIndex: Int = 0,
    val polaroidFrameIndex: Int = 0,
    val colorPaletteStyleIndex: Int = 0,
    val colorPaletteVariantIndex: Int = 0,
    val colorPaletteHorizontalOffsetRatio: Float = 0f,
    val colorPaletteVerticalOffsetRatio: Float = 0f,
    val colorPaletteElementCount: Int = 6,
    val colorPaletteShowHexValue: Boolean = true,
    val colorPaletteShowRgbValue: Boolean = true,
    val paperDotsConfig: WatermarkPaperDotsConfig = WatermarkPaperDotsConfig(),
    val watermarkFontKey: String = "inter_variable",
    val signatureFontKey: String = "harshita",
    val signatureText: String = "",
    val signatureColor: Int? = null,
    val signatureHorizontalOffsetRatio: Float = 0f,
    val signatureVerticalOffsetRatio: Float = 0f,
    val signatureScaleFactor: Float = 1f,
    val cutoutOverlayStyle: WatermarkCutoutOverlayStyle = WatermarkCutoutOverlayStyle.None,
    val cutoutSubjectScaleFactor: Float = DEFAULT_WATERMARK_CUTOUT_SUBJECT_SCALE_FACTOR,
    val cutoutFrameAspectRatio: WatermarkCutoutFrameAspectRatio = DEFAULT_WATERMARK_CUTOUT_FRAME_ASPECT_RATIO,
    val subjectOutlineConfig: WatermarkSubjectOutlineConfig = WatermarkSubjectOutlineConfig()
) {
    fun label(language: AppLanguage, variantNumber: Int? = null): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhLabel
        AppLanguage.English -> enLabel
        else -> {
            val base = watermarkModernTemplateTitle(template, language)
            if (variantNumber != null) "$base $variantNumber" else base
        }
    }
}

internal data class WatermarkModernPresetSeries(
    val key: String,
    val zhTitle: String,
    val enTitle: String,
    val zhSubtitle: String,
    val enSubtitle: String,
    val presets: List<WatermarkModernPresetSpec>,
    val headlineEmoji: String? = null,
    val headlineZhLabel: String? = null,
    val headlineEnLabel: String? = null,
    val generateRenderedPreviews: Boolean = true
) {
    fun title(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhTitle
        AppLanguage.English -> enTitle
        else -> {
            val rootKey = key.substringBefore(":")
            val template = watermarkModernSeriesTemplate(rootKey)
            template?.let { watermarkModernTemplateTitle(it, language) } ?: enTitle
        }
    }

    fun subtitle(language: AppLanguage): String = when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> zhSubtitle
        AppLanguage.English -> enSubtitle
        else -> {
            val rootKey = key.substringBefore(":")
            val template = watermarkModernSeriesTemplate(rootKey)
            template?.let { watermarkModernTemplateSubtitle(it, language) } ?: enSubtitle
        }
    }
}

private fun watermarkModernSeriesTemplate(rootKey: String): WatermarkTemplatePreset? = when (rootKey) {
    "frame" -> WatermarkTemplatePreset.WatermarkFrame
    "direct" -> WatermarkTemplatePreset.DirectWatermark
    "full_color" -> WatermarkTemplatePreset.FullColor
    "full_color_gradient" -> WatermarkTemplatePreset.FullColor
    "border" -> WatermarkTemplatePreset.ThinWhiteBorder
    "floating" -> WatermarkTemplatePreset.Floating
    "blur" -> WatermarkTemplatePreset.BlurBackground
    "film" -> WatermarkTemplatePreset.FilmRetro
    "polaroid" -> WatermarkTemplatePreset.Polaroid
    "paper_dots" -> WatermarkTemplatePreset.PaperDots
    "color" -> WatermarkTemplatePreset.ColorPalette
    else -> null
}

private fun watermarkModernFullColorTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "半屏色彩"
    AppLanguage.TraditionalChinese -> "半屏色彩"
    AppLanguage.Japanese -> "ハーフカラー"
    AppLanguage.Korean -> "하프 컬러"
    AppLanguage.French -> "Demi-couleur"
    AppLanguage.German -> "Halbfarbe"
    AppLanguage.Italian -> "Mezzo colore"
    AppLanguage.Spanish -> "Medio color"
    AppLanguage.Portuguese -> "Meia cor"
    AppLanguage.Russian -> "Полуцвет"
    AppLanguage.Thai -> "สีครึ่งภาพ"
    AppLanguage.Vietnamese -> "Màu nửa khung"
    AppLanguage.Indonesian -> "Warna setengah"
    AppLanguage.Arabic -> "لون نصفي"
    AppLanguage.Ukrainian -> "Кольорова половина"
    AppLanguage.Polish -> "Pół w kolorze"
    AppLanguage.Czech -> "Barevná půlka"
    AppLanguage.Dutch -> "Kleurhelft"
    AppLanguage.Swedish -> "Färghalva"
    AppLanguage.Danish -> "Halv farve"
    AppLanguage.Norwegian -> "Fargehalvdel"
    AppLanguage.Finnish -> "Väripuolisko"
    AppLanguage.Greek -> "Μισό χρώμα"
    AppLanguage.Romanian -> "Jumătate color"
    AppLanguage.Hungarian -> "Félig színes"
    AppLanguage.Turkish -> "Yarım renk"
    AppLanguage.Malay -> "Separuh Warna"
    AppLanguage.Hebrew -> "חצי צבע"
    AppLanguage.English -> "Half Color"
}

private fun watermarkModernFullColorSubtitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "半屏色彩"
    AppLanguage.TraditionalChinese -> "半屏色彩"
    AppLanguage.Japanese -> "ハーフカラー"
    AppLanguage.Korean -> "하프 컬러"
    AppLanguage.French -> "Demi-couleur"
    AppLanguage.German -> "Halbfarbe"
    AppLanguage.Italian -> "Mezzo colore"
    AppLanguage.Spanish -> "Medio color"
    AppLanguage.Portuguese -> "Meia cor"
    AppLanguage.Russian -> "Полуцвет"
    AppLanguage.Thai -> "สีครึ่งภาพ"
    AppLanguage.Vietnamese -> "Màu nửa khung"
    AppLanguage.Indonesian -> "Warna setengah"
    AppLanguage.Arabic -> "لون نصفي"
    AppLanguage.Ukrainian -> "Кольорова половина"
    AppLanguage.Polish -> "Pół w kolorze"
    AppLanguage.Czech -> "Barevná půlka"
    AppLanguage.Dutch -> "Kleurhelft"
    AppLanguage.Swedish -> "Färghalva"
    AppLanguage.Danish -> "Halv farve"
    AppLanguage.Norwegian -> "Fargehalvdel"
    AppLanguage.Finnish -> "Väripuolisko"
    AppLanguage.Greek -> "Μισό χρώμα"
    AppLanguage.Romanian -> "Jumătate color"
    AppLanguage.Hungarian -> "Félig színes"
    AppLanguage.Turkish -> "Yarım renk"
    AppLanguage.Malay -> "Separuh Warna"
    AppLanguage.Hebrew -> "חצי צבע"
    AppLanguage.English -> "Half Color"
}

private fun watermarkModernTemplateTitle(
    template: WatermarkTemplatePreset,
    language: AppLanguage
): String {
    val extra = getWatermarkExtraCopy(language)
    return when (template) {
        WatermarkTemplatePreset.WatermarkFrame -> extra.templateWatermarkFrame
        WatermarkTemplatePreset.BlurBackground -> extra.templateBlurBackground
        WatermarkTemplatePreset.ThinWhiteBorder -> extra.templateThinWhiteBorder
        WatermarkTemplatePreset.Floating -> extra.templateFloating
        WatermarkTemplatePreset.Polaroid -> extra.templatePolaroid
        WatermarkTemplatePreset.FilmRetro -> extra.templateFilmRetro
        WatermarkTemplatePreset.PaperDots -> extra.templatePaperDots
        WatermarkTemplatePreset.DirectWatermark -> extra.templateDirectWatermark
        WatermarkTemplatePreset.FullColor -> watermarkModernFullColorTitle(language)
        WatermarkTemplatePreset.ColorPalette -> extra.templateColorPalette
    }
}

private fun watermarkModernTemplateSubtitle(
    template: WatermarkTemplatePreset,
    language: AppLanguage
): String {
    val extra = getWatermarkExtraCopy(language)
    return when (template) {
        WatermarkTemplatePreset.WatermarkFrame -> extra.templateWatermarkFrameDesc
        WatermarkTemplatePreset.BlurBackground -> extra.templateBlurBackgroundDesc
        WatermarkTemplatePreset.ThinWhiteBorder -> extra.templateThinWhiteBorderDesc
        WatermarkTemplatePreset.Floating -> extra.templateFloatingDesc
        WatermarkTemplatePreset.Polaroid -> extra.templatePolaroidDesc
        WatermarkTemplatePreset.FilmRetro -> extra.templateFilmRetroDesc
        WatermarkTemplatePreset.PaperDots -> extra.templatePaperDotsDesc
        WatermarkTemplatePreset.DirectWatermark -> extra.templateDirectWatermarkDesc
        WatermarkTemplatePreset.FullColor -> watermarkModernFullColorSubtitle(language)
        WatermarkTemplatePreset.ColorPalette -> extra.templateColorPaletteDesc
    }
}

internal data class WatermarkModernPresetTabState(
    val series: List<WatermarkModernPresetSeries>,
    val previewBitmaps: Map<String, Bitmap>,
    val isPreviewLoading: Boolean
)

private data class WatermarkModernPresetSessionState(
    val series: List<WatermarkModernPresetSeries>,
    val previewCopyByKey: Map<String, WatermarkModernPreviewCopy>
)

private data class WatermarkModernSeriesHeadlineStyle(
    val emoji: String,
    val zhLabel: String,
    val enLabel: String
)

private data class WatermarkModernPreviewCopy(
    val modelLine: String,
    val iso: String,
    val shutter: String,
    val focalLength: String,
    val aperture: String,
    val dateTimeOriginal: String,
    val cityLocation: String
) {
    val cacheKey: String
        get() = "$modelLine|$iso|$shutter|$focalLength|$aperture|$dateTimeOriginal|$cityLocation"

    fun exifInfo(): WatermarkExifInfo = WATERMARK_MODERN_PREVIEW_EXIF.copy(
        model = modelLine,
        iso = iso,
        shutter = shutter,
        focalLength = focalLength,
        aperture = aperture,
        dateTimeOriginal = dateTimeOriginal,
        cityLocation = cityLocation
    )
}

private data class WatermarkModernPreviewSpecLine(
    val iso: String,
    val shutter: String,
    val focalLength: String,
    val aperture: String
)

private data class WatermarkModernPresetRenderRequest(
    val sourceIndex: Int,
    val preset: WatermarkModernPresetSpec,
    val demoAsset: ColorCardFilterFeedDemoAsset,
    val previewCopy: WatermarkModernPreviewCopy,
    val cacheKey: String
)

private val WATERMARK_MODERN_PREVIEW_EXIF = WatermarkExifInfo(
    make = "Canon",
    model = "EOS 6D Mark II",
    iso = "100",
    shutter = "1/250",
    focalLength = "35",
    aperture = "2.8",
    dateTimeOriginal = "2026:01:19 15:51:05",
    latitude = 31.2304,
    longitude = 121.4737,
    cityLocation = "Shanghai, China"
)

private val WATERMARK_MODERN_PRESET_SESSION_STATE: WatermarkModernPresetSessionState by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
) {
    buildWatermarkModernPresetSessionState()
}

private val WATERMARK_MODERN_PREVIEW_MODEL_LINES = listOf(
    "EOS R5",
    "EOS R6 Mark II",
    "EOS R8",
    "EOS R7",
    "EOS R10",
    "EOS R50",
    "EOS RP",
    "EOS 5D Mark IV",
    "EOS 6D Mark II",
    "EOS 90D",
    "EOS M50",
    "PowerShot G7 X"
)

private val WATERMARK_MODERN_PREVIEW_SPEC_LINES = listOf(
    WatermarkModernPreviewSpecLine("100", "1/500", "35", "2.8"),
    WatermarkModernPreviewSpecLine("160", "1/320", "50", "1.8"),
    WatermarkModernPreviewSpecLine("200", "1/250", "24", "4.0"),
    WatermarkModernPreviewSpecLine("320", "1/200", "70", "2.8"),
    WatermarkModernPreviewSpecLine("400", "1/160", "85", "1.8"),
    WatermarkModernPreviewSpecLine("640", "1/125", "28", "5.6"),
    WatermarkModernPreviewSpecLine("800", "1/100", "105", "4.0"),
    WatermarkModernPreviewSpecLine("1250", "1/80", "40", "2.0"),
    WatermarkModernPreviewSpecLine("1600", "1/60", "35", "1.4"),
    WatermarkModernPreviewSpecLine("200", "1/800", "135", "2.8"),
    WatermarkModernPreviewSpecLine("100", "1/1000", "16", "8.0"),
    WatermarkModernPreviewSpecLine("500", "1/180", "55", "3.5")
)

private val WATERMARK_MODERN_PREVIEW_CITY_LINES = listOf(
    "Shanghai",
    "Hangzhou",
    "Suzhou",
    "Xiamen",
    "Chengdu",
    "Shenzhen",
    "Nanjing",
    "Qingdao",
    "Dali",
    "Tokyo",
    "Seoul",
    "Bangkok"
)

private val WATERMARK_MODERN_GENERIC_HEADLINE_EMOJIS = listOf(
    "✦",
    "◻️",
    "▫️",
    "◦"
)

private val WATERMARK_MODERN_SERIES_HEADLINE_STYLES = mapOf(
    "frame" to listOf(
        WatermarkModernSeriesHeadlineStyle("🪟", "水印边框", "Watermark Frames")
    ),
    "direct" to listOf(
        WatermarkModernSeriesHeadlineStyle("🪶", "极简水印", "Minimal Marks")
    ),
    "full_color" to listOf(
        WatermarkModernSeriesHeadlineStyle("🟦", "半屏色彩", "Half Color")
    ),
    "full_color_gradient" to listOf(
        WatermarkModernSeriesHeadlineStyle("🌈", "半屏渐变色彩", "Half Gradient Color")
    ),
    "border" to listOf(
        WatermarkModernSeriesHeadlineStyle("⬜", "留白边框", "Clean Borders")
    ),
    "floating" to listOf(
        WatermarkModernSeriesHeadlineStyle("🪶", "悬浮相纸", "Floating Paper")
    ),
    "blur" to listOf(
        WatermarkModernSeriesHeadlineStyle("🫧", "背景模糊", "Blur Backgrounds")
    ),
    "film" to listOf(
        WatermarkModernSeriesHeadlineStyle("📽️", "胶片复古", "Film Retro")
    ),
    "polaroid" to listOf(
        WatermarkModernSeriesHeadlineStyle("🗒️", "拍立得", "Polaroid")
    ),
    "paper_dots" to listOf(
        WatermarkModernSeriesHeadlineStyle("🔘", "波点纸孔", "Paper Dots")
    ),
    "color" to listOf(
        WatermarkModernSeriesHeadlineStyle("🧩", "色彩拼贴", "Color Layouts")
    )
)

@Composable
internal fun WatermarkModernPresetTabPage(
    onPresetSelected: (String) -> Unit,
    onScrollActivityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val state = rememberWatermarkModernPresetTabState()
    val primaryTextColor = MaterialTheme.colorScheme.onSurface
    val listState = rememberLazyListState()

    LaunchedEffect(listState, onScrollActivityChanged) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { onScrollActivityChanged(it) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentPadding = PaddingValues(top = 52.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = watermarkModernHeaderTitle(language),
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Normal,
                            color = primaryTextColor
                        )
                    }
                }
            }
        }

        items(state.series, key = { it.key }) { series ->
            WatermarkModernPresetSeriesRow(
                series = series,
                language = language,
                previewBitmaps = state.previewBitmaps,
                onPresetSelected = onPresetSelected
            )
        }
    }
}

@Composable
internal fun rememberWatermarkModernPresetTabState(): WatermarkModernPresetTabState {
    val context = LocalContext.current
    val demoAssets = rememberColorCardFilterFeedDemoAssets()
    val sessionState = remember { WATERMARK_MODERN_PRESET_SESSION_STATE }
    val series = sessionState.series
    val previewEnabledPresets = remember(series) {
        series
            .filter { it.generateRenderedPreviews }
            .flatMap { it.presets }
    }
    var previewBitmaps by remember { mutableStateOf(emptyMap<String, Bitmap>()) }
    var isPreviewLoading by remember { mutableStateOf(false) }

    LaunchedEffect(context, series) {
        val staticPreviewKeys = series
            .filterNot { it.generateRenderedPreviews }
            .flatMap { it.presets }
            .map { it.key }
        if (staticPreviewKeys.isEmpty()) return@LaunchedEffect
        val appContext = context.applicationContext
        val nextStaticMap = linkedMapOf<String, Bitmap>()
        staticPreviewKeys.forEach { presetKey ->
            loadWatermarkModernStaticPreviewBitmap(
                context = appContext,
                presetKey = presetKey
            )?.let { bitmap ->
                nextStaticMap[presetKey] = bitmap
            }
        }
        if (nextStaticMap.isNotEmpty()) {
            previewBitmaps = previewBitmaps + nextStaticMap
        }
    }

    LaunchedEffect(context, demoAssets, sessionState) {
        if (demoAssets.isEmpty()) return@LaunchedEffect
        val allPresets = previewEnabledPresets
        if (allPresets.isEmpty()) return@LaunchedEffect
        val appContext = context.applicationContext
        val staticPreviewMap = previewBitmaps.filterKeys { key ->
            series.any { watermarkSeries ->
                !watermarkSeries.generateRenderedPreviews &&
                    watermarkSeries.presets.any { preset -> preset.key == key }
            }
        }
        val nextMap = linkedMapOf<String, Bitmap>().apply {
            allPresets.forEachIndexed { index, preset ->
                val demoAsset = demoAssets[index % demoAssets.size]
                val previewCopy = sessionState.previewCopyByKey[preset.key]
                    ?: buildWatermarkModernPreviewCopy(index)
                val cacheKey = watermarkModernPreviewCacheKey(
                    preset = preset,
                    demoAsset = demoAsset,
                    previewCopy = previewCopy
                )
                getCachedWatermarkModernPreviewBitmap(cacheKey)?.let { cachedBitmap ->
                    put(preset.key, cachedBitmap)
                }
            }
        }
        if (nextMap.isNotEmpty()) {
            previewBitmaps = staticPreviewMap + nextMap
        }

        val missingPresets = allPresets.mapIndexedNotNull { index, preset ->
            val demoAsset = demoAssets[index % demoAssets.size]
            val previewCopy = sessionState.previewCopyByKey[preset.key]
                ?: buildWatermarkModernPreviewCopy(index)
            val cacheKey = watermarkModernPreviewCacheKey(
                preset = preset,
                demoAsset = demoAsset,
                previewCopy = previewCopy
            )
            if (nextMap[preset.key] == null) {
                WatermarkModernPresetRenderRequest(
                    sourceIndex = index,
                    preset = preset,
                    demoAsset = demoAsset,
                    previewCopy = previewCopy,
                    cacheKey = cacheKey
                )
            } else {
                null
            }
        }
        if (missingPresets.isEmpty()) {
            isPreviewLoading = false
            return@LaunchedEffect
        }

        isPreviewLoading = true
        try {
            missingPresets.forEach { request ->
                renderWatermarkModernPresetPreview(
                    context = appContext,
                    preset = request.preset,
                    demoAsset = request.demoAsset,
                    previewCopy = request.previewCopy
                )?.let { previewBitmap ->
                    cacheWatermarkModernPreviewBitmap(request.cacheKey, previewBitmap)
                    nextMap[request.preset.key] = previewBitmap
                    previewBitmaps = staticPreviewMap + nextMap
                }
                if (request.sourceIndex % 2 == 1) {
                    delay(8L)
                }
            }
        } finally {
            isPreviewLoading = false
        }
    }

    return WatermarkModernPresetTabState(
        series = series,
        previewBitmaps = previewBitmaps,
        isPreviewLoading = isPreviewLoading
    )
}

@Composable
internal fun WatermarkModernPresetSeriesRow(
    series: WatermarkModernPresetSeries,
    language: AppLanguage,
    previewBitmaps: Map<String, Bitmap>,
    onPresetSelected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = watermarkModernSeriesHeadline(series, language),
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(series.presets, key = { _, preset -> preset.key }) { index, preset ->
                WatermarkModernPresetCard(
                    preset = preset,
                    language = language,
                    ordinalInSeries = index + 1,
                    previewBitmap = previewBitmaps[preset.key],
                    onClick = { onPresetSelected(preset.key) }
                )
            }
        }
    }
}

private fun loadWatermarkModernStaticPreviewBitmap(
    context: Context,
    presetKey: String
): Bitmap? {
    val assetName = "$WATERMARK_MODERN_STATIC_PREVIEW_ASSET_DIR/$presetKey.webp"
    return runCatching {
        context.assets.open(assetName).use { input ->
            BitmapFactory.decodeStream(input)
        }
    }.getOrNull()?.takeUnless { it.isRecycled || it.width <= 0 || it.height <= 0 }
}

private fun watermarkModernHeaderTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese,
    AppLanguage.TraditionalChinese -> "\u6c34\u5370\u8fb9\u6846"
    AppLanguage.English -> "Watermark Frame"
    else -> watermarkModernTemplateTitle(WatermarkTemplatePreset.WatermarkFrame, language)
}

internal fun watermarkModernSeriesHeadline(
    series: WatermarkModernPresetSeries,
    language: AppLanguage
): String {
    val emoji = series.headlineEmoji ?: watermarkModernSeriesEmoji(series.key)
    val title = when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> {
            series.headlineZhLabel ?: defaultWatermarkModernSeriesHeadlineLabel(series, language)
        }
        AppLanguage.English -> {
            series.headlineEnLabel ?: defaultWatermarkModernSeriesHeadlineLabel(series, language)
        }
        else -> defaultWatermarkModernSeriesHeadlineLabel(series, language)
    }
    return "$emoji $title"
}

private fun defaultWatermarkModernSeriesHeadlineLabel(
    series: WatermarkModernPresetSeries,
    language: AppLanguage
): String {
    if (language !in setOf(AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese)) {
        return series.subtitle(language)
    }
    return when (series.key.substringBefore(":")) {
        "frame" -> "\u65f6\u95f4\u5730\u70b9\u5728\u8fb9\u7f18"
        "direct" -> "\u53ea\u7559\u5173\u952e\u4fe1\u606f"
        "border" -> "\u7559\u767d\u66f4\u5e72\u51c0"
        "floating" -> "\u8f7b\u8f7b\u6d6e\u8d77\u6765"
        "blur" -> "\u8ba9\u4e3b\u4f53\u66f4\u5b89\u9759"
        "film" -> "\u65e7\u9897\u7c92\u65b0\u60c5\u7eea"
        "polaroid" -> "\u76f8\u7eb8\u5373\u65f6\u611f"
        "paper_dots" -> "\u4f1a\u547c\u5438\u7684\u7eb8\u5b54"
        "color" -> "\u4ece\u7167\u7247\u53d6\u8272"
        "subject_outline" -> "\u81ea\u52a8\u56f4\u4f4f\u4e3b\u4f53"
        "subject_outline_art" -> "\u8721\u7b14\u7c89\u7b14\u624b\u7a3f\u611f"
        else -> series.subtitle(language)
    }
}

private fun watermarkModernSeriesEmoji(seriesKey: String): String = when (seriesKey.substringBefore(":")) {
    "frame" -> "\uD83D\uDCF7"
    "direct" -> "\u2728"
    "full_color" -> "\uD83C\uDFA8"
    "full_color_gradient" -> "\uD83C\uDF08"
    "border" -> "\uD83D\uDDBC\uFE0F"
    "floating" -> "\u2601\uFE0F"
    "blur" -> "\uD83C\uDF2B\uFE0F"
    "film" -> "\uD83C\uDF9E\uFE0F"
    "polaroid" -> "\uD83D\uDCC4"
    "paper_dots" -> "\u26AA"
    "color" -> "\uD83C\uDFA8"
    "subject_outline" -> "\uD83D\uDD8D\uFE0F"
    "subject_outline_art" -> "\uD83D\uDD8D\uFE0F"
    else -> "\u2728"
}
@Composable
internal fun WatermarkModernPresetCard(
    preset: WatermarkModernPresetSpec,
    language: AppLanguage,
    ordinalInSeries: Int = 1,
    previewBitmap: Bitmap?,
    onClick: () -> Unit
) {
    val previewAspectRatio = previewBitmap
        ?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        ?.let { bitmap -> bitmap.width.toFloat() / bitmap.height.toFloat() }
        ?: if (preset.previewAspectRatio > 0f) {
            preset.previewAspectRatio
        } else {
            WATERMARK_MODERN_PREVIEW_DEFAULT_ASPECT_RATIO
        }
    val previewHeight = 148.dp
    val cardWidth = (previewHeight * previewAspectRatio.coerceIn(0.42f, 1.9f)).coerceAtLeast(72.dp)
    val cardShape = RoundedCornerShape(12.dp)
    val cardInteractionSource = remember { MutableInteractionSource() }
    val previewInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = cardShape
                )
                .clickable(
                    interactionSource = previewInteractionSource,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null && !previewBitmap.isRecycled) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = preset.label(language, ordinalInSeries),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

private suspend fun renderWatermarkModernPresetPreview(
    context: Context,
    preset: WatermarkModernPresetSpec,
    demoAsset: ColorCardFilterFeedDemoAsset,
    previewCopy: WatermarkModernPreviewCopy
): Bitmap? {
    return runCatching {
        val outputSizeOverride = preset.previewOutputSize
            .takeIf { it.first > 0 && it.second > 0 }
            ?: WATERMARK_MODERN_PREVIEW_DEFAULT_SIZE
        val sourceBitmap = decodeColorCardFilterFeedDemoBitmap(
            context = context,
            assetName = demoAsset.assetName,
            maxSidePx = WATERMARK_MODERN_PREVIEW_MAX_SIDE
        ) ?: return null
        renderWatermarkPreviewBitmap(
            context = context,
            sourceUri = null,
            sourceBitmapOverride = sourceBitmap,
            preserveSourceBitmapOverride = true,
            sourceIdentity = "modern-watermark:${preset.key}:${demoAsset.id}",
            template = preset.template,
            selectedBrandKey = "canon",
            customLogoPath = null,
            selectedVariantIndex = 1,
            showLogoWatermark = preset.showLogoWatermark,
            showModelWatermark = preset.showModelWatermark,
            showDateWatermark = preset.showDateWatermark,
            showExifWatermark = preset.showExifWatermark,
            showLocationWatermark = preset.showLocationWatermark,
            showCityLocationWatermark = preset.showCityLocationWatermark,
            modelLineOverride = previewCopy.modelLine,
            frameSideTopBorderRatio = preset.frameSideTopBorderRatio,
            frameLayoutMode = preset.frameLayoutMode,
            logoScaleFactor = preset.logoScaleFactor,
            frameBackgroundColorIndex = preset.frameBackgroundColorIndex,
            thinBorderColorIndex = preset.thinBorderColorIndex,
            roundedBorderColorIndex = preset.roundedBorderColorIndex,
            borderCornerMode = BorderCornerMode.Straight,
            blurImageCornerRadiusRatio = 0f,
            borderImageScaleFactor = preset.borderImageScaleFactor,
            cutoutOverlayStyle = preset.cutoutOverlayStyle,
            cutoutSubjectScaleFactor = preset.cutoutSubjectScaleFactor,
            cutoutFrameAspectRatio = preset.cutoutFrameAspectRatio,
            subjectOutlineConfig = preset.subjectOutlineConfig,
            floatingBackgroundColorIndex = preset.floatingBackgroundColorIndex,
            floatingStrokeColorIndex = preset.floatingStrokeColorIndex,
            floatingImageCornerRadiusRatio = 0f,
            filmFrameIndex = preset.filmFrameIndex,
            polaroidFrameIndex = preset.polaroidFrameIndex,
            colorPaletteStyleIndex = preset.colorPaletteStyleIndex,
            colorPaletteVariantIndex = preset.colorPaletteVariantIndex,
            colorPaletteHorizontalOffsetRatio = preset.colorPaletteHorizontalOffsetRatio,
            colorPaletteVerticalOffsetRatio = preset.colorPaletteVerticalOffsetRatio,
            colorPaletteElementCount = preset.colorPaletteElementCount,
            colorPaletteShowHexValue = preset.colorPaletteShowHexValue,
            colorPaletteShowRgbValue = preset.colorPaletteShowRgbValue,
            paperDotsConfig = preset.paperDotsConfig,
            watermarkFontKey = preset.watermarkFontKey,
            signatureFontKey = preset.signatureFontKey,
            signatureText = preset.signatureText,
            signatureColor = preset.signatureColor,
            signatureHorizontalOffsetRatio = preset.signatureHorizontalOffsetRatio,
            signatureVerticalOffsetRatio = preset.signatureVerticalOffsetRatio,
            signatureScaleFactor = preset.signatureScaleFactor,
            fullColorGradientColorsOverride = resolveFullColorGradientColorsOverride(preset.fullColorGradientPresetIndex),
            fullColorUseGradientBackground = preset.fullColorUseGradientBackground,
            outputSizeOverride = outputSizeOverride,
            exifInfoOverride = previewCopy.exifInfo(),
            previewMaxSidePx = WATERMARK_MODERN_PREVIEW_MAX_SIDE,
            suppressFrameSidePaddingWhenFixedOutput = true,
            enableColorPaletteCache = true
        )
    }.getOrNull()
}

private fun buildWatermarkModernPresetSessionState(): WatermarkModernPresetSessionState {
    val series = organizeWatermarkModernPresetSeries(buildWatermarkModernPresetSeries())
    val previewCopyByKey = series
        .flatMap { it.presets }
        .mapIndexed { index, preset -> preset.key to buildWatermarkModernPreviewCopy(index) }
        .toMap()
    return WatermarkModernPresetSessionState(
        series = series,
        previewCopyByKey = previewCopyByKey
    )
}

private fun buildWatermarkModernPreviewCopy(index: Int): WatermarkModernPreviewCopy {
    val modelLine = WATERMARK_MODERN_PREVIEW_MODEL_LINES[
        (index * 5) % WATERMARK_MODERN_PREVIEW_MODEL_LINES.size
    ]
    val specLine = WATERMARK_MODERN_PREVIEW_SPEC_LINES[
        (index * 7) % WATERMARK_MODERN_PREVIEW_SPEC_LINES.size
    ]
    val cityLine = WATERMARK_MODERN_PREVIEW_CITY_LINES[
        (index * 11) % WATERMARK_MODERN_PREVIEW_CITY_LINES.size
    ]
    val day = ((index * 3) % 28) + 1
    val hour = 8 + ((index * 5) % 12)
    val minute = (index * 13 + 7) % 60
    val second = (index * 17 + 11) % 60
    return WatermarkModernPreviewCopy(
        modelLine = modelLine,
        iso = specLine.iso,
        shutter = specLine.shutter,
        focalLength = specLine.focalLength,
        aperture = specLine.aperture,
        dateTimeOriginal = "2026:02:${day.toString().padStart(2, '0')} " +
            "${hour.toString().padStart(2, '0')}:" +
            "${minute.toString().padStart(2, '0')}:" +
            second.toString().padStart(2, '0'),
        cityLocation = cityLine
    )
}

internal fun resolveWatermarkModernPresetSpec(key: String?): WatermarkModernPresetSpec? {
    if (key == null) return null
    return buildWatermarkModernPresetSeries()
        .asSequence()
        .flatMap { it.presets.asSequence() }
        .firstOrNull { it.key == key }
}

private fun organizeWatermarkModernPresetSeries(
    source: List<WatermarkModernPresetSeries>
): List<WatermarkModernPresetSeries> {
    val primary = mutableListOf<WatermarkModernPresetSeries>()
    val secondary = mutableListOf<WatermarkModernPresetSeries>()
    source.shuffled().forEach { rawSeries ->
        val series = rawSeries.copy(presets = rawSeries.presets.shuffled())
        if (series.presets.size < 8) {
            primary += series
        } else {
            val firstCount = ((series.presets.size + 1) / 2).coerceAtLeast(4)
            primary += series.copy(
                key = "${series.key}:base",
                zhSubtitle = series.zhSubtitle,
                enSubtitle = series.enSubtitle,
                presets = series.presets.take(firstCount)
            )
            secondary += series.copy(
                key = "${series.key}:variation",
                zhSubtitle = series.zhSubtitle,
                enSubtitle = series.enSubtitle,
                presets = series.presets.drop(firstCount)
            )
        }
    }
    return assignWatermarkModernSeriesHeadlines(primary + secondary)
}

private fun assignWatermarkModernSeriesHeadlines(
    source: List<WatermarkModernPresetSeries>
): List<WatermarkModernPresetSeries> {
    val occurrenceByRootKey = mutableMapOf<String, Int>()
    return source.map { series ->
        val rootKey = series.key.substringBefore(":")
        val variantIndex = occurrenceByRootKey.getOrDefault(rootKey, 0)
        occurrenceByRootKey[rootKey] = variantIndex + 1
        if (variantIndex == 0) return@map series
        val headlineStyle = resolveWatermarkModernSeriesHeadlineStyle(rootKey, variantIndex, series)
        series.copy(
            headlineEmoji = headlineStyle.emoji,
            headlineZhLabel = headlineStyle.zhLabel,
            headlineEnLabel = headlineStyle.enLabel
        )
    }
}

private fun resolveWatermarkModernSeriesHeadlineStyle(
    rootKey: String,
    variantIndex: Int,
    series: WatermarkModernPresetSeries
): WatermarkModernSeriesHeadlineStyle {
    val styles = WATERMARK_MODERN_SERIES_HEADLINE_STYLES[rootKey]
    if (!styles.isNullOrEmpty()) {
        val styleIndex = (variantIndex - 1).coerceAtLeast(0)
        styles.getOrNull(styleIndex)?.let { return it }
        val fallbackStyle = styles[styleIndex % styles.size]
        return fallbackStyle.copy(
            emoji = WATERMARK_MODERN_GENERIC_HEADLINE_EMOJIS[
                styleIndex % WATERMARK_MODERN_GENERIC_HEADLINE_EMOJIS.size
            ],
            zhLabel = appendWatermarkModernZhHeadlineOrdinal(
                fallbackStyle.zhLabel,
                styleIndex + 2
            ),
            enLabel = "${fallbackStyle.enLabel} ${styleIndex + 2}"
        )
    }
    return WatermarkModernSeriesHeadlineStyle(
        emoji = WATERMARK_MODERN_GENERIC_HEADLINE_EMOJIS[
            (variantIndex - 1).coerceAtLeast(0) % WATERMARK_MODERN_GENERIC_HEADLINE_EMOJIS.size
        ],
        zhLabel = appendWatermarkModernZhHeadlineOrdinal(
            defaultWatermarkModernSeriesHeadlineLabel(series, AppLanguage.SimplifiedChinese),
            variantIndex + 1
        ),
        enLabel = "${defaultWatermarkModernSeriesHeadlineLabel(series, AppLanguage.English)} ${variantIndex + 1}"
    )
}

private fun appendWatermarkModernZhHeadlineOrdinal(base: String, ordinal: Int): String {
    val suffix = when (ordinal) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        else -> ordinal.toString()
    }
    return if (base.length >= 5) {
        base.take(4) + suffix
    } else {
        base + suffix
    }
}

private fun buildWatermarkModernPresetSeries(): List<WatermarkModernPresetSeries> = listOf(
    WatermarkModernPresetSeries(
        key = "frame",
        zhTitle = "水印边框",
        enTitle = "Watermark Frames",
        zhSubtitle = "时间地点在边缘",
        enSubtitle = "Time and place on the edge",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "frame_center_white",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "经典居中",
                enLabel = "Classic Center",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE
            ),
            WatermarkModernPresetSpec(
                key = "frame_left_white",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "\u5de6\u5bf9\u9f50\u4fe1\u606f",
                enLabel = "Left Details",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                frameLayoutMode = WatermarkFrameLayoutMode.Left
            ),
            WatermarkModernPresetSpec(
                key = "frame_right_white",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "\u53f3\u5bf9\u9f50\u4fe1\u606f",
                enLabel = "Right Details",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                frameLayoutMode = WatermarkFrameLayoutMode.Right
            ),
            WatermarkModernPresetSpec(
                key = "frame_black_center",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "黑色底栏",
                enLabel = "Black Plate",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                frameBackgroundColorIndex = 7
            ),
            WatermarkModernPresetSpec(
                key = "frame_soft_blue",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "雾蓝底栏",
                enLabel = "Soft Blue",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                frameBackgroundColorIndex = 5,
                frameSideTopBorderRatio = 0.024f
            ),
            WatermarkModernPresetSpec(
                key = "frame_black_left",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "\u9ed1\u5e95\u5de6\u680f",
                enLabel = "Black Left",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                frameLayoutMode = WatermarkFrameLayoutMode.Left,
                frameBackgroundColorIndex = 7,
                logoScaleFactor = 0.92f
            ),
            WatermarkModernPresetSpec(
                key = "frame_warm_right",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "\u6696\u767d\u53f3\u680f",
                enLabel = "Warm Right",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                frameLayoutMode = WatermarkFrameLayoutMode.Right,
                frameBackgroundColorIndex = 1,
                frameSideTopBorderRatio = 0.021f
            ),
            WatermarkModernPresetSpec(
                key = "frame_logo_date",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "Logo 与日期",
                enLabel = "Logo and Date",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                showModelWatermark = false,
                showExifWatermark = false,
                showCityLocationWatermark = false,
                frameSideTopBorderRatio = 0.020f
            ),
            WatermarkModernPresetSpec(
                key = "frame_place_date",
                template = WatermarkTemplatePreset.WatermarkFrame,
                zhLabel = "地点与日期",
                enLabel = "Place and Date",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = true,
                frameLayoutMode = WatermarkFrameLayoutMode.Left
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "direct",
        zhTitle = "极简水印",
        enTitle = "Minimal Marks",
        zhSubtitle = "\u53ea\u7559\u5173\u952e\u4fe1\u606f",
        enSubtitle = "Only key details",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "direct_full",
                template = WatermarkTemplatePreset.DirectWatermark,
                zhLabel = "\u673a\u578b\u4e0e\u53c2\u6570",
                enLabel = "Model and EXIF"
            ),
            WatermarkModernPresetSpec(
                key = "direct_logo_date",
                template = WatermarkTemplatePreset.DirectWatermark,
                zhLabel = "Logo \u4e0e\u65e5\u671f",
                enLabel = "Logo and Date",
                showModelWatermark = false,
                showExifWatermark = false
            ),
            WatermarkModernPresetSpec(
                key = "direct_specs_only",
                template = WatermarkTemplatePreset.DirectWatermark,
                zhLabel = "参数短句",
                enLabel = "Specs Only",
                showLogoWatermark = false,
                showModelWatermark = false,
                showDateWatermark = false,
                showCityLocationWatermark = false
            ),
            WatermarkModernPresetSpec(
                key = "direct_place_date",
                template = WatermarkTemplatePreset.DirectWatermark,
                zhLabel = "\u5730\u70b9\u4e0e\u65e5\u671f",
                enLabel = "Place and Date",
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = true,
                showCityLocationWatermark = true,
                watermarkFontKey = "system"
            ),
            WatermarkModernPresetSpec(
                key = "direct_logo_only",
                template = WatermarkTemplatePreset.DirectWatermark,
                zhLabel = "仅 Logo",
                enLabel = "Logo Only",
                showModelWatermark = false,
                showDateWatermark = false,
                showExifWatermark = false,
                showCityLocationWatermark = false,
                logoScaleFactor = 1.12f
            ),
            WatermarkModernPresetSpec(
                key = "direct_date_only",
                template = WatermarkTemplatePreset.DirectWatermark,
                zhLabel = "仅日期",
                enLabel = "Date Only",
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showCityLocationWatermark = false,
                watermarkFontKey = "system"
            ),
            WatermarkModernPresetSpec(
                key = "direct_model_date",
                template = WatermarkTemplatePreset.DirectWatermark,
                zhLabel = "机型与日期",
                enLabel = "Model and Date",
                showLogoWatermark = false,
                showExifWatermark = false,
                showCityLocationWatermark = false
            ),
            WatermarkModernPresetSpec(
                key = "direct_logo_place",
                template = WatermarkTemplatePreset.DirectWatermark,
                zhLabel = "Logo 与地点",
                enLabel = "Logo and Place",
                showModelWatermark = false,
                showDateWatermark = false,
                showExifWatermark = false,
                showCityLocationWatermark = true,
                logoScaleFactor = 0.92f
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "full_color",
        zhTitle = "半屏色彩",
        enTitle = "Half Color",
        zhSubtitle = "半屏色彩",
        enSubtitle = "Half Color",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "full_color_balanced",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "时间与地点",
                enLabel = "Time and Place",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = true,
                watermarkFontKey = "inter_variable"
            ),
            WatermarkModernPresetSpec(
                key = "full_color_clockwise",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "轻字时刻",
                enLabel = "Light Clock",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = true,
                watermarkFontKey = "clockwise"
            ),
            WatermarkModernPresetSpec(
                key = "full_color_time_only",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "只留时间",
                enLabel = "Time Only",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = false,
                watermarkFontKey = "inter_variable"
            ),
            WatermarkModernPresetSpec(
                key = "full_color_place_only",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "只留地点",
                enLabel = "Place Only",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showDateWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = true,
                watermarkFontKey = "clockwise"
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "full_color_gradient",
        zhTitle = "半屏渐变色彩",
        enTitle = "Half Gradient Color",
        zhSubtitle = "半屏渐变色彩",
        enSubtitle = "Half Gradient Color",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "full_color_gradient_balanced",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "渐变时间与地点",
                enLabel = "Gradient Time and Place",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = true,
                fullColorUseGradientBackground = true,
                fullColorGradientPresetIndex = 0,
                watermarkFontKey = "inter_variable"
            ),
            WatermarkModernPresetSpec(
                key = "full_color_gradient_clockwise",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "渐变轻字时刻",
                enLabel = "Gradient Light Clock",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = true,
                fullColorUseGradientBackground = true,
                fullColorGradientPresetIndex = 1,
                watermarkFontKey = "clockwise"
            ),
            WatermarkModernPresetSpec(
                key = "full_color_gradient_time_only",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "渐变只留时间",
                enLabel = "Gradient Time Only",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = false,
                fullColorUseGradientBackground = true,
                fullColorGradientPresetIndex = 2,
                watermarkFontKey = "inter_variable"
            ),
            WatermarkModernPresetSpec(
                key = "full_color_gradient_place_only",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "渐变只留地点",
                enLabel = "Gradient Place Only",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showDateWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = true,
                fullColorUseGradientBackground = true,
                fullColorGradientPresetIndex = 3,
                watermarkFontKey = "inter_variable"
            ),
            WatermarkModernPresetSpec(
                key = "full_color_gradient_clock_only",
                template = WatermarkTemplatePreset.FullColor,
                zhLabel = "渐变大字时间",
                enLabel = "Gradient Bold Time",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = false,
                fullColorUseGradientBackground = true,
                fullColorGradientPresetIndex = 4,
                watermarkFontKey = "clockwise"
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "border",
        zhTitle = "留白边框",
        enTitle = "Clean Borders",
        zhSubtitle = "\u7559\u767d\u66f4\u5e72\u51c0",
        enSubtitle = "Cleaner whitespace",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "border_white_straight",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "白色直角",
                enLabel = "White Edge"
            ),
            WatermarkModernPresetSpec(
                key = "border_white_round",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "\u5185\u5706\u89d2",
                enLabel = "Inner Round",
                borderCornerMode = BorderCornerMode.InnerRounded,
                borderImageScaleFactor = 0.96f
            ),
            WatermarkModernPresetSpec(
                key = "border_black",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "黑色细边",
                enLabel = "Black Edge",
                thinBorderColorIndex = 7
            ),
            WatermarkModernPresetSpec(
                key = "border_green",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "苔绿细边",
                enLabel = "Moss Edge",
                thinBorderColorIndex = 3,
                borderImageScaleFactor = 0.94f
            ),
            WatermarkModernPresetSpec(
                key = "border_wide_white",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "\u6a2a\u56fe\u767d\u8fb9",
                enLabel = "Wide White",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                borderImageScaleFactor = 0.95f
            ),
            WatermarkModernPresetSpec(
                key = "border_round_black",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "\u9ed1\u5e95\u5706\u89d2",
                enLabel = "Black Round",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE,
                thinBorderColorIndex = 7,
                borderCornerMode = BorderCornerMode.InnerRounded,
                borderImageScaleFactor = 0.93f
            ),
            WatermarkModernPresetSpec(
                key = "border_blue_air",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "雾蓝留白",
                enLabel = "Blue Air",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                thinBorderColorIndex = 5,
                borderImageScaleFactor = 0.91f
            ),
            WatermarkModernPresetSpec(
                key = "border_warm_gallery",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "暖白展墙",
                enLabel = "Warm Gallery",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                thinBorderColorIndex = 1,
                borderCornerMode = BorderCornerMode.InnerRounded,
                borderImageScaleFactor = 0.90f
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "floating",
        zhTitle = "悬浮相纸",
        enTitle = "Floating Paper",
        zhSubtitle = "照片轻轻浮起",
        enSubtitle = "A quiet lifted photo",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "floating_white",
                template = WatermarkTemplatePreset.Floating,
                zhLabel = "白底悬浮",
                enLabel = "White Float",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE
            ),
            WatermarkModernPresetSpec(
                key = "floating_black",
                template = WatermarkTemplatePreset.Floating,
                zhLabel = "黑底悬浮",
                enLabel = "Black Float",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE,
                floatingBackgroundColorIndex = 7
            ),
            WatermarkModernPresetSpec(
                key = "floating_blue_stroke",
                template = WatermarkTemplatePreset.Floating,
                zhLabel = "蓝底描边",
                enLabel = "Blue Stroke",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE,
                floatingBackgroundColorIndex = 5,
                floatingStrokeColorIndex = 8,
                floatingImageCornerRadiusRatio = 0.045f
            ),
            WatermarkModernPresetSpec(
                key = "floating_pink_round",
                template = WatermarkTemplatePreset.Floating,
                zhLabel = "雾粉圆角",
                enLabel = "Pink Round",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE,
                floatingBackgroundColorIndex = 0,
                floatingImageCornerRadiusRatio = 0.09f
            ),
            WatermarkModernPresetSpec(
                key = "floating_warm_stroke",
                template = WatermarkTemplatePreset.Floating,
                zhLabel = "\u6696\u7eb8\u9ed1\u8fb9",
                enLabel = "Warm Stroke",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE,
                floatingBackgroundColorIndex = 1,
                floatingStrokeColorIndex = 7,
                floatingImageCornerRadiusRatio = 0.035f
            ),
            WatermarkModernPresetSpec(
                key = "floating_green_soft",
                template = WatermarkTemplatePreset.Floating,
                zhLabel = "苔绿软边",
                enLabel = "Moss Soft",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE,
                floatingBackgroundColorIndex = 3,
                floatingImageCornerRadiusRatio = 0.075f
            ),
            WatermarkModernPresetSpec(
                key = "floating_black_white",
                template = WatermarkTemplatePreset.Floating,
                zhLabel = "黑底白线",
                enLabel = "Black White",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE,
                floatingBackgroundColorIndex = 7,
                floatingStrokeColorIndex = 8,
                floatingImageCornerRadiusRatio = 0.018f
            ),
            WatermarkModernPresetSpec(
                key = "floating_cyan_card",
                template = WatermarkTemplatePreset.Floating,
                zhLabel = "青灰卡片",
                enLabel = "Cyan Card",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                floatingBackgroundColorIndex = 4,
                floatingStrokeColorIndex = 0,
                floatingImageCornerRadiusRatio = 0.055f
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "blur",
        zhTitle = "背景模糊",
        enTitle = "Blur Backgrounds",
        zhSubtitle = "让主体更安静",
        enSubtitle = "Let the subject stay quiet",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "blur_soft",
                template = WatermarkTemplatePreset.BlurBackground,
                zhLabel = "柔和圆角",
                enLabel = "Soft Round",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE
            ),
            WatermarkModernPresetSpec(
                key = "blur_square",
                template = WatermarkTemplatePreset.BlurBackground,
                zhLabel = "方形画面",
                enLabel = "Square Image",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE,
                blurImageCornerRadiusRatio = 0f
            ),
            WatermarkModernPresetSpec(
                key = "blur_large_round",
                template = WatermarkTemplatePreset.BlurBackground,
                zhLabel = "\u5927\u5706\u89d2",
                enLabel = "Large Round",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE,
                blurImageCornerRadiusRatio = 0.14f
            ),
            WatermarkModernPresetSpec(
                key = "blur_soft_portrait",
                template = WatermarkTemplatePreset.BlurBackground,
                zhLabel = "\u7ad6\u56fe\u67d4\u7126",
                enLabel = "Portrait Blur",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                blurImageCornerRadiusRatio = 0.075f
            ),
            WatermarkModernPresetSpec(
                key = "blur_wide_square",
                template = WatermarkTemplatePreset.BlurBackground,
                zhLabel = "\u6a2a\u56fe\u6a21\u7cca",
                enLabel = "Wide Blur",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                blurImageCornerRadiusRatio = 0.035f
            ),
            WatermarkModernPresetSpec(
                key = "blur_logo_date",
                template = WatermarkTemplatePreset.BlurBackground,
                zhLabel = "Logo 日期",
                enLabel = "Logo Date",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE,
                showModelWatermark = false,
                showExifWatermark = false,
                showCityLocationWatermark = false,
                blurImageCornerRadiusRatio = 0.055f
            ),
            WatermarkModernPresetSpec(
                key = "blur_specs_black",
                template = WatermarkTemplatePreset.BlurBackground,
                zhLabel = "参数黑影",
                enLabel = "Specs Shade",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE,
                showLogoWatermark = false,
                showDateWatermark = false,
                showCityLocationWatermark = false,
                blurImageCornerRadiusRatio = 0.02f
            ),
            WatermarkModernPresetSpec(
                key = "blur_place_date",
                template = WatermarkTemplatePreset.BlurBackground,
                zhLabel = "地点日期",
                enLabel = "Place Date",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showExifWatermark = false,
                showCityLocationWatermark = true,
                blurImageCornerRadiusRatio = 0.10f
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "film",
        zhTitle = "胶片复古",
        enTitle = "Film Retro",
        zhSubtitle = "旧颗粒新情绪",
        enSubtitle = "Fresh grain mood",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "film_white",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "白色胶片",
                enLabel = "White Film",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_SIZE,
                filmFrameIndex = 0
            ),
            WatermarkModernPresetSpec(
                key = "film_black",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "黑色胶片",
                enLabel = "Black Film",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_SIZE,
                filmFrameIndex = 1
            ),
            WatermarkModernPresetSpec(
                key = "film_vertical",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "竖黑胶片",
                enLabel = "Vertical Film",
                filmFrameIndex = 2
            ),
            WatermarkModernPresetSpec(
                key = "film_large_black",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "\u5927\u9ed1\u8fb9",
                enLabel = "Large Black",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_SIZE,
                filmFrameIndex = 3
            ),
            WatermarkModernPresetSpec(
                key = "film_weathered",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "剥落边缘",
                enLabel = "Weathered Edge",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_SIZE,
                filmFrameIndex = 4
            ),
            WatermarkModernPresetSpec(
                key = "film_white_signature",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "\u767d\u6846\u7b7e\u540d",
                enLabel = "White Signature",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_SIZE,
                filmFrameIndex = 0,
                signatureText = "Shanghai",
                signatureScaleFactor = 0.72f,
                signatureVerticalOffsetRatio = 0.02f
            ),
            WatermarkModernPresetSpec(
                key = "film_black_signature",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "\u9ed1\u6846\u624b\u5199",
                enLabel = "Black Script",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_WIDE_SIZE,
                filmFrameIndex = 1,
                signatureText = "PHOTOO",
                signatureColor = 0xFFFFFFFF.toInt(),
                signatureScaleFactor = 0.64f,
                signatureHorizontalOffsetRatio = -0.05f
            ),
            WatermarkModernPresetSpec(
                key = "film_vertical_detail",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "\u7ad6\u7248\u7559\u767d",
                enLabel = "Portrait Strip",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                filmFrameIndex = 2,
                signatureText = "01.19",
                signatureScaleFactor = 0.58f
            ),
            WatermarkModernPresetSpec(
                key = "film_large_black_wide",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "\u5927\u9ed1\u6a2a\u7248",
                enLabel = "Large Wide",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                filmFrameIndex = 3
            ),
            WatermarkModernPresetSpec(
                key = "film_weathered_wide",
                template = WatermarkTemplatePreset.FilmRetro,
                zhLabel = "\u98ce\u5316\u6a2a\u7248",
                enLabel = "Weathered Wide",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                filmFrameIndex = 4,
                signatureText = "film",
                signatureScaleFactor = 0.62f
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "polaroid",
        zhTitle = "\u62cd\u7acb\u5f97",
        enTitle = "Polaroid",
        zhSubtitle = "\u76f8\u7eb8\u5373\u65f6\u611f",
        enSubtitle = "Instant paper feel",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "polaroid_classic",
                template = WatermarkTemplatePreset.Polaroid,
                zhLabel = "经典宽底",
                enLabel = "Classic Wide",
                polaroidFrameIndex = 0
            ),
            WatermarkModernPresetSpec(
                key = "polaroid_folded",
                template = WatermarkTemplatePreset.Polaroid,
                zhLabel = "折角竖版",
                enLabel = "Folded Portrait",
                polaroidFrameIndex = 1
            ),
            WatermarkModernPresetSpec(
                key = "polaroid_silver",
                template = WatermarkTemplatePreset.Polaroid,
                zhLabel = "横版银灰",
                enLabel = "Silver Wide",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                polaroidFrameIndex = 2
            ),
            WatermarkModernPresetSpec(
                key = "polaroid_warm",
                template = WatermarkTemplatePreset.Polaroid,
                zhLabel = "复古米白",
                enLabel = "Warm Paper",
                polaroidFrameIndex = 3
            ),
            WatermarkModernPresetSpec(
                key = "polaroid_grain",
                template = WatermarkTemplatePreset.Polaroid,
                zhLabel = "颗粒白边",
                enLabel = "Grain Edge",
                polaroidFrameIndex = 4
            ),
            WatermarkModernPresetSpec(
                key = "polaroid_classic_wide",
                template = WatermarkTemplatePreset.Polaroid,
                zhLabel = "经典横版",
                enLabel = "Classic Wide 2",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                polaroidFrameIndex = 0
            ),
            WatermarkModernPresetSpec(
                key = "polaroid_warm_portrait",
                template = WatermarkTemplatePreset.Polaroid,
                zhLabel = "米白竖版",
                enLabel = "Warm Portrait",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                polaroidFrameIndex = 3
            ),
            WatermarkModernPresetSpec(
                key = "polaroid_grain_square",
                template = WatermarkTemplatePreset.Polaroid,
                zhLabel = "颗粒方片",
                enLabel = "Grain Square",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SQUARE_SIZE,
                polaroidFrameIndex = 4
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "paper_dots",
        zhTitle = "波点纸孔",
        enTitle = "Paper Dots",
        zhSubtitle = "\u4f1a\u547c\u5438\u7684\u7eb8\u5b54",
        enSubtitle = "Breathing paper dots",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "paper_circle",
                template = WatermarkTemplatePreset.PaperDots,
                zhLabel = "圆点纸孔",
                enLabel = "Circle Dots",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE,
                paperDotsConfig = WatermarkPaperDotsConfig(
                    layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                    backgroundColorIndex = 3,
                    dotShape = WatermarkPaperDotsDotShape.Circle
                )
            ),
            WatermarkModernPresetSpec(
                key = "paper_heart",
                template = WatermarkTemplatePreset.PaperDots,
                zhLabel = "爱心纸孔",
                enLabel = "Heart Dots",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE,
                paperDotsConfig = WatermarkPaperDotsConfig(
                    layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                    backgroundColorIndex = 2,
                    dotShape = WatermarkPaperDotsDotShape.Heart,
                    dotCount = 20
                )
            ),
            WatermarkModernPresetSpec(
                key = "paper_star_duo",
                template = WatermarkTemplatePreset.PaperDots,
                zhLabel = "双色星孔",
                enLabel = "Duotone Stars",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE,
                paperDotsConfig = WatermarkPaperDotsConfig(
                    layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                    backgroundMode = WatermarkPaperDotsBackgroundMode.DuoTone,
                    backgroundColorIndex = 4,
                    backgroundSecondaryColorIndex = 6,
                    dotShape = WatermarkPaperDotsDotShape.Star,
                    dotSize = 24f,
                    dotCount = 22,
                    strokeMode = WatermarkPaperDotsStrokeMode.Mix
                )
            ),
            WatermarkModernPresetSpec(
                key = "paper_photo_snow",
                template = WatermarkTemplatePreset.PaperDots,
                zhLabel = "照片雪花",
                enLabel = "Photo Snow",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE,
                paperDotsConfig = WatermarkPaperDotsConfig(
                    layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                    backgroundMode = WatermarkPaperDotsBackgroundMode.Photo,
                    backgroundColorIndex = 4,
                    dotShape = WatermarkPaperDotsDotShape.Snowflake,
                    dotSize = 20f,
                    dotCount = 18
                )
            ),
            WatermarkModernPresetSpec(
                key = "paper_line_wide",
                template = WatermarkTemplatePreset.PaperDots,
                zhLabel = "\u83f1\u5f62\u6253\u5b54",
                enLabel = "Diamond Holes",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE,
                paperDotsConfig = WatermarkPaperDotsConfig(
                    layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                    backgroundMode = WatermarkPaperDotsBackgroundMode.Solid,
                    backgroundColorIndex = 5,
                    dotShape = WatermarkPaperDotsDotShape.Diamond,
                    dotSize = 18f,
                    dotCount = 24,
                    strokeMode = WatermarkPaperDotsStrokeMode.AllStroke
                )
            ),
            WatermarkModernPresetSpec(
                key = "paper_square_green",
                template = WatermarkTemplatePreset.PaperDots,
                zhLabel = "方孔绿纸",
                enLabel = "Square Moss",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE,
                paperDotsConfig = WatermarkPaperDotsConfig(
                    layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                    backgroundColorIndex = 3,
                    dotShape = WatermarkPaperDotsDotShape.Square,
                    dotSize = 22f,
                    dotCount = 18
                )
            ),
            WatermarkModernPresetSpec(
                key = "paper_drop_blue",
                template = WatermarkTemplatePreset.PaperDots,
                zhLabel = "水滴蓝纸",
                enLabel = "Blue Drops",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE,
                paperDotsConfig = WatermarkPaperDotsConfig(
                    layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                    backgroundColorIndex = 5,
                    dotShape = WatermarkPaperDotsDotShape.Drop,
                    dotSize = 19f,
                    dotCount = 22,
                    strokeMode = WatermarkPaperDotsStrokeMode.Mix
                )
            ),
            WatermarkModernPresetSpec(
                key = "paper_hex_photo",
                template = WatermarkTemplatePreset.PaperDots,
                zhLabel = "六边照片",
                enLabel = "Hex Photo",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_PAPER_DOTS_SIZE,
                paperDotsConfig = WatermarkPaperDotsConfig(
                    layoutDirection = WatermarkPaperDotsLayoutDirection.ImageTop,
                    backgroundMode = WatermarkPaperDotsBackgroundMode.Photo,
                    backgroundColorIndex = 1,
                    dotShape = WatermarkPaperDotsDotShape.Hexagon,
                    dotSize = 20f,
                    dotCount = 20
                )
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "color",
        zhTitle = "色彩拼贴",
        enTitle = "Color Layouts",
        zhSubtitle = "\u4ece\u7167\u7247\u53d6\u8272",
        enSubtitle = "Colors from the photo",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "color_hex_default",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "底部色卡",
                enLabel = "Bottom Palette",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.HexCard.ordinal
            ),
            WatermarkModernPresetSpec(
                key = "color_hex_heart",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "爱心色卡",
                enLabel = "Heart Palette",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.HexCard.ordinal,
                colorPaletteVariantIndex = 2
            ),
            WatermarkModernPresetSpec(
                key = "color_editorial",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "刊页竖栏",
                enLabel = "Editorial Strip",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_SOFT_PORTRAIT_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.EditorialStrip.ordinal
            ),
            WatermarkModernPresetSpec(
                key = "color_chinese",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "中国色其一",
                enLabel = "Chinese Palette",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_EXTRA_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_CHINESE_WIDE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.ChinesePalette.ordinal
            ),
            WatermarkModernPresetSpec(
                key = "color_mosaic",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "像素方块",
                enLabel = "Mosaic Grid",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.MosaicGrid.ordinal,
                colorPaletteElementCount = 10
            ),
            WatermarkModernPresetSpec(
                key = "color_circle",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "圆形色带",
                enLabel = "Circle Bands",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.CircleBands.ordinal
            ),
            WatermarkModernPresetSpec(
                key = "color_hex_wide",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "\u6a2a\u7248\u8272\u5361",
                enLabel = "Wide Palette",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_EXTRA_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_CHINESE_WIDE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.HexCard.ordinal,
                colorPaletteElementCount = 5,
                colorPaletteShowRgbValue = false
            ),
            WatermarkModernPresetSpec(
                key = "color_editorial_wide",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "\u520a\u9875\u6a2a\u680f",
                enLabel = "Editorial Wide",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.EditorialStrip.ordinal,
                colorPaletteHorizontalOffsetRatio = 0.06f
            ),
            WatermarkModernPresetSpec(
                key = "color_chinese_soft",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "\u4e2d\u56fd\u8272\u4e4b\u4e8c",
                enLabel = "Chinese Soft",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_EXTRA_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_CHINESE_WIDE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.ChinesePaletteMono.ordinal,
                colorPaletteVariantIndex = 1,
                colorPaletteElementCount = 5
            ),
            WatermarkModernPresetSpec(
                key = "color_gradient_bar",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "底部渐变",
                enLabel = "Gradient Bar",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.BottomGradientBar.ordinal,
                colorPaletteElementCount = 8,
                colorPaletteShowRgbValue = false
            ),
            WatermarkModernPresetSpec(
                key = "color_center_stripes",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "横向色条",
                enLabel = "Center Stripes",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_WIDE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_WIDE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.CenterStripes.ordinal,
                colorPaletteVariantIndex = 1,
                colorPaletteElementCount = 7
            ),
            WatermarkModernPresetSpec(
                key = "color_center_blocks",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "块面拼贴",
                enLabel = "Center Blocks",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_SQUARE_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_LARGE_SQUARE_SIZE,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.CenterBlocks.ordinal,
                colorPaletteVariantIndex = 1,
                colorPaletteElementCount = 7
            )
        )
    ),
    WatermarkModernPresetSeries(
        key = "color_glass",
        zhTitle = "玻璃面板",
        enTitle = "Glass Panel",
        zhSubtitle = "照片底部玻璃面板",
        enSubtitle = "Glass panel at the photo bottom",
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "color_glass_dots",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "清透单行",
                enLabel = "Clear Single",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showDateWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = false,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.GlassDots.ordinal,
                colorPaletteVariantIndex = 0,
                colorPaletteElementCount = 6,
                colorPaletteShowHexValue = false,
                colorPaletteShowRgbValue = false
            ),
            WatermarkModernPresetSpec(
                key = "color_glass_dots_double",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "清透双行",
                enLabel = "Clear Double",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showDateWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = false,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.GlassDots.ordinal,
                colorPaletteVariantIndex = 1,
                colorPaletteElementCount = 6,
                colorPaletteShowHexValue = false,
                colorPaletteShowRgbValue = false
            ),
            WatermarkModernPresetSpec(
                key = "color_glass_dots_pearl_hex",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "珍珠 HEX",
                enLabel = "Pearl HEX",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showDateWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = false,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.GlassDots.ordinal,
                colorPaletteVariantIndex = 3,
                colorPaletteElementCount = 8,
                colorPaletteShowHexValue = true,
                colorPaletteShowRgbValue = false
            ),
            WatermarkModernPresetSpec(
                key = "color_glass_dots_smoke_double",
                template = WatermarkTemplatePreset.ColorPalette,
                zhLabel = "雾灰双行",
                enLabel = "Smoke Double",
                previewAspectRatio = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_ASPECT_RATIO,
                previewOutputSize = WATERMARK_MODERN_PREVIEW_TALL_DETAIL_SIZE,
                showLogoWatermark = false,
                showModelWatermark = false,
                showDateWatermark = false,
                showExifWatermark = false,
                showLocationWatermark = false,
                showCityLocationWatermark = false,
                colorPaletteStyleIndex = WatermarkColorPaletteStyle.GlassDots.ordinal,
                colorPaletteVariantIndex = 5,
                colorPaletteElementCount = 8,
                colorPaletteShowHexValue = false,
                colorPaletteShowRgbValue = false
            )
        )
    )
) + buildWatermarkModernCutoutPresetSeries() + buildWatermarkModernSubjectOutlinePresetSeries()

private fun watermarkModernPreviewCacheKey(
    preset: WatermarkModernPresetSpec,
    demoAsset: ColorCardFilterFeedDemoAsset,
    previewCopy: WatermarkModernPreviewCopy
): String = buildString {
    append(WATERMARK_MODERN_PREVIEW_CACHE_VERSION)
    append(':')
    append(preset.key)
    append(':')
    append(demoAsset.id)
    append(':')
    append(preset.previewOutputSize.first)
    append('x')
    append(preset.previewOutputSize.second)
    append(':')
    append(preset.cutoutFrameAspectRatio.name)
    append(':')
    append(previewCopy.cacheKey)
}

private fun getCachedWatermarkModernPreviewBitmap(cacheKey: String): Bitmap? {
    return synchronized(WATERMARK_MODERN_PREVIEW_CACHE_LOCK) {
        WATERMARK_MODERN_PREVIEW_BITMAP_CACHE[cacheKey]?.takeUnless { it.isRecycled }
    }
}

private fun cacheWatermarkModernPreviewBitmap(cacheKey: String, bitmap: Bitmap) {
    synchronized(WATERMARK_MODERN_PREVIEW_CACHE_LOCK) {
        WATERMARK_MODERN_PREVIEW_BITMAP_CACHE[cacheKey] = bitmap
    }
}
