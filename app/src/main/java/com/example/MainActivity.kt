package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
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
import com.example.core.hardware.PrayerStatusNotificationManager
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.hardware.RecitationPlayerHolder
import com.example.core.hardware.RemoteBannerImage
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.stringsFor
import com.example.core.preferences.FeatureUsageTracker
import com.example.core.preferences.SmartFeatureId
import com.example.data.firebase.AdminAccess
import com.example.data.firebase.AdminRepository
import com.example.data.firebase.MihrabRepository
import com.example.data.firebase.UserSettingsDoc
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.isFirebaseInitialized
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
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.FloatingLiquidGlassDock
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.MihrabTheme
import com.example.ui.theme.MihrabThemePreset
import com.example.ui.theme.SacredAmbientCanvas
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.json.JSONObject

private const val AUTH_LANG_PREFS = "mihrab_auth_lang"
private const val AUTH_LANG_CODE = "lang_code"
private const val AUTH_LANG_PENDING = "pending_apply"

class MainActivity : ComponentActivity() {
    private val launchTabTrigger = MutableStateFlow<Pair<String, Int>?>(null)
    private var launchNonce = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check wiring with launch Intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        intent?.getStringExtra("launch_tab")?.let { tab ->
            launchTabTrigger.value = tab to (++launchNonce)
        }

        setContent {
            MihrabRootApp(launchTabFlow = launchTabTrigger)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("launch_tab")?.let { tab ->
            launchTabTrigger.value = tab to (++launchNonce)
        }
    }
}

