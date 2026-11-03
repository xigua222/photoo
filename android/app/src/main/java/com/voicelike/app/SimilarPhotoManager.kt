package com.voicelike.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

internal fun normalizeSimilarGroupsForDisplay(groups: List<List<MediaItem>>): List<List<MediaItem>> {
    if (groups.isEmpty()) return emptyList()

    val normalized = ArrayList<List<MediaItem>>(groups.size)
    val seenLeadIds = HashSet<Long>(groups.size)
    val globallyAssignedItemIds = HashSet<Long>(groups.sumOf { it.size })

    groups.forEach { group ->
        val uniqueItems = ArrayList<MediaItem>(group.size)
        val seenInGroup = HashSet<Long>(group.size)
        group.forEach { item ->
            if (seenInGroup.add(item.id) && !globallyAssignedItemIds.contains(item.id)) {
                uniqueItems.add(item)
            }
        }

        val leadId = uniqueItems.firstOrNull()?.id ?: return@forEach
        if (uniqueItems.size < 2 || !seenLeadIds.add(leadId)) {
            return@forEach
        }

        normalized.add(uniqueItems)
        uniqueItems.forEach { item -> globallyAssignedItemIds.add(item.id) }
    }

    return normalized
}

/**
 * Similar-photo stage with stable snapshot persistence.
 * Raw groups are persisted for the whole scan domain; ignored ids only affect displayed groups.
 */
object SimilarPhotoManager {
    private const val PHASH_WEIGHT = 0.7
    private const val DHASH_WEIGHT = 0.3
    private const val PHASH_THRESHOLD = 14
    private const val DHASH_THRESHOLD = 10
    private const val COMBINED_SCORE_THRESHOLD = 11.5
    private const val LSH_BANDS = 4
    private const val BAND_BITS = 16
    private const val MAX_MATCHES_PER_SEED = 8
    private const val UPDATE_INTERVAL_MS = 2000L

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress = _scanProgress.asStateFlow()

    private val _foundGroups = MutableStateFlow<List<List<MediaItem>>>(emptyList())
    val foundGroups = _foundGroups.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var statusObserverJob: Job? = null
    private var progressObserverJob: Job? = null
    private var groupUpdateJob: Job? = null
    private var snapshotPersistJob: Job? = null

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var currentMediaSnapshot: List<MediaItem> = emptyList()
    @Volatile
    private var currentCatalogSignature: Long = 0L
    @Volatile
    private var lastObservedMediaScanState: Boolean? = null
    @Volatile
    private var rawFoundGroups: List<List<MediaItem>> = emptyList()

    private var lastUpdateTimestamp = 0L
    private var isComputingGroups = false
    private var lastRefreshProgress = 0f
    private val similarCacheLock = Any()
    @Volatile
    private var cachedSourceSignature: String = ""
    @Volatile
    private var cachedComparableEntries: List<ComparableEntry> = emptyList()
    @Volatile
    private var cachedLshIndex: Array<MutableMap<Int, MutableList<Int>>>? = null
    @Volatile
    private var lastStartSourceSignature: String = ""
    @Volatile
    private var lastStartIgnoredSignature: Long = 0L
    private val pauseRequests = AtomicInteger(0)

    private val currentIgnoredIds = java.util.Collections.synchronizedSet(HashSet<Long>())

    fun requestPause() {
        pauseRequests.incrementAndGet()
        _isPaused.value = pauseRequests.get() > 0
        groupUpdateJob?.cancel()
    }

