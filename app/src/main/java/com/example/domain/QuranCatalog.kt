package com.example.domain

data class SurahMeta(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val ayahCount: Int,
    val startPage: Int,
    val isMeccan: Boolean
)

data class MushafWordToken(
    val textUthmani: String,
    val lineNumber: Int,
    val isEndOfAyah: Boolean = false
)

data class MushafAyahItem(
    val globalNumber: Int,
    val surahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val numberInSurah: Int,
    val textUthmani: String,
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbQuarter: Int,
    val sajdah: Boolean,
    val words: List<MushafWordToken> = emptyList()
)

data class MushafPageModel(
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbQuarter: Int,
    val ayahs: List<MushafAyahItem>,
    val isOfflineFallback: Boolean = false
) {
    val hizbNumber: Int get() = ((hizbQuarter - 1) / 4) + 1
    val rubInHizb: Int get() = ((hizbQuarter - 1) % 4) + 1
}

object QuranCatalog {
    val juzStartPages: List<Int> = listOf(
        1, 22, 42, 62, 82, 102, 121, 142, 162, 182,
        201, 222, 242, 262, 282, 302, 322, 342, 362, 382,
        402, 422, 442, 462, 482, 502, 522, 542, 562, 582
    )

    fun juzForPage(page: Int): Int {
        val idx = juzStartPages.indexOfLast { it <= page }
        return if (idx >= 0) idx + 1 else 1
    }

