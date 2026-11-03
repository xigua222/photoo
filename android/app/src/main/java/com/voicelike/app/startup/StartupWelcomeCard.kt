package com.voicelike.app.startup

import com.voicelike.app.format
import com.voicelike.app.organizeSortingStrings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.voicelike.app.liquidglass.rememberLiquidGlassBackdrop
import com.voicelike.app.AppLanguage
import com.voicelike.app.AppStats
import com.voicelike.app.ColorCardPage
import com.voicelike.app.LocalAppLanguage
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.LocalHapticsEnabled
import com.voicelike.app.MainDeleteQueueButton
import com.voicelike.app.MainStatsEntryButton
import com.voicelike.app.MainTopActionRail
import com.voicelike.app.MediaItem
import com.voicelike.app.R
import com.voicelike.app.getColorCardUiStrings
import com.voicelike.app.getNewPageStrings
import kotlinx.coroutines.delay
import kotlin.math.pow

private data class StartupQuickAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

private data class StartupMineAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

data class StartupBottomBarScrollSnapshot(
    val source: String,
    val absoluteOffsetPx: Int,
    val nearTop: Boolean,
    val forceHidden: Boolean = false
)

internal fun startupBottomBarAbsoluteOffset(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Int = (firstVisibleItemIndex * 100_000) + firstVisibleItemScrollOffset

// What the organize home shows about the current organize range.
data class StartupOrganizeScope(
    val label: String,
    val active: Boolean,
    // Photos left in the range, null while unknown.
    val remainingCount: Int?,
    val onOpen: () -> Unit,
    val onClear: () -> Unit
)

enum class StartupWelcomeTab {
    Organize,
    ColorCard,
    Watermark,
    Insights,
    Mine
}

@Composable
fun StartupWelcomeCard(
    copy: StartupWelcomeCopy,
    loading: Boolean,
    pendingDeleteCount: Int = 0,
    onStartRequested: () -> Unit,
    startTransitionActive: Boolean = false,
    startTransitionProgress: Float = 0f,
    onStartTransitionRequested: (() -> Unit)? = null,
    onStatsRequested: (() -> Unit)? = null,
    onDeleteQueueRequested: (() -> Unit)? = null,
    onLikesRequested: (() -> Unit)? = null,
    onColorCardRequested: (() -> Unit)? = null,
    onColorCardFilterRequested: ((String) -> Unit)? = null,
    onColorCardCameraRequested: (() -> Unit)? = null,
    onStampRequested: (() -> Unit)? = null,
    onElectronicAlbumRequested: (() -> Unit)? = null,
    onPromptManagerRequested: (() -> Unit)? = null,
    onFillLightRequested: (() -> Unit)? = null,
    onWatermarkRequested: (() -> Unit)? = null,
    onWatermarkPresetRequested: ((String) -> Unit)? = null,
    selectedModernTab: StartupWelcomeTab = StartupWelcomeTab.Organize,
    onModernTabSelected: (StartupWelcomeTab) -> Unit = {},
    modernNavigationEnabled: Boolean = false,
    modernMineStats: AppStats = AppStats(),
    modernMineTotalPhotos: Int = 0,
    modernMineTotalSizeBytes: Long = 0L,
    modernMineAvatarType: String = "girl",
    modernMineCustomAvatarPath: String? = null,
    modernMineLikedPhotos: List<MediaItem> = emptyList(),
    modernMineAllMedia: List<MediaItem> = emptyList(),
    modernMineLikedIds: Set<Long> = emptySet(),
    onShootingStatsRequested: (() -> Unit)? = null,
    onDeepOrganizeRequested: (() -> Unit)? = null,
    onKeepGalleryRequested: (() -> Unit)? = null,
    onRecycleBinRequested: (() -> Unit)? = null,
    onMineGlobeRequested: (() -> Unit)? = null,
    onLabRequested: (() -> Unit)? = null,
    onCampfireRequested: (() -> Unit)? = null,
    onHelpCenterRequested: (() -> Unit)? = null,
    onSettingsRequested: (() -> Unit)? = null,
    onMineAvatarTypeChange: ((String) -> Unit)? = null,
    onMineCustomAvatarPathChange: ((String?) -> Unit)? = null,
    organizeScope: StartupOrganizeScope? = null,
    organizeHome: StartupOrganizeHome? = null,
    insightsContent: (@Composable () -> Unit)? = null
) {
    if (modernNavigationEnabled) {
        ModernStartupWelcomeCard(
            copy = copy,
            loading = loading,
            onStartRequested = onStartRequested,
            startTransitionActive = startTransitionActive,
            startTransitionProgress = startTransitionProgress,
            onStartTransitionRequested = onStartTransitionRequested,
            onStatsRequested = onStatsRequested,
            onLikesRequested = onLikesRequested,
            onColorCardRequested = onColorCardRequested,
            onColorCardFilterRequested = onColorCardFilterRequested,
            onColorCardCameraRequested = onColorCardCameraRequested,
            onStampRequested = onStampRequested,
            onElectronicAlbumRequested = onElectronicAlbumRequested,
            onPromptManagerRequested = onPromptManagerRequested,
            onFillLightRequested = onFillLightRequested,
            onWatermarkRequested = onWatermarkRequested,
            onWatermarkPresetRequested = onWatermarkPresetRequested,
            selectedTab = selectedModernTab,
            onTabSelected = onModernTabSelected,
            mineStats = modernMineStats,
            mineTotalPhotos = modernMineTotalPhotos,
            mineTotalSizeBytes = modernMineTotalSizeBytes,
            mineAvatarType = modernMineAvatarType,
            mineCustomAvatarPath = modernMineCustomAvatarPath,
            mineLikedPhotos = modernMineLikedPhotos,
            mineAllMedia = modernMineAllMedia,
            mineLikedIds = modernMineLikedIds,
            onShootingStatsRequested = onShootingStatsRequested,
            onDeepOrganizeRequested = onDeepOrganizeRequested,
            onKeepGalleryRequested = onKeepGalleryRequested,
            onRecycleBinRequested = onRecycleBinRequested,
            onMineGlobeRequested = onMineGlobeRequested,
            onLabRequested = onLabRequested,
            onCampfireRequested = onCampfireRequested,
            onHelpCenterRequested = onHelpCenterRequested,
            onSettingsRequested = onSettingsRequested,
            onMineAvatarTypeChange = onMineAvatarTypeChange,
            onMineCustomAvatarPathChange = onMineCustomAvatarPathChange,
            organizeScope = organizeScope,
            organizeHome = organizeHome,
            insightsContent = insightsContent
        )
        return
    }

    var dismissing by remember { mutableStateOf(false) }
    val localProgress by animateFloatAsState(
        targetValue = if (dismissing) 1f else 0f,
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "startup_welcome_dismiss"
    )
    val progress = if (onStartTransitionRequested != null) {
        startTransitionProgress
    } else {
        localProgress
    }
    val dismissingState = if (onStartTransitionRequested != null) {
        startTransitionActive
    } else {
        dismissing
    }
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val arcPeak = 1f - ((progress * 2f) - 1f).pow(2)
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val quickActions = remember(strings, language, onLikesRequested, onColorCardRequested) {
        buildList {
            if (onLikesRequested != null) {
                add(
                    StartupQuickAction(
                        label = strings.likes,
                        icon = Icons.Outlined.FavoriteBorder,
                        onClick = onLikesRequested
                    )
                )
            }
            if (onColorCardRequested != null) {
                add(
                    StartupQuickAction(
                        label = localizedStartupCreativeTitle(language),
                        icon = Icons.Outlined.Palette,
                        onClick = onColorCardRequested
                    )
                )
            }
        }
    }

    LaunchedEffect(dismissing) {
        if (onStartTransitionRequested == null && dismissing) {
            delay(460L)
            onStartRequested()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        MainTopActionRail(
            modifier = Modifier.align(Alignment.TopCenter),
            startContent = {
                if (onStatsRequested != null) {
                    MainStatsEntryButton(
                        onClick = onStatsRequested,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp)
                    )
                }
            },
            endContent = {
                if (pendingDeleteCount > 0 && onDeleteQueueRequested != null) {
                    MainDeleteQueueButton(
                        pendingCount = pendingDeleteCount,
                        onClick = onDeleteQueueRequested,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                    )
                }
            }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 420.dp)
                .graphicsLayer {
                    translationX = screenWidthPx * 0.95f * progress
                    translationY = -screenHeightPx * (0.66f * progress) - screenHeightPx * 0.09f * arcPeak
                    rotationZ = 22f * progress
                    scaleX = 1f - (0.14f * progress)
                    scaleY = 1f - (0.14f * progress)
                    alpha = 1f - (0.88f * progress)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 288.dp, max = 388.dp)
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logokt),
                    contentDescription = "Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = copy.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                if (quickActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        quickActions.forEach { action ->
                            StartupQuickActionButton(
                                label = action.label,
                                icon = action.icon,
                                onClick = action.onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onStartTransitionRequested?.invoke() ?: run {
                            dismissing = true
                        }
                    },
                    enabled = !loading && !dismissingState,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (loading) copy.buttonLoadingLabel else copy.buttonLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernStartupWelcomeCard(
    copy: StartupWelcomeCopy,
    loading: Boolean,
    onStartRequested: () -> Unit,
    startTransitionActive: Boolean,
    startTransitionProgress: Float,
    onStartTransitionRequested: (() -> Unit)?,
    onStatsRequested: (() -> Unit)?,
    onLikesRequested: (() -> Unit)?,
    onColorCardRequested: (() -> Unit)?,
    onColorCardFilterRequested: ((String) -> Unit)?,
    onColorCardCameraRequested: (() -> Unit)?,
    onStampRequested: (() -> Unit)?,
    onElectronicAlbumRequested: (() -> Unit)?,
    onPromptManagerRequested: (() -> Unit)?,
    onFillLightRequested: (() -> Unit)?,
    onWatermarkRequested: (() -> Unit)?,
    onWatermarkPresetRequested: ((String) -> Unit)?,
    selectedTab: StartupWelcomeTab,
    onTabSelected: (StartupWelcomeTab) -> Unit,
    mineStats: AppStats,
    mineTotalPhotos: Int,
    mineTotalSizeBytes: Long,
    mineAvatarType: String,
    mineCustomAvatarPath: String?,
    mineLikedPhotos: List<MediaItem>,
    mineAllMedia: List<MediaItem>,
    mineLikedIds: Set<Long>,
    onShootingStatsRequested: (() -> Unit)?,
    onDeepOrganizeRequested: (() -> Unit)?,
    onKeepGalleryRequested: (() -> Unit)?,
    onRecycleBinRequested: (() -> Unit)?,
    onMineGlobeRequested: (() -> Unit)?,
    onLabRequested: (() -> Unit)?,
    onCampfireRequested: (() -> Unit)?,
    onHelpCenterRequested: (() -> Unit)?,
    onSettingsRequested: (() -> Unit)?,
    onMineAvatarTypeChange: ((String) -> Unit)?,
    onMineCustomAvatarPathChange: ((String?) -> Unit)?,
    organizeScope: StartupOrganizeScope?,
    organizeHome: StartupOrganizeHome?,
    insightsContent: (@Composable () -> Unit)?
) {
    var dismissing by remember { mutableStateOf(false) }
    var bottomNavigationHidden by remember { mutableStateOf(false) }
    var colorCardEntryResetSignal by remember { mutableIntStateOf(1) }
    var mineScrollToTopSignal by remember { mutableIntStateOf(0) }
    val tabStateHolder = rememberSaveableStateHolder()
    var bottomBarLastSource by remember { mutableStateOf<String?>(null) }
    var bottomBarLastOffsetPx by remember { mutableStateOf<Int?>(null) }
    var bottomBarHideTravelPx by remember { mutableFloatStateOf(0f) }
    var bottomBarShowTravelPx by remember { mutableFloatStateOf(0f) }
    val localProgress by animateFloatAsState(
        targetValue = if (dismissing) 1f else 0f,
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "startup_welcome_dismiss"
    )
    val progress = if (onStartTransitionRequested != null) {
        startTransitionProgress
    } else {
        localProgress
    }
    val dismissingState = if (onStartTransitionRequested != null) {
        startTransitionActive
    } else {
        dismissing
    }
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val bottomBarHideThresholdPx = with(density) { 32.dp.toPx() }
    val bottomBarShowThresholdPx = with(density) { 14.dp.toPx() }
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val newPageStrings = remember(language) { getNewPageStrings(language) }
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val performHaptic = remember(haptic, hapticsAllowed) {
        {
            if (hapticsAllowed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    LaunchedEffect(dismissing) {
        if (onStartTransitionRequested == null && dismissing) {
            delay(460L)
            onStartRequested()
        }
    }
    LaunchedEffect(selectedTab) {
        bottomNavigationHidden = false
        bottomBarLastSource = null
        bottomBarLastOffsetPx = null
        bottomBarHideTravelPx = 0f
        bottomBarShowTravelPx = 0f
    }
    // The liquid glass tab bar refracts the tab pages, so the pages are recorded into its backdrop.
    val pagesBackdrop = rememberLiquidGlassBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (pagesBackdrop != null) {
                        Modifier.layerBackdrop(pagesBackdrop)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val initialIndex = startupTabTransitionIndex(initialState)
                    val targetIndex = startupTabTransitionIndex(targetState)
                    val direction = if (targetIndex >= initialIndex) 1 else -1
                    (
                        slideInHorizontally(
                            initialOffsetX = { fullWidth ->
                                (fullWidth * 0.22f * direction).toInt()
                            },
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing)
                        )
                        ).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth ->
                                (-fullWidth * 0.16f * direction).toInt()
                            },
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        )
                    ).using(SizeTransform(clip = false))
                },
                label = "startup_tab_content"
            ) { activeTab ->
                when (activeTab) {
                    StartupWelcomeTab.Organize -> StartupOrganizeTab(
                        copy = copy,
                        loading = loading,
                        dismissing = dismissingState,
                        progress = progress,
                        screenWidthPx = screenWidthPx,
                        screenHeightPx = screenHeightPx,
                        onDeepOrganizeClick = onDeepOrganizeRequested?.let { onDeepOrganize ->
                            {
                                performHaptic()
                                onDeepOrganize()
                            }
                        },
                        onRecycleBinClick = onRecycleBinRequested?.let { onRecycleBin ->
                            {
                                performHaptic()
                                onRecycleBin()
                            }
                        },
                        onStartClick = {
                            performHaptic()
                            onStartTransitionRequested?.invoke() ?: run {
                                dismissing = true
                            }
                        },
                        organizeScope = organizeScope?.let { scope ->
                            scope.copy(
                                onOpen = {
                                    performHaptic()
                                    scope.onOpen()
                                },
                                onClear = {
                                    performHaptic()
                                    scope.onClear()
                                }
                            )
                        },
                        organizeHome = organizeHome
                    )
                    StartupWelcomeTab.ColorCard,
                    StartupWelcomeTab.Watermark -> tabStateHolder.SaveableStateProvider("startup-color-card-tab") {
                        StartupColorCardTab(
                            onBack = { onTabSelected(StartupWelcomeTab.Organize) },
                            onOpenCamera = onColorCardCameraRequested ?: {},
                            onOpenStampCamera = onStampRequested ?: {},
                            onOpenElectronicAlbum = onElectronicAlbumRequested ?: {},
                            onOpenPromptManager = onPromptManagerRequested ?: {},
                            onOpenFillLight = onFillLightRequested ?: {},
                            onOpenWatermarkPreset = { presetKey ->
                                if (onWatermarkPresetRequested != null) {
                                    onWatermarkPresetRequested(presetKey)
                                } else {
                                    onWatermarkRequested?.invoke()
                                }
                            },
                            entryResetSignal = colorCardEntryResetSignal,
                            onBottomBarScrollSnapshotChanged = { snapshot ->
                                if (snapshot.forceHidden) {
                                    bottomNavigationHidden = true
                                    bottomBarLastSource = snapshot.source
                                    bottomBarLastOffsetPx = snapshot.absoluteOffsetPx
                                    bottomBarHideTravelPx = 0f
                                    bottomBarShowTravelPx = 0f
                                } else if (snapshot.nearTop) {
                                    bottomNavigationHidden = false
                                    bottomBarLastSource = snapshot.source
                                    bottomBarLastOffsetPx = snapshot.absoluteOffsetPx
                                    bottomBarHideTravelPx = 0f
                                    bottomBarShowTravelPx = 0f
                                } else {
                                    val previousSource = bottomBarLastSource
                                    val previousOffsetPx = bottomBarLastOffsetPx
                                    bottomBarLastSource = snapshot.source
                                    bottomBarLastOffsetPx = snapshot.absoluteOffsetPx
                                    if (previousSource != snapshot.source || previousOffsetPx == null) {
                                        bottomBarHideTravelPx = 0f
                                        bottomBarShowTravelPx = 0f
                                    } else {
                                        val deltaPx = snapshot.absoluteOffsetPx - previousOffsetPx
                                        when {
                                            deltaPx > 0 -> {
                                                bottomBarHideTravelPx += deltaPx
                                                bottomBarShowTravelPx = 0f
                                                if (bottomBarHideTravelPx >= bottomBarHideThresholdPx) {
                                                    bottomNavigationHidden = true
                                                }
                                            }

                                            deltaPx < 0 -> {
                                                bottomBarShowTravelPx += -deltaPx
                                                bottomBarHideTravelPx = 0f
                                                if (bottomBarShowTravelPx >= bottomBarShowThresholdPx) {
                                                    bottomNavigationHidden = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    StartupWelcomeTab.Insights -> tabStateHolder.SaveableStateProvider("startup-insights-tab") {
                        insightsContent?.invoke()
                    }

                    StartupWelcomeTab.Mine -> tabStateHolder.SaveableStateProvider("startup-mine-tab") {
                        StartupMineTab(
                            title = localizedStartupMineTitle(language),
                            stats = mineStats,
                            totalPhotos = mineTotalPhotos,
                            avatarType = mineAvatarType,
                            customAvatarPath = mineCustomAvatarPath,
                            allMedia = mineAllMedia,
                            likedIds = mineLikedIds,
                            likedPhotos = mineLikedPhotos,
                            onAlbumInsightsClick = onShootingStatsRequested,
                            onMineGlobeRequested = onMineGlobeRequested,
                            onProcessedClick = onKeepGalleryRequested,
                            onCampfireClick = onCampfireRequested,
                            onHelpCenterClick = onHelpCenterRequested,
                            onLikesClick = onLikesRequested,
                            onSettingsClick = onSettingsRequested,
                            onAvatarTypeChange = onMineAvatarTypeChange,
                            onCustomAvatarPathChange = onMineCustomAvatarPathChange,
                            scrollToTopSignal = mineScrollToTopSignal,
                            onBottomBarScrollSnapshotChanged = { snapshot ->
                                if (snapshot.forceHidden) {
                                    bottomNavigationHidden = true
                                    bottomBarLastSource = snapshot.source
                                    bottomBarLastOffsetPx = snapshot.absoluteOffsetPx
                                    bottomBarHideTravelPx = 0f
                                    bottomBarShowTravelPx = 0f
                                } else if (snapshot.nearTop) {
                                    bottomNavigationHidden = false
                                    bottomBarLastSource = snapshot.source
                                    bottomBarLastOffsetPx = snapshot.absoluteOffsetPx
                                    bottomBarHideTravelPx = 0f
                                    bottomBarShowTravelPx = 0f
                                } else {
                                    val previousSource = bottomBarLastSource
                                    val previousOffsetPx = bottomBarLastOffsetPx
                                    bottomBarLastSource = snapshot.source
                                    bottomBarLastOffsetPx = snapshot.absoluteOffsetPx
                                    if (previousSource != snapshot.source || previousOffsetPx == null) {
                                        bottomBarHideTravelPx = 0f
                                        bottomBarShowTravelPx = 0f
                                    } else {
                                        val deltaPx = snapshot.absoluteOffsetPx - previousOffsetPx
                                        when {
                                            deltaPx > 0 -> {
                                                bottomBarHideTravelPx += deltaPx
                                                bottomBarShowTravelPx = 0f
                                                if (bottomBarHideTravelPx >= bottomBarHideThresholdPx) {
                                                    bottomNavigationHidden = true
                                                }
                                            }

                                            deltaPx < 0 -> {
                                                bottomBarShowTravelPx += -deltaPx
                                                bottomBarHideTravelPx = 0f
                                                if (bottomBarShowTravelPx >= bottomBarShowThresholdPx) {
                                                    bottomNavigationHidden = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        val onBottomTabSelected: (StartupWelcomeTab) -> Unit = { tab ->
            performHaptic()
            onTabSelected(tab)
        }
        val onBottomTabReselected: (StartupWelcomeTab) -> Unit = { tab ->
            performHaptic()
            bottomNavigationHidden = false
            bottomBarLastSource = null
            bottomBarLastOffsetPx = null
            bottomBarHideTravelPx = 0f
            bottomBarShowTravelPx = 0f
            when (tab) {
                StartupWelcomeTab.ColorCard -> colorCardEntryResetSignal += 1
                StartupWelcomeTab.Mine -> mineScrollToTopSignal += 1
                StartupWelcomeTab.Organize,
                StartupWelcomeTab.Insights,
                StartupWelcomeTab.Watermark -> Unit
            }
        }
        if (pagesBackdrop != null) {
            StartupLiquidGlassTabBar(
                backdrop = pagesBackdrop,
                selectedTab = selectedTab,
                onTabSelected = onBottomTabSelected,
                onTabReselected = onBottomTabReselected,
                hidden = bottomNavigationHidden || dismissingState,
                appLanguage = LocalAppLanguage.current,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            StartupBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = onBottomTabSelected,
                onTabReselected = onBottomTabReselected,
                hidden = bottomNavigationHidden || dismissingState,
                appLanguage = LocalAppLanguage.current,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun StartupOrganizeTab(
    copy: StartupWelcomeCopy,
    loading: Boolean,
    dismissing: Boolean,
    progress: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onDeepOrganizeClick: (() -> Unit)?,
    onRecycleBinClick: (() -> Unit)?,
    onStartClick: () -> Unit,
    organizeScope: StartupOrganizeScope? = null,
    organizeHome: StartupOrganizeHome? = null
) {
    if (organizeHome != null) {
        StartupOrganizeHomeContent(
            copy = copy,
            loading = loading,
            progress = progress,
            home = organizeHome,
            scope = organizeScope,
            onStartClick = { if (!dismissing) onStartClick() }
        )
        return
    }
    val arcPeak = 1f - ((progress * 2f) - 1f).pow(2)
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val title = localizedStartupOrganizeTitle(language)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp)
                    .graphicsLayer {
                        alpha = 1f - progress
                        translationY = -22.dp.toPx() * progress
                    }
                    .blur(14.dp * progress)
            )
            Spacer(modifier = Modifier.size(42.dp))
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 296.dp, max = 392.dp)
                .graphicsLayer {
                    translationX = screenWidthPx * 0.95f * progress
                    translationY = -screenHeightPx * (0.66f * progress) - screenHeightPx * 0.09f * arcPeak
                    rotationZ = 22f * progress
                    scaleX = 1f - (0.14f * progress)
                    scaleY = 1f - (0.14f * progress)
                    alpha = 1f - (0.88f * progress)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 268.dp, max = 360.dp)
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logokt),
                    contentDescription = "Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = copy.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StartupOrganizeShortcutButton(
                        label = strings.deepOrganizeTitle,
                        icon = StartupActionIcons.DeepOrganize,
                        onClick = onDeepOrganizeClick,
                        modifier = Modifier.weight(1f)
                    )
                    StartupOrganizeShortcutButton(
                        label = strings.recycleBinTitle,
                        icon = Icons.Outlined.Delete,
                        onClick = onRecycleBinClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onStartClick,
                    enabled = !loading && !dismissing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    val remainingInRange = organizeScope?.remainingCount
                    Text(
                        text = when {
                            loading -> copy.buttonLoadingLabel
                            remainingInRange != null && remainingInRange > 0 -> {
                                val sortingStrings = organizeSortingStrings(language)
                                sortingStrings.format(sortingStrings.organizeCountFmt, remainingInRange)
                            }
                            else -> startupOrganizeStartLabel(language)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (organizeScope != null) {
            StartupOrganizeScopeChip(
                scope = organizeScope,
                modifier = Modifier
                    .padding(top = 22.dp, start = 20.dp, end = 20.dp)
                    .graphicsLayer { alpha = 1f - progress }
            )
        }
        }
    }
}

@Composable
internal fun StartupOrganizeScopeChip(
    scope: StartupOrganizeScope,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val chipShape = RoundedCornerShape(18.dp)
    val clearLabel = organizeSortingStrings(LocalAppLanguage.current).clearRange
    Row(
        modifier = modifier
            .height(36.dp)
            .widthIn(max = 320.dp)
            .clip(chipShape)
            .then(
                if (scope.active) {
                    Modifier
                        .background(primary.copy(alpha = 0.14f), chipShape)
                        .border(1.dp, primary.copy(alpha = 0.45f), chipShape)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface, chipShape)
                        .border(0.5.dp, onSurface.copy(alpha = 0.12f), chipShape)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = scope.onOpen)
                .padding(start = 14.dp, end = if (scope.active) 4.dp else 12.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!scope.active) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = null,
                    tint = onSurface,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = scope.label,
                fontSize = 14.sp,
                fontWeight = if (scope.active) FontWeight.Bold else FontWeight.Medium,
                color = if (scope.active) primary else onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = if (scope.active) primary else onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
        if (scope.active) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 36.dp)
                    .clickable(onClick = scope.onClear),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(primary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = clearLabel,
                        tint = primary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StartupOrganizeShortcutButton(
    label: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val itemShape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(itemShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (onClick != null) 0.78f else 0.34f),
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (onClick != null) 0.86f else 0.36f),
            maxLines = 1
        )
    }
}

@Composable
private fun StartupFeatureTab(
    title: String,
    subtitle: String,
    buttonLabel: String,
    icon: ImageVector,
    onOpen: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(26.dp))
            Button(
                onClick = { onOpen?.invoke() },
                enabled = onOpen != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111827),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = buttonLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StartupColorCardTab(
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenStampCamera: () -> Unit,
    onOpenElectronicAlbum: () -> Unit,
    onOpenPromptManager: () -> Unit,
    onOpenFillLight: () -> Unit,
    onOpenWatermarkPreset: (String) -> Unit,
    entryResetSignal: Int,
    onBottomBarScrollSnapshotChanged: (StartupBottomBarScrollSnapshot) -> Unit
) {
    ColorCardPage(
        onBack = onBack,
        onOpenCamera = onOpenCamera,
        onOpenStampCamera = onOpenStampCamera,
        onOpenElectronicAlbum = onOpenElectronicAlbum,
        onOpenPromptManager = onOpenPromptManager,
        onOpenFillLight = onOpenFillLight,
        onOpenWatermarkPreset = onOpenWatermarkPreset,
        entryResetSignal = entryResetSignal,
        onBottomBarScrollSnapshotChanged = onBottomBarScrollSnapshotChanged,
        bottomContentPadding = 124.dp,
        modernEmbeddedStyle = true,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun StartupMineTab(
    title: String,
    stats: AppStats,
    totalPhotos: Int,
    avatarType: String,
    customAvatarPath: String?,
    allMedia: List<MediaItem>,
    likedIds: Set<Long>,
    likedPhotos: List<MediaItem>,
    onAlbumInsightsClick: (() -> Unit)?,
    onMineGlobeRequested: (() -> Unit)?,
    onProcessedClick: (() -> Unit)?,
    onCampfireClick: (() -> Unit)?,
    onHelpCenterClick: (() -> Unit)?,
    onLikesClick: (() -> Unit)?,
    onSettingsClick: (() -> Unit)?,
    onAvatarTypeChange: ((String) -> Unit)?,
    onCustomAvatarPathChange: ((String?) -> Unit)?,
    scrollToTopSignal: Int,
    onBottomBarScrollSnapshotChanged: (StartupBottomBarScrollSnapshot) -> Unit
) {
    StartupMineProfilePage(
        title = title,
        stats = stats,
        totalPhotos = totalPhotos,
        avatarType = avatarType,
        customAvatarPath = customAvatarPath,
        allMedia = allMedia,
        likedIds = likedIds,
        likedPhotos = likedPhotos,
        onAlbumInsightsClick = onAlbumInsightsClick,
        onGlobePageClick = onMineGlobeRequested,
        onProcessedClick = onProcessedClick,
        onCampfireClick = onCampfireClick,
        onHelpCenterClick = onHelpCenterClick,
        onLikesClick = onLikesClick,
        onSettingsClick = onSettingsClick,
        onAvatarTypeChange = onAvatarTypeChange,
        onCustomAvatarPathChange = onCustomAvatarPathChange,
        scrollToTopSignal = scrollToTopSignal,
        onBottomBarScrollSnapshotChanged = onBottomBarScrollSnapshotChanged
    )
}

@Composable
private fun StartupMineActionRow(action: StartupMineAction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = action.onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = action.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StartupBottomNavigationBar(
    selectedTab: StartupWelcomeTab,
    onTabSelected: (StartupWelcomeTab) -> Unit,
    onTabReselected: (StartupWelcomeTab) -> Unit = {},
    hidden: Boolean,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val tabs = remember {
        listOf(
            StartupWelcomeTab.Organize,
            StartupWelcomeTab.ColorCard,
            StartupWelcomeTab.Insights,
            StartupWelcomeTab.Mine
        )
    }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val hiddenOffset by animateDpAsState(
        targetValue = if (hidden) 118.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "startup_bottom_nav_hidden_offset"
    )
    val navAlpha by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "startup_bottom_nav_alpha"
    )
    val container = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val selectedBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    val navShape = RoundedCornerShape(35.dp)
    val selectedShape = RoundedCornerShape(26.dp)

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .height(70.dp)
            .graphicsLayer {
                translationY = with(density) { hiddenOffset.toPx() }
                alpha = navAlpha
            }
            .clip(navShape)
            .background(container)
            .border(1.dp, borderColor, navShape)
            .padding(8.dp)
    ) {
        val itemWidth = maxWidth / tabs.size
        val selectedOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "startup_bottom_nav_selected_offset"
        )

        Box(
            modifier = Modifier
                .offset(x = selectedOffset)
                .width(itemWidth)
                .fillMaxSize()
                .clip(selectedShape)
                .background(selectedBackground)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = tab == selectedTab
                val itemColor by animateColorAsState(
                    targetValue = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.44f)
                },
                    animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing),
                    label = "startup_bottom_nav_item_color"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                        .clickable(
                            interactionSource = remember(tab) { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (selected) {
                                onTabReselected(tab)
                            } else {
                                onTabSelected(tab)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = startupTabIcon(tab),
                        contentDescription = localizedStartupTabContentDescription(appLanguage, tab),
                        tint = itemColor,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }
    }
}

private fun startupTabTransitionIndex(tab: StartupWelcomeTab): Int = when (tab) {
    StartupWelcomeTab.Organize -> 0
    StartupWelcomeTab.ColorCard,
    StartupWelcomeTab.Watermark -> 1
    StartupWelcomeTab.Insights -> 2
    StartupWelcomeTab.Mine -> 3
}

internal fun startupTabIcon(tab: StartupWelcomeTab): ImageVector = when (tab) {
    StartupWelcomeTab.Organize -> StartupBottomNavIcons.Organize
    StartupWelcomeTab.ColorCard -> StartupBottomNavIcons.ColorCard
    StartupWelcomeTab.Watermark -> StartupBottomNavIcons.ColorCard
    StartupWelcomeTab.Insights -> StartupBottomNavIcons.Insights
    StartupWelcomeTab.Mine -> StartupBottomNavIcons.Mine
}

private fun startupTabContentDescription(language: AppLanguage, tab: StartupWelcomeTab): String = when (tab) {
    StartupWelcomeTab.Organize -> when (language) {
        AppLanguage.SimplifiedChinese -> "整理"
        AppLanguage.TraditionalChinese -> "整理"
        AppLanguage.Japanese -> "整理"
        AppLanguage.Korean -> "정리"
        AppLanguage.French -> "Organiser"
        AppLanguage.German -> "Organisieren"
        AppLanguage.Italian -> "Organizza"
        AppLanguage.Spanish -> "Organizar"
        AppLanguage.Portuguese -> "Organizar"
        AppLanguage.Russian -> "Организовать"
        AppLanguage.Thai -> "จัดระเบียบ"
        AppLanguage.Vietnamese -> "Sắp xếp"
        AppLanguage.Indonesian -> "Atur"
        AppLanguage.Arabic -> "منظم"
        AppLanguage.Ukrainian -> "Розбір"
        AppLanguage.Polish -> "Porządki"
        AppLanguage.Czech -> "Třídění"
        AppLanguage.Dutch -> "Ordenen"
        AppLanguage.Swedish -> "Ordna"
        AppLanguage.Danish -> "Sortér"
        AppLanguage.Norwegian -> "Rydd"
        AppLanguage.Finnish -> "Järjestä"
        AppLanguage.Greek -> "Ταξινόμηση"
        AppLanguage.Romanian -> "Organizare"
        AppLanguage.Hungarian -> "Rendezés"
        AppLanguage.Turkish -> "Düzenle"
        AppLanguage.Malay -> "Susun"
        AppLanguage.Hebrew -> "סידור"
        AppLanguage.English -> "Organize"
    }
    StartupWelcomeTab.ColorCard -> when (language) {
        AppLanguage.SimplifiedChinese -> "色卡"
        AppLanguage.TraditionalChinese -> "色卡"
        AppLanguage.Japanese -> "カラーカード"
        AppLanguage.Korean -> "색상 카드"
        AppLanguage.French -> "Carte de Couleurs"
        AppLanguage.German -> "Farbkarte"
        AppLanguage.Italian -> "Scheda Colori"
        AppLanguage.Spanish -> "Tarjeta de Colores"
        AppLanguage.Portuguese -> "Cartão de Cores"
        AppLanguage.Russian -> "Цветовая Карта"
        AppLanguage.Thai -> "การ์ดสี"
        AppLanguage.Vietnamese -> "Thẻ Màu"
        AppLanguage.Indonesian -> "Kartu Warna"
        AppLanguage.Arabic -> "بطاقة الألوان"
        AppLanguage.Ukrainian -> "Зразки кольорів"
        AppLanguage.Polish -> "Próbki kolorów"
        AppLanguage.Czech -> "Vzorník"
        AppLanguage.Dutch -> "Kleurstalen"
        AppLanguage.Swedish -> "Färgprover"
        AppLanguage.Danish -> "Farveprøver"
        AppLanguage.Norwegian -> "Fargeprøver"
        AppLanguage.Finnish -> "Värimallit"
        AppLanguage.Greek -> "Δείγματα"
        AppLanguage.Romanian -> "Mostre"
        AppLanguage.Hungarian -> "Színminták"
        AppLanguage.Turkish -> "Renk kartı"
        AppLanguage.Malay -> "Kad Warna"
        AppLanguage.Hebrew -> "דוגמיות צבע"
        AppLanguage.English -> "Color Card"
    }
    StartupWelcomeTab.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese -> "水印边框"
        AppLanguage.TraditionalChinese -> "水印邊框"
        AppLanguage.Japanese -> "透かしフレーム"
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
        AppLanguage.Malay -> "Tera Air"
        AppLanguage.Hebrew -> "סימן מים"
        AppLanguage.English -> "Watermark"
    }
    StartupWelcomeTab.Insights -> localizedStartupInsightsTitle(language)
    StartupWelcomeTab.Mine -> when (language) {
        AppLanguage.SimplifiedChinese -> "我的"
        AppLanguage.TraditionalChinese -> "我的"
        AppLanguage.Japanese -> "マイ"
        AppLanguage.Korean -> "내 것"
        AppLanguage.French -> "Mes"
        AppLanguage.German -> "Meine"
        AppLanguage.Italian -> "Miei"
        AppLanguage.Spanish -> "Míos"
        AppLanguage.Portuguese -> "Meus"
        AppLanguage.Russian -> "Мои"
        AppLanguage.Thai -> "ของฉัน"
        AppLanguage.Vietnamese -> "Của tôi"
        AppLanguage.Indonesian -> "Milikku"
        AppLanguage.Arabic -> "خاص بي"
        AppLanguage.Ukrainian -> "Я"
        AppLanguage.Polish -> "Ja"
        AppLanguage.Czech -> "Já"
        AppLanguage.Dutch -> "Ik"
        AppLanguage.Swedish -> "Jag"
        AppLanguage.Danish -> "Mig"
        AppLanguage.Norwegian -> "Meg"
        AppLanguage.Finnish -> "Minä"
        AppLanguage.Greek -> "Εγώ"
        AppLanguage.Romanian -> "Eu"
        AppLanguage.Hungarian -> "Én"
        AppLanguage.Turkish -> "Ben"
        AppLanguage.Malay -> "Saya"
        AppLanguage.Hebrew -> "אני"
        AppLanguage.English -> "Me"
    }
}

private fun startupMineTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese,
    AppLanguage.TraditionalChinese -> "我的"
    else -> "Me"
}

private fun startupOrganizeTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese,
    AppLanguage.TraditionalChinese -> "整理"
    else -> "Organize"
}

private fun localizedStartupCreativeTitle(language: AppLanguage): String = getColorCardUiStrings(language).pageTitle

private fun localizedStartupMineTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "我的"
    AppLanguage.TraditionalChinese -> "我的"
    AppLanguage.Japanese -> "マイ"
    AppLanguage.Korean -> "내 것"
    AppLanguage.French -> "Mes"
    AppLanguage.German -> "Meine"
    AppLanguage.Italian -> "Miei"
    AppLanguage.Spanish -> "Míos"
    AppLanguage.Portuguese -> "Meus"
    AppLanguage.Russian -> "Мои"
    AppLanguage.Thai -> "ของฉัน"
    AppLanguage.Vietnamese -> "Của tôi"
    AppLanguage.Indonesian -> "Milikku"
    AppLanguage.Arabic -> "خاص بي"
    AppLanguage.Ukrainian -> "Я"
    AppLanguage.Polish -> "Ja"
    AppLanguage.Czech -> "Já"
    AppLanguage.Dutch -> "Ik"
    AppLanguage.Swedish -> "Jag"
    AppLanguage.Danish -> "Mig"
    AppLanguage.Norwegian -> "Meg"
    AppLanguage.Finnish -> "Minä"
    AppLanguage.Greek -> "Εγώ"
    AppLanguage.Romanian -> "Eu"
    AppLanguage.Hungarian -> "Én"
    AppLanguage.Turkish -> "Ben"
    AppLanguage.Malay -> "Saya"
    AppLanguage.Hebrew -> "אני"
    AppLanguage.English -> "Me"
}

private fun localizedStartupOrganizeTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "整理"
    AppLanguage.TraditionalChinese -> "整理"
    AppLanguage.Japanese -> "整理"
    AppLanguage.Korean -> "정리"
    AppLanguage.French -> "Organiser"
    AppLanguage.German -> "Organisieren"
    AppLanguage.Italian -> "Organizza"
    AppLanguage.Spanish -> "Organizar"
    AppLanguage.Portuguese -> "Organizar"
    AppLanguage.Russian -> "Организация"
    AppLanguage.Thai -> "จัดระเบียบ"
    AppLanguage.Vietnamese -> "Sắp xếp"
    AppLanguage.Indonesian -> "Atur"
    AppLanguage.Arabic -> "التنظيم"
    AppLanguage.Ukrainian -> "Розбір"
    AppLanguage.Polish -> "Porządki"
    AppLanguage.Czech -> "Třídění"
    AppLanguage.Dutch -> "Ordenen"
    AppLanguage.Swedish -> "Ordna"
    AppLanguage.Danish -> "Sortér"
    AppLanguage.Norwegian -> "Rydd"
    AppLanguage.Finnish -> "Järjestä"
    AppLanguage.Greek -> "Ταξινόμηση"
    AppLanguage.Romanian -> "Organizare"
    AppLanguage.Hungarian -> "Rendezés"
    AppLanguage.Turkish -> "Düzenle"
    AppLanguage.Malay -> "Susun"
    AppLanguage.Hebrew -> "סידור"
    AppLanguage.English -> "Organize"
}

private fun localizedStartupWatermarkTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "水印边框"
    AppLanguage.TraditionalChinese -> "水印邊框"
    AppLanguage.Japanese -> "透かしフレーム"
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
    AppLanguage.Malay -> "Tera Air"
    AppLanguage.Hebrew -> "סימן מים"
    AppLanguage.English -> "Watermark"
}

internal fun localizedStartupTabContentDescription(language: AppLanguage, tab: StartupWelcomeTab): String = when (tab) {
    StartupWelcomeTab.Organize -> localizedStartupOrganizeTitle(language)
    StartupWelcomeTab.ColorCard -> localizedStartupCreativeTitle(language)
    StartupWelcomeTab.Watermark -> localizedStartupWatermarkTitle(language)
    StartupWelcomeTab.Insights -> localizedStartupInsightsTitle(language)
    StartupWelcomeTab.Mine -> localizedStartupMineTitle(language)
}

private fun localizedStartupInsightsTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "洞察"
    AppLanguage.TraditionalChinese -> "洞察"
    AppLanguage.Japanese -> "インサイト"
    AppLanguage.Korean -> "인사이트"
    AppLanguage.French -> "Statistiques"
    AppLanguage.German -> "Einblicke"
    AppLanguage.Italian -> "Statistiche"
    AppLanguage.Spanish -> "Estadísticas"
    AppLanguage.Portuguese -> "Estatísticas"
    AppLanguage.Russian -> "Аналитика"
    AppLanguage.Thai -> "ข้อมูลเชิงลึก"
    AppLanguage.Vietnamese -> "Thống kê"
    AppLanguage.Indonesian -> "Wawasan"
    AppLanguage.Arabic -> "الرؤى"
    AppLanguage.Ukrainian -> "Аналітика"
    AppLanguage.Polish -> "Statystyki"
    AppLanguage.Czech -> "Přehledy"
    AppLanguage.Dutch -> "Inzichten"
    AppLanguage.Swedish -> "Insikter"
    AppLanguage.Danish -> "Indsigt"
    AppLanguage.Norwegian -> "Innsikt"
    AppLanguage.Finnish -> "Tilastot"
    AppLanguage.Greek -> "Αναλύσεις"
    AppLanguage.Romanian -> "Analize"
    AppLanguage.Hungarian -> "Elemzések"
    AppLanguage.Turkish -> "İçgörüler"
    AppLanguage.Malay -> "Wawasan"
    AppLanguage.Hebrew -> "תובנות"
    AppLanguage.English -> "Insights"
}

private object StartupBottomNavIcons {
    val Insights: ImageVector by lazy {
        ImageVector.Builder(
            name = "StartupBottomNavInsights",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlineNavPath("M3 21H21")
            outlineNavPath("M6 17V11")
            outlineNavPath("M10 17V6")
            outlineNavPath("M14 17V9")
            outlineNavPath("M18 17V4")
        }.build()
    }

    val ColorCard: ImageVector by lazy {
        ImageVector.Builder(
            name = "StartupBottomNavColorCard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlineNavPath("M12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22Z")
            outlineNavPath("M12 16C9.79086 16 8 14.2091 8 12C8 9.79086 9.79086 8 12 8C14.2091 8 16 9.79086 16 12C16 14.2091 14.2091 16 12 16Z")
            listOf(
                "M12 2V8",
                "M12 16V22",
                "M2 12H8",
                "M16 12H22",
                "M4.92896 4.92871L9.1716 9.17135",
                "M14.8284 14.8286L19.071 19.0713",
                "M4.92896 19.0713L9.1716 14.8286",
                "M14.8284 9.17139L19.071 4.92875"
            ).forEach(::outlineNavPath)
        }.build()
    }

    val WatermarkFrame: ImageVector by lazy {
        ImageVector.Builder(
            name = "StartupBottomNavWatermarkFrame",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlineNavPath(
                "M19.4 20H4.6C4.26863 20 4 19.7314 4 19.4V4.6C4 4.26863 4.26863 4 4.6 4H19.4C19.7314 4 20 4.26863 20 4.6V19.4C20 19.7314 19.7314 20 19.4 20Z",
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            outlineNavPath(
                "M11 12V4",
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            outlineNavPath(
                "M4 12H20",
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    val Organize: ImageVector by lazy {
        ImageVector.Builder(
            name = "StartupBottomNavOrganize",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlineNavPath("M7 6L17 6")
            outlineNavPath("M7 9L17 9")
            outlineNavPath("M9 17H15")
            outlineNavPath("M3 12H2.6C2.26863 12 2 12.2686 2 12.6V21.4C2 21.7314 2.26863 22 2.6 22H21.4C21.7314 22 22 21.7314 22 21.4V12.6C22 12.2686 21.7314 12 21.4 12H21M3 12V2.6C3 2.26863 3.26863 2 3.6 2H20.4C20.7314 2 21 2.26863 21 2.6V12M3 12H21")
        }.build()
    }

    val Mine: ImageVector by lazy {
        ImageVector.Builder(
            name = "StartupBottomNavMine",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            filledNavPath("M2.6954 7.18536L11.6954 11.1854L12.3046 9.81464L3.3046 5.81464L2.6954 7.18536ZM12.75 21.5V10.5H11.25V21.5H12.75ZM12.3046 11.1854L21.3046 7.18536L20.6954 5.81464L11.6954 9.81464L12.3046 11.1854Z")
            outlineNavPath("M3 17.1101V6.88992C3 6.65281 3.13964 6.43794 3.35632 6.34164L11.7563 2.6083C11.9115 2.53935 12.0885 2.53935 12.2437 2.6083L20.6437 6.34164C20.8604 6.43794 21 6.65281 21 6.88992V17.1101C21 17.3472 20.8604 17.5621 20.6437 17.6584L12.2437 21.3917C12.0885 21.4606 11.9115 21.4606 11.7563 21.3917L3.35632 17.6584C3.13964 17.5621 3 17.3472 3 17.1101Z")
            outlineNavPath("M7.5 4.5L16.1437 8.34164C16.3604 8.43794 16.5 8.65281 16.5 8.88992V12.5")
        }.build()
    }
}

private object StartupActionIcons {
    val DeepOrganize: ImageVector by lazy {
        ImageVector.Builder(
            name = "StartupDeepOrganize",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            outlineNavPath("M7.375 16.047H16.625", strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            outlineNavPath(
                "M21.25 9.883V17.581C21.25 19.2839 19.8699 20.664 18.167 20.664H5.833C4.13012 20.664 2.75 19.2839 2.75 17.581V6.419C2.75 4.71612 4.13012 3.336 5.833 3.336H8.917C9.9477 3.336 10.9114 3.85198 11.487 4.71308L12.36 6.03908C12.6828 6.52925 13.2304 6.809 13.809 6.809H18.167C19.8679 6.809 21.247 8.18499 21.25 9.883",
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            outlineNavPath(
                "M11.517 4.723L18.444 3.613C19.4314 3.45469 20.222 4.2162 20.222 5.134V7.591",
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }
}

private fun ImageVector.Builder.outlineNavPath(
    pathData: String,
    strokeLineCap: StrokeCap = StrokeCap.Round,
    strokeLineJoin: StrokeJoin = StrokeJoin.Round
) {
    addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = strokeLineCap,
        strokeLineJoin = strokeLineJoin,
        pathFillType = PathFillType.NonZero
    )
}

private fun ImageVector.Builder.filledNavPath(pathData: String) {
    addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.NonZero
    )
}

internal fun startupOrganizeStartLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u5f00\u59cb\u6574\u7406"
    AppLanguage.TraditionalChinese -> "\u958b\u59cb\u6574\u7406"
    AppLanguage.Japanese -> "\u6574\u7406\u3092\u59cb\u3081\u308b"
    AppLanguage.Korean -> "\uc815\ub9ac \uc2dc\uc791"
    AppLanguage.French -> "Commencer à organiser"
    AppLanguage.German -> "Organisieren beginnen"
    AppLanguage.Italian -> "Inizia a organizzare"
    AppLanguage.Spanish -> "Empezar a organizar"
    AppLanguage.Portuguese -> "Começar a organizar"
    AppLanguage.Russian -> "\u041d\u0430\u0447\u0430\u0442\u044c \u0443\u043f\u043e\u0440\u044f\u0434\u043e\u0447\u0438\u0432\u0430\u0442\u044c"
    AppLanguage.Thai -> "\u0e40\u0e23\u0e34\u0e48\u0e21\u0e08\u0e31\u0e14\u0e40\u0e23\u0e35\u0e22\u0e07"
    AppLanguage.Vietnamese -> "B\u1eaft \u0111\u1ea7u s\u1eafp x\u1ebfp"
    AppLanguage.Indonesian -> "Mulai Mengatur"
    AppLanguage.Arabic -> "\u0627\u0628\u062f\u0623 \u0627\u0644\u062a\u0646\u0638\u064a\u0645"
    AppLanguage.Ukrainian -> "Почати розбір"
    AppLanguage.Polish -> "Zacznij porządki"
    AppLanguage.Czech -> "Začít třídit"
    AppLanguage.Dutch -> "Begin met ordenen"
    AppLanguage.Swedish -> "Börja sortera"
    AppLanguage.Danish -> "Begynd at sortere"
    AppLanguage.Norwegian -> "Begynn å rydde"
    AppLanguage.Finnish -> "Aloita järjestäminen"
    AppLanguage.Greek -> "Έναρξη ταξινόμησης"
    AppLanguage.Romanian -> "Începeți organizarea"
    AppLanguage.Hungarian -> "Rendezés indítása"
    AppLanguage.Turkish -> "Düzenlemeye Başla"
    AppLanguage.Malay -> "Mula Menyusun"
    AppLanguage.Hebrew -> "התחלת הסידור"
    AppLanguage.English -> "Start Organizing"
}

private fun startupProgressStatsLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u6574\u7406\u8fdb\u5ea6\u7edf\u8ba1"
    AppLanguage.TraditionalChinese -> "\u6574\u7406\u9032\u5ea6\u7d71\u8a08"
    AppLanguage.Japanese -> "\u6574\u7406\u9032\u5ea6"
    AppLanguage.Korean -> "\uc815\ub9ac \uc9c4\ub3d9"
    AppLanguage.French -> "Progression de l'organisation"
    AppLanguage.German -> "Organisationsfortschritt"
    AppLanguage.Italian -> "Progresso dell'organizzazione"
    AppLanguage.Spanish -> "Progreso de organizaci\u00f3n"
    AppLanguage.Portuguese -> "Progresso da Organiza\u00e7\u00e3o"
    AppLanguage.Russian -> "\u041f\u0440\u043e\u0433\u0440\u0435\u0441\u0441 \u0443\u043f\u043e\u0440\u044f\u0434\u043e\u0447\u0438\u0432\u0430\u043d\u0438\u044f"
    AppLanguage.Thai -> "\u0e04\u0e27\u0e32\u0e21\u0e01\u0e49\u0e2d\u0e32\u0e02\u0e13\u0e30\u0e08\u0e31\u0e14\u0e40\u0e23\u0e35\u0e22\u0e07"
    AppLanguage.Vietnamese -> "Ti\u1ebfn tr\u00ecnh s\u1eafp x\u1ebfp"
    AppLanguage.Indonesian -> "Kemajuan Pengaturan"
    AppLanguage.Arabic -> "\u062a\u0642\u062f\u0645 \u0627\u0644\u062a\u0646\u0638\u064a\u0645"
    AppLanguage.Ukrainian -> "Хід розбору"
    AppLanguage.Polish -> "Postęp porządkowania"
    AppLanguage.Czech -> "Průběh třídění"
    AppLanguage.Dutch -> "Voortgang ordenen"
    AppLanguage.Swedish -> "Sorteringens framsteg"
    AppLanguage.Danish -> "Fremdrift i sortering"
    AppLanguage.Norwegian -> "Fremdrift for rydding"
    AppLanguage.Finnish -> "Järjestelyn edistyminen"
    AppLanguage.Greek -> "Πρόοδος ταξινόμησης"
    AppLanguage.Romanian -> "Progresul organizării"
    AppLanguage.Hungarian -> "Rendezés előrehaladása"
    AppLanguage.Turkish -> "Düzenleme İlerlemesi"
    AppLanguage.Malay -> "Kemajuan Penyusunan"
    AppLanguage.Hebrew -> "התקדמות הסידור"
    AppLanguage.English -> "Organizing Progress"
}

private fun startupFeatureSubtitle(language: AppLanguage, tab: StartupWelcomeTab): String = when (tab) {
    StartupWelcomeTab.ColorCard -> when (language) {
        AppLanguage.SimplifiedChinese -> "?????????"
        AppLanguage.TraditionalChinese -> "?????????"
        else -> "Keep the colors you want to return to."
    }
    StartupWelcomeTab.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese -> "?????????????????"
        AppLanguage.TraditionalChinese -> "?????????????????"
        else -> "Keep time, place, and camera details in the frame."
    }
    else -> ""
}

private fun startupFeatureButtonLabel(language: AppLanguage, tab: StartupWelcomeTab): String = when (tab) {
    StartupWelcomeTab.ColorCard -> when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> "进入色彩"
        else -> "Open Color Card"
    }
    StartupWelcomeTab.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> "进入水印边框"
        else -> "Open Watermark Frame"
    }
    else -> ""
}

private enum class StartupQuickActionType {
    ColorCard,
    Watermark
}

private fun resolveStartupQuickActionLabel(
    language: AppLanguage,
    type: StartupQuickActionType
): String = when (type) {
    StartupQuickActionType.ColorCard -> when (language) {
        AppLanguage.SimplifiedChinese -> "??"
        AppLanguage.TraditionalChinese -> "??"
        AppLanguage.Japanese -> "??????"
        AppLanguage.Korean -> "?? ??"
        AppLanguage.French -> "Carte de Couleurs"
        AppLanguage.German -> "Farbkarte"
        AppLanguage.Italian -> "Scheda Colori"
        AppLanguage.Spanish -> "Tarjeta de Colores"
        AppLanguage.Portuguese -> "Cart?o de Cores"
        AppLanguage.Russian -> "???????? ?????"
        AppLanguage.Thai -> "???????"
        AppLanguage.Vietnamese -> "Th? M?u"
        AppLanguage.Indonesian -> "Kartu Warna"
        AppLanguage.Arabic -> "????? ???????"
        else -> "Color Card"
    }
    StartupQuickActionType.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese -> "??"
        AppLanguage.TraditionalChinese -> "??"
        AppLanguage.Japanese -> "???"
        AppLanguage.Korean -> "????"
        AppLanguage.French -> "Filigrane"
        AppLanguage.German -> "Wasserzeichen"
        AppLanguage.Italian -> "Filigrana"
        AppLanguage.Spanish -> "Marca de Agua"
        AppLanguage.Portuguese -> "Marca d'?gua"
        AppLanguage.Russian -> "??????? ????"
        AppLanguage.Thai -> "??????"
        AppLanguage.Vietnamese -> "D?u N??c"
        AppLanguage.Indonesian -> "Tanda Air"
        AppLanguage.Arabic -> "????? ?????"
        else -> "Watermark"
    }
}

private fun startupQuickActionLabel(
    language: AppLanguage,
    type: StartupQuickActionType
): String = when (type) {
    StartupQuickActionType.ColorCard -> when (language) {
        AppLanguage.SimplifiedChinese -> "??"
        AppLanguage.TraditionalChinese -> "??"
        AppLanguage.Japanese -> "??????"
        AppLanguage.Korean -> "?? ??"
        AppLanguage.French -> "Carte de Couleurs"
        AppLanguage.German -> "Farbkarte"
        AppLanguage.Italian -> "Scheda Colori"
        AppLanguage.Spanish -> "Tarjeta de Colores"
        AppLanguage.Portuguese -> "Cart?o de Cores"
        AppLanguage.Russian -> "???????? ?????"
        AppLanguage.Thai -> "???????"
        AppLanguage.Vietnamese -> "Th? M?u"
        AppLanguage.Indonesian -> "Kartu Warna"
        AppLanguage.Arabic -> "????? ???????"
        else -> "Color Card"
    }
    StartupQuickActionType.Watermark -> when (language) {
        AppLanguage.SimplifiedChinese -> "??"
        AppLanguage.TraditionalChinese -> "??"
        AppLanguage.Japanese -> "???"
        AppLanguage.Korean -> "????"
        AppLanguage.French -> "Filigrane"
        AppLanguage.German -> "Wasserzeichen"
        AppLanguage.Italian -> "Filigrana"
        AppLanguage.Spanish -> "Marca de Agua"
        AppLanguage.Portuguese -> "Marca d'?gua"
        AppLanguage.Russian -> "??????? ????"
        AppLanguage.Thai -> "??????"
        AppLanguage.Vietnamese -> "D?u N??c"
        AppLanguage.Indonesian -> "Tanda Air"
        AppLanguage.Arabic -> "????? ?????"
        else -> "Watermark"
    }
}

@Composable
private fun StartupQuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(18.dp)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = buttonShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
