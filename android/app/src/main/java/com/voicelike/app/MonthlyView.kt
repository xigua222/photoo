package com.voicelike.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyListView(
    allMedia: List<MediaItem>,
    processedIds: Set<Long>,
    currentMonth: String = "All",
    showProgressOverlay: Boolean = false,
    onToggleProgressOverlay: (Boolean) -> Unit = {},
    onMonthClick: (List<MediaItem>) -> Unit,
    onAllClick: () -> Unit,
    onBack: () -> Unit
) {
    // Group by Year-Month (Offloaded to background thread)
    val groupedMedia by produceState<Map<String, List<MediaItem>>>(initialValue = emptyMap(), key1 = allMedia) {
        val computed = withContext(Dispatchers.Default) {
            val format = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            allMedia.groupBy { 
                format.format(Date(it.dateAdded * 1000)) 
            }.toSortedMap(compareByDescending { it }) // Newest first
        }
        value = computed
    }

    val yearFormat = SimpleDateFormat("yyyy", Locale.US)
    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    // Colors for months (Magazine style palette)
    val monthColors = if (isDark) {
        listOf(
            Color(0xFF6B5A00),
            Color(0xFF7A3434),
            Color(0xFF245B56),
            Color(0xFF234B5A),
            Color(0xFF365444),
            Color(0xFF7A5331),
            Color(0xFF3A4E53),
            Color(0xFF6A3978),
            Color(0xFF3B427A),
            Color(0xFF242A52),
            Color(0xFF5C5246),
            Color(0xFF4E6421)
        )
    } else {
        listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFFFF6B6B), // Red
            Color(0xFF4ECDC4), // Teal
            Color(0xFF45B7D1), // Blue
            Color(0xFF96CEB4), // Green
            Color(0xFFFFBE76), // Orange
            Color(0xFFDFF9FB), // Light Blue
            Color(0xFFE056FD), // Purple
            Color(0xFF686DE0), // Indigo
            Color(0xFF30336B), // Deep Blue
            Color(0xFFF7F1E3), // Cream
            Color(0xFFA3CB38)  // Olive
        )
    }

    // Group by Year for Sticky Headers
    val groupedByYear = remember(groupedMedia) {
        groupedMedia.entries
            .filter { it.value.isNotEmpty() }
            .groupBy { 
                val date = Date(it.value.first().dateAdded * 1000)
                yearFormat.format(date)
            }
    }

    // Pre-calculate colors to ensure stability during scrolling
    val doneSurface = MaterialTheme.colorScheme.surfaceVariant
    val neutralCardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F6)
    val monthAccentColorMap = remember(groupedMedia) {
        val map = mutableMapOf<String, Color>()
        var colorIndex = 0
        // Iterate in the same order as the list display (groupedMedia is sorted descending)
        groupedMedia.forEach { (key, items) ->
            if (items.isNotEmpty()) {
                map[key] = monthColors[colorIndex % monthColors.size]
                colorIndex++
            }
        }
        map
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = strings.back,
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                iconTint = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = strings.monthlyTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { onToggleProgressOverlay(!showProgressOverlay) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .bounceClick(onClick = { onToggleProgressOverlay(!showProgressOverlay) })
                    .clip(CircleShape)
                    .background(
                        if (showProgressOverlay) {
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.28f else 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            ) {
                Icon(
                    Icons.Outlined.DonutLarge,
                    contentDescription = if (showProgressOverlay) "Hide progress overlay" else "Show progress overlay",
                    tint = if (showProgressOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // All Months Option
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val isSelected = currentMonth == "All"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(staggeredRevealModifier(1))
                        .height(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(neutralCardColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                        .clickable { onAllClick() }
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.filterAll ?: "All", // Fallback if string missing
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            groupedByYear.forEach { (year, months) ->
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = 32.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = year,
                            fontSize = 60.sp, // Much bigger
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                itemsIndexed(months) { index, (key, items) ->
                    val firstItem = items.firstOrNull() ?: return@itemsIndexed
                    val date = Date(firstItem.dateAdded * 1000)
                    
                    // Localized Month
                    val cal = Calendar.getInstance()
                    cal.time = date
                    val monthIndex = cal.get(Calendar.MONTH)
                    val month = strings.months.getOrElse(monthIndex) { "" }
                    
                    // Calculate completion
                    val total = items.size
                    val processedCount = items.count { processedIds.contains(it.id) }
                    val isDone = total > 0 && processedCount == total

                    val accentColor = monthAccentColorMap[key] ?: Color(0xFFF3F4F6)
                    val cardColor = if (isDone) doneSurface else accentColor
                    val isSelected = key == currentMonth
                    val progress = if (total > 0) processedCount.toFloat() / total.toFloat() else 0f
                    val shape = RoundedCornerShape(24.dp)
                    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.42f else 0.24f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(staggeredRevealModifier(2 + (index % 4))) // Reduced cap from 10 to 4 for faster scrolling
                            .height(110.dp) // Slightly taller
                            .padding(vertical = 6.dp)
                            .clip(shape)
                            .background(if (showProgressOverlay) neutralCardColor else cardColor)
                            .border(1.dp, borderColor, shape)
                            .bounceClick(onClick = { onMonthClick(items) })
                    ) {
                        if (showProgressOverlay && progress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .background(accentColor)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = month,
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = if (!showProgressOverlay && isDone && !isSelected) {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else if (!showProgressOverlay && isDone) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Done",
                                    tint = Color(0xFF10B981), // Green
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${processedCount}/${total}",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun MonthlyReviewView(
    monthItems: List<MediaItem>,
    processedIds: Set<Long>,
    pendingTrashCount: Int,
    muteVideos: Boolean,
    onTrashClick: () -> Unit,
    onSwipe: (MediaItem, Boolean, Boolean) -> Unit, // item, isTrash, isLike
    onUndo: (List<UndoAction>, (List<UndoAction>) -> Unit, (MediaItem) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    // Filter out already processed items
    // Maintain a local list state to allow swiping
    // We start with all items in the month, filter out processed ones
    var queue by remember { mutableStateOf(monthItems.filter { !processedIds.contains(it.id) }) }
    var undoStack by remember { mutableStateOf(listOf<UndoAction>()) }
    val isDone = queue.isEmpty()
    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dangerContainerColor = if (isDark) MaterialTheme.colorScheme.errorContainer else Color(0xFFFEF2F2)
    val dangerContentColor = if (isDark) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFDC2626)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isDone) {
            // Done State
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Done",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.monthlyDoneTitle,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))
                androidx.compose.material3.Button(
                    onClick = onBack,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                        contentColor = if (isDark) Color.Black else Color.White
                    )
                ) {
                    Text(strings.monthlyDoneButton)
                }
            }
        } else {
            // Card Stack
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Show top 3
                val visibleItems = queue.take(3).reversed()
                
                visibleItems.forEachIndexed { index, item ->
                    val isTopCard = index == visibleItems.lastIndex
                    val stackIndex = visibleItems.lastIndex - index
                    
                    key(item.id) {
                        DraggableCard(
                            item = item,
                            isTopCard = isTopCard,
                            stackIndex = stackIndex,
                            pauseMainVideos = false,
                            shouldAnalyze = isTopCard, // Analyze top card
                            onSwipeLeft = { 
                                onSwipe(item, true, false) // Trash
                                undoStack = undoStack + UndoAction.Trash(item)
                                queue = queue.drop(1)
                            },
                            onSwipeRight = { 
                                onSwipe(item, false, true) // Keep (Like) - Right swipe is Like
                                undoStack = undoStack + UndoAction.Like(item)
                                queue = queue.drop(1)
                            },
                            onSwipeUp = { 
                                onSwipe(item, false, false) // Keep
                                undoStack = undoStack + UndoAction.Keep(item)
                                queue = queue.drop(1)
                            },
                            muteVideos = muteVideos, // Pass parameter
                            onDetail = { /* Detail view not implemented for this simplified view yet, or can add later */ }
                        )
                    }
                }
            }
            
            // Back Button
            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = strings.back,
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                iconTint = MaterialTheme.colorScheme.onBackground
            )
            
            // Counter
            Text(
                text = "${queue.size} left",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace
            )

            // Undo Button (Bottom Center)
            AnimatedVisibility(
                visible = undoStack.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .bottomNavigationBarPadding(60.dp)
            ) {
                androidx.compose.material3.Button(
                    onClick = { 
                        onUndo(undoStack, { undoStack = it }, { item -> queue = listOf(item) + queue })
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(30.dp),
                    elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = strings.undo,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(strings.undo, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Trash Bin (Top Right)
            if (pendingTrashCount > 0) {
                androidx.compose.material3.Button(
                    onClick = onTrashClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = dangerContainerColor),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.error.copy(alpha = 0.28f) else Color(0xFFFECACA)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Clean",
                        tint = dangerContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(pendingTrashCount.toString(), color = dangerContentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
