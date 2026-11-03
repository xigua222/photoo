package com.voicelike.app.stamp

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val STAMP_CALENDAR_PREFS_NAME = "stamp_calendar_prefs"
private const val STAMP_CALENDAR_PREFS_KEY = "stamp_calendar_days_v1"
private const val STAMP_CALENDAR_COLLAPSED_PREFS_KEY = "stamp_calendar_collapsed_v1"
private const val STAMP_CALENDAR_WALL_META_PREFS_KEY = "stamp_calendar_wall_meta_v1"
private const val STAMP_CALENDAR_LOG_TAG = "StampCalendarStore"
// The stamp images live here; the index backup sits next to them so both survive or go together.
private const val STAMP_CALENDAR_FILES_DIR = "stamp_calendar"
private const val STAMP_CALENDAR_INDEX_BACKUP_FILE = "index_backup_v1.json"
private val STAMP_FILE_NAME_PATTERN = Regex("""^photoo_stamp_(\d{8}_\d{6}_\d{3})_\d+\.png$""")
private val stampCalendarDataVersion = MutableStateFlow(0)
@Volatile
private var stampCalendarEntriesCacheRaw: String? = null
@Volatile
private var stampCalendarEntriesCache: Map<String, StampCalendarDayEntry>? = null
@Volatile
private var stampCalendarParseFailureLoggedForRaw: String? = null

// Every entries mutation rewrites the whole snapshot (read all → modify → write
// all), so overlapping calls from different threads would drop each other's
// writes. All entries reads/writes are serialized onto this single thread.
private val stampCalendarStoreExecutor: ExecutorService =
    Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "stamp-calendar-store").apply { isDaemon = true }
    }

private fun <T> runSerializedStampCalendarOperation(block: () -> T): T {
    return try {
        stampCalendarStoreExecutor.submit(Callable { block() }).get()
    } catch (e: ExecutionException) {
        throw (e.cause ?: e)
    }
}

internal data class StampCalendarDayEntry(
    val dayKey: String,
    val selectedUri: String?,
    val uris: List<String>,
    val note: String = "",
    val noteByUri: Map<String, String> = emptyMap(),
    val emojiByUri: Map<String, String> = emptyMap(),
    val skinByUri: Map<String, StampCalendarStampSkin> = emptyMap(),
    val cutoutLockedByUri: Map<String, Boolean> = emptyMap(),
    val punchStyleByUri: Map<String, String> = emptyMap()
)

internal data class StampCalendarParsedDate(
    val year: Int,
    val month: Int,
    val day: Int
)

internal fun stampCalendarTodayKey(): String = stampCalendarDayKey(Calendar.getInstance())

internal fun observeStampCalendarDataVersion() = stampCalendarDataVersion.asStateFlow()

internal fun loadStampCalendarCollapsedPreference(context: Context): Boolean {
    return context.applicationContext.getSharedPreferences(
        STAMP_CALENDAR_PREFS_NAME,
        Context.MODE_PRIVATE
    ).getBoolean(STAMP_CALENDAR_COLLAPSED_PREFS_KEY, false)
}

internal fun updateStampCalendarCollapsedPreference(
    context: Context,
    collapsed: Boolean
) {
    context.applicationContext.getSharedPreferences(
        STAMP_CALENDAR_PREFS_NAME,
        Context.MODE_PRIVATE
    ).edit()
        .putBoolean(STAMP_CALENDAR_COLLAPSED_PREFS_KEY, collapsed)
        .apply()
}

internal fun loadStampCalendarWallMetaPreference(context: Context): Boolean {
    return context.applicationContext.getSharedPreferences(
        STAMP_CALENDAR_PREFS_NAME,
        Context.MODE_PRIVATE
    ).getBoolean(STAMP_CALENDAR_WALL_META_PREFS_KEY, false)
}

internal fun updateStampCalendarWallMetaPreference(
    context: Context,
    enabled: Boolean
) {
    context.applicationContext.getSharedPreferences(
        STAMP_CALENDAR_PREFS_NAME,
        Context.MODE_PRIVATE
    ).edit()
        .putBoolean(STAMP_CALENDAR_WALL_META_PREFS_KEY, enabled)
        .apply()
}

internal fun stampCalendarMonthKey(calendar: Calendar): String =
    String.format(
        Locale.US,
        "%04d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1
    )

internal fun stampCalendarDayKey(calendar: Calendar): String =
    String.format(
        Locale.US,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )

