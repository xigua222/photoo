package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightNarratorTest {
    private val en = insightStrings(AppLanguage.English)
    private val zh = insightStrings(AppLanguage.SimplifiedChinese)

    private val yearPeriod = InsightPeriod.ofYear(2026, 10)
    private val lifetime = InsightLifetimeStats(firstDayValue = 20140302, firstDayKey = "2014-03-02", total = 1000)

    private fun base(
        total: Int = 111,
        previousTotal: Int = 0,
        previousToDate: Int? = null,
        inProgress: Boolean = true,
        record: Boolean = false,
        period: InsightPeriod = yearPeriod
    ) = InsightPeriodStats(
        period = period,
        isInProgress = inProgress,
        total = total,
        photos = total,
        activeDays = 9,
        busiestDayKey = "2026-10-03",
        busiestDayCount = 40,
        heatmap = mapOf("2026-10-03" to 40, "2026-10-04" to 20, "2026-10-05" to 51),
        weekdayCounts = listOf(10, 5, 5, 5, 5, 6, 75),
        hourCounts = (0 until 24).map { if (it in 17 until 20) 25 else 2 },
        comparison = InsightComparison(
            isYear = period.isYear,
            previousTotal = previousTotal,
            previousToDate = previousToDate,
            isRecord = record
        )
    )

    @Test
    fun comparison_thresholds() {
        val record = InsightNarrator.comparison(base(record = true, previousTotal = 50), lifetime, en)
        assertEquals(InsightComparisonLine("Your most-recorded year yet", "", InsightTrend.Up), record)

        val smallUp = InsightNarrator.comparison(base(previousTotal = 100, previousToDate = 7), lifetime, en)
        assertEquals("Only 7 this time last year", smallUp.line)
        assertEquals("", smallUp.detail)

        val smallDown = InsightNarrator.comparison(base(total = 3, previousTotal = 8, inProgress = false), lifetime, en)
        assertEquals(InsightComparisonLine("5 fewer than last year", "", InsightTrend.Down), smallDown)

        val same = InsightNarrator.comparison(base(previousTotal = 200, previousToDate = 110), lifetime, en)
        assertEquals(InsightComparisonLine("About the same as this time last year", "", InsightTrend.Flat), same)

        val more = InsightNarrator.comparison(base(previousTotal = 200, previousToDate = 94), lifetime, en)
        assertEquals(InsightComparisonLine("17 more than this time last year", "+18%", InsightTrend.Up), more)

        val huge = InsightNarrator.comparison(base(previousTotal = 10, inProgress = false), lifetime, en)
        assertEquals("101 more than last year", huge.line)
        assertEquals("", huge.detail)

        val began = InsightNarrator.comparison(base(), lifetime.copy(firstDayValue = 20261003), en)
        assertEquals(InsightComparisonLine("Your library begins on Oct 3", "", InsightTrend.None), began)

        val gap = InsightNarrator.comparison(base(), lifetime, en)
        assertEquals("111 more than last year", gap.line)

        val zhMore = InsightNarrator.comparison(base(previousTotal = 200, previousToDate = 94), lifetime, zh)
        assertEquals("比去年同期多 17 个", zhMore.line)
    }

    @Test
    fun rhythm_thresholds() {
        assertEquals("You shoot most on Saturday, especially in the evening", InsightNarrator.rhythmLine(base(), en))
        assertEquals("你最常在星期六拍照，尤其是傍晚", InsightNarrator.rhythmLine(base(), zh))

        val even = base().copy(weekdayCounts = listOf(16, 16, 16, 16, 16, 16, 15), hourCounts = List(24) { 4 })
        assertEquals("Your shots are spread evenly across the week", InsightNarrator.rhythmLine(even, en))

        val weekdayOnly = base().copy(hourCounts = List(24) { 4 })
        assertEquals("You shoot most on Saturday", InsightNarrator.rhythmLine(weekdayOnly, en))

        val dominated = base().copy(busiestDayKey = "2026-10-14", busiestDayCount = 101,
            heatmap = mapOf("2026-10-14" to 101, "2026-10-03" to 6))
        assertEquals("Busiest day: Oct 14, with 101 shots", InsightNarrator.rhythmLine(dominated, en))

        val sparse = base(total = 3).copy(activeDays = 2, busiestDayCount = 2, heatmap = mapOf("2026-10-03" to 2, "2026-10-04" to 1))
        assertEquals("Busiest day: Oct 3, with 2 shots", InsightNarrator.rhythmLine(sparse, en))

        val tied = sparse.copy(total = 4, heatmap = mapOf("2026-10-03" to 2, "2026-10-04" to 2))
        assertEquals(en.insRhythmSparse, InsightNarrator.rhythmLine(tied, en))

        val single = sparse.copy(busiestDayCount = 1)
        assertEquals(en.insRhythmSparse, InsightNarrator.rhythmLine(single, en))
    }

    @Test
    fun activeDaysLine() {
        val stats = base().copy(elapsedDays = 31, activeDays = 7)
        assertEquals("31 天里有 7 天按下了快门", InsightNarrator.activeDaysLine(stats, zh))
        assertEquals("", InsightNarrator.activeDaysLine(stats.copy(elapsedDays = 0), zh))
        assertEquals("You took photos on 9 of 9 days", InsightNarrator.activeDaysLine(stats.copy(elapsedDays = 5, activeDays = 9), en))
    }

    @Test
    fun places_lines() {
        val many = base().copy(
            placeTop = listOf(InsightRankedEntry("Cupertino, CA", 30), InsightRankedEntry("San Jose, CA", 10)),
            placeDistinct = 4, placeTotal = 50, locatedCount = 55,
            farthest = InsightFarthestPlace(8213.0, "Tokyo, Japan", 0.0, 0.0, 0.0, 0.0),
            travelKm = 640.0
        )
        assertEquals("You left footprints in 4 places, most often in Cupertino", InsightNarrator.placesLine(many, false, en))
        assertEquals(
            listOf("Farthest: Tokyo, 8,213 km from home", "Your camera traveled about 640 km"),
            InsightNarrator.placesSublines(many, en)
        )

        val one = base().copy(placeTop = listOf(InsightRankedEntry("中国北京市", 9)), placeDistinct = 1,
            farthest = InsightFarthestPlace(120.0, null, 0.0, 0.0, 0.0, 0.0), travelKm = 10.0)
        assertEquals("这段时间的照片都拍在北京市", InsightNarrator.placesLine(one, false, zh))
        assertEquals(listOf("最远到过 120 公里 之外"), InsightNarrator.placesSublines(one, zh))

        val near = one.copy(farthest = InsightFarthestPlace(99.0, "X", 0.0, 0.0, 0.0, 0.0))
        assertNull(InsightNarrator.farthestLine(near, en))

        val coordsOnly = base().copy(locatedCount = 55)
        assertEquals("55 shots have locations — naming places…", InsightNarrator.placesLine(coordsOnly, false, en))
        assertEquals(en.insPlacesScanning, InsightNarrator.placesLine(base(), true, en))
        assertEquals(en.insPlacesNone, InsightNarrator.placesLine(base(), false, en))
    }

    @Test
    fun lens_keywords_video() {
        val dominant = base().copy(
            cameraTop = listOf(InsightRankedEntry("Apple iPhone 15 Pro", 80), InsightRankedEntry("SONY ILCE-7M4", 1)),
            cameraDistinct = 2, cameraTotal = 81
        )
        assertEquals("iPhone 15 Pro took 99% of your photos", InsightNarrator.lensLine(dominant, false, en))

        val many = base().copy(
            cameraTop = listOf(InsightRankedEntry("NIKON CORPORATION NIKON D850", 4), InsightRankedEntry("Apple iPhone 13", 3)),
            cameraDistinct = 3, cameraTotal = 10
        )
        assertEquals("3 cameras, with Nikon D850 in the lead", InsightNarrator.lensLine(many, false, en))
        assertEquals("你用 3 台相机记录，Mapped 最常出场", InsightNarrator.lensLine(many, false, zh) { "Mapped" })

        val one = base().copy(cameraTop = listOf(InsightRankedEntry("Pixel 8", 9)), cameraDistinct = 1, cameraTotal = 9)
        assertEquals("Every shot came from Pixel 8", InsightNarrator.lensLine(one, false, en))
        assertEquals(en.insReadingDetails, InsightNarrator.lensLine(base(), true, en))
        assertEquals(en.insLensNone, InsightNarrator.lensLine(base(), false, en))

        val keywords = base().copy(keywordTop = listOf("sky", "night_sky", "outdoor", "beach").map { InsightRankedEntry(it, 3) })
        assertEquals("You shoot sky, night sky, and outdoor the most", InsightNarrator.keywordsLine(keywords, en))
        assertEquals("天空、夜空和户外", zh.listJoin(listOf("天空", "夜空", "户外")))
        assertEquals("a and b", en.listJoin(listOf("a", "b")))
        assertEquals("", InsightNarrator.keywordsLine(base(), en))

        assertEquals("2 hr 14 min of video", InsightNarrator.videoLine(base().copy(videoSeconds = 8040.0), en))
        assertEquals("视频共 1 小时 20 分", InsightNarrator.videoLine(base().copy(videoSeconds = 4800.0), zh))
        assertEquals("", InsightNarrator.videoLine(base().copy(videoSeconds = 30.0), en))
    }

    @Test
    fun shooting_sentences() {
        val focal = InsightShootingDistribution(InsightShootingParam.Focal, listOf(1, 3, 0, 0, 1, 0), 5, 26.0, 26.0, 3, 13.0, 77.0)
        val stats = InsightShootingStats(mapOf(InsightShootingParam.Focal to focal), sampled = 5)
        assertEquals("You shot most at 26 mm (60%)", InsightNarrator.shootingHeadline(focal, en))
        assertEquals("Ultra wide 20%, telephoto 20%", InsightNarrator.shootingDetail(focal, stats, en))
        assertEquals("Based on 5 photos with shooting data", InsightNarrator.shootingBasis(focal, en))

        val iso = InsightShootingDistribution(InsightShootingParam.Iso, listOf(8, 0, 0, 8, 0, 0), 16, 450.0, null, 0, 100.0, 800.0)
        val night = InsightShootingStats(dayIso = 100.0, nightIso = 800.0)
        assertEquals("Half your photos were shot at ISO 450 or lower", InsightNarrator.shootingHeadline(iso, en))
        assertEquals("At night your median ISO is 800, about 8.0× the daytime", InsightNarrator.shootingDetail(iso, night, en))
        assertNull(InsightNarrator.shootingDetail(iso, InsightShootingStats(), en))

        val shutter = InsightShootingDistribution(InsightShootingParam.Shutter, listOf(0, 0, 4, 0, 1, 0), 5, 1.0 / 30, null, 0, 0.01, 0.2)
        assertEquals("Half your photos used 1/30 s or faster", InsightNarrator.shootingHeadline(shutter, en))
        assertEquals("20% used 1/15 s or slower", InsightNarrator.shootingDetail(shutter, InsightShootingStats(slowShutterCount = 1), en))

        val aperture = InsightShootingDistribution(InsightShootingParam.Aperture, listOf(1, 1, 0, 0, 0, 0), 2, 1.9, 1.8, 1, 1.8, 2.2)
        assertEquals("Apertures from f/1.8 to f/2.2", InsightNarrator.shootingDetail(aperture, InsightShootingStats(), en))
    }

    @Test
    fun quiet_and_lifetime() {
        val quiet = InsightPeriodStats(period = InsightPeriod.ofMonth(2026, 9), lastActiveBeforeDayValue = 20260827)
        val narrative = InsightNarrator.narrate(quiet, lifetime, false, zh)
        assertEquals("这段时间没有新照片，上一次记录在 8月27日。", narrative.quietBody)
        assertEquals("去看 2026年8月", narrative.quietJumpTitle)
        assertEquals(InsightPeriod.ofMonth(2026, 8), narrative.quietJumpPeriod)
        assertEquals("", narrative.comparison)
        assertEquals("始于 2014年3月2日", narrative.lifetimeSince)

        val quietYear = InsightPeriodStats(period = InsightPeriod.ofYear(2024), lastActiveBeforeDayValue = 20221203)
        assertEquals("Nothing new here. Your last shot was on December 3, 2022.", InsightNarrator.quietBody(quietYear, en))
        assertEquals("Go to 2022", InsightNarrator.quietJumpTitle(quietYear, en))

        val none = InsightPeriodStats(period = InsightPeriod.ofYear(2013))
        assertEquals(en.insQuietBodyNone, InsightNarrator.quietBody(none, en))
        assertEquals("", InsightNarrator.quietJumpTitle(none, en))

        val withDays = lifetime.copy(
            coverage = InsightCoverage(photoBase = 1200, screenshotCount = 30, scanned = 1100),
            onThisDay = listOf(
                InsightOnThisDay(1, 2025, "2025-10-16", 3, photo(1, 2025, 10, 16)),
                InsightOnThisDay(3, 2023, "2023-10-16", 2, photo(2, 2023, 10, 16))
            )
        )
        assertEquals("往年的今天，你拍下了 5 张照片", InsightNarrator.onThisDayLine(withDays, zh))
        assertEquals("基于 1,200 张照片，另有 30 张截图未计入", InsightNarrator.coverageBasis(withDays, zh))
        assertEquals("还有 100 张尚未读取拍摄信息", InsightNarrator.coveragePending(withDays, zh))
        assertNull(InsightNarrator.coveragePending(lifetime, zh))
    }

    @Test
    fun aiPrompt_languages() {
        val data = lifetime.copy(
            coverage = InsightCoverage(photoBase = 1000, withCamera = 500),
            cameraTop5 = listOf(InsightRankedEntry("Pixel 8", 400)),
            monthly12 = listOf(InsightRankedEntry("2026-10", 12))
        )
        val zhPrompt = InsightNarrator.aiPrompt(data, zh)
        assertTrue(zhPrompt.startsWith("我在 Photoo 里整理出了这份相册洞察数据。"))
        assertTrue(zhPrompt.contains("常用相机 Top5:\n- Pixel 8: 400\n"))
        assertTrue(zhPrompt.contains("相机信息 50.0%"))
        assertTrue(zhPrompt.contains("常去地点 Top5:\n暂无数据\n"))
        assertFalse(zhPrompt.contains("Please write"))

        val jaPrompt = InsightNarrator.aiPrompt(data, insightStrings(AppLanguage.Japanese))
        assertTrue(jaPrompt.contains("Please write the final critique in Japanese."))
        assertTrue(jaPrompt.contains("- 2026-10: 12"))
        assertTrue(InsightNarrator.aiPrompt(data, en).contains("Please write the final critique in English."))
        assertTrue(InsightNarrator.aiPrompt(data, insightStrings(AppLanguage.Arabic)).contains("in Arabic."))
    }

    @Test
    fun cityNames() {
        assertEquals("Cupertino", insightCityName("Cupertino, CA", en.locale))
        assertEquals("东京", insightCityName("东京，日本", zh.locale))
        assertEquals("北京市", insightCityName("中国北京市", zh.locale))
    }
}
