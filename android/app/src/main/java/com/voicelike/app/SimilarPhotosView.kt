package com.voicelike.app

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi

private data class SimilarFullScreenSession(
    val items: List<MediaItem>,
    val initialIndex: Int,
    val sourceBoundsInRoot: androidx.compose.ui.geometry.Rect?,
    val sourceBoundsByItemId: Map<Long, androidx.compose.ui.geometry.Rect>
)

private val SimilarCardFooterHeight = 64.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimilarPhotosView(
    similarGroups: List<List<MediaItem>>,
    muteVideos: Boolean = true,
    isScanning: Boolean = false,
    progress: Float = 0f,
    pendingTrashCount: Int = 0,
    canUndo: Boolean = false,
    onClose: () -> Unit,
    onFinishProcessing: (Set<Long>, Set<Long>) -> Unit, // Legacy support, can be ignored if using onDecision
    onDecision: (Set<Long>, Set<Long>) -> Unit = { _, _ -> }, // Immediate callback: (deleted, processed)
    onUndo: () -> Unit = {},
    onViewTrash: () -> Unit = {}
) {
    var localDecisionHistory by remember {
        mutableStateOf(listOf<Pair<Set<Long>, Set<Long>>>())
    }
    val locallyHiddenIds = remember(localDecisionHistory) {
        buildSet<Long> {
            localDecisionHistory.forEach { (_, processedIds) ->
                addAll(processedIds)
            }
        }
    }

    // We no longer maintain currentGroupIndex. 
    // We always show the FIRST group in the list.
    // When a decision is made, the list is updated externally (by removing processed items),
    // causing the next group to naturally slide into the first position.
    
    // Ensure we only process groups that are valid pairs (or at least > 1)
    val validGroups = remember(similarGroups, locallyHiddenIds) {
        normalizeSimilarGroupsForDisplay(
            similarGroups.mapNotNull { group ->
                group.filterNot { locallyHiddenIds.contains(it.id) }
                    .takeIf { it.size >= 2 }
            }
        )
    }
    val validGroupsByLeadId = remember(validGroups) {
        validGroups.associateBy { it.firstOrNull()?.id }
    }

    // Stabilize the current group: Try to keep showing the same group even if list updates (incremental scan)
    var displayedGroupId by remember { mutableStateOf<Long?>(null) }

    val currentGroup = remember(validGroups, validGroupsByLeadId, displayedGroupId) {
        if (displayedGroupId != null) {
            val found = validGroupsByLeadId[displayedGroupId]
            if (found != null) return@remember found
        }
        validGroups.firstOrNull()
    }

    LaunchedEffect(currentGroup?.firstOrNull()?.id) {
        val currentLeadId = currentGroup?.firstOrNull()?.id ?: return@LaunchedEffect
        // Only lock onto the initially displayed group. After that, keep the
        // caller-selected group target so transient refreshes do not rewind us.
        if (displayedGroupId == null) {
            displayedGroupId = currentLeadId
        }
    }
    
    val idsToDelete = remember(localDecisionHistory) {
        buildSet<Long> {
            localDecisionHistory.forEach { (deletedIds, _) ->
                addAll(deletedIds)
            }
        }
    }
    val idsProcessed = remember(localDecisionHistory) {
        buildSet<Long> {
            localDecisionHistory.forEach { (_, processedIds) ->
                addAll(processedIds)
            }
        }
    }
    
    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val neutralCardBorder = if (isDark) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    } else {
        BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f))
    }
    val pillBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    }

    // Item Details Cache (for Location/Date)
    val itemDetails = remember { mutableStateMapOf<Long, MediaItem>() }
    LaunchedEffect(currentGroup) {
        currentGroup
            ?.take(2)
            ?.forEach { item ->
                val cached = PhotoCardDataCache.get(item.id)
                if (cached != null) {
                    itemDetails[cached.id] = cached
                    return@forEach
                }
                val insight = withContext(Dispatchers.Default) {
                    MediaInsightRepository.get(context, item.id)
                }
                itemDetails[item.id] = item.copy(
                    location = insight?.location ?: item.location
                )
            }
    }
    LaunchedEffect(currentGroup?.firstOrNull()?.id) {
        val previewItems = currentGroup?.take(2).orEmpty()
        if (previewItems.isEmpty()) return@LaunchedEffect
        prefetchSimilarCompareMedia(context, previewItems)
    }

    // Done Check (Empty State)
    if (currentGroup == null && !isScanning) {
        ScanStagePlaceholder(
            title = strings.noSimilarPhotosFound ?: "No similar photos found",
            subtitle = strings.similarPhotosDone ?: "You're all caught up!",
            isScanning = false,
            progress = 1f,
            actionLabel = strings.done ?: "Done",
            onAction = {
                onFinishProcessing(idsToDelete, idsProcessed)
                onClose()
            }
        )
        return
    }

    // Scanning State
    if (currentGroup == null && isScanning) {
        ScanStagePlaceholder(
            title = strings.scanningWait ?: "Please wait...",
            subtitle = strings.scanningDescriptionDetailed
                ?: "This may take 1-3 minutes. You can exit and let it run in the background.",
            isScanning = true,
            progress = progress,
            actionLabel = strings.exit ?: "Exit",
            onAction = onClose
        )
        return
    }

    // Selection State for Current Group
    // Reset selection when the group changes (based on ID of first item)
    val currentGroupId = currentGroup?.firstOrNull()?.id
    var keptId by remember(currentGroupId) { mutableStateOf<Long?>(null) }
    
    // Expanded Detail State
    var expandedId by remember { mutableStateOf<Long?>(null) }

    // Local FullScreen State (Decoupled from Activity)
    val scope = rememberCoroutineScope()
    val cardBoundsById = remember { mutableStateMapOf<Long, androidx.compose.ui.geometry.Rect>() }
    val photoBoundsById = remember { mutableStateMapOf<Long, androidx.compose.ui.geometry.Rect>() }
    var hiddenSourceItemId by remember { mutableStateOf<Long?>(null) }
    var localFullScreenState by remember { mutableStateOf<SimilarFullScreenSession?>(null) }
    var splitCompareSession by remember { mutableStateOf<SimilarSplitCompareSession?>(null) }

    LaunchedEffect(currentGroupId) {
        hiddenSourceItemId = null
        splitCompareSession = null
    }

    val pair = currentGroup!!.take(2)
    fun resolvePostDecisionGroupLeadId(processedIds: Set<Long>): Long? {
        val activeGroup = currentGroup ?: return null
        val activeLeadId = activeGroup.firstOrNull()?.id ?: return null
        val currentIndex = validGroups.indexOfFirst { group ->
            group.firstOrNull()?.id == activeLeadId
        }
        val remainingCurrentGroup = activeGroup.filterNot { processedIds.contains(it.id) }
        if (remainingCurrentGroup.size >= 2) {
            return remainingCurrentGroup.first().id
        }
        if (currentIndex < 0) {
            return null
        }
        return validGroups
            .drop(currentIndex + 1)
            .firstOrNull()
            ?.firstOrNull()
            ?.id
    }

    fun recordDecision(deletedIds: Set<Long>, processedIds: Set<Long>) {
        localDecisionHistory = localDecisionHistory + (deletedIds to processedIds)
    }

    val handleKeepSelected: () -> Unit = {
        if (keptId != null) {
            val unkept = pair.filter { it.id != keptId }.map { it.id }.toSet()
            val allInPair = pair.map { it.id }.toSet()

            recordDecision(unkept, allInPair)
            displayedGroupId = resolvePostDecisionGroupLeadId(allInPair)
            splitCompareSession = null
            onDecision(unkept, allInPair)
        }
    }
    val handleKeepAll: () -> Unit = {
        val allInPair = pair.map { it.id }.toSet()

        recordDecision(emptySet(), allInPair)
        displayedGroupId = resolvePostDecisionGroupLeadId(allInPair)
        splitCompareSession = null
        onDecision(emptySet(), allInPair)
    }
    val handleDeleteAll: () -> Unit = {
        val allInPair = pair.map { it.id }.toSet()

        recordDecision(allInPair, allInPair)
        displayedGroupId = resolvePostDecisionGroupLeadId(allInPair)
        splitCompareSession = null
        onDecision(allInPair, allInPair)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6))
            // Removed click-to-close behavior as requested
    ) {
        val comparisonAreaHeight = (maxHeight * 0.42f).coerceIn(240.dp, 520.dp)
        val undoSlotHeight = 52.dp

        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = 48.dp), // Add padding to internal content instead of root to allow full screen bg
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isScanning) strings.scanning else "${strings.similar} (${validGroups.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Clean Button
                        AnimatedVisibility(visible = pendingTrashCount > 0) {
                            Button(
                                onClick = onViewTrash,
                                modifier = Modifier.bounceClick(onClick = onViewTrash),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)), // red-50
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)), // red-200
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Clean",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(pendingTrashCount.toString(), color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        UnifiedSubpageTopBarIconButton(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            onClick = {
                                onFinishProcessing(idsToDelete, idsProcessed)
                                onClose()
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Start
            ) {
            Surface(
                onClick = {
                    splitCompareSession = SimilarSplitCompareSession(items = pair)
                },
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f)
                } else {
                    Color.White.copy(alpha = 0.94f)
                },
                contentColor = MaterialTheme.colorScheme.onBackground,
                border = BorderStroke(1.dp, pillBorderColor),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInFull,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "全屏对比",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(comparisonAreaHeight),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { item ->
                        key(item.id) {
                        val isSelected = keptId == item.id
                        val isSourceHidden = hiddenSourceItemId == item.id
                        val details = itemDetails[item.id] ?: item
                        
                        val openFullScreen: () -> Unit = {
                            val sourceBoundsByItemId = pair.mapNotNull { candidate ->
                                val targetBounds =
                                    photoBoundsById[candidate.id] ?: cardBoundsById[candidate.id]
                                targetBounds?.let { candidate.id to it }
                            }.toMap()
                            localFullScreenState = SimilarFullScreenSession(
                                items = pair,
                                initialIndex = pair.indexOf(item),
                                sourceBoundsInRoot = sourceBoundsByItemId[item.id]
                                    ?: photoBoundsById[item.id]
                                    ?: cardBoundsById[item.id],
                                sourceBoundsByItemId = sourceBoundsByItemId
                            )
                            hiddenSourceItemId = null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .onGloballyPositioned { coordinates ->
                                    cardBoundsById[item.id] = coordinates.boundsInRoot()
                                }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .advancedTextureCard(shape = RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                                border = if (isSelected) {
                                    BorderStroke(3.dp, Color(0xFF60A5FA))
                                } else {
                                    neutralCardBorder
                                }
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Photo Area
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                photoBoundsById[item.id] = coordinates.boundsInRoot()
                                            }
                                    ) {
                                        val comparisonImageRequest = remember(item.id, item.uri) {
                                            buildSimilarCompareImageRequest(
                                                context = context,
                                                item = item
                                            )
                                        }
                                        AsyncImage(
                                            model = comparisonImageRequest,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surface)
                                        )
                                        if (isSourceHidden) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        if (isDark) {
                                                            MaterialTheme.colorScheme.surface
                                                        } else {
                                                            Color(0xFFF3F4F6)
                                                        }
                                                    )
                                            ) {
                                                AsyncImage(
                                                    model = comparisonImageRequest,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        
                                        // Selection Checkmark Overlay
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(12.dp)
                                                    .background(Color(0xFF3B82F6), CircleShape)
                                                    .size(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    
                                    // Footer Info Area (White Space)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(SimilarCardFooterHeight)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Date
                                        val date = Date(details.dateAdded * 1000)
                                        Text(
                                            text = dateFormatter.format(date),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        // Size and Dimensions (New)
                                        Spacer(modifier = Modifier.height(1.dp))
                                        val sizeStr = Formatter.formatFileSize(context, details.size)
                                        val dimStr = "${details.width} x ${details.height}"
                                        Text(
                                            text = "$dimStr • $sizeStr",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        Spacer(modifier = Modifier.height(1.dp))
                                        val hasLocation = details.location != "Unknown Location" && details.location.isNotBlank()
                                        Text(
                                            text = if (hasLocation) details.location else " ",
                                            fontSize = 10.sp,
                                            color = if (hasLocation) {
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                            } else {
                                                Color.Transparent
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(item.id, keptId) {
                                        detectTapGestures(
                                            onTap = {
                                                keptId = if (keptId == item.id) null else item.id
                                            },
                                            onLongPress = {
                                                openFullScreen()
                                            }
                                        )
                                    }
                            )
                        }
                        }
                    }
                }
            }

            // Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(undoSlotHeight),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = canUndo,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        Button(
                            onClick = { 
                                if (localDecisionHistory.isNotEmpty()) {
                                    localDecisionHistory = localDecisionHistory.dropLast(1)
                                }
                                displayedGroupId = null
                                onUndo() 
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(30.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .advancedTextureButton(shape = RoundedCornerShape(30.dp))
                                .bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Replay,
                                contentDescription = strings.undo,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.undo,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                // "Keep Selected" Button (Primary)
                Button(
                    onClick = handleKeepSelected,
                    enabled = keptId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(strings.keepSelected, fontSize = 16.sp)
                }

                // Secondary Actions Row: Keep All | Delete All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Keep All
                    Button(
                        onClick = handleKeepAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F6),
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .advancedTextureButton(shape = RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(strings.keepAll, fontSize = 14.sp)
                    }

                    // Delete All (New)
                    Button(
                        onClick = handleDeleteAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.errorContainer else Color(0xFFFEE2E2),
                            contentColor = if (isDark) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFDC2626)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .advancedTextureButton(shape = RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.deleteAll, fontSize = 14.sp)
                    }
                }
            }
        }
        
        // Local FullScreen Viewer
        AnimatedVisibility(
            visible = localFullScreenState != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            localFullScreenState?.let { session ->
                val dismissSourceBoundsByItemId = session.items.mapNotNull { candidate ->
                    val targetBounds = photoBoundsById[candidate.id] ?: cardBoundsById[candidate.id]
                    targetBounds?.let { candidate.id to it }
                }.toMap()
                SimilarFullScreenImageViewer(
                    items = session.items,
                    initialIndex = session.initialIndex,
                    isMuted = muteVideos,
                    sourceBoundsInRoot = session.sourceBoundsInRoot,
                    sourceBoundsByItemId = session.sourceBoundsByItemId,
                    dismissSourceBoundsByItemId = dismissSourceBoundsByItemId,
                    onSharedDismissStart = { sourceItemId ->
                        hiddenSourceItemId = sourceItemId
                    },
                    onDismiss = {
                        if (hiddenSourceItemId != null) {
                            localFullScreenState = null
                            scope.launch {
                                withFrameNanos { }
                                if (localFullScreenState == null) {
                                    hiddenSourceItemId = null
                                }
                            }
                        } else {
                            localFullScreenState = null
                            hiddenSourceItemId = null
                        }
                    }
                )
            }
        }

        splitCompareSession?.let { session ->
            SimilarSplitCompareView(
                items = session.items,
                selectedItemId = keptId,
                onSelectedItemChange = { selectedId ->
                    keptId = if (keptId == selectedId) null else selectedId
                },
                onDismiss = {
                    splitCompareSession = null
                },
                onKeepSelected = handleKeepSelected,
                onKeepAll = handleKeepAll,
                onDeleteAll = handleDeleteAll
            )
        }
    }
}
