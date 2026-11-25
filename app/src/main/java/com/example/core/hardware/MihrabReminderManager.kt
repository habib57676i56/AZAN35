package com.example.core.hardware

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.formatNumerals
import com.example.data.firebase.AdhanAlertMode
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.CalculationMethod
import com.example.domain.PrayerTimesEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.Calendar

data class MihrabReminderSettings(
    val prayerEntryAlertsEnabled: Boolean = true,
    val adhkarMorningEnabled: Boolean = true,
    val adhkarMorningHour: Int = 6,
    val adhkarMorningMinute: Int = 0,
    val adhkarEveningEnabled: Boolean = true,
    val adhkarEveningHour: Int = 17,
    val adhkarEveningMinute: Int = 0,
    val tahajjudReminderEnabled: Boolean = true,
    val tahajjudOffsetBeforeFajrMin: Int = 0 // 0 = Exact Last Third of Night, 45/60 = mins before Fajr
)

object MihrabReminderManager {
    private const val PREFS_NAME = "mihrab_reminders_prefs"

    private const val KEY_PRAYER_ALERTS = "prayer_entry_alerts"
    private const val KEY_ADHKAR_MORNING = "adhkar_morning_enabled"
    private const val KEY_ADHKAR_MORNING_H = "adhkar_morning_hour"
    private const val KEY_ADHKAR_MORNING_M = "adhkar_morning_minute"
    private const val KEY_ADHKAR_EVENING = "adhkar_evening_enabled"
    private const val KEY_ADHKAR_EVENING_H = "adhkar_evening_hour"
    private const val KEY_ADHKAR_EVENING_M = "adhkar_evening_minute"
    private const val KEY_TAHAJJUD_ENABLED = "tahajjud_reminder_enabled"
    private const val KEY_TAHAJJUD_OFFSET = "tahajjud_offset_before_fajr"

    // Cached snapshot for background rescheduling & boot recovery
    private const val KEY_LANG = "cached_lang"
    private const val KEY_LAT = "cached_lat"
    private const val KEY_LNG = "cached_lng"
    private const val KEY_METHOD = "cached_method"
    private const val KEY_TRAVELER = "cached_traveler"
    private const val KEY_INDIC = "cached_indic"
    private const val KEY_ADHAN_ENABLED = "cached_adhan_enabled"
    private const val KEY_ADHAN_VOICE = "cached_adhan_voice"
    private const val KEY_CUSTOM_ADHAN_PATH = "cached_custom_adhan_path"
    private const val KEY_PER_PRAYER_JSON = "cached_per_prayer_json"

    const val ACTION_PRAYER_ALARM = "com.example.action.PRAYER_ALARM"
    const val ACTION_ADHKAR_MORNING = "com.example.action.ADHKAR_MORNING"
    const val ACTION_ADHKAR_EVENING = "com.example.action.ADHKAR_EVENING"
    const val ACTION_TAHAJJUD_ALARM = "com.example.action.TAHAJJUD_ALARM"

    const val REMINDERS_CHANNEL_ID = "mihrab_spiritual_reminders"

    private const val REQ_FAJR = 4101
    private const val REQ_DHUHR = 4102
    private const val REQ_ASR = 4103
    private const val REQ_MAGHRIB = 4104
    private const val REQ_ISHA = 4105
    private const val REQ_ADHKAR_MORNING = 4201
    private const val REQ_ADHKAR_EVENING = 4202
    private const val REQ_TAHAJJUD = 4301

    private val _settingsFlow = MutableStateFlow<MihrabReminderSettings?>(null)
    val settingsFlow: StateFlow<MihrabReminderSettings?> = _settingsFlow.asStateFlow()

