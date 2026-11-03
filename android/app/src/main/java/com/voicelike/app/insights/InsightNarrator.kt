package com.voicelike.app

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

// Turns statistics into sentences, ported from iOS Views/Insights/InsightNarrator.swift and the
// shooting-tile sentences of InsightShootingTile.swift. Every sentence carries a significance
// threshold: small samples do not get a conclusion, small bases do not get a percentage.

/** All page sentences for one result. Empty strings mean "show nothing". */
internal data class InsightNarrative(
    /** "比去年同期多 18 个" / "这是你记录最多的一年" / "你的相册始于 10月3日". */
    val comparison: String = "",
    /** "+18%" tail after the comparison, or empty. */
    val comparisonDetail: String = "",
    val trend: InsightTrend = InsightTrend.None,
    /** "你最常在星期六拍照，尤其是傍晚". */
    val rhythmLine: String = "",
    /** "31 天里有 7 天按下了快门". */
    val rhythmSubline: String = "",
    /** "你在 4 个地方留下了足迹，最常在北京". */
    val placesLine: String = "",
    /** Farthest and travel lines, each only past its threshold. */
    val placesSublines: List<String> = emptyList(),
    /** "Pixel 8 拍下了 72% 的照片". */
    val lensLine: String = "",
    /** "你最常拍天空、夜空和户外". */
    val keywordsLine: String = "",
    /** "视频共 2 小时 14 分", empty under a minute. */
    val videoLine: String = "",
    /** "始于 2014年3月2日". */
    val lifetimeSince: String = "",
    /** Empty period: "这段时间没有新照片，上一次记录在 9月27日。". */
    val quietBody: String = "",
    /** Empty period jump button: "去看 2026年9月", empty when there is nowhere to go. */
    val quietJumpTitle: String = "",
    val quietJumpPeriod: InsightPeriod? = null
)

internal data class InsightComparisonLine(val line: String, val detail: String, val trend: InsightTrend)

internal object InsightNarrator {

    // MARK: Thresholds

    /** Fewer items than this: no rhythm conclusion, only the busiest day. */
    const val RHYTHM_MIN_ITEMS = 24
    const val RHYTHM_MIN_ACTIVE_DAYS = 4
    /** The busiest day holding this share means the distribution is just that day's shadow. */
    const val SINGLE_DAY_DOMINANCE = 0.5
    /** Weekday peak share (even spread is about 14%). */
    const val WEEKDAY_SHARE = 0.20
    const val DAYPART_SHARE = 0.35
    const val BUSIEST_DAY_MIN_COUNT = 2
    /** Previous period below this: absolute numbers only, never a percentage. */
    const val SMALL_BASE = 10
    /** |delta| / base under this reads "about the same". */
    const val SAME_BAND = 0.03
    /** Percent tails above this carry no information. */
    const val MAX_PERCENT = 999.0
    const val FARTHEST_KM = 100.0
    const val TRAVEL_KM = 50.0
    const val DOMINANT_LENS_SHARE = 0.6
    const val VIDEO_SECONDS = 60.0
    const val KEYWORD_COUNT = 3

    // MARK: Entry

    /**
     * Every sentence of the page for one period result. [cameraName] prettifies raw camera labels
     * (pass DeviceModelNameResolver.resolveCameraLabel(context, it) on Android).
     */
    fun narrate(
        stats: InsightPeriodStats,
        lifetime: InsightLifetimeStats,
        isScanning: Boolean,
        s: InsightStrings,
        cameraName: (String) -> String = ::insightCameraDisplayName
    ): InsightNarrative {
        val since = lifetimeSince(lifetime, s)
        if (stats.isEmpty) {
            return InsightNarrative(
                lifetimeSince = since,
                quietBody = quietBody(stats, s),
                quietJumpTitle = quietJumpTitle(stats, s),
                quietJumpPeriod = InsightStatsEngine.quietJumpPeriod(stats)
            )
        }
        val compared = comparison(stats, lifetime, s)
        return InsightNarrative(
            comparison = compared.line,
            comparisonDetail = compared.detail,
            trend = compared.trend,
            rhythmLine = rhythmLine(stats, s),
            rhythmSubline = activeDaysLine(stats, s),
            placesLine = placesLine(stats, isScanning, s),
            placesSublines = placesSublines(stats, s),
            lensLine = lensLine(stats, isScanning, s, cameraName),
            keywordsLine = keywordsLine(stats, s),
            videoLine = videoLine(stats, s),
            lifetimeSince = since
        )
    }

