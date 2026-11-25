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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import com.example.core.preferences.FeatureUsageTracker
import com.example.core.preferences.SmartFeatureId
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.UserSettingsDoc
import com.example.data.firebase.getAppCheckDebugSecret
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
    subTabTriggerNonce: Int = 0,
    prefilledAiQuestion: String? = null,
    onConsumePrefilledQuestion: () -> Unit = {},
    appOperations: AppOperationsConfig = AppOperationsConfig()
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassPalette.current
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val logsState by viewModel.prayerLogsState.collectAsStateWithLifecycle()
    val manualOffsets by viewModel.manualOffsets.collectAsStateWithLifecycle()

    val isArabicUi = appLanguage.isArabicUi
    val useArabicDigits = useIndic && isArabicUi
    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val logs = (logsState as? UiState.Success)?.data ?: emptyList()
    val todayLog = logs.firstOrNull { it.dateKey == viewModel.todayDateKey() }
    val hijriToday = remember(settings) { viewModel.computeHijriToday(settings) }
    val sunnahFast = remember(hijriToday, appLanguage) {
        HijriCalendarEngine.isSunnahFastingDay(Calendar.getInstance(), hijriToday, appLanguage)
    }
    val occasions = remember(hijriToday, appLanguage) {
        HijriCalendarEngine.upcomingOccasions(hijriToday, appLanguage)
    }
    val whiteDaysCountdown = remember(hijriToday, settings.hijriOffset, appLanguage) {
        HijriCalendarEngine.computeWhiteDaysCountdown(Calendar.getInstance(), settings.hijriOffset.toInt(), appLanguage)
    }
    val allMonthsWhiteDays = remember(hijriToday, settings.hijriOffset, appLanguage) {
        HijriCalendarEngine.allMonthsWhiteDaysSchedule(Calendar.getInstance(), settings.hijriOffset.toInt(), appLanguage)
    }
    val schedule = remember(settings, manualOffsets) { viewModel.computeSchedule(settings, manualOffsets) }

    var activeSection by remember { mutableIntStateOf(initialSubTab) } // 0=Calendar&Ramadan, 1=Knowledge, 2=Noorania, 3=AskAI
    LaunchedEffect(initialSubTab, subTabTriggerNonce) {
        activeSection = initialSubTab
    }
    LaunchedEffect(prefilledAiQuestion) {
        if (!prefilledAiQuestion.isNullOrBlank()) {
            activeSection = 3
        }
    }
    LaunchedEffect(activeSection) {
        when (activeSection) {
            0 -> FeatureUsageTracker.recordUsage(context, SmartFeatureId.HIJRI)
            3 -> FeatureUsageTracker.recordUsage(context, SmartFeatureId.AI_COMPANION)
        }
    }

    val learnTabs = listOf(
        Triple("${strings.hijriCalendar} & ${strings.ramadanMode}", Icons.Rounded.CalendarMonth, 0),
        Triple(strings.knowledgeHub, MihrabIcons.ScholarBook, 1),
        Triple(appLanguage.pick("القاعدة النورانية", "Al-Noorania", "Al-Nuroniyya"), Icons.Rounded.RecordVoiceOver, 2),
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
                                            Text(
                                                formatNumerals(hijriToday.formattedFor(appLanguage), useArabicDigits),
                                                style = MaterialTheme.typography.headlineMedium.withTabularNums(),
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (!isArabicUi) {
                                                Text(hijriToday.formattedAr(), fontFamily = AmiriFontFamily, fontSize = 21.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(appLanguage.pick("تعديل:", "Adj:", "Tuzatish:"), style = MaterialTheme.typography.labelLarge)
                                            TextButton(onClick = {
                                                viewModel.updateSettings(mapOf("hijriOffset" to (settings.hijriOffset - 1L).coerceIn(-2L, 2L)))
                                            }) { Text(formatNumerals("-1", useArabicDigits)) }
                                            Text(formatNumerals("${settings.hijriOffset}", useArabicDigits), style = MaterialTheme.typography.titleMedium.withTabularNums(), fontWeight = FontWeight.Bold)
                                            TextButton(onClick = {
                                                viewModel.updateSettings(mapOf("hijriOffset" to (settings.hijriOffset + 1L).coerceIn(-2L, 2L)))
                                            }) { Text(formatNumerals("+1", useArabicDigits)) }
                                        }
                                    }

                                    if (sunnahFast != null) {
                                        Surface(
                                            color = BurnishedGold.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = appLanguage.pick(
                                                    "صيام مستحب اليوم: $sunnahFast",
                                                    "Today's Fasting Virtue: $sunnahFast",
                                                    "Bugungi ro'za fazilati: $sunnahFast"
                                                ),
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        appLanguage.pick("متابعة الصيام اليومي", "Today's Fasting Tracker", "Bugungi ro'za kuzatuvi"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            "NONE" to appLanguage.pick("غير صائم", "Not Fasting", "Ro'za emas"),
                                            "VOLUNTARY" to appLanguage.pick("صيام سنة", "Sunnah", "Nafl / Sunnat"),
                                            "RAMADAN" to appLanguage.pick("رمضان", "Ramadan", "Ramazon"),
                                            "QADA" to appLanguage.pick("قضاء", "Qada", "Qazo")
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
                                                appLanguage.pick(
                                                    "وضع رمضان والتحدي الإيماني",
                                                    "Ramadan Mode & Challenge",
                                                    "Ramazon rejimi va kundalik maqsadlar"
                                                ),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                formatNumerals(
                                                    appLanguage.pick(
                                                        "الإمساك ${schedule.imsak.formattedTime} • الإفطار ${schedule.maghrib.formattedTime}",
                                                        "Suhoor ends ${schedule.imsak.formattedTime} • Iftar ${schedule.maghrib.formattedTime}",
                                                        "Saharlik ${schedule.imsak.formattedTime} • Iftorlik ${schedule.maghrib.formattedTime}"
                                                    ),
                                                    useArabicDigits
                                                ),
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
                                        val tasks = when {
                                            isArabicUi -> listOf(
                                                "قراءة جزء أو الورد القرآني اليومي",
                                                "صدقة يومية أو تفطير صائم",
                                                "صلاة التراويح والقيام",
                                                "سحور مبارك مع تمر وماء"
                                            )
                                            appLanguage == AppLanguage.UZBEK -> listOf(
                                                "Kundalik 1 juz Qur'on tilovati",
                                                "Kunlik sadaqa / ro'zadorga iftorlik",
                                                "Taroveh va tungi namoz",
                                                "Barakali saharlik (xurmo va suv)"
                                            )
                                            else -> listOf(
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

                    // All 12 Hijri Months White Days (13, 14, 15) Countdown & Schedule
                    item {
                        LiquidGlassCard(
                            shape = RoundedCornerShape(26.dp),
                            containerColor = glass.glassFillStrong,
                            borderColor = BurnishedGold.copy(alpha = 0.48f),
                            contentPadding = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("learn_white_days_card")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = BurnishedGold)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appLanguage.pick(
                                            "الأيام البيض (١٣، ١٤، ١٥) لجميع الأشهر الهجرية",
                                            "White Days (13, 14, 15) Across All 12 Months",
                                            "Barcha 12 hijriy oylar uchun Oq kunlar (13, 14, 15)"
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatNumerals(
                                            if (whiteDaysCountdown.isTodayWhiteDay) {
                                                appLanguage.pick(
                                                    "اليوم ${whiteDaysCountdown.currentWhiteDayNumber ?: 13} من الأيام البيض في شهر ${whiteDaysCountdown.monthNameFor(appLanguage)} • ${whiteDaysCountdown.gregorianRange}",
                                                    "Today is White Day ${whiteDaysCountdown.currentWhiteDayNumber ?: 13} (${whiteDaysCountdown.monthNameFor(appLanguage)}) • ${whiteDaysCountdown.gregorianRange}",
                                                    "Bugun ${whiteDaysCountdown.monthNameFor(appLanguage)} oyi Oq kuni (${whiteDaysCountdown.currentWhiteDayNumber ?: 13}) • ${whiteDaysCountdown.gregorianRange}"
                                                )
                                            } else {
                                                appLanguage.pick(
                                                    "الأيام البيض القادمة (${whiteDaysCountdown.monthNameFor(appLanguage)}): متبقي ${whiteDaysCountdown.daysRemaining} أيام • ${whiteDaysCountdown.gregorianRange}",
                                                    "Next White Days (${whiteDaysCountdown.monthNameFor(appLanguage)}): ${whiteDaysCountdown.daysRemaining} days left • ${whiteDaysCountdown.gregorianRange}",
                                                    "Keyingi Oq kunlar (${whiteDaysCountdown.monthNameFor(appLanguage)}): ${whiteDaysCountdown.daysRemaining} kun qoldi • ${whiteDaysCountdown.gregorianRange}"
                                                )
                                            },
                                            useArabicDigits
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                allMonthsWhiteDays.forEach { monthItem ->
                                    val badgeText = when {
                                        monthItem.isActiveNow -> appLanguage.pick("جارية الآن", "Active Now", "Hozir davom etmoqda")
                                        monthItem.alreadyPassedThisMonth -> appLanguage.pick("مضت", "Passed", "O'tdi")
                                        monthItem.daysUntilStart == 0 -> appLanguage.pick("تبدأ اليوم", "Starts Today", "Bugun boshlanadi")
                                        else -> formatNumerals(
                                            appLanguage.pick(
                                                "متبقي ${monthItem.daysUntilStart} يوم",
                                                "In ${monthItem.daysUntilStart}d",
                                                "${monthItem.daysUntilStart} kun"
                                            ),
                                            useArabicDigits
                                        )
                                    }
                                    Surface(
                                        color = if (monthItem.isActiveNow) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                        } else {
                                            glass.glassSubtleFill
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (monthItem.isActiveNow) BurnishedGold else glass.glassBorder
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = formatNumerals(
                                                        appLanguage.pick(
                                                            "١٣–١٥ ${monthItem.monthNameFor(appLanguage)} ${monthItem.hijriYear} هـ",
                                                            "13–15 ${monthItem.monthNameFor(appLanguage)} ${monthItem.hijriYear} AH",
                                                            "${monthItem.monthNameFor(appLanguage)} 13–15, ${monthItem.hijriYear}"
                                                        ),
                                                        useArabicDigits
                                                    ),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = formatNumerals(monthItem.gregorianRange, useArabicDigits),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                color = when {
                                                    monthItem.isActiveNow -> MaterialTheme.colorScheme.primary
                                                    monthItem.alreadyPassedThisMonth -> MaterialTheme.colorScheme.surfaceVariant
                                                    else -> BurnishedGold.copy(alpha = 0.16f)
                                                },
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = badgeText,
                                                    style = MaterialTheme.typography.labelMedium.withTabularNums(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        monthItem.isActiveNow -> MaterialTheme.colorScheme.onPrimary
                                                        monthItem.alreadyPassedThisMonth -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        else -> MaterialTheme.colorScheme.primary
                                                    },
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            appLanguage.pick(
                                "المناسبات الإسلامية القادمة",
                                "Upcoming Islamic Occasions",
                                "Yaqinlashib kelayotgan islomiy kunlar"
                            ),
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
                                        text = if (occ.daysRemaining == 0) {
                                            appLanguage.pick("اليوم", "Today", "Bugun")
                                        } else {
                                            formatNumerals(
                                                appLanguage.pick("${occ.daysRemaining} ي", "${occ.daysRemaining}d", "${occ.daysRemaining} kun"),
                                                useArabicDigits
                                            )
                                        },
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

                1 -> item { KnowledgeHubContent(audioPlayer = audioPlayer, appLanguage = appLanguage) }
                2 -> item { NooraniaContent(audioPlayer = audioPlayer, appLanguage = appLanguage) }
                3 -> item {
                    AskAiAndKhutbahContent(
                        aiViewModel = aiViewModel,
                        appLanguage = appLanguage,
                        strings = strings,
                        prefilledAiQuestion = prefilledAiQuestion,
                        onConsumePrefilledQuestion = onConsumePrefilledQuestion,
                        globalAiFlagEnabled = appOperations.aiAssistantEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun KnowledgeHubContent(audioPlayer: QuranTtsAudioPlayer, appLanguage: AppLanguage) {
    var topicTab by remember { mutableIntStateOf(0) } // 0=Hadith, 1=Prophets, 2=NewMuslim, 3=Quiz
    var quizIdx by remember { mutableIntStateOf(0) }
    var quizScore by remember { mutableIntStateOf(0) }
    var quizExplanation by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val labels = when {
            appLanguage.isArabicUi -> listOf("الأربعون النووية", "قصص الأنبياء", "المسلم الجديد", "اختبار المعرفة")
            appLanguage == AppLanguage.UZBEK -> listOf("40 Hadis", "Payg'ambarlar", "Yangi musulmon", "Viktorina")
            else -> listOf("40 Hadith", "Prophets", "New Muslim", "Quiz")
        }
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
                            label = appLanguage.pick("استماع للقصة", "Listen to Story", "Qissani tinglash")
                        )
                    }
                }
            }
            2 -> {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Text(
                        appLanguage.pick("مسار المسلم الجديد (٣٠ يوماً)", "New Muslim 30-Day Path", "Yangi musulmon uchun 30 kunlik yo'l"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                        Text(
                            appLanguage.pick("اختبار الثقافة الإسلامية", "Islamic Knowledge Trivia", "Islomiy bilimlar viktorinasi"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${appLanguage.pick("النتيجة", "Score", "Ball")}: $quizScore",
                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                            color = MaterialTheme.colorScheme.primary
                        )
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
                            Text(appLanguage.pick("السؤال التالي", "Next Question", "Keyingi savol"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NooraniaContent(audioPlayer: QuranTtsAudioPlayer, appLanguage: AppLanguage) {
    val glass = LocalLiquidGlassPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            appLanguage.pick(
                "القاعدة النورانية • تأسيس النطق العربي",
                "Al-Qaida Al-Noorania • Arabic Reading Foundations",
                "Al-Qoida An-Nuroniyya • Arabcha o'qish asoslari"
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            appLanguage.pick(
                "اضغط استماع لسماع مخرج كل حرف بالحركات.",
                "Tap listen to hear each letter & vowelized Harakat pronunciation.",
                "Har bir harf va harakat talaffuzini eshitish uchun tinglash tugmasini bosing."
            ),
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
    prefilledAiQuestion: String?,
    onConsumePrefilledQuestion: () -> Unit,
    globalAiFlagEnabled: Boolean
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassPalette.current
    val companionState by aiViewModel.companionState.collectAsStateWithLifecycle()
    val chatHistory by aiViewModel.chatHistoryState.collectAsStateWithLifecycle()
    val aiConfig by aiViewModel.aiRuntimeConfig.collectAsStateWithLifecycle()
    val effectiveAiEnabled = aiConfig.enabled && globalAiFlagEnabled
    val hifzQuizState by aiViewModel.hifzQuizState.collectAsStateWithLifecycle()
    val khutbahState by aiViewModel.khutbahState.collectAsStateWithLifecycle()

    var aiSubMode by remember { mutableIntStateOf(0) } // 0 = Tafsir & Q&A Chat, 1 = Hifz & Mutashabihat Coach
    var questionInput by remember { mutableStateOf("") }
    var hifzTopicInput by remember(appLanguage) {
        mutableStateOf(
            appLanguage.pick(
                "جزء عمّ وسورة الكهف",
                "Juz Amma & Surah Al-Kahf",
                "Amma porasi va Kahf surasi"
            )
        )
    }
    var selectedQuizOption by remember { mutableIntStateOf(-1) }
    var khutbahTopic by remember { mutableStateOf("") }

    LaunchedEffect(prefilledAiQuestion) {
        if (!prefilledAiQuestion.isNullOrBlank()) {
            aiSubMode = 0
            questionInput = prefilledAiQuestion
            onConsumePrefilledQuestion()
        }
    }

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
                                    text = "${aiConfig.modelName} • ${
                                        appLanguage.pick(
                                            "محفوظ تلقائياً في سحابتك",
                                            "History synced to cloud",
                                            "Tarix bulutda saqlanadi"
                                        )
                                    }",
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
                        val starterPrompts = when {
                            appLanguage.isArabicUi -> listOf(
                                "اشرح معاني المفردات الغريبة في سورة العاديات",
                                "ما فضل أذكار الصباح والمساء؟",
                                "كيف أزيد الخشوع في الصلاة؟",
                                "أمثلة على المتشابهات في سورة البقرة وآل عمران"
                            )
                            appLanguage == AppLanguage.UZBEK -> listOf(
                                "Odiyot surasidagi noyob so'zlar ma'nosini tushuntiring",
                                "Tonggi va kechki zikrlarning fazilati qanday?",
                                "Namozda xushu'ni qanday oshirish mumkin?",
                                "Baqara va Oli Imron suralaridagi mutashobih oyatlar"
                            )
                            else -> listOf(
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
                                                        appLanguage.pick("سؤالك", "You", "Savolingiz")
                                                    } else {
                                                        appLanguage.pick("المساعد الإسلامي", "Mihrab AI", "Mihrob AI")
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
                                        appLanguage.pick(
                                            "جارٍ استحضار الجواب الموثق...",
                                            "Preparing authentic response...",
                                            "Ishonchli javob tayyorlanmoqda..."
                                        ),
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
                                        Text(
                                            appLanguage.pick("يكتب الآن...", "Streaming...", "Yozilmoqda..."),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(s.text, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            is AiUiState.Error -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                    if (isAppCheckTokenError(s.message)) {
                                        AppCheckInlineDiagnosticCard(appLanguage = appLanguage)
                                    }
                                }
                            }
                            else -> Unit
                        }

                        OutlinedTextField(
                            value = questionInput,
                            onValueChange = { questionInput = it },
                            label = {
                                Text(
                                    appLanguage.pick(
                                        "اسأل عن تفسير آية، معنى كلمة قرآنية، أو الأذكار...",
                                        "Ask about Tafsir, Quranic vocabulary, Adhkar, or rulings...",
                                        "Oyat tafsiri, Qur'on so'zlari ma'nosi yoki zikrlar haqida so'rang..."
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("ai_question_input"),
                            minLines = 2
                        )
                        Button(
                            onClick = {
                                val q = questionInput
                                questionInput = ""
                                FeatureUsageTracker.recordUsage(context, SmartFeatureId.AI_COMPANION)
                                aiViewModel.askCompanion(q, appLanguage.nativeLabel)
                            },
                            enabled = effectiveAiEnabled && questionInput.isNotBlank() &&
                                companionState !is AiUiState.Loading && companionState !is AiUiState.Streaming,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_ask_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(appLanguage.pick("إرسال للمساعد الذكي", "Ask Islamic AI", "Islomiy AI dan so'rash"))
                        }
                    } else {
                        Text(
                            text = appLanguage.pick(
                                "اختبر حفظك للقرآن الكريم عبر إكمال الآيات مع تنبيهات دقيقة لمواضع المتشابهات.",
                                "Practice Quran memorization with verse-completion challenges and Mutashabihat alerts.",
                                "Mutashobih oyatlar eslatmasi bilan oyatlarni davom ettirib Qur'on hifzingizni sinang."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = hifzTopicInput,
                            onValueChange = { hifzTopicInput = it },
                            label = {
                                Text(
                                    appLanguage.pick(
                                        "السورة أو الجزء (مثال: سورة الكهف، جزء تبارك)",
                                        "Surah or Juz scope",
                                        "Sura yoki pora (masalan: Kahf surasi)"
                                    )
                                )
                            },
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
                            Text(
                                appLanguage.pick(
                                    "توليد سؤال حفظ ومتشابهات",
                                    "Generate Hifz & Mutashabihat Quiz",
                                    "Hifz va mutashobih savolini yaratish"
                                )
                            )
                        }

                        when (val qs = hifzQuizState) {
                            is UiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AiPulsingDotsIndicator()
                                Text(
                                    appLanguage.pick(
                                        "جارٍ إعداد السؤال...",
                                        "Generating memorization challenge...",
                                        "Savol tayyorlanmoqda..."
                                    )
                                )
                            }
                            is UiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(qs.message, color = MaterialTheme.colorScheme.error)
                                if (isAppCheckTokenError(qs.message)) {
                                    AppCheckInlineDiagnosticCard(appLanguage = appLanguage)
                                }
                            }
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
                                                    text = appLanguage.pick("تنبيه المتشابهات", "Mutashabihat Alert", "Mutashobih oyat eslatmasi"),
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
                    appLanguage.pick(
                        "ملخّص خطبة الجمعة والورد اليومي",
                        "Friday Khutbah & Daily Wird Summarizer",
                        "Juma xutbasi va kunlik vird xulosasi"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedTextField(
                value = khutbahTopic,
                onValueChange = { khutbahTopic = it },
                label = {
                    Text(
                        appLanguage.pick(
                            "اكتب موضوع الخطبة أو ملاحظاتك (مثل: الصبر والشكر)",
                            "Enter Khutbah notes or topic (e.g., Patience & Gratitude)",
                            "Xutba mavzusi yoki qaydlaringizni yozing (masalan: Sabr va shukr)"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { aiViewModel.summarizeKhutbah(khutbahTopic, appLanguage.nativeLabel) },
                enabled = effectiveAiEnabled && khutbahTopic.isNotBlank() && khutbahState !is UiState.Loading,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(appLanguage.pick("توليد ملخص عملي", "Generate Actionable Summary", "Amaliy xulosa yaratish"))
            }

            when (val ks = khutbahState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(26.dp))
                is UiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ks.message, color = MaterialTheme.colorScheme.error)
                    if (isAppCheckTokenError(ks.message)) {
                        AppCheckInlineDiagnosticCard(appLanguage = appLanguage)
                    }
                }
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

private fun isAppCheckTokenError(message: String): Boolean {
    val lower = message.lowercase()
    return lower.contains("app check") ||
        lower.contains("appcheck") ||
        lower.contains("token") ||
        lower.contains("403") ||
        lower.contains("unauthenticated") ||
        lower.contains("attestation")
}

@Composable
private fun AppCheckInlineDiagnosticCard(appLanguage: AppLanguage) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val debugToken = remember { getAppCheckDebugSecret(context) }
    var copied by remember { mutableStateOf(false) }

    Surface(
        color = BurnishedGold.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.48f)),
        modifier = Modifier.fillMaxWidth().testTag("ai_appcheck_diagnostic_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = BurnishedGold)
                Text(
                    text = appLanguage.pick(
                        "حل خطأ توكن Firebase App Check (نسخة التصحيح)",
                        "Fix Firebase App Check Token Error (Debug APK)",
                        "Firebase App Check token xatosini tuzatish (Debug APK)"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = appLanguage.pick(
                    "انسخ رمز التصحيح أدناه وأضفه في Firebase Console ← App Check ← Apps ← Manage debug tokens، ثم أعد المحاولة (أو استخدم تبويب الإعدادات لتعديله).",
                    "Copy the Debug Token below and register it in Firebase Console -> App Check -> Apps -> Manage debug tokens, then retry.",
                    "Quyidagi Debug Token ni nusxalab, Firebase Console -> App Check -> Apps -> Manage debug tokens bo'limiga qo'shing va qayta urinib ko'ring."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = debugToken ?: appLanguage.pick(
                        "لم يتولد الرمز بعد — افتح الإعدادات للعرض أو التخصيص",
                        "Token not generated yet — open Settings to inspect or set custom UUID",
                        "Token hali yaratilmadi — ko'rish yoki kiritish uchun Sozlamalarni oching"
                    ),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
            if (!debugToken.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(debugToken))
                        copied = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("ai_copy_appcheck_token")
                ) {
                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (copied) appLanguage.pick("تم نسخ الرمز ✓", "Token Copied ✓", "Token nusxalandi ✓")
                        else appLanguage.pick("نسخ Debug Token", "Copy Debug Token", "Debug Token nusxalash")
                    )
                }
            }
        }
    }
}
