package com.voicelike.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.icu.text.Transliterator
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.voicelike.app.startup.StartupBottomBarScrollSnapshot
import com.voicelike.app.startup.startupBottomBarAbsoluteOffset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.voicelike.app.i18n.FilterFeedStrings
import com.voicelike.app.colorcard.lensy.rememberColorCardLensyAssessment
import com.voicelike.app.motionphoto.MotionPhotoExportCoordinator
import com.voicelike.app.motionphoto.toDynamicPhotoExportToastMessage
import com.voicelike.app.motionphoto.MotionPhotoFileRepository
import com.voicelike.app.motionphoto.MotionPhotoLabPage
import com.voicelike.app.motionphoto.MotionPhotoRenderSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

private const val COLOR_CARD_REFRESH_MIN_DURATION_MS = 980L
private const val COLOR_CARD_REFRESH_FEED_RESET_DELAY_MS = 260L
private const val COLOR_CARD_MAX_VISIBLE_COLORS = 10
private const val COLOR_CARD_PRIMARY_COLOR_MIN_SATURATION = 0.18f
private const val COLOR_CARD_PRIMARY_COLOR_MIN_BRIGHTNESS = 0.14f
private const val COLOR_CARD_PRIMARY_COLOR_BOUNDARY_GUARD_DEGREES = 4.5f
private const val COLOR_CARD_IMPORTED_OVERLAY_DISMISS_DELAY_MS = 16L
private const val COLOR_CARD_MODERN_HEADER_COLLAPSE_OFFSET_PX = 36
private const val COLOR_CARD_MODERN_HEADER_EXPAND_OFFSET_PX = 10
private const val COLOR_CARD_CHINESE_TRADITIONAL_TAG = "中国传统色"

private enum class ColorCardTab {
    Mine,
    Inspiration,
    Filter
}

private enum class ModernColorCardRootFeedMode {
    Inspiration,
    Watermark,
    Filter,
    Prompt
}

private enum class ModernColorCardRootSectionSource {
    Watermark,
    Filter
}

private fun supportsModernColorCardPromptTab(language: AppLanguage): Boolean =
    language == AppLanguage.SimplifiedChinese || language == AppLanguage.TraditionalChinese

private fun modernColorCardRootFeedModes(): List<ModernColorCardRootFeedMode> =
    ModernColorCardRootFeedMode.entries.filterNot { it == ModernColorCardRootFeedMode.Prompt }

private data class ModernColorCardRootSection(
    val key: String,
    val source: ModernColorCardRootSectionSource,
    val filterSeries: ColorCardFilterFeedModernSeries? = null,
    val watermarkSeries: WatermarkModernPresetSeries? = null
)

private sealed interface ColorCardDetailTarget {
    data class Mine(val palette: SavedColorCardPalette) : ColorCardDetailTarget
    data class Inspiration(val entry: ColorCardChineseInspirationEntry) : ColorCardDetailTarget
    data class TopJournal(val entry: ColorCardTopJournalPaletteEntry) : ColorCardDetailTarget
    data class Nice(val entry: ColorCardNicePaletteEntry) : ColorCardDetailTarget
    data class Nord(val entry: ColorCardNordPaletteEntry) : ColorCardDetailTarget
    data class Paletteer(val entry: ColorCardPaletteerPaletteEntry) : ColorCardDetailTarget
    data class Gradient(val entry: ColorCardGradientInspirationEntry) : ColorCardDetailTarget
}

private data class ColorCardDetailHeader(
    val eyebrow: String,
    val title: String,
    val useChineseFont: Boolean
)

private data class ColorCardDisplayProfile(
    val referenceColorSpace: String,
    val deviceWideGamut: String,
    val windowColorMode: String,
    val renderingNote: String
)

private enum class ColorCardInspirationPrimaryColor(
    val label: String,
    val swatch: Int
) {
    Red("红", 0xFFC84E5A.toInt()),
    Orange("橙", 0xFFD08A46.toInt()),
    Yellow("黄", 0xFFD6B652.toInt()),
    Green("绿", 0xFF5D9E6A.toInt()),
    Cyan("青", 0xFF4B9EA0.toInt()),
    Blue("蓝", 0xFF4F77B9.toInt()),
    Purple("紫", 0xFF8B63B8.toInt())
}

private data class ColorCardInspirationFilter(
    val query: String = "",
    val primaryColor: ColorCardInspirationPrimaryColor? = null,
    val topicTag: String? = null
) {
    val hasActiveSelection: Boolean
        get() = query.isNotBlank() || primaryColor != null || !topicTag.isNullOrBlank()
}

private data class ColorCardInspirationFeedEntry(
    val key: String,
    val sourceTag: String,
    val target: ColorCardDetailTarget
)

private data class ColorCardInspirationCatalog(
    val topJournalEntries: List<ColorCardTopJournalPaletteEntry>,
    val niceEntries: List<ColorCardNicePaletteEntry>,
    val nordEntries: List<ColorCardNordPaletteEntry>,
    val paletteerEntries: List<ColorCardPaletteerPaletteEntry>,
    val chineseEntries: List<ColorCardChineseInspirationEntry>,
    val gradientEntries: List<ColorCardGradientInspirationEntry>
)

private data class ColorCardShareSpec(
    val chooserTitle: String,
    val subject: String,
    val text: String
)

private data class ColorCardFavoriteSpec(
    val key: String,
    val title: String,
    val sourceTag: String,
    val useChineseFont: Boolean,
    val colors: List<Int>,
    val gradientSpec: ColorCardGradientSpec? = null
)

private sealed interface ColorCardPendingPaletteAction {
    val palette: SavedColorCardPalette

    data class Delete(override val palette: SavedColorCardPalette) : ColorCardPendingPaletteAction

    data class Unfavorite(override val palette: SavedColorCardPalette) : ColorCardPendingPaletteAction
}

