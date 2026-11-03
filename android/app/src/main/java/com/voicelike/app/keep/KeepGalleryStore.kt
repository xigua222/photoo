package com.voicelike.app.keep

import android.content.Context
import android.util.Log
import com.voicelike.app.MediaItem
import com.voicelike.app.loadMediaDateAddedByIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

data class KeepGalleryMetaState(
    val count: Int = 0,
    val expectedCount: Int = 0,
    val expectedFingerprint: Long = 0L,
    val version: Long = 0L,
    val isLoaded: Boolean = false,
    val hasPersistedIndex: Boolean = false
)

data class KeepIndexEntry(
    val id: Long,
    val dateAdded: Long
)

class KeepGalleryStore private constructor(
    private val appContext: Context
) {
    companion object {
        private const val TAG = "KeepGalleryStore"
        private const val FILE_MAGIC = 0x4B455031
        private const val FILE_VERSION = 1
        private const val PERSIST_DEBOUNCE_MS = 900L

        @Volatile
        private var instance: KeepGalleryStore? = null

        fun get(context: Context): KeepGalleryStore {
            return instance ?: synchronized(this) {
                instance ?: KeepGalleryStore(context.applicationContext).also { instance = it }
            }
        }

        private fun mixKeepId(id: Long): Long {
            var value = id
            value = (value xor (value ushr 33)) * -0xae502812aa7333L
            value = (value xor (value ushr 33)) * -0x3b314601e57a13adL
            return value xor (value ushr 33)
        }

        private fun fingerprintOf(ids: Collection<Long>): Long {
            var xorAcc = 0L
            var sumAcc = 0L
            var count = 0L
            ids.forEach { id ->
                val mixed = mixKeepId(id)
                xorAcc = xorAcc xor mixed
                sumAcc += mixed
                count += 1L
            }
            return combineFingerprint(xorAcc, sumAcc, count)
        }

        private fun combineFingerprint(
            xorAcc: Long,
            sumAcc: Long,
            count: Long
        ): Long {
            return xorAcc xor java.lang.Long.rotateLeft(sumAcc, 17) xor (count * -7046029254386353131L)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val entries = ArrayList<KeepIndexEntry>(512)
    private val indexById = HashMap<Long, Int>(512)
    private val indexFile = File(appContext.filesDir, "keep_gallery_index.bin")
    private val tempFile = File(appContext.filesDir, "keep_gallery_index.tmp")
    private val _metaState = MutableStateFlow(KeepGalleryMetaState())

    private var versionCounter = 0L
    private var loaded = false
    private var persistJob: Job? = null
    private var actualXorAcc = 0L
    private var actualSumAcc = 0L
    private var expectedXorAcc = 0L
    private var expectedSumAcc = 0L
    private var expectedCount = 0

    val metaState: StateFlow<KeepGalleryMetaState> = _metaState.asStateFlow()

    init {
        scope.launch {
            ensureLoaded()
        }
    }

    suspend fun ensureLoaded() {
        mutex.withLock {
            ensureLoadedLocked()
        }
    }

    suspend fun snapshotEntries(): List<KeepIndexEntry> {
        return mutex.withLock {
            ensureLoadedLocked()
            ArrayList(entries)
        }
    }

    suspend fun ensureSynced(keepIds: Set<Long>) {
        ensureLoaded()
        val targetFingerprint = fingerprintOf(keepIds)
        val current = metaState.value
        if (
            current.hasPersistedIndex &&
            current.expectedCount == keepIds.size &&
            current.expectedFingerprint == targetFingerprint
        ) {
            return
        }

        val dateMap = withContext(Dispatchers.IO) {
            loadMediaDateAddedByIds(appContext, keepIds)
        }
        val rebuiltEntries = ArrayList<KeepIndexEntry>(dateMap.size)
        dateMap.forEach { (id, dateAdded) ->
            rebuiltEntries += KeepIndexEntry(id = id, dateAdded = dateAdded)
        }
        rebuiltEntries.sortWith(compareByDescending<KeepIndexEntry> { it.dateAdded }.thenByDescending { it.id })

        mutex.withLock {
            ensureLoadedLocked()
            replaceEntriesLocked(
                newEntries = rebuiltEntries,
                expectedIds = keepIds,
                persistImmediately = true
            )
        }
    }

    fun upsert(item: MediaItem) {
        upsertAll(listOf(item))
    }

    fun upsertAll(items: Collection<MediaItem>) {
        if (items.isEmpty()) return
        scope.launch {
            mutex.withLock {
                ensureLoadedLocked()
                var changed = false
                items.forEach { item ->
                    if (applyUpsertLocked(KeepIndexEntry(id = item.id, dateAdded = item.dateAdded))) {
                        changed = true
                    }
                }
                if (changed) {
                    publishMetaLocked()
                    schedulePersistLocked()
                }
            }
        }
    }

    fun removeIds(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        scope.launch {
            mutex.withLock {
                ensureLoadedLocked()
                if (!applyRemoveIdsLocked(ids)) return@withLock
                publishMetaLocked()
                schedulePersistLocked()
            }
        }
    }

    private suspend fun ensureLoadedLocked() {
        if (loaded) return

        if (!indexFile.exists()) {
            loaded = true
            publishMetaLocked()
            return
        }

        try {
            DataInputStream(BufferedInputStream(indexFile.inputStream())).use { input ->
                val magic = input.readInt()
                val version = input.readInt()
                val storedExpectedCount = input.readInt()
                val entryCount = input.readInt()
                val storedExpectedXor = input.readLong()
                val storedExpectedSum = input.readLong()
                if (magic != FILE_MAGIC || version != FILE_VERSION || storedExpectedCount < 0 || entryCount < 0) {
                    throw IllegalStateException("Invalid keep index header")
                }

                val loadedEntries = ArrayList<KeepIndexEntry>(entryCount)
                repeat(entryCount) {
                    loadedEntries += KeepIndexEntry(
                        id = input.readLong(),
                        dateAdded = input.readLong()
                    )
                }

                rebuildActualIndexLocked(loadedEntries)
                expectedCount = storedExpectedCount
                expectedXorAcc = storedExpectedXor
                expectedSumAcc = storedExpectedSum
                loaded = true
                publishMetaLocked()
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to load keep index", error)
            runCatching { indexFile.delete() }
            runCatching { tempFile.delete() }
            clearLocked()
            loaded = true
            publishMetaLocked()
        }
    }

    private fun replaceEntriesLocked(
        newEntries: List<KeepIndexEntry>,
        expectedIds: Set<Long>,
        persistImmediately: Boolean
    ) {
        rebuildActualIndexLocked(newEntries)
        expectedCount = expectedIds.size
        expectedXorAcc = 0L
        expectedSumAcc = 0L
        expectedIds.forEach { id ->
            val mixed = mixKeepId(id)
            expectedXorAcc = expectedXorAcc xor mixed
            expectedSumAcc += mixed
        }
        loaded = true
        publishMetaLocked()
        schedulePersistLocked(immediate = persistImmediately)
    }

    private fun rebuildActualIndexLocked(newEntries: List<KeepIndexEntry>) {
        entries.clear()
        indexById.clear()
        actualXorAcc = 0L
        actualSumAcc = 0L

        newEntries.forEachIndexed { index, entry ->
            entries += entry
            indexById[entry.id] = index
            val mixed = mixKeepId(entry.id)
            actualXorAcc = actualXorAcc xor mixed
            actualSumAcc += mixed
        }
    }

    private fun clearLocked() {
        entries.clear()
        indexById.clear()
        actualXorAcc = 0L
        actualSumAcc = 0L
        expectedXorAcc = 0L
        expectedSumAcc = 0L
        expectedCount = 0
    }

    private fun applyUpsertLocked(entry: KeepIndexEntry): Boolean {
        val existingIndex = indexById[entry.id]
        if (existingIndex != null) {
            val existingEntry = entries[existingIndex]
            if (existingEntry.dateAdded == entry.dateAdded) {
                return false
            }
            entries.removeAt(existingIndex)
            indexById.remove(entry.id)
            reindexFromLocked(existingIndex)
            onActualRemovedLocked(entry.id)
        }

        val insertIndex = findInsertIndexLocked(entry)
        entries.add(insertIndex, entry)
        reindexFromLocked(insertIndex)
        onActualAddedLocked(entry.id)

        if (existingIndex == null) {
            onExpectedAddedLocked(entry.id)
        }

        return true
    }

    private fun applyRemoveIdsLocked(ids: Collection<Long>): Boolean {
        val removalIndices = ids.asSequence()
            .mapNotNull { id -> indexById[id] }
            .distinct()
            .sortedDescending()
            .toList()
        if (removalIndices.isEmpty()) return false

        removalIndices.forEach { index ->
            val removed = entries.removeAt(index)
            indexById.remove(removed.id)
            onActualRemovedLocked(removed.id)
            onExpectedRemovedLocked(removed.id)
        }
        reindexFromLocked(removalIndices.last())
        return true
    }

    private fun onActualAddedLocked(id: Long) {
        val mixed = mixKeepId(id)
        actualXorAcc = actualXorAcc xor mixed
        actualSumAcc += mixed
    }

    private fun onActualRemovedLocked(id: Long) {
        val mixed = mixKeepId(id)
        actualXorAcc = actualXorAcc xor mixed
        actualSumAcc -= mixed
    }

    private fun onExpectedAddedLocked(id: Long) {
        val mixed = mixKeepId(id)
        expectedXorAcc = expectedXorAcc xor mixed
        expectedSumAcc += mixed
        expectedCount += 1
    }

    private fun onExpectedRemovedLocked(id: Long) {
        val mixed = mixKeepId(id)
        expectedXorAcc = expectedXorAcc xor mixed
        expectedSumAcc -= mixed
        expectedCount = (expectedCount - 1).coerceAtLeast(0)
    }

    private fun findInsertIndexLocked(entry: KeepIndexEntry): Int {
        var low = 0
        var high = entries.size
        while (low < high) {
            val mid = (low + high) ushr 1
            val pivot = entries[mid]
            val shouldInsertBefore =
                entry.dateAdded > pivot.dateAdded ||
                    (entry.dateAdded == pivot.dateAdded && entry.id > pivot.id)
            if (shouldInsertBefore) {
                high = mid
            } else {
                low = mid + 1
            }
        }
        return low
    }

    private fun reindexFromLocked(startIndex: Int) {
        if (startIndex < 0) return
        for (index in startIndex until entries.size) {
            indexById[entries[index].id] = index
        }
    }

    private fun publishMetaLocked() {
        versionCounter += 1L
        _metaState.value = KeepGalleryMetaState(
            count = entries.size,
            expectedCount = expectedCount,
            expectedFingerprint = combineFingerprint(expectedXorAcc, expectedSumAcc, expectedCount.toLong()),
            version = versionCounter,
            isLoaded = loaded,
            hasPersistedIndex = indexFile.exists() || persistJob != null
        )
    }

    private fun schedulePersistLocked(
        immediate: Boolean = false
    ) {
        persistJob?.cancel()
        val snapshot = ArrayList(entries)
        val snapshotExpectedCount = expectedCount
        val snapshotExpectedXor = expectedXorAcc
        val snapshotExpectedSum = expectedSumAcc
        persistJob = scope.launch {
            if (!immediate) {
                delay(PERSIST_DEBOUNCE_MS)
            }
            writeSnapshot(
                entriesSnapshot = snapshot,
                expectedCountSnapshot = snapshotExpectedCount,
                expectedXorSnapshot = snapshotExpectedXor,
                expectedSumSnapshot = snapshotExpectedSum
            )
            persistJob = null
        }
    }

    private fun writeSnapshot(
        entriesSnapshot: List<KeepIndexEntry>,
        expectedCountSnapshot: Int,
        expectedXorSnapshot: Long,
        expectedSumSnapshot: Long
    ) {
        runCatching {
            tempFile.parentFile?.mkdirs()
            DataOutputStream(BufferedOutputStream(tempFile.outputStream())).use { output ->
                output.writeInt(FILE_MAGIC)
                output.writeInt(FILE_VERSION)
                output.writeInt(expectedCountSnapshot)
                output.writeInt(entriesSnapshot.size)
                output.writeLong(expectedXorSnapshot)
                output.writeLong(expectedSumSnapshot)
                entriesSnapshot.forEach { entry ->
                    output.writeLong(entry.id)
                    output.writeLong(entry.dateAdded)
                }
                output.flush()
            }
            tempFile.copyTo(indexFile, overwrite = true)
            tempFile.delete()
        }.onFailure { error ->
            Log.e(TAG, "Failed to persist keep index", error)
            runCatching { tempFile.delete() }
        }
    }
}
