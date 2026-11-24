package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.example.core.hardware.AdhanAudioPlayer
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
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.SanctuaryEmerald
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
    onOpenKhutbahAi: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isArabicUi = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU

    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefsState.collectAsStateWithLifecycle()
    val logsState by viewModel.prayerLogsState.collectAsStateWithLifecycle()
    val manualOffsets by viewModel.manualOffsets.collectAsStateWithLifecycle()
    val adhanVoices by viewModel.adhanVoicesState.collectAsStateWithLifecycle()
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
    val nextPrayerEntry = corePrayers.firstOrNull { it.timeMinutes * 60 > nowSeconds } ?: schedule.fajr
    val secondsRemaining = if (nextPrayerEntry.timeMinutes * 60 > nowSeconds) {
        nextPrayerEntry.timeMinutes * 60 - nowSeconds
    } else {
        (24 * 3600 - nowSeconds) + nextPrayerEntry.timeMinutes * 60
    }
    val countdownStr = String.format(
        "%02d:%02d:%02d",
        secondsRemaining / 3600,
        (secondsRemaining % 3600) / 60,
        secondsRemaining % 60
    )

    var showQiblaSheet by remember { mutableStateOf(false) }
    var showAdhanSheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showGuidesSheet by remember { mutableStateOf(false) }
    var showRakahCounterSheet by remember { mutableStateOf(false) }
    var khushuPrePrayerMode by remember { mutableStateOf(false) }

    val isFriday = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY }

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
            // 1. Editorial Prayer Hero Card
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = SanctuaryEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_prayer_banner),
                            contentDescription = "Sanctuary Mihrab at twilight",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(254.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(254.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            SanctuaryEmerald.copy(alpha = 0.55f),
                                            SanctuaryEmerald.copy(alpha = 0.94f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabicUi) hijriToday.formattedAr() else formatNumerals(hijriToday.formattedEn(), useIndic),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFE2B95B)
                                    )
                                    Text(
                                        text = "${settings.city} • ${CalculationMethod.fromId(settings.calculationMethod).id}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                Surface(
                                    onClick = { showQiblaSheet = true },
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.testTag("open_qibla_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(MihrabIcons.KaabaCube, null, tint = Color(0xFFE2B95B), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${formatNumerals("${schedule.qiblaBearingDegrees.toInt()}°", useIndic)} ${if (isArabicUi) "القبلة" else "Qibla"}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            Text(
                                text = strings.nextPrayer,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2B95B)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabicUi) nextPrayerEntry.nameAr else "${nextPrayerEntry.nameEn} • ${nextPrayerEntry.nameAr}",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isArabicUi) {
                                            "الأذان الساعة ${formatNumerals(nextPrayerEntry.formattedTime, useIndic)}"
                                        } else {
                                            "Adhan at ${formatNumerals(nextPrayerEntry.formattedTime, useIndic)}"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Text(
                                    text = formatNumerals(countdownStr, useIndic),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2B95B)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${if (isArabicUi) "الإمساك" else "Imsak"}: ${formatNumerals(schedule.imsak.formattedTime, useIndic)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${if (isArabicUi) "الشروق" else "Sunrise"}: ${formatNumerals(schedule.sunrise.formattedTime, useIndic)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${if (isArabicUi) "القيام ⅓" else "Qiyam ⅓"}: ${formatNumerals(schedule.lastThirdQiyam, useIndic)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFE2B95B)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Action Tools Strip with semantic icons + Custom Adhan trigger
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { showAdhanSheet = true },
                        label = { Text(strings.adhanSettingsTitle) },
                        leadingIcon = { Icon(MihrabIcons.AdhanVoice, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("open_adhan_settings_chip")
                    )
                    AssistChip(
                        onClick = { showQiblaSheet = true },
                        label = { Text(strings.qiblaCompass) },
                        leadingIcon = { Icon(MihrabIcons.Qibla, null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { showMethodSheet = true },
                        label = { Text(if (isArabicUi) "الحساب والتعديل" else "Method & Offsets") },
                        leadingIcon = { Icon(MihrabIcons.MethodTune, null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { showGuidesSheet = true },
                        label = { Text(if (isArabicUi) "أحكام الصلوات" else "Prayer Guides") },
                        leadingIcon = { Icon(MihrabIcons.Guides, null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { showRakahCounterSheet = true },
                        label = { Text(if (isArabicUi) "عدّاد الركعات" else "Rak'ah & Focus") },
                        leadingIcon = { Icon(MihrabIcons.RakahTimer, null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { khushuPrePrayerMode = !khushuPrePrayerMode },
                        label = { Text(if (isArabicUi) "تأمل الخشوع" else "Khushu' Reflection") },
                        leadingIcon = { Icon(MihrabIcons.KhushuStar, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            if (khushuPrePrayerMode) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            tint = if (star <= rating) BurnishedGold else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isFriday) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                }
                            )
                            if (appOperations.aiAssistantEnabled) {
                                OutlinedButton(onClick = onOpenKhutbahAi) {
                                    Text(if (isArabicUi) "تلخيص خطبة الجمعة بالذكاء الاصطناعي" else "Summarize Today's Khutbah with AI")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Today's Prayer Schedule & Tracker
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.prayerTracker, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = if (isArabicUi) {
                                        "${formatNumerals("$completedCount / 5", useIndic)} فرائض • $activeVoiceLabel"
                                    } else {
                                        "${formatNumerals("$completedCount / 5", useIndic)} Completed • $activeVoiceLabel"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            if (isArabicUi) "السنن الرواتب والنوافل" else "Sunnah & Nawafil Schedule",
                            style = MaterialTheme.typography.titleMedium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = todayLog?.sunnahRawatib == true,
                                onClick = { viewModel.togglePrayerToday("sunnahRawatib", todayLog) },
                                label = { Text(if (isArabicUi) "السنن الرواتب (١٢)" else "12 Rawatib") }
                            )
                            FilterChip(
                                selected = todayLog?.duha == true,
                                onClick = { viewModel.togglePrayerToday("duha", todayLog) },
                                label = { Text(if (isArabicUi) "الضحى" else "Duha") }
                            )
                            FilterChip(
                                selected = todayLog?.witr == true,
                                onClick = { viewModel.togglePrayerToday("witr", todayLog) },
                                label = { Text(if (isArabicUi) "الوتر" else "Witr") }
                            )
                            FilterChip(
                                selected = todayLog?.qiyam == true,
                                onClick = { viewModel.togglePrayerToday("qiyam", todayLog) },
                                label = { Text(if (isArabicUi) "قيام الليل" else "Qiyam / Tahajjud") }
                            )
                        }
                    }
                }
            }

            // 4. Qada Missed Prayers Counter
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        style = MaterialTheme.typography.titleMedium,
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
            }

            // 5. Nearby Mosques & Iqamah Times
            item {
                Text(
                    if (isArabicUi) "المساجد القريبة وأوقات الإقامة (${settings.city})"
                    else "Nearby Mosques & Iqamah (${settings.city})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(mosques) { mosque ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Mosque, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(mosque.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(mosque.distanceLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            if (isArabicUi) {
                                "الإقامة: الفجر ${mosque.iqamahFajr} • الظهر ${mosque.iqamahDhuhr} • العصر ${mosque.iqamahAsr} • المغرب ${mosque.iqamahMaghrib} • العشاء ${mosque.iqamahIsha}"
                            } else {
                                "Iqamah: Fajr ${mosque.iqamahFajr} • Dhuhr ${mosque.iqamahDhuhr} • Asr ${mosque.iqamahAsr} • Maghrib ${mosque.iqamahMaghrib} • Isha ${mosque.iqamahIsha}"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            if (isArabicUi) "خطبة الجمعة: ${mosque.jummahKhutbah}" else "Jumu'ah Khutbah: ${mosque.jummahKhutbah}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            mosque.facilities.forEach { facility ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = facility,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
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
            sheetState = sheetState
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

                // Active Playback / Buffering Banner
                if (adhanState.isPlaying || adhanState.isBuffering || !adhanState.errorMessage.isNullOrBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                if (adhanState.isBuffering) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(MihrabIcons.iconForAdhanMode(adhanState.activeMode), null, tint = MaterialTheme.colorScheme.primary)
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

                // Built-in 5 Famous Muezzins synced from Firestore / Room
                adhanVoices.forEach { voice ->
                    val isSelected = settings.adhanVoiceId == voice.id
                    Card(
                        onClick = { viewModel.updateSettings(mapOf("adhanVoiceId" to voice.id)) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("adhan_voice_${voice.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isArabicUi) voice.nameAr else voice.nameEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(voice.muezzin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        if (adhanState.isPlaying && adhanState.activeLabel == voice.nameAr) {
                                            adhanAudioPlayer.stop()
                                        } else {
                                            adhanAudioPlayer.playPreview(voice, AdhanAlertMode.FULL_ADHAN)
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (adhanState.isPlaying && adhanState.activeLabel == voice.nameAr) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (adhanState.isPlaying && adhanState.activeLabel == voice.nameAr) strings.stopAdhan else strings.previewAdhan)
                                }
                                if (isSelected) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                if (appOperations.customAdhanUploadEnabled) {
                    // Custom Audio File Upload / Selection via SAF
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (settings.adhanVoiceId == "custom") MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    modifier = Modifier.weight(1f).testTag("pick_custom_adhan_button")
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
                }

                HorizontalDivider()
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
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                AdhanAlertMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = currentMode == mode,
                                        onClick = {
                                            viewModel.savePerPrayerAdhanMode(settings.perPrayerAdhanModes, pKey, mode)
                                        },
                                        leadingIcon = { Icon(MihrabIcons.iconForAdhanMode(mode), null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(if (isArabicUi) mode.labelAr else mode.labelEn) }
                                    )
                                }
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
        ModalBottomSheet(onDismissRequest = { showQiblaSheet = false }) {
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
        ModalBottomSheet(onDismissRequest = { showMethodSheet = false }) {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateSettings(mapOf("calculationMethod" to method.id)) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
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
                HorizontalDivider()
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
                            Text("${if (offset > 0) "+" else ""}$offset min", style = MaterialTheme.typography.titleMedium)
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
        ModalBottomSheet(onDismissRequest = { showGuidesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(if (isArabicUi) "أدلة الصلوات والسنن" else "Salah & Special Prayer Guides", style = MaterialTheme.typography.headlineMedium)
                IslamicLibraryData.prayerGuides.forEach { guide ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        ModalBottomSheet(onDismissRequest = { showRakahCounterSheet = false }) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isArabicUi) "الركعات" else "Rak'ah", style = MaterialTheme.typography.labelLarge)
                            Text(formatNumerals("$rakahCount / 4", useIndic), style = MaterialTheme.typography.displayMedium)
                            Button(onClick = { rakahCount = (rakahCount + 1) % 5 }) {
                                Text(if (isArabicUi) "+١ ركعة" else "+1 Rak'ah")
                            }
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isArabicUi) "السجود" else "Sujud", style = MaterialTheme.typography.labelLarge)
                            Text(formatNumerals("$sujudCount / 2", useIndic), style = MaterialTheme.typography.displayMedium)
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
                HorizontalDivider()
                Text(
                    "${if (isArabicUi) "مؤقت الخشوع" else "Quiet Focus"}: ${focusSeconds / 60}:${String.format("%02d", focusSeconds % 60)}",
                    style = MaterialTheme.typography.titleMedium
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
    Surface(
        onClick = onToggle,
        color = if (isNext) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${entry.key}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isArabicUi) entry.nameAr else entry.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isArabicUi) {
                            Spacer(Modifier.width(8.dp))
                            Text(entry.nameAr, fontFamily = AmiriFontFamily, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(entry.sunnahInfo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
