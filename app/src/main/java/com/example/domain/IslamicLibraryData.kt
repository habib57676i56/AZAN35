package com.example.domain

data class DhikrItem(
    val id: String,
    val category: String,
    val title: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val targetCount: Int,
    val virtueAndTakhrij: String
)

data class AllahName(
    val number: Int,
    val arabic: String,
    val transliteration: String,
    val meaning: String
)

data class HadithItem(
    val number: Int,
    val title: String,
    val arabic: String,
    val translation: String,
    val takhrij: String,
    val reflection: String
)

data class ProphetStory(
    val name: String,
    val arabicName: String,
    val era: String,
    val summary: String,
    val keyLesson: String,
    val quranRef: String
)

data class NooraniaLesson(
    val letter: String,
    val name: String,
    val forms: String,
    val harakatExamples: String,
    val pronunciationTip: String
)

data class PrayerGuide(
    val title: String,
    val subtitle: String,
    val steps: List<String>,
    val reference: String
)

data class TriviaQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class TravelPhrase(
    val english: String,
    val arabic: String,
    val transliteration: String,
    val turkish: String,
    val urdu: String
)

object IslamicLibraryData {

    val adhkarCategories = listOf(
        "Morning", "Evening", "Post-Prayer", "Sleep & Waking",
        "Wudu & Mosque", "Travel & Food", "Emergency & Grief", "Prophets' Duas"
    )

