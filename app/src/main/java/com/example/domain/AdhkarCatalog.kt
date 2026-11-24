package com.example.domain

import com.example.data.local.AdhanVoiceEntity
import com.example.data.local.AdhkarEntity

enum class AdhkarCategoryMeta(
    val id: String,
    val titleAr: String,
    val titleEn: String
) {
    MORNING("MORNING", "أذكار الصباح", "Morning"),
    EVENING("EVENING", "أذكار المساء", "Evening"),
    AFTER_PRAYER("AFTER_PRAYER", "بعد الصلاة", "After Prayer"),
    SLEEP("SLEEP", "أذكار النوم", "Sleep"),
    WAKING_UP("WAKING_UP", "الاستيقاظ", "Waking Up"),
    MOSQUE("MOSQUE", "المسجد والوضوء", "Mosque & Wudu"),
    TRAVEL("TRAVEL", "أذكار السفر", "Travel"),
    HOME_FOOD("HOME_FOOD", "المنزل والطعام", "Home & Food"),
    RUQYAH("RUQYAH", "الرقية الشرعية", "Ruqyah Shariah"),
    QURANIC_DUAS("QURANIC_DUAS", "أدعية قرآنية", "Quranic Duas"),
    PROPHETIC_DUAS("PROPHETIC_DUAS", "أدعية نبوية", "Prophetic Duas"),
    GENERAL_DUAS("GENERAL_DUAS", "أدعية عامة", "General Duas");

    companion object {
        fun fromId(id: String): AdhkarCategoryMeta =
            entries.firstOrNull { it.id == id } ?: MORNING
    }
}

object AdhkarAndAdhanCatalog {

    val defaultAdhanVoices: List<AdhanVoiceEntity> = listOf(
        AdhanVoiceEntity(
            id = "makkah",
            nameAr = "أذان المسجد الحرام",
            nameEn = "Makkah Al-Haram Adhan",
            muezzin = "الشيخ علي أحمد ملا",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan1.mp3",
            active = true
        ),
        AdhanVoiceEntity(
            id = "madinah",
            nameAr = "أذان المسجد النبوي",
            nameEn = "Al-Masjid An-Nabawi Adhan",
            muezzin = "الشيخ عصام بخاري",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan2.mp3",
            active = true
        ),
        AdhanVoiceEntity(
            id = "aqsa",
            nameAr = "أذان المسجد الأقصى",
            nameEn = "Al-Aqsa Mosque Adhan",
            muezzin = "مؤذن المسجد الأقصى",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan3.mp3",
            active = true
        ),
        AdhanVoiceEntity(
            id = "alafasy",
            nameAr = "مشاري راشد العفاسي",
            nameEn = "Mishary Rashid Alafasy",
            muezzin = "الشيخ مشاري العفاسي",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan6.mp3",
            active = true
        ),
        AdhanVoiceEntity(
            id = "abdulbasit",
            nameAr = "عبد الباسط عبد الصمد",
            nameEn = "Abdul Basit 'Abdus-Samad",
            muezzin = "الشيخ عبد الباسط عبد الصمد",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan12.mp3",
            active = true
        )
    )