    val surahs: List<SurahMeta> = listOf(
        SurahMeta(1, "الفاتحة", "Al-Fatihah", 7, 1, true),
        SurahMeta(2, "البقرة", "Al-Baqarah", 286, 2, false),
        SurahMeta(3, "آل عمران", "Aal-i-Imran", 200, 50, false),
        SurahMeta(4, "النساء", "An-Nisa", 176, 77, false),
        SurahMeta(5, "المائدة", "Al-Ma'idah", 120, 106, false),
        SurahMeta(6, "الأنعام", "Al-An'am", 165, 128, true),
        SurahMeta(7, "الأعراف", "Al-A'raf", 206, 151, true),
        SurahMeta(8, "الأنفال", "Al-Anfal", 75, 177, false),
        SurahMeta(9, "التوبة", "At-Tawbah", 129, 187, false),
        SurahMeta(10, "يونس", "Yunus", 109, 208, true),
        SurahMeta(11, "هود", "Hud", 123, 221, true),
        SurahMeta(12, "يوسف", "Yusuf", 111, 235, true),
        SurahMeta(13, "الرعد", "Ar-Ra'd", 43, 249, false),
        SurahMeta(14, "إبراهيم", "Ibrahim", 52, 255, true),
        SurahMeta(15, "الحجر", "Al-Hijr", 99, 262, true),
        SurahMeta(16, "النحل", "An-Nahl", 128, 267, true),
        SurahMeta(17, "الإسراء", "Al-Isra", 111, 282, true),
        SurahMeta(18, "الكهف", "Al-Kahf", 110, 293, true),
        SurahMeta(19, "مريم", "Maryam", 98, 305, true),
        SurahMeta(20, "طه", "Taha", 135, 312, true),
        SurahMeta(21, "الأنبياء", "Al-Anbiya", 112, 322, true),
        SurahMeta(22, "الحج", "Al-Hajj", 78, 332, false),
        SurahMeta(23, "المؤمنون", "Al-Mu'minun", 118, 342, true),
        SurahMeta(24, "النور", "An-Nur", 64, 350, false),
        SurahMeta(25, "الفرقان", "Al-Furqan", 77, 359, true),
        SurahMeta(26, "الشعراء", "Ash-Shu'ara", 227, 367, true),
        SurahMeta(27, "النمل", "An-Naml", 93, 377, true),
        SurahMeta(28, "القصص", "Al-Qasas", 88, 385, true),
        SurahMeta(29, "العنكبوت", "Al-Ankabut", 69, 396, true),
        SurahMeta(30, "الروم", "Ar-Rum", 60, 404, true),
        SurahMeta(31, "لقمان", "Luqman", 34, 411, true),
        SurahMeta(32, "السجدة", "As-Sajdah", 30, 415, true),
        SurahMeta(33, "الأحزاب", "Al-Ahzab", 73, 418, false),
        SurahMeta(34, "سبأ", "Saba", 54, 428, true),
        SurahMeta(35, "فاطر", "Fatir", 45, 434, true),
        SurahMeta(36, "يس", "Ya-Sin", 83, 440, true),
        SurahMeta(37, "الصافات", "As-Saffat", 182, 446, true),
        SurahMeta(38, "ص", "Sad", 88, 453, true),
        SurahMeta(39, "الزمر", "Az-Zumar", 75, 458, true),
        SurahMeta(40, "غافر", "Ghafir", 85, 467, true),
        SurahMeta(41, "فصلت", "Fussilat", 54, 477, true),
        SurahMeta(42, "الشورى", "Ash-Shura", 53, 483, true),
        SurahMeta(43, "الزخرف", "Az-Zukhruf", 89, 489, true),
        SurahMeta(44, "الدخان", "Ad-Dukhan", 59, 496, true),
        SurahMeta(45, "الجاثية", "Al-Jathiyah", 37, 499, true),
        SurahMeta(46, "الأحقاف", "Al-Ahqaf", 35, 502, true),
        SurahMeta(47, "محمد", "Muhammad", 38, 507, false),
        SurahMeta(48, "الفتح", "Al-Fath", 29, 511, false),
        SurahMeta(49, "الحجرات", "Al-Hujurat", 18, 515, false),
        SurahMeta(50, "ق", "Qaf", 45, 518, true),
        SurahMeta(51, "الذاريات", "Adh-Dhariyat", 60, 520, true),
        SurahMeta(52, "الطور", "At-Tur", 49, 523, true),
        SurahMeta(53, "النجم", "An-Najm", 62, 526, true),
        SurahMeta(54, "القمر", "Al-Qamar", 55, 528, true),
        SurahMeta(55, "الرحمن", "Ar-Rahman", 78, 531, false),
        SurahMeta(56, "الواقعة", "Al-Waqi'ah", 96, 534, true),
        SurahMeta(57, "الحديد", "Al-Hadid", 29, 537, false),
        SurahMeta(58, "المجادلة", "Al-Mujadila", 22, 542, false),
        SurahMeta(59, "الحشر", "Al-Hashr", 24, 545, false),
        SurahMeta(60, "الممتحنة", "Al-Mumtahanah", 13, 549, false),
        SurahMeta(61, "الصف", "As-Saff", 14, 551, false),
        SurahMeta(62, "الجمعة", "Al-Jumu'ah", 11, 553, false),
        SurahMeta(63, "المنافقون", "Al-Munafiqun", 11, 554, false),
        SurahMeta(64, "التغابن", "At-Taghabun", 18, 556, false),
        SurahMeta(65, "الطلاق", "At-Talaq", 12, 558, false),
        SurahMeta(66, "التحريم", "At-Tahrim", 12, 560, false),
        SurahMeta(67, "الملك", "Al-Mulk", 30, 562, true),
        SurahMeta(68, "القلم", "Al-Qalam", 52, 564, true),
        SurahMeta(69, "الحاقة", "Al-Haqqah", 52, 566, true),
        SurahMeta(70, "المعارج", "Al-Ma'arij", 44, 568, true),
        SurahMeta(71, "نوح", "Nuh", 28, 570, true),
        SurahMeta(72, "الجن", "Al-Jinn", 28, 572, true),
        SurahMeta(73, "المزمل", "Al-Muzzammil", 20, 574, true),
        SurahMeta(74, "المدثر", "Al-Muddaththir", 56, 575, true),
        SurahMeta(75, "القيامة", "Al-Qiyamah", 40, 577, true),
        SurahMeta(76, "الإنسان", "Al-Insan", 31, 578, false),
        SurahMeta(77, "المرسلات", "Al-Mursalat", 50, 580, true),
        SurahMeta(78, "النبأ", "An-Naba", 40, 582, true),
        SurahMeta(79, "النازعات", "An-Nazi'at", 46, 583, true),
        SurahMeta(80, "عبس", "Abasa", 42, 585, true),
        SurahMeta(81, "التكوير", "At-Takwir", 29, 586, true),
        SurahMeta(82, "الانفطار", "Al-Infitar", 19, 587, true),
        SurahMeta(83, "المطففين", "Al-Mutaffifin", 36, 587, true),
        SurahMeta(84, "الانشقاق", "Al-Inshiqaq", 25, 589, true),
        SurahMeta(85, "البروج", "Al-Buruj", 22, 590, true),
        SurahMeta(86, "الطارق", "At-Tariq", 17, 591, true),
        SurahMeta(87, "الأعلى", "Al-A'la", 19, 591, true),
        SurahMeta(88, "الغاشية", "Al-Ghashiyah", 26, 592, true),
        SurahMeta(89, "الفجر", "Al-Fajr", 30, 593, true),
        SurahMeta(90, "البلد", "Al-Balad", 20, 594, true),
        SurahMeta(91, "الشمس", "Ash-Shams", 15, 595, true),
        SurahMeta(92, "الليل", "Al-Layl", 21, 595, true),
        SurahMeta(93, "الضحى", "Ad-Duha", 11, 596, true),
        SurahMeta(94, "الشرح", "Ash-Sharh", 8, 596, true),
        SurahMeta(95, "التين", "At-Tin", 8, 597, true),
        SurahMeta(96, "العلق", "Al-Alaq", 19, 597, true),
        SurahMeta(97, "القدر", "Al-Qadr", 5, 598, true),
        SurahMeta(98, "البينة", "Al-Bayyinah", 8, 598, false),
        SurahMeta(99, "الزلزلة", "Az-Zalzalah", 8, 599, false),
        SurahMeta(100, "العاديات", "Al-Adiyat", 11, 599, true),
        SurahMeta(101, "القارعة", "Al-Qari'ah", 11, 600, true),
        SurahMeta(102, "التكاثر", "At-Takathur", 8, 600, true),
        SurahMeta(103, "العصر", "Al-Asr", 3, 601, true),
        SurahMeta(104, "الهمزة", "Al-Humazah", 9, 601, true),
        SurahMeta(105, "الفيل", "Al-Fil", 5, 601, true),
        SurahMeta(106, "قريش", "Quraysh", 4, 602, true),
        SurahMeta(107, "الماعون", "Al-Ma'un", 7, 602, true),
        SurahMeta(108, "الكوثر", "Al-Kawthar", 3, 602, true),
        SurahMeta(109, "الكافرون", "Al-Kafirun", 6, 603, true),
        SurahMeta(110, "النصر", "An-Nasr", 3, 603, false),
        SurahMeta(111, "المسد", "Al-Masad", 5, 603, true),
        SurahMeta(112, "الإخلاص", "Al-Ikhlas", 4, 604, true),
        SurahMeta(113, "الفلق", "Al-Falaq", 5, 604, true),
        SurahMeta(114, "الناس", "An-Nas", 6, 604, true)
    )

