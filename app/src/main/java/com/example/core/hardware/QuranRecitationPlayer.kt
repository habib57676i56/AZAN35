package com.example.core.hardware

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.data.firebase.MihrabRepository
import com.example.data.local.AudioCacheDao
import com.example.data.local.CachedAudioEntity
import com.example.data.local.MushafDao
import com.example.data.local.ReciterEntity
import com.example.domain.MushafAyahItem
import com.example.domain.QuranCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

enum class RecitationPlayMode {
    CONTINUOUS_SURAH,
    SINGLE_AYAH,
    A_TO_B_LOOP
}

data class RecitationPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val reciter: ReciterEntity = MihrabRepository.DEFAULT_RECITERS.first { it.id == "ar.alafasy" },
    val currentSurah: Int = 1,
    val currentAyah: Int = 1,
    val currentGlobalAyah: Int = 1,
    val currentPage: Int = 1,
    val speed: Float = 1.0f,
    val playMode: RecitationPlayMode = RecitationPlayMode.CONTINUOUS_SURAH,
    val loopStartAyah: Int = 1,
    val loopEndAyah: Int = 7,
    val loopRepeatTotal: Int = 3,
    val loopRemaining: Int = 3,
    val offlineDownloadingCount: Int = 0,
    val errorMessage: String? = null
)

object RecitationPlayerHolder {
    @Volatile
    var instance: QuranRecitationPlayer? = null

    fun getOrCreate(
        context: Context,
        audioCacheDao: AudioCacheDao,
        mushafDao: MushafDao
    ): QuranRecitationPlayer {
        return instance ?: synchronized(this) {
            instance ?: QuranRecitationPlayer(
                context = context.applicationContext,
                audioCacheDao = audioCacheDao,
                mushafDao = mushafDao
            ).also { instance = it }
        }
    }
}