    // MARK: Comparison

    fun comparison(stats: InsightPeriodStats, lifetime: InsightLifetimeStats, s: InsightStrings): InsightComparisonLine {
        val comparison = stats.comparison
        val isYear = stats.period.isYear
        if (comparison.isRecord) {
            return InsightComparisonLine(if (isYear) s.insRecordYear else s.insRecordMonth, "", InsightTrend.Up)
        }
        // In-progress periods compare with the same point of the previous one.
        val toDate = if (stats.isInProgress) comparison.previousToDate else null
        val base = maxOf(0, toDate ?: comparison.previousTotal)
        val reference = when {
            isYear && toDate != null -> s.insRefLastYearToDate
            isYear -> s.insRefLastYear
            toDate != null -> s.insRefLastMonthToDate
            else -> s.insRefLastMonth
        }
        val current = stats.total
        val delta = current - base

        if (base == 0) {
            val first = lifetime.firstDayValue
            if (first != null && stats.period.containsDay(first)) {
                return InsightComparisonLine(s.format(s.insLibraryBeganFmt, s.monthDay(first)), "", InsightTrend.None)
            }
            // Gap in the library: "only 0 last year" reads oddly, say how many more.
            return InsightComparisonLine(s.format(s.insCompareMoreFmt, reference, s.number(current)), "", InsightTrend.Up)
        }
        if (base < SMALL_BASE) {
            return when {
                delta > 0 -> InsightComparisonLine(s.format(s.insCompareSmallBaseFmt, reference, s.number(base)), "", InsightTrend.Up)
                delta == 0 -> InsightComparisonLine(s.format(s.insCompareSameFmt, reference), "", InsightTrend.Flat)
                else -> InsightComparisonLine(s.format(s.insCompareFewerFmt, reference, s.number(-delta)), "", InsightTrend.Down)
            }
        }
        val ratio = delta.toDouble() / base
        if (abs(ratio) < SAME_BAND) {
            return InsightComparisonLine(s.format(s.insCompareSameFmt, reference), "", InsightTrend.Flat)
        }
        val line = s.format(if (delta > 0) s.insCompareMoreFmt else s.insCompareFewerFmt, reference, s.number(abs(delta)))
        val detail = if (abs(ratio) * 100 <= MAX_PERCENT) s.signedPercent(ratio) else ""
        return InsightComparisonLine(line, detail, if (delta > 0) InsightTrend.Up else InsightTrend.Down)
    }

    // MARK: Rhythm

    fun rhythmLine(stats: InsightPeriodStats, s: InsightStrings): String {
        val busiestShare = stats.busiestDayCount.toDouble() / maxOf(1, stats.total)
        if (stats.total < RHYTHM_MIN_ITEMS || stats.activeDays < RHYTHM_MIN_ACTIVE_DAYS || busiestShare >= SINGLE_DAY_DOMINANCE) {
            return sparseRhythmLine(stats, s)
        }
        val weekday = uniquePeak(stats.weekdayCounts)
            ?.takeIf { it.second >= WEEKDAY_SHARE }
            ?.let { s.weekdayNames().getOrNull(it.first) }
        val daypart = uniquePeak(stats.daypartCounts())
            ?.takeIf { it.second >= DAYPART_SHARE }
            ?.let { InsightDaypart.entries[it.first] }
        return when {
            weekday != null && daypart != null -> s.format(s.insRhythmPeakFmt, weekday, s.daypartPhrase(daypart))
            weekday != null -> s.format(s.insRhythmPeakWeekdayOnlyFmt, weekday)
            daypart != null -> s.format(s.insRhythmPeakDaypartOnlyFmt, s.daypartPhrase(daypart))
            else -> s.insRhythmEven
        }
    }

    /** Too few items or one day dominates: state the busiest day, or ask for more photos when it is tied. */
    private fun sparseRhythmLine(stats: InsightPeriodStats, s: InsightStrings): String {
        val count = stats.busiestDayCount
        val dayKey = stats.busiestDayKey
        if (dayKey == null || count < BUSIEST_DAY_MIN_COUNT) return s.insRhythmSparse
        if (stats.heatmap.values.count { it == count } > 1) return s.insRhythmSparse
        return s.format(s.insBusiestDayFmt, s.monthDay(dayKey), s.number(count))
    }

