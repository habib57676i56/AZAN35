package com.example.data.firebase

import android.content.Context
import com.example.R
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
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class MihrabRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
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

    // --- USER SETTINGS ---
    fun observeUserSettings(uid: String): Flow<UserSettingsDoc?> {
        val path = "users/$uid"
        return safeFirestoreFlow(path, OperationType.GET, uid) {
            db.collection("users").document(uid).snapshots().map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserSettingsDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
        }
    }

    suspend fun ensureUserSettings(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "Believer" }.take(60)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "preferredLanguage" to "en",
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

    suspend fun addBookmark(surahNumber: Int, ayahNumber: Int, collectionName: String, note: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val docId = "bm_${surahNumber}_$ayahNumber"
        val path = "users/$uid/bookmarks/$docId"
        return try {
            requireUserId()
            val cleanCol = collectionName.trim().ifBlank { "Favorites" }.take(60)
            val cleanNote = note?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
            val docRef = db.collection("users").document(uid).collection("bookmarks").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "collectionName" to cleanCol,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanNote != null) updates["note"] = cleanNote
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "surahNumber" to surahNumber.toLong(),
                    "ayahNumber" to ayahNumber.toLong(),
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
            db.collection("users").document(uid).collection("obligations").document(id).delete().await()
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
            val cleanAlias = alias.trim().ifBlank { "Anonymous Believer" }.take(40)
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
            val cleanTitle = title.trim().ifBlank { "Family Khatmah" }.take(80)
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
            val subCollections = listOf("prayerLogs", "duaJournal", "bookmarks", "tasbihCounters", "obligations")
            for (sub in subCollections) {
                val docs = userRef.collection(sub).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            userRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
