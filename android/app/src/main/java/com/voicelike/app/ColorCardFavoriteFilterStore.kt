package com.voicelike.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ColorCardFavoriteFilterStore private constructor(
    private val appContext: Context
) {
    companion object {
        private const val PREFS_NAME = "color_card_favorite_filters"
        private const val KEY_FAVORITE_IDS = "favorite_ids"

        @Volatile
        private var instance: ColorCardFavoriteFilterStore? = null

        fun get(context: Context): ColorCardFavoriteFilterStore {
            return instance ?: synchronized(this) {
                instance ?: ColorCardFavoriteFilterStore(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _favoriteFilterIds = MutableStateFlow(loadFavoriteIds())

    val favoriteFilterIds: StateFlow<List<String>> = _favoriteFilterIds.asStateFlow()

    fun toggle(
        paletteId: String
    ) {
        val normalizedId = paletteId.trim()
        if (normalizedId.isBlank()) return
        synchronized(this) {
            val current = loadFavoriteIds().toMutableList()
            if (current.remove(normalizedId).not()) {
                current.add(0, normalizedId)
            }
            persist(current)
        }
    }

    fun isFavorite(
        paletteId: String
    ): Boolean = favoriteFilterIds.value.contains(paletteId.trim())

    private fun persist(
        ids: List<String>
    ) {
        val normalized = ids
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
        prefs.edit()
            .putString(KEY_FAVORITE_IDS, normalized.joinToString(separator = "\n"))
            .apply()
        _favoriteFilterIds.value = normalized
    }

    private fun loadFavoriteIds(): List<String> {
        return prefs.getString(KEY_FAVORITE_IDS, null)
            ?.split('\n')
            .orEmpty()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    }
}
