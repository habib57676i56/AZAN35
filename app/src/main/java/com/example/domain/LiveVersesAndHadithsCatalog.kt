package com.example.domain

import com.example.core.i18n.AppLanguage

data class LiveSpiritualQuote(
    val id: String,
    val isQuran: Boolean,
    val arabicText: String,
    val meaningAr: String,
    val meaningEn: String,
    val meaningUz: String,
    val sourceAr: String,
    val sourceEn: String,
    val sourceUz: String
) {
    fun meaningFor(language: AppLanguage): String = language.pick(meaningAr, meaningEn, meaningUz)
    fun sourceFor(language: AppLanguage): String = language.pick(sourceAr, sourceEn, sourceUz)
}

object LiveVersesAndHadithsCatalog {
    val entries: List<LiveSpiritualQuote> = listOf(
        LiveSpiritualQuote(
            id = "q_1",
            isQuran = true,
            arabicText = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            meaningAr = "بذكر الله وتلاوة كتابه تنشرح الصدور وتستقر النفوس.",
            meaningEn = "Unquestionably, by the remembrance of Allah hearts are assured.",
            meaningUz = "Ogoh bo'lingizkim, Allohni zikr qilish bilan qalblar orom olur!",
            sourceAr = "سورة الرعد • الآية ٢٨",
            sourceEn = "Surah Ar-Ra'd • 13:28",
            sourceUz = "Ra'd surasi • 28-oyat"
        ),
        LiveSpiritualQuote(
            id = "h_1",
            isQuran = false,
            arabicText = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
            meaningAr = "أفضل الناس قدراً ومنزلةً من يتعلم القرآن الكريم ويعلمه للناس.",
            meaningEn = "The best among you are those who learn the Quran and teach it.",
            meaningUz = "Sizlarning eng yaxshingiz Qur'onni o'rgangan va uni o'rgatgan kishidir.",
            sourceAr = "صحيح البخاري • ٥٠٢٧",
            sourceEn = "Sahih al-Bukhari • 5027",
            sourceUz = "Sahih al-Buxoriy • 5027"
        ),
        LiveSpiritualQuote(
            id = "q_2",
            isQuran = true,
            arabicText = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            meaningAr = "من ذكر الله في قلبه ولسانه ذكره الله في الملأ الأعلى وأجزل له العطاء.",
            meaningEn = "So remember Me; I will remember you. And be grateful to Me and do not deny Me.",
            meaningUz = "Bas, Meni eslangiz, Men ham sizni eslayman va Menga shukr qilingiz.",
            sourceAr = "سورة البقرة • الآية ١٥٢",
            sourceEn = "Surah Al-Baqarah • 2:152",
            sourceUz = "Baqara surasi • 152-oyat"
        ),
        LiveSpiritualQuote(
            id = "h_2",
            isQuran = false,
            arabicText = "مَنْ صَلَّى الْبَرْدَيْنِ دَخَلَ الْجَنَّةَ",
            meaningAr = "المحافظة على صلاتي الفجر والعصر في وقتهما سببٌ لدخول الجنة.",
            meaningEn = "Whoever prays the two cool prayers (Fajr and Asr) will enter Paradise.",
            meaningUz = "Kim ikki salqin namozni (Bomdod va Asrni) o'qisa, jannatga kiradi.",
            sourceAr = "متفق عليه • البخاري ٥٧٤",
            sourceEn = "Agreed Upon • Bukhari 574",
            sourceUz = "Muttafaqun alayh • Buxoriy 574"
        ),
        LiveSpiritualQuote(
            id = "q_3",
            isQuran = true,
            arabicText = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            meaningAr = "بشارة قرآنية بأن الفرج واليسر مقرونان دائماً بالشدة ولا يغلب عسرٌ يسرين.",
            meaningEn = "For indeed, with hardship will be ease. Indeed, with hardship will be ease.",
            meaningUz = "Bas, albatta har bir qiyinchilik bilan birga yengillik bordir.",
            sourceAr = "سورة الشرح • الآيتان ٥-٦",
            sourceEn = "Surah Ash-Sharh • 94:5-6",
            sourceUz = "Sharh surasi • 5-6-oyatlar"
        ),
        LiveSpiritualQuote(
            id = "h_3",
            isQuran = false,
            arabicText = "أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ",
            meaningAr = "المداومة على القليل من الطاعات كل يوم أحب إلى الله من الانقطاع.",
            meaningEn = "The most beloved deeds to Allah are those done consistently, even if small.",
            meaningUz = "Allohga eng mahbub amal — oz bo'lsa-da, davomiy qilinganidir.",
            sourceAr = "صحيح البخاري • ٦٤٦٤",
            sourceEn = "Sahih al-Bukhari • 6464",
            sourceUz = "Sahih al-Buxoriy • 6464"
        ),
        LiveSpiritualQuote(
            id = "q_4",
            isQuran = true,
            arabicText = "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
            meaningAr = "الله قريب سميع للدعاء يجيب عبده إذا ناداه بصدق وإخلاص.",
            meaningEn = "And when My servants ask you concerning Me — indeed I am near. I respond to the invocation of the supplicant.",
            meaningUz = "Bandalarim Mendan siz haqingizda so'rasalar, albatta Men yaqinman; duo qiluvchining duosini ijobat qilaman.",
            sourceAr = "سورة البقرة • الآية ١٨٦",
            sourceEn = "Surah Al-Baqarah • 2:186",
            sourceUz = "Baqara surasi • 186-oyat"
        ),
        LiveSpiritualQuote(
            id = "h_4",
            isQuran = false,
            arabicText = "الدُّعَاءُ هُوَ الْعِبَادَةُ",
            meaningAr = "الدعاء جوهر العبادة ودليل صدق الافتقار واللجوء إلى الله تعالى.",
            meaningEn = "Supplication (Dua) is the essence of worship.",
            meaningUz = "Duo — ibodatning mag'zi va o'zidir.",
            sourceAr = "سنن الترمذي • ٣٢٤٧ (صحيح)",
            sourceEn = "Jami' at-Tirmidhi • 3247 (Sahih)",
            sourceUz = "Sunani Termiziy • 3247 (Sahih)"
        ),
        LiveSpiritualQuote(
            id = "q_5",
            isQuran = true,
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            meaningAr = "من أجمع الأدعية القرآنية لخير الدنيا والآخرة والوقاية من النار.",
            meaningEn = "Our Lord, give us in this world good and in the Hereafter good and protect us from the punishment of the Fire.",
            meaningUz = "Ey Robbimiz, bizga bu dunyoda ham yaxshilik, oxiratda ham yaxshilik bergin va bizni do'zax azobidan saqlagin.",
            sourceAr = "سورة البقرة • الآية ٢٠١",
            sourceEn = "Surah Al-Baqarah • 2:201",
            sourceUz = "Baqara surasi • 201-oyat"
        ),
        LiveSpiritualQuote(
            id = "h_5",
            isQuran = false,
            arabicText = "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
            meaningAr = "طلب العلم النافع قربة عظيمة تيسّر لصاحبها سبل الهداية والجنة.",
            meaningEn = "Whoever treads a path in pursuit of knowledge, Allah will make easy for him a path to Paradise.",
            meaningUz = "Kim ilm talabida yo'lga chiqsa, Alloh unga jannat yo'lini oson qiladi.",
            sourceAr = "صحيح مسلم • ٢٦٩٩",
            sourceEn = "Sahih Muslim • 2699",
            sourceUz = "Sahih Muslim • 2699"
        )
    )
}