@Composable
fun ColorCardPage(
    onBack: () -> Unit,
    onOpenCamera: () -> Unit = {},
    onOpenStampCamera: () -> Unit = {},
    onOpenElectronicAlbum: () -> Unit = {},
    onOpenPromptManager: () -> Unit = {},
    onOpenFillLight: () -> Unit = {},
    onOpenWatermarkPreset: (String) -> Unit = {},
    onInternalBackHandlingChanged: (Boolean) -> Unit = {},
    entryResetSignal: Int = 0,
    initialFilterPaletteId: String? = null,
    autoLaunchPickerForInitialFilter: Boolean = false,
    onInitialFilterConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    onScrollActivityChanged: (Boolean) -> Unit = {},
    onBottomBarScrollSnapshotChanged: (StartupBottomBarScrollSnapshot) -> Unit = {},
    bottomContentPadding: Dp = 28.dp,
    modernEmbeddedStyle: Boolean = false,
    onRootLayerChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val appLanguage = LocalAppLanguage.current
    val appStrings = LocalAppStrings.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val scope = rememberCoroutineScope()
    val store = remember(context) { ColorCardStore.get(context) }
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val performHaptic = remember(haptic, hapticsAllowed) {
        {
            if (hapticsAllowed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    val palettes by store.palettes.collectAsState()
    val favoritePalettesByKey = remember(palettes) {
        palettes
            .filter { it.isFavorite && !it.favoriteKey.isNullOrBlank() }
            .associateBy { it.favoriteKey.orEmpty() }
    }
    var importedImageSource by remember { mutableStateOf<ColorCardImportedImageSource?>(null) }
    var importingImage by remember { mutableStateOf(false) }
    var pendingImportedFilterPaletteId by remember { mutableStateOf<String?>(null) }
    var pendingImportedPanelMode by remember { mutableStateOf<ColorCardTracePanelMode?>(null) }
    var appliedInitialFilterPaletteId by rememberSaveable { mutableStateOf<String?>(null) }
    val latestImportedImageSource = rememberUpdatedState(importedImageSource)

    var selectedTab by if (modernEmbeddedStyle) {
        remember { mutableStateOf(ColorCardTab.Filter) }
    } else {
        rememberSaveable { mutableStateOf(ColorCardTab.Inspiration) }
    }
    var selectedDetail by remember { mutableStateOf<ColorCardDetailTarget?>(null) }
    var detailTransitionTarget by remember { mutableStateOf<ColorCardDetailTarget?>(null) }
    var selectedPromptEntry by remember { mutableStateOf<ColorCardPromptEntry?>(null) }
    var promptGeneratorVisible by remember { mutableStateOf(false) }
    var pendingPaletteAction by remember { mutableStateOf<ColorCardPendingPaletteAction?>(null) }
    var inspirationCatalog by remember(context) { mutableStateOf<ColorCardInspirationCatalog?>(null) }
    var inspirationLoading by remember(context) { mutableStateOf(false) }
    var inspirationSearchMode by rememberSaveable { mutableStateOf(false) }
    var inspirationSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedInspirationPrimaryColorName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedInspirationTopicTag by rememberSaveable { mutableStateOf<String?>(null) }
    val mineListState = rememberLazyListState()
    var inspirationListResetToken by rememberSaveable { mutableIntStateOf(0) }
    val inspirationListState = remember(inspirationListResetToken) { LazyListState() }
    val inspirationResultListState = rememberLazyListState()
    var modernRootFeedModeIndex by rememberSaveable {
        mutableIntStateOf(ModernColorCardRootFeedMode.Inspiration.ordinal)
    }
    val modernRootFeedModes = remember { modernColorCardRootFeedModes() }
    var modernRootFeedShuffleSeed by rememberSaveable {
        mutableLongStateOf(Random.Default.nextLong())
    }
    val modernRootFeedMode = ModernColorCardRootFeedMode.entries
        .getOrElse(modernRootFeedModeIndex) { ModernColorCardRootFeedMode.Inspiration }
        .takeIf { it in modernRootFeedModes }
        ?: ModernColorCardRootFeedMode.Inspiration
    var modernRootListResetToken by rememberSaveable { mutableIntStateOf(0) }
    val modernRootInspirationListState = remember(modernRootListResetToken) { LazyListState() }
    val modernRootWatermarkListState = remember(modernRootListResetToken) { LazyListState() }
    val modernRootPromptListState = remember(modernRootListResetToken) { LazyListState() }
    var filterListResetToken by rememberSaveable { mutableIntStateOf(0) }
    val filterListState = remember(filterListResetToken) { LazyListState() }
    var inspirationFilterLastAutoScrollSignature by rememberSaveable { mutableStateOf<String?>(null) }
    var inspirationShuffleSeed by rememberSaveable {
        mutableLongStateOf(nextColorCardInspirationShuffleSeed())
    }
    var inspirationRefreshing by remember { mutableStateOf(false) }
    val bottomBarNearTopThresholdPx = with(density) { 56.dp.roundToPx() }
    LaunchedEffect(
        mineListState,
        inspirationListState,
        inspirationResultListState,
        modernRootInspirationListState,
        modernRootWatermarkListState,
        modernRootPromptListState,
        filterListState,
        onScrollActivityChanged
    ) {
        snapshotFlow {
            mineListState.isScrollInProgress ||
                inspirationListState.isScrollInProgress ||
                inspirationResultListState.isScrollInProgress ||
                modernRootInspirationListState.isScrollInProgress ||
                modernRootWatermarkListState.isScrollInProgress ||
                modernRootPromptListState.isScrollInProgress ||
                filterListState.isScrollInProgress
        }.collect { onScrollActivityChanged(it) }
    }
    val inspirationSearchFilter = ColorCardInspirationFilter(
        query = inspirationSearchQuery,
        primaryColor = selectedInspirationPrimaryColorName?.let { name ->
            ColorCardInspirationPrimaryColor.entries.firstOrNull { it.name == name }
        },
        topicTag = selectedInspirationTopicTag
    )
    val isModernRootLayer by remember(
        modernEmbeddedStyle,
        selectedTab,
        selectedDetail,
        selectedPromptEntry,
        promptGeneratorVisible,
        inspirationSearchMode
    ) {
        derivedStateOf {
            !modernEmbeddedStyle ||
                (
                    selectedTab == ColorCardTab.Filter &&
                        selectedDetail == null &&
                        selectedPromptEntry == null &&
                        !promptGeneratorVisible &&
                        !inspirationSearchMode
                )
        }
    }
    LaunchedEffect(
        modernEmbeddedStyle,
        selectedTab,
        selectedDetail,
        selectedPromptEntry,
        promptGeneratorVisible,
        inspirationSearchMode,
        inspirationSearchFilter.hasActiveSelection,
        modernRootFeedMode,
        isModernRootLayer,
        mineListState,
        inspirationListState,
        inspirationResultListState,
        modernRootInspirationListState,
        modernRootWatermarkListState,
        modernRootPromptListState,
        filterListState,
        bottomBarNearTopThresholdPx,
        onBottomBarScrollSnapshotChanged
    ) {
        if (!modernEmbeddedStyle) return@LaunchedEffect
        snapshotFlow {
            if (!isModernRootLayer) {
                StartupBottomBarScrollSnapshot(
                    source = "colorcard-overlay",
                    absoluteOffsetPx = 0,
                    nearTop = false,
                    forceHidden = true
                )
            } else {
                val activeListState = when (selectedTab) {
                    ColorCardTab.Mine -> mineListState
                    ColorCardTab.Inspiration -> {
                        if (inspirationSearchMode && inspirationSearchFilter.hasActiveSelection) {
                            inspirationResultListState
                        } else {
                            inspirationListState
                        }
                    }
                    ColorCardTab.Filter -> when (modernRootFeedMode) {
                        ModernColorCardRootFeedMode.Inspiration -> modernRootInspirationListState
                        ModernColorCardRootFeedMode.Watermark -> modernRootWatermarkListState
                        ModernColorCardRootFeedMode.Prompt -> modernRootPromptListState
                        ModernColorCardRootFeedMode.Filter -> filterListState
                    }
                }
                val source = when (selectedTab) {
                    ColorCardTab.Mine -> "colorcard-mine"
                    ColorCardTab.Inspiration -> {
                        if (inspirationSearchMode && inspirationSearchFilter.hasActiveSelection) {
                            "colorcard-inspiration-search"
                        } else {
                            "colorcard-inspiration"
                        }
                    }
                    ColorCardTab.Filter -> when (modernRootFeedMode) {
                        ModernColorCardRootFeedMode.Inspiration -> "colorcard-root-inspiration"
                        ModernColorCardRootFeedMode.Watermark -> "colorcard-root-watermark"
                        ModernColorCardRootFeedMode.Prompt -> "colorcard-root-prompt"
                        ModernColorCardRootFeedMode.Filter -> "colorcard-root-filter"
                    }
                }
                StartupBottomBarScrollSnapshot(
                    source = source,
                    absoluteOffsetPx = startupBottomBarAbsoluteOffset(
                        activeListState.firstVisibleItemIndex,
                        activeListState.firstVisibleItemScrollOffset
                    ),
                    nearTop = activeListState.firstVisibleItemIndex == 0 &&
                        activeListState.firstVisibleItemScrollOffset <= bottomBarNearTopThresholdPx,
                    forceHidden = false
                )
            }
        }.collect { onBottomBarScrollSnapshotChanged(it) }
    }
    var modernHeaderCollapsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(
        modernEmbeddedStyle,
        selectedTab,
        modernRootFeedMode,
        inspirationSearchMode,
        inspirationSearchFilter.hasActiveSelection,
        mineListState,
        inspirationListState,
        inspirationResultListState,
        modernRootInspirationListState,
        modernRootWatermarkListState,
        modernRootPromptListState,
        filterListState
    ) {
        if (!modernEmbeddedStyle) {
            modernHeaderCollapsed = false
            return@LaunchedEffect
        }
        snapshotFlow {
            val activeListState = when (selectedTab) {
                ColorCardTab.Mine -> mineListState
                ColorCardTab.Inspiration -> {
                    if (inspirationSearchMode && inspirationSearchFilter.hasActiveSelection) {
                        inspirationResultListState
                    } else {
                        inspirationListState
                    }
                }
                ColorCardTab.Filter -> when (modernRootFeedMode) {
                    ModernColorCardRootFeedMode.Inspiration -> modernRootInspirationListState
                    ModernColorCardRootFeedMode.Watermark -> modernRootWatermarkListState
                    ModernColorCardRootFeedMode.Prompt -> modernRootPromptListState
                    ModernColorCardRootFeedMode.Filter -> filterListState
                }
            }
            activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
        }.collect { (firstVisibleItemIndex, firstVisibleItemScrollOffset) ->
            val shouldCollapse =
                firstVisibleItemIndex > 0 ||
                    firstVisibleItemScrollOffset >= COLOR_CARD_MODERN_HEADER_COLLAPSE_OFFSET_PX
            val shouldExpand =
                firstVisibleItemIndex == 0 &&
                    firstVisibleItemScrollOffset <= COLOR_CARD_MODERN_HEADER_EXPAND_OFFSET_PX
            modernHeaderCollapsed = when {
                modernHeaderCollapsed && shouldExpand -> false
                !modernHeaderCollapsed && shouldCollapse -> true
                else -> modernHeaderCollapsed
            }
        }
    }
    val clearInspirationSearchMode = {
        inspirationSearchMode = false
        inspirationSearchQuery = ""
        selectedInspirationPrimaryColorName = null
        selectedInspirationTopicTag = null
        inspirationFilterLastAutoScrollSignature = null
    }
    val openColorCardSubpage = {
        if (selectedTab == ColorCardTab.Filter) {
            selectedTab = ColorCardTab.Inspiration
        }
    }
    val handleInternalBack = {
        when {
            promptGeneratorVisible -> promptGeneratorVisible = false
            selectedPromptEntry != null -> selectedPromptEntry = null
            selectedDetail != null -> selectedDetail = null
            inspirationSearchMode -> clearInspirationSearchMode()
            modernEmbeddedStyle && selectedTab != ColorCardTab.Filter -> selectedTab = ColorCardTab.Filter
        }
    }
    val dismissImportedImagePicker: () -> Unit = {
        importedImageSource?.let(::cleanupColorCardImportedImageSource)
        importedImageSource = null
        pendingImportedFilterPaletteId = null
        pendingImportedPanelMode = null
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            pendingImportedFilterPaletteId = null
            pendingImportedPanelMode = null
            return@rememberLauncherForActivityResult
        }
        val uri = result.data?.data ?: result.data?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        if (uri == null) {
            pendingImportedFilterPaletteId = null
            pendingImportedPanelMode = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            importingImage = true
            runCatching {
                importColorCardPickedImage(context = context, sourceUri = uri)
            }.onSuccess { imported ->
                importedImageSource?.let(::cleanupColorCardImportedImageSource)
                importedImageSource = imported.copy(
                    initialPanelMode = if (pendingImportedFilterPaletteId != null) {
                        ColorCardTracePanelMode.Filter
                    } else {
                        pendingImportedPanelMode ?: imported.initialPanelMode
                    },
                    initialPaletteId = pendingImportedFilterPaletteId
                )
                pendingImportedFilterPaletteId = null
                pendingImportedPanelMode = null
            }.onFailure { error ->
                pendingImportedFilterPaletteId = null
                pendingImportedPanelMode = null
                Toast.makeText(
                    context,
                    error.message ?: "\u6253\u5f00\u56fe\u7247\u5931\u8d25",
                    Toast.LENGTH_SHORT
                ).show()
            }
            importingImage = false
        }
    }
    val launchImagePicker: (ColorCardTracePanelMode) -> Unit = { initialPanelMode ->
        pendingImportedFilterPaletteId = null
        pendingImportedPanelMode = initialPanelMode
        imagePickerLauncher.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        )
    }
    val scrollCurrentTabToTop: () -> Unit = {
        scope.launch {
            when (selectedTab) {
                ColorCardTab.Mine -> {
                    if (palettes.isNotEmpty()) {
                        mineListState.animateScrollToItem(0)
                    }
                }
                ColorCardTab.Inspiration -> {
                    if (inspirationSearchMode && inspirationSearchFilter.hasActiveSelection) {
                        inspirationResultListState.animateScrollToItem(0)
                    } else if (inspirationCatalog != null) {
                        inspirationListState.animateScrollToItem(0)
                    }
                }
                ColorCardTab.Filter -> {
                    when (modernRootFeedMode) {
                        ModernColorCardRootFeedMode.Inspiration -> {
                            modernRootInspirationListState.animateScrollToItem(0)
                        }
                        ModernColorCardRootFeedMode.Watermark -> {
                            modernRootWatermarkListState.animateScrollToItem(0)
                        }
                        ModernColorCardRootFeedMode.Prompt -> {
                            modernRootPromptListState.animateScrollToItem(0)
                        }
                        ModernColorCardRootFeedMode.Filter -> {
                            filterListState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(initialFilterPaletteId) {
        val paletteId = initialFilterPaletteId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (appliedInitialFilterPaletteId == paletteId) return@LaunchedEffect
        appliedInitialFilterPaletteId = paletteId
        pendingImportedFilterPaletteId = paletteId
        pendingImportedPanelMode = ColorCardTracePanelMode.Filter
        selectedTab = ColorCardTab.Filter
        modernRootFeedModeIndex = ModernColorCardRootFeedMode.Filter.ordinal
        if (autoLaunchPickerForInitialFilter) {
            delay(COLOR_CARD_IMPORTED_OVERLAY_DISMISS_DELAY_MS)
            imagePickerLauncher.launch(
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "image/*"
                }
            )
        }
        onInitialFilterConsumed()
    }

    LaunchedEffect(selectedDetail) {
        selectedDetail?.let { detailTransitionTarget = it }
    }

    LaunchedEffect(store) {
        store.ensureLoaded()
    }
    LaunchedEffect(modernEmbeddedStyle, entryResetSignal) {
        if (!modernEmbeddedStyle) return@LaunchedEffect
        selectedDetail = null
        selectedPromptEntry = null
        promptGeneratorVisible = false
        clearInspirationSearchMode()
        selectedTab = ColorCardTab.Filter
        modernRootFeedModeIndex = ModernColorCardRootFeedMode.Inspiration.ordinal
        modernRootListResetToken += 1
        filterListResetToken += 1
    }
    LaunchedEffect(
        entryResetSignal,
        modernEmbeddedStyle,
        modernRootInspirationListState,
        modernRootWatermarkListState,
        modernRootPromptListState,
        filterListState
    ) {
        if (!modernEmbeddedStyle) return@LaunchedEffect
        modernRootInspirationListState.scrollToItem(0)
        modernRootWatermarkListState.scrollToItem(0)
        modernRootPromptListState.scrollToItem(0)
        filterListState.scrollToItem(0)
    }
    LaunchedEffect(selectedTab, context) {
        if (selectedTab != ColorCardTab.Inspiration || inspirationCatalog != null || inspirationLoading) {
            return@LaunchedEffect
        }

        inspirationLoading = true
        try {
            inspirationCatalog = loadColorCardInspirationCatalog(context)
        } finally {
            inspirationLoading = false
        }
    }
    LaunchedEffect(palettes, selectedDetail) {
        val currentDetail = selectedDetail
        if (currentDetail is ColorCardDetailTarget.Mine) {
            val stillExists = palettes.any { it.id == currentDetail.palette.id }
            if (!stillExists) {
                selectedDetail = null
            }
        }
    }

    val handlesBackInternally =
        promptGeneratorVisible ||
            selectedPromptEntry != null ||
            selectedDetail != null ||
            inspirationSearchMode ||
            (modernEmbeddedStyle && selectedTab != ColorCardTab.Filter)

    SideEffect {
        onInternalBackHandlingChanged(handlesBackInternally)
        onRootLayerChanged(isModernRootLayer)
    }
    DisposableEffect(Unit) {
        onDispose {
            onInternalBackHandlingChanged(false)
            onRootLayerChanged(true)
        }
    }

    BackHandler(enabled = handlesBackInternally) {
        handleInternalBack()
    }
    DisposableEffect(Unit) {
        onDispose {
            cleanupColorCardImportedImageSource(latestImportedImageSource.value)
        }
    }

    pendingPaletteAction?.let { pendingAction ->
        val palette = pendingAction.palette
        val isUnfavorite = pendingAction is ColorCardPendingPaletteAction.Unfavorite
        AlertDialog(
            onDismissRequest = { pendingPaletteAction = null },
            title = {
                Text(text = if (isUnfavorite) colorCardStrings.unfavoriteTitle else colorCardStrings.deletePaletteTitle)
            },
            text = {
                Text(
                    text = if (isUnfavorite) {
                        colorCardStrings.unfavoriteMessage
                    } else {
                        colorCardStrings.deleteMessage
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingPaletteAction = null
                        if ((selectedDetail as? ColorCardDetailTarget.Mine)?.palette?.id == palette.id) {
                            selectedDetail = null
                        }
                        store.deletePaletteBlocking(palette.id)
                    }
                ) {
                    Text(
                        text = if (isUnfavorite) colorCardStrings.unfavoriteTitle else colorCardStrings.deletePaletteTitle,
                        color = if (isUnfavorite) Color(0xFFD35C63) else Color(0xFFD64545)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPaletteAction = null }) {
                    Text(text = colorCardStrings.close)
                }
            }
        )
    }

    if (promptGeneratorVisible) {
        ColorCardPromptGeneratorPage(
            onBack = { promptGeneratorVisible = false }
        )
        return
    }

    val libraryTopContent: (@Composable () -> Unit)? = if (modernEmbeddedStyle && selectedTab != ColorCardTab.Filter) {
        {
            ColorCardTabSlider(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    if (it != ColorCardTab.Inspiration) {
                        clearInspirationSearchMode()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(top = 4.dp, bottom = 6.dp),
                compact = true
            )
        }
    } else {
        null
    }

    val renderColorCardLibraryPages: @Composable () -> Unit = {
        when (selectedTab) {
            ColorCardTab.Mine -> {
                MyColorCardList(
                    listState = mineListState,
                    palettes = palettes,
                    onPaletteClick = { selectedDetail = ColorCardDetailTarget.Mine(it) },
                    onDeleteRequest = { pendingPaletteAction = ColorCardPendingPaletteAction.Delete(it) },
                    onFavoriteToggleRequest = { palette ->
                        pendingPaletteAction = ColorCardPendingPaletteAction.Unfavorite(palette)
                    },
                    topContent = libraryTopContent,
                    bottomContentPadding = bottomContentPadding
                )
            }
            ColorCardTab.Inspiration -> {
                val catalog = inspirationCatalog
                if (catalog == null) {
                    ColorCardInspirationLoadingState(
                        isLoading = inspirationLoading
                    )
                } else {
                    val appLanguage = LocalAppLanguage.current
                    val feedEntries = remember(
                        inspirationShuffleSeed,
                        appLanguage,
                        catalog.topJournalEntries,
                        catalog.niceEntries,
                        catalog.nordEntries,
                        catalog.paletteerEntries,
                        catalog.chineseEntries,
                        catalog.gradientEntries
                    ) {
                        buildShuffledColorCardInspirationFeed(
                            shuffleSeed = inspirationShuffleSeed,
                            language = appLanguage,
                            topJournalEntries = catalog.topJournalEntries,
                            niceEntries = catalog.niceEntries,
                            nordEntries = catalog.nordEntries,
                            paletteerEntries = catalog.paletteerEntries,
                            chineseEntries = catalog.chineseEntries,
                            gradientEntries = catalog.gradientEntries
                        )
                    }
                    val filterTopics = remember(feedEntries) {
                        buildColorCardInspirationFilterTopics(feedEntries)
                    }
                    val filteredEntries = remember(feedEntries, inspirationSearchFilter) {
                        feedEntries.filter { entry ->
                            matchesColorCardInspirationFilter(entry, inspirationSearchFilter)
                        }
                    }

                    val inspirationSearchBar: @Composable () -> Unit = {
                        ColorCardInspirationSearchBar(
                            active = inspirationSearchMode,
                            filter = inspirationSearchFilter,
                            onActivate = {
                                inspirationSearchMode = true
                            },
                            onQueryChange = { inspirationSearchQuery = it },
                            onClose = {
                                clearInspirationSearchMode()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )
                    }
                    if (inspirationSearchMode) {
                        ColorCardInspirationFilterResultsPage(
                            filter = inspirationSearchFilter,
                            topics = filterTopics,
                            filteredEntries = filteredEntries,
                            listState = inspirationResultListState,
                            favoritePalettesByKey = favoritePalettesByKey,
                            onPrimaryColorToggle = { primaryColor ->
                                selectedInspirationPrimaryColorName = if (
                                    inspirationSearchFilter.primaryColor == primaryColor
                                ) {
                                    null
                                } else {
                                    primaryColor.name
                                }
                            },
                            onTopicToggle = { topicTag ->
                                selectedInspirationTopicTag = if (
                                    inspirationSearchFilter.topicTag == topicTag
                                ) {
                                    null
                                } else {
                                    topicTag
                                }
                            },
                            onTopJournalClick = { selectedDetail = ColorCardDetailTarget.TopJournal(it) },
                            onNiceClick = { selectedDetail = ColorCardDetailTarget.Nice(it) },
                            onNordClick = { selectedDetail = ColorCardDetailTarget.Nord(it) },
                            onPaletteerClick = { selectedDetail = ColorCardDetailTarget.Paletteer(it) },
                            onChineseEntryClick = { selectedDetail = ColorCardDetailTarget.Inspiration(it) },
                            onGradientClick = { selectedDetail = ColorCardDetailTarget.Gradient(it) },
                            onFavoriteToggle = { favoriteSpec ->
                                val existing = favoritePalettesByKey[favoriteSpec.key]
                                if (existing != null) {
                                    pendingPaletteAction = ColorCardPendingPaletteAction.Unfavorite(existing)
                                } else {
                                    store.savePaletteBlocking(
                                        colors = favoriteSpec.colors,
                                        sourceMediaId = null,
                                        title = favoriteSpec.title,
                                        sourceTag = favoriteSpec.sourceTag,
                                        useChineseFont = favoriteSpec.useChineseFont,
                                        favoriteKey = favoriteSpec.key,
                                        gradientSpec = favoriteSpec.gradientSpec
                                    )
                                }
                            },
                            searchBar = inspirationSearchBar,
                            topContent = libraryTopContent,
                            lastAutoScrollSignature = inspirationFilterLastAutoScrollSignature,
                            onLastAutoScrollSignatureChange = { signature ->
                                inspirationFilterLastAutoScrollSignature = signature
                            },
                            modifier = Modifier.fillMaxSize(),
                            bottomContentPadding = bottomContentPadding
                        )
                    } else {
                        ColorCardInspirationList(
                            listState = inspirationListState,
                            feedEntries = feedEntries,
                            isRefreshing = inspirationRefreshing,
                            onRefresh = {
                                if (!inspirationRefreshing) {
                                    scope.launch {
                                        val refreshStartedAt = System.currentTimeMillis()
                                        inspirationRefreshing = true
                                        delay(COLOR_CARD_REFRESH_FEED_RESET_DELAY_MS)
                                        inspirationListResetToken += 1
                                        inspirationShuffleSeed = nextColorCardInspirationShuffleSeed()
                                        val elapsed = System.currentTimeMillis() - refreshStartedAt
                                        val remainingDelay =
                                            (COLOR_CARD_REFRESH_MIN_DURATION_MS - elapsed).coerceAtLeast(0L)
                                        delay(remainingDelay)
                                        inspirationRefreshing = false
                                    }
                                }
                            },
                            onTopJournalClick = { selectedDetail = ColorCardDetailTarget.TopJournal(it) },
                            onNiceClick = { selectedDetail = ColorCardDetailTarget.Nice(it) },
                            onNordClick = { selectedDetail = ColorCardDetailTarget.Nord(it) },
                            onPaletteerClick = { selectedDetail = ColorCardDetailTarget.Paletteer(it) },
                            onChineseEntryClick = { selectedDetail = ColorCardDetailTarget.Inspiration(it) },
                            onGradientClick = { selectedDetail = ColorCardDetailTarget.Gradient(it) },
                            favoritePalettesByKey = favoritePalettesByKey,
                            onFavoriteToggle = { favoriteSpec ->
                                val existing = favoritePalettesByKey[favoriteSpec.key]
                                if (existing != null) {
                                    pendingPaletteAction = ColorCardPendingPaletteAction.Unfavorite(existing)
                                } else {
                                    store.savePaletteBlocking(
                                        colors = favoriteSpec.colors,
                                        sourceMediaId = null,
                                        title = favoriteSpec.title,
                                        sourceTag = favoriteSpec.sourceTag,
                                        useChineseFont = favoriteSpec.useChineseFont,
                                        favoriteKey = favoriteSpec.key,
                                        gradientSpec = favoriteSpec.gradientSpec
                                    )
                                }
                            },
                            searchBar = inspirationSearchBar,
                            topContent = libraryTopContent,
                            modifier = Modifier.fillMaxSize(),
                            bottomContentPadding = bottomContentPadding
                        )
                    }
                }
            }
            ColorCardTab.Filter -> Unit
        }
    }
    val tryFilterPalette: (String) -> Unit = { paletteId ->
        pendingImportedFilterPaletteId = paletteId
        imagePickerLauncher.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        )
    }
    val modernPromptFeedState = rememberColorCardPromptFeedState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (modernEmbeddedStyle) {
            val modernFilterFeedState = rememberColorCardFilterFeedModernUiState()
            val modernWatermarkFeedState = rememberWatermarkModernPresetTabState()
            val modernRootFeedPagerState = rememberPagerState(
                initialPage = modernRootFeedModes.indexOf(modernRootFeedMode).coerceAtLeast(0),
                pageCount = { modernRootFeedModes.size }
            )
            LaunchedEffect(modernRootFeedMode, modernRootFeedModes) {
                val targetPage = modernRootFeedModes.indexOf(modernRootFeedMode).coerceAtLeast(0)
                if (modernRootFeedPagerState.currentPage != targetPage) {
                    modernRootFeedPagerState.animateScrollToPage(targetPage)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val topBarIsDark = isSystemInDarkTheme()
                    val topBarDividerAlpha by animateFloatAsState(
                        targetValue = if (modernHeaderCollapsed) 1f else 0f,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        label = "modern_top_bar_divider_alpha"
                    )
                    val topBarDividerBaseAlpha = if (topBarIsDark) {
                        0.28f
                    } else {
                        0.16f
                    }
                    val topBarDividerColor = MaterialTheme.colorScheme.outline
                    val topBarModifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (topBarIsDark) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
                            }
                        )

                    Column(modifier = topBarModifier) {
                        ModernColorCardPageHeader(
                            title = localizedModernColorPageTitle(appLanguage),
                            primaryActionLabel = colorCardStrings.mineEyebrowFallback,
                            primaryActionIconRes = R.drawable.colorcard_entry_palette,
                            secondaryActionLabel = modernChooseImageLabel(appLanguage, colorCardStrings),
                            secondaryActionIcon = ColorCardAddPhotoIcon,
                            tertiaryActionLabel = modernCameraLabel(appLanguage, appStrings),
                            tertiaryActionIcon = ColorCardCameraIcon,
                            quaternaryActionLabel = modernStampCameraLabel(appLanguage),
                            quaternaryActionIconRes = R.drawable.colorcard_entry_stamp,
                            albumActionLabel = modernElectronicAlbumLabel(appLanguage),
                            albumActionIconRes = R.drawable.colorcard_entry_album,
                            promptManagerActionLabel = modernPromptManagerLabel(appLanguage),
                            promptManagerActionIconRes = R.drawable.colorcard_entry_prompt_manager,
                            promptManagerActionVisible = supportsModernColorCardPromptTab(appLanguage),
                            fillLightActionLabel = modernFillLightLabel(appLanguage),
                            fillLightActionIconRes = R.drawable.colorcard_entry_fill_light,
                            topLabel = colorCardModernTopLabel(appLanguage),
                            collapsed = modernHeaderCollapsed,
                            primaryActionEnabled = true,
                            secondaryActionEnabled = !importingImage,
                            tertiaryActionEnabled = !importingImage,
                            quaternaryActionEnabled = true,
                            albumActionEnabled = true,
                            promptManagerActionEnabled = supportsModernColorCardPromptTab(appLanguage),
                            fillLightActionEnabled = true,
                            onPrimaryAction = {
                                performHaptic()
                                openColorCardSubpage()
                            },
                            onSecondaryAction = {
                                performHaptic()
                                launchImagePicker(ColorCardTracePanelMode.Filter)
                            },
                            onTertiaryAction = {
                                performHaptic()
                                onOpenCamera()
                            },
                            onQuaternaryAction = {
                                performHaptic()
                                onOpenStampCamera()
                            },
                            onAlbumAction = {
                                performHaptic()
                                onOpenElectronicAlbum()
                            },
                            onPromptManagerAction = {
                                if (supportsModernColorCardPromptTab(appLanguage)) {
                                    performHaptic()
                                    onOpenPromptManager()
                                }
                            },
                            onFillLightAction = {
                                performHaptic()
                                onOpenFillLight()
                            },
                            onScrollToTop = { scrollCurrentTabToTop() }
                        )
                        AnimatedVisibility(
                            visible = !modernHeaderCollapsed,
                            enter = expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = spring(
                                    dampingRatio = 0.92f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            ),
                            exit = shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = spring(
                                    dampingRatio = 0.94f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                            )
                        ) {
                            ModernColorCardRootFeedTabs(
                                selectedMode = modernRootFeedMode,
                                modes = modernRootFeedModes,
                                onModeSelected = { mode ->
                                    performHaptic()
                                    modernRootFeedModeIndex = mode.ordinal
                                },
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 8.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    topBarDividerColor.copy(alpha = topBarDividerBaseAlpha * topBarDividerAlpha)
                                )
                        )
                    }
                    HorizontalPager(
                        state = modernRootFeedPagerState,
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (modernRootFeedModes[page]) {
                            ModernColorCardRootFeedMode.Inspiration -> {
                                ModernColorCardInspirationFeed(
                                    listState = modernRootInspirationListState,
                                    filterState = modernFilterFeedState,
                                    watermarkState = modernWatermarkFeedState,
                                    shuffleSeed = modernRootFeedShuffleSeed,
                                    entryResetSignal = entryResetSignal,
                                    onTryFilter = tryFilterPalette,
                                    onOpenWatermarkPreset = onOpenWatermarkPreset,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            ModernColorCardRootFeedMode.Watermark -> {
                                ModernColorCardWatermarkOnlyFeed(
                                    listState = modernRootWatermarkListState,
                                    state = modernWatermarkFeedState,
                                    onOpenWatermarkPreset = onOpenWatermarkPreset,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            ModernColorCardRootFeedMode.Prompt -> {
                                ModernColorCardPromptOnlyFeed(
                                    listState = modernRootPromptListState,
                                    state = modernPromptFeedState,
                                    onPromptSelected = { selectedPromptEntry = it },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            ModernColorCardRootFeedMode.Filter -> {
                                ModernColorCardFilterOnlyFeed(
                                    listState = filterListState,
                                    state = modernFilterFeedState,
                                    onTryFilter = tryFilterPalette,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                PageAnimatedVisibility(
                    visible = selectedTab != ColorCardTab.Filter,
                    enter = FluidTransitions.SheetEnter,
                    exit = FluidTransitions.SheetExit,
                    restartContentOnOpen = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        UnifiedSubpageTopBar(
                            title = colorCardStrings.mineEyebrowFallback,
                            onBack = handleInternalBack,
                            onTitleClick = { scrollCurrentTabToTop() }
                        )
                        renderColorCardLibraryPages()
                    }
                }
            }
        } else {
            UnifiedSubpageTopBar(
                title = colorCardStrings.pageTitle,
                onBack = onBack,
                onTitleClick = { scrollCurrentTabToTop() },
                trailing = {
                    UnifiedSubpageTopBarIconButton(
                        imageVector = ColorCardCameraIcon,
                        contentDescription = appStrings.camera,
                        onClick = onOpenCamera,
                        enabled = !importingImage
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    UnifiedSubpageTopBarIconButton(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = colorCardStrings.pickFromAlbum,
                        onClick = { launchImagePicker(ColorCardTracePanelMode.Picker) },
                        enabled = !importingImage
                    )
                }
            )

            ColorCardTabSlider(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    if (it != ColorCardTab.Inspiration) {
                        clearInspirationSearchMode()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp),
                compact = false
            )

            when (selectedTab) {
                ColorCardTab.Filter -> {
                    ColorCardFilterFeedTab(
                        onTryFilter = tryFilterPalette,
                        listState = filterListState,
                        onFeedRefreshReset = {
                            filterListResetToken += 1
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomContentPadding)
                    )
                }
                else -> renderColorCardLibraryPages()
            }
        }
    }

    importedImageSource?.let { imported ->
        ColorCardImportedImagePickerOverlay(
            source = imported,
            onDismissRequest = dismissImportedImagePicker,
            onReplaceSource = { replacement ->
                importedImageSource?.let(::cleanupColorCardImportedImageSource)
                importedImageSource = replacement.copy(
                    initialPanelMode = imported.initialPanelMode,
                    initialPaletteId = imported.initialPaletteId
                )
            }
        )
    }

    PageAnimatedVisibility(
        visible = selectedDetail != null,
        enter = FluidTransitions.ParallaxPushEnter,
        exit = FluidTransitions.ParallaxPopExit,
        restartContentOnOpen = false
    ) {
        detailTransitionTarget?.let { detail ->
            ColorCardDetailPage(
                target = detail,
                onBack = { selectedDetail = null }
            )
        }
    }

    PageAnimatedVisibility(
        visible = selectedPromptEntry != null,
        enter = FluidTransitions.ParallaxPushEnter,
        exit = FluidTransitions.ParallaxPopExit,
        restartContentOnOpen = false
    ) {
        selectedPromptEntry?.let { promptEntry ->
            ColorCardPromptDetailPage(
                entry = promptEntry,
                onBack = { selectedPromptEntry = null },
                initialPreviewBitmap = modernPromptFeedState.previewBitmaps[promptEntry.id]
            )
        }
    }
}

private suspend fun loadColorCardInspirationCatalog(context: Context): ColorCardInspirationCatalog = coroutineScope {
    val appContext = context.applicationContext
    val topJournalEntries = async(Dispatchers.IO) {
        loadColorCardTopJournalInspiration(appContext)
    }
    val niceEntries = async(Dispatchers.IO) {
        loadColorCardNiceInspiration(appContext)
    }
    val nordEntries = async(Dispatchers.Default) {
        loadColorCardNordInspiration()
    }
    val paletteerEntries = async(Dispatchers.IO) {
        loadColorCardPaletteerInspiration(appContext)
    }
    val rootTopicEntries = async(Dispatchers.IO) {
        loadColorCardRootTopicInspiration(appContext)
    }
    val chineseEntries = async(Dispatchers.IO) {
        loadColorCardChineseInspiration(appContext)
            .sortedWith(compareBy<ColorCardChineseInspirationEntry> { it.page }.thenBy { it.name })
    }
    val gradientEntries = async(Dispatchers.IO) {
        loadColorCardGradientInspiration(appContext)
    }

    ColorCardInspirationCatalog(
        topJournalEntries = topJournalEntries.await()
            .filter { it.colors.size in 1..COLOR_CARD_MAX_VISIBLE_COLORS },
        niceEntries = niceEntries.await()
            .filter { it.colors.size in 1..COLOR_CARD_MAX_VISIBLE_COLORS },
        nordEntries = nordEntries.await()
            .filter { it.colors.size in 1..COLOR_CARD_MAX_VISIBLE_COLORS },
        paletteerEntries = (
            paletteerEntries.await() +
                rootTopicEntries.await().filterNot { it.collectionName == COLOR_CARD_CHINESE_TRADITIONAL_TAG }
            )
            .filter { it.colors.size in 1..COLOR_CARD_MAX_VISIBLE_COLORS },
        chineseEntries = chineseEntries.await(),
        gradientEntries = gradientEntries.await()
            .filter { it.colors.size in 1..COLOR_CARD_MAX_VISIBLE_COLORS }
    )
}

private val ColorCardCameraIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ColorCardCameraIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes("M7.72 2.79L5.5 5.75C5.26393 6.06475 4.89344 6.25 4.5 6.25H4C2.48122 6.25 1.25 7.48122 1.25 9V19C1.25 20.5188 2.48122 21.75 4 21.75H20C21.5188 21.75 22.75 20.5188 22.75 19V9C22.75 7.48122 21.5188 6.25 20 6.25H19.5C19.1066 6.25 18.7361 6.06474 18.5 5.75L16.28 2.79C16.0251 2.45007 15.625 2.25 15.2 2.25H8.8C8.37508 2.25 7.97495 2.45007 7.72 2.79ZM12 8.25C9.37664 8.25 7.25 10.3767 7.25 13C7.25 15.6233 9.37664 17.75 12 17.75C14.6233 17.75 16.75 15.6233 16.75 13C16.75 10.3767 14.6233 8.25 12 8.25Z"),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        )
    }.build()
}

private val ColorCardAddPhotoIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ColorCardAddPhotoIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes("M3.6 2.25C2.85442 2.25 2.25 2.85441 2.25 3.6V20.4C2.25 21.1456 2.85441 21.75 3.6 21.75H20.4C21.1456 21.75 21.75 21.1456 21.75 20.4V3.6C21.75 2.85442 21.1456 2.25 20.4 2.25H3.6ZM12.75 9C12.75 8.58579 12.4142 8.25 12 8.25C11.5858 8.25 11.25 8.58579 11.25 9V11.25H9C8.58579 11.25 8.25 11.5858 8.25 12C8.25 12.4142 8.58579 12.75 9 12.75H11.25V15C11.25 15.4142 11.5858 15.75 12 15.75C12.4142 15.75 12.75 15.4142 12.75 15V12.75H15C15.4142 12.75 15.75 12.4142 15.75 12C15.75 11.5858 15.4142 11.25 15 11.25H12.75V9Z"),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        )
    }.build()
}

private val StampCameraIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "StampCameraIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes("M21 16.5a2.25 2.25 0 0 1 .843-1.756a3.76 3.76 0 0 0 1.407-2.935a3 3 0 0 0-1.285-2.462a2.25 2.25 0 0 1 .227-3.833A2 2 0 0 0 23.25 3.75a3 3 0 0 0-3-3a2 2 0 0 0-1.764 1.058a2.25 2.25 0 0 1-3.742.349A3.76 3.76 0 0 0 11.809.75a3 3 0 0 0-2.462 1.285a2.25 2.25 0 0 1-3.833-.227A2 2 0 0 0 3.75.75a3 3 0 0 0-3 3a2 2 0 0 0 1.058 1.764a2.25 2.25 0 0 1 .349 3.742A3.76 3.76 0 0 0 .75 12.191a3 3 0 0 0 1.285 2.462a2.25 2.25 0 0 1-.227 3.833A2 2 0 0 0 .75 20.25a3 3 0 0 0 3 3a2 2 0 0 0 1.764-1.058a2.25 2.25 0 0 1 3.742-.349a3.76 3.76 0 0 0 2.935 1.407a3 3 0 0 0 2.462-1.285a2.25 2.25 0 0 1 3.833.227a2 2 0 0 0 1.764 1.058a3 3 0 0 0 3-3a2 2 0 0 0-1.058-1.764A2.25 2.25 0 0 1 21 16.5"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color(0xFFBFBFBF)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
        addPath(
            pathData = addPathNodes("M6.75 6.75h10.5v10.5H6.75z"),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color(0xFFBFBFBF)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        )
    }.build()
}

@Composable
private fun ModernColorCardPageHeader(
    title: String,
    primaryActionLabel: String,
    primaryActionIconRes: Int,
    secondaryActionLabel: String,
    secondaryActionIcon: ImageVector,
    tertiaryActionLabel: String,
    tertiaryActionIcon: ImageVector,
    quaternaryActionLabel: String,
    quaternaryActionIconRes: Int,
    albumActionLabel: String,
    albumActionIconRes: Int,
    promptManagerActionLabel: String,
    promptManagerActionIconRes: Int,
    promptManagerActionVisible: Boolean,
    fillLightActionLabel: String,
    fillLightActionIconRes: Int,
    topLabel: String,
    collapsed: Boolean,
    primaryActionEnabled: Boolean,
    secondaryActionEnabled: Boolean,
    tertiaryActionEnabled: Boolean,
    quaternaryActionEnabled: Boolean,
    albumActionEnabled: Boolean,
    promptManagerActionEnabled: Boolean,
    fillLightActionEnabled: Boolean,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onTertiaryAction: () -> Unit,
    onQuaternaryAction: () -> Unit,
    onAlbumAction: () -> Unit,
    onPromptManagerAction: () -> Unit,
    onFillLightAction: () -> Unit,
    onScrollToTop: () -> Unit
) {
    val spacingAnimationSpec = spring<Dp>(
        dampingRatio = 0.92f,
        stiffness = Spring.StiffnessMediumLow
    )
    val sizeAnimationSpec = spring<IntSize>(
        dampingRatio = 0.90f,
        stiffness = Spring.StiffnessLow
    )
    val actionSpacing by animateDpAsState(
        targetValue = if (collapsed) 8.dp else 10.dp,
        animationSpec = spacingAnimationSpec,
        label = "action_spacing"
    )
    val topPadding by animateDpAsState(
        targetValue = if (collapsed) 10.dp else 16.dp,
        animationSpec = spacingAnimationSpec,
        label = "top_padding"
    )
    val bottomPadding by animateDpAsState(
        targetValue = if (collapsed) 10.dp else 14.dp,
        animationSpec = spacingAnimationSpec,
        label = "bottom_padding"
    )
    val verticalArrangementSpacing by animateDpAsState(
        targetValue = if (collapsed) 8.dp else 14.dp,
        animationSpec = spacingAnimationSpec,
        label = "vertical_spacing"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = 18.dp,
                top = topPadding,
                end = 18.dp,
                bottom = bottomPadding
            ),
        verticalArrangement = Arrangement.spacedBy(verticalArrangementSpacing)
    ) {
        AnimatedVisibility(
            visible = !collapsed,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = spring(
                    dampingRatio = 0.92f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
            ),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = spring(
                    dampingRatio = 0.96f,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
            )
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(actionSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(
                        animationSpec = sizeAnimationSpec
                    ),
                verticalArrangement = Arrangement.spacedBy(actionSpacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernColorCardActionButton(
                        label = secondaryActionLabel,
                        icon = secondaryActionIcon,
                        onClick = onSecondaryAction,
                        enabled = secondaryActionEnabled,
                        compact = true,
                        compactHeight = if (collapsed) 50.dp else 74.dp,
                        modifier = Modifier.weight(1f)
                    )
                    ModernColorCardActionButton(
                        label = tertiaryActionLabel,
                        icon = tertiaryActionIcon,
                        onClick = onTertiaryAction,
                        enabled = tertiaryActionEnabled,
                        compact = true,
                        compactHeight = if (collapsed) 50.dp else 74.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
                AnimatedVisibility(
                    visible = !collapsed,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = spring(
                            dampingRatio = 0.92f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = spring(
                            dampingRatio = 0.96f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                    )
                ) {
                    val miniActions = buildList {
                        add(
                            ModernColorCardMiniActionSpec(
                                label = primaryActionLabel,
                                iconRes = primaryActionIconRes,
                                iconSize = 36.dp,
                                enabled = primaryActionEnabled,
                                onClick = onPrimaryAction
                            )
                        )
                        add(
                            ModernColorCardMiniActionSpec(
                                label = quaternaryActionLabel,
                                iconRes = quaternaryActionIconRes,
                                iconSize = 36.dp,
                                enabled = quaternaryActionEnabled,
                                onClick = onQuaternaryAction
                            )
                        )
                        add(
                            ModernColorCardMiniActionSpec(
                                label = albumActionLabel,
                                iconRes = albumActionIconRes,
                                enabled = albumActionEnabled,
                                onClick = onAlbumAction
                            )
                        )
                        if (promptManagerActionVisible) {
                            add(
                                ModernColorCardMiniActionSpec(
                                    label = promptManagerActionLabel,
                                    iconRes = promptManagerActionIconRes,
                                    enabled = promptManagerActionEnabled,
                                    onClick = onPromptManagerAction
                                )
                            )
                        }
                        add(
                            ModernColorCardMiniActionSpec(
                                label = fillLightActionLabel,
                                iconRes = fillLightActionIconRes,
                                iconSize = 34.dp,
                                enabled = fillLightActionEnabled,
                                onClick = onFillLightAction
                            )
                        )
                    }
                    ModernColorCardMiniActionStrip(
                        modifier = Modifier.fillMaxWidth(),
                        actionSpacing = actionSpacing,
                        actions = miniActions
                    )
                }
            }
            AnimatedVisibility(
                visible = collapsed,
                enter = expandHorizontally(
                    expandFrom = Alignment.End,
                    animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                ),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.End,
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing)
                )
            ) {
                ModernColorCardTopIconButton(
                    contentDescription = topLabel,
                    onClick = onScrollToTop,
                    modifier = Modifier.size(50.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernColorCardActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    compact: Boolean = false,
    compactHeight: Dp = 74.dp,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
    }
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.82f else 0.36f)
    val borderAlpha = when {
        !enabled && isDark -> 0.16f
        !enabled -> 0.07f
        isDark -> 0.24f
        else -> 0.08f
    }
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = borderAlpha)
    val shape = RoundedCornerShape(18.dp)
    val buttonModifier = modifier
        .height(if (compact) compactHeight else 84.dp)
        .clip(shape)
        .background(containerColor)
        .border(
            width = if (compact) 1.35.dp else 1.dp,
            color = borderColor,
            shape = shape
        )
        .clickable(enabled = enabled, onClick = onClick)
    if (compact) {
        Row(
            modifier = buttonModifier
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(25.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = if (compactHeight <= 52.dp) 15.sp else 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (compactHeight <= 52.dp) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    Column(
        modifier = buttonModifier
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private data class ModernColorCardMiniActionSpec(
    val label: String,
    val iconRes: Int,
    val iconSize: Dp = 40.dp,
    val enabled: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun ModernColorCardMiniActionStrip(
    actions: List<ModernColorCardMiniActionSpec>,
    actionSpacing: Dp,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val indicatorTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val indicatorColor = Color(0xFFFF2D55)
    val indicatorProgress by remember {
        derivedStateOf {
            val max = scrollState.maxValue
            if (max <= 0) 0f else scrollState.value.toFloat() / max.toFloat()
        }
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(actionSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                ModernColorCardMiniActionButton(
                    label = action.label,
                    iconRes = action.iconRes,
                    iconSize = action.iconSize,
                    onClick = action.onClick,
                    enabled = action.enabled,
                    modifier = Modifier.width(76.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(indicatorTrackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .offset(x = (24.dp * indicatorProgress))
                    .clip(RoundedCornerShape(999.dp))
                    .background(indicatorColor)
            )
        }
    }
}

@Composable
private fun ModernColorCardMiniActionButton(
    label: String,
    iconRes: Int,
    iconSize: Dp = 56.dp,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.80f else 0.34f)
    val buttonModifier = modifier
        .height(88.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable(
            enabled = enabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )

    Column(
        modifier = buttonModifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = if (enabled) 1f else 0.36f
                    }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        )
    }
}

@Composable
private fun ModernColorCardTopIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowUpward,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun ModernColorCardRootFeedTabs(
    selectedMode: ModernColorCardRootFeedMode,
    modes: List<ModernColorCardRootFeedMode>,
    onModeSelected: (ModernColorCardRootFeedMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage = LocalAppLanguage.current
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(top = 2.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { mode ->
            val selected = mode == selectedMode
            val interactionSource = remember(mode) { MutableInteractionSource() }
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.84f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "modern_color_root_feed_scale_${mode.name}"
            )
            val color by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                },
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "modern_color_root_feed_color_${mode.name}"
            )

            Text(
                text = modernRootFeedModeDisplayLabel(appLanguage, mode),
                color = color,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onModeSelected(mode) }
                    )
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ModernColorCardInspirationFeed(
    listState: LazyListState,
    filterState: ColorCardFilterFeedModernUiState,
    watermarkState: WatermarkModernPresetTabState,
    shuffleSeed: Long,
    entryResetSignal: Int,
    onTryFilter: (String) -> Unit,
    onOpenWatermarkPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // The mixed inspiration feed combines independently loaded section sources.
    // Rendering a partial list first lets LazyColumn preserve a keyed anchor when
    // later sections are inserted above it, which makes the page appear to open
    // in the middle. Wait for both sources, then keep the entry pinned to the
    // absolute top until the user actually starts scrolling.
    val mixedFeedReady =
        filterState.series.isNotEmpty() &&
            watermarkState.series.isNotEmpty()
    if (!mixedFeedReady) {
        ColorCardFilterFeedLoadingState(modifier = modifier)
        return
    }

    val sections = remember(filterState.series, watermarkState.series, shuffleSeed) {
        buildModernColorCardRootSections(
            filterSeries = filterState.series,
            watermarkSeries = watermarkState.series,
            shuffleSeed = shuffleSeed
        )
    }
    val sectionOrderSignature = remember(sections) {
        sections.joinToString(separator = "\u0001") { it.key }
    }
    var initialTopLockReleased by remember(entryResetSignal) { mutableStateOf(false) }

    LaunchedEffect(listState, entryResetSignal) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    initialTopLockReleased = true
                }
            }
    }
    LaunchedEffect(entryResetSignal, sectionOrderSignature, initialTopLockReleased) {
        if (!initialTopLockReleased) {
            listState.scrollToItem(0)
        }
    }

    if (sections.isEmpty()) {
        ColorCardFilterFeedLoadingState(modifier = modifier)
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(sections, key = { it.key }) { section ->
            when (section.source) {
                ModernColorCardRootSectionSource.Filter -> {
                    section.filterSeries?.let { series ->
                        ModernColorCardTaggedFilterSectionRow(
                            series = series,
                            previewBitmaps = filterState.previewBitmaps,
                            onTryFilter = onTryFilter
                        )
                    }
                }
                ModernColorCardRootSectionSource.Watermark -> {
                    section.watermarkSeries?.let { series ->
                        ModernColorCardTaggedWatermarkSectionRow(
                            series = series,
                            previewBitmaps = watermarkState.previewBitmaps,
                            onOpenWatermarkPreset = onOpenWatermarkPreset
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernColorCardWatermarkOnlyFeed(
    listState: LazyListState,
    state: WatermarkModernPresetTabState,
    onOpenWatermarkPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(state.series, key = { it.key }) { series ->
            WatermarkModernPresetSeriesRow(
                series = series,
                language = LocalAppLanguage.current,
                previewBitmaps = state.previewBitmaps,
                onPresetSelected = onOpenWatermarkPreset
            )
        }
    }
}

@Composable
private fun ModernColorCardFilterOnlyFeed(
    listState: LazyListState,
    state: ColorCardFilterFeedModernUiState,
    onTryFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.series.isEmpty()) {
        ColorCardFilterFeedLoadingState(modifier = modifier)
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(state.series, key = { it.key }) { series ->
            ColorCardFilterFeedModernSeriesRow(
                series = series,
                previewBitmaps = state.previewBitmaps,
                onTryFilter = onTryFilter
            )
        }
    }
}

@Composable
private fun ModernColorCardTaggedFilterSectionRow(
    series: ColorCardFilterFeedModernSeries,
    previewBitmaps: Map<String, Bitmap>,
    onTryFilter: (String) -> Unit
) {
    val appLanguage = LocalAppLanguage.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModernColorCardSectionHeading(
            title = colorCardFilterFeedModernSeriesHeadlineWithEmoji(
                appLanguage = appLanguage,
                sectionKey = series.sectionKey
            ),
            tagText = localizedModernColorCardRootSectionTag(appLanguage, ModernColorCardRootSectionSource.Filter),
            source = ModernColorCardRootSectionSource.Filter
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(series.entries, key = { it.entry.key }) { seriesEntry ->
                val entry = seriesEntry.entry
                val previewKey = buildColorCardFilterFeedPreviewKey(
                    candidate = entry.candidate,
                    demoAssetName = entry.demoAssetName
                )
                ColorCardFilterFeedModernPresetCard(
                    entry = entry,
                    previewBitmap = previewBitmaps[previewKey],
                    onClick = { onTryFilter(entry.candidate.paletteId) }
                )
            }
        }
    }
}

@Composable
private fun ModernColorCardTaggedWatermarkSectionRow(
    series: WatermarkModernPresetSeries,
    previewBitmaps: Map<String, Bitmap>,
    onOpenWatermarkPreset: (String) -> Unit
) {
    val appLanguage = LocalAppLanguage.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModernColorCardSectionHeading(
            title = watermarkModernSeriesHeadline(series, appLanguage),
            tagText = localizedModernColorCardRootSectionTag(appLanguage, ModernColorCardRootSectionSource.Watermark),
            source = ModernColorCardRootSectionSource.Watermark
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(series.presets, key = { it.key }) { preset ->
                WatermarkModernPresetCard(
                    preset = preset,
                    language = appLanguage,
                    previewBitmap = previewBitmaps[preset.key],
                    onClick = { onOpenWatermarkPreset(preset.key) }
                )
            }
        }
    }
}

@Composable
private fun ModernColorCardSectionHeading(
    title: String,
    tagText: String,
    source: ModernColorCardRootSectionSource,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dividerGap = 8.dp
    val tagColor = when (source) {
        ModernColorCardRootSectionSource.Watermark -> {
            if (isDark) Color(0xFFC8B9E6) else Color(0xFF7C6AA3)
        }
        ModernColorCardRootSectionSource.Filter -> {
            if (isDark) Color(0xFFB8D5B0) else Color(0xFF5F8662)
        }
    }
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(18.dp)
                    .background(tagColor.copy(alpha = 0.92f))
            )
            Spacer(modifier = Modifier.width(dividerGap))
            Text(
                text = tagText,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = tagColor
            )
        }
    }
}

private fun buildModernColorCardRootSections(
    filterSeries: List<ColorCardFilterFeedModernSeries>,
    watermarkSeries: List<WatermarkModernPresetSeries>,
    shuffleSeed: Long
): List<ModernColorCardRootSection> {
    if (filterSeries.isEmpty() && watermarkSeries.isEmpty()) return emptyList()
    val random = Random(shuffleSeed)
    return (
        filterSeries.map { series ->
            ModernColorCardRootSection(
                key = "filter:${series.key}",
                source = ModernColorCardRootSectionSource.Filter,
                filterSeries = series
            )
        } +
            watermarkSeries.map { series ->
                ModernColorCardRootSection(
                    key = "watermark:${series.key}",
                    source = ModernColorCardRootSectionSource.Watermark,
                    watermarkSeries = series
                )
            }
    ).shuffled(random)
}

private fun modernColorCardRootFeedModeLabel(
    language: AppLanguage,
    mode: ModernColorCardRootFeedMode
): String = when (mode) {
    ModernColorCardRootFeedMode.Inspiration -> when (language) {
        AppLanguage.SimplifiedChinese -> "\u7075\u611f"
        AppLanguage.TraditionalChinese -> "\u9748\u611f"
        else -> "Inspiration"
    }
    ModernColorCardRootFeedMode.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese -> "水印边框"
        AppLanguage.TraditionalChinese -> "水印邊框"
        else -> "Watermark"
    }
    ModernColorCardRootFeedMode.Filter -> when (language) {
        AppLanguage.SimplifiedChinese -> "滤镜"
        AppLanguage.TraditionalChinese -> "濾鏡"
        else -> "Filter"
    }
    ModernColorCardRootFeedMode.Prompt -> when (language) {
        AppLanguage.SimplifiedChinese -> "\u63d0\u793a\u8bcd"
        AppLanguage.TraditionalChinese -> "\u63d0\u793a\u8a5e"
        else -> "Prompt"
    }
}

private fun modernColorCardRootSectionTag(
    language: AppLanguage,
    source: ModernColorCardRootSectionSource
): String = when (source) {
    ModernColorCardRootSectionSource.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese -> "水印边框"
        AppLanguage.TraditionalChinese -> "水印邊框"
        else -> "Watermark"
    }
    ModernColorCardRootSectionSource.Filter -> when (language) {
        AppLanguage.SimplifiedChinese -> "滤镜"
        AppLanguage.TraditionalChinese -> "濾鏡"
        else -> "Filter"
    }
}

private fun localizedModernColorCardRootFeedModeLabel(
    language: AppLanguage,
    mode: ModernColorCardRootFeedMode
): String = when (mode) {
    ModernColorCardRootFeedMode.Inspiration -> when (language) {
        AppLanguage.SimplifiedChinese -> "灵感"
        AppLanguage.TraditionalChinese -> "靈感"
        AppLanguage.Japanese -> "インスピレーション"
        AppLanguage.Korean -> "영감"
        AppLanguage.French -> "Inspiration"
        AppLanguage.German -> "Inspiration"
        AppLanguage.Italian -> "Ispirazione"
        AppLanguage.Spanish -> "Inspiración"
        AppLanguage.Portuguese -> "Inspiração"
        AppLanguage.Russian -> "Вдохновение"
        AppLanguage.Thai -> "แรงบันดาลใจ"
        AppLanguage.Vietnamese -> "Cảm hứng"
        AppLanguage.Indonesian -> "Inspirasi"
        AppLanguage.Arabic -> "إلهام"
        AppLanguage.Ukrainian -> "Натхнення"
        AppLanguage.Polish -> "Inspiracje"
        AppLanguage.Czech -> "Inspirace"
        AppLanguage.Dutch -> "Inspiratie"
        AppLanguage.Swedish -> "Inspiration"
        AppLanguage.Danish -> "Inspiration"
        AppLanguage.Norwegian -> "Inspirasjon"
        AppLanguage.Finnish -> "Inspiraatio"
        AppLanguage.Greek -> "Έμπνευση"
        AppLanguage.Romanian -> "Inspirație"
        AppLanguage.Hungarian -> "Inspiráció"
        AppLanguage.Turkish -> "İlham"
        AppLanguage.Malay -> "Inspirasi"
        AppLanguage.Hebrew -> "השראה"
        AppLanguage.English -> "Inspiration"
    }
    ModernColorCardRootFeedMode.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese -> "水印"
        AppLanguage.TraditionalChinese -> "水印"
        AppLanguage.Japanese -> "透かし"
        AppLanguage.Korean -> "워터마크"
        AppLanguage.French -> "Filigrane"
        AppLanguage.German -> "Wasserzeichen"
        AppLanguage.Italian -> "Filigrana"
        AppLanguage.Spanish -> "Marca de agua"
        AppLanguage.Portuguese -> "Marca d'água"
        AppLanguage.Russian -> "Водяной знак"
        AppLanguage.Thai -> "ลายน้ำ"
        AppLanguage.Vietnamese -> "Hình mờ"
        AppLanguage.Indonesian -> "Tanda air"
        AppLanguage.Arabic -> "علامة مائية"
        AppLanguage.Ukrainian -> "Водяний знак"
        AppLanguage.Polish -> "Znak wodny"
        AppLanguage.Czech -> "Vodoznak"
        AppLanguage.Dutch -> "Watermerk"
        AppLanguage.Swedish -> "Vattenstämpel"
        AppLanguage.Danish -> "Vandmærke"
        AppLanguage.Norwegian -> "Vannmerke"
        AppLanguage.Finnish -> "Vesileima"
        AppLanguage.Greek -> "Υδατογράφημα"
        AppLanguage.Romanian -> "Filigran"
        AppLanguage.Hungarian -> "Vízjel"
        AppLanguage.Turkish -> "Filigran"
        AppLanguage.Malay -> "Tera air"
        AppLanguage.Hebrew -> "סימן מים"
        AppLanguage.English -> "Watermark"
    }
    ModernColorCardRootFeedMode.Filter -> when (language) {
        AppLanguage.SimplifiedChinese -> "滤镜"
        AppLanguage.TraditionalChinese -> "濾鏡"
        AppLanguage.Japanese -> "フィルター"
        AppLanguage.Korean -> "필터"
        AppLanguage.French -> "Filtre"
        AppLanguage.German -> "Filter"
        AppLanguage.Italian -> "Filtro"
        AppLanguage.Spanish -> "Filtro"
        AppLanguage.Portuguese -> "Filtro"
        AppLanguage.Russian -> "Фильтр"
        AppLanguage.Thai -> "ฟิลเตอร์"
        AppLanguage.Vietnamese -> "Bộ lọc"
        AppLanguage.Indonesian -> "Filter"
        AppLanguage.Arabic -> "فلتر"
        AppLanguage.Ukrainian -> "Фільтр"
        AppLanguage.Polish -> "Filtr"
        AppLanguage.Czech -> "Filtr"
        AppLanguage.Dutch -> "Filter"
        AppLanguage.Swedish -> "Filter"
        AppLanguage.Danish -> "Filter"
        AppLanguage.Norwegian -> "Filter"
        AppLanguage.Finnish -> "Suodatin"
        AppLanguage.Greek -> "Φίλτρο"
        AppLanguage.Romanian -> "Filtru"
        AppLanguage.Hungarian -> "Szűrő"
        AppLanguage.Turkish -> "Filtre"
        AppLanguage.Malay -> "Penapis"
        AppLanguage.Hebrew -> "מסנן"
        AppLanguage.English -> "Filter"
    }
    ModernColorCardRootFeedMode.Prompt -> when (language) {
        AppLanguage.SimplifiedChinese -> "\u63d0\u793a\u8bcd"
        AppLanguage.TraditionalChinese -> "\u63d0\u793a\u8a5e"
        AppLanguage.Japanese -> "Prompt"
        AppLanguage.Korean -> "Prompt"
        AppLanguage.French -> "Prompt"
        AppLanguage.German -> "Prompt"
        AppLanguage.Italian -> "Prompt"
        AppLanguage.Spanish -> "Prompt"
        AppLanguage.Portuguese -> "Prompt"
        AppLanguage.Russian -> "Prompt"
        AppLanguage.Thai -> "Prompt"
        AppLanguage.Vietnamese -> "Prompt"
        AppLanguage.Indonesian -> "Prompt"
        AppLanguage.Arabic -> "Prompt"
        AppLanguage.Ukrainian -> "Prompt"
        AppLanguage.Polish -> "Prompt"
        AppLanguage.Czech -> "Prompt"
        AppLanguage.Dutch -> "Prompt"
        AppLanguage.Swedish -> "Prompt"
        AppLanguage.Danish -> "Prompt"
        AppLanguage.Norwegian -> "Prompt"
        AppLanguage.Finnish -> "Prompt"
        AppLanguage.Greek -> "Prompt"
        AppLanguage.Romanian -> "Prompt"
        AppLanguage.Hungarian -> "Prompt"
        AppLanguage.Turkish -> "Prompt"
        AppLanguage.Malay -> "Prompt"
        AppLanguage.Hebrew -> "Prompt"
        AppLanguage.English -> "Prompt"
    }
}

private fun localizedModernColorCardRootSectionTag(
    language: AppLanguage,
    source: ModernColorCardRootSectionSource
): String = when (source) {
    ModernColorCardRootSectionSource.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese -> "\u8fb9\u6846"
        AppLanguage.TraditionalChinese -> "\u908a\u6846"
        else -> "Border"
    }
    ModernColorCardRootSectionSource.Filter -> when (language) {
        AppLanguage.SimplifiedChinese -> "\u6ee4\u955c"
        AppLanguage.TraditionalChinese -> "\u6ffe\u93e1"
        else -> "Filter"
    }
}

private fun localizedModernColorPageTitle(language: AppLanguage): String = getColorCardUiStrings(language).pageTitle

private fun colorCardModernTopLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese,
    AppLanguage.TraditionalChinese -> "\u56de\u9876"
    else -> "Top"
}

private fun modernColorPageTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u521b\u610f"
    AppLanguage.TraditionalChinese -> "\u5275\u610f"
    else -> getColorCardUiStrings(language).pageTitle
}

private fun modernRootFeedModeDisplayLabel(
    language: AppLanguage,
    mode: ModernColorCardRootFeedMode
): String {
    if (mode == ModernColorCardRootFeedMode.Watermark) {
        return when (language) {
            AppLanguage.SimplifiedChinese -> "\u6c34\u5370\u8fb9\u6846"
            AppLanguage.TraditionalChinese -> "\u6c34\u5370\u908a\u6846"
            else -> localizedModernColorCardRootFeedModeLabel(language, mode)
        }
    }
    return localizedModernColorCardRootFeedModeLabel(language, mode)
}

private fun modernChooseImageLabel(
    language: AppLanguage,
    colorCardStrings: ColorCardUiStrings
): String = when (language) {
    AppLanguage.SimplifiedChinese -> "选择图片"
    AppLanguage.TraditionalChinese -> "選擇圖片"
    AppLanguage.Japanese -> "写真"
    AppLanguage.Korean -> "사진"
    AppLanguage.French -> "Image"
    AppLanguage.German -> "Bild"
    AppLanguage.Italian -> "Foto"
    AppLanguage.Spanish -> "Imagen"
    AppLanguage.Portuguese -> "Imagem"
    AppLanguage.Russian -> "Фото"
    AppLanguage.Thai -> "รูปภาพ"
    AppLanguage.Vietnamese -> "Ảnh"
    AppLanguage.Indonesian -> "Foto"
    AppLanguage.Arabic -> "صورة"
    AppLanguage.Ukrainian -> "Фото"
    AppLanguage.Polish -> "Zdjęcie"
    AppLanguage.Czech -> "Obrázek"
    AppLanguage.Dutch -> "Afbeelding"
    AppLanguage.Swedish -> "Bild"
    AppLanguage.Danish -> "Billede"
    AppLanguage.Norwegian -> "Bilde"
    AppLanguage.Finnish -> "Kuva"
    AppLanguage.Greek -> "Εικόνα"
    AppLanguage.Romanian -> "Imagine"
    AppLanguage.Hungarian -> "Kép"
    AppLanguage.Turkish -> "Görsel"
    AppLanguage.Malay -> "Imej"
    AppLanguage.Hebrew -> "תמונה"
    AppLanguage.English -> "Image"
}

private fun modernCameraLabel(
    language: AppLanguage,
    appStrings: LocalizedStrings
): String = when (language) {
    AppLanguage.SimplifiedChinese,
    AppLanguage.TraditionalChinese -> appStrings.camera
    AppLanguage.Japanese -> "カメラ"
    AppLanguage.Korean -> "카메라"
    AppLanguage.French -> "Caméra"
    AppLanguage.German -> "Kamera"
    AppLanguage.Italian -> "Camera"
    AppLanguage.Spanish -> "Cámara"
    AppLanguage.Portuguese -> "Câmera"
    AppLanguage.Russian -> "Камера"
    AppLanguage.Thai -> "กล้อง"
    AppLanguage.Vietnamese -> "Camera"
    AppLanguage.Indonesian -> "Kamera"
    AppLanguage.Arabic -> "كاميرا"
    AppLanguage.Ukrainian -> "Камера"
    AppLanguage.Polish -> "Aparat"
    AppLanguage.Czech -> "Fotoaparát"
    AppLanguage.Dutch -> "Camera"
    AppLanguage.Swedish -> "Kamera"
    AppLanguage.Danish -> "Kamera"
    AppLanguage.Norwegian -> "Kamera"
    AppLanguage.Finnish -> "Kamera"
    AppLanguage.Greek -> "Κάμερα"
    AppLanguage.Romanian -> "Cameră"
    AppLanguage.Hungarian -> "Kamera"
    AppLanguage.Turkish -> "Kamera"
    AppLanguage.Malay -> "Kamera"
    AppLanguage.Hebrew -> "מצלמה"
    AppLanguage.English -> "Camera"
}

private fun modernStampCameraLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u96c6\u90ae"
    AppLanguage.TraditionalChinese -> "\u96c6\u90ae"
    AppLanguage.Japanese -> "スタンプ"
    AppLanguage.Korean -> "스탬프"
    AppLanguage.French -> "Timbre"
    AppLanguage.German -> "Stempel"
    AppLanguage.Italian -> "Timbro"
    AppLanguage.Spanish -> "Sello"
    AppLanguage.Portuguese -> "Selo"
    AppLanguage.Russian -> "Штамп"
    AppLanguage.Thai -> "สแตมป์"
    AppLanguage.Vietnamese -> "Tem"
    AppLanguage.Indonesian -> "Stempel"
    AppLanguage.Arabic -> "طابع"
    AppLanguage.Ukrainian -> "Марка"
    AppLanguage.Polish -> "Znaczek"
    AppLanguage.Czech -> "Známka"
    AppLanguage.Dutch -> "Postzegel"
    AppLanguage.Swedish -> "Frimärke"
    AppLanguage.Danish -> "Frimærke"
    AppLanguage.Norwegian -> "Frimerke"
    AppLanguage.Finnish -> "Postimerkki"
    AppLanguage.Greek -> "Γραμματόσημο"
    AppLanguage.Romanian -> "Timbru"
    AppLanguage.Hungarian -> "Bélyeg"
    AppLanguage.Turkish -> "Pul"
    AppLanguage.Malay -> "Setem"
    AppLanguage.Hebrew -> "בול"
    AppLanguage.English -> "Stamp"
}

private fun modernElectronicAlbumLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7535\u5b50\u76f8\u518c"
    AppLanguage.TraditionalChinese -> "\u96fb\u5b50\u76f8\u7c3f"
    else -> "Album"
}

private fun modernPromptManagerLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u63d0\u793a\u8bcd"
    AppLanguage.TraditionalChinese -> "\u63d0\u793a\u8a5e"
    else -> "Prompts"
}

private fun modernFillLightLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u95ea\u5149\u706f"
    AppLanguage.TraditionalChinese -> "\u88dc\u5149\u71c8"
    else -> "Light"
}

@Composable
private fun ColorCardTabSlider(
    selectedTab: ColorCardTab,
    onTabSelected: (ColorCardTab) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .then(if (compact) Modifier.width(182.dp) else Modifier.fillMaxWidth())
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ColorCardTabLabel(
            title = colorCardStrings.tabMine,
            selected = selectedTab == ColorCardTab.Mine,
            onClick = { onTabSelected(ColorCardTab.Mine) },
            modifier = Modifier.weight(1f)
        )
        ColorCardTabLabel(
            title = colorCardStrings.tabInspiration,
            selected = selectedTab == ColorCardTab.Inspiration,
            onClick = { onTabSelected(ColorCardTab.Inspiration) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ColorCardTabLabel(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MyColorCardList(
    listState: LazyListState,
    palettes: List<SavedColorCardPalette>,
    onPaletteClick: (SavedColorCardPalette) -> Unit,
    onDeleteRequest: (SavedColorCardPalette) -> Unit,
    onFavoriteToggleRequest: (SavedColorCardPalette) -> Unit,
    topContent: (@Composable () -> Unit)? = null,
    bottomContentPadding: Dp = 28.dp
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val isDark = isSystemInDarkTheme()
    val emptySecondary = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
    } else {
        Color(0xFF666666)
    }

    if (palettes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomContentPadding)
        ) {
            if (topContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 4.dp)
                ) {
                    topContent()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = colorCardStrings.myEmptyTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = colorCardStrings.myEmptyBody,
                        color = emptySecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (topContent != null) {
            item(key = "top-content") {
                topContent()
            }
        }
        items(
            items = palettes,
            key = { it.id }
        ) { palette ->
            MyColorCardItem(
                palette = palette,
                onOpen = { onPaletteClick(palette) },
                onShare = {
                    shareColorCardText(
                        context = context,
                        chooserTitle = "\u5bfc\u51fa PHOTOO\u8272\u5361",
                        subject = "PHOTOO\u8272\u5361",
                        text = buildPaletteDetailText(palette)
                    )
                },
                onDelete = { onDeleteRequest(palette) },
                onFavoriteToggle = { onFavoriteToggleRequest(palette) }
            )
        }
    }
}

@Composable
private fun MyColorCardItem(
    palette: SavedColorCardPalette,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    InspirationPaletteCard(
        title = palette.title,
        sourceTag = palette.sourceTag?.takeIf { it.isNotBlank() } ?: getColorCardInspirationCopy(LocalAppLanguage.current).topicMine,
        secondaryActionImageVector = if (palette.isFavorite) {
            Icons.Filled.Favorite
        } else {
            Icons.Outlined.Delete
        },
        secondaryActionContentDescription = if (palette.isFavorite) {
            "\u53d6\u6d88\u6536\u85cf"
        } else {
            "\u5220\u9664\u8272\u5361"
        },
        secondaryActionTint = if (palette.isFavorite) {
            Color(0xFFD35C63)
        } else {
            Color(0xFFD64545)
        },
        onClick = onOpen,
        onShare = onShare,
        onSecondaryAction = if (palette.isFavorite) onFavoriteToggle else onDelete,
        useChineseFont = palette.useChineseFont,
        preview = {
            if (palette.gradientSpec != null) {
                ColorCardGradientPreview(
                    gradientSpec = palette.gradientSpec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    shape = RoundedCornerShape(20.dp),
                    borderColor = inspirationCardBorderColor()
                )
            } else {
                ColorCardPalettePreview(colors = palette.colors)
            }
        }
    )
}

@Composable
private fun ColorCardInspirationLoadingState(
    isLoading: Boolean
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardPageCopy = remember(appLanguage) { getColorCardPageGlobalCopy(appLanguage) }
    val isDark = isSystemInDarkTheme()
    val secondaryText = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
    } else {
        Color(0xFF666666)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onBackground,
                    strokeWidth = 2.4.dp,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = if (isLoading) colorCardPageCopy.inspirationPreparing else colorCardPageCopy.inspirationEmpty,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isLoading) {
                    colorCardPageCopy.inspirationPreparingBody
                } else {
                    colorCardPageCopy.inspirationEmptyBody
                },
                color = secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ColorCardInspirationList(
    listState: LazyListState,
    feedEntries: List<ColorCardInspirationFeedEntry>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTopJournalClick: (ColorCardTopJournalPaletteEntry) -> Unit,
    onNiceClick: (ColorCardNicePaletteEntry) -> Unit,
    onNordClick: (ColorCardNordPaletteEntry) -> Unit,
    onPaletteerClick: (ColorCardPaletteerPaletteEntry) -> Unit,
    onChineseEntryClick: (ColorCardChineseInspirationEntry) -> Unit,
    onGradientClick: (ColorCardGradientInspirationEntry) -> Unit,
    favoritePalettesByKey: Map<String, SavedColorCardPalette>,
    onFavoriteToggle: (ColorCardFavoriteSpec) -> Unit,
    searchBar: @Composable () -> Unit,
    topContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 28.dp
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                color = inspirationRefreshAccentColor()
            )
        }
    ) {
        ColorCardInspirationFeedList(
            listState = listState,
            feedEntries = feedEntries,
            favoritePalettesByKey = favoritePalettesByKey,
            onTopJournalClick = onTopJournalClick,
            onNiceClick = onNiceClick,
            onNordClick = onNordClick,
            onPaletteerClick = onPaletteerClick,
            onChineseEntryClick = onChineseEntryClick,
            onGradientClick = onGradientClick,
            onFavoriteToggle = onFavoriteToggle,
            headerContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topContent?.invoke()
                    searchBar()
                }
            },
            bottomContentPadding = bottomContentPadding
        )
    }
}

