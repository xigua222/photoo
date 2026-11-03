package com.voicelike.app

import android.content.Context
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// One organize range: time AND album AND kind, each defaulting to "All".
// time is either a preset (Today / OnThisDay / RandomDay) or a picked month/day;
// RandomDay keeps its rolled date in [day] so the pill can show it.
data class MediaFilterCriteria(
    val folder: String = "All",
    val type: String = "All",
    val month: String = "All",
    val day: String = "All",
    val timePreset: String = OrganizeTimePreset.ALL,
    // A photo set brought in from Insights: only these ids count. Temporary, never saved.
    val collectionTitle: String? = null,
    val collectionIds: Set<Long>? = null
) {
    val isActive: Boolean get() = this != MediaFilterCriteria()
    val isCollection: Boolean get() = collectionIds != null

    fun withoutCollection() = copy(collectionTitle = null, collectionIds = null)
    val hasTimeCondition: Boolean
        get() = timePreset != OrganizeTimePreset.ALL || month != "All" || day != "All"

    // Any range edit leaves the Insights collection, like iOS.
    fun withTime(timePreset: String = OrganizeTimePreset.ALL, month: String = "All", day: String = "All") =
        copy(timePreset = timePreset, month = month, day = day, collectionTitle = null, collectionIds = null)

    fun withoutTime() = withTime()
}

object OrganizeTimePreset {
    const val ALL = "All"
    const val TODAY = "Today"
    const val ON_THIS_DAY = "OnThisDay"
    const val RANDOM_DAY = "RandomDay"
}

// Kinds shown in the range panel, in panel order. Only kinds Android can tell
// apart from MediaStore data are listed.
object OrganizeKind {
    const val ALL = "All"
    const val PHOTO = "Image"
    const val LIVE = "Live"
    const val VIDEO = "Video"
    const val SCREENSHOT = "Screenshot"
    const val SCREEN_RECORDING = "ScreenRecording"
    const val PANORAMA = "Panorama"
    const val ANIMATED = "Animated"
    const val RAW = "Raw"

    val panelOrder = listOf(ALL, PHOTO, LIVE, VIDEO, SCREENSHOT, SCREEN_RECORDING, PANORAMA, ANIMATED, RAW)
}

// With a range the whole range is one deck (capped), otherwise one batch.
const val ORGANIZE_RANGE_QUEUE_CAP = 2000
const val ORGANIZE_BATCH_SIZE_MIN = 16
// 60 was the top of the slider before 4.0; keep it so those preferences survive the update.
const val ORGANIZE_BATCH_SIZE_MAX = 60
const val ORGANIZE_BATCH_SIZE_DEFAULT = 16

fun organizeQueueLimit(batchSize: Int, criteria: MediaFilterCriteria): Int =
    if (criteria.isActive) maxOf(ORGANIZE_RANGE_QUEUE_CAP, batchSize) else batchSize

// Hidden albums sit out of organizing unless that exact album is the selected one.
fun effectiveHiddenFolders(hiddenFolders: Set<String>, criteria: MediaFilterCriteria): Set<String> =
    if (criteria.folder != "All" && criteria.folder in hiddenFolders) hiddenFolders - criteria.folder else hiddenFolders

internal data class HomeFeedSnapshot(
    val displayMedia: List<MediaItem>,
    val isFullAlbumDone: Boolean,
    val remainingPendingCount: Int
)

internal data class FilterSheetUiModel(
    val folderList: List<FilterFolderData>,
    val typeCounts: Map<String, Int>,
    val featuredDays: List<FeaturedDayEntry>
)

internal data class CatalogSyncResolution(
    val displayMedia: List<MediaItem>,
    val isFullAlbumDone: Boolean,
    val currentBatchTotal: Int,
    val shouldAlignCurrentDisplay: Boolean,
    val preservesBatchProgress: Boolean
)

internal class GlobalMediaCatalog
internal class SwipeWindow(@Suppress("UNUSED_PARAMETER") catalog: GlobalMediaCatalog)

private data class MediaLocalDateCacheEntry(
    val dateAdded: Long,
    val monthValue: Int,
    val dayValue: Int
)

