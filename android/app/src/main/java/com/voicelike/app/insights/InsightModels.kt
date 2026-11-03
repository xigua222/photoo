package com.voicelike.app

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// Insights (洞察) data model, ported from iOS Views/Insights/InsightsModels.swift.
// Everything here is a plain value; InsightStatsEngine produces it, the UI only reads it.
// Day values are yyyymmdd ints (mediaDayValue), month values yyyymm ints (mediaMonthValue),
// day keys "yyyy-MM-dd", month keys "yyyy-MM". Coordinates are raw WGS-84.

// MARK: Period

enum class InsightPeriodMode { Year, Month }

/**
 * The period on screen. [month] is kept in Year mode too (1...12), so switching back to Month
 * lands on the month the user last looked at, like iOS displayedMonth.
 */
data class InsightPeriod(val mode: InsightPeriodMode, val year: Int, val month: Int = 1) {
    val isYear: Boolean get() = mode == InsightPeriodMode.Year

    /** year * 12 + month - 1 of the period's first month. */
    val startMonthIndex: Int get() = if (isYear) year * 12 else year * 12 + month - 1

    /** First and last day value (yyyymmdd) of the period. */
    val firstDayValue: Int get() = if (isYear) year * 10000 + 101 else year * 10000 + month * 100 + 1
    val lastDayValue: Int
        get() = if (isYear) year * 10000 + 1231 else year * 10000 + month * 100 + insightDaysInMonth(year, month)

    fun containsDay(dayValue: Int): Boolean =
        if (isYear) dayValue / 10000 == year else dayValue / 100 == year * 100 + month

    /** "2026" or "2026-03"; also a stable cache key together with [mode]. */
    val key: String
        get() = if (isYear) "%04d".format(Locale.US, year) else "%04d-%02d".format(Locale.US, year, month)

    /** The period before this one (last year / last month). */
    fun previous(): InsightPeriod = when {
        isYear -> copy(year = year - 1)
        month == 1 -> copy(year = year - 1, month = 12)
        else -> copy(month = month - 1)
    }

    fun next(): InsightPeriod = when {
        isYear -> copy(year = year + 1)
        month == 12 -> copy(year = year + 1, month = 1)
        else -> copy(month = month + 1)
    }

    fun withMode(newMode: InsightPeriodMode): InsightPeriod = copy(mode = newMode)

    companion object {
        fun ofYear(year: Int, month: Int = 1) = InsightPeriod(InsightPeriodMode.Year, year, month.coerceIn(1, 12))
        fun ofMonth(year: Int, month: Int) = InsightPeriod(InsightPeriodMode.Month, year, month.coerceIn(1, 12))
    }
}

// MARK: Drill-down

/**
 * A drill-down intent. Everything except [Day] and [Month] is limited to the current period
 * (see InsightStatsEngine.drillItems).
 */
sealed class InsightDrill {
    data object Period : InsightDrill()
    data object Photos : InsightDrill()
    data object Videos : InsightDrill()
    data object Screenshots : InsightDrill()
    /** Items with a place label. */
    data object Located : InsightDrill()
    data object Liked : InsightDrill()
    /** One day ("yyyy-MM-dd"), not limited to the period. */
    data class Day(val dayKey: String) : InsightDrill()
    /** One month ("yyyy-MM"), not limited to the period. */
    data class Month(val monthKey: String) : InsightDrill()
    /** Raw camera label (as stored in the snapshot, not prettified). */
    data class Camera(val raw: String) : InsightDrill()
    /** Normalised place label (insightNormalizedPlaceLabel). */
    data class Place(val label: String) : InsightDrill()
    data class Keyword(val word: String) : InsightDrill()
    /** Calendar weekday 1...7, 1 = Sunday. */
    data class Weekday(val weekday: Int) : InsightDrill()
    data class Daypart(val part: InsightDaypart) : InsightDrill()
    data class Format(val format: InsightFormat) : InsightDrill()
    data class Orientation(val orientation: InsightOrientation) : InsightDrill()
    data class Shooting(val param: InsightShootingParam, val binIndex: Int) : InsightDrill()
}

