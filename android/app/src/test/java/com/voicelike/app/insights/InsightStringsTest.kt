package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat

class InsightStringsTest {

    private fun stringFields(s: InsightStrings): Map<String, String> =
        InsightStrings::class.java.declaredFields
            .filter { it.type == String::class.java }
            .associate { field ->
                field.isAccessible = true
                field.name to field.get(s) as String
            }

    @Test
    fun everyLanguageIsCompleteAndFormattable() {
        val placeholder = Regex("""%(\d\$)?[sd]""")
        for (language in AppLanguage.entries) {
            val s = insightStrings(language)
            assertEquals(language, s.language)
            for ((name, value) in stringFields(s)) {
                assertTrue("$language.$name empty", value.isNotEmpty() || name.startsWith("list"))
                assertFalse("$language.$name has a middle dot", value.contains('·') || value.contains('・'))
                assertFalse("$language.$name has an iOS placeholder", value.contains("%@") || value.contains("$@"))
                if (name.endsWith("Pattern")) {
                    SimpleDateFormat(value, s.locale) // must parse
                    continue
                }
                if (placeholder.containsMatchIn(value)) {
                    val out = String.format(s.locale, value, 7, 8, 9)
                    assertFalse("$language.$name left a placeholder: $out", placeholder.containsMatchIn(out))
                }
            }
            // English placeholders must be mirrored in every language.
            val english = stringFields(insightStrings(AppLanguage.English))
            for ((name, value) in stringFields(s)) {
                val expected = placeholder.findAll(english.getValue(name)).count()
                assertEquals("$language.$name placeholder count", expected, placeholder.findAll(value).count())
            }
            assertEquals(7, s.weekdayNames().size)
            assertTrue(s.veryShortWeekdayNames().all { it.isNotEmpty() })
            assertTrue(s.fullDate(20261016).isNotEmpty())
            assertTrue(s.monthDay(20261016).isNotEmpty())
        }
    }

    @Test
    fun formattingHelpers() {
        val zh = insightStrings(AppLanguage.SimplifiedChinese)
        val en = insightStrings(AppLanguage.English)
        assertEquals("2026年", zh.periodTitle(InsightPeriod.ofYear(2026)))
        assertEquals("2026年3月", zh.periodTitle(InsightPeriod.ofMonth(2026, 3)))
        assertEquals("2026-3", en.periodTitle(InsightPeriod.ofMonth(2026, 3)))
        assertEquals("10月14日", zh.dayLabel("2026-10-14"))
        assertEquals("10/14", en.dayLabel("2026-10-14"))
        assertEquals("10月14日", zh.monthDay(20261014))
        assertEquals("Oct 14", en.monthDay(20261014))
        assertEquals("2014年3月2日", zh.fullDate(20140302))
        assertEquals("March 2, 2014", en.fullDate(20140302))
        assertEquals("8,213 公里", zh.distance(8213.4))
        assertEquals("12 km", en.distance(12.0))
        assertEquals("1 小时 20 分", zh.duration(4800.0))
        assertEquals("45 min", en.duration(2700.0))
        assertEquals("3 hr", en.duration(10_800.0))
        assertEquals("72%", en.percent(0.724))
        assertEquals("+18%", en.signedPercent(0.18))
        assertEquals("-12%", en.signedPercent(-0.12))
        assertEquals("52,318", en.number(52318))
        assertEquals("2 年前", zh.yearsAgo(2))
        assertEquals("1 shot", en.shots(1))
        assertEquals("3 shots", en.shots(3))
        assertEquals("1 day", en.days(1))
        assertEquals("正在读取拍摄信息（42%）", zh.scanningLine(0.42))
        assertEquals("星期六", zh.weekdayNames()[6])
        assertEquals("Saturday", en.weekdayNames()[6])
        assertEquals("六", zh.veryShortWeekdayNames()[6])
        assertEquals("S", en.veryShortWeekdayNames()[6])
        assertEquals("05:00–09:00", insightDaypartTitle(InsightDaypart.EarlyMorning))
        assertEquals("20:00–00:00", insightDaypartTitle(InsightDaypart.Night))
        // Arabic keeps Latin digits
        assertTrue(insightStrings(AppLanguage.Arabic).number(1234).none { it in '٠'..'٩' })
    }

    @Test
    fun drillTitles() {
        val zh = insightStrings(AppLanguage.SimplifiedChinese)
        val en = insightStrings(AppLanguage.English)
        val year = InsightPeriod.ofYear(2026)
        assertEquals("2026年照片", zh.drillTitle(InsightDrill.Photos, year))
        assertEquals("2026 Photos", en.drillTitle(InsightDrill.Photos, year))
        assertEquals("2026年 Pixel 8", zh.drillTitle(InsightDrill.Camera("Pixel 8"), year))
        assertEquals("2026 21–30 mm", en.drillTitle(InsightDrill.Shooting(InsightShootingParam.Focal, 1), year))
        assertEquals("2026年星期日", zh.drillTitle(InsightDrill.Weekday(1), year))
        assertEquals("10月14日", zh.drillTitle(InsightDrill.Day("2026-10-14"), year))
        assertEquals("2025年3月", zh.drillTitle(InsightDrill.Month("2025-03"), year))
        assertEquals("2026年竖拍", zh.drillTitle(InsightDrill.Orientation(InsightOrientation.Vertical), year))
        assertEquals("2026 night sky", en.drillTitle(InsightDrill.Keyword("night_sky"), year))
        assertEquals("2026年照片（42）", zh.countTitle(zh.drillTitle(InsightDrill.Photos, year), 42))
        assertEquals("2026 Photos (42)", en.countTitle(en.drillTitle(InsightDrill.Photos, year), 42))
    }
}
