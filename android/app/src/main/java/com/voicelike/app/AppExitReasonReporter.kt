package com.voicelike.app

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logs why earlier processes of the app ended (Android 11+), so a crash report says "native crash
 * in libhwui.so" or "ANR in main thread at …" instead of only "ungraceful termination". Runs once
 * per launch and only reports exits newer than the last one it saw.
 */
object AppExitReasonReporter {
    private const val PREFS_NAME = "photoo_prefs"
    private const val KEY_LAST_EXIT_TS = "debug_probe_last_reported_exit_ts"
    private const val TAG = "ExitReason"
    private const val MAX_EXITS = 8
    private const val ANR_TRACE_MAX_LINES = 40
    private const val TOMBSTONE_SCAN_BYTES = 256 * 1024
    private const val TOMBSTONE_MAX_HINTS = 12

    fun reportPreviousExits(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        runCatching { reportLocked(context.applicationContext) }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun reportLocked(context: Context) {
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastReported = prefs.getLong(KEY_LAST_EXIT_TS, 0L)
        val exits = activityManager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS)
            .filter { it.timestamp > lastReported }
            .sortedBy { it.timestamp }
        if (exits.isEmpty()) return
        // First run after install: only remember where we are, older history is not ours to explain.
        if (lastReported > 0L) {
            exits.filter(::isWorthReporting).forEach { exit ->
                AppRuntimeLogStore.logError(context, TAG, describe(exit))
            }
        }
        prefs.edit().putLong(KEY_LAST_EXIT_TS, exits.maxOf { it.timestamp }).apply()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isWorthReporting(exit: ApplicationExitInfo): Boolean = when (exit.reason) {
        ApplicationExitInfo.REASON_CRASH,
        ApplicationExitInfo.REASON_CRASH_NATIVE,
        ApplicationExitInfo.REASON_ANR,
        ApplicationExitInfo.REASON_LOW_MEMORY,
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> true
        // Kills by the system while the app was on screen also explain a "foreground termination".
        ApplicationExitInfo.REASON_SIGNALED,
        ApplicationExitInfo.REASON_OTHER,
        ApplicationExitInfo.REASON_UNKNOWN ->
            exit.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
        else -> false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun describe(exit: ApplicationExitInfo): String = buildString {
        val time = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(exit.timestamp))
        append("Previous process ended at ").append(time)
        append(": ").append(reasonName(exit.reason))
        if (exit.reason == ApplicationExitInfo.REASON_CRASH_NATIVE || exit.reason == ApplicationExitInfo.REASON_SIGNALED) {
            append(" signal=").append(exit.status)
        }
        append(", ").append(importanceName(exit.importance))
        if (exit.pss > 0) append(", pss=").append(exit.pss / 1024).append("MB")
        if (exit.rss > 0) append(", rss=").append(exit.rss / 1024).append("MB")
        exit.description?.takeIf { it.isNotBlank() }?.let { append(", ").append(it.replace('\n', ' ')) }
        when (exit.reason) {
            ApplicationExitInfo.REASON_ANR -> anrTrace(exit)?.let { append('\n').append(it) }
            ApplicationExitInfo.REASON_CRASH_NATIVE ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) tombstoneHints(exit)?.let { append('\n').append(it) }
        }
    }

    /** The main thread section of the ANR trace: what the UI thread was stuck on. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun anrTrace(exit: ApplicationExitInfo): String? = runCatching {
        exit.traceInputStream?.bufferedReader()?.use { reader ->
            val lines = reader.lineSequence()
                .dropWhile { !it.startsWith("\"main\"") }
                .takeWhile { it.isNotBlank() }
                .take(ANR_TRACE_MAX_LINES)
                .toList()
            lines.takeIf { it.isNotEmpty() }?.joinToString("\n") { "  $it" }
        }
    }.getOrNull()

    /**
     * Native tombstones are protobuf; without a parser the readable strings still show which
     * library and function the crashing thread was in (e.g. libhwui RenderThread frames).
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun tombstoneHints(exit: ApplicationExitInfo): String? = runCatching {
        val bytes = exit.traceInputStream?.use { it.readNBytesCompat(TOMBSTONE_SCAN_BYTES) } ?: return null
        val hints = LinkedHashSet<String>()
        val run = StringBuilder()
        fun flush() {
            val text = cleanTombstoneString(run.toString())
            if (text.length >= 6 && (text.contains(".so") || text.contains("::") || text.contains("Thread") || text.startsWith("Abort"))) {
                hints += text
            }
            run.setLength(0)
        }
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E) run.append(c.toChar()) else flush()
            if (hints.size >= TOMBSTONE_MAX_HINTS) break
        }
        flush()
        hints.take(TOMBSTONE_MAX_HINTS).takeIf { it.isNotEmpty() }?.joinToString("\n") { "  native: $it" }
    }.getOrNull()

    // Protobuf framing leaks into the printable runs: a length byte in front, a build id behind.
    private val TOMBSTONE_BUILD_ID = Regex("""B [0-9a-f]{16,}.*$""")

    private fun cleanTombstoneString(raw: String): String = raw
        .replace(TOMBSTONE_BUILD_ID, "")
        .trimStart { !(it.isLetter() || it == '/' || it == '_') }
        .substringBeforeLast('"')
        .trimEnd { it == '(' || it == '"' || it.isWhitespace() }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (out.size() < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "CRASH (Java)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY (killed by the system)"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_OTHER -> "OTHER"
        else -> "UNKNOWN($reason)"
    }

    private fun importanceName(importance: Int): String = when {
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "foreground"
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "visible"
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "service"
        else -> "background"
    }
}
