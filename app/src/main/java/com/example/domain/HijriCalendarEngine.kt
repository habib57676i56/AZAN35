package com.example.domain

import com.example.core.i18n.AppLanguage
import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,
    val monthNameEn: String,
    val monthNameAr: String,
    val monthNameUz: String
) {
    fun formattedEn(): String = "$day $monthNameEn $year AH"
    fun formattedAr(): String = "$day $monthNameAr $year هـ"
    fun formattedUz(): String = "$day-$monthNameUz $year-hijriy"
    fun formattedFor(language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC, AppLanguage.URDU -> formattedAr()
        AppLanguage.UZBEK -> formattedUz()
        AppLanguage.ENGLISH, AppLanguage.TURKISH -> formattedEn()
    }
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

    private val monthsUz = listOf(
        "Muharram", "Safar", "Rabiul-avval", "Rabiul-oxir",
        "Jumodul-avval", "Jumodul-oxir", "Rajab", "Sha'bon",
        "Ramazon", "Shavvol", "Zulqa'da", "Zulhijja"
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
            monthNameAr = monthsAr[hMonth - 1],
            monthNameUz = monthsUz[hMonth - 1]
        )
    }

    fun isWhiteDay(hijriDay: Int): Boolean = hijriDay in 13..15

    fun isSunnahFastingDay(
        calendar: Calendar,
        hijri: HijriDate,
        language: AppLanguage = AppLanguage.ENGLISH
    ): String? {
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        return when {
            hijri.month == 9 -> language.pick("صيام رمضان المبارك", "Obligatory Ramadan Fast", "Muborak Ramazon ro'zasi")
            hijri.month == 12 && hijri.day == 9 -> language.pick("صيام يوم عرفة", "Day of Arafah Fast", "Arafa kuni ro'zasi")
            hijri.month == 1 && (hijri.day == 9 || hijri.day == 10) -> language.pick("صيام تاسوعاء / عاشوراء", "Tasu'a / Ashura Fast", "Tasu'a / Ashuro ro'zasi")
            isWhiteDay(hijri.day) -> language.pick("الأيام البيض (١٣-١٥)", "White Days (Ayyam al-Bid)", "Oydin kunlar (13-15)")
            dow == Calendar.MONDAY -> language.pick("صيام سنة الإثنين", "Monday Sunnah Fast", "Dushanba sunnat ro'zasi")
            dow == Calendar.THURSDAY -> language.pick("صيام سنة الخميس", "Thursday Sunnah Fast", "Payshanba sunnat ro'zasi")
            else -> null
        }
    }

    fun upcomingOccasions(
        todayHijri: HijriDate,
        language: AppLanguage = AppLanguage.ENGLISH
    ): List<IslamicOccasion> {
        data class OccasionSpec(
            val titleAr: String,
            val titleEn: String,
            val titleUz: String,
            val month: Int,
            val day: Int,
            val noteAr: String,
            val noteEn: String,
            val noteUz: String
        )
        val milestones = listOf(
            OccasionSpec(
                "رأس السنة الهجرية (١ محرم)", "Islamic New Year (1 Muharram)", "Hijriy Yangi yil (1-Muharram)",
                1, 1,
                "بداية العام الهجري واستحضار دروس الهجرة النبوية.",
                "Beginning of the Hijri year; remember the Prophet's migration.",
                "Hijriy yilning boshlanishi va Payg'ambarimiz hijratlaridan ibrat."
            ),
            OccasionSpec(
                "يوم عاشوراء (١٠ محرم)", "Day of Ashura (10 Muharram)", "Ashuro kuni (10-Muharram)",
                1, 10,
                "صيام عاشوراء يكفر ذنوب السنة الماضية.",
                "Fasting Ashura expiates the sins of the previous year.",
                "Ashuro ro'zasi o'tgan bir yillik gunohlarga kafforat bo'ladi."
            ),
            OccasionSpec(
                "غرة رجب (١ رجب)", "1st of Rajab", "Rajab oyining boshlanishi",
                7, 1,
                "من الأشهر الحرم؛ موسم للتوبة والإكثار من الطاعات.",
                "One of the four sacred months; increase repentance & good deeds.",
                "Harom oylardan biri; tavba va solih amallarni ko'paytirish mavsumi."
            ),
            OccasionSpec(
                "النصف من شعبان (١٥ شعبان)", "Mid-Sha'ban (15 Sha'ban)", "Baroat kechasi (15-Sha'bon)",
                8, 15,
                "تهيئة القلوب لرمضان وقضاء ما فات من الصيام.",
                "Prepare hearts and make up any missed Ramadan fasts.",
                "Qalblarni Ramazonga tayyorlash va qazo ro'zalarni tutish."
            ),
            OccasionSpec(
                "غرة شهر رمضان (١ رمضان)", "Start of Ramadan (1 Ramadan)", "Ramazon oyi kirishi (1-Ramazon)",
                9, 1,
                "شهر القرآن والصيام والقيام والعتق من النيران.",
                "Month of the Quran, fasting, Taraweeh, and mercy.",
                "Qur'on, ro'za, tarovih va rahmat oyi."
            ),
            OccasionSpec(
                "العشر الأواخر وليلة القدر", "Last 10 Nights & Laylat al-Qadr", "Oxirgi o'n kunlik va Qadr kechasi",
                9, 21,
                "تحرّ ليلة القدر في الأوتار؛ خير من ألف شهر.",
                "Seek Laylat al-Qadr in the odd nights; better than 1,000 months.",
                "Toq kechalarda Qadr kechasini izlang; ming oydan yaxshiroqdir."
            ),
            OccasionSpec(
                "عيد الفطر المبارك (١ شوال)", "Eid al-Fitr (1 Shawwal)", "Ramazon hayiti (1-Shavvol)",
                10, 1,
                "يوم الشكر والتكبير وإخراج زكاة الفطر قبل الصلاة.",
                "Day of gratitude, Takbirat, and Zakat al-Fitr before prayer.",
                "Shukrona, takbirlar va hayit namozidan oldin fitr sadaqasi kuni."
            ),
            OccasionSpec(
                "أوائل ذي الحجة (١ ذو الحجة)", "First 10 Days of Dhul-Hijjah", "Zulhijjaning dastlabki 10 kuni",
                12, 1,
                "أفضل أيام الدنيا وأحبها إلى الله للعمل الصالح.",
                "No days are more beloved to Allah for righteous deeds.",
                "Solih amallar Allohga eng mahbub bo'lgan muborak kunlar."
            ),
            OccasionSpec(
                "يوم عرفة (٩ ذو الحجة)", "Day of Arafah (9 Dhul-Hijjah)", "Arafa kuni (9-Zulhijja)",
                12, 9,
                "صيامه يكفّر سنتين، وأعظم أيام الدعاء والعتق.",
                "Fasting expiates two years of sins; greatest day of Dua.",
                "Ro'zasi ikki yillik gunohlarga kafforat, duo qabul bo'ladigan eng ulug' kun."
            ),
            OccasionSpec(
                "عيد الأضحى المبارك (١٠ ذو الحجة)", "Eid al-Adha (10 Dhul-Hijjah)", "Qurbon hayiti (10-Zulhijja)",
                12, 10,
                "يوم النحر والتكبير والبهجة وصلة الأرحام.",
                "Day of Sacrifice (Udhiyah), joy, and Takbirat.",
                "Qurbonlik, takbir, quvonch va silai rahm kuni."
            )
        )

        val todayApproxDayOfYear = (todayHijri.month - 1) * 29.5 + todayHijri.day
        return milestones.map { spec ->
            val targetDayOfYear = (spec.month - 1) * 29.5 + spec.day
            var diff = floor(targetDayOfYear - todayApproxDayOfYear).toInt()
            if (diff < 0) diff += 354
            IslamicOccasion(
                title = language.pick(spec.titleAr, spec.titleEn, spec.titleUz),
                hijriMonth = spec.month,
                hijriDay = spec.day,
                virtueNote = language.pick(spec.noteAr, spec.noteEn, spec.noteUz),
                daysRemaining = diff
            )
        }.sortedBy { it.daysRemaining }
    }
}