// MARK: Time of day

/** Hour ranges are [startHour, endHour). */
enum class InsightDaypart(val startHour: Int, val endHour: Int) {
    LateNight(0, 5),
    EarlyMorning(5, 9),
    Morning(9, 12),
    Afternoon(12, 17),
    Evening(17, 20),
    Night(20, 24);

    fun contains(hour: Int): Boolean = hour in startHour until endHour

    companion object {
        fun of(hour: Int): InsightDaypart = entries.firstOrNull { it.contains(hour) } ?: Night
    }
}

// MARK: Capture formats

/** Android capture formats. Single source of truth for both counting and drill-down. */
enum class InsightFormat {
    /** Live photo or motion photo. */
    Live,
    Panorama,
    Raw,
    Animated,
    ScreenRecording;

    fun matches(item: MediaItem): Boolean = when (this) {
        Live -> item.isLivePhoto || item.isMotionPhoto
        Panorama -> isPanoramaMedia(item)
        Raw -> isRawMedia(item)
        Animated -> isAnimatedMedia(item)
        ScreenRecording -> isScreenRecordingMedia(item)
    }
}

data class InsightFormatCount(val format: InsightFormat, val count: Int)

// MARK: Orientation

enum class InsightOrientation {
    Vertical, Horizontal, Square;

    companion object {
        /** Square when |w - h| / longer side < 2%; zero-sized counts as square (iOS rule). */
        fun of(width: Int, height: Int): InsightOrientation {
            val w = width.toDouble()
            val h = height.toDouble()
            val longer = max(w, h)
            if (longer <= 0) return Square
            if (abs(w - h) / longer < 0.02) return Square
            return if (h > w) Vertical else Horizontal
        }
    }
}

data class InsightOrientationSplit(val vertical: Int = 0, val horizontal: Int = 0, val square: Int = 0) {
    val total: Int get() = vertical + horizontal + square

    fun count(orientation: InsightOrientation): Int = when (orientation) {
        InsightOrientation.Vertical -> vertical
        InsightOrientation.Horizontal -> horizontal
        InsightOrientation.Square -> square
    }

    fun share(orientation: InsightOrientation): Double = if (total > 0) count(orientation).toDouble() / total else 0.0

    /** Largest share; ties resolve in Vertical, Horizontal, Square order. Null when empty. */
    val dominant: InsightOrientation?
        get() = if (total == 0) null else InsightOrientation.entries.maxByOrNull { count(it) }
}

// MARK: Shooting parameters (EXIF)

/** Focal length is 35 mm equivalent; shutter is in seconds. */
enum class InsightShootingParam {
    Focal, Aperture, Iso, Shutter;

    /** Inclusive upper bound of each bin; the last bin is open. */
    val upperBounds: DoubleArray
        get() = when (this) {
            Focal -> doubleArrayOf(20.0, 30.0, 40.0, 60.0, 100.0)
            Aperture -> doubleArrayOf(1.8, 2.4, 3.5, 5.6, 8.0)
            Iso -> doubleArrayOf(100.0, 200.0, 400.0, 800.0, 1600.0)
            Shutter -> doubleArrayOf(1.0 / 1000, 1.0 / 250, 1.0 / 60, 1.0 / 15, 0.5)
        }

    val binCount: Int get() = upperBounds.size + 1

    fun bin(value: Double): Int {
        val bounds = upperBounds
        for (i in bounds.indices) if (value <= bounds[i] * (1 + 1e-9)) return i
        return bounds.size
    }

    /** The positive, finite value of this parameter in a snapshot, or null. */
    fun value(snapshot: MediaInsightSnapshot): Double? {
        val raw: Double? = when (this) {
            Focal -> snapshot.focal35?.toDouble()
            Aperture -> snapshot.aperture?.toDouble()
            Iso -> snapshot.iso?.toDouble()
            Shutter -> snapshot.exposure
        }
        return raw?.takeIf { it.isFinite() && it > 0 }
    }

