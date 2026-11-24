package com.example.domain

import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,
    val monthNameEn: String,
    val monthNameAr: String
) {
    fun formattedEn(): String = "$day $monthNameEn $year AH"
    fun formattedAr(): String = "$day $monthNameAr $year هـ"
}

data class IslamicOccasion(
    val title: String,
    val hijriMonth: Int,
    val hijriDay: Int,
    val virtueNote: String,
    val daysRemaining: Int
)

object HijriCalendarEngine {

    private val monthsEn = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
    )

    private val monthsAr = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun gregorianToHijri(calendar: Calendar, offsetDays: Int = 0): HijriDate {
        val adj = calendar.clone() as Calendar
        adj.add(Calendar.DAY_OF_MONTH, offsetDays)
        val y = adj.get(Calendar.YEAR)
        val m = adj.get(Calendar.MONTH) + 1
        val d = adj.get(Calendar.DAY_OF_MONTH)

        val jd = if (y > 1582 || (y == 1582 && m > 10) || (y == 1582 && m == 10 && d > 14)) {
            ((1461 * (y + 4800 + (m - 14) / 12)) / 4 +
                (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12 -
                (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4 + d - 32075)
        } else {
            367 * y - (7 * (y + 5001 + (m - 9) / 7)) / 4 + (275 * m) / 9 + d + 1729777
        }

        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hMonth = ((24 * l) / 709).coerceIn(1, 12)
        val hDay = (l - (709 * hMonth) / 24).coerceIn(1, 30)
        val hYear = 30 * n + j - 30

        return HijriDate(
            year = hYear,
            month = hMonth,
            day = hDay,
            monthNameEn = monthsEn[hMonth - 1],
            monthNameAr = monthsAr[hMonth - 1]
        )
    }

    fun isWhiteDay(hijriDay: Int): Boolean = hijriDay in 13..15

    fun isSunnahFastingDay(calendar: Calendar, hijri: HijriDate): String? {
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        return when {
            hijri.month == 9 -> "Obligatory Ramadan Fast"
            hijri.month == 12 && hijri.day == 9 -> "Day of Arafah Fast"
            hijri.month == 1 && (hijri.day == 9 || hijri.day == 10) -> "Tasu'a / Ashura Fast"
            isWhiteDay(hijri.day) -> "White Days (Ayyam al-Bid)"
            dow == Calendar.MONDAY -> "Monday Sunnah Fast"
            dow == Calendar.THURSDAY -> "Thursday Sunnah Fast"
            else -> null
        }
    }

    fun upcomingOccasions(todayHijri: HijriDate): List<IslamicOccasion> {
        val milestones = listOf(
            Triple("Islamic New Year (1 Muharram)", 1, 1) to "Beginning of the Hijri year; remember the Prophet's migration.",
            Triple("Day of Ashura (10 Muharram)", 1, 10) to "Fasting Ashura expiates the sins of the previous year.",
            Triple("1st of Rajab", 7, 1) to "One of the four sacred months; increase repentance & good deeds.",
            Triple("Mid-Sha'ban (15 Sha'ban)", 8, 15) to "Prepare hearts and make up any missed Ramadan fasts.",
            Triple("Start of Ramadan (1 Ramadan)", 9, 1) to "Month of the Quran, fasting, Taraweeh, and mercy.",
            Triple("Last 10 Nights & Laylat al-Qadr", 9, 21) to "Seek Laylat al-Qadr in the odd nights; better than 1,000 months.",
            Triple("Eid al-Fitr (1 Shawwal)", 10, 1) to "Day of gratitude, Takbirat, and Zakat al-Fitr before prayer.",
            Triple("First 10 Days of Dhul-Hijjah", 12, 1) to "No days are more beloved to Allah for righteous deeds.",
            Triple("Day of Arafah (9 Dhul-Hijjah)", 12, 9) to "Fasting expiates two years of sins; greatest day of Dua.",
            Triple("Eid al-Adha (10 Dhul-Hijjah)", 12, 10) to "Day of Sacrifice (Udhiyah), joy, and Takbirat."
        )

        val todayApproxDayOfYear = (todayHijri.month - 1) * 29.5 + todayHijri.day
        return milestones.map { (meta, virtue) ->
            val targetDayOfYear = (meta.second - 1) * 29.5 + meta.third
            var diff = floor(targetDayOfYear - todayApproxDayOfYear).toInt()
            if (diff < 0) diff += 354
            IslamicOccasion(
                title = meta.first,
                hijriMonth = meta.second,
                hijriDay = meta.third,
                virtueNote = virtue,
                daysRemaining = diff
            )
        }.sortedBy { it.daysRemaining }
    }
}
