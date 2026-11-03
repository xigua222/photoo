package com.voicelike.app

import android.content.Context

object ScanCacheMigrationManager {
    private const val PREFS_NAME = "voicelike_prefs"
    private const val LEGACY_PREFS_NAME = "photoo_prefs"
    private const val KEY_SCAN_CACHE_SCHEMA_VERSION = "scan_cache_schema_version"
    private const val CURRENT_SCAN_CACHE_SCHEMA_VERSION = 3
    private const val KEY_LOW_QUALITY_RESULTS = "low_quality_results"
    private const val KEY_SCANNED_LOW_QUALITY_IDS = "scanned_low_quality_ids"

    fun migrateIfNeeded(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSchemaVersion = prefs.getInt(KEY_SCAN_CACHE_SCHEMA_VERSION, 0)
        if (lastSchemaVersion >= CURRENT_SCAN_CACHE_SCHEMA_VERSION) return

        val success = runCatching {
            clearIncompatibleScanCaches(appContext)
            AppRuntimeLogStore.logInfo(
                appContext,
                "ScanCacheMigration",
                "Cleared incompatible scan caches for schema $CURRENT_SCAN_CACHE_SCHEMA_VERSION"
            )
        }.onFailure { error ->
            AppRuntimeLogStore.logError(
                appContext,
                "ScanCacheMigration",
                "Failed to clear incompatible scan caches",
                error
            )
        }.isSuccess

        if (success) {
            prefs.edit()
                .putInt(KEY_SCAN_CACHE_SCHEMA_VERSION, CURRENT_SCAN_CACHE_SCHEMA_VERSION)
                .apply()
        }
    }

    private fun clearIncompatibleScanCaches(context: Context) {
        CatalogScanCoordinator.flush()
        SimilarPhotoManager.resetSession()
        LowQualityManager.resetSession()
        MediaAnalysisManager.clearPersistentScanCaches(context)
        SimilarScanSnapshotStore.clear(context)
        LowQualityScanSnapshotStore.clear(context)
        context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LOW_QUALITY_RESULTS)
            .remove(KEY_SCANNED_LOW_QUALITY_IDS)
            .apply()
    }
}
