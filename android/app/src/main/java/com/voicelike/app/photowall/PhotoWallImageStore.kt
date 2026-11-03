package com.voicelike.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.voicelike.app.keep.loadKeepGalleryThumbBitmap
import com.voicelike.app.keep.peekBestKeepGalleryThumbBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

// Thumbnails for the photo wall, on top of the keep gallery's thumbnail cache (platform thumbnails
// first, Coil as a fallback). Each visible pass says which photos it wants at which size; loads for
// photos that scrolled away before finishing are dropped. Main thread only.
internal class PhotoWallImageStore(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    class Entry(
        val bucket: Int,
        val bitmap: Bitmap,
    ) {
        // Reused for every draw of this bitmap (local matrix set per tile).
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    // Bumped when an image arrives, so the wall draws again.
    var version by mutableIntStateOf(0)
        private set

    private val entries =
        object : LinkedHashMap<Long, Entry>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Entry>?): Boolean = size > MAX_ENTRIES
        }
    private val inFlight = HashMap<Long, Pair<Int, Job>>()
    private val wanted = HashSet<Long>()
    private val permits = Semaphore(MAX_PARALLEL_LOADS)

    fun peek(id: Long): Entry? = entries[id]

    fun beginPass() {
        wanted.clear()
    }

    // [allowUpgrade] is false while the wall moves: a photo that already has an image keeps it until
    // the wall stops, then gets the sharper one.
    fun want(
        item: MediaItem,
        bucket: Int,
        allowUpgrade: Boolean,
    ) {
        val id = item.id
        wanted.add(id)
        val existing = entries[id]
        if (existing != null && (existing.bucket >= bucket || !allowUpgrade)) return
        val loading = inFlight[id]
        if (loading != null && loading.first >= bucket) return
        if (existing == null) {
            peekBestKeepGalleryThumbBitmap(item, bucket)?.let { cached ->
                entries[id] = Entry(bucket, cached)
                version += 1
                return
            }
        }
        loading?.second?.cancel()
        val job =
            scope.launch {
                val bitmap = permits.withPermit { loadKeepGalleryThumbBitmap(context, item, bucket) }
                if (bitmap != null) {
                    val current = entries[id]
                    if (current == null || current.bucket < bucket || current.bitmap !== bitmap) {
                        entries[id] = Entry(bucket, bitmap)
                        version += 1
                    }
                }
            }
        inFlight[id] = bucket to job
        job.invokeOnCompletion {
            if (inFlight[id]?.second === job) inFlight.remove(id)
        }
    }

    // Drops loads for photos no longer anywhere near the screen.
    fun endPass() {
        if (inFlight.isEmpty()) return
        val iterator = inFlight.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in wanted) {
                entry.value.second.cancel()
                iterator.remove()
            }
        }
    }

    fun clear() {
        inFlight.values.forEach { it.second.cancel() }
        inFlight.clear()
        entries.clear()
    }

    private companion object {
        const val MAX_ENTRIES = 600
        const val MAX_PARALLEL_LOADS = 6
    }
}
