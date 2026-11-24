package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.hardware.AdhanAudioPlayer
import com.example.core.hardware.QuranRecitationPlayer
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.stringsFor
import com.example.data.firebase.AdminAccess
import com.example.data.firebase.AdminRepository
import com.example.data.firebase.MihrabRepository
import com.example.data.firebase.UserSettingsDoc
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.performSignOut
import com.example.data.local.MihrabLocalDatabase
import com.example.data.quran.MushafRepository
import com.example.ui.icons.MihrabIcons
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DhikrScreen
import com.example.ui.screens.KidsModeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.PrayerScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MihrabTheme
import com.example.ui.theme.MihrabThemePreset
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check wiring with launch Intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val launchTab = when (intent?.getStringExtra("launch_tab")) {
            "quran" -> 1
            "dhikr" -> 2
            "learn" -> 3
            "tools" -> 4
            else -> 0
        }

        setContent {
            MihrabRootApp(initialTab = launchTab)
        }
    }
}

@Composable
private fun MihrabRootApp(initialTab: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var themePreset by rememberSaveable { mutableStateOf(MihrabThemePreset.EMERALD_SANCTUARY) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val user = currentUser
    if (user == null) {
        MihrabTheme(
            preset = themePreset,
            themeMode = AppThemeMode.SYSTEM,
            fontScale = 1.0f,
            arabicPrimary = true
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AuthScreen(
                    signInButtonText = stringsFor(AppLanguage.ARABIC).signInWithGoogle,
                    onAuthSuccess = {}
                )
            }
        }
    } else {
        val uid = user.uid
        val isSuperAdmin = AdminAccess.isSuperAdmin(user)
        key(uid) {
            AuthenticatedMihrabShell(
                currentUserId = uid,
                displayName = user.displayName ?: "مؤمن",
                isSuperAdmin = isSuperAdmin,
                initialTab = initialTab,
                themePreset = themePreset,
                onChangeThemePreset = { themePreset = it },
                onSignOut = {
                    performSignOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedMihrabShell(
    currentUserId: String,
    displayName: String,
    isSuperAdmin: Boolean,
    initialTab: Int,
    themePreset: MihrabThemePreset,
    onChangeThemePreset: (MihrabThemePreset) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val localDb = remember(context) { MihrabLocalDatabase.getInstance(context) }
    val db = remember(context) {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }
    val sharedRepository = remember(db, localDb) {
        MihrabRepository(
            db = db,
            localPrefsDao = localDb.localPrefsDao(),
            reciterDao = localDb.reciterDao(),
            adhanVoiceDao = localDb.adhanVoiceDao(),
            adhkarDao = localDb.adhkarDao()
        )
    }
    val mushafRepository = remember(localDb) { MushafRepository(localDb.mushafDao()) }

    val mihrabViewModel: MihrabViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                MihrabViewModel(
                    repository = sharedRepository,
                    mushafRepository = mushafRepository,
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val aiViewModel: AiViewModel = viewModel(
        key = "ai_$currentUserId",
        factory = viewModelFactory {
            initializer {
                AiViewModel(
                    mihrabRepository = sharedRepository,
                    currentUserId = currentUserId
                )
            }
        }
    )
    val audioPlayer = remember(context) { QuranTtsAudioPlayer(context) }
    val adhanAudioPlayer = remember(context) { AdhanAudioPlayer(context) }
    val recitationPlayer = remember(context, localDb) {
        QuranRecitationPlayer(
            context = context,
            audioCacheDao = localDb.audioCacheDao(),
            mushafDao = localDb.mushafDao()
        )
    }

    DisposableEffect(audioPlayer, adhanAudioPlayer, recitationPlayer) {
        onDispose {
            audioPlayer.release()
            adhanAudioPlayer.release()
            recitationPlayer.release()
        }
    }

    // Request notification permission on Android 13+ for background recitation media controls
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val settingsState by mihrabViewModel.settingsState.collectAsStateWithLifecycle()
    val localPrefs by mihrabViewModel.localPrefsState.collectAsStateWithLifecycle()
    val statusMessage by mihrabViewModel.statusMessage.collectAsStateWithLifecycle()
    val useIndic by mihrabViewModel.useIndicNumerals.collectAsStateWithLifecycle()
    val quranDefaults by mihrabViewModel.quranDefaultsState.collectAsStateWithLifecycle()
    val appOperations by mihrabViewModel.appOperationsState.collectAsStateWithLifecycle()

    LaunchedEffect(quranDefaults) {
        mushafRepository.setApiSources(
            primaryBase = quranDefaults.quranPageApiBase,
            fallbackBase = quranDefaults.quranFallbackApiBase
        )
        recitationPlayer.setAudioCdnBase(quranDefaults.audioCdnBase)
    }

    // Use Room localPrefs first for zero-flicker cold start before Firestore emits
    val remoteSettings = (settingsState as? UiState.Success)?.data
    val localPerPrayerModes = remember(localPrefs?.perPrayerAdhanJson) {
        val raw = localPrefs?.perPrayerAdhanJson ?: "{}"
        try {
            val obj = JSONObject(raw)
            val defaults = UserSettingsDoc().perPrayerAdhanModes.toMutableMap()
            obj.keys().forEach { k -> defaults[k] = obj.optString(k, "FULL_ADHAN") }
            defaults
        } catch (_: Exception) {
            UserSettingsDoc().perPrayerAdhanModes
        }
    }
    val effectiveSettings = remoteSettings ?: UserSettingsDoc(
        userId = currentUserId,
        displayName = displayName,
        preferredLanguage = localPrefs?.preferredLanguage ?: "ar",
        themeMode = localPrefs?.themeMode ?: AppThemeMode.SYSTEM.id,
        mushafPaper = localPrefs?.mushafPaper ?: "WHITE",
        fontScale = (localPrefs?.fontScale ?: 1.0f).toDouble(),
        lastReadPage = (localPrefs?.lastReadPage ?: 1).toLong(),
        lastReadSurah = (localPrefs?.lastReadSurah ?: 1).toLong(),
        lastReadAyah = (localPrefs?.lastReadAyah ?: 1).toLong(),
        preferredReciterId = localPrefs?.preferredReciterId ?: "ar.alafasy",
        adhanEnabled = localPrefs?.adhanEnabled ?: true,
        adhanVoiceId = localPrefs?.adhanVoiceId ?: "makkah",
        perPrayerAdhanModes = localPerPrayerModes,
        customAdhanName = localPrefs?.customAdhanName ?: "",
        khatmahPagesRead = (localPrefs?.khatmahPagesRead ?: 0).toLong()
    )

    val appLanguage = AppLanguage.fromCode(effectiveSettings.preferredLanguage)
    val themeMode = AppThemeMode.fromId(effectiveSettings.themeMode)
    val fontScale = effectiveSettings.fontScale.toFloat().coerceIn(0.85f, 1.35f)
    val arabicPrimary = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU
    val strings = remember(appLanguage) { stringsFor(appLanguage) }

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var learnSubTab by rememberSaveable { mutableIntStateOf(0) }
    var showAdminDashboard by rememberSaveable { mutableStateOf(false) }
    var bannerDismissedKey by rememberSaveable { mutableStateOf("") }

    if (showAdminDashboard && isSuperAdmin) {
        BackHandler { showAdminDashboard = false }
    } else if (selectedTab != 0) {
        BackHandler { selectedTab = 0 }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            snackbarHostState.showSnackbar(statusMessage!!)
            mihrabViewModel.clearMessage()
        }
    }

    val navLabels = listOf(
        strings.tabPrayer,
        strings.tabQuran,
        strings.tabDhikr,
        strings.tabLearn,
        strings.tabTools
    )
    val navItems = navLabels.mapIndexed { idx, label ->
        val (filled, outlined) = MihrabIcons.navIcons(idx)
        Triple(label, filled, outlined)
    }

    MihrabTheme(
        preset = themePreset,
        themeMode = themeMode,
        fontScale = fontScale,
        arabicPrimary = arabicPrimary
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CompositionLocalProvider(LocalLayoutDirection provides appLanguage.layoutDirection) {
                if (showAdminDashboard && isSuperAdmin) {
                    val adminRepository = remember(db) { AdminRepository(db = db) }
                    val adminViewModel: AdminViewModel = viewModel(
                        key = "admin_$currentUserId",
                        factory = viewModelFactory {
                            initializer {
                                AdminViewModel(
                                    adminRepository = adminRepository,
                                    mihrabRepository = sharedRepository,
                                    currentUserId = currentUserId
                                )
                            }
                        }
                    )
                    AdminDashboardScreen(
                        adminViewModel = adminViewModel,
                        adhanAudioPlayer = adhanAudioPlayer,
                        appLanguage = appLanguage,
                        strings = strings,
                        onBack = { showAdminDashboard = false }
                    )
                } else if (effectiveSettings.kidsMode && appOperations.kidsModeEnabled) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("${strings.appTitle} • أطفال") },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                            )
                        },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            KidsModeScreen(
                                audioPlayer = audioPlayer,
                                onExitKidsMode = { mihrabViewModel.updateSettings(mapOf("kidsMode" to false)) }
                            )
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useNavRail = maxWidth >= 700.dp

                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = MihrabIcons.MihrabArch,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(strings.appTitle, style = MaterialTheme.typography.headlineMedium)
                                        }
                                    },
                                    actions = {
                                        if (isSuperAdmin) {
                                            IconButton(
                                                onClick = { showAdminDashboard = true },
                                                modifier = Modifier.testTag("top_bar_admin_button")
                                            ) {
                                                Icon(
                                                    imageVector = MihrabIcons.AdminShield,
                                                    contentDescription = strings.adminDashboardTitle,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                                )
                            },
                            bottomBar = {
                                if (!useNavRail) {
                                    NavigationBar {
                                        navItems.forEachIndexed { idx, (label, filled, outlined) ->
                                            NavigationBarItem(
                                                selected = selectedTab == idx,
                                                onClick = { selectedTab = idx },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selectedTab == idx) filled else outlined,
                                                        contentDescription = label
                                                    )
                                                },
                                                label = { Text(label) },
                                                modifier = Modifier.testTag("nav_tab_$idx")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (useNavRail) {
                                    NavigationRail {
                                        navItems.forEachIndexed { idx, (label, filled, outlined) ->
                                            NavigationRailItem(
                                                selected = selectedTab == idx,
                                                onClick = { selectedTab = idx },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selectedTab == idx) filled else outlined,
                                                        contentDescription = label
                                                    )
                                                },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                ) {
                                    val bannerTitle = if (arabicPrimary) appOperations.bannerTitleAr.ifBlank { appOperations.bannerTitleEn }
                                    else appOperations.bannerTitleEn.ifBlank { appOperations.bannerTitleAr }
                                    val bannerBody = if (arabicPrimary) appOperations.bannerBodyAr.ifBlank { appOperations.bannerBodyEn }
                                    else appOperations.bannerBodyEn.ifBlank { appOperations.bannerBodyAr }
                                    val bannerSignature = "$bannerTitle|$bannerBody|${appOperations.bannerTone}"

                                    if (appOperations.bannerEnabled &&
                                        (bannerTitle.isNotBlank() || bannerBody.isNotBlank()) &&
                                        bannerDismissedKey != bannerSignature
                                    ) {
                                        val toneContainer = when (appOperations.bannerTone) {
                                            "URGENT" -> MaterialTheme.colorScheme.errorContainer
                                            "SUCCESS" -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                        val toneContent = when (appOperations.bannerTone) {
                                            "URGENT" -> MaterialTheme.colorScheme.onErrorContainer
                                            "SUCCESS" -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = toneContainer),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                                .testTag("global_broadcast_banner")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = MihrabIcons.BroadcastBanner,
                                                    contentDescription = null,
                                                    tint = toneContent
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    if (bannerTitle.isNotBlank()) {
                                                        Text(
                                                            text = bannerTitle,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = toneContent
                                                        )
                                                    }
                                                    if (bannerBody.isNotBlank()) {
                                                        Text(
                                                            text = bannerBody,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = toneContent
                                                        )
                                                    }
                                                }
                                                IconButton(onClick = { bannerDismissedKey = bannerSignature }) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = strings.cancel,
                                                        tint = toneContent
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (appOperations.maintenanceMode) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.WarningAmber,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Column {
                                                    Text(
                                                        text = strings.maintenanceNoticeTitle,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                    Text(
                                                        text = strings.maintenanceNoticeBody,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        when (selectedTab) {
                                            0 -> PrayerScreen(
                                                viewModel = mihrabViewModel,
                                                adhanAudioPlayer = adhanAudioPlayer,
                                                appLanguage = appLanguage,
                                                strings = strings,
                                                useIndic = useIndic,
                                                appOperations = appOperations,
                                                onOpenKhutbahAi = {
                                                    learnSubTab = 3
                                                    selectedTab = 3
                                                }
                                            )
                                            1 -> QuranScreen(
                                                viewModel = mihrabViewModel,
                                                aiViewModel = aiViewModel,
                                                audioPlayer = audioPlayer,
                                                recitationPlayer = recitationPlayer,
                                                appLanguage = appLanguage,
                                                strings = strings,
                                                appOperations = appOperations,
                                                onOpenAiAssistant = {
                                                    learnSubTab = 3
                                                    selectedTab = 3
                                                }
                                            )
                                            2 -> DhikrScreen(
                                                viewModel = mihrabViewModel,
                                                audioPlayer = audioPlayer,
                                                currentUserId = currentUserId,
                                                appLanguage = appLanguage,
                                                strings = strings,
                                                useIndic = useIndic,
                                                appOperations = appOperations
                                            )
                                            3 -> LearnScreen(
                                                viewModel = mihrabViewModel,
                                                aiViewModel = aiViewModel,
                                                audioPlayer = audioPlayer,
                                                appLanguage = appLanguage,
                                                strings = strings,
                                                useIndic = useIndic,
                                                initialSubTab = learnSubTab,
                                                appOperations = appOperations
                                            )
                                            4 -> ToolsScreen(
                                                viewModel = mihrabViewModel,
                                                strings = strings,
                                                appLanguage = appLanguage,
                                                currentThemePreset = themePreset,
                                                onChangeThemePreset = onChangeThemePreset,
                                                currentThemeMode = themeMode,
                                                useIndic = useIndic,
                                                onToggleIndic = { mihrabViewModel.toggleIndicNumerals(it) },
                                                isSuperAdmin = isSuperAdmin,
                                                onOpenAdminDashboard = { if (isSuperAdmin) showAdminDashboard = true },
                                                appOperations = appOperations,
                                                onSignOut = onSignOut
                                            )
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
}
