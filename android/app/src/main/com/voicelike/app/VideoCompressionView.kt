package com.voicelike.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlin.math.roundToInt
import android.media.MediaMetadataRetriever

@Composable
fun VideoCompressionView(
    videos: List<MediaItem>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val resumeState = remember { mutableStateOf(prefs.videoCompressResume) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    val results = remember { mutableStateMapOf<Long, CompressionResult>() }
    val durationMap = remember { mutableStateMapOf<Long, Long>() }
    val compressionCounts = remember { mutableStateMapOf<Long, Int>() }
    val removedIds = remember { mutableStateListOf<Long>() }
    var thresholdMb by remember { mutableIntStateOf(50) }
    var isCompressing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var activeItem by remember { mutableStateOf<MediaItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resultSummary by remember { mutableStateOf<CompressionSummary?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var pendingDeleteIds by remember { mutableStateOf<List<Long>?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val currentPending = pendingDeleteIds
        if (result.resultCode == Activity.RESULT_OK && currentPending != null) {
            removedIds.addAll(currentPending)
            selectedIds.removeAll(currentPending.toSet())
            currentPending.forEach { results.remove(it) }
        }
        pendingDeleteIds = null
        isDeleting = false
        resultSummary = null
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
        MediaAnalysisManager.warmVideoThumbs(context, warmTargets)
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
            .background(Color(0xFFF3F4F6))
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
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = strings.videoCompressThresholdLabel ?: "Filter Threshold",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "≥ ${formatFileSize(thresholdMb * 1024L * 1024L)}",
                                    fontSize = 12.sp,
                                    color = Color.Black.copy(alpha = 0.7f)
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
                                        thumbColor = Color.Black,
                                        activeTrackColor = Color.Black,
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "0MB", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.5f))
                                    Text(text = "500MB", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.5f))
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
                            onDurationResolved = { durationMap[item.id] = it },
                            onToggle = {
                                if (isSelected) {
                                    selectedIds.remove(item.id)
                                } else {
                                    selectedIds.add(item.id)
                                }
                            }
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
                            val targetVideoBitrate = deriveTargetVideoBitrate(item.size, durationMs, selectedBitrate)
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
                            pendingDeleteIds = summary.items.map { it.id }
                            val uris = summary.items.map { it.originalUri }
                            val intentSender = MediaStore.createTrashRequest(context.contentResolver, uris, true).intentSender
                            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
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
    }
}

@Composable
private fun VideoCompressionHeader(
    title: String,
    onBack: () -> Unit,
    backLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White)
                .bounceClick(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = backLabel, tint = Color.Black)
        }
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun CompressionWarningCard(
    title: String,
    message: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB91C1C)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color(0xFF7F1D1D),
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
    if (options.isEmpty()) {
        return
    }
    val selected = options.firstOrNull { it.key == selectedKey } ?: options.first()
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${(selected.ratio * 100).toInt()}% · ≥${selected.minVideoBitrate / 1_000_000} Mbps / ${selected.audioBitrate / 1000} kbps",
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = option.key == selectedKey,
                        onClick = { onSelect(option) },
                        label = { Text(option.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Black,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF3F4F6),
                            labelColor = Color.Black
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(32.dp)
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
    onToggle: () -> Unit
) {
    val context = LocalContext.current
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
        onClick = { }, // Handle via bounceClick
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onToggle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE5E7EB))
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.uri)
                            .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                            .crossfade(true)
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
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = null,
                            tint = Color.White,
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
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    if (estimatedSize != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$estimateLabel ${formatFileSize(estimatedSize)}",
                            fontSize = 11.sp,
                            color = Color.Black.copy(alpha = 0.5f)
                        )
                    }
                    if (compressCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(countLabel, compressCount),
                            fontSize = 11.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF3B82F6) else Color(0xFFE5E7EB)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
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
    val ratio = if (item.originalSize > 0) {
        (item.outputSize.toFloat() / item.originalSize.toFloat()) * 100f
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
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
                    .background(Color(0xFFE5E7EB))
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.originalUri)
                    .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                    .crossfade(true)
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
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$beforeLabel ${formatFileSize(item.originalSize)} · $afterLabel ${formatFileSize(item.outputSize)}",
                fontSize = 12.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$ratioLabel ${ratio.toInt()}%",
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.7f)
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
        Text(text = text, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.6f))
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
                color = Color(0xFF3B82F6),
                trackColor = Color(0xFFE5E7EB)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (activeName.isNullOrBlank()) compressingLabel else "$compressingLabel · $activeName",
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.7f)
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = errorMessage, fontSize = 12.sp, color = Color(0xFFDC2626))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCompress,
            enabled = !isCompressing && enabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .bounceClick(onClick = onCompress)
        ) {
            Text(
                text = if (isCompressing) compressingLabel else actionLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                progressJob.cancel()
                onProgress(1f)
                continuation.resume(outputFile)
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
            outputFile.delete()
            onProgress(0f)
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        continuation.invokeOnCancellation {
            transformer.removeListener(listener)
            transformer.cancel()
            progressJob.cancel()
            outputFile.delete()
        }
    }
}

private suspend fun saveCompressedVideo(
    context: Context,
    tempFile: File,
    originalName: String,
    originalDateAdded: Long
): Uri? = withContext(Dispatchers.IO) {
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
            if (originalDateAdded > 0) {
                put(MediaStore.Video.Media.DATE_ADDED, originalDateAdded)
                put(MediaStore.Video.Media.DATE_MODIFIED, originalDateAdded)
            }
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
    val savedBytes = (summary.totalOriginal - summary.totalOutput).coerceAtLeast(0)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$savedLabel ${formatFileSize(savedBytes)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$countLabel ${summary.items.size}",
                        fontSize = 12.sp,
                        color = Color.Black.copy(alpha = 0.7f)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(onClick = onDelete)
            ) {
                Text(text = deleteLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onKeep,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(onClick = onKeep)
            ) {
                Text(text = keepLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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

private fun deriveTargetVideoBitrate(sizeBytes: Long, durationMs: Long?, option: BitrateOption): Int {
    val originalBitrate = estimateOriginalVideoBitrate(sizeBytes, durationMs)
    val target = if (originalBitrate != null) {
        (originalBitrate * option.ratio).toInt()
    } else {
        option.fallbackVideoBitrate
    }
    return target.coerceAtLeast(option.minVideoBitrate)
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

private data class BitrateOption(
    val key: String,
    val label: String,
    val ratio: Float,
    val minVideoBitrate: Int,
    val fallbackVideoBitrate: Int,
    val audioBitrate: Int
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
