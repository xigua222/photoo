package com.voicelike.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ColorCardFilterBrowserTabStore private constructor(
    private val appContext: Context
) {
    companion object {
        private const val PREFS_NAME = "color_card_filter_browser_tabs"
        private const val KEY_SELECTED_TAB = "selected_filter_tab"

        @Volatile
        private var instance: ColorCardFilterBrowserTabStore? = null

        fun get(context: Context): ColorCardFilterBrowserTabStore {
            return instance ?: synchronized(this) {
                instance ?: ColorCardFilterBrowserTabStore(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _selectedTabKey = MutableStateFlow(loadSelectedTabKey())

    val selectedTabKey: StateFlow<String> = _selectedTabKey.asStateFlow()

    fun setSelectedTabKey(
        key: String
    ) {
        val normalized = key.trim().ifBlank { "common" }
        prefs.edit().putString(KEY_SELECTED_TAB, normalized).apply()
        _selectedTabKey.value = normalized
    }

    private fun loadSelectedTabKey(): String {
        return prefs.getString(KEY_SELECTED_TAB, "common").orEmpty().ifBlank { "common" }
    }
}
