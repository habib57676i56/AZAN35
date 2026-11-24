package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.stringsFor
import com.example.data.firebase.KhatmahGroupDoc
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.QuranContentData
import com.example.domain.RecitationCheckResult
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.withTabularNums
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlMusammiSection(
    aiViewModel: AiViewModel,
    appLanguage: AppLanguage = AppLanguage.ARABIC,
    strings: LocalizedStrings = stringsFor(appLanguage)
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassPalette.current
    val coachState by aiViewModel.recitationCoachState.collectAsStateWithLifecycle()

    var selectedSurahIdx by remember { mutableIntStateOf(0) }
    val surah = QuranContentData.surahs[selectedSurahIdx]
    var selectedAyahIdx by remember { mutableIntStateOf(0) }
    val ayah = surah.ayahs[selectedAyahIdx.coerceIn(0, surah.ayahs.lastIndex)]

    var examMode by remember { mutableStateOf(false) }
    var maskFraction by remember { mutableFloatStateOf(0.5f) }
    var hintRevealedWords by remember { mutableIntStateOf(0) }
    var spokenAttempt by remember { mutableStateOf("") }
    var diffResult by remember { mutableStateOf<RecitationCheckResult?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var micNotice by remember { mutableStateOf<String?>(null) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startArabicSpeechRecognition() {
        if (speechRecognizer == null) {
            micNotice = appLanguage.pick(
                "التعرف على الصوت غير متاح في هذا الجهاز؛ يمكنك كتابة أو لصق الآية أدناه.",
                "Device speech recognizer unavailable; type or paste Arabic below to check words.",
                "Qurilmada ovozli aniqlash mavjud emas; quyida arabcha matnni kiriting."
            )
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                micNotice = appLanguage.pick("جارٍ الاستماع للتلاوة بالعربية...", "Listening in Arabic...", "Arabcha tilovat tinglanmoqda...")
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                micNotice = appLanguage.pick(
                    "انتهى التقاط الصوت (رمز $error). يمكنك التعديل والاختبار يدوياً أدناه.",
                    "Speech capture ended (code $error). Edit or test below.",
                    "Ovoz yozish yakunlandi (kod $error). Quyida tahrirlashingiz mumkin."
                )
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull().orEmpty()
                if (best.isNotEmpty()) {
                    spokenAttempt = best
                    diffResult = QuranContentData.compareRecitation(ayah.arabic, best)
                    micNotice = null
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val best = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!best.isNullOrBlank()) spokenAttempt = best
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        speechRecognizer.startListening(intent)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startArabicSpeechRecognition()
        else micNotice = appLanguage.pick(
            "تم رفض إذن الميكروفون؛ اكتب التلاوة أدناه.",
            "Microphone permission denied; type your recitation below.",
            "Mikrofon ruxsati berilmadi; quyida yozib tekshiring."
        )
    }

    val words = remember(ayah.arabic) {
        ayah.arabic.split(Regex("\\s+")).filter { it.isNotBlank() && it != "۝" }
    }
    val visibleThreshold = ((1f - maskFraction) * words.size).roundToInt().coerceAtLeast(hintRevealedWords)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LiquidGlassCard(
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        appLanguage.pick("المُسمِّع ومصحح التلاوة", "Al-Musammi' Recitation Checker", "Musammi' qiroat tekshiruvchi"),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        appLanguage.pick(
                            "إخفاء متدرج للكلمات ومدرب تجويد ذكي",
                            "Progressive word masking & AI Tajweed coach",
                            "So'zlarni bosqichma-bosqich yashirish va AI tajvid ustozi"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(appLanguage.pick("اختبار", "Exam", "Imtihon"), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(6.dp))
                    Switch(checked = examMode, onCheckedChange = {
                        examMode = it
                        if (it) maskFraction = 1f
                    })
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                QuranContentData.surahs.forEachIndexed { idx, s ->
                    LiquidGlassPill(
                        selected = selectedSurahIdx == idx,
                        onClick = {
                            selectedSurahIdx = idx
                            selectedAyahIdx = 0
                            diffResult = null
                            hintRevealedWords = 0
                            aiViewModel.resetRecitationCoach()
                        },
                        label = if (appLanguage.isArabicUi) s.nameAr else s.nameEn.substringBefore(" (")
                    )
                }
            }

            if (surah.ayahs.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    surah.ayahs.forEachIndexed { idx, item ->
                        LiquidGlassPill(
                            selected = selectedAyahIdx == idx,
                            onClick = {
                                selectedAyahIdx = idx
                                diffResult = null
                                hintRevealedWords = 0
                            },
                            label = appLanguage.pick("آية ${item.number}", "Ayah ${item.number}", "${item.number}-oyat")
                        )
                    }
                }
            }

            if (!examMode) {
                Text(
                    appLanguage.pick(
                        "نسبة الإخفاء المتدرج: ${(maskFraction * 100).roundToInt()}%",
                        "Progressive Page Masking: ${(maskFraction * 100).roundToInt()}% hidden",
                        "Yashirish darajasi: ${(maskFraction * 100).roundToInt()}%"
                    ),
                    style = MaterialTheme.typography.labelLarge.withTabularNums()
                )
                Slider(
                    value = maskFraction,
                    onValueChange = { maskFraction = it },
                    valueRange = 0f..1f
                )
            }

            Surface(
                color = glass.glassSubtleFill,
                border = BorderStroke(1.dp, glass.glassBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val maskedText = words.mapIndexed { idx, word ->
                    if (idx < visibleThreshold) word else "••••"
                }.joinToString("  ")

                Text(
                    text = maskedText,
                    fontFamily = AmiriFontFamily,
                    fontSize = 24.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!examMode) {
                    OutlinedButton(onClick = { hintRevealedWords = (hintRevealedWords + 1).coerceAtMost(words.size) }) {
                        Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(appLanguage.pick("تلميح كلمة", "Hint Word", "So'z yordam"))
                    }
                }
                Button(
                    onClick = {
                        if (isListening) {
                            speechRecognizer?.stopListening()
                            isListening = false
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) startArabicSpeechRecognition()
                            else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.testTag("musammi_mic_button")
                ) {
                    Icon(if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isListening) strings.stopAdhan
                        else appLanguage.pick("تسميع بالصوت", "Recite by Voice", "Ovoz bilan o'qish")
                    )
                }
            }

            if (micNotice != null) {
                Text(micNotice!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            OutlinedTextField(
                value = spokenAttempt,
                onValueChange = { spokenAttempt = it },
                label = {
                    Text(
                        appLanguage.pick(
                            "محاولة التسميع (نص صوتي أو كتابة عربية)",
                            "Recitation attempt (voice transcript or manual Arabic)",
                            "Tilovat matni (ovozli yoki arabcha yozma)"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { diffResult = QuranContentData.compareRecitation(ayah.arabic, spokenAttempt) },
                    enabled = spokenAttempt.isNotBlank()
                ) {
                    Text(appLanguage.pick("فحص الكلمات", "Check Words", "So'zlarni tekshirish"))
                }
                OutlinedButton(
                    onClick = {
                        aiViewModel.evaluateRecitation(
                            surahName = surah.nameEn,
                            ayahRef = "${surah.number}:${ayah.number}",
                            expectedArabic = ayah.arabic,
                            recitedText = spokenAttempt
                        )
                    },
                    enabled = spokenAttempt.isNotBlank()
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(appLanguage.pick("مدرب التجويد AI", "AI Tajweed Coach", "AI Tajvid ustozi"))
                }
            }

            diffResult?.let { result ->
                HorizontalDivider(color = glass.glassBorder)
                Text(
                    appLanguage.pick(
                        "الدقة: ${result.accuracyPercent}% • ${result.missingCount} كلمة للمراجعة",
                        "Accuracy: ${result.accuracyPercent}% • ${result.missingCount} word(s) to review",
                        "Aniqlik: ${result.accuracyPercent}% • ${result.missingCount} ta so'z tekshirilishi kerak"
                    ),
                    style = MaterialTheme.typography.titleMedium.withTabularNums(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.wordDiffs.forEach { diff ->
                        Surface(
                            color = if (diff.matched) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = diff.expectedWord,
                                fontFamily = AmiriFontFamily,
                                fontSize = 18.sp,
                                color = if (diff.matched) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            when (val cs = coachState) {
                is AiUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                is AiUiState.Success -> LiquidGlassCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    contentPadding = 12.dp
                ) {
                    Text(cs.text, style = MaterialTheme.typography.bodyMedium)
                }
                is AiUiState.Error -> Text(cs.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KhatmahAndGroupsSection(
    settings: UserSettingsDoc,
    groupsState: UiState<List<KhatmahGroupDoc>>,
    viewModel: MihrabViewModel,
    familyKhatmahEnabled: Boolean = true,
    appLanguage: AppLanguage = AppLanguage.ARABIC,
    strings: LocalizedStrings = stringsFor(appLanguage)
) {
    val glass = LocalLiquidGlassPalette.current
    var newGroupName by remember { mutableStateOf("") }
    var dailyPagesGoal by remember { mutableIntStateOf(20) }
    val pagesRead = settings.khatmahPagesRead.toInt().coerceIn(0, 604)
    val daysLeft = ((604 - pagesRead).coerceAtLeast(0) + dailyPagesGoal - 1) / dailyPagesGoal

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LiquidGlassCard(
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                appLanguage.pick("خطة الختمة الشخصية", "Personal Khatmah Planner", "Shaxsiy Xatma rejasi"),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                appLanguage.pick(
                    "$pagesRead / 604 صفحة (${(pagesRead * 100) / 604}%) • المتبقي تقريباً $daysLeft يوم بمعدل $dailyPagesGoal صفحة/يوم",
                    "$pagesRead / 604 Mushaf Pages (${(pagesRead * 100) / 604}%) • Est. $daysLeft days remaining at $dailyPagesGoal pages/day",
                    "$pagesRead / 604 sahifa (${(pagesRead * 100) / 604}%) • Kuniga $dailyPagesGoal sahifadan ~${daysLeft} kun qoldi"
                ),
                style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(
                progress = { pagesRead / 604f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    viewModel.updateSettings(mapOf("khatmahPagesRead" to (pagesRead + 4).coerceAtMost(604).toLong()))
                }) {
                    Text(appLanguage.pick("+٤ صفحات (ورد)", "+4 Pages (1 Wird)", "+4 Sahifa (Vird)"))
                }
                OutlinedButton(onClick = {
                    viewModel.updateSettings(mapOf("khatmahPagesRead" to (pagesRead + 20).coerceAtMost(604).toLong()))
                }) {
                    Text(appLanguage.pick("+٢٠ صفحة (جزء)", "+20 Pages (1 Juz')", "+20 Sahifa (1 Juz)"))
                }
            }
            Text(
                appLanguage.pick(
                    "الورد اليومي المستهدف: $dailyPagesGoal صفحة/يوم",
                    "Daily Pace Goal: $dailyPagesGoal pages/day",
                    "Kunlik maqsad: $dailyPagesGoal sahifa/kun"
                ),
                style = MaterialTheme.typography.labelLarge.withTabularNums()
            )
            Slider(
                value = dailyPagesGoal.toFloat(),
                onValueChange = { dailyPagesGoal = it.roundToInt().coerceIn(4, 40) },
                valueRange = 4f..40f
            )
        }

        if (familyKhatmahEnabled) {
            LiquidGlassCard(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    appLanguage.pick("ختمة العائلة والأصدقاء", "Family & Friends Khatmah", "Oila va do'stlar Xatmasi"),
                    style = MaterialTheme.typography.titleLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text(appLanguage.pick("اسم المجموعة الجديدة", "New group title", "Yangi guruh nomi")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        if (newGroupName.isNotBlank()) {
                            viewModel.createKhatmahGroup(newGroupName)
                            newGroupName = ""
                        }
                    }) {
                        Icon(Icons.Rounded.GroupAdd, null, modifier = Modifier.size(18.dp))
                    }
                }

                when (groupsState) {
                    is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    is UiState.Error -> Text(groupsState.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> {
                        if (groupsState.data.isEmpty()) {
                            Text(
                                appLanguage.pick(
                                    "أنشئ ختمة عائلية أعلاه وحدد الأجزاء ١–٣٠ معاً بشكل مباشر.",
                                    "Create a Family Khatmah above and mark Juz' 1–30 together in real time.",
                                    "Yuqorida oilaviy Xatma yarating va 1–30 juzlarni birgalikda belgilang."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            groupsState.data.forEach { group ->
                                LiquidGlassCard(
                                    containerColor = glass.glassSubtleFill,
                                    contentPadding = 12.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${group.completedJuz.size}/30 ${strings.juzLabel} • ${group.joinCode}",
                                            style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        (1..30).forEach { juz ->
                                            val done = juz.toLong() in group.completedJuz
                                            LiquidGlassPill(
                                                selected = done,
                                                onClick = { viewModel.toggleGroupJuz(group, juz) },
                                                icon = if (done) Icons.Rounded.CheckCircle else null,
                                                label = "J$juz"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
