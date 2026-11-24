package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.AdminAccess
import com.example.data.firebase.AiRuntimeConfig
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.QuranDefaultsConfig
import com.example.ui.icons.MihrabIcons
import com.example.ui.viewmodel.AdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.roundToInt

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    adhanAudioPlayer: AdhanAudioPlayer,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    val isArabicUi = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU
    val currentUser = Firebase.auth.currentUser

    // Layer 1 Route Guard: Strictly block UI if not verified habiybullohy@gmail.com
    if (!AdminAccess.isSuperAdmin(currentUser)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.widthIn(max = 460.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = strings.adminAccessDenied,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(onClick = onBack) {
                        Text(if (isArabicUi) "رجوع" else "Go Back")
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Top Super Admin Bar
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                            Icon(
                                imageVector = MihrabIcons.AdminShield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
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

                    if (isBusy) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    if (adminMsg != null) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
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

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabLabels.forEachIndexed { idx, (label, icon) ->
                            FilterChip(
                                selected = adminTab == idx,
                                onClick = { adminTab = idx },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp)) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("admin_tab_$idx")
                            )
                        }
                    }
                }
            }

            when (adminTab) {
                0 -> QuranDefaultsTab(
                    initialConfig = quranDefaults,
                    isArabicUi = isArabicUi,
                    strings = strings,
                    onSave = { adminViewModel.saveQuranDefaults(it) }
                )
                1 -> RecitersCrudTab(
                    reciters = reciters,
                    isArabicUi = isArabicUi,
                    strings = strings,
                    onUpsert = { adminViewModel.upsertReciter(it) },
                    onDelete = { adminViewModel.deleteReciter(it) },
                    onSeedDefaults = { adminViewModel.seedDefaultReciters() }
                )
                2 -> AdhkarCrudTab(
                    adhkarList = adhkar,
                    isArabicUi = isArabicUi,
                    strings = strings,
                    onUpsert = { adminViewModel.upsertAdhkar(it) },
                    onDelete = { adminViewModel.deleteAdhkar(it) },
                    onSeedDefaults = { adminViewModel.seedDefaultAdhkar() }
                )
                3 -> AdhanAudioCrudTab(
                    voices = adhanVoices,
                    adhanAudioPlayer = adhanAudioPlayer,
                    isArabicUi = isArabicUi,
                    strings = strings,
                    onUpsert = { adminViewModel.upsertAdhanVoice(it) },
                    onDelete = { adminViewModel.deleteAdhanVoice(it) },
                    onSeedDefaults = { adminViewModel.seedDefaultAdhanVoices() }
                )
                4 -> AiControlTab(
                    initialConfig = aiConfig,
                    isArabicUi = isArabicUi,
                    strings = strings,
                    onSave = { adminViewModel.saveAiConfig(it) }
                )
                5 -> AppOperationsTab(
                    initialOps = appOps,
                    users = users,
                    isArabicUi = isArabicUi,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuranDefaultsTab(
    initialConfig: QuranDefaultsConfig,
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    onSave: (QuranDefaultsConfig) -> Unit
) {
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isArabicUi) "مصادر واجهة برمجة القرآن والصوتيات" else "Quran & Audio API Sources",
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedTextField(
                        value = apiPrimary,
                        onValueChange = { apiPrimary = it },
                        label = { Text(if (isArabicUi) "مصدر صفحات المصحف الأساسي (HTTPS)" else "Primary Mushaf API Base (HTTPS)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiFallback,
                        onValueChange = { apiFallback = it },
                        label = { Text(if (isArabicUi) "مصدر القرآن الاحتياطي والبحث" else "Fallback Quran & Search API Base") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = audioCdn,
                        onValueChange = { audioCdn = it },
                        label = { Text(if (isArabicUi) "خادم تلاوات الآيات (EveryAyah CDN)" else "Verse Audio CDN Base") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = defaultReciter,
                        onValueChange = { defaultReciter = it },
                        label = { Text(if (isArabicUi) "القارئ الافتراضي للمستخدمين الجدد" else "Default Reciter ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isArabicUi) "الافتراضيات العامة للمظهر والخط" else "Global Default Paper, Theme & Font Scale",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(if (isArabicUi) "ورق المصحف الافتراضي" else "Default Mushaf Paper", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WHITE" to strings.paperWhite, "SEPIA" to strings.paperSepia, "DARK" to strings.paperDark).forEach { (id, lbl) ->
                            FilterChip(selected = defaultPaper == id, onClick = { defaultPaper = id }, label = { Text(lbl) })
                        }
                    }
                    Text(if (isArabicUi) "مظهر التطبيق الافتراضي" else "Default App Theme", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to strings.themeSystem, "LIGHT" to strings.themeLight, "DARK" to strings.themeDark).forEach { (id, lbl) ->
                            FilterChip(selected = defaultTheme == id, onClick = { defaultTheme = id }, label = { Text(lbl) })
                        }
                    }
                    Text("${strings.fontScaleLabel}: ${(defaultScale * 100).roundToInt()}%")
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiControlTab(
    initialConfig: AiRuntimeConfig,
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    onSave: (AiRuntimeConfig) -> Unit
) {
    var enabled by remember(initialConfig) { mutableStateOf(initialConfig.enabled) }
    var modelName by remember(initialConfig) { mutableStateOf(initialConfig.modelName) }
    var systemPrompt by remember(initialConfig) { mutableStateOf(initialConfig.systemPrompt) }
    var dailyLimit by remember(initialConfig) { mutableStateOf(initialConfig.dailyLimitPerUser.toString()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isArabicUi) "تفعيل المساعد الإسلامي الذكي" else "Enable Islamic AI Assistant",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                if (isArabicUi) "يتحكم فوراً في التفسير والحفظ والمحادثة لجميع المستخدمين" else "Live control over Tafsir, Hifz quiz & Q&A",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }

                    HorizontalDivider()

                    Text(if (isArabicUi) "النموذج النشط (Gemini Model ID)" else "Active Model ID", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("gemini-3.8-flash", "gemini-4-hydrogen", "gemini-3.8-pro").forEach { id ->
                            FilterChip(selected = modelName == id, onClick = { modelName = id }, label = { Text(id) })
                        }
                    }
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text(if (isArabicUi) "معرّف النموذج" else "Custom Model ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dailyLimit,
                        onValueChange = { v -> dailyLimit = v.filter { it.isDigit() }.take(3) },
                        label = { Text(if (isArabicUi) "الحد اليومي لكل مستخدم (٥ - ٥٠٠)" else "Daily Rate Limit per User (5-500)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text(if (isArabicUi) "التوجيه الأساسي للذكاء الاصطناعي (System Prompt)" else "Gemini System Prompt") },
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppOperationsTab(
    initialOps: AppOperationsConfig,
    users: List<com.example.data.firebase.UserSettingsDoc>,
    isArabicUi: Boolean,
    strings: LocalizedStrings,
    onSaveOperations: (AppOperationsConfig) -> Unit,
    onUpdateUser: (String, Map<String, Any>) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    var ops by remember(initialOps) { mutableStateOf(initialOps) }
    var userQuery by remember { mutableStateOf("") }

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
        // 1. Live In-App Broadcast Banner
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(MihrabIcons.BroadcastBanner, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isArabicUi) "لافتة البث والإشعارات الفورية" else "Live In-App Broadcast Banner", style = MaterialTheme.typography.titleLarge)
                        }
                        Switch(checked = ops.bannerEnabled, onCheckedChange = { ops = ops.copy(bannerEnabled = it) })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("INFO" to (if (isArabicUi) "إعلان" else "Info"), "REMINDER" to (if (isArabicUi) "تذكير" else "Reminder"), "ALERT" to (if (isArabicUi) "تنبيه هام" else "Alert")).forEach { (tone, lbl) ->
                            FilterChip(selected = ops.bannerTone == tone, onClick = { ops = ops.copy(bannerTone = tone) }, label = { Text(lbl) })
                        }
                    }
                    OutlinedTextField(value = ops.bannerTitleAr, onValueChange = { ops = ops.copy(bannerTitleAr = it) }, label = { Text("العنوان بالعربية") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ops.bannerBodyAr, onValueChange = { ops = ops.copy(bannerBodyAr = it) }, label = { Text("نص الرسالة بالعربية") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ops.bannerTitleEn, onValueChange = { ops = ops.copy(bannerTitleEn = it) }, label = { Text("Title (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ops.bannerBodyEn, onValueChange = { ops = ops.copy(bannerBodyEn = it) }, label = { Text("Message (English)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // 2. Global Feature Flags
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MihrabIcons.FeatureFlags, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isArabicUi) "أعلام الميزات العالمية (Feature Flags)" else "Global Feature Flags", style = MaterialTheme.typography.titleLarge)
                    }
                    FlagSwitchRow(if (isArabicUi) "تلاوات القرآن بالصوت" else "Quran Audio Recitation", ops.quranAudioEnabled) { ops = ops.copy(quranAudioEnabled = it) }
                    FlagSwitchRow(if (isArabicUi) "المساعد الذكي" else "AI Assistant", ops.aiAssistantEnabled) { ops = ops.copy(aiAssistantEnabled = it) }
                    FlagSwitchRow(if (isArabicUi) "حائط الدعاء المجتمعي" else "Community Dua Wall", ops.communityDuaWallEnabled) { ops = ops.copy(communityDuaWallEnabled = it) }
                    FlagSwitchRow(if (isArabicUi) "ختمة العائلة المشتركة" else "Family Khatmah Groups", ops.familyKhatmahEnabled) { ops = ops.copy(familyKhatmahEnabled = it) }
                    FlagSwitchRow(if (isArabicUi) "القبلة بالواقع المعزز" else "AR Qibla Camera", ops.arQiblaEnabled) { ops = ops.copy(arQiblaEnabled = it) }
                    FlagSwitchRow(if (isArabicUi) "وضع الأطفال" else "Kids Mode", ops.kidsModeEnabled) { ops = ops.copy(kidsModeEnabled = it) }
                    FlagSwitchRow(if (isArabicUi) "رفع أذان من الجهاز" else "Custom Device Adhan Upload", ops.customAdhanUploadEnabled) { ops = ops.copy(customAdhanUploadEnabled = it) }
                    FlagSwitchRow(if (isArabicUi) "وضع الصيانة السحابي" else "Maintenance Mode", ops.maintenanceMode) { ops = ops.copy(maintenanceMode = it) }

                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { onSaveOperations(ops) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.adminSaveChanges)
                    }
                }
            }
        }

        // 3. Full JSON Database Backup Export & Import
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MihrabIcons.DatabaseBackup, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isArabicUi) "النسخ الاحتياطي الشامل لقاعدة البيانات" else "Full JSON Database Backup & Restore", style = MaterialTheme.typography.titleLarge)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onExportBackup, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.adminExportBackup)
                        }
                        OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.UploadFile, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.adminImportBackup)
                        }
                    }
                }
            }
        }

        // 4. Users Directory & Quick Operations
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MihrabIcons.UsersManager, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isArabicUi) "إدارة المستخدمين (${users.size})" else "Users Directory (${users.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    OutlinedTextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        label = { Text(if (isArabicUi) "بحث بالاسم أو المدينة أو UID" else "Search user name, city or UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(filteredUsers, key = { it.userId }) { u ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(u.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${u.city} • ${u.preferredLanguage.uppercase()} • صفحة ${u.lastReadPage} • ختمة ${u.khatmahPagesRead}/604",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(u.userId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isArabicUi) "أطفال" else "Kids", style = MaterialTheme.typography.labelSmall)
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
