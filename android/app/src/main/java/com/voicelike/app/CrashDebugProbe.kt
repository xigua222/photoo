package com.voicelike.app

import android.content.Context

object CrashDebugProbe {
    private const val PREFS_NAME = "photoo_prefs"
    private const val KEY_FOREGROUND = "debug_probe_foreground"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun onProcessCreate(context: Context) {
        val appContext = context.applicationContext
        val pref = prefs(appContext)
        val wasForeground = pref.getBoolean(KEY_FOREGROUND, false)
        if (wasForeground) {
            AppRuntimeLogStore.logError(
                appContext,
                "CrashProbe",
                "Detected ungraceful previous termination while app was foreground"
            )
        }
        pref.edit()
            .putBoolean(KEY_FOREGROUND, false)
            .apply()
    }

    fun markForeground(context: Context, isForeground: Boolean) {
        val appContext = context.applicationContext
        prefs(appContext).edit()
            .putBoolean(KEY_FOREGROUND, isForeground)
            .apply()
    }
}
