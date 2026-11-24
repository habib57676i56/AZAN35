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
import com.example.data.firebase.GoogleSignInButton
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.IslamicGeometricPatternCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.SacredAmbientCanvas
import com.example.ui.theme.SacredEmerald

@Composable
fun AuthScreen(
    signInButtonText: String,
    onAuthSuccess: () -> Unit,
    firebaseConfigMissing: Boolean = false
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val glass = LocalLiquidGlassPalette.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SacredAmbientCanvas {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .widthIn(max = 520.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                .height(236.dp)
                                .clip(RoundedCornerShape(30.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_kaaba_horizon),
                                contentDescription = "إطلالة هادئة نحو المسجد الحرام",
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
                                    text = "محراب • Mihrab",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "رفيقك المتكامل للقرآن والصلاة والذكر",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "مصحف المدينة الكامل بـ ٦٠٤ صفحات ونص عثماني تفاعلي مع كبار القرّاء، مواقيت الصلاة والقبلة بالواقع المعزز، الأذكار، وأدوات الزكاة والمواريث بمزامنة سحابية آمنة.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    LiquidGlassCard(
                        shape = RoundedCornerShape(26.dp),
                        contentPadding = 18.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrustRow(
                            icon = Icons.Rounded.Lock,
                            title = "خصوصية تامة ومزامنة محمية",
                            subtitle = "موضع قراءتك وعلامات المصحف وأدعيتك محفوظة لحسابك وحدك مع دعم التصفح دون إنترنت."
                        )
                        TrustRow(
                            icon = Icons.Rounded.Mosque,
                            title = "تجربة خاشعة بلا إعلانات",
                            subtitle = "قراءة متواصلة في المصحف ومتابعة الصلوات والتسبيح بتركيز وهدوء."
                        )
                        TrustRow(
                            icon = Icons.Rounded.VerifiedUser,
                            title = "مصحف المدينة وتلاوات متزامنة",
                            subtitle = "نص عثماني تفاعلي مع تظليل الآية وتقليب الصفحات تلقائياً أثناء التلاوة."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

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
                                text = "إعدادات Firebase غير مدمجة في هذه النسخة (ملف app/google-services.json مفقود أثناء بناء APK). أضِف الملف في GitHub ثم أعد البناء لتفعيل تسجيل الدخول.",
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
