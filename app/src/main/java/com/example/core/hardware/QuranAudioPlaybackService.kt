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
            ACTION_STOP -> {
                RecitationPlayerHolder.instance?.stopFromService()
                stopForegroundCompat()
                stopSelf()
            }
            else -> updateNotification()
        }
        return START_NOT_STICKY
    }

    private fun updateNotification() {
        createChannelIfNeeded()
        val state = RecitationPlayerHolder.instance?.state?.value
        val surahMeta = QuranCatalog.surahByNumber(state?.currentSurah ?: 1)
        val title = "${surahMeta.nameAr} • آية ${state?.currentAyah ?: 1}"
        val subtitle = state?.reciter?.nameAr ?: "مشاري راشد العفاسي"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("launch_tab", "quran")
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_TOGGLE_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(openAppIntent)
            .setOngoing(state?.isPlaying == true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                if (state?.isPlaying == true) "إيقاف مؤقت" else "تشغيل",
                toggleIntent
            )
            .addAction(0, "إيقاف", stopIntent)
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

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تلاوة القرآن الكريم",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "تشغيل التلاوة القرآنية في الخلفية"
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
        const val ACTION_STOP = "com.example.action.AUDIO_STOP"
    }
}
