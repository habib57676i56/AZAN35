package com.example.domain

data class WordMeaning(
    val arabic: String,
    val transliteration: String,
    val meaning: String
)

data class QuranAyah(
    val number: Int,
    val arabic: String,
    val transliteration: String,
    val translationEn: String,
    val translationTr: String,
    val translationUr: String,
    val tafsirMuyassar: String,
    val tafsirIbnKathir: String,
    val tafsirSadi: String,
    val asbabAlNuzul: String,
    val tajweedWaqfNote: String,
    val qiraatNote: String,
    val rareWordMeaning: String,
    val mutashabihatRef: String,
    val themeTag: String,
    val sajdahRequired: Boolean = false,
    val wordByWord: List<WordMeaning> = emptyList()
)

data class QuranSurah(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val revelationPlace: String,
    val juzNumber: Int,
    val ayahCountTotal: Int,
    val ayahs: List<QuranAyah>
)

data class RecitationWordDiff(
    val expectedWord: String,
    val matched: Boolean
)

data class RecitationCheckResult(
    val accuracyPercent: Int,
    val wordDiffs: List<RecitationWordDiff>,
    val missingCount: Int
)

object QuranContentData {

    val thematicTopics = listOf(
        "All", "Opening & Prayer", "Protection & Ruqyah", "Mercy & Guidance",
        "Patience & Trust", "Sustenance", "Heart & Reflection"
    )

