package com.voicelike.app

import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Insights statistics, ported from iOS Views/Insights/InsightStatsEngine.swift and
// Views/Mine/InsightStatsViewModel.swift (smart default period).
//
// Pure functions, safe to run on Dispatchers.Default. Performance rules (100k items):
// - one pass over the items per call, at most one snapshot lookup per item;
// - local dates come from MediaLocalDateCache (mediaDayValue), never a Calendar per item;
//   the weekday is derived from the day value, the hour from the zone offset;
// - per-call caches for place-label normalisation (one regex check per distinct raw string).

object InsightStatsEngine {

    // MARK: Lifetime

    /**
     * Whole-library numbers: first day, year/month totals, streak, coverage, camera timeline,
     * on-this-day, home coordinate and the AI prompt inputs.
     */
    fun computeLifetime(
        items: List<MediaItem>,
        snapshots: Map<Long, MediaInsightSnapshot>,
        @Suppress("UNUSED_PARAMETER") likedIds: Set<Long>,
        nowMs: Long = System.currentTimeMillis()
    ): InsightLifetimeStats {
        val today = todayDayValue(nowMs)
        val todayYear = today / 10000
        val todayMonthDay = today % 10000
        val labels = PlaceLabelCache(placeAliases(snapshots))

        var total = 0
        var photoTotal = 0
        var videoSeconds = 0.0
        var firstDay = Int.MAX_VALUE
        val dayCounts = HashMap<Int, IntBox>(4096)
        val monthCounts = HashMap<Int, IntBox>(256)
        val cameraCounts = HashMap<String, IntBox>()
        val labelPhotoCounts = HashMap<String, IntBox>()
        val labelSeen = HashSet<String>()
        val keywordCounts = HashMap<String, IntBox>()
        val homeCells = HashMap<Int, GeoSum>()
        val cameraYears = HashMap<Int, HashMap<String, IntBox>>()
        var photoBase = 0
        var screenshotCount = 0
        var scanned = 0
        var withCamera = 0
        var withLocation = 0
        var withTags = 0
        val onThisDay = HashMap<Int, OnThisDayAcc>()

        for (item in items) {
            total++
            val isPhoto = item.type == "photo"
            if (isPhoto) photoTotal++ else videoSeconds += item.duration / 1000.0
            val day = mediaDayValue(item)
            val validDay = day / 10000 >= MIN_YEAR
            if (validDay) {
                dayCounts.getOrPut(day) { IntBox() }.value++
                monthCounts.getOrPut(day / 100) { IntBox() }.value++
                if (day < firstDay) firstDay = day
            }

            val snapshot = snapshots[item.id]
            val camera = insightCameraLabel(snapshot)
            val label = labels.label(snapshot)
            if (label != null) labelSeen.add(label)
            val hasCoordinate = snapshot != null && hasUsableCoordinate(snapshot)
            if (hasCoordinate) {
                homeCells.getOrPut(cellKey(snapshot!!.latitude!!, snapshot.longitude!!, 0.5)) { GeoSum() }
                    .add(snapshot.latitude!!, snapshot.longitude!!)
            }
            if (!isPhoto) continue

            if (camera != null) cameraCounts.getOrPut(camera) { IntBox() }.value++
            if (label != null) labelPhotoCounts.getOrPut(label) { IntBox() }.value++
            val keywords = snapshot?.keywords.orEmpty()
            if (keywords.isNotEmpty()) forEachDistinct(keywords) { keywordCounts.getOrPut(it) { IntBox() }.value++ }

            if (isScreenshotMedia(item)) {
                screenshotCount++
                continue
            }
            photoBase++
            if (snapshot?.metadataScanned == true) scanned++
            if (camera != null) withCamera++
            if (hasCoordinate) withLocation++
            if (keywords.isNotEmpty()) withTags++
            if (camera != null && validDay) {
                cameraYears.getOrPut(day / 10000) { HashMap() }.getOrPut(camera) { IntBox() }.value++
            }
            if (validDay && day % 10000 == todayMonthDay && day / 10000 < todayYear) {
                val acc = onThisDay.getOrPut(day / 10000) { OnThisDayAcc(day) }
                acc.count++
                val current = acc.cover
                if (current == null || newer(item, current)) acc.cover = item
            }
        }

        val hasFirst = firstDay != Int.MAX_VALUE
        val monthByValue = HashMap<Int, Int>(monthCounts.size)
        val monthByKey = HashMap<String, Int>(monthCounts.size)
        val yearCounts = HashMap<Int, Int>()
        for ((month, box) in monthCounts) {
            monthByValue[month] = box.value
            monthByKey[monthKeyOf(month)] = box.value
            yearCounts[month / 100] = (yearCounts[month / 100] ?: 0) + box.value
        }
        val yearTotals = if (hasFirst) {
            val firstYear = firstDay / 10000
            (firstYear..maxOf(firstYear, todayYear)).map { InsightYearTotal(it, yearCounts[it] ?: 0) }
        } else emptyList()

        val currentMonthIndex = todayYear * 12 + (today / 100) % 100 - 1
        val monthly12 = (11 downTo 0).map { offset ->
            val index = currentMonthIndex - offset
            val value = (index / 12) * 100 + index % 12 + 1
            InsightRankedEntry(monthKeyOf(value), monthByValue[value] ?: 0)
        }

        val ordinals = dayCounts.keys.map { ordinalOfDayValue(it) }.sorted().toIntArray()
        val streak = longestRun(ordinals)?.let { it.second - it.first + 1 } ?: 0

        val timeline = cameraYears.keys.sorted().mapNotNull { year ->
            val counts = cameraYears[year] ?: return@mapNotNull null
            var bestName: String? = null
            var bestCount = -1
            var yearTotal = 0
            for ((name, box) in counts) {
                yearTotal += box.value
                if (box.value > bestCount || (box.value == bestCount && name < bestName!!)) {
                    bestName = name
                    bestCount = box.value
                }
            }
            bestName?.let { InsightCameraYear(year, it, bestCount, yearTotal) }
        }

        val onThisDayList = onThisDay.keys.sortedDescending().take(8).mapNotNull { year ->
            val acc = onThisDay[year] ?: return@mapNotNull null
            val cover = acc.cover ?: return@mapNotNull null
            InsightOnThisDay(todayYear - year, year, dayKeyOf(acc.day), acc.count, cover)
        }

        return InsightLifetimeStats(
            todayDayValue = today,
            firstDayValue = if (hasFirst) firstDay else null,
            firstDayKey = if (hasFirst) dayKeyOf(firstDay) else null,
            yearTotals = yearTotals,
            monthCounts = monthByKey,
            monthCountsByValue = monthByValue,
            total = total,
            photoTotal = photoTotal,
            videoSeconds = videoSeconds,
            activeDays = dayCounts.size,
            longestStreak = streak,
            distinctPlaces = labelSeen.size,
            coverage = InsightCoverage(photoBase, screenshotCount, scanned, withCamera, withLocation, withTags),
            cameraTimeline = timeline,
            onThisDay = onThisDayList,
            home = home(homeCells),
            cameraTop5 = ranked(cameraCounts, 5),
            locationTop5 = ranked(labelPhotoCounts, 5),
            keywordTop30 = ranked(keywordCounts, 30),
            monthly12 = monthly12
        )
    }