@Composable
private fun ColorCardInspirationFeedList(
    listState: LazyListState,
    feedEntries: List<ColorCardInspirationFeedEntry>,
    favoritePalettesByKey: Map<String, SavedColorCardPalette>,
    onTopJournalClick: (ColorCardTopJournalPaletteEntry) -> Unit,
    onNiceClick: (ColorCardNicePaletteEntry) -> Unit,
    onNordClick: (ColorCardNordPaletteEntry) -> Unit,
    onPaletteerClick: (ColorCardPaletteerPaletteEntry) -> Unit,
    onChineseEntryClick: (ColorCardChineseInspirationEntry) -> Unit,
    onGradientClick: (ColorCardGradientInspirationEntry) -> Unit,
    onFavoriteToggle: (ColorCardFavoriteSpec) -> Unit,
    headerContent: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 28.dp
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val colorCardPageCopy = remember(appLanguage) { getColorCardPageGlobalCopy(appLanguage) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (headerContent != null) {
            item(key = "search-header") {
                headerContent()
            }
        }
        if (feedEntries.isEmpty()) {
            if (emptyContent != null) {
                item(key = "empty-content") {
                    emptyContent()
                }
            }
            return@LazyColumn
        }
        items(
            items = feedEntries,
            key = { it.key }
        ) { entry ->
            when (val target = entry.target) {
                is ColorCardDetailTarget.TopJournal -> {
                    MultiPaletteInspirationItem(
                        title = target.entry.title,
                        colors = target.entry.colors,
                        sourceTag = entry.sourceTag,
                        favorited = favoritePalettesByKey.containsKey(entry.key),
                        onClick = { onTopJournalClick(target.entry) },
                        onShare = {
                            shareColorCardText(
                                context = context,
                                chooserTitle = colorCardPageCopy.exportTopJournal,
                                subject = colorCardPageCopy.topJournal,
                                text = buildTopJournalPaletteDetailText(target.entry)
                            )
                        },
                        onFavoriteToggle = {
                            onFavoriteToggle(buildColorCardFavoriteSpec(entry))
                        }
                    )
                }

                is ColorCardDetailTarget.Nice -> {
                    MultiPaletteInspirationItem(
                        title = target.entry.title,
                        colors = target.entry.colors,
                        sourceTag = entry.sourceTag,
                        favorited = favoritePalettesByKey.containsKey(entry.key),
                        onClick = { onNiceClick(target.entry) },
                        onShare = {
                            shareColorCardText(
                                context = context,
                                chooserTitle = colorCardPageCopy.exportNice,
                                subject = colorCardPageCopy.nice,
                                text = buildNicePaletteDetailText(target.entry)
                            )
                        },
                        onFavoriteToggle = {
                            onFavoriteToggle(buildColorCardFavoriteSpec(entry))
                        }
                    )
                }

                is ColorCardDetailTarget.Nord -> {
                    MultiPaletteInspirationItem(
                        title = target.entry.title,
                        colors = target.entry.colors,
                        sourceTag = entry.sourceTag,
                        favorited = favoritePalettesByKey.containsKey(entry.key),
                        onClick = { onNordClick(target.entry) },
                        onShare = {
                            shareColorCardText(
                                context = context,
                                chooserTitle = colorCardPageCopy.exportNord,
                                subject = colorCardPageCopy.nord,
                                text = buildNordPaletteDetailText(target.entry)
                            )
                        },
                        onFavoriteToggle = {
                            onFavoriteToggle(buildColorCardFavoriteSpec(entry))
                        }
                    )
                }

                is ColorCardDetailTarget.Paletteer -> {
                    val appLanguage = LocalAppLanguage.current
                    val inspirationCopy = remember(appLanguage) { getColorCardInspirationCopy(appLanguage) }
                    MultiPaletteInspirationItem(
                        title = if (target.entry.sourceTag == "#${inspirationCopy.topicInspiration}" || target.entry.sourceTag == "#灵感" || target.entry.sourceTag == "#靈感") {
                            "${target.entry.collectionName}\n${target.entry.paletteName}"
                        } else {
                            target.entry.paletteName
                        },
                        colors = target.entry.colors,
                        sourceTag = entry.sourceTag,
                        favorited = favoritePalettesByKey.containsKey(entry.key),
                        onClick = { onPaletteerClick(target.entry) },
                        onShare = {
                            shareColorCardText(
                                context = context,
                                chooserTitle = colorCardPageCopy.exportInspiration,
                                subject = colorCardPageCopy.inspiration,
                                text = buildPaletteerPaletteDetailText(target.entry)
                            )
                        },
                        onFavoriteToggle = {
                            onFavoriteToggle(buildColorCardFavoriteSpec(entry))
                        }
                    )
                }

                is ColorCardDetailTarget.Inspiration -> {
                    ChineseColorInspirationItem(
                        entry = target.entry,
                        sourceTag = entry.sourceTag,
                        favorited = favoritePalettesByKey.containsKey(entry.key),
                        onClick = { onChineseEntryClick(target.entry) },
                        onShare = {
                            shareColorCardText(
                                context = context,
                                chooserTitle = when (appLanguage) {
                                    AppLanguage.SimplifiedChinese -> "导出${target.entry.sourceTag}"
                                    AppLanguage.TraditionalChinese -> "匯出${target.entry.sourceTag}"
                                    AppLanguage.Korean -> "내보내기 ${target.entry.sourceTag}"
                                    AppLanguage.Japanese -> "${target.entry.sourceTag} を書き出し"
                                    AppLanguage.French -> "Exporter ${target.entry.sourceTag}"
                                    AppLanguage.German -> "${target.entry.sourceTag} exportieren"
                                    AppLanguage.Italian -> "Esporta ${target.entry.sourceTag}"
                                    AppLanguage.Portuguese -> "Exportar ${target.entry.sourceTag}"
                                    AppLanguage.Spanish -> "Exportar ${target.entry.sourceTag}"
                                    AppLanguage.Russian -> "Экспорт ${target.entry.sourceTag}"
                                    AppLanguage.Thai -> "ส่งออก ${target.entry.sourceTag}"
                                    AppLanguage.Vietnamese -> "Xuất ${target.entry.sourceTag}"
                                    AppLanguage.Indonesian -> "Ekspor ${target.entry.sourceTag}"
                                    AppLanguage.Arabic -> "تصدير ${target.entry.sourceTag}"
                                    AppLanguage.Ukrainian -> "Експорт ${target.entry.sourceTag}"
                                    AppLanguage.Polish -> "Eksportuj ${target.entry.sourceTag}"
                                    AppLanguage.Czech -> "Exportovat ${target.entry.sourceTag}"
                                    AppLanguage.Dutch -> "Exporteer ${target.entry.sourceTag}"
                                    AppLanguage.Swedish -> "Exportera ${target.entry.sourceTag}"
                                    AppLanguage.Danish -> "Eksportér ${target.entry.sourceTag}"
                                    AppLanguage.Norwegian -> "Eksporter ${target.entry.sourceTag}"
                                    AppLanguage.Finnish -> "Vie ${target.entry.sourceTag}"
                                    AppLanguage.Greek -> "Εξαγωγή ${target.entry.sourceTag}"
                                    AppLanguage.Romanian -> "Exportă ${target.entry.sourceTag}"
                                    AppLanguage.Hungarian -> "Exportálás: ${target.entry.sourceTag}"
                                    AppLanguage.Turkish -> "Dışa aktar: ${target.entry.sourceTag}"
                                    AppLanguage.Malay -> "Eksport ${target.entry.sourceTag}"
                                    AppLanguage.Hebrew -> "ייצוא ${target.entry.sourceTag}"
                                    AppLanguage.English -> "Export ${target.entry.sourceTag}"
                                },
                                subject = target.entry.sourceTag,
                                text = buildChineseColorDetailText(target.entry)
                            )
                        },
                        onFavoriteToggle = {
                            onFavoriteToggle(buildColorCardFavoriteSpec(entry))
                        }
                    )
                }

                is ColorCardDetailTarget.Gradient -> {
                    GradientInspirationItem(
                        entry = target.entry,
                        sourceTag = entry.sourceTag,
                        favorited = favoritePalettesByKey.containsKey(entry.key),
                        onClick = { onGradientClick(target.entry) },
                        onShare = {
                            shareColorCardText(
                                context = context,
                                chooserTitle = colorCardPageCopy.exportGradientRecipe,
                                subject = colorCardPageCopy.gradientRecipe,
                                text = buildColorCardGradientDetailText(
                                    title = target.entry.title,
                                    sourceTag = target.entry.sourceName,
                                    gradientSpec = target.entry.gradientSpec
                                )
                            )
                        },
                        onFavoriteToggle = {
                            onFavoriteToggle(buildColorCardFavoriteSpec(entry))
                        }
                    )
                }

                is ColorCardDetailTarget.Mine -> Unit
            }
        }
    }
}

