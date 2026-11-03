package com.voicelike.app

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

enum class LabSlideshowSource {
    Favorites,
    Folder
}

enum class LabPlaybackMode {
    HomeCard,
    FullscreenSlideshow
}

data class LabSlideshowConfig(
    val source: LabSlideshowSource = LabSlideshowSource.Favorites,
    val playbackMode: LabPlaybackMode = LabPlaybackMode.HomeCard,
    val folderName: String = "",
    val intervalSec: Int = 3,
    val fullscreenLandscape: Boolean = false,
    val displayMode: ElectronicAlbumDisplayMode = ElectronicAlbumDisplayMode.Smart,
    val wholeViewBlurBackgroundEnabled: Boolean = false,
    val durationEnabled: Boolean = false,
    val durationMin: Int = 10,
    val shuffleEnabled: Boolean = false
)

@Composable
fun LabHomePage(
    onOpenAlbumSettings: () -> Unit,
    onOpenCopyrightVerify: () -> Unit,
    onOpenLiveSyncTool: () -> Unit,
    advancedTextureEnabled: Boolean,
    onAdvancedTextureEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val strings = getNewPageStrings(appLanguage)
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    fun performHaptic() {
        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = strings.labPageTitle,
            onBack = {
                performHaptic()
                onBack()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = strings.labAlbumTitle,
                subtitle = strings.labAlbumSubtitle,
                backgroundColor = Color(0xFF10B981),
                titleColor = Color.White,
                count = 0,
                showCount = false,
                subtitleMaxLines = 2,
                onClick = {
                    performHaptic()
                    onOpenAlbumSettings()
                },
                modifier = Modifier.fillMaxWidth().height(112.dp)
            )

            FeatureCard(
                title = getWatermarkExtraCopy(LocalAppLanguage.current).copyrightVerifyTitle,
                subtitle = getWatermarkExtraCopy(LocalAppLanguage.current).copyrightVerifySubtitle,
                backgroundColor = Color(0xFF4F83CC),
                titleColor = Color.White,
                count = 0,
                showCount = false,
                subtitleMaxLines = 2,
                onClick = {
                    performHaptic()
                    onOpenCopyrightVerify()
                },
                modifier = Modifier.fillMaxWidth().height(112.dp)
            )

            Text(
                text = strings.labDisclaimerText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(36.dp))
        }
    }
}

private val LabToggleDisabledBase = Color(0xFF6B7280)

private fun labToggleCardBackground(baseColor: Color, enabled: Boolean): Color =
    if (enabled) baseColor else lerp(baseColor, LabToggleDisabledBase, 0.74f)

private fun labToggleTitleColor(enabled: Boolean): Color =
    if (enabled) Color.White else Color(0xFFF8FAFC)

@Composable
private fun labToggleSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Color.White.copy(alpha = 0.42f),
    uncheckedThumbColor = Color(0xFFF8FAFC),
    uncheckedTrackColor = Color(0xFF334155)
)

private val LAB_INTERVAL_OPTIONS_SEC: List<Int> = buildList {
    addAll(2..60)
    for (minute in 2..10) add(minute * 60)
}

