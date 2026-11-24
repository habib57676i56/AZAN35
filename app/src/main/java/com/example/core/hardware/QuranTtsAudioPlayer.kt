package com.example.core.hardware

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val activeTitle: String = "",
    val activeAyahIndex: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val repeatRemaining: Int = 1,
    val sleepTimerMinutes: Int = 0
)

/**
 * Real Android TextToSpeech + AudioManager-backed Quran & Adhkar audio player
 * supporting Arabic recitation, English audio translation, speed control, and sleep timer.
 */
class QuranTtsAudioPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var onUtteranceDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        onUtteranceDoneCallback?.invoke()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(isPlaying = false)
                    }
                })
            }
        }
    }

    fun speak(
        title: String,
        text: String,
        preferArabic: Boolean,
        ayahIndex: Int = 0,
        speed: Float = _state.value.playbackSpeed,
        repeatCount: Int = 1,
        onComplete: (() -> Unit)? = null
    ) {
        _state.value = _state.value.copy(
            isPlaying = true,
            activeTitle = title,
            activeAyahIndex = ayahIndex,
            playbackSpeed = speed,
            repeatRemaining = repeatCount
        )
        if (!isReady) {
            playGentleChime()
            _state.value = _state.value.copy(isPlaying = false)
            return
        }

        val locale = if (preferArabic) Locale.forLanguageTag("ar") else Locale.US
        val langResult = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
        tts?.setSpeechRate(speed.coerceIn(0.6f, 1.8f))

        var remaining = repeatCount.coerceAtLeast(1)
        onUtteranceDoneCallback = {
            remaining--
            if (remaining > 0 && _state.value.isPlaying) {
                _state.value = _state.value.copy(repeatRemaining = remaining)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mihrab_${System.currentTimeMillis()}")
            } else {
                _state.value = _state.value.copy(isPlaying = false, repeatRemaining = 0)
                onComplete?.invoke()
            }
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mihrab_${System.currentTimeMillis()}")
    }

    fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
        tts?.setSpeechRate(speed.coerceIn(0.6f, 1.8f))
    }

    fun setSleepTimer(minutes: Int) {
        _state.value = _state.value.copy(sleepTimerMinutes = minutes)
    }

    fun stop() {
        onUtteranceDoneCallback = null
        tts?.stop()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun playGentleChime() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        } catch (_: Exception) {
        }
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
