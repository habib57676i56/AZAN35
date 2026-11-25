package com.example.domain

import com.example.core.i18n.AppLanguage
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

enum class CalculationMethod(
    val id: String,
    val displayName: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMin: Int? = null
) {
    MWL("MWL", "Muslim World League", 18.0, 17.0),
    ISNA("ISNA", "ISNA (North America)", 15.0, 15.0),
    UMM_AL_QURA("UMM_AL_QURA", "Umm al-Qura (Makkah)", 18.5, 0.0, 90),
    EGYPTIAN("EGYPTIAN", "Egyptian General Authority", 19.5, 17.5),
    KARACHI("KARACHI", "Univ. of Islamic Sciences, Karachi", 18.0, 18.0);

    companion object {
        fun fromId(id: String): CalculationMethod =
            entries.firstOrNull { it.id == id } ?: MWL
    }
}

data class CityPreset(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val defaultMethod: CalculationMethod
)

data class PrayerTimeEntry(
    val key: String,
    val nameEn: String,
    val nameAr: String,
    val nameUz: String,
    val timeMinutes: Int, // minutes from midnight [0..1439]
    val formattedTime: String,
    val sunnahInfo: String,
    val sunnahInfoAr: String,
    val sunnahInfoUz: String
) {
    fun nameFor(language: AppLanguage): String = language.pick(nameAr, nameEn, nameUz)
    fun sunnahInfoFor(language: AppLanguage): String = language.pick(sunnahInfoAr, sunnahInfo, sunnahInfoUz)
}

data class DailyPrayerSchedule(
    val imsak: PrayerTimeEntry,
    val fajr: PrayerTimeEntry,
    val sunrise: PrayerTimeEntry,
    val dhuhr: PrayerTimeEntry,
    val asr: PrayerTimeEntry,
    val maghrib: PrayerTimeEntry,
    val isha: PrayerTimeEntry,
    val nightMidnight: String,
    val lastThirdQiyam: String,
    val qiblaBearingDegrees: Double,
    val distanceToKaabaKm: Int,
    val nightMidnightMinutes: Int = 0,
    val lastThirdQiyamMinutes: Int = 0
)

data class MosquePlace(
    val name: String,
    val distanceLabel: String,
    val iqamahFajr: String,
    val iqamahDhuhr: String,
    val iqamahAsr: String,
    val iqamahMaghrib: String,
    val iqamahIsha: String,
    val jummahKhutbah: String,
    val facilities: List<String>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isRealOsmData: Boolean = false,
    val address: String = ""
)

object PrayerTimesEngine {

    private const val KAABA_LAT = 21.422487
    private const val KAABA_LNG = 39.826206

    val cityPresets: List<CityPreset> = listOf(
        CityPreset("Makkah", "Saudi Arabia", 21.4225, 39.8262, CalculationMethod.UMM_AL_QURA),
        CityPreset("Madinah", "Saudi Arabia", 24.4672, 39.6112, CalculationMethod.UMM_AL_QURA),
        CityPreset("Tashkent", "Uzbekistan", 41.2995, 69.2401, CalculationMethod.MWL),
        CityPreset("Samarkand", "Uzbekistan", 39.6542, 66.9597, CalculationMethod.MWL),
        CityPreset("Istanbul", "Türkiye", 41.0082, 28.9784, CalculationMethod.MWL),
        CityPreset("Cairo", "Egypt", 30.0444, 31.2357, CalculationMethod.EGYPTIAN),
        CityPreset("Dubai", "UAE", 25.2048, 55.2708, CalculationMethod.UMM_AL_QURA),
        CityPreset("London", "United Kingdom", 51.5074, -0.1278, CalculationMethod.MWL),
        CityPreset("New York", "United States", 40.7128, -74.0060, CalculationMethod.ISNA),
        CityPreset("Los Angeles", "United States", 34.0522, -118.2437, CalculationMethod.ISNA),
        CityPreset("Karachi", "Pakistan", 24.8607, 67.0011, CalculationMethod.KARACHI),
        CityPreset("Jakarta", "Indonesia", -6.2088, 106.8456, CalculationMethod.MWL),
        CityPreset("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, CalculationMethod.MWL),
        CityPreset("Toronto", "Canada", 43.6532, -79.3832, CalculationMethod.ISNA)
    )

    fun computeQiblaBearing(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lng1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lng2 = Math.toRadians(KAABA_LNG)
        val dLng = lng2 - lng1
        val y = sin(dLng)
        val x = cos(lat1) * tan(lat2) - sin(lat1) * cos(dLng)
        val deg = Math.toDegrees(atan2(y, x))
        return (deg + 360.0) % 360.0
    }

    fun computeDistanceToKaabaKm(latitude: Double, longitude: Double): Int {
        val r = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLng = Math.toRadians(KAABA_LNG - longitude)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).roundToInt()
    }

