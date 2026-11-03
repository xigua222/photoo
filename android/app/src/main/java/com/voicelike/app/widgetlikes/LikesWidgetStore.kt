package com.voicelike.app.widgetlikes

import android.content.Context

object LikesWidgetStore {
    private const val PREFS_NAME = "likes_widget_prefs"
    private const val KEY_URI_PREFIX = "widget_uri_"
    private const val KEY_MEDIA_ID_PREFIX = "widget_media_id_"

    fun saveSelection(context: Context, appWidgetId: Int, mediaId: Long, uriString: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_URI_PREFIX$appWidgetId", uriString)
            .putLong("$KEY_MEDIA_ID_PREFIX$appWidgetId", mediaId)
            .apply()
    }

    fun getSelectedUri(context: Context, appWidgetId: Int): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_URI_PREFIX$appWidgetId", null)
    }

    fun getSelectedMediaId(context: Context, appWidgetId: Int): Long? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong("$KEY_MEDIA_ID_PREFIX$appWidgetId", -1L)
        return if (value > 0L) value else null
    }

    fun clearSelection(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("$KEY_URI_PREFIX$appWidgetId")
            .remove("$KEY_MEDIA_ID_PREFIX$appWidgetId")
            .apply()
    }
}