    /** Unique maximum and its share; null when tied for first or all zero. */
    internal fun uniquePeak(counts: List<Int>): Pair<Int, Double>? {
        var total = 0
        var best = -1
        var bestValue = 0
        var tied = false
        counts.forEachIndexed { index, value ->
            total += value
            if (value > bestValue) {
                bestValue = value
                best = index
                tied = false
            } else if (value == bestValue && value > 0) {
                tied = true
            }
        }
        if (total <= 0 || best < 0 || tied) return null
        return best to bestValue.toDouble() / total
    }

    /** "31 天里有 7 天按下了快门"; empty when no day of the period has passed. */
    fun activeDaysLine(stats: InsightPeriodStats, s: InsightStrings): String {
        if (stats.elapsedDays <= 0) return ""
        val active = stats.activeDays
        return s.format(s.insActiveDaysFmt, s.number(maxOf(stats.elapsedDays, active)), s.number(active))
    }

    // MARK: Places

    fun placesLine(stats: InsightPeriodStats, isScanning: Boolean, s: InsightStrings): String {
        val top = stats.placeTop.firstOrNull()
        if (stats.placeDistinct <= 0 || top == null) {
            if (stats.locatedCount > 0) return s.format(s.insPlacesCoordsOnlyFmt, s.number(stats.locatedCount))
            return if (isScanning) s.insPlacesScanning else s.insPlacesNone
        }
        if (stats.placeDistinct == 1) return s.format(s.insPlacesOneFmt, sentencePlace(top.id, s))
        return s.format(s.insPlacesManyFmt, s.number(stats.placeDistinct), sentencePlace(top.id, s))
    }

    fun farthestLine(stats: InsightPeriodStats, s: InsightStrings): String? {
        val farthest = stats.farthest ?: return null
        if (farthest.kilometers < FARTHEST_KM) return null
        val distance = s.distance(farthest.kilometers)
        val label = farthest.label?.let { sentencePlace(it, s) }
        return if (!label.isNullOrEmpty()) s.format(s.insFarthestFmt, distance, label)
        else s.format(s.insFarthestNoLabelFmt, distance)
    }

    fun travelLine(stats: InsightPeriodStats, s: InsightStrings): String? =
        if (stats.travelKm >= TRAVEL_KM) s.format(s.insTravelFmt, s.distance(stats.travelKm)) else null

    fun placesSublines(stats: InsightPeriodStats, s: InsightStrings): List<String> =
        listOfNotNull(farthestLine(stats, s), travelLine(stats, s))

    /** Sentences use the city only, like the map labels; lists keep the full label. */
    fun sentencePlace(label: String, s: InsightStrings): String = insightCityName(label, s.locale).ifEmpty { label }

    // MARK: Lens & subjects

    fun lensLine(
        stats: InsightPeriodStats,
        isScanning: Boolean,
        s: InsightStrings,
        cameraName: (String) -> String = ::insightCameraDisplayName
    ): String {
        val top = stats.cameraTop.firstOrNull()
        if (stats.cameraTotal <= 0 || top == null) return if (isScanning) s.insReadingDetails else s.insLensNone
        val name = cameraName(top.id)
        if (stats.cameraDistinct <= 1) return s.format(s.insLensOneFmt, name)
        val share = top.count.toDouble() / stats.cameraTotal
        if (share >= DOMINANT_LENS_SHARE) {
            // More than one camera: never round up to "100%".
            return s.format(s.insLensDominantFmt, name, s.percent(min(share, 0.99)))
        }
        return s.format(s.insLensManyFmt, s.number(stats.cameraDistinct), name)
    }

    fun keywordsLine(stats: InsightPeriodStats, s: InsightStrings): String {
        val names = stats.keywordTop.take(KEYWORD_COUNT).map { insightKeywordDisplayName(it.id) }.filter { it.isNotEmpty() }
        if (names.isEmpty()) return ""
        return s.format(s.insKeywordsFmt, s.listJoin(names))
    }

    fun videoLine(stats: InsightPeriodStats, s: InsightStrings): String =
        if (stats.videoSeconds >= VIDEO_SECONDS) s.format(s.insVideoTotalFmt, s.duration(stats.videoSeconds)) else ""

    // MARK: Shooting

