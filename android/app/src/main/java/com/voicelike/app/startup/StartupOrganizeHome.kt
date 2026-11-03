package com.voicelike.app.startup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.voicelike.app.CatalogScanCoordinator
import com.voicelike.app.LocalAppLanguage
import com.voicelike.app.LocalAppStrings
import com.voicelike.app.LowQualityManager
import com.voicelike.app.MainDeleteQueueButton
import com.voicelike.app.MediaItem
import com.voicelike.app.PHOTO_WALL_ENTRY_HEIGHT
import com.voicelike.app.PhotoWallNewBadge
import com.voicelike.app.PhotoWallPreferences
import com.voicelike.app.PhotoWallPreviewMosaic
import com.voicelike.app.photoWallStrings
import com.voicelike.app.SimilarPhotoManager
import com.voicelike.app.format
import com.voicelike.app.formatFileSize
import com.voicelike.app.normalizeSimilarGroupsForDisplay
import com.voicelike.app.organizeResultStrings
import com.voicelike.app.organizeSortingStrings
import com.voicelike.app.rememberHomeCardStillImageRequest
import com.voicelike.app.rememberHomeCardVideoFrameImageRequest
import com.voicelike.app.resolveHomeCardPhotoAspectRatio
import com.voicelike.app.shouldAnalyzeAsStandardPhoto
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// Everything the organize home shows besides the range chip: the next deck, the
// counts behind the Organize button and the deep-organize tool entries.
data class StartupOrganizeHome(
    // The first cards of the real deck the sorting page will deal.
    val deckItems: List<MediaItem>,
    val hasPermission: Boolean,
    val pendingDeleteCount: Int,
    val todayOrganized: Int,
    val savedBytes: Long,
    // Whether the active range has any photo at all (tells "range done" from "range empty").
    val scopeHasPhotos: Boolean,
    val onPendingDelete: () -> Unit,
    val onProgress: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onDeepOrganize: () -> Unit,
    val onSimilar: () -> Unit,
    val onLowQuality: () -> Unit,
    val onVideoCompress: () -> Unit,
    val onImageCompress: () -> Unit,
    val onRecycleBin: () -> Unit,
    // The first photos the photo wall opens with, for the little wall on its entry card.
    val photoWallPreview: List<MediaItem> = emptyList(),
    val onPhotoWall: () -> Unit = {}
)

private enum class HomeDeckPlaceholder { Loading, NeedsAccess, AllDone, RangeDone, RangeEmpty }

private data class HomeDeckSlot(val side: Float, val dy: Float, val scale: Float, val rotation: Float)

// Top card centred, the next two fanned out left and right behind it (iOS DeckHero).
private val homeDeckSlots = listOf(
    HomeDeckSlot(side = 0f, dy = -0.02f, scale = 1f, rotation = 0f),
    HomeDeckSlot(side = -1f, dy = 0.06f, scale = 0.9f, rotation = -8f),
    HomeDeckSlot(side = 1f, dy = 0.06f, scale = 0.9f, rotation = 8f)
)
private const val HOME_DECK_PEEK_RATIO = 0.3f
private const val HOME_DECK_BOX_ASPECT = 0.62f

