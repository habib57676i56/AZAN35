package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.HijriCalendarEngine
import com.example.domain.IslamicLibraryData
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.IslamicGeometricPatternCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.SanctuaryEmerald
import com.example.ui.theme.withTabularNums
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearnScreen(
    viewModel: MihrabViewModel,
    aiViewModel: AiViewModel,
    audioPlayer: QuranTtsAudioPlayer,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    useIndic: Boolean,
    initialSubTab: Int = 0,
    appOperations: AppOperationsConfig = AppOperationsConfig()
) {
    val glass = LocalLiquidGlassPalette.current
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val logsState by viewModel.prayerLogsState.collectAsStateWithLifecycle()
    val manualOffsets by viewModel.manualOffsets.collectAsStateWithLifecycle()

    val isArabicUi = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU
    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val logs = (logsState as? UiState.Success)?.data ?: emptyList()
    val todayLog = logs.firstOrNull { it.dateKey == viewModel.todayDateKey() }
    val hijriToday = remember(settings) { viewModel.computeHijriToday(settings) }
    val sunnahFast = remember(hijriToday) { HijriCalendarEngine.isSunnahFastingDay(Calendar.getInstance(), hijriToday) }
    val occasions = remember(hijriToday) { HijriCalendarEngine.upcomingOccasions(hijriToday) }
    val schedule = remember(settings, manualOffsets) { viewModel.computeSchedule(settings, manualOffsets) }

    var activeSection by remember(initialSubTab) { mutableIntStateOf(initialSubTab) } // 0=Calendar&Ramadan, 1=Knowledge, 2=Noorania, 3=AskAI

    val learnTabs = listOf(
        Triple("${strings.hijriCalendar} & ${strings.ramadanMode}", Icons.Rounded.CalendarMonth, 0),
        Triple(strings.knowledgeHub, MihrabIcons.ScholarBook, 1),
        Triple(if (isArabicUi) "القاعدة النورانية" else "Al-Noorania", Icons.Rounded.RecordVoiceOver, 2),
        Triple(strings.askAiCompanion, Icons.Rounded.Psychology, 3)
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(learnTabs, key = { it.third }) { (label, icon, idx) ->
                        LiquidGlassPill(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            icon = icon,
                            label = label,
                            modifier = Modifier.testTag("learn_tab_$idx")
                        )
                    }
                }
            }

            when (activeSection) {
                0 -> {
                    item {
                        LiquidGlassCard(
                            shape = RoundedCornerShape(26.dp),
                            containerColor = glass.glassFillStrong,
                            contentPadding = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                IslamicGeometricPatternCanvas(
                                    modifier = Modifier.matchParentSize(),
                                    tint = MaterialTheme.colorScheme.primary,
                                    alpha = 0.03f
                                )
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(formatNumerals(hijriToday.formattedEn(), useIndic), style = MaterialTheme.typography.headlineMedium.withTabularNums(), fontWeight = FontWeight.Bold)
                                            Text(hijriToday.formattedAr(), fontFamily = AmiriFontFamily, fontSize = 21.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (isArabicUi) "تعديل:" else "Adj:", style = MaterialTheme.typography.labelLarge)
                                            TextButton(onClick = {
                                                viewModel.updateSettings(mapOf("hijriOffset" to (settings.hijriOffset - 1L).coerceIn(-2L, 2L)))
                                            }) { Text("-1") }
                                            Text("${settings.hijriOffset}", style = MaterialTheme.typography.titleMedium.withTabularNums(), fontWeight = FontWeight.Bold)
                                            TextButton(onClick = {
                                                viewModel.updateSettings(mapOf("hijriOffset" to (settings.hijriOffset + 1L).coerceIn(-2L, 2L)))
                                            }) { Text("+1") }
                                        }
                                    }

                                    if (sunnahFast != null) {
                                        Surface(
                                            color = BurnishedGold.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = if (isArabicUi) "صيام مستحب اليوم: $sunnahFast" else "Today's Fasting Virtue: $sunnahFast",
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Text(if (isArabicUi) "متابعة الصيام اليومي" else "Today's Fasting Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            "NONE" to (if (isArabicUi) "غير صائم" else "Not Fasting"),
                                            "VOLUNTARY" to (if (isArabicUi) "صيام سنة" else "Sunnah"),
                                            "RAMADAN" to (if (isArabicUi) "رمضان" else "Ramadan"),
                                            "QADA" to (if (isArabicUi) "قضاء" else "Qada")
                                        ).forEach { (key, label) ->
                                            LiquidGlassPill(
                                                selected = (todayLog?.fastingType ?: "NONE") == key,
                                                onClick = { viewModel.setFastingToday(key, todayLog) },
                                                label = label
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dedicated Ramadan Mode Card
                    item {
                        val ramadanChecklist = remember { mutableStateListOf(false, false, false, false) }
                        LiquidGlassCard(
                            shape = RoundedCornerShape(28.dp),
                            containerColor = SanctuaryEmerald.copy(alpha = 0.94f),
                            borderColor = BurnishedGold.copy(alpha = 0.48f),
                            contentPadding = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(166.dp)) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_ramadan_lantern),
                                        contentDescription = "Ramadan Fanous Lantern",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IslamicGeometricPatternCanvas(
                                        modifier = Modifier.matchParentSize(),
                                        tint = Color.White,
                                        alpha = 0.045f
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(
                                            Brush.verticalGradient(listOf(Color.Transparent, SanctuaryEmerald.copy(alpha = 0.94f)))
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                if (isArabicUi) "وضع رمضان والتحدي الإيماني" else "Ramadan Mode & Challenge",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                if (isArabicUi) {
                                                    "الإمساك ${schedule.imsak.formattedTime} • الإفطار ${schedule.maghrib.formattedTime}"
                                                } else {
                                                    "Suhoor ends ${schedule.imsak.formattedTime} • Iftar ${schedule.maghrib.formattedTime}"
                                                },
                                                style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                                color = Color(0xFFE2B95B)
                                            )
                                        }
                                        Switch(
                                            checked = settings.ramadanMode,
                                            onCheckedChange = { viewModel.updateSettings(mapOf("ramadanMode" to it)) }
                                        )
                                    }
                                }
                                if (settings.ramadanMode || hijriToday.month == 9) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "دعاء الإفطار: ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ",
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                        val tasks = if (isArabicUi) {
                                            listOf(
                                                "قراءة جزء أو الورد القرآني اليومي",
                                                "صدقة يومية أو تفطير صائم",
                                                "صلاة التراويح والقيام",
                                                "سحور مبارك مع تمر وماء"
                                            )
                                        } else {
                                            listOf(
                                                "1 Juz' / Wird Quran Reading",
                                                "Daily Sadaqah / Feeding someone",
                                                "Taraweeh & Qiyam Prayer",
                                                "Healthy Suhoor (Dates & Hydration)"
                                            )
                                        }
                                        tasks.forEachIndexed { i, task ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = ramadanChecklist[i],
                                                    onCheckedChange = { ramadanChecklist[i] = it }
                                                )
                                                Text(task, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            if (isArabicUi) "المناسبات الإسلامية القادمة" else "Upcoming Islamic Occasions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(occasions) { occ ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(occ.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(occ.virtueNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.width(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = if (occ.daysRemaining == 0) (if (isArabicUi) "اليوم" else "Today") else "${occ.daysRemaining}d",
                                        style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> item { KnowledgeHubContent(audioPlayer = audioPlayer, isArabicUi = isArabicUi) }
                2 -> item { NooraniaContent(audioPlayer = audioPlayer, isArabicUi = isArabicUi) }
                3 -> item {
                    AskAiAndKhutbahContent(
                        aiViewModel = aiViewModel,
                        appLanguage = appLanguage,
                        strings = strings,
                        isArabicUi = isArabicUi,
                        globalAiFlagEnabled = appOperations.aiAssistantEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun KnowledgeHubContent(audioPlayer: QuranTtsAudioPlayer, isArabicUi: Boolean) {
    var topicTab by remember { mutableIntStateOf(0) } // 0=Hadith, 1=Prophets, 2=NewMuslim, 3=Quiz
    var quizIdx by remember { mutableIntStateOf(0) }
    var quizScore by remember { mutableIntStateOf(0) }
    var quizExplanation by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val labels = if (isArabicUi) listOf("الأربعون النووية", "قصص الأنبياء", "المسلم الجديد", "اختبار المعرفة")
        else listOf("40 Hadith", "Prophets", "New Muslim", "Quiz")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(labels.size) { idx ->
                LiquidGlassPill(selected = topicTab == idx, onClick = { topicTab = idx }, label = labels[idx])
            }
        }

        when (topicTab) {
            0 -> {
                IslamicLibraryData.nawawiHadith.forEach { hadith ->
                    LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                        Text("حديث #${hadith.number} • ${hadith.title}", style = MaterialTheme.typography.titleMedium.withTabularNums(), fontWeight = FontWeight.Bold)
                        Text(hadith.arabic, fontFamily = AmiriFontFamily, fontSize = 21.sp, lineHeight = 36.sp)
                        Text(hadith.translation, style = MaterialTheme.typography.bodyLarge)
                        Text(hadith.takhrij, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(hadith.reflection, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            1 -> {
                IslamicLibraryData.prophetsStories.forEach { p ->
                    LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(p.arabicName, fontFamily = AmiriFontFamily, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(p.era, style = MaterialTheme.typography.labelLarge, color = BurnishedGold)
                        Text(p.summary, style = MaterialTheme.typography.bodyLarge)
                        Text("${p.keyLesson} (${p.quranRef})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LiquidGlassPill(
                            selected = false,
                            onClick = { audioPlayer.speak(p.name, "${p.name}. ${p.summary}. ${p.keyLesson}", preferArabic = false) },
                            icon = Icons.Rounded.PlayArrow,
                            label = if (isArabicUi) "استماع للقصة" else "Listen to Story"
                        )
                    }
                }
            }
            2 -> {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Text(if (isArabicUi) "مسار المسلم الجديد (٣٠ يوماً)" else "New Muslim 30-Day Path", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IslamicLibraryData.newMuslimPath.forEach { (step, desc) ->
                        Text(step, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(desc, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(color = LocalLiquidGlassPalette.current.glassBorder)
                    }
                }
            }
            3 -> {
                val questions = IslamicLibraryData.triviaQuiz
                val q = questions[quizIdx % questions.size]
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isArabicUi) "اختبار الثقافة الإسلامية" else "Islamic Knowledge Trivia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${if (isArabicUi) "النتيجة" else "Score"}: $quizScore", style = MaterialTheme.typography.titleMedium.withTabularNums(), color = MaterialTheme.colorScheme.primary)
                    }
                    Text(q.question, style = MaterialTheme.typography.titleMedium)
                    q.options.forEachIndexed { idx, opt ->
                        OutlinedButton(
                            onClick = {
                                if (idx == q.correctIndex) quizScore++
                                quizExplanation = q.explanation
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(opt)
                        }
                    }
                    if (quizExplanation != null) {
                        Text(quizExplanation!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { quizIdx = (quizIdx + 1) % questions.size; quizExplanation = null }) {
                            Text(if (isArabicUi) "السؤال التالي" else "Next Question")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NooraniaContent(audioPlayer: QuranTtsAudioPlayer, isArabicUi: Boolean) {
    val glass = LocalLiquidGlassPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (isArabicUi) "القاعدة النورانية • تأسيس النطق العربي" else "Al-Qaida Al-Noorania • Arabic Reading Foundations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (isArabicUi) "اضغط استماع لسماع مخرج كل حرف بالحركات." else "Tap listen to hear each letter & vowelized Harakat pronunciation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IslamicLibraryData.nooraniaLessons.forEach { lesson ->
            LiquidGlassCard(shape = RoundedCornerShape(22.dp), contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, glass.glassBorder),
                        modifier = Modifier.size(62.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = lesson.letter,
                                fontFamily = AmiriFontFamily,
                                fontSize = 36.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(lesson.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(lesson.forms, fontFamily = AmiriFontFamily, fontSize = 18.sp)
                        Text(lesson.harakatExamples, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text(lesson.pronunciationTip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        onClick = { audioPlayer.speak(lesson.name, lesson.letter, preferArabic = true) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Pronounce ${lesson.name}", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPulsingDotsIndicator(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "aiDots")
    val a1 by infinite.animateFloat(0.25f, 1f, infiniteRepeatable(tween(460), RepeatMode.Reverse), label = "dot1")
    val a2 by infinite.animateFloat(0.25f, 1f, infiniteRepeatable(tween(460, delayMillis = 150), RepeatMode.Reverse), label = "dot2")
    val a3 by infinite.animateFloat(0.25f, 1f, infiniteRepeatable(tween(460, delayMillis = 300), RepeatMode.Reverse), label = "dot3")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(a1, a2, a3).forEach { alphaVal ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alphaVal)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskAiAndKhutbahContent(
    aiViewModel: AiViewModel,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    isArabicUi: Boolean,
    globalAiFlagEnabled: Boolean
) {
    val glass = LocalLiquidGlassPalette.current
    val companionState by aiViewModel.companionState.collectAsStateWithLifecycle()
    val chatHistory by aiViewModel.chatHistoryState.collectAsStateWithLifecycle()
    val aiConfig by aiViewModel.aiRuntimeConfig.collectAsStateWithLifecycle()
    val effectiveAiEnabled = aiConfig.enabled && globalAiFlagEnabled
    val hifzQuizState by aiViewModel.hifzQuizState.collectAsStateWithLifecycle()
    val khutbahState by aiViewModel.khutbahState.collectAsStateWithLifecycle()

    var aiSubMode by remember { mutableIntStateOf(0) } // 0 = Tafsir & Q&A Chat, 1 = Hifz & Mutashabihat Coach
    var questionInput by remember { mutableStateOf("") }
    var hifzTopicInput by remember { mutableStateOf(if (isArabicUi) "جزء عمّ وسورة الكهف" else "Juz Amma & Surah Al-Kahf") }
    var selectedQuizOption by remember { mutableIntStateOf(-1) }
    var khutbahTopic by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LiquidGlassCard(
            shape = RoundedCornerShape(26.dp),
            containerColor = glass.glassFillStrong,
            contentPadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IslamicGeometricPatternCanvas(
                    modifier = Modifier.matchParentSize(),
                    tint = BurnishedGold,
                    alpha = 0.03f
                )
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, glass.glassBorder),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(MihrabIcons.AiSpark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(strings.aiAssistantTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${aiConfig.modelName} • ${if (isArabicUi) "محفوظ تلقائياً في سحابتك" else "History synced to cloud"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (aiSubMode == 0 && chatHistory.isNotEmpty()) {
                            IconButton(onClick = { aiViewModel.clearChatHistory() }) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = strings.aiClearHistory)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassPill(
                            selected = aiSubMode == 0,
                            onClick = { aiSubMode = 0 },
                            icon = Icons.Rounded.MenuBook,
                            label = strings.aiChatTab,
                            modifier = Modifier.testTag("ai_mode_chat")
                        )
                        LiquidGlassPill(
                            selected = aiSubMode == 1,
                            onClick = { aiSubMode = 1 },
                            icon = MihrabIcons.AiQuiz,
                            label = strings.aiQuizTab,
                            modifier = Modifier.testTag("ai_mode_quiz")
                        )
                    }

                    if (!effectiveAiEnabled) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.aiDisabledNotice,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (aiSubMode == 0) {
                        val starterPrompts = if (isArabicUi) {
                            listOf(
                                "اشرح معاني المفردات الغريبة في سورة العاديات",
                                "ما فضل أذكار الصباح والمساء؟",
                                "كيف أزيد الخشوع في الصلاة؟",
                                "أمثلة على المتشابهات في سورة البقرة وآل عمران"
                            )
                        } else {
                            listOf(
                                "Explain the rare words in Surah Al-Adiyat",
                                "Virtues of Morning & Evening Adhkar",
                                "How to build Khushu' in Salah?",
                                "Compare similar verses (Mutashabihat) in Al-Baqarah"
                            )
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            starterPrompts.forEach { starter ->
                                LiquidGlassPill(
                                    selected = false,
                                    onClick = { questionInput = starter },
                                    label = starter
                                )
                            }
                        }

                        // Frosted Glass Chat History Bubbles
                        if (chatHistory.isNotEmpty()) {
                            HorizontalDivider(color = glass.glassBorder)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                chatHistory.forEach { msg ->
                                    val isUser = msg.role == "user"
                                    Surface(
                                        color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        else glass.glassFill,
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else glass.glassBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isUser) {
                                                        if (isArabicUi) "سؤالك" else "You"
                                                    } else {
                                                        if (isArabicUi) "المساعد الإسلامي" else "Mihrab AI"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            if (!msg.surahRef.isNullOrBlank()) {
                                                // Embedded frosted Quran citation pill
                                                Surface(
                                                    color = BurnishedGold.copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.4f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.AutoStories, null, tint = BurnishedGold, modifier = Modifier.size(14.dp))
                                                        Text(msg.surahRef, style = MaterialTheme.typography.labelSmall, color = BurnishedGold, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(msg.content, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                        }

                        // Live Streaming Bubble with Pulsing Dots Indicator
                        when (val s = companionState) {
                            is AiUiState.Loading -> Surface(
                                color = glass.glassFill,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, glass.glassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AiPulsingDotsIndicator()
                                    Text(
                                        if (isArabicUi) "جارٍ استحضار الجواب الموثق..." else "Preparing authentic response...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            is AiUiState.Streaming -> Surface(
                                color = glass.glassFill,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AiPulsingDotsIndicator()
                                        Text(if (isArabicUi) "يكتب الآن..." else "Streaming...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(s.text, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            is AiUiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            else -> Unit
                        }

                        OutlinedTextField(
                            value = questionInput,
                            onValueChange = { questionInput = it },
                            label = {
                                Text(
                                    if (isArabicUi) "اسأل عن تفسير آية، معنى كلمة قرآنية، أو الأذكار..."
                                    else "Ask about Tafsir, Quranic vocabulary, Adhkar, or rulings..."
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("ai_question_input"),
                            minLines = 2
                        )
                        Button(
                            onClick = {
                                val q = questionInput
                                questionInput = ""
                                aiViewModel.askCompanion(q, appLanguage.nativeLabel)
                            },
                            enabled = effectiveAiEnabled && questionInput.isNotBlank() &&
                                companionState !is AiUiState.Loading && companionState !is AiUiState.Streaming,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_ask_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isArabicUi) "إرسال للمساعد الذكي" else "Ask Islamic AI")
                        }
                    } else {
                        Text(
                            text = if (isArabicUi) {
                                "اختبر حفظك للقرآن الكريم عبر إكمال الآيات مع تنبيهات دقيقة لمواضع المتشابهات."
                            } else {
                                "Practice Quran memorization with verse-completion challenges and Mutashabihat alerts."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = hifzTopicInput,
                            onValueChange = { hifzTopicInput = it },
                            label = { Text(if (isArabicUi) "السورة أو الجزء (مثال: سورة الكهف، جزء تبارك)" else "Surah or Juz scope") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                selectedQuizOption = -1
                                aiViewModel.generateHifzQuiz(hifzTopicInput, appLanguage.nativeLabel)
                            },
                            enabled = effectiveAiEnabled && hifzQuizState !is UiState.Loading,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_generate_quiz_button")
                        ) {
                            Icon(Icons.Rounded.Quiz, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isArabicUi) "توليد سؤال حفظ ومتشابهات" else "Generate Hifz & Mutashabihat Quiz")
                        }

                        when (val qs = hifzQuizState) {
                            is UiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AiPulsingDotsIndicator()
                                Text(if (isArabicUi) "جارٍ إعداد السؤال..." else "Generating memorization challenge...")
                            }
                            is UiState.Error -> Text(qs.message, color = MaterialTheme.colorScheme.error)
                            is UiState.Success -> qs.data?.let { quiz ->
                                LiquidGlassCard(shape = RoundedCornerShape(22.dp), borderColor = BurnishedGold.copy(alpha = 0.45f)) {
                                    Text(quiz.surahRef, style = MaterialTheme.typography.labelLarge, color = BurnishedGold)
                                    Text(
                                        text = quiz.promptVerse,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 23.sp,
                                        lineHeight = 40.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    quiz.options.forEachIndexed { idx, opt ->
                                        val isPicked = selectedQuizOption == idx
                                        val isCorrect = idx == quiz.correctIndex
                                        OutlinedButton(
                                            onClick = { selectedQuizOption = idx },
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = opt,
                                                    fontFamily = AmiriFontFamily,
                                                    fontSize = 18.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (selectedQuizOption >= 0 && isCorrect) {
                                                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                                } else if (isPicked && !isCorrect) {
                                                    Icon(Icons.Rounded.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }

                                    if (selectedQuizOption >= 0) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = if (isArabicUi) "تنبيه المتشابهات" else "Mutashabihat Alert",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(quiz.mutashabihatAlert, style = MaterialTheme.typography.bodyMedium)
                                                HorizontalDivider(color = glass.glassBorder)
                                                Text(quiz.explanation, style = MaterialTheme.typography.bodyMedium)
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

        // Friday Khutbah & Daily Wird Summarizer
        LiquidGlassCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(MihrabIcons.Guides, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (isArabicUi) "ملخّص خطبة الجمعة والورد اليومي" else "Friday Khutbah & Daily Wird Summarizer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedTextField(
                value = khutbahTopic,
                onValueChange = { khutbahTopic = it },
                label = {
                    Text(
                        if (isArabicUi) "اكتب موضوع الخطبة أو ملاحظاتك (مثل: الصبر والشكر)"
                        else "Enter Khutbah notes or topic (e.g., Patience & Gratitude)"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { aiViewModel.summarizeKhutbah(khutbahTopic, appLanguage.nativeLabel) },
                enabled = effectiveAiEnabled && khutbahTopic.isNotBlank() && khutbahState !is UiState.Loading,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (isArabicUi) "توليد ملخص عملي" else "Generate Actionable Summary")
            }

            when (val ks = khutbahState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(26.dp))
                is UiState.Error -> Text(ks.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> ks.data?.let { summary ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(summary.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            summary.keyPoints.forEach { pt -> Text("• $pt", style = MaterialTheme.typography.bodyMedium) }
                            Text(summary.weeklyAction, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(summary.reflectionVerse, style = MaterialTheme.typography.labelSmall, color = BurnishedGold)
                        }
                    }
                }
            }
        }
    }
}
