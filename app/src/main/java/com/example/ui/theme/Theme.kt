package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
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
        cardInner = Color(0xFFF4EFE3),
        fg = Color(0xFF141C1A),
        accent = SacredEmerald,
        border = ChampagneGold
    ),
    SEPIA(
        id = "SEPIA",
        bg = Color(0xFFF4EAD4),
        cardInner = Color(0xFFEADBC0),
        fg = Color(0xFF2D2111),
        accent = Color(0xFF6B4B1B),
        border = Color(0xFFB58634)
    ),
    DARK(
        id = "DARK",
        bg = Color(0xFF071310),
        cardInner = Color(0xFF10231F),
        fg = Color(0xFFF2F6F4),
        accent = LuminousMintEmerald,
        border = GlowingGold
    );

    companion object {
        fun fromId(id: String): MushafPaperTheme =
            entries.firstOrNull { it.id == id } ?: WHITE
    }
}

enum class MihrabThemePreset(val label: String, val labelAr: String) {
    EMERALD_SANCTUARY("Emerald Sanctuary", "الزمرد المقدس"),
    ROYAL_GOLD("Royal Gold", "الذهب الملكي"),
    MOSQUE_BLUE("Mosque Sapphire", "ياقوت المسجد"),
    WARM_PARCHMENT("Warm Parchment", "الرق الدافئ")
}

@Immutable
data class LiquidGlassPalette(
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasBottom: Color,
    val ambientGlowPrimary: Color,
    val ambientGlowGold: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassSubtleFill: Color,
    val glassBorder: Color,
    val specularHighlight: Color,
    val accentGold: Color,
    val dockBackground: Color,
    val dockActiveIndicator: Color
)

val LocalLiquidGlassPalette = staticCompositionLocalOf {
    LiquidGlassPalette(
        isDark = false,
        canvasTop = AlabasterCanvas,
        canvasBottom = AlabasterCanvasDeep,
        ambientGlowPrimary = SacredEmerald.copy(alpha = 0.08f),
        ambientGlowGold = ChampagneGold.copy(alpha = 0.11f),
        glassFill = LightGlassFill,
        glassFillStrong = LightGlassFillStrong,
        glassSubtleFill = Color(0x70FFFFFF),
        glassBorder = LightGlassBorder,
        specularHighlight = LightSpecularTop,
        accentGold = ChampagneGold,
        dockBackground = Color(0xD9FFFFFF),
        dockActiveIndicator = SacredEmerald.copy(alpha = 0.13f)
    )
}

private val EmeraldLightScheme = lightColorScheme(
    primary = SacredEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EAE3),
    onPrimaryContainer = SacredEmerald,
    secondary = ChampagneGold,
    onSecondary = Color(0xFF281E06),
    secondaryContainer = PaleGoldWash,
    onSecondaryContainer = Color(0xFF4A3709),
    tertiary = SacredEmeraldMuted,
    onTertiary = Color.White,
    background = AlabasterCanvas,
    onBackground = WarmInk,
    surface = AlabasterCanvas,
    onSurface = WarmInk,
    surfaceVariant = Color(0xFFECEAE2),
    onSurfaceVariant = MutedSlateInk,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightGlassFill,
    surfaceContainer = Color(0xCCF3EFE6),
    surfaceContainerHigh = LightGlassFillStrong,
    outline = Color(0x330D5C4B),
    error = AlertCoral,
    errorContainer = SoftCoralContainer
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = LuminousMintEmerald,
    onPrimary = Color(0xFF03261C),
    primaryContainer = Color(0xFF103B31),
    onPrimaryContainer = Color(0xFFB5F2DE),
    secondary = GlowingGold,
    onSecondary = Color(0xFF2F2103),
    secondaryContainer = Color(0xFF3E3010),
    onSecondaryContainer = Color(0xFFFFEAB8),
    tertiary = Color(0xFF6AE5C3),
    onTertiary = Color(0xFF03261C),
    background = MidnightObsidianTop,
    onBackground = PearlWhite,
    surface = MidnightObsidianTop,
    onSurface = PearlWhite,
    surfaceVariant = DeepSlateEmerald,
    onSurfaceVariant = CalmSageGray,
    surfaceContainerLowest = Color(0xFF040B09),
    surfaceContainerLow = DarkGlassFill,
    surfaceContainer = Color(0xB8142C27),
    surfaceContainerHigh = DarkGlassFillStrong,
    outline = DarkGlassBorder,
    error = DarkAlertCoral,
    errorContainer = Color(0xFF5D2118)
)

private val RoyalGoldLightScheme = EmeraldLightScheme.copy(
    primary = RoyalGoldLightPrimary,
    primaryContainer = RoyalGoldLightContainer,
    onPrimaryContainer = Color(0xFF4A3405),
    secondary = SacredEmerald,
    tertiary = ChampagneGold,
    outline = Color(0x2E8C6514)
)

private val RoyalGoldDarkScheme = EmeraldDarkScheme.copy(
    primary = RoyalGoldDarkPrimary,
    onPrimary = Color(0xFF2D1E02),
    primaryContainer = RoyalGoldDarkContainer,
    onPrimaryContainer = Color(0xFFFFECB8),
    secondary = LuminousMintEmerald
)

private val MosqueBlueLightScheme = EmeraldLightScheme.copy(
    primary = MosqueBluePrimary,
    primaryContainer = Color(0xFFD5E8F7),
    onPrimaryContainer = MosqueBluePrimary,
    tertiary = MosqueBlueSecondary,
    outline = Color(0x26124670)
)

