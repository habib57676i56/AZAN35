package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.local.AdhanVoiceDao
import com.example.data.local.AdhanVoiceEntity
import com.example.data.local.AdhkarDao
import com.example.data.local.AdhkarEntity
import com.example.data.local.LocalPrefsDao
import com.example.data.local.LocalUserPrefsEntity
import com.example.data.local.ReciterDao
import com.example.data.local.ReciterEntity
import com.example.domain.AdhkarAndAdhanCatalog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class MihrabRepository(
    private val db: FirebaseFirestore,
    private val localPrefsDao: LocalPrefsDao? = null,
    private val reciterDao: ReciterDao? = null,
    private val adhanVoiceDao: AdhanVoiceDao? = null,
    private val adhkarDao: AdhkarDao? = null,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> safeFirestoreFlow(
        path: String,
        operationType: OperationType,
        uid: String,
        flowProvider: () -> Flow<T>
    ): Flow<T> = flow {
        emitAll(
            flowProvider().catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, operationType, path)
                }
                throw error
            }
        )
    }

    // --- USER SETTINGS (SYNCED FIRESTORE + ROOM LOCAL CACHE) ---
    fun observeUserSettings(uid: String): Flow<UserSettingsDoc?> {
        val path = "users/$uid"
        return safeFirestoreFlow(path, OperationType.GET, uid) {
            db.collection("users").document(uid).snapshots().map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserSettingsDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }.onEach { doc ->
                if (doc != null && localPrefsDao != null) {
                    val existingLocal = localPrefsDao.getPrefs(uid)
                    localPrefsDao.upsertPrefs(
                        LocalUserPrefsEntity(
                            userId = uid,
                            preferredLanguage = doc.preferredLanguage,
                            themeMode = doc.themeMode,
                            mushafPaper = doc.mushafPaper,
                            fontScale = doc.fontScale.toFloat(),
                            lastReadPage = doc.lastReadPage.toInt().coerceIn(1, 604),
                            lastReadSurah = doc.lastReadSurah.toInt().coerceIn(1, 114),
                            lastReadAyah = doc.lastReadAyah.toInt().coerceIn(1, 286),
                            preferredReciterId = doc.preferredReciterId,
                            adhanEnabled = doc.adhanEnabled,
                            adhanVoiceId = doc.adhanVoiceId,
                            customAdhanName = doc.customAdhanName,
                            customAdhanLocalPath = existingLocal?.customAdhanLocalPath ?: "",
                            perPrayerAdhanJson = JSONObject(doc.perPrayerAdhanModes).toString(),
                            khatmahPagesRead = doc.khatmahPagesRead.toInt().coerceIn(0, 604)
                        )
                    )
                }
            }
        }
    }

    fun observeLocalPrefs(uid: String): Flow<LocalUserPrefsEntity?> {
        return localPrefsDao?.observePrefs(uid) ?: flow { emit(null) }
    }

    suspend fun ensureUserSettings(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "مؤمن" }.take(60)
                val defaultsSnap = runCatching {
                    db.collection("adminConfigs").document("quran_defaults").get().await()
                }.getOrNull()
                val defaultsObj = runCatching {
                    JSONObject(defaultsSnap?.getString("valueJson") ?: "{}")
                }.getOrNull()
                val defReciter = defaultsObj?.optString("defaultReciterId")?.takeIf { it.isNotBlank() } ?: "ar.alafasy"
                val defPaper = defaultsObj?.optString("defaultPaper")?.takeIf { it in listOf("WHITE", "SEPIA", "DARK") } ?: "WHITE"
                val defTheme = defaultsObj?.optString("defaultThemeMode")?.takeIf { it in listOf("LIGHT", "DARK", "SYSTEM") } ?: "SYSTEM"
                val defScale = defaultsObj?.optDouble("defaultFontScale", 1.0)?.coerceIn(0.85, 1.35) ?: 1.0
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "preferredLanguage" to "ar",
                    "themeMode" to defTheme,
                    "mushafPaper" to defPaper,
                    "fontScale" to defScale,
                    "lastReadPage" to 1L,
                    "lastReadSurah" to 1L,
                    "lastReadAyah" to 1L,
                    "preferredReciterId" to defReciter,
                    "adhanEnabled" to true,
                    "adhanVoiceId" to "makkah",
                    "perPrayerAdhanModes" to mapOf(
                        "fajr" to "FULL_ADHAN",
                        "dhuhr" to "FULL_ADHAN",
                        "asr" to "FULL_ADHAN",
                        "maghrib" to "FULL_ADHAN",
                        "isha" to "FULL_ADHAN"
                    ),
                    "customAdhanName" to "",
                    "calculationMethod" to "MWL",
                    "city" to "Makkah",
                    "latitude" to 21.4225,
                    "longitude" to 39.8262,
                    "hijriOffset" to 0L,
                    "travelerMode" to false,
                    "ramadanMode" to false,
                    "kidsMode" to false,
                    "qadaFajr" to 0L,
                    "qadaDhuhr" to 0L,
                    "qadaAsr" to 0L,
                    "qadaMaghrib" to 0L,
                    "qadaIsha" to 0L,
                    "streakDays" to 1L,
                    "khatmahPagesRead" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                // Backfill any newly added Phase 1 & Phase 2 fields on existing user documents safely
                val data = snap.data ?: emptyMap()
                val backfill = mutableMapOf<String, Any>()
                if (!data.containsKey("themeMode")) backfill["themeMode"] = "SYSTEM"
                if (!data.containsKey("mushafPaper")) backfill["mushafPaper"] = "WHITE"
                if (!data.containsKey("fontScale")) backfill["fontScale"] = 1.0
                if (!data.containsKey("lastReadPage")) backfill["lastReadPage"] = 1L
                if (!data.containsKey("lastReadSurah")) backfill["lastReadSurah"] = 1L
                if (!data.containsKey("lastReadAyah")) backfill["lastReadAyah"] = 1L
                if (!data.containsKey("preferredReciterId")) backfill["preferredReciterId"] = "ar.alafasy"
                if (!data.containsKey("adhanEnabled")) backfill["adhanEnabled"] = true
                if (!data.containsKey("adhanVoiceId")) backfill["adhanVoiceId"] = "makkah"
                if (!data.containsKey("perPrayerAdhanModes")) {
                    backfill["perPrayerAdhanModes"] = mapOf(
                        "fajr" to "FULL_ADHAN",
                        "dhuhr" to "FULL_ADHAN",
                        "asr" to "FULL_ADHAN",
                        "maghrib" to "FULL_ADHAN",
                        "isha" to "FULL_ADHAN"
                    )
                }
                if (!data.containsKey("customAdhanName")) backfill["customAdhanName"] = ""
                if (backfill.isNotEmpty()) {
                    backfill["updatedAt"] = FieldValue.serverTimestamp()
                    docRef.update(backfill).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserSettings(fields: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid"
        return try {
            requireUserId()
            // Mirror immediately into local SQLite for instant zero-jank reactivity
            localPrefsDao?.let { dao ->
                val cur = dao.getPrefs(uid) ?: LocalUserPrefsEntity(userId = uid)
                val updatedModesJson = (fields["perPrayerAdhanModes"] as? Map<*, *>)?.let { m ->
                    val obj = JSONObject()
                    m.forEach { (k, v) -> if (k != null && v != null) obj.put(k.toString(), v.toString()) }
                    obj.toString()
                } ?: cur.perPrayerAdhanJson
                dao.upsertPrefs(
                    cur.copy(
                        preferredLanguage = (fields["preferredLanguage"] as? String) ?: cur.preferredLanguage,
                        themeMode = (fields["themeMode"] as? String) ?: cur.themeMode,
                        mushafPaper = (fields["mushafPaper"] as? String) ?: cur.mushafPaper,
                        fontScale = (fields["fontScale"] as? Number)?.toFloat() ?: cur.fontScale,
                        lastReadPage = (fields["lastReadPage"] as? Number)?.toInt() ?: cur.lastReadPage,
                        lastReadSurah = (fields["lastReadSurah"] as? Number)?.toInt() ?: cur.lastReadSurah,
                        lastReadAyah = (fields["lastReadAyah"] as? Number)?.toInt() ?: cur.lastReadAyah,
                        preferredReciterId = (fields["preferredReciterId"] as? String) ?: cur.preferredReciterId,
                        adhanEnabled = (fields["adhanEnabled"] as? Boolean) ?: cur.adhanEnabled,
                        adhanVoiceId = (fields["adhanVoiceId"] as? String) ?: cur.adhanVoiceId,
                        customAdhanName = (fields["customAdhanName"] as? String) ?: cur.customAdhanName,
                        perPrayerAdhanJson = updatedModesJson,
                        khatmahPagesRead = (fields["khatmahPagesRead"] as? Number)?.toInt() ?: cur.khatmahPagesRead,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            val clean = fields.toMutableMap()
            clean.remove("userId")
            clean.remove("createdAt")
            clean["updatedAt"] = FieldValue.serverTimestamp()
            db.collection("users").document(uid).update(clean).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCustomAdhanLocalFile(localPath: String, displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            val cleanName = displayName.trim().take(120)
            localPrefsDao?.let { dao ->
                val cur = dao.getPrefs(uid) ?: LocalUserPrefsEntity(userId = uid)
                dao.upsertPrefs(
                    cur.copy(
                        customAdhanLocalPath = localPath,
                        customAdhanName = cleanName,
                        adhanVoiceId = "custom",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            updateUserSettings(
                mapOf(
                    "customAdhanName" to cleanName,
                    "adhanVoiceId" to "custom"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- RECITERS CATALOG (DYNAMIC FIRESTORE + SEEDED ROOM SQLITE CACHE) ---
    suspend fun ensureDefaultRecitersSeededLocally() {
        val dao = reciterDao ?: return
        if (dao.getActiveReciters().isEmpty()) {
            dao.upsertAll(DEFAULT_RECITERS)
        }
    }

    fun observeReciters(uid: String): Flow<List<ReciterEntity>> {
        val path = "reciters"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("reciters")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    val docs = snap.toObjects(ReciterDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (docs.isNotEmpty()) {
                        val mapped = docs.sortedBy { it.sortOrder }.map { d ->
                            ReciterEntity(
                                id = d.id.ifBlank { d.editionId },
                                nameAr = d.nameAr,
                                nameEn = d.nameEn,
                                styleAr = d.styleAr,
                                styleEn = d.styleEn,
                                editionId = d.editionId,
                                everyAyahFolder = d.everyAyahFolder,
                                sortOrder = d.sortOrder.toInt(),
                                active = d.active
                            )
                        }
                        reciterDao?.upsertAll(mapped)
                        mapped
                    } else {
                        reciterDao?.getActiveReciters()?.takeIf { it.isNotEmpty() } ?: DEFAULT_RECITERS
                    }
                }
        }
    }

    // --- PHASE 2 CATALOGS: ADHAN VOICES & 12-CATEGORY ADHKAR ENCYCLOPEDIA ---
    suspend fun ensureAdhanAndAdhkarSeededLocally() {
        adhanVoiceDao?.let { dao ->
            if (dao.getActiveVoices().isEmpty()) {
                dao.upsertAll(AdhkarAndAdhanCatalog.defaultAdhanVoices)
            }
        }
        adhkarDao?.let { dao ->
            if (dao.getAllActive().isEmpty()) {
                dao.upsertAll(AdhkarAndAdhanCatalog.defaultAdhkarEntities)
            }
        }
    }

    fun observeAdhanVoices(uid: String): Flow<List<AdhanVoiceEntity>> {
        val path = "adhanVoices"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("adhanVoices")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    val docs = snap.toObjects(AdhanVoiceDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (docs.isNotEmpty()) {
                        val mapped = docs.map { d ->
                            AdhanVoiceEntity(
                                id = d.id.ifBlank { d.nameEn.lowercase().replace(" ", "_") },
                                nameAr = d.nameAr,
                                nameEn = d.nameEn,
                                muezzin = d.muezzin,
                                audioUrl = d.audioUrl,
                                active = d.active
                            )
                        }
                        adhanVoiceDao?.upsertAll(mapped)
                        mapped
                    } else {
                        adhanVoiceDao?.getActiveVoices()?.takeIf { it.isNotEmpty() }
                            ?: AdhkarAndAdhanCatalog.defaultAdhanVoices
                    }
                }
        }
    }

    fun observeAdhkarItems(uid: String): Flow<List<AdhkarEntity>> {
        val path = "adhkarItems"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("adhkarItems")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    val docs = snap.toObjects(AdhkarItemDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (docs.isNotEmpty()) {
                        val mapped = docs.sortedBy { it.sortOrder }.map { d ->
                            AdhkarEntity(
                                id = d.id,
                                category = d.category,
                                titleAr = d.titleAr,
                                titleEn = d.titleEn,
                                arabic = d.arabic,
                                transliteration = d.transliteration,
                                translation = d.translation,
                                targetCount = d.targetCount.toInt().coerceIn(1, 1000),
                                referenceAr = d.referenceAr,
                                referenceEn = d.referenceEn,
                                iconName = d.iconName,
                                sortOrder = d.sortOrder.toInt(),
                                active = d.active
                            )
                        }
                        adhkarDao?.upsertAll(mapped)
                        mapped
                    } else {
                        adhkarDao?.getAllActive()?.takeIf { it.isNotEmpty() }
                            ?: AdhkarAndAdhanCatalog.defaultAdhkarEntities
                    }
                }
        }
    }

    fun observeAiConfig(uid: String): Flow<AiRuntimeConfig> {
        val path = "adminConfigs/ai_settings"
        return safeFirestoreFlow(path, OperationType.GET, uid) {
            db.collection("adminConfigs").document("ai_settings")
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) {
                        AiRuntimeConfig()
                    } else {
                        val valueJson = snap.getString("valueJson") ?: "{}"
                        try {
                            val obj = JSONObject(valueJson)
                            val rawModel = obj.optString("modelName", "gemini-3.8-flash").trim()
                            val safeModel = if (rawModel.startsWith("gemini-3") || rawModel.startsWith("gemini-4")) {
                                rawModel
                            } else {
                                "gemini-3.8-flash"
                            }
                            AiRuntimeConfig(
                                enabled = obj.optBoolean("enabled", true),
                                modelName = safeModel,
                                systemPrompt = obj.optString("systemPrompt")
                                    .takeIf { it.isNotBlank() }
                                    ?: AiRuntimeConfig.DEFAULT_ISLAMIC_SYSTEM_PROMPT,
                                dailyLimitPerUser = obj.optInt("dailyLimitPerUser", 30).coerceIn(5, 500)
                            )
                        } catch (_: Exception) {
                            AiRuntimeConfig()
                        }
                    }
                }
        }
    }

    fun observeQuranDefaults(uid: String): Flow<QuranDefaultsConfig> {
        val path = "adminConfigs/quran_defaults"
        return safeFirestoreFlow(path, OperationType.GET, uid) {
            db.collection("adminConfigs").document("quran_defaults")
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) {
                        QuranDefaultsConfig()
                    } else {
                        val valueJson = snap.getString("valueJson") ?: "{}"
                        try {
                            val obj = JSONObject(valueJson)
                            QuranDefaultsConfig(
                                quranPageApiBase = obj.optString("quranPageApiBase", "https://api.quran.com/api/v4")
                                    .takeIf { it.startsWith("https://") } ?: "https://api.quran.com/api/v4",
                                quranFallbackApiBase = obj.optString("quranFallbackApiBase", "https://api.alquran.cloud/v1")
                                    .takeIf { it.startsWith("https://") } ?: "https://api.alquran.cloud/v1",
                                audioCdnBase = obj.optString("audioCdnBase", "https://everyayah.com/data")
                                    .takeIf { it.startsWith("https://") } ?: "https://everyayah.com/data",
                                defaultReciterId = obj.optString("defaultReciterId", "ar.alafasy").ifBlank { "ar.alafasy" },
                                defaultPaper = obj.optString("defaultPaper", "WHITE")
                                    .takeIf { it in listOf("WHITE", "SEPIA", "DARK") } ?: "WHITE",
                                defaultThemeMode = obj.optString("defaultThemeMode", "SYSTEM")
                                    .takeIf { it in listOf("LIGHT", "DARK", "SYSTEM") } ?: "SYSTEM",
                                defaultFontScale = obj.optDouble("defaultFontScale", 1.0).coerceIn(0.85, 1.35)
                            )
                        } catch (_: Exception) {
                            QuranDefaultsConfig()
                        }
                    }
                }
        }
    }

    fun observeAppOperations(uid: String): Flow<AppOperationsConfig> {
        val path = "adminConfigs/app_operations"
        return safeFirestoreFlow(path, OperationType.GET, uid) {
            db.collection("adminConfigs").document("app_operations")
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) {
                        AppOperationsConfig()
                    } else {
                        val valueJson = snap.getString("valueJson") ?: "{}"
                        try {
                            val obj = JSONObject(valueJson)
                            AppOperationsConfig(
                                bannerEnabled = obj.optBoolean("bannerEnabled", false),
                                bannerTitleAr = obj.optString("bannerTitleAr", ""),
                                bannerTitleEn = obj.optString("bannerTitleEn", ""),
                                bannerBodyAr = obj.optString("bannerBodyAr", ""),
                                bannerBodyEn = obj.optString("bannerBodyEn", ""),
                                bannerTone = obj.optString("bannerTone", "INFO"),
                                aiAssistantEnabled = obj.optBoolean("aiAssistantEnabled", true),
                                quranAudioEnabled = obj.optBoolean("quranAudioEnabled", true),
                                communityDuaWallEnabled = obj.optBoolean("communityDuaWallEnabled", true),
                                familyKhatmahEnabled = obj.optBoolean("familyKhatmahEnabled", true),
                                arQiblaEnabled = obj.optBoolean("arQiblaEnabled", true),
                                kidsModeEnabled = obj.optBoolean("kidsModeEnabled", true),
                                customAdhanUploadEnabled = obj.optBoolean("customAdhanUploadEnabled", true),
                                maintenanceMode = obj.optBoolean("maintenanceMode", false)
                            )
                        } catch (_: Exception) {
                            AppOperationsConfig()
                        }
                    }
                }
        }
    }

    // --- PRAYER LOGS ---
    fun observePrayerLogs(uid: String): Flow<List<PrayerLogDoc>> {
        val path = "users/$uid/prayerLogs"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("users").document(uid).collection("prayerLogs")
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .limit(60)
                .snapshots()
                .map { snap -> snap.toObjects(PrayerLogDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun upsertPrayerLog(log: PrayerLogDoc): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/prayerLogs/${log.dateKey}"
        return try {
            requireUserId()
            require(log.dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Invalid date key" }
            val docRef = db.collection("users").document(uid).collection("prayerLogs").document(log.dateKey)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "fajr" to log.fajr,
                        "dhuhr" to log.dhuhr,
                        "asr" to log.asr,
                        "maghrib" to log.maghrib,
                        "isha" to log.isha,
                        "sunnahRawatib" to log.sunnahRawatib,
                        "duha" to log.duha,
                        "witr" to log.witr,
                        "qiyam" to log.qiyam,
                        "fastingType" to log.fastingType,
                        "khushuRating" to log.khushuRating.coerceIn(0L, 5L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "dateKey" to log.dateKey,
                        "fajr" to log.fajr,
                        "dhuhr" to log.dhuhr,
                        "asr" to log.asr,
                        "maghrib" to log.maghrib,
                        "isha" to log.isha,
                        "sunnahRawatib" to log.sunnahRawatib,
                        "duha" to log.duha,
                        "witr" to log.witr,
                        "qiyam" to log.qiyam,
                        "fastingType" to log.fastingType,
                        "khushuRating" to log.khushuRating.coerceIn(0L, 5L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- JOURNAL ENTRIES (DUA, VERSE NOTES, GRATITUDE) ---
    fun observeJournalEntries(uid: String): Flow<List<JournalEntryDoc>> {
        val path = "users/$uid/duaJournal"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("users").document(uid).collection("duaJournal")
                .snapshots()
                .map { snap -> snap.toObjects(JournalEntryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun addJournalEntry(entryType: String, title: String, body: String, reference: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/duaJournal"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanBody = body.trim().take(2000)
            require(cleanTitle.isNotEmpty() && cleanBody.isNotEmpty()) { "Title and reflection cannot be empty" }
            val cleanRef = reference?.trim()?.take(120)?.takeIf { it.isNotEmpty() }
            val docRef = db.collection("users").document(uid).collection("duaJournal").document()
            val payload = mapOf(
                "entryType" to entryType,
                "title" to cleanTitle,
                "body" to cleanBody,
                "reference" to cleanRef,
                "answered" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleJournalAnswered(entryId: String, answered: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/duaJournal/$entryId"
        return try {
            requireUserId()
            require(entryId.isNotBlank()) { "Invalid journal ID" }
            db.collection("users").document(uid).collection("duaJournal").document(entryId)
                .update(mapOf("answered" to answered, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/duaJournal/$entryId"
        return try {
            requireUserId()
            require(entryId.isNotBlank()) { "Invalid journal ID" }
            db.collection("users").document(uid).collection("duaJournal").document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- BOOKMARKS ---
    fun observeBookmarks(uid: String): Flow<List<BookmarkDoc>> {
        val path = "users/$uid/bookmarks"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("users").document(uid).collection("bookmarks")
                .snapshots()
                .map { snap -> snap.toObjects(BookmarkDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun addBookmark(
        surahNumber: Int,
        ayahNumber: Int,
        collectionName: String,
        note: String?,
        pageNumber: Int? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val docId = "bm_${surahNumber}_$ayahNumber"
        val path = "users/$uid/bookmarks/$docId"
        return try {
            requireUserId()
            val cleanCol = collectionName.trim().ifBlank { "Favorites" }.take(60)
            val cleanNote = note?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
            val safePage = pageNumber?.coerceIn(1, 604)?.toLong()
            val docRef = db.collection("users").document(uid).collection("bookmarks").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "collectionName" to cleanCol,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanNote != null) updates["note"] = cleanNote
                if (safePage != null) updates["pageNumber"] = safePage
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "surahNumber" to surahNumber.toLong(),
                    "ayahNumber" to ayahNumber.toLong(),
                    "pageNumber" to safePage,
                    "collectionName" to cleanCol,
                    "note" to cleanNote,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBookmark(bookmarkId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/bookmarks/$bookmarkId"
        return try {
            requireUserId()
            require(bookmarkId.isNotBlank()) { "Invalid bookmark ID" }
            db.collection("users").document(uid).collection("bookmarks").document(bookmarkId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- TASBIH COUNTERS ---
    fun observeTasbihCounters(uid: String): Flow<List<TasbihCounterDoc>> {
        val path = "users/$uid/tasbihCounters"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("users").document(uid).collection("tasbihCounters")
                .snapshots()
                .map { snap -> snap.toObjects(TasbihCounterDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun upsertTasbihCounter(id: String, label: String, arabicText: String, count: Long, target: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val safeId = id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64).ifBlank { "custom_dhikr" }
        val path = "users/$uid/tasbihCounters/$safeId"
        return try {
            requireUserId()
            val docRef = db.collection("users").document(uid).collection("tasbihCounters").document(safeId)
            val snap = docRef.get().await()
            val cleanLabel = label.trim().ifBlank { "Dhikr" }.take(80)
            val cleanArabic = arabicText.trim().take(200)
            val safeCount = count.coerceIn(0L, 10_000_000L)
            val safeTarget = target.coerceIn(1L, 100_000L)
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "label" to cleanLabel,
                        "arabicText" to cleanArabic,
                        "count" to safeCount,
                        "target" to safeTarget,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "label" to cleanLabel,
                        "arabicText" to cleanArabic,
                        "count" to safeCount,
                        "target" to safeTarget,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- OBLIGATIONS (CHARITY, DEBTS, WASIYYAH) ---
    fun observeObligations(uid: String): Flow<List<ObligationDoc>> {
        val path = "users/$uid/obligations"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("users").document(uid).collection("obligations")
                .snapshots()
                .map { snap -> snap.toObjects(ObligationDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun addObligation(kind: String, title: String, amount: Double, notes: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/obligations"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Title cannot be empty" }
            val cleanNotes = notes?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
            val payload = mapOf(
                "kind" to kind,
                "title" to cleanTitle,
                "amount" to amount.coerceAtLeast(0.0),
                "settled" to (kind == "CHARITY"),
                "notes" to cleanNotes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("users").document(uid).collection("obligations").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleObligationSettled(doc: ObligationDoc): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/obligations/${doc.id}"
        return try {
            requireUserId()
            require(doc.id.isNotBlank()) { "Invalid obligation ID" }
            db.collection("users").document(uid).collection("obligations").document(doc.id)
                .update(mapOf("settled" to !doc.settled, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteObligation(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/obligations/$id"
        return try {
            requireUserId()
            require(id.isNotBlank()) { "Invalid obligation ID" }
            db.collection("users").document(uid).collection("obligations").document(id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- PHASE 2: ADHKAR LOGS & AI CHATS ---
    fun observeAdhkarLogs(uid: String, dateKey: String): Flow<List<AdhkarLogDoc>> {
        val path = "users/$uid/adhkarLogs"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("users").document(uid).collection("adhkarLogs")
                .whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snap -> snap.toObjects(AdhkarLogDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun saveAdhkarLog(dateKey: String, category: String, completedCount: Long, targetCount: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val docId = "${dateKey}_${category.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}".take(64)
        val path = "users/$uid/adhkarLogs/$docId"
        return try {
            requireUserId()
            val ref = db.collection("users").document(uid).collection("adhkarLogs").document(docId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "completedCount" to completedCount.coerceIn(0L, 10000L),
                        "targetCount" to targetCount.coerceIn(1L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "dateKey" to dateKey,
                        "category" to category.take(60),
                        "completedCount" to completedCount.coerceIn(0L, 10000L),
                        "targetCount" to targetCount.coerceIn(1L, 10000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeAiChats(uid: String): Flow<List<AiChatDoc>> {
        val path = "users/$uid/aiChats"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("users").document(uid).collection("aiChats")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(60)
                .snapshots()
                .map { snap -> snap.toObjects(AiChatDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun saveAiChatMessage(role: String, content: String, surahRef: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/aiChats"
        return try {
            requireUserId()
            val cleanContent = content.trim().take(8000)
            require(cleanContent.isNotEmpty()) { "Message content cannot be empty" }
            val payload = mapOf(
                "role" to if (role == "assistant") "assistant" else "user",
                "content" to cleanContent,
                "surahRef" to surahRef?.trim()?.take(64)?.takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("users").document(uid).collection("aiChats").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun clearAiChats(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/aiChats"
        return try {
            requireUserId()
            val docs = db.collection("users").document(uid).collection("aiChats").get().await()
            for (d in docs.documents) {
                d.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- COMMUNITY ANONYMOUS DUA WALL ---
    fun observeDuaWall(uid: String): Flow<List<DuaWallPostDoc>> {
        val path = "duaWall"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("duaWall")
                .whereEqualTo("visibility", "COMMUNITY")
                .snapshots()
                .map { snap -> snap.toObjects(DuaWallPostDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun postToDuaWall(alias: String, category: String, requestText: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "duaWall"
        return try {
            requireUserId()
            val cleanText = requestText.trim().take(500)
            require(cleanText.length >= 5) { "Please write at least 5 characters for your Dua request." }
            val cleanAlias = alias.trim().ifBlank { "طالب دعاء" }.take(40)
            val payload = mapOf(
                "authorUid" to uid,
                "alias" to cleanAlias,
                "category" to category,
                "requestText" to cleanText,
                "visibility" to "COMMUNITY",
                "ameenCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("duaWall").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementAmeen(postId: String): Result<Unit> {
        val path = "duaWall/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Invalid post ID" }
            db.collection("duaWall").document(postId)
                .update(
                    mapOf(
                        "ameenCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDuaWallPost(postId: String): Result<Unit> {
        val path = "duaWall/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Invalid post ID" }
            db.collection("duaWall").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- FAMILY KHATMAH GROUPS ---
    fun observeKhatmahGroups(uid: String): Flow<List<KhatmahGroupDoc>> {
        val path = "khatmahGroups"
        return safeFirestoreFlow(path, OperationType.LIST, uid) {
            db.collection("khatmahGroups")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap -> snap.toObjects(KhatmahGroupDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }
    }

    suspend fun createKhatmahGroup(title: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "khatmahGroups"
        return try {
            requireUserId()
            val cleanTitle = title.trim().ifBlank { "ختمة العائلة" }.take(80)
            val code = ("NOOR" + (1000..9999).random()).take(10)
            val payload = mapOf(
                "ownerUid" to uid,
                "title" to cleanTitle,
                "joinCode" to code,
                "memberUids" to listOf(uid),
                "completedJuz" to emptyList<Long>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("khatmahGroups").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleGroupJuz(group: KhatmahGroupDoc, juz: Int): Result<Unit> {
        val path = "khatmahGroups/${group.id}"
        return try {
            requireUserId()
            require(group.id.isNotBlank()) { "Invalid group ID" }
            val updated = group.completedJuz.toMutableSet()
            val juzLong = juz.toLong()
            if (juzLong in updated) updated.remove(juzLong) else updated.add(juzLong)
            db.collection("khatmahGroups").document(group.id)
                .update(
                    mapOf(
                        "completedJuz" to updated.sorted(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- EXPORT & SINGLE-TAP DELETE ---
    suspend fun exportUserDataJson(): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            requireUserId()
            val userDoc = db.collection("users").document(uid).get().await()
            val logs = db.collection("users").document(uid).collection("prayerLogs").get().await()
            val journal = db.collection("users").document(uid).collection("duaJournal").get().await()
            val bookmarks = db.collection("users").document(uid).collection("bookmarks").get().await()
            val tasbih = db.collection("users").document(uid).collection("tasbihCounters").get().await()
            val obligations = db.collection("users").document(uid).collection("obligations").get().await()

            val root = JSONObject().apply {
                put("exportedAt", System.currentTimeMillis())
                put("settings", JSONObject(userDoc.data ?: emptyMap<String, Any>()))
                put("prayerLogs", JSONArray(logs.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
                put("duaJournal", JSONArray(journal.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
                put("bookmarks", JSONArray(bookmarks.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
                put("tasbihCounters", JSONArray(tasbih.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
                put("obligations", JSONArray(obligations.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) }))
            }
            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserData(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            requireUserId()
            val userRef = db.collection("users").document(uid)
            val subCollections = listOf("prayerLogs", "duaJournal", "bookmarks", "tasbihCounters", "obligations", "adhkarLogs", "aiChats")
            for (sub in subCollections) {
                val docs = userRef.collection(sub).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            userRef.delete().await()
            localPrefsDao?.deletePrefs(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    companion object {
        val DEFAULT_RECITERS: List<ReciterEntity> = listOf(
            ReciterEntity("ar.husary", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary", "مرتل مجوّد", "Murattal", "ar.husary", "Husary_128kbps", 1, true),
            ReciterEntity("ar.minshawi", "محمد صديق المنشاوي", "Muhammad Siddiq Al-Minshawi", "مرتل خاشع", "Murattal", "ar.minshawi", "Minshawy_Murattal_128kbps", 2, true),
            ReciterEntity("ar.abdulbasitmurattal", "عبد الباسط عبد الصمد", "Abdul Basit 'Abdus-Samad", "مرتل", "Murattal", "ar.abdulbasitmurattal", "Abdul_Basit_Murattal_192kbps", 3, true),
            ReciterEntity("ar.alafasy", "مشاري راشد العفاسي", "Mishary Rashid Alafasy", "مرتل", "Murattal", "ar.alafasy", "Alafasy_128kbps", 4, true),
            ReciterEntity("ar.mahermuaiqly", "ماهر المعيقلي", "Maher Al-Muaiqly", "إمام الحرم المكي", "Haramain", "ar.mahermuaiqly", "MaherAlMuaiqly128kbps", 5, true),
            ReciterEntity("ar.abdurrahmaansudais", "عبد الرحمن السديس", "Abdurrahman As-Sudais", "إمام الحرم المكي", "Haramain", "ar.abdurrahmaansudais", "Abdurrahmaan_As-Sudais_192kbps", 6, true),
            ReciterEntity("ar.yasseraldossari", "ياسر الدوسري", "Yasser Al-Dosari", "إمام الحرم المكي", "Haramain", "ar.alafasy", "Yasser_Ad-Dussary_128kbps", 7, true),
            ReciterEntity("ar.saoodshuraym", "سعود الشريم", "Saud Al-Shuraim", "إمام الحرم المكي", "Haramain", "ar.saoodshuraym", "Saood_ash-Shuraym_128kbps", 8, true)
        )
    }
}
