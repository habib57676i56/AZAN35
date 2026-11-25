package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

object AdminAccess {
    const val SUPER_ADMIN_EMAIL = "habiybullohy@gmail.com"

    fun isSuperAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return isSuperAdmin(user.email, user.isEmailVerified)
    }

    fun isSuperAdmin(email: String?, emailVerified: Boolean): Boolean {
        return email?.trim()?.lowercase() == SUPER_ADMIN_EMAIL && emailVerified
    }

    fun isAdmin(user: FirebaseUser?, userDoc: UserSettingsDoc?): Boolean {
        if (isSuperAdmin(user)) return true
        return user != null &&
            userDoc != null &&
            userDoc.adminRole == "ADMIN" &&
            !userDoc.accountSuspended
    }
}

enum class AdhanAlertMode(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val labelUz: String
) {
    FULL_ADHAN("FULL_ADHAN", "أذان كامل", "Full Adhan", "To'liq azon"),
    TAKBIR_ONLY("TAKBIR_ONLY", "تكبير فقط", "Takbir Only", "Faqat takbir"),
    GENTLE_CHIME("GENTLE_CHIME", "نغمة هادئة", "Gentle Chime", "Yengil ohang"),
    SILENT("SILENT", "صامت", "Silent", "Jimjit");

    fun labelFor(language: com.example.core.i18n.AppLanguage): String =
        language.pick(labelAr, labelEn, labelUz)

    companion object {
        fun fromId(id: String?): AdhanAlertMode =
            entries.firstOrNull { it.id == id } ?: FULL_ADHAN
    }
}

