package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.AdhanAudioPlayer
import com.example.core.hardware.RemoteAvatarImage
import com.example.core.hardware.RemoteBannerImage
import com.example.core.hardware.encodeCompressedBannerBitmapFromUri
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.AdminAccess
import com.example.data.firebase.AiRuntimeConfig
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.QuranDefaultsConfig
import com.example.data.firebase.UserSettingsDoc
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.SacredAmbientCanvas
import com.example.ui.theme.withTabularNums
import com.example.ui.viewmodel.AdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    adhanAudioPlayer: AdhanAudioPlayer,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    initialAdminTab: Int = 0,
    adminTabNonce: Int = 0,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val currentUser = Firebase.auth.currentUser

    // Layer 1 Route Guard: Allow Super Admin or non-suspended delegated Admins
    if (!adminViewModel.isAuthorized) {
        SacredAmbientCanvas {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(
                    shape = RoundedCornerShape(26.dp),
                    containerColor = glass.glassFillStrong,
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
                    contentPadding = 24.dp,
                    modifier = Modifier.widthIn(max = 460.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = strings.adminAccessDenied,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onBack, shape = RoundedCornerShape(18.dp)) {
                            Text(appLanguage.pick("رجوع", "Go Back", "Orqaga"))
                        }
                    }
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val isBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()
    val adminMsg by adminViewModel.adminMessage.collectAsStateWithLifecycle()
    val backupJson by adminViewModel.backupExportJson.collectAsStateWithLifecycle()
    val reciters by adminViewModel.recitersAdminState.collectAsStateWithLifecycle()
    val adhkar by adminViewModel.adhkarAdminState.collectAsStateWithLifecycle()
    val adhanVoices by adminViewModel.adhanVoicesAdminState.collectAsStateWithLifecycle()
    val users by adminViewModel.usersAdminState.collectAsStateWithLifecycle()
    val quranDefaults by adminViewModel.quranDefaultsState.collectAsStateWithLifecycle()
    val aiConfig by adminViewModel.aiConfigState.collectAsStateWithLifecycle()
    val appOps by adminViewModel.appOperationsState.collectAsStateWithLifecycle()
    val videos by adminViewModel.videosAdminState.collectAsStateWithLifecycle()

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = backupJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                }
            }
        }
        adminViewModel.clearBackupExport()
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (!raw.isNullOrBlank()) {
                adminViewModel.importFullDatabaseBackup(raw)
            }
        }
    }

    LaunchedEffect(backupJson) {
        if (backupJson != null) {
            exportBackupLauncher.launch("mihrab_full_db_backup.json")
        }
    }

    var adminTab by remember { mutableIntStateOf(initialAdminTab) }
    LaunchedEffect(initialAdminTab, adminTabNonce) {
        adminTab = initialAdminTab
    }
    val tabLabels = listOf(
        strings.adminTabDefaults to MihrabIcons.ScholarBook,
        strings.adminTabReciters to MihrabIcons.AdhanVoice,
        strings.adminTabAdhkar to MihrabIcons.TasbihBeads,
        strings.adminTabAdhan to MihrabIcons.BroadcastBanner,
        strings.adminTabAiControl to MihrabIcons.AiSpark,
        strings.adminTabOperations to MihrabIcons.FeatureFlags,
        appLanguage.pick("الفيديوهات", "Videos", "Videolar") to Icons.Rounded.OndemandVideo
    )

    SacredAmbientCanvas {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
            ) {
                // Frosted Liquid Glass Super Admin Header + KPI Row
                Surface(
                    color = glass.dockBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = BurnishedGold.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = MihrabIcons.AdminShield,
                                            contentDescription = null,
                                            tint = BurnishedGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.adminDashboardTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (AdminAccess.isSuperAdmin(currentUser)) {
                                            "${strings.adminBadgeLabel} • ${AdminAccess.SUPER_ADMIN_EMAIL}"
                                        } else {
                                            "${appLanguage.pick("مسؤول معيّن", "Appointed Admin", "Tayinlangan admin")} • ${currentUser?.email.orEmpty()}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // KPI Glass Summary Strip
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                AdminKpiPill(
                                    label = appLanguage.pick("المستخدمون", "Users", "Foydalanuvchilar"),
                                    value = "${users.size}"
                                )
                            }
                            item {
                                AdminKpiPill(
                                    label = appLanguage.pick("القراء النشطون", "Reciters", "Faol qorilar"),
                                    value = "${reciters.count { it.active }}/${reciters.size}"
                                )
                            }
                            item {
                                AdminKpiPill(
                                    label = appLanguage.pick("الأذكار", "Adhkar", "Zikrlar"),
                                    value = "${adhkar.size}"
                                )
                            }
                            item {
                                AdminKpiPill(
                                    label = appLanguage.pick("الفيديوهات", "Videos", "Videolar"),
                                    value = "${videos.count { it.active }}/${videos.size}"
                                )
                            }
                            item {
                                AdminKpiPill(
                                    label = appLanguage.pick("الذكاء الاصطناعي", "AI", "Sun'iy intellekt"),
                                    value = "${if (aiConfig.enabled) "ON" else "OFF"} • ${aiConfig.dailyLimitPerUser}/d"
                                )
                            }
                        }

                        if (isBusy) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        if (adminMsg != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = adminMsg!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { adminViewModel.clearMessage() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tabLabels.size) { idx ->
                                val (label, icon) = tabLabels[idx]
                                LiquidGlassPill(
                                    selected = adminTab == idx,
                                    onClick = { adminTab = idx },
                                    icon = icon,
                                    label = label,
                                    modifier = Modifier.testTag("admin_tab_$idx")
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = glass.glassBorder, thickness = 0.8.dp)

                when (adminTab) {
                    0 -> QuranDefaultsTab(
                        initialConfig = quranDefaults,
                        appLanguage = appLanguage,
                        strings = strings,
                        onSave = { adminViewModel.saveQuranDefaults(it) }
                    )
                    1 -> RecitersCrudTab(
                        reciters = reciters,
                        appLanguage = appLanguage,
                        strings = strings,
                        onUpsert = { adminViewModel.upsertReciter(it) },
                        onDelete = { adminViewModel.deleteReciter(it) },
                        onSeedDefaults = { adminViewModel.seedDefaultReciters() }
                    )
                    2 -> AdhkarCrudTab(
                        adhkarList = adhkar,
                        appLanguage = appLanguage,
                        strings = strings,
                        onUpsert = { adminViewModel.upsertAdhkar(it) },
                        onDelete = { adminViewModel.deleteAdhkar(it) },
                        onSeedDefaults = { adminViewModel.seedDefaultAdhkar() }
                    )
                    3 -> AdhanAudioCrudTab(
                        voices = adhanVoices,
                        adhanAudioPlayer = adhanAudioPlayer,
                        appLanguage = appLanguage,
                        strings = strings,
                        onUpsert = { adminViewModel.upsertAdhanVoice(it) },
                        onDelete = { adminViewModel.deleteAdhanVoice(it) },
                        onSeedDefaults = { adminViewModel.seedDefaultAdhanVoices() }
                    )
                    4 -> AiControlTab(
                        initialConfig = aiConfig,
                        appLanguage = appLanguage,
                        strings = strings,
                        onSave = { adminViewModel.saveAiConfig(it) }
                    )
                    5 -> AppOperationsTab(
                        initialOps = appOps,
                        users = users,
                        appLanguage = appLanguage,
                        strings = strings,
                        onSaveOperations = { adminViewModel.saveAppOperations(it) },
                        onUpdateUser = { uid, fields -> adminViewModel.updateUserAsAdmin(uid, fields) },
                        onSetUserVerified = { uid, v -> adminViewModel.setUserVerified(uid, v) },
                        onSetUserSuspended = { uid, s -> adminViewModel.setUserSuspended(uid, s) },
                        onSetUserAdminRole = { uid, makeAdmin -> adminViewModel.setUserAdminRole(uid, makeAdmin) },
                        onExportBackup = { adminViewModel.exportFullDatabaseBackup() },
                        onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "text/*")) }
                    )
                    6 -> VideosCrudTab(
                        videos = videos,
                        appLanguage = appLanguage,
                        strings = strings,
                        onUpsert = { adminViewModel.upsertVideo(it) },
                        onDelete = { adminViewModel.deleteVideo(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpiPill(label: String, value: String) {
    val glass = LocalLiquidGlassPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = glass.glassFill,
        border = BorderStroke(1.dp, glass.glassBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.labelLarge.withTabularNums(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuranDefaultsTab(
    initialConfig: QuranDefaultsConfig,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onSave: (QuranDefaultsConfig) -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    var apiPrimary by remember(initialConfig) { mutableStateOf(initialConfig.quranPageApiBase) }
    var apiFallback by remember(initialConfig) { mutableStateOf(initialConfig.quranFallbackApiBase) }
    var audioCdn by remember(initialConfig) { mutableStateOf(initialConfig.audioCdnBase) }
    var defaultReciter by remember(initialConfig) { mutableStateOf(initialConfig.defaultReciterId) }
    var defaultPaper by remember(initialConfig) { mutableStateOf(initialConfig.defaultPaper) }
    var defaultTheme by remember(initialConfig) { mutableStateOf(initialConfig.defaultThemeMode) }
    var defaultScale by remember(initialConfig) { mutableStateOf(initialConfig.defaultFontScale.toFloat()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Text(
                    appLanguage.pick("مصادر واجهة برمجة القرآن والصوتيات", "Quran & Audio API Sources", "Qur'on va audio API manbalari"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = apiPrimary,
                    onValueChange = { apiPrimary = it },
                    label = { Text(appLanguage.pick("مصدر صفحات المصحف الأساسي (HTTPS)", "Primary Mushaf API Base (HTTPS)", "Asosiy Mushaf API manzili (HTTPS)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiFallback,
                    onValueChange = { apiFallback = it },
                    label = { Text(appLanguage.pick("مصدر القرآن الاحتياطي والبحث", "Fallback Quran & Search API Base", "Zaxira Qur'on va qidiruv API")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = audioCdn,
                    onValueChange = { audioCdn = it },
                    label = { Text(appLanguage.pick("خادم تلاوات الآيات (EveryAyah CDN)", "Verse Audio CDN Base", "Oyat audiosi CDN manzili")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = defaultReciter,
                    onValueChange = { defaultReciter = it },
                    label = { Text(appLanguage.pick("القارئ الافتراضي للمستخدمين الجدد", "Default Reciter ID", "Standart qori ID")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Text(
                    appLanguage.pick("الافتراضيات العامة للمظهر والخط", "Global Default Paper, Theme & Font Scale", "Umumiy standart qog'oz, mavzu va shrift"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(appLanguage.pick("ورق المصحف الافتراضي", "Default Mushaf Paper", "Standart Mushaf qog'ozi"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("WHITE" to strings.paperWhite, "SEPIA" to strings.paperSepia, "DARK" to strings.paperDark).forEach { (id, lbl) ->
                        LiquidGlassPill(selected = defaultPaper == id, onClick = { defaultPaper = id }, label = lbl)
                    }
                }
                Text(appLanguage.pick("مظهر التطبيق الافتراضي", "Default App Theme", "Standart ilova mavzusi"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SYSTEM" to strings.themeSystem, "LIGHT" to strings.themeLight, "DARK" to strings.themeDark).forEach { (id, lbl) ->
                        LiquidGlassPill(selected = defaultTheme == id, onClick = { defaultTheme = id }, label = lbl)
                    }
                }
                Text("${strings.fontScaleLabel}: ${(defaultScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge.withTabularNums())
                Slider(value = defaultScale, onValueChange = { defaultScale = it }, valueRange = 0.85f..1.35f)

                Button(
                    onClick = {
                        onSave(
                            QuranDefaultsConfig(
                                quranPageApiBase = apiPrimary,
                                quranFallbackApiBase = apiFallback,
                                audioCdnBase = audioCdn,
                                defaultReciterId = defaultReciter,
                                defaultPaper = defaultPaper,
                                defaultThemeMode = defaultTheme,
                                defaultFontScale = defaultScale.toDouble()
                            )
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.adminSaveChanges)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiControlTab(
    initialConfig: AiRuntimeConfig,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onSave: (AiRuntimeConfig) -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    var enabled by remember(initialConfig) { mutableStateOf(initialConfig.enabled) }
    var modelName by remember(initialConfig) { mutableStateOf(initialConfig.modelName) }
    var systemPrompt by remember(initialConfig) { mutableStateOf(initialConfig.systemPrompt) }
    var dailyLimit by remember(initialConfig) { mutableStateOf(initialConfig.dailyLimitPerUser.toString()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            appLanguage.pick("تفعيل المساعد الإسلامي الذكي", "Enable Islamic AI Assistant", "Islomiy AI yordamchisini yoqish"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            appLanguage.pick(
                                "يتحكم فوراً في التفسير والحفظ والمحادثة لجميع المستخدمين",
                                "Live control over Tafsir, Hifz quiz & Q&A",
                                "Tafsir, hifz testi va savol-javobni jonli boshqarish"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                HorizontalDivider(color = glass.glassBorder)

                Text(appLanguage.pick("النموذج النشط (Gemini Model ID)", "Active Model ID", "Faol model ID"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("gemini-3.8-flash", "gemini-4-hydrogen", "gemini-3.8-pro").forEach { id ->
                        LiquidGlassPill(selected = modelName == id, onClick = { modelName = id }, label = id)
                    }
                }
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text(appLanguage.pick("معرّف النموذج", "Custom Model ID", "Maxsus model ID")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dailyLimit,
                    onValueChange = { v -> dailyLimit = v.filter { it.isDigit() }.take(3) },
                    label = { Text(appLanguage.pick("الحد اليومي لكل مستخدم (٥ - ٥٠٠)", "Daily Rate Limit per User (5-500)", "Kunlik limit (5-500)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text(appLanguage.pick("التوجيه الأساسي للذكاء الاصطناعي (System Prompt)", "Gemini System Prompt", "Gemini tizim yo'riqnomasi")) },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onSave(
                            AiRuntimeConfig(
                                enabled = enabled,
                                modelName = modelName,
                                systemPrompt = systemPrompt,
                                dailyLimitPerUser = (dailyLimit.toIntOrNull() ?: 30).coerceIn(5, 500)
                            )
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.adminSaveChanges)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppOperationsTab(
    initialOps: AppOperationsConfig,
    users: List<UserSettingsDoc>,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onSaveOperations: (AppOperationsConfig) -> Unit,
    onUpdateUser: (String, Map<String, Any>) -> Unit,
    onSetUserVerified: (String, Boolean) -> Unit,
    onSetUserSuspended: (String, Boolean) -> Unit,
    onSetUserAdminRole: (String, Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalLiquidGlassPalette.current
    var ops by remember(initialOps) { mutableStateOf(initialOps) }
    var userQuery by remember { mutableStateOf("") }
    var bannerPhotoStatus by remember { mutableStateOf<String?>(null) }
    var pendingAdminRoleTarget by remember { mutableStateOf<Pair<UserSettingsDoc, Boolean>?>(null) }
    val currentActorUid = remember { runCatching { Firebase.auth.currentUser?.uid }.getOrNull() }

    // Android Zero-Permission Photo Picker for Banner Image Upload
    val bannerPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            bannerPhotoStatus = appLanguage.pick("جاري تجهيز الصورة...", "Preparing image...", "Rasm tayyorlanmoqda...")
            scope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    encodeCompressedBannerBitmapFromUri(context, uri)
                }
                if (!encoded.isNullOrBlank()) {
                    val updated = ops.copy(bannerImageUrl = encoded, bannerEnabled = true)
                    ops = updated
                    onSaveOperations(updated)
                    bannerPhotoStatus = appLanguage.pick(
                        "تم رفع ونشر صورة الإعلان (${encoded.length / 1024} كيلوبايت) بنجاح.",
                        "Banner image uploaded & published (${encoded.length / 1024} KB).",
                        "E'lon rasmi yuklandi va nashr qilindi (${encoded.length / 1024} KB)."
                    )
                } else {
                    bannerPhotoStatus = appLanguage.pick(
                        "تعذر معالجة الصورة المختارة؛ جرّب صورة أخرى أو رابط HTTPS.",
                        "Could not process selected photo; try another photo or HTTPS URL.",
                        "Tanlangan rasmni qayta ishlab bo'lmadi; boshqa rasm yoki HTTPS havola kiriting."
                    )
                }
            }
        }
    }

    val filteredUsers = remember(users, userQuery) {
        if (userQuery.isBlank()) users else {
            val q = userQuery.trim().lowercase()
            users.filter {
                it.displayName.lowercase().contains(q) ||
                    it.email.lowercase().contains(q) ||
                    it.city.lowercase().contains(q) ||
                    it.userId.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live In-App Broadcast & Ad Banner (with Photo Picker Image + Clickable URL Link + AR/EN/UZ)
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(MihrabIcons.BroadcastBanner, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            appLanguage.pick(
                                "لافتة الإعلان والبث الفوري (صورة ورابط)",
                                "Live Broadcast & Ad Banner (Image & Link)",
                                "Jonli e'lon va reklama banneri (Rasm va havola)"
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(checked = ops.bannerEnabled, onCheckedChange = { ops = ops.copy(bannerEnabled = it) })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "INFO" to appLanguage.pick("إعلان", "Info", "E'lon"),
                        "REMINDER" to appLanguage.pick("تذكير", "Reminder", "Eslatma"),
                        "ALERT" to appLanguage.pick("تنبيه هام", "Alert", "Muhim xabar")
                    ).forEach { (tone, lbl) ->
                        LiquidGlassPill(selected = ops.bannerTone == tone, onClick = { ops = ops.copy(bannerTone = tone) }, label = lbl)
                    }
                }
                OutlinedTextField(value = ops.bannerTitleAr, onValueChange = { ops = ops.copy(bannerTitleAr = it) }, label = { Text("العنوان بالعربية") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ops.bannerBodyAr, onValueChange = { ops = ops.copy(bannerBodyAr = it) }, label = { Text("نص الرسالة بالعربية") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ops.bannerTitleEn, onValueChange = { ops = ops.copy(bannerTitleEn = it) }, label = { Text("Title (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ops.bannerBodyEn, onValueChange = { ops = ops.copy(bannerBodyEn = it) }, label = { Text("Message (English)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ops.bannerTitleUz, onValueChange = { ops = ops.copy(bannerTitleUz = it) }, label = { Text("Sarlavha (Oʻzbekcha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ops.bannerBodyUz, onValueChange = { ops = ops.copy(bannerBodyUz = it) }, label = { Text("Xabar matni (Oʻzbekcha)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())

                HorizontalDivider(color = glass.glassBorder)
                Text(
                    appLanguage.pick(
                        "صورة الإعلان والرابط التشعبي",
                        "Banner Image & Clickable URL",
                        "Banner rasmi va bosiladigan havola"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            bannerPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("admin_pick_banner_image")
                    ) {
                        Icon(Icons.Rounded.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(appLanguage.pick("اختيار صورة من الاستوديو", "Pick Image from Gallery", "Galereyadan rasm tanlash"))
                    }
                    if (ops.bannerImageUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                ops = ops.copy(bannerImageUrl = "")
                                bannerPhotoStatus = null
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(appLanguage.pick("مسح", "Clear", "Tozalash"))
                        }
                    }
                }
                OutlinedTextField(
                    value = ops.bannerImageUrl,
                    onValueChange = { ops = ops.copy(bannerImageUrl = it) },
                    leadingIcon = { Icon(Icons.Rounded.Image, null) },
                    label = {
                        Text(
                            appLanguage.pick(
                                "رابط الصورة (HTTPS) أو صورة مضمّنة",
                                "Image URL (HTTPS) or picked data URI",
                                "Rasm havolasi (HTTPS) yoki yuklangan rasm"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!bannerPhotoStatus.isNullOrBlank()) {
                    Text(bannerPhotoStatus!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (ops.bannerImageUrl.isNotBlank()) {
                    RemoteBannerImage(
                        imageUrl = ops.bannerImageUrl,
                        contentDescription = ops.bannerTitleAr.ifBlank { ops.bannerTitleEn },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = ops.bannerLinkUrl,
                    onValueChange = { ops = ops.copy(bannerLinkUrl = it) },
                    leadingIcon = { Icon(Icons.Rounded.Link, null) },
                    label = {
                        Text(
                            appLanguage.pick(
                                "الرابط عند الضغط على الإعلان (https://...)",
                                "Banner Destination URL (https://...)",
                                "Bosilganda ochiladigan havola (https://...)"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ops.bannerCtaAr,
                        onValueChange = { ops = ops.copy(bannerCtaAr = it) },
                        label = { Text("زر الإجراء (AR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ops.bannerCtaEn,
                        onValueChange = { ops = ops.copy(bannerCtaEn = it) },
                        label = { Text("CTA (EN)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ops.bannerCtaUz,
                        onValueChange = { ops = ops.copy(bannerCtaUz = it) },
                        label = { Text("Tugma (UZ)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (ops.bannerEnabled) {
                        appLanguage.pick(
                            "الحالة: الإعلان مفعّل وسيظهر بالصورة والرابط في الواجهة الرئيسية وعند دخول التطبيق.",
                            "Status: Active — visible in Home feed & on app entry.",
                            "Holat: Faol — bosh sahifada va kirishda ko'rinadi."
                        )
                    } else {
                        appLanguage.pick(
                            "تنبيه: مفتاح التفعيل بالأعلى مغلق حالياً — فعّله ليظهر الإعلان للمستخدمين.",
                            "Note: Banner switch above is OFF — turn it ON to display.",
                            "Eslatma: Yuqoridagi kalit O'CHIQ — ko'rinishi uchun yoqing."
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ops.bannerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = {
                        val hasAnyContent = ops.bannerTitleAr.isNotBlank() ||
                            ops.bannerTitleEn.isNotBlank() ||
                            ops.bannerTitleUz.isNotBlank() ||
                            ops.bannerBodyAr.isNotBlank() ||
                            ops.bannerBodyEn.isNotBlank() ||
                            ops.bannerBodyUz.isNotBlank() ||
                            ops.bannerImageUrl.isNotBlank() ||
                            ops.bannerLinkUrl.isNotBlank()
                        val toPublish = if (hasAnyContent) ops.copy(bannerEnabled = true) else ops
                        ops = toPublish
                        onSaveOperations(toPublish)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_publish_banner_button")
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick(
                            "حفظ ونشر الإعلان الآن (في الشريط والرئيسية والإشعارات)",
                            "Save & Publish Ad Banner Now",
                            "E'lonni hozir saqlash va nashr qilish"
                        )
                    )
                }
                OutlinedButton(
                    onClick = {
                        val subj = ops.bannerTitleAr.ifBlank { ops.bannerTitleEn }.ifBlank { "تنبيه هام من تطبيق محراب" }
                        val bodyTxt = buildString {
                            val b = ops.bannerBodyAr.ifBlank { ops.bannerBodyEn }
                            if (b.isNotBlank()) appendLine(b)
                            if (ops.bannerLinkUrl.isNotBlank()) {
                                appendLine()
                                appendLine("الرابط: ${ops.bannerLinkUrl.trim()}")
                            }
                        }
                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                            putExtra(Intent.EXTRA_SUBJECT, subj)
                            putExtra(Intent.EXTRA_TEXT, bodyTxt)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(emailIntent) }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_email_alert_button")
                ) {
                    Icon(Icons.Rounded.Email, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick(
                            "إرسال التذكير الهام بالبريد الإلكتروني",
                            "Send Important Alert via Email",
                            "Muhim eslatmani email orqali yuborish"
                        )
                    )
                }
            }
        }

        // 1b. Support Contact Channels & "About Osama / Mihrab" Editor
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.SupportAgent, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        appLanguage.pick(
                            "بيانات التواصل مع الدعم وقسم «عن أسامة»",
                            "Support Contact & About Osama Settings",
                            "Qo'llab-quvvatlash va «Usoma haqida» sozlamalari"
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = ops.supportWhatsapp,
                    onValueChange = { ops = ops.copy(supportWhatsapp = it) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Chat, null) },
                    label = { Text(appLanguage.pick("رقم واتساب الدعم", "Support WhatsApp", "WhatsApp raqami")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_support_whatsapp_input")
                )
                OutlinedTextField(
                    value = ops.supportTelegram,
                    onValueChange = { ops = ops.copy(supportTelegram = it) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Send, null) },
                    label = { Text(appLanguage.pick("رقم أو معرف تيليجرام", "Support Telegram", "Telegram raqami")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_support_telegram_input")
                )
                OutlinedTextField(
                    value = ops.supportEmail,
                    onValueChange = { ops = ops.copy(supportEmail = it) },
                    leadingIcon = { Icon(Icons.Rounded.Email, null) },
                    label = { Text(appLanguage.pick("البريد الإلكتروني للدعم", "Support Email", "Qo'llab-quvvatlash emaili")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_support_email_input")
                )
                HorizontalDivider(color = glass.glassBorder)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = BurnishedGold)
                    Text(
                        appLanguage.pick("نصوص صفحة «عن أسامة وتطبيق محراب»", "About Osama / Mihrab Copy", "«Usoma haqida» matnlari"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = ops.aboutTitleAr,
                    onValueChange = { ops = ops.copy(aboutTitleAr = it) },
                    label = { Text("عنوان قسم عن أسامة (AR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ops.aboutBodyAr,
                    onValueChange = { ops = ops.copy(aboutBodyAr = it) },
                    label = { Text("نبذة عن أسامة والتطبيق (AR)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ops.aboutTitleEn,
                    onValueChange = { ops = ops.copy(aboutTitleEn = it) },
                    label = { Text("About Title (EN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ops.aboutBodyEn,
                    onValueChange = { ops = ops.copy(aboutBodyEn = it) },
                    label = { Text("About Body (EN)") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ops.aboutTitleUz,
                    onValueChange = { ops = ops.copy(aboutTitleUz = it) },
                    label = { Text("Haqida sarlavhasi (UZ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ops.aboutBodyUz,
                    onValueChange = { ops = ops.copy(aboutBodyUz = it) },
                    label = { Text("Haqida matni (UZ)") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSaveOperations(ops) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_save_support_about_button")
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick(
                            "حفظ بيانات الدعم وقسم عن أسامة",
                            "Save Support & About Settings",
                            "Aloqa va haqida ma'lumotlarini saqlash"
                        )
                    )
                }
            }
        }

        // 2. Global Feature Flags
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(MihrabIcons.FeatureFlags, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick("أعلام الميزات العالمية (Feature Flags)", "Global Feature Flags", "Global funksiya bayroqlari"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                FlagSwitchRow(appLanguage.pick("تلاوات القرآن بالصوت", "Quran Audio Recitation", "Qur'on audio tilovati"), ops.quranAudioEnabled) { ops = ops.copy(quranAudioEnabled = it) }
                FlagSwitchRow(appLanguage.pick("المساعد الذكي", "AI Assistant", "AI yordamchi"), ops.aiAssistantEnabled) { ops = ops.copy(aiAssistantEnabled = it) }
                FlagSwitchRow(appLanguage.pick("حائط الدعاء المجتمعي", "Community Dua Wall", "Jamoa duo devori"), ops.communityDuaWallEnabled) { ops = ops.copy(communityDuaWallEnabled = it) }
                FlagSwitchRow(appLanguage.pick("ختمة العائلة المشتركة", "Family Khatmah Groups", "Oila xatmalari"), ops.familyKhatmahEnabled) { ops = ops.copy(familyKhatmahEnabled = it) }
                FlagSwitchRow(appLanguage.pick("القبلة بالواقع المعزز", "AR Qibla Camera", "AR Qibla kamerasi"), ops.arQiblaEnabled) { ops = ops.copy(arQiblaEnabled = it) }
                FlagSwitchRow(appLanguage.pick("وضع الأطفال", "Kids Mode", "Bolalar rejimi"), ops.kidsModeEnabled) { ops = ops.copy(kidsModeEnabled = it) }
                FlagSwitchRow(appLanguage.pick("رفع أذان من الجهاز", "Custom Device Adhan Upload", "Maxsus azon yuklash"), ops.customAdhanUploadEnabled) { ops = ops.copy(customAdhanUploadEnabled = it) }
                FlagSwitchRow(appLanguage.pick("الاختصارات الذكية أسفل الرئيسية", "Bottom Smart Shortcuts", "Pastki aqlli yorliqlar"), ops.smartShortcutsEnabled) { ops = ops.copy(smartShortcutsEnabled = it) }
                FlagSwitchRow(appLanguage.pick("بطاقة الآيات والأحاديث الحية", "Live Rotating Verses & Hadiths", "Jonli oyat va hadislar kartasi"), ops.liveVersesCardEnabled) { ops = ops.copy(liveVersesCardEnabled = it) }
                FlagSwitchRow(appLanguage.pick("المساجد القريبة بالخريطة (GPS)", "Nearby Mosques (GPS / OSM)", "Yaqin atrofdagi masjidlar"), ops.nearbyMosquesEnabled) { ops = ops.copy(nearbyMosquesEnabled = it) }
                FlagSwitchRow(appLanguage.pick("شريط المواقيت الدائم بالإشعارات", "Persistent Prayer Shade Notification", "Doimiy namoz vaqtlari paneli"), ops.persistentPrayerNotificationAllowed) { ops = ops.copy(persistentPrayerNotificationAllowed = it) }
                FlagSwitchRow(appLanguage.pick("قسم الفيديوهات الإسلامية والكرتون", "Islamic Videos & Cartoons Section", "Islomiy videolar bo'limi"), ops.videosEnabled) { ops = ops.copy(videosEnabled = it) }
                FlagSwitchRow(appLanguage.pick("وضع الصيانة السحابي", "Maintenance Mode", "Texnik xizmat rejimi"), ops.maintenanceMode) { ops = ops.copy(maintenanceMode = it) }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onSaveOperations(ops) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_operations_button")
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.adminSaveChanges)
                }
            }
        }

        // 3. Full JSON Database Backup Export & Import
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(MihrabIcons.DatabaseBackup, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick("النسخ الاحتياطي الشامل لقاعدة البيانات", "Full JSON Database Backup & Restore", "To'liq JSON ma'lumotlar bazasi zaxirasi"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onExportBackup, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.adminExportBackup)
                    }
                    OutlinedButton(onClick = onImportBackup, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.UploadFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.adminImportBackup)
                    }
                }
            }
        }

        // 4. Users Directory, Verification, Suspension & Admin Appointment
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(MihrabIcons.UsersManager, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick("إدارة المستخدمين وتوثيقهم (${users.size})", "Users Governance (${users.size})", "Foydalanuvchilarni boshqarish (${users.size})"),
                        style = MaterialTheme.typography.titleLarge.withTabularNums(),
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    label = { Text(appLanguage.pick("بحث بالاسم أو البريد أو المدينة أو UID", "Search name, email, city or UID", "Ism, email, shahar yoki UID bo'yicha qidirish")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(filteredUsers, key = { it.userId }) { u ->
            val isTargetSuperAdmin = u.email.trim().lowercase() == AdminAccess.SUPER_ADMIN_EMAIL
            val isTargetAdmin = isTargetSuperAdmin || u.adminRole == "ADMIN"
            val isSelf = u.userId == currentActorUid

            LiquidGlassCard(
                shape = RoundedCornerShape(22.dp),
                borderColor = when {
                    u.accountSuspended -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                    isTargetAdmin -> BurnishedGold.copy(alpha = 0.55f)
                    u.accountVerified -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else -> glass.glassBorder
                },
                contentPadding = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_card_${u.userId}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteAvatarImage(
                        photoUrl = u.photoUrl,
                        displayName = u.displayName,
                        size = 46.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = u.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (u.accountVerified || isTargetSuperAdmin) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (u.email.isNotBlank()) {
                            Text(
                                text = u.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "${u.city} • ${u.preferredLanguage.uppercase()} • ${appLanguage.pick("صفحة", "Page", "Sahifa")} ${u.lastReadPage} • ${u.khatmahPagesRead}/604",
                            style = MaterialTheme.typography.bodySmall.withTabularNums(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(u.userId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(appLanguage.pick("أطفال", "Kids", "Bolalar"), style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = u.kidsMode,
                            onCheckedChange = { checked ->
                                onUpdateUser(u.userId, mapOf("kidsMode" to checked))
                            }
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isTargetSuperAdmin) {
                        Surface(shape = CircleShape, color = BurnishedGold.copy(alpha = 0.18f)) {
                            Text(
                                appLanguage.pick("المشرف الأعلى", "Super Admin", "Super Admin"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BurnishedGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (u.adminRole == "ADMIN") {
                        Surface(shape = CircleShape, color = BurnishedGold.copy(alpha = 0.16f)) {
                            Text(
                                appLanguage.pick("مسؤول / أدمن", "Admin", "Admin"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BurnishedGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (u.accountVerified) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                            Text(
                                appLanguage.pick("موثّق ✓", "Verified ✓", "Tasdiqlangan ✓"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (u.accountSuspended) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)) {
                            Text(
                                appLanguage.pick("حساب معلّق", "Suspended", "To'xtatilgan"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (!isTargetSuperAdmin) {
                    HorizontalDivider(color = glass.glassBorder)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiquidGlassPill(
                            selected = u.accountVerified,
                            onClick = { onSetUserVerified(u.userId, !u.accountVerified) },
                            icon = Icons.Rounded.Verified,
                            label = if (u.accountVerified) {
                                appLanguage.pick("إلغاء التوثيق", "Unverify", "Tasdiqni olish")
                            } else {
                                appLanguage.pick("توثيق الحساب", "Verify Account", "Hisobni tasdiqlash")
                            },
                            modifier = Modifier.testTag("verify_user_btn_${u.userId}")
                        )

                        if (!isSelf) {
                            LiquidGlassPill(
                                selected = u.accountSuspended,
                                onClick = { onSetUserSuspended(u.userId, !u.accountSuspended) },
                                icon = Icons.Rounded.Block,
                                activeColor = MaterialTheme.colorScheme.error,
                                label = if (u.accountSuspended) {
                                    appLanguage.pick("رفع التعليق", "Unsuspend", "Faollashtirish")
                                } else {
                                    appLanguage.pick("تعليق الحساب", "Suspend Account", "To'xtatish")
                                },
                                modifier = Modifier.testTag("suspend_user_btn_${u.userId}")
                            )
                        }

                        if (!isSelf || u.adminRole != "ADMIN") {
                            val willMakeAdmin = u.adminRole != "ADMIN"
                            LiquidGlassPill(
                                selected = u.adminRole == "ADMIN",
                                onClick = { pendingAdminRoleTarget = u to willMakeAdmin },
                                icon = Icons.Rounded.AdminPanelSettings,
                                activeColor = BurnishedGold,
                                label = if (willMakeAdmin) {
                                    appLanguage.pick("تعيين كأدمن", "Make Admin", "Admin qilish")
                                } else {
                                    appLanguage.pick("إزالة الأدمن", "Remove Admin", "Adminlikni olish")
                                },
                                modifier = Modifier.testTag("admin_role_btn_${u.userId}")
                            )
                        }
                    }
                }
            }
        }
    }

    pendingAdminRoleTarget?.let { (targetUser, makeAdmin) ->
        AlertDialog(
            onDismissRequest = { pendingAdminRoleTarget = null },
            title = {
                Text(
                    if (makeAdmin) {
                        appLanguage.pick("تأكيد تعيين مسؤول جديد", "Confirm Appointing Admin", "Yangi admin tayinlashni tasdiqlang")
                    } else {
                        appLanguage.pick("تأكيد إزالة صلاحية المسؤول", "Confirm Removing Admin", "Admin vakolatini olish")
                    }
                )
            },
            text = {
                Text(
                    if (makeAdmin) {
                        appLanguage.pick(
                            "هل أنت متأكد من تعيين «${targetUser.displayName}» (${targetUser.email.ifBlank { targetUser.userId }}) مسؤولاً (أدمن) في تطبيق محراب؟ سيحصل على صلاحيات لوحة الإدارة وإدارة الإعلانات والمحتوى والمستخدمين.",
                            "Are you sure you want to appoint ${targetUser.displayName} (${targetUser.email.ifBlank { targetUser.userId }}) as an Admin? They will gain access to the Admin Dashboard.",
                            "${targetUser.displayName} (${targetUser.email.ifBlank { targetUser.userId }}) foydalanuvchisini Admin etib tayinlamoqchimisiz?"
                        )
                    } else {
                        appLanguage.pick(
                            "هل أنت متأكد من إزالة صلاحية الأدمن عن «${targetUser.displayName}» (${targetUser.email.ifBlank { targetUser.userId }})؟",
                            "Remove Admin privileges from ${targetUser.displayName} (${targetUser.email.ifBlank { targetUser.userId }})?",
                            "${targetUser.displayName} dan admin vakolatini olib tashlaysizmi?"
                        )
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingAdminRoleTarget = null
                        onSetUserAdminRole(targetUser.userId, makeAdmin)
                    },
                    colors = if (!makeAdmin) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                    modifier = Modifier.testTag("confirm_admin_role_button")
                ) {
                    Text(
                        if (makeAdmin) {
                            appLanguage.pick("تأكيد التعيين كأدمن", "Confirm Admin", "Tasdiqlash")
                        } else {
                            appLanguage.pick("تأكيد الإزالة", "Remove", "Olib tashlash")
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAdminRoleTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun FlagSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
