package com.voicelike.app

import com.google.gson.Gson
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private fun isCoordinateLocationText(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    val match = Regex("""\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*""").matchEntire(text.trim()) ?: return false
    val lat = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return false
    val lon = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return false
    return lat in -90.0..90.0 && lon in -180.0..180.0
}

private data class UniversalStatsData(
    val cameraTop5: List<Pair<String, Int>> = emptyList(),
    val locationTop5: List<Pair<String, Int>> = emptyList(),
    val monthly12: List<Pair<String, Int>> = emptyList(),
    val keywordTop30: List<Pair<String, Int>> = emptyList(),
    val photoTotal: Int = 0,
    val exifCovered: Int = 0,
    val mlCovered: Int = 0,
    val unifiedCovered: Int = 0
)

private data class MostActiveDaySummary(
    val dayLabel: String = "",
    val itemCount: Int = 0
)

private data class ShootingStatsData(
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val totalCount: Int = 0,
    val totalDurationMins: Int = 0,
    val screenshotCount: Int = 0,
    val likedCount: Int = 0,
    val cameraPhotoCount: Int = 0,
    val mostActiveDay: MostActiveDaySummary? = null,
    val locationCount: Int = 0,
    val heatmapData: Map<String, Int> = emptyMap(),
    val movieComparison: String = ""
)

private object ShootingStatsUiCache {
    var universalKey: String? = null
    var universalData: Any? = null
    var periodKey: String? = null
    var periodData: Any? = null
}

private object ShootingStatsBackgroundWork {
    private val remediatedUniversalKeys = mutableSetOf<String>()

    fun markCoordinateRemediationStarted(key: String): Boolean = synchronized(this) {
        remediatedUniversalKeys.add(key)
    }
}

private object ShootingStatsPersistentCache {
    private const val PREFS_NAME = "shooting_stats_cache_v2"
    private const val KEY_UNIVERSAL_KEY = "universal_key"
    private const val KEY_UNIVERSAL_DATA = "universal_data"
    private const val KEY_PERIOD_KEY = "period_key"
    private const val KEY_PERIOD_DATA = "period_data"
    private val gson = Gson()

    private fun serializeSafely(payload: Any): String? {
        return runCatching { gson.toJson(payload) }.getOrNull()
    }

    fun loadUniversal(context: android.content.Context, key: String): UniversalStatsData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (prefs.getString(KEY_UNIVERSAL_KEY, null) != key) return null
        val json = prefs.getString(KEY_UNIVERSAL_DATA, null) ?: return null
        return runCatching { gson.fromJson(json, UniversalStatsData::class.java) }.getOrNull()
    }

    fun saveUniversal(context: android.content.Context, key: String, data: UniversalStatsData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = serializeSafely(data) ?: return
        prefs.edit()
            .putString(KEY_UNIVERSAL_KEY, key)
            .putString(KEY_UNIVERSAL_DATA, json)
            .apply()
    }

    fun loadPeriod(context: android.content.Context, key: String): ShootingStatsData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (prefs.getString(KEY_PERIOD_KEY, null) != key) return null
        val json = prefs.getString(KEY_PERIOD_DATA, null) ?: return null
        return runCatching { gson.fromJson(json, ShootingStatsData::class.java) }.getOrNull()
    }

    fun savePeriod(context: android.content.Context, key: String, data: ShootingStatsData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = serializeSafely(data) ?: return
        prefs.edit()
            .putString(KEY_PERIOD_KEY, key)
            .putString(KEY_PERIOD_DATA, json)
            .apply()
    }
}

private fun buildMediaSignature(items: List<MediaItem>): String {
    val first = items.firstOrNull()
    val last = items.lastOrNull()
    return listOf(
        items.size,
        first?.id ?: -1L,
        first?.dateAdded ?: -1L,
        last?.id ?: -1L,
        last?.dateAdded ?: -1L
    ).joinToString("|")
}

private fun buildLikedSignature(ids: Set<Long>): String {
    var sum = 0L
    ids.forEach { sum += it }
    return "${ids.size}|$sum"
}