    val surahs: List<QuranSurah> = listOf(
        QuranSurah(
            number = 1,
            nameAr = "سورة الفاتحة",
            nameEn = "Al-Fatihah (The Opening)",
            revelationPlace = "Makkah",
            juzNumber = 1,
            ayahCountTotal = 7,
            ayahs = listOf(
                QuranAyah(
                    number = 1,
                    arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    transliteration = "Bismillāhi r-Raḥmāni r-Raḥīm",
                    translationEn = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                    translationTr = "Rahmân ve Rahîm olan Allah'ın adıyla.",
                    translationUr = "اللہ کے نام سے جو بے حد مہربان نہایت رحم والا ہے۔",
                    tafsirMuyassar = "I begin my recitation seeking blessing through the name of Allah, whose mercy encompasses all creation (Al-Rahman) and is especially bestowed upon the believers (Al-Rahim).",
                    tafsirIbnKathir = "The Companions opened the Book of Allah with Bismillah; Al-Rahman denotes vast, all-encompassing mercy, while Al-Rahim denotes enduring, intimate mercy toward believers.",
                    tafsirSadi = "Every righteous deed begun with Allah's name attains blessing and protection from shortfall.",
                    asbabAlNuzul = "Revealed early in Makkah as the foundation of daily Salah and opening of the Mushaf.",
                    tajweedWaqfNote = "Lam of Allah is pronounced heavy (Tafkheem) after Fatha/Damma, light (Tarqeeq) after Kasra in Bismi-llah. Stop cleanly (Waqf Tamm) at the end.",
                    qiraatNote = "Hafs & Warsh agree on reciting Al-Rahim with elongation (Madd 'Arid lis-Sukun 2-4-6 counts on pause).",
                    rareWordMeaning = "Al-Rahman: vast mercy; Al-Rahim: continual special mercy.",
                    mutashabihatRef = "Also appears in Surah An-Naml (27:30) in the letter of Sulayman.",
                    themeTag = "Opening & Prayer",
                    wordByWord = listOf(
                        WordMeaning("بِسْمِ", "Bismi", "In the name of"),
                        WordMeaning("اللَّهِ", "Allahi", "Allah"),
                        WordMeaning("الرَّحْمَٰنِ", "Ar-Rahman", "The Entirely Merciful"),
                        WordMeaning("الرَّحِيمِ", "Ar-Rahim", "The Especially Merciful")
                    )
                ),
                QuranAyah(
                    number = 2,
                    arabic = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                    transliteration = "Al-ḥamdu lillāhi Rabbi l-ʿālamīn",
                    translationEn = "All praise is due to Allah, Lord of the worlds.",
                    translationTr = "Hamd, âlemlerin Rabbi Allah'a mahsustur.",
                    translationUr = "تمام تعریفیں اللہ ہی کے لیے ہیں جو تمام جہانوں کا رب ہے۔",
                    tafsirMuyassar = "All praise, perfection, and gratitude belong exclusively to Allah, the Creator, Sustainer, and Nurturer of every realm.",
                    tafsirIbnKathir = "Al-Hamd includes praising Allah for His perfection and thanking Him for His blessings; 'Alamin encompasses everything besides Allah.",
                    tafsirSadi = "True servitude begins by recognizing Allah's Lordship (Rububiyyah) over every atom.",
                    asbabAlNuzul = "Part of Al-Sab' al-Mathani (the Seven Oft-Repeated Verses).",
                    tajweedWaqfNote = "Pronounce Al-Qamariyyah Lam clearly in Al-Hamdu and Al-'Alamin.",
                    qiraatNote = "Agreed upon by all canonical Qira'at.",
                    rareWordMeaning = "Al-'Alamin: all created worlds (angels, humans, jinn, cosmos).",
                    mutashabihatRef = "Opened similarly in Al-An'am (6:1), Al-Kahf (18:1), Saba (34:1), Fatir (35:1).",
                    themeTag = "Opening & Prayer",
                    wordByWord = listOf(
                        WordMeaning("الْحَمْدُ", "Al-hamdu", "All praise"),
                        WordMeaning("لِلَّهِ", "lillahi", "is for Allah"),
                        WordMeaning("رَبِّ", "Rabbi", "Lord of"),
                        WordMeaning("الْعَالَمِينَ", "al-'alamin", "the worlds")
                    )
                ),
                QuranAyah(
                    number = 3,
                    arabic = "الرَّحْمَٰنِ الرَّحِيمِ",
                    transliteration = "Ar-Raḥmāni r-Raḥīm",
                    translationEn = "The Entirely Merciful, the Especially Merciful.",
                    translationTr = "O, Rahmân ve Rahîm'dir.",
                    translationUr = "جو بڑا مہربان نہایت رحم والا ہے۔",
                    tafsirMuyassar = "Reiterates Allah's mercy immediately after His Lordship to fill the worshiper's heart with hope.",
                    tafsirIbnKathir = "Balances awe of Rabb al-'Alamin with longing for His mercy.",
                    tafsirSadi = "Mercy is the overarching attribute through which creation is nurtured.",
                    asbabAlNuzul = "Makkan revelation.",
                    tajweedWaqfNote = "Solar Lam (Lam Shamsiyyah) assimilates into the heavy Ra with slight nasal resonance absent.",
                    qiraatNote = "Consistent across Hafs and Warsh.",
                    rareWordMeaning = "Raḥmah: active, nurturing grace.",
                    mutashabihatRef = "Echoes Ayah 1.",
                    themeTag = "Mercy & Guidance"
                ),
                QuranAyah(
                    number = 4,
                    arabic = "مَالِكِ يَوْمِ الدِّينِ",
                    transliteration = "Māliki yawmi d-dīn",
                    translationEn = "Sovereign of the Day of Recompense.",
                    translationTr = "Din (hesap) gününün sahibidir.",
                    translationUr = "بدلے کے دن (قیامت) کا مالک ہے۔",
                    tafsirMuyassar = "He alone holds absolute authority on the Day when every soul is judged and rewarded.",
                    tafsirIbnKathir = "Yawm al-Din is the Day of Reckoning when earthly kingships vanish and only Allah's sovereignty remains.",
                    tafsirSadi = "Reciting this in every Rak'ah keeps accountability alive in the believer's conscience.",
                    asbabAlNuzul = "Makkan revelation.",
                    tajweedWaqfNote = "Natural 2-count Madd in Maliki (Hafs).",
                    qiraatNote = "Hafs recites 'Māliki' (Owner/Master); Warsh & others recite 'Maliki' (King/Sovereign) — both are mutawatir.",
                    rareWordMeaning = "Al-Din: recompense, reckoning, and judgment.",
                    mutashabihatRef = "Compare Al-Infitar (82:17-19).",
                    themeTag = "Opening & Prayer"
                ),
                QuranAyah(
                    number = 5,
                    arabic = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
                    transliteration = "Iyyāka naʿbudu wa iyyāka nastaʿīn",
                    translationEn = "It is You alone we worship and You alone we ask for help.",
                    translationTr = "Yalnız sana ibadet eder ve yalnız senden yardım dileriz.",
                    translationUr = "ہم تیری ہی عبادت کرتے ہیں اور تجھ ہی سے مدد چاہتے ہیں۔",
                    tafsirMuyassar = "We direct all worship exclusively to You, and seek Your aid in every spiritual and worldly matter.",
                    tafsirIbnKathir = "The pivotal verse of Al-Fatihah: Ikhlas (pure worship) paired with Tawakkul (seeking aid solely from Allah).",
                    tafsirSadi = "Worship precedes seeking help because servitude is the goal and divine assistance is the means.",
                    asbabAlNuzul = "In Hadith Qudsi, Allah says: 'This is between Me and My servant, and My servant shall have what he asks.'",
                    tajweedWaqfNote = "Emphasize the Shaddah on Ya in Iyyaka without over-bouncing; pronounce 'Ayn clearly.",
                    qiraatNote = "Identical across Hafs and Warsh.",
                    rareWordMeaning = "Nasta'in: earnestly seek support and strength.",
                    mutashabihatRef = "Compare Hud (11:123): 'So worship Him and rely upon Him.'",
                    themeTag = "Patience & Trust",
                    wordByWord = listOf(
                        WordMeaning("إِيَّاكَ", "Iyyaka", "You alone"),
                        WordMeaning("نَعْبُدُ", "na'budu", "we worship"),
                        WordMeaning("وَإِيَّاكَ", "wa iyyaka", "and You alone"),
                        WordMeaning("نَسْتَعِينُ", "nasta'in", "we ask for help")
                    )
                ),
                QuranAyah(
                    number = 6,
                    arabic = "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                    transliteration = "Ihdinā ṣ-ṣirāṭa l-mustaqīm",
                    translationEn = "Guide us to the straight path.",
                    translationTr = "Bizi doğru yola ilet.",
                    translationUr = "ہمیں سیدھے راستے کی ہدایت دے۔",
                    tafsirMuyassar = "Show us, keep us firm upon, and increase us in clarity on the clear path of Islam.",
                    tafsirIbnKathir = "Even guided believers ask for continual, deepening guidance at every moment.",
                    tafsirSadi = "Al-Sirat al-Mustaqim is knowing the truth and acting upon it.",
                    asbabAlNuzul = "Makkan revelation.",
                    tajweedWaqfNote = "Strong Isti'la (elevation) on Sad and Ta in Al-Sirat.",
                    qiraatNote = "Hafs recites with pure Sad; some Qira'at blend Sad slightly toward Zay or Sin (Qunbul).",
                    rareWordMeaning = "Al-Sirat: clear, wide, direct highway.",
                    mutashabihatRef = "Compare Al-An'am (6:153).",
                    themeTag = "Mercy & Guidance"
                ),
                QuranAyah(
                    number = 7,
                    arabic = "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
                    transliteration = "Ṣirāṭa lladhīna anʿamta ʿalayhim ghayri l-maghḍūbi ʿalayhim wa lā ḍ-ḍāllīn",
                    translationEn = "The path of those upon whom You have bestowed favor, not of those who have evoked anger or of those who are astray.",
                    translationTr = "Kendilerine nimet verdiklerinin yoluna; gazaba uğrayanların ve sapmışların yoluna değil.",
                    translationUr = "ان لوگوں کا راستہ جن پر تو نے انعام کیا، نہ کہ جن پر تیرا غضب ہوا اور نہ گمراہوں کا۔",
                    tafsirMuyassar = "The path of the prophets, truthful, martyrs, and righteous — safe from knowing truth yet rejecting it, or acting without knowledge.",
                    tafsirIbnKathir = "Say 'Ameen' upon completing Al-Fatihah in prayer, as the angels say Ameen.",
                    tafsirSadi = "Guidance requires both sound knowledge and sincere practice.",
                    asbabAlNuzul = "Makkan revelation.",
                    tajweedWaqfNote = "Mandatory 6-count Madd Lazim Kalimi Muthaqqal in 'ad-dallin' before the doubled Lam.",
                    qiraatNote = "Hafs & Warsh both maintain the 6-count Madd Lazim.",
                    rareWordMeaning = "An'amta: those graced with true faith and righteousness.",
                    mutashabihatRef = "Explained in An-Nisa (4:69).",
                    themeTag = "Mercy & Guidance"
                )
            )
        ),
        QuranSurah(
            number = 2,
            nameAr = "آية الكرسي (البقرة)",
            nameEn = "Ayat al-Kursi & Al-Baqarah",
            revelationPlace = "Madinah",
            juzNumber = 3,
            ayahCountTotal = 286,
            ayahs = listOf(
                QuranAyah(
                    number = 255,
                    arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
                    transliteration = "Allāhu lā ilāha illā Huwa l-Ḥayyu l-Qayyūm, lā taʾkhudhuhu sinatun wa lā nawm...",
                    translationEn = "Allah — there is no deity except Him, the Ever-Living, the Sustainer of existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth...",
                    translationTr = "Allah, O'ndan başka ilah yoktur; O Hayy'dır, Kayyûm'dur. Kendisini ne bir uyuklama ne de uyku tutar...",
                    translationUr = "اللہ وہ ہے جس کے سوا کوئی معبود نہیں، وہ زندہ ہے، سب کا تھامنے والا ہے، نہ اسے اونگھ آتی ہے نہ نیند...",
                    tafsirMuyassar = "The greatest verse in the Quran: affirms Allah's absolute oneness, eternal life, self-subsistence, perfect knowledge, and effortless preservation of the universe.",
                    tafsirIbnKathir = "Reciting Ayat al-Kursi after every obligatory prayer and before sleep brings continuous divine protection.",
                    tafsirSadi = "Ten monumental affirmations of Tawhid and divine majesty in a single verse.",
                    asbabAlNuzul = "Madinan revelation; praised in Sahih Muslim when Ubayy ibn Ka'b identified it as the greatest verse.",
                    tajweedWaqfNote = "Observe the Waqf Jaa'iz (ج) and Waqf Awla (قلے) pause marks carefully so meaning stays intact.",
                    qiraatNote = "Hafs & Warsh match closely; Warsh lengthens Madd Badal in 'Ya'uduhu'.",
                    rareWordMeaning = "Sinah: light drowsiness preceding sleep; Ya'uduhu: burdens or tires Him.",
                    mutashabihatRef = "Opening phrase repeats in Al 'Imran (3:2).",
                    themeTag = "Protection & Ruqyah",
                    wordByWord = listOf(
                        WordMeaning("الْحَيُّ", "Al-Hayyu", "The Ever-Living"),
                        WordMeaning("الْقَيُّومُ", "Al-Qayyum", "The Sustainer"),
                        WordMeaning("سِنَةٌ", "sinatun", "drowsiness"),
                        WordMeaning("يَئُودُهُ", "ya'uduhu", "tires Him")
                    )
                ),
                QuranAyah(
                    number = 286,
                    arabic = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا ۚ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ ۗ رَبَّنَا لَا تُؤَاخِذْنَا إِنْ نَسِينَا أَوْ أَخْطَأْنَا",
                    transliteration = "Lā yukallifu llāhu nafsan illā wusʿahā...",
                    translationEn = "Allah does not charge a soul except [with that within] its capacity. It will have [the consequence of] what [good] it has gained, and it will bear [the consequence of] what [evil] it has earned. Our Lord, do not impose blame upon us if we have forgotten or erred.",
                    translationTr = "Allah bir kimseyi ancak gücünün yettiği şeyle yükümlü kılar...",
                    translationUr = "اللہ کسی جان پر اس کی طاقت سے بڑھ کر بوجھ نہیں ڈالتا...",
                    tafsirMuyassar = "Allah's law is filled with ease and mercy; He never burdens a believer beyond ability, and accepts sincere prayer when we forget or err.",
                    tafsirIbnKathir = "The Prophet ﷺ taught that whoever recites the last two verses of Al-Baqarah at night, they will suffice him.",
                    tafsirSadi = "Notice 'kasabat' (easy natural good) vs 'iktasabat' (deliberate effort in sin) — showing Allah's generosity.",
                    asbabAlNuzul = "Revealed after the Companions feared accountability for unspoken inner thoughts.",
                    tajweedWaqfNote = "Idgham with Ghunnah in 'nafsan illa' is actually Izhar (Tanween before Hamza); Idgham occurs in 'in nasina'.",
                    qiraatNote = "Agreed across canonical Qira'at.",
                    rareWordMeaning = "Wus'aha: comfortable capacity without unbearable hardship.",
                    mutashabihatRef = "Similar mercy principle in Al-A'raf (7:42) and Al-Talaq (65:7).",
                    themeTag = "Patience & Trust"
                )
            )
        ),
        QuranSurah(
            number = 18,
            nameAr = "سورة الكهف (مختارات)",
            nameEn = "Al-Kahf (Friday Light)",
            revelationPlace = "Makkah",
            juzNumber = 15,
            ayahCountTotal = 110,
            ayahs = listOf(
                QuranAyah(
                    number = 1,
                    arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَنْزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَلْ لَهُ عِوَجًا",
                    transliteration = "Al-ḥamdu lillāhi lladhī anzala ʿalā ʿabdihi l-kitāba wa lam yajʿal lahu ʿiwajā",
                    translationEn = "All praise is due to Allah, who has sent down upon His Servant the Book and has not made therein any deviance.",
                    translationTr = "Hamd, kuluna Kitab'ı indiren ve onda hiçbir eğrilik kılmayan Allah'a mahsustur.",
                    translationUr = "تمام تعریفیں اس اللہ کے لیے ہیں جس نے اپنے بندے پر کتاب نازل کی اور اس میں کوئی کجی نہ رکھی۔",
                    tafsirMuyassar = "Praise Allah who revealed the pure Quran, perfectly balanced without contradiction or crookedness.",
                    tafsirIbnKathir = "Recommended every Friday: 'Whoever reads Surah Al-Kahf on Friday, a light will shine for him between the two Fridays.'",
                    tafsirSadi = "Calling the Prophet 'His Servant' ('Abdihi) is the highest station of honor.",
                    asbabAlNuzul = "Revealed when Quraysh questioned the Prophet ﷺ regarding the Youth of the Cave, Dhul-Qarnayn, and the Soul.",
                    tajweedWaqfNote = "Notice the small 'Sakt' (brief breathless pause) at 'iwaja before 'qayyima' in Hafs recitation!",
                    qiraatNote = "Hafs makes a Sakt (pause without breath) on 'iwaja'; Warsh connects smoothly.",
                    rareWordMeaning = "'Iwaja: distortion, contradiction, or crookedness.",
                    mutashabihatRef = "Opening praise shared with Al-Fatihah (1:2).",
                    themeTag = "Heart & Reflection"
                ),
                QuranAyah(
                    number = 10,
                    arabic = "رَبَّنَا آتِنَا مِنْ لَدُنْكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا",
                    transliteration = "Rabbanā ātinā min ladunka raḥmatan wa hayyiʾ lanā min amrinā rashadā",
                    translationEn = "Our Lord, grant us from Yourself mercy and prepare for us from our affair right guidance.",
                    translationTr = "Rabbimiz! Bize katından bir rahmet ver ve işimizde bize doğruyu kolaylaştır.",
                    translationUr = "اے ہمارے رب! ہمیں اپنے پاس سے رحمت عطا فرما اور ہمارے کام میں درستگی پیدا فرما۔",
                    tafsirMuyassar = "The heartfelt supplication of the Youth of the Cave when seeking refuge in faith.",
                    tafsirIbnKathir = "A timeless Dua for youth, students, and anyone facing moral pressure.",
                    tafsirSadi = "Combining Rahmah (mercy) and Rashad (soundness of path) secures both worldly and eternal success.",
                    asbabAlNuzul = "Story of Ashab al-Kahf.",
                    tajweedWaqfNote = "Idgham bila Ghunnah in 'min ladunka'.",
                    qiraatNote = "Consistent in Hafs and Warsh.",
                    rareWordMeaning = "Rashada: uprightness and sound direction.",
                    mutashabihatRef = "Compare Al 'Imran (3:8) Rabbana Dua.",
                    themeTag = "Patience & Trust"
                )
            )
        ),
        QuranSurah(
            number = 55,
            nameAr = "سورة الرحمن (مختارات)",
            nameEn = "Ar-Rahman (The Most Merciful)",
            revelationPlace = "Madinah",
            juzNumber = 27,
            ayahCountTotal = 78,
            ayahs = listOf(
                QuranAyah(
                    number = 1,
                    arabic = "الرَّحْمَٰنُ ۝ عَلَّمَ الْقُرْآنَ ۝ خَلَقَ الْإِنْسَانَ ۝ عَلَّمَهُ الْبَيَانَ",
                    transliteration = "Ar-Raḥmān. ʿAllama l-Qurʾān. Khalaqa l-insān. ʿAllamahu l-bayān.",
                    translationEn = "The Most Merciful. Taught the Quran. Created humankind. Taught them eloquence.",
                    translationTr = "Rahmân, Kur'an'ı öğretti. İnsanı yarattı. Ona beyanı (ifade yeteneğini) öğretti.",
                    translationUr = "رحمٰن نے قرآن سکھایا۔ انسان کو پیدا کیا۔ اسے بولنا سکھایا۔",
                    tafsirMuyassar = "The greatest blessing of Al-Rahman is teaching the Quran, followed by creating humanity and gifting speech and understanding.",
                    tafsirIbnKathir = "Teaching the Quran is mentioned even before human creation because spiritual life depends on revelation.",
                    tafsirSadi = "Al-Bayan includes articulating truth, reading, and comprehending divine signs.",
                    asbabAlNuzul = "Revealed to remind creation of Allah's countless visible and hidden favors.",
                    tajweedWaqfNote = "Pause cleanly on the Sukun of each short rhythmic Ayah.",
                    qiraatNote = "Uniform rhythm across Qira'at.",
                    rareWordMeaning = "Al-Bayan: clarity of expression, speech, and discernment.",
                    mutashabihatRef = "Echoes Al-Alaq (96:4-5) on teaching.",
                    themeTag = "Mercy & Guidance"
                ),
                QuranAyah(
                    number = 13,
                    arabic = "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ",
                    transliteration = "Fa-bi-ayyi ālāʾi Rabbikumā tukadhdhibān",
                    translationEn = "So which of the favors of your Lord would you deny?",
                    translationTr = "O halde Rabbinizin hangi nimetlerini yalanlayabilirsiniz?",
                    translationUr = "تو تم اپنے رب کی کون کون سی نعمتوں کو جھٹلاؤ گے؟",
                    tafsirMuyassar = "Addressing both humankind and jinn, calling hearts to gratitude.",
                    tafsirIbnKathir = "The Prophet ﷺ praised the jinn for responding to this verse with: 'None of Your favors do we deny, our Lord! To You belongs all praise.'",
                    tafsirSadi = "Repeated 31 times to awaken awe and thankfulness.",
                    asbabAlNuzul = "Tirmidhi narrates the Prophet recited it to Companions and mentioned the jinn's grateful response.",
                    tajweedWaqfNote = "Madd Muttasil (4-5 counts) in 'aalaa-i'.",
                    qiraatNote = "Identical across Hafs & Warsh.",
                    rareWordMeaning = "Alaa': abundant, multifaceted blessings.",
                    mutashabihatRef = "Refrain repeated throughout Surah Ar-Rahman.",
                    themeTag = "Heart & Reflection"
                )
            )
        ),
        QuranSurah(
            number = 96,
            nameAr = "سورة العلق (سجدة التلاوة)",
            nameEn = "Al-'Alaq (First Revelation & Sajdah)",
            revelationPlace = "Makkah",
            juzNumber = 30,
            ayahCountTotal = 19,
            ayahs = listOf(
                QuranAyah(
                    number = 1,
                    arabic = "اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ",
                    transliteration = "Iqraʾ bismi Rabbika lladhī khalaq",
                    translationEn = "Read in the name of your Lord who created.",
                    translationTr = "Yaratan Rabbinin adıyla oku!",
                    translationUr = "پڑھیے اپنے رب کے نام سے جس نے پیدا کیا۔",
                    tafsirMuyassar = "The very first command revealed to Prophet Muhammad ﷺ in Cave Hira.",
                    tafsirIbnKathir = "Islam begins with reading, reflection, and anchoring all knowledge in Allah's name.",
                    tafsirSadi = "Knowledge without connection to the Creator loses its compass.",
                    asbabAlNuzul = "First revelation brought by Angel Jibril in Ramadan at Cave Hira (Sahih al-Bukhari).",
                    tajweedWaqfNote = "Qalqalah Sughra on the Qaf of 'Iqra'.",
                    qiraatNote = "Agreed across canonical Qira'at.",
                    rareWordMeaning = "Iqra': recite, proclaim, and read with reflection.",
                    mutashabihatRef = "First revelation of the Quran.",
                    themeTag = "Heart & Reflection"
                ),
                QuranAyah(
                    number = 19,
                    arabic = "كَلَّا لَا تُطِعْهُ وَاسْجُدْ وَاقْتَرِبْ ۩",
                    transliteration = "Kallā lā tuṭiʿhu wa-sjud wa-qtarib",
                    translationEn = "No! Do not obey him. But prostrate and draw near [to Allah].",
                    translationTr = "Hayır! Ona uyma; secde et ve (Rabbine) yaklaş!",
                    translationUr = "ہرگز نہیں! اس کا کہنا نہ مانیے، سجدہ کیجیے اور (اپنے رب کے) قریب ہو جائیے۔",
                    tafsirMuyassar = "Do not yield to intimidation against prayer; prostrate to Allah and grow closest to Him in Sujud.",
                    tafsirIbnKathir = "The Prophet ﷺ said: 'The closest a servant comes to his Lord is when he is in prostration, so increase supplication therein.'",
                    tafsirSadi = "Prostration is the summit of humility and spiritual nearness.",
                    asbabAlNuzul = "Revealed when Abu Jahl threatened to stop the Prophet ﷺ from praying at the Kaaba.",
                    tajweedWaqfNote = "Verse of Prostration (Sajdah Tilawah ۩) — perform one prostration saying 'Subhana Rabbiyal-A'la'.",
                    qiraatNote = "Agreed across Hafs and Warsh.",
                    rareWordMeaning = "Iqtarib: seek intimate spiritual closeness.",
                    mutashabihatRef = "One of the 15 Verses of Prostration in the Quran.",
                    themeTag = "Opening & Prayer",
                    sajdahRequired = true
                )
            )
        ),
        QuranSurah(
            number = 103,
            nameAr = "سورة العصر",
            nameEn = "Al-'Asr (The Declining Day)",
            revelationPlace = "Makkah",
            juzNumber = 30,
            ayahCountTotal = 3,
            ayahs = listOf(
                QuranAyah(
                    number = 1,
                    arabic = "وَالْعَصْرِ ۝ إِنَّ الْإِنْسَانَ لَفِي خُسْرٍ ۝ إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ",
                    transliteration = "Wal-ʿaṣr. Inna l-insāna lafī khusr. Illā lladhīna āmanū wa ʿamilu ṣ-ṣāliḥāti wa tawāṣaw bil-ḥaqqi wa tawāṣaw biṣ-ṣabr.",
                    translationEn = "By time, indeed, humankind is in loss, except for those who have believed and done righteous deeds and advised each other to truth and advised each other to patience.",
                    translationTr = "Asra yemin olsun ki insan gerçekten ziyan içindedir. Ancak iman edip salih amel işleyenler, birbirlerine hakkı ve sabrı tavsiye edenler müstesna.",
                    translationUr = "زمانے کی قسم! بے شک انسان گھاٹے میں ہے، سوائے ان لوگوں کے جو ایمان لائے اور نیک عمل کیے اور آپس میں حق اور صبر کی تلقین کی۔",
                    tafsirMuyassar = "Time bears witness that human life runs toward loss unless anchored in four pillars: Faith, Righteous Deeds, Mutual Counsel to Truth, and Patience.",
                    tafsirIbnKathir = "Imam al-Shafi'i remarked: 'If Allah had revealed no proof to His creation other than this Surah, it would have sufficed them.'",
                    tafsirSadi = "The first two pillars perfect oneself; the latter two build a righteous community.",
                    asbabAlNuzul = "Makkan revelation; Companions recited it to one another before parting.",
                    tajweedWaqfNote = "Ghunnah (2 counts) on the doubled Nun of 'Inna'; Qalqalah on Ba in 'bi-s-sabr' on pause.",
                    qiraatNote = "Consistent across Qira'at.",
                    rareWordMeaning = "Tawasaw: earnestly and mutually counsel one another.",
                    mutashabihatRef = "Compare Al-Balad (90:17) on advising to patience and mercy.",
                    themeTag = "Patience & Trust"
                )
            )
        ),
        QuranSurah(
            number = 112,
            nameAr = "سورة الإخلاص",
            nameEn = "Al-Ikhlas (Sincerity / Tawhid)",
            revelationPlace = "Makkah",
            juzNumber = 30,
            ayahCountTotal = 4,
            ayahs = listOf(
                QuranAyah(
                    number = 1,
                    arabic = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
                    transliteration = "Qul Huwa llāhu Aḥad. Allāhu ṣ-Ṣamad. Lam yalid wa lam yūlad. Wa lam yakun lahu kufuwan aḥad.",
                    translationEn = "Say, 'He is Allah, [who is] One, Allah, the Eternal Refuge. He neither begets nor is born, nor is there to Him any equivalent.'",
                    translationTr = "De ki: O, Allah'tır, bir tektir. Allah Samed'dir. Doğurmamış ve doğmamıştır. O'nun hiçbir dengi yoktur.",
                    translationUr = "کہہ دیجیے کہ وہ اللہ ایک ہے۔ اللہ بے نیاز ہے۔ نہ اس کی کوئی اولاد ہے اور نہ وہ کسی سے پیدا ہوا، اور اس کے برابر کوئی نہیں۔",
                    tafsirMuyassar = "Pure declaration of Allah's absolute oneness, self-sufficiency, and transcendence above all likeness.",
                    tafsirIbnKathir = "Equivalent to one-third of the Quran in meaning because the Quran revolves around Tawhid, rulings, and stories.",
                    tafsirSadi = "Al-Samad means the Master to whom all creation turns in every need while He needs none.",
                    asbabAlNuzul = "Revealed when idolaters asked the Prophet ﷺ to describe the lineage of his Lord.",
                    tajweedWaqfNote = "Qalqalah Kubra on the final Dal of Ahad, Samad, Yulad, Ahad when pausing.",
                    qiraatNote = "Hafs pronounces 'Kufuwan'; other Qira'at read 'Kuf'an'.",
                    rareWordMeaning = "Al-Samad: Perfect Master sought by all creation; Kufuwan: peer or equal.",
                    mutashabihatRef = "Recited with Al-Falaq and Al-Nas for morning, evening, and bedtime protection.",
                    themeTag = "Protection & Ruqyah"
                )
            )
        ),
        QuranSurah(
            number = 113,
            nameAr = "سورة الفلق",
            nameEn = "Al-Falaq (The Daybreak)",
            revelationPlace = "Makkah",
            juzNumber = 30,
            ayahCountTotal = 5,
            ayahs = listOf(
                QuranAyah(
                    number = 1,
                    arabic = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِنْ شَرِّ مَا خَلَقَ ۝ وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",
                    transliteration = "Qul aʿūdhu bi-Rabbi l-falaq, min sharri mā khalaq...",
                    translationEn = "Say, 'I seek refuge in the Lord of daybreak, from the evil of that which He created, and from the evil of darkness when it settles, and from the evil of the blowers in knots, and from the evil of an envier when he envies.'",
                    translationTr = "De ki: Sabahın Rabbine sığınırım; yarattığı şeylerin şerrinden, karanlığı çöktüğü zaman gecenin şerrinden, düğümlere üfleyenlerin şerrinden ve haset ettiği zaman hasetçinin şerrinden.",
                    translationUr = "کہہ دیجیے میں صبح کے رب کی پناہ مانگتا ہوں، ہر اس چیز کے شر سے جو اس نے پیدا کی، اور اندھیری رات کے شر سے جب وہ چھا جائے...",
                    tafsirMuyassar = "Seeking Allah's fortress against all external harm, night perils, sorcery, and envy.",
                    tafsirIbnKathir = "Together with Surah An-Nas, called Al-Mu'awwidhatayn — the Prophet ﷺ recited them every night before sleeping.",
                    tafsirSadi = "Just as Allah splits the dark night with dawn (Al-Falaq), He dispels every fear from His servant.",
                    asbabAlNuzul = "Revealed as divine protection and Ruqyah.",
                    tajweedWaqfNote = "Ikhfa Haqiqi in 'min sharri'; Qalqalah Kubra on Qaf, Ba, Dal at verse endings.",
                    qiraatNote = "Agreed across Hafs and Warsh.",
                    rareWordMeaning = "Ghasiq idha waqab: deep night when darkness settles in.",
                    mutashabihatRef = "Paired with Surah An-Nas (114).",
                    themeTag = "Protection & Ruqyah"
                )
            )
        ),
        QuranSurah(
            number = 114,
            nameAr = "سورة الناس",
            nameEn = "Al-Nas (Humankind)",
            revelationPlace = "Makkah",
            juzNumber = 30,
            ayahCountTotal = 6,
            ayahs = listOf(
                QuranAyah(
                    number = 1,
                    arabic = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",
                    transliteration = "Qul aʿūdhu bi-Rabbi n-nās, Maliki n-nās, Ilāhi n-nās...",
                    translationEn = "Say, 'I seek refuge in the Lord of humankind, the Sovereign of humankind, the God of humankind, from the evil of the retreating whisperer — who whispers into the breasts of humankind — from among the jinn and humankind.'",
                    translationTr = "De ki: İnsanların Rabbine, insanların Melikine, insanların İlahına sığınırım; sinsi vesvesecinin şerrinden...",
                    translationUr = "کہہ دیجیے میں تمام انسانوں کے رب کی، انسانوں کے بادشاہ کی، انسانوں کے معبود کی پناہ مانگتا ہوں...",
                    tafsirMuyassar = "Invoking Allah by His Lordship, Sovereignty, and Divinity for inner peace against obsessive whispers.",
                    tafsirIbnKathir = "Al-Khannas retreats whenever the servant remembers Allah (Dhikr).",
                    tafsirSadi = "Al-Falaq shields from outer harms; An-Nas shields from inner spiritual whispers.",
                    asbabAlNuzul = "Final Surah of the Mushaf, sealing the Quran with refuge in Allah.",
                    tajweedWaqfNote = "Hold Ghunnah for 2 counts on every 'An-Nas' (Nun Mushaddadah).",
                    qiraatNote = "Agreed across Hafs and Warsh.",
                    rareWordMeaning = "Al-Khannas: one who shrinks back and hides when Allah is remembered.",
                    mutashabihatRef = "Paired with Al-Falaq (113).",
                    themeTag = "Protection & Ruqyah"
                )
            )
        )
    )

