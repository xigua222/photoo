package com.voicelike.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import java.util.UUID

internal data class SavedColorCardPalette(
    val id: String,
    val createdAt: Long,
    val sourceMediaId: Long?,
    val colors: List<Int>,
    val title: String? = null,
    val sourceTag: String? = null,
    val useChineseFont: Boolean = false,
    val favoriteKey: String? = null,
    val gradientSpec: ColorCardGradientSpec? = null
) {
    val isFavorite: Boolean
        get() = !favoriteKey.isNullOrBlank()

    val isGradient: Boolean
        get() = gradientSpec != null
}

internal fun shouldAutoNameUserCollectedPalette(
    title: String?,
    sourceTag: String?,
    favoriteKey: String?,
    gradientSpec: ColorCardGradientSpec?
): Boolean {
    return title.isNullOrBlank() &&
        sourceTag.isNullOrBlank() &&
        favoriteKey.isNullOrBlank() &&
        gradientSpec == null
}

internal fun buildUserCollectedPaletteAutoTitle(
    createdAt: Long,
    usedTitles: Set<String>
): String {
    val prefix = SimpleDateFormat("yyMMdd", Locale.US).format(Date(createdAt))
    var index = 0
    while (true) {
        val candidate = prefix + encodeColorCardAlphabetSuffix(index)
        if (candidate !in usedTitles) {
            return candidate
        }
        index += 1
    }
}

internal fun encodeColorCardAlphabetSuffix(
    index: Int
): String {
    require(index >= 0) { "index must be >= 0" }
    var working = index
    val builder = StringBuilder()
    do {
        val remainder = working % 26
        builder.append(('A'.code + remainder).toChar())
        working = (working / 26) - 1
    } while (working >= 0)
    return builder.reverse().toString()
}

internal fun normalizeUserCollectedPaletteTitles(
    palettes: List<SavedColorCardPalette>
): List<SavedColorCardPalette> {
    if (palettes.isEmpty()) return palettes

    val usedTitles = palettes.asSequence()
        .mapNotNull { palette -> palette.title?.trim()?.takeIf { it.isNotBlank() } }
        .toMutableSet()
    var changed = false
    val generatedTitlesById = mutableMapOf<String, String>()

    palettes.sortedBy(SavedColorCardPalette::createdAt).forEach { palette ->
        if (
            !shouldAutoNameUserCollectedPalette(
                title = palette.title,
                sourceTag = palette.sourceTag,
                favoriteKey = palette.favoriteKey,
                gradientSpec = palette.gradientSpec
            )
        ) {
            return@forEach
        }
        val generatedTitle = buildUserCollectedPaletteAutoTitle(
            createdAt = palette.createdAt,
            usedTitles = usedTitles
        )
        usedTitles += generatedTitle
        generatedTitlesById[palette.id] = generatedTitle
    }

    val normalized = palettes.map { palette ->
        val generatedTitle = generatedTitlesById[palette.id] ?: return@map palette
        changed = true
        palette.copy(title = generatedTitle)
    }
    return if (changed) normalized else palettes
}

