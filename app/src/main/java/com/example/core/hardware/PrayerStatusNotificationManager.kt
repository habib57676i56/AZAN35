package com.example.core.hardware

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.formatNumerals
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.CalculationMethod
import com.example.domain.HijriCalendarEngine
import com.example.domain.PrayerTimesEngine
import java.util.Calendar

object PrayerStatusNotificationManager {
    private const val PREFS_NAME = "mihrab_prayer_shade"
    private const val KEY_ENABLED = "persistent_bar_enabled"
    private const val KEY_LANG = "lang"
    private const val KEY_CITY = "city"
    private const val KEY_LAT = "lat"
    private const val KEY_LNG = "lng"
    private const val KEY_METHOD = "method"
    private const val KEY_TRAVELER = "traveler"
    private const val KEY_INDIC = "indic"

    const val CHANNEL_ID = "mihrab_persistent_prayer_times"
    const val NOTIFICATION_ID = 7022

    fun isEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(
        context: Context,
        enabled: Boolean,
        settings: UserSettingsDoc,
        offsets: Map<String, Int>,
        language: AppLanguage,
        useIndic: Boolean,
        appOperations: AppOperationsConfig = AppOperationsConfig()
    ) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        syncAndSchedule(context, settings, offsets, language, useIndic, appOperations)
    }

    fun savedLanguage(context: Context): AppLanguage {
        val code = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "ar") ?: "ar"
        return AppLanguage.fromCode(code)
    }

    fun syncAndSchedule(
        context: Context,
        settings: UserSettingsDoc,
        offsets: Map<String, Int>,
        language: AppLanguage,
        useIndic: Boolean,
        appOperations: AppOperationsConfig = AppOperationsConfig()
    ) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LANG, language.code)
            .putString(KEY_CITY, settings.city)
            .putFloat(KEY_LAT, settings.latitude.toFloat())
            .putFloat(KEY_LNG, settings.longitude.toFloat())
            .putString(KEY_METHOD, settings.calculationMethod)
            .putBoolean(KEY_TRAVELER, settings.travelerMode)
            .putBoolean(KEY_INDIC, useIndic)
            .apply()

        val enabled = prefs.getBoolean(KEY_ENABLED, false) && appOperations.persistentPrayerNotificationAllowed
        if (!enabled) {
            cancelNotificationAndAlarm(appCtx)
            return
        }
        showOrUpdateNow(
            appCtx,
            settings.latitude,
            settings.longitude,
            settings.city,
            settings.calculationMethod,
            settings.travelerMode,
            offsets,
            language,
            useIndic
        )
        scheduleNextRefresh(appCtx)
    }

    fun refreshFromSavedPrefs(context: Context) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            cancelNotificationAndAlarm(appCtx)
            return
        }
        val lang = AppLanguage.fromCode(prefs.getString(KEY_LANG, "ar") ?: "ar")
        val city = prefs.getString(KEY_CITY, "Makkah") ?: "Makkah"
        val lat = prefs.getFloat(KEY_LAT, 21.4225f).toDouble()
        val lng = prefs.getFloat(KEY_LNG, 39.8262f).toDouble()
        val method = prefs.getString(KEY_METHOD, "MWL") ?: "MWL"
        val traveler = prefs.getBoolean(KEY_TRAVELER, false)
        val indic = prefs.getBoolean(KEY_INDIC, lang.isArabicUi)
        showOrUpdateNow(appCtx, lat, lng, city, method, traveler, emptyMap(), lang, indic)
        scheduleNextRefresh(appCtx)
    }

    private fun showOrUpdateNow(
        context: Context,
        latitude: Double,
        longitude: Double,
        city: String,
        methodId: String,
        travelerMode: Boolean,
        offsets: Map<String, Int>,
        language: AppLanguage,
        useIndic: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context, language)
        val now = Calendar.getInstance()
        val schedule = PrayerTimesEngine.calculateForDate(
            calendar = now,
            latitude = latitude,
            longitude = longitude,
            method = CalculationMethod.fromId(methodId),
            travelerMode = travelerMode,
            manualOffsetsMin = offsets
        )
        val hijri = HijriCalendarEngine.gregorianToHijri(now)
        val indicUi = useIndic && language.isArabicUi
        val hijriText = formatNumerals(hijri.formattedFor(language), indicUi)

        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val five = listOf(schedule.fajr, schedule.dhuhr, schedule.asr, schedule.maghrib, schedule.isha)
        val nextIndex = five.indexOfFirst { it.timeMinutes > nowMin }.let { if (it >= 0) it else 0 }
        val next = five[nextIndex]
        val diffMin = ((next.timeMinutes - nowMin) + 1440) % 1440
        val h = diffMin / 60
        val m = diffMin % 60
        val remainingStr = formatNumerals(
            when {
                h > 0 && m > 0 -> language.pick("${h}س ${m}د", "${h}h ${m}m", "${h}s ${m}d")
                h > 0 -> language.pick("${h}س", "${h}h", "${h}s")
                else -> language.pick("${m}د", "${m}m", "${m}d")
            },
            indicUi
        )
        val countdownPill = language.pick("بعد $remainingStr", "in $remainingStr", "$remainingStr qoldi")
        val nextTimeStr = formatNumerals(next.formattedTime, indicUi)
        val fallbackTitle = language.pick(
            "${next.nameAr}: $nextTimeStr ($countdownPill) • $city",
            "Next: ${next.nameEn} $nextTimeStr ($countdownPill) • $city",
            "Keyingi: ${next.nameUz} $nextTimeStr ($countdownPill) • $city"
        )
        val summaryCompact = five.joinToString(" • ") { entry ->
            val shortName = entry.nameFor(language)
            val t = formatNumerals(entry.formattedTime.replace(" AM", "").replace(" PM", ""), indicUi)
            "$shortName $t"
        }

        val openIntent = PendingIntent.getActivity(
            context,
            21,
            Intent(context, MainActivity::class.java).apply {
                putExtra("launch_tab", "prayer")
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val refreshIntent = PendingIntent.getBroadcast(
            context,
            7024,
            Intent(context, PrayerStatusRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val compactView = RemoteViews(context.packageName, R.layout.notification_prayer_compact).apply {
            setTextViewText(R.id.tv_compact_title, "${next.nameFor(language)}  •  $nextTimeStr")
            setTextViewText(R.id.tv_compact_subtitle, "$city  •  $hijriText")
            setTextViewText(R.id.tv_compact_countdown, countdownPill)
        }

        val tileIds = intArrayOf(R.id.tile_p1, R.id.tile_p2, R.id.tile_p3, R.id.tile_p4, R.id.tile_p5)
        val badgeIds = intArrayOf(R.id.badge_p1, R.id.badge_p2, R.id.badge_p3, R.id.badge_p4, R.id.badge_p5)
        val nameIds = intArrayOf(R.id.name_p1, R.id.name_p2, R.id.name_p3, R.id.name_p4, R.id.name_p5)
        val timeIds = intArrayOf(R.id.time_p1, R.id.time_p2, R.id.time_p3, R.id.time_p4, R.id.time_p5)

        val expandedView = RemoteViews(context.packageName, R.layout.notification_prayer_expanded).apply {
            setTextViewText(R.id.tv_exp_meta, "$city  •  $hijriText")
            setTextViewText(
                R.id.tv_exp_next_title,
                language.pick(
                    "القادمة: ${next.nameAr}  $nextTimeStr",
                    "Next: ${next.nameEn}  $nextTimeStr",
                    "Keyingi: ${next.nameUz}  $nextTimeStr"
                )
            )
            setTextViewText(R.id.tv_exp_countdown, countdownPill)
            setOnClickPendingIntent(R.id.btn_exp_refresh, refreshIntent)

            five.forEachIndexed { idx, entry ->
                val isNext = idx == nextIndex
                val shortTime = formatNumerals(
                    entry.formattedTime.replace(" AM", "").replace(" PM", ""),
                    indicUi
                )
                setTextViewText(nameIds[idx], entry.nameFor(language))
                setTextViewText(timeIds[idx], shortTime)
                setInt(
                    tileIds[idx],
                    "setBackgroundResource",
                    if (isNext) R.drawable.bg_notif_prayer_tile_active else R.drawable.bg_notif_prayer_tile
                )
                if (isNext) {
                    setViewVisibility(badgeIds[idx], View.VISIBLE)
                    setTextViewText(badgeIds[idx], language.pick("القادمة", "NEXT", "KEYINGI"))
                    setTextColor(nameIds[idx], 0xFFFFFFFF.toInt())
                    setTextColor(timeIds[idx], 0xFFFFE082.toInt())
                } else {
                    setViewVisibility(badgeIds[idx], View.GONE)
                    setTextColor(nameIds[idx], 0xFFD2EAE1.toInt())
                    setTextColor(timeIds[idx], 0xFFFFFFFF.toInt())
                }
            }

            setTextViewText(
                R.id.tv_exp_sunrise,
                "☀ ${schedule.sunrise.nameFor(language)} ${formatNumerals(schedule.sunrise.formattedTime, indicUi)}"
            )
            setTextViewText(
                R.id.tv_exp_qiyam,
                "☾ ${language.pick("الثلث الأخير", "Qiyam", "Tahajjud")} ${formatNumerals(schedule.lastThirdQiyam, indicUi)}"
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_mihrab)
            .setColor(0xFF0F6B4E.toInt())
            .setContentTitle(fallbackTitle)
            .setContentText(summaryCompact)
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr?.notify(NOTIFICATION_ID, notification)
    }

    private fun scheduleNextRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            7023,
            Intent(context, PrayerStatusRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + 2 * 60 * 1000L
        runCatching {
            alarmManager.setWindow(
                AlarmManager.RTC,
                triggerAt,
                45 * 1000L,
                pi
            )
        }
    }

    private fun cancelNotificationAndAlarm(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr?.cancel(NOTIFICATION_ID)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            7023,
            Intent(context, PrayerStatusRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    private fun createChannel(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            val existing = mgr.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    language.pick("مواقيت الصلاة الدائمة", "Permanent Prayer Times", "Doimiy namoz vaqtlari"),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = language.pick(
                        "عرض مواقيت الصلوات الخمس باستمرار في لوحة الإشعارات",
                        "Always-on 5 daily prayer times in control shade",
                        "Bildirishnoma pardasida 5 vaqt namozni doimiy ko'rsatish"
                    )
                    setShowBadge(false)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }
}