    // MARK: Period

    /**
     * Everything shown for one year / month. [lifetime] supplies the home coordinate, the
     * library's first day (elapsed days) and the other periods' totals (record check).
     */
    fun computePeriod(
        items: List<MediaItem>,
        snapshots: Map<Long, MediaInsightSnapshot>,
        likedIds: Set<Long>,
        period: InsightPeriod,
        lifetime: InsightLifetimeStats,
        nowMs: Long = System.currentTimeMillis()
    ): InsightPeriodStats {
        val today = todayDayValue(nowMs)
        val nowYear = today / 10000
        val nowMonth = (today / 100) % 100
        val nowDay = today % 100
        val isYear = period.isYear
        val inProgress = if (isYear) period.year == nowYear else period.year == nowYear && period.month == nowMonth
        val previous = period.previous()
        val periodStartMonth = period.startMonthIndex
        val zone = TimeZone.getDefault()
        val labels = PlaceLabelCache(placeAliases(snapshots))

        var previousTotal = 0
        var previousToDate = 0
        val previousDays = HashSet<Int>()
        var lastActiveBefore = Int.MIN_VALUE

        var photos = 0
        var videos = 0
        var screenshots = 0
        var liked = 0
        var videoSeconds = 0.0
        val hourCounts = IntArray(24)
        val weekdayCounts = IntArray(7)
        val formatCounts = IntArray(InsightFormat.entries.size)
        var vertical = 0
        var horizontal = 0
        var square = 0
        val cameraCounts = HashMap<String, IntBox>()
        var cameraTotal = 0
        val placeCounts = HashMap<String, IntBox>()
        val placeGeo = HashMap<String, GeoSum>()
        var placeTotal = 0
        var locatedCount = 0
        val keywordCounts = HashMap<String, IntBox>()
        val days = HashMap<Int, PeriodDay>(64)
        val cells = HashMap<Int, GeoSum>()
        val points = ArrayList<GeoPoint>()
        val shooting = ShootingAccumulator()

        for (item in items) {
            val day = mediaDayValue(item)
            if (day / 10000 < MIN_YEAR) continue
            if (previous.containsDay(day)) {
                previousTotal++
                previousDays.add(day)
                if (inProgress) {
                    val toDate = if (isYear) day % 10000 <= nowMonth * 100 + nowDay else day % 100 <= nowDay
                    if (toDate) previousToDate++
                }
            }
            val monthIndex = (day / 10000) * 12 + (day / 100) % 100 - 1
            if (monthIndex < periodStartMonth) {
                if (day > lastActiveBefore) lastActiveBefore = day
                continue
            }
            if (!period.containsDay(day)) continue

            val isPhoto = item.type == "photo"
            val isScreenshot = isPhoto && isScreenshotMedia(item)
            val isLiked = item.id in likedIds
            if (isPhoto) photos++ else {
                videos++
                videoSeconds += item.duration / 1000.0
            }
            if (isScreenshot) screenshots++
            if (isLiked) liked++

            InsightFormat.entries.forEachIndexed { index, format -> if (format.matches(item)) formatCounts[index]++ }
            if (isPhoto && !isScreenshot) {
                when (InsightOrientation.of(item.width, item.height)) {
                    InsightOrientation.Vertical -> vertical++
                    InsightOrientation.Horizontal -> horizontal++
                    InsightOrientation.Square -> square++
                }
            }

            val hour = localHour(item, zone)
            hourCounts[hour]++
            weekdayCounts[weekdayIndex(ordinalOfDayValue(day))]++

            val snapshot = snapshots[item.id]
            val camera = insightCameraLabel(snapshot)
            if (isPhoto && camera != null) {
                cameraCounts.getOrPut(camera) { IntBox() }.value++
                cameraTotal++
            }
            val label = labels.label(snapshot)
            if (label != null) {
                placeCounts.getOrPut(label) { IntBox() }.value++
                placeTotal++
            }
            val keywords = snapshot?.keywords.orEmpty()
            if (keywords.isNotEmpty()) forEachDistinct(keywords) { keywordCounts.getOrPut(it) { IntBox() }.value++ }

            val entry = days.getOrPut(day) { PeriodDay() }
            entry.count++
            if (!isScreenshot) {
                if (isPhoto) {
                    if (isLiked && entry.liked.let { it == null || newer(item, it) }) entry.liked = item
                    if (entry.photo.let { it == null || older(item, it) }) entry.photo = item
                } else if (entry.video.let { it == null || older(item, it) }) {
                    entry.video = item
                }
            }

            if (snapshot != null && hasUsableCoordinate(snapshot)) {
                val lat = snapshot.latitude!!
                val lon = snapshot.longitude!!
                locatedCount++
                entry.geo.add(lat, lon)
                cells.getOrPut(cellKey(lat, lon, 0.25)) { GeoSum() }.add(lat, lon)
                points.add(GeoPoint(lat, lon, label))
                if (label != null) placeGeo.getOrPut(label) { GeoSum() }.add(lat, lon)
            }
            if (isPhoto && !isScreenshot && snapshot?.shootingScanned == true) shooting.add(snapshot, hour)
        }

        // Days
        val dayValues = days.keys.sorted()
        val heatmap = LinkedHashMap<String, Int>(dayValues.size)
        var busiest = -1
        var busiestCount = 0
        for (day in dayValues) {
            val count = days[day]!!.count
            heatmap[dayKeyOf(day)] = count
            if (count >= busiestCount) {
                busiestCount = count
                busiest = day
            }
        }
        val busiestEntry = if (busiest >= 0) days[busiest] else null
        val cover = busiestEntry?.let { it.liked ?: it.photo ?: it.video }
        val ordinals = dayValues.map { ordinalOfDayValue(it) }.toIntArray()
        val streak = longestRun(ordinals)?.let { (start, end) ->
            InsightStreak(end - start + 1, dayKeyOf(dayValues[start]), dayKeyOf(dayValues[end]))
        }

        // Places & geography
        val placeTop = ranked(placeCounts, 5)
        val pins = placeTop.take(3).mapNotNull { entry ->
            val geo = placeGeo[entry.id] ?: return@mapNotNull null
            if (geo.count == 0) return@mapNotNull null
            InsightPlacePin(entry.id, geo.latitude / geo.count, geo.longitude / geo.count, entry.count)
        }
        val clusters = cells.entries
            .sortedWith(compareByDescending<Map.Entry<Int, GeoSum>> { it.value.count }.thenBy { it.key })
            .take(40)
            .map { (_, geo) -> InsightGeoCluster(geo.latitude / geo.count, geo.longitude / geo.count, geo.count) }

        val formats = InsightFormat.entries.mapIndexedNotNull { index, format ->
            formatCounts[index].takeIf { it > 0 }?.let { InsightFormatCount(format, it) }
        }.sortedByDescending { it.count }

        val total = photos + videos
        return InsightPeriodStats(
            period = period,
            isInProgress = inProgress,
            total = total,
            photos = photos,
            videos = videos,
            screenshots = screenshots,
            liked = liked,
            videoSeconds = videoSeconds,
            hourCounts = hourCounts.toList(),
            weekdayCounts = weekdayCounts.toList(),
            heatmap = heatmap,
            heatThresholds = heatThresholds(heatmap.values),
            chart = chart(period, days, today),
            activeDays = dayValues.size,
            elapsedDays = elapsedDays(period, lifetime.firstDayValue, today),
            longestStreak = streak,
            busiestDayKey = if (busiest >= 0) dayKeyOf(busiest) else null,
            busiestDayCount = busiestCount,
            cover = cover,
            coverIsLiked = cover != null && cover === busiestEntry?.liked,
            cameraTop = ranked(cameraCounts, 5),
            cameraDistinct = cameraCounts.size,
            cameraTotal = cameraTotal,
            placeTop = placeTop,
            placeDistinct = placeCounts.size,
            placeTotal = placeTotal,
            locatedCount = locatedCount,
            placePins = pins,
            geoClusters = clusters,
            farthest = farthest(points, lifetime.home),
            travelKm = travelKilometers(dayValues, days),
            keywordTop = ranked(keywordCounts, 24),
            formats = formats,
            orientation = InsightOrientationSplit(vertical, horizontal, square),
            shooting = shooting.finish(),
            lastActiveBeforeDayValue = lastActiveBefore.takeIf { it != Int.MIN_VALUE },
            comparison = InsightComparison(
                isYear = isYear,
                previousTotal = previousTotal,
                previousToDate = if (inProgress) previousToDate else null,
                isRecord = isRecord(period, total, lifetime),
                totalDelta = total - previousTotal,
                activeDayDelta = dayValues.size - previousDays.size
            )
        )
    }