@Composable
private fun MihrabRootApp(launchTabFlow: StateFlow<Pair<String, Int>?>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val firebaseReady = remember(context) { isFirebaseInitialized(context) }
    val launchTrigger by launchTabFlow.collectAsStateWithLifecycle()

    val currentUser by remember(firebaseReady) {
        if (firebaseReady) {
            Firebase.auth.authStateFlow()
        } else {
            flowOf<FirebaseUser?>(null)
        }
    }.collectAsStateWithLifecycle(
        initialValue = if (firebaseReady) runCatching { Firebase.auth.currentUser }.getOrNull() else null
    )

    var themePreset by rememberSaveable { mutableStateOf(MihrabThemePreset.EMERALD_SANCTUARY) }
    var authLanguage by remember {
        val prefs = context.getSharedPreferences(AUTH_LANG_PREFS, Context.MODE_PRIVATE)
        mutableStateOf(AppLanguage.fromCode(prefs.getString(AUTH_LANG_CODE, "ar") ?: "ar"))
    }

    LaunchedEffect(firebaseReady) {
        if (firebaseReady) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        val authStrings = remember(authLanguage) { stringsFor(authLanguage) }
        MihrabTheme(
            preset = themePreset,
            themeMode = AppThemeMode.SYSTEM,
            fontScale = 1.0f,
            arabicPrimary = authLanguage.isArabicUi
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AuthScreen(
                    selectedLanguage = authLanguage,
                    onSelectLanguage = { lang ->
                        authLanguage = lang
                        context.getSharedPreferences(AUTH_LANG_PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putString(AUTH_LANG_CODE, lang.code)
                            .putBoolean(AUTH_LANG_PENDING, true)
                            .apply()
                    },
                    strings = authStrings,
                    signInButtonText = authStrings.signInWithGoogle,
                    onAuthSuccess = {},
                    firebaseConfigMissing = !firebaseReady
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
                launchTrigger = launchTrigger,
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
    launchTrigger: Pair<String, Int>?,
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
        RecitationPlayerHolder.getOrCreate(
            context = context,
            audioCacheDao = localDb.audioCacheDao(),
            mushafDao = localDb.mushafDao()
        )
    }

    // Keep QuranRecitationPlayer alive across Activity backgrounding/exit so Foreground Audio Service continues!
    DisposableEffect(audioPlayer, adhanAudioPlayer) {
        onDispose {
            audioPlayer.release()
            adhanAudioPlayer.release()
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
    LaunchedEffect(remoteSettings) {
        if (remoteSettings != null) {
            val prefs = context.getSharedPreferences(AUTH_LANG_PREFS, Context.MODE_PRIVATE)
            if (prefs.getBoolean(AUTH_LANG_PENDING, false)) {
                val chosen = prefs.getString(AUTH_LANG_CODE, "ar") ?: "ar"
                prefs.edit().putBoolean(AUTH_LANG_PENDING, false).apply()
                if (chosen != remoteSettings.preferredLanguage) {
                    mihrabViewModel.updateSettings(mapOf("preferredLanguage" to chosen))
                }
            }
        }
    }

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
    val manualOffsets by mihrabViewModel.manualOffsets.collectAsStateWithLifecycle()

    LaunchedEffect(effectiveSettings, manualOffsets, appLanguage, useIndic, appOperations) {
        PrayerStatusNotificationManager.syncAndSchedule(
            context = context,
            settings = effectiveSettings,
            offsets = manualOffsets,
            language = appLanguage,
            useIndic = useIndic,
            appOperations = appOperations
        )
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var quranSubTab by rememberSaveable { mutableIntStateOf(0) }
    var quranSubTabNonce by rememberSaveable { mutableIntStateOf(0) }
    var learnSubTab by rememberSaveable { mutableIntStateOf(0) }
    var learnSubTabNonce by rememberSaveable { mutableIntStateOf(0) }
    var dhikrSubTab by rememberSaveable { mutableIntStateOf(0) }
    var dhikrSubTabNonce by rememberSaveable { mutableIntStateOf(0) }
    var toolsSubTab by rememberSaveable { mutableIntStateOf(0) }
    var toolsSubTabNonce by rememberSaveable { mutableIntStateOf(0) }
    var pendingAiPrompt by rememberSaveable { mutableStateOf<String?>(null) }
    var showAdminDashboard by rememberSaveable { mutableStateOf(false) }
    var bannerDismissedKey by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(launchTrigger) {
        when (launchTrigger?.first) {
            "prayer" -> selectedTab = 0
            "quran" -> {
                quranSubTab = 0
                quranSubTabNonce += 1
                selectedTab = 1
            }
            "dhikr" -> {
                dhikrSubTab = 1
                dhikrSubTabNonce += 1
                selectedTab = 2
            }
            "learn" -> {
                learnSubTab = 3
                learnSubTabNonce += 1
                selectedTab = 3
            }
            "tools" -> {
                toolsSubTab = 2
                toolsSubTabNonce += 1
                selectedTab = 4
            }
        }
    }

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
        val glass = LocalLiquidGlassPalette.current
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
                    SacredAmbientCanvas {
                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                Column {
                                    TopAppBar(
                                        title = {
                                            Text(
                                                "${strings.appTitle} • ${appLanguage.pick("أطفال", "Kids", "Bolalar")}",
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = glass.dockBackground)
                                    )
                                    HorizontalDivider(color = glass.glassBorder, thickness = 0.8.dp)
                                }
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
                                    appLanguage = appLanguage,
                                    strings = strings,
                                    onExitKidsMode = { mihrabViewModel.updateSettings(mapOf("kidsMode" to false)) }
                                )
                            }
                        }
                    }
                } else {
                    SacredAmbientCanvas {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val useNavRail = maxWidth >= 700.dp

                            Scaffold(
                                containerColor = Color.Transparent,
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                topBar = {
                                    Column {
                                        TopAppBar(
                                            title = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                                        border = BorderStroke(1.dp, glass.glassBorder),
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = MihrabIcons.MihrabArch,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = strings.appTitle,
                                                        style = MaterialTheme.typography.headlineMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            },
                                            actions = {
                                                if (isSuperAdmin) {
                                                    Surface(
                                                        onClick = { showAdminDashboard = true },
                                                        shape = CircleShape,
                                                        color = BurnishedGold.copy(alpha = 0.14f),
                                                        border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.5f)),
                                                        modifier = Modifier
                                                            .padding(end = 8.dp)
                                                            .size(40.dp)
                                                            .testTag("top_bar_admin_button")
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = MihrabIcons.AdminShield,
                                                                contentDescription = strings.adminDashboardTitle,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            colors = TopAppBarDefaults.topAppBarColors(containerColor = glass.dockBackground)
                                        )
                                        HorizontalDivider(color = glass.glassBorder, thickness = 0.8.dp)
                                    }
                                },
                                bottomBar = {
                                    if (!useNavRail) {
                                        FloatingLiquidGlassDock(
                                            items = navItems,
                                            selectedTab = selectedTab,
                                            onSelectTab = { selectedTab = it }
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    if (useNavRail) {
                                        NavigationRail(
                                            containerColor = glass.dockBackground
                                        ) {
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
                                        val bannerTitle = appLanguage.pick(
                                            appOperations.bannerTitleAr.ifBlank { appOperations.bannerTitleEn },
                                            appOperations.bannerTitleEn.ifBlank { appOperations.bannerTitleAr },
                                            appOperations.bannerTitleUz.ifBlank {
                                                appOperations.bannerTitleEn.ifBlank { appOperations.bannerTitleAr }
                                            }
                                        )
                                        val bannerBody = appLanguage.pick(
                                            appOperations.bannerBodyAr.ifBlank { appOperations.bannerBodyEn },
                                            appOperations.bannerBodyEn.ifBlank { appOperations.bannerBodyAr },
                                            appOperations.bannerBodyUz.ifBlank {
                                                appOperations.bannerBodyEn.ifBlank { appOperations.bannerBodyAr }
                                            }
                                        )
                                        val bannerCta = appLanguage.pick(
                                            appOperations.bannerCtaAr.ifBlank { "افتح الرابط" },
                                            appOperations.bannerCtaEn.ifBlank { "Open Link" },
                                            appOperations.bannerCtaUz.ifBlank { "Havolani ochish" }
                                        )
                                        val bannerLink = appOperations.bannerLinkUrl.trim()
                                        val hasImage = appOperations.bannerImageUrl.isNotBlank()
                                        val bannerSignature = "$bannerTitle|$bannerBody|${appOperations.bannerImageUrl.take(48)}|$bannerLink|${appOperations.bannerTone}"

                                        if (appOperations.bannerEnabled &&
                                            (bannerTitle.isNotBlank() || bannerBody.isNotBlank() || hasImage) &&
                                            bannerDismissedKey != bannerSignature
                                        ) {
                                            val toneBorder = when (appOperations.bannerTone) {
                                                "URGENT", "ALERT" -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                                                "SUCCESS" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            }
                                            val toneContent = when (appOperations.bannerTone) {
                                                "URGENT", "ALERT" -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                            val openBannerLink = {
                                                if (bannerLink.isNotBlank()) {
                                                    val normalized = if (bannerLink.startsWith("http://") || bannerLink.startsWith("https://")) {
                                                        bannerLink
                                                    } else {
                                                        "https://$bannerLink"
                                                    }
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                            LiquidGlassCard(
                                                shape = RoundedCornerShape(20.dp),
                                                containerColor = glass.glassFillStrong,
                                                borderColor = toneBorder,
                                                contentPadding = 12.dp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                                    .then(
                                                        if (bannerLink.isNotBlank()) Modifier.clickable { openBannerLink() } else Modifier
                                                    )
                                                    .testTag("global_broadcast_banner")
                                            ) {
                                                if (hasImage) {
                                                    RemoteBannerImage(
                                                        imageUrl = appOperations.bannerImageUrl,
                                                        contentDescription = bannerTitle.ifBlank { null },
                                                        maxHeight = 148.dp
                                                    )
                                                }
                                                Row(
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
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                        if (bannerLink.isNotBlank()) {
                                                            Spacer(Modifier.height(6.dp))
                                                            Surface(
                                                                onClick = openBannerLink,
                                                                color = toneContent.copy(alpha = 0.14f),
                                                                border = BorderStroke(1.dp, toneContent.copy(alpha = 0.4f)),
                                                                shape = CircleShape
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                                        contentDescription = null,
                                                                        tint = toneContent,
                                                                        modifier = Modifier.size(13.dp)
                                                                    )
                                                                    Text(
                                                                        text = bannerCta,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = toneContent
                                                                    )
                                                                }
                                                            }
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
                                            LiquidGlassCard(
                                                shape = RoundedCornerShape(20.dp),
                                                containerColor = glass.glassFillStrong,
                                                borderColor = BurnishedGold.copy(alpha = 0.55f),
                                                contentPadding = 12.dp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.WarningAmber,
                                                        contentDescription = null,
                                                        tint = BurnishedGold
                                                    )
                                                    Column {
                                                        Text(
                                                            text = strings.maintenanceNoticeTitle,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BurnishedGold
                                                        )
                                                        Text(
                                                            text = strings.maintenanceNoticeBody,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface
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
                                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.AI_COMPANION)
                                                        learnSubTab = 3
                                                        learnSubTabNonce += 1
                                                        selectedTab = 3
                                                    },
                                                    onOpenMushaf = {
                                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.MUSHAF)
                                                        quranSubTab = 0
                                                        quranSubTabNonce += 1
                                                        selectedTab = 1
                                                    },
                                                    onOpenKhatmah = {
                                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.KHATMAH)
                                                        quranSubTab = 2
                                                        quranSubTabNonce += 1
                                                        selectedTab = 1
                                                    },
                                                    onOpenAdhkar = {
                                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.ADHKAR)
                                                        dhikrSubTab = 0
                                                        dhikrSubTabNonce += 1
                                                        selectedTab = 2
                                                    },
                                                    onOpenTasbih = {
                                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.TASBIH)
                                                        dhikrSubTab = 1
                                                        dhikrSubTabNonce += 1
                                                        selectedTab = 2
                                                    },
                                                    onOpenAi = { prompt ->
                                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.AI_COMPANION)
                                                        if (!prompt.isNullOrBlank()) {
                                                            pendingAiPrompt = prompt
                                                        }
                                                        learnSubTab = 3
                                                        learnSubTabNonce += 1
                                                        selectedTab = 3
                                                    },
                                                    onOpenHijri = {
                                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.HIJRI)
                                                        learnSubTab = 0
                                                        learnSubTabNonce += 1
                                                        selectedTab = 3
                                                    },
                                                    onOpenZakat = {
                                                        FeatureUsageTracker.recordUsage(context, SmartFeatureId.ZAKAT)
                                                        toolsSubTab = 0
                                                        toolsSubTabNonce += 1
                                                        selectedTab = 4
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
                                                    initialSubTab = quranSubTab,
                                                    subTabTriggerNonce = quranSubTabNonce,
                                                    onOpenAiAssistant = {
                                                        learnSubTab = 3
                                                        learnSubTabNonce += 1
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
                                                    appOperations = appOperations,
                                                    initialSubTab = dhikrSubTab,
                                                    subTabTriggerNonce = dhikrSubTabNonce
                                                )
                                                3 -> LearnScreen(
                                                    viewModel = mihrabViewModel,
                                                    aiViewModel = aiViewModel,
                                                    audioPlayer = audioPlayer,
                                                    appLanguage = appLanguage,
                                                    strings = strings,
                                                    useIndic = useIndic,
                                                    initialSubTab = learnSubTab,
                                                    subTabTriggerNonce = learnSubTabNonce,
                                                    prefilledAiQuestion = pendingAiPrompt,
                                                    onConsumePrefilledQuestion = { pendingAiPrompt = null },
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
                                                    initialSection = toolsSubTab,
                                                    sectionNonce = toolsSubTabNonce,
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
}
