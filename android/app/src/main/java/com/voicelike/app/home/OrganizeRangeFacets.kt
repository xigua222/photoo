package com.voicelike.app

// Counts behind the organize range panel, built in one pass over the library.
// "remaining" always means photos not yet organized and not pending delete.

internal const val ORGANIZE_RANGE_PREVIEW_COUNT = 6

internal data class OrganizeAlbumFacet(
    val name: String,
    val remaining: Int,
    val total: Int,
    val pinned: Boolean,
    val hidden: Boolean,
    val previews: List<MediaItem>
)

internal data class OrganizeMonthFacet(
    val monthKey: String,
    val year: Int,
    val month: Int,
    val remaining: Int,
    val total: Int,
    val previews: List<MediaItem>
)

internal data class OrganizeDayFacet(
    val dayKey: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val remaining: Int,
    // Newest remaining photo of that day.
    val cover: MediaItem
)

internal data class OrganizeRangeFacets(
    // Remaining photos for the whole pending range; drives the Organize N button.
    val total: Int,
    // Remaining under the pending album + kind, per time preset (ALL / TODAY / ON_THIS_DAY).
    val timeCounts: Map<String, Int>,
    // Days that still have remaining photos under the pending album + kind.
    val randomDayCandidates: List<String>,
    // Albums under the pending time + kind, in panel order; hidden ones are left out unless selected.
    val albums: List<OrganizeAlbumFacet>,
    val hiddenAlbums: List<OrganizeAlbumFacet>,
    // Remaining under the pending time + album, per kind in OrganizeKind.panelOrder.
    val kindCounts: Map<String, Int>,
    // Months that have photos under the pending album + kind, newest first.
    val months: List<OrganizeMonthFacet>
) {
    companion object {
        val Empty = OrganizeRangeFacets(
            total = 0,
            timeCounts = emptyMap(),
            randomDayCandidates = emptyList(),
            albums = emptyList(),
            hiddenAlbums = emptyList(),
            kindCounts = emptyMap(),
            months = emptyList()
        )
    }
}

private class AlbumAccumulator(val name: String) {
    var remaining = 0
    var total = 0
    val remainingPreviews = ArrayList<MediaItem>(ORGANIZE_RANGE_PREVIEW_COUNT)
    val totalPreviews = ArrayList<MediaItem>(ORGANIZE_RANGE_PREVIEW_COUNT)
}

private class MonthAccumulator(val monthValue: Int) {
    var remaining = 0
    var total = 0
    val previews = ArrayList<MediaItem>(ORGANIZE_RANGE_PREVIEW_COUNT)
}

/**
 * [source] is the library without pending-delete photos and without the hidden-album
 * filter: hidden albums are handled here so a selected hidden album still counts.
 */
