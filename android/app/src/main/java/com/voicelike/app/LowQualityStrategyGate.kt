package com.voicelike.app

import android.content.Context

object LowQualityStrategyGate {
    const val LEGACY_STRATEGY_VERSION = 2
    const val CURRENT_STRATEGY_VERSION = 3

    private const val PREFS_NAME = "voicelike_prefs"
    private const val KEY_SHARED_LAST_VERSION_CODE = "last_version_code"
    private const val KEY_LAST_APP_VERSION_CODE = "low_quality_strategy_last_version_code"
    private const val KEY_APPROVED_STRATEGY_VERSION = "low_quality_strategy_approved_version"
    private const val KEY_PENDING_STRATEGY_VERSION = "low_quality_strategy_pending_version"

    fun registerAppLaunch(context: Context, currentVersionCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sharedLastVersion = prefs.getInt(KEY_SHARED_LAST_VERSION_CODE, 0)
        val lastAppVersion = prefs.getInt(KEY_LAST_APP_VERSION_CODE, sharedLastVersion)
        var approvedStrategyVersion = prefs.getInt(KEY_APPROVED_STRATEGY_VERSION, 0)
        var pendingStrategyVersion = prefs.getInt(KEY_PENDING_STRATEGY_VERSION, 0)

        if (approvedStrategyVersion == 0) {
            approvedStrategyVersion = if (lastAppVersion == 0) {
                CURRENT_STRATEGY_VERSION
            } else {
                LEGACY_STRATEGY_VERSION
            }
        }

        if (
            lastAppVersion != 0 &&
            lastAppVersion != currentVersionCode &&
            approvedStrategyVersion < CURRENT_STRATEGY_VERSION
        ) {
            pendingStrategyVersion = CURRENT_STRATEGY_VERSION
        }

        prefs.edit()
            .putInt(KEY_LAST_APP_VERSION_CODE, currentVersionCode)
            .putInt(KEY_APPROVED_STRATEGY_VERSION, approvedStrategyVersion)
            .putInt(KEY_PENDING_STRATEGY_VERSION, pendingStrategyVersion)
            .apply()
    }

    fun markCurrentStrategyApplied(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_APPROVED_STRATEGY_VERSION, CURRENT_STRATEGY_VERSION)
            .putInt(KEY_PENDING_STRATEGY_VERSION, 0)
            .apply()
    }

    fun resolvedStrategyVersion(context: Context?): Int {
        if (context == null) return LEGACY_STRATEGY_VERSION
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sharedLastVersion = prefs.getInt(KEY_SHARED_LAST_VERSION_CODE, 0)
        val lastAppVersion = prefs.getInt(KEY_LAST_APP_VERSION_CODE, sharedLastVersion)
        val approvedStrategyVersion = prefs.getInt(KEY_APPROVED_STRATEGY_VERSION, 0)
        val pendingStrategyVersion = prefs.getInt(KEY_PENDING_STRATEGY_VERSION, 0)

        if (approvedStrategyVersion == 0 && lastAppVersion == 0) {
            return CURRENT_STRATEGY_VERSION
        }
        if (pendingStrategyVersion >= CURRENT_STRATEGY_VERSION) {
            return LEGACY_STRATEGY_VERSION
        }
        return if (approvedStrategyVersion >= CURRENT_STRATEGY_VERSION) {
            CURRENT_STRATEGY_VERSION
        } else {
            LEGACY_STRATEGY_VERSION
        }
    }
}
