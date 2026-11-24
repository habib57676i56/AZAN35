package com.example.core.preferences

import android.content.Context
import com.example.core.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SmartFeatureId(
    val key: String,
    val labelAr: String,
    val labelEn: String,
    val labelUz: String,
    val defaultWeight: Int
) {
    MUSHAF("mushaf", "مصحف المدينة", "Madinah Mushaf", "Madina Mushafi", 6),
    TASBIH("tasbih", "المسبحة الرقمية", "Digital Tasbih", "Raqamli Tasbeh", 5),
    ADHKAR("adhkar", "موسوعة الأذكار", "Adhkar", "Zikrlar", 4),
    AI_COMPANION("ai_companion", "المساعد الذكي", "Islamic AI", "Islomiy AI", 4),
    QIBLA("qibla", "بوصلة القبلة", "Qibla AR", "Qibla AR", 3),
    MOSQUES("mosques", "المساجد القريبة", "Nearby Mosques", "Yaqin masjidlar", 3),
    KHATMAH("khatmah", "خطة الختمة", "Khatmah Plan", "Xatma rejasi", 2),
    ZAKAT("zakat", "حاسبة الزكاة", "Zakat Calc", "Zakot hisobi", 2),
    HIJRI("hijri", "التقويم الهجري", "Hijri Calendar", "Hijriy taqvim", 1),
    RAKAH("rakah", "مرشد الركعات", "Rakah Guide", "Rakat ko'rsatkich", 1);

    fun labelFor(language: AppLanguage): String = language.pick(labelAr, labelEn, labelUz)
}

data class RankedSmartFeature(
    val feature: SmartFeatureId,
    val count: Int
)

object FeatureUsageTracker {
    private const val PREFS_NAME = "mihrab_smart_shortcuts"
    private const val KEY_PREFIX = "usage_"

    private val _countsVersion = MutableStateFlow(0)
    val countsVersion: StateFlow<Int> = _countsVersion.asStateFlow()

    fun recordUsage(context: Context, feature: SmartFeatureId) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cur = prefs.getInt("$KEY_PREFIX${feature.key}", 0)
        prefs.edit().putInt("$KEY_PREFIX${feature.key}", cur + 1).apply()
        _countsVersion.value += 1
    }

    fun getTopFeatures(context: Context, limit: Int = 5): List<RankedSmartFeature> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return SmartFeatureId.entries
            .map { feature ->
                val hits = prefs.getInt("$KEY_PREFIX${feature.key}", 0)
                RankedSmartFeature(feature = feature, count = hits)
            }
            .sortedWith(
                compareByDescending<RankedSmartFeature> { it.count }
                    .thenByDescending { it.feature.defaultWeight }
            )
            .take(limit)
    }

    fun resetUsage(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
        _countsVersion.value += 1
    }
}