private object MediaLocalDateCache {
    private val cache = ConcurrentHashMap<Long, MediaLocalDateCacheEntry>()

    fun monthValue(item: MediaItem): Int = getOrPut(item).monthValue

    fun dayValue(item: MediaItem): Int = getOrPut(item).dayValue

    private fun getOrPut(item: MediaItem): MediaLocalDateCacheEntry {
        val cached = cache[item.id]
        if (cached != null && cached.dateAdded == item.dateAdded) {
            return cached
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = item.dateAdded * 1000L
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return MediaLocalDateCacheEntry(
            dateAdded = item.dateAdded,
            monthValue = year * 100 + month,
            dayValue = year * 10000 + month * 100 + day
        ).also { cache[item.id] = it }
    }
}

private fun parseMonthFilterValue(key: String): Int? {
    if (key.length != 7 || key[4] != '-') return null
    val year = key.substring(0, 4).toIntOrNull() ?: return null
    val month = key.substring(5, 7).toIntOrNull() ?: return null
    if (month !in 1..12) return null
    return year * 100 + month
}

private fun parseDayFilterValue(key: String): Int? {
    if (key.length != 10 || key[4] != '-' || key[7] != '-') return null
    val year = key.substring(0, 4).toIntOrNull() ?: return null
    val month = key.substring(5, 7).toIntOrNull() ?: return null
    val day = key.substring(8, 10).toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    return year * 10000 + month * 100 + day
}

private fun formatDayFilterValue(dayValue: Int): String {
    val year = dayValue / 10000
    val month = (dayValue / 100) % 100
    val day = dayValue % 100
    return "%04d-%02d-%02d".format(Locale.US, year, month, day)
}

private fun matchesMonthFilter(item: MediaItem, monthKey: String): Boolean {
    val monthValue = parseMonthFilterValue(monthKey) ?: return false
    return MediaLocalDateCache.monthValue(item) == monthValue
}

private fun matchesDayFilter(item: MediaItem, dayKey: String): Boolean {
    val dayValue = parseDayFilterValue(dayKey) ?: return false
    return MediaLocalDateCache.dayValue(item) == dayValue
}

internal fun todayDayValue(nowMs: Long = System.currentTimeMillis()): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = nowMs
    return calendar.get(Calendar.YEAR) * 10000 +
        (calendar.get(Calendar.MONTH) + 1) * 100 +
        calendar.get(Calendar.DAY_OF_MONTH)
}

internal fun mediaDayValue(item: MediaItem): Int = MediaLocalDateCache.dayValue(item)

internal fun mediaMonthValue(item: MediaItem): Int = MediaLocalDateCache.monthValue(item)

internal fun dayKeyOf(dayValue: Int): String = formatDayFilterValue(dayValue)

internal fun monthKeyOf(monthValue: Int): String =
    "%04d-%02d".format(Locale.US, monthValue / 100, monthValue % 100)

internal fun mediaMatchesTime(item: MediaItem, criteria: MediaFilterCriteria, today: Int = todayDayValue()): Boolean {
    return when (criteria.timePreset) {
        OrganizeTimePreset.TODAY -> MediaLocalDateCache.dayValue(item) == today
        // Same month and day in earlier years; today itself does not count.
        OrganizeTimePreset.ON_THIS_DAY -> {
            val dayValue = MediaLocalDateCache.dayValue(item)
            dayValue % 10000 == today % 10000 && dayValue / 10000 < today / 10000
        }
        else -> when {
            criteria.day != "All" -> matchesDayFilter(item, criteria.day)
            criteria.month != "All" -> matchesMonthFilter(item, criteria.month)
            else -> true
        }
    }
}

private fun mediaPathLower(item: MediaItem): String =
    (item.path.orEmpty() + "/" + item.bucketName).lowercase(Locale.ROOT)

private val SCREENSHOT_MARKERS = listOf("screenshot", "screen_shot", "screencap", "\u622A\u5C4F", "\u622A\u56FE", "\u622A\u5716")
private val SCREEN_RECORDING_MARKERS = listOf("screenrecord", "screen_record", "screen record", "screen-record", "\u5F55\u5C4F", "\u9304\u5C4F")

