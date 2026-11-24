package com.example.core.i18n

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val layoutDirection: LayoutDirection
) {
    ENGLISH("en", "English", LayoutDirection.Ltr),
    ARABIC("ar", "العربية", LayoutDirection.Rtl),
    TURKISH("tr", "Türkçe", LayoutDirection.Ltr),
    URDU("ur", "اردو", LayoutDirection.Rtl);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

data class LocalizedStrings(
    val appTitle: String,
    val tabPrayer: String,
    val tabQuran: String,
    val tabDhikr: String,
    val tabLearn: String,
    val tabTools: String,
    val nextPrayer: String,
    val qiblaCompass: String,
    val prayerTracker: String,
    val qiyamLastThird: String,
    val travelerMode: String,
    val qadaCounter: String,
    val mushafReader: String,
    val alMusammi: String,
    val khatmahPlan: String,
    val morningEveningAdhkar: String,
    val digitalTasbih: String,
    val namesOfAllah: String,
    val duaJournal: String,
    val communityDuaWall: String,
    val hijriCalendar: String,
    val ramadanMode: String,
    val knowledgeHub: String,
    val askAiCompanion: String,
    val zakatCalculator: String,
    val inheritanceCalculator: String,
    val pilgrimGuide: String,
    val settingsPrivacy: String,
    val signInWithGoogle: String,
    val signOut: String,
    val ameen: String,
    val save: String,
    val cancel: String
)

fun stringsFor(language: AppLanguage): LocalizedStrings = when (language) {
    AppLanguage.ENGLISH -> LocalizedStrings(
        appTitle = "Mihrab",
        tabPrayer = "Prayer",
        tabQuran = "Quran",
        tabDhikr = "Dhikr",
        tabLearn = "Learn",
        tabTools = "Tools",
        nextPrayer = "Next Prayer",
        qiblaCompass = "Qibla & AR",
        prayerTracker = "Daily Prayer Tracker",
        qiyamLastThird = "Last Third of Night",
        travelerMode = "Traveler Mode (Qasr & Jam')",
        qadaCounter = "Missed Prayers (Qada)",
        mushafReader = "Mushaf",
        alMusammi = "Al-Musammi'",
        khatmahPlan = "Khatmah",
        morningEveningAdhkar = "Adhkar",
        digitalTasbih = "Tasbih",
        namesOfAllah = "99 Names",
        duaJournal = "Journal",
        communityDuaWall = "Dua Wall",
        hijriCalendar = "Calendar",
        ramadanMode = "Ramadan",
        knowledgeHub = "Knowledge",
        askAiCompanion = "Ask Scholarly AI",
        zakatCalculator = "Zakat",
        inheritanceCalculator = "Inheritance",
        pilgrimGuide = "Hajj & Umrah",
        settingsPrivacy = "Settings & Privacy",
        signInWithGoogle = "Sign in with Google",
        signOut = "Sign Out",
        ameen = "Ameen",
        save = "Save",
        cancel = "Cancel"
    )
    AppLanguage.ARABIC -> LocalizedStrings(
        appTitle = "محراب",
        tabPrayer = "الصلاة",
        tabQuran = "القرآن",
        tabDhikr = "الأذكار",
        tabLearn = "المعرفة",
        tabTools = "الأدوات",
        nextPrayer = "الصلاة القادمة",
        qiblaCompass = "القبلة والواقع المعزز",
        prayerTracker = "متابعة الصلوات اليومية",
        qiyamLastThird = "الثلث الأخير من الليل",
        travelerMode = "وضع المسافر (قصر وجمع)",
        qadaCounter = "عداد القضاء",
        mushafReader = "المصحف",
        alMusammi = "المُسمِّع",
        khatmahPlan = "الختمة",
        morningEveningAdhkar = "الأذكار",
        digitalTasbih = "التسبيح",
        namesOfAllah = "الأسماء الحسنى",
        duaJournal = "مفكرة الدعاء",
        communityDuaWall = "حائط الدعاء",
        hijriCalendar = "التقويم",
        ramadanMode = "رمضان",
        knowledgeHub = "المكتبة",
        askAiCompanion = "اسأل المساعد",
        zakatCalculator = "الزكاة",
        inheritanceCalculator = "المواريث",
        pilgrimGuide = "الحج والعمرة",
        settingsPrivacy = "الإعدادات والخصوصية",
        signInWithGoogle = "تسجيل الدخول عبر Google",
        signOut = "تسجيل الخروج",
        ameen = "آمين",
        save = "حفظ",
        cancel = "إلغاء"
    )
    AppLanguage.TURKISH -> LocalizedStrings(
        appTitle = "Mihrab",
        tabPrayer = "Namaz",
        tabQuran = "Kur'an",
        tabDhikr = "Zikir",
        tabLearn = "İlim",
        tabTools = "Araçlar",
        nextPrayer = "Sonraki Vakit",
        qiblaCompass = "Kıble & AR",
        prayerTracker = "Günlük Namaz Takibi",
        qiyamLastThird = "Gecenin Son Üçte Biri",
        travelerMode = "Seferi Modu (Kasr & Cem)",
        qadaCounter = "Kaza Namazı Sayacı",
        mushafReader = "Mushaf",
        alMusammi = "Hafızlık Kontrol",
        khatmahPlan = "Hatim",
        morningEveningAdhkar = "Zikirler",
        digitalTasbih = "Tesbih",
        namesOfAllah = "Esmaül Hüsna",
        duaJournal = "Dua Günlüğü",
        communityDuaWall = "Dua Duvarı",
        hijriCalendar = "Takvim",
        ramadanMode = "Ramazan",
        knowledgeHub = "İlim",
        askAiCompanion = "Yapay Zeka",
        zakatCalculator = "Zekat",
        inheritanceCalculator = "Miras",
        pilgrimGuide = "Hac & Umre",
        settingsPrivacy = "Ayarlar & Gizlilik",
        signInWithGoogle = "Google ile Giriş Yap",
        signOut = "Çıkış Yap",
        ameen = "Amin",
        save = "Kaydet",
        cancel = "İptal"
    )
    AppLanguage.URDU -> LocalizedStrings(
        appTitle = "محراب",
        tabPrayer = "نماز",
        tabQuran = "قرآن",
        tabDhikr = "اذکار",
        tabLearn = "علم",
        tabTools = "ٹولز",
        nextPrayer = "اگلی نماز",
        qiblaCompass = "قبلہ اور اے آر",
        prayerTracker = "روزانہ نماز ٹریکر",
        qiyamLastThird = "رات کا آخری تہائی حصہ",
        travelerMode = "مسافر موڈ (قصر و جمع)",
        qadaCounter = "قضا نماز کاؤنٹ",
        mushafReader = "مصحف",
        alMusammi = "المسمع",
        khatmahPlan = "ختمِ قرآن",
        morningEveningAdhkar = "اذکار",
        digitalTasbih = "تسبیح",
        namesOfAllah = "اسماء الحسنیٰ",
        duaJournal = "دعا ڈائری",
        communityDuaWall = "دعا وال",
        hijriCalendar = "کیلنڈر",
        ramadanMode = "رمضان",
        knowledgeHub = "معلومات",
        askAiCompanion = "علمی معاون",
        zakatCalculator = "زکوٰۃ",
        inheritanceCalculator = "وراثت",
        pilgrimGuide = "حج و عمرہ",
        settingsPrivacy = "ترتیبات اور رازداری",
        signInWithGoogle = "Google سے سائن ان کریں",
        signOut = "سائن آؤٹ",
        ameen = "آمین",
        save = "محفوظ کریں",
        cancel = "منسوخ"
    )
}

fun formatNumerals(input: String, useIndic: Boolean): String {
    if (!useIndic) return input
    val indicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val sb = StringBuilder(input.length)
    for (ch in input) {
        if (ch in '0'..'9') {
            sb.append(indicDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}
