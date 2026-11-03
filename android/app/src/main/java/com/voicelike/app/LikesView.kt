package com.voicelike.app

import android.location.Geocoder
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowDropDown
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun LikesView(
    likedItems: List<MediaItem>,
    onBack: () -> Unit,
    onShare: (MediaItem) -> Unit,
    prefs: PreferencesManager,
    allMedia: List<MediaItem>,
    folderUsage: Map<String, Int>,
    userCreatedFolders: Set<String>,
    onLikeRemoved: (MediaItem) -> Unit = {}
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val topInset = 48.dp
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Danmaku State
    var danmakuEnabled by remember { mutableStateOf(prefs.danmakuEnabled) }

    // Selection State (null = Likes, String = Folder Name)
    var selectedBucketName by remember { mutableStateOf<String?>(null) }
    var showDropdown by remember { mutableStateOf(false) }

    // Liked Items State (Hoisted for Remove Dialog visibility)
    // Renamed concept to "displayQueue" but keeping variable name to minimize diff
    val likedQueueState = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val lastAppendSize = remember { mutableStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { kotlin.math.max(1, likedQueueState.value.size) })
    val folderCache = remember { mutableStateMapOf<String, List<MediaItem>>() }
    val livePlaybackEndedById = remember { mutableStateMapOf<Long, Boolean>() }
    val liveReplayTokenById = remember { mutableStateMapOf<Long, Long>() }
    var videoControlsHeightDp by remember { mutableStateOf(0.dp) }
    var isFolderLoading by remember { mutableStateOf(false) }
    var displayMode by remember { mutableStateOf(likesDisplayModeFromPref(prefs.likesDisplayMode)) }

    // Computed Folders
    // Use local state for sorting counts to trigger recomposition
    var likesFolderCounts by remember { mutableStateOf(prefs.likesFolderSelectionCounts) }
    
    val folders = remember(allMedia, likesFolderCounts, userCreatedFolders) {
        val groups = allMedia.groupBy { it.bucketName }
        groups.keys.filter { !it.isBlank() }.sortedWith(
            compareByDescending<String> { name ->
                // Sort by selection count in Likes view
                likesFolderCounts[name] ?: 0
            }
        )
    }

    // Current Source Items for Infinite Scroll
    var currentSourceItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var localLikedItems by remember { mutableStateOf(likedItems) }

    LaunchedEffect(likedItems) {
        localLikedItems = likedItems
    }

    LaunchedEffect(allMedia) {
        folderCache.clear()
    }

    LaunchedEffect(localLikedItems, selectedBucketName, allMedia) {
        val bucket = selectedBucketName
        if (bucket == null) {
            isFolderLoading = false
            currentSourceItems = localLikedItems
            val validLikedIds = localLikedItems.map { it.id }.toSet()
            livePlaybackEndedById.keys.toList().forEach { id ->
                if (!validLikedIds.contains(id)) {
                    livePlaybackEndedById.remove(id)
                    liveReplayTokenById.remove(id)
                }
            }
            likedQueueState.value = if (localLikedItems.isNotEmpty()) {
                withContext(Dispatchers.Default) { localLikedItems.shuffled() }
            } else {
                emptyList()
            }
        } else {
            val cached = folderCache[bucket]
            if (cached != null) {
                isFolderLoading = false
                currentSourceItems = cached
                likedQueueState.value = cached
            } else {
                isFolderLoading = true
                val source = withContext(Dispatchers.IO) {
                    allMedia.filter {
                        it.bucketName == bucket &&
                        File(it.path ?: "").exists()
                    }
                }
                val sorted = withContext(Dispatchers.Default) {
                    source.sortedByDescending { it.dateAdded }
                }
                folderCache[bucket] = sorted
                currentSourceItems = sorted
                val validFolderIds = sorted.map { it.id }.toSet()
                livePlaybackEndedById.keys.toList().forEach { id ->
                    if (!validFolderIds.contains(id)) {
                        livePlaybackEndedById.remove(id)
                        liveReplayTokenById.remove(id)
                    }
                }
                likedQueueState.value = sorted
                isFolderLoading = false
            }
        }
        
        lastAppendSize.value = likedQueueState.value.size
        if (likedQueueState.value.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }
    
    // Comment State
    var showCommentSheet by remember { mutableStateOf(false) }
    var currentCommentItem by remember { mutableStateOf<MediaItem?>(null) }
    var commentsRevision by remember { mutableIntStateOf(0) }

    // Remove State
    var showRemoveDialog by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<MediaItem?>(null) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var galleryPreviewVisible by remember { mutableStateOf(false) }
    var galleryPreviewItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var galleryPreviewStartIndex by remember { mutableIntStateOf(0) }
    var galleryPreviewCurrentIndex by remember { mutableIntStateOf(0) }
    var galleryPreviewDismissRequestToken by remember { mutableLongStateOf(0L) }
    var galleryPreviewSharedDismissEnabled by remember { mutableStateOf(true) }
    var galleryPreviewSourceBounds by remember { mutableStateOf<Rect?>(null) }
    val galleryCardBoundsById = remember { mutableStateMapOf<Long, Rect>() }
    val galleryMediaSizeById = remember { mutableStateMapOf<Long, IntSize>() }
    var galleryHiddenSourceItemId by remember { mutableStateOf<Long?>(null) }
    var pendingGalleryRemovalItem by remember { mutableStateOf<MediaItem?>(null) }
    val galleryItems = remember(localLikedItems) {
        localLikedItems
            .filter { it.type == "photo" }
            .distinctBy { it.id }
            .sortedByDescending { it.dateAdded }
    }
    val requestGalleryPreviewDismiss: () -> Unit = {
        if (galleryPreviewVisible) {
            galleryPreviewDismissRequestToken += 1L
        }
    }
    fun removeLikedItem(item: MediaItem) {
        localLikedItems = localLikedItems.filter { it.id != item.id }
        likedQueueState.value = likedQueueState.value.filter { it.id != item.id }
        galleryCardBoundsById.remove(item.id)
        galleryMediaSizeById.remove(item.id)

        val liked = prefs.likedIds.toMutableSet()
        liked.remove(item.id)
        prefs.likedIds = liked
        onLikeRemoved(item)
    }
    val onGalleryPreviewDismissed: () -> Unit = {
        galleryHiddenSourceItemId = null
        galleryPreviewVisible = false
        galleryPreviewItems = emptyList()
        galleryPreviewSourceBounds = null
        galleryPreviewDismissRequestToken = 0L
        galleryPreviewSharedDismissEnabled = true
        pendingGalleryRemovalItem?.let { removeLikedItem(it) }
        pendingGalleryRemovalItem = null
    }

    BackHandler(enabled = galleryPreviewVisible) {
        requestGalleryPreviewDismiss()
    }

    LikesCommentSheet(
        item = currentCommentItem,
        visible = showCommentSheet,
        prefs = prefs,
        onDismiss = {
            showCommentSheet = false
            currentCommentItem = null
        },
        onCommentsChanged = {
            commentsRevision += 1
        }
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (displayMode == LikesDisplayMode.TikTok) Color.Black else MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { /* Absorb clicks */ }
    ) {
        if (displayMode == LikesDisplayMode.TikTok && isFolderLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                }
            }
        } else if ((displayMode == LikesDisplayMode.TikTok && likedQueueState.value.isEmpty()) ||
            (displayMode == LikesDisplayMode.Gallery && galleryItems.isEmpty())
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (displayMode == LikesDisplayMode.Gallery || selectedBucketName == null) strings.noLikes else "No items in folder",
                    color = if (displayMode == LikesDisplayMode.TikTok) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
        } else {
            if (displayMode == LikesDisplayMode.TikTok) {
                LaunchedEffect(likedQueueState.value.size) {
                    val size = likedQueueState.value.size
                    if (size > 0 && pagerState.currentPage >= size) {
                        pagerState.scrollToPage(size - 1)
                    }
                }
                LaunchedEffect(pagerState.currentPage, likedQueueState.value.size, currentSourceItems.size) {
                    if (currentSourceItems.isNotEmpty() &&
                        pagerState.currentPage >= likedQueueState.value.size - 2 &&
                        lastAppendSize.value == likedQueueState.value.size
                    ) {
                        val extra = withContext(Dispatchers.Default) { currentSourceItems.shuffled() }
                        likedQueueState.value = likedQueueState.value + extra
                        lastAppendSize.value = likedQueueState.value.size
                    }
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { index ->
                        val itemId = likedQueueState.value.getOrNull(index)?.id
                        if (itemId != null) "${itemId}_$index" else "empty_$index"
                    }
                ) { page ->
                    val item = likedQueueState.value.getOrNull(page) ?: return@VerticalPager
                    val isActive = page == pagerState.currentPage
                    var currentItem by remember { mutableStateOf(item) }
                    var exifLoaded by remember { mutableStateOf(false) }
                    val hasLiveEnded = livePlaybackEndedById[currentItem.id] == true
                    val videoFrameRequest = rememberVideoFrameImageRequest(currentItem.uri, crossfade = false)
                    val stillImageRequest = rememberStillImageRequest(currentItem.uri, crossfade = false)
                    val livePlaybackItem = remember(currentItem.id, currentItem.uri, currentItem.livePhotoVideoUri, currentItem.isMotionPhoto) {
                        if (currentItem.isMotionPhoto) {
                            currentItem
                        } else {
                            currentItem.copy(uri = currentItem.livePhotoVideoUri ?: currentItem.uri, mimeType = "video/mp4")
                        }
                    }

                    LaunchedEffect(item.id) {
                        if (!exifLoaded && item.type == "photo") {
                            withContext(Dispatchers.IO) {
                                try {
                                    val previewInsights = MediaAnalysisManager.resolveCardPreviewInsights(
                                        context = context,
                                        item = item,
                                        timeBudgetMs = 900L
                                    )
                                    currentItem = currentItem.copy(
                                        location = previewInsights.location,
                                        isMotionPhoto = previewInsights.motionInfo?.isMotionPhoto == true,
                                        motionPhotoVideoOffset = previewInsights.motionInfo?.videoOffset ?: 0L,
                                        motionPhotoVideoLength = previewInsights.motionInfo?.videoLength ?: 0L
                                    )
                                    exifLoaded = true
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    }

                    Box(Modifier.fillMaxSize()) {
                        if (currentItem.type == "video") {
                            if (isActive) {
                                FullScreenVideoPlayer(
                                    item = currentItem,
                                    isMuted = false,
                                    onMuteChange = {},
                                    onDismiss = {},
                                    dismissEnabled = false,
                                    enableOpenCloseTransition = false,
                                    showCloseButton = false,
                                    showTopBar = false,
                                    showMuteButton = false,
                                    showCenterPlayPause = false,
                                    tapTogglesPlayPause = true,
                                    controlsHorizontalPadding = 16.dp,
                                    controlsBottomPadding = 24.dp,
                                    controlsRightInset = 92.dp,
                                    onControlsHeightChanged = { measured ->
                                        if (measured > 0.dp) {
                                            videoControlsHeightDp = measured
                                        }
                                    }
                                )
                            } else {
                                AsyncImage(
                                    model = videoFrameRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                        } else if ((currentItem.isLivePhoto && currentItem.livePhotoVideoUri != null) || currentItem.isMotionPhoto) {
                            if (isActive && !hasLiveEnded) {
                                key(currentItem.id, liveReplayTokenById[currentItem.id] ?: 0L) {
                                    FullScreenVideoPlayer(
                                        item = livePlaybackItem,
                                        isMuted = false,
                                        onMuteChange = {},
                                        onDismiss = {},
                                        dismissEnabled = false,
                                        enableOpenCloseTransition = false,
                                        showCloseButton = false,
                                        showTopBar = false,
                                        showMuteButton = false,
                                        showSpeedControl = false,
                                        showCenterPlayPause = false,
                                        controlsEnabled = false,
                                        onPlaybackEnded = {
                                            livePlaybackEndedById[currentItem.id] = true
                                        }
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = stillImageRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 16.dp, top = topInset + 48.dp)
                                    .background(
                                        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable(enabled = isActive && hasLiveEnded) {
                                        livePlaybackEndedById[currentItem.id] = false
                                        liveReplayTokenById[currentItem.id] =
                                            (liveReplayTokenById[currentItem.id] ?: 0L) + 1L
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            }
                        } else {
                            AsyncImage(
                                model = stillImageRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Danmaku Overlay (Inside Pager Item)
                        if (danmakuEnabled) {
                            val comments = remember(commentsRevision, item.id) {
                                prefs.comments[item.id] ?: emptyList()
                            }
                            DanmakuOverlay(comments = comments)
                        }

                        // Bottom Info (Location/Date)
                        val hasActiveVideoTimeline = isActive && currentItem.type == "video"
                        val videoInfoBottomPadding = if (hasActiveVideoTimeline) {
                            if (videoControlsHeightDp > 0.dp) videoControlsHeightDp + 16.dp else 112.dp
                        } else {
                            16.dp
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = videoInfoBottomPadding)
                        ) {
                            if (currentItem.location != "Unknown Location") {
                                Text(
                                    text = currentItem.location,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(currentItem.dateAdded * 1000))
                            Text(
                                text = dateStr,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                LikesGalleryContent(
                    items = galleryItems,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = topInset + 52.dp, bottom = 16.dp),
                    hiddenSourceItemId = galleryHiddenSourceItemId,
                    onItemBoundsMeasured = { itemId, bounds ->
                        galleryCardBoundsById[itemId] = bounds
                    },
                    onItemMediaSizeMeasured = { itemId, size ->
                        galleryMediaSizeById[itemId] = size
                    },
                    onItemClick = { item, bounds ->
                        val previewItems = galleryItems.toList()
                        val pageIndex = previewItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                        galleryPreviewItems = previewItems
                        galleryPreviewStartIndex = pageIndex
                        galleryPreviewCurrentIndex = pageIndex
                        galleryPreviewDismissRequestToken = 0L
                        galleryPreviewSourceBounds = galleryCardBoundsById[item.id] ?: bounds
                        galleryPreviewVisible = true
                    }
                )
            }
            
            // Top Dropdown Bar
            if (displayMode == LikesDisplayMode.TikTok && !galleryPreviewVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topInset)
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                 // Use a Material3 styled Surface for better elevation and shape
                 Surface(
                     shape = RoundedCornerShape(24.dp),
                     color = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f),
                     contentColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                     shadowElevation = 4.dp,
                     onClick = { showDropdown = true },
                     modifier = Modifier
                         .height(40.dp)
                         .advancedTextureCard(shape = RoundedCornerShape(24.dp))
                         .bounceClick()
                 ) {
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         modifier = Modifier.padding(horizontal = 16.dp)
                     ) {
                         Text(
                             text = selectedBucketName ?: strings.likes,
                             fontWeight = FontWeight.Bold,
                             fontSize = 16.sp
                         )
                         Spacer(Modifier.width(4.dp))
                         Icon(
                             imageVector = if (showDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                             contentDescription = null,
                             modifier = Modifier.size(20.dp)
                         )
                     }
                 }
                 
                 // Material3 DropdownMenu with shape
                val menuMaxHeight = 320.dp
                // Offset removed to keep it close to the header
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                ) {
                    val menuTextColor = if (isDark) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        Color.White
                    }
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        offset = DpOffset(0.dp, 4.dp), // Minimal spacing
                        modifier = Modifier
                           .background(if (isDark) MaterialTheme.colorScheme.surface else Color.Black.copy(alpha = 0.9f))
                           .heightIn(max = menuMaxHeight)
                           .width(200.dp)
                    ) {
                         // Header
                         DropdownMenuItem(
                             text = { Text(strings.likes, fontWeight = FontWeight.Bold, color = menuTextColor) },
                             onClick = {
                                 selectedBucketName = null
                                 showDropdown = false
                             },
                             leadingIcon = { Icon(Icons.Outlined.Favorite, null, tint = Color(0xFFFF5252)) },
                             trailingIcon = if (selectedBucketName == null) {
                                 { Icon(Icons.Outlined.Check, null, tint = menuTextColor, modifier = Modifier.size(18.dp)) }
                             } else null,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.bounceClick()
                         )
                         
                         HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = menuTextColor.copy(alpha = 0.2f))
                         
                         // Scrollable content handled by DropdownMenu internally, but let's ensure it looks good
                         folders.forEach { name ->
                             DropdownMenuItem(
                                 text = { 
                                     Text(
                                        text = name,
                                        color = menuTextColor,
                                        fontWeight = if (selectedBucketName == name) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                     )
                                 },
                                 onClick = {
                                     selectedBucketName = name
                                     showDropdown = false
                                     // Update selection count for sorting
                                     val newCounts = likesFolderCounts.toMutableMap()
                                     newCounts[name] = (newCounts[name] ?: 0) + 1
                                     prefs.likesFolderSelectionCounts = newCounts
                                     likesFolderCounts = newCounts
                                 },
                                 leadingIcon = { 
                                     Icon(
                                         imageVector = Icons.Outlined.Folder, 
                                         contentDescription = null, 
                                         tint = menuTextColor.copy(alpha = 0.7f) 
                                     ) 
                                 },
                                 trailingIcon = if (selectedBucketName == name) {
                                     { Icon(Icons.Outlined.Check, null, tint = menuTextColor, modifier = Modifier.size(18.dp)) }
                                 } else null,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.bounceClick()
                             )
                         }
                     }
                 }
            }
            }

            // Right Side Action Buttons
            val currentItem = if (galleryPreviewVisible) {
                galleryPreviewItems.getOrNull(galleryPreviewCurrentIndex)
            } else {
                likedQueueState.value.getOrNull(pagerState.currentPage)
            }
            val showUnifiedActionButtons =
                displayMode == LikesDisplayMode.TikTok &&
                    !galleryPreviewVisible
            if (showUnifiedActionButtons && currentItem != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(if (galleryPreviewVisible) 30f else 5f)
                        .padding(end = 12.dp)
                        .bottomNavigationBarPadding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    LikesTikTokActionIconButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = strings.back,
                        onClick = onBack
                    )

                    // Comment Button
                    val commentCount = remember(commentsRevision, currentItem.id) {
                        prefs.comments[currentItem.id]?.size ?: 0
                    }
                    
                    Box {
                        LikesTikTokActionIconButton(
                            icon = LikesActionIcons.Comment,
                            contentDescription = strings.comment,
                            onClick = {
                                currentCommentItem = currentItem
                                showCommentSheet = true
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                        
                        // Comment Count Badge
                        if (commentCount > 0) {
                            val badgeText = if (commentCount > 99) "99+" else commentCount.toString()
                            val isPill = badgeText.length >= 2
                            val badgeShape = if (isPill) RoundedCornerShape(9.dp) else CircleShape
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-3).dp)
                                    .zIndex(2f)
                                    .height(18.dp)
                                    .defaultMinSize(minWidth = 18.dp)
                                    .clip(badgeShape)
                                    .background(Color.Black.copy(alpha = 0.58f))
                                    .padding(horizontal = if (isPill) 6.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeText,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }
                    }

                    // Share Button
                    LikesTikTokActionIconButton(
                        icon = LikesActionIcons.Share,
                        contentDescription = strings.share,
                        onClick = { onShare(currentItem) },
                    )
                    
                    // Danmaku Switch
                    Box {
                        LikesTikTokActionIconButton(
                            icon = LikesActionIcons.Danmaku,
                            contentDescription = "Danmaku",
                            onClick = { 
                                danmakuEnabled = !danmakuEnabled 
                                prefs.danmakuEnabled = danmakuEnabled
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )

                        if (danmakuEnabled) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.White, CircleShape)
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    // Remove Like Button
                    if (selectedBucketName == null || galleryPreviewVisible) {
                        LikesTikTokActionIconButton(
                            icon = LikesActionIcons.Remove,
                            contentDescription = strings.removeLike,
                            onClick = {
                                itemToRemove = currentItem
                                showRemoveDialog = true
                            },
                        )
                    }
                }
            }

            LikesGalleryPreviewOverlay(
                visible = galleryPreviewVisible,
                items = galleryPreviewItems,
                startIndex = galleryPreviewStartIndex,
                sourceBoundsInRoot = galleryPreviewSourceBounds,
                sourceBoundsByItemId = galleryCardBoundsById,
                mediaSizeByItemId = galleryMediaSizeById,
                dismissRequestToken = galleryPreviewDismissRequestToken,
                sharedDismissEnabled = galleryPreviewSharedDismissEnabled,
                danmakuEnabled = danmakuEnabled,
                commentsProvider = { mediaId ->
                    if (commentsRevision >= 0) {
                        prefs.comments[mediaId] ?: emptyList()
                    } else {
                        emptyList()
                    }
                },
                onPageChanged = { page -> galleryPreviewCurrentIndex = page },
                onSharedDismissStart = { sourceItemId ->
                    galleryHiddenSourceItemId = sourceItemId
                },
                onDismissRequest = onGalleryPreviewDismissed,
                chromeContent = { activeItem ->
                    LikesPreviewActionButtons(
                        strings = strings,
                        commentCount = remember(commentsRevision, activeItem.id) {
                            prefs.comments[activeItem.id]?.size ?: 0
                        },
                        danmakuEnabled = danmakuEnabled,
                        showRemoveButton = selectedBucketName == null || galleryPreviewVisible,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp)
                            .bottomNavigationBarPadding(64.dp),
                        onComment = {
                            currentCommentItem = activeItem
                            showCommentSheet = true
                        },
                        onShare = { onShare(activeItem) },
                        onToggleDanmaku = {
                            danmakuEnabled = !danmakuEnabled
                            prefs.danmakuEnabled = danmakuEnabled
                        },
                        onRemove = {
                            itemToRemove = activeItem
                            showRemoveDialog = true
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
            )
        }
        
        // Remove Dialog
        if (showRemoveDialog && itemToRemove != null) {
            AlertDialog(
                onDismissRequest = { 
                    showRemoveDialog = false 
                    itemToRemove = null
                },
                title = { Text(strings.removeLikeTitle) },
                text = { Text(strings.removeLikeMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val item = itemToRemove!!
                            if (galleryPreviewVisible) {
                                pendingGalleryRemovalItem = item
                                galleryPreviewSharedDismissEnabled = false
                                requestGalleryPreviewDismiss()
                            } else {
                                removeLikedItem(item)
                            }
                            showRemoveDialog = false
                            itemToRemove = null
                        }
                    ) {
                        Text(strings.remove, color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showRemoveDialog = false 
                        itemToRemove = null
                    }) {
                        Text(strings.cancel, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                textContentColor = MaterialTheme.colorScheme.onBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        }

        LikesTutorialDialog(
            visible = showTutorialDialog,
            onDismiss = { showTutorialDialog = false }
        )
        
        if (displayMode == LikesDisplayMode.TikTok) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LikesDisplayModeToggleButton(
                        mode = displayMode,
                        isDark = isDark,
                        onToggle = {
                            displayMode = LikesDisplayMode.Gallery
                            prefs.likesDisplayMode = displayMode.toPrefValue()
                        }
                    )
                    UnifiedSubpageTopBarIconButton(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Tutorial",
                        onClick = { showTutorialDialog = true },
                        containerColor = if (isDark) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        } else {
                            Color.Black.copy(alpha = 0.2f)
                        },
                        iconTint = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White
                    )
                }
            }
        } else {
            UnifiedSubpageTopBar(
                title = "",
                onBack = onBack,
                titleColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                backButtonContainerColor = if (isDark) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                } else {
                    Color.Black.copy(alpha = 0.2f)
                },
                backButtonIconColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White
            ) {
                LikesGalleryColumnsButton(isDark = isDark)
                LikesDisplayModeToggleButton(
                    mode = displayMode,
                    isDark = isDark,
                    onToggle = {
                        displayMode = if (displayMode == LikesDisplayMode.TikTok) {
                            LikesDisplayMode.Gallery
                        } else {
                            LikesDisplayMode.TikTok
                        }
                        prefs.likesDisplayMode = displayMode.toPrefValue()
                    }
                )
                UnifiedSubpageTopBarIconButton(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = "Tutorial",
                    onClick = { showTutorialDialog = true },
                    containerColor = if (isDark) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    } else {
                        Color.Black.copy(alpha = 0.2f)
                    },
                    iconTint = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White
                )
            }
        }
    }
}

@Composable
private fun LikesTikTokActionIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .bounceClick(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

private object LikesActionIcons {
    val Comment: ImageVector by lazy {
        ImageVector.Builder(
            name = "LikesComment",
            defaultWidth = 640.dp,
            defaultHeight = 640.dp,
            viewportWidth = 640f,
            viewportHeight = 640f
        ).apply {
            path(fill = SolidColor(Color(0xFFBFBFBF))) {
                moveTo(64f, 416f)
                lineTo(64f, 192f)
                curveTo(64f, 139f, 107f, 96f, 160f, 96f)
                lineTo(480f, 96f)
                curveTo(533f, 96f, 576f, 139f, 576f, 192f)
                lineTo(576f, 416f)
                curveTo(576f, 469f, 533f, 512f, 480f, 512f)
                lineTo(360f, 512f)
                curveTo(354.8f, 512f, 349.8f, 513.7f, 345.6f, 516.8f)
                lineTo(230.4f, 603.2f)
                curveTo(226.2f, 606.3f, 221.2f, 608f, 216f, 608f)
                curveTo(202.7f, 608f, 192f, 597.3f, 192f, 584f)
                lineTo(192f, 512f)
                lineTo(160f, 512f)
                curveTo(107f, 512f, 64f, 469f, 64f, 416f)
                close()
            }
        }.build()
    }

    val Remove: ImageVector by lazy {
        ImageVector.Builder(
            name = "LikesRemove",
            defaultWidth = 640.dp,
            defaultHeight = 640.dp,
            viewportWidth = 640f,
            viewportHeight = 640f
        ).apply {
            path(fill = SolidColor(Color(0xFFBFBFBF))) {
                moveTo(197.1f, 96f)
                curveTo(214.4f, 96f, 231.3f, 99.4f, 247f, 105.7f)
                lineTo(301.8f, 190.9f)
                lineTo(226.4f, 266.3f)
                curveTo(224.9f, 267.8f, 224f, 269.9f, 224.1f, 272.1f)
                reflectiveCurveTo(225.1f, 276.3f, 226.7f, 277.8f)
                lineTo(338.7f, 381.8f)
                curveTo(341.6f, 384.5f, 346.1f, 384.7f, 349.2f, 382.1f)
                reflectiveCurveTo(353f, 375.1f, 350.9f, 371.7f)
                lineTo(290.5f, 273.6f)
                lineTo(381.2f, 198f)
                curveTo(383.8f, 195.9f, 384.7f, 192.3f, 383.6f, 189.2f)
                lineTo(360.4f, 124.6f)
                curveTo(383.6f, 106.3f, 412.6f, 96f, 442.9f, 96f)
                curveTo(516.4f, 96f, 576f, 155.6f, 576f, 229.1f)
                lineTo(576f, 231.7f)
                curveTo(576f, 343.9f, 436.1f, 474.2f, 363.1f, 529.9f)
                curveTo(350.7f, 539.3f, 335.5f, 544f, 320f, 544f)
                reflectiveCurveTo(289.2f, 539.4f, 276.9f, 529.9f)
                curveTo(203.9f, 474.2f, 64f, 343.9f, 64f, 231.7f)
                lineTo(64f, 229.1f)
                curveTo(64f, 155.6f, 123.6f, 96f, 197.1f, 96f)
                close()
            }
        }.build()
    }

    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "LikesShare",
            defaultWidth = 640.dp,
            defaultHeight = 640.dp,
            viewportWidth = 640f,
            viewportHeight = 640f
        ).apply {
            path(fill = SolidColor(Color(0xFFBFBFBF))) {
                moveTo(371.8f, 82.4f)
                curveTo(359.8f, 87.4f, 352f, 99f, 352f, 112f)
                lineTo(352f, 192f)
                lineTo(240f, 192f)
                curveTo(142.8f, 192f, 64f, 270.8f, 64f, 368f)
                curveTo(64f, 481.3f, 145.5f, 531.9f, 164.2f, 542.1f)
                curveTo(166.7f, 543.5f, 169.5f, 544f, 172.3f, 544f)
                curveTo(183.2f, 544f, 192f, 535.1f, 192f, 524.3f)
                curveTo(192f, 516.8f, 187.7f, 509.9f, 182.2f, 504.8f)
                curveTo(172.8f, 496f, 160f, 478.4f, 160f, 448.1f)
                curveTo(160f, 395.1f, 203f, 352.1f, 256f, 352.1f)
                lineTo(352f, 352.1f)
                lineTo(352f, 432.1f)
                curveTo(352f, 445f, 359.8f, 456.7f, 371.8f, 461.7f)
                curveTo(383.8f, 466.7f, 397.5f, 463.9f, 406.7f, 454.8f)
                lineTo(566.7f, 294.8f)
                curveTo(579.2f, 282.3f, 579.2f, 262f, 566.7f, 249.5f)
                lineTo(406.7f, 89.5f)
                curveTo(397.5f, 80.3f, 383.8f, 77.5f, 371.8f, 82.5f)
                close()
            }
        }.build()
    }

    val Danmaku: ImageVector by lazy {
        ImageVector.Builder(
            name = "LikesDanmaku",
            defaultWidth = 640.dp,
            defaultHeight = 640.dp,
            viewportWidth = 640f,
            viewportHeight = 640f
        ).apply {
            path(fill = SolidColor(Color(0xFFBFBFBF))) {
                moveTo(557.7f, 64.9f)
                lineTo(363.5f, 139.6f)
                lineTo(365.8f, 110.3f)
                curveTo(366.8f, 97.5f, 353f, 88.8f, 341.8f, 95.2f)
                lineTo(165.3f, 197.4f)
                curveTo(102.6f, 233.7f, 64f, 300.6f, 64f, 373f)
                curveTo(64f, 485.1f, 154.9f, 576f, 267f, 576f)
                curveTo(339.4f, 576f, 406.3f, 537.4f, 442.6f, 474.7f)
                lineTo(544.8f, 298.3f)
                curveTo(551.3f, 287.2f, 542.6f, 273.3f, 529.7f, 274.3f)
                lineTo(500.4f, 276.6f)
                lineTo(575.1f, 82.4f)
                curveTo(575.7f, 80.9f, 576f, 79.2f, 576f, 77.6f)
                curveTo(576f, 70.1f, 570f, 64.1f, 562.5f, 64.1f)
                curveTo(560.8f, 64.1f, 559.2f, 64.4f, 557.7f, 64.9f)
                close()
                moveTo(256f, 256f)
                curveTo(326.7f, 256f, 384f, 313.3f, 384f, 384f)
                curveTo(384f, 454.7f, 326.7f, 512f, 256f, 512f)
                curveTo(185.3f, 512f, 128f, 454.7f, 128f, 384f)
                curveTo(128f, 313.3f, 185.3f, 256f, 256f, 256f)
                close()
                moveTo(256f, 352f)
                curveTo(256f, 334.3f, 241.7f, 320f, 224f, 320f)
                curveTo(206.3f, 320f, 192f, 334.3f, 192f, 352f)
                curveTo(192f, 369.7f, 206.3f, 384f, 224f, 384f)
                curveTo(241.7f, 384f, 256f, 369.7f, 256f, 352f)
                close()
                moveTo(272f, 448f)
                curveTo(280.8f, 448f, 288f, 440.8f, 288f, 432f)
                curveTo(288f, 423.2f, 280.8f, 416f, 272f, 416f)
                curveTo(263.2f, 416f, 256f, 423.2f, 256f, 432f)
                curveTo(256f, 440.8f, 263.2f, 448f, 272f, 448f)
                close()
            }
        }.build()
    }
}

@Composable
fun LikesPreviewActionButtons(
    strings: LocalizedStrings,
    commentCount: Int,
    danmakuEnabled: Boolean,
    showRemoveButton: Boolean,
    modifier: Modifier = Modifier,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box {
            LikesTikTokActionIconButton(
                icon = LikesActionIcons.Comment,
                contentDescription = strings.comment,
                onClick = onComment,
                modifier = Modifier.align(Alignment.Center)
            )

            if (commentCount > 0) {
                val badgeText = if (commentCount > 99) "99+" else commentCount.toString()
                val isPill = badgeText.length >= 2
                val badgeShape = if (isPill) RoundedCornerShape(9.dp) else CircleShape
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-3).dp)
                        .zIndex(2f)
                        .height(18.dp)
                        .defaultMinSize(minWidth = 18.dp)
                        .clip(badgeShape)
                        .background(Color.Black.copy(alpha = 0.58f))
                        .padding(horizontal = if (isPill) 6.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }

        LikesTikTokActionIconButton(
            icon = LikesActionIcons.Share,
            contentDescription = strings.share,
            onClick = onShare,
        )

        Box {
            LikesTikTokActionIconButton(
                icon = LikesActionIcons.Danmaku,
                contentDescription = "Danmaku",
                onClick = onToggleDanmaku,
                modifier = Modifier.align(Alignment.Center)
            )

            if (danmakuEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        if (showRemoveButton) {
            LikesTikTokActionIconButton(
                icon = LikesActionIcons.Remove,
                contentDescription = strings.removeLike,
                onClick = onRemove,
            )
        }
    }
}

@Composable
fun DanmakuOverlay(
    comments: List<String>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        if (comments.isNotEmpty()) {
            val trackHeight = 60.dp
            val startY = 100.dp 
            
            val playOnce = comments.size <= 3
            val trackCount = if (playOnce) comments.size else minOf(5, comments.size)
            
            val nextIndex = remember(comments) { java.util.concurrent.atomic.AtomicInteger(0) }
            
            for (i in 0 until trackCount) {
                DanmakuTrack(
                    comments = comments,
                    nextIndex = if (playOnce) null else nextIndex,
                    topOffset = startY + trackHeight * i,
                    playOnce = playOnce,
                    fixedIndex = if (playOnce) i else null
                )
            }
        }
    }
}

@Composable
fun DanmakuTrack(
    comments: List<String>, 
    nextIndex: java.util.concurrent.atomic.AtomicInteger?, 
    topOffset: Dp,
    playOnce: Boolean,
    fixedIndex: Int?
) {
    var text by remember { mutableStateOf("") }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current
    val startX = with(density) { screenWidth.toPx() }
    val endX = -1000f

    val anim = remember { Animatable(startX) }

    LaunchedEffect(comments, playOnce, fixedIndex) {
        if (playOnce) {
            val index = fixedIndex ?: 0
            if (index in comments.indices) {
                text = comments[index]
                anim.snapTo(startX)
                val duration = (8000..15000).random()
                anim.animateTo(endX, animationSpec = tween(duration, easing = LinearEasing))
            }
        } else {
            while(true) {
                val index = (nextIndex?.getAndIncrement() ?: 0) % comments.size
                text = comments[index]
                
                anim.snapTo(startX)
                val duration = (8000..15000).random()
                anim.animateTo(endX, animationSpec = tween(duration, easing = LinearEasing))
                
                val delayMin = 500L
                val delayMax = 2000L
                kotlinx.coroutines.delay((delayMin..delayMax).random())
            }
        }
    }

    if (text.isNotEmpty()) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .offset(y = topOffset)
                .offset { IntOffset(anim.value.roundToInt(), 0) }
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