internal fun isScreenshotMedia(item: MediaItem): Boolean =
    item.type == "photo" && SCREENSHOT_MARKERS.any { mediaPathLower(item).contains(it) }

internal fun isScreenRecordingMedia(item: MediaItem): Boolean =
    item.type == "video" && SCREEN_RECORDING_MARKERS.any { mediaPathLower(item).contains(it) }

internal fun isAnimatedMedia(item: MediaItem): Boolean {
    if (item.type != "photo") return false
    val mime = item.mimeType.lowercase(Locale.ROOT)
    return mime.contains("gif") || item.displayName.lowercase(Locale.ROOT).endsWith(".gif")
}

internal fun isRawMedia(item: MediaItem): Boolean {
    if (item.type != "photo") return false
    return item.mimeType.lowercase(Locale.ROOT).contains("raw") ||
        isRawPhotoFileName(item.displayName.lowercase(Locale.ROOT))
}

// Panoramas are recognised by the camera's PANO file prefix or a very wide frame
// (width and height are compared orientation-free).
internal fun isPanoramaMedia(item: MediaItem): Boolean {
    if (item.type != "photo" || item.isLivePhoto || item.isMotionPhoto) return false
    if (item.displayName.uppercase(Locale.ROOT).startsWith("PANO")) return true
    val longSide = maxOf(item.width, item.height)
    val shortSide = minOf(item.width, item.height)
    return shortSide > 0 && longSide.toFloat() / shortSide >= 2.6f
}

internal fun mediaMatchesKind(item: MediaItem, kind: String): Boolean = when (kind) {
    OrganizeKind.PHOTO -> item.type == "photo" && !item.isLivePhoto && !item.isMotionPhoto && !isScreenshotMedia(item)
    OrganizeKind.LIVE -> item.isLivePhoto || item.isMotionPhoto
    OrganizeKind.VIDEO -> item.type == "video"
    OrganizeKind.SCREENSHOT -> isScreenshotMedia(item)
    OrganizeKind.SCREEN_RECORDING -> isScreenRecordingMedia(item)
    OrganizeKind.PANORAMA -> isPanoramaMedia(item)
    OrganizeKind.ANIMATED -> isAnimatedMedia(item)
    OrganizeKind.RAW -> isRawMedia(item)
    else -> true
}

internal fun applyMediaFilter(source: List<MediaItem>, criteria: MediaFilterCriteria): List<MediaItem> {
    if (!criteria.isActive) return source
    val today = todayDayValue()
    return source.filter { mediaMatchesFilterCriteria(it, criteria, today) }
}

internal fun mediaMatchesFilterCriteria(
    item: MediaItem,
    criteria: MediaFilterCriteria,
    today: Int = todayDayValue()
): Boolean {
    criteria.collectionIds?.let { ids -> return item.id in ids }
    if (criteria.folder != "All" && item.bucketName != criteria.folder) return false
    if (criteria.type != "All" && !mediaMatchesKind(item, criteria.type)) return false
    return mediaMatchesTime(item, criteria, today)
}

internal fun selectPendingMedia(
    allMedia: List<MediaItem>,
    processedIds: Set<Long>,
    criteria: MediaFilterCriteria
): List<MediaItem> {
    return applyMediaFilter(allMedia, criteria)
        .filter { !processedIds.contains(it.id) }
        .distinctBy { it.id }
}

internal fun selectProcessedMedia(
    allMedia: List<MediaItem>,
    processedIds: Set<Long>,
    criteria: MediaFilterCriteria
): List<MediaItem> {
    return applyMediaFilter(allMedia, criteria)
        .filter { processedIds.contains(it.id) }
        .distinctBy { it.id }
}