    fun surahByNumber(number: Int): SurahMeta =
        surahs.getOrElse((number - 1).coerceIn(0, surahs.lastIndex)) { surahs.first() }

    fun globalAyahNumber(surahNumber: Int, ayahNumber: Int): Int {
        var offset = 0
        for (s in surahs) {
            if (s.number < surahNumber) offset += s.ayahCount else break
        }
        return offset + ayahNumber.coerceAtLeast(1)
    }

    fun surahAndAyahFromGlobal(globalNumber: Int): Pair<Int, Int> {
        var remaining = globalNumber.coerceIn(1, 6236)
        for (s in surahs) {
            if (remaining <= s.ayahCount) return s.number to remaining
            remaining -= s.ayahCount
        }
        return 114 to 6
    }

    fun offlineStarterPage(pageNumber: Int): MushafPageModel? = when (pageNumber) {
        1 -> MushafPageModel(
            pageNumber = 1,
            juzNumber = 1,
            hizbQuarter = 1,
            isOfflineFallback = true,
            ayahs = listOf(
                MushafAyahItem(1, 1, "الفاتحة", "Al-Fatihah", 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", 1, 1, 1, false),
                MushafAyahItem(2, 1, "الفاتحة", "Al-Fatihah", 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", 1, 1, 1, false),
                MushafAyahItem(3, 1, "الفاتحة", "Al-Fatihah", 3, "الرَّحْمَٰنِ الرَّحِيمِ", 1, 1, 1, false),
                MushafAyahItem(4, 1, "الفاتحة", "Al-Fatihah", 4, "مَالِكِ يَوْمِ الدِّينِ", 1, 1, 1, false),
                MushafAyahItem(5, 1, "الفاتحة", "Al-Fatihah", 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", 1, 1, 1, false),
                MushafAyahItem(6, 1, "الفاتحة", "Al-Fatihah", 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", 1, 1, 1, false),
                MushafAyahItem(7, 1, "الفاتحة", "Al-Fatihah", 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", 1, 1, 1, false)
            )
        )
        2 -> MushafPageModel(
            pageNumber = 2,
            juzNumber = 1,
            hizbQuarter = 1,
            isOfflineFallback = true,
            ayahs = listOf(
                MushafAyahItem(8, 2, "البقرة", "Al-Baqarah", 1, "الم", 2, 1, 1, false),
                MushafAyahItem(9, 2, "البقرة", "Al-Baqarah", 2, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ", 2, 1, 1, false),
                MushafAyahItem(10, 2, "البقرة", "Al-Baqarah", 3, "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ وَمِمَّا رَزَقْنَاهُمْ يُنفِقُونَ", 2, 1, 1, false),
                MushafAyahItem(11, 2, "البقرة", "Al-Baqarah", 4, "وَالَّذِينَ يُؤْمِنُونَ بِمَا أُنزِلَ إِلَيْكَ وَمَا أُنزِلَ مِن قَبْلِكَ وَبِالْآخِرَةِ هُمْ يُوقِنُونَ", 2, 1, 1, false),
                MushafAyahItem(12, 2, "البقرة", "Al-Baqarah", 5, "أُولَٰئِكَ عَلَىٰ هُدًى مِّن رَّبِّهِمْ ۖ وَأُولَٰئِكَ هُمُ الْمُفْلِحُونَ", 2, 1, 1, false)
            )
        )
        604 -> MushafPageModel(
            pageNumber = 604,
            juzNumber = 30,
            hizbQuarter = 240,
            isOfflineFallback = true,
            ayahs = listOf(
                MushafAyahItem(6222, 112, "الإخلاص", "Al-Ikhlas", 1, "قُلْ هُوَ اللَّهُ أَحَدٌ", 604, 30, 240, false),
                MushafAyahItem(6223, 112, "الإخلاص", "Al-Ikhlas", 2, "اللَّهُ الصَّمَدُ", 604, 30, 240, false),
                MushafAyahItem(6224, 112, "الإخلاص", "Al-Ikhlas", 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", 604, 30, 240, false),
                MushafAyahItem(6225, 112, "الإخلاص", "Al-Ikhlas", 4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", 604, 30, 240, false),
                MushafAyahItem(6226, 113, "الفلق", "Al-Falaq", 1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", 604, 30, 240, false),
                MushafAyahItem(6227, 113, "الفلق", "Al-Falaq", 2, "مِن شَرِّ مَا خَلَقَ", 604, 30, 240, false),
                MushafAyahItem(6228, 113, "الفلق", "Al-Falaq", 3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", 604, 30, 240, false),
                MushafAyahItem(6229, 113, "الفلق", "Al-Falaq", 4, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", 604, 30, 240, false),
                MushafAyahItem(6230, 113, "الفلق", "Al-Falaq", 5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", 604, 30, 240, false),
                MushafAyahItem(6231, 114, "الناس", "An-Nas", 1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", 604, 30, 240, false),
                MushafAyahItem(6232, 114, "الناس", "An-Nas", 2, "مَلِكِ النَّاسِ", 604, 30, 240, false),
                MushafAyahItem(6233, 114, "الناس", "An-Nas", 3, "إِلَٰهِ النَّاسِ", 604, 30, 240, false),
                MushafAyahItem(6234, 114, "الناس", "An-Nas", 4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", 604, 30, 240, false),
                MushafAyahItem(6235, 114, "الناس", "An-Nas", 5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", 604, 30, 240, false),
                MushafAyahItem(6236, 114, "الناس", "An-Nas", 6, "مِنَ الْجِنَّةِ وَالنَّاسِ", 604, 30, 240, false)
            )
        )
        else -> null
    }
}
