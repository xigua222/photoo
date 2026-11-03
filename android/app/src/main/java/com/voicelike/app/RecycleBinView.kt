package com.voicelike.app

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.util.Locale
import kotlin.math.max

import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.interaction.MutableInteractionSource

data class RecycleBinItem(
    val media: MediaItem,
    val expiresAt: Long
)

fun loadRecycleBinItems(
    context: Context,
    trashInfo: Map<Long, Long>
): Pair<List<RecycleBinItem>, Set<Long>> {
    val results = mutableListOf<RecycleBinItem>()
    val staleIds = mutableSetOf<Long>() // Kept for signature compatibility, but unused for filtering
    val resolver = context.contentResolver
    
    // Query ALL trashed items from system
    val projectionList = mutableListOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.DURATION,
        MediaStore.MediaColumns.IS_TRASHED,
        "datetaken"
    )
    
    // Add DATE_EXPIRES for Android 11+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        projectionList.add(MediaStore.MediaColumns.DATE_EXPIRES)
    }
    
    val projection = projectionList.toTypedArray()
    
    // Query external files (images and videos)
    val baseUri = MediaStore.Files.getContentUri("external")
    
    // Selection: IS_TRASHED = 1 AND (MEDIA_TYPE = IMAGE OR MEDIA_TYPE = VIDEO)
    val selection = "${MediaStore.MediaColumns.IS_TRASHED} = 1 AND " +
                   "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR " +
                   "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
    
    val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
        }
        resolver.query(baseUri, projection, args, null)
    } else {
        resolver.query(
            baseUri,
            projection,
            selection,
            null,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )
    }

    cursor?.use { c ->
        val idIdx = c.getColumnIndex(MediaStore.Files.FileColumns._ID)
        val mediaTypeIdx = c.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val dateAddedIdx = c.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateTakenIdx = c.getColumnIndex("datetaken")
        val sizeIdx = c.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
        val widthIdx = c.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
        val heightIdx = c.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
        val mimeIdx = c.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
        val nameIdx = c.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val durationIdx = c.getColumnIndex(MediaStore.MediaColumns.DURATION)
        val expiresIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
            c.getColumnIndex(MediaStore.MediaColumns.DATE_EXPIRES) else -1

        while (c.moveToNext()) {
            val id = c.getLong(idIdx)
            val mediaType = c.getInt(mediaTypeIdx)
            val type = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) "video" else "photo"
            
            // Construct URI
            val uri = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            } else {
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }

            val rawDateAdded = if (dateAddedIdx >= 0) c.getLong(dateAddedIdx) else 0L
            val rawDateTaken = if (dateTakenIdx >= 0) c.getLong(dateTakenIdx) else 0L
            val finalDate = if (rawDateTaken > 0) rawDateTaken / 1000 else rawDateAdded

            val mediaItem = MediaItem(
                id = id,
                uri = uri,
                type = type,
                dateAdded = finalDate,
                size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L,
                width = if (widthIdx >= 0) c.getInt(widthIdx) else 0,
                height = if (heightIdx >= 0) c.getInt(heightIdx) else 0,
                mimeType = if (mimeIdx >= 0) c.getString(mimeIdx) ?: "" else "",
                displayName = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else "",
                path = null,
                bucketName = "",
                duration = if (durationIdx >= 0) c.getLong(durationIdx) else 0L
            )

            // Determine Expiration
            var expiresAt = 0L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && expiresIdx >= 0) {
                val systemExpiresSec = c.getLong(expiresIdx)
                if (systemExpiresSec > 0) {
                    expiresAt = systemExpiresSec * 1000L
                }
            }
            
            // Fallback to local trash info if system expiration is missing (e.g. pre-R or not set)
            if (expiresAt == 0L) {
                expiresAt = trashInfo[id] ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000L)
            }

            results.add(RecycleBinItem(mediaItem, expiresAt))
        }
    }
    
    // Also check for stale IDs in our local DB (ids in trashInfo that are NOT in results)
    // This helps keep our local DB clean even if we read from system
    val foundIds = results.map { it.media.id }.toSet()
    trashInfo.keys.forEach { id ->
        if (!foundIds.contains(id)) {
            staleIds.add(id)
        }
    }

    val sorted = results.sortedByDescending { it.expiresAt }
    return Pair(sorted, staleIds)
}

