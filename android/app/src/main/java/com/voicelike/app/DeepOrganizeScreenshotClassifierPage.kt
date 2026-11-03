package com.voicelike.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DeepOrganizeScreenshotClassifierPage(
    allMedia: List<MediaItem>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val candidates = remember(allMedia) { selectDeepOrganizeScreenshotCandidates(allMedia) }
    var summary by remember { mutableStateOf<DeepOrganizeScreenshotClassificationSummary?>(null) }
    var isLoadingCached by remember { mutableStateOf(true) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var processedCount by rememberSaveable { mutableIntStateOf(0) }
    var totalCount by rememberSaveable { mutableIntStateOf(candidates.size) }
    var activeCategory by rememberSaveable { mutableStateOf<DeepOrganizeScreenshotCategory?>(null) }
    var runningJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(candidates) {
        isLoadingCached = true
        summary = loadDeepOrganizeScreenshotClassificationSummary(context, candidates)
        totalCount = candidates.size
        processedCount = if (summary?.modelWorked == false) 0 else summary?.totalCount ?: 0
        isLoadingCached = false
    }

    activeCategory?.let { category ->
        DeepOrganizeScreenshotCategoryGridPage(
            category = category,
            items = summary?.groupedItems?.get(category).orEmpty(),
            onBack = { activeCategory = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = localizedDeepOrganizeScreenshotClassifierTitle(),
            onBack = {
                runningJob?.cancel()
                onBack()
            }
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1F2937) else Color(0xFF111827)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = localizedDeepOrganizeScreenshotClassifierTitle(),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = localizedDeepOrganizeScreenshotClassifierSubtitle(),
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Outlined.HourglassTop else Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    val progressValue = when {
                        totalCount <= 0 -> 0f
                        else -> processedCount.toFloat() / totalCount.toFloat()
                    }.coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.22f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = resolveScreenshotClassifierStatusText(
                            candidateCount = candidates.size,
                            processedCount = processedCount,
                            totalCount = totalCount,
                            isRunning = isRunning,
                            hasSummary = summary != null,
                            modelWorked = summary?.modelWorked != false
                        ),
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isRunning || candidates.isEmpty()) return@Button
                            runningJob?.cancel()
                            activeCategory = null
                            summary = null
                            runningJob = scope.launch {
                                isRunning = true
                                processedCount = 0
                                totalCount = candidates.size
                                summary = classifyDeepOrganizeScreenshots(context, candidates) { processed, total ->
                                    processedCount = processed
                                    totalCount = total
                                }
                                processedCount = summary?.totalCount ?: processedCount
                                isRunning = false
                            }
                        },
                        enabled = !isRunning && candidates.isNotEmpty(),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF111827),
                            disabledContainerColor = Color.White.copy(alpha = 0.32f),
                            disabledContentColor = Color(0xFF111827).copy(alpha = 0.45f)
                        )
                    ) {
                        if (isLoadingCached) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF111827)
                            )
                        } else {
                            Text(
                                text = resolveScreenshotClassifierActionText(
                                    isRunning = isRunning,
                                    hasSummary = summary != null && summary?.modelWorked != false
                                ),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    summary?.diagnosticMessage?.takeIf { it.isNotBlank() }?.let { diagnostic ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = diagnostic,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val grouped = summary?.groupedItems ?: emptyMap()
            listOf(
                DeepOrganizeScreenshotCategory.Chat,
                DeepOrganizeScreenshotCategory.Order,
                DeepOrganizeScreenshotCategory.Pay,
                DeepOrganizeScreenshotCategory.Other
            ).forEachIndexed { index, category ->
                ScreenshotCategoryEntryCard(
                    title = localizedDeepOrganizeScreenshotCategoryName(category),
                    count = grouped[category].orEmpty().size,
                    color = when (category) {
                        DeepOrganizeScreenshotCategory.Chat -> Color(0xFF3B82F6)
                        DeepOrganizeScreenshotCategory.Order -> Color(0xFFF59E0B)
                        DeepOrganizeScreenshotCategory.Pay -> Color(0xFF10B981)
                        DeepOrganizeScreenshotCategory.Other -> Color(0xFF6B7280)
                    },
                    enabled = !isRunning && grouped[category].orEmpty().isNotEmpty(),
                    onClick = { activeCategory = category }
                )
                if (index != 3) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ScreenshotCategoryEntryCard(
    title: String,
    count: Int,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    FeatureCard(
        title = title,
        subtitle = if (enabled) {
            if (Locale.getDefault().language.equals("zh", ignoreCase = true)) "查看这一类截图" else "Open this bucket"
        } else {
            if (Locale.getDefault().language.equals("zh", ignoreCase = true)) "分类完成后可进入" else "Available after classification"
        },
        backgroundColor = if (isDark) color.copy(alpha = 0.60f) else color,
        titleColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
        count = count,
        onClick = { if (enabled) onClick() }
    )
}

@Composable
private fun DeepOrganizeScreenshotCategoryGridPage(
    category: DeepOrganizeScreenshotCategory,
    items: List<DeepOrganizeScreenshotClassifiedItem>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = localizedDeepOrganizeScreenshotCategoryName(category),
            onBack = onBack
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (Locale.getDefault().language.equals("zh", ignoreCase = true)) "共 ${items.size} 张" else "${items.size} items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(items, key = { it.item.id }) { classified ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(classified.item.uri)
                                .size(420)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = classified.item.displayName.ifBlank { classified.item.bucketName },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text(
                                text = "${(classified.confidence * 100f).roundToInt()}%",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveScreenshotClassifierStatusText(
    candidateCount: Int,
    processedCount: Int,
    totalCount: Int,
    isRunning: Boolean,
    hasSummary: Boolean,
    modelWorked: Boolean
): String {
    val isZh = Locale.getDefault().language.equals("zh", ignoreCase = true)
    return when {
        candidateCount == 0 -> if (isZh) "没有找到截图文件夹里的图片" else "No screenshots found in screenshot folders"
        isRunning -> if (isZh) "正在分类 $processedCount / $totalCount" else "Classifying $processedCount / $totalCount"
        !modelWorked -> if (isZh) "模型没有实际跑起来，请重试" else "Model did not actually run. Please retry"
        hasSummary -> if (isZh) "已整理 $processedCount / $totalCount" else "Sorted $processedCount / $totalCount"
        else -> if (isZh) "共发现 $totalCount 张候选截图" else "$totalCount screenshots ready to classify"
    }
}

private fun resolveScreenshotClassifierActionText(isRunning: Boolean, hasSummary: Boolean): String {
    val isZh = Locale.getDefault().language.equals("zh", ignoreCase = true)
    return when {
        isRunning -> if (isZh) "分类中" else "Running"
        hasSummary -> if (isZh) "重新分类" else "Run Again"
        else -> if (isZh) "开始分类" else "Start"
    }
}