@Composable
internal fun StartupOrganizeHomeContent(
    copy: StartupWelcomeCopy,
    loading: Boolean,
    progress: Float,
    home: StartupOrganizeHome,
    scope: StartupOrganizeScope?,
    onStartClick: () -> Unit
) {
    val language = LocalAppLanguage.current
    val sortingStrings = organizeSortingStrings(language)
    val resultStrings = organizeResultStrings(language)
    val onBackground = MaterialTheme.colorScheme.onBackground
    val muted = onBackground.copy(alpha = 0.55f)
    val remaining = scope?.remainingCount
    val rangeActive = scope?.active == true
    val placeholder = when {
        !home.hasPermission -> HomeDeckPlaceholder.NeedsAccess
        home.deckItems.isNotEmpty() -> null
        loading || remaining == null -> HomeDeckPlaceholder.Loading
        remaining > 0 -> HomeDeckPlaceholder.Loading
        rangeActive && !home.scopeHasPhotos -> HomeDeckPlaceholder.RangeEmpty
        rangeActive -> HomeDeckPlaceholder.RangeDone
        else -> HomeDeckPlaceholder.AllDone
    }
    val queueFinished = placeholder == HomeDeckPlaceholder.AllDone ||
        placeholder == HomeDeckPlaceholder.RangeDone ||
        placeholder == HomeDeckPlaceholder.RangeEmpty
    // Chrome sinks and fades while the deck lifts into the sorting page.
    val chromeModifier = Modifier.graphicsLayer {
        alpha = 1f - progress
        translationY = 16.dp.toPx() * progress
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = progress == 0f)
            .statusBarsPadding()
            .padding(bottom = 132.dp)
    ) {
        Row(
            modifier = chromeModifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = copy.title,
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = onBackground),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 21.sp, maxFontSize = 28.sp, stepSize = 0.5.sp),
                modifier = Modifier.weight(1f)
            )
            if (home.pendingDeleteCount > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                MainDeleteQueueButton(
                    pendingCount = home.pendingDeleteCount,
                    onClick = home.onPendingDelete
                )
            }
        }
        Text(
            text = copy.subtitle,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = muted,
            modifier = chromeModifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 2.dp)
        )

        Box(modifier = Modifier.padding(top = 28.dp)) {
            AnimatedContent(
                targetState = placeholder != null && placeholder != HomeDeckPlaceholder.Loading,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "home-deck"
            ) { showsEmptyState ->
                if (showsEmptyState && placeholder != null) {
                    HomeDeckEmptyState(
                        placeholder = placeholder,
                        sortingStrings = sortingStrings,
                        title = when (placeholder) {
                            HomeDeckPlaceholder.AllDone -> resultStrings.allDoneTitle
                            HomeDeckPlaceholder.RangeDone -> resultStrings.rangeDoneTitle
                            HomeDeckPlaceholder.RangeEmpty -> sortingStrings.scopeEmptyTitle
                            else -> resultStrings.accessTitle
                        },
                        message = when (placeholder) {
                            HomeDeckPlaceholder.AllDone -> sortingStrings.homeAllDoneMessage
                            HomeDeckPlaceholder.RangeDone -> resultStrings.rangeDoneMessage
                            HomeDeckPlaceholder.RangeEmpty -> sortingStrings.scopeEmptyMessage
                            else -> null
                        },
                        actionLabel = when (placeholder) {
                            HomeDeckPlaceholder.RangeDone, HomeDeckPlaceholder.RangeEmpty -> sortingStrings.clearRange
                            HomeDeckPlaceholder.NeedsAccess -> resultStrings.accessButton
                            else -> null
                        },
                        onAction = when (placeholder) {
                            HomeDeckPlaceholder.NeedsAccess -> home.onRequestPermission
                            else -> scope?.onClear ?: {}
                        },
                        modifier = chromeModifier
                    )
                } else {
                    HomeDeckHero(
                        items = home.deckItems,
                        loadingLabel = sortingStrings.deckLoading,
                        progress = progress,
                        onTap = onStartClick
                    )
                }
            }
        }

        if (scope != null && home.hasPermission) {
            StartupOrganizeScopeChip(
                scope = scope,
                modifier = chromeModifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 22.dp, start = 20.dp, end = 20.dp)
            )
        }

        Column(
            modifier = chromeModifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = if (home.hasPermission) 18.dp else 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!queueFinished) {
                HomePrimaryButton(
                    label = when {
                        !home.hasPermission -> resultStrings.accessButton
                        loading || remaining == null || remaining <= 0 -> startupOrganizeStartLabel(language)
                        else -> sortingStrings.format(sortingStrings.organizeCountFmt, remaining)
                    },
                    onClick = if (home.hasPermission) onStartClick else home.onRequestPermission
                )
            }
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = home.onProgress)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = sortingStrings.format(sortingStrings.todayOrganizedFmt, home.todayOrganized.toString()),
                    fontSize = 13.sp,
                    color = muted,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sortingStrings.format(sortingStrings.savedFmt, formatFileSize(home.savedBytes)),
                        fontSize = 13.sp,
                        color = muted,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = muted.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Another way to organize: the whole library spread out, flick away the misses (PhotoWallPage).
        if (home.hasPermission) {
            HomePhotoWallEntry(
                previewItems = home.photoWallPreview,
                onClick = home.onPhotoWall,
                modifier = chromeModifier.padding(start = 20.dp, end = 20.dp, top = 30.dp)
            )
        }

        HomeDeepOrganizeSection(
            home = home,
            sortingStrings = sortingStrings,
            modifier = chromeModifier.padding(start = 20.dp, end = 20.dp, top = if (home.hasPermission) 24.dp else 30.dp)
        )
    }
    // Content scrolling up fades out under the status bar instead of running beneath its icons.
    val pageBackground = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(pageBackground)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(20.dp)
            .background(Brush.verticalGradient(listOf(pageBackground, pageBackground.copy(alpha = 0f))))
    )
    }
}