internal fun buildFilterSheetUiModel(
    sourceMedia: List<MediaItem>,
    pendingTrashIds: Set<Long>,
    processedIds: Set<Long>,
    hiddenFolders: Set<String>,
    prioritizedFolders: Set<String>,
    month: String,
    day: String
): FilterSheetUiModel {
    val filterCountSource = selectActiveCatalogMedia(
        allMedia = sourceMedia,
        pendingTrashIds = pendingTrashIds,
        hiddenFolders = emptySet()
    )
    val dateCriteria = MediaFilterCriteria(folder = "All", type = "All", month = month, day = day)
    val filteredMediaForCounts = applyMediaFilter(filterCountSource, dateCriteria).distinctBy { it.id }
    val remainingMediaForCounts = selectPendingMedia(filterCountSource, processedIds, dateCriteria).distinctBy { it.id }
    val pendingVisibleMedia = remainingMediaForCounts.filter { !hiddenFolders.contains(it.bucketName) }
    val totalVisibleMedia = filteredMediaForCounts.filter { !hiddenFolders.contains(it.bucketName) }
    val groupedTotal = filteredMediaForCounts.groupBy { it.bucketName }
    val groupedRemaining = remainingMediaForCounts.groupBy { it.bucketName }
    val sortedGroups = groupedTotal.entries
        .asSequence()
        .filter { it.value.isNotEmpty() }
        .sortedWith(
            compareByDescending<Map.Entry<String, List<MediaItem>>> {
                prioritizedFolders.contains(it.key)
            }.thenByDescending { it.value.size }
        )
        .toList()

    val folderList = buildList {
        if (filteredMediaForCounts.isNotEmpty()) {
            add(
                FilterFolderData(
                    name = "All",
                    count = pendingVisibleMedia.size,
                    totalCount = totalVisibleMedia.size,
                    previews = pendingVisibleMedia.take(7)
                )
            )
        } else {
            add(
                FilterFolderData(
                    name = "All",
                    count = 0,
                    totalCount = 0,
                    previews = emptyList()
                )
            )
        }

        sortedGroups.forEach { (name, totalItems) ->
            val remainingItems = groupedRemaining[name]
            add(
                FilterFolderData(
                    name = name,
                    count = remainingItems?.size ?: 0,
                    totalCount = totalItems.size,
                    previews = (remainingItems ?: totalItems).take(7)
                )
            )
        }
    }

    val typeCounts = mutableMapOf(
        "All" to totalVisibleMedia.size,
        "Image" to 0,
        "Video" to 0,
        "Live" to 0
    )
    totalVisibleMedia.forEach { item ->
        when {
            item.isLivePhoto || item.isMotionPhoto -> typeCounts["Live"] = (typeCounts["Live"] ?: 0) + 1
            item.type == "video" -> typeCounts["Video"] = (typeCounts["Video"] ?: 0) + 1
            else -> typeCounts["Image"] = (typeCounts["Image"] ?: 0) + 1
        }
    }

    val featuredDays = linkedMapOf<Int, Int>()
    totalVisibleMedia.forEach { item ->
        val key = MediaLocalDateCache.dayValue(item)
        featuredDays[key] = (featuredDays[key] ?: 0) + 1
    }

    return FilterSheetUiModel(
        folderList = folderList,
        typeCounts = typeCounts,
        featuredDays = featuredDays.entries
            .map { FeaturedDayEntry(dayKey = formatDayFilterValue(it.key), count = it.value) }
            .sortedByDescending { it.count }
    )
}

internal fun collectStatsWarmupCandidates(
    context: Context,
    media: List<MediaItem>,
    maxSample: Int = 1400,
    maxWarmup: Int = 420
): List<MediaItem> {
    val photos = media
        .asSequence()
        .filter { it.type == "photo" }
        .sortedByDescending { it.dateAdded }
        .take(maxSample)
        .toList()
    if (photos.isEmpty()) return emptyList()

    val snapshot = MediaInsightRepository.getMany(context, photos.map { it.id })
    return photos
        .filter { item ->
            val cached = snapshot[item.id]
            val cameraMissing = cached?.camera.isNullOrBlank() || cached?.camera == "--"
            val locationMissing = cached?.location.isNullOrBlank() || cached?.location.equals("Unknown Location", ignoreCase = true)
            val keywordMissing = cached?.keywords?.isEmpty() != false
            cameraMissing || locationMissing || keywordMissing
        }
        .take(maxWarmup)
}

