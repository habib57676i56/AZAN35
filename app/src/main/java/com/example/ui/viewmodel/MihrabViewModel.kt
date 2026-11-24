package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.BookmarkDoc
import com.example.data.firebase.DuaWallPostDoc
import com.example.data.firebase.JournalEntryDoc
import com.example.data.firebase.KhatmahGroupDoc
import com.example.data.firebase.MihrabRepository
import com.example.data.firebase.ObligationDoc
import com.example.data.firebase.PrayerLogDoc
import com.example.data.firebase.TasbihCounterDoc
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.CalculationMethod
import com.example.domain.CityPreset
import com.example.domain.DailyPrayerSchedule
import com.example.domain.HijriCalendarEngine
import com.example.domain.HijriDate
import com.example.domain.PrayerTimesEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MihrabViewModel(
    private val repository: MihrabRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _manualOffsets = MutableStateFlow<Map<String, Int>>(emptyMap())
    val manualOffsets: StateFlow<Map<String, Int>> = _manualOffsets.asStateFlow()

    private val _useIndicNumerals = MutableStateFlow(false)
    val useIndicNumerals: StateFlow<Boolean> = _useIndicNumerals.asStateFlow()

    private val _exportedJson = MutableStateFlow<String?>(null)
    val exportedJson: StateFlow<String?> = _exportedJson.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserSettings(initialDisplayName).onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to sync user profile"
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val settingsState: StateFlow<UiState<UserSettingsDoc?>> =
        repository.observeUserSettings(currentUserId).toUiStateFlow("user settings")

    val prayerLogsState: StateFlow<UiState<List<PrayerLogDoc>>> =
        repository.observePrayerLogs(currentUserId).toUiStateFlow("prayer logs")

    val journalState: StateFlow<UiState<List<JournalEntryDoc>>> =
        repository.observeJournalEntries(currentUserId).toUiStateFlow("dua journal")

    val bookmarksState: StateFlow<UiState<List<BookmarkDoc>>> =
        repository.observeBookmarks(currentUserId).toUiStateFlow("bookmarks")

    val tasbihState: StateFlow<UiState<List<TasbihCounterDoc>>> =
        repository.observeTasbihCounters(currentUserId).toUiStateFlow("tasbih counters")

    val obligationsState: StateFlow<UiState<List<ObligationDoc>>> =
        repository.observeObligations(currentUserId).toUiStateFlow("obligations")

    val duaWallState: StateFlow<UiState<List<DuaWallPostDoc>>> =
        repository.observeDuaWall(currentUserId).toUiStateFlow("community dua wall")

    val khatmahGroupsState: StateFlow<UiState<List<KhatmahGroupDoc>>> =
        repository.observeKhatmahGroups(currentUserId).toUiStateFlow("family khatmah")

    fun todayDateKey(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Calendar.getInstance().time)
    }

    fun computeSchedule(settings: UserSettingsDoc?, offsets: Map<String, Int>): DailyPrayerSchedule {
        val s = settings ?: UserSettingsDoc()
        return PrayerTimesEngine.calculateForDate(
            calendar = Calendar.getInstance(),
            latitude = s.latitude,
            longitude = s.longitude,
            method = CalculationMethod.fromId(s.calculationMethod),
            travelerMode = s.travelerMode,
            manualOffsetsMin = offsets
        )
    }

    fun computeHijriToday(settings: UserSettingsDoc?): HijriDate {
        val offset = (settings?.hijriOffset ?: 0L).toInt()
        return HijriCalendarEngine.gregorianToHijri(Calendar.getInstance(), offset)
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun clearExportedJson() {
        _exportedJson.value = null
    }

    fun toggleIndicNumerals(enabled: Boolean) {
        _useIndicNumerals.value = enabled
    }

    fun adjustPrayerOffset(prayerKey: String, deltaMin: Int) {
        val current = _manualOffsets.value.toMutableMap()
        val updated = ((current[prayerKey] ?: 0) + deltaMin).coerceIn(-30, 30)
        current[prayerKey] = updated
        _manualOffsets.value = current
    }

    fun selectCityPreset(preset: CityPreset) {
        updateSettings(
            mapOf(
                "city" to preset.name,
                "latitude" to preset.latitude,
                "longitude" to preset.longitude,
                "calculationMethod" to preset.defaultMethod.id
            )
        )
    }

    fun updateLocationCoordinates(cityLabel: String, lat: Double, lng: Double) {
        updateSettings(
            mapOf(
                "city" to cityLabel.trim().ifBlank { "Current GPS" }.take(80),
                "latitude" to lat.coerceIn(-90.0, 90.0),
                "longitude" to lng.coerceIn(-180.0, 180.0)
            )
        )
    }

    fun updateSettings(fields: Map<String, Any>) {
        viewModelScope.launch {
            repository.updateUserSettings(fields).onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update settings"
            }
        }
    }

    fun togglePrayerToday(prayerKey: String, currentLog: PrayerLogDoc?) {
        val dateKey = todayDateKey()
        val base = currentLog ?: PrayerLogDoc(dateKey = dateKey)
        val updated = when (prayerKey) {
            "fajr" -> base.copy(fajr = !base.fajr)
            "dhuhr" -> base.copy(dhuhr = !base.dhuhr)
            "asr" -> base.copy(asr = !base.asr)
            "maghrib" -> base.copy(maghrib = !base.maghrib)
            "isha" -> base.copy(isha = !base.isha)
            "sunnahRawatib" -> base.copy(sunnahRawatib = !base.sunnahRawatib)
            "duha" -> base.copy(duha = !base.duha)
            "witr" -> base.copy(witr = !base.witr)
            "qiyam" -> base.copy(qiyam = !base.qiyam)
            else -> base
        }
        viewModelScope.launch {
            repository.upsertPrayerLog(updated).onFailure {
                _statusMessage.value = it.localizedMessage ?: "Could not save prayer log"
            }
        }
    }

    fun setFastingToday(fastingType: String, currentLog: PrayerLogDoc?) {
        val dateKey = todayDateKey()
        val base = currentLog ?: PrayerLogDoc(dateKey = dateKey)
        viewModelScope.launch {
            repository.upsertPrayerLog(base.copy(fastingType = fastingType)).onFailure {
                _statusMessage.value = it.localizedMessage ?: "Could not save fasting status"
            }
        }
    }

    fun setKhushuRating(rating: Int, currentLog: PrayerLogDoc?) {
        val dateKey = todayDateKey()
        val base = currentLog ?: PrayerLogDoc(dateKey = dateKey)
        viewModelScope.launch {
            repository.upsertPrayerLog(base.copy(khushuRating = rating.toLong())).onFailure {
                _statusMessage.value = it.localizedMessage ?: "Could not save khushu reflection"
            }
        }
    }

    fun adjustQada(field: String, currentVal: Long, delta: Long) {
        val next = (currentVal + delta).coerceIn(0L, 100000L)
        updateSettings(mapOf(field to next))
    }

    fun addJournalEntry(entryType: String, title: String, body: String, reference: String?) {
        viewModelScope.launch {
            repository.addJournalEntry(entryType, title, body, reference)
                .onSuccess { _statusMessage.value = "Saved to your private journal." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not save entry" }
        }
    }

    fun toggleJournalAnswered(entry: JournalEntryDoc) {
        viewModelScope.launch {
            repository.toggleJournalAnswered(entry.id, !entry.answered)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update entry" }
        }
    }

    fun deleteJournalEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteJournalEntry(entryId)
        }
    }

    fun addBookmark(surahNumber: Int, ayahNumber: Int, collectionName: String, note: String?) {
        viewModelScope.launch {
            repository.addBookmark(surahNumber, ayahNumber, collectionName, note)
                .onSuccess { _statusMessage.value = "Bookmarked $surahNumber:$ayahNumber in $collectionName" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Bookmark failed" }
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    fun saveTasbihCounter(id: String, label: String, arabicText: String, count: Long, target: Long) {
        viewModelScope.launch {
            repository.upsertTasbihCounter(id, label, arabicText, count, target)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save Tasbih counter" }
        }
    }

    fun addObligation(kind: String, title: String, amount: Double, notes: String?) {
        viewModelScope.launch {
            repository.addObligation(kind, title, amount, notes)
                .onSuccess { _statusMessage.value = "Saved record." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save record" }
        }
    }

    fun toggleObligation(doc: ObligationDoc) {
        viewModelScope.launch {
            repository.toggleObligationSettled(doc)
        }
    }

    fun deleteObligation(id: String) {
        viewModelScope.launch {
            repository.deleteObligation(id)
        }
    }

    fun postDuaWall(alias: String, category: String, requestText: String) {
        viewModelScope.launch {
            repository.postToDuaWall(alias, category, requestText)
                .onSuccess { _statusMessage.value = "Shared anonymously on the Dua Wall." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to post Dua" }
        }
    }

    fun sayAmeen(postId: String) {
        viewModelScope.launch {
            repository.incrementAmeen(postId)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not record Ameen" }
        }
    }

    fun deleteMyDuaPost(postId: String) {
        viewModelScope.launch {
            repository.deleteDuaWallPost(postId)
        }
    }

    fun createKhatmahGroup(title: String) {
        viewModelScope.launch {
            repository.createKhatmahGroup(title)
                .onSuccess { _statusMessage.value = "Family Khatmah created!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to create group" }
        }
    }

    fun toggleGroupJuz(group: KhatmahGroupDoc, juz: Int) {
        viewModelScope.launch {
            repository.toggleGroupJuz(group, juz)
        }
    }

    fun exportAllData() {
        viewModelScope.launch {
            repository.exportUserDataJson()
                .onSuccess { _exportedJson.value = it }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Export failed" }
        }
    }

    fun deleteAccountAndData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserData()
                .onSuccess {
                    _statusMessage.value = "All cloud worship data deleted."
                    onComplete()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    private companion object {
        const val TAG = "MihrabViewModel"
    }
}