@Composable
private fun HomeDeckHero(
    items: List<MediaItem>,
    loadingLabel: String,
    progress: Float,
    onTap: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressDarken by animateFloatAsState(if (pressed) 0.04f else 0f, tween(120), label = "deck-press")
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val displayWidth = ((maxWidth - 32.dp) * 0.5f).coerceIn(150.dp, 230.dp)
        val boxHeight = displayWidth / HOME_DECK_BOX_ASPECT
        val deck = items.take(homeDeckSlots.size)
        fun photoSize(item: MediaItem?): Pair<Dp, Dp> {
            val aspect = item?.let(::resolveHomeCardPhotoAspectRatio) ?: HOME_DECK_BOX_ASPECT
            return if (aspect >= HOME_DECK_BOX_ASPECT) {
                displayWidth to displayWidth / aspect
            } else {
                boxHeight * aspect to boxHeight
            }
        }
        val (frontW, frontH) = photoSize(deck.firstOrNull())
        val containerHeight = frontH + max(frontW.value, frontH.value).dp * 0.26f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .clickable(interactionSource = interaction, indication = null, onClick = onTap)
                .graphicsLayer {
                    val lift = 1f + 0.12f * progress
                    scaleX = lift
                    scaleY = lift
                    alpha = 1f - progress
                    translationY = -24.dp.toPx() * progress
                },
            contentAlignment = Alignment.Center
        ) {
            if (deck.isEmpty()) {
                HomeDeckSkeleton(width = frontW, height = frontH, label = loadingLabel)
                return@Box
            }
            for (index in deck.indices.reversed()) {
                val slot = homeDeckSlots[index]
                val item = deck[index]
                val (rawW, rawH) = photoSize(item)
                // Back photos are never wider or taller than the top photo.
                val cap = if (index == 0) 1f else min(1f, min(frontW / rawW, frontH / rawH))
                val w = rawW * slot.scale * cap
                val h = rawH * slot.scale * cap
                val dx = if (slot.side == 0f) {
                    0.dp
                } else {
                    val radians = Math.toRadians(abs(slot.rotation).toDouble())
                    val halfExtent = ((w.value * cos(radians) + h.value * sin(radians)) / 2).toFloat()
                    val outerEdge = frontW.value / 2 + frontW.value * HOME_DECK_PEEK_RATIO
                    (slot.side * (outerEdge - halfExtent)).dp
                }
                HomeDeckCard(
                    item = item,
                    width = w,
                    height = h,
                    darken = pressDarken,
                    modifier = Modifier
                        .offset(x = dx, y = frontH * slot.dy)
                        .graphicsLayer { rotationZ = slot.rotation }
                )
            }
        }
    }
}