    val defaultAdhkarEntities: List<AdhkarEntity> = listOf(
        // 1. MORNING
        AdhkarEntity(
            id = "morning_1", category = "MORNING",
            titleAr = "سيد الاستغفار", titleEn = "Sayyid al-Istighfar",
            arabic = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ خَلَقْتَنِي وَأَنَا عَبْدُكَ وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            transliteration = "Allāhumma Anta Rabbī lā ilāha illā Anta, khalaqtanī wa anā ʿabduka...",
            translation = "O Allah, You are my Lord, none has the right to be worshiped except You. You created me and I am Your servant... forgive me, for none forgives sins except You.",
            targetCount = 1,
            referenceAr = "صحيح البخاري (٦٣٠٦): من قالها موقناً بها فمات من يومه دخل الجنة.",
            referenceEn = "Sahih al-Bukhari (6306): Whoever recites it with conviction enters Paradise if they pass away that day.",
            sortOrder = 1
        ),
        AdhkarEntity(
            id = "morning_2", category = "MORNING",
            titleAr = "آية الكرسي", titleEn = "Ayat al-Kursi",
            arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            transliteration = "Allāhu lā ilāha illā Huwa l-Ḥayyu l-Qayyūm...",
            translation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of all existence...",
            targetCount = 1,
            referenceAr = "سورة البقرة (٢٥٥) • النسائي والحاكم: حفظ من الشيطان حتى يمسي.",
            referenceEn = "Quran (2:255) • Al-Nasa'i: Protection throughout the day.",
            sortOrder = 2
        ),
        AdhkarEntity(
            id = "morning_3", category = "MORNING",
            titleAr = "بسم الله الذي لا يضر مع اسمه شيء", titleEn = "Protection Against Sudden Harm",
            arabic = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            transliteration = "Bismillāhi lladhī lā yaḍurru maʿa ismihi shayʾun fi l-arḍi wa lā fi s-samāʾi wa Huwa s-Samīʿu l-ʿAlīm",
            translation = "In the name of Allah, with whose name nothing on earth or in heaven can cause harm, and He is the All-Hearing, All-Knowing.",
            targetCount = 3,
            referenceAr = "أبو داود (٥٠٨٨) والترمذي (صحيح): لم يضره شيء.",
            referenceEn = "Abu Dawud (5088), Tirmidhi: Nothing shall harm the reciter.",
            sortOrder = 3
        ),
        AdhkarEntity(
            id = "morning_4", category = "MORNING",
            titleAr = "رضيت بالله رباً", titleEn = "Contentment with Allah, Islam & Muhammad ﷺ",
            arabic = "رَضِيتُ بِاللَّهِ رَبًّا وَبِالْإِسْلَامِ دِينًا وَبِمُحَمَّدٍ ﷺ نَبِيًّا",
            transliteration = "Raḍītu billāhi Rabban wa bil-Islāmi dīnan wa bi-Muḥammadin ﷺ nabiyyā",
            translation = "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad ﷺ as my Prophet.",
            targetCount = 3,
            referenceAr = "أبو داود وأحمد: كان حقاً على الله أن يرضيه يوم القيامة.",
            referenceEn = "Abu Dawud & Ahmad: Allah promises to please whoever says it 3 times.",
            sortOrder = 4
        ),

        // 2. EVENING
        AdhkarEntity(
            id = "evening_1", category = "EVENING",
            titleAr = "أمسينا وأمسى الملك لله", titleEn = "Evening Affirmation of Dominion",
            arabic = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "Amsaynā wa amsa l-mulku lillāhi wal-ḥamdu lillāh...",
            translation = "We have reached the evening and all dominion belongs to Allah; all praise is for Allah.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٢٧٢٣).",
            referenceEn = "Sahih Muslim (2723).",
            sortOrder = 5
        ),
        AdhkarEntity(
            id = "evening_2", category = "EVENING",
            titleAr = "أعوذ بكلمات الله التامات", titleEn = "Refuge in Allah's Perfect Words",
            arabic = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            transliteration = "Aʿūdhu bikalimāti llāhi t-tāmmāti min sharri mā khalaq",
            translation = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            targetCount = 3,
            referenceAr = "صحيح مسلم (٢٧٠٩): حماية من كل شر في الليل.",
            referenceEn = "Sahih Muslim (2709): Nighttime protection.",
            sortOrder = 6
        ),
        AdhkarEntity(
            id = "evening_3", category = "EVENING",
            titleAr = "حسبي الله لا إله إلا هو", titleEn = "Sufficiency in Allah",
            arabic = "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            transliteration = "Ḥasbiya llāhu lā ilāha illā Huwa ʿalayhi tawakkaltu wa Huwa Rabbu l-ʿArshi l-ʿAẓīm",
            translation = "Allah is sufficient for me; there is none worthy of worship except Him. In Him I place my trust.",
            targetCount = 7,
            referenceAr = "أبو داود (٥٠٨١): كفاه الله ما أهمه من أمر الدنيا والآخرة.",
            referenceEn = "Abu Dawud (5081): Allah suffices whoever says it 7 times against all worries.",
            sortOrder = 7
        ),

        // 3. AFTER_PRAYER
        AdhkarEntity(
            id = "after_prayer_1", category = "AFTER_PRAYER",
            titleAr = "الاستغفار والسلام بعد التسليم", titleEn = "Istighfar & As-Salam After Salah",
            arabic = "أَسْتَغْفِرُ اللَّهَ (ثَلَاثًا) اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
            transliteration = "Astaghfirullāh (3x). Allāhumma Anta s-Salāmu wa minka s-salāmu, tabārakta yā Dha l-Jalāli wal-Ikrām",
            translation = "I seek Allah's forgiveness (3x). O Allah, You are Peace and from You comes peace; Blessed are You, Possessor of Majesty and Honor.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٥٩١).",
            referenceEn = "Sahih Muslim (591).",
            sortOrder = 8
        ),
        AdhkarEntity(
            id = "after_prayer_2", category = "AFTER_PRAYER",
            titleAr = "تسبيح فاطمة (سبحان الله ٣٣)", titleEn = "SubhanAllah (33x)",
            arabic = "سُبْحَانَ اللَّهِ",
            transliteration = "SubḥānAllāh",
            translation = "Glory be to Allah.",
            targetCount = 33,
            referenceAr = "صحيح مسلم (٥٩٧): سبحان الله ٣٣، والحمد لله ٣٣، والله أكبر ٣٣.",
            referenceEn = "Sahih Muslim (597): Part of the post-prayer Tasbih of 33-33-33.",
            sortOrder = 9
        ),
        AdhkarEntity(
            id = "after_prayer_3", category = "AFTER_PRAYER",
            titleAr = "الحمد لله (٣٣)", titleEn = "Alhamdulillah (33x)",
            arabic = "الْحَمْدُ لِلَّهِ",
            transliteration = "Alḥamdulillāh",
            translation = "All praise is due to Allah.",
            targetCount = 33,
            referenceAr = "صحيح مسلم (٥٩٧).",
            referenceEn = "Sahih Muslim (597).",
            sortOrder = 10
        ),
        AdhkarEntity(
            id = "after_prayer_4", category = "AFTER_PRAYER",
            titleAr = "الله أكبر (٣٣)", titleEn = "Allahu Akbar (33x)",
            arabic = "اللَّهُ أَكْبَرُ",
            transliteration = "Allāhu Akbar",
            translation = "Allah is the Greatest.",
            targetCount = 33,
            referenceAr = "صحيح مسلم (٥٩٧).",
            referenceEn = "Sahih Muslim (597).",
            sortOrder = 11
        ),
        AdhkarEntity(
            id = "after_prayer_5", category = "AFTER_PRAYER",
            titleAr = "اللهم أعني على ذكرك", titleEn = "Aid in Remembrance & Worship",
            arabic = "اللَّهُمَّ أَعِنِّي عَلَىٰ ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            transliteration = "Allāhumma aʿinnī ʿalā dhikrika wa shukrika wa ḥusni ʿibādatik",
            translation = "O Allah, help me to remember You, thank You, and worship You in the best manner.",
            targetCount = 1,
            referenceAr = "أبو داود (١٥٢٢) والنسائي (صحيح): أوصى بها النبي ﷺ معاذ بن جبل.",
            referenceEn = "Abu Dawud (1522): Advised to Mu'adh ibn Jabal after every prayer.",
            sortOrder = 12
        ),

        // 4. SLEEP
        AdhkarEntity(
            id = "sleep_1", category = "SLEEP",
            titleAr = "دعاء النوم", titleEn = "Before Sleeping",
            arabic = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            transliteration = "Bismika Allāhumma amūtu wa aḥyā",
            translation = "In Your name, O Allah, I die and I live.",
            targetCount = 1,
            referenceAr = "صحيح البخاري (٦٣٢٤).",
            referenceEn = "Sahih al-Bukhari (6324).",
            sortOrder = 13
        ),
        AdhkarEntity(
            id = "sleep_2", category = "SLEEP",
            titleAr = "باسمك ربي وضعت جنبي", titleEn = "Placing Side on the Bed",
            arabic = "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي وَبِكَ أَرْفَعُهُ فَإِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ",
            transliteration = "Bismika Rabbī waḍaʿtu janbī wa bika arfaʿuhu...",
            translation = "In Your name my Lord, I lie down and in Your name I rise; if You take my soul, have mercy on it, and if You return it, protect it as You protect Your righteous servants.",
            targetCount = 1,
            referenceAr = "صحيح البخاري (٦٣٢٠) ومسلم (٢٧١٤).",
            referenceEn = "Sahih al-Bukhari (6320) & Muslim (2714).",
            sortOrder = 14
        ),
        AdhkarEntity(
            id = "sleep_3", category = "SLEEP",
            titleAr = "المعوذات مع النفث في الكفين", titleEn = "Al-Ikhlas & Al-Mu'awwidhat",
            arabic = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
            transliteration = "Qul Huwa llāhu Aḥad • Qul Aʿūdhu bi-Rabbi l-Falaq • Qul Aʿūdhu bi-Rabbi n-Nās",
            translation = "Recite Surahs 112, 113, and 114 gently into cupped palms and wipe over the body.",
            targetCount = 3,
            referenceAr = "صحيح البخاري (٥٠١٧): يجمع كفيه فينفث فيهما ثم يمسح بهما ما استطاع من جسده ثلاث مرات.",
            referenceEn = "Sahih al-Bukhari (5017): Repeated three times before sleep.",
            sortOrder = 15
        ),

        // 5. WAKING_UP
        AdhkarEntity(
            id = "waking_1", category = "WAKING_UP",
            titleAr = "الحمد لله الذي أحيانا", titleEn = "Upon Waking Up",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            transliteration = "Al-ḥamdu lillāhi lladhī aḥyānā baʿda mā amātanā wa ilayhi n-nushūr",
            translation = "All praise is for Allah who restored us to life after causing us to sleep, and unto Him is the resurrection.",
            targetCount = 1,
            referenceAr = "صحيح البخاري (٦٣١٢).",
            referenceEn = "Sahih al-Bukhari (6312).",
            sortOrder = 16
        ),
        AdhkarEntity(
            id = "waking_2", category = "WAKING_UP",
            titleAr = "الحمد لله الذي عافاني في جسدي", titleEn = "Gratitude for Health & Soul",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي عَافَانِي فِي جَسَدِي وَرَدَّ عَلَيَّ رُوحِي وَأَذِنَ لِي بِذِكْرِهِ",
            transliteration = "Al-ḥamdu lillāhi lladhī ʿāfānī fī jasadī wa radda ʿalayya rūḥī wa adhina lī bi-dhikrih",
            translation = "Praise be to Allah who gave strength to my body, returned my soul to me, and permitted me to remember Him.",
            targetCount = 1,
            referenceAr = "سنن الترمذي (٣٤٠١ - حسن).",
            referenceEn = "Jami' al-Tirmidhi (3401).",
            sortOrder = 17
        ),

        // 6. MOSQUE
        AdhkarEntity(
            id = "mosque_1", category = "MOSQUE",
            titleAr = "الذكر بعد الفراغ من الوضوء", titleEn = "After Completing Wudu",
            arabic = "أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ اللَّهُمَّ اجْعَلْنِي مِنَ التَّوَّابِينَ وَاجْعَلْنِي مِنَ الْمُتَطَهِّرِينَ",
            transliteration = "Ashhadu an lā ilāha illa llāhu waḥdahu lā sharīka lahu...",
            translation = "I bear witness that none has the right to be worshiped except Allah alone, and Muhammad is His servant and Messenger. O Allah, make me among those who repent and purify themselves.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٢٣٤) والترمذي (٥٥): تُفتح له أبواب الجنة الثمانية.",
            referenceEn = "Sahih Muslim (234) & Tirmidhi (55): Opens all eight gates of Paradise.",
            sortOrder = 18
        ),
        AdhkarEntity(
            id = "mosque_2", category = "MOSQUE",
            titleAr = "دعاء الذهاب إلى المسجد", titleEn = "Walking to the Mosque",
            arabic = "اللَّهُمَّ اجْعَلْ فِي قَلْبِي نُورًا وَفِي لِسَانِي نُورًا وَاجْعَلْ فِي سَمْعِي نُورًا وَاجْعَلْ فِي بَصَرِي نُورًا",
            transliteration = "Allāhumma jʿal fī qalbī nūran wa fī lisānī nūran...",
            translation = "O Allah, place light in my heart, light upon my tongue, light in my hearing, and light in my sight.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٧٦٣).",
            referenceEn = "Sahih Muslim (763).",
            sortOrder = 19
        ),
        AdhkarEntity(
            id = "mosque_3", category = "MOSQUE",
            titleAr = "دعاء دخول المسجد", titleEn = "Entering the Mosque",
            arabic = "بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَىٰ رَسُولِ اللَّهِ اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            transliteration = "Bismillāhi waṣ-ṣalātu was-salāmu ʿalā Rasūlillāh, Allāhumma ftaḥ lī abwāba raḥmatik",
            translation = "In the name of Allah, blessings and peace upon the Messenger of Allah. O Allah, open for me the gates of Your mercy.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٧١٣): يُقدِّم رجله اليمنى عند الدخول.",
            referenceEn = "Sahih Muslim (713): Step in with the right foot.",
            sortOrder = 20
        ),
        AdhkarEntity(
            id = "mosque_4", category = "MOSQUE",
            titleAr = "دعاء الخروج من المسجد", titleEn = "Leaving the Mosque",
            arabic = "بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَىٰ رَسُولِ اللَّهِ اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            transliteration = "Bismillāh, Allāhumma innī asʾaluka min faḍlik",
            translation = "In the name of Allah; O Allah, indeed I ask You from Your bounty.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٧١٣): يُقدِّم رجله اليسرى عند الخروج.",
            referenceEn = "Sahih Muslim (713).",
            sortOrder = 21
        ),

        // 7. TRAVEL
        AdhkarEntity(
            id = "travel_1", category = "TRAVEL",
            titleAr = "دعاء الركوب والسفر", titleEn = "Boarding & Travel Supplication",
            arabic = "اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَٰذَا الْبِرَّ وَالتَّقْوَىٰ وَمِنَ الْعَمَلِ مَا تَرْضَىٰ",
            transliteration = "Allāhu Akbar (3x). Subḥāna lladhī sakhkhara lanā hādhā wa mā kunnā lahu muqrinīn...",
            translation = "Allah is the Greatest (3x). Glory to Him who has subjected this to us, and surely to our Lord we shall return. O Allah, we ask You on this journey for righteousness, piety, and deeds pleasing to You.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (١٣٤٢) والزخرف (١٣-١٤).",
            referenceEn = "Sahih Muslim (1342) & Quran (43:13-14).",
            sortOrder = 22
        ),
        AdhkarEntity(
            id = "travel_2", category = "TRAVEL",
            titleAr = "دعاء نزل المكان الجديد", titleEn = "Arriving at a Hotel or Destination",
            arabic = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            transliteration = "Aʿūdhu bikalimāti llāhi t-tāmmāti min sharri mā khalaq",
            translation = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٢٧٠٨): من نزل منزلاً فقالها لم يضره شيء حتى يرتحل.",
            referenceEn = "Sahih Muslim (2708): Safety at any stopover.",
            sortOrder = 23
        ),

