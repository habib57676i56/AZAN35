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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.hardware.shareDesignedVerseTextOrCard
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.core.preferences.FeatureUsageTracker
import com.example.core.preferences.SmartFeatureId
import com.example.data.firebase.AppOperationsConfig
import com.example.domain.AdhkarCategoryMeta
import com.example.domain.IslamicLibraryData
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.IslamicGeometricPatternCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.LuminousMintEmerald
import com.example.ui.theme.SacredEaseOutExpo
import com.example.ui.theme.withTabularNums
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun DhikrScreen(
    viewModel: MihrabViewModel,
    audioPlayer: QuranTtsAudioPlayer,
    currentUserId: String,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    useIndic: Boolean,
    appOperations: AppOperationsConfig = AppOperationsConfig(),
    initialSubTab: Int = 0,
    subTabTriggerNonce: Int = 0
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassPalette.current
    val isArabicUi = appLanguage.isArabicUi
    val useArabicDigits = useIndic && isArabicUi
    val tasbihState by viewModel.tasbihState.collectAsStateWithLifecycle()
    val journalState by viewModel.journalState.collectAsStateWithLifecycle()
    val duaWallState by viewModel.duaWallState.collectAsStateWithLifecycle()
    val adhkarCatalog by viewModel.adhkarCatalogState.collectAsStateWithLifecycle()
    val todayLogs by viewModel.todayAdhkarLogsState.collectAsStateWithLifecycle()

    var activeSection by remember { mutableIntStateOf(initialSubTab) } // 0=Adhkar, 1=Tasbih, 2=99Names, 3=Journal/Wall
    LaunchedEffect(initialSubTab, subTabTriggerNonce) {
        activeSection = initialSubTab
    }
    LaunchedEffect(activeSection) {
        when (activeSection) {
            0 -> FeatureUsageTracker.recordUsage(context, SmartFeatureId.ADHKAR)
            1 -> FeatureUsageTracker.recordUsage(context, SmartFeatureId.TASBIH)
        }
    }

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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(topTabs, key = { it.third }) { (label, icon, idx) ->
                        LiquidGlassPill(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            label = label,
                            icon = icon,
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
                        LiquidGlassCard(
                            shape = RoundedCornerShape(26.dp),
                            containerColor = glass.glassFillStrong,
                            borderColor = glass.glassBorder,
                            contentPadding = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                IslamicGeometricPatternCanvas(
                                    modifier = Modifier.matchParentSize(),
                                    tint = MaterialTheme.colorScheme.primary,
                                    alpha = 0.035f
                                )
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        val primaryCol = MaterialTheme.colorScheme.primary
                                        val trackCol = glass.glassBorder
                                        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArc(
                                                    color = trackCol,
                                                    startAngle = 0f,
                                                    sweepAngle = 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = 11f)
                                                )
                                                drawArc(
                                                    brush = Brush.sweepGradient(listOf(primaryCol, BurnishedGold, primaryCol)),
                                                    startAngle = -90f,
                                                    sweepAngle = 360f * progressRatio,
                                                    useCenter = false,
                                                    style = Stroke(width = 11f, cap = StrokeCap.Round)
                                                )
                                            }
                                            Text(
                                                formatNumerals("$completedInCat/${activeCategoryItems.size}", useArabicDigits),
                                                style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = MihrabIcons.iconForAdhkarCategory(selectedCategory.id),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = selectedCategory.titleFor(appLanguage),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = appLanguage.pick(
                                                    "حصن المسلم الشامل (${AdhkarCategoryMeta.entries.size} باباً) • اضغط على البطاقة للتسبيح والعد وحفظ الإنجاز اليومي.",
                                                    "Comprehensive Hisn al-Muslim (${AdhkarCategoryMeta.entries.size} categories) • Tap cards to count & sync daily progress.",
                                                    "To'liq Hisnul Muslim (${AdhkarCategoryMeta.entries.size} bo'lim) • Sanash va kunlik natijani saqlash uchun kartani bosing."
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Daily Global Completion Summary
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(strings.dailyAdhkarStats, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = formatNumerals("$totalDailyCompleted / $totalDailyTarget", useArabicDigits),
                                                    style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { (totalDailyCompleted.toFloat() / totalDailyTarget).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(7.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search bar across all 12 Adhkar categories
                    item {
                        LiquidGlassCard(
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = 12.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                                    Text(
                                        appLanguage.pick(
                                            "ابحث في جميع الأذكار والأدعية...",
                                            "Search all 12 Adhkar categories...",
                                            "Barcha 12 zikr bo'limidan qidirish..."
                                        )
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
                                LiquidGlassPill(
                                    selected = selectedCategory == cat && adhkarQuery.isBlank(),
                                    onClick = {
                                        adhkarQuery = ""
                                        selectedCategory = cat
                                    },
                                    icon = if (catDone) Icons.Rounded.CheckCircle else MihrabIcons.iconForAdhkarCategory(cat.id),
                                    label = cat.titleFor(appLanguage),
                                    activeColor = if (catDone) LuminousMintEmerald else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("adhkar_cat_${cat.id}")
                                )
                            }
                        }
                    }

                    items(catItems, key = { it.id }) { dhikr ->
                        val doneCount = adhkarProgress[dhikr.id] ?: 0
                        val finished = doneCount >= dhikr.targetCount
                        val catMeta = AdhkarCategoryMeta.fromId(dhikr.category)
                        LiquidGlassCard(
                            shape = RoundedCornerShape(24.dp),
                            containerColor = if (finished) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else glass.glassFill,
                            borderColor = if (finished) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else glass.glassBorder,
                            onClick = {
                                val next = (doneCount + 1).coerceAtMost(dhikr.targetCount)
                                adhkarProgress[dhikr.id] = next
                                triggerHaptic(context)
                                val itemsForCat = adhkarCatalog.filter { it.category == dhikr.category }
                                val doneInCat = itemsForCat.count { (adhkarProgress[it.id] ?: 0) >= it.targetCount }
                                viewModel.logAdhkarProgress(dhikr.category, doneInCat, itemsForCat.size)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adhkar_card_${dhikr.id}")
                        ) {
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
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = MihrabIcons.iconForAdhkarItem(dhikr.iconName, dhikr.category),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isArabicUi) dhikr.titleAr else "${dhikr.titleEn} • ${catMeta.titleFor(appLanguage)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
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
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Refresh,
                                                contentDescription = appLanguage.pick("إعادة", "Reset", "Nolga tushirish"),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        color = if (finished) MaterialTheme.colorScheme.primary else BurnishedGold.copy(alpha = 0.22f),
                                        shape = CircleShape,
                                        border = BorderStroke(
                                            1.dp,
                                            if (finished) MaterialTheme.colorScheme.primary else BurnishedGold.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Text(
                                            text = formatNumerals("$doneCount/${dhikr.targetCount}", useArabicDigits),
                                            color = if (finished) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
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
                                LiquidGlassPill(
                                    selected = false,
                                    onClick = {
                                        audioPlayer.speak(dhikr.titleAr, dhikr.arabic, preferArabic = true)
                                    },
                                    icon = Icons.Rounded.PlayArrow,
                                    label = appLanguage.pick("استماع", "Listen", "Tinglash")
                                )
                                LiquidGlassPill(
                                    selected = false,
                                    onClick = {
                                        shareDesignedVerseTextOrCard(
                                            context = context,
                                            surahLabel = if (isArabicUi) dhikr.titleAr else dhikr.titleEn,
                                            arabicText = dhikr.arabic,
                                            translation = if (isArabicUi) dhikr.referenceAr else dhikr.translation
                                        )
                                    },
                                    icon = Icons.Rounded.Share,
                                    label = appLanguage.pick("هدية حسنات", "Hasanat Gift", "Savob hadyasi")
                                )
                            }
                        }
                    }
                }

                1 -> item {
                    TasbihSection(
                        tasbihState = tasbihState,
                        appLanguage = appLanguage,
                        strings = strings,
                        useArabicDigits = useArabicDigits,
                        onSaveCounter = { id, label, arabic, count, target ->
                            FeatureUsageTracker.recordUsage(context, SmartFeatureId.TASBIH)
                            viewModel.saveTasbihCounter(id, label, arabic, count, target)
                        }
                    )
                }

                2 -> item {
                    NamesOfAllahSection(appLanguage = appLanguage, useArabicDigits = useArabicDigits)
                }

                3 -> item {
                    JournalAndDuaWallSection(
                        journalState = journalState,
                        duaWallState = duaWallState,
                        currentUserId = currentUserId,
                        appLanguage = appLanguage,
                        strings = strings,
                        useArabicDigits = useArabicDigits,
                        communityDuaWallEnabled = appOperations.communityDuaWallEnabled,
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
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    useArabicDigits: Boolean,
    onSaveCounter: (String, String, String, Long, Long) -> Unit
) {
    val isArabicUi = appLanguage.isArabicUi
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalLiquidGlassPalette.current
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

    // Ripple animation state for the luminous glass Tasbih orb
    val rippleProgress = remember { Animatable(1f) }
    val orbInteraction = remember { MutableInteractionSource() }
    val orbPressed by orbInteraction.collectIsPressedAsState()
    val orbScale by animateFloatAsState(
        targetValue = if (orbPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tasbihOrbScale"
    )

    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(recognizer) {
        onDispose { recognizer?.destroy() }
    }

    fun startVoiceTasbih() {
        if (recognizer == null) {
            voiceStatus = appLanguage.pick(
                "التعرف الصوتي غير متوفر على هذا الجهاز؛ اضغط على السبحة للعد.",
                "Speech recognizer unavailable on this device; tap the Tasbih orb to count.",
                "Bu qurilmada ovozli tanish mavjud emas; sanash uchun tasbeh doirasini bosing."
            )
            return
        }
        val speechLang = when (appLanguage) {
            AppLanguage.ARABIC, AppLanguage.URDU -> "ar-SA"
            AppLanguage.UZBEK -> "uz-UZ"
            else -> "en-US"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLang)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                voiceListening = true
                voiceStatus = appLanguage.pick("انطق الذكر الآن...", "Speak your Dhikr now...", "Endi zikrni ayting...")
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { voiceListening = false }
            override fun onError(error: Int) {
                voiceListening = false
                voiceStatus = appLanguage.pick(
                    "توقف الاستماع. اضغط السبحة أو أعد المحاولة.",
                    "Voice capture ended. Tap orb or retry.",
                    "Tinglash to'xtadi. Doirani bosing yoki qayta urining."
                )
            }
            override fun onResults(results: Bundle?) {
                voiceListening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().lowercase()
                val matches = text.split(" ").count {
                    it.contains("subhan") || it.contains("hamd") || it.contains("akbar") || it.contains("allah") ||
                        it.contains("alloh") || it.contains("astag") ||
                        it.contains("سبحان") || it.contains("الحمد") || it.contains("أكبر") || it.contains("أستغفر")
                }.coerceAtLeast(if (text.isNotBlank()) 1 else 0)
                if (matches > 0) {
                    localCount += matches
                    triggerHaptic(context)
                    onSaveCounter(activePreset.first, activePreset.second, activePreset.third, localCount.toLong(), target.toLong())
                    voiceStatus = appLanguage.pick(
                        "تم احتساب +$matches (\"$text\")",
                        "Counted +$matches (\"$text\")",
                        "+$matches qo'shildi (\"$text\")"
                    )
                }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        recognizer.startListening(intent)
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceTasbih()
        else voiceStatus = appLanguage.pick(
            "تم رفض إذن الميكروفون؛ استخدم اللمس للعد.",
            "Microphone permission denied; tap the orb to count.",
            "Mikrofon ruxsati berilmadi; sanash uchun doirani bosing."
        )
    }

    LiquidGlassCard(
        shape = RoundedCornerShape(28.dp),
        containerColor = glass.glassFillStrong,
        borderColor = glass.glassBorder,
        contentPadding = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IslamicGeometricPatternCanvas(
                modifier = Modifier.matchParentSize(),
                tint = BurnishedGold,
                alpha = 0.035f
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(MihrabIcons.TasbihBeads, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.digitalTasbih, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    LiquidGlassPill(
                        selected = false,
                        onClick = { showNewDhikrDialog = true },
                        icon = Icons.Rounded.Add,
                        label = appLanguage.pick("ذكر مخصص", "Add Custom", "Maxsus zikr")
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allPresets, key = { it.first }) { item ->
                        LiquidGlassPill(
                            selected = selectedId == item.first,
                            onClick = { selectedId = item.first },
                            label = if (isArabicUi) item.third else item.second
                        )
                    }
                }

                Text(
                    text = activePreset.third,
                    fontFamily = AmiriFontFamily,
                    fontSize = 32.sp,
                    lineHeight = 46.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                if (!isArabicUi) {
                    Text(activePreset.second, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val cycleProgress = (localCount % target).toFloat() / target.toFloat()
                val primaryCol = MaterialTheme.colorScheme.primary
                val trackCol = glass.glassBorder

                // Centerpiece Luminous Interactive Glass & Gold Tasbih Orb
                Box(
                    modifier = Modifier.size(242.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Expanding radial pulse halo triggered on tap
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val rippleVal = rippleProgress.value
                        if (rippleVal < 0.99f) {
                            val radius = (size.minDimension * 0.38f) + (size.minDimension * 0.14f * rippleVal)
                            drawCircle(
                                color = BurnishedGold.copy(alpha = (1f - rippleVal) * 0.42f),
                                radius = radius,
                                style = Stroke(width = (1f - rippleVal) * 8f)
                            )
                        }
                        // Outer soft ambient halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryCol.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.5f
                            )
                        )
                    }

                    Surface(
                        onClick = {
                            localCount += 1
                            triggerHaptic(context)
                            scope.launch {
                                rippleProgress.snapTo(0f)
                                rippleProgress.animateTo(1f, animationSpec = tween(520, easing = SacredEaseOutExpo))
                            }
                            onSaveCounter(activePreset.first, activePreset.second, activePreset.third, localCount.toLong(), target.toLong())
                        },
                        interactionSource = orbInteraction,
                        shape = CircleShape,
                        color = glass.glassFillStrong,
                        shadowElevation = if (glass.isDark) 10.dp else 6.dp,
                        border = BorderStroke(1.5.dp, BurnishedGold.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .size(214.dp)
                            .graphicsLayer {
                                scaleX = orbScale
                                scaleY = orbScale
                            }
                            .testTag("tasbih_tap_orb")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            primaryCol.copy(alpha = 0.14f),
                                            BurnishedGold.copy(alpha = 0.06f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                drawArc(color = trackCol, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 14f))
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(primaryCol, BurnishedGold, primaryCol)),
                                    startAngle = -90f,
                                    sweepAngle = 360f * cycleProgress,
                                    useCenter = false,
                                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatNumerals(localCount.toString(), useArabicDigits),
                                    style = MaterialTheme.typography.displayLarge.withTabularNums(),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = appLanguage.pick(
                                        "الهدف ${formatNumerals(target.toString(), useArabicDigits)} • اضغط",
                                        "Goal ${formatNumerals(target.toString(), useArabicDigits)} • Tap",
                                        "Maqsad ${formatNumerals(target.toString(), useArabicDigits)} • Bosing"
                                    ),
                                    style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(33, 99, 100, 1000).forEach { goal ->
                        LiquidGlassPill(
                            selected = target == goal,
                            onClick = {
                                target = goal
                                onSaveCounter(activePreset.first, activePreset.second, activePreset.third, localCount.toLong(), target.toLong())
                            },
                            label = formatNumerals(goal.toString(), useArabicDigits)
                        )
                    }
                    LiquidGlassPill(
                        selected = target !in listOf(33, 99, 100, 1000),
                        onClick = {
                            customTargetInput = target.toString()
                            showCustomTargetDialog = true
                        },
                        label = strings.customTargetLabel
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (voiceListening) {
                                recognizer?.stopListening()
                                voiceListening = false
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) startVoiceTasbih() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(if (voiceListening) Icons.Rounded.Stop else Icons.Rounded.Mic, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (voiceListening) {
                                appLanguage.pick("جارٍ الاستماع...", "Listening...", "Tinglanmoqda...")
                            } else {
                                appLanguage.pick("تسبيح صوتي", "Voice Tasbih", "Ovozli tasbeh")
                            }
                        )
                    }
                    LiquidGlassPill(
                        selected = false,
                        onClick = {
                            localCount = 0
                            onSaveCounter(activePreset.first, activePreset.second, activePreset.third, 0L, target.toLong())
                        },
                        icon = Icons.Rounded.Refresh,
                        label = appLanguage.pick("تصفير", "Reset", "Nolga tushirish")
                    )
                }

                if (voiceStatus != null) {
                    Text(voiceStatus!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
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
                    label = {
                        Text(
                            appLanguage.pick(
                                "العدد المطلوب (١ - ١٠٠٠٠٠)",
                                "Target count (1 - 100000)",
                                "Maqsadli son (1 - 100000)"
                            )
                        )
                    },
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
            title = { Text(appLanguage.pick("إضافة ذكر مخصص", "Create Custom Dhikr", "Maxsus zikr qo'shish")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newDhikrLabel,
                        onValueChange = { newDhikrLabel = it },
                        label = {
                            Text(
                                appLanguage.pick(
                                    "العنوان (مثل: لا حول ولا قوة إلا بالله)",
                                    "Short label",
                                    "Qisqa nom"
                                )
                            )
                        },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newDhikrArabic,
                        onValueChange = { newDhikrArabic = it },
                        label = { Text(appLanguage.pick("النص العربي", "Arabic text", "Arabcha matn")) },
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
private fun NamesOfAllahSection(appLanguage: AppLanguage, useArabicDigits: Boolean) {
    val glass = LocalLiquidGlassPalette.current
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
                appLanguage.pick("أسماء الله الحسنى (٩٩ اسماً)", "Asma al-Husna (99 Names)", "Allohning 99 go'zal ismi"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            LiquidGlassPill(
                selected = quizMode,
                onClick = { quizMode = !quizMode; quizFeedback = null },
                icon = Icons.Rounded.Stars,
                label = if (quizMode) appLanguage.pick("عرض القائمة", "Browse List", "Ro'yxat")
                else appLanguage.pick("اختبار", "Flashcard Quiz", "Viktorina")
            )
        }

        if (quizMode) {
            val all = IslamicLibraryData.ninetyNineNames
            val current = all[quizIndex % all.size]
            val distractors = remember(quizIndex) {
                val others = all.filter { it.number != current.number }.shuffled().take(3)
                (others + current).shuffled()
            }
            LiquidGlassCard(
                shape = RoundedCornerShape(26.dp),
                containerColor = glass.glassFillStrong,
                borderColor = BurnishedGold.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        formatNumerals(
                            appLanguage.pick(
                                "النتيجة: $quizScore • #${current.number}/99",
                                "Score: $quizScore • #${current.number}/99",
                                "Ball: $quizScore • #${current.number}/99"
                            ),
                            useArabicDigits
                        ),
                        style = MaterialTheme.typography.labelLarge.withTabularNums(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(current.arabic, fontFamily = AmiriFontFamily, fontSize = 40.sp, color = MaterialTheme.colorScheme.primary)
                    Text(current.transliteration, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                            shape = RoundedCornerShape(18.dp),
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
            LiquidGlassCard(shape = RoundedCornerShape(22.dp), contentPadding = 12.dp) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    label = { Text(appLanguage.pick("ابحث في الأسماء الحسنى...", "Search 99 Names...", "99 ismdan qidirish...")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            filtered.forEach { name ->
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
                            Text(
                                "${formatNumerals(name.number.toString(), useArabicDigits)}. ${name.transliteration}",
                                style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                fontWeight = FontWeight.Bold
                            )
                            Text(name.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(name.arabic, fontFamily = AmiriFontFamily, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
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
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    useArabicDigits: Boolean,
    communityDuaWallEnabled: Boolean,
    viewModel: MihrabViewModel
) {
    val glass = LocalLiquidGlassPalette.current
    var subMode by remember { mutableIntStateOf(0) } // 0=Private Journal, 1=Community Dua Wall
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("DUA") }

    var wallAlias by remember {
        mutableStateOf(appLanguage.pick("طالب دعاء", "Anonymous Believer", "Duo so'rovchi"))
    }
    var wallCategory by remember { mutableStateOf("HEALING") }
    var wallRequest by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LiquidGlassPill(selected = subMode == 0, onClick = { subMode = 0 }, label = strings.duaJournal)
            if (communityDuaWallEnabled) {
                LiquidGlassPill(selected = subMode == 1, onClick = { subMode = 1 }, label = strings.communityDuaWall)
            }
        }

        if (subMode == 0 || !communityDuaWallEnabled) {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Text(strings.duaJournal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "DUA" to appLanguage.pick("دعاء خاص", "Personal Dua", "Shaxsiy duo"),
                        "GRATITUDE" to appLanguage.pick("شكر ونعمة", "Gratitude", "Shukrona"),
                        "VERSE_NOTE" to appLanguage.pick("تأمل", "Reflection", "Tafakkur")
                    ).forEach { (k, lbl) ->
                        LiquidGlassPill(selected = entryType == k, onClick = { entryType = k }, label = lbl)
                    }
                }
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(appLanguage.pick("العنوان", "Title", "Sarlavha")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bodyInput,
                    onValueChange = { bodyInput = it },
                    label = {
                        Text(
                            appLanguage.pick(
                                "اكتب دعاءك أو تأملك الخاص...",
                                "Write your private prayer or gratitude...",
                                "Shaxsiy duongiz yoki shukronangizni yozing..."
                            )
                        )
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.addJournalEntry(entryType, titleInput, bodyInput, null)
                        titleInput = ""
                        bodyInput = ""
                    },
                    enabled = titleInput.isNotBlank() && bodyInput.isNotBlank(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.save)
                }
            }

            when (journalState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(journalState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    journalState.data.forEach { entry ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(22.dp),
                            borderColor = if (entry.answered) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else glass.glassBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${entry.entryType} • ${entry.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.toggleJournalAnswered(entry) }) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = appLanguage.pick("تمت الاستجابة", "Mark answered", "Ijobat bo'ldi"),
                                        tint = if (entry.answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteJournalEntry(entry.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = appLanguage.pick("حذف", "Delete entry", "O'chirish"))
                                }
                            }
                            Text(entry.body, style = MaterialTheme.typography.bodyMedium)
                            if (entry.answered) {
                                Text(
                                    appLanguage.pick(
                                        "الحمد لله — تم استجابة الدعاء",
                                        "Alhamdulillah — Marked as Answered",
                                        "Alhamdulillah — Duo ijobat bo'ldi deb belgilandi"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Text(strings.communityDuaWall, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    appLanguage.pick(
                        "شارك طلب دعاء مجهول الهوية ليؤمّن عليه المسلمون.",
                        "Share a prayer request anonymously. Your real identity is never shown.",
                        "Anonim tarzda duo so'rang. Haqiqiy ismingiz ko'rsatilmaydi."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "HEALING" to appLanguage.pick("شفاء", "Healing", "Shifo"),
                        "GUIDANCE" to appLanguage.pick("هداية", "Guidance", "Hidoyat"),
                        "SUSTENANCE" to appLanguage.pick("رزق", "Sustenance", "Rizq"),
                        "FAMILY" to appLanguage.pick("عائلة", "Family", "Oila"),
                        "DECEASED" to appLanguage.pick("رحمة", "Deceased", "Marhumlar"),
                        "GENERAL" to appLanguage.pick("عام", "General", "Umumiy")
                    ).forEach { (cat, catLabel) ->
                        LiquidGlassPill(selected = wallCategory == cat, onClick = { wallCategory = cat }, label = catLabel)
                    }
                }
                OutlinedTextField(
                    value = wallAlias,
                    onValueChange = { wallAlias = it },
                    label = { Text(appLanguage.pick("الاسم المستعار", "Pen name (anonymous)", "Taxallus (anonim)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wallRequest,
                    onValueChange = { wallRequest = it },
                    label = {
                        Text(
                            appLanguage.pick(
                                "طلب الدعاء (٥ أحرف على الأقل)",
                                "Dua request (min 5 chars)",
                                "Duo matni (kamida 5 ta harf)"
                            )
                        )
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.postDuaWall(wallAlias, wallCategory, wallRequest)
                        wallRequest = ""
                    },
                    enabled = wallRequest.trim().length >= 5,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(appLanguage.pick("مشاركة في حائط الدعاء", "Share on Dua Wall", "Duo devorida ulashish"))
                }
            }

            when (duaWallState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(duaWallState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (duaWallState.data.isEmpty()) {
                        Text(
                            appLanguage.pick(
                                "لا توجد طلبات دعاء بعد. كن أول من يطلب الدعاء.",
                                "No community requests yet.",
                                "Hozircha duo so'rovlari yo'q. Birinchi bo'ling."
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        duaWallState.data.forEach { post ->
                            LiquidGlassCard(
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${post.alias} • ${post.category}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    if (post.authorUid == currentUserId) {
                                        IconButton(onClick = { viewModel.deleteMyDuaPost(post.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = appLanguage.pick("حذف", "Delete my post", "O'chirish"), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Text(post.requestText, style = MaterialTheme.typography.bodyLarge)
                                LiquidGlassPill(
                                    selected = false,
                                    onClick = { viewModel.sayAmeen(post.id) },
                                    icon = Icons.Rounded.Favorite,
                                    label = "${strings.ameen} (${formatNumerals(post.ameenCount.toString(), useArabicDigits)})"
                                )
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