@Composable
fun RecycleBinView(
    items: List<RecycleBinItem>,
    onRestore: (Set<Long>) -> Unit,
    onDelete: (Set<Long>) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val selectedStates = remember(items) { mutableStateMapOf<Long, Boolean>() }
    var selectedCount by remember(items) { mutableIntStateOf(0) }
    var previewItem by remember { mutableStateOf<MediaItem?>(null) }
    val previewSourceBoundsById = remember { mutableStateMapOf<Long, androidx.compose.ui.geometry.Rect>() }
    var hiddenPreviewSourceId by remember { mutableStateOf<Long?>(null) }
    val gridState = rememberLazyGridState()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val allItemIds = remember(items) { items.map { it.media.id }.toSet() }
    val isAllSelected = selectedCount >= allItemIds.size && allItemIds.isNotEmpty()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RecycleBinHeader(
                title = strings.recycleBinTitle,
                onBack = onBack,
                isAllSelected = isAllSelected,
                onSelectAll = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isAllSelected) {
                        selectedStates.clear()
                        selectedCount = 0
                    } else {
                        selectedStates.clear()
                        allItemIds.forEach { selectedStates[it] = true }
                        selectedCount = allItemIds.size
                    }
                }
            )

            if (items.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.recycleBinEmpty,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.media.id }) { item ->
                            val itemId = item.media.id
                            val isSelected = selectedStates[itemId] == true
                            val daysLeft = calculateDaysLeft(item.expiresAt)
                            val daysLabel = String.format(Locale.getDefault(), strings.recycleBinDaysLeftFormat, daysLeft)
                            RecycleBinGridItem(
                                item = item.media,
                                daysLabel = daysLabel,
                                isSelected = isSelected,
                                onToggle = {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (isSelected) {
                                        selectedStates[itemId] = false
                                        selectedCount = (selectedCount - 1).coerceAtLeast(0)
                                    } else {
                                        selectedStates[itemId] = true
                                        selectedCount += 1
                                    }
                                },
                                onLongPreview = {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    previewItem = item.media
                                    hiddenPreviewSourceId = null
                                },
                                onBoundsMeasured = { bounds ->
                                    previewSourceBoundsById[itemId] = bounds
                                },
                                isSourceHidden = hiddenPreviewSourceId == itemId
                            )
                        }
                    }
                    
                    // Quick Scroll Bar
                    if (items.size > 20) {
                        RecycleBinFastScroller(
                            gridState = gridState,
                            totalItems = items.size,
                            rows = (items.size + 2) / 3,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedCount > 0,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .bottomNavigationBarPadding(24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        val selection = selectedStates.selectedIds()
                        if (selection.isNotEmpty()) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRestore(selection)
                            selectedStates.clear()
                            selectedCount = 0
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Text(text = strings.recycleBinRestore, color = if (isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val selection = selectedStates.selectedIds()
                        if (selection.isNotEmpty()) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete(selection)
                            selectedStates.clear()
                            selectedCount = 0
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.recycleBinDelete, fontWeight = FontWeight.Bold)
                }
            }
        }

        previewItem?.let { item ->
            MediaPreviewOverlay(
                item = item,
                sourceBoundsInRoot = previewSourceBoundsById[item.id],
                onSharedDismissStart = { sourceItemId ->
                    hiddenPreviewSourceId = sourceItemId
                },
                sharedTargetCornerRadius = 12.dp,
                sharedScaleClampMin = 0.04f,
                onDismiss = {
                    if (hiddenPreviewSourceId != null) {
                        previewItem = null
                        scope.launch {
                            withFrameNanos { }
                            if (previewItem == null) {
                                hiddenPreviewSourceId = null
                            }
                        }
                    } else {
                        previewItem = null
                        hiddenPreviewSourceId = null
                    }
                }
            )
        }
    }
}

@Composable
private fun RecycleBinHeader(
    title: String,
    onBack: () -> Unit,
    isAllSelected: Boolean,
    onSelectAll: () -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    UnifiedSubpageTopBar(
        title = title,
        onBack = {
            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onBack()
        }
    ) {
        TextButton(onClick = onSelectAll) {
            Text(
                text = if (isAllSelected) strings.recycleBinUnselectAll else strings.recycleBinSelectAll,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun RecycleBinGridItem(
    item: MediaItem,
    daysLabel: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onLongPreview: () -> Unit,
    onBoundsMeasured: (androidx.compose.ui.geometry.Rect) -> Unit,
    isSourceHidden: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    val mediaRequest = remember(item.id, item.uri, item.type, context) {
        val builder = ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(false)
            .size(220)
            .memoryCacheKey("recycle_bin_${item.type}_${item.id}")
            .diskCacheKey("recycle_bin_${item.type}_${item.id}")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)

        if (item.type == "video") {
            builder
                .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                .allowHardware(false)
        } else {
            builder.allowHardware(true)
        }

        builder.build()
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .graphicsLayer {
                alpha = if (isSourceHidden) 0f else 1f
            }
            .onGloballyPositioned { coordinates ->
                onBoundsMeasured(coordinates.boundsInRoot())
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle,
                onLongClick = onLongPreview
            )
    ) {
        if (item.type == "video") {
            AsyncImage(
                model = mediaRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Icon(
                imageVector = Icons.Outlined.PlayCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        } else {
            AsyncImage(
                model = mediaRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (androidx.compose.foundation.isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                    } else {
                        Color.Black.copy(alpha = 0.6f)
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = daysLabel,
                fontSize = 10.sp,
                color = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground else Color.White,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(0xFF3B82F6) else {
                        if (androidx.compose.foundation.isSystemInDarkTheme()) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        } else {
                            Color.White.copy(alpha = 0.8f)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private fun calculateDaysLeft(expiresAt: Long): Int {
    val now = System.currentTimeMillis()
    val diff = expiresAt - now
    val dayMs = 24L * 60L * 60L * 1000L
    val days = (diff + dayMs - 1L) / dayMs
    return max(1, days.toInt())
}

private fun SnapshotStateMap<Long, Boolean>.selectedIds(): Set<Long> {
    return entries.asSequence()
        .filter { it.value }
        .map { it.key }
        .toSet()
}
