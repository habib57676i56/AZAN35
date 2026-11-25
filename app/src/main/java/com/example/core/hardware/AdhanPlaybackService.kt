package com.example.core.hardware

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.data.firebase.AdhanAlertMode
import com.example.domain.AdhkarAndAdhanCatalog
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

data class AdhanRingingInfo(
    val prayerKey: String,
    val prayerLabel: String,
    val prayerTime: String,
    val muezzinLabel: String,
    val mode: AdhanAlertMode
)

object AdhanRuntimeController {
    private val _ringing = MutableStateFlow<AdhanRingingInfo?>(null)
    val ringing: StateFlow<AdhanRingingInfo?> = _ringing.asStateFlow()

    @Volatile
    var activePlayerForSilence: AdhanAudioPlayer? = null

    fun isRingingNow(): Boolean =
        _ringing.value != null || (activePlayerForSilence?.state?.value?.isPlaying == true)

    internal fun setRinging(info: AdhanRingingInfo?) {
        _ringing.value = info
    }

    fun triggerAdhan(
        context: Context,
        prayerKey: String,
        prayerLabel: String,
        prayerTime: String,
        voiceId: String,
        mode: AdhanAlertMode,
        customLocalPath: String,
        language: AppLanguage
    ) {
        val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_PLAY_ADHAN
            putExtra(AdhanPlaybackService.EXTRA_PRAYER_KEY, prayerKey)
            putExtra(AdhanPlaybackService.EXTRA_PRAYER_LABEL, prayerLabel)
            putExtra(AdhanPlaybackService.EXTRA_PRAYER_TIME, prayerTime)
            putExtra(AdhanPlaybackService.EXTRA_VOICE_ID, voiceId)
            putExtra(AdhanPlaybackService.EXTRA_ALERT_MODE, mode.id)
            putExtra(AdhanPlaybackService.EXTRA_CUSTOM_PATH, customLocalPath)
            putExtra(AdhanPlaybackService.EXTRA_LANG_CODE, language.code)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.applicationContext.startForegroundService(intent) }
        } else {
            runCatching { context.applicationContext.startService(intent) }
        }
    }

    fun stopAdhan(context: Context) {
        activePlayerForSilence?.stop()
        _ringing.value = null
        val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_STOP_ADHAN
        }
        runCatching { context.applicationContext.startService(intent) }
    }
}

class AdhanPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var playJob: Job? = null
    private var stopTimerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var artworkBitmap: Bitmap? = null

    private var audioManager: AudioManager? = null
    private var baselineVolumes: IntArray = intArrayOf()
    private var volumeObserver: ContentObserver? = null
    private var volumeReceiverRegistered = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val volumeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION" && AdhanRuntimeController.ringing.value != null) {
                stopAndShutdown()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        createChannel()
        initMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ADHAN -> {
                stopAndShutdown()
                return START_NOT_STICKY
            }
            ACTION_PLAY_ADHAN -> {
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: "prayer"
                val prayerLabel = intent.getStringExtra(EXTRA_PRAYER_LABEL) ?: "الصلاة"
                val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
                val voiceId = intent.getStringExtra(EXTRA_VOICE_ID) ?: "makkah"
                val mode = AdhanAlertMode.fromId(intent.getStringExtra(EXTRA_ALERT_MODE))
                val customPath = intent.getStringExtra(EXTRA_CUSTOM_PATH).orEmpty()
                val lang = AppLanguage.fromCode(intent.getStringExtra(EXTRA_LANG_CODE) ?: "ar")
                startAdhan(prayerKey, prayerLabel, prayerTime, voiceId, mode, customPath, lang)
            }
        }
        return START_NOT_STICKY
    }

    private fun startAdhan(
        prayerKey: String,
        prayerLabel: String,
        prayerTime: String,
        voiceId: String,
        mode: AdhanAlertMode,
        customPath: String,
        language: AppLanguage
    ) {
        stopPlaybackOnly()

        val voice = AdhkarAndAdhanCatalog.defaultAdhanVoices.firstOrNull { it.id == voiceId }
            ?: AdhkarAndAdhanCatalog.defaultAdhanVoices.first()
        val muezzinLabel = if (language.isArabicUi) voice.nameAr else voice.nameEn
        val info = AdhanRingingInfo(
            prayerKey = prayerKey,
            prayerLabel = prayerLabel,
            prayerTime = prayerTime,
            muezzinLabel = muezzinLabel,
            mode = mode
        )
        AdhanRuntimeController.setRinging(info)
        updateMediaSessionState(info, language)

        val notification = buildNotification(info, language)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        registerVolumeWatchers()

        if (mode == AdhanAlertMode.SILENT) {
            // Leave entry notification visible without sound if SILENT was passed
            return
        }

        if (mode == AdhanAlertMode.GENTLE_CHIME) {
            playJob = serviceScope.launch {
                playGentleChimeTone()
                stopAndShutdown()
            }
            return
        }

        playJob = serviceScope.launch {
            val customFile = customPath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.length() > 512L }
            val adhanFile = customFile ?: downloadOrGetCachedAdhan(voice.id, voice.audioUrl)
            if (adhanFile == null || !adhanFile.exists()) {
                playGentleChimeTone()
                stopAndShutdown()
                return@launch
            }

            try {
                val player = MediaPlayer().apply {
                    setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(adhanFile.absolutePath)
                    setOnCompletionListener {
                        stopAndShutdown()
                    }
                    setOnErrorListener { _, _, _ ->
                        stopAndShutdown()
                        true
                    }
                    prepare()
                    start()
                }
                mediaPlayer = player

                if (mode == AdhanAlertMode.TAKBIR_ONLY) {
                    stopTimerJob = serviceScope.launch {
                        delay(11_500L)
                        stopAndShutdown()
                    }
                }
            } catch (_: Exception) {
                playGentleChimeTone()
                stopAndShutdown()
            }
        }
    }

    private suspend fun playGentleChimeTone() = withContext(Dispatchers.IO) {
        var tg: ToneGenerator? = null
        try {
            tg = ToneGenerator(AudioManager.STREAM_ALARM, 80)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 340)
            Thread.sleep(430)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 420)
            Thread.sleep(500)
        } catch (_: Exception) {
        } finally {
            runCatching { tg?.release() }
        }
    }

    private suspend fun downloadOrGetCachedAdhan(voiceId: String, audioUrl: String): File? =
        withContext(Dispatchers.IO) {
            val cacheDir = File(filesDir, "adhan_cache").apply { mkdirs() }
            val safeId = voiceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val cached = File(cacheDir, "$safeId.mp3")
            if (cached.exists() && cached.length() > 4096L) return@withContext cached
            if (!audioUrl.startsWith("https://")) return@withContext null

            runCatching {
                val req = Request.Builder().url(audioUrl).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body ?: return@use null
                    val tmp = File(cacheDir, "$safeId.mp3.tmp")
                    tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                    if (tmp.length() > 4096L) {
                        tmp.renameTo(cached)
                        cached
                    } else {
                        tmp.delete()
                        null
                    }
                }
            }.getOrNull()
        }

    private fun registerVolumeWatchers() {
        val mgr = audioManager ?: return
        baselineVolumes = intArrayOf(
            mgr.getStreamVolume(AudioManager.STREAM_MUSIC),
            mgr.getStreamVolume(AudioManager.STREAM_ALARM),
            mgr.getStreamVolume(AudioManager.STREAM_RING)
        )

        if (volumeObserver == null) {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    val m = audioManager ?: return
                    if (AdhanRuntimeController.ringing.value == null) return
                    val current = intArrayOf(
                        m.getStreamVolume(AudioManager.STREAM_MUSIC),
                        m.getStreamVolume(AudioManager.STREAM_ALARM),
                        m.getStreamVolume(AudioManager.STREAM_RING)
                    )
                    if (baselineVolumes.size == 3 && !current.contentEquals(baselineVolumes)) {
                        stopAndShutdown()
                    }
                }
            }
            runCatching {
                contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
                volumeObserver = observer
            }
        }

        if (!volumeReceiverRegistered) {
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { registerReceiver(volumeBroadcastReceiver, filter, RECEIVER_EXPORTED) }
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                runCatching { registerReceiver(volumeBroadcastReceiver, filter) }
            }
            volumeReceiverRegistered = true
        }
    }

    private fun unregisterVolumeWatchers() {
        volumeObserver?.let {
            runCatching { contentResolver.unregisterContentObserver(it) }
        }
        volumeObserver = null
        if (volumeReceiverRegistered) {
            runCatching { unregisterReceiver(volumeBroadcastReceiver) }
            volumeReceiverRegistered = false
        }
    }

    private fun initMediaSession() {
        val session = MediaSession(this, "MihrabAdhanSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPause() = stopAndShutdown()
                override fun onStop() = stopAndShutdown()
                override fun onPlay() = stopAndShutdown()
            })
            isActive = true
        }
        mediaSession = session
    }

    private fun updateMediaSessionState(info: AdhanRingingInfo, language: AppLanguage) {
        val session = mediaSession ?: return
        val art = artworkBitmap ?: buildAdhanArtwork().also { artworkBitmap = it }
        val title = language.pick(
            "حان وقت صلاة ${info.prayerLabel} • ${info.prayerTime}",
            "Adhan: ${info.prayerLabel} • ${info.prayerTime}",
            "Azon: ${info.prayerLabel} • ${info.prayerTime}"
        )
        val subtitle = "${info.muezzinLabel} • ${
            language.pick(
                "اضغط زر الصوت للإيقاف",
                "Press volume key to silence",
                "O'chirish uchun ovoz tugmasini bosing"
            )
        }"

        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, subtitle)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "محراب • Mihrab")
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
                .build()
        )

        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_STOP or PlaybackState.ACTION_PAUSE)
                .setState(PlaybackState.STATE_PLAYING, 0L, 1.0f)
                .build()
        )
    }

    private fun buildNotification(info: AdhanRingingInfo, language: AppLanguage): Notification {
        val openPending = PendingIntent.getActivity(
            this,
            901,
            Intent(this, MainActivity::class.java).apply {
                putExtra("launch_tab", "prayer")
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getService(
            this,
            902,
            Intent(this, AdhanPlaybackService::class.java).apply { action = ACTION_STOP_ADHAN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = language.pick(
            "الأذان • حان الآن وقت صلاة ${info.prayerLabel}",
            "Adhan • Time for ${info.prayerLabel}",
            "Azon • ${info.prayerLabel} namozi vaqti kirdi"
        )
        val body = language.pick(
            "${info.muezzinLabel} (${info.prayerTime}) • اضغط إيقاف أو زر خفض/رفع الصوت",
            "${info.muezzinLabel} (${info.prayerTime}) • Tap Stop or press Volume key to mute",
            "${info.muezzinLabel} (${info.prayerTime}) • To'xtatish yoki ovoz tugmasini bosing"
        )
        val stopLabel = language.pick("إيقاف الأذان", "Stop Adhan", "Azonni to'xtatish")

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder
            .setSmallIcon(R.drawable.ic_notif_mihrab)
            .setLargeIcon(artworkBitmap ?: buildAdhanArtwork().also { artworkBitmap = it })
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setContentIntent(openPending)
            .setDeleteIntent(stopPending)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_ALARM)
            .addAction(
                Notification.Action.Builder(
                    R.drawable.ic_media_stop,
                    stopLabel,
                    stopPending
                ).build()
            )

        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات الأذان والمواقيت",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دخول وقت الصلاة وتشغيل الأذان مع التحكم من شاشة القفل"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null) // Service handles Adhan audio directly so volume keys silence cleanly
            }
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildAdhanArtwork(): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(0xFF072B23.toInt(), 0xFF0D5C4B.toInt(), 0xFF061A15.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 64f, 64f, bgPaint)
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE5BE67.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.34f, goldPaint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            textSize = 72f
            isFakeBoldText = true
        }
        canvas.drawText("الأذان", size / 2f, size * 0.48f, textPaint)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE5BE67.toInt()
            textAlign = Paint.Align.CENTER
            textSize = 36f
        }
        canvas.drawText("MIHRAB", size / 2f, size * 0.61f, subPaint)
        return bmp
    }

    private fun stopPlaybackOnly() {
        stopTimerJob?.cancel()
        stopTimerJob = null
        playJob?.cancel()
        playJob = null
        runCatching {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        }
        mediaPlayer = null
    }

    private fun stopAndShutdown() {
        stopPlaybackOnly()
        unregisterVolumeWatchers()
        AdhanRuntimeController.setRinging(null)
        runCatching {
            mediaSession?.setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_STOPPED, 0L, 1.0f)
                    .build()
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopPlaybackOnly()
        unregisterVolumeWatchers()
        AdhanRuntimeController.setRinging(null)
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
        artworkBitmap?.recycle()
        artworkBitmap = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "mihrab_adhan_alerts"
        const val NOTIFICATION_ID = 7088
        const val ACTION_PLAY_ADHAN = "com.example.action.PLAY_ADHAN"
        const val ACTION_STOP_ADHAN = "com.example.action.STOP_ADHAN"

        const val EXTRA_PRAYER_KEY = "prayer_key"
        const val EXTRA_PRAYER_LABEL = "prayer_label"
        const val EXTRA_PRAYER_TIME = "prayer_time"
        const val EXTRA_VOICE_ID = "voice_id"
        const val EXTRA_ALERT_MODE = "alert_mode"
        const val EXTRA_CUSTOM_PATH = "custom_path"
        const val EXTRA_LANG_CODE = "lang_code"
    }
}