    val adhkarList: List<DhikrItem> = listOf(
        DhikrItem(
            "morn_1", "Morning", "Sayyid al-Istighfar (Master Supplication for Forgiveness)",
            "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ خَلَقْتَنِي وَأَنَا عَبْدُكَ وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ",
            "Allāhumma Anta Rabbī lā ilāha illā Anta, khalaqtanī wa anā ʿabduka...",
            "O Allah, You are my Lord, none has the right to be worshiped except You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can.",
            1,
            "Narrated in Sahih al-Bukhari (6306): Whoever says it with conviction in the morning and dies before evening enters Paradise."
        ),
        DhikrItem(
            "morn_2", "Morning", "Contentment in Islam",
            "رَضِيتُ بِاللَّهِ رَبًّا وَبِالْإِسْلَامِ دِينًا وَبِمُحَمَّدٍ ﷺ نَبِيًّا",
            "Raḍītu billāhi Rabban, wa bil-Islāmi dīnan, wa bi-Muḥammadin ﷺ nabiyyā",
            "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad ﷺ as my Prophet.",
            3,
            "Abu Dawud & Ahmad (Hasan): Allah promises to please whoever repeats it three times morning and evening."
        ),
        DhikrItem(
            "morn_3", "Morning", "Protection Against Sudden Harm",
            "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            "Bismillāhi lladhī lā yaḍurru maʿa ismihi shayʾun fi l-arḍi wa lā fi s-samāʾi wa Huwa s-Samīʿu l-ʿAlīm",
            "In the name of Allah, with whose name nothing on earth or in heaven can cause harm, and He is the All-Hearing, All-Knowing.",
            3,
            "Abu Dawud (5088), Tirmidhi (Sahih): Nothing shall harm the reciter until evening."
        ),
        DhikrItem(
            "eve_1", "Evening", "Evening Affirmation of Dominion",
            "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
            "Amsaynā wa amsa l-mulku lillāhi wal-ḥamdu lillāh, lā ilāha illa llāhu waḥdahu lā sharīka lah",
            "We have reached the evening and all dominion belongs to Allah; all praise is for Allah. None has the right to be worshiped except Allah alone.",
            1,
            "Sahih Muslim (2723): Evening remembrance of gratitude and Tawhid."
        ),
        DhikrItem(
            "eve_2", "Evening", "Refuge from Evil of Creation",
            "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            "Aʿūdhu bikalimāti llāhi t-tāmmāti min sharri mā khalaq",
            "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            3,
            "Sahih Muslim (2709): Protects from nocturnal harm and sting."
        ),
        DhikrItem(
            "post_1", "Post-Prayer", "Tasbih Fatimah (33-33-34)",
            "سُبْحَانَ اللَّهِ (٣٣) • الْحَمْدُ لِلَّهِ (٣٣) • اللَّهُ أَكْبَرُ (٣٤)",
            "SubḥānAllāh (33) • Alḥamdulillāh (33) • Allāhu Akbar (34)",
            "Glory be to Allah • Praise be to Allah • Allah is the Greatest.",
            33,
            "Sahih Muslim (597): Sins are forgiven even if like the foam of the sea."
        ),
        DhikrItem(
            "post_2", "Post-Prayer", "Supplication for Remembrance & Excellence",
            "اللَّهُمَّ أَعِنِّي عَلَىٰ ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            "Allāhumma aʿinnī ʿalā dhikrika wa shukrika wa ḥusni ʿibādatik",
            "O Allah, help me to remember You, thank You, and worship You in the best manner.",
            1,
            "Abu Dawud (1522), Al-Nasa'i (Sahih): Advised by the Prophet ﷺ to Mu'adh ibn Jabal after every Salah."
        ),
        DhikrItem(
            "sleep_1", "Sleep & Waking", "Before Sleeping",
            "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            "Bismika Allāhumma amūtu wa aḥyā",
            "In Your name, O Allah, I die and I live.",
            1,
            "Sahih al-Bukhari (6324)."
        ),
        DhikrItem(
            "sleep_2", "Sleep & Waking", "Upon Waking Up",
            "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            "Al-ḥamdu lillāhi lladhī aḥyānā baʿda mā amātanā wa ilayhi n-nushūr",
            "All praise is for Allah who restored us to life after causing us to sleep, and unto Him is the resurrection.",
            1,
            "Sahih al-Bukhari (6312)."
        ),
        DhikrItem(
            "mosque_1", "Wudu & Mosque", "Entering the Mosque",
            "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            "Allāhumma ftaḥ lī abwāba raḥmatik",
            "O Allah, open for me the gates of Your mercy.",
            1,
            "Sahih Muslim (713): Step in with the right foot."
        ),
        DhikrItem(
            "mosque_2", "Wudu & Mosque", "After Completing Wudu",
            "أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ",
            "Ashhadu an lā ilāha illa llāhu waḥdahu lā sharīka lahu wa ashhadu anna Muḥammadan ʿabduhu wa rasūluh",
            "I bear witness that none has the right to be worshiped except Allah alone, and Muhammad is His servant and Messenger.",
            1,
            "Sahih Muslim (234): Opens all eight gates of Paradise."
        ),
        DhikrItem(
            "travel_1", "Travel & Food", "Dua of Travel",
            "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ",
            "Subḥāna lladhī sakhkhara lanā hādhā wa mā kunnā lahu muqrinīn, wa innā ilā Rabbinā lamunqalibūn",
            "Glory to Him who has subjected this to us, though we could never have controlled it ourselves, and surely to our Lord we shall return.",
            1,
            "Sahih Muslim (1342) & Quran (43:13-14)."
        ),
        DhikrItem(
            "emg_1", "Emergency & Grief", "Relief from Anxiety, Debt & Distress",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ وَالْعَجْزِ وَالْكَسَلِ وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            "Allāhumma innī aʿūdhu bika mina l-hammi wal-ḥazan, wal-ʿajzi wal-kasal, wa ḍalaʿi d-dayni wa ghalabati r-rijāl",
            "O Allah, I seek refuge in You from anxiety and grief, weakness and laziness, and from overwhelming debt and dominance of people.",
            1,
            "Sahih al-Bukhari (6369)."
        ),
        DhikrItem(
            "emg_2", "Emergency & Grief", "Grief & Loss Consolation (Istirja')",
            "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ ۝ اللَّهُمَّ أْجُرْنِي فِي مُصِيبَتِي وَأَخْلِفْ لِي خَيْرًا مِنْهَا",
            "Innā lillāhi wa innā ilayhi rājiʿūn. Allāhumma jurnī fī muṣībatī wa akhlif lī khayran minhā",
            "Indeed we belong to Allah and to Him we return. O Allah, reward me in my affliction and replace it with something better.",
            1,
            "Sahih Muslim (918): Narrated by Umm Salamah."
        ),
        DhikrItem(
            "proph_1", "Prophets' Duas", "Dua of Prophet Yunus (A.S.)",
            "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            "Lā ilāha illā Anta subḥānaka innī kuntu mina ẓ-ẓālimīn",
            "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            3,
            "Quran (21:87) & Tirmidhi (Sahih): No Muslim calls upon Allah with these words except that He responds."
        ),
        DhikrItem(
            "proph_2", "Prophets' Duas", "Dua of Prophet Musa (A.S.) for Good",
            "رَبِّ إِنِّي لِمَا أَنْزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ",
            "Rabbi innī limā anzalta ilayya min khayrin faqīr",
            "My Lord, indeed I am in need of whatever good You would send down to me.",
            1,
            "Quran (28:24)."
        )
    )

