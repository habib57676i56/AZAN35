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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val useDark = darkTheme || preset == MihrabThemePreset.MIDNIGHT_QIYAM
    val colorScheme = when {
        preset == MihrabThemePreset.MOSQUE_BLUE && useDark -> MosqueBlueDarkScheme
        preset == MihrabThemePreset.MOSQUE_BLUE -> MosqueBlueLightScheme
        preset == MihrabThemePreset.WARM_PARCHMENT && !useDark -> SepiaLightScheme
        useDark -> EmeraldDarkScheme
        else -> EmeraldLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MihrabTypography,
        shapes = MihrabShapes,
        content = content
    )
}