        // 8. HOME_FOOD
        AdhkarEntity(
            id = "home_1", category = "HOME_FOOD",
            titleAr = "دعاء الخروج من المنزل", titleEn = "Leaving the Home",
            arabic = "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            transliteration = "Bismillāh, tawakkaltu ʿala llāh, wa lā ḥawla wa lā quwwata illā billāh",
            translation = "In the name of Allah, I place my trust in Allah, and there is no power nor strength except with Allah.",
            targetCount = 1,
            referenceAr = "أبو داود (٥٠٩٥) والترمذي (٣٤٢٦): يقال له هُديت وكُفيت ووُقيت.",
            referenceEn = "Abu Dawud (5095), Tirmidhi (3426): Guided, sufficed, and protected.",
            sortOrder = 24
        ),
        AdhkarEntity(
            id = "home_2", category = "HOME_FOOD",
            titleAr = "دعاء دخول المنزل", titleEn = "Entering the Home",
            arabic = "بِسْمِ اللَّهِ وَلَجْنَا وَبِسْمِ اللَّهِ خَرَجْنَا وَعَلَىٰ رَبِّنَا تَوَكَّلْنَا",
            transliteration = "Bismillāhi walajnā wa bismillāhi kharajnā wa ʿalā Rabbinā tawakkalnā",
            translation = "In the name of Allah we enter, in the name of Allah we leave, and upon our Lord we rely.",
            targetCount = 1,
            referenceAr = "أبو داود (٥٠٩٦) ثم يسلم على أهله.",
            referenceEn = "Abu Dawud (5096), followed by greeting the household with Salam.",
            sortOrder = 25
        ),
        AdhkarEntity(
            id = "home_3", category = "HOME_FOOD",
            titleAr = "الحمد بعد الطعام والشراب", titleEn = "Gratitude After Eating",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            transliteration = "Al-ḥamdu lillāhi lladhī aṭʿamanī hādhā wa razaqanīhi min ghayri ḥawlin minnī wa lā quwwah",
            translation = "All praise is for Allah who fed me this and provided it for me without any might or power from me.",
            targetCount = 1,
            referenceAr = "الترمذي (٣٤٥٨) وأبو داود (٤٠٢٣): غُفر له ما تقدم من ذنبه.",
            referenceEn = "Tirmidhi (3458) & Abu Dawud (4023).",
            sortOrder = 26
        ),

