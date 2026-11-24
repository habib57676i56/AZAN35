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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.hardware.shareDesignedVerseTextOrCard
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.domain.AdhkarCategoryMeta
import com.example.domain.IslamicLibraryData
import com.example.ui.icons.MihrabIcons
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
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    useIndic: Boolean
) {
    val context = LocalContext.current
    val isArabicUi = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU
    val tasbihState by viewModel.tasbihState.collectAsStateWithLifecycle()
    val journalState by viewModel.journalState.collectAsStateWithLifecycle()
    val duaWallState by viewModel.duaWallState.collectAsStateWithLifecycle()
    val adhkarCatalog by viewModel.adhkarCatalogState.collectAsStateWithLifecycle()
    val todayLogs by viewModel.todayAdhkarLogsState.collectAsStateWithLifecycle()

    var activeSection by remember { mutableIntStateOf(0) } // 0=Adhkar, 1=Tasbih, 2=99Names, 3=Journal/Wall
    var selectedCategory by remember { mutableStateOf(AdhkarCategoryMeta.MORNING) }
    var adhkarQuery by remember { mutableStateOf("") }
    val adhkarProgress = remember { mutableStateMapOf<String, Int>() }

    // Pre-hydrate completed indicator from today's Firestore adhkarLogs
    LaunchedEffect(todayLogs, adhkarCatalog) {
        todayLogs.forEach { log ->
            val itemsInCat = adhkarCatalog.filter { it.category == log.category }
            if (itemsInCat.isNotEmpty() && log.completedCount >= log.targetCount) {
                itemsInCat.forEach { item ->
                    if ((adhkarProgress[item.id] ?: 0) < item.targetCount) {
                        adhkarProgress[item.id] = item.targetCount
                    }
                }
            }
        }
    }

    val topTabs = listOf(
        Triple(strings.morningEveningAdhkar, MihrabIcons.iconForAdhkarCategory("MORNING"), 0),
        Triple(strings.digitalTasbih, MihrabIcons.TasbihBeads, 1),
        Triple(strings.namesOfAllah, Icons.Rounded.Stars, 2),
        Triple("${strings.duaJournal} & ${strings.communityDuaWall}", Icons.Rounded.EditNote, 3)
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topTabs.forEach { (label, icon, idx) ->
                        FilterChip(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("dhikr_tab_$idx")
                        )
                    }
                }
            }

            when (activeSection) {
                0 -> {
                    val catItems = adhkarCatalog.filter { item ->
                        val matchesCat = adhkarQuery.isNotBlank() || item.category == selectedCategory.id
                        val q = adhkarQuery.trim().lowercase()
                        val matchesQuery = q.isEmpty() ||
                            item.titleAr.contains(q) ||
                            item.titleEn.lowercase().contains(q) ||
                            item.arabic.contains(q) ||
                            item.translation.lowercase().contains(q)
                        matchesCat && matchesQuery
                    }

                    val activeCategoryItems = adhkarCatalog.filter { it.category == selectedCategory.id }
                    val completedInCat = activeCategoryItems.count { (adhkarProgress[it.id] ?: 0) >= it.targetCount }
                    val progressRatio = if (activeCategoryItems.isEmpty()) 0f else completedInCat.toFloat() / activeCategoryItems.size
                    val totalDailyCompleted = todayLogs.sumOf { it.completedCount.toInt() }
                    val totalDailyTarget = adhkarCatalog.size.coerceAtLeast(1)

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val primaryCol = MaterialTheme.colorScheme.primary
                                    val trackCol = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
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
                                            formatNumerals("$completedInCat/${activeCategoryItems.size}", useIndic),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = MihrabIcons.iconForAdhkarCategory(selectedCategory.id),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (isArabicUi) selectedCategory.titleAr else selectedCategory.titleEn,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = if (isArabicUi) {
                                                "حصن المسلم الشامل (${AdhkarCategoryMeta.entries.size} باباً) • اضغط على البطاقة للتسبيح والعد وحفظ الإنجاز اليومي."
                                            } else {
                                                "Comprehensive Hisn al-Muslim (${AdhkarCategoryMeta.entries.size} categories) • Tap cards to count & sync daily progress."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Daily Global Completion Summary
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(strings.dailyAdhkarStats, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = formatNumerals("$totalDailyCompleted / $totalDailyTarget", useIndic),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { (totalDailyCompleted.toFloat() / totalDailyTarget).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Search bar across all 12 Adhkar categories
                    item {
                        OutlinedTextField(
                            value = adhkarQuery,
                            onValueChange = { adhkarQuery = it },
                            leadingIcon = { Icon(MihrabIcons.Search, contentDescription = null) },
                            trailingIcon = {
                                if (adhkarQuery.isNotEmpty()) {
                                    IconButton(onClick = { adhkarQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = strings.cancel)
                                    }
                                }
                            },
                            label = {
                                Text(if (isArabicUi) "ابحث في جميع الأذكار والأدعية..." else "Search all 12 Adhkar categories...")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 12 Adhkar Categories with semantic icons
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AdhkarCategoryMeta.entries, key = { it.id }) { cat ->
                                val logForCat = todayLogs.firstOrNull { it.category == cat.id }
                                val catDone = logForCat != null && logForCat.completedCount >= logForCat.targetCount
                                FilterChip(
                                    selected = selectedCategory == cat && adhkarQuery.isBlank(),
                                    onClick = {
                                        adhkarQuery = ""
                                        selectedCategory = cat
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (catDone) Icons.Rounded.CheckCircle else MihrabIcons.iconForAdhkarCategory(cat.id),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (catDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    label = { Text(if (isArabicUi) cat.titleAr else cat.titleEn) },
                                    modifier = Modifier.testTag("adhkar_cat_${cat.id}")
                                )
                            }
                        }
                    }

                    items(catItems, key = { it.id }) { dhikr ->
                        val doneCount = adhkarProgress[dhikr.id] ?: 0
                        val finished = doneCount >= dhikr.targetCount
                        val catMeta = AdhkarCategoryMeta.fromId(dhikr.category)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (finished) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adhkar_card_${dhikr.id}")
                                .clickable {
                                    val next = (doneCount + 1).coerceAtMost(dhikr.targetCount)
                                    adhkarProgress[dhikr.id] = next
                                    triggerHaptic(context)
                                    val itemsForCat = adhkarCatalog.filter { it.category == dhikr.category }
                                    val doneInCat = itemsForCat.count { (adhkarProgress[it.id] ?: 0) >= it.targetCount }
                                    viewModel.logAdhkarProgress(dhikr.category, doneInCat, itemsForCat.size)
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = MihrabIcons.iconForAdhkarCategory(dhikr.category),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (isArabicUi) dhikr.titleAr else "${dhikr.titleEn} • ${catMeta.titleEn}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (doneCount > 0) {
                                            IconButton(
                                                onClick = {
                                                    adhkarProgress[dhikr.id] = 0
                                                    val itemsForCat = adhkarCatalog.filter { it.category == dhikr.category }
                                                    val doneInCat = itemsForCat.count { (adhkarProgress[it.id] ?: 0) >= it.targetCount }
                                                    viewModel.logAdhkarProgress(dhikr.category, doneInCat, itemsForCat.size)
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Refresh,
                                                    contentDescription = if (isArabicUi) "إعادة" else "Reset",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            color = if (finished) MaterialTheme.colorScheme.primary else BurnishedGold.copy(alpha = 0.22f),
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
                                }

                                Text(
                                    text = dhikr.arabic,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 23.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (!isArabicUi) {
                                    Text(dhikr.transliteration, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(dhikr.translation, style = MaterialTheme.typography.bodyLarge)
                                }
                                Text(
                                    text = if (isArabicUi) dhikr.referenceAr else dhikr.referenceEn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        audioPlayer.speak(dhikr.titleAr, dhikr.arabic, preferArabic = true)
                                    }) {
                                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isArabicUi) "استماع" else "Listen")
                                    }
                                    OutlinedButton(onClick = {
                                        shareDesignedVerseTextOrCard(
                                            context = context,
                                            surahLabel = if (isArabicUi) dhikr.titleAr else dhikr.titleEn,
                                            arabicText = dhikr.arabic,
                                            translation = if (isArabicUi) dhikr.referenceAr else dhikr.translation
                                        )
                                    }) {
                                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isArabicUi) "هدية حسنات" else "Hasanat Gift")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> item {
                    TasbihSection(
                        tasbihState = tasbihState,
                        isArabicUi = isArabicUi,
                        strings = strings,
                        useIndic = useIndic,
                        onSaveCounter = { id, label, arabic, count, target ->
                            viewModel.saveTasbihCounter(id, label, arabic, count, target)
                        }
                    )
                }

                2 -> item {
                    NamesOfAllahSection(isArabicUi = isArabicUi, useIndic = useIndic)
                }

                3 -> item {
                    JournalAndDuaWallSection(
                        journalState = journalState,
                        duaWallState = duaWallState,
                        currentUserId = currentUserId,
                        isArabicUi = isArabicUi,
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
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    useIndic: Boolean,
    onSaveCounter: (String, String, String, Long, Long) -> Unit
) {
    val context = LocalContext.current
    val basePresets = listOf(
        Triple("subhanallah", "SubhanAllah", "سُبْحَانَ اللَّهِ"),
        Triple("alhamdulillah", "Alhamdulillah", "الْحَمْدُ لِلَّهِ"),
        Triple("allahuakbar", "Allahu Akbar", "اللَّهُ أَكْبَرُ"),
        Triple("istighfar", "Astaghfirullah", "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ"),
        Triple("salawat", "Salawat ﷺ", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ")
    )

    val savedCounters = (tasbihState as? UiState.Success)?.data ?: emptyList()
    val customPresets = savedCounters
        .filter { doc -> basePresets.none { it.first == doc.id } }
        .map { doc -> Triple(doc.id, doc.label, doc.arabicText.ifBlank { doc.label }) }
    val allPresets = basePresets + customPresets

    var selectedId by remember { mutableStateOf("subhanallah") }
    val activePreset = allPresets.firstOrNull { it.first == selectedId } ?: allPresets.first()
    val cloudDoc = savedCounters.firstOrNull { it.id == activePreset.first }

    var localCount by remember(activePreset.first, cloudDoc?.count) {
        mutableIntStateOf((cloudDoc?.count ?: 0L).toInt())
    }
    var target by remember(activePreset.first, cloudDoc?.target) {
        mutableIntStateOf((cloudDoc?.target ?: 33L).toInt().coerceAtLeast(1))
    }
    var voiceListening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }

    var showCustomTargetDialog by remember { mutableStateOf(false) }
    var customTargetInput by remember { mutableStateOf(target.toString()) }
    var showNewDhikrDialog by remember { mutableStateOf(false) }
    var newDhikrLabel by remember { mutableStateOf("") }
    var newDhikrArabic by remember { mutableStateOf("") }

    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(recognizer) {
        onDispose { recognizer?.destroy() }
    }

    fun startVoiceTasbih() {
        if (recognizer == null) {
            voiceStatus = if (isArabicUi) {
                "التعرف الصوتي غير متوفر على هذا الجهاز؛ اضغط على السبحة للعد."
            } else {
                "Speech recognizer unavailable on this device; tap the Tasbih orb to count."
            }
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isArabicUi) "ar-SA" else "en-US")
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                voiceListening = true
                voiceStatus = if (isArabicUi) "انطق الذكر الآن..." else "Speak your Dhikr now..."
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { voiceListening = false }
            override fun onError(error: Int) {
                voiceListening = false
                voiceStatus = if (isArabicUi) "توقف الاستماع. اضغط السبحة أو أعد المحاولة." else "Voice capture ended. Tap orb or retry."
            }
            override fun onResults(results: Bundle?) {
                voiceListening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().lowercase()
                val matches = text.split(" ").count {
                    it.contains("subhan") || it.contains("hamd") || it.contains("akbar") || it.contains("allah") ||
                        it.contains("سبحان") || it.contains("الحمد") || it.contains("أكبر") || it.contains("أستغفر")
                }.coerceAtLeast(if (text.isNotBlank()) 1 else 0)
                if (matches > 0) {
                    localCount += matches
                    triggerHaptic(context)
                    onSaveCounter(activePreset.first, activePreset.second, activePreset.third, localCount.toLong(), target.toLong())
                    voiceStatus = if (isArabicUi) "تم احتساب +$matches (\"$text\")" else "Counted +$matches (\"$text\")"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        recognizer.startListening(intent)
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceTasbih()
        else voiceStatus = if (isArabicUi) "تم رفض إذن الميكروفون؛ استخدم اللمس للعد." else "Microphone permission denied; tap the orb to count."
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(MihrabIcons.TasbihBeads, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(strings.digitalTasbih, style = MaterialTheme.typography.titleLarge)
                }
                OutlinedButton(onClick = { showNewDhikrDialog = true }) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isArabicUi) "ذكر مخصص" else "Add Custom")
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                allPresets.forEach { item ->
                    FilterChip(
                        selected = selectedId == item.first,
                        onClick = { selectedId = item.first },
                        label = { Text(if (isArabicUi) item.third else item.second) }
                    )
                }
            }

            Text(
                text = activePreset.third,
                fontFamily = AmiriFontFamily,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            if (!isArabicUi) {
                Text(activePreset.second, style = MaterialTheme.typography.titleMedium)
            }

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
                    .size(204.dp)
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
                            text = if (isArabicUi) {
                                "الهدف ${formatNumerals(target.toString(), useIndic)} • اضغط"
                            } else {
                                "Goal ${formatNumerals(target.toString(), useIndic)} • Tap"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                FilterChip(
                    selected = target !in listOf(33, 99, 100, 1000),
                    onClick = {
                        customTargetInput = target.toString()
                        showCustomTargetDialog = true
                    },
                    label = { Text(strings.customTargetLabel) }
                )
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
                    Text(
                        if (voiceListening) {
                            if (isArabicUi) "جارٍ الاستماع..." else "Listening..."
                        } else {
                            if (isArabicUi) "تسبيح صوتي" else "Voice Tasbih"
                        }
                    )
                }
                OutlinedButton(onClick = {
                    localCount = 0
                    onSaveCounter(activePreset.first, activePreset.second, activePreset.third, 0L, target.toLong())
                }) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabicUi) "تصفير" else "Reset")
                }
            }

            if (voiceStatus != null) {
                Text(voiceStatus!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showCustomTargetDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTargetDialog = false },
            title = { Text(strings.customTargetLabel) },
            text = {
                OutlinedTextField(
                    value = customTargetInput,
                    onValueChange = { customTargetInput = it.filter { ch -> ch.isDigit() }.take(5) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(if (isArabicUi) "العدد المطلوب (١ - ١٠٠٠٠٠)" else "Target count (1 - 100000)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = (customTargetInput.toIntOrNull() ?: 33).coerceIn(1, 100_000)
                    target = parsed
                    onSaveCounter(activePreset.first, activePreset.second, activePreset.third, localCount.toLong(), target.toLong())
                    showCustomTargetDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTargetDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showNewDhikrDialog) {
        AlertDialog(
            onDismissRequest = { showNewDhikrDialog = false },
            title = { Text(if (isArabicUi) "إضافة ذكر مخصص" else "Create Custom Dhikr") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newDhikrLabel,
                        onValueChange = { newDhikrLabel = it },
                        label = { Text(if (isArabicUi) "العنوان (مثل: لا حول ولا قوة إلا بالله)" else "Short label") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newDhikrArabic,
                        onValueChange = { newDhikrArabic = it },
                        label = { Text(if (isArabicUi) "النص العربي" else "Arabic text") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = "custom_${System.currentTimeMillis()}"
                        val lbl = newDhikrLabel.trim().take(60)
                        val arb = newDhikrArabic.trim().ifBlank { lbl }.take(180)
                        onSaveCounter(id, lbl, arb, 0L, 33L)
                        selectedId = id
                        showNewDhikrDialog = false
                        newDhikrLabel = ""
                        newDhikrArabic = ""
                    },
                    enabled = newDhikrLabel.isNotBlank()
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showNewDhikrDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun NamesOfAllahSection(isArabicUi: Boolean, useIndic: Boolean) {
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
            Text(
                if (isArabicUi) "أسماء الله الحسنى (٩٩ اسماً)" else "Asma al-Husna (99 Names)",
                style = MaterialTheme.typography.titleLarge
            )
            FilterChip(
                selected = quizMode,
                onClick = { quizMode = !quizMode; quizFeedback = null },
                label = { Text(if (quizMode) (if (isArabicUi) "عرض القائمة" else "Browse List") else (if (isArabicUi) "اختبار" else "Flashcard Quiz")) }
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
                    Text(
                        formatNumerals("Score: $quizScore • #${current.number}/99", useIndic),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(current.arabic, fontFamily = AmiriFontFamily, fontSize = 38.sp)
                    Text(current.transliteration, style = MaterialTheme.typography.headlineMedium)
                    distractors.forEach { opt ->
                        OutlinedButton(
                            onClick = {
                                if (opt.number == current.number) {
                                    quizScore++
                                    quizFeedback = "${current.arabic} (${current.transliteration}) = ${current.meaning}"
                                    quizIndex = (quizIndex + 1) % all.size
                                } else {
                                    quizFeedback = "${current.transliteration} = '${current.meaning}'."
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
                label = { Text(if (isArabicUi) "ابحث في الأسماء الحسنى..." else "Search 99 Names...") },
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
                            Text("${formatNumerals(name.number.toString(), useIndic)}. ${name.transliteration}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    viewModel: MihrabViewModel
) {
    var subMode by remember { mutableIntStateOf(0) } // 0=Private Journal, 1=Community Dua Wall
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("DUA") }

    var wallAlias by remember { mutableStateOf(if (isArabicUi) "طالب دعاء" else "Anonymous Believer") }
    var wallCategory by remember { mutableStateOf("HEALING") }
    var wallRequest by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = subMode == 0, onClick = { subMode = 0 }, label = { Text(strings.duaJournal) })
            FilterChip(selected = subMode == 1, onClick = { subMode = 1 }, label = { Text(strings.communityDuaWall) })
        }

        if (subMode == 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.duaJournal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "DUA" to (if (isArabicUi) "دعاء خاص" else "Personal Dua"),
                            "GRATITUDE" to (if (isArabicUi) "شكر ونعمة" else "Gratitude"),
                            "VERSE_NOTE" to (if (isArabicUi) "تأمل" else "Reflection")
                        ).forEach { (k, lbl) ->
                            FilterChip(selected = entryType == k, onClick = { entryType = k }, label = { Text(lbl) })
                        }
                    }
                    OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text(if (isArabicUi) "العنوان" else "Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodyInput, onValueChange = { bodyInput = it }, label = { Text(if (isArabicUi) "اكتب دعاءك أو تأملك الخاص..." else "Write your private prayer or gratitude...") }, minLines = 2, modifier = Modifier.fillMaxWidth())
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
                                    Text(
                                        if (isArabicUi) "الحمد لله — تم استجابة الدعاء" else "Alhamdulillah — Marked as Answered",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.communityDuaWall, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (isArabicUi) "شارك طلب دعاء مجهول الهوية ليؤمّن عليه المسلمون." else "Share a prayer request anonymously. Your real identity is never shown.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("HEALING", "GUIDANCE", "SUSTENANCE", "FAMILY", "DECEASED", "GENERAL").forEach { cat ->
                            FilterChip(selected = wallCategory == cat, onClick = { wallCategory = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = wallAlias, onValueChange = { wallAlias = it }, label = { Text(if (isArabicUi) "الاسم المستعار" else "Pen name (anonymous)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = wallRequest, onValueChange = { wallRequest = it }, label = { Text(if (isArabicUi) "طلب الدعاء (٥ أحرف على الأقل)" else "Dua request (min 5 chars)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            viewModel.postDuaWall(wallAlias, wallCategory, wallRequest)
                            wallRequest = ""
                        },
                        enabled = wallRequest.trim().length >= 5
                    ) {
                        Text(if (isArabicUi) "مشاركة في حائط الدعاء" else "Share on Dua Wall")
                    }
                }
            }

            when (duaWallState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(duaWallState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (duaWallState.data.isEmpty()) {
                        Text(
                            if (isArabicUi) "لا توجد طلبات دعاء بعد. كن أول من يطلب الدعاء." else "No community requests yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