    /** Mode for focal / aperture ("26 mm 用得最多，占 61%"), median for ISO / shutter. */
    fun shootingHeadline(distribution: InsightShootingDistribution, s: InsightStrings): String {
        val param = distribution.param
        return when (param) {
            InsightShootingParam.Focal, InsightShootingParam.Aperture -> distribution.mode?.let {
                s.format(s.insShootModeFmt, param.format(it), s.percent(distribution.share(distribution.modeCount)))
            }
            InsightShootingParam.Iso -> distribution.median?.let {
                s.format(s.insShootISOMedianFmt, it.roundToLong().toString())
            }
            InsightShootingParam.Shutter -> distribution.median?.let {
                s.format(s.insShootShutterMedianFmt, param.format(it))
            }
        } ?: ""
    }

    /** The data-mining line under the headline, or null when the data does not support one. */
    fun shootingDetail(distribution: InsightShootingDistribution, shooting: InsightShootingStats, s: InsightStrings): String? {
        val counts = distribution.counts
        val param = distribution.param
        return when (param) {
            InsightShootingParam.Focal -> {
                // Ultra wide = first bin (≤ 20 mm), tele = above 40 mm (2x and longer on phones).
                val ultraWide = counts.firstOrNull() ?: 0
                val tele = counts.drop(3).sum()
                if (ultraWide + tele <= 0) null
                else s.format(s.insShootLensMixFmt, s.percent(distribution.share(ultraWide)), s.percent(distribution.share(tele)))
            }
            InsightShootingParam.Aperture -> {
                val low = distribution.min
                val high = distribution.max
                if (low == null || high == null || param.format(low) == param.format(high)) null
                else s.format(s.insShootApertureRangeFmt, param.format(low), param.format(high))
            }
            InsightShootingParam.Iso -> {
                val night = shooting.nightIso
                val day = shooting.dayIso
                if (night != null && day != null && day > 0 && night >= day * 1.5) {
                    s.format(s.insShootNightISOFmt, night.roundToLong().toString(), s.ratio(night / day))
                } else {
                    val high = counts.lastOrNull() ?: 0
                    if (high > 0) s.format(s.insShootHighISOFmt, s.percent(distribution.share(high))) else null
                }
            }
            InsightShootingParam.Shutter -> {
                val night = shooting.nightShutter
                val day = shooting.dayShutter
                if (night != null && day != null && night >= day * 1.5) {
                    s.format(s.insShootNightShutterFmt, param.format(night), param.format(day))
                } else if (shooting.slowShutterCount > 0) {
                    s.format(s.insShootSlowShutterFmt, s.percent(distribution.share(shooting.slowShutterCount)))
                } else null
            }
        }
    }

    /** "基于 N 张有拍摄参数的照片". */
    fun shootingBasis(distribution: InsightShootingDistribution, s: InsightStrings): String =
        s.format(s.insShootBasisFmt, s.number(distribution.total))

    /** Empty tile text: pending while the scanner runs, else empty. */
    fun shootingEmpty(isScanning: Boolean, s: InsightStrings): String = if (isScanning) s.insShootPending else s.insShootEmpty

    // MARK: Lifetime

    /** "始于 2014年3月2日", empty for an empty library. */
    fun lifetimeSince(lifetime: InsightLifetimeStats, s: InsightStrings): String =
        lifetime.firstDayValue?.let { s.format(s.insSinceFmt, s.fullDate(it)) } ?: ""

    /** "往年的今天，你拍下了 N 张照片", empty without matches. */
    fun onThisDayLine(lifetime: InsightLifetimeStats, s: InsightStrings): String {
        val total = lifetime.onThisDay.sumOf { it.count }
        return if (total > 0) s.format(s.insOnThisDayLineFmt, s.number(total)) else ""
    }

    /** "占当年 72%" for a camera timeline row. */
    fun cameraYearShare(entry: InsightCameraYear, s: InsightStrings): String =
        s.format(s.insCameraTimelineShareFmt, s.percent(entry.share))

    // MARK: Quiet period

    fun quietBody(stats: InsightPeriodStats, s: InsightStrings): String {
        val last = stats.lastActiveBeforeDayValue ?: return s.insQuietBodyNone
        // Across years "12月3日" would read as this year: include the year.
        val text = if (last / 10000 == stats.period.year) s.monthDay(last) else s.fullDate(last)
        return s.format(s.insQuietBodyFmt, text)
    }

    fun quietJumpTitle(stats: InsightPeriodStats, s: InsightStrings): String {
        val target = InsightStatsEngine.quietJumpPeriod(stats) ?: return ""
        return s.format(s.insJumpToFmt, s.periodTitle(target))
    }

    // MARK: Coverage footer