    fun releasePause() {
        if (pauseRequests.get() > 0) {
            pauseRequests.decrementAndGet()
        }
        _isPaused.value = pauseRequests.get() > 0
        if (_isPaused.value) return
        if (rawFoundGroups.isNotEmpty()) {
            publishDisplayGroups()
        } else if (currentMediaSnapshot.isNotEmpty()) {
            updateGroups(currentMediaSnapshot, force = true)
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        MediaAnalysisManager.init(context)
        ensureObservers()
    }

    fun setDisplayIgnoredIds(ids: Set<Long>) {
        synchronized(currentIgnoredIds) {
            currentIgnoredIds.clear()
            currentIgnoredIds.addAll(ids)
        }
        lastStartIgnoredSignature = buildIgnoredIdsSignature(ids)
        publishDisplayGroups()
    }

    fun addIgnoredIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        synchronized(currentIgnoredIds) {
            currentIgnoredIds.addAll(ids)
            lastStartIgnoredSignature = buildIgnoredIdsSignature(currentIgnoredIds)
        }
        publishDisplayGroups()
    }

    fun startScan(
        context: Context,
        allMedia: List<MediaItem>,
        ignoredIds: Set<Long> = emptySet(),
        catalogSignature: Long = 0L
    ) {
        init(context)
        currentMediaSnapshot = allMedia
        currentCatalogSignature = catalogSignature.takeIf { it != 0L } ?: buildMediaScanSignature(allMedia, 0)
        setDisplayIgnoredIds(ignoredIds)
        ensureObservers()

        val sourceSignature = buildSourceSignature(allMedia)
        val inputChanged = sourceSignature != lastStartSourceSignature
        lastStartSourceSignature = sourceSignature

        scope.launch {
            val hydratedProgress = MediaAnalysisManager.hydrateProgressFromCache(context, allMedia)
            _scanProgress.value = hydratedProgress

            val (restoredGroups, restoredFromSnapshot) = restorePersistedGroups(context, allMedia, currentCatalogSignature)
            if (restoredFromSnapshot || restoredGroups.isNotEmpty()) {
                rawFoundGroups = restoredGroups
                publishDisplayGroups()
            }

            val shouldForceRefresh =
                inputChanged ||
                    (!MediaAnalysisManager.isScanning.value && hydratedProgress < 0.999f) ||
                    (!restoredFromSnapshot && restoredGroups.isEmpty())
            if (shouldForceRefresh) {
                updateGroups(currentMediaSnapshot, force = true)
            }
        }
    }

    suspend fun hydrateFromCache(
        context: Context,
        allMedia: List<MediaItem>,
        ignoredIds: Set<Long> = emptySet(),
        catalogSignature: Long = 0L
    ) {
        init(context)
        currentMediaSnapshot = allMedia
        currentCatalogSignature = catalogSignature.takeIf { it != 0L } ?: buildMediaScanSignature(allMedia, 0)
        setDisplayIgnoredIds(ignoredIds)
        lastStartSourceSignature = buildSourceSignature(allMedia)

        val hydratedProgress = MediaAnalysisManager.hydrateProgressFromCache(context, allMedia)
        _scanProgress.value = hydratedProgress

        if (allMedia.isEmpty()) {
            rawFoundGroups = emptyList()
            _foundGroups.value = emptyList()
            _isScanning.value = false
            return
        }

        val (restoredGroups, restoredFromSnapshot) = restorePersistedGroups(context, allMedia, currentCatalogSignature)
        val effectiveGroups = when {
            restoredFromSnapshot -> restoredGroups
            else -> emptyList()
        }
        rawFoundGroups = effectiveGroups
        publishDisplayGroups()
        if (restoredFromSnapshot) {
            _scanProgress.value = 1f
        }
        _isScanning.value = false
    }

    fun flushSnapshot() {
        val context = appContext ?: return
        val snapshot = snapshotForPersist(isFinal = true) ?: return
        SimilarScanSnapshotStore.save(context, snapshot)
    }

    fun trimTransientCaches() {
        synchronized(similarCacheLock) {
            cachedSourceSignature = ""
            cachedComparableEntries = emptyList()
            cachedLshIndex = null
        }
    }