internal fun buildMovedMediaItem(
    sourceItem: MediaItem,
    targetFolderPath: String,
    targetBucketName: String
): MediaItem {
    val fallbackExtension = when {
        sourceItem.type == "video" -> "mp4"
        sourceItem.mimeType.contains("png", ignoreCase = true) -> "png"
        else -> "jpg"
    }
    val sourcePath = sourceItem.path.orEmpty()
    val fileName = sourcePath
        .takeIf { it.isNotBlank() }
        ?.let { File(it).name }
        ?.takeIf { it.isNotBlank() }
        ?: sourceItem.displayName
            .takeIf { it.isNotBlank() }
        ?: "${sourceItem.id}.$fallbackExtension"
    val normalizedTarget = targetFolderPath.trim().trimEnd('/') + "/"
    return sourceItem.copy(
        path = normalizedTarget + fileName,
        bucketName = targetBucketName
    )
}

private fun stableRandomSortKey(itemId: Long, seed: Long): Long {
    var mixed = itemId xor seed
    mixed = (mixed xor (mixed ushr 33)) * -49064778989728563L
    mixed = (mixed xor (mixed ushr 33)) * -4265267296055464877L
    return mixed xor (mixed ushr 33)
}

internal fun orderMediaForQueue(
    media: List<MediaItem>,
    queueOrder: String,
    randomOrderSeed: Long? = null
): List<MediaItem> {
    return when (queueOrder) {
        "time_asc" -> media.sortedWith(compareBy<MediaItem> { it.dateAdded }.thenBy { it.id })
        "time", "time_desc" -> media.sortedWith(
            compareByDescending<MediaItem> { it.dateAdded }.thenByDescending { it.id }
        )
        // Unknown sizes go last; ties are newest first.
        "size_desc" -> media.sortedWith(
            compareBy<MediaItem> { if (it.size > 0L) 0 else 1 }
                .thenByDescending { it.size }
                .thenByDescending { it.dateAdded }
                .thenByDescending { it.id }
        )
        "size_asc" -> media.sortedWith(
            compareBy<MediaItem> { if (it.size > 0L) 0 else 1 }
                .thenBy { it.size }
                .thenByDescending { it.dateAdded }
                .thenByDescending { it.id }
        )
        else -> {
            val seed = randomOrderSeed ?: return media.shuffled()
            media.sortedWith(compareBy<MediaItem> { stableRandomSortKey(it.id, seed) }.thenBy { it.id })
        }
    }
}

internal fun buildMediaIdSet(media: List<MediaItem>): Set<Long> {
    return media.mapTo(HashSet(maxOf(media.size, 16))) { it.id }
}

private fun buildSimilarGroupKey(group: List<MediaItem>): String {
    return group
        .asSequence()
        .map { it.id }
        .sorted()
        .joinToString(",")
}

internal fun dedupeSimilarGroups(groups: Collection<List<MediaItem>>): List<List<MediaItem>> {
    if (groups.isEmpty()) return emptyList()
    return groups
        .asSequence()
        .map { group -> group to buildSimilarGroupKey(group) }
        .distinctBy { it.second }
        .map { it.first }
        .toList()
}

internal fun computeSimilarGroupsToAppend(
    existingGroups: List<List<MediaItem>>,
    incomingGroups: Collection<List<MediaItem>>
): List<List<MediaItem>> {
    if (incomingGroups.isEmpty()) return emptyList()
    val existingKeys = existingGroups
        .asSequence()
        .mapTo(HashSet(maxOf(existingGroups.size, 16))) { buildSimilarGroupKey(it) }

    return incomingGroups
        .asSequence()
        .map { group -> group to buildSimilarGroupKey(group) }
        .filter { (_, key) -> existingKeys.add(key) }
        .map { it.first }
        .toList()
}