    /** One value: "26 mm", "f/1.8", "ISO 80", "1/120 s". */
    fun format(value: Double): String = when (this) {
        Focal -> "${value.roundToLong()} mm"
        Aperture -> "f/" + trimmed(value)
        Iso -> "ISO ${value.roundToLong()}"
        Shutter -> shutter(value) + " s"
    }

    /** Short axis label: "≤20", "21–30", ">100"; ISO and shutter use "≤" for every closed bin. */
    fun axisLabel(bin: Int): String {
        val bounds = upperBounds
        return when (this) {
            Focal, Aperture -> when {
                bin <= 0 -> "≤" + bare(bounds[0])
                bin >= bounds.size -> ">" + bare(bounds[bounds.size - 1])
                else -> bare(lowerBound(bin)) + "–" + bare(bounds[bin])
            }
            Iso, Shutter ->
                if (bin >= bounds.size) ">" + bare(bounds[bounds.size - 1]) else "≤" + bare(bounds[bin.coerceAtLeast(0)])
        }
    }

    /** Drill title: "21–30 mm", "f/1.9–2.4", "ISO 101–200", "1/1000–1/250 s". */
    fun rangeLabel(bin: Int): String {
        val bounds = upperBounds
        val body = when {
            bin <= 0 -> "≤" + bare(bounds[0])
            bin >= bounds.size -> ">" + bare(bounds[bounds.size - 1])
            else -> bare(lowerBound(bin)) + "–" + bare(bounds[bin])
        }
        return when (this) {
            Focal -> "$body mm"
            Aperture -> "f/$body"
            Iso -> "ISO $body"
            Shutter -> "$body s"
        }
    }

    private fun lowerBound(bin: Int): Double {
        val previous = upperBounds[bin - 1]
        return when (this) {
            Focal, Iso -> previous + 1
            Aperture -> previous + 0.1
            Shutter -> previous
        }
    }

    private fun bare(value: Double): String = when (this) {
        Focal, Iso -> value.roundToLong().toString()
        Aperture -> trimmed(value)
        Shutter -> shutter(value)
    }

    internal companion object {
        fun trimmed(value: Double): String {
            val rounded = (value * 10).roundToLong() / 10.0
            return if (rounded == Math.rint(rounded)) rounded.roundToLong().toString()
            else String.format(Locale.US, "%.1f", rounded)
        }

        /** 1 s and slower as seconds, faster as 1/N. */
        fun shutter(seconds: Double): String =
            if (seconds >= 1) trimmed(seconds) else "1/${(1 / seconds).roundToLong()}"
    }
}

data class InsightShootingBin(val index: Int, val label: String, val count: Int)

/** One parameter's distribution in the period. */
data class InsightShootingDistribution(
    val param: InsightShootingParam,
    /** Count per bin, size = param.binCount. */
    val counts: List<Int>,
    val total: Int,
    val median: Double?,
    /** Most frequent exact value (focal rounded, aperture to 0.1); null for ISO / shutter. */
    val mode: Double?,
    val modeCount: Int,
    val min: Double?,
    val max: Double?
) {
    /** Chart bars with axis labels. */
    val bins: List<InsightShootingBin>
        get() = counts.mapIndexed { i, c -> InsightShootingBin(i, param.axisLabel(i), c) }

    fun share(count: Int): Double = count.toDouble() / max(total, 1)
}

data class InsightShootingStats(
    val distributions: Map<InsightShootingParam, InsightShootingDistribution> = emptyMap(),
    /** Photos with at least one parameter. */
    val sampled: Int = 0,
    /** Day (9–17 h) vs night (20–5 h) medians; null unless each side has ≥ 8 samples. */
    val dayIso: Double? = null,
    val nightIso: Double? = null,
    val dayShutter: Double? = null,
    val nightShutter: Double? = null,
    /** Photos at 1/15 s or slower. */
    val slowShutterCount: Int = 0
) {
    fun distribution(param: InsightShootingParam): InsightShootingDistribution? =
        distributions[param]?.takeIf { it.total > 0 }

    /** Parameters that have data, in Focal, Aperture, Iso, Shutter order (segmented control). */
    val available: List<InsightShootingParam>
        get() = InsightShootingParam.entries.filter { distribution(it) != null }

    companion object {
        /** Day and night each need this many samples before comparing them. */
        const val MINIMUM_PER_SIDE = 8
    }
}

