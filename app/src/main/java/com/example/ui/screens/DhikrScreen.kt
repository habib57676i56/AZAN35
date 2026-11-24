package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.hardware.shareDesignedVerseTextOrCard
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.domain.IslamicLibraryData
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BurnishedGold
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DhikrScreen(
    viewModel: MihrabViewModel,
    audioPlayer: QuranTtsAudioPlayer,
    currentUserId: String,
    strings: LocalizedStrings,
    useIndic: Boolean
) {
    val context = LocalContext.current
    val tasbihState by viewModel.tasbihState.collectAsStateWithLifecycle()
    val journalState by viewModel.journalState.collectAsStateWithLifecycle()
    val duaWallState by viewModel.duaWallState.collectAsStateWithLifecycle()

    var activeSection by remember { mutableIntStateOf(0) } // 0=Adhkar, 1=Tasbih, 2=99Names, 3=Journal/Wall
    var selectedCategory by remember { mutableStateOf("Morning") }
    val adhkarProgress = remember { mutableStateMapOf<String, Int>() }

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
                        strings.morningEveningAdhkar,
                        strings.digitalTasbih,
                        strings.namesOfAllah,
                        "${strings.duaJournal} & ${strings.communityDuaWall}"
                    ).forEachIndexed { idx, label ->
                        FilterChip(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            label = { Text(label) },
                            modifier = Modifier.testTag("dhikr_tab_$idx")
                        )
                    }
                }
            }

            when (activeSection) {
                0 -> {
                    val catItems = IslamicLibraryData.adhkarList.filter { it.category == selectedCategory }
                    val completedInCat = catItems.count { (adhkarProgress[it.id] ?: 0) >= it.targetCount }
                    val progressRatio = if (catItems.isEmpty()) 0f else completedInCat.toFloat() / catItems.size

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val primaryCol = MaterialTheme.colorScheme.primary
                                val trackCol = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = trackCol,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 10f)
                                        )
                                        drawArc(
                                            color = primaryCol,
                                            startAngle = -90f,
                                            sweepAngle = 360f * progressRatio,
                                            useCenter = false,
                                            style = Stroke(width = 10f, cap = StrokeCap.Round)
                                        )
                                    }
                                    Text(
                                        formatNumerals("$completedInCat/${catItems.size}", useIndic),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("Hisn al-Muslim • Daily Adhkar", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "Tap each dhikr card to count repetitions & verify authentic Hadith Takhrij.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            IslamicLibraryData.adhkarCategories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }

                    items(catItems, key = { it.id }) { dhikr ->
                        val doneCount = adhkarProgress[dhikr.id] ?: 0
                        val finished = doneCount >= dhikr.targetCount
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (finished) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = (doneCount + 1).coerceAtMost(dhikr.targetCount)
                                    adhkarProgress[dhikr.id] = next
                                    triggerHaptic(context)
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(dhikr.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Surface(
                                        color = if (finished) MaterialTheme.colorScheme.primary else BurnishedGold.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = formatNumerals("$doneCount/${dhikr.targetCount}", useIndic),
                                            color = if (finished) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = dhikr.arabic,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 23.sp,
                                    lineHeight = 38.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(dhikr.transliteration, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text(dhikr.translation, style = MaterialTheme.typography.bodyLarge)
                                Text(dhikr.virtueAndTakhrij, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        audioPlayer.speak(dhikr.title, dhikr.arabic, preferArabic = true)
                                    }) {
                                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Listen")
                                    }
                                    OutlinedButton(onClick = {
                                        shareDesignedVerseTextOrCard(context, dhikr.title, dhikr.arabic, dhikr.translation)
                                    }) {
                                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Hasanat Gift")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> item {
                    TasbihSection(
                        tasbihState = tasbihState,
                        useIndic = useIndic,
                        onSaveCounter = { id, label, arabic, count, target ->
                            viewModel.saveTasbihCounter(id, label, arabic, count, target)
                        }
                    )
                }

                2 -> item {
                    NamesOfAllahSection()
                }

                3 -> item {
                    JournalAndDuaWallSection(
                        journalState = journalState,
                        duaWallState = duaWallState,
                        currentUserId = currentUserId,
                        strings = strings,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasbihSection(
    tasbihState: UiState<List<com.example.data.firebase.TasbihCounterDoc>>,
    useIndic: Boolean,
    onSaveCounter: (String, String, String, Long, Long) -> Unit
) {
    val context = LocalContext.current
    val presets = listOf(
        Triple("subhanallah", "SubhanAllah", "سُبْحَانَ اللَّهِ"),
        Triple("alhamdulillah", "Alhamdulillah", "الْحَمْدُ لِلَّهِ"),
        Triple("allahuakbar", "Allahu Akbar", "اللَّهُ أَكْبَرُ"),
        Triple("istighfar", "Astaghfirullah (Daily Istighfar)", "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ"),
        Triple("salawat", "Salawat upon the Prophet ﷺ", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ")
    )

    var selectedPresetIdx by remember { mutableIntStateOf(0) }
    val activePreset = presets[selectedPresetIdx]
    val savedCounters = (tasbihState as? UiState.Success)?.data ?: emptyList()
    val cloudDoc = savedCounters.firstOrNull { it.id == activePreset.first }

    var localCount by remember(activePreset.first, cloudDoc?.count) {
        mutableIntStateOf((cloudDoc?.count ?: 0L).toInt())
    }
    var target by remember(activePreset.first, cloudDoc?.target) {
        mutableIntStateOf((cloudDoc?.target ?: 33L).toInt().coerceAtLeast(1))
    }
    var voiceListening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }

    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(recognizer) {
        onDispose { recognizer?.destroy() }
    }

    fun startVoiceTasbih() {
        if (recognizer == null) {
            voiceStatus = "Speech recognizer unavailable on this device; tap the Tasbih orb to count."
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { voiceListening = true; voiceStatus = "Say SubhanAllah / Alhamdulillah / Allahu Akbar..." }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { voiceListening = false }
            override fun onError(error: Int) {
                voiceListening = false
                voiceStatus = "Voice capture ended. Tap orb or press Start Voice again."
            }
            override fun onResults(results: Bundle?) {
                voiceListening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().lowercase()
                val matches = text.split(" ").count {
                    it.contains("subhan") || it.contains("hamd") || it.contains("akbar") || it.contains("allah")
                }.coerceAtLeast(if (text.isNotBlank()) 1 else 0)
                if (matches > 0) {
                    localCount += matches
                    triggerHaptic(context)
                    onSaveCounter(activePreset.first, activePreset.second, activePreset.third, localCount.toLong(), target.toLong())
                    voiceStatus = "Counted +$matches from voice (\"$text\")"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        recognizer.startListening(intent)
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceTasbih()
        else voiceStatus = "Microphone permission denied; tap the orb to count."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                presets.forEachIndexed { idx, item ->
                    FilterChip(
                        selected = selectedPresetIdx == idx,
                        onClick = { selectedPresetIdx = idx },
                        label = { Text(item.second.substringBefore(" (")) }
                    )
                }
            }

            Text(
                text = activePreset.third,
                fontFamily = AmiriFontFamily,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(activePreset.second, style = MaterialTheme.typography.titleMedium)

            val cycleProgress = (localCount % target).toFloat() / target.toFloat()
            val primaryCol = MaterialTheme.colorScheme.primary
            val trackCol = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

            Surface(
                onClick = {
                    localCount += 1
                    triggerHaptic(context)
                    onSaveCounter(activePreset.first, activePreset.second, activePreset.third, localCount.toLong(), target.toLong())
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(200.dp)
                    .testTag("tasbih_tap_orb")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        drawArc(color = trackCol, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 14f))
                        drawArc(
                            color = primaryCol,
                            startAngle = -90f,
                            sweepAngle = 360f * cycleProgress,
                            useCenter = false,
                            style = Stroke(width = 14f, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatNumerals(localCount.toString(), useIndic),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Goal ${formatNumerals(target.toString(), useIndic)} • Tap",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100, 1000).forEach { goal ->
                    FilterChip(
                        selected = target == goal,
                        onClick = {
                            target = goal
                            onSaveCounter(activePreset.first, activePreset.second, activePreset.third, localCount.toLong(), target.toLong())
                        },
                        label = { Text(formatNumerals(goal.toString(), useIndic)) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    if (voiceListening) {
                        recognizer?.stopListening()
                        voiceListening = false
                    } else {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (granted) startVoiceTasbih() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }) {
                    Icon(if (voiceListening) Icons.Rounded.Stop else Icons.Rounded.Mic, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (voiceListening) "Listening..." else "Voice Tasbih")
                }
                OutlinedButton(onClick = {
                    localCount = 0
                    onSaveCounter(activePreset.first, activePreset.second, activePreset.third, 0L, target.toLong())
                }) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }
            }

            if (voiceStatus != null) {
                Text(voiceStatus!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun NamesOfAllahSection() {
    var query by remember { mutableStateOf("") }
    var quizMode by remember { mutableStateOf(false) }
    var quizIndex by remember { mutableIntStateOf(0) }
    var quizScore by remember { mutableIntStateOf(0) }
    var quizFeedback by remember { mutableStateOf<String?>(null) }

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) IslamicLibraryData.ninetyNineNames
        else IslamicLibraryData.ninetyNineNames.filter {
            it.transliteration.lowercase().contains(q) || it.meaning.lowercase().contains(q) || it.arabic.contains(q)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Asma al-Husna (99 Names)", style = MaterialTheme.typography.titleLarge)
            FilterChip(
                selected = quizMode,
                onClick = { quizMode = !quizMode; quizFeedback = null },
                label = { Text(if (quizMode) "Browse List" else "Flashcard Quiz") }
            )
        }

        if (quizMode) {
            val all = IslamicLibraryData.ninetyNineNames
            val current = all[quizIndex % all.size]
            val distractors = remember(quizIndex) {
                val others = all.filter { it.number != current.number }.shuffled().take(3)
                (others + current).shuffled()
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Score: $quizScore • #${current.number}/99", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(current.arabic, fontFamily = AmiriFontFamily, fontSize = 38.sp)
                    Text(current.transliteration, style = MaterialTheme.typography.headlineMedium)
                    distractors.forEach { opt ->
                        OutlinedButton(
                            onClick = {
                                if (opt.number == current.number) {
                                    quizScore++
                                    quizFeedback = "Correct! ${current.transliteration} = ${current.meaning}"
                                    quizIndex = (quizIndex + 1) % all.size
                                } else {
                                    quizFeedback = "Reflect again — ${current.transliteration} means '${current.meaning}'."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(opt.meaning)
                        }
                    }
                    if (quizFeedback != null) {
                        Text(quizFeedback!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                label = { Text("Search 99 Names...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            filtered.forEach { name ->
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
                            Text("${name.number}. ${name.transliteration}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(name.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(name.arabic, fontFamily = AmiriFontFamily, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalAndDuaWallSection(
    journalState: UiState<List<com.example.data.firebase.JournalEntryDoc>>,
    duaWallState: UiState<List<com.example.data.firebase.DuaWallPostDoc>>,
    currentUserId: String,
    strings: LocalizedStrings,
    viewModel: MihrabViewModel
) {
    var subMode by remember { mutableIntStateOf(0) } // 0=Private Journal, 1=Community Dua Wall
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("DUA") }

    var wallAlias by remember { mutableStateOf("Anonymous Believer") }
    var wallCategory by remember { mutableStateOf("HEALING") }
    var wallRequest by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = subMode == 0, onClick = { subMode = 0 }, label = { Text("Private Journal") })
            FilterChip(selected = subMode == 1, onClick = { subMode = 1 }, label = { Text("Community Dua Wall") })
        }

        if (subMode == 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Private Dua & Gratitude Journal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("DUA" to "Personal Dua", "GRATITUDE" to "Gratitude", "VERSE_NOTE" to "Reflection").forEach { (k, lbl) ->
                            FilterChip(selected = entryType == k, onClick = { entryType = k }, label = { Text(lbl) })
                        }
                    }
                    OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodyInput, onValueChange = { bodyInput = it }, label = { Text("Write your private prayer or gratitude...") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            viewModel.addJournalEntry(entryType, titleInput, bodyInput, null)
                            titleInput = ""
                            bodyInput = ""
                        },
                        enabled = titleInput.isNotBlank() && bodyInput.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.save)
                    }
                }
            }

            when (journalState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(journalState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    journalState.data.forEach { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${entry.entryType} • ${entry.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.toggleJournalAnswered(entry) }) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = "Mark answered",
                                            tint = if (entry.answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteJournalEntry(entry.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete entry")
                                    }
                                }
                                Text(entry.body, style = MaterialTheme.typography.bodyMedium)
                                if (entry.answered) {
                                    Text("Alhamdulillah — Marked as Answered", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Anonymous Community Dua Wall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Share a prayer request anonymously. Your real identity is never shown.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("HEALING", "GUIDANCE", "SUSTENANCE", "FAMILY", "DECEASED", "GENERAL").forEach { cat ->
                            FilterChip(selected = wallCategory == cat, onClick = { wallCategory = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = wallAlias, onValueChange = { wallAlias = it }, label = { Text("Pen name (anonymous)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = wallRequest, onValueChange = { wallRequest = it }, label = { Text("Dua request (min 5 chars)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            viewModel.postDuaWall(wallAlias, wallCategory, wallRequest)
                            wallRequest = ""
                        },
                        enabled = wallRequest.trim().length >= 5
                    ) {
                        Text("Share on Dua Wall")
                    }
                }
            }

            when (duaWallState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(duaWallState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (duaWallState.data.isEmpty()) {
                        Text("No community requests yet. Be the first to ask your brothers and sisters for a Dua.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        duaWallState.data.forEach { post ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${post.alias} • ${post.category}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        if (post.authorUid == currentUserId) {
                                            IconButton(onClick = { viewModel.deleteMyDuaPost(post.id) }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Delete my post", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    Text(post.requestText, style = MaterialTheme.typography.bodyLarge)
                                    OutlinedButton(onClick = { viewModel.sayAmeen(post.id) }) {
                                        Icon(Icons.Rounded.Favorite, null, tint = BurnishedGold, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("${strings.ameen} (${post.ameenCount})")
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

@Suppress("DEPRECATION")
private fun triggerHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                v?.vibrate(25)
            }
        }
    } catch (_: Exception) {
    }
}