    val ninetyNineNames: List<AllahName> = listOf(
        AllahName(1, "الرَّحْمَنُ", "Ar-Rahman", "The Entirely Merciful"),
        AllahName(2, "الرَّحِيمُ", "Ar-Rahim", "The Especially Merciful"),
        AllahName(3, "الْمَلِكُ", "Al-Malik", "The Sovereign King"),
        AllahName(4, "الْقُدُّوسُ", "Al-Quddus", "The Absolutely Pure"),
        AllahName(5, "السَّلَامُ", "As-Salam", "The Source of Peace"),
        AllahName(6, "الْمُؤْمِنُ", "Al-Mu'min", "The Granter of Security"),
        AllahName(7, "الْمُهَيْمِنُ", "Al-Muhaymin", "The Guardian & Overseer"),
        AllahName(8, "الْعَزِيزُ", "Al-Aziz", "The Almighty"),
        AllahName(9, "الْجَبَّارُ", "Al-Jabbar", "The Compeller & Mender"),
        AllahName(10, "الْمُتَكَبِّرُ", "Al-Mutakabbir", "The Supreme in Majesty"),
        AllahName(11, "الْخَالِقُ", "Al-Khaliq", "The Creator"),
        AllahName(12, "الْبَارِئُ", "Al-Bari'", "The Originator"),
        AllahName(13, "الْمُصَوِّرُ", "Al-Musawwir", "The Fashioner of Forms"),
        AllahName(14, "الْغَفَّارُ", "Al-Ghaffar", "The Perpetual Forgiver"),
        AllahName(15, "الْقَهَّارُ", "Al-Qahhar", "The All-Prevailing"),
        AllahName(16, "الْوَهَّابُ", "Al-Wahhab", "The Supreme Bestower"),
        AllahName(17, "الرَّزَّاقُ", "Ar-Razzaq", "The Provider"),
        AllahName(18, "الْفَتَّاحُ", "Al-Fattah", "The Opener & Judge"),
        AllahName(19, "الْعَلِيمُ", "Al-Alim", "The All-Knowing"),
        AllahName(20, "الْقَابِضُ", "Al-Qabid", "The Restrainer"),
        AllahName(21, "الْبَاسِطُ", "Al-Basit", "The Expander"),
        AllahName(22, "الْخَافِضُ", "Al-Khafid", "The Abaser"),
        AllahName(23, "الرَّافِعُ", "Ar-Rafi'", "The Exalter"),
        AllahName(24, "الْمُعِزُّ", "Al-Mu'izz", "The Giver of Honor"),
        AllahName(25, "الْمُذِلُّ", "Al-Mudhill", "The Giver of Dishonor"),
        AllahName(26, "السَّمِيعُ", "As-Sami'", "The All-Hearing"),
        AllahName(27, "الْبَصِيرُ", "Al-Basir", "The All-Seeing"),
        AllahName(28, "الْحَكَمُ", "Al-Hakam", "The Impartial Judge"),
        AllahName(29, "الْعَدْلُ", "Al-Adl", "The Utterly Just"),
        AllahName(30, "اللَّطِيفُ", "Al-Latif", "The Subtle & Kind"),
        AllahName(31, "الْخَبِيرُ", "Al-Khabir", "The All-Aware"),
        AllahName(32, "الْحَلِيمُ", "Al-Halim", "The Forbearing"),
        AllahName(33, "الْعَظِيمُ", "Al-Azim", "The Magnificent"),
        AllahName(34, "الْغَفُورُ", "Al-Ghafur", "The All-Forgiving"),
        AllahName(35, "الشَّكُورُ", "Ash-Shakur", "The Most Appreciative"),
        AllahName(36, "الْعَلِيُّ", "Al-Aliyy", "The Most High"),
        AllahName(37, "الْكَبِيرُ", "Al-Kabir", "The Most Great"),
        AllahName(38, "الْحَفِيظُ", "Al-Hafiz", "The Preserver"),
        AllahName(39, "الْمُقِيتُ", "Al-Muqit", "The Sustainer"),
        AllahName(40, "الْحَسِيبُ", "Al-Hasib", "The Reckoner"),
        AllahName(41, "الْجَلِيلُ", "Al-Jalil", "The Majestic"),
        AllahName(42, "الْكَرِيمُ", "Al-Karim", "The Most Generous"),
        AllahName(43, "الرَّقِيبُ", "Ar-Raqib", "The Watchful"),
        AllahName(44, "الْمُجِيبُ", "Al-Mujib", "The Responsive"),
        AllahName(45, "الْوَاسِعُ", "Al-Wasi'", "The All-Encompassing"),
        AllahName(46, "الْحَكِيمُ", "Al-Hakim", "The All-Wise"),
        AllahName(47, "الْوَدُودُ", "Al-Wadud", "The Most Loving"),
        AllahName(48, "الْمَجِيدُ", "Al-Majid", "The Glorious"),
        AllahName(49, "الْبَاعِثُ", "Al-Ba'ith", "The Resurrector"),
        AllahName(50, "الشَّهِيدُ", "Ash-Shahid", "The Witness"),
        AllahName(51, "الْحَقُّ", "Al-Haqq", "The Absolute Truth"),
        AllahName(52, "الْوَكِيلُ", "Al-Wakil", "The Trustee & Disposer"),
        AllahName(53, "الْقَوِيُّ", "Al-Qawiyy", "The All-Strong"),
        AllahName(54, "الْمَتِينُ", "Al-Matin", "The Firm & Steadfast"),
        AllahName(55, "الْوَلِيُّ", "Al-Waliyy", "The Protecting Friend"),
        AllahName(56, "الْحَمِيدُ", "Al-Hamid", "The Praiseworthy"),
        AllahName(57, "الْمُحْصِي", "Al-Muhsi", "The Accounter"),
        AllahName(58, "الْمُبْدِئُ", "Al-Mubdi'", "The Originator"),
        AllahName(59, "الْمُعِيدُ", "Al-Mu'id", "The Restorer"),
        AllahName(60, "الْمُحْيِي", "Al-Muhyi", "The Giver of Life"),
        AllahName(61, "الْمُمِيتُ", "Al-Mumit", "The Bringer of Death"),
        AllahName(62, "الْحَيُّ", "Al-Hayy", "The Ever-Living"),
        AllahName(63, "الْقَيُّومُ", "Al-Qayyum", "The Self-Subsisting"),
        AllahName(64, "الْوَاجِدُ", "Al-Wajid", "The Perceiver"),
        AllahName(65, "الْمَاجِدُ", "Al-Majid", "The Illustrious"),
        AllahName(66, "الْوَاحِدُ", "Al-Wahid", "The One"),
        AllahName(67, "الْأَحَدُ", "Al-Ahad", "The Unique"),
        AllahName(68, "الصَّمَدُ", "As-Samad", "The Eternal Refuge"),
        AllahName(69, "الْقَادِرُ", "Al-Qadir", "The All-Capable"),
        AllahName(70, "الْمُقْتَدِرُ", "Al-Muqtadir", "The All-Determiner"),
        AllahName(71, "الْمُقَدِّمُ", "Al-Muqaddim", "The Expediter"),
        AllahName(72, "الْمُؤَخِّرُ", "Al-Mu'akhkhir", "The Delayer"),
        AllahName(73, "الْأَوَّلُ", "Al-Awwal", "The First"),
        AllahName(74, "الْآخِرُ", "Al-Akhir", "The Last"),
        AllahName(75, "الظَّاهِرُ", "Az-Zahir", "The Manifest"),
        AllahName(76, "الْبَاطِنُ", "Al-Batin", "The Hidden"),
        AllahName(77, "الْوَالِي", "Al-Wali", "The Governor"),
        AllahName(78, "الْمُتَعَالِي", "Al-Muta'ali", "The Most Exalted"),
        AllahName(79, "الْبَرُّ", "Al-Barr", "The Source of Goodness"),
        AllahName(80, "التَّوَّابُ", "At-Tawwab", "The Acceptor of Repentance"),
        AllahName(81, "الْمُنْتَقِمُ", "Al-Muntaqim", "The Avenger"),
        AllahName(82, "الْعَفُوُّ", "Al-Afuww", "The Pardoner"),
        AllahName(83, "الرَّءُوفُ", "Ar-Ra'uf", "The Most Kind"),
        AllahName(84, "مَالِكُ الْمُلْكِ", "Malik al-Mulk", "Owner of Sovereignty"),
        AllahName(85, "ذُو الْجَلَالِ وَالْإِكْرَامِ", "Dhul-Jalali wal-Ikram", "Lord of Majesty & Generosity"),
        AllahName(86, "الْمُقْسِطُ", "Al-Muqsit", "The Equitable"),
        AllahName(87, "الْجَامِعُ", "Al-Jami'", "The Gatherer"),
        AllahName(88, "الْغَنِيُّ", "Al-Ghaniyy", "The Self-Sufficient"),
        AllahName(89, "الْمُغْنِي", "Al-Mughni", "The Enricher"),
        AllahName(90, "الْمَانِعُ", "Al-Mani'", "The Preventer"),
        AllahName(91, "الضَّارُّ", "Ad-Darr", "The Distresser"),
        AllahName(92, "النَّافِعُ", "An-Nafi'", "The Propitious"),
        AllahName(93, "النُّورُ", "An-Nur", "The Light"),
        AllahName(94, "الْهَادِي", "Al-Hadi", "The Guide"),
        AllahName(95, "الْبَدِيعُ", "Al-Badi'", "The Incomparable Originator"),
        AllahName(96, "الْبَاقِي", "Al-Baqi", "The Everlasting"),
        AllahName(97, "الْوَارِثُ", "Al-Warith", "The Inheritor"),
        AllahName(98, "الرَّشِيدُ", "Ar-Rashid", "The Guide to Right Path"),
        AllahName(99, "الصَّبُورُ", "As-Sabur", "The Patient")
    )