internal class HomeFeedCoordinator(
    @Suppress("UNUSED_PARAMETER") catalog: GlobalMediaCatalog? = null,
    @Suppress("UNUSED_PARAMETER") swipeWindow: SwipeWindow? = null
) {
    private var lastAllMedia: List<MediaItem> = emptyList()
    private var currentDisplay: List<MediaItem> = emptyList()
    private var lastCriteria: MediaFilterCriteria = MediaFilterCriteria()
    private var lastBatchSize: Int = 50
    private var lastPendingCount: Int = 0

    fun configureRollbackBuffer(@Suppress("UNUSED_PARAMETER") size: Int) = Unit

    fun rebuildFromAllMedia(
        allMedia: List<MediaItem>,
        criteria: MediaFilterCriteria,
        batchSize: Int,
        processedIds: Set<Long>
    ): HomeFeedSnapshot {
        lastAllMedia = allMedia
        lastCriteria = criteria
        lastBatchSize = batchSize
        val pending = selectPendingMedia(allMedia, processedIds, criteria)
        lastPendingCount = pending.size
        currentDisplay = pending.take(organizeQueueLimit(batchSize, criteria))
        return HomeFeedSnapshot(
            displayMedia = currentDisplay,
            isFullAlbumDone = pending.isEmpty(),
            remainingPendingCount = lastPendingCount
        )
    }

    fun onCatalogChanged(
        allMedia: List<MediaItem>,
        criteria: MediaFilterCriteria,
        batchSize: Int,
        processedIds: Set<Long>
    ): HomeFeedSnapshot = rebuildFromAllMedia(allMedia, criteria, batchSize, processedIds)

    fun onSwipeCommitted(
        item: MediaItem,
        criteria: MediaFilterCriteria,
        batchSize: Int,
        processedIds: Set<Long>,
        processedAdded: Boolean
    ): HomeFeedSnapshot {
        if (criteria != lastCriteria) {
            return rebuildFromAllMedia(lastAllMedia, criteria, batchSize, processedIds)
        }
        lastCriteria = criteria
        lastBatchSize = batchSize
        currentDisplay = currentDisplay.filterNot { processedIds.contains(it.id) }
        if (processedAdded && mediaMatchesFilterCriteria(item, criteria)) {
            lastPendingCount = (lastPendingCount - 1).coerceAtLeast(0)
        }
        return HomeFeedSnapshot(
            displayMedia = currentDisplay,
            isFullAlbumDone = lastPendingCount == 0,
            remainingPendingCount = lastPendingCount
        )
    }

    fun onUndoRestore(
        item: MediaItem,
        criteria: MediaFilterCriteria,
        batchSize: Int,
        restoresPendingCount: Boolean,
        catalogOverride: List<MediaItem>? = null,
        preserveVisibleOrder: Boolean = false,
        stableVisibleOrderIds: List<Long> = emptyList()
    ): HomeFeedSnapshot {
        lastCriteria = criteria
        lastBatchSize = batchSize
        if (catalogOverride != null) {
            lastAllMedia = catalogOverride
        }

        val latestItem = lastAllMedia.find { it.id == item.id } ?: item
        if (restoresPendingCount && mediaMatchesFilterCriteria(item, criteria)) {
            lastPendingCount += 1
        }
        currentDisplay = if (preserveVisibleOrder) {
            val visibleById = LinkedHashMap<Long, MediaItem>().apply {
                put(latestItem.id, latestItem)
                currentDisplay.forEach { visibleItem ->
                    if (visibleItem.id != latestItem.id) {
                        putIfAbsent(visibleItem.id, visibleItem)
                    }
                }
            }
            buildList {
                if (stableVisibleOrderIds.isNotEmpty()) {
                    stableVisibleOrderIds.forEach { stableId ->
                        visibleById.remove(stableId)?.let(::add)
                    }
                }
                lastAllMedia.forEach { catalogItem ->
                    visibleById.remove(catalogItem.id)?.let(::add)
                }
                if (visibleById.isNotEmpty()) {
                    addAll(visibleById.values)
                }
            }.take(organizeQueueLimit(batchSize, criteria))
        } else {
            (listOf(latestItem) + currentDisplay.filter { it.id != latestItem.id })
                .take(organizeQueueLimit(batchSize, criteria))
        }

        return HomeFeedSnapshot(
            displayMedia = currentDisplay,
            isFullAlbumDone = lastPendingCount == 0,
            remainingPendingCount = lastPendingCount
        )
    }

    fun extendVisibleWindow(
        criteria: MediaFilterCriteria,
        batchSize: Int,
        processedIds: Set<Long>,
        targetSize: Int = batchSize,
        catalogOverride: List<MediaItem>? = null
    ): HomeFeedSnapshot {
        lastCriteria = criteria
        lastBatchSize = batchSize
        if (catalogOverride != null) {
            lastAllMedia = catalogOverride
        }

        val pending = selectPendingMedia(lastAllMedia, processedIds, criteria)
        lastPendingCount = pending.size
        val desiredSize = targetSize
            .coerceAtLeast(currentDisplay.size)
            .coerceAtLeast(1)
        currentDisplay = mergeDisplayKeepingVisible(
            currentDisplay = currentDisplay,
            targetDisplay = pending,
            limit = desiredSize
        )
        return HomeFeedSnapshot(
            displayMedia = currentDisplay,
            isFullAlbumDone = pending.isEmpty(),
            remainingPendingCount = lastPendingCount
        )
    }

    fun remainingCount(criteria: MediaFilterCriteria, processedIds: Set<Long>): Int {
        if (criteria == lastCriteria) {
            return lastPendingCount
        }
        return selectPendingMedia(lastAllMedia, processedIds, criteria).size
    }

    fun alignCurrentDisplay(display: List<MediaItem>) {
        currentDisplay = display.distinctBy { it.id }
    }
}

