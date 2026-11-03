package com.voicelike.app.keep

import android.content.Context
import android.util.LruCache
import com.voicelike.app.MediaItem
import com.voicelike.app.loadMediaByIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class KeepGalleryPage(
    val items: List<MediaItem>,
    val nextOffset: Int,
    val hasMore: Boolean,
    val staleIds: Set<Long>
)

internal fun keepGalleryConsumedBatchCount(
    resolvedMask: List<Boolean>,
    remainingSlots: Int
): Int {
    if (resolvedMask.isEmpty()) return 0
    var acceptedResolvedCount = 0
    var consumedCount = 0
    for (resolved in resolvedMask) {
        if (resolved) {
            if (acceptedResolvedCount >= remainingSlots) {
                break
            }
            acceptedResolvedCount += 1
        }
        consumedCount += 1
    }
    return consumedCount
}

class KeepGalleryDataSource(
    context: Context
) {
    companion object {
        private const val CACHE_SIZE = 2048
        private const val PAGE_SCAN_CHUNK_SIZE = 240
    }

    private val appContext = context.applicationContext
    private val itemCache = object : LruCache<Long, MediaItem>(CACHE_SIZE) {}

    suspend fun loadPage(
        snapshot: List<KeepIndexEntry>,
        offset: Int,
        limit: Int
    ): KeepGalleryPage = withContext(Dispatchers.IO) {
        if (snapshot.isEmpty() || offset >= snapshot.size || limit <= 0) {
            return@withContext KeepGalleryPage(
                items = emptyList(),
                nextOffset = offset,
                hasMore = false,
                staleIds = emptySet()
            )
        }

        var cursor = offset
        val resolvedItems = ArrayList<MediaItem>(limit)
        val staleIds = LinkedHashSet<Long>()

        while (cursor < snapshot.size && resolvedItems.size < limit) {
            val batchEnd = (cursor + PAGE_SCAN_CHUNK_SIZE).coerceAtMost(snapshot.size)
            val orderedIds = snapshot.subList(cursor, batchEnd).map { it.id }
            val missingIds = ArrayList<Long>(orderedIds.size)
            val resolvedById = LinkedHashMap<Long, MediaItem>(orderedIds.size)

            orderedIds.forEach { id ->
                val cached = itemCache.get(id)
                if (cached != null) {
                    resolvedById[id] = cached
                } else {
                    missingIds += id
                }
            }

            if (missingIds.isNotEmpty()) {
                loadMediaByIds(appContext, missingIds).forEach { item ->
                    itemCache.put(item.id, item)
                    resolvedById[item.id] = item
                }
            }

            val consumedCount = keepGalleryConsumedBatchCount(
                resolvedMask = orderedIds.map { resolvedById[it] != null },
                remainingSlots = (limit - resolvedItems.size).coerceAtLeast(0)
            )
            for (index in 0 until consumedCount) {
                val id = orderedIds[index]
                val resolved = resolvedById[id]
                if (resolved != null) {
                    resolvedItems += resolved
                } else {
                    staleIds += id
                }
            }
            cursor += consumedCount
        }

        KeepGalleryPage(
            items = resolvedItems,
            nextOffset = cursor,
            hasMore = cursor < snapshot.size,
            staleIds = staleIds
        )
    }
}