internal class ColorCardStore private constructor(
    private val appContext: Context
) {
    companion object {
        private const val TAG = "ColorCardStore"
        private const val LEGACY_SNAPSHOT_PREFS = "color_card_store"
        private const val LEGACY_SNAPSHOT_PREFS_KEY = "palettes_snapshot_v1"
        internal const val MAX_PALETTES = 240
        internal const val MIN_PALETTE_COLOR_COUNT = 1
        internal const val MAX_PALETTE_COLOR_COUNT = 10

        @Volatile
        private var instance: ColorCardStore? = null

        fun get(context: Context): ColorCardStore {
            return instance ?: synchronized(this) {
                instance ?: ColorCardStore(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val legacySnapshotPrefs = appContext.getSharedPreferences(LEGACY_SNAPSHOT_PREFS, Context.MODE_PRIVATE)
    private val persistence = ColorCardStorePersistence(
        storageFile = File(appContext.filesDir, "color_cards_v2.bin"),
        legacyStorageFile = File(appContext.filesDir, "color_cards.json"),
        logError = { message, error -> Log.e(TAG, message, error) },
        logInfo = { message -> Log.i(TAG, message) }
    )
    private val _palettes = MutableStateFlow<List<SavedColorCardPalette>>(emptyList())

    private var loaded = false

    val palettes: StateFlow<List<SavedColorCardPalette>> = _palettes.asStateFlow()

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

    suspend fun savePalette(
        colors: List<Int>,
        sourceMediaId: Long?,
        title: String? = null,
        sourceTag: String? = null,
        useChineseFont: Boolean = false,
        favoriteKey: String? = null,
        gradientSpec: ColorCardGradientSpec? = null
    ): SavedColorCardPalette? {
        val normalizedColors = colors.take(MAX_PALETTE_COLOR_COUNT)
        if (normalizedColors.size < MIN_PALETTE_COLOR_COUNT) return null

        return mutex.withLock {
            ensureLoadedLocked()
            val normalizedTitle = title?.trim().orEmpty().ifBlank { null }
            val normalizedSourceTag = sourceTag?.trim().orEmpty().ifBlank { null }
            val normalizedFavoriteKey = favoriteKey?.trim().orEmpty().ifBlank { null }
            val normalizedGradientSpec = gradientSpec?.let(::normalizeColorCardGradientSpec)
            val savedAt = System.currentTimeMillis()
            val resolvedTitle = if (
                shouldAutoNameUserCollectedPalette(
                    title = normalizedTitle,
                    sourceTag = normalizedSourceTag,
                    favoriteKey = normalizedFavoriteKey,
                    gradientSpec = normalizedGradientSpec
                )
            ) {
                buildUserCollectedPaletteAutoTitle(
                    createdAt = savedAt,
                    usedTitles = _palettes.value.asSequence()
                        .mapNotNull { palette -> palette.title?.takeIf { it.isNotBlank() } }
                        .toSet()
                )
            } else {
                normalizedTitle
            }
            val saved = SavedColorCardPalette(
                id = UUID.randomUUID().toString(),
                createdAt = savedAt,
                sourceMediaId = sourceMediaId,
                colors = normalizedColors,
                title = resolvedTitle,
                sourceTag = normalizedSourceTag,
                useChineseFont = useChineseFont,
                favoriteKey = normalizedFavoriteKey,
                gradientSpec = normalizedGradientSpec
            )
            val updated = (listOf(saved) + _palettes.value)
                .filterNot { existing ->
                    existing.id != saved.id &&
                        !saved.favoriteKey.isNullOrBlank() &&
                        existing.favoriteKey == saved.favoriteKey
                }
                .sortedByDescending { it.createdAt }
                .take(MAX_PALETTES)
            if (!persistLocked(updated)) {
                return@withLock null
            }
            _palettes.value = updated
            saved
        }
    }

    fun savePaletteBlocking(
        colors: List<Int>,
        sourceMediaId: Long?,
        title: String? = null,
        sourceTag: String? = null,
        useChineseFont: Boolean = false,
        favoriteKey: String? = null,
        gradientSpec: ColorCardGradientSpec? = null
    ): SavedColorCardPalette? = runBlocking(Dispatchers.IO) {
        savePalette(
            colors = colors,
            sourceMediaId = sourceMediaId,
            title = title,
            sourceTag = sourceTag,
            useChineseFont = useChineseFont,
            favoriteKey = favoriteKey,
            gradientSpec = gradientSpec
        )
    }

    suspend fun deletePalette(id: String) {
        mutex.withLock {
            ensureLoadedLocked()
            val updated = _palettes.value.filterNot { it.id == id }
            if (updated.size == _palettes.value.size) return
            if (!persistLocked(updated)) return
            _palettes.value = updated
        }
    }

    fun deletePaletteBlocking(id: String) {
        runBlocking(Dispatchers.IO) {
            deletePalette(id)
        }
    }

    private fun ensureLoadedLocked() {
        if (loaded) return

        val loadResult = persistence.loadSnapshot(
            legacyMirroredSnapshot = loadLegacyMirroredSnapshotRaw()
        )
        val palettes = normalizeUserCollectedPaletteTitles(loadResult.palettes)
        val paletteTitlesNormalized = palettes !== loadResult.palettes

        loaded = true
        _palettes.value = palettes

        when {
            paletteTitlesNormalized -> {
                if (!persistence.persist(palettes)) {
                    Log.e(TAG, "Failed to rewrite color-card snapshot after generating palette titles")
                }
            }

            loadResult.requiresRewrite &&
                (
                    loadResult.source == ColorCardStoreSnapshotSource.BinaryRecovered ||
                        palettes.isNotEmpty()
                    ) -> {
                if (!persistence.persist(palettes)) {
                    Log.e(TAG, "Failed to rewrite recovered color-card snapshot from ${loadResult.source}")
                }
            }

            loadResult.requiresRewrite -> {
                Log.i(TAG, "Skipped rewriting empty recovered color-card snapshot from ${loadResult.source}")
            }

            loadResult.source == ColorCardStoreSnapshotSource.Corrupted -> {
                Log.e(TAG, "All discovered color-card snapshots were unreadable; keeping empty in-memory state without overwriting disk")
            }

            else -> {
                Log.i(TAG, "Loaded ${palettes.size} color cards from ${loadResult.source}")
            }
        }
    }

    private fun persistLocked(palettes: List<SavedColorCardPalette>): Boolean {
        if (!persistence.persist(palettes)) {
            Log.e(TAG, "Failed to persist color cards to binary snapshot")
            return false
        }
        return true
    }

    private fun loadLegacyMirroredSnapshotRaw(): String? {
        return legacySnapshotPrefs.getString(LEGACY_SNAPSHOT_PREFS_KEY, null)
            ?.takeIf { it.isNotBlank() }
    }
}

internal fun selectColorCardPaletteSnapshot(
    filePalettes: List<SavedColorCardPalette>?,
    mirroredPalettes: List<SavedColorCardPalette>?
): List<SavedColorCardPalette> {
    return listOfNotNull(filePalettes, mirroredPalettes)
        .maxWithOrNull(
            compareBy<List<SavedColorCardPalette>>(
                { snapshot -> snapshot.maxOfOrNull(SavedColorCardPalette::createdAt) ?: Long.MIN_VALUE },
                { snapshot -> snapshot.size }
            )
        )
        ?: emptyList()
}
