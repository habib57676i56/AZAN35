package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AiCompanionRepository
import com.example.data.firebase.KhutbahSummaryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiViewModel(
    private val aiRepository: AiCompanionRepository = AiCompanionRepository()
) : ViewModel() {

    private val _companionState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val companionState: StateFlow<AiUiState> = _companionState.asStateFlow()

    private val _recitationCoachState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val recitationCoachState: StateFlow<AiUiState> = _recitationCoachState.asStateFlow()

    private val _khutbahState = MutableStateFlow<UiState<KhutbahSummaryResult?>>(UiState.Success(null))
    val khutbahState: StateFlow<UiState<KhutbahSummaryResult?>> = _khutbahState.asStateFlow()

    fun askCompanion(question: String, languageName: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _companionState.value = AiUiState.Loading
            var latest = ""
            try {
                aiRepository.askScholarlyCompanionStream(trimmed, languageName).collect { chunk ->
                    latest = chunk
                    _companionState.value = AiUiState.Streaming(chunk)
                }
                if (latest.isNotBlank()) {
                    _companionState.value = AiUiState.Success(latest)
                } else {
                    _companionState.value = AiUiState.Error("No response received. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Companion stream timed out", e)
                _companionState.value = AiUiState.Error("Request timed out after 60 seconds. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Companion stream failed", e)
                _companionState.value = AiUiState.Error(e.localizedMessage ?: "AI Companion failed.")
            }
        }
    }

    fun evaluateRecitation(
        surahName: String,
        ayahRef: String,
        expectedArabic: String,
        recitedText: String
    ) {
        viewModelScope.launch {
            _recitationCoachState.value = AiUiState.Loading
            aiRepository.evaluateRecitation(surahName, ayahRef, expectedArabic, recitedText)
                .onSuccess { feedback ->
                    _recitationCoachState.value = AiUiState.Success(feedback)
                }
                .onFailure { err ->
                    _recitationCoachState.value = AiUiState.Error(err.localizedMessage ?: "Coach failed.")
                }
        }
    }

    fun summarizeKhutbah(topicOrNotes: String) {
        val trimmed = topicOrNotes.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _khutbahState.value = UiState.Loading
            aiRepository.generateKhutbahOrWirdSummary(trimmed)
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