    fun resetSession(clearPersisted: Boolean = false) {
        pauseRequests.set(0)
        _isPaused.value = false
        groupUpdateJob?.cancel()
        groupUpdateJob = null
        snapshotPersistJob?.cancel()
        snapshotPersistJob = null
        _scanProgress.value = 0f
        _foundGroups.value = emptyList()
        rawFoundGroups = emptyList()
        currentMediaSnapshot = emptyList()
        currentCatalogSignature = 0L
        lastUpdateTimestamp = 0L
        lastRefreshProgress = 0f
        isComputingGroups = false
        lastStartSourceSignature = ""
        lastStartIgnoredSignature = 0L
        lastObservedMediaScanState = null
        synchronized(currentIgnoredIds) {
            currentIgnoredIds.clear()
        }
        synchronized(similarCacheLock) {
            cachedSourceSignature = ""
            cachedComparableEntries = emptyList()
            cachedLshIndex = null
        }
        if (clearPersisted) {
            appContext?.let { SimilarScanSnapshotStore.clear(it) }
        }
    }

    private fun ensureObservers() {
        if (statusObserverJob == null) {
            statusObserverJob = scope.launch {
                MediaAnalysisManager.isScanning.collect { scanning ->
                    val previous = lastObservedMediaScanState
                    lastObservedMediaScanState = scanning
                    _isScanning.value = scanning || isComputingGroups
                    if (previous == true && !scanning) {
                        _scanProgress.value = MediaAnalysisManager.progress.value.coerceIn(0f, 1f)
                        updateGroups(currentMediaSnapshot, force = true)
                    }
                }
            }
        }

        if (progressObserverJob == null) {
            progressObserverJob = scope.launch {
                MediaAnalysisManager.progress.collect { progress ->
                    _scanProgress.value = progress.coerceIn(0f, 1f)
                    if (progress > 0f && progress < 1f) {
                        val shouldRefresh = if (shouldThrottleGroupRefresh()) {
                            (progress - lastRefreshProgress) >= 0.12f || progress >= 0.995f
                        } else {
                            (progress - lastRefreshProgress) >= 0.03f || progress >= 0.98f
                        }
                        if (shouldRefresh) {
                            lastRefreshProgress = progress
                            updateGroups(currentMediaSnapshot)
                        }
                    }
                }
            }
        }
    }

    private fun updateGroups(
        allMedia: List<MediaItem>,
        force: Boolean = false
    ) {
        if (pauseRequests.get() > 0) return
        if (allMedia.isEmpty()) {
            rawFoundGroups = emptyList()
            publishDisplayGroups()
            _scanProgress.value = 1f
            _isScanning.value = false
            return
        }

        val now = System.currentTimeMillis()
        if (!force && AppPerformanceGovernor.shouldPauseNonCriticalWork()) {
            return
        }
        val minInterval = AppPerformanceGovernor.scaledDelay(UPDATE_INTERVAL_MS)
        if (!force && (now - lastUpdateTimestamp < minInterval || isComputingGroups)) {
            return
        }

        isComputingGroups = true
        _isScanning.value = true
        groupUpdateJob = scope.launch(Dispatchers.Default) {
            try {
                if (shouldThrottleGroupRefresh() && !force && MediaAnalysisManager.isScanning.value) {
                    return@launch
                }
                val groups = findSimilarGroupsGlobal(allMedia, cachedOnly = true)
                rawFoundGroups = groups
                publishDisplayGroups()
                persistStableSnapshotIfReady()
                lastUpdateTimestamp = System.currentTimeMillis()
            } catch (_: OutOfMemoryError) {
                appContext?.let { context ->
                    MemorySafetyMode.activateOomFallback(context)
                    AppPerformanceGovernor.onCaughtOutOfMemory()
                }
                trimTransientCaches()
            } finally {
                isComputingGroups = false
                if (groupUpdateJob === this) {
                    groupUpdateJob = null
                }
                _scanProgress.value = MediaAnalysisManager.progress.value.coerceIn(0f, 1f)
                _isScanning.value = MediaAnalysisManager.isScanning.value
            }
        }
    }

