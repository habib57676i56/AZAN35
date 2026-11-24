package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.UserSettingsDoc
import com.example.domain.CalculatorsEngine
import com.example.domain.IslamicLibraryData
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MihrabThemePreset
import com.example.ui.theme.MushafPaperTheme
import com.example.ui.viewmodel.MihrabViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
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
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val obligationsState by viewModel.obligationsState.collectAsStateWithLifecycle()
    val exportedJson by viewModel.exportedJson.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: UserSettingsDoc()
    val isArabicUi = appLanguage == AppLanguage.ARABIC || appLanguage == AppLanguage.URDU
    var activeSection by remember { mutableIntStateOf(0) } // 0=Calculators, 1=Pilgrim&Charity, 2=Settings

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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val toolsTabs = if (isArabicUi) {
                        listOf("الحاسبات الشرعية", strings.pilgrimGuide, strings.settingsPrivacy)
                    } else {
                        listOf("Calculators", "${strings.pilgrimGuide} & Charity", strings.settingsPrivacy)
                    }
                    toolsTabs.forEachIndexed { idx, lbl ->
                        FilterChip(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            label = { Text(lbl) },
                            modifier = Modifier.testTag("tools_tab_$idx")
                        )
                    }
                }
            }

            when (activeSection) {
                0 -> item { CalculatorsSection() }
                1 -> item { PilgrimAndObligationsSection(obligationsState = obligationsState, viewModel = viewModel) }
                2 -> item {
                    SettingsAndPrivacySection(
                        settings = settings,
                        strings = strings,
                        isArabicUi = isArabicUi,
                        currentThemePreset = currentThemePreset,
                        onChangeThemePreset = onChangeThemePreset,
                        currentThemeMode = currentThemeMode,
                        useIndic = useIndic,
                        onToggleIndic = onToggleIndic,
                        viewModel = viewModel,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorsSection() {
    var calcTab by remember { mutableIntStateOf(0) } // 0=Zakat, 1=Inheritance, 2=Water

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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Zakat", "Inheritance (Faraid)", "Wudu Water").forEachIndexed { idx, lbl ->
                FilterChip(selected = calcTab == idx, onClick = { calcTab = idx }, label = { Text(lbl) })
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Zakat & Nisab Calculator (2.5%)", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(value = cashInput, onValueChange = { cashInput = it }, label = { Text("Cash & Bank Savings") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = goldGramsInput, onValueChange = { goldGramsInput = it }, label = { Text("Gold (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = goldPriceInput, onValueChange = { goldPriceInput = it }, label = { Text("Gold / g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = tradeInput, onValueChange = { tradeInput = it }, label = { Text("Trade / Stocks") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = debtInput, onValueChange = { debtInput = it }, label = { Text("Immediate Debts") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Use Silver Nisab (595g — safer for poor)", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = useSilverNisab, onCheckedChange = { useSilverNisab = it })
                        }
                        HorizontalDivider()
                        Text("Net Zakatable Wealth: ${String.format("%.2f", res.netZakatableWealth)}", style = MaterialTheme.typography.titleMedium)
                        Text("Active Nisab Threshold: ${String.format("%.2f", res.activeNisabValue)}", style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            color = if (res.isZakatDue) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (res.isZakatDue) "Zakat Payable (after 1 lunar Hawl): ${String.format("%.2f", res.zakatPayable)}"
                                else "Below Nisab threshold — Zakat is not obligatory.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
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
                    daughters = daughtersCount
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Islamic Inheritance (Mawarith / Faraid)", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(value = estateInput, onValueChange = { estateInput = it }, label = { Text("Net Estate Value (after debts & Wasiyyah <= 1/3)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("NONE" to "No Spouse", "HUSBAND" to "Husband", "WIFE" to "Wife").forEach { (k, l) ->
                                FilterChip(selected = spouseType == k, onClick = { spouseType = k }, label = { Text(l) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = hasFather, onCheckedChange = { hasFather = it })
                                Text("Father")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = hasMother, onCheckedChange = { hasMother = it })
                                Text("Mother")
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Sons: $sonsCount")
                            Row {
                                TextButton(onClick = { sonsCount = (sonsCount - 1).coerceAtLeast(0) }) { Text("-") }
                                TextButton(onClick = { sonsCount = (sonsCount + 1).coerceAtMost(12) }) { Text("+") }
                            }
                            Text("Daughters: $daughtersCount")
                            Row {
                                TextButton(onClick = { daughtersCount = (daughtersCount - 1).coerceAtLeast(0) }) { Text("-") }
                                TextButton(onClick = { daughtersCount = (daughtersCount + 1).coerceAtMost(12) }) { Text("+") }
                            }
                        }
                        HorizontalDivider()
                        shares.forEach { share ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${share.heirLabel} (${share.fractionLabel})", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${String.format("%.1f", share.percentage)}% • ${String.format("%.2f", share.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                val water = CalculatorsEngine.estimateAblutionWater(isGhusl, tapSeconds)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WaterDrop, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Prophetic Water Conservation (Mudd & Sa')", style = MaterialTheme.typography.titleLarge)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !isGhusl, onClick = { isGhusl = false }, label = { Text("Wudu (1 Mudd ~ 0.75L)") })
                            FilterChip(selected = isGhusl, onClick = { isGhusl = true }, label = { Text("Ghusl (1 Sa' ~ 3L)") })
                        }
                        Text("Tap open duration: $tapSeconds seconds", style = MaterialTheme.typography.labelLarge)
                        Slider(value = tapSeconds.toFloat(), onValueChange = { tapSeconds = it.roundToInt() }, valueRange = 5f..180f)
                        Text(
                            "Estimated usage: ${String.format("%.1f", water.estimatedLiters)} L (~${water.ratio}x Prophetic amount)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(water.feedback, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PilgrimAndObligationsSection(
    obligationsState: UiState<List<com.example.data.firebase.ObligationDoc>>,
    viewModel: MihrabViewModel
) {
    var tawafRounds by remember { mutableIntStateOf(0) }
    var saiRounds by remember { mutableIntStateOf(0) }
    val checklist = remember { mutableStateListOf(false, false, false, false) }

    var obKind by remember { mutableStateOf("CHARITY") }
    var obTitle by remember { mutableStateOf("") }
    var obAmount by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Hajj & Umrah Pilgrim Guide", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tawaf", style = MaterialTheme.typography.titleMedium)
                            Text("$tawafRounds / 7", style = MaterialTheme.typography.headlineMedium)
                            Button(onClick = { tawafRounds = (tawafRounds + 1) % 8 }) { Text("+1 Round") }
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sa'i", style = MaterialTheme.typography.titleMedium)
                            Text("$saiRounds / 7", style = MaterialTheme.typography.headlineMedium)
                            Button(onClick = { saiRounds = (saiRounds + 1) % 8 }) { Text("+1 Lap") }
                        }
                    }
                }
                val prepItems = listOf("Ihram garments & Niyyah at Miqat", "Travel Dua & Talbiyah", "Comfortable sandals & hydration", "Updated Wasiyyah & settled debts")
                prepItems.forEachIndexed { idx, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checklist[idx], onCheckedChange = { checklist[idx] = it })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Travel & Umrah Phrasebook", style = MaterialTheme.typography.titleLarge)
                }
                IslamicLibraryData.travelPhrases.forEach { p ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(p.english, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${p.arabic} (${p.transliteration})", fontFamily = AmiriFontFamily, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("TR: ${p.turkish} • UR: ${p.urdu}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sadaqah, Debts & Wasiyyah Tracker", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CHARITY" to "Charity", "DEBT" to "Debt", "WASIYYAH" to "Will Note").forEach { (k, l) ->
                        FilterChip(selected = obKind == k, onClick = { obKind = k }, label = { Text(l) })
                    }
                }
                OutlinedTextField(value = obTitle, onValueChange = { obTitle = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = obAmount, onValueChange = { obAmount = it }, label = { Text("Amount (0 if note)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        viewModel.addObligation(obKind, obTitle, obAmount.toDoubleOrNull() ?: 0.0, null)
                        obTitle = ""
                        obAmount = ""
                    },
                    enabled = obTitle.isNotBlank()
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Record")
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
                                    "${String.format("%.2f", ob.amount)} • ${if (ob.settled) "Fulfilled" else "Pending"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.toggleObligation(ob) }) {
                                Icon(Icons.Rounded.CheckCircle, "Toggle settled", tint = if (ob.settled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            }
                            IconButton(onClick = { viewModel.deleteObligation(ob.id) }) {
                                Icon(Icons.Rounded.Delete, "Delete record")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsAndPrivacySection(
    settings: UserSettingsDoc,
    strings: LocalizedStrings,
    isArabicUi: Boolean,
    currentThemePreset: MihrabThemePreset,
    onChangeThemePreset: (MihrabThemePreset) -> Unit,
    currentThemeMode: AppThemeMode,
    useIndic: Boolean,
    onToggleIndic: (Boolean) -> Unit,
    viewModel: MihrabViewModel,
    onSignOut: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Arabic-first Language & RTL Switcher
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.languageToggleTitle, style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = settings.preferredLanguage == lang.code,
                            onClick = { viewModel.updateSettings(mapOf("preferredLanguage" to lang.code)) },
                            label = { Text(lang.nativeLabel) },
                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isArabicUi) "الأرقام العربية الشرقية (٠١٢٣)" else "Eastern Arabic Numerals (٠١٢٣)")
                    Switch(checked = useIndic, onCheckedChange = onToggleIndic)
                }
            }
        }

        // 2. Persistent 3-Mode Theme Engine + Typography Scale + Mushaf Paper
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.themeModeTitle, style = MaterialTheme.typography.titleLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentThemeMode == AppThemeMode.LIGHT,
                        onClick = { viewModel.updateSettings(mapOf("themeMode" to AppThemeMode.LIGHT.id)) },
                        leadingIcon = { Icon(Icons.Rounded.LightMode, null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.themeLight) },
                        modifier = Modifier.testTag("theme_mode_light")
                    )
                    FilterChip(
                        selected = currentThemeMode == AppThemeMode.DARK,
                        onClick = { viewModel.updateSettings(mapOf("themeMode" to AppThemeMode.DARK.id)) },
                        leadingIcon = { Icon(Icons.Rounded.DarkMode, null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.themeDark) },
                        modifier = Modifier.testTag("theme_mode_dark")
                    )
                    FilterChip(
                        selected = currentThemeMode == AppThemeMode.SYSTEM,
                        onClick = { viewModel.updateSettings(mapOf("themeMode" to AppThemeMode.SYSTEM.id)) },
                        leadingIcon = { Icon(Icons.Rounded.BrightnessAuto, null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.themeSystem) },
                        modifier = Modifier.testTag("theme_mode_system")
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MihrabThemePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = currentThemePreset == preset,
                            onClick = { onChangeThemePreset(preset) },
                            label = { Text(preset.label) }
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = if (isArabicUi) "ورق مصحف المدينة وحجم الخط" else "Madinah Mushaf Paper & Typography Scale",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MushafPaperTheme.entries.forEach { paper ->
                        val label = when (paper) {
                            MushafPaperTheme.WHITE -> strings.paperWhite
                            MushafPaperTheme.SEPIA -> strings.paperSepia
                            MushafPaperTheme.DARK -> strings.paperDark
                        }
                        FilterChip(
                            selected = settings.mushafPaper == paper.id,
                            onClick = { viewModel.updateSettings(mapOf("mushafPaper" to paper.id)) },
                            label = { Text(label) }
                        )
                    }
                }

                val currentScale = settings.fontScale.toFloat().coerceIn(0.85f, 1.35f)
                Text(
                    text = "${strings.fontScaleLabel}: ${(currentScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = currentScale,
                    onValueChange = { viewModel.updateSettings(mapOf("fontScale" to it.toDouble())) },
                    valueRange = 0.85f..1.35f
                )

                HorizontalDivider()

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isArabicUi) "تنبيهات الأذان" else "Adhan Alerts",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (isArabicUi) "تفعيل صوت الأذان وتنبيهات أوقات الصلاة" else "Enable Adhan audio & prayer alerts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.adhanEnabled,
                        onCheckedChange = { viewModel.updateSettings(mapOf("adhanEnabled" to it)) }
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isArabicUi) "وضع الأطفال" else "Kids Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (isArabicUi) "واجهة مبسطة وممتعة لتعليم القرآن والأذكار" else "Simplified joyful learning & star rewards",
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

        // 3. Privacy, Export & Account Controls
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.settingsPrivacy, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (isArabicUi) {
                        "تطبيق محراب: مصحف المدينة التفاعلي بـ ٦٠٤ صفحات ونص عثماني كامل مع القراء والتخزين المحلي، مواقيت الصلاة والقبلة بالواقع المعزز، الأذكار، الزكاة والمواريث."
                    } else {
                        "Mihrab: 604-page interactive Uthmani Madinah Mushaf with multi-reciter audio & offline SQLite+Cloud sync, Prayer & AR Qibla, Adhkar, Zakat & Mawarith tools."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { viewModel.exportAllData() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isArabicUi) "تصدير بياناتي (JSON)" else "Export My Worship Data (JSON)")
                }
                OutlinedButton(
                    onClick = onSignOut,
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.DeleteForever, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isArabicUi) "حذف جميع البيانات السحابية وتسجيل الخروج" else "Delete All Cloud Data & Sign Out")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isArabicUi) "حذف جميع البيانات السحابية؟" else "Delete All Cloud Data?") },
            text = {
                Text(
                    if (isArabicUi) {
                        "سيتم حذف سجلات الصلوات ومفكرة الدعاء وعلامات المصحف وعدادات التسبيح نهائياً من السحابة."
                    } else {
                        "This permanently deletes your cloud prayer logs, Dua journal, bookmarks, Tasbih counters, and obligations from Firestore."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccountAndData(onComplete = onSignOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (isArabicUi) "حذف الكل" else "Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}
