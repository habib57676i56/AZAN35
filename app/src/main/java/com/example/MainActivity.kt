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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
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
import com.example.data.firebase.MihrabRepository
import com.example.data.firebase.UserSettingsDoc
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.performSignOut
import com.example.data.local.MihrabLocalDatabase
import com.example.data.quran.MushafRepository
import com.example.ui.icons.MihrabIcons
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
        key(uid) {
            AuthenticatedMihrabShell(
                currentUserId = uid,
                displayName = user.displayName ?: "مؤمن",
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
    initialTab: Int,
    themePreset: MihrabThemePreset,
    onChangeThemePreset: (MihrabThemePreset) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val localDb = remember(context) { MihrabLocalDatabase.getInstance(context) }
    val sharedRepository = remember(context, localDb) {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        MihrabRepository(
            db = db,
            localPrefsDao = localDb.localPrefsDao(),
            reciterDao = localDb.reciterDao(),
            adhanVoiceDao = localDb.adhanVoiceDao(),
            adhkarDao = localDb.adhkarDao()
        )
    }

    val mihrabViewModel: MihrabViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                MihrabViewModel(
                    repository = sharedRepository,
                    mushafRepository = MushafRepository(localDb.mushafDao()),
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

    if (selectedTab != 0) {
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
                if (effectiveSettings.kidsMode) {
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
                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                ) {
                                    when (selectedTab) {
                                        0 -> PrayerScreen(
                                            viewModel = mihrabViewModel,
                                            adhanAudioPlayer = adhanAudioPlayer,
                                            appLanguage = appLanguage,
                                            strings = strings,
                                            useIndic = useIndic,
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
                                            useIndic = useIndic
                                        )
                                        3 -> LearnScreen(
                                            viewModel = mihrabViewModel,
                                            aiViewModel = aiViewModel,
                                            audioPlayer = audioPlayer,
                                            appLanguage = appLanguage,
                                            strings = strings,
                                            useIndic = useIndic,
                                            initialSubTab = learnSubTab
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