    /** "基于 N 张照片，另有 M 张截图未计入". */
    fun coverageBasis(lifetime: InsightLifetimeStats, s: InsightStrings): String =
        s.format(s.insCoverageBasisFmt, s.number(lifetime.coverage.photoBase), s.number(lifetime.coverage.screenshotCount))

    /** "还有 N 张尚未读取拍摄信息", or null when everything has been read. */
    fun coveragePending(lifetime: InsightLifetimeStats, s: InsightStrings): String? {
        val pending = lifetime.coverage.pending
        return if (pending > 0) s.format(s.insCoveragePendingFmt, s.number(pending)) else null
    }

    // MARK: AI prompt

    /**
     * The "let AI read your album" prompt (iOS InsightsPageView.copyAiPrompt, aligned with the old
     * Android buildAiRoastPrompt). Chinese prompts for zh-Hans / zh-Hant; English with a
     * "write in X" hint for every other language. [cameraName] maps raw camera labels for the list.
     */
    fun aiPrompt(lifetime: InsightLifetimeStats, s: InsightStrings, cameraName: (String) -> String = { it }): String {
        val coverage = lifetime.coverage
        fun pct(part: Int) = String.format(Locale.US, "%.1f%%", coverage.ratio(part) * 100)
        val coverageRatio = listOf(
            "${s.insCoverageCamera} ${pct(coverage.withCamera)}",
            "${s.insCoverageLocation} ${pct(coverage.withLocation)}",
            "${s.insCoverageTags} ${pct(coverage.withTags)}"
        ).joinToString("\n")
        val t = promptText(s.language)

        val prompt = StringBuilder()
        prompt.append(t.intro1).append("\n\n").append(t.intro2).append("\n\n").append(t.intro3).append("\n\n")
        t.languageHint?.let { prompt.append(it).append("\n\n") }
        prompt.append(t.sourceLead).append('\n')
        prompt.append(t.sectionCoverage).append('\n').append(coverageRatio).append("\n\n")
        prompt.append(t.sectionCamera).append('\n')
        if (lifetime.cameraTop5.isEmpty()) prompt.append(t.noData).append('\n')
        else lifetime.cameraTop5.forEach { prompt.append("- ").append(cameraName(it.id)).append(": ").append(it.count).append('\n') }
        prompt.append('\n').append(t.sectionLocation).append('\n')
        if (lifetime.locationTop5.isEmpty()) prompt.append(t.noData).append('\n')
        else lifetime.locationTop5.forEach { prompt.append("- ").append(it.id).append(": ").append(it.count).append('\n') }
        prompt.append('\n').append(t.sectionTrend).append('\n')
        lifetime.monthly12.forEach { prompt.append("- ").append(it.id).append(": ").append(it.count).append('\n') }
        prompt.append('\n').append(t.sectionTags).append('\n')
        if (lifetime.keywordTop30.isEmpty()) prompt.append(t.noData).append('\n')
        else lifetime.keywordTop30.forEach { prompt.append("- ").append(it.id).append(": ").append(it.count).append('\n') }
        return prompt.toString()
    }

    private class PromptText(
        val intro1: String,
        val intro2: String,
        val intro3: String,
        val sourceLead: String,
        val sectionCoverage: String,
        val sectionCamera: String,
        val sectionLocation: String,
        val sectionTrend: String,
        val sectionTags: String,
        val noData: String,
        val languageHint: String?
    )

