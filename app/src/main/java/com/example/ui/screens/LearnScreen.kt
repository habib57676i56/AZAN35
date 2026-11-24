package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.HijriCalendarEngine
import com.example.domain.IslamicLibraryData
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.SanctuaryEmerald
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
    initialSubTab: Int = 0
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val logsState by viewModel.prayerLogsState.collectAsStateWithLifecycle()
    val manualOffsets by viewModel.manualOffsets.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val logs = (logsState as? UiState.Success)?.data ?: emptyList()
    val todayLog = logs.firstOrNull { it.dateKey == viewModel.todayDateKey() }
    val hijriToday = remember(settings) { viewModel.computeHijriToday(settings) }
    val sunnahFast = remember(hijriToday) { HijriCalendarEngine.isSunnahFastingDay(Calendar.getInstance(), hijriToday) }
    val occasions = remember(hijriToday) { HijriCalendarEngine.upcomingOccasions(hijriToday) }
    val schedule = remember(settings, manualOffsets) { viewModel.computeSchedule(settings, manualOffsets) }

    var activeSection by remember(initialSubTab) { mutableIntStateOf(initialSubTab) } // 0=Calendar&Ramadan, 1=Knowledge, 2=Noorania, 3=AskAI

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "${strings.hijriCalendar} & ${strings.ramadanMode}",
                        strings.knowledgeHub,
                        "Al-Noorania",
                        strings.askAiCompanion
                    ).forEachIndexed { idx, label ->
                        FilterChip(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            label = { Text(label) },
                            modifier = Modifier.testTag("learn_tab_$idx")
                        )
                    }
                }
            }

            when (activeSection) {
                0 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(formatNumerals(hijriToday.formattedEn(), useIndic), style = MaterialTheme.typography.headlineMedium)
                                        Text(hijriToday.formattedAr(), fontFamily = AmiriFontFamily, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Adj:", style = MaterialTheme.typography.labelLarge)
                                        TextButton(onClick = {
                                            viewModel.updateSettings(mapOf("hijriOffset" to (settings.hijriOffset - 1L).coerceIn(-2L, 2L)))
                                        }) { Text("-1") }
                                        Text("${settings.hijriOffset}", style = MaterialTheme.typography.titleMedium)
                                        TextButton(onClick = {
                                            viewModel.updateSettings(mapOf("hijriOffset" to (settings.hijriOffset + 1L).coerceIn(-2L, 2L)))
                                        }) { Text("+1") }
                                    }
                                }

                                if (sunnahFast != null) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                                        Text(
                                            "Today's Fasting Virtue: $sunnahFast",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                Text("Today's Fasting Tracker", style = MaterialTheme.typography.titleMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("NONE" to "Not Fasting", "VOLUNTARY" to "Sunnah", "RAMADAN" to "Ramadan", "QADA" to "Makeup (Qada)").forEach { (key, label) ->
                                        FilterChip(
                                            selected = (todayLog?.fastingType ?: "NONE") == key,
                                            onClick = { viewModel.setFastingToday(key, todayLog) },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Dedicated Ramadan Mode Card
                    item {
                        val ramadanChecklist = remember { mutableStateListOf(false, false, false, false) }
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = SanctuaryEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_ramadan_lantern),
                                        contentDescription = "Ramadan Fanous Lantern",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(
                                            Brush.verticalGradient(listOf(Color.Transparent, SanctuaryEmerald.copy(alpha = 0.92f)))
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Ramadan Mode & Challenge", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                            Text(
                                                "Suhoor ends ${schedule.imsak.formattedTime} • Iftar ${schedule.maghrib.formattedTime}",
                                                style = MaterialTheme.typography.bodyMedium,
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
                                            "Iftar Dua: ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ",
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                        val tasks = listOf(
                                            "1 Juz' / Wird Quran Reading",
                                            "Daily Sadaqah / Feeding someone",
                                            "Taraweeh & Qiyam Prayer",
                                            "Healthy Suhoor (Dates & Hydration)"
                                        )
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

                    item { Text("Upcoming Islamic Occasions", style = MaterialTheme.typography.titleLarge) }
                    items(occasions) { occ ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(occ.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(occ.virtueNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                    Text(
                                        text = if (occ.daysRemaining == 0) "Today" else "${occ.daysRemaining}d",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> item { KnowledgeHubContent(audioPlayer = audioPlayer) }
                2 -> item { NooraniaContent(audioPlayer = audioPlayer) }
                3 -> item { AskAiAndKhutbahContent(aiViewModel = aiViewModel, appLanguage = appLanguage) }
            }
        }
    }
}

@Composable
private fun KnowledgeHubContent(audioPlayer: QuranTtsAudioPlayer) {
    var topicTab by remember { mutableIntStateOf(0) } // 0=Hadith, 1=Prophets, 2=NewMuslim, 3=Quiz
    var quizIdx by remember { mutableIntStateOf(0) }
    var quizScore by remember { mutableIntStateOf(0) }
    var quizExplanation by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("40 Hadith", "Prophets", "New Muslim", "Quiz").forEachIndexed { idx, lbl ->
                FilterChip(selected = topicTab == idx, onClick = { topicTab = idx }, label = { Text(lbl) })
            }
        }

        when (topicTab) {
            0 -> {
                IslamicLibraryData.nawawiHadith.forEach { hadith ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Hadith #${hadith.number} • ${hadith.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(hadith.arabic, fontFamily = AmiriFontFamily, fontSize = 21.sp, lineHeight = 34.sp)
                            Text(hadith.translation, style = MaterialTheme.typography.bodyLarge)
                            Text("Takhrij: ${hadith.takhrij}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text("Takeaway: ${hadith.reflection}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            1 -> {
                IslamicLibraryData.prophetsStories.forEach { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(p.arabicName, fontFamily = AmiriFontFamily, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(p.era, style = MaterialTheme.typography.labelLarge, color = BurnishedGold)
                            Text(p.summary, style = MaterialTheme.typography.bodyLarge)
                            Text("Lesson: ${p.keyLesson} (${p.quranRef})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = { audioPlayer.speak(p.name, "${p.name}. ${p.summary}. ${p.keyLesson}", preferArabic = false) }) {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Listen to Story")
                            }
                        }
                    }
                }
            }
            2 -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("New Muslim 30-Day Path", style = MaterialTheme.typography.titleLarge)
                        IslamicLibraryData.newMuslimPath.forEach { (step, desc) ->
                            Text(step, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(desc, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider()
                        }
                    }
                }
            }
            3 -> {
                val questions = IslamicLibraryData.triviaQuiz
                val q = questions[quizIdx % questions.size]
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Islamic Knowledge Trivia", style = MaterialTheme.typography.titleLarge)
                            Text("Score: $quizScore", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(q.question, style = MaterialTheme.typography.titleMedium)
                        q.options.forEachIndexed { idx, opt ->
                            OutlinedButton(
                                onClick = {
                                    if (idx == q.correctIndex) quizScore++
                                    quizExplanation = q.explanation
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(opt)
                            }
                        }
                        if (quizExplanation != null) {
                            Text(quizExplanation!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Button(onClick = { quizIdx = (quizIdx + 1) % questions.size; quizExplanation = null }) {
                                Text("Next Question")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NooraniaContent(audioPlayer: QuranTtsAudioPlayer) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Al-Qaida Al-Noorania • Arabic Reading Foundations", style = MaterialTheme.typography.titleLarge)
        Text("Tap listen to hear each letter & vowelized Harakat pronunciation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IslamicLibraryData.nooraniaLessons.forEach { lesson ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lesson.letter,
                        fontFamily = AmiriFontFamily,
                        fontSize = 42.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 14.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lesson.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Forms: ${lesson.forms}", fontFamily = AmiriFontFamily, fontSize = 18.sp)
                        Text(lesson.harakatExamples, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text(lesson.pronunciationTip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { audioPlayer.speak(lesson.name, lesson.letter, preferArabic = true) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Pronounce ${lesson.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AskAiAndKhutbahContent(
    aiViewModel: AiViewModel,
    appLanguage: AppLanguage
) {
    val companionState by aiViewModel.companionState.collectAsStateWithLifecycle()
    val khutbahState by aiViewModel.khutbahState.collectAsStateWithLifecycle()

    var questionInput by remember { mutableStateOf("") }
    var khutbahTopic by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Ask Scholarly AI Assistant", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Powered by Firebase AI Logic. Grounds answers in Quran & authentic Hadith while reminding you to consult qualified local scholars for formal Fatwas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it },
                    label = { Text("Ask about Quran Tafsir, Sunnah etiquette, or history...") },
                    modifier = Modifier.fillMaxWidth().testTag("ai_question_input"),
                    minLines = 2
                )
                Button(
                    onClick = { aiViewModel.askCompanion(questionInput, appLanguage.nativeLabel) },
                    enabled = questionInput.isNotBlank() && companionState !is AiUiState.Loading,
                    modifier = Modifier.testTag("ai_ask_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ask")
                }

                when (val s = companionState) {
                    is AiUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    is AiUiState.Streaming -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(s.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
                    }
                    is AiUiState.Success -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(s.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
                    }
                    is AiUiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    else -> Unit
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Friday Khutbah & Daily Wird Summarizer", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = khutbahTopic,
                    onValueChange = { khutbahTopic = it },
                    label = { Text("Enter Khutbah notes or topic (e.g., Patience & Gratitude)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { aiViewModel.summarizeKhutbah(khutbahTopic) },
                    enabled = khutbahTopic.isNotBlank() && khutbahState !is UiState.Loading
                ) {
                    Text("Generate Actionable Summary")
                }

                when (val ks = khutbahState) {
                    is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(26.dp))
                    is UiState.Error -> Text(ks.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> ks.data?.let { summary ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(summary.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                summary.keyPoints.forEach { pt -> Text("• $pt", style = MaterialTheme.typography.bodyMedium) }
                                Text("Weekly Action: ${summary.weeklyAction}", style = MaterialTheme.typography.labelLarge)
                                Text("Verse/Hadith: ${summary.reflectionVerse}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
