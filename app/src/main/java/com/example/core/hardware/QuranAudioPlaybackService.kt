package com.example.core.hardware

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.MainActivity
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.domain.QuranCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuranAudioPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObserverJob: Job? = null
    private var mediaSession: MediaSession? = null

    private var cachedArtKey: String = ""
    private var cachedArtwork: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        observePlayerState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stateObserverJob?.cancel()
        serviceScope.cancel()
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
        cachedArtwork?.recycle()
        cachedArtwork = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PAUSE -> {
                RecitationPlayerHolder.instance?.togglePlayPause()
                updateNotification()
            }
            ACTION_NEXT -> RecitationPlayerHolder.instance?.nextAyah()
            ACTION_PREV -> RecitationPlayerHolder.instance?.previousAyah()
            ACTION_SWITCH_RECITER -> {
                RecitationPlayerHolder.instance?.switchToNextReciter()
                updateNotification()
            }
            ACTION_STOP -> {
                RecitationPlayerHolder.instance?.stopFromService()
                runCatching { mediaSession?.isActive = false }
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> updateNotification()
        }
        val active = RecitationPlayerHolder.instance?.state?.value?.let {
            it.isPlaying || it.isPaused || it.isBuffering
        } ?: false
        return if (active) START_STICKY else START_NOT_STICKY
    }

    private fun initMediaSession() {
        if (mediaSession != null) return
        val session = MediaSession(this, "MihrabQuranMediaSession")
        session.setCallback(
            object : MediaSession.Callback() {
                override fun onPlay() {
                    RecitationPlayerHolder.instance?.togglePlayPause()
                }

                override fun onPause() {
                    RecitationPlayerHolder.instance?.togglePlayPause()
                }

                override fun onSkipToNext() {
                    RecitationPlayerHolder.instance?.nextAyah()
                }

                override fun onSkipToPrevious() {
                    RecitationPlayerHolder.instance?.previousAyah()
                }

                override fun onStop() {
                    RecitationPlayerHolder.instance?.stopFromService()
                    session.isActive = false
                    stopForegroundCompat()
                    stopSelf()
                }

                override fun onSeekTo(pos: Long) {
                    RecitationPlayerHolder.instance?.seekTo(pos)
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    when (action) {
                        CUSTOM_ACTION_SWITCH_RECITER -> {
                            RecitationPlayerHolder.instance?.switchToNextReciter()
                        }
                        CUSTOM_ACTION_CYCLE_SPEED -> {
                            val player = RecitationPlayerHolder.instance ?: return
                            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                            val cur = player.state.value.speed
                            val nextIdx = (speeds.indexOfFirst { kotlin.math.abs(it - cur) < 0.05f } + 1) % speeds.size
                            player.setSpeed(speeds[nextIdx])
                        }
                    }
                }
            }
        )
        session.isActive = true
        mediaSession = session
    }

    private fun observePlayerState() {
        stateObserverJob?.cancel()
        val player = RecitationPlayerHolder.instance ?: return
        stateObserverJob = serviceScope.launch {
            player.state.collectLatest { st ->
                if (st.isPlaying || st.isPaused || st.isBuffering) {
                    updateNotification()
                }
            }
        }
    }

    private fun updateNotification() {
        initMediaSession()
        val lang = PrayerStatusNotificationManager.savedLanguage(this)
        createChannelIfNeeded(lang)
        val state = RecitationPlayerHolder.instance?.state?.value
        val surahNum = state?.currentSurah ?: 1
        val ayahNum = state?.currentAyah ?: 1
        val surahMeta = QuranCatalog.surahByNumber(surahNum)

        val surahName = if (lang.isArabicUi) surahMeta.nameAr else surahMeta.nameEn
        val reciterName = if (lang.isArabicUi) {
            state?.reciter?.nameAr ?: "مشاري راشد العفاسي"
        } else {
            state?.reciter?.nameEn ?: "Mishary Rashid Alafasy"
        }
        val reciterStyle = if (lang.isArabicUi) {
            state?.reciter?.styleAr ?: "مرتل"
        } else {
            state?.reciter?.styleEn ?: "Murattal"
        }

        val title = lang.pick(
            "سورة $surahName • آية $ayahNum",
            "Surah $surahName • Ayah $ayahNum",
            "$surahName surasi • $ayahNum-oyat"
        )
        val subtitle = "$reciterName • $reciterStyle"

        val artKey = "${state?.reciter?.id ?: "default"}_$surahNum"
        if (cachedArtKey != artKey || cachedArtwork == null) {
            cachedArtwork?.recycle()
            cachedArtwork = buildReciterLockScreenArtwork(
                reciterName = reciterName,
                surahNameAr = surahMeta.nameAr,
                surahSub = lang.pick("سورة ${surahMeta.nameAr}", surahMeta.nameEn, "${surahMeta.nameEn} surasi")
            )
            cachedArtKey = artKey
        }

        val session = mediaSession
        if (session != null) {
            session.isActive = true
            val duration = (state?.durationMs ?: 0L).coerceAtLeast(1L)
            val position = (state?.positionMs ?: 0L).coerceIn(0L, duration)
            val pbStateCode = when {
                state?.isBuffering == true -> PlaybackState.STATE_BUFFERING
                state?.isPlaying == true -> PlaybackState.STATE_PLAYING
                state?.isPaused == true -> PlaybackState.STATE_PAUSED
                else -> PlaybackState.STATE_STOPPED
            }
            val switchLabel = lang.pick("القارئ التالي", "Next Sheikh", "Keyingi qori")
            val speedLabel = "${state?.speed ?: 1.0f}x"
            val pbState = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_SEEK_TO
                )
                .addCustomAction(CUSTOM_ACTION_SWITCH_RECITER, switchLabel, R.drawable.ic_notif_mihrab)
                .addCustomAction(CUSTOM_ACTION_CYCLE_SPEED, speedLabel, R.drawable.ic_notif_refresh)
                .setState(pbStateCode, position, state?.speed ?: 1.0f)
                .build()
            session.setPlaybackState(pbState)

            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, reciterName)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
                .putString(
                    MediaMetadata.METADATA_KEY_ALBUM,
                    lang.pick("محراب • القرآن الكريم", "Mihrab • Holy Quran", "Mihrob • Qur'oni Karim")
                )
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .apply {
                    cachedArtwork?.let { putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) }
                }
                .build()
            session.setMetadata(metadata)
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("launch_tab", "quran")
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_TOGGLE_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val switchReciterIntent = PendingIntent.getService(
            this,
            5,
            Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_SWITCH_RECITER },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPlaying = state?.isPlaying == true
        val prevLabel = lang.pick("السابق", "Prev", "Oldingi")
        val toggleLabel = if (isPlaying) lang.pick("إيقاف مؤقت", "Pause", "Pauza") else lang.pick("تشغيل", "Play", "Ijro")
        val nextLabel = lang.pick("التالي", "Next", "Keyingi")
        val sheikhSwitchLabel = lang.pick("تبديل الشيخ", "Switch Sheikh", "Qorini almashtirish")
        val stopLabel = lang.pick("إيقاف", "Stop", "To'xtatish")

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && session != null) {
            val builder = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif_mihrab)
                .setColor(0xFF0F6B4E.toInt())
                .setColorized(true)
                .setLargeIcon(cachedArtwork)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSubText(lang.pick("تلاوة القراء • اضغط واسحب", "Reciters Live • Swipe controls", "Qorilar tilovati"))
                .setContentIntent(openAppIntent)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_media_prev),
                        prevLabel,
                        prevIntent
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(
                            this,
                            if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
                        ),
                        toggleLabel,
                        toggleIntent
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_media_next),
                        nextLabel,
                        nextIntent
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_notif_mihrab),
                        sheikhSwitchLabel,
                        switchReciterIntent
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_media_stop),
                        stopLabel,
                        stopIntent
                    ).build()
                )
                .setStyle(
                    Notification.MediaStyle()
                        .setMediaSession(session.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )

            // Android 16+ status chip short critical text right near camera cutout
            if (Build.VERSION.SDK_INT >= 36) {
                runCatching {
                    val chipText = "${if (isPlaying) "▶" else "⏸"} $surahName"
                    val method = builder.javaClass.getMethod("setShortCriticalText", String::class.java)
                    method.invoke(builder, chipText.take(12))
                }
            }
            builder.build()
        } else {
            androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif_mihrab)
                .setColor(0xFF0F6B4E.toInt())
                .setLargeIcon(cachedArtwork)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(openAppIntent)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_media_prev, prevLabel, prevIntent)
                .addAction(
                    if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play,
                    toggleLabel,
                    toggleIntent
                )
                .addAction(R.drawable.ic_media_next, nextLabel, nextIntent)
                .addAction(R.drawable.ic_notif_mihrab, sheikhSwitchLabel, switchReciterIntent)
                .addAction(R.drawable.ic_media_stop, stopLabel, stopIntent)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildReciterLockScreenArtwork(
        reciterName: String,
        surahNameAr: String,
        surahSub: String
    ): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(0xFF062018.toInt(), 0xFF0D4433.toInt(), 0xFF165F48.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Subtle gold ornamental frame
        val goldStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x88E5BE5C.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(24f, 24f, size - 24f, size - 24f), 44f, 44f, goldStroke)

        // Pointed Mihrab arch silhouette
        val archPath = Path().apply {
            moveTo(148f, 340f)
            lineTo(148f, 190f)
            quadTo(148f, 118f, 256f, 68f)
            quadTo(364f, 118f, 364f, 190f)
            lineTo(364f, 340f)
            close()
        }
        val archFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x24D4AF37
            style = Paint.Style.FILL
        }
        canvas.drawPath(archPath, archFill)
        canvas.drawPath(archPath, goldStroke)

        // Glowing crescent inside arch
        val crescentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF4D47A.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(256f, 146f, 24f, crescentPaint)
        val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF114C3A.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(266f, 140f, 20f, cutPaint)

        // Surah Arabic title
        val surahPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFE59E.toInt()
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("سورة $surahNameAr", size / 2f, 236f, surahPaint)

        // Surah secondary label
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD4EFE5.toInt()
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(surahSub, size / 2f, 276f, subPaint)

        // Reciter Sheikh name wrapped cleanly near bottom
        val reciterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val layout = StaticLayout.Builder.obtain(reciterName, 0, reciterName.length, reciterPaint, size - 88)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .build()
        canvas.save()
        canvas.translate(44f, 368f)
        layout.draw(canvas)
        canvas.restore()

        return bmp
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createChannelIfNeeded(lang: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    lang.pick("تلاوة القرآن الكريم", "Quran Recitation", "Qur'on tilovati"),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = lang.pick(
                        "تشغيل التلاوة القرآنية في الخلفية وعلى شاشة القفل",
                        "Background & lock screen Quran recitation player",
                        "Fonda va qulflangan ekranda Qur'on tilovati"
                    )
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(false)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "mihrab_quran_audio_channel"
        const val NOTIFICATION_ID = 7011
        const val ACTION_START_OR_UPDATE = "com.example.action.AUDIO_START_UPDATE"
        const val ACTION_TOGGLE_PAUSE = "com.example.action.AUDIO_TOGGLE"
        const val ACTION_NEXT = "com.example.action.AUDIO_NEXT"
        const val ACTION_PREV = "com.example.action.AUDIO_PREV"
        const val ACTION_SWITCH_RECITER = "com.example.action.AUDIO_SWITCH_RECITER"
        const val ACTION_STOP = "com.example.action.AUDIO_STOP"
        const val CUSTOM_ACTION_SWITCH_RECITER = "mihrab_switch_reciter"
        const val CUSTOM_ACTION_CYCLE_SPEED = "mihrab_cycle_speed"
    }
}