    private suspend fun restorePersistedGroups(
        context: Context,
        allMedia: List<MediaItem>,
        signature: Long
    ): Pair<List<List<MediaItem>>, Boolean> {
        val snapshot = SimilarScanSnapshotStore.load(context)
        val mediaById = allMedia.associateBy { it.id }
        if (snapshot != null && snapshot.signature == signature && snapshot.isFinal) {
            val restoredGroups = snapshot.groupsByIds.mapNotNull { groupIds ->
                val items = groupIds.mapNotNull { mediaById[it] }
                items.takeIf { it.size >= 2 }
            }
            return restoredGroups to true
        }
        return emptyList<List<MediaItem>>() to false
    }

    private fun publishDisplayGroups() {
        val ignoredIds = ignoredIdsSnapshot()
        _foundGroups.value = normalizeSimilarGroupsForDisplay(
            rawFoundGroups.mapNotNull { group ->
                val visible = group.filterNot { ignoredIds.contains(it.id) }
                visible.takeIf { it.size >= 2 }
            }
        )
    }

    private fun persistStableSnapshotIfReady() {
        val context = appContext ?: return
        val snapshot = snapshotForPersist(isFinal = true) ?: return
        snapshotPersistJob?.cancel()
        snapshotPersistJob = scope.launch(Dispatchers.IO) {
            SimilarScanSnapshotStore.save(context, snapshot)
        }
    }

    private fun snapshotForPersist(isFinal: Boolean): SimilarScanSnapshot? {
        if (currentCatalogSignature == 0L) return null
        if (isFinal) {
            val scanSettled =
                !MediaAnalysisManager.isScanning.value &&
                    MediaAnalysisManager.progress.value >= 0.999f
            if (!scanSettled) return null
        }
        val groupsByIds = rawFoundGroups.map { group -> group.map { it.id } }
        return SimilarScanSnapshot(
            signature = currentCatalogSignature,
            groupsByIds = groupsByIds,
            updatedAtMs = System.currentTimeMillis(),
            isFinal = isFinal
        )
    }

    private fun ignoredIdsSnapshot(): Set<Long> {
        return synchronized(currentIgnoredIds) { HashSet(currentIgnoredIds) }
    }

    private suspend fun findSimilarGroupsGlobal(
        sortedPhotos: List<MediaItem>,
        cachedOnly: Boolean = false
    ): List<List<MediaItem>> {
        val groups = mutableListOf<List<MediaItem>>()
        val visited = mutableSetOf<Long>()
        val sourceSignature = buildSourceSignature(sortedPhotos)
        val (entries, index) = getOrBuildSourceCache(sourceSignature, sortedPhotos, cachedOnly = cachedOnly)
        if (entries.size < 2) return emptyList()

        for (entry in entries) {
            if (visited.contains(entry.id)) continue

            val ordered = rankCandidates(
                target = entry,
                entries = entries,
                index = index,
                visited = visited,
                requireHigherIndex = true
            )
            if (ordered.isNotEmpty()) {
                val group = listOf(entry.item) + ordered.take(MAX_MATCHES_PER_SEED).map { it.entry.item }
                groups.add(group)
                group.forEach { visited.add(it.id) }
            }
        }
        return groups
    }

    private fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    suspend fun findSimilarForItems(
        context: Context,
        targets: List<MediaItem>,
        allSorted: List<MediaItem>
    ): Map<Long, List<MediaItem>> = withContext(Dispatchers.Default) {
        init(context)
        val results = mutableMapOf<Long, List<MediaItem>>()
        val sourceSignature = buildSourceSignature(allSorted)
        val (entries, index) = getOrBuildSourceCache(sourceSignature, allSorted)
        if (entries.size < 2) return@withContext results
        val entryById = entries.associateBy { it.id }

        for (target in targets) {
            val targetEntry = entryById[target.id] ?: continue
            val ordered = rankCandidates(
                target = targetEntry,
                entries = entries,
                index = index,
                visited = emptySet(),
                requireHigherIndex = false
            )
            if (ordered.isNotEmpty()) {
                results[target.id] = listOf(target) + ordered.take(MAX_MATCHES_PER_SEED).map { it.entry.item }
            }
        }
        results
    }