@Keep
@IgnoreExtraProperties
data class UserSettingsDoc(
    val userId: String = "",
    val displayName: String = "مؤمن",
    val email: String = "",
    val photoUrl: String = "",
    val accountVerified: Boolean = false,
    val accountSuspended: Boolean = false,
    val adminRole: String = "USER",
    val preferredLanguage: String = "ar",
    val themeMode: String = "SYSTEM",
    val mushafPaper: String = "WHITE",
    val fontScale: Double = 1.0,
    val lastReadPage: Long = 1L,
    val lastReadSurah: Long = 1L,
    val lastReadAyah: Long = 1L,
    val preferredReciterId: String = "ar.alafasy",
    val adhanEnabled: Boolean = true,
    val adhanVoiceId: String = "makkah",
    val perPrayerAdhanModes: Map<String, String> = mapOf(
        "fajr" to "FULL_ADHAN",
        "dhuhr" to "FULL_ADHAN",
        "asr" to "FULL_ADHAN",
        "maghrib" to "FULL_ADHAN",
        "isha" to "FULL_ADHAN"
    ),
    val customAdhanName: String = "",
    val calculationMethod: String = "MWL",
    val city: String = "Makkah",
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val hijriOffset: Long = 0L,
    val travelerMode: Boolean = false,
    val ramadanMode: Boolean = false,
    val kidsMode: Boolean = false,
    val qadaFajr: Long = 0L,
    val qadaDhuhr: Long = 0L,
    val qadaAsr: Long = 0L,
    val qadaMaghrib: Long = 0L,
    val qadaIsha: Long = 0L,
    val streakDays: Long = 1L,
    val khatmahPagesRead: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrayerLogDoc(
    val dateKey: String = "",
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val sunnahRawatib: Boolean = false,
    val duha: Boolean = false,
    val witr: Boolean = false,
    val qiyam: Boolean = false,
    val fastingType: String = "NONE",
    val khushuRating: Long = 3L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JournalEntryDoc(
    @DocumentId val id: String = "",
    val entryType: String = "DUA",
    val title: String = "",
    val body: String = "",
    val reference: String? = null,
    val answered: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BookmarkDoc(
    @DocumentId val id: String = "",
    val surahNumber: Long = 1L,
    val ayahNumber: Long = 1L,
    val pageNumber: Long? = null,
    val collectionName: String = "Favorites",
    val note: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TasbihCounterDoc(
    @DocumentId val id: String = "",
    val label: String = "",
    val arabicText: String = "",
    val count: Long = 0L,
    val target: Long = 33L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ObligationDoc(
    @DocumentId val id: String = "",
    val kind: String = "CHARITY",
    val title: String = "",
    val amount: Double = 0.0,
    val settled: Boolean = false,
    val notes: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReciterDoc(
    @DocumentId val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val styleAr: String = "مرتل",
    val styleEn: String = "Murattal",
    val editionId: String = "ar.alafasy",
    val everyAyahFolder: String = "Alafasy_128kbps",
    val cloudAudioBaseUrl: String = "",
    val sortOrder: Long = 1L,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdhkarLogDoc(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val category: String = "",
    val completedCount: Long = 0L,
    val targetCount: Long = 1L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiChatDoc(
    @DocumentId val id: String = "",
    val role: String = "user",
    val content: String = "",
    val surahRef: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminConfigDoc(
    @DocumentId val id: String = "",
    val configKey: String = "",
    val valueJson: String = "{}",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdhanVoiceDoc(
    @DocumentId val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val muezzin: String = "",
    val audioUrl: String = "",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdhkarItemDoc(
    @DocumentId val id: String = "",
    val category: String = "MORNING",
    val titleAr: String = "",
    val titleEn: String = "",
    val arabic: String = "",
    val transliteration: String = "",
    val translation: String = "",
    val targetCount: Long = 1L,
    val referenceAr: String = "",
    val referenceEn: String = "",
    val iconName: String = "",
    val sortOrder: Long = 1L,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

enum class VideoCategory(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val labelUz: String
) {
    EXPLAIN_ISLAM("EXPLAIN_ISLAM", "شرح الإسلام", "Explaining Islam", "Islomni tushuntirish"),
    ISLAMIC_CARTOON("ISLAMIC_CARTOON", "كرتون إسلامي", "Islamic Cartoons", "Islomiy multfilmlar"),
    QURAN_LESSONS("QURAN_LESSONS", "دروس القرآن", "Quran Lessons", "Qur'on darslari"),
    PROPHET_STORIES("PROPHET_STORIES", "قصص الأنبياء", "Prophet Stories", "Payg'ambarlar qissasi");

    fun labelFor(language: com.example.core.i18n.AppLanguage): String =
        language.pick(labelAr, labelEn, labelUz)

    companion object {
        fun fromId(id: String?): VideoCategory =
            entries.firstOrNull { it.id == id } ?: EXPLAIN_ISLAM
    }
}

@Keep
@IgnoreExtraProperties
data class VideoItemDoc(
    @DocumentId val id: String = "",
    val category: String = "EXPLAIN_ISLAM",
    val titleAr: String = "",
    val titleEn: String = "",
    val titleUz: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val descriptionUz: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val durationLabel: String = "",
    val sortOrder: Long = 1L,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class AiRuntimeConfig(
    val enabled: Boolean = true,
    val modelName: String = "gemini-3.8-flash",
    val systemPrompt: String = DEFAULT_ISLAMIC_SYSTEM_PROMPT,
    val dailyLimitPerUser: Int = 30
) {
    companion object {
        const val DEFAULT_ISLAMIC_SYSTEM_PROMPT =
            "أنت مساعد علمي إسلامي موثوق ومُعين على التدبر داخل تطبيق محراب. " +
                "استند في إجاباتك على القرآن الكريم والسنة النبوية الصحيحة وأقوال أئمة التفسير المعتبرين كابن كثير والسعدي والتفسير الميسر. " +
                "التزم بالأدب والوضوح واللغة العربية الفصيحة، أو لغة سؤال المستخدم، مع التنبيه اللطيف عند المسائل الفقهية الخاصة بالرجوع إلى أهل العلم المعتمدين."
    }
}

data class QuranDefaultsConfig(
    val quranPageApiBase: String = "https://api.quran.com/api/v4",
    val quranFallbackApiBase: String = "https://api.alquran.cloud/v1",
    val audioCdnBase: String = "https://everyayah.com/data",
    val defaultReciterId: String = "ar.alafasy",
    val defaultPaper: String = "WHITE",
    val defaultThemeMode: String = "SYSTEM",
    val defaultFontScale: Double = 1.0
)

data class AppOperationsConfig(
    val bannerEnabled: Boolean = false,
    val bannerTitleAr: String = "",
    val bannerTitleEn: String = "",
    val bannerTitleUz: String = "",
    val bannerBodyAr: String = "",
    val bannerBodyEn: String = "",
    val bannerBodyUz: String = "",
    val bannerImageUrl: String = "",
    val bannerLinkUrl: String = "",
    val bannerCtaAr: String = "",
    val bannerCtaEn: String = "",
    val bannerCtaUz: String = "",
    val bannerTone: String = "INFO",
    val aiAssistantEnabled: Boolean = true,
    val quranAudioEnabled: Boolean = true,
    val communityDuaWallEnabled: Boolean = true,
    val familyKhatmahEnabled: Boolean = true,
    val arQiblaEnabled: Boolean = true,
    val kidsModeEnabled: Boolean = true,
    val customAdhanUploadEnabled: Boolean = true,
    val smartShortcutsEnabled: Boolean = true,
    val liveVersesCardEnabled: Boolean = true,
    val nearbyMosquesEnabled: Boolean = true,
    val persistentPrayerNotificationAllowed: Boolean = true,
    val videosEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val supportWhatsapp: String = DEFAULT_SUPPORT_WHATSAPP,
    val supportTelegram: String = DEFAULT_SUPPORT_TELEGRAM,
    val supportEmail: String = DEFAULT_SUPPORT_EMAIL,
    val aboutTitleAr: String = DEFAULT_ABOUT_TITLE_AR,
    val aboutTitleEn: String = DEFAULT_ABOUT_TITLE_EN,
    val aboutTitleUz: String = DEFAULT_ABOUT_TITLE_UZ,
    val aboutBodyAr: String = DEFAULT_ABOUT_BODY_AR,
    val aboutBodyEn: String = DEFAULT_ABOUT_BODY_EN,
    val aboutBodyUz: String = DEFAULT_ABOUT_BODY_UZ
) {
    companion object {
        const val DEFAULT_SUPPORT_WHATSAPP = "+1 (581) 509-1598"
        const val DEFAULT_SUPPORT_TELEGRAM = "+963 984 215 420"
        const val DEFAULT_SUPPORT_EMAIL = "habiybullohy@gmail.com"
        const val DEFAULT_ABOUT_TITLE_AR = "عن أسامة وتطبيق محراب"
        const val DEFAULT_ABOUT_TITLE_EN = "About Osama & Mihrab"
        const val DEFAULT_ABOUT_TITLE_UZ = "Usoma va Mihrob haqida"
        const val DEFAULT_ABOUT_BODY_AR =
            "تطبيق «محراب» من تطوير وإشراف أسامة، صُمّم ليكون رفيق المسلم اليومي في تلاوة القرآن الكريم بمصحف المدينة الموثوق، ومواقيت الصلاة الدقيقة، والأذكار اليومية، وأدوات الزكاة والعبادات بتجربة هادئة وخالية من الإعلانات المزعجة. نسعد بتواصلكم وملاحظاتكم واقتراحاتكم في أي وقت."
        const val DEFAULT_ABOUT_BODY_EN =
            "Mihrab is crafted and maintained by Osama as a peaceful, ad-free Islamic sanctuary bringing together the complete 604-page Madinah Mushaf, accurate prayer times, authentic Adhkar, and worship utilities. We warmly welcome your feedback, questions, and suggestions."
        const val DEFAULT_ABOUT_BODY_UZ =
            "«Mihrob» ilovasi Usoma tomonidan musulmonlarning kundalik ibodatlari uchun tinch, reklamasiz hamroh sifatida yaratilgan: 604 sahifalik Madina Mushafi, aniq namoz vaqtlari, sahih zikrlar va qulay hisoblagichlar. Fikr va takliflaringiz uchun har doim aloqadamiz."
    }
}

@Keep
@IgnoreExtraProperties
data class DuaWallPostDoc(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val alias: String = "Anonymous Believer",
    val category: String = "GENERAL",
    val requestText: String = "",
    val visibility: String = "COMMUNITY",
    val ameenCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KhatmahGroupDoc(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val title: String = "",
    val joinCode: String = "",
    val memberUids: List<String> = emptyList(),
    val completedJuz: List<Long> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