class QuranRecitationPlayer(
    context: Context,
    private val audioCacheDao: AudioCacheDao,
    private val mushafDao: MushafDao
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioDir = File(appContext.filesDir, "quran_audio").apply { mkdirs() }
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var audioCdnBase: String = "https://everyayah.com/data"

    private var mediaPlayer: MediaPlayer? = null
    private var playJob: Job? = null

    private val _state = MutableStateFlow(RecitationPlaybackState())
    val state: StateFlow<RecitationPlaybackState> = _state.asStateFlow()

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (_state.value.isPlaying) togglePlayPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.isPlaying) {
                    runCatching { mediaPlayer?.pause() }
                    _state.value = _state.value.copy(isPlaying = false, isPaused = true)
                    updateForegroundMediaService()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                runCatching { mediaPlayer?.setVolume(0.35f, 0.35f) }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                runCatching { mediaPlayer?.setVolume(1.0f, 1.0f) }
            }
        }
    }

    init {
        RecitationPlayerHolder.instance = this
    }

    fun setAudioCdnBase(baseUrl: String) {
        val clean = baseUrl.trim().trimEnd('/')
        if (clean.startsWith("https://")) {
            audioCdnBase = clean
        }
    }

    fun setReciter(reciter: ReciterEntity) {
        val wasPlaying = _state.value.isPlaying
        val s = _state.value.currentSurah
        val a = _state.value.currentAyah
        val p = _state.value.currentPage
        _state.value = _state.value.copy(reciter = reciter, errorMessage = null)
        if (wasPlaying) {
            playAyah(s, a, p, reciter, _state.value.playMode)
        }
    }

    fun setSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.75f, 2.0f)
        _state.value = _state.value.copy(speed = safeSpeed)
        applyPlaybackSpeed(safeSpeed)
    }

    fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        pageNumber: Int,
        reciter: ReciterEntity = _state.value.reciter,
        mode: RecitationPlayMode = _state.value.playMode
    ) {
        playJob?.cancel()
        val global = QuranCatalog.globalAyahNumber(surahNumber, ayahNumber)
        _state.value = _state.value.copy(
            isBuffering = true,
            isPlaying = false,
            isPaused = false,
            reciter = reciter,
            currentSurah = surahNumber,
            currentAyah = ayahNumber,
            currentGlobalAyah = global,
            currentPage = pageNumber.coerceIn(1, 604),
            playMode = mode,
            errorMessage = null
        )

        playJob = scope.launch {
            try {
                val file = ensureAyahCached(reciter, surahNumber, ayahNumber, global)
                requestAudioFocus()
                startMediaPlayer(file)
                startForegroundMediaService()
                prefetchNextTwoAyahs(reciter, surahNumber, ayahNumber)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Playback error for $surahNumber:$ayahNumber", e)
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = e.localizedMessage ?: "Audio playback failed"
                )
            }
        }
    }

    fun startAToBLoop(
        surahNumber: Int,
        startAyah: Int,
        endAyah: Int,
        repeats: Int,
        pageNumber: Int
    ) {
        val maxAyah = QuranCatalog.surahByNumber(surahNumber).ayahCount
        val safeStart = startAyah.coerceIn(1, maxAyah)
        val safeEnd = endAyah.coerceIn(safeStart, maxAyah)
        val safeRepeats = repeats.coerceIn(1, 50)
        _state.value = _state.value.copy(
            playMode = RecitationPlayMode.A_TO_B_LOOP,
            loopStartAyah = safeStart,
            loopEndAyah = safeEnd,
            loopRepeatTotal = safeRepeats,
            loopRemaining = safeRepeats
        )
        playAyah(surahNumber, safeStart, pageNumber, _state.value.reciter, RecitationPlayMode.A_TO_B_LOOP)
    }

    fun clearLoopAndContinue() {
        _state.value = _state.value.copy(playMode = RecitationPlayMode.CONTINUOUS_SURAH)
    }

    fun togglePlayPause() {
        val mp = mediaPlayer
        if (_state.value.isPlaying && mp != null) {
            runCatching { mp.pause() }
            _state.value = _state.value.copy(isPlaying = false, isPaused = true)
            updateForegroundMediaService()
        } else if (_state.value.isPaused && mp != null) {
            requestAudioFocus()
            runCatching {
                applyPlaybackSpeed(_state.value.speed)
                mp.start()
            }
            _state.value = _state.value.copy(isPlaying = true, isPaused = false)
            startForegroundMediaService()
        } else {
            val st = _state.value
            playAyah(st.currentSurah, st.currentAyah, st.currentPage, st.reciter, st.playMode)
        }
    }

    fun nextAyah() {
        val st = _state.value
        val meta = QuranCatalog.surahByNumber(st.currentSurah)
        val (nextSurah, nextAyah) = if (st.currentAyah < meta.ayahCount) {
            st.currentSurah to (st.currentAyah + 1)
        } else if (st.currentSurah < 114) {
            (st.currentSurah + 1) to 1
        } else {
            1 to 1
        }
        scope.launch {
            val resolvedPage = resolvePageForAyah(nextSurah, nextAyah, st.currentPage)
            playAyah(nextSurah, nextAyah, resolvedPage, st.reciter, st.playMode)
        }
    }

    fun previousAyah() {
        val st = _state.value
        val (prevSurah, prevAyah) = if (st.currentAyah > 1) {
            st.currentSurah to (st.currentAyah - 1)
        } else if (st.currentSurah > 1) {
            val prevMeta = QuranCatalog.surahByNumber(st.currentSurah - 1)
            prevMeta.number to prevMeta.ayahCount
        } else {
            1 to 1
        }
        scope.launch {
            val resolvedPage = resolvePageForAyah(prevSurah, prevAyah, st.currentPage)
            playAyah(prevSurah, prevAyah, resolvedPage, st.reciter, st.playMode)
        }
    }

    fun cachePageAyahsOffline(ayahs: List<MushafAyahItem>) {
        if (ayahs.isEmpty()) return
        val reciter = _state.value.reciter
        scope.launch {
            _state.value = _state.value.copy(offlineDownloadingCount = ayahs.size)
            for (ayah in ayahs) {
                runCatching {
                    ensureAyahCached(reciter, ayah.surahNumber, ayah.numberInSurah, ayah.globalNumber)
                }
            }
            _state.value = _state.value.copy(offlineDownloadingCount = 0)
        }
    }

    fun stop() {
        stopFromService()
        stopForegroundMediaService()
    }

    fun stopFromService() {
        playJob?.cancel()
        abandonAudioFocus()
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _state.value = _state.value.copy(isPlaying = false, isPaused = false, isBuffering = false)
    }

    fun release() {
        stop()
        if (RecitationPlayerHolder.instance === this) {
            RecitationPlayerHolder.instance = null
        }
    }

    private fun requestAudioFocus() {
        val mgr = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
                .also { focusRequest = it }
            runCatching { mgr.requestAudioFocus(req) }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                mgr.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
        }
    }

    private fun abandonAudioFocus() {
        val mgr = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { runCatching { mgr.abandonAudioFocusRequest(it) } }
        } else {
            @Suppress("DEPRECATION")
            runCatching { mgr.abandonAudioFocus(audioFocusListener) }
        }
    }

    private fun startMediaPlayer(file: File) {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        val mp = MediaPlayer().apply {
            setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(file.absolutePath)
            setOnPreparedListener { player ->
                applyPlaybackSpeed(_state.value.speed, player)
                player.start()
                _state.value = _state.value.copy(
                    isPlaying = true,
                    isPaused = false,
                    isBuffering = false,
                    errorMessage = null
                )
                updateForegroundMediaService()
            }
            setOnCompletionListener {
                handleAyahCompleted()
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Audio error ($what)"
                )
                true
            }
            prepareAsync()
        }
        mediaPlayer = mp
    }

    private fun handleAyahCompleted() {
        val st = _state.value
        when (st.playMode) {
            RecitationPlayMode.SINGLE_AYAH -> {
                _state.value = st.copy(isPlaying = false, isPaused = false)
                stopForegroundMediaService()
            }
            RecitationPlayMode.A_TO_B_LOOP -> {
                if (st.currentAyah < st.loopEndAyah) {
                    scope.launch {
                        val nextAyah = st.currentAyah + 1
                        val page = resolvePageForAyah(st.currentSurah, nextAyah, st.currentPage)
                        playAyah(st.currentSurah, nextAyah, page, st.reciter, RecitationPlayMode.A_TO_B_LOOP)
                    }
                } else if (st.loopRemaining > 1) {
                    _state.value = st.copy(loopRemaining = st.loopRemaining - 1)
                    scope.launch {
                        val page = resolvePageForAyah(st.currentSurah, st.loopStartAyah, st.currentPage)
                        playAyah(st.currentSurah, st.loopStartAyah, page, st.reciter, RecitationPlayMode.A_TO_B_LOOP)
                    }
                } else {
                    _state.value = st.copy(isPlaying = false, isPaused = false, loopRemaining = 0)
                    stopForegroundMediaService()
                }
            }
            RecitationPlayMode.CONTINUOUS_SURAH -> {
                nextAyah()
            }
        }
    }

    private suspend fun resolvePageForAyah(surah: Int, ayah: Int, fallbackPage: Int): Int {
        val indexed = withContext(Dispatchers.IO) { mushafDao.getAyah(surah, ayah) }
        if (indexed != null) return indexed.pageNumber
        val meta = QuranCatalog.surahByNumber(surah)
        if (ayah == 1) return meta.startPage
        return fallbackPage
    }

    private suspend fun ensureAyahCached(
        reciter: ReciterEntity,
        surahNumber: Int,
        ayahNumber: Int,
        globalAyah: Int
    ): File = withContext(Dispatchers.IO) {
        val cacheKey = "${reciter.id}_$globalAyah"
        val existing = audioCacheDao.getCachedAudio(cacheKey)
        if (existing != null) {
            val file = File(existing.localFilePath)
            if (file.exists() && file.length() > 1024L) {
                return@withContext file
            }
        }

        val reciterDir = File(audioDir, reciter.id.replace(Regex("[^a-zA-Z0-9._-]"), "_")).apply { mkdirs() }
        val targetFile = File(reciterDir, "${surahNumber}_${ayahNumber}_$globalAyah.mp3")
        if (targetFile.exists() && targetFile.length() > 1024L) {
            audioCacheDao.insertCachedAudio(
                CachedAudioEntity(cacheKey, reciter.id, globalAyah, targetFile.absolutePath, targetFile.length())
            )
            return@withContext targetFile
        }

        val surahStr = surahNumber.toString().padStart(3, '0')
        val ayahStr = ayahNumber.toString().padStart(3, '0')
        val primaryUrl = "${audioCdnBase}/${reciter.everyAyahFolder}/$surahStr$ayahStr.mp3"
        val fallbackUrl = "https://cdn.islamic.network/quran/audio/128/${reciter.editionId}/$globalAyah.mp3"

        val bytes = downloadBytesOrNull(primaryUrl)
            ?: downloadBytesOrNull(fallbackUrl)
            ?: throw IllegalStateException("تعذر تنزيل التلاوة الصوتية؛ تحقق من الاتصال.")

        targetFile.writeBytes(bytes)
        audioCacheDao.insertCachedAudio(
            CachedAudioEntity(cacheKey, reciter.id, globalAyah, targetFile.absolutePath, targetFile.length())
        )
        targetFile
    }

    private fun downloadBytesOrNull(url: String): ByteArray? = runCatching {
        val req = Request.Builder().url(url).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val bytes = resp.body?.bytes()
            if (bytes != null && bytes.size > 1024) bytes else null
        }
    }.getOrNull()

    private fun prefetchNextTwoAyahs(reciter: ReciterEntity, surah: Int, ayah: Int) {
        scope.launch(Dispatchers.IO) {
            val meta = QuranCatalog.surahByNumber(surah)
            val next1 = if (ayah + 1 <= meta.ayahCount) surah to (ayah + 1) else null
            val next2 = if (ayah + 2 <= meta.ayahCount) surah to (ayah + 2) else null
            listOfNotNull(next1, next2).forEach { (s, a) ->
                runCatching {
                    ensureAyahCached(reciter, s, a, QuranCatalog.globalAyahNumber(s, a))
                }
            }
        }
    }

    private fun applyPlaybackSpeed(speed: Float, target: MediaPlayer? = mediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && target != null) {
            runCatching {
                target.playbackParams = target.playbackParams.setSpeed(speed.coerceIn(0.75f, 2.0f))
            }
        }
    }

    private fun startForegroundMediaService() {
        runCatching {
            val intent = Intent(appContext, QuranAudioPlaybackService::class.java).apply {
                action = QuranAudioPlaybackService.ACTION_START_OR_UPDATE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        }
    }

    private fun updateForegroundMediaService() {
        runCatching {
            val intent = Intent(appContext, QuranAudioPlaybackService::class.java).apply {
                action = QuranAudioPlaybackService.ACTION_START_OR_UPDATE
            }
            appContext.startService(intent)
        }
    }

    private fun stopForegroundMediaService() {
        runCatching {
            val intent = Intent(appContext, QuranAudioPlaybackService::class.java).apply {
                action = QuranAudioPlaybackService.ACTION_STOP
            }
            appContext.startService(intent)
        }
    }

    private companion object {
        const val TAG = "QuranRecitationPlayer"
    }
}
