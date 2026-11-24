package com.example.core.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerStatusRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        PrayerStatusNotificationManager.refreshFromSavedPrefs(context)
    }
}
