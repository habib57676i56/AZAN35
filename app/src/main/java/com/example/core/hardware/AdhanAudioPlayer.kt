package com.example.core.hardware

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.firebase.AdhanAlertMode
import com.example.data.local.AdhanVoiceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class AdhanPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val activeLabel: String = "",
    val activeMode: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val errorMessage: String? = null
)

class AdhanAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val adhanCacheDir = File(appContext.filesDir, "adhan_cache").apply { mkdirs() }
    private val customAdhanDir = File(appContext.filesDir, "adhan_custom").apply { mkdirs() }

    private var mediaPlayer: MediaPlayer? = null
    private var stopTimerJob: Job? = null
    private var playJob: Job? = null

    private val _state = MutableStateFlow(AdhanPlaybackState())
    val state: StateFlow<AdhanPlaybackState> = _state.asStateFlow()

    /**
     * Copies a user-selected audio file from Android's zero-permission Storage Access Framework URI
     * into app-private filesDir so it stays playable offline across restarts without storage permissions.
     */
    suspend fun importCustomAudioFromUri(uri: Uri): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val resolver = appContext.contentResolver
            var displayName = "Custom_Adhan.mp3"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        displayName = cursor.getString(idx)?.takeIf { it.isNotBlank() } ?: displayName
                    }
                }
            }

            // Clear older custom adhan files so app storage remains lean
            customAdhanDir.listFiles()?.forEach { it.delete() }

            val safeExt = displayName.substringAfterLast('.', "mp3")
                .lowercase()
                .replace(Regex("[^a-z0-9]"), "")
                .ifBlank { "mp3" }
            val destFile = File(customAdhanDir, "user_custom_adhan.$safeExt")

            val copiedBytes = resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Unable to read selected audio file.")

            if (copiedBytes <= 0L) {
                throw IllegalStateException("Selected audio file appears to be empty.")
            }

            Result.success(destFile.absolutePath to displayName.take(80))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun playPreview(
        voice: AdhanVoiceEntity,
        mode: AdhanAlertMode,
        customLocalPath: String? = null,
        customDisplayName: String? = null
    ) {
        stop()

        if (mode == AdhanAlertMode.SILENT) {
            _state.value = AdhanPlaybackState(
                isPlaying = false,
                activeLabel = voice.nameAr,
                activeMode = mode
            )
            return
        }

        if (mode == AdhanAlertMode.GENTLE_CHIME) {
            playGentleChime(voice.nameAr)
            return
        }

        val label = if (!customLocalPath.isNullOrBlank() && File(customLocalPath).exists()) {
            customDisplayName?.ifBlank { voice.nameAr } ?: voice.nameAr
        } else {
            voice.nameAr
        }

        playJob = scope.launch {
            _state.value = AdhanPlaybackState(
                isBuffering = true,
                activeLabel = label,
                activeMode = mode
            )

            val customFile = customLocalPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 512L }
            val audioFile = customFile ?: downloadAndCacheAdhan(voice)

            if (audioFile == null || !audioFile.exists()) {
                // Graceful fallback to gentle chime if offline and Adhan track is not yet cached
                playGentleChime(label, offlineFallbackNotice = "تعذر تحميل ملف الأذان؛ تم تشغيل تنبيه صوتي لطيف.")
                return@launch
            }

            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(audioFile.absolutePath)
                    setOnCompletionListener {
                        stop()
                    }
                    setOnErrorListener { _, _, _ ->
                        stop()
                        true
                    }
                    prepare()
                    start()
                }
                mediaPlayer = player
                _state.value = AdhanPlaybackState(
                    isPlaying = true,
                    isBuffering = false,
                    activeLabel = label,
                    activeMode = mode
                )

                // Takbir Only mode stops cleanly after the opening Takbir (~11 seconds)
                if (mode == AdhanAlertMode.TAKBIR_ONLY) {
                    stopTimerJob = scope.launch {
                        delay(11_000L)
                        stop()
                    }
                }
            } catch (e: Exception) {
                _state.value = AdhanPlaybackState(
                    errorMessage = e.localizedMessage ?: "تعذر تشغيل ملف الأذان"
                )
            }
        }
    }

    private fun playGentleChime(label: String, offlineFallbackNotice: String? = null) {
        playJob = scope.launch {
            _state.value = AdhanPlaybackState(
                isPlaying = true,
                activeLabel = label,
                activeMode = AdhanAlertMode.GENTLE_CHIME,
                errorMessage = offlineFallbackNotice
            )
            withContext(Dispatchers.IO) {
                var tg: ToneGenerator? = null
                try {
                    tg = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 320)
                    Thread.sleep(420)
                    tg.startTone(ToneGenerator.TONE_PROP_ACK, 380)
                    Thread.sleep(450)
                } catch (_: Exception) {
                } finally {
                    try {
                        tg?.release()
                    } catch (_: Exception) {
                    }
                }
            }
            _state.value = AdhanPlaybackState(
                isPlaying = false,
                activeLabel = label,
                activeMode = AdhanAlertMode.GENTLE_CHIME,
                errorMessage = offlineFallbackNotice
            )
        }
    }

    private suspend fun downloadAndCacheAdhan(voice: AdhanVoiceEntity): File? = withContext(Dispatchers.IO) {
        val safeId = voice.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val cached = File(adhanCacheDir, "$safeId.mp3")
        if (cached.exists() && cached.length() > 4096L) {
            return@withContext cached
        }
        val url = voice.audioUrl.trim()
        if (!url.startsWith("https://")) return@withContext null

        try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body ?: return@withContext null
                val tmp = File(adhanCacheDir, "$safeId.mp3.tmp")
                tmp.outputStream().use { out ->
                    body.byteStream().copyTo(out)
                }
                if (tmp.length() > 4096L) {
                    tmp.renameTo(cached)
                    cached
                } else {
                    tmp.delete()
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun stop() {
        stopTimerJob?.cancel()
        stopTimerJob = null
        playJob?.cancel()
        playJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _state.value = AdhanPlaybackState()
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
