package com.voicelike.app

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RuntimeLogEntry(
    val ts: Long,
    val level: String,
    val tag: String,
    val message: String,
    val thread: String
) {
    fun formatLine(): String {
        val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
        return "${fmt.format(Date(ts))} [$level/$tag] $message (${thread})"
    }
}

object AppRuntimeLogStore {
    private const val LOG_FILE_NAME = "runtime_events_v1.jsonl"
    private const val EXPORT_FILE_NAME = "runtime_events_export.txt"
    private const val RETENTION_MS = 24L * 60L * 60L * 1000L
    private const val MAX_LINES = 5000
    private const val MAX_FILE_BYTES = 2L * 1024L * 1024L
    private val hiddenCatalogStartupMessagesForIndexDebug = listOf(
        "Keeping verified catalog during full refresh",
        "Warm catalog cache restored"
    )
    private val lock = Any()
    private const val R8_MAP_ID_PREFIX = "r8-map-id-"
    private const val CRASH_MAX_FRAMES = 40
    private const val ERROR_MAX_FRAMES = 8
    private const val OOM_MAX_FRAMES = 12
    private const val MAX_CAUSES = 4
    private const val CRASH_DEDUPE_WINDOW_MS = 3_000L
    private var lastCrashSignature: String? = null
    private var lastCrashUptimeMs = 0L