    val nawawiHadith: List<HadithItem> = listOf(
        HadithItem(
            1, "Actions Are by Intentions",
            "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            "Verily, actions are judged by intentions, and every person shall have what they intended.",
            "Sahih al-Bukhari (1) & Sahih Muslim (1907) — Narrated by Umar ibn al-Khattab",
            "Sincerity transforms daily habits — eating, working, resting — into worship when intended for Allah."
        ),
        HadithItem(
            2, "Islam, Iman, and Ihsan (Hadith Jibril)",
            "أَنْ تَعْبُدَ اللَّهَ كَأَنَّكَ تَرَاهُ، فَإِنْ لَمْ تَكُنْ تَرَاهُ فَإِنَّهُ يَرَاكَ",
            "Ihsan is to worship Allah as though you see Him; and if you cannot see Him, know that He sees you.",
            "Sahih Muslim (8) — Narrated by Umar ibn al-Khattab",
            "Maps the three dimensions of the deen: outward submission, inward conviction, and spiritual mindfulness."
        ),
        HadithItem(
            6, "Guarding the Heart",
            "أَلَا وَإِنَّ فِي الْجَسَدِ مُضْغَةً إِذَا صَلَحَتْ صَلَحَ الْجَسَدُ كُلُّهُ... أَلَا وَهِيَ الْقَلْبُ",
            "Truly, in the body there is a morsel of flesh; if it is sound, the whole body is sound... Truly, it is the heart.",
            "Sahih al-Bukhari (52) & Sahih Muslim (1599)",
            "Avoid doubtful matters so the spiritual heart stays clear and receptive."
        ),
        HadithItem(
            13, "Brotherly Love",
            "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
            "None of you truly believes until he loves for his brother what he loves for himself.",
            "Sahih al-Bukhari (13) & Sahih Muslim (45)",
            "Faith is measured by compassion, empathy, and wanting goodness for others."
        ),
        HadithItem(
            18, "Taqwa & Good Character",
            "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ",
            "Be mindful of Allah wherever you are, follow a bad deed with a good one to erase it, and treat people with noble character.",
            "Jami' al-Tirmidhi (1987, Hasan Sahih)",
            "Three golden rules for private integrity, repentance, and social grace."
        ),
        HadithItem(
            32, "No Harming, No Reciprocating Harm",
            "لَا ضَرَرَ وَلَا ضِرَارَ",
            "There should be neither harming nor reciprocating harm.",
            "Sunan Ibn Majah (2340), Muwatta Malik — Graded Sahih",
            "One of the five foundational maxims of Islamic jurisprudence (Fiqh)."
        )
    )

