package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.LauncherShortcutHelper
import com.example.core.hardware.LauncherShortcutTarget
import com.example.core.hardware.LocationAndMosqueLocator
import com.example.core.hardware.PrayerStatusNotificationManager
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.core.i18n.formatNumerals
import com.example.core.preferences.FeatureUsageTracker
import com.example.core.preferences.SmartFeatureId
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.UserSettingsDoc
import com.example.data.firebase.clearCustomAppCheckDebugToken
import com.example.data.firebase.getAppCheckDebugSecret
import com.example.data.firebase.getCustomAppCheckOverride
import com.example.data.firebase.saveCustomAppCheckDebugToken
import com.example.domain.CalculatorsEngine
import com.example.domain.IslamicLibraryData
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.IslamicGeometricPatternCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.MihrabThemePreset
import com.example.ui.theme.MushafPaperTheme
import com.example.ui.theme.withTabularNums
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ToolsScreen(
    viewModel: MihrabViewModel,
    strings: LocalizedStrings,
    appLanguage: AppLanguage,
    currentThemePreset: MihrabThemePreset,
    onChangeThemePreset: (MihrabThemePreset) -> Unit,
    currentThemeMode: AppThemeMode,
    useIndic: Boolean,
    onToggleIndic: (Boolean) -> Unit,
    isSuperAdmin: Boolean = false,
    onOpenAdminDashboard: () -> Unit = {},
    initialSection: Int = 0,
    sectionNonce: Int = 0,
    appOperations: AppOperationsConfig = AppOperationsConfig(),
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val obligationsState by viewModel.obligationsState.collectAsStateWithLifecycle()
    val exportedJson by viewModel.exportedJson.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val isArabicUi = appLanguage.isArabicUi
    val useArabicDigits = useIndic && isArabicUi
    var activeSection by remember { mutableIntStateOf(initialSection) } // 0=Calculators, 1=Pilgrim&Charity, 2=Settings
    LaunchedEffect(initialSection, sectionNonce) {
        activeSection = initialSection
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = exportedJson
        if (uri != null && json != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                }
            } catch (_: Exception) {
            }
        }
        viewModel.clearExportedJson()
    }

    LaunchedEffect(exportedJson) {
        if (exportedJson != null) {
            createDocLauncher.launch("mihrab_backup.json")
        }
    }

    val toolsTabs = listOf(
        appLanguage.pick("الحاسبات الشرعية", "Calculators", "Hisoblagichlar") to MihrabIcons.Calculators,
        appLanguage.pick(strings.pilgrimGuide, "${strings.pilgrimGuide} & Charity", "Haj va Sadaqa") to MihrabIcons.KaabaCube,
        strings.settingsPrivacy to MihrabIcons.Settings
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(toolsTabs.size) { idx ->
                        val (lbl, icon) = toolsTabs[idx]
                        LiquidGlassPill(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            icon = icon,
                            label = lbl,
                            modifier = Modifier.testTag("tools_tab_$idx")
                        )
                    }
                }
            }

            when (activeSection) {
                0 -> item { CalculatorsSection(appLanguage = appLanguage, useArabicDigits = useArabicDigits) }
                1 -> item {
                    PilgrimAndObligationsSection(
                        obligationsState = obligationsState,
                        appLanguage = appLanguage,
                        strings = strings,
                        useArabicDigits = useArabicDigits,
                        viewModel = viewModel
                    )
                }
                2 -> item {
                    SettingsAndPrivacySection(
                        settings = settings,
                        strings = strings,
                        appLanguage = appLanguage,
                        currentThemePreset = currentThemePreset,
                        onChangeThemePreset = onChangeThemePreset,
                        currentThemeMode = currentThemeMode,
                        useIndic = useIndic,
                        onToggleIndic = onToggleIndic,
                        isSuperAdmin = isSuperAdmin,
                        onOpenAdminDashboard = onOpenAdminDashboard,
                        appOperations = appOperations,
                        viewModel = viewModel,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalculatorsSection(
    appLanguage: AppLanguage,
    useArabicDigits: Boolean
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassPalette.current
    var calcTab by remember { mutableIntStateOf(0) } // 0=Zakat, 1=Inheritance, 2=Water

    LaunchedEffect(calcTab) {
        if (calcTab == 0) {
            FeatureUsageTracker.recordUsage(context, SmartFeatureId.ZAKAT)
        }
    }

    // Zakat states
    var cashInput by remember { mutableStateOf("5000") }
    var goldGramsInput by remember { mutableStateOf("20") }
    var goldPriceInput by remember { mutableStateOf("85") }
    var silverGramsInput by remember { mutableStateOf("0") }
    var silverPriceInput by remember { mutableStateOf("1.05") }
    var tradeInput by remember { mutableStateOf("0") }
    var debtInput by remember { mutableStateOf("0") }
    var useSilverNisab by remember { mutableStateOf(false) }

    // Inheritance states
    var estateInput by remember { mutableStateOf("120000") }
    var spouseType by remember { mutableStateOf("WIFE") }
    var hasFather by remember { mutableStateOf(true) }
    var hasMother by remember { mutableStateOf(true) }
    var sonsCount by remember { mutableIntStateOf(1) }
    var daughtersCount by remember { mutableIntStateOf(1) }

    // Water states
    var isGhusl by remember { mutableStateOf(false) }
    var tapSeconds by remember { mutableIntStateOf(30) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val labels = listOf(
                appLanguage.pick("الزكاة", "Zakat", "Zakot"),
                appLanguage.pick("المواريث (الفرائض)", "Inheritance (Faraid)", "Meros (Faroiz)"),
                appLanguage.pick("ماء الوضوء", "Wudu Water", "Tahorat suvi")
            )
            items(labels.size) { idx ->
                LiquidGlassPill(selected = calcTab == idx, onClick = { calcTab = idx }, label = labels[idx])
            }
        }

        when (calcTab) {
            0 -> {
                val res = CalculatorsEngine.calculateZakat(
                    cashAndSavings = cashInput.toDoubleOrNull() ?: 0.0,
                    goldGrams24k = goldGramsInput.toDoubleOrNull() ?: 0.0,
                    goldPricePerGram = goldPriceInput.toDoubleOrNull() ?: 85.0,
                    silverGrams = silverGramsInput.toDoubleOrNull() ?: 0.0,
                    silverPricePerGram = silverPriceInput.toDoubleOrNull() ?: 1.05,
                    tradeInventoryAndStocks = tradeInput.toDoubleOrNull() ?: 0.0,
                    shortTermDebtsOwed = debtInput.toDoubleOrNull() ?: 0.0,
                    useSilverNisab = useSilverNisab
                )
                LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                    Text(
                        appLanguage.pick(
                            "حاسبة الزكاة والنصاب (٢.٥٪)",
                            "Zakat & Nisab Calculator (2.5%)",
                            "Zakot va Nisob hisoblagichi (2.5%)"
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = cashInput,
                        onValueChange = { cashInput = it },
                        label = { Text(appLanguage.pick("النقد والمدخرات البنكية", "Cash & Bank Savings", "Naqd pul va bank jamg'armalari")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = goldGramsInput,
                            onValueChange = { goldGramsInput = it },
                            label = { Text(appLanguage.pick("الذهب (غ)", "Gold (g)", "Oltin (g)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = goldPriceInput,
                            onValueChange = { goldPriceInput = it },
                            label = { Text(appLanguage.pick("سعر الغرام", "Gold / g", "Oltin narxi / g")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = silverGramsInput,
                            onValueChange = { silverGramsInput = it },
                            label = { Text(appLanguage.pick("الفضة (غ)", "Silver (g)", "Kumush (g)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = silverPriceInput,
                            onValueChange = { silverPriceInput = it },
                            label = { Text(appLanguage.pick("الفضة / غ", "Silver / g", "Kumush / g")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tradeInput,
                            onValueChange = { tradeInput = it },
                            label = { Text(appLanguage.pick("عروض التجارة والأسهم", "Trade / Stocks", "Tijorat va aksiyalar")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = debtInput,
                            onValueChange = { debtInput = it },
                            label = { Text(appLanguage.pick("الديون الحالة", "Immediate Debts", "Qisqa muddatli qarzlar")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            appLanguage.pick(
                                "اعتماد نصاب الفضة (٥٩٥ غ — الأحوط للفقراء)",
                                "Use Silver Nisab (595g — safer for poor)",
                                "Kumush nisobini qo'llash (595g — faqirlar uchun afzal)"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = useSilverNisab, onCheckedChange = { useSilverNisab = it })
                    }
                    HorizontalDivider(color = glass.glassBorder)
                    Text(
                        formatNumerals(
                            "${appLanguage.pick("صافي المال الزكوي", "Net Zakatable Wealth", "Sof zakot mulki")}: ${String.format(Locale.US, "%.2f", res.netZakatableWealth)}",
                            useArabicDigits
                        ),
                        style = MaterialTheme.typography.titleMedium.withTabularNums()
                    )
                    Text(
                        formatNumerals(
                            "${appLanguage.pick("قيمة النصاب", "Active Nisab Threshold", "Faol nisob miqdori")}: ${String.format(Locale.US, "%.2f", res.activeNisabValue)}",
                            useArabicDigits
                        ),
                        style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = if (res.isZakatDue) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else glass.glassFill,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (res.isZakatDue) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else glass.glassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (res.isZakatDue) {
                                formatNumerals(
                                    "${appLanguage.pick("الزكاة الواجبة (بعد حولٍ قمري)", "Zakat Payable (after 1 lunar Hawl)", "To'lanadigan zakot (1 qamariy yildan so'ng)")}: ${String.format(Locale.US, "%.2f", res.zakatPayable)}",
                                    useArabicDigits
                                )
                            } else {
                                appLanguage.pick(
                                    "أقل من النصاب — لا تجب الزكاة بعد.",
                                    "Below Nisab threshold — Zakat is not obligatory.",
                                    "Nisobdan kam — hozircha zakot farz emas."
                                )
                            },
                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = if (res.isZakatDue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            1 -> {
                val shares = CalculatorsEngine.calculateInheritance(
                    netEstate = estateInput.toDoubleOrNull() ?: 0.0,
                    spouse = spouseType,
                    hasFather = hasFather,
                    hasMother = hasMother,
                    sons = sonsCount,
                    daughters = daughtersCount,
                    language = appLanguage
                )
                LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                    Text(
                        appLanguage.pick("حاسبة المواريث الشرعية (الفرائض)", "Islamic Inheritance (Mawarith / Faraid)", "Islomiy meros hisoblagichi (Faroiz)"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = estateInput,
                        onValueChange = { estateInput = it },
                        label = {
                            Text(
                                appLanguage.pick(
                                    "صافي التركة (بعد الديون والوصية في الثلث)",
                                    "Net Estate Value (after debts & Wasiyyah <= 1/3)",
                                    "Sof meros qiymati (qarzlar va 1/3 vasiyatdan keyin)"
                                )
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "NONE" to appLanguage.pick("بدون زوج/ة", "No Spouse", "Juftsiz"),
                            "HUSBAND" to appLanguage.pick("الزوج", "Husband", "Er"),
                            "WIFE" to appLanguage.pick("الزوجة", "Wife", "Xotin")
                        ).forEach { (k, l) ->
                            LiquidGlassPill(selected = spouseType == k, onClick = { spouseType = k }, label = l)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasFather, onCheckedChange = { hasFather = it })
                            Text(appLanguage.pick("الأب", "Father", "Ota"))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasMother, onCheckedChange = { hasMother = it })
                            Text(appLanguage.pick("الأم", "Mother", "Ona"))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatNumerals("${appLanguage.pick("الأبناء", "Sons", "O'g'illar")}: $sonsCount", useArabicDigits),
                            style = MaterialTheme.typography.bodyLarge.withTabularNums()
                        )
                        Row {
                            TextButton(onClick = { sonsCount = (sonsCount - 1).coerceAtLeast(0) }) { Text("-") }
                            TextButton(onClick = { sonsCount = (sonsCount + 1).coerceAtMost(12) }) { Text("+") }
                        }
                        Text(
                            formatNumerals("${appLanguage.pick("البنات", "Daughters", "Qizlar")}: $daughtersCount", useArabicDigits),
                            style = MaterialTheme.typography.bodyLarge.withTabularNums()
                        )
                        Row {
                            TextButton(onClick = { daughtersCount = (daughtersCount - 1).coerceAtLeast(0) }) { Text("-") }
                            TextButton(onClick = { daughtersCount = (daughtersCount + 1).coerceAtMost(12) }) { Text("+") }
                        }
                    }
                    HorizontalDivider(color = glass.glassBorder)
                    shares.forEach { share ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${share.heirLabel} (${share.fractionLabel})", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                formatNumerals(
                                    "${String.format(Locale.US, "%.1f", share.percentage)}% • ${String.format(Locale.US, "%.2f", share.amount)}",
                                    useArabicDigits
                                ),
                                style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            2 -> {
                val water = CalculatorsEngine.estimateAblutionWater(isGhusl, tapSeconds, appLanguage)
                LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WaterDrop, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            appLanguage.pick(
                                "مقياس ترشيد ماء الوضوء (المُد والصاع)",
                                "Prophetic Water Conservation (Mudd & Sa')",
                                "Tahoratda suvni tejash (Mud va So')"
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LiquidGlassPill(
                            selected = !isGhusl,
                            onClick = { isGhusl = false },
                            label = appLanguage.pick("وضوء (مُد ~ ٠.٧٥ لتر)", "Wudu (1 Mudd ~ 0.75L)", "Tahorat (1 mud ~ 0.75L)")
                        )
                        LiquidGlassPill(
                            selected = isGhusl,
                            onClick = { isGhusl = true },
                            label = appLanguage.pick("غسل (صاع ~ ٣ لتر)", "Ghusl (1 Sa' ~ 3L)", "G'usl (1 so' ~ 3L)")
                        )
                    }
                    Text(
                        formatNumerals(
                            appLanguage.pick(
                                "مدة فتح الصنبور: $tapSeconds ثانية",
                                "Tap open duration: $tapSeconds seconds",
                                "Jo'mrak ochiq vaqti: $tapSeconds soniya"
                            ),
                            useArabicDigits
                        ),
                        style = MaterialTheme.typography.labelLarge.withTabularNums()
                    )
                    Slider(value = tapSeconds.toFloat(), onValueChange = { tapSeconds = it.roundToInt() }, valueRange = 5f..180f)
                    Text(
                        formatNumerals(
                            appLanguage.pick(
                                "الاستهلاك التقديري: ${String.format(Locale.US, "%.1f", water.estimatedLiters)} لتر (~${water.ratio}x الهدي النبوي)",
                                "Estimated usage: ${String.format(Locale.US, "%.1f", water.estimatedLiters)} L (~${water.ratio}x Prophetic amount)",
                                "Taxminiy sarf: ${String.format(Locale.US, "%.1f", water.estimatedLiters)} L (~${water.ratio}x sunnat me'yori)"
                            ),
                            useArabicDigits
                        ),
                        style = MaterialTheme.typography.titleMedium.withTabularNums(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(water.feedback, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PilgrimAndObligationsSection(
    obligationsState: UiState<List<com.example.data.firebase.ObligationDoc>>,
    appLanguage: AppLanguage,
    strings: LocalizedStrings,
    useArabicDigits: Boolean,
    viewModel: MihrabViewModel
) {
    val glass = LocalLiquidGlassPalette.current
    var tawafRounds by remember { mutableIntStateOf(0) }
    var saiRounds by remember { mutableIntStateOf(0) }
    val checklist = remember { mutableStateListOf(false, false, false, false) }

    var obKind by remember { mutableStateOf("CHARITY") }
    var obTitle by remember { mutableStateOf("") }
    var obAmount by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
            Text(strings.pilgrimGuide, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LiquidGlassCard(shape = RoundedCornerShape(20.dp), contentPadding = 12.dp, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(appLanguage.pick("الطواف", "Tawaf", "Tavof"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(formatNumerals("$tawafRounds / 7", useArabicDigits), style = MaterialTheme.typography.headlineMedium.withTabularNums(), color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { tawafRounds = (tawafRounds + 1) % 8 }, shape = RoundedCornerShape(14.dp)) {
                            Text(appLanguage.pick("+١ شوط", "+1 Round", "+1 davra"))
                        }
                    }
                }
                LiquidGlassCard(shape = RoundedCornerShape(20.dp), contentPadding = 12.dp, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(appLanguage.pick("السعي", "Sa'i", "Sa'y"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(formatNumerals("$saiRounds / 7", useArabicDigits), style = MaterialTheme.typography.headlineMedium.withTabularNums(), color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { saiRounds = (saiRounds + 1) % 8 }, shape = RoundedCornerShape(14.dp)) {
                            Text(appLanguage.pick("+١ شوط", "+1 Lap", "+1 yurish"))
                        }
                    }
                }
            }
            val prepItems = when {
                appLanguage.isArabicUi -> listOf(
                    "ملابس الإحرام والنية من الميقات",
                    "دعاء السفر والتلبية",
                    "حذاء مريح وترطيب مستمر",
                    "تحديث الوصية وسداد الديون"
                )
                appLanguage == AppLanguage.UZBEK -> listOf(
                    "Miqotdan ehrom kiyimi va niyat",
                    "Safar duosi va Talbiya",
                    "Qulay poyabzal va suv",
                    "Vasiyatni yangilash va qarzlarni uzish"
                )
                else -> listOf(
                    "Ihram garments & Niyyah at Miqat",
                    "Travel Dua & Talbiyah",
                    "Comfortable sandals & hydration",
                    "Updated Wasiyyah & settled debts"
                )
            }
            prepItems.forEachIndexed { idx, label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checklist[idx], onCheckedChange = { checklist[idx] = it })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    appLanguage.pick("دليل عبارات السفر والعمرة", "Travel & Umrah Phrasebook", "Safar va Umra iboralari"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            IslamicLibraryData.travelPhrases.forEach { p ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(p.english, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${p.arabic} (${p.transliteration})", fontFamily = AmiriFontFamily, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Text("TR: ${p.turkish} • UR: ${p.urdu}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = glass.glassBorder)
            }
        }

        LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
            Text(
                appLanguage.pick("سجل الصدقات والديون والوصية", "Sadaqah, Debts & Wasiyyah Tracker", "Sadaqa, qarzlar va vasiyat daftari"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "CHARITY" to appLanguage.pick("صدقة", "Charity", "Sadaqa"),
                    "DEBT" to appLanguage.pick("دين / أمانة", "Debt", "Qarz"),
                    "WASIYYAH" to appLanguage.pick("وصية", "Will Note", "Vasiyat")
                ).forEach { (k, l) ->
                    LiquidGlassPill(selected = obKind == k, onClick = { obKind = k }, label = l)
                }
            }
            OutlinedTextField(
                value = obTitle,
                onValueChange = { obTitle = it },
                label = { Text(appLanguage.pick("الوصف", "Description", "Tavsif")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = obAmount,
                onValueChange = { obAmount = it },
                label = { Text(appLanguage.pick("المبلغ (٠ إذا كانت ملاحظة)", "Amount (0 if note)", "Miqdor (izoh bo'lsa 0)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.addObligation(obKind, obTitle, obAmount.toDoubleOrNull() ?: 0.0, null)
                    obTitle = ""
                    obAmount = ""
                },
                enabled = obTitle.isNotBlank(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(appLanguage.pick("إضافة سجل", "Add Record", "Yozuv qo'shish"))
            }

            if (obligationsState is UiState.Success) {
                obligationsState.data.forEach { ob ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${ob.kind}: ${ob.title}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                formatNumerals(
                                    "${String.format(Locale.US, "%.2f", ob.amount)} • ${
                                        if (ob.settled) appLanguage.pick("مكتمل", "Fulfilled", "Bajarildi")
                                        else appLanguage.pick("قيد المتابعة", "Pending", "Kutilmoqda")
                                    }",
                                    useArabicDigits
                                ),
                                style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.toggleObligation(ob) }) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = appLanguage.pick("تبديل الحالة", "Toggle settled", "Holatni almashtirish"),
                                tint = if (ob.settled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { viewModel.deleteObligation(ob.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = appLanguage.pick("حذف", "Delete record", "O'chirish"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModePreviewCard(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    swatchBg: Color,
    swatchAccent: Color,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassPalette.current
    val borderCol = if (selected) MaterialTheme.colorScheme.primary else glass.glassBorder
    val bgCol = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else glass.glassSubtleFill

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = bgCol,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderCol),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(swatchBg)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(swatchAccent)
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = swatchAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsAndPrivacySection(
    settings: UserSettingsDoc,
    strings: LocalizedStrings,
    appLanguage: AppLanguage,
    currentThemePreset: MihrabThemePreset,
    onChangeThemePreset: (MihrabThemePreset) -> Unit,
    currentThemeMode: AppThemeMode,
    useIndic: Boolean,
    onToggleIndic: (Boolean) -> Unit,
    isSuperAdmin: Boolean,
    onOpenAdminDashboard: () -> Unit,
    appOperations: AppOperationsConfig,
    viewModel: MihrabViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val glass = LocalLiquidGlassPalette.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isLocatingGps by viewModel.isLocatingGps.collectAsStateWithLifecycle()
    val locationNotice by viewModel.locationNotice.collectAsStateWithLifecycle()
    val manualOffsets by viewModel.manualOffsets.collectAsStateWithLifecycle()

    var persistentBarEnabled by remember { mutableStateOf(PrayerStatusNotificationManager.isEnabled(context)) }
    var shortcutFeedback by remember { mutableStateOf<String?>(null) }

    var activeDebugToken by remember { mutableStateOf(getAppCheckDebugSecret(context) ?: "") }
    var customDebugTokenInput by remember { mutableStateOf(getCustomAppCheckOverride(context)) }
    var tokenStatusMsg by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectGpsAndMosques(context, appLanguage)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            persistentBarEnabled = true
            PrayerStatusNotificationManager.setEnabled(
                context = context,
                enabled = true,
                settings = settings,
                offsets = manualOffsets,
                language = appLanguage,
                useIndic = useIndic && appLanguage.isArabicUi,
                appOperations = appOperations
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (isSuperAdmin) {
            LiquidGlassCard(
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                borderColor = BurnishedGold.copy(alpha = 0.55f),
                contentPadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_admin_dashboard_card")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IslamicGeometricPatternCanvas(
                        modifier = Modifier.matchParentSize(),
                        tint = BurnishedGold,
                        alpha = 0.045f
                    )
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = BurnishedGold.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.5f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(MihrabIcons.AdminShield, contentDescription = null, tint = BurnishedGold, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.adminDashboardTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = strings.adminBadgeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Button(
                            onClick = onOpenAdminDashboard,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_admin_dashboard_button")
                        ) {
                            Icon(MihrabIcons.AdminShield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                appLanguage.pick(
                                    "فتح لوحة الإدارة الشاملة",
                                    "Open Super Admin Dashboard",
                                    "Super Admin panelini ochish"
                                )
                            )
                        }
                    }
                }
            }
        }

        // 1. GPS Location & Prayer Precision Control
        LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    appLanguage.pick(
                        "الموقع الجغرافي ودقة المواقيت",
                        "Location & Prayer Times Precision",
                        "Joylashuv va namoz vaqtlari aniqligi"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${settings.city} (${String.format(Locale.US, "%.3f", settings.latitude)}, ${String.format(Locale.US, "%.3f", settings.longitude)})",
                style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) {
                            viewModel.detectGpsAndMosques(context, appLanguage)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isLocatingGps,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).testTag("settings_detect_gps_button")
                ) {
                    if (isLocatingGps) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.locatingGps)
                    } else {
                        Icon(Icons.Rounded.MyLocation, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.detectMyLocation)
                    }
                }
                if (!LocationAndMosqueLocator.isLocationEnabled(context)) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(appLanguage.pick("تفعيل GPS", "GPS Settings", "GPS yoqish"))
                    }
                }
            }
            if (!locationNotice.isNullOrBlank()) {
                Text(
                    text = locationNotice!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 2. Permanent Notification Shade Bar & Home Screen Launcher Shortcuts
        LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
            if (appOperations.persistentPrayerNotificationAllowed) {
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
                        Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(strings.persistentPrayerBarTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(strings.persistentPrayerBarDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = persistentBarEnabled,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                persistentBarEnabled = wantEnable
                                PrayerStatusNotificationManager.setEnabled(
                                    context = context,
                                    enabled = wantEnable,
                                    settings = settings,
                                    offsets = manualOffsets,
                                    language = appLanguage,
                                    useIndic = useIndic && appLanguage.isArabicUi,
                                    appOperations = appOperations
                                )
                            }
                        },
                        modifier = Modifier.testTag("settings_persistent_shade_switch")
                    )
                }
                if (persistentBarEnabled) {
                    LiquidGlassPill(
                        selected = false,
                        onClick = {
                            PrayerStatusNotificationManager.syncAndSchedule(
                                context = context,
                                settings = settings,
                                offsets = manualOffsets,
                                language = appLanguage,
                                useIndic = useIndic && appLanguage.isArabicUi,
                                appOperations = appOperations
                            )
                        },
                        icon = Icons.Rounded.Refresh,
                        label = appLanguage.pick("تحديث الإشعار الدائم الآن", "Refresh Prayer Shade Now", "Bildirishnomani yangilash")
                    )
                }
                HorizontalDivider(color = glass.glassBorder)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AppShortcut, null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.pinShortcutsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = appLanguage.pick(
                    "ثبّت اختصارات سريعة على الشاشة الرئيسية للهاتف للوصول بنقرة واحدة.",
                    "Pin one-tap quick launch shortcuts directly onto your phone's home screen.",
                    "Telefon bosh ekraniga bir bosishda ochiladigan yorliqlarni mahkamlang."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LauncherShortcutTarget.entries.forEach { target ->
                    LiquidGlassPill(
                        selected = false,
                        onClick = {
                            val ok = LauncherShortcutHelper.requestPinShortcut(context, target, appLanguage)
                            shortcutFeedback = if (ok) {
                                appLanguage.pick(
                                    "تم إرسال طلب تثبيت اختصار: ${target.shortLabel(appLanguage)}",
                                    "Shortcut request sent: ${target.shortLabel(appLanguage)}",
                                    "Yorliq so'rovi yuborildi: ${target.shortLabel(appLanguage)}"
                                )
                            } else {
                                appLanguage.pick(
                                    "اضغط مطولاً على أيقونة التطبيق في الشاشة الرئيسية لاستخدام الاختصارات",
                                    "Long-press the app icon on your home screen for quick shortcuts",
                                    "Tezkor yorliqlar uchun ilova belgisini bosib turing"
                                )
                            }
                        },
                        icon = Icons.Rounded.AppShortcut,
                        label = target.shortLabel(appLanguage),
                        modifier = Modifier.testTag("settings_pin_shortcut_${target.id}")
                    )
                }
            }
            if (!shortcutFeedback.isNullOrBlank()) {
                Text(shortcutFeedback!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        // 3. Language & Numerals Switcher (AR, EN, UZ, TR, UR)
        LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(MihrabIcons.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.languageToggleTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppLanguage.entries.forEach { lang ->
                    LiquidGlassPill(
                        selected = settings.preferredLanguage == lang.code,
                        onClick = { viewModel.updateSettings(mapOf("preferredLanguage" to lang.code)) },
                        label = lang.nativeLabel,
                        modifier = Modifier.testTag("lang_chip_${lang.code}")
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    appLanguage.pick(
                        "الأرقام العربية الشرقية (٠١٢٣)",
                        "Eastern Arabic Numerals (٠١٢٣)",
                        "Sharqiy arab raqamlari (٠١٢٣)"
                    ),
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = useIndic, onCheckedChange = onToggleIndic)
            }
        }

        // 4. Persistent 3-Mode Theme Engine + Visual Previews + Typography Scale + Mushaf Paper
        LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(MihrabIcons.ThemePalette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.themeModeTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeModePreviewCard(
                    selected = currentThemeMode == AppThemeMode.LIGHT,
                    onClick = { viewModel.updateSettings(mapOf("themeMode" to AppThemeMode.LIGHT.id)) },
                    label = strings.themeLight,
                    icon = Icons.Rounded.LightMode,
                    swatchBg = Color(0xFFF6F4EE),
                    swatchAccent = Color(0xFF0D5C4B),
                    modifier = Modifier.weight(1f).testTag("theme_mode_light")
                )
                ThemeModePreviewCard(
                    selected = currentThemeMode == AppThemeMode.DARK,
                    onClick = { viewModel.updateSettings(mapOf("themeMode" to AppThemeMode.DARK.id)) },
                    label = strings.themeDark,
                    icon = Icons.Rounded.DarkMode,
                    swatchBg = Color(0xFF081412),
                    swatchAccent = Color(0xFF3FD9A9),
                    modifier = Modifier.weight(1f).testTag("theme_mode_dark")
                )
                ThemeModePreviewCard(
                    selected = currentThemeMode == AppThemeMode.SYSTEM,
                    onClick = { viewModel.updateSettings(mapOf("themeMode" to AppThemeMode.SYSTEM.id)) },
                    label = strings.themeSystem,
                    icon = Icons.Rounded.BrightnessAuto,
                    swatchBg = Color(0xFF1D3A34),
                    swatchAccent = Color(0xFFE6C26B),
                    modifier = Modifier.weight(1f).testTag("theme_mode_system")
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MihrabThemePreset.entries.forEach { preset ->
                    val swatchColor = when (preset) {
                        MihrabThemePreset.EMERALD_SANCTUARY -> Color(0xFF0D5C4B)
                        MihrabThemePreset.ROYAL_GOLD -> Color(0xFFC9982E)
                        MihrabThemePreset.MOSQUE_BLUE -> Color(0xFF124670)
                        MihrabThemePreset.WARM_PARCHMENT -> Color(0xFFA97B3A)
                    }
                    LiquidGlassPill(
                        selected = currentThemePreset == preset,
                        onClick = { onChangeThemePreset(preset) },
                        label = preset.labelFor(appLanguage),
                        activeColor = swatchColor
                    )
                }
            }

            HorizontalDivider(color = glass.glassBorder)

            Text(
                text = appLanguage.pick(
                    "ورق مصحف المدينة وحجم الخط",
                    "Madinah Mushaf Paper & Typography Scale",
                    "Madina Mushafi qog'ozi va shrift o'lchami"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MushafPaperTheme.entries.forEach { paper ->
                    val label = when (paper) {
                        MushafPaperTheme.WHITE -> strings.paperWhite
                        MushafPaperTheme.SEPIA -> strings.paperSepia
                        MushafPaperTheme.DARK -> strings.paperDark
                    }
                    LiquidGlassPill(
                        selected = settings.mushafPaper == paper.id,
                        onClick = { viewModel.updateSettings(mapOf("mushafPaper" to paper.id)) },
                        label = label
                    )
                }
            }

            val currentScale = settings.fontScale.toFloat().coerceIn(0.85f, 1.35f)
            Text(
                text = "${strings.fontScaleLabel}: ${(currentScale * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge.withTabularNums()
            )
            Slider(
                value = currentScale,
                onValueChange = { viewModel.updateSettings(mapOf("fontScale" to it.toDouble())) },
                valueRange = 0.85f..1.35f
            )

            HorizontalDivider(color = glass.glassBorder)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        appLanguage.pick("تنبيهات الأذان", "Adhan Alerts", "Azon bildirishnomalari"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        appLanguage.pick(
                            "تفعيل صوت الأذان وتنبيهات أوقات الصلاة",
                            "Enable Adhan audio & prayer alerts",
                            "Azon ovozi va namoz eslatmalarini yoqish"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.adhanEnabled,
                    onCheckedChange = { viewModel.updateSettings(mapOf("adhanEnabled" to it)) }
                )
            }

            if (appOperations.kidsModeEnabled) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            appLanguage.pick("وضع الأطفال", "Kids Mode", "Bolalar rejimi"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            appLanguage.pick(
                                "واجهة مبسطة وممتعة لتعليم القرآن والأذكار",
                                "Simplified joyful learning & star rewards",
                                "Bolalar uchun soddalashtirilgan Qur'on va zikrlar"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.kidsMode,
                        onCheckedChange = { viewModel.updateSettings(mapOf("kidsMode" to it)) }
                    )
                }
            }
        }

        // 5. Firebase App Check Debug Token Inspector & Custom Override (for Debug APKs & AI Studio/GitHub builds)
        LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.VerifiedUser, null, tint = BurnishedGold)
                Text(strings.appCheckDebugTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = appLanguage.pick(
                    "لحل خطأ (Firebase App Check token is invalid) عند تشغيل الذكاء الاصطناعي في نسخة التصحيح (Debug APK)، انسخ هذا الرمز وسجّله في Firebase Console ← App Check ← Apps ← Manage debug tokens.",
                    "To fix 'Firebase App Check token is invalid' on Debug APKs, copy this token and register it under Firebase Console -> App Check -> Apps -> Manage debug tokens.",
                    "Debug APK da 'Firebase App Check token is invalid' xatosini tuzatish uchun ushbu tokenni nusxalab, Firebase Console -> App Check -> Apps -> Manage debug tokens bo'limiga qo'shing."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = glass.glassSubtleFill,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, glass.glassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeDebugToken.ifBlank {
                            appLanguage.pick("غير متوفر بعد", "Not generated yet", "Hali yaratilmagan")
                        },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val current = getAppCheckDebugSecret(context) ?: activeDebugToken
                            if (current.isNotBlank()) {
                                activeDebugToken = current
                                clipboard.setText(AnnotatedString(current))
                                tokenStatusMsg = appLanguage.pick("تم نسخ التوكن ✓", "Copied to clipboard ✓", "Nusxalandi ✓")
                            }
                        },
                        modifier = Modifier.testTag("settings_copy_appcheck_token")
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = appLanguage.pick("نسخ", "Copy", "Nusxalash"))
                    }
                }
            }
            OutlinedTextField(
                value = customDebugTokenInput,
                onValueChange = { customDebugTokenInput = it },
                label = {
                    Text(
                        appLanguage.pick(
                            "تخصيص Debug Token (UUID اختياري مسجّل في Firebase)",
                            "Custom Debug Token Override (optional Firebase UUID)",
                            "Maxsus Debug Token (ixtiyoriy Firebase UUID)"
                        )
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        saveCustomAppCheckDebugToken(context, customDebugTokenInput)
                        activeDebugToken = getAppCheckDebugSecret(context) ?: customDebugTokenInput.trim()
                        tokenStatusMsg = appLanguage.pick("تم حفظ التوكن وتحديثه ✓", "Debug token applied ✓", "Token saqlandi ✓")
                    },
                    enabled = customDebugTokenInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.save)
                }
                OutlinedButton(
                    onClick = {
                        clearCustomAppCheckDebugToken(context)
                        customDebugTokenInput = ""
                        activeDebugToken = getAppCheckDebugSecret(context) ?: ""
                        tokenStatusMsg = appLanguage.pick("تمت إعادة الضبط للرمز التلقائي", "Reset to auto token", "Avto tokenga qaytarildi")
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(appLanguage.pick("إعادة ضبط", "Reset Auto", "Tiklash"))
                }
            }
            if (!tokenStatusMsg.isNullOrBlank()) {
                Text(tokenStatusMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        // 6. Privacy, Export & Account Controls
        LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
            Text(strings.settingsPrivacy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = appLanguage.pick(
                    "تطبيق محراب: مصحف المدينة التفاعلي بـ ٦٠٤ صفحات ونص عثماني كامل مع القراء والتخزين المحلي، مواقيت الصلاة والقبلة بالواقع المعزز، الأذكار، الزكاة والمواريث.",
                    "Mihrab: 604-page interactive Uthmani Madinah Mushaf with multi-reciter audio & offline SQLite+Cloud sync, Prayer & AR Qibla, Adhkar, Zakat & Mawarith tools.",
                    "Mihrob: 604 sahifalik interaktiv Usmoniy Madina Mushafi, ko'p qorilar audiosi, GPS namoz vaqtlari va Qibla, zikrlar, zakot va meros hisoblagichi."
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = { viewModel.exportAllData() },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(appLanguage.pick("تصدير بياناتي (JSON)", "Export My Worship Data (JSON)", "Ma'lumotlarni eksport qilish (JSON)"))
            }
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.signOut)
            }
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.DeleteForever, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    appLanguage.pick(
                        "حذف جميع البيانات السحابية وتسجيل الخروج",
                        "Delete All Cloud Data & Sign Out",
                        "Barcha bulut ma'lumotlarini o'chirish va chiqish"
                    )
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(appLanguage.pick("حذف جميع البيانات السحابية؟", "Delete All Cloud Data?", "Barcha bulut ma'lumotlarini o'chirasizmi?")) },
            text = {
                Text(
                    appLanguage.pick(
                        "سيتم حذف سجلات الصلوات ومفكرة الدعاء وعلامات المصحف وعدادات التسبيح نهائياً من السحابة.",
                        "This permanently deletes your cloud prayer logs, Dua journal, bookmarks, Tasbih counters, and obligations from Firestore.",
                        "Bu bulutdagi namoz qaydlari, duo daftari, xatcho'plar, tasbeh va majburiyatlarni butunlay o'chiradi."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccountAndData(onComplete = onSignOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(appLanguage.pick("حذف الكل", "Delete Everything", "Hammasini o'chirish")) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}
