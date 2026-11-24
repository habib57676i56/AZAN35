package com.example.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.data.firebase.AdhanAlertMode
import com.example.domain.AdhkarCategoryMeta

/**
 * Purpose-built Islamic & Material Symbols icon system for Mihrab across
 * Navigation, Prayer Times, Adhan modes, Madinah Mushaf tools, 12 Adhkar categories,
 * AI Assistant, and Settings.
 */
object MihrabIcons {

    // Custom vector: Islamic pointed Mihrab arch & hanging Qandil lantern silhouette
    val MihrabArch: ImageVector by lazy {
        ImageVector.Builder(
            name = "MihrabArch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 21f)
                lineTo(4f, 11f)
                curveTo(4f, 6.8f, 7.8f, 3.8f, 12f, 2.2f)
                curveTo(16.2f, 3.8f, 20f, 6.8f, 20f, 11f)
                lineTo(20f, 21f)
                close()
                // Inner arch & lantern
                moveTo(7.5f, 21f)
                lineTo(7.5f, 12f)
                curveTo(7.5f, 9.2f, 9.6f, 7.1f, 12f, 6f)
                curveTo(14.4f, 7.1f, 16.5f, 9.2f, 16.5f, 12f)
                lineTo(16.5f, 21f)
                moveTo(12f, 6f)
                lineTo(12f, 10.2f)
                moveTo(10.5f, 12f)
                lineTo(13.5f, 12f)
                lineTo(12.8f, 14.5f)
                lineTo(11.2f, 14.5f)
                close()
            }
        }.build()
    }

    // Custom vector: Islamic 33-bead Tasbih loop
    val TasbihBeads: ImageVector by lazy {
        ImageVector.Builder(
            name = "TasbihBeads",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 4f)
                curveTo(16.5f, 4f, 19.5f, 7.2f, 19.5f, 11.2f)
                curveTo(19.5f, 15.2f, 16f, 18.2f, 12f, 18.2f)
                curveTo(8f, 18.2f, 4.5f, 15.2f, 4.5f, 11.2f)
                curveTo(4.5f, 7.2f, 7.5f, 4f, 12f, 4f)
                close()
                moveTo(12f, 18.2f)
                lineTo(12f, 22f)
                moveTo(10.2f, 20.5f)
                lineTo(13.8f, 20.5f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2.5f)
                arcTo(1.5f, 1.5f, 0f, true, true, 11.9f, 2.5f)
                close()
            }
        }.build()
    }

    // Custom vector: Kaaba orientation cube icon
    val KaabaCube: ImageVector by lazy {
        ImageVector.Builder(
            name = "KaabaCube",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                lineTo(20f, 6.8f)
                lineTo(20f, 17.2f)
                lineTo(12f, 21.5f)
                lineTo(4f, 17.2f)
                lineTo(4f, 6.8f)
                close()
                moveTo(4f, 6.8f)
                lineTo(12f, 11f)
                lineTo(20f, 6.8f)
                moveTo(12f, 11f)
                lineTo(12f, 21.5f)
                // Kiswah band
                moveTo(4f, 10f)
                lineTo(12f, 14.2f)
                lineTo(20f, 10f)
            }
        }.build()
    }

    // Bottom Navigation / Rail semantic pairs (Filled, Outlined)
    fun navIcons(tabIndex: Int): Pair<ImageVector, ImageVector> = when (tabIndex) {
        0 -> Icons.Rounded.Mosque to Icons.Outlined.Mosque
        1 -> Icons.Rounded.AutoStories to Icons.Outlined.AutoStories
        2 -> Icons.Rounded.Favorite to Icons.Outlined.FavoriteBorder
        3 -> Icons.Rounded.Psychology to Icons.Outlined.Psychology
        else -> Icons.Rounded.Handyman to Icons.Outlined.Handyman
    }

    // All 12 Adhkar Encyclopedia Categories
    fun iconForAdhkarCategory(categoryId: String): ImageVector =
        when (AdhkarCategoryMeta.fromId(categoryId)) {
            AdhkarCategoryMeta.MORNING -> Icons.Rounded.WbSunny
            AdhkarCategoryMeta.EVENING -> Icons.Rounded.NightsStay
            AdhkarCategoryMeta.AFTER_PRAYER -> Icons.Rounded.CheckCircle
            AdhkarCategoryMeta.SLEEP -> Icons.Rounded.Bedtime
            AdhkarCategoryMeta.WAKING_UP -> Icons.Rounded.Alarm
            AdhkarCategoryMeta.MOSQUE -> Icons.Rounded.Mosque
            AdhkarCategoryMeta.TRAVEL -> Icons.Rounded.Flight
            AdhkarCategoryMeta.HOME_FOOD -> Icons.Rounded.Home
            AdhkarCategoryMeta.RUQYAH -> Icons.Rounded.HealthAndSafety
            AdhkarCategoryMeta.QURANIC_DUAS -> Icons.Rounded.AutoStories
            AdhkarCategoryMeta.PROPHETIC_DUAS -> Icons.Rounded.Verified
            AdhkarCategoryMeta.GENERAL_DUAS -> Icons.Rounded.VolunteerActivism
        }

    // Prayer time icons (Fajr -> Isha)
    fun iconForPrayer(prayerKey: String): ImageVector = when (prayerKey.lowercase()) {
        "fajr" -> Icons.Rounded.WbTwilight
        "dhuhr" -> Icons.Rounded.WbSunny
        "asr" -> Icons.Rounded.Grain
        "maghrib" -> Icons.Rounded.NightsStay
        "isha" -> Icons.Rounded.Bedtime
        else -> Icons.Rounded.Mosque
    }

    // Per-prayer Adhan alert mode icons
    fun iconForAdhanMode(mode: AdhanAlertMode): ImageVector = when (mode) {
        AdhanAlertMode.FULL_ADHAN -> Icons.Rounded.RecordVoiceOver
        AdhanAlertMode.TAKBIR_ONLY -> Icons.Rounded.NotificationsActive
        AdhanAlertMode.GENTLE_CHIME -> Icons.Rounded.MusicNote
        AdhanAlertMode.SILENT -> Icons.Rounded.VolumeOff
    }

    // Semantic Icons for Quick Actions, Mushaf, AI & Settings
    val Qibla: ImageVector = Icons.Rounded.Explore
    val MethodTune: ImageVector = Icons.Rounded.Tune
    val Guides: ImageVector = Icons.AutoMirrored.Rounded.MenuBook
    val RakahTimer: ImageVector = Icons.Rounded.Timer
    val KhushuStar: ImageVector = Icons.Rounded.Stars
    val AdhanVoice: ImageVector = Icons.Rounded.VolumeUp
    val MushafIndex: ImageVector = Icons.AutoMirrored.Rounded.List
    val Search: ImageVector = Icons.Rounded.Search
    val Bookmark: ImageVector = Icons.Rounded.Bookmark
    val Note: ImageVector = Icons.Rounded.EditNote
    val AiSpark: ImageVector = Icons.Rounded.AutoAwesome
    val AiQuiz: ImageVector = Icons.Rounded.Quiz
    val Language: ImageVector = Icons.Rounded.Language
    val ThemePalette: ImageVector = Icons.Rounded.Palette
    val Calculators: ImageVector = Icons.Rounded.Calculate
    val Privacy: ImageVector = Icons.Rounded.PrivacyTip
    val Settings: ImageVector = Icons.Rounded.Settings
    val ScholarBook: ImageVector = Icons.Rounded.MenuBook
}
