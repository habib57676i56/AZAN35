package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.data.firebase.PrayerLogDoc
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.CalculationMethod
import com.example.domain.IslamicLibraryData
import com.example.domain.PrayerTimeEntry
import com.example.domain.PrayerTimesEngine
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.SanctuaryEmerald
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrayerScreen(
    viewModel: MihrabViewModel,
    strings: LocalizedStrings,
    useIndic: Boolean,
    onOpenKhutbahAi: () -> Unit
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val logsState by viewModel.prayerLogsState.collectAsStateWithLifecycle()
    val manualOffsets by viewModel.manualOffsets.collectAsStateWithLifecycle()

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
    var showMethodSheet by remember { mutableStateOf(false) }
    var showGuidesSheet by remember { mutableStateOf(false) }
    var showRakahCounterSheet by remember { mutableStateOf(false) }
    var khushuPrePrayerMode by remember { mutableStateOf(false) }

    val isFriday = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY }

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
                                .height(250.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
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
                                        text = formatNumerals(hijriToday.formattedEn(), useIndic),
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
                                        Icon(Icons.Rounded.Explore, null, tint = Color(0xFFE2B95B), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${formatNumerals("${schedule.qiblaBearingDegrees.toInt()}°", useIndic)} Qibla",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            Text(
                                text = strings.nextPrayer.uppercase(),
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
                                        text = "${nextPrayerEntry.nameEn} • ${nextPrayerEntry.nameAr}",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Adhan at ${formatNumerals(nextPrayerEntry.formattedTime, useIndic)}",
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
                                    text = "Imsak: ${formatNumerals(schedule.imsak.formattedTime, useIndic)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Sunrise: ${formatNumerals(schedule.sunrise.formattedTime, useIndic)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Qiyam ⅓: ${formatNumerals(schedule.lastThirdQiyam, useIndic)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFE2B95B)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Action Tools Strip
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { showQiblaSheet = true },
                        label = { Text(strings.qiblaCompass) },
                        leadingIcon = { Icon(Icons.Rounded.Explore, null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { showMethodSheet = true },
                        label = { Text("Method & Offsets") },
                        leadingIcon = { Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { showGuidesSheet = true },
                        label = { Text("Prayer Guides") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { showRakahCounterSheet = true },
                        label = { Text("Rak'ah & Focus") },
                        leadingIcon = { Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { khushuPrePrayerMode = !khushuPrePrayerMode },
                        label = { Text("Khushu' Reflection") },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Khushu' Pre-Prayer Reflection & Friday Sunnah Banner
            if (khushuPrePrayerMode) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Khushu' Reflection Before Prayer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Pause for 10 seconds. Imagine standing before the King of Kings as if this were your farewell prayer. Silence worldly notifications.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val rating = (todayLog?.khushuRating ?: 3L).toInt()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Today's presence: ", style = MaterialTheme.typography.labelLarge)
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
                            Text("Jumu'ah Mubarak • Friday Sunnahs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("• Ghusl & clean fragrance • Recite Surah Al-Kahf • Abundant Salawat upon the Prophet ﷺ • Seek the final hour before Maghrib for answered Dua.")
                            OutlinedButton(onClick = onOpenKhutbahAi) {
                                Text("Summarize Today's Khutbah with AI")
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
                                Text(strings.prayerTracker, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    formatNumerals("$completedCount / 5 Obligatory Completed", useIndic),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AirplanemodeActive, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text("Qasr", style = MaterialTheme.typography.labelLarge)
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
                            PrayerRowItem(
                                entry = entry,
                                isCompleted = checked,
                                isNext = nextPrayerEntry.key == entry.key,
                                useIndic = useIndic,
                                onToggle = { viewModel.togglePrayerToday(key, todayLog) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Sunnah & Nawafil Schedule", style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = todayLog?.sunnahRawatib == true,
                                onClick = { viewModel.togglePrayerToday("sunnahRawatib", todayLog) },
                                label = { Text("12 Rawatib") }
                            )
                            FilterChip(
                                selected = todayLog?.duha == true,
                                onClick = { viewModel.togglePrayerToday("duha", todayLog) },
                                label = { Text("Duha") }
                            )
                            FilterChip(
                                selected = todayLog?.witr == true,
                                onClick = { viewModel.togglePrayerToday("witr", todayLog) },
                                label = { Text("Witr") }
                            )
                            FilterChip(
                                selected = todayLog?.qiyam == true,
                                onClick = { viewModel.togglePrayerToday("qiyam", todayLog) },
                                label = { Text("Qiyam / Tahajjud") }
                            )
                        }
                    }
                }
            }

            // 4. Qada Missed Prayers Counter & Weekly Consistency
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.qadaCounter, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Track and gradually fulfill missed obligatory prayers with ease.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val qadaItems = listOf(
                            Triple("Fajr", "qadaFajr", settings.qadaFajr),
                            Triple("Dhuhr", "qadaDhuhr", settings.qadaDhuhr),
                            Triple("Asr", "qadaAsr", settings.qadaAsr),
                            Triple("Maghrib", "qadaMaghrib", settings.qadaMaghrib),
                            Triple("Isha", "qadaIsha", settings.qadaIsha)
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
                    "Nearby Mosques & Iqamah (${settings.city})",
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
                            "Iqamah: Fajr ${mosque.iqamahFajr} • Dhuhr ${mosque.iqamahDhuhr} • Asr ${mosque.iqamahAsr} • Maghrib ${mosque.iqamahMaghrib} • Isha ${mosque.iqamahIsha}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Jumu'ah Khutbah: ${mosque.jummahKhutbah}",
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

    if (showQiblaSheet) {
        ModalBottomSheet(onDismissRequest = { showQiblaSheet = false }) {
            QiblaAndLocationDialogContent(
                currentCity = settings.city,
                qiblaBearing = schedule.qiblaBearingDegrees,
                distanceKm = schedule.distanceToKaabaKm,
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
                Text("Calculation Method & Manual Offsets", style = MaterialTheme.typography.headlineMedium)
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
                Text("Manual Minute Adjustments", style = MaterialTheme.typography.titleMedium)
                listOf("fajr" to "Fajr", "dhuhr" to "Dhuhr", "asr" to "Asr", "maghrib" to "Maghrib", "isha" to "Isha").forEach { (key, label) ->
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
                    Text("Done")
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
                Text("Salah & Special Prayer Guides", style = MaterialTheme.typography.headlineMedium)
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
                Text("Rak'ah Counter & Worship Focus", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tap each Sujud or Rak'ah to prevent forgetfulness while praying.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rak'ah", style = MaterialTheme.typography.labelLarge)
                            Text("$rakahCount / 4", style = MaterialTheme.typography.displayMedium)
                            Button(onClick = { rakahCount = (rakahCount + 1) % 5 }) {
                                Text("+1 Rak'ah")
                            }
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sujud", style = MaterialTheme.typography.labelLarge)
                            Text("$sujudCount / 2", style = MaterialTheme.typography.displayMedium)
                            Button(onClick = {
                                if (sujudCount == 1) {
                                    sujudCount = 0
                                    rakahCount = (rakahCount + 1) % 5
                                } else {
                                    sujudCount = 1
                                }
                            }) {
                                Text("Log Sujud")
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text("Quiet Worship Focus Timer: ${focusSeconds / 60}:${String.format("%02d", focusSeconds % 60)}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { focusRunning = !focusRunning }) {
                        Text(if (focusRunning) "Pause Timer" else "Start 10-Min Focus")
                    }
                    OutlinedButton(onClick = { focusRunning = false; focusSeconds = 600; rakahCount = 0; sujudCount = 0 }) {
                        Text("Reset")
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
    useIndic: Boolean,
    onToggle: () -> Unit
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
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.nameEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Text(entry.nameAr, fontFamily = AmiriFontFamily, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(entry.sunnahInfo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