    private suspend fun getOrBuildSourceCache(
        sourceSignature: String,
        allSorted: List<MediaItem>,
        cachedOnly: Boolean = false
    ): Pair<List<ComparableEntry>, Array<MutableMap<Int, MutableList<Int>>>> {
        val cacheSignature = if (cachedOnly) "cached:$sourceSignature" else "full:$sourceSignature"
        synchronized(similarCacheLock) {
            val cachedIndexSnapshot = cachedLshIndex
            if (cachedSourceSignature == cacheSignature && cachedIndexSnapshot != null) {
                return cachedComparableEntries to cachedIndexSnapshot
            }
        }

        val rebuiltEntries = buildComparableEntries(allSorted, cachedOnly = cachedOnly)
        val rebuiltIndex = buildLshIndex(rebuiltEntries)
        val shouldRetainCache = shouldRetainComparableCache()

        if (shouldRetainCache) {
            synchronized(similarCacheLock) {
                cachedSourceSignature = cacheSignature
                cachedComparableEntries = rebuiltEntries
                cachedLshIndex = rebuiltIndex
            }
        } else {
            trimTransientCaches()
        }
        return rebuiltEntries to rebuiltIndex
    }

    private fun buildSourceSignature(allSorted: List<MediaItem>): String {
        if (allSorted.isEmpty()) return "empty"
        val size = allSorted.size
        val latestDate = allSorted.firstOrNull()?.dateAdded ?: 0L
        val leadingIds = allSorted.asSequence().take(64).joinToString(",") { it.id.toString() }
        return "$size|$latestDate|$leadingIds"
    }

    private fun buildIgnoredIdsSignature(ignoredIds: Iterable<Long>): Long {
        var count = 0
        var sum = 0L
        var xor = 0L
        ignoredIds.forEach { id ->
            count += 1
            val mixed = (id * 1103515245L) xor (id ushr 11)
            sum += mixed
            xor = xor xor (mixed * -7046029254386353131L)
        }
        return (count.toLong() * 31L + sum) xor xor
    }

    private data class ComparableEntry(
        val index: Int,
        val item: MediaItem,
        val id: Long,
        val pHash: Long,
        val dHash: Long,
        val aspectRatio: Float?
    )

    private data class RankedMatch(
        val entry: ComparableEntry,
        val combinedScore: Double,
        val pHashDistance: Int,
        val dHashDistance: Int
    )

    private suspend fun buildComparableEntries(
        allPhotos: List<MediaItem>,
        cachedOnly: Boolean = false
    ): List<ComparableEntry> {
        val sorted = allPhotos
            .asSequence()
            .filter { shouldAnalyzeAsStandardPhoto(it) }
            .sortedByDescending { it.dateAdded }
            .toList()

        val entries = ArrayList<ComparableEntry>(sorted.size)
        sorted.forEachIndexed { index, item ->
            val analysis = if (cachedOnly) {
                MediaAnalysisManager.getAnalysis(item.id)
            } else {
                MediaAnalysisManager.ensureSimilarityHashes(item)
            } ?: return@forEachIndexed
            if (analysis.pHash == 0L || analysis.dHash == 0L) return@forEachIndexed
            entries += ComparableEntry(
                index = index,
                item = item,
                id = item.id,
                pHash = analysis.pHash,
                dHash = analysis.dHash,
                aspectRatio = item.aspectRatioOrNull()
            )
        }
        return entries
    }

    private fun buildLshIndex(entries: List<ComparableEntry>): Array<MutableMap<Int, MutableList<Int>>> {
        if (entries.size < 2) {
            return Array(LSH_BANDS) { mutableMapOf<Int, MutableList<Int>>() }
        }
        val bands = Array(LSH_BANDS) { mutableMapOf<Int, MutableList<Int>>() }
        entries.forEachIndexed { entryIndex, entry ->
            repeat(LSH_BANDS) { band ->
                val key = segmentKey(entry.pHash, band)
                bands[band].getOrPut(key) { mutableListOf() }.add(entryIndex)
            }
        }
        return bands
    }