internal fun buildOrganizeRangeFacets(
    source: List<MediaItem>,
    processedIds: Set<Long>,
    hiddenFolders: Set<String>,
    prioritizedFolders: Set<String>,
    criteria: MediaFilterCriteria
): OrganizeRangeFacets {
    if (source.isEmpty()) return OrganizeRangeFacets.Empty
    // An Insights collection narrows everything to its photos; the other conditions start from All.
    criteria.collectionIds?.let { ids ->
        return buildOrganizeRangeFacets(
            source = source.filter { it.id in ids },
            processedIds = processedIds,
            hiddenFolders = hiddenFolders,
            prioritizedFolders = prioritizedFolders,
            criteria = criteria.withoutCollection()
        )
    }
    val today = todayDayValue()
    val selectedFolder = criteria.folder
    var total = 0
    var allTime = 0
    var todayCount = 0
    var onThisDayCount = 0
    val randomDays = HashSet<Int>()
    val albums = HashMap<String, AlbumAccumulator>()
    // Hidden albums are listed whole, whatever the time and kind (like iOS).
    val hiddenAlbums = HashMap<String, AlbumAccumulator>()
    val kindCounts = HashMap<String, Int>()
    val months = HashMap<Int, MonthAccumulator>()
    val seen = HashSet<Long>(source.size * 2)
    val todayPreset = criteria.withTime(OrganizeTimePreset.TODAY)
    val onThisDayPreset = criteria.withTime(OrganizeTimePreset.ON_THIS_DAY)

    for (item in source) {
        if (!seen.add(item.id)) continue
        val remaining = item.id !in processedIds
        val bucket = item.bucketName
        val albumOk = if (selectedFolder == "All") bucket !in hiddenFolders else bucket == selectedFolder
        val kindOk = criteria.type == "All" || mediaMatchesKind(item, criteria.type)
        val timeOk = mediaMatchesTime(item, criteria, today)

        if (albumOk && kindOk) {
            val monthValue = mediaMonthValue(item)
            val month = months.getOrPut(monthValue) { MonthAccumulator(monthValue) }
            month.total += 1
            if (remaining) {
                month.remaining += 1
                if (month.previews.size < ORGANIZE_RANGE_PREVIEW_COUNT) month.previews.add(item)
                allTime += 1
                if (mediaMatchesTime(item, todayPreset, today)) todayCount += 1
                if (mediaMatchesTime(item, onThisDayPreset, today)) onThisDayCount += 1
                randomDays.add(mediaDayValue(item))
                if (timeOk) total += 1
            }
        }

        if (bucket in hiddenFolders && bucket != selectedFolder) {
            val hidden = hiddenAlbums.getOrPut(bucket) { AlbumAccumulator(bucket) }
            hidden.total += 1
            if (hidden.totalPreviews.size < ORGANIZE_RANGE_PREVIEW_COUNT) hidden.totalPreviews.add(item)
            if (remaining) {
                hidden.remaining += 1
                if (hidden.remainingPreviews.size < ORGANIZE_RANGE_PREVIEW_COUNT) hidden.remainingPreviews.add(item)
            }
        } else if (kindOk && timeOk) {
            val album = albums.getOrPut(bucket) { AlbumAccumulator(bucket) }
            album.total += 1
            if (album.totalPreviews.size < ORGANIZE_RANGE_PREVIEW_COUNT) album.totalPreviews.add(item)
            if (remaining) {
                album.remaining += 1
                if (album.remainingPreviews.size < ORGANIZE_RANGE_PREVIEW_COUNT) album.remainingPreviews.add(item)
            }
        }

        if (albumOk && timeOk && remaining) {
            for (kind in OrganizeKind.panelOrder) {
                if (kind == OrganizeKind.ALL || mediaMatchesKind(item, kind)) {
                    kindCounts[kind] = (kindCounts[kind] ?: 0) + 1
                }
            }
        }
    }

    fun AlbumAccumulator.toFacet() = OrganizeAlbumFacet(
        name = name,
        remaining = remaining,
        total = total,
        pinned = name in prioritizedFolders,
        hidden = name in hiddenFolders,
        previews = if (remainingPreviews.isNotEmpty()) remainingPreviews else totalPreviews
    )
    val albumFacets = albums.values
        .map { acc ->
            OrganizeAlbumFacet(
                name = acc.name,
                remaining = acc.remaining,
                total = acc.total,
                pinned = acc.name in prioritizedFolders,
                hidden = acc.name in hiddenFolders,
                previews = if (acc.remainingPreviews.isNotEmpty()) acc.remainingPreviews else acc.totalPreviews
            )
        }
        .sortedWith(
            compareByDescending<OrganizeAlbumFacet> { it.pinned }
                .thenByDescending { it.remaining }
                .thenByDescending { it.total }
                .thenBy { it.name }
        )

    return OrganizeRangeFacets(
        total = total,
        timeCounts = mapOf(
            OrganizeTimePreset.ALL to allTime,
            OrganizeTimePreset.TODAY to todayCount,
            OrganizeTimePreset.ON_THIS_DAY to onThisDayCount,
            OrganizeTimePreset.RANDOM_DAY to randomDays.size
        ),
        randomDayCandidates = randomDays.sortedDescending().map(::dayKeyOf),
        albums = albumFacets,
        hiddenAlbums = hiddenAlbums.values.map { it.toFacet() }.sortedBy { it.name },
        kindCounts = kindCounts,
        months = months.values
            .sortedByDescending { it.monthValue }
            .map { acc ->
                OrganizeMonthFacet(
                    monthKey = monthKeyOf(acc.monthValue),
                    year = acc.monthValue / 100,
                    month = acc.monthValue % 100,
                    remaining = acc.remaining,
                    total = acc.total,
                    previews = acc.previews.toList()
                )
            }
    )
}

/** Days of one month that still have remaining photos under the pending album + kind. */
internal fun buildOrganizeMonthDays(
    source: List<MediaItem>,
    processedIds: Set<Long>,
    hiddenFolders: Set<String>,
    criteria: MediaFilterCriteria,
    monthKey: String
): List<OrganizeDayFacet> {
    val monthCriteria = criteria.withTime(month = monthKey)
    val selectedFolder = criteria.folder
    val counts = HashMap<Int, Int>()
    val covers = HashMap<Int, MediaItem>()
    val today = todayDayValue()
    val seen = HashSet<Long>()
    for (item in source) {
        if (!seen.add(item.id) || item.id in processedIds) continue
        val albumOk = if (selectedFolder == "All") item.bucketName !in hiddenFolders else item.bucketName == selectedFolder
        if (!albumOk) continue
        if (criteria.type != "All" && !mediaMatchesKind(item, criteria.type)) continue
        if (!mediaMatchesTime(item, monthCriteria, today)) continue
        val dayValue = mediaDayValue(item)
        counts[dayValue] = (counts[dayValue] ?: 0) + 1
        val cover = covers[dayValue]
        if (cover == null || item.dateAdded > cover.dateAdded) covers[dayValue] = item
    }
    return counts.entries
        .sortedBy { it.key }
        .map { (dayValue, count) ->
            OrganizeDayFacet(
                dayKey = dayKeyOf(dayValue),
                year = dayValue / 10000,
                month = (dayValue / 100) % 100,
                day = dayValue % 100,
                remaining = count,
                cover = covers.getValue(dayValue)
            )
        }
}

/** Rolls a day that still has photos, avoiding the current pick when there is a choice. */
internal fun pickRandomOrganizeDay(candidates: List<String>, current: String?): String? {
    if (candidates.isEmpty()) return null
    val pool = candidates.filter { it != current }.ifEmpty { candidates }
    return pool.random()
}

/**
 * Photos "Organize Again" can bring back: organized before, but not liked, moved
 * or waiting for deletion. Hidden albums are left out.
 */
internal fun selectReorganizeCandidates(
    allMedia: List<MediaItem>,
    processedIds: Set<Long>,
    pendingTrashIds: Set<Long>,
    likedIds: Set<Long>,
    movedIds: Set<Long>,
    hiddenFolders: Set<String>
): List<MediaItem> {
    if (processedIds.isEmpty()) return emptyList()
    return allMedia
        .asSequence()
        .filter { it.id in processedIds }
        .filter { it.id !in pendingTrashIds && it.id !in likedIds && it.id !in movedIds }
        .filter { it.bucketName !in hiddenFolders }
        .distinctBy { it.id }
        .toList()
}