@Composable
private fun HomeDeckCard(
    item: MediaItem,
    width: Dp,
    height: Dp,
    darken: Float,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(14.dp)
    val request = if (item.type == "video") {
        rememberHomeCardVideoFrameImageRequest(item.id, item.uri, crossfade = false)
    } else {
        rememberHomeCardStillImageRequest(item.id, item.uri, crossfade = false)
    }
    val darkenFilter = remember(darken) {
        if (darken <= 0f) {
            null
        } else {
            val k = 1f - darken
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        k, 0f, 0f, 0f, 0f,
                        0f, k, 0f, 0f, 0f,
                        0f, 0f, k, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }
    Box(
        modifier = modifier
            .size(width, height)
            .shadow(
                elevation = if (isDark) 12.dp else 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.48f else 0.14f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.48f else 0.18f)
            )
            .clip(shape)
            .background(if (isDark) Color(0xFF1F1F22) else Color(0xFFF7F7FA))
            .border(
                width = if (isDark) 1.dp else 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                shape = shape
            )
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = darkenFilter,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun HomeDeckSkeleton(width: Dp, height: Dp, label: String) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(shape)
            .background(if (isDark) Color(0xFF27272A) else Color.White)
            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// Rings around an icon, then title, message and an optional way out (iOS HomeEmptyState).
@Composable
private fun HomeDeckEmptyState(
    placeholder: HomeDeckPlaceholder,
    sortingStrings: com.voicelike.app.OrganizeSortingStrings,
    title: String,
    message: String?,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = placeholder == HomeDeckPlaceholder.AllDone || placeholder == HomeDeckPlaceholder.RangeDone
    val tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    val icon = when (placeholder) {
        HomeDeckPlaceholder.AllDone -> Icons.Outlined.CheckCircle
        HomeDeckPlaceholder.RangeDone, HomeDeckPlaceholder.RangeEmpty -> Icons.Outlined.FilterList
        else -> Icons.Outlined.PhotoLibrary
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(236.dp), contentAlignment = Alignment.Center) {
            for (ring in 2 downTo 0) {
                Box(
                    modifier = Modifier
                        .size((124 + ring * 52).dp)
                        .border(1.dp, tint.copy(alpha = (0.16f - ring * 0.045f).coerceAtLeast(0f)), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(tint.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(38.dp))
            }
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        if (message != null) {
            Text(
                text = message,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (actionLabel != null) {
            val shape = RoundedCornerShape(22.dp)
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .height(44.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f), shape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (placeholder != HomeDeckPlaceholder.NeedsAccess) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = actionLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun HomePrimaryButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeDeepOrganizeSection(
    home: StartupOrganizeHome,
    sortingStrings: com.voicelike.app.OrganizeSortingStrings,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scanState by CatalogScanCoordinator.state.collectAsState()
    val similarGroups by SimilarPhotoManager.foundGroups.collectAsState()
    val similarCount = remember(similarGroups) { normalizeSimilarGroupsForDisplay(similarGroups).size }
    val lowQualityItems by LowQualityManager.foundItems.collectAsState()
    val lowQualityCount = remember(lowQualityItems) {
        lowQualityItems.count { shouldAnalyzeAsStandardPhoto(it.first) }
    }
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.deepOrganizeTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            // Scanning feeds the similar / low-quality tools; the full page has pause and rescan.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = home.onDeepOrganize)
                    .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (scanState.isRunning) {
                        sortingStrings.format(
                            sortingStrings.scanningFmt,
                            (scanState.overallProgress * 100).toInt().coerceIn(0, 99)
                        )
                    } else {
                        sortingStrings.seeAll
                    },
                    fontSize = 13.sp,
                    color = muted,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeToolTile(
                icon = Icons.Outlined.ContentCopy,
                title = strings.similar ?: "Similar Photos",
                subtitle = strings.similarPhotosSubtitle ?: "",
                badge = similarCount,
                onClick = home.onSimilar,
                modifier = Modifier.weight(1f)
            )
            HomeToolTile(
                icon = Icons.Outlined.BlurOn,
                title = strings.lowQuality ?: "Low Quality",
                subtitle = strings.lowQualitySubtitle ?: "",
                badge = lowQualityCount,
                onClick = home.onLowQuality,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeToolTile(
                icon = Icons.Outlined.VideoSettings,
                title = strings.videoCompressTitle ?: "Video Compression",
                subtitle = strings.videoCompressSubtitle ?: "",
                badge = 0,
                onClick = home.onVideoCompress,
                modifier = Modifier.weight(1f)
            )
            HomeToolTile(
                icon = Icons.Outlined.Image,
                title = strings.imageCompressTitle,
                subtitle = strings.imageCompressSubtitle,
                badge = 0,
                onClick = home.onImageCompress,
                modifier = Modifier.weight(1f)
            )
        }
        HomeToolRow(
            icon = Icons.Outlined.Delete,
            title = strings.recycleBinTitle,
            onClick = home.onRecycleBin
        )
    }
}

// Title and one line on the left, a tilted corner of the wall on the right (iOS PhotoWallEntry);
// NEW next to the title until the wall has been opened once.
@Composable
private fun HomePhotoWallEntry(
    previewItems: List<MediaItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = photoWallStrings(LocalAppLanguage.current)
    LaunchedEffect(Unit) { PhotoWallPreferences.load(context) }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PHOTO_WALL_ENTRY_HEIGHT)
            .then(homeGroupCardModifier(20.dp))
            .clip(shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 18.dp, end = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = strings.title,
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 17.sp, stepSize = 0.5.sp),
                    modifier = Modifier.weight(1f, fill = false)
                )
                AnimatedVisibility(
                    visible = !PhotoWallPreferences.visited,
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 0.6f)
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        PhotoWallNewBadge()
                    }
                }
            }
            Text(
                text = strings.subtitle,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        PhotoWallPreviewMosaic(
            items = previewItems,
            modifier = Modifier
                .width(150.dp)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun homeGroupCardModifier(radius: Dp): Modifier {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(radius)
    return Modifier
        .then(if (isDark) Modifier else Modifier.shadow(4.dp, shape, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f)))
        .clip(shape)
        .background(if (isDark) Color(0xFF27272A) else Color.White, shape)
        .border(0.5.dp, if (isDark) Color(0xFF3F3F46).copy(alpha = 0.6f) else Color(0xFFDCDCE3).copy(alpha = 0.6f), shape)
}

@Composable
private fun HomeIconChip(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HomeCountBadge(count: Int) {
    Box(
        modifier = Modifier
            .heightIn(min = 26.dp)
            .widthIn(min = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun HomeToolTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(homeGroupCardModifier(20.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 128.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            HomeIconChip(icon)
            Spacer(modifier = Modifier.weight(1f))
            if (badge > 0) HomeCountBadge(badge)
        }
        BasicText(
            text = title,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 15.sp, stepSize = 0.5.sp),
            modifier = Modifier.padding(top = 14.dp)
        )
        if (subtitle.isNotBlank()) {
            BasicText(
                text = subtitle,
                style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun HomeToolRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(homeGroupCardModifier(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeIconChip(icon)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}
