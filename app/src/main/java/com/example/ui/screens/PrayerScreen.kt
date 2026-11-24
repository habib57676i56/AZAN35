package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.hardware.AdhanAudioPlayer
import com.example.core.hardware.shareDesignedVerseTextOrCard
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.data.firebase.AdhanAlertMode
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.CalculationMethod
import com.example.domain.IslamicLibraryData
import com.example.domain.PrayerTimeEntry
import com.example.domain.PrayerTimesEngine
import com.example.domain.QuranContentData
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.IslamicGeometricPatternCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.LuminousMintEmerald
import com.example.ui.theme.SkyMoodPeriod
import com.example.ui.theme.skyMoodGradient
import com.example.ui.theme.withTabularNums
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrayerScreen(
    viewModel: MihrabViewModel,
    adhanAudioPlayer: AdhanAudioPlayer,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    useIndic: Boolean,
    appOperations: AppOperationsConfig = AppOperationsConfig(),
    onOpenKhutbahAi: () -> Unit,
    onOpenMushaf: () -> Unit = {},
    onOpenTasbih: () -> Unit = {},
    onOpenAi: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalLiquidGlassPalette.current
    val isArabicUi = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU

    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefsState.collectAsStateWithLifecycle()
    val logsState by viewModel.prayerLogsState.collectAsStateWithLifecycle()
    val manualOffsets by viewModel.manualOffsets.collectAsStateWithLifecycle()
    val adhanVoices by viewModel.adhanVoicesState.collectAsStateWithLifecycle()
    val adhkarCatalog by viewModel.adhkarCatalogState.collectAsStateWithLifecycle()
    val todayAdhkarLogs by viewModel.todayAdhkarLogsState.collectAsStateWithLifecycle()
    val adhanState by adhanAudioPlayer.state.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val logs = (logsState as? UiState.Success)?.data ?: emptyList()
    val todayKey = viewModel.todayDateKey()
    val todayLog = logs.firstOrNull { it.dateKey == todayKey }

    val schedule = remember(settings, manualOffsets) {
        viewModel.computeSchedule(settings, manualOffsets)
    }
    val hijriToday = remember(settings) { viewModel.computeHijriToday(settings) }
    val mosques = remember(settings.city, schedule) {
        PrayerTimesEngine.getMosquesForCity(settings.city, schedule)
    }

    var nowSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            nowSeconds = cal.get(Calendar.HOUR_OF_DAY) * 3600 +
                cal.get(Calendar.MINUTE) * 60 +
                cal.get(Calendar.SECOND)
            delay(1000L)
        }
    }

    val corePrayers = listOf(schedule.fajr, schedule.dhuhr, schedule.asr, schedule.maghrib, schedule.isha)
    val nextIndex = corePrayers.indexOfFirst { it.timeMinutes * 60 > nowSeconds }.let { if (it >= 0) it else 0 }
    val nextPrayerEntry = corePrayers[nextIndex]
    val prevPrayerEntry = corePrayers[(nextIndex - 1 + corePrayers.size) % corePrayers.size]
    val secondsRemaining = if (nextPrayerEntry.timeMinutes * 60 > nowSeconds) {
        nextPrayerEntry.timeMinutes * 60 - nowSeconds
    } else {
        (24 * 3600 - nowSeconds) + nextPrayerEntry.timeMinutes * 60
    }
    val totalWindowSeconds = run {
        val diff = nextPrayerEntry.timeMinutes * 60 - prevPrayerEntry.timeMinutes * 60
        if (diff > 0) diff else (24 * 3600 + diff)
    }.coerceAtLeast(60)
    val countdownProgress = (1f - (secondsRemaining.toFloat() / totalWindowSeconds.toFloat())).coerceIn(0.05f, 1f)

    val countdownStr = String.format(
        "%02d:%02d:%02d",
        secondsRemaining / 3600,
        (secondsRemaining % 3600) / 60,
        secondsRemaining % 60
    )

    val skyMood = remember(nowSeconds, schedule) {
        val nowMin = nowSeconds / 60
        when {
            nowMin in schedule.fajr.timeMinutes until schedule.sunrise.timeMinutes -> SkyMoodPeriod.FAJR_DAWN
            nowMin in schedule.sunrise.timeMinutes until schedule.asr.timeMinutes -> SkyMoodPeriod.DHUHR_DAYLIGHT
            nowMin in schedule.asr.timeMinutes until schedule.maghrib.timeMinutes -> SkyMoodPeriod.ASR_GOLDEN
            nowMin in schedule.maghrib.timeMinutes until schedule.isha.timeMinutes -> SkyMoodPeriod.MAGHRIB_TWILIGHT
            else -> SkyMoodPeriod.ISHA_NIGHT
        }
    }

    var showQiblaSheet by remember { mutableStateOf(false) }
    var showAdhanSheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showGuidesSheet by remember { mutableStateOf(false) }
    var showRakahCounterSheet by remember { mutableStateOf(false) }
    var khushuPrePrayerMode by remember { mutableStateOf(false) }

    val isFriday = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY }
    val dailyAyahPair = remember {
        val all = QuranContentData.surahs.flatMap { s -> s.ayahs.map { s to it } }
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        all[day % all.size]
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                adhanAudioPlayer.importCustomAudioFromUri(uri)
                    .onSuccess { (localPath, displayName) ->
                        viewModel.saveCustomAdhanFile(localPath, displayName)
                    }
                    .onFailure { err ->
                        viewModel.showMessage(err.localizedMessage ?: "تعذر استيراد ملف الأذان")
                    }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Sacred Dynamic Sky-Mood Prayer Hero Card with Live Countdown Ring
            item {
                val skyColors = skyMoodGradient(skyMood)
                val goldCol = glass.accentGold
                LiquidGlassCard(
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = 0.dp,
                    borderColor = goldCol.copy(alpha = 0.36f),
                    elevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_prayer_banner),
                            contentDescription = "Sanctuary Mihrab Horizon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.24f)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Brush.linearGradient(colors = skyColors.map { it.copy(alpha = 0.90f) }))
                        )
                        IslamicGeometricPatternCanvas(
                            modifier = Modifier.matchParentSize(),
                            tint = goldCol,
                            alpha = 0.055f
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabicUi) hijriToday.formattedAr() else formatNumerals(hijriToday.formattedEn(), useIndic),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFF6D98E),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${settings.city} • ${CalculationMethod.fromId(settings.calculationMethod).id}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.86f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    onClick = { showQiblaSheet = true },
                                    color = Color.White.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("open_qibla_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(MihrabIcons.KaabaCube, null, tint = Color(0xFFF6D98E), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${formatNumerals("${schedule.qiblaBearingDegrees.toInt()}°", useIndic)} ${if (isArabicUi) "القبلة" else "Qibla"}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.14f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = strings.nextPrayer,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF6D98E),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isArabicUi) nextPrayerEntry.nameAr else "${nextPrayerEntry.nameEn} • ${nextPrayerEntry.nameAr}",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isArabicUi) {
                                            "الأذان الساعة ${formatNumerals(nextPrayerEntry.formattedTime, useIndic)}"
                                        } else {
                                            "Adhan at ${formatNumerals(nextPrayerEntry.formattedTime, useIndic)}"
                                        },
                                        style = MaterialTheme.typography.bodyLarge.withTabularNums(),
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }

                                // Live Circular Countdown Ring
                                Box(
                                    modifier = Modifier.size(110.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                        val strokeW = 7.dp.toPx()
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.16f),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = strokeW)
                                        )
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                colors = listOf(Color(0xFFF6D98E), LuminousMintEmerald, Color(0xFFF6D98E))
                                            ),
                                            startAngle = -90f,
                                            sweepAngle = 360f * countdownProgress,
                                            useCenter = false,
                                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatNumerals(countdownStr, useIndic),
                                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (isArabicUi) "المتبقي" else "Remaining",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF6D98E)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HeroTimePill(
                                    label = if (isArabicUi) "الإمساك" else "Imsak",
                                    value = formatNumerals(schedule.imsak.formattedTime, useIndic),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroTimePill(
                                    label = if (isArabicUi) "الشروق" else "Sunrise",
                                    value = formatNumerals(schedule.sunrise.formattedTime, useIndic),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroTimePill(
                                    label = if (isArabicUi) "القيام ⅓" else "Qiyam ⅓",
                                    value = formatNumerals(schedule.lastThirdQiyam, useIndic),
                                    highlight = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick-Action Liquid Glass Pills
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        LiquidGlassPill(
                            selected = true,
                            onClick = onOpenMushaf,
                            icon = Icons.Rounded.AutoStories,
                            label = if (isArabicUi) {
                                "متابعة المصحف (${formatNumerals(settings.lastReadPage.toString(), useIndic)})"
                            } else {
                                "Mushaf p.${settings.lastReadPage}"
                            }
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = { showQiblaSheet = true },
                            icon = MihrabIcons.Qibla,
                            label = strings.qiblaCompass
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = onOpenTasbih,
                            icon = MihrabIcons.TasbihBeads,
                            label = strings.digitalTasbih
                        )
                    }
                    if (appOperations.aiAssistantEnabled) {
                        item {
                            LiquidGlassPill(
                                selected = false,
                                onClick = onOpenAi,
                                icon = MihrabIcons.AiSpark,
                                label = if (isArabicUi) "المساعد الذكي" else "Islamic AI"
                            )
                        }
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = { showAdhanSheet = true },
                            icon = MihrabIcons.AdhanVoice,
                            label = strings.adhanSettingsTitle,
                            modifier = Modifier.testTag("open_adhan_settings_chip")
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = { showMethodSheet = true },
                            icon = MihrabIcons.MethodTune,
                            label = if (isArabicUi) "الحساب والتعديل" else "Method & Offsets"
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = { showGuidesSheet = true },
                            icon = MihrabIcons.Guides,
                            label = if (isArabicUi) "أحكام الصلوات" else "Prayer Guides"
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = { showRakahCounterSheet = true },
                            icon = MihrabIcons.RakahTimer,
                            label = if (isArabicUi) "عدّاد الركعات" else "Rak'ah & Focus"
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = khushuPrePrayerMode,
                            onClick = { khushuPrePrayerMode = !khushuPrePrayerMode },
                            icon = MihrabIcons.KhushuStar,
                            label = if (isArabicUi) "تأمل الخشوع" else "Khushu'"
                        )
                    }
                }
            }

            // 3. Daily Worship Bento Grid + Ayah of the Day
            item {
                val pagesRead = settings.khatmahPagesRead.toInt().coerceIn(0, 604)
                val khatmahRatio = (pagesRead / 604f).coerceIn(0.02f, 1f)
                val totalAdhkarTarget = adhkarCatalog.size.coerceAtLeast(1)
                val totalAdhkarDone = todayAdhkarLogs.sumOf { it.completedCount.toInt() }.coerceAtMost(totalAdhkarTarget)
                val adhkarRatio = (totalAdhkarDone.toFloat() / totalAdhkarTarget.toFloat()).coerceIn(0f, 1f)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidGlassCard(
                            onClick = onOpenMushaf,
                            contentPadding = 14.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MiniCircularRing(
                                    progress = khatmahRatio,
                                    ringColor = MaterialTheme.colorScheme.primary,
                                    centerLabel = formatNumerals("${(khatmahRatio * 100).toInt()}%", useIndic)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabicUi) "الورد والختمة" else "Daily Wird",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatNumerals("$pagesRead / 604", useIndic),
                                        style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isArabicUi) "صفحة ${formatNumerals(settings.lastReadPage.toString(), useIndic)}"
                                        else "Last p.${settings.lastReadPage}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        LiquidGlassCard(
                            onClick = onOpenTasbih,
                            contentPadding = 14.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MiniCircularRing(
                                    progress = adhkarRatio,
                                    ringColor = glass.accentGold,
                                    centerLabel = formatNumerals("$totalAdhkarDone", useIndic)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabicUi) "الأذكار والتسبيح" else "Daily Adhkar",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatNumerals("$totalAdhkarDone / $totalAdhkarTarget", useIndic),
                                        style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                        color = glass.accentGold
                                    )
                                    Text(
                                        text = if (isArabicUi) "اضغط للسبحة" else "Open Tasbih",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Ayah of the Day Glass Feature Card
                    val (dailySurah, dailyAyah) = dailyAyahPair
                    LiquidGlassCard(
                        contentPadding = 0.dp,
                        borderColor = glass.accentGold.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IslamicGeometricPatternCanvas(
                                modifier = Modifier.matchParentSize(),
                                tint = glass.accentGold,
                                alpha = 0.04f
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(MihrabIcons.MihrabArch, null, tint = glass.accentGold, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = if (isArabicUi) "آية اليوم • ${dailySurah.nameAr}" else "Ayah of the Day • ${dailySurah.nameEn}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        onClick = {
                                            shareDesignedVerseTextOrCard(
                                                context = context,
                                                surahLabel = "${dailySurah.nameAr} (${dailySurah.number}:${dailyAyah.number})",
                                                arabicText = dailyAyah.arabic,
                                                translation = if (isArabicUi) dailyAyah.tafsirMuyassar else dailyAyah.translationEn
                                            )
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Rounded.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = if (isArabicUi) "مشاركة" else "Share",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "﴿${dailyAyah.arabic}﴾",
                                    fontFamily = AmiriQuranFontFamily,
                                    fontSize = 21.sp,
                                    lineHeight = 36.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (!isArabicUi) {
                                    Text(
                                        text = dailyAyah.translationEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (khushuPrePrayerMode) {
                item {
                    LiquidGlassCard(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        borderColor = glass.accentGold.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isArabicUi) "استحضار الخشوع قبل الصلاة" else "Khushu' Reflection Before Prayer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isArabicUi) {
                                "توقف ١٠ ثوانٍ وتذكّر أنك تقف بين يدي ملك الملوك؛ صلِّ صلاة مودِّع."
                            } else {
                                "Pause for 10 seconds. Imagine standing before the King of Kings as if this were your farewell prayer."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val rating = (todayLog?.khushuRating ?: 3L).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isArabicUi) "حضور القلب اليوم: " else "Today's presence: ", style = MaterialTheme.typography.labelLarge)
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { viewModel.setKhushuRating(star, todayLog) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = "Khushu $star",
                                        tint = if (star <= rating) glass.accentGold else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isFriday) {
                item {
                    LiquidGlassCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
                        borderColor = glass.accentGold.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isArabicUi) "جمعة مباركة • سنن يوم الجمعة" else "Jumu'ah Mubarak • Friday Sunnahs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isArabicUi) {
                                "• الغسل والتطيب • قراءة سورة الكهف • الإكثار من الصلاة على النبي ﷺ • تحري ساعة الإجابة قبل المغرب."
                            } else {
                                "• Ghusl & fragrance • Surah Al-Kahf • Abundant Salawat ﷺ • Seek the final hour before Maghrib."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (appOperations.aiAssistantEnabled) {
                            OutlinedButton(onClick = onOpenKhutbahAi) {
                                Text(if (isArabicUi) "تلخيص خطبة الجمعة بالذكاء الاصطناعي" else "Summarize Today's Khutbah with AI")
                            }
                        }
                    }
                }
            }

            // 4. Today's Prayer Timetable & Tracker
            item {
                val completedCount = listOf(
                    todayLog?.fajr == true,
                    todayLog?.dhuhr == true,
                    todayLog?.asr == true,
                    todayLog?.maghrib == true,
                    todayLog?.isha == true
                ).count { it }

                val activeVoice = adhanVoices.firstOrNull { it.id == settings.adhanVoiceId } ?: adhanVoices.firstOrNull()
                val activeVoiceLabel = when {
                    !settings.adhanEnabled -> if (isArabicUi) "الأذان متوقف" else "Adhan Muted"
                    settings.adhanVoiceId == "custom" && settings.customAdhanName.isNotBlank() -> settings.customAdhanName
                    activeVoice != null -> if (isArabicUi) activeVoice.nameAr else activeVoice.nameEn
                    else -> ""
                }

                LiquidGlassCard(
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = 18.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.prayerTracker, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = if (isArabicUi) {
                                    "${formatNumerals("$completedCount / 5", useIndic)} فرائض • $activeVoiceLabel"
                                } else {
                                    "${formatNumerals("$completedCount / 5", useIndic)} Completed • $activeVoiceLabel"
                                },
                                style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AirplanemodeActive, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isArabicUi) "قصر" else "Qasr", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = settings.travelerMode,
                                onCheckedChange = { viewModel.updateSettings(mapOf("travelerMode" to it)) }
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { completedCount / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary
                    )

                    val prayerRows = listOf(
                        Triple(schedule.fajr, todayLog?.fajr == true, "fajr"),
                        Triple(schedule.dhuhr, todayLog?.dhuhr == true, "dhuhr"),
                        Triple(schedule.asr, todayLog?.asr == true, "asr"),
                        Triple(schedule.maghrib, todayLog?.maghrib == true, "maghrib"),
                        Triple(schedule.isha, todayLog?.isha == true, "isha")
                    )

                    prayerRows.forEach { (entry, checked, key) ->
                        val mode = AdhanAlertMode.fromId(settings.perPrayerAdhanModes[key])
                        PrayerRowItem(
                            entry = entry,
                            isCompleted = checked,
                            isNext = nextPrayerEntry.key == entry.key,
                            alertMode = mode,
                            isArabicUi = isArabicUi,
                            useIndic = useIndic,
                            onToggle = { viewModel.togglePrayerToday(key, todayLog) },
                            onOpenAdhan = { showAdhanSheet = true }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = glass.glassBorder)
                    Text(
                        if (isArabicUi) "السنن الرواتب والنوافل" else "Sunnah & Nawafil Schedule",
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassPill(
                            selected = todayLog?.sunnahRawatib == true,
                            onClick = { viewModel.togglePrayerToday("sunnahRawatib", todayLog) },
                            label = if (isArabicUi) "السنن الرواتب (١٢)" else "12 Rawatib"
                        )
                        LiquidGlassPill(
                            selected = todayLog?.duha == true,
                            onClick = { viewModel.togglePrayerToday("duha", todayLog) },
                            label = if (isArabicUi) "الضحى" else "Duha"
                        )
                        LiquidGlassPill(
                            selected = todayLog?.witr == true,
                            onClick = { viewModel.togglePrayerToday("witr", todayLog) },
                            label = if (isArabicUi) "الوتر" else "Witr"
                        )
                        LiquidGlassPill(
                            selected = todayLog?.qiyam == true,
                            onClick = { viewModel.togglePrayerToday("qiyam", todayLog) },
                            label = if (isArabicUi) "قيام الليل" else "Qiyam / Tahajjud"
                        )
                    }
                }
            }

            // 5. Qada Missed Prayers Counter
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.qadaCounter, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (isArabicUi) "تابع قضاء الصلوات الفائتة وأتمها بالتدريج." else "Track and gradually fulfill missed obligatory prayers with ease.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val qadaItems = listOf(
                        Triple(if (isArabicUi) "الفجر" else "Fajr", "qadaFajr", settings.qadaFajr),
                        Triple(if (isArabicUi) "الظهر" else "Dhuhr", "qadaDhuhr", settings.qadaDhuhr),
                        Triple(if (isArabicUi) "العصر" else "Asr", "qadaAsr", settings.qadaAsr),
                        Triple(if (isArabicUi) "المغرب" else "Maghrib", "qadaMaghrib", settings.qadaMaghrib),
                        Triple(if (isArabicUi) "العشاء" else "Isha", "qadaIsha", settings.qadaIsha)
                    )
                    qadaItems.forEach { (label, field, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.adjustQada(field, value, -1L) }) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label Qada")
                                }
                                Text(
                                    text = formatNumerals(value.toString(), useIndic),
                                    style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { viewModel.adjustQada(field, value, 1L) }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase $label Qada")
                                }
                            }
                        }
                    }
                }
            }

            // 6. Nearby Mosques & Iqamah Times
            item {
                Text(
                    if (isArabicUi) "المساجد القريبة وأوقات الإقامة (${settings.city})"
                    else "Nearby Mosques & Iqamah (${settings.city})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(mosques) { mosque ->
                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Mosque, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(mosque.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            mosque.distanceLabel,
                            style = MaterialTheme.typography.labelLarge.withTabularNums(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        if (isArabicUi) {
                            "الإقامة: الفجر ${mosque.iqamahFajr} • الظهر ${mosque.iqamahDhuhr} • العصر ${mosque.iqamahAsr} • المغرب ${mosque.iqamahMaghrib} • العشاء ${mosque.iqamahIsha}"
                        } else {
                            "Iqamah: Fajr ${mosque.iqamahFajr} • Dhuhr ${mosque.iqamahDhuhr} • Asr ${mosque.iqamahAsr} • Maghrib ${mosque.iqamahMaghrib} • Isha ${mosque.iqamahIsha}"
                        },
                        style = MaterialTheme.typography.bodyMedium.withTabularNums()
                    )
                    Text(
                        if (isArabicUi) "خطبة الجمعة: ${mosque.jummahKhutbah}" else "Jumu'ah Khutbah: ${mosque.jummahKhutbah}",
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.accentGold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        mosque.facilities.forEach { facility ->
                            Surface(
                                color = glass.glassSubtleFill,
                                border = BorderStroke(1.dp, glass.glassBorder),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = facility,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CUSTOM ADHAN & PER-PRAYER ALERT SHEET ---
    if (showAdhanSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val fallbackVoice = adhanVoices.first()
        val selectedVoice = adhanVoices.firstOrNull { it.id == settings.adhanVoiceId } ?: fallbackVoice
        val customPath = localPrefs?.customAdhanLocalPath?.takeIf { it.isNotBlank() }

        ModalBottomSheet(
            onDismissRequest = {
                adhanAudioPlayer.stop()
                showAdhanSheet = false
            },
            sheetState = sheetState,
            containerColor = glass.glassFillStrong
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.adhanSettingsTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = settings.adhanEnabled,
                        onCheckedChange = { viewModel.updateSettings(mapOf("adhanEnabled" to it)) }
                    )
                }

                if (adhanState.isPlaying || adhanState.isBuffering || !adhanState.errorMessage.isNullOrBlank()) {
                    LiquidGlassCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        contentPadding = 12.dp
                    ) {
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
                                if (adhanState.isBuffering) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    AdhanWaveformIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text(adhanState.activeLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    if (!adhanState.errorMessage.isNullOrBlank()) {
                                        Text(adhanState.errorMessage!!, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            if (adhanState.isPlaying || adhanState.isBuffering) {
                                OutlinedButton(onClick = { adhanAudioPlayer.stop() }) {
                                    Icon(Icons.Rounded.Stop, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.stopAdhan)
                                }
                            }
                        }
                    }
                }

                Text(strings.chooseAdhanVoice, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                adhanVoices.forEach { voice ->
                    val isSelected = settings.adhanVoiceId == voice.id
                    val isPlayingThis = adhanState.isPlaying && adhanState.activeLabel == voice.nameAr
                    LiquidGlassCard(
                        onClick = { viewModel.updateSettings(mapOf("adhanVoiceId" to voice.id)) },
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else glass.glassFill,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else glass.glassBorder,
                        contentPadding = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adhan_voice_${voice.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isArabicUi) voice.nameAr else voice.nameEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(voice.muezzin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isPlayingThis) {
                                    AdhanWaveformIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (isPlayingThis) {
                                            adhanAudioPlayer.stop()
                                        } else {
                                            adhanAudioPlayer.playPreview(voice, AdhanAlertMode.FULL_ADHAN)
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isPlayingThis) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isPlayingThis) strings.stopAdhan else strings.previewAdhan)
                                }
                                if (isSelected) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                if (appOperations.customAdhanUploadEnabled) {
                    LiquidGlassCard(
                        containerColor = if (settings.adhanVoiceId == "custom") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else glass.glassFill,
                        contentPadding = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.AudioFile, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.uploadCustomAdhan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (settings.customAdhanName.isNotBlank()) {
                                    Text(
                                        text = "${if (isArabicUi) "الملف المختار" else "Selected"}: ${settings.customAdhanName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_custom_adhan_button")
                            ) {
                                Text(strings.uploadCustomAdhan)
                            }
                            if (!customPath.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateSettings(mapOf("adhanVoiceId" to "custom"))
                                        adhanAudioPlayer.playPreview(
                                            voice = selectedVoice,
                                            mode = AdhanAlertMode.FULL_ADHAN,
                                            customLocalPath = customPath,
                                            customDisplayName = settings.customAdhanName
                                        )
                                    }
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.previewAdhan)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = glass.glassBorder)
                Text(
                    if (isArabicUi) "تخصيص تنبيه كل صلاة" else "Per-Prayer Notification Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val fivePrayers = listOf(
                    "fajr" to (if (isArabicUi) "الفجر" else "Fajr"),
                    "dhuhr" to (if (isArabicUi) "الظهر" else "Dhuhr"),
                    "asr" to (if (isArabicUi) "العصر" else "Asr"),
                    "maghrib" to (if (isArabicUi) "المغرب" else "Maghrib"),
                    "isha" to (if (isArabicUi) "العشاء" else "Isha")
                )

                fivePrayers.forEach { (pKey, pLabel) ->
                    val currentMode = AdhanAlertMode.fromId(settings.perPrayerAdhanModes[pKey])
                    LiquidGlassCard(contentPadding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(MihrabIcons.iconForPrayer(pKey), null, tint = MaterialTheme.colorScheme.primary)
                                Text(pLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = {
                                    adhanAudioPlayer.playPreview(
                                        voice = selectedVoice,
                                        mode = currentMode,
                                        customLocalPath = if (settings.adhanVoiceId == "custom") customPath else null,
                                        customDisplayName = settings.customAdhanName
                                    )
                                }
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = strings.previewAdhan)
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AdhanAlertMode.entries.forEach { mode ->
                                LiquidGlassPill(
                                    selected = currentMode == mode,
                                    onClick = { viewModel.savePerPrayerAdhanMode(settings.perPrayerAdhanModes, pKey, mode) },
                                    icon = MihrabIcons.iconForAdhanMode(mode),
                                    label = if (isArabicUi) mode.labelAr else mode.labelEn
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        adhanAudioPlayer.stop()
                        showAdhanSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArabicUi) "تم" else "Done")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showQiblaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQiblaSheet = false },
            containerColor = glass.glassFillStrong
        ) {
            QiblaAndLocationDialogContent(
                currentCity = settings.city,
                qiblaBearing = schedule.qiblaBearingDegrees,
                distanceKm = schedule.distanceToKaabaKm,
                isArabicUi = isArabicUi,
                useIndic = useIndic,
                arQiblaEnabled = appOperations.arQiblaEnabled,
                onSelectCity = { preset -> viewModel.selectCityPreset(preset) },
                onGpsLocation = { label, lat, lng -> viewModel.updateLocationCoordinates(label, lat, lng) },
                onClose = { showQiblaSheet = false }
            )
        }
    }

    if (showMethodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMethodSheet = false },
            containerColor = glass.glassFillStrong
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isArabicUi) "طريقة الحساب والتعديلات اليدوية" else "Calculation Method & Manual Offsets",
                    style = MaterialTheme.typography.headlineMedium
                )
                CalculationMethod.entries.forEach { method ->
                    LiquidGlassCard(
                        onClick = { viewModel.updateSettings(mapOf("calculationMethod" to method.id)) },
                        containerColor = if (settings.calculationMethod == method.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else glass.glassFill,
                        contentPadding = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(method.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Fajr ${method.fajrAngle}° • Isha ${method.ishaIntervalMin?.let { "$it min" } ?: "${method.ishaAngle}°"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = settings.calculationMethod == method.id,
                                onCheckedChange = { viewModel.updateSettings(mapOf("calculationMethod" to method.id)) }
                            )
                        }
                    }
                }
                HorizontalDivider(color = glass.glassBorder)
                Text(if (isArabicUi) "تعديل الدقائق يدوياً" else "Manual Minute Adjustments", style = MaterialTheme.typography.titleMedium)
                listOf(
                    "fajr" to (if (isArabicUi) "الفجر" else "Fajr"),
                    "dhuhr" to (if (isArabicUi) "الظهر" else "Dhuhr"),
                    "asr" to (if (isArabicUi) "العصر" else "Asr"),
                    "maghrib" to (if (isArabicUi) "المغرب" else "Maghrib"),
                    "isha" to (if (isArabicUi) "العشاء" else "Isha")
                ).forEach { (key, label) ->
                    val offset = manualOffsets[key] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.adjustPrayerOffset(key, -1) }) {
                                Icon(Icons.Rounded.Remove, "Minus 1 min")
                            }
                            Text(
                                "${if (offset > 0) "+" else ""}$offset min",
                                style = MaterialTheme.typography.titleMedium.withTabularNums()
                            )
                            IconButton(onClick = { viewModel.adjustPrayerOffset(key, 1) }) {
                                Icon(Icons.Rounded.Add, "Plus 1 min")
                            }
                        }
                    }
                }
                Button(onClick = { showMethodSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isArabicUi) "تم" else "Done")
                }
            }
        }
    }

    if (showGuidesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGuidesSheet = false },
            containerColor = glass.glassFillStrong
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(if (isArabicUi) "أدلة الصلوات والسنن" else "Salah & Special Prayer Guides", style = MaterialTheme.typography.headlineMedium)
                IslamicLibraryData.prayerGuides.forEach { guide ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(guide.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(guide.subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        guide.steps.forEach { step ->
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Source: ${guide.reference}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showRakahCounterSheet) {
        var rakahCount by remember { mutableIntStateOf(0) }
        var sujudCount by remember { mutableIntStateOf(0) }
        var focusRunning by remember { mutableStateOf(false) }
        var focusSeconds by remember { mutableIntStateOf(600) }
        LaunchedEffect(focusRunning) {
            while (focusRunning && focusSeconds > 0) {
                delay(1000L)
                focusSeconds--
            }
            if (focusSeconds == 0) focusRunning = false
        }
        ModalBottomSheet(
            onDismissRequest = { showRakahCounterSheet = false },
            containerColor = glass.glassFillStrong
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(if (isArabicUi) "عدّاد الركعات ومؤقت الخشوع" else "Rak'ah Counter & Worship Focus", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (isArabicUi) "اضغط بعد كل سجدة أو ركعة لتفادي السهو في الصلاة." else "Tap each Sujud or Rak'ah to prevent forgetfulness while praying.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isArabicUi) "الركعات" else "Rak'ah", style = MaterialTheme.typography.labelLarge)
                            Text(formatNumerals("$rakahCount / 4", useIndic), style = MaterialTheme.typography.displayMedium.withTabularNums())
                            Button(onClick = { rakahCount = (rakahCount + 1) % 5 }) {
                                Text(if (isArabicUi) "+١ ركعة" else "+1 Rak'ah")
                            }
                        }
                    }
                    LiquidGlassCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isArabicUi) "السجود" else "Sujud", style = MaterialTheme.typography.labelLarge)
                            Text(formatNumerals("$sujudCount / 2", useIndic), style = MaterialTheme.typography.displayMedium.withTabularNums())
                            Button(onClick = {
                                if (sujudCount == 1) {
                                    sujudCount = 0
                                    rakahCount = (rakahCount + 1) % 5
                                } else {
                                    sujudCount = 1
                                }
                            }) {
                                Text(if (isArabicUi) "تسجيل سجدة" else "Log Sujud")
                            }
                        }
                    }
                }
                HorizontalDivider(color = glass.glassBorder)
                Text(
                    "${if (isArabicUi) "مؤقت الخشوع" else "Quiet Focus"}: ${focusSeconds / 60}:${String.format("%02d", focusSeconds % 60)}",
                    style = MaterialTheme.typography.titleMedium.withTabularNums()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { focusRunning = !focusRunning }) {
                        Text(if (focusRunning) (if (isArabicUi) "إيقاف" else "Pause Timer") else (if (isArabicUi) "بدء ١٠ دقائق" else "Start 10-Min Focus"))
                    }
                    OutlinedButton(onClick = { focusRunning = false; focusSeconds = 600; rakahCount = 0; sujudCount = 0 }) {
                        Text(if (isArabicUi) "إعادة" else "Reset")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroTimePill(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = if (highlight) 0.18f else 0.11f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (highlight) 0.34f else 0.16f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) Color(0xFFF6D98E) else Color.White.copy(alpha = 0.78f),
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.withTabularNums(),
                fontWeight = FontWeight.Bold,
                color = if (highlight) Color(0xFFF6D98E) else Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MiniCircularRing(
    progress: Float,
    ringColor: Color,
    centerLabel: String
) {
    val trackCol = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = 5.dp.toPx()
            drawArc(color = trackCol, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = w))
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = w, cap = StrokeCap.Round)
            )
        }
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.labelSmall.withTabularNums(),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AdhanWaveformIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "adhanWave")
    val barHeights = listOf(0, 150, 300, 100).map { delayMs ->
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(460, delayMillis = delayMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$delayMs"
        ).value
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(18.dp)
    ) {
        barHeights.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * frac).dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun PrayerRowItem(
    entry: PrayerTimeEntry,
    isCompleted: Boolean,
    isNext: Boolean,
    alertMode: AdhanAlertMode,
    isArabicUi: Boolean,
    useIndic: Boolean,
    onToggle: () -> Unit,
    onOpenAdhan: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    Surface(
        onClick = onToggle,
        color = if (isNext) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f) else glass.glassSubtleFill,
        border = BorderStroke(
            width = if (isNext) 1.5.dp else 1.dp,
            color = if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) else glass.glassBorder.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${entry.key}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "${entry.nameEn} completed" else "Mark ${entry.nameEn}",
                    tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = MihrabIcons.iconForPrayer(entry.key),
                    contentDescription = null,
                    tint = if (isNext) MaterialTheme.colorScheme.primary else glass.accentGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isArabicUi) entry.nameAr else entry.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold
                        )
                        if (!isArabicUi) {
                            Spacer(Modifier.width(8.dp))
                            Text(entry.nameAr, fontFamily = AmiriFontFamily, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        entry.sunnahInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onOpenAdhan, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = MihrabIcons.iconForAdhanMode(alertMode),
                        contentDescription = if (isArabicUi) alertMode.labelAr else alertMode.labelEn,
                        tint = if (alertMode == AdhanAlertMode.SILENT) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = formatNumerals(entry.formattedTime, useIndic),
                    style = MaterialTheme.typography.titleMedium.withTabularNums(),
                    fontWeight = FontWeight.Bold,
                    color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
