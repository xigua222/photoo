package com.voicelike.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import androidx.compose.runtime.withFrameNanos
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlin.math.roundToInt
import android.media.MediaMetadataRetriever

private const val MEDIASTORE_URI_BATCH_LIMIT = 1800

internal object VideoCompressionSessionCache {
    private val durationById = ConcurrentHashMap<Long, Long>()
    private val sourceInfoById = ConcurrentHashMap<Long, CompressionVideoSourceInfo>()

    fun durationSnapshot(): Map<Long, Long> = durationById.toMap()

    fun sourceInfoSnapshot(): Map<Long, CompressionVideoSourceInfo> = sourceInfoById.toMap()

    fun updateDuration(mediaId: Long, durationMs: Long?) {
        if (durationMs != null && durationMs > 0L) {
            durationById[mediaId] = durationMs
        }
    }

    fun updateSourceInfo(mediaId: Long, sourceInfo: CompressionVideoSourceInfo?) {
        if (sourceInfo != null) {
            sourceInfoById[mediaId] = sourceInfo
        }
    }

    suspend fun prewarm(context: Context, items: List<MediaItem>, maxCount: Int = 12) {
        withContext(Dispatchers.IO) {
            items.asSequence()
                .filter { it.type == "video" }
                .take(maxCount)
                .forEach { item ->
                    if (!durationById.containsKey(item.id)) {
                        updateDuration(item.id, loadVideoDurationMs(context, item.uri))
                    }
                }
        }
    }
}

