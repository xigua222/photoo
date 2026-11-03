package com.voicelike.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.location.Geocoder
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

import coil.compose.AsyncImage
import coil.request.ImageRequest

private fun isCoordinateLocationText(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    val match = Regex("""\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*""").matchEntire(text.trim()) ?: return false
    val lat = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return false
    val lon = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return false
    return lat in -90.0..90.0 && lon in -180.0..180.0
}

@Composable
fun DailyCalendarView(
    currentMonth: String, // Format: "yyyy-MM"
    allMedia: List<MediaItem>,
    onDaySelected: (String) -> Unit, // Returns "yyyy-MM-dd"
    onBack: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val emitDaySelection: (String) -> Unit = { day ->
        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onDaySelected(day)
    }
    // Cover Mode is now the only mode
    
    val calendar = remember(currentMonth) {
        val cal = Calendar.getInstance()
        val parts = currentMonth.split("-")
        if (parts.size == 2) {
            cal.set(Calendar.YEAR, parts[0].toInt())
            cal.set(Calendar.MONTH, parts[1].toInt() - 1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        cal
    }

    val daysInMonth = remember(calendar) {
        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val startDayOfWeek = remember(calendar) {
        // 1 = Sunday, 2 = Monday, ...
        calendar.get(Calendar.DAY_OF_WEEK)
    }
    
    // 0-based offset for the first day (e.g. if starts on Tuesday (3), offset is 2)
    val offset = startDayOfWeek - 1

    // Group media by day
    // Map: Day -> Pair(Count, CoverItem)
    val dayData = remember(allMedia, currentMonth) {
        val data = mutableMapOf<Int, Pair<Int, MediaItem?>>()
        
        // Optimize: Use timestamp range instead of string formatting
        val cal = Calendar.getInstance()
        val parts = currentMonth.split("-")
        if (parts.size == 2) {
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startTs = cal.timeInMillis / 1000L
            
            cal.add(Calendar.MONTH, 1)
            val endTs = cal.timeInMillis / 1000L
            
            // Filter by range first (fast)
            val monthItems = allMedia.filter { it.dateAdded in startTs until endTs }
            
            // Group items
            val groupedItems = mutableMapOf<Int, MutableList<MediaItem>>()
            val itemCal = Calendar.getInstance()
            monthItems.forEach { item ->
                itemCal.timeInMillis = item.dateAdded * 1000L
                val day = itemCal.get(Calendar.DAY_OF_MONTH)
                groupedItems.getOrPut(day) { mutableListOf() }.add(item)
            }
            
            // Populate data map
            groupedItems.forEach { (day, items) ->
                val cover = items.maxByOrNull { it.dateAdded }
                data[day] = items.size to cover
            }
        }
        data
    }
    
    val maxCount = remember(dayData) {
        dayData.values.maxOfOrNull { it.first } ?: 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Calendar Grid (Contains everything to scroll together)
        val totalCells = daysInMonth + offset
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Header Section
            item(span = { GridItemSpan(7) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 24.dp)
                ) {
                    UnifiedSubpageTopBarIconButton(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = LocalAppStrings.current.back,
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Text(
                        text = currentMonth,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            // 2. "All Days" Button
            item(span = { GridItemSpan(7) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { emitDaySelection("All") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LocalAppStrings.current.allDays, // Localized
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 3. Week Days Header
            item(span = { GridItemSpan(7) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    weekDays.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Calendar Cells
            items(totalCells) { index ->
                if (index < offset) {
                    Box(modifier = Modifier.aspectRatio(1f)) // Empty spacer
                } else {
                    val day = index - offset + 1
                    val data = dayData[day]
                    val count = data?.first ?: 0
                    val cover = data?.second
                    
                    DayCoverCell(
                        day = day,
                        dayLabel = day.toString(),
                        cover = cover,
                        onSelect = {
                            val dayStr = day.toString().padStart(2, '0')
                            emitDaySelection("$currentMonth-$dayStr")
                        }
                    )
                }
            }
            
            // 5. Smart Recommendations
            if (dayData.isNotEmpty()) {
                item(span = { GridItemSpan(7) }) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SmartRecommendations(
                        dayData = dayData,
                        allMedia = allMedia,
                        currentMonth = currentMonth,
                        maxCount = maxCount,
                        onDaySelected = { day ->
                            val dayStr = day.toString().padStart(2, '0')
                            emitDaySelection("$currentMonth-$dayStr")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SmartRecommendations(
    dayData: Map<Int, Pair<Int, MediaItem?>>,
    allMedia: List<MediaItem>, // Added parameter
    currentMonth: String, // Added parameter to help filter
    maxCount: Int,
    onDaySelected: (Int) -> Unit
) {
    // Colors from MonthlyView
    val monthColors = listOf(
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
    
    // Find interesting days (e.g. top 5 by count)
    val interestingDays = remember(dayData) {
        dayData.entries
            .filter { it.value.first > 0 }
            .sortedByDescending { it.value.first }
            .take(5)
            .map { it.key to it.value.first }
    }
    
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val newPageStrings = getNewPageStrings(LocalAppLanguage.current)
    val dayLabel: (Int) -> String = newPageStrings.dailyDayLabel
    if (interestingDays.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.background else Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = newPageStrings.dailySmartRecommendationTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Vertical list of large cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                interestingDays.forEachIndexed { index, (day, count) ->
                    val items = dayData[day]?.second?.let { listOf(it) } ?: emptyList() // Fallback if list not available in simplified map
                    // But wait, dayData is Map<Int, Pair<Int, MediaItem?>> now. We need the list to find location.
                    // The simplified dayData in DailyCalendarView optimized out the list.
                    // We need to pass the full map or re-query?
                    // Re-querying is expensive. 
                    // Let's modify DailyCalendarView to pass full list map or use a helper.
                    // Actually, SmartRecommendations is inside DailyCalendarView scope? No, it's outside.
                    // Let's change SmartRecommendations signature to take full list map if possible, 
                    // OR, since we only show top 5, we can just filter allMedia again for these 5 days.
                    // That's cheap enough.
                    
                    // Filter items for this day
                    val dayItems = remember(day, currentMonth, allMedia) {
                        val cal = Calendar.getInstance()
                        val parts = currentMonth.split("-")
                        if (parts.size == 2) {
                            val year = parts[0].toInt()
                            val month = parts[1].toInt() - 1
                            cal.set(year, month, day)
                            val start = cal.timeInMillis / 1000L
                            cal.add(Calendar.DATE, 1)
                            val end = cal.timeInMillis / 1000L
                            allMedia.filter { it.dateAdded in start until end }
                        } else emptyList()
                    }
                    
                    val previewItem = remember(dayItems, day) {
                        val withLocation = dayItems.firstOrNull { it.location != "Unknown Location" }
                        if (withLocation != null) {
                            withLocation
                        } else {
                            val index = if (dayItems.isNotEmpty()) (day * 7 + dayItems.size) % dayItems.size else -1
                            dayItems.getOrNull(index)
                        }
                    }
                    val displayCover = previewItem ?: dayItems.firstOrNull()
                    
                    // Location: Try cover first, then search list
                    var location by remember(displayCover) { mutableStateOf(displayCover?.location ?: strings.unknownLocation) }
                    
                    LaunchedEffect(dayItems, displayCover) {
                        val isValidLocation: (String?) -> Boolean = { text ->
                            !text.isNullOrBlank() &&
                                text != "Unknown Location" &&
                                text != strings.unknownLocation &&
                                !isCoordinateLocationText(text)
                        }
                        if (isValidLocation(displayCover?.location)) {
                            location = displayCover?.location ?: strings.unknownLocation
                            return@LaunchedEffect
                        }
                        
                        val validItem = dayItems.firstOrNull { isValidLocation(it.location) }
                        if (validItem != null) {
                            location = validItem.location
                            return@LaunchedEffect
                        }
                        
                        withContext(Dispatchers.IO) {
                            var foundLocation: String? = null
                            val candidates = dayItems.take(20)
                            if (candidates.isNotEmpty()) {
                                MediaAnalysisManager.prefetchUnifiedInsights(
                                    context = context,
                                    items = candidates,
                                    languageCode = null,
                                    includeKeywords = false,
                                    maxCount = candidates.size,
                                    timeBudgetMs = 1300L
                                )
                                val insightMap = MediaInsightRepository.getMany(context, candidates.map { it.id })
                                for (item in candidates) {
                                    val candidateLocation = insightMap[item.id]?.location
                                    if (isValidLocation(candidateLocation)) {
                                        foundLocation = candidateLocation
                                        break
                                    }
                                }
                                if (foundLocation == null) {
                                    // Fallback: temporary independent parse for missing cache entries.
                                    for (item in candidates.take(6)) {
                                        val tempLocation = MediaAnalysisManager.temporaryResolveLocation(context, item)
                                        if (isValidLocation(tempLocation)) {
                                            foundLocation = tempLocation
                                            break
                                        }
                                    }
                                }
                            }
                            
                            if (foundLocation != null) {
                                withContext(Dispatchers.Main) {
                                    location = foundLocation!!
                                }
                            }
                        }
                    }
                    
                    // Large Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) // Lighter background for card
                            .clickable { onDaySelected(day) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (previewItem != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(previewItem.uri)
                                        .crossfade(true)
                                        .size(160)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        // Right: Info
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = dayLabel(day),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val showLocation = location.isNotBlank() && location != "Unknown Location" && location != strings.unknownLocation
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = newPageStrings.dailyPhotoCountLabel(count),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (showLocation) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = location,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        }
                        
                        // Optional: Arrow or Chevron
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack, // Should be ChevronRight but using ArrowBack flipped or similar
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.scale(scaleX = -1f, scaleY = 1f) // Flip for Right arrow
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayCoverCell(
    day: Int,
    dayLabel: String,
    cover: MediaItem?,
    onSelect: () -> Unit
) {
    val isZero = cover == null
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isZero) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !isZero, onClick = onSelect)
    ) {
        if (cover != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cover.uri)
                    .crossfade(true)
                    .size(200) // Thumbnail size
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay for Text Visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // Day Number (Centered)
            Text(
                text = dayLabel,
                color = Color.White,
                fontSize = 20.sp, // Larger font
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Empty State - No dots, just date
            Text(
                text = dayLabel,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
