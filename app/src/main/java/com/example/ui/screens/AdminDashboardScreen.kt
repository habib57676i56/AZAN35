package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.AdhanAudioPlayer
import com.example.core.hardware.RemoteBannerImage
import com.example.core.hardware.encodeCompressedBannerBitmapFromUri
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.AdminAccess
import com.example.data.firebase.AiRuntimeConfig
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.QuranDefaultsConfig
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
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val currentUser = Firebase.auth.currentUser

    // Layer 1 Route Guard: Strictly block UI if not verified habiybullohy@gmail.com
    if (!AdminAccess.isSuperAdmin(currentUser)) {
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

    var adminTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf(
        strings.adminTabDefaults to MihrabIcons.ScholarBook,
        strings.adminTabReciters to MihrabIcons.AdhanVoice,
        strings.adminTabAdhkar to MihrabIcons.TasbihBeads,
        strings.adminTabAdhan to MihrabIcons.BroadcastBanner,
        strings.adminTabAiControl to MihrabIcons.AiSpark,
        strings.adminTabOperations to MihrabIcons.FeatureFlags
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
                                        text = "${strings.adminBadgeLabel} • ${AdminAccess.SUPER_ADMIN_EMAIL}",
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
                        onExportBackup = { adminViewModel.exportFullDatabaseBackup() },
                        onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "text/*")) }
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
    users: List<com.example.data.firebase.UserSettingsDoc>,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onSaveOperations: (AppOperationsConfig) -> Unit,
    onUpdateUser: (String, Map<String, Any>) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalLiquidGlassPalette.current
    var ops by remember(initialOps) { mutableStateOf(initialOps) }
    var userQuery by remember { mutableStateOf("") }
    var bannerPhotoStatus by remember { mutableStateOf<String?>(null) }

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
                    onClick = { onSaveOperations(ops) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_publish_banner_button")
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick(
                            "حفظ ونشر الإعلان الآن",
                            "Save & Publish Ad Banner Now",
                            "E'lonni hozir saqlash va nashr qilish"
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

        // 4. Users Directory & Quick Operations
        item {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(MihrabIcons.UsersManager, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        appLanguage.pick("إدارة المستخدمين (${users.size})", "Users Directory (${users.size})", "Foydalanuvchilar (${users.size})"),
                        style = MaterialTheme.typography.titleLarge.withTabularNums(),
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    label = { Text(appLanguage.pick("بحث بالاسم أو المدينة أو UID", "Search user name, city or UID", "Ism, shahar yoki UID bo'yicha qidirish")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(filteredUsers, key = { it.userId }) { u ->
            LiquidGlassCard(shape = RoundedCornerShape(22.dp), contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(u.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            }
        }
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