// MARK: Ranked lists / chart

/** Ranking row: [id] is the raw label (used for drill-down), display formatting is the UI's job. */
data class InsightRankedEntry(val id: String, val count: Int)

/**
 * One overview bar. Year: 12 months, key "yyyy-MM", label "1".."12" (drill Month).
 * Month: one per day, key "yyyy-MM-dd", label only on 1/8/15/22/29 (drill Day).
 */
data class InsightChartBucket(val key: String, val label: String?, val count: Int, val isFuture: Boolean)

data class InsightChart(
    val buckets: List<InsightChartBucket> = emptyList(),
    /** Index of the first future bucket (drawn as dots); buckets.size when none. */
    val futureFrom: Int = 0
) {
    val drillIsMonth: Boolean get() = buckets.firstOrNull()?.key?.length == 7

    fun drillFor(index: Int): InsightDrill? {
        val bucket = buckets.getOrNull(index) ?: return null
        return if (bucket.key.length == 7) InsightDrill.Month(bucket.key) else InsightDrill.Day(bucket.key)
    }
}

// MARK: Geography

data class InsightCoordinate(val latitude: Double, val longitude: Double)

data class InsightGeoCluster(val latitude: Double, val longitude: Double, val count: Int)

data class InsightPlacePin(val label: String, val latitude: Double, val longitude: Double, val count: Int)

/** The point farthest from home (the library's densest 0.5° cell). */
data class InsightFarthestPlace(
    val kilometers: Double,
    val label: String?,
    val latitude: Double,
    val longitude: Double,
    val homeLatitude: Double,
    val homeLongitude: Double
)

// MARK: Streak / comparison

data class InsightStreak(val length: Int, val startDayKey: String, val endDayKey: String)

enum class InsightTrend { Up, Down, Flat, None }

/** Against the previous year / month. */
data class InsightComparison(
    val isYear: Boolean = true,
    val previousTotal: Int = 0,
    /** Same-to-date count of the previous period while this period is in progress, else null. */
    val previousToDate: Int? = null,
    /** Strictly more than every other non-empty period, with at least one other. */
    val isRecord: Boolean = false,
    val totalDelta: Int = 0,
    val activeDayDelta: Int = 0
)

// MARK: Period stats

data class InsightPeriodStats(
    val period: InsightPeriod,
    /** The period contains today. */
    val isInProgress: Boolean = false,
    val total: Int = 0,
    val photos: Int = 0,
    val videos: Int = 0,
    val screenshots: Int = 0,
    val liked: Int = 0,
    val videoSeconds: Double = 0.0,
    /** 24 buckets, device time zone. */
    val hourCounts: List<Int> = List(24) { 0 },
    /** 7 buckets, index 0 = Sunday. */
    val weekdayCounts: List<Int> = List(7) { 0 },
    /** dayKey -> count for days with items. */
    val heatmap: Map<String, Int> = emptyMap(),
    /** 20/45/70/90 percentile of non-zero days (4 ascending values) or empty. */
    val heatThresholds: List<Int> = emptyList(),
    val chart: InsightChart = InsightChart(),
    val activeDays: Int = 0,
    /** Days elapsed in the period: max(period start, library start) .. min(period end, today). */
    val elapsedDays: Int = 0,
    val longestStreak: InsightStreak? = null,
    /** Busiest day; ties go to the later day. */
    val busiestDayKey: String? = null,
    val busiestDayCount: Int = 0,
    /** Newest liked non-screenshot photo of the busiest day, else its earliest photo, else earliest video. */
    val cover: MediaItem? = null,
    val coverIsLiked: Boolean = false,
    val cameraTop: List<InsightRankedEntry> = emptyList(),
    val cameraDistinct: Int = 0,
    /** Photos with camera info (share denominator). */
    val cameraTotal: Int = 0,
    val placeTop: List<InsightRankedEntry> = emptyList(),
    val placeDistinct: Int = 0,
    /** Items with a place label (share denominator). */
    val placeTotal: Int = 0,
    /** Items with coordinates. */
    val locatedCount: Int = 0,
    val placePins: List<InsightPlacePin> = emptyList(),
    /** 0.25° cells, count desc, at most 40. */
    val geoClusters: List<InsightGeoCluster> = emptyList(),
    val farthest: InsightFarthestPlace? = null,
    val travelKm: Double = 0.0,
    /** At most 24. */
    val keywordTop: List<InsightRankedEntry> = emptyList(),
    /** Non-zero formats, count desc. */
    val formats: List<InsightFormatCount> = emptyList(),
    val orientation: InsightOrientationSplit = InsightOrientationSplit(),
    val shooting: InsightShootingStats = InsightShootingStats(),
    /** Day value of the last item before the period starts (quiet period jump). */
    val lastActiveBeforeDayValue: Int? = null,
    val comparison: InsightComparison = InsightComparison(isYear = period.isYear)
) {
    val isEmpty: Boolean get() = total == 0

    fun heatLevel(count: Int): Int = InsightStatsEngine.heatLevel(count, heatThresholds)

    fun daypartCounts(): List<Int> = InsightDaypart.entries.map { part ->
        (part.startHour until part.endHour).sumOf { hourCounts.getOrElse(it) { 0 } }
    }
}

