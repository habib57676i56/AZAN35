package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.stringsFor
import com.example.domain.IslamicLibraryData
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.IslamicGeometricPatternCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KidsModeScreen(
    audioPlayer: QuranTtsAudioPlayer,
    appLanguage: AppLanguage = AppLanguage.ARABIC,
    strings: LocalizedStrings = stringsFor(appLanguage),
    onExitKidsMode: () -> Unit
) {
    val starsEarned = remember { mutableStateListOf(true, false, false, false) }
    val countStars = starsEarned.count { it }
    val glass = LocalLiquidGlassPalette.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LiquidGlassCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                    borderColor = glass.accentGold.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IslamicGeometricPatternCanvas(
                            modifier = Modifier.matchParentSize(),
                            tint = glass.accentGold,
                            alpha = 0.05f
                        )
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.ChildCare, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = appLanguage.pick("واحة محراب للصغار", "Mihrab Kids Sanctuary", "Mihrob Bolalar Bog'i"),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                                OutlinedButton(
                                    onClick = onExitKidsMode,
                                    modifier = Modifier.testTag("kids_exit_button")
                                ) {
                                    Text(appLanguage.pick("خروج", "Exit Kids Mode", "Chiqish"))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(countStars) {
                                    Icon(Icons.Rounded.Star, null, tint = glass.accentGold, modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = appLanguage.pick(
                                        "حصلت على $countStars / 4 نجوم اليوم!",
                                        "$countStars / 4 Stars Earned Today!",
                                        "Bugun $countStars / 4 ta yulduz oldingiz!"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = appLanguage.pick("أعمالي الصالحة اليوم", "My Daily Good Deeds", "Bugungi yaxshi ishlarim"),
                        style = MaterialTheme.typography.titleLarge
                    )
                    val deeds = when {
                        appLanguage.isArabicUi -> listOf(
                            "قلت بسم الله قبل الطعام",
                            "صليت مع عائلتي",
                            "قرأت سورة الإخلاص",
                            "تحدثت بلطف مع والديّ"
                        )
                        appLanguage == AppLanguage.UZBEK -> listOf(
                            "Ovqatdan oldin Bismillah dedim",
                            "Oilam bilan namoz o'qidim",
                            "Ixlos surasini o'qidim",
                            "Ota-onamga muloyim gapirdim"
                        )
                        else -> listOf(
                            "Said Bismillah before eating",
                            "Prayed with family",
                            "Recited Surah Al-Ikhlas",
                            "Spoke kindly to parents"
                        )
                    }
                    deeds.forEachIndexed { idx, deed ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = starsEarned[idx], onCheckedChange = { starsEarned[idx] = it })
                            Text(deed, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = appLanguage.pick("اضغط على الحروف العربية للاستماع!", "Tap Arabic Letters to Hear!", "Eshitish uchun arab harflarini bosing!"),
                        style = MaterialTheme.typography.titleLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        IslamicLibraryData.nooraniaLessons.forEach { l ->
                            Surface(
                                onClick = { audioPlayer.speak(l.name, l.letter, preferArabic = true) },
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, glass.glassBorder),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(l.letter, fontFamily = AmiriFontFamily, fontSize = 34.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(l.name.substringBefore(" "), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = appLanguage.pick("قصص الأنبياء قبل النوم", "Bedtime Prophet Stories", "Uyqu oldidan Payg'ambarlar qissasi"),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IslamicLibraryData.prophetsStories.take(4).forEach { story ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (appLanguage.isArabicUi) story.arabicName else story.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(story.keyLesson, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = {
                                audioPlayer.speak(story.name, "${story.name}. ${story.summary}", preferArabic = false)
                            }) {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(appLanguage.pick("استماع", "Listen", "Tinglash"))
                            }
                        }
                    }
                }
            }
        }
    }
}
