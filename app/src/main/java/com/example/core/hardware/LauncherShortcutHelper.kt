package com.example.core.hardware

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.core.i18n.AppLanguage

enum class LauncherShortcutTarget(
    val id: String,
    val launchTab: String,
    val shortAr: String,
    val shortEn: String,
    val shortUz: String,
    val longAr: String,
    val longEn: String,
    val longUz: String
) {
    PRAYER("mihrab_pin_prayer", "prayer", "الصلاة", "Prayer", "Namoz", "مواقيت الصلاة والقبلة", "Prayer & Qibla", "Namoz va Qibla"),
    QURAN("mihrab_pin_quran", "quran", "المصحف", "Mushaf", "Qur'on", "مصحف المدينة المنورة", "Madinah Mushaf", "Madina Mushafi"),
    TASBIH("mihrab_pin_tasbih", "dhikr", "المسبحة", "Tasbih", "Tasbeh", "المسبحة والأذكار", "Tasbih & Adhkar", "Tasbeh va Zikrlar"),
    AI("mihrab_pin_ai", "learn", "المساعد", "Islamic AI", "AI Ustoz", "المساعد الإسلامي الذكي", "Islamic AI Assistant", "Islomiy AI yordamchi");

    fun shortLabel(lang: AppLanguage): String = lang.pick(shortAr, shortEn, shortUz)
    fun longLabel(lang: AppLanguage): String = lang.pick(longAr, longEn, longUz)
}

object LauncherShortcutHelper {

    fun isPinSupported(context: Context): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    fun requestPinShortcut(
        context: Context,
        target: LauncherShortcutTarget,
        language: AppLanguage
    ): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("launch_tab", target.launchTab)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val shortcutInfo = ShortcutInfoCompat.Builder(context, target.id)
            .setShortLabel(target.shortLabel(language))
            .setLongLabel(target.longLabel(language))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(launchIntent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }
}
