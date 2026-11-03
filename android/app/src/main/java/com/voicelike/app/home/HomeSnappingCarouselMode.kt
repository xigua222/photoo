package com.voicelike.app

import android.content.Context

private const val HOME_SNAPPING_CAROUSEL_LAB_PREFS = "photoo_home_card_backtrack_lab"
private const val KEY_HOME_SNAPPING_CAROUSEL_ENABLED = "home_card_backtrack_enabled"

class HomeSnappingCarouselLabPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(
        HOME_SNAPPING_CAROUSEL_LAB_PREFS,
        Context.MODE_PRIVATE
    )

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_HOME_SNAPPING_CAROUSEL_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HOME_SNAPPING_CAROUSEL_ENABLED, value).apply()
        }
}
