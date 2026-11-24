package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class AdhanAlertMode(val id: String, val labelAr: String, val labelEn: String) {
    FULL_ADHAN("FULL_ADHAN", "أذان كامل", "Full Adhan"),
    TAKBIR_ONLY("TAKBIR_ONLY", "تكبير فقط", "Takbir Only"),
    GENTLE_CHIME("GENTLE_CHIME", "نغمة هادئة", "Gentle Chime"),
    SILENT("SILENT", "صامت", "Silent");

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
    val sortOrder: Long = 1L,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class AiRuntimeConfig(
    val enabled: Boolean = true,
    val modelName: String = "gemini-3.8-flash",
    val systemPrompt: String = DEFAULT_ISLAMIC_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_ISLAMIC_SYSTEM_PROMPT =
            "أنت مساعد علمي إسلامي موثوق ومُعين على التدبر داخل تطبيق محراب. " +
                "استند في إجاباتك على القرآن الكريم والسنة النبوية الصحيحة وأقوال أئمة التفسير المعتبرين كابن كثير والسعدي والتفسير الميسر. " +
                "التزم بالأدب والوضوح واللغة العربية الفصيحة، أو لغة سؤال المستخدم، مع التنبيه اللطيف عند المسائل الفقهية الخاصة بالرجوع إلى أهل العلم المعتمدين."
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