    val prophetsStories: List<ProphetStory> = listOf(
        ProphetStory("Adam (A.S.)", "آدم عليه السلام", "Creation & Repentance", "First human and prophet, honored by angels, taught the names of all things, and taught humanity the words of sincere repentance.", "True dignity lies in returning to Allah humbly after any slip.", "Quran 2:30-37, 7:23"),
        ProphetStory("Nuh (A.S.)", "نوح عليه السلام", "950 Years of Patience", "Called his people tirelessly day and night, built the Ark by divine command through mocking crowds, and preserved faith across the flood.", "Stay steadfast upon truth even when numbers are small.", "Quran 11:25-49, 71:1-28"),
        ProphetStory("Ibrahim (A.S.)", "إبراهيم عليه السلام", "Khalilullah (Friend of Allah)", "Reasoned toward the Creator of the heavens, walked unharmed from the fire, rebuilt the Kaaba with Isma'il, and modeled ultimate trust.", "Make Allah's pleasure your compass above every attachment.", "Quran 2:124-129, 21:51-73"),
        ProphetStory("Musa (A.S.)", "موسى عليه السلام", "Kalimullah", "Protected in the Nile as an infant, addressed directly by Allah, confronted Pharaoh's tyranny, and parted the Red Sea by Allah's leave.", "Allah opens paths where no road seems visible.", "Quran 20:9-98, 28:3-43"),
        ProphetStory("Yusuf (A.S.)", "يوسف عليه السلام", "Ahsan al-Qasas", "Betrayed into a well and tested in prison yet kept spotless character, rising to save nations from famine and forgiving his brothers.", "Patience and chastity turn trials into avenues of leadership.", "Quran 12:1-111"),
        ProphetStory("Isa (A.S.)", "عيسى عليه السلام", "Ruhullah", "Born miraculously to Maryam, spoke from the cradle, healed by Allah's leave, and preached pure devotion, mercy, and humility.", "Spiritual purity and mercy outshine worldly materialism.", "Quran 3:45-55, 19:16-36"),
        ProphetStory("Muhammad ﷺ", "محمد ﷺ", "Seal of the Prophets", "Mercy to all worlds, received revelation at 40, endured Makkah with patience, built a compassionate society in Madinah, and forgave Makkah upon conquest.", "Character, mercy, and steadfastness conquer hearts forever.", "Quran 21:107, 33:21")
    )

