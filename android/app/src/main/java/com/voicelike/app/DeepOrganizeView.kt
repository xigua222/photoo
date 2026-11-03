package com.voicelike.app

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Check

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DeepOrganizeView(
    allMedia: List<MediaItem>, // Added parameter
    onBack: () -> Unit,
    onSimilarClick: () -> Unit,
    onLowQualityClick: () -> Unit,
    onVideoCompressClick: () -> Unit,
    onImageCompressClick: () -> Unit,
    isScanPaused: Boolean,
    onToggleScanPause: () -> Unit,
    onRescanClick: () -> Unit,
    totalMediaCount: Int = 0
) {
    val catalogScanState by CatalogScanCoordinator.state.collectAsState()
    val isScanning = catalogScanState.isRunning
    val progress = catalogScanState.overallProgress
    val scanRatePerMinute by MediaAnalysisManager.scanRatePerMinute.collectAsState()
    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    var showRescanConfirm by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    val similarGroups by SimilarPhotoManager.foundGroups.collectAsState()
    val normalizedSimilarGroups = remember(similarGroups) {
        normalizeSimilarGroupsForDisplay(similarGroups)
    }
    val similarCount = remember(normalizedSimilarGroups) {
        normalizedSimilarGroups.size
    }
    val similarPreviewTargets = remember(normalizedSimilarGroups) {
        normalizedSimilarGroups.firstOrNull()?.take(2).orEmpty()
    }
    LaunchedEffect(similarPreviewTargets) {
        if (similarPreviewTargets.isEmpty()) return@LaunchedEffect
        prefetchSimilarCompareMedia(context, similarPreviewTargets)
    }

    val lowQualityItems by LowQualityManager.foundItems.collectAsState()
    val normalizedLowQualityItems = remember(lowQualityItems) {
        lowQualityItems.filter { shouldAnalyzeAsStandardPhoto(it.first) }
    }
    val lowQualityCount = remember(normalizedLowQualityItems) { normalizedLowQualityItems.size }
    val isAnyScanRunning = catalogScanState.isRunning
    val isPreparingScan = !catalogScanState.isRunning && catalogScanState.signature == 0L && progress <= 0f
    val isDeepOrganizeComplete = remember(progress, isScanning) {
        progress >= 0.999f && !isScanning
    }

    // Calculate percentage text
    val percent = when {
        progress >= 0.999f -> 100
        progress > 0f -> (progress * 100f).roundToInt().coerceAtLeast(1)
        else -> 0
    }
    val totalPhotos = remember(allMedia) { allMedia.count { it.type == "photo" } }
    val remainingPhotos = max(0, totalPhotos - (progress * totalPhotos).toInt())
    val estimatedFromRate = if (scanRatePerMinute > 0f) {
        max(1, ceil(remainingPhotos / scanRatePerMinute).toInt())
    } else {
        max(1, ceil(remainingPhotos / 2000.0).toInt())
    }
    
    // Dynamic Description
    val rawDesc = strings.scanningDescriptionDetailed
        ?: "This may take about %d minutes depending on your gallery size. You can exit and let it run in the background."
    val desc = when {
        rawDesc.contains("%d") -> rawDesc.format(estimatedFromRate)
        rawDesc.contains("1-3") -> rawDesc.replace("1-3", "$estimatedFromRate")
        rawDesc.contains("1~3") -> rawDesc.replace("1~3", "$estimatedFromRate")
        rawDesc.contains("1銆?") -> rawDesc.replace("1銆?", "$estimatedFromRate")
        rawDesc.contains("1锝?") -> rawDesc.replace("1锝?", "$estimatedFromRate")
        else -> rawDesc
    }
    val scanActionContentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    val scanActionIconSize = 18.dp
    val scanActionTextSize = 13.sp
    val scanActionItemSpacing = 6.dp

    if (showRescanConfirm) {
        AlertDialog(
            onDismissRequest = { showRescanConfirm = false },
            title = { Text(strings.rescan) },
            text = { Text(strings.rescanConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRescanConfirm = false
                        onRescanClick()
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = strings.deepOrganizeTitle ?: "Deep Organize",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Progress Block ---
            // Using a Gradient Background to match the style of feature cards
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent), // Transparent to show gradient
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .then(staggeredRevealModifier(0)) // Clip for background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1F2937)) // Cool Dark Gray (Slate 800 style) - Professional & Modern
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    isPreparingScan -> strings.scanningWait ?: "Preparing gallery scan..."
                                    isScanning && isScanPaused -> strings.scanPaused
                                    isScanning || !isDeepOrganizeComplete -> strings.deepOrganizeScanning ?: "Scanning Gallery..."
                                    else -> strings.deepOrganizeComplete ?: "Scan Complete"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White // White text on gradient
                            )
                            
                            Text(
                                text = "${percent}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                        
                        if (isPreparingScan || isScanning || !isDeepOrganizeComplete) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isPreparingScan) {
                                    strings.scanningWait ?: "Preparing gallery scan..."
                                } else if (isScanPaused) {
                                    strings.scanningWait ?: "Scanning is paused. You can continue at any time."
                                } else {
                                    desc
                                },
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val canToggleScanPause = isAnyScanRunning
                            OutlinedButton(
                                onClick = onToggleScanPause,
                                enabled = canToggleScanPause,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White.copy(alpha = 0.42f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = if (canToggleScanPause) 0.45f else 0.18f)
                                ),
                                contentPadding = scanActionContentPadding,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isScanPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(scanActionIconSize)
                                )
                                Spacer(modifier = Modifier.width(scanActionItemSpacing))
                                Text(
                                    text = if (isScanPaused) strings.continueOrganizing else strings.stop,
                                    fontSize = scanActionTextSize,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { showRescanConfirm = true },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF111827)
                                ),
                                contentPadding = scanActionContentPadding,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(scanActionIconSize)
                                )
                                Spacer(modifier = Modifier.width(scanActionItemSpacing))
                                Text(
                                    text = strings.rescan,
                                    fontSize = scanActionTextSize,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            
            // --- Feature Grid ---
            // Removed "Tools" title per user request
            
            // Similar Photos
            FeatureCard(
                title = strings.similar ?: "Similar Photos",
                subtitle = strings.similarPhotosSubtitle ?: "Find and clean duplicate shots",
                backgroundColor = if (isDark) Color(0xFF45B7D1).copy(alpha = 0.6f) else Color(0xFF45B7D1), // Blue
                titleColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                count = similarCount,
                onClick = onSimilarClick,
                modifier = staggeredRevealModifier(1)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Low Quality
            FeatureCard(
                title = strings.lowQuality ?: "Low Quality",
                subtitle = strings.lowQualitySubtitle ?: "Blurry, dark, and bad photos",
                backgroundColor = if (isDark) Color(0xFFFF6B6B).copy(alpha = 0.6f) else Color(0xFFFF6B6B), // Red
                titleColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                count = lowQualityCount,
                onClick = onLowQualityClick,
                modifier = staggeredRevealModifier(2)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FeatureCard(
                title = strings.videoCompressTitle ?: "Video Compression",
                subtitle = strings.videoCompressSubtitle ?: "Reduce size with minimal quality loss",
                backgroundColor = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color(0xFF8B5CF6),
                titleColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                count = 0,
                showCount = false,
                onClick = onVideoCompressClick,
                modifier = staggeredRevealModifier(3)
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureCard(
                title = strings.imageCompressTitle,
                subtitle = strings.imageCompressSubtitle,
                backgroundColor = if (isDark) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF10B981),
                titleColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                count = 0,
                showCount = false,
                onClick = onImageCompressClick,
                modifier = staggeredRevealModifier(4)
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    backgroundColor: Color,
    titleColor: Color,
    count: Int,
    showCount: Boolean = true,
    subtitleMaxLines: Int = 1,
    contentEndPadding: Dp = 0.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val glassTint = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .advancedTextureCard(
                shape = RoundedCornerShape(24.dp),
                tint = glassTint
            )
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = contentEndPadding),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp, // Larger title
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = titleColor.copy(alpha = 0.8f),
                    lineHeight = 16.sp,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (showCount) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .widthIn(min = 32.dp)
                        .background(titleColor.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (count == 0) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Done",
                            tint = titleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = count.toString(),
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}


