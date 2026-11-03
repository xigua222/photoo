package com.voicelike.app.motionphoto

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class MotionPhotoSessionPaths(
    val sessionId: String,
    val sessionDir: File,
    val tempDir: File,
    val framesDir: File,
    val outputDir: File
)

internal class MotionPhotoFileRepository(
    private val context: Context
) {
    private val rootDir: File
        get() = File(context.filesDir, "motion_photo_filter")

    fun ensureRoot(): File = rootDir.apply { mkdirs() }

    fun createSession(displayName: String): MotionPhotoSessionPaths {
        val normalizedName = displayName
            .substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "image" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sessionId = "${normalizedName}_$timestamp"
        val sessionDir = File(ensureRoot(), sessionId).apply { mkdirs() }
        val tempDir = File(sessionDir, "temp").apply { mkdirs() }
        val framesDir = File(sessionDir, "frames").apply { mkdirs() }
        val outputDir = File(sessionDir, "output").apply { mkdirs() }
        return MotionPhotoSessionPaths(
            sessionId = sessionId,
            sessionDir = sessionDir,
            tempDir = tempDir,
            framesDir = framesDir,
            outputDir = outputDir
        )
    }
}
