package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserSettingsDoc(
    val userId: String = "",
    val displayName: String = "Believer",
    val preferredLanguage: String = "en",
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