    val nooraniaLessons: List<NooraniaLesson> = listOf(
        NooraniaLesson("ا", "Alif (ألف)", "ا / ـا", "أَ (a) • إِ (i) • أُ (u)", "Long open vowel sound or Hamza glottal stop."),
        NooraniaLesson("ب", "Ba (باء)", "بـ / ـبـ / ـب", "بَ (ba) • بِ (bi) • بُ (bu) • بً (ban)", "Both lips meet softly; single dot underneath."),
        NooraniaLesson("ت", "Ta (تاء)", "تـ / ـتـ / ـت", "تَ (ta) • تِ (ti) • تُ (tu) • تّ (tta)", "Tip of tongue touches upper front teeth roots; two dots above."),
        NooraniaLesson("ث", "Tha (ثاء)", "ثـ / ـثـ / ـث", "ثَ (tha) • ثِ (thi) • ثُ (thu)", "Soft 'th' as in 'think'; tip of tongue between teeth."),
        NooraniaLesson("ج", "Jim (جيم)", "جـ / ـجـ / ـج", "جَ (ja) • جِ (ji) • جُ (ju) • أَجْ (aj)", "Middle of tongue meets hard palate; Qalqalah when paused with Sukun."),
        NooraniaLesson("ح", "Ha (حاء)", "حـ / ـحـ / ـح", "حَ (ḥa) • حِ (ḥi) • حُ (ḥu)", "Crisp breathy 'h' from middle of the throat."),
        NooraniaLesson("خ", "Kha (خاء)", "خـ / ـخـ / ـخ", "خَ (kha) • خِ (khi) • خُ (khu)", "Heavy letter from top of the throat."),
        NooraniaLesson("د", "Dal (دال)", "د / ـد", "دَ (da) • دِ (di) • دُ (du) • قَدْ (qad)", "Non-connecting letter; Qalqalah when carrying Sukun.")
    )