        // 9. RUQYAH
        AdhkarEntity(
            id = "ruqyah_1", category = "RUQYAH",
            titleAr = "سورة الفاتحة (الرقية الشافية)", titleEn = "Surah Al-Fatihah (Healing Ruqyah)",
            arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۝ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝ الرَّحْمَٰنِ الرَّحِيمِ ۝ مَالِكِ يَوْمِ الدِّينِ ۝ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۝ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۝ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
            transliteration = "Al-ḥamdu lillāhi Rabbi l-ʿālamīn...",
            translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful. All praise is due to Allah, Lord of the worlds...",
            targetCount = 7,
            referenceAr = "صحيح البخاري (٥٧٣٦): أم الكتاب رقية شافية بإذن الله.",
            referenceEn = "Sahih al-Bukhari (5736): Al-Fatihah as Ruqyah for healing.",
            sortOrder = 27
        ),
        AdhkarEntity(
            id = "ruqyah_2", category = "RUQYAH",
            titleAr = "أذهب الباس رب الناس", titleEn = "Prophetic Healing Supplication",
            arabic = "اللَّهُمَّ رَبَّ النَّاسِ أَذْهِبِ الْبَاسَ اشْفِ وَأَنْتَ الشَّافِي لَا شِفَاءَ إِلَّا شِفَاؤُكَ شِفَاءً لَا يُغَادِرُ سَقَمًا",
            transliteration = "Allāhumma Rabba n-nās, adhhibi l-bās, ishfi wa Anta sh-Shāfī, lā shifāʾa illā shifāʾuka, shifāʾan lā yughādiru saqamā",
            translation = "O Allah, Lord of mankind, remove the affliction and heal; You are the Healer. There is no healing except Your healing—a healing that leaves behind no illness.",
            targetCount = 3,
            referenceAr = "صحيح البخاري (٥٧٤٣) ومسلم (٢١٩١).",
            referenceEn = "Sahih al-Bukhari (5743) & Muslim (2191).",
            sortOrder = 28
        ),
        AdhkarEntity(
            id = "ruqyah_3", category = "RUQYAH",
            titleAr = "أعيذك بكلمات الله التامة", titleEn = "Ruqyah Protection for Children & Family",
            arabic = "أُعِيذُكُمَا بِكَلِمَاتِ اللَّهِ التَّامَّةِ مِنْ كُلِّ شَيْطَانٍ وَهَامَّةٍ وَمِنْ كُلِّ عَيْنٍ لَامَّةٍ",
            transliteration = "Uʿīdhukumā bikalimāti llāhi t-tāmmati min kulli shayṭānin wa hāmmah wa min kulli ʿaynin lāmmah",
            translation = "I seek protection for you in the perfect words of Allah from every devil, every harmful creature, and every envious eye.",
            targetCount = 3,
            referenceAr = "صحيح البخاري (٣٣٧١): كان النبي ﷺ يعوّذ بها الحسن والحسين.",
            referenceEn = "Sahih al-Bukhari (3371): Used by the Prophet ﷺ for Al-Hasan and Al-Husayn.",
            sortOrder = 29
        ),