    // MARK: Drill-down

    /**
     * The items behind one drill, newest first. Everything except Day / Month is limited to
     * [period]. Matching mirrors the counting rules above, so a tapped count equals the grid size.
     */
    fun drillItems(
        items: List<MediaItem>,
        snapshots: Map<Long, MediaInsightSnapshot>,
        likedIds: Set<Long>,
        period: InsightPeriod,
        drill: InsightDrill
    ): List<MediaItem> {
        val zone = TimeZone.getDefault()
        val labels = PlaceLabelCache(placeAliases(snapshots))
        val targetDay = (drill as? InsightDrill.Day)?.let { parseDayKey(it.dayKey) }
        val targetMonth = (drill as? InsightDrill.Month)?.let { parseMonthKey(it.monthKey) }
        if (drill is InsightDrill.Day && targetDay == null) return emptyList()
        if (drill is InsightDrill.Month && targetMonth == null) return emptyList()

        val matches = ArrayList<MediaItem>()
        for (item in items) {
            val day = mediaDayValue(item)
            val scoped = drill !is InsightDrill.Day && drill !is InsightDrill.Month
            if (scoped && !period.containsDay(day)) continue
            val isPhoto = item.type == "photo"
            val keep = when (drill) {
                is InsightDrill.Day -> day == targetDay
                is InsightDrill.Month -> day / 100 == targetMonth
                InsightDrill.Period -> true
                InsightDrill.Photos -> isPhoto
                InsightDrill.Videos -> !isPhoto
                InsightDrill.Screenshots -> isScreenshotMedia(item)
                InsightDrill.Located -> labels.label(snapshots[item.id]) != null
                InsightDrill.Liked -> item.id in likedIds
                is InsightDrill.Camera -> isPhoto && insightCameraLabel(snapshots[item.id]) == drill.raw
                is InsightDrill.Place -> labels.label(snapshots[item.id]) == drill.label
                is InsightDrill.Keyword -> snapshots[item.id]?.keywords?.contains(drill.word) == true
                is InsightDrill.Weekday -> weekdayIndex(ordinalOfDayValue(day)) + 1 == drill.weekday
                is InsightDrill.Daypart -> InsightDaypart.of(localHour(item, zone)) == drill.part
                is InsightDrill.Format -> drill.format.matches(item)
                is InsightDrill.Orientation ->
                    isPhoto && !isScreenshotMedia(item) && InsightOrientation.of(item.width, item.height) == drill.orientation
                is InsightDrill.Shooting -> {
                    val value = snapshots[item.id]?.let { drill.param.value(it) }
                    isPhoto && value != null && !isScreenshotMedia(item) && drill.param.bin(value) == drill.binIndex
                }
            }
            if (keep) matches.add(item)
        }
        matches.sortWith(compareByDescending<MediaItem> { it.dateAdded }.thenByDescending { it.id })
        return matches
    }

