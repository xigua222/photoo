package com.voicelike.app

internal fun mergeMediaIntoQueue(
    currentMedia: List<MediaItem>,
    additions: List<MediaItem>,
    queueOrder: String,
    randomOrderSeed: Long? = null
): List<MediaItem> {
    if (additions.isEmpty()) return currentMedia
    return orderMediaForQueue(
        media = (currentMedia + additions).distinctBy { it.id },
        queueOrder = queueOrder,
        randomOrderSeed = randomOrderSeed
    )
}

internal fun removeMediaByIds(
    currentMedia: List<MediaItem>,
    removedIds: Set<Long>
): List<MediaItem> {
    if (removedIds.isEmpty()) return currentMedia
    return currentMedia.filter { !removedIds.contains(it.id) }
}

internal fun sumMediaSize(items: List<MediaItem>): Long {
    if (items.isEmpty()) return 0L
    return items.sumOf { it.size }
}

internal fun sumMediaSizeByIds(
    currentMedia: List<MediaItem>,
    targetIds: Set<Long>
): Long {
    if (currentMedia.isEmpty() || targetIds.isEmpty()) return 0L
    return currentMedia.asSequence()
        .filter { targetIds.contains(it.id) }
        .sumOf { it.size }
}