    val prayerGuides: List<PrayerGuide> = listOf(
        PrayerGuide(
            "How to Pray (Step-by-Step)",
            "Pillars & Sunnah sequence for beginners",
            listOf(
                "1. Perform Wudu cleanly, face the Qibla, and form silent intention (Niyyah) in your heart.",
                "2. Raise hands to ears/shoulders and say Takbirat al-Ihram: 'Allahu Akbar'.",
                "3. Recite opening Dua, 'A'udhu billah...', then Surah Al-Fatihah and another short Surah.",
                "4. Bow into Ruku' with a straight back; say 'Subhana Rabbiyal-Azim' 3 times.",
                "5. Rise upright ('Sami'Allahu liman hamidah — Rabbana wa lakal-hamd').",
                "6. Prostrate into Sujud on 7 bones (forehead/nose, palms, knees, toes); say 'Subhana Rabbiyal-A'la' 3 times.",
                "7. Sit briefly ('Rabbighfir li'), perform second Sujud, then complete remaining Rak'ahs and Tashahhud + Salam."
            ),
            "Sahih al-Bukhari (757)"
        ),
        PrayerGuide(
            "Sujud al-Sahw (Prostration of Forgetfulness)",
            "What to do if you add, omit, or doubt in Salah",
            listOf(
                "1. Omitted a Sunnah/Wajib (e.g., missed middle Tashahhud): Perform 2 prostrations BEFORE saying Salam.",
                "2. Added an extra movement (e.g., extra bowing or standing): Say Salam, then perform 2 prostrations AFTER Salam, followed by another Salam.",
                "3. Doubt in number of Rak'ahs (e.g., 3 or 4?): Build upon the lesser, certain number (3), complete the prayer, and perform 2 prostrations before Salam."
            ),
            "Sahih al-Bukhari (1224) & Sahih Muslim (571)"
        ),
        PrayerGuide(
            "Salat al-Istikhara & Hajah",
            "Seeking Allah's guidance for major decisions",
            listOf(
                "1. Pray two non-obligatory Rak'ahs with sincere presence of heart.",
                "2. Praise Allah, send Salawat upon the Prophet ﷺ, and recite Dua al-Istikhara naming your specific decision.",
                "3. Proceed practically with wise counsel (Shura) — Allah facilitates what is good or turns away what is harmful."
            ),
            "Sahih al-Bukhari (1162)"
        ),
        PrayerGuide(
            "Salat al-Janazah (Funeral Prayer)",
            "Four Takbirs — standing only, no Ruku' or Sujud",
            listOf(
                "1. 1st Takbir: Recite Surah Al-Fatihah silently.",
                "2. 2nd Takbir: Recite Al-Salat al-Ibrahimiyyah (Durood).",
                "3. 3rd Takbir: Make sincere Dua for the deceased ('Allahummaghfir lahu warhamhu...').",
                "4. 4th Takbir: Brief pause/dua, then turn right and say 'Assalamu alaykum wa rahmatullah'."
            ),
            "Sahih Muslim (963)"
        ),
        PrayerGuide(
            "Eclipse (Kusuf) & Rain (Istisqa)",
            "Special congregational Sunnah prayers",
            listOf(
                "Eclipse (Kusuf): Two Rak'ahs, each containing TWO long standings (Qiyam) and TWO long bowings (Ruku') followed by two Sujud.",
                "Rain (Istisqa): Two open-air Rak'ahs like Eid prayer, followed by a Khutbah, turning the outer cloak inside-out in humility, and earnest Dua for mercy."
            ),
            "Sahih al-Bukhari (1044 & 1011)"
        )
    )