private val MosqueBlueDarkScheme = EmeraldDarkScheme.copy(
    primary = MosqueBlueDarkPrimary,
    onPrimary = Color(0xFF032036),
    primaryContainer = Color(0xFF143C5E),
    onPrimaryContainer = Color(0xFFCDE9FF),
    background = Color(0xFF060E17),
    surface = Color(0xFF060E17),
    surfaceVariant = Color(0xFF0E1D2D),
    surfaceContainerLow = Color(0x9E0F2032),
    surfaceContainer = Color(0xB813283D),
    surfaceContainerHigh = Color(0xCC162E46)
)

private val SepiaLightScheme = EmeraldLightScheme.copy(
    background = WarmSepiaBackground,
    surface = WarmSepiaBackground,
    surfaceVariant = WarmSepiaSurface,
    surfaceContainerLow = Color(0xCCF7EFDD),
    surfaceContainer = WarmSepiaSurface,
    surfaceContainerHigh = Color(0xEBFAF3E4)
)

val MihrabShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
private fun animateScheme(target: ColorScheme): ColorScheme {
    val anim = tween<Color>(durationMillis = 300)
    return ColorScheme(
        primary = animateColorAsState(target.primary, anim, label = "p").value,
        onPrimary = animateColorAsState(target.onPrimary, anim, label = "op").value,
        primaryContainer = animateColorAsState(target.primaryContainer, anim, label = "pc").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, anim, label = "opc").value,
        inversePrimary = target.inversePrimary,
        secondary = animateColorAsState(target.secondary, anim, label = "s").value,
        onSecondary = animateColorAsState(target.onSecondary, anim, label = "os").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, anim, label = "sc").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, anim, label = "osc").value,
        tertiary = animateColorAsState(target.tertiary, anim, label = "t").value,
        onTertiary = target.onTertiary,
        tertiaryContainer = target.tertiaryContainer,
        onTertiaryContainer = target.onTertiaryContainer,
        background = animateColorAsState(target.background, anim, label = "bg").value,
        onBackground = animateColorAsState(target.onBackground, anim, label = "obg").value,
        surface = animateColorAsState(target.surface, anim, label = "sf").value,
        onSurface = animateColorAsState(target.onSurface, anim, label = "osf").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, anim, label = "sv").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, anim, label = "osv").value,
        surfaceTint = target.surfaceTint,
        inverseSurface = target.inverseSurface,
        inverseOnSurface = target.inverseOnSurface,
        error = target.error,
        onError = target.onError,
        errorContainer = target.errorContainer,
        onErrorContainer = target.onErrorContainer,
        outline = animateColorAsState(target.outline, anim, label = "ol").value,
        outlineVariant = target.outlineVariant,
        scrim = target.scrim,
        surfaceBright = target.surfaceBright,
        surfaceDim = target.surfaceDim,
        surfaceContainerLowest = target.surfaceContainerLowest,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, anim, label = "scl").value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, anim, label = "scn").value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, anim, label = "sch").value,
        surfaceContainerHighest = target.surfaceContainerHighest
    )
}

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
        AppThemeMode.SYSTEM -> systemDark
    }

    val targetScheme = when {
        preset == MihrabThemePreset.MOSQUE_BLUE && useDark -> MosqueBlueDarkScheme
        preset == MihrabThemePreset.MOSQUE_BLUE -> MosqueBlueLightScheme
        preset == MihrabThemePreset.ROYAL_GOLD && useDark -> RoyalGoldDarkScheme
        preset == MihrabThemePreset.ROYAL_GOLD -> RoyalGoldLightScheme
        preset == MihrabThemePreset.WARM_PARCHMENT && !useDark -> SepiaLightScheme
        useDark -> EmeraldDarkScheme
        else -> EmeraldLightScheme
    }

    val colorScheme = animateScheme(targetScheme)

    val glassPalette = if (useDark) {
        val topCanvas = if (preset == MihrabThemePreset.MOSQUE_BLUE) Color(0xFF060E17) else MidnightObsidianTop
        val botCanvas = if (preset == MihrabThemePreset.MOSQUE_BLUE) Color(0xFF0B1C2D) else MidnightObsidianBottom
        LiquidGlassPalette(
            isDark = true,
            canvasTop = topCanvas,
            canvasBottom = botCanvas,
            ambientGlowPrimary = colorScheme.primary.copy(alpha = 0.14f),
            ambientGlowGold = GlowingGold.copy(alpha = 0.10f),
            glassFill = colorScheme.surfaceContainerLow,
            glassFillStrong = colorScheme.surfaceContainerHigh,
            glassSubtleFill = Color(0x3D142C27),
            glassBorder = DarkGlassBorder,
            specularHighlight = DarkSpecularTop,
            accentGold = GlowingGold,
            dockBackground = Color(0xCC0D201C),
            dockActiveIndicator = colorScheme.primary.copy(alpha = 0.20f)
        )
    } else {
        LiquidGlassPalette(
            isDark = false,
            canvasTop = colorScheme.background,
            canvasBottom = if (preset == MihrabThemePreset.WARM_PARCHMENT) Color(0xFFEBDDC0) else AlabasterCanvasDeep,
            ambientGlowPrimary = colorScheme.primary.copy(alpha = 0.08f),
            ambientGlowGold = ChampagneGold.copy(alpha = 0.12f),
            glassFill = colorScheme.surfaceContainerLow,
            glassFillStrong = colorScheme.surfaceContainerHigh,
            glassSubtleFill = Color(0x7AFFFFFF),
            glassBorder = colorScheme.outline,
            specularHighlight = LightSpecularTop,
            accentGold = ChampagneGold,
            dockBackground = Color(0xDBFFFFFF),
            dockActiveIndicator = colorScheme.primary.copy(alpha = 0.13f)
        )
    }

    CompositionLocalProvider(LocalLiquidGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildMihrabTypography(fontScale = fontScale, arabicPrimary = arabicPrimary),
            shapes = MihrabShapes,
            content = content
        )
    }
}
