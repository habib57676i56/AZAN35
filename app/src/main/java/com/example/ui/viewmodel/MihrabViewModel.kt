package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AdhanAlertMode
import com.example.data.firebase.AdhkarLogDoc
import com.example.data.firebase.AiRuntimeConfig
import com.example.data.firebase.BookmarkDoc
import com.example.data.firebase.DuaWallPostDoc
import com.example.data.firebase.JournalEntryDoc
import com.example.data.firebase.KhatmahGroupDoc
import com.example.data.firebase.MihrabRepository
import com.example.data.firebase.ObligationDoc
import com.example.data.firebase.PrayerLogDoc
import com.example.data.firebase.TasbihCounterDoc
import com.example.data.firebase.UserSettingsDoc
import com.example.data.local.AdhanVoiceEntity
import com.example.data.local.AdhkarEntity
import com.example.data.local.LocalUserPrefsEntity
import com.example.data.local.ReciterEntity
import com.example.data.quran.MushafRepository
import com.example.data.quran.QuranSearchHit
import com.example.domain.AdhkarAndAdhanCatalog
import com.example.domain.CalculationMethod
import com.example.domain.CityPreset
import com.example.domain.DailyPrayerSchedule
import com.example.domain.HijriCalendarEngine
import com.example.domain.HijriDate
import com.example.domain.MushafPageModel
import com.example.domain.PrayerTimesEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val mushafRepository: MushafRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _manualOffsets = MutableStateFlow<Map<String, Int>>(emptyMap())
    val manualOffsets: StateFlow<Map<String, Int>> = _manualOffsets.asStateFlow()

    private val _useIndicNumerals = MutableStateFlow(true)
    val useIndicNumerals: StateFlow<Boolean> = _useIndicNumerals.asStateFlow()

    private val _exportedJson = MutableStateFlow<String?>(null)
    val exportedJson: StateFlow<String?> = _exportedJson.asStateFlow()

    // Phase 1 Mushaf State
    private val _mushafPages = MutableStateFlow<Map<Int, UiState<MushafPageModel>>>(emptyMap())
    val mushafPages: StateFlow<Map<Int, UiState<MushafPageModel>>> = _mushafPages.asStateFlow()

    private val _offlineSyncProgress = MutableStateFlow<Float?>(null)
    val offlineSyncProgress: StateFlow<Float?> = _offlineSyncProgress.asStateFlow()

    private val _searchResults = MutableStateFlow<List<QuranSearchHit>>(emptyList())
    val searchResults: StateFlow<List<QuranSearchHit>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var savePositionJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultRecitersSeededLocally()
            repository.ensureAdhanAndAdhkarSeededLocally()
            repository.ensureUserSettings(initialDisplayName).onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر مزامنة الملف الشخصي"
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

    val localPrefsState: StateFlow<LocalUserPrefsEntity?> =
        repository.observeLocalPrefs(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val recitersState: StateFlow<List<ReciterEntity>> =
        repository.observeReciters(currentUserId)
            .catch { emit(MihrabRepository.DEFAULT_RECITERS) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = MihrabRepository.DEFAULT_RECITERS
            )

    val cachedMushafPageCount: StateFlow<Int> =
        mushafRepository.cachedPageCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

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

    // --- PHASE 2 STATE FLOWS: ADHAN VOICES, ADHKAR ENCYCLOPEDIA, DAILY ADHKAR STATS & AI CONFIG ---
    val adhanVoicesState: StateFlow<List<AdhanVoiceEntity>> =
        repository.observeAdhanVoices(currentUserId)
            .catch { emit(AdhkarAndAdhanCatalog.defaultAdhanVoices) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = AdhkarAndAdhanCatalog.defaultAdhanVoices
            )

    val adhkarCatalogState: StateFlow<List<AdhkarEntity>> =
        repository.observeAdhkarItems(currentUserId)
            .catch { emit(AdhkarAndAdhanCatalog.defaultAdhkarEntities) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = AdhkarAndAdhanCatalog.defaultAdhkarEntities
            )

    val todayAdhkarLogsState: StateFlow<List<AdhkarLogDoc>> =
        repository.observeAdhkarLogs(currentUserId, todayDateKey())
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val aiConfigState: StateFlow<AiRuntimeConfig> =
        repository.observeAiConfig(currentUserId)
            .catch { emit(AiRuntimeConfig()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = AiRuntimeConfig()
            )

    // --- PHASE 1 MUSHAF ACTIONS ---
    fun ensureMushafPageLoaded(page: Int) {
        val safePage = page.coerceIn(1, 604)
        val existing = _mushafPages.value[safePage]
        if (existing is UiState.Success || existing is UiState.Loading) return

        _mushafPages.value = _mushafPages.value + (safePage to UiState.Loading)
        viewModelScope.launch {
            mushafRepository.getPage(safePage)
                .onSuccess { model ->
                    _mushafPages.value = _mushafPages.value + (safePage to UiState.Success(model))
                    mushafRepository.prefetchPages(safePage)
                }
                .onFailure { err ->
                    _mushafPages.value = _mushafPages.value + (
                        safePage to UiState.Error(err.localizedMessage ?: "تعذر تحميل الصفحة")
                    )
                }
        }
    }

    fun retryMushafPage(page: Int) {
        val safePage = page.coerceIn(1, 604)
        _mushafPages.value = _mushafPages.value - safePage
        ensureMushafPageLoaded(safePage)
    }

    fun saveLastReadPosition(page: Int, surah: Int, ayah: Int) {
        val safePage = page.coerceIn(1, 604)
        val safeSurah = surah.coerceIn(1, 114)
        val safeAyah = ayah.coerceIn(1, 286)
        savePositionJob?.cancel()
        savePositionJob = viewModelScope.launch {
            delay(500L)
            repository.updateUserSettings(
                mapOf(
                    "lastReadPage" to safePage.toLong(),
                    "lastReadSurah" to safeSurah.toLong(),
                    "lastReadAyah" to safeAyah.toLong()
                )
            )
        }
    }

    fun syncFullQuranOffline() {
        if (_offlineSyncProgress.value != null) return
        viewModelScope.launch {
            _offlineSyncProgress.value = 0.05f
            mushafRepository.downloadFullQuranOffline { progress ->
                _offlineSyncProgress.value = progress
            }.onSuccess { count ->
                _offlineSyncProgress.value = null
                _statusMessage.value = "تم حفظ $count صفحة من مصحف المدينة للقراءة دون إنترنت"
            }.onFailure { err ->
                _offlineSyncProgress.value = null
                _statusMessage.value = err.localizedMessage ?: "تعذر تنزيل المصحف"
            }
        }
    }

    fun searchQuran(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(250L)
            val hits = mushafRepository.searchQuran(query)
            _searchResults.value = hits
            _isSearching.value = false
        }
    }

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

    fun showMessage(msg: String) {
        _statusMessage.value = msg
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
                "city" to cityLabel.trim().ifBlank { "GPS" }.take(80),
                "latitude" to lat.coerceIn(-90.0, 90.0),
                "longitude" to lng.coerceIn(-180.0, 180.0)
            )
        )
    }

    fun updateSettings(fields: Map<String, Any>) {
        viewModelScope.launch {
            repository.updateUserSettings(fields).onFailure {
                _statusMessage.value = it.localizedMessage ?: "تعذر حفظ الإعدادات"
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
                _statusMessage.value = it.localizedMessage ?: "تعذر حفظ سجل الصلاة"
            }
        }
    }

    fun setFastingToday(fastingType: String, currentLog: PrayerLogDoc?) {
        val dateKey = todayDateKey()
        val base = currentLog ?: PrayerLogDoc(dateKey = dateKey)
        viewModelScope.launch {
            repository.upsertPrayerLog(base.copy(fastingType = fastingType)).onFailure {
                _statusMessage.value = it.localizedMessage ?: "تعذر حفظ حالة الصيام"
            }
        }
    }

    fun setKhushuRating(rating: Int, currentLog: PrayerLogDoc?) {
        val dateKey = todayDateKey()
        val base = currentLog ?: PrayerLogDoc(dateKey = dateKey)
        viewModelScope.launch {
            repository.upsertPrayerLog(base.copy(khushuRating = rating.toLong())).onFailure {
                _statusMessage.value = it.localizedMessage ?: "تعذر حفظ الخشوع"
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
                .onSuccess { _statusMessage.value = "تم الحفظ في مفكرتك الخاصة" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر الحفظ" }
        }
    }

    fun toggleJournalAnswered(entry: JournalEntryDoc) {
        viewModelScope.launch {
            repository.toggleJournalAnswered(entry.id, !entry.answered)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر التحديث" }
        }
    }

    fun deleteJournalEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteJournalEntry(entryId)
        }
    }

    fun addBookmark(
        surahNumber: Int,
        ayahNumber: Int,
        collectionName: String,
        note: String?,
        pageNumber: Int? = null
    ) {
        viewModelScope.launch {
            repository.addBookmark(surahNumber, ayahNumber, collectionName, note, pageNumber)
                .onSuccess { _statusMessage.value = "تم حفظ العلامة $surahNumber:$ayahNumber" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر حفظ العلامة" }
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
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر حفظ التسبيح" }
        }
    }

    fun logAdhkarProgress(category: String, completedCount: Int, targetCount: Int) {
        viewModelScope.launch {
            repository.saveAdhkarLog(
                dateKey = todayDateKey(),
                category = category,
                completedCount = completedCount.toLong(),
                targetCount = targetCount.coerceAtLeast(1).toLong()
            )
        }
    }

    fun savePerPrayerAdhanMode(currentMap: Map<String, String>, prayerKey: String, mode: AdhanAlertMode) {
        val updated = currentMap.toMutableMap()
        updated[prayerKey] = mode.id
        updateSettings(mapOf("perPrayerAdhanModes" to updated))
    }

    fun saveCustomAdhanFile(localPath: String, displayName: String) {
        viewModelScope.launch {
            repository.saveCustomAdhanLocalFile(localPath, displayName)
                .onSuccess { _statusMessage.value = "تم حفظ الأذان المخصص: $displayName" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر حفظ ملف الأذان" }
        }
    }

    fun addObligation(kind: String, title: String, amount: Double, notes: String?) {
        viewModelScope.launch {
            repository.addObligation(kind, title, amount, notes)
                .onSuccess { _statusMessage.value = "تم حفظ السجل" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر الحفظ" }
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
                .onSuccess { _statusMessage.value = "تمت مشاركة دعائك في حائط الدعاء" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر نشر الدعاء" }
        }
    }

    fun sayAmeen(postId: String) {
        viewModelScope.launch {
            repository.incrementAmeen(postId)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر تسجيل آمين" }
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
                .onSuccess { _statusMessage.value = "تم إنشاء ختمة العائلة" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر إنشاء المجموعة" }
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
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر التصدير" }
        }
    }

    fun deleteAccountAndData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserData()
                .onSuccess {
                    _statusMessage.value = "تم حذف جميع بياناتك السحابية"
                    onComplete()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر الحذف" }
        }
    }

    private companion object {
        const val TAG = "MihrabViewModel"
    }
}
