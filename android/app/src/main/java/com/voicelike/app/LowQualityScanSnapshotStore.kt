package com.voicelike.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class LowQualityScanSnapshot(
    val signature: Long,
    val reasonById: Map<Long, String>,
    val scannedIds: Set<Long>,
    val updatedAtMs: Long,
    val isFinal: Boolean,
    val version: Int = LowQualityStrategyGate.LEGACY_STRATEGY_VERSION
)

object LowQualityScanSnapshotStore {
    private const val FILE_NAME = "low_quality_scan_snapshot_v3.json"
    private const val LEGACY_FILE_NAME = "low_quality_scan_snapshot_v1.json"
    private val gson = Gson()

    fun load(context: Context): LowQualityScanSnapshot? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return runCatching {
            val json = file.readText()
            if (json.isBlank()) return null
            gson.fromJson<LowQualityScanSnapshot>(
                json,
                object : TypeToken<LowQualityScanSnapshot>() {}.type
            )
        }.getOrNull()
    }

    fun save(context: Context, snapshot: LowQualityScanSnapshot) {
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            val json = gson.toJson(snapshot)
            tmp.writeText(json)
            if (!tmp.renameTo(file)) {
                file.writeText(json)
                runCatching { tmp.delete() }
            }
        }
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
        runCatching { File(context.filesDir, "$FILE_NAME.tmp").delete() }
        runCatching { File(context.filesDir, LEGACY_FILE_NAME).delete() }
        runCatching { File(context.filesDir, "$LEGACY_FILE_NAME.tmp").delete() }
    }
}
