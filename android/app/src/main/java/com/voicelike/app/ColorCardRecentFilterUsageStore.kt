package com.voicelike.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ColorCardRecentFilterUsageStore private constructor(
    private val appContext: Context
) {
    companion object {
        private const val PREFS_NAME = "color_card_recent_filter_usage"
        private const val KEY_RECENT_IDS = "recent_ids"
        private const val MAX_TRACKED_FILTERS = 64

        @Volatile
        private var instance: ColorCardRecentFilterUsageStore? = null

        fun get(context: Context): ColorCardRecentFilterUsageStore {
            return instance ?: synchronized(this) {
                instance ?: ColorCardRecentFilterUsageStore(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _topFilterIds = MutableStateFlow(loadTopFilterIds())

    val topFilterIds: StateFlow<List<String>> = _topFilterIds.asStateFlow()

    fun recordExpandedPickerSelection(
        paletteId: String
    ) {
        val normalizedId = paletteId.trim()
        if (normalizedId.isBlank()) return
        synchronized(this) {
            val nextIds = buildList {
                add(normalizedId)
                addAll(loadStoredIds().filterNot { it == normalizedId })
            }.take(MAX_TRACKED_FILTERS)
            prefs.edit()
                .putString(KEY_RECENT_IDS, nextIds.joinToString(separator = "\n"))
                .apply()
            _topFilterIds.value = loadTopFilterIds()
        }
    }

    private fun loadTopFilterIds(
        limit: Int = 5
    ): List<String> {
        return loadStoredIds().take(limit)
    }

    private fun loadStoredIds(): List<String> {
        return prefs.getString(KEY_RECENT_IDS, null)
            ?.split('\n')
            .orEmpty()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(MAX_TRACKED_FILTERS)
    }
}

// gate probe
private val gateProbe : Int = 1
