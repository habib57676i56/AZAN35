package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val id: String) {
    LIGHT("LIGHT"),
    DARK("DARK"),
    SYSTEM("SYSTEM");

    companion object {
        fun fromId(id: String): AppThemeMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

enum class MushafPaperTheme(
    val id: String,
    val bg: Color,
    val cardInner: Color,
    val fg: Color,
    val accent: Color,
    val border: Color
) {
    WHITE(
        id = "WHITE",
        bg = Color(0xFFFCFBF7),
        cardInner = Color(0xFFF6F2E7),
        fg = Color(0xFF151E19),
        accent = Color(0xFF0C3B2E),
        border = Color(0xFFC89B3C)
    ),
    SEPIA(
        id = "SEPIA",
        bg = Color(0xFFF3E7CE),
        cardInner = Color(0xFFEBDCB9),
        fg = Color(0xFF2D2111),
        accent = Color(0xFF6B4B1B),
        border = Color(0xFFB58634)
    ),
    DARK(
        id = "DARK",
        bg = Color(0xFF0C1512),
        cardInner = Color(0xFF12211C),
        fg = Color(0xFFEDE5D3),
        accent = Color(0xFF7AD3AF),
        border = Color(0xFFE2B95B)
    );

    companion object {
        fun fromId(id: String): MushafPaperTheme =
            entries.firstOrNull { it.id == id } ?: WHITE
    }
}

enum class MihrabThemePreset(val label: String) {
    EMERALD_SANCTUARY("Emerald Sanctuary"),
    MOSQUE_BLUE("Mosque Blue"),
    WARM_PARCHMENT("Warm Parchment"),
    MIDNIGHT_QIYAM("Midnight Qiyam")
}

private val EmeraldLightScheme = lightColorScheme(
    primary = SanctuaryEmerald,
    onPrimary = Color.White,
    primaryContainer = SoftSage,
    onPrimaryContainer = SanctuaryEmerald,
    secondary = BurnishedGold,
    onSecondary = Color.White,
    secondaryContainer = PaleGoldWash,
    onSecondaryContainer = Color(0xFF4A3709),
    tertiary = LuminousJade,
    onTertiary = Color.White,
    background = ParchmentAlabaster,
    onBackground = CrispInk,
    surface = ParchmentAlabaster,
    onSurface = CrispInk,
    surfaceVariant = ParchmentCream,
    onSurfaceVariant = MutedOliveInk,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = ParchmentCream,
    surfaceContainer = ParchmentElevated,
    outline = Color(0xFFCBD4CE),
    error = AlertCoral,
    errorContainer = SoftCoralContainer
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = Color(0xFF7AD3AF),
    onPrimary = Color(0xFF04271D),
    primaryContainer = Color(0xFF144B3B),
    onPrimaryContainer = SoftSage,
    secondary = WarmSaffron,
    onSecondary = Color(0xFF332300),
    secondaryContainer = Color(0xFF4F3B0F),
    onSecondaryContainer = PaleGoldWash,
    tertiary = Color(0xFF9DD9C0),
    onTertiary = Color(0xFF04271D),
    background = MidnightEmerald,
    onBackground = CandlelightIvory,
    surface = MidnightEmerald,
    onSurface = CandlelightIvory,
    surfaceVariant = DeepSlateEmerald,
    onSurfaceVariant = SoftMoonstone,
    surfaceContainerLowest = Color(0xFF070E0C),
    surfaceContainerLow = DeepSlateEmerald,
    surfaceContainer = ElevatedNightEmerald,
    outline = Color(0xFF30443C),
    error = DarkAlertCoral,
    errorContainer = Color(0xFF5D2118)
)

private val MosqueBlueLightScheme = EmeraldLightScheme.copy(
    primary = MosqueBluePrimary,
    primaryContainer = Color(0xFFD6E6F5),
    onPrimaryContainer = MosqueBluePrimary,
    tertiary = MosqueBlueSecondary
)

private val MosqueBlueDarkScheme = EmeraldDarkScheme.copy(
    primary = MosqueBlueDarkPrimary,
    primaryContainer = Color(0xFF164368),
    background = Color(0xFF09131D),
    surface = Color(0xFF09131D),
    surfaceVariant = Color(0xFF102132),
    surfaceContainerLow = Color(0xFF102132),
    surfaceContainer = Color(0xFF172D42)
)

private val SepiaLightScheme = EmeraldLightScheme.copy(
    background = WarmSepiaBackground,
    surface = WarmSepiaBackground,
    surfaceVariant = WarmSepiaSurface,
    surfaceContainerLow = WarmSepiaSurface,
    surfaceContainer = Color(0xFFE3D3B3)
)

val MihrabShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MihrabTheme(
    preset: MihrabThemePreset = MihrabThemePreset.EMERALD_SANCTUARY,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    fontScale: Float = 1.0f,
    arabicPrimary: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark || preset == MihrabThemePreset.MIDNIGHT_QIYAM
    }
    val colorScheme = when {
        preset == MihrabThemePreset.MOSQUE_BLUE && useDark -> MosqueBlueDarkScheme
        preset == MihrabThemePreset.MOSQUE_BLUE -> MosqueBlueLightScheme
        preset == MihrabThemePreset.WARM_PARCHMENT && !useDark -> SepiaLightScheme
        useDark -> EmeraldDarkScheme
        else -> EmeraldLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildMihrabTypography(fontScale = fontScale, arabicPrimary = arabicPrimary),
        shapes = MihrabShapes,
        content = content
    )
}
