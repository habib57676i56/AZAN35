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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.KhatmahGroupDoc
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.QuranContentData
import com.example.domain.RecitationCheckResult
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlMusammiSection(
    aiViewModel: AiViewModel
) {
    val context = LocalContext.current
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
            micNotice = "Device speech recognizer unavailable on this emulator; type or paste Arabic below to test word checking."
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true; micNotice = "Listening in Arabic..." }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                micNotice = "Speech capture ended (code $error). You can also type or edit your recitation below."
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
        else micNotice = "Microphone permission denied; type your recitation attempt below."
    }

    val words = remember(ayah.arabic) {
        ayah.arabic.split(Regex("\\s+")).filter { it.isNotBlank() && it != "۝" }
    }
    val visibleThreshold = ((1f - maskFraction) * words.size).roundToInt().coerceAtLeast(hintRevealedWords)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Al-Musammi' Recitation Checker", style = MaterialTheme.typography.titleLarge)
                        Text("Progressive word masking & AI Tajweed coach", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Exam", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = examMode, onCheckedChange = {
                            examMode = it
                            if (it) maskFraction = 1f
                        })
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuranContentData.surahs.forEachIndexed { idx, s ->
                        FilterChip(
                            selected = selectedSurahIdx == idx,
                            onClick = {
                                selectedSurahIdx = idx
                                selectedAyahIdx = 0
                                diffResult = null
                                hintRevealedWords = 0
                                aiViewModel.resetRecitationCoach()
                            },
                            label = { Text(s.nameEn.substringBefore(" (")) }
                        )
                    }
                }

                if (surah.ayahs.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        surah.ayahs.forEachIndexed { idx, item ->
                            FilterChip(
                                selected = selectedAyahIdx == idx,
                                onClick = {
                                    selectedAyahIdx = idx
                                    diffResult = null
                                    hintRevealedWords = 0
                                },
                                label = { Text("Ayah ${item.number}") }
                            )
                        }
                    }
                }

                if (!examMode) {
                    Text("Progressive Page Masking: ${(maskFraction * 100).roundToInt()}% hidden", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = maskFraction,
                        onValueChange = { maskFraction = it },
                        valueRange = 0f..1f
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
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
                            Text("Hint Word")
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
                        Text(if (isListening) "Stop" else "Recite by Voice")
                    }
                }

                if (micNotice != null) {
                    Text(micNotice!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }

                OutlinedTextField(
                    value = spokenAttempt,
                    onValueChange = { spokenAttempt = it },
                    label = { Text("Recitation attempt (voice transcript or manual Arabic)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { diffResult = QuranContentData.compareRecitation(ayah.arabic, spokenAttempt) },
                        enabled = spokenAttempt.isNotBlank()
                    ) {
                        Text("Check Words")
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
                        Text("AI Tajweed Coach")
                    }
                }

                diffResult?.let { result ->
                    HorizontalDivider()
                    Text(
                        "Accuracy: ${result.accuracyPercent}% • ${result.missingCount} word(s) to review",
                        style = MaterialTheme.typography.titleMedium,
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
                    is AiUiState.Success -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(cs.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                    is AiUiState.Error -> Text(cs.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    else -> Unit
                }
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
    familyKhatmahEnabled: Boolean = true
) {
    var newGroupName by remember { mutableStateOf("") }
    var dailyPagesGoal by remember { mutableIntStateOf(20) }
    val pagesRead = settings.khatmahPagesRead.toInt().coerceIn(0, 604)
    val daysLeft = ((604 - pagesRead).coerceAtLeast(0) + dailyPagesGoal - 1) / dailyPagesGoal

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Personal Khatmah Planner", style = MaterialTheme.typography.titleLarge)
                Text(
                    "$pagesRead / 604 Mushaf Pages (${(pagesRead * 100) / 604}%) • Est. $daysLeft days remaining at $dailyPagesGoal pages/day",
                    style = MaterialTheme.typography.bodyMedium,
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
                        Text("+4 Pages (1 Wird)")
                    }
                    OutlinedButton(onClick = {
                        viewModel.updateSettings(mapOf("khatmahPagesRead" to (pagesRead + 20).coerceAtMost(604).toLong()))
                    }) {
                        Text("+20 Pages (1 Juz')")
                    }
                }
                Text("Daily Pace Goal: $dailyPagesGoal pages/day", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = dailyPagesGoal.toFloat(),
                    onValueChange = { dailyPagesGoal = it.roundToInt().coerceIn(4, 40) },
                    valueRange = 4f..40f
                )
            }
        }

        if (familyKhatmahEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Family & Friends Khatmah", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("New group title") },
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
                                "Create a Family Khatmah above and mark Juz' 1–30 together in real time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            groupsState.data.forEach { group ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("${group.completedJuz.size}/30 Juz • Code ${group.joinCode}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        }
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            (1..30).forEach { juz ->
                                                val done = juz.toLong() in group.completedJuz
                                                FilterChip(
                                                    selected = done,
                                                    onClick = { viewModel.toggleGroupJuz(group, juz) },
                                                    label = { Text("J$juz") },
                                                    leadingIcon = if (done) {
                                                        { Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(14.dp)) }
                                                    } else null
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
}
}
