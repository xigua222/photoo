package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightStatsEngineTest {

    private fun compute(
        items: List<MediaItem>,
        period: InsightPeriod,
        snapshots: Map<Long, MediaInsightSnapshot> = emptyMap(),
        liked: Set<Long> = emptySet(),
        now: Long = NOW
    ): Pair<InsightLifetimeStats, InsightPeriodStats> {
        val lifetime = InsightStatsEngine.computeLifetime(items, snapshots, liked, now)
        return lifetime to InsightStatsEngine.computePeriod(items, snapshots, liked, period, lifetime, now)
    }

    @Test
    fun periodScoping_yearAndMonth() {
        val items = listOf(
            photo(1, 2026, 10, 3), photo(2, 2026, 10, 3), video(3, 2026, 9, 1),
            screenshot(4, 2026, 1, 5), photo(5, 2025, 12, 31), photo(6, 2027, 1, 1)
        )
        val (_, year) = compute(items, InsightPeriod.ofYear(2026))
        assertEquals(4, year.total)
        assertEquals(3, year.photos)
        assertEquals(1, year.videos)
        assertEquals(1, year.screenshots)
        assertEquals(3, year.activeDays)

        val (_, month) = compute(items, InsightPeriod.ofMonth(2026, 10))
        assertEquals(2, month.total)
        assertEquals(1, month.activeDays)
        assertEquals("2026-10-03", month.busiestDayKey)
        assertEquals(mapOf("2026-10-03" to 2), month.heatmap)
    }

    @Test
    fun chart_yearMonthsAndFutureFrom() {
        val items = listOf(photo(1, 2026, 3, 2), photo(2, 2026, 3, 9), photo(3, 2026, 10, 1))
        val (_, year) = compute(items, InsightPeriod.ofYear(2026))
        assertEquals(12, year.chart.buckets.size)
        assertEquals(2, year.chart.buckets[2].count)
        assertEquals("2026-03", year.chart.buckets[2].key)
        assertEquals(10, year.chart.futureFrom) // November onward
        assertEquals(InsightDrill.Month("2026-03"), year.chart.drillFor(2))

        val (_, month) = compute(items, InsightPeriod.ofMonth(2026, 10))
        assertEquals(31, month.chart.buckets.size)
        assertEquals(16, month.chart.futureFrom) // the 17th is the first future day
        assertEquals("8", month.chart.buckets[7].label)
        assertNull(month.chart.buckets[1].label)
        assertEquals(InsightDrill.Day("2026-10-01"), month.chart.drillFor(0))
    }

    @Test
    fun comparison_sameToDateWhileInProgress() {
        val items = listOf(
            photo(1, 2026, 2, 1),
            photo(10, 2025, 3, 1), photo(11, 2025, 10, 16), photo(12, 2025, 10, 17), photo(13, 2025, 11, 1)
        )
        val (_, year) = compute(items, InsightPeriod.ofYear(2026))
        assertTrue(year.isInProgress)
        assertEquals(4, year.comparison.previousTotal)
        assertEquals(2, year.comparison.previousToDate)

        val monthItems = listOf(photo(1, 2026, 10, 2), photo(2, 2026, 9, 10), photo(3, 2026, 9, 16), photo(4, 2026, 9, 20))
        val (_, month) = compute(monthItems, InsightPeriod.ofMonth(2026, 10))
        assertEquals(3, month.comparison.previousTotal)
        assertEquals(2, month.comparison.previousToDate)

        val (_, past) = compute(monthItems, InsightPeriod.ofMonth(2026, 9))
        assertFalse(past.isInProgress)
        assertNull(past.comparison.previousToDate)
    }

    @Test
    fun comparison_recordNeedsAnotherPeriod() {
        val one = listOf(photo(1, 2026, 1, 1), photo(2, 2026, 1, 2))
        assertFalse(compute(one, InsightPeriod.ofYear(2026)).second.comparison.isRecord)

        val two = one + photo(3, 2024, 5, 5)
        assertTrue(compute(two, InsightPeriod.ofYear(2026)).second.comparison.isRecord)

        val tie = two + photo(4, 2024, 5, 6)
        assertFalse(compute(tie, InsightPeriod.ofYear(2026)).second.comparison.isRecord)
    }

    @Test
    fun streak_longestRunTiesGoToLatest() {
        val items = listOf(1, 2, 3, 5, 6, 7, 10).mapIndexed { i, d -> photo(i.toLong() + 1, 2026, 3, d) }
        val (lifetime, stats) = compute(items, InsightPeriod.ofMonth(2026, 3))
        val streak = stats.longestStreak!!
        assertEquals(3, streak.length)
        assertEquals("2026-03-05", streak.startDayKey)
        assertEquals("2026-03-07", streak.endDayKey)
        assertEquals(3, lifetime.longestStreak)
        assertEquals(7, lifetime.activeDays)
    }

    @Test
    fun busiestDay_tieGoesToLaterDayAndCoverRule() {
        val items = listOf(
            photo(1, 2026, 3, 4, hour = 9), photo(2, 2026, 3, 4, hour = 10), photo(3, 2026, 3, 4, hour = 11),
            screenshot(4, 2026, 3, 9, hour = 8), photo(5, 2026, 3, 9, hour = 10), photo(6, 2026, 3, 9, hour = 15)
        )
        val (_, stats) = compute(items, InsightPeriod.ofMonth(2026, 3))
        assertEquals("2026-03-09", stats.busiestDayKey)
        assertEquals(3, stats.busiestDayCount)
        assertEquals(5L, stats.cover?.id) // earliest non-screenshot photo
        assertFalse(stats.coverIsLiked)

        val (_, likedStats) = compute(items, InsightPeriod.ofMonth(2026, 3), liked = setOf(4L, 5L, 6L))
        assertEquals(6L, likedStats.cover?.id) // newest liked non-screenshot photo
        assertTrue(likedStats.coverIsLiked)

        val onlyVideos = listOf(video(7, 2026, 3, 2, hour = 18), video(8, 2026, 3, 2, hour = 9))
        assertEquals(8L, compute(onlyVideos, InsightPeriod.ofMonth(2026, 3)).second.cover?.id)
    }

    @Test
    fun heatThresholdsAndLevels() {
        val thresholds = InsightStatsEngine.heatThresholds((1..10).toList() + listOf(0, 0))
        assertEquals(listOf(2, 5, 7, 9), thresholds)
        assertEquals(0, InsightStatsEngine.heatLevel(0, thresholds))
        assertEquals(1, InsightStatsEngine.heatLevel(1, thresholds))
        assertEquals(1, InsightStatsEngine.heatLevel(2, thresholds))
        assertEquals(2, InsightStatsEngine.heatLevel(3, thresholds))
        assertEquals(4, InsightStatsEngine.heatLevel(9, thresholds))
        assertEquals(5, InsightStatsEngine.heatLevel(10, thresholds))
        assertEquals(1, InsightStatsEngine.heatLevel(7, emptyList()))
        assertTrue(InsightStatsEngine.heatThresholds(listOf(0, 0)).isEmpty())
    }

    @Test
    fun shooting_binsMediansModesAndDayNight() {
        val focals = listOf(13f, 24f, 26f, 26f, 77f)
        val items = ArrayList<MediaItem>()
        val snaps = HashMap<Long, MediaInsightSnapshot>()
        focals.forEachIndexed { i, f ->
            val id = i + 1L
            items += photo(id, 2026, 3, 1, hour = 12)
            snaps[id] = shot(focal = f, aperture = if (i == 0) 1.78f else 2.2f)
        }
        // 8 daytime ISO 100 at 1/500 s and 8 night ISO 800 at 1/30 s
        for (i in 0 until 8) {
            val day = 100L + i
            val night = 200L + i
            items += photo(day, 2026, 3, 2, hour = 10)
            items += photo(night, 2026, 3, 2, hour = 22)
            snaps[day] = shot(iso = 100, exposure = 1.0 / 500)
            snaps[night] = shot(iso = 800, exposure = 1.0 / 30)
        }
        // screenshots and unscanned photos never count
        items += screenshot(300, 2026, 3, 3)
        snaps[300L] = shot(focal = 26f)
        items += photo(301, 2026, 3, 3)
        snaps[301L] = MediaInsightSnapshot(focal35 = 26f, shootingScanned = false)

        val (_, stats) = compute(items, InsightPeriod.ofMonth(2026, 3), snaps)
        val shooting = stats.shooting
        assertEquals(21, shooting.sampled)
        val focal = shooting.distribution(InsightShootingParam.Focal)!!
        assertEquals(listOf(1, 3, 0, 0, 1, 0), focal.counts)
        assertEquals(26.0, focal.median!!, 1e-9)
        assertEquals(26.0, focal.mode!!, 1e-9)
        assertEquals(2, focal.modeCount)
        assertEquals(13.0, focal.min!!, 1e-9)

        val aperture = shooting.distribution(InsightShootingParam.Aperture)!!
        assertEquals(1.78, aperture.min!!, 1e-6) // 1.78 lands in the first bin (≤ 1.8)
        assertEquals(1, aperture.counts[0])

        val iso = shooting.distribution(InsightShootingParam.Iso)!!
        assertEquals(450.0, iso.median!!, 1e-9)
        assertEquals(100.0, shooting.dayIso!!, 1e-9)
        assertEquals(800.0, shooting.nightIso!!, 1e-9)
        assertEquals(1.0 / 30, shooting.nightShutter!!, 1e-12)
        assertEquals(0, shooting.slowShutterCount)
        assertEquals(
            listOf(InsightShootingParam.Focal, InsightShootingParam.Aperture, InsightShootingParam.Iso, InsightShootingParam.Shutter),
            shooting.available
        )
    }

    @Test
    fun shootingParam_labelsAndBins() {
        assertEquals(0, InsightShootingParam.Focal.bin(20.0))
        assertEquals(1, InsightShootingParam.Focal.bin(20.5))
        assertEquals(5, InsightShootingParam.Focal.bin(240.0))
        assertEquals(3, InsightShootingParam.Shutter.bin(1.0 / 15))
        assertEquals("21–30 mm", InsightShootingParam.Focal.rangeLabel(1))
        assertEquals("f/1.9–2.4", InsightShootingParam.Aperture.rangeLabel(1))
        assertEquals("ISO 101–200", InsightShootingParam.Iso.rangeLabel(1))
        assertEquals("1/1000–1/250 s", InsightShootingParam.Shutter.rangeLabel(1))
        assertEquals(">100 mm", InsightShootingParam.Focal.rangeLabel(5))
        assertEquals("≤20", InsightShootingParam.Focal.axisLabel(0))
        assertEquals("≤1/1000", InsightShootingParam.Shutter.axisLabel(0))
        assertEquals(">1/2", InsightShootingParam.Shutter.axisLabel(5))
        assertEquals("f/1.8", InsightShootingParam.Aperture.format(1.8))
        assertEquals("f/2", InsightShootingParam.Aperture.format(2.0))
        assertEquals("1/120 s", InsightShootingParam.Shutter.format(1.0 / 120))
        assertEquals("2 s", InsightShootingParam.Shutter.format(2.0))
    }

    @Test
    fun orientation_squareRuleAndScope() {
        assertEquals(InsightOrientation.Square, InsightOrientation.of(1000, 1015))
        assertEquals(InsightOrientation.Vertical, InsightOrientation.of(1000, 1030))
        assertEquals(InsightOrientation.Horizontal, InsightOrientation.of(4000, 3000))
        assertEquals(InsightOrientation.Square, InsightOrientation.of(0, 0))

        val items = listOf(
            photo(1, 2026, 3, 1, width = 3000, height = 4000),
            photo(2, 2026, 3, 1, width = 4000, height = 3000),
            photo(3, 2026, 3, 1, width = 3000, height = 3000),
            screenshot(4, 2026, 3, 1),
            video(5, 2026, 3, 1)
        )
        val (_, stats) = compute(items, InsightPeriod.ofMonth(2026, 3))
        assertEquals(InsightOrientationSplit(1, 1, 1), stats.orientation)
    }

    @Test
    fun geo_clustersPinsHomeFarthestAndTravel() {
        val items = ArrayList<MediaItem>()
        val snaps = HashMap<Long, MediaInsightSnapshot>()
        // Home: 20 photos in Beijing in 2025
        for (i in 0 until 20) {
            val id = 1L + i
            items += photo(id, 2025, 5, 1 + i % 20)
            snaps[id] = geo(39.90 + i * 0.001, 116.40, "Beijing, China")
        }
        // Period (2026-03): day 1 Beijing, day 2 Shanghai (unlabelled) + labelled point 5 km away
        items += photo(100, 2026, 3, 1); snaps[100L] = geo(39.91, 116.41, "Beijing, China")
        items += photo(101, 2026, 3, 1); snaps[101L] = geo(39.911, 116.411, "Beijing, China")
        items += photo(102, 2026, 3, 2); snaps[102L] = geo(31.23, 121.47, null)
        items += photo(103, 2026, 3, 2); snaps[103L] = geo(31.24, 121.48, "Shanghai, China")
        items += photo(104, 2026, 3, 2); snaps[104L] = geo(31.23, 121.47, "31.23, 121.47")

        val (lifetime, stats) = compute(items, InsightPeriod.ofMonth(2026, 3), snaps)
        val home = lifetime.home!!
        assertEquals(39.9, home.latitude, 0.05)
        assertEquals(116.4, home.longitude, 0.05)
        assertEquals(2, lifetime.distinctPlaces)

        assertEquals(5, stats.locatedCount)
        assertEquals(3, stats.placeTotal)
        assertEquals(listOf(InsightRankedEntry("Beijing, China", 2), InsightRankedEntry("Shanghai, China", 1)), stats.placeTop)
        assertEquals(2, stats.placePins.size)
        assertEquals(2, stats.geoClusters.size)
        assertEquals(3, stats.geoClusters[0].count) // Shanghai cell holds three points

        val farthest = stats.farthest!!
        assertTrue(farthest.kilometers > 1000)
        assertEquals("Shanghai, China", farthest.label)
        assertTrue(stats.travelKm > 1000)
    }

    @Test
    fun farthest_needsHundredKilometers() {
        val items = listOf(photo(1, 2026, 3, 1), photo(2, 2026, 3, 2))
        val snaps = mapOf(1L to geo(39.9, 116.4, "Beijing"), 2L to geo(40.3, 116.4, "Huairou"))
        val (_, stats) = compute(items, InsightPeriod.ofMonth(2026, 3), snaps)
        assertNull(stats.farthest)
        assertTrue(stats.travelKm in 40.0..50.0)
    }

    @Test
    fun placeLabelNormalisation() {
        assertEquals("Paris, France", insightNormalizedPlaceLabel("Paris, France"))
        assertEquals("Cupertino, CA", insightNormalizedPlaceLabel("1 Infinite Loop, Cupertino, CA, USA"))
        assertEquals("东京, 日本", insightNormalizedPlaceLabel("东京，日本"))
        assertNull(insightPlaceLabel(MediaInsightSnapshot(location = "Unknown Location")))
        assertNull(insightPlaceLabel(MediaInsightSnapshot(location = "--")))
        assertNull(insightPlaceLabel(MediaInsightSnapshot(location = "31.2, 121.4")))
        assertNull(insightCameraLabel(MediaInsightSnapshot(camera = " -- ")))
        assertEquals("Pixel 8", insightCameraLabel(MediaInsightSnapshot(camera = "Pixel 8")))
    }

    @Test
    fun drill_matchingAndOrder() {
        val items = listOf(
            photo(1, 2026, 3, 1, hour = 22),          // Sunday
            photo(2, 2026, 3, 2, hour = 7, live = true),
            video(3, 2026, 3, 2, hour = 8),
            screenshot(4, 2026, 3, 3),
            photo(5, 2025, 3, 1)
        )
        val snaps = mapOf(
            1L to MediaInsightSnapshot(camera = "Pixel 8", keywords = listOf("sky", "night_sky"), location = "Paris, France"),
            3L to MediaInsightSnapshot(camera = "Pixel 8", keywords = listOf("sky")),
            2L to shot(focal = 24f),
            5L to MediaInsightSnapshot(camera = "Pixel 8")
        )
        val period = InsightPeriod.ofMonth(2026, 3)
        fun ids(drill: InsightDrill, liked: Set<Long> = emptySet()) =
            InsightStatsEngine.drillItems(items, snaps, liked, period, drill).map { it.id }

        assertEquals(listOf(4L, 3L, 2L, 1L), ids(InsightDrill.Period))
        assertEquals(listOf(4L, 2L, 1L), ids(InsightDrill.Photos))
        assertEquals(listOf(3L), ids(InsightDrill.Videos))
        assertEquals(listOf(4L), ids(InsightDrill.Screenshots))
        assertEquals(listOf(1L), ids(InsightDrill.Camera("Pixel 8"))) // photos only, period only
        assertEquals(listOf(3L, 1L), ids(InsightDrill.Keyword("sky")))
        assertEquals(listOf(1L), ids(InsightDrill.Place("Paris, France")))
        assertEquals(listOf(1L), ids(InsightDrill.Located))
        assertEquals(listOf(1L), ids(InsightDrill.Weekday(1)))
        assertEquals(listOf(1L), ids(InsightDrill.Daypart(InsightDaypart.Night)))
        assertEquals(listOf(3L, 2L), ids(InsightDrill.Daypart(InsightDaypart.EarlyMorning)))
        assertEquals(listOf(2L), ids(InsightDrill.Format(InsightFormat.Live)))
        assertEquals(listOf(2L), ids(InsightDrill.Shooting(InsightShootingParam.Focal, 1)))
        assertEquals(listOf(2L, 1L), ids(InsightDrill.Orientation(InsightOrientation.Vertical)))
        assertEquals(listOf(2L), ids(InsightDrill.Liked, liked = setOf(2L, 5L)))
        // Day / Month ignore the period
        assertEquals(listOf(5L), ids(InsightDrill.Day("2025-03-01")))
        assertEquals(listOf(5L), ids(InsightDrill.Month("2025-03")))

        // Counts agree with the drill sizes
        val lifetime = InsightStatsEngine.computeLifetime(items, snaps, emptySet(), NOW)
        val stats = InsightStatsEngine.computePeriod(items, snaps, emptySet(), period, lifetime, NOW)
        assertEquals(1, stats.weekdayCounts[0])
        assertEquals(listOf(InsightRankedEntry("Pixel 8", 1)), stats.cameraTop)
        assertEquals(InsightRankedEntry("sky", 2), stats.keywordTop.first())
        assertEquals(listOf(InsightFormatCount(InsightFormat.Live, 1)), stats.formats)
    }

    @Test
    fun weekdayIndexFromDayValue() {
        // 2026-03-01 is a Sunday, 1970-01-01 a Thursday
        assertEquals(0, InsightStatsEngine.weekdayIndex(InsightStatsEngine.ordinalOfDayValue(20260301)))
        assertEquals(4, InsightStatsEngine.weekdayIndex(0))
        assertEquals(6, InsightStatsEngine.weekdayIndex(InsightStatsEngine.ordinalOfDayValue(20261017)))
    }

    @Test
    fun defaultPeriod_rules() {
        fun lifetimeOf(items: List<MediaItem>) = InsightStatsEngine.computeLifetime(items, emptyMap(), emptySet(), NOW)

        val threeMonths = listOf(photo(1, 2026, 1, 1), photo(2, 2026, 4, 1), photo(3, 2026, 9, 1))
        assertEquals(InsightPeriodMode.Year, InsightStatsEngine.defaultPeriod(lifetimeOf(threeMonths), NOW).mode)

        val thisMonth = listOf(photo(1, 2026, 10, 1), photo(2, 2025, 4, 1))
        assertEquals(InsightPeriod.ofMonth(2026, 10), InsightStatsEngine.defaultPeriod(lifetimeOf(thisMonth), NOW))

        val older = listOf(photo(1, 2026, 2, 1), photo(2, 2025, 4, 1))
        assertEquals(InsightPeriod.ofMonth(2026, 2), InsightStatsEngine.defaultPeriod(lifetimeOf(older), NOW))

        val empty = InsightStatsEngine.defaultPeriod(lifetimeOf(emptyList()), NOW)
        assertEquals(InsightPeriod.ofYear(2026, 10), empty)

        InsightDefaultPeriodOnce.resetForTests()
        assertNull(InsightDefaultPeriodOnce.take(lifetimeOf(emptyList()), NOW))
        assertNotNull(InsightDefaultPeriodOnce.take(lifetimeOf(older), NOW))
        assertNull(InsightDefaultPeriodOnce.take(lifetimeOf(older), NOW))
        InsightDefaultPeriodOnce.resetForTests()
    }

    @Test
    fun periodStepping_limits() {
        val lifetime = InsightStatsEngine.computeLifetime(listOf(photo(1, 2024, 5, 3)), emptyMap(), emptySet(), NOW)
        assertFalse(InsightStatsEngine.canStepNext(InsightPeriod.ofMonth(2026, 10), NOW))
        assertTrue(InsightStatsEngine.canStepNext(InsightPeriod.ofMonth(2026, 9), NOW))
        assertFalse(InsightStatsEngine.canStepNext(InsightPeriod.ofYear(2026), NOW))
        assertFalse(InsightStatsEngine.canStepPrev(InsightPeriod.ofMonth(2024, 5), lifetime))
        assertTrue(InsightStatsEngine.canStepPrev(InsightPeriod.ofMonth(2024, 6), lifetime))
        assertFalse(InsightStatsEngine.canStepPrev(InsightPeriod.ofYear(2024), lifetime))
        assertEquals(InsightPeriod.ofMonth(2024, 5), InsightStatsEngine.step(InsightPeriod.ofMonth(2024, 7), -5, lifetime, NOW))
        assertEquals(InsightPeriod.ofMonth(2025, 1), InsightStatsEngine.step(InsightPeriod.ofMonth(2024, 12), 1, lifetime, NOW))
        assertEquals(InsightPeriod.ofMonth(2026, 10), InsightStatsEngine.clamp(InsightPeriod.ofMonth(2027, 2), lifetime, NOW))
        assertEquals(InsightPeriod.ofMonth(2024, 5), InsightStatsEngine.clamp(InsightPeriod.ofMonth(2020, 2), lifetime, NOW))
        assertEquals(20240101..20241231, InsightStatsEngine.periodBounds(InsightPeriod.ofYear(2024)))
        assertEquals(20240201..20240229, InsightStatsEngine.periodBounds(InsightPeriod.ofMonth(2024, 2)))
    }

    @Test
    fun lifetime_totalsCoverageTimelineOnThisDay() {
        val items = listOf(
            photo(1, 2023, 10, 16, hour = 9), photo(2, 2023, 10, 16, hour = 18), screenshot(3, 2023, 10, 16),
            photo(4, 2025, 10, 16), photo(5, 2025, 3, 1), video(6, 2025, 3, 1, durationMs = 90_000),
            photo(7, 2026, 10, 16)
        )
        val snaps = mapOf(
            1L to MediaInsightSnapshot(camera = "Pixel 6", metadataScanned = true, latitude = 1.0, longitude = 1.0),
            2L to MediaInsightSnapshot(camera = "Pixel 6", metadataScanned = true, keywords = listOf("cat")),
            4L to MediaInsightSnapshot(camera = "Pixel 8", metadataScanned = true),
            5L to MediaInsightSnapshot(camera = "Galaxy", metadataScanned = true)
        )
        val lifetime = InsightStatsEngine.computeLifetime(items, snaps, emptySet(), NOW)
        assertEquals(7, lifetime.total)
        assertEquals("2023-10-16", lifetime.firstDayKey)
        assertEquals(20231016, lifetime.firstDayValue)
        assertEquals(listOf(InsightYearTotal(2023, 3), InsightYearTotal(2024, 0), InsightYearTotal(2025, 3), InsightYearTotal(2026, 1)), lifetime.yearTotals)
        assertEquals(InsightCoverage(photoBase = 5, screenshotCount = 1, scanned = 4, withCamera = 4, withLocation = 1, withTags = 1), lifetime.coverage)
        assertEquals(1, lifetime.coverage.pending)
        assertEquals(90.0, lifetime.videoSeconds, 1e-9)
        assertEquals(listOf(2023, 2025), lifetime.cameraTimeline.map { it.year })
        assertEquals("Pixel 6", lifetime.cameraTimeline[0].camera)
        assertEquals("Galaxy", lifetime.cameraTimeline[1].camera) // 1-1 tie broken by name
        assertEquals(listOf(2025, 2023), lifetime.onThisDay.map { it.year })
        assertEquals(listOf(1, 3), lifetime.onThisDay.map { it.yearsAgo })
        assertEquals(2, lifetime.onThisDay[1].count) // screenshot excluded
        assertEquals(2L, lifetime.onThisDay[1].cover.id) // newest photo of that day
        assertEquals(12, lifetime.monthly12.size)
        assertEquals("2026-10", lifetime.monthly12.last().id)
        assertEquals(InsightRankedEntry("Pixel 6", 2), lifetime.cameraTop5.first())
    }

    @Test
    fun quietPeriodAndElapsedDays() {
        val items = listOf(photo(1, 2026, 8, 27), photo(2, 2026, 8, 5))
        val (_, quiet) = compute(items, InsightPeriod.ofMonth(2026, 9))
        assertTrue(quiet.isEmpty)
        assertEquals(20260827, quiet.lastActiveBeforeDayValue)
        assertEquals(InsightPeriod.ofMonth(2026, 8), InsightStatsEngine.quietJumpPeriod(quiet))

        val (_, august) = compute(items, InsightPeriod.ofMonth(2026, 8))
        assertEquals(27, august.elapsedDays) // library starts on the 5th
        val (_, october) = compute(items, InsightPeriod.ofMonth(2026, 10))
        assertEquals(16, october.elapsedDays)
    }

    @Test
    fun largeLibraryIsFast() {
        val items = ArrayList<MediaItem>(100_000)
        val snaps = HashMap<Long, MediaInsightSnapshot>(100_000)
        val base = localSeconds(2016, 1, 1)
        for (i in 0 until 100_000) {
            val id = i.toLong() + 1
            val item = photo(id, 2016, 1, 1).copy(dateAdded = base + i * 3_000L)
            items += item
            snaps[id] = MediaInsightSnapshot(
                camera = "Cam ${i % 7}",
                location = "City ${i % 50}, Country",
                latitude = 30.0 + (i % 50) * 0.3,
                longitude = 110.0 + (i % 50) * 0.3,
                keywords = listOf("k${i % 30}", "k${i % 11}"),
                focal35 = 26f, iso = 100 + i % 900, exposure = 0.01, shootingScanned = true
            )
        }
        // warm the date cache like the app does; report the best of three runs (CI machines are noisy)
        val lifetime = InsightStatsEngine.computeLifetime(items, snaps, emptySet(), NOW)
        var elapsedMs = Long.MAX_VALUE
        var lifetimeMs = 0L
        repeat(3) {
            val start = System.nanoTime()
            val again = InsightStatsEngine.computeLifetime(items, snaps, emptySet(), NOW)
            val mid = System.nanoTime()
            val stats = InsightStatsEngine.computePeriod(items, snaps, emptySet(), InsightPeriod.ofYear(2020), again, NOW)
            val ms = (System.nanoTime() - start) / 1_000_000
            if (ms < elapsedMs) {
                elapsedMs = ms
                lifetimeMs = (mid - start) / 1_000_000
            }
            assertEquals(lifetime.total, again.total)
            assertTrue(stats.total > 0)
        }
        println("insights warm lifetime+period on 100k: $elapsedMs ms (lifetime $lifetimeMs ms)")
        assertTrue("took $elapsedMs ms", elapsedMs < 3_000)
    }
}
