package com.example.data.firebase

import com.example.domain.AdhkarAndAdhanCatalog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireSuperAdmin(): String {
        val user = auth.currentUser
        if (!AdminAccess.isSuperAdmin(user)) {
            throw SecurityException("Super Admin access restricted exclusively to ${AdminAccess.SUPER_ADMIN_EMAIL}.")
        }
        return user!!.uid
    }

    private fun <T> safeAdminFlow(
        path: String,
        operationType: OperationType,
        flowProvider: () -> Flow<T>
    ): Flow<T> = flow {
        requireSuperAdmin()
        emitAll(
            flowProvider().catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && AdminAccess.isSuperAdmin(auth.currentUser)) {
                    handleFirestoreError(ex, operationType, path)
                }
                throw error
            }
        )
    }

    // --- ADMIN OBSERVERS (INCLUDE INACTIVE ITEMS & FULL USER DIRECTORY) ---
    fun observeAllRecitersAdmin(): Flow<List<ReciterDoc>> =
        safeAdminFlow("reciters", OperationType.LIST) {
            db.collection("reciters").snapshots().map { snap ->
                snap.toObjects(ReciterDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
        }

    fun observeAllAdhkarAdmin(): Flow<List<AdhkarItemDoc>> =
        safeAdminFlow("adhkarItems", OperationType.LIST) {
            db.collection("adhkarItems").snapshots().map { snap ->
                snap.toObjects(AdhkarItemDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
        }

    fun observeAllAdhanVoicesAdmin(): Flow<List<AdhanVoiceDoc>> =
        safeAdminFlow("adhanVoices", OperationType.LIST) {
            db.collection("adhanVoices").snapshots().map { snap ->
                snap.toObjects(AdhanVoiceDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        }

    fun observeAllUsersAdmin(): Flow<List<UserSettingsDoc>> =
        safeAdminFlow("users", OperationType.LIST) {
            db.collection("users").limit(200).snapshots().map { snap ->
                snap.toObjects(UserSettingsDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        }

    // --- ADMIN CONFIG SAVES ---
    suspend fun saveAdminConfig(configKey: String, jsonObject: JSONObject): Result<Unit> {
        val path = "adminConfigs/$configKey"
        return try {
            requireSuperAdmin()
            val cleanKey = configKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80)
            require(cleanKey.isNotEmpty()) { "Invalid config key" }
            val ref = db.collection("adminConfigs").document(cleanKey)
            val existing = ref.get().await()
            val valueJson = jsonObject.toString().take(15900)
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "configKey" to cleanKey,
                        "valueJson" to valueJson,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "configKey" to cleanKey,
                        "valueJson" to valueJson,
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

    suspend fun saveQuranDefaults(cfg: QuranDefaultsConfig): Result<Unit> {
        val json = JSONObject().apply {
            put("quranPageApiBase", cfg.quranPageApiBase.trim())
            put("quranFallbackApiBase", cfg.quranFallbackApiBase.trim())
            put("audioCdnBase", cfg.audioCdnBase.trim())
            put("defaultReciterId", cfg.defaultReciterId.trim().ifBlank { "ar.alafasy" })
            put("defaultPaper", cfg.defaultPaper)
            put("defaultThemeMode", cfg.defaultThemeMode)
            put("defaultFontScale", cfg.defaultFontScale.coerceIn(0.85, 1.35))
        }
        return saveAdminConfig("quran_defaults", json)
    }

    suspend fun saveAiConfig(cfg: AiRuntimeConfig): Result<Unit> {
        val safeModel = cfg.modelName.trim().let {
            if (it.startsWith("gemini-3") || it.startsWith("gemini-4")) it else "gemini-3.8-flash"
        }
        val json = JSONObject().apply {
            put("enabled", cfg.enabled)
            put("modelName", safeModel)
            put("systemPrompt", cfg.systemPrompt.trim().ifBlank { AiRuntimeConfig.DEFAULT_ISLAMIC_SYSTEM_PROMPT })
            put("dailyLimitPerUser", cfg.dailyLimitPerUser.coerceIn(5, 500))
        }
        return saveAdminConfig("ai_settings", json)
    }

    suspend fun saveAppOperations(cfg: AppOperationsConfig): Result<Unit> {
        val json = JSONObject().apply {
            put("bannerEnabled", cfg.bannerEnabled)
            put("bannerTitleAr", cfg.bannerTitleAr.trim().take(120))
            put("bannerTitleEn", cfg.bannerTitleEn.trim().take(120))
            put("bannerTitleUz", cfg.bannerTitleUz.trim().take(120))
            put("bannerBodyAr", cfg.bannerBodyAr.trim().take(500))
            put("bannerBodyEn", cfg.bannerBodyEn.trim().take(500))
            put("bannerBodyUz", cfg.bannerBodyUz.trim().take(500))
            put("bannerImageUrl", cfg.bannerImageUrl.trim().take(180000))
            put("bannerLinkUrl", cfg.bannerLinkUrl.trim().take(500))
            put("bannerCtaAr", cfg.bannerCtaAr.trim().take(60))
            put("bannerCtaEn", cfg.bannerCtaEn.trim().take(60))
            put("bannerCtaUz", cfg.bannerCtaUz.trim().take(60))
            put("bannerTone", cfg.bannerTone)
            put("aiAssistantEnabled", cfg.aiAssistantEnabled)
            put("quranAudioEnabled", cfg.quranAudioEnabled)
            put("communityDuaWallEnabled", cfg.communityDuaWallEnabled)
            put("familyKhatmahEnabled", cfg.familyKhatmahEnabled)
            put("arQiblaEnabled", cfg.arQiblaEnabled)
            put("kidsModeEnabled", cfg.kidsModeEnabled)
            put("customAdhanUploadEnabled", cfg.customAdhanUploadEnabled)
            put("smartShortcutsEnabled", cfg.smartShortcutsEnabled)
            put("liveVersesCardEnabled", cfg.liveVersesCardEnabled)
            put("nearbyMosquesEnabled", cfg.nearbyMosquesEnabled)
            put("persistentPrayerNotificationAllowed", cfg.persistentPrayerNotificationAllowed)
            put("maintenanceMode", cfg.maintenanceMode)
        }
        return saveAdminConfig("app_operations", json)
    }

    // --- RECITERS CRUD ---
    suspend fun upsertReciter(reciter: ReciterDoc): Result<Unit> {
        val safeId = (reciter.id.ifBlank { reciter.editionId })
            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .take(64)
            .ifBlank { "reciter_${System.currentTimeMillis()}" }
        val path = "reciters/$safeId"
        return try {
            requireSuperAdmin()
            val ref = db.collection("reciters").document(safeId)
            val snap = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "nameAr" to reciter.nameAr.trim().ifBlank { "قارئ" }.take(100),
                "nameEn" to reciter.nameEn.trim().ifBlank { "Reciter" }.take(100),
                "styleAr" to reciter.styleAr.trim().take(60),
                "styleEn" to reciter.styleEn.trim().take(60),
                "editionId" to reciter.editionId.trim().ifBlank { safeId }.take(80),
                "everyAyahFolder" to reciter.everyAyahFolder.trim().ifBlank { "Alafasy_128kbps" }.take(120),
                "cloudAudioBaseUrl" to reciter.cloudAudioBaseUrl.trim().take(500),
                "sortOrder" to reciter.sortOrder.coerceIn(0L, 1000L),
                "active" to reciter.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                payload["createdAt"] = snap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReciter(reciterId: String): Result<Unit> {
        val path = "reciters/$reciterId"
        return try {
            requireSuperAdmin()
            require(reciterId.isNotBlank()) { "Invalid reciter ID" }
            db.collection("reciters").document(reciterId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultReciters(): Result<Int> {
        return try {
            requireSuperAdmin()
            var count = 0
            for (r in MihrabRepository.DEFAULT_RECITERS) {
                upsertReciter(
                    ReciterDoc(
                        id = r.id,
                        nameAr = r.nameAr,
                        nameEn = r.nameEn,
                        styleAr = r.styleAr,
                        styleEn = r.styleEn,
                        editionId = r.editionId,
                        everyAyahFolder = r.everyAyahFolder,
                        cloudAudioBaseUrl = r.cloudAudioBaseUrl,
                        sortOrder = r.sortOrder.toLong(),
                        active = r.active
                    )
                ).getOrThrow()
                count++
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ADHKAR CRUD ---
    suspend fun upsertAdhkar(item: AdhkarItemDoc): Result<Unit> {
        val safeId = item.id
            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .take(80)
            .ifBlank { "dhikr_${System.currentTimeMillis()}" }
        val path = "adhkarItems/$safeId"
        return try {
            requireSuperAdmin()
            val ref = db.collection("adhkarItems").document(safeId)
            val snap = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "category" to item.category,
                "titleAr" to item.titleAr.trim().ifBlank { "ذكر" }.take(160),
                "titleEn" to item.titleEn.trim().ifBlank { "Dhikr" }.take(160),
                "arabic" to item.arabic.trim().ifBlank { "سبحان الله" }.take(4000),
                "transliteration" to item.transliteration.trim().take(4000),
                "translation" to item.translation.trim().take(4000),
                "targetCount" to item.targetCount.coerceIn(1L, 1000L),
                "referenceAr" to item.referenceAr.trim().take(400),
                "referenceEn" to item.referenceEn.trim().take(400),
                "iconName" to item.iconName.trim().take(60),
                "sortOrder" to item.sortOrder.coerceIn(0L, 10000L),
                "active" to item.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            payload["createdAt"] = snap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdhkar(itemId: String): Result<Unit> {
        val path = "adhkarItems/$itemId"
        return try {
            requireSuperAdmin()
            require(itemId.isNotBlank()) { "Invalid Adhkar ID" }
            db.collection("adhkarItems").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultAdhkar(): Result<Int> {
        return try {
            requireSuperAdmin()
            var count = 0
            for (d in AdhkarAndAdhanCatalog.defaultAdhkarEntities) {
                upsertAdhkar(
                    AdhkarItemDoc(
                        id = d.id,
                        category = d.category,
                        titleAr = d.titleAr,
                        titleEn = d.titleEn,
                        arabic = d.arabic,
                        transliteration = d.transliteration,
                        translation = d.translation,
                        targetCount = d.targetCount.toLong(),
                        referenceAr = d.referenceAr,
                        referenceEn = d.referenceEn,
                        iconName = d.iconName,
                        sortOrder = d.sortOrder.toLong(),
                        active = d.active
                    )
                ).getOrThrow()
                count++
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ADHAN VOICES CRUD ---
    suspend fun upsertAdhanVoice(voice: AdhanVoiceDoc): Result<Unit> {
        val safeId = voice.id
            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .take(64)
            .ifBlank { "adhan_${System.currentTimeMillis()}" }
        val path = "adhanVoices/$safeId"
        return try {
            requireSuperAdmin()
            val ref = db.collection("adhanVoices").document(safeId)
            val snap = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "nameAr" to voice.nameAr.trim().ifBlank { "أذان" }.take(100),
                "nameEn" to voice.nameEn.trim().ifBlank { "Adhan" }.take(100),
                "muezzin" to voice.muezzin.trim().ifBlank { "Muezzin" }.take(100),
                "audioUrl" to voice.audioUrl.trim().take(500),
                "active" to voice.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            payload["createdAt"] = snap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdhanVoice(voiceId: String): Result<Unit> {
        val path = "adhanVoices/$voiceId"
        return try {
            requireSuperAdmin()
            require(voiceId.isNotBlank()) { "Invalid Adhan Voice ID" }
            db.collection("adhanVoices").document(voiceId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultAdhanVoices(): Result<Int> {
        return try {
            requireSuperAdmin()
            var count = 0
            for (v in AdhkarAndAdhanCatalog.defaultAdhanVoices) {
                upsertAdhanVoice(
                    AdhanVoiceDoc(
                        id = v.id,
                        nameAr = v.nameAr,
                        nameEn = v.nameEn,
                        muezzin = v.muezzin,
                        audioUrl = v.audioUrl,
                        active = v.active
                    )
                ).getOrThrow()
                count++
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- USER MANAGEMENT ---
    suspend fun updateUserAsAdmin(targetUid: String, fields: Map<String, Any>): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireSuperAdmin()
            require(targetUid.isNotBlank()) { "Invalid target UID" }
            val clean = fields.toMutableMap()
            clean.remove("userId")
            clean.remove("createdAt")
            clean["updatedAt"] = FieldValue.serverTimestamp()
            db.collection("users").document(targetUid).update(clean).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FULL DATABASE JSON BACKUP EXPORT & IMPORT ---
    suspend fun exportFullDatabaseBackupJson(): Result<String> {
        return try {
            requireSuperAdmin()
            val configsSnap = db.collection("adminConfigs").get().await()
            val recitersSnap = db.collection("reciters").get().await()
            val adhkarSnap = db.collection("adhkarItems").get().await()
            val adhanSnap = db.collection("adhanVoices").get().await()
            val usersSnap = db.collection("users").limit(200).get().await()

            fun docsToArray(docs: List<DocumentSnapshot>): JSONArray {
                val arr = JSONArray()
                for (doc in docs) {
                    val obj = JSONObject(doc.data ?: emptyMap<String, Any>())
                    obj.put("_id", doc.id)
                    obj.remove("createdAt")
                    obj.remove("updatedAt")
                    arr.put(obj)
                }
                return arr
            }

            val root = JSONObject().apply {
                put("backupVersion", 3)
                put("exportedAt", System.currentTimeMillis())
                put("exportedBy", AdminAccess.SUPER_ADMIN_EMAIL)
                put("adminConfigs", docsToArray(configsSnap.documents))
                put("reciters", docsToArray(recitersSnap.documents))
                put("adhkarItems", docsToArray(adhkarSnap.documents))
                put("adhanVoices", docsToArray(adhanSnap.documents))
                put("usersSummary", docsToArray(usersSnap.documents))
            }
            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "adminConfigs")
            Result.failure(e)
        }
    }

    suspend fun importFullDatabaseBackupJson(rawJson: String): Result<Int> {
        return try {
            requireSuperAdmin()
            val root = JSONObject(rawJson)
            var importedCount = 0

            root.optJSONArray("adminConfigs")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val key = obj.optString("_id").ifBlank { obj.optString("configKey") }
                    val valJson = obj.optString("valueJson", "{}")
                    if (key.isNotBlank()) {
                        saveAdminConfig(key, JSONObject(valJson)).getOrThrow()
                        importedCount++
                    }
                }
            }

            root.optJSONArray("reciters")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("_id").ifBlank { obj.optString("editionId") }
                    upsertReciter(
                        ReciterDoc(
                            id = id,
                            nameAr = obj.optString("nameAr", "قارئ"),
                            nameEn = obj.optString("nameEn", "Reciter"),
                            styleAr = obj.optString("styleAr", "مرتل"),
                            styleEn = obj.optString("styleEn", "Murattal"),
                            editionId = obj.optString("editionId", id),
                            everyAyahFolder = obj.optString("everyAyahFolder", "Alafasy_128kbps"),
                            cloudAudioBaseUrl = obj.optString("cloudAudioBaseUrl", ""),
                            sortOrder = obj.optLong("sortOrder", (i + 1).toLong()),
                            active = obj.optBoolean("active", true)
                        )
                    ).getOrThrow()
                    importedCount++
                }
            }

            root.optJSONArray("adhkarItems")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("_id").ifBlank { "dhikr_imp_$i" }
                    upsertAdhkar(
                        AdhkarItemDoc(
                            id = id,
                            category = obj.optString("category", "MORNING"),
                            titleAr = obj.optString("titleAr", "ذكر"),
                            titleEn = obj.optString("titleEn", "Dhikr"),
                            arabic = obj.optString("arabic", ""),
                            transliteration = obj.optString("transliteration", ""),
                            translation = obj.optString("translation", ""),
                            targetCount = obj.optLong("targetCount", 1L),
                            referenceAr = obj.optString("referenceAr", ""),
                            referenceEn = obj.optString("referenceEn", ""),
                            iconName = obj.optString("iconName", ""),
                            sortOrder = obj.optLong("sortOrder", (i + 1).toLong()),
                            active = obj.optBoolean("active", true)
                        )
                    ).getOrThrow()
                    importedCount++
                }
            }

            root.optJSONArray("adhanVoices")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("_id").ifBlank { "adhan_imp_$i" }
                    upsertAdhanVoice(
                        AdhanVoiceDoc(
                            id = id,
                            nameAr = obj.optString("nameAr", "أذان"),
                            nameEn = obj.optString("nameEn", "Adhan"),
                            muezzin = obj.optString("muezzin", "Muezzin"),
                            audioUrl = obj.optString("audioUrl", ""),
                            active = obj.optBoolean("active", true)
                        )
                    ).getOrThrow()
                    importedCount++
                }
            }

            Result.success(importedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
