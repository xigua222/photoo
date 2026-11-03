package com.voicelike.app.keep

import com.voicelike.app.MediaItem
import java.util.LinkedHashSet

fun buildKeepIdSnapshot(
    processedIds: Set<Long>,
    likedIds: Set<Long>,
    pendingTrashIds: Set<Long>,
    movedIds: Set<Long>
): Set<Long> {
    if (processedIds.isEmpty()) return emptySet()
    val keepIds = LinkedHashSet<Long>(processedIds.size)
    processedIds.forEach { id ->
        if (!likedIds.contains(id) && !pendingTrashIds.contains(id) && !movedIds.contains(id)) {
            keepIds.add(id)
        }
    }
    return keepIds
}

fun resolveMediaItemsByIds(
    media: List<MediaItem>,
    ids: Set<Long>
): List<MediaItem> {
    if (media.isEmpty() || ids.isEmpty()) return emptyList()
    return media.asSequence()
        .filter { ids.contains(it.id) }
        .toList()
}