        // 10. QURANIC_DUAS
        AdhkarEntity(
            id = "quran_dua_1", category = "QURANIC_DUAS",
            titleAr = "ربنا آتنا في الدنيا حسنة", titleEn = "Goodness in Both Worlds",
            arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Rabbanā ātinā fi d-dunyā ḥasanatan wa fi l-ākhirati ḥasanatan wa qinā ʿadhāba n-nār",
            translation = "Our Lord, give us in this world that which is good and in the Hereafter that which is good, and protect us from the punishment of the Fire.",
            targetCount = 1,
            referenceAr = "سورة البقرة (٢٠١) • أكثر دعاء النبي ﷺ (صحيح البخاري ٦٣٨٩).",
            referenceEn = "Quran (2:201) • Most frequent Dua of the Prophet ﷺ.",
            sortOrder = 30
        ),
        AdhkarEntity(
            id = "quran_dua_2", category = "QURANIC_DUAS",
            titleAr = "ربنا لا تزغ قلوبنا", titleEn = "Steadfastness of the Heart",
            arabic = "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً ۚ إِنَّكَ أَنْتَ الْوَهَّابُ",
            transliteration = "Rabbanā lā tuzigh qulūbanā baʿda idh hadaytanā wa hab lanā min ladunka raḥmah",
            translation = "Our Lord, let not our hearts deviate after You have guided us and grant us from Yourself mercy. Indeed, You are the Bestower.",
            targetCount = 1,
            referenceAr = "سورة آل عمران (٨).",
            referenceEn = "Quran (3:8).",
            sortOrder = 31
        ),
        AdhkarEntity(
            id = "quran_dua_3", category = "QURANIC_DUAS",
            titleAr = "دعاء الوالدين والذرية الصالحة", titleEn = "Prayer for Righteous Family",
            arabic = "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
            transliteration = "Rabbanā hab lanā min azwājinā wa dhurriyyātinā qurrata aʿyunin wajʿalnā lil-muttaqīna imāmā",
            translation = "Our Lord, grant us from among our spouses and offspring comfort to our eyes and make us an example for the righteous.",
            targetCount = 1,
            referenceAr = "سورة الفرقان (٧٤).",
            referenceEn = "Quran (25:74).",
            sortOrder = 32
        ),
        AdhkarEntity(
            id = "quran_dua_4", category = "QURANIC_DUAS",
            titleAr = "دعاء يونس عليه السلام في الكرب", titleEn = "Supplication of Prophet Yunus",
            arabic = "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            transliteration = "Lā ilāha illā Anta subḥānaka innī kuntu mina ẓ-ẓālimīn",
            translation = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            targetCount = 3,
            referenceAr = "سورة الأنبياء (٨٧) والترمذي (٣٥٠٥): ما دعا بها مسلم قط إلا استجاب الله له.",
            referenceEn = "Quran (21:87) & Tirmidhi (3505).",
            sortOrder = 33
        ),