    fun calculateForDate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        travelerMode: Boolean = false,
        manualOffsetsMin: Map<String, Int> = emptyMap()
    ): DailyPrayerSchedule {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val tzOffsetHours = TimeZone.getDefault().getOffset(calendar.timeInMillis) / 3600000.0

        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val d = jd - 2451545.0

        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d

        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = darcsin(dsin(e) * dsin(l))

        val noon = fixHour(12.0 + tzOffsetHours - longitude / 15.0 - eqt)

        val sunriseDiff = sunAngleHourDiff(0.833, latitude, decl)
        val fajrDiff = sunAngleHourDiff(method.fajrAngle, latitude, decl)
        val asrDiff = asrAngleHourDiff(1.0, latitude, decl)
        val maghribDiff = sunriseDiff

        val fajrHours = (noon - fajrDiff)
        val sunriseHours = (noon - sunriseDiff)
        val dhuhrHours = noon + 0.02
        val asrHours = (noon + asrDiff)
        val maghribHours = (noon + maghribDiff)
        val ishaHours = if (method.ishaIntervalMin != null) {
            maghribHours + method.ishaIntervalMin / 60.0
        } else {
            noon + sunAngleHourDiff(method.ishaAngle, latitude, decl)
        }

        val fajrMin = applyOffset(hoursToMinutes(fajrHours), manualOffsetsMin["fajr"] ?: 0)
        val imsakMin = (fajrMin - 10 + 1440) % 1440
        val sunriseMin = applyOffset(hoursToMinutes(sunriseHours), 0)
        val dhuhrMin = applyOffset(hoursToMinutes(dhuhrHours), manualOffsetsMin["dhuhr"] ?: 0)
        val asrMin = applyOffset(hoursToMinutes(asrHours), manualOffsetsMin["asr"] ?: 0)
        val maghribMin = applyOffset(hoursToMinutes(maghribHours), manualOffsetsMin["maghrib"] ?: 0)
        val ishaMin = applyOffset(hoursToMinutes(ishaHours), manualOffsetsMin["isha"] ?: 0)

        val nightDuration = ((fajrMin - maghribMin) + 1440) % 1440
        val midnightMin = (maghribMin + nightDuration / 2) % 1440
        val lastThirdMin = (fajrMin - nightDuration / 3 + 1440) % 1440

        return DailyPrayerSchedule(
            imsak = PrayerTimeEntry(
                key = "imsak",
                nameEn = "Imsak",
                nameAr = "الإمساك",
                nameUz = "Imsok",
                timeMinutes = imsakMin,
                formattedTime = formatMinutes(imsakMin),
                sunnahInfo = "Pause eating ~10 min before Fajr",
                sunnahInfoAr = "يُستحب الإمساك قبل الفجر بنحو ١٠ دقائق",
                sunnahInfoUz = "Bomdoddan ~10 daqiqa oldin saharlikni yakunlash"
            ),
            fajr = PrayerTimeEntry(
                key = "fajr",
                nameEn = "Fajr",
                nameAr = "الفجر",
                nameUz = "Bomdod",
                timeMinutes = fajrMin,
                formattedTime = formatMinutes(fajrMin),
                sunnahInfo = "2 Sunnah before + 2 Fard",
                sunnahInfoAr = "ركعتان سنة قبلية + ركعتان فرض",
                sunnahInfoUz = "2 rakat sunnat + 2 rakat farz"
            ),
            sunrise = PrayerTimeEntry(
                key = "sunrise",
                nameEn = "Sunrise",
                nameAr = "الشروق",
                nameUz = "Quyosh",
                timeMinutes = sunriseMin,
                formattedTime = formatMinutes(sunriseMin),
                sunnahInfo = "Duha begins ~15 min after",
                sunnahInfoAr = "تبدأ صلاة الضحى بعده بنحو ١٥ دقيقة",
                sunnahInfoUz = "Quyosh chiqqandan 15 daqiqa o'tgach Zuho vaqti kiradi"
            ),
            dhuhr = PrayerTimeEntry(
                key = "dhuhr",
                nameEn = if (travelerMode) "Dhuhr (Qasr: 2)" else "Dhuhr",
                nameAr = if (travelerMode) "الظهر (قصر: ٢)" else "الظهر",
                nameUz = if (travelerMode) "Peshin (Qasr: 2)" else "Peshin",
                timeMinutes = dhuhrMin,
                formattedTime = formatMinutes(dhuhrMin),
                sunnahInfo = if (travelerMode) "2 Fard (May combine w/ Asr)" else "4 Sunnah before + 4 Fard + 2 after",
                sunnahInfoAr = if (travelerMode) "ركعتان قصراً (يجوز الجمع مع العصر)" else "٤ سنة قبلية + ٤ فرض + ٢ بعدية",
                sunnahInfoUz = if (travelerMode) "2 rakat qasr (Asr bilan jamlash mumkin)" else "4 sunnat + 4 farz + 2 sunnat"
            ),
            asr = PrayerTimeEntry(
                key = "asr",
                nameEn = if (travelerMode) "Asr (Qasr: 2)" else "Asr",
                nameAr = if (travelerMode) "العصر (قصر: ٢)" else "العصر",
                nameUz = if (travelerMode) "Asr (Qasr: 2)" else "Asr",
                timeMinutes = asrMin,
                formattedTime = formatMinutes(asrMin),
                sunnahInfo = if (travelerMode) "2 Fard (Shortened)" else "4 Fard (4 Sunnah before recommended)",
                sunnahInfoAr = if (travelerMode) "ركعتان قصراً" else "٤ فرض (ويُستحب ٤ سنة قبلها)",
                sunnahInfoUz = if (travelerMode) "2 rakat qasr" else "4 rakat farz (oldin 4 sunnat mustahab)"
            ),
            maghrib = PrayerTimeEntry(
                key = "maghrib",
                nameEn = "Maghrib",
                nameAr = "المغرب",
                nameUz = "Shom",
                timeMinutes = maghribMin,
                formattedTime = formatMinutes(maghribMin),
                sunnahInfo = "3 Fard + 2 Sunnah after",
                sunnahInfoAr = "٣ فرض + ٢ سنة بعدية",
                sunnahInfoUz = "3 rakat farz + 2 rakat sunnat"
            ),
            isha = PrayerTimeEntry(
                key = "isha",
                nameEn = if (travelerMode) "Isha (Qasr: 2)" else "Isha",
                nameAr = if (travelerMode) "العشاء (قصر: ٢)" else "العشاء",
                nameUz = if (travelerMode) "Xufton (Qasr: 2)" else "Xufton",
                timeMinutes = ishaMin,
                formattedTime = formatMinutes(ishaMin),
                sunnahInfo = if (travelerMode) "2 Fard + Witr" else "4 Fard + 2 Sunnah + Witr",
                sunnahInfoAr = if (travelerMode) "ركعتان قصراً + الوتر" else "٤ فرض + ٢ سنة + الوتر",
                sunnahInfoUz = if (travelerMode) "2 rakat qasr + Vitr" else "4 farz + 2 sunnat + Vitr"
            ),
            nightMidnight = formatMinutes(midnightMin),
            lastThirdQiyam = formatMinutes(lastThirdMin),
            qiblaBearingDegrees = computeQiblaBearing(latitude, longitude),
            distanceToKaabaKm = computeDistanceToKaabaKm(latitude, longitude),
            nightMidnightMinutes = midnightMin,
            lastThirdQiyamMinutes = lastThirdMin
        )
    }

    fun getMosquesForCity(
        city: String,
        schedule: DailyPrayerSchedule,
        language: AppLanguage = AppLanguage.ARABIC
    ): List<MosquePlace> {
        fun addMin(min: Int, delta: Int) = formatMinutes((min + delta) % 1440)
        return listOf(
            MosquePlace(
                name = language.pick(
                    "مسجد النور ($city)",
                    "Masjid Al-Noor ($city Central)",
                    "An-Nur masjidi ($city markazi)"
                ),
                distanceLabel = language.pick("٠.٦ كم (تقديري)", "0.6 km (est.)", "0.6 km (taxminiy)"),
                iqamahFajr = addMin(schedule.fajr.timeMinutes, 20),
                iqamahDhuhr = addMin(schedule.dhuhr.timeMinutes, 15),
                iqamahAsr = addMin(schedule.asr.timeMinutes, 15),
                iqamahMaghrib = addMin(schedule.maghrib.timeMinutes, 7),
                iqamahIsha = addMin(schedule.isha.timeMinutes, 15),
                jummahKhutbah = "1:15 PM",
                facilities = listOf(
                    language.pick("مواضئ", "Wudu Area", "Tahoratxona"),
                    language.pick("مصلى نساء", "Women's Section", "Ayollar xonasi"),
                    language.pick("موقف سيارات", "Parking", "Avtoturargoh")
                )
            ),
            MosquePlace(
                name = language.pick(
                    "المركز الإسلامي الجامع ($city)",
                    "Islamic Heritage Center ($city)",
                    "Markaziy Jome Masjidi ($city)"
                ),
                distanceLabel = language.pick("١.٤ كم (تقديري)", "1.4 km (est.)", "1.4 km (taxminiy)"),
                iqamahFajr = addMin(schedule.fajr.timeMinutes, 25),
                iqamahDhuhr = addMin(schedule.dhuhr.timeMinutes, 20),
                iqamahAsr = addMin(schedule.asr.timeMinutes, 20),
                iqamahMaghrib = addMin(schedule.maghrib.timeMinutes, 5),
                iqamahIsha = addMin(schedule.isha.timeMinutes, 20),
                jummahKhutbah = "1:30 PM",
                facilities = listOf(
                    language.pick("مواضئ", "Wudu Area", "Tahoratxona"),
                    language.pick("حلقات تحفيظ", "Quran Halaqah", "Qur'on darslari")
                )
            )
        )
    }

    private fun applyOffset(minutes: Int, offset: Int): Int = ((minutes + offset) % 1440 + 1440) % 1440

    private fun hoursToMinutes(hours: Double): Int {
        val h = fixHour(hours)
        return (h * 60.0).roundToInt() % 1440
    }

    fun formatMinutes(totalMin: Int): String {
        val norm = ((totalMin % 1440) + 1440) % 1440
        val h24 = norm / 60
        val m = norm % 60
        val amPm = if (h24 < 12) "AM" else "PM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return String.format("%d:%02d %s", h12, m, amPm)
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunAngleHourDiff(angle: Double, lat: Double, decl: Double): Double {
        val cosH = (-dsin(angle) - dsin(lat) * dsin(decl)) / (dcos(lat) * dcos(decl))
        val clamped = cosH.coerceIn(-0.999, 0.999)
        return darccos(clamped) / 15.0
    }

    private fun asrAngleHourDiff(factor: Double, lat: Double, decl: Double): Double {
        val angle = -darccot(factor + dtan(abs(lat - decl)))
        val cosH = (dsin(angle) - dsin(lat) * dsin(decl)) / (dcos(lat) * dcos(decl))
        val clamped = cosH.coerceIn(-0.999, 0.999)
        return darccos(clamped) / 15.0
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun darccos(x: Double) = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))
    private fun fixAngle(a: Double): Double = (a % 360.0 + 360.0) % 360.0
    private fun fixHour(h: Double): Double = (h % 24.0 + 24.0) % 24.0
}
