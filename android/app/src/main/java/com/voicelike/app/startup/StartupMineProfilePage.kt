package com.voicelike.app.startup

import com.voicelike.app.rememberLikesGalleryColumns
import com.voicelike.app.likesGallerySpacing
import com.voicelike.app.likesGalleryPinchToResize
import com.voicelike.app.likesGalleryCornerRadius
import com.voicelike.app.LikesGalleryColumnsButton
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.voicelike.app.AppLanguage
import com.voicelike.app.AppStats
import com.voicelike.app.AvatarTypeSelectorRow
import com.voicelike.app.campfireSupporters
import com.voicelike.app.LikesCommentSheet
import com.voicelike.app.LikesGalleryItemCard
import com.voicelike.app.LikesGalleryPreviewOverlay
import com.voicelike.app.LikesPreviewActionButtons
import com.voicelike.app.LocalAppLanguage
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.LocalHapticsEnabled
import com.voicelike.app.MediaItem
import com.voicelike.app.PreferencesManager
import com.voicelike.app.R
import com.voicelike.app.ShootingStatsView
import com.voicelike.app.bottomNavigationBarPadding
import com.voicelike.app.bounceClick
import com.voicelike.app.getNewPageStrings
import com.voicelike.app.persistUserAvatarImage
import com.voicelike.app.resolveUserAvatarModel
import com.voicelike.app.shareMedia
import com.voicelike.app.helpcenter.getHelpCenterActionTitle
import java.io.File
import java.util.Locale
import kotlin.math.floor
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StartupMineProfilePage(
    title: String,
    stats: AppStats,
    totalPhotos: Int,
    avatarType: String,
    customAvatarPath: String?,
    allMedia: List<MediaItem>,
    likedIds: Set<Long>,
    likedPhotos: List<MediaItem>,
    onAlbumInsightsClick: (() -> Unit)?,
    onGlobePageClick: (() -> Unit)?,
    onProcessedClick: (() -> Unit)?,
    onCampfireClick: (() -> Unit)?,
    onHelpCenterClick: (() -> Unit)?,
    onLikesClick: (() -> Unit)?,
    onSettingsClick: (() -> Unit)?,
    onAvatarTypeChange: ((String) -> Unit)?,
    onCustomAvatarPathChange: ((String?) -> Unit)?,
    scrollToTopSignal: Int = 0,
    onBottomBarScrollSnapshotChanged: (StartupBottomBarScrollSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember(appContext) {
        appContext.getSharedPreferences(STARTUP_MINE_PROFILE_PREFS_NAME, Context.MODE_PRIVATE)
    }
    val scope = rememberCoroutineScope()
    val appPrefs = remember(appContext) { PreferencesManager(appContext) }
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val performHaptic = remember(haptic, hapticsAllowed) {
        {
            if (hapticsAllowed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    var customStatsBackgroundPath by remember {
        mutableStateOf(
            prefs.getString(STARTUP_MINE_STATS_BACKGROUND_PATH_KEY, null)
                ?.takeIf { File(it).exists() }
        )
    }
    var customStatsBackgroundUri by remember {
        mutableStateOf(prefs.getString(STARTUP_MINE_STATS_BACKGROUND_URI_KEY, null))
    }
    var userDisplayName by remember {
        mutableStateOf(
            prefs.getString(STARTUP_MINE_USER_DISPLAY_NAME_KEY, STARTUP_MINE_DEFAULT_USER_NAME)
                .orEmpty()
                .trim()
                .ifBlank { STARTUP_MINE_DEFAULT_USER_NAME }
        )
    }
    val statsBackgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = persistStartupMineBackgroundImage(appContext, uri)
                if (savedPath != null) {
                    customStatsBackgroundPath = savedPath
                    customStatsBackgroundUri = null
                    prefs.edit()
                        .putString(STARTUP_MINE_STATS_BACKGROUND_PATH_KEY, savedPath)
                        .remove(STARTUP_MINE_STATS_BACKGROUND_URI_KEY)
                        .apply()
                }
            }
        }
    }
    LaunchedEffect(customStatsBackgroundUri, customStatsBackgroundPath) {
        val legacyUriString = customStatsBackgroundUri ?: return@LaunchedEffect
        if (customStatsBackgroundPath != null) return@LaunchedEffect
        val migratedPath = persistStartupMineBackgroundImage(appContext, Uri.parse(legacyUriString))
        if (migratedPath != null) {
            customStatsBackgroundPath = migratedPath
            customStatsBackgroundUri = null
            prefs.edit()
                .putString(STARTUP_MINE_STATS_BACKGROUND_PATH_KEY, migratedPath)
                .remove(STARTUP_MINE_STATS_BACKGROUND_URI_KEY)
                .apply()
        } else {
            customStatsBackgroundUri = null
            prefs.edit().remove(STARTUP_MINE_STATS_BACKGROUND_URI_KEY).apply()
        }
    }
    val galleryState = rememberLazyStaggeredGridState()
    val avatarModel = remember(avatarType, customAvatarPath) {
        resolveUserAvatarModel(avatarType, customAvatarPath)
    }
    val statsBackgroundModel = remember(customStatsBackgroundPath, customStatsBackgroundUri) {
        customStatsBackgroundPath
            ?.let(::File)
            ?: customStatsBackgroundUri
            ?.let(Uri::parse)
            ?: R.drawable.watermark_template_demo
    }
    var localLikedPhotos by remember { mutableStateOf(likedPhotos) }
    LaunchedEffect(likedPhotos) {
        localLikedPhotos = likedPhotos
    }
    val mineGalleryItems = remember(localLikedPhotos) {
        localLikedPhotos
            .filter { it.type == "photo" }
            .distinctBy { it.id }
            .sortedByDescending { it.dateAdded }
    }
    var danmakuEnabled by remember { mutableStateOf(appPrefs.danmakuEnabled) }
    var galleryPreviewVisible by remember { mutableStateOf(false) }
    var galleryPreviewItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var galleryPreviewStartIndex by remember { mutableIntStateOf(0) }
    var galleryPreviewSourceBounds by remember { mutableStateOf<Rect?>(null) }
    var galleryHiddenSourceItemId by remember { mutableStateOf<Long?>(null) }
    var showCommentSheet by remember { mutableStateOf(false) }
    var currentCommentItem by remember { mutableStateOf<MediaItem?>(null) }
    var commentsRevision by remember { mutableIntStateOf(0) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showUserNameDialog by remember { mutableStateOf(false) }
    var showCampfireBadgeDialog by remember { mutableStateOf(false) }
    var selectedDrawerPanel by rememberSaveable { mutableStateOf(StartupMineDrawerPanel.Likes) }
    val isSelfReportedCampfireParticipation = appPrefs.campfireSelfParticipationChecked
    val isCampfireSupporter = remember(userDisplayName, isSelfReportedCampfireParticipation) {
        isSelfReportedCampfireParticipation || campfireSupporters.any { supporter ->
            supporter.idLabel.trim().equals(userDisplayName.trim(), ignoreCase = true)
        }
    }
    val galleryCardBoundsById = remember { mutableStateMapOf<Long, Rect>() }
    val galleryMediaSizeById = remember { mutableStateMapOf<Long, IntSize>() }
    val bottomBarNearTopThresholdPx = with(density) { 56.dp.roundToPx() }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = persistUserAvatarImage(appContext, uri)
                if (savedPath != null) {
                    onCustomAvatarPathChange?.invoke(savedPath)
                }
            }
        }
    }

    LaunchedEffect(
        galleryState,
        galleryPreviewVisible,
        showCommentSheet,
        bottomBarNearTopThresholdPx,
        onBottomBarScrollSnapshotChanged
    ) {
        snapshotFlow {
            val itemIndex = galleryState.firstVisibleItemIndex
            val itemOffset = galleryState.firstVisibleItemScrollOffset
            StartupBottomBarScrollSnapshot(
                source = "mine-gallery",
                absoluteOffsetPx = startupBottomBarAbsoluteOffset(itemIndex, itemOffset),
                nearTop = !galleryPreviewVisible &&
                    !showCommentSheet &&
                    itemIndex == 0 &&
                    itemOffset <= bottomBarNearTopThresholdPx,
                forceHidden = galleryPreviewVisible || showCommentSheet
            )
        }.collect { onBottomBarScrollSnapshotChanged(it) }
    }
    LaunchedEffect(scrollToTopSignal, galleryState) {
        if (scrollToTopSignal == 0) return@LaunchedEffect
        galleryState.animateScrollToItem(0)
    }

    LaunchedEffect(mineGalleryItems) {
        val validIds = mineGalleryItems.map { it.id }.toSet()
        galleryCardBoundsById.keys.toList().forEach { itemId ->
            if (itemId !in validIds) {
                galleryCardBoundsById.remove(itemId)
                galleryMediaSizeById.remove(itemId)
            }
        }
        if (galleryPreviewVisible && galleryPreviewItems.none { it.id in validIds }) {
            galleryPreviewVisible = false
            galleryPreviewItems = emptyList()
            galleryPreviewSourceBounds = null
            galleryHiddenSourceItemId = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        StartupMineScrollableLayeredGrid(
            state = galleryState,
            stats = stats,
            totalPhotos = totalPhotos,
            avatarModel = avatarModel,
            userDisplayName = userDisplayName,
            allMedia = allMedia,
            likedIds = likedIds,
            likedPhotos = mineGalleryItems,
            onGlobePageClick = onGlobePageClick,
            hiddenSourceItemId = galleryHiddenSourceItemId,
            backgroundModel = statsBackgroundModel,
            onStatsBackgroundClick = {
                statsBackgroundPicker.launch(
                    PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onSettingsClick = onSettingsClick?.let { onSettings ->
                {
                    performHaptic()
                    onSettings()
                }
            },
            onProcessedClick = onProcessedClick,
            onCampfireClick = onCampfireClick?.let { onCampfire ->
                {
                    performHaptic()
                    onCampfire()
                }
            },
            onHelpCenterClick = onHelpCenterClick?.let { onHelpCenter ->
                {
                    performHaptic()
                    onHelpCenter()
                }
            },
            onLikesClick = onLikesClick?.let { onLikes ->
                {
                    performHaptic()
                    onLikes()
                }
            },
            onAvatarClick = { showAvatarDialog = true },
            onUserNameClick = { showUserNameDialog = true },
            showCampfireBadge = isCampfireSupporter,
            onCampfireBadgeClick = { showCampfireBadgeDialog = true },
            selectedPanel = selectedDrawerPanel,
            onPanelSelected = { panel ->
                if (selectedDrawerPanel != panel) {
                    performHaptic()
                    selectedDrawerPanel = panel
                }
            },
            onItemBoundsMeasured = { itemId, bounds ->
                galleryCardBoundsById[itemId] = bounds
            },
            onItemMediaSizeMeasured = { itemId, size ->
                galleryMediaSizeById[itemId] = size
            },
            onItemClick = { item, bounds ->
                val previewItems = mineGalleryItems.toList()
                val pageIndex = previewItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                galleryPreviewItems = previewItems
                galleryPreviewStartIndex = pageIndex
                galleryPreviewSourceBounds = galleryCardBoundsById[item.id] ?: bounds
                galleryHiddenSourceItemId = null
                galleryPreviewVisible = true
            }
        )

        LikesGalleryPreviewOverlay(
            visible = galleryPreviewVisible,
            items = galleryPreviewItems,
            startIndex = galleryPreviewStartIndex,
            sourceBoundsInRoot = galleryPreviewSourceBounds,
            sourceBoundsByItemId = galleryCardBoundsById,
            mediaSizeByItemId = galleryMediaSizeById,
            danmakuEnabled = danmakuEnabled,
            commentsProvider = { mediaId ->
                if (commentsRevision >= 0) {
                    appPrefs.comments[mediaId] ?: emptyList()
                } else {
                    emptyList()
                }
            },
            onPageChanged = {},
            onSharedDismissStart = { sourceItemId ->
                galleryHiddenSourceItemId = sourceItemId
            },
            onDismissRequest = {
                galleryHiddenSourceItemId = null
                galleryPreviewVisible = false
                galleryPreviewItems = emptyList()
                galleryPreviewSourceBounds = null
            },
            chromeContent = { activeItem ->
                LikesPreviewActionButtons(
                    strings = strings,
                    commentCount = remember(commentsRevision, activeItem.id) {
                        appPrefs.comments[activeItem.id]?.size ?: 0
                    },
                    danmakuEnabled = danmakuEnabled,
                    showRemoveButton = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp)
                        .bottomNavigationBarPadding(64.dp),
                    onComment = {
                        currentCommentItem = activeItem
                        showCommentSheet = true
                    },
                    onShare = {
                        shareMedia(appContext, activeItem)
                    },
                    onToggleDanmaku = {
                        danmakuEnabled = !danmakuEnabled
                        appPrefs.danmakuEnabled = danmakuEnabled
                    },
                    onRemove = {
                        val updatedLikedIds = appPrefs.likedIds.toMutableSet()
                        updatedLikedIds.remove(activeItem.id)
                        appPrefs.likedIds = updatedLikedIds
                        localLikedPhotos = localLikedPhotos.filter { it.id != activeItem.id }
                        if (currentCommentItem?.id == activeItem.id) {
                            showCommentSheet = false
                            currentCommentItem = null
                        }
                        val remainingPreviewItems = galleryPreviewItems.filter { it.id != activeItem.id }
                        galleryPreviewItems = remainingPreviewItems
                        if (remainingPreviewItems.isEmpty()) {
                            galleryHiddenSourceItemId = null
                            galleryPreviewVisible = false
                            galleryPreviewSourceBounds = null
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(20f)
        )

        LikesCommentSheet(
            item = currentCommentItem,
            visible = showCommentSheet,
            prefs = appPrefs,
            onDismiss = {
                showCommentSheet = false
                currentCommentItem = null
            },
            onCommentsChanged = {
                commentsRevision += 1
            }
        )

        if (showAvatarDialog) {
            StartupMineAvatarDialog(
                avatarType = avatarType,
                onDismissRequest = { showAvatarDialog = false },
                onSelectBuiltIn = { type ->
                    onAvatarTypeChange?.invoke(type)
                    showAvatarDialog = false
                },
                onUploadCustom = {
                    showAvatarDialog = false
                    avatarPicker.launch(
                        PickVisualMediaRequest(
                            mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            )
        }

        if (showUserNameDialog) {
            StartupMineUserNameDialog(
                initialName = userDisplayName,
                onDismissRequest = { showUserNameDialog = false },
                onConfirm = { nextName ->
                    val normalizedName = nextName.trim().ifBlank { STARTUP_MINE_DEFAULT_USER_NAME }
                    userDisplayName = normalizedName
                    prefs.edit().putString(STARTUP_MINE_USER_DISPLAY_NAME_KEY, normalizedName).apply()
                    showUserNameDialog = false
                }
            )
        }

        if (showCampfireBadgeDialog) {
            AlertDialog(
                onDismissRequest = { showCampfireBadgeDialog = false },
                title = {
                    Text(text = "篝火计划的参与者！", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(text = "感谢有你，一路相伴~")
                },
                confirmButton = {
                    TextButton(onClick = { showCampfireBadgeDialog = false }) {
                        Text("知道啦")
                    }
                }
            )
        }
    }
}

@Composable
private fun StartupMineScrollableLayeredGrid(
    state: LazyStaggeredGridState,
    stats: AppStats,
    totalPhotos: Int,
    avatarModel: Any,
    userDisplayName: String,
    allMedia: List<MediaItem>,
    likedIds: Set<Long>,
    likedPhotos: List<MediaItem>,
    onGlobePageClick: (() -> Unit)?,
    hiddenSourceItemId: Long?,
    onSettingsClick: (() -> Unit)?,
    onProcessedClick: (() -> Unit)?,
    onCampfireClick: (() -> Unit)?,
    onHelpCenterClick: (() -> Unit)?,
    onLikesClick: (() -> Unit)?,
    onAvatarClick: (() -> Unit)?,
    onUserNameClick: (() -> Unit)?,
    showCampfireBadge: Boolean,
    onCampfireBadgeClick: (() -> Unit)?,
    selectedPanel: StartupMineDrawerPanel,
    onPanelSelected: (StartupMineDrawerPanel) -> Unit,
    backgroundModel: Any,
    onStatsBackgroundClick: () -> Unit,
    onItemBoundsMeasured: (Long, Rect) -> Unit,
    onItemMediaSizeMeasured: (Long, IntSize) -> Unit,
    onItemClick: (MediaItem, Rect) -> Unit
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val likesHorizontalSpacing = 8.dp
    // Same column count as the Likes page; the header row offers the picker.
    val likesColumns = rememberLikesGalleryColumns()
    val likesGridSpacing = likesGallerySpacing(likesColumns)
    val likesCornerRadius = likesGalleryCornerRadius(likesColumns)

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(likesColumns),
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (selectedPanel == StartupMineDrawerPanel.Likes) Modifier.likesGalleryPinchToResize() else Modifier
            ),
        horizontalArrangement = Arrangement.spacedBy(likesGridSpacing),
        verticalItemSpacing = likesGridSpacing,
        contentPadding = PaddingValues(
            start = likesHorizontalSpacing,
            end = likesHorizontalSpacing,
            top = 0.dp,
            bottom = 118.dp
        )
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(336.dp)
            ) {
                val fullBleedWidth = maxWidth + likesHorizontalSpacing * 2
                Box(
                    modifier = Modifier
                        .requiredWidth(fullBleedWidth)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                ) {
                    StartupMineHeroBackdrop(
                        backgroundModel = backgroundModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    StartupMineBackgroundChangeTapZone(
                        onClick = onStatsBackgroundClick,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    StartupMineLikesDrawerHeader(
                        likesLabel = strings.likes,
                        insightsLabel = strings.shootingStatsTitle,
                        selectedPanel = selectedPanel,
                        onPanelSelected = onPanelSelected,
                        onLikesClick = onLikesClick,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 58.dp)
                ) {
                    StartupMinePageHeader(
                        onSettingsClick = onSettingsClick,
                        modifier = Modifier.zIndex(2f)
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    StartupMineProfileSummary(
                        avatarModel = avatarModel,
                        userDisplayName = userDisplayName,
                        usageText = rememberStartupMineUsageLabel(LocalContext.current, language),
                        showCampfireBadge = showCampfireBadge,
                        onCampfireBadgeClick = onCampfireBadgeClick,
                        onTextBlockClick = onUserNameClick,
                        onAvatarClick = onAvatarClick,
                        modifier = Modifier
                            .zIndex(0f)
                            .offset(y = (-4).dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    StartupMineStatsStrip(
                        stats = stats,
                        totalPhotos = totalPhotos,
                        likedCount = likedPhotos.size,
                        onProcessedClick = onProcessedClick,
                        modifier = Modifier.zIndex(0f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    StartupMineQuickActionBar(
                        onCampfireClick = onCampfireClick,
                        onHelpCenterClick = onHelpCenterClick
                    )
                }
            }
        }

        if (selectedPanel == StartupMineDrawerPanel.Likes && likedPhotos.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mineLikedEmptyLabel(language),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else if (selectedPanel == StartupMineDrawerPanel.Likes) {
            items(likedPhotos, key = { it.id }) { item ->
                LikesGalleryItemCard(
                    item = item,
                    hidden = hiddenSourceItemId == item.id,
                    cornerRadius = likesCornerRadius,
                    onItemBoundsMeasured = onItemBoundsMeasured,
                    onItemMediaSizeMeasured = onItemMediaSizeMeasured,
                    onClick = onItemClick,
                    modifier = Modifier.animateItem()
                )
            }
        } else {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = 24.dp)
                ) {
                    ShootingStatsView(
                        allMedia = allMedia,
                        likedIds = likedIds,
                        useInternalScroll = false,
                        onOpenGlobe = { onGlobePageClick?.invoke() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StartupMineHeroBackdrop(
    backgroundModel: Any,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundRequestKey = remember(backgroundModel) {
        when (backgroundModel) {
            is File -> backgroundModel.absolutePath
            is Uri -> backgroundModel.toString()
            else -> backgroundModel.toString()
        }
    }
    val backgroundRequest = remember(context, backgroundModel, backgroundRequestKey) {
        ImageRequest.Builder(context)
            .data(backgroundModel)
            .crossfade(false)
            .memoryCacheKey("startup_mine_background_$backgroundRequestKey")
            .diskCacheKey("startup_mine_background_$backgroundRequestKey")
            .build()
    }
    Box(modifier = modifier) {
        key(backgroundRequestKey) {
            AsyncImage(
                model = backgroundRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.16f))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.08f),
                            0.58f to Color.Black.copy(alpha = 0.12f),
                            1.00f to Color.Black.copy(alpha = 0.34f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@Composable
private fun StartupMineBackgroundChangeTapZone(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 54.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}

@Composable
private fun StartupMineLikesDrawerHeader(
    likesLabel: String,
    insightsLabel: String,
    selectedPanel: StartupMineDrawerPanel,
    onPanelSelected: (StartupMineDrawerPanel) -> Unit,
    onLikesClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 6.dp, bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StartupMineDrawerTabs(
                likesLabel = likesLabel,
                insightsLabel = insightsLabel,
                selectedPanel = selectedPanel,
                onPanelSelected = onPanelSelected,
                modifier = Modifier.weight(1f)
            )
            if (selectedPanel == StartupMineDrawerPanel.Likes) {
                LikesGalleryColumnsButton(isDark = false, plain = true)
            }
            if (onLikesClick != null && selectedPanel == StartupMineDrawerPanel.Likes) {
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLikesClick
                        )
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = StartupMineLikesLegacyPageIcon,
                        contentDescription = null,
                        tint = Color(0xFFBFBFBF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StartupMineDrawerTabs(
    likesLabel: String,
    insightsLabel: String,
    selectedPanel: StartupMineDrawerPanel,
    onPanelSelected: (StartupMineDrawerPanel) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabScrollState = rememberScrollState()
    Row(
        modifier = modifier.horizontalScroll(tabScrollState),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album insights moved to its own bottom tab; the Mine drawer only keeps Likes.
        listOf(StartupMineDrawerPanel.Likes).forEach { panel ->
            val selected = panel == selectedPanel
            val label = if (panel == StartupMineDrawerPanel.Likes) likesLabel else insightsLabel
            val baseFontSize = startupMineDrawerTabFontSize(label)
            val baseLineHeight = (baseFontSize.value + 4f).sp
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.84f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "mine_drawer_tab_scale_${panel.name}"
            )
            val color by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                },
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "mine_drawer_tab_color_${panel.name}"
            )
            Text(
                text = label,
                color = color,
                fontSize = baseFontSize,
                lineHeight = baseLineHeight,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = remember(panel) { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPanelSelected(panel) }
                    )
                    .padding(vertical = 4.dp)
            )
        }
    }
}

private fun startupMineDrawerTabFontSize(label: String) = when {
    label.length >= 16 -> 18.sp
    label.length >= 11 -> 20.sp
    else -> 22.sp
}

private val StartupMineLikesLegacyPageIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "StartupMineLikesLegacyPageIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        outlinedPath("M7 15H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v3")
        outlinedPath("M10.25 19H8a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v1.25")
        outlinedPath("M18 22l3.35-3.284a2.143 2.143 0 0 0 .005-3.071a2.24 2.24 0 0 0-3.129-.006l-.224.22l-.223-.22a2.24 2.24 0 0 0-3.128-.006a2.143 2.143 0 0 0-.006 3.071z")
    }.build()
}

private fun ImageVector.Builder.outlinedPath(pathData: String) {
    addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    )
}

@Composable
private fun StartupMinePageHeader(
    onSettingsClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (onSettingsClick != null) {
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .bounceClick(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StartupMineQuickActionBar(
    onCampfireClick: (() -> Unit)?,
    onHelpCenterClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val newPageStrings = getNewPageStrings(language)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StartupMineQuickCard(
            label = newPageStrings.campfireTitle,
            icon = Icons.Outlined.LocalFireDepartment,
            onClick = onCampfireClick,
            modifier = Modifier.weight(1f)
        )
        StartupMineQuickCard(
            label = getHelpCenterActionTitle(language),
            icon = Icons.Outlined.HelpOutline,
            onClick = onHelpCenterClick,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun StartupMineQuickCard(
    label: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.26f))
                .blur(20.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (onClick != null) 0.96f else 0.42f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = if (onClick != null) 0.98f else 0.44f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StartupMineAvatarDialog(
    avatarType: String,
    onDismissRequest: () -> Unit,
    onSelectBuiltIn: (String) -> Unit,
    onUploadCustom: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = strings.avatar,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AvatarTypeSelectorRow(
                    avatarType = avatarType,
                    onAvatarTypeChange = onSelectBuiltIn
                )
                OutlinedButton(
                    onClick = onUploadCustom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload custom avatar")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun StartupMineUserNameDialog(
    initialName: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editingName by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "\u8BBE\u7F6E\u7528\u6237ID", fontWeight = FontWeight.Bold)
        },
        text = {
            TextField(
                value = editingName,
                onValueChange = { next ->
                    editingName = if (next.length <= 24) next else next.take(24)
                },
                singleLine = true,
                placeholder = { Text(STARTUP_MINE_DEFAULT_USER_NAME) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(editingName) }) {
                Text("\u4FDD\u5B58")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("\u53D6\u6D88")
            }
        }
    )
}

@Composable
private fun StartupMineProfileSummary(
    avatarModel: Any,
    userDisplayName: String,
    usageText: String,
    showCampfireBadge: Boolean,
    onCampfireBadgeClick: (() -> Unit)?,
    onTextBlockClick: (() -> Unit)?,
    onAvatarClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .size(66.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f), CircleShape)
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.92f),
                    shape = CircleShape
                )
                .then(if (onAvatarClick != null) Modifier.bounceClick(onClick = onAvatarClick) else Modifier)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = avatarModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = if (onTextBlockClick != null) {
                Modifier.bounceClick(onClick = onTextBlockClick)
            } else {
                Modifier
            },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = userDisplayName,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.96f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showCampfireBadge) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .then(
                                if (onCampfireBadgeClick != null) {
                                    Modifier.bounceClick(onClick = onCampfireBadgeClick)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = STARTUP_MINE_CAMPFIRE_BADGE_ASSET,
                            contentDescription = "Campfire Supporter",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = usageText,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StartupMineStatsStrip(
    stats: AppStats,
    totalPhotos: Int,
    likedCount: Int,
    onProcessedClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val savedSpace = formatCompactBytes(stats.savedSize)
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StartupMineStatCell(
            value = stats.processed.toString(),
            label = strings.totalProcessed,
            onClick = onProcessedClick,
            showChevron = onProcessedClick != null,
            modifier = Modifier.weight(1f)
        )
        StartupMineStatCell(
            value = stats.trashed.toString(),
            label = strings.deleted,
            accent = Color(0xFFFF6B6B),
            modifier = Modifier.weight(1f)
        )
        StartupMineStatCell(
            value = likedCount.toString(),
            label = strings.likes,
            accent = Color(0xFFFFB347),
            modifier = Modifier.weight(1f)
        )
        StartupMineStatCell(
            value = savedSpace,
            label = mineSavedLabel(language),
            accent = Color(0xFF8CC8FF),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StartupMineStatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.96f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showChevron) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

private enum class StartupMineDrawerPanel {
    Likes,
    Insights
}

private enum class MineQuickActionType {
    Insights,
    Campfire,
    HelpCenter
}

private fun mineQuickActionSubtitle(language: AppLanguage, type: MineQuickActionType): String = when (type) {
    MineQuickActionType.Insights -> when (language) {
        AppLanguage.SimplifiedChinese -> "鏌ョ湅鎷嶆憚娲炲療"
        AppLanguage.TraditionalChinese -> "鏌ョ湅鎷嶆敐娲炲療"
        else -> "View shooting insights"
    }
    MineQuickActionType.Campfire -> when (language) {
        AppLanguage.SimplifiedChinese -> "杩涘叆绡濈伀璁″垝"
        AppLanguage.TraditionalChinese -> "閫插叆绡濈伀瑷堢暙"
        else -> "Open Campfire Plan"
    }
    MineQuickActionType.HelpCenter -> when (language) {
        AppLanguage.SimplifiedChinese -> "鏌ョ湅甯姪涓績"
        AppLanguage.TraditionalChinese -> "鏌ョ湅骞姪涓績"
        else -> "Open help center"
    }
}

private const val STARTUP_MINE_PROFILE_PREFS_NAME = "startup_mine_profile_prefs"
private const val STARTUP_MINE_STATS_BACKGROUND_URI_KEY = "stats_background_uri"
private const val STARTUP_MINE_STATS_BACKGROUND_PATH_KEY = "stats_background_path"
private const val STARTUP_MINE_USER_DISPLAY_NAME_KEY = "user_display_name"
private const val STARTUP_MINE_DEFAULT_USER_NAME = "\u795E\u79D8\u7528\u6237"
private const val STARTUP_MINE_STATS_BACKGROUND_DIR = "startup_mine_background"
private const val STARTUP_MINE_STATS_BACKGROUND_FILE_PREFIX = "stats_background"
private const val STARTUP_MINE_CAMPFIRE_BADGE_ASSET =
    "file:///android_asset/campfire_supporters/supporter_default.webp"

private fun mineTopLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u9876\u90e8"
    AppLanguage.TraditionalChinese -> "\u9802\u90e8"
    else -> "Top"
}

private fun mineLikedEmptyLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u559c\u6b22\u7684\u7167\u7247\u4f1a\u51fa\u73b0\u5728\u8fd9\u91cc"
    AppLanguage.TraditionalChinese -> "\u559c\u6b61\u7684\u7167\u7247\u6703\u51fa\u73fe\u5728\u9019\u88e1"
    AppLanguage.Japanese -> "\u559c\u3070\u308c\u306e\u5199\u771f\u304c\u3053\u3053\u306b\u8868\u793a\u3055\u308c\u307e\u3059"
    AppLanguage.Korean -> "\uc990\uae30\uc6c3\uc740 \uc0ac\uc9c4\uc774 \uc5ec\uae30\uc5d0 \ud45c\uc2dc\ub429\ub2c8\ub2e4"
    AppLanguage.French -> "Les photos aimées apparaîtront ici"
    AppLanguage.German -> "Gemochte Fotos werden hier angezeigt"
    AppLanguage.Italian -> "Le foto che ti piacciono appariranno qui"
    AppLanguage.Spanish -> "Las fotos que te gusten aparecerán aquí"
    AppLanguage.Portuguese -> "As fotos curtidas aparecerão aqui"
    AppLanguage.Russian -> "\u041f\u043e\u043d\u0440\u0430\u0432\u0438\u0432\u0448\u0438\u0435\u0441\u044f \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438 \u043f\u043e\u044f\u0432\u044f\u0442\u0441\u044f \u0437\u0434\u0435\u0441\u044c"
    AppLanguage.Thai -> "\u0e23\u0e39\u0e49\u0e17\u0e35\u0e48\u0e04\u0e1a\u0e1a\u0e31\u0e0a\u0e19\u0e30\u0e08\u0e30\u0e1b\u0e49\u0e2d\u0e22\u0e19\u0e35\u0e49\u0e17\u0e35\u0e48\u0e19\u0e35\u0e48"
    AppLanguage.Vietnamese -> "\u1ea2nh \u0111\u00e3 th\u00edch s\u1ebd xu\u1ea5t hi\u1ec7n \u1edf \u0111\u00e2y"
    AppLanguage.Indonesian -> "Foto yang Anda sukai akan muncul di sini"
    AppLanguage.Arabic -> "\u0633\u062a\u0638\u0647\u0631 \u0627\u0644\u0635\u0648\u0631 \u0627\u0644\u062a\u064a \u062d\u0628\u0628\u062a\u0647\u0627 \u0647\u0646\u0627"
    AppLanguage.Ukrainian -> "Вподобані фото з'являться тут"
    AppLanguage.Polish -> "Polubione zdjęcia pojawią się tutaj"
    AppLanguage.Czech -> "Oblíbené fotky se zobrazí tady"
    AppLanguage.Dutch -> "Foto's die je leuk vindt verschijnen hier"
    AppLanguage.Swedish -> "Gillade bilder visas här"
    AppLanguage.Danish -> "Favoritbilleder vises her"
    AppLanguage.Norwegian -> "Likte bilder vises her"
    AppLanguage.Finnish -> "Tykätyt kuvat näkyvät tässä"
    AppLanguage.Greek -> "Οι αγαπημένες φωτογραφίες θα εμφανίζονται εδώ"
    AppLanguage.Romanian -> "Pozele apreciate vor apărea aici"
    AppLanguage.Hungarian -> "A kedvelt fotók itt jelennek meg"
    AppLanguage.Turkish -> "Beğenilen fotoğraflar burada görünecek"
    AppLanguage.Malay -> "Foto yang disukai akan muncul di sini"
    AppLanguage.Hebrew -> "תמונות אהובות יופיעו כאן"
    AppLanguage.English -> "Liked photos will appear here"
}

private fun mineSavedLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u8282\u7701"
    AppLanguage.TraditionalChinese -> "\u7bc0\u7701"
    AppLanguage.Japanese -> "\u4fdd\u5b58"
    AppLanguage.Korean -> "\uc808\uc57d"
    AppLanguage.French -> "\u00c9conomis\u00e9"
    AppLanguage.German -> "Gespart"
    AppLanguage.Italian -> "Risparmiato"
    AppLanguage.Spanish -> "Ahorrado"
    AppLanguage.Portuguese -> "Economizado"
    AppLanguage.Russian -> "\u0421\u044d\u043a\u043e\u043d\u043e\u043c\u043b\u0435\u043d\u043e"
    AppLanguage.Thai -> "\u0e1b\u0e23\u0e30\u0e2b\u0e22\u0e31\u0e14"
    AppLanguage.Vietnamese -> "\u0110\u00e3 ti\u1ebft ki\u1ec7m"
    AppLanguage.Indonesian -> "Dihemat"
    AppLanguage.Arabic -> "\u062a\u0645 \u062a\u0648\u0641\u064a\u0631\u0647"
    AppLanguage.Ukrainian -> "Звільнено"
    AppLanguage.Polish -> "Zaoszczędzone"
    AppLanguage.Czech -> "Ušetřeno"
    AppLanguage.Dutch -> "Bespaard"
    AppLanguage.Swedish -> "Frigjort"
    AppLanguage.Danish -> "Sparet"
    AppLanguage.Norwegian -> "Spart"
    AppLanguage.Finnish -> "Säästetty"
    AppLanguage.Greek -> "Αποθηκεύτηκαν"
    AppLanguage.Romanian -> "Economisit"
    AppLanguage.Hungarian -> "Megtakarítva"
    AppLanguage.Turkish -> "Kaydedilen"
    AppLanguage.Malay -> "Dijimatkan"
    AppLanguage.Hebrew -> "נחסך"
    AppLanguage.English -> "Saved"
}

private fun formatCompactBytes(bytes: Long): String {
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    if (gb >= 1.0) return "${formatTruncatedDecimal(gb, 1)}G"
    val mb = bytes / 1024.0 / 1024.0
    return "${formatTruncatedDecimal(mb, 1)}M"
}

@Composable
private fun rememberStartupMineUsageLabel(appContext: Context, language: AppLanguage): String {
    val installTimeMs = remember(appContext) {
        runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).firstInstallTime
        }.getOrDefault(System.currentTimeMillis())
    }
    val daysUsed = remember(installTimeMs) {
        (((System.currentTimeMillis() - installTimeMs).coerceAtLeast(0L)) / 86_400_000L)
            .toInt()
            .coerceAtLeast(1)
    }
    return when (language) {
        AppLanguage.SimplifiedChinese -> "\u60A8\u5DF2\u4F7F\u7528 photoo $daysUsed \u5929"
        AppLanguage.TraditionalChinese -> "\u60a8\u5df2\u4f7f\u7528 photoo $daysUsed \u5929"
        AppLanguage.Japanese -> "photoo \u306e\u5229\u7528\u65e5\u6570: $daysUsed \u65e5"
        AppLanguage.Korean -> "photoo \uc0ac\uc6a9 $daysUsed\uc77c\ucc28"
        AppLanguage.French -> "Vous utilisez photoo depuis $daysUsed jours"
        AppLanguage.German -> "Sie nutzen photoo seit $daysUsed Tagen"
        AppLanguage.Italian -> "Usi photoo da $daysUsed giorni"
        AppLanguage.Spanish -> "Llevas $daysUsed d\u00edas usando photoo"
        AppLanguage.Portuguese -> "Voc\u00ea usa o photoo h\u00e1 $daysUsed dias"
        AppLanguage.Russian -> "\u0412\u044b \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0435 photoo \u0443\u0436\u0435 $daysUsed \u0434\u043d\u0435\u0439"
        AppLanguage.Thai -> "\u0e04\u0e38\u0e13\u0e43\u0e0a\u0e49 photoo \u0e21\u0e32\u0e41\u0e25\u0e49\u0e27 $daysUsed \u0e27\u0e31\u0e19"
        AppLanguage.Vietnamese -> "B\u1ea1n \u0111\u00e3 d\u00f9ng photoo trong $daysUsed ng\u00e0y"
        AppLanguage.Indonesian -> "Anda sudah memakai photoo selama $daysUsed hari"
        AppLanguage.Arabic -> "\u0644\u0642\u062f \u0627\u0633\u062a\u062e\u062f\u0645\u062a photoo \u0644\u0645\u062f\u0629 $daysUsed \u064a\u0648\u0645\u064b\u0627"
        AppLanguage.Ukrainian -> "Днів разом із photoo: $daysUsed"
        AppLanguage.Polish -> "Korzystasz z photoo od $daysUsed dni"
        AppLanguage.Czech -> "photoo používáte už $daysUsed. den"
        AppLanguage.Dutch -> "Je gebruikt photoo al $daysUsed dagen"
        AppLanguage.Swedish -> "Du har använt photoo i $daysUsed dagar"
        AppLanguage.Danish -> "Du har brugt photoo i $daysUsed dage"
        AppLanguage.Norwegian -> "Du har brukt photoo i $daysUsed dager"
        AppLanguage.Finnish -> "photoo on ollut käytössäsi $daysUsed päivää"
        AppLanguage.Greek -> "Χρησιμοποιείτε το photoo εδώ και $daysUsed ημέρες"
        AppLanguage.Romanian -> "Folosiți Photoo de $daysUsed zile"
        AppLanguage.Hungarian -> "$daysUsed napja használja a Photoót"
        AppLanguage.Turkish -> "photoo'yu $daysUsed gündür kullanıyorsun"
        AppLanguage.Malay -> "Anda telah menggunakan photoo selama $daysUsed hari"
        AppLanguage.Hebrew -> "השתמשת ב-Photoo כבר $daysUsed ימים"
        AppLanguage.English -> "You've used photoo for $daysUsed days"
    }
}
private fun formatTruncatedDecimal(value: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val truncated = floor(value * factor) / factor
    return if (decimals == 0) {
        truncated.toLong().toString()
    } else {
        String.format("%.${decimals}f", truncated)
    }
}

private suspend fun persistStartupMineBackgroundImage(
    context: Context,
    sourceUri: Uri
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(sourceUri)?.lowercase(Locale.ROOT).orEmpty()
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
        val targetDir = File(context.filesDir, STARTUP_MINE_STATS_BACKGROUND_DIR).apply { mkdirs() }
        val tempFile = File(targetDir, "$STARTUP_MINE_STATS_BACKGROUND_FILE_PREFIX.tmp")
        val targetFile = File(
            targetDir,
            "${STARTUP_MINE_STATS_BACKGROUND_FILE_PREFIX}_${System.currentTimeMillis()}.$extension"
        )
        resolver.openInputStream(sourceUri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            runCatching { tempFile.delete() }
        }
        targetDir.listFiles()?.forEach { file ->
            if (
                file != targetFile &&
                file.name.startsWith(STARTUP_MINE_STATS_BACKGROUND_FILE_PREFIX)
            ) {
                runCatching { file.delete() }
            }
        }
        targetFile.absolutePath
    }.getOrNull()
}