@Composable
private fun ColorCardInspirationSearchBar(
    active: Boolean,
    filter: ColorCardInspirationFilter,
    onActivate: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(18.dp)
    val containerColor = MaterialTheme.colorScheme.background
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.44f else 0.64f)
    val secondaryText = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    } else {
        Color(0xFF777777)
    }

    LaunchedEffect(active) {
        if (active) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    Row(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .then(
                if (!active) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onActivate
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = colorCardStrings.searchFilterContentDescription,
            tint = secondaryText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        if (active) {
            BasicTextField(
                value = filter.query,
                onValueChange = onQueryChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    }
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (filter.query.isBlank()) {
                            Text(
                                text = if (filter.primaryColor != null || !filter.topicTag.isNullOrBlank()) {
                                    colorCardStrings.searchContinueHint
                                } else {
                                    colorCardStrings.searchHint
                                },
                                color = secondaryText,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            Text(
                text = colorCardStrings.searchHint,
                color = secondaryText,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (active) {
            Spacer(modifier = Modifier.width(8.dp))
            ColorCardIconActionButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = colorCardStrings.closeFilterContentDescription,
                tint = secondaryText,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    onClose()
                }
            )
        }
    }
}

@Composable
private fun ColorCardInspirationFilterResultsPage(
    filter: ColorCardInspirationFilter,
    topics: List<String>,
    filteredEntries: List<ColorCardInspirationFeedEntry>,
    listState: LazyListState,
    favoritePalettesByKey: Map<String, SavedColorCardPalette>,
    onPrimaryColorToggle: (ColorCardInspirationPrimaryColor) -> Unit,
    onTopicToggle: (String) -> Unit,
    onTopJournalClick: (ColorCardTopJournalPaletteEntry) -> Unit,
    onNiceClick: (ColorCardNicePaletteEntry) -> Unit,
    onNordClick: (ColorCardNordPaletteEntry) -> Unit,
    onPaletteerClick: (ColorCardPaletteerPaletteEntry) -> Unit,
    onChineseEntryClick: (ColorCardChineseInspirationEntry) -> Unit,
    onGradientClick: (ColorCardGradientInspirationEntry) -> Unit,
    onFavoriteToggle: (ColorCardFavoriteSpec) -> Unit,
    searchBar: @Composable () -> Unit,
    topContent: (@Composable () -> Unit)? = null,
    lastAutoScrollSignature: String?,
    onLastAutoScrollSignatureChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 28.dp
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val secondaryText = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
    } else {
        Color(0xFF666666)
    }
    var controlsCollapsed by rememberSaveable(
        filter.query,
        filter.primaryColor?.name,
        filter.topicTag
    ) {
        mutableStateOf(filter.hasActiveSelection && filteredEntries.isNotEmpty())
    }
    val autoScrollSignature = remember(
        filter.query,
        filter.primaryColor?.name,
        filter.topicTag,
        filteredEntries.size
    ) {
        listOf(
            filter.query.trim(),
            filter.primaryColor?.name.orEmpty(),
            filter.topicTag.orEmpty(),
            filteredEntries.size.toString()
        ).joinToString(separator = "\u0001")
    }

    LaunchedEffect(
        autoScrollSignature,
        filter.hasActiveSelection,
        filteredEntries.isNotEmpty(),
        lastAutoScrollSignature
    ) {
        if (!filter.hasActiveSelection || filteredEntries.isEmpty()) {
            if (lastAutoScrollSignature != null) {
                onLastAutoScrollSignatureChange(null)
            }
            return@LaunchedEffect
        }
        if (lastAutoScrollSignature != autoScrollSignature) {
            listState.scrollToItem(0)
            onLastAutoScrollSignatureChange(autoScrollSignature)
        }
    }

    LaunchedEffect(
        filter.query,
        filter.primaryColor?.name,
        filter.topicTag,
        filter.hasActiveSelection,
        filteredEntries.isNotEmpty()
    ) {
        controlsCollapsed = filter.hasActiveSelection && filteredEntries.isNotEmpty()
    }

    val filterHeader: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            topContent?.invoke()
            searchBar()
            if (controlsCollapsed) {
                ColorCardInspirationCollapsedFilterBar(
                    filter = filter,
                    resultCount = filteredEntries.size,
                    onExpand = { controlsCollapsed = false },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ColorCardInspirationFilterControls(
                    filter = filter,
                    topics = topics,
                    onPrimaryColorToggle = onPrimaryColorToggle,
                    onTopicToggle = onTopicToggle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (filter.hasActiveSelection && filteredEntries.isNotEmpty()) {
                Text(
                    text = colorCardStrings.resultCountLabel(filteredEntries.size),
                    color = secondaryText,
                    fontSize = 13.sp
                )
            }
        }
    }

    ColorCardInspirationFeedList(
        listState = listState,
        feedEntries = if (filter.hasActiveSelection) filteredEntries else emptyList(),
        favoritePalettesByKey = favoritePalettesByKey,
        onTopJournalClick = onTopJournalClick,
        onNiceClick = onNiceClick,
        onNordClick = onNordClick,
        onPaletteerClick = onPaletteerClick,
        onChineseEntryClick = onChineseEntryClick,
        onGradientClick = onGradientClick,
        onFavoriteToggle = onFavoriteToggle,
        headerContent = filterHeader,
        emptyContent = if (filter.hasActiveSelection && filteredEntries.isEmpty()) {
            {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = colorCardStrings.noResultTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = colorCardStrings.noResultBody,
                        color = secondaryText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            null
        },
        modifier = modifier.fillMaxSize(),
        bottomContentPadding = bottomContentPadding
    )
}

@Composable
private fun ColorCardInspirationFilterControls(
    filter: ColorCardInspirationFilter,
    topics: List<String>,
    onPrimaryColorToggle: (ColorCardInspirationPrimaryColor) -> Unit,
    onTopicToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val secondaryText = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
    } else {
        Color(0xFF747474)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = colorCardStrings.primaryColorLabel,
                color = secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(ColorCardInspirationPrimaryColor.entries) { primaryColor ->
                    ColorCardInspirationPrimaryColorItem(
                        primaryColor = primaryColor,
                        selected = filter.primaryColor == primaryColor,
                        onClick = { onPrimaryColorToggle(primaryColor) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = colorCardStrings.topicLabel,
                color = secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                topics.forEach { topic ->
                    ColorCardInspirationTopicChip(
                        topic = topic,
                        selected = filter.topicTag == topic,
                        onClick = { onTopicToggle(topic) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCardInspirationCollapsedFilterBar(
    filter: ColorCardInspirationFilter,
    resultCount: Int,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
    } else {
        Color.Black.copy(alpha = 0.035f)
    }
    val borderColor = inspirationCardBorderColor()
    val secondaryText = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f)
    } else {
        Color(0xFF6B6B6B)
    }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildColorCardInspirationFilterSummary(filter, LocalAppLanguage.current),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$resultCount",
            color = secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        ColorCardActionChip(
            label = colorCardStrings.filterAction,
            onClick = onExpand,
            compact = true
        )
    }
}

@Composable
private fun ColorCardInspirationPrimaryColorItem(
    primaryColor: ColorCardInspirationPrimaryColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
    }
    val borderColor = if (selected) {
        inspirationRefreshAccentColor()
    } else {
        inspirationCardBorderColor()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 42.dp else 36.dp)
                .clip(CircleShape)
                .background(Color(primaryColor.swatch))
                .colorCardSwatchOutline(colorInt = primaryColor.swatch, shape = CircleShape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = CircleShape
                )
        )
        Text(
            text = resolveLocalizedPrimaryColorLabel(primaryColor, LocalAppLanguage.current),
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun ColorCardInspirationTopicChip(
    topic: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = isSystemInDarkTheme()
    val containerColor = if (selected) {
        inspirationRefreshAccentColor().copy(alpha = if (isDark) 0.28f else 0.16f)
    } else if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }
    val borderColor = if (selected) {
        inspirationRefreshAccentColor().copy(alpha = if (isDark) 0.72f else 0.34f)
    } else {
        inspirationCardBorderColor()
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = normalizeColorCardTag(topic),
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun MultiPaletteInspirationItem(
    title: String,
    colors: List<Int>,
    sourceTag: String,
    favorited: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    InspirationPaletteCard(
        title = title,
        sourceTag = sourceTag,
        secondaryActionImageVector = if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        secondaryActionContentDescription = if (favorited) colorCardStrings.unfavoriteContentDescription else colorCardStrings.favoriteContentDescription,
        secondaryActionTint = if (favorited) Color(0xFFD35C63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
        onClick = onClick,
        onShare = onShare,
        onSecondaryAction = onFavoriteToggle,
        preview = {
            ColorCardPalettePreview(colors = colors)
        }
    )
}

@Composable
private fun GradientInspirationItem(
    entry: ColorCardGradientInspirationEntry,
    sourceTag: String,
    favorited: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    InspirationPaletteCard(
        title = entry.title,
        sourceTag = sourceTag,
        secondaryActionImageVector = if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        secondaryActionContentDescription = if (favorited) colorCardStrings.unfavoriteContentDescription else colorCardStrings.favoriteContentDescription,
        secondaryActionTint = if (favorited) Color(0xFFD35C63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
        onClick = onClick,
        onShare = onShare,
        onSecondaryAction = onFavoriteToggle,
        preview = {
            ColorCardGradientPreview(
                gradientSpec = entry.gradientSpec,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                shape = RoundedCornerShape(20.dp),
                borderColor = inspirationCardBorderColor()
            )
        }
    )
}

@Composable
private fun ColorCardSourceTagChip(
    tag: String
) {
    val appLanguage = LocalAppLanguage.current
    val localizedTag = remember(tag, appLanguage) { localizeColorCardSourceTag(tag, appLanguage) }
    val normalizedTag = remember(localizedTag) { normalizeColorCardTag(localizedTag) }
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) {
        inspirationRefreshAccentColor().copy(alpha = 0.22f)
    } else {
        Color(0xFFF0E3D6)
    }
    val contentColor = if (isDark) {
        Color(0xFFF7EBDD)
    } else {
        Color(0xFF8B5E3C)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = normalizedTag,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ChineseColorInspirationItem(
    entry: ColorCardChineseInspirationEntry,
    sourceTag: String,
    favorited: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val language = LocalAppLanguage.current
    val colorCardStrings = remember(language) { getColorCardUiStrings(language) }
    InspirationPaletteCard(
        title = resolveLocalizedChineseColorCardTitle(entry.name, language),
        sourceTag = sourceTag,
        secondaryActionImageVector = if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        secondaryActionContentDescription = if (favorited) colorCardStrings.unfavoriteContentDescription else colorCardStrings.favoriteContentDescription,
        secondaryActionTint = if (favorited) Color(0xFFD35C63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
        useChineseFont = language == AppLanguage.SimplifiedChinese || language == AppLanguage.TraditionalChinese,
        onClick = onClick,
        onShare = onShare,
        onSecondaryAction = onFavoriteToggle,
        preview = {
            ColorCardPalettePreview(colors = listOf(entry.color))
        }
    )
}

@Composable
private fun InspirationPaletteCard(
    title: String?,
    sourceTag: String?,
    secondaryActionImageVector: ImageVector,
    secondaryActionContentDescription: String,
    secondaryActionTint: Color,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onSecondaryAction: () -> Unit,
    useChineseFont: Boolean = false,
    preview: @Composable () -> Unit
) {
    val borderColor = inspirationCardBorderColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        preview()
        if (!title.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            if (useChineseFont) {
                ChineseColorNameText(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    textSizeSp = 25f,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (shouldUseColorCardLeiko(title)) {
                LeikoText(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    textSizeSp = 16f,
                    fontWeight = FontWeight.SemiBold,
                    lineHeightSp = 22f,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp,
                    maxLines = 2
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            sourceTag?.takeIf { it.isNotBlank() }?.let { tag ->
                ColorCardSourceTagChip(tag = tag)
            } ?: Spacer(modifier = Modifier.width(1.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorCardIconActionButton(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = getColorCardUiStrings(LocalAppLanguage.current).shareContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = onShare
                )
                ColorCardIconActionButton(
                    imageVector = secondaryActionImageVector,
                    contentDescription = secondaryActionContentDescription,
                    tint = secondaryActionTint,
                    onClick = onSecondaryAction
                )
            }
        }
    }
}

@Composable
private fun ColorCardPalettePreview(
    colors: List<Int>
) {
    val previewColors = remember(colors) { colors.ifEmpty { listOf(0xFFBBBBBB.toInt()) } }
    val swatchGap = 4.dp

    if (previewColors.size == 1) {
        val color = previewColors.first()
        val contentColor = colorCardContentColor(color)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(color))
                .colorCardSwatchOutline(colorInt = color, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${colorCardMetrics(color).hex}",
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(swatchGap)
        ) {
            previewColors.forEach { colorInt ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (previewColors.size >= 7) 72.dp else 92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(colorInt))
                        .colorCardSwatchOutline(colorInt = colorInt, shape = RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
private fun inspirationCardBorderColor(): Color {
    return if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
}

private fun inspirationRefreshAccentColor(): Color {
    return Color(0xFF9C6B44)
}

private fun needsDarkSwatchOutline(colorInt: Int): Boolean {
    val color = Color(colorInt)
    return color.red >= 0.985f && color.green >= 0.985f && color.blue >= 0.985f
}

private fun colorCardWhiteOutlineColor(): Color {
    return Color.Black.copy(alpha = 0.16f)
}

private fun Modifier.colorCardSwatchOutline(
    colorInt: Int,
    shape: Shape,
    width: androidx.compose.ui.unit.Dp = 1.dp
): Modifier {
    return if (needsDarkSwatchOutline(colorInt)) {
        this.border(width = width, color = colorCardWhiteOutlineColor(), shape = shape)
    } else {
        this
    }
}

private fun colorCardSelectionBorderColor(
    colorInt: Int,
    fallback: Color
): Color {
    return if (needsDarkSwatchOutline(colorInt)) {
        colorCardWhiteOutlineColor()
    } else {
        fallback
    }
}

private fun buildColorCardInspirationFilterTopics(
    feedEntries: List<ColorCardInspirationFeedEntry>
): List<String> {
    val ordered = LinkedHashSet<String>()
    feedEntries.forEach { entry ->
        if (entry.sourceTag.isNotBlank()) {
            ordered += normalizeColorCardTag(entry.sourceTag)
        }
    }
    return ordered.toList()
}

private fun buildColorCardInspirationFilterSummary(
    filter: ColorCardInspirationFilter,
    language: AppLanguage
): String {
    val colorCardStrings = getColorCardUiStrings(language)
    val summaryParts = buildList {
        filter.query.trim().takeIf { it.isNotBlank() }?.let { add(it) }
        filter.primaryColor?.let { add(resolveLocalizedPrimaryColorLabel(it, language)) }
        filter.topicTag?.takeIf { it.isNotBlank() }?.let { add(normalizeColorCardTag(it)) }
    }
    return summaryParts.joinToString(" · ").ifBlank { "搜索灵感、色名或专题" }
}

private fun matchesColorCardInspirationFilter(
    entry: ColorCardInspirationFeedEntry,
    filter: ColorCardInspirationFilter
): Boolean {
    val normalizedTopicTag = filter.topicTag?.takeIf { it.isNotBlank() }?.let(::normalizeColorCardTag)
    if (normalizedTopicTag != null && normalizeColorCardTag(entry.sourceTag) != normalizedTopicTag) {
        return false
    }
    if (filter.primaryColor != null) {
        val matchesPrimaryColor = colorCardInspirationEntryColors(entry).any { colorInt ->
            classifyColorCardInspirationPrimaryColor(colorInt) == filter.primaryColor
        }
        if (!matchesPrimaryColor) {
            return false
        }
    }
    val normalizedQuery = normalizeColorCardSearchText(filter.query)
    if (normalizedQuery.isBlank()) {
        return true
    }
    return buildColorCardInspirationSearchTexts(entry).any { token ->
        normalizeColorCardSearchText(token).contains(normalizedQuery)
    }
}

private fun normalizeColorCardTag(tag: String): String {
    val trimmed = tag.trim()
    if (trimmed.isBlank()) return trimmed
    return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
}

private fun localizeColorCardSourceTag(tag: String, language: AppLanguage): String {
    val inspirationCopy = getColorCardInspirationCopy(language)
    val trimmedTag = tag.trim()
    return when (trimmedTag) {
        "我的", "我的" -> inspirationCopy.topicMine
        "#我的", "#我的" -> "#${inspirationCopy.topicMine}"
        "灵感", "靈感" -> inspirationCopy.topicInspiration
        "#灵感", "#靈感" -> "#${inspirationCopy.topicInspiration}"
        "顶刊", "頂刊" -> inspirationCopy.topicTopJournal
        "#顶刊", "#頂刊" -> "#${inspirationCopy.topicTopJournal}"
        "渐变", "漸變" -> inspirationCopy.topicGradient
        "#渐变", "#漸變" -> "#${inspirationCopy.topicGradient}"
        "中国传统色", "中國傳統色" -> inspirationCopy.topicChineseTraditional
        "#中国传统色", "#中國傳統色" -> "#${inspirationCopy.topicChineseTraditional}"
        "国画颜料", "國畫顏料" -> inspirationCopy.topicChinesePigments
        "#国画颜料", "#國畫顏料" -> "#${inspirationCopy.topicChinesePigments}"
        "吉卜力" -> inspirationCopy.topicGhibli
        "#吉卜力" -> "#${inspirationCopy.topicGhibli}"
        "艺术家", "藝術家" -> inspirationCopy.topicArtists
        "#艺术家", "#藝術家" -> "#${inspirationCopy.topicArtists}"
        "圣彼得堡冬宫", "聖彼得堡冬宮" -> inspirationCopy.topicHermitage
        "#圣彼得堡冬宫", "#聖彼得堡冬宮" -> "#${inspirationCopy.topicHermitage}"
        "墨西哥艺术", "墨西哥藝術" -> inspirationCopy.topicMexicanArt
        "#墨西哥艺术", "#墨西哥藝術" -> "#${inspirationCopy.topicMexicanArt}"
        "大都会博物馆", "大都會博物館" -> inspirationCopy.topicMetMuseum
        "#大都会博物馆", "#大都會博物館" -> "#${inspirationCopy.topicMetMuseum}"
        "太平洋" -> inspirationCopy.topicPacific
        "#太平洋" -> "#${inspirationCopy.topicPacific}"
        "日本传统色", "日本傳統色" -> inspirationCopy.topicJapaneseTraditional
        "#日本传统色", "#日本傳統色" -> "#${inspirationCopy.topicJapaneseTraditional}"
        "澳大利亚", "澳大利亞" -> inspirationCopy.topicAustralia
        "#澳大利亚", "#澳大利亞" -> "#${inspirationCopy.topicAustralia}"
        "荷兰艺术家", "荷蘭藝術家" -> inspirationCopy.topicDutchMasters
        "#荷兰艺术家", "#荷蘭藝術家" -> "#${inspirationCopy.topicDutchMasters}"
        "蝴蝶的颜色", "蝴蝶的顏色" -> inspirationCopy.topicButterflies
        "#蝴蝶的颜色", "#蝴蝶的顏色" -> "#${inspirationCopy.topicButterflies}"
        "色名词典", "色名詞典" -> inspirationCopy.topicColorNamesBestOf
        "#色名词典", "#色名詞典" -> "#${inspirationCopy.topicColorNamesBestOf}"
        "全球高校" -> inspirationCopy.topicGlobalUniversities
        "#全球高校" -> "#${inspirationCopy.topicGlobalUniversities}"
        "游戏", "遊戲" -> inspirationCopy.topicGame
        "#游戏", "#遊戲" -> "#${inspirationCopy.topicGame}"
        else -> tag
    }
}

private fun normalizeColorCardSearchText(text: String): String {
    return buildString {
        text.trim().forEach { char ->
            if (
                !char.isWhitespace() &&
                char != '#' &&
                char != '/' &&
                char != '·' &&
                char != '-' &&
                char != '_'
            ) {
                append(char.lowercaseChar())
            }
        }
    }
}

private fun buildColorCardInspirationSearchTexts(
    entry: ColorCardInspirationFeedEntry
): List<String> {
    return buildList {
        add(normalizeColorCardTag(entry.sourceTag))
        when (val target = entry.target) {
            is ColorCardDetailTarget.Inspiration -> {
                add(target.entry.name)
                add(target.entry.sourceTag)
            }
            is ColorCardDetailTarget.TopJournal -> {
                add(target.entry.title)
                add("#顶刊")
            }
            is ColorCardDetailTarget.Nice -> {
                add(target.entry.title)
                add("#NICE")
            }
            is ColorCardDetailTarget.Nord -> {
                add(target.entry.title)
                add(target.entry.subtitle)
                add("#Nord")
            }
            is ColorCardDetailTarget.Paletteer -> {
                add(target.entry.collectionName)
                add(target.entry.paletteName)
                add(target.entry.sourceTag)
            }
            is ColorCardDetailTarget.Gradient -> {
                add(target.entry.title)
                add(target.entry.sourceName)
                add("#渐变")
            }
            is ColorCardDetailTarget.Mine -> {
                target.palette.title?.let(::add)
                target.palette.sourceTag?.let(::add)
            }
        }
    }
}

private fun colorCardInspirationEntryColors(
    entry: ColorCardInspirationFeedEntry
): List<Int> {
    return when (val target = entry.target) {
        is ColorCardDetailTarget.Inspiration -> listOf(target.entry.color)
        is ColorCardDetailTarget.TopJournal -> target.entry.colors
        is ColorCardDetailTarget.Nice -> target.entry.colors
        is ColorCardDetailTarget.Nord -> target.entry.colors
        is ColorCardDetailTarget.Paletteer -> target.entry.colors
        is ColorCardDetailTarget.Gradient -> target.entry.colors
        is ColorCardDetailTarget.Mine -> target.palette.colors
    }
}

private fun classifyColorCardInspirationPrimaryColor(
    colorInt: Int
): ColorCardInspirationPrimaryColor? {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(colorInt, hsv)
    val hue = hsv[0]
    val saturation = hsv[1]
    val brightness = hsv[2]
    if (
        brightness <= COLOR_CARD_PRIMARY_COLOR_MIN_BRIGHTNESS ||
        saturation <= COLOR_CARD_PRIMARY_COLOR_MIN_SATURATION ||
        (brightness >= 0.94f && saturation <= 0.24f) ||
        (brightness <= 0.22f && saturation <= 0.24f) ||
        isColorCardPrimaryColorBoundaryHue(hue)
    ) {
        return null
    }
    return when {
        hue < 18f || hue >= 342f -> ColorCardInspirationPrimaryColor.Red
        hue < 42f -> ColorCardInspirationPrimaryColor.Orange
        hue < 68f -> ColorCardInspirationPrimaryColor.Yellow
        hue < 162f -> ColorCardInspirationPrimaryColor.Green
        hue < 196f -> ColorCardInspirationPrimaryColor.Cyan
        hue < 252f -> ColorCardInspirationPrimaryColor.Blue
        else -> ColorCardInspirationPrimaryColor.Purple
    }
}

private fun isColorCardPrimaryColorBoundaryHue(hue: Float): Boolean {
    val normalizedHue = ((hue % 360f) + 360f) % 360f
    val boundaries = floatArrayOf(18f, 42f, 68f, 162f, 196f, 252f, 342f)
    return boundaries.any { boundary ->
        colorCardHueDistance(normalizedHue, boundary) < COLOR_CARD_PRIMARY_COLOR_BOUNDARY_GUARD_DEGREES
    }
}

private fun colorCardHueDistance(a: Float, b: Float): Float {
    val diff = abs(a - b)
    return min(diff, 360f - diff)
}

private fun resolveLocalizedPrimaryColorLabel(
    color: ColorCardInspirationPrimaryColor,
    language: AppLanguage
): String = when (language) {
    AppLanguage.SimplifiedChinese -> color.label
    AppLanguage.TraditionalChinese -> when (color) {
        ColorCardInspirationPrimaryColor.Red -> "紅"
        ColorCardInspirationPrimaryColor.Orange -> "橙"
        ColorCardInspirationPrimaryColor.Yellow -> "黃"
        ColorCardInspirationPrimaryColor.Green -> "綠"
        ColorCardInspirationPrimaryColor.Cyan -> "青"
        ColorCardInspirationPrimaryColor.Blue -> "藍"
        ColorCardInspirationPrimaryColor.Purple -> "紫"
    }
    else -> when (color) {
        ColorCardInspirationPrimaryColor.Red -> "Red"
        ColorCardInspirationPrimaryColor.Orange -> "Orange"
        ColorCardInspirationPrimaryColor.Yellow -> "Yellow"
        ColorCardInspirationPrimaryColor.Green -> "Green"
        ColorCardInspirationPrimaryColor.Cyan -> "Cyan"
        ColorCardInspirationPrimaryColor.Blue -> "Blue"
        ColorCardInspirationPrimaryColor.Purple -> "Purple"
    }
}

private fun resolveLocalizedChineseColorCardTitle(
    name: String,
    language: AppLanguage
): String {
    if (language == AppLanguage.SimplifiedChinese || language == AppLanguage.TraditionalChinese) {
        return name
    }
    val pinyin = romanizeChineseColorName(name)
    return if (pinyin.isBlank() || pinyin.equals(name, ignoreCase = true)) name else "$name · $pinyin"
}

private fun romanizeChineseColorName(name: String): String {
    return runCatching {
        val raw = Transliterator.getInstance("Han-Latin")
            .transliterate(name)
            .replace(Regex("\\s+"), " ")
            .trim()
        raw.split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
                }
            }
    }.getOrDefault(name)
}

private data class ColorCardInspirationFeedBucket(
    val entries: MutableList<ColorCardInspirationFeedEntry>,
    val cycleStride: Int = 1,
    var remainingCooldownCycles: Int = 0
)

private fun buildShuffledColorCardInspirationFeed(
    shuffleSeed: Long,
    language: AppLanguage,
    topJournalEntries: List<ColorCardTopJournalPaletteEntry>,
    niceEntries: List<ColorCardNicePaletteEntry>,
    nordEntries: List<ColorCardNordPaletteEntry>,
    paletteerEntries: List<ColorCardPaletteerPaletteEntry>,
    chineseEntries: List<ColorCardChineseInspirationEntry>,
    gradientEntries: List<ColorCardGradientInspirationEntry>
): List<ColorCardInspirationFeedEntry> {
    val random = Random(shuffleSeed)
    val paletteerBuckets = paletteerEntries
        .groupBy { it.sourceTag }
        .values
        .mapNotNull { entries ->
            val feedEntries = entries.shuffled(random).map { entry ->
                ColorCardInspirationFeedEntry(
                    key = "paletteer-${entry.index}-${entry.collectionName}-${entry.paletteName}",
                    sourceTag = normalizeColorCardTag(entry.sourceTag),
                    target = ColorCardDetailTarget.Paletteer(entry)
                )
            }
            if (feedEntries.isEmpty()) {
                null
            } else {
                ColorCardInspirationFeedBucket(
                    entries = feedEntries.toMutableList(),
                    cycleStride = entries.maxOfOrNull { it.feedCycleStride } ?: 1
                )
            }
        }
    val inspirationCopy = getColorCardInspirationCopy(language)
    val buckets = (listOf(
        ColorCardInspirationFeedBucket(
            entries = topJournalEntries.shuffled(random).map { entry ->
                ColorCardInspirationFeedEntry(
                    key = "journal-${entry.index}",
                    sourceTag = normalizeColorCardTag(inspirationCopy.topicTopJournal),
                    target = ColorCardDetailTarget.TopJournal(entry)
                )
            }.toMutableList()
        ),
        ColorCardInspirationFeedBucket(
            entries = niceEntries.shuffled(random).map { entry ->
                ColorCardInspirationFeedEntry(
                    key = "nice-${entry.index}",
                    sourceTag = normalizeColorCardTag("NICE"),
                    target = ColorCardDetailTarget.Nice(entry)
                )
            }.toMutableList()
        ),
        ColorCardInspirationFeedBucket(
            entries = nordEntries.shuffled(random).map { entry ->
                ColorCardInspirationFeedEntry(
                    key = "nord-${entry.index}-${entry.title}",
                    sourceTag = normalizeColorCardTag("Nord"),
                    target = ColorCardDetailTarget.Nord(entry)
                )
            }.toMutableList()
        ),
        ColorCardInspirationFeedBucket(
            entries = chineseEntries.shuffled(random).map { entry ->
                ColorCardInspirationFeedEntry(
                    key = "chinese-${entry.sourceTag}-${entry.page}-${entry.name}",
                    sourceTag = normalizeColorCardTag(entry.sourceTag),
                    target = ColorCardDetailTarget.Inspiration(entry)
                )
            }.toMutableList()
        ),
        ColorCardInspirationFeedBucket(
            entries = gradientEntries.shuffled(random).map { entry ->
                ColorCardInspirationFeedEntry(
                    key = entry.key,
                    sourceTag = normalizeColorCardTag(inspirationCopy.topicGradient),
                    target = ColorCardDetailTarget.Gradient(entry)
                )
            }.toMutableList()
        )
    ) + paletteerBuckets)
        .filter { it.entries.isNotEmpty() }
        .toMutableList()

    return interleaveColorCardInspirationBuckets(
        buckets = buckets,
        random = random
    )
}

private fun interleaveColorCardInspirationBuckets(
    buckets: MutableList<ColorCardInspirationFeedBucket>,
    random: Random
): List<ColorCardInspirationFeedEntry> {
    val result = ArrayList<ColorCardInspirationFeedEntry>(buckets.sumOf { it.entries.size })
    while (buckets.isNotEmpty()) {
        val cycleOrder = buckets.indices.shuffled(random)
        var emittedInCycle = false
        cycleOrder.forEach { bucketIndex ->
            val bucket = buckets.getOrNull(bucketIndex) ?: return@forEach
            if (bucket.entries.isEmpty()) return@forEach
            if (bucket.remainingCooldownCycles > 0) return@forEach
            result += bucket.entries.removeAt(0)
            bucket.remainingCooldownCycles = bucket.cycleStride.coerceAtLeast(1)
            emittedInCycle = true
        }
        buckets.removeAll { it.entries.isEmpty() }
        if (buckets.isEmpty()) {
            break
        }
        if (!emittedInCycle) {
            val fallbackBucket = buckets.random(random)
            result += fallbackBucket.entries.removeAt(0)
            fallbackBucket.remainingCooldownCycles = fallbackBucket.cycleStride.coerceAtLeast(1)
            buckets.removeAll { it.entries.isEmpty() }
        }
        buckets.forEach { bucket ->
            if (bucket.remainingCooldownCycles > 0) {
                bucket.remainingCooldownCycles -= 1
            }
        }
    }
    return result
}

private fun nextColorCardInspirationShuffleSeed(): Long {
    return System.currentTimeMillis() xor Random.Default.nextLong()
}

@Composable
private fun ColorCardDetailPage(
    target: ColorCardDetailTarget,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val colors = remember(target) {
        when (target) {
            is ColorCardDetailTarget.Mine -> target.palette.colors
            is ColorCardDetailTarget.Inspiration -> listOf(target.entry.color)
            is ColorCardDetailTarget.TopJournal -> target.entry.colors
            is ColorCardDetailTarget.Nice -> target.entry.colors
            is ColorCardDetailTarget.Nord -> target.entry.colors
            is ColorCardDetailTarget.Paletteer -> target.entry.colors
            is ColorCardDetailTarget.Gradient -> target.entry.colors
        }
    }
    val initialGradientSpec = remember(target) {
        when (target) {
            is ColorCardDetailTarget.Mine -> target.palette.gradientSpec
                ?: buildColorCardGradientSpecFromColors(target.palette.colors)
            is ColorCardDetailTarget.Inspiration -> null
            is ColorCardDetailTarget.TopJournal -> buildColorCardGradientSpecFromColors(target.entry.colors)
            is ColorCardDetailTarget.Nice -> buildColorCardGradientSpecFromColors(target.entry.colors)
            is ColorCardDetailTarget.Nord -> buildColorCardGradientSpecFromColors(target.entry.colors)
            is ColorCardDetailTarget.Paletteer -> buildColorCardGradientSpecFromColors(target.entry.colors)
            is ColorCardDetailTarget.Gradient -> target.entry.gradientSpec
        }
    }
    var editableGradientSpec by remember(target) { mutableStateOf(initialGradientSpec) }
    var gradientPanelVisible by remember(target) { mutableStateOf(false) }
    var showMetricInfoDialog by remember(target) { mutableStateOf(false) }
    val hasAllPage = remember(colors) { colors.size > 1 }
    val hasGradientPage = editableGradientSpec != null
    val pageCount = remember(colors, hasAllPage, hasGradientPage) {
        (
            colors.size +
                (if (hasAllPage) 1 else 0) +
                (if (hasGradientPage) 1 else 0)
            ).coerceAtLeast(1)
    }
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val detailIndicatorListState = rememberLazyListState()
    val gradientPageIndex = if (hasGradientPage) pageCount - 1 else -1
    val isAllPage = hasAllPage && pagerState.currentPage == 0
    val isGradientPage = hasGradientPage && pagerState.currentPage == gradientPageIndex
    val currentIndex = when {
        colors.isEmpty() -> 0
        isAllPage || isGradientPage -> 0
        hasAllPage -> (pagerState.currentPage - 1).coerceIn(0, colors.lastIndex)
        else -> pagerState.currentPage.coerceIn(0, colors.lastIndex)
    }
    val gradientPaletteColors = remember(editableGradientSpec) {
        editableGradientSpec?.let(::colorCardGradientPaletteColors).orEmpty()
    }
    val currentColor = when {
        isGradientPage -> gradientPaletteColors.firstOrNull()
            ?: colors.firstOrNull()
            ?: 0xFFBBBBBB.toInt()
        else -> colors.getOrElse(currentIndex) { 0xFFBBBBBB.toInt() }
    }
    val contentColor = when {
        isAllPage -> colorCardOverviewContentColor(colors)
        isGradientPage -> colorCardOverviewContentColor(gradientPaletteColors.ifEmpty { colors })
        else -> colorCardContentColor(currentColor)
    }
    val secondaryText = contentColor.copy(alpha = 0.62f)
    val metricInfoMetrics = remember(currentColor, isAllPage, isGradientPage) {
        if (isAllPage || isGradientPage) {
            null
        } else {
            colorCardMetrics(currentColor)
        }
    }
    DisposableEffect(activity) {
        val supportsWideGamut = supportsColorCardWideGamut(activity)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity == null || !supportsWideGamut) {
            onDispose {}
        } else {
            val window = activity.window
            val previousColorMode = window.colorMode
            runCatching {
                window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
            }
            onDispose {
                runCatching {
                    window.colorMode = previousColorMode
                }
            }
        }
    }
    val shareGradientSpec: () -> Unit = {
        editableGradientSpec?.let { gradientSpec ->
            val shareSpec = buildColorCardGradientShareSpec(
                target = target,
                gradientSpec = gradientSpec
            )
            shareColorCardText(
                context = context,
                chooserTitle = shareSpec.chooserTitle,
                subject = shareSpec.subject,
                text = shareSpec.text
            )
        } ?: Unit
    }
    val selectedIndicatorIndex = when {
        !hasAllPage -> 0
        isAllPage -> 0
        isGradientPage && hasGradientPage -> colors.size + 1
        else -> currentIndex + 1
    }
    val detailHeader = when (target) {
        is ColorCardDetailTarget.Mine -> {
            target.palette.title?.takeIf { it.isNotBlank() }?.let { title ->
                ColorCardDetailHeader(
                    eyebrow = target.palette.sourceTag?.takeIf { it.isNotBlank() } ?: colorCardStrings.mineEyebrowFallback,
                    title = title,
                    useChineseFont = target.palette.useChineseFont
                )
            }
        }
        is ColorCardDetailTarget.Inspiration -> ColorCardDetailHeader(
            eyebrow = target.entry.sourceTag,
            title = resolveLocalizedChineseColorCardTitle(target.entry.name, appLanguage),
            useChineseFont = appLanguage == AppLanguage.SimplifiedChinese || appLanguage == AppLanguage.TraditionalChinese
        )
        is ColorCardDetailTarget.TopJournal -> ColorCardDetailHeader(
            eyebrow = colorCardStrings.topJournalEyebrow,
            title = target.entry.title,
            useChineseFont = false
        )
        is ColorCardDetailTarget.Nice -> ColorCardDetailHeader(
            eyebrow = "NICE",
            title = target.entry.title,
            useChineseFont = false
        )
        is ColorCardDetailTarget.Nord -> ColorCardDetailHeader(
            eyebrow = "Nord",
            title = target.entry.title,
            useChineseFont = false
        )
        is ColorCardDetailTarget.Paletteer -> ColorCardDetailHeader(
            eyebrow = "${colorCardStrings.inspirationEyebrowPrefix} · ${target.entry.collectionName}",
            title = target.entry.paletteName,
            useChineseFont = false
        )
        is ColorCardDetailTarget.Gradient -> ColorCardDetailHeader(
            eyebrow = "${colorCardStrings.gradientEyebrowPrefix} · ${target.entry.sourceName}",
            title = target.entry.title,
            useChineseFont = false
        )
    }
    val exportHeader = detailHeader?.let { header ->
        ColorCardExportHeader(
            eyebrow = header.eyebrow,
            title = header.title,
            useChineseFont = header.useChineseFont
        )
    }
    val saveCurrentImage: () -> Unit = saveCurrentImage@{
        val exportSpec = when {
            isGradientPage -> editableGradientSpec?.let { gradientSpec ->
                ColorCardImageExportSpec.Gradient(
                    gradientSpec = gradientSpec,
                    header = exportHeader
                )
            }
            isAllPage -> ColorCardImageExportSpec.All(
                colors = colors,
                header = exportHeader
            )
            else -> ColorCardImageExportSpec.Single(
                colorInt = currentColor,
                colorIndex = currentIndex,
                totalColors = colors.size,
                header = exportHeader
            )
        } ?: return@saveCurrentImage
        scope.launch {
            val savedUri = saveColorCardExportImage(
                context = context,
                spec = exportSpec
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    if (savedUri != null) colorCardStrings.saveImageSuccess else colorCardStrings.saveImageFailure,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(currentColor))
    ) {
        LaunchedEffect(hasAllPage, selectedIndicatorIndex) {
            if (!hasAllPage) return@LaunchedEffect

            val layoutInfo = detailIndicatorListState.layoutInfo
            val selectedItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == selectedIndicatorIndex }
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val isFullyVisible = selectedItem != null &&
                selectedItem.offset >= viewportStart &&
                (selectedItem.offset + selectedItem.size) <= viewportEnd

            if (!isFullyVisible) {
                detailIndicatorListState.animateScrollToItem(selectedIndicatorIndex)
            }
        }

        LaunchedEffect(isAllPage, isGradientPage) {
            if (isAllPage || isGradientPage) {
                showMetricInfoDialog = false
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> "$index-$target" }
        ) { page ->
            when {
                hasAllPage && page == 0 -> {
                    ColorCardAllDetailSheet(colors = colors)
                }
                hasGradientPage && page == gradientPageIndex -> {
                    editableGradientSpec?.let { gradientSpec ->
                        ColorCardGradientDetailSheet(
                            gradientSpec = gradientSpec,
                            eyebrow = detailHeader?.eyebrow,
                            title = detailHeader?.title,
                            useChineseFont = detailHeader?.useChineseFont == true,
                            panelVisible = gradientPanelVisible,
                            onGradientSpecChange = { updated ->
                                editableGradientSpec = normalizeColorCardGradientSpec(updated)
                            },
                            onExportGradient = shareGradientSpec,
                            onSaveImage = saveCurrentImage
                        )
                    }
                }
                else -> {
                    val colorPageIndex = if (hasAllPage) page - 1 else page
                    ColorCardDetailSheet(
                        colorInt = colors[colorPageIndex],
                        colorIndex = colorPageIndex,
                        totalColors = colors.size,
                        detailHeader = detailHeader
                    )
                }
            }
        }

        UnifiedSubpageTopBar(
            title = if (isAllPage || isGradientPage) "" else "\u8272\u5361",
            onBack = onBack,
            titleColor = contentColor,
            backButtonContainerColor = contentColor.copy(alpha = 0.10f),
            backButtonIconColor = contentColor,
            trailing = if (isGradientPage) {
                {
                    UnifiedSubpageTopBarIconButton(
                        imageVector = if (gradientPanelVisible) Icons.Outlined.Close else Icons.Outlined.Tune,
                        contentDescription = if (gradientPanelVisible) "\u9690\u85cf\u8bbe\u7f6e\u9762\u677f" else "\u663e\u793a\u8bbe\u7f6e\u9762\u677f",
                        onClick = {
                            gradientPanelVisible = !gradientPanelVisible
                        },
                        containerColor = contentColor.copy(alpha = 0.10f),
                        iconTint = contentColor
                    )
                }
            } else if (!isAllPage) {
                {
                    UnifiedSubpageTopBarIconButton(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = colorCardStrings.detailsTitle,
                        onClick = {
                            showMetricInfoDialog = true
                        },
                        containerColor = contentColor.copy(alpha = 0.10f),
                        iconTint = contentColor
                    )
                }
            } else {
                null
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        metricInfoMetrics?.let { metrics ->
            if (showMetricInfoDialog) {
                ColorCardMetricInfoDialogCompactV2(
                    colorInt = currentColor,
                    metrics = metrics,
                    onDismissRequest = {
                        showMetricInfoDialog = false
                    },
                    onCopy = {
                        copyColorCardText(
                            context = context,
                            label = "color_metric_detail",
                            text = buildColorCardMetricInfoTextCompactForSharingV2(
                                colorInt = currentColor,
                                metrics = metrics
                            ),
                        successMessage = colorCardStrings.copiedDetails
                        )
                        showMetricInfoDialog = false
                    }
                )
            }
        }

        when {
            true -> Unit
            target is ColorCardDetailTarget.Inspiration -> {
                Text(
                    text = colorCardStrings.inspirationEyebrowPrefix,
                    color = secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 62.dp, end = 24.dp)
                )
            }
            target is ColorCardDetailTarget.TopJournal -> {
                Text(
                    text = colorCardStrings.topJournalEyebrow,
                    color = secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 62.dp, end = 24.dp)
                )
            }
            target is ColorCardDetailTarget.Nice -> {
                Text(
                    text = "NICE",
                    color = secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 62.dp, end = 24.dp)
                )
            }
            target is ColorCardDetailTarget.Nord -> {
                Text(
                    text = "Nord",
                    color = secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 62.dp, end = 24.dp)
                )
            }
            target is ColorCardDetailTarget.Paletteer -> {
                Text(
                    text = colorCardStrings.inspirationEyebrowPrefix,
                    color = secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 62.dp, end = 24.dp)
                )
            }
            else -> Unit
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasAllPage && !isAllPage && !isGradientPage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(contentColor.copy(alpha = 0.10f))
                ) {
                    LazyRow(
                        state = detailIndicatorListState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item(key = "all") {
                            ColorCardAllPageIndicator(
                                selected = isAllPage,
                                contentColor = contentColor,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                }
                            )
                        }
                        itemsIndexed(
                            items = colors,
                            key = { index, colorInt -> "color-$index-$colorInt" }
                        ) { index, colorInt ->
                            Box(
                                modifier = Modifier
                                    .size(if (!isAllPage && !isGradientPage && index == currentIndex) 34.dp else 28.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (!isAllPage && !isGradientPage && index == currentIndex) 2.dp else 1.dp,
                                        color = colorCardSelectionBorderColor(
                                            colorInt = colorInt,
                                            fallback = contentColor.copy(
                                                alpha = if (!isAllPage && !isGradientPage && index == currentIndex) 0.96f else 0.34f
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index + 1)
                                        }
                                    }
                            )
                        }
                        if (hasGradientPage && editableGradientSpec != null) {
                            item(key = "gradient") {
                                ColorCardGradientPageIndicator(
                                    selected = isGradientPage,
                                    contentColor = contentColor,
                                    gradientSpec = editableGradientSpec!!,
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(gradientPageIndex)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (!isGradientPage) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorCardActionChip(
                        label = if (isAllPage) colorCardStrings.exportAllGroup else colorCardStrings.exportCurrentColor,
                        onClick = {
                            val shareSpec = buildColorCardShareSpec(
                                target = target,
                                colorIndex = currentIndex,
                                exportAll = isAllPage
                            )
                            shareColorCardText(
                                context = context,
                                chooserTitle = shareSpec.chooserTitle,
                                subject = shareSpec.subject,
                                text = shareSpec.text
                            )
                        },
                        compact = true,
                        customContainerColor = contentColor.copy(alpha = 0.10f),
                        customContentColor = contentColor
                    )
                    ColorCardActionChip(
                        label = colorCardStrings.saveImage,
                        onClick = saveCurrentImage,
                        compact = true,
                        customContainerColor = contentColor.copy(alpha = 0.10f),
                        customContentColor = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCardAllDetailSheet(
    colors: List<Int>
) {
    val hexSize = if (colors.size >= 6) 24.sp else 30.sp

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        colors.forEach { colorInt ->
            val contentColor = colorCardContentColor(colorInt)
            val metrics = remember(colorInt) { colorCardMetrics(colorInt) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(colorInt))
                    .colorCardSwatchOutline(colorInt = colorInt, shape = RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${metrics.hex}",
                    color = contentColor,
                    fontSize = hexSize,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ColorCardDetailSheet(
    colorInt: Int,
    colorIndex: Int,
    totalColors: Int,
    detailHeader: ColorCardDetailHeader?
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val metrics = remember(colorInt) { colorCardMetrics(colorInt) }
    val contentColor = colorCardContentColor(colorInt)
    val secondaryText = contentColor.copy(alpha = 0.60f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp, vertical = 118.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (detailHeader != null) {
                Text(
                    text = detailHeader.eyebrow,
                    color = secondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (detailHeader.useChineseFont) {
                    ChineseColorNameText(
                        text = detailHeader.title,
                        color = contentColor,
                        textSizeSp = 34f,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (shouldUseColorCardLeiko(detailHeader.title)) {
                    LeikoText(
                        text = detailHeader.title,
                        color = contentColor,
                        textSizeSp = 30f,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = detailHeader.title,
                        color = contentColor,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = "${colorIndex + 1} / $totalColors",
                    color = secondaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            ColorMetricBlock(
                label = "CMYK",
                icon = ColorCardMetricIcons.Blend,
                value = metrics.cmyk,
                contentColor = contentColor,
                secondaryText = secondaryText,
                onValueLongPress = {
                    copyColorCardText(
                        context = context,
                        label = "color_metric_cmyk",
                        text = "CMYK: ${metrics.cmyk}",
                        successMessage = colorCardStrings.copiedMetricValue
                    )
                }
            )
            ColorMetricBlock(
                label = "RGB",
                icon = ColorCardMetricIcons.VectorSquare,
                value = metrics.rgb,
                contentColor = contentColor,
                secondaryText = secondaryText,
                onValueLongPress = {
                    copyColorCardText(
                        context = context,
                        label = "color_metric_rgb",
                        text = "RGB: ${metrics.rgb}",
                        successMessage = colorCardStrings.copiedMetricValue
                    )
                }
            )
            ColorMetricBlock(
                label = "HSB",
                icon = ColorCardMetricIcons.Rainbow,
                value = metrics.hsb,
                contentColor = contentColor,
                secondaryText = secondaryText,
                onValueLongPress = {
                    copyColorCardText(
                        context = context,
                        label = "color_metric_hsb",
                        text = "HSB: ${metrics.hsb}",
                        successMessage = colorCardStrings.copiedMetricValue
                    )
                }
            )
            ColorMetricBlock(
                label = "HEX",
                icon = ColorCardMetricIcons.Contrast,
                value = "#${metrics.hex}",
                contentColor = contentColor,
                secondaryText = secondaryText,
                onValueLongPress = {
                    copyColorCardText(
                        context = context,
                        label = "color_metric_hex",
                        text = "HEX: #${metrics.hex}",
                        successMessage = colorCardStrings.copiedMetricValue
                    )
                }
            )
            Spacer(modifier = Modifier.height(92.dp))
        }
    }
}

private fun buildColorCardMetricInfoText(
    colorInt: Int,
    metrics: ColorCardMetrics,
    displayProfile: ColorCardDisplayProfile
): String {
    val harmonySwatches = resolveColorCardHarmonySwatches(colorInt)
    return buildString {
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("HEXA: #")
        append(metrics.hexa)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("RGBA: ")
        append(metrics.rgba)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("HSL: ")
        append(metrics.hsl)
        append('\n')
        append("LAB: ")
        append(metrics.lab)
        append('\n')
        append("ΔE00 · 白底: ")
        append(metrics.deltaE00OnWhite)
        append('\n')
        append("ΔE00 · 黑底: ")
        append(metrics.deltaE00OnBlack)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
        append('\n')
        append('\n')
        append("相关配色: ")
        append('\n')
        harmonySwatches.forEach { swatch ->
            append(swatch.label)
            append(": ")
            append(swatch.hex)
            append('\n')
        }
        append('\n')
        append("相关配色: ")
        append('\n')
        harmonySwatches.forEach { swatch ->
            append(swatch.label)
            append(": ")
            append(swatch.hex)
            append('\n')
        }
        append('\n')
        append("相关配色: ")
        append('\n')
        harmonySwatches.forEach { swatch ->
            append(swatch.label)
            append(": ")
            append(swatch.hex)
            append('\n')
        }
        append('\n')
        append("颜色基准: ")
        append('\n')
        append("设备广色域: ")
        append('\n')
        append("当前窗口: ")
        append('\n')
        append("显示说明: ")
        append('\n')
        append('\n')
        append("\u5bf9\u767d\u5e95\u5bf9\u6bd4\u5ea6: ")
        append(metrics.contrastOnWhite)
        append('\n')
        append("\u5bf9\u767d\u5e95 WCAG: ")
        append(metrics.wcagOnWhite)
        append('\n')
        append("\u5bf9\u9ed1\u5e95\u5bf9\u6bd4\u5ea6: ")
        append(metrics.contrastOnBlack)
        append('\n')
        append("\u5bf9\u9ed1\u5e95 WCAG: ")
        append(metrics.wcagOnBlack)
        append('\n')
        append("\u63a8\u8350\u6587\u5b57: ")
        append(metrics.wcagRecommendedText)
    }
}

private fun buildColorCardMetricInfoTextCompact(
    colorInt: Int,
    metrics: ColorCardMetrics
): String {
    val harmonySwatches = resolveColorCardHarmonySwatches(colorInt)
    return buildString {
        append("HEX: #")
        append(metrics.hex)
        append('\n')
        append("HEXA: #")
        append(metrics.hexa)
        append('\n')
        append("RGB: ")
        append(metrics.rgb)
        append('\n')
        append("RGBA: ")
        append(metrics.rgba)
        append('\n')
        append("HSB: ")
        append(metrics.hsb)
        append('\n')
        append("HSL: ")
        append(metrics.hsl)
        append('\n')
        append("LAB: ")
        append(metrics.lab)
        append('\n')
        append("ΔE00 · 白底: ")
        append(metrics.deltaE00OnWhite)
        append('\n')
        append("ΔE00 · 黑底: ")
        append(metrics.deltaE00OnBlack)
        append('\n')
        append("CMYK: ")
        append(metrics.cmyk)
        append('\n')
        append('\n')
        append("对白底对比度: ")
        append(metrics.contrastOnWhite)
        append('\n')
        append("对白底 WCAG: ")
        append(metrics.wcagOnWhite)
        append('\n')
        append("对黑底对比度: ")
        append(metrics.contrastOnBlack)
        append('\n')
        append("对黑底 WCAG: ")
        append(metrics.wcagOnBlack)
        append('\n')
        append("推荐文字: ")
        append(metrics.wcagRecommendedText)
    }
}

private fun buildColorCardMetricInfoTextCompactForSharing(
    colorInt: Int,
    metrics: ColorCardMetrics
): String {
    val harmonySwatches = resolveColorCardHarmonySwatches(colorInt)
    return buildString {
        append(buildColorCardMetricInfoTextCompact(colorInt = colorInt, metrics = metrics))
        append('\n')
        append('\n')
        append("相关配色:")
        harmonySwatches.forEach { swatch ->
            append('\n')
            append(swatch.label)
            append(": ")
            append(swatch.hex)
        }
    }
}

@Composable
private fun ColorCardMetricInfoDialogCompact(
    colorInt: Int,
    metrics: ColorCardMetrics,
    onDismissRequest: () -> Unit,
    onCopy: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val harmonySwatches = remember(colorInt) { resolveColorCardHarmonySwatches(colorInt) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = colorCardStrings.detailsTitle)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ColorCardMetricInfoRow(label = "HEX", value = "#${metrics.hex}")
                ColorCardMetricInfoRow(label = "HEXA", value = "#${metrics.hexa}")
                ColorCardMetricInfoRow(label = "RGB", value = metrics.rgb)
                ColorCardMetricInfoRow(label = "RGBA", value = metrics.rgba)
                ColorCardMetricInfoRow(label = "HSB", value = metrics.hsb)
                ColorCardMetricInfoRow(label = "HSL", value = metrics.hsl)
                ColorCardMetricInfoRow(label = "LAB", value = metrics.lab)
                ColorCardMetricInfoRow(label = "ΔE00 · 白底", value = metrics.deltaE00OnWhite)
                ColorCardMetricInfoRow(label = "ΔE00 · 黑底", value = metrics.deltaE00OnBlack)
                ColorCardMetricInfoRow(label = "CMYK", value = metrics.cmyk)
                ColorCardHarmonyInfoSection(swatches = harmonySwatches)
                ColorCardMetricInfoRow(
                    label = "\u5bf9\u6bd4\u5ea6 \u00b7 \u767d\u5e95",
                    value = metrics.contrastOnWhite
                )
                ColorCardMetricInfoRow(
                    label = "WCAG \u00b7 \u767d\u5e95",
                    value = metrics.wcagOnWhite
                )
                ColorCardMetricInfoRow(
                    label = "\u5bf9\u6bd4\u5ea6 \u00b7 \u9ed1\u5e95",
                    value = metrics.contrastOnBlack
                )
                ColorCardMetricInfoRow(
                    label = "WCAG \u00b7 \u9ed1\u5e95",
                    value = metrics.wcagOnBlack
                )
                ColorCardMetricInfoRow(
                    label = "\u63a8\u8350\u6587\u5b57",
                    value = metrics.wcagRecommendedText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text(text = colorCardStrings.copyAll)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = colorCardStrings.close)
            }
        }
    )
}

@Composable
private fun ColorCardMetricInfoDialog(
    colorInt: Int,
    metrics: ColorCardMetrics,
    displayProfile: ColorCardDisplayProfile,
    onDismissRequest: () -> Unit,
    onCopy: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val harmonySwatches = remember(colorInt) { resolveColorCardHarmonySwatches(colorInt) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = colorCardStrings.detailsTitle)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ColorCardMetricInfoRow(label = "HEX", value = "#${metrics.hex}")
                ColorCardMetricInfoRow(label = "HEXA", value = "#${metrics.hexa}")
                ColorCardMetricInfoRow(label = "RGB", value = metrics.rgb)
                ColorCardMetricInfoRow(label = "RGBA", value = metrics.rgba)
                ColorCardMetricInfoRow(label = "HSB", value = metrics.hsb)
                ColorCardMetricInfoRow(label = "HSL", value = metrics.hsl)
                ColorCardMetricInfoRow(label = "LAB", value = metrics.lab)
                ColorCardMetricInfoRow(label = "ΔE00 · 白底", value = metrics.deltaE00OnWhite)
                ColorCardMetricInfoRow(label = "ΔE00 · 黑底", value = metrics.deltaE00OnBlack)
                ColorCardMetricInfoRow(label = "CMYK", value = metrics.cmyk)
                ColorCardHarmonyInfoSection(swatches = harmonySwatches)
                ColorCardMetricInfoRow(
                    label = "颜色基准",
                    value = displayProfile.referenceColorSpace
                )
                ColorCardMetricInfoRow(
                    label = "设备广色域",
                    value = displayProfile.deviceWideGamut
                )
                ColorCardMetricInfoRow(
                    label = "当前窗口",
                    value = displayProfile.windowColorMode
                )
                ColorCardMetricInfoRow(
                    label = "显示说明",
                    value = displayProfile.renderingNote
                )
                ColorCardMetricInfoRow(
                    label = "\u5bf9\u6bd4\u5ea6 \u00b7 \u767d\u5e95",
                    value = metrics.contrastOnWhite
                )
                ColorCardMetricInfoRow(
                    label = "WCAG \u00b7 \u767d\u5e95",
                    value = metrics.wcagOnWhite
                )
                ColorCardMetricInfoRow(
                    label = "\u5bf9\u6bd4\u5ea6 \u00b7 \u9ed1\u5e95",
                    value = metrics.contrastOnBlack
                )
                ColorCardMetricInfoRow(
                    label = "WCAG \u00b7 \u9ed1\u5e95",
                    value = metrics.wcagOnBlack
                )
                ColorCardMetricInfoRow(
                    label = "\u63a8\u8350\u6587\u5b57",
                    value = metrics.wcagRecommendedText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text(text = colorCardStrings.copyAll)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = colorCardStrings.close)
            }
        }
    )
}

@Composable
private fun ColorCardHarmonyInfoSection(
    swatches: List<ColorCardHarmonySwatch>
) {
    if (swatches.isEmpty()) return
    val appLanguage = LocalAppLanguage.current
    val colorCardStrings = remember(appLanguage) { getColorCardUiStrings(appLanguage) }
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = colorCardStrings.relatedColors,
            color = secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        swatches.forEach { swatch ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(swatch.colorInt))
                        .colorCardSwatchOutline(
                            colorInt = swatch.colorInt,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Text(
                    text = swatch.label,
                    color = secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(58.dp)
                )
                Text(
                    text = swatch.hex,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ColorCardMetricInfoRow(
    label: String,
    value: String
) {
    val secondaryText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ColorCardAllPageIndicator(
    selected: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = if (selected) 0.22f else 0.08f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = contentColor.copy(alpha = if (selected) 0.96f else 0.34f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ALL",
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ColorCardGradientPageIndicator(
    selected: Boolean,
    contentColor: Color,
    gradientSpec: ColorCardGradientSpec,
    onClick: () -> Unit
) {
    ColorCardGradientPreview(
        gradientSpec = gradientSpec,
        modifier = Modifier
            .size(if (selected) 34.dp else 28.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        borderColor = contentColor.copy(alpha = if (selected) 0.96f else 0.34f)
    )
}

@Composable
private fun ColorCardIconActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ColorCardDisplayStatusChip(
    label: String,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(contentColor.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun colorCardOverviewContentColor(colors: List<Int>): Color {
    if (colors.isEmpty()) return Color.White
    val averageLuminance = colors.map { Color(it).luminance() }.average().toFloat()
    return if (averageLuminance > 0.58f) Color.Black else Color.White
}

private fun buildColorCardShareSpec(
    target: ColorCardDetailTarget,
    colorIndex: Int,
    exportAll: Boolean
): ColorCardShareSpec {
    return when (target) {
        is ColorCardDetailTarget.Mine -> {
            if (exportAll) {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa PHOTOO\u8272\u5361",
                    subject = "PHOTOO\u8272\u5361",
                    text = buildPaletteDetailText(target.palette)
                )
            } else {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa PHOTOO\u8272\u5361\u989c\u8272",
                    subject = "PHOTOO\u8272\u5361",
                    text = buildSingleColorDetailText(target.palette, colorIndex)
                )
            }
        }
        is ColorCardDetailTarget.Inspiration -> {
            ColorCardShareSpec(
                chooserTitle = "导出${target.entry.sourceTag}",
                subject = target.entry.sourceTag,
                text = buildChineseColorDetailText(target.entry)
            )
        }
        is ColorCardDetailTarget.TopJournal -> {
            if (exportAll) {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa\u9876\u520a\u914d\u8272",
                    subject = "\u9876\u520a\u914d\u8272",
                    text = buildTopJournalPaletteDetailText(target.entry)
                )
            } else {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa\u9876\u520a\u914d\u8272",
                    subject = "\u9876\u520a\u914d\u8272",
                    text = buildTopJournalSingleColorDetailText(target.entry, colorIndex)
                )
            }
        }
        is ColorCardDetailTarget.Nice -> {
            if (exportAll) {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa NICE \u914d\u8272",
                    subject = "NICE",
                    text = buildNicePaletteDetailText(target.entry)
                )
            } else {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa NICE \u914d\u8272",
                    subject = "NICE",
                    text = buildNiceSingleColorDetailText(target.entry, colorIndex)
                )
            }
        }
        is ColorCardDetailTarget.Nord -> {
            if (exportAll) {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa Nord \u914d\u8272",
                    subject = "Nord",
                    text = buildNordPaletteDetailText(target.entry)
                )
            } else {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa Nord \u914d\u8272",
                    subject = "Nord",
                    text = buildNordSingleColorDetailText(target.entry, colorIndex)
                )
            }
        }
        is ColorCardDetailTarget.Paletteer -> {
            if (exportAll) {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa灵感配色",
                    subject = "灵感",
                    text = buildPaletteerPaletteDetailText(target.entry)
                )
            } else {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa灵感配色",
                    subject = "灵感",
                    text = buildPaletteerSingleColorDetailText(target.entry, colorIndex)
                )
            }
        }
        is ColorCardDetailTarget.Gradient -> {
            if (exportAll) {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa\u6e10\u53d8\u8272\u5361",
                    subject = "\u6e10\u53d8\u8272\u5361",
                    text = buildGradientInspirationPaletteDetailText(target.entry)
                )
            } else {
                ColorCardShareSpec(
                    chooserTitle = "\u5bfc\u51fa\u6e10\u53d8\u989c\u8272",
                    subject = "\u6e10\u53d8\u8272\u5361",
                    text = buildGradientInspirationSingleColorDetailText(target.entry, colorIndex)
                )
            }
        }
    }
}

private fun buildColorCardGradientShareSpec(
    target: ColorCardDetailTarget,
    gradientSpec: ColorCardGradientSpec
): ColorCardShareSpec {
    return when (target) {
        is ColorCardDetailTarget.Mine -> ColorCardShareSpec(
            chooserTitle = "\u5bfc\u51fa PHOTOO\u6e10\u53d8\u914d\u65b9",
            subject = "PHOTOO\u6e10\u53d8\u914d\u65b9",
            text = buildColorCardGradientDetailText(
                title = target.palette.title ?: "PHOTOO\u6e10\u53d8",
                sourceTag = target.palette.sourceTag,
                gradientSpec = gradientSpec
            )
        )
        is ColorCardDetailTarget.Inspiration -> ColorCardShareSpec(
            chooserTitle = "\u5bfc\u51fa\u6e10\u53d8\u914d\u65b9",
            subject = "\u6e10\u53d8\u914d\u65b9",
            text = buildColorCardGradientDetailText(
                title = target.entry.name,
                sourceTag = target.entry.sourceTag,
                gradientSpec = gradientSpec
            )
        )
        is ColorCardDetailTarget.TopJournal -> ColorCardShareSpec(
            chooserTitle = "\u5bfc\u51fa\u6e10\u53d8\u914d\u65b9",
            subject = "\u6e10\u53d8\u914d\u65b9",
            text = buildColorCardGradientDetailText(
                title = target.entry.title,
                sourceTag = "\u9876\u520a\u914d\u8272",
                gradientSpec = gradientSpec
            )
        )
        is ColorCardDetailTarget.Nice -> ColorCardShareSpec(
            chooserTitle = "\u5bfc\u51fa\u6e10\u53d8\u914d\u65b9",
            subject = "\u6e10\u53d8\u914d\u65b9",
            text = buildColorCardGradientDetailText(
                title = target.entry.title,
                sourceTag = "NICE",
                gradientSpec = gradientSpec
            )
        )
        is ColorCardDetailTarget.Nord -> ColorCardShareSpec(
            chooserTitle = "\u5bfc\u51fa\u6e10\u53d8\u914d\u65b9",
            subject = "\u6e10\u53d8\u914d\u65b9",
            text = buildColorCardGradientDetailText(
                title = target.entry.title,
                sourceTag = "Nord",
                gradientSpec = gradientSpec
            )
        )
        is ColorCardDetailTarget.Paletteer -> ColorCardShareSpec(
            chooserTitle = "\u5bfc\u51fa\u6e10\u53d8\u914d\u65b9",
            subject = "\u6e10\u53d8\u914d\u65b9",
            text = buildColorCardGradientDetailText(
                title = target.entry.paletteName,
                sourceTag = target.entry.collectionName,
                gradientSpec = gradientSpec
            )
        )
        is ColorCardDetailTarget.Gradient -> ColorCardShareSpec(
            chooserTitle = "\u5bfc\u51fa\u6e10\u53d8\u914d\u65b9",
            subject = "\u6e10\u53d8\u914d\u65b9",
            text = buildColorCardGradientDetailText(
                title = target.entry.title,
                sourceTag = target.entry.sourceName,
                gradientSpec = gradientSpec
            )
        )
    }
}

private fun buildColorCardFavoriteSpec(
    entry: ColorCardInspirationFeedEntry
): ColorCardFavoriteSpec {
    return when (val target = entry.target) {
        is ColorCardDetailTarget.Inspiration -> ColorCardFavoriteSpec(
            key = entry.key,
            title = target.entry.name,
            sourceTag = entry.sourceTag,
            useChineseFont = true,
            colors = listOf(target.entry.color)
        )
        is ColorCardDetailTarget.TopJournal -> ColorCardFavoriteSpec(
            key = entry.key,
            title = target.entry.title,
            sourceTag = entry.sourceTag,
            useChineseFont = false,
            colors = target.entry.colors
        )
        is ColorCardDetailTarget.Nice -> ColorCardFavoriteSpec(
            key = entry.key,
            title = target.entry.title,
            sourceTag = entry.sourceTag,
            useChineseFont = false,
            colors = target.entry.colors
        )
        is ColorCardDetailTarget.Nord -> ColorCardFavoriteSpec(
            key = entry.key,
            title = target.entry.title,
            sourceTag = entry.sourceTag,
            useChineseFont = false,
            colors = target.entry.colors
        )
        is ColorCardDetailTarget.Paletteer -> ColorCardFavoriteSpec(
            key = entry.key,
            title = if (target.entry.sourceTag == "#灵感") {
                "${target.entry.collectionName}\n${target.entry.paletteName}"
            } else {
                target.entry.paletteName
            },
            sourceTag = entry.sourceTag,
            useChineseFont = false,
            colors = target.entry.colors
        )
        is ColorCardDetailTarget.Gradient -> ColorCardFavoriteSpec(
            key = entry.key,
            title = target.entry.title,
            sourceTag = target.entry.sourceName,
            useChineseFont = false,
            colors = target.entry.colors,
            gradientSpec = target.entry.gradientSpec
        )
        is ColorCardDetailTarget.Mine -> ColorCardFavoriteSpec(
            key = entry.key,
            title = target.palette.title ?: "\u8272\u5361",
            sourceTag = target.palette.sourceTag ?: entry.sourceTag,
            useChineseFont = target.palette.useChineseFont,
            colors = target.palette.colors,
            gradientSpec = target.palette.gradientSpec
        )
    }
}

@Composable
private fun ColorMetricBlock(
    label: String,
    icon: ImageVector? = null,
    value: String,
    contentColor: Color,
    secondaryText: Color,
    onValueLongPress: (() -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { imageVector ->
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = label,
                color = secondaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            color = contentColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = if (onValueLongPress != null) {
                Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onValueLongPress
                )
            } else {
                Modifier
            }
        )
    }
}

@Composable
private fun ColorCardActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    compact: Boolean = false,
    customContainerColor: Color? = null,
    customContentColor: Color? = null
) {
    val isDark = isSystemInDarkTheme()
    val defaultContainer = when {
        customContainerColor != null -> customContainerColor
        destructive -> Color(0xFFD64545).copy(alpha = if (isDark) 0.18f else 0.10f)
        isDark -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        else -> Color.Black.copy(alpha = 0.05f)
    }
    val defaultContent = when {
        customContentColor != null -> customContentColor
        destructive -> Color(0xFFD64545)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val shape = RoundedCornerShape(if (compact) 18.dp else 20.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(defaultContainer)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 9.dp else 11.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = defaultContent,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColorCardImportedImagePickerOverlay(
    source: ColorCardImportedImageSource,
    onDismissRequest: () -> Unit,
    onReplaceSource: (ColorCardImportedImageSource) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { ColorCardStore.get(context) }
    val savedPalettes by store.palettes.collectAsState()
    val recipeStore = remember(context) { ColorCardRecipeStore.get(context) }
    val savedRecipes by recipeStore.recipes.collectAsState()
    val density = LocalDensity.current
    val hostView = LocalView.current
    val rootWindowView = hostView.rootView
    val activity = remember(context) { context.findActivity() }
    val isDark = isSystemInDarkTheme()
    DisposableEffect(activity, source.sessionId) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    var backdropBitmap by remember(source.sessionId) { mutableStateOf<Bitmap?>(null) }
    var visualReady by remember(source.sessionId) { mutableStateOf(false) }
    var overlayVisible by remember(source.sessionId) { mutableStateOf(true) }
    var dismissScheduled by remember(source.sessionId) { mutableStateOf(false) }
    val latestOnDismissRequest = rememberUpdatedState(onDismissRequest)
    val windowSizePx = remember(
        rootWindowView.width,
        rootWindowView.height,
        source.sessionId
    ) {
        IntSize(
            width = rootWindowView.width.coerceAtLeast(1),
            height = rootWindowView.height.coerceAtLeast(1)
        )
    }
    val imageBounds = remember(
        density,
        windowSizePx,
        source.width,
        source.height
    ) {
        resolveImportedColorCardPickerBounds(
            density = density,
            windowSizePx = windowSizePx,
            imageWidth = source.width,
            imageHeight = source.height
        )
    }
    val adjustedImageBounds = rememberColorCardTraceAdjustedImageBounds(bounds = imageBounds)
    val editingItem = remember(source.sessionId, source.sourceMediaItem?.id) {
        source.toImportedMediaItem()
    }
    val colorPickerState = rememberHomeCardColorPickerState(
        active = overlayVisible,
        item = editingItem,
        sourceMediaId = null,
        persistenceKey = "imported_color_card_picker"
    )
    val appLanguage = LocalAppLanguage.current
    val colorCardTraceCopy = remember(appLanguage) { getColorCardTraceCopy(appLanguage) }
    val colorCardPageCopy = remember(appLanguage) { getColorCardPageGlobalCopy(appLanguage) }
    val lensyEnabled = remember(context, appLanguage) {
        isLensySupported(appLanguage) && AiHomepageDisplayPreferences(context).lensyEnabled
    }
    var panelMode by remember(source.sessionId) {
        mutableStateOf(source.initialPanelMode ?: ColorCardTracePanelMode.Picker)
    }
    var effectPanelMode by remember(source.sessionId) {
        mutableStateOf(source.initialPanelMode ?: ColorCardTracePanelMode.Picker)
    }
    var pendingRecipeToApply by remember(source.sessionId) { mutableStateOf<SavedColorCardRecipe?>(null) }
    var showDiscardEditDialog by remember(source.sessionId) { mutableStateOf(false) }
    var showRecipeImportDialog by remember(source.sessionId) { mutableStateOf(false) }
    var recipeImportText by remember(source.sessionId) { mutableStateOf("") }
    var portraitPanelExpanded by remember(source.sessionId) { mutableStateOf(false) }
    var lensyDismissedForSession by remember(source.sessionId) { mutableStateOf(false) }
    val hoverVisuals = rememberHomeCardLongPressHoverVisuals(active = visualReady && overlayVisible)
    var saveImageState by remember(source.sessionId) {
        mutableStateOf(ColorCardTraceSaveImageState.Idle)
    }
    var comparePressed by remember(source.sessionId) { mutableStateOf(false) }
    var exportSuccessPayload by remember(source.sessionId) {
        mutableStateOf<ColorCardTraceExportSuccessPayload?>(null)
    }
    var showMotionPhotoLab by remember(source.sessionId) { mutableStateOf(false) }
    var showDynamicExportMenu by remember(source.sessionId) { mutableStateOf(false) }
    var imitateReferenceImage by remember(source.sessionId) { mutableStateOf<ColorCardTemporaryImage?>(null) }
    var imitateReferenceLoading by remember(source.sessionId) { mutableStateOf(false) }
    var imitateAnalysis by remember(source.sessionId) { mutableStateOf<ColorCardImitateAnalysis?>(null) }
    val clearImitateReference: () -> Unit = {
        imitateReferenceImage?.let(::cleanupColorCardTemporaryImage)
        imitateReferenceImage = null
        imitateAnalysis = null
        imitateReferenceLoading = false
    }
    val imitateReferencePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            imitateReferenceLoading = true
            var importedReference: ColorCardTemporaryImage? = null
            runCatching {
                val referenceImage = importColorCardTemporaryImage(
                    context = context.applicationContext,
                    sourceUri = uri,
                    tempPrefix = "imported_imitate_reference"
                )
                importedReference = referenceImage
                val analysis = withContext(Dispatchers.Default) {
                    analyzeColorCardImitateStyle(
                        sourceFile = source.tempFile,
                        referenceFile = referenceImage.tempFile
                    )
                } ?: error("Reference analysis failed")
                referenceImage to analysis
            }.onSuccess { (referenceImage, analysis) ->
                imitateReferenceImage?.let(::cleanupColorCardTemporaryImage)
                imitateReferenceImage = referenceImage
                imitateAnalysis = analysis
            }.onFailure {
                importedReference?.let(::cleanupColorCardTemporaryImage)
                Toast.makeText(context, colorCardTraceCopy.referenceAnalysisFailed, Toast.LENGTH_SHORT).show()
            }
            imitateReferenceLoading = false
        }
    }
    val changeImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                importColorCardPickedImage(
                    context = context.applicationContext,
                    sourceUri = uri
                )
            }.onSuccess { replacement ->
                exportSuccessPayload?.release()
                exportSuccessPayload = null
                onReplaceSource(replacement)
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: colorCardTraceCopy.openImageFailed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val imageRequest = remember(context, source.sessionId, source.uri) {
        buildHomeCardHoverStillImageRequest(
            context = context,
            uri = source.uri,
            itemId = source.sessionId,
            crossfade = false
        )
    }
    DisposableEffect(imitateReferenceImage) {
        val imageToCleanup = imitateReferenceImage
        onDispose {
            cleanupColorCardTemporaryImage(imageToCleanup)
        }
    }
    DisposableEffect(exportSuccessPayload) {
        val payloadToRelease = exportSuccessPayload
        onDispose {
            payloadToRelease?.release()
        }
    }
    LaunchedEffect(source.sessionId) {
        overlayVisible = true
        dismissScheduled = false
    }
    var initialFilterApplied by remember(source.sessionId) { mutableStateOf(false) }
    val traceFilterState = rememberColorCardTraceFilterState(
        active = overlayVisible,
        panelMode = effectPanelMode,
        sourceUri = source.uri,
        sourceFile = source.tempFile,
        sourceSessionId = source.sessionId,
        sourceColors = if (colorPickerState.isReady) colorPickerState.colors else emptyList(),
        savedPalettes = savedPalettes,
        imitateReferenceLabel = imitateReferenceImage?.displayName,
        imitateAnalysis = imitateAnalysis,
        imitateReferenceLoading = imitateReferenceLoading,
        initialBuiltinPaletteId = source.initialPaletteId
    )
    LaunchedEffect(
        source.initialPaletteId,
        traceFilterState.candidates,
        traceFilterState.selectedCandidate?.paletteId,
        initialFilterApplied
    ) {
        val initialPaletteId = source.initialPaletteId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (initialFilterApplied) return@LaunchedEffect
        val candidateExists = traceFilterState.candidates.any { it.paletteId == initialPaletteId }
        if (!candidateExists) {
            if (traceFilterState.candidates.isNotEmpty()) {
                initialFilterApplied = true
            }
            return@LaunchedEffect
        }
        panelMode = ColorCardTracePanelMode.Filter
        effectPanelMode = ColorCardTracePanelMode.Filter
        if (traceFilterState.selectedCandidate?.paletteId != initialPaletteId) {
            traceFilterState.selectCandidate(initialPaletteId)
            return@LaunchedEffect
        }
        initialFilterApplied = true
    }
    val lensyAssessment = rememberColorCardLensyAssessment(
        enabled = lensyEnabled,
        effectPanelMode = effectPanelMode,
        traceState = traceFilterState
    )
    val isDynamicSource = editingItem.isLivePhoto || editingItem.isMotionPhoto
    val isColorAwakeningSelected =
        traceFilterState.selectedCandidate?.kind == ColorCardTraceFilterKind.MotionPhotoEntry
    val dynamicSourceItem = source.sourceMediaItem ?: editingItem
    var renderedBitmap: Bitmap? = null
    val dynamicSaveActions = remember(
        isDynamicSource,
        isColorAwakeningSelected,
        editingItem.isLivePhoto,
        editingItem.isMotionPhoto
    ) {
        if (!isDynamicSource || isColorAwakeningSelected) {
            emptyList()
        } else {
            listOf(
                ColorCardTraceSaveMenuAction(
                    id = "image",
                    title = colorCardTraceCopy.exportImageTitle,
                    subtitle = colorCardTraceCopy.exportImageSubtitle
                ),
                ColorCardTraceSaveMenuAction(
                    id = "live",
                    title = colorCardTraceCopy.exportLiveTitle,
                    subtitle = colorCardTraceCopy.exportLiveSubtitle
                )
            )
        }
    }
    val resolvedDynamicSaveActions = remember(dynamicSaveActions) {
        dynamicSaveActions.map { action ->
            if (action.id == "live") {
                action.copy(
                    title = colorCardTraceCopy.exportLiveTitle,
                    subtitle = colorCardTraceCopy.exportLiveAdjustedSubtitle
                )
            } else {
                action
            }
        }
    }
    val sessionLensyMessage = if (lensyDismissedForSession) null else lensyAssessment?.message
    val performDismiss: () -> Unit = {
        if (!dismissScheduled) {
            dismissScheduled = true
            overlayVisible = false
            scope.launch {
                delay(COLOR_CARD_IMPORTED_OVERLAY_DISMISS_DELAY_MS)
                latestOnDismissRequest.value()
            }
        }
    }
    val requestDismiss: () -> Unit = {
        if (traceFilterState.hasUserEdits) {
            showDiscardEditDialog = true
        } else {
            performDismiss()
        }
    }

    BackHandler(enabled = !dismissScheduled) {
        if (showDynamicExportMenu) {
            showDynamicExportMenu = false
            return@BackHandler
        }
        requestDismiss()
    }

    LaunchedEffect(isColorAwakeningSelected) {
        if (isColorAwakeningSelected) {
            showDynamicExportMenu = false
        }
    }

    LaunchedEffect(source.sessionId, activity, rootWindowView) {
        visualReady = false
        val fallbackBackdrop = captureHomeCardLongPressBackdropBitmapFallback(rootWindowView)
        backdropBitmap = fallbackBackdrop
        val captured = captureHomeCardLongPressBackdropBitmap(
            activity = activity,
            rootView = rootWindowView,
            fallbackBitmap = fallbackBackdrop
        )
        if (captured != null) {
            backdropBitmap = captured
        }
    }
    LaunchedEffect(traceFilterState.mode, traceFilterState.candidates, pendingRecipeToApply?.id) {
        val recipe = pendingRecipeToApply ?: return@LaunchedEffect
        val expectedMode = colorCardRecipeModeToPanelMode(recipe.mode)
        if (traceFilterState.mode != expectedMode) return@LaunchedEffect
        if (traceFilterState.isAnalyzing) return@LaunchedEffect
        if (traceFilterState.candidates.none { it.paletteId == recipe.paletteId }) {
            Toast.makeText(context, colorCardTraceCopy.recipeTargetNotFound, Toast.LENGTH_SHORT).show()
            pendingRecipeToApply = null
            return@LaunchedEffect
        }
        val snappedColor = (((recipe.color.coerceIn(-1f, 1f) + 1f) / 2f) * 10f).roundToInt() / 10f
        val snappedTone = (((recipe.tone.coerceIn(-1f, 1f) + 1f) / 2f) * 10f).roundToInt() / 10f
        traceFilterState.selectCandidate(recipe.paletteId)
        traceFilterState.updateSharpen(recipe.sharpen)
        traceFilterState.updateSaturation(recipe.saturation)
        traceFilterState.updateBrightness(recipe.brightness)
        traceFilterState.updatePaletteStrength(recipe.paletteStrength)
        traceFilterState.updatePortraitVitality(recipe.portraitVitality)
        traceFilterState.updatePortraitSoftness(recipe.portraitSoftness)
        traceFilterState.updatePortraitDetailSuppression(recipe.portraitDetailSuppression)
        recipe.filmGrainAssetId?.let { assetId ->
            traceFilterState.selectFilmGrainAsset(assetId)
        }
        traceFilterState.setFilmGrainEnabled(recipe.filmGrainEnabled)
        traceFilterState.updateAdjustment(
            snappedColor.coerceIn(0f, 1f),
            (1f - snappedTone).coerceIn(0f, 1f)
        )
        pendingRecipeToApply = null
    }

    val saveRenderedStill: () -> Unit = saveRenderedStill@{
        saveImageState = ColorCardTraceSaveImageState.Saving
        scope.launch {
            var renderedBitmap: Bitmap? = null
            runCatching {
                renderedBitmap = renderColorCardTraceModeBitmap(
                    context = context.applicationContext,
                    sourceFile = source.tempFile,
                    traceState = traceFilterState
                )
            }.getOrElse {
                Toast.makeText(context, colorCardTraceCopy.renderFailed, Toast.LENGTH_SHORT).show()
                saveImageState = ColorCardTraceSaveImageState.Idle
                return@launch
            }
            if (renderedBitmap == null) {
                Toast.makeText(context, colorCardTraceCopy.nothingToSave, Toast.LENGTH_SHORT).show()
                saveImageState = ColorCardTraceSaveImageState.Idle
                return@launch
            }
            val safeRenderedBitmap = renderedBitmap ?: return@launch
            try {
                val savedUri = saveColorCardRenderedBitmap(
                    context = context.applicationContext,
                    bitmap = safeRenderedBitmap,
                    baseName = editingItem.displayName,
                    sourceUri = editingItem.uri
                )
                saveImageState = if (savedUri != null) {
                    ColorCardTraceSaveImageState.Saved
                } else {
                    ColorCardTraceSaveImageState.Idle
                }
                if (savedUri != null) {
                    val exportRecipe = buildColorCardTraceRecipeFromCurrent(
                        mode = effectPanelMode,
                        traceState = traceFilterState
                    )
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = safeRenderedBitmap,
                        savedUri = savedUri,
                        displayName = editingItem.displayName,
                        recipe = exportRecipe
                    )
                    delay(800)
                } else {
                    Toast.makeText(context, colorCardTraceCopy.saveFailed, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, colorCardTraceCopy.renderFailed, Toast.LENGTH_SHORT).show()
                saveImageState = ColorCardTraceSaveImageState.Idle
            } finally {
                safeRenderedBitmap.recycle()
                if (saveImageState == ColorCardTraceSaveImageState.Saved) {
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }
            }
        }
    }

    val saveRenderedLive: () -> Unit = saveRenderedLive@{
        val dynamicSourceItem = source.sourceMediaItem ?: editingItem
        saveImageState = ColorCardTraceSaveImageState.Saving
        scope.launch {
            var renderedBitmap: Bitmap? = null
            val session = MotionPhotoFileRepository(context.applicationContext)
                .createSession(editingItem.displayName)
            if (isColorAwakeningSelected) {
                runCatching {
                    withContext(Dispatchers.Default) {
                        MotionPhotoExportCoordinator.export(
                            context = context.applicationContext,
                            sourceFile = source.tempFile,
                            fallbackBitmap = traceFilterState.previewSourceBitmap,
                            displayName = editingItem.displayName,
                            session = session,
                            spec = MotionPhotoRenderSpec.dynamicSave(
                                revealPattern = traceFilterState.motionPhotoRevealPattern,
                                revealSeed = kotlin.random.Random.nextLong()
                            ),
                            onProgress = {}
                        )
                    }
                }.onSuccess { result ->
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = result.previewBitmap,
                        savedUri = result.savedUri,
                        displayName = result.displayName,
                        recipe = null
                    )
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }.onFailure { error ->
                    saveImageState = ColorCardTraceSaveImageState.Idle
                    Toast.makeText(
                        context,
                        error.toDynamicPhotoExportToastMessage("Live Photo"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (dynamicSourceItem.isLivePhoto || dynamicSourceItem.isMotionPhoto) {
                runCatching {
                    val safeRenderedBitmap = renderColorCardTraceModeBitmap(
                        context = context.applicationContext,
                        sourceFile = source.tempFile,
                        traceState = traceFilterState
                    ) ?: error("Nothing to save in this mode")
                    renderedBitmap = safeRenderedBitmap
                    MotionPhotoExportCoordinator.exportRenderedDynamicPhoto(
                        context = context.applicationContext,
                        sourceItem = dynamicSourceItem,
                        renderedCoverBitmap = safeRenderedBitmap,
                        traceState = traceFilterState,
                        displayName = editingItem.displayName,
                        session = session,
                        spec = MotionPhotoRenderSpec.dynamicSave(
                            revealPattern = traceFilterState.motionPhotoRevealPattern,
                            revealSeed = kotlin.random.Random.nextLong()
                        ),
                        onProgress = {}
                    )
                }.onSuccess { result ->
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = result.previewBitmap,
                        savedUri = result.primarySavedUri,
                        displayName = result.displayName,
                        recipe = null
                    )
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }.onFailure { error ->
                    saveImageState = ColorCardTraceSaveImageState.Idle
                    Toast.makeText(
                        context,
                        error.toDynamicPhotoExportToastMessage("Motion Photo"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                runCatching {
                    MotionPhotoExportCoordinator.export(
                        context = context.applicationContext,
                        sourceFile = source.tempFile,
                        fallbackBitmap = traceFilterState.previewSourceBitmap,
                        displayName = editingItem.displayName,
                        session = session,
                        spec = MotionPhotoRenderSpec(),
                        onProgress = {}
                    )
                }.onSuccess { result ->
                    exportSuccessPayload?.release()
                    exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                        beforeBitmap = traceFilterState.previewSourceBitmap,
                        afterBitmap = result.previewBitmap,
                        savedUri = result.savedUri,
                        displayName = result.displayName,
                        recipe = null
                    )
                    saveImageState = ColorCardTraceSaveImageState.Idle
                }.onFailure { error ->
                    saveImageState = ColorCardTraceSaveImageState.Idle
                    Toast.makeText(
                        context,
                        error.toDynamicPhotoExportToastMessage("Live Photo"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val handleSaveRequest: () -> Unit = handleSaveRequest@{
        comparePressed = false
        if (saveImageState == ColorCardTraceSaveImageState.Saving) {
            return@handleSaveRequest
        }
        if (isColorAwakeningSelected) {
            saveRenderedLive()
            return@handleSaveRequest
        }
        if (isDynamicSource) {
            showDynamicExportMenu = !showDynamicExportMenu
            return@handleSaveRequest
        }
        saveRenderedStill()
    }

    HomeCardLongPressHoverOverlay(
        active = overlayVisible && exportSuccessPayload == null,
        visuals = hoverVisuals,
        backdropBitmap = backdropBitmap,
        imageRequest = imageRequest,
        imageBoundsInRoot = adjustedImageBounds,
        windowSizePx = windowSizePx,
        isDark = isDark,
        quote = null,
        canRefreshQuote = false,
        onQuoteRefreshRequest = {},
        dismissOnBackdropTap = false,
        showCloseButton = false,
        imageOverlayContent = {
            ColorCardTraceFilterImageOverlay(
                traceState = traceFilterState,
                colorPickerState = colorPickerState,
                panelMode = effectPanelMode,
                comparePressed = comparePressed,
                showPortraitFaces = portraitPanelExpanded,
                showLiveBadge = isDynamicSource
            )
        },
        footerOverlayContent = {
            ColorCardTraceEditingTopBar(
                isDark = isDark,
                panelMode = panelMode,
                saveState = saveImageState,
                saveEnabled = if (panelMode == ColorCardTracePanelMode.Picker) {
                    colorPickerState.isReady
                } else {
                    traceFilterState.canSaveRenderedImage
                },
                saveMenuExpanded = showDynamicExportMenu,
                saveMenuActions = resolvedDynamicSaveActions,
                onBack = requestDismiss,
                onSave = {
                    if (panelMode == ColorCardTracePanelMode.Picker) {
                        if (colorPickerState.isReady) {
                            colorPickerState.save()
                        }
                        return@ColorCardTraceEditingTopBar
                    }
                    handleSaveRequest()
                    return@ColorCardTraceEditingTopBar
                    comparePressed = false
                    if (saveImageState == ColorCardTraceSaveImageState.Saving) {
                        return@ColorCardTraceEditingTopBar
                    }
                    if (traceFilterState.selectedCandidate?.kind == ColorCardTraceFilterKind.MotionPhotoEntry) {
                        saveImageState = ColorCardTraceSaveImageState.Saving
                        scope.launch {
                            val session = MotionPhotoFileRepository(context.applicationContext)
                                .createSession(source.displayName)
                            runCatching {
                                MotionPhotoExportCoordinator.export(
                                    context = context.applicationContext,
                                    sourceFile = source.tempFile,
                                    fallbackBitmap = traceFilterState.previewSourceBitmap,
                                    displayName = source.displayName,
                                    session = session,
                                    spec = MotionPhotoRenderSpec(),
                                    onProgress = {}
                                )
                            }.onSuccess { result ->
                                exportSuccessPayload?.release()
                                exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                                    beforeBitmap = traceFilterState.previewSourceBitmap,
                                    afterBitmap = result.previewBitmap,
                                    savedUri = result.savedUri,
                                    displayName = result.displayName,
                                    recipe = null
                                )
                                saveImageState = ColorCardTraceSaveImageState.Idle
                            }.onFailure { error ->
                                saveImageState = ColorCardTraceSaveImageState.Idle
                                Toast.makeText(
                                    context,
                                    error.toDynamicPhotoExportToastMessage("Live Photo"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        return@ColorCardTraceEditingTopBar
                    }
                    saveImageState = ColorCardTraceSaveImageState.Saving
                    scope.launch {
                        var renderedBitmap: Bitmap? = null
                        runCatching {
                            renderedBitmap = renderColorCardTraceModeBitmap(
                                context = context.applicationContext,
                                sourceFile = source.tempFile,
                                traceState = traceFilterState
                            )
                        }.getOrElse {
                            Toast.makeText(
                                context,
                                "娣囨繂鐡ㄦ径杈Е閿涘矁顕柌宥堢槸",
                                Toast.LENGTH_SHORT
                            ).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                            return@launch
                        }
                        if (renderedBitmap == null) {
                            Toast.makeText(
                                context,
                                "褰撳墠妯″紡娌℃湁鍙繚瀛樼殑婊ら暅缁撴灉",
                                Toast.LENGTH_SHORT
                            ).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                            return@launch
                        }
                        val safeRenderedBitmap = renderedBitmap ?: return@launch
                        try {
                            val savedUri = saveColorCardRenderedBitmap(
                                context = context.applicationContext,
                                bitmap = safeRenderedBitmap,
                                baseName = source.displayName,
                                sourceUri = source.uri
                            )
                            saveImageState = if (savedUri != null) {
                                ColorCardTraceSaveImageState.Saved
                            } else {
                                ColorCardTraceSaveImageState.Idle
                            }
                            if (savedUri != null) {
                                val exportRecipe = buildColorCardTraceRecipeFromCurrent(
                                    mode = effectPanelMode,
                                    traceState = traceFilterState
                                )
                                exportSuccessPayload?.release()
                                exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                                    beforeBitmap = traceFilterState.previewSourceBitmap,
                                    afterBitmap = safeRenderedBitmap,
                                    savedUri = savedUri,
                                    displayName = source.displayName,
                                    recipe = exportRecipe
                                )
                                delay(800)
                            } else {
                                Toast.makeText(
                                    context,
                                    "淇濆瓨澶辫触锛岃閲嶈瘯",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "娣囨繂鐡ㄦ径杈Е閿涘矁顕柌宥堢槸",
                                Toast.LENGTH_SHORT
                            ).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                        } finally {
                            safeRenderedBitmap.recycle()
                            if (saveImageState == ColorCardTraceSaveImageState.Saved) {
                                saveImageState = ColorCardTraceSaveImageState.Idle
                            }
                        }
                    }
                },
                onSaveMenuDismiss = { showDynamicExportMenu = false },
                onSaveMenuAction = { actionId ->
                    when (actionId) {
                        "live" -> saveRenderedLive()
                        else -> saveRenderedStill()
                    }
                }
            )
            ColorCardTraceLensyFloatingOverlay(
                message = sessionLensyMessage,
                imageBoundsInRoot = adjustedImageBounds,
                windowSizePx = windowSizePx,
                onDismissRequest = { lensyDismissedForSession = true }
            )
            ColorCardTraceFilterDeckOverlay(
                active = overlayVisible,
                visuals = hoverVisuals,
                colorPickerState = colorPickerState,
                traceState = traceFilterState,
                isDark = isDark,
                panelMode = panelMode,
                effectPanelMode = effectPanelMode,
                recipes = savedRecipes,
                saveImageState = saveImageState,
                comparePressed = comparePressed,
                portraitPanelExpanded = portraitPanelExpanded,
                onPanelModeChange = { nextMode ->
                    panelMode = nextMode
                    if (nextMode != ColorCardTracePanelMode.Recipe) {
                        effectPanelMode = nextMode
                    }
                },
                onComparePressedChange = { comparePressed = it },
                onPortraitPanelExpandedChange = { portraitPanelExpanded = it },
                onSaveImage = {
                    handleSaveRequest()
                    return@ColorCardTraceFilterDeckOverlay
                    if (saveImageState == ColorCardTraceSaveImageState.Saving) {
                        return@ColorCardTraceFilterDeckOverlay
                    }
                    if (traceFilterState.selectedCandidate?.kind == ColorCardTraceFilterKind.MotionPhotoEntry) {
                        saveImageState = ColorCardTraceSaveImageState.Saving
                        scope.launch {
                            val session = MotionPhotoFileRepository(context.applicationContext)
                                .createSession(source.displayName)
                            runCatching {
                                MotionPhotoExportCoordinator.export(
                                    context = context.applicationContext,
                                    sourceFile = source.tempFile,
                                    fallbackBitmap = traceFilterState.previewSourceBitmap,
                                    displayName = source.displayName,
                                    session = session,
                                    spec = MotionPhotoRenderSpec(),
                                    onProgress = {}
                                )
                            }.onSuccess { result ->
                                exportSuccessPayload?.release()
                                exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                                    beforeBitmap = traceFilterState.previewSourceBitmap,
                                    afterBitmap = result.previewBitmap,
                                    savedUri = result.savedUri,
                                    displayName = result.displayName,
                                    recipe = null
                                )
                                saveImageState = ColorCardTraceSaveImageState.Idle
                            }.onFailure { error ->
                                saveImageState = ColorCardTraceSaveImageState.Idle
                                Toast.makeText(
                                    context,
                                    error.toDynamicPhotoExportToastMessage("Motion Photo"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        return@ColorCardTraceFilterDeckOverlay
                    }
                    saveImageState = ColorCardTraceSaveImageState.Saving
                    scope.launch {
                        var renderedBitmap: Bitmap? = null
                        runCatching {
                            renderedBitmap = renderColorCardTraceModeBitmap(
                                context = context.applicationContext,
                                sourceFile = source.tempFile,
                                traceState = traceFilterState
                            )
                        }.getOrElse {
                            Toast.makeText(
                                context,
                                "淇濆瓨澶辫触锛岃閲嶈瘯",
                                Toast.LENGTH_SHORT
                            ).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                            return@launch
                        }
                        if (renderedBitmap == null) {
                            Toast.makeText(
                                context,
                                colorCardTraceCopy.nothingToSave,
                                Toast.LENGTH_SHORT
                            ).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                            return@launch
                        }
                        val safeRenderedBitmap = renderedBitmap ?: return@launch
                        try {
                            val savedUri = saveColorCardRenderedBitmap(
                                context = context.applicationContext,
                                bitmap = safeRenderedBitmap,
                                baseName = source.displayName,
                                sourceUri = source.uri
                            )
                            saveImageState = if (savedUri != null) {
                                ColorCardTraceSaveImageState.Saved
                            } else {
                                ColorCardTraceSaveImageState.Idle
                            }
                            if (savedUri != null) {
                                val exportRecipe = buildColorCardTraceRecipeFromCurrent(
                                    mode = effectPanelMode,
                                    traceState = traceFilterState
                                )
                                exportSuccessPayload?.release()
                                exportSuccessPayload = buildColorCardTraceExportSuccessPayload(
                                    beforeBitmap = traceFilterState.previewSourceBitmap,
                                    afterBitmap = safeRenderedBitmap,
                                    savedUri = savedUri,
                                    displayName = source.displayName,
                                    recipe = exportRecipe
                                )
                                delay(800)
                            } else {
                                Toast.makeText(
                                    context,
                                    colorCardTraceCopy.saveFailed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "淇濆瓨澶辫触锛岃閲嶈瘯",
                                Toast.LENGTH_SHORT
                            ).show()
                            saveImageState = ColorCardTraceSaveImageState.Idle
                        } finally {
                            safeRenderedBitmap.recycle()
                            if (saveImageState == ColorCardTraceSaveImageState.Saved) {
                                saveImageState = ColorCardTraceSaveImageState.Idle
                            }
                        }
                    }
                },
                onSaveRecipe = {
                    val activeMode = effectPanelMode
                    val recipe = buildColorCardTraceRecipeFromCurrent(
                        mode = activeMode,
                        traceState = traceFilterState
                    )
                    if (recipe == null) {
                        Toast.makeText(context, colorCardTraceCopy.noRecipeToSave, Toast.LENGTH_SHORT).show()
                        return@ColorCardTraceFilterDeckOverlay
                    }
                    scope.launch {
                        val saved = recipeStore.saveRecipe(recipe)
                        Toast.makeText(
                            context,
                            if (saved != null) colorCardPageCopy.saveCurrentRecipe else colorCardPageCopy.saveRecipeFailed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onApplyRecipe = { recipe ->
                    effectPanelMode = colorCardRecipeModeToPanelMode(recipe.mode)
                    pendingRecipeToApply = recipe
                },
                onShareRecipe = { recipe ->
                    shareColorCardText(
                        context = context,
                        chooserTitle = colorCardPageCopy.shareRecipe,
                        subject = recipe.title,
                        text = buildColorCardRecipeShareText(listOf(recipe))
                    )
                },
                onDeleteRecipe = { recipe ->
                    scope.launch {
                        val deleted = recipeStore.deleteRecipe(recipe.id)
                        Toast.makeText(
                            context,
                            if (deleted) colorCardPageCopy.deleteRecipe else colorCardPageCopy.deleteRecipeFailed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onImportRecipes = {
                    showRecipeImportDialog = true
                },
                onExportAllRecipes = {
                    if (savedRecipes.isEmpty()) {
                        Toast.makeText(context, colorCardTraceCopy.noRecipesToExport, Toast.LENGTH_SHORT).show()
                    } else {
                        shareColorCardText(
                            context = context,
                            chooserTitle = colorCardPageCopy.exportAllRecipes,
                            subject = colorCardPageCopy.recipePack,
                            text = buildColorCardRecipeShareText(savedRecipes)
                        )
                    }
                },
                onMotionPhotoEntryClick = {
                    showMotionPhotoLab = true
                },
                imitateReferencePreviewUri = imitateReferenceImage?.uri,
                imitateReferenceDisplayName = imitateReferenceImage?.displayName,
                imitateReferenceColors = imitateAnalysis?.paletteColors.orEmpty(),
                imitateReferenceLoading = imitateReferenceLoading,
                onPickImitateReference = {
                    imitateReferencePicker.launch("image/*")
                },
                onClearImitateReference = clearImitateReference
            )
        },
        onImageReady = {
            if (overlayVisible && !visualReady) {
                visualReady = true
            }
        },
        onDismissRequest = requestDismiss
    )
    exportSuccessPayload?.let { payload ->
        ColorCardTraceExportSuccessDialog(
            payload = payload,
            onBackToEditing = {
                exportSuccessPayload = null
            },
            onChangeImage = {
                changeImagePicker.launch("image/*")
            },
            onExitEditing = {
                exportSuccessPayload = null
                performDismiss()
            },
            onCopyRecipe = {
                val recipeText = payload.copyText ?: return@ColorCardTraceExportSuccessDialog
                copyColorCardText(
                    context = context,
                    label = payload.copyLabel ?: "color_card",
                    text = recipeText,
                    successMessage = colorCardPageCopy.copied
                )
            },
            onShareImage = {
                shareColorCardRenderedImage(
                    context = context,
                    uri = payload.savedUri,
                    displayName = payload.displayName
                )
            }
        )
    }
    if (showMotionPhotoLab) {
        MotionPhotoLabPage(
            sourceBitmap = traceFilterState.previewSourceBitmap,
            sourceFile = source.tempFile,
            displayName = source.displayName,
            onBack = { showMotionPhotoLab = false }
        )
    }
    if (showRecipeImportDialog) {
        AlertDialog(
            onDismissRequest = { showRecipeImportDialog = false },
            title = { Text(colorCardPageCopy.importRecipe) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = colorCardPageCopy.importRecipeBody,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = recipeImportText,
                        onValueChange = { recipeImportText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rawText = recipeImportText
                        scope.launch {
                            val importedCount = runCatching {
                                importSharedColorCardRecipes(
                                    rawText = rawText,
                                    recipeStore = recipeStore,
                                    paletteStore = store
                                )
                            }.getOrElse {
                                Toast.makeText(context, colorCardTraceCopy.importFailed, Toast.LENGTH_SHORT).show()
                                0
                            }
                            if (importedCount > 0) {
                                panelMode = ColorCardTracePanelMode.Recipe
                                Toast.makeText(context, colorCardTraceCopy.importedRecipesCount(importedCount), Toast.LENGTH_SHORT).show()
                                showRecipeImportDialog = false
                                recipeImportText = ""
                            }
                        }
                    }
                ) {
                    Text(colorCardPageCopy.importAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecipeImportDialog = false }) {
                    Text(colorCardPageCopy.cancel)
                }
            }
        )
    }
    if (showDiscardEditDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardEditDialog = false },
            title = { Text(stringResource(id = R.string.color_card_discard_dialog_title)) },
            text = { Text(stringResource(id = R.string.color_card_discard_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardEditDialog = false
                        performDismiss()
                    }
                ) {
                    Text(stringResource(id = R.string.color_card_discard_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardEditDialog = false }) {
                    Text(stringResource(id = R.string.color_card_discard_dialog_dismiss))
                }
            }
        )
    }
}

private fun resolveImportedColorCardPickerBounds(
    density: androidx.compose.ui.unit.Density,
    windowSizePx: IntSize,
    imageWidth: Int,
    imageHeight: Int
): Rect = with(density) {
    val horizontalInset = 24.dp.toPx()
    val topInset = 116.dp.toPx()
    val bottomInset = 260.dp.toPx()
    val safeWindowWidth = windowSizePx.width.coerceAtLeast(1).toFloat()
    val safeWindowHeight = windowSizePx.height.coerceAtLeast(1).toFloat()
    val availableWidth = (safeWindowWidth - (horizontalInset * 2f)).coerceAtLeast(1f)
    val availableHeight = (safeWindowHeight - topInset - bottomInset).coerceAtLeast(1f)
    val safeAspectRatio = when {
        imageWidth > 0 && imageHeight > 0 -> imageWidth.toFloat() / imageHeight.toFloat()
        else -> 1f
    }.coerceAtLeast(0.01f)
    val widthByHeight = availableHeight * safeAspectRatio
    val overlayWidth = min(availableWidth, widthByHeight)
    val overlayHeight = (overlayWidth / safeAspectRatio).coerceAtMost(availableHeight)
    val left = ((safeWindowWidth - overlayWidth) / 2f).coerceAtLeast(horizontalInset)
    val top = topInset + ((availableHeight - overlayHeight) / 2f).coerceAtLeast(0f)
    Rect(
        left = left,
        top = top,
        right = left + overlayWidth,
        bottom = top + overlayHeight
    )
}

private suspend fun importColorCardPickedImage(
    context: Context,
    sourceUri: Uri
): ColorCardImportedImageSource = importColorCardEditingImage(context, sourceUri)

private fun cleanupColorCardImportedImageSource(source: ColorCardImportedImageSource?) {
    cleanupColorCardEditingImageSource(source)
}

private fun ColorCardImportedImageSource.toMediaItem(): MediaItem {
    return MediaItem(
        id = -sessionId,
        uri = uri,
        type = "photo",
        dateAdded = System.currentTimeMillis() / 1000L,
        size = tempFile.length(),
        width = width,
        height = height,
        mimeType = mimeType,
        displayName = displayName,
        resolution = if (width > 0 && height > 0) "${width}×$height" else "",
        path = tempFile.absolutePath
    )
}

private fun resolveColorCardDisplayProfile(
    activity: Activity?,
    wideGamutActive: Boolean
): ColorCardDisplayProfile {
    val referenceColorSpace = "sRGB IEC 61966-2.1"
    if (activity == null) {
        return ColorCardDisplayProfile(
            referenceColorSpace = referenceColorSpace,
            deviceWideGamut = "未知",
            windowColorMode = "Default",
            renderingNote = "当前上下文无法读取设备色域信息，色卡仍按 sRGB 基准绘制。"
        )
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return ColorCardDisplayProfile(
            referenceColorSpace = referenceColorSpace,
            deviceWideGamut = "Android 8 以下不支持广色域色彩管理",
            windowColorMode = "Default（sRGB 回退）",
            renderingNote = "当前设备走 sRGB 兼容路径，不启用广色域窗口。"
        )
    }

    val displayWideGamut = activity.display?.isWideColorGamut == true
    val systemWideGamut = activity.resources.configuration.isScreenWideColorGamut
    val deviceWideGamut = when {
        systemWideGamut -> "支持（系统色彩管理可用）"
        displayWideGamut -> "面板支持，但系统未授予广色域色管"
        else -> "不支持"
    }
    val windowColorMode = if (wideGamutActive) {
        "Wide Gamut（仅色卡详情页）"
    } else {
        "Default（sRGB 兼容路径）"
    }
    val renderingNote = when {
        wideGamutActive -> {
            "本页已开启广色域窗口；色值仍以 sRGB 数值为基准，最终观感仍受屏幕校准影响。"
        }
        systemWideGamut -> {
            "设备支持广色域，但当前未激活该窗口模式时会回退到 sRGB。"
        }
        displayWideGamut -> {
            "设备面板具备更大色域，但当前系统未提供可管理的广色域窗口。"
        }
        else -> {
            "当前设备按标准 sRGB 路径显示，这是最稳的跨设备一致性方案。"
        }
    }
    return ColorCardDisplayProfile(
        referenceColorSpace = referenceColorSpace,
        deviceWideGamut = deviceWideGamut,
        windowColorMode = windowColorMode,
        renderingNote = renderingNote
    )
}

private fun supportsColorCardWideGamut(activity: Activity?): Boolean {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return false
    }
    return activity.resources.configuration.isScreenWideColorGamut
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
