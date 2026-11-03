package com.voicelike.app

import android.icu.util.ChineseCalendar
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HolidayUtils {

    fun getHoliday(date: Date, strings: LocalizedStrings): String? {
        val cal = Calendar.getInstance()
        cal.time = date
        val month = cal.get(Calendar.MONTH) + 1 // 1-12
        val day = cal.get(Calendar.DAY_OF_MONTH)
        
        // 1. Gregorian Fixed Holidays
        // New Year: Jan 1
        if (month == 1 && day == 1) return "New Year's Day" // Need localized string key
        
        // Valentine's: Feb 14
        if (month == 2 && day == 14) return "Valentine's Day"
        
        // Children's Day: Jun 1
        if (month == 6 && day == 1) return "Children's Day"
        
        // Labor Day: May 1-5
        if (month == 5 && day in 1..5) return "Labor Day Holiday"
        
        // National Day: Oct 1-7 (China)
        if (month == 10 && day in 1..7) return "National Day Holiday"

        // 2. Lunar Holidays (Requires Android N / API 24+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val chineseCal = ChineseCalendar()
            chineseCal.time = date
            val lMonth = chineseCal.get(ChineseCalendar.MONTH) + 1 // 0-based in ICU? ChineseCalendar.MONTH is 0-11. 
            // Note: ChineseCalendar.MONTH is 0-based. 1st month is 0.
            // However, leap months might complicate this. 
            // isLeapMonth check is needed.
            val isLeap = chineseCal.get(ChineseCalendar.IS_LEAP_MONTH) == 1
            
            if (!isLeap) {
                val lunarMonth = lMonth
                val lunarDay = chineseCal.get(ChineseCalendar.DAY_OF_MONTH)
                
                // Spring Festival (CNY): Lunar 1.1 - 1.7 (Approx week)
                if (lunarMonth == 1 && lunarDay in 1..7) return "Spring Festival"
                
                // Dragon Boat: Lunar 5.5
                if (lunarMonth == 5 && lunarDay == 5) return "Dragon Boat Festival"
                
                // Mid-Autumn: Lunar 8.15
                if (lunarMonth == 8 && lunarDay == 15) return "Mid-Autumn Festival"
            }
        }
        
        return null
    }
    
    // Helper to map internal English keys to LocalizedStrings
    fun getLocalizedHolidayName(holidayKey: String, strings: LocalizedStrings): String {
        return when(holidayKey) {
            "New Year's Day" -> strings.holidayNewYear
            "Valentine's Day" -> strings.holidayValentines
            "Children's Day" -> strings.holidayChildrens
            "Labor Day Holiday" -> strings.holidayLaborDay
            "National Day Holiday" -> strings.holidayNationalDay
            "Spring Festival" -> strings.holidaySpringFestival
            "Dragon Boat Festival" -> strings.holidayDragonBoat
            "Mid-Autumn Festival" -> strings.holidayMidAutumn
            else -> holidayKey
        }
    }
}