    fun getSettings(context: Context): MihrabReminderSettings {
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loaded = MihrabReminderSettings(
            prayerEntryAlertsEnabled = p.getBoolean(KEY_PRAYER_ALERTS, true),
            adhkarMorningEnabled = p.getBoolean(KEY_ADHKAR_MORNING, true),
            adhkarMorningHour = p.getInt(KEY_ADHKAR_MORNING_H, 6),
            adhkarMorningMinute = p.getInt(KEY_ADHKAR_MORNING_M, 0),
            adhkarEveningEnabled = p.getBoolean(KEY_ADHKAR_EVENING, true),
            adhkarEveningHour = p.getInt(KEY_ADHKAR_EVENING_H, 17),
            adhkarEveningMinute = p.getInt(KEY_ADHKAR_EVENING_M, 0),
            tahajjudReminderEnabled = p.getBoolean(KEY_TAHAJJUD_ENABLED, true),
            tahajjudOffsetBeforeFajrMin = p.getInt(KEY_TAHAJJUD_OFFSET, 0)
        )
        _settingsFlow.value = loaded
        return loaded
    }

    fun updateSettings(
        context: Context,
        updater: (MihrabReminderSettings) -> MihrabReminderSettings
    ) {
        val appCtx = context.applicationContext
        val updated = updater(getSettings(appCtx))
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PRAYER_ALERTS, updated.prayerEntryAlertsEnabled)
            .putBoolean(KEY_ADHKAR_MORNING, updated.adhkarMorningEnabled)
            .putInt(KEY_ADHKAR_MORNING_H, updated.adhkarMorningHour)
            .putInt(KEY_ADHKAR_MORNING_M, updated.adhkarMorningMinute)
            .putBoolean(KEY_ADHKAR_EVENING, updated.adhkarEveningEnabled)
            .putInt(KEY_ADHKAR_EVENING_H, updated.adhkarEveningHour)
            .putInt(KEY_ADHKAR_EVENING_M, updated.adhkarEveningMinute)
            .putBoolean(KEY_TAHAJJUD_ENABLED, updated.tahajjudReminderEnabled)
            .putInt(KEY_TAHAJJUD_OFFSET, updated.tahajjudOffsetBeforeFajrMin)
            .apply()
        _settingsFlow.value = updated
        rescheduleFromSavedPrefs(appCtx)
    }

    fun syncAndScheduleAll(
        context: Context,
        settings: UserSettingsDoc,
        offsets: Map<String, Int>,
        language: AppLanguage,
        useIndic: Boolean,
        customAdhanPath: String = ""
    ) {
        val appCtx = context.applicationContext
        val perPrayerJson = JSONObject(settings.perPrayerAdhanModes).toString()
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANG, language.code)
            .putFloat(KEY_LAT, settings.latitude.toFloat())
            .putFloat(KEY_LNG, settings.longitude.toFloat())
            .putString(KEY_METHOD, settings.calculationMethod)
            .putBoolean(KEY_TRAVELER, settings.travelerMode)
            .putBoolean(KEY_INDIC, useIndic)
            .putBoolean(KEY_ADHAN_ENABLED, settings.adhanEnabled)
            .putString(KEY_ADHAN_VOICE, settings.adhanVoiceId)
            .putString(KEY_CUSTOM_ADHAN_PATH, customAdhanPath)
            .putString(KEY_PER_PRAYER_JSON, perPrayerJson)
            .apply()

        scheduleInternal(
            context = appCtx,
            latitude = settings.latitude,
            longitude = settings.longitude,
            methodId = settings.calculationMethod,
            travelerMode = settings.travelerMode,
            offsets = offsets,
            language = language,
            useIndic = useIndic
        )
    }

    fun rescheduleFromSavedPrefs(context: Context) {
        val appCtx = context.applicationContext
        val p = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lang = AppLanguage.fromCode(p.getString(KEY_LANG, "ar") ?: "ar")
        val lat = p.getFloat(KEY_LAT, 21.4225f).toDouble()
        val lng = p.getFloat(KEY_LNG, 39.8262f).toDouble()
        val method = p.getString(KEY_METHOD, "MWL") ?: "MWL"
        val traveler = p.getBoolean(KEY_TRAVELER, false)
        val indic = p.getBoolean(KEY_INDIC, true)
        scheduleInternal(appCtx, lat, lng, method, traveler, emptyMap(), lang, indic)
    }

    private fun scheduleInternal(
        context: Context,
        latitude: Double,
        longitude: Double,
        methodId: String,
        travelerMode: Boolean,
        offsets: Map<String, Int>,
        language: AppLanguage,
        useIndic: Boolean
    ) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderCfg = getSettings(context)
        val now = Calendar.getInstance()
        val nowMs = now.timeInMillis

        val todaySchedule = PrayerTimesEngine.calculateForDate(
            calendar = now,
            latitude = latitude,
            longitude = longitude,
            method = CalculationMethod.fromId(methodId),
            travelerMode = travelerMode,
            manualOffsetsMin = offsets
        )
        val tomorrowCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val tomorrowSchedule = PrayerTimesEngine.calculateForDate(
            calendar = tomorrowCal,
            latitude = latitude,
            longitude = longitude,
            method = CalculationMethod.fromId(methodId),
            travelerMode = travelerMode,
            manualOffsetsMin = offsets
        )

        val indicUi = useIndic && language.isArabicUi

        // 1. 5 Daily Prayers Entry & Adhan Alarms
        val prayerSpecs = listOf(
            Triple("fajr", REQ_FAJR, todaySchedule.fajr to tomorrowSchedule.fajr),
            Triple("dhuhr", REQ_DHUHR, todaySchedule.dhuhr to tomorrowSchedule.dhuhr),
            Triple("asr", REQ_ASR, todaySchedule.asr to tomorrowSchedule.asr),
            Triple("maghrib", REQ_MAGHRIB, todaySchedule.maghrib to tomorrowSchedule.maghrib),
            Triple("isha", REQ_ISHA, todaySchedule.isha to tomorrowSchedule.isha)
        )

        prayerSpecs.forEach { (key, reqCode, pair) ->
            val (todayEntry, tomorrowEntry) = pair
            val pendingIntent = buildPrayerAlarmPendingIntent(
                context = context,
                reqCode = reqCode,
                prayerKey = key,
                prayerLabel = todayEntry.nameFor(language),
                prayerTime = formatNumerals(todayEntry.formattedTime, indicUi)
            )
            if (reminderCfg.prayerEntryAlertsEnabled) {
                var targetCal = calendarAtMinutes(now, todayEntry.timeMinutes)
                if (targetCal.timeInMillis <= nowMs + 5_000L) {
                    targetCal = calendarAtMinutes(tomorrowCal, tomorrowEntry.timeMinutes)
                }
                scheduleSafe(alarmMgr, targetCal.timeInMillis, pendingIntent)
            } else {
                alarmMgr.cancel(pendingIntent)
            }
        }

        // 2. Morning Adhkar Alarm
        val morningPending = buildSimpleAlarmPendingIntent(context, REQ_ADHKAR_MORNING, ACTION_ADHKAR_MORNING)
        if (reminderCfg.adhkarMorningEnabled) {
            val target = nextOccurrenceAt(now, reminderCfg.adhkarMorningHour, reminderCfg.adhkarMorningMinute)
            scheduleSafe(alarmMgr, target, morningPending)
        } else {
            alarmMgr.cancel(morningPending)
        }

        // 3. Evening Adhkar Alarm
        val eveningPending = buildSimpleAlarmPendingIntent(context, REQ_ADHKAR_EVENING, ACTION_ADHKAR_EVENING)
        if (reminderCfg.adhkarEveningEnabled) {
            val target = nextOccurrenceAt(now, reminderCfg.adhkarEveningHour, reminderCfg.adhkarEveningMinute)
            scheduleSafe(alarmMgr, target, eveningPending)
        } else {
            alarmMgr.cancel(eveningPending)
        }

        // 4. Tahajjud & Qiyam al-Layl Alarm
        val tahajjudPending = buildSimpleAlarmPendingIntent(context, REQ_TAHAJJUD, ACTION_TAHAJJUD_ALARM)
        if (reminderCfg.tahajjudReminderEnabled) {
            val todayTargetMin = computeTahajjudMinutes(todaySchedule.lastThirdQiyamMinutes, todaySchedule.fajr.timeMinutes, reminderCfg.tahajjudOffsetBeforeFajrMin)
            val tomorrowTargetMin = computeTahajjudMinutes(tomorrowSchedule.lastThirdQiyamMinutes, tomorrowSchedule.fajr.timeMinutes, reminderCfg.tahajjudOffsetBeforeFajrMin)
            var targetCal = calendarAtMinutes(now, todayTargetMin)
            if (targetCal.timeInMillis <= nowMs + 5_000L) {
                targetCal = calendarAtMinutes(tomorrowCal, tomorrowTargetMin)
            }
            scheduleSafe(alarmMgr, targetCal.timeInMillis, tahajjudPending)
        } else {
            alarmMgr.cancel(tahajjudPending)
        }
    }

    private fun computeTahajjudMinutes(lastThirdMin: Int, fajrMin: Int, offsetBeforeFajr: Int): Int {
        return if (offsetBeforeFajr <= 0) {
            lastThirdMin
        } else {
            (fajrMin - offsetBeforeFajr + 1440) % 1440
        }
    }

    private fun calendarAtMinutes(baseDay: Calendar, minutesFromMidnight: Int): Calendar {
        return (baseDay.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, (minutesFromMidnight / 60).coerceIn(0, 23))
            set(Calendar.MINUTE, (minutesFromMidnight % 60).coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun nextOccurrenceAt(now: Calendar, hour: Int, minute: Int): Long {
        val c = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now.timeInMillis + 5_000L) {
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.timeInMillis
    }

    private fun scheduleSafe(alarmMgr: AlarmManager, triggerAtMillis: Long, operation: PendingIntent) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } else {
                alarmMgr.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
        }
    }

    private fun buildPrayerAlarmPendingIntent(
        context: Context,
        reqCode: Int,
        prayerKey: String,
        prayerLabel: String,
        prayerTime: String
    ): PendingIntent {
        val intent = Intent(context, MihrabAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            putExtra(AdhanPlaybackService.EXTRA_PRAYER_KEY, prayerKey)
            putExtra(AdhanPlaybackService.EXTRA_PRAYER_LABEL, prayerLabel)
            putExtra(AdhanPlaybackService.EXTRA_PRAYER_TIME, prayerTime)
        }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSimpleAlarmPendingIntent(context: Context, reqCode: Int, action: String): PendingIntent {
        val intent = Intent(context, MihrabAlarmReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun handleFiredIntent(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lang = AppLanguage.fromCode(prefs.getString(KEY_LANG, "ar") ?: "ar")

        when (intent.action) {
            ACTION_PRAYER_ALARM -> {
                val prayerKey = intent.getStringExtra(AdhanPlaybackService.EXTRA_PRAYER_KEY) ?: "prayer"
                val prayerLabel = intent.getStringExtra(AdhanPlaybackService.EXTRA_PRAYER_LABEL) ?: "الصلاة"
                val prayerTime = intent.getStringExtra(AdhanPlaybackService.EXTRA_PRAYER_TIME) ?: ""

                val adhanEnabled = prefs.getBoolean(KEY_ADHAN_ENABLED, true)
                val voiceId = prefs.getString(KEY_ADHAN_VOICE, "makkah") ?: "makkah"
                val customPath = prefs.getString(KEY_CUSTOM_ADHAN_PATH, "").orEmpty()
                val modesMapJson = prefs.getString(KEY_PER_PRAYER_JSON, "{}").orEmpty()
                val modeId = runCatching {
                    JSONObject(modesMapJson).optString(prayerKey, AdhanAlertMode.FULL_ADHAN.id)
                }.getOrDefault(AdhanAlertMode.FULL_ADHAN.id)
                val alertMode = if (!adhanEnabled) AdhanAlertMode.SILENT else AdhanAlertMode.fromId(modeId)

                // Always post prayer entry notification or launch Adhan foreground service with Lock Screen / Volume button stop!
                if (alertMode == AdhanAlertMode.SILENT) {
                    postReminderNotification(
                        context = appCtx,
                        notifId = 7101,
                        title = lang.pick("حان الآن وقت صلاة $prayerLabel", "Time for $prayerLabel Prayer", "$prayerLabel namozi vaqti kirdi"),
                        body = lang.pick("الموعد: $prayerTime • حيّ على الصلاة", "Time: $prayerTime • Stand for Salah", "Vaqt: $prayerTime • Namozga shoshiling"),
                        launchTab = "prayer"
                    )
                } else {
                    AdhanRuntimeController.triggerAdhan(
                        context = appCtx,
                        prayerKey = prayerKey,
                        prayerLabel = prayerLabel,
                        prayerTime = prayerTime,
                        voiceId = voiceId,
                        mode = alertMode,
                        customLocalPath = customPath,
                        language = lang
                    )
                }
                PrayerStatusNotificationManager.refreshFromSavedPrefs(appCtx)
                rescheduleFromSavedPrefs(appCtx)
            }
            ACTION_ADHKAR_MORNING -> {
                sendAdhkarReminderNow(appCtx, isMorning = true, language = lang)
                rescheduleFromSavedPrefs(appCtx)
            }
            ACTION_ADHKAR_EVENING -> {
                sendAdhkarReminderNow(appCtx, isMorning = false, language = lang)
                rescheduleFromSavedPrefs(appCtx)
            }
            ACTION_TAHAJJUD_ALARM -> {
                sendTahajjudReminderNow(appCtx, language = lang)
                rescheduleFromSavedPrefs(appCtx)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                rescheduleFromSavedPrefs(appCtx)
                PrayerStatusNotificationManager.refreshFromSavedPrefs(appCtx)
            }
        }
    }

    fun sendAdhkarReminderNow(context: Context, isMorning: Boolean, language: AppLanguage) {
        val title = if (isMorning) {
            language.pick("أذكار الصباح • حصنُك ليومك", "Morning Adhkar Reminder", "Tonggi zikrlar eslatmasi")
        } else {
            language.pick("أذكار المساء • حصنُك لليلك", "Evening Adhkar Reminder", "Kechki zikrlar eslatmasi")
        }
        val body = if (isMorning) {
            language.pick(
                "«أصبحنا وأصبح الملك لله» — اضغط لقراءة أذكار الصباح وحفظ وردك اليومي.",
                "Start your morning with remembrance of Allah — tap to read Morning Adhkar.",
                "Kuningizni Allohning zikri bilan boshlang — tonggi zikrlarni oching."
            )
        } else {
            language.pick(
                "«أمسينا وأمسى الملك لله» — اضغط لقراءة أذكار المساء.",
                "Protect your evening with remembrance of Allah — tap to read Evening Adhkar.",
                "Kechangizni zikr bilan fayzli qiling — kechki zikrlarni oching."
            )
        }
        postReminderNotification(
            context = context,
            notifId = if (isMorning) 7201 else 7202,
            title = title,
            body = body,
            launchTab = "dhikr"
        )
    }

    fun sendTahajjudReminderNow(context: Context, language: AppLanguage) {
        postReminderNotification(
            context = context,
            notifId = 7301,
            title = language.pick(
                "الثلث الأخير من الليل • صلاة التهجد والقيام",
                "Last Third of the Night • Tahajjud & Qiyam",
                "Kechaning oxirgi uchdan biri • Tahajjud vaqti"
            ),
            body = language.pick(
                "ينزل ربنا إلى السماء الدنيا: هل من داعٍ فأستجيب له؟ قم وصلِّ ركعتين وادعُ الله.",
                "Stand for Qiyam al-Layl & sincere Dua — the gates of mercy and acceptance are open.",
                "Tahajjud namozi va ijobat duolari uchun eng muborak vaqt kirdi."
            ),
            launchTab = "prayer"
        )
    }

    private fun postReminderNotification(
        context: Context,
        notifId: Int,
        title: String,
        body: String,
        launchTab: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureRemindersChannel(context)
        val openPending = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                putExtra("launch_tab", launchTab)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_mihrab)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        mgr.notify(notifId, notif)
    }

    private fun ensureRemindersChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                REMINDERS_CHANNEL_ID,
                "تذكيرات الأذكار والتهجد والصلوات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دخول وقت الصلاة وتذكير أذكار الصباح والمساء وصلاة التهجد وقيام الليل"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            mgr.createNotificationChannel(channel)
        }
    }
}