    fun searchAyahs(query: String, themeFilter: String): List<Pair<QuranSurah, QuranAyah>> {
        val q = query.trim().lowercase()
        val results = mutableListOf<Pair<QuranSurah, QuranAyah>>()
        for (surah in surahs) {
            for (ayah in surah.ayahs) {
                val matchesTheme = themeFilter == "All" || ayah.themeTag == themeFilter
                val matchesQuery = q.isEmpty() ||
                    surah.nameEn.lowercase().contains(q) ||
                    surah.nameAr.contains(q) ||
                    ayah.arabic.contains(q) ||
                    ayah.translationEn.lowercase().contains(q) ||
                    ayah.transliteration.lowercase().contains(q)
                if (matchesTheme && matchesQuery) {
                    results.add(surah to ayah)
                }
            }
        }
        return results
    }

    private fun stripArabicDiacritics(input: String): String {
        return input
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED۞۩]"), "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ٱ", "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .trim()
    }

    fun compareRecitation(expectedArabic: String, spokenOrTyped: String): RecitationCheckResult {
        val expectedWords = expectedArabic.split(Regex("\\s+")).filter { it.isNotBlank() && it != "۝" }
        val spokenNormSet = stripArabicDiacritics(spokenOrTyped)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .toSet()

        if (expectedWords.isEmpty()) return RecitationCheckResult(100, emptyList(), 0)

        var matchedCount = 0
        val diffs = expectedWords.map { raw ->
            val norm = stripArabicDiacritics(raw)
            val isMatch = spokenNormSet.any { it == norm || (norm.length > 2 && it.contains(norm)) }
            if (isMatch) matchedCount++
            RecitationWordDiff(raw, isMatch)
        }
        val pct = (matchedCount * 100) / expectedWords.size
        return RecitationCheckResult(
            accuracyPercent = pct,
            wordDiffs = diffs,
            missingCount = expectedWords.size - matchedCount
        )
    }
}