fun selectActiveCatalogMedia(
    allMedia: List<MediaItem>,
    pendingTrashIds: Set<Long>,
    hiddenFolders: Set<String>
): List<MediaItem> {
    if (allMedia.isEmpty()) return emptyList()
    if (pendingTrashIds.isEmpty() && hiddenFolders.isEmpty()) return allMedia
    return allMedia.asSequence()
        .filter { it.id !in pendingTrashIds }
        .filter { hiddenFolders.isEmpty() || it.bucketName !in hiddenFolders }
        .toList()
}

fun selectVideoCompressionCandidates(activeMedia: List<MediaItem>): List<MediaItem> {
    return activeMedia.filter { it.type == "video" }
}

fun selectImageCompressionCandidates(activeMedia: List<MediaItem>): List<MediaItem> {
    return activeMedia.filter { shouldAnalyzeAsStandardPhoto(it) }
}

internal fun buildMediaScanSignature(items: List<MediaItem>, pendingTrashCount: Int): Long {
    if (items.isEmpty()) return 0L
    var acc = 1125899906842597L
    items.forEach { item ->
        acc = acc * 31L + item.id
        acc = acc * 31L + item.dateAdded
        acc = acc * 31L + item.size
        acc = acc * 31L + item.type.hashCode().toLong()
    }
    acc = acc * 31L + items.size.toLong()
    acc = acc * 31L + pendingTrashCount.toLong()
    return acc
}

internal fun buildCatalogSyncSignature(items: List<MediaItem>): Long {
    if (items.isEmpty()) return 0L
    var acc = 1469598103934665603L
    items.forEach { item ->
        acc = acc * 31L + item.id
        acc = acc * 31L + item.dateAdded
        acc = acc * 31L + item.size
        acc = acc * 31L + item.type.hashCode().toLong()
        acc = acc * 31L + item.bucketName.hashCode().toLong()
        acc = acc * 31L + if (item.isLivePhoto) 1L else 0L
        acc = acc * 31L + if (item.isMotionPhoto) 1L else 0L
    }
    acc = acc * 31L + items.size.toLong()
    return acc
}

internal fun mergeDisplayKeepingVisible(
    currentDisplay: List<MediaItem>,
    targetDisplay: List<MediaItem>,
    limit: Int
): List<MediaItem> {
    if (limit <= 0) return emptyList()
    if (currentDisplay.isEmpty()) return targetDisplay.take(limit)
    if (targetDisplay.isEmpty()) return emptyList()

    val targetById = targetDisplay.associateBy { it.id }
    val merged = mutableListOf<MediaItem>()
    val seen = HashSet<Long>()

    currentDisplay.forEach { item ->
        val latest = targetById[item.id] ?: return@forEach
        if (seen.add(latest.id)) merged.add(latest)
    }
    targetDisplay.forEach { item ->
        if (seen.add(item.id)) merged.add(item)
    }
    return merged.take(limit)
}

internal fun replaceMediaItemById(
    media: List<MediaItem>,
    updated: MediaItem
): List<MediaItem> {
    if (media.isEmpty()) return media
    val index = media.indexOfFirst { it.id == updated.id }
    if (index < 0) return media
    if (media[index] == updated) return media
    return media.toMutableList().also { it[index] = updated }
}

