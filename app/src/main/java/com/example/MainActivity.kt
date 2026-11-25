package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.hardware.AdhanAudioPlayer
import com.example.core.hardware.AdhanRuntimeController
import com.example.core.hardware.DynamicIslandReciterOverlay
import com.example.core.hardware.MihrabReminderManager
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) &&
            AdhanRuntimeController.isRingingNow()
        ) {
            AdhanRuntimeController.stopAdhan(this)
            return true
        }
        return super.dispatchKeyEvent(event)
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
                    appContext = context.applicationContext,
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
    val reciters by mihrabViewModel.recitersState.collectAsStateWithLifecycle()

    LaunchedEffect(reciters) {
        recitationPlayer.setAvailableReciters(reciters)
    }

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

    LaunchedEffect(effectiveSettings, manualOffsets, appLanguage, useIndic, localPrefs?.customAdhanLocalPath) {
        MihrabReminderManager.syncAndScheduleAll(
            context = context,
            settings = effectiveSettings,
            offsets = manualOffsets,
            language = appLanguage,
            useIndic = useIndic,
            customAdhanPath = localPrefs?.customAdhanLocalPath.orEmpty()
        )
    }

    val ringingAdhan by AdhanRuntimeController.ringing.collectAsStateWithLifecycle()
    val isDelegatedAdmin = remoteSettings?.adminRole == "ADMIN" && remoteSettings.accountSuspended != true
    val isAdmin = isSuperAdmin || isDelegatedAdmin
    val isAccountSuspended = remoteSettings?.accountSuspended == true && !isSuperAdmin

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

    if (showAdminDashboard && isAdmin && !isAccountSuspended) {
        BackHandler { showAdminDashboard = false }
    } else if (selectedTab != 0 && !isAccountSuspended) {
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
                if (isAccountSuspended) {
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
                                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                contentPadding = 22.dp,
                                modifier = Modifier
                                    .widthIn(max = 480.dp)
                                    .testTag("suspended_account_screen")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = appLanguage.pick(
                                            "تم تعليق حسابك مؤقتاً",
                                            "Account Suspended",
                                            "Hisobingiz vaqtincha to'xtatilgan"
                                        ),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = appLanguage.pick(
                                            "قامت إدارة تطبيق محراب بتعليق الوصول لهذا الحساب. يمكنك التواصل مع فريق الدعم الفني لمراجعة الحساب.",
                                            "An administrator has suspended this account. Please contact Support to review your access.",
                                            "Ma'muriyat ushbu hisobni vaqtincha to'xtatdi. Qo'llab-quvvatlash xizmatiga murojaat qiling."
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val digits = appOperations.supportWhatsapp.filter { it.isDigit() }
                                            if (digits.isNotBlank()) {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${appLanguage.pick("واتساب الدعم:", "WhatsApp Support:", "WhatsApp:")} ${appOperations.supportWhatsapp}")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val rawTg = appOperations.supportTelegram.trim().replace(" ", "").replace("-", "")
                                            val tgUrl = if (rawTg.startsWith("@")) {
                                                "https://t.me/${rawTg.removePrefix("@")}"
                                            } else {
                                                "https://t.me/$rawTg"
                                            }
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(tgUrl))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${appLanguage.pick("تيليجرام الدعم:", "Telegram Support:", "Telegram:")} ${appOperations.supportTelegram}")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${appOperations.supportEmail.trim()}"))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${appLanguage.pick("البريد:", "Email:", "Email:")} ${appOperations.supportEmail}")
                                    }
                                    Button(
                                        onClick = onSignOut,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("sign_out_suspended_button")
                                    ) {
                                        Text(strings.signOut)
                                    }
                                }
                            }
                        }
                    }
                } else if (showAdminDashboard && isAdmin) {
                    val adminRepository = remember(db) { AdminRepository(db = db) }
                    val adminViewModel: AdminViewModel = viewModel(
                        key = "admin_${currentUserId}_$isDelegatedAdmin",
                        factory = viewModelFactory {
                            initializer {
                                AdminViewModel(
                                    adminRepository = adminRepository,
                                    mihrabRepository = sharedRepository,
                                    currentUserId = currentUserId,
                                    isDelegatedAdmin = isDelegatedAdmin
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
                                                if (isAdmin) {
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
                                        ringingAdhan?.let { activeAdhan ->
                                            LiquidGlassCard(
                                                shape = RoundedCornerShape(20.dp),
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                                borderColor = BurnishedGold,
                                                contentPadding = 12.dp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                                    .testTag("active_adhan_ringing_banner")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = MihrabIcons.AdhanVoice,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = if (activeAdhan.isTahajjud) {
                                                                appLanguage.pick(
                                                                    "منبه التهجد والقيام • ${com.example.core.i18n.formatNumerals(activeAdhan.prayerTime, useIndic && appLanguage.isArabicUi)}",
                                                                    "Tahajjud Alert • ${activeAdhan.prayerTime}",
                                                                    "Tahajjud vaqti • ${activeAdhan.prayerTime}"
                                                                )
                                                            } else {
                                                                appLanguage.pick(
                                                                    "حان الآن وقت ${activeAdhan.prayerLabel} • ${com.example.core.i18n.formatNumerals(activeAdhan.prayerTime, useIndic && appLanguage.isArabicUi)}",
                                                                    "Prayer Time: ${activeAdhan.prayerLabel} • ${activeAdhan.prayerTime}",
                                                                    "Namoz vaqti: ${activeAdhan.prayerLabel} • ${activeAdhan.prayerTime}"
                                                                )
                                                            },
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                        Text(
                                                            text = appLanguage.pick(
                                                                "${activeAdhan.muezzinLabel} • اضغط زر الصوت أو الزر لإيقاف الصوت",
                                                                "${activeAdhan.muezzinLabel} • Press Volume key or button to stop",
                                                                "${activeAdhan.muezzinLabel} • To'xtatish uchun ovoz tugmasini bosing"
                                                            ),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                                        )
                                                    }
                                                    Surface(
                                                        onClick = { AdhanRuntimeController.stopAdhan(context) },
                                                        color = MaterialTheme.colorScheme.error,
                                                        shape = CircleShape,
                                                        modifier = Modifier.testTag("stop_ringing_adhan_button")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.NotificationsOff,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onError,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = appLanguage.pick("إيقاف", "Stop", "To'xtatish"),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onError
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

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
                                        val hasAdContent = bannerTitle.isNotBlank() ||
                                            bannerBody.isNotBlank() ||
                                            hasImage ||
                                            bannerLink.isNotBlank()
                                        val bannerSignature = "$bannerTitle|$bannerBody|${appOperations.bannerImageUrl.take(48)}|$bannerLink|${appOperations.bannerTone}"

                                        LaunchedEffect(appOperations.bannerEnabled, hasAdContent, bannerSignature) {
                                            if (appOperations.bannerEnabled && hasAdContent) {
                                                MihrabReminderManager.postBannerAnnouncementNotificationIfNew(
                                                    context = context,
                                                    signature = bannerSignature,
                                                    title = bannerTitle,
                                                    body = bannerBody,
                                                    imageUrl = appOperations.bannerImageUrl,
                                                    linkUrl = bannerLink,
                                                    language = appLanguage
                                                )
                                            }
                                        }

                                        val toneBorder = when (appOperations.bannerTone) {
                                            "URGENT", "ALERT" -> MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
                                            "SUCCESS" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                                            else -> BurnishedGold.copy(alpha = 0.65f)
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

                                        if (!showAdminDashboard && appOperations.bannerEnabled && hasAdContent) {
                                            Surface(
                                                onClick = { bannerDismissedKey = "" },
                                                color = glass.glassFillStrong,
                                                border = BorderStroke(1.dp, toneBorder),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                                    .testTag("top_announcement_strip")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = MihrabIcons.BroadcastBanner,
                                                        contentDescription = null,
                                                        tint = toneContent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = bannerTitle.ifBlank {
                                                            bannerBody.ifBlank {
                                                                appLanguage.pick("إعلان وتنبيه هام في محراب", "Important Announcement", "Muhim e'lon")
                                                            }
                                                        },
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = toneContent,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Surface(
                                                        onClick = {
                                                            if (bannerLink.isNotBlank() && !hasImage && bannerBody.isBlank()) {
                                                                openBannerLink()
                                                            } else {
                                                                bannerDismissedKey = ""
                                                            }
                                                        },
                                                        color = toneContent.copy(alpha = 0.14f),
                                                        shape = CircleShape
                                                    ) {
                                                        Text(
                                                            text = if (bannerLink.isNotBlank() && !hasImage && bannerBody.isBlank()) {
                                                                bannerCta
                                                            } else {
                                                                appLanguage.pick("عرض", "View", "Ko'rish")
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = toneContent,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (!showAdminDashboard &&
                                            appOperations.bannerEnabled &&
                                            hasAdContent &&
                                            bannerDismissedKey != bannerSignature
                                        ) {
                                            Dialog(onDismissRequest = { bannerDismissedKey = bannerSignature }) {
                                                LiquidGlassCard(
                                                    shape = RoundedCornerShape(26.dp),
                                                    containerColor = MaterialTheme.colorScheme.surface,
                                                    borderColor = toneBorder,
                                                    elevation = 14.dp,
                                                    contentPadding = 16.dp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .verticalScroll(rememberScrollState())
                                                        .testTag("app_entry_ad_dialog")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            color = toneContent.copy(alpha = 0.14f),
                                                            shape = CircleShape
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = MihrabIcons.BroadcastBanner,
                                                                    contentDescription = null,
                                                                    tint = toneContent,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                                Text(
                                                                    text = appLanguage.pick("إعلان / إشعار هام", "Announcement", "Muhim e'lon"),
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = toneContent
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = { bannerDismissedKey = bannerSignature },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Close,
                                                                contentDescription = strings.cancel,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    if (hasImage) {
                                                        RemoteBannerImage(
                                                            imageUrl = appOperations.bannerImageUrl,
                                                            contentDescription = bannerTitle.ifBlank { null },
                                                            maxHeight = 220.dp,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .then(
                                                                    if (bannerLink.isNotBlank()) Modifier.clickable { openBannerLink() } else Modifier
                                                                )
                                                        )
                                                    }

                                                    if (bannerTitle.isNotBlank()) {
                                                        Text(
                                                            text = bannerTitle,
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    if (bannerBody.isNotBlank()) {
                                                        Text(
                                                            text = bannerBody,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (bannerLink.isNotBlank()) {
                                                            Button(
                                                                onClick = {
                                                                    openBannerLink()
                                                                    bannerDismissedKey = bannerSignature
                                                                },
                                                                shape = RoundedCornerShape(14.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(Modifier.width(6.dp))
                                                                Text(bannerCta)
                                                            }
                                                        }
                                                        TextButton(
                                                            onClick = { bannerDismissedKey = bannerSignature },
                                                            modifier = if (bannerLink.isBlank()) Modifier.fillMaxWidth() else Modifier
                                                        ) {
                                                            Text(appLanguage.pick("متابعة إلى محراب", "Continue", "Davom etish"))
                                                        }
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
                                                    isSuperAdmin = isAdmin,
                                                    onOpenAdminDashboard = { if (isAdmin) showAdminDashboard = true },
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

                            DynamicIslandReciterOverlay(
                                recitationPlayer = recitationPlayer,
                                reciters = reciters,
                                appLanguage = appLanguage,
                                strings = strings,
                                useIndic = useIndic,
                                onOpenMushaf = {
                                    quranSubTab = 0
                                    quranSubTabNonce += 1
                                    selectedTab = 1
                                },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}