    // MARK: Periods

    /**
     * Smart default: this year if it has ≥ 3 active months so far; else this month if it has items;
     * else the latest earlier month with items; else (empty library) this year.
     * See [InsightDefaultPeriodOnce] for the once-per-process rule.
     */
    fun defaultPeriod(lifetime: InsightLifetimeStats, nowMs: Long = System.currentTimeMillis()): InsightPeriod {
        val today = todayDayValue(nowMs)
        val year = today / 10000
        val month = (today / 100) % 100
        val current = year * 100 + month
        val counts = lifetime.monthCountsByValue
        val activeThisYear = counts.count { (key, count) -> count > 0 && key / 100 == year && key <= current }
        if (activeThisYear >= 3) return InsightPeriod.ofYear(year, month)
        if ((counts[current] ?: 0) > 0) return InsightPeriod.ofMonth(year, month)
        val latest = counts.filter { (key, count) -> count > 0 && key < current }.keys.maxOrNull()
            ?: return InsightPeriod.ofYear(year, month)
        return InsightPeriod.ofMonth(latest / 100, latest % 100)
    }

    /** The period containing today. */
    fun currentPeriod(mode: InsightPeriodMode, nowMs: Long = System.currentTimeMillis()): InsightPeriod {
        val today = todayDayValue(nowMs)
        return InsightPeriod(mode, today / 10000, (today / 100) % 100)
    }

    /** First and last day value (yyyymmdd) of the period. */
    fun periodBounds(period: InsightPeriod): IntRange = period.firstDayValue..period.lastDayValue

    /** Can't go past the current year / month. */
    fun canStepNext(period: InsightPeriod, nowMs: Long = System.currentTimeMillis()): Boolean {
        val today = todayDayValue(nowMs)
        val nowYear = today / 10000
        val nowMonth = (today / 100) % 100
        return if (period.isYear) period.year < nowYear
        else period.year < nowYear || (period.year == nowYear && period.month < nowMonth)
    }

    /** Can't go before the library's first month (no limit while the first day is unknown). */
    fun canStepPrev(period: InsightPeriod, lifetime: InsightLifetimeStats): Boolean {
        val firstYear = lifetime.firstYear ?: return true
        val firstMonth = lifetime.firstMonth ?: return true
        return if (period.isYear) period.year > firstYear
        else period.year > firstYear || (period.year == firstYear && period.month > firstMonth)
    }