    fun logError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        append(context, "ERROR", tag, enrichMessage(message, throwable))
    }

    fun logInfo(context: Context, tag: String, message: String) {
        append(context, "INFO", tag, message)
    }

    fun logCrash(context: Context, tag: String, throwable: Throwable) {
        // Several threads can die on the same bug in the same instant; one full trace is enough.
        val signature = "${throwable.javaClass.name}@${throwable.stackTrace.firstOrNull()}"
        val now = SystemClock.uptimeMillis()
        synchronized(lock) {
            if (signature == lastCrashSignature && now - lastCrashUptimeMs < CRASH_DEDUPE_WINDOW_MS) return
            lastCrashSignature = signature
            lastCrashUptimeMs = now
        }
        append(context, "CRASH", tag, "${throwable.message ?: "uncaught exception"}\n${stackTraceText(throwable, CRASH_MAX_FRAMES)}")
    }

    /**
     * App version, build type and R8 mapping id, so an exported log can be matched to the exact
     * mapping file. The id is what R8 writes into every class's SourceFile ("r8-map-id-<id>").
     */
    fun buildSummary(context: Context): String {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val versionName = packageInfo?.versionName ?: "?"
        val versionCode = packageInfo?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode.toLong()
        } ?: -1L
        val buildType = if (BuildConfig.DEBUG) "debug" else "release"
        return "$versionName ($versionCode) $buildType, mapping ${r8MapId ?: "none"}"
    }

    private val r8MapId: String? by lazy {
        Throwable().stackTrace.firstOrNull()?.fileName
            ?.takeIf { it.startsWith(R8_MAP_ID_PREFIX) }
            ?.removePrefix(R8_MAP_ID_PREFIX)
    }

    fun logCrashLowOverhead(context: Context, tag: String, throwable: Throwable) {
        val now = System.currentTimeMillis()
        val msg = buildString {
            append(throwable.javaClass.simpleName)
            val detail = throwable.message.orEmpty()
            if (detail.isNotBlank()) {
                append(": ")
                append(detail.replace('\n', ' ').replace('\r', ' '))
            }
            // Kept short under memory pressure, but still in retrace-able "at" form.
            throwable.stackTrace.take(OOM_MAX_FRAMES).forEach { frame ->
                append(" | at ")
                append(frame.toString())
            }
        }
        val line = "$now\tCRASH\t$tag\t${Thread.currentThread().name}\t$msg\n"
        synchronized(lock) {
            val file = File(context.filesDir, LOG_FILE_NAME)
            appendRawLocked(file, line)
        }
    }

    fun logJank(context: Context, message: String) {
        append(context, "JANK", "Frame", message)
    }

    fun readRecent(context: Context, limit: Int = 80): List<RuntimeLogEntry> {
        val file = File(context.filesDir, LOG_FILE_NAME)
        if (!file.exists()) return emptyList()
        val now = System.currentTimeMillis()
        val cutoff = now - RETENTION_MS
        val entries = file.readLines()
            .mapNotNull { parseLine(it) }
            .filter { it.ts >= cutoff }
        return entries.takeLast(limit).asReversed()
    }

    fun readRecentForIndexDebug(context: Context, limit: Int = 80): List<RuntimeLogEntry> {
        return readRecent(context, MAX_LINES)
            .asReversed()
            .filterNot(::shouldHideFromIndexDebug)
            .takeLast(limit)
            .asReversed()
    }

    fun clear(context: Context) {
        synchronized(lock) {
            kotlin.runCatching { File(context.filesDir, LOG_FILE_NAME).delete() }
        }
    }

    fun exportLast24hToCache(context: Context): File {
        val exportFile = File(context.cacheDir, EXPORT_FILE_NAME)
        val lines = readRecent(context, MAX_LINES).asReversed().map { it.formatLine() }
        exportFile.writeText(lines.joinToString(separator = "\n"))
        return exportFile
    }

    fun logFile(context: Context): File = File(context.filesDir, LOG_FILE_NAME)

    private fun append(context: Context, level: String, tag: String, message: String) {
        val now = System.currentTimeMillis()
        val item = RuntimeLogEntry(
            ts = now,
            level = level,
            tag = tag,
            message = message,
            thread = Thread.currentThread().name
        )
        synchronized(lock) {
            val file = File(context.filesDir, LOG_FILE_NAME)
            // Under memory pressure, avoid JSON object allocations and expensive full-file pruning.
            if (isOomLike(item.message)) {
                appendRawLocked(file, "${item.ts}\t${item.level}\t${item.tag}\t${item.thread}\t${sanitizeOneLine(item.message)}\n")
                return
            }
            if (file.exists() && file.length() > MAX_FILE_BYTES) {
                pruneLocked(file, now - RETENTION_MS)
            }
            appendRawLocked(file, encodeLine(item) + "\n")
        }
    }

    private fun pruneLocked(file: File, cutoff: Long) {
        if (!file.exists()) return
        val kept = file.readLines()
            .mapNotNull { parseLine(it) }
            .filter { it.ts >= cutoff }
            .takeLast(MAX_LINES)
        file.writeText(kept.joinToString(separator = "\n") { encodeLine(it) } + if (kept.isNotEmpty()) "\n" else "")
    }

    private fun encodeLine(item: RuntimeLogEntry): String {
        // Build JSON manually to avoid JSONObject's internal LinkedHashMap allocations on hot paths.
        return buildString(160) {
            append("{\"ts\":")
            append(item.ts)
            append(",\"level\":\"")
            append(jsonEscape(item.level))
            append("\",\"tag\":\"")
            append(jsonEscape(item.tag))
            append("\",\"message\":\"")
            append(jsonEscape(item.message))
            append("\",\"thread\":\"")
            append(jsonEscape(item.thread))
            append("\"}")
        }
    }

    private fun parseLine(line: String): RuntimeLogEntry? {
        return parseJsonLine(line) ?: parseTabSeparatedLine(line)
    }

    private fun parseJsonLine(line: String): RuntimeLogEntry? {
        return kotlin.runCatching {
            val json = JSONObject(line)
            RuntimeLogEntry(
                ts = json.optLong("ts", 0L),
                level = json.optString("level", "INFO"),
                tag = json.optString("tag", "App"),
                message = json.optString("message", ""),
                thread = json.optString("thread", "unknown")
            )
        }.getOrNull()
    }

    private fun parseTabSeparatedLine(line: String): RuntimeLogEntry? {
        val parts = line.split('\t', limit = 5)
        if (parts.size < 5) return null
        val ts = parts[0].toLongOrNull() ?: return null
        return RuntimeLogEntry(
            ts = ts,
            level = parts[1].ifBlank { "INFO" },
            tag = parts[2].ifBlank { "App" },
            message = parts[4],
            thread = parts[3].ifBlank { "unknown" }
        )
    }

    private fun enrichMessage(message: String, throwable: Throwable?): String {
        if (throwable == null) return message
        return "$message | ${stackTraceText(throwable, ERROR_MAX_FRAMES)}"
    }

    /**
     * Exception, frames and cause chain in the standard `at class.method(file:line)` form, so the
     * exported log can go straight through R8 retrace. Frame keeps R8's "r8-map-id-…" source name.
     */
    private fun stackTraceText(throwable: Throwable, maxFrames: Int): String = buildString {
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth <= MAX_CAUSES) {
            if (depth > 0) append("\nCaused by: ")
            append(current.javaClass.name)
            current.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(sanitizeOneLine(it)) }
            val frames = current.stackTrace
            frames.take(maxFrames).forEach { frame -> append("\n  at ").append(frame.toString()) }
            if (frames.size > maxFrames) append("\n  ... ${frames.size - maxFrames} more")
            current = current.cause?.takeIf { it !== current }
            depth++
        }
    }

    private fun appendRawLocked(file: File, text: String) {
        runCatching {
            FileOutputStream(file, true).use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private fun isOomLike(message: String): Boolean {
        val lower = message.lowercase(Locale.US)
        return lower.contains("outofmemoryerror") || lower.contains("failed to allocate")
    }

    private fun shouldHideFromIndexDebug(entry: RuntimeLogEntry): Boolean {
        if (entry.tag != "CatalogStartup" || entry.level != "INFO") return false
        return hiddenCatalogStartupMessagesForIndexDebug.any { prefix ->
            entry.message.startsWith(prefix)
        }
    }

    private fun sanitizeOneLine(text: String): String {
        return text.replace('\n', ' ').replace('\r', ' ')
    }

    private fun jsonEscape(text: String): String {
        val src = text
        val sb = StringBuilder(src.length + 16)
        src.forEach { ch ->
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append("\\u")
                        sb.append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }
}

object AppJankMonitor {
    private const val JANK_FRAME_GAP_MS = 120f
    private const val LOG_INTERVAL_MS = 1500L
    private var started = false
    private var lastFrameNs = 0L
    private var lastLogUptimeMs = 0L

    fun start(context: Context) {
        if (started) return
        started = true
        val choreographer = android.view.Choreographer.getInstance()
        val callback = object : android.view.Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameNs != 0L) {
                    val gapMs = (frameTimeNanos - lastFrameNs) / 1_000_000f
                    if (gapMs >= JANK_FRAME_GAP_MS) {
                        val now = SystemClock.uptimeMillis()
                        if (now - lastLogUptimeMs >= LOG_INTERVAL_MS) {
                            lastLogUptimeMs = now
                            AppRuntimeLogStore.logJank(context, "main frame gap ${"%.1f".format(Locale.US, gapMs)}ms")
                        }
                    }
                }
                lastFrameNs = frameTimeNanos
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)
    }
}