internal fun stampCalendarDayKey(
    year: Int,
    month: Int,
    day: Int
): String {
    val resolvedMonth = month.coerceIn(1, 12)
    val resolvedDay = day.coerceIn(1, stampCalendarDaysInMonth(year, resolvedMonth))
    return String.format(
        Locale.US,
        "%04d-%02d-%02d",
        year,
        resolvedMonth,
        resolvedDay
    )
}

internal fun stampCalendarDaysInMonth(
    year: Int,
    month: Int
): Int {
    val resolvedMonth = month.coerceIn(1, 12)
    return Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, resolvedMonth - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
}

internal fun parseStampCalendarDate(dayKey: String): StampCalendarParsedDate {
    parseCompactStampCalendarDate(dayKey)?.let { compactDate ->
        return StampCalendarParsedDate(
            year = compactDate.year,
            month = compactDate.month,
            day = compactDate.day
        )
    }
    val today = Calendar.getInstance()
    val parts = dayKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: today.get(Calendar.YEAR)
    val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12)
        ?: (today.get(Calendar.MONTH) + 1)
    val maxDay = stampCalendarDaysInMonth(year, month)
    val day = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(1, maxDay)
        ?: today.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDay)
    return StampCalendarParsedDate(
        year = year,
        month = month,
        day = day
    )
}

private data class StampCalendarSortDate(
    val year: Int,
    val month: Int,
    val day: Int
)

private fun parseStampCalendarDateForSort(dayKey: String): StampCalendarSortDate? {
    parseCompactStampCalendarDate(dayKey)?.let { compactDate ->
        return StampCalendarSortDate(
            year = compactDate.year,
            month = compactDate.month,
            day = compactDate.day
        )
    }
    val parts = dayKey.split("-")
    if (parts.size < 3) return null
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
    if (year !in 1..9999) return null
    val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: return null
    val maxDay = stampCalendarDaysInMonth(year, month)
    val day = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(1, maxDay) ?: return null
    return StampCalendarSortDate(
        year = year,
        month = month,
        day = day
    )
}

private fun parseCompactStampCalendarDate(dayKey: String): StampCalendarParsedDate? {
    val compact = dayKey.trim()
    if (compact.length != 8 || !compact.all(Char::isDigit)) return null
    val year = compact.substring(0, 4).toIntOrNull() ?: return null
    if (year !in 1..9999) return null
    val month = compact.substring(4, 6).toIntOrNull()?.coerceIn(1, 12) ?: return null
    val maxDay = stampCalendarDaysInMonth(year, month)
    val day = compact.substring(6, 8).toIntOrNull()?.coerceIn(1, maxDay) ?: return null
    return StampCalendarParsedDate(
        year = year,
        month = month,
        day = day
    )
}

private fun parseStampCalendarMonthKeyForSort(monthKey: String): Pair<Int, Int>? {
    val parts = monthKey.split("-")
    if (parts.size < 2) return null
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
    if (year !in 1..9999) return null
    val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: return null
    return year to month
}

internal fun stampCalendarSortValue(dayKey: String): Long {
    val parsed = parseStampCalendarDateForSort(dayKey) ?: return Long.MIN_VALUE
    return (parsed.year.toLong() * 10_000L) +
        (parsed.month.toLong() * 100L) +
        parsed.day.toLong()
}

internal fun sortStampCalendarEntriesDescending(
    entries: Iterable<StampCalendarDayEntry>
): List<StampCalendarDayEntry> {
    return entries.sortedWith(
        compareByDescending<StampCalendarDayEntry> { stampCalendarSortValue(it.dayKey) }
            .thenByDescending { it.dayKey }
    )
}

internal fun sortStampCalendarEntriesAscending(
    entries: Iterable<StampCalendarDayEntry>
): List<StampCalendarDayEntry> {
    return entries.sortedWith(
        compareBy<StampCalendarDayEntry> { stampCalendarSortValue(it.dayKey) }
            .thenBy { it.dayKey }
    )
}

internal fun stampCalendarDayOfMonth(dayKey: String): Int {
    return parseStampCalendarDateForSort(dayKey)?.day ?: parseStampCalendarDate(dayKey).day
}