@Composable
fun ElectronicAlbumSettingsPage(
    folderOptions: List<String>,
    config: LabSlideshowConfig,
    onConfigChange: (LabSlideshowConfig) -> Unit,
    onStartSlideshow: (LabSlideshowConfig) -> Unit,
    onBack: () -> Unit
) {
    val strings = getNewPageStrings(LocalAppLanguage.current)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    fun performHaptic() {
        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    var showFolderDialog by remember { mutableStateOf(false) }
    var draftConfig by remember(config, folderOptions) {
        mutableStateOf(
            if (config.source == LabSlideshowSource.Folder && config.folderName.isNotBlank() && !folderOptions.contains(config.folderName)) {
                config.copy(folderName = folderOptions.firstOrNull().orEmpty())
            } else {
                config
            }
        )
    }
    val intervalIndex = remember(draftConfig.intervalSec) {
        LAB_INTERVAL_OPTIONS_SEC.indexOfFirst { it == draftConfig.intervalSec }.takeIf { it >= 0 } ?: 1
    }

    fun commit(newConfig: LabSlideshowConfig) {
        draftConfig = newConfig
        onConfigChange(newConfig)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = strings.labAlbumSettingsTitle,
            onBack = {
                performHaptic()
                onBack()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .advancedTextureCard(
                        shape = RoundedCornerShape(16.dp),
                        tint = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.16f)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = strings.labSourcePrefix,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabChoiceChip(
                            text = strings.labSourceFavorites,
                            selected = draftConfig.source == LabSlideshowSource.Favorites,
                            isDark = isDark,
                            onClick = {
                                performHaptic()
                                commit(draftConfig.copy(source = LabSlideshowSource.Favorites))
                            }
                        )
                        LabChoiceChip(
                            text = strings.labSourceFolder,
                            selected = draftConfig.source == LabSlideshowSource.Folder,
                            isDark = isDark,
                            onClick = {
                                performHaptic()
                                val fallback = draftConfig.folderName.ifBlank { folderOptions.firstOrNull().orEmpty() }
                                commit(draftConfig.copy(source = LabSlideshowSource.Folder, folderName = fallback))
                            }
                        )
                    }

                    if (draftConfig.source == LabSlideshowSource.Folder) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                .bounceClick(onClick = {
                                    performHaptic()
                                    showFolderDialog = true
                                })
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = draftConfig.folderName.ifBlank { strings.labSelectFolder },
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                            )
                        }
                    }

                    Text(
                        text = strings.labPlaybackModeTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabChoiceChip(
                            text = strings.labPlaybackModeCard,
                            selected = draftConfig.playbackMode == LabPlaybackMode.HomeCard,
                            isDark = isDark,
                            onClick = {
                                performHaptic()
                                commit(draftConfig.copy(playbackMode = LabPlaybackMode.HomeCard))
                            }
                        )
                        LabChoiceChip(
                            text = strings.labPlaybackModeFullscreen,
                            selected = draftConfig.playbackMode == LabPlaybackMode.FullscreenSlideshow,
                            isDark = isDark,
                            onClick = {
                                performHaptic()
                                commit(draftConfig.copy(playbackMode = LabPlaybackMode.FullscreenSlideshow))
                            }
                        )
                    }

                    if (draftConfig.playbackMode == LabPlaybackMode.FullscreenSlideshow) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.labFullscreenLandscapeLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = draftConfig.fullscreenLandscape,
                                onCheckedChange = { enabled ->
                                    performHaptic()
                                    commit(draftConfig.copy(fullscreenLandscape = enabled))
                                },
                                modifier = Modifier.advancedTextureButton(shape = CircleShape)
                            )
                        }

                        Text(
                            text = strings.labDisplayModeTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            listOf(
                                ElectronicAlbumDisplayMode.Smart,
                                ElectronicAlbumDisplayMode.ScaleToFitCenter,
                                ElectronicAlbumDisplayMode.CropToFitCenter,
                                ElectronicAlbumDisplayMode.ZoomIn,
                                ElectronicAlbumDisplayMode.ZoomOut
                            ).forEach { mode ->
                                LabChoiceChip(
                                    text = mode.label(strings),
                                    selected = draftConfig.displayMode == mode,
                                    isDark = isDark,
                                    onClick = {
                                        performHaptic()
                                        commit(draftConfig.copy(displayMode = mode))
                                    }
                                )
                            }
                        }

                        if (draftConfig.displayMode == ElectronicAlbumDisplayMode.ScaleToFitCenter) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.labWholeViewBlurBackgroundLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Switch(
                                    checked = draftConfig.wholeViewBlurBackgroundEnabled,
                                    onCheckedChange = { enabled ->
                                        performHaptic()
                                        commit(draftConfig.copy(wholeViewBlurBackgroundEnabled = enabled))
                                    },
                                    modifier = Modifier.advancedTextureButton(shape = CircleShape)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            strings.labIntervalLabel(draftConfig.intervalSec),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = intervalIndex.toFloat(),
                            onValueChange = { value ->
                                val index = value.roundToInt().coerceIn(0, LAB_INTERVAL_OPTIONS_SEC.lastIndex)
                                commit(draftConfig.copy(intervalSec = LAB_INTERVAL_OPTIONS_SEC[index]))
                            },
                            valueRange = 0f..LAB_INTERVAL_OPTIONS_SEC.lastIndex.toFloat(),
                            steps = (LAB_INTERVAL_OPTIONS_SEC.size - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                                activeTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                                inactiveTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f),
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.labShuffleLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = draftConfig.shuffleEnabled,
                            onCheckedChange = { enabled ->
                                performHaptic()
                                commit(draftConfig.copy(shuffleEnabled = enabled))
                            },
                            modifier = Modifier.advancedTextureButton(shape = CircleShape)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.labDurationSwitchLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = draftConfig.durationEnabled,
                            onCheckedChange = { enabled ->
                                performHaptic()
                                commit(draftConfig.copy(durationEnabled = enabled))
                            },
                            modifier = Modifier.advancedTextureButton(shape = CircleShape)
                        )
                    }

                    if (draftConfig.durationEnabled) {
                        Column {
                            Text(strings.labDurationLabel(draftConfig.durationMin), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = draftConfig.durationMin.toFloat(),
                                onValueChange = { value ->
                                    commit(draftConfig.copy(durationMin = value.roundToInt().coerceIn(1, 180)))
                                },
                                valueRange = 1f..180f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                                    activeTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                                    inactiveTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                )
                            )
                        }
                    } else {
                        Text(
                            strings.labDurationForever,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            Text(
                text = strings.labPlaybackHint,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Button(
                onClick = {
                    performHaptic()
                    onStartSlideshow(draftConfig)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                    contentColor = if (isDark) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .advancedTextureButton(shape = RoundedCornerShape(12.dp))
            ) {
                Text(strings.labStartPlayback, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(36.dp))
        }
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text(strings.labSelectFolder, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (folderOptions.isEmpty()) {
                        Text(strings.labNoFolderAvailable)
                    } else {
                        folderOptions.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (folder == draftConfig.folderName) {
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .bounceClick(onClick = {
                                        performHaptic()
                                        commit(draftConfig.copy(folderName = folder))
                                        showFolderDialog = false
                                    })
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folder,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (folder == draftConfig.folderName) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    performHaptic()
                    showFolderDialog = false
                }) {
                    Text(strings.labCloseText)
                }
            }
        )
    }
}

@Composable
private fun LabChoiceChip(
    text: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
            selectedLabelColor = if (isDark) Color.Black else Color.White,
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
            labelColor = MaterialTheme.colorScheme.onBackground
        ),
        border = null,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .height(40.dp)
            .advancedTextureButton(shape = RoundedCornerShape(20.dp))
    )
}