        // 11. PROPHETIC_DUAS
        AdhkarEntity(
            id = "prophetic_1", category = "PROPHETIC_DUAS",
            titleAr = "يا مقلب القلوب ثبت قلبي على دينك", titleEn = "Steadfastness Upon Faith",
            arabic = "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَىٰ دِينِكَ",
            transliteration = "Yā Muqalliba l-qulūbi thabbit qalbī ʿalā dīnik",
            translation = "O Turner of hearts, keep my heart firm upon Your religion.",
            targetCount = 3,
            referenceAr = "الترمذي (٢١٤٠ - صحيح).",
            referenceEn = "Jami' al-Tirmidhi (2140).",
            sortOrder = 34
        ),
        AdhkarEntity(
            id = "prophetic_2", category = "PROPHETIC_DUAS",
            titleAr = "جوامع الدعاء (أسألك الهدى والتقى)", titleEn = "Guidance, Piety, Chastity & Contentment",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَىٰ وَالتُّقَىٰ وَالْعَفَافَ وَالْغِنَىٰ",
            transliteration = "Allāhumma innī asʾaluka l-hudā wat-tuqā wal-ʿafāfa wal-ghinā",
            translation = "O Allah, indeed I ask You for guidance, piety, chastity, and self-sufficiency.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٢٧٢١).",
            referenceEn = "Sahih Muslim (2721).",
            sortOrder = 35
        ),
        AdhkarEntity(
            id = "prophetic_3", category = "PROPHETIC_DUAS",
            titleAr = "العلم النافع والرزق الطيب", titleEn = "Beneficial Knowledge & Pure Provision",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا طَيِّبًا وَعَمَلًا مُتَقَبَّلًا",
            transliteration = "Allāhumma innī asʾaluka ʿilman nāfiʿan wa rizqan ṭayyiban wa ʿamalan mutaqabbalā",
            translation = "O Allah, I ask You for beneficial knowledge, good and pure provision, and deeds that are accepted.",
            targetCount = 1,
            referenceAr = "ابن ماجه (٩٢٥ - صحيح): كان يقولها بعد سلام الفجر.",
            referenceEn = "Sunan Ibn Majah (925): Recited after Fajr.",
            sortOrder = 36
        ),

