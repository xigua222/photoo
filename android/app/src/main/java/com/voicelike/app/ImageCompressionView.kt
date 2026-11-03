package com.voicelike.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.withFrameNanos
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private const val MEDIASTORE_URI_BATCH_LIMIT = 1800

data class ImageQualityOption(val key: String, val label: String, val maxSizePx: Int, val quality: Int)

@Composable
fun ImageCompressionView(
    photos: List<MediaItem>,
    onBack: () -> Unit,
    deletionMode: String = "recycle_bin",
    onMediaDeleted: (List<Long>) -> Unit = {}
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val resumeState = remember { mutableStateOf(prefs.imageCompressResume) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    val results = remember { mutableStateMapOf<Long, CompressionResult>() }
    val compressionCounts = remember { mutableStateMapOf<Long, Int>() }
    val removedIds = remember { mutableStateListOf<Long>() }
    var isCompressing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var activeItem by remember { mutableStateOf<MediaItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resultSummary by remember { mutableStateOf<CompressionSummary?>(null) }
    var previewItem by remember { mutableStateOf<MediaItem?>(null) }
    val previewSourceBoundsById = remember { mutableStateMapOf<Long, androidx.compose.ui.geometry.Rect>() }
    var hiddenPreviewSourceId by remember { mutableStateOf<Long?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var pendingDeleteBatch by remember { mutableStateOf<List<CompressionSummaryItem>?>(null) }
    var pendingDeleteBatches by remember { mutableStateOf<List<List<CompressionSummaryItem>>>(emptyList()) }
    var deleteLaunchToken by remember { mutableIntStateOf(0) }
    var thresholdMb by remember { mutableIntStateOf(prefs.imageCompressThresholdMb.coerceIn(3, 10)) }
    val minBytes = thresholdMb * 1024L * 1024L
    val eligiblePhotos = remember(photos) {
        photos.filter(::shouldAnalyzeAsStandardPhoto)
    }
    val qualityOptions = remember(strings) {
        listOf(
            ImageQualityOption("small", "S", 1280, 72),
            ImageQualityOption("balanced", "M", 1920, 82),
            ImageQualityOption("sharp", "L", 2560, 90),
            ImageQualityOption("original", strings.imageCompressOriginalLabel, Int.MAX_VALUE, 85)
        )
    }
    var selectedQualityKey by remember { mutableStateOf(prefs.imageCompressQualityKey) }
    val selectedQuality = remember(selectedQualityKey, qualityOptions) {
        qualityOptions.firstOrNull { it.key == selectedQualityKey } ?: qualityOptions[1]
    }
    val visiblePhotos by remember(eligiblePhotos, removedIds, minBytes, thresholdMb) {
        derivedStateOf {
            eligiblePhotos
                .filter {
                    val effectiveSize = MediaAnalysisManager.getEffectiveSize(it)
                    effectiveSize >= minBytes && !removedIds.contains(it.id)
                }
        }
    }
    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val groupedByMonth by remember(visiblePhotos) {
        derivedStateOf {
            visiblePhotos.groupBy { monthFormat.format(Date(it.dateAdded * 1000)) }
                .toSortedMap(compareByDescending { it })
        }
    }
    LaunchedEffect(visiblePhotos, resumeState.value.queueIds) {
        val visibleIdSet = visiblePhotos.map { it.id }.toSet()
        val pendingIds = resumeState.value.queueIds.filter { it in visibleIdSet }
        if (pendingIds.isNotEmpty()) {
            selectedIds.clear()
            selectedIds.addAll(pendingIds)
            if (pendingIds.size != resumeState.value.queueIds.size) {
                val updated = resumeState.value.copy(queueIds = pendingIds)
                prefs.imageCompressResume = updated
                resumeState.value = updated
            }
        }
    }
    val gridState = rememberLazyGridState()
    val dragVisitedIds = remember { mutableSetOf<Long>() }
    var dragSelectMode by remember { mutableStateOf(true) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val currentBatch = pendingDeleteBatch
        if (result.resultCode == Activity.RESULT_OK && currentBatch != null) {
            val currentIds = currentBatch.map { it.id }
            removedIds.addAll(currentIds)
            selectedIds.removeAll(currentIds.toSet())
            currentIds.forEach { results.remove(it) }
            onMediaDeleted(currentIds)
            if (pendingDeleteBatches.isNotEmpty()) {
                pendingDeleteBatch = pendingDeleteBatches.first()
                pendingDeleteBatches = pendingDeleteBatches.drop(1)
                deleteLaunchToken++
                return@rememberLauncherForActivityResult
            }
        }
        pendingDeleteBatch = null
        pendingDeleteBatches = emptyList()
        isDeleting = false
        resultSummary = null
    }
    LaunchedEffect(deleteLaunchToken, pendingDeleteBatch, isDeleting, deletionMode) {
        val batch = pendingDeleteBatch
        if (!isDeleting || batch.isNullOrEmpty()) return@LaunchedEffect
        val uris = batch.map { it.originalUri }
        try {
            val intentSender = if (deletionMode == "recycle_bin") {
                MediaStore.createTrashRequest(context.contentResolver, uris, true).intentSender
            } else {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            }
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        } catch (e: Exception) {
            e.printStackTrace()
            pendingDeleteBatch = null
            pendingDeleteBatches = emptyList()
            isDeleting = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ImageCompressionHeader(
                title = strings.imageCompressTitle,
                onBack = onBack,
                backLabel = strings.back
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(gridState, selectedIds) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragVisitedIds.clear()
                                val id = findGridItemIdAtOffset(gridState, offset)
                                if (id != null) {
                                    dragSelectMode = !selectedIds.contains(id)
                                    if (dragSelectMode) {
                                        if (!selectedIds.contains(id)) selectedIds.add(id)
                                    } else {
                                        selectedIds.remove(id)
                                    }
                                    dragVisitedIds.add(id)
                                }
                            },
                            onDrag = { change, _ ->
                                val id = findGridItemIdAtOffset(gridState, change.position)
                                if (id != null && !dragVisitedIds.contains(id)) {
                                    if (dragSelectMode) {
                                        if (!selectedIds.contains(id)) selectedIds.add(id)
                                    } else {
                                        selectedIds.remove(id)
                                    }
                                    dragVisitedIds.add(id)
                                }
                                change.consume()
                            },
                            onDragEnd = { dragVisitedIds.clear() },
                            onDragCancel = { dragVisitedIds.clear() }
                        )
                    },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                    ) {
                        CompressionWarningCard(
                            title = strings.imageCompressWarningTitle,
                            message = strings.imageCompressWarningBody
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ImageCompressionQualityCard(
                            selected = selectedQuality,
                            options = qualityOptions,
                            onSelect = { option ->
                            selectedQualityKey = option.key
                            prefs.imageCompressQualityKey = option.key
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ImageCompressionThresholdCard(
                            label = strings.imageCompressThresholdLabel,
                            thresholdMb = thresholdMb,
                            onThresholdChange = { value ->
                                thresholdMb = value
                                prefs.imageCompressThresholdMb = value
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (visiblePhotos.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            EmptyCompressionState(text = strings.imageCompressSelectHint)
                        }
                    }
                } else {
                    groupedByMonth.forEach { (month, itemsInMonth) ->
                        val monthSelectedCount = itemsInMonth.count { selectedIds.contains(it.id) }
                        val allSelected = monthSelectedCount == itemsInMonth.size && itemsInMonth.isNotEmpty()
                        item(
                            key = "month_header_$month",
                            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = month,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = allSelected,
                                    onClick = {
                                        if (allSelected) {
                                            selectedIds.removeAll(itemsInMonth.map { it.id }.toSet())
                                        } else {
                                            itemsInMonth.forEach { if (!selectedIds.contains(it.id)) selectedIds.add(it.id) }
                                        }
                                    },
                                    label = { Text(if (allSelected) strings.imageCompressSelected else strings.imageCompressSelectAll, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                                        selectedLabelColor = if (isDark) Color.Black else Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onBackground
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.advancedTextureButton(shape = RoundedCornerShape(20.dp))
                                )
                            }
                        }
                        items(itemsInMonth, key = { it.id }) { item ->
                            val isSelected = selectedIds.contains(item.id)
                            val displaySize = MediaAnalysisManager.getEffectiveSize(item)
                            ImageCompressionGridItem(
                                item = item,
                                isSelected = isSelected,
                                displaySize = displaySize,
                                onToggle = {
                                    if (isSelected) {
                                        selectedIds.remove(item.id)
                                    } else {
                                        selectedIds.add(item.id)
                                    }
                                },
                                onLongPreview = {
                                    if (hapticsAllowed) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    previewItem = item
                                    hiddenPreviewSourceId = null
                                },
                                onBoundsMeasured = { bounds ->
                                    previewSourceBoundsById[item.id] = bounds
                                },
                                isSourceHidden = hiddenPreviewSourceId == item.id
                            )
                        }
                    }
                }
            }

            CompressionFooter(
                isCompressing = isCompressing,
                progress = progress,
                activeName = activeItem?.displayName?.ifBlank { strings.imageCompressTitle },
                errorMessage = errorMessage,
                actionLabel = strings.imageCompressAction,
                compressingLabel = strings.imageCompressing,
                enabled = selectedIds.isNotEmpty(),
                onCompress = {
                    if (isCompressing || selectedIds.isEmpty()) return@CompressionFooter
                    if (hapticsAllowed) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    scope.launch {
                        isCompressing = true
                        errorMessage = null
                        val visibleIdSet = visiblePhotos.map { it.id }.toSet()
                        val pendingQueue = resumeState.value.queueIds.filter { it in visibleIdSet }
                        val pendingSet = pendingQueue.toSet()
                        val selectionQueue = visiblePhotos.filter { selectedIds.contains(it.id) }.map { it.id }
                        val selectionSet = selectionQueue.toSet()
                        val usePending = pendingQueue.isNotEmpty() && (selectionSet.isEmpty() || selectionSet.intersect(pendingSet).isNotEmpty())
                        val queueIds = if (usePending) pendingQueue else selectionQueue
                        if (queueIds.isEmpty()) {
                            isCompressing = false
                            return@launch
                        }
                        val doneSet = if (usePending) resumeState.value.doneIds.toMutableSet() else mutableSetOf()
                        val cleanedDoneSet = doneSet.filter { it in queueIds }.toMutableSet()
                        val totalCount = queueIds.size
                        val itemMap = visiblePhotos.associateBy { it.id }
                        val resumed = CompressionResumeState(queueIds = queueIds, doneIds = cleanedDoneSet.toList())
                        prefs.imageCompressResume = resumed
                        resumeState.value = resumed
                        val successResults = mutableListOf<CompressionSummaryItem>()
                        queueIds.forEachIndexed { _, id ->
                            if (cleanedDoneSet.contains(id)) {
                                if (totalCount > 0) {
                                    progress = cleanedDoneSet.size.toFloat() / totalCount.toFloat()
                                }
                                return@forEachIndexed
                            }
                            val item = itemMap[id] ?: return@forEachIndexed
                            activeItem = item
                            if (totalCount > 0) {
                                progress = cleanedDoneSet.size.toFloat() / totalCount.toFloat()
                            }
                            val baseIndex = cleanedDoneSet.size
                            val tempFile = compressImageToTempFile(
                                context = context,
                                item = item,
                                maxSizePx = selectedQuality.maxSizePx,
                                quality = selectedQuality.quality,
                                onProgress = { p ->
                                    if (totalCount > 0) {
                                        val current = (baseIndex.toFloat() + p.coerceIn(0f, 1f)) / totalCount.toFloat()
                                        progress = current.coerceIn(0f, 1f)
                                    }
                                }
                            )
                            if (tempFile == null) {
                                errorMessage = strings.imageCompressError
                                return@forEachIndexed
                            }
                            val (outputUri, outputSize) = withContext(Dispatchers.IO) {
                                val uri = saveCompressedImage(context, tempFile, item.displayName, item.dateAdded, item.uri)
                                val size = tempFile.length()
                                tempFile.delete()
                                Pair(uri, size)
                            }
                            if (outputUri == null) {
                                errorMessage = strings.imageCompressError
                            } else {
                                compressionCounts[item.id] = (compressionCounts[item.id] ?: 0) + 1
                                results[item.id] = CompressionResult(
                                    originalSize = item.size,
                                    outputSize = outputSize,
                                    outputUri = outputUri
                                )
                                cleanedDoneSet.add(item.id)
                                val updatedResume = CompressionResumeState(queueIds = queueIds, doneIds = cleanedDoneSet.toList())
                                prefs.imageCompressResume = updatedResume
                                resumeState.value = updatedResume
                                successResults.add(
                                    CompressionSummaryItem(
                                        id = item.id,
                                        displayName = item.displayName,
                                        originalUri = item.uri,
                                        originalSize = item.size,
                                        outputSize = outputSize
                                    )
                                )
                            }
                        }
                        activeItem = null
                        isCompressing = false
                        if (cleanedDoneSet.size == queueIds.size) {
                            val cleared = CompressionResumeState()
                            prefs.imageCompressResume = cleared
                            resumeState.value = cleared
                        }
                        if (successResults.isNotEmpty()) {
                            val totalOriginal = successResults.sumOf { it.originalSize }
                            val totalOutput = successResults.sumOf { it.outputSize }
                            val savedBytes = (totalOriginal - totalOutput).coerceAtLeast(0)
                            if (savedBytes > 0) {
                                val updatedStats = prefs.stats.copy(savedSize = prefs.stats.savedSize + savedBytes)
                                prefs.stats = updatedStats
                            }
                            resultSummary = CompressionSummary(
                                items = successResults,
                                totalOriginal = totalOriginal,
                                totalOutput = totalOutput
                            )
                            selectedIds.removeAll(successResults.map { it.id }.toSet())
                        }
                    }
                }
            )
        }

        val summary = resultSummary
        if (summary != null) {
            val deleteOriginals = {
                if (!isDeleting) {
                    isDeleting = true
                    scope.launch {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val batches = summary.items.chunked(MEDIASTORE_URI_BATCH_LIMIT)
                            val firstBatch = batches.firstOrNull()
                            if (firstBatch != null) {
                                pendingDeleteBatch = firstBatch
                                pendingDeleteBatches = batches.drop(1)
                                deleteLaunchToken++
                            } else {
                                isDeleting = false
                                resultSummary = null
                            }
                        } else {
                            withContext(Dispatchers.IO) {
                                summary.items.forEach { item ->
                                    context.contentResolver.delete(item.originalUri, null, null)
                                }
                            }
                            val ids = summary.items.map { it.id }
                            removedIds.addAll(ids)
                            selectedIds.removeAll(ids.toSet())
                            ids.forEach { results.remove(it) }
                            isDeleting = false
                            resultSummary = null
                        }
                    }
                }
            }
            ImageCompressionResultView(
                title = strings.imageCompressResultTitle,
                savedLabel = strings.imageCompressResultSaved,
                countLabel = strings.imageCompressResultCount,
                beforeLabel = strings.imageCompressBefore,
                afterLabel = strings.imageCompressAfter,
                ratioLabel = strings.imageCompressRatio,
                deleteLabel = strings.imageCompressDeleteOriginal,
                keepLabel = strings.imageCompressKeepOriginal,
                summary = summary,
                isDeleting = isDeleting,
                onKeep = { resultSummary = null },
                onDelete = deleteOriginals
            )
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

private fun findGridItemIdAtOffset(state: LazyGridState, position: Offset): Long? {
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
        val left = item.offset.x
        val top = item.offset.y
        val right = left + item.size.width
        val bottom = top + item.size.height
        position.x >= left && position.x <= right && position.y >= top && position.y <= bottom
    } ?: return null
    val key = info.key
    return if (key is Long) key else null
}

@Composable
private fun ImageCompressionHeader(
    title: String,
    onBack: () -> Unit,
    backLabel: String
) {
    UnifiedSubpageTopBar(
        title = title,
        onBack = onBack,
        backContentDescription = backLabel
    )
}

@Composable
private fun ImageCompressionThresholdCard(
    label: String,
    thresholdMb: Int,
    onThresholdChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .advancedTextureCard(shape = RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "≥ ${formatFileSize(thresholdMb * 1024L * 1024L)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = thresholdMb.toFloat(),
                onValueChange = { value ->
                    val rounded = value.roundToInt().coerceIn(3, 10)
                    if (rounded != thresholdMb) {
                        if (hapticsAllowed) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onThresholdChange(rounded)
                    }
                },
                valueRange = 3f..10f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                    activeTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                    inactiveTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "3MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Text(text = "10MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun ImageCompressionGridItem(
    item: MediaItem,
    isSelected: Boolean,
    displaySize: Long,
    onToggle: () -> Unit,
    onLongPreview: () -> Unit,
    onBoundsMeasured: (androidx.compose.ui.geometry.Rect) -> Unit,
    isSourceHidden: Boolean
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(false)
                .size(300)
                .memoryCacheKey("image_thumb_${item.id}_grid")
                .diskCacheKey("image_thumb_${item.id}_grid")
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .allowHardware(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    if (isDark) MaterialTheme.colorScheme.background.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = formatFileSize(displaySize),
                fontSize = 9.sp,
                color = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(0xFF3B82F6) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageCompressionResultItem(
    item: CompressionSummaryItem,
    ratioLabel: String,
    beforeLabel: String,
    afterLabel: String
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val ratio = if (item.originalSize > 0) {
        (item.outputSize.toFloat() / item.originalSize.toFloat()) * 100f
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.originalUri)
                    .crossfade(false)
                    .size(200)
                    .memoryCacheKey("image_thumb_${item.id}_result")
                    .diskCacheKey("image_thumb_${item.id}_result")
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName.ifBlank { "Image ${item.id}" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$beforeLabel ${formatFileSize(item.originalSize)} · $afterLabel ${formatFileSize(item.outputSize)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$ratioLabel ${ratio.toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ImageCompressionResultView(
    title: String,
    savedLabel: String,
    countLabel: String,
    beforeLabel: String,
    afterLabel: String,
    ratioLabel: String,
    deleteLabel: String,
    keepLabel: String,
    summary: CompressionSummary,
    isDeleting: Boolean,
    onKeep: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val savedBytes = (summary.totalOriginal - summary.totalOutput).coerceAtLeast(0)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .advancedTextureCard(shape = RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$savedLabel ${formatFileSize(savedBytes)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$countLabel ${summary.items.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                    items(summary.items.size, key = { summary.items[it].id }) { index ->
                        val item = summary.items[index]
                    ImageCompressionResultItem(
                        item = item,
                        ratioLabel = ratioLabel,
                        beforeLabel = beforeLabel,
                        afterLabel = afterLabel
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDelete,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF111827),
                    contentColor = if (isDark) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .advancedTextureButton(shape = RoundedCornerShape(16.dp))
                    .bounceClick(onClick = onDelete)
            ) {
                Text(text = deleteLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onKeep,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .advancedTextureButton(shape = RoundedCornerShape(16.dp))
                    .bounceClick(onClick = onKeep)
            ) {
                Text(text = keepLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        CelebrationConfetti()
    }
}

private suspend fun compressImageToTempFile(
    context: Context,
    item: MediaItem,
    maxSizePx: Int,
    quality: Int,
    onProgress: (Float) -> Unit
): File? = withContext(Dispatchers.IO) {
    var bitmap: Bitmap? = null
    var rotatedBitmap: Bitmap? = null
    var scaledBitmap: Bitmap? = null
    try {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(item.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) return@withContext null
        val sampleSize = calculateInSampleSize(width, height, maxSizePx)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        bitmap = resolver.openInputStream(item.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return@withContext null
        onProgress(0.3f)
        rotatedBitmap = rotateBitmapIfNeeded(context, item.uri, bitmap!!)
        scaledBitmap = if (rotatedBitmap!!.width > maxSizePx || rotatedBitmap!!.height > maxSizePx) {
            val scale = maxSizePx.toFloat() / max(rotatedBitmap!!.width, rotatedBitmap!!.height).toFloat()
            Bitmap.createScaledBitmap(
                rotatedBitmap!!,
                (rotatedBitmap!!.width * scale).roundToInt(),
                (rotatedBitmap!!.height * scale).roundToInt(),
                true
            )
        } else {
            rotatedBitmap
        }
        onProgress(0.7f)
        val outputFile = File(context.cacheDir, "image_compress_${item.id}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 95), out)
        }
        onProgress(1f)
        outputFile
    } catch (_: OutOfMemoryError) {
        runCatching { System.gc() }
        null
    } catch (_: Exception) {
        null
    } finally {
        if (scaledBitmap != null && scaledBitmap !== rotatedBitmap && scaledBitmap !== bitmap) {
            scaledBitmap?.recycle()
        }
        if (rotatedBitmap != null && rotatedBitmap !== bitmap) {
            rotatedBitmap?.recycle()
        }
        bitmap?.recycle()
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxSizePx: Int): Int {
    var inSampleSize = 1
    if (height > maxSizePx || width > maxSizePx) {
        var halfHeight = height / 2
        var halfWidth = width / 2
        while (halfHeight / inSampleSize >= maxSizePx && halfWidth / inSampleSize >= maxSizePx) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun rotateBitmapIfNeeded(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    val orientation = openExifInterface(context.contentResolver, uri)
        ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ?: ExifInterface.ORIENTATION_NORMAL
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    return rotated
}

private val EXIF_COPY_EXCLUDED_TAGS = setOf(
    // XMP carries motion-photo markers; the compressed copy has no video trailer.
    ExifInterface.TAG_XMP,
    ExifInterface.TAG_ORIENTATION,
    ExifInterface.TAG_IMAGE_WIDTH,
    ExifInterface.TAG_IMAGE_LENGTH,
    ExifInterface.TAG_PIXEL_X_DIMENSION,
    ExifInterface.TAG_PIXEL_Y_DIMENSION,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
    ExifInterface.TAG_STRIP_OFFSETS,
    ExifInterface.TAG_STRIP_BYTE_COUNTS,
    ExifInterface.TAG_ROWS_PER_STRIP
)

private val EXIF_COPY_TAGS: List<String> by lazy {
    ExifInterface::class.java.fields
        .filter { field ->
            field.type == String::class.java &&
                field.name.startsWith("TAG_")
        }
        .mapNotNull { field ->
            runCatching { field.get(null) as? String }.getOrNull()
        }
        .filterNot { it in EXIF_COPY_EXCLUDED_TAGS }
        .distinct()
}

private fun copyExifMetadata(source: ExifInterface?, target: ExifInterface) {
    if (source == null) return
    EXIF_COPY_TAGS.forEach { tag ->
        val value = source.getAttribute(tag) ?: return@forEach
        runCatching { target.setAttribute(tag, value) }
    }
    // Pixel data is physically rotated before saving, so orientation must be normalized.
    target.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
}

private fun openExifInterface(
    resolver: android.content.ContentResolver,
    uri: Uri
): ExifInterface? {
    return runCatching {
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor)
        }
    }.getOrNull() ?: runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream)
        }
    }.getOrNull()
}

private suspend fun saveCompressedImage(
    context: Context,
    tempFile: File,
    originalName: String,
    originalDateAdded: Long,
    originalUri: Uri
): Uri? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val baseName = if (originalName.isBlank()) {
        "image_${System.currentTimeMillis()}"
    } else {
        originalName.substringBeforeLast(".")
    }
    val fileName = "${baseName}_compressed.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (originalDateAdded > 0) {
            put(MediaStore.Images.Media.DATE_ADDED, originalDateAdded)
            put(MediaStore.Images.Media.DATE_MODIFIED, originalDateAdded)
            put(MediaStore.Images.Media.DATE_TAKEN, originalDateAdded * 1000)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Photoo")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    // Metadata goes into the plain temp file first: editing EXIF through a pending MediaStore
    // descriptor fails on many devices. The source is opened as the original so GPS isn't redacted.
    runCatching {
        val originalExif = openExifInputStream(context, originalUri)?.use { ExifInterface(it) }
        val targetExif = ExifInterface(tempFile.absolutePath)
        copyExifMetadata(originalExif, targetExif)
        targetExif.saveAttributes()
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
    val outputStream = resolver.openOutputStream(uri)
    if (outputStream == null) {
        resolver.delete(uri, null, null)
        return@withContext null
    }
    outputStream.use { out ->
        tempFile.inputStream().use { input ->
            input.copyTo(out)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val pendingValues = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(uri, pendingValues, null, null)
    }
    uri
}

@Composable
private fun ImageCompressionQualityCard(
    selected: ImageQualityOption,
    options: List<ImageQualityOption>,
    onSelect: (ImageQualityOption) -> Unit
) {
    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .advancedTextureCard(shape = RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (selected.key == "original") "${strings.imageCompressOriginalSize} · ${selected.quality}%" else "${selected.maxSizePx}px · ${selected.quality}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = option.key == selected.key,
                        onClick = { onSelect(option) },
                        label = { Text(option.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                            selectedLabelColor = if (isDark) Color.Black else Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.advancedTextureButton(shape = RoundedCornerShape(20.dp))
                    )
                }
            }
        }
    }
}
