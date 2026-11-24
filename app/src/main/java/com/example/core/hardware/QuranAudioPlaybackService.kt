package com.example.core.hardware

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.QuranCatalog

class QuranAudioPlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PAUSE -> {
                RecitationPlayerHolder.instance?.togglePlayPause()
                updateNotification()
            }
            ACTION_NEXT -> RecitationPlayerHolder.instance?.nextAyah()
            ACTION_PREV -> RecitationPlayerHolder.instance?.previousAyah()
            ACTION_STOP -> {
                RecitationPlayerHolder.instance?.stopFromService()
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

    private fun updateNotification() {
        val lang = PrayerStatusNotificationManager.savedLanguage(this)
        createChannelIfNeeded(lang)
        val state = RecitationPlayerHolder.instance?.state?.value
        val surahMeta = QuranCatalog.surahByNumber(state?.currentSurah ?: 1)
        val ayahNum = state?.currentAyah ?: 1
        val surahName = if (lang.isArabicUi) surahMeta.nameAr else surahMeta.nameEn
        val title = lang.pick(
            "$surahName • آية $ayahNum",
            "$surahName • Ayah $ayahNum",
            "$surahName • $ayahNum-oyat"
        )
        val subtitle = if (lang.isArabicUi) {
            state?.reciter?.nameAr ?: "مشاري راشد العفاسي"
        } else {
            state?.reciter?.nameEn ?: "Mishary Rashid Alafasy"
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

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_mihrab)
            .setColor(0xFF0F6B4E.toInt())
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(openAppIntent)
            .setOngoing(state?.isPlaying == true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, lang.pick("السابق", "Prev", "Oldingi"), prevIntent)
            .addAction(
                0,
                if (state?.isPlaying == true) lang.pick("إيقاف مؤقت", "Pause", "Pauza")
                else lang.pick("تشغيل", "Play", "Ijro"),
                toggleIntent
            )
            .addAction(0, lang.pick("التالي", "Next", "Keyingi"), nextIntent)
            .addAction(0, lang.pick("إيقاف", "Stop", "To'xtat"), stopIntent)
            .build()

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

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createChannelIfNeeded(lang: com.example.core.i18n.AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    lang.pick("تلاوة القرآن الكريم", "Quran Recitation", "Qur'on tilovati"),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = lang.pick(
                        "تشغيل التلاوة القرآنية في الخلفية",
                        "Background Quran recitation playback",
                        "Qur'on tilovatini fonda ijro etish"
                    )
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
        const val ACTION_STOP = "com.example.action.AUDIO_STOP"
    }
}