// MARK: Lifetime stats

data class InsightYearTotal(val year: Int, val count: Int)

/** Coverage over non-screenshot photos; screenshots are counted apart and never in the base. */
data class InsightCoverage(
    val photoBase: Int = 0,
    val screenshotCount: Int = 0,
    /** EXIF metadata read (snapshot.metadataScanned). */
    val scanned: Int = 0,
    val withCamera: Int = 0,
    val withLocation: Int = 0,
    val withTags: Int = 0
) {
    val pending: Int get() = max(photoBase - scanned, 0)

    fun ratio(part: Int): Double = if (photoBase > 0) part.toDouble() / photoBase else 0.0
}

/** Each year's most used camera (ties broken by name). */
data class InsightCameraYear(val year: Int, val camera: String, val count: Int, val yearTotal: Int) {
    val share: Double get() = if (yearTotal > 0) count.toDouble() / yearTotal else 0.0
}

/** Photos (no screenshots) taken on today's month-day in an earlier year. */
data class InsightOnThisDay(val yearsAgo: Int, val year: Int, val dayKey: String, val count: Int, val cover: MediaItem)

data class InsightLifetimeStats(
    /** Day value of today at compute time. */
    val todayDayValue: Int = 0,
    val firstDayValue: Int? = null,
    val firstDayKey: String? = null,
    /** First year .. max(first year, this year), empty years included. Ascending. */
    val yearTotals: List<InsightYearTotal> = emptyList(),
    /** monthKey -> count, non-zero months only. */
    val monthCounts: Map<String, Int> = emptyMap(),
    /** month value (yyyymm) -> count, non-zero months only. */
    val monthCountsByValue: Map<Int, Int> = emptyMap(),
    val total: Int = 0,
    val photoTotal: Int = 0,
    val videoSeconds: Double = 0.0,
    val activeDays: Int = 0,
    val longestStreak: Int = 0,
    val distinctPlaces: Int = 0,
    val coverage: InsightCoverage = InsightCoverage(),
    /** Ascending by year, only years with camera info. The tile shows newest first. */
    val cameraTimeline: List<InsightCameraYear> = emptyList(),
    /** Newest year first, at most 8. */
    val onThisDay: List<InsightOnThisDay> = emptyList(),
    /** Densest 0.5° cell over the whole library (with 3x3 neighbour weights). */
    val home: InsightCoordinate? = null,
    // AI prompt inputs (photos only)
    val cameraTop5: List<InsightRankedEntry> = emptyList(),
    val locationTop5: List<InsightRankedEntry> = emptyList(),
    val keywordTop30: List<InsightRankedEntry> = emptyList(),
    /** Last 12 months incl. the current one, oldest first, key "yyyy-MM". */
    val monthly12: List<InsightRankedEntry> = emptyList()
) {
    val isEmpty: Boolean get() = total == 0
    val firstYear: Int? get() = firstDayValue?.let { it / 10000 }
    val firstMonth: Int? get() = firstDayValue?.let { (it / 100) % 100 }
}