    private fun rankCandidates(
        target: ComparableEntry,
        entries: List<ComparableEntry>,
        index: Array<MutableMap<Int, MutableList<Int>>>,
        visited: Set<Long>,
        requireHigherIndex: Boolean
    ): List<RankedMatch> {
        val candidateIndexes = LinkedHashSet<Int>()
        repeat(LSH_BANDS) { band ->
            val key = segmentKey(target.pHash, band)
            index[band][key].orEmpty().forEach { candidateIndexes.add(it) }
        }

        return candidateIndexes.asSequence()
            .map { entries[it] }
            .filter { it.id != target.id }
            .filter { !visited.contains(it.id) }
            .filter { !requireHigherIndex || it.index > target.index }
            .filter { isComparableMatch(target, it) }
            .mapNotNull { candidate ->
                val pDistance = hammingDistance(target.pHash, candidate.pHash)
                if (pDistance > PHASH_THRESHOLD) return@mapNotNull null

                val dDistance = hammingDistance(target.dHash, candidate.dHash)
                if (dDistance > DHASH_THRESHOLD) return@mapNotNull null

                val score = (pDistance * PHASH_WEIGHT) + (dDistance * DHASH_WEIGHT)
                if (score > COMBINED_SCORE_THRESHOLD) return@mapNotNull null

                RankedMatch(
                    entry = candidate,
                    combinedScore = score,
                    pHashDistance = pDistance,
                    dHashDistance = dDistance
                )
            }
            .sortedWith(
                compareBy<RankedMatch>(
                    { it.combinedScore },
                    { it.pHashDistance },
                    { it.dHashDistance },
                    { abs(target.item.dateAdded - it.entry.item.dateAdded) },
                    { it.entry.id }
                )
            )
            .toList()
    }

    private fun segmentKey(hash: Long, band: Int): Int {
        val shift = band * BAND_BITS
        return ((hash ushr shift) and 0xFFFFL).toInt()
    }

    private fun isComparableMatch(current: ComparableEntry, candidate: ComparableEntry): Boolean {
        val isLiveVsStatic = current.item.isLivePhoto != candidate.item.isLivePhoto
        val isSameMoment = abs(current.item.dateAdded - candidate.item.dateAdded) <= 2
        if (isLiveVsStatic && isSameMoment) {
            return false
        }

        val currentRatio = current.aspectRatio
        val candidateRatio = candidate.aspectRatio
        if (currentRatio != null && candidateRatio != null) {
            if (abs(currentRatio - candidateRatio) > 0.12f) {
                return false
            }
        }

        return true
    }

    private fun MediaItem.aspectRatioOrNull(): Float? {
        if (width <= 0 || height <= 0) return null
        return width.toFloat() / height.toFloat()
    }

    private fun shouldRetainComparableCache(): Boolean {
        val context = appContext ?: return true
        return !MemorySafetyMode.shouldReduceBackgroundWork(context)
    }

    private fun shouldThrottleGroupRefresh(): Boolean {
        val context = appContext ?: return false
        return MemorySafetyMode.shouldReduceBackgroundWork(context)
    }

    fun removeProcessedItems(processedIds: Set<Long>) {
        if (processedIds.isEmpty()) return
        publishDisplayGroups()
    }

    fun restoreIgnoredIds(ids: Set<Long>, allMedia: List<MediaItem>) {
        currentMediaSnapshot = allMedia
        synchronized(currentIgnoredIds) {
            currentIgnoredIds.removeAll(ids)
            lastStartIgnoredSignature = buildIgnoredIdsSignature(currentIgnoredIds)
        }
        publishDisplayGroups()
    }

    suspend fun findSimilarPhotos(context: Context, items: List<MediaItem>): List<List<MediaItem>> {
        init(context)
        return withContext(Dispatchers.Default) {
            findSimilarGroupsGlobal(items)
        }
    }
}