    /** Moves by [offset] periods, stopping at the allowed range. */
    fun step(
        period: InsightPeriod,
        offset: Int,
        lifetime: InsightLifetimeStats,
        nowMs: Long = System.currentTimeMillis()
    ): InsightPeriod {
        var result = period
        repeat(kotlin.math.abs(offset)) {
            result = if (offset > 0) {
                if (!canStepNext(result, nowMs)) return result
                result.next()
            } else {
                if (!canStepPrev(result, lifetime)) return result
                result.previous()
            }
        }
        return result
    }

    /** Pulls a period back inside [library first month, current month]. */
    fun clamp(period: InsightPeriod, lifetime: InsightLifetimeStats, nowMs: Long = System.currentTimeMillis()): InsightPeriod {
        val today = todayDayValue(nowMs)
        val nowIndex = (today / 10000) * 12 + (today / 100) % 100 - 1
        val first = lifetime.firstDayValue
        val firstIndex = first?.let { (it / 10000) * 12 + (it / 100) % 100 - 1 }
        return if (period.isYear) {
            var year = minOf(period.year, nowIndex / 12)
            if (firstIndex != null) year = maxOf(year, minOf(firstIndex / 12, nowIndex / 12))
            period.copy(year = year)
        } else {
            var index = period.year * 12 + period.month - 1
            index = index.coerceAtMost(nowIndex)
            if (firstIndex != null) index = maxOf(index, minOf(firstIndex, nowIndex))
            period.copy(year = index / 12, month = index % 12 + 1)
        }
    }

    /** The period of the last item before an empty period (quiet tile jump), keeping the mode. */
    fun quietJumpPeriod(stats: InsightPeriodStats): InsightPeriod? {
        val last = stats.lastActiveBeforeDayValue ?: return null
        return InsightPeriod(stats.period.mode, last / 10000, (last / 100) % 100)
    }

    // MARK: Heat

    /** Nearest-rank 20/45/70/90 percentiles of the non-zero counts; empty when there are none. */
    fun heatThresholds(counts: Collection<Int>): List<Int> {
        val sorted = counts.filter { it > 0 }.sorted()
        if (sorted.isEmpty()) return emptyList()
        val n = sorted.size
        return listOf(0.2, 0.45, 0.7, 0.9).map { p ->
            val rank = ceil(n * p).toInt()
            sorted[(rank - 1).coerceIn(0, n - 1)]
        }
    }

    /** Heat level 0...5: 0 = no items; 1 when thresholds are missing or count is 1. */
    fun heatLevel(count: Int, thresholds: List<Int>): Int {
        if (count <= 0) return 0
        if (thresholds.size != 4 || count <= 1) return 1
        return when {
            count <= thresholds[0] -> 1
            count <= thresholds[1] -> 2
            count <= thresholds[2] -> 3
            count <= thresholds[3] -> 4
            else -> 5
        }
    }

    // MARK: Pure helpers (internal for tests)

    internal const val MIN_YEAR = 1971

