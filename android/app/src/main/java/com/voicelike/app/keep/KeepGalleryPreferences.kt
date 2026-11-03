package com.voicelike.app.keep

import android.content.Context

private const val KEEP_GALLERY_PREFS_NAME = "keep_gallery_prefs"
private const val KEEP_GALLERY_GRID_COLUMNS_KEY = "grid_columns"

internal fun loadKeepGalleryGridColumns(
    context: Context,
    defaultValue: Int,
    minValue: Int,
    maxValue: Int
): Int {
    return context.applicationContext
        .getSharedPreferences(KEEP_GALLERY_PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEEP_GALLERY_GRID_COLUMNS_KEY, defaultValue)
        .coerceIn(minValue, maxValue)
}

internal fun persistKeepGalleryGridColumns(
    context: Context,
    value: Int,
    minValue: Int,
    maxValue: Int
) {
    context.applicationContext
        .getSharedPreferences(KEEP_GALLERY_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEEP_GALLERY_GRID_COLUMNS_KEY, value.coerceIn(minValue, maxValue))
        .apply()
}