// MARK: Label helpers (shared by engine, drill and narrator)

private val COORDINATE_LABEL = Regex("""^-?\d+(?:\.\d+)?,\s*-?\d+(?:\.\d+)?$""")

/** A reverse-geocoded place name, or null for "Unknown Location", "--", blank or a "lat, lon" string. */
internal fun insightGeocodedLocation(raw: String?): String? {
    val trimmed = raw?.trim() ?: return null
    if (trimmed.isEmpty() || trimmed == "Unknown Location" || trimmed == "--") return null
    if (COORDINATE_LABEL.matches(trimmed)) return null
    return trimmed
}

/**
 * Stable city/region key: first two comma parts (full-width comma too), skipping a leading
 * street-number part when there are at least three parts. Port of InsightStatsCalculator.normalizedPlaceLabel.
 */
internal fun insightNormalizedPlaceLabel(geocoded: String?): String? {
    val trimmed = geocoded?.trim() ?: return null
    if (trimmed.isEmpty() || trimmed == "Unknown Location") return null
    val parts = trimmed.replace('，', ',').split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null
    val skipStreet = parts.size >= 3 && parts[0].any { it.isDigit() }
    return parts.drop(if (skipStreet) 1 else 0).take(2).joinToString(", ")
}

/** Place label of a snapshot as the engine counts it. */
internal fun insightPlaceLabel(snapshot: MediaInsightSnapshot?): String? =
    insightNormalizedPlaceLabel(insightGeocodedLocation(snapshot?.location))

/** The raw camera label when it is usable (trimmed non-empty and not "--"). */
internal fun insightCameraLabel(snapshot: MediaInsightSnapshot?): String? {
    val raw = snapshot?.camera ?: return null
    val trimmed = raw.trim()
    return if (trimmed.isEmpty() || trimmed == "--") null else raw
}

/**
 * Fallback camera prettifier (iOS InsightLabelFormatter.camera). The Android UI should prefer
 * DeviceModelNameResolver.resolveCameraLabel(context, raw), which maps model codes to market names.
 */
internal fun insightCameraDisplayName(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return raw
    var words = trimmed.split(' ').filter { it.isNotEmpty() }
    if (words.size > 1 && words[0].equals("Apple", ignoreCase = true) && words[1].lowercase(Locale.ROOT).startsWith("ip")) {
        return words.drop(1).joinToString(" ")
    }
    if (words.size >= 3) {
        val brand = words[0].lowercase(Locale.ROOT)
        val repeatAt = (1 until words.size).firstOrNull { words[it].lowercase(Locale.ROOT) == brand }
        if (repeatAt != null) words = words.drop(repeatAt)
    }
    val corporate = setOf("corporation", "corp.", "corp", "co.,", "ltd.", "co.,ltd.", "inc.", "imaging")
    if (words.size >= 3) {
        words = listOf(words[0]) + words.drop(1).filter { it.lowercase(Locale.ROOT) !in corporate }
    }
    val first = words.firstOrNull()
    if (first != null && first.length > 2 && first == first.uppercase(Locale.ROOT) &&
        first.any { it.isLetter() } && first.none { it.isDigit() }
    ) {
        words = listOf(first.take(1) + first.drop(1).lowercase(Locale.ROOT)) + words.drop(1)
    }
    return words.joinToString(" ")
}

/** AI tag display: underscores to spaces. */
internal fun insightKeywordDisplayName(raw: String): String = raw.replace('_', ' ')

internal fun insightDaysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
}

/** Rounded percentage helper used by tiles: 0.724 -> 72. */
internal fun insightPercentInt(fraction: Double): Int = (fraction * 100).roundToInt()
