package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.stringsFor
import com.example.data.firebase.GoogleSignInButton
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.IslamicGeometricPatternCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.SacredAmbientCanvas
import com.example.ui.theme.SacredEmerald

@Composable
fun AuthScreen(
    selectedLanguage: AppLanguage = AppLanguage.ARABIC,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    strings: LocalizedStrings = stringsFor(selectedLanguage),
    signInButtonText: String = strings.signInWithGoogle,
    onAuthSuccess: () -> Unit,
    firebaseConfigMissing: Boolean = false
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val glass = LocalLiquidGlassPalette.current

    CompositionLocalProvider(LocalLayoutDirection provides selectedLanguage.layoutDirection) {
        SacredAmbientCanvas {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .widthIn(max = 520.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Pre-login language selector (Arabic / English / Uzbek)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(AppLanguage.ARABIC, AppLanguage.ENGLISH, AppLanguage.UZBEK).forEach { lang ->
                            LiquidGlassPill(
                                selected = selectedLanguage == lang,
                                onClick = { onSelectLanguage(lang) },
                                label = lang.nativeLabel
                            )
                        }
                    }

                    LiquidGlassCard(
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = 0.dp,
                        borderColor = glass.accentGold.copy(alpha = 0.38f),
                        elevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(228.dp)
                                .clip(RoundedCornerShape(30.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_kaaba_horizon),
                                contentDescription = selectedLanguage.pick(
                                    "إطلالة هادئة نحو المسجد الحرام",
                                    "Serene view toward Masjid al-Haram",
                                    "Masjidul Harom tomon sokin manzara"
                                ),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x33051914),
                                                SacredEmerald.copy(alpha = 0.92f)
                                            )
                                        )
                                    )
                            )
                            IslamicGeometricPatternCanvas(
                                modifier = Modifier.fillMaxSize(),
                                tint = glass.accentGold,
                                alpha = 0.06f
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(22.dp)
                            ) {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    fontFamily = AmiriQuranFontFamily,
                                    fontSize = 23.sp,
                                    color = Color(0xFFF6E2B3)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedLanguage.pick(
                                        "محراب • Mihrab",
                                        "Mihrab • محراب",
                                        "Mihrob • محراب"
                                    ),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = selectedLanguage.pick(
                            "رفيقك المتكامل للقرآن والصلاة والذكر",
                            "Your Complete Companion for Quran, Salah & Dhikr",
                            "Qur'on, Namoz va Zikr uchun mukammal hamrohingiz"
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedLanguage.pick(
                            "مصحف المدينة الكامل بـ ٦٠٤ صفحات ونص عثماني تفاعلي مع كبار القرّاء، مواقيت الصلاة والقبلة بالواقع المعزز، الأذكار، وأدوات الزكاة والمواريث بمزامنة سحابية آمنة.",
                            "Complete 604-page Madinah Mushaf with top reciters, GPS prayer times & AR Qibla, nearby mosques, Adhkar, and Zakat & inheritance tools synced securely.",
                            "604 sahifalik to'liq Madina Mushafi va mashhur qorilar, aniq GPS namoz vaqtlari va Qibla, yaqin masjidlar, zikrlar hamda zakot va meros asboblari."
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LiquidGlassCard(
                        shape = RoundedCornerShape(26.dp),
                        contentPadding = 18.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrustRow(
                            icon = Icons.Rounded.Lock,
                            title = selectedLanguage.pick(
                                "خصوصية تامة ومزامنة محمية",
                                "Complete Privacy & Cloud Sync",
                                "To'liq maxfiylik va bulutli sinxronlash"
                            ),
                            subtitle = selectedLanguage.pick(
                                "موضع قراءتك وعلامات المصحف وأدعيتك محفوظة لحسابك وحدك مع دعم التصفح دون إنترنت.",
                                "Your reading progress, bookmarks, and journals are securely saved to your account with offline support.",
                                "O'qish sahifangiz, xatcho'plaringiz va duolaringiz hisobingizda xavfsiz saqlanadi va oflayn ishlaydi."
                            )
                        )
                        TrustRow(
                            icon = Icons.Rounded.Mosque,
                            title = selectedLanguage.pick(
                                "تجربة خاشعة بلا إعلانات",
                                "Focused Worship Without Ads",
                                "Reklamasiz xotirjam ibodat"
                            ),
                            subtitle = selectedLanguage.pick(
                                "قراءة متواصلة في المصحف ومتابعة الصلوات والتسبيح بتركيز وهدوء.",
                                "Read Quran, track daily prayers, find nearby mosques, and make Dhikr peacefully.",
                                "Qur'on o'qish, namozlarni kuzatish, yaqin masjidlarni topish va zikr qilish uchun sokin muhit."
                            )
                        )
                        TrustRow(
                            icon = Icons.Rounded.VerifiedUser,
                            title = selectedLanguage.pick(
                                "مصحف المدينة وتلاوات في الخلفية",
                                "Madinah Mushaf & Background Recitation",
                                "Madina Mushafi va fonda tilovat"
                            ),
                            subtitle = selectedLanguage.pick(
                                "نص عثماني تفاعلي مع تظليل الآية واستمرار التلاوة بعد الخروج من التطبيق.",
                                "Interactive Uthmani script with verse highlighting and continuous audio even after leaving the app.",
                                "Oyatni ajratib ko'rsatish va ilovadan chiqqanda ham uzluksiz davom etadigan audio tilovat."
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (firebaseConfigMissing) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = selectedLanguage.pick(
                                    "إعدادات Firebase غير مدمجة في هذه النسخة (ملف app/google-services.json مفقود أثناء بناء APK). أضِف الملف في GitHub ثم أعد البناء لتفعيل تسجيل الدخول.",
                                    "Firebase configuration is missing from this build (app/google-services.json was absent during APK build). Add it in GitHub and rebuild to enable Sign-In.",
                                    "Ushbu APK yig'ilishida Firebase sozlamalari (app/google-services.json) topilmadi. Kirishni yoqish uchun faylni qo'shib qayta yig'ing."
                                ),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    GoogleSignInButton(
                        buttonText = signInButtonText,
                        onAuthSuccess = {
                            errorMessage = null
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            errorMessage = msg
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val glass = LocalLiquidGlassPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = glass.accentGold.copy(alpha = 0.16f),
            shape = CircleShape,
            border = BorderStroke(1.dp, glass.accentGold.copy(alpha = 0.35f)),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
