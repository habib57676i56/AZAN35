package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.material3.TextButton
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.hardware.AdhanAudioPlayer
import com.example.core.hardware.AdhanRuntimeController
import com.example.core.hardware.LauncherShortcutHelper
import com.example.core.hardware.LauncherShortcutTarget
import com.example.core.hardware.LocationAndMosqueLocator
import com.example.core.hardware.MihrabReminderManager
import com.example.core.hardware.MihrabReminderSettings
import com.example.core.hardware.PrayerStatusNotificationManager
import com.example.core.hardware.shareDesignedVerseTextOrCard
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.core.preferences.FeatureUsageTracker
import com.example.core.preferences.SmartFeatureId
import com.example.data.firebase.AdhanAlertMode
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.CalculationMethod
import com.example.domain.HijriCalendarEngine
import com.example.domain.IslamicLibraryData
import com.example.domain.LiveVersesAndHadithsCatalog
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
    onOpenKhatmah: () -> Unit = {},
    onOpenAdhkar: () -> Unit = {},
    onOpenTasbih: () -> Unit = {},
    onOpenAi: (String?) -> Unit = {},
    onOpenHijri: () -> Unit = {},
    onOpenZakat: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalLiquidGlassPalette.current
    val isArabicUi = appLanguage.isArabicUi
    val useArabicDigits = useIndic && isArabicUi
    val listState = rememberLazyListState()

    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefsState.collectAsStateWithLifecycle()
    val logsState by viewModel.prayerLogsState.collectAsStateWithLifecycle()
    val manualOffsets by viewModel.manualOffsets.collectAsStateWithLifecycle()
    val adhanVoices by viewModel.adhanVoicesState.collectAsStateWithLifecycle()
    val adhkarCatalog by viewModel.adhkarCatalogState.collectAsStateWithLifecycle()
    val todayAdhkarLogs by viewModel.todayAdhkarLogsState.collectAsStateWithLifecycle()
    val adhanState by adhanAudioPlayer.state.collectAsStateWithLifecycle()
    val nearbyMosques by viewModel.nearbyMosques.collectAsStateWithLifecycle()
    val nearbyMosquesLoading by viewModel.nearbyMosquesLoading.collectAsStateWithLifecycle()
    val isLocatingGps by viewModel.isLocatingGps.collectAsStateWithLifecycle()
    val locationNotice by viewModel.locationNotice.collectAsStateWithLifecycle()
    val usageVersion by FeatureUsageTracker.countsVersion.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val logs = (logsState as? UiState.Success)?.data ?: emptyList()
    val todayKey = viewModel.todayDateKey()
    val todayLog = logs.firstOrNull { it.dateKey == todayKey }

    val schedule = remember(settings, manualOffsets) {
        viewModel.computeSchedule(settings, manualOffsets)
    }
    val hijriToday = remember(settings) { viewModel.computeHijriToday(settings) }
    val mosques = remember(nearbyMosques, settings.city, schedule, appLanguage) {
        nearbyMosques.ifEmpty { PrayerTimesEngine.getMosquesForCity(settings.city, schedule, appLanguage) }
    }

    LaunchedEffect(settings.city, settings.latitude, settings.longitude, appLanguage) {
        if (appOperations.nearbyMosquesEnabled) {
            viewModel.refreshNearbyMosques(settings, manualOffsets, appLanguage)
        }
    }

    var persistentBarEnabled by remember(context) {
        mutableStateOf(PrayerStatusNotificationManager.isEnabled(context))
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            viewModel.detectGpsAndMosques(context, appLanguage, manualOffsets, settings)
        } else {
            viewModel.showMessage(
                appLanguage.pick(
                    "تم رفض إذن الموقع؛ يمكنك اختيار المدينة يدوياً.",
                    "Location permission denied; you can choose a city manually.",
                    "Joylashuv ruxsati berilmadi; shaharni qo'lda tanlashingiz mumkin."
                )
            )
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            persistentBarEnabled = true
            PrayerStatusNotificationManager.setEnabled(
                context, true, settings, manualOffsets, appLanguage, useIndic, appOperations
            )
        } else {
            viewModel.showMessage(
                appLanguage.pick(
                    "اسمح بالإشعارات لتثبيت مواقيت الصلاة في لوحة الهاتف.",
                    "Allow notifications to pin permanent prayer times in the shade.",
                    "Bildirishnomada doimiy namoz vaqtlarini ko'rsatish uchun ruxsat bering."
                )
            )
        }
    }

    fun requestGpsDetection() {
        val fineOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineOk || coarseOk) {
            viewModel.detectGpsAndMosques(context, appLanguage, manualOffsets, settings)
        } else {
            gpsPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    fun togglePersistentBar() {
        if (persistentBarEnabled) {
            persistentBarEnabled = false
            PrayerStatusNotificationManager.setEnabled(
                context, false, settings, manualOffsets, appLanguage, useIndic, appOperations
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                persistentBarEnabled = true
                PrayerStatusNotificationManager.setEnabled(
                    context, true, settings, manualOffsets, appLanguage, useIndic, appOperations
                )
            }
        }
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
                        viewModel.showMessage(
                            err.localizedMessage ?: appLanguage.pick(
                                "تعذر استيراد ملف الأذان",
                                "Unable to import Adhan audio",
                                "Azon faylini yuklab bo'lmadi"
                            )
                        )
                    }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
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
                                        text = formatNumerals(hijriToday.formattedFor(appLanguage), useArabicDigits),
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
                                    onClick = { requestGpsDetection() },
                                    color = Color.White.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("hero_gps_locate_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isLocatingGps) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFFF6D98E)
                                            )
                                        } else {
                                            Icon(Icons.Rounded.MyLocation, null, tint = Color(0xFFF6D98E), modifier = Modifier.size(15.dp))
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isLocatingGps) strings.locatingGps else appLanguage.pick("موقعي", "GPS", "GPS"),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    onClick = {
                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.QIBLA)
                                        showQiblaSheet = true
                                    },
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
                                            text = "${formatNumerals("${schedule.qiblaBearingDegrees.toInt()}°", useArabicDigits)} ${appLanguage.pick("القبلة", "Qibla", "Qibla")}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            if (!locationNotice.isNullOrBlank()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.28f),
                                    border = BorderStroke(1.dp, Color(0xFFF6D98E).copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = locationNotice!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!LocationAndMosqueLocator.isLocationEnabled(context)) {
                                            TextButton(
                                                onClick = {
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        )
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    appLanguage.pick("تفعيل GPS", "Enable GPS", "GPS yoqish"),
                                                    color = Color(0xFFF6D98E),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { viewModel.clearLocationNotice() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
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
                                        text = if (isArabicUi) nextPrayerEntry.nameAr else "${nextPrayerEntry.nameFor(appLanguage)} • ${nextPrayerEntry.nameAr}",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = appLanguage.pick(
                                            "الأذان الساعة ${formatNumerals(nextPrayerEntry.formattedTime, useArabicDigits)}",
                                            "Adhan at ${formatNumerals(nextPrayerEntry.formattedTime, false)}",
                                            "Azon vaqti: ${nextPrayerEntry.formattedTime}"
                                        ),
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
                                            text = formatNumerals(countdownStr, useArabicDigits),
                                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = appLanguage.pick("المتبقي", "Remaining", "Qoldi"),
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
                                    label = schedule.imsak.nameFor(appLanguage),
                                    value = formatNumerals(schedule.imsak.formattedTime, useArabicDigits),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroTimePill(
                                    label = schedule.sunrise.nameFor(appLanguage),
                                    value = formatNumerals(schedule.sunrise.formattedTime, useArabicDigits),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroTimePill(
                                    label = appLanguage.pick("القيام ⅓", "Qiyam ⅓", "Tahajjud ⅓"),
                                    value = formatNumerals(schedule.lastThirdQiyam, useArabicDigits),
                                    highlight = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 1b. White Days (الأيام البيض) Countdown Strip Card
            item {
                val whiteDays = remember(hijriToday, settings.hijriOffset, appLanguage) {
                    HijriCalendarEngine.computeWhiteDaysCountdown(
                        todayGregorian = Calendar.getInstance(),
                        offsetDays = settings.hijriOffset.toInt(),
                        language = appLanguage
                    )
                }
                val whiteStatusBadge = if (whiteDays.isTodayWhiteDay) {
                    appLanguage.pick(
                        "اليوم ${whiteDays.currentWhiteDayNumber ?: 13} من الأيام البيض",
                        "White Day ${whiteDays.currentWhiteDayNumber ?: 13} Today",
                        "Bugun Oq kun (${whiteDays.currentWhiteDayNumber ?: 13})"
                    )
                } else {
                    appLanguage.pick(
                        "متبقي ${whiteDays.daysRemaining} أيام",
                        "In ${whiteDays.daysRemaining} days",
                        "${whiteDays.daysRemaining} kun qoldi"
                    )
                }
                LiquidGlassCard(
                    onClick = { onOpenHijri() },
                    shape = RoundedCornerShape(22.dp),
                    containerColor = if (whiteDays.isTodayWhiteDay) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        glass.glassFillStrong
                    },
                    borderColor = glass.accentGold.copy(alpha = 0.45f),
                    contentPadding = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_white_days_card")
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
                            Icon(
                                imageVector = Icons.Rounded.NightsStay,
                                contentDescription = null,
                                tint = glass.accentGold
                            )
                            Column {
                                Text(
                                    text = appLanguage.pick(
                                        "الأيام البيض • ${whiteDays.monthNameFor(appLanguage)} (١٣-١٥)",
                                        "White Days • ${whiteDays.monthNameFor(appLanguage)} (13–15)",
                                        "Oq kunlar • ${whiteDays.monthNameFor(appLanguage)} (13–15)"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatNumerals(whiteDays.gregorianRange, useArabicDigits),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = glass.accentGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, glass.accentGold.copy(alpha = 0.45f)),
                            shape = CircleShape
                        ) {
                            Text(
                                text = formatNumerals(whiteStatusBadge, useArabicDigits),
                                style = MaterialTheme.typography.labelMedium.withTabularNums(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
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
                            onClick = {
                                FeatureUsageTracker.recordUsage(context, SmartFeatureId.MUSHAF)
                                onOpenMushaf()
                            },
                            icon = Icons.Rounded.AutoStories,
                            label = appLanguage.pick(
                                "متابعة المصحف (${formatNumerals(settings.lastReadPage.toString(), useArabicDigits)})",
                                "Mushaf p.${settings.lastReadPage}",
                                "Mushaf ${settings.lastReadPage}-bet"
                            )
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = { requestGpsDetection() },
                            icon = Icons.Rounded.MyLocation,
                            label = strings.detectMyLocation
                        )
                    }
                    if (appOperations.persistentPrayerNotificationAllowed) {
                        item {
                            LiquidGlassPill(
                                selected = persistentBarEnabled,
                                onClick = { togglePersistentBar() },
                                icon = Icons.Rounded.Schedule,
                                label = appLanguage.pick(
                                    if (persistentBarEnabled) "شريط الصلاة: مفعّل" else "تثبيت الصلاة بالإشعارات",
                                    if (persistentBarEnabled) "Shade Bar: ON" else "Pin Prayer Bar",
                                    if (persistentBarEnabled) "Bildirishnoma: Yoqilgan" else "Namoz panelini yoqish"
                                ),
                                modifier = Modifier.testTag("toggle_persistent_bar_pill")
                            )
                        }
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = {
                                FeatureUsageTracker.recordUsage(context, SmartFeatureId.QIBLA)
                                showQiblaSheet = true
                            },
                            icon = MihrabIcons.Qibla,
                            label = strings.qiblaCompass
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = {
                                FeatureUsageTracker.recordUsage(context, SmartFeatureId.TASBIH)
                                onOpenTasbih()
                            },
                            icon = MihrabIcons.TasbihBeads,
                            label = strings.digitalTasbih
                        )
                    }
                    if (appOperations.aiAssistantEnabled) {
                        item {
                            LiquidGlassPill(
                                selected = false,
                                onClick = {
                                    FeatureUsageTracker.recordUsage(context, SmartFeatureId.AI_COMPANION)
                                    onOpenAi(null)
                                },
                                icon = MihrabIcons.AiSpark,
                                label = appLanguage.pick("المساعد الذكي", "Islamic AI", "Islomiy AI")
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
                            label = appLanguage.pick("الحساب والتعديل", "Method & Offsets", "Hisob va tuzatish")
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = { showGuidesSheet = true },
                            icon = MihrabIcons.Guides,
                            label = appLanguage.pick("أحكام الصلوات", "Prayer Guides", "Namoz qo'llanmasi")
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = false,
                            onClick = {
                                FeatureUsageTracker.recordUsage(context, SmartFeatureId.RAKAH)
                                showRakahCounterSheet = true
                            },
                            icon = MihrabIcons.RakahTimer,
                            label = appLanguage.pick("عدّاد الركعات", "Rak'ah & Focus", "Rakat sanagich")
                        )
                    }
                    item {
                        LiquidGlassPill(
                            selected = khushuPrePrayerMode,
                            onClick = { khushuPrePrayerMode = !khushuPrePrayerMode },
                            icon = MihrabIcons.KhushuStar,
                            label = appLanguage.pick("تأمل الخشوع", "Khushu'", "Xushu' tafakkuri")
                        )
                    }
                }
            }

            // 3. Daily Worship Bento Grid + Ayah of the Day + Live Rotating Ayahs & Hadiths
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
                            onClick = {
                                FeatureUsageTracker.recordUsage(context, SmartFeatureId.MUSHAF)
                                onOpenMushaf()
                            },
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
                                    centerLabel = formatNumerals("${(khatmahRatio * 100).toInt()}%", useArabicDigits)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appLanguage.pick("الورد والختمة", "Daily Wird", "Kunlik vird"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatNumerals("$pagesRead / 604", useArabicDigits),
                                        style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = appLanguage.pick(
                                            "صفحة ${formatNumerals(settings.lastReadPage.toString(), useArabicDigits)}",
                                            "Last p.${settings.lastReadPage}",
                                            "Oxirgi ${settings.lastReadPage}-bet"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        LiquidGlassCard(
                            onClick = {
                                FeatureUsageTracker.recordUsage(context, SmartFeatureId.TASBIH)
                                onOpenTasbih()
                            },
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
                                    centerLabel = formatNumerals("$totalAdhkarDone", useArabicDigits)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appLanguage.pick("الأذكار والتسبيح", "Daily Adhkar", "Kunlik zikrlar"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatNumerals("$totalAdhkarDone / $totalAdhkarTarget", useArabicDigits),
                                        style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                        color = glass.accentGold
                                    )
                                    Text(
                                        text = appLanguage.pick("اضغط للسبحة", "Open Tasbih", "Tasbehni ochish"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (appOperations.liveVersesCardEnabled) {
                        val quotes = LiveVersesAndHadithsCatalog.entries
                        var quoteIndex by remember { mutableIntStateOf(0) }
                        var quotePaused by remember { mutableStateOf(false) }

                        LaunchedEffect(quoteIndex, quotePaused) {
                            if (!quotePaused && quotes.isNotEmpty()) {
                                delay(9000L)
                                quoteIndex = (quoteIndex + 1) % quotes.size
                            }
                        }

                        val currentQuote = quotes[quoteIndex % quotes.size]
                        LiquidGlassCard(
                            contentPadding = 14.dp,
                            borderColor = glass.accentGold.copy(alpha = 0.42f),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_verses_hadith_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, null, tint = glass.accentGold, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "${strings.liveVersesTitle} • ${if (currentQuote.isQuran) appLanguage.pick("آية", "Quran", "Oyat") else appLanguage.pick("حديث", "Hadith", "Hadis")}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { quoteIndex = (quoteIndex - 1 + quotes.size) % quotes.size },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { quotePaused = !quotePaused },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            if (quotePaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                            contentDescription = if (quotePaused) "Resume" else "Pause",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { quoteIndex = (quoteIndex + 1) % quotes.size },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            AnimatedContent(targetState = currentQuote, label = "liveSpiritualQuote") { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (item.isQuran) "﴿${item.arabicText}﴾" else "«${item.arabicText}»",
                                        fontFamily = AmiriQuranFontFamily,
                                        fontSize = 19.sp,
                                        lineHeight = 32.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = item.meaningFor(appLanguage),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = item.sourceFor(appLanguage),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.accentGold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val txt = "${currentQuote.arabicText}\n${currentQuote.meaningFor(appLanguage)}\n(${currentQuote.sourceFor(appLanguage)})"
                                        cm.setPrimaryClip(ClipData.newPlainText("Mihrab Quote", txt))
                                        viewModel.showMessage(strings.copiedToast)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.copyAyah, maxLines = 1)
                                }
                                if (appOperations.aiAssistantEnabled) {
                                    OutlinedButton(
                                        onClick = {
                                            val q = appLanguage.pick(
                                                "اشرح لي معنى وفضل: ${currentQuote.arabicText} (${currentQuote.sourceAr})",
                                                "Explain the tafsir/meaning and lessons of: ${currentQuote.arabicText} (${currentQuote.sourceEn})",
                                                "Ushbu oyat/hadisning ma'nosi va foydalarini tushuntirib bering: ${currentQuote.arabicText} (${currentQuote.sourceUz})"
                                            )
                                            onOpenAi(q)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(MihrabIcons.AiSpark, null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(appLanguage.pick("اسأل AI", "Ask AI", "AI dan so'rash"), maxLines = 1)
                                    }
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
                                            text = appLanguage.pick(
                                                "آية اليوم • ${dailySurah.nameAr}",
                                                "Ayah of the Day • ${dailySurah.nameEn}",
                                                "Kun oyati • ${dailySurah.nameEn}"
                                            ),
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
                                                text = strings.shareAyah,
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
                            appLanguage.pick(
                                "استحضار الخشوع قبل الصلاة",
                                "Khushu' Reflection Before Prayer",
                                "Namozdan oldingi xushu' va tafakkur"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            appLanguage.pick(
                                "توقف ١٠ ثوانٍ وتذكّر أنك تقف بين يدي ملك الملوك؛ صلِّ صلاة مودِّع.",
                                "Pause for 10 seconds. Imagine standing before the King of Kings as if this were your farewell prayer.",
                                "10 soniya to'xtab, olamlar Robbisi huzurida turganingizni his qiling; xayrlashuv namozi kabi o'qing."
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val rating = (todayLog?.khushuRating ?: 3L).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                appLanguage.pick("حضور القلب اليوم: ", "Today's presence: ", "Bugungi qalb huzuri: "),
                                style = MaterialTheme.typography.labelLarge
                            )
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
                            appLanguage.pick(
                                "جمعة مباركة • سنن يوم الجمعة",
                                "Jumu'ah Mubarak • Friday Sunnahs",
                                "Juma muborak • Juma sunnatlari"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            appLanguage.pick(
                                "• الغسل والتطيب • قراءة سورة الكهف • الإكثار من الصلاة على النبي ﷺ • تحري ساعة الإجابة قبل المغرب.",
                                "• Ghusl & fragrance • Surah Al-Kahf • Abundant Salawat ﷺ • Seek the final hour before Maghrib.",
                                "• G'usl va xushbo'ylik • Kahf surasi • Ko'p salavot ﷺ • Shomdan oldingi ijobat soatini kuting."
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (appOperations.aiAssistantEnabled) {
                            OutlinedButton(onClick = onOpenKhutbahAi) {
                                Text(
                                    appLanguage.pick(
                                        "تلخيص خطبة الجمعة بالذكاء الاصطناعي",
                                        "Summarize Today's Khutbah with AI",
                                        "Juma xutbasini AI bilan xulosa qilish"
                                    )
                                )
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
                                text = appLanguage.pick(
                                    "${formatNumerals("$completedCount / 5", useArabicDigits)} فرائض • $activeVoiceLabel",
                                    "${formatNumerals("$completedCount / 5", false)} Completed • $activeVoiceLabel",
                                    "$completedCount / 5 farz • $activeVoiceLabel"
                                ),
                                style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AirplanemodeActive, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(appLanguage.pick("قصر", "Qasr", "Qasr"), style = MaterialTheme.typography.labelLarge)
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
                            appLanguage = appLanguage,
                            useArabicDigits = useArabicDigits,
                            onToggle = { viewModel.togglePrayerToday(key, todayLog) },
                            onOpenAdhan = { showAdhanSheet = true }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = glass.glassBorder)
                    Text(
                        appLanguage.pick("السنن الرواتب والنوافل", "Sunnah & Nawafil Schedule", "Sunnat va nafl namozlar"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassPill(
                            selected = todayLog?.sunnahRawatib == true,
                            onClick = { viewModel.togglePrayerToday("sunnahRawatib", todayLog) },
                            label = appLanguage.pick("السنن الرواتب (١٢)", "12 Rawatib", "12 Rakat Sunnat")
                        )
                        LiquidGlassPill(
                            selected = todayLog?.duha == true,
                            onClick = { viewModel.togglePrayerToday("duha", todayLog) },
                            label = appLanguage.pick("الضحى", "Duha", "Zuho")
                        )
                        LiquidGlassPill(
                            selected = todayLog?.witr == true,
                            onClick = { viewModel.togglePrayerToday("witr", todayLog) },
                            label = appLanguage.pick("الوتر", "Witr", "Vitr")
                        )
                        LiquidGlassPill(
                            selected = todayLog?.qiyam == true,
                            onClick = { viewModel.togglePrayerToday("qiyam", todayLog) },
                            label = appLanguage.pick("قيام الليل", "Qiyam / Tahajjud", "Tahajjud")
                        )
                    }
                }
            }

            // 4b. Prayer Entry Alerts, Volume-Key Adhan Mute, Morning/Evening Adhkar & Tahajjud Reminders
            item {
                val reminderFlowState by MihrabReminderManager.settingsFlow.collectAsStateWithLifecycle()
                val reminderState = reminderFlowState ?: remember(context) {
                    MihrabReminderManager.getSettings(context)
                }

                fun updateReminders(updater: (MihrabReminderSettings) -> MihrabReminderSettings) {
                    MihrabReminderManager.updateSettings(context, updater)
                    MihrabReminderManager.syncAndScheduleAll(
                        context = context,
                        settings = settings,
                        offsets = manualOffsets,
                        language = appLanguage,
                        useIndic = useIndic,
                        customAdhanPath = localPrefs?.customAdhanLocalPath.orEmpty()
                    )
                }

                LiquidGlassCard(
                    shape = RoundedCornerShape(26.dp),
                    containerColor = glass.glassFillStrong,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spiritual_reminders_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appLanguage.pick(
                                    "التنبيهات والأذان والتهجد والأذكار",
                                    "Prayer, Adhan, Adhkar & Tahajjud Alerts",
                                    "Namoz, Azon, Zikr va Tahajjud eslatmalari"
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = appLanguage.pick(
                                    "تنبيه دخول الصلاة على شاشة القفل مع إطفاء الأذان بزر الصوت.",
                                    "Lock-screen Adhan controls (press Volume key to silence) & daily reminders.",
                                    "Qulf ekranida azon boshqaruvi (ovoz tugmasi bilan o'chirish) va kundalik eslatmalar."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = glass.glassBorder)

                    // 1. Prayer Entry & Lock-Screen Adhan Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                appLanguage.pick(
                                    "إشعار دخول وقت الصلاة والأذان",
                                    "Prayer Entry Notification & Adhan",
                                    "Namoz vaqti kirishi va azon bildirishnomasi"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                appLanguage.pick(
                                    "يظهر في شاشة القفل وشريط التحكم وتطفئه بزر الصوت",
                                    "Appears on Lock Screen & Shade; press Volume keys to mute",
                                    "Qulf ekrani va boshqaruv panelida ko'rinadi"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderState.prayerEntryAlertsEnabled,
                            onCheckedChange = { enabled ->
                                updateReminders { it.copy(prayerEntryAlertsEnabled = enabled) }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val nextMode = AdhanAlertMode.fromId(settings.perPrayerAdhanModes[nextPrayerEntry.key])
                                AdhanRuntimeController.triggerAdhan(
                                    context = context,
                                    prayerKey = nextPrayerEntry.key,
                                    prayerLabel = nextPrayerEntry.nameFor(appLanguage),
                                    prayerTime = formatNumerals(nextPrayerEntry.formattedTime, useArabicDigits),
                                    voiceId = settings.adhanVoiceId,
                                    mode = if (nextMode == AdhanAlertMode.SILENT) AdhanAlertMode.TAKBIR_ONLY else nextMode,
                                    customLocalPath = localPrefs?.customAdhanLocalPath.orEmpty(),
                                    language = appLanguage
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_lockscreen_adhan_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            appLanguage.pick(
                                "تجربة إشعار الأذان (شاشة القفل + شريط التحكم + زر الصوت)",
                                "Test Lock-Screen Adhan Alert (Volume Key Mutes)",
                                "Qulf ekrani azonini sinash (Ovoz tugmasi o'chiradi)"
                            )
                        )
                    }

                    HorizontalDivider(color = glass.glassBorder)

                    // 2. Morning Adhkar Reminder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.WbSunny, null, tint = glass.accentGold, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    appLanguage.pick("تذكير أذكار الصباح", "Morning Adhkar Reminder", "Tonggi zikrlar eslatmasi"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    formatNumerals(
                                        String.format("%02d:%02d", reminderState.adhkarMorningHour, reminderState.adhkarMorningMinute),
                                        useArabicDigits
                                    ),
                                    style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { MihrabReminderManager.sendAdhkarReminderNow(context, isMorning = true, language = appLanguage) }
                            ) {
                                Text(appLanguage.pick("تجربة", "Test", "Sinash"))
                            }
                            Switch(
                                checked = reminderState.adhkarMorningEnabled,
                                onCheckedChange = { enabled ->
                                    updateReminders { it.copy(adhkarMorningEnabled = enabled) }
                                }
                            )
                        }
                    }
                    if (reminderState.adhkarMorningEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5 to 30, 6 to 0, 6 to 30, 7 to 0).forEach { (h, m) ->
                                val selected = reminderState.adhkarMorningHour == h && reminderState.adhkarMorningMinute == m
                                val lbl = formatNumerals(String.format("%02d:%02d", h, m), useArabicDigits)
                                LiquidGlassPill(
                                    selected = selected,
                                    onClick = {
                                        updateReminders { it.copy(adhkarMorningHour = h, adhkarMorningMinute = m) }
                                    },
                                    label = lbl
                                )
                            }
                        }
                    }

                    // 3. Evening Adhkar Reminder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.NightsStay, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    appLanguage.pick("تذكير أذكار المساء", "Evening Adhkar Reminder", "Kechki zikrlar eslatmasi"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    formatNumerals(
                                        String.format("%02d:%02d", reminderState.adhkarEveningHour, reminderState.adhkarEveningMinute),
                                        useArabicDigits
                                    ),
                                    style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { MihrabReminderManager.sendAdhkarReminderNow(context, isMorning = false, language = appLanguage) }
                            ) {
                                Text(appLanguage.pick("تجربة", "Test", "Sinash"))
                            }
                            Switch(
                                checked = reminderState.adhkarEveningEnabled,
                                onCheckedChange = { enabled ->
                                    updateReminders { it.copy(adhkarEveningEnabled = enabled) }
                                }
                            )
                        }
                    }
                    if (reminderState.adhkarEveningEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(16 to 30, 17 to 0, 17 to 30, 18 to 0).forEach { (h, m) ->
                                val selected = reminderState.adhkarEveningHour == h && reminderState.adhkarEveningMinute == m
                                val lbl = formatNumerals(String.format("%02d:%02d", h, m), useArabicDigits)
                                LiquidGlassPill(
                                    selected = selected,
                                    onClick = {
                                        updateReminders { it.copy(adhkarEveningHour = h, adhkarEveningMinute = m) }
                                    },
                                    label = lbl
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = glass.glassBorder)

                    // 4. Tahajjud & Qiyam al-Layl Reminder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(MihrabIcons.KhushuStar, null, tint = glass.accentGold, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    appLanguage.pick("تذكير صلاة التهجد وقيام الليل", "Tahajjud & Qiyam Reminder", "Tahajjud va tungi namoz eslatmasi"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    appLanguage.pick(
                                        "بداية الثلث الأخير اليوم: ${formatNumerals(schedule.lastThirdQiyam, useArabicDigits)}",
                                        "Tonight's Last Third starts ${schedule.lastThirdQiyam}",
                                        "Kechaning oxirgi uchdan biri: ${schedule.lastThirdQiyam}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                    color = glass.accentGold
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { MihrabReminderManager.sendTahajjudReminderNow(context, appLanguage) }
                            ) {
                                Text(appLanguage.pick("تجربة", "Test", "Sinash"))
                            }
                            Switch(
                                checked = reminderState.tahajjudReminderEnabled,
                                onCheckedChange = { enabled ->
                                    updateReminders { it.copy(tahajjudReminderEnabled = enabled) }
                                }
                            )
                        }
                    }
                    if (reminderState.tahajjudReminderEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                0 to appLanguage.pick("بداية الثلث الأخير", "Last Third Start", "Oxirgi uchdan bir"),
                                45 to appLanguage.pick("قبل الفجر بـ ٤٥ د", "45m Before Fajr", "Bomdoddan 45m oldin"),
                                60 to appLanguage.pick("قبل الفجر بـ ٦٠ د", "60m Before Fajr", "Bomdoddan 60m oldin")
                            ).forEach { (offsetMin, label) ->
                                LiquidGlassPill(
                                    selected = reminderState.tahajjudOffsetBeforeFajrMin == offsetMin,
                                    onClick = {
                                        updateReminders { it.copy(tahajjudOffsetBeforeFajrMin = offsetMin) }
                                    },
                                    label = label
                                )
                            }
                        }
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
                        appLanguage.pick(
                            "تابع قضاء الصلوات الفائتة وأتمها بالتدريج.",
                            "Track and gradually fulfill missed obligatory prayers with ease.",
                            "Qazo namozlaringizni tartib bilan hisoblab, ado etib boring."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val qadaItems = listOf(
                        Triple(schedule.fajr.nameFor(appLanguage), "qadaFajr", settings.qadaFajr),
                        Triple(schedule.dhuhr.nameFor(appLanguage), "qadaDhuhr", settings.qadaDhuhr),
                        Triple(schedule.asr.nameFor(appLanguage), "qadaAsr", settings.qadaAsr),
                        Triple(schedule.maghrib.nameFor(appLanguage), "qadaMaghrib", settings.qadaMaghrib),
                        Triple(schedule.isha.nameFor(appLanguage), "qadaIsha", settings.qadaIsha)
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
                                    text = formatNumerals(value.toString(), useArabicDigits),
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

            // 6. Real GPS Nearby Mosques & Iqamah Times
            if (appOperations.nearbyMosquesEnabled) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${strings.nearbyMosquesTitle} (${settings.city})",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                appLanguage.pick(
                                    "تحديث مباشر عبر الخريطة المفتوحة (OpenStreetMap) مع المسار",
                                    "Live OpenStreetMap GPS search with turn-by-turn directions",
                                    "OpenStreetMap orqali real GPS masjidlar qidiruvi va yo'nalish"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshNearbyMosques(settings, manualOffsets, appLanguage) }
                        ) {
                            if (nearbyMosquesLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh mosques")
                            }
                        }
                    }
                }
                items(mosques) { mosque ->
                    LiquidGlassCard(
                        onClick = {
                            FeatureUsageTracker.recordUsage(context, SmartFeatureId.MOSQUES)
                            LocationAndMosqueLocator.openMosqueDirections(context, mosque)
                        },
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
                                Column {
                                    Text(mosque.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (mosque.address.isNotBlank()) {
                                        Text(
                                            mosque.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Surface(
                                color = if (mosque.isRealOsmData) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else glass.glassSubtleFill,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Directions, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text(
                                        formatNumerals(mosque.distanceLabel, useArabicDigits),
                                        style = MaterialTheme.typography.labelLarge.withTabularNums(),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Text(
                            formatNumerals(
                                appLanguage.pick(
                                    "الإقامة: الفجر ${mosque.iqamahFajr} • الظهر ${mosque.iqamahDhuhr} • العصر ${mosque.iqamahAsr} • المغرب ${mosque.iqamahMaghrib} • العشاء ${mosque.iqamahIsha}",
                                    "Iqamah: Fajr ${mosque.iqamahFajr} • Dhuhr ${mosque.iqamahDhuhr} • Asr ${mosque.iqamahAsr} • Maghrib ${mosque.iqamahMaghrib} • Isha ${mosque.iqamahIsha}",
                                    "Iqoma: Bomdod ${mosque.iqamahFajr} • Peshin ${mosque.iqamahDhuhr} • Asr ${mosque.iqamahAsr} • Shom ${mosque.iqamahMaghrib} • Xufton ${mosque.iqamahIsha}"
                                ),
                                useArabicDigits
                            ),
                            style = MaterialTheme.typography.bodyMedium.withTabularNums()
                        )
                        Text(
                            formatNumerals(
                                appLanguage.pick(
                                    "خطبة الجمعة: ${mosque.jummahKhutbah}",
                                    "Jumu'ah Khutbah: ${mosque.jummahKhutbah}",
                                    "Juma xutbasi: ${mosque.jummahKhutbah}"
                                ),
                                useArabicDigits
                            ),
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

            // 7. Bottom Smart Frequently-Used Shortcuts & Home Screen Pinning
            if (appOperations.smartShortcutsEnabled) {
                item {
                    val topFeatures = remember(usageVersion, context) {
                        FeatureUsageTracker.getTopFeatures(context, 5)
                    }

                    fun launchSmartFeature(fid: SmartFeatureId) {
                        FeatureUsageTracker.recordUsage(context, fid)
                        when (fid) {
                            SmartFeatureId.MUSHAF -> onOpenMushaf()
                            SmartFeatureId.TASBIH -> onOpenTasbih()
                            SmartFeatureId.ADHKAR -> onOpenAdhkar()
                            SmartFeatureId.AI_COMPANION -> onOpenAi(null)
                            SmartFeatureId.QIBLA -> showQiblaSheet = true
                            SmartFeatureId.MOSQUES -> scope.launch {
                                val targetIndex = (listState.layoutInfo.totalItemsCount - 2).coerceAtLeast(0)
                                listState.animateScrollToItem(targetIndex)
                            }
                            SmartFeatureId.KHATMAH -> onOpenKhatmah()
                            SmartFeatureId.ZAKAT -> onOpenZakat()
                            SmartFeatureId.HIJRI -> onOpenHijri()
                            SmartFeatureId.RAKAH -> showRakahCounterSheet = true
                        }
                    }

                    fun pinTargetForFeature(fid: SmartFeatureId): LauncherShortcutTarget? = when (fid) {
                        SmartFeatureId.MUSHAF, SmartFeatureId.KHATMAH -> LauncherShortcutTarget.QURAN
                        SmartFeatureId.TASBIH, SmartFeatureId.ADHKAR -> LauncherShortcutTarget.TASBIH
                        SmartFeatureId.AI_COMPANION -> LauncherShortcutTarget.AI
                        SmartFeatureId.QIBLA, SmartFeatureId.MOSQUES -> LauncherShortcutTarget.PRAYER
                        else -> null
                    }

                    LiquidGlassCard(
                        shape = RoundedCornerShape(26.dp),
                        borderColor = glass.accentGold.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_smart_shortcuts_card")
                    ) {
                        Text(strings.smartShortcutsTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            appLanguage.pick(
                                "أكثر المزايا التي تستخدمها للوصول السريع وتثبيتها على شاشة الهاتف.",
                                "Your most frequently used tools, ranked automatically with 1-tap home screen pinning.",
                                "Eng ko'p foydalanilgan bo'limlaringiz va telefon ekraniga yorliq sifatida qo'shish."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        topFeatures.forEach { ranked ->
                            val icon = when (ranked.feature) {
                                SmartFeatureId.MUSHAF -> Icons.Rounded.AutoStories
                                SmartFeatureId.TASBIH -> MihrabIcons.TasbihBeads
                                SmartFeatureId.ADHKAR -> Icons.Rounded.VolunteerActivism
                                SmartFeatureId.AI_COMPANION -> MihrabIcons.AiSpark
                                SmartFeatureId.QIBLA -> Icons.Rounded.Explore
                                SmartFeatureId.MOSQUES -> Icons.Rounded.Mosque
                                SmartFeatureId.KHATMAH -> Icons.Rounded.AutoStories
                                SmartFeatureId.ZAKAT -> Icons.Rounded.Schedule
                                SmartFeatureId.HIJRI -> Icons.Rounded.CalendarMonth
                                SmartFeatureId.RAKAH -> MihrabIcons.RakahTimer
                            }
                            val pinTarget = pinTargetForFeature(ranked.feature)
                            Surface(
                                onClick = { launchSmartFeature(ranked.feature) },
                                color = glass.glassSubtleFill,
                                border = BorderStroke(1.dp, glass.glassBorder),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text(
                                                ranked.feature.labelFor(appLanguage),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (ranked.count > 0) {
                                                Text(
                                                    appLanguage.pick(
                                                        "استُخدم ${formatNumerals(ranked.count.toString(), useArabicDigits)} مرة",
                                                        "Opened ${ranked.count}×",
                                                        "${ranked.count} marta ochilgan"
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (pinTarget != null && LauncherShortcutHelper.isPinSupported(context)) {
                                        IconButton(
                                            onClick = {
                                                val ok = LauncherShortcutHelper.requestPinShortcut(context, pinTarget, appLanguage)
                                                viewModel.showMessage(
                                                    if (ok) appLanguage.pick(
                                                        "أُرسل طلب إضافة الاختصار للشاشة الرئيسية",
                                                        "Home screen shortcut pin requested",
                                                        "Bosh ekranga yorliq qo'shish so'raldi"
                                                    ) else appLanguage.pick(
                                                        "مشغّل النظام لا يدعم التثبيت التلقائي",
                                                        "Launcher does not support pinned shortcuts",
                                                        "Qurilma yorliq qo'shishni qo'llab-quvvatlamaydi"
                                                    )
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Rounded.PushPin, contentDescription = strings.pinShortcutsTitle, tint = glass.accentGold)
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
                                        text = "${appLanguage.pick("الملف المختار", "Selected", "Tanlangan fayl")}: ${settings.customAdhanName}",
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
                    appLanguage.pick("تخصيص تنبيه كل صلاة", "Per-Prayer Notification Mode", "Har bir namoz uchun bildirishnoma turi"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val fivePrayers = listOf(
                    "fajr" to schedule.fajr.nameFor(appLanguage),
                    "dhuhr" to schedule.dhuhr.nameFor(appLanguage),
                    "asr" to schedule.asr.nameFor(appLanguage),
                    "maghrib" to schedule.maghrib.nameFor(appLanguage),
                    "isha" to schedule.isha.nameFor(appLanguage)
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
                                    label = mode.labelFor(appLanguage)
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
                    Text(appLanguage.pick("تم", "Done", "Tayyor"))
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
                appLanguage = appLanguage,
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
                    appLanguage.pick("طريقة الحساب والتعديلات اليدوية", "Calculation Method & Manual Offsets", "Hisoblash usuli va qo'lda tuzatish"),
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
                Text(
                    appLanguage.pick("تعديل الدقائق يدوياً", "Manual Minute Adjustments", "Daqiqalarni qo'lda moslash"),
                    style = MaterialTheme.typography.titleMedium
                )
                listOf(
                    "fajr" to schedule.fajr.nameFor(appLanguage),
                    "dhuhr" to schedule.dhuhr.nameFor(appLanguage),
                    "asr" to schedule.asr.nameFor(appLanguage),
                    "maghrib" to schedule.maghrib.nameFor(appLanguage),
                    "isha" to schedule.isha.nameFor(appLanguage)
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
                    Text(appLanguage.pick("تم", "Done", "Tayyor"))
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
                Text(
                    appLanguage.pick("أدلة الصلوات والسنن", "Salah & Special Prayer Guides", "Namoz va sunnatlar qo'llanmasi"),
                    style = MaterialTheme.typography.headlineMedium
                )
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
                Text(
                    appLanguage.pick("عدّاد الركعات ومؤقت الخشوع", "Rak'ah Counter & Worship Focus", "Rakat sanagich va xushu' taymeri"),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    appLanguage.pick(
                        "اضغط بعد كل سجدة أو ركعة لتفادي السهو في الصلاة.",
                        "Tap each Sujud or Rak'ah to prevent forgetfulness while praying.",
                        "Namozda adashmaslik uchun har sajda yoki rakatdan so'ng bosing."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(appLanguage.pick("الركعات", "Rak'ah", "Rakat"), style = MaterialTheme.typography.labelLarge)
                            Text(formatNumerals("$rakahCount / 4", useArabicDigits), style = MaterialTheme.typography.displayMedium.withTabularNums())
                            Button(onClick = { rakahCount = (rakahCount + 1) % 5 }) {
                                Text(appLanguage.pick("+١ ركعة", "+1 Rak'ah", "+1 Rakat"))
                            }
                        }
                    }
                    LiquidGlassCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(appLanguage.pick("السجود", "Sujud", "Sajda"), style = MaterialTheme.typography.labelLarge)
                            Text(formatNumerals("$sujudCount / 2", useArabicDigits), style = MaterialTheme.typography.displayMedium.withTabularNums())
                            Button(onClick = {
                                if (sujudCount == 1) {
                                    sujudCount = 0
                                    rakahCount = (rakahCount + 1) % 5
                                } else {
                                    sujudCount = 1
                                }
                            }) {
                                Text(appLanguage.pick("تسجيل سجدة", "Log Sujud", "Sajdani belgilash"))
                            }
                        }
                    }
                }
                HorizontalDivider(color = glass.glassBorder)
                Text(
                    "${appLanguage.pick("مؤقت الخشوع", "Quiet Focus", "Xushu' taymeri")}: ${formatNumerals("${focusSeconds / 60}:${String.format("%02d", focusSeconds % 60)}", useArabicDigits)}",
                    style = MaterialTheme.typography.titleMedium.withTabularNums()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { focusRunning = !focusRunning }) {
                        Text(
                            if (focusRunning) appLanguage.pick("إيقاف", "Pause Timer", "To'xtatish")
                            else appLanguage.pick("بدء ١٠ دقائق", "Start 10-Min Focus", "10 daqiqa boshlash")
                        )
                    }
                    OutlinedButton(onClick = { focusRunning = false; focusSeconds = 600; rakahCount = 0; sujudCount = 0 }) {
                        Text(appLanguage.pick("إعادة", "Reset", "Qayta boshlash"))
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
    appLanguage: AppLanguage,
    useArabicDigits: Boolean,
    onToggle: () -> Unit,
    onOpenAdhan: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val isArabicUi = appLanguage.isArabicUi
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
                            text = entry.nameFor(appLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold
                        )
                        if (!isArabicUi) {
                            Spacer(Modifier.width(8.dp))
                            Text(entry.nameAr, fontFamily = AmiriFontFamily, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        entry.sunnahInfoFor(appLanguage),
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
                        contentDescription = alertMode.labelFor(appLanguage),
                        tint = if (alertMode == AdhanAlertMode.SILENT) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = formatNumerals(entry.formattedTime, useArabicDigits),
                    style = MaterialTheme.typography.titleMedium.withTabularNums(),
                    fontWeight = FontWeight.Bold,
                    color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