@Composable
fun ElectronicAlbumPlaybackPage(
    queue: List<MediaItem>,
    intervalMs: Long,
    durationMs: Long?,
    playbackMode: LabPlaybackMode,
    forceLandscapeInFullscreen: Boolean,
    displayMode: ElectronicAlbumDisplayMode,
    wholeViewBlurBackgroundEnabled: Boolean,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val strings = getNewPageStrings(LocalAppLanguage.current)
    val normalizedQueue = remember(queue) {
        queue.filter { it.type == "photo" && !it.isLivePhoto && !it.isMotionPhoto }
    }
    var currentIndex by remember(normalizedQueue) { mutableIntStateOf(0) }
    val current = normalizedQueue.getOrNull(currentIndex)
    var hintToastShown by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isUserDraggingCard by remember { mutableStateOf(false) }
    val autoSwipeOffsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val isFullscreenMode = playbackMode == LabPlaybackMode.FullscreenSlideshow
    fun advanceToNext() {
        if (normalizedQueue.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % normalizedQueue.size
        }
    }

    BackHandler(enabled = true) { onExit() }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    DisposableEffect(activity, isFullscreenMode) {
        val window = activity?.window
        val decorView = window?.decorView
        val controller = if (window != null && decorView != null) {
            WindowInsetsControllerCompat(window, decorView)
        } else {
            null
        }
        if (isFullscreenMode && window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            if (isFullscreenMode && window != null) {
                controller?.show(WindowInsetsCompat.Type.statusBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    DisposableEffect(activity, isFullscreenMode, forceLandscapeInFullscreen) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (activity != null && isFullscreenMode && forceLandscapeInFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            if (activity != null && isFullscreenMode && forceLandscapeInFullscreen) {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    LaunchedEffect(normalizedQueue) {
        if (normalizedQueue.isEmpty()) {
            onExit()
        } else {
            currentIndex = 0
        }
    }

    suspend fun autoAdvance() {
        if (playbackMode == LabPlaybackMode.HomeCard) {
            autoSwipeOffsetX.snapTo(0f)
            autoSwipeOffsetX.animateTo(
                targetValue = 1200f,
                animationSpec = tween(durationMillis = 240, easing = androidx.compose.animation.core.FastOutLinearInEasing)
            )
            advanceToNext()
            autoSwipeOffsetX.snapTo(0f)
        } else {
            advanceToNext()
        }
    }

    LaunchedEffect(normalizedQueue, intervalMs, isPaused, isUserDraggingCard, playbackMode) {
        if (normalizedQueue.size <= 1) return@LaunchedEffect
        while (isActive) {
            if (isPaused || isUserDraggingCard) {
                delay(120)
                continue
            }
            delay(intervalMs.coerceAtLeast(800L))
            autoAdvance()
        }
    }

    LaunchedEffect(durationMs) {
        val finalDurationMs = durationMs
        if (finalDurationMs != null) {
            delay(finalDurationMs.coerceAtLeast(30_000L))
            onExit()
        }
    }

    LaunchedEffect(displayMode, currentIndex, normalizedQueue) {
        if (displayMode != ElectronicAlbumDisplayMode.Smart || normalizedQueue.isEmpty()) return@LaunchedEffect
        normalizedQueue.getOrNull(currentIndex)?.let { ElectronicAlbumSmartFraming.warmUp(context, it) }
        normalizedQueue.getOrNull((currentIndex + 1) % normalizedQueue.size)?.let {
            ElectronicAlbumSmartFraming.warmUp(context, it)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreenMode) Color.Black else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
        val playbackModifier = if (isFullscreenMode) {
            Modifier.pointerInput(normalizedQueue, widthPx) {
                detectTapGestures(
                    onTap = { pos ->
                        when {
                            pos.x > widthPx * 0.67f -> {
                                if (normalizedQueue.isNotEmpty()) {
                                    currentIndex = (currentIndex + 1) % normalizedQueue.size
                                }
                            }
                            pos.x < widthPx * 0.33f -> {
                                if (normalizedQueue.isNotEmpty()) {
                                    currentIndex = if (currentIndex == 0) {
                                        normalizedQueue.lastIndex
                                    } else {
                                        currentIndex - 1
                                    }
                                }
                            }
                            else -> {
                                isPaused = !isPaused
                            }
                        }
                        if (!hintToastShown) {
                            Toast.makeText(context, strings.labPlaybackHint, Toast.LENGTH_SHORT).show()
                            hintToastShown = true
                        }
                    },
                    onLongPress = { onExit() }
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(playbackModifier),
            contentAlignment = Alignment.Center
        ) {
            if (current != null) {
                if (playbackMode == LabPlaybackMode.HomeCard) {
                    val stackRenderCount = normalizedQueue.size.coerceAtMost(6)
                    val stackItems = remember(normalizedQueue, currentIndex) {
                        if (normalizedQueue.isEmpty()) {
                            emptyList()
                        } else {
                            List(stackRenderCount) { offset ->
                                normalizedQueue[(currentIndex + offset) % normalizedQueue.size]
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        for (stackIndex in (stackItems.lastIndex downTo 0)) {
                            val stackItem = stackItems[stackIndex]
                            val isTop = stackIndex == 0
                            val shouldAnalyze = stackIndex <= 1
                            val canAdvanceStack = normalizedQueue.size > 1
                            key("${currentIndex + stackIndex}-${stackItem.id}") {
                                Box(
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = 1.08f
                                        scaleY = 1.08f
                                        translationX = if (isTop) autoSwipeOffsetX.value else 0f
                                        alpha = if (isTop) {
                                            (1f - (kotlin.math.abs(autoSwipeOffsetX.value) / 1200f)).coerceIn(0.55f, 1f)
                                        } else {
                                            1f
                                        }
                                    }
                                ) {
                                    DraggableCard(
                                        item = stackItem,
                                        isTopCard = isTop,
                                        stackIndex = stackIndex,
                                        muteVideos = true,
                                        pauseMainVideos = false,
                                        topCardPlaybackArmed = true,
                                        shouldAnalyze = shouldAnalyze,
                                        blockVideoDuringEntry = false,
                                        disableStackAnimation = false,
                                        suppressActionIndicators = true,
                                        leftAction = SwipeAction.Delete,
                                        rightAction = SwipeAction.Like,
                                        upAction = SwipeAction.Keep,
                                        onSwipeLeft = { if (isTop && canAdvanceStack) advanceToNext() },
                                        onSwipeRight = { if (isTop && canAdvanceStack) advanceToNext() },
                                        onSwipeUp = { if (isTop && canAdvanceStack) advanceToNext() },
                                        onDetail = {},
                                        onDragStart = { if (isTop) isUserDraggingCard = true },
                                        onDragCancel = { if (isTop) isUserDraggingCard = false },
                                        onDragEnd = if (isTop) { _, _ ->
                                            isUserDraggingCard = false
                                            null
                                        } else null,
                                        allowSwipeLeft = canAdvanceStack,
                                        allowSwipeRight = canAdvanceStack,
                                        allowSwipeUp = canAdvanceStack
                                    )
                                }
                            }
                        }
                    }
                } else {
                    AutoPanFullscreenMedia(
                        item = current,
                        intervalMs = intervalMs,
                        displayMode = displayMode,
                        wholeViewBlurBackgroundEnabled = wholeViewBlurBackgroundEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoPanFullscreenMedia(
    item: MediaItem,
    intervalMs: Long,
    displayMode: ElectronicAlbumDisplayMode,
    wholeViewBlurBackgroundEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier) {
        val containerRatio = (maxWidth.value / maxHeight.value).takeIf { it > 0f } ?: 1f
        val imageRequest = remember(item.id, item.uri) {
            ImageRequest.Builder(context)
                .data(item.uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .crossfade(false)
                .build()
        }
        val smartFrameState = produceState<ElectronicAlbumSmartFrame?>(
            initialValue = if (displayMode == ElectronicAlbumDisplayMode.Smart) {
                ElectronicAlbumSmartFraming.peek(item.id)
            } else {
                null
            },
            key1 = item.id,
            key2 = displayMode
        ) {
            if (displayMode != ElectronicAlbumDisplayMode.Smart) {
                value = null
                return@produceState
            }
            value = ElectronicAlbumSmartFraming.peek(item.id)
            if (value == null) {
                ElectronicAlbumSmartFraming.warmUp(context, item)
                value = ElectronicAlbumSmartFraming.peek(item.id)
            }
        }
        val motionDurationMs = remember(intervalMs) {
            ((intervalMs.coerceAtLeast(1200L) * 0.92f).roundToInt()).coerceAtLeast(1800)
        }
        val biasAnimatable = remember(item.id, displayMode) { Animatable(-1f) }
        val zoomProgressAnimatable = remember(item.id, displayMode) { Animatable(0f) }

        LaunchedEffect(item.id, displayMode, motionDurationMs) {
            val startBias = when (displayMode) {
                ElectronicAlbumDisplayMode.ScaleToFitCenter,
                ElectronicAlbumDisplayMode.ZoomIn,
                ElectronicAlbumDisplayMode.ZoomOut -> 0f
                else -> -1f
            }
            val targetBias = when (displayMode) {
                ElectronicAlbumDisplayMode.Smart,
                ElectronicAlbumDisplayMode.CropToFitCenter -> 1f
                ElectronicAlbumDisplayMode.ScaleToFitCenter -> 0f
                ElectronicAlbumDisplayMode.ZoomIn,
                ElectronicAlbumDisplayMode.ZoomOut -> 0f
            }
            biasAnimatable.stop()
            biasAnimatable.snapTo(startBias)
            if (startBias != targetBias) {
                biasAnimatable.animateTo(
                    targetValue = targetBias,
                    animationSpec = tween(
                        durationMillis = motionDurationMs,
                        easing = LinearEasing
                    )
                )
            }
        }

        LaunchedEffect(item.id, displayMode, motionDurationMs) {
            zoomProgressAnimatable.stop()
            zoomProgressAnimatable.snapTo(0f)
            if (
                displayMode == ElectronicAlbumDisplayMode.ZoomIn ||
                displayMode == ElectronicAlbumDisplayMode.ZoomOut
            ) {
                zoomProgressAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = motionDurationMs,
                        easing = LinearEasing
                    )
                )
            }
        }

        val displaySpec =
            resolveElectronicAlbumDisplaySpec(
                mode = displayMode,
                item = item,
                containerRatio = containerRatio,
                animatedBias = biasAnimatable.value,
                motionProgress = zoomProgressAnimatable.value,
                smartFrame = smartFrameState.value
            )
        val useBlurredWholeViewBackground =
            displayMode == ElectronicAlbumDisplayMode.ScaleToFitCenter && wholeViewBlurBackgroundEnabled

        Box(modifier = Modifier.fillMaxSize()) {
            if (useBlurredWholeViewBackground) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.18f
                            scaleY = 1.18f
                            alpha = 0.96f
                        }
                        .blur(38.dp),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f))
                )
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = displaySpec.extraScale
                        scaleY = displaySpec.extraScale
                    },
                contentScale = displaySpec.contentScale,
                alignment = displaySpec.alignment
            )
        }
    }
}



