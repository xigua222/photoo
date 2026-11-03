package com.voicelike.app

import android.content.Context

private const val COLOR_CARD_FEATURE_PREFS_NAME = "photoo_prefs"
private const val KEY_COLOR_CARD_FEATURE_ENABLED = "color_card_feature_enabled"

class ColorCardFeaturePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(COLOR_CARD_FEATURE_PREFS_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = true
        set(value) {
            prefs.edit().putBoolean(KEY_COLOR_CARD_FEATURE_ENABLED, true).apply()
        }
}