@OptIn(FlowPreview::class)
@Composable
fun ShootingStatsView(
    allMedia: List<MediaItem>,
    likedIds: Set<Long>,
    useInternalScroll: Boolean = true,
    onOpenGlobe: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    val currentLang = LocalAppLanguage.current.code
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    var selectedPeriod by remember { mutableStateOf("Year") } // "Year" or "Month"
    
    // State for navigation
    var displayedDate by remember { mutableStateOf(Calendar.getInstance()) }
    
    val targetYear = displayedDate.get(Calendar.YEAR)
    val targetMonth = displayedDate.get(Calendar.MONTH) // 0-indexed
    val now = Calendar.getInstance()
    val maxYear = now.get(Calendar.YEAR)
    val maxMonthForYear = if (targetYear >= maxYear) now.get(Calendar.MONTH) else Calendar.DECEMBER
    val canGoNext = if (selectedPeriod == "Year") {
        targetYear < maxYear
    } else {
        targetYear < maxYear || (targetYear == maxYear && targetMonth < maxMonthForYear)
    }
    val canGoPrev = true

    val insightVersion by produceState(initialValue = MediaInsightRepository.version.value) {
        MediaInsightRepository.version
            .debounce(450)
            .collect { value = it }
    }
    val mediaSignature = remember(allMedia) { buildMediaSignature(allMedia) }
    val likedSignature = remember(likedIds) { buildLikedSignature(likedIds) }
    val photoIds = remember(allMedia) {
        allMedia.asSequence()
            .filter { it.type == "photo" }
            .map { it.id }
            .toSet()
    }
    val universalCacheKey = remember(mediaSignature, currentLang, insightVersion) {
        "u|$mediaSignature|$currentLang|$insightVersion"
    }
    val persistedUniversal = remember(universalCacheKey) {
        ShootingStatsPersistentCache.loadUniversal(context, universalCacheKey)
    }

    val universalStats by produceState(
        initialValue = (ShootingStatsUiCache.universalData as? UniversalStatsData)
            ?: persistedUniversal
            ?: UniversalStatsData(),
        photoIds,
        currentLang,
        insightVersion
    ) {
        val cacheKey = universalCacheKey
        val cached = (ShootingStatsUiCache.universalData as? UniversalStatsData)
        if (ShootingStatsUiCache.universalKey == cacheKey && cached != null) {
            value = cached
            return@produceState
        }
        if (persistedUniversal != null) {
            value = persistedUniversal
            ShootingStatsUiCache.universalKey = cacheKey
            ShootingStatsUiCache.universalData = persistedUniversal
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            val photoTotal = photoIds.size
            val photoInsights = MediaInsightRepository.getMany(context, photoIds)

            val cameraCounts = mutableMapOf<String, Int>()
            val locationCounts = mutableMapOf<String, Int>()
            val keywordCounts = mutableMapOf<String, Int>()
            var exifCovered = 0
            var mlCovered = 0
            var unifiedCovered = 0

            photoInsights.values.forEach { snapshot ->
                val camera = snapshot.camera
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && it != "--" }
                    ?.let { DeviceModelNameResolver.resolveCameraLabel(context, it) }
                val location = snapshot.location
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && !it.equals("Unknown Location", ignoreCase = true) }
                val keywords = snapshot.keywords
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val hasPhotoInfo = camera != null || location != null
                val hasAiTags = keywords.isNotEmpty()
                if (hasPhotoInfo) exifCovered++
                if (hasAiTags) mlCovered++
                if (hasPhotoInfo || hasAiTags) unifiedCovered++

                if (camera != null) {
                    cameraCounts[camera] = (cameraCounts[camera] ?: 0) + 1
                }
                if (location != null) {
                    locationCounts[location] = (locationCounts[location] ?: 0) + 1
                }
                keywords.forEach { kw ->
                    keywordCounts[kw] = (keywordCounts[kw] ?: 0) + 1
                }
            }

            val nowCal = Calendar.getInstance()
            nowCal.set(Calendar.DAY_OF_MONTH, 1)
            nowCal.set(Calendar.HOUR_OF_DAY, 0)
            nowCal.set(Calendar.MINUTE, 0)
            nowCal.set(Calendar.SECOND, 0)
            nowCal.set(Calendar.MILLISECOND, 0)

            val monthBuckets = linkedMapOf<String, Int>()
            for (offset in 11 downTo 0) {
                val cal = nowCal.clone() as Calendar
                cal.add(Calendar.MONTH, -offset)
                val key = "%04d-%02d".format(
                    Locale.US,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1
                )
                monthBuckets[key] = 0
            }

            val itemCal = Calendar.getInstance()
            allMedia.forEach { item ->
                itemCal.timeInMillis = item.dateAdded * 1000L
                val key = "%04d-%02d".format(
                    Locale.US,
                    itemCal.get(Calendar.YEAR),
                    itemCal.get(Calendar.MONTH) + 1
                )
                if (monthBuckets.containsKey(key)) {
                    monthBuckets[key] = (monthBuckets[key] ?: 0) + 1
                }
            }

            UniversalStatsData(
                cameraTop5 = cameraCounts.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { it.key to it.value },
                locationTop5 = locationCounts.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { it.key to it.value },
                monthly12 = monthBuckets.entries.map { entry -> entry.key to entry.value },
                keywordTop30 = keywordCounts.entries
                    .sortedByDescending { it.value }
                    .take(30)
                    .map { it.key to it.value },
                photoTotal = photoTotal,
                exifCovered = exifCovered,
                mlCovered = mlCovered,
                unifiedCovered = unifiedCovered
            )
        }
        ShootingStatsUiCache.universalKey = cacheKey
        ShootingStatsUiCache.universalData = value
        ShootingStatsPersistentCache.saveUniversal(context, cacheKey, value)
    }

    LaunchedEffect(universalCacheKey, photoIds) {
        if (photoIds.isEmpty()) return@LaunchedEffect
        if (!ShootingStatsBackgroundWork.markCoordinateRemediationStarted(universalCacheKey)) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val coordinateEntries = MediaInsightRepository.getMany(context, photoIds)
                .filter { (_, snapshot) -> isCoordinateLocationText(snapshot.location) }
                .entries
                .take(180)
            if (coordinateEntries.isEmpty()) return@withContext
            withContext(Dispatchers.IO) {
                coordinateEntries.forEach { (id, snapshot) ->
                    MediaInsightRepository.upsertLocation(context, id, snapshot.location)
                }
            }
        }
    }

    LaunchedEffect(selectedPeriod) {
        val updated = displayedDate.clone() as Calendar
        if (updated.get(Calendar.YEAR) > maxYear) {
            updated.set(Calendar.YEAR, maxYear)
            updated.set(Calendar.MONTH, now.get(Calendar.MONTH))
        }
        if (selectedPeriod == "Month" && updated.get(Calendar.YEAR) == maxYear && updated.get(Calendar.MONTH) > now.get(Calendar.MONTH)) {
            updated.set(Calendar.MONTH, now.get(Calendar.MONTH))
        }
        displayedDate = updated
    }

    // Reset displayed date to now when period changes (optional, but good for UX)
    // Or keep it? Let's keep it to current context if possible, but switching Year->Month should probably go to current month of that year.
    // Logic: When switching to Month, use the displayedYear + current month? Or just keep displayedDate as is.
    // Keeping displayedDate as is works fine.

    val periodCacheKey = remember(mediaSignature, selectedPeriod, targetYear, targetMonth, likedSignature) {
        "p|$mediaSignature|$selectedPeriod|$targetYear|$targetMonth|$likedSignature"
    }
    val persistedPeriod = remember(periodCacheKey) {
        ShootingStatsPersistentCache.loadPeriod(context, periodCacheKey)
    }

    val statsData by produceState(
        initialValue = (ShootingStatsUiCache.periodData as? ShootingStatsData)
            ?: persistedPeriod
            ?: ShootingStatsData(),
        allMedia,
        selectedPeriod,
        targetYear,
        targetMonth,
        likedIds
    ) {
        val cacheKey = periodCacheKey
        val cached = (ShootingStatsUiCache.periodData as? ShootingStatsData)
        if (ShootingStatsUiCache.periodKey == cacheKey && cached != null) {
            value = cached
            return@produceState
        }
        if (persistedPeriod != null) {
            value = persistedPeriod
            ShootingStatsUiCache.periodKey = cacheKey
            ShootingStatsUiCache.periodData = persistedPeriod
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            val calendar = Calendar.getInstance()
            val filteredMedia = allMedia.filter { item ->
                calendar.timeInMillis = item.dateAdded * 1000L
                if (selectedPeriod == "Year") {
                    calendar.get(Calendar.YEAR) == targetYear
                } else {
                    calendar.get(Calendar.YEAR) == targetYear &&
                        calendar.get(Calendar.MONTH) == targetMonth
                }
            }

            val photoCount = filteredMedia.count { it.type == "photo" }
            val videoCount = filteredMedia.count { it.type == "video" }
            val totalCount = photoCount + videoCount
            val totalDurationMins = (filteredMedia.sumOf { it.duration } / 60000).toInt()

            // Movie Comparison Logic
            val movieComparison = run {
                val moviesEn = listOf(
                    "Titanic" to 195, 
                    "Avatar" to 162, 
                    "The Matrix" to 136, 
                    "Inception" to 148, 
                    "The Godfather" to 175,
                    "Pulp Fiction" to 154,
                    "Forrest Gump" to 142
                )
                                val moviesZh = listOf(
                    "Wolf Warrior 2" to 123,
                    "Hi, Mom" to 128,
                    "Full River Red" to 159,
                    "The Wandering Earth" to 125,
                    "Ne Zha" to 110,
                    "The Battle at Lake Changjin" to 176,
                    "YOLO" to 129
                )

                if (totalDurationMins < 45) {
                    val ratio = String.format("%.1f", totalDurationMins.toFloat() / 3f)
                    String.format(strings.shootingStatsShortDrama, ratio)
                } else if (totalDurationMins < 120) {
                    val ratio = String.format("%.1f", totalDurationMins.toFloat() / 45f)
                    String.format(strings.shootingStatsTvDrama, ratio)
                } else {
                    val list = if (currentLang.startsWith("zh")) moviesZh else moviesEn
                    val index = (totalDurationMins % list.size)
                    val (name, duration) = list[index]
                    val ratio = String.format("%.1f", totalDurationMins.toFloat() / duration)
                    "Equiv. to $ratio x $name"
            }
            }

            val screenshotCount = filteredMedia.count {
                it.type == "photo" &&
                (it.bucketName.contains("Screenshot", ignoreCase = true) || 
                    it.bucketName.contains("截屏", ignoreCase = true) ||
                    it.bucketName.contains("截图", ignoreCase = true))
            }
            val likedCount = filteredMedia.count { likedIds.contains(it.id) }
            val cameraPhotoCount = (photoCount - screenshotCount).coerceAtLeast(0)
            val activityDate = Date()
            val activityDayFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
            val heatmapDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val activeDayCounts = mutableMapOf<String, Int>()
            val heatmapData = mutableMapOf<String, Int>()
            val bucketNames = LinkedHashSet<String>()
            filteredMedia.forEach { item ->
                activityDate.time = item.dateAdded * 1000L
                val activeDayKey = activityDayFormat.format(activityDate)
                activeDayCounts[activeDayKey] = (activeDayCounts[activeDayKey] ?: 0) + 1

                val heatmapKey = heatmapDateFormat.format(activityDate)
                heatmapData[heatmapKey] = (heatmapData[heatmapKey] ?: 0) + 1
                bucketNames.add(item.bucketName)
            }
            val mostActiveDay = activeDayCounts.maxByOrNull { it.value }?.let { entry ->
                MostActiveDaySummary(
                    dayLabel = entry.key,
                    itemCount = entry.value
                )
            }

            ShootingStatsData(
                photoCount = photoCount,
                videoCount = videoCount,
                totalCount = totalCount,
                totalDurationMins = totalDurationMins,
                screenshotCount = screenshotCount,
                likedCount = likedCount,
                cameraPhotoCount = cameraPhotoCount,
                mostActiveDay = mostActiveDay,
                locationCount = bucketNames.size,
                heatmapData = heatmapData,
                movieComparison = movieComparison
            )
        }
        ShootingStatsUiCache.periodKey = cacheKey
        ShootingStatsUiCache.periodData = value
        ShootingStatsPersistentCache.savePeriod(context, cacheKey, value)
    }

    // Unpack stats for UI usage
    val photoCount = statsData.photoCount
    val videoCount = statsData.videoCount
    val totalCount = statsData.totalCount
    val totalDurationMins = statsData.totalDurationMins
    val movieComparison = statsData.movieComparison
    val screenshotCount = statsData.screenshotCount
    val likedCount = statsData.likedCount
    val cameraPhotoCount = statsData.cameraPhotoCount
    val mostActiveDay = statsData.mostActiveDay
    val locationCount = statsData.locationCount
    val heatmapData = statsData.heatmapData

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 0.dp)
            .then(
                if (useInternalScroll) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                }
            )
    ) {
        UniversalDataSection(
            cameraTop5 = universalStats.cameraTop5,
            locationTop5 = universalStats.locationTop5,
            monthly12 = universalStats.monthly12,
            keywordTop30 = universalStats.keywordTop30,
            photoTotal = universalStats.photoTotal,
            unifiedCovered = universalStats.unifiedCovered,
            currentLang = currentLang,
            isDark = isDark,
            onOpenGlobe = onOpenGlobe,
            modifier = Modifier.then(staggeredRevealModifier(0))
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Header & Segmented Control ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .then(staggeredRevealModifier(1)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F6), RoundedCornerShape(50))
                    .padding(4.dp)
            ) {
                listOf("Year" to strings.shootingStatsYear, "Month" to strings.shootingStatsMonth).forEach { (key, label) ->
                    val isSelected = selectedPeriod == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { 
                                if (!isSelected) {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedPeriod = key 
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        if (selectedPeriod == "Year" && targetYear == 1826) {
            ShootingStatsEasterEgg(
                isDark = isDark,
                onPrev = {
                    if (canGoPrev) {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val cal = displayedDate.clone() as Calendar
                        cal.add(Calendar.YEAR, -1)
                        displayedDate = cal
                    }
                },
                onNext = {
                    if (canGoNext) {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val cal = displayedDate.clone() as Calendar
                        cal.add(Calendar.YEAR, 1)
                        displayedDate = cal
                    }
                },
                canGoNext = canGoNext
            )
        } else {
            // --- Summary Card with Navigation ---
        val summaryLocale = remember(appLanguage) {
            when (appLanguage) {
                AppLanguage.English -> Locale.ENGLISH
                AppLanguage.SimplifiedChinese -> Locale.SIMPLIFIED_CHINESE
                AppLanguage.TraditionalChinese -> Locale.TRADITIONAL_CHINESE
                AppLanguage.Korean -> Locale.KOREAN
                AppLanguage.Japanese -> Locale.JAPANESE
                AppLanguage.French -> Locale.FRENCH
                AppLanguage.German -> Locale.GERMAN
                AppLanguage.Italian -> Locale.ITALIAN
                AppLanguage.Portuguese -> Locale("pt")
                AppLanguage.Spanish -> Locale("es")
                AppLanguage.Russian -> Locale("ru")
                AppLanguage.Thai -> Locale("th")
                AppLanguage.Vietnamese -> Locale("vi")
                AppLanguage.Indonesian -> Locale("id")
                AppLanguage.Arabic -> Locale("ar")
                else -> appLanguage.locale
            }
        }
        val summaryTitle = if (selectedPeriod == "Year") {
            targetYear.toString()
        } else {
            SimpleDateFormat("MMMM yyyy", summaryLocale).format(displayedDate.time)
        }
        
        // Use correct format string based on period
        val summaryFormatStr = if (selectedPeriod == "Year") strings.shootingStatsSummary else strings.shootingStatsSummaryMonth
        
        val summaryText = remember(summaryFormatStr, summaryTitle, totalCount) {
            try {
                String.format(summaryFormatStr, summaryTitle, totalCount)
            } catch (e: Exception) {
                "$summaryTitle: $totalCount items"
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(staggeredRevealModifier(2))
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFF7ED))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = summaryText,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF9A3412),
                    lineHeight = 40.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
        val itemsText = remember(strings.shootingStatsItems, cameraPhotoCount, videoCount, screenshotCount, strings.shootingStatsScreenshots) {
            try {
                String.format(strings.shootingStatsItems, cameraPhotoCount, videoCount) + " | ${screenshotCount} ${strings.shootingStatsScreenshots}"
            } catch (e: Exception) {
                "$cameraPhotoCount Photos | $videoCount Videos | $screenshotCount ${strings.shootingStatsScreenshots}"
            }
        }

                Text(
                    text = itemsText,
                    fontSize = 14.sp,
                    color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color(0xFFC2410C).copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 0.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (canGoPrev) {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val cal = displayedDate.clone() as Calendar
                                if (selectedPeriod == "Year") cal.add(Calendar.YEAR, -1) else cal.add(Calendar.MONTH, -1)
                                displayedDate = cal
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Prev",
                            tint = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF9A3412)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (canGoNext) {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val cal = displayedDate.clone() as Calendar
                                if (selectedPeriod == "Year") cal.add(Calendar.YEAR, 1) else cal.add(Calendar.MONTH, 1)
                                displayedDate = cal
                            }
                        },
                        enabled = canGoNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = if (isDark) {
                                if (canGoNext) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            } else {
                                if (canGoNext) Color(0xFF9A3412) else Color(0xFF9A3412).copy(alpha = 0.3f)
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // --- 2x2 Grid Layout for Stats ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Video & Location
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(3))
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Video Stats
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF1E40AF).copy(alpha = 0.2f) else Color(0xFFEFF6FF))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = strings.shootingStatsVideoTitle,
                            tint = if (isDark) Color(0xFF93C5FD).copy(alpha = 0.9f) else Color(0xFF1E40AF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.shootingStatsVideoTitle,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF93C5FD).copy(alpha = 0.9f) else Color(0xFF1E40AF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val durationText: String = remember(strings.shootingStatsVideoDuration, totalDurationMins, videoCount) {
                        try {
                            if (totalDurationMins == 0 && videoCount > 0) {
                                // Show "< 1" or handle gracefully
                                String.format(strings.shootingStatsVideoDuration, "< 1")
                            } else {
                                String.format(strings.shootingStatsVideoDuration, totalDurationMins)
                            }
                        } catch (e: Exception) {
                            "$totalDurationMins mins"
                        }
                    }
                    Text(
                        text = durationText,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFBFDBFE).copy(alpha = 0.9f) else Color(0xFF1E3A8A)
                    )
                    Text(
                        text = movieComparison,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF93C5FD).copy(alpha = 0.6f) else Color(0xFF60A5FA),
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Location Stats
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF92400E).copy(alpha = 0.2f) else Color(0xFFFEF3C7))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = strings.shootingStatsLocationTitle,
                            tint = if (isDark) Color(0xFFFCD34D).copy(alpha = 0.9f) else Color(0xFF92400E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.shootingStatsLocationTitle,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFFCD34D).copy(alpha = 0.9f) else Color(0xFF92400E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val locationText = remember(strings.shootingStatsLocationPlaces, locationCount) {
                        try {
                            String.format(strings.shootingStatsLocationPlaces, locationCount)
                        } catch (e: Exception) {
                            "$locationCount Places"
                        }
                    }
                    Text(
                        text = locationText,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFDE68A).copy(alpha = 0.9f) else Color(0xFF78350F)
                    )
                }
            }

            // Row 2: Screenshots & Active Day
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(4))
                    .height(IntrinsicSize.Min)
                    .heightIn(min = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Screenshots
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF166534).copy(alpha = 0.2f) else Color(0xFFF0FDF4))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = strings.likes,
                                tint = if (isDark) Color(0xFF86EFAC).copy(alpha = 0.9f) else Color(0xFF166534),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.likes,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF86EFAC).copy(alpha = 0.9f) else Color(0xFF166534),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = likedCount.toString(),
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFBBF7D0).copy(alpha = 0.9f) else Color(0xFF14532D)
                        )
                    }
                }
                
                // Most Active Day
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF6B21A8).copy(alpha = 0.2f) else Color(0xFFFAF5FF))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Whatshot,
                                contentDescription = strings.shootingStatsActiveDayTitle,
                                tint = if (isDark) Color(0xFFD8B4FE).copy(alpha = 0.9f) else Color(0xFF6B21A8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.shootingStatsActiveDayTitle,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFD8B4FE).copy(alpha = 0.9f) else Color(0xFF6B21A8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (mostActiveDay != null) {
                            Text(
                                text = String.format(
                                    strings.shootingStatsMostActiveDay,
                                    mostActiveDay.dayLabel,
                                    mostActiveDay.itemCount
                                ),
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFE9D5FF).copy(alpha = 0.9f) else Color(0xFF581C87)
                            )
                        } else {
                            Text(
                                text = "-",
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFE9D5FF).copy(alpha = 0.9f) else Color(0xFF581C87)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- Heatmap (Contribution Graph) ---
        Text(
            text = strings.shootingStatsHeatmapTitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(bottom = 12.dp)
                .then(staggeredRevealModifier(5))
        )
        
        // Heatmap Data (Already calculated in statsData)

    val heatmapColors = if (isDark) {
        val base = MaterialTheme.colorScheme.primary
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            base.copy(alpha = 0.2f),
            base.copy(alpha = 0.35f),
            base.copy(alpha = 0.5f),
            base.copy(alpha = 0.7f),
            base.copy(alpha = 0.85f)
        )
    } else {
        listOf(
            Color(0xFFF3F4F6),
            Color(0xFFD1FAE5),
            Color(0xFF6EE7B7),
            Color(0xFF34D399),
            Color(0xFF059669),
            Color(0xFF047857)
        )
    }
        
        if (selectedPeriod == "Month") {
            // Transposed Grid (Calendar Style): Column of Rows
            // 6 Weeks x 7 Days
            val calendarGrid = remember(targetYear, targetMonth) {
                 val grid = mutableListOf<Date>()
                 val cal = Calendar.getInstance()
                 cal.set(Calendar.YEAR, targetYear)
                 cal.set(Calendar.MONTH, targetMonth)
                 cal.set(Calendar.DAY_OF_MONTH, 1)
                 
                 // Find start of week (Sunday)
                 val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                 cal.add(Calendar.DATE, -(dayOfWeek - 1))
                 
                 // Add 42 days (6 weeks)
                 for (i in 0 until 42) {
                     grid.add(cal.time)
                     cal.add(Calendar.DATE, 1)
                 }
                 grid
            }
            
            // Pre-compute month grid colors to avoid per-cell Calendar/DateFormat objects
            val monthGridColors = remember(targetYear, targetMonth, heatmapData, heatmapColors) {
                calendarGrid.map { cellDate ->
                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cellDate)
                    val count = heatmapData[dateKey] ?: 0
                    when {
                        count == 0 -> heatmapColors[0]
                        count <= 2 -> heatmapColors[1]
                        count <= 5 -> heatmapColors[2]
                        count <= 10 -> heatmapColors[3]
                        count <= 20 -> heatmapColors[4]
                        else -> heatmapColors[5]
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(6)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    for (w in 0 until 6) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            for (d in 0 until 7) {
                                val index = w * 7 + d
                                if (index < monthGridColors.size) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(monthGridColors[index])
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
        } else {
            // Year View: Keep Horizontal Scroll but maybe optimized?
            // Use existing logic for Year (Horizontal)
            val weeks = 53
            val scrollState = rememberScrollState()
            LaunchedEffect(selectedPeriod, targetYear) {
                 scrollState.scrollTo(0)
            }

            val weekCellSize = 10.dp
            val weekColumnWidth = 16.dp
            val weekRowSpacing = 3.dp

            // Pre-compute year grid colors to avoid per-cell Calendar/DateFormat objects (371 cells)
            val yearGridColors = remember(targetYear, heatmapData, heatmapColors) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, targetYear)
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                cal.add(Calendar.DATE, -(dayOfWeek - 1))
                
                val colors = mutableListOf<Color>()
                for (w in 0 until weeks) {
                    for (d in 0 until 7) {
                        val dateKey = sdf.format(cal.time)
                        val count = heatmapData[dateKey] ?: 0
                        colors.add(when {
                            count == 0 -> heatmapColors[0]
                            count <= 2 -> heatmapColors[1]
                            count <= 5 -> heatmapColors[2]
                            count <= 10 -> heatmapColors[3]
                            count <= 20 -> heatmapColors[4]
                            else -> heatmapColors[5]
                        })
                        cal.add(Calendar.DATE, 1)
                    }
                }
                colors
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .then(staggeredRevealModifier(6))
                    .heightIn(min = 90.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                for (w in 0 until weeks) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(weekRowSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(weekColumnWidth)
                    ) {
                        for (dayOfWeek in 0 until 7) {
                            val colorIndex = w * 7 + dayOfWeek
                            Box(
                                modifier = Modifier
                                    .size(weekCellSize)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(yearGridColors[colorIndex])
                            )
                        }
                    }
                }
            }

            val startCal = remember(targetYear, selectedPeriod) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    add(Calendar.DATE, -(dayOfWeek - 1))
                }
            }
            val monthLabelIndices = remember(targetYear, selectedPeriod) {
                listOf(2, 5, 8, 11).map { monthIndex ->
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, targetYear)
                    cal.set(Calendar.MONTH, monthIndex)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val diffDays = ((cal.timeInMillis - startCal.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
                    val weekIndex = (diffDays / 7).coerceIn(0, weeks - 1)
                    weekIndex to (monthIndex + 1).toString()
                }.toMap()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                for (w in 0 until weeks) {
                    Box(
                        modifier = Modifier
                            .width(weekColumnWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        monthLabelIndices[w]?.let { label ->
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
        
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .then(staggeredRevealModifier(7)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.shootingStatsHeatmapLess,
                fontSize = 10.sp, 
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            heatmapColors.forEach {
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(it))
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = strings.shootingStatsHeatmapMore,
                fontSize = 10.sp, 
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ShootingStatsPage(
    allMedia: List<MediaItem>,
    likedIds: Set<Long>,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showGlobePage by rememberSaveable { mutableStateOf(false) }
    if (showGlobePage) {
        GlobeViewPage(
            allMedia = allMedia,
            onBack = { showGlobePage = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = strings.shootingStatsTitle,
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(6.dp))
        ShootingStatsView(
            allMedia = allMedia,
            likedIds = likedIds,
            onOpenGlobe = { showGlobePage = true }
        )
    }
}

@Composable
private fun UniversalDataSection(
    cameraTop5: List<Pair<String, Int>>,
    locationTop5: List<Pair<String, Int>>,
    monthly12: List<Pair<String, Int>>,
    keywordTop30: List<Pair<String, Int>>,
    photoTotal: Int,
    unifiedCovered: Int,
    currentLang: String,
    isDark: Boolean,
    onOpenGlobe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage = LocalAppLanguage.current
    val newPageStrings = getNewPageStrings(appLanguage)
    val aiPromptStrings = when (appLanguage) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> newPageStrings
        else -> NewPageStringsEn
    }
    val clipboard = LocalClipboardManager.current
    val responseLanguageHint = if (
        appLanguage == AppLanguage.SimplifiedChinese || appLanguage == AppLanguage.TraditionalChinese
    ) {
        null
    } else {
        val languageName = when (appLanguage) {
            AppLanguage.English -> "English"
            AppLanguage.Korean -> "Korean"
            AppLanguage.Japanese -> "Japanese"
            AppLanguage.French -> "French"
            AppLanguage.German -> "German"
            AppLanguage.Italian -> "Italian"
            AppLanguage.Portuguese -> "Portuguese"
            AppLanguage.Spanish -> "Spanish"
            AppLanguage.Russian -> "Russian"
            AppLanguage.Thai -> "Thai"
            AppLanguage.Vietnamese -> "Vietnamese"
            AppLanguage.Indonesian -> "Indonesian"
            AppLanguage.Arabic -> "Arabic"
            AppLanguage.Ukrainian -> "Ukrainian"
            AppLanguage.Polish -> "Polish"
            AppLanguage.Czech -> "Czech"
            AppLanguage.Dutch -> "Dutch"
            AppLanguage.Swedish -> "Swedish"
            AppLanguage.Danish -> "Danish"
            AppLanguage.Norwegian -> "Norwegian"
            AppLanguage.Finnish -> "Finnish"
            AppLanguage.Greek -> "Greek"
            AppLanguage.Romanian -> "Romanian"
            AppLanguage.Hungarian -> "Hungarian"
            AppLanguage.Turkish -> "Turkish"
            AppLanguage.Malay -> "Malay"
            AppLanguage.Hebrew -> "Hebrew"
            else -> "English"
        }
        "Please write the final critique in $languageName."
    }
    var copyHintVisible by remember { mutableStateOf(false) }
    var copyHintToken by remember { mutableIntStateOf(0) }
    val unifiedCoverageText = if (photoTotal > 0) {
        val ratio = (unifiedCovered * 100f / photoTotal)
        val ratioText = String.format(Locale.US, "%.1f", ratio)
        newPageStrings.statsCoverageLabel(ratioText)
    } else {
        newPageStrings.statsCoverageLabel("0.0")
    }
    LaunchedEffect(copyHintToken) {
        if (copyHintToken <= 0) return@LaunchedEffect
        copyHintVisible = true
        delay(3200)
        copyHintVisible = false
    }
    var revealCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        if (revealCount >= 4) return@LaunchedEffect
        revealCount = 0
        repeat(4) { idx ->
            delay(70)
            revealCount = idx + 1
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = newPageStrings.statsGeneralDataTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB))
                    .clickable {
                        val promptCoverageText = if (photoTotal > 0) {
                            val ratio = (unifiedCovered * 100f / photoTotal)
                            val ratioText = String.format(Locale.US, "%.1f", ratio)
                            aiPromptStrings.statsCoverageLabel(ratioText)
                        } else {
                            aiPromptStrings.statsCoverageLabel("0.0")
                        }
                        val prompt = buildAiRoastPrompt(
                            cameraTop5 = cameraTop5,
                            locationTop5 = locationTop5,
                            monthly12 = monthly12,
                            keywordTop30 = keywordTop30,
                            coverageText = promptCoverageText,
                            newPageStrings = aiPromptStrings,
                            responseLanguageHint = responseLanguageHint
                        )
                        clipboard.setText(AnnotatedString(prompt))
                        copyHintToken += 1
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = newPageStrings.statsCopyAiPromptButton,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = newPageStrings.statsCoverageTip(unifiedCoverageText),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        AnimatedVisibility(
            visible = copyHintVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF3F4F6))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = aiPromptStrings.statsCopyHint,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedVisibility(
                visible = revealCount >= 1,
                enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                    slideInVertically(
                        initialOffsetY = { it / 7 },
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
            ) {
            UniversalCard(
                title = newPageStrings.statsFrequentCamerasTitle,
                isDark = isDark,
                background = if (isDark) Color(0xFF0F2742) else Color(0xFFEFF6FF),
                titleColor = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1E40AF)
            ) {
                TopBars(
                    items = cameraTop5,
                    emptyText = newPageStrings.statsProgressiveFillHint,
                    isDark = isDark,
                    barColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                )
            }
            }

            AnimatedVisibility(
                visible = revealCount >= 2,
                enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                    slideInVertically(
                        initialOffsetY = { it / 7 },
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
            ) {
            UniversalCard(
                title = newPageStrings.statsFrequentPlacesTitle,
                isDark = isDark,
                background = if (isDark) Color(0xFF3A2A0C) else Color(0xFFFFF7E6),
                titleColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                headerAction = {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = "Globe",
                        tint = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onOpenGlobe() }
                    )
                }
            ) {
                TopBars(
                    items = locationTop5,
                    emptyText = newPageStrings.statsProgressiveFillHint,
                    isDark = isDark,
                    barColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                )
            }
            }

            AnimatedVisibility(
                visible = revealCount >= 3,
                enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                    slideInVertically(
                        initialOffsetY = { it / 7 },
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
            ) {
            UniversalCard(
                title = newPageStrings.statsLast12MonthsTrendTitle,
                isDark = isDark,
                background = if (isDark) Color(0xFF0D3A34) else Color(0xFFECFDF5),
                titleColor = if (isDark) Color(0xFF99F6E4) else Color(0xFF047857)
            ) {
                MonthlyLineChart(
                    monthly12 = monthly12,
                    isDark = isDark,
                    strokeColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E),
                    dotColor = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0D9488)
                )
            }
            }

            AnimatedVisibility(
                visible = revealCount >= 4,
                enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                    slideInVertically(
                        initialOffsetY = { it / 7 },
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
            ) {
            UniversalCard(
                title = newPageStrings.statsAiTagCloudTitle,
                isDark = isDark,
                background = if (isDark) Color(0xFF2C1C48) else Color(0xFFF5F3FF),
                titleColor = if (isDark) Color(0xFFE9D5FF) else Color(0xFF6D28D9)
            ) {
                if (keywordTop30.isEmpty()) {
                    Text(
                        text = newPageStrings.statsNoCloudHint,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                } else {
                    RectWordCloud(items = keywordTop30, isDark = isDark)
                }
            }
            }
        }
    }
}

private fun buildAiRoastPrompt(
    cameraTop5: List<Pair<String, Int>>,
    locationTop5: List<Pair<String, Int>>,
    monthly12: List<Pair<String, Int>>,
    keywordTop30: List<Pair<String, Int>>,
    coverageText: String,
    newPageStrings: NewPageStrings,
    responseLanguageHint: String? = null
): String {
    fun formatPairs(items: List<Pair<String, Int>>, empty: String): String {
        if (items.isEmpty()) return empty
        return items.joinToString("\n") { "- ${it.first}: ${it.second}" }
    }

    val rawData = buildString {
        appendLine(newPageStrings.statsPromptSectionCoverage)
        appendLine(coverageText)
        appendLine()
        appendLine(newPageStrings.statsPromptSectionCameraTop5)
        appendLine(formatPairs(cameraTop5, newPageStrings.statsPromptNoDataLine))
        appendLine()
        appendLine(newPageStrings.statsPromptSectionLocationTop5)
        appendLine(formatPairs(locationTop5, newPageStrings.statsPromptNoDataLine))
        appendLine()
        appendLine(newPageStrings.statsPromptSectionTrend)
        if (monthly12.isEmpty()) {
            appendLine(newPageStrings.statsPromptNoDataLine)
        } else {
            monthly12.forEach { appendLine("- ${it.first}: ${it.second}") }
        }
        appendLine()
        appendLine(newPageStrings.statsPromptSectionTagCloudTop30)
        appendLine(formatPairs(keywordTop30, newPageStrings.statsPromptNoDataLine))
    }

    return """
${newPageStrings.statsPromptIntro1}

${newPageStrings.statsPromptIntro2}

${newPageStrings.statsPromptIntro3}

${responseLanguageHint ?: ""}

${newPageStrings.statsPromptSourceLead}
$rawData
""".trimIndent()
}

@Composable
private fun UniversalCard(
    title: String,
    isDark: Boolean,
    background: Color,
    titleColor: Color,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            if (headerAction != null) {
                headerAction()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun TopBars(
    items: List<Pair<String, Int>>,
    emptyText: String,
    isDark: Boolean,
    barColor: Color
) {
    if (items.isEmpty()) {
        Text(
            text = emptyText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        return
    }
    val maxValue = items.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    var barsReady by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (barsReady) return@LaunchedEffect
        delay(80)
        barsReady = true
    }
    val barProgress by animateFloatAsState(
        targetValue = if (barsReady) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "topBarsReveal"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (name, value) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = value.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(((value / maxValue).coerceIn(0f, 1f) * barProgress).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyLineChart(
    monthly12: List<Pair<String, Int>>,
    isDark: Boolean,
    strokeColor: Color,
    dotColor: Color
) {
    if (monthly12.isEmpty()) {
        return
    }
    val counts = monthly12.map { it.second }
    val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val axisColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
    var lineReady by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (lineReady) return@LaunchedEffect
        delay(90)
        lineReady = true
    }
    val lineProgress by animateFloatAsState(
        targetValue = if (lineReady) 1f else 0f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "monthlyLineReveal"
    )

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
    ) {
        val left = 6.dp.toPx()
        val right = size.width - 6.dp.toPx()
        val labelTop = 6.dp.toPx()
        val top = 26.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val w = right - left
        val h = bottom - top
        if (w <= 0f || h <= 0f || monthly12.size < 2) return@Canvas

        drawLine(
            color = axisColor,
            start = androidx.compose.ui.geometry.Offset(left, bottom),
            end = androidx.compose.ui.geometry.Offset(right, bottom),
            strokeWidth = 1.dp.toPx()
        )

        val points = monthly12.mapIndexed { index, (_, count) ->
            val x = left + (w * index / (monthly12.size - 1).toFloat())
            val y = bottom - (h * (count.toFloat() / maxCount.toFloat()))
            androidx.compose.ui.geometry.Offset(x, y)
        }
        val fullPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }
        val visiblePath = Path()
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(fullPath, false)
        val revealLength = pathMeasure.length * lineProgress.coerceIn(0f, 1f)
        pathMeasure.getSegment(0f, revealLength, visiblePath, true)
        drawPath(
            path = visiblePath,
            color = strokeColor,
            style = Stroke(width = 2.4.dp.toPx())
        )
        val visiblePointProgress = lineProgress * (points.size - 1).coerceAtLeast(1)
        points.forEachIndexed { index, p ->
            val local = (visiblePointProgress - index).coerceIn(0f, 1f)
            if (local <= 0f) return@forEachIndexed
            drawCircle(
                color = dotColor,
                radius = 2.2.dp.toPx() * local.coerceAtLeast(0.35f),
                center = p
            )
        }

        val maxIndex = counts.indices.maxByOrNull { counts[it] } ?: 0
        val minIndex = counts.indices.minByOrNull { counts[it] } ?: 0
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 11.sp.toPx()
            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        }
        val maxP = points[maxIndex]
        val minP = points[minIndex]
        val baseOffset = 10.dp.toPx()
        var maxLabelY = (maxP.y - baseOffset).coerceIn(labelTop + 10.dp.toPx(), maxP.y - 2.dp.toPx())
        var minLabelY = (minP.y - baseOffset).coerceIn(labelTop + 10.dp.toPx(), minP.y - 2.dp.toPx())
        if (kotlin.math.abs(maxP.x - minP.x) < 30.dp.toPx() && kotlin.math.abs(maxLabelY - minLabelY) < 14.dp.toPx()) {
            minLabelY = (minLabelY - 14.dp.toPx()).coerceIn(labelTop + 10.dp.toPx(), minP.y - 2.dp.toPx())
        }
        if (lineProgress > 0.98f) {
            drawContext.canvas.nativeCanvas.drawText(
                counts[maxIndex].toString(),
                maxP.x,
                maxLabelY,
                labelPaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                counts[minIndex].toString(),
                minP.x,
                minLabelY,
                labelPaint
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val firstLabel = monthly12.firstOrNull()?.first?.substringAfter("-") ?: "--"
        val lastLabel = monthly12.lastOrNull()?.first?.substringAfter("-") ?: "--"
        Text(
            text = firstLabel,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        Text(
            text = lastLabel,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun RectWordCloud(
    items: List<Pair<String, Int>>,
    isDark: Boolean
) {
    val maxCount = items.maxOfOrNull { it.second } ?: 1
    val words = remember(items, maxCount) {
        items
            .sortedByDescending { it.second }
            .map { (word, count) ->
                val t = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                // Exponential stretch to make size hierarchy obvious
                val boosted = t * t
                val sizeSp = 12f + boosted * 30f
                Triple(word, sizeSp, t)
            }
    }
    val cloudHeight = remember(items.size) {
        (170 + (items.size * 2)).coerceIn(170, 250).dp
    }

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(cloudHeight)
    ) {
        if (words.isEmpty()) return@Canvas
        val margin = 6.dp.toPx()
        val wordPadding = 3.dp.toPx()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxW = size.width
        val maxH = size.height
        val placed = mutableListOf<android.graphics.RectF>()
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
        }

        fun isMostlyCjk(text: String): Boolean {
            if (text.isBlank()) return false
            val cjkCount = text.count { ch ->
                Character.UnicodeScript.of(ch.code) in setOf(
                    Character.UnicodeScript.HAN,
                    Character.UnicodeScript.HIRAGANA,
                    Character.UnicodeScript.KATAKANA,
                    Character.UnicodeScript.HANGUL
                )
            }
            return cjkCount >= (text.length * 0.6f)
        }

        words.forEachIndexed { index, (word, sizeSp, t) ->
            textPaint.textSize = sizeSp.sp.toPx()
            textPaint.color = if (isDark) {
                android.graphics.Color.argb(
                    (170 + (t * 85f).toInt()).coerceIn(0, 255),
                    236, 210, 255
                )
            } else {
                val palette = intArrayOf(
                    android.graphics.Color.parseColor("#2563EB"),
                    android.graphics.Color.parseColor("#0EA5E9"),
                    android.graphics.Color.parseColor("#14B8A6"),
                    android.graphics.Color.parseColor("#F59E0B"),
                    android.graphics.Color.parseColor("#F97316"),
                    android.graphics.Color.parseColor("#8B5CF6")
                )
                palette[index % palette.size]
            }
            textPaint.typeface = if (t > 0.72f) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

            val fm = textPaint.fontMetrics
            val textH = max(1f, fm.descent - fm.ascent)
            val useVertical = isMostlyCjk(word) && t < 0.62f && (index % 3 == 0)
            val chars = if (useVertical) word.toCharArray().map { it.toString() } else emptyList()
            val lineGap = if (useVertical) textH * 0.08f else 0f
            val textW = if (useVertical) {
                max(1f, chars.maxOfOrNull { textPaint.measureText(it) } ?: textH * 0.8f)
            } else {
                max(1f, textPaint.measureText(word))
            }
            val occupiedH = if (useVertical) {
                chars.size * textH + (chars.size - 1).coerceAtLeast(0) * lineGap
            } else {
                textH
            }

            var chosenRect: android.graphics.RectF? = null
            var drawStartY = 0f

            // Spiral placement from center outwards to emulate real cloud fill
            for (step in 0..1800) {
                val theta = step * 0.34f + index * 0.18f
                val radius = 1.45f * sqrt(step.toFloat()) * 3.4f
                val x = centerX + cos(theta) * radius - textW / 2f
                val y = centerY + sin(theta) * radius - occupiedH / 2f
                val rect = android.graphics.RectF(
                    x - wordPadding,
                    y - wordPadding,
                    x + textW + wordPadding,
                    y + occupiedH + wordPadding
                )
                if (rect.left < margin || rect.top < margin || rect.right > maxW - margin || rect.bottom > maxH - margin) continue
                if (placed.none { android.graphics.RectF.intersects(it, rect) }) {
                    chosenRect = rect
                    drawStartY = y - fm.ascent
                    break
                }
            }

            if (chosenRect != null) {
                placed.add(chosenRect)
                val drawX = chosenRect.left + wordPadding
                if (useVertical) {
                    var baseline = drawStartY
                    chars.forEach { ch ->
                        drawContext.canvas.nativeCanvas.drawText(ch, drawX, baseline, textPaint)
                        baseline += textH + lineGap
                    }
                } else {
                    drawContext.canvas.nativeCanvas.drawText(word, drawX, drawStartY, textPaint)
                }
            }
        }
    }
}

// Helper to calculate date for grid cell (Year view only now)
fun getDateForGrid(year: Int, month: Int, period: String, weekIndex: Int, dayIndex: Int, totalWeeks: Int): Date {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, Calendar.JANUARY)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    cal.add(Calendar.DATE, -(dayOfWeek - 1))
    cal.add(Calendar.DATE, weekIndex * 7 + dayIndex)
    return cal.time
}