    /** Days since 1970-01-01 (Howard Hinnant days_from_civil). */
    internal fun dayOrdinal(year: Int, month: Int, day: Int): Int {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    internal fun ordinalOfDayValue(dayValue: Int): Int =
        dayOrdinal(dayValue / 10000, (dayValue / 100) % 100, dayValue % 100)

    /** Weekday index, 0 = Sunday. 1970-01-01 was a Thursday. */
    internal fun weekdayIndex(ordinal: Int): Int = ((ordinal % 7) + 7 + 4) % 7

    /** Longest run of consecutive ordinals (ascending, distinct); ties go to the latest run. Index range. */
    internal fun longestRun(ordinals: IntArray): Pair<Int, Int>? {
        if (ordinals.isEmpty()) return null
        var bestStart = 0
        var bestEnd = 0
        var runStart = 0
        for (i in 1 until ordinals.size) {
            if (ordinals[i] != ordinals[i - 1] + 1) runStart = i
            if (i - runStart >= bestEnd - bestStart) {
                bestStart = runStart
                bestEnd = i
            }
        }
        return bestStart to bestEnd
    }

    /** Great-circle distance in km (WGS-84 mean radius). */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rad = Math.PI / 180
        val dLat = (lat2 - lat1) * rad
        val dLon = (lon2 - lon1) * rad
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1 * rad) * cos(lat2 * rad) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * 6371.0088 * asin(min(1.0, sqrt(a)))
    }

    internal fun isUsableCoordinate(latitude: Double, longitude: Double): Boolean =
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            (kotlin.math.abs(latitude) > 0.0001 || kotlin.math.abs(longitude) > 0.0001)

    internal fun parseDayKey(key: String): Int? {
        if (key.length != 10 || key[4] != '-' || key[7] != '-') return null
        val y = key.substring(0, 4).toIntOrNull() ?: return null
        val m = key.substring(5, 7).toIntOrNull() ?: return null
        val d = key.substring(8, 10).toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        return y * 10000 + m * 100 + d
    }

    internal fun parseMonthKey(key: String): Int? {
        if (key.length != 7 || key[4] != '-') return null
        val y = key.substring(0, 4).toIntOrNull() ?: return null
        val m = key.substring(5, 7).toIntOrNull() ?: return null
        if (m !in 1..12) return null
        return y * 100 + m
    }

    /** Local hour 0...23 of the capture time in [zone]. */
    internal fun localHour(item: MediaItem, zone: TimeZone): Int {
        val ms = item.dateAdded * 1000L
        val local = ms + zone.getOffset(ms)
        return (Math.floorMod(local, 86_400_000L) / 3_600_000L).toInt().coerceIn(0, 23)
    }

    // MARK: Private

    private fun hasUsableCoordinate(snapshot: MediaInsightSnapshot): Boolean {
        val lat = snapshot.latitude ?: return false
        val lon = snapshot.longitude ?: return false
        return isUsableCoordinate(lat, lon)
    }

    private fun cellKey(latitude: Double, longitude: Double, size: Double): Int {
        val x = floor((longitude + 180) / size).toInt()
        val y = floor((latitude + 90) / size).toInt()
        return x * 8192 + y
    }

    private fun newer(a: MediaItem, b: MediaItem): Boolean =
        if (a.dateAdded != b.dateAdded) a.dateAdded > b.dateAdded else a.id < b.id

    private fun older(a: MediaItem, b: MediaItem): Boolean =
        if (a.dateAdded != b.dateAdded) a.dateAdded < b.dateAdded else a.id < b.id

    private inline fun forEachDistinct(words: List<String>, action: (String) -> Unit) {
        if (words.size <= 8) {
            // Tag lists are short: a quadratic check avoids a set per item.
            for (i in words.indices) {
                val word = words[i]
                var duplicate = false
                for (j in 0 until i) if (words[j] == word) { duplicate = true; break }
                if (!duplicate) action(word)
            }
            return
        }
        val seen = HashSet<String>(words.size * 2)
        for (word in words) if (seen.add(word)) action(word)
    }

    private fun ranked(counts: Map<String, IntBox>, limit: Int): List<InsightRankedEntry> =
        counts.entries.asSequence()
            .filter { it.value.value > 0 }
            .sortedWith(compareByDescending<Map.Entry<String, IntBox>> { it.value.value }.thenBy { it.key })
            .take(limit)
            .map { InsightRankedEntry(it.key, it.value.value) }
            .toList()

    private fun chart(period: InsightPeriod, days: Map<Int, PeriodDay>, today: Int): InsightChart {
        val nowYear = today / 10000
        val nowMonth = (today / 100) % 100
        val buckets = if (period.isYear) {
            val totals = IntArray(13)
            for ((day, entry) in days) totals[(day / 100) % 100] += entry.count
            (1..12).map { month ->
                InsightChartBucket(
                    key = monthKeyOf(period.year * 100 + month),
                    label = month.toString(),
                    count = totals[month],
                    isFuture = period.year > nowYear || (period.year == nowYear && month > nowMonth)
                )
            }
        } else {
            (1..insightDaysInMonth(period.year, period.month)).map { d ->
                val value = period.year * 10000 + period.month * 100 + d
                InsightChartBucket(
                    key = dayKeyOf(value),
                    label = if (d in DAY_LABELS) d.toString() else null,
                    count = days[value]?.count ?: 0,
                    isFuture = value > today
                )
            }
        }
        val futureFrom = buckets.indexOfFirst { it.isFuture }.let { if (it < 0) buckets.size else it }
        return InsightChart(buckets, futureFrom)
    }

    private val DAY_LABELS = setOf(1, 8, 15, 22, 29)

    private fun elapsedDays(period: InsightPeriod, firstDay: Int?, today: Int): Int {
        val from = maxOf(period.firstDayValue, firstDay ?: Int.MIN_VALUE)
        val to = minOf(period.lastDayValue, today)
        if (to < from) return 0
        return ordinalOfDayValue(to) - ordinalOfDayValue(from) + 1
    }

    private fun isRecord(period: InsightPeriod, total: Int, lifetime: InsightLifetimeStats): Boolean {
        if (total <= 0) return false
        var others = 0
        if (period.isYear) {
            val years = HashMap<Int, Int>()
            for ((month, count) in lifetime.monthCountsByValue) years[month / 100] = (years[month / 100] ?: 0) + count
            for ((year, count) in years) {
                if (year == period.year || count <= 0) continue
                if (count >= total) return false
                others++
            }
        } else {
            val self = period.year * 100 + period.month
            for ((month, count) in lifetime.monthCountsByValue) {
                if (month == self || count <= 0) continue
                if (count >= total) return false
                others++
            }
        }
        return others >= 1
    }

    /** Densest 0.5° cell scored with its 8 neighbours (1 / 0.75 / 0.5), mean of those 9 cells. */
    private fun home(cells: Map<Int, GeoSum>): InsightCoordinate? {
        var winner: Int? = null
        var bestScore = -1.0
        var bestCore = -1
        for ((key, sum) in cells) {
            var score = 0.0
            for (dx in -1..1) for (dy in -1..1) {
                val weight = if (dx == 0 && dy == 0) 1.0 else if (dx == 0 || dy == 0) 0.75 else 0.5
                score += (cells[key + dx * 8192 + dy]?.count ?: 0) * weight
            }
            val better = score > bestScore ||
                (score == bestScore && sum.count > bestCore) ||
                (score == bestScore && sum.count == bestCore && key < (winner ?: Int.MAX_VALUE))
            if (better) {
                winner = key
                bestScore = score
                bestCore = sum.count
            }
        }
        val center = winner ?: return null
        val total = GeoSum()
        for (dx in -1..1) for (dy in -1..1) cells[center + dx * 8192 + dy]?.let { total.add(it) }
        if (total.count == 0) return null
        return InsightCoordinate(total.latitude / total.count, total.longitude / total.count)
    }

    private fun farthest(points: List<GeoPoint>, home: InsightCoordinate?): InsightFarthestPlace? {
        if (points.isEmpty() || home == null) return null
        var best = -1
        var bestDistance = 0.0
        for (i in points.indices) {
            val p = points[i]
            val d = haversineKm(home.latitude, home.longitude, p.latitude, p.longitude)
            if (best < 0 || d > bestDistance + 1e-9 ||
                (kotlin.math.abs(d - bestDistance) <= 1e-9 && precedes(p, points[best]))
            ) {
                best = i
                bestDistance = d
            }
        }
        if (best < 0 || bestDistance < FARTHEST_MIN_KM) return null
        val point = points[best]
        var label = point.label
        if (label == null) {
            // Borrow the nearest labelled point within 50 km.
            var nearest = 50.0
            for (other in points) {
                val name = other.label ?: continue
                val d = haversineKm(point.latitude, point.longitude, other.latitude, other.longitude)
                if (d < nearest || (d == nearest && label != null && name < label)) {
                    nearest = d
                    label = name
                }
            }
        }
        return InsightFarthestPlace(bestDistance, label, point.latitude, point.longitude, home.latitude, home.longitude)
    }

    internal const val FARTHEST_MIN_KM = 100.0

    /** Equal distances: labelled first, then label, latitude, longitude. */
    private fun precedes(a: GeoPoint, b: GeoPoint): Boolean {
        if ((a.label == null) != (b.label == null)) return a.label != null
        if (a.label != null && b.label != null && a.label != b.label) return a.label < b.label
        if (a.latitude != b.latitude) return a.latitude < b.latitude
        return a.longitude < b.longitude
    }

    /** Sum of legs ≥ 1 km between consecutive days' centroids. */
    private fun travelKilometers(dayValues: List<Int>, days: Map<Int, PeriodDay>): Double {
        var total = 0.0
        var prevLat = 0.0
        var prevLon = 0.0
        var hasPrev = false
        for (day in dayValues) {
            val geo = days[day]?.geo ?: continue
            if (geo.count == 0) continue
            val lat = geo.latitude / geo.count
            val lon = geo.longitude / geo.count
            if (hasPrev) {
                val leg = haversineKm(prevLat, prevLon, lat, lon)
                if (leg >= 1) total += leg
            }
            prevLat = lat
            prevLon = lon
            hasPrev = true
        }
        return total
    }

    private class IntBox {
        var value = 0
    }

    private class GeoSum {
        var latitude = 0.0
        var longitude = 0.0
        var count = 0

        fun add(lat: Double, lon: Double) {
            latitude += lat
            longitude += lon
            count++
        }

        fun add(other: GeoSum) {
            latitude += other.latitude
            longitude += other.longitude
            count += other.count
        }
    }

    private class GeoPoint(val latitude: Double, val longitude: Double, val label: String?)

    private class PeriodDay {
        var count = 0
        var liked: MediaItem? = null
        var photo: MediaItem? = null
        var video: MediaItem? = null
        val geo = GeoSum()
    }

    private class OnThisDayAcc(val day: Int) {
        var count = 0
        var cover: MediaItem? = null
    }

    /** Raw location string -> normalised label; the regex runs once per distinct string. */
    private class PlaceLabelCache(private val aliases: Map<String, String> = emptyMap()) {
        private val cache = HashMap<String, String>()

        fun label(snapshot: MediaInsightSnapshot?): String? {
            val raw = snapshot?.location ?: return null
            val cached = cache[raw]
            if (cached != null) return cached.ifEmpty { null }
            val normalized = insightNormalizedPlaceLabel(insightGeocodedLocation(raw))
            val label = normalized?.let { aliases[it] ?: it }
            cache[raw] = label ?: ""
            return label
        }
    }

    private const val PLACE_MERGE_KM = 10.0
    private const val PLACE_MERGE_SCRIPT_KM = 50.0
    private var aliasSource: Map<Long, MediaInsightSnapshot>? = null
    private var aliasResult: Map<String, String> = emptyMap()

    /**
     * The system geocoder names the same city differently from one coordinate to the next
     * ("北京市" from the locality, "Bei Jing Shi" from the province). Labels of one country are folded
     * into the label with the most photos when their centres lie within 10 km, or within 50 km when
     * one name is written in CJK and the other in Latin letters (one place, two scripts), so that
     * neighbouring cities like Guangzhou and Foshan stay apart.
     */
    private fun placeHasCjk(label: String): Boolean {
        val name = label.substringBeforeLast(',')
        return name.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
    }

    internal fun placeAliases(snapshots: Map<Long, MediaInsightSnapshot>): Map<String, String> {
        synchronized(this) {
            if (aliasSource === snapshots) return aliasResult
        }
        val sums = HashMap<String, GeoSum>()
        val normalizedCache = HashMap<String, String>()
        for (snapshot in snapshots.values) {
            val raw = snapshot.location ?: continue
            val lat = snapshot.latitude ?: continue
            val lon = snapshot.longitude ?: continue
            val label = normalizedCache.getOrPut(raw) {
                insightNormalizedPlaceLabel(insightGeocodedLocation(raw)).orEmpty()
            }
            if (label.isEmpty()) continue
            val sum = sums.getOrPut(label) { GeoSum() }
            sum.latitude += lat
            sum.longitude += lon
            sum.count++
        }
        val labels = sums.entries
            .sortedWith(compareByDescending<Map.Entry<String, GeoSum>> { it.value.count }.thenBy { it.key })
        val aliases = HashMap<String, String>()
        val canonical = ArrayList<Triple<String, Double, Double>>()
        for ((label, sum) in labels) {
            val lat = sum.latitude / sum.count
            val lon = sum.longitude / sum.count
            val country = label.substringAfterLast(',', "").trim()
            val target = canonical.firstOrNull { (other, otherLat, otherLon) ->
                if (other.substringAfterLast(',', "").trim() != country) return@firstOrNull false
                val km = haversineKm(lat, lon, otherLat, otherLon)
                km <= PLACE_MERGE_KM ||
                    (km <= PLACE_MERGE_SCRIPT_KM && placeHasCjk(label) != placeHasCjk(other))
            }
            if (target != null) aliases[label] = target.first else canonical.add(Triple(label, lat, lon))
        }
        synchronized(this) {
            aliasSource = snapshots
            aliasResult = aliases
        }
        return aliases
    }

    /** Collects per-parameter values, sorts once at the end for medians. */
    private class ShootingAccumulator {
        private val values = Array(InsightShootingParam.entries.size) { DoubleList() }
        private val dayIso = DoubleList()
        private val nightIso = DoubleList()
        private val dayShutter = DoubleList()
        private val nightShutter = DoubleList()
        private var sampled = 0

        fun add(snapshot: MediaInsightSnapshot, hour: Int) {
            var any = false
            for (param in InsightShootingParam.entries) {
                val value = param.value(snapshot) ?: continue
                values[param.ordinal].add(value)
                any = true
            }
            if (!any) return
            sampled++
            val isNight = hour >= 20 || hour < 5
            val isDay = hour in 9 until 17
            InsightShootingParam.Iso.value(snapshot)?.let { if (isNight) nightIso.add(it) else if (isDay) dayIso.add(it) }
            InsightShootingParam.Shutter.value(snapshot)?.let {
                if (isNight) nightShutter.add(it) else if (isDay) dayShutter.add(it)
            }
        }

        fun finish(): InsightShootingStats {
            val distributions = HashMap<InsightShootingParam, InsightShootingDistribution>()
            var slow = 0
            for (param in InsightShootingParam.entries) {
                val sorted = values[param.ordinal].sorted()
                if (sorted.isEmpty()) continue
                val counts = IntArray(param.binCount)
                val modes = HashMap<Double, Int>()
                for (value in sorted) {
                    counts[param.bin(value)]++
                    when (param) {
                        InsightShootingParam.Focal -> Math.floor(value + 0.5).let { modes[it] = (modes[it] ?: 0) + 1 }
                        InsightShootingParam.Aperture -> (Math.floor(value * 10 + 0.5) / 10).let { modes[it] = (modes[it] ?: 0) + 1 }
                        else -> Unit
                    }
                }
                var modeKey: Double? = null
                var modeCount = 0
                for ((key, count) in modes) {
                    if (count > modeCount || (count == modeCount && key < modeKey!!)) {
                        modeKey = key
                        modeCount = count
                    }
                }
                distributions[param] = InsightShootingDistribution(
                    param = param,
                    counts = counts.toList(),
                    total = sorted.size,
                    median = median(sorted),
                    mode = modeKey,
                    modeCount = modeCount,
                    min = sorted.first(),
                    max = sorted.last()
                )
                if (param == InsightShootingParam.Shutter) {
                    slow = sorted.count { it >= 1.0 / 15 * (1 - 1e-9) }
                }
            }
            val minimum = InsightShootingStats.MINIMUM_PER_SIDE
            val isoPair = dayIso.size >= minimum && nightIso.size >= minimum
            val shutterPair = dayShutter.size >= minimum && nightShutter.size >= minimum
            return InsightShootingStats(
                distributions = distributions,
                sampled = sampled,
                dayIso = if (isoPair) median(dayIso.sorted()) else null,
                nightIso = if (isoPair) median(nightIso.sorted()) else null,
                dayShutter = if (shutterPair) median(dayShutter.sorted()) else null,
                nightShutter = if (shutterPair) median(nightShutter.sorted()) else null,
                slowShutterCount = slow
            )
        }

        private fun median(sorted: DoubleArray): Double? {
            if (sorted.isEmpty()) return null
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
        }
    }

    private class DoubleList {
        private var data = DoubleArray(16)
        var size = 0
            private set

        fun add(value: Double) {
            if (size == data.size) data = data.copyOf(size * 2)
            data[size++] = value
        }

        fun sorted(): DoubleArray = data.copyOf(size).also { it.sort() }
    }
}

/**
 * The smart default period runs once per process and never after the user changed the period
 * (iOS InsightStatsViewModel.smartDefaultResolved / userTouchedPeriod).
 */
object InsightDefaultPeriodOnce {
    @Volatile
    private var resolved = false

    /** The default period the first time a non-empty library is seen, else null. */
    @Synchronized
    fun take(lifetime: InsightLifetimeStats, nowMs: Long = System.currentTimeMillis()): InsightPeriod? {
        if (resolved || lifetime.isEmpty) return null
        resolved = true
        return InsightStatsEngine.defaultPeriod(lifetime, nowMs)
    }

    /** Call when the user changes the period by hand. */
    fun markUserTouched() {
        resolved = true
    }

    internal fun resetForTests() {
        resolved = false
    }
}
