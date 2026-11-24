package com.example.ui.screens

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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalizedStrings
import com.example.data.firebase.AppOperationsConfig
import com.example.data.firebase.UserSettingsDoc
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
    appOperations: AppOperationsConfig = AppOperationsConfig(),
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

    val toolsTabs = if (isArabicUi) {
        listOf(
            "الحاسبات الشرعية" to MihrabIcons.Calculators,
            strings.pilgrimGuide to MihrabIcons.KaabaCube,
            strings.settingsPrivacy to MihrabIcons.Settings
        )
    } else {
        listOf(
            "Calculators" to MihrabIcons.Calculators,
            "${strings.pilgrimGuide} & Charity" to MihrabIcons.KaabaCube,
            strings.settingsPrivacy to MihrabIcons.Settings
        )
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
                        isSuperAdmin = isSuperAdmin,
                        onOpenAdminDashboard = onOpenAdminDashboard,
                        kidsModeEnabledFlag = appOperations.kidsModeEnabled,
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
private fun CalculatorsSection() {
    val glass = LocalLiquidGlassPalette.current
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
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val labels = listOf("Zakat", "Inheritance (Faraid)", "Wudu Water")
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
                    Text("Zakat & Nisab Calculator (2.5%)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        Text("Use Silver Nisab (595g — safer for poor)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = useSilverNisab, onCheckedChange = { useSilverNisab = it })
                    }
                    HorizontalDivider(color = glass.glassBorder)
                    Text("Net Zakatable Wealth: ${String.format("%.2f", res.netZakatableWealth)}", style = MaterialTheme.typography.titleMedium.withTabularNums())
                    Text("Active Nisab Threshold: ${String.format("%.2f", res.activeNisabValue)}", style = MaterialTheme.typography.bodyMedium.withTabularNums(), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            text = if (res.isZakatDue) "Zakat Payable (after 1 lunar Hawl): ${String.format("%.2f", res.zakatPayable)}"
                            else "Below Nisab threshold — Zakat is not obligatory.",
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
                    daughters = daughtersCount
                )
                LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                    Text("Islamic Inheritance (Mawarith / Faraid)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = estateInput, onValueChange = { estateInput = it }, label = { Text("Net Estate Value (after debts & Wasiyyah <= 1/3)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NONE" to "No Spouse", "HUSBAND" to "Husband", "WIFE" to "Wife").forEach { (k, l) ->
                            LiquidGlassPill(selected = spouseType == k, onClick = { spouseType = k }, label = l)
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
                        Text("Sons: $sonsCount", style = MaterialTheme.typography.bodyLarge.withTabularNums())
                        Row {
                            TextButton(onClick = { sonsCount = (sonsCount - 1).coerceAtLeast(0) }) { Text("-") }
                            TextButton(onClick = { sonsCount = (sonsCount + 1).coerceAtMost(12) }) { Text("+") }
                        }
                        Text("Daughters: $daughtersCount", style = MaterialTheme.typography.bodyLarge.withTabularNums())
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
                                "${String.format("%.1f", share.percentage)}% • ${String.format("%.2f", share.amount)}",
                                style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            2 -> {
                val water = CalculatorsEngine.estimateAblutionWater(isGhusl, tapSeconds)
                LiquidGlassCard(shape = RoundedCornerShape(24.dp), containerColor = glass.glassFillStrong) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WaterDrop, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Prophetic Water Conservation (Mudd & Sa')", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LiquidGlassPill(selected = !isGhusl, onClick = { isGhusl = false }, label = "Wudu (1 Mudd ~ 0.75L)")
                        LiquidGlassPill(selected = isGhusl, onClick = { isGhusl = true }, label = "Ghusl (1 Sa' ~ 3L)")
                    }
                    Text("Tap open duration: $tapSeconds seconds", style = MaterialTheme.typography.labelLarge.withTabularNums())
                    Slider(value = tapSeconds.toFloat(), onValueChange = { tapSeconds = it.roundToInt() }, valueRange = 5f..180f)
                    Text(
                        "Estimated usage: ${String.format("%.1f", water.estimatedLiters)} L (~${water.ratio}x Prophetic amount)",
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
            Text("Hajj & Umrah Pilgrim Guide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LiquidGlassCard(shape = RoundedCornerShape(20.dp), contentPadding = 12.dp, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tawaf", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$tawafRounds / 7", style = MaterialTheme.typography.headlineMedium.withTabularNums(), color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { tawafRounds = (tawafRounds + 1) % 8 }, shape = RoundedCornerShape(14.dp)) { Text("+1 Round") }
                    }
                }
                LiquidGlassCard(shape = RoundedCornerShape(20.dp), contentPadding = 12.dp, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sa'i", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$saiRounds / 7", style = MaterialTheme.typography.headlineMedium.withTabularNums(), color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { saiRounds = (saiRounds + 1) % 8 }, shape = RoundedCornerShape(14.dp)) { Text("+1 Lap") }
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

        LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Travel & Umrah Phrasebook", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            Text("Sadaqah, Debts & Wasiyyah Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("CHARITY" to "Charity", "DEBT" to "Debt", "WASIYYAH" to "Will Note").forEach { (k, l) ->
                    LiquidGlassPill(selected = obKind == k, onClick = { obKind = k }, label = l)
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
                enabled = obTitle.isNotBlank(),
                shape = RoundedCornerShape(18.dp)
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
                                style = MaterialTheme.typography.bodyMedium.withTabularNums(),
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
            // Mini visual device swatch preview
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
    isArabicUi: Boolean,
    currentThemePreset: MihrabThemePreset,
    onChangeThemePreset: (MihrabThemePreset) -> Unit,
    currentThemeMode: AppThemeMode,
    useIndic: Boolean,
    onToggleIndic: (Boolean) -> Unit,
    isSuperAdmin: Boolean,
    onOpenAdminDashboard: () -> Unit,
    kidsModeEnabledFlag: Boolean,
    viewModel: MihrabViewModel,
    onSignOut: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                            Text(if (isArabicUi) "فتح لوحة الإدارة الشاملة" else "Open Super Admin Dashboard")
                        }
                    }
                }
            }
        }

        // 1. Arabic-first Language & RTL Switcher
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
                Text(if (isArabicUi) "الأرقام العربية الشرقية (٠١٢٣)" else "Eastern Arabic Numerals (٠١٢٣)", modifier = Modifier.weight(1f))
                Switch(checked = useIndic, onCheckedChange = onToggleIndic)
            }
        }

        // 2. Persistent 3-Mode Theme Engine + Visual Previews + Typography Scale + Mushaf Paper
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
                        label = if (isArabicUi) preset.labelAr else preset.label,
                        activeColor = swatchColor
                    )
                }
            }

            HorizontalDivider(color = glass.glassBorder)

            Text(
                text = if (isArabicUi) "ورق مصحف المدينة وحجم الخط" else "Madinah Mushaf Paper & Typography Scale",
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

            if (kidsModeEnabledFlag) {
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
        LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
            Text(strings.settingsPrivacy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isArabicUi) "تصدير بياناتي (JSON)" else "Export My Worship Data (JSON)")
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
                Text(if (isArabicUi) "حذف جميع البيانات السحابية وتسجيل الخروج" else "Delete All Cloud Data & Sign Out")
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