internal fun refreshSortedMediaByDate(
    previousSorted: List<MediaItem>,
    latestMedia: List<MediaItem>
): List<MediaItem> {
    if (latestMedia.isEmpty()) return emptyList()
    if (previousSorted.isEmpty() || previousSorted.size != latestMedia.size) {
        return latestMedia.sortedBy(MediaItem::dateAdded)
    }

    val previousIndexById = HashMap<Long, Int>(previousSorted.size)
    previousSorted.forEachIndexed { index, item ->
        previousIndexById[item.id] = index
    }

    var updatedSorted: MutableList<MediaItem>? = null
    latestMedia.forEach { item ->
        val previousIndex = previousIndexById[item.id] ?: return latestMedia.sortedBy(MediaItem::dateAdded)
        val previousItem = previousSorted[previousIndex]
        if (previousItem.dateAdded != item.dateAdded) {
            return latestMedia.sortedBy(MediaItem::dateAdded)
        }
        if (previousItem != item) {
            if (updatedSorted == null) {
                updatedSorted = previousSorted.toMutableList()
            }
            updatedSorted!![previousIndex] = item
        }
    }

    return updatedSorted ?: previousSorted
}

internal fun computeCatalogSyncResolution(
    homeFeedCoordinator: HomeFeedCoordinator,
    allMedia: List<MediaItem>,
    pendingTrashIds: Set<Long>,
    hiddenFolders: Set<String>,
    criteria: MediaFilterCriteria,
    batchSize: Int,
    processedIds: Set<Long>,
    previousDisplay: List<MediaItem>,
    previousBatchTotal: Int,
    previousBatchConsumed: Int,
    allowPartialWindowPreservation: Boolean = false
): CatalogSyncResolution {
    val activeMedia = selectActiveCatalogMedia(allMedia, pendingTrashIds, hiddenFolders)
    val pending = selectPendingMedia(activeMedia, processedIds, criteria)
    val previousIds = previousDisplay.map { it.id }.toSet()
    val synced = homeFeedCoordinator.onCatalogChanged(
        activeMedia,
        criteria,
        batchSize,
        processedIds
    )
    val syncedDistinct = synced.displayMedia.distinctBy { it.id }
    val pendingById = pending.associateBy { it.id }
    val preservedCurrentDisplay = previousDisplay.mapNotNull { pendingById[it.id] }
    val canKeepCurrentDisplay =
        previousDisplay.isNotEmpty() &&
            preservedCurrentDisplay.size == previousIds.size
    val hasPartialWindowOverlap =
        allowPartialWindowPreservation &&
            previousDisplay.isNotEmpty() &&
            preservedCurrentDisplay.isNotEmpty()
    val shouldPreserveBatchWindow =
        canKeepCurrentDisplay ||
            hasPartialWindowOverlap ||
            previousBatchConsumed > 0 || (
            previousDisplay.isNotEmpty() &&
                previousBatchTotal > 0 &&
                previousDisplay.size < previousBatchTotal
            )
    val preservedWindow = if (shouldPreserveBatchWindow && preservedCurrentDisplay.isNotEmpty()) {
        preservedCurrentDisplay
    } else {
        syncedDistinct
    }
    val resolvedDisplay = if (shouldPreserveBatchWindow && preservedWindow.isNotEmpty()) {
        preservedWindow
    } else {
        syncedDistinct
    }
    val keepsCurrentProgress = shouldPreserveBatchWindow || (
        syncedDistinct.size <= previousDisplay.size &&
            syncedDistinct.all { previousIds.contains(it.id) }
        )
    val resolvedBatchTotal = if (keepsCurrentProgress) {
        previousBatchTotal.coerceAtLeast(resolvedDisplay.size)
    } else {
        resolvedDisplay.size
    }
    return CatalogSyncResolution(
        displayMedia = resolvedDisplay,
        isFullAlbumDone = synced.isFullAlbumDone,
        currentBatchTotal = resolvedBatchTotal,
        shouldAlignCurrentDisplay = shouldPreserveBatchWindow,
        preservesBatchProgress = keepsCurrentProgress
    )
}