internal fun loadStampCalendarMonthEntries(
    context: Context,
    monthKey: String
): Map<Int, StampCalendarDayEntry> = runSerializedStampCalendarOperation {
    val targetMonth = parseStampCalendarMonthKeyForSort(monthKey)
        ?: return@runSerializedStampCalendarOperation emptyMap()
    val targetYear = targetMonth.first
    val targetMonthValue = targetMonth.second
    readStampCalendarEntries(context)
        .values
        .mapNotNull { entry ->
            val parsed = parseStampCalendarDateForSort(entry.dayKey) ?: return@mapNotNull null
            if (parsed.year != targetYear || parsed.month != targetMonthValue) {
                return@mapNotNull null
            }
            parsed.day to entry
        }
        .toMap()
}

internal fun loadAllStampCalendarEntries(context: Context): List<StampCalendarDayEntry> =
    runSerializedStampCalendarOperation {
        sortStampCalendarEntriesDescending(readStampCalendarEntries(context).values)
    }

internal fun addStampToCalendarDay(
    context: Context,
    dayKey: String,
    uriString: String,
    punchStyleKey: String? = null
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
    val nextUris = buildList {
        existing?.uris?.forEach { add(it) }
        if (uriString !in this) {
            add(uriString)
        }
    }
    val normalizedPunchStyleKey = punchStyleKey
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    entries[dayKey] = StampCalendarDayEntry(
        dayKey = dayKey,
        selectedUri = existing?.selectedUri ?: nextUris.firstOrNull(),
        uris = nextUris,
        note = existing?.note.orEmpty(),
        noteByUri = existing?.noteByUri.orEmpty().filterKeys { it in nextUris },
        emojiByUri = existing?.emojiByUri.orEmpty().filterKeys { it in nextUris },
        skinByUri = existing?.skinByUri.orEmpty().filterKeys { it in nextUris },
        cutoutLockedByUri = existing?.cutoutLockedByUri.orEmpty()
            .filterKeys { it in nextUris },
        punchStyleByUri = existing?.punchStyleByUri.orEmpty()
            .toMutableMap()
            .apply {
                if (normalizedPunchStyleKey == null) {
                    remove(uriString)
                } else {
                    put(uriString, normalizedPunchStyleKey)
                }
            }
            .filterKeys { it in nextUris }
    )
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun selectStampForCalendarDay(
    context: Context,
    dayKey: String,
    uriString: String
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
        ?: return@runSerializedStampCalendarOperation true
    if (uriString !in existing.uris) {
        return@runSerializedStampCalendarOperation true
    }
    entries[dayKey] = existing.copy(selectedUri = uriString)
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun removeStampFromCalendarDay(
    context: Context,
    dayKey: String,
    uriString: String
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
        ?: return@runSerializedStampCalendarOperation true
    val remainingUris = existing.uris.filterNot { it == uriString }
    if (remainingUris.isEmpty()) {
        entries.remove(dayKey)
    } else {
        val nextSelected = existing.selectedUri
            ?.takeIf { it in remainingUris }
            ?: remainingUris.firstOrNull()
        entries[dayKey] = existing.copy(
            selectedUri = nextSelected,
            uris = remainingUris,
            noteByUri = existing.noteByUri.filterKeys { it in remainingUris },
            emojiByUri = existing.emojiByUri.filterKeys { it in remainingUris },
            skinByUri = existing.skinByUri.filterKeys { it in remainingUris },
            cutoutLockedByUri = existing.cutoutLockedByUri.filterKeys { it in remainingUris },
            punchStyleByUri = existing.punchStyleByUri.filterKeys { it in remainingUris }
        )
    }
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun updateStampCalendarDayNote(
    context: Context,
    dayKey: String,
    note: String
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
        ?: return@runSerializedStampCalendarOperation true
    entries[dayKey] = existing.copy(note = note.trimEnd())
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun updateStampCalendarStampNote(
    context: Context,
    dayKey: String,
    uriString: String,
    note: String
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
        ?: return@runSerializedStampCalendarOperation true
    if (uriString !in existing.uris) {
        return@runSerializedStampCalendarOperation true
    }
    val trimmedNote = note.trimEnd()
    val nextNoteByUri = existing.noteByUri.toMutableMap().apply {
        if (trimmedNote.isBlank()) {
            remove(uriString)
        } else {
            put(uriString, trimmedNote)
        }
    }
    entries[dayKey] = existing.copy(noteByUri = nextNoteByUri)
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun updateStampCalendarStampEmoji(
    context: Context,
    dayKey: String,
    uriString: String,
    emoji: String?
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
        ?: return@runSerializedStampCalendarOperation true
    if (uriString !in existing.uris) {
        return@runSerializedStampCalendarOperation true
    }
    val nextEmojiByUri = existing.emojiByUri.toMutableMap().apply {
        if (emoji.isNullOrBlank()) {
            remove(uriString)
        } else {
            put(uriString, emoji)
        }
    }
    entries[dayKey] = existing.copy(emojiByUri = nextEmojiByUri)
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun updateStampCalendarStampSkin(
    context: Context,
    dayKey: String,
    uriString: String,
    skin: StampCalendarStampSkin
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
        ?: return@runSerializedStampCalendarOperation true
    if (uriString !in existing.uris) {
        return@runSerializedStampCalendarOperation true
    }
    val normalizedSkin = skin.normalized()
    val nextSkinByUri = existing.skinByUri.toMutableMap().apply {
        if (normalizedSkin.isOriginalStyle()) {
            remove(uriString)
        } else {
            put(uriString, normalizedSkin)
        }
    }
    entries[dayKey] = existing.copy(skinByUri = nextSkinByUri)
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun updateStampCalendarStampCutoutLock(
    context: Context,
    dayKey: String,
    uriString: String,
    locked: Boolean
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
        ?: return@runSerializedStampCalendarOperation true
    if (uriString !in existing.uris) {
        return@runSerializedStampCalendarOperation true
    }
    val nextLockByUri = existing.cutoutLockedByUri.toMutableMap().apply {
        if (locked) {
            put(uriString, true)
        } else {
            remove(uriString)
        }
    }
    entries[dayKey] = existing.copy(cutoutLockedByUri = nextLockByUri)
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun updateStampCalendarStampPunchStyle(
    context: Context,
    dayKey: String,
    uriString: String,
    punchStyleKey: String?
): Boolean = runSerializedStampCalendarOperation {
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val existing = entries[dayKey]
        ?: return@runSerializedStampCalendarOperation true
    if (uriString !in existing.uris) {
        return@runSerializedStampCalendarOperation true
    }
    val normalizedKey = punchStyleKey
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val nextPunchStyleByUri = existing.punchStyleByUri.toMutableMap().apply {
        if (normalizedKey == null) {
            remove(uriString)
        } else {
            put(uriString, normalizedKey)
        }
    }
    entries[dayKey] = existing.copy(punchStyleByUri = nextPunchStyleByUri)
    writeStampCalendarEntries(context, entries.values.toList())
}

internal fun moveStampToCalendarDay(
    context: Context,
    fromDayKey: String,
    toDayKey: String,
    uriString: String
): Boolean = runSerializedStampCalendarOperation {
    if (fromDayKey == toDayKey) {
        return@runSerializedStampCalendarOperation true
    }
    val entries = readStampCalendarEntriesOrNull(context)
        ?.toMutableMap()
        ?: return@runSerializedStampCalendarOperation false
    val sourceEntry = entries[fromDayKey]
        ?: return@runSerializedStampCalendarOperation true
    if (uriString !in sourceEntry.uris) {
        return@runSerializedStampCalendarOperation true
    }

    val movedNote = (
        sourceEntry.noteByUri[uriString]
            ?.trimEnd()
            ?.takeIf { it.isNotEmpty() }
            ?: sourceEntry.note.trimEnd().takeIf { it.isNotEmpty() }
        )
    val movedEmoji = sourceEntry.emojiByUri[uriString]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val movedSkin = sourceEntry.skinByUri[uriString]?.normalized()
    val movedCutoutLock = sourceEntry.cutoutLockedByUri[uriString] == true
    val movedPunchStyle = sourceEntry.punchStyleByUri[uriString]

    val remainingUris = sourceEntry.uris.filterNot { it == uriString }
    if (remainingUris.isEmpty()) {
        entries.remove(fromDayKey)
    } else {
        val nextSelected = sourceEntry.selectedUri
            ?.takeIf { it in remainingUris }
            ?: remainingUris.firstOrNull()
        entries[fromDayKey] = sourceEntry.copy(
            selectedUri = nextSelected,
            uris = remainingUris,
            noteByUri = sourceEntry.noteByUri.filterKeys { it in remainingUris },
            emojiByUri = sourceEntry.emojiByUri.filterKeys { it in remainingUris },
            skinByUri = sourceEntry.skinByUri.filterKeys { it in remainingUris },
            cutoutLockedByUri = sourceEntry.cutoutLockedByUri.filterKeys { it in remainingUris },
            punchStyleByUri = sourceEntry.punchStyleByUri.filterKeys { it in remainingUris }
        )
    }

    val targetEntry = entries[toDayKey]
    val nextTargetUris = buildList {
        targetEntry?.uris?.forEach { uri ->
            if (uri !in this) {
                add(uri)
            }
        }
        if (uriString !in this) {
            add(uriString)
        }
    }
    val nextTargetNoteByUri = targetEntry?.noteByUri.orEmpty().toMutableMap().apply {
        if (movedNote.isNullOrBlank()) {
            remove(uriString)
        } else {
            put(uriString, movedNote)
        }
    }
    val nextTargetEmojiByUri = targetEntry?.emojiByUri.orEmpty().toMutableMap().apply {
        if (movedEmoji.isNullOrBlank()) {
            remove(uriString)
        } else {
            put(uriString, movedEmoji)
        }
    }
    val nextTargetSkinByUri = targetEntry?.skinByUri.orEmpty().toMutableMap().apply {
        if (movedSkin == null || movedSkin.isOriginalStyle()) {
            remove(uriString)
        } else {
            put(uriString, movedSkin)
        }
    }
    val nextTargetLockByUri = targetEntry?.cutoutLockedByUri.orEmpty().toMutableMap().apply {
        if (movedCutoutLock) {
            put(uriString, true)
        } else {
            remove(uriString)
        }
    }
    val nextTargetPunchStyleByUri = targetEntry?.punchStyleByUri.orEmpty().toMutableMap().apply {
        if (movedPunchStyle.isNullOrBlank()) {
            remove(uriString)
        } else {
            put(uriString, movedPunchStyle)
        }
    }
    entries[toDayKey] = StampCalendarDayEntry(
        dayKey = toDayKey,
        selectedUri = uriString,
        uris = nextTargetUris,
        note = targetEntry?.note.orEmpty(),
        noteByUri = nextTargetNoteByUri.filterKeys { it in nextTargetUris },
        emojiByUri = nextTargetEmojiByUri.filterKeys { it in nextTargetUris },
        skinByUri = nextTargetSkinByUri.filterKeys { it in nextTargetUris },
        cutoutLockedByUri = nextTargetLockByUri.filterKeys { it in nextTargetUris },
        punchStyleByUri = nextTargetPunchStyleByUri.filterKeys { it in nextTargetUris }
    )
    writeStampCalendarEntries(context, entries.values.toList())
}

private fun readStampCalendarEntries(context: Context): Map<String, StampCalendarDayEntry> =
    readStampCalendarEntriesOrNull(context) ?: emptyMap()

// Returns null when the stored JSON fails to parse. Mutating callers must bail
// out instead of persisting an empty snapshot over the unreadable (but real)
// data — an empty-map fallback here would wipe the whole calendar on the next
// write.
private fun readStampCalendarEntriesOrNull(context: Context): Map<String, StampCalendarDayEntry>? {
    val prefs = context.applicationContext.getSharedPreferences(
        STAMP_CALENDAR_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    val storedRaw = prefs.getString(STAMP_CALENDAR_PREFS_KEY, null).orEmpty()
    // An empty read is not proof the calendar is empty (the prefs file can fail to load or be
    // lost); treating it as empty let the next save overwrite every earlier stamp.
    val raw = storedRaw.ifBlank {
        recoverBlankStampCalendarIndex(context) ?: return emptyMap()
    }
    val cachedRaw = stampCalendarEntriesCacheRaw
    val cachedEntries = stampCalendarEntriesCache
    if (cachedEntries != null && cachedRaw == raw) {
        return cachedEntries
    }

    val parsedEntries = runCatching {
        val root = JSONObject(raw)
        val days = root.optJSONObject("days") ?: JSONObject()
        buildMap {
            days.keys().forEach { key ->
                val dayObject = days.optJSONObject(key) ?: return@forEach
                val uriArray = dayObject.optJSONArray("uris") ?: JSONArray()
                val uris = buildList {
                    for (index in 0 until uriArray.length()) {
                        val value = uriArray.optString(index).trim()
                        if (value.isNotEmpty() && value !in this) {
                            add(value)
                        }
                    }
                }
                val selectedUri = dayObject.optString("selectedUri")
                    .trim()
                    .takeIf { it.isNotEmpty() && it in uris }
                    ?: uris.firstOrNull()
                val note = dayObject.optString("note").orEmpty()
                val noteObject = dayObject.optJSONObject("noteByUri") ?: JSONObject()
                val noteByUri = buildMap {
                    noteObject.keys().forEach { uri ->
                        val itemNote = noteObject.optString(uri).trimEnd()
                        if (uri in uris && itemNote.isNotEmpty()) {
                            put(uri, itemNote)
                        }
                    }
                    if (note.isNotBlank() && selectedUri != null && selectedUri !in this) {
                        put(selectedUri, note)
                    }
                }
                val emojiObject = dayObject.optJSONObject("emojiByUri") ?: JSONObject()
                val emojiByUri = buildMap {
                    emojiObject.keys().forEach { uri ->
                        val emoji = emojiObject.optString(uri).trim()
                        if (uri in uris && emoji.isNotEmpty()) {
                            put(uri, emoji)
                        }
                    }
                }
                val skinObject = dayObject.optJSONObject("skinByUri") ?: JSONObject()
                val skinByUri = buildMap {
                    skinObject.keys().forEach { uri ->
                        if (uri !in uris) return@forEach
                        val skinEntry = skinObject.optJSONObject(uri) ?: return@forEach
                        val style = when (skinEntry.optString("style").trim()) {
                            "white_border" -> StampCalendarSkinStyle.WhiteBorder
                            "editorial" -> StampCalendarSkinStyle.Editorial
                            "cutout_auto" -> StampCalendarSkinStyle.Cutout
                            else -> StampCalendarSkinStyle.Original
                        }
                        val imageEffect = when (skinEntry.optString("imageEffect").trim()) {
                            "cartoon" -> StampCalendarImageEffect.Cartoon
                            "ink_spread" -> StampCalendarImageEffect.InkSpread
                            "texture_overlay" -> StampCalendarImageEffect.TextureOverlay
                            "color_quantize" -> StampCalendarImageEffect.ColorQuantize
                            "halftone" -> StampCalendarImageEffect.Halftone
                            "misprint_offset" -> StampCalendarImageEffect.MisprintOffset
                            else -> StampCalendarImageEffect.None
                        }
                        val skin = StampCalendarStampSkin(
                            style = style,
                            numberLabel = skinEntry.optString("numberLabel").orEmpty(),
                            title = skinEntry.optString("title").orEmpty(),
                            subtitle = skinEntry.optString("subtitle").orEmpty(),
                            cutoutAssetPath = skinEntry.optString("cutoutAssetPath").orEmpty(),
                            backgroundColorArgb = if (skinEntry.has("backgroundColorArgb")) {
                                skinEntry.optInt("backgroundColorArgb")
                            } else {
                                0
                            },
                            imageEffect = imageEffect
                        ).normalized()
                        if (!skin.isOriginalStyle()) {
                            put(uri, skin)
                        }
                    }
                }
                val cutoutLockObject = dayObject.optJSONObject("cutoutLockedByUri") ?: JSONObject()
                val cutoutLockedByUri = buildMap {
                    cutoutLockObject.keys().forEach { uri ->
                        if (uri !in uris) return@forEach
                        if (cutoutLockObject.optBoolean(uri, false)) {
                            put(uri, true)
                        }
                    }
                }
                val punchStyleObject = dayObject.optJSONObject("punchStyleByUri") ?: JSONObject()
                val punchStyleByUri = buildMap {
                    punchStyleObject.keys().forEach { uri ->
                        if (uri !in uris) return@forEach
                        val styleKey = punchStyleObject.optString(uri).trim()
                        if (styleKey.isNotEmpty()) {
                            put(uri, styleKey)
                        }
                    }
                }
                if (uris.isNotEmpty()) {
                    put(
                        key,
                        StampCalendarDayEntry(
                            dayKey = key,
                            selectedUri = selectedUri,
                            uris = uris,
                            note = note,
                            noteByUri = noteByUri,
                            emojiByUri = emojiByUri,
                            skinByUri = skinByUri,
                            cutoutLockedByUri = cutoutLockedByUri,
                            punchStyleByUri = punchStyleByUri
                        )
                    )
                }
            }
        }
    }.getOrNull()
    if (parsedEntries == null) {
        if (stampCalendarParseFailureLoggedForRaw != raw) {
            stampCalendarParseFailureLoggedForRaw = raw
            Log.e(
                STAMP_CALENDAR_LOG_TAG,
                "stamp calendar json parse failed, bytes=${raw.length}"
            )
        }
        // The backup holds the last index that parsed and committed; bring it back if it differs.
        val backupRaw = readStampCalendarIndexBackup(context)
        if (backupRaw != null && backupRaw != raw && restoreStampCalendarIndex(context, backupRaw)) {
            return readStampCalendarEntriesOrNull(context)
        }
        return null
    }
    stampCalendarEntriesCacheRaw = raw
    stampCalendarEntriesCache = parsedEntries
    // Users upgrading from builds without a backup get one on their first read.
    if (parsedEntries.isNotEmpty() && !stampCalendarIndexBackupFile(context).exists()) {
        writeStampCalendarIndexBackup(context, raw)
    }
    return parsedEntries
}

private fun stampCalendarFilesDir(context: Context): File =
    File(context.applicationContext.filesDir, STAMP_CALENDAR_FILES_DIR)

private fun stampCalendarIndexBackupFile(context: Context): File =
    File(stampCalendarFilesDir(context), STAMP_CALENDAR_INDEX_BACKUP_FILE)

private fun readStampCalendarIndexBackup(context: Context): String? = runCatching {
    stampCalendarIndexBackupFile(context).takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() }
}.getOrNull()

/** Atomic (temp + fsync + rename), so a crash mid-write keeps the previous backup intact. */
private fun writeStampCalendarIndexBackup(context: Context, serialized: String) {
    runCatching {
        val dir = stampCalendarFilesDir(context)
        if (!dir.exists() && !dir.mkdirs()) return
        val target = File(dir, STAMP_CALENDAR_INDEX_BACKUP_FILE)
        val tmp = File(dir, "$STAMP_CALENDAR_INDEX_BACKUP_FILE.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(serialized.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }.onFailure { Log.e(STAMP_CALENDAR_LOG_TAG, "stamp calendar index backup failed", it) }
}

private fun restoreStampCalendarIndex(context: Context, raw: String): Boolean {
    val restored = runCatching {
        context.applicationContext.getSharedPreferences(STAMP_CALENDAR_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(STAMP_CALENDAR_PREFS_KEY, raw)
            .commit()
    }.getOrDefault(false)
    Log.w(STAMP_CALENDAR_LOG_TAG, "stamp calendar index restored from backup ok=$restored bytes=${raw.length}")
    return restored
}

/**
 * The index read empty: bring back the backup, or, without one, rebuild the days from the stamp
 * files themselves (their names carry the capture time). Null means there really is nothing.
 */
private fun recoverBlankStampCalendarIndex(context: Context): String? {
    readStampCalendarIndexBackup(context)?.let { backupRaw ->
        if (restoreStampCalendarIndex(context, backupRaw)) return backupRaw
    }
    val entries = rebuildStampCalendarEntriesFromFiles(context)
    if (entries.isEmpty()) return null
    Log.w(STAMP_CALENDAR_LOG_TAG, "stamp calendar index rebuilt from files days=${entries.size}")
    if (!writeStampCalendarEntries(context, entries)) return null
    return context.applicationContext
        .getSharedPreferences(STAMP_CALENDAR_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(STAMP_CALENDAR_PREFS_KEY, null)
        ?.takeIf { it.isNotBlank() }
}

private fun rebuildStampCalendarEntriesFromFiles(context: Context): List<StampCalendarDayEntry> {
    val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
    val stamps = stampCalendarFilesDir(context).listFiles().orEmpty()
        .filter { it.isFile && it.length() > 0L }
        .mapNotNull { file ->
            val timestamp = STAMP_FILE_NAME_PATTERN.matchEntire(file.name)?.groupValues?.get(1)
                ?: return@mapNotNull null
            val time = runCatching { timestampFormat.parse(timestamp) }.getOrNull() ?: return@mapNotNull null
            Triple(time.time, file, stampCalendarDayKey(Calendar.getInstance().apply { this.time = time }))
        }
        .sortedBy { it.first }
    return stamps.groupBy({ it.third }, { Uri.fromFile(it.second).toString() })
        .map { (dayKey, uris) -> StampCalendarDayEntry(dayKey = dayKey, selectedUri = uris.first(), uris = uris) }
}

private fun writeStampCalendarEntries(
    context: Context,
    entries: List<StampCalendarDayEntry>
): Boolean {
    val root = JSONObject()
    val days = JSONObject()
    sortStampCalendarEntriesAscending(entries)
        .forEach { entry ->
            if (entry.uris.isEmpty()) return@forEach
            val dayObject = JSONObject()
            dayObject.put("selectedUri", entry.selectedUri ?: entry.uris.first())
            val uriArray = JSONArray()
            entry.uris.distinct().forEach { uriArray.put(it) }
            dayObject.put("uris", uriArray)
            dayObject.put("note", entry.note)
            val noteObject = JSONObject()
            entry.noteByUri
                .filterKeys { it in entry.uris }
                .forEach { (uri, note) ->
                    if (note.isNotBlank()) {
                        noteObject.put(uri, note)
                    }
                }
            dayObject.put("noteByUri", noteObject)
            val emojiObject = JSONObject()
            entry.emojiByUri
                .filterKeys { it in entry.uris }
                .forEach { (uri, emoji) ->
                    if (emoji.isNotBlank()) {
                        emojiObject.put(uri, emoji)
                    }
                }
            dayObject.put("emojiByUri", emojiObject)
            val skinObject = JSONObject()
            entry.skinByUri
                .filterKeys { it in entry.uris }
                .forEach { (uri, skin) ->
                    val normalizedSkin = skin.normalized()
                    if (!normalizedSkin.isOriginalStyle()) {
                        skinObject.put(
                            uri,
                            JSONObject().apply {
                                put(
                                    "style",
                                    when (normalizedSkin.style) {
                                        StampCalendarSkinStyle.WhiteBorder -> "white_border"
                                        StampCalendarSkinStyle.Editorial -> "editorial"
                                        StampCalendarSkinStyle.Cutout -> "cutout_auto"
                                        StampCalendarSkinStyle.Original -> "original"
                                    }
                                )
                                put(
                                    "imageEffect",
                                    when (normalizedSkin.imageEffect) {
                                        StampCalendarImageEffect.None -> "none"
                                        StampCalendarImageEffect.Cartoon -> "cartoon"
                                        StampCalendarImageEffect.InkSpread -> "ink_spread"
                                        StampCalendarImageEffect.TextureOverlay -> "texture_overlay"
                                        StampCalendarImageEffect.ColorQuantize -> "color_quantize"
                                        StampCalendarImageEffect.Halftone -> "halftone"
                                        StampCalendarImageEffect.MisprintOffset -> "misprint_offset"
                                    }
                                )
                                put("numberLabel", normalizedSkin.numberLabel)
                                put("title", normalizedSkin.title)
                                put("subtitle", normalizedSkin.subtitle)
                                put("cutoutAssetPath", normalizedSkin.cutoutAssetPath)
                                put("backgroundColorArgb", normalizedSkin.backgroundColorArgb)
                            }
                        )
                    }
                }
            dayObject.put("skinByUri", skinObject)
            val cutoutLockObject = JSONObject()
            entry.cutoutLockedByUri
                .filterKeys { it in entry.uris }
                .forEach { (uri, locked) ->
                    if (locked) {
                        cutoutLockObject.put(uri, true)
                    }
                }
            dayObject.put("cutoutLockedByUri", cutoutLockObject)
            val punchStyleObject = JSONObject()
            entry.punchStyleByUri
                .filterKeys { it in entry.uris }
                .forEach { (uri, styleKey) ->
                    if (styleKey.isNotBlank()) {
                        punchStyleObject.put(uri, styleKey)
                    }
                }
            dayObject.put("punchStyleByUri", punchStyleObject)
            days.put(entry.dayKey, dayObject)
        }
    root.put("days", days)
    val serialized = root.toString()
    val prefs = context.applicationContext.getSharedPreferences(
        STAMP_CALENDAR_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    var committed = runCatching {
        prefs.edit()
            .putString(STAMP_CALENDAR_PREFS_KEY, serialized)
            .commit()
    }.getOrDefault(false)
    if (!committed) {
        Log.e(STAMP_CALENDAR_LOG_TAG, "stamp calendar commit failed, retrying once")
        committed = runCatching {
            prefs.edit()
                .putString(STAMP_CALENDAR_PREFS_KEY, serialized)
                .commit()
        }.getOrDefault(false)
    }
    if (!committed) {
        Log.e(
            STAMP_CALENDAR_LOG_TAG,
            "stamp calendar commit failed twice; entry count=${entries.size}"
        )
        return false
    }
    stampCalendarEntriesCacheRaw = serialized
    stampCalendarEntriesCache = entries
        .filter { it.uris.isNotEmpty() }
        .associateBy { it.dayKey }
    writeStampCalendarIndexBackup(context, serialized)
    stampCalendarDataVersion.value += 1
    return true
}