    val newMuslimPath: List<Pair<String, String>> = listOf(
        "Day 1–3: Shahadah & Wudu" to "Embrace the beauty of Tawhid and practice peaceful, unhurried Wudu.",
        "Day 4–10: Learning Al-Fatihah & 2 Rak'ahs" to "Start gently with Fajr and Maghrib using short verses and Al-Fatihah.",
        "Day 11–18: Building All 5 Daily Anchors" to "Connect your schedule around the five prayers as spiritual breathers.",
        "Day 19–25: Community & Friday Jummah" to "Visit a local mosque, meet welcoming Muslims, and enjoy weekly reminders.",
        "Day 26–30: Daily Wird & Character" to "Read 5 minutes of Quran translation daily and nourish family & neighbors with kindness."
    )

    val travelPhrases: List<TravelPhrase> = listOf(
        TravelPhrase("Where is the prayer room?", "أين مصلى الصلاة؟", "Ayna musalla as-salah?", "Mescit nerede?", "نماز کی جگہ کہاں ہے؟"),
        TravelPhrase("Where is the Wudu area?", "أين مكان الوضوء؟", "Ayna makan al-wudu?", "Abdesthane nerede?", "وضو خانہ کہاں ہے؟"),
        TravelPhrase("Which way is the Qibla?", "ما هو اتجاه القبلة؟", "Ma huwa ittijah al-Qiblah?", "Kıble ne tarafta?", "قبلہ کس طرف ہے؟"),
        TravelPhrase("Is this food Halal?", "هل هذا الطعام حلال؟", "Hal hadha at-ta'am halal?", "Bu yemek helal mi?", "کیا یہ کھانا حلال ہے؟"),
        TravelPhrase("May Allah reward you", "جزاك الله خيراً", "JazakAllahu khayran", "Allah razı olsun", "جزاک اللہ خیراً")
    )

    val triviaQuiz: List<TriviaQuestion> = listOf(
        TriviaQuestion(
            "Which Surah is known as 'Umm al-Kitab' (Mother of the Book)?",
            listOf("Al-Baqarah", "Al-Fatihah", "Al-Ikhlas", "Yasin"),
            1,
            "Surah Al-Fatihah summarizes all themes of the Quran: praise, Tawhid, guidance, and worship."
        ),
        TriviaQuestion(
            "How many months are in the Islamic Hijri lunar calendar?",
            listOf("10", "11", "12", "13"),
            2,
            "Quran (9:36) confirms the number of months with Allah is twelve, four of which are sacred."
        ),
        TriviaQuestion(
            "Which prayer consists of 4 Takbirs with no bowing or prostration?",
            listOf("Salat al-Eid", "Salat al-Kusuf", "Salat al-Janazah", "Salat al-Witr"),
            2,
            "Salat al-Janazah (Funeral Prayer) is prayed standing with four Takbirs."
        ),
        TriviaQuestion(
            "What is the standard Zakat rate on savings held above Nisab for one lunar year (Hawl)?",
            listOf("1.5%", "2.5%", "5%", "10%"),
            1,
            "2.5% (one-fortieth) is payable on cash, gold, silver, and trade assets after 1 Hijri year."
        ),
        TriviaQuestion(
            "Which nights are called 'The White Days' (Ayyam al-Bid) for recommended monthly fasting?",
            listOf("1st, 2nd, 3rd", "10th, 11th, 12th", "13th, 14th, 15th", "27th, 28th, 29th"),
            2,
            "Fasting the 13th, 14th, and 15th of each Hijri month carries the reward of fasting the whole year."
        )
    )
}
