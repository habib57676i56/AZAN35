package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AiChatDoc
import com.example.data.firebase.AiCompanionRepository
import com.example.data.firebase.AiRuntimeConfig
import com.example.data.firebase.HifzQuizItem
import com.example.data.firebase.KhutbahSummaryResult
import com.example.data.firebase.MihrabRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiViewModel(
    private val mihrabRepository: MihrabRepository? = null,
    private val currentUserId: String? = null,
    private val aiRepository: AiCompanionRepository = AiCompanionRepository()
) : ViewModel() {

    private val _companionState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val companionState: StateFlow<AiUiState> = _companionState.asStateFlow()

    private val _ayahTafsirState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val ayahTafsirState: StateFlow<AiUiState> = _ayahTafsirState.asStateFlow()

    private val _recitationCoachState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val recitationCoachState: StateFlow<AiUiState> = _recitationCoachState.asStateFlow()

    private val _khutbahState = MutableStateFlow<UiState<KhutbahSummaryResult?>>(UiState.Success(null))
    val khutbahState: StateFlow<UiState<KhutbahSummaryResult?>> = _khutbahState.asStateFlow()

    private val _hifzQuizState = MutableStateFlow<UiState<HifzQuizItem?>>(UiState.Success(null))
    val hifzQuizState: StateFlow<UiState<HifzQuizItem?>> = _hifzQuizState.asStateFlow()

    val aiRuntimeConfig: StateFlow<AiRuntimeConfig> =
        if (mihrabRepository != null && !currentUserId.isNullOrBlank()) {
            mihrabRepository.observeAiConfig(currentUserId)
                .catch { emit(AiRuntimeConfig()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AiRuntimeConfig())
        } else {
            MutableStateFlow(AiRuntimeConfig()).asStateFlow()
        }

    val chatHistoryState: StateFlow<List<AiChatDoc>> =
        if (mihrabRepository != null && !currentUserId.isNullOrBlank()) {
            mihrabRepository.observeAiChats(currentUserId)
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        } else {
            flowOf(emptyList<AiChatDoc>()).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }

    private var sessionRequestsToday = 0

    private fun countTodayUserRequests(): Int {
        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val persistedToday = chatHistoryState.value.count { doc ->
            if (doc.role != "user") return@count false
            val date = doc.createdAt?.toDate() ?: return@count false
            val c = Calendar.getInstance().apply { time = date }
            c.get(Calendar.YEAR) == nowYear && c.get(Calendar.DAY_OF_YEAR) == nowDay
        }
        return maxOf(persistedToday, sessionRequestsToday)
    }

    fun askCompanion(question: String, languageName: String, surahRef: String? = null) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val cfg = aiRuntimeConfig.value
        if (!cfg.enabled) {
            _companionState.value = AiUiState.Error("تم تعطيل المساعد الذكي مؤقتاً من إعدادات الإدارة.")
            return
        }
        if (countTodayUserRequests() >= cfg.dailyLimitPerUser) {
            _companionState.value = AiUiState.Error("بلغت الحد اليومي (${cfg.dailyLimitPerUser}) لاستخدام المساعد الذكي.")
            return
        }
        sessionRequestsToday++

        val priorHistory = chatHistoryState.value
        viewModelScope.launch {
            _companionState.value = AiUiState.Loading
            mihrabRepository?.saveAiChatMessage(role = "user", content = trimmed, surahRef = surahRef)

            var latest = ""
            try {
                aiRepository.askAssistantChatStream(
                    history = priorHistory,
                    question = trimmed,
                    config = cfg,
                    languageName = languageName
                ).collect { chunk ->
                    latest = chunk
                    _companionState.value = AiUiState.Streaming(chunk)
                }
                if (latest.isNotBlank()) {
                    _companionState.value = AiUiState.Success(latest)
                    mihrabRepository?.saveAiChatMessage(
                        role = "assistant",
                        content = latest,
                        surahRef = surahRef
                    )
                } else {
                    _companionState.value = AiUiState.Error("لم يتم استلام رد. حاول مرة أخرى.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Companion stream timed out", e)
                _companionState.value = AiUiState.Error("انتهت مهلة الطلب. يرجى المحاولة مجدداً.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Companion stream failed", e)
                _companionState.value = AiUiState.Error(e.localizedMessage ?: "تعذر الاتصال بالمساعد الذكي.")
            }
        }
    }

    /**
     * Connected directly to the "Ask AI Tafsir" button inside the Madinah Mushaf Ayah Action Menu.
     * Respects dynamic AdminConfig model/prompt and persists both Q&A into users/{uid}/aiChats.
     */
    fun askAiTafsirForAyah(
        surahNameAr: String,
        surahNumber: Int,
        ayahNumber: Int,
        arabicText: String,
        languageName: String
    ) {
        val cfg = aiRuntimeConfig.value
        if (!cfg.enabled) {
            _ayahTafsirState.value = AiUiState.Error("تم تعطيل المساعد الذكي مؤقتاً من إعدادات الإدارة.")
            return
        }
        if (countTodayUserRequests() >= cfg.dailyLimitPerUser) {
            _ayahTafsirState.value = AiUiState.Error("بلغت الحد اليومي (${cfg.dailyLimitPerUser}) لاستخدام المساعد الذكي.")
            return
        }
        sessionRequestsToday++
        val ref = "$surahNumber:$ayahNumber"
        val userSummaryPrompt = "تفسير ومعاني مفردات سورة $surahNameAr ($ref): $arabicText"

        viewModelScope.launch {
            _ayahTafsirState.value = AiUiState.Loading
            mihrabRepository?.saveAiChatMessage(role = "user", content = userSummaryPrompt, surahRef = ref)

            var latest = ""
            try {
                aiRepository.askAyahTafsirStream(
                    surahNameAr = surahNameAr,
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    arabicText = arabicText,
                    config = cfg,
                    languageName = languageName
                ).collect { chunk ->
                    latest = chunk
                    _ayahTafsirState.value = AiUiState.Streaming(chunk)
                }
                if (latest.isNotBlank()) {
                    _ayahTafsirState.value = AiUiState.Success(latest)
                    mihrabRepository?.saveAiChatMessage(
                        role = "assistant",
                        content = latest,
                        surahRef = ref
                    )
                } else {
                    _ayahTafsirState.value = AiUiState.Error("لم يتم استلام التفسير.")
                }
            } catch (e: TimeoutCancellationException) {
                _ayahTafsirState.value = AiUiState.Error("انتهت مهلة التفسير. حاول مجدداً.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ayah AI Tafsir failed", e)
                _ayahTafsirState.value = AiUiState.Error(e.localizedMessage ?: "تعذر جلب التفسير بالذكاء الاصطناعي.")
            }
        }
    }

    fun generateHifzQuiz(topicOrSurah: String, languageName: String) {
        val cfg = aiRuntimeConfig.value
        if (!cfg.enabled) {
            _hifzQuizState.value = UiState.Error("تم تعطيل المساعد الذكي مؤقتاً من إعدادات الإدارة.")
            return
        }
        if (countTodayUserRequests() >= cfg.dailyLimitPerUser) {
            _hifzQuizState.value = UiState.Error("بلغت الحد اليومي (${cfg.dailyLimitPerUser}) لاستخدام المساعد الذكي.")
            return
        }
        sessionRequestsToday++
        viewModelScope.launch {
            _hifzQuizState.value = UiState.Loading
            aiRepository.generateMemorizationQuiz(
                topicOrSurah = topicOrSurah,
                config = cfg,
                languageName = languageName
            ).onSuccess { item ->
                _hifzQuizState.value = UiState.Success(item)
            }.onFailure { err ->
                _hifzQuizState.value = UiState.Error(err.localizedMessage ?: "تعذر توليد اختبار الحفظ.")
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            mihrabRepository?.clearAiChats()
            _companionState.value = AiUiState.Idle
        }
    }

    fun resetAyahTafsir() {
        _ayahTafsirState.value = AiUiState.Idle
    }

    fun evaluateRecitation(
        surahName: String,
        ayahRef: String,
        expectedArabic: String,
        recitedText: String
    ) {
        viewModelScope.launch {
            _recitationCoachState.value = AiUiState.Loading
            aiRepository.evaluateRecitation(
                surahName = surahName,
                ayahRef = ayahRef,
                expectedArabic = expectedArabic,
                recitedText = recitedText,
                config = aiRuntimeConfig.value
            )
                .onSuccess { feedback ->
                    _recitationCoachState.value = AiUiState.Success(feedback)
                }
                .onFailure { err ->
                    _recitationCoachState.value = AiUiState.Error(err.localizedMessage ?: "Coach failed.")
                }
        }
    }

    fun summarizeKhutbah(topicOrNotes: String, languageName: String = "العربية") {
        val trimmed = topicOrNotes.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _khutbahState.value = UiState.Loading
            aiRepository.generateKhutbahOrWirdSummary(
                notesOrTopic = trimmed,
                config = aiRuntimeConfig.value,
                languageName = languageName
            )
                .onSuccess { summary ->
                    _khutbahState.value = UiState.Success(summary)
                }
                .onFailure { err ->
                    _khutbahState.value = UiState.Error(err.localizedMessage ?: "Summary failed.")
                }
        }
    }

    fun resetRecitationCoach() {
        _recitationCoachState.value = AiUiState.Idle
    }

    private companion object {
        const val TAG = "AiViewModel"
    }
}
