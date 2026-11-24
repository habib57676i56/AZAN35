package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal),
    Font(R.font.tajawal, FontWeight.Medium),
    Font(R.font.tajawal, FontWeight.Bold)
)

val AmiriQuranFontFamily = FontFamily(
    Font(R.font.amiri_quran, FontWeight.Normal),
    Font(R.font.amiri_quran, FontWeight.Bold)
)

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

val PlayfairDisplayFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

fun buildMihrabTypography(fontScale: Float = 1.0f, arabicPrimary: Boolean = true): Typography {
    val scale = fontScale.coerceIn(0.8f, 1.5f)
    val displayFamily = if (arabicPrimary) CairoFontFamily else PlayfairDisplayFontFamily
    val uiFamily = if (arabicPrimary) CairoFontFamily else PlusJakartaSansFontFamily
    val bodyFamily = if (arabicPrimary) TajawalFontFamily else PlusJakartaSansFontFamily

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (34f * scale).sp,
            lineHeight = (44f * scale).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (26f * scale).sp,
            lineHeight = (36f * scale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (21f * scale).sp,
            lineHeight = (30f * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (18f * scale).sp,
            lineHeight = (26f * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15.5f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scale).sp,
            lineHeight = (23f * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13f * scale).sp,
            lineHeight = (18f * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * scale).sp,
            lineHeight = (16f * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * scale).sp,
            lineHeight = (15f * scale).sp
        )
    )
}

val MihrabTypography = buildMihrabTypography()
