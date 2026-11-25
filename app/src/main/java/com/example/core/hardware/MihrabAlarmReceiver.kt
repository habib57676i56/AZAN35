package com.example.core.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MihrabAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        MihrabReminderManager.handleFiredIntent(context, intent)
    }
}