        // 12. GENERAL_DUAS
        AdhkarEntity(
            id = "general_1", category = "GENERAL_DUAS",
            titleAr = "دعاء تفريج الهم والحزن", titleEn = "Relief from Anxiety, Debt & Grief",
            arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ وَالْعَجْزِ وَالْكَسَلِ وَالْبُخْلِ وَالْجُبْنِ وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            transliteration = "Allāhumma innī aʿūdhu bika mina l-hammi wal-ḥazan...",
            translation = "O Allah, I seek refuge in You from anxiety and sorrow, weakness and laziness, miserliness and cowardice, the burden of debts and being overpowered by men.",
            targetCount = 1,
            referenceAr = "صحيح البخاري (٦٣٦٩).",
            referenceEn = "Sahih al-Bukhari (6369).",
            sortOrder = 37
        ),
        AdhkarEntity(
            id = "general_2", category = "GENERAL_DUAS",
            titleAr = "يا حي يا قيوم برحمتك أستغيث", titleEn = "Seeking Relief & Rectification",
            arabic = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ أَصْلِحْ لِي شَأْنِي كُلَّهُ وَلَا تَكِلْنِي إِلَىٰ نَفْسِي طَرْفَةَ عَيْنٍ",
            transliteration = "Yā Ḥayyu yā Qayyūmu bi-raḥmatika astaghīth, aṣliḥ lī shaʾnī kullahu wa lā takilnī ilā nafsī ṭarfata ʿayn",
            translation = "O Ever-Living, O Sustainer, in Your mercy I seek relief; rectify all of my affairs and do not leave me to myself even for the blink of an eye.",
            targetCount = 3,
            referenceAr = "النسائي والحاكم (حسن).",
            referenceEn = "Al-Nasa'i & Al-Hakim.",
            sortOrder = 38
        ),
        AdhkarEntity(
            id = "general_3", category = "GENERAL_DUAS",
            titleAr = "الاسترجاع عند المصيبة", titleEn = "Patience During Calamity (Istirja')",
            arabic = "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ اللَّهُمَّ أْجُرْنِي فِي مُصِيبَتِي وَأَخْلِفْ لِي خَيْرًا مِنْهَا",
            transliteration = "Innā lillāhi wa innā ilayhi rājiʿūn, Allāhumma jurnī fī muṣībatī wa akhlif lī khayran minhā",
            translation = "Indeed we belong to Allah and to Him we return. O Allah, reward me in my affliction and replace it with something better.",
            targetCount = 1,
            referenceAr = "صحيح مسلم (٩١٨): حديث أم سلمة رضي الله عنها.",
            referenceEn = "Sahih Muslim (918).",
            sortOrder = 39
        )
    )
}