@Composable
fun VideoCompressionView(
    videos: List<MediaItem>,
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
    val resumeState = remember { mutableStateOf(prefs.videoCompressResume) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    val results = remember { mutableStateMapOf<Long, CompressionResult>() }
    val durationMap = remember {
        mutableStateMapOf<Long, Long>().apply {
            putAll(VideoCompressionSessionCache.durationSnapshot())
        }
    }
    val sourceInfoMap = remember {
        mutableStateMapOf<Long, CompressionVideoSourceInfo>().apply {
            putAll(VideoCompressionSessionCache.sourceInfoSnapshot())
        }
    }
    val compressionCounts = remember { mutableStateMapOf<Long, Int>() }
    val removedIds = remember { mutableStateListOf<Long>() }
    var thresholdMb by remember { mutableIntStateOf(50) }
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
    val bitrateOptions = listOf(
        BitrateOption("low", strings.videoCompressBitrateLow, 0.33f, 4_200_000, 5_000_000, 160_000),
        BitrateOption("balanced", strings.videoCompressBitrateBalanced, 0.42f, 5_200_000, 6_200_000, 160_000),
        BitrateOption("high", strings.videoCompressBitrateHigh, 0.55f, 7_000_000, 8_500_000, 192_000)
    )
    val initialBitrateKey = prefs.videoCompressBitrateKey
    var selectedBitrateKey by remember {
        mutableStateOf(bitrateOptions.firstOrNull { it.key == initialBitrateKey }?.key ?: "balanced")
    }
    val selectedBitrate = bitrateOptions.firstOrNull { it.key == selectedBitrateKey } ?: bitrateOptions[1]
    val targetAudioBitrate = selectedBitrate.audioBitrate
    val visibleVideos by remember(videos, removedIds, compressionCounts, durationMap, selectedBitrateKey, thresholdMb) {
        derivedStateOf {
            videos
                .filter { !removedIds.contains(it.id) }
                .filter { item ->
                    if (item.size < thresholdMb * 1024L * 1024L) return@filter false
                    
                    val durationMs = durationMap[item.id]
                    if (durationMs == null || durationMs <= 0) {
                        true
                    } else {
                        val originalBitrate = estimateOriginalVideoBitrate(item.size, durationMs)
                        if (originalBitrate == null) {
                            true
                        } else {
                            val targetVideoBitrate = deriveTargetVideoBitrate(item.size, durationMs, selectedBitrate)
                            targetVideoBitrate < originalBitrate
                        }
                    }
                }
                .sortedWith(
                    compareBy<MediaItem> { if ((compressionCounts[it.id] ?: 0) > 0) 1 else 0 }
                        .thenByDescending { it.dateAdded }
                        .thenByDescending { it.id }
                )
        }
    }
    val visibleIdSet = remember(visibleVideos) { visibleVideos.map { it.id }.toSet() }
    val hasVisibleSelection = selectedIds.any { it in visibleIdSet }
    val warmTargets = remember(visibleVideos) { visibleVideos.take(40) }

    LaunchedEffect(warmTargets) {
        launch {
            MediaAnalysisManager.warmVideoThumbs(context, warmTargets)
        }
        launch {
            VideoCompressionSessionCache.prewarm(context, warmTargets, maxCount = 16)
        }
    }
    LaunchedEffect(visibleVideos, resumeState.value.queueIds) {
        val visibleSet = visibleVideos.map { it.id }.toSet()
        val pendingIds = resumeState.value.queueIds.filter { it in visibleSet }
        if (pendingIds.isNotEmpty()) {
            selectedIds.clear()
            selectedIds.addAll(pendingIds)
            if (pendingIds.size != resumeState.value.queueIds.size) {
                val updated = resumeState.value.copy(queueIds = pendingIds)
                prefs.videoCompressResume = updated
                resumeState.value = updated
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            VideoCompressionHeader(
                title = strings.videoCompressTitle,
                onBack = onBack,
                backLabel = strings.back
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        CompressionWarningCard(
                            title = strings.videoCompressWarningTitle,
                            message = strings.videoCompressWarningBody
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CompressionTargetInfo(
                            label = strings.videoCompressTargetLabel,
                            options = bitrateOptions,
                            selectedKey = selectedBitrateKey,
                            onSelect = { option ->
                                selectedBitrateKey = option.key
                                prefs.videoCompressBitrateKey = option.key
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Threshold Slider
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
                                    text = strings.videoCompressThresholdLabel ?: "Filter Threshold",
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
                                        val rounded = value.roundToInt()
                                        // Snap to nearest 50
                                        val snapped = ((rounded + 25) / 50) * 50
                                        val clamped = snapped.coerceIn(0, 500)
                                        
                                        if (clamped != thresholdMb) {
                                            if (hapticsAllowed) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            thresholdMb = clamped
                                        }
                                    },
                                    valueRange = 0f..500f,
                                    steps = 9, // (500-0)/50 - 1 = 9 steps between 0 and 500
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
                                    Text(text = "0MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                    Text(text = "500MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (visibleVideos.isEmpty()) {
                    item {
                        EmptyCompressionState(text = strings.videoCompressSelectHint)
                    }
                } else {
                    items(visibleVideos, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        VideoCompressionItem(
                            item = item,
                            isSelected = isSelected,
                            estimateLabel = strings.videoCompressEstimate,
                            countLabel = strings.videoCompressTimes,
                            compressCount = compressionCounts[item.id] ?: 0,
                            videoBitrate = deriveTargetVideoBitrate(item.size, durationMap[item.id], selectedBitrate),
                            audioBitrate = targetAudioBitrate,
                            durationMs = durationMap[item.id],
                            onDurationResolved = {
                                durationMap[item.id] = it
                                VideoCompressionSessionCache.updateDuration(item.id, it)
                            },
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

            CompressionFooter(
                isCompressing = isCompressing,
                progress = progress,
                activeName = activeItem?.displayName?.ifBlank { strings.videoCompressTitle },
                errorMessage = errorMessage,
                actionLabel = strings.videoCompressAction,
                compressingLabel = strings.videoCompressing,
                enabled = hasVisibleSelection,
                onCompress = {
                    if (isCompressing || !hasVisibleSelection) return@CompressionFooter
                    if (hapticsAllowed) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    scope.launch {
                        isCompressing = true
                        errorMessage = null
                        val pendingQueue = resumeState.value.queueIds.filter { it in visibleIdSet }
                        val pendingSet = pendingQueue.toSet()
                        val selectionQueue = visibleVideos.filter { selectedIds.contains(it.id) }.map { it.id }
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
                        val itemMap = visibleVideos.associateBy { it.id }
                        val resumed = CompressionResumeState(queueIds = queueIds, doneIds = cleanedDoneSet.toList())
                        prefs.videoCompressResume = resumed
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
                            val durationMs = durationMap[item.id] ?: loadVideoDurationMs(context, item.uri)
                            val sourceInfo = sourceInfoMap[item.id] ?: loadCompressionVideoSourceInfo(context, item.uri)
                            if (sourceInfo != null) {
                                sourceInfoMap[item.id] = sourceInfo
                                VideoCompressionSessionCache.updateSourceInfo(item.id, sourceInfo)
                            }
                            val targetVideoBitrate = deriveTargetVideoBitrate(
                                sizeBytes = item.size,
                                durationMs = durationMs,
                                option = selectedBitrate,
                                sourceInfo = sourceInfo
                            )
                            val tempFile = compressToTempFile(
                                context = context,
                                item = item,
                                targetVideoBitrate = targetVideoBitrate,
                                onProgress = { p ->
                                    if (totalCount > 0) {
                                        val current = (baseIndex.toFloat() + p.coerceIn(0f, 1f)) / totalCount.toFloat()
                                        progress = current.coerceIn(0f, 1f)
                                    }
                                }
                            )
                            if (tempFile == null) {
                                errorMessage = strings.videoCompressError
                                return@forEachIndexed
                            }
                            val outputUri = withContext(Dispatchers.IO) {
                                saveCompressedVideo(context, tempFile, item.displayName, item.dateAdded)
                            }
                            val outputSize = tempFile.length()
                            tempFile.delete()
                            if (outputUri == null) {
                                errorMessage = strings.videoCompressError
                            } else {
                                compressionCounts[item.id] = (compressionCounts[item.id] ?: 0) + 1
                                results[item.id] = CompressionResult(
                                    originalSize = item.size,
                                    outputSize = outputSize,
                                    outputUri = outputUri
                                )
                                cleanedDoneSet.add(item.id)
                                val updatedResume = CompressionResumeState(queueIds = queueIds, doneIds = cleanedDoneSet.toList())
                                prefs.videoCompressResume = updatedResume
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
                            prefs.videoCompressResume = cleared
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
            VideoCompressionResultView(
                title = strings.videoCompressResultTitle,
                savedLabel = strings.videoCompressResultSaved,
                countLabel = strings.videoCompressResultCount,
                beforeLabel = strings.videoCompressBefore,
                afterLabel = strings.videoCompressAfter,
                ratioLabel = strings.videoCompressRatio,
                deleteLabel = strings.videoCompressDeleteOriginal,
                keepLabel = strings.videoCompressKeepOriginal,
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

@Composable
private fun VideoCompressionHeader(
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
fun CompressionWarningCard(
    title: String,
    message: String
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.errorContainer else Color(0xFFFEE2E2)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .advancedTextureCard(shape = RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFB91C1C)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = if (isDark) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f) else Color(0xFF7F1D1D),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun CompressionTargetInfo(
    label: String,
    options: List<BitrateOption>,
    selectedKey: String,
    onSelect: (BitrateOption) -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    if (options.isEmpty()) {
        return
    }
    val selected = options.firstOrNull { it.key == selectedKey } ?: options.first()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .advancedTextureCard(shape = RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isDark) Color(0xFFBDBDBD).copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
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
                text = "${(selected.ratio * 100).toInt()}% · ≥${selected.minVideoBitrate / 1_000_000} Mbps / ${selected.audioBitrate / 1000} kbps",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = option.key == selectedKey,
                        onClick = { onSelect(option) },
                        label = { Text(option.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f) else Color.Black,
                            selectedLabelColor = if (isDark) MaterialTheme.colorScheme.background else Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                            .bounceClick(onClick = { onSelect(option) })
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoCompressionItem(
    item: MediaItem,
    isSelected: Boolean,
    estimateLabel: String,
    countLabel: String,
    compressCount: Int,
    videoBitrate: Int,
    audioBitrate: Int,
    durationMs: Long?,
    onDurationResolved: (Long) -> Unit,
    onToggle: () -> Unit,
    onLongPreview: () -> Unit,
    onBoundsMeasured: (androidx.compose.ui.geometry.Rect) -> Unit,
    isSourceHidden: Boolean
) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    androidx.compose.runtime.LaunchedEffect(item.id) {
        if (durationMs == null) {
            val loaded = loadVideoDurationMs(context, item.uri) ?: -1L
            onDurationResolved(loaded)
        }
    }
    val estimatedSize = if (durationMs != null && durationMs > 0) {
        estimateCompressedSize(durationMs, videoBitrate, audioBitrate)
    } else {
        null
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .advancedTextureCard(shape = RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isDark) Color(0xFFBDBDBD).copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isDark) Color(0xFFBDBDBD).copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.uri)
                            .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                            .crossfade(false)
                            .size(256)
                            .memoryCacheKey("video_thumb_${item.id}_list")
                            .diskCacheKey("video_thumb_${item.id}_list")
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
                            .padding(6.dp)
                            .size(20.dp)
                            .background(
                                if (isDark) MaterialTheme.colorScheme.background.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = null,
                            tint = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName.ifBlank { "Video ${item.id}" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFileSize(item.size),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    if (estimatedSize != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$estimateLabel ${formatFileSize(estimatedSize)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    if (compressCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(countLabel, compressCount),
                            fontSize = 11.sp,
                            color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else Color(0xFF2563EB)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color(0xFF3B82F6)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompressionResultItem(
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
                    .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                    .crossfade(false)
                    .size(200)
                    .memoryCacheKey("video_thumb_${item.id}_result")
                    .diskCacheKey("video_thumb_${item.id}_result")
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
                text = item.displayName.ifBlank { "Video ${item.id}" },
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
fun EmptyCompressionState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@Composable
fun CompressionFooter(
    isCompressing: Boolean,
    progress: Float,
    activeName: String?,
    errorMessage: String?,
    actionLabel: String,
    compressingLabel: String,
    enabled: Boolean,
    onCompress: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (isCompressing) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color(0xFF3B82F6),
                trackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f) else Color(0xFFE5E7EB)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (activeName.isNullOrBlank()) compressingLabel else "$compressingLabel · $activeName",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = errorMessage, fontSize = 12.sp, color = if (isDark) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else Color(0xFFDC2626))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCompress,
            enabled = !isCompressing && enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color(0xFF3B82F6),
                contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .advancedTextureButton(shape = RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (isDark) Color(0xFFBDBDBD).copy(alpha = 0.6f) else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .bounceClick(onClick = onCompress)
        ) {
            Text(
                text = if (isCompressing) compressingLabel else actionLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
            )
        }
    }
}

@OptIn(UnstableApi::class)
private suspend fun compressToTempFile(
    context: Context,
    item: MediaItem,
    targetVideoBitrate: Int,
    onProgress: (Float) -> Unit
): File? = withContext(Dispatchers.Main) {
    context.cacheDir.mkdirs()
    val outputFile = File(context.cacheDir, "compress_${item.id}_${System.currentTimeMillis()}.mp4")
    val transformer = Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .setEncoderFactory(
            DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(targetVideoBitrate)
                        .build()
                )
                .build()
        )
        .build()
    suspendCancellableCoroutine<File?> { continuation ->
        val progressScope = CoroutineScope(Dispatchers.Main.immediate)
        val progressJob = progressScope.launch {
            var simulated = 0f
            while (continuation.isActive) {
                simulated = (simulated + 0.02f).coerceAtMost(0.9f)
                onProgress(simulated)
                delay(200)
            }
        }
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                transformer.removeListener(this)
                progressScope.launch {
                    progressJob.cancel()
                    val readyFile = waitForGeneratedVideoFile(outputFile)
                    onProgress(if (readyFile != null) 1f else 0f)
                    continuation.resume(readyFile)
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                transformer.removeListener(this)
                progressJob.cancel()
                outputFile.delete()
                onProgress(0f)
                continuation.resume(null)
            }
        }
        try {
            transformer.addListener(listener)
            transformer.start(androidx.media3.common.MediaItem.fromUri(item.uri), outputFile.absolutePath)
        } catch (e: Exception) {
            transformer.removeListener(listener)
            progressJob.cancel()
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { outputFile.delete() }
            onProgress(0f)
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        continuation.invokeOnCancellation {
            transformer.removeListener(listener)
            transformer.cancel()
            progressJob.cancel()
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { outputFile.delete() }
        }
    }
}

private suspend fun waitForGeneratedVideoFile(outputFile: File): File? {
    repeat(20) { attempt ->
        if (outputFile.exists() && outputFile.length() > 0L) {
            return outputFile
        }
        delay(if (attempt < 5) 40L else 80L)
    }
    runCatching { outputFile.delete() }
    return null
}

private suspend fun saveCompressedVideo(
    context: Context,
    tempFile: File,
    originalName: String,
    originalDateAdded: Long
): Uri? = withContext(Dispatchers.IO) {
    if (!tempFile.exists() || tempFile.length() <= 0L) return@withContext null
    val resolver = context.contentResolver
    val baseName = if (originalName.isBlank()) {
        "video_${System.currentTimeMillis()}"
    } else {
        originalName.substringBeforeLast(".")
    }
    val fileName = "${baseName}_compressed.mp4"
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (originalDateAdded > 0) {
            put(MediaStore.Video.Media.DATE_ADDED, originalDateAdded)
            put(MediaStore.Video.Media.DATE_MODIFIED, originalDateAdded)
            put(MediaStore.Video.Media.DATE_TAKEN, originalDateAdded * 1000)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Photoo")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
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
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }
        resolver.update(uri, pendingValues, null, null)
    }
    uri
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    if (bytes < 1024) return "$bytes B"
    val unit = 1024
    val exp = (kotlin.math.log(bytes.toDouble(), unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1]
    val value = bytes / unit.toDouble().pow(exp.toDouble())
    return String.format("%.1f %sB", value, pre)
}

@Composable
private fun VideoCompressionResultView(
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
                items(summary.items, key = { it.id }) { item ->
                    CompressionResultItem(
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
                    containerColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color(0xFF111827),
                    contentColor = if (isDark) MaterialTheme.colorScheme.background else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .advancedTextureButton(shape = RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (isDark) Color(0xFFBDBDBD).copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
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
                    .border(
                        1.dp,
                        if (isDark) Color(0xFFBDBDBD).copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .bounceClick(onClick = onKeep)
            ) {
                Text(text = keepLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        CelebrationConfetti()
    }
}

private fun estimateCompressedSize(durationMs: Long, videoBitrate: Int, audioBitrate: Int): Long {
    val totalBitrate = videoBitrate + audioBitrate
    val seconds = durationMs / 1000.0
    return ((totalBitrate * seconds) / 8.0).toLong().coerceAtLeast(1L)
}

private fun estimateOriginalVideoBitrate(sizeBytes: Long, durationMs: Long?): Int? {
    if (sizeBytes <= 0 || durationMs == null || durationMs <= 0) return null
    val seconds = durationMs / 1000.0
    if (seconds <= 0.0) return null
    val bitrate = ((sizeBytes * 8.0) / seconds).toInt()
    return bitrate.coerceAtLeast(100_000)
}

private fun deriveTargetVideoBitrate(
    sizeBytes: Long,
    durationMs: Long?,
    option: BitrateOption,
    sourceInfo: CompressionVideoSourceInfo? = null
): Int {
    val originalBitrate = estimateOriginalVideoBitrate(sizeBytes, durationMs)
    var target = if (originalBitrate != null) {
        (originalBitrate * option.ratio).toInt()
    } else {
        option.fallbackVideoBitrate
    }
    sourceInfo?.let { info ->
        val longEdge = maxOf(info.width, info.height)
        val resolutionFloor = when {
            longEdge >= 3840 -> when (option.key) {
                "low" -> 12_000_000
                "balanced" -> 15_000_000
                else -> 18_000_000
            }
            longEdge >= 2560 -> when (option.key) {
                "low" -> 8_000_000
                "balanced" -> 10_000_000
                else -> 12_000_000
            }
            longEdge >= 1920 -> when (option.key) {
                "low" -> 6_000_000
                "balanced" -> 8_000_000
                else -> 10_000_000
            }
            else -> option.minVideoBitrate
        }
        target = maxOf(target, resolutionFloor)
        if (info.mimeType in setOf("video/hevc", "video/h265", "video/x-h265")) {
            target = (target * 1.35f).roundToInt()
        }
        if (info.frameRate >= 50f) {
            target = (target * 1.2f).roundToInt()
        }
    }
    target = target.coerceAtLeast(option.minVideoBitrate)
    if (originalBitrate != null) {
        target = target.coerceAtMost((originalBitrate * 0.92f).roundToInt().coerceAtLeast(option.minVideoBitrate))
    }
    return target
}

private suspend fun loadVideoDurationMs(context: Context, uri: Uri): Long? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private suspend fun loadCompressionVideoSourceInfo(
    context: Context,
    uri: Uri
): CompressionVideoSourceInfo? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        CompressionVideoSourceInfo(
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
            frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 0f,
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.lowercase().orEmpty()
        )
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private data class BitrateOption(
    val key: String,
    val label: String,
    val ratio: Float,
    val minVideoBitrate: Int,
    val fallbackVideoBitrate: Int,
    val audioBitrate: Int
)

internal data class CompressionVideoSourceInfo(
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val mimeType: String
)

data class CompressionSummaryItem(
    val id: Long,
    val displayName: String,
    val originalUri: Uri,
    val originalSize: Long,
    val outputSize: Long
)

data class CompressionSummary(
    val items: List<CompressionSummaryItem>,
    val totalOriginal: Long,
    val totalOutput: Long
)

data class CompressionResult(
    val originalSize: Long,
    val outputSize: Long,
    val outputUri: Uri
)
