package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.hardware.QuranTtsAudioPlayer
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.stringsFor
import com.example.data.firebase.MihrabRepository
import com.example.data.firebase.UserSettingsDoc
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.performSignOut
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DhikrScreen
import com.example.ui.screens.KidsModeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.PrayerScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MihrabTheme
import com.example.ui.theme.MihrabThemePreset
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

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
    var darkModeOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    MihrabTheme(
        preset = themePreset,
        darkTheme = darkModeOverride ?: androidx.compose.foundation.isSystemInDarkTheme()
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    signInButtonText = stringsFor(AppLanguage.ENGLISH).signInWithGoogle,
                    onAuthSuccess = {}
                )
            } else {
                val uid = user.uid
                key(uid) {
                    AuthenticatedMihrabShell(
                        currentUserId = uid,
                        displayName = user.displayName ?: "Believer",
                        initialTab = initialTab,
                        themePreset = themePreset,
                        onChangeThemePreset = { themePreset = it },
                        darkModeOverride = darkModeOverride,
                        onChangeDarkMode = { darkModeOverride = it },
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
    darkModeOverride: Boolean?,
    onChangeDarkMode: (Boolean?) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    val mihrabViewModel: MihrabViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MihrabViewModel(
                    repository = MihrabRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val aiViewModel: AiViewModel = viewModel(key = "ai_$currentUserId")
    val audioPlayer = remember(context) { QuranTtsAudioPlayer(context) }
    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.release() }
    }

    val settingsState by mihrabViewModel.settingsState.collectAsStateWithLifecycle()
    val statusMessage by mihrabViewModel.statusMessage.collectAsStateWithLifecycle()
    val useIndic by mihrabViewModel.useIndicNumerals.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val appLanguage = AppLanguage.fromCode(settings.preferredLanguage)
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

    val navItems = listOf(
        Triple(strings.tabPrayer, Icons.Rounded.Mosque, Icons.Outlined.Mosque),
        Triple(strings.tabQuran, Icons.Rounded.AutoStories, Icons.Outlined.AutoStories),
        Triple(strings.tabDhikr, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder),
        Triple(strings.tabLearn, Icons.Rounded.School, Icons.Outlined.School),
        Triple(strings.tabTools, Icons.Rounded.Build, Icons.Outlined.Build)
    )

    CompositionLocalProvider(LocalLayoutDirection provides appLanguage.layoutDirection) {
        if (settings.kidsMode) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("${strings.appTitle} • Kids") },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                            title = { Text(strings.appTitle, style = MaterialTheme.typography.headlineMedium) },
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
                    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when (selectedTab) {
                                0 -> PrayerScreen(
                                    viewModel = mihrabViewModel,
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
                                    appLanguage = appLanguage
                                )
                                2 -> DhikrScreen(
                                    viewModel = mihrabViewModel,
                                    audioPlayer = audioPlayer,
                                    currentUserId = currentUserId,
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
                                    currentThemePreset = themePreset,
                                    onChangeThemePreset = onChangeThemePreset,
                                    darkModeOverride = darkModeOverride,
                                    onChangeDarkMode = onChangeDarkMode,
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
