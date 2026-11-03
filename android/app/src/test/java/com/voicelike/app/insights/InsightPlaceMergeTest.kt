package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Test

class InsightPlaceMergeTest {

    @Test
    fun sameCityUnderTwoGeocoderNamesCountsOnce() {
        val items = (1L..4L).map { photo(it, 2026, 10, 10) }
        val snapshots = mapOf(
            1L to geo(39.90, 116.40, "北京市, China"),
            2L to geo(39.92, 116.39, "北京市, China"),
            3L to geo(40.08, 116.59, "Bei Jing Shi, China"),
            4L to geo(31.23, 121.47, "Shang Hai Shi, China")
        )
        val lifetime = InsightStatsEngine.computeLifetime(items, snapshots, emptySet(), NOW)
        val stats = InsightStatsEngine.computePeriod(items, snapshots, emptySet(), InsightPeriod.ofYear(2026), lifetime, NOW)
        assertEquals(listOf("北京市, China" to 3, "Shang Hai Shi, China" to 1), stats.placeTop.map { it.id to it.count })
        assertEquals(2, stats.placeDistinct)
        val drilled = InsightStatsEngine.drillItems(items, snapshots, emptySet(), InsightPeriod.ofYear(2026), InsightDrill.Place("北京市, China"))
        assertEquals(setOf(1L, 2L, 3L), drilled.map { it.id }.toSet())
    }

    @Test
    fun neighbouringCitiesWithRealNamesStayApart() {
        val items = (1L..2L).map { photo(it, 2026, 10, 10) }
        val snapshots = mapOf(
            1L to geo(23.13, 113.26, "广州市, China"),
            2L to geo(23.02, 113.12, "佛山市, China")
        )
        val lifetime = InsightStatsEngine.computeLifetime(items, snapshots, emptySet(), NOW)
        val stats = InsightStatsEngine.computePeriod(items, snapshots, emptySet(), InsightPeriod.ofYear(2026), lifetime, NOW)
        assertEquals(2, stats.placeDistinct)
    }
}