    private fun promptText(language: AppLanguage): PromptText = when (language) {
        AppLanguage.SimplifiedChinese -> PromptText(
            intro1 = "我在 Photoo 里整理出了这份相册洞察数据。你可以把我当成一个真实用户，基于这些数据认真读一读我的拍照习惯。",
            intro2 = "我希望你写一段有观点、有幽默感但不刻薄的点评：既要说出我拍照风格的特点和可能的偏好，也请从审美、记录意识、生活方式这些角度真诚地夸夸我。",
            intro3 = "请尽量结合具体数据来分析，不要空泛，不要模板化。",
            sourceLead = "我的原始数据如下：",
            sectionCoverage = "数据覆盖率:",
            sectionCamera = "常用相机 Top5:",
            sectionLocation = "常去地点 Top5:",
            sectionTrend = "近12月照片与视频趋势:",
            sectionTags = "AI标签 Top30:",
            noData = "暂无数据",
            languageHint = null
        )
        AppLanguage.TraditionalChinese -> PromptText(
            intro1 = "我在 Photoo 裡整理出了這份相簿洞察資料。你可以把我當成一位真實使用者，基於這些資料認真讀一讀我的拍照習慣。",
            intro2 = "我希望你寫一段有觀點、有幽默感但不刻薄的點評：既要說出我拍照風格的特點和可能偏好，也請從審美、記錄意識、生活方式等角度真誠地稱讚我。",
            intro3 = "請盡量結合具體資料分析，不要空泛、不要模板化。",
            sourceLead = "我的原始資料如下：",
            sectionCoverage = "資料覆蓋率:",
            sectionCamera = "常用相機 Top5:",
            sectionLocation = "常去地點 Top5:",
            sectionTrend = "近12月照片與影片趨勢:",
            sectionTags = "AI標籤 Top30:",
            noData = "暫無資料",
            languageHint = null
        )
        else -> PromptText(
            intro1 = "I organized this album insight data in Photoo. Please treat me like a real user and carefully read my shooting habits based on the data.",
            intro2 = "I want a critique with clear opinions and humor but not mean. Please describe my shooting style and preferences, and also praise me sincerely from aesthetics, recording awareness, and lifestyle.",
            intro3 = "Please analyze with concrete data as much as possible, and avoid generic templates.",
            sourceLead = "My raw data is below:",
            sectionCoverage = "Data coverage:",
            sectionCamera = "Top 5 cameras:",
            sectionLocation = "Top 5 locations:",
            sectionTrend = "Photos and videos (last 12 months):",
            sectionTags = "Top 30 AI tags:",
            noData = "No data",
            languageHint = "Please write the final critique in ${promptLanguageName(language)}."
        )
    }

    private fun promptLanguageName(language: AppLanguage): String = when (language) {
        AppLanguage.Japanese -> "Japanese"
        AppLanguage.Korean -> "Korean"
        AppLanguage.French -> "French"
        AppLanguage.German -> "German"
        AppLanguage.Italian -> "Italian"
        AppLanguage.Spanish -> "Spanish"
        AppLanguage.Portuguese -> "Portuguese"
        AppLanguage.Russian -> "Russian"
        AppLanguage.Thai -> "Thai"
        AppLanguage.Vietnamese -> "Vietnamese"
        AppLanguage.Indonesian -> "Indonesian"
        AppLanguage.Arabic -> "Arabic"
        AppLanguage.Ukrainian -> "Ukrainian"
        AppLanguage.Polish -> "Polish"
        AppLanguage.Czech -> "Czech"
        AppLanguage.Dutch -> "Dutch"
        AppLanguage.Swedish -> "Swedish"
        AppLanguage.Danish -> "Danish"
        AppLanguage.Norwegian -> "Norwegian (Bokmål)"
        AppLanguage.Finnish -> "Finnish"
        AppLanguage.Greek -> "Greek"
        AppLanguage.Romanian -> "Romanian"
        AppLanguage.Hungarian -> "Hungarian"
        AppLanguage.Turkish -> "Turkish"
        AppLanguage.Malay -> "Malay"
        AppLanguage.Hebrew -> "Hebrew"
        else -> "English"
    }
}

// MARK: City names (iOS InsightPlaceName.city)

private val countryNameCache = ConcurrentHashMap<String, List<String>>()

/**
 * The city part of a place label, used in sentences and map/globe labels:
 * "Cupertino, CA" -> "Cupertino", "东京，日本" -> "东京", "中国北京市" -> "北京市".
 */
internal fun insightCityName(label: String, locale: Locale): String {
    val first = label.split(',', '，').map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: label.trim()
    if (first.isEmpty() || !isCjkCodePoint(first.codePointAt(0))) return first
    for (country in countryNames(locale)) {
        if (first.startsWith(country)) {
            val rest = first.substring(country.length).trim()
            if (rest.length >= 2) return rest
        }
    }
    return first
}

private fun isCjkCodePoint(cp: Int): Boolean =
    Character.isIdeographic(cp) || cp in 0x3040..0x30FF || cp in 0xAC00..0xD7AF || cp in 0x1100..0x11FF

/** Country names in [locale] plus the short forms addresses use, longest first. */
private fun countryNames(locale: Locale): List<String> = countryNameCache.getOrPut(locale.toLanguageTag()) {
    val localized = Locale.getISOCountries().map { Locale("", it).getDisplayCountry(locale) }
    (localized + listOf("中国", "中國", "日本", "대한민국", "한국"))
        .filter { it.length >= 2 }
        .distinct()
        .sortedByDescending { it.length }
}
